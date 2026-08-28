package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loukatech.mbote.model.*
import com.loukatech.mbote.ui.theme.PurpleLight
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties

@Composable
fun StatusRingAvatar(
    avatarUrl: String,
    hasStory: Boolean = false,
    isViewed: Boolean = false,
    size: Dp = 52.dp,
    onClick: () -> Unit = {}
) {
    val ringBrush = when {
        !hasStory -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        isViewed -> Brush.linearGradient(listOf(Color.LightGray, Color.Gray))
        else -> Brush.linearGradient(listOf(PurplePrimary, PurpleLight, Color(0xFF00C49F)))
    }

    Box(
        modifier = Modifier
            .size(size)
            .border(
                width = if (hasStory) 2.5.dp else 0.dp,
                brush = ringBrush,
                shape = CircleShape
            )
            .padding(if (hasStory) 3.dp else 0.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Rechercher…",
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Effacer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_input")
    )
}

@Composable
fun FilterChipRow(
    filters: List<String>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectFilter(filter) }
                    .testTag("filter_chip_$filter")
            ) {
                Text(
                    text = filter,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, message: String, isGroup: Boolean) -> Unit,
    onPickFromContacts: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isGroup by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 16.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .testTag("new_chat_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Icon, Title & Close Button
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
                                imageVector = if (isGroup) Icons.Default.Groups else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isGroup) "Nouveau groupe" else "Nouveau message",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isGroup) "Discutez en communauté" else "Démarrez un chat privé",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Type selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (!isGroup) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isGroup = false }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "💬 Direct",
                                color = if (!isGroup) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isGroup) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isGroup = true }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "👥 Groupe",
                                color = if (isGroup) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }

                // Pick from contacts button
                if (onPickFromContacts != null && !isGroup) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickFromContacts() }
                            .testTag("pick_from_contacts_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PurplePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Choisir depuis mes Masta (contacts)",
                                color = PurplePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Input Field 1: Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isGroup) "Nom du groupe *" else "Nom ou numéro du destinataire *") },
                    placeholder = { Text(if (isGroup) "ex. Amis du Congo" else "ex. Grace Ondongo ou +242...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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
                        .fillMaxWidth()
                        .testTag("new_chat_name_input")
                )

                // Input Field 2: Initial Message
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message initial") },
                    placeholder = { Text("Écrivez votre premier message...") },
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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
                        .fillMaxWidth()
                        .testTag("new_chat_message_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onConfirm(name, message, isGroup) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        enabled = name.isNotBlank() && message.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("new_chat_confirm_button")
                    ) {
                        Text(
                            text = if (isGroup) "Créer" else "Démarrer",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddStatusDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, imageUrl: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var statusMode by remember { mutableStateOf("Texte") } // "Texte", "Photo", "Vocal"
    var selectedColorIndex by remember { mutableStateOf(0) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var privacyOption by remember { mutableStateOf("Mes Masta (contacts)") }

    val bgGradients = listOf(
        listOf(Color(0xFF7C3AED), Color(0xFFC084FC)), // Mbote Purple
        listOf(Color(0xFF2563EB), Color(0xFF38BDF8)), // Ocean Blue
        listOf(Color(0xFF059669), Color(0xFF34D399)), // Emerald Green
        listOf(Color(0xFFDC2626), Color(0xFFF87171)), // Crimson Sunset
        listOf(Color(0xFFD97706), Color(0xFFFBBF24)), // Amber Warm
        listOf(Color(0xFF0F172A), Color(0xFF334155))  // Midnight Dark
    )

    val presetImages = listOf(
        null,
        "https://images.unsplash.com/photo-1501504905252-473c47e087f8?w=500&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=500&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80"
    )

    val currentGradient = bgGradients[selectedColorIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp)
                .testTag("add_status_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Publier un statut",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Partagez avec $privacyOption",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Mode Switcher (Texte / Photo / Vocal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Texte", "Photo", "Vocal").forEach { mode ->
                        val isSelected = statusMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { statusMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Live Preview Canvas Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (statusMode == "Texte" || presetImages[selectedImageIndex] == null) {
                                androidx.compose.ui.graphics.Brush.linearGradient(currentGradient)
                            } else {
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (statusMode == "Photo" && presetImages[selectedImageIndex] != null) {
                        AsyncImage(
                            model = presetImages[selectedImageIndex],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }

                    if (statusMode == "Vocal") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isRecordingAudio = !isRecordingAudio },
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecordingAudio) Color(0xFFEF4444) else Color.White.copy(alpha = 0.25f))
                            ) {
                                Icon(
                                    imageVector = if (isRecordingAudio) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Enregistrer",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (isRecordingAudio) "Enregistrement en cours (00:08)..." else "Appuyez pour enregistrer un statut vocal",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = if (text.isBlank()) "Votre statut apparaîtra ici..." else text,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Input Field
                if (statusMode != "Vocal") {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Quoi de neuf ? Exprimez-vous...") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_status_text_input")
                    )
                }

                // Customization Pickers
                if (statusMode == "Texte") {
                    Text(
                        text = "Couleurs de fond :",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        bgGradients.forEachIndexed { index, grad ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Brush.linearGradient(grad))
                                    .border(
                                        width = if (selectedColorIndex == index) 3.dp else 0.dp,
                                        color = if (selectedColorIndex == index) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = index }
                            )
                        }
                    }
                } else if (statusMode == "Photo") {
                    Text(
                        text = "Choisir une illustration de fond :",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presetImages.size) { index ->
                            val img = presetImages[index]
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selectedImageIndex == index) 2.5.dp else 1.dp,
                                        color = if (selectedImageIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedImageIndex = index }
                            ) {
                                if (img != null) {
                                    AsyncImage(
                                        model = img,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = "Dégradé",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Bar: Annuler & Publier
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val imgToPost = if (statusMode == "Photo") presetImages[selectedImageIndex] else null
                            val textToPost = if (statusMode == "Vocal") "🎙️ Statut Vocal MBoté" else text
                            onConfirm(textToPost, imgToPost)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = (statusMode == "Vocal" && isRecordingAudio) || text.isNotBlank() || (statusMode == "Photo" && presetImages[selectedImageIndex] != null),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp)
                            .testTag("confirm_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Publier le statut",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewMeetingDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, duration: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(30) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Créer une visioconférence",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Sujet de la réunion") },
                    placeholder = { Text("Ex: Point d'avancement projet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("meeting_title_input")
                )

                Text(
                    text = "Durée prévue : $duration minutes",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 15f..120f,
                    steps = 6
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(title, duration) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("confirm_meeting_button")
                    ) {
                        Text("Démarrer")
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String, 
        bio: String, 
        phone: String, 
        city: String,
        avatar: String,
        coverUrl: String,
        channelAvatar: String,
        channelBanner: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var bio by remember { mutableStateOf(currentProfile.bio) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var city by remember { mutableStateOf(currentProfile.city) }

    // Photos state
    var avatar by remember { mutableStateOf(currentProfile.avatar) }
    var coverUrl by remember { mutableStateOf(currentProfile.coverUrl) }
    var channelAvatar by remember { mutableStateOf(currentProfile.channelAvatar) }
    var channelBanner by remember { mutableStateOf(currentProfile.channelBanner) }

    val stockAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80"
    )

    val stockBanners = listOf(
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1557683316-973673baf926?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Modifier mon profil & Médias",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )

                // PART 1: Text info
                Text(
                    text = "Informations Générales",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary.copy(alpha = 0.8f)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = {
                            if (it.length <= 150) {
                                bio = it
                            }
                        },
                        label = { Text("Biographie / Présentation") },
                        supportingText = {
                            Text(
                                text = "${bio.length}/150 caractères",
                                color = if (bio.length >= 140) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Ville & Pays") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // PART 2: Account Media customization
                Text(
                    text = "Photos de mon Compte",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary.copy(alpha = 0.8f)
                )

                // Account Avatar Customizer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Photo de Profil du Compte", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, PurplePrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Sélectionner un avatar premium :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(stockAvatars) { itemUrl ->
                                    val isSelected = itemUrl == avatar
                                    AsyncImage(
                                        model = itemUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) PurplePrimary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { avatar = itemUrl },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = avatar,
                        onValueChange = { avatar = it },
                        label = { Text("Lien personnalisé Photo de Profil") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Account Banner Customizer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bannière de couverture du Compte", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sélectionner une bannière premium :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(stockBanners) { itemUrl ->
                                val isSelected = itemUrl == coverUrl
                                AsyncImage(
                                    model = itemUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) PurplePrimary else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { coverUrl = itemUrl },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Lien personnalisé de la Bannière") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // PART 3: Channel Media customization
                Text(
                    text = "Photos de ma Chaîne MBoté",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary.copy(alpha = 0.8f)
                )

                // Channel Avatar Customizer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Photo de Profil de la Chaîne", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = channelAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, PurplePrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Sélectionner un logo de chaîne :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(stockAvatars) { itemUrl ->
                                    val isSelected = itemUrl == channelAvatar
                                    AsyncImage(
                                        model = itemUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) PurplePrimary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { channelAvatar = itemUrl },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = channelAvatar,
                        onValueChange = { channelAvatar = it },
                        label = { Text("Lien personnalisé Photo Chaîne") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Channel Banner Customizer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bannière de la Chaîne", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    AsyncImage(
                        model = channelBanner,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sélectionner une bannière de chaîne :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(stockBanners) { itemUrl ->
                                val isSelected = itemUrl == channelBanner
                                AsyncImage(
                                    model = itemUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) PurplePrimary else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { channelBanner = itemUrl },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = channelBanner,
                        onValueChange = { channelBanner = it },
                        label = { Text("Lien de la Bannière de Chaîne") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            onConfirm(name, bio, phone, city, avatar, coverUrl, channelAvatar, channelBanner) 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

/**
 * Dedicated dialog for modifying user's biography with 150 characters limit
 */
@Composable
fun EditBioDialog(
    currentBio: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var bio by remember { mutableStateOf(currentBio) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Modifier ma biographie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Présentez-vous en quelques mots (150 car. max)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = bio,
                    onValueChange = {
                        if (it.length <= 150) {
                            bio = it
                        }
                    },
                    placeholder = { Text("Écrivez votre bio ici...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (bio.length >= 150) "Limite atteinte" else "",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${bio.length}/150",
                                fontWeight = FontWeight.Bold,
                                color = if (bio.length >= 140) Color(0xFFEF4444) else PurplePrimary,
                                fontSize = 11.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(bio) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Mettre à jour")
                    }
                }
            }
        }
    }
}

/**
 * 1) Share Profile Dialog with QR Code and Unique Deep Link
 */
@Composable
fun ShareProfileQrDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val cleanUsername = userProfile.username.removePrefix("@")
    val deepLink = "https://mbote.app/u/$cleanUsername"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Partager mon profil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // Profile Card with QR Code
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, PurpleSoft),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User Avatar & Name
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, PurplePrimary, CircleShape)
                        ) {
                            AsyncImage(
                                model = userProfile.avatar,
                                contentDescription = userProfile.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = userProfile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = userProfile.username,
                            color = PurplePrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        if (userProfile.bio.isNotBlank()) {
                            Text(
                                text = userProfile.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Big QR Code Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFAF8FF),
                            border = BorderStroke(1.dp, Color(0xFFEDE9FE)),
                            modifier = Modifier.size(170.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Code QR de profil",
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(150.dp)
                                )
                            }
                        }

                        Text(
                            text = "Scannez avec MBoté pour discuter instantanément",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Deep Link Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PurpleSoft.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = deepLink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurplePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(deepLink))
                                Toast.makeText(context, "Lien copié : $deepLink", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier",
                                tint = PurplePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(deepLink))
                            Toast.makeText(context, "Lien copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copier", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Partage de $deepLink vers vos contacts MBoté !", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 5) Animated Reaction Picker Popover with emojis
 */
@Composable
fun ReactionPickerBar(
    onSelectReaction: (emoji: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reactions = listOf(
        "❤️" to "J'aime",
        "👍" to "Top",
        "🔥" to "Mbote",
        "😂" to "Haha",
        "😮" to "Waouh",
        "👏" to "Bravo",
        "🙏" to "Respect",
        "🇨🇬" to "Congo"
    )

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF1E1B4B),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color(0xFF6D28D9).copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            reactions.forEachIndexed { index, (emoji, _) ->
                var isHovered by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isHovered) 1.35f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "reaction_scale"
                )

                Text(
                    text = emoji,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .scale(scale)
                        .clickable {
                            onSelectReaction(emoji)
                            onDismiss()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 6) Full Screen Media Viewer Lightbox (Images and Videos)
 */
@Composable
fun FullScreenMediaViewerDialog(
    mediaUrl: String,
    mediaTitle: String = "Média",
    authorName: String = "MBoté",
    authorAvatar: String = "",
    timestamp: String = "Aujourd'hui",
    isVideo: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableFloatStateOf(0.35f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showHeartAnimation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Media Content (Zoomable / Panable)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = mediaTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Center Play/Pause button
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Lecture",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = mediaTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }

                // Heart overlay if double tapped
                AnimatedVisibility(
                    visible = showHeartAnimation,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer plein écran",
                            tint = Color.White
                        )
                    }

                    if (authorAvatar.isNotBlank()) {
                        AsyncImage(
                            model = authorAvatar,
                            contentDescription = authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    }

                    Column {
                        Text(
                            text = authorName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = timestamp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Image enregistrée dans la galerie !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Télécharger",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Lien de partage généré !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Video / Media Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(16.dp)
            ) {
                if (mediaTitle.isNotBlank()) {
                    Text(
                        text = mediaTitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isVideo) {
                    Slider(
                        value = videoProgress,
                        onValueChange = { videoProgress = it },
                        colors = SliderDefaults.colors(
                            thumbColor = PurplePrimary,
                            activeTrackColor = PurplePrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "00:15 / 00:45",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )

                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Son",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// UNIFORM DESIGN SYSTEM COMPONENTS: BUTTONS & MODALS
// =========================================================================

@Composable
fun MbotePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    containerColor: Color = PurplePrimary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        modifier = modifier
            .height(height)
            .testTag("mbote_primary_btn_${text.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MboteSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
            .height(height)
            .testTag("mbote_secondary_btn_${text.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MboteOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    height: Dp = 50.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        modifier = modifier
            .height(height)
            .testTag("mbote_outlined_btn_${text.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MboteModalHeader(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = PurplePrimary,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun MboteStandardDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = PurplePrimary,
    confirmButtonText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String = "Fermer",
    isConfirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 24.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MboteModalHeader(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    iconTint = iconTint,
                    onClose = onDismissRequest
                )

                content()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MboteOutlinedButton(
                        text = dismissButtonText,
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    )

                    if (confirmButtonText != null && onConfirm != null) {
                        MbotePrimaryButton(
                            text = confirmButtonText,
                            onClick = onConfirm,
                            enabled = isConfirmEnabled,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// LANGUAGE SELECTION DIALOG (REQUIREMENT 3)
// =========================================================================

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLang by remember { mutableStateOf(currentLanguage) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 24.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("language_selection_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MboteModalHeader(
                    title = "Langue de l'application",
                    subtitle = "Choisissez votre langue d'affichage et de traduction",
                    icon = Icons.Outlined.Translate,
                    iconTint = PurplePrimary,
                    onClose = onDismiss
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = lang == selectedLang
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) PurplePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { selectedLang = lang }
                                .testTag("lang_option_${lang.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = lang.flag,
                                        fontSize = 28.sp
                                    )
                                    Column {
                                        Text(
                                            text = lang.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp,
                                            color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = lang.nativeName,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = lang.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLang = lang },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PurplePrimary
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MboteOutlinedButton(
                        text = "Annuler",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    MbotePrimaryButton(
                        text = "Enregistrer",
                        onClick = {
                            onSelectLanguage(selectedLang)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// CURRENCY SELECTION DIALOG (REQUIREMENT 3)
// =========================================================================

@Composable
fun CurrencySelectionDialog(
    currentCurrency: AppCurrency,
    onSelectCurrency: (AppCurrency) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCurr by remember { mutableStateOf(currentCurrency) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 24.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("currency_selection_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MboteModalHeader(
                    title = "Devise monétaire",
                    subtitle = "Affichage de vos soldes, pourboires et cadeaux",
                    icon = Icons.Outlined.Paid,
                    iconTint = Color(0xFF059669),
                    onClose = onDismiss
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppCurrency.values().forEach { curr ->
                        val isSelected = curr == selectedCurr
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) Color(0xFF059669).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { selectedCurr = curr }
                                .testTag("currency_option_${curr.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF059669).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = curr.symbol,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${curr.flag} ${curr.displayName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Exemple : ${formatAppCurrency(45000L, curr)}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedCurr = curr },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF059669)
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MboteOutlinedButton(
                        text = "Annuler",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    MbotePrimaryButton(
                        text = "Appliquer",
                        containerColor = Color(0xFF059669),
                        onClick = {
                            onSelectCurrency(selectedCurr)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// OFFLINE INDICATOR BANNER (REQUIREMENT 4)
// =========================================================================

@Composable
fun OfflineModeBanner(
    isOffline: Boolean,
    onToggleOnline: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = Color(0xFFD97706),
            contentColor = Color.White,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Mode Hors Ligne • Contenus et médias sauvegardés localement",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Reconnecter",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleOnline() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

