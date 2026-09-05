package com.loukatech.mbote.service.api

import android.util.Log
import android.content.Context
import android.net.Uri
import com.loukatech.mbote.BuildConfig
import com.loukatech.mbote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Global Configuration for MBoté Backend Server & API Endpoints.
 * The server address is injected at build time.  It is intentionally blank until
 * MBOTE_API_BASE_URL is configured; authentication must never fall back to a fake account.
 */
object MboteBackendConfig {
    var baseUrl: String = BuildConfig.MBOTE_API_BASE_URL.trimEnd('/')
    var authToken: String? = null
    var refreshToken: String? = null
    var adminToken: String? = null
    var isServerConnected: Boolean = false
    var lastPingMs: Long = 0L
    var serverEnvironment: String = "Non configuré"

    val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}

// ---------------------------------------------------------------------------------
// AUTH & USER DTOs
// ---------------------------------------------------------------------------------
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String = "android_device_main",
    val pushToken: String? = null
)

@Serializable
data class RegisterRequest(
    val accountType: String,
    val accountVisibility: String,
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val phoneNumber: String,
    val birthDate: String? = null,
    val country: String,
    val city: String,
    val address: String,
    val gender: String? = null,
    val bio: String = "",
    val business: BusinessRegistrationRequest? = null
)

@Serializable
data class BusinessRegistrationRequest(
    val name: String,
    val category: String,
    val registrationNumber: String,
    val taxId: String = "",
    val website: String = "",
    val representativeName: String
)

@Serializable
data class PendingOtpChallenge(
    val requiresOtpVerification: Boolean = true,
    val pendingUserId: JsonElement,
    val deliveryChannel: String,
    val target: String,
    val flow: String,
    val devOtp: String? = null
)

@Serializable
data class VerifyOtpRequest(val pendingUserId: String, val otp: String)

@Serializable
data class AuthUserData(
    val id: JsonElement,
    val name: String = "",
    val username: String = "",
    val email: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    val avatar: String = "",
    val country: String = "",
    val city: String = "",
    @SerialName("account_type") val accountType: String = "personal",
    @SerialName("account_visibility") val accountVisibility: String = "public"
)

@Serializable
data class VerifiedAuthResponse(val token: String, val user: AuthUserData)

@Serializable
data class RegistrationPublicConfig(
    val termsOfService: String = "",
    val privacyPolicy: String = "",
    val businessCategories: List<String> = emptyList()
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String? = null,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
private data class ForgotPasswordResponse(val success: Boolean = false, val message: String? = null, val resetUrl: String? = null)

@Serializable
data class ResetPasswordConfirmRequest(
    val email: String,
    val resetCode: String,
    val newPassword: String
)

@Serializable
data class ProfileUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val bio: String? = null,
    val avatar: String? = null,
    val coverUrl: String? = null
)

@Serializable
private data class UserSettingsRequest(val value: JsonObject)

@Serializable
private data class UserSettingsResponse(val value: JsonObject = JsonObject(emptyMap()))

@Serializable
private data class DeleteAccountResponse(val ok: Boolean = false)

@Serializable
data class AdminLoginRequest(
    val adminKey: String,
    val email: String,
    val password: String
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)

@Serializable
data class AuthResponseData(
    val token: String,
    val refreshToken: String? = null,
    val userId: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val avatar: String = "",
    val role: String = "USER",
    val isVerified: Boolean = true
)

@Serializable
data class AdminStatsData(
    val activeUsersCount: Int = 12450,
    val onlineNowCount: Int = 3840,
    val totalMessagesToday: Long = 98450L,
    val activeCallsCount: Int = 142,
    val shortVideosTotal: Int = 1280,
    val totalMobileMoneyTipsFcfa: Long = 4850000L,
    val serverUptimeSec: Long = 1249500L,
    val cpuUsagePercent: Float = 14.8f,
    val ramUsageMb: Int = 512,
    val databaseStatus: String = "Opérationnel (PostgreSQL 16 High-Availability)",
    val apiVersion: String = "v1.4.2-mbote-prod"
)

data class SendMessageDto(
    val chatId: String,
    val text: String,
    val mediaType: String = "NONE",
    val mediaUrl: String? = null,
    val replyToMessageId: String? = null,
    val metadata: JsonObject? = null
)

@Serializable
private data class BackendSendMessageRequest(
    val content: String,
    val type: String = "text",
    val metadata: JsonObject? = null
)

@Serializable
private data class ChatReactionRequest(val emoji: String)

data class ChatParticipantDto(
    val id: String,
    val name: String,
    val avatar: String = "",
    val isOnline: Boolean = false,
    val role: String = "Membre"
)

data class MessageDto(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val text: String,
    val timestamp: String,
    val mediaType: String = "TEXT",
    val mediaUrl: String? = null,
    val isStarred: Boolean = false,
    val isMine: Boolean = false,
    val status: String = "sent",
    val isRecalled: Boolean = false,
    val reactions: Map<String, Int> = emptyMap(),
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val audioDurationSec: Int = 0,
    val metadata: JsonObject? = null
)

