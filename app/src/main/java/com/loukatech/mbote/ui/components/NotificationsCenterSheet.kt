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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loukatech.mbote.model.MboteNotification
import com.loukatech.mbote.model.NotificationType
import com.loukatech.mbote.ui.theme.PurplePrimary

@Composable
fun NotificationsCenterSheet(
    notifications: List<MboteNotification>,
    onDismiss: () -> Unit,
    onNotificationClick: (MboteNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onSimulateFcmPush: (type: NotificationType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Toutes") }
    var showSimulateMenu by remember { mutableStateOf(false) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Messages" -> notifications.filter { it.type == NotificationType.MESSAGE }
            "Emplois" -> notifications.filter { it.type == NotificationType.JOB_APPLICATION }
            "Likes & Reels" -> notifications.filter { it.type == NotificationType.VIDEO_LIKE }
            else -> notifications
        }
    }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(8.dp)
                .testTag("notifications_center_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
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
                                .background(Color(0xFFF5EEFD)),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFEF4444),
                                            contentColor = Color.White
                                        ) {
                                            Text(unreadCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF6B21A8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Centre de Notifications",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (unreadCount > 0) "$unreadCount non lue(s) • Temps réel FCM" else "À jour • Temps réel FCM",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_notifications")) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Mark read, Clear, Simulate Push
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = onMarkAllRead,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tout lire", fontSize = 12.5.sp)
                        }

                        TextButton(
                            onClick = onClearAll,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Effacer", fontSize = 12.5.sp, color = Color(0xFFEF4444))
                        }
                    }

                    // FCM Test Trigger Button
                    Box {
                        OutlinedButton(
                            onClick = { showSimulateMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("simulate_fcm_button")
                        ) {
                            Icon(
                                Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test FCM Push", fontSize = 12.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showSimulateMenu,
                            onDismissRequest = { showSimulateMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("💬 Test Push Message") },
                                onClick = {
                                    showSimulateMenu = false
                                    onSimulateFcmPush(NotificationType.MESSAGE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("💼 Test Push Candidature Emploi") },
                                onClick = {
                                    showSimulateMenu = false
                                    onSimulateFcmPush(NotificationType.JOB_APPLICATION)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("❤️ Test Push Like Video Reel") },
                                onClick = {
                                    showSimulateMenu = false
                                    onSimulateFcmPush(NotificationType.VIDEO_LIKE)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Toutes", "Messages", "Emplois", "Likes & Reels").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFAF5FF),
                                selectedLabelColor = PurplePrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = PurplePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notifications List
                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Aucune notification pour le moment",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Les nouvelles alertes messages, emplois et likes apparaîtront ici.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotifications, key = { it.id }) { item ->
                            NotificationItemCard(
                                notification = item,
                                onClick = {
                                    onNotificationClick(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: MboteNotification,
    onClick: () -> Unit
) {
    val backgroundColor = if (notification.isRead) Color(0xFFF8FAFC) else Color(0xFFFAF5FF)
    val borderColor = if (notification.isRead) Color(0xFFF1F5F9) else Color(0xFFE9D5FF)

    val iconColor = when (notification.type) {
        NotificationType.MESSAGE -> Color(0xFF7C3AED)
        NotificationType.JOB_APPLICATION -> Color(0xFF2563EB)
        NotificationType.VIDEO_LIKE -> Color(0xFFEC4899)
        NotificationType.GIFT_RECEIVED -> Color(0xFFFFB703)
        NotificationType.LIVE_MESSAGE, NotificationType.LIVE_BROADCAST -> Color(0xFFE11D48)
        NotificationType.SYSTEM -> Color(0xFFD97706)
    }

    val typeIcon = when (notification.type) {
        NotificationType.MESSAGE -> Icons.Outlined.ChatBubbleOutline
        NotificationType.JOB_APPLICATION -> Icons.Outlined.WorkOutline
        NotificationType.VIDEO_LIKE -> Icons.Outlined.FavoriteBorder
        NotificationType.GIFT_RECEIVED -> Icons.Outlined.CardGiftcard
        NotificationType.LIVE_MESSAGE, NotificationType.LIVE_BROADCAST -> Icons.Outlined.Videocam
        NotificationType.SYSTEM -> Icons.Outlined.Info
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon or Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (notification.senderAvatar != null) {
                    AsyncImage(
                        model = notification.senderAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = notification.timestamp,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.body,
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                if (notification.actionText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = iconColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = notification.actionText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            var showNotifMenu by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current

            Box(modifier = Modifier.padding(start = 4.dp)) {
                IconButton(
                    onClick = { showNotifMenu = true },
                    modifier = Modifier.size(28.dp).testTag("notif_more_${notification.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options notification",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
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
                        text = { Text("🔕 Silencer cette catégorie") },
                        leadingIcon = { Icon(Icons.Outlined.NotificationsOff, contentDescription = null) },
                        onClick = {
                            showNotifMenu = false
                            android.widget.Toast.makeText(context, "Notifications de cette catégorie masquées", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("🗑️ Supprimer", color = Color(0xFFEF4444)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                        onClick = {
                            showNotifMenu = false
                            android.widget.Toast.makeText(context, "Notification supprimée", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )
            }
        }
    }
}
