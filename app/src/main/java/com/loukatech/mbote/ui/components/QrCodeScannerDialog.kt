package com.loukatech.mbote.ui.components

import android.widget.Toast
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.model.Chat
import com.loukatech.mbote.model.MastaUser
import com.loukatech.mbote.model.UserProfile
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

data class ScannedContactResult(
    val id: String,
    val name: String,
    val username: String,
    val avatar: String,
    val phone: String,
    val bio: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScannerDialog(
    userProfile: UserProfile,
    allChats: List<Chat> = emptyList(),
    allMastaUsers: List<MastaUser> = emptyList(),
    onOpenChat: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Scan, 1: My QR
    var isFlashOn by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<ScannedContactResult?>(null) }
    var manualInput by remember { mutableStateOf("") }
    var showManualInputDialog by remember { mutableStateOf(false) }

    // Scanner beam animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_beam")
    val beamOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_beam_offset"
    )

    fun handleScannedValue(rawValue: String) {
        val key = rawValue.substringAfterLast('/').substringAfterLast(':').trim().removePrefix("@")
        val user = allMastaUsers.firstOrNull {
            it.id == key || it.name.replace(" ", "", ignoreCase = true).equals(key.replace("_", ""), ignoreCase = true)
        }
        val chat = allChats.firstOrNull { it.id == key }
        scannedResult = when {
            user != null -> ScannedContactResult(
                id = user.id,
                name = user.name,
                username = "",
                avatar = user.avatar,
                phone = "",
                bio = user.infoSubtitle
            )
            chat != null -> ScannedContactResult(
                id = chat.id,
                name = chat.name,
                username = "",
                avatar = chat.avatar,
                phone = "",
                bio = if (chat.isGroup) "Groupe MBoté" else "Discussion MBoté"
            )
            else -> null
        }
        if (scannedResult == null) {
            Toast.makeText(context, "Ce QR code ne correspond à aucun compte chargé depuis le serveur.", Toast.LENGTH_LONG).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar with Close Icon
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
                            color = MbotePurpleSoft,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MbotePurplePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = "Scanner de Contact QR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_qr_scanner_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MbotePurplePrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Scanner un QR Code", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = MbotePurplePrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Mon QR Code", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = MbotePurplePrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tab Content
                if (selectedTab == 0) {
                    // TAB 0: SCANNER
                    if (scannedResult != null) {
                        // SCANNED RESULT CARD
                        val contact = scannedResult!!
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFAF8FF),
                            border = BorderStroke(1.5.dp, MbotePurplePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Text("Contact Détecté !", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                AsyncImage(
                                    model = contact.avatar,
                                    contentDescription = contact.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MbotePurplePrimary, CircleShape)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(contact.username, color = MbotePurplePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(contact.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (contact.bio.isNotBlank()) {
                                    Text(
                                        text = contact.bio,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenChat(contact.id)
                                            Toast.makeText(context, "Discussion ouverte avec ${contact.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).testTag("start_chat_scanned_button")
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Discuter", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { scannedResult = null },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("rescan_qr_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rescanner")
                                    }
                                }
                            }
                        }
                    } else {
                        // LIVE VIEWFINDER
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // Camera viewfinder box
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .border(2.dp, if (isFlashOn) Color.Yellow else MbotePurplePrimary, RoundedCornerShape(16.dp))
                            ) {
                                // Animated scanning laser beam
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .offset(y = beamOffset.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MbotePurpleLight,
                                                    Color.White,
                                                    MbotePurpleLight,
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

                            // Flash toggle button
                            IconButton(
                                onClick = {
                                    isFlashOn = !isFlashOn
                                    Toast.makeText(context, if (isFlashOn) "Flash activé 🔦" else "Flash désactivé 🌑", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Torche",
                                    tint = if (isFlashOn) Color.Yellow else Color.White
                                )
                            }

                            Text(
                                text = "Placez le QR Code de votre contact dans le cadre",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    GmsBarcodeScanning.getClient(context).startScan()
                                        .addOnSuccessListener { barcode ->
                                            barcode.rawValue?.let(::handleScannedValue)
                                        }
                                        .addOnFailureListener { error ->
                                            Toast.makeText(context, error.message ?: "Lecture du QR code impossible.", Toast.LENGTH_LONG).show()
                                        }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("scan_real_qr_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Scanner un QR code")
                            }
                            OutlinedButton(
                                onClick = { showManualInputDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("manual_qr_entry_button")
                            ) {
                                Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Entrer un identifiant")
                            }
                        }
                    }
                } else {
                    // TAB 1: MY PERSONAL QR CODE
                    val cleanUsername = userProfile.username.removePrefix("@")
                    val profileUrl = "https://mbote.app/u/$cleanUsername"

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, MbotePurpleSoft),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = userProfile.avatar,
                                    contentDescription = userProfile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MbotePurplePrimary, CircleShape)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(userProfile.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                                    Text(userProfile.username, color = MbotePurplePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFFAF8FF),
                                    border = BorderStroke(1.dp, Color(0xFFEDE9FE)),
                                    modifier = Modifier.size(160.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = "Votre QR Code Contact",
                                            tint = MbotePurplePrimary,
                                            modifier = Modifier.size(140.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Votre QR Code unique MBoté. Vos amis peuvent le scanner pour vous ajouter instantanément.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(profileUrl))
                                    Toast.makeText(context, "📋 Lien de profil copié : $profileUrl", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).testTag("copy_my_qr_link_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier le lien", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Partage de votre QR code lancé !", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("share_my_qr_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Partager")
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual ID Entry Dialog
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = MbotePurplePrimary)
                    Text("Recherche par ID / Numéro", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Entrez l’identifiant exact d’un compte déjà chargé depuis le serveur.", fontSize = 12.5.sp)
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = { Text("@nom_utilisateur ou +242...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MbotePurplePrimary),
                        modifier = Modifier.fillMaxWidth().testTag("manual_qr_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val query = manualInput.trim()
                        if (query.isNotBlank()) {
                            val matchedUser = allMastaUsers.find {
                                it.id.equals(query, ignoreCase = true) || it.name.equals(query.removePrefix("@"), ignoreCase = true)
                            }
                            if (matchedUser == null) {
                                Toast.makeText(context, "Compte serveur introuvable.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            scannedResult = ScannedContactResult(
                                id = matchedUser.id,
                                name = matchedUser.name,
                                username = "",
                                avatar = matchedUser.avatar,
                                phone = "",
                                bio = matchedUser.infoSubtitle
                            )
                            showManualInputDialog = false
                            manualInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Rechercher & Scanner")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
