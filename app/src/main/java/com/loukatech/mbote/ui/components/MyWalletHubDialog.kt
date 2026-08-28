package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWalletHubDialog(
    userProfile: UserProfile,
    userGiftState: UserGiftState,
    onCashout: (amount: Long, provider: String, phone: String) -> Unit,
    onTopUpWallet: (amount: Long, provider: String) -> Unit,
    onOpenBadgeStore: () -> Unit = {},
    onOpenGiftStore: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Tous, 1: Cadeaux Reçus, 2: Cadeaux Envoyés, 3: Retraits effectués
    var showCashoutModal by remember { mutableStateOf(false) }
    var showTopUpModal by remember { mutableStateOf(false) }

    val totalWithdrawn = remember(userGiftState.withdrawals) {
        userGiftState.withdrawals.sumOf { it.amountFcfa }
    }

    val pendingWithdrawalsCount = remember(userGiftState.withdrawals) {
        userGiftState.withdrawals.count { it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.PROCESSING }
    }

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
                    Text("💳", fontSize = 24.sp)
                    Column {
                        Text(
                            text = "Mon Portefeuille MBoté",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gestion des soldes, cadeaux & retraits Mobile Money",
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Balance Summary Card (Dual Balances: MBoté Pay + Virtual Gifts)
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF6D28D9).copy(alpha = 0.85f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.85f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Solde MBoté Pay",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${userProfile.walletBalanceFcfa} FCFA",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Disponible",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Gains Cadeaux Virtuels",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${userGiftState.totalVirtualEarnedFcfa} FCFA",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }

                                Button(
                                    onClick = { showCashoutModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp).testTag("cashout_button")
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Encaisser", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 2. Fast Actions Row (Recharger, Badges VIP, Boutique Packs)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTopUpModal = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recharger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onOpenBadgeStore()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("👑 Badges VIP", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                onOpenGiftStore()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("🎁 Boutique", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Pending Payments Notification banner if any
                if (pendingWithdrawalsCount > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = "$pendingWithdrawalsCount paiement(s) en cours de validation",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "Vérification opérateur Mobile Money (Délai moyen: 15-30 min)",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Tab Navigation Bar
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = MbotePurplePrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Tous", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Reçus 🎁", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Envoyés 📤", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Retraits 🏧", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // 5. Tab Content: Withdrawals or Gift Transactions
                if (selectedTab == 3) {
                    // Withdrawals List
                    if (userGiftState.withdrawals.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucun retrait effectué pour le moment", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(userGiftState.withdrawals) { withdrawal ->
                            WithdrawalItemCard(withdrawal = withdrawal)
                        }
                    }
                } else {
                    // Filtered Gift Transactions List
                    val filteredList = when (selectedTab) {
                        1 -> userGiftState.transactions.filter { it.isReceived }
                        2 -> userGiftState.transactions.filter { !it.isReceived }
                        else -> userGiftState.transactions
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucune transaction enregistrée", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(filteredList) { tx ->
                            GiftTxCard(tx = tx)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
            ) {
                Text("Fermer")
            }
        }
    )

    // Submodal: Cashout Modal
    if (showCashoutModal) {
        var cashoutAmountText by remember { mutableStateOf("${userGiftState.totalVirtualEarnedFcfa}") }
        var cashoutProvider by remember { mutableStateOf("MTN Mobile Money") }
        var cashoutPhone by remember { mutableStateOf("+242 06 123 4567") }

        AlertDialog(
            onDismissRequest = { showCashoutModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏧", fontSize = 22.sp)
                    Text("Encaisser vos Gains Cadeaux", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Solde disponible : ${userGiftState.totalVirtualEarnedFcfa} FCFA",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = cashoutAmountText,
                        onValueChange = { cashoutAmountText = it },
                        label = { Text("Montant à retirer (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cashoutPhone,
                        onValueChange = { cashoutPhone = it },
                        label = { Text("Numéro Mobile Money / Compte") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Opérateur de retrait :", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    listOf("MTN Mobile Money", "Airtel Money", "MBoté Pay (Instantané)").forEach { provider ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cashoutProvider = provider },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cashoutProvider == provider) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(provider, fontSize = 12.sp)
                                RadioButton(selected = cashoutProvider == provider, onClick = { cashoutProvider = provider })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cashoutAmountText.toLongOrNull() ?: 0L
                        if (amount > 0 && amount <= userGiftState.totalVirtualEarnedFcfa) {
                            onCashout(amount, cashoutProvider, cashoutPhone)
                            Toast.makeText(context, "✅ Retrait de $amount FCFA demandé via $cashoutProvider !", Toast.LENGTH_SHORT).show()
                            showCashoutModal = false
                        } else {
                            Toast.makeText(context, "Montant invalide", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Valider le Retrait")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashoutModal = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Submodal: Top-Up Wallet Modal
    if (showTopUpModal) {
        var topUpAmountText by remember { mutableStateOf("10000") }
        var topUpProvider by remember { mutableStateOf("MTN Mobile Money") }

        AlertDialog(
            onDismissRequest = { showTopUpModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳", fontSize = 22.sp)
                    Text("Recharger mon Portefeuille", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = topUpAmountText,
                        onValueChange = { topUpAmountText = it },
                        label = { Text("Montant à recharger (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payer avec :", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    listOf("MTN Mobile Money", "Airtel Money", "Carte Bancaire Visa").forEach { provider ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { topUpProvider = provider },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (topUpProvider == provider) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(provider, fontSize = 12.sp)
                                RadioButton(selected = topUpProvider == provider, onClick = { topUpProvider = provider })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = topUpAmountText.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            onTopUpWallet(amount, topUpProvider)
                            Toast.makeText(context, "✅ Portefeuille rechargé de $amount FCFA via $topUpProvider !", Toast.LENGTH_SHORT).show()
                            showTopUpModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Recharger Maintenant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTopUpModal = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun WithdrawalItemCard(withdrawal: WithdrawalTransaction) {
    val statusColor = Color(withdrawal.status.colorHex)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Retrait ${withdrawal.provider}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "- ${withdrawal.amountFcfa} FCFA",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp,
                    color = Color(0xFFEF4444)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vers : ${withdrawal.destinationAccount} • ${withdrawal.referenceCode}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = withdrawal.timestamp,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = withdrawal.status.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun GiftTxCard(tx: GiftTransaction) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (tx.isReceived) Color(0xFF10B981).copy(alpha = 0.15f) else MbotePurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(tx.emoji, fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (tx.isReceived) "Reçu de ${tx.counterpartName}" else "Offert à ${tx.counterpartName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
                Text(
                    text = "${tx.giftName} • ${tx.timestamp}",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (tx.isReceived) "+ ${tx.amountFcfa} F" else "- ${tx.amountFcfa} F",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (tx.isReceived) Color(0xFF10B981) else MbotePurplePrimary
                )
                Text(
                    text = tx.status,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
