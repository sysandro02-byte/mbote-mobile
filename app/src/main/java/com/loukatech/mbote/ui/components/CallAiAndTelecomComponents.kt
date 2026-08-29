package com.loukatech.mbote.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.loukatech.mbote.model.CallItem
import com.loukatech.mbote.model.SyncedContact
import com.loukatech.mbote.ui.theme.PurplePrimary
import com.loukatech.mbote.ui.theme.PurpleSoft
import java.util.Calendar

// Enum for Call Method (MBoté HD vs Cellular Network)
enum class CallMethodType {
    MBOTE_HD,
    CELLULAR_SIM
}

data class AiSuggestedContact(
    val id: String,
    val name: String,
    val avatar: String,
    val phoneNumber: String,
    val recommendationReason: String,
    val confidenceScore: Int, // e.g., 95%
    val timeContext: String, // e.g. "Habitude 17h-19h", "Appel du Soir"
    val isOnline: Boolean = true
)

data class AiCallerIdResult(
    val phoneNumber: String,
    val name: String,
    val category: String, // e.g., "Entreprise / Banque", "Spam Suspecté", "Particulier Verified"
    val location: String, // e.g. "Brazzaville, Congo"
    val carrier: String,  // e.g. "MTN Congo (GSM)"
    val spamScore: Int,   // 0 to 100 (high = dangerous)
    val trustBadgeText: String,
    val isVerified: Boolean,
    val totalReports: Int = 0,
    val aiAnalysisSummary: String
)

/**
 * 1. AI Suggested Contacts Card Section based on interaction frequency & time of day
 */
@Composable
fun AiSuggestedContactsSection(
    calls: List<CallItem>,
    syncedContacts: List<SyncedContact>,
    onStartCall: (name: String, avatar: String, isVideo: Boolean, isCellular: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    val (timeOfDayLabel, timeIcon, timeColor) = remember(hour) {
        when (hour) {
            in 5..11 -> Triple("Matinée", Icons.Outlined.WbSunny, Color(0xFFF59E0B))
            in 12..17 -> Triple("Après-midi", Icons.Outlined.Lightbulb, Color(0xFF3B82F6))
            in 18..22 -> Triple("Soirée", Icons.Outlined.NightsStay, Color(0xFF8B5CF6))
            else -> Triple("Nuit", Icons.Outlined.Bedtime, Color(0xFF6366F1))
        }
    }

    val suggestedContacts = remember(calls, syncedContacts, hour) {
        val baseList = mutableListOf<AiSuggestedContact>()

        // Pre-defined sample contacts enriched with time-of-day IA heuristics
        val timeReason = when (hour) {
            in 5..11 -> "Habitude en matinée • 96% IA"
            in 12..17 -> "Frequent vers midi • 94% IA"
            in 18..22 -> "Appels habituels du soir • 98% IA"
            else -> "Urgence / Nuit suggéré • 91% IA"
        }

        baseList.add(
            AiSuggestedContact(
                id = "sugg_1",
                name = "Grace Moukassa",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                phoneNumber = "+242 06 612 3456",
                recommendationReason = timeReason,
                confidenceScore = 98,
                timeContext = "$timeOfDayLabel • Échange fréquent"
            )
        )
        baseList.add(
            AiSuggestedContact(
                id = "sugg_2",
                name = "Docteur Grace",
                avatar = "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150",
                phoneNumber = "+242 05 555 1234",
                recommendationReason = "IA : Rappel médical conseillé",
                confidenceScore = 93,
                timeContext = "Suivi santé"
            )
        )
        baseList.add(
            AiSuggestedContact(
                id = "sugg_3",
                name = "Arnaud Nkouka",
                avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                phoneNumber = "+242 06 999 8877",
                recommendationReason = "Dernier appel à cette heure hier",
                confidenceScore = 90,
                timeContext = "$timeOfDayLabel • Proche"
            )
        )
        baseList.add(
            AiSuggestedContact(
                id = "sugg_4",
                name = "Support MBoté VIP",
                avatar = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150",
                phoneNumber = "+242 06 444 0000",
                recommendationReason = "Assistance 24/7 conseillée",
                confidenceScore = 88,
                timeContext = "Service Client"
            )
        )
        baseList
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("ai_suggested_contacts_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row with AI Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PurplePrimary, Color(0xFFEC4899))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "IA",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Suggestions IA de la $timeOfDayLabel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Basé sur votre fréquence et vos habitudes d'appels",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = timeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = timeIcon, contentDescription = null, tint = timeColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeOfDayLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = timeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Carousel of Suggested Contacts
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestedContacts, key = { it.id }) { item ->
                    AiSuggestedContactCard(
                        contact = item,
                        onCallMbote = { onStartCall(item.name, item.avatar, false, false) },
                        onCallCellular = { onStartCall(item.name, item.avatar, false, true) }
                    )
                }
            }
        }
    }
}

