package com.loukatech.mbote.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.JobOffer
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    jobs: List<JobOffer>,
    onBackClick: () -> Unit,
    onLikeJob: (String) -> Unit,
    onBookmarkJob: (String) -> Unit = {},
    onApplyJob: (String) -> Unit = {},
    onPostJob: (
        title: String,
        company: String,
        location: String,
        domain: String,
        contractType: String,
        workMode: String,
        salary: String,
        description: String,
        requirements: List<String>,
        benefits: List<String>
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onShareJob: (JobOffer) -> Unit = {},
    onReportJob: (JobOffer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf("Tous") }
    var selectedContractType by remember { mutableStateOf("Tous") }
    var showOnlySaved by remember { mutableStateOf(false) }

    var selectedJobForDetails by remember { mutableStateOf<JobOffer?>(null) }
    var appliedJobTitle by remember { mutableStateOf<String?>(null) }
    var showPostJobDialog by remember { mutableStateOf(false) }

    val domains = listOf(
        "Tous",
        "Ingénierie Logicielle",
        "Télécoms & Réseaux",
        "Design & Ergonomie",
        "Finance & Fintech",
        "IA & Data",
        "Marketing & Ventes"
    )

    val contractTypes = listOf("Tous", "CDI", "CDD", "Stage", "Freelance", "Télétravail")

    val filteredJobs = jobs.filter { job ->
        val matchesDomain = (selectedDomain == "Tous" || job.domain == selectedDomain)
        val matchesContract = when (selectedContractType) {
            "Tous" -> true
            "Télétravail" -> job.workMode.contains("Télétravail", ignoreCase = true) || job.location.contains("Télétravail", ignoreCase = true)
            else -> job.contractType.equals(selectedContractType, ignoreCase = true) || job.duration.contains(selectedContractType, ignoreCase = true)
        }
        val matchesSaved = !showOnlySaved || job.isSaved
        val matchesQuery = searchQuery.isBlank() ||
                job.title.contains(searchQuery, ignoreCase = true) ||
                job.company.contains(searchQuery, ignoreCase = true) ||
                job.location.contains(searchQuery, ignoreCase = true) ||
                job.description.contains(searchQuery, ignoreCase = true) ||
                job.requirements.any { it.contains(searchQuery, ignoreCase = true) }

        matchesDomain && matchesContract && matchesSaved && matchesQuery
    }

    if (appliedJobTitle != null) {
        AlertDialog(
            onDismissRequest = { appliedJobTitle = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Candidature transmise !",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Votre profil professionnel MBoté et vos coordonnées ont été envoyés avec succès au recruteur pour le poste : \"$appliedJobTitle\".",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { appliedJobTitle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Compris", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPostJobDialog) {
        PostJobOfferDialog(
            onDismiss = { showPostJobDialog = false },
            onPost = { title, company, location, domain, contractType, workMode, salary, description, reqs, bens ->
                onPostJob(title, company, location, domain, contractType, workMode, salary, description, reqs, bens)
                showPostJobDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("jobs_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = MbotePurplePrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emplois & Recrutement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1B4B)
                            )
                            Text(
                                text = "${jobs.size} offres actives • MBoté Talents",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }

                        // Bookmark Filter Toggle Button
                        Surface(
                            onClick = { showOnlySaved = !showOnlySaved },
                            shape = RoundedCornerShape(14.dp),
                            color = if (showOnlySaved) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (showOnlySaved) MbotePurplePrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("saved_jobs_filter_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (showOnlySaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Favoris",
                                    tint = if (showOnlySaved) MbotePurplePrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Top Header 3-Dots Menu
                        var showHeaderJobsMenu by remember { mutableStateOf(false) }
                        val context = androidx.compose.ui.platform.LocalContext.current

                        Box {
                            Surface(
                                onClick = { showHeaderJobsMenu = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("jobs_header_more_vert")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options Emploi",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showHeaderJobsMenu,
                                onDismissRequest = { showHeaderJobsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("➕ Publier une offre d'emploi") },
                                    leadingIcon = { Icon(Icons.Default.AddBusiness, contentDescription = null, tint = MbotePurplePrimary) },
                                    onClick = {
                                        showHeaderJobsMenu = false
                                        showPostJobDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔖 Mes candidatures enregistrées") },
                                    leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
                                    onClick = {
                                        showHeaderJobsMenu = false
                                        showOnlySaved = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📄 Importer mon CV MBoté (PDF)") },
                                    leadingIcon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                                    onClick = {
                                        showHeaderJobsMenu = false
                                        android.widget.Toast.makeText(context, "CV MBoté prêt pour la postulation en 1-clic", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("🔔 Configurer les alertes emploi") },
                                    leadingIcon = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
                                    onClick = {
                                        showHeaderJobsMenu = false
                                        android.widget.Toast.makeText(context, "Alertes activées pour vos domaines préférés", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // Post Job Button
                        Button(
                            onClick = { showPostJobDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("post_job_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publier", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Search Bar Inside Jobs Top Header
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF3F4F6)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Rechercher",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Poste, entreprise, compétences (ex: Kotlin, Télécom)...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("jobs_search_input")
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Domain Horizontal Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        domains.forEach { domain ->
                            val isSelected = (selectedDomain == domain)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MbotePurplePrimary else Color(0xFFF3E8FF).copy(alpha = 0.5f),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedDomain = domain }
                                    .testTag("domain_chip_$domain")
                            ) {
                                Text(
                                    text = domain,
                                    color = if (isSelected) Color.White else Color(0xFF4C1D95),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Contract Type Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        contractTypes.forEach { type ->
                            val isSelected = (selectedContractType == type)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF1E1B4B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF1E1B4B) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedContractType = type }
                                    .testTag("contract_chip_$type")
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        if (filteredJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFFAF9FD)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WorkOutline,
                            contentDescription = null,
                            tint = MbotePurplePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune offre trouvée",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B4B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Essayez d'ajuster vos critères ou filtres pour découvrir d'autres opportunités.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            searchQuery = ""
                            selectedDomain = "Tous"
                            selectedContractType = "Tous"
                            showOnlySaved = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Réinitialiser les filtres", color = MbotePurplePrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFFAF9FD)),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Header Banner inside list
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF4C1D95)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Trouvez votre prochain défi 🚀",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Postulez directement avec votre profil et CV MBoté en un clic.",
                                    color = Color(0xFFDDD6FE),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                items(filteredJobs, key = { it.id }) { job ->
                    JobOfferCard(
                        job = job,
                        onCardClick = { selectedJobForDetails = job },
                        onLike = { onLikeJob(job.id) },
                        onBookmark = { onBookmarkJob(job.id) },
                        onApply = {
                            onApplyJob(job.id)
                            appliedJobTitle = job.title
                        },
                        onShare = { onShareJob(job) },
                        onReport = { onReportJob(job) }
                    )
                }
            }
        }

        // Job Details Bottom Sheet
        selectedJobForDetails?.let { job ->
            JobDetailsBottomSheet(
                job = job,
                onDismiss = { selectedJobForDetails = null },
                onLike = { onLikeJob(job.id) },
                onBookmark = { onBookmarkJob(job.id) },
                onApply = {
                    onApplyJob(job.id)
                    appliedJobTitle = job.title
                    selectedJobForDetails = null
                },
                onShare = { onShareJob(job) }
            )
        }
    }
}

@Composable
fun JobOfferCard(
    job: JobOffer,
    onCardClick: () -> Unit,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCardClick() }
            .testTag("job_card_${job.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Company Avatar + Name & Job Title + Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Company Logo / Initial Squircle
                if (!job.companyLogo.isNullOrBlank()) {
                    AsyncImage(
                        model = job.companyLogo,
                        contentDescription = job.company,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = job.company.take(2).uppercase(),
                            color = MbotePurplePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = job.company,
                            style = MaterialTheme.typography.bodySmall,
                            color = MbotePurplePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " • ${job.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Bookmark Icon & 3-Dots Options Menu
                var showJobCardMenu by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBookmark,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (job.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Favori",
                            tint = if (job.isSaved) MbotePurplePrimary else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showJobCardMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options de l'offre",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showJobCardMenu,
                            onDismissRequest = { showJobCardMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (job.isSaved) "🔖 Retirer des enregistrés" else "🔖 Enregistrer l'offre") },
                                leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                                onClick = {
                                    showJobCardMenu = false
                                    onBookmark()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📤 Partager l'offre") },
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                onClick = {
                                    showJobCardMenu = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📋 Copier les détails") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showJobCardMenu = false
                                    android.widget.Toast.makeText(context, "Détails de l'offre ${job.title} copiés", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("🚩 Signaler cette annonce") },
                                leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showJobCardMenu = false
                                    onReport()
                                    android.widget.Toast.makeText(context, "Signalement envoyé à l'administration MBoté. Merci pour votre vigilance ! 🚩", android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row: Domain, Contract, Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEDE9FE)
                ) {
                    Text(
                        text = job.contractType.ifBlank { job.duration },
                        color = Color(0xFF5B21B6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = job.workMode,
                        color = Color(0xFF334155),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = job.domain,
                        color = Color(0xFF92400E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description Snippet
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Bar: Salary, Likes, Apply Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rémunération",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = job.salary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Like button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLike() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (job.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "J'aime",
                            tint = if (job.isLiked) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = job.likesCount.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Quick Apply Button
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("apply_job_button_${job.id}")
                    ) {
                        Text("Postuler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsBottomSheet(
    job: JobOffer,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onApply: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = job.company.take(2).uppercase(),
                        color = MbotePurplePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${job.company} • ${job.location}",
                        fontSize = 13.sp,
                        color = MbotePurplePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onBookmark) {
                    Icon(
                        imageVector = if (job.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Favori",
                        tint = if (job.isSaved) MbotePurplePrimary else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Info Row (Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBox(
                    title = "Contrat",
                    value = job.contractType.ifBlank { job.duration },
                    modifier = Modifier.weight(1f)
                )
                InfoBox(
                    title = "Mode",
                    value = job.workMode,
                    modifier = Modifier.weight(1f)
                )
                InfoBox(
                    title = "Candidatures",
                    value = "${job.applicantsCount} reçues",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description
            Text(
                text = "Description du poste",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1B4B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = job.description,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                lineHeight = 19.sp
            )

            // Requirements
            if (job.requirements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Profil recherché & Compétences",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                job.requirements.forEach { req ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", color = MbotePurplePrimary, fontWeight = FontWeight.Bold)
                        Text(
                            text = req,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Benefits
            if (job.benefits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Avantages & Rémunération",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Salaire : ${job.salary}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                job.benefits.forEach { ben ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("✓ ", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        Text(
                            text = ben,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MbotePurplePrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager", color = MbotePurplePrimary)
                }

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(2f)
                        .testTag("modal_apply_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Postuler maintenant", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PostJobOfferDialog(
    onDismiss: () -> Unit,
    onPost: (
        title: String,
        company: String,
        location: String,
        domain: String,
        contractType: String,
        workMode: String,
        salary: String,
        description: String,
        requirements: List<String>,
        benefits: List<String>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Brazzaville, Congo") }
    var domain by remember { mutableStateOf("Ingénierie Logicielle") }
    var contractType by remember { mutableStateOf("CDI") }
    var workMode by remember { mutableStateOf("Hybride") }
    var salary by remember { mutableStateOf("Selon profil (FCFA)") }
    var description by remember { mutableStateOf("") }
    var requirementsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Publier une offre d'emploi", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Intitulé du poste *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Nom de l'entreprise *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contractType,
                        onValueChange = { contractType = it },
                        label = { Text("Contrat (CDI, CDD...)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = workMode,
                        onValueChange = { workMode = it },
                        label = { Text("Mode (Hybride, Remote)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Rémunération / Fourchette FCFA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description des missions *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = requirementsText,
                    onValueChange = { requirementsText = it },
                    label = { Text("Compétences requises (séparées par des virgules)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && company.isNotBlank() && description.isNotBlank()) {
                        val reqs = requirementsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onPost(
                            title,
                            company,
                            location,
                            domain,
                            contractType,
                            workMode,
                            salary,
                            description,
                            reqs,
                            listOf("Mutuelle santé", "Cadre de travail stimulant")
                        )
                    }
                },
                enabled = title.isNotBlank() && company.isNotBlank() && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
            ) {
                Text("Publier l'offre")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
