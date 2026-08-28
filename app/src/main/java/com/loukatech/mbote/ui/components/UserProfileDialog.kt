package com.loukatech.mbote.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.model.ProfileDisplayData
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@Composable
fun UserProfileDialog(
    profile: ProfileDisplayData,
    onDismiss: () -> Unit,
    onStartChat: (String) -> Unit,
    onVoiceCall: (String, String) -> Unit,
    onVideoCall: (String, String) -> Unit
) {
    val isCelebrity = profile.isCelebrity
    val isCompany = profile.isCompany

    // Derived username
    val username = remember(profile.name) {
        "@" + profile.name.lowercase()
            .replace(" ", "")
            .replace("é", "e")
            .replace("è", "e")
            .replace("à", "a")
            .replace("ô", "o")
            .replace("û", "u")
    }

    // Category label (Culture, Humour, Tech, etc.)
    val category = remember(profile.name, isCelebrity, isCompany) {
        when {
            isCelebrity -> "Culture"
            isCompany -> "Entreprise"
            profile.name.contains("Aron") || profile.name.contains("Intelligence") -> "Tech"
            profile.name.contains("Journaliste") -> "Médias"
            else -> "Premium"
        }
    }

    // Unique Premium Cover banner representing African sunsets, landscapes, and modern abstracts
    val coverImage = remember(profile.name) {
        val hash = kotlin.math.abs(profile.name.hashCode())
        when (hash % 5) {
            0 -> "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=800&auto=format&fit=crop&q=80" // Savanna sunset (exactly like the image!)
            1 -> "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop&q=80" // Music concert
            2 -> "https://images.unsplash.com/photo-1519741497674-611481863552?w=800&auto=format&fit=crop&q=80" // River sunset
            3 -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80" // Light show
            else -> "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&auto=format&fit=crop&q=80" // Modern tech network
        }
    }

    // Unique statistics to make every profile realistic, active, and unique!
    val abonneesCount = remember(profile.name) {
        val hash = kotlin.math.abs(profile.name.hashCode())
        val base = if (isCelebrity) 12000 else if (isCompany) 42000 else 850
        val offset = hash % 2000
        val total = base + offset
        if (total >= 1000) {
            String.format(java.util.Locale.US, "%.1fk", total / 1000.0)
        } else {
            "$total"
        }
    }

    val videosCount = remember(profile.name) {
        val hash = kotlin.math.abs(profile.name.hashCode())
        val base = if (isCelebrity) 8 else if (isCompany) 24 else 2
        (base + (hash % 6)).coerceAtLeast(1)
    }

    val viewsCount = remember(profile.name) {
        val hash = kotlin.math.abs(profile.name.hashCode())
        val base = if (isCelebrity) 180 else if (isCompany) 420 else 10
        val offset = hash % 50
        "${base + offset}k"
    }

    val likesCount = remember(profile.name) {
        val hash = kotlin.math.abs(profile.name.hashCode())
        val base = if (isCelebrity) 5 else if (isCompany) 15 else 1
        val offset = (hash % 10) / 2.0
        String.format(java.util.Locale.US, "%.1fk", base + offset)
    }

    // Interactive States inside the dialog
    var isFollowing by remember { mutableStateOf(false) }
    var greetingSent by remember { mutableStateOf(false) }

    // Mock thumbnails for publications (matching the exact layout)
    val mockThumbnails = remember(profile.name, videosCount) {
        val hash = kotlin.math.abs(profile.name.hashCode())
        val count = videosCount.coerceAtMost(3)
        List(count) { i ->
            val imgIndex = (hash + i) % 5
            val imgUrl = when (imgIndex) {
                0 -> "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=400&auto=format&fit=crop&q=80"
                1 -> "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=400&auto=format&fit=crop&q=80"
                2 -> "https://images.unsplash.com/photo-1519741497674-611481863552?w=400&auto=format&fit=crop&q=80"
                3 -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop&q=80"
                else -> "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=400&auto=format&fit=crop&q=80"
            }
            val views = ((hash * (i + 1)) % 45 + 1).coerceAtLeast(1)
            Pair(imgUrl, views)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("user_profile_display_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Cover & Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    // Cover Banner Image
                    AsyncImage(
                        model = coverImage,
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("close_profile_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Category Pill top-left
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MbotePurplePrimary.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⭐ Créateur $category",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Profile Info Card (Overlapping Avatar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with glowing ring
                    Box(
                        modifier = Modifier
                            .offset(y = (-45).dp)
                            .size(90.dp)
                    ) {
                        AsyncImage(
                            model = profile.avatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120" },
                            contentDescription = profile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(3.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .border(5.dp, MbotePurplePrimary, CircleShape)
                        )

                        // Online green dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height((-30).dp))

                    // Creator Name & Verified Check
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCelebrity || isCompany) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Profil vérifié",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MbotePurplePrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    // Location & Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${profile.city}, Congo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    // Bio
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Creator Stats Row
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CreatorStatCol(title = "Abonnés", value = abonneesCount)
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            CreatorStatCol(title = "Vidéos", value = "$videosCount")
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            CreatorStatCol(title = "Total Vues", value = viewsCount)
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            CreatorStatCol(title = "J'aime", value = likesCount)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Action Buttons Row (Matching Image layout perfectly)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Suivre Button
                        Button(
                            onClick = {
                                isFollowing = !isFollowing
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else MbotePurplePrimary,
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("profile_follow_button")
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFollowing) "Abonné" else "Suivre",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Message Button (Direct chat)
                        Button(
                            onClick = {
                                onDismiss()
                                onStartChat(profile.name)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                                .testTag("profile_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Message",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Tip / Soutenir Button (With EMERALD look)
                        FilledTonalButton(
                            onClick = {
                                greetingSent = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF10B981).copy(alpha = 0.18f),
                                contentColor = Color(0xFF059669)
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("profile_tip_button")
                        ) {
                            Text(text = "💸 Tip", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Friendly MBoté greeting banner button
                    if (!greetingSent) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MbotePurpleLight.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MbotePurpleLight.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clickable {
                                    greetingSent = true
                                }
                                .testTag("send_mbote_greeting_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "👋", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = "Envoyer un MBoté de bienvenue",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Créer un premier contact chaleureux",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "Envoyer",
                                    color = MbotePurplePrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MBoté envoyé avec succès !",
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Call Quick Actions Section (Extending usability)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onVoiceCall(profile.name, profile.avatar)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Appel Audio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onVideoCall(profile.name, profile.avatar)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Appel Vidéo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Creator's published Shorts Grid Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vidéos du créateur (${mockThumbnails.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ShortMBoté 🎬",
                            style = MaterialTheme.typography.labelMedium,
                            color = MbotePurplePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row of recent videos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mockThumbnails.forEach { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.7f)
                                    .clickable {
                                        onDismiss()
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = item.first,
                                        contentDescription = "Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.75f)
                                                    )
                                                )
                                            )
                                    )
                                    // Views count bottom
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${item.second}k",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CreatorStatCol(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}
