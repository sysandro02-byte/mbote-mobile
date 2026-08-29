package com.loukatech.mbote.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.loukatech.mbote.ui.components.AiCallPatternSummaryVisualizer
import com.loukatech.mbote.ui.components.AiCallerIdPremiumDialog
import com.loukatech.mbote.ui.components.AiSuggestedContactsSection
import com.loukatech.mbote.ui.components.AutoCallRecordingCard
import com.loukatech.mbote.ui.components.CallDetailSheetDialog
import com.loukatech.mbote.ui.components.CallMethodType
import com.loukatech.mbote.ui.components.CallRecordingItem
import com.loukatech.mbote.ui.components.CallRecordingListSection
import com.loukatech.mbote.ui.components.CallRecordingPlayerDialog
import com.loukatech.mbote.ui.components.CallStatsAnalyticsSection
import com.loukatech.mbote.ui.components.CallSummaryHeader
import com.loukatech.mbote.ui.components.CellularAndDeviceContactsBar
import com.loukatech.mbote.ui.components.DevicePhoneContactsDialog
import com.loukatech.mbote.ui.components.FilterChipRow
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft

enum class CallTabSection {
    HISTORY,
    DIALPAD,
    STATS,
    AI_SUGGESTIONS
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
    var showAiCallerIdDialog by remember { mutableStateOf(false) }
    var showDeviceContactsDialog by remember { mutableStateOf(false) }
    var selectedCallMethod by remember { mutableStateOf(CallMethodType.MBOTE_HD) }
    var dialedNumber by remember { mutableStateOf("") }

    var callList by remember { mutableStateOf(calls) }
    var callSettings by remember { mutableStateOf(CallSettings()) }
    var isAutoRecordCallsEnabled by remember { mutableStateOf(true) }
    var activeRecordingForPlayer by remember { mutableStateOf<CallRecordingItem?>(null) }
    var selectedCallForDetails by remember { mutableStateOf<CallItem?>(null) }

