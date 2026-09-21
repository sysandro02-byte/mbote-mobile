package com.loukatech.mbote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loukatech.mbote.service.LiveWebRtcManager
import com.loukatech.mbote.service.MboteSocketManager
import com.loukatech.mbote.service.api.LiveStreamDto
import com.loukatech.mbote.service.api.MboteApiService
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun LiveViewerDialog(
    live: LiveStreamDto,
    currentUserName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mboteApi = remember { MboteApiService() }
    var rtc by remember { mutableStateOf<LiveWebRtcManager?>(null) }
    var remoteTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var viewerCount by remember { mutableStateOf(live.viewerCount) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val comments = remember { mutableStateListOf<LiveComment>() }

    fun closeViewer() {
        rtc?.close()
        rtc = null
        scope.launch { mboteApi.leaveLive(live.id) }
        onDismiss()
    }

    LaunchedEffect(live.id) {
        mboteApi.joinLive(live.id)
            .onSuccess {
                rtc = LiveWebRtcManager(
                    context = context,
                    streamId = live.id,
                    broadcaster = false,
                    onRemoteVideoTrack = { track -> remoteTrack = track }
                )
            }
            .onFailure { error = it.message ?: "Impossible de rejoindre ce Live." }
    }

    LaunchedEffect(live.id) {
        MboteSocketManager.liveStreamEvents.collect { event ->
            if (event.streamId != live.id) return@collect
            when (event.type) {
                "LIVE_VIEWER_COUNT" -> viewerCount = event.viewerCount
                "LIVE_COMMENT" -> event.payloadText?.takeIf { it.isNotBlank() }?.let { text ->
                    comments.add(
                        LiveComment(
                            id = event.timestamp.toString(),
                            senderName = event.senderName,
                            text = text,
                            timestamp = "Maintenant"
                        )
                    )
                }
                "LIVE_STATUS" -> if (event.status == "ENDED") closeViewer()
            }
        }
    }

    DisposableEffect(live.id) {
        onDispose {
            rtc?.close()
            rtc = null
        }
    }

    Dialog(
        onDismissRequest = ::closeViewer,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                error != null -> {
                    Text(
                        text = error.orEmpty(),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                remoteTrack != null && rtc != null -> {
                    key(remoteTrack) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceViewRenderer(ctx).also { renderer ->
                                    renderer.init(rtc!!.eglContext(), null)
                                    renderer.setMirror(false)
                                    remoteTrack!!.addSink(renderer)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                    Text(
                        text = "Connexion au Live…",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 92.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.Red, shape = RoundedCornerShape(6.dp)) {
                            Text(" LIVE ", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("$viewerCount spectateurs", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(live.title, color = Color.White, fontSize = 16.sp)
                    if (live.hostName.isNotBlank()) {
                        Text(live.hostName, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                IconButton(
                    onClick = ::closeViewer,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Quitter", tint = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
            ) {
                if (comments.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(comments.takeLast(20)) { item ->
                            Surface(
                                color = Color.Black.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${item.senderName}: ${item.text}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        placeholder = { Text("Commenter…", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = comment.trim()
                            if (text.isNotBlank()) {
                                MboteSocketManager.sendLiveComment(
                                    streamId = live.id,
                                    senderName = currentUserName.ifBlank { "Spectateur" },
                                    text = text
                                )
                                comment = ""
                            }
                        },
                        modifier = Modifier.background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Envoyer", tint = Color.Black)
                    }
                }
            }
        }
    }
}
