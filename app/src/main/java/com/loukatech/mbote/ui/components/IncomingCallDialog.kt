package com.loukatech.mbote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.loukatech.mbote.service.CallSocketEvent
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@Composable
fun IncomingCallDialog(
    invite: CallSocketEvent,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        shape = RoundedCornerShape(24.dp),
        icon = {
            if (invite.callerAvatar.isNotBlank()) {
                AsyncImage(
                    model = invite.callerAvatar,
                    contentDescription = invite.callerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = if (invite.isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = null,
                    tint = MbotePurplePrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(invite.callerName.ifBlank { "Utilisateur MBoté" }, fontWeight = FontWeight.Bold)
                Text(
                    if (invite.isVideo) "Appel vidéo MBoté entrant" else "Appel audio MBoté entrant",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        text = {
            Text(
                "Accepter pour démarrer la connexion WebRTC sécurisée.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)) {
                Icon(if (invite.isVideo) Icons.Default.Videocam else Icons.Default.Phone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Accepter")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject) {
                Icon(Icons.Default.CallEnd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refuser")
            }
        }
    )
}
