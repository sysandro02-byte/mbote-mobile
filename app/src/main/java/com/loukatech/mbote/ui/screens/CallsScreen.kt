package com.loukatech.mbote.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import coil.compose.AsyncImage
import com.loukatech.mbote.model.CallItem
import com.loukatech.mbote.model.CallType
import com.loukatech.mbote.ui.components.FilterChipRow
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft

@Composable
fun CallsScreen(
    calls: List<CallItem>,
    onStartCall: (name: String, avatar: String, isVideo: Boolean) -> Unit,
    onOpenChat: (name: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(searchQuery.isNotBlank()) }
    var showHeaderMenu by remember { mutableStateOf(false) }
    var callList by remember { mutableStateOf(calls) }

    // Keep callList synced if external list changes
    LaunchedEffect(calls) {
        if (calls.isNotEmpty() && callList.isEmpty()) {
            callList = calls
        }
    }

    val filters = listOf("Tous", "Manqués")

    val displayedCalls = remember(callList, selectedFilter, searchQuery) {
        callList.filter { call ->
            val matchesFilter = if (selectedFilter == "Manqués") call.type == CallType.MISSED else true
            val matchesQuery = searchQuery.isBlank() || call.name.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Header Row with Title, 3-dots Menu, and Search Loupe Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Journal des appels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Appels vocaux et vidéo HD chiffrés",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Header Action Buttons: Search Loupe Toggle + 3-Dots Header Menu
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Loupe Toggle Button (placed BEFORE 3-dots icon)
                        Surface(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible && searchQuery.isNotBlank()) {
                                    searchQuery = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSearchVisible || searchQuery.isNotBlank()) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("calls_search_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Afficher la recherche",
                                    tint = if (isSearchVisible || searchQuery.isNotBlank()) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 3-Dots Header Menu
                        Box {
                            Surface(
                                onClick = { showHeaderMenu = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("calls_header_menu")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options du journal",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showHeaderMenu,
                                onDismissRequest = { showHeaderMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Lancer une réunion HD") },
                                    leadingIcon = { Icon(Icons.Default.VideoCall, contentDescription = null, tint = PurplePrimary) },
                                    onClick = {
                                        showHeaderMenu = false
                                        onStartCall("Réunion MBoté Group", "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150", true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Effacer tout le journal") },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                    onClick = {
                                        showHeaderMenu = false
                                        callList = emptyList()
                                        Toast.makeText(context, "Journal des appels effacé", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Paramètres des appels") },
                                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                    onClick = {
                                        showHeaderMenu = false
                                        Toast.makeText(context, "Qualité vidéo HD & Chiffrement E2E activés", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar for Calls (Appears conditionally with animation below header)
            item {
                AnimatedVisibility(
                    visible = isSearchVisible || searchQuery.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher un contact ou un numéro...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = PurplePrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = PurplePrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                FilterChipRow(
                    filters = filters,
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (displayedCalls.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneMissed,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Aucun résultat pour \"$searchQuery\"" else "Aucun appel dans cette catégorie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                items(displayedCalls, key = { it.id }) { call ->
                    CallItemRow(
                        call = call,
                        onRedial = { isVideo -> onStartCall(call.name, call.avatar, isVideo) },
                        onOpenChat = { onOpenChat(call.name) },
                        onDeleteCall = {
                            callList = callList.filterNot { it.id == call.id }
                            Toast.makeText(context, "Appel supprimé du journal", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // FAB to start a quick call
        FloatingActionButton(
            onClick = {
                onStartCall(
                    "Grace Makiese",
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                    false
                )
            },
            containerColor = PurplePrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 20.dp)
                .testTag("fab_new_call")
        ) {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = "Nouvel appel"
            )
        }
    }
}

@Composable
fun CallItemRow(
    call: CallItem,
    onRedial: (isVideo: Boolean) -> Unit,
    onOpenChat: () -> Unit = {},
    onDeleteCall: () -> Unit
) {
    val context = LocalContext.current
    var showCallItemMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRedial(call.isVideo) }
            .testTag("call_item_${call.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(PurpleSoft)
            ) {
                AsyncImage(
                    model = call.avatar,
                    contentDescription = call.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (call.type == CallType.MISSED) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (call.type) {
                        CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF10B981)
                        CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to PurplePrimary
                        CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFEF4444)
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${call.timestamp} • ${call.durationText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Direct Call / Videocam Button
            IconButton(
                onClick = { onRedial(call.isVideo) },
                modifier = Modifier.testTag("redial_button_${call.id}")
            ) {
                Icon(
                    imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Rappeler",
                    tint = PurplePrimary
                )
            }

            // 3-Dots Item Menu according to requirements 3 & 7
            Box {
                IconButton(
                    onClick = { showCallItemMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("call_item_more_${call.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options de l'appel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showCallItemMenu,
                    onDismissRequest = { showCallItemMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rappeler (Vocal)") },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = PurplePrimary) },
                        onClick = {
                            showCallItemMenu = false
                            onRedial(false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Appel Vidéo HD") },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = PurplePrimary) },
                        onClick = {
                            showCallItemMenu = false
                            onRedial(true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Envoyer un message") },
                        leadingIcon = { Icon(Icons.Outlined.Chat, contentDescription = null) },
                        onClick = {
                            showCallItemMenu = false
                            onOpenChat()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Supprimer de l'historique") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                        onClick = {
                            showCallItemMenu = false
                            onDeleteCall()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bloquer ce contact") },
                        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null, tint = Color(0xFFEF4444)) },
                        onClick = {
                            showCallItemMenu = false
                            Toast.makeText(context, "${call.name} à été bloqué(e)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
