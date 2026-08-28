package com.loukatech.mbote.ui.components

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loukatech.mbote.model.BadgeType
import com.loukatech.mbote.model.GiftItem
import com.loukatech.mbote.model.defaultGiftItems
import com.loukatech.mbote.service.api.AdminStatsData
import com.loukatech.mbote.service.api.MboteBackendConfig
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AdminTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OVERVIEW("Vue Globale", Icons.Outlined.Dashboard),
    USERS("Utilisateurs", Icons.Outlined.People),
    CHANNELS("Chaînes", Icons.Outlined.Campaign),
    GIFTS_BADGES("Boutique & Badges", Icons.Outlined.CardGiftcard),
    JOBS_FINANCE("Emplois & MoMo", Icons.Outlined.Payments),
    SYSTEM("Système", Icons.Outlined.Settings),
    REPORTS("Signalements 🚩", Icons.Outlined.Report)
}

data class AdminUserItem(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    var role: String, // "Admin", "Modérateur", "Utilisateur"
    var isVerified: Boolean,
    var isBanned: Boolean,
    val joinedDate: String,
    val city: String
)

data class AdminJobItem(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val salary: String,
    var status: String // "En attente", "Approuvé", "Rejeté"
)

data class AdminChannelItem(
    val id: String,
    val name: String,
    val owner: String,
    val subscribersCount: Int,
    var isVerified: Boolean,
    var isPinned: Boolean,
    val reportsCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onAdminLogin: suspend (adminKey: String, email: String, pass: String) -> Result<AdminStatsData>,
    onSaveServerConfig: (url: String) -> Unit,
    viewModel: com.loukatech.mbote.ui.viewmodel.MboteViewModel? = null
) {
    var adminKey by remember { mutableStateOf("MBOTE-ADMIN-2026") }
    var email by remember { mutableStateOf("admin@loukatech.com") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var adminStats by remember { mutableStateOf<AdminStatsData?>(null) }
    var showServerConfig by remember { mutableStateOf(false) }
    var customServerUrl by remember { mutableStateOf(MboteBackendConfig.baseUrl) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1120), // Deep space modern admin dark theme
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 2.dp)
                .testTag("admin_login_dialog")
        ) {
            if (adminStats != null) {
                // FULL ENTERPRISE ADMIN CONSOLE
                AdminDashboardContent(
                    stats = adminStats!!,
                    onDismiss = onDismiss,
                    showServerConfig = showServerConfig,
                    onToggleServerConfig = { showServerConfig = !showServerConfig },
                    customServerUrl = customServerUrl,
                    onServerUrlChange = { customServerUrl = it },
                    onSaveServerUrl = {
                        MboteBackendConfig.baseUrl = customServerUrl
                        onSaveServerConfig(customServerUrl)
                    },
                    viewModel = viewModel
                )
            } else {
                // ADMIN LOGIN FORM VIEW
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Shield Security Badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF59E0B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Console d'Administration MBoté",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Système de supervision et gestion centrale LoukaTech 🇨🇬",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Notice / Preset Hint
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clé Admin par défaut : MBOTE-ADMIN-2026",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    errorMessage?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Admin Secret Key Input
                    OutlinedTextField(
                        value = adminKey,
                        onValueChange = { adminKey = it; errorMessage = null },
                        label = { Text("Clé Secrète Admin (Master Key)") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Key, contentDescription = null, tint = Color(0xFFA78BFA))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFA78BFA),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Admin Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email Administrateur") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Badge, contentDescription = null, tint = Color(0xFFA78BFA))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFA78BFA),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Mot de passe Maître (Optionnel)") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFA78BFA))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFA78BFA),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (adminKey.isBlank()) {
                                errorMessage = "Veuillez renseigner la clé secrète admin."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = onAdminLogin(adminKey.trim(), email.trim(), password)
                                isLoading = false
                                if (result.isSuccess) {
                                    adminStats = result.getOrNull()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Accès refusé. Vérifiez vos droits."
                                }
                            }
                        },
                        enabled = !isLoading && adminKey.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_login_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Connexion Sécurisée", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fermer et retourner", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardContent(
    stats: AdminStatsData,
    onDismiss: () -> Unit,
    showServerConfig: Boolean,
    onToggleServerConfig: () -> Unit,
    customServerUrl: String,
    onServerUrlChange: (String) -> Unit,
    onSaveServerUrl: () -> Unit,
    viewModel: com.loukatech.mbote.ui.viewmodel.MboteViewModel? = null
) {
    var currentAdminTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    val coroutineScope = rememberCoroutineScope()
    var actionToast by remember { mutableStateOf<String?>(null) }
    val reportsList by (viewModel?.reports ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState(initial = emptyList())

    // Sample admin users
    var adminUsers by remember {
        mutableStateOf(
            listOf(
                AdminUserItem("u_1", "Michel Loutala", "m.loutala@gmail.com", "+242 06 800 00 01", "Admin", true, false, "12 Jan 2026", "Brazzaville"),
                AdminUserItem("u_2", "Aïcha Diallo", "aicha.diallo@afriq.cg", "+242 05 500 12 34", "Modérateur", true, false, "01 Fév 2026", "Pointe-Noire"),
                AdminUserItem("u_3", "Cedric Moukoko", "cedric.m@gmail.com", "+242 06 912 34 56", "Utilisateur", false, false, "15 Fév 2026", "Brazzaville"),
                AdminUserItem("u_4", "Spam Bot 242", "spambot@anonymous.net", "+242 04 000 99 99", "Utilisateur", false, true, "20 Fév 2026", "Inconnu")
            )
        )
    }

    // Sample jobs moderation
    var adminJobs by remember {
        mutableStateOf(
            listOf(
                AdminJobItem("j_1", "Développeur Mobile Android Kotlin", "LoukaTech R&D", "Brazzaville", "850.000 FCFA", "Approuvé"),
                AdminJobItem("j_2", "Responsable Logistique & Port", "Congo Transit", "Pointe-Noire", "600.000 FCFA", "En attente"),
                AdminJobItem("j_3", "Chef de Projet Énergie Solaire", "Solar Congo", "Oyo", "750.000 FCFA", "En attente")
            )
        )
    }

    // Sample channels moderation
    var adminChannels by remember {
        mutableStateOf(
            listOf(
                AdminChannelItem("ch_1", "MBoté Officiel", "LoukaTech", 4820, true, true, 0),
                AdminChannelItem("ch_2", "Aventures & Découvertes", "Aïcha Diallo", 2310, true, false, 0),
                AdminChannelItem("ch_3", "Tech Congo", "Michel Loutala", 3150, true, false, 1),
                AdminChannelItem("ch_4", "Brazza & Kin Musique", "Collectif 242", 6840, true, false, 2)
            )
        )
    }

    // Broadcast message state
    var broadcastTitle by remember { mutableStateOf("Annonce MBoté") }
    var broadcastMessage by remember { mutableStateOf("") }
    var isBroadcasting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Admin Header
        Surface(
            color = Color(0xFF0F172A),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Studio MBoté",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF7C3AED).copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, Color(0xFF7C3AED))
                        ) {
                            Text(
                                text = "v2.6 Cluster",
                                color = Color(0xFFC084FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AdminTab.values()) { tab ->
                        val isSelected = currentAdminTab == tab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFA78BFA) else Color(0xFF334155)
                            ),
                            modifier = Modifier.clickable { currentAdminTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Toast Banner
        actionToast?.let { toast ->
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = toast, color = Color(0xFFD1FAE5), fontSize = 12.sp)
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (currentAdminTab) {
                AdminTab.OVERVIEW -> {
                    AdminOverviewTabContent(
                        stats = stats,
                        onSendAlert = {
                            currentAdminTab = AdminTab.SYSTEM
                        }
                    )
                }

                AdminTab.USERS -> {
                    AdminUsersTabContent(
                        users = adminUsers,
                        onToggleVerification = { userId ->
                            adminUsers = adminUsers.map { u ->
                                if (u.id == userId) {
                                    val newV = !u.isVerified
                                    actionToast = if (newV) "Badge vérifié ✓ accordé à ${u.name}" else "Badge vérifié retiré"
                                    u.copy(isVerified = newV)
                                } else u
                            }
                        },
                        onToggleBan = { userId ->
                            adminUsers = adminUsers.map { u ->
                                if (u.id == userId) {
                                    val newB = !u.isBanned
                                    actionToast = if (newB) "Compte ${u.name} suspendu ⛔" else "Compte ${u.name} réactivé ✅"
                                    u.copy(isBanned = newB)
                                } else u
                            }
                        },
                        onPromoteRole = { userId ->
                            adminUsers = adminUsers.map { u ->
                                if (u.id == userId) {
                                    val newRole = when (u.role) {
                                        "Utilisateur" -> "Modérateur"
                                        "Modérateur" -> "Admin"
                                        else -> "Utilisateur"
                                    }
                                    actionToast = "Rôle de ${u.name} mis à jour vers : $newRole"
                                    u.copy(role = newRole)
                                } else u
                            }
                        }
                    )
                }

                AdminTab.CHANNELS -> {
                    AdminChannelsTabContent(
                        channels = adminChannels,
                        onTogglePin = { channelId ->
                            adminChannels = adminChannels.map { ch ->
                                if (ch.id == channelId) {
                                    val newP = !ch.isPinned
                                    actionToast = if (newP) "Chaîne '${ch.name}' épinglée en tête d'Actus" else "Chaîne détachée"
                                    ch.copy(isPinned = newP)
                                } else ch
                            }
                        },
                        onToggleVerify = { channelId ->
                            adminChannels = adminChannels.map { ch ->
                                if (ch.id == channelId) {
                                    val newV = !ch.isVerified
                                    actionToast = if (newV) "Chaîne vérifiée avec succès ✓" else "Certification retirée"
                                    ch.copy(isVerified = newV)
                                } else ch
                            }
                        }
                    )
                }

                AdminTab.GIFTS_BADGES -> {
                    AdminGiftsBadgesTabContent(
                        onPriceUpdated = { giftName, newPrice ->
                            actionToast = "Prix de '$giftName' ajusté à $newPrice FCFA !"
                        },
                        onRestocked = { giftName, count ->
                            actionToast = "+$count '$giftName' approvisionnés en boutique !"
                        }
                    )
                }

                AdminTab.JOBS_FINANCE -> {
                    AdminJobsFinanceTabContent(
                        jobs = adminJobs,
                        stats = stats,
                        onApproveJob = { jobId ->
                            adminJobs = adminJobs.map { j ->
                                if (j.id == jobId) {
                                    actionToast = "Offre '${j.title}' approuvée et publiée !"
                                    j.copy(status = "Approuvé")
                                } else j
                            }
                        },
                        onRejectJob = { jobId ->
                            adminJobs = adminJobs.map { j ->
                                if (j.id == jobId) {
                                    actionToast = "Offre rejetée."
                                    j.copy(status = "Rejeté")
                                } else j
                            }
                        }
                    )
                }

                AdminTab.SYSTEM -> {
                    AdminSystemTabContent(
                        customServerUrl = customServerUrl,
                        onServerUrlChange = onServerUrlChange,
                        onSaveServerUrl = onSaveServerUrl,
                        broadcastTitle = broadcastTitle,
                        onBroadcastTitleChange = { broadcastTitle = it },
                        broadcastMessage = broadcastMessage,
                        onBroadcastMessageChange = { broadcastMessage = it },
                        isBroadcasting = isBroadcasting,
                        onSendBroadcast = {
                            if (broadcastMessage.isNotBlank()) {
                                isBroadcasting = true
                                coroutineScope.launch {
                                    delay(1200)
                                    isBroadcasting = false
                                    actionToast = "Notification push globale diffusée à tous les appareils connectés 🚀"
                                    broadcastMessage = ""
                                }
                            }
                        }
                    )
                }

                AdminTab.REPORTS -> {
                    AdminReportsTabContent(
                        reports = reportsList,
                        onUpdateStatus = { reportId, newStatus ->
                            viewModel?.updateReportStatus(reportId, newStatus)
                            actionToast = "Signalement mis à jour : statut '$newStatus' !"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewTabContent(
    stats: AdminStatsData,
    onSendAlert: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Server Cluster Status
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Infrastructure Backend LoukaTech",
                            color = Color(0xFFA78BFA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "99.98% Uptime",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AdminMetricTile("Latence Ping", "${MboteBackendConfig.lastPingMs} ms", Color(0xFF38BDF8))
                        AdminMetricTile("Charge CPU", "${stats.cpuUsagePercent}%", Color(0xFF10B981))
                        AdminMetricTile("RAM Serveur", "${stats.ramUsageMb} MB", Color(0xFFFBBF24))
                        AdminMetricTile("Taux d'Erreur", "0.01%", Color(0xFFA78BFA))
                    }
                }
            }
        }

        // Live Community Activity Grid
        item {
            Text(
                text = "Activité MBoté en Direct",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Utilisateurs Inscrits", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${stats.activeUsersCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("🟢 ${stats.onlineNowCount} en ligne", color = Color(0xFF10B981), fontSize = 10.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Messages Aujourd'hui", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${stats.totalMessagesToday}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("⚡ Chiffrement E2E Actif", color = Color(0xFFA78BFA), fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MBoté Shorts Vidéos", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${stats.shortVideosTotal}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("🔥 98.4% Taux de rétention", color = Color(0xFFEC4899), fontSize = 10.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pourboires Créateurs", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${stats.totalMobileMoneyTipsFcfa} FCFA", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("💳 MoMo & Airtel Money", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }

        // Quick Admin Broadcast Action Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0764).copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color(0xFF7C3AED))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Diffusion d'Urgence", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Envoyer une notification push immédiate", color = Color(0xFFD8B4FE), fontSize = 11.sp)
                    }
                    Button(
                        onClick = onSendAlert,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Diffuser 📣", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersTabContent(
    users: List<AdminUserItem>,
    onToggleVerification: (String) -> Unit,
    onToggleBan: (String) -> Unit,
    onPromoteRole: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = users.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.city.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher un utilisateur (nom, email, ville)...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color(0xFF334155)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredUsers, key = { it.id }) { user ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, if (user.isBanned) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (user.isBanned) Color(0xFFEF4444) else Color(0xFF7C3AED),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = user.name.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = user.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                                        if (user.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Vérifié", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text(text = "${user.email} • ${user.city}", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                                }
                            }

                            // Role Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (user.role) {
                                    "Admin" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    "Modérateur" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                    else -> Color(0xFF64748B).copy(alpha = 0.2f)
                                },
                                modifier = Modifier.clickable { onPromoteRole(user.id) }
                            ) {
                                Text(
                                    text = user.role,
                                    color = when (user.role) {
                                        "Admin" -> Color(0xFFFBBF24)
                                        "Modérateur" -> Color(0xFFC084FC)
                                        else -> Color(0xFFCBD5E1)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onToggleVerification(user.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                            ) {
                                Text(if (user.isVerified) "Retirer Badge" else "Vérifier ✓", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { onToggleBan(user.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (user.isBanned) Color(0xFF10B981) else Color(0xFFEF4444)
                                ),
                                border = BorderStroke(1.dp, if (user.isBanned) Color(0xFF10B981) else Color(0xFFEF4444))
                            ) {
                                Text(if (user.isBanned) "Réactiver" else "Suspendre", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminChannelsTabContent(
    channels: List<AdminChannelItem>,
    onTogglePin: (String) -> Unit,
    onToggleVerify: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Modération des Chaînes Publiques",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(channels, key = { it.id }) { channel ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = channel.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                if (channel.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                }
                                if (channel.isPinned) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.PushPin, contentDescription = "Épinglée", tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(text = "Propriétaire: ${channel.owner} • ${channel.subscribersCount} abonnés", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }

                        if (channel.reportsCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "⚠️ ${channel.reportsCount} signalements",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTogglePin(channel.id) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f))
                        ) {
                            Text(if (channel.isPinned) "Détacher" else "Épingler 📌", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { onToggleVerify(channel.id) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Text(if (channel.isVerified) "Certifiée ✓" else "Certifier", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminJobsFinanceTabContent(
    jobs: List<AdminJobItem>,
    stats: AdminStatsData,
    onApproveJob: (String) -> Unit,
    onRejectJob: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Validation des Offres d'Emploi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(jobs, key = { it.id }) { job ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = job.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                            Text(text = "${job.company} • ${job.location} • ${job.salary}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (job.status) {
                                "Approuvé" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                "Rejeté" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = job.status,
                                color = when (job.status) {
                                    "Approuvé" -> Color(0xFF34D399)
                                    "Rejeté" -> Color(0xFFFCA5A5)
                                    else -> Color(0xFFFBBF24)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (job.status == "En attente") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApproveJob(job.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Approuver ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onRejectJob(job.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                border = BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Text("Rejeter", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSystemTabContent(
    customServerUrl: String,
    onServerUrlChange: (String) -> Unit,
    onSaveServerUrl: () -> Unit,
    broadcastTitle: String,
    onBroadcastTitleChange: (String) -> Unit,
    broadcastMessage: String,
    onBroadcastMessageChange: (String) -> Unit,
    isBroadcasting: Boolean,
    onSendBroadcast: () -> Unit
) {
    var savedToast by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Global Broadcast Message Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF7C3AED))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Diffusion Push Globale (Tous les utilisateurs)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = onBroadcastTitleChange,
                        label = { Text("Titre de la notification") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = broadcastMessage,
                        onValueChange = onBroadcastMessageChange,
                        label = { Text("Message de diffusion push...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onSendBroadcast,
                        enabled = !isBroadcasting && broadcastMessage.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isBroadcasting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Diffuser la Notification Push 📢", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Server API Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "URL Racine du Serveur Backend LoukaTech", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customServerUrl,
                        onValueChange = onServerUrlChange,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onSaveServerUrl()
                            savedToast = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer et Reconnecter l'API", fontWeight = FontWeight.Bold)
                    }

                    if (savedToast) {
                        Text(
                            text = "✓ Nouvelle URL backend enregistrée avec succès !",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricTile(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = Color(0xFF94A3B8), fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminGiftsBadgesTabContent(
    onPriceUpdated: (giftName: String, newPrice: Long) -> Unit,
    onRestocked: (giftName: String, count: Int) -> Unit
) {
    var giftsList by remember { mutableStateOf(defaultGiftItems()) }
    var selectedGiftToEdit by remember { mutableStateOf<GiftItem?>(null) }
    var editPriceText by remember { mutableStateOf("") }
    var restockAmountText by remember { mutableStateOf("100") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Platform Revenue Overview for Admin
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Revenus Monétisation & Cadeaux",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.5.sp
                        )
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                            Text(
                                text = "Encaissement Admin Direct",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AdminMetricTile(title = "Revenus Cadeaux Vente", value = "1.845.000 F", color = Color(0xFFFFD700))
                        AdminMetricTile(title = "Revenus Badges VIP", value = "785.000 F", color = Color(0xFF38BDF8))
                        AdminMetricTile(title = "Total Encaissé", value = "2.630.000 F", color = Color(0xFF4ADE80))
                    }
                }
            }
        }

        // Section Badges VIP / Top Donateur / Créateur Certifié
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Statuts & Badges Payants de la Plateforme 👑",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BadgeType.values().forEach { badge ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(badge.emoji, fontSize = 16.sp)
                                Text(badge.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "${badge.priceFcfa} FCFA",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Section Ajustement des Prix et Approvisionnement des Stocks Cadeaux
        item {
            Text(
                text = "Gestion du Catalogue de Cadeaux (Prix & Stock) 🪙",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )
        }

        items(giftsList) { gift ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(gift.emoji, fontSize = 28.sp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = gift.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Prix : ${gift.priceFcfa} FCFA • Valeur Créateur : ${gift.priceFcfa} F",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Restock button
                        Button(
                            onClick = {
                                onRestocked(gift.name, 100)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+100 Stock", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Edit Price button
                        Button(
                            onClick = {
                                selectedGiftToEdit = gift
                                editPriceText = gift.priceFcfa.toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Prix ✏️", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal to change gift price
    selectedGiftToEdit?.let { gift ->
        AlertDialog(
            onDismissRequest = { selectedGiftToEdit = null },
            title = {
                Text("Ajuster le Prix de ${gift.emoji} ${gift.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nouveau prix unitaire en boutique (FCFA) :", fontSize = 12.sp)
                    OutlinedTextField(
                        value = editPriceText,
                        onValueChange = { editPriceText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = editPriceText.toLongOrNull() ?: gift.priceFcfa
                        if (newPrice > 0) {
                            giftsList = giftsList.map { g ->
                                if (g.id == gift.id) g.copy(priceFcfa = newPrice) else g
                            }
                            onPriceUpdated(gift.name, newPrice)
                            selectedGiftToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedGiftToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun AdminReportsTabContent(
    reports: List<com.loukatech.mbote.model.ReportItem>,
    onUpdateStatus: (String, String) -> Unit
) {
    if (reports.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Report,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aucun signalement actif",
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Gestion des Signalements 🚩",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "En tant qu'administrateur principal, vous recevez tous les signalements. Vous pouvez choisir de déléguer la résolution aux co-administrateurs ou aux modérateurs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(reports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Report,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[${report.type}]",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = report.targetName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = report.timestamp,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Raison : ${report.reason}",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "Signalé par : ${report.reporterName}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status Badge
                        val badgeColor = when (report.status) {
                            "Envoyé à l'Admin" -> Color(0xFFFBBF24)
                            "Transmis aux Co-Admins" -> Color(0xFF38BDF8)
                            "Transmis aux Modérateurs" -> Color(0xFFC084FC)
                            else -> Color(0xFF34D399) // Résolu
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = "Statut : ${report.status}",
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Delegate / Resolve Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (report.status == "Envoyé à l'Admin") {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(report.id, "Transmis aux Co-Admins") },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                                ) {
                                    Text("Aux Co-Admins 👥", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { onUpdateStatus(report.id, "Transmis aux Modérateurs") },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC084FC)),
                                    border = BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.4f))
                                ) {
                                    Text("Aux Modérateurs 🛡️", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (report.status != "Résolu") {
                                Button(
                                    onClick = { onUpdateStatus(report.id, "Résolu") },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Résoudre ✓", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
