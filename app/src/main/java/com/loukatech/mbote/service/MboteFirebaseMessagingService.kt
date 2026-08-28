package com.loukatech.mbote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.loukatech.mbote.MainActivity
import com.loukatech.mbote.R

class MboteFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM", "Refreshed FCM token: $token")
        // In production, sync token with backend server
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "MBoté"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Nouveau message sécurisé reçu"
        val type = remoteMessage.data["type"] ?: "message"

        when (type) {
            "gift", "live_gift" -> {
                val sender = remoteMessage.data["sender"] ?: "Un abonné"
                val giftName = remoteMessage.data["gift_name"] ?: "Cadeau Virtuel"
                val emoji = remoteMessage.data["emoji"] ?: "🎁"
                val amount = remoteMessage.data["amount_fcfa"]?.toLongOrNull() ?: 500L
                MboteNotificationManager.dispatchLiveGiftNotification(
                    context = this,
                    senderName = sender,
                    giftName = giftName,
                    emoji = emoji,
                    amountFcfa = amount
                )
            }
            "live_message" -> {
                val sender = remoteMessage.data["sender"] ?: "Spectateur"
                val msg = remoteMessage.data["message"] ?: body
                MboteNotificationManager.dispatchLiveMessageNotification(
                    context = this,
                    senderName = sender,
                    messageText = msg
                )
            }
            "live_broadcast", "live_stream" -> {
                val host = remoteMessage.data["host"] ?: remoteMessage.data["sender"] ?: "Créateur MBoté"
                val streamTitle = remoteMessage.data["title"] ?: body
                showNotification("🔴 $host est en direct !", streamTitle, "live_broadcast")
            }
            else -> {
                showNotification(title, body, type)
            }
        }
    }

    private fun showNotification(title: String, messageBody: String, type: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            pendingIntentFlags
        )

        val channelId = "mbote_push_notifications_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MBoté Notifications Push",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications en temps réel pour les messages et demandes d'amis"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
