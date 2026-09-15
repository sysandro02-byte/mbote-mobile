package com.loukatech.mbote.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDisplayData(
    val id: String,
    val name: String,
    val avatar: String,
    val subtitle: String = "Ami(e) • MBoté",
    val bio: String = "Heureux(se) d'être sur MBoté ! Échangeons en toute sécurité. 🔐",
    val city: String = "Brazzaville",
    val isCelebrity: Boolean = false,
    val isCompany: Boolean = false,
    val mutualFriends: Int = 0,
    val username: String = "",
    val coverUrl: String = "",
    val category: String = "",
    val followersCount: Int = 0,
    val videosCount: Int = 0,
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val isFollowing: Boolean = false
)
