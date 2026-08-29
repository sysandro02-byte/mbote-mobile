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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loukatech.mbote.model.CallSettings
import com.loukatech.mbote.ui.theme.PurplePrimary

enum class CallSettingsSubPage {
    MAIN,
    CALLER_ID_SPAM,
    ACCESSIBILITY,
    ASSISTED_DIALING,
    BLOCKED_NUMBERS,
    CALLS_ACCOUNTS,
    DISPLAY_OPTIONS,
    QUICK_RESPONSES,
    SOUNDS_VIBRATION,
    VOICEMAIL_CONFIG,
    CONTACT_RINGTONES,
    CALLER_ID_ANNOUNCEMENT,
    FLIP_TO_SILENCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSettingsDialog(
    callSettings: CallSettings,
    onUpdateSettings: (CallSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentSubPage by remember { mutableStateOf(CallSettingsSubPage.MAIN) }

    Dialog(
        onDismissRequest = {
            if (currentSubPage != CallSettingsSubPage.MAIN) {
                currentSubPage = CallSettingsSubPage.MAIN
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentSubPage) {
                                CallSettingsSubPage.MAIN -> "Paramètres d'appel"
                                CallSettingsSubPage.CALLER_ID_SPAM -> "ID appelant et anti-spam"
                                CallSettingsSubPage.ACCESSIBILITY -> "Accessibilité"
                                CallSettingsSubPage.ASSISTED_DIALING -> "Composition assistée"
                                CallSettingsSubPage.BLOCKED_NUMBERS -> "Numéros bloqués"
                                CallSettingsSubPage.CALLS_ACCOUNTS -> "Comptes et réseau d'appel"
                                CallSettingsSubPage.DISPLAY_OPTIONS -> "Options d'affichage"
                                CallSettingsSubPage.QUICK_RESPONSES -> "Réponses rapides (SMS)"
                                CallSettingsSubPage.SOUNDS_VIBRATION -> "Sons et vibrations"
                                CallSettingsSubPage.VOICEMAIL_CONFIG -> "Messagerie et répondeur"
                                CallSettingsSubPage.CONTACT_RINGTONES -> "Sonneries des contacts"
                                CallSettingsSubPage.CALLER_ID_ANNOUNCEMENT -> "Annonce vocale de l'appelant"
                                CallSettingsSubPage.FLIP_TO_SILENCE -> "Retourner pour couper le son"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (currentSubPage != CallSettingsSubPage.MAIN) {
                                    currentSubPage = CallSettingsSubPage.MAIN
                                } else {
                                    onDismiss()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentSubPage) {
                    CallSettingsSubPage.MAIN -> {
                        CallSettingsMainList(
                            settings = callSettings,
                            onNavigateTo = { currentSubPage = it },
                            onToggleSetting = { onUpdateSettings(it) }
                        )
                    }
                    CallSettingsSubPage.CALLER_ID_SPAM -> {
                        CallerIdAndSpamSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.ACCESSIBILITY -> {
                        AccessibilitySubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.ASSISTED_DIALING -> {
                        AssistedDialingSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.BLOCKED_NUMBERS -> {
                        BlockedNumbersSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.CALLS_ACCOUNTS -> {
                        CallsAccountsSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.DISPLAY_OPTIONS -> {
                        DisplayOptionsSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.QUICK_RESPONSES -> {
                        QuickResponsesSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.SOUNDS_VIBRATION -> {
                        SoundsVibrationSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.VOICEMAIL_CONFIG -> {
                        VoicemailConfigSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.CONTACT_RINGTONES -> {
                        ContactRingtonesSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.CALLER_ID_ANNOUNCEMENT -> {
                        CallerIdAnnouncementSubPage(callSettings, onUpdateSettings)
                    }
                    CallSettingsSubPage.FLIP_TO_SILENCE -> {
                        FlipToSilenceSubPage(callSettings, onUpdateSettings)
                    }
                }
            }
        }
    }
}

@Composable
fun CallSettingsMainList(
    settings: CallSettings,
    onNavigateTo: (CallSettingsSubPage) -> Unit,
    onToggleSetting: (CallSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Section: Assistance d'appel
        item {
            CallSettingsSectionHeader("Assistance d'appel")
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.Info,
                title = "ID appelant et anti-spam",
                subtitle = if (settings.callerIdAndSpamEnabled) "Activé" else "Désactivé",
                onClick = { onNavigateTo(CallSettingsSubPage.CALLER_ID_SPAM) }
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }

        // Section: Général
        item {
            CallSettingsSectionHeader("Général")
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibilité",
                onClick = { onNavigateTo(CallSettingsSubPage.ACCESSIBILITY) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.TravelExplore,
                title = "Composition assistée",
                subtitle = "Indicatif pays automatique (${settings.defaultCountryCode})",
                onClick = { onNavigateTo(CallSettingsSubPage.ASSISTED_DIALING) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.Block,
                title = "Numéros bloqués",
                subtitle = "${settings.blockedNumbers.size} numéro(s) bloqué(s)",
                onClick = { onNavigateTo(CallSettingsSubPage.BLOCKED_NUMBERS) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.Call,
                title = "Comptes & Réseau d'appel",
                subtitle = "Comptes d'appel, Double appel & Appels Wi-Fi",
                onClick = { onNavigateTo(CallSettingsSubPage.CALLS_ACCOUNTS) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.FormatListBulleted,
                title = "Options d'affichage",
                subtitle = "Trier par : ${if (settings.sortByNameFirst) "Prénom" else "Nom"}",
                onClick = { onNavigateTo(CallSettingsSubPage.DISPLAY_OPTIONS) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "Réponses rapides (SMS)",
                subtitle = "Modifier les SMS de rejet d'appel",
                onClick = { onNavigateTo(CallSettingsSubPage.QUICK_RESPONSES) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.VolumeUp,
                title = "Sons et vibrations",
                subtitle = settings.callRingtone,
                onClick = { onNavigateTo(CallSettingsSubPage.SOUNDS_VIBRATION) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Default.Voicemail,
                title = "Messagerie & Répondeur",
                subtitle = "Notifications et numéro (${settings.voicemailNumber})",
                onClick = { onNavigateTo(CallSettingsSubPage.VOICEMAIL_CONFIG) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.MusicNote,
                title = "Sonneries des contacts",
                subtitle = "Attribuer des sonneries personnalisées",
                onClick = { onNavigateTo(CallSettingsSubPage.CONTACT_RINGTONES) }
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }

        // Section: Avancé
        item {
            CallSettingsSectionHeader("Avancé")
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.Campaign,
                title = "Annonce vocale de l'appelant",
                subtitle = settings.callerIdAnnouncement,
                onClick = { onNavigateTo(CallSettingsSubPage.CALLER_ID_ANNOUNCEMENT) }
            )
        }
        item {
            CallSettingsItemRow(
                icon = Icons.Outlined.SyncProblem,
                title = "Retourner pour couper le son",
                subtitle = if (settings.flipToSilence) "Activé" else "Désactivé",
                onClick = { onNavigateTo(CallSettingsSubPage.FLIP_TO_SILENCE) }
            )
        }
    }
}

@Composable
fun CallSettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = PurplePrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun CallSettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ----------------- SUB-PAGES -----------------

@Composable
fun CallerIdAndSpamSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Identifiez les entreprises et les appelants, et soyez averti automatiquement contre les numéros suspects ou spams.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingToggleCard(
            title = "Afficher l'ID d'appelant et spam",
            description = "Identifier les numéros d'entreprises et spams lors de la réception ou émission d'appels.",
            checked = settings.callerIdAndSpamEnabled,
            onCheckedChange = { onUpdate(settings.copy(callerIdAndSpamEnabled = it)) }
        )

        SettingToggleCard(
            title = "Filtrer les appels indésirables",
            description = "Empêcher les numéros spams suspectés de faire sonner votre téléphone.",
            checked = settings.filterSpamCalls,
            onCheckedChange = { onUpdate(settings.copy(filterSpamCalls = it)) }
        )

        SettingToggleCard(
            title = "Badge d'appels certifiés",
            description = "Afficher une coche de vérification pour les organisations vérifiées.",
            checked = settings.verifiedCallsBadge,
            onCheckedChange = { onUpdate(settings.copy(verifiedCallsBadge = it)) }
        )
    }
}

@Composable
fun AccessibilitySubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggleCard(
            title = "Compatibilité appareils auditifs (HAC)",
            description = "Activer le mode couplage acoustique pour aides auditives.",
            checked = settings.hearingAidCompatibility,
            onCheckedChange = { onUpdate(settings.copy(hearingAidCompatibility = it)) }
        )

        SettingToggleCard(
            title = "Texte en temps réel (RTT)",
            description = "Permet l'envoi de messages texte en direct pendant un appel vocal.",
            checked = settings.realTimeTextRtt,
            onCheckedChange = { onUpdate(settings.copy(realTimeTextRtt = it)) }
        )

        SettingToggleCard(
            title = "Sous-titres & Transcriptions en direct",
            description = "Transcrire la parole en direct sur l'écran pendant vos conversations.",
            checked = settings.liveTranscribeCalls,
            onCheckedChange = { onUpdate(settings.copy(liveTranscribeCalls = it)) }
        )
    }
}

@Composable
fun AssistedDialingSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    val countries = listOf("+242" to "Congo (Brazzaville)", "+243" to "RD Congo (Kinshasa)", "+33" to "France", "+1" to "États-Unis / Canada", "+237" to "Cameroun", "+225" to "Côte d'Ivoire")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggleCard(
            title = "Composition assistée",
            description = "Prédit et ajoute automatiquement l'indicatif international lors des appels.",
            checked = settings.assistedDialingEnabled,
            onCheckedChange = { onUpdate(settings.copy(assistedDialingEnabled = it)) }
        )

        Text(
            text = "Pays par défaut :",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        countries.forEach { (code, name) ->
            Surface(
                onClick = { onUpdate(settings.copy(defaultCountryCode = code)) },
                shape = RoundedCornerShape(12.dp),
                color = if (settings.defaultCountryCode == code) PurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (settings.defaultCountryCode == code) PurplePrimary else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text(code, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (settings.defaultCountryCode == code) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurplePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedNumbersSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    var newNumberInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggleCard(
            title = "Appels masqués et inconnus",
            description = "Bloquer automatiquement les appels provenant de numéros non identifiés.",
            checked = settings.blockUnknownCallers,
            onCheckedChange = { onUpdate(settings.copy(blockUnknownCallers = it)) }
        )

        Text(
            text = "Ajouter un numéro à bloquer :",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newNumberInput,
                onValueChange = { newNumberInput = it },
                placeholder = { Text("ex: +242 06 123 4567") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    if (newNumberInput.isNotBlank()) {
                        val updated = settings.blockedNumbers + newNumberInput.trim()
                        onUpdate(settings.copy(blockedNumbers = updated))
                        newNumberInput = ""
                        Toast.makeText(context, "Numéro ajouté aux bloqués", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text("Bloquer")
            }
        }

        Text(
            text = "Liste des numéros bloqués (${settings.blockedNumbers.size}) :",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.blockedNumbers) { num ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(num, fontWeight = FontWeight.Medium)
                        IconButton(onClick = {
                            val updated = settings.blockedNumbers.filterNot { it == num }
                            onUpdate(settings.copy(blockedNumbers = updated))
                            Toast.makeText(context, "Numéro débloqué", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Débloquer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsAccountsSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggleCard(
            title = "Appels Wi-Fi HD (VoWiFi)",
            description = "Passer et recevoir des appels en haute définition via le réseau Wi-Fi.",
            checked = settings.wifiCallingHd,
            onCheckedChange = { onUpdate(settings.copy(wifiCallingHd = it)) }
        )

        SettingToggleCard(
            title = "Double appel (Signal d'appel)",
            description = "Pendant un appel, recevoir les notifications de nouveaux appels entrants.",
            checked = settings.callWaitingEnabled,
            onCheckedChange = { onUpdate(settings.copy(callWaitingEnabled = it)) }
        )

        SettingToggleCard(
            title = "Renvoi d'appel",
            description = "Rediriger les appels entrants vers un autre numéro en cas d'indisponibilité.",
            checked = settings.callForwardingEnabled,
            onCheckedChange = { onUpdate(settings.copy(callForwardingEnabled = it)) }
        )
    }
}

@Composable
fun DisplayOptionsSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggleCard(
            title = "Trier par prénom",
            description = if (settings.sortByNameFirst) "Trié par prénom en premier" else "Trié par nom en premier",
            checked = settings.sortByNameFirst,
            onCheckedChange = { onUpdate(settings.copy(sortByNameFirst = it)) }
        )

        Text(
            text = "Format d'affichage des noms :",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        listOf("Prénom d'abord", "Nom d'abord").forEach { format ->
            Surface(
                onClick = { onUpdate(settings.copy(nameDisplayFormat = format)) },
                shape = RoundedCornerShape(12.dp),
                color = if (settings.nameDisplayFormat == format) PurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (settings.nameDisplayFormat == format) PurplePrimary else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(format, fontWeight = FontWeight.Medium)
                    if (settings.nameDisplayFormat == format) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurplePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickResponsesSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editedText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Modifier les réponses SMS rapides envoyées lors du rejet d'un appel avec message :",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        settings.quickResponses.forEachIndexed { index, resp ->
            Surface(
                onClick = {
                    editingIndex = index
                    editedText = resp
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = resp,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Modifier",
                        tint = PurplePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (editingIndex != null) {
            AlertDialog(
                onDismissRequest = { editingIndex = null },
                title = { Text("Modifier la réponse rapide") },
                text = {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val updatedList = settings.quickResponses.toMutableList()
                        updatedList[editingIndex!!] = editedText
                        onUpdate(settings.copy(quickResponses = updatedList))
                        editingIndex = null
                        Toast.makeText(context, "Réponse enregistrée", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Enregistrer", fontWeight = FontWeight.Bold, color = PurplePrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingIndex = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun SoundsVibrationSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    val ringtones = listOf("MBoté Harmony (Par défaut)", "Congo Rumba Rhythms", "Kinshasa Sunset Chimes", "Brazza Flute HD", "Digital Modern")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggleCard(
            title = "Vibrer pour les appels",
            description = "Le téléphone vibre en continu lors de la réception d'appels.",
            checked = settings.vibrateForCalls,
            onCheckedChange = { onUpdate(settings.copy(vibrateForCalls = it)) }
        )

        SettingToggleCard(
            title = "Tons du clavier (DTMF)",
            description = "Émettre une rétroaction acoustique lors de la saisie de numéros.",
            checked = settings.dialpadTones,
            onCheckedChange = { onUpdate(settings.copy(dialpadTones = it)) }
        )

        SettingToggleCard(
            title = "Vibrer à la réponse",
            description = "Légère vibration lorsque le correspondant décroche l'appel.",
            checked = settings.vibrateOnAnswer,
            onCheckedChange = { onUpdate(settings.copy(vibrateOnAnswer = it)) }
        )

        Text(
            text = "Sélectionner la sonnerie :",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        ringtones.forEach { tone ->
            Surface(
                onClick = { onUpdate(settings.copy(callRingtone = tone)) },
                shape = RoundedCornerShape(12.dp),
                color = if (settings.callRingtone == tone) PurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (settings.callRingtone == tone) PurplePrimary else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(tone, fontWeight = FontWeight.Medium)
                    if (settings.callRingtone == tone) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurplePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun VoicemailConfigSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    var vmNumber by remember { mutableStateOf(settings.voicemailNumber) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggleCard(
            title = "Notifications de répondeur",
            description = "Afficher les alertes dans la barre d'état pour les nouveaux messages.",
            checked = settings.voicemailNotifications,
            onCheckedChange = { onUpdate(settings.copy(voicemailNotifications = it)) }
        )

        SettingToggleCard(
            title = "Vibrer pour le répondeur",
            description = "Vibrer lorsqu'un nouveau message vocal est reçu.",
            checked = settings.voicemailVibration,
            onCheckedChange = { onUpdate(settings.copy(voicemailVibration = it)) }
        )

        Text(
            text = "Numéro du service répondeur :",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = vmNumber,
            onValueChange = { vmNumber = it },
            placeholder = { Text("123 / 888 / +242...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                TextButton(onClick = {
                    onUpdate(settings.copy(voicemailNumber = vmNumber))
                    Toast.makeText(context, "Numéro du répondeur mis à jour", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold, color = PurplePrimary)
                }
            }
        )
    }
}

@Composable
fun ContactRingtonesSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mélodies attribuées aux contacts VIP :",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        settings.contactRingtones.forEach { (contact, tone) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(contact, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(tone, fontSize = 13.sp, color = PurplePrimary)
                    }
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = PurplePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun CallerIdAnnouncementSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    val options = listOf("Toujours", "Uniquement avec casque", "Jamais")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Le nom et le numéro de l'appelant seront énoncés à haute voix par synthèse vocale lors d'un appel entrant.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        options.forEach { opt ->
            Surface(
                onClick = { onUpdate(settings.copy(callerIdAnnouncement = opt)) },
                shape = RoundedCornerShape(12.dp),
                color = if (settings.callerIdAnnouncement == opt) PurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (settings.callerIdAnnouncement == opt) PurplePrimary else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(opt, fontWeight = FontWeight.Medium)
                    if (settings.callerIdAnnouncement == opt) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurplePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun FlipToSilenceSubPage(
    settings: CallSettings,
    onUpdate: (CallSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pour couper le son d'un appel entrant, retournez votre téléphone écran vers le bas sur une surface plate.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingToggleCard(
            title = "Retourner pour couper le son",
            description = "Le capteur désactive la sonnerie dès que l'appareil est placé face vers le bas.",
            checked = settings.flipToSilence,
            onCheckedChange = { onUpdate(settings.copy(flipToSilence = it)) }
        )
    }
}

@Composable
fun SettingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PurplePrimary
                )
            )
        }
    }
}
