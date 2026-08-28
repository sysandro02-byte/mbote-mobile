package com.loukatech.mbote.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loukatech.mbote.model.LinkedChildInfo
import com.loukatech.mbote.model.ParentalSubscriptionPlan
import com.loukatech.mbote.model.UserProfile
import com.loukatech.mbote.ui.components.ParentalControlPremiumBadge
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlPremiumScreen(
    userProfile: UserProfile,
    linkedChild: LinkedChildInfo,
    onBack: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenParentalSettings: () -> Unit,
    onUpgradePlan: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val plans = remember {
        listOf(
            ParentalSubscriptionPlan(
                id = "plan_monthly",
                title = "Bouclier Mensuel",
                priceFcfa = 2500L,
                period = "/mois",
                maxChildren = 1,
                isPopular = false,
                description = "Protection essentielle pour 1 enfant avec quota 2h & verrouillage nocturne.",
                features = listOf(
                    "Liaison QR code 1 compte enfant",
                    "Déconnexion automatique quota 2h/jour",
                    "Verrouillage nocturne (00h-06h)",
                    "Alerte SOS d'urgence Brevo & Push"
                )
            ),
            ParentalSubscriptionPlan(
                id = "plan_annual",
                title = "Bouclier Annuel Pro",
                priceFcfa = 24000L,
                period = "/an",
                discount = "-20%",
                maxChildren = 2,
                isPopular = true,
                description = "La formule recommandée pour 2 enfants avec 2 mois offerts et audit 30j.",
                features = listOf(
                    "Liaison QR code jusqu'à 2 enfants",
                    "Toutes les fonctionnalités mensuelles incluses",
                    "Couvre-feu des commentaires (dès 20h)",
                    "Audit hebdomadaire & journal d'alertes",
                    "Support prioritaire LoukaTech 24/7"
                )
            ),
            ParentalSubscriptionPlan(
                id = "plan_family",
                title = "Bouclier Famille Totale",
                priceFcfa = 4000L,
                period = "/mois",
                maxChildren = 5,
                isPopular = false,
                description = "Tranquillité absolue pour toute la fratrie jusqu'à 5 enfants.",
                features = listOf(
                    "Liaison QR code jusqu'à 5 enfants",
                    "Surveillance multi-appareils en simultané",
                    "Alertes SMS & WhatsApp d'urgence",
                    "Tableau de bord partagé avec les deux parents",
                    "Rapports mensuels de temps d'écran par enfant"
                )
            )
        )
    }

    var selectedPlanId by remember { mutableStateOf("plan_annual") }
    var selectedPaymentMethod by remember { mutableStateOf("Orange Money") }
    var showPaymentSuccessSheet by remember { mutableStateOf(false) }

    val goldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFFD97706))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bouclier Parental Pro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "👑 PREMIUM",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenParentalSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Paramètres de contrôle",
                            tint = MbotePurplePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOpenQrScanner,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("premium_screen_scan_qr_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanner QR", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val plan = plans.firstOrNull { it.id == selectedPlanId } ?: plans.first()
                            onUpgradePlan(plan.id, plan.priceFcfa)
                            showPaymentSuccessSheet = true
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("premium_screen_activate_plan_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Activer mon Pack", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier.testTag("parental_control_premium_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status & Active Subscription Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            brush = goldGradient,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Bouclier Parental Actif",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Plan Annuel Pro • Renouvellement 28/09/2026",
                                        fontSize = 11.sp,
                                        color = Color(0xFFC7D2FE)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF22C55E).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF22C55E))
                            ) {
                                Text(
                                    text = "EN LIGNE ✓",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Linked Child Quick Card
                        if (userProfile.isChildAccountLinkedByQrScan) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AsyncImage(
                                        model = linkedChild.avatar,
                                        contentDescription = linkedChild.name,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = "Enfant lié : ${linkedChild.name} (${linkedChild.age} ans)",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        )
                                        Text(
                                            text = "📱 ${linkedChild.deviceModel} • 🔋 ${linkedChild.batteryLevel}%",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = onOpenParentalSettings,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Gérer", color = Color(0xFFE0E7FF), fontSize = 11.5.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Aucun compte enfant lié par QR code",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Button(
                                    onClick = onOpenQrScanner,
                                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Lier QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Subscription Plans
            item {
                Text(
                    text = "Choisir ou Renouveler votre Formule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(plans.size) { index ->
                val plan = plans[index]
                val isSelected = selectedPlanId == plan.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPlanId = plan.id }
                        .testTag("plan_card_${plan.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MbotePurplePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPlanId = plan.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = MbotePurplePrimary)
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = plan.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (plan.discount != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = plan.discount,
                                                    color = Color(0xFF166534),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${plan.maxChildren} enfant(s) protégé(s)",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${plan.priceFcfa} FCFA",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MbotePurplePrimary
                                )
                                Text(
                                    text = plan.period,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = plan.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        plan.features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(text = feat, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }

            // Payment Methods Selector (Mobile Money)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Moyen de Paiement Sécurisé",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Orange Money", "MTN MoMo", "Airtel Money", "Carte Visa").forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPaymentMethod = method },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MbotePurplePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = method,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Benefits Grid Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Avantages Exclusifs du Bouclier Parental",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                BenefitFeatureCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Liaison Chiffrée par QR Code",
                    description = "Connexion instantanée parent-enfant sans partager les mots de passe personnels. Signature cryptographique RSA-2048."
                )
            }

            item {
                BenefitFeatureCard(
                    icon = Icons.Default.Timer,
                    title = "Coupe-Circuit Quota 2h Quotidien",
                    description = "Déconnexion forcée de l'application dès que l'enfant atteint son temps d'écran maximal journalier fixé par le parent."
                )
            }

            item {
                BenefitFeatureCard(
                    icon = Icons.Default.LockClock,
                    title = "Verrouillage Nocturne (00h-06h)",
                    description = "Coupe automatiquement les notifications et bloque l'accès aux vidéos et discussions pendant les heures de sommeil."
                )
            }

            item {
                BenefitFeatureCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "Alertes SOS Brevo & Push 24h/24",
                    description = "Réception en direct d'e-mails transactionnels Brevo et de notifications prioritaires dès qu'un contenu suspect est détecté."
                )
            }

            item {
                BenefitFeatureCard(
                    icon = Icons.Outlined.Analytics,
                    title = "Audit & Journal des Actions à Risque",
                    description = "Historique détaillé des tentatives de commentaires nocturnes ou d'accès restreints pour un dialogue bienveillant."
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showPaymentSuccessSheet) {
        AlertDialog(
            onDismissRequest = { showPaymentSuccessSheet = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Abonnement Activé avec Succès ! 🎉", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Text(
                    "Votre formule Bouclier Parental Pro a été validée via $selectedPaymentMethod. Vos enfants bénéficient de la protection maximale MBoté.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentSuccessSheet = false
                        Toast.makeText(context, "👑 Bouclier Parental MBoté Activé !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("OK, Merci !")
                }
            }
        )
    }
}

@Composable
private fun BenefitFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MbotePurplePrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MbotePurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
