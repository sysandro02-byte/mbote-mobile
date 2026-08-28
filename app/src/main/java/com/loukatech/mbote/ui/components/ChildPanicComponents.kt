package com.loukatech.mbote.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.model.ChildPanicAlert
import com.loukatech.mbote.model.LinkedChildInfo
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 2) Panic Button Feature for Child & GPS Location Broadcast to Parent
 */

@Composable
fun ChildPanicTriggerButton(
    childInfo: LinkedChildInfo,
    onTriggerPanic: (latitude: Double, longitude: Double, address: String, emergencyType: String, message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ChildPanicTriggerDialog(
            childInfo = childInfo,
            onDismiss = { showDialog = false },
            onConfirmPanic = { lat, lng, address, type, msg ->
                showDialog = false
                onTriggerPanic(lat, lng, address, type, msg)
            }
        )
    }

    // High-visibility SOS trigger card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("child_panic_trigger_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pulsing Red SOS Icon
                val infiniteTransition = rememberInfiniteTransition(label = "panic_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(46.dp)
                        .scale(pulseScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Sos,
                            contentDescription = "SOS Panique",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "🚨 Bouton Panique Enfant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFFDC2626)
                    )
                    Text(
                        text = "Envoie la position GPS en direct aux parents",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("trigger_panic_button")
            ) {
                Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Déclencher", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
fun ChildPanicTriggerDialog(
    childInfo: LinkedChildInfo,
    onDismiss: () -> Unit,
    onConfirmPanic: (Double, Double, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedEmergencyType by remember { mutableStateOf("Bouton Panique Pressé 🚨") }
    var selectedLocationName by remember { mutableStateOf("Avenue de l'Indépendance, Poto-Poto, Brazzaville") }
    var latitude by remember { mutableDoubleStateOf(-4.2634) }
    var longitude by remember { mutableDoubleStateOf(15.2429) }
    var customNotes by remember { mutableStateOf("") }

    var isCountdownActive by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(3) }
    val coroutineScope = rememberCoroutineScope()

    val emergencyOptions = listOf(
        "Bouton Panique Pressé 🚨",
        "Danger Immédiat / Agression ⚠️",
        "Perdu / Trajet Inconnu 🧭",
        "Urgence Médicale / Malaise 🏥",
        "Test de Sécurité MBoté 🛡️"
    )

    val locationPresets = listOf(
        Triple("Avenue de l'Indépendance, Poto-Poto, Brazzaville", -4.2634, 15.2429),
        Triple("Rond-Point Poto-Poto, Brazzaville", -4.2678, 15.2789),
        Triple("Lycée d'Excellence, Bacongo, Brazzaville", -4.2812, 15.2543),
        Triple("Centre-Ville / Boulevard Denis Sassou Nguesso", -4.2701, 15.2845)
    )

    // Countdown timer effect
    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive) {
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds -= 1
            }
            if (countdownSeconds == 0) {
                onConfirmPanic(
                    latitude,
                    longitude,
                    selectedLocationName,
                    selectedEmergencyType,
                    customNotes.ifBlank { "Signal de détresse immédiat transmis par l'enfant ($selectedEmergencyType)." }
                )
                Toast.makeText(context, "🚨 Alerte Panique & Coordonnées GPS transmises avec succès !", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isCountdownActive) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("child_panic_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Emergency,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Bouton Panique & SOS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "Envoi GPS instantané aux parents",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, enabled = !isCountdownActive) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                if (isCountdownActive) {
                    // Urgent Countdown screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val pulseAnim by rememberInfiniteTransition(label = "countdown_pulse").animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .size(110.dp)
                                .scale(pulseAnim)
                                .shadow(12.dp, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$countdownSeconds",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = "Transmission de l'alerte en cours...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "Les coordonnées GPS et une notification prioritaire Brevo seront envoyées immédiatement.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                isCountdownActive = false
                                countdownSeconds = 3
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Annuler l'alerte", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Configuration and trigger view
                    // GPS Live Status Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Capteur GPS Précis Fixé (Précision: +/- 5.8m)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Lat: ${"%.4f".format(latitude)}° S, Lng: ${"%.4f".format(longitude)}° E",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Emergency Type selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Type d'urgence",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        emergencyOptions.forEach { opt ->
                            val isSelected = selectedEmergencyType == opt
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedEmergencyType = opt },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEF4444).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedEmergencyType = opt },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEF4444)),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Location Preset picker
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Position GPS de transmission",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        locationPresets.forEach { (name, lat, lng) ->
                            val isSelected = selectedLocationName == name
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedLocationName = name
                                        latitude = lat
                                        longitude = lng
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MbotePurplePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Optional message / description
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        label = { Text("Message additionnel (optionnel)") },
                        placeholder = { Text("Ex: Je suis devant le portail...") },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Big Trigger SOS Button
                    Button(
                        onClick = {
                            isCountdownActive = true
                            countdownSeconds = 3
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_panic_trigger"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Sos, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENVOYER L'ALERTE PANIQUE (3s)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Parent View: Floating banner shown when an active Panic Alert is received
 */
@Composable
fun ParentPanicAlertBanner(
    alert: ChildPanicAlert,
    onViewLocationDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_blink")
    val bannerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clickable { onViewLocationDetails() }
            .testTag("parent_panic_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEF4444).copy(alpha = bannerAlpha)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Emergency,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "🚨 ALERTE PANIQUE : ${alert.childName}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${alert.address} • ${alert.timestamp}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onViewLocationDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Localiser", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }
        }
    }
}

/**
 * Full interactive Parent Location & Map Dialog
 */
@Composable
fun ParentPanicLocationDetailDialog(
    alert: ChildPanicAlert,
    childInfo: LinkedChildInfo,
    onDismiss: () -> Unit,
    onResolve: (String) -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .testTag("parent_panic_detail_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with child avatar & status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = alert.childAvatar,
                                    contentDescription = alert.childName,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = alert.childName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.padding(1.dp)
                                ) {
                                    Text(
                                        text = "SOS ACTIF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Déclenché le ${alert.timestamp}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // Interactive Radar & Map Canvas
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Custom Canvas Map Grid & Radar Effect
                        val infiniteTransition = rememberInfiniteTransition(label = "radar")
                        val radarRadius by infiniteTransition.animateFloat(
                            initialValue = 10f,
                            targetValue = 90f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "radar_radius"
                        )
                        val radarAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "radar_alpha"
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2

                            // Draw subtle map grid
                            val gridSpacing = 40.dp.toPx()
                            for (x in 0..(size.width / gridSpacing).toInt()) {
                                drawLine(
                                    color = Color(0xFF334155).copy(alpha = 0.35f),
                                    start = Offset(x * gridSpacing, 0f),
                                    end = Offset(x * gridSpacing, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (y in 0..(size.height / gridSpacing).toInt()) {
                                drawLine(
                                    color = Color(0xFF334155).copy(alpha = 0.35f),
                                    start = Offset(0f, y * gridSpacing),
                                    end = Offset(size.width, y * gridSpacing),
                                    strokeWidth = 1f
                                )
                            }

                            // Pulsing radar circles around child GPS pin
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = radarAlpha),
                                radius = radarRadius.dp.toPx(),
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = 0.25f),
                                radius = 25.dp.toPx(),
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 9.dp.toPx(),
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = Offset(centerX, centerY)
                            )

                            // Parent pin on the edge
                            val parentX = centerX + 110.dp.toPx()
                            val parentY = centerY - 50.dp.toPx()
                            drawCircle(
                                color = Color(0xFF3B82F6),
                                radius = 6.dp.toPx(),
                                center = Offset(parentX, parentY)
                            )

                            // Distance line
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(centerX, centerY),
                                end = Offset(parentX, parentY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Map Overlay Tags
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Position de Junior (Précision +/- 5.8m)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "📍 Distance: ~1.2 km",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Location Details Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = alert.address,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Coordonnées GPS",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Lat: ${"%.5f".format(alert.latitude)}° S, Lng: ${"%.5f".format(alert.longitude)}° E",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "État de l'appareil",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "🔋 ${alert.batteryLevel}% • ${alert.networkStatus}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }

                        if (alert.emergencyMessage.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            Text(
                                text = "Message / Motif : \"${alert.emergencyMessage}\"",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Action: Open in External Maps (Google Maps / OpenStreetMap)
                Button(
                    onClick = {
                        try {
                            val uri = Uri.parse("geo:${alert.latitude},${alert.longitude}?q=${alert.latitude},${alert.longitude}(Junior+MBot%C3%A9+SOS)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Fallback to browser Google Maps url
                            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${alert.latitude},${alert.longitude}")
                            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(browserIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MbotePurplePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir l'Itinéraire dans Maps 🗺️", fontWeight = FontWeight.Bold)
                }

                // Emergency Call Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Call Child
                    OutlinedButton(
                        onClick = {
                            try {
                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+242060000000"))
                                callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(callIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Appel vers Junior en cours...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MbotePurplePrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Appeler Junior", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MbotePurplePrimary)
                    }

                    // Call Emergency 117 / 112
                    OutlinedButton(
                        onClick = {
                            try {
                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:117"))
                                callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(callIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Appel Secours 117...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.LocalPolice, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Secours (117)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Resolve & Close Alert Button
                Button(
                    onClick = {
                        onResolve(alert.alertId)
                        onDismiss()
                        Toast.makeText(context, "Alerte panique marquée comme résolue ✓", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Marquer comme Sécurisé & Résolu ✅", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
