package com.loukatech.mbote.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages_cache")
data class CachedMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val isRead: Boolean,
    val isDelivered: Boolean,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "conversations_cache")
data class CachedConversationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val lastSeenText: String
)

@Entity(tableName = "media_content_cache")
data class CachedMediaEntity(
    @PrimaryKey val id: String,
    val mediaUrl: String,
    val mediaType: String,
    val title: String,
    val localFilePath: String? = null,
    val cachedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "news_cache")
data class CachedNewsEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val authorAvatar: String,
    val category: String,
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

@Entity(tableName = "shorts_cache")
data class CachedShortVideoEntity(
    @PrimaryKey val id: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val description: String,
    val creatorName: String,
    val creatorAvatar: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

@Entity(tableName = "gift_metadata_cache")
data class CachedGiftMetadataEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val priceFcfa: Long,
    val description: String = "",
    val stockCount: Int = 100,
    val isAvailable: Boolean = true
)

@Entity(tableName = "badge_metadata_cache")
data class CachedBadgeMetadataEntity(
    @PrimaryKey val id: String,
    val typeName: String,
    val title: String,
    val shortLabel: String,
    val emoji: String,
    val colorHex: Long,
    val priceFcfa: Long,
    val description: String,
    val isAvailable: Boolean = true
)

@Entity(tableName = "gift_transactions_cache")
data class CachedGiftTransactionEntity(
    @PrimaryKey val id: String,
    val giftId: String,
    val giftName: String,
    val emoji: String,
    val amountFcfa: Long,
    val isReceived: Boolean,
    val counterpartName: String,
    val timestamp: String,
    val status: String = "Disponible"
)

@Dao
interface MboteDao {
    @Query("SELECT * FROM messages_cache WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<CachedMessageEntity>>

    @Query("SELECT * FROM messages_cache WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedMessages(): List<CachedMessageEntity>

    @Query("UPDATE messages_cache SET syncStatus = :status WHERE id = :id")
    suspend fun updateMessageSyncStatus(id: String, status: String)

    @Query("DELETE FROM messages_cache WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CachedMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessageEntity>)

    @Query("SELECT * FROM conversations_cache")
    fun getAllConversations(): Flow<List<CachedConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: CachedConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<CachedConversationEntity>)

    // Media Cache
    @Query("SELECT * FROM media_content_cache")
    fun getAllCachedMedia(): Flow<List<CachedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMedia(media: List<CachedMediaEntity>)

    // News Cache
    @Query("SELECT * FROM news_cache")
    fun getAllCachedNews(): Flow<List<CachedNewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedNews(news: List<CachedNewsEntity>)

    // Shorts Cache
    @Query("SELECT * FROM shorts_cache")
    fun getAllCachedShorts(): Flow<List<CachedShortVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedShorts(shorts: List<CachedShortVideoEntity>)

    // Gift Metadata Cache
    @Query("SELECT * FROM gift_metadata_cache")
    fun getAllCachedGifts(): Flow<List<CachedGiftMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedGifts(gifts: List<CachedGiftMetadataEntity>)

    // Badge Metadata Cache
    @Query("SELECT * FROM badge_metadata_cache")
    fun getAllCachedBadges(): Flow<List<CachedBadgeMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedBadges(badges: List<CachedBadgeMetadataEntity>)

    // Gift Transactions Cache
    @Query("SELECT * FROM gift_transactions_cache ORDER BY id DESC")
    fun getAllCachedGiftTransactions(): Flow<List<CachedGiftTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedGiftTransactions(txs: List<CachedGiftTransactionEntity>)
}

@Database(
    entities = [
        CachedMessageEntity::class,
        CachedConversationEntity::class,
        CachedMediaEntity::class,
        CachedNewsEntity::class,
        CachedShortVideoEntity::class,
        CachedGiftMetadataEntity::class,
        CachedBadgeMetadataEntity::class,
        CachedGiftTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MboteRoomDatabase : RoomDatabase() {
    abstract fun mboteDao(): MboteDao

    companion object {
        @Volatile
        private var INSTANCE: MboteRoomDatabase? = null

        fun getDatabase(context: android.content.Context): MboteRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MboteRoomDatabase::class.java,
                    "mbote_offline_cache.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
