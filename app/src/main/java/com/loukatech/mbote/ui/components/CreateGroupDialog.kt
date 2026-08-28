package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.model.SyncedContact
import com.loukatech.mbote.ui.theme.PurplePrimary

data class GroupAvatarPreset(
    val id: String,
    val title: String,
    val imageUrl: String
)

@Composable
fun CreateGroupDialog(
    contacts: List<SyncedContact>,
    onDismiss: () -> Unit,
    onConfirm: (groupName: String, description: String, members: List<SyncedContact>, avatar: String, initialMessage: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarPresets = remember {
        listOf(
            GroupAvatarPreset(
                "team",
                "Équipe",
                "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200&auto=format&fit=crop&q=80"
            ),
            GroupAvatarPreset(
                "tech",
                "Tech Brazza",
                "https://images.unsplash.com/photo-1531482615713-2afd69097998?w=200&auto=format&fit=crop&q=80"
            ),
            GroupAvatarPreset(
                "music",
                "Musique & Rumba",
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&auto=format&fit=crop&q=80"
            ),
            GroupAvatarPreset(
                "friends",
                "Masta & Potes",
                "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=200&auto=format&fit=crop&q=80"
            ),
            GroupAvatarPreset(
                "business",
                "Business 242",
                "https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=200&auto=format&fit=crop&q=80"
            ),
            GroupAvatarPreset(
                "family",
                "Famille",
                "https://images.unsplash.com/photo-1609234656388-0ff363383899?w=200&auto=format&fit=crop&q=80"
            ),
            GroupAvatarPreset(
                "creative",
                "Créatifs & Art",
                "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=200&auto=format&fit=crop&q=80"
            )
        )
    }

    var selectedAvatarUrl by remember { mutableStateOf(avatarPresets[0].imageUrl) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var initialMessage by remember { mutableStateOf("Bienvenue dans notre groupe MBoté !") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedContactIds = remember { mutableStateListOf<String>() }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
        }
    }

    val selectedContacts = remember(contacts, selectedContactIds) {
        contacts.filter { selectedContactIds.contains(it.id) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 16.dp,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .padding(6.dp)
                .testTag("create_group_dialog")
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Nouveau Groupe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${selectedContactIds.size} membre(s) sélectionné(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = PurplePrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("close_group_dialog")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Group Icon + Name Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Group Photo Badge with tap to toggle presets
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFAF5FF))
                            .border(2.dp, PurplePrimary, CircleShape)
                            .clickable { showAvatarPicker = !showAvatarPicker }
                            .testTag("group_avatar_picker_trigger"),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedAvatarUrl,
                            contentDescription = "Photo du groupe",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Changer l'image",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Group Name Input
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Nom du groupe *") },
                        placeholder = { Text("ex. Mbote Squad Brazza") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("group_name_input")
                    )
                }

                // Avatar Presets Picker Tray (expandable or always accessible)
                if (showAvatarPicker) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Choisir une icône de groupe :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurplePrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(avatarPresets, key = { it.id }) { preset ->
                            val isSelected = selectedAvatarUrl == preset.imageUrl
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedAvatarUrl = preset.imageUrl
                                    showAvatarPicker = false
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(
                                            if (isSelected) 2.5.dp else 1.dp,
                                            if (isSelected) PurplePrimary else Color.Transparent,
                                            CircleShape
                                        )
                                ) {
                                    AsyncImage(
                                        model = preset.imageUrl,
                                        contentDescription = preset.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / But du groupe (optionnel)") },
                    placeholder = { Text("Partagez la vision du groupe...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Selected Members Horizontal Chip List
                if (selectedContacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Membres sélectionnés (${selectedContacts.size}) :",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedContacts, key = { it.id }) { contact ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PurplePrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = contact.name.split(" ").firstOrNull() ?: contact.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PurplePrimary
                                    )
                                    IconButton(
                                        onClick = { selectedContactIds.remove(contact.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Retirer",
                                            tint = PurplePrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Contacts Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher des contacts à ajouter...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Contacts Selection List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val isSelected = selectedContactIds.contains(contact.id)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (isSelected) selectedContactIds.remove(contact.id)
                                    else selectedContactIds.add(contact.id)
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PurplePrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
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
                                        Text(
                                            text = contact.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PurplePrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (contact.isMboteUser) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = PurplePrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = contact.phoneNumber,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedContactIds.remove(contact.id)
                                        else selectedContactIds.add(contact.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PurplePrimary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val selectedMembers = contacts.filter { selectedContactIds.contains(it.id) }
                            onConfirm(groupName, description, selectedMembers, selectedAvatarUrl, initialMessage)
                        },
                        enabled = groupName.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_create_group_button")
                    ) {
                        Text("Créer le groupe", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
