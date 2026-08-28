package com.loukatech.mbote.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    initialEmail: String = "",
    onDismiss: () -> Unit,
    onRequestResetCode: suspend (String) -> Result<String>,
    onConfirmReset: suspend (email: String, code: String, newPass: String) -> Result<Boolean>,
    onResetSuccess: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Email, 2: Code & New Password, 3: Success
    var email by remember { mutableStateOf(initialEmail) }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isEmailValid by remember(email) {
        derivedStateOf {
            email.trim().contains("@") && email.trim().contains(".") && email.trim().length >= 5
        }
    }

    val newPassHasMinLength = remember(newPassword) { newPassword.length >= 8 }
    val newPassHasUpper = remember(newPassword) { newPassword.any { it.isUpperCase() } }
    val newPassHasDigit = remember(newPassword) { newPassword.any { it.isDigit() } }
    val newPassHasSpecial = remember(newPassword) { newPassword.any { !it.isLetterOrDigit() } }

    val passwordStrengthScore = remember(newPassword) {
        if (newPassword.isEmpty()) 0
        else {
            var score = 0
            if (newPassHasMinLength) score++
            if (newPassHasUpper) score++
            if (newPassHasDigit) score++
            if (newPassHasSpecial) score++
            score
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("forgot_password_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MbotePurplePrimary.copy(alpha = 0.2f), MbotePurpleSoft.copy(alpha = 0.4f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (step == 3) Icons.Default.CheckCircle else Icons.Outlined.LockReset,
                        contentDescription = null,
                        tint = if (step == 3) Color(0xFF10B981) else MbotePurplePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (step) {
                        1 -> "Mot de passe oublié ?"
                        2 -> "Vérification du code"
                        else -> "Mot de passe réinitialisé !"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (step) {
                        1 -> "Saisissez l'adresse email associée à votre compte MBoté pour recevoir votre code sécurisé."
                        2 -> "Un code à 6 chiffres a été envoyé à $email. Entrez-le avec votre nouveau mot de passe."
                        else -> "Votre nouveau mot de passe a été configuré avec succès. Vous pouvez maintenant vous connecter."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Error Message
                errorMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // STEP 1: Enter Email
                if (step == 1) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Adresse Email") },
                        placeholder = { Text("ex: contact@exemple.com") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = MbotePurplePrimary)
                        },
                        trailingIcon = {
                            if (email.isNotBlank()) {
                                if (isEmailValid) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Email Valide", tint = Color(0xFF10B981))
                                } else {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = "Email Invalide", tint = Color(0xFFEF4444))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = email.isNotBlank() && !isEmailValid,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_email_input")
                    )

                    if (email.isNotBlank() && !isEmailValid) {
                        Text(
                            text = "Format d'adresse email invalide (ex: nom@exemple.com)",
                            color = Color(0xFFEF4444),
                            fontSize = 11.5.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!isEmailValid) {
                                errorMessage = "Veuillez entrer une adresse email valide."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = onRequestResetCode(email.trim())
                                isLoading = false
                                if (result.isSuccess) {
                                    successMessage = result.getOrNull()
                                    step = 2
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Erreur d'envoi"
                                }
                            }
                        },
                        enabled = !isLoading && isEmailValid,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_reset_code_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Envoyer le code", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // STEP 2: Enter Code & New Password
                if (step == 2) {
                    OutlinedTextField(
                        value = resetCode,
                        onValueChange = {
                            if (it.length <= 6) resetCode = it
                            errorMessage = null
                        },
                        label = { Text("Code à 6 chiffres") },
                        placeholder = { Text("123456") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Pin, contentDescription = null, tint = MbotePurplePrimary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = null
                        },
                        label = { Text("Nouveau mot de passe") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MbotePurplePrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (newPassword.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Force du mot de passe :",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.5.sp
                                )
                                Text(
                                    text = when (passwordStrengthScore) {
                                        0, 1 -> "Faible"
                                        2 -> "Moyen"
                                        3 -> "Fort"
                                        4 -> "Très fort (Sécurisé) ✓"
                                        else -> ""
                                    },
                                    color = when (passwordStrengthScore) {
                                        0, 1 -> Color(0xFFEF4444)
                                        2 -> Color(0xFFF59E0B)
                                        3 -> Color(0xFF10B981)
                                        4 -> Color(0xFF8B5CF6)
                                        else -> Color.Transparent
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val activeColor = when (passwordStrengthScore) {
                                    0, 1 -> Color(0xFFEF4444)
                                    2 -> Color(0xFFF59E0B)
                                    3 -> Color(0xFF10B981)
                                    4 -> Color(0xFF8B5CF6)
                                    else -> Color.Transparent
                                }
                                for (i in 1..4) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (i <= passwordStrengthScore) activeColor else Color(0xFFE2E8F0)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Confirmer le mot de passe") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MbotePurplePrimary)
                        },
                        trailingIcon = {
                            if (confirmPassword.isNotEmpty()) {
                                if (confirmPassword == newPassword) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Mots de passe identiques", tint = Color(0xFF10B981))
                                } else {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = "Mots de passe différents", tint = Color(0xFFEF4444))
                                }
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (resetCode.length < 4) {
                                errorMessage = "Veuillez saisir le code reçu."
                                return@Button
                            }
                            if (newPassword.length < 6) {
                                errorMessage = "Le mot de passe doit contenir au moins 6 caractères."
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                errorMessage = "Les mots de passe ne correspondent pas."
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = onConfirmReset(email.trim(), resetCode.trim(), newPassword)
                                isLoading = false
                                if (result.isSuccess) {
                                    step = 3
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Code incorrect ou expiré."
                                }
                            }
                        },
                        enabled = !isLoading && resetCode.isNotBlank() && newPassword.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Enregistrer le mot de passe", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // STEP 3: Success Confirmation
                if (step == 3) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onResetSuccess()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Aller à la connexion", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (step == 3) "Fermer" else "Annuler et retourner",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
