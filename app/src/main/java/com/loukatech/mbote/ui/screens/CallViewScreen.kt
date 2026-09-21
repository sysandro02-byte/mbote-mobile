package com.loukatech.mbote.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.loukatech.mbote.model.CallItem
import com.loukatech.mbote.service.RtcRoomManager
import com.loukatech.mbote.service.api.MboteApiService
import com.loukatech.mbote.ui.components.RtcVideoSurface
import com.loukatech.mbote.ui.theme.DarkBackground
import com.loukatech.mbote.ui.theme.PurpleDark
import com.loukatech.mbote.ui.theme.PurplePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack

@Composable
fun CallViewScreen(
    call: CallItem,
    onEndCall: (durationText: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { MboteApiService() }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val previousAudioMode = remember { audioManager.mode }
    val previousSpeakerState = remember { audioManager.isSpeakerphoneOn }

    var isMuted by remember(call.id) { mutableStateOf(false) }
    var isVideoOff by remember(call.id) { mutableStateOf(!call.isVideo) }
    var isSpeakerOn by remember(call.id) { mutableStateOf(call.isVideo) }
    var callSeconds by remember(call.id) { mutableStateOf(0) }
    var rtc by remember(call.id) { mutableStateOf<RtcRoomManager?>(null) }
    var localTrack by remember(call.id) { mutableStateOf<VideoTrack?>(null) }
    var remoteTrack by remember(call.id) { mutableStateOf<VideoTrack?>(null) }
    var rtcError by remember(call.id) { mutableStateOf<String?>(null) }

    var microphoneAllowed by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraAllowed by remember {
        mutableStateOf(
            !call.isVideo ||
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
        cameraAllowed = !call.isVideo ||
            grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    val connected = call.callState.equals("CONNECTED", ignoreCase = true)

    LaunchedEffect(call.id, connected) {
        if (!connected) return@LaunchedEffect
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    LaunchedEffect(call.id, connected, microphoneAllowed, cameraAllowed) {
        if (!connected || call.roomCode.isBlank()) return@LaunchedEffect

        val missingPermissions = buildList {
            if (!microphoneAllowed) add(Manifest.permission.RECORD_AUDIO)
            if (call.isVideo && !cameraAllowed) add(Manifest.permission.CAMERA)
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            return@LaunchedEffect
        }

        rtc?.close()
        rtc = null
        localTrack = null
        remoteTrack = null
        rtcError = null

        val iceServers = api.fetchLiveIceServers().getOrElse {
            rtcError = it.message ?: "Configuration réseau de l’appel indisponible."
            return@LaunchedEffect
        }
        if (iceServers.isEmpty()) {
            rtcError = "Aucun serveur ICE/TURN n’est disponible."
            return@LaunchedEffect
        }

        runCatching {
            RtcRoomManager(
                context = context,
                roomCode = call.roomCode,
                isVideoCall = call.isVideo,
                iceServerConfig = iceServers,
                onLocalVideoTrack = { track -> scope.launch { localTrack = track } },
                onRemoteVideoTrack = { _, track -> scope.launch { remoteTrack = track } },
                onPeerLeft = { scope.launch { remoteTrack = null } },
            )
        }.onSuccess { rtc = it }
            .onFailure { rtcError = it.message ?: "Le média WebRTC n’a pas pu démarrer." }
    }

    LaunchedEffect(rtc, isMuted, isVideoOff) {
        rtc?.setMicrophoneEnabled(!isMuted)
        rtc?.setVideoEnabled(call.isVideo && !isVideoOff)
    }

    LaunchedEffect(connected, isSpeakerOn) {
        if (connected) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = isSpeakerOn
        }
    }

    DisposableEffect(call.id) {
        onDispose {
            rtc?.close()
            rtc = null
            @Suppress("DEPRECATION")
            runCatching { audioManager.isSpeakerphoneOn = previousSpeakerState }
            runCatching { audioManager.mode = previousAudioMode }
        }
    }

    val minutes = callSeconds / 60
    val seconds = callSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)
    val statusText = when {
        rtcError != null -> rtcError.orEmpty()
        !connected -> when (call.callState.uppercase()) {
            "RINGING" -> "Appel en cours…"
            "CONNECTING" -> "Connexion…"
            else -> call.callState.ifBlank { "Connexion…" }
        }
        !microphoneAllowed || (call.isVideo && !cameraAllowed) -> "Autorisation caméra/micro requise"
        else -> if (call.isVideo) "Appel vidéo • $formattedDuration" else "Appel vocal • $formattedDuration"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PurpleDark, DarkBackground)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("call_view_screen")
    ) {
        if (call.isVideo && connected && remoteTrack != null && rtc != null && !isVideoOff) {
            RtcVideoSurface(
                track = remoteTrack!!,
                eglContext = rtc!!.eglContext(),
                mirror = false,
                modifier = Modifier.fillMaxSize()
            )
            localTrack?.let { track ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 88.dp, end = 16.dp)
                        .width(112.dp)
                        .height(168.dp)
                ) {
                    RtcVideoSurface(
                        track = track,
                        eglContext = rtc!!.eglContext(),
                        mirror = true,
                        overlay = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Text("WebRTC sécurisé", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (!call.isVideo || remoteTrack == null || !connected || isVideoOff) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(4.dp)
                    ) {
                        AsyncImage(
                            model = call.avatar.takeIf { it.isNotBlank() },
                            contentDescription = call.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    call.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    statusText,
                    color = if (rtcError == null) Color.White.copy(alpha = 0.8f) else Color(0xFFFCA5A5),
                    fontSize = 15.sp
                )
            }

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
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        enabled = connected,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color.Black.copy(alpha = 0.35f))
                            .testTag("call_mute_button")
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Micro",
                            tint = if (isMuted) PurplePrimary else Color.White
                        )
                    }

                    IconButton(
                        onClick = { if (call.isVideo) isVideoOff = !isVideoOff },
                        enabled = connected && call.isVideo,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isVideoOff) Color.White else Color.Black.copy(alpha = 0.35f))
                            .testTag("call_video_button")
                    ) {
                        Icon(
                            if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Caméra",
                            tint = if (isVideoOff) PurplePrimary else Color.White
                        )
                    }

                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        enabled = connected,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) Color.White else Color.Black.copy(alpha = 0.35f))
                            .testTag("call_speaker_button")
                    ) {
                        Icon(
                            if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Haut-parleur",
                            tint = if (isSpeakerOn) PurplePrimary else Color.White
                        )
                    }

                    if (call.isVideo) {
                        IconButton(
                            onClick = { rtc?.switchCamera() },
                            enabled = connected && !isVideoOff,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .testTag("call_switch_camera_button")
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Changer de caméra", tint = Color.White)
                        }
                    }
                }

                IconButton(
                    onClick = {
                        val duration = if (minutes > 0) "$minutes min $seconds s" else "$seconds s"
                        onEndCall(duration)
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("call_end_button")
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Raccrocher",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
