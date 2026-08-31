package com.loukatech.mbote.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.loukatech.mbote.model.NewsPost
import com.loukatech.mbote.model.ChannelSummary
import com.loukatech.mbote.model.ShortVideo
import com.loukatech.mbote.model.StatusItem
import com.loukatech.mbote.ui.components.CreateChannelDialog
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

data class ShortVideoData(
    val id: String,
    val title: String,
    val duration: String,
    val views: String,
    val gradientColors: List<Color>
)

data class ChannelPost(
    val id: String = java.util.UUID.randomUUID().toString(),
    val channelId: String,
    val title: String,
    val content: String,
    val timestamp: String = "Il y a 10 min",
    val imageUrl: String? = null,
    val likesCount: Int = 42,
    val isLiked: Boolean = false,
    val commentsCount: Int = 18,
    val tags: List<String> = listOf("MBoté", "Actus", "Congo")
)

data class ChannelInfo(
    val id: String,
    val name: String,
    val bio: String,
    val avatarLetter: String,
    val isVerified: Boolean = true,
    val category: String = "Officiel",
    val subscribersCount: Int = 1420,
    val isSubscribed: Boolean = false,
    val coverUrl: String? = null,
    val posts: List<ChannelPost> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActusScreen(
    newsPosts: List<NewsPost>,
    statuses: List<StatusItem>,
    currentUserName: String = "",
    shortVideos: List<ShortVideo> = emptyList(),
    channels: List<ChannelSummary> = emptyList(),
    isSyncing: Boolean = false,
    onLikeClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onCommentClick: (NewsPost) -> Unit,
    onStatusClick: (StatusItem) -> Unit,
    onAddStatusClick: () -> Unit,
    onOpenShortVideos: () -> Unit = {},
    onOpenCreatorProfile: (ShortVideo) -> Unit = {},
    onCreateShortVideoClick: () -> Unit = {},
    onCreateChannel: (String, String, Boolean, String) -> Unit = { _, _, _, _ -> },
    onToggleChannelSubscription: (String, Boolean) -> Unit = { _, _ -> },
    onPublishNews: (title: String, content: String, mediaUri: android.net.Uri?, category: String, mediaType: String) -> Unit = { _, _, _, _, _ -> },
    onAuthorProfileClick: (String, String) -> Unit = { _, _ -> },
    onReportContent: (String, String) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(searchQuery.isNotBlank()) }
    var selectedCategoryFilter by remember { mutableStateOf("Pour vous") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showPublishTypeMenu by remember { mutableStateOf(false) }
    var showNewActusModal by remember { mutableStateOf(false) }
    var initialMediaType by remember { mutableStateOf("Photo") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Full screen media viewer state according to requirement (6)
    var activeFullScreenMediaUrl by remember { mutableStateOf<String?>(null) }
    var activeFullScreenTitle by remember { mutableStateOf("") }
    var activeFullScreenAuthor by remember { mutableStateOf("MBoté") }
    var activeFullScreenAvatar by remember { mutableStateOf("") }
    var activeFullScreenIsVideo by remember { mutableStateOf(false) }

    val categoryFilters = listOf("Pour vous", "Public", "Amis", "Tendances", "Communauté")

    val displayShortVideos = shortVideos

    var channelsList by remember(channels) {
        mutableStateOf(channels.map { channel ->
            ChannelInfo(
                id = channel.id,
                name = channel.name,
                bio = channel.description,
                avatarLetter = channel.name.firstOrNull()?.uppercase() ?: "M",
                isVerified = channel.canPublish,
                category = channel.category,
                subscribersCount = channel.subscriberCount,
                isSubscribed = channel.subscribedByMe,
                coverUrl = channel.bannerUrl,
                posts = emptyList()
            )
        })
    }
    var selectedChannelForView by remember { mutableStateOf<ChannelInfo?>(null) }
    var channelFeedbackToast by remember { mutableStateOf<String?>(null) }

    // Dedicated Full Screen Channel View (When user clicks "Voir")
    if (selectedChannelForView != null) {
        ChannelDetailContent(
            channel = selectedChannelForView!!,
            onBack = { selectedChannelForView = null },
            onToggleSubscribe = { channelId ->
                val channel = channelsList.find { it.id == channelId }
                if (channel != null) {
                    onToggleChannelSubscription(channel.id, channel.isSubscribed)
                    channelsList = channelsList.map { ch ->
                        if (ch.id == channelId) {
                            val newSub = !ch.isSubscribed
                            val newCount = if (newSub) ch.subscribersCount + 1 else (ch.subscribersCount - 1).coerceAtLeast(0)
                            ch.copy(isSubscribed = newSub, subscribersCount = newCount)
                        } else ch
                    }
                    selectedChannelForView = channelsList.find { it.id == channelId }
                }
            },
            onShareChannel = { ch ->
                channelFeedbackToast = "Lien de la chaîne '${ch.name}' partagé ! 🔗"
            },
            onSharePost = { post ->
                channelFeedbackToast = "Publication partagée avec succès ! 🚀"
            },
            onLikePost = { post ->
                // Toggle channel post like
                val updatedPosts = selectedChannelForView!!.posts.map { p ->
                    if (p.id == post.id) {
                        val newLiked = !p.isLiked
                        p.copy(
                            isLiked = newLiked,
                            likesCount = if (newLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                        )
                    } else p
                }
                selectedChannelForView = selectedChannelForView!!.copy(posts = updatedPosts)
            },
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                onRefresh()
                coroutineScope.launch {
                    kotlinx.coroutines.delay(600)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
            // 1. Page Header Title & Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actus",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Loupe Toggle Button (placed BEFORE Filter button)
                        Surface(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible && searchQuery.isNotBlank()) {
                                    searchQuery = ""
                                }
                            },
                            shape = CircleShape,
                            color = if (isSearchVisible || searchQuery.isNotBlank()) MbotePurplePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("actus_search_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Afficher la recherche",
                                    tint = if (isSearchVisible || searchQuery.isNotBlank()) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Filter Dropdown Button
                        Box {
                            Surface(
                                onClick = { showFilterMenu = true },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterAlt,
                                        contentDescription = "Filtrer",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                categoryFilters.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, fontWeight = if (selectedCategoryFilter == cat) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = {
                                            if (selectedCategoryFilter == cat) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MbotePurplePrimary)
                                            }
                                        },
                                        onClick = {
                                            selectedCategoryFilter = cat
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Animated Search Bar (Appears conditionally on search loupe click)
            item {
                AnimatedVisibility(
                    visible = isSearchVisible || searchQuery.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher dans les actus...", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Rechercher",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                focusedBorderColor = MbotePurplePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. ShortVidéo Carousel Section
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Shortvidéo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = CircleShape,
                                color = MbotePurpleSoft
                            ) {
                                Text(
                                    text = "${displayShortVideos.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MbotePurplePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onCreateShortVideoClick) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Créer un short",
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Créer", color = MbotePurplePrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(onClick = onOpenShortVideos) {
                                Text("Voir tout", color = MbotePurplePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayShortVideos, key = { it.id }) { item ->
                                ShortVideoCard(
                                    short = item,
                                    onClick = onOpenShortVideos,
                                    onCreatorClick = { onOpenCreatorProfile(item) }
                                )
                            }
                        }

                        // Circular Right Navigation Arrow Button Overlay
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clickable { onOpenShortVideos() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Voir plus de shorts",
                                    tint = MbotePurplePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Category Filter Chips Bar
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryFilters) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MbotePurplePrimary else Color(0xFFF3F4F6),
                            modifier = Modifier.clickable { selectedCategoryFilter = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 5. Status / Post Creator Widget ("Quoi de neuf, Loukatech")
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MbotePurplePrimary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUserName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                                            .take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "M" },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Text(
                                text = if (currentUserName.isBlank()) "Quoi de neuf ?" else "Quoi de neuf, $currentUserName ?",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        initialMediaType = "Photo"
                                        showNewActusModal = true
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PostActionButton(
                                icon = Icons.Outlined.Image,
                                label = "Photo",
                                color = Color(0xFF10B981),
                                onClick = {
                                    initialMediaType = "Photo"
                                    showNewActusModal = true
                                }
                            )
                            PostActionButton(
                                icon = Icons.Outlined.Videocam,
                                label = "Vidéo",
                                color = Color(0xFF3B82F6),
                                onClick = {
                                    initialMediaType = "Vidéo"
                                    showNewActusModal = true
                                }
                            )
                            PostActionButton(
                                icon = Icons.Outlined.Mic,
                                label = "Audio",
                                color = Color(0xFF8B5CF6),
                                onClick = {
                                    initialMediaType = "Audio"
                                    showNewActusModal = true
                                }
                            )
                            PostActionButton(
                                icon = Icons.Outlined.Send,
                                label = "Publier",
                                color = MbotePurplePrimary,
                                onClick = {
                                    initialMediaType = "Texte"
                                    showNewActusModal = true
                                }
                            )
                        }
                    }
                }
            }

            // 6. "Chaînes" Section Carousel
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chaînes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showCreateChannelDialog = true }) {
                            Text("Créer une chaîne", color = MbotePurplePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(channelsList, key = { it.id }) { channel ->
                            ChannelCardItem(
                                channel = channel,
                                onOpen = { selectedChannelForView = channel },
                                onSubscribe = {
                                    onToggleChannelSubscription(channel.id, channel.isSubscribed)
                                        channelsList = channelsList.map { ch ->
                                            if (ch.id == channel.id) {
                                                ch.copy(
                                                    isSubscribed = !ch.isSubscribed,
                                                    subscribersCount = if (ch.isSubscribed) (ch.subscribersCount - 1).coerceAtLeast(0) else ch.subscribersCount + 1
                                                )
                                            } else ch
                                        }
                                }
                            )
                        }
                    }
                }
            }

            // 7. News / Status Feed Posts Header
            item {
                Text(
                    text = "Fil d'actualités",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 6.dp)
                )
            }

            // Filtered Feed Items
            if (isSyncing) {
                item {
                    com.loukatech.mbote.ui.components.FeedSkeletonList(count = 3)
                }
            } else {
                val filteredPosts = newsPosts.filter { post ->
                    if (searchQuery.isBlank()) true
                    else post.content.contains(searchQuery, ignoreCase = true) ||
                            post.authorName.contains(searchQuery, ignoreCase = true)
                }

                items(filteredPosts, key = { it.id }) { post ->
                    ActusPostCard(
                        post = post,
                        onLike = { onLikeClick(post.id) },
                        onComment = { onCommentClick(post) },
                        onShare = { onShareClick(post.id) },
                        onOpenMediaFullScreen = { url, title, author, avatar, isVideo ->
                            activeFullScreenMediaUrl = url
                            activeFullScreenTitle = title
                            activeFullScreenAuthor = author
                            activeFullScreenAvatar = avatar
                            activeFullScreenIsVideo = isVideo
                        },
                        onProfileClick = { onAuthorProfileClick(post.authorName, post.authorAvatar) },
                        onReportClick = { onReportContent("Actualité", post.title) }
                    )
                }
            }
        }
    }

    // Full Screen Lightbox Dialog Overlay for Media according to requirement (6)
    activeFullScreenMediaUrl?.let { mediaUrl ->
        com.loukatech.mbote.ui.components.FullScreenMediaViewerDialog(
            mediaUrl = mediaUrl,
            mediaTitle = activeFullScreenTitle,
            authorName = activeFullScreenAuthor,
            authorAvatar = activeFullScreenAvatar,
            isVideo = activeFullScreenIsVideo,
            onDismiss = { activeFullScreenMediaUrl = null }
        )
    }

        // Speed Dial Popup Menu Overlay (when FAB clicked)
        AnimatedVisibility(
            visible = showPublishTypeMenu,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 150.dp, end = 20.dp)
        ) {
            PublishTypeMenu(
                onSelectType = { typeLabel ->
                    showPublishTypeMenu = false
                    initialMediaType = typeLabel
                    showNewActusModal = true
                }
            )
        }

        // Floating Action Button (+)
        FloatingActionButton(
            onClick = { showPublishTypeMenu = !showPublishTypeMenu },
            containerColor = MbotePurplePrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .testTag("actus_fab_add")
        ) {
            Icon(
                imageVector = if (showPublishTypeMenu) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Publier sur Actus",
                modifier = Modifier.size(28.dp)
            )
        }

        // Create Channel Wizard Modal Sheet
        if (showCreateChannelDialog) {
            CreateChannelDialog(
                onDismiss = { showCreateChannelDialog = false },
                onConfirm = { channelName, description, isPublic, initialPost ->
                    onCreateChannel(channelName, description, isPublic, initialPost)
                    showCreateChannelDialog = false
                }
            )
        }

        // Nouvel Actus Creation Modal
        if (showNewActusModal) {
            NewActusModal(
                initialMediaType = initialMediaType,
                onDismiss = { showNewActusModal = false },
                onPublish = { title, content, mediaUri, category, mediaType ->
                    onPublishNews(title, content, mediaUri, category, mediaType)
                    showNewActusModal = false
                }
            )
        }
    }
}

