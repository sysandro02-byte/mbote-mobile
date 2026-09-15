package com.loukatech.mbote.service.api

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
data class CallParticipantDto(
    val id: String,
    val name: String,
    val avatar: String,
    val isMuted: Boolean = false,
    val isVideoOff: Boolean = false,
    val isHost: Boolean = false,
    val isScreenSharing: Boolean = false,
    val audioVolumeLevel: Float = 0f
)

@Serializable
data class GroupCallSessionDto(
    val roomCode: String,
    val roomTitle: String,
    val isVideoCall: Boolean = true,
    val hostUserId: String,
    val participants: List<CallParticipantDto> = emptyList(),
    val connectionQuality: String = "SERVER_CONNECTED",
    val encryptionStandard: String = "WebRTC E2EE",
    val createdAtTimestamp: Long = 0L
)

@Serializable
data class CreateGroupCallRequest(
    val roomTitle: String,
    val isVideoCall: Boolean = true,
    val participantIds: List<String> = emptyList()
)

@Serializable
data class ParticipantStateUpdateRequest(
    val roomCode: String,
    val userId: String,
    val isMuted: Boolean,
    val isVideoOff: Boolean,
    val isScreenSharing: Boolean = false
)

class GroupCallApiService {
    private suspend inline fun <reified Request, reified Response> request(
        endpoint: String,
        method: String,
        body: Request? = null
    ): Result<Response> = withContext(Dispatchers.IO) {
        val token = MboteBackendConfig.authToken?.takeIf(String::isNotBlank)
            ?: return@withContext Result.failure(IllegalStateException("Session MBoté requise."))
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("${MboteBackendConfig.baseUrl}$endpoint").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
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
            val responseText = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText) }.orEmpty()
            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("Erreur serveur ($code)"))
            }
            val response = MboteBackendConfig.jsonParser.decodeFromString<ApiResponse<Response>>(responseText)
            response.data?.let(Result.Companion::success)
                ?: Result.failure(IllegalStateException("Réponse serveur incomplète"))
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun createGroupCall(request: CreateGroupCallRequest): Result<GroupCallSessionDto> =
        request("/calls/group/create", "POST", request)

    suspend fun joinGroupCall(roomCode: String): Result<GroupCallSessionDto> =
        request<Unit, GroupCallSessionDto>("/calls/group/join/${roomCode.trim()}", "POST")

    suspend fun updateParticipantState(request: ParticipantStateUpdateRequest): Result<Boolean> =
        request("/calls/group/update-state", "PUT", request)

    suspend fun leaveGroupCall(roomCode: String): Result<Boolean> =
        request<Unit, Boolean>("/calls/group/leave/${roomCode.trim()}", "POST")
}