data class ChatDto(
    val id: String,
    val name: String,
    val avatar: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isGroup: Boolean = false,
    val isChannel: Boolean = false,
    val participants: List<ChatParticipantDto> = emptyList(),
    val disappearingDurationSec: Int = 0
)

@Serializable
private data class CreateChatRequest(
    val isGroup: Boolean,
    val name: String,
    val participantIds: List<Int>
)

@Serializable
private data class CreatedEntityResponse(val id: JsonElement)

@Serializable
private data class CreateChannelRequest(
    val name: String,
    val description: String,
    val slug: String,
    val category: String = "Public",
    val privacy: String = "public",
    val language: String = "fr"
)

@Serializable
private data class CreateChannelPostRequest(
    val type: String = "text",
    val content: String,
    val visibility: String = "public",
    val allowComments: Boolean = true,
    val allowShares: Boolean = true
)

@Serializable
private data class BackendChannelDto(
    val id: JsonElement,
    val name: String = "",
    val description: String = "",
    val category: String? = null,
    @SerialName("inferred_category") val inferredCategory: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("subscriber_count") val subscriberCount: Int = 0,
    @SerialName("subscribed_by_me") val subscribedByMe: Boolean = false,
    @SerialName("can_publish") val canPublish: Boolean = false
)

@Serializable
private data class ConfirmQrLoginRequest(val pairingToken: String)

@Serializable
private data class ApiErrorResponse(val error: String? = null, val message: String? = null, val code: String? = null)

@Serializable
data class MediaSearchItem(
    val id: String,
    val title: String = "",
    val url: String,
    val previewUrl: String = "",
    val type: String = "gif"
)

@Serializable
private data class MediaSearchResponse(val items: List<MediaSearchItem> = emptyList())

@Serializable
private data class PaymentIntentRequest(val provider: String, val amount: Long, val currency: String, val phone: String)

@Serializable
data class PaymentIntentResponse(
    val id: String,
    val provider: String,
    val status: String,
    val amount: Long,
    val currency: String,
    val merchantCode: String? = null,
    val ussdCode: String? = null,
    val instructions: String? = null
)

@Serializable
private data class PublicMastaUserDto(
    val id: JsonElement,
    val name: String = "",
    val username: String = "",
    val avatar: String? = null,
    val bio: String? = null,
    val country: String? = null,
    val city: String? = null,
    @SerialName("relationship_status") val relationshipStatus: String = "none",
    @SerialName("suggestion_reason") val suggestionReason: String = "Compte public MBoté"
)

