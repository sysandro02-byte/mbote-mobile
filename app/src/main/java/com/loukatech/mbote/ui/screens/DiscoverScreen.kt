package com.loukatech.mbote.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.DiscoverProfile
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    profiles: List<DiscoverProfile>,
    onOpenProfile: (DiscoverProfile) -> Unit,
    onSendGreeting: (DiscoverProfile) -> Unit,
    onStartChat: (DiscoverProfile) -> Unit,
    onOpenAronQuestions: () -> Unit,
    onStartEyeContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Tous") }
    var searchKeyword by remember { mutableStateOf("") }
    var showGreetedSnackbar by remember { mutableStateOf<String?>(null) }

    val filters = listOf("Tous", "Brazzaville", "Kinshasa", "Pointe-Noire", "Abidjan", "Tech", "Musique", "Art")

    val filteredProfiles = remember(profiles, selectedFilter, searchKeyword) {
        profiles.filter { p ->
            val matchesSearch = searchKeyword.isBlank() ||
                p.name.contains(searchKeyword, ignoreCase = true) ||
                p.city.contains(searchKeyword, ignoreCase = true) ||
                p.bio.contains(searchKeyword, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Tous" -> true
                "Brazzaville" -> p.city.equals("Brazzaville", ignoreCase = true)
                "Kinshasa" -> p.city.equals("Kinshasa", ignoreCase = true)
                "Pointe-Noire" -> p.city.equals("Pointe-Noire", ignoreCase = true)
                "Abidjan" -> p.city.equals("Abidjan", ignoreCase = true)
                "Tech" -> p.interests.any { it.contains("Tech", true) || it.contains("IA", true) }
                "Musique" -> p.interests.any { it.contains("Musique", true) || it.contains("Rumba", true) || it.contains("Chant", true) }
                "Art" -> p.interests.any { it.contains("Art", true) || it.contains("Design", true) || it.contains("Photo", true) }
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        snackbarHost = {
            if (showGreetedSnackbar != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showGreetedSnackbar = null }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    containerColor = MbotePurplePrimary
                ) {
                    Text("Salutation MBoté ! envoyée à $showGreetedSnackbar 👋")
                }
            }
        },
        modifier = modifier.fillMaxSize().testTag("discover_screen")
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Banner: The Aron Questions & Deep Human Connections
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(MbotePurplePrimary, Color(0xFF9333EA), Color(0xFFC084FC))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "✨ Connexions Humaines MBoté",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Les 36 Questions d'Arthur Aron",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "36 questions scientifiques et un exercice de 4 minutes pour approfondir vos relations et tisser des liens sincères.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onOpenAronQuestions,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MbotePurplePrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("explore_aron_questions_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QuestionAnswer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Voir les 36 questions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = onStartEyeContact,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("start_eye_contact_hero_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("4 min", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Column {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholder = { Text("Rechercher par ville, intérêt ou prénom...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MbotePurplePrimary)
                        },
                        trailingIcon = if (searchKeyword.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchKeyword = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Effacer")
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discover_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MbotePurplePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Profils de la communauté (${filteredProfiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trié par affinité ✨",
                        style = MaterialTheme.typography.bodySmall,
                        color = MbotePurplePrimary
                    )
                }
            }

            // Profile Cards
            items(filteredProfiles, key = { it.id }) { profile ->
                DiscoverProfileCard(
                    profile = profile,
                    onOpenProfile = { onOpenProfile(profile) },
                    onSendGreeting = {
                        onSendGreeting(profile)
                        showGreetedSnackbar = profile.name
                    },
                    onStartChat = { onStartChat(profile) }
                )
            }
        }
    }
}

@Composable
fun DiscoverProfileCard(
    profile: DiscoverProfile,
    onOpenProfile: () -> Unit,
    onSendGreeting: () -> Unit,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenProfile() }
            .testTag("discover_card_${profile.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with online dot
                Box {
                    AsyncImage(
                        model = profile.avatar,
                        contentDescription = profile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${profile.name}, ${profile.age}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Vérifié",
                            tint = MbotePurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${profile.city}, ${profile.country}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Match Affinity Badge & 3-Dots Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MbotePurpleSoft
                    ) {
                        Text(
                            text = "${profile.matchAffinity}% affinité",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MbotePurplePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    var showDiscoverMenu by remember { mutableStateOf(false) }
                    val context = androidx.compose.ui.platform.LocalContext.current

                    Box {
                        IconButton(
                            onClick = { showDiscoverMenu = true },
                            modifier = Modifier.size(32.dp).testTag("discover_card_more_${profile.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options profil",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDiscoverMenu,
                            onDismissRequest = { showDiscoverMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("👋 Envoyer un salut MBoté") },
                                leadingIcon = { Icon(Icons.Default.WavingHand, contentDescription = null, tint = MbotePurplePrimary) },
                                onClick = {
                                    showDiscoverMenu = false
                                    onSendGreeting()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("💬 Démarrer une discussion") },
                                leadingIcon = { Icon(Icons.Outlined.Chat, contentDescription = null) },
                                onClick = {
                                    showDiscoverMenu = false
                                    onStartChat()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔮 Proposer une Question d'Aron") },
                                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MbotePurplePrimary) },
                                onClick = {
                                    showDiscoverMenu = false
                                    android.widget.Toast.makeText(context, "Question d'Aron envoyée à ${profile.name}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📤 Partager ce profil") },
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                onClick = {
                                    showDiscoverMenu = false
                                    android.widget.Toast.makeText(context, "Profil de ${profile.name} copié", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("🙈 Masquer ce profil") },
                                leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                                onClick = {
                                    showDiscoverMenu = false
                                    android.widget.Toast.makeText(context, "Profil de ${profile.name} masqué", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🚩 Signaler") },
                                leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showDiscoverMenu = false
                                    android.widget.Toast.makeText(context, "Profil de ${profile.name} signalé aux modérateurs", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bio
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Favorite Aron Question Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "🔮", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Question d'Aron favorite :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MbotePurplePrimary
                        )
                        Text(
                            text = "« ${profile.favoriteAronQuestion} »",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interests tags
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(profile.interests) { interest ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MbotePurpleSoft.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "#$interest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MbotePurplePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSendGreeting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MbotePurplePrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("greet_btn_${profile.id}")
                ) {
                    Text("Dire MBoté ! 👋", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStartChat,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MbotePurplePrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("chat_btn_${profile.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discuter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
