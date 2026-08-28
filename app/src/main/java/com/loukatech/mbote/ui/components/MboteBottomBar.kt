package com.loukatech.mbote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.model.NavigationTab
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@Composable
fun MboteBottomBar(
    currentTab: NavigationTab,
    unreadMessagesCount: Int,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("bottom_nav_bar"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 16.dp,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Actus
            ModernBottomNavItem(
                tab = NavigationTab.ACTUS,
                label = "Actus",
                selectedIcon = Icons.Filled.Newspaper,
                unselectedIcon = Icons.Outlined.Newspaper,
                isSelected = currentTab == NavigationTab.ACTUS,
                badge = null,
                onClick = { onTabSelected(NavigationTab.ACTUS) }
            )

            // 2. Appel
            ModernBottomNavItem(
                tab = NavigationTab.CALLS,
                label = "Appel",
                selectedIcon = Icons.Filled.Call,
                unselectedIcon = Icons.Outlined.Call,
                isSelected = currentTab == NavigationTab.CALLS,
                badge = null,
                onClick = { onTabSelected(NavigationTab.CALLS) }
            )

            // 3. Prominent Glowing Center Tab (Messages)
            ProminentCenterTab(
                isSelected = currentTab == NavigationTab.MESSAGES,
                unreadCount = unreadMessagesCount,
                onClick = { onTabSelected(NavigationTab.MESSAGES) }
            )

            // 4. Shorts (replaces Réunions in bottom bar as requested)
            ModernBottomNavItem(
                tab = NavigationTab.SHORTS,
                label = "Shorts",
                selectedIcon = Icons.Filled.Movie,
                unselectedIcon = Icons.Outlined.Movie,
                isSelected = currentTab == NavigationTab.SHORTS,
                badge = null,
                onClick = { onTabSelected(NavigationTab.SHORTS) }
            )

            // 5. Masta (replaces Paramètres in bottom menu bar)
            ModernBottomNavItem(
                tab = NavigationTab.MASTA,
                label = "Masta",
                selectedIcon = Icons.Filled.Diversity3,
                unselectedIcon = Icons.Outlined.Diversity3,
                isSelected = currentTab == NavigationTab.MASTA || currentTab == NavigationTab.DISCOVER,
                badge = null,
                onClick = { onTabSelected(NavigationTab.MASTA) }
            )
        }
    }
}

@Composable
private fun ModernBottomNavItem(
    tab: NavigationTab,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    badge: Int?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val activeContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    val containerBg by animateColorAsState(
        targetValue = if (isSelected) activeContainer else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_container_bg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        label = "nav_icon_tint"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        label = "nav_text_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_scale"
    )

    Column(
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .scale(scale)
            .testTag("tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = containerBg,
                modifier = Modifier.size(width = 50.dp, height = 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (badge != null && badge > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = if (badge > 99) "99+" else badge.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProminentCenterTab(
    isSelected: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "center_tab_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .scale(scale)
            .offset(y = (-4).dp)
            .testTag("tab_messages_prominent"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Outer Glowing Radial Aura
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = if (isSelected) pulseAlpha else 0.15f),
                            Color(0xFFD946EF).copy(alpha = if (isSelected) (pulseAlpha * 0.4f).coerceIn(0f, 1f) else 0.05f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Elevated Shadowed Outer Container
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (isSelected) 6.dp else 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF3B82F6))
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE))
                        )
                    }
                )
            ) {
                // Vibrant Core Button with Rich Gradient & Inner Shine
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isSelected) {
                                    listOf(
                                        Color(0xFFA855F7), // Light Purple
                                        Color(0xFF7C3AED), // Mbote Core Purple
                                        Color(0xFF5B21B6)  // Deep Indigo Purple
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF9333EA),
                                        Color(0xFF6D28D9),
                                        Color(0xFF4C1D95)
                                    )
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forum,
                        contentDescription = "Messages",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    // Unread Badge
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                    )
                                )
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MbotePurplePrimary)
                )
            }
            Text(
                text = "Message",
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
