package com.loukatech.mbote.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.loukatech.mbote.model.Chat
import com.loukatech.mbote.model.Message
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class ExportedMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val isMine: Boolean,
    val text: String,
    val timestamp: String,
    val mediaType: String,
    val mediaUrl: String? = null,
    val audioDurationSec: Int = 0,
    val reactions: Map<String, Int> = emptyMap(),
    val translatedText: String? = null,
    val location: ExportedLocation? = null,
    val payment: ExportedPayment? = null,
    val isEncrypted: Boolean = true
)

@Serializable
data class ExportedLocation(
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val isLive: Boolean
)

@Serializable
data class ExportedPayment(
    val transactionId: String,
    val amount: String,
    val provider: String,
    val status: String,
    val note: String
)

@Serializable
data class ExportedParticipant(
    val id: String,
    val name: String,
    val role: String,
    val isOnline: Boolean
)

@Serializable
data class ChatBackupExport(
    val exportVersion: String = "2.0",
    val appName: String = "MBoté Messenger",
    val securityStandard: String = "AES-256 Chiffrement de bout en bout",
    val exportTimestamp: Long,
    val exportDateFormatted: String,
    val chatId: String,
    val chatName: String,
    val isGroup: Boolean,
    val isChannel: Boolean,
    val isAI: Boolean,
    val totalMessages: Int,
    val participants: List<ExportedParticipant>,
    val messages: List<ExportedMessage>
)

data class ChatExportResult(
    val file: File,
    val totalMessages: Int,
    val jsonString: String,
    val formattedSize: String
)

object ChatExportManager {

    private val jsonFormatter = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Serializes the active chat messages and metadata into a local JSON backup file.
     */
    fun exportChatHistory(context: Context, chat: Chat): Result<ChatExportResult> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val now = Date()

            val exportedMessages = chat.messages.map { msg ->
                ExportedMessage(
                    id = msg.id,
                    senderId = msg.senderId,
                    senderName = msg.senderName.ifBlank { if (msg.isMine) "Moi" else chat.name },
                    isMine = msg.isMine,
                    text = msg.text,
                    timestamp = msg.timestamp,
                    mediaType = msg.mediaType.name,
                    mediaUrl = msg.mediaUrl,
                    audioDurationSec = msg.audioDurationSec,
                    reactions = msg.reactions,
                    translatedText = msg.translatedText,
                    location = msg.locationData?.let {
                        ExportedLocation(
                            placeName = it.placeName,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            isLive = it.isLive
                        )
                    },
                    payment = msg.paymentData?.let {
                        ExportedPayment(
                            transactionId = it.transactionId,
                            amount = it.amount,
                            provider = it.provider,
                            status = it.status,
                            note = it.note
                        )
                    },
                    isEncrypted = msg.isEncrypted
                )
            }

            val exportedParticipants = chat.participants.map {
                ExportedParticipant(
                    id = it.id,
                    name = it.name,
                    role = it.role,
                    isOnline = it.isOnline
                )
            }

            val backup = ChatBackupExport(
                exportTimestamp = now.time,
                exportDateFormatted = dateFormat.format(now),
                chatId = chat.id,
                chatName = chat.name,
                isGroup = chat.isGroup,
                isChannel = chat.isChannel,
                isAI = chat.isAI,
                totalMessages = exportedMessages.size,
                participants = exportedParticipants,
                messages = exportedMessages
            )

            val jsonString = jsonFormatter.encodeToString(backup)

            // Save to internal backups directory
            val backupDir = File(context.filesDir, "backups").apply {
                if (!exists()) mkdirs()
            }

            val sanitizedName = chat.name
                .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                .take(20)
                .lowercase()
                .ifEmpty { "chat" }

            val fileName = "mbote_export_${sanitizedName}_${fileDateFormat.format(now)}.json"
            val targetFile = File(backupDir, fileName)

            FileWriter(targetFile).use { writer ->
                writer.write(jsonString)
            }

            val bytes = targetFile.length()
            val formattedSize = when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
            }

            Result.success(
                ChatExportResult(
                    file = targetFile,
                    totalMessages = exportedMessages.size,
                    jsonString = jsonString,
                    formattedSize = formattedSize
                )
            )
        } catch (e: Exception) {
            Log.e("ChatExportManager", "Failed to export chat history", e)
            Result.failure(e)
        }
    }

    /**
     * Copies the formatted JSON string to Android Clipboard
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MBoté Chat JSON Backup", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "JSON copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
    }

    /**
     * Shares the JSON backup content via Android System Share sheet
     */
    fun shareExportJson(context: Context, chatName: String, jsonString: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, jsonString)
                putExtra(Intent.EXTRA_TITLE, "Sauvegarde MBoté - $chatName.json")
                putExtra(Intent.EXTRA_SUBJECT, "Export JSON Discussion MBoté: $chatName")
                type = "application/json"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Partager la sauvegarde JSON")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e("ChatExportManager", "Error sharing JSON export", e)
            Toast.makeText(context, "Erreur lors du partage : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
