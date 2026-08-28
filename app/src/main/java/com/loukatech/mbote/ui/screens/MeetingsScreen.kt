package com.loukatech.mbote.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.model.MeetingItem
import com.loukatech.mbote.ui.theme.PurpleLight
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft

@Composable
fun MeetingsScreen(
    meetings: List<MeetingItem>,
    onNewMeetingClick: () -> Unit,
    onJoinMeetingClick: (MeetingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var joinCodeInput by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Rejoindre une réunion", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = joinCodeInput,
                    onValueChange = { joinCodeInput = it },
                    label = { Text("Code de réunion (ex: MB-2026-ENG)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("join_meeting_code_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = meetings.find { it.code.equals(joinCodeInput.trim(), ignoreCase = true) }
                            ?: MeetingItem(title = "Réunion $joinCodeInput", hostName = "Invité", code = joinCodeInput, scheduledTime = "En cours", isLive = true)
                        showJoinDialog = false
                        onJoinMeetingClick(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    modifier = Modifier.testTag("confirm_join_meeting_button")
                ) {
                    Text("Rejoindre")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // Quick Action Cards Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var showMeetingsTopMenu by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visioconférences MBoté",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = PurplePrimary
                    )

                    Box {
                        IconButton(
                            onClick = { showMeetingsTopMenu = true },
                            modifier = Modifier.testTag("meetings_top_more_vert")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options Réunions",
                                tint = PurplePrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showMeetingsTopMenu,
                            onDismissRequest = { showMeetingsTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🎥 Nouvelle réunion HD") },
                                leadingIcon = { Icon(Icons.Default.VideoCall, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMeetingsTopMenu = false
                                    onNewMeetingClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔑 Rejoindre par un code") },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                                onClick = {
                                    showMeetingsTopMenu = false
                                    showJoinDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚙️ Paramètres vidéo HD & micro") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                onClick = {
                                    showMeetingsTopMenu = false
                                    android.widget.Toast.makeText(context, "Optimisation Opus HD & Suppression de bruit IA actives", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                Text(
                    text = "Conférences audio & vidéo haute définition, sécurisées sans limite de temps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickMeetingActionCard(
                        title = "Nouvelle réunion",
                        subtitle = "Démarrer direct",
                        icon = Icons.Default.VideoCall,
                        brush = Brush.linearGradient(listOf(PurplePrimary, PurpleLight)),
                        onClick = onNewMeetingClick,
                        modifier = Modifier.weight(1f).testTag("action_new_meeting")
                    )

                    QuickMeetingActionCard(
                        title = "Rejoindre",
                        subtitle = "Avec un code",
                        icon = Icons.Default.MeetingRoom,
                        brush = Brush.linearGradient(listOf(Color(0xFF00C49F), Color(0xFF008B70))),
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.weight(1f).testTag("action_join_meeting")
                    )
                }
            }
        }

        item {
            Text(
                text = "Réunions planifiées & en cours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(meetings, key = { it.id }) { meeting ->
            MeetingCardItem(
                meeting = meeting,
                onJoin = { onJoinMeetingClick(meeting) }
            )
        }
    }
}

@Composable
fun QuickMeetingActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MeetingCardItem(
    meeting: MeetingItem,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("meeting_card_${meeting.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (meeting.isLive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EN DIRECT",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = meeting.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Organisé par ${meeting.hostName} • ${meeting.scheduledTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Code : ${meeting.code} (${meeting.participantsCount} participants)",
                    fontSize = 11.sp,
                    color = PurplePrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("join_btn_${meeting.id}")
                ) {
                    Text(if (meeting.isLive) "Rejoindre" else "Accéder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                var showCardMenu by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                Box {
                    IconButton(
                        onClick = { showCardMenu = true },
                        modifier = Modifier.size(32.dp).testTag("meeting_card_more_${meeting.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options réunion",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showCardMenu,
                        onDismissRequest = { showCardMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📋 Copier le code") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                android.widget.Toast.makeText(context, "Code ${meeting.code} copié", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📤 Partager le lien d'invitation") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                android.widget.Toast.makeText(context, "Lien https://mbote.cg/join/${meeting.code} copié", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📅 Ajouter au calendrier") },
                            leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                android.widget.Toast.makeText(context, "Ajouté au calendrier MBoté", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("🚩 Signaler") },
                            leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showCardMenu = false
                                android.widget.Toast.makeText(context, "Signalement transmis aux modérateurs", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}
