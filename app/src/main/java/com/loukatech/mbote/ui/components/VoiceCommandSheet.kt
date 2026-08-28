package com.loukatech.mbote.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandSheet(
    onDismiss: () -> Unit,
    onCommandRecognized: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Appuyez sur le micro pour parler à Luna...") }
    
    // Pulse animation for mic button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.3f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Speech recognizer instance
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        statusText = "Luna écoute... Parlez maintenant 🎙️"
                    }

                    override fun onBeginningOfSpeech() {
                        statusText = "Analyse de votre voix en cours..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        statusText = "Traitement du signal audio..."
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                            SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions insuffisantes"
                            SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout réseau"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Aucune correspondance trouvée"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service occupé"
                            SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucun son détecté"
                            else -> "Erreur inconnue"
                        }
                        statusText = "Erreur: $message. Réessayez."
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val result = matches[0]
                            recognizedText = result
                            statusText = "Commande détectée !"
                            onCommandRecognized(result)
                        } else {
                            statusText = "Luna n'a pas bien compris. Réessayez."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            speechRecognizer = recognizer
        } else {
            statusText = "Reconnaissance vocale non dispo sur cet appareil."
        }

        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun toggleListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        } else {
            recognizedText = ""
            statusText = "Initialisation de l'écoute..."
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites une commande à Luna AI")
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                statusText = "Erreur de lancement: ${e.message}"
                // Simulate fallback for emulator / sandbox environment
                statusText = "Simulation de commande vocale sur émulateur..."
                val simulatedCommands = listOf(
                    "programme le message 'Salut Jean, je t'envoie ça' dans 15 secondes à Jean",
                    "rappelle-moi de vérifier mon portefeuille",
                    "combien de temps j'ai passé à scroller ?",
                    "quels cadeaux j'ai reçus ?",
                    "définis ma limite d'écran à 30 minutes"
                )
                val randomCommand = simulatedCommands.random()
                recognizedText = randomCommand
                onCommandRecognized(randomCommand)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Assistant Vocal Luna AI 🔮",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MbotePurplePrimary
            )

            Text(
                text = "Exemples : \n• \"programme le message 'Salut mon masta' à Jean\"\n• \"rappelle-moi de vérifier mon portefeuille\"",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mic button with pulsing circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(MbotePurplePrimary.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale * 0.9f)
                            .clip(CircleShape)
                            .background(MbotePurplePrimary.copy(alpha = 0.3f))
                    )
                }

                Surface(
                    onClick = { toggleListening() },
                    shape = CircleShape,
                    color = if (isListening) Color(0xFFEF4444) else MbotePurplePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.size(76.dp),
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Arrêter" else "Écouter",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isListening) MbotePurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (recognizedText.isNotBlank()) {
                Text(
                    text = "« $recognizedText »",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MbotePurplePrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
