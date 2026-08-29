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
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Toutes") }
    var showSimulateMenu by remember { mutableStateOf(false) }
    var showWidgetCard by remember { mutableStateOf(true) }
    var isSimulatingOffline by remember { mutableStateOf(false) }

    val defaultRichNotifications = remember(notifications) {
        if (notifications.size < 4) {
            listOf(
                MboteNotification(
                    id = "notif_loukatech_views",
                    type = NotificationType.SYSTEM,
                    title = "LoukaTech",
                    body = "a 18 nouvelles vues sur ses publications.",
                    timestamp = "7 j",
                    isRead = false,
                    senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    actionText = "LoukaTech"
                ),
                MboteNotification(
                    id = "notif_followers_mbot",
                    type = NotificationType.JOB_APPLICATION,
                    title = "Mwamba Toto et Silas Sass",
                    body = "vous suivent à présent sur MBoté.",
                    timestamp = "21 j",
                    isRead = false,
                    senderAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    actionText = "LoukaTech"
                ),
                MboteNotification(
                    id = "notif_security_alert",
                    type = NotificationType.SYSTEM,
                    title = "Sécurité MBoté",
                    body = "Nous avons détecté une nouvelle connexion depuis un appareil à Brazzaville.",
                    timestamp = "3 j",
                    isRead = true,
                    senderAvatar = null,
                    actionText = "Vérifier l'appareil"
                ),
                MboteNotification(
                    id = "notif_comment_like",
                    type = NotificationType.VIDEO_LIKE,
                    title = "Hevecel Freud",
                    body = "aime votre commentaire : « Testez MBoté HD Voice... »",
                    timestamp = "2 j",
                    isRead = true,
                    senderAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    actionText = "MBoté Feed"
                ),
                MboteNotification(
                    id = "notif_mention_pub",
                    type = NotificationType.MESSAGE,
                    title = "BS GABON",
                    body = "a mentionné votre nom et celui d'autres abonnés dans une publication.",
                    timestamp = "19 j",
                    isRead = true,
                    senderAvatar = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150",
                    actionText = "OSE-K SARL"
                )
            ) + notifications
        } else {
            notifications
        }
    }

    val filteredNotifications = remember(defaultRichNotifications, selectedFilter) {
        when (selectedFilter) {
            "Messages" -> defaultRichNotifications.filter { it.type == NotificationType.MESSAGE }
            "Emplois" -> defaultRichNotifications.filter { it.type == NotificationType.JOB_APPLICATION }
            "Likes & Reels" -> defaultRichNotifications.filter { it.type == NotificationType.VIDEO_LIKE }
            else -> defaultRichNotifications
        }
    }

    val recentNotifications = remember(filteredNotifications) {
        filteredNotifications.filter { !it.isRead || it.timestamp.contains("h") || it.timestamp.contains("j") && it.timestamp.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 10 < 10 }
    }

    val olderNotifications = remember(filteredNotifications, recentNotifications) {
        filteredNotifications.filter { !recentNotifications.contains(it) }
    }

    val unreadCount = remember(defaultRichNotifications) { defaultRichNotifications.count { !it.isRead } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF0F6FF), // Soft clean blue background matching Facebook/social reference
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(4.dp)
                .testTag("notifications_center_sheet")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isSimulatingOffline) 52.dp else 0.dp)
                ) {
                    // Header Bar (Large Title Notifications + Search Icon + Menu + Close)
                    Surface(
                        color = Color.White,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu",
                                            tint = Color(0xFF0F172A)
                                        )
                                    }

                                    Text(
                                        text = "Notifications",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )

                                    if (unreadCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444)
                                        ) {
                                            Text(
                                                text = unreadCount.toString(),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            isSimulatingOffline = !isSimulatingOffline
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "Rechercher",
                                            tint = Color(0xFF0F172A)
                                        )
                                    }

                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("close_notifications")
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Fermer",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action pills & Push FCM Test Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.clickable { onMarkAllRead() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tout marquer lue", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFFEF2F2),
                                        modifier = Modifier.clickable { onClearAll() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Effacer", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                        }
                                    }
                                }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = PurplePrimary,
                                        modifier = Modifier
                                            .clickable { showSimulateMenu = true }
                                            .testTag("simulate_fcm_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Test Push", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showSimulateMenu,
                                        onDismissRequest = { showSimulateMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("💬 Push Message MBoté") },
                                            onClick = {
                                                showSimulateMenu = false
                                                onSimulateFcmPush(NotificationType.MESSAGE)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("💼 Push Opportunité Emploi") },
                                            onClick = {
                                                showSimulateMenu = false
                                                onSimulateFcmPush(NotificationType.JOB_APPLICATION)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("❤️ Push Interaction Reel") },
                                            onClick = {
                                                showSimulateMenu = false
                                                onSimulateFcmPush(NotificationType.VIDEO_LIKE)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Notification Feed
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
                    ) {
                        // Section: Nouveau
                        item {
                            Text(
                                text = "Nouveau",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(recentNotifications.ifEmpty { defaultRichNotifications.take(2) }, key = { "recent_${it.id}" }) { notif ->
                            NotificationItemRowRefined(
                                notification = notif,
                                onClick = { onNotificationClick(notif) }
                            )
                        }

                        // Embedded Feature / Promo Card ("Soyez informé(e) plus vite") matching image 1
                        if (showWidgetCard) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Soyez informé(e) plus vite",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Nouveau ! Vous pouvez suivre vos appels VoIP HD, alertes Caller ID IA et actualités MBoté directement sur votre écran d'accueil avec le widget MBoté.",
                                            fontSize = 13.sp,
                                            color = Color(0xFF334155),
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        
                                        // Blue primary button
                                        Button(
                                            onClick = {
                                                android.widget.Toast.makeText(context, "Widget MBoté ajouté à l'écran d'accueil !", android.widget.Toast.LENGTH_SHORT).show()
                                                showWidgetCard = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(42.dp)
                                        ) {
                                            Text("Ajouter le widget", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Gray secondary button
                                        Button(
                                            onClick = { showWidgetCard = false },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color(0xFF1E293B)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(42.dp)
                                        ) {
                                            Text("Plus tard", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Plus ancien
                        if (olderNotifications.isNotEmpty() || recentNotifications.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Plus ancien",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }

                            items(olderNotifications.ifEmpty { defaultRichNotifications.drop(2) }, key = { "older_${it.id}" }) { notif ->
                                NotificationItemRowRefined(
                                    notification = notif,
                                    onClick = { onNotificationClick(notif) }
                                )
                            }
                        }
                    }
                }

                // Offline Network Notice Bar matching image 1
                if (isSimulatingOffline) {
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Connexion impossible actuellement.",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = { isSimulatingOffline = false },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "RÉESSAYER",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
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

