package com.loukatech.mbote.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.ui.theme.PurplePrimary

@Composable
fun FullScreenMediaViewerDialog(
    mediaUrl: String,
    isVideo: Boolean = false,
    senderName: String = "",
    timestamp: String = "",
    caption: String = "",
    onDismiss: () -> Unit,
    onReaction: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).testTag("fullscreen_media_viewer")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    offset = Offset((centerX - tapOffset.x) * 1.2f, (centerY - tapOffset.y) * 1.2f)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.7f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                    (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else offset = Offset.Zero
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isVideo) {
                    Text("Lecture vidéo indisponible sur cette version", color = Color.White)
                } else {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Image plein écran",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            rotationZ = rotation
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .85f), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = .2f)).testTag("media_viewer_close_button")
                        ) { Icon(Icons.Default.Close, "Fermer la vue plein écran", tint = Color.White) }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(senderName.ifBlank { if (isVideo) "Vidéo MBoté" else "Photo MBoté" }, color = Color.White, fontWeight = FontWeight.Bold)
                            if (timestamp.isNotBlank()) Text(timestamp, color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                        }
                    }
                    Row {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, mediaUrl)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager le média"))
                        }) { Icon(Icons.Default.Share, "Partager le média", tint = Color.White) }
                        IconButton(
                            onClick = {
                                runCatching {
                                    val request = DownloadManager.Request(Uri.parse(mediaUrl))
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "mbote-${System.currentTimeMillis()}")
                                    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                                }.onSuccess {
                                    Toast.makeText(context, "Téléchargement démarré", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "Téléchargement impossible", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("media_viewer_download_button")
                        ) { Icon(Icons.Default.Download, "Télécharger le média", tint = Color.White) }
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls && !isVideo,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = .7f)) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { scale = (scale + .5f).coerceAtMost(5f) }, modifier = Modifier.testTag("zoom_in_button")) {
                            Icon(Icons.Default.ZoomIn, "Zoom avant", tint = Color.White)
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (scale == 1f) PurplePrimary else Color.White.copy(alpha = .2f),
                            modifier = Modifier.size(36.dp).clickable {
                                scale = 1f; offset = Offset.Zero; rotation = 0f
                            }.testTag("zoom_reset_button")
                        ) { Box(contentAlignment = Alignment.Center) { Text("${(scale * 100).toInt()}%", color = Color.White, fontSize = 9.sp) } }
                        IconButton(onClick = { scale = (scale - .5f).coerceAtLeast(1f); if (scale == 1f) offset = Offset.Zero }, modifier = Modifier.testTag("zoom_out_button")) {
                            Icon(Icons.Default.ZoomOut, "Zoom arrière", tint = Color.White)
                        }
                        IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                            Icon(Icons.Default.RotateRight, "Pivoter de 90°", tint = Color.White)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .9f))))
                        .navigationBarsPadding()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (caption.isNotBlank()) Text(caption, color = Color.White, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("❤️", "🔥", "👍", "😂", "😮").forEach { emoji ->
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = .2f),
                                    modifier = Modifier.size(38.dp).clickable { onReaction(emoji) }
                                ) { Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 18.sp) } }
                            }
                        }
                        if (!isVideo) Text("Double-tap pour zoomer", color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
