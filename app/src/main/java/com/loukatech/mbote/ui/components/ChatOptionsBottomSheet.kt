package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatOptionsBottomSheet(
    onDismiss: () -> Unit,
    onSearchClick: () -> Unit,
    onWallpaperClick: () -> Unit,
    onEphemeralClick: () -> Unit,
    onInviteClick: () -> Unit,
    onAronQuestionsClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onPollClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.testTag("chat_options_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Options de conversation MBoté",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                item {
                    QuickActionItem(
                        icon = Icons.Default.Search,
                        title = "Rechercher",
                        subtitle = "Rechercher des mots-clés dans la discussion",
                        onClick = {
                            onDismiss()
                            onSearchClick()
                        },
                        testTag = "chat_opt_search"
                    )
                }
                item {
                    QuickActionItem(
                        icon = Icons.Default.Palette,
                        title = "Personnalisation & Fond",
                        subtitle = "Changer le fond d'écran de discussion",
                        onClick = {
                            onDismiss()
                            onWallpaperClick()
                        },
                        testTag = "chat_opt_wallpaper"
                    )
                }
                item {
                    QuickActionItem(
                        icon = Icons.Default.Timer,
                        title = "Messages éphémères",
                        subtitle = "Disparition automatique des nouveaux messages",
                        onClick = {
                            onDismiss()
                            onEphemeralClick()
                        },
                        testTag = "chat_opt_ephemeral"
                    )
                }
                item {
                    QuickActionItem(
                        icon = Icons.Default.Link,
                        title = "Lien d'invitation",
                        subtitle = "Générer un lien pour convier des amis",
                        onClick = {
                            onDismiss()
                            onInviteClick()
                        },
                        testTag = "chat_opt_invite"
                    )
                }
                item {
                    QuickActionItem(
                        icon = Icons.Default.HelpOutline,
                        title = "36 Questions d'Aron",
                        subtitle = "Série de questions interactives pour briser la glace",
                        onClick = {
                            onDismiss()
                            onAronQuestionsClick()
                        },
                        testTag = "chat_opt_aron"
                    )
                }
                item {
                    QuickActionItem(
                        icon = Icons.Default.AttachMoney,
                        title = "MBoté Pay (Transférer)",
                        subtitle = "Transférer des FCFA instantanément",
                        onClick = {
                            onDismiss()
                            onPaymentClick()
                        },
                        testTag = "chat_opt_payment"
                    )
                }
                item {
                    QuickActionItem(
                        icon = Icons.Default.BarChart,
                        title = "Nouveau sondage",
                        subtitle = "Poser une question avec des choix multiples",
                        onClick = {
                            onDismiss()
                            onPollClick()
                        },
                        testTag = "chat_opt_poll"
                    )
                }
                item {
                    QuickActionItem(
                        icon = Icons.Default.Download,
                        title = "Exporter la discussion",
                        subtitle = "Générer un fichier d'historique JSON",
                        onClick = {
                            onDismiss()
                            onExportClick()
                        },
                        testTag = "chat_opt_export"
                    )
                }
            }
        }
    }
}
