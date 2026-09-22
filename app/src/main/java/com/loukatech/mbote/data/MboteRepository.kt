package com.loukatech.mbote.data

import com.loukatech.mbote.model.*
import com.loukatech.mbote.service.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Serializable
private data class CachedAuthSession(
    val authToken: String,
    val refreshToken: String? = null,
    val userProfile: UserProfile
)

class MboteRepository(
    val apiService: MboteApiService = MboteApiService(),
    val publicationApiService: PublicationApiService = PublicationApiService(),
    val groupCallApiService: GroupCallApiService = GroupCallApiService()
) {

    init {
        INSTANCE = this
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val maxInlineStatusImageBytes = 2 * 1024 * 1024
    private val maxInlineStatusAudioBytes = 4 * 1024 * 1024
    private val maxInlineActusImageBytes = 2 * 1024 * 1024

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _userGiftState = MutableStateFlow(UserGiftState())
    val userGiftState: StateFlow<UserGiftState> = _userGiftState.asStateFlow()

    private val _channels = MutableStateFlow<List<ChannelSummary>>(emptyList())
    val channels: StateFlow<List<ChannelSummary>> = _channels.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _productionReadiness = MutableStateFlow(ProductionReadiness())
    val productionReadiness: StateFlow<ProductionReadiness> = _productionReadiness.asStateFlow()


    private val _cachedMessageCount = MutableStateFlow(0)
    val cachedMessageCount: StateFlow<Int> = _cachedMessageCount.asStateFlow()

    fun toggleOfflineMode() {
        _isOffline.value = !_isOffline.value
    }

    suspend fun login(email: String, pass: String): Result<PendingOtpChallenge> =
        apiService.login(LoginRequest(email = email, password = pass))

    suspend fun verifyLoginOtp(pendingUserId: String, otp: String): Result<Unit> =
        applyVerifiedSession(apiService.verifyLoginOtp(pendingUserId, otp))

    suspend fun register(request: RegisterRequest): Result<PendingOtpChallenge> = apiService.register(request)

    suspend fun confirmDesktopQrLogin(qrPayload: String): Result<Unit> {
        val token = runCatching {
            android.net.Uri.parse(qrPayload).getQueryParameter("token")
        }.getOrNull()?.trim().orEmpty().ifBlank { qrPayload.trim() }
        if (token.length !in 32..256 || !token.matches(Regex("^[A-Za-z0-9_-]+$"))) {
            return Result.failure(IllegalArgumentException("Ce QR code n’est pas une session Mboté PC valide."))
        }
        return apiService.confirmDesktopQrLogin(token)
    }

    suspend fun getRegistrationPublicConfig(): Result<RegistrationPublicConfig> = apiService.getRegistrationPublicConfig()

    suspend fun refreshProductionReadiness(): Result<ProductionReadiness> {
        val result = apiService.getProductionReadiness()
        result.onSuccess { _productionReadiness.value = it }
        return result
    }


    suspend fun verifyRegistrationOtp(pendingUserId: String, otp: String): Result<Unit> {
        return applyVerifiedSession(apiService.verifyRegistrationOtp(pendingUserId, otp))
    }

    private fun applyVerifiedSession(result: Result<VerifiedAuthResponse>): Result<Unit> {
        return if (result.isSuccess) {
            val data = result.getOrNull()!!
            _userProfile.update {
                it.copy(
                    id = data.user.id.toString().trim('"'),
                    name = data.user.name,
                    username = data.user.username,
                    email = data.user.email,
                    phone = data.user.phoneNumber,
                    avatar = if (data.user.avatar.isNotBlank()) data.user.avatar else it.avatar,
                    country = data.user.country,
                    city = data.user.city,
                    isVerified = true
                )
            }
            _isAuthenticated.value = true
            saveCachedSession()
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Code OTP invalide"))
        }
    }

    suspend fun loginWithGoogle(email: String = "", displayName: String = "", avatarUrl: String? = null): Result<Unit> {
        if (email.isBlank() || displayName.isBlank()) {
            return Result.failure(IllegalStateException("Google OAuth doit fournir un jeton d'identité vérifié."))
        }
        return loginFromAuthResponse(apiService.loginWithGoogle(GoogleAuthRequest(email = email, displayName = displayName, avatarUrl = avatarUrl)))
    }

    suspend fun loginWithGitHub(email: String = "", displayName: String = "", avatarUrl: String? = null): Result<Unit> =
        Result.failure(UnsupportedOperationException("GitHub OAuth doit être configuré côté serveur."))

    private fun loginFromAuthResponse(result: Result<AuthResponseData>): Result<Unit> = if (result.isSuccess) {
        val data = result.getOrNull()!!
        _userProfile.update { it.copy(id = data.userId, name = data.name, email = data.email, phone = data.phone, avatar = data.avatar, role = data.role, isVerified = data.isVerified) }
        MboteBackendConfig.refreshToken = data.refreshToken
        _isAuthenticated.value = true
        saveCachedSession()
        Result.success(Unit)
    } else Result.failure(result.exceptionOrNull() ?: Exception("Échec de l'authentification"))

    suspend fun requestPasswordReset(email: String): Result<String> {
        return apiService.requestForgotPassword(email)
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPass: String): Result<Boolean> =
        apiService.confirmResetPassword(
            ResetPasswordConfirmRequest(email = email, resetCode = code, newPassword = newPass)
        )

    suspend fun loginAdmin(key: String, email: String, pass: String): Result<AdminStatsData> {
        return apiService.loginAdmin(AdminLoginRequest(adminKey = key, email = email, password = pass))
    }

    suspend fun getAdminStats(): Result<AdminStatsData> {
        return apiService.getAdminStats()
    }

    fun setBackendServerUrl(newUrl: String) {
        MboteBackendConfig.baseUrl = newUrl
    }

    fun logout() {
        MboteBackendConfig.authToken?.takeIf { it.isNotBlank() }?.let { token ->
            CoroutineScope(Dispatchers.IO).launch {
                apiService.logoutCurrentSession(token)
            }
        }
        MboteBackendConfig.authToken = null
        MboteBackendConfig.refreshToken = null
        _isAuthenticated.value = false
        clearCachedSession()
    }

    suspend fun deleteMyAccount(): Result<Unit> {
        val result = apiService.deleteMyAccount()
        return if (result.isSuccess && result.getOrDefault(false)) {
            logout()
            _userProfile.value = UserProfile()
            saveCachedUserProfile()
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: IllegalStateException("Suppression du compte impossible"))
        }
    }

    private val _notifications = MutableStateFlow<List<MboteNotification>>(emptyList())
    val notifications: StateFlow<List<MboteNotification>> = _notifications.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messagingError = MutableStateFlow<String?>(null)
    val messagingError: StateFlow<String?> = _messagingError.asStateFlow()

    fun clearMessagingError() { _messagingError.value = null }

    private val _calls = MutableStateFlow<List<CallItem>>(emptyList())
    val calls: StateFlow<List<CallItem>> = _calls.asStateFlow()

    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses: StateFlow<List<StatusItem>> = _statuses.asStateFlow()

    private val _newsPosts = MutableStateFlow<List<NewsPost>>(emptyList())
    val newsPosts: StateFlow<List<NewsPost>> = _newsPosts.asStateFlow()

    private val _meetings = MutableStateFlow<List<MeetingItem>>(emptyList())
    val meetings: StateFlow<List<MeetingItem>> = _meetings.asStateFlow()

    private val _jobs = MutableStateFlow<List<JobOffer>>(emptyList())
    val jobs: StateFlow<List<JobOffer>> = _jobs.asStateFlow()

    private val _aronQuestions = MutableStateFlow<List<AronQuestion>>(emptyList())
    val aronQuestions: StateFlow<List<AronQuestion>> = _aronQuestions.asStateFlow()

    private val _discoverProfiles = MutableStateFlow<List<DiscoverProfile>>(emptyList())
    val discoverProfiles: StateFlow<List<DiscoverProfile>> = _discoverProfiles.asStateFlow()

    private val _reports = MutableStateFlow<List<com.loukatech.mbote.model.ReportItem>>(emptyList())
    val reports: StateFlow<List<com.loukatech.mbote.model.ReportItem>> = _reports.asStateFlow()

    fun submitReport(type: String, targetName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            apiService.submitReportApi(type, targetName, "Signalement de contenu suspect ou inapproprié")
                .onSuccess { remoteId ->
                    val report = com.loukatech.mbote.model.ReportItem(
                        id = remoteId,
                        type = type,
                        targetName = targetName,
                        reporterName = _userProfile.value.name,
                        reason = "Signalement de contenu suspect ou inapproprié",
                        status = "PENDING",
                        timestamp = "À l'instant"
                    )
                    _reports.update { listOf(report) + it }
                }
                .onFailure { _messagingError.value = it.message ?: "Le signalement n’a pas pu être envoyé." }
        }
    }

    fun updateReportStatus(reportId: String, newStatus: String) {
        _reports.update { list ->
            list.map { rep ->
                if (rep.id == reportId) rep.copy(status = newStatus) else rep
            }
        }
    }

    private val _shortVideos = MutableStateFlow<List<ShortVideo>>(emptyList())
    val shortVideos: StateFlow<List<ShortVideo>> = _shortVideos.asStateFlow()

    private val _blockedContactIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedContactIds: StateFlow<Set<String>> = _blockedContactIds.asStateFlow()

    private val _mastaUsers = MutableStateFlow<List<MastaUser>>(emptyList())
    val mastaUsers: StateFlow<List<MastaUser>> = _mastaUsers.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequestDto>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequestDto>> = _friendRequests.asStateFlow()

    fun updateMastaUsers(users: List<MastaUser>) {
        _mastaUsers.value = users
    }

    fun blockContact(contactId: String) {
        _blockedContactIds.update { it + contactId }
        CoroutineScope(Dispatchers.IO).launch {
            apiService.setUserBlockedApi(contactId, true).onFailure {
                _blockedContactIds.update { ids -> ids - contactId }
                _messagingError.value = it.message ?: "Le contact n’a pas pu être bloqué."
            }
        }
    }

    fun unblockContact(contactId: String) {
        _blockedContactIds.update { it - contactId }
        CoroutineScope(Dispatchers.IO).launch {
            apiService.setUserBlockedApi(contactId, false).onFailure {
                _blockedContactIds.update { ids -> ids + contactId }
                _messagingError.value = it.message ?: "Le contact n’a pas pu être débloqué."
            }
        }
    }

    fun isContactBlocked(contactId: String): Boolean {
        return _blockedContactIds.value.contains(contactId)
    }

    private fun appendOptimisticMessage(chatId: String, message: Message, preview: String) {
        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id == chatId) chat.copy(
                    lastMessage = preview,
                    lastMessageTime = message.timestamp,
                    messages = chat.messages + message
                ) else chat
            }
        }
    }

    private fun reconcileMessage(chatId: String, localId: String, remote: Message) {
        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id != chatId) chat else {
                    val messages = chat.messages
                        .filterNot { it.id == remote.id && it.id != localId }
                        .map { if (it.id == localId) remote else it }
                    chat.copy(
                        lastMessage = remote.text,
                        lastMessageTime = remote.timestamp,
                        messages = messages
                    )
                }
            }
        }
    }

    private fun rejectOptimisticMessage(chatId: String, localId: String, error: Throwable?) {
        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id != chatId) chat else {
                    val messages = chat.messages.filterNot { it.id == localId }
                    val last = messages.lastOrNull()
                    chat.copy(
                        lastMessage = last?.text.orEmpty(),
                        lastMessageTime = last?.timestamp ?: chat.lastMessageTime,
                        messages = messages
                    )
                }
            }
        }
        _messagingError.value = error?.message ?: "Le message n’a pas pu être envoyé."
    }

    fun sendMediaMessage(
        context: android.content.Context,
        chatId: String,
        mediaUrl: String,
        mediaType: MediaType,
        caption: String = "",
        replyTo: Message? = null
    ) {
        val currentTime = timeFormat.format(Date())
        val previewText = when {
            caption.isNotBlank() -> caption
            mediaType == MediaType.VIDEO -> "📹 Vidéo"
            mediaType == MediaType.IMAGE -> "📷 Photo"
            else -> "Fichier média"
        }
        val localId = "local_${UUID.randomUUID()}"
        val newMsg = Message(
            id = localId,
            text = caption.ifBlank { if (mediaType == MediaType.VIDEO) "Vidéo" else "Photo" },
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatar,
            timestamp = currentTime,
            status = MessageStatus.SENT,
            isMine = true,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            replyToText = replyTo?.text,
            replyToSender = replyTo?.senderName
        )
        appendOptimisticMessage(chatId, newMsg, previewText)

        CoroutineScope(Dispatchers.IO).launch {
            val persistedUrl = if (mediaUrl.startsWith("content:") || mediaUrl.startsWith("file:")) {
                contentUriToDataUrl(context, android.net.Uri.parse(mediaUrl), 12 * 1024 * 1024)
                    .getOrElse {
                        rejectOptimisticMessage(chatId, localId, it)
                        return@launch
                    }
            } else mediaUrl
            apiService.sendMessageApi(
                SendMessageDto(
                    chatId = chatId,
                    text = previewText,
                    mediaType = if (mediaType == MediaType.VIDEO) "VIDEO" else "IMAGE",
                    mediaUrl = persistedUrl,
                    replyToMessageId = replyTo?.id
                )
            ).onSuccess { reconcileMessage(chatId, localId, it.toMessage()) }
                .onFailure { rejectOptimisticMessage(chatId, localId, it) }
        }
    }

    fun sendMessage(chatId: String, text: String, replyTo: Message? = null) {
        val currentTime = timeFormat.format(Date())
        val chat = _chats.value.find { it.id == chatId }
        val disappearingSec = chat?.disappearingTimerSec ?: 0

        val localId = "local_${UUID.randomUUID()}"
        val newMessage = Message(
            id = localId,
            text = text,
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatar,
            timestamp = currentTime,
            status = MessageStatus.SENT,
            isMine = true,
            replyToText = replyTo?.text,
            replyToSender = replyTo?.senderName,
            disappearingDurationSec = disappearingSec
        )

        appendOptimisticMessage(chatId, newMessage, text)

        // Post message to backend REST API asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            apiService.sendMessageApi(
                SendMessageDto(
                    chatId = chatId,
                    text = text,
                    mediaType = "TEXT",
                    replyToMessageId = replyTo?.id
                )
            ).onSuccess { reconcileMessage(chatId, localId, it.toMessage()) }
                .onFailure { rejectOptimisticMessage(chatId, localId, it) }
        }

    }

    fun sendVoiceMessage(
        context: android.content.Context,
        chatId: String,
        audioPath: String,
        durationSec: Int,
        replyTo: Message? = null
    ) {
        val currentTime = timeFormat.format(Date())
        val formattedDuration = String.format("%02d:%02d", durationSec / 60, durationSec % 60)
        val chat = _chats.value.find { it.id == chatId }
        val disappearingSec = chat?.disappearingTimerSec ?: 0

        val localId = "local_${UUID.randomUUID()}"
        val voiceMsg = Message(
            id = localId,
            text = "🎤 Message vocal ($formattedDuration)",
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatar,
            timestamp = currentTime,
            status = MessageStatus.SENT,
            isMine = true,
            mediaType = MediaType.AUDIO,
            mediaUrl = audioPath,
            audioDurationSec = durationSec,
            replyToText = replyTo?.text,
            replyToSender = replyTo?.senderName,
            disappearingDurationSec = disappearingSec
        )

        appendOptimisticMessage(chatId, voiceMsg, "🎤 Message vocal ($formattedDuration)")

        // Post message to backend REST API asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val audioFile = File(audioPath)
            if (!audioFile.isFile || audioFile.length() <= 0L || audioFile.length() > 12 * 1024 * 1024) {
                rejectOptimisticMessage(chatId, localId, IllegalArgumentException("Le message vocal est introuvable ou trop volumineux."))
                return@launch
            }
            val extension = audioFile.extension.lowercase()
            val mime = when (extension) {
                "m4a", "mp4" -> "audio/mp4"
                "ogg", "opus" -> "audio/ogg"
                "wav" -> "audio/wav"
                else -> "audio/mpeg"
            }
            val encoded = android.util.Base64.encodeToString(audioFile.readBytes(), android.util.Base64.NO_WRAP)
            val remoteAudioUrl = "data:$mime;base64,$encoded"
            apiService.sendMessageApi(
                SendMessageDto(
                    chatId = chatId,
                    text = "🎤 Message vocal ($formattedDuration)",
                    mediaType = "AUDIO",
                    mediaUrl = remoteAudioUrl,
                    replyToMessageId = replyTo?.id
                )
            ).onSuccess { reconcileMessage(chatId, localId, it.toMessage()) }
                .onFailure { rejectOptimisticMessage(chatId, localId, it) }
        }

    }

    private fun sendStructuredChatMessage(
        chatId: String,
        message: Message,
        preview: String,
        apiMediaType: String,
        metadata: JsonObject
    ) {
        appendOptimisticMessage(chatId, message, preview)
        CoroutineScope(Dispatchers.IO).launch {
            apiService.sendMessageApi(
                SendMessageDto(
                    chatId = chatId,
                    text = message.text,
                    mediaType = apiMediaType,
                    metadata = metadata
                )
            ).onSuccess { reconcileMessage(chatId, message.id, it.toMessage()) }
                .onFailure { rejectOptimisticMessage(chatId, message.id, it) }
        }
    }

    private inline fun <reified T> decodeMessageMetadata(metadata: JsonObject?, key: String): T? {
        return metadata?.get(key)?.let { element ->
            runCatching { json.decodeFromString<T>(element.toString()) }.getOrNull()
        }
    }

    fun MessageDto.toMessage(): Message {
        val displayTime = timestamp.substringAfter('T', timestamp).take(5).ifBlank { "À l'instant" }
        val mine = isMine || senderId == _userProfile.value.id
        val poll = decodeMessageMetadata<PollData>(metadata, "pollData")
        val location = decodeMessageMetadata<LocationData>(metadata, "locationData")
        val payment = decodeMessageMetadata<PaymentTransferData>(metadata, "paymentData")
        val aron = decodeMessageMetadata<AronQuestion>(metadata, "aronQuestion")
        return Message(
            id = this.id,
            text = this.text,
            senderId = this.senderId,
            senderName = this.senderName,
            senderAvatar = this.senderAvatar,
            timestamp = displayTime,
            status = when (status.lowercase()) {
                "read" -> MessageStatus.READ
                "delivered" -> MessageStatus.DELIVERED
                else -> MessageStatus.SENT
            },
            isMine = mine,
            isEncrypted = true,
            mediaType = when (this.mediaType) {
                "IMAGE" -> MediaType.IMAGE
                "AUDIO" -> MediaType.AUDIO
                "VIDEO" -> MediaType.VIDEO
                "FILE" -> MediaType.FILE
                "LOCATION" -> MediaType.LOCATION
                "POLL" -> MediaType.POLL
                "PAYMENT" -> MediaType.PAYMENT
                "ARON_QUESTION" -> MediaType.ARON_QUESTION
                else -> MediaType.NONE
            },
            mediaUrl = this.mediaUrl,
            audioDurationSec = this.audioDurationSec,
            isRecalled = this.isRecalled,
            isStarred = this.isStarred,
            replyToText = this.replyToText,
            replyToSender = this.replyToSender,
            reactions = this.reactions,
            pollData = poll,
            locationData = location,
            paymentData = payment,
            aronQuestion = aron
        )
    }

    suspend fun syncAllFromBackend(): Result<Unit> {
        val hasSession = !MboteBackendConfig.authToken.isNullOrBlank()
        // Asynchronously fetch Call History from real REST API
        if (hasSession) try {
            val callsResult = apiService.fetchCallHistory()
            if (callsResult.isSuccess) {
                val remoteCalls = callsResult.getOrNull()
                if (remoteCalls != null) {
                    _calls.value = remoteCalls
                }
            }
        } catch (e: Exception) {
            // Keep existing calls if offline
        }

        // Asynchronously fetch Masta users from real REST API
        try {
            val mastaResult = apiService.fetchMastaUsers()
            if (mastaResult.isSuccess) {
                val remoteMasta = mastaResult.getOrNull() ?: emptyList()
                _mastaUsers.value = remoteMasta
            }
        } catch (e: Exception) {
            // Keep existing Masta if offline
        }

        // Asynchronously fetch Short videos from real REST API
        try {
            val shortsResult = apiService.fetchShortVideos()
            if (shortsResult.isSuccess) {
                val remoteShorts = shortsResult.getOrNull() ?: emptyList()
                _shortVideos.value = remoteShorts
            }
        } catch (e: Exception) {
            // Keep existing Short videos if offline
        }

        publicationApiService.fetchActusPosts().onSuccess { _newsPosts.value = it }
        publicationApiService.fetchStatuses(_userProfile.value.id).onSuccess { _statuses.value = it }

        if (!hasSession) {
            _messagingError.value = null
            return Result.success(Unit)
        }

        apiService.fetchMyBadges().onSuccess { badgeIds ->
            val badges = badgeIds.mapNotNull { id -> BadgeType.entries.firstOrNull { it.id == id } }
            _userProfile.update { it.copy(badges = badges) }
        }

        apiService.fetchGiftCatalog().onSuccess { catalog ->
            _userGiftState.update { current ->
                current.copy(
                    storeGifts = catalog.map { item ->
                        GiftItem(
                            id = item.id,
                            name = item.name,
                            emoji = item.emoji,
                            priceFcfa = item.priceFcfa,
                            description = item.description
                        )
                    }
                )
            }
        }

        apiService.fetchGiftState().onSuccess { remote ->
            _userGiftState.update { current ->
                current.copy(
                    inventory = remote.inventory.associate { it.giftId to it.quantity },
                    transactions = remote.transactions.map { tx ->
                        GiftTransaction(
                            id = tx.id,
                            giftId = tx.giftId,
                            giftName = tx.giftName,
                            emoji = tx.emoji,
                            amountFcfa = tx.amountFcfa,
                            isReceived = !tx.isSent,
                            counterpartName = tx.counterpartName,
                            timestamp = tx.createdAt.ifBlank { "Récent" },
                            status = when (tx.status.uppercase(Locale.ROOT)) {
                                "COMPLETED" -> "Complété"
                                "PENDING" -> "En attente"
                                "FAILED" -> "Échoué"
                                else -> tx.status
                            }
                        )
                    },
                    withdrawals = remote.withdrawals.map { withdrawal ->
                        WithdrawalTransaction(
                            id = withdrawal.id,
                            amountFcfa = withdrawal.amountFcfa,
                            provider = withdrawal.provider,
                            destinationAccount = withdrawal.destinationAccount,
                            timestamp = withdrawal.createdAt.ifBlank { "Récent" },
                            status = runCatching {
                                WithdrawalStatus.valueOf(withdrawal.status.uppercase(Locale.ROOT))
                            }.getOrDefault(WithdrawalStatus.PENDING),
                            referenceCode = "RET-${withdrawal.id.take(8).uppercase(Locale.ROOT)}"
                        )
                    },
                    totalVirtualEarnedFcfa = remote.giftEarningsBalanceFcfa
                )
            }
            _userProfile.update { it.copy(walletBalanceFcfa = remote.walletBalanceFcfa) }
        }

        val chatsResult = apiService.fetchUserChats()
        if (chatsResult.isSuccess) {
            val remoteChats = chatsResult.getOrNull() ?: emptyList()
            val mappedChats = remoteChats.map { chatDto ->
                val messagesResult = apiService.fetchMessagesForChat(chatDto.id)
                val messagesList = if (messagesResult.isSuccess) {
                    messagesResult.getOrNull()?.map { it.toMessage() } ?: emptyList()
                } else {
                    emptyList()
                }
                
                val participants = chatDto.participants.map { participant ->
                    Participant(
                        id = participant.id,
                        name = participant.name,
                        avatar = participant.avatar,
                        isOnline = participant.isOnline,
                        role = participant.role
                    )
                }
                val other = participants.firstOrNull { it.id != _userProfile.value.id }
                Chat(
                    id = chatDto.id,
                    name = chatDto.name.takeUnless { it == "Discussion" || it.isBlank() } ?: other?.name ?: "Discussion",
                    avatar = chatDto.avatar.ifBlank { other?.avatar.orEmpty() },
                    lastMessage = chatDto.lastMessage,
                    lastMessageTime = chatDto.lastMessageTime,
                    unreadCount = chatDto.unreadCount,
                    isOnline = chatDto.isOnline,
                    isGroup = chatDto.isGroup,
                    isChannel = chatDto.isChannel,
                    isAI = false,
                    isVerified = false,
                    participants = participants,
                    disappearingTimerSec = chatDto.disappearingDurationSec,
                    messages = messagesList
                )
            }
            _chats.value = mappedChats
            return Result.success(Unit)
        } else {
            return Result.failure(chatsResult.exceptionOrNull() ?: Exception("Échec de synchronisation des chats"))
        }
    }

    suspend fun addCallLog(call: CallItem) {
        _calls.update { listOf(call) + it }
        try {
            apiService.logCallApi(call)
        } catch (e: Exception) {
            // Log local only if offline
        }
    }

    suspend fun refreshCallsFromBackend() {
        val result = apiService.fetchCallHistory()
        if (result.isSuccess) {
            val remoteCalls = result.getOrNull()
            if (remoteCalls != null) {
                _calls.value = remoteCalls
            }
        }
    }

    suspend fun refreshMastaFromBackend() {
        apiService.fetchAronQuestions().onSuccess { _aronQuestions.value = it }
        apiService.fetchDiscoverProfiles().onSuccess { _discoverProfiles.value = it }
        apiService.fetchMastaUsers().onSuccess { _mastaUsers.value = it }
        apiService.fetchFriendRequests().onSuccess { _friendRequests.value = it }
    }

    suspend fun sendFriendRequest(targetUserId: String): Result<Unit> {
        val result = apiService.sendFriendRequest(targetUserId)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        refreshMastaFromBackend()
        return Result.success(Unit)
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        val result = apiService.acceptFriendRequest(requestId)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        refreshMastaFromBackend()
        return Result.success(Unit)
    }

    suspend fun declineOrCancelFriendRequest(requestId: String): Result<Unit> {
        val result = apiService.declineOrCancelFriendRequest(requestId)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        refreshMastaFromBackend()
        return Result.success(Unit)
    }

    suspend fun refreshShortsFromBackend() {
        val result = apiService.fetchShortVideos()
        if (result.isSuccess) {
            val remoteShorts = result.getOrNull()
            if (remoteShorts != null) {
                _shortVideos.value = remoteShorts
            }
        }
    }

    suspend fun refreshPublicationsFromBackend() {
        publicationApiService.fetchActusPosts().onSuccess { _newsPosts.value = it }
        publicationApiService.fetchStatuses(_userProfile.value.id).onSuccess { _statuses.value = it }
    }

    suspend fun refreshMessagesForChat(chatId: String) {
        val result = apiService.fetchMessagesForChat(chatId)
        if (result.isSuccess) {
            val remoteMessages = result.getOrNull()?.map { it.toMessage() } ?: emptyList()
            _chats.update { chatList ->
                chatList.map { chat ->
                    if (chat.id == chatId) {
                        chat.copy(
                            messages = remoteMessages
                        )
                    } else chat
                }
            }
        }
    }

    suspend fun refreshChatsFromBackend() {
        syncAllFromBackend()
    }

    fun sendAronQuestion(chatId: String, question: AronQuestion) {
        val currentTime = timeFormat.format(Date())
        val msgText = "🔮 Question d'Aron #${question.id} :\n« ${question.questionFr} »"

        val aronMsg = Message(
            id = "local_${UUID.randomUUID()}",
            text = msgText,
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatar,
            timestamp = currentTime,
            status = MessageStatus.SENT,
            isMine = true,
            mediaType = MediaType.ARON_QUESTION,
            aronQuestion = question
        )

        sendStructuredChatMessage(
            chatId = chatId,
            message = aronMsg,
            preview = "🔮 ${question.category} : Question #${question.id}",
            apiMediaType = "ARON_QUESTION",
            metadata = buildJsonObject { put("aronQuestion", json.parseToJsonElement(json.encodeToString(question))) }
        )
    }

    fun sendPoll(chatId: String, question: String, optionTexts: List<String>, isMultipleChoice: Boolean = false) {
        val currentTime = timeFormat.format(Date())
        val pollOptions = optionTexts.filter { it.isNotBlank() }.map { text ->
            PollOption(text = text.trim(), votesCount = 0, voterIds = emptyList())
        }

        val pollData = PollData(
            question = question.trim(),
            options = pollOptions,
            isMultipleChoice = isMultipleChoice
        )

        val pollMessage = Message(
            id = "local_${UUID.randomUUID()}",
            text = "📊 Sondage : $question",
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatar,
            timestamp = currentTime,
            status = MessageStatus.SENT,
            isMine = true,
            mediaType = MediaType.POLL,
            pollData = pollData
        )

        sendStructuredChatMessage(
            chatId = chatId,
            message = pollMessage,
            preview = "📊 Sondage : $question",
            apiMediaType = "POLL",
            metadata = buildJsonObject { put("pollData", json.parseToJsonElement(json.encodeToString(pollData))) }
        )
    }

    fun votePoll(chatId: String, messageId: String, optionId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            apiService.votePollApi(messageId, optionId)
                .onSuccess { refreshMessagesForChat(chatId) }
                .onFailure { _messagingError.value = it.message ?: "Le vote n’a pas pu être enregistré." }
        }
    }

    fun sendLocation(
        chatId: String,
        placeName: String,
        latitude: Double = -4.2634,
        longitude: Double = 15.2429,
        isLive: Boolean = false,
        durationMinutes: Int = 15
    ) {
        val currentTime = timeFormat.format(Date())
        val locData = LocationData(
            latitude = latitude,
            longitude = longitude,
            placeName = placeName,
            isLive = isLive,
            durationRemainingText = if (isLive) "$durationMinutes min" else null
        )

        val locMessage = Message(
            id = "local_${UUID.randomUUID()}",
            text = if (isLive) "📍 Position en direct partagée ($durationMinutes min)" else "📍 Lieu : $placeName",
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatar,
            timestamp = currentTime,
            status = MessageStatus.SENT,
            isMine = true,
            mediaType = MediaType.LOCATION,
            locationData = locData
        )

        sendStructuredChatMessage(
            chatId = chatId,
            message = locMessage,
            preview = locMessage.text,
            apiMediaType = "LOCATION",
            metadata = buildJsonObject { put("locationData", json.parseToJsonElement(json.encodeToString(locData))) }
        )
    }

    suspend fun sendPaymentTransfer(
        chatId: String,
        amount: String,
        provider: String,
        phone: String,
        note: String,
        isRequest: Boolean
    ): Result<String?> {
        val currentTime = timeFormat.format(Date())
        val amountFcfa = amount.filter(Char::isDigit).toLongOrNull()
            ?: return Result.failure(IllegalArgumentException("Montant invalide."))

        val paymentStatus: String
        val reference: String?
        var checkoutUrl: String? = null
        if (isRequest) {
            paymentStatus = "REQUESTED"
            reference = null
        } else {
            if (phone.count(Char::isDigit) < 8) {
                return Result.failure(IllegalArgumentException("Numéro Mobile Money invalide."))
            }
            val intent = apiService.createPaymentIntent(provider, amountFcfa, phone, note).getOrElse {
                return Result.failure(it)
            }
            paymentStatus = intent.status
            reference = intent.id
            checkoutUrl = intent.checkoutUrl?.takeIf { intent.checkoutRequired || intent.status.equals("PENDING", ignoreCase = true) }
        }

        val paymentData = PaymentTransferData(
            amount = "$amountFcfa FCFA",
            provider = provider,
            note = listOfNotNull(note.takeIf { it.isNotBlank() }, reference?.let { "Réf. $it" }).joinToString(" • "),
            isRequest = isRequest,
            status = paymentStatus
        )
        val msgText = if (isRequest) {
            "💳 Demande de paiement : $amountFcfa FCFA ($provider)"
        } else {
            "💸 Paiement initié : $amountFcfa FCFA via $provider"
        }
        val payMessage = Message(
            id = "local_${UUID.randomUUID()}",
            text = msgText,
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatar,
            timestamp = currentTime,
            status = MessageStatus.SENT,
            isMine = true,
            mediaType = MediaType.PAYMENT,
            paymentData = paymentData
        )
        sendStructuredChatMessage(
            chatId = chatId,
            message = payMessage,
            preview = msgText,
            apiMediaType = "PAYMENT",
            metadata = buildJsonObject { put("paymentData", json.parseToJsonElement(json.encodeToString(paymentData))) }
        )
        return Result.success(checkoutUrl)
    }

    fun translateMessage(chatId: String, messageId: String, targetLanguage: String = "Lingala") {
        val message = _chats.value.firstOrNull { it.id == chatId }?.messages?.firstOrNull { it.id == messageId }
            ?: run {
                _messagingError.value = "Message introuvable pour la traduction."
                return
            }
        CoroutineScope(Dispatchers.IO).launch {
            apiService.translateTextApi(message.text, targetLanguage)
                .onSuccess { translated ->
                    _chats.update { chatList ->
                        chatList.map { chat ->
                            if (chat.id != chatId) chat else chat.copy(
                                messages = chat.messages.map { msg ->
                                    if (msg.id == messageId) msg.copy(translatedText = translated, targetLanguage = targetLanguage) else msg
                                }
                            )
                        }
                    }
                }
                .onFailure { _messagingError.value = it.message ?: "La traduction n’a pas pu être effectuée." }
        }
    }

    fun setChatWallpaper(chatId: String, colorHex: String?, imageUrl: String?) {
        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(
                        wallpaperColor = colorHex,
                        wallpaperImageUrl = imageUrl
                    )
                } else chat
            }
        }
    }

    fun setChatDisappearingTimer(chatId: String, durationSeconds: Int) {
        val currentTime = timeFormat.format(Date())
        val timerLabel = when (durationSeconds) {
            5 -> "5 secondes"
            3600 -> "1 heure"
            86400 -> "24 heures"
            604800 -> "7 jours"
            2592000 -> "30 jours"
            0 -> "Désactivé"
            else -> "$durationSeconds s"
        }

        val noticeText = if (durationSeconds > 0) {
            "⏱️ Vous avez activé les messages éphémères ($timerLabel). Les nouveaux messages disparaîtront après cette durée."
        } else {
            "⏱️ Messages éphémères désactivés. Les messages seront conservés."
        }

        val systemNoticeMessage = Message(
            text = noticeText,
            senderId = "system",
            senderName = "Système MBoté",
            senderAvatar = "",
            timestamp = currentTime,
            status = MessageStatus.READ,
            isMine = false,
            disappearingDurationSec = 0
        )

        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(
                        disappearingTimerSec = durationSeconds,
                        messages = chat.messages + systemNoticeMessage
                    )
                } else chat
            }
        }
    }

    suspend fun toggleLikeShortVideo(videoId: String): Result<Unit> = reactToShortVideo(videoId, "❤️")

    suspend fun reactToShortVideo(videoId: String, emoji: String): Result<Unit> {
        val response = apiService.toggleLikeShortVideoApi(videoId, true)
        if (response.isFailure) return Result.failure(response.exceptionOrNull()!!)
        val liked = response.getOrThrow()
        refreshShortsFromBackend()
        _shortVideos.update { videos ->
            videos.map { if (it.id == videoId) it.copy(userReaction = if (liked) emoji else null) else it }
        }
        return Result.success(Unit)
    }

    suspend fun toggleBookmarkShortVideo(videoId: String): Result<Unit> {
        val response = apiService.toggleShortBookmark(videoId)
        if (response.isFailure) return Result.failure(response.exceptionOrNull()!!)
        val (count, saved) = response.getOrThrow()
        _shortVideos.update { videos -> videos.map { if (it.id == videoId) it.copy(bookmarksCount = count, isBookmarked = saved) else it } }
        return Result.success(Unit)
    }

    suspend fun toggleFollowShortCreator(creatorId: String): Result<Unit> {
        val response = apiService.toggleShortFollow(creatorId)
        if (response.isFailure) return Result.failure(response.exceptionOrNull()!!)
        val followed = response.getOrThrow().second
        _shortVideos.update { videos -> videos.map { if (it.creatorId == creatorId) it.copy(isFollowing = followed) else it } }
        return Result.success(Unit)
    }

    suspend fun addShortVideoComment(videoId: String, text: String): Result<Unit> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Commentaire requis."))
        val newComment = ShortVideoComment(
            authorName = _userProfile.value.name,
            authorUsername = _userProfile.value.username,
            authorAvatar = _userProfile.value.avatar,
            text = text,
            timestamp = "À l'instant",
            likesCount = 0,
            isLiked = false
        )

        val response = apiService.addShortVideoCommentApi(videoId, newComment)
        if (response.isFailure) return Result.failure(response.exceptionOrNull()!!)
        return refreshShortComments(videoId)
    }

    fun toggleLikeShortComment(videoId: String, commentId: String) {
        _shortVideos.update { list ->
            list.map { v ->
                if (v.id == videoId) {
                    val updatedComments = v.comments.map { c ->
                        if (c.id == commentId) {
                            val newLiked = !c.isLiked
                            c.copy(
                                isLiked = newLiked,
                                likesCount = if (newLiked) c.likesCount + 1 else (c.likesCount - 1).coerceAtLeast(0)
                            )
                        } else c
                    }
                    v.copy(comments = updatedComments)
                } else v
            }
        }
    }

    suspend fun createShortVideo(
        context: android.content.Context,
        videoUri: android.net.Uri,
        durationSeconds: Int,
        caption: String,
        hashtags: List<String>,
        musicTitle: String,
        musicArtist: String,
        thumbnailUrl: String,
        location: String? = null,
        visibility: String = "public"
    ): Result<ShortVideo> {
        val uploaded = apiService.uploadPublicationVideo(context, videoUri, "short-videos")
        if (uploaded.isFailure) return Result.failure(uploaded.exceptionOrNull()!!)
        val user = _userProfile.value
        val newShort = ShortVideo(
            creatorId = user.id,
            creatorName = user.name,
            creatorUsername = user.username,
            creatorAvatar = user.avatar,
            isCreatorVerified = user.isVerified,
            isFollowing = true,
            videoThumbnailUrl = thumbnailUrl,
            videoPlaybackUrl = uploaded.getOrThrow(),
            caption = caption,
            hashtags = hashtags,
            musicTitle = musicTitle.ifBlank { "Son original • ${user.name}" },
            musicArtist = musicArtist.ifBlank { user.name },
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            sharesCount = 0,
            bookmarksCount = 0,
            isBookmarked = false,
            durationFormatted = "%d:%02d".format(durationSeconds / 60, durationSeconds % 60),
            location = location,
            timestamp = "À l'instant",
            comments = emptyList()
        )
        val created = apiService.createShortVideoApi(newShort, visibility)
        if (created.isFailure) return Result.failure(created.exceptionOrNull()!!)
        val published = created.getOrThrow()
        _shortVideos.update { listOf(published) + it.filterNot { video -> video.id == published.id } }
        return Result.success(published)
    }

    suspend fun shareShortVideoToChat(chatId: String, shortVideo: ShortVideo): Result<Unit> {
        val shared = apiService.shareShortVideo(shortVideo.id, chatId)
        if (shared.isFailure) return Result.failure(shared.exceptionOrNull()!!)
        val shareMessage = "🎬 MBoté Shorts de ${shortVideo.creatorName} (${shortVideo.creatorUsername}) :\n\"${shortVideo.caption}\"\n🎵 ${shortVideo.musicTitle} #MBoteShorts"
        sendMessage(chatId, shareMessage)
        _shortVideos.update { list ->
            list.map { v ->
                if (v.id == shortVideo.id) v.copy(sharesCount = shared.getOrThrow()) else v
            }
        }
        return Result.success(Unit)
    }

    suspend fun refreshShortComments(videoId: String): Result<Unit> {
        val result = apiService.fetchShortVideoComments(videoId)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        val comments = result.getOrThrow()
        _shortVideos.update { videos ->
            videos.map { if (it.id == videoId) it.copy(comments = comments, commentsCount = comments.size) else it }
        }
        return Result.success(Unit)
    }

    suspend fun markShortViewed(videoId: String): Result<Unit> = apiService.markShortViewed(videoId)

    fun tipCreator(videoId: String, amountFcfa: Long, provider: String = "MBoté Pay / MTN MoMo") {
        // The payment backend must confirm a tip before any balance changes.
    }

    fun buyGiftBundle(bundle: GiftBundle, provider: String = "MBoté Pay / MTN MoMo"): Boolean {
        return false
    }

    private fun paymentProvider(providerLabel: String): Result<String> = when {
        providerLabel.contains("MTN", ignoreCase = true) -> Result.success("mtn")
        providerLabel.contains("Airtel", ignoreCase = true) -> Result.success("airtel")
        else -> Result.failure(IllegalArgumentException("Ce moyen de paiement n’est pas encore pris en charge."))
    }

    private fun paymentPhone(): Result<String> {
        val phone = _userProfile.value.phone.filter(Char::isDigit)
        return if (phone.length in 8..15) Result.success(phone)
        else Result.failure(IllegalStateException("Ajoutez un numéro Mobile Money valide à votre profil."))
    }

    suspend fun requestGiftPurchase(
        gift: GiftItem,
        count: Int,
        providerLabel: String
    ): Result<PaymentIntentResponse> {
        if (count !in 1..100) return Result.failure(IllegalArgumentException("Quantité invalide."))
        val provider = paymentProvider(providerLabel).getOrElse { return Result.failure(it) }
        val phone = paymentPhone().getOrElse { return Result.failure(it) }
        return apiService.createPaymentIntent(
            provider = provider,
            amountFcfa = 0L,
            phone = phone,
            purpose = "GIFT_PURCHASE",
            giftId = gift.id,
            quantity = count
        )
    }

    suspend fun requestBadgePurchase(
        badgeType: BadgeType,
        providerLabel: String
    ): Result<PaymentIntentResponse> {
        val provider = paymentProvider(providerLabel).getOrElse { return Result.failure(it) }
        val phone = paymentPhone().getOrElse { return Result.failure(it) }
        return apiService.createPaymentIntent(
            provider = provider,
            amountFcfa = 0L,
            phone = phone,
            purpose = "BADGE_PURCHASE",
            badgeId = badgeType.id
        )
    }

    suspend fun requestWalletTopUp(
        amountFcfa: Long,
        providerLabel: String
    ): Result<PaymentIntentResponse> {
        if (amountFcfa <= 0) return Result.failure(IllegalArgumentException("Montant invalide."))
        val provider = paymentProvider(providerLabel).getOrElse { return Result.failure(it) }
        val phone = paymentPhone().getOrElse { return Result.failure(it) }
        return apiService.createPaymentIntent(
            provider = provider,
            amountFcfa = amountFcfa,
            phone = phone,
            purpose = "WALLET_TOPUP"
        )
    }

    suspend fun refreshPaymentAndSync(intentId: String): Result<PaymentIntentResponse> {
        val result = apiService.fetchPaymentIntent(intentId)
        val intent = result.getOrNull()
        if (intent != null && (intent.fulfilled || !intent.status.equals("PENDING", ignoreCase = true))) {
            syncAllFromBackend()
        }
        return result
    }

    fun buySingleGift(gift: GiftItem, count: Int = 1, provider: String = "MTN Mobile Money"): Boolean = false

    fun buyBadge(badgeType: BadgeType, provider: String = "MTN Mobile Money"): Boolean = false

    fun updateGiftPrice(giftId: String, newPriceFcfa: Long) {
        _userGiftState.update { current ->
            val updatedGifts = current.storeGifts.map { gift ->
                if (gift.id == giftId) gift.copy(priceFcfa = newPriceFcfa) else gift
            }
            current.copy(storeGifts = updatedGifts)
        }
    }

    fun adminRestockGift(giftId: String, additionalCount: Int) {
        _userGiftState.update { current ->
            val updatedInventory = current.inventory.toMutableMap()
            updatedInventory[giftId] = (updatedInventory[giftId] ?: 0) + additionalCount
            current.copy(inventory = updatedInventory)
        }
    }

    fun togglePremiumStatus(isPremium: Boolean) {
        _userProfile.update { it.copy(isPremium = isPremium) }
    }

    fun sendGift(giftId: String, recipientId: String, multiplier: Int = 1): Boolean {
        if (giftId.isBlank() || recipientId.isBlank() || multiplier < 1) return false
        CoroutineScope(Dispatchers.IO).launch {
            apiService.sendGiftApi(giftId, recipientId, multiplier)
                .onSuccess { syncAllFromBackend() }
                .onFailure { _messagingError.value = it.message ?: "Le cadeau n’a pas pu être envoyé." }
        }
        return true
    }

    fun cashoutVirtualGifts(amountFcfa: Long, destinationProvider: String, phoneNumber: String): Boolean {
        if (amountFcfa <= 0 || destinationProvider.isBlank() || phoneNumber.isBlank()) return false
        CoroutineScope(Dispatchers.IO).launch {
            apiService.requestWalletWithdrawalApi(amountFcfa, destinationProvider, phoneNumber)
                .onSuccess { syncAllFromBackend() }
                .onFailure { _messagingError.value = it.message ?: "La demande de retrait a échoué." }
        }
        return true
    }

    fun updateWithdrawalStatus(withdrawalId: String, newStatus: WithdrawalStatus) {
        _userGiftState.update { current ->
            val updated = current.withdrawals.map {
                if (it.id == withdrawalId) it.copy(status = newStatus) else it
            }
            current.copy(withdrawals = updated)
        }
    }

    // Luna AI State Variables
    private var _scrollingMinutes = 34
    private var _screenLimitMinutes = 45

    fun getScrollingMinutes(): Int = _scrollingMinutes
    fun addScrollingMinutes(min: Int) {
        _scrollingMinutes += min
    }

    fun addReaction(chatId: String, messageId: String, emoji: String) {
        CoroutineScope(Dispatchers.IO).launch {
            apiService.toggleMessageReactionApi(messageId, emoji)
                .onSuccess { reactions ->
                    _chats.update { chatList ->
                        chatList.map { chat ->
                            if (chat.id == chatId) chat.copy(messages = chat.messages.map { message ->
                                if (message.id == messageId) message.copy(reactions = reactions) else message
                            }) else chat
                        }
                    }
                }
                .onFailure { _messagingError.value = it.message ?: "Réaction impossible." }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            apiService.deleteMessageApi(messageId)
                .onSuccess {
                    _chats.update { chatList ->
                        chatList.map { chat ->
                            if (chat.id == chatId) chat.copy(messages = chat.messages.map { message ->
                                if (message.id == messageId) message.copy(isRecalled = true, text = "Ce message a été supprimé") else message
                            }) else chat
                        }
                    }
                }
                .onFailure { _messagingError.value = it.message ?: "Suppression impossible." }
        }
    }

    fun markChatAsRead(chatId: String) {
        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id == chatId) chat.copy(unreadCount = 0) else chat
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            apiService.markChatReadApi(chatId)
                .onFailure { _messagingError.value = it.message ?: "Lecture non synchronisée." }
        }
    }

    suspend fun createDirectChatByUserId(userId: String, displayName: String, avatar: String = ""): Result<Chat> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("Utilisateur MBoté invalide."))
        _chats.value.firstOrNull { chat -> chat.participants.any { it.id == userId } && !chat.isGroup }?.let {
            return Result.success(it)
        }
        val dto = apiService.createDirectChatApi(userId).getOrElse { return Result.failure(it) }
        val chat = Chat(
            id = dto.id,
            name = dto.name.takeUnless { it.isBlank() || it == "Discussion" } ?: displayName,
            avatar = dto.avatar.ifBlank { avatar },
            lastMessage = dto.lastMessage,
            lastMessageTime = dto.lastMessageTime,
            unreadCount = dto.unreadCount,
            isOnline = dto.isOnline,
            isGroup = false,
            participants = dto.participants.map { Participant(it.id, it.name, it.avatar, it.isOnline, it.role) },
            disappearingTimerSec = dto.disappearingDurationSec
        )
        _chats.update { listOf(chat) + it.filterNot { current -> current.id == chat.id } }
        return Result.success(chat)
    }

    suspend fun createDirectChat(name: String, initialMessage: String): Result<Chat> {
        val contact = _mastaUsers.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Sélectionnez un utilisateur MBoté réel dans Masta."))
        val participantId = contact.id.takeIf(String::isNotBlank)
            ?: return Result.failure(IllegalArgumentException("Ce profil n’est pas relié au serveur MBoté."))
        val dto = apiService.createDirectChatApi(participantId).getOrElse { return Result.failure(it) }
        val participants = dto.participants.map {
            Participant(it.id, it.name, it.avatar, it.isOnline, it.role)
        }
        val chat = Chat(
            id = dto.id,
            name = dto.name.takeUnless { it.isBlank() || it == "Discussion" } ?: contact.name,
            avatar = dto.avatar.ifBlank { contact.avatar },
            lastMessage = dto.lastMessage,
            lastMessageTime = dto.lastMessageTime,
            unreadCount = dto.unreadCount,
            isOnline = dto.isOnline,
            isGroup = false,
            participants = participants,
            disappearingTimerSec = dto.disappearingDurationSec
        )
        _chats.update { listOf(chat) + it.filterNot { current -> current.id == chat.id } }
        if (initialMessage.isNotBlank()) sendMessage(chat.id, initialMessage.trim())
        return Result.success(chat)
    }

    suspend fun createGroupApi(
        groupName: String,
        description: String,
        members: List<SyncedContact>,
        avatar: String? = null,
        initialMessage: String = ""
    ): Result<Chat> {
        val participantIds = members.map { it.id }.filter(String::isNotBlank).distinct()
        if (members.isNotEmpty() && participantIds.size != members.size) {
            return Result.failure(IllegalArgumentException("Sélectionnez des contacts MBoté synchronisés avec le serveur."))
        }
        val createdId = apiService.createGroupApi(groupName, participantIds).getOrElse {
            return Result.failure(it)
        }
        val participantList = members.map { contact ->
            Participant(
                id = contact.id,
                name = contact.name,
                avatar = contact.avatarUrl.orEmpty(),
                role = "Membre"
            )
        } + Participant(
            id = _userProfile.value.id,
            name = _userProfile.value.name,
            avatar = _userProfile.value.avatar,
            role = "Admin"
        )

        val groupAvatar = avatar.orEmpty()

        val newGroup = Chat(
            id = createdId,
            name = groupName,
            avatar = groupAvatar,
            lastMessage = "",
            lastMessageTime = "",
            isGroup = true,
            isChannel = false,
            participants = participantList,
            messages = emptyList()
        )
        _chats.update { listOf(newGroup) + it }
        return Result.success(newGroup)
    }

    suspend fun createChannelApi(
        channelName: String,
        description: String,
        isPublic: Boolean,
        initialMessage: String = ""
    ): Result<String> {
        val channelId = apiService.createChannelApi(channelName, description, isPublic, initialMessage).getOrElse {
            return Result.failure(it)
        }
        val newChannel = Chat(
            id = channelId,
            name = channelName,
            avatar = "",
            lastMessage = initialMessage,
            lastMessageTime = "À l'instant",
            isGroup = false,
            isChannel = true,
            isVerified = true,
            participants = listOf(
                Participant(
                    id = _userProfile.value.id,
                    name = _userProfile.value.name,
                    avatar = _userProfile.value.avatar,
                    role = "Propriétaire"
                )
            ),
            messages = listOf(
                Message(
                    text = initialMessage,
                    senderId = _userProfile.value.id,
                    senderName = _userProfile.value.name,
                    timestamp = timeFormat.format(Date()),
                    status = MessageStatus.SENT,
                    isMine = true
                )
            )
        )
        _chats.update { listOf(newChannel) + it }
        refreshChannels()
        return Result.success(channelId)
    }

    suspend fun refreshChannels(): Result<Unit> {
        val result = apiService.fetchChannels()
        result.getOrNull()?.let { _channels.value = it }
        return result.map { Unit }
    }

    suspend fun setChannelSubscription(channelId: String, subscribe: Boolean): Result<Unit> {
        val result = apiService.setChannelSubscription(channelId, subscribe)
        if (result.isSuccess) refreshChannels()
        return result
    }

    suspend fun addStatus(
        text: String,
        mediaDataUrl: String? = null,
        mediaType: String = "text",
        background: String? = null,
        visibility: String = "friends"
    ): Result<StatusItem> {
        val normalizedType = mediaType.trim().lowercase().let { raw ->
            when {
                raw in setOf("text", "texte") -> "text"
                raw in setOf("image", "photo") || raw.startsWith("image/") -> "image"
                raw in setOf("audio", "voice", "vocal") || raw.startsWith("audio/") -> "audio"
                raw in setOf("video", "vidéo") || raw.startsWith("video/") -> "video"
                else -> "text"
            }
        }
        val content = mediaDataUrl?.takeIf(String::isNotBlank) ?: text.trim()
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("Le statut doit contenir du texte ou un média."))
        }
        val request = CreateStatusRequest(
            type = normalizedType,
            content = content,
            background = background,
            visibility = visibility,
            caption = text.trim().takeIf(String::isNotBlank)
        )
        val result = publicationApiService.createStatus(request, _userProfile.value.id)
        result.onSuccess { created -> _statuses.update { listOf(created) + it.filterNot { status -> status.id == created.id } } }
        return result
    }

    suspend fun addStatusFromDevice(
        context: android.content.Context,
        text: String,
        mediaUri: android.net.Uri?,
        mediaType: String,
        background: String? = null,
        visibility: String = "friends"
    ): Result<StatusItem> {
        val dataUrl = if (mediaUri != null && mediaType != "text") {
            val inlineLimit = if (mediaType == "audio") maxInlineStatusAudioBytes else maxInlineStatusImageBytes
            contentUriToDataUrl(context, mediaUri, inlineLimit).getOrElse { return Result.failure(it) }
        } else null
        return addStatus(text, dataUrl, mediaType, background, visibility)
    }

    suspend fun markStatusViewed(statusId: String): Result<Unit> = publicationApiService.markStatusViewed(statusId)

    suspend fun toggleNewsLike(postId: String): Result<Unit> {
        val result = publicationApiService.reactToActusPost(postId, "❤️")
        if (result.isSuccess) publicationApiService.fetchActusPosts().onSuccess { _newsPosts.value = it }
        return result
    }

    suspend fun shareNewsPost(postId: String): Result<Unit> {
        val result = publicationApiService.shareActusPost(postId)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        _newsPosts.update { posts -> posts.map { if (it.id == postId) it.copy(sharesCount = result.getOrThrow()) else it } }
        return Result.success(Unit)
    }

    suspend fun publishPostApi(
        title: String,
        content: String,
        mediaUrl: String? = null,
        category: String = "Communauté",
        mediaType: String = "text",
        durationSeconds: Int? = null
    ): Result<NewsPost> {
        val description = listOf(title.trim(), content.trim(), category.takeIf(String::isNotBlank)?.let { "Catégorie : $it" }).filterNotNull().filter(String::isNotBlank).joinToString("\n")
        val type = mediaType.lowercase().takeIf { it in setOf("text", "image", "audio", "video") } ?: "text"
        val request = CreateActusPostRequest(
            type = type,
            content = if (type == "text") description else mediaUrl.orEmpty(),
            thumbnail = if (type == "text") null else description,
            durationSeconds = durationSeconds
        )
        if (request.content.isBlank()) return Result.failure(IllegalArgumentException("Contenu de publication requis."))
        val result = publicationApiService.createActusPost(request)
        result.onSuccess { created -> _newsPosts.update { listOf(created) + it.filterNot { post -> post.id == created.id } } }
        return result
    }

    suspend fun publishPostFromDevice(
        context: android.content.Context,
        title: String,
        content: String,
        mediaUri: android.net.Uri?,
        category: String,
        mediaType: String
    ): Result<NewsPost> {
        val media = when {
            mediaUri == null || mediaType == "text" -> null
            mediaType == "video" -> apiService.uploadPublicationVideo(context, mediaUri, "actus-videos").getOrElse { return Result.failure(it) }
            else -> contentUriToDataUrl(context, mediaUri, maxInlineActusImageBytes).getOrElse { return Result.failure(it) }
        }
        return publishPostApi(title, content, media, category, mediaType)
    }

    private suspend fun contentUriToDataUrl(
        context: android.content.Context,
        uri: android.net.Uri,
        maxBytes: Int
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: throw IllegalArgumentException("Type de média Android inconnu.")
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) throw IllegalArgumentException("Média trop volumineux (maximum ${maxBytes / 1024 / 1024} Mo).")
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            } ?: throw IllegalArgumentException("Média Android inaccessible.")
            "data:$mime;base64,${android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)}"
        }
    }

    suspend fun createDirectRtcCall(
        peerUserId: String,
        peerName: String,
        peerAvatar: String,
        isVideo: Boolean
    ): Result<CallItem> {
        if (peerUserId.isBlank()) return Result.failure(IllegalArgumentException("Destinataire d’appel invalide."))
        val request = CreateGroupCallRequest(
            roomTitle = if (isVideo) "Appel vidéo avec $peerName" else "Appel audio avec $peerName",
            isVideoCall = isVideo,
            participantIds = listOf(peerUserId)
        )
        return groupCallApiService.createGroupCall(request).map { session ->
            CallItem(
                name = peerName,
                avatar = peerAvatar,
                type = CallType.OUTGOING,
                isVideo = isVideo,
                timestamp = "Connexion…",
                durationText = "",
                peerUserId = peerUserId,
                roomCode = session.roomCode,
                callState = "RINGING"
            )
        }
    }

    suspend fun joinDirectRtcCall(
        roomCode: String,
        callerUserId: String,
        callerName: String,
        callerAvatar: String,
        isVideo: Boolean
    ): Result<CallItem> =
        groupCallApiService.joinGroupCall(roomCode).map { session ->
            CallItem(
                name = callerName,
                avatar = callerAvatar,
                type = CallType.INCOMING,
                isVideo = isVideo,
                timestamp = "Connecté",
                durationText = "",
                peerUserId = callerUserId,
                roomCode = session.roomCode,
                callState = "CONNECTED"
            )
        }

    suspend fun leaveRtcCall(roomCode: String): Result<Boolean> =
        groupCallApiService.leaveGroupCall(roomCode)

    suspend fun updateRtcParticipantState(
        roomCode: String,
        isMuted: Boolean,
        isVideoOff: Boolean
    ): Result<Boolean> = groupCallApiService.updateParticipantState(
        ParticipantStateUpdateRequest(
            roomCode = roomCode,
            userId = _userProfile.value.id,
            isMuted = isMuted,
            isVideoOff = isVideoOff
        )
    )

    suspend fun createGroupCallRoomApi(roomTitle: String, isVideo: Boolean = true): Result<MeetingItem> {
        val req = CreateGroupCallRequest(roomTitle = roomTitle, isVideoCall = isVideo)
        val res = groupCallApiService.createGroupCall(req)
        return if (res.isSuccess) {
            val session = res.getOrNull()!!
            val meeting = MeetingItem(
                title = session.roomTitle,
                hostName = _userProfile.value.name,
                code = session.roomCode,
                scheduledTime = "En cours",
                durationMinutes = 60,
                isLive = true,
                participantsCount = session.participants.size
            )
            _meetings.update { listOf(meeting) + it }
            Result.success(meeting)
        } else {
            Result.failure(res.exceptionOrNull() ?: IllegalStateException("Création de la réunion impossible."))
        }
    }

    suspend fun joinGroupCallRoomApi(roomCode: String): Result<MeetingItem> {
        val res = groupCallApiService.joinGroupCall(roomCode)
        return if (res.isSuccess) {
            val session = res.getOrNull()!!
            val meeting = MeetingItem(
                title = session.roomTitle,
                hostName = session.participants.firstOrNull { it.id == session.hostUserId }?.name ?: "Organisateur",
                code = session.roomCode,
                scheduledTime = "En cours",
                durationMinutes = 60,
                isLive = true,
                participantsCount = session.participants.size
            )
            _meetings.update { listOf(meeting) + it }
            Result.success(meeting)
        } else {
            Result.failure(res.exceptionOrNull() ?: IllegalStateException("Réunion introuvable ou serveur indisponible."))
        }
    }

    suspend fun addNewsComment(postId: String, text: String): Result<Unit> {
        val result = publicationApiService.addActusComment(postId, text)
        if (result.isFailure) return result
        val comments = publicationApiService.fetchActusComments(postId)
        comments.onSuccess { loaded ->
            _newsPosts.update { posts -> posts.map { if (it.id == postId) it.copy(comments = loaded, commentsCount = loaded.size) else it } }
        }
        return Result.success(Unit)
    }

    suspend fun toggleJobLike(jobId: String): Result<Unit> {
        val result = publicationApiService.toggleJobLike(jobId)
        if (result.isSuccess) refreshJobs()
        return result
    }

    suspend fun toggleJobBookmark(jobId: String): Result<Unit> {
        val result = publicationApiService.toggleJobBookmark(jobId)
        if (result.isSuccess) refreshJobs()
        return result
    }

    suspend fun refreshJobs(): Result<Unit> = publicationApiService.fetchJobs()
        .onSuccess { _jobs.value = it }.map { Unit }

    suspend fun applyToJob(jobId: String, cvUrl: String = ""): Result<Unit> = publicationApiService.applyToJob(jobId, cvUrl)

    suspend fun postJobOffer(
        title: String,
        company: String,
        location: String,
        domain: String,
        contractType: String,
        workMode: String,
        salary: String,
        description: String,
        requirements: List<String> = emptyList(),
        benefits: List<String> = emptyList()
    ): Result<JobOffer> {
        val details = listOf(description, workMode,
            requirements.joinToString(", "), benefits.joinToString(", "))
            .filter(String::isNotBlank).joinToString("\n")
        if (title.isBlank() || company.isBlank() || location.isBlank() || description.isBlank()) {
            return Result.failure(IllegalArgumentException("Titre, entreprise, lieu et description sont obligatoires."))
        }
        return publicationApiService.createJob(mapOf(
            "title" to title.trim(), "company" to company.trim(), "location" to location.trim(),
            "activityDomain" to domain.trim().ifBlank { "Autre" },
            "type" to contractType.trim().ifBlank { "CDI" },
            "duration" to contractType.trim().ifBlank { "CDI" },
            "workMode" to workMode.trim().ifBlank { "Sur site" },
            "salary" to salary.trim(), "description" to details,
            "applyEmail" to _userProfile.value.email.trim()
        )).onSuccess { newJob -> _jobs.update { listOf(newJob) + it.filterNot { job -> job.id == newJob.id } } }
    }

    fun updateUserProfile(name: String, bio: String, phone: String, city: String) {
        _userProfile.update {
            it.copy(
                name = name,
                bio = bio,
                phone = phone,
                city = city
            )
        }
        saveCachedUserProfile()
        syncProfileToBackend()
        syncSettingsToBackend()
    }

    fun setThemeMode(mode: AppThemeMode) {
        _userProfile.update {
            it.copy(
                themeMode = mode,
                darkModeEnabled = mode == AppThemeMode.DARK
            )
        }
        saveCachedUserProfile()
        syncSettingsToBackend()
    }

    fun toggleDarkMode() {
        _userProfile.update {
            val newDark = !it.darkModeEnabled
            it.copy(
                darkModeEnabled = newDark,
                themeMode = if (newDark) AppThemeMode.DARK else AppThemeMode.LIGHT
            )
        }
        saveCachedUserProfile()
        syncSettingsToBackend()
    }

    fun toggleNotifications() {
        _userProfile.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }
        saveCachedUserProfile()
        syncSettingsToBackend()
    }

    private fun createInitialChats(): List<Chat> = emptyList()

    private fun isDemoChat(chat: Chat): Boolean {
        val demoIds = setOf("chat_luna", "chat_tech_hub", "chat_grace", "chat_canal_officiel", "chat_yannick")
        val demoNames = setOf(
            "Luna AI - MBoté Assistant",
            "Tech Hub Brazzaville 🇨🇬",
            "Grace Makiese",
            "MBoté Actualités Officielles",
            "Yannick Nguesso"
        )
        return chat.id in demoIds || chat.name in demoNames || chat.messages.any { it.id.startsWith("m_th_") || it.id.startsWith("m_g_") || it.id.startsWith("m_y_") || it.id.startsWith("m_ai_") }
    }

    private fun createInitialCalls(): List<CallItem> = emptyList()

    private fun createInitialMeetings(): List<MeetingItem> = emptyList()


    // --- Notification System Methods ---

    fun addNotification(notification: MboteNotification) {
        _notifications.update { listOf(notification) + it }
    }

    fun markNotificationAsRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    fun markAllNotificationsAsRead() {
        _notifications.update { list ->
            list.map { it.copy(isRead = true) }
        }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    private fun createInitialNotifications(): List<MboteNotification> = emptyList()

    fun updateLanguage(language: AppLanguage) {
        _userProfile.update { it.copy(language = language) }
        saveCachedUserProfile()
        syncSettingsToBackend()
    }

    fun updateCurrency(currency: AppCurrency) {
        _userProfile.update { it.copy(currency = currency) }
        saveCachedUserProfile()
        syncSettingsToBackend()
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        _userProfile.update { it.copy(themeMode = themeMode) }
        saveCachedUserProfile()
        syncSettingsToBackend()
    }

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        saveCachedUserProfile()
        syncProfileToBackend()
        syncSettingsToBackend()
    }

    private fun syncProfileToBackend() {
        if (MboteBackendConfig.authToken.isNullOrBlank()) return
        val profile = _userProfile.value
        CoroutineScope(Dispatchers.IO).launch {
            apiService.updateMyProfile(
                ProfileUpdateRequest(
                    name = profile.name.takeIf { it.isNotBlank() },
                    email = profile.email.takeIf { it.isNotBlank() },
                    bio = profile.bio,
                    avatar = profile.avatar.takeIf { it.isNotBlank() },
                    coverUrl = profile.coverUrl.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    private fun syncSettingsToBackend() {
        if (MboteBackendConfig.authToken.isNullOrBlank()) return
        val profile = _userProfile.value
        val settings = buildJsonObject {
            put("notificationsEnabled", profile.notificationsEnabled)
            put("themeMode", profile.themeMode.name)
            put("darkModeEnabled", profile.darkModeEnabled)
            put("language", profile.language.name)
            put("currency", profile.currency.name)
            put("autoTranslateTo", profile.autoTranslateTo)
            put("e2eEncryptionEnabled", profile.e2eEncryptionEnabled)
            put("parentalControlActive", profile.parentalControlActive)
            put("parentEmail", profile.parentEmail)
            put("nightLockdownEnabled", profile.nightLockdownEnabled)
            put("maxDailyScreenTimeMinutes", profile.maxDailyScreenTimeMinutes)
            put("commentCurfewHour", profile.commentCurfewHour)
            put("schoolHoursRestrictionEnabled", profile.schoolHoursRestrictionEnabled)
            put("isChildAccountLinkedByQrScan", profile.isChildAccountLinkedByQrScan)
        }
        CoroutineScope(Dispatchers.IO).launch {
            apiService.updateMySettings(settings)
        }
    }

    private var cacheDir: File? = null
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        coerceInputValues = true
    }

    fun initializeCache(filesDir: File) {
        this.cacheDir = filesDir
        loadCachedData()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            _chats.collect {
                saveCachedChats()
            }
        }
        scope.launch {
            _newsPosts.collect {
                saveCachedNews()
            }
        }
        scope.launch {
            _shortVideos.collect {
                saveCachedVideos()
            }
        }
    }

    private fun loadCachedData() {
        val dir = cacheDir ?: return

        try {
            val profileFile = File(dir, "mbote_cached_profile.json")
            if (profileFile.exists()) {
                val profileText = profileFile.readText()
                val cachedProfile = json.decodeFromString(UserProfile.serializer(), profileText)
                _userProfile.value = cachedProfile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val encryptedSessionFile = File(dir, "mbote_cached_session.enc")
            val legacySessionFile = File(dir, "mbote_cached_session.json")
            val sessionText = when {
                encryptedSessionFile.exists() -> SecureSessionCrypto.decrypt(encryptedSessionFile.readText())
                legacySessionFile.exists() -> legacySessionFile.readText()
                else -> null
            }
            if (!sessionText.isNullOrBlank()) {
                val cachedSession = json.decodeFromString(CachedAuthSession.serializer(), sessionText)
                if (cachedSession.authToken.isNotBlank()) {
                    MboteBackendConfig.authToken = cachedSession.authToken
                    MboteBackendConfig.refreshToken = cachedSession.refreshToken
                    _userProfile.value = cachedSession.userProfile
                    _isAuthenticated.value = true
                    if (legacySessionFile.exists()) {
                        encryptedSessionFile.writeText(SecureSessionCrypto.encrypt(sessionText))
                        legacySessionFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            File(dir, "mbote_cached_session.enc").delete()
            File(dir, "mbote_cached_session.json").delete()
            MboteBackendConfig.authToken = null
            MboteBackendConfig.refreshToken = null
            _isAuthenticated.value = false
        }

        try {
            val chatsFile = File(dir, "mbote_cached_chats.json")
            if (chatsFile.exists()) {
                val chatsText = chatsFile.readText()
                val cachedChats = json.decodeFromString(ListSerializer(Chat.serializer()), chatsText)
                val realCachedChats = cachedChats.filterNot(::isDemoChat)
                if (realCachedChats.isNotEmpty()) {
                    _chats.value = realCachedChats
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val newsFile = File(dir, "mbote_cached_news.json")
            if (newsFile.exists()) {
                val newsText = newsFile.readText()
                val cachedNews = json.decodeFromString(ListSerializer(NewsPost.serializer()), newsText)
                if (cachedNews.isNotEmpty()) {
                    _newsPosts.value = cachedNews
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val videoFile = File(dir, "mbote_cached_videos.json")
            if (videoFile.exists()) {
                val videoText = videoFile.readText()
                val cachedVideos = json.decodeFromString(ListSerializer(ShortVideo.serializer()), videoText)
                if (cachedVideos.isNotEmpty()) {
                    _shortVideos.value = cachedVideos
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCachedUserProfile() {
        val dir = cacheDir ?: return
        try {
            val profileFile = File(dir, "mbote_cached_profile.json")
            val text = json.encodeToString(UserProfile.serializer(), _userProfile.value)
            profileFile.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCachedSession() {
        val dir = cacheDir ?: return
        val token = MboteBackendConfig.authToken?.takeIf { it.isNotBlank() } ?: return
        try {
            val sessionFile = File(dir, "mbote_cached_session.enc")
            val text = json.encodeToString(
                CachedAuthSession.serializer(),
                CachedAuthSession(
                    authToken = token,
                    refreshToken = MboteBackendConfig.refreshToken,
                    userProfile = _userProfile.value
                )
            )
            sessionFile.writeText(SecureSessionCrypto.encrypt(text))
            File(dir, "mbote_cached_session.json").delete()
            saveCachedUserProfile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearCachedSession() {
        val dir = cacheDir ?: return
        try {
            File(dir, "mbote_cached_session.enc").delete()
            File(dir, "mbote_cached_session.json").delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCachedChats() {
        val dir = cacheDir ?: return
        try {
            val chatsFile = File(dir, "mbote_cached_chats.json")
            val text = json.encodeToString(ListSerializer(Chat.serializer()), _chats.value)
            chatsFile.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCachedNews() {
        val dir = cacheDir ?: return
        try {
            val newsFile = File(dir, "mbote_cached_news.json")
            val text = json.encodeToString(ListSerializer(NewsPost.serializer()), _newsPosts.value)
            newsFile.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCachedVideos() {
        val dir = cacheDir ?: return
        try {
            val videoFile = File(dir, "mbote_cached_videos.json")
            val text = json.encodeToString(ListSerializer(ShortVideo.serializer()), _shortVideos.value)
            videoFile.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun performCloudBackup(context: android.content.Context, isAuto: Boolean = false): Result<com.loukatech.mbote.service.BackupMetaData> {
        val email = _userProfile.value.email.ifBlank { "m.loutala@gmail.com" }
        return com.loukatech.mbote.service.MboteCloudBackupManager.performCloudBackup(
            context = context,
            userEmail = email,
            chats = _chats.value,
            calls = _calls.value,
            isAuto = isAuto
        )
    }

    suspend fun restoreCloudBackup(context: android.content.Context): Result<com.loukatech.mbote.service.BackupRestoreResult> {
        val email = _userProfile.value.email.ifBlank { "m.loutala@gmail.com" }
        val result = com.loukatech.mbote.service.MboteCloudBackupManager.restoreCloudBackup(context, email)
        return if (result.isSuccess) {
            val (restoredChats, restoredCalls) = result.getOrNull()!!
            if (restoredChats.isNotEmpty()) {
                _chats.update { currentList ->
                    val restoredMap = restoredChats.associateBy { it.id }
                    val merged = currentList.map { localChat ->
                        restoredMap[localChat.id] ?: localChat
                    }
                    val newChats = restoredChats.filter { rc -> currentList.none { lc -> lc.id == rc.id } }
                    merged + newChats
                }
            }
            if (restoredCalls.isNotEmpty()) {
                _calls.update { currentList ->
                    val restoredMap = restoredCalls.associateBy { it.id }
                    val merged = currentList.map { localCall ->
                        restoredMap[localCall.id] ?: localCall
                    }
                    val newCalls = restoredCalls.filter { rc -> currentList.none { lc -> lc.id == rc.id } }
                    merged + newCalls
                }
            }
            val totalMessages = restoredChats.sumOf { it.messages.size }
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.getDefault())
            Result.success(
                com.loukatech.mbote.service.BackupRestoreResult(
                    chatsCount = restoredChats.size,
                    messagesCount = totalMessages,
                    callsCount = restoredCalls.size,
                    timestamp = dateFormat.format(java.util.Date()),
                    backupId = "restore_" + System.currentTimeMillis()
                )
            )
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Échec de la restauration cloud"))
        }
    }

    fun checkAndRunAutoBackup(context: android.content.Context) {
        if (com.loukatech.mbote.service.MboteCloudBackupManager.isAutoBackupDue(context)) {
            val (enabled, _, wifiOnly) = com.loukatech.mbote.service.MboteCloudBackupManager.getAutoBackupPreferences(context)
            if (enabled && com.loukatech.mbote.service.MboteCloudBackupManager.isNetworkSuitable(context, wifiOnly)) {
                CoroutineScope(Dispatchers.IO).launch {
                    performCloudBackup(context, isAuto = true)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MboteRepository? = null

        fun getInstance(): MboteRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MboteRepository().also { INSTANCE = it }
            }
        }
    }
}
