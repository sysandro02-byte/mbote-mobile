package com.loukatech.mbote.model

data class ChannelSummary(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val subscriberCount: Int = 0,
    val subscribedByMe: Boolean = false,
    val canPublish: Boolean = false
)
