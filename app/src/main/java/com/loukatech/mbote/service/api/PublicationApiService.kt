package com.loukatech.mbote.service.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class PublicationDto(
    val id: String,
    val authorName: String,
    val authorAvatar: String,
    val authorTitle: String = "Membre MBoté",
    val contentText: String,
    val mediaUrl: String? = null,
    val mediaType: String = "IMAGE", // IMAGE, VIDEO, POLL
    val timestamp: String = "À l'instant",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val category: String = "GÉNÉRAL"
)

@Serializable
data class CreatePublicationRequest(
    val authorName: String,
    val authorAvatar: String,
    val contentText: String,
    val mediaUrl: String? = null,
    val mediaType: String = "IMAGE",
    val category: String = "GÉNÉRAL"
)

@Serializable
data class CommentDto(
    val id: String,
    val postId: String,
    val authorName: String,
    val authorAvatar: String,
    val commentText: String,
    val timestamp: String = "À l'instant"
)

@Serializable
private data class CreateCommentRequest(val text: String)

@Serializable
data class LikePublicationResponse(
    val postId: String,
    val isLiked: Boolean,
    val totalLikes: Int
)

class PublicationApiService {
    private val tag = "PublicationApiService"

    /**
     * Fetch Live Publications API Endpoint
     */
    suspend fun fetchPublications(category: String = "TOUS"): Result<List<PublicationDto>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${MboteBackendConfig.baseUrl}/publications?category=$category"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                MboteBackendConfig.authToken?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                val response = MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<List<PublicationDto>>>(responseText)
                Result.success(response.data ?: emptyList())
            } else {
                Result.failure(IllegalStateException("Publications API HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Log.w(tag, "Publication API indisponible: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Publish New Post API Endpoint
     */
    suspend fun publishNewPost(request: CreatePublicationRequest): Result<PublicationDto> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${MboteBackendConfig.baseUrl}/publications"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                MboteBackendConfig.authToken?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

            val jsonBody = MboteBackendConfig.jsonParser.encodeToString(request)
            OutputStreamWriter(connection.outputStream, "UTF-8").use {
                it.write(jsonBody)
                it.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                val response = MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<PublicationDto>>(responseText)
                Result.success(response.data!!)
            } else {
                Result.failure(IllegalStateException("Publication API HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle Like on Publication API
     */
    suspend fun toggleLikePublication(postId: String, currentLiked: Boolean): Result<LikePublicationResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${MboteBackendConfig.baseUrl}/publications/$postId/like"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                MboteBackendConfig.authToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) return@withContext Result.failure(IllegalStateException("Publication API HTTP $responseCode"))
            val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
            Result.success(MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<LikePublicationResponse>>(responseText).data!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Post Comment API
     */
    suspend fun addComment(postId: String, authorName: String, authorAvatar: String, text: String): Result<CommentDto> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL("${MboteBackendConfig.baseUrl}/publications/$postId/comments").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                MboteBackendConfig.authToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            OutputStreamWriter(connection.outputStream, "UTF-8").use { it.write(MboteBackendConfig.jsonParser.encodeToString(CreateCommentRequest(text))) }
            if (connection.responseCode !in 200..299) return@withContext Result.failure(IllegalStateException("Publication API HTTP ${connection.responseCode}"))
            val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
            Result.success(MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<CommentDto>>(responseText).data!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