@Serializable
private data class BackendShortVideoDto(
    val id: JsonElement,
    @SerialName("user_id") val userId: JsonElement,
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_username") val userUsername: String = "",
    @SerialName("user_avatar") val userAvatar: String? = null,
    val caption: String = "",
    @SerialName("video_url") val videoUrl: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("music_name") val musicName: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("liked_by_me") val likedByMe: Boolean = false,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("share_count") val shareCount: Int = 0,
    @SerialName("bookmark_count") val bookmarkCount: Int = 0,
    @SerialName("saved_by_me") val savedByMe: Boolean = false,
    @SerialName("followed_by_me") val followedByMe: Boolean = false,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
private data class CreateShortVideoRequest(
    val caption: String,
    val videoUrl: String,
    val durationSeconds: Int,
    val musicName: String? = null,
    val thumbnailUrl: String? = null,
    val visibility: String = "public"
)

@Serializable
private data class ShortLikeResponse(val likeCount: Int = 0, val likedByMe: Boolean = false)

@Serializable
private data class ShortBookmarkResponse(val bookmarkCount: Int = 0, val savedByMe: Boolean = false)

@Serializable
private data class ShortFollowResponse(val followerCount: Int = 0, val followedByMe: Boolean = false)

@Serializable
private data class ShortShareResponse(val shareCount: Int = 0)

@Serializable
private data class ShortUploadResponse(val url: String)

@Serializable
private data class BackendShortCommentDto(
    val id: JsonElement,
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_avatar") val userAvatar: String? = null,
    val content: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
private data class CreateShortCommentRequest(val content: String)

/**
 * Full Production-Ready REST API Service for MBoté
 * Performs real HTTP REST calls with JSON payloads, Bearer Authorization, and error handling.
 */
class MboteApiService {
    private val maxJsonResponseChars = 2_000_000
    private val maxLogBodyChars = 1_200

    private fun JsonObject.element(vararg keys: String): JsonElement? =
        keys.firstNotNullOfOrNull { key -> this[key]?.takeUnless { it is JsonNull } }

    private fun JsonObject.string(vararg keys: String, default: String = ""): String =
        element(*keys)?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.boolean(vararg keys: String, default: Boolean = false): Boolean =
        element(*keys)?.jsonPrimitive?.booleanOrNull ?: default

    private fun JsonObject.integer(vararg keys: String, default: Int = 0): Int =
        element(*keys)?.jsonPrimitive?.intOrNull ?: default

    private fun responseArray(json: String): JsonArray {
        val root = MboteBackendConfig.jsonParser.parseToJsonElement(json)
        return when (root) {
            is JsonArray -> root
            is JsonObject -> {
                val keys = listOf(
                    "data",
                    "items",
                    "results",
                    "chats",
                    "messages",
                    "shortVideos",
                    "short_videos",
                    "videos",
                    "publications",
                    "posts",
                    "statuses"
                )
                keys.firstNotNullOfOrNull { key ->
                    root[key] as? JsonArray
                        ?: (root[key] as? JsonObject)?.let { nested ->
                            nested["data"] as? JsonArray ?: nested["items"] as? JsonArray
                        }
                } ?: JsonArray(emptyList())
            }
            else -> JsonArray(emptyList())
        }
    }

    private fun responseObject(json: String): JsonObject {
        val root = MboteBackendConfig.jsonParser.parseToJsonElement(json)
        return when (root) {
            is JsonObject -> (root["data"] as? JsonObject) ?: root
            else -> throw IllegalStateException("Réponse serveur invalide")
        }
    }

    private fun readHttpText(inputStream: java.io.InputStream?, endpoint: String): String {
        if (inputStream == null) return ""
        val buffer = CharArray(16 * 1024)
        val response = StringBuilder()
        BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                if (response.length + read > maxJsonResponseChars) {
                    val remaining = maxJsonResponseChars - response.length
                    if (remaining > 0) response.append(buffer, 0, remaining)
                    throw IllegalStateException("Réponse serveur trop volumineuse pour $endpoint")
                }
                response.append(buffer, 0, read)
            }
        }
        return response.toString()
    }

    private fun logHttpResponse(method: String, endpoint: String, responseCode: Int, responseText: String) {
        val preview = responseText
            .replace("\n", " ")
            .replace("\r", " ")
            .take(maxLogBodyChars)
        val suffix = if (responseText.length > maxLogBodyChars) "…(${responseText.length} chars)" else ""
        Log.d(tag, "HTTP $method $endpoint ($responseCode): $preview$suffix")
    }

    private fun parseChatDto(element: JsonElement): ChatDto {
        val obj = element.jsonObject
        val participants = (obj["participants"] as? JsonArray).orEmpty().mapNotNull { item ->
            val participant = item as? JsonObject ?: return@mapNotNull null
            val id = participant.string("id")
            if (id.isBlank()) return@mapNotNull null
            ChatParticipantDto(
                id = id,
                name = participant.string("name", "username", default = "Utilisateur"),
                avatar = participant.string("avatar", "avatar_url"),
                isOnline = participant.boolean("is_online", "isOnline"),
                role = participant.string("role", default = "Membre")
            )
        }
        return ChatDto(
            id = obj.string("id"),
            name = obj.string("name", default = if (obj.boolean("is_group", "isGroup")) "Groupe" else "Discussion"),
            avatar = obj.string("avatar", "avatar_url"),
            lastMessage = obj.string("last_message", "lastMessage"),
            lastMessageTime = obj.string("last_message_at", "lastMessageTime", "created_at"),
            unreadCount = obj.integer("unread_count", "unreadCount"),
            isOnline = obj.boolean("is_online", "isOnline"),
            isGroup = obj.boolean("is_group", "isGroup"),
            isChannel = obj.boolean("is_channel", "isChannel"),
            participants = participants,
            disappearingDurationSec = obj.integer("disappearing_duration", "disappearingDuration")
        )
    }

    private fun parseMessageDto(element: JsonElement): MessageDto {
        val obj = element.jsonObject
        val rawType = obj.string("type", "media_type", "mediaType", default = "text").lowercase()
        val rawContent = obj.string("content", "text")
        val attachment = runCatching { MboteBackendConfig.jsonParser.parseToJsonElement(rawContent) as? JsonObject }.getOrNull()
        val metadata = obj.element("metadata") as? JsonObject
        val reply = (metadata?.element("replyTo", "reply_to") ?: obj.element("reply_context", "replyContext")) as? JsonObject
        val reactions = mutableMapOf<String, Int>()
        when (val rawReactions = obj["reactions"]) {
            is JsonArray -> rawReactions.forEach { reactionElement ->
                val reaction = reactionElement as? JsonObject ?: return@forEach
                val emoji = reaction.string("emoji")
                if (emoji.isNotBlank()) reactions[emoji] = (reactions[emoji] ?: 0) + 1
            }
            is JsonObject -> rawReactions.forEach { (emoji, value) ->
                reactions[emoji] = when (value) {
                    is JsonArray -> value.size
                    is JsonPrimitive -> value.intOrNull ?: 0
                    else -> 0
                }
            }
            else -> Unit
        }
        return MessageDto(
            id = obj.string("id"),
            chatId = obj.string("chat_id", "chatId"),
            senderId = obj.string("sender_id", "senderId"),
            senderName = obj.string("sender_name", "senderName", default = "Utilisateur MBoté"),
            senderAvatar = obj.string("sender_avatar", "senderAvatar"),
            text = attachment?.string("caption")?.takeIf { it.isNotBlank() } ?: when (rawType) {
                "image" -> "Photo"
                "video" -> "Vidéo"
                "audio" -> "Message vocal"
                "file", "document" -> "Document"
                else -> rawContent
            },
            timestamp = obj.string("created_at", "timestamp"),
            mediaType = rawType.uppercase(),
            mediaUrl = attachment?.string("url")?.takeIf { it.isNotBlank() }
                ?: obj.string("media_url", "mediaUrl").takeIf { it.isNotBlank() },
            isStarred = obj.boolean("is_starred", "isStarred"),
            isMine = obj.boolean("is_mine", "isMine"),
            status = obj.string("status", default = "sent"),
            isRecalled = obj.boolean("is_recalled", "isRecalled"),
            reactions = reactions,
            replyToMessageId = reply?.string("message_id", "messageId"),
            replyToText = reply?.string("text"),
            replyToSender = reply?.string("sender_name", "senderName"),
            audioDurationSec = attachment?.integer("durationSeconds") ?: metadata?.integer("durationSeconds") ?: 0,
            metadata = metadata
        )
    }

    private val tag = "MboteApiService"

    /**
     * Executes HTTP Request safely on IO Coroutine Dispatcher
     */
    private suspend inline fun <reified Req, Res> executeHttpRequest(
        endpoint: String,
        method: String = "GET",
        requestBody: Req? = null,
        token: String? = MboteBackendConfig.authToken,
        crossinline deserialize: (String) -> Res
    ): Result<Res> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val fullUrl = if (endpoint.startsWith("http")) endpoint else "${MboteBackendConfig.baseUrl}$endpoint"
            val url = URL(fullUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                token?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

            if (requestBody != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                connection.doOutput = true
                val jsonString = MboteBackendConfig.jsonParser.encodeToString(requestBody)
                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = readHttpText(inputStream, endpoint)
            logHttpResponse(method, endpoint, responseCode, responseText)

            if (responseCode in 200..299) {
                val result = deserialize(responseText)
                Result.success(result)
            } else {
                val serverError = runCatching {
                    MboteBackendConfig.jsonParser.decodeFromString<ApiErrorResponse>(responseText).let { it.error ?: it.message }
                }.getOrNull()
                Result.failure(Exception(serverError ?: "Erreur serveur ($responseCode)"))
            }
        } catch (e: Exception) {
            Log.w(tag, "HTTP request failed for $endpoint: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Ping Server Health & measure Latency
     */
    suspend fun pingServer(): Result<Long> = withContext(Dispatchers.IO) {
        if (MboteBackendConfig.baseUrl.isBlank()) {
            MboteBackendConfig.isServerConnected = false
            return@withContext Result.failure(IllegalStateException("MBOTE_API_BASE_URL n'est pas configurée"))
        }
        val start = System.currentTimeMillis()
        try {
            val url = URL("${MboteBackendConfig.baseUrl}/health")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            conn.responseCode
            conn.disconnect()
            val latency = System.currentTimeMillis() - start
            MboteBackendConfig.lastPingMs = latency
            MboteBackendConfig.isServerConnected = true
            Result.success(latency)
        } catch (e: Exception) {
            MboteBackendConfig.isServerConnected = false
            Result.failure(e)
        }
    }

    /**
     * User Login API
     */
    suspend fun login(request: LoginRequest): Result<PendingOtpChallenge> {
        return executeHttpRequest(
            endpoint = "/auth/login",
            method = "POST",
            requestBody = request
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<PendingOtpChallenge>(json)
        }
    }

    suspend fun searchGiphy(query: String, stickers: Boolean): Result<List<MediaSearchItem>> {
        val safeQuery = java.net.URLEncoder.encode(query.trim().take(50), "UTF-8")
        val type = if (stickers) "sticker" else "gif"
        return executeHttpRequest<Unit, List<MediaSearchItem>>(
            endpoint = "/media/search?type=$type&q=$safeQuery&limit=20"
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<MediaSearchResponse>(json).items }
    }

    suspend fun createPaymentIntent(provider: String, amountFcfa: Long, phone: String): Result<PaymentIntentResponse> =
        executeHttpRequest(
            endpoint = "/payments/intents",
            method = "POST",
            requestBody = PaymentIntentRequest(provider, amountFcfa, "XAF", phone)
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<PaymentIntentResponse>(json) }

    suspend fun getRegistrationPublicConfig(): Result<RegistrationPublicConfig> =
        executeHttpRequest<Unit, RegistrationPublicConfig>(endpoint = "/public-settings") { json ->
            MboteBackendConfig.jsonParser.decodeFromString<RegistrationPublicConfig>(json)
        }

    suspend fun verifyLoginOtp(pendingUserId: String, otp: String): Result<VerifiedAuthResponse> {
        val response = executeHttpRequest(
            endpoint = "/auth/verify-login-otp",
            method = "POST",
            requestBody = VerifyOtpRequest(pendingUserId, otp)
        ) { json -> MboteBackendConfig.jsonParser.decodeFromJsonElement<VerifiedAuthResponse>(responseObject(json)) }
        response.getOrNull()?.let { MboteBackendConfig.authToken = it.token }
        return response
    }

    /**
     * User Registration API
     */
    suspend fun register(request: RegisterRequest): Result<PendingOtpChallenge> {
        return executeHttpRequest(
            endpoint = "/auth/register",
            method = "POST",
            requestBody = request
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<PendingOtpChallenge>(json)
        }
    }

    suspend fun verifyRegistrationOtp(pendingUserId: String, otp: String): Result<VerifiedAuthResponse> {
        val response = executeHttpRequest(
            endpoint = "/auth/verify-registration-otp",
            method = "POST",
            requestBody = VerifyOtpRequest(pendingUserId, otp)
        ) { json -> MboteBackendConfig.jsonParser.decodeFromJsonElement<VerifiedAuthResponse>(responseObject(json)) }
        response.getOrNull()?.let { MboteBackendConfig.authToken = it.token }
        return response
    }

    suspend fun logoutCurrentSession(token: String? = MboteBackendConfig.authToken): Result<Boolean> =
        executeHttpRequest<Unit, Boolean>(
            endpoint = "/auth/logout",
            method = "POST",
            token = token
        ) { true }

    suspend fun updateMyProfile(request: ProfileUpdateRequest): Result<AuthUserData> =
        executeHttpRequest(
            endpoint = "/users/me/profile",
            method = "PUT",
            requestBody = request
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<AuthUserData>(json) }

    suspend fun fetchMySettings(): Result<JsonObject> =
        executeHttpRequest<Unit, JsonObject>(
            endpoint = "/users/me/settings",
            method = "GET"
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<UserSettingsResponse>(json).value }

    suspend fun updateMySettings(settings: JsonObject): Result<JsonObject> =
        executeHttpRequest(
            endpoint = "/users/me/settings",
            method = "PUT",
            requestBody = UserSettingsRequest(settings)
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<UserSettingsResponse>(json).value }

    suspend fun deleteMyAccount(): Result<Boolean> =
        executeHttpRequest<Unit, Boolean>(
            endpoint = "/users/me",
            method = "DELETE"
        ) { json ->
            runCatching { MboteBackendConfig.jsonParser.decodeFromString<DeleteAccountResponse>(json).ok }.getOrDefault(true)
        }

    /**
     * Google Sign-In API
     */
    suspend fun loginWithGoogle(request: GoogleAuthRequest): Result<AuthResponseData> {
        val response = executeHttpRequest(
            endpoint = "/auth/google",
            method = "POST",
            requestBody = request
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<AuthResponseData>>(json).data
                ?: throw IllegalStateException("Réponse Google incomplète")
        }

        return if (response.isSuccess) {
            val authData = response.getOrNull()!!
            MboteBackendConfig.authToken = authData.token
            Result.success(authData)
        } else Result.failure(response.exceptionOrNull() ?: Exception("Échec de connexion Google"))
    }

    /**
     * Forgot Password Request API
     */
    suspend fun requestForgotPassword(email: String): Result<String> {
        val request = ForgotPasswordRequest(email)
        return executeHttpRequest(
                endpoint = "/auth/forgot-password",
                method = "POST",
                requestBody = request
            ) { json ->
                MboteBackendConfig.jsonParser.decodeFromString<ForgotPasswordResponse>(json).message
                    ?: "Si ce compte existe, un lien de réinitialisation a été envoyé."
            }
    }

    /**
     * Reset Password Confirmation
     */
    suspend fun confirmResetPassword(request: ResetPasswordConfirmRequest): Result<Boolean> {
        return executeHttpRequest(
                endpoint = "/auth/reset-password-confirm",
                method = "POST",
                requestBody = request
            ) { true }
    }

    /**
     * Admin Portal Login API
     */
    suspend fun loginAdmin(request: AdminLoginRequest): Result<AdminStatsData> {
        return Result.failure(UnsupportedOperationException("Connectez-vous avec un compte administrateur réel."))
    }

    /**
     * Fetch Live Admin Statistics
     */
    suspend fun getAdminStats(): Result<AdminStatsData> {
        return executeHttpRequest<Unit, AdminStatsData>(endpoint = "/admin/stats") { json ->
            MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<AdminStatsData>>(json).data
                ?: throw IllegalStateException("Statistiques administrateur indisponibles")
        }
    }

    /**
     * Fetch all user chat conversations from backend REST API
     */
    suspend fun fetchUserChats(): Result<List<ChatDto>> {
        return executeHttpRequest<Unit, List<ChatDto>>(
            endpoint = "/chats",
            method = "GET"
        ) { json ->
            responseArray(json).map(::parseChatDto).filter { it.id.isNotBlank() }
        }
    }

    /**
     * Fetch messages for a specific chat from backend REST API
     */
    suspend fun fetchMessagesForChat(chatId: String): Result<List<MessageDto>> {
        return executeHttpRequest<Unit, List<MessageDto>>(
            endpoint = "/chats/$chatId/messages",
            method = "GET"
        ) { json ->
            responseArray(json).map(::parseMessageDto).filter { it.id.isNotBlank() }
        }
    }

    /**
     * Send message API call to backend server
     */
    suspend fun sendMessageApi(dto: SendMessageDto): Result<MessageDto> {
        val messageType = when (dto.mediaType.uppercase()) {
            "IMAGE" -> "image"
            "VIDEO" -> "video"
            "AUDIO" -> "audio"
            "FILE" -> "file"
            "LOCATION" -> "location"
            "POLL" -> "poll"
            "PAYMENT" -> "payment"
            "ARON_QUESTION" -> "aron_question"
            else -> "text"
        }
        val content = dto.mediaUrl?.takeIf { it.isNotBlank() }?.let { url ->
            buildJsonObject {
                put("url", url)
                put("caption", dto.text)
                if (messageType == "audio") put("name", "Message vocal")
            }.toString()
        } ?: dto.text
        val metadata = buildJsonObject {
            dto.metadata?.forEach { (key, value) -> put(key, value) }
            dto.replyToMessageId?.takeIf { it.isNotBlank() }?.let { messageId ->
                put("replyTo", buildJsonObject { put("messageId", messageId) })
            }
        }.takeIf { it.isNotEmpty() }
        val request = BackendSendMessageRequest(content = content, type = messageType, metadata = metadata)
        return executeHttpRequest<BackendSendMessageRequest, MessageDto>(
            endpoint = "/chats/${dto.chatId}/messages",
            method = "POST",
            requestBody = request
        ) { json ->
            parseMessageDto(responseObject(json))
        }
    }

    suspend fun markChatReadApi(chatId: String): Result<Boolean> =
        executeHttpRequest<Unit, Boolean>(endpoint = "/chats/$chatId/read", method = "POST") { true }

    suspend fun toggleMessageReactionApi(messageId: String, emoji: String): Result<Map<String, Int>> {
        return executeHttpRequest<ChatReactionRequest, Map<String, Int>>(
            endpoint = "/messages/$messageId/reactions",
            method = "POST",
            requestBody = ChatReactionRequest(emoji)
        ) { json ->
            val obj = responseObject(json)
            val reactions = obj["reactions"]
            val counts = mutableMapOf<String, Int>()
            when (reactions) {
                is JsonArray -> reactions.forEach { item ->
                    val reaction = item as? JsonObject ?: return@forEach
                    val value = reaction.string("emoji")
                    if (value.isNotBlank()) counts[value] = (counts[value] ?: 0) + 1
                }
                is JsonObject -> reactions.forEach { (key, value) ->
                    counts[key] = if (value is JsonArray) value.size else value.jsonPrimitive.intOrNull ?: 0
                }
                else -> Unit
            }
            counts
        }
    }

    /**
     * Delete message API endpoint
     */
    suspend fun deleteMessageApi(messageId: String): Result<Boolean> {
        return executeHttpRequest<Unit, Boolean>(
            endpoint = "/messages/$messageId",
            method = "DELETE"
        ) { true }
    }

    /**
     * Star message API endpoint
     */
    suspend fun starMessageApi(messageId: String): Result<Boolean> {
        return executeHttpRequest<Unit, Boolean>(
            endpoint = "/messages/$messageId/star",
            method = "POST"
        ) { true }
    }

    /**
     * Fetch call history from the backend server
     */
    suspend fun fetchCallHistory(): Result<List<CallItem>> {
        return executeHttpRequest<Unit, List<CallItem>>(
            endpoint = "/calls/history",
            method = "GET"
        ) { json ->
            try {
                MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<List<CallItem>>>(json).data ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Log a new call to the backend server
     */
    suspend fun logCallApi(callItem: CallItem): Result<CallItem> {
        return executeHttpRequest<CallItem, CallItem>(
            endpoint = "/calls/log",
            method = "POST",
            requestBody = callItem
        ) { json ->
            try {
                MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<CallItem>>(json).data!!
            } catch (e: Exception) {
                callItem
            }
        }
    }

    /**
     * Fetch Masta users from the backend REST API
     */
    suspend fun fetchMastaUsers(): Result<List<MastaUser>> {
        return executeHttpRequest<Unit, List<MastaUser>>(
            endpoint = "/users/public?limit=100",
            method = "GET"
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<List<PublicMastaUserDto>>(json).map { user ->
                MastaUser(
                    id = user.id.toString().trim('"'),
                    name = user.name.ifBlank { user.username },
                    avatar = user.avatar.orEmpty(),
                    infoSubtitle = user.bio?.takeIf(String::isNotBlank) ?: user.suggestionReason,
                    city = user.city.orEmpty(),
                    subType = if (user.relationshipStatus == "accepted") MastaSubOption.FRIENDS else MastaSubOption.SUGGESTIONS
                )
            }
        }
    }

    /**
     * Fetch all Short videos from backend REST API
     */
    suspend fun fetchShortVideos(): Result<List<ShortVideo>> {
        return executeHttpRequest<Unit, List<ShortVideo>>(
            endpoint = "/short-videos?limit=12",
            method = "GET"
        ) { json ->
            responseArray(json).map {
                mapShortVideo(MboteBackendConfig.jsonParser.decodeFromJsonElement<BackendShortVideoDto>(it))
            }
        }
    }

    /**
     * Create a new Short video on backend server
     */
    suspend fun createShortVideoApi(video: ShortVideo): Result<ShortVideo> {
        val request = CreateShortVideoRequest(
            caption = video.caption,
            videoUrl = video.videoPlaybackUrl,
            durationSeconds = video.durationFormatted.substringBefore(':').toIntOrNull()?.times(60)
                ?.plus(video.durationFormatted.substringAfter(':').toIntOrNull() ?: 0) ?: 0,
            musicName = video.musicTitle,
            thumbnailUrl = video.videoThumbnailUrl
        )
        return executeHttpRequest<CreateShortVideoRequest, ShortVideo>(
            endpoint = "/short-videos",
            method = "POST",
            requestBody = request
        ) { json -> mapShortVideo(MboteBackendConfig.jsonParser.decodeFromString(json)) }
    }

    /**
     * Toggle like for a Short video on backend server
     */
    suspend fun toggleLikeShortVideoApi(videoId: String, isLiked: Boolean): Result<Boolean> {
        return executeHttpRequest<Unit, Boolean>(
            endpoint = "/short-videos/$videoId/likes",
            method = "POST"
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<ShortLikeResponse>(json).likedByMe
        }
    }

    /**
     * Add a comment to a Short video on backend server
     */
    suspend fun addShortVideoCommentApi(videoId: String, comment: ShortVideoComment): Result<ShortVideoComment> {
        return executeHttpRequest<CreateShortCommentRequest, ShortVideoComment>(
            endpoint = "/short-videos/$videoId/comments",
            method = "POST",
            requestBody = CreateShortCommentRequest(comment.text)
        ) { json -> mapShortComment(MboteBackendConfig.jsonParser.decodeFromString(json)) }
    }

    suspend fun confirmDesktopQrLogin(pairingToken: String): Result<Unit> =
        executeHttpRequest(
            endpoint = "/auth/qr/confirm",
            method = "POST",
            requestBody = ConfirmQrLoginRequest(pairingToken)
        ) { Unit }

    suspend fun createGroupApi(name: String, participantIds: List<Int>): Result<String> =
        executeHttpRequest(
            endpoint = "/chats",
            method = "POST",
            requestBody = CreateChatRequest(true, name, participantIds.distinct())
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<CreatedEntityResponse>(json).id.toString().trim('"')
        }

    suspend fun createDirectChatApi(participantId: Int): Result<ChatDto> =
        executeHttpRequest(
            endpoint = "/chats",
            method = "POST",
            requestBody = CreateChatRequest(false, "", listOf(participantId))
        ) { json -> parseChatDto(responseObject(json)) }

    suspend fun createChannelApi(
        name: String,
        description: String,
        isPublic: Boolean,
        initialPost: String
    ): Result<String> {
        val slug = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "chaine-${System.currentTimeMillis()}" }
        val created = executeHttpRequest(
            endpoint = "/channels",
            method = "POST",
            requestBody = CreateChannelRequest(
                name = name,
                description = description,
                slug = slug,
                privacy = if (isPublic) "public" else "private"
            )
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<CreatedEntityResponse>(json).id.toString().trim('"')
        }
        val channelId = created.getOrElse { return Result.failure(it) }
        if (initialPost.isNotBlank()) {
            val post = executeHttpRequest<CreateChannelPostRequest, Unit>(
                endpoint = "/channels/$channelId/posts",
                method = "POST",
                requestBody = CreateChannelPostRequest(
                    content = initialPost,
                    visibility = if (isPublic) "public" else "private"
                )
            ) { Unit }
            if (post.isFailure) return Result.failure(post.exceptionOrNull()!!)
        }
        return Result.success(channelId)
    }

    suspend fun fetchChannels(): Result<List<ChannelSummary>> =
        executeHttpRequest<Unit, List<ChannelSummary>>(endpoint = "/channels") { json ->
            MboteBackendConfig.jsonParser.decodeFromString<List<BackendChannelDto>>(json).map { channel ->
                ChannelSummary(
                    id = channel.id.toString().trim('"'),
                    name = channel.name,
                    description = channel.description,
                    category = channel.category?.takeIf(String::isNotBlank)
                        ?: channel.inferredCategory?.takeIf(String::isNotBlank)
                        ?: "Public",
                    avatarUrl = channel.avatarUrl,
                    bannerUrl = channel.bannerUrl,
                    subscriberCount = channel.subscriberCount,
                    subscribedByMe = channel.subscribedByMe,
                    canPublish = channel.canPublish
                )
            }
        }

    suspend fun setChannelSubscription(channelId: String, subscribe: Boolean): Result<Unit> =
        executeHttpRequest<Unit, Unit>(
            endpoint = "/channels/$channelId/subscribe",
            method = if (subscribe) "POST" else "DELETE"
        ) { Unit }

    suspend fun fetchShortVideoComments(videoId: String): Result<List<ShortVideoComment>> =
        executeHttpRequest<Unit, List<ShortVideoComment>>("/short-videos/$videoId/comments?limit=100") { json ->
            MboteBackendConfig.jsonParser.decodeFromString<List<BackendShortCommentDto>>(json).map(::mapShortComment)
        }

    suspend fun toggleShortBookmark(videoId: String): Result<Pair<Int, Boolean>> =
        executeHttpRequest<Unit, Pair<Int, Boolean>>("/short-videos/$videoId/bookmarks", "POST") { json ->
            val response = MboteBackendConfig.jsonParser.decodeFromString<ShortBookmarkResponse>(json)
            response.bookmarkCount to response.savedByMe
        }

    suspend fun toggleShortFollow(authorId: String): Result<Pair<Int, Boolean>> =
        executeHttpRequest<Unit, Pair<Int, Boolean>>("/short-videos/authors/$authorId/follow", "POST") { json ->
            val response = MboteBackendConfig.jsonParser.decodeFromString<ShortFollowResponse>(json)
            response.followerCount to response.followedByMe
        }

    suspend fun shareShortVideo(videoId: String, targetChatId: String? = null): Result<Int> =
        executeHttpRequest<Map<String, String?>, Int>(
            endpoint = "/short-videos/$videoId/shares",
            method = "POST",
            requestBody = mapOf("targetChatId" to targetChatId)
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<ShortShareResponse>(json).shareCount }

    suspend fun markShortViewed(videoId: String): Result<Unit> =
        executeHttpRequest<Unit, Unit>("/short-videos/$videoId/views", "POST") { Unit }

    suspend fun uploadPublicationVideo(context: Context, source: Uri, surface: String): Result<String> = withContext(Dispatchers.IO) {
        val token = MboteBackendConfig.authToken?.trim().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Session MBoté requise."))
        val safeSurface = if (surface == "actus-videos") "actus-videos" else "short-videos"
        val resolver = context.contentResolver
        val mimeType = resolver.getType(source)?.takeIf { it.startsWith("video/") } ?: "application/octet-stream"
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("${MboteBackendConfig.baseUrl}/uploads/$safeSurface").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setChunkedStreamingMode(256 * 1024)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", mimeType)
                setRequestProperty("Accept", "application/json")
            }
            resolver.openInputStream(source)?.use { input ->
                connection.outputStream.use { output -> input.copyTo(output, 256 * 1024) }
            } ?: return@withContext Result.failure(IllegalStateException("Vidéo Android inaccessible."))
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = readHttpText(stream, "/uploads/$safeSurface")
            if (code !in 200..299) {
                val error = runCatching { MboteBackendConfig.jsonParser.decodeFromString<ApiErrorResponse>(response) }.getOrNull()
                Result.failure(IllegalStateException(error?.error ?: error?.message ?: "Upload HTTP $code"))
            } else {
                Result.success(MboteBackendConfig.jsonParser.decodeFromString<ShortUploadResponse>(response).url)
            }
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            connection?.disconnect()
        }
    }

    private fun mapShortVideo(video: BackendShortVideoDto) = ShortVideo(
        id = video.id.toString().trim('"'),
        creatorId = video.userId.toString().trim('"'),
        creatorName = video.userName,
        creatorUsername = video.userUsername,
        creatorAvatar = video.userAvatar.orEmpty(),
        isFollowing = video.followedByMe,
        videoThumbnailUrl = video.thumbnailUrl.orEmpty(),
        videoPlaybackUrl = video.videoUrl,
        caption = video.caption,
        hashtags = Regex("#[\\p{L}\\p{N}_-]+").findAll(video.caption).map { it.value }.toList(),
        musicTitle = video.musicName.orEmpty(),
        musicArtist = video.userName,
        likesCount = video.likeCount,
        isLiked = video.likedByMe,
        commentsCount = video.commentCount,
        sharesCount = video.shareCount,
        bookmarksCount = video.bookmarkCount,
        isBookmarked = video.savedByMe,
        viewsCount = video.viewCount,
        durationFormatted = "%d:%02d".format(video.durationSeconds / 60, video.durationSeconds % 60),
        timestamp = video.createdAt,
        reactionsCount = emptyMap()
    )

    private fun mapShortComment(comment: BackendShortCommentDto) = ShortVideoComment(
        id = comment.id.toString().trim('"'),
        authorName = comment.userName.ifBlank { "Utilisateur MBoté" },
        authorUsername = "",
        authorAvatar = comment.userAvatar.orEmpty(),
        text = comment.content,
        timestamp = comment.createdAt
    )

    private fun String.capitalizeWords(): String = split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
