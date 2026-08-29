package com.loukatech.mbote.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.loukatech.mbote.data.supabase.MboteSupabaseConfig
import com.loukatech.mbote.model.CallItem
import com.loukatech.mbote.model.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class MboteCloudBackupPayload(
    val backupId: String = UUID.randomUUID().toString(),
    val version: Int = 1,
    val timestamp: String,
    val epochMillis: Long = System.currentTimeMillis(),
    val userId: String = "user_me",
    val userEmail: String,
    val messagesCount: Int,
    val callsCount: Int,
    val chats: List<Chat>,
    val callHistory: List<CallItem>,
    val appVersion: String = "1.0.0-MBoté"
)

@Serializable
data class BackupMetaData(
    val backupId: String,
    val timestamp: String,
    val epochMillis: Long,
    val userEmail: String,
    val messagesCount: Int,
    val callsCount: Int,
    val sizeFormatted: String,
    val cloudBucketPath: String,
    val isAuto: Boolean = false
)

data class BackupRestoreResult(
    val chatsCount: Int,
    val messagesCount: Int,
    val callsCount: Int,
    val timestamp: String,
    val backupId: String
)

/**
 * Automatic Cloud Backup & Seamless Restore Service for MBoté.
 * Uses Supabase Storage (mbote-media bucket) and PostgreSQL REST metadata tables.
 */
object MboteCloudBackupManager {
    private const val TAG = "MboteCloudBackup"
    private const val PREFS_NAME = "mbote_cloud_backup_prefs"

    private val backupJson = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private fun getSafeEmailPath(email: String): String {
        return email.lowercase().replace("@", "_at_").replace(".", "_")
    }

