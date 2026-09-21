package com.loukatech.mbote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Legacy entry point retained for binary/source compatibility.
 * The former hard-coded public-figure demo profile was removed from production.
 */
@Composable
fun LindaPublicProfileDialog(
    onDismiss: () -> Unit,
    onStartChat: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.statusBarsPadding().padding(12.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Outlined.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.padding(24.dp).size(48.dp)
                        )
                    }
                    Text("Profil public indisponible", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Ce profil de démonstration a été retiré. Ouvrez un profil réel depuis Masta, Actus ou ShortMBoté.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
