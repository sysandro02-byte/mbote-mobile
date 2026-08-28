package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loukatech.mbote.model.GiftTransaction
import com.loukatech.mbote.model.UserGiftState
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftHistoryDialog(
    userGiftState: UserGiftState,
    onCashout: (amountFcfa: Long, provider: String, phone: String) -> Unit = { _, _, _ -> },
    onOpenStore: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Tous, 1: Reçus, 2: Envoyés
    var showCashoutModal by remember { mutableStateOf(false) }

    // Cashout form states
    var cashoutAmount by remember { mutableStateOf("${userGiftState.totalVirtualEarnedFcfa}") }
    var cashoutProvider by remember { mutableStateOf("MTN Mobile Money") }
    var cashoutPhone by remember { mutableStateOf("+242 06 123 4567") }

    val filteredTransactions = remember(selectedFilter, userGiftState.transactions) {
        when (selectedFilter) {
            1 -> userGiftState.transactions.filter { it.isReceived }
            2 -> userGiftState.transactions.filter { !it.isReceived }
            else -> userGiftState.transactions
        }
    }

    val totalReceivedCount = remember(userGiftState.transactions) {
        userGiftState.transactions.count { it.isReceived }
    }
    val totalSentCount = remember(userGiftState.transactions) {
        userGiftState.transactions.count { !it.isReceived }
    }

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
                            color = Color(0xFFFFD700).copy(alpha = 0.25f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🪙", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Historique des Cadeaux",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gains virtuels & cadeaux échangés",
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
                        Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Summary Card for Virtual Money & Cashout
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFEF3C7), // Warm golden amber
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Solde Total Cumulé (Cadeaux Reçus)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${userGiftState.totalVirtualEarnedFcfa} FCFA",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "Reçu en direct via les lives & pourboires MBoté",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showCashoutModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("cashout_button")
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Encaisser (MoMo)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenStore()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF92400E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Text("🎁 Boutique", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats overview row (Reçus vs Envoyés)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📥", fontSize = 20.sp)
                            Column {
                                Text(text = "$totalReceivedCount reçus", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Lives & publications", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📤", fontSize = 20.sp)
                            Column {
                                Text(text = "$totalSentCount envoyés", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Aux créateurs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter tabs
                TabRow(
                    selectedTabIndex = selectedFilter,
                    containerColor = Color.Transparent,
                    contentColor = MbotePurplePrimary
                ) {
                    Tab(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        text = { Text("Tous (${userGiftState.transactions.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        text = { Text("📥 Reçus ($totalReceivedCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        text = { Text("📤 Envoyés ($totalSentCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transactions List
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎁", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucune transaction trouvée dans cette catégorie.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTransactions) { tx ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (tx.isReceived) Color(0xFF10B981).copy(alpha = 0.15f) else MbotePurplePrimary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(tx.emoji, fontSize = 22.sp)
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.giftName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (tx.isReceived) "De : ${tx.counterpartName}" else "À : ${tx.counterpartName}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = tx.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when {
                                                    tx.status.contains("Disponible") -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    tx.status.contains("Encaissé") -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                }
                                            ) {
                                                Text(
                                                    text = tx.status,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = when {
                                                        tx.status.contains("Disponible") -> Color(0xFF10B981)
                                                        tx.status.contains("Encaissé") -> Color(0xFF3B82F6)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (tx.isReceived) "+${tx.amountFcfa} F" else "-${tx.amountFcfa} F",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (tx.isReceived) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
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

    // Cashout Dialog Sheet
    if (showCashoutModal) {
        AlertDialog(
            onDismissRequest = { showCashoutModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏧 Encaisser mes gains")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Transférez vos gains de cadeaux (${userGiftState.totalVirtualEarnedFcfa} FCFA) vers votre compte Mobile Money ou portefeuille MBoté.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = cashoutAmount,
                        onValueChange = { cashoutAmount = it },
                        label = { Text("Montant à encaisser (FCFA)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cashoutPhone,
                        onValueChange = { cashoutPhone = it },
                        label = { Text("Numéro Mobile Money") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Opérateur de retrait :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    listOf("MTN Mobile Money", "Airtel Money", "MBoté Pay (Portefeuille)").forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cashoutProvider = provider }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(provider, fontSize = 13.sp)
                            RadioButton(selected = cashoutProvider == provider, onClick = { cashoutProvider = provider })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cashoutAmount.toLongOrNull() ?: userGiftState.totalVirtualEarnedFcfa
                        if (amount > 0 && amount <= userGiftState.totalVirtualEarnedFcfa) {
                            onCashout(amount, cashoutProvider, cashoutPhone)
                            Toast.makeText(context, "✅ Demande d'encaissement de $amount FCFA envoyée vers $cashoutPhone ($cashoutProvider) !", Toast.LENGTH_LONG).show()
                            showCashoutModal = false
                        } else {
                            Toast.makeText(context, "Montant invalide ou solde insuffisant.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Confirmer le retrait", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashoutModal = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
