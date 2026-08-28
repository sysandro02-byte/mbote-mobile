package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("mbote_prefs", android.content.Context.MODE_PRIVATE) }

    var profileVisibility by remember { mutableStateOf("Mes contacts") }
    var onlineStatusVisibility by remember { mutableStateOf("Tout le monde") }
    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var contactSyncPermission by remember { mutableStateOf(true) }
    var dataSharingAnalytics by remember { mutableStateOf(false) }
    var adPersonalization by remember { mutableStateOf(false) }
    var offlineCacheEnabled by remember { mutableStateOf(true) }
    var biometricLockEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    var biometricTimeout by remember { mutableStateOf(sharedPrefs.getString("biometric_timeout", "Immédiatement") ?: "Immédiatement") }
    var showTestBiometricOverlay by remember { mutableStateOf(false) }

    if (showTestBiometricOverlay) {
        BiometricLockOverlay(
            isTestMode = true,
            onUnlockSuccess = {
                showTestBiometricOverlay = false
                Toast.makeText(context, "✅ Déverrouillage biométrique validé !", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showTestBiometricOverlay = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres de Confidentialité", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card explaining privacy & E2EE
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MbotePurplePrimary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MbotePurplePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Confidentialité & Chiffrement de bout en bout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Vos messages, appels et données de contact sont protégés par le protocole sécurisé MBoté E2EE.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 1: Profile & Visibility
            Text(
                text = "Visibilité du Profil & Présence",
                style = MaterialTheme.typography.titleSmall,
                color = MbotePurplePrimary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrivacyChoiceRow(
                        title = "Visibilité du profil",
                        subtitle = "Qui peut voir vos détails et vos publications",
                        options = listOf("Tout le monde", "Mes contacts", "Personne"),
                        selected = profileVisibility,
                        onSelect = { profileVisibility = it }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PrivacyChoiceRow(
                        title = "Statut en ligne / Vu à",
                        subtitle = "Qui peut voir lorsque vous êtes en ligne",
                        options = listOf("Tout le monde", "Mes contacts", "Personne"),
                        selected = onlineStatusVisibility,
                        onSelect = { onlineStatusVisibility = it }
                    )
                }
            }

            // Section 2: Messaging & Read Receipts
            Text(
                text = "Messagerie & Sécurité",
                style = MaterialTheme.typography.titleSmall,
                color = MbotePurplePrimary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Confirmations de lecture", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Afficher les double coches bleues lorsque vous lisez un message", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = readReceiptsEnabled,
                            onCheckedChange = { readReceiptsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary),
                            modifier = Modifier.testTag("read_receipts_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Biometric Authentication (Fingerprint / Face Unlock) Toggle & Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MbotePurplePrimary, modifier = Modifier.size(18.dp))
                                Text(text = "Verrouillage biométrique (Empreinte / Visage)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Exiger l'empreinte digitale ou Face Unlock pour ouvrir MBoté", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = biometricLockEnabled,
                            onCheckedChange = { isChecked ->
                                biometricLockEnabled = isChecked
                                sharedPrefs.edit().putBoolean("biometric_enabled", isChecked).apply()
                                if (isChecked) {
                                    Toast.makeText(context, "🔒 Verrouillage biométrique activé !", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "🔓 Verrouillage biométrique désactivé.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary),
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }

                    if (biometricLockEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        PrivacyChoiceRow(
                            title = "Délai de verrouillage automatique",
                            subtitle = "Temps avant verrouillage après mise en arrière-plan",
                            options = listOf("Immédiatement", "Après 1 min", "Après 15 min", "Après 1 heure"),
                            selected = biometricTimeout,
                            onSelect = { choice ->
                                biometricTimeout = choice
                                sharedPrefs.edit().putString("biometric_timeout", choice).apply()
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showTestBiometricOverlay = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_biometric_button")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tester le déverrouillage biométrique", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }

            // Section 3: Contact Permissions & Data Sharing
            Text(
                text = "Contacts & Partage de Données",
                style = MaterialTheme.typography.titleSmall,
                color = MbotePurplePrimary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Autorisation de synchronisation des contacts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Permettre à MBoté de scanner et synchroniser les contacts du téléphone pour suggérer des amis", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = contactSyncPermission,
                            onCheckedChange = { contactSyncPermission = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary),
                            modifier = Modifier.testTag("contact_sync_permission_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Cache local Room hors ligne", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Conserver les anciens messages et conversations en local sur l'appareil", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = offlineCacheEnabled,
                            onCheckedChange = { offlineCacheEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Partage des données analytiques", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Aider à améliorer l'application en partageant des rapports d'utilisation anonymes", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = dataSharingAnalytics,
                            onCheckedChange = { dataSharingAnalytics = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Personnalisation publicitaire", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Permettre l'affichage de suggestions adaptées à vos centres d'intérêt", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = adPersonalization,
                            onCheckedChange = { adPersonalization = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MbotePurplePrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Paramètres de confidentialité enregistrés avec succès !", Toast.LENGTH_LONG).show()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_privacy_settings_button")
            ) {
                Text("Enregistrer les préférences", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun PrivacyChoiceRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MbotePurplePrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = selected,
                    color = MbotePurplePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(opt)
                                expanded = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = opt, fontSize = 13.sp, fontWeight = if (opt == selected) FontWeight.Bold else FontWeight.Normal)
                        if (opt == selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MbotePurplePrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
