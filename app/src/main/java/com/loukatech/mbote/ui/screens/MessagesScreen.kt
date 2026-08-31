package com.loukatech.mbote.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.loukatech.mbote.model.*
import com.loukatech.mbote.service.PartnerTypingState
import com.loukatech.mbote.ui.components.FilterChipRow
import com.loukatech.mbote.ui.components.StatusRingAvatar
import com.loukatech.mbote.ui.theme.PurpleLight
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    chats: List<Chat>,
    statuses: List<StatusItem>,
    userProfile: UserProfile,
    searchQuery: String,
    selectedFilter: String,
    isSyncing: Boolean = false,
    socketTypingMap: Map<String, PartnerTypingState> = emptyMap(),
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onOpenContactsSync: () -> Unit = {},
    onJoinByLinkClick: () -> Unit = {},
    onStatusClick: (StatusItem) -> Unit,
    onAddStatusClick: () -> Unit,
    onProfileClick: (String, String) -> Unit = { _, _ -> },
    onConfirmDesktopQr: suspend (String) -> Result<Unit> = { Result.failure(IllegalStateException("Connexion QR indisponible")) },
    modifier: Modifier = Modifier
) {
    var filterOptions = listOf("Tous", "Discussions", "Groupes", "Canaux", "Non lus")
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showImportantMessagesDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(searchQuery.isNotBlank()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val desktopQrScanner = remember(context) { GmsBarcodeScanning.getClient(context) }

    fun scanDesktopQr() {
        desktopQrScanner.startScan()
            .addOnSuccessListener { barcode ->
                val payload = barcode.rawValue.orEmpty()
                coroutineScope.launch {
                    onConfirmDesktopQr(payload)
                        .onSuccess { Toast.makeText(context, "Mboté PC connecté avec succès.", Toast.LENGTH_LONG).show() }
                        .onFailure { Toast.makeText(context, it.message ?: "Connexion QR impossible.", Toast.LENGTH_LONG).show() }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, error.message ?: "Scanner QR indisponible.", Toast.LENGTH_LONG).show()
            }
    }

    if (showQrScannerDialog) {
        com.loukatech.mbote.ui.components.QrCodeScannerDialog(
            userProfile = userProfile,
            allChats = chats,
            onOpenChat = { chatId ->
                onChatClick(chatId)
            },
            onDismiss = { showQrScannerDialog = false }
        )
    }

    if (showImportantMessagesDialog) {
        val starredMessages = remember(chats) {
            chats.flatMap { c -> c.messages.map { m -> c to m } }.filter { it.second.isStarred }
        }
        AlertDialog(
            onDismissRequest = { showImportantMessagesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Messages importants", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Retrouvez ici tous les messages que vous avez marqués d'une étoile ⭐ :", fontSize = 13.sp)
                    if (starredMessages.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Aucun message marqué d'une étoile pour le moment.",
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(starredMessages) { (chatItem, msg) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("${chatItem.name} • ${msg.timestamp}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                        Text("« ${msg.text} »", fontSize = 13.sp, fontStyle = FontStyle.Italic)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImportantMessagesDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) {
                    Text("Compris")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // 📌 FIXED UPPER SECTION (Title, Search, Statuts Récents, Filter Chips)
            // Stays strictly fixed at top while messages scroll underneath.
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .zIndex(2f)
            ) {
                // 1. Header Banner: "Messages" title with gradient underline & quick action buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title "Messages" with purple gradient underline bar
                        Column {
                            Text(
                                text = "Messages",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF7C3AED),
                                                Color(0xFF9333EA),
                                                Color(0xFFC084FC)
                                            )
                                        )
                                    )
                            )
                        }

                        // Action Buttons: Search loupe toggle, Synchronize contacts, 3 Dots Menu inside squircles
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Standalone Search Icon button (placed FIRST: Search -> QR Scanner -> Contacts -> 3-dots)
                            Surface(
                                onClick = {
                                    isSearchVisible = !isSearchVisible
                                    if (!isSearchVisible && searchQuery.isNotBlank()) {
                                        onSearchChange("")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSearchVisible || searchQuery.isNotBlank()) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("header_search_toggle_button")
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

                            // QR Code Scanner button (placed NEXT to Search button)
                            Surface(
                                onClick = { showQrScannerDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("header_qr_scanner_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scanner un QR Code de contact",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Contacts sync button
                            Surface(
                                onClick = onOpenContactsSync,
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("header_contacts_sync_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = "Contacts synchronisés",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                             // 3-Dots More Options squircle button
                            Box {
                                Surface(
                                    onClick = { showOptionsMenu = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("header_more_options")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Nouveau groupe") },
                                        leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = PurplePrimary) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onNewChatClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rejoindre via un lien") },
                                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onJoinByLinkClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Contacts synchronisés") },
                                        leadingIcon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onOpenContactsSync()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Messages importants ⭐") },
                                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308)) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showImportantMessagesDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Web MBoté (Scanner QR)") },
                                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PurplePrimary) },
                                        onClick = {
                                            showOptionsMenu = false
                                            scanDesktopQr()
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Tout marquer comme lu") },
                                        leadingIcon = { Icon(Icons.Outlined.DoneAll, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            android.widget.Toast.makeText(context, "Toutes les discussions ont été marquées comme lues", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Archiver toutes les discussions") },
                                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            android.widget.Toast.makeText(context, "Discussions archivées", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Search Bar (Appears conditionally on search loupe click)
                AnimatedVisibility(
                    visible = isSearchVisible || searchQuery.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Recherche",
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = onSearchChange,
                                    placeholder = {
                                        Text(
                                            text = "Rechercher discussions, contacts…",
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = PurplePrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_search_bar")
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchChange("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Effacer la recherche",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔍 ${chats.size} discussion(s) trouvée(s)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PurplePrimary
                                )
                                Text(
                                    text = "Effacer",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onSearchChange("") }
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Status Stories Carousel (Statuts récents)
                if (searchQuery.isBlank()) {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "Statuts récents",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // My Status Item
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onAddStatusClick() }
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        StatusRingAvatar(
                                            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                                            hasStory = false,
                                            size = 52.dp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(PurplePrimary)
                                                .padding(1.5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Ajouter statut",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Mon statut",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Other Users Stories
                            items(statuses.filter { !it.isMine }) { status ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onStatusClick(status) }
                                ) {
                                    StatusRingAvatar(
                                        avatarUrl = status.userAvatar,
                                        hasStory = true,
                                        isViewed = status.isViewed,
                                        size = 52.dp,
                                        onClick = { onStatusClick(status) }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = status.userName.split(" ").firstOrNull() ?: status.userName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Filter Chips Row
                FilterChipRow(
                    filters = filterOptions,
                    selectedFilter = selectedFilter,
                    onSelectFilter = onFilterChange,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )

                // Subtle bottom border separating fixed top section from scrolling list
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }

            // =========================================================================
            // 📜 SCROLLABLE DISCUSSIONS / MESSAGES LIST
            // Passes underneath the fixed status & header section smoothly!
            // =========================================================================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                if (isSyncing) {
                    item {
                        com.loukatech.mbote.ui.components.MessageListSkeleton(count = 7)
                    }
                } else if (chats.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = PurpleLight,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Aucune discussion trouvée" else "Aucune discussion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Aucun contact, groupe ou message ne correspond à « $searchQuery »"
                                else
                                    "Démarrez une nouvelle conversation en appuyant sur le bouton +",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onSearchChange("") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Réinitialiser la recherche")
                                }
                            }
                        }
                    }
                } else {
                    items(chats, key = { it.id }) { chat ->
                        val isPartnerTyping = socketTypingMap[chat.id]?.isTyping == true
                        ChatItemRow(
                            chat = chat,
                            isTyping = isPartnerTyping,
                            searchQuery = searchQuery,
                            onClick = { onChatClick(chat.id) },
                            onProfileClick = { onProfileClick(chat.name, chat.avatar) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Start New Chat
        FloatingActionButton(
            onClick = onNewChatClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 20.dp)
                .testTag("new_chat_fab"),
            containerColor = PurplePrimary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = "Nouveau message"
            )
        }
    }
}

@Composable
private fun ChatItemRow(
    chat: Chat,
    isTyping: Boolean = false,
    searchQuery: String = "",
    onClick: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("chat_item_${chat.id}"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar with Online Indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = chat.avatar,
                    contentDescription = chat.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    contentScale = ContentScale.Crop
                )
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Chat Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = chat.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Vérifié",
                                tint = PurplePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isTyping) "À l'instant" else chat.lastMessageTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTyping || chat.unreadCount > 0) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isTyping || chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastMsg = chat.messages.lastOrNull()
                    val isMine = lastMsg?.isMine == true

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTyping) {
                            // Real-time animated typing indicator subtitle
                            Text(
                                text = "✍️ En train d'écrire…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PurplePrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            if (isMine) {
                                val tickIcon = when (lastMsg?.status) {
                                    MessageStatus.READ -> Icons.Outlined.DoneAll
                                    MessageStatus.DELIVERED -> Icons.Outlined.DoneAll
                                    else -> Icons.Outlined.Check
                                }
                                val tickTint = when (lastMsg?.status) {
                                    MessageStatus.READ -> Color(0xFF0284C7) // Double blue tick!
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Icon(
                                    imageVector = tickIcon,
                                    contentDescription = if (lastMsg?.status == MessageStatus.READ) "Lu" else "Envoyé",
                                    tint = tickTint,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp)
                                )
                            }

                            Text(
                                text = chat.lastMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (chat.unreadCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = PurplePrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