@Composable
private fun PublishTypeMenu(
    onSelectType: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        tonalElevation = 6.dp,
        modifier = modifier
            .width(180.dp)
            .border(1.dp, Color(0xFFEDE9FE), RoundedCornerShape(24.dp))
            .padding(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PublishTypeMenuItem(
                icon = Icons.Outlined.Image,
                label = "Image",
                onClick = { onSelectType("Photo") }
            )
            PublishTypeMenuItem(
                icon = Icons.Outlined.Edit,
                label = "Texte",
                onClick = { onSelectType("Texte") }
            )
            PublishTypeMenuItem(
                icon = Icons.Outlined.Mic,
                label = "Audio",
                onClick = { onSelectType("Audio") }
            )
            PublishTypeMenuItem(
                icon = Icons.Outlined.Videocam,
                label = "Vidéo",
                onClick = { onSelectType("Vidéo") }
            )
        }
    }
}

@Composable
private fun PublishTypeMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFAF8FF),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MbotePurplePrimary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E1065)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewActusModal(
    initialMediaType: String = "Photo",
    onDismiss: () -> Unit,
    onPublish: (title: String, content: String, mediaUri: android.net.Uri?, category: String, mediaType: String) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var selectedMediaTab by remember { mutableStateOf(initialMediaType) }
    var attachedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachedMediaUri = uri }
    val apiMediaType = when (selectedMediaTab) {
        "Photo", "Image" -> "image"
        "Vidéo" -> "video"
        "Audio" -> "audio"
        else -> "text"
    }

    val wordCount = remember(contentText) {
        if (contentText.isBlank()) 0
        else contentText.trim().split("\\s+".toRegex()).size
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    // Top Bar Controls (Back Arrow, Brouillon, Publier)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Brouillon", fontSize = 13.sp, color = Color.DarkGray)
                            }

                            Button(
                                onClick = {
                                    onPublish(
                                        titleText.trim(),
                                        contentText.trim(),
                                        attachedMediaUri,
                                        "Communauté",
                                        apiMediaType
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Publier", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Main Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title Header with Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Article,
                                contentDescription = null,
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nouvel Actus",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Partagez une information importante 💜",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Espace Brouillons Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.clickable { }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Espace Brouillons",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MbotePurplePrimary,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "0",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Stepper Bar (1 Contenu -> 2 Détails -> 3 Aperçu)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Step 1
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MbotePurplePrimary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Contenu", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MbotePurplePrimary)
                            }

                            HorizontalDivider(modifier = Modifier.weight(0.15f), color = Color(0xFFE5E7EB))

                            // Step 2
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE5E7EB),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("2", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Détails", fontSize = 12.5.sp, color = Color.Gray)
                            }

                            HorizontalDivider(modifier = Modifier.weight(0.15f), color = Color(0xFFE5E7EB))

                            // Step 3
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE5E7EB),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("3", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Aperçu", fontSize = 12.5.sp, color = Color.Gray)
                            }
                        }

                        // Author Info Header Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MbotePurplePrimary,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("LO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Loukatech",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFFF3F4F6)
                                            ) {
                                                Text(
                                                    text = "Créateur MBoté 💜",
                                                    fontSize = 10.sp,
                                                    color = MbotePurplePrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Public,
                                            contentDescription = null,
                                            tint = Color.DarkGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Public", fontSize = 11.5.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }

                        // Field 1: Titre de l'actus *
                        Column {
                            Text(
                                text = "Titre de l'actus *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = titleText,
                                onValueChange = { if (it.length <= 120) titleText = it },
                                placeholder = { Text("Titre de votre publication", fontSize = 13.5.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    Text(
                                        text = "${titleText.length}/120",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MbotePurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )
                        }

                        // Field 2: Contenu de l'actus *
                        Column {
                            Text(
                                text = "Contenu de l'actus *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    // Rich Text Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFAFAFA))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Text("B", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Text("I", fontStyle = FontStyle.Italic, fontSize = 14.sp)
                                        }
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Text("U", textDecoration = TextDecoration.Underline, fontSize = 14.sp)
                                        }
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.SentimentSatisfied, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFE5E7EB))

                                    // Content Input Area
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 100.dp)
                                            .padding(10.dp)
                                    ) {
                                        if (contentText.isEmpty()) {
                                            Text(
                                                text = "Écrivez ici le contenu de votre actus...",
                                                fontSize = 13.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        OutlinedTextField(
                                            value = contentText,
                                            onValueChange = { contentText = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            )
                                        )

                                        Text(
                                            text = "$wordCount/250 mots",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Section 3: Ajouter des médias
                        Column {
                            Text(
                                text = "Ajouter des médias",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Tab Option Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MediaTabChip(
                                    icon = Icons.Outlined.Image,
                                    label = "Photo",
                                    isSelected = selectedMediaTab == "Photo" || selectedMediaTab == "Image",
                                    onClick = { selectedMediaTab = "Photo" },
                                    modifier = Modifier.weight(1f)
                                )
                                MediaTabChip(
                                    icon = Icons.Outlined.Videocam,
                                    label = "Vidéo",
                                    isSelected = selectedMediaTab == "Vidéo",
                                    onClick = { selectedMediaTab = "Vidéo" },
                                    modifier = Modifier.weight(1f)
                                )
                                MediaTabChip(
                                    icon = Icons.Outlined.Mic,
                                    label = "Audio",
                                    isSelected = selectedMediaTab == "Audio",
                                    onClick = { selectedMediaTab = "Audio" },
                                    modifier = Modifier.weight(1f)
                                )
                                MediaTabChip(
                                    icon = Icons.Outlined.Article,
                                    label = "Texte",
                                    isSelected = selectedMediaTab == "Texte",
                                    onClick = { selectedMediaTab = "Texte" },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Attached Media Preview & Dotted Add Box
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                attachedMediaUri?.let { media ->
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        AsyncImage(
                                            model = media,
                                            contentDescription = "Média attaché",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Top Right Close Icon
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(22.dp)
                                                .clickable { attachedMediaUri = null }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Supprimer",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Dashed Add Media Box
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .drawWithContent {
                                            drawContent()
                                            val stroke = Stroke(
                                                width = 2.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                            )
                                            drawRoundRect(
                                                color = Color(0xFFC084FC),
                                                cornerRadius = CornerRadius(14.dp.toPx()),
                                                style = stroke
                                            )
                                        }
                                        .clickable {
                                            val mime = when (apiMediaType) {
                                                "image" -> "image/*"
                                                "video" -> "video/*"
                                                "audio" -> "audio/*"
                                                else -> "*/*"
                                            }
                                            mediaPicker.launch(mime)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Ajouter un fichier",
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Primary Action Button
                    Button(
                        onClick = {
                            onPublish(
                                titleText.trim(),
                                contentText.trim(),
                                attachedMediaUri,
                                "Communauté",
                                apiMediaType
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                        enabled = contentText.isNotBlank() || titleText.isNotBlank() || attachedMediaUri != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Suivant : Détails >",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTabChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFAF5FF) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) MbotePurplePrimary else Color(0xFFE5E7EB)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MbotePurplePrimary else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MbotePurplePrimary else Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShortVideoCard(
    short: ShortVideo,
    onClick: () -> Unit,
    onCreatorClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(118.dp)
            .height(175.dp)
            .clickable { onClick() }
            .testTag("short_video_card_preview"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Thumbnail
            if (short.videoThumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = short.videoThumbnailUrl,
                    contentDescription = short.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(MbotePurplePrimary, Color(0xFF6D28D9))
                            )
                        )
                )
            }

            // Dark gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            // Duration badge top left
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = short.durationFormatted,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Options / Info icon top right
            IconButton(
                onClick = onCreatorClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profil du créateur",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom Caption & Creator Avatar + Name
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = short.caption,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clickable { onCreatorClick() }
                        .testTag("preview_creator_profile_click")
                ) {
                    AsyncImage(
                        model = short.creatorAvatar,
                        contentDescription = short.creatorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(1.dp, MbotePurplePrimary, CircleShape)
                    )
                    Text(
                        text = short.creatorName.split(" ").firstOrNull() ?: short.creatorName,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

@Composable
private fun PostActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChannelCardItem(
    channel: ChannelInfo,
    onOpen: () -> Unit,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.width(230.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MbotePurplePrimary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = channel.avatarLetter,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${channel.subscribersCount} abonnés",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (channel.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Vérifié",
                        tint = MbotePurplePrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = channel.bio,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CONDITIONAL BUTTON:
            // If NOT subscribed -> Sparkling / scintillating "S'abonner" button with pulsing and sparkle animations!
            // If SUBSCRIBED -> "Voir" button to view publications and share!
            AnimatedContent(
                targetState = channel.isSubscribed,
                label = "channel_button_transition"
            ) { isSubscribed ->
                if (!isSubscribed) {
                    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_loop")
                    val shimmerProg by infiniteTransition.animateFloat(
                        initialValue = -0.5f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1600, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmer"
                    )
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.97f,
                        targetValue = 1.03f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(850, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    val sparkleOpacity by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "opacity"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF7C3AED),
                                        Color(0xFFA855F7),
                                        Color(0xFFF59E0B),
                                        Color(0xFFEC4899),
                                        Color(0xFF7C3AED)
                                    ),
                                    startX = shimmerProg * 350f,
                                    endX = (shimmerProg + 0.6f) * 350f
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFBBF24),
                                        Color(0xFFE879F9),
                                        Color(0xFFFBBF24)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSubscribe() }
                            .testTag("subscribe_channel_btn_${channel.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFEF08A).copy(alpha = sparkleOpacity),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "S'abonner ✨",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("view_channel_btn_${channel.id}"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voir",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Full Screen Channel View:
 * Displays full-screen channel details, published posts, share actions, subscription management,
 * while maintaining the top app bar and bottom navigation menu.
 */
@Composable
fun ChannelDetailContent(
    channel: ChannelInfo,
    onBack: () -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onShareChannel: (ChannelInfo) -> Unit,
    onSharePost: (ChannelPost) -> Unit,
    onLikePost: (ChannelPost) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var copiedToast by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("channel_detail_content")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Channel Header Banner with Navigation Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF581C87),
                                Color(0xFF7C3AED),
                                Color(0xFFEC4899)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { onBack() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Retour aux chaînes",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                    // Channel Avatar overlapping header
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF3B0764),
                        border = BorderStroke(3.dp, Color.White),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 12.dp)
                            .size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = channel.avatarLetter,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        }
                    }

                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = channel.category,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Channel Info & Meta
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (channel.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Vérifié",
                                    tint = MbotePurplePrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Text(
                            text = "${channel.subscribersCount} abonnés",
                            style = MaterialTheme.typography.labelMedium,
                            color = MbotePurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = channel.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Channel Action Buttons (Share Channel & Subscribe toggle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Share Channel Button
                        OutlinedButton(
                            onClick = {
                                onShareChannel(channel)
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Rejoignez la chaîne '${channel.name}' sur MBoté 🇨🇬 : https://mbote.app/channel/${channel.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Partager la chaîne")
                                context.startActivity(shareIntent)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("share_channel_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MbotePurplePrimary
                            ),
                            border = BorderStroke(1.2.dp, MbotePurplePrimary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager la chaîne", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Subscribe / Unsubscribe Toggle Button
                        Button(
                            onClick = { onToggleSubscribe(channel.id) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("toggle_channel_sub_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (channel.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MbotePurplePrimary,
                                contentColor = if (channel.isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                            )
                        ) {
                            Text(
                                text = if (channel.isSubscribed) "Abonné ✓" else "S'abonner ✨",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Publications Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Publications (${channel.posts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Récentes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // List of Channel Posts
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (channel.posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucune publication pour le moment dans cette chaîne.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(channel.posts, key = { it.id }) { post ->
                            ChannelPostCard(
                                post = post,
                                channel = channel,
                                onLike = { onLikePost(post) },
                                onShare = {
                                    onSharePost(post)
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "${post.title}\n\n${post.content}\n\nPartagé depuis la chaîne '${channel.name}' sur MBoté 🇨🇬")
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Partager la publication")
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Individual post item inside a Channel
 */
@Composable
private fun ChannelPostCard(
    post: ChannelPost,
    channel: ChannelInfo,
    onLike: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Post Author Header & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MbotePurplePrimary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = channel.avatarLetter, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = channel.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                Text(
                    text = post.timestamp,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Post Title
            Text(
                text = post.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Post Body Content
            Text(
                text = post.content,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            // Attached Image (if any)
            post.imageUrl?.let { imgUrl ->
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = imgUrl,
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Hashtags
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    post.tags.take(3).forEach { tag ->
                        Text(
                            text = "#$tag",
                            color = MbotePurplePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(6.dp))

            // Post Footer Interaction Actions (Like, Comment, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLike() }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (post.isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Comment count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Commentaires",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share Publication Action (Requested by user)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MbotePurplePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { onShare() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager la publication",
                            tint = MbotePurplePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Partager",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MbotePurplePrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActusPostCard(
    post: NewsPost,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onOpenMediaFullScreen: (url: String, title: String, authorName: String, authorAvatar: String, isVideo: Boolean) -> Unit = { _, _, _, _, _ -> },
    onProfileClick: () -> Unit = {},
    onReportClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showPostMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var currentReactionEmoji by remember { mutableStateOf(if (post.isLiked) "❤️" else null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("news_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Avatar, Name, Timestamp, Subtitle, 3-Dots Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable { onProfileClick() }
                ) {
                    if (post.authorAvatar.isNotBlank()) {
                        AsyncImage(
                            model = post.authorAvatar,
                            contentDescription = post.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MbotePurplePrimary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = post.authorName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${post.timestamp} •",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF3F4F6)
                            ) {
                                Text(
                                    text = post.authorRole,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // 3-Dots Menu Options according to requirement (7)
                Box {
                    IconButton(onClick = { showPostMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options de la publication",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPostMenu,
                        onDismissRequest = { showPostMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Enregistrer la publication") },
                            leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                            onClick = {
                                showPostMenu = false
                                Toast.makeText(context, "Publication enregistrée dans vos favoris", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copier le lien") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                showPostMenu = false
                                Toast.makeText(context, "Lien copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("S'abonner à l'auteur") },
                            leadingIcon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = MbotePurplePrimary) },
                            onClick = {
                                showPostMenu = false
                                Toast.makeText(context, "Vous suivez maintenant ${post.authorName}", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Masquer cette publication") },
                            leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                            onClick = {
                                showPostMenu = false
                                Toast.makeText(context, "Publication masquée de votre fil", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Signaler le contenu") },
                            leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showPostMenu = false
                                onReportClick()
                                Toast.makeText(context, "Signalement envoyé à l'administration MBoté. Merci pour votre vigilance ! 🚩", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post Text Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            // Post Image Attachment -> Full screen viewer on tap according to requirement (6)
            post.imageUrl?.let { imgUrl ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onOpenMediaFullScreen(
                                imgUrl,
                                post.title.ifBlank { post.content },
                                post.authorName,
                                post.authorAvatar,
                                false
                            )
                        }
                ) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = post.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Fullscreen Indicator Overlay Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Plein écran",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Plein écran", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reaction Counts Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentReactionEmoji != null) "$currentReactionEmoji ${post.likesCount + 1}" else if (post.likesCount > 0) "💙 ${post.likesCount}" else "Aucune réaction",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${post.commentsCount} commentaire",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "1 partage",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Bar with Animated Reactions Popup according to requirement (5)
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Views
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Vues",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "V.. 124",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Like Reaction Button (Tap/Long Press reveals reactions popover)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (showReactionPicker) {
                                    showReactionPicker = false
                                } else {
                                    showReactionPicker = true
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        if (currentReactionEmoji != null) {
                            Text(text = currentReactionEmoji!!, fontSize = 15.sp)
                        } else {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Aimer",
                                tint = if (post.isLiked) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentReactionEmoji != null) "Réagi" else "J'aime",
                            fontSize = 11.sp,
                            color = if (currentReactionEmoji != null || post.isLiked) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Comments
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onComment() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Commenter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = post.commentsCount.toString(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                onShare()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Partager",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Partager",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Reaction Emoji Bar Popover overlay
                if (showReactionPicker) {
                    com.loukatech.mbote.ui.components.ReactionPickerBar(
                        onSelectReaction = { emoji ->
                            currentReactionEmoji = emoji
                            onLike()
                            Toast.makeText(context, "Réaction $emoji envoyée !", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { showReactionPicker = false },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-45).dp)
                    )
                }
            }
        }
    }
}
