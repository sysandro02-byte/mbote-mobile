package com.loukatech.mbote.service

import android.util.Log
import com.loukatech.mbote.data.supabase.MboteBackendConfig
import com.loukatech.mbote.model.Message
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
private data class SmartReplyMessage(
    val text: String,
    val isMine: Boolean,
    val senderName: String
)

@Serializable
private data class SmartReplyRequest(
    val messages: List<SmartReplyMessage>,
    val conciseness: String
)

@Serializable
private data class SmartReplyData(val suggestions: List<String> = emptyList())

@Serializable
private data class SmartReplyResponse(
    val success: Boolean,
    val data: SmartReplyData? = null,
    val error: String? = null
)

/**
 * Smart replies use the MBoté backend so the Gemini credential never ships in
 * the APK. No fabricated suggestion is returned when the network or provider
 * is unavailable.
 */
object GeminiService {
    private const val TAG = "GeminiService"
    var isEnabled: Boolean = true
    var conciseness: String = "Balanced"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getSmartReplies(messages: List<Message>): List<String> = withContext(Dispatchers.IO) {
        if (!isEnabled || messages.isEmpty()) return@withContext emptyList()
        val token = MboteBackendConfig.authToken?.trim().orEmpty()
        if (token.isBlank()) return@withContext emptyList()

        val body = SmartReplyRequest(
            messages = messages.takeLast(6).map {
                SmartReplyMessage(
                    text = it.text.take(1000),
                    isMine = it.isMine,
                    senderName = it.senderName.take(80)
                )
            },
            conciseness = conciseness
        )
        val request = Request.Builder()
            .url("${MboteBackendConfig.baseUrl}/ai/smart-replies")
            .header("Authorization", "Bearer $token")
            .post(json.encodeToString(body).toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Smart replies HTTP ${response.code}")
                    return@use emptyList()
                }
                val parsed = json.decodeFromString<SmartReplyResponse>(
                    response.body?.string().orEmpty()
                )
                parsed.data?.suggestions
                    ?.map(String::trim)
                    ?.filter(String::isNotBlank)
                    ?.take(3)
                    .orEmpty()
            }
        }.onFailure { Log.w(TAG, "Smart replies unavailable", it) }
            .getOrDefault(emptyList())
    }
}
