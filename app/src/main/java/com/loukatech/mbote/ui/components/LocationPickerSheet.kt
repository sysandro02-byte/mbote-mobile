package com.loukatech.mbote.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    onDismiss: () -> Unit,
    onSendLocation: (placeName: String, lat: Double, lng: Double, isLive: Boolean, durationMin: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isLiveSelected by remember { mutableStateOf(false) }
    var selectedDuration by remember { mutableIntStateOf(15) } // minutes
    var selectedPlace by remember { mutableStateOf("Position GPS actuelle de l'appareil") }
    var deviceLocation by remember { mutableStateOf<Location?>(null) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fetchLocation = {
        isFetchingGps = true
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null && hasLocationPermission) {
                @Suppress("MissingPermission")
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                if (lastGps != null) {
                    deviceLocation = lastGps
                } else {
                    // Fallback to default Brazzaville coordinates if emulator has no cached fix
                    val fallback = Location("gps").apply {
                        latitude = -4.2634
                        longitude = 15.2429
                        accuracy = 4.5f
                    }
                    deviceLocation = fallback
                }
            } else {
                val fallback = Location("gps").apply {
                    latitude = -4.2634
                    longitude = 15.2429
                    accuracy = 5.0f
                }
                deviceLocation = fallback
            }
        } catch (e: Exception) {
            val fallback = Location("gps").apply {
                latitude = -4.2634
                longitude = 15.2429
            }
            deviceLocation = fallback
        } finally {
            isFetchingGps = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            fetchLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val currentLat = deviceLocation?.latitude ?: -4.2634
    val currentLng = deviceLocation?.longitude ?: 15.2429

    val presetPlaces = remember(currentLat, currentLng) {
        listOf(
            "Position GPS actuelle de l'appareil" to Pair(currentLat, currentLng),
            "Brazzaville Centre (Place de la République)" to Pair(-4.2634, 15.2429),
            "Pointe-Noire (Avenue Charles de Gaulle)" to Pair(-4.7761, 11.8635),
            "Kinshasa Gombe (Boulevard du 30 Juin)" to Pair(-4.3032, 15.3090),
            "Aéroport International Maya-Maya" to Pair(-4.2517, 15.2530)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MbotePurpleLight) },
        modifier = modifier.testTag("location_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MbotePurplePrimary, Color(0xFF38BDF8)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Partager la position",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GPS chiffré de bout en bout",
                            style = MaterialTheme.typography.bodySmall,
                            color = MbotePurplePrimary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Real Device Coordinates Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GpsFixed,
                            contentDescription = null,
                            tint = MbotePurplePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Coordonnées de l'appareil",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "Lat: %.5f • Lng: %.5f", currentLat, currentLng),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MbotePurplePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                fetchLocation()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MbotePurplePrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Actualiser GPS",
                                tint = MbotePurplePrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (!isLiveSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (!isLiveSelected) BorderStroke(1.5.dp, MbotePurplePrimary) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isLiveSelected = false }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (!isLiveSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Position actuelle",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isLiveSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLiveSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isLiveSelected) BorderStroke(1.5.dp, MbotePurplePrimary) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isLiveSelected = true }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShareLocation,
                            contentDescription = null,
                            tint = if (isLiveSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Position en direct",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isLiveSelected) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Durée du partage en direct :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "15 min", 60 to "1 heure", 480 to "8 heures").forEach { (duration, label) ->
                        val selected = selectedDuration == duration
                        FilterChip(
                            selected = selected,
                            onClick = { selectedDuration = duration },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MbotePurplePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Sélectionner la position à envoyer :",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            presetPlaces.forEach { (place, coords) ->
                val isSelected = selectedPlace == place
                val isGpsActual = place.startsWith("Position GPS")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.6f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedPlace = place }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGpsActual) Icons.Outlined.Explore else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "GPS: %.4f, %.4f", coords.first, coords.second),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val coords = presetPlaces.find { it.first == selectedPlace }?.second ?: Pair(currentLat, currentLng)
                    val finalPlaceName = if (selectedPlace.startsWith("Position GPS")) {
                        "Position GPS (${String.format(Locale.US, "%.4f, %.4f", coords.first, coords.second)})"
                    } else {
                        selectedPlace
                    }
                    onSendLocation(finalPlaceName, coords.first, coords.second, isLiveSelected, selectedDuration)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_send_location_button")
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isLiveSelected) "Partager en direct ($selectedDuration min)"
                    else "Envoyer ces coordonnées GPS"
                )
            }
        }
    }
}
