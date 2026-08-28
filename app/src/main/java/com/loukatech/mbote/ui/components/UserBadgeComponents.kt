package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.model.BadgeType
import com.loukatech.mbote.model.UserProfile
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@Composable
fun UserBadgeChip(
    badge: BadgeType,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val badgeColor = Color(badge.colorHex)
    val backgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            badgeColor.copy(alpha = 0.25f),
            badgeColor.copy(alpha = 0.12f)
        )
    )

    Surface(
        shape = RoundedCornerShape(if (compact) 6.dp else 12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.7f)),
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 6.dp else 12.dp))
            .background(backgroundBrush)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("badge_chip_${badge.id}")
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)
        ) {
            Text(
                text = badge.emoji,
                fontSize = if (compact) 11.sp else 13.sp
            )
            if (showLabel) {
                Text(
                    text = badge.shortLabel,
                    fontSize = if (compact) 10.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge == BadgeType.VIP) Color(0xFFFFD700) else badgeColor
                )
            }
        }
    }
}

@Composable
fun UserBadgesRow(
    badges: List<BadgeType>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onBadgeClick: ((BadgeType) -> Unit)? = null
) {
    if (badges.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(badges) { badge ->
            UserBadgeChip(
                badge = badge,
                compact = compact,
                onClick = if (onBadgeClick != null) { { onBadgeClick(badge) } } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeStoreDialog(
    userProfile: UserProfile,
    onBuyBadge: (BadgeType, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedBadgeToBuy by remember { mutableStateOf<BadgeType?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("MTN Mobile Money") }
    var isSuccess by remember { mutableStateOf(false) }
    var successBadgeName by remember { mutableStateOf("") }

    val paymentMethods = listOf(
        "MTN Mobile Money",
        "Airtel Money",
        "MBoté Pay (Solde: ${userProfile.walletBalanceFcfa} F)",
        "Carte Bancaire / Visa"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👑", fontSize = 22.sp)
                    Column {
                        Text(
                            text = "Badges & Statuts MBoté",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Distinction officielle sur votre profil & en Live",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        },
        text = {
            if (isSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Félicitations !",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Votre $successBadgeName est maintenant actif et visible par toute la communauté !",
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MbotePurpleSoft.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("💡", fontSize = 20.sp)
                                Text(
                                    text = "Les badges mettent en valeur vos dons, votre créativité et votre réputation au sein du réseau MBoté.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(BadgeType.values()) { badge ->
                        val isOwned = userProfile.badges.contains(badge)
                        val badgeColor = Color(badge.colorHex)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOwned) badgeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                if (isOwned) 1.5.dp else 1.dp,
                                if (isOwned) badgeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
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
                                        Text(badge.emoji, fontSize = 26.sp)
                                        Column {
                                            Text(
                                                text = badge.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${badge.priceFcfa} FCFA (À vie)",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = MbotePurplePrimary
                                            )
                                        }
                                    }

                                    if (isOwned) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("Actif", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { selectedBadgeToBuy = badge },
                                            colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = "Acheter",
                                                color = if (badge == BadgeType.VIP) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = badge.description,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(
                    onClick = {
                        isSuccess = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Terminé")
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(onClick = onDismiss) {
                    Text("Fermer")
                }
            }
        }
    )

    // Purchase confirmation sub-dialog
    selectedBadgeToBuy?.let { badge ->
        AlertDialog(
            onDismissRequest = { selectedBadgeToBuy = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(badge.emoji, fontSize = 24.sp)
                    Text("Obtenir ${badge.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Montant à régler : ${badge.priceFcfa} FCFA",
                        fontWeight = FontWeight.Bold,
                        color = MbotePurplePrimary,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Sélectionnez votre moyen de paiement :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    paymentMethods.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(method, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                RadioButton(selected = isSelected, onClick = { selectedPaymentMethod = method })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuyBadge(badge, selectedPaymentMethod)
                        successBadgeName = badge.title
                        isSuccess = true
                        selectedBadgeToBuy = null
                        Toast.makeText(context, "✅ ${badge.title} activé avec succès !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Confirmer & Payer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBadgeToBuy = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
