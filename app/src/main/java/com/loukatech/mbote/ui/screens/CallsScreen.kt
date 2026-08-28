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
import com.loukatech.mbote.model.CallSettings
import com.loukatech.mbote.model.CallType
import com.loukatech.mbote.model.SyncedContact
import com.loukatech.mbote.model.VoicemailItem
import com.loukatech.mbote.ui.components.CallDialpadView
import com.loukatech.mbote.ui.components.CallHelpFeedbackDialog
import com.loukatech.mbote.ui.components.CallSettingsDialog
import com.loukatech.mbote.ui.components.FilterChipRow
import com.loukatech.mbote.ui.components.VisualVoicemailView
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft

enum class CallTabSection {
    HISTORY,
    DIALPAD,
    VOICEMAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    calls: List<CallItem>,
    onStartCall: (name: String, avatar: String, isVideo: Boolean) -> Unit,
    onOpenChat: (name: String) -> Unit = {},
    syncedContacts: List<SyncedContact> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(CallTabSection.HISTORY) }
    var selectedFilter by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var showHeaderMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHelpFeedbackDialog by remember { mutableStateOf(false) }
    var dialedNumber by remember { mutableStateOf("") }

    var callList by remember { mutableStateOf(calls) }
    var callSettings by remember { mutableStateOf(CallSettings()) }
    var isVisualVoicemailActive by remember { mutableStateOf(false) }

    var sampleVoicemails by remember {
        mutableStateOf(
            listOf(
                VoicemailItem(
                    callerName = "Grace Makiese",
                    callerNumber = "+242 06 555 4321",
                    callerAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                    timestamp = "Aujourd'hui, 14:32",
                    durationSeconds = 34,
                    transcription = "Bonjour Marc, je te rappelle concernant la réunion de projet MBoté. Fais-moi signe dès que tu as 5 minutes !",
                    isImportant = true
                ),
                VoicemailItem(
                    callerName = "Aron Loutala",
                    callerNumber = "+242 06 111 2233",
                    callerAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                    timestamp = "Hier, 18:10",
                    durationSeconds = 52,
                    transcription = "Salut Marc, les serveurs audio WebRTC sont prêts et validés pour Brazzaville et Kinshasa.",
                    isImportant = false
                )
            )
        )
    }

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

    Scaffold(
        topBar = {
            // Modern Top Search Bar matching Image 2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Contacts Bar (Image 2 style)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search contacts",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = PurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PurplePrimary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("calls_search_input")
                    )

                    // 3-Dots Menu Button matching Image 2
                    Box {
                        IconButton(
                            onClick = { showHeaderMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("calls_overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3-Dots Menu Dropdown matching Image 2 options
                        DropdownMenu(
                            expanded = showHeaderMenu,
                            onDismissRequest = { showHeaderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Call history") },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showHeaderMenu = false
                                    activeTab = CallTabSection.HISTORY
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showHeaderMenu = false
                                    showSettingsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Help & feedback") },
                                leadingIcon = { Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showHeaderMenu = false
                                    showHelpFeedbackDialog = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Lancer une réunion HD") },
                                leadingIcon = { Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    showHeaderMenu = false
                                    onStartCall("Réunion MBoté HD", "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150", true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Effacer le journal") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showHeaderMenu = false
                                    callList = emptyList()
                                    Toast.makeText(context, "Journal des appels effacé", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Sub navigation / Segmented Tabs (Historique, Clavier, Messagerie)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabSegmentButton(
                        label = "Journal",
                        icon = Icons.Outlined.History,
                        isSelected = activeTab == CallTabSection.HISTORY,
                        onClick = { activeTab = CallTabSection.HISTORY },
                        modifier = Modifier.weight(1f)
                    )
                    TabSegmentButton(
                        label = "Clavier",
                        icon = Icons.Default.Dialpad,
                        isSelected = activeTab == CallTabSection.DIALPAD,
                        onClick = { activeTab = CallTabSection.DIALPAD },
                        modifier = Modifier.weight(1f)
                    )
                    TabSegmentButton(
                        label = "Messagerie",
                        icon = Icons.Default.Voicemail,
                        isSelected = activeTab == CallTabSection.VOICEMAIL,
                        onClick = { activeTab = CallTabSection.VOICEMAIL },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            // Floating Keypad Button like in Image 2 (visible on History & Voicemail)
            if (activeTab != CallTabSection.DIALPAD) {
                Surface(
                    onClick = { activeTab = CallTabSection.DIALPAD },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFD6E2FB), // Soft modern blue/lavender tint as in image 2
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(bottom = 72.dp)
                        .size(56.dp)
                        .testTag("fab_keypad_toggle")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Ouvrir le clavier d'appel",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                CallTabSection.HISTORY -> {
                    // Call History View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        item {
                            FilterChipRow(
                                filters = filters,
                                selectedFilter = selectedFilter,
                                onSelectFilter = { selectedFilter = it },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (displayedCalls.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneMissed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Aucun résultat pour \"$searchQuery\"" else "Aucun appel récent",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Vos appels récents s'afficheront ici",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                }

                CallTabSection.DIALPAD -> {
                    // Dialpad matching Image 1
                    CallDialpadView(
                        dialedNumber = dialedNumber,
                        onNumberChange = { dialedNumber = it },
                        onStartCall = { number, isVideo ->
                            onStartCall(
                                number,
                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                                isVideo
                            )
                        },
                        contacts = syncedContacts,
                        onSelectContact = { contact ->
                            onStartCall(contact.name, contact.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                CallTabSection.VOICEMAIL -> {
                    // Visual Voicemail matching Image 2
                    VisualVoicemailView(
                        isVisualVoicemailActive = isVisualVoicemailActive,
                        onCallVoicemail = {
                            onStartCall("Messagerie Vocale (123)", "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150", false)
                        },
                        onToggleActiveState = { isVisualVoicemailActive = it },
                        voicemails = sampleVoicemails,
                        onDeleteVoicemail = { id ->
                            sampleVoicemails = sampleVoicemails.filterNot { it.id == id }
                            Toast.makeText(context, "Message vocal supprimé", Toast.LENGTH_SHORT).show()
                        },
                        onCallBack = { name, number ->
                            onStartCall(name, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Call Settings Screen/Dialog (Image 3)
    if (showSettingsDialog) {
        CallSettingsDialog(
            callSettings = callSettings,
            onUpdateSettings = { callSettings = it },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Help & Feedback Dialog
    if (showHelpFeedbackDialog) {
        CallHelpFeedbackDialog(
            onDismiss = { showHelpFeedbackDialog = false }
        )
    }
}

@Composable
fun TabSegmentButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isSelected) PurplePrimary else Color.Transparent),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
                    .size(48.dp)
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

            // 3-Dots Item Menu
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