    // Helper function to handle calling via MBoté HD vs Cellular SIM
    val handleInitiateCall = { name: String, avatar: String, isVideo: Boolean, isCellular: Boolean ->
        if (isCellular || selectedCallMethod == CallMethodType.CELLULAR_SIM) {
            val phoneNum = if (name.startsWith("+") || name.any { it.isDigit() }) name else "+242 06 612 3456"
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNum.replace(" ", "")}"))
                context.startActivity(intent)
                Toast.makeText(context, "Composition sur le réseau cellulaire SIM pour $name...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lancement de l'appel SIM pour $name ($phoneNum)", Toast.LENGTH_SHORT).show()
            }
        } else {
            onStartCall(name, avatar, isVideo)
        }
    }

    var sampleRecordings by remember {
        mutableStateOf(
            listOf(
                CallRecordingItem(
                    id = "rec_1",
                    callerName = "Grace Makiese",
                    callerNumber = "+242 06 555 4321",
                    callerAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                    timestamp = "Aujourd'hui, 14:10",
                    durationText = "12:30",
                    fileSizeText = "14.2 MB",
                    isIncoming = true
                ),
                CallRecordingItem(
                    id = "rec_2",
                    callerName = "Tech Hub Brazzaville",
                    callerNumber = "+242 05 777 8899",
                    callerAvatar = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150&auto=format&fit=crop&q=80",
                    timestamp = "Hier, 18:45",
                    durationText = "34:12",
                    fileSizeText = "38.5 MB",
                    isIncoming = false
                ),
                CallRecordingItem(
                    id = "rec_3",
                    callerName = "Audrey Matondo",
                    callerNumber = "+242 06 888 9900",
                    callerAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                    timestamp = "21 Août, 20:05",
                    durationText = "19:02",
                    fileSizeText = "21.6 MB",
                    isIncoming = false
                )
            )
        )
    }

    LaunchedEffect(calls) {
        callList = calls
    }

    val filters = listOf("Tous", "Manqués")

    // Cleaned sanitized query for numeric matching
    val cleanDigitsQuery = remember(searchQuery) {
        searchQuery.filter { it.isDigit() || it == '+' }
    }

    // Filter past call logs by name or phone number
    val displayedCalls = remember(callList, selectedFilter, searchQuery, cleanDigitsQuery) {
        callList.filter { call ->
            val matchesFilter = if (selectedFilter == "Manqués") call.type == CallType.MISSED else true
            if (searchQuery.isBlank()) {
                matchesFilter
            } else {
                val callDigits = call.phoneNumber.filter { it.isDigit() || it == '+' }
                val matchesName = call.name.contains(searchQuery.trim(), ignoreCase = true)
                val matchesPhone = (cleanDigitsQuery.isNotBlank() && callDigits.contains(cleanDigitsQuery)) ||
                        call.phoneNumber.contains(searchQuery.trim(), ignoreCase = true)
                matchesFilter && (matchesName || matchesPhone)
            }
        }
    }

    // Filter synced contacts by name or phone number when searching
    val matchingContacts = remember(syncedContacts, searchQuery, cleanDigitsQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            syncedContacts.filter { contact ->
                val contactDigits = contact.phoneNumber.filter { it.isDigit() || it == '+' }
                val matchesName = contact.name.contains(searchQuery.trim(), ignoreCase = true)
                val matchesPhone = (cleanDigitsQuery.isNotBlank() && contactDigits.contains(cleanDigitsQuery)) ||
                        contact.phoneNumber.contains(searchQuery.trim(), ignoreCase = true)
                matchesName || matchesPhone
            }
        }
    }

    Scaffold(
        topBar = {
            // Search bar at the top of the Calls tab to quickly filter contacts and past call logs
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
                    // Search Bar (Filtered by Name or Phone Number)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Rechercher contacts ou numéros...",
                                fontSize = 14.sp,
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
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("calls_clear_search_button")
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Effacer la recherche",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

                    // 3-Dots Menu Button
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

                        // 3-Dots Menu Dropdown
                        DropdownMenu(
                            expanded = showHeaderMenu,
                            onDismissRequest = { showHeaderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("IA Caller ID Premium") },
                                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                onClick = {
                                    showHeaderMenu = false
                                    showAiCallerIdDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Contacts de l'appareil") },
                                leadingIcon = { Icon(Icons.Outlined.ContactPhone, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showHeaderMenu = false
                                    showDeviceContactsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Historique des appels") },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showHeaderMenu = false
                                    activeTab = CallTabSection.HISTORY
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Paramètres") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showHeaderMenu = false
                                    showSettingsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Aide et commentaires") },
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

                // Sub navigation / Segmented Tabs (Journal, Clavier, Statistiques, Suggestions IA)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        label = "Statistiques",
                        icon = Icons.Outlined.Analytics,
                        isSelected = activeTab == CallTabSection.STATS,
                        onClick = { activeTab = CallTabSection.STATS },
                        modifier = Modifier.weight(1.1f)
                    )
                    TabSegmentButton(
                        label = "Suggestions IA",
                        icon = Icons.Outlined.AutoAwesome,
                        isSelected = activeTab == CallTabSection.AI_SUGGESTIONS,
                        onClick = { activeTab = CallTabSection.AI_SUGGESTIONS },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        },
        floatingActionButton = {
            // Floating Keypad Button visible on History & Statistiques
            if (activeTab != CallTabSection.DIALPAD) {
                Surface(
                    onClick = { activeTab = CallTabSection.DIALPAD },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFD6E2FB),
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
                    // Call History & Search Filter Results View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        if (searchQuery.isBlank()) {
                            // Filter chips (Tous / Manqués)
                            item {
                                FilterChipRow(
                                    filters = filters,
                                    selectedFilter = selectedFilter,
                                    onSelectFilter = { selectedFilter = it },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        } else {
                            // Active Search Banner
                            item {
                                Surface(
                                    color = PurplePrimary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Recherche: « $searchQuery »",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PurplePrimary
                                        )
                                        Text(
                                            text = "${matchingContacts.size + displayedCalls.size} résultat(s)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Quick Direct Dial Card if user typed numbers/characters
                            if (searchQuery.isNotBlank()) {
                                item {
                                    Surface(
                                        onClick = {
                                            handleInitiateCall(
                                                searchQuery.trim(),
                                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                                                false,
                                                selectedCallMethod == CallMethodType.CELLULAR_SIM
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .testTag("direct_call_search_card")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(if (selectedCallMethod == CallMethodType.CELLULAR_SIM) Color(0xFF10B981) else PurplePrimary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (selectedCallMethod == CallMethodType.CELLULAR_SIM) Icons.Outlined.SimCard else Icons.Default.Call,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = if (selectedCallMethod == CallMethodType.CELLULAR_SIM) "Appeler via Réseau SIM" else "Appeler MBoté HD",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = searchQuery.trim(),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        handleInitiateCall(
                                                            searchQuery.trim(),
                                                            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                                                            false,
                                                            false
                                                        )
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Call,
                                                        contentDescription = "Appel audio HD",
                                                        tint = PurplePrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        handleInitiateCall(
                                                            searchQuery.trim(),
                                                            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                                                            false,
                                                            true
                                                        )
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.SimCard,
                                                        contentDescription = "Appel SIM GSM",
                                                        tint = Color(0xFF10B981),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 1. Matching Contacts Section
                            if (matchingContacts.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "CONTACTS TROUVÉS (${matchingContacts.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary,
                                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                                    )
                                }

                                items(matchingContacts, key = { "contact_${it.id}" }) { contact ->
                                    SearchContactRow(
                                        contact = contact,
                                        onStartAudioCall = {
                                            handleInitiateCall(contact.name, contact.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false, selectedCallMethod == CallMethodType.CELLULAR_SIM)
                                        },
                                        onStartVideoCall = {
                                            handleInitiateCall(contact.name, contact.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", true, false)
                                        },
                                        onOpenChat = { onOpenChat(contact.name) }
                                    )
                                }
                            }
                        }

                        // 2. Matching Past Call Logs Section
                        if (displayedCalls.isNotEmpty()) {
                            if (searchQuery.isNotBlank()) {
                                item {
                                    Text(
                                        text = "JOURNAL DES APPELS (${displayedCalls.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary,
                                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            items(displayedCalls, key = { "call_${it.id}" }) { call ->
                                SwipeableCallItemRow(
                                    call = call,
                                    onRedial = { isVideo -> handleInitiateCall(call.name, call.avatar, isVideo, selectedCallMethod == CallMethodType.CELLULAR_SIM) },
                                    onOpenChat = { onOpenChat(call.name) },
                                    onDeleteCall = {
                                        callList = callList.filterNot { it.id == call.id }
                                        Toast.makeText(context, "Appel supprimé du journal", Toast.LENGTH_SHORT).show()
                                    },
                                    onPlayRecording = {
                                        val rec = sampleRecordings.firstOrNull { it.callerName.equals(call.name, ignoreCase = true) }
                                            ?: CallRecordingItem(
                                                id = "rec_${call.id}",
                                                callerName = call.name,
                                                callerNumber = call.phoneNumber,
                                                callerAvatar = call.avatar,
                                                timestamp = call.timestamp,
                                                durationText = call.durationText.ifBlank { "04:12" },
                                                fileSizeText = "4.2 MB",
                                                isIncoming = call.type == CallType.INCOMING
                                            )
                                        activeRecordingForPlayer = rec
                                    },
                                    onOpenDetails = {
                                        selectedCallForDetails = call
                                    }
                                )
                            }
                        }

                        // Empty State (When no call logs and no contacts match)
                        if (displayedCalls.isEmpty() && matchingContacts.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.PhoneMissed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Aucun contact ni appel pour \"$searchQuery\"" else "Aucun appel récent",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Vérifiez l'orthographe du nom ou du numéro" else "Vos appels récents s'afficheront ici",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (searchQuery.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                dialedNumber = searchQuery.trim()
                                                activeTab = CallTabSection.DIALPAD
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                                        ) {
                                            Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Composer sur le clavier")
                                        }
                                    }
                                }
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
                            handleInitiateCall(
                                number,
                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                                isVideo,
                                selectedCallMethod == CallMethodType.CELLULAR_SIM
                            )
                        },
                        contacts = syncedContacts,
                        onSelectContact = { contact ->
                            handleInitiateCall(contact.name, contact.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false, selectedCallMethod == CallMethodType.CELLULAR_SIM)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                CallTabSection.STATS -> {
                    // Toggleable Statistics View & Automatic Call Recording Manager
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        // 1. Interaction Statistics Summary Header & Most Contacted Carousel
                        item {
                            CallSummaryHeader(
                                calls = callList,
                                syncedContacts = syncedContacts,
                                selectedFilter = selectedFilter,
                                onSelectFilter = { selectedFilter = it },
                                onStartCall = { name, avatar, isVideo ->
                                    handleInitiateCall(name, avatar, isVideo, selectedCallMethod == CallMethodType.CELLULAR_SIM)
                                }
                            )
                        }

                        // 2. Automatic Call Recording Feature Card (Toggle incoming/outgoing)
                        item {
                            AutoCallRecordingCard(
                                isEnabled = isAutoRecordCallsEnabled,
                                onToggle = { isAutoRecordCallsEnabled = it }
                            )
                        }

                        // 3. Analytics duration & peak hours section
                        item {
                            CallStatsAnalyticsSection(calls = callList)
                        }

                        // 4. Saved Call Recordings List
                        item {
                            CallRecordingListSection(
                                recordings = sampleRecordings,
                                onPlayRecording = { rec ->
                                    activeRecordingForPlayer = rec
                                },
                                onDeleteRecording = { recId ->
                                    sampleRecordings = sampleRecordings.filterNot { it.id == recId }
                                    Toast.makeText(context, "Enregistrement supprimé", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                CallTabSection.AI_SUGGESTIONS -> {
                    // Dedicated AI Suggestions Tab View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        // AI Visual Call History Pattern Visualizer & Peak Times
                        item {
                            AiCallPatternSummaryVisualizer(calls = callList)
                        }

                        // AI-driven suggested contacts carousel
                        item {
                            AiSuggestedContactsSection(
                                calls = callList,
                                syncedContacts = syncedContacts,
                                onStartCall = { name, avatar, isVideo, isCellular ->
                                    handleInitiateCall(name, avatar, isVideo, isCellular)
                                }
                            )
                        }

                        // Telecom Cellular SIM & Phonebook Contacts Access Card
                        item {
                            CellularAndDeviceContactsBar(
                                selectedCallMethod = selectedCallMethod,
                                onSelectCallMethod = { selectedCallMethod = it },
                                onOpenDeviceContactsDialog = { showDeviceContactsDialog = true },
                                onOpenAiCallerIdDialog = { showAiCallerIdDialog = true }
                            )
                        }
                    }
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

    // AI Caller ID Premium Dialog
    if (showAiCallerIdDialog) {
        AiCallerIdPremiumDialog(
            onDismiss = { showAiCallerIdDialog = false },
            onStartCall = { name, number, isCellular ->
                handleInitiateCall(name, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false, isCellular)
            }
        )
    }

    // Device Phone Contacts Picker Dialog
    if (showDeviceContactsDialog) {
        DevicePhoneContactsDialog(
            onDismiss = { showDeviceContactsDialog = false },
            onStartCall = { name, avatar, isVideo, isCellular ->
                handleInitiateCall(name, avatar, isVideo, isCellular)
            }
        )
    }

    // Active Recording Player Modal Dialog
    activeRecordingForPlayer?.let { recording ->
        CallRecordingPlayerDialog(
            recording = recording,
            onDismiss = { activeRecordingForPlayer = null },
            onCallBack = {
                handleInitiateCall(recording.callerName, recording.callerAvatar, false, selectedCallMethod == CallMethodType.CELLULAR_SIM)
            }
        )
    }

    // Call Details & Local Saved Recordings Management Dialog
    selectedCallForDetails?.let { selectedCall ->
        CallDetailSheetDialog(
            call = selectedCall,
            recordings = sampleRecordings.filter { rec ->
                rec.callerName.contains(selectedCall.name, ignoreCase = true) ||
                selectedCall.name.contains(rec.callerName, ignoreCase = true)
            }.ifEmpty { sampleRecordings },
            onDismiss = { selectedCallForDetails = null },
            onStartCall = { isVideo ->
                handleInitiateCall(selectedCall.name, selectedCall.avatar, isVideo, selectedCallMethod == CallMethodType.CELLULAR_SIM)
            },
            onOpenChat = {
                onOpenChat(selectedCall.name)
            },
            onUpdateRecordingName = { recId, newName ->
                sampleRecordings = sampleRecordings.map {
                    if (it.id == recId) it.copy(customName = newName) else it
                }
            },
            onDeleteRecording = { recId ->
                sampleRecordings = sampleRecordings.filterNot { it.id == recId }
                Toast.makeText(context, "Enregistrement supprimé", Toast.LENGTH_SHORT).show()
            },
            onPlayRecording = { rec ->
                activeRecordingForPlayer = rec
            }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchContactRow(
    contact: SyncedContact,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onOpenChat: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartAudioCall() }
            .testTag("search_contact_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(PurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                if (!contact.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = contact.avatarUrl,
                        contentDescription = contact.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (contact.isMboteUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = PurplePrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "MBoté",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phoneNumber,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onStartAudioCall,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Appel audio",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onStartVideoCall,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Appel vidéo HD",
                        tint = PurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenChat,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = "Discuter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallItemRow(
    call: CallItem,
    onRedial: (isVideo: Boolean) -> Unit,
    onOpenChat: () -> Unit = {},
    onDeleteCall: () -> Unit,
    onPlayRecording: () -> Unit = {},
    onOpenDetails: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCallItemMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = call.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (call.type == CallType.MISSED) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                    if (call.isVideo) {
                        Surface(
                            color = PurplePrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "HD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Direction, Phone and Timestamp
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${call.phoneNumber} • ${call.timestamp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Prominent Call Duration & Type Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (call.type) {
                        CallType.MISSED -> {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Non abouti (Manqué)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                        CallType.INCOMING -> {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Durée: ${call.durationText}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        }
                        CallType.OUTGOING -> {
                            Surface(
                                color = PurplePrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = PurplePrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Durée: ${call.durationText}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PurplePrimary
                                    )
                                }
                            }
                        }
                    }
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
                    DropdownMenuItem(
                        text = { Text("Écouter l'enregistrement") },
                        leadingIcon = { Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = PurplePrimary) },
                        onClick = {
                            showCallItemMenu = false
                            onPlayRecording()
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
                            Toast.makeText(context, "${call.name} a été bloqué(e)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Call Item Row wrapper supporting Swipe Right (Call back) & Swipe Left (Delete)
 */
@Composable
fun SwipeableCallItemRow(
    call: CallItem,
    onRedial: (isVideo: Boolean) -> Unit,
    onOpenChat: () -> Unit = {},
    onDeleteCall: () -> Unit,
    onPlayRecording: () -> Unit = {},
    onOpenDetails: () -> Unit = {}
) {
    val context = LocalContext.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipePx = 280f
    val triggerThreshold = 130f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    offsetX > 20f -> Color(0xFF10B981)
                    offsetX < -20f -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
    ) {
        // Swipe Right Action (Call back)
        if (offsetX > 20f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Rappeler",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rappeler",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Swipe Left Action (Delete)
        if (offsetX < -20f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supprimer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Draggable Row
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > triggerThreshold) {
                                onRedial(call.isVideo)
                                Toast.makeText(context, "Rappel de ${call.name}...", Toast.LENGTH_SHORT).show()
                            } else if (offsetX < -triggerThreshold) {
                                onDeleteCall()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = offsetX + dragAmount
                            offsetX = newOffset.coerceIn(-maxSwipePx, maxSwipePx)
                        }
                    )
                }
        ) {
            CallItemRow(
                call = call,
                onRedial = onRedial,
                onOpenChat = onOpenChat,
                onDeleteCall = onDeleteCall,
                onPlayRecording = onPlayRecording,
                onOpenDetails = onOpenDetails
            )
        }
    }
}
