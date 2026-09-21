package com.loukatech.mbote.service.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.HttpURLConnection
import java.net.URL

/**
 * Runtime ICE configuration returned by the authenticated MBoté backend.
 *
 * TURN credentials deliberately never live in BuildConfig or application resources:
 * they can be rotated on the server without rebuilding the Android application.
 */
data class RtcIceServerConfig(
    val urls: List<String>,
    val username: String = "",
    val credential: String = "",
)

class RtcIceConfigService {
    suspend fun fetchIceServers(): List<RtcIceServerConfig> = withContext(Dispatchers.IO) {
        val token = MboteBackendConfig.authToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Session MBoté requise pour initialiser WebRTC")

        val connection = (URL("${MboteBackendConfig.baseUrl}/rtc/ice-servers").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            useCaches = false
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val text = reader.readText()
                if (text.length > 64_000) text.take(64_000) else text
            }.orEmpty()

            if (status !in 200..299) {
                throw IllegalStateException("Configuration WebRTC indisponible ($status)")
            }

            val root = MboteBackendConfig.jsonParser.parseToJsonElement(body) as? JsonObject
                ?: throw IllegalStateException("Réponse WebRTC invalide")
            val data = (root["data"] as? JsonObject) ?: root
            val entries = data["iceServers"] as? JsonArray
                ?: throw IllegalStateException("Serveurs ICE absents")

            val servers = entries.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                val rawUrls = item["urls"]
                val urls = when (rawUrls) {
                    is JsonArray -> rawUrls.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    is JsonPrimitive -> listOfNotNull(rawUrls.contentOrNull)
                    else -> emptyList()
                }.map(String::trim).filter(String::isNotBlank)

                if (urls.isEmpty()) return@mapNotNull null

                RtcIceServerConfig(
                    urls = urls,
                    username = (item["username"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                    credential = (item["credential"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                )
            }

            if (servers.none { server -> server.urls.any { it.startsWith("turn:", true) || it.startsWith("turns:", true) } }) {
                throw IllegalStateException("Aucun relais TURN disponible")
            }
            servers
        } finally {
            connection.disconnect()
        }
    }
}
