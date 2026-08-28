package com.loukatech.mbote.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ShortVideoComment(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String,
    val text: String,
    val timestamp: String = "Il y a 10 min",
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

@Serializable
data class ShortVideo(
    val id: String = UUID.randomUUID().toString(),
    val creatorId: String,
    val creatorName: String,
    val creatorUsername: String,
    val creatorAvatar: String,
    val creatorBio: String = "Créateur de contenus & passionné de culture africaine sur MBoté 🇨🇬",
    val isCreatorVerified: Boolean = false,
    val isFollowing: Boolean = false,
    val videoThumbnailUrl: String,
    val videoPlaybackUrl: String = "",
    val caption: String,
    val hashtags: List<String> = emptyList(),
    val musicTitle: String,
    val musicArtist: String,
    val musicCoverUrl: String = "",
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val userReaction: String? = null, // e.g. "❤️", "🔥", "👏", "😮", "💎", "🇨🇬"
    val reactionsCount: Map<String, Int> = mapOf(
        "❤️" to 142,
        "🔥" to 89,
        "👏" to 45,
        "😮" to 18,
        "💎" to 32,
        "🇨🇬" to 67
    ),
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val bookmarksCount: Int = 0,
    val isBookmarked: Boolean = false,
    val viewsCount: Int = 1,
    val durationFormatted: String = "1:00",
    val location: String? = null,
    val timestamp: String = "Aujourd'hui",
    val category: String = "Culture & Tech", // Culture, Musique, Tech, Humour, Danse
    val comments: List<ShortVideoComment> = emptyList()
)

@Serializable
enum class ShortVideosFeedType {
    FOR_YOU, FOLLOWING, TRENDING
}

