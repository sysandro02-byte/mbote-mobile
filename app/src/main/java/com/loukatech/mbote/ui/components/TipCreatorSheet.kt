package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipCreatorSheet(
    shortVideo: ShortVideo,
    walletBalanceFcfa: Long,
    userGiftState: UserGiftState = UserGiftState(),
    onSendTip: (amount: Long, provider: String) -> Unit = { _, _ -> },
    onSendGift: (giftId: String) -> Boolean = { false },
    onBuySingleGift: (GiftItem, Int, String) -> Unit = { _, _, _ -> },
    onBuyBundle: (GiftBundle, String) -> Unit = { _, _ -> },
    onCashout: (amount: Long, provider: String, phone: String) -> Unit = { _, _, _ -> },
    onOpenStore: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(1) } // Default to 1: Cadeaux Virtuels

    val amounts = listOf(500L, 1000L, 2000L, 5000L, 10000L)
    var selectedAmount by remember { mutableStateOf(1000L) }
    var selectedProvider by remember { mutableStateOf("MBoté Pay (Solde)") }
    var isSuccess by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("Transaction effectuée avec succès ! ✨") }

    // Instant buy dialog for out-of-stock gift
    var giftToInstantBuy by remember { mutableStateOf<GiftItem?>(null) }
    var instantBuyProvider by remember { mutableStateOf("MTN Mobile Money") }
    var instantBuyQuantity by remember { mutableIntStateOf(1) }

    // Cashout state
    var cashoutAmountText by remember { mutableStateOf(userGiftState.totalVirtualEarnedFcfa.toString()) }
    var cashoutProvider by remember { mutableStateOf("MTN Mobile Money") }
    var cashoutPhone by remember { mutableStateOf("+242 06 400 00 00") }

    val providers = listOf(
        "MBoté Pay (Solde)",
        "MTN Mobile Money",
        "Airtel Money",
        "Orange Money"
    )

    val giftItems = remember { defaultGiftItems() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("tip_creator_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Soutenir & Cadeaux MBoté 🎁",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: 0: Pourboire, 1: Cadeaux, 2: Encaisser
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MbotePurplePrimary
            ) {
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🎁 Cadeaux Virtuels", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("💰 Pourboire FCFA", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🏧 Encaisser (${userGiftState.totalVirtualEarnedFcfa} F)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSuccess) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = successMessage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        isSuccess = false
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Terminer")
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // Pourboire FCFA classique
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = shortVideo.creatorAvatar,
                                contentDescription = shortVideo.creatorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = shortVideo.creatorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "Envoyer un pourboire direct en FCFA", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                amounts.forEach { amt ->
                                    val isSelected = selectedAmount == amt
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedAmount = amt }
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$amt F",
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            providers.forEach { provider ->
                                val isSelected = selectedProvider == provider
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable { selectedProvider = provider },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = provider, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        RadioButton(selected = isSelected, onClick = { selectedProvider = provider })
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    onSendTip(selectedAmount, selectedProvider)
                                    successMessage = "Pourboire de $selectedAmount FCFA envoyé à ${shortVideo.creatorName} !"
                                    isSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Envoyer le pourboire de $selectedAmount FCFA", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    1 -> {
                        // Virtual Gifts Store & Inventory to Send (Requirement 3)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cadeaux exclusifs pour ${shortVideo.creatorName} :",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenStore()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("🛍️ Boutique", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MbotePurplePrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.height(290.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(giftItems) { gift ->
                                    val count = userGiftState.inventory[gift.id] ?: 0
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (count > 0)
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        ),
                                        border = if (count > 0) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.6f)) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = gift.emoji, fontSize = 34.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = gift.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                text = "${gift.priceFcfa} FCFA",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MbotePurplePrimary
                                            )
                                            
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (count > 0) "Stock : $count en réserve" else "Épuisé (0 en stock)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (count > 0) Color(0xFF10B981) else Color.Red
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (count > 0) {
                                                Button(
                                                    onClick = {
                                                        val sent = onSendGift(gift.id)
                                                        if (sent) {
                                                            Toast.makeText(
                                                                context,
                                                                "🎁 ${gift.emoji} ${gift.name} envoyé à ${shortVideo.creatorName} !",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            successMessage = "🎁 ${gift.emoji} ${gift.name} offert avec succès à ${shortVideo.creatorName} !"
                                                            isSuccess = true
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(32.dp)
                                                        .testTag("send_gift_${gift.id}")
                                                ) {
                                                    Text("Envoyer (1)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        giftToInstantBuy = gift
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(32.dp)
                                                        .testTag("buy_gift_${gift.id}")
                                                ) {
                                                    Text("Acheter", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Cashout received gifts for virtual or real currency (Requirement 4)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🪙 Valeur totale de vos cadeaux reçus",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${userGiftState.totalVirtualEarnedFcfa} FCFA",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFB45309)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Encaissable instantanément en monnaie virtuelle ou Mobile Money réel",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Moyen de retrait :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            listOf("MTN Mobile Money", "Airtel Money", "MBoté Pay (Solde Virtuel)", "Virement Bancaire").forEach { method ->
                                val isSelected = cashoutProvider == method
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.5.dp)
                                        .clickable { cashoutProvider = method },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = method, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        RadioButton(selected = isSelected, onClick = { cashoutProvider = method })
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = cashoutPhone,
                                onValueChange = { cashoutPhone = it },
                                label = { Text("Numéro / Coordonnées de réception") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (userGiftState.totalVirtualEarnedFcfa > 0) {
                                        onCashout(userGiftState.totalVirtualEarnedFcfa, cashoutProvider, cashoutPhone)
                                        successMessage = "🏧 Retrait de ${userGiftState.totalVirtualEarnedFcfa} FCFA validé via $cashoutProvider vers $cashoutPhone !"
                                        isSuccess = true
                                    } else {
                                        Toast.makeText(context, "Aucun solde de cadeau à encaisser pour le moment.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_cashout_button")
                            ) {
                                Text("Encaisser ${userGiftState.totalVirtualEarnedFcfa} FCFA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Instant Purchase Dialog when a gift is out-of-stock
    giftToInstantBuy?.let { gift ->
        AlertDialog(
            onDismissRequest = { giftToInstantBuy = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(gift.emoji, fontSize = 24.sp)
                    Text("Acheter : ${gift.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Vous n'avez pas de ${gift.name} en réserve. Achetez-en instantanément :",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prix unitaire :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${gift.priceFcfa} FCFA", fontWeight = FontWeight.Bold, color = MbotePurplePrimary, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quantité :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (instantBuyQuantity > 1) instantBuyQuantity-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("$instantBuyQuantity", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            IconButton(
                                onClick = { instantBuyQuantity++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }

                    val totalCost = gift.priceFcfa * instantBuyQuantity
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total à payer :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$totalCost FCFA", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MbotePurplePrimary)
                        }
                    }

                    Text("Mode de paiement instantané :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    listOf("MTN Mobile Money", "Airtel Money", "MBoté Pay").forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { instantBuyProvider = p }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = instantBuyProvider == p, onClick = { instantBuyProvider = p })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(p, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuySingleGift(gift, instantBuyQuantity, instantBuyProvider)
                        Toast.makeText(context, "✅ Achat de $instantBuyQuantity ${gift.name} réussi !", Toast.LENGTH_SHORT).show()
                        giftToInstantBuy = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Valider l'achat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { giftToInstantBuy = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
