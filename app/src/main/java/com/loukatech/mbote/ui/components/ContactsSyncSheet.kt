package com.loukatech.mbote.ui.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loukatech.mbote.model.SyncedContact
import com.loukatech.mbote.ui.theme.PurpleLight
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft

@Composable
fun ContactsSyncSheet(
    contacts: List<SyncedContact>,
    onSyncNow: () -> Unit,
    onContactSelected: (SyncedContact) -> Unit,
    onAudioCallClick: (SyncedContact) -> Unit,
    onDismiss: () -> Unit,
    blockedContactIds: Set<String> = emptySet(),
    onBlockContact: (String) -> Unit = {},
    onUnblockContact: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: Tous (Non bloqués), 1: MBoté uniquement, 2: Bloqués
    var showPermissionRationale by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSyncNow()
        } else {
            showPermissionRationale = true
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            icon = { Icon(Icons.Default.Contacts, contentDescription = null, tint = PurplePrimary) },
            title = { Text("Accès aux contacts requis", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "MBoté utilise la permission 'Contacts' pour synchroniser votre carnet d'adresses en toute confidentialité et détecter vos proches utilisant le réseau chiffré."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    val filteredContacts = remember(contacts, searchQuery, selectedFilterIndex, blockedContactIds) {
        contacts.filter { contact ->
            val isBlocked = blockedContactIds.contains(contact.id) || blockedContactIds.contains(contact.name)
            val matchesQuery = searchQuery.isBlank() ||
                    contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.phoneNumber.contains(searchQuery)

            val matchesFilter = when (selectedFilterIndex) {
                0 -> !isBlocked // Tous non bloqués
                1 -> !isBlocked && contact.isMboteUser // MBoté actifs non bloqués
                2 -> isBlocked // Bloqués uniquement
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(6.dp)
                .testTag("contacts_sync_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PurpleSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Contacts & Masta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${contacts.count { !blockedContactIds.contains(it.id) }} synchronisés • E2EE",
                                style = MaterialTheme.typography.bodySmall,
                                color = PurplePrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("close_contacts_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un contact, numéro…", fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contacts_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilterIndex == 0,
                        onClick = { selectedFilterIndex = 0 },
                        label = { Text("Tous (${contacts.count { !blockedContactIds.contains(it.id) }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedFilterIndex == 1,
                        onClick = { selectedFilterIndex = 1 },
                        label = { Text("MBoté ✨") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = Color.White
                        )
                    )

                    if (blockedContactIds.isNotEmpty()) {
                        FilterChip(
                            selected = selectedFilterIndex == 2,
                            onClick = { selectedFilterIndex = 2 },
                            label = { Text("Bloqués (${blockedContactIds.size}) 🚫") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDC2626),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Contacts
                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selectedFilterIndex == 2) Icons.Default.CheckCircle else Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = PurpleLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilterIndex == 2) "Aucun contact bloqué" else "Aucun contact trouvé",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            val isBlocked = blockedContactIds.contains(contact.id) || blockedContactIds.contains(contact.name)
                            ContactItemRow(
                                contact = contact,
                                isBlocked = isBlocked,
                                onChatClick = {
                                    if (!isBlocked) onContactSelected(contact)
                                    else Toast.makeText(context, "Débloquez le contact pour démarrer une discussion", Toast.LENGTH_SHORT).show()
                                },
                                onCallClick = {
                                    if (!isBlocked) onAudioCallClick(contact)
                                    else Toast.makeText(context, "Contact bloqué", Toast.LENGTH_SHORT).show()
                                },
                                onBlockClick = {
                                    onBlockContact(contact.id)
                                    Toast.makeText(context, "${contact.name} bloqué", Toast.LENGTH_SHORT).show()
                                },
                                onUnblockClick = {
                                    onUnblockContact(contact.id)
                                    Toast.makeText(context, "${contact.name} débloqué", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Sync Button
                Button(
                    onClick = onSyncNow,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_contacts_now_button")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synchroniser les contacts du téléphone")
                }
            }
        }
    }
}

@Composable
fun ContactItemRow(
    contact: SyncedContact,
    isBlocked: Boolean = false,
    onChatClick: () -> Unit,
    onCallClick: () -> Unit,
    onBlockClick: () -> Unit = {},
    onUnblockClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isBlocked) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (isBlocked) BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
            .testTag("contact_item_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isBlocked) Color(0xFFFEE2E2) else if (contact.isMboteUser) PurpleSoft else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (contact.avatarUrl != null) {
                    AsyncImage(
                        model = contact.avatarUrl,
                        contentDescription = contact.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initials = contact.name.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "C" }
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = if (isBlocked) Color(0xFFDC2626) else if (contact.isMboteUser) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isBlocked) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (contact.isMboteUser && !isBlocked) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Utilise MBoté",
                            tint = PurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isBlocked) "🚫 Contact bloqué" else contact.statusText,
                    fontSize = 11.sp,
                    color = if (isBlocked) Color(0xFFDC2626) else if (contact.isMboteUser) Color(0xFF10B981) else PurpleLight
                )
            }

            // Actions (Chat, Call, More / Unblock)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBlocked) {
                    Button(
                        onClick = onUnblockClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Débloquer", fontSize = 11.5.sp, color = Color.White)
                    }
                } else {
                    if (contact.isMboteUser) {
                        IconButton(
                            onClick = onCallClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Appeler",
                                tint = PurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onChatClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Discuter",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onChatClick,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Inviter", fontSize = 11.sp)
                        }
                    }

                    // Context Menu for options including "Bloquer"
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("💬 Démarrer une discussion") },
                                leadingIcon = { Icon(Icons.Default.ChatBubble, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    onChatClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📞 Appel vocal HD") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onCallClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📹 Appel vidéo HD") },
                                leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onCallClick()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("🚫 Bloquer le contact", color = Color(0xFFDC2626)) },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFDC2626)) },
                                onClick = {
                                    showMenu = false
                                    onBlockClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
