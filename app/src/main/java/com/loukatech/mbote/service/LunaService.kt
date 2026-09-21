package com.loukatech.mbote.service

import com.loukatech.mbote.service.api.MboteBackendConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class LunaHistoryMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class LunaRequest(
    val message: String,
    val history: List<LunaHistoryMessage> = emptyList(),
)

@Serializable
private data class LunaData(
    val answer: String,
    val provider: String = "groq",
    val model: String = "",
)

@Serializable
private data class LunaResponse(
    val success: Boolean,
    val data: LunaData? = null,
    val error: String? = null,
)

/**
 * Real Luna transport. Groq credentials remain exclusively on the MBoté backend.
 */
object LunaService {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun ask(
        message: String,
        history: List<LunaHistoryMessage> = emptyList(),
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = MboteBackendConfig.authToken?.trim().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Session MBoté requise."))
        if (message.isBlank()) return@withContext Result.failure(IllegalArgumentException("Message Luna vide."))

        runCatching {
            val body = LunaRequest(
                message = message.trim().take(6000),
                history = history.takeLast(10).map {
                    it.copy(
                        role = if (it.role == "assistant") "assistant" else "user",
                        content = it.content.trim().take(3000),
                    )
                }.filter { it.content.isNotBlank() },
            )
            val request = Request.Builder()
                .url("${MboteBackendConfig.baseUrl}/ai/luna")
                .header("Authorization", "Bearer $token")
                .post(json.encodeToString(body).toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val parsed = json.decodeFromString<LunaResponse>(response.body?.string().orEmpty())
                if (!response.isSuccessful || !parsed.success) {
                    throw IllegalStateException(parsed.error ?: "Luna est temporairement indisponible.")
                }
                parsed.data?.answer?.trim()?.takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("Réponse Luna vide.")
            }
        }
    }
}
