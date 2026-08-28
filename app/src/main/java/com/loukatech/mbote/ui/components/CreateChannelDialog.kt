package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loukatech.mbote.ui.theme.PurplePrimary

@Composable
fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (channelName: String, description: String, isPublic: Boolean, initialPost: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current wizard step (1: Détails, 2: Personnalisation, 3: Paramètres, 4: Aperçu)
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: Details fields
    var channelName by remember { mutableStateOf("Aventures & Découvertes") }
    var username by remember { mutableStateOf("aventures.decouvertes") }
    var description by remember { mutableStateOf("Bienvenue sur ma chaîne ! Je partage mes plus beaux voyages, itinéraires et conseils culturels à travers le Congo et l'Afrique.") }
    var category by remember { mutableStateOf("Voyage & Aventure") }
    var language by remember { mutableStateOf("Français") }
    var linkInput by remember { mutableStateOf("") }
    val addedLinks = remember { mutableStateListOf("https://instagram.com/ma.chaine") }

    // Step 2: Customization fields
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }
    val avatarPresets = listOf(
        "✈️", "🌍", "🎥", "🎵", "💼", "🎨", "⚽", "💡"
    )

    // Step 3: Settings fields
    var isPublic by remember { mutableStateOf(true) }
    var allowComments by remember { mutableStateOf(true) }
    var allowReactions by remember { mutableStateOf(true) }

    // Step 4: Preview initial announcement
    var initialPost by remember { mutableStateOf("Bienvenue à tous sur ma chaîne officielle ! Restez connectés pour ne rien rater. 🚀") }

    // Dropdown expanded states
    var categoryExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Voyage & Aventure",
        "Tech & Innovation",
        "Actualités & Médias",
        "Musique & Culture",
        "Business & Entrepreneuriat",
        "Divertissement",
        "Sport & Fitness",
        "Art & Design",
        "Mode & Beauté",
        "Lifestyle"
    )

    val languages = listOf("Français", "Lingala", "Kituba", "English", "Swahili")

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
                .testTag("create_channel_dialog")
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
                                imageVector = Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Créer une chaîne",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Étape $currentStep sur 4",
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
                            .testTag("close_channel_dialog")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stepper Indicator (1 Détails, 2 Personnalisation, 3 Paramètres, 4 Aperçu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val steps = listOf(
                        1 to "Détails",
                        2 to "Style",
                        3 to "Accès",
                        4 to "Aperçu"
                    )

                    steps.forEach { (stepNum, stepName) ->
                        val isActive = stepNum == currentStep
                        val isDone = stepNum < currentStep

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isActive -> Color(0xFFFAF5FF)
                                isDone -> Color(0xFFF0FDF4)
                                else -> Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isActive -> PurplePrimary
                                    isDone -> Color(0xFF86EFAC)
                                    else -> Color(0xFFE2E8F0)
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (stepNum < currentStep) currentStep = stepNum
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isDone -> Color(0xFF10B981)
                                                isActive -> PurplePrimary
                                                else -> Color(0xFFCBD5E1)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = stepNum.toString(),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(5.dp))

                                Text(
                                    text = stepName,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isActive -> PurplePrimary
                                        isDone -> Color(0xFF16A34A)
                                        else -> Color(0xFF64748B)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Wizard Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> {
                            // Step 1: Details
                            Text(
                                text = "Informations de base",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "Donnez un nom clair, un identifiant unique et une promesse simple à votre chaîne.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                            )

                            // Nom de la chaîne *
                            OutlinedTextField(
                                value = channelName,
                                onValueChange = { if (it.length <= 80) channelName = it },
                                label = { Text("Nom de la chaîne *") },
                                placeholder = { Text("Aventures & Découvertes") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Text(
                                        text = "${channelName.length}/80",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("channel_name_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Nom d'utilisateur * (@ handle)
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it.lowercase().replace(" ", ".").filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }
                                },
                                label = { Text("Nom d’utilisateur *") },
                                prefix = { Text("@ ", fontWeight = FontWeight.Bold, color = PurplePrimary) },
                                placeholder = { Text("aventures.decouvertes") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "mbote.com/@${if (username.isBlank()) "nom.de.chaine" else username}",
                                fontSize = 11.sp,
                                color = PurplePrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Description *
                            OutlinedTextField(
                                value = description,
                                onValueChange = { if (it.length <= 700) description = it },
                                label = { Text("Description *") },
                                placeholder = { Text("Bienvenue sur ma chaîne ! Je partage...") },
                                minLines = 3,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${description.length}/700",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Catégorie Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Catégorie") },
                                    trailingIcon = {
                                        IconButton(onClick = { categoryExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurplePrimary,
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    categories.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                category = item
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Langue de la chaîne Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = language,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Langue de la chaîne") },
                                    trailingIcon = {
                                        IconButton(onClick = { languageExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurplePrimary,
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { languageExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = languageExpanded,
                                    onDismissRequest = { languageExpanded = false }
                                ) {
                                    languages.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                language = item
                                                languageExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Ajouter un lien
                            Text(
                                text = "Liens externes",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF334155)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = linkInput,
                                    onValueChange = { linkInput = it },
                                    placeholder = { Text("https://instagram.com/ma.chaine") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurplePrimary,
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = {
                                        if (linkInput.isNotBlank()) {
                                            addedLinks.add(linkInput.trim())
                                            linkInput = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text("Ajouter", fontSize = 13.sp)
                                }
                            }

                            if (addedLinks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    addedLinks.forEach { link ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Link,
                                                        contentDescription = null,
                                                        tint = PurplePrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = link,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF1E293B)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { addedLinks.remove(link) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Supprimer",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Step 2: Personnalisation
                            Text(
                                text = "Personnalisation visuelle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "Choisissez une icône représentative et le style de votre chaîne.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                            )

                            Text(
                                text = "Icône / Avatar de la chaîne :",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF334155)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFAF5FF))
                                        .border(2.dp, PurplePrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = avatarPresets[selectedAvatarIndex],
                                        fontSize = 36.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(avatarPresets.size) { idx ->
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedAvatarIndex == idx) Color(0xFFFAF5FF) else Color(0xFFF1F5F9)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (selectedAvatarIndex == idx) PurplePrimary else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { selectedAvatarIndex = idx },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = avatarPresets[idx], fontSize = 22.sp)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Step 3: Paramètres
                            Text(
                                text = "Paramètres de la chaîne",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "Définissez la visibilité et la modération de votre chaîne.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                            )

                            // Type de chaîne
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isPublic) Color(0xFFFAF5FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isPublic) PurplePrimary else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPublic = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isPublic, onClick = { isPublic = true })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Chaîne Publique", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Tout le monde peut découvrir votre chaîne et s'y abonner.", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (!isPublic) Color(0xFFFAF5FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (!isPublic) PurplePrimary else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPublic = false }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = !isPublic, onClick = { isPublic = false })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Chaîne Privée", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("L'accès se fait uniquement via lien d'invitation secret.", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Switches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Autoriser les réactions aux messages", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Switch(checked = allowReactions, onCheckedChange = { allowReactions = it })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Autoriser les commentaires d'abonnés", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Switch(checked = allowComments, onCheckedChange = { allowComments = it })
                            }
                        }

                        4 -> {
                            // Step 4: Aperçu
                            Text(
                                text = "Aperçu de votre chaîne",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "Vérifiez les informations avant la création définitive.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                            )

                            // Preview Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(1.5.dp, PurplePrimary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(avatarPresets[selectedAvatarIndex], fontSize = 28.sp)
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = channelName.ifEmpty { "Chaîne sans nom" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = PurplePrimary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = if (isPublic) "Publique" else "Privée",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PurplePrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "@${username.ifEmpty { "nom.de.chaine" }} • $category • $language",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = description,
                                        fontSize = 13.sp,
                                        color = Color(0xFF334155),
                                        lineHeight = 18.sp
                                    )

                                    if (addedLinks.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Link,
                                                contentDescription = null,
                                                tint = PurplePrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${addedLinks.size} lien(s) ajouté(s)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PurplePrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = initialPost,
                                onValueChange = { initialPost = it },
                                label = { Text("Premier message sur la chaîne") },
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Notice
                Text(
                    text = "Votre compte peut créer jusqu’à 3 chaînes.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Navigation Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Précédent", color = Color(0xFF475569))
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Annuler", color = Color(0xFF64748B))
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < 4) {
                                currentStep++
                            } else {
                                onConfirm(channelName, description, isPublic, initialPost)
                            }
                        },
                        enabled = channelName.isNotBlank() && description.isNotBlank() && username.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.testTag("confirm_create_channel_button")
                    ) {
                        Text(if (currentStep == 4) "Créer la chaîne" else "Suivant")
                    }
                }
            }
        }
    }
}
