package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@Composable
fun LindaPublicProfileDialog(
    onDismiss: () -> Unit,
    onStartChat: () -> Unit = {}
) {
    var isFollowing by remember { mutableStateOf(true) }
    var isBioExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Tout") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("linda_profile_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Cover Photo Banner & App Bar Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    // Cover photo from Unsplash
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&auto=format&fit=crop&q=80",
                        contentDescription = "Cover Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Semi-transparent gradient overlay for readibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )

                    // Top Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Click button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Right Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Recherche",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Plus d'options",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Profile Overlapping Avatar & Primary Info Container
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-30).dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Avatar Row & Details Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Avatar Overlapping with thick white border
                            Box(
                                modifier = Modifier
                                    .offset(y = (-55).dp)
                                    .size(116.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(4.dp)
                            ) {
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1509631179647-0177331693ae?w=300&auto=format&fit=crop&q=80",
                                    contentDescription = "Linda Bongo Ondimba",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Name & Follower statistics Column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .offset(y = (-10).dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Linda Bongo Ondimba",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Verified Badge checkmark (Blue checkmark)
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Vérifié",
                                        tint = Color(0xFF1D4ED8), // Deep blue verified check
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Followers, Suivis Count Row
                                Text(
                                    text = "322 K followers • 59 suivi(e)s",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "• 2 K publications",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                              
                                Spacer(modifier = Modifier.height(8.dp))
                              
                                // Start Chat quick click button
                                OutlinedButton(
                                    onClick = onStartChat,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MbotePurplePrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(12.dp), tint = MbotePurplePrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Discuter sur MBoté", fontSize = 11.sp, color = MbotePurplePrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Shifted down layout for main content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-35).dp)
                        ) {
                            // 3. Biography with collapsible "Voir plus"
                            val bioText = "Je ne réponds à aucun message Inbox, si une Linda Bongo vous écrit et demande de l'argent ou des faveurs, sachez que c'est une arnaque. Restez vigilants s'il vous plaît sur les réseaux sociaux."
                            Text(
                                text = if (isBioExpanded) bioText else "${bioText.take(90)}...",
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = if (isBioExpanded) "Voir moins" else "Voir plus",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                modifier = Modifier
                                    .clickable { isBioExpanded = !isBioExpanded }
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 4. Metadata List (Public Figure, Libreville, Insta, Phone)
                            MetadataRow(icon = Icons.Outlined.FolderOpen, text = "Personnalité publique")
                            MetadataRow(icon = Icons.Outlined.LocationOn, text = "Libreville")
                            MetadataRow(icon = Icons.Outlined.PhotoCamera, text = "Linda_Bongo_Ondimba")
                            MetadataRow(icon = Icons.Outlined.Phone, text = "+241 66682882")

                            Spacer(modifier = Modifier.height(14.dp))

                            // 5. Mutual Followers Stack Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Overlapping mini avatars
                                Box(
                                    modifier = Modifier.width(72.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    listOf(
                                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop&q=80",
                                        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=100&auto=format&fit=crop&q=80",
                                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=100&auto=format&fit=crop&q=80"
                                    ).forEachIndexed { index, avatarUrl ->
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .offset(x = (index * 18).dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Mutual followers text description
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Suivi(e) par Aris Niamba, Dany Ntoma, Marketplacee Kadila Mahouna et 9 autres personnes",
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // 6. Giant Blue Action Button (Suivi(e))
                            Button(
                                onClick = { isFollowing = !isFollowing },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFF1E88E5) else MbotePurplePrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (isFollowing) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isFollowing) "Suivi(e)" else "Suivre",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 7. Tab Segment Pill Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Tout", "Photos", "Reels", "Plus").forEach { tab ->
                                    val isTabSelected = selectedTab == tab
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isTabSelected) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isTabSelected) Color(0xFF1E88E5).copy(alpha = 0.3f) else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .clickable { selectedTab = tab }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (tab == "Plus") "Plus ▼" else tab,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTabSelected) Color(0xFF1E88E5) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 8. Informations personnelles Section
                            Text(
                                text = "Informations personnelles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            InfoDetailItem(icon = Icons.Outlined.LocationOn, label = "Libreville")
                            InfoDetailItem(icon = Icons.Outlined.Home, label = "Libreville")
                            InfoDetailItem(icon = Icons.Outlined.Female, label = "Femme")

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Voir plus de détails",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { }
                                    .padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // 9. Détails Section
                            Text(
                                text = "Détails",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            InfoDetailItem(
                                icon = Icons.Outlined.Star,
                                label = "Recommandé par 92 % (10 avis)",
                                iconTint = Color(0xFFEAB308) // Star gold color
                            )

                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoDetailItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color.Gray
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
