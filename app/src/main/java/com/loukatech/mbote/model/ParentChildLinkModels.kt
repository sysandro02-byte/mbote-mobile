package com.loukatech.mbote.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ChildInstalledApp(
    val packageName: String,
    val appName: String,
    val category: String, // "Réseaux Sociaux", "Jeux", "Streaming", "Éducation", "Messagerie", "Utilitaires", "Musique"
    val iconEmoji: String,
    val isBlocked: Boolean = false,
    val dailyUsageMinutes: Int = 0,
    val ageRating: String = "3+",
    val restrictedDuringSchoolHours: Boolean = false,
    val lastUsed: String = "Aujourd'hui"
)

@Immutable
@Serializable
data class ChildPanicAlert(
    val alertId: String = "PANIC-" + System.currentTimeMillis(),
    val childId: String = "MB-CHILD-88392",
    val childName: String = "Junior Loutala",
    val childAvatar: String = "https://images.unsplash.com/photo-1543610892-0b1f7e6d8ac1?w=150&auto=format&fit=crop&q=80",
    val timestamp: String = "À l'instant",
    val latitude: Double = -4.2634,
    val longitude: Double = 15.2429,
    val address: String = "Avenue de l'Indépendance, Poto-Poto, Brazzaville",
    val city: String = "Brazzaville, Congo",
    val batteryLevel: Int = 88,
    val emergencyType: String = "Bouton Panique Pressé 🚨",
    val emergencyMessage: String = "Alerte de détresse immédiate déclenchée par l'enfant. Localisation GPS transmise en temps réel.",
    val accuracyMeters: Float = 6.5f,
    val networkStatus: String = "4G MTN Congo",
    val isResolved: Boolean = false
)

@Immutable
@Serializable
data class LinkedChildInfo(
    val id: String = "MB-CHILD-88392",
    val name: String = "Junior Loutala",
    val username: String = "@junior_lt",
    val avatar: String = "https://images.unsplash.com/photo-1543610892-0b1f7e6d8ac1?w=150&auto=format&fit=crop&q=80",
    val age: Int = 13,
    val schoolName: String = "Lycée d'Excellence de Brazzaville",
    val deviceModel: String = "Samsung Galaxy A15 (Android 14)",
    val batteryLevel: Int = 88,
    val isOnline: Boolean = true,
    val lastActive: String = "À l'instant",
    val linkToken: String = "MBOTE-LINK-QR-9941-XYZ",
    val installedApps: List<ChildInstalledApp> = defaultChildInstalledApps,
    val lastPanicAlert: ChildPanicAlert? = null
)