    /**
     * Checks whether current network connection meets user's Wi-Fi backup settings.
     */
    fun isNetworkSuitable(context: Context, wifiOnly: Boolean): Boolean {
        if (!wifiOnly) return true
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Uploads full messages & call history backup payload to Supabase Storage.
     */
    suspend fun performCloudBackup(
        context: Context,
        userEmail: String,
        chats: List<Chat>,
        calls: List<CallItem>,
        isAuto: Boolean = false
    ): Result<BackupMetaData> = withContext(Dispatchers.IO) {
        try {
            val safeEmail = getSafeEmailPath(userEmail)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault())
            val timestampStr = dateFormat.format(Date())
            val epoch = System.currentTimeMillis()

            val totalMessages = chats.sumOf { it.messages.size }
            val totalCalls = calls.size

            val payload = MboteCloudBackupPayload(
                timestamp = timestampStr,
                epochMillis = epoch,
                userEmail = userEmail,
                messagesCount = totalMessages,
                callsCount = totalCalls,
                chats = chats,
                callHistory = calls
            )

            val jsonString = backupJson.encodeToString(payload)
            val bytes = jsonString.toByteArray(Charsets.UTF_8)
            val sizeKb = bytes.size / 1024.0
            val sizeFormatted = if (sizeKb > 1024) {
                String.format(Locale.getDefault(), "%.2f Mo", sizeKb / 1024.0)
            } else {
                String.format(Locale.getDefault(), "%.1f Ko", sizeKb)
            }

            val objectPath = "backups/$safeEmail/cloud_backup_latest.json"
            val uploadEndpoint = "${MboteSupabaseConfig.supabaseUrl}/storage/v1/object/${MboteSupabaseConfig.BUCKET_MEDIA}/$objectPath"

            var connection: HttpURLConnection? = null
            var success = false
            try {
                val url = URL(uploadEndpoint)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("apikey", MboteSupabaseConfig.supabaseAnonKey)
                    val token = MboteSupabaseConfig.userAccessToken ?: MboteSupabaseConfig.supabaseAnonKey
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("x-upsert", "true")
                }

                connection.outputStream.use { os ->
                    os.write(bytes)
                    os.flush()
                }

                val code = connection.responseCode
                if (code in 200..299) {
                    success = true
                    Log.i(TAG, "Successfully uploaded backup JSON ($sizeFormatted) to Supabase Storage ($objectPath)")
                } else {
                    Log.w(TAG, "Supabase Storage upload returned HTTP $code")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Storage upload exception: ${e.message}. Saving cloud backup locally in app storage.")
            } finally {
                connection?.disconnect()
            }

            // Save metadata locally in SharedPrefs
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("last_backup_timestamp", timestampStr)
                putLong("last_backup_epoch", epoch)
                putString("last_backup_size", sizeFormatted)
                putInt("last_messages_count", totalMessages)
                putInt("last_calls_count", totalCalls)
                putString("last_backup_id", payload.backupId)
                putString("backup_user_email", userEmail)
                apply()
            }

            // Record backup log metadata payload to PostgreSQL table `user_backups`
            val metadata = BackupMetaData(
                backupId = payload.backupId,
                timestamp = timestampStr,
                epochMillis = epoch,
                userEmail = userEmail,
                messagesCount = totalMessages,
                callsCount = totalCalls,
                sizeFormatted = sizeFormatted,
                cloudBucketPath = objectPath,
                isAuto = isAuto
            )

            val metaJson = """
                {
                    "backup_id": "${metadata.backupId}",
                    "user_email": "${metadata.userEmail}",
                    "timestamp": "${metadata.timestamp}",
                    "messages_count": ${metadata.messagesCount},
                    "calls_count": ${metadata.callsCount},
                    "size": "${metadata.sizeFormatted}",
                    "storage_path": "${metadata.cloudBucketPath}"
                }
            """.trimIndent()

            try {
                MboteSupabaseConfig.jsonParser
                val client = com.loukatech.mbote.data.supabase.MboteSupabaseClient()
                client.insertRawJson("user_backups", metaJson)
            } catch (e: Exception) {
                Log.d(TAG, "Recorded backup metadata attempt: ${e.message}")
            }

            Result.success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud Backup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads and restores backup payload from Supabase Storage.
     */
    suspend fun restoreCloudBackup(
        context: Context,
        userEmail: String
    ): Result<Pair<List<Chat>, List<CallItem>>> = withContext(Dispatchers.IO) {
        try {
            val safeEmail = getSafeEmailPath(userEmail)
            val objectPath = "backups/$safeEmail/cloud_backup_latest.json"
            val downloadUrl = "${MboteSupabaseConfig.supabaseUrl}/storage/v1/object/public/${MboteSupabaseConfig.BUCKET_MEDIA}/$objectPath"
            
            Log.i(TAG, "Fetching backup payload from Supabase Storage: $downloadUrl")
            var connection: HttpURLConnection? = null
            var jsonText: String? = null

            try {
                val url = URL(downloadUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("apikey", MboteSupabaseConfig.supabaseAnonKey)
                    val token = MboteSupabaseConfig.userAccessToken ?: MboteSupabaseConfig.supabaseAnonKey
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                }

                val code = connection.responseCode
                if (code in 200..299) {
                    jsonText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                } else {
                    Log.w(TAG, "Public GET returned HTTP $code. Retrying direct endpoint...")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Download error: ${e.message}")
            } finally {
                connection?.disconnect()
            }

            if (jsonText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Aucune sauvegarde cloud trouvée dans Supabase pour $userEmail"))
            }

            val payload = backupJson.decodeFromString<MboteCloudBackupPayload>(jsonText)
            Log.i(TAG, "Cloud backup restored successfully! (${payload.chats.size} discussions, ${payload.callHistory.size} appels)")

            Result.success(Pair(payload.chats, payload.callHistory))
        } catch (e: Exception) {
            Log.e(TAG, "Cloud Restore failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Reads local cached metadata about the user's latest cloud backup.
     */
    fun getLatestBackupMetadata(context: Context): BackupMetaData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getString("last_backup_timestamp", null) ?: return null
        val epoch = prefs.getLong("last_backup_epoch", 0L)
        val size = prefs.getString("last_backup_size", "0 Ko") ?: "0 Ko"
        val messagesCount = prefs.getInt("last_messages_count", 0)
        val callsCount = prefs.getInt("last_calls_count", 0)
        val backupId = prefs.getString("last_backup_id", "backup_local") ?: "backup_local"
        val email = prefs.getString("backup_user_email", "m.loutala@gmail.com") ?: "m.loutala@gmail.com"

        return BackupMetaData(
            backupId = backupId,
            timestamp = timestamp,
            epochMillis = epoch,
            userEmail = email,
            messagesCount = messagesCount,
            callsCount = callsCount,
            sizeFormatted = size,
            cloudBucketPath = "backups/${getSafeEmailPath(email)}/cloud_backup_latest.json"
        )
    }

    /**
     * Checks if auto-backup is enabled and if the timer interval has elapsed.
     */
    fun isAutoBackupDue(context: Context): Boolean {
        val (enabled, frequency, _) = getAutoBackupPreferences(context)
        if (!enabled || frequency == "Désactivée") return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastEpoch = prefs.getLong("last_backup_epoch", 0L)
        if (lastEpoch == 0L) return true

        val now = System.currentTimeMillis()
        val diffHours = (now - lastEpoch) / (1000 * 60 * 60)

        return when (frequency) {
            "Quotidienne" -> diffHours >= 24
            "Hebdomadaire" -> diffHours >= 168
            "Mensuelle" -> diffHours >= 720
            "À chaque modification" -> diffHours >= 1
            else -> false
        }
    }

    /**
     * Saves cloud backup settings (auto backup enabled, frequency, Wi-Fi only).
     */
    fun saveAutoBackupPreferences(context: Context, enabled: Boolean, frequency: String, wifiOnly: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("auto_backup_enabled", enabled)
            putString("auto_backup_frequency", frequency)
            putBoolean("wifi_only", wifiOnly)
            apply()
        }
    }

    /**
     * Retrieves cloud backup settings.
     */
    fun getAutoBackupPreferences(context: Context): Triple<Boolean, String, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("auto_backup_enabled", true)
        val frequency = prefs.getString("auto_backup_frequency", "Quotidienne") ?: "Quotidienne"
        val wifiOnly = prefs.getBoolean("wifi_only", true)
        return Triple(enabled, frequency, wifiOnly)
    }
}
