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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.CallItem
import com.loukatech.mbote.ui.theme.DarkBackground
import com.loukatech.mbote.ui.theme.PurpleDark
import com.loukatech.mbote.ui.theme.PurplePrimary
import kotlinx.coroutines.delay

@Composable
fun CallViewScreen(
    call: CallItem,
    onEndCall: (durationText: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(!call.isVideo) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var callSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val minutes = callSeconds / 60
    val seconds = callSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PurpleDark, DarkBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("call_view_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Chiffré de bout en bout",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = call.avatar,
                        contentDescription = call.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = call.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (call.isVideo) "Appel vidéo en direct • $formattedDuration" else "Appel vocal sécurisé • $formattedDuration",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp
                )
            }

            // Real CameraX Live Video Stream Feed
            if (call.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(vertical = 12.dp)
                ) {
                    com.loukatech.mbote.ui.components.CameraVideoPreview(
                        isVideoOff = isVideoOff,
                        avatarUrl = call.avatar,
                        userName = "Vous",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom In-Call Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Mic
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                            .testTag("call_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Micro",
                            tint = if (isMuted) PurplePrimary else Color.White
                        )
                    }

                    // Video toggle
                    IconButton(
                        onClick = { isVideoOff = !isVideoOff },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isVideoOff) Color.White else Color.White.copy(alpha = 0.2f))
                            .testTag("call_video_button")
                    ) {
                        Icon(
                            imageVector = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Caméra",
                            tint = if (isVideoOff) PurplePrimary else Color.White
                        )
                    }

                    // Speaker
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f))
                            .testTag("call_speaker_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Haut-parleur",
                            tint = if (isSpeakerOn) PurplePrimary else Color.White
                        )
                    }
                }

                // Big Red End Call Button
                IconButton(
                    onClick = {
                        val durationStr = if (minutes > 0) {
                            "$minutes min $seconds s"
                        } else {
                            "$seconds s"
                        }
                        onEndCall(durationStr)
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("call_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Raccrocher",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
