package com.loukatech.mbote.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.LinkedChildInfo
import com.loukatech.mbote.model.UserProfile
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@Composable
fun ParentalControlPremiumBadge(
    isPremium: Boolean,
    isParentalActive: Boolean,
    isChildLinked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val goldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFFD97706))
    )
    val shieldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5), Color(0xFF2563EB))
    )
    val activeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399))
    )

    val currentGradient = when {
        isParentalActive && isChildLinked -> activeGradient
        isPremium -> goldGradient
        else -> shieldGradient
    }

    if (compact) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = modifier
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                .background(currentGradient, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .testTag("parental_premium_badge_compact")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Bouclier Parental",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isParentalActive && isChildLinked) "Bouclier Actif 🛡️" else if (isPremium) "Parent Pro 👑" else "Contrôle Parental",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                brush = currentGradient
            ),
            shadowElevation = 3.dp,
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("parental_premium_badge_full")
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(currentGradient, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isChildLinked) Icons.Default.Shield else Icons.Outlined.Security,
                            contentDescription = "Badge Contrôle Parental",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MBoté Bouclier Parental",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPremium) Color(0xFFFFD700).copy(alpha = 0.2f) else MbotePurplePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isPremium) "👑 PREMIUM" else "STANDARD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isPremium) Color(0xFFB45309) else MbotePurplePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = when {
                                !isPremium -> "Outil Premium • Débloquer la surveillance & le quota 2h"
                                !isChildLinked -> "Compte enfant non lié • Scanner le QR code pour activer"
                                isParentalActive -> "Protection active • Quota 2h, Verrouillage 00h-06h & SOS"
                                else -> "Prêt à être activé • Compte enfant synchronisé"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ouvrir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ParentShieldProfileCard(
    userProfile: UserProfile,
    linkedChild: LinkedChildInfo,
    onOpenSettings: () -> Unit,
    onOpenPremium: () -> Unit,
    onScanQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLinked = userProfile.isChildAccountLinkedByQrScan
    val isActive = userProfile.parentalControlActive

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parent_shield_profile_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive && isLinked) Color(0xFFF0FDF4) else Color(0xFFFAF5FF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isActive && isLinked) Color(0xFF86EFAC) else Color(0xFFD8B4FE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🛡️", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "Bouclier Parental MBoté",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isActive && isLinked) Color(0xFF166534) else Color(0xFF6B21A8)
                        )
                        Text(
                            text = if (isLinked) "Enfant associé : ${linkedChild.name}" else "Aucun enfant associé",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive && isLinked) Color(0xFF22C55E) else MbotePurplePrimary,
                    modifier = Modifier.clickable(onClick = if (isLinked) onOpenSettings else onScanQr)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLinked) Icons.Outlined.VerifiedUser else Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isLinked) "Gérer" else "Lier QR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (isLinked) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = linkedChild.avatar,
                            contentDescription = linkedChild.name,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                text = "${linkedChild.name} (${linkedChild.age} ans)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Appareil: ${linkedChild.deviceModel} • 🔋 ${linkedChild.batteryLevel}%",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                        Text("En ligne", fontSize = 10.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
