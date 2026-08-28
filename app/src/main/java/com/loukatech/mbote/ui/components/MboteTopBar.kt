package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.loukatech.mbote.model.NavigationTab
import com.loukatech.mbote.model.UserProfile

/**
 * Top App Bar strictly matching the visual header reference:
 * - Left: Purple gradient squircle with white 3-dot speech bubble
 * - Center: White stadium search pill with search icon and "Rechercher des..."
 * - Right: 4 distinct items: Bell, Briefcase, Video Cam (Réunions), and "LO" avatar with green online badge.
 */
@Composable
fun MboteTopBar(
    currentTab: NavigationTab,
    userProfile: UserProfile,
    onSearchClick: () -> Unit,
    onJobsClick: () -> Unit,
    onMeetingsClick: () -> Unit = {},
    unreadNotificationsCount: Int = 0,
    onNotificationsClick: () -> Unit = onSearchClick,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // 1. App Logo: Purple Gradient Squircle with 3-dot Message Bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF9333EA), // vibrant violet-purple
                                Color(0xFF6B21A8)  // deep violet
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(100f, 100f)
                        )
                    )
                    .clickable { onSearchClick() }
                    .testTag("app_logo_button"),
                contentAlignment = Alignment.Center
            ) {
                MboteSpeechBubbleIcon(modifier = Modifier.size(24.dp))
            }

            // 2. Brand name MBOTE replacing the search bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "MBOTE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF9333EA), // Premium brand purple
                    letterSpacing = 2.sp,
                    modifier = Modifier.testTag("brand_header_title")
                )
            }

            // 3. Right Action Buttons: Bell, Briefcase, Movie Clapper, Avatar (LO with green online dot)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Bell / Notifications Button
                Box {
                    HeaderActionButton(
                        icon = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        onClick = onNotificationsClick,
                        testTag = "notifications_button"
                    )
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }

                // Briefcase / Jobs Button
                HeaderActionButton(
                    icon = Icons.Outlined.WorkOutline,
                    contentDescription = "Emplois",
                    onClick = onJobsClick,
                    testTag = "jobs_button"
                )

                // Videocam / MBoté Réunions Button (Swapped with shorts as requested)
                HeaderActionButton(
                    icon = Icons.Outlined.Videocam,
                    contentDescription = "MBoté Réunions",
                    onClick = onMeetingsClick,
                    testTag = "meetings_top_button"
                )

                // User Avatar with Green Active Dot
                Box(
                    modifier = Modifier
                        .size(37.dp)
                        .clickable { onProfileClick() }
                        .testTag("profile_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.avatar.isNotBlank()) {
                        AsyncImage(
                            model = userProfile.avatar,
                            contentDescription = "Photo de profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        // Purple circle with bold initials as fallback
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF7C3AED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(userProfile.name),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Bright Green Online Status Dot on Bottom Right
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        shadowElevation = 0.5.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Custom-drawn speech bubble icon with 3 distinct dots inside
 * to match the exact visual brand icon in the screenshot.
 */
@Composable
fun MboteSpeechBubbleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw the main rounded speech bubble body
        val bubblePath = Path().apply {
            val mainRect = RoundRect(
                left = w * 0.10f,
                top = h * 0.12f,
                right = w * 0.90f,
                bottom = h * 0.78f,
                cornerRadius = CornerRadius(w * 0.32f, h * 0.32f)
            )
            addRoundRect(mainRect)
            
            // Speech tail at bottom left
            moveTo(w * 0.22f, h * 0.72f)
            quadraticTo(
                w * 0.12f, h * 0.92f,
                w * 0.14f, h * 0.96f
            )
            quadraticTo(
                w * 0.30f, h * 0.88f,
                w * 0.42f, h * 0.76f
            )
            close()
        }
        
        drawPath(bubblePath, color = Color.White)
        
        // Draw the 3 horizontal dots inside the bubble with brand purple color
        val dotRadius = w * 0.055f
        val centerY = h * 0.45f
        val dotColor = Color(0xFF7C3AED)
        
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(w * 0.34f, centerY)
        )
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(w * 0.50f, centerY)
        )
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(w * 0.66f, centerY)
        )
    }
}

private fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "LO"
    val parts = trimmed.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
        parts.size == 1 -> "${parts[0].first().uppercaseChar()}O"
        else -> "LO"
    }
}
