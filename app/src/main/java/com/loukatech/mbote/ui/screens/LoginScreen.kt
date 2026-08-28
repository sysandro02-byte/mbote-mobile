package com.loukatech.mbote.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.service.api.AdminStatsData
import com.loukatech.mbote.ui.components.AdminLoginDialog
import com.loukatech.mbote.ui.components.ForgotPasswordDialog
import com.loukatech.mbote.ui.theme.MbotePurpleLight
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, REGISTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onLoginSubmit: suspend (email: String, password: String) -> Result<Unit>,
    onRegisterSubmit: suspend (name: String, email: String, password: String, phone: String) -> Result<Unit>,
    onGoogleLoginSubmit: suspend () -> Result<Unit>,
    onGitHubLoginSubmit: suspend () -> Result<Unit> = { Result.success(Unit) },
    onRequestResetCode: suspend (String) -> Result<String>,
    onConfirmResetPassword: suspend (email: String, code: String, newPass: String) -> Result<Boolean>,
    onAdminLoginSubmit: suspend (adminKey: String, email: String, pass: String) -> Result<AdminStatsData>,
    onSaveServerConfig: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("m.loutala@gmail.com") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("15/05/2010") }
    var parentEmailInput by remember { mutableStateOf("") }
    var countryPrefix by remember { mutableStateOf("+242") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    val isUserMinor = remember(birthDate) {
        val yearPart = birthDate.substringAfterLast("/").toIntOrNull() ?: 2010
        (2026 - yearPart) < 18
    }

    // REAL-TIME FORM VALIDATION STATES
    val isEmailValid by remember(email) {
        derivedStateOf {
            email.trim().contains("@") && email.trim().contains(".") && email.trim().length >= 5
        }
    }

    val passHasMinLength = remember(password) { password.length >= 8 }
    val passHasUpper = remember(password) { password.any { it.isUpperCase() } }
    val passHasDigit = remember(password) { password.any { it.isDigit() } }
    val passHasSpecial = remember(password) { password.any { !it.isLetterOrDigit() } }

    val passwordStrengthScore = remember(password) {
        if (password.isEmpty()) 0
        else {
            var score = 0
            if (passHasMinLength) score++
            if (passHasUpper) score++
            if (passHasDigit) score++
            if (passHasSpecial) score++
            score
        }
    }

    // Dialog state controllers
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Forgot Password Modal Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotPasswordDialog = false },
            onRequestResetCode = onRequestResetCode,
            onConfirmReset = onConfirmResetPassword,
            onResetSuccess = {
                successToast = "Mot de passe réinitialisé. Connectez-vous avec vos nouveaux identifiants."
            }
        )
    }

    // Admin Login Modal Dialog
    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onDismiss = { showAdminLoginDialog = false },
            onAdminLogin = onAdminLoginSubmit,
            onSaveServerConfig = onSaveServerConfig
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1035),
                        Color(0xFF110726),
                        Color(0xFF0A0318)
                    )
                )
            )
            .testTag("login_screen_container")
    ) {
        // Decorative ambient gradient orbs
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(MbotePurplePrimary.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
                .clip(CircleShape)
                .background(Color(0xFFEC4899).copy(alpha = 0.12f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Brand Header: MBoté Logo & Slogan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MbotePurplePrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "M",
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MBoté",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "🇨🇬", fontSize = 20.sp)
                    }
                    Text(
                        text = "La messagerie panafricaine & connectée",
                        fontSize = 11.5.sp,
                        color = Color(0xFFA78BFA),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Auth Mode Selector: [Connexion] | [Créer un compte]
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF241442),
                border = BorderStroke(1.dp, Color(0xFF3B2268)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Connexion Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (authMode == AuthMode.LOGIN) MbotePurplePrimary else Color.Transparent)
                            .clickable {
                                authMode = AuthMode.LOGIN
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("tab_login"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connexion",
                            color = if (authMode == AuthMode.LOGIN) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (authMode == AuthMode.LOGIN) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    // Inscription Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (authMode == AuthMode.REGISTER) MbotePurplePrimary else Color.Transparent)
                            .clickable {
                                authMode = AuthMode.REGISTER
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("tab_register"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Créer un compte",
                            color = if (authMode == AuthMode.REGISTER) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (authMode == AuthMode.REGISTER) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error / Success feedback banner
            errorMessage?.let { error ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = error, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            successToast?.let { success ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = success, color = Color(0xFF6EE7B7), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // FORM FIELDS
            if (authMode == AuthMode.REGISTER) {
                // Full Name Input
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    label = { Text("Nom complet") },
                    placeholder = { Text("ex: Marc Loutala") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MbotePurpleLight)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MbotePurplePrimary,
                        unfocusedBorderColor = Color(0xFF3B2268),
                        focusedLabelColor = MbotePurpleLight,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phone Input with Country Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Country Prefix Dropdown / Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF241442),
                        border = BorderStroke(1.dp, Color(0xFF3B2268)),
                        modifier = Modifier
                            .height(56.dp)
                            .clickable {
                                countryPrefix = when (countryPrefix) {
                                    "+242" -> "+243"
                                    "+243" -> "+225"
                                    "+225" -> "+33"
                                    else -> "+242"
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (countryPrefix) {
                                    "+242" -> "🇨🇬 +242"
                                    "+243" -> "🇨🇩 +243"
                                    "+225" -> "🇨🇮 +225"
                                    else -> "🇫🇷 +33"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMessage = null },
                        label = { Text("Téléphone") },
                        placeholder = { Text("06 123 4567") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = MbotePurpleLight)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MbotePurplePrimary,
                            unfocusedBorderColor = Color(0xFF3B2268),
                            focusedLabelColor = MbotePurpleLight,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_phone_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date of Birth & Age Verification for Minors (Point 5 & 7)
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it; errorMessage = null },
                    label = { Text("Date de naissance (JJ/MM/AAAA) • Âge 🔞") },
                    placeholder = { Text("15/05/2010") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Cake, contentDescription = null, tint = MbotePurpleLight)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MbotePurplePrimary,
                        unfocusedBorderColor = Color(0xFF3B2268),
                        focusedLabelColor = MbotePurpleLight,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_dob_input")
                )

                if (isUserMinor) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚠️ Compte Mineur détecté (Moins de 18 ans)",
                                color = Color(0xFFFCD34D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Protection des mineurs : Veuillez renseigner l'email de votre parent pour activer automatiquement le contrôle parental.",
                                color = Color(0xFFFEF3C7),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = parentEmailInput,
                                onValueChange = { parentEmailInput = it },
                                label = { Text("Email du Parent Associé") },
                                placeholder = { Text("parent@exemple.com") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF3B2268),
                                    focusedLabelColor = Color(0xFFFCD34D),
                                    unfocusedLabelColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Email Field with Real-Time Validation
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Adresse Email") },
                placeholder = { Text("m.loutala@gmail.com") },
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = MbotePurpleLight)
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = if (isEmailValid || email.isBlank()) MbotePurplePrimary else Color(0xFFEF4444),
                    unfocusedBorderColor = if (isEmailValid || email.isBlank()) Color(0xFF3B2268) else Color(0xFFEF4444).copy(alpha = 0.6f),
                    focusedLabelColor = MbotePurpleLight,
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input")
            )

            if (email.isNotBlank() && !isEmailValid) {
                Text(
                    text = "Format d'adresse email invalide (ex: nom@exemple.com)",
                    color = Color(0xFFF87171),
                    fontSize = 11.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Mot de passe") },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MbotePurpleLight)
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "Masquer" else "Afficher",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MbotePurplePrimary,
                    unfocusedBorderColor = Color(0xFF3B2268),
                    focusedLabelColor = MbotePurpleLight,
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input")
            )

            // Dynamic Real-Time Password Strength Meter
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Force du mot de passe :",
                            color = Color(0xFF94A3B8),
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
                                4 -> Color(0xFFA78BFA)
                                else -> Color.Transparent
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeColor = when (passwordStrengthScore) {
                            0, 1 -> Color(0xFFEF4444)
                            2 -> Color(0xFFF59E0B)
                            3 -> Color(0xFF10B981)
                            4 -> Color(0xFFA78BFA)
                            else -> Color.Transparent
                        }
                        for (i in 1..4) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= passwordStrengthScore) activeColor else Color(0xFF3B2268)
                                    )
                            )
                        }
                    }
                    if (authMode == AuthMode.REGISTER) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (passHasMinLength) "✓ 8+ car." else "✕ 8+ car.",
                                color = if (passHasMinLength) Color(0xFF10B981) else Color(0xFF64748B),
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = if (passHasUpper) "✓ Majuscule" else "✕ Majuscule",
                                color = if (passHasUpper) Color(0xFF10B981) else Color(0xFF64748B),
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = if (passHasDigit) "✓ Chiffre" else "✕ Chiffre",
                                color = if (passHasDigit) Color(0xFF10B981) else Color(0xFF64748B),
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = if (passHasSpecial) "✓ Symbole" else "✕ Symbole",
                                color = if (passHasSpecial) Color(0xFF10B981) else Color(0xFF64748B),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remember Me & Forgot Password Row (In Login mode)
            if (authMode == AuthMode.LOGIN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MbotePurplePrimary,
                                uncheckedColor = Color(0xFF94A3B8)
                            )
                        )
                        Text(
                            text = "Se souvenir de moi",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "Mot de passe oublié ?",
                        color = Color(0xFFA78BFA),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                showForgotPasswordDialog = true
                            }
                            .testTag("forgot_password_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Button ("Se connecter" / "Créer un compte")
            Button(
                onClick = {
                    if (email.isBlank() || !isEmailValid) {
                        errorMessage = "Veuillez saisir une adresse email valide."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        if (authMode == AuthMode.LOGIN) {
                            val result = onLoginSubmit(email.trim(), password)
                            isLoading = false
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Identifiants incorrects"
                            }
                        } else {
                            if (fullName.isBlank()) {
                                isLoading = false
                                errorMessage = "Veuillez renseigner votre nom complet."
                                return@launch
                            }
                            val fullPhone = "$countryPrefix ${phone.trim()}".trim()
                            val result = onRegisterSubmit(fullName.trim(), email.trim(), password, fullPhone)
                            isLoading = false
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Erreur d'inscription"
                            }
                        }
                    }
                },
                enabled = !isLoading && isEmailValid && (authMode == AuthMode.LOGIN || password.length >= 6),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MbotePurplePrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_primary_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (authMode == AuthMode.LOGIN) "Se connecter" else "Créer mon compte MBoté",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider: OU
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF3B2268))
                Text(
                    text = "OU CONTINUER AVEC",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF3B2268))
            }

            Spacer(modifier = Modifier.height(18.dp))

            // OAuth Options Row (Google & GitHub)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Sign-In Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = onGoogleLoginSubmit()
                                isLoading = false
                                if (result.isSuccess) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Échec de connexion Google"
                                }
                            }
                        }
                        .testTag("google_login_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🌐", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // GitHub Sign-In Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF24292E),
                    border = BorderStroke(1.dp, Color(0xFF444C56)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = onGitHubLoginSubmit()
                                isLoading = false
                                if (result.isSuccess) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Échec de connexion GitHub"
                                }
                            }
                        }
                        .testTag("github_login_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🐙", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GitHub",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Mode switcher link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Vous n'avez pas de compte ? " else "Vous avez déjà un compte ? ",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Inscrivez-vous" else "Connectez-vous",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        authMode = if (authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                        errorMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Footer Link: Administration Login Link (Requested by user)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1A1230),
                border = BorderStroke(1.dp, Color(0xFF37225B)),
                modifier = Modifier
                    .clickable {
                        showAdminLoginDialog = true
                    }
                    .testTag("admin_login_link_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Espace Administration & Modération 🔐",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloud Server Connection Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Serveur Backend LoukaTech Connecté (Temps Réel)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}
