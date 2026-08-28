package com.loukatech.mbote.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loukatech.mbote.ui.viewmodel.MboteViewModel
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
import com.loukatech.mbote.data.MastaData
import com.loukatech.mbote.model.MastaSubOption
import com.loukatech.mbote.model.MastaUser
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MastaScreen(
    viewModel: MboteViewModel,
    onOpenChat: (String) -> Unit = {},
    onStartCall: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onOpenSettings: () -> Unit = {},
    onOpenProfile: (MastaUser) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(MastaSubOption.ONLINE) }
    var selectedCityFilter by remember { mutableStateOf("Toutes") }
    var searchQuery by remember { mutableStateOf("") }
    var showContactBanner by remember { mutableStateOf(true) }

    // Dynamic state for masta users list from ViewModel
    val mastaUsersList by viewModel.mastaUsers.collectAsStateWithLifecycle()

    val receivedCount = remember(mastaUsersList) {
        mastaUsersList.count { it.subType == MastaSubOption.RECEIVED }
    }
    val sentCount = remember(mastaUsersList) {
        mastaUsersList.count { it.subType == MastaSubOption.SENT }
    }
    val onlineCount = remember(mastaUsersList) {
        mastaUsersList.count { it.isOnline || it.subType == MastaSubOption.ONLINE }
    }

    val filteredList = remember(mastaUsersList, selectedOption, selectedCityFilter, searchQuery) {
        mastaUsersList.filter { u ->
            val matchesSearch = searchQuery.isBlank() ||
                    u.name.contains(searchQuery, ignoreCase = true) ||
                    u.infoSubtitle.contains(searchQuery, ignoreCase = true) ||
                    u.city.contains(searchQuery, ignoreCase = true)

            val matchesCity = selectedCityFilter == "Toutes" || u.city.equals(selectedCityFilter, ignoreCase = true)

            val matchesTab = when (selectedOption) {
                MastaSubOption.ONLINE -> u.isOnline || u.subType == MastaSubOption.ONLINE
                MastaSubOption.RECEIVED -> u.subType == MastaSubOption.RECEIVED
                MastaSubOption.SENT -> u.subType == MastaSubOption.SENT
                MastaSubOption.FRIENDS -> u.subType == MastaSubOption.FRIENDS || u.isOnline
                MastaSubOption.SUGGESTIONS -> u.subType == MastaSubOption.SUGGESTIONS
                MastaSubOption.RECOMMENDATIONS -> u.subType == MastaSubOption.RECOMMENDATIONS || u.subType == MastaSubOption.SUGGESTIONS
                MastaSubOption.CITIES -> true
            }

            matchesSearch && matchesCity && matchesTab
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MbotePurplePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Diversity3,
                                    contentDescription = null,
                                    tint = MbotePurplePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "Masta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    Surface(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("masta_header_settings")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Paramètres",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize().testTag("masta_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Dedicated search bar (always visible)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher un ami, ville ou profession...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MbotePurplePrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MbotePurplePrimary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("masta_search_bar")
            )

            // Top Horizontal Filter Chips Bar (Matching Screenshots 1 & 5)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    // Online Badge Chip
                    FilterChip(
                        selected = selectedOption == MastaSubOption.ONLINE,
                        onClick = { selectedOption = MastaSubOption.ONLINE },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text("$onlineCount en ligne")
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                item {
                    // Received Requests Chip
                    FilterChip(
                        selected = selectedOption == MastaSubOption.RECEIVED,
                        onClick = { selectedOption = MastaSubOption.RECEIVED },
                        label = {
                            Text(if (receivedCount > 0) "Reçu ($receivedCount)" else "Reçu")
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                item {
                    // Sent Requests Chip
                    FilterChip(
                        selected = selectedOption == MastaSubOption.SENT,
                        onClick = { selectedOption = MastaSubOption.SENT },
                        label = {
                            Text(if (sentCount > 0) "Envoyés ($sentCount)" else "Envoyés")
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                item {
                    // Friends Chip
                    FilterChip(
                        selected = selectedOption == MastaSubOption.FRIENDS,
                        onClick = { selectedOption = MastaSubOption.FRIENDS },
                        label = { Text("Vos ami(e)s") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                item {
                    // Suggestions Chip
                    FilterChip(
                        selected = selectedOption == MastaSubOption.SUGGESTIONS,
                        onClick = { selectedOption = MastaSubOption.SUGGESTIONS },
                        label = { Text("Suggestions") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                item {
                    // Recommendations Chip
                    FilterChip(
                        selected = selectedOption == MastaSubOption.RECOMMENDATIONS,
                        onClick = { selectedOption = MastaSubOption.RECOMMENDATIONS },
                        label = { Text("Recommandations") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                item {
                    // Cities Chip
                    FilterChip(
                        selected = selectedOption == MastaSubOption.CITIES,
                        onClick = { selectedOption = MastaSubOption.CITIES },
                        label = { Text("Villes") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // City Sub-filter Row if Cities selected
            if (selectedOption == MastaSubOption.CITIES) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cities = listOf("Toutes", "Brazzaville", "Pointe-Noire", "Kinshasa", "Dolisie", "Libreville")
                    items(cities) { city ->
                        AssistChip(
                            onClick = { selectedCityFilter = city },
                            label = { Text(city) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCityFilter == city) MbotePurplePrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            // Main Content List
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Section Title & Counter
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headerTitle = when (selectedOption) {
                            MastaSubOption.ONLINE -> "Amis en ligne"
                            MastaSubOption.RECEIVED -> "Invitations"
                            MastaSubOption.SENT -> "Invitations envoyées"
                            MastaSubOption.FRIENDS -> "Vos ami(e)s"
                            MastaSubOption.SUGGESTIONS -> "Personnes que vous pourriez connaître"
                            MastaSubOption.RECOMMENDATIONS -> "Recommandé pour vous"
                            MastaSubOption.CITIES -> "Masta par ville ($selectedCityFilter)"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = headerTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedOption == MastaSubOption.RECEIVED) {
                                Text(
                                    text = "$receivedCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }

                        if (selectedOption == MastaSubOption.RECEIVED) {
                            TextButton(onClick = {
                                Toast.makeText(context, "Tri par date d'invitation", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Trier", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            TextButton(onClick = {
                                Toast.makeText(context, "Mise à jour du flux Masta", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Voir tout", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Contact Import Banner Card (Matching Screenshot 4 exactly)
                if ((selectedOption == MastaSubOption.SUGGESTIONS || selectedOption == MastaSubOption.RECOMMENDATIONS || selectedOption == MastaSubOption.FRIENDS) && showContactBanner) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ContactPhone,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Activez l'importation des contacts pour trouver vos amis",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 19.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { showContactBanner = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Fermer",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "Cela vous aidera à retrouver vos amis déjà présents sur MBoté.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { showContactBanner = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Plus tard", fontWeight = FontWeight.Medium)
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Importation des contacts activée !", Toast.LENGTH_SHORT).show()
                                            showContactBanner = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDBEAFE),
                                            contentColor = Color(0xFF1D4ED8)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Activer", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Empty State if no profiles match filter
                if (filteredList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucun Masta trouvé dans cette section",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // User Cards List (Matching Image 1, 2, 3, 4, 5 layouts)
                items(filteredList, key = { it.id }) { mastaUser ->
                    MastaUserItemCard(
                        user = mastaUser,
                        activeTab = selectedOption,
                        onConfirmClick = {
                            val updated = mastaUsersList.map {
                                if (it.id == mastaUser.id) it.copy(subType = MastaSubOption.FRIENDS, isOnline = true) else it
                            }
                            viewModel.updateMastaUsers(updated)
                            Toast.makeText(context, "Invitation de ${mastaUser.name} acceptée 🎉", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteClick = {
                            val updated = mastaUsersList.filter { it.id != mastaUser.id }
                            viewModel.updateMastaUsers(updated)
                            Toast.makeText(context, "Invitation supprimée", Toast.LENGTH_SHORT).show()
                        },
                        onCancelSentClick = {
                            val updated = mastaUsersList.filter { it.id != mastaUser.id }
                            viewModel.updateMastaUsers(updated)
                            Toast.makeText(context, "Invitation annulée", Toast.LENGTH_SHORT).show()
                        },
                        onAddFriendClick = {
                            val updated = mastaUsersList.map {
                                if (it.id == mastaUser.id) it.copy(subType = MastaSubOption.SENT, timeBadge = "À l'instant") else it
                            }
                            viewModel.updateMastaUsers(updated)
                            Toast.makeText(context, "Invitation envoyée à ${mastaUser.name} 📤", Toast.LENGTH_SHORT).show()
                        },
                        onChatClick = { onOpenChat(mastaUser.name) },
                        onCallClick = { onStartCall(mastaUser.name, mastaUser.avatar, false) },
                        onVideoCallClick = { onStartCall(mastaUser.name, mastaUser.avatar, true) },
                        onProfileClick = { onOpenProfile(mastaUser) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MastaUserItemCard(
    user: MastaUser,
    activeTab: MastaSubOption,
    onConfirmClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelSentClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onChatClick: () -> Unit,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (user.id == "linda_bongo" || user.name == "Linda Bongo Ondimba") {
                    onProfileClick()
                } else {
                    onChatClick()
                }
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large Avatar with Optional Online Badge / Time Badge
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = user.avatar,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            )

            if (user.isOnline || activeTab == MastaSubOption.ONLINE) {
                // Bright Green Online Dot Badge (Matching Image 1)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            } else if (!user.timeBadge.isNullOrEmpty() && activeTab == MastaSubOption.RECEIVED) {
                // Time pill badge on bottom right (e.g. 10h, 8h - Image 2)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = user.timeBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Details Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Time Sent display for Sent tab (Image 3)
                if (activeTab == MastaSubOption.SENT && !user.timeBadge.isNullOrEmpty()) {
                    Text(
                        text = user.timeBadge,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mutual Friends Row with overlapping mini avatars (Images 2, 4)
            if (user.mutualFriendsCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (user.mutualFriendsAvatars.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            user.mutualFriendsAvatars.take(2).forEach { av ->
                                AsyncImage(
                                    model = av,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (user.mutualFriendsCount == 1) "1 ami(e) en commun" else "${user.mutualFriendsCount} ami(e)s en commun",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = user.infoSubtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Buttons Section per Tab (Matching Images 1, 2, 3, 4 exactly)
            when (activeTab) {
                MastaSubOption.RECEIVED -> {
                    // Two side-by-side buttons: Confirmer (Blue) & Supprimer (Gray) - Image 2 & 5
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirmClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Supprimer", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }

                MastaSubOption.SENT -> {
                    // Single wide button: Annuler l'invitation (Gray) - Image 3
                    Button(
                        onClick = onCancelSentClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text("Annuler l'invitation", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }

                MastaSubOption.SUGGESTIONS, MastaSubOption.RECOMMENDATIONS -> {
                    // Two side-by-side buttons: Ajouter ami(e) (Blue) & Supprimer (Gray) - Image 4
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddFriendClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ajouter ami(e)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Supprimer", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }

                MastaSubOption.ONLINE, MastaSubOption.FRIENDS, MastaSubOption.CITIES -> {
                    // Quick Chat / Call icons for active friends - Image 1
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onChatClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MbotePurplePrimary.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Discussion",
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onCallClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Appel vocal",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onVideoCallClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Appel vidéo",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
