package com.loukatech.mbote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    recipientName: String,
    onDismiss: () -> Unit,
    onSendPayment: (amount: String, provider: String, note: String, isRequest: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("5000") }
    var selectedProvider by remember { mutableStateOf("MTN MoMo") }
    var note by remember { mutableStateOf("Participation") }
    var isRequest by remember { mutableStateOf(false) }

    val providers = listOf(
        "MTN MoMo" to Color(0xFFFFCC00),
        "Airtel Money" to Color(0xFFE60000),
        "Orange Money" to Color(0xFFFF7900),
        "M-Pesa" to Color(0xFF00AA4F)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MbotePurpleLight) },
        modifier = modifier.testTag("payment_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MbotePurplePrimary, Color(0xFF10B981)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MBoté Pay • Mobile Money",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paiements sécurisés et chiffrés",
                            style = MaterialTheme.typography.bodySmall,
                            color = MbotePurplePrimary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Toggle: Send vs Request
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (!isRequest) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRequest = false }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (!isRequest) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Envoyer des fonds",
                            fontWeight = if (!isRequest) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isRequest) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isRequest) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRequest = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isRequest) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Demander un paiement",
                            fontWeight = if (isRequest) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRequest) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() } },
                label = { Text("Montant en FCFA") },
                placeholder = { Text("Ex: 5000") },
                suffix = { Text("FCFA", fontWeight = FontWeight.Bold, color = MbotePurplePrimary) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_amount_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Amount Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("1000", "2500", "5000", "10000", "25000").forEach { quickAmount ->
                    SuggestionChip(
                        onClick = { amount = quickAmount },
                        label = { Text("$quickAmount F", fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MbotePurpleSoft.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Provider Selection
            Text(
                text = "Opérateur Mobile Money :",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                providers.forEach { (provider, badgeColor) ->
                    val isSelected = selectedProvider == provider
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedProvider = provider }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = provider.replace(" Money", ""),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Motif / Note pour $recipientName") },
                placeholder = { Text("Ex: Remboursement repas") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val formatted = "$amount FCFA"
                    onSendPayment(formatted, selectedProvider, note, isRequest)
                },
                enabled = amount.isNotBlank() && (amount.toLongOrNull() ?: 0) > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_payment_button")
            ) {
                Text(
                    if (isRequest) "Demander $amount FCFA via $selectedProvider"
                    else "Transférer $amount FCFA via $selectedProvider"
                )
            }
        }
    }
}
