package com.loukatech.mbote.model

enum class MastaSubOption(val displayName: String) {
    ONLINE("🟢 En ligne"),
    RECEIVED("Reçu"),
    SENT("Envoyés"),
    FRIENDS("Amis"),
    SUGGESTIONS("Suggestions"),
    RECOMMENDATIONS("Recommandations"),
    CITIES("Villes")
}

data class MastaUser(
    val id: String,
    val name: String,
    val avatar: String,
    val infoSubtitle: String, // e.g. "Travaille chez MTN CONGO" or "Habite à Brazzaville"
    val mutualFriendsCount: Int = 0,
    val mutualFriendsAvatars: List<String> = emptyList(),
    val isOnline: Boolean = false,
    val city: String = "Brazzaville",
    val timeBadge: String? = null, // e.g. "10 h", "4 j", "3 sem"
    val subType: MastaSubOption = MastaSubOption.FRIENDS
)
