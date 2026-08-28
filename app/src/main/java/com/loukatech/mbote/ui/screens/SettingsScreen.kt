package com.loukatech.mbote.ui.screens

import android.widget.Toast
import android.content.Context
import com.loukatech.mbote.service.MboteSoundPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loukatech.mbote.model.AppThemeMode
import com.loukatech.mbote.model.UserProfile
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.components.EditBioDialog
import com.loukatech.mbote.ui.components.GiftHistoryDialog
import com.loukatech.mbote.ui.components.GiftStoreDialog
import com.loukatech.mbote.ui.components.ShareProfileQrDialog
import com.loukatech.mbote.ui.components.ParentalControlPremiumBadge
import com.loukatech.mbote.ui.components.ParentShieldProfileCard
import com.loukatech.mbote.ui.components.ChildQrScannerDialog
import com.loukatech.mbote.ui.components.ChildLinkSuccessDialog
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

enum class SettingSection {
    ACCOUNT,
    PRIVACY,
    NOTIFICATIONS,
    CHATS,
    AUDIO_VIDEO,
    BACKUP,
    AI_TOOLS,
    FIND_DEVICE,
    HELP_SUPPORT,
    PARENTAL_CONTROL_PREMIUM
}

@Composable
fun SettingsScreen(
    userProfile: UserProfile,
    onEditProfileClick: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    onToggleNotifications: () -> Unit,
    onJobsClick: () -> Unit,
    onSyncContactsClick: () -> Unit = {},
    onAronQuestionsClick: () -> Unit = {},
    onEyeContactClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onShortVideosClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onUpdateBio: (String) -> Unit = {},
    onAccessRequestClick: () -> Unit = {},
    blockedContactIds: Set<String> = emptySet(),
    onUnblockContact: (String) -> Unit = {},
    allChats: List<com.loukatech.mbote.model.Chat> = emptyList(),
    allMastaUsers: List<com.loukatech.mbote.model.MastaUser> = emptyList(),
    userGiftState: UserGiftState = UserGiftState(),
    onCashout: (amount: Long, provider: String, phone: String) -> Unit = { _, _, _ -> },
    onBuyBundle: (GiftBundle, String) -> Unit = { _, _ -> },
    onBuySingleGift: (GiftItem, Int, String) -> Unit = { _, _, _ -> },
    onLanguageChange: (AppLanguage) -> Unit = {},
    onCurrencyChange: (AppCurrency) -> Unit = {},
    onSaveParentalControl: (Boolean, String, Boolean, Int, Int, Boolean, Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
    onSendSosAlert: (String, String) -> Boolean = { _, _ -> true },
    onTogglePremium: (Boolean) -> Unit = {},
    linkedChild: LinkedChildInfo = LinkedChildInfo(),
    onUpgradeParentalPlan: (String, Long) -> Unit = { _, _ -> },
    onProcessScannedQr: (String) -> Unit = {},
    childApps: List<ChildInstalledApp> = emptyList(),
    onToggleAppBlocked: (packageName: String, isBlocked: Boolean) -> Unit = { _, _ -> },
    onToggleAllApps: (blockAll: Boolean, category: String?) -> Unit = { _, _ -> },
    onToggleAppSchoolRestriction: (packageName: String, restricted: Boolean) -> Unit = { _, _ -> },
    panicAlerts: List<ChildPanicAlert> = emptyList(),
    activePanicAlert: ChildPanicAlert? = null,
    onTriggerPanicAlert: (latitude: Double, longitude: Double, address: String, emergencyType: String, message: String) -> Unit = { _, _, _, _, _ -> },
    onResolvePanicAlert: (alertId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQrDialog by remember { mutableStateOf(false) }
    var showShareProfileDialog by remember { mutableStateOf(false) }
    var showEditBioDialog by remember { mutableStateOf(false) }
    var showParentalControlDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var offlineModeEnabled by remember { mutableStateOf(false) }
    var activeSubSetting by remember { mutableStateOf<SettingSection?>(null) }
    var showGiftHistoryDialog by remember { mutableStateOf(false) }
    var showGiftStoreDialog by remember { mutableStateOf(false) }
    var showWalletHubDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showBadgeStoreDialog by remember { mutableStateOf(false) }
    var showChildScannerDialog by remember { mutableStateOf(false) }
    var showChildSuccessDialog by remember { mutableStateOf(false) }

    // Language Selection Dialog
    if (showLanguageDialog) {
        com.loukatech.mbote.ui.components.LanguageSelectionDialog(
            currentLanguage = userProfile.language,
            onSelectLanguage = { newLang ->
                onLanguageChange(newLang)
                Toast.makeText(context, "Langue changée en ${newLang.displayName} (${newLang.nativeName})", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Currency Selection Dialog
    if (showCurrencyDialog) {
        com.loukatech.mbote.ui.components.CurrencySelectionDialog(
            currentCurrency = userProfile.currency,
            onSelectCurrency = { newCurr ->
                onCurrencyChange(newCurr)
                Toast.makeText(context, "Devise définie sur ${newCurr.displayName} (${newCurr.symbol})", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    // Mon Portefeuille Hub Dialog
    if (showWalletHubDialog) {
        com.loukatech.mbote.ui.components.MyWalletHubDialog(
            userProfile = userProfile,
            userGiftState = userGiftState,
            onCashout = onCashout,
            onTopUpWallet = { amount, provider ->
                Toast.makeText(context, "Portefeuille rechargé de $amount FCFA via $provider", Toast.LENGTH_SHORT).show()
            },
            onOpenBadgeStore = { showBadgeStoreDialog = true },
            onOpenGiftStore = { showGiftStoreDialog = true },
            onDismiss = { showWalletHubDialog = false }
        )
    }

    // Creator Analytics Dashboard Dialog (D3/Recharts style Canvas Charts)
    if (showAnalyticsDialog) {
        com.loukatech.mbote.ui.components.CreatorAnalyticsDashboardDialog(
            userProfile = userProfile,
            onTogglePremium = {
                onTogglePremium(true)
                Toast.makeText(context, "👑 MBoté Premium activé !", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAnalyticsDialog = false }
        )
    }

    // Badge Store Dialog (VIP, Top Donateur, Créateur Certifié)
    if (showBadgeStoreDialog) {
        com.loukatech.mbote.ui.components.BadgeStoreDialog(
            userProfile = userProfile,
            onBuyBadge = { badge, provider ->
                // buy badge logic
            },
            onDismiss = { showBadgeStoreDialog = false }
        )
    }

    // 0) Gift History & Store Dialogs
    if (showGiftHistoryDialog) {
        GiftHistoryDialog(
            userGiftState = userGiftState,
            onCashout = onCashout,
            onOpenStore = {
                showGiftHistoryDialog = false
                showGiftStoreDialog = true
            },
            onDismiss = { showGiftHistoryDialog = false }
        )
    }

    if (showGiftStoreDialog) {
        GiftStoreDialog(
            userGiftState = userGiftState,
            onBuyBundle = onBuyBundle,
            onBuySingleGift = onBuySingleGift,
            onDismiss = { showGiftStoreDialog = false }
        )
    }

    // 1) Share Profile QR & Deep-link Dialog
    if (showShareProfileDialog) {
        ShareProfileQrDialog(
            userProfile = userProfile,
            onDismiss = { showShareProfileDialog = false }
        )
    }

    // 2) Edit Bio Dialog (150 chars limit)
    if (showEditBioDialog) {
        EditBioDialog(
            currentBio = userProfile.bio,
            onDismiss = { showEditBioDialog = false },
            onConfirm = { newBio ->
                onUpdateBio(newBio)
                showEditBioDialog = false
                Toast.makeText(context, "Biographie mise à jour !", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Parental Control Dialog
    if (showParentalControlDialog) {
        com.loukatech.mbote.ui.components.ParentalControlDialog(
            userProfile = userProfile,
            onDismiss = { showParentalControlDialog = false },
            onSaveParentalControl = onSaveParentalControl,
            onSendSosAlert = onSendSosAlert,
            onUnlockPremium = { onTogglePremium(true) },
            onOpenPremiumScreen = {
                showParentalControlDialog = false
                activeSubSetting = SettingSection.PARENTAL_CONTROL_PREMIUM
            },
            linkedChild = linkedChild,
            childApps = childApps,
            onToggleAppBlocked = onToggleAppBlocked,
            onToggleAllApps = onToggleAllApps,
            onToggleAppSchoolRestriction = onToggleAppSchoolRestriction,
            panicAlerts = panicAlerts,
            activePanicAlert = activePanicAlert,
            onTriggerPanicAlert = onTriggerPanicAlert,
            onResolvePanicAlert = onResolvePanicAlert
        )
    }

    // Direct QR Scanner Dialog for child account linking
    if (showChildScannerDialog) {
        ChildQrScannerDialog(
            onDismiss = { showChildScannerDialog = false },
            onQrScanned = { payload ->
                showChildScannerDialog = false
                onProcessScannedQr(payload)
                showChildSuccessDialog = true
            }
        )
    }

    // Direct Success Dialog
    if (showChildSuccessDialog) {
        ChildLinkSuccessDialog(
            childInfo = linkedChild,
            linkedTimestamp = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
            onDismiss = { showChildSuccessDialog = false },
            onConfigureRules = {
                showChildSuccessDialog = false
                showParentalControlDialog = true
            }
        )
    }

    // 1) Compose screen for the premium parental control features
    if (activeSubSetting == SettingSection.PARENTAL_CONTROL_PREMIUM) {
        ParentalControlPremiumScreen(
            userProfile = userProfile,
            linkedChild = linkedChild,
            onBack = { activeSubSetting = null },
            onOpenQrScanner = { showChildScannerDialog = true },
            onOpenParentalSettings = {
                activeSubSetting = null
                showParentalControlDialog = true
            },
            onUpgradePlan = { planId, price ->
                onUpgradeParentalPlan(planId, price)
                onTogglePremium(true)
            }
        )
        return
    }

    // Sub-Settings Dialogs
    when (activeSubSetting) {
        SettingSection.ACCOUNT -> AccountSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.PRIVACY -> PrivacySubSettingsDialog(
            blockedContactIds = blockedContactIds,
            onUnblockContact = onUnblockContact,
            allChats = allChats,
            allMastaUsers = allMastaUsers,
            onDismiss = { activeSubSetting = null }
        )
        SettingSection.NOTIFICATIONS -> NotificationsSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.CHATS -> ChatSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.AUDIO_VIDEO -> AudioVideoSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.BACKUP -> BackupSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.AI_TOOLS -> AiToolsSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.FIND_DEVICE -> FindMyDeviceSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.HELP_SUPPORT -> HelpSupportSubSettingsDialog(onDismiss = { activeSubSetting = null })
        SettingSection.PARENTAL_CONTROL_PREMIUM -> {}
        null -> {}
    }

    // Dialog for Theme Selection (Light, Dark, System)
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Thème de l'application",
                    tint = MbotePurplePrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Thème de l'application",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choisissez comment MBoté s'affiche sur votre appareil :",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    listOf(
                        Triple(AppThemeMode.LIGHT, Icons.Default.LightMode, "Clair (Lumineux)"),
                        Triple(AppThemeMode.DARK, Icons.Default.DarkMode, "Sombre (Nuit reposante)"),
                        Triple(AppThemeMode.SYSTEM, Icons.Default.SettingsBrightness, "Système (Automatique)")
                    ).forEach { (mode, icon, label) ->
                        val isSelected = userProfile.themeMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MbotePurplePrimary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                    Toast.makeText(
                                        context,
                                        "Thème « ${mode.label} » activé",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("theme_dialog_option_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = mode.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onThemeModeChange(mode)
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = MbotePurplePrimary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Fermer", color = MbotePurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // QR Masta Dialog
    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "QR Masta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MbotePurplePrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, MbotePurpleSoft),
                        modifier = Modifier.size(200.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code",
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(170.dp)
                            )
                        }
                    }

                    Text(
                        text = "Ajouter un ami ou scanner un QR pour démarrer une discussion chiffrée avec ${userProfile.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { showQrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // User Profile Card Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Cover photo banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MbotePurplePrimary, MbotePurpleLight)
                                )
                            )
                    ) {
                        AsyncImage(
                            model = userProfile.coverUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Avatar overlapping
                            Box(
                                modifier = Modifier
                                    .offset(y = (-40).dp)
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(3.dp)
                            ) {
                                AsyncImage(
                                    model = userProfile.avatar,
                                    contentDescription = userProfile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.offset(y = (-10).dp)
                            ) {
                                Button(
                                    onClick = { showShareProfileDialog = true },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("share_profile_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Partager mon profil", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onEditProfileClick,
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("edit_profile_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Modifier", fontSize = 11.5.sp)
                                }

                                // 3-dots Menu on Profile Card
                                Box {
                                    IconButton(
                                        onClick = { showProfileMenu = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options du profil",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showProfileMenu,
                                        onDismissRequest = { showProfileMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Partager mon profil (QR & Lien)") },
                                            leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                                            onClick = {
                                                showProfileMenu = false
                                                showShareProfileDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Modifier ma biographie (150 car.)") },
                                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                                            onClick = {
                                                showProfileMenu = false
                                                showEditBioDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Modifier toutes les infos") },
                                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                            onClick = {
                                                showProfileMenu = false
                                                onEditProfileClick()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Historique des cadeaux & Gains") },
                                            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
                                            onClick = {
                                                showProfileMenu = false
                                                showGiftHistoryDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Boutique de cadeaux virtuels") },
                                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                                            onClick = {
                                                showProfileMenu = false
                                                showGiftStoreDialog = true
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Confidentialité du profil") },
                                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                            onClick = {
                                                showProfileMenu = false
                                                activeSubSetting = SettingSection.PRIVACY
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = userProfile.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            com.loukatech.mbote.ui.components.UserBadgesRow(badges = userProfile.badges, compact = true)
                            ParentalControlPremiumBadge(
                                isPremium = userProfile.isPremium,
                                isParentalActive = userProfile.parentalControlActive,
                                isChildLinked = userProfile.isChildAccountLinkedByQrScan,
                                onClick = { showParentalControlDialog = true },
                                compact = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${userProfile.username} • ${userProfile.city}, ${userProfile.country}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MbotePurplePrimary,
                                fontWeight = FontWeight.Medium
                            )

                            // Créé le : 24/08/2026
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "Créé le 24/08/2026",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interactive Biography with 150 chars limit indicator and pencil
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditBioDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Bio",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MbotePurplePrimary
                                        )
                                        Text(
                                            text = "• ${userProfile.bio.length}/150 caractères",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = userProfile.bio.ifBlank { "Appuyez pour ajouter une biographie..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (userProfile.bio.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { showEditBioDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifier la biographie",
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ProfileStatItem(title = "Discussions", value = "14")
                            ProfileStatItem(title = "Contacts", value = "154")
                            ProfileStatItem(title = "Groupes", value = "6")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Gift & Earnings banner in Profile Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGiftHistoryDialog = true }
                                .testTag("profile_gift_history_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎁", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = "Gains cadeaux : ${formatAppCurrency(userGiftState.totalVirtualEarnedFcfa, userProfile.currency)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Voir l'historique complet & encaisser",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MbotePurplePrimary
                                ) {
                                    Text(
                                        text = "Historique",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4) Premium User Status Visual Indicator (Parental Control Shield)
                        ParentShieldProfileCard(
                            userProfile = userProfile,
                            linkedChild = linkedChild,
                            onOpenSettings = { showParentalControlDialog = true },
                            onOpenPremium = { activeSubSetting = SettingSection.PARENTAL_CONTROL_PREMIUM },
                            onScanQr = { showChildScannerDialog = true }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Channel info card header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ma Chaîne Officielle MBoté 📢",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MbotePurplePrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MbotePurplePrimary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "Créateur certifié",
                                    color = MbotePurplePrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Channel card body with channel banner & avatar
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Channel Banner preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                ) {
                                    AsyncImage(
                                        model = userProfile.channelBanner,
                                        contentDescription = "Channel Banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Overlap channel avatar
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 25.dp, start = 12.dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(2.dp)
                                    ) {
                                        AsyncImage(
                                            model = userProfile.channelAvatar,
                                            contentDescription = "Channel Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(12.dp).padding(top = 4.dp)) {
                                    Text(
                                        text = userProfile.channelName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Abonnés: 1,4K • Catégorie: Tech & Vlog",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Mon Portefeuille, Badges & Monétisation
        item {
            SectionHeader(title = "Mon Portefeuille & Monétisation")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Mon Portefeuille (Solde, Retraits, Gains)
                    SettingsClickRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Mon Portefeuille MBoté",
                        subtitle = "Solde disponible (${formatAppCurrency(userProfile.walletBalanceFcfa, userProfile.currency)}), retraits Mobile Money & suivi",
                        onClick = { showWalletHubDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Tableau de Bord Analytique Créateur
                    SettingsClickRow(
                        icon = Icons.Default.Analytics,
                        title = "Tableau de Bord Analytique Créateur",
                        subtitle = "Évolution des gains par jour/semaine/mois, graphiques & mécènes ${if (userProfile.isPremium) "⭐" else "(Premium)"}",
                        onClick = { showAnalyticsDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Boutique des Badges & Statuts VIP
                    SettingsClickRow(
                        icon = Icons.Default.Verified,
                        title = "Badges & Statuts VIP / Top Donateur",
                        subtitle = "Acquérir des badges de distinction officiels pour le profil & les Lives",
                        onClick = { showBadgeStoreDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Boutique de Cadeaux
                    SettingsClickRow(
                        icon = Icons.Default.Storefront,
                        title = "Boutique de Cadeaux MBoté",
                        subtitle = "Acheter des packs (diamants, bague en or, lingots, couronne)",
                        onClick = { showGiftStoreDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Historique des Cadeaux
                    SettingsClickRow(
                        icon = Icons.Default.CardGiftcard,
                        title = "Historique des Cadeaux",
                        subtitle = "Cadeaux reçus et envoyés, gains cumulés (${userGiftState.totalVirtualEarnedFcfa} FCFA)",
                        onClick = { showGiftHistoryDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Bouclier Parental Pro 👑
                    SettingsClickRow(
                        icon = Icons.Default.Shield,
                        title = "Bouclier Parental Pro 👑",
                        subtitle = "Formules d'abonnement, gestion fratrie & scan QR",
                        badge = if (userProfile.isPremium) "👑 Pro" else "⭐ Premium",
                        badgeColor = Color(0xFFEAB308),
                        onClick = { activeSubSetting = SettingSection.PARENTAL_CONTROL_PREMIUM }
                    )
                }
            }
        }

        // Section: Compte & Sécurité
        item {
            SectionHeader(title = "Compte & Confidentialité")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Compte
                    SettingsClickRow(
                        icon = Icons.Outlined.ManageAccounts,
                        title = "Compte",
                        subtitle = "Notifications de sécurité, changer de numéro",
                        onClick = { activeSubSetting = SettingSection.ACCOUNT }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Contrôle Parental (Protection des mineurs)
                    SettingsClickRow(
                        icon = Icons.Default.Shield,
                        title = "Contrôle Parental 🛡️",
                        subtitle = if (userProfile.parentalControlActive) "Activé (${userProfile.parentEmail}) • Enfant lié ✓" else "Protection des mineurs 🔞 (Liaison QR)",
                        badge = if (userProfile.parentalControlActive && userProfile.isChildAccountLinkedByQrScan) "✓ Protégé" else if (userProfile.isPremium) "👑 Pro" else "⭐ Premium",
                        badgeColor = if (userProfile.parentalControlActive && userProfile.isChildAccountLinkedByQrScan) Color(0xFF10B981) else Color(0xFFEAB308),
                        onClick = { showParentalControlDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Confidentialité
                    SettingsClickRow(
                        icon = Icons.Outlined.Lock,
                        title = "Confidentialité",
                        subtitle = "Comptes bloqués, messages éphémères",
                        onClick = { activeSubSetting = SettingSection.PRIVACY }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Demande d'accès (Permissions)
                    SettingsClickRow(
                        icon = Icons.Outlined.Security,
                        title = "Demande d'accès",
                        subtitle = "Caméra, Microphone, Localisation géographique",
                        onClick = onAccessRequestClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // QR Masta
                    SettingsClickRow(
                        icon = Icons.Outlined.QrCodeScanner,
                        title = "QR Masta",
                        subtitle = "Ajouter un ami ou scanner un QR",
                        onClick = { showQrDialog = true }
                    )
                }
            }
        }

        // Section: Apparence & Notifications
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(title = "Apparence & Connectivité")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Thème & Apparence Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MbotePurpleSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (userProfile.themeMode) {
                                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                                            AppThemeMode.DARK -> Icons.Default.DarkMode
                                            AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                                        },
                                        contentDescription = "Thème",
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Thème de l'application",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${userProfile.themeMode.label} • ${userProfile.themeMode.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Choisir le thème",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3-Way Segmented Theme Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(AppThemeMode.LIGHT, Icons.Default.LightMode, "Clair"),
                                Triple(AppThemeMode.DARK, Icons.Default.DarkMode, "Sombre"),
                                Triple(AppThemeMode.SYSTEM, Icons.Default.SettingsBrightness, "Système")
                            ).forEach { (mode, icon, label) ->
                                val isSelected = userProfile.themeMode == mode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MbotePurplePrimary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onThemeModeChange(mode)
                                            Toast.makeText(
                                                context,
                                                "Thème « $label » activé",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .testTag("theme_button_${mode.name.lowercase()}"),
                                    shadowElevation = if (isSelected) 2.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Langue de l'application
                    SettingsClickRow(
                        icon = Icons.Outlined.Translate,
                        title = "Langue de l'application",
                        subtitle = "${userProfile.language.flag} ${userProfile.language.displayName} (${userProfile.language.nativeName})",
                        onClick = { showLanguageDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Devise monétaire
                    SettingsClickRow(
                        icon = Icons.Outlined.Paid,
                        title = "Devise monétaire",
                        subtitle = "${userProfile.currency.flag} ${userProfile.currency.displayName} • Symbole : ${userProfile.currency.symbol}",
                        onClick = { showCurrencyDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Mode hors ligne
                    SettingsToggleRow(
                        icon = Icons.Outlined.WifiOff,
                        title = "Mode hors ligne",
                        subtitle = "Désactiver les notifications et utiliser le stockage local",
                        isChecked = offlineModeEnabled,
                        onToggle = {
                            offlineModeEnabled = !offlineModeEnabled
                            Toast.makeText(
                                context,
                                if (offlineModeEnabled) "Mode hors ligne activé" else "Mode hors ligne désactivé",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Notifications
                    SettingsClickRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = "Sonneries des messages, groupes et appels",
                        onClick = { activeSubSetting = SettingSection.NOTIFICATIONS }
                    )
                }
            }
        }

        // Section: Discussions, Audio & Vidéo
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(title = "Discussions & Médias")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Discussions
                    SettingsClickRow(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Discussions",
                        subtitle = "Thèmes, fonds d'écran, historique",
                        onClick = { activeSubSetting = SettingSection.CHATS }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Audio et Vidéo
                    SettingsClickRow(
                        icon = Icons.Outlined.Videocam,
                        title = "Audio et Vidéo",
                        subtitle = "Microphone, partage d'écran",
                        onClick = { activeSubSetting = SettingSection.AUDIO_VIDEO }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Stockage et données
                    SettingsClickRow(
                        icon = Icons.Outlined.DataSaverOn,
                        title = "Stockage et données",
                        subtitle = "Utilisation réseau, téléchargement auto.",
                        onClick = onStorageClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Sauvegarde
                    SettingsClickRow(
                        icon = Icons.Outlined.CloudUpload,
                        title = "Sauvegarde",
                        subtitle = "Sauvegarder vos discussions localement",
                        onClick = { activeSubSetting = SettingSection.BACKUP }
                    )
                }
            }
        }

        // Section: Services Premium ⭐
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(title = "Services Premium MBoté ⭐")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Outils IA ⭐ Premium
                    SettingsClickRow(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Outils IA",
                        subtitle = "Réponses intelligentes, résumés, traductions",
                        badge = "⭐ Premium",
                        badgeColor = Color(0xFFEAB308),
                        onClick = { activeSubSetting = SettingSection.AI_TOOLS }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Localiser mon appareil ⭐ Premium
                    SettingsClickRow(
                        icon = Icons.Outlined.GpsFixed,
                        title = "Localiser mon appareil",
                        subtitle = "Position, historique et actions à distance",
                        badge = "⭐ Premium",
                        badgeColor = Color(0xFFEAB308),
                        onClick = { activeSubSetting = SettingSection.FIND_DEVICE }
                    )
                }
            }
        }

        // Section: Aide, Administration & Session
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(title = "Espace Pro & Administration")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Espace Administration & Modération
                    SettingsClickRow(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = "Espace Administration & Modération",
                        subtitle = "Statistiques, santé serveur LoukaTech, logs",
                        badge = "🔐 Admin",
                        badgeColor = Color(0xFF7C3AED),
                        iconColor = Color(0xFF7C3AED),
                        onClick = onAdminClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Changer de compte / Connexion
                    SettingsClickRow(
                        icon = Icons.Outlined.AccountCircle,
                        title = "Changer de compte / Se connecter",
                        subtitle = "Accéder à l'écran de connexion MBoté",
                        iconColor = MbotePurplePrimary,
                        onClick = onLoginClick
                    )
                }
            }
        }

        // Section: Aide & Session
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(title = "Assistance & Session")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Aide
                    SettingsClickRow(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Aide",
                        subtitle = "Pages d'aide, nous contacter",
                        onClick = { activeSubSetting = SettingSection.HELP_SUPPORT }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Déconnexion
                    SettingsClickRow(
                        icon = Icons.Outlined.Logout,
                        title = "Déconnexion",
                        subtitle = "Quitter votre session active",
                        iconColor = Color(0xFFEF4444),
                        onClick = onLogoutClick
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MbotePurplePrimary)
        Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MbotePurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MbotePurplePrimary, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
        )
    }
}

@Composable
fun SettingsClickRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    iconColor: Color = MbotePurplePrimary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (iconColor == Color(0xFFEF4444)) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Sub-Settings Components & Dialogs
@Composable
fun SubSettingsDialogHeader(
    title: String,
    icon: ImageVector,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MbotePurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MbotePurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SubSettingOptionChoice(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                val isSelected = opt == selectedOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(opt) },
                    label = { Text(opt, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MbotePurplePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun AccountSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var securityAlerts by remember { mutableStateOf(true) }
    var twoFactorAuth by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Compte", icon = Icons.Outlined.ManageAccounts, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Shield,
                        title = "Notifications de sécurité",
                        subtitle = "Recevoir une alerte lors d'une connexion sur un nouvel appareil",
                        isChecked = securityAlerts,
                        onToggle = { securityAlerts = !securityAlerts }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.Password,
                        title = "Vérification en deux étapes",
                        subtitle = "Exiger un code PIN à 6 chiffres lors de la réinscription",
                        isChecked = twoFactorAuth,
                        onToggle = { twoFactorAuth = !twoFactorAuth }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.PhonelinkSetup,
                        title = "Changer de numéro de téléphone",
                        subtitle = "Transférer vos discussions et données vers un nouveau numéro",
                        onClick = {
                            Toast.makeText(context, "Saisissez votre nouveau numéro (+242...)", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.Description,
                        title = "Demander les infos de mon compte",
                        subtitle = "Générer un rapport chiffré LoukaTech de votre profil et réglages",
                        onClick = {
                            Toast.makeText(context, "Demande envoyée. Rapport disponible sous 24h.", Toast.LENGTH_LONG).show()
                        }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Supprimer mon compte",
                        subtitle = "Effacer définitivement vos messages, groupes et profil MBoté",
                        iconColor = Color(0xFFEF4444),
                        onClick = {
                            Toast.makeText(context, "Pour supprimer votre compte, vérifiez par SMS", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Paramètres du compte enregistrés", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer les modifications", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrivacySubSettingsDialog(
    blockedContactIds: Set<String>,
    onUnblockContact: (String) -> Unit,
    allChats: List<com.loukatech.mbote.model.Chat>,
    allMastaUsers: List<com.loukatech.mbote.model.MastaUser>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var lastSeen by remember { mutableStateOf("Mes contacts") }
    var profilePhoto by remember { mutableStateOf("Tout le monde") }
    var readReceipts by remember { mutableStateOf(true) }
    var disappearingMsgs by remember { mutableStateOf("24 heures") }
    var fingerprintLock by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }

    // Map blocked contact IDs to display details
    val blockedUsersDisplayList = remember(blockedContactIds, allChats, allMastaUsers) {
        blockedContactIds.map { id ->
            val mastaUser = allMastaUsers.find { it.id == id }
            if (mastaUser != null) {
                com.loukatech.mbote.ui.screens.BlockedUserDisplay(id = id, name = mastaUser.name, avatar = mastaUser.avatar)
            } else {
                val chat = allChats.find { it.id == id }
                if (chat != null) {
                    com.loukatech.mbote.ui.screens.BlockedUserDisplay(id = id, name = chat.name, avatar = chat.avatar)
                } else {
                    val participant = allChats.flatMap { it.participants }.find { it.id == id }
                    if (participant != null) {
                        com.loukatech.mbote.ui.screens.BlockedUserDisplay(id = id, name = participant.name, avatar = participant.avatar)
                    } else {
                        com.loukatech.mbote.ui.screens.BlockedUserDisplay(id = id, name = "Utilisateur #$id", avatar = "")
                    }
                }
            }
        }
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Utilisateurs bloqués",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    if (blockedUsersDisplayList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Aucun utilisateur bloqué",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Vos communications sont ouvertes.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(blockedUsersDisplayList.size) { index ->
                                val blockedUser = blockedUsersDisplayList[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AsyncImage(
                                            model = blockedUser.avatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" },
                                            contentDescription = blockedUser.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                        )
                                        Text(
                                            text = blockedUser.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            onUnblockContact(blockedUser.id)
                                            Toast.makeText(context, "${blockedUser.name} débloqué(e) !", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "Débloquer",
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBlockedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fermer", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            com.loukatech.mbote.ui.components.PrivacySettingsScreen(onBack = onDismiss)
        }
    }
}

@Composable
fun NotificationsSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("mbote_prefs", Context.MODE_PRIVATE) }
    var chatSounds by remember { mutableStateOf(sharedPrefs.getBoolean("chat_sounds", true)) }
    var ringtone by remember { mutableStateOf(sharedPrefs.getString("notif_sound", "MBoté Crystal") ?: "MBoté Crystal") }
    var vibration by remember { mutableStateOf(sharedPrefs.getString("vibration_intensity", "Courte") ?: "Courte") }
    var preview by remember { mutableStateOf(sharedPrefs.getBoolean("message_preview", true)) }
    var groupNotifs by remember { mutableStateOf(sharedPrefs.getBoolean("group_notifs", true)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Notifications", icon = Icons.Outlined.Notifications, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.VolumeUp,
                        title = "Sons de la discussion",
                        subtitle = "Jouer des sons pour les messages entrants et sortants",
                        isChecked = chatSounds,
                        onToggle = { 
                            chatSounds = !chatSounds 
                            sharedPrefs.edit().putBoolean("chat_sounds", chatSounds).apply()
                        }
                    )

                    SubSettingOptionChoice(
                        title = "Sonnerie des messages",
                        options = listOf("MBoté Crystal", "MBoté Echo", "MBoté Sunset", "MBoté Rhythm", "Marimba", "Pop", "Silencieux"),
                        selectedOption = ringtone,
                        onSelect = { 
                            ringtone = it 
                            sharedPrefs.edit().putString("notif_sound", it).apply()
                            if (it != "Silencieux" && chatSounds) {
                                MboteSoundPlayer.playSound(it)
                            }
                        }
                    )

                    SubSettingOptionChoice(
                        title = "Intensité de vibration",
                        options = listOf("Courte", "Longue", "Désactivée"),
                        selectedOption = vibration,
                        onSelect = { 
                            vibration = it 
                            sharedPrefs.edit().putString("vibration_intensity", it).apply()
                        }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.Visibility,
                        title = "Aperçu des messages",
                        subtitle = "Afficher le texte et l'expéditeur dans la bannière",
                        isChecked = preview,
                        onToggle = { 
                            preview = !preview 
                            sharedPrefs.edit().putBoolean("message_preview", preview).apply()
                        }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.Group,
                        title = "Notifications de groupe",
                        subtitle = "Alertes sonores et vibrations pour les canaux et groupes",
                        isChecked = groupNotifs,
                        onToggle = { 
                            groupNotifs = !groupNotifs 
                            sharedPrefs.edit().putBoolean("group_notifs", groupNotifs).apply()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Notifications configurées !", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Appliquer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChatSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var bubbleTheme by remember { mutableStateOf("Violet MBoté") }
    var wallpaper by remember { mutableStateOf("Motif MBoté") }
    var fontSize by remember { mutableStateOf("Moyenne") }
    var enterIsSend by remember { mutableStateOf(true) }
    var saveToGallery by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Discussions & Style", icon = Icons.Outlined.ChatBubbleOutline, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SubSettingOptionChoice(
                        title = "Thème des bulles",
                        options = listOf("Violet MBoté", "Vert Émeraude", "Bleu Océan", "Sombre"),
                        selectedOption = bubbleTheme,
                        onSelect = { bubbleTheme = it }
                    )

                    SubSettingOptionChoice(
                        title = "Fond d'écran des conversations",
                        options = listOf("Motif MBoté", "Couleur unie", "Photo perso"),
                        selectedOption = wallpaper,
                        onSelect = { wallpaper = it }
                    )

                    SubSettingOptionChoice(
                        title = "Taille de police",
                        options = listOf("Petite", "Moyenne", "Grande"),
                        selectedOption = fontSize,
                        onSelect = { fontSize = it }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.KeyboardReturn,
                        title = "Touche Entrée pour envoyer",
                        subtitle = "L'appui sur la touche Entrée envoie le message",
                        isChecked = enterIsSend,
                        onToggle = { enterIsSend = !enterIsSend }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.Image,
                        title = "Enregistrer dans la galerie",
                        subtitle = "Visibilité automatique des photos reçues dans la galerie",
                        isChecked = saveToGallery,
                        onToggle = { saveToGallery = !saveToGallery }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.CleaningServices,
                        title = "Effacer le contenu des chats",
                        subtitle = "Supprimer tous les messages tout en conservant les contacts",
                        onClick = {
                            Toast.makeText(context, "Historique des discussions nettoyé", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Thème et préférences de discussion enregistrés", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AudioVideoSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var quality by remember { mutableStateOf("Haute Définition HD") }
    var noiseCancel by remember { mutableStateOf(true) }
    var opusCodec by remember { mutableStateOf(true) }
    var hdShare by remember { mutableStateOf(true) }
    var echoCancel by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Audio et Vidéo HD", icon = Icons.Outlined.Videocam, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SubSettingOptionChoice(
                        title = "Qualité des appels vocal & vidéo",
                        options = listOf("Haute Définition HD", "Économie de données", "Automatique"),
                        selectedOption = quality,
                        onSelect = { quality = it }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.NoiseControlOff,
                        title = "Suppression de bruit IA",
                        subtitle = "Filtrer les bruits parasites et voix de fond pendant vos appels",
                        isChecked = noiseCancel,
                        onToggle = { noiseCancel = !noiseCancel }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.GraphicEq,
                        title = "Codec Audio HD Opus",
                        subtitle = "Audio haute fidélité optimisé pour la 3G/4G/5G",
                        isChecked = opusCodec,
                        onToggle = { opusCodec = !opusCodec }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.ScreenShare,
                        title = "Partage d'écran HD 1080p",
                        subtitle = "Autoriser la diffusion de votre écran lors des réunions",
                        isChecked = hdShare,
                        onToggle = { hdShare = !hdShare }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.Hearing,
                        title = "Atténuation de l'écho",
                        subtitle = "Éliminer le retour sonore avec le haut-parleur activé",
                        isChecked = echoCancel,
                        onToggle = { echoCancel = !echoCancel }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Paramètres audio et vidéo appliqués", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Valider la configuration", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BackupSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var frequency by remember { mutableStateOf("Quotidienne") }
    var includeVideos by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(true) }
    var isBackingUp by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Sauvegarde des données", icon = Icons.Outlined.CloudUpload, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MbotePurpleSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Dernière sauvegarde", fontWeight = FontWeight.Bold, color = MbotePurplePrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Aujourd'hui à 03:00 • 48.2 Mo", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Emplacement : Stockage local chiffré LoukaTech", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    SubSettingOptionChoice(
                        title = "Fréquence de sauvegarde automatique",
                        options = listOf("Quotidienne", "Hebdomadaire", "Mensuelle", "Désactivée"),
                        selectedOption = frequency,
                        onSelect = { frequency = it }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.VideoFile,
                        title = "Inclure les vidéos",
                        subtitle = "Sauvegarder également les fichiers vidéo reçus (+120 Mo)",
                        isChecked = includeVideos,
                        onToggle = { includeVideos = !includeVideos }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.Wifi,
                        title = "Sauvegarder uniquement en Wi-Fi",
                        subtitle = "Ne pas utiliser le forfait de données cellulaires",
                        isChecked = wifiOnly,
                        onToggle = { wifiOnly = !wifiOnly }
                    )

                    if (isBackingUp) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            LinearProgressIndicator(color = MbotePurplePrimary, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Sauvegarde chiffrée en cours...", fontSize = 12.sp, color = MbotePurplePrimary)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                isBackingUp = true
                                Toast.makeText(context, "Lancement de la sauvegarde...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sauvegarder maintenant", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fermer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AiToolsSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var assistantEnabled by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("Lingala") }
    var autoSummaries by remember { mutableStateOf(true) }
    var quickReplies by remember { mutableStateOf(com.loukatech.mbote.service.GeminiService.isEnabled) }
    var concisenessLevel by remember { mutableStateOf(com.loukatech.mbote.service.GeminiService.conciseness) }
    var aiImages by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Outils IA ⭐ Premium", icon = Icons.Outlined.AutoAwesome, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Psychology,
                        title = "Assistant Aron AI",
                        subtitle = "IA conversationnelle pour réponses, recherches et conseils",
                        isChecked = assistantEnabled,
                        onToggle = { assistantEnabled = !assistantEnabled }
                    )

                    SubSettingOptionChoice(
                        title = "Langue de traduction par défaut",
                        options = listOf("Lingala", "Kituba", "Français", "Anglais"),
                        selectedOption = language,
                        onSelect = { language = it }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.ShortText,
                        title = "Résumés automatiques",
                        subtitle = "Condenser les longs messages et discussions de groupe non lues",
                        isChecked = autoSummaries,
                        onToggle = { autoSummaries = !autoSummaries }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.ElectricBolt,
                        title = "Réponses intelligentes (Gemini)",
                        subtitle = "Suggestions de réponses contextuelles alimentées par l'IA Gemini",
                        isChecked = quickReplies,
                        onToggle = { quickReplies = !quickReplies }
                    )

                    if (quickReplies) {
                        SubSettingOptionChoice(
                            title = "Longueur des réponses suggérées",
                            options = listOf("Brief", "Balanced", "Elaborate"),
                            selectedOption = concisenessLevel,
                            onSelect = { concisenessLevel = it }
                        )
                    }

                    SettingsToggleRow(
                        icon = Icons.Outlined.Brush,
                        title = "Générateur d'images IA",
                        subtitle = "Créer et partager des illustrations IA directement dans vos tchats",
                        isChecked = aiImages,
                        onToggle = { aiImages = !aiImages }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Text(
                        text = "Suivi d'utilisation hebdomadaire",
                        style = MaterialTheme.typography.titleSmall,
                        color = MbotePurplePrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.AccessTime,
                        title = "Service de suivi en arrière-plan",
                        subtitle = "Mesure votre temps d'écran et envoie un bilan de défilement chaque semaine",
                        isChecked = true,
                        onToggle = { /* Toujours activé */ }
                    )

                    OutlinedButton(
                        onClick = {
                            com.loukatech.mbote.service.AppUsageTrackingService.triggerRecap(context)
                            Toast.makeText(context, "Bilan hebdomadaire simulé envoyé !", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simuler le bilan hebdomadaire 📊", color = MbotePurplePrimary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        com.loukatech.mbote.service.GeminiService.isEnabled = quickReplies
                        com.loukatech.mbote.service.GeminiService.conciseness = concisenessLevel
                        Toast.makeText(context, "Options IA enregistrées", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Valider les services Premium", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FindMyDeviceSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var gpsTracking by remember { mutableStateOf(true) }
    var simAlert by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Localiser mon appareil ⭐ Premium", icon = Icons.Outlined.GpsFixed, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.LocationOn,
                        title = "Suivi GPS en temps réel",
                        subtitle = "Géolocaliser l'appareil en cas de perte ou de vol",
                        isChecked = gpsTracking,
                        onToggle = { gpsTracking = !gpsTracking }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.History,
                        title = "Historique de position (24h)",
                        subtitle = "3 déplacements enregistrés aujourd'hui (Brazzaville)",
                        onClick = {
                            Toast.makeText(context, "3 dernières positions enregistrées à Brazzaville", Toast.LENGTH_LONG).show()
                        }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.RingVolume,
                        title = "Faire sonner l'appareil à distance",
                        subtitle = "Déclencher une alarme à volume maximal pendant 2 minutes",
                        onClick = {
                            Toast.makeText(context, "Test d'alarme sonore déclenché à volume maximal !", Toast.LENGTH_LONG).show()
                        }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.LockPerson,
                        title = "Verrouillage à distance",
                        subtitle = "Afficher un message d'urgence sur l'écran verrouillé",
                        onClick = {
                            Toast.makeText(context, "Écran de verrouillage à distance configuré", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsToggleRow(
                        icon = Icons.Outlined.SimCard,
                        title = "Alerte changement de carte SIM",
                        subtitle = "Envoyer une alerte de sécurité si une nouvelle carte SIM est insérée",
                        isChecked = simAlert,
                        onToggle = { simAlert = !simAlert }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Localisation et sécurité configurées", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fermer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HelpSupportSubSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SubSettingsDialogHeader(title = "Aide & Support Client", icon = Icons.Outlined.HelpOutline, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsClickRow(
                        icon = Icons.Outlined.Quiz,
                        title = "Foire Aux Questions (FAQ)",
                        subtitle = "Consulter les réponses aux questions les plus fréquentes",
                        onClick = {
                            Toast.makeText(context, "Ouverture de la FAQ MBoté...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.SupportAgent,
                        title = "Contacter le support client LoukaTech",
                        subtitle = "Discuter avec notre équipe à support@loukatech.com",
                        onClick = {
                            Toast.makeText(context, "E-mail adressé à support@loukatech.com", Toast.LENGTH_LONG).show()
                        }
                    )

                    SettingsClickRow(
                        icon = Icons.Outlined.Gavel,
                        title = "Conditions et Confidentialité",
                        subtitle = "Lire nos engagements en matière de protection des données",
                        onClick = {
                            Toast.makeText(context, "Conditions Générales d'Utilisation MBoté 2026", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MBoté Messenger v2.4.0-pro", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Propulsé par LoukaTech • 2026", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text("🟢 Serveurs opérationnels", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fermer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class BlockedUserDisplay(
    val id: String,
    val name: String,
    val avatar: String
)
