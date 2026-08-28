package com.loukatech.mbote.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@Composable
fun EyeContactTimerDialog(
    secondsRemaining: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, secs)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFFEC4899).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Regard Soutenu (4 Minutes)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MbotePurplePrimary
                )
                Text(
                    text = "Étape finale des questions d'Aron",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = MbotePurplePrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (secondsRemaining > 0)
                        "Maintenez le contact visuel en silence..."
                    else
                        "✨ Exercice terminé avec succès !",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (secondsRemaining > 0) MaterialTheme.colorScheme.onSurface else Color(0xFF10B981)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { (240 - secondsRemaining) / 240f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MbotePurplePrimary,
                    trackColor = MbotePurpleSoft
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MbotePurpleSoft.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Conseil d'Arthur Aron :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MbotePurplePrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Regardez-vous silencieusement dans les yeux sans parler. Respirez calmement et laissez s'installer une présence mutuelle sincère.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                modifier = Modifier.fillMaxWidth().testTag("close_eye_contact_dialog")
            ) {
                Text(if (secondsRemaining == 0) "Terminer l'exercice ✨" else "Arrêter le chronomètre")
            }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.testTag("eye_contact_timer_dialog")
    )
}
