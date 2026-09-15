package com.loukatech.mbote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.MeetingItem
import com.loukatech.mbote.ui.theme.DarkBackground
import com.loukatech.mbote.ui.theme.DarkSurface
import com.loukatech.mbote.ui.theme.PurplePrimary
import kotlinx.coroutines.delay

@Composable
fun MeetingRoomScreen(
    meeting: MeetingItem,
    isMuted: Boolean,
    isVideoOff: Boolean,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onLeaveMeeting: () -> Unit,
    modifier: Modifier = Modifier
) {
    var meetingSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            meetingSeconds++
        }
    }

    val minutes = meetingSeconds / 60
    val seconds = meetingSeconds % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)

    // The API currently exposes the meeting membership count but not remote
    // media tracks. Never invent identities: render only the authenticated
    // device preview until signaling supplies actual remote participants.
    val participantName = meeting.hostName.ifBlank { "Participant" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("meeting_room_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Meeting Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = meeting.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code : ${meeting.code} • $durationText",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "WebRTC Mesh HD",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Authenticated participant preview. Remote tiles are added only
            // from real signaling participants, never from a local sample list.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                MeetingVideoTile(
                    name = participantName,
                    avatar = "",
                    isVideoOff = isVideoOff,
                    isMuted = isMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                )
            }

            // Bottom In-Meeting Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                        .testTag("meeting_mute_toggle")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Micro",
                        tint = if (isMuted) PurplePrimary else Color.White
                    )
                }

                IconButton(
                    onClick = onToggleVideo,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isVideoOff) Color.White else Color.White.copy(alpha = 0.2f))
                        .testTag("meeting_video_toggle")
                ) {
                    Icon(
                        imageVector = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Caméra",
                        tint = if (isVideoOff) PurplePrimary else Color.White
                    )
                }

                IconButton(
                    onClick = { /* Screen share toggle */ },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = "Partager écran",
                        tint = Color.White
                    )
                }

                // Leave Meeting Button
                IconButton(
                    onClick = onLeaveMeeting,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("meeting_leave_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Quitter",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MeetingVideoTile(
    name: String,
    avatar: String,
    isVideoOff: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!isVideoOff) {
                // Real CameraX stream for the authenticated participant
                com.loukatech.mbote.ui.components.CameraVideoPreview(
                    isVideoOff = isVideoOff,
                    avatarUrl = avatar,
                    userName = name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Name & Status Badge Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isMuted) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Micro coupé",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
