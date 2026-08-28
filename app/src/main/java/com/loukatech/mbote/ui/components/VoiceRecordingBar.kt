package com.loukatech.mbote.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.loukatech.mbote.service.AudioRecorderManager
import com.loukatech.mbote.service.RecordingState
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft
import java.io.File

@Composable
fun VoiceRecordingInputBar(
    recorderManager: AudioRecorderManager,
    onSendVoiceMessage: (file: File, durationSec: Int, waveform: List<Float>) -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordingState by recorderManager.recordingState.collectAsState()
    val amplitudes by recorderManager.amplitudes.collectAsState()

    var showPermissionRationale by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            recorderManager.startRecording()
        } else {
            showPermissionRationale = true
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            icon = { Icon(Icons.Default.Mic, contentDescription = null, tint = PurplePrimary) },
            title = { Text("Permission Microphone requise", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Pour enregistrer et envoyer des messages vocaux haute fidélité chiffrés sur MBoté, veuillez accorder l'accès au microphone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    val isRecording = recordingState is RecordingState.Recording
    val isPaused = recordingState is RecordingState.Paused

    val durationSec = when (val state = recordingState) {
        is RecordingState.Recording -> state.durationSec
        is RecordingState.Paused -> state.durationSec
        is RecordingState.Completed -> state.durationSec
        else -> 0
    }

    val formattedDuration = remember(durationSec) {
        val mins = durationSec / 60
        val secs = durationSec % 60
        String.format("%02d:%02d", mins, secs)
    }

    // Blinking red recording dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "recording_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("voice_recording_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Delete / Trash Action
            IconButton(
                onClick = {
                    recorderManager.cancelRecording()
                    onCancelRecording()
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .testTag("cancel_voice_record_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Supprimer l'enregistrement",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Duration & Pulsing indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPaused) Color(0xFFF59E0B)
                            else Color(0xFFEF4444).copy(alpha = dotAlpha)
                        )
                )
                Text(
                    text = formattedDuration,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Waveform Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                LiveRecordingWaveform(
                    amplitudes = amplitudes,
                    isRecording = isRecording,
                    isPaused = isPaused,
                    waveColor = PurplePrimary
                )
            }

            // Pause / Resume Toggle
            IconButton(
                onClick = {
                    if (isPaused) {
                        recorderManager.resumeRecording()
                    } else if (isRecording) {
                        recorderManager.pauseRecording()
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PurpleSoft)
                    .testTag("pause_resume_record_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Reprendre" else "Mettre en pause",
                    tint = PurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Send Voice Message Button
            IconButton(
                onClick = {
                    val recordedFile = recorderManager.stopRecording()
                    if (recordedFile != null) {
                        onSendVoiceMessage(
                            recordedFile,
                            durationSec.coerceAtLeast(1),
                            amplitudes.ifEmpty { List(25) { 0.3f } }
                        )
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PurplePrimary)
                    .testTag("send_voice_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Envoyer le message vocal",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
