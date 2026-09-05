package com.loukatech.mbote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.components.*
import com.loukatech.mbote.ui.screens.*
import com.loukatech.mbote.ui.theme.MBoteTheme
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft
import com.loukatech.mbote.ui.viewmodel.MboteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MboteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initializeCache(applicationContext.filesDir)
        com.loukatech.mbote.service.MboteI18nService.initialize(this)
        com.loukatech.mbote.service.MboteNotificationManager.initNotificationChannels(this)
        enableEdgeToEdge()

        setContent {
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val notifications by viewModel.notifications.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
            val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
            val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
            val activeMeetingRoom by viewModel.activeMeetingRoom.collectAsStateWithLifecycle()
            val showJobsScreen by viewModel.showJobsScreen.collectAsStateWithLifecycle()
            val showShortVideosScreen by viewModel.showShortVideosScreen.collectAsStateWithLifecycle()
            val shortVideosReturnTo by viewModel.shortVideosReturnTo.collectAsStateWithLifecycle()
            val shortVideos by viewModel.shortVideos.collectAsStateWithLifecycle()
            val showCreateShortVideoDialog by viewModel.showCreateShortVideoDialog.collectAsStateWithLifecycle()

            val chats by viewModel.filteredChats.collectAsStateWithLifecycle()
            val statuses by viewModel.statuses.collectAsStateWithLifecycle()
            val calls by viewModel.calls.collectAsStateWithLifecycle()
            val newsPosts by viewModel.newsPosts.collectAsStateWithLifecycle()
            val channels by viewModel.channels.collectAsStateWithLifecycle()
            val meetings by viewModel.meetings.collectAsStateWithLifecycle()
            val jobs by viewModel.jobs.collectAsStateWithLifecycle()
            val discoverProfiles by viewModel.discoverProfiles.collectAsStateWithLifecycle()

            val unreadCount by viewModel.totalUnreadMessages.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val chatFilter by viewModel.chatFilter.collectAsStateWithLifecycle()

            // Dialogs and Sheet States
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("mbote_prefs", android.content.Context.MODE_PRIVATE) }
            val isBiometricEnabled = remember { sharedPrefs.getBoolean("biometric_enabled", false) }
            var isAppUnlocked by remember { mutableStateOf(!isBiometricEnabled) }

            if (isBiometricEnabled && !isAppUnlocked) {
                BiometricLockOverlay(
                    isTestMode = false,
                    onUnlockSuccess = { isAppUnlocked = true }
                )
            }

            val showProfileForUser = { name: String, avatar: String ->
                val isCelebrity = name == "Linda Bongo Ondimba" || name.contains("Linda", ignoreCase = true)
                val isCompany = name == "Journaliste MBoté" || name.contains("MBoté", ignoreCase = true) || name.contains("Congo Telecom", ignoreCase = true) || name.contains("MTN", ignoreCase = true)
                val bio = when {
                    isCelebrity -> "Avocate, philanthrope et fervente activiste pour l'éducation et l'autonomisation des femmes en Afrique Centrale. 🇬🇦✨"
                    isCompany -> "Votre compte professionnel officiel sur MBoté. Contactez-nous pour toute assistance ou information complémentaire."
                    else -> "Discutons sur MBoté ! Tous mes échanges sont chiffrés de bout en bout et hautement sécurisés. 🔒💬"
                }
                val city = when {
                    isCelebrity -> "Libreville"
                    isCompany -> "Brazzaville"
                    else -> "Brazzaville"
                }
                val subtitle = when {
                    isCelebrity -> "Personnalité publique"
                    isCompany -> "Compte Entreprise"
                    else -> "Ami(e) • En ligne"
                }

                val displayData = ProfileDisplayData(
                    id = name,
                    name = name,
                    avatar = avatar,
                    subtitle = subtitle,
                    bio = bio,
                    city = city,
                    isCelebrity = isCelebrity,
                    isCompany = isCompany,
                    mutualFriends = if (isCelebrity) 12 else 4
                )
                viewModel.setSelectedUserProfile(displayData)
            }

            val showNotificationsSheet by viewModel.showNotificationsSheet.collectAsStateWithLifecycle()
            val showQuickActionsMenu by viewModel.showQuickActionsMenu.collectAsStateWithLifecycle()
            val showNewChatDialog by viewModel.showNewChatDialog.collectAsStateWithLifecycle()
            val showCreateGroupDialog by viewModel.showCreateGroupDialog.collectAsStateWithLifecycle()
            val showCreateChannelDialog by viewModel.showCreateChannelDialog.collectAsStateWithLifecycle()
            val showJoinByInviteLinkDialog by viewModel.showJoinByInviteLinkDialog.collectAsStateWithLifecycle()
            val showMastaSheet by viewModel.showMastaSheet.collectAsStateWithLifecycle()
            val showContactsSyncSheet by viewModel.showContactsSyncSheet.collectAsStateWithLifecycle()
            val contactsSyncState by viewModel.contactsSyncState.collectAsStateWithLifecycle()
            val syncedContacts by viewModel.syncedContacts.collectAsStateWithLifecycle()
            val showAddStatusDialog by viewModel.showAddStatusDialog.collectAsStateWithLifecycle()
            val showNewMeetingDialog by viewModel.showNewMeetingDialog.collectAsStateWithLifecycle()
            val showEditProfileDialog by viewModel.showEditProfileDialog.collectAsStateWithLifecycle()
            val showAronQuestionsSheet by viewModel.showAronQuestionsSheet.collectAsStateWithLifecycle()
            val showPollDialog by viewModel.showPollDialog.collectAsStateWithLifecycle()
            val showLocationSheet by viewModel.showLocationSheet.collectAsStateWithLifecycle()
            val showPaymentSheet by viewModel.showPaymentSheet.collectAsStateWithLifecycle()
            val isPartnerTyping by viewModel.isPartnerTyping.collectAsStateWithLifecycle()
            val socketTypingMap by viewModel.socketTypingMap.collectAsStateWithLifecycle()
            val blockedContactIds by viewModel.blockedContactIds.collectAsStateWithLifecycle()
            val showStorageDialog by viewModel.showStorageDialog.collectAsStateWithLifecycle()
            val showAccessRequestDialog by viewModel.showAccessRequestDialog.collectAsStateWithLifecycle()
            val showEyeContactDialog by viewModel.showEyeContactDialog.collectAsStateWithLifecycle()
            val eyeContactSeconds by viewModel.eyeContactSeconds.collectAsStateWithLifecycle()

            val selectedPostForComments by viewModel.selectedPostForComments.collectAsStateWithLifecycle()
            val selectedStatusStory by viewModel.selectedStatusStory.collectAsStateWithLifecycle()
            val selectedCreatorProfile by viewModel.selectedCreatorProfile.collectAsStateWithLifecycle()
            val selectedUserProfile by viewModel.selectedUserProfile.collectAsStateWithLifecycle()
            val userGiftState by viewModel.userGiftState.collectAsStateWithLifecycle()
            val linkedChild by viewModel.linkedChildInfo.collectAsStateWithLifecycle()
            val parentChildLinkState by viewModel.parentChildLinkState.collectAsStateWithLifecycle()
            val childApps by viewModel.childInstalledApps.collectAsStateWithLifecycle()
            val panicAlerts by viewModel.childPanicAlerts.collectAsStateWithLifecycle()
            val activePanicAlert by viewModel.activePanicAlert.collectAsStateWithLifecycle()

            val isMeetingMuted by viewModel.isMutedInMeeting.collectAsStateWithLifecycle()
            val isMeetingVideoOff by viewModel.isVideoOffInMeeting.collectAsStateWithLifecycle()
            val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
            val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
            val isDataSyncing by viewModel.isDataSyncing.collectAsStateWithLifecycle()
            val publicationError by viewModel.publicationError.collectAsStateWithLifecycle()
            val showLoginScreen by viewModel.showLoginScreen.collectAsStateWithLifecycle()
            val showAdminLoginDialog by viewModel.showAdminLoginDialog.collectAsStateWithLifecycle()
            val showForgotPasswordDialog by viewModel.showForgotPasswordDialog.collectAsStateWithLifecycle()
            var usageTrackingStarted by remember { mutableStateOf(false) }
            val onboardingCompletedKey = "mbote.onboarding.completed.v1"
            var showLaunchSplash by remember { mutableStateOf(true) }
            var showLaunchOnboarding by remember { mutableStateOf(false) }

            val isSystemDark = isSystemInDarkTheme()

            LaunchedEffect(isAuthenticated) {
                if (isAuthenticated && !usageTrackingStarted) {
                    com.loukatech.mbote.service.AppUsageTrackingService.start(applicationContext)
                    usageTrackingStarted = true
                } else if (!isAuthenticated && usageTrackingStarted) {
                    com.loukatech.mbote.service.AppUsageTrackingService.stop(applicationContext)
                    usageTrackingStarted = false
                }
            }

            LaunchedEffect(isAuthenticated, showLoginScreen) {
                if (showLaunchSplash) {
                    delay(700)
                    showLaunchSplash = false
                }
                showLaunchOnboarding = (!isAuthenticated || showLoginScreen) &&
                    !sharedPrefs.getBoolean(onboardingCompletedKey, false)
            }

            val finishLaunchOnboarding = {
                sharedPrefs.edit().putBoolean(onboardingCompletedKey, true).apply()
                showLaunchOnboarding = false
            }

            LaunchedEffect(publicationError) {
                publicationError?.let {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                    viewModel.clearPublicationError()
                }
            }
            val isDarkTheme = when (userProfile.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemDark
            }

            MBoteTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        showLaunchSplash -> MboteLaunchSplashScreen()
                        showLaunchOnboarding && (!isAuthenticated || showLoginScreen) -> MboteLaunchOnboardingScreen(
                            onFinish = finishLaunchOnboarding,
                            onSkip = finishLaunchOnboarding
                        )
                        else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideScreen = maxWidth >= 720.dp
                        when {
                        // 0. Login & Auth Screen (if logged out or explicitly opened)
                        !isAuthenticated || showLoginScreen -> {
                            LoginScreen(
                                onLoginSuccess = { viewModel.setShowLoginScreen(false) },
                                onLoginSubmit = { email, pass -> viewModel.login(email, pass) },
                                onRegisterSubmit = { request -> viewModel.register(request) },
                                onVerifyRegistrationOtp = { pendingUserId, otp -> viewModel.verifyRegistrationOtp(pendingUserId, otp) },
                                onVerifyLoginOtp = { pendingUserId, otp -> viewModel.verifyLoginOtp(pendingUserId, otp) },
                                onLoadRegistrationConfig = { viewModel.getRegistrationPublicConfig() },
                                onGoogleLoginSubmit = { viewModel.loginWithGoogle() },
                                onGitHubLoginSubmit = { viewModel.loginWithGitHub() },
                                onRequestResetCode = { email -> viewModel.requestPasswordReset(email) },
                                onConfirmResetPassword = { email, code, pass -> viewModel.confirmPasswordReset(email, code, pass) },
                                onAdminLoginSubmit = { key, email, pass -> viewModel.loginAdmin(key, email, pass) },
                                onSaveServerConfig = { url -> viewModel.setBackendServerUrl(url) }
                            )
                        }

                        // 1. Active Call Screen
                        activeCall != null -> {
                            CallViewScreen(
                                call = activeCall!!,
                                onEndCall = { duration -> viewModel.endCall(duration) }
                            )
                        }

                        // 2. Active Meeting Room Screen
                        activeMeetingRoom != null -> {
                            MeetingRoomScreen(
                                meeting = activeMeetingRoom!!,
                                isMuted = isMeetingMuted,
                                isVideoOff = isMeetingVideoOff,
                                onToggleMute = { viewModel.toggleMeetingMute() },
                                onToggleVideo = { viewModel.toggleMeetingVideo() },
                                onLeaveMeeting = { viewModel.leaveMeeting() }
                            )
                        }

                        // 3. Chat Detail Screen
                        activeChat != null && !isWideScreen -> {
                            val partnerContactId = activeChat!!.participants.firstOrNull { it.id != "user_me" }?.id ?: activeChat!!.id
                            val isChatBlocked = blockedContactIds.contains(partnerContactId) || blockedContactIds.contains(activeChat!!.id)
                            val isFriend = viewModel.isFriend(activeChat!!.name)

                            ChatDetailScreen(
                                chat = activeChat!!,
                                currentUserAvatar = userProfile.avatar,
                                isPartnerTyping = isPartnerTyping,
                                isBlocked = isChatBlocked,
                                isFriend = isFriend,
                                onBackClick = { viewModel.closeChat() },
                                onAudioCallClick = {
                                    viewModel.startCall(activeChat!!.name, activeChat!!.avatar, false)
                                },
                                onVideoCallClick = {
                                    viewModel.startCall(activeChat!!.name, activeChat!!.avatar, true)
                                },
                                onSendMessage = { text, replyTo ->
                                    viewModel.sendMessage(activeChat!!.id, text, replyTo)
                                },
                                onSendVoiceMessage = { audioPath, durationSec, replyTo ->
                                    viewModel.sendVoiceMessage(context, activeChat!!.id, audioPath, durationSec, replyTo)
                                },
                                onSendMediaMessage = { mediaUrl, isVideo, caption ->
                                    viewModel.sendMediaAttachment(context, activeChat!!.id, mediaUrl, isVideo, caption)
                                },
                                onSearchGiphy = { query, stickers -> viewModel.searchGiphy(query, stickers) },
                                onToggleBlock = { shouldBlock ->
                                    if (shouldBlock) {
                                        viewModel.blockContact(partnerContactId)
                                    } else {
                                        viewModel.unblockContact(partnerContactId)
                                        viewModel.unblockContact(activeChat!!.id)
                                    }
                                },
                                onReaction = { msgId, emoji ->
                                    viewModel.addReaction(activeChat!!.id, msgId, emoji)
                                },
                                onDeleteMessage = { msgId ->
                                    viewModel.deleteMessage(activeChat!!.id, msgId)
                                },
                                onOpenAronQuestions = {
                                    viewModel.setShowAronQuestionsSheet(true)
                                },
                                onOpenPollDialog = {
                                    viewModel.setShowPollDialog(true)
                                },
                                onOpenLocationPicker = {
                                    viewModel.setShowLocationSheet(true)
                                },
                                onOpenPaymentSheet = {
                                    viewModel.setShowPaymentSheet(true)
                                },
                                onVotePoll = { msgId, optionId ->
                                    viewModel.votePoll(activeChat!!.id, msgId, optionId)
                                },
                                onTranslateMessage = { msgId, lang ->
                                    viewModel.translateMessage(activeChat!!.id, msgId, lang)
                                },
                                onSetDisappearingTimer = { sec ->
                                    viewModel.setChatDisappearingTimer(activeChat!!.id, sec)
                                },
                                onUpdateWallpaper = { colorHex, imgUrl ->
                                    viewModel.setChatWallpaper(activeChat!!.id, colorHex, imgUrl)
                                },
                                onUserComposing = { isComposing ->
                                    viewModel.onUserTypingChanged(activeChat!!.id, isComposing)
                                },
                                onProfileClick = { showProfileForUser(activeChat!!.name, activeChat!!.avatar) }
                            )
                        }

                        // 4. Jobs Screen (app?tab=emplois)
                        showJobsScreen -> {
                            JobsScreen(
                                jobs = jobs,
                                onBackClick = { viewModel.setShowJobsScreen(false) },
                                onLikeJob = { jobId -> viewModel.toggleJobLike(jobId) },
                                onBookmarkJob = { jobId -> viewModel.toggleJobBookmark(jobId) },
                                onApplyJob = { jobId -> viewModel.applyToJob(jobId) },
                                onPostJob = { title, company, location, domain, contractType, workMode, salary, description, reqs, bens ->
                                    viewModel.postJobOffer(
                                        title = title,
                                        company = company,
                                        location = location,
                                        domain = domain,
                                        contractType = contractType,
                                        workMode = workMode,
                                        salary = salary,
                                        description = description,
                                        requirements = reqs,
                                        benefits = bens
                                    )
                                },
                                onShareJob = { job ->
                                    activeChat?.let { chat ->
                                        viewModel.sendMessage(
                                            chatId = chat.id,
                                            text = "💼 Opportunité d'emploi MBoté : ${job.title} chez ${job.company} (${job.salary}) - ${job.location}"
                                        )
                                    }
                                },
                                onReportJob = { job -> viewModel.submitReport("Offre Emploi", job.title) }
                            )
                        }

                        // 5. Short Videos Screen (short-videos?returnTo=...)
                        showShortVideosScreen -> {
                            ShortVideosScreen(
                                viewModel = viewModel,
                                returnTo = shortVideosReturnTo,
                                onBack = { viewModel.closeShortVideos() }
                            )
                        }

                        // 6. Main App Shell with Tabs
                        else -> {
                            val isImmersiveShorts = currentTab == NavigationTab.SHORTS
                            Scaffold(
                                topBar = {
                                    if (!isImmersiveShorts) {
                                        Column {
                                            MboteTopBar(
                                                currentTab = currentTab,
                                                userProfile = userProfile,
                                                unreadNotificationsCount = notifications.count { !it.isRead },
                                                onSearchClick = {
                                                    viewModel.setTab(NavigationTab.MESSAGES)
                                                },
                                                onNotificationsClick = {
                                                    viewModel.setShowNotificationsSheet(true)
                                                },
                                                onJobsClick = {
                                                    viewModel.setShowJobsScreen(true)
                                                },
                                                onMeetingsClick = {
                                                    viewModel.setTab(NavigationTab.MEETINGS)
                                                },
                                                onProfileClick = {
                                                    viewModel.setTab(NavigationTab.SETTINGS)
                                                }
                                            )

                                             if (isOffline) {
                                                Surface(
                                                    color = Color(0xFFD97706),
                                                    contentColor = Color.White,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.toggleOfflineMode() }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.WifiOff,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = "Mode Hors Ligne • Messages, publications et vidéos consultables hors connexion",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Text(
                                                            text = "Reconnecter",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                bottomBar = {
                                    if (!isImmersiveShorts) {
                                        MboteBottomBar(
                                            currentTab = currentTab,
                                            unreadMessagesCount = unreadCount,
                                            onTabSelected = { tab ->
                                                viewModel.setTab(tab)
                                            }
                                        )
                                    }
                                }
                             ) { paddingValues ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isImmersiveShorts) PaddingValues(0.dp) else paddingValues)
                                ) {
                                    when (currentTab) {
                                        NavigationTab.MESSAGES -> {
                                            if (isWideScreen) {
                                                Row(modifier = Modifier.fillMaxSize()) {
                                                    Box(modifier = Modifier.width(360.dp).fillMaxHeight()) {
                                                        MessagesScreen(
                                                            chats = chats,
                                                            statuses = statuses,
                                                            userProfile = userProfile,
                                                            searchQuery = searchQuery,
                                                            selectedFilter = chatFilter,
                                                            isSyncing = isDataSyncing,
                                                            socketTypingMap = socketTypingMap,
                                                            onSearchChange = { viewModel.setSearchQuery(it) },
                                                            onFilterChange = { viewModel.setChatFilter(it) },
                                                            onChatClick = { chatId -> viewModel.openChat(chatId) },
                                                            onNewChatClick = { viewModel.setShowQuickActionsMenu(true) },
                                                            onOpenContactsSync = { viewModel.setShowContactsSyncSheet(true) },
                                                            onJoinByLinkClick = { viewModel.setShowJoinByInviteLinkDialog(true) },
                                                            onStatusClick = { status -> viewModel.setSelectedStatusStory(status) },
                                                            onAddStatusClick = { viewModel.setShowAddStatusDialog(true) },
                                                            onConfirmDesktopQr = { payload -> viewModel.confirmDesktopQrLogin(payload) },
                                                            onProfileClick = { name, avatar -> showProfileForUser(name, avatar) }
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                    ) {
                                                        if (activeChat != null) {
                                                            val partnerContactId = activeChat!!.participants.firstOrNull { it.id != "user_me" }?.id ?: activeChat!!.id
                                                            val isChatBlocked = blockedContactIds.contains(partnerContactId) || blockedContactIds.contains(activeChat!!.id)
                                                            val isFriend = viewModel.isFriend(activeChat!!.name)
                                                            ChatDetailScreen(
                                                                chat = activeChat!!,
                                                                currentUserAvatar = userProfile.avatar,
                                                                isPartnerTyping = isPartnerTyping,
                                                                isBlocked = isChatBlocked,
                                                                isFriend = isFriend,
                                                                onBackClick = { viewModel.closeChat() },
                                                                onAudioCallClick = { viewModel.startCall(activeChat!!.name, activeChat!!.avatar, false) },
                                                                onVideoCallClick = { viewModel.startCall(activeChat!!.name, activeChat!!.avatar, true) },
                                                                onSendMessage = { text, replyTo -> viewModel.sendMessage(activeChat!!.id, text, replyTo) },
                                                                onSendVoiceMessage = { audioPath, durationSec, replyTo -> viewModel.sendVoiceMessage(context, activeChat!!.id, audioPath, durationSec, replyTo) },
                                                                onSendMediaMessage = { mediaUrl, isVideo, caption -> viewModel.sendMediaAttachment(context, activeChat!!.id, mediaUrl, isVideo, caption) },
                                                                onSearchGiphy = { query, stickers -> viewModel.searchGiphy(query, stickers) },
                                                                onToggleBlock = { shouldBlock ->
                                                                    if (shouldBlock) {
                                                                        viewModel.blockContact(partnerContactId)
                                                                    } else {
                                                                        viewModel.unblockContact(partnerContactId)
                                                                        viewModel.unblockContact(activeChat!!.id)
                                                                    }
                                                                },
                                                                onReaction = { msgId, emoji -> viewModel.addReaction(activeChat!!.id, msgId, emoji) },
                                                                onDeleteMessage = { msgId -> viewModel.deleteMessage(activeChat!!.id, msgId) },
                                                                onOpenAronQuestions = { viewModel.setShowAronQuestionsSheet(true) },
                                                                onOpenPollDialog = { viewModel.setShowPollDialog(true) },
                                                                onOpenLocationPicker = { viewModel.setShowLocationSheet(true) },
                                                                onOpenPaymentSheet = { viewModel.setShowPaymentSheet(true) },
                                                                onVotePoll = { msgId, optionId -> viewModel.votePoll(activeChat!!.id, msgId, optionId) },
                                                                onTranslateMessage = { msgId, lang -> viewModel.translateMessage(activeChat!!.id, msgId, lang) },
                                                                onSetDisappearingTimer = { sec -> viewModel.setChatDisappearingTimer(activeChat!!.id, sec) },
                                                                onUpdateWallpaper = { colorHex, imgUrl -> viewModel.setChatWallpaper(activeChat!!.id, colorHex, imgUrl) },
                                                                onUserComposing = { isComposing -> viewModel.onUserTypingChanged(activeChat!!.id, isComposing) },
                                                                onProfileClick = { showProfileForUser(activeChat!!.name, activeChat!!.avatar) }
                                                            )
                                                        } else {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                                                    .padding(32.dp),
                                                                verticalArrangement = Arrangement.Center,
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Favorite,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                                    modifier = Modifier.size(72.dp)
                                                                )
                                                                Spacer(modifier = Modifier.height(16.dp))
                                                                Text(
                                                                    text = "Bienvenue sur MBoté",
                                                                    fontSize = 20.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Text(
                                                                    text = "Sélectionnez une discussion à gauche pour commencer à échanger en toute sécurité.",
                                                                    fontSize = 14.sp,
                                                                    textAlign = TextAlign.Center,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                MessagesScreen(
                                                    chats = chats,
                                                    statuses = statuses,
                                                    userProfile = userProfile,
                                                    searchQuery = searchQuery,
                                                    selectedFilter = chatFilter,
                                                    isSyncing = isDataSyncing,
                                                    socketTypingMap = socketTypingMap,
                                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                                    onFilterChange = { viewModel.setChatFilter(it) },
                                                    onChatClick = { chatId -> viewModel.openChat(chatId) },
                                                    onNewChatClick = { viewModel.setShowQuickActionsMenu(true) },
                                                    onOpenContactsSync = { viewModel.setShowContactsSyncSheet(true) },
                                                    onJoinByLinkClick = { viewModel.setShowJoinByInviteLinkDialog(true) },
                                                    onStatusClick = { status -> viewModel.setSelectedStatusStory(status) },
                                                    onAddStatusClick = { viewModel.setShowAddStatusDialog(true) },
                                                    onConfirmDesktopQr = { payload -> viewModel.confirmDesktopQrLogin(payload) },
                                                    onProfileClick = { name, avatar -> showProfileForUser(name, avatar) }
                                                )
                                            }
                                        }
 
                                        NavigationTab.DISCOVER -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxHeight()) {
                                                    DiscoverScreen(
                                                        profiles = discoverProfiles,
                                                        onOpenProfile = { profile ->
                                                            viewModel.startChatWithProfile(profile)
                                                        },
                                                        onSendGreeting = { profile ->
                                                            viewModel.sendMboteGreeting(profile)
                                                        },
                                                        onStartChat = { profile ->
                                                            viewModel.startChatWithProfile(profile)
                                                        },
                                                        onOpenAronQuestions = {
                                                            viewModel.setShowAronQuestionsSheet(true)
                                                        },
                                                        onStartEyeContact = {
                                                            viewModel.startEyeContactTimer()
                                                        }
                                                    )
                                                }
                                            }
                                        }
 
                                        NavigationTab.CALLS -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxHeight()) {
                                                    CallsScreen(
                                                        calls = calls,
                                                        onStartCall = { name, avatar, isVideo ->
                                                            viewModel.startCall(name, avatar, isVideo)
                                                        },
                                                        onOpenChat = { name ->
                                                            viewModel.openChatByName(name)
                                                        },
                                                        syncedContacts = syncedContacts
                                                    )
                                                }
                                            }
                                        }
 
                                        NavigationTab.ACTUS -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxHeight()) {
                                                    ActusScreen(
                                                        newsPosts = newsPosts,
                                                        statuses = statuses,
                                                        currentUserName = userProfile.name,
                                                        shortVideos = shortVideos,
                                                        channels = channels,
                                                        isSyncing = isDataSyncing,
                                                        onLikeClick = { postId -> viewModel.toggleNewsLike(postId) },
                                                        onShareClick = { postId -> viewModel.shareNewsPost(postId) },
                                                        onCommentClick = { post -> viewModel.setSelectedPostForComments(post) },
                                                        onStatusClick = { status -> viewModel.setSelectedStatusStory(status) },
                                                        onAddStatusClick = { viewModel.setShowAddStatusDialog(true) },
                                                        onOpenShortVideos = { viewModel.openShortVideos("/app?tab=actus") },
                                                        onOpenCreatorProfile = { video -> viewModel.setSelectedCreatorProfile(video) },
                                                        onCreateShortVideoClick = { viewModel.setShowCreateShortVideoDialog(true) },
                                                        onCreateChannel = { name, description, isPublic, initialPost ->
                                                            viewModel.createChannel(name, description, isPublic, initialPost)
                                                        },
                                                        onToggleChannelSubscription = { channelId, isSubscribed ->
                                                            viewModel.toggleChannelSubscription(channelId, isSubscribed)
                                                        },
                                                        onPublishNews = { title, content, mediaUri, category, mediaType ->
                                                            viewModel.addNewsPost(context, title, content, mediaUri, category, mediaType)
                                                        },
                                                        onRefresh = { viewModel.triggerDataSync() },
                                                        onAuthorProfileClick = { name, avatar -> showProfileForUser(name, avatar) },
                                                        onReportContent = { type, target -> viewModel.submitReport(type, target) }
                                                    )
                                                }
                                            }
                                        }
 
                                        NavigationTab.SHORTS -> {
                                            ShortVideosScreen(
                                                viewModel = viewModel,
                                                returnTo = "/app?tab=actus",
                                                onBack = { viewModel.setTab(NavigationTab.ACTUS) }
                                            )
                                        }
 
                                        NavigationTab.MASTA -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxHeight()) {
                                                    MastaScreen(
                                                        viewModel = viewModel,
                                                        onOpenChat = { name -> viewModel.openChatByName(name) },
                                                        onStartCall = { name, avatar, isVideo -> viewModel.startCall(name, avatar, isVideo) },
                                                        onOpenSettings = { viewModel.setTab(NavigationTab.SETTINGS) },
                                                        onOpenProfile = { mastaUser -> showProfileForUser(mastaUser.name, mastaUser.avatar) }
                                                    )
                                                }
                                            }
                                        }
 
                                        NavigationTab.MEETINGS -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxHeight()) {
                                                    MeetingsScreen(
                                                        meetings = meetings,
                                                        onNewMeetingClick = { viewModel.setShowNewMeetingDialog(true) },
                                                        onJoinMeetingClick = { meeting -> viewModel.startMeeting(meeting) }
                                                    )
                                                }
                                            }
                                        }
 
                                        NavigationTab.SETTINGS -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxHeight()) {
                                                    SettingsScreen(
                                                        userProfile = userProfile,
                                                        onEditProfileClick = { viewModel.setShowEditProfileDialog(true) },
                                                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                                                        onThemeModeChange = { viewModel.setThemeMode(it) },
                                                        onToggleNotifications = { viewModel.toggleNotifications() },
                                                        onToggleOfflineMode = { viewModel.toggleOfflineMode() },
                                                        onJobsClick = { viewModel.setShowJobsScreen(true) },
                                                        onSyncContactsClick = { viewModel.setShowContactsSyncSheet(true) },
                                                        onAronQuestionsClick = { viewModel.setShowAronQuestionsSheet(true) },
                                                        onEyeContactClick = { viewModel.startEyeContactTimer() },
                                                        onStorageClick = { viewModel.setShowStorageDialog(true) },
                                                        onShortVideosClick = { viewModel.openShortVideos("/app?tab=settings") },
                                                        onLoginClick = { viewModel.setShowLoginScreen(true) },
                                                        onAdminClick = { viewModel.setShowAdminLoginDialog(true) },
                                                        onLogoutClick = { viewModel.logout() },
                                                        onDeleteAccountClick = { callback -> viewModel.deleteMyAccount(callback) },
                                                        onUpdateBio = { newBio -> viewModel.updateBio(newBio) },
                                                        onAccessRequestClick = { viewModel.setShowAccessRequestDialog(true) },
                                                        userGiftState = userGiftState,
                                                        onCashout = { amount, provider, phone ->
                                                            viewModel.cashoutVirtualGifts(amount, provider, phone)
                                                        },
                                                        onBuyBundle = { bundle, provider ->
                                                            viewModel.buyGiftBundle(bundle, provider)
                                                        },
                                                        onBuySingleGift = { gift, count, provider ->
                                                            viewModel.buySingleGift(gift, count, provider)
                                                        },
                                                        onLanguageChange = { lang ->
                                                            viewModel.updateLanguage(lang)
                                                        },
                                                        onCurrencyChange = { curr ->
                                                            viewModel.updateCurrency(curr)
                                                        },
                                                        onSaveParentalControl = { active, email, night, time, curfew, school, isChildLinked ->
                                                            viewModel.updateParentalControl(active, email, night, time, curfew, school, isChildLinked)
                                                        },
                                                        onSendSosAlert = { email, reason ->
                                                            viewModel.sendSosAlert(email, reason)
                                                        },
                                                        onTogglePremium = { isPremium ->
                                                            viewModel.togglePremiumStatus(isPremium)
                                                        },
                                                        linkedChild = linkedChild,
                                                        onUpgradeParentalPlan = { planId, _ ->
                                                            viewModel.updateParentalSubscriptionPlan(planId, planId)
                                                        },
                                                        onProcessScannedQr = { payload ->
                                                            viewModel.processParentChildQrCode(payload)
                                                        },
                                                        childApps = childApps,
                                                        onToggleAppBlocked = { pkg, blocked ->
                                                            viewModel.toggleChildAppBlocked(pkg, blocked)
                                                        },
                                                        onToggleAllApps = { blocked, category ->
                                                            viewModel.toggleAllChildApps(blocked, category)
                                                        },
                                                        onToggleAppSchoolRestriction = { pkg, restricted ->
                                                            viewModel.setChildAppSchoolRestriction(pkg, restricted)
                                                        },
                                                        panicAlerts = panicAlerts,
                                                        activePanicAlert = activePanicAlert,
                                                        onTriggerPanicAlert = { lat, lng, addr, type, msg ->
                                                            viewModel.triggerChildPanicAlert(lat, lng, addr, type, msg)
                                                        },
                                                        onResolvePanicAlert = { alertId ->
                                                            viewModel.resolvePanicAlert(alertId)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dialogs & Sheets

                    if (showNotificationsSheet) {
                        NotificationsCenterSheet(
                            notifications = notifications,
                            onDismiss = { viewModel.setShowNotificationsSheet(false) },
                            onNotificationClick = { notification ->
                                viewModel.markNotificationAsRead(notification.id)
                                viewModel.setShowNotificationsSheet(false)
                                when (notification.type) {
                                    NotificationType.MESSAGE -> {
                                        if (notification.targetId != null) {
                                            viewModel.openChat(notification.targetId)
                                        } else {
                                            viewModel.setTab(NavigationTab.MESSAGES)
                                        }
                                    }
                                    NotificationType.JOB_APPLICATION -> {
                                        viewModel.setShowJobsScreen(true)
                                    }
                                    NotificationType.VIDEO_LIKE -> {
                                        viewModel.openShortVideos("/app?tab=dashboard")
                                    }
                                    NotificationType.GIFT_RECEIVED -> {
                                        viewModel.setShowPaymentSheet(true)
                                    }
                                    NotificationType.LIVE_MESSAGE, NotificationType.LIVE_BROADCAST -> {
                                        viewModel.setTab(NavigationTab.SHORTS)
                                    }
                                    NotificationType.SYSTEM -> {}
                                }
                            },
                            onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
                            onClearAll = { viewModel.clearAllNotifications() },
                            onSimulateFcmPush = { type ->
                                viewModel.sendFcmTestNotification(type, context)
                            }
                        )
                    }

                    if (showQuickActionsMenu) {
                        MboteQuickActionsSheet(
                            onDismiss = { viewModel.setShowQuickActionsMenu(false) },
                            onNewMessageClick = { viewModel.setShowNewChatDialog(true) },
                            onCreateGroupClick = { viewModel.setShowCreateGroupDialog(true) },
                            onCreateChannelClick = { viewModel.setShowCreateChannelDialog(true) },
                            onMastaClick = { viewModel.setShowMastaSheet(true) }
                        )
                    }

                    if (showNewChatDialog) {
                        NewChatDialog(
                            onDismiss = { viewModel.setShowNewChatDialog(false) },
                            onConfirm = { name, message, isGroup ->
                                viewModel.createChat(name, message, isGroup)
                            },
                            onPickFromContacts = {
                                viewModel.setShowNewChatDialog(false)
                                viewModel.setShowMastaSheet(true)
                            }
                        )
                    }

                    if (showCreateGroupDialog) {
                        CreateGroupDialog(
                            contacts = syncedContacts,
                            onDismiss = { viewModel.setShowCreateGroupDialog(false) },
                            onConfirm = { groupName, description, members, avatar, initialMessage ->
                                viewModel.createGroup(groupName, description, members, avatar, initialMessage)
                            }
                        )
                    }

                    if (showCreateChannelDialog) {
                        CreateChannelDialog(
                            onDismiss = { viewModel.setShowCreateChannelDialog(false) },
                            onConfirm = { channelName, description, isPublic, initialPost ->
                                viewModel.createChannel(channelName, description, isPublic, initialPost)
                            }
                        )
                    }

                    if (showMastaSheet) {
                        MastaHubSheet(
                            contacts = syncedContacts,
                            onDismiss = { viewModel.setShowMastaSheet(false) },
                            onStartChat = { contact ->
                                viewModel.startChatWithContact(contact)
                            },
                            onVoiceCall = { contact ->
                                viewModel.startCall(contact.name, contact.avatarUrl ?: "", false)
                            },
                            onVideoCall = { contact ->
                                viewModel.startCall(contact.name, contact.avatarUrl ?: "", true)
                            },
                            onAddNewMasta = { name, phone, isMboteUser ->
                                viewModel.addNewMasta(name, phone, isMboteUser)
                            }
                        )
                    }

                    if (showCreateShortVideoDialog) {
                        CreateShortVideoDialog(
                            onDismiss = { viewModel.setShowCreateShortVideoDialog(false) },
                            onPublish = { videoUri, duration, caption, hashtags, musicTitle, musicArtist, thumbnailUrl, location ->
                                viewModel.createShortVideo(
                                    context = context,
                                    videoUri = videoUri,
                                    durationSeconds = duration,
                                    caption = caption,
                                    hashtags = hashtags,
                                    musicTitle = musicTitle,
                                    musicArtist = musicArtist,
                                    thumbnailUrl = thumbnailUrl,
                                    location = location
                                )
                            }
                        )
                    }

                    if (showContactsSyncSheet) {
                        ContactsSyncSheet(
                            contacts = syncedContacts,
                            onSyncNow = { viewModel.syncPhoneContacts() },
                            onContactSelected = { contact ->
                                viewModel.startChatWithContact(contact)
                            },
                            onAudioCallClick = { contact ->
                                viewModel.setShowContactsSyncSheet(false)
                                viewModel.startCall(contact.name, contact.avatarUrl ?: "", false)
                            },
                            onDismiss = { viewModel.setShowContactsSyncSheet(false) },
                            blockedContactIds = blockedContactIds,
                            onBlockContact = { contactId -> viewModel.blockContact(contactId) },
                            onUnblockContact = { contactId -> viewModel.unblockContact(contactId) }
                        )
                    }

                    if (showAronQuestionsSheet) {
                        AronQuestionsSheet(
                            onDismiss = { viewModel.setShowAronQuestionsSheet(false) },
                            onSelectQuestion = { question ->
                                if (activeChat != null) {
                                    viewModel.sendAronQuestion(activeChat!!.id, question)
                                } else {
                                    // Start a chat with AI Luna to explore question
                                    viewModel.openChat("chat_luna")
                                    viewModel.sendAronQuestion("chat_luna", question)
                                }
                            },
                            onStartEyeContactExercise = {
                                viewModel.setShowAronQuestionsSheet(false)
                                viewModel.startEyeContactTimer()
                            }
                        )
                    }

                    if (showPollDialog && activeChat != null) {
                        PollDialog(
                            onDismiss = { viewModel.setShowPollDialog(false) },
                            onCreatePoll = { question, options, isMultipleChoice ->
                                viewModel.sendPoll(activeChat!!.id, question, options, isMultipleChoice)
                            }
                        )
                    }

                    if (showLocationSheet && activeChat != null) {
                        LocationPickerSheet(
                            onDismiss = { viewModel.setShowLocationSheet(false) },
                            onSendLocation = { placeName, lat, lng, isLive, durationMin ->
                                viewModel.sendLocation(activeChat!!.id, placeName, lat, lng, isLive, durationMin)
                            }
                        )
                    }

                    if (showPaymentSheet && activeChat != null) {
                        PaymentSheet(
                            recipientName = activeChat!!.name,
                            onDismiss = { viewModel.setShowPaymentSheet(false) },
                            onSendPayment = { amount, provider, note, isRequest ->
                                viewModel.sendPaymentTransfer(activeChat!!.id, amount, provider, note, isRequest)
                            }
                        )
                    }

                    if (showJoinByInviteLinkDialog) {
                        com.loukatech.mbote.ui.components.JoinByInviteLinkDialog(
                            onDismiss = { viewModel.setShowJoinByInviteLinkDialog(false) },
                            onJoin = { url ->
                                viewModel.setShowJoinByInviteLinkDialog(false)
                            }
                        )
                    }

                    if (showStorageDialog) {
                        StorageManagerDialog(
                            onDismiss = { viewModel.setShowStorageDialog(false) },
                            chats = chats
                        )
                    }

                    if (showAccessRequestDialog) {
                        AccessRequestDialog(
                            onDismiss = { viewModel.setShowAccessRequestDialog(false) }
                        )
                    }

                    if (showEyeContactDialog) {
                        EyeContactTimerDialog(
                            secondsRemaining = eyeContactSeconds,
                            onDismiss = { viewModel.stopEyeContactTimer() }
                        )
                    }

                    if (showAddStatusDialog) {
                        AddStatusDialog(
                            onDismiss = { viewModel.setShowAddStatusDialog(false) },
                            onConfirm = { text, mediaUri, mediaType, background, visibility ->
                                viewModel.postStatus(context, text, mediaUri, mediaType, background, visibility)
                            }
                        )
                    }

                    if (showNewMeetingDialog) {
                        NewMeetingDialog(
                            onDismiss = { viewModel.setShowNewMeetingDialog(false) },
                            onConfirm = { title, duration ->
                                viewModel.createMeeting(title, duration)
                            }
                        )
                    }

                    if (showEditProfileDialog) {
                        EditProfileDialog(
                            currentProfile = userProfile,
                            onDismiss = { viewModel.setShowEditProfileDialog(false) },
                            onConfirm = { name, bio, phone, city, avatar, coverUrl, channelAvatar, channelBanner ->
                                viewModel.updateUserProfile(
                                    userProfile.copy(
                                        name = name,
                                        bio = bio,
                                        phone = phone,
                                        city = city,
                                        avatar = avatar,
                                        coverUrl = coverUrl,
                                        channelAvatar = channelAvatar,
                                        channelBanner = channelBanner
                                    )
                                )
                                viewModel.setShowEditProfileDialog(false)
                            }
                        )
                    }

                    // Creator Public Profile Dialog
                    if (selectedCreatorProfile != null) {
                        CreatorPublicProfileDialog(
                            video = selectedCreatorProfile!!,
                            allShorts = shortVideos,
                            onDismiss = { viewModel.setSelectedCreatorProfile(null) },
                            onStartChat = { video -> viewModel.startChatWithCreator(video) },
                            onSendGreeting = { video -> viewModel.sendGreetingToCreator(video) },
                            onToggleFollow = { creatorId -> viewModel.toggleFollowShortCreator(creatorId) },
                            onOpenTip = { video ->
                                viewModel.setSelectedCreatorProfile(null)
                                viewModel.setSelectedShortVideoForTip(video)
                            },
                            onSelectVideo = { video ->
                                viewModel.openShortVideos("/app?tab=actus")
                            }
                        )
                    }

                    // Story Viewer Dialog
                    if (selectedStatusStory != null) {
                        StoryViewerDialog(
                            status = selectedStatusStory!!,
                            onDismiss = { viewModel.setSelectedStatusStory(null) },
                            onSendMessage = { text ->
                                val targetChat = chats.find { it.name.contains(selectedStatusStory!!.userName, ignoreCase = true) } ?: chats.firstOrNull()
                                targetChat?.let { viewModel.sendMessage(it.id, "Réponse au statut : $text") }
                            }
                        )
                    }

                    // Comments Sheet Dialog
                    val latestNewsPosts by viewModel.newsPosts.collectAsStateWithLifecycle()
                    val currentPost = remember(latestNewsPosts, selectedPostForComments) {
                        latestNewsPosts.find { it.id == selectedPostForComments?.id }
                    }
                    if (currentPost != null) {
                        CommentsSheetDialog(
                            post = currentPost,
                            onDismiss = { viewModel.setSelectedPostForComments(null) },
                            onAddComment = { text ->
                                viewModel.addNewsComment(currentPost.id, text)
                            }
                        )
                    }

                    // Admin Login Dialog
                    if (showAdminLoginDialog) {
                        AdminLoginDialog(
                            onDismiss = { viewModel.setShowAdminLoginDialog(false) },
                            onAdminLogin = { key, email, pass -> viewModel.loginAdmin(key, email, pass) },
                            onSaveServerConfig = { url -> viewModel.setBackendServerUrl(url) },
                            viewModel = viewModel
                        )
                    }

                    // Forgot Password Dialog
                    if (showForgotPasswordDialog) {
                        ForgotPasswordDialog(
                            initialEmail = userProfile.email,
                            onDismiss = { viewModel.setShowForgotPasswordDialog(false) },
                            onRequestResetCode = { email -> viewModel.requestPasswordReset(email) },
                            onConfirmReset = { email, code, pass -> viewModel.confirmPasswordReset(email, code, pass) },
                            onResetSuccess = { viewModel.setShowForgotPasswordDialog(false) }
                        )
                    }

                    // User Public Profile Dialog
                    if (selectedUserProfile != null) {
                        UserProfileDialog(
                            profile = selectedUserProfile!!,
                            onDismiss = { viewModel.setSelectedUserProfile(null) },
                            onStartChat = { name ->
                                viewModel.setSelectedUserProfile(null)
                                viewModel.openChatByName(name)
                            },
                            onVoiceCall = { name, avatar ->
                                viewModel.setSelectedUserProfile(null)
                                viewModel.startCall(name, avatar, false)
                            },
                            onVideoCall = { name, avatar ->
                                viewModel.setSelectedUserProfile(null)
                                viewModel.startCall(name, avatar, true)
                            }
                        )
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun StoryViewerDialog(
    status: StatusItem,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableIntStateOf(14) }
    var replyText by remember { mutableStateOf("") }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val floatingHearts = remember { mutableStateListOf<Long>() }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Quick emoji reactions for status
    val quickEmojis = remember { listOf("❤️", "🔥", "👏", "😂", "😍", "🇨🇬", "💎") }

    // Story automatic progress bar with pause capability
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (progress < 1f) {
                delay(50)
                progress += 0.010f // Total ~5.0 seconds
            }
            if (progress >= 1f) {
                delay(200)
                onDismiss()
            }
        }
    }

    fun spawnHeartBurst() {
        val id = System.currentTimeMillis() + (0..1000).random()
        floatingHearts.add(id)
        coroutineScope.launch {
            delay(1200)
            floatingHearts.remove(id)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onDoubleTap = {
                                isLiked = !isLiked
                                if (isLiked) {
                                    likesCount++
                                    spawnHeartBurst()
                                    actionNotification = "Vous avez aimé ce statut ❤️"
                                } else {
                                    likesCount--
                                    actionNotification = "Mention J'aime retirée"
                                }
                            }
                        )
                    }
            ) {
                // Background media / aesthetic gradient
                if (status.imageUrl != null) {
                    AsyncImage(
                        model = status.imageUrl,
                        contentDescription = "Status Story",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF3B0764), Color(0xFF1E1B4B), Color(0xFF0F172A))
                                )
                            )
                    )
                }

                // Smooth cinematic dark gradient overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.90f)
                                )
                            )
                        )
                )

                // Top Progress Bar - WhatsApp style with Innovation (Glowing Pulse Gradient + Duration Timer)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Segmented Story Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Previous segment (completed)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.95f))
                        )

                        // Current active segment with animated glow
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF8B5CF6),
                                                Color(0xFFEC4899),
                                                Color(0xFF38BDF8)
                                            )
                                        )
                                    )
                            )
                        }

                        // Next segment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Status Timer Badge ("⏱️ 0:03s / 0:06s • MBoté Status HD")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentSec = (progress * 5f).toInt()
                        Text(
                            text = "⏱️ 0:0${currentSec}s / 0:05s",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isPaused) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "PAUSE",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "HD Live ⚡",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Top User Header (Avatar, Name, Timestamp, Close)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(2.dp, MbotePurplePrimary, CircleShape)
                        ) {
                            AsyncImage(
                                model = status.userAvatar,
                                contentDescription = status.userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            Text(
                                text = status.userName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = status.timestamp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center Status Text / Audio Waveform
                if (status.text != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status.text,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            lineHeight = 26.sp
                        )
                    }
                }

                // Flying hearts animation on like
                floatingHearts.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = 1.3f
                                scaleY = 1.3f
                            }
                    ) {
                        Text(text = "❤️", fontSize = 72.sp)
                    }
                }

                // Action notification toast banner inside story viewer
                actionNotification?.let { notif ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MbotePurplePrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 84.dp)
                    ) {
                        Text(
                            text = notif,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }

                // Bottom Actions: Quick emojis + J'aime + Partager + Envoyer le message
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Emoji Reaction Bar (1-click reply)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onSendMessage(emoji)
                                        spawnHeartBurst()
                                        actionNotification = "Réaction $emoji envoyée !"
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Action Buttons Row: J'aime & Partager
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button with count and animated feedback
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isLiked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.20f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isLiked = !isLiked
                                    if (isLiked) {
                                        likesCount++
                                        spawnHeartBurst()
                                        actionNotification = "Vous avez aimé ce statut ❤️"
                                    } else {
                                        likesCount--
                                        actionNotification = "Mention J'aime retirée"
                                    }
                                }
                                .testTag("status_like_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "J'aime",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$likesCount J'aime",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Share Button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.20f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "Regarde le statut de ${status.userName} sur MBoté !"
                                        )
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Partager le statut"))
                                    actionNotification = "Partage du statut..."
                                }
                                .testTag("status_share_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Partager",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Send Message Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Répondre à ${status.userName}…", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.25f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.20f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("status_reply_input")
                        )

                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onSendMessage(replyText)
                                    actionNotification = "Message envoyé à ${status.userName} 💬"
                                    replyText = ""
                                }
                            },
                            enabled = replyText.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (replyText.isNotBlank()) MbotePurplePrimary else Color.White.copy(alpha = 0.3f))
                                .testTag("status_send_reply_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Envoyer message",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsSheetDialog(
    post: NewsPost,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Commentaires (${post.comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MbotePurplePrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // List of Comments
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (post.comments.isEmpty()) {
                        item {
                            Text(
                                text = "Soyez le premier à commenter cette actualité !",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    } else {
                        items(post.comments, key = { it.id }) { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MbotePurpleSoft)
                                ) {
                                    AsyncImage(
                                        model = comment.authorAvatar,
                                        contentDescription = comment.authorName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = comment.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = comment.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = comment.text, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Add Comment Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Votre commentaire…", fontSize = 13.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_field")
                    )

                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onAddComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MbotePurplePrimary)
                            .testTag("send_comment_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
