package com.loukatech.mbote.service.api

import com.loukatech.mbote.model.Comment
import com.loukatech.mbote.model.NewsPost
import com.loukatech.mbote.model.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class CreateActusPostRequest(
    val type: String,
    val content: String,
    val thumbnail: String? = null,
    val durationSeconds: Int? = null,
    val visibility: String = "public",
    val allowed_viewers: List<String> = emptyList(),
    val allowComments: Boolean = true,
    val allowShares: Boolean = true,
    val pinned: Boolean = false
)

@Serializable
data class CreateStatusRequest(
    val type: String,
    val content: String,
    val background: String? = null,
    val visibility: String = "friends",
    val allowed_viewers: List<String> = emptyList(),
    val durationHours: Int = 24,
    val caption: String? = null
)

@Serializable
private data class ReactionRequest(val reaction: String)

@Serializable
private data class ContentRequest(val content: String)

@Serializable
private data class PublicationErrorResponse(val error: String? = null, val message: String? = null)

@Serializable
private data class PublicationShareResponse(val shareCount: Int = 0)

@Serializable
private data class ActusPostDto(
    val id: JsonElement,
    @SerialName("author_id") val authorId: JsonElement? = null,
    @SerialName("author_name") val authorName: String = "",
    @SerialName("author_avatar") val authorAvatar: String = "",
    val type: String = "text",
    val content: String = "",
    val thumbnail: String? = null,
    val visibility: String = "public",
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("share_count") val shareCount: Int = 0,
    @SerialName("reaction_count") val reactionCount: Int = 0,
    @SerialName("my_reaction") val myReaction: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
private data class ActusCommentDto(
    val id: JsonElement,
    @SerialName("user_name") val userName: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("user_avatar") val userAvatar: String = "",
    @SerialName("author_avatar") val authorAvatar: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
private data class StatusDto(
    val id: JsonElement,
    @SerialName("user_id") val userId: JsonElement,
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_avatar") val userAvatar: String = "",
    val type: String = "text",
    val content: String = "",
    val background: String? = null,
    val caption: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("reaction_count") val reactionCount: Int = 0,
    @SerialName("my_reaction") val myReaction: String? = null,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("share_count") val shareCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0
)

/** REST client for the canonical MBoté publication APIs. */
class PublicationApiService {
    private val maxJsonResponseChars = 2_000_000

    private fun readHttpText(inputStream: java.io.InputStream?, endpoint: String): String {
        if (inputStream == null) return ""
        val buffer = CharArray(16 * 1024)
        val response = StringBuilder()
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
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

    private suspend inline fun <reified Req> request(
        endpoint: String,
        method: String = "GET",
        body: Req? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = MboteBackendConfig.authToken?.trim().orEmpty()
        if (token.isBlank() && method != "GET") {
            return@withContext Result.failure(IllegalStateException("Session MBoté requise."))
        }
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("${MboteBackendConfig.baseUrl}$endpoint").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 12_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
            }
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                    it.write(MboteBackendConfig.jsonParser.encodeToString(body))
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = readHttpText(stream, endpoint)
            if (code !in 200..299) {
                val error = runCatching { MboteBackendConfig.jsonParser.decodeFromString<PublicationErrorResponse>(response) }.getOrNull()
                return@withContext Result.failure(IllegalStateException(error?.error ?: error?.message ?: "API HTTP $code"))
            }
            Result.success(response)
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            connection?.disconnect()
        }
    }

    private fun responseArray(payload: String, vararg keys: String): JsonArray {
        val root = MboteBackendConfig.jsonParser.parseToJsonElement(payload)
        if (root is JsonArray) return root
        if (root is JsonObject) {
            val lookupKeys = keys.toList() + listOf("data", "items", "results", "posts", "publications", "statuses")
            lookupKeys.forEach { key ->
                (root[key] as? JsonArray)?.let { return it }
                (root[key] as? JsonObject)?.let { nested ->
                    (nested["data"] as? JsonArray)?.let { return it }
                    (nested["items"] as? JsonArray)?.let { return it }
                }
            }
        }
        return JsonArray(emptyList())
    }

    suspend fun fetchActusPosts(): Result<List<NewsPost>> = request<Unit>("/actus/posts?limit=20")
        .mapCatching { payload ->
            responseArray(payload, "actus", "news", "posts").map {
                mapActus(MboteBackendConfig.jsonParser.decodeFromJsonElement<ActusPostDto>(it))
            }
        }

    suspend fun createActusPost(request: CreateActusPostRequest): Result<NewsPost> =
        request("/actus/posts", "POST", request).mapCatching { mapActus(MboteBackendConfig.jsonParser.decodeFromString(it)) }

    suspend fun reactToActusPost(postId: String, reaction: String): Result<Unit> =
        request("/actus/posts/$postId/reactions", "POST", ReactionRequest(reaction)).map { Unit }

    suspend fun fetchActusComments(postId: String): Result<List<Comment>> =
        request<Unit>("/actus/posts/$postId/comments?limit=30").mapCatching { payload ->
            responseArray(payload, "comments").map { rawComment ->
                val comment = MboteBackendConfig.jsonParser.decodeFromJsonElement<ActusCommentDto>(rawComment)
                Comment(
                    id = comment.id.asId(),
                    authorName = comment.userName.ifBlank { comment.authorName.ifBlank { "Utilisateur MBoté" } },
                    authorAvatar = comment.userAvatar.ifBlank { comment.authorAvatar },
                    text = comment.content,
                    timestamp = comment.createdAt
                )
            }
        }

    suspend fun addActusComment(postId: String, content: String): Result<Unit> =
        request("/actus/posts/$postId/comments", "POST", ContentRequest(content)).map { Unit }

    suspend fun shareActusPost(postId: String): Result<Int> =
        request("/actus/posts/$postId/shares", "POST", emptyMap<String, String>()).mapCatching {
            MboteBackendConfig.jsonParser.decodeFromString<PublicationShareResponse>(it).shareCount
        }

    suspend fun fetchStatuses(currentUserId: String): Result<List<StatusItem>> = request<Unit>("/status")
        .mapCatching { payload ->
            responseArray(payload, "status", "statuses", "stories").map {
                mapStatus(MboteBackendConfig.jsonParser.decodeFromJsonElement<StatusDto>(it), currentUserId)
            }
        }

    suspend fun createStatus(request: CreateStatusRequest, currentUserId: String): Result<StatusItem> =
        this.request("/status/publications", "POST", request).mapCatching {
            mapStatus(MboteBackendConfig.jsonParser.decodeFromString(it), currentUserId)
        }

    suspend fun markStatusViewed(statusId: String): Result<Unit> =
        request<Unit>("/status/$statusId/views", "POST").map { Unit }

    suspend fun reactToStatus(statusId: String, reaction: String): Result<Unit> =
        request("/status/$statusId/reactions", "POST", ReactionRequest(reaction)).map { Unit }

    suspend fun commentStatus(statusId: String, content: String): Result<Unit> =
        request("/status/$statusId/comments", "POST", ContentRequest(content)).map { Unit }

    suspend fun shareStatus(statusId: String): Result<Unit> =
        request<Unit>("/status/$statusId/shares", "POST").map { Unit }

    suspend fun deleteStatus(statusId: String): Result<Unit> =
        request<Unit>("/status/$statusId", "DELETE").map { Unit }

    private fun mapActus(dto: ActusPostDto): NewsPost {
        val mediaUrl = if (dto.type == "text") null else dto.content
        val description = if (dto.type == "text") dto.content else dto.thumbnail.orEmpty()
        val lines = description.lines().filter(String::isNotBlank)
        return NewsPost(
            id = dto.id.asId(),
            authorName = dto.authorName.ifBlank { "Utilisateur MBoté" },
            authorAvatar = dto.authorAvatar,
            authorRole = "Actus",
            category = dto.visibility,
            title = lines.firstOrNull().orEmpty(),
            content = lines.drop(1).joinToString("\n").ifBlank { description },
            imageUrl = mediaUrl,
            timestamp = dto.createdAt,
            likesCount = dto.reactionCount,
            commentsCount = dto.commentCount,
            isLiked = dto.myReaction != null,
            sharesCount = dto.shareCount,
            mediaType = dto.type
        )
    }

    private fun mapStatus(dto: StatusDto, currentUserId: String) = StatusItem(
        id = dto.id.asId(),
        userName = dto.userName.ifBlank { "Utilisateur MBoté" },
        userAvatar = dto.userAvatar,
        timestamp = dto.createdAt,
        text = if (dto.type == "text") dto.content else dto.caption,
        imageUrl = if (dto.type == "image") dto.content else null,
        audioUrl = if (dto.type == "audio") dto.content else null,
        isAudioStatus = dto.type == "audio",
        isViewed = dto.viewCount > 0,
        isMine = dto.userId.asId() == currentUserId,
        reactionsCount = dto.reactionCount,
        commentsCount = dto.commentCount,
        sharesCount = dto.shareCount,
        userReaction = dto.myReaction,
        background = dto.background
    )

    private fun JsonElement.asId(): String = toString().trim('"')
}
