package com.loukatech.mbote.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import coil.compose.AsyncImage
import com.loukatech.mbote.model.ShortVideo
import com.loukatech.mbote.model.ShortVideosFeedType
import com.loukatech.mbote.ui.components.CreateShortVideoDialog
import com.loukatech.mbote.ui.components.CreatorPublicProfileDialog
import com.loukatech.mbote.ui.components.GiftStoreDialog
import com.loukatech.mbote.ui.components.GiftHistoryDialog
import com.loukatech.mbote.ui.components.LiveBroadcastDialog
import com.loukatech.mbote.ui.components.ShortVideoCommentsSheet
import com.loukatech.mbote.ui.components.ShortVideoShareSheet
import com.loukatech.mbote.ui.components.TipCreatorSheet
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.viewmodel.MboteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortVideosScreen(
    viewModel: MboteViewModel,
    returnTo: String? = "/app?tab=dashboard",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortVideos by viewModel.shortVideos.collectAsState()
    val feedType by viewModel.shortVideosFeedType.collectAsState()
    val selectedVideoForComments by viewModel.selectedShortVideoForComments.collectAsState()
    val selectedVideoForShare by viewModel.selectedShortVideoForShare.collectAsState()
    val selectedVideoForTip by viewModel.selectedShortVideoForTip.collectAsState()
    val selectedCreatorProfile by viewModel.selectedCreatorProfile.collectAsState()
    val showCreateDialog by viewModel.showCreateShortVideoDialog.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val userGiftState by viewModel.userGiftState.collectAsState()
    val chats by viewModel.chats.collectAsState()

    var showMustWatchDrawer by remember { mutableStateOf(false) }
    var showLiveDialog by remember { mutableStateOf(false) }
    var showGiftStoreDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isGlobalMuted by remember { mutableStateOf(false) }

    val displayedVideos = remember(shortVideos, feedType, searchQuery) {
        val baseList = when (feedType) {
            ShortVideosFeedType.FOR_YOU -> shortVideos
            ShortVideosFeedType.FOLLOWING -> shortVideos.filter { it.isFollowing }
            ShortVideosFeedType.TRENDING -> shortVideos.sortedByDescending { it.likesCount }
        }
        if (searchQuery.isBlank()) baseList
        else baseList.filter {
            it.caption.contains(searchQuery, ignoreCase = true) ||
            it.creatorName.contains(searchQuery, ignoreCase = true) ||
            it.hashtags.any { h -> h.contains(searchQuery, ignoreCase = true) }
        }
    }

    val pagerState = rememberPagerState(pageCount = { displayedVideos.size })
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(pagerState.currentPage, displayedVideos) {
        displayedVideos.getOrNull(pagerState.currentPage)?.let { viewModel.markShortViewed(it.id) }
        if (pagerState.currentPage > 0) {
            viewModel.addScrollingMinutes(1)
            com.loukatech.mbote.service.AppUsageTrackingService.recordScroll(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("short_videos_screen")
    ) {
        if (displayedVideos.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = displayedVideos[page]
                ShortVideoCardExact(
                    video = video,
                    isMuted = isGlobalMuted,
                    onToggleMute = { isGlobalMuted = !isGlobalMuted },
                    onToggleLike = { viewModel.toggleLikeShortVideo(video.id) },
                    onReact = { emoji -> viewModel.reactToShortVideo(video.id, emoji) },
                    onToggleBookmark = { viewModel.toggleBookmarkShortVideo(video.id) },
                    onToggleFollow = { viewModel.toggleFollowShortCreator(video.creatorId) },
                    onOpenCreatorProfile = { viewModel.setSelectedCreatorProfile(video) },
                    onOpenComments = { viewModel.setSelectedShortVideoForComments(video) },
                    onOpenShare = { viewModel.setSelectedShortVideoForShare(video) },
                    onOpenTip = { viewModel.setSelectedShortVideoForTip(video) }
                )
            }
        }

        // Top Navigation Header: SHORTMBOTÉ Bar with Live, Create, Search & Menu
        ShortMbotHeader(
            onBack = onBack,
            onLiveClick = { showLiveDialog = true },
            onCreateClick = { viewModel.setShowCreateShortVideoDialog(true) },
            onSearchToggle = { isSearchActive = !isSearchActive },
            onReportActiveVideo = {
                val currentVideo = displayedVideos.getOrNull(pagerState.currentPage)
                if (currentVideo != null) {
                    viewModel.submitReport("Vidéo Short", currentVideo.caption.take(25) + "...")
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Sub Header: Feed Tabs ("Abonnements", "Pour vous" with underline, "Tendances")
        ShortMbotFeedTabs(
            currentFeedType = feedType,
            onSelectFeedType = { viewModel.setShortVideosFeedType(it) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 52.dp)
        )

        // Search Bar Overlay when active
        if (isSearchActive) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MbotePurplePrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher des créateurs, musiques...", color = Color.Gray, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MbotePurplePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Bottom drawer "À ne pas manquer" (toggleable)
        AnimatedVisibility(
            visible = showMustWatchDrawer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            MustWatchDrawer(
                videos = shortVideos,
                onClose = { showMustWatchDrawer = false },
                onSelectVideo = { selected ->
                    showMustWatchDrawer = false
                    val index = displayedVideos.indexOfFirst { it.id == selected.id }
                    if (index >= 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                }
            )
        }

        // Prominent Floating "À ne pas manquer" Pill Button
        if (!showMustWatchDrawer) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp)
                    .clickable { showMustWatchDrawer = true }
                    .testTag("open_must_watch_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Ouvrir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "À ne pas manquer",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6))
                    )
                }
            }
        }

        // Comments, Share, Tip Sheets & Create Dialog
        val currentVideoForComments = remember(shortVideos, selectedVideoForComments) {
            shortVideos.find { it.id == selectedVideoForComments?.id }
        }
        currentVideoForComments?.let { video ->
            ShortVideoCommentsSheet(
                shortVideo = video,
                onDismiss = { viewModel.setSelectedShortVideoForComments(null) },
                onAddComment = { text -> viewModel.addShortVideoComment(video.id, text) },
                onToggleLikeComment = { commentId -> viewModel.toggleLikeShortComment(video.id, commentId) }
            )
        }

        selectedVideoForShare?.let { video ->
            ShortVideoShareSheet(
                shortVideo = video,
                chats = chats,
                onDismiss = { viewModel.setSelectedShortVideoForShare(null) },
                onSendToChat = { chat ->
                    viewModel.shareShortVideoToChat(chat.id, video)
                }
            )
        }

        selectedVideoForTip?.let { video ->
            TipCreatorSheet(
                shortVideo = video,
                walletBalanceFcfa = userProfile.walletBalanceFcfa,
                userGiftState = userGiftState,
                onSendTip = { amount, provider ->
                    viewModel.tipCreator(video.id, amount, provider)
                },
                onSendGift = { giftId ->
                    viewModel.sendGift(giftId, video.creatorName)
                },
                onBuySingleGift = { gift, count, provider ->
                    viewModel.buySingleGift(gift, count, provider)
                },
                onBuyBundle = { bundle, provider ->
                    viewModel.buyGiftBundle(bundle, provider)
                },
                onCashout = { amount, provider, phone ->
                    viewModel.cashoutVirtualGifts(amount, provider, phone)
                },
                onOpenStore = {
                    showGiftStoreDialog = true
                },
                onDismiss = { viewModel.setSelectedShortVideoForTip(null) }
            )
        }

        if (showGiftStoreDialog) {
            GiftStoreDialog(
                userGiftState = userGiftState,
                onBuyBundle = { bundle, provider ->
                    viewModel.buyGiftBundle(bundle, provider)
                },
                onBuySingleGift = { gift, count, provider ->
                    viewModel.buySingleGift(gift, count, provider)
                },
                onDismiss = { showGiftStoreDialog = false }
            )
        }

        selectedCreatorProfile?.let { creatorVideo ->
            CreatorPublicProfileDialog(
                video = creatorVideo,
                allShorts = shortVideos,
                onDismiss = { viewModel.setSelectedCreatorProfile(null) },
                onStartChat = { video ->
                    viewModel.startChatWithCreator(video)
                },
                onSendGreeting = { video ->
                    viewModel.sendGreetingToCreator(video)
                },
                onToggleFollow = { creatorId ->
                    viewModel.toggleFollowShortCreator(creatorId)
                },
                onOpenTip = { video ->
                    viewModel.setSelectedCreatorProfile(null)
                    viewModel.setSelectedShortVideoForTip(video)
                },
                onSelectVideo = { video ->
                    val index = displayedVideos.indexOfFirst { it.id == video.id }
                    if (index != -1) {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }
                }
            )
        }

        if (showCreateDialog) {
            CreateShortVideoDialog(
                onDismiss = { viewModel.setShowCreateShortVideoDialog(false) },
                onPublish = { videoUri, duration, caption, hashtags, musicTitle, musicArtist, thumb, loc ->
                    viewModel.createShortVideo(context, videoUri, duration, caption, hashtags, musicTitle, musicArtist, thumb, loc)
                }
            )
        }

        if (showLiveDialog) {
            LiveBroadcastDialog(
                currentUserAvatar = userProfile.avatar,
                userGiftState = userGiftState,
                isPremiumUser = userProfile.isPremium,
                userBadges = userProfile.badges,
                onSendGift = { giftId, multiplier ->
                    viewModel.sendGift(giftId, "Diffuseur Live MBoté", multiplier)
                },
                onBuyBundle = { bundle, provider ->
                    viewModel.buyGiftBundle(bundle, provider)
                },
                onBuySingleGift = { gift, count, provider ->
                    viewModel.buySingleGift(gift, count, provider)
                },
                onDismiss = { showLiveDialog = false }
            )
        }
    }
}

/**
 * Top Header preserving the SHORTMBOTÉ design
 */
@Composable
fun ShortMbotHeader(
    onBack: () -> Unit,
    onLiveClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onReportActiveVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Arrow + SHORTMBOTÉ Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onBack() }
                    .testTag("short_videos_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SHORT",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "MBOTÉ",
                        color = Color(0xFF8B5CF6),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Action Icons (Live, Plus, Search, More)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Purple Live Broadcast button ((•))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED))
                        .clickable { onLiveClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Live MBoté",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Purple Plus button (+)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED))
                        .clickable { onCreateClick() }
                        .testTag("create_short_video_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nouveau Short",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Search icon
                IconButton(
                    onClick = onSearchToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Recherche",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Three dots overflow
                var showMenu by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp).testTag("short_videos_more_vert")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Plus d'options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🎥 Qualité de lecture (1080p HD)") },
                            leadingIcon = { Icon(Icons.Default.Hd, contentDescription = null, tint = Color(0xFF7C3AED)) },
                            onClick = {
                                showMenu = false
                                android.widget.Toast.makeText(context, "Qualité vidéo configurée sur HD 1080p (Opus + H.265)", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔊 Audio Spatial MBoté Shorts") },
                            leadingIcon = { Icon(Icons.Default.VolumeUp, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                android.widget.Toast.makeText(context, "Audio Spatial 3D activé", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔞 Contrôle parental & Sécurité") },
                            leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                android.widget.Toast.makeText(context, "Mode filtre parental actif", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("📤 Partager le flux de Shorts") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                android.widget.Toast.makeText(context, "Lien du flux copié dans le presse-papiers", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🚩 Signaler un contenu") },
                            leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onReportActiveVideo()
                                android.widget.Toast.makeText(context, "Signalement envoyé à l'administration MBoté. Merci pour votre vigilance ! 🚩", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Feed Tabs matching Image 1: "Abonnements", "Pour vous" (bold white with underline indicator), "Tendances"
 */
@Composable
fun ShortMbotFeedTabs(
    currentFeedType: ShortVideosFeedType,
    onSelectFeedType: (ShortVideosFeedType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeedTextTabItem(
                title = "Abonnements",
                isSelected = currentFeedType == ShortVideosFeedType.FOLLOWING,
                onClick = { onSelectFeedType(ShortVideosFeedType.FOLLOWING) }
            )

            FeedTextTabItem(
                title = "Pour vous",
                isSelected = currentFeedType == ShortVideosFeedType.FOR_YOU,
                onClick = { onSelectFeedType(ShortVideosFeedType.FOR_YOU) }
            )

            FeedTextTabItem(
                title = "Tendances",
                isSelected = currentFeedType == ShortVideosFeedType.TRENDING,
                onClick = { onSelectFeedType(ShortVideosFeedType.TRENDING) }
            )
        }
    }
}

@Composable
fun FeedTextTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (isSelected) 16.sp else 14.sp
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color.White)
            )
        } else {
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
}

data class FloatingReactionParticle(
    val id: Long = System.currentTimeMillis() + (0..100000).random(),
    val emoji: String,
    val initialXOffset: Float = (-25..25).random().toFloat()
)

/**
 * Fullscreen Video Card replicating the exact icons and typography:
 * - Top-right Mute / Sound button
 * - Right column: Avatar, Heart/Reactions, Chat, Bookmark, Share, Pourboire, Rotating Vinyl
 * - Left column: Name, Blue Verified Badge, 'Abonné' / '+ Suivre' pill, Yellow location pin, Multi-line caption, Light blue hashtags, Music ticker
 */
@Composable
fun ShortVideoCardExact(
    video: ShortVideo,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onToggleLike: () -> Unit,
    onReact: (String) -> Unit = {},
    onToggleBookmark: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenCreatorProfile: () -> Unit = {},
    onOpenComments: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var showReactionTray by remember { mutableStateOf(false) }
    var isCaptionExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val reactionParticles = remember { mutableStateListOf<FloatingReactionParticle>() }
    var nativeVideoView by remember(video.id) { mutableStateOf<VideoView?>(null) }
    var nativeMediaPlayer by remember(video.id) { mutableStateOf<MediaPlayer?>(null) }

    val reactionOptions = remember {
        listOf(
            "❤️" to "J'adore",
            "🔥" to "Feu",
            "👏" to "Bravo",
            "😮" to "Wouah",
            "💎" to "Pépite",
            "🇨🇬" to "MBoté"
        )
    }

    fun spawnReactionParticles(emoji: String) {
        repeat(5) {
            reactionParticles.add(FloatingReactionParticle(emoji = emoji))
        }
        coroutineScope.launch {
            delay(1600)
            if (reactionParticles.isNotEmpty()) {
                reactionParticles.removeRange(0, (5).coerceAtMost(reactionParticles.size))
            }
        }
    }

    // Rotating vinyl animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_angle"
    )

    LaunchedEffect(isPlaying, nativeVideoView) {
        nativeVideoView?.let { player -> if (isPlaying) player.start() else player.pause() }
    }

    LaunchedEffect(isMuted, nativeMediaPlayer) {
        val volume = if (isMuted) 0f else 1f
        nativeMediaPlayer?.setVolume(volume, volume)
    }

    DisposableEffect(nativeVideoView) {
        onDispose {
            nativeVideoView?.stopPlayback()
            nativeMediaPlayer = null
        }
    }

    // Real playback progress from the native Android player.
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(nativeVideoView, isPlaying) {
        while (nativeVideoView != null) {
            val duration = nativeVideoView?.duration?.takeIf { it > 0 } ?: 0
            val position = nativeVideoView?.currentPosition ?: 0
            progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            delay(200)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (showReactionTray) {
                            showReactionTray = false
                        } else {
                            isPlaying = !isPlaying
                        }
                    },
                    onDoubleTap = {
                        val activeEmoji = video.userReaction ?: "❤️"
                        if (!video.isLiked) onToggleLike()
                        spawnReactionParticles(activeEmoji)
                        showHeartBurst = true
                        coroutineScope.launch {
                            delay(800)
                            showHeartBurst = false
                        }
                    }
                )
            }
    ) {
        if (video.videoPlaybackUrl.isNotBlank()) {
            AndroidView(
                factory = { context ->
                    VideoView(context).also { player ->
                        nativeVideoView = player
                        player.setVideoURI(Uri.parse(video.videoPlaybackUrl))
                        player.setOnPreparedListener { mediaPlayer ->
                            nativeMediaPlayer = mediaPlayer
                            mediaPlayer.isLooping = true
                            val volume = if (isMuted) 0f else 1f
                            mediaPlayer.setVolume(volume, volume)
                            if (isPlaying) player.start()
                        }
                    }
                },
                update = { player -> if (isPlaying) player.start() else player.pause() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = video.videoThumbnailUrl,
                contentDescription = video.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradient overlays for crisp text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Floating Reaction Particles Animation
        reactionParticles.forEach { particle ->
            key(particle.id) {
                val animProgress = remember { Animatable(0f) }
                LaunchedEffect(particle.id) {
                    animProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(1400, easing = FastOutSlowInEasing)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = (40 + particle.initialXOffset).dp, bottom = 120.dp)
                        .graphicsLayer {
                            translationY = -animProgress.value * 350f
                            translationX = (kotlin.math.sin(animProgress.value * 3.14f * 2) * 24f)
                            alpha = (1f - animProgress.value).coerceIn(0f, 1f)
                            scaleX = 0.8f + (animProgress.value * 0.8f)
                            scaleY = 0.8f + (animProgress.value * 0.8f)
                        }
                ) {
                    Text(
                        text = particle.emoji,
                        fontSize = 28.sp
                    )
                }
            }
        }

        // Sound Mute Toggle Icon (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 50.dp, end = 16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onToggleMute() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Activer le son" else "Couper le son",
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }

        // Center Pause Indicator
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Heart Burst Animation on double tap
        if (showHeartBurst) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = video.userReaction ?: "❤️",
                    fontSize = 80.sp,
                    modifier = Modifier.scale(1.2f)
                )
            }
        }

        // Left Bottom Information Column (Creator, Verified, Follow Button, Location, Caption, Hashtags, Music)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.80f)
                .padding(start = 14.dp, bottom = 22.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Creator Row: Name + Blue Verified Check + 'Abonné' Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onOpenCreatorProfile() }
            ) {
                Text(
                    text = video.creatorName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (video.isCreatorVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Vérifié",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Follow / Abonné Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (video.isFollowing) Color.Black.copy(alpha = 0.45f) else MbotePurplePrimary,
                    border = if (video.isFollowing) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else null,
                    modifier = Modifier
                        .clickable { onToggleFollow() }
                        .testTag("follow_creator_button")
                ) {
                    Text(
                        text = if (video.isFollowing) "Abonné" else "+ Suivre",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            // Location Tag: Yellow Pin + Place Name
            if (!video.location.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Text(text = "📍", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = video.location,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Caption / Description
            Text(
                text = video.caption,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = if (isCaptionExpanded) 6 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded }
            )

            // Hashtags with light blue color
            if (video.hashtags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    video.hashtags.take(3).forEach { tag ->
                        Text(
                            text = tag,
                            color = Color(0xFF60A5FA),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Music Ticker at bottom
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${video.musicTitle} • ${video.musicArtist}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Horizontal Floating Reactions Tray Overlay
        AnimatedVisibility(
            visible = showReactionTray,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 72.dp, bottom = 260.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E1B2E).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reactionOptions.forEach { (emoji, label) ->
                        val isSelected = (video.userReaction == emoji)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MbotePurplePrimary.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable {
                                    showReactionTray = false
                                    onReact(emoji)
                                    spawnReactionParticles(emoji)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = if (isSelected) 24.sp else 20.sp
                            )
                        }
                    }
                }
            }
        }

        // Right Vertical Action Column matching the exact icons and labels
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Creator Avatar with click to navigate to public profile
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable { onOpenCreatorProfile() }
                    .testTag("creator_avatar_clickable")
            ) {
                AsyncImage(
                    model = video.creatorAvatar,
                    contentDescription = video.creatorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, if (video.isFollowing) MbotePurplePrimary else Color.White, CircleShape)
                )

                // Plus Follow Badge if not following
                if (!video.isFollowing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 6.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MbotePurplePrimary)
                            .clickable { onToggleFollow() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Suivre",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // 2. Reaction / Like Action (Shows active emoji reaction if reacted, otherwise Heart)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        if (video.userReaction != null) {
                            onToggleLike()
                        } else {
                            showReactionTray = !showReactionTray
                        }
                    }
                    .testTag("short_reaction_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (video.userReaction != null) {
                        Text(
                            text = video.userReaction!!,
                            fontSize = 24.sp
                        )
                    } else {
                        Icon(
                            imageVector = if (video.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Réagir",
                            tint = if (video.isLiked) Color(0xFFEF4444) else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatShortCount(video.likesCount),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Comment Action (Chat bubble with count)
            CleanActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                iconTint = Color.White,
                count = formatShortCount(video.commentsCount),
                onClick = onOpenComments,
                testTag = "short_comment_button"
            )

            // 4. Bookmark Action
            CleanActionButton(
                icon = if (video.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                iconTint = if (video.isBookmarked) Color(0xFFFBBF24) else Color.White,
                count = formatShortCount(video.bookmarksCount),
                onClick = onToggleBookmark,
                testTag = "short_bookmark_button"
            )

            // 5. Share Action
            CleanActionButton(
                icon = Icons.Outlined.Share,
                iconTint = Color.White,
                count = formatShortCount(video.sharesCount),
                onClick = onOpenShare,
                testTag = "short_share_button"
            )

            // 6. Pourboire Action: Green circle with '$' + label "Pourboire"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onOpenTip() }
                    .testTag("short_tip_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pourboire",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 7. Rotating Vinyl Record Album Cover
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF181524))
                    .border(2.dp, Color(0xFF2E2746), CircleShape)
                    .rotate(if (isPlaying) discRotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = video.musicCoverUrl.ifBlank { video.videoThumbnailUrl },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                )
            }
        }

        // Bottom playback progress line
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            color = MbotePurplePrimary,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun CleanActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    count: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        if (count.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = count,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Bottom Drawer: "À ne pas manquer"
 */
@Composable
fun MustWatchDrawer(
    videos: List<ShortVideo>,
    onClose: () -> Unit,
    onSelectVideo: (ShortVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.Black.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.6f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Header: "À ne pas manquer" + Close (X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "À ne pas manquer",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )

                // Close Button 'X' in dark circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Column Grid of Recommended Videos
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(videos, key = { it.id }) { video ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectVideo(video) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1B2E))
                        ) {
                            AsyncImage(
                                model = video.videoThumbnailUrl,
                                contentDescription = video.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    text = video.durationFormatted,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = video.caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = video.creatorUsername,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            maxLines = 1
                        )

                        Text(
                            text = "${video.viewsCount} vues",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatShortCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}
