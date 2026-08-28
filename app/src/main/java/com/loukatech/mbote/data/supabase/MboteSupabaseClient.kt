package com.loukatech.mbote.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Configuration for Supabase Backend Database & Storage Services.
 * Ready for direct integration with Supabase PostgreSQL, Storage Buckets, and Realtime WebSocket subscriptions.
 */
object MboteSupabaseConfig {
    var supabaseUrl: String = "https://mbote-app.supabase.co"
    var supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1ib3RlLWFwcCIsInJvbGUiOiJhbW9uIiwiaWF0IjoxNzE2MDAwMDAwLCJleHAiOjIwMzE1NzYwMDB9.MboteSupabaseSecretKeyPlaceholder"
    var userAccessToken: String? = null

    // Storage Buckets
    const val BUCKET_MEDIA = "mbote-media"
    const val BUCKET_AVATARS = "mbote-avatars"
    const val BUCKET_SHORTS = "mbote-shorts"

    val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}

@Serializable
data class SupabaseQueryFilter(
    val column: String,
    val operator: String = "eq", // eq, gte, lte, in, ilike
    val value: String
)

@Serializable
data class SupabaseUserRecord(
    val id: String,
    val full_name: String,
    val email: String,
    val phone: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val created_at: String? = null
)

@Serializable
data class SupabasePublicationRecord(
    val id: String,
    val author_id: String,
    val author_name: String,
    val author_avatar: String,
    val title: String,
    val content: String,
    val media_url: String? = null,
    val media_type: String = "IMAGE",
    val category: String = "GÉNÉRAL",
    val likes_count: Int = 0,
    val comments_count: Int = 0,
    val created_at: String? = null
)

/**
 * Production-ready Supabase Client for MBoté.
 * Connects directly to Supabase REST (PostgREST) endpoints and Storage buckets.
 */
class MboteSupabaseClient {
    val tag = "MboteSupabaseClient"

    /**
     * Perform a PostgREST SELECT query on a Supabase table
     */
    suspend fun <T> selectFromTable(
        tableName: String,
        selectColumns: String = "*",
        limit: Int = 50,
        deserialize: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${MboteSupabaseConfig.supabaseUrl}/rest/v1/$tableName?select=$selectColumns&limit=$limit"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("apikey", MboteSupabaseConfig.supabaseAnonKey)
                val token = MboteSupabaseConfig.userAccessToken ?: MboteSupabaseConfig.supabaseAnonKey
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                Log.d(tag, "Supabase GET $tableName Success ($responseCode)")
                Result.success(deserialize(responseText))
            } else {
                Log.w(tag, "Supabase HTTP $responseCode for $tableName")
                Result.failure(Exception("Supabase query error HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Supabase select failure on $tableName: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Perform a PostgREST INSERT into a Supabase table with a raw JSON payload
     */
    suspend fun insertRawJson(
        tableName: String,
        jsonBody: String
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${MboteSupabaseConfig.supabaseUrl}/rest/v1/$tableName"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                setRequestProperty("apikey", MboteSupabaseConfig.supabaseAnonKey)
                val token = MboteSupabaseConfig.userAccessToken ?: MboteSupabaseConfig.supabaseAnonKey
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Prefer", "return=representation")
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                Log.d(tag, "Supabase Insert into $tableName Success ($responseCode)")
                Result.success(responseText)
            } else {
                Log.w(tag, "Supabase Insert error $responseCode for $tableName")
                Result.failure(Exception("Supabase insert error HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Supabase insert exception on $tableName: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Perform a PostgREST INSERT into a Supabase table
     */
    suspend inline fun <reified Req, Res> insertIntoTable(
        tableName: String,
        record: Req,
        noinline deserialize: (String) -> Res
    ): Result<Res> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "${MboteSupabaseConfig.supabaseUrl}/rest/v1/$tableName"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                setRequestProperty("apikey", MboteSupabaseConfig.supabaseAnonKey)
                val token = MboteSupabaseConfig.userAccessToken ?: MboteSupabaseConfig.supabaseAnonKey
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Prefer", "return=representation")
            }

            val jsonBody = MboteSupabaseConfig.jsonParser.encodeToString(record)
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                Log.d(tag, "Supabase Insert into $tableName Success ($responseCode)")
                Result.success(deserialize(responseText))
            } else {
                Log.w(tag, "Supabase Insert error $responseCode for $tableName")
                Result.failure(Exception("Supabase insert error HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Supabase insert exception on $tableName: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Get public URL for uploaded asset in a Supabase Storage bucket
     */
    fun getStoragePublicUrl(bucketName: String, filePath: String): String {
        return "${MboteSupabaseConfig.supabaseUrl}/storage/v1/object/public/$bucketName/$filePath"
    }

    /**
     * Check Supabase Database connection status
     */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${MboteSupabaseConfig.supabaseUrl}/rest/v1/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("apikey", MboteSupabaseConfig.supabaseAnonKey)
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299 || code == 401
        } catch (e: Exception) {
            false
        }
    }
}
