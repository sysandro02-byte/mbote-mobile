package com.loukatech.mbote.ui.components

import android.widget.Toast
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

    // Demo contact candidates for instant scan simulation
    val sampleScanCandidates = remember(allChats, allMastaUsers) {
        val list = mutableListOf<ScannedContactResult>()
        allMastaUsers.take(4).forEach { u ->
            list.add(ScannedContactResult(id = u.id, name = u.name, username = "@${u.name.lowercase().replace(" ", "")}", avatar = u.avatar, phone = "+242 06 888 1234", bio = u.infoSubtitle))
        }
        allChats.take(3).forEach { c ->
            if (list.none { it.id == c.id }) {
                list.add(ScannedContactResult(id = c.id, name = c.name, username = "@${c.name.lowercase().replace(" ", "")}", avatar = c.avatar, phone = "+242 05 555 9999", bio = "Contact MBoté certifié"))
            }
        }
        if (list.isEmpty()) {
            list.add(ScannedContactResult(id = "user_aron", name = "Aron Ngala", username = "@aron_ngala", avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", phone = "+242 06 999 0000", bio = "Ambassadeur LoukaTech & Passionné de Tech"))
            list.add(ScannedContactResult(id = "user_linda", name = "Linda Bongo Ondimba", username = "@linda_bongo", avatar = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=150", phone = "+241 07 111 2222", bio = "Créatrice MBoté & Musique"))
        }
        list
    }

    fun handleSimulateScan(contact: ScannedContactResult) {
        scannedResult = contact
        Toast.makeText(context, "📷 Code QR Détecté avec succès !", Toast.LENGTH_SHORT).show()
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

                        // Simulation / Quick action buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Simuler la détection d'un contact :",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sampleScanCandidates) { cand ->
                                    Surface(
                                        onClick = { handleSimulateScan(cand) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MbotePurpleSoft,
                                        border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.3f)),
                                        modifier = Modifier.testTag("simulate_scan_contact_${cand.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AsyncImage(
                                                model = cand.avatar,
                                                contentDescription = cand.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(24.dp).clip(CircleShape)
                                            )
                                            Text(cand.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showManualInputDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("manual_qr_entry_button")
                                ) {
                                    Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Entrer ID manuellement", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Pick from gallery simulation
                                        val randomContact = sampleScanCandidates.random()
                                        handleSimulateScan(randomContact)
                                        Toast.makeText(context, "Image de QR code importée de la galerie !", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("gallery_qr_picker_button")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Galerie", fontSize = 12.sp)
                                }
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
                    Text("Entrez un nom d'utilisateur (ex: @aron_ngala) ou un numéro de téléphone pour simuler le scan QR.", fontSize = 12.5.sp)
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
                            val matched = sampleScanCandidates.find {
                                it.username.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
                            } ?: ScannedContactResult(
                                id = "user_manual_${query.hashCode()}",
                                name = query.removePrefix("@").replaceFirstChar { it.uppercase() },
                                username = if (query.startsWith("@")) query else "@$query",
                                avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                phone = "+242 06 000 0000",
                                bio = "Nouveau contact découvert via scanner QR MBoté"
                            )
                            scannedResult = matched
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
