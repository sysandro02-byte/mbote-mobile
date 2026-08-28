package com.loukatech.mbote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.loukatech.mbote.MainActivity
import com.loukatech.mbote.model.MboteNotification
import com.loukatech.mbote.model.NotificationType

object MboteNotificationManager {

    const val CHANNEL_MESSAGES = "mbote_channel_messages"
    const val CHANNEL_JOBS = "mbote_channel_jobs"
    const val CHANNEL_LIKES = "mbote_channel_likes"
    const val CHANNEL_LIVE_GIFTS = "mbote_channel_live_gifts"
    const val CHANNEL_LIVE_MESSAGES = "mbote_channel_live_messages"

    private var fcmToken: String? = null

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Channel for Messages
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages MBoté",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes en temps réel pour nouveaux messages, groupes et chaînes"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Channel for Job Applications
            val jobsChannel = NotificationChannel(
                CHANNEL_JOBS,
                "Candidatures & Emplois",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de candidatures et nouvelles offres d'emploi"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Channel for Video Likes & Interactions
            val likesChannel = NotificationChannel(
                CHANNEL_LIKES,
                "Likes & Interactions Shorts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertes de likes et réactions sur vos MBoté Reels"
                enableVibration(false)
                setShowBadge(true)
            }

            // 4. Channel for Live Gifts & Tips
            val giftsChannel = NotificationChannel(
                CHANNEL_LIVE_GIFTS,
                "Cadeaux & Pourboires en Direct",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes instantanées lors de la réception d'un cadeau en direct ou chat"
                enableVibration(true)
                setShowBadge(true)
            }

            // 5. Channel for Live Messages & Broadcasts
            val liveMessagesChannel = NotificationChannel(
                CHANNEL_LIVE_MESSAGES,
                "Messages des Sessions en Direct",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Commentaires et questions en temps réel pendant vos diffusions"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, jobsChannel, likesChannel, giftsChannel, liveMessagesChannel)
            )
        }
    }

    fun dispatchLiveGiftNotification(
        context: Context,
        senderName: String,
        giftName: String,
        emoji: String,
        amountFcfa: Long,
        liveSessionTitle: String = "Session Live"
    ) {
        val notification = MboteNotification(
            type = NotificationType.GIFT_RECEIVED,
            title = "🎁 Nouveau Cadeau Reçu en Direct !",
            body = "$senderName vous a envoyé $emoji $giftName (${amountFcfa} FCFA) pendant « $liveSessionTitle »",
            actionText = "Voir le direct"
        )
        dispatchSystemNotification(context, notification)
    }

    fun dispatchLiveMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        liveSessionTitle: String = "Session Live"
    ) {
        val notification = MboteNotification(
            type = NotificationType.LIVE_MESSAGE,
            title = "💬 Message en direct de $senderName",
            body = "« $messageText » dans $liveSessionTitle",
            actionText = "Répondre"
        )
        dispatchSystemNotification(context, notification)
    }

    fun dispatchSystemNotification(context: Context, notification: MboteNotification) {
        try {
            val channelId = when (notification.type) {
                NotificationType.MESSAGE -> CHANNEL_MESSAGES
                NotificationType.JOB_APPLICATION -> CHANNEL_JOBS
                NotificationType.VIDEO_LIKE -> CHANNEL_LIKES
                NotificationType.GIFT_RECEIVED -> CHANNEL_LIVE_GIFTS
                NotificationType.LIVE_MESSAGE -> CHANNEL_LIVE_MESSAGES
                NotificationType.LIVE_BROADCAST -> CHANNEL_LIVE_MESSAGES
                NotificationType.SYSTEM -> CHANNEL_MESSAGES
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NOTIFICATION_TYPE", notification.type.name)
                putExtra("TARGET_ID", notification.targetId)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                pendingIntentFlags
            )

            val smallIcon = when (notification.type) {
                NotificationType.MESSAGE -> android.R.drawable.stat_notify_chat
                NotificationType.JOB_APPLICATION -> android.R.drawable.ic_dialog_info
                NotificationType.VIDEO_LIKE -> android.R.drawable.star_on
                NotificationType.GIFT_RECEIVED -> android.R.drawable.star_on
                NotificationType.LIVE_MESSAGE -> android.R.drawable.stat_notify_chat
                NotificationType.LIVE_BROADCAST -> android.R.drawable.ic_menu_camera
                NotificationType.SYSTEM -> android.R.drawable.ic_dialog_alert
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setPriority(
                    if (notification.type == NotificationType.VIDEO_LIKE) 
                        NotificationCompat.PRIORITY_DEFAULT 
                    else 
                        NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("com.loukatech.mbote.NOTIFICATIONS")

            val notificationManager = NotificationManagerCompat.from(context)
            // Generate unique integer ID based on string UUID hashCode
            val notificationId = (notification.id.hashCode() and 0x7FFFFFFF)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS missing on Android 13+ or restricted
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateFcmToken(token: String) {
        fcmToken = token
    }

    fun getFcmToken(): String {
        return fcmToken ?: "fcm_token_mbote_demo_device_${System.currentTimeMillis().toString().takeLast(6)}"
    }
}
