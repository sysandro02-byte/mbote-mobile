package com.loukatech.mbote.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.CallItem
import com.loukatech.mbote.model.CallType
import com.loukatech.mbote.model.SyncedContact
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft

data class FrequentContactSummary(
    val name: String,
    val avatar: String,
    val phoneNumber: String,
    val totalCalls: Int,
    val isVideo: Boolean,
    val latestDuration: String
)

data class CallRecordingItem(
    val id: String,
    val callerName: String,
    val callerNumber: String,
    val callerAvatar: String,
    val timestamp: String,
    val durationText: String,
    val fileSizeText: String = "2.8 MB",
    val isIncoming: Boolean = true,
    val customName: String? = null
)

/**
 * Summary Header displayed at the top of the Calls Tab showing:
 * 1. Interaction Statistics (Total, Incoming, Outgoing, Missed & proportional bar)
 * 2. Most Contacted Individuals Carousel with quick 1-tap call triggers
 */
@Composable
fun CallSummaryHeader(
    calls: List<CallItem>,
    syncedContacts: List<SyncedContact> = emptyList(),
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onStartCall: (name: String, avatar: String, isVideo: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCalls = calls.size
    val incomingCount = calls.count { it.type == CallType.INCOMING }
    val outgoingCount = calls.count { it.type == CallType.OUTGOING }
    val missedCount = calls.count { it.type == CallType.MISSED }

    // Top contacted contacts derived from calls history
    val frequentContacts = remember(calls, syncedContacts) {
        val grouped = calls.groupBy { it.name }
        grouped.entries
            .sortedByDescending { it.value.size }
            .take(6)
            .map { entry ->
                val name = entry.key
                val personCalls = entry.value
                val sampleCall = personCalls.first()
                val matchedContact = syncedContacts.firstOrNull { it.name.equals(name, ignoreCase = true) }
                FrequentContactSummary(
                    name = name,
                    avatar = matchedContact?.avatarUrl ?: sampleCall.avatar,
                    phoneNumber = sampleCall.phoneNumber,
                    totalCalls = personCalls.size,
                    isVideo = sampleCall.isVideo,
                    latestDuration = personCalls.firstOrNull { it.durationText.isNotBlank() && it.durationText != "Manqué" }?.durationText ?: "Récents"
                )
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Stats Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            ),
            border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calls_interaction_stats_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Title and badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Statistiques d'appels",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = PurplePrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$totalCalls interaction${if (totalCalls > 1) "s" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PurplePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPillItem(
                        label = "Entrants",
                        count = incomingCount,
                        icon = Icons.AutoMirrored.Filled.CallReceived,
                        color = Color(0xFF10B981),
                        isSelected = selectedFilter == "Tous",
                        onClick = { onSelectFilter("Tous") },
                        modifier = Modifier.weight(1f)
                    )
                    StatPillItem(
                        label = "Sortants",
                        count = outgoingCount,
                        icon = Icons.AutoMirrored.Filled.CallMade,
                        color = PurplePrimary,
                        isSelected = selectedFilter == "Tous",
                        onClick = { onSelectFilter("Tous") },
                        modifier = Modifier.weight(1f)
                    )
                    StatPillItem(
                        label = "Manqués",
                        count = missedCount,
                        icon = Icons.AutoMirrored.Filled.CallMissed,
                        color = Color(0xFFEF4444),
                        isSelected = selectedFilter == "Manqués",
                        onClick = { onSelectFilter(if (selectedFilter == "Manqués") "Tous" else "Manqués") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Proportional Progress Bar if there are calls
                if (totalCalls > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (incomingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(incomingCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFF10B981))
                            )
                        }
                        if (outgoingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(outgoingCount.toFloat())
                                    .fillMaxHeight()
                                    .background(PurplePrimary)
                            )
                        }
                        if (missedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(missedCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFFEF4444))
                            )
                        }
                    }
                }
            }
        }

        // 2. Most Contacted Individuals Carousel
        if (frequentContacts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PLUS CONTACTÉS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = PurplePrimary
                )
                Text(
                    text = "${frequentContacts.size} fréquents",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("most_contacted_row")
            ) {
                items(frequentContacts, key = { "frequent_${it.name}" }) { item ->
                    FrequentContactCard(
                        item = item,
                        onAudioCall = { onStartCall(item.name, item.avatar, false) },
                        onVideoCall = { onStartCall(item.name, item.avatar, true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPillItem(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FrequentContactCard(
    item: FrequentContactSummary,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onAudioCall,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        shadowElevation = 1.dp,
        modifier = modifier
            .width(118.dp)
            .testTag("frequent_contact_${item.name}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with Badge
            Box(
                modifier = Modifier.size(46.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(PurpleSoft)
                ) {
                    AsyncImage(
                        model = item.avatar,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Call count badge
                Surface(
                    color = PurplePrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(17.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${item.totalCalls}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Contact Name
            Text(
                text = item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duration / Last exchange
            Text(
                text = item.latestDuration,
                fontSize = 10.sp,
                color = PurplePrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action buttons (Audio & Video)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Surface(
                    onClick = onAudioCall,
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Appel audio",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Surface(
                    onClick = onVideoCall,
                    shape = CircleShape,
                    color = PurplePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Appel vidéo",
                            tint = PurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card for toggling Automatic Call Recording (Incoming & Outgoing calls)
 */
@Composable
fun AutoCallRecordingCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, if (isEnabled) PurplePrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("auto_call_recording_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) PurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Outlined.GraphicEq else Icons.Outlined.MicOff,
                            contentDescription = null,
                            tint = if (isEnabled) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Enregistrement automatique",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnabled) "Actif • Entrants et sortants" else "Désactivé",
                            fontSize = 12.sp,
                            color = if (isEnabled) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        onToggle(checked)
                        Toast.makeText(
                            context,
                            if (checked) "Enregistrement automatique activé" else "Enregistrement automatique désactivé",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PurplePrimary
                    ),
                    modifier = Modifier.testTag("auto_record_switch")
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = PurplePrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AudioFile, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Qualité HD AAC", fontSize = 11.sp, color = PurplePrimary, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stockage chiffré", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section displaying saved call recordings list
 */
@Composable
fun CallRecordingListSection(
    recordings: List<CallRecordingItem>,
    onPlayRecording: (CallRecordingItem) -> Unit,
    onDeleteRecording: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ENREGISTREMENTS SAUVEGARDÉS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "${recordings.size} fichier(s)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recordings.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun enregistrement vocal disponible",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recordings.forEach { recording ->
                    CallRecordingCard(
                        recording = recording,
                        onPlay = { onPlayRecording(recording) },
                        onDelete = { onDeleteRecording(recording.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallRecordingCard(
    recording: CallRecordingItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onRename: ((newName: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember(recording.customName) { mutableStateOf(recording.customName ?: "Enregistrement_${recording.callerName}") }

    val displayTitle = recording.customName ?: recording.callerName

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_card_${recording.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play/Pause Button
                Surface(
                    onClick = {
                        isPlaying = !isPlaying
                        onPlay()
                    },
                    shape = CircleShape,
                    color = PurplePrimary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Écouter",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayTitle,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (recording.customName != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = PurplePrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Renommé",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${recording.callerName} (${recording.callerNumber}) • ${recording.timestamp}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = PurplePrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = recording.durationText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = recording.fileSizeText,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Rename Action
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Renommer",
                            tint = PurplePrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Share Action
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_SUBJECT, "Clip Audio - $displayTitle")
                                putExtra(Intent.EXTRA_TEXT, "Enregistrement vocal d'appel MBoté : $displayTitle (${recording.durationText})")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Partager le clip audio"))
                            Toast.makeText(context, "Ouverture du partage pour : $displayTitle", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Partager",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Delete Action
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }

    // Rename Prompt Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = PurplePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Renommer le clip audio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Saisissez le nouveau nom pour cet enregistrement local :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        label = { Text("Nom du fichier") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInputText.trim()
                        if (trimmed.isNotBlank()) {
                            onRename?.invoke(trimmed)
                            Toast.makeText(context, "Renommé en : $trimmed", Toast.LENGTH_SHORT).show()
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * Interactive Call Recording Player Modal Dialog
 */
@Composable
fun CallRecordingPlayerDialog(
    recording: CallRecordingItem,
    onDismiss: () -> Unit,
    onCallBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0.35f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = PurplePrimary
                )
                Text(
                    text = "Enregistrement d'appel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Caller profile
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PurpleSoft)
                ) {
                    AsyncImage(
                        model = recording.callerAvatar,
                        contentDescription = recording.callerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recording.callerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${recording.callerNumber} • ${recording.timestamp}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform / Audio Progress
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    colors = SliderDefaults.colors(
                        thumbColor = PurplePrimary,
                        activeTrackColor = PurplePrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "01:12", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = recording.durationText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { progress = (progress - 0.1f).coerceAtLeast(0f) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Replay10, contentDescription = "Reculer", modifier = Modifier.size(20.dp))
                        }
                    }

                    Surface(
                        onClick = { isPlaying = !isPlaying },
                        shape = CircleShape,
                        color = PurplePrimary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { progress = (progress + 0.1f).coerceAtMost(1f) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Forward10, contentDescription = "Avancer", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onCallBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rappeler")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Detailed Call Stats Analytics Section (Duration, Avg, Peak hours)
 */
@Composable
fun CallStatsAnalyticsSection(
    calls: List<CallItem>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("call_stats_analytics_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aperçu du temps de parole",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsMetricItem(
                    title = "Temps total",
                    value = "3h 42 min",
                    subtitle = "Cette semaine",
                    icon = Icons.Outlined.Timer,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricItem(
                    title = "Durée moyenne",
                    value = "8 min 12 s",
                    subtitle = "Par appel",
                    icon = Icons.Outlined.Schedule,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricItem(
                    title = "Heure de pointe",
                    value = "18h - 20h",
                    subtitle = "Fin de journée",
                    icon = Icons.Outlined.Speed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsMetricItem(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = subtitle, fontSize = 9.sp, color = PurplePrimary)
    }
}

/**
 * Dedicated local management view in the call details screen for accessed saved recordings,
 * including the ability to play, rename, or share these clips.
 */
@Composable
fun CallDetailSheetDialog(
    call: CallItem,
    recordings: List<CallRecordingItem>,
    onDismiss: () -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onOpenChat: () -> Unit,
    onUpdateRecordingName: (recordingId: String, newName: String) -> Unit,
    onDeleteRecording: (recordingId: String) -> Unit,
    onPlayRecording: (CallRecordingItem) -> Unit
) {
    var localRecordings by remember(recordings) { mutableStateOf(recordings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Top Header Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(PurpleSoft)
                    ) {
                        AsyncImage(
                            model = call.avatar,
                            contentDescription = call.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = call.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = PurplePrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Vérifié MBoté",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${call.phoneNumber} • ${call.timestamp}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = when (call.type) {
                                    CallType.INCOMING -> Color(0xFF10B981).copy(alpha = 0.12f)
                                    CallType.OUTGOING -> PurplePrimary.copy(alpha = 0.12f)
                                    CallType.MISSED -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when (call.type) {
                                        CallType.INCOMING -> "Appel Entrant (${call.durationText})"
                                        CallType.OUTGOING -> "Appel Sortant (${call.durationText})"
                                        CallType.MISSED -> "Appel Manqué"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (call.type) {
                                        CallType.INCOMING -> Color(0xFF047857)
                                        CallType.OUTGOING -> PurplePrimary
                                        CallType.MISSED -> Color(0xFFEF4444)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row (Call, Video, Chat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onStartCall(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Appeler", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onStartCall(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vidéo HD", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenChat()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", fontSize = 11.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // DEDICATED LOCAL MANAGEMENT VIEW FOR SAVED RECORDINGS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FolderSpecial,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gestionnaire d'Enregistrements",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = PurplePrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${localRecordings.size} clip(s)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Consultez, écoutez, renommez ou partagez vos enregistrements vocaux conservés localement.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                if (localRecordings.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Aucun enregistrement sauvegardé pour cet appel.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        localRecordings.forEach { rec ->
                            CallRecordingCard(
                                recording = rec,
                                onPlay = { onPlayRecording(rec) },
                                onDelete = {
                                    localRecordings = localRecordings.filterNot { it.id == rec.id }
                                    onDeleteRecording(rec.id)
                                },
                                onRename = { newName ->
                                    localRecordings = localRecordings.map {
                                        if (it.id == rec.id) it.copy(customName = newName) else it
                                    }
                                    onUpdateRecordingName(rec.id, newName)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text("Fermer")
            }
        }
    )
}
