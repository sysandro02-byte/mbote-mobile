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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MboteRepository(
    val apiService: MboteApiService = MboteApiService(),
    val publicationApiService: PublicationApiService = PublicationApiService(),
    val groupCallApiService: GroupCallApiService = GroupCallApiService()
) {

    init {
        INSTANCE = this
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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
        _isAuthenticated.value = true
        Result.success(Unit)
    } else Result.failure(result.exceptionOrNull() ?: Exception("Échec de l'authentification"))

    suspend fun requestPasswordReset(email: String): Result<String> {
        return apiService.requestForgotPassword(email)
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPass: String): Result<Boolean> {
        val supabaseProvider = com.loukatech.mbote.data.supabase.SupabaseServiceProvider()
        supabaseProvider.confirmPasswordReset(email, code, newPass)
        return apiService.confirmResetPassword(ResetPasswordConfirmRequest(email = email, resetCode = code, newPassword = newPass))
    }

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
        MboteBackendConfig.authToken = null
        _isAuthenticated.value = false
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

    private val _discoverProfiles = MutableStateFlow<List<DiscoverProfile>>(emptyList())
    val discoverProfiles: StateFlow<List<DiscoverProfile>> = _discoverProfiles.asStateFlow()

    private val _reports = MutableStateFlow<List<com.loukatech.mbote.model.ReportItem>>(emptyList())
    val reports: StateFlow<List<com.loukatech.mbote.model.ReportItem>> = _reports.asStateFlow()

    fun submitReport(type: String, targetName: String) {
        val newReport = com.loukatech.mbote.model.ReportItem(
            id = "rep_" + java.util.UUID.randomUUID().toString().take(6),
            type = type,
            targetName = targetName,
            reporterName = _userProfile.value.name,
            reason = "Signalement de contenu suspect ou inapproprié",
            status = "Envoyé à l'Admin",
            timestamp = "À l'instant"
        )
        _reports.update { listOf(newReport) + it }
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

    fun updateMastaUsers(users: List<MastaUser>) {
        _mastaUsers.value = users
    }

    fun blockContact(contactId: String) {
        _blockedContactIds.update { it + contactId }
    }

    fun unblockContact(contactId: String) {
        _blockedContactIds.update { it - contactId }
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

        // If chatting with AI, generate an instant response
        if (chat?.isAI == true) {
            triggerAiResponse(chatId, text)
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

        if (chat?.isAI == true) {
            triggerAiResponse(chatId, "Message vocal reçu")
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
        // Asynchronously fetch Call History from real REST API
        try {
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
        val result = apiService.fetchMastaUsers()
        if (result.isSuccess) {
            val remoteMasta = result.getOrNull()
            if (remoteMasta != null && remoteMasta.isNotEmpty()) {
                _mastaUsers.value = remoteMasta
            }
        }
    }

    suspend fun refreshShortsFromBackend() {
        val result = apiService.fetchShortVideos()
        if (result.isSuccess) {
            val remoteShorts = result.getOrNull()
            if (remoteShorts != null && remoteShorts.isNotEmpty()) {
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
        _messagingError.value = "Le vote de sondage attend l'endpoint backend MBoté dédié. Aucun vote local simulé n'a été enregistré."
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

    fun sendPaymentTransfer(
        chatId: String,
        amount: String,
        provider: String = "MTN MoMo",
        note: String = "Paiement MBoté",
        isRequest: Boolean = false
    ) {
        val currentTime = timeFormat.format(Date())
        val paymentData = PaymentTransferData(
            amount = amount,
            provider = provider,
            note = note,
            isRequest = isRequest,
            status = if (isRequest) "Demande en attente" else "Transfert réussi"
        )

        val msgText = if (isRequest) "💳 Demande de paiement : $amount ($provider)" else "💸 Transfert envoyé : $amount via $provider"
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
    }

    fun translateMessage(chatId: String, messageId: String, targetLanguage: String = "Lingala") {
        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id == chatId) {
                    val updatedMessages = chat.messages.map { msg ->
                        if (msg.id == messageId) {
                            val translated = getInstantTranslation(msg.text, targetLanguage)
                            msg.copy(
                                translatedText = translated,
                                targetLanguage = targetLanguage
                            )
                        } else msg
                    }
                    chat.copy(messages = updatedMessages)
                } else chat
            }
        }
    }

    private fun getInstantTranslation(text: String, targetLang: String): String {
        return when (targetLang.lowercase()) {
            "lingala" -> when {
                text.contains("bonjour", true) || text.contains("salut", true) -> "Mbote na yo ! Ozali malamu ?"
                text.contains("merci", true) -> "Matondi mingi !"
                text.contains("comment", true) -> "Ndenge nini ?"
                text.contains("réunion", true) -> "Likita ezali kobongisama malamu."
                text.contains("bienvenue", true) -> "Boyei malamu na MBoté !"
                else -> "Traduction Lingala : « $text » → [Maloba ya sika na Lingala ya pete]"
            }
            "français" -> when {
                text.contains("mbote", true) -> "Bonjour ! Comment allez-vous ?"
                text.contains("matondi", true) -> "Merci beaucoup !"
                text.contains("maloba", true) -> "Ces paroles sont pleines de sagesse."
                else -> "Traduction : « $text »"
            }
            "anglais" -> "English translation: \"$text\""
            else -> "[Traduit en $targetLang] : $text"
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

    fun getOrCreateChatForContact(contact: SyncedContact): String {
        val existing = _chats.value.find { it.name.equals(contact.name, ignoreCase = true) }
        if (existing != null) {
            return existing.id
        }

        val newChat = Chat(
            name = contact.name,
            avatar = contact.avatarUrl.orEmpty(),
            lastMessage = "Discussion chiffrée démarrée",
            lastMessageTime = "À l'instant",
            isOnline = contact.isMboteUser,
            messages = listOf(
                Message(
                    text = "Discussion chiffrée de bout en bout avec ${contact.name}.",
                    senderId = "system",
                    senderName = "Système MBoté",
                    timestamp = timeFormat.format(Date()),
                    status = MessageStatus.READ,
                    isMine = false
                )
            )
        )
        _chats.update { listOf(newChat) + it }
        return newChat.id
    }

    fun getOrCreateChatForProfile(profile: DiscoverProfile): String {
        val existing = _chats.value.find { it.name.equals(profile.name, ignoreCase = true) }
        if (existing != null) {
            return existing.id
        }

        val newChat = Chat(
            name = profile.name,
            avatar = profile.avatar,
            lastMessage = "Connexion humaine MBoté établie 👋",
            lastMessageTime = "À l'instant",
            isOnline = true,
            isVerified = true,
            messages = listOf(
                Message(
                    text = "Mbote ${profile.name} ! Ravi(e) de te découvrir sur le réseau MBoté. Vos échanges sont protégés par le chiffrement de bout en bout.",
                    senderId = "system",
                    senderName = "Connexion MBoté",
                    timestamp = timeFormat.format(Date()),
                    status = MessageStatus.READ,
                    isMine = false
                )
            )
        )
        _chats.update { listOf(newChat) + it }
        return newChat.id
    }

    fun sendMboteGreeting(profile: DiscoverProfile) {
        val chatId = getOrCreateChatForProfile(profile)
        sendMessage(chatId, "Mbote ${profile.name} ! 👋 J'ai adoré ton profil et tes centres d'intérêt (${profile.interests.take(2).joinToString(", ")}). Ravi(e) de faire ta connaissance !")
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

    fun getOrCreateChatForCreator(shortVideo: ShortVideo): String {
        val existingChat = _chats.value.find { it.name.equals(shortVideo.creatorName, ignoreCase = true) }
        if (existingChat != null) {
            return existingChat.id
        }

        val newChat = Chat(
            id = "chat_${shortVideo.creatorId}_${System.currentTimeMillis()}",
            name = shortVideo.creatorName,
            avatar = shortVideo.creatorAvatar,
            lastMessage = "Discussion démarrée via ShortMBoté ✨",
            lastMessageTime = timeFormat.format(Date()),
            unreadCount = 0,
            isOnline = true,
            isVerified = shortVideo.isCreatorVerified,
            messages = listOf(
                Message(
                    id = "msg_${System.currentTimeMillis()}",
                    text = "Mbote ! Merci d'avoir regardé mon Short sur MBoté ✨ Comment puis-je t'aider ?",
                    senderId = shortVideo.creatorId,
                    senderName = shortVideo.creatorName,
                    timestamp = timeFormat.format(Date()),
                    status = MessageStatus.READ,
                    isMine = false
                )
            )
        )

        _chats.update { list -> listOf(newChat) + list }
        return newChat.id
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
        location: String? = null
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
        val created = apiService.createShortVideoApi(newShort)
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

    suspend fun requestGiftPurchase(amountFcfa: Long, providerLabel: String): Result<PaymentIntentResponse> {
        val provider = when {
            providerLabel.contains("MTN", ignoreCase = true) -> "mtn_momo"
            providerLabel.contains("Airtel", ignoreCase = true) -> "airtel_money"
            else -> "mbote_pay"
        }
        val phone = _userProfile.value.phone.filter(Char::isDigit)
        if (phone.length !in 8..15) return Result.failure(IllegalStateException("Ajoutez un numéro Mobile Money valide à votre profil."))
        return apiService.createPaymentIntent(provider, amountFcfa, phone)
    }

    fun buySingleGift(gift: GiftItem, count: Int = 1, provider: String = "MBoté Pay / MTN MoMo"): Boolean {
        return false
    }

    fun buyBadge(badgeType: BadgeType, provider: String = "MTN Mobile Money"): Boolean {
        _userProfile.update { u ->
            val newBadges = if (u.badges.contains(badgeType)) u.badges else u.badges + badgeType
            val newWallet = if (provider.contains("MBoté", ignoreCase = true)) {
                (u.walletBalanceFcfa - badgeType.priceFcfa).coerceAtLeast(0L)
            } else {
                u.walletBalanceFcfa
            }
            u.copy(badges = newBadges, walletBalanceFcfa = newWallet)
        }
        // Admin receives the revenue
        _userGiftState.update { current ->
            current.copy(adminPlatformBadgeRevenueFcfa = current.adminPlatformBadgeRevenueFcfa + badgeType.priceFcfa)
        }
        return true
    }

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

    fun sendGift(giftId: String, recipientName: String, multiplier: Int = 1): Boolean {
        val currentState = _userGiftState.value
        val currentCount = currentState.inventory[giftId] ?: 0
        if (currentCount < multiplier) return false

        val giftItem = currentState.storeGifts.find { it.id == giftId } ?: defaultGiftItems().find { it.id == giftId } ?: return false
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val totalAmount = giftItem.priceFcfa * multiplier

        val newTransaction = GiftTransaction(
            giftId = giftId,
            giftName = if (multiplier > 1) "${giftItem.name} (x$multiplier)" else giftItem.name,
            emoji = giftItem.emoji,
            amountFcfa = totalAmount,
            isReceived = false,
            counterpartName = recipientName,
            timestamp = "Aujourd'hui à $timeNow",
            status = "Complété"
        )

        _userGiftState.update { current ->
            val updatedInventory = current.inventory.toMutableMap()
            updatedInventory[giftId] = (currentCount - multiplier).coerceAtLeast(0)
            current.copy(
                inventory = updatedInventory,
                transactions = listOf(newTransaction) + current.transactions
            )
        }

        _userProfile.update { u ->
            u.copy(totalGiftsSentFcfa = u.totalGiftsSentFcfa + totalAmount)
        }
        return true
    }

    fun cashoutVirtualGifts(amountFcfa: Long, destinationProvider: String, phoneNumber: String): Boolean {
        val currentState = _userGiftState.value
        if (currentState.totalVirtualEarnedFcfa < amountFcfa) return false

        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val isInstant = destinationProvider.contains("MBoté", ignoreCase = true)
        val newWithdrawal = WithdrawalTransaction(
            amountFcfa = amountFcfa,
            provider = destinationProvider,
            destinationAccount = phoneNumber,
            timestamp = "Aujourd'hui à $timeNow",
            status = if (isInstant) WithdrawalStatus.COMPLETED else WithdrawalStatus.PENDING
        )

        _userGiftState.update { current ->
            current.copy(
                totalVirtualEarnedFcfa = (current.totalVirtualEarnedFcfa - amountFcfa).coerceAtLeast(0L),
                withdrawals = listOf(newWithdrawal) + current.withdrawals,
                transactions = current.transactions.map {
                    if (it.isReceived && it.status == "Disponible") it.copy(status = "Encaissé ($destinationProvider)") else it
                }
            )
        }
        // Also credit user wallet if cashing out to MBoté Pay
        if (isInstant) {
            _userProfile.update { u ->
                u.copy(walletBalanceFcfa = u.walletBalanceFcfa + amountFcfa)
            }
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

    data class ScheduledMessage(
        val text: String,
        val destChatId: String,
        val destName: String,
        val delaySeconds: Long,
        val timestampScheduled: String
    )

    private val _scheduledMessages = MutableStateFlow<List<ScheduledMessage>>(emptyList())
    val scheduledMessages: StateFlow<List<ScheduledMessage>> = _scheduledMessages.asStateFlow()

    private fun triggerAiResponse(chatId: String, userPrompt: String) {
        val currentTime = timeFormat.format(Date())
        val isScheduling = userPrompt.contains("programme", ignoreCase = true) || 
                            userPrompt.contains("planifie", ignoreCase = true) || 
                            userPrompt.contains("différé", ignoreCase = true) || 
                            userPrompt.contains("envoyer après", ignoreCase = true)
        
        val isGiftsQuery = userPrompt.contains("cadeau", ignoreCase = true) || 
                            userPrompt.contains("reçu", ignoreCase = true) || 
                            userPrompt.contains("gain", ignoreCase = true)
        
        val isScreenTimeQuery = userPrompt.contains("scroll", ignoreCase = true) || 
                                 userPrompt.contains("temps", ignoreCase = true) || 
                                 userPrompt.contains("écran", ignoreCase = true) || 
                                 userPrompt.contains("minute", ignoreCase = true)

        val aiReplyText = when {
            isScheduling -> {
                // Parse message inside quotes or after "le message"
                val textMatch = Regex("['\"«]([^'\"»]+)['\"»]").find(userPrompt)
                val msgText = textMatch?.groupValues?.get(1) ?: run {
                    val index = userPrompt.lowercase().indexOf("message")
                    if (index != -1 && index + 8 < userPrompt.length) {
                        userPrompt.substring(index + 8).trim()
                    } else {
                        "Mboté ! Comment tu vas ?"
                    }
                }

                // Parse delay
                val secondsMatch = Regex("(\\d+)\\s*(seconde|secondes|sec)").find(userPrompt)
                val minutesMatch = Regex("(\\d+)\\s*(minute|minutes|min)").find(userPrompt)
                val delaySeconds = when {
                    secondsMatch != null -> secondsMatch.groupValues[1].toLong()
                    minutesMatch != null -> minutesMatch.groupValues[1].toLong() * 60L
                    else -> 10L // Default to 10 seconds for fun demoing
                }

                // Find recipient chat
                val chatsVal = _chats.value
                var targetChat = chatsVal.find { chat ->
                    chat.id != "chat_luna_ai" && (
                        userPrompt.contains(chat.name, ignoreCase = true) || 
                        chat.name.split(" ").any { part -> part.length > 2 && userPrompt.contains(part, ignoreCase = true) }
                    )
                }
                if (targetChat == null) {
                    targetChat = chatsVal.find { it.id != "chat_luna_ai" && it.id != chatId } ?: chatsVal.firstOrNull()
                }

                val targetChatId = targetChat?.id ?: chatId
                val targetChatName = targetChat?.name ?: "Mon masta"

                val schedMsg = ScheduledMessage(
                    text = msgText,
                    destChatId = targetChatId,
                    destName = targetChatName,
                    delaySeconds = delaySeconds,
                    timestampScheduled = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                )
                _scheduledMessages.update { it + schedMsg }

                // Launch delayed sending mechanism
                CoroutineScope(Dispatchers.Default).launch {
                    kotlinx.coroutines.delay(delaySeconds * 1000L)
                    sendMessage(targetChatId, msgText)
                    _scheduledMessages.update { list -> list.filter { it != schedMsg } }
                }

                val targetTimeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis() + delaySeconds * 1000L))
                "D'accord ! J'ai programmé votre message : **« $msgText »** pour **$targetChatName**.\nIl sera envoyé automatiquement dans **$delaySeconds secondes** (à $targetTimeStr). 🕒"
            }
            isGiftsQuery -> {
                val receivedTransactions = _userGiftState.value.transactions.filter { it.isReceived }
                val totalGains = _userGiftState.value.totalVirtualEarnedFcfa
                val giftsStr = if (receivedTransactions.isEmpty()) {
                    "Vous n'avez pas encore reçu de cadeaux sur vos directs ou vidéos."
                } else {
                    receivedTransactions.joinToString("\n") { tx ->
                        "• ${tx.emoji} **${tx.giftName}** reçu de *${tx.counterpartName}* (${tx.amountFcfa} FCFA) - ${tx.timestamp}"
                    }
                }
                "Voici un récapitulatif de vos cadeaux reçus 🎁 :\n\n$giftsStr\n\n💰 **Total de vos gains accumulés : $totalGains FCFA**\nVous pouvez transférer ce solde vers MTN MoMo, Airtel Money ou votre portefeuille MBoté à tout moment depuis les Paramètres !"
            }
            isScreenTimeQuery -> {
                val isLimitConfig = userPrompt.contains("limite", ignoreCase = true) && Regex("\\d+").containsMatchIn(userPrompt)
                if (isLimitConfig) {
                    val newLimit = Regex("\\d+").find(userPrompt)?.value?.toIntOrNull() ?: 45
                    _screenLimitMinutes = newLimit
                    "Parfait ! J'ai configuré votre limite de temps d'écran à **$newLimit minutes** pour aujourd'hui. Je vous alerterai dès que vous la dépasserez ! ⏳😊"
                } else {
                    "Vous avez passé **$_scrollingMinutes minutes** à scroller et naviguer sur MBoté aujourd'hui. 📱\n" +
                    "Votre limite d'utilisation recommandée est de **$_screenLimitMinutes minutes**.\n\n" +
                    "💡 *Conseil de Luna :* Faire des pauses de 5 minutes toutes les 30 minutes aide à garder l'esprit frais et reposé !"
                }
            }
            userPrompt.contains("aide", ignoreCase = true) || userPrompt.contains("faire", ignoreCase = true) || userPrompt.contains("fonction", ignoreCase = true) || (userPrompt.contains("luna", ignoreCase = true) && userPrompt.contains("quoi", ignoreCase = true)) ->
                "Je suis Luna, votre assistante IA MBoté ! Voici ce que je peux faire pour vous :\n\n" +
                "1️⃣ **Planifier des messages** 🕒 : Dites-moi par exemple *\"programme le message 'Salut mon masta' dans 10 secondes\"*.\n" +
                "2️⃣ **Rappeler vos cadeaux reçus** 🎁 : Demandez-moi *\"quels cadeaux j'ai reçus ?\"* ou *\"mes gains de cadeaux\"*.\n" +
                "3️⃣ **Suivre votre temps d'écran** ⏳ : Demandez-moi *\"combien de temps j'ai passé à scroller ?\"* ou *\"définis ma limite d'écran à 60 minutes\"*.\n" +
                "4️⃣ **Aide générale** 💡 : Je réponds à vos questions sur la sécurité, les transferts d'argent, les appels et plus encore !"
            userPrompt.contains("rappelle", ignoreCase = true) || userPrompt.contains("remind", ignoreCase = true) || userPrompt.contains("portefeuille", ignoreCase = true) || userPrompt.contains("wallet", ignoreCase = true) ->
                "⏰ **Rappel activé par Luna AI !**\n" +
                "J'ai bien enregistré votre rappel : *\"Vérifier mon portefeuille\"*.\n" +
                "Je vous enverrai une notification de rappel très bientôt pour ne pas oublier ! 💼💰"
            userPrompt.contains("aron", ignoreCase = true) || userPrompt.contains("question", ignoreCase = true) ->
                "Les 36 Questions d'Arthur Aron sont une méthode formidable pour créer des liens authentiques ! Ma réponse à cette question : ce que j'apprécie le plus, c'est d'aider chacun à communiquer librement et en toute sécurité."
            userPrompt.contains("bonjour", ignoreCase = true) || userPrompt.contains("mbote", ignoreCase = true) ->
                "Mbote ! Comment puis-je vous aider aujourd'hui sur MBoté ? Je peux vous renseigner sur les messages chiffrés, les questions d'Aron, les transferts d'argent ou les réunions."
            userPrompt.contains("sécurité", ignoreCase = true) || userPrompt.contains("chiffr", ignoreCase = true) ->
                "Sur MBoté, tous vos messages, appels, vocaux et positions sont sécurisés de bout en bout avec un chiffrement AES-256 / Signal protocol."
            userPrompt.contains("argent", ignoreCase = true) || userPrompt.contains("paiement", ignoreCase = true) ->
                "Vous pouvez envoyer des fonds via MTN MoMo, Airtel Money ou Orange Money directement dans vos discussions sans quitter MBoté !"
            userPrompt.contains("réunion", ignoreCase = true) ->
                "Vous pouvez démarrer une visioconférence instantanée ou planifier une réunion d'équipe dans l'onglet Réunions !"
            else ->
                "Merci pour votre message ! Je suis Luna, l'intelligence artificielle intégrée à MBoté. Je peux planifier des messages, résumer des discussions, rappeler vos cadeaux reçus, suivre votre temps d'utilisation ou vous guider sur l'application. Écrivez \"aide\" pour voir toutes mes capacités ! 🌟"
        }

        val aiMessage = Message(
            text = aiReplyText,
            senderId = "luna_ai",
            senderName = "Luna AI",
            senderAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
            timestamp = currentTime,
            status = MessageStatus.READ,
            isMine = false
        )

        _chats.update { chatList ->
            chatList.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(
                        lastMessage = aiReplyText,
                        lastMessageTime = currentTime,
                        messages = chat.messages + aiMessage
                    )
                } else chat
            }
        }
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

    suspend fun createDirectChat(name: String, initialMessage: String): Result<Chat> {
        val contact = _mastaUsers.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Sélectionnez un utilisateur MBoté réel dans Masta."))
        val participantId = contact.id.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Ce profil n’est pas encore relié au serveur MBoté."))
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
        val participantIds = members.mapNotNull { it.id.toIntOrNull() }.distinct()
        if (members.isNotEmpty() && participantIds.isEmpty()) {
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
        val content = mediaDataUrl?.takeIf(String::isNotBlank) ?: text.trim()
        val request = CreateStatusRequest(
            type = mediaType,
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
            contentUriToDataUrl(context, mediaUri, 12 * 1024 * 1024).getOrElse { return Result.failure(it) }
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
            else -> contentUriToDataUrl(context, mediaUri, 12 * 1024 * 1024).getOrElse { return Result.failure(it) }
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
            val fallback = createMeeting(roomTitle)
            Result.success(fallback)
        }
    }

    suspend fun joinGroupCallRoomApi(roomCode: String): Result<MeetingItem> {
        val res = groupCallApiService.joinGroupCall(roomCode)
        return if (res.isSuccess) {
            val session = res.getOrNull()!!
            val meeting = MeetingItem(
                title = session.roomTitle,
                hostName = "Organisateur",
                code = session.roomCode,
                scheduledTime = "En cours",
                durationMinutes = 60,
                isLive = true,
                participantsCount = session.participants.size
            )
            _meetings.update { listOf(meeting) + it }
            Result.success(meeting)
        } else {
            val meeting = MeetingItem(
                title = "Réunion Visioconférence #$roomCode",
                hostName = "MBoté Host",
                code = roomCode,
                scheduledTime = "En cours",
                durationMinutes = 45,
                isLive = true,
                participantsCount = 2
            )
            _meetings.update { listOf(meeting) + it }
            Result.success(meeting)
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

    fun createMeeting(title: String, durationMin: Int = 45): MeetingItem {
        val code = "MB-" + (100..999).random() + "-" + (100..999).random()
        val newMeeting = MeetingItem(
            title = title,
            hostName = _userProfile.value.name,
            code = code,
            scheduledTime = "En cours",
            durationMinutes = durationMin,
            isLive = true,
            participantsCount = 1
        )
        _meetings.update { listOf(newMeeting) + it }
        return newMeeting
    }

    fun toggleJobLike(jobId: String) {
        _jobs.update { jobList ->
            jobList.map { job ->
                if (job.id == jobId) {
                    val newLiked = !job.isLiked
                    val newCount = if (newLiked) job.likesCount + 1 else job.likesCount - 1
                    job.copy(isLiked = newLiked, likesCount = newCount)
                } else job
            }
        }
    }

    fun toggleJobBookmark(jobId: String) {
        _jobs.update { jobList ->
            jobList.map { job ->
                if (job.id == jobId) {
                    job.copy(isSaved = !job.isSaved)
                } else job
            }
        }
    }

    fun applyToJob(jobId: String): Boolean {
        var applied = false
        _jobs.update { jobList ->
            jobList.map { job ->
                if (job.id == jobId) {
                    applied = true
                    job.copy(applicantsCount = job.applicantsCount + 1)
                } else job
            }
        }
        return applied
    }

    fun postJobOffer(
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
    ): JobOffer {
        val newJob = JobOffer(
            title = title,
            company = company,
            location = location,
            domain = domain,
            contractType = contractType,
            duration = contractType,
            workMode = workMode,
            salary = if (salary.isBlank()) "Selon profil (FCFA)" else salary,
            description = description,
            requirements = requirements,
            benefits = benefits,
            postedDate = "À l'instant",
            deadline = "30 jours",
            applicantsCount = 1,
            likesCount = 1
        )
        _jobs.update { listOf(newJob) + it }
        return newJob
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
    }

    fun setThemeMode(mode: AppThemeMode) {
        _userProfile.update {
            it.copy(
                themeMode = mode,
                darkModeEnabled = mode == AppThemeMode.DARK
            )
        }
    }

    fun toggleDarkMode() {
        _userProfile.update {
            val newDark = !it.darkModeEnabled
            it.copy(
                darkModeEnabled = newDark,
                themeMode = if (newDark) AppThemeMode.DARK else AppThemeMode.LIGHT
            )
        }
    }

    fun toggleNotifications() {
        _userProfile.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }
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

    private fun createInitialCalls(): List<CallItem> {
        return listOf(
            CallItem(
                name = "Grace Makiese",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                type = CallType.INCOMING,
                isVideo = true,
                timestamp = "Aujourd'hui à 14:10",
                durationText = "12 min 30 s",
                phoneNumber = "+242 06 555 4321"
            ),
            CallItem(
                name = "Tech Hub Brazzaville",
                avatar = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150&auto=format&fit=crop&q=80",
                type = CallType.OUTGOING,
                isVideo = false,
                timestamp = "Hier à 18:45",
                durationText = "34 min 12 s",
                phoneNumber = "+242 05 777 8899"
            ),
            CallItem(
                name = "Grace Makiese",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                type = CallType.OUTGOING,
                isVideo = true,
                timestamp = "Hier à 11:20",
                durationText = "5 min 45 s",
                phoneNumber = "+242 06 555 4321"
            ),
            CallItem(
                name = "Aron Loutala",
                avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                type = CallType.INCOMING,
                isVideo = false,
                timestamp = "22 Août à 16:30",
                durationText = "8 min 14 s",
                phoneNumber = "+242 06 111 2233"
            ),
            CallItem(
                name = "Audrey Matondo",
                avatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                type = CallType.OUTGOING,
                isVideo = true,
                timestamp = "21 Août à 20:05",
                durationText = "19 min 02 s",
                phoneNumber = "+242 06 888 9900"
            ),
            CallItem(
                name = "Yannick Nguesso",
                avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                type = CallType.MISSED,
                isVideo = true,
                timestamp = "20 Août à 09:15",
                durationText = "Manqué",
                phoneNumber = "+242 06 444 3322"
            )
        )
    }

    private fun createInitialMeetings(): List<MeetingItem> {
        return listOf(
            MeetingItem(
                title = "Point Quotidien - Équipe Ingénierie",
                hostName = "Marc Loutala",
                code = "MB-2026-ENG",
                scheduledTime = "Aujourd'hui à 15:00",
                durationMinutes = 30,
                isLive = true,
                participantsCount = 6
            ),
            MeetingItem(
                title = "Revue de Conception Mobile & IA",
                hostName = "Grace Makiese",
                code = "MB-DESIGN-REV",
                scheduledTime = "Demain à 10:30",
                durationMinutes = 45,
                isLive = false,
                participantsCount = 4
            ),
            MeetingItem(
                title = "Partenariats Télécoms & Connectivité",
                hostName = "Yannick Nguesso",
                code = "MB-TELCO-PNT",
                scheduledTime = "Jeudi à 14:00",
                durationMinutes = 60,
                isLive = false,
                participantsCount = 8
            )
        )
    }

    private fun createInitialJobs(): List<JobOffer> {
        return listOf(
            JobOffer(
                id = "job_1",
                title = "Développeur Mobile Android Senior (Kotlin / Jetpack Compose)",
                company = "LoukaTech R&D",
                companyLogo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                location = "Brazzaville, Congo (Hybride)",
                type = "Temps plein",
                contractType = "CDI",
                workMode = "Hybride",
                experienceLevel = "Senior (4+ ans)",
                salary = "1 200 000 - 1 800 000 FCFA / mois",
                duration = "CDI",
                domain = "Ingénierie Logicielle",
                description = "Rejoignez l'équipe d'ingénierie centrale de MBoté pour concevoir, optimiser et déployer les fonctionnalités temps réel, le chiffrement de bout en bout et les expériences immersives sur Android.",
                requirements = listOf(
                    "Maîtrise avancée de Kotlin et Jetpack Compose (Clean Architecture, M3)",
                    "Expérience avec WebRTC, Coroutines / Flow et Room Database",
                    "Sens aigu de l'ergonomie, de l'accessibilité et de la fluidité à 60/120 FPS",
                    "Capacité à travailler en équipe agile et esprit d'innovation africaine"
                ),
                benefits = listOf(
                    "Assurance santé à 100% (salarié et famille)",
                    "Ordinateur portable pro dernière génération + budget équipement",
                    "Primes semestrielles de performance & intéressement",
                    "Horaires flexibles et 2 jours de télétravail par semaine"
                ),
                postedDate = "Il y a 2 h",
                deadline = "15 Octobre 2026",
                applicantsCount = 14,
                likesCount = 38,
                isLiked = true,
                isSaved = true
            ),
            JobOffer(
                id = "job_2",
                title = "Ingénieur Télécoms & Infrastructure WebRTC Temps Réel",
                company = "MBoté Networks & Cloud",
                companyLogo = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=150&auto=format&fit=crop&q=80",
                location = "Pointe-Noire, Congo",
                type = "Temps plein",
                contractType = "CDI",
                workMode = "Présentiel",
                experienceLevel = "Intermédiaire (2-4 ans)",
                salary = "950 000 - 1 400 000 FCFA / mois",
                duration = "CDI",
                domain = "Télécoms & Réseaux",
                description = "Optimisation des flux audio et vidéo peer-to-peer à travers les réseaux 3G/4G/5G africains. Gestion des serveurs STUN/TURN, SFU et résilience aux coupures réseau.",
                requirements = listOf(
                    "Connaissances solides des protocoles SIP, WebRTC, RTP/RTCP et Codecs Opus/VP8",
                    "Expérience Linux serveur, Docker et monitoring réseau",
                    "Diplôme d'Ingénieur en Télécoms ou Réseaux & Systèmes"
                ),
                benefits = listOf(
                    "Prise en charge forfait connexion haut débit",
                    "Couverture médicale complète",
                    "Plan de formation internationale certifiante"
                ),
                postedDate = "Aujourd'hui",
                deadline = "30 Septembre 2026",
                applicantsCount = 8,
                likesCount = 19
            ),
            JobOffer(
                id = "job_3",
                title = "Product Designer UI/UX Mobile & Web",
                company = "Studio Créatif Mbote",
                companyLogo = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                location = "Kinshasa / Télétravail complet",
                type = "Temps plein",
                contractType = "CDI",
                workMode = "Télétravail",
                experienceLevel = "Intermédiaire",
                salary = "800 000 - 1 250 000 FCFA / mois",
                duration = "CDI",
                domain = "Design & Ergonomie",
                description = "Création des interfaces utilisateur intuitives, de micro-interactions fluides et de design systems pour MBoté, MBoté Shorts et l'écosystème pro.",
                requirements = listOf(
                    "Excellente maîtrise de Figma, Material Design 3 et design tokens",
                    "Portfolio mobile démontrant une attention rigoureuse aux détails",
                    "Sensibilité pour l'accessibilité et la diversité culturelle"
                ),
                benefits = listOf(
                    "Télétravail 100% avec indemnité d'installation",
                    "Abonnements outils de design et bibliothèques d'assets",
                    "Participation aux événements tech continentaux"
                ),
                postedDate = "Il y a 1 jour",
                deadline = "20 Octobre 2026",
                applicantsCount = 22,
                likesCount = 45
            ),
            JobOffer(
                id = "job_4",
                title = "Responsable Partenariats Mobile Money & Fintech",
                company = "MBoté Pay Solutions",
                companyLogo = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80",
                location = "Brazzaville, Congo",
                type = "Temps plein",
                contractType = "CDI",
                workMode = "Présentiel",
                experienceLevel = "Senior (5+ ans)",
                salary = "1 100 000 - 1 600 000 FCFA / mois",
                duration = "CDI",
                domain = "Finance & Fintech",
                description = "Développement des intégrations bancaires et Mobile Money (MTN MoMo, Airtel Money) pour les paiements in-app, les transferts P2P et les pourboires créateurs.",
                requirements = listOf(
                    "Expérience réussie dans le secteur bancaire ou Mobile Money en zone CEMAC",
                    "Excellentes compétences en négociation B2B et régulation financière",
                    "Bac+5 en Gestion, Finance ou Économie"
                ),
                benefits = listOf(
                    "Véhicule de fonction ou indemnité transport",
                    "Assurance santé groupe",
                    "Prime d'objectifs trimestrielle"
                ),
                postedDate = "Il y a 2 jours",
                deadline = "10 Octobre 2026",
                applicantsCount = 11,
                likesCount = 27
            ),
            JobOffer(
                id = "job_5",
                title = "Stagiaire Ingénieur Backend Cloud & IA (Python / Go)",
                company = "LoukaTech Innovation Lab",
                companyLogo = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                location = "Brazzaville, Congo (Hybride)",
                type = "Temps plein",
                contractType = "Stage",
                workMode = "Hybride",
                experienceLevel = "Junior / Étudiant fin de cycle",
                salary = "350 000 FCFA / mois",
                duration = "Stage (6 mois, pré-embauche)",
                domain = "IA & Data",
                description = "Participez au développement des microservices de traitement de texte et de transcription vocale locale (Lingala, Kituba, Français) intégrés à MBoté.",
                requirements = listOf(
                    "Bonnes bases en Python ou Go et bases de données relationnelles",
                    "Curiosité pour le Machine Learning et les LLMs",
                    "Étudiant en Master ou dernière année d'école d'ingénieurs"
                ),
                benefits = listOf(
                    "Indemnité de stage très attractive avec opportunité d'embauche en CDI",
                    "Encadrement par des ingénieurs seniors",
                    "Repas pris en charge au bureau"
                ),
                postedDate = "Il y a 3 jours",
                deadline = "30 Septembre 2026",
                applicantsCount = 35,
                likesCount = 52
            )
        )
    }

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

    private fun createInitialNotifications(): List<MboteNotification> {
        return listOf(
            MboteNotification(
                type = NotificationType.MESSAGE,
                title = "💬 Grace Ondongo",
                body = "Nouveau message : Salut ! As-tu vu l'offre d'emploi Senior Android Dev chez LoukaTech ?",
                timestamp = "Il y a 5 min",
                isRead = false,
                senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80"
            ),
            MboteNotification(
                type = NotificationType.JOB_APPLICATION,
                title = "💼 Nouvelle Candidature Reçue",
                body = "Patrick Mabiala a postulé au poste de Développeur Mobile Senior (CDI).",
                timestamp = "Il y a 15 min",
                isRead = false,
                actionText = "Voir la candidature"
            ),
            MboteNotification(
                type = NotificationType.VIDEO_LIKE,
                title = "❤️ Nouveau Like sur MBoté Reel",
                body = "Merveille K. et 14 autres personnes ont aimé votre vidéo 'Couché de soleil sur la Corniche de Brazzaville'.",
                timestamp = "Il y a 1h",
                isRead = true
            )
        )
    }

    fun updateLanguage(language: AppLanguage) {
        _userProfile.update { it.copy(language = language) }
        saveCachedUserProfile()
    }

    fun updateCurrency(currency: AppCurrency) {
        _userProfile.update { it.copy(currency = currency) }
        saveCachedUserProfile()
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        _userProfile.update { it.copy(themeMode = themeMode) }
        saveCachedUserProfile()
    }

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        saveCachedUserProfile()
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