val defaultChildInstalledApps = listOf(
    ChildInstalledApp(
        packageName = "com.zhiliaoapp.musically",
        appName = "TikTok",
        category = "Réseaux Sociaux",
        iconEmoji = "🎵",
        isBlocked = true,
        dailyUsageMinutes = 52,
        ageRating = "13+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Il y a 10 min"
    ),
    ChildInstalledApp(
        packageName = "com.instagram.android",
        appName = "Instagram",
        category = "Réseaux Sociaux",
        iconEmoji = "📸",
        isBlocked = true,
        dailyUsageMinutes = 38,
        ageRating = "13+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Il y a 25 min"
    ),
    ChildInstalledApp(
        packageName = "com.roblox.client",
        appName = "Roblox",
        category = "Jeux",
        iconEmoji = "🎮",
        isBlocked = false,
        dailyUsageMinutes = 45,
        ageRating = "7+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Hier"
    ),
    ChildInstalledApp(
        packageName = "com.supercell.brawlstars",
        appName = "Brawl Stars",
        category = "Jeux",
        iconEmoji = "⭐",
        isBlocked = false,
        dailyUsageMinutes = 30,
        ageRating = "9+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Hier"
    ),
    ChildInstalledApp(
        packageName = "com.google.android.youtube",
        appName = "YouTube",
        category = "Streaming",
        iconEmoji = "▶️",
        isBlocked = false,
        dailyUsageMinutes = 60,
        ageRating = "12+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Il y a 1h"
    ),
    ChildInstalledApp(
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        category = "Messagerie",
        iconEmoji = "💬",
        isBlocked = false,
        dailyUsageMinutes = 25,
        ageRating = "13+",
        restrictedDuringSchoolHours = false,
        lastUsed = "Il y a 5 min"
    ),
    ChildInstalledApp(
        packageName = "com.snapchat.android",
        appName = "Snapchat",
        category = "Réseaux Sociaux",
        iconEmoji = "👻",
        isBlocked = true,
        dailyUsageMinutes = 20,
        ageRating = "13+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Il y a 2h"
    ),
    ChildInstalledApp(
        packageName = "com.google.android.apps.classroom",
        appName = "Google Classroom",
        category = "Éducation",
        iconEmoji = "📚",
        isBlocked = false,
        dailyUsageMinutes = 40,
        ageRating = "3+",
        restrictedDuringSchoolHours = false,
        lastUsed = "Aujourd'hui"
    ),
    ChildInstalledApp(
        packageName = "com.duolingo",
        appName = "Duolingo",
        category = "Éducation",
        iconEmoji = "🦉",
        isBlocked = false,
        dailyUsageMinutes = 15,
        ageRating = "3+",
        restrictedDuringSchoolHours = false,
        lastUsed = "Ce matin"
    ),
    ChildInstalledApp(
        packageName = "com.loukatech.mbote",
        appName = "MBoté Chat (Famille)",
        category = "Messagerie",
        iconEmoji = "💜",
        isBlocked = false,
        dailyUsageMinutes = 35,
        ageRating = "3+",
        restrictedDuringSchoolHours = false,
        lastUsed = "En cours"
    ),
    ChildInstalledApp(
        packageName = "com.netflix.mediaclient",
        appName = "Netflix",
        category = "Streaming",
        iconEmoji = "🍿",
        isBlocked = true,
        dailyUsageMinutes = 0,
        ageRating = "12+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Il y a 3 jours"
    ),
    ChildInstalledApp(
        packageName = "com.dts.freefireth",
        appName = "Free Fire MAX",
        category = "Jeux",
        iconEmoji = "🔥",
        isBlocked = true,
        dailyUsageMinutes = 0,
        ageRating = "16+",
        restrictedDuringSchoolHours = true,
        lastUsed = "Il y a 4 jours"
    ),
    ChildInstalledApp(
        packageName = "com.spotify.music",
        appName = "Spotify Music",
        category = "Musique",
        iconEmoji = "🎧",
        isBlocked = false,
        dailyUsageMinutes = 50,
        ageRating = "12+",
        restrictedDuringSchoolHours = false,
        lastUsed = "Il y a 30 min"
    ),
    ChildInstalledApp(
        packageName = "com.sec.android.app.calculator",
        appName = "Calculatrice & Outils",
        category = "Utilitaires",
        iconEmoji = "🧮",
        isBlocked = false,
        dailyUsageMinutes = 10,
        ageRating = "3+",
        restrictedDuringSchoolHours = false,
        lastUsed = "Aujourd'hui"
    )
)

@Immutable
sealed interface ParentChildLinkState {
    data object Idle : ParentChildLinkState
    data object Scanning : ParentChildLinkState
    data class Verifying(
        val qrPayload: String,
        val progress: Float = 0.35f,
        val statusMessage: String = "Chiffrement et vérification de la clé du compte enfant..."
    ) : ParentChildLinkState
    data class Success(
        val childProfile: LinkedChildInfo,
        val linkedAt: String,
        val activeProtections: List<String> = listOf(
            "Quota quotidien strict (2h max par jour)",
            "Verrouillage nocturne automatique (00:00 - 06:00)",
            "Couvre-feu des commentaires (dès 20:00)",
            "Dispatcheur d'alertes SOS Brevo & Push 24/7",
            "Gestion et blocage des applications à distance",
            "Bouton Panique Enfant avec géolocalisation GPS en direct"
        )
    ) : ParentChildLinkState
    data class Error(val message: String) : ParentChildLinkState
}

@Immutable
@Serializable
data class ParentalSubscriptionPlan(
    val id: String,
    val title: String,
    val priceFcfa: Long,
    val period: String,
    val discount: String? = null,
    val maxChildren: Int = 1,
    val isPopular: Boolean = false,
    val description: String,
    val features: List<String>
)
