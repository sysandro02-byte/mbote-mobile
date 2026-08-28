package com.loukatech.mbote.service

import android.util.Log
import com.loukatech.mbote.BuildConfig
import com.loukatech.mbote.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiInstruction? = null,
    val generationConfig: GeminiConfig? = null
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiInstruction(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    // User settings
    var isEnabled: Boolean = true
    var conciseness: String = "Balanced" // Brief, Balanced, Elaborate

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getSmartReplies(messages: List<Message>): List<String> = withContext(Dispatchers.IO) {
        if (!isEnabled) {
            return@withContext emptyList()
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "gemini-api-key-placeholder") {
            Log.d(TAG, "Gemini API key is placeholder or blank, using fallback suggestions")
            return@withContext getFallbackReplies(messages)
        }

        // Format conversation history
        val historyStr = messages.takeLast(6).joinToString("\n") { msg ->
            val sender = if (msg.isMine) "Moi" else msg.senderName.ifBlank { "L'autre" }
            "$sender: ${msg.text}"
        }

        val lengthInstruction = when (conciseness) {
            "Brief" -> "ultra-courtes (1-2 mots maximum, ex: 'D'accord', 'Oui', 'Mboté')"
            "Elaborate" -> "plus élaborées et détaillées (5-8 mots, de vraies courtes phrases polies)"
            else -> "courtes et naturelles (2-4 mots, ex: 'Ça marche !', 'À plus tard')"
        }

        val prompt = "Voici l'historique récent des messages :\n$historyStr\n\nSuggère exactement 3 réponses adaptées en format JSON."

        val requestBodyObj = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiInstruction(
                parts = listOf(
                    GeminiPart(
                        text = "Tu es un assistant de réponse rapide 'Smart Reply' intégré dans l'application de messagerie congolaise MBoté. " +
                                "Génère exactement 3 suggestions de réponses qui soient $lengthInstruction. " +
                                "Tu peux utiliser le français ou des expressions courantes congolaises (ex. lingala/kituba comme 'Mboté', 'Ça va', 'Ko yoka te', 'Merci mingi') si approprié. " +
                                "Réponds uniquement avec un tableau JSON de chaînes de caractères (ex: [\"D'accord !\", \"À plus tard\", \"Ça marche\"]). Ne mets aucun autre texte en dehors du tableau JSON."
                    )
                )
            ),
            generationConfig = GeminiConfig(
                temperature = 0.5f,
                responseMimeType = "application/json"
            )
        )

        try {
            val requestBodyStr = json.encodeToString(GeminiRequest.serializer(), requestBodyObj)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed with code: ${response.code}")
                    return@withContext getFallbackReplies(messages)
                }

                val bodyStr = response.body?.string() ?: return@withContext getFallbackReplies(messages)
                val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), bodyStr)
                val responseText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!responseText.isNullOrBlank()) {
                    try {
                        val parsed = json.decodeFromString<List<String>>(responseText)
                        if (parsed.isNotEmpty()) {
                            return@withContext parsed.take(3)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse suggestions JSON: $responseText", e)
                        // Try regex fallback to find items in [ ... ]
                        val matches = Regex("\"([^\"]+)\"").findAll(responseText)
                        val items = matches.map { it.groupValues[1] }.toList()
                        if (items.isNotEmpty()) {
                            return@withContext items.take(3)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating smart reply", e)
        }

        return@withContext getFallbackReplies(messages)
    }

    private fun getFallbackReplies(messages: List<Message>): List<String> {
        val lastMsg = messages.lastOrNull { !it.isMine }?.text?.lowercase() ?: ""
        return when {
            lastMsg.contains("mboté") || lastMsg.contains("salut") || lastMsg.contains("bonjour") -> {
                listOf("Mboté ! Ça va ?", "Salut, comment tu vas ?", "Mboté, na sango nini ?")
            }
            lastMsg.contains("merci") -> {
                listOf("Avec plaisir !", "Pas de soucis !", "Na tondo yo mingi (merci) !")
            }
            lastMsg.contains("?") -> {
                listOf("Oui, bien sûr !", "Je ne sais pas trop.", "Je regarde ça de suite.")
            }
            else -> {
                listOf("D'accord !", "Ça marche", "À plus tard !")
            }
        }
    }
}
