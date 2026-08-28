package com.loukatech.mbote.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.model.Chat
import com.loukatech.mbote.model.MediaType
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

data class StorageMediaItem(
    val id: String,
    val title: String,
    val type: MediaType, // VIDEO, IMAGE, AUDIO, FILE
    val sizeBytes: Long,
    val chatName: String,
    val chatId: String,
    val dateFormatted: String,
    val thumbnailUrl: String? = null
) {
    val sizeFormatted: String
        get() = formatStorageSizeBytes(sizeBytes)
}

enum class StorageTab {
    LARGE_FILES, BY_CONVERSATION, BY_TYPE
}

fun formatStorageSizeBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        val rounded = (mb * 10).toInt() / 10.0
        "$rounded Mo"
    } else {
        val kb = (bytes / 1024.0).toInt()
        "$kb Ko"
    }
}

fun List<StorageMediaItem>.calculateTotalBytes(): Long {
    var sum = 0L
    for (item in this) {
        sum += item.sizeBytes
    }
    return sum
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerDialog(
    onDismiss: () -> Unit,
    chats: List<Chat> = emptyList(),
    modifier: Modifier = Modifier
) {
    var mediaItems by remember {
        mutableStateOf(
            listOf(
                StorageMediaItem(
                    id = "m1",
                    title = "Vidéo HD - Concert Rumba Kintele 2026.mp4",
                    type = MediaType.VIDEO,
                    sizeBytes = 44_878_950L,
                    chatName = "Team MBoté Congo",
                    chatId = "c1",
                    dateFormatted = "22 Août 2026",
                    thumbnailUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500"
                ),
                StorageMediaItem(
                    id = "m2",
                    title = "Film Docu - Pont Brazzaville Kinshasa.mp4",
                    type = MediaType.VIDEO,
                    sizeBytes = 40_055_600L,
                    chatName = "MBoté Officiel",
                    chatId = "c2",
                    dateFormatted = "19 Août 2026",
                    thumbnailUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=500"
                ),
                StorageMediaItem(
                    id = "m3",
                    title = "Album Photos HD - Soirée Anniversaire.zip",
                    type = MediaType.FILE,
                    sizeBytes = 29_884_400L,
                    chatName = "Grace Ondongo",
                    chatId = "c3",
                    dateFormatted = "15 Août 2026",
                    thumbnailUrl = null
                ),
                StorageMediaItem(
                    id = "m4",
                    title = "Reportage - Marché Poto-Poto Brazza.mp4",
                    type = MediaType.VIDEO,
                    sizeBytes = 25_270_000L,
                    chatName = "Arsène Kouka",
                    chatId = "c4",
                    dateFormatted = "12 Août 2026",
                    thumbnailUrl = "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=500"
                ),
                StorageMediaItem(
                    id = "m5",
                    title = "Studio Session - Instrumental Afrobeat.wav",
                    type = MediaType.AUDIO,
                    sizeBytes = 19_504_000L,
                    chatName = "Régis Loufoua",
                    chatId = "c5",
                    dateFormatted = "10 Août 2026",
                    thumbnailUrl = null
                ),
                StorageMediaItem(
                    id = "m6",
                    title = "Photos 4K - Plage de Pointe-Noire.png",
                    type = MediaType.IMAGE,
                    sizeBytes = 17_196_000L,
                    chatName = "Grace Ondongo",
                    chatId = "c3",
                    dateFormatted = "08 Août 2026",
                    thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500"
                ),
                StorageMediaItem(
                    id = "m7",
                    title = "Rapport Projet MBoté V2 Finale.pdf",
                    type = MediaType.FILE,
                    sizeBytes = 12_897_000L,
                    chatName = "Team MBoté Congo",
                    chatId = "c1",
                    dateFormatted = "05 Août 2026",
                    thumbnailUrl = null
                ),
                StorageMediaItem(
                    id = "m8",
                    title = "Message Vocal - Explication Stratégie (12 min).opus",
                    type = MediaType.AUDIO,
                    sizeBytes = 10_276_000L,
                    chatName = "Divin Mabiala",
                    chatId = "c6",
                    dateFormatted = "02 Août 2026",
                    thumbnailUrl = null
                ),
                StorageMediaItem(
                    id = "m9",
                    title = "Danse Traditionnelle Sapeurs Brazza.mp4",
                    type = MediaType.VIDEO,
                    sizeBytes = 8_808_000L,
                    chatName = "MBoté Officiel",
                    chatId = "c2",
                    dateFormatted = "28 Juil 2026",
                    thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500"
                ),
                StorageMediaItem(
                    id = "m10",
                    title = "Coucher de Soleil - Fleuve Congo.jpg",
                    type = MediaType.IMAGE,
                    sizeBytes = 7_025_000L,
                    chatName = "Grace Ondongo",
                    chatId = "c3",
                    dateFormatted = "25 Juil 2026",
                    thumbnailUrl = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=500"
                ),
                StorageMediaItem(
                    id = "m11",
                    title = "Maquette UI MBoté Mobile.png",
                    type = MediaType.IMAGE,
                    sizeBytes = 4_404_000L,
                    chatName = "Arsène Kouka",
                    chatId = "c4",
                    dateFormatted = "20 Juil 2026",
                    thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500"
                ),
                StorageMediaItem(
                    id = "m12",
                    title = "Note Vocale Réunion Client.opus",
                    type = MediaType.AUDIO,
                    sizeBytes = 3_250_000L,
                    chatName = "Team MBoté Congo",
                    chatId = "c1",
                    dateFormatted = "18 Juil 2026",
                    thumbnailUrl = null
                )
            )
        )
    }

    var cacheSizeBytes by remember { mutableStateOf(14_500_000L) }
    var selectedTab by remember { mutableStateOf(StorageTab.LARGE_FILES) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMediaIds by remember { mutableStateOf(setOf<String>()) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var itemToDeleteConfirm by remember { mutableStateOf<StorageMediaItem?>(null) }
    var chatToDeleteConfirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    var typeToDeleteConfirm by remember { mutableStateOf<MediaType?>(null) }

    val totalMediaBytes = remember(mediaItems) { mediaItems.calculateTotalBytes() }
    val totalUsedBytes = totalMediaBytes + cacheSizeBytes

    val videoBytes = remember(mediaItems) { mediaItems.filter { it.type == MediaType.VIDEO }.calculateTotalBytes() }
    val imageBytes = remember(mediaItems) { mediaItems.filter { it.type == MediaType.IMAGE }.calculateTotalBytes() }
    val audioBytes = remember(mediaItems) { mediaItems.filter { it.type == MediaType.AUDIO }.calculateTotalBytes() }
    val fileBytes = remember(mediaItems) { mediaItems.filter { it.type == MediaType.FILE }.calculateTotalBytes() }

    val formattedTotalUsed = remember(totalUsedBytes) { formatStorageSizeBytes(totalUsedBytes) }

    val filteredMediaItems = remember(mediaItems, searchQuery) {
        mediaItems.filter { item ->
            searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.chatName.contains(searchQuery, ignoreCase = true)
        }.sortedByDescending { it.sizeBytes }
    }

    val conversationMap = remember(mediaItems) {
        mediaItems.groupBy { it.chatId }
    }

    val selectedItemsList = remember(selectedMediaIds, mediaItems) {
        mediaItems.filter { selectedMediaIds.contains(it.id) }
    }
    val selectedBytes = remember(selectedItemsList) { selectedItemsList.calculateTotalBytes() }
    val formattedSelectedBytes = remember(selectedBytes) { formatStorageSizeBytes(selectedBytes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 16.dp,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("storage_manager_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MbotePurpleSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Stockage et Données",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Utilisé : $formattedTotalUsed sur 64 Go",
                                style = MaterialTheme.typography.bodySmall,
                                color = MbotePurplePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .testTag("close_storage_dialog")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Storage Overview Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Répartition du stockage",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF1E293B)
                            )
                            if (cacheSizeBytes > 0) {
                                TextButton(
                                    onClick = {
                                        val freed = cacheSizeBytes
                                        cacheSizeBytes = 0L
                                        val freedText = formatStorageSizeBytes(freed)
                                        notificationMessage = "Cache vidé ($freedText libérés) !"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Vider cache",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MbotePurplePrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Segmented Progress Bar
                        val maxStorageForBar = (totalUsedBytes.coerceAtLeast(1L)).toFloat()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            if (videoBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(videoBytes / maxStorageForBar)
                                        .fillMaxHeight()
                                        .background(MbotePurplePrimary)
                                )
                            }
                            if (imageBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(imageBytes / maxStorageForBar)
                                        .fillMaxHeight()
                                        .background(Color(0xFFEC4899))
                                )
                            }
                            if (audioBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(audioBytes / maxStorageForBar)
                                        .fillMaxHeight()
                                        .background(Color(0xFF3B82F6))
                                )
                            }
                            if (fileBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(fileBytes / maxStorageForBar)
                                        .fillMaxHeight()
                                        .background(Color(0xFFF59E0B))
                                )
                            }
                            if (cacheSizeBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(cacheSizeBytes / maxStorageForBar)
                                        .fillMaxHeight()
                                        .background(Color(0xFF94A3B8))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Color Legends Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StorageLegendItem(
                                color = MbotePurplePrimary,
                                label = "Vidéos",
                                sizeText = formatStorageSizeBytes(videoBytes)
                            )
                            StorageLegendItem(
                                color = Color(0xFFEC4899),
                                label = "Photos",
                                sizeText = formatStorageSizeBytes(imageBytes)
                            )
                            StorageLegendItem(
                                color = Color(0xFF3B82F6),
                                label = "Vocaux",
                                sizeText = formatStorageSizeBytes(audioBytes)
                            )
                            StorageLegendItem(
                                color = Color(0xFFF59E0B),
                                label = "Docs",
                                sizeText = formatStorageSizeBytes(fileBytes)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification Pill
                AnimatedVisibility(
                    visible = notificationMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = notificationMessage ?: "",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { notificationMessage = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Tabs Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        StorageTab.LARGE_FILES to "Volumineux (${filteredMediaItems.size})",
                        StorageTab.BY_CONVERSATION to "Par discussion",
                        StorageTab.BY_TYPE to "Par type"
                    )

                    tabs.forEach { (tab, title) ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) Color(0xFFFAF5FF) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MbotePurplePrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = tab }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MbotePurplePrimary else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content View according to selectedTab
                when (selectedTab) {
                    StorageTab.LARGE_FILES -> {
                        Column(modifier = Modifier.weight(1f)) {
                            // Search and Select All Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Rechercher un média...") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Effacer",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MbotePurplePrimary,
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color(0xFFF8FAFC)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                val allSelected = filteredMediaItems.isNotEmpty() &&
                                        filteredMediaItems.all { selectedMediaIds.contains(it.id) }

                                FilterChip(
                                    selected = allSelected,
                                    onClick = {
                                        selectedMediaIds = if (allSelected) {
                                            emptySet()
                                        } else {
                                            filteredMediaItems.map { it.id }.toSet()
                                        }
                                    },
                                    label = {
                                        Text(
                                            if (allSelected) "Désélectionner" else "Tout sélec.",
                                            fontSize = 11.5.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFAF5FF),
                                        selectedLabelColor = MbotePurplePrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (filteredMediaItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Aucun fichier volumineux trouvé !",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            "Votre stockage est propre et optimisé.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredMediaItems, key = { it.id }) { media ->
                                        val isSelected = selectedMediaIds.contains(media.id)
                                        StorageMediaItemCard(
                                            media = media,
                                            isSelected = isSelected,
                                            onToggleSelect = {
                                                selectedMediaIds = if (isSelected) {
                                                    selectedMediaIds - media.id
                                                } else {
                                                    selectedMediaIds + media.id
                                                }
                                            },
                                            onDeleteSingle = {
                                                itemToDeleteConfirm = media
                                            }
                                        )
                                    }
                                }
                            }

                            // Bottom Delete Bar if selection is not empty
                            if (selectedMediaIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "${selectedMediaIds.size} sélectionné(s)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF991B1B)
                                            )
                                            Text(
                                                text = "Total : $formattedSelectedBytes",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFFB91C1C)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val count = selectedMediaIds.size
                                                val freedMb = formattedSelectedBytes

                                                mediaItems = mediaItems.filterNot { selectedMediaIds.contains(it.id) }
                                                selectedMediaIds = emptySet()
                                                notificationMessage = "$count élément(s) supprimé(s) ($freedMb libérés) !"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Supprimer", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StorageTab.BY_CONVERSATION -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(conversationMap.entries.toList(), key = { it.key }) { (chatId, items) ->
                                val chatName = items.firstOrNull()?.chatName ?: "Discussion"
                                val chatTotalBytes = items.calculateTotalBytes()
                                val formattedChatSize = formatStorageSizeBytes(chatTotalBytes)

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
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
                                                        .background(Color(0xFFE9D5FF)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = chatName.take(2).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MbotePurplePrimary,
                                                        fontSize = 14.sp
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = chatName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "${items.size} média(s) • $formattedChatSize",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    chatToDeleteConfirm = chatId to chatName
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color(0xFFDC2626)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Nettoyer", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items.take(3).forEach { m ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = when (m.type) {
                                                                MediaType.VIDEO -> Icons.Default.Videocam
                                                                MediaType.IMAGE -> Icons.Default.Image
                                                                MediaType.AUDIO -> Icons.Default.Mic
                                                                else -> Icons.Default.Description
                                                            },
                                                            contentDescription = null,
                                                            tint = MbotePurplePrimary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = m.sizeFormatted,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }
                                            if (items.size > 3) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFAF5FF),
                                                    border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                                                ) {
                                                    Text(
                                                        text = "+${items.size - 3}",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MbotePurplePrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StorageTab.BY_TYPE -> {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val typesList = listOf(
                                Triple(MediaType.VIDEO, "Vidéos", videoBytes),
                                Triple(MediaType.IMAGE, "Photos & Images", imageBytes),
                                Triple(MediaType.AUDIO, "Messages vocaux", audioBytes),
                                Triple(MediaType.FILE, "Documents & Fichiers", fileBytes)
                            )

                            typesList.forEach { (type, label, bytes) ->
                                val count = mediaItems.count { it.type == type }
                                val formattedSize = formatStorageSizeBytes(bytes)

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (type) {
                                                            MediaType.VIDEO -> Color(0xFFFAF5FF)
                                                            MediaType.IMAGE -> Color(0xFFFDF2F8)
                                                            MediaType.AUDIO -> Color(0xFFEFF6FF)
                                                            else -> Color(0xFFFFFBEB)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (type) {
                                                        MediaType.VIDEO -> Icons.Default.Videocam
                                                        MediaType.IMAGE -> Icons.Default.Image
                                                        MediaType.AUDIO -> Icons.Default.Mic
                                                        else -> Icons.Default.Description
                                                    },
                                                    contentDescription = null,
                                                    tint = when (type) {
                                                        MediaType.VIDEO -> MbotePurplePrimary
                                                        MediaType.IMAGE -> Color(0xFFEC4899)
                                                        MediaType.AUDIO -> Color(0xFF3B82F6)
                                                        else -> Color(0xFFF59E0B)
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = label,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.5.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "$count fichier(s) • $formattedSize",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                if (count > 0) {
                                                    typeToDeleteConfirm = type
                                                }
                                            },
                                            enabled = count > 0,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (count > 0) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFDC2626)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Vider", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Single item delete dialog confirmation
    itemToDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDeleteConfirm = null },
            title = {
                Text("Supprimer ce fichier ?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Voulez-vous supprimer '${item.title}' (${item.sizeFormatted}) ? Cette action est irréversible.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val freedMb = item.sizeFormatted
                        mediaItems = mediaItems.filterNot { it.id == item.id }
                        selectedMediaIds = selectedMediaIds - item.id
                        notificationMessage = "Fichier supprimé ($freedMb libérés) !"
                        itemToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteConfirm = null }) {
                    Text("Annuler", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Chat delete confirmation
    chatToDeleteConfirm?.let { (chatId, chatName) ->
        val chatItems = mediaItems.filter { it.chatId == chatId }
        val chatBytes = chatItems.calculateTotalBytes()
        val formattedSize = formatStorageSizeBytes(chatBytes)

        AlertDialog(
            onDismissRequest = { chatToDeleteConfirm = null },
            title = {
                Text("Nettoyer '$chatName' ?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Voulez-vous supprimer tous les ${chatItems.size} médias de cette discussion ($formattedSize) ?",
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mediaItems = mediaItems.filterNot { it.chatId == chatId }
                        selectedMediaIds = selectedMediaIds.filterNot { id ->
                            chatItems.any { it.id == id }
                        }.toSet()
                        notificationMessage = "Médias de '$chatName' supprimés ($formattedSize libérés) !"
                        chatToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Supprimer tout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDeleteConfirm = null }) {
                    Text("Annuler", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Type delete confirmation
    typeToDeleteConfirm?.let { type ->
        val typeItems = mediaItems.filter { it.type == type }
        val typeBytes = typeItems.calculateTotalBytes()
        val formattedSize = formatStorageSizeBytes(typeBytes)
        val label = when (type) {
            MediaType.VIDEO -> "toutes les vidéos"
            MediaType.IMAGE -> "toutes les photos"
            MediaType.AUDIO -> "tous les messages vocaux"
            else -> "tous les documents"
        }

        AlertDialog(
            onDismissRequest = { typeToDeleteConfirm = null },
            title = {
                Text("Vider $label ?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Voulez-vous supprimer les ${typeItems.size} fichiers de ce type ($formattedSize) ?",
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mediaItems = mediaItems.filterNot { it.type == type }
                        selectedMediaIds = selectedMediaIds.filterNot { id ->
                            typeItems.any { it.id == id }
                        }.toSet()
                        notificationMessage = "Fichiers de ce type supprimés ($formattedSize libérés) !"
                        typeToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { typeToDeleteConfirm = null }) {
                    Text("Annuler", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun StorageMediaItemCard(
    media: StorageMediaItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDeleteSingle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFFAF5FF) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isSelected) MbotePurplePrimary else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MbotePurplePrimary
                ),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (media.type) {
                            MediaType.VIDEO -> Color(0xFFFAF5FF)
                            MediaType.IMAGE -> Color(0xFFFDF2F8)
                            MediaType.AUDIO -> Color(0xFFEFF6FF)
                            else -> Color(0xFFFFFBEB)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (media.thumbnailUrl != null) {
                    AsyncImage(
                        model = media.thumbnailUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (media.type) {
                            MediaType.VIDEO -> Icons.Default.Videocam
                            MediaType.IMAGE -> Icons.Default.Image
                            MediaType.AUDIO -> Icons.Default.Mic
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = when (media.type) {
                            MediaType.VIDEO -> MbotePurplePrimary
                            MediaType.IMAGE -> Color(0xFFEC4899)
                            MediaType.AUDIO -> Color(0xFF3B82F6)
                            else -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = media.chatName,
                        fontSize = 11.5.sp,
                        color = MbotePurplePrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "• ${media.dateFormatted}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = media.sizeFormatted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MbotePurplePrimary
                )
                IconButton(
                    onClick = onDeleteSingle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    sizeText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label ($sizeText)",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569)
        )
    }
}
