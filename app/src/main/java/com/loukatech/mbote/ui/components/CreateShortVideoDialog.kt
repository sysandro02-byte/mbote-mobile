package com.loukatech.mbote.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShortVideoDialog(
    onDismiss: () -> Unit,
    onPublish: (
        videoUri: android.net.Uri,
        durationSeconds: Int,
        caption: String,
        hashtags: List<String>,
        musicTitle: String,
        musicArtist: String,
        thumbnailUrl: String,
        location: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current active wizard step: 1 = Découper, 2 = Détails, 3 = Aperçu & publier
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: Découper state
    var startTimeSec by remember { mutableFloatStateOf(0f) }
    var endTimeSec by remember { mutableFloatStateOf(120f) }
    var videoSourceText by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isMuted by remember { mutableStateOf(false) }

    // Step 2: Details state
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var musicSearchQuery by remember { mutableStateOf("") }
    var selectedMusicId by remember { mutableStateOf("no_music") }
    var musicVolume by remember { mutableFloatStateOf(0.6f) }

    // Step 3: Aperçu & publier state
    var taggedContactQuery by remember { mutableStateOf("") }
    var privacySetting by remember { mutableStateOf("Public - visible par tous") }
    var allowComments by remember { mutableStateOf(true) }
    var draftCount by remember { mutableIntStateOf(0) }
    var isPublishing by remember { mutableStateOf(false) }
    var publishError by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideoUri = uri
        videoSourceText = uri?.lastPathSegment.orEmpty()
        publishError = null
    }

    val presetMusicList = remember {
        listOf(
            MusicOption("no_music", "Son original de la vidéo", "Aucune musique ajoutée", "00:00", isAudioOnly = true)
        )
    }

    val selectedMusicObj = presetMusicList.find { it.id == selectedMusicId } ?: presetMusicList.first()

    val emojisList = listOf("😍", "🔥", "✨", "🌍", "💚", "✈️")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .testTag("create_short_video_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF6F4FE),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Step Header Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step 1 Badge
                            StepHeaderItem(
                                stepNumber = 1,
                                label = "Découper",
                                isActive = currentStep == 1,
                                onClick = { currentStep = 1 }
                            )

                            // Step 2 Badge
                            StepHeaderItem(
                                stepNumber = 2,
                                label = "Détails",
                                isActive = currentStep == 2,
                                onClick = { if (currentStep > 1) currentStep = 2 }
                            )

                            // Step 3 Badge
                            StepHeaderItem(
                                stepNumber = 3,
                                label = "Aperçu &\npublier",
                                isActive = currentStep == 3,
                                onClick = { /* stay */ }
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1EAFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = Color(0xFF6B21A8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Main Content Step Container Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE9D8FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                Step1TrimScreen(
                                    startTimeSec = startTimeSec,
                                    endTimeSec = endTimeSec,
                                    isMuted = isMuted,
                                    videoSourceText = videoSourceText,
                                    onStartTimeChange = { startTimeSec = it },
                                    onEndTimeChange = { endTimeSec = it },
                                    onToggleMute = { isMuted = !isMuted },
                                    onResetTrim = {
                                        startTimeSec = 0f
                                        endTimeSec = 120f
                                    },
                                    onSelectFileClick = {
                                        videoPicker.launch("video/*")
                                    }
                                )
                            }
                            2 -> {
                                Step2DetailsScreen(
                                    title = title,
                                    onTitleChange = { title = it },
                                    subtitle = subtitle,
                                    onSubtitleChange = { subtitle = it },
                                    emojis = emojisList,
                                    onAddEmoji = { emoji ->
                                        if (title.length < 90) title += " $emoji"
                                    },
                                    musicQuery = musicSearchQuery,
                                    onMusicQueryChange = { musicSearchQuery = it },
                                    musicList = presetMusicList.filter {
                                        musicSearchQuery.isBlank() || it.title.contains(musicSearchQuery, ignoreCase = true) || it.artist.contains(musicSearchQuery, ignoreCase = true)
                                    },
                                    selectedMusicId = selectedMusicId,
                                    onSelectMusic = { selectedMusicId = it },
                                    volume = musicVolume,
                                    onVolumeChange = { musicVolume = it },
                                    onGenerateAiDetails = {
                                        publishError = "La génération automatique n’est pas disponible sans API IA configurée."
                                    }
                                )
                            }
                            3 -> {
                                Step3PreviewScreen(
                                    title = title,
                                    subtitle = subtitle,
                                    musicTitle = selectedMusicObj.title,
                                    taggedContactQuery = taggedContactQuery,
                                    onTaggedContactQueryChange = { taggedContactQuery = it },
                                    privacySetting = privacySetting,
                                    onPrivacySettingChange = { privacySetting = it },
                                    allowComments = allowComments,
                                    onAllowCommentsChange = { allowComments = it },
                                    draftCount = draftCount,
                                    onSaveDraft = {
                                        draftCount++
                                    },
                                    onBackToStep2 = { currentStep = 2 }
                                )
                            }
                        }
                    }
                }

                // Bottom Tip Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Conseil : prévisualisez votre vidéo avant de publier pour un meilleur rendu.",
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom Main Action Button
                val nextButtonText = when (currentStep) {
                    1 -> "Suivant : Détails →"
                    2 -> "Suivant : Aperçu & publier →"
                    else -> "Publier ma ShortVideo 🚀"
                }

                Button(
                    onClick = {
                        if (selectedVideoUri == null) {
                            publishError = "Sélectionnez une vraie vidéo avant de continuer."
                        } else if (currentStep < 3) {
                            currentStep++
                        } else {
                            isPublishing = true
                            onPublish(
                                selectedVideoUri!!,
                                (endTimeSec - startTimeSec).toInt().coerceIn(1, 120),
                                title.trim(),
                                Regex("#[\\p{L}\\p{N}_-]+").findAll("$title $subtitle").map { it.value }.toList(),
                                selectedMusicObj.title,
                                selectedMusicObj.artist,
                                "",
                                ""
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("step_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFF7C3AED),
                                        Color(0xFF6D28D9)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = nextButtonText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                publishError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun StepHeaderItem(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color(0xFF7C3AED) else Color.White
                )
                .border(
                    width = if (isActive) 0.dp else 1.5.dp,
                    color = Color(0xFFC4B5FD),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = if (isActive) Color.White else Color(0xFF6D28D9),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isActive) Color(0xFF6D28D9) else Color(0xFF6B7280),
            lineHeight = 14.sp
        )
    }
}

/* ================= STEP 1: DÉCOUPER ================= */
@Composable
private fun Step1TrimScreen(
    startTimeSec: Float,
    endTimeSec: Float,
    isMuted: Boolean,
    videoSourceText: String,
    onStartTimeChange: (Float) -> Unit,
    onEndTimeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onResetTrim: () -> Unit,
    onSelectFileClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "1. Découpez votre vidéo",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1B4B)
        )
        Text(
            text = "Sélectionnez la partie que vous souhaitez publier.",
            fontSize = 13.sp,
            color = Color(0xFF6B7280)
        )

        // Dark Vertical Video Player Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            // Background Image Simulation
            AsyncImage(
                model = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop&q=80",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.45f)
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "00:00 / 01:12",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Plein écran",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Center Play Icon & Helper
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Aperçu vidéo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Aperçu vidéo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Choisissez une vidéo verticale pour commencer.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Extrait Sélectionné Card Section
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFAF5FF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXTRAIT SÉLECTIONNÉ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED),
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = "À ajuster",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "00:00",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B4B)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "IA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSelectFileClick,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Caméra", fontSize = 12.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Time Pickers Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D8FD)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "DÉBUT", fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                            Text(text = "00:00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D8FD)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "FIN", fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                            Text(text = "02:00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                        }
                    }
                }

                // Trimmer Frames Visualizer Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E1B4B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF818CF8)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF4338CA)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFC4B5FD)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF312E81)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFA78BFA)))
                        }

                        // Trimmer Handles Overlay
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(3.dp, Color(0xFF8B5CF6), RoundedCornerShape(14.dp)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF8B5CF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.width(2.dp).height(20.dp).background(Color.White))
                            }

                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF8B5CF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.width(2.dp).height(20.dp).background(Color.White))
                            }
                        }
                    }
                }

                // Trimmer Sliders Controls
                Column {
                    Text(text = "Début de l'extrait", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                    Slider(
                        value = startTimeSec,
                        onValueChange = onStartTimeChange,
                        valueRange = 0f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8B5CF6),
                            activeTrackColor = Color(0xFF8B5CF6)
                        )
                    )

                    Text(text = "Fin de l'extrait", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                    Slider(
                        value = endTimeSec,
                        onValueChange = onEndTimeChange,
                        valueRange = 60f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8B5CF6),
                            activeTrackColor = Color(0xFF8B5CF6)
                        )
                    )
                }

                Text(
                    text = "Durée complète acceptée 00:00 - 02:00",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onSelectFileClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Caméra", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Supprimer", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onResetTrim,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Réinitialiser", fontSize = 12.sp)
            }
        }

        // Upload / Pick File Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFAF8FF),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD8B4FE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Sélectionner ...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B4B)
                        )
                        Text(
                            text = "MP4, WebM ou caméra · jusqu'à 50 Mo",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSelectFileClick,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED))
                ) {
                    Text(text = "CHOISIR", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Astuce Box
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF3E8FF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Astuce : faites glisser les poignées pour ajuster le début et la fin de votre ShortVideo.",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B21A8),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/* ================= STEP 2: DÉTAILS ================= */
@Composable
private fun Step2DetailsScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    subtitle: String,
    onSubtitleChange: (String) -> Unit,
    emojis: List<String>,
    onAddEmoji: (String) -> Unit,
    musicQuery: String,
    onMusicQueryChange: (String) -> Unit,
    musicList: List<MusicOption>,
    selectedMusicId: String,
    onSelectMusic: (String) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onGenerateAiDetails: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "2. Ajoutez les détails",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = "Donnez plus d'impact à votre ShortVideo.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Button(
                onClick = onGenerateAiDetails,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("IA détails", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Field Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Titre *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                leadingIcon = {
                    Text(text = "🌄", fontSize = 18.sp)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFFE9D8FD)
                )
            )
            Text(
                text = "${title.length}/100",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.align(Alignment.End)
            )
        }

        // Field Subtitle
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Sous-titre", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))

            OutlinedTextField(
                value = subtitle,
                onValueChange = onSubtitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFFE9D8FD)
                )
            )
            Text(
                text = "${subtitle.length}/150",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.align(Alignment.End)
            )
        }

        // Add Emojis
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Ajouter des emojis", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emojis) { emoji ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D8FD)),
                        modifier = Modifier.clickable { onAddEmoji(emoji) }
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D8FD)),
                        modifier = Modifier.clickable { }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Music Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Musique", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))

            OutlinedTextField(
                value = musicQuery,
                onValueChange = onMusicQueryChange,
                placeholder = { Text("Rechercher une musique...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B7280)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFFE9D8FD)
                )
            )

            // Music Options List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                musicList.forEach { music ->
                    val isSelected = music.id == selectedMusicId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFFAF5FF) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMusic(music.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFFF3E8FF) else Color(0xFFF3F4F6)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (music.isAudioOnly) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF7C3AED) else Color(0xFF6B7280),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = music.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E1B4B)
                                    )
                                    Text(
                                        text = "${music.artist} · ${music.duration}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Volume Control Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFAF5FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D8FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                            Text(text = "Volume de la musique", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF3E8FF)
                        ) {
                            Text(
                                text = "${(volume * 100).toInt()}%",
                                color = Color(0xFF7C3AED),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8B5CF6),
                            activeTrackColor = Color(0xFF8B5CF6)
                        )
                    )

                    Text(
                        text = "Le volume modifie immédiatement l'aperçu audio.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

/* ================= STEP 3: APERÇU & PUBLIER ================= */
@Composable
private fun Step3PreviewScreen(
    title: String,
    subtitle: String,
    musicTitle: String,
    taggedContactQuery: String,
    onTaggedContactQueryChange: (String) -> Unit,
    privacySetting: String,
    onPrivacySettingChange: (String) -> Unit,
    allowComments: Boolean,
    onAllowCommentsChange: (Boolean) -> Unit,
    draftCount: Int,
    onSaveDraft: () -> Unit,
    onBackToStep2: () -> Unit
) {
    var showPrivacyMenu by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3. Aperçu avant publication",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = "Vérifiez le rendu final de votre ShortVideo.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }

            OutlinedButton(
                onClick = onBackToStep2,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED))
            ) {
                Text("Retour", fontSize = 12.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
            }
        }

        // Video Mockup Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop&q=80",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("ShortVideo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White)
                }

                // Bottom Content Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Text("Loukatech 💜", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Text(text = musicTitle, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Identifier Contacts
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Identifier des contacts", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
            Text("Mentionnez des personnes qui pourraient apprécier votre ShortVideo.", fontSize = 11.sp, color = Color(0xFF6B7280))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = taggedContactQuery,
                    onValueChange = onTaggedContactQueryChange,
                    placeholder = { Text("Rechercher un contact", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B7280)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color(0xFFE9D8FD)
                    )
                )

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6))
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Privacy Settings
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Paramètres de confidentialité", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))

            Box {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D8FD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyMenu = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = privacySetting, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E1B4B))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF6B7280))
                    }
                }

                DropdownMenu(
                    expanded = showPrivacyMenu,
                    onDismissRequest = { showPrivacyMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Public - visible par tous") },
                        onClick = {
                            onPrivacySettingChange("Public - visible par tous")
                            showPrivacyMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Amis uniquement") },
                        onClick = {
                            onPrivacySettingChange("Amis uniquement")
                            showPrivacyMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Privé - moi uniquement") },
                        onClick = {
                            onPrivacySettingChange("Privé - moi uniquement")
                            showPrivacyMenu = false
                        }
                    )
                }
            }
        }

        // Allow Comments Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Autoriser les commentaires", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
            Switch(
                checked = allowComments,
                onCheckedChange = onAllowCommentsChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF8B5CF6)
                )
            )
        }

        // Save Draft Button
        OutlinedButton(
            onClick = onSaveDraft,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7C3AED))
        ) {
            Text("Enregistrer comme brouillon", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // Espace Brouillons Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF3E8FF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Espace Brouillons", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = draftCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class MusicOption(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val isAudioOnly: Boolean = false
)
