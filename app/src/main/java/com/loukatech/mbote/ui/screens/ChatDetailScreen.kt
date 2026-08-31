package com.loukatech.mbote.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.loukatech.mbote.model.*
import com.loukatech.mbote.service.AudioRecorderManager
import com.loukatech.mbote.service.ChatExportManager
import com.loukatech.mbote.service.ChatExportResult
import com.loukatech.mbote.ui.components.AudioPlaybackWaveform
import com.loukatech.mbote.ui.components.ChatExportDialog
import com.loukatech.mbote.ui.components.ChatSettingsDialog
import com.loukatech.mbote.ui.components.VoiceRecordingInputBar
import com.loukatech.mbote.ui.components.generateStaticWaveform
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.graphicsLayer
import com.loukatech.mbote.ui.components.FullScreenMediaViewerDialog
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FullScreenMediaData(
    val url: String,
    val isVideo: Boolean,
    val senderName: String,
    val timestamp: String,
    val caption: String,
    val msgId: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chat: Chat,
    currentUserAvatar: String = "",
    isPartnerTyping: Boolean = false,
    isBlocked: Boolean = false,
    isFriend: Boolean = true,
    onBackClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onSendMessage: (String, Message?) -> Unit,
    onSendVoiceMessage: (audioPath: String, durationSec: Int, replyTo: Message?) -> Unit = { _, _, _ -> },
    onSendMediaMessage: (mediaUrl: String, isVideo: Boolean, caption: String) -> Unit = { _, _, _ -> },
    onToggleBlock: (Boolean) -> Unit = {},
    onReaction: (messageId: String, emoji: String) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onOpenAronQuestions: () -> Unit = {},
    onOpenPollDialog: () -> Unit = {},
    onOpenLocationPicker: () -> Unit = {},
    onOpenPaymentSheet: () -> Unit = {},
    onVotePoll: (messageId: String, optionId: String) -> Unit = { _, _ -> },
    onTranslateMessage: (messageId: String, targetLang: String) -> Unit = { _, _ -> },
    onSetDisappearingTimer: (seconds: Int) -> Unit = {},
    onUpdateWallpaper: (colorHex: String?, imageUrl: String?) -> Unit = { _, _ -> },
    onUserComposing: (isComposing: Boolean) -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var isRecordingActive by remember { mutableStateOf(false) }
    var showAttachmentsMenu by remember { mutableStateOf(false) }
    var showDisappearingMenu by remember { mutableStateOf(false) }
    var showChatSettingsDialog by remember { mutableStateOf(false) }
    var showInviteLinkDialog by remember { mutableStateOf(false) }
    var showMicPermissionRationale by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var currentExportResult by remember { mutableStateOf<ChatExportResult?>(null) }
    var showLunaVoiceCommandSheet by remember { mutableStateOf(false) }

    // Media Viewer & In-Chat Search State
    var activeFullScreenMedia by remember { mutableStateOf<FullScreenMediaData?>(null) }
    var isSearchingInChat by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var showMediaPickerSheet by remember { mutableStateOf(false) }
    var mediaPickerIsVideo by remember { mutableStateOf(false) }
    var showEmojiGifPanel by remember { mutableStateOf(false) }
    var emojiGifTab by remember { mutableIntStateOf(0) } // 0: Emojis, 1: GIFs

    val recorderManager = remember { AudioRecorderManager(context) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isChannelOwner = remember(chat) {
        chat.isChannel && (
            chat.participants.any { it.role == "Propriétaire" || it.id == "me" } ||
            chat.name.contains("Ma Chaîne", ignoreCase = true) ||
            chat.name.contains("Official", ignoreCase = true) ||
            chat.name.contains("Louka", ignoreCase = true)
        )
    }

    val matchedMessageIndices = remember(chat.messages, inChatSearchQuery) {
        if (inChatSearchQuery.isBlank()) emptyList<Int>()
        else chat.messages.mapIndexedNotNull { index, msg ->
            if (msg.text.contains(inChatSearchQuery, ignoreCase = true) ||
                msg.senderName.contains(inChatSearchQuery, ignoreCase = true)) index else null
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = recorderManager.startRecording()
            if (started) {
                isRecordingActive = true
            }
        } else {
            showMicPermissionRationale = true
        }
    }

    if (showMicPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showMicPermissionRationale = false },
            icon = { Icon(Icons.Default.Mic, contentDescription = null, tint = MbotePurplePrimary) },
            title = { Text("Autoriser le microphone", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Pour enregistrer un message vocal chiffré sur MBoté, l'application a besoin d'accéder au microphone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMicPermissionRationale = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionRationale = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Smart Reply suggestions state powered by Gemini
    var smartReplies by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingReplies by remember { mutableStateOf(false) }

    LaunchedEffect(chat.messages) {
        val lastMsg = chat.messages.lastOrNull()
        if (lastMsg != null && !lastMsg.isMine) {
            isLoadingReplies = true
            smartReplies = com.loukatech.mbote.service.GeminiService.getSmartReplies(chat.messages)
            isLoadingReplies = false
        } else {
            smartReplies = emptyList()
        }
    }

    LaunchedEffect(chat.messages.size) {
        if (chat.messages.isNotEmpty()) {
            listState.animateScrollToItem(chat.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSearchingInChat) {
                    // Interactive In-Chat Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSearchingInChat = false
                            inChatSearchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Fermer la recherche",
                                tint = MbotePurplePrimary
                            )
                        }

                        TextField(
                            value = inChatSearchQuery,
                            onValueChange = {
                                inChatSearchQuery = it
                                currentMatchIndex = 0
                                if (it.isNotBlank()) {
                                    val matches = chat.messages.mapIndexedNotNull { index, msg ->
                                        if (msg.text.contains(it, ignoreCase = true) || msg.senderName.contains(it, ignoreCase = true)) index else null
                                    }
                                    if (matches.isNotEmpty()) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(matches.last())
                                        }
                                    }
                                }
                            },
                            placeholder = { Text("Rechercher dans cette discussion…", fontSize = 13.5.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MbotePurplePrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("in_chat_search_input")
                        )

                        if (inChatSearchQuery.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MbotePurpleSoft,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "${matchedMessageIndices.size} trouvé(s)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MbotePurplePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (matchedMessageIndices.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (matchedMessageIndices.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex - 1 + matchedMessageIndices.size) % matchedMessageIndices.size
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(matchedMessageIndices[currentMatchIndex])
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Précédent", tint = MbotePurplePrimary)
                                }

                                IconButton(
                                    onClick = {
                                        if (matchedMessageIndices.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex + 1) % matchedMessageIndices.size
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(matchedMessageIndices[currentMatchIndex])
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Suivant", tint = MbotePurplePrimary)
                                }
                            }

                            IconButton(onClick = { inChatSearchQuery = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick, modifier = Modifier.testTag("chat_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = MbotePurplePrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MbotePurpleSoft)
                                .clickable {
                                    onProfileClick()
                                }
                        ) {
                            AsyncImage(
                                model = chat.avatar,
                                contentDescription = chat.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onProfileClick()
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chat.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (chat.isVerified || chat.isAI) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isBlocked)
                                    "🚫 Contact bloqué"
                                else if (isPartnerTyping)
                                    "En train d'écrire… ✍️"
                                else if (chat.disappearingTimerSec > 0)
                                    "Éphémère (${formatDisappearingTime(chat.disappearingTimerSec)}) • Chiffré"
                                else if (chat.isOnline) "En ligne • Chiffré AES-256"
                                else "Chiffrement AES-256 actif",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBlocked) Color(0xFFDC2626) else if (isPartnerTyping) MbotePurplePrimary else if (chat.disappearingTimerSec > 0) MbotePurplePrimary else if (chat.isOnline) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                fontWeight = if (isPartnerTyping || isBlocked) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Search in chat button
                        IconButton(
                            onClick = { isSearchingInChat = true },
                            modifier = Modifier.testTag("chat_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Rechercher dans la discussion",
                                tint = MbotePurplePrimary
                            )
                        }

                        // Aron Shortcut Button
                        IconButton(
                            onClick = onOpenAronQuestions,
                            modifier = Modifier.testTag("chat_aron_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Questions d'Aron",
                                tint = Color(0xFFEC4899)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isFriend) {
                                    onAudioCallClick()
                                } else {
                                    Toast.makeText(context, "Vous devez être amis sur MBoté pour vous appeler.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.testTag("audio_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Appel audio",
                                tint = if (isFriend) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isFriend) {
                                    onVideoCallClick()
                                } else {
                                    Toast.makeText(context, "Vous devez être amis sur MBoté pour vous appeler.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.testTag("video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Appel vidéo",
                                tint = if (isFriend) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(onClick = { showDisappearingMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (showDisappearingMenu) {
                            com.loukatech.mbote.ui.components.ChatOptionsBottomSheet(
                                onDismiss = { showDisappearingMenu = false },
                                onSearchClick = {
                                    isSearchingInChat = true
                                },
                                onWallpaperClick = {
                                    showChatSettingsDialog = true
                                },
                                onEphemeralClick = {
                                    showChatSettingsDialog = true
                                },
                                onInviteClick = {
                                    showInviteLinkDialog = true
                                },
                                onAronQuestionsClick = {
                                    onOpenAronQuestions()
                                },
                                onPaymentClick = {
                                    onOpenPaymentSheet()
                                },
                                onPollClick = {
                                    onOpenPollDialog()
                                },
                                onExportClick = {
                                    val exportRes = ChatExportManager.exportChatHistory(context, chat)
                                    if (exportRes.isSuccess) {
                                        currentExportResult = exportRes.getOrNull()
                                        showExportSuccessDialog = true
                                    } else {
                                        Toast.makeText(context, "Erreur lors de l'export JSON", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (isBlocked) {
                    // Blocked state ribbon & unblock affordance
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚫 Vous avez bloqué ce contact",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "Débloquez ce contact pour lui envoyer des messages.",
                                    color = Color(0xFF991B1B),
                                    fontSize = 11.5.sp
                                )
                            }
                            Button(
                                onClick = { onToggleBlock(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("unblock_chat_btn")
                            ) {
                                Text("Débloquer", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else if (chat.isChannel && !isChannelOwner) {
                    // Non-owner subscriber read-only ribbon
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Canal de diffusion. Seuls les administrateurs peuvent publier.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (!isFriend) {
                    // Not friend state ribbon
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Échanges Restreints",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Tant que vous n'êtes pas encore amis sur MBoté, vous ne pouvez pas vous envoyer de messages ni vous appeler.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column {
                        // Smart Reply Row suggested by Gemini
                        if (smartReplies.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Smart Reply",
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Smart Reply ✨",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MbotePurplePrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(smartReplies) { reply ->
                                            Surface(
                                                onClick = {
                                                    onSendMessage(reply, null)
                                                    smartReplies = emptyList() // Clear after sending
                                                    coroutineScope.launch {
                                                        if (chat.messages.isNotEmpty()) {
                                                            listState.animateScrollToItem(chat.messages.size - 1)
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MbotePurpleSoft,
                                                border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.2f)),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = reply,
                                                    fontSize = 12.sp,
                                                    color = MbotePurplePrimary,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Replying banner
                        if (replyingToMessage != null) {
                            Surface(
                                color = MbotePurpleSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Réponse à ${replyingToMessage?.senderName ?: "un message"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MbotePurplePrimary
                                        )
                                        Text(
                                            text = replyingToMessage?.text ?: "",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { replyingToMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Fermer réponse",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Attachments Action Sheet / Expandable Bar
                        if (showAttachmentsMenu) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        AttachmentOptionButton(
                                            icon = Icons.Default.Image,
                                            label = "Photo",
                                            color = Color(0xFFEC4899),
                                            onClick = {
                                                showAttachmentsMenu = false
                                                mediaPickerIsVideo = false
                                                showMediaPickerSheet = true
                                            }
                                        )
                                    }
                                    item {
                                        AttachmentOptionButton(
                                            icon = Icons.Default.Videocam,
                                            label = "Vidéo",
                                            color = Color(0xFFF97316),
                                            onClick = {
                                                showAttachmentsMenu = false
                                                mediaPickerIsVideo = true
                                                showMediaPickerSheet = true
                                            }
                                        )
                                    }
                                    item {
                                        AttachmentOptionButton(
                                            icon = Icons.Default.Favorite,
                                            label = "Aron",
                                            color = Color(0xFFE11D48),
                                            onClick = {
                                                showAttachmentsMenu = false
                                                onOpenAronQuestions()
                                            }
                                        )
                                    }
                                    item {
                                        AttachmentOptionButton(
                                            icon = Icons.Default.Poll,
                                            label = "Sondage",
                                            color = Color(0xFF8B5CF6),
                                            onClick = {
                                                showAttachmentsMenu = false
                                                onOpenPollDialog()
                                            }
                                        )
                                    }
                                    item {
                                        AttachmentOptionButton(
                                            icon = Icons.Default.LocationOn,
                                            label = "Position",
                                            color = Color(0xFF0EA5E9),
                                            onClick = {
                                                showAttachmentsMenu = false
                                                onOpenLocationPicker()
                                            }
                                        )
                                    }
                                    item {
                                        AttachmentOptionButton(
                                            icon = Icons.Default.AccountBalanceWallet,
                                            label = "MBoté Pay",
                                            color = Color(0xFF10B981),
                                            onClick = {
                                                showAttachmentsMenu = false
                                                onOpenPaymentSheet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                    // Recording Bar OR Text Input Bar
                    AnimatedContent(
                        targetState = isRecordingActive,
                        label = "input_mode_transition"
                    ) { recording ->
                        if (recording) {
                            VoiceRecordingInputBar(
                                recorderManager = recorderManager,
                                onSendVoiceMessage = { file, durationSec, _ ->
                                    isRecordingActive = false
                                    onSendVoiceMessage(file.absolutePath, durationSec, replyingToMessage)
                                    replyingToMessage = null
                                    coroutineScope.launch {
                                        if (chat.messages.isNotEmpty()) {
                                            listState.animateScrollToItem(chat.messages.size)
                                        }
                                    }
                                },
                                onCancelRecording = {
                                    isRecordingActive = false
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { showAttachmentsMenu = !showAttachmentsMenu },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showAttachmentsMenu) Icons.Default.Close else Icons.Default.AddCircleOutline,
                                        contentDescription = "Menu pièces jointes",
                                        tint = MbotePurplePrimary
                                    )
                                }

                                IconButton(
                                    onClick = { showEmojiGifPanel = !showEmojiGifPanel },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.EmojiEmotions,
                                        contentDescription = "Emojis et GIFs WhatsApp",
                                        tint = if (showEmojiGifPanel) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextField(
                                    value = messageText,
                                    onValueChange = {
                                        messageText = it
                                        onUserComposing(it.isNotBlank())
                                    },
                                    placeholder = {
                                        Text(
                                            text = if (chat.isChannel) "Publier un élément sur la chaîne… 📢" else "Écrivez un message sécurisé…",
                                            fontSize = 14.sp
                                        )
                                    },
                                    trailingIcon = if (chat.id == "chat_luna") {
                                        {
                                            IconButton(onClick = { showLunaVoiceCommandSheet = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.SettingsVoice,
                                                    contentDescription = "Assistant Vocal Luna AI",
                                                    tint = MbotePurplePrimary
                                                )
                                            }
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field")
                                )

                                if (messageText.isNotBlank()) {
                                    // Dedicated Voice Recording button when text is also present
                                    IconButton(
                                        onClick = {
                                            val hasMic = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasMic) {
                                                val started = recorderManager.startRecording()
                                                if (started) {
                                                    isRecordingActive = true
                                                }
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .testTag("voice_record_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Enregistrer un message vocal",
                                            tint = MbotePurplePrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Send Text Button
                                    IconButton(
                                        onClick = {
                                            onUserComposing(false)
                                            onSendMessage(messageText, replyingToMessage)
                                            messageText = ""
                                            replyingToMessage = null
                                            coroutineScope.launch {
                                                if (chat.messages.isNotEmpty()) {
                                                    listState.animateScrollToItem(chat.messages.size)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MbotePurplePrimary)
                                            .testTag("chat_send_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Envoyer",
                                            tint = Color.White
                                        )
                                    }
                                } else {
                                    // Dedicated Voice Recording Button when input is empty
                                    IconButton(
                                        onClick = {
                                            val hasMic = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasMic) {
                                                val started = recorderManager.startRecording()
                                                if (started) {
                                                    isRecordingActive = true
                                                }
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MbotePurplePrimary)
                                            .testTag("voice_record_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Enregistrer un message vocal",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                        if (showEmojiGifPanel) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Tab(
                                        selected = emojiGifTab == 0,
                                        onClick = { emojiGifTab = 0 },
                                        text = { Text("😊 Emojis", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                                    )
                                    Tab(
                                        selected = emojiGifTab == 1,
                                        onClick = { emojiGifTab = 1 },
                                        text = { Text("🖼️ GIFs & Stickers", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(6.dp))

                                if (emojiGifTab == 0) {
                                    val emojis = listOf(
                                        "🇨🇬", "🇨🇩", "🇦🇴", "🇨🇲", "🇬🇦", "🇸🇳", "🌍", "🦁", "🐆", "🌴",
                                        "❤️", "🔥", "👍", "😂", "😍", "👏", "🎉", "💯", "🚀", "✨",
                                        "🙏", "😎", "🤩", "🥰", "🥳", "😭", "😤", "💪", "👑", "💎",
                                        "⭐", "💡", "🤝", "🙌", "💥", "💫", "💬", "🎵", "☕", "🥁"
                                    )
                                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(7),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(emojis.size) { index ->
                                            val emoji = emojis[index]
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        messageText += emoji
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = emoji, fontSize = 21.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Outlined.GifBox, contentDescription = null, tint = MbotePurplePrimary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Fournisseur GIF/stickers non configuré", fontWeight = FontWeight.Bold)
                                            Text(
                                                "Ajoutez GIPHY_API_KEY au fichier .env pour activer la recherche réelle.",
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
    },
    content = { padding ->
        val customBgColor = remember(chat.wallpaperColor) {
            if (!chat.wallpaperColor.isNullOrBlank()) {
                try {
                    Color(android.graphics.Color.parseColor(chat.wallpaperColor))
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(
                    if (customBgColor != null) Modifier.background(customBgColor)
                    else Modifier.background(MaterialTheme.colorScheme.background)
                )
        ) {
            if (!chat.wallpaperImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = chat.wallpaperImageUrl,
                    contentDescription = "Fond d'écran",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Contrast overlay so text is crystal clear
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.60f))
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // E2E Notice & Disappearing Timer Notice
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { showChatSettingsDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MbotePurplePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (chat.disappearingTimerSec > 0)
                                        "Messages chiffrés • Autodestruction (${formatDisappearingTime(chat.disappearingTimerSec)})"
                                    else "Messages chiffrés de bout en bout.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(chat.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        chat = chat,
                        currentUserAvatar = currentUserAvatar,
                        searchHighlightQuery = inChatSearchQuery,
                        onReplyClick = { replyingToMessage = message },
                        onReactionClick = { emoji -> onReaction(message.id, emoji) },
                        onDeleteClick = { onDeleteMessage(message.id) },
                        onVotePoll = { optionId -> onVotePoll(message.id, optionId) },
                        onTranslate = { targetLang -> onTranslateMessage(message.id, targetLang) },
                        onOpenMediaViewer = { url, isVideo, sender, time, caption ->
                            activeFullScreenMedia = FullScreenMediaData(
                                url = url,
                                isVideo = isVideo,
                                senderName = sender,
                                timestamp = time,
                                caption = caption,
                                msgId = message.id
                            )
                        }
                    )
                }

                if (isPartnerTyping) {
                    item(key = "typing_bubble_indicator") {
                        TypingIndicatorBubble(
                            avatarUrl = chat.avatar,
                            partnerName = chat.name
                        )
                    }
                }
            }
        }

        if (showLunaVoiceCommandSheet) {
            com.loukatech.mbote.ui.components.VoiceCommandSheet(
                onDismiss = { showLunaVoiceCommandSheet = false },
                onCommandRecognized = { command ->
                    showLunaVoiceCommandSheet = false
                    onSendMessage(command, null)
                }
            )
        }

        if (showChatSettingsDialog) {
            ChatSettingsDialog(
                chat = chat,
                isBlocked = isBlocked,
                onDismiss = { showChatSettingsDialog = false },
                onToggleBlock = { blocked -> onToggleBlock(blocked) },
                onUpdateDisappearingTimer = { sec -> onSetDisappearingTimer(sec) },
                onUpdateWallpaper = { colorHex, imgUrl -> onUpdateWallpaper(colorHex, imgUrl) },
                onTriggerExport = {
                    val exportRes = ChatExportManager.exportChatHistory(context, chat)
                    if (exportRes.isSuccess) {
                        currentExportResult = exportRes.getOrNull()
                        showExportSuccessDialog = true
                    } else {
                        Toast.makeText(context, "Erreur lors de l'export JSON", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showInviteLinkDialog) {
            com.loukatech.mbote.ui.components.InviteLinkDialog(
                titleName = chat.name,
                isChannel = chat.isChannel,
                onDismiss = { showInviteLinkDialog = false }
            )
        }

        if (showExportSuccessDialog && currentExportResult != null) {
            ChatExportDialog(
                chatName = chat.name,
                exportResult = currentExportResult!!,
                onDismiss = { showExportSuccessDialog = false }
            )
        }

        // Full Screen Image / Video Viewer Dialog
        if (activeFullScreenMedia != null) {
            FullScreenMediaViewerDialog(
                mediaUrl = activeFullScreenMedia!!.url,
                isVideo = activeFullScreenMedia!!.isVideo,
                senderName = activeFullScreenMedia!!.senderName,
                timestamp = activeFullScreenMedia!!.timestamp,
                caption = activeFullScreenMedia!!.caption,
                onDismiss = { activeFullScreenMedia = null },
                onReaction = { emoji -> onReaction(activeFullScreenMedia!!.msgId, emoji) }
            )
        }

        // Quick Media Picker Dialog (Photos & Videos)
        if (showMediaPickerSheet) {
            MediaPickerDialog(
                isVideo = mediaPickerIsVideo,
                onDismiss = { showMediaPickerSheet = false },
                onSendMedia = { url, caption ->
                    showMediaPickerSheet = false
                    onSendMediaMessage(url, mediaPickerIsVideo, caption)
                }
            )
        }
    }
)
}

@Composable
fun AttachmentOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ParticipantCircularAvatar(
    avatarUrl: String,
    senderName: String,
    isMine: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        shadowElevation = 1.dp,
        color = if (isMine) MbotePurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.testTag("chat_participant_avatar_${if (isMine) "mine" else "other"}")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = senderName.ifBlank { "Avatar participant" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                val initial = senderName.trim().take(1).uppercase().ifBlank { "?" }
                Text(
                    text = initial,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) MbotePurplePrimary else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    chat: Chat,
    currentUserAvatar: String = "",
    searchHighlightQuery: String = "",
    onReplyClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onVotePoll: (optionId: String) -> Unit = {},
    onTranslate: (targetLang: String) -> Unit = {},
    onOpenMediaViewer: (url: String, isVideo: Boolean, sender: String, timestamp: String, caption: String) -> Unit = { _, _, _, _, _ -> }
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReactionSelector by remember { mutableStateOf(false) }

    val avatarUrl = remember(message, chat, currentUserAvatar) {
        if (message.isMine) {
            message.senderAvatar.ifBlank { currentUserAvatar }
        } else {
            if (message.senderAvatar.isNotBlank()) {
                message.senderAvatar
            } else {
                val participant = chat.participants.find {
                    it.id == message.senderId || it.name.equals(message.senderName, ignoreCase = true)
                }
                participant?.avatar?.ifBlank { null } ?: chat.avatar
            }
        }
    }

    val senderDisplayName = if (message.isMine) "Moi" else message.senderName.ifBlank { chat.name }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!message.isMine) {
                ParticipantCircularAvatar(
                    avatarUrl = avatarUrl,
                    senderName = senderDisplayName,
                    isMine = false,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 6.dp, bottom = 2.dp)
                        .size(30.dp)
                )
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isMine) 18.dp else 4.dp,
                        bottomEnd = if (message.isMine) 4.dp else 18.dp
                    ),
                    color = if (message.isMine) MbotePurplePrimary else MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.5.dp,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onClick = {
                                if (message.mediaType == MediaType.IMAGE || message.mediaType == MediaType.VIDEO) {
                                    onOpenMediaViewer(
                                        message.mediaUrl ?: "",
                                        message.mediaType == MediaType.VIDEO,
                                        senderDisplayName,
                                        message.timestamp,
                                        message.text
                                    )
                                } else if (message.mediaType == MediaType.NONE) {
                                    showMenu = true
                                }
                            },
                            onLongClick = {
                                showReactionSelector = true
                            }
                        )
                        .testTag("message_bubble_${message.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Sender Name if in group & incoming
                        if (!message.isMine && message.senderName.isNotBlank() && chat.isGroup) {
                            Text(
                                text = message.senderName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MbotePurpleLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                    // Quoted message
                    if (message.replyToText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (message.isMine) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = message.replyToSender ?: "Réponse",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (message.isMine) Color.White else MbotePurplePrimary
                                )
                                Text(
                                    text = message.replyToText,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    color = if (message.isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Media Renderers
                    when (message.mediaType) {
                        MediaType.IMAGE, MediaType.VIDEO -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .clickable {
                                            onOpenMediaViewer(
                                                message.mediaUrl ?: "",
                                                message.mediaType == MediaType.VIDEO,
                                                senderDisplayName,
                                                message.timestamp,
                                                message.text
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!message.mediaUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = message.mediaUrl,
                                            contentDescription = if (message.mediaType == MediaType.VIDEO) "Vidéo" else "Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (message.mediaType == MediaType.VIDEO) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Lire la vidéo",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                                if (message.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        MediaType.AUDIO -> {
                            VoiceAudioPlayerBubble(
                                durationSec = message.audioDurationSec.coerceAtLeast(4),
                                isMine = message.isMine
                            )
                        }
                        MediaType.ARON_QUESTION -> {
                            message.aronQuestion?.let { q ->
                                AronQuestionBubbleCard(question = q, isMine = message.isMine)
                            }
                        }
                        MediaType.POLL -> {
                            message.pollData?.let { poll ->
                                PollBubbleCard(poll = poll, isMine = message.isMine, onVote = onVotePoll)
                            }
                        }
                        MediaType.LOCATION -> {
                            message.locationData?.let { loc ->
                                LocationBubbleCard(loc = loc, isMine = message.isMine)
                            }
                        }
                        MediaType.PAYMENT -> {
                            message.paymentData?.let { pay ->
                                PaymentBubbleCard(payment = pay, isMine = message.isMine)
                            }
                        }
                        else -> {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Translation Banner if translated
                    if (!message.translatedText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (message.isMine) Color.White.copy(alpha = 0.15f) else MbotePurpleSoft
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "🌐 Traduction (${message.targetLanguage ?: "Lingala"}) :",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (message.isMine) Color.White else MbotePurplePrimary
                                )
                                Text(
                                    text = message.translatedText,
                                    fontSize = 11.sp,
                                    color = if (message.isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time & Status & Disappearing Indicator
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.disappearingDurationSec > 0) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Éphémère",
                                tint = if (message.isMine) Color.White.copy(alpha = 0.7f) else MbotePurplePrimary,
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        Text(
                            text = message.timestamp,
                            fontSize = 10.sp,
                            color = if (message.isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (message.isMine) {
                            val tickIcon = when (message.status) {
                                MessageStatus.READ -> Icons.Outlined.DoneAll
                                MessageStatus.DELIVERED -> Icons.Outlined.DoneAll
                                MessageStatus.SENT -> Icons.Outlined.Check
                            }
                            val tickTint = when (message.status) {
                                MessageStatus.READ -> Color(0xFF38BDF8) // Double blue tick! (Light cyan blue for contrast on purple)
                                MessageStatus.DELIVERED -> Color.White.copy(alpha = 0.85f)
                                MessageStatus.SENT -> Color.White.copy(alpha = 0.6f)
                            }
                            Icon(
                                imageVector = tickIcon,
                                contentDescription = when (message.status) {
                                    MessageStatus.READ -> "Lu (Double bleu)"
                                    MessageStatus.DELIVERED -> "Distribué"
                                    MessageStatus.SENT -> "Envoyé"
                                },
                                tint = tickTint,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Répondre") },
                    onClick = {
                        showMenu = false
                        onReplyClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("🌐 Traduire en Lingala") },
                    onClick = {
                        showMenu = false
                        onTranslate("Lingala")
                    }
                )
                DropdownMenuItem(
                    text = { Text("🇫🇷 Traduire en Français") },
                    onClick = {
                        showMenu = false
                        onTranslate("Français")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Réagir ❤️") },
                    onClick = {
                        showMenu = false
                        onReactionClick("❤️")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Réagir 👍") },
                    onClick = {
                        showMenu = false
                        onReactionClick("👍")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Réagir 🔥") },
                    onClick = {
                        showMenu = false
                        onReactionClick("🔥")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Supprimer") },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444)) }
                )
            }
        }

        if (message.isMine) {
            ParticipantCircularAvatar(
                avatarUrl = avatarUrl,
                senderName = "Moi",
                isMine = true,
                modifier = Modifier
                    .padding(start = 6.dp, end = 4.dp, bottom = 2.dp)
                    .size(30.dp)
            )
        }
    }

        // Reactions list
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.forEach { (emoji, count) ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .padding(1.dp)
                            .clickable { onReactionClick(emoji) }
                    ) {
                        Text(
                            text = "$emoji $count",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (showReactionSelector) {
            MessageReactionChooserDialog(
                onDismiss = { showReactionSelector = false },
                onSelectEmoji = { emoji ->
                    onReactionClick(emoji)
                    showReactionSelector = false
                }
            )
        }
    }
}

@Composable
fun AronQuestionBubbleCard(
    question: AronQuestion,
    isMine: Boolean
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else MbotePurpleSoft
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔮", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Question #${question.id} • ${question.category}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) Color.White else MbotePurplePrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "« ${question.questionFr} »",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
            )

            if (!question.questionLn.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🇨🇬 Lingala : ${question.questionLn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PollBubbleCard(
    poll: PollData,
    isMine: Boolean,
    onVote: (optionId: String) -> Unit
) {
    val totalVotes = poll.options.sumOf { it.votesCount }.coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMine) Color.White.copy(alpha = 0.18f) else MbotePurpleSoft.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Poll,
                    contentDescription = null,
                    tint = if (isMine) Color.White else MbotePurplePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sondage interactif",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) Color.White else MbotePurplePrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = poll.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            poll.options.forEach { option ->
                val percentage = ((option.votesCount.toFloat() / totalVotes) * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isMine) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onVote(option.id) }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$percentage% (${option.votesCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMine) Color.White else MbotePurplePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { option.votesCount.toFloat() / totalVotes },
                            color = if (isMine) Color.White else MbotePurplePrimary,
                            trackColor = if (isMine) Color.White.copy(alpha = 0.2f) else MbotePurpleSoft,
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationBubbleCard(
    loc: LocationData,
    isMine: Boolean
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else MbotePurpleSoft
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location_bubble_${loc.latitude}_${loc.longitude}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isMine) Color.White else Color(0xFF0EA5E9),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (loc.isLive) "📍 Position en direct (${loc.durationRemainingText ?: "Actif"})" else "📍 Position GPS partagée",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMine) Color.White else MbotePurplePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = loc.placeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Coordonnées : ${loc.latitude}, ${loc.longitude}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        try {
                            val mapUri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(${Uri.encode(loc.placeName)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}")
                            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(webIntent)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = if (isMine) Color.White else MbotePurplePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ouvrir dans Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMine) Color.White else MbotePurplePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentBubbleCard(
    payment: PaymentTransferData,
    isMine: Boolean
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else MbotePurpleSoft
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (isMine) Color.White else Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${payment.provider} • MBoté Pay",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMine) Color.White else MbotePurplePrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = payment.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMine) Color.White else Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = payment.amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Motif : ${payment.note} • Réf: ${payment.transactionId}",
                fontSize = 10.sp,
                color = if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VoiceAudioPlayerBubble(
    durationSec: Int,
    isMine: Boolean
) {
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    val waveformBars = remember { generateStaticWaveform(28) }

    LaunchedEffect(isPlaying, speedMultiplier) {
        if (isPlaying) {
            val totalSteps = (durationSec * 10).coerceAtLeast(20)
            val stepDelayMs = (100 / speedMultiplier).toLong()

            while (isPlaying && playbackProgress < 1.0f) {
                delay(stepDelayMs)
                playbackProgress += 1f / totalSteps
            }
            if (playbackProgress >= 1.0f) {
                isPlaying = false
                playbackProgress = 0f
            }
        }
    }

    val currentSeconds = (playbackProgress * durationSec).toInt()
    val timeLabel = String.format("%d:%02d", currentSeconds / 60, currentSeconds % 60)
    val totalLabel = String.format("%d:%02d", durationSec / 60, durationSec % 60)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play / Pause Circle Button
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isMine) Color.White.copy(alpha = 0.25f) else MbotePurpleSoft)
                    .testTag("voice_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Lire",
                    tint = if (isMine) Color.White else MbotePurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Visual Waveform representation
            Box(modifier = Modifier.weight(1f)) {
                AudioPlaybackWaveform(
                    progress = playbackProgress,
                    bars = waveformBars,
                    activeColor = if (isMine) Color.White else MbotePurplePrimary,
                    inactiveColor = if (isMine) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                )
            }

            // Speed multiplier toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isMine) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        speedMultiplier = when (speedMultiplier) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                    }
            ) {
                Text(
                    text = "${speedMultiplier}x",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) Color.White else MbotePurplePrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // Duration / Timing footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isPlaying) "$timeLabel / $totalLabel" else totalLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isMine) Color.White.copy(alpha = 0.8f) else MbotePurplePrimary,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "Opus HD",
                    fontSize = 10.sp,
                    color = if (isMine) Color.White.copy(alpha = 0.8f) else MbotePurplePrimary
                )
            }
        }
    }
}

private fun formatDisappearingTime(seconds: Int): String {
    return when {
        seconds <= 0 -> "Désactivé"
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        seconds < 86400 -> "${seconds / 3600}h"
        else -> "${seconds / 86400}j"
    }
}

@Composable
fun MessageReactionChooserDialog(
    onDismiss: () -> Unit,
    onSelectEmoji: (String) -> Unit
) {
    val quickEmojis = listOf("❤️", "👍", "🔥", "😂", "😮", "😢", "🙏", "👏", "🎉", "💯", "🚀", "💡", "🇨🇬", "🤝")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Réagir au message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top quick reaction pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickEmojis.take(6).forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    onSelectEmoji(emoji)
                                },
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                // Second row of emojis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickEmojis.drop(6).take(6).forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    onSelectEmoji(emoji)
                                },
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = MbotePurplePrimary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun TypingIndicatorBubble(
    avatarUrl: String,
    partnerName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 400, delayMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 400, delayMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        ParticipantCircularAvatar(
            avatarUrl = avatarUrl,
            senderName = partnerName,
            isMine = false,
            modifier = Modifier
                .padding(start = 4.dp, end = 6.dp, bottom = 2.dp)
                .size(30.dp)
        )

        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MbotePurplePrimary)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MbotePurplePrimary)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MbotePurplePrimary)
                )
            }
        }
    }
}

@Composable
fun MediaPickerDialog(
    isVideo: Boolean,
    onDismiss: () -> Unit,
    onSendMedia: (url: String, caption: String) -> Unit
) {
    val sampleImages = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800",
        "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=800",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800"
    )

    val sampleVideos = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
    )

    var selectedUrl by remember { mutableStateOf(if (isVideo) sampleVideos[0] else sampleImages[0]) }
    var caption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Image,
                    contentDescription = null,
                    tint = if (isVideo) Color(0xFFF97316) else Color(0xFFEC4899)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isVideo) "Envoyer une vidéo" else "Envoyer une photo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isVideo) "Sélectionnez une vidéo de votre galerie :" else "Sélectionnez une photo de votre galerie :",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Presets gallery grid / row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val mediaList = if (isVideo) sampleVideos else sampleImages
                    items(mediaList) { itemUrl ->
                        val isSelected = selectedUrl == itemUrl
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    BorderStroke(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) MbotePurplePrimary else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedUrl = itemUrl }
                        ) {
                            if (isVideo) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = itemUrl,
                                    contentDescription = "Photo option",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Ajouter une légende… (facultatif)", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendMedia(selectedUrl, caption) },
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Envoyer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