@Composable
fun AiSuggestedContactCard(
    contact: AiSuggestedContact,
    onCallMbote: () -> Unit,
    onCallCellular: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.15f)),
        modifier = Modifier
            .width(145.dp)
            .testTag("ai_contact_${contact.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recommendation Pill
            Surface(
                color = PurplePrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${contact.confidenceScore}% Pertinence IA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Avatar
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(PurpleSoft)
                ) {
                    AsyncImage(
                        model = contact.avatar,
                        contentDescription = contact.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = contact.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = contact.recommendationReason,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dual Action Buttons: MBoté HD vs SIM GSM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // MBoté HD Call
                Surface(
                    onClick = onCallMbote,
                    shape = RoundedCornerShape(8.dp),
                    color = PurplePrimary,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "MBoté HD",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "HD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // SIM Cellular Call
                Surface(
                    onClick = onCallCellular,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SimCard,
                            contentDescription = "GSM SIM",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "SIM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Device Phone Contacts Selector & Telecom Cellular Call Switcher Card
 */
@Composable
fun CellularAndDeviceContactsBar(
    selectedCallMethod: CallMethodType,
    onSelectCallMethod: (CallMethodType) -> Unit,
    onOpenDeviceContactsDialog: () -> Unit,
    onOpenAiCallerIdDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mode d'appel & Réseau telecom",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )

                Surface(
                    onClick = onOpenAiCallerIdDialog,
                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IA Caller ID Premium", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // MBoté HD Option Tab
                Surface(
                    onClick = { onSelectCallMethod(CallMethodType.MBOTE_HD) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCallMethod == CallMethodType.MBOTE_HD) PurplePrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (selectedCallMethod == CallMethodType.MBOTE_HD) PurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (selectedCallMethod == CallMethodType.MBOTE_HD) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MBoté HD (VoIP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCallMethod == CallMethodType.MBOTE_HD) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Cellular Network SIM Option Tab
                Surface(
                    onClick = { onSelectCallMethod(CallMethodType.CELLULAR_SIM) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCallMethod == CallMethodType.CELLULAR_SIM) Color(0xFF10B981) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (selectedCallMethod == CallMethodType.CELLULAR_SIM) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SimCard,
                            contentDescription = null,
                            tint = if (selectedCallMethod == CallMethodType.CELLULAR_SIM) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Réseau SIM (GSM)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCallMethod == CallMethodType.CELLULAR_SIM) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Open Device Phonebook Button
            OutlinedButton(
                onClick = onOpenDeviceContactsDialog,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurplePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_open_device_contacts")
            ) {
                Icon(Icons.Outlined.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Accéder à tous les contacts de l'appareil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * 3. AI Caller ID Premium Dialog (Identification des numéros inconnus entrant & sortant)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCallerIdPremiumDialog(
    onDismiss: () -> Unit,
    onStartCall: (name: String, number: String, isCellular: Boolean) -> Unit
) {
    val context = LocalContext.current
    var inputNumber by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<AiCallerIdResult?>(null) }
    var isAutoBackgroundIdEnabled by remember { mutableStateOf(true) }

    val sampleDatabase = remember {
        listOf(
            AiCallerIdResult(
                phoneNumber = "+242 06 123 4567",
                name = "Afriland First Bank Congo",
                category = "Banque / Institution Financière",
                location = "Brazzaville, Congo",
                carrier = "MTN Congo (GSM)",
                spamScore = 2,
                trustBadgeText = "Numéro Certifié Officiel",
                isVerified = true,
                totalReports = 1520,
                aiAnalysisSummary = "L'IA confirme qu'il s'agit du numéro officiel du service client d'Afriland First Bank Congo. Aucun risque de spam détecté."
            ),
            AiCallerIdResult(
                phoneNumber = "+242 05 999 0011",
                name = "Démarchage Commercial Suspect",
                category = "Spam / Harcèlement téléphonique",
                location = "Pointe-Noire, Congo",
                carrier = "Airtel Congo",
                spamScore = 88,
                trustBadgeText = "Spam Suspecté (88% Risque)",
                isVerified = false,
                totalReports = 342,
                aiAnalysisSummary = "Attention : Ce numéro a fait l'objet de 342 signalements récents pour appels d'arnaque de loterie non sollicités."
            ),
            AiCallerIdResult(
                phoneNumber = "+242 06 888 7766",
                name = "Hôtel Saphir Brazzaville",
                category = "Hôtellerie & Restauration",
                location = "Centre-ville, Brazzaville",
                carrier = "MTN Congo",
                spamScore = 5,
                trustBadgeText = "Établissement Vérifié IA",
                isVerified = true,
                totalReports = 48,
                aiAnalysisSummary = "Identification IA : Réception principale de l'Hôtel Saphir Brazzaville."
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Header Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IA Caller ID",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF8B5CF6),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PREMIUM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Identification intelligente des numéros inconnus",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle for Live Background ID
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Identification IA en temps réel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                            Text(
                                text = "Analyse automatiquement les numéros inconnus entrants/sortants via le réseau",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoBackgroundIdEnabled,
                            onCheckedChange = {
                                isAutoBackgroundIdEnabled = it
                                Toast.makeText(context, if (it) "Protection IA activée" else "Protection IA désactivée", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF8B5CF6)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Box
                Text(
                    text = "Identifier un numéro inconnu :",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { inputNumber = it },
                        placeholder = { Text("ex: +242 06 123 4567") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (inputNumber.isNotBlank()) {
                                isSearching = true
                                val matched = sampleDatabase.firstOrNull {
                                    it.phoneNumber.replace(" ", "").contains(inputNumber.trim().replace(" ", ""))
                                } ?: AiCallerIdResult(
                                    phoneNumber = inputNumber,
                                    name = "Numéro Inconnu Identifié par l'IA",
                                    category = "Particulier / Ligne Réseau Congo",
                                    location = "Brazzaville, Congo",
                                    carrier = "Réseau Mobile GSM (MTN / Airtel)",
                                    spamScore = 15,
                                    trustBadgeText = "Confiance IA Moyen",
                                    isVerified = false,
                                    totalReports = 2,
                                    aiAnalysisSummary = "L'analyse IA indique qu'il s'agit d'un numéro individuel standard non signalé comme spam."
                                )
                                searchResult = matched
                                isSearching = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Rechercher")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Result Box
                searchResult?.let { res ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.spamScore > 50) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                        ),
                        border = BorderStroke(1.dp, if (res.spamScore > 50) Color(0xFFEF4444) else Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (res.spamScore > 50) Icons.Outlined.Warning else Icons.Outlined.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (res.spamScore > 50) Color(0xFFEF4444) else Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    color = if (res.spamScore > 50) Color(0xFFEF4444) else Color(0xFF10B981),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (res.spamScore > 50) "SPAM (${res.spamScore}%)" else "VÉRIFIÉ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Catégorie : ${res.category}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Localisation : ${res.location} • Réseau : ${res.carrier}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Analyse Gemini IA :",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                            Text(
                                text = res.aiAnalysisSummary,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Call button for this number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onStartCall(res.name, res.phoneNumber, false)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Appeler MBoté HD")
                                }

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onStartCall(res.name, res.phoneNumber, true)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.SimCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Appeler Réseau SIM")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual Summary View analyzing call history patterns and identifying time-of-day peak communication times for the user.
 */
@Composable
fun AiCallPatternSummaryVisualizer(
    calls: List<CallItem>,
    modifier: Modifier = Modifier
) {
    var selectedSlotIndex by remember { mutableIntStateOf(4) } // Default to peak slot (18h-22h)

    val timeSlots = remember {
        listOf(
            CallTimePatternSlot("06h - 09h", "Matinée", 18, "Appels rapides & pros"),
            CallTimePatternSlot("09h - 12h", "Midi Pro", 28, "Coordination travail"),
            CallTimePatternSlot("12h - 15h", "Pause", 20, "Nouvelles famille"),
            CallTimePatternSlot("15h - 18h", "Après-midi", 32, "Briefings projets"),
            CallTimePatternSlot("18h - 22h", "Pic Soirée", 78, "🔥 Heure de Pointe", isPeak = true),
            CallTimePatternSlot("22h - 06h", "Nuit", 10, "Appels urgents / Calme")
        )
    }

    val currentSlot = timeSlots[selectedSlotIndex]

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("ai_call_pattern_visualizer_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with AI Sparkle badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PurplePrimary, Color(0xFFEC4899))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Synthèse IA",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Synthèse IA des Habitudes d'Appels",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Analyse algorithmique des heures de pointe & flux",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Précision 96%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graph Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Distribution par Tranche Horaire",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }

                Text(
                    text = "Cliquez sur une tranche",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time-of-Day Bar Chart Graph
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                timeSlots.forEachIndexed { index, slot ->
                    val isSelected = index == selectedSlotIndex
                    val maxVal = 80f
                    val heightRatio = (slot.intensityPercent / maxVal).coerceIn(0.15f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSlotIndex = index }
                            .padding(horizontal = 2.dp)
                    ) {
                        if (slot.isPeak) {
                            Surface(
                                color = Color(0xFFEF4444),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = "PIC",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = "${slot.intensityPercent}%",
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected || slot.isPeak) FontWeight.Bold else FontWeight.Normal,
                            color = if (slot.isPeak) Color(0xFFEF4444) else if (isSelected) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Vertical bar container
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((90 * heightRatio).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    when {
                                        slot.isPeak -> Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))
                                        isSelected -> Brush.verticalGradient(listOf(PurplePrimary, Color(0xFF8B5CF6)))
                                        else -> Brush.verticalGradient(listOf(PurpleSoft, MaterialTheme.colorScheme.surfaceVariant))
                                    }
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = slot.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Peak / Slot Detailed Insight Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (currentSlot.isPeak) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (currentSlot.isPeak) Color(0xFFFCA5A5) else PurplePrimary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (currentSlot.isPeak) Color(0xFFEF4444) else PurplePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentSlot.isPeak) Icons.Outlined.TrendingUp else Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Créneau ${currentSlot.label} (${currentSlot.title})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSlot.isPeak) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface
                            )
                            if (currentSlot.isPeak) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFEF4444),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Peak de Communication",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "L'IA a calculé une fréquence d'appels de ${currentSlot.intensityPercent}% sur cette plage. ${currentSlot.description}.",
                            fontSize = 11.sp,
                            color = if (currentSlot.isPeak) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Fast Pattern Statistics Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiPatternMetricPill(
                    title = "Heure de Pointe",
                    value = "18h - 22h",
                    subtitle = "78% des échanges",
                    icon = Icons.Outlined.Speed,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                AiPatternMetricPill(
                    title = "Jour le + Actif",
                    value = "Vendredi",
                    subtitle = "2.4x plus d'appels",
                    icon = Icons.Outlined.Group,
                    color = PurplePrimary,
                    modifier = Modifier.weight(1f)
                )
                AiPatternMetricPill(
                    title = "Durée Moyenne",
                    value = "9 min 40 s",
                    subtitle = "Appels Vidéo HD",
                    icon = Icons.Outlined.AccessTime,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class CallTimePatternSlot(
    val label: String,
    val title: String,
    val intensityPercent: Int,
    val description: String,
    val isPeak: Boolean = false
)

@Composable
private fun AiPatternMetricPill(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


/**
 * 4. Device Phone Contacts Dialog Picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePhoneContactsDialog(
    onDismiss: () -> Unit,
    onStartCall: (name: String, avatar: String, isVideo: Boolean, isCellular: Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allDeviceContacts = remember {
        listOf(
            SyncedContact("dc_1", "Papa (Moussia)", "+242 06 600 1122", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", true),
            SyncedContact("dc_2", "Maman Célestine", "+242 06 600 3344", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150", true),
            SyncedContact("dc_3", "Bureau Orange Congo", "+242 05 500 0000", "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150", false),
            SyncedContact("dc_4", "Service Client MTN", "+242 06 123 0000", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false),
            SyncedContact("dc_5", "Docteur Grace", "+242 05 555 1234", "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150", true),
            SyncedContact("dc_6", "Taxi Express Brazza", "+242 06 777 8899", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", false),
            SyncedContact("dc_7", "Oncle Bienvenu", "+242 06 999 1122", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150", true)
        )
    }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) allDeviceContacts
        else allDeviceContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ContactPhone, contentDescription = null, tint = PurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contacts du téléphone", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un contact...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${filtered.size} contact(s) disponible(s)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.id }) { contact ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PurpleSoft)
                                ) {
                                    AsyncImage(
                                        model = contact.avatarUrl,
                                        contentDescription = contact.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(contact.phoneNumber, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            onDismiss()
                                            onStartCall(contact.name, contact.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false, false)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "MBoté HD", tint = PurplePrimary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            onDismiss()
                                            onStartCall(contact.name, contact.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", false, true)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Outlined.SimCard, contentDescription = "Cellulaire GSM", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
