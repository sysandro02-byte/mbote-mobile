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
import kotlinx.serialization.json.JsonElement
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

@Serializable
data class SendMessageDto(
    val chatId: String,
    val text: String,
    val mediaType: String = "NONE",
    val mediaUrl: String? = null,
    val replyToMessageId: String? = null
)

@Serializable
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
    val isMine: Boolean = false
)

@Serializable
data class ChatDto(
    val id: String,
    val name: String,
    val avatar: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isGroup: Boolean = false,
    val isChannel: Boolean = false
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
private data class ConfirmQrLoginRequest(val pairingToken: String)

@Serializable
private data class ApiErrorResponse(val error: String? = null, val message: String? = null, val code: String? = null)

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

            val responseText = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { it.readText() }
            Log.d(tag, "HTTP $method $endpoint ($responseCode): $responseText")

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

    suspend fun getRegistrationPublicConfig(): Result<RegistrationPublicConfig> =
        executeHttpRequest<Unit, RegistrationPublicConfig>(endpoint = "/public-settings") { json ->
            MboteBackendConfig.jsonParser.decodeFromString<RegistrationPublicConfig>(json)
        }

    suspend fun verifyLoginOtp(pendingUserId: String, otp: String): Result<VerifiedAuthResponse> {
        val response = executeHttpRequest(
            endpoint = "/auth/verify-login-otp",
            method = "POST",
            requestBody = VerifyOtpRequest(pendingUserId, otp)
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<VerifiedAuthResponse>(json) }
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
        ) { json -> MboteBackendConfig.jsonParser.decodeFromString<VerifiedAuthResponse>(json) }
        response.getOrNull()?.let { MboteBackendConfig.authToken = it.token }
        return response
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
            try {
                MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<List<ChatDto>>>(json).data ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
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
            try {
                MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<List<MessageDto>>>(json).data ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Send message API call to backend server
     */
    suspend fun sendMessageApi(dto: SendMessageDto): Result<MessageDto> {
        return executeHttpRequest<SendMessageDto, MessageDto>(
            endpoint = "/chats/${dto.chatId}/messages",
            method = "POST",
            requestBody = dto
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<MessageDto>>(json).data
                ?: throw IllegalStateException("Réponse de message incomplète")
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
            endpoint = "/short-videos?limit=40",
            method = "GET"
        ) { json ->
            MboteBackendConfig.jsonParser.decodeFromString<List<BackendShortVideoDto>>(json).map(::mapShortVideo)
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
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
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
