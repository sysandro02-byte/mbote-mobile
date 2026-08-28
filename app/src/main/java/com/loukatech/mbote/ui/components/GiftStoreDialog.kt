package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftStoreDialog(
    userGiftState: UserGiftState,
    onBuyBundle: (GiftBundle, String) -> Unit,
    onBuySingleGift: (GiftItem, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Packs & Lots, 1: À l'unité
    var selectedProvider by remember { mutableStateOf("MTN Mobile Money") }
    var phoneNumber by remember { mutableStateOf("+242 06 123 4567") }
    var isSuccess by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val paymentProviders = listOf(
        "MTN Mobile Money" to "📱",
        "Airtel Money" to "📲",
        "Orange Money" to "🟠",
        "MBoté Pay (Portefeuille)" to "💜",
        "Carte Bancaire (Visa/Mastercard)" to "💳"
    )

    val bundles = remember { defaultGiftBundles() }
    val singleGifts = remember { defaultGiftItems() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🎁", fontSize = 22.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Boutique de Cadeaux MBoté",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Achetez des lots et diamants pour les lives",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Inventory Summary Strip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MbotePurpleSoft.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        singleGifts.forEach { item ->
                            val count = userGiftState.inventory[item.id] ?: 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(item.emoji, fontSize = 20.sp)
                                Text(
                                    text = "$count",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (count > 0) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Packs vs À l'Unité
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MbotePurplePrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📦 Lots & Packs Promo", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("✨ Cadeaux à l'Unité", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSuccess) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Achat Réussi !",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = successMessage,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { isSuccess = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("Continuer les achats", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Content based on tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedTab == 0) {
                            // BUNDLES LIST
                            items(bundles) { bundle ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    border = if (bundle.isPopular) androidx.compose.foundation.BorderStroke(1.5.dp, MbotePurplePrimary) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = bundle.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                if (bundle.badge.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (bundle.isPopular) MbotePurplePrimary else Color(0xFFE11D48)
                                                    ) {
                                                        Text(
                                                            text = bundle.badge,
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                if (bundle.originalPriceFcfa > 0) {
                                                    Text(
                                                        text = "${bundle.originalPriceFcfa} F",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textDecoration = TextDecoration.LineThrough,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${bundle.priceFcfa} FCFA",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MbotePurplePrimary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = bundle.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        // Bundle Content items
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            bundle.contents.forEach { content ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surface
                                                ) {
                                                    Text(
                                                        text = content,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                onBuyBundle(bundle, selectedProvider)
                                                successMessage = "Félicitations ! Le lot \"${bundle.title}\" a été crédité sur votre compte MBoté."
                                                isSuccess = true
                                                Toast.makeText(context, "Achat de ${bundle.title} confirmé !", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(42.dp)
                                                .testTag("buy_bundle_${bundle.id}")
                                        ) {
                                            Text("Acheter via $selectedProvider", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            // SINGLE GIFTS LIST
                            items(singleGifts) { gift ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(text = gift.emoji, fontSize = 36.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = gift.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(text = "${gift.priceFcfa} FCFA l'unité", fontSize = 12.sp, color = MbotePurplePrimary, fontWeight = FontWeight.SemiBold)
                                            Text(text = "Possédés: ${userGiftState.inventory[gift.id] ?: 0}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Buy 1
                                            Button(
                                                onClick = {
                                                    onBuySingleGift(gift, 1, selectedProvider)
                                                    successMessage = "1x ${gift.name} (${gift.emoji}) ajouté à votre inventaire !"
                                                    isSuccess = true
                                                    Toast.makeText(context, "Achat de 1x ${gift.name} réussi", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("+1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            // Buy 5
                                            OutlinedButton(
                                                onClick = {
                                                    onBuySingleGift(gift, 5, selectedProvider)
                                                    successMessage = "5x ${gift.name} (${gift.emoji}) ajoutés à votre inventaire !"
                                                    isSuccess = true
                                                    Toast.makeText(context, "Achat de 5x ${gift.name} réussi", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("+5", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MbotePurplePrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Payment Provider Selection Section at bottom
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Moyen de paiement sélectionné :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            paymentProviders.forEach { (provider, icon) ->
                                val isSelected = selectedProvider == provider
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MbotePurplePrimary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable { selectedProvider = provider }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(icon, fontSize = 18.sp)
                                            Text(provider, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedProvider = provider }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
