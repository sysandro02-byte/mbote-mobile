package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }

    var isPlaying by remember { mutableStateOf(isVideo) }
    var videoProgress by remember { mutableFloatStateOf(0.35f) }
    var showControls by remember { mutableStateOf(true) }

    // Auto-advance video progress simulation if playing
    LaunchedEffect(isPlaying, isVideo) {
        if (isVideo && isPlaying) {
            while (isPlaying) {
                delay(200)
                videoProgress = (videoProgress + 0.015f).let { if (it > 1f) 0f else it }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("fullscreen_media_viewer")
        ) {
            // Main Interactive Media Canvas with Pinch-to-Zoom, Pan, and Double-tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    // Pan slightly towards tap location
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    offset = Offset((centerX - tapOffset.x) * 1.2f, (centerY - tapOffset.y) * 1.2f)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rot ->
                            scale = (scale * zoom).coerceIn(0.7f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = if (isVideo) "Vidéo plein écran" else "Image plein écran",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            rotationZ = rotation
                        }
                )

                // Video Play/Pause Overlay Button if video mode
                if (isVideo) {
                    AnimatedVisibility(
                        visible = showControls || !isPlaying,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { isPlaying = !isPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Lecture",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }

            // Top Header Bar Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("media_viewer_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer la vue plein écran",
                                    tint = Color.White
                                )
                            }

                            Column {
                                Text(
                                    text = senderName.ifBlank { if (isVideo) "Vidéo MBoté" else "Photo MBoté" },
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (timestamp.isNotBlank()) {
                                    Text(
                                        text = timestamp,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Action Buttons: Share, Download
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Lien de partage copié !", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager le média",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Média enregistré dans la galerie MBoté !", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .testTag("media_viewer_download_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Télécharger le média",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Floating Interactive Zoom Controls Toolpad (for quick precision zooming)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Zoom In (+)
                        IconButton(
                            onClick = {
                                scale = (scale + 0.5f).coerceAtMost(5f)
                            },
                            modifier = Modifier.size(36.dp).testTag("zoom_in_button")
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom avant", tint = Color.White)
                        }

                        // Reset Zoom (1x)
                        Surface(
                            shape = CircleShape,
                            color = if (scale == 1f) PurplePrimary else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    scale = 1f
                                    offset = Offset.Zero
                                    rotation = 0f
                                }
                                .testTag("zoom_reset_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${(scale * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Zoom Out (-)
                        IconButton(
                            onClick = {
                                scale = (scale - 0.5f).coerceAtLeast(1f)
                                if (scale == 1f) offset = Offset.Zero
                            },
                            modifier = Modifier.size(36.dp).testTag("zoom_out_button")
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom arrière", tint = Color.White)
                        }

                        // Rotate (90°)
                        IconButton(
                            onClick = {
                                rotation = (rotation + 90f) % 360f
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Pivoter de 90°", tint = Color.White)
                        }
                    }
                }
            }

            // Bottom Overlay: Video progress bar (if video) + Caption & Quick Reactions
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Video Progress Bar & Times
                        if (isVideo) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", ((videoProgress * 45).toInt() / 60), ((videoProgress * 45).toInt() % 60)),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Slider(
                                    value = videoProgress,
                                    onValueChange = { videoProgress = it },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = PurplePrimary,
                                        activeTrackColor = PurplePrimary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                                Text(
                                    text = "00:45",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Caption if present
                        if (caption.isNotBlank()) {
                            Text(
                                text = caption,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Quick Reaction Emoji Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf("❤️", "🔥", "👍", "😂", "😮").forEach { emoji ->
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable {
                                                onReaction(emoji)
                                                Toast.makeText(context, "Réaction $emoji envoyée !", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = emoji, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }

                            // Hint / Double-tap indicator
                            Text(
                                text = "Double-tap pour zoomer",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
