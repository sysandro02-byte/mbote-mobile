package com.loukatech.mbote.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.loukatech.mbote.model.MeetingItem
import com.loukatech.mbote.service.RtcRoomManager
import com.loukatech.mbote.service.api.MboteApiService
import com.loukatech.mbote.ui.components.RtcVideoSurface
import com.loukatech.mbote.ui.theme.DarkBackground
import com.loukatech.mbote.ui.theme.DarkSurface
import com.loukatech.mbote.ui.theme.PurplePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { MboteApiService() }

    var meetingSeconds by remember(meeting.code) { mutableStateOf(0) }
    var rtc by remember(meeting.code) { mutableStateOf<RtcRoomManager?>(null) }
    var localTrack by remember(meeting.code) { mutableStateOf<VideoTrack?>(null) }
    val remoteTracks = remember(meeting.code) { mutableStateMapOf<String, VideoTrack>() }
    var rtcError by remember(meeting.code) { mutableStateOf<String?>(null) }

    var microphoneAllowed by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraAllowed by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        microphoneAllowed = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        cameraAllowed = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(meeting.code) {
        while (true) {
            delay(1000)
            meetingSeconds++
        }
    }

    LaunchedEffect(meeting.code, microphoneAllowed, cameraAllowed) {
        if (meeting.code.isBlank()) {
            rtcError = "Code de réunion invalide."
            return@LaunchedEffect
        }
        val missing = buildList {
            if (!microphoneAllowed) add(Manifest.permission.RECORD_AUDIO)
            if (!cameraAllowed) add(Manifest.permission.CAMERA)
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
            return@LaunchedEffect
        }

        rtc?.close()
        rtc = null
        remoteTracks.clear()
        localTrack = null
        rtcError = null

        val iceServers = api.fetchLiveIceServers().getOrElse {
            rtcError = it.message ?: "Configuration réseau de la réunion indisponible."
            return@LaunchedEffect
        }
        if (iceServers.isEmpty()) {
            rtcError = "Aucun serveur ICE/TURN n’est disponible."
            return@LaunchedEffect
        }

        runCatching {
            RtcRoomManager(
                context = context,
                roomCode = meeting.code,
                isVideoCall = true,
                iceServerConfig = iceServers,
                onLocalVideoTrack = { track -> scope.launch { localTrack = track } },
                onRemoteVideoTrack = { peerId, track -> scope.launch { remoteTracks[peerId] = track } },
                onPeerLeft = { peerId -> scope.launch { remoteTracks.remove(peerId) } },
            )
        }.onSuccess { rtc = it }
            .onFailure { rtcError = it.message ?: "Le média WebRTC n’a pas pu démarrer." }
    }

    LaunchedEffect(rtc, isMuted, isVideoOff) {
        rtc?.setMicrophoneEnabled(!isMuted)
        rtc?.setVideoEnabled(!isVideoOff)
    }

    DisposableEffect(meeting.code) {
        onDispose {
            rtc?.close()
            rtc = null
            remoteTracks.clear()
        }
    }

    val minutes = meetingSeconds / 60
    val seconds = meetingSeconds % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)
    val mainRemote = remoteTracks.entries.firstOrNull()
    val participantName = meeting.hostName.ifBlank { "Participant MBoté" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("meeting_room_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        meeting.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Code : ${meeting.code} • $durationText",
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
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Text(
                            "${remoteTracks.size + 1} • WebRTC",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    rtcError != null -> {
                        Text(
                            rtcError.orEmpty(),
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    !microphoneAllowed || !cameraAllowed -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text("Autorisation caméra/micro requise", color = Color.White)
                        }
                    }
                    mainRemote != null && rtc != null -> {
                        RtcVideoSurface(
                            track = mainRemote.value,
                            eglContext = rtc!!.eglContext(),
                            mirror = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    localTrack != null && rtc != null && !isVideoOff -> {
                        RtcVideoSurface(
                            track = localTrack!!,
                            eglContext = rtc!!.eglContext(),
                            mirror = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape).background(PurplePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    participantName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (rtc == null) "Connexion à la réunion…" else "En attente d’autres participants",
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (mainRemote != null && localTrack != null && rtc != null && !isVideoOff) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .width(104.dp)
                            .height(150.dp)
                    ) {
                        RtcVideoSurface(
                            track = localTrack!!,
                            eglContext = rtc!!.eglContext(),
                            mirror = true,
                            overlay = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (remoteTracks.size > 1 && rtc != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        remoteTracks.entries.drop(1).take(3).forEach { (_, track) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(92.dp)
                            ) {
                                RtcVideoSurface(
                                    track = track,
                                    eglContext = rtc!!.eglContext(),
                                    mirror = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                        if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
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
                        if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Caméra",
                        tint = if (isVideoOff) PurplePrimary else Color.White
                    )
                }

                IconButton(
                    onClick = { rtc?.switchCamera() },
                    enabled = !isVideoOff,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("meeting_switch_camera")
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Changer de caméra", tint = Color.White)
                }

                IconButton(
                    onClick = onLeaveMeeting,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("meeting_leave_button")
                ) {
                    Icon(
                        Icons.Default.CallEnd,
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(PurplePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.ifBlank { "P" }.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
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
                Text(name.ifBlank { "Participant" }, color = Color.White, fontSize = 10.sp)
                if (isMuted) {
                    Icon(
                        Icons.Default.MicOff,
                        contentDescription = "Micro coupé",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (isVideoOff) {
                    Icon(
                        Icons.Default.VideocamOff,
                        contentDescription = "Caméra coupée",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
