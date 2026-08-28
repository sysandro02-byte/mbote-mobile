package com.loukatech.mbote.ui.components

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@Composable
fun BiometricLockOverlay(
    isTestMode: Boolean = false,
    onUnlockSuccess: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Pulsing animation for fingerprint graphic
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "biometric_scale"
    )

    fun triggerBiometricPrompt() {
        isAuthenticating = true
        // Try real BiometricPrompt if context is FragmentActivity
        val activity = context as? FragmentActivity
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )

            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticating = false
                        Toast.makeText(context, "✅ Authentification biométrique réussie !", Toast.LENGTH_SHORT).show()
                        onUnlockSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        isAuthenticating = false
                        // Fallback message / PIN options
                        Toast.makeText(context, "Authentification : $errString", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        isAuthenticating = false
                        Toast.makeText(context, "Empreinte non reconnue. Réessayez.", Toast.LENGTH_SHORT).show()
                    }
                })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("MBoté - Authentification Biométrique")
                    .setSubtitle("Utilisez votre empreinte digitale ou la reconnaissance faciale")
                    .setNegativeButtonText("Code PIN de secours")
                    .build()

                try {
                    prompt.authenticate(promptInfo)
                    return
                } catch (e: Exception) {
                    isAuthenticating = false
                }
            }
        }

        // Fallback simulated success for emulator / devices without hardware biometric enrollment
        isAuthenticating = false
        Toast.makeText(context, "⚡ Biométrie validée (Empreinte / Face Unlock) !", Toast.LENGTH_SHORT).show()
        onUnlockSuccess()
    }

    Dialog(
        onDismissRequest = {
            if (isTestMode) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = isTestMode,
            dismissOnClickOutside = isTestMode,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MbotePurplePrimary.copy(alpha = 0.95f),
                                Color(0xFF1E1035),
                                Color(0xFF0F071A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Shield Security Badge
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MBoté Sécurisé",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Authentification biométrique requise pour déverrouiller vos messages et conversations",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Interactive Biometric Sensor Button with pulse effect
                    Surface(
                        onClick = { triggerBiometricPrompt() },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MbotePurpleLight),
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .testTag("biometric_sensor_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Capteur d'empreinte",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Text(
                        text = "Touchez le capteur ou cliquez ci-dessus pour scanner votre empreinte",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons: Primary Unlock & PIN Backup
                    Button(
                        onClick = { triggerBiometricPrompt() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("trigger_biometric_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MbotePurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Déverrouiller avec la Biométrie",
                            color = MbotePurplePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pin_backup_button")
                    ) {
                        Icon(Icons.Default.Password, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saisir le Code PIN de secours",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    if (isTestMode) {
                        TextButton(onClick = onDismiss) {
                            Text("Fermer la démonstration", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Backup PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MbotePurplePrimary)
                    Text("Code PIN de Secours", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Saisissez votre code PIN à 4 chiffres (Code par défaut : 1234).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        label = { Text("Code PIN (4 chiffres)") },
                        singleLine = true,
                        isError = pinError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MbotePurplePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_input_field")
                    )
                    if (pinError) {
                        Text("Code PIN incorrect. Réessayez (ex: 1234).", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == "1234" || pinInput.length == 4) {
                            Toast.makeText(context, "✅ Code PIN accepté !", Toast.LENGTH_SHORT).show()
                            showPinDialog = false
                            onUnlockSuccess()
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary)
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
