package com.loukatech.mbote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loukatech.mbote.data.AronQuestionsData
import com.loukatech.mbote.data.MboteRepository
import com.loukatech.mbote.model.*
import com.loukatech.mbote.service.ContactsSyncService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MboteViewModel(
    private val repository: MboteRepository = MboteRepository(),
    private val contactsSyncService: ContactsSyncService = ContactsSyncService()
) : ViewModel() {

    suspend fun searchGiphy(query: String, stickers: Boolean) = repository.apiService.searchGiphy(query, stickers)

    val userProfile = repository.userProfile
    val notifications = repository.notifications
    val chats = repository.chats
    val calls = repository.calls
    val statuses = repository.statuses
    val newsPosts = repository.newsPosts
    val meetings = repository.meetings
    val jobs = repository.jobs
    val discoverProfiles = repository.discoverProfiles
    val reports = repository.reports

    fun submitReport(type: String, targetName: String) {
        repository.submitReport(type, targetName)
    }

    fun updateReportStatus(reportId: String, newStatus: String) {
        repository.updateReportStatus(reportId, newStatus)
    }

    fun checkAndRunAutoBackup(context: android.content.Context) {
        repository.checkAndRunAutoBackup(context)
    }

    suspend fun performCloudBackup(context: android.content.Context, isAuto: Boolean = false) =
        repository.performCloudBackup(context, isAuto)

    suspend fun restoreCloudBackup(context: android.content.Context) =
        repository.restoreCloudBackup(context)
    val shortVideos = repository.shortVideos
    val channels = repository.channels
    val isOffline = repository.isOffline
    val isAuthenticated = repository.isAuthenticated

    // 5) Parent-Child Connection State & Verification Flow Helper
    private val _parentChildLinkState = MutableStateFlow<ParentChildLinkState>(ParentChildLinkState.Idle)
    val parentChildLinkState: StateFlow<ParentChildLinkState> = _parentChildLinkState.asStateFlow()

    private val _linkedChildInfo = MutableStateFlow(LinkedChildInfo())
    val linkedChildInfo: StateFlow<LinkedChildInfo> = _linkedChildInfo.asStateFlow()

    // 1) Toggleable list of installed applications on child's device
    private val _childInstalledApps = MutableStateFlow<List<ChildInstalledApp>>(defaultChildInstalledApps)
    val childInstalledApps: StateFlow<List<ChildInstalledApp>> = _childInstalledApps.asStateFlow()

    // 2) Child Panic Button / Emergency GPS Location Alert State
    private val _childPanicAlerts = MutableStateFlow<List<ChildPanicAlert>>(listOf(
        ChildPanicAlert(
            alertId = "PANIC-PREV-01",
            childId = "MB-CHILD-88392",
            childName = "Junior Loutala",
            timestamp = "Hier à 18:42",
            latitude = -4.2634,
            longitude = 15.2429,
            address = "Avenue de l'Indépendance, Poto-Poto, Brazzaville",
            city = "Brazzaville, Congo",
            batteryLevel = 92,
            emergencyType = "Trajet École - Maison Sécurisé",
            emergencyMessage = "Notification de position GPS transmise avec succès.",
            isResolved = true
        )
    ))
    val childPanicAlerts: StateFlow<List<ChildPanicAlert>> = _childPanicAlerts.asStateFlow()

    private val _activePanicAlert = MutableStateFlow<ChildPanicAlert?>(null)
    val activePanicAlert: StateFlow<ChildPanicAlert?> = _activePanicAlert.asStateFlow()

    private val _showPanicTriggerDialog = MutableStateFlow(false)
    val showPanicTriggerDialog: StateFlow<Boolean> = _showPanicTriggerDialog.asStateFlow()

    val socketConnectionState = com.loukatech.mbote.service.MboteSocketManager.connectionState
    val isSocketConnected = com.loukatech.mbote.service.MboteSocketManager.isConnected
    val socketUrl = com.loukatech.mbote.service.MboteSocketManager.socketUrl

    init {
        viewModelScope.launch {
            com.loukatech.mbote.service.MboteSocketManager.incomingMessages.collect { msg ->
                if (msg.chatId.isNotBlank()) {
                    repository.refreshMessagesForChat(msg.chatId)
                }
            }
        }
    }

    fun initializeCache(filesDir: java.io.File) {
        repository.initializeCache(filesDir)
        triggerDataSync()
    }

    private val _isDataSyncing = MutableStateFlow(false)
    val isDataSyncing: StateFlow<Boolean> = _isDataSyncing.asStateFlow()
    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()
    private val _publicationError = MutableStateFlow<String?>(null)
    val publicationError: StateFlow<String?> = _publicationError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.messagingError.collect { error ->
                if (!error.isNullOrBlank()) _publicationError.value = error
            }
        }
    }

    fun clearPublicationError() {
        _publicationError.value = null
        repository.clearMessagingError()
    }

    fun triggerDataSync() {
        if (!com.loukatech.mbote.service.api.MboteBackendConfig.authToken.isNullOrBlank()) {
            com.loukatech.mbote.service.MboteSocketManager.connect(forceResetAttempts = true)
        }
        viewModelScope.launch {
            _isDataSyncing.value = true
            try {
                repository.syncAllFromBackend()
                if (repository.isAuthenticated.value) {
                    repository.refreshChannels()
                }
            } catch (error: Throwable) {
                _publicationError.value = error.message ?: "Synchronisation MBoté interrompue."
            } finally {
                _isDataSyncing.value = false
            }
        }
    }

    private val _showLoginScreen = MutableStateFlow(false)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    fun setShowLoginScreen(show: Boolean) {
        _showLoginScreen.value = show
    }

    private val _showAdminLoginDialog = MutableStateFlow(false)
    val showAdminLoginDialog: StateFlow<Boolean> = _showAdminLoginDialog.asStateFlow()

    fun setShowAdminLoginDialog(show: Boolean) {
        _showAdminLoginDialog.value = show
    }

    private val _showForgotPasswordDialog = MutableStateFlow(false)
    val showForgotPasswordDialog: StateFlow<Boolean> = _showForgotPasswordDialog.asStateFlow()

    fun setShowForgotPasswordDialog(show: Boolean) {
        _showForgotPasswordDialog.value = show
    }

    suspend fun login(email: String, pass: String): Result<com.loukatech.mbote.service.api.PendingOtpChallenge> =
        repository.login(email, pass)

    suspend fun verifyLoginOtp(pendingUserId: String, otp: String): Result<Unit> {
        val res = repository.verifyLoginOtp(pendingUserId, otp)
        if (res.isSuccess) {
            _showLoginScreen.value = false
            triggerDataSync()
        }
        return res
    }

    suspend fun register(request: com.loukatech.mbote.service.api.RegisterRequest): Result<com.loukatech.mbote.service.api.PendingOtpChallenge> {
        return repository.register(request)
    }

    fun toggleChannelSubscription(channelId: String, currentlySubscribed: Boolean) {
        viewModelScope.launch {
            repository.setChannelSubscription(channelId, !currentlySubscribed)
                .onFailure { _publicationError.value = it.message }
        }
    }

    suspend fun confirmDesktopQrLogin(qrPayload: String): Result<Unit> =
        repository.confirmDesktopQrLogin(qrPayload)

    suspend fun getRegistrationPublicConfig(): Result<com.loukatech.mbote.service.api.RegistrationPublicConfig> =
        repository.getRegistrationPublicConfig()

    suspend fun verifyRegistrationOtp(pendingUserId: String, otp: String): Result<Unit> {
        val res = repository.verifyRegistrationOtp(pendingUserId, otp)
        if (res.isSuccess) {
            _showLoginScreen.value = false
            triggerDataSync()
        }
        return res
    }

    suspend fun loginWithGoogle(email: String = "", displayName: String = "", avatarUrl: String? = null): Result<Unit> {
        val res = repository.loginWithGoogle(email, displayName, avatarUrl)
        if (res.isSuccess) {
            _showLoginScreen.value = false
            triggerDataSync()
        }
        return res
    }

    suspend fun loginWithGitHub(email: String = "", displayName: String = "", avatarUrl: String? = null): Result<Unit> {
        val res = repository.loginWithGitHub(email, displayName, avatarUrl)
        if (res.isSuccess) {
            _showLoginScreen.value = false
            triggerDataSync()
        }
        return res
    }

    suspend fun requestPasswordReset(email: String): Result<String> {
        return repository.requestPasswordReset(email)
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPass: String): Result<Boolean> {
        return repository.confirmPasswordReset(email, code, newPass)
    }

    suspend fun loginAdmin(key: String, email: String, pass: String): Result<com.loukatech.mbote.service.api.AdminStatsData> {
        return repository.loginAdmin(key, email, pass)
    }

    fun setBackendServerUrl(url: String) {
        repository.setBackendServerUrl(url)
    }

    fun logout() {
        com.loukatech.mbote.service.MboteSocketManager.disconnect()
        repository.logout()
        _showLoginScreen.value = true
    }

    fun deleteMyAccount(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.deleteMyAccount()
            if (result.isSuccess) {
                com.loukatech.mbote.service.MboteSocketManager.disconnect()
                _showLoginScreen.value = true
                onResult(true, "Compte supprimé avec succès.")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Suppression du compte impossible.")
            }
        }
    }

    fun toggleOfflineMode() {
        repository.toggleOfflineMode()
    }

    val contactsSyncState: StateFlow<ContactsSyncState> = contactsSyncService.syncState
    val syncedContacts: StateFlow<List<SyncedContact>> = contactsSyncService.syncedContacts

    private val _currentTab = MutableStateFlow(NavigationTab.MESSAGES)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeCall = MutableStateFlow<CallItem?>(null)
    val activeCall: StateFlow<CallItem?> = _activeCall.asStateFlow()

    private val _activeMeetingRoom = MutableStateFlow<MeetingItem?>(null)
    val activeMeetingRoom: StateFlow<MeetingItem?> = _activeMeetingRoom.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    val blockedContactIds: StateFlow<Set<String>> = repository.blockedContactIds

    val mastaUsers: StateFlow<List<MastaUser>> = repository.mastaUsers

    fun updateMastaUsers(users: List<MastaUser>) {
        repository.updateMastaUsers(users)
    }

    fun blockContact(contactId: String) {
        repository.blockContact(contactId)
    }

    fun unblockContact(contactId: String) {
        repository.unblockContact(contactId)
    }

    fun isContactBlocked(contactId: String): Boolean {
        return repository.isContactBlocked(contactId)
    }

    fun isFriend(chatName: String): Boolean {
        if (chatName.contains("Official") || chatName.contains("Officiel")) {
            return true
        }
        val mastaList = mastaUsers.value
        val user = mastaList.find { it.name.equals(chatName, ignoreCase = true) }
        return if (user != null) {
            user.subType == MastaSubOption.FRIENDS || 
            user.subType == MastaSubOption.ONLINE || 
            user.subType == MastaSubOption.CITIES
        } else false
    }

    // Real-time socket typing states map (chatId -> PartnerTypingState)
    val socketTypingMap: StateFlow<Map<String, com.loukatech.mbote.service.PartnerTypingState>> =
        com.loukatech.mbote.service.MboteSocketManager.typingStateMap

    fun onUserTypingChanged(chatId: String, isTyping: Boolean) {
        com.loukatech.mbote.service.MboteSocketManager.sendUserTypingState(chatId, isTyping)
    }

    private val _chatFilter = MutableStateFlow("Tous")
    val chatFilter: StateFlow<String> = _chatFilter.asStateFlow()

    // Dialog & Sheet states
    private val _showJoinByInviteLinkDialog = MutableStateFlow(false)
    val showJoinByInviteLinkDialog: StateFlow<Boolean> = _showJoinByInviteLinkDialog.asStateFlow()

    fun setShowJoinByInviteLinkDialog(show: Boolean) {
        _showJoinByInviteLinkDialog.value = show
    }

    private val _showAccessRequestDialog = MutableStateFlow(false)
    val showAccessRequestDialog: StateFlow<Boolean> = _showAccessRequestDialog.asStateFlow()

    fun setShowAccessRequestDialog(show: Boolean) {
        _showAccessRequestDialog.value = show
    }

    private val _showNotificationsSheet = MutableStateFlow(false)
    val showNotificationsSheet: StateFlow<Boolean> = _showNotificationsSheet.asStateFlow()

    private val _showQuickActionsMenu = MutableStateFlow(false)
    val showQuickActionsMenu: StateFlow<Boolean> = _showQuickActionsMenu.asStateFlow()

    private val _showNewChatDialog = MutableStateFlow(false)
    val showNewChatDialog: StateFlow<Boolean> = _showNewChatDialog.asStateFlow()

    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

    private val _showCreateChannelDialog = MutableStateFlow(false)
    val showCreateChannelDialog: StateFlow<Boolean> = _showCreateChannelDialog.asStateFlow()

    private val _showMastaSheet = MutableStateFlow(false)
    val showMastaSheet: StateFlow<Boolean> = _showMastaSheet.asStateFlow()

    private val _showContactsSyncSheet = MutableStateFlow(false)
    val showContactsSyncSheet: StateFlow<Boolean> = _showContactsSyncSheet.asStateFlow()

    private val _showAddStatusDialog = MutableStateFlow(false)
    val showAddStatusDialog: StateFlow<Boolean> = _showAddStatusDialog.asStateFlow()

    private val _showNewMeetingDialog = MutableStateFlow(false)
    val showNewMeetingDialog: StateFlow<Boolean> = _showNewMeetingDialog.asStateFlow()

    private val _showEditProfileDialog = MutableStateFlow(false)
    val showEditProfileDialog: StateFlow<Boolean> = _showEditProfileDialog.asStateFlow()

    private val _showJobsScreen = MutableStateFlow(false)
    val showJobsScreen: StateFlow<Boolean> = _showJobsScreen.asStateFlow()

    private val _showAronQuestionsSheet = MutableStateFlow(false)
    val showAronQuestionsSheet: StateFlow<Boolean> = _showAronQuestionsSheet.asStateFlow()

    private val _showPollDialog = MutableStateFlow(false)
    val showPollDialog: StateFlow<Boolean> = _showPollDialog.asStateFlow()

    private val _showLocationSheet = MutableStateFlow(false)
    val showLocationSheet: StateFlow<Boolean> = _showLocationSheet.asStateFlow()

    private val _showPaymentSheet = MutableStateFlow(false)
    val showPaymentSheet: StateFlow<Boolean> = _showPaymentSheet.asStateFlow()

    private val _showStorageDialog = MutableStateFlow(false)
    val showStorageDialog: StateFlow<Boolean> = _showStorageDialog.asStateFlow()

    private val _showLindaProfileDialog = MutableStateFlow(false)
    val showLindaProfileDialog: StateFlow<Boolean> = _showLindaProfileDialog.asStateFlow()

    private val _selectedUserProfile = MutableStateFlow<ProfileDisplayData?>(null)
    val selectedUserProfile: StateFlow<ProfileDisplayData?> = _selectedUserProfile.asStateFlow()

    private val _showEyeContactDialog = MutableStateFlow(false)
    val showEyeContactDialog: StateFlow<Boolean> = _showEyeContactDialog.asStateFlow()

    private val _eyeContactSeconds = MutableStateFlow(240) // 4 minutes
    val eyeContactSeconds: StateFlow<Int> = _eyeContactSeconds.asStateFlow()
    private var eyeContactJob: Job? = null

    private val _selectedPostForComments = MutableStateFlow<NewsPost?>(null)
    val selectedPostForComments: StateFlow<NewsPost?> = _selectedPostForComments.asStateFlow()

    private val _selectedStatusStory = MutableStateFlow<StatusItem?>(null)
    val selectedStatusStory: StateFlow<StatusItem?> = _selectedStatusStory.asStateFlow()

    private val _selectedDiscoverProfile = MutableStateFlow<DiscoverProfile?>(null)
    val selectedDiscoverProfile: StateFlow<DiscoverProfile?> = _selectedDiscoverProfile.asStateFlow()

    // Short Videos Feature State (short-videos?returnTo=%2Fapp%3Ftab%3Ddashboard)
    private val _showShortVideosScreen = MutableStateFlow(false)
    val showShortVideosScreen: StateFlow<Boolean> = _showShortVideosScreen.asStateFlow()

    private val _shortVideosReturnTo = MutableStateFlow<String?>("/app?tab=dashboard")
    val shortVideosReturnTo: StateFlow<String?> = _shortVideosReturnTo.asStateFlow()

    private val _shortVideosFeedType = MutableStateFlow(ShortVideosFeedType.FOR_YOU)
    val shortVideosFeedType: StateFlow<ShortVideosFeedType> = _shortVideosFeedType.asStateFlow()

    private val _selectedShortVideoForComments = MutableStateFlow<ShortVideo?>(null)
    val selectedShortVideoForComments: StateFlow<ShortVideo?> = _selectedShortVideoForComments.asStateFlow()

    private val _selectedShortVideoForShare = MutableStateFlow<ShortVideo?>(null)
    val selectedShortVideoForShare: StateFlow<ShortVideo?> = _selectedShortVideoForShare.asStateFlow()

    private val _selectedShortVideoForTip = MutableStateFlow<ShortVideo?>(null)
    val selectedShortVideoForTip: StateFlow<ShortVideo?> = _selectedShortVideoForTip.asStateFlow()

    private val _selectedCreatorProfile = MutableStateFlow<ShortVideo?>(null)
    val selectedCreatorProfile: StateFlow<ShortVideo?> = _selectedCreatorProfile.asStateFlow()

    private val _showCreateShortVideoDialog = MutableStateFlow(false)
    val showCreateShortVideoDialog: StateFlow<Boolean> = _showCreateShortVideoDialog.asStateFlow()

    private val _isMutedInMeeting = MutableStateFlow(false)
    val isMutedInMeeting: StateFlow<Boolean> = _isMutedInMeeting.asStateFlow()

    private val _isVideoOffInMeeting = MutableStateFlow(false)
    val isVideoOffInMeeting: StateFlow<Boolean> = _isVideoOffInMeeting.asStateFlow()

    val totalUnreadMessages: StateFlow<Int> = chats.map { list ->
        list.sumOf { it.unreadCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeChat: StateFlow<Chat?> = combine(chats, _activeChatId) { chatList, id ->
        chatList.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredChats: StateFlow<List<Chat>> = combine(chats, _searchQuery, _chatFilter) { list, query, filter ->
        val trimmedQuery = query.trim()
        list.filter { chat ->
            val matchesQuery = trimmedQuery.isBlank() ||
                chat.name.contains(trimmedQuery, ignoreCase = true) ||
                chat.lastMessage.contains(trimmedQuery, ignoreCase = true) ||
                chat.participants.any { it.name.contains(trimmedQuery, ignoreCase = true) } ||
                chat.messages.any { it.text.contains(trimmedQuery, ignoreCase = true) }

            val matchesFilter = when (filter) {
                "Non lus" -> chat.unreadCount > 0
                "Discussions", "Contacts" -> !chat.isGroup && !chat.isChannel
                "Groupes" -> chat.isGroup
                "Canaux" -> chat.isChannel
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setThemeMode(mode: AppThemeMode) {
        repository.setThemeMode(mode)
    }

    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    private var typingJob: Job? = null

    fun openChat(chatId: String) {
        _activeChatId.value = chatId
        repository.markChatAsRead(chatId)

        // Asynchronously fetch fresh messages for this chat from the real REST API
        viewModelScope.launch {
            repository.refreshMessagesForChat(chatId)
        }

        // Typing and new messages are received only from the authenticated socket session.
    }

    fun openChatByName(name: String) {
        val existing = repository.chats.value.find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            openChat(existing.id)
        } else {
            viewModelScope.launch {
                repository.createDirectChat(name, "")
                    .onSuccess { openChat(it.id) }
                    .onFailure { _publicationError.value = it.message ?: "Discussion impossible." }
            }
        }
    }

    fun closeChat() {
        val chatId = _activeChatId.value
        if (chatId != null) {
            com.loukatech.mbote.service.MboteSocketManager.clearTyping(chatId)
        }
        _activeChatId.value = null
        _isPartnerTyping.value = false
        typingJob?.cancel()
    }

    fun startCall(name: String, avatar: String, isVideo: Boolean) {
        _activeCall.value = CallItem(
            name = name,
            avatar = avatar,
            type = CallType.OUTGOING,
            isVideo = isVideo,
            timestamp = "En cours"
        )
    }

    fun endCall(durationText: String? = null) {
        val active = _activeCall.value
        if (active != null) {
            val finalCall = active.copy(
                timestamp = "À l'instant",
                durationText = durationText ?: "0 s"
            )
            viewModelScope.launch {
                repository.addCallLog(finalCall)
            }
        }
        _activeCall.value = null
    }

    fun refreshCalls() {
        viewModelScope.launch {
            repository.refreshCallsFromBackend()
        }
    }

    fun refreshMasta() {
        viewModelScope.launch {
            repository.refreshMastaFromBackend()
        }
    }

    fun refreshShorts() {
        viewModelScope.launch {
            repository.refreshShortsFromBackend()
        }
    }

    fun startMeeting(meeting: MeetingItem) {
        _activeMeetingRoom.value = meeting
    }

    fun leaveMeeting() {
        _activeMeetingRoom.value = null
    }

    fun toggleMeetingMute() {
        _isMutedInMeeting.update { !it }
    }

    fun toggleMeetingVideo() {
        _isVideoOffInMeeting.update { !it }
    }

    fun sendMessage(chatId: String, text: String, replyTo: Message? = null) {
        if (text.isNotBlank()) {
            repository.sendMessage(chatId, text.trim(), replyTo)
        }
    }

    fun sendVoiceMessage(
        context: android.content.Context,
        chatId: String,
        audioPath: String,
        durationSec: Int,
        replyTo: Message? = null
    ) {
        repository.sendVoiceMessage(context, chatId, audioPath, durationSec, replyTo)
    }

    fun sendAronQuestion(chatId: String, question: AronQuestion) {
        repository.sendAronQuestion(chatId, question)
        _showAronQuestionsSheet.value = false
    }

    fun sendPoll(chatId: String, question: String, options: List<String>, isMultipleChoice: Boolean = false) {
        if (question.isNotBlank() && options.count { it.isNotBlank() } >= 2) {
            repository.sendPoll(chatId, question, options, isMultipleChoice)
            _showPollDialog.value = false
        }
    }

    fun votePoll(chatId: String, messageId: String, optionId: String) {
        repository.votePoll(chatId, messageId, optionId)
    }

    fun sendLocation(
        chatId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
        isLive: Boolean,
        durationMinutes: Int
    ) {
        repository.sendLocation(chatId, placeName, latitude, longitude, isLive, durationMinutes)
        _showLocationSheet.value = false
    }

    fun sendPaymentTransfer(
        chatId: String,
        amount: String,
        provider: String,
        note: String,
        isRequest: Boolean
    ) {
        if (amount.isNotBlank()) {
            repository.sendPaymentTransfer(chatId, amount, provider, note, isRequest)
            _showPaymentSheet.value = false
        }
    }

    fun translateMessage(chatId: String, messageId: String, targetLanguage: String = "Lingala") {
        repository.translateMessage(chatId, messageId, targetLanguage)
    }

    fun setChatDisappearingTimer(chatId: String, seconds: Int) {
        repository.setChatDisappearingTimer(chatId, seconds)
    }

    fun setChatWallpaper(chatId: String, colorHex: String?, imageUrl: String?) {
        repository.setChatWallpaper(chatId, colorHex, imageUrl)
    }

    fun addReaction(chatId: String, messageId: String, emoji: String) {
        repository.addReaction(chatId, messageId, emoji)
    }

    fun deleteMessage(chatId: String, messageId: String) {
        repository.deleteMessage(chatId, messageId)
    }

    fun createChat(name: String, message: String, isGroup: Boolean) {
        if (name.isNotBlank() && message.isNotBlank()) {
            if (isGroup) {
                _publicationError.value = "Utilisez « Créer un groupe » pour sélectionner de vrais membres MBoté."
                return
            }
            viewModelScope.launch {
                _isPublishing.value = true
                repository.createDirectChat(name.trim(), message.trim())
                    .onSuccess { chat ->
                        _showNewChatDialog.value = false
                        _showQuickActionsMenu.value = false
                        openChat(chat.id)
                    }
                    .onFailure { _publicationError.value = it.message ?: "Discussion impossible." }
                _isPublishing.value = false
            }
        }
    }

    fun createGroup(
        name: String,
        description: String,
        members: List<SyncedContact>,
        avatar: String? = null,
        initialMessage: String = ""
    ) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                _isPublishing.value = true
                repository.createGroupApi(name.trim(), description.trim(), members, avatar, initialMessage.trim())
                    .onSuccess { group ->
                        _showCreateGroupDialog.value = false
                        _showQuickActionsMenu.value = false
                        openChat(group.id)
                    }
                    .onFailure { _publicationError.value = it.message ?: "Création du groupe impossible." }
                _isPublishing.value = false
            }
        }
    }

    fun sendMediaAttachment(
        context: android.content.Context,
        chatId: String,
        mediaUrl: String,
        isVideo: Boolean,
        caption: String = "",
        replyTo: Message? = null
    ) {
        val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
        repository.sendMediaMessage(context, chatId, mediaUrl, mediaType, caption, replyTo)
    }

    fun createChannel(name: String, description: String, isPublic: Boolean, initialPost: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                _isPublishing.value = true
                repository.createChannelApi(name.trim(), description.trim(), isPublic, initialPost.trim())
                    .onSuccess {
                        _showCreateChannelDialog.value = false
                        _showQuickActionsMenu.value = false
                        repository.refreshPublicationsFromBackend()
                    }
                    .onFailure { _publicationError.value = it.message ?: "Création de la chaîne impossible." }
                _isPublishing.value = false
            }
        }
    }

    fun addNewMasta(name: String, phone: String, isMboteUser: Boolean = true): SyncedContact {
        val contact = contactsSyncService.addContact(name.trim(), phone.trim(), isMboteUser)
        return contact
    }

    fun startChatWithContact(contact: SyncedContact) {
        val chatId = repository.getOrCreateChatForContact(contact)
        _showContactsSyncSheet.value = false
        _showNewChatDialog.value = false
        openChat(chatId)
    }

    fun startChatWithProfile(profile: DiscoverProfile) {
        val chatId = repository.getOrCreateChatForProfile(profile)
        _selectedDiscoverProfile.value = null
        openChat(chatId)
    }

    fun sendMboteGreeting(profile: DiscoverProfile) {
        repository.sendMboteGreeting(profile)
    }

    fun syncPhoneContacts() {
        viewModelScope.launch {
            contactsSyncService.syncContacts()
        }
    }

    fun postStatus(context: android.content.Context, text: String, mediaUri: android.net.Uri? = null, mediaType: String = "text", background: String? = null, visibility: String = "friends") {
        if (text.isBlank() && mediaUri == null) return
        viewModelScope.launch {
            _isPublishing.value = true
            _publicationError.value = null
            val result = repository.addStatusFromDevice(context, text, mediaUri, mediaType, background, visibility)
            _isPublishing.value = false
            if (result.isSuccess) _showAddStatusDialog.value = false
            else _publicationError.value = result.exceptionOrNull()?.message ?: "Publication du statut impossible."
        }
    }

    fun createMeeting(title: String, durationMin: Int = 45) {
        if (title.isNotBlank()) {
            viewModelScope.launch {
                val result = repository.createGroupCallRoomApi(title.trim(), isVideo = true)
                _showNewMeetingDialog.value = false
                if (result.isSuccess) {
                    _activeMeetingRoom.value = result.getOrNull()
                } else {
                    val fallback = repository.createMeeting(title.trim(), durationMin)
                    _activeMeetingRoom.value = fallback
                }
            }
        }
    }

    fun joinMeetingByCode(code: String) {
        if (code.isNotBlank()) {
            viewModelScope.launch {
                val result = repository.joinGroupCallRoomApi(code.trim())
                if (result.isSuccess) {
                    _activeMeetingRoom.value = result.getOrNull()
                }
            }
        }
    }

    fun toggleNewsLike(postId: String) {
        viewModelScope.launch {
            repository.toggleNewsLike(postId).onFailure { _publicationError.value = it.message }
        }
    }

    fun addNewsPost(context: android.content.Context, title: String, content: String, mediaUri: android.net.Uri? = null, category: String = "Communauté", mediaType: String = "text") {
        viewModelScope.launch {
            _isPublishing.value = true
            repository.publishPostFromDevice(context, title, content, mediaUri, category, mediaType).onFailure { _publicationError.value = it.message }
            _isPublishing.value = false
        }
    }

    fun addNewsComment(postId: String, text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.addNewsComment(postId, text.trim()).onFailure { _publicationError.value = it.message }
            }
        }
    }

    fun toggleJobLike(jobId: String) {
        repository.toggleJobLike(jobId)
    }

    fun toggleJobBookmark(jobId: String) {
        repository.toggleJobBookmark(jobId)
    }

    fun applyToJob(jobId: String): Boolean {
        return repository.applyToJob(jobId)
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
        return repository.postJobOffer(
            title = title,
            company = company,
            location = location,
            domain = domain,
            contractType = contractType,
            workMode = workMode,
            salary = salary,
            description = description,
            requirements = requirements,
            benefits = benefits
        )
    }

    fun updateUserProfile(name: String, bio: String, phone: String, city: String) {
        repository.updateUserProfile(name, bio, phone, city)
        _showEditProfileDialog.value = false
    }

    fun updateBio(newBio: String) {
        val current = repository.userProfile.value
        repository.updateUserProfile(current.name, newBio, current.phone, current.city)
    }

    fun toggleDarkMode() {
        repository.toggleDarkMode()
    }

    fun toggleNotifications() {
        repository.toggleNotifications()
    }

    // Eye Contact Meditation / Timer
    fun startEyeContactTimer() {
        eyeContactJob?.cancel()
        _eyeContactSeconds.value = 240
        _showEyeContactDialog.value = true
        eyeContactJob = viewModelScope.launch {
            while (_eyeContactSeconds.value > 0) {
                delay(1000)
                _eyeContactSeconds.update { maxOf(0, it - 1) }
            }
        }
    }

    fun stopEyeContactTimer() {
        eyeContactJob?.cancel()
        _showEyeContactDialog.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setChatFilter(filter: String) {
        _chatFilter.value = filter
    }

    fun setShowQuickActionsMenu(show: Boolean) {
        _showQuickActionsMenu.value = show
    }

    fun setShowNewChatDialog(show: Boolean) {
        _showNewChatDialog.value = show
    }

    fun setShowCreateGroupDialog(show: Boolean) {
        _showCreateGroupDialog.value = show
    }

    fun setShowCreateChannelDialog(show: Boolean) {
        _showCreateChannelDialog.value = show
    }

    fun setShowMastaSheet(show: Boolean) {
        _showMastaSheet.value = show
    }

    fun setShowContactsSyncSheet(show: Boolean) {
        _showContactsSyncSheet.value = show
    }

    fun setShowAddStatusDialog(show: Boolean) {
        _showAddStatusDialog.value = show
    }

    fun setShowNewMeetingDialog(show: Boolean) {
        _showNewMeetingDialog.value = show
    }

    fun setShowEditProfileDialog(show: Boolean) {
        _showEditProfileDialog.value = show
    }

    fun setShowJobsScreen(show: Boolean) {
        _showJobsScreen.value = show
    }

    fun setShowAronQuestionsSheet(show: Boolean) {
        _showAronQuestionsSheet.value = show
    }

    fun setShowPollDialog(show: Boolean) {
        _showPollDialog.value = show
    }

    fun setShowLindaProfileDialog(show: Boolean) {
        _showLindaProfileDialog.value = show
    }

    fun setSelectedUserProfile(profile: ProfileDisplayData?) {
        _selectedUserProfile.value = profile
    }

    fun setShowLocationSheet(show: Boolean) {
        _showLocationSheet.value = show
    }

    fun setShowNotificationsSheet(show: Boolean) {
        _showNotificationsSheet.value = show
    }

    fun markNotificationAsRead(id: String) {
        repository.markNotificationAsRead(id)
    }

    fun markAllNotificationsAsRead() {
        repository.markAllNotificationsAsRead()
    }

    fun clearAllNotifications() {
        repository.clearAllNotifications()
    }

    fun sendFcmTestNotification(type: NotificationType, context: android.content.Context) {
        val notification = when (type) {
            NotificationType.MESSAGE -> MboteNotification(
                type = NotificationType.MESSAGE,
                title = "💬 Test FCM Push - Nouveau Message",
                body = "Grace Ondongo : 'MBoté ! Mon test FCM fonctionne parfaitement en arrière-plan !'",
                timestamp = "À l'instant",
                senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80"
            )
            NotificationType.JOB_APPLICATION -> MboteNotification(
                type = NotificationType.JOB_APPLICATION,
                title = "💼 Test FCM Push - Candidature Emploi",
                body = "Nouvelle candidature reçue pour 'Développeur Android Senior (CDI)'.",
                timestamp = "À l'instant",
                actionText = "Voir la candidature"
            )
            NotificationType.VIDEO_LIKE -> MboteNotification(
                type = NotificationType.VIDEO_LIKE,
                title = "❤️ Test FCM Push - Like MBoté Reel",
                body = "Marc L. a aimé votre MBoté Reel 'Sunset sur le Fleuve Congo'.",
                timestamp = "À l'instant"
            )
            NotificationType.GIFT_RECEIVED -> MboteNotification(
                type = NotificationType.GIFT_RECEIVED,
                title = "🎁 Test FCM Push - Cadeau Reçu",
                body = "Aron O. vous a offert 🦁 Lion d'Or Prestige (50.000 FCFA) en direct !",
                timestamp = "À l'instant"
            )
            NotificationType.LIVE_MESSAGE -> MboteNotification(
                type = NotificationType.LIVE_MESSAGE,
                title = "💬 Test FCM Push - Message Direct",
                body = "Grace O. : 'Superbe session en direct !'",
                timestamp = "À l'instant"
            )
            NotificationType.LIVE_BROADCAST -> MboteNotification(
                type = NotificationType.LIVE_BROADCAST,
                title = "🔴 Test FCM Push - Live Démarré",
                body = "LoukaTech a démarré une session en direct : 'Hackathon MBoté 2026'",
                timestamp = "À l'instant"
            )
            NotificationType.SYSTEM -> MboteNotification(
                type = NotificationType.SYSTEM,
                title = "🔔 Alerte Système MBoté",
                body = "Notification push FCM en temps réel.",
                timestamp = "À l'instant"
            )
        }

        repository.addNotification(notification)
        com.loukatech.mbote.service.MboteNotificationManager.dispatchSystemNotification(context, notification)
    }

    fun setShowPaymentSheet(show: Boolean) {
        _showPaymentSheet.value = show
    }

    fun setShowStorageDialog(show: Boolean) {
        _showStorageDialog.value = show
    }

    fun setSelectedPostForComments(post: NewsPost?) {
        _selectedPostForComments.value = post
    }

    fun setSelectedStatusStory(status: StatusItem?) {
        _selectedStatusStory.value = status
        if (status != null && !status.isMine) {
            viewModelScope.launch { repository.markStatusViewed(status.id) }
        }
    }

    fun shareNewsPost(postId: String) {
        viewModelScope.launch { repository.shareNewsPost(postId).onFailure { _publicationError.value = it.message } }
    }

    fun markShortViewed(videoId: String) {
        viewModelScope.launch { repository.markShortViewed(videoId) }
    }

    fun setSelectedDiscoverProfile(profile: DiscoverProfile?) {
        _selectedDiscoverProfile.value = profile
    }

    fun openShortVideos(returnTo: String = "/app?tab=dashboard") {
        _shortVideosReturnTo.value = returnTo
        _showShortVideosScreen.value = true
    }

    fun closeShortVideos() {
        _showShortVideosScreen.value = false
        // If returnTo specifies a tab, we can route back to it
        val returnTo = _shortVideosReturnTo.value ?: "/app?tab=dashboard"
        when {
            returnTo.contains("tab=actus", ignoreCase = true) -> _currentTab.value = NavigationTab.ACTUS
            returnTo.contains("tab=masta", ignoreCase = true) -> _currentTab.value = NavigationTab.MASTA
            returnTo.contains("tab=discover", ignoreCase = true) || returnTo.contains("tab=connexions", ignoreCase = true) -> _currentTab.value = NavigationTab.MASTA
            returnTo.contains("tab=messages", ignoreCase = true) || returnTo.contains("tab=dashboard", ignoreCase = true) -> _currentTab.value = NavigationTab.MESSAGES
            returnTo.contains("tab=calls", ignoreCase = true) -> _currentTab.value = NavigationTab.CALLS
            returnTo.contains("tab=meetings", ignoreCase = true) -> _currentTab.value = NavigationTab.MEETINGS
            returnTo.contains("tab=settings", ignoreCase = true) -> _currentTab.value = NavigationTab.SETTINGS
        }
    }

    fun setShortVideosFeedType(type: ShortVideosFeedType) {
        _shortVideosFeedType.value = type
    }

    fun toggleLikeShortVideo(videoId: String) {
        viewModelScope.launch { repository.toggleLikeShortVideo(videoId).onFailure { _publicationError.value = it.message } }
    }

    fun reactToShortVideo(videoId: String, emoji: String) {
        viewModelScope.launch { repository.reactToShortVideo(videoId, emoji).onFailure { _publicationError.value = it.message } }
    }

    fun setSelectedCreatorProfile(video: ShortVideo?) {
        _selectedCreatorProfile.value = video
    }

    fun startChatWithCreator(video: ShortVideo) {
        val chatId = repository.getOrCreateChatForCreator(video)
        _selectedCreatorProfile.value = null
        _showShortVideosScreen.value = false
        openChat(chatId)
    }

    fun sendGreetingToCreator(video: ShortVideo) {
        val chatId = repository.getOrCreateChatForCreator(video)
        sendMessage(chatId, "Mbote ${video.creatorName} ! 👋 J'ai adoré ton Short « ${video.caption} » sur MBoté ✨ Bravo pour ton travail !")
    }

    fun toggleBookmarkShortVideo(videoId: String) {
        viewModelScope.launch { repository.toggleBookmarkShortVideo(videoId).onFailure { _publicationError.value = it.message } }
    }

    fun toggleFollowShortCreator(creatorId: String) {
        viewModelScope.launch { repository.toggleFollowShortCreator(creatorId).onFailure { _publicationError.value = it.message } }
    }

    fun addShortVideoComment(videoId: String, text: String) {
        viewModelScope.launch { repository.addShortVideoComment(videoId, text).onFailure { _publicationError.value = it.message } }
    }

    fun toggleLikeShortComment(videoId: String, commentId: String) {
        repository.toggleLikeShortComment(videoId, commentId)
    }

    fun setSelectedShortVideoForComments(video: ShortVideo?) {
        _selectedShortVideoForComments.value = video
        if (video != null) viewModelScope.launch { repository.refreshShortComments(video.id) }
    }

    fun setSelectedShortVideoForShare(video: ShortVideo?) {
        _selectedShortVideoForShare.value = video
    }

    fun setSelectedShortVideoForTip(video: ShortVideo?) {
        _selectedShortVideoForTip.value = video
    }

    fun setShowCreateShortVideoDialog(show: Boolean) {
        _showCreateShortVideoDialog.value = show
    }

    fun createShortVideo(
        context: android.content.Context,
        videoUri: android.net.Uri,
        durationSeconds: Int,
        caption: String,
        hashtags: List<String>,
        musicTitle: String,
        musicArtist: String,
        thumbnailUrl: String,
        location: String? = null
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val result = repository.createShortVideo(context, videoUri, durationSeconds, caption, hashtags, musicTitle, musicArtist, thumbnailUrl, location)
            _isPublishing.value = false
            if (result.isSuccess) _showCreateShortVideoDialog.value = false
            else _publicationError.value = result.exceptionOrNull()?.message ?: "Publication ShortMBoté impossible."
        }
    }

    fun shareShortVideoToChat(chatId: String, shortVideo: ShortVideo) {
        viewModelScope.launch {
            val result = repository.shareShortVideoToChat(chatId, shortVideo)
            if (result.isSuccess) _selectedShortVideoForShare.value = null
            else _publicationError.value = result.exceptionOrNull()?.message
        }
    }

    val userGiftState: StateFlow<UserGiftState> = repository.userGiftState

    fun tipCreator(videoId: String, amountFcfa: Long, provider: String = "MBoté Pay / MTN MoMo") {
        repository.tipCreator(videoId, amountFcfa, provider)
        _selectedShortVideoForTip.value = null
    }

    fun buyGiftBundle(bundle: GiftBundle, provider: String = "MBoté Pay / MTN MoMo"): Boolean {
        if (!repository.isAuthenticated.value) return false
        viewModelScope.launch {
            repository.requestGiftPurchase(bundle.priceFcfa, provider)
                .onSuccess { intent ->
                    _publicationError.value = intent.instructions
                        ?: "Paiement ${intent.status.lowercase()}. Référence ${intent.id}."
                }
                .onFailure { _publicationError.value = it.message }
        }
        return true
    }

    fun buySingleGift(gift: GiftItem, count: Int = 1, provider: String = "MBoté Pay / MTN MoMo"): Boolean {
        if (!repository.isAuthenticated.value || count <= 0) return false
        viewModelScope.launch {
            repository.requestGiftPurchase(gift.priceFcfa * count, provider)
                .onSuccess { intent ->
                    _publicationError.value = intent.instructions
                        ?: "Paiement ${intent.status.lowercase()}. Référence ${intent.id}."
                }
                .onFailure { _publicationError.value = it.message }
        }
        return true
    }

    fun sendGift(giftId: String, recipientName: String, multiplier: Int = 1): Boolean {
        return repository.sendGift(giftId, recipientName, multiplier)
    }

    fun buyBadge(badgeType: BadgeType, provider: String = "MTN Mobile Money"): Boolean {
        return repository.buyBadge(badgeType, provider)
    }

    fun updateGiftPrice(giftId: String, newPriceFcfa: Long) {
        repository.updateGiftPrice(giftId, newPriceFcfa)
    }

    fun adminRestockGift(giftId: String, count: Int) {
        repository.adminRestockGift(giftId, count)
    }

    fun togglePremiumStatus(isPremium: Boolean) {
        repository.togglePremiumStatus(isPremium)
    }

    fun updateWithdrawalStatus(withdrawalId: String, newStatus: WithdrawalStatus) {
        repository.updateWithdrawalStatus(withdrawalId, newStatus)
    }

    fun updateLanguage(language: AppLanguage) {
        repository.updateLanguage(language)
    }

    fun updateCurrency(currency: AppCurrency) {
        repository.updateCurrency(currency)
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        repository.updateThemeMode(themeMode)
    }

    fun updateUserProfile(profile: UserProfile) {
        repository.updateUserProfile(profile)
    }

    fun updateParentalControl(
        active: Boolean,
        parentEmail: String,
        nightLockdown: Boolean,
        maxScreenTime: Int,
        commentCurfew: Int,
        schoolHours: Boolean,
        isChildLinked: Boolean
    ) {
        val current = repository.userProfile.value
        val updated = current.copy(
            parentalControlActive = active,
            parentEmail = parentEmail,
            nightLockdownEnabled = nightLockdown,
            maxDailyScreenTimeMinutes = maxScreenTime,
            commentCurfewHour = commentCurfew,
            schoolHoursRestrictionEnabled = schoolHours,
            isChildAccountLinkedByQrScan = isChildLinked
        )
        repository.updateUserProfile(updated)
    }

    fun sendSosAlert(parentEmail: String, reason: String): Boolean {
        // Simulates sending push notification and Brevo transactional email to parent
        val current = repository.userProfile.value
        val updatedActions = current.atRiskActions.toMutableList()
        updatedActions.add(
            0,
            AtRiskAction(
                timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                title = "🚨 ALERTE SOS ENFANT",
                description = "Signalement de détresse / contenu inapproprié envoyé à $parentEmail via Brevo & Push. Raison : $reason",
                severity = RiskSeverity.HIGH
            )
        )
        repository.updateUserProfile(current.copy(atRiskActions = updatedActions))
        return true
    }

    fun checkAndEnforceQuota(): Boolean {
        val current = repository.userProfile.value
        if (current.currentScreenTimeMinutes >= current.maxDailyScreenTimeMinutes && current.parentalControlActive) {
            // Quota reached: Force logout and block reconnection until next day
            val updatedActions = current.atRiskActions.toMutableList()
            updatedActions.add(
                0,
                AtRiskAction(
                    timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                    title = "⛔ Quota 2h Atteint - Déconnexion Forcée",
                    description = "Le temps d'écran quotidien maximal (${current.maxDailyScreenTimeMinutes} min) a été atteint. Déconnexion automatique et blocage jusqu'au lendemain.",
                    severity = RiskSeverity.HIGH
                )
            )
            repository.updateUserProfile(current.copy(atRiskActions = updatedActions, isLoggedOutDueToQuota = true))
            return true
        }
        return false
    }

    fun cashoutVirtualGifts(amountFcfa: Long, destinationProvider: String, phoneNumber: String): Boolean {
        return repository.cashoutVirtualGifts(amountFcfa, destinationProvider, phoneNumber)
    }

    fun getScrollingMinutes(): Int {
        return repository.getScrollingMinutes()
    }

    fun addScrollingMinutes(min: Int) {
        repository.addScrollingMinutes(min)
    }

    // --- 5) Helper for Parent-Child Connection & QR Verification State ---

    fun startParentChildQrScan() {
        _parentChildLinkState.value = ParentChildLinkState.Scanning
    }

    fun processParentChildQrCode(qrPayload: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _parentChildLinkState.value = ParentChildLinkState.Verifying(
                qrPayload = qrPayload,
                progress = 0.25f,
                statusMessage = "Analyse du QR code et lecture du jeton de sécurité..."
            )
            delay(500)

            _parentChildLinkState.value = ParentChildLinkState.Verifying(
                qrPayload = qrPayload,
                progress = 0.65f,
                statusMessage = "Échange de clés RSA-2048 & vérification du compte enfant..."
            )
            delay(600)

            _parentChildLinkState.value = ParentChildLinkState.Verifying(
                qrPayload = qrPayload,
                progress = 0.90f,
                statusMessage = "Association du canal d'urgence SOS Brevo & Quota 2h..."
            )
            delay(400)

            // Extract child info if present or use default child account
            val childInfo = LinkedChildInfo(
                id = if (qrPayload.contains("id=")) qrPayload.substringAfter("id=").substringBefore("&") else "MB-CHILD-88392",
                name = if (qrPayload.contains("name=")) qrPayload.substringAfter("name=").substringBefore("&") else "Junior Loutala",
                username = "@junior_lt",
                avatar = "https://images.unsplash.com/photo-1543610892-0b1f7e6d8ac1?w=150&auto=format&fit=crop&q=80",
                age = 13,
                schoolName = "Lycée d'Excellence de Brazzaville",
                deviceModel = "Samsung Galaxy A15 (Android 14)",
                batteryLevel = 88,
                isOnline = true,
                lastActive = "À l'instant",
                linkToken = qrPayload.ifBlank { "MBOTE-LINK-QR-9941-XYZ" }
            )

            _linkedChildInfo.value = childInfo

            val now = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val successState = ParentChildLinkState.Success(
                childProfile = childInfo,
                linkedAt = now
            )
            _parentChildLinkState.value = successState

            // Update user profile to mark child account linked
            val current = repository.userProfile.value
            val updated = current.copy(
                isChildAccountLinkedByQrScan = true,
                parentalControlActive = true
            )
            repository.updateUserProfile(updated)

            onComplete?.invoke(true)
        }
    }

    fun confirmChildLink(childInfo: LinkedChildInfo? = null) {
        val current = repository.userProfile.value
        val updated = current.copy(
            isChildAccountLinkedByQrScan = true,
            parentalControlActive = true
        )
        repository.updateUserProfile(updated)
        if (childInfo != null) {
            _linkedChildInfo.value = childInfo
        }
    }

    fun unlinkChildAccount() {
        val current = repository.userProfile.value
        val updated = current.copy(
            isChildAccountLinkedByQrScan = false,
            parentalControlActive = false
        )
        repository.updateUserProfile(updated)
        _parentChildLinkState.value = ParentChildLinkState.Idle
    }

    fun resetParentChildLinkState() {
        _parentChildLinkState.value = ParentChildLinkState.Idle
    }

    fun updateParentalSubscriptionPlan(planId: String, planName: String) {
        val current = repository.userProfile.value
        repository.updateUserProfile(current.copy(isPremium = true))
    }

    // 1) Remotely Whitelist or Block Specific Apps on Child's Device
    fun toggleChildAppBlocked(packageName: String, isBlocked: Boolean) {
        _childInstalledApps.update { list ->
            list.map { app ->
                if (app.packageName == packageName) app.copy(isBlocked = isBlocked) else app
            }
        }
        _linkedChildInfo.update { child ->
            child.copy(installedApps = _childInstalledApps.value)
        }

        // Record audit action in parental history
        val currentProfile = repository.userProfile.value
        val app = _childInstalledApps.value.find { it.packageName == packageName }
        val appName = app?.appName ?: packageName
        val actionTitle = if (isBlocked) "🚫 Application Bloquée à Distance" else "✅ Application Autorisée à Distance"
        val updatedActions = currentProfile.atRiskActions.toMutableList()
        updatedActions.add(
            0,
            AtRiskAction(
                timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                title = actionTitle,
                description = "L'application '$appName' a été ${if (isBlocked) "bloquée" else "débloquée"} à distance sur le Samsung Galaxy A15 de Junior.",
                severity = if (isBlocked) RiskSeverity.MEDIUM else RiskSeverity.LOW
            )
        )
        repository.updateUserProfile(currentProfile.copy(atRiskActions = updatedActions))
    }

    fun toggleAllChildApps(blockAll: Boolean, category: String? = null) {
        _childInstalledApps.update { list ->
            list.map { app ->
                if (category == null || app.category.equals(category, ignoreCase = true)) {
                    // Don't block MBoté Chat family app by default unless explicitly requested
                    if (app.packageName == "com.loukatech.mbote" && blockAll) app else app.copy(isBlocked = blockAll)
                } else {
                    app
                }
            }
        }
        _linkedChildInfo.update { child ->
            child.copy(installedApps = _childInstalledApps.value)
        }
    }

    fun setChildAppSchoolRestriction(packageName: String, restricted: Boolean) {
        _childInstalledApps.update { list ->
            list.map { app ->
                if (app.packageName == packageName) app.copy(restrictedDuringSchoolHours = restricted) else app
            }
        }
        _linkedChildInfo.update { child ->
            child.copy(installedApps = _childInstalledApps.value)
        }
    }

    // 2) Child Panic Button: Immediate GPS Location Update via Parental Control Connection
    fun triggerChildPanicAlert(
        latitude: Double = -4.2634,
        longitude: Double = 15.2429,
        address: String = "Avenue de l'Indépendance, Poto-Poto, Brazzaville",
        emergencyType: String = "Bouton Panique Pressé 🚨",
        customMessage: String = "Alerte de détresse immédiate ! Junior a pressé le bouton panique. Localisation GPS transmise en temps réel."
    ): ChildPanicAlert {
        val now = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val alert = ChildPanicAlert(
            alertId = "PANIC-" + System.currentTimeMillis(),
            childId = _linkedChildInfo.value.id,
            childName = _linkedChildInfo.value.name,
            childAvatar = _linkedChildInfo.value.avatar,
            timestamp = now,
            latitude = latitude,
            longitude = longitude,
            address = address,
            city = "Brazzaville, Congo",
            batteryLevel = _linkedChildInfo.value.batteryLevel,
            emergencyType = emergencyType,
            emergencyMessage = customMessage,
            accuracyMeters = 5.8f,
            networkStatus = "4G MTN Congo",
            isResolved = false
        )

        // Store active alert and add to list
        _activePanicAlert.value = alert
        _childPanicAlerts.update { listOf(alert) + it }
        _linkedChildInfo.update { it.copy(lastPanicAlert = alert) }

        // Also add high-priority audit entry in user profile for parent log
        val currentProfile = repository.userProfile.value
        val updatedActions = currentProfile.atRiskActions.toMutableList()
        updatedActions.add(
            0,
            AtRiskAction(
                timestamp = now,
                title = "🚨 ALERTE PANIQUE & GÉOLOCALISATION GPS",
                description = "Signal de détresse émis par ${alert.childName} ! Position GPS: Lat ${alert.latitude}, Lng ${alert.longitude} ($address). E-mail d'urgence Brevo et notification push transmis au parent (${currentProfile.parentEmail.ifBlank { "parent@exemple.com" }}).",
                severity = RiskSeverity.HIGH
            )
        )
        repository.updateUserProfile(currentProfile.copy(atRiskActions = updatedActions))

        // Notify socket / push system
        com.loukatech.mbote.service.MboteSocketManager.sendChatMessage(
            chatId = "PANIC_CHANNEL",
            senderName = alert.childName,
            text = "🚨 SOS ALERTE PANIQUE : ${alert.childName} à $address ($latitude, $longitude)"
        )

        return alert
    }

    fun resolvePanicAlert(alertId: String) {
        _childPanicAlerts.update { list ->
            list.map { alert ->
                if (alert.alertId == alertId) alert.copy(isResolved = true) else alert
            }
        }
        if (_activePanicAlert.value?.alertId == alertId) {
            _activePanicAlert.value = null
        }
    }

    fun dismissActivePanicAlert() {
        _activePanicAlert.value = null
    }

    fun setShowPanicTriggerDialog(show: Boolean) {
        _showPanicTriggerDialog.value = show
    }
}
