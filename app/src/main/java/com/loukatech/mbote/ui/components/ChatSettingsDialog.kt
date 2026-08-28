package com.loukatech.mbote.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loukatech.mbote.model.Chat
import com.loukatech.mbote.service.ChatExportManager
import com.loukatech.mbote.service.ChatExportResult
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

data class WallpaperPreset(
    val id: String,
    val name: String,
    val colorHex: String? = null,
    val imageUrl: String? = null,
    val previewColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsDialog(
    chat: Chat,
    onDismiss: () -> Unit,
    onUpdateDisappearingTimer: (Int) -> Unit,
    onUpdateWallpaper: (colorHex: String?, imageUrl: String?) -> Unit,
    onTriggerExport: () -> Unit,
    isBlocked: Boolean = false,
    onToggleBlock: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Éphémère, 1: Fond d'écran, 2: Sécurité & Sauvegarde
    var showBlockConfirmDialog by remember { mutableStateOf(false) }

    var currentDisappearingTimer by remember { mutableIntStateOf(chat.disappearingTimerSec) }
    var selectedWallpaperColor by remember { mutableStateOf(chat.wallpaperColor) }
    var selectedWallpaperImage by remember { mutableStateOf(chat.wallpaperImageUrl) }

    val colorPresets = remember {
        listOf(
            WallpaperPreset("default", "Par défaut", null, null, Color(0xFFF8FAFC)),
            WallpaperPreset("purple_dark", "Violet Nuit", "#1E1B4B", null, Color(0xFF1E1B4B)),
            WallpaperPreset("emerald", "Émeraude Congo", "#064E3B", null, Color(0xFF064E3B)),
            WallpaperPreset("midnight", "Minuit Sombre", "#0F172A", null, Color(0xFF0F172A)),
            WallpaperPreset("amber", "Ambre Chaleureux", "#78350F", null, Color(0xFF78350F)),
            WallpaperPreset("slate", "Bleu Ardoise", "#1E293B", null, Color(0xFF1E293B)),
            WallpaperPreset("rose", "Velours Rose", "#831843", null, Color(0xFF831843)),
            WallpaperPreset("amoled", "Noir Absolu", "#000000", null, Color(0xFF000000)),
            WallpaperPreset("light_warm", "Sable Clair", "#FEF3C7", null, Color(0xFFFEF3C7)),
            WallpaperPreset("lavender", "Lavande Douce", "#EDE9FE", null, Color(0xFFEDE9FE))
        )
    }

    val imagePresets = remember {
        listOf(
            WallpaperPreset(
                id = "african_art",
                name = "Motifs Géométriques",
                imageUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=600&auto=format&fit=crop&q=80",
                previewColor = Color(0xFF6B21A8)
            ),
            WallpaperPreset(
                id = "congo_sunset",
                name = "Coucher de Soleil",
                imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80",
                previewColor = Color(0xFFD97706)
            ),
            WallpaperPreset(
                id = "nature_green",
                name = "Forêt Tropicale",
                imageUrl = "https://images.unsplash.com/photo-1511497584788-87676104235f?w=600&auto=format&fit=crop&q=80",
                previewColor = Color(0xFF059669)
            ),
            WallpaperPreset(
                id = "starry_sky",
                name = "Nébuleuse Étoilée",
                imageUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
                previewColor = Color(0xFF312E81)
            ),
            WallpaperPreset(
                id = "abstract_mesh",
                name = "Gradient Moderne",
                imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                previewColor = Color(0xFF4338CA)
            )
        )
    }

    val timerOptions = remember {
        listOf(
            0 to "Désactivé (Permanent)",
            5 to "5 secondes (Test rapide)",
            3600 to "1 heure",
            86400 to "24 heures (Recommandé)",
            604800 to "7 jours",
            2592000 to "30 jours"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(8.dp)
                .testTag("chat_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
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
                                .background(MbotePurpleSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Paramètres de discussion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = chat.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Selector
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MbotePurplePrimary,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Éphémère", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Fond d'écran", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sauvegarde", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        // TAB 0: Disappearing Messages
                        0 -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MbotePurplePrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockClock,
                                        contentDescription = null,
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Messages Éphémères",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Les messages s'effaceront automatiquement après le délai choisi pour tous les participants.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Délai d'autodestruction :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            timerOptions.forEach { (seconds, label) ->
                                val isSelected = currentDisappearingTimer == seconds
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MbotePurpleSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = if (isSelected) BorderStroke(1.5.dp, MbotePurplePrimary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            currentDisappearingTimer = seconds
                                            onUpdateDisappearingTimer(seconds)
                                        }
                                        .testTag("timer_option_$seconds")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (seconds == 0) Icons.Default.AllInclusive else Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MbotePurplePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                currentDisappearingTimer = seconds
                                                onUpdateDisappearingTimer(seconds)
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = MbotePurplePrimary)
                                        )
                                    }
                                }
                            }
                        }

                        // TAB 1: Chat Wallpaper & Background
                        1 -> {
                            Text(
                                text = "Couleurs d'ambiance :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Color selection grid/scroll
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                colorPresets.forEach { preset ->
                                    val isSelected = selectedWallpaperColor == preset.colorHex && selectedWallpaperImage == null
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedWallpaperColor = preset.colorHex
                                                selectedWallpaperImage = null
                                                onUpdateWallpaper(preset.colorHex, null)
                                            }
                                            .padding(4.dp)
                                            .testTag("wallpaper_color_${preset.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(preset.previewColor)
                                                .then(
                                                    if (isSelected) Modifier.background(
                                                        MbotePurplePrimary,
                                                        CircleShape
                                                    ) else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(preset.previewColor)
                                                    .then(
                                                        if (preset.colorHex == null) Modifier.background(
                                                            Color.LightGray.copy(alpha = 0.3f)
                                                        ) else Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (preset.colorHex == "#FEF3C7" || preset.colorHex == "#EDE9FE" || preset.colorHex == null) Color.Black else Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = preset.name,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Fonds d'écran artistiques :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Image Wallpapers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                imagePresets.forEach { preset ->
                                    val isSelected = selectedWallpaperImage == preset.imageUrl
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        border = if (isSelected) BorderStroke(2.dp, MbotePurplePrimary) else null,
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                selectedWallpaperImage = preset.imageUrl
                                                selectedWallpaperColor = null
                                                onUpdateWallpaper(null, preset.imageUrl)
                                            }
                                            .testTag("wallpaper_img_${preset.id}")
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = preset.imageUrl,
                                                contentDescription = preset.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                                        )
                                                    )
                                            )

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(6.dp)
                                                        .align(Alignment.TopEnd)
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(MbotePurplePrimary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = preset.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Reset Wallpaper Button
                            OutlinedButton(
                                onClick = {
                                    selectedWallpaperColor = null
                                    selectedWallpaperImage = null
                                    onUpdateWallpaper(null, null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rétablir le fond par défaut", fontSize = 12.sp)
                            }
                        }

                        // TAB 2: Export & Security Backup
                        2 -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sauvegarde locale JSON",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Exportez l'intégralité de l'historique de cette discussion (${chat.messages.size} messages) dans un fichier JSON structuré, incluant les métadonnées, pièces jointes, votes et transferts.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onTriggerExport()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chat_settings_export_btn")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Exporter la discussion (JSON)")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Contact Blocking & Privacy Section
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isBlocked) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isBlocked) BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = if (isBlocked) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBlocked) "Contact bloqué" else "Gestion du contact & Blocage",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isBlocked) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (isBlocked)
                                            "Ce contact est actuellement bloqué. Les messages entrants et appels de sa part sont stoppés et il n'apparaît plus dans vos contacts actifs."
                                        else
                                            "Bloquer ce contact arrêtera la réception de nouveaux messages et appels et masquera ce contact de votre liste active.",
                                        fontSize = 12.sp,
                                        color = if (isBlocked) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isBlocked) {
                                        Button(
                                            onClick = {
                                                onToggleBlock(false)
                                                Toast.makeText(context, "Contact ${chat.name} débloqué", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("unblock_contact_btn")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Débloquer ${chat.name}", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { showBlockConfirmDialog = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("block_contact_btn")
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFDC2626))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Bloquer ${chat.name}", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer et fermer")
                }
            }
        }
    }

    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            icon = {
                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
            },
            title = {
                Text(text = "Bloquer ${chat.name} ?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Les contacts bloqués ne peuvent plus vous envoyer de messages ni vous appeler. Cette conversation sera suspendue et le contact sera masqué de votre liste de contacts.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirmDialog = false
                        onToggleBlock(true)
                        Toast.makeText(context, "Contact ${chat.name} bloqué", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Bloquer", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
