package com.loukatech.mbote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loukatech.mbote.model.SyncedContact

data class FriendRequest(
    val id: String,
    val name: String,
    val phone: String,
    val avatarUrl: String?,
    val isIncoming: Boolean, // true = request received, false = request sent
    val timestamp: String = "Il y a 2h"
)

/**
 * Masta (Amis & Contacts MBoté) Management Hub:
 * - Fully supports Light & Dark themes
 * - Tabs: "Mes Masta" and "Demandes d'amitié" (Sent & Received)
 * - Search & Fast Add new Masta buddy or send friend request
 */
@Composable
fun MastaHubSheet(
    contacts: List<SyncedContact>,
    onDismiss: () -> Unit,
    onStartChat: (SyncedContact) -> Unit,
    onVoiceCall: (SyncedContact) -> Unit,
    onVideoCall: (SyncedContact) -> Unit,
    onAddNewMasta: (name: String, phone: String, isMboteUser: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddMastaDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Mes Masta") } // "Mes Masta", "Demandes"
    var selectedFilter by remember { mutableStateOf("Tous") } // "Tous", "Sur MBoté", "En ligne"

    // Initial Friend Requests state (Received & Sent)
    val friendRequests = remember {
        mutableStateListOf(
            FriendRequest(
                id = "fr_1",
                name = "Kevine Moundele",
                phone = "+242 06 987 65 43",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                isIncoming = true,
                timestamp = "Aujourd'hui à 14:20"
            ),
            FriendRequest(
                id = "fr_2",
                name = "Glodi Mavoungou",
                phone = "+243 81 234 56 78",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                isIncoming = true,
                timestamp = "Hier à 18:45"
            ),
            FriendRequest(
                id = "fr_3",
                name = "Grâce Kamba",
                phone = "+242 05 112 23 34",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                isIncoming = false,
                timestamp = "Envoyée il y a 3h"
            ),
            FriendRequest(
                id = "fr_4",
                name = "Chancel Mbemba",
                phone = "+243 99 888 77 66",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                isIncoming = false,
                timestamp = "Envoyée hier"
            )
        )
    }

    val incomingCount = friendRequests.count { it.isIncoming }

    val filteredContacts = remember(contacts, searchQuery, selectedFilter) {
        contacts.filter { contact ->
            val matchesQuery = searchQuery.isBlank() ||
                    contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.phoneNumber.contains(searchQuery)
            val matchesFilter = when (selectedFilter) {
                "Sur MBoté" -> contact.isMboteUser
                "En ligne" -> contact.isMboteUser
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    if (showAddMastaDialog) {
        AddMastaDialog(
            onDismiss = { showAddMastaDialog = false },
            onConfirm = { name, phone, isMboteUser ->
                onAddNewMasta(name, phone, isMboteUser)
                // Also add to sent friend requests
                friendRequests.add(
                    FriendRequest(
                        id = "fr_${System.currentTimeMillis()}",
                        name = name,
                        phone = phone,
                        avatarUrl = null,
                        isIncoming = false,
                        timestamp = "À l'instant"
                    )
                )
                showAddMastaDialog = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 16.dp,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
                .testTag("masta_hub_dialog")
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
                                imageVector = Icons.Outlined.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Masta (Mes Amis)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${contacts.count { it.isMboteUser }} Masta sur MBoté",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
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
                            .testTag("close_masta_hub")
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

                // Navigation Tabs: Mes Masta vs Demandes d'amitié
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Mes Masta", "Demandes d'amitié").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tab == "Demandes d'amitié" && incomingCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = incomingCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == "Mes Masta") {
                    // Search & Add Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher un Masta...") },
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
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Effacer",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { showAddMastaDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("add_masta_button")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ajouter", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips: Tous / Sur MBoté / En ligne
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tous", "Sur MBoté", "En ligne").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { selectedFilter = filter }
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contacts List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            MastaContactCard(
                                contact = contact,
                                onChatClick = {
                                    onDismiss()
                                    onStartChat(contact)
                                },
                                onVoiceClick = {
                                    onDismiss()
                                    onVoiceCall(contact)
                                },
                                onVideoClick = {
                                    onDismiss()
                                    onVideoCall(contact)
                                }
                            )
                        }
                    }
                } else {
                    // Demandes d'amitié tab content (Received & Sent)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section 1: Demandes reçues
                            val incomingRequests = friendRequests.filter { it.isIncoming }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Demandes reçues (${incomingRequests.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (incomingRequests.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Aucune demande d'amitié en attente",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(incomingRequests, key = { it.id }) { req ->
                                    FriendRequestCard(
                                        request = req,
                                        onAccept = {
                                            // Add to contacts and remove from requests
                                            onAddNewMasta(req.name, req.phone, true)
                                            friendRequests.remove(req)
                                        },
                                        onDecline = {
                                            friendRequests.remove(req)
                                        }
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(12.dp)) }

                            // Section 2: Demandes envoyées
                            val outgoingRequests = friendRequests.filter { !it.isIncoming }
                            item {
                                Text(
                                    text = "Demandes envoyées (${outgoingRequests.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (outgoingRequests.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Vous n'avez envoyé aucune demande",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(outgoingRequests, key = { it.id }) { req ->
                                    FriendRequestCard(
                                        request = req,
                                        onAccept = {},
                                        onDecline = {
                                            friendRequests.remove(req)
                                        }
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

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (request.avatarUrl != null) {
                    AsyncImage(
                        model = request.avatarUrl,
                        contentDescription = request.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = request.name.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (request.isIncoming) "Souhaite vous ajouter comme Masta" else "Demande d'amitié envoyée • ${request.timestamp}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (request.isIncoming) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Refuser",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Accepter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Annuler", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MastaContactCard(
    contact: SyncedContact,
    onChatClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
            .testTag("masta_card_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online status badge
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }

                if (contact.isMboteUser) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (contact.isMboteUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Vérifié",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = contact.phoneNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = contact.statusText ?: if (contact.isMboteUser) "Masta actif" else "Inviter sur MBoté",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (contact.isMboteUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Quick Call & Chat Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Message",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (contact.isMboteUser) {
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = "Appel audio",
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onVideoClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Outlined.Videocam,
                            contentDescription = "Appel vidéo",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddMastaDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, isMboteUser: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phonePrefix by remember { mutableStateOf("+242") }
    var phoneNumber by remember { mutableStateOf("") }

    val countryPrefixes = listOf(
        "+242" to "🇨🇬 Congo",
        "+243" to "🇨🇩 RDC",
        "+33" to "🇫🇷 France",
        "+1" to "🇺🇸 USA"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 16.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("add_masta_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "Ajouter un Masta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Envoyez une demande d'amitié",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet ou pseudo *") },
                    placeholder = { Text("ex. Grace Ondongo") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Country Prefix Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Indicatif du pays :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        countryPrefixes.forEach { (prefix, label) ->
                            val isSelected = phonePrefix == prefix
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { phonePrefix = prefix }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = prefix,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Phone Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { c -> c.isDigit() || c == ' ' } },
                    label = { Text("Numéro de téléphone *") },
                    prefix = { Text("$phonePrefix ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    placeholder = { Text("06 123 4567") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val fullPhone = "$phonePrefix ${phoneNumber.trim()}"
                            onConfirm(name, fullPhone, true)
                        },
                        enabled = name.isNotBlank() && phoneNumber.length >= 6,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Ajouter", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
