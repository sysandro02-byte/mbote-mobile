package com.loukatech.mbote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Videocam
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

/**
 * Quick Action Menu matching the exact visual requirements:
 * Adaptable across both Dark and Light themes with elevated Material 3 design:
 * 1. Nouveau message
 * 2. Créer un groupe
 * 3. Créer une chaîne
 * 4. Masta
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MboteQuickActionsSheet(
    onDismiss: () -> Unit,
    onNewMessageClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreateChannelClick: () -> Unit,
    onMastaClick: () -> Unit,
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
        modifier = modifier.testTag("quick_actions_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Title
            Text(
                text = "Actions rapides MBoté",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
            )

            // Row 1: Nouveau message
            QuickActionItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "Nouveau message",
                subtitle = "Démarrer une conversation directe",
                onClick = {
                    onDismiss()
                    onNewMessageClick()
                },
                testTag = "action_new_message"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Créer un groupe
            QuickActionItem(
                icon = Icons.Outlined.Groups,
                title = "Créer un groupe",
                subtitle = "Échanger à plusieurs avec vos contacts",
                onClick = {
                    onDismiss()
                    onCreateGroupClick()
                },
                testTag = "action_create_group"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Créer une chaîne
            QuickActionItem(
                icon = Icons.Outlined.Videocam,
                title = "Créer une chaîne",
                subtitle = "Diffuser vos actualités & vidéos",
                onClick = {
                    onDismiss()
                    onCreateChannelClick()
                },
                testTag = "action_create_channel"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Masta
            QuickActionItem(
                icon = Icons.Outlined.PersonAdd,
                title = "Masta",
                subtitle = "Ajouter des amis ou ouvrir l'Assistant IA",
                onClick = {
                    onDismiss()
                    onMastaClick()
                },
                testTag = "action_masta"
            )
        }
    }
}

/**
 * Individual action item row matching the visual reference:
 * - Dynamic theme container and icon badge
 * - High-contrast text adapting to light and dark theme
 */
@Composable
fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Soft Purple Theme Circle with Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MbotePurplePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title & Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.1.sp
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

