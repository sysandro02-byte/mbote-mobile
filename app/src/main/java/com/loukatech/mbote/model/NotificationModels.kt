package com.loukatech.mbote.model

import java.util.UUID

enum class NotificationType {
    MESSAGE,
    JOB_APPLICATION,
    VIDEO_LIKE,
    GIFT_RECEIVED,
    LIVE_MESSAGE,
    LIVE_BROADCAST,
    SYSTEM
}

data class MboteNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val title: String,
    val body: String,
    val timestamp: String = "À l'instant",
    val isRead: Boolean = false,
    val senderAvatar: String? = null,
    val targetId: String? = null,
    val actionText: String? = null
)
