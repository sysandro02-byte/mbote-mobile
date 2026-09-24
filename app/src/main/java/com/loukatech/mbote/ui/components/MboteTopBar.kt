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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.NavigationTab
import com.loukatech.mbote.model.UserProfile

/**
 * Android port of the canonical web MBoté TopBar:
 * brand → search → notifications → jobs → ShortMBoté → profile.
 * It stays responsive on narrow phones while keeping every action functional.
 */
@Composable
fun MboteTopBar(
    currentTab: NavigationTab,
    userProfile: UserProfile,
    onSearchClick: () -> Unit,
    onJobsClick: () -> Unit,
    onShortVideosClick: () -> Unit = {},
    unreadNotificationsCount: Int = 0,
    onNotificationsClick: () -> Unit = onSearchClick,
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("top_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
        ) {
            val compact = maxWidth < 430.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onHomeClick)
                        .testTag("app_logo_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 38.dp else 42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF9333EA), Color(0xFF6B21A8)),
                                    start = Offset.Zero,
                                    end = Offset(120f, 120f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MboteSpeechBubbleIcon(modifier = Modifier.size(if (compact) 23.dp else 25.dp))
                    }
                    if (!compact) {
                        Column {
                            Text(
                                text = "MBoté",
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.testTag("brand_header_title")
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .width(44.dp)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C3AED))
                            )
                        }
                    }
                }

                if (!compact) {
                    Surface(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("global_search_button"),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Rechercher des amis, vidéos, événements…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Outlined.Mic, contentDescription = "Recherche vocale", modifier = Modifier.size(19.dp))
                        }
                    }
                } else {
                    HeaderActionButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Rechercher",
                        onClick = onSearchClick,
                        testTag = "global_search_button",
                        compact = true
                    )
                }

                HeaderNotificationButton(
                    unreadCount = unreadNotificationsCount,
                    onClick = onNotificationsClick,
                    compact = compact
                )
                HeaderActionButton(
                    icon = Icons.Outlined.WorkOutline,
                    contentDescription = "Emplois",
                    onClick = onJobsClick,
                    testTag = "jobs_button",
                    compact = compact
                )
                HeaderActionButton(
                    icon = Icons.Outlined.Movie,
                    contentDescription = "ShortMBoté",
                    onClick = onShortVideosClick,
                    testTag = "shorts_top_button",
                    compact = compact
                )
                ProfileHeaderButton(
                    userProfile = userProfile,
                    onClick = onProfileClick,
                    compact = compact
                )
            }
        }
    }
}

@Composable
private fun HeaderNotificationButton(
    unreadCount: Int,
    onClick: () -> Unit,
    compact: Boolean
) {
    Box {
        HeaderActionButton(
            icon = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            onClick = onClick,
            testTag = "notifications_button",
            compact = compact
        )
        if (unreadCount > 0) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-2).dp),
                color = Color(0xFFEF4444),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    compact: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(if (compact) 34.dp else 38.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(if (compact) 10.dp else 12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
        shadowElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(if (compact) 18.dp else 21.dp)
            )
        }
    }
}

@Composable
private fun ProfileHeaderButton(
    userProfile: UserProfile,
    onClick: () -> Unit,
    compact: Boolean
) {
    Box(
        modifier = Modifier
            .size(if (compact) 35.dp else 39.dp)
            .clickable(onClick = onClick)
            .testTag("profile_avatar_button"),
        contentAlignment = Alignment.Center
    ) {
        if (userProfile.avatar.isNotBlank()) {
            AsyncImage(
                model = userProfile.avatar,
                contentDescription = "Profil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF7C3AED)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(userProfile.name),
                    color = Color.White,
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Box(
            modifier = Modifier
                .size(if (compact) 9.dp else 11.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(Color(0xFF22C55E))
                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

@Composable
fun MboteSpeechBubbleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bubblePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = w * 0.10f,
                    top = h * 0.12f,
                    right = w * 0.90f,
                    bottom = h * 0.78f,
                    cornerRadius = CornerRadius(w * 0.32f, h * 0.32f)
                )
            )
            moveTo(w * 0.22f, h * 0.72f)
            quadraticTo(w * 0.12f, h * 0.92f, w * 0.14f, h * 0.96f)
            quadraticTo(w * 0.30f, h * 0.88f, w * 0.42f, h * 0.76f)
            close()
        }
        drawPath(bubblePath, color = Color.White)
        val radius = w * 0.055f
        val y = h * 0.45f
        val purple = Color(0xFF7C3AED)
        drawCircle(purple, radius, Offset(w * 0.34f, y))
        drawCircle(purple, radius, Offset(w * 0.50f, y))
        drawCircle(purple, radius, Offset(w * 0.66f, y))
    }
}

private fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter(String::isNotBlank)
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
        parts.size == 1 -> "${parts[0].first().uppercaseChar()}O"
        else -> "MB"
    }
}
