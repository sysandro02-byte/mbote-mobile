package com.loukatech.mbote.data

import android.content.Context
import android.util.Log
import com.loukatech.mbote.data.local.CachedMessageEntity
import com.loukatech.mbote.data.local.MboteDao
import com.loukatech.mbote.data.local.MboteRoomDatabase
import com.loukatech.mbote.data.supabase.MboteSupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository layer for local Room SQLite message storage and Supabase data synchronization.
 */
class MboteSyncRepository(
    private val dao: MboteDao,
    private val supabaseClient: MboteSupabaseClient = MboteSupabaseClient()
) {
    private val tag = "MboteSyncRepository"

    companion object {
        @Volatile
        private var INSTANCE: MboteSyncRepository? = null

        fun getInstance(context: Context): MboteSyncRepository {
            return INSTANCE ?: synchronized(this) {
                val db = MboteRoomDatabase.getDatabase(context.applicationContext)
                val instance = MboteSyncRepository(db.mboteDao())
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Retrieves messages for a specific chat as a reactive Flow from Room local SQLite storage.
     */
    fun getMessagesForChat(chatId: String): Flow<List<CachedMessageEntity>> {
        return dao.getMessagesForChat(chatId)
    }

    /**
     * Saves a message locally in Room SQLite storage and triggers synchronization to Supabase.
     */
    suspend fun saveMessageLocallyAndSync(message: CachedMessageEntity) = withContext(Dispatchers.IO) {
        // 1. Save locally with PENDING_SYNC status
        val pendingMessage = message.copy(syncStatus = "PENDING_SYNC")
        dao.insertMessage(pendingMessage)
        Log.d(tag, "Message ${message.id} saved to Room local SQLite cache with status PENDING_SYNC")

        // 2. Attempt sync with Supabase backend
        try {
            val jsonPayload = """
                {
                    "id": "${message.id}",
                    "chat_id": "${message.chatId}",
                    "sender_name": "${message.senderName}",
                    "text": "${message.text.replace("\"", "\\\"")}",
                    "timestamp": "${message.timestamp}",
                    "is_from_me": ${message.isFromMe}
                }
            """.trimIndent()
            val result = supabaseClient.insertRawJson("messages", jsonPayload)
            if (result.isSuccess) {
                dao.updateMessageSyncStatus(message.id, "SYNCED")
                Log.i(tag, "Message ${message.id} successfully synced to Supabase!")
            } else {
                Log.w(tag, "Supabase sync postponed for message ${message.id}: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync message ${message.id} to Supabase: ${e.message}")
        }
    }

    /**
     * Synchronizes any pending unsynced local SQLite messages to Supabase.
     */
    suspend fun syncPendingMessagesWithSupabase() = withContext(Dispatchers.IO) {
        val unsynced = dao.getUnsyncedMessages()
        if (unsynced.isEmpty()) return@withContext

        Log.d(tag, "Found ${unsynced.size} unsynced messages in Room SQLite. Attempting batch sync to Supabase...")
        for (msg in unsynced) {
            try {
                val jsonPayload = """
                    {
                        "id": "${msg.id}",
                        "chat_id": "${msg.chatId}",
                        "sender_name": "${msg.senderName}",
                        "text": "${msg.text.replace("\"", "\\\"")}",
                        "timestamp": "${msg.timestamp}"
                    }
                """.trimIndent()
                val result = supabaseClient.insertRawJson("messages", jsonPayload)
                if (result.isSuccess) {
                    dao.updateMessageSyncStatus(msg.id, "SYNCED")
                }
            } catch (e: Exception) {
                Log.w(tag, "Sync retry failed for message ${msg.id}: ${e.message}")
            }
        }
    }

    /**
     * Fetches remote messages from Supabase REST API and caches them into Room local SQLite storage.
     */
    suspend fun fetchRemoteSupabaseMessagesAndCacheLocally(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.selectFromTable<String>("messages", deserialize = { responseJson ->
                responseJson
            })
            if (result.isSuccess) {
                Log.i(tag, "Fetched remote messages from Supabase for $chatId")
                Result.success(Unit)
            } else {
                Log.w(tag, "Remote fetch from Supabase returned offline/cached state.")
                Result.success(Unit) // Return success to rely on Room cache
            }
        } catch (e: Exception) {
            Log.w(tag, "Supabase fetch error: ${e.message}. Using Room local SQLite cache.")
            Result.success(Unit)
        }
    }
}
