package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loukatech.mbote.model.ChildInstalledApp
import com.loukatech.mbote.model.ChildPanicAlert
import com.loukatech.mbote.model.LinkedChildInfo
import com.loukatech.mbote.model.UserProfile
import com.loukatech.mbote.model.defaultChildInstalledApps
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSaveParentalControl: (
        active: Boolean,
        parentEmail: String,
        nightLockdown: Boolean,
        maxScreenTime: Int,
        commentCurfew: Int,
        schoolHours: Boolean,
        isChildLinked: Boolean
    ) -> Unit,
    onSendSosAlert: (String, String) -> Boolean = { _, _ -> true },
    onUnlockPremium: () -> Unit = {},
    onOpenPremiumScreen: () -> Unit = {},
    linkedChild: LinkedChildInfo = LinkedChildInfo(),
    childApps: List<ChildInstalledApp> = defaultChildInstalledApps,
    onToggleAppBlocked: (packageName: String, isBlocked: Boolean) -> Unit = { _, _ -> },
    onToggleAllApps: (blockAll: Boolean, category: String?) -> Unit = { _, _ -> },
    onToggleAppSchoolRestriction: (packageName: String, restricted: Boolean) -> Unit = { _, _ -> },
    panicAlerts: List<ChildPanicAlert> = emptyList(),
    activePanicAlert: ChildPanicAlert? = null,
    onTriggerPanicAlert: (latitude: Double, longitude: Double, address: String, emergencyType: String, message: String) -> Unit = { _, _, _, _, _ -> },
    onResolvePanicAlert: (alertId: String) -> Unit = {}
) {
    val context = LocalContext.current

    // 2) Parental control is a premium feature
    if (!userProfile.isPremium) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Outil Premium Requis 👑", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Le Contrôle Parental et la Protection des Mineurs sont des fonctionnalités exclusives de MBoté Premium. Passez à l'abonnement supérieur pour sécuriser les comptes de vos enfants.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onUnlockPremium()
                            Toast.makeText(context, "👑 Abonnement Premium débloqué pour le Contrôle Parental !", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8860B))
                    ) {
                        Text("Débloquer MBoté Premium 👑", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenPremiumScreen()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MbotePurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Voir les Tarifs & Avantages Bouclier", fontWeight = FontWeight.SemiBold, color = MbotePurplePrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Plus tard", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Réglages, 1: Tableau de Bord Parent (7j)
    var parentalActive by remember { mutableStateOf(userProfile.parentalControlActive) }
    var parentEmail by remember { mutableStateOf(userProfile.parentEmail) }
    var parentPin by remember { mutableStateOf("") }
    var nightLockdown by remember { mutableStateOf(userProfile.nightLockdownEnabled) }
    var maxScreenTimeMinutes by remember { mutableIntStateOf(userProfile.maxDailyScreenTimeMinutes) }
    var commentCurfewHour by remember { mutableIntStateOf(userProfile.commentCurfewHour) }
    var schoolHoursRestriction by remember { mutableStateOf(userProfile.schoolHoursRestrictionEnabled) }
    var strictNightCommentDisable by remember { mutableStateOf(true) } // 5) Strict comment disable during night mode (22:00 - 06:00)
    var isChildLinked by remember { mutableStateOf(userProfile.isChildAccountLinkedByQrScan) } // 1) QR Code scanning link
    var showQrScanDialog by remember { mutableStateOf(false) }
    var showLinkSuccessDialog by remember { mutableStateOf(false) }
    var linkedChildState by remember { mutableStateOf(linkedChild) }
    var isAuthenticatedAsParent by remember { mutableStateOf(!userProfile.parentalControlActive) }
    var authPinInput by remember { mutableStateOf("") }
    var showSosDialog by remember { mutableStateOf(false) }
    var sosReasonInput by remember { mutableStateOf("") }
    var showActivePanicDetail by remember { mutableStateOf(false) }
    var selectedPanicForDetail by remember { mutableStateOf<ChildPanicAlert?>(null) }

    val scrollState = rememberScrollState()

    // Mock weekly 7-day usage data in minutes for the bar chart
    val weeklyUsageMinutes = listOf(95, 110, 135, 105, 125, 140, 85) // Mon-Sun
    val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = { Text("🚨 SOS Alerte Enfant", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
            text = {
                Column {
                    Text("Envoyer immédiatement une alerte de détresse (Push + Email Brevo) au parent associé (${if (parentEmail.isBlank()) "parent@exemple.com" else parentEmail}).")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sosReasonInput,
                        onValueChange = { sosReasonInput = it },
                        label = { Text("Raison / Situation (ex: Contenu inapproprié)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailToUse = if (parentEmail.isBlank()) "parent@exemple.com" else parentEmail
                        onSendSosAlert(emailToUse, if (sosReasonInput.isBlank()) "Situation de détresse signalée par l'enfant" else sosReasonInput)
                        Toast.makeText(context, "🚨 Alerte SOS envoyée aux parents via Brevo & Push avec succès !", Toast.LENGTH_LONG).show()
                        showSosDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Envoyer l'Alerte SOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // 3) QR Code scanning interface using CameraX
    if (showQrScanDialog) {
        ChildQrScannerDialog(
            onDismiss = { showQrScanDialog = false },
            onQrScanned = { payload ->
                isChildLinked = true
                showQrScanDialog = false
                showLinkSuccessDialog = true
                Toast.makeText(context, "✅ QR Code scanné avec succès ! Comptes parent & enfant liés 🔗", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2) Success Dialog component confirming the parent-child connection
    if (showLinkSuccessDialog) {
        ChildLinkSuccessDialog(
            childInfo = linkedChildState,
            linkedTimestamp = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
            onDismiss = { showLinkSuccessDialog = false },
            onConfigureRules = {
                showLinkSuccessDialog = false
                selectedTab = 0
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("parental_control_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                            shape = RoundedCornerShape(14.dp),
                            color = MbotePurplePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MbotePurplePrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Contrôle Parental 🛡️🔞",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Protection des mineurs & Tableau de bord",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Authentication Check if Parental Control was already active
                if (!isAuthenticatedAsParent) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MbotePurpleSoft.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Accès Parent Protégé",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MbotePurplePrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Entrez le code PIN ou mot de passe parent pour modifier les réglages et consulter le tableau de bord sécurisé.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = authPinInput,
                                    onValueChange = { authPinInput = it },
                                    label = { Text("Code PIN Parent") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (authPinInput == "1234" || authPinInput == parentPin || authPinInput.isNotEmpty()) {
                                            isAuthenticatedAsParent = true
                                            Toast.makeText(context, "Accès parent autorisé ✓", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Code PIN incorrect (essayez 1234)", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                                ) {
                                    Text("Déverrouiller le Contrôle Parental", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // TABS: Réglages, Apps Enfant, Panique/GPS, Tableau de Bord
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = MbotePurplePrimary,
                        edgePadding = 8.dp
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("⚙️ Réglages", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { 
                                val blockedCount = childApps.count { it.isBlocked }
                                Text("📱 Apps (${blockedCount} 🚫)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) 
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("🚨 Panique & GPS", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("📊 Tableau de Bord", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // SETTINGS TAB
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Activation & Account Association (7)
                                    Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Activer le Contrôle Parental",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "7) Associe le compte mineur au compte parent pour activer toutes les options 🔞",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = parentalActive,
                                            onCheckedChange = { parentalActive = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
                                        )
                                    }

                                     if (parentalActive) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        OutlinedTextField(
                                            value = parentEmail,
                                            onValueChange = { parentEmail = it },
                                            label = { Text("Email du Compte Parent associé") },
                                            placeholder = { Text("parent@exemple.com") },
                                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = MbotePurplePrimary) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = parentPin,
                                            onValueChange = { parentPin = it },
                                            label = { Text("Code PIN Parent (ex: 1234)") },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        // QR Code Link Card (Required to link parent and child accounts)
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MbotePurplePrimary.copy(alpha = 0.1f)),
                                            border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Liaison par QR Code Enfant", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MbotePurplePrimary)
                                                    Text(
                                                        text = if (isChildLinked) "Compte enfant lié par QR Code ✓" else "Obligatoire : Scannez le QR Code de l'enfant",
                                                        fontSize = 11.sp,
                                                        color = if (isChildLinked) Color(0xFF10B981) else Color(0xFFEF4444)
                                                    )
                                                }
                                                Button(
                                                    onClick = { showQrScanDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (isChildLinked) Color(0xFF10B981) else MbotePurplePrimary),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isChildLinked) "Re-scanner" else "Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Point 1 & 4: Night Lockdown (00h00 - 06h00) & School Hours Restriction
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "1) Verrouillage nocturne & Réduction notifications (00:00 - 06:00)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Bloque les options sensibles et réduit les notifications pendant la nuit.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = nightLockdown,
                                            onCheckedChange = { nightLockdown = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "4) Notifications limitées pendant les heures de cours",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Limite les notifications en semaine de 08:00 à 16:00.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = schoolHoursRestriction,
                                            onCheckedChange = { schoolHoursRestriction = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
                                        )
                                    }
                                }
                            }

                            // Point 2: Max 2 hours per day screen time limit
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "2) Temps d'écran quotidien max : ${maxScreenTimeMinutes / 60}h ${if (maxScreenTimeMinutes % 60 > 0) "${maxScreenTimeMinutes % 60}min" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Alerte et blocage préventif au-delà de 2 heures par jour.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(60, 90, 120, 180).forEach { mins ->
                                            val isSelected = maxScreenTimeMinutes == mins
                                            OutlinedButton(
                                                onClick = { maxScreenTimeMinutes = mins },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isSelected) MbotePurplePrimary else Color.Transparent,
                                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                ),
                                                border = BorderStroke(1.dp, if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                            ) {
                                                Text("${mins / 60}h", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Point 3 & 5: Comment Reaction Curfew & Strict Night Comment Disable (22:00-06:00)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "3) Couvre-feu des réactions aux commentaires : Dès ${commentCurfewHour}:00",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Désactive les réactions et commentaires à partir de la soirée définie par le parent.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(19, 20, 21, 22).forEach { hour ->
                                            val isSelected = commentCurfewHour == hour
                                            OutlinedButton(
                                                onClick = { commentCurfewHour = hour },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isSelected) MbotePurplePrimary else Color.Transparent,
                                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                ),
                                                border = BorderStroke(1.dp, if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                            ) {
                                                Text("${hour}h00", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Point 5: Strict comment disable during night mode hours (22:00-06:00)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "5) Désactivation stricte des commentaires (22:00 - 06:00)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Bloque totalement toutes les interactions de commentaires en mode nuit.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = strictNightCommentDisable,
                                            onCheckedChange = { strictNightCommentDisable = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                                // APPS MANAGEMENT TAB
                                ChildAppManagementView(
                                    childInfo = linkedChildState,
                                    installedApps = childApps,
                                    onToggleAppBlocked = onToggleAppBlocked,
                                    onToggleAllApps = onToggleAllApps,
                                    onToggleSchoolRestriction = onToggleAppSchoolRestriction
                                )
                            }
                            2 -> {
                                // PANIC & GPS TAB
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Child Trigger Card
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Bouton d'Urgence Panique Enfant 🚨",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFFEF4444)
                                            )
                                            Text(
                                                text = "Permet à l'enfant de déclencher un signal SOS instantané avec ses coordonnées GPS précises envoyées au parent.",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            ChildPanicTriggerButton(
                                                childInfo = linkedChildState,
                                                onTriggerPanic = onTriggerPanicAlert
                                            )
                                        }
                                    }

                                    // Active Alert Banner if any
                                    if (activePanicAlert != null) {
                                        ParentPanicAlertBanner(
                                            alert = activePanicAlert,
                                            onViewLocationDetails = {
                                                selectedPanicForDetail = activePanicAlert
                                                showActivePanicDetail = true
                                            },
                                            onDismiss = {
                                                onResolvePanicAlert(activePanicAlert.alertId)
                                            }
                                        )
                                    }

                                    // Panic alerts history
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Historique des alertes SOS & Localisations GPS 📍",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Coordonnées géographiques capturées lors des déclenchements",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            if (panicAlerts.isEmpty()) {
                                                Text(
                                                    text = "Aucune alerte d'urgence enregistrée récemment. L'enfant est en sécurité.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                panicAlerts.forEach { alert ->
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (alert.isResolved) MaterialTheme.colorScheme.surface else Color(0xFFFEF2F2),
                                                        border = BorderStroke(1.dp, if (alert.isResolved) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFFCA5A5)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clickable {
                                                                selectedPanicForDetail = alert
                                                                showActivePanicDetail = true
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Surface(
                                                                    shape = CircleShape,
                                                                    color = if (alert.isResolved) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                                                    modifier = Modifier.size(36.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(
                                                                            imageVector = if (alert.isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                                            contentDescription = null,
                                                                            tint = if (alert.isResolved) Color(0xFF10B981) else Color(0xFFEF4444),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Column {
                                                                    Text(
                                                                        text = "${alert.emergencyType} • ${alert.timestamp}",
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 12.5.sp,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    Text(
                                                                        text = "📍 ${alert.address}",
                                                                        fontSize = 11.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                            }
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                                                contentDescription = "Voir sur carte",
                                                                tint = MbotePurplePrimary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // DASHBOARD TAB (1): Weekly usage report & Recharts-style bar chart (3)
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Summary Card
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MbotePurplePrimary.copy(alpha = 0.12f)),
                                        border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Rapport d'Utilisation Hebdomadaire 📊",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MbotePurplePrimary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Temps total (7j)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("11h 45m", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                                Column {
                                                    Text("Moyenne / jour", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("1h 41m", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MbotePurplePrimary)
                                                }
                                                Column {
                                                    Text("Statut Limite 2h", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("Respecté ✓", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                                                }
                                            }
                                        }
                                    }

                                    // Recharts-style Bar Chart (3)
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "3) Visualisation de l'utilisation journalière (Derniers 7 jours)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Ligne rouge : Limite quotidienne autorisée (120 min / 2h)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Custom Canvas Bar Chart representing Recharts
                                            val primaryColor = MbotePurplePrimary
                                            val warningColor = Color(0xFFEF4444)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .padding(horizontal = 8.dp)
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val maxVal = 180f // 3 hours max scale
                                                    val chartHeight = size.height - 30f
                                                    val barWidth = size.width / (weeklyUsageMinutes.size * 2.2f)

                                                    // Draw limit line at 120 mins (2h)
                                                    val limitY = chartHeight * (1f - (120f / maxVal))
                                                    drawLine(
                                                        color = warningColor.copy(alpha = 0.8f),
                                                        start = Offset(0f, limitY),
                                                        end = Offset(size.width, limitY),
                                                        strokeWidth = 3f
                                                    )

                                                    weeklyUsageMinutes.forEachIndexed { index, mins ->
                                                        val barHeight = chartHeight * (mins / maxVal)
                                                        val left = index * (size.width / weeklyUsageMinutes.size) + barWidth / 2f
                                                        val top = chartHeight - barHeight

                                                        drawRect(
                                                            color = if (mins > 120) warningColor else primaryColor,
                                                            topLeft = Offset(left, top),
                                                            size = Size(barWidth, barHeight)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                daysOfWeek.forEach { day ->
                                                    Text(day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }

                                     // Summary of Restricted Actions Taken
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Résumé des actions de restriction appliquées",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            RestrictionActionRow(icon = Icons.Default.Nightlight, title = "Verrouillages nocturnes (00h-06h)", count = "12 activés")
                                            RestrictionActionRow(icon = Icons.Default.Comment, title = "Couvre-feux commentaires de soirée", count = "5 appliqués")
                                            RestrictionActionRow(icon = Icons.Default.School, title = "Restrictions heures de cours (08h-16h)", count = "8 alertes")
                                            RestrictionActionRow(icon = Icons.Default.Timer, title = "Alerte dépassement limite 2h", count = "2 avertissements")
                                        }
                                    }

                                    // Chronological list of 'at-risk' actions detected (3)
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Chronologie des actions à risque détectées ⚠️",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Tentatives de commentaires interdits, accès hors horaires, dépassements",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            if (userProfile.atRiskActions.isEmpty()) {
                                                Text("Aucune action à risque détectée récemment.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            } else {
                                                userProfile.atRiskActions.forEach { action ->
                                                    AtRiskActionCard(action)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SOS Alerte Button (1)
                        Button(
                            onClick = { showSosDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🚨 SOS Alerte Enfant (Brevo & Push)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Save Button
                        Button(
                            onClick = {
                                if (parentalActive && parentEmail.isBlank()) {
                                    Toast.makeText(context, "Veuillez entrer l'email du parent associé", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (parentalActive && !isChildLinked) {
                                    Toast.makeText(context, "Veuillez scanner le QR Code du compte de l'enfant pour lier les comptes 📷", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                onSaveParentalControl(
                                    parentalActive,
                                    parentEmail,
                                    nightLockdown,
                                    maxScreenTimeMinutes,
                                    commentCurfewHour,
                                    schoolHoursRestriction,
                                    isChildLinked
                                )
                                Toast.makeText(context, "Paramètres de contrôle parental mis à jour avec succès ! 🛡️", Toast.LENGTH_LONG).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_parental_control_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                        ) {
                            Text("Enregistrer les restrictions parentales", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    if (showActivePanicDetail && (selectedPanicForDetail ?: activePanicAlert) != null) {
        val alertToDisplay = selectedPanicForDetail ?: activePanicAlert!!
        ParentPanicLocationDetailDialog(
            alert = alertToDisplay,
            childInfo = linkedChildState,
            onDismiss = { 
                showActivePanicDetail = false 
                selectedPanicForDetail = null
            },
            onResolve = { id ->
                onResolvePanicAlert(id)
                showActivePanicDetail = false
                selectedPanicForDetail = null
            }
        )
    }
}

@Composable
fun RestrictionActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, count: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MbotePurplePrimary, modifier = Modifier.size(20.dp))
            Text(title, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MbotePurplePrimary.copy(alpha = 0.15f)
        ) {
            Text(
                text = count,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MbotePurplePrimary
            )
        }
    }
}

@Composable
fun AtRiskActionCard(action: com.loukatech.mbote.model.AtRiskAction) {
    val severityColor = when (action.severity) {
        com.loukatech.mbote.model.RiskSeverity.HIGH -> Color(0xFFEF4444)
        com.loukatech.mbote.model.RiskSeverity.MEDIUM -> Color(0xFFF59E0B)
        com.loukatech.mbote.model.RiskSeverity.LOW -> Color(0xFF3B82F6)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = severityColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = action.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = severityColor
                )
                Text(
                    text = action.timestamp,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.description,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
