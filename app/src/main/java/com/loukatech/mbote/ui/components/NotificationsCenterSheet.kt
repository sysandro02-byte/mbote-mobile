package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loukatech.mbote.model.MboteNotification
import com.loukatech.mbote.model.NotificationType
import com.loukatech.mbote.ui.theme.PurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsCenterSheet(
    notifications: List<MboteNotification>,
    onDismiss: () -> Unit,
    onNotificationClick: (MboteNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Toutes") }
    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Messages" -> notifications.filter { it.type == NotificationType.MESSAGE }
            "Emplois" -> notifications.filter { it.type == NotificationType.JOB_APPLICATION }
            "Likes & Reels" -> notifications.filter { it.type == NotificationType.VIDEO_LIKE }
            else -> notifications
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_notifications")) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Toutes", "Messages", "Emplois", "Likes & Reels").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onMarkAllRead) { Text("Tout marquer comme lu") }
                TextButton(onClick = onClearAll) { Text("Effacer") }
            }
            if (filteredNotifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Aucune notification", fontWeight = FontWeight.SemiBold)
                        Text("Les notifications reçues du serveur apparaîtront ici.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notification ->
                        NotificationItemRowRefined(notification = notification, onClick = { onNotificationClick(notification) })
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemRowRefined(
    notification: MboteNotification,
    onClick: () -> Unit
) {
    var showNotifMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Overlay Badge Icon & Background Color based on Notification Type matching social reference
    val (badgeIcon, badgeColor) = when (notification.type) {
        NotificationType.MESSAGE -> Pair(Icons.Default.Chat, Color(0xFF10B981))
        NotificationType.JOB_APPLICATION -> Pair(Icons.Default.Person, Color(0xFF2563EB))
        NotificationType.VIDEO_LIKE -> Pair(Icons.Default.ThumbUp, Color(0xFF2563EB))
        NotificationType.GIFT_RECEIVED -> Pair(Icons.Default.CardGiftcard, Color(0xFFF59E0B))
        NotificationType.LIVE_MESSAGE, NotificationType.LIVE_BROADCAST -> Pair(Icons.Default.Videocam, Color(0xFFEF4444))
        NotificationType.SYSTEM -> Pair(Icons.Default.Flag, Color(0xFFEA580C))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (!notification.isRead) Color(0xFFEBF3FF) else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large Avatar + Circular Badge Overlapping at Bottom Right
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (notification.senderAvatar != null) {
                        AsyncImage(
                            model = notification.senderAvatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PurplePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = notification.title.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = PurplePrimary
                            )
                        }
                    }
                }

                // Overlapping badge icon on bottom right
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    border = BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body content: Title (Bold) + text body + elapsed time inline
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append(notification.title + " ")
                            append(notification.body + " ")
                        },
                        fontSize = 13.5.sp,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = notification.timestamp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                if (!notification.actionText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Options 3-dot Menu Button
            Box(modifier = Modifier.padding(start = 4.dp)) {
                IconButton(
                    onClick = { showNotifMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("notif_more_${notification.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showNotifMenu,
                    onDismissRequest = { showNotifMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (notification.isRead) "✉️ Marquer comme non lue" else "✓ Marquer comme lue") },
                        leadingIcon = { Icon(Icons.Outlined.MarkAsUnread, contentDescription = null) },
                        onClick = {
                            showNotifMenu = false
                            android.widget.Toast.makeText(context, "Statut de notification mis à jour", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🔕 Ne plus recevoir cette alerte") },
                        leadingIcon = { Icon(Icons.Outlined.NotificationsOff, contentDescription = null) },
                        onClick = {
                            showNotifMenu = false
                            android.widget.Toast.makeText(context, "Catégorie masquée", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("🗑️ Supprimer la notification", color = Color(0xFFEF4444)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                        onClick = {
                            showNotifMenu = false
                            android.widget.Toast.makeText(context, "Notification supprimée", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

