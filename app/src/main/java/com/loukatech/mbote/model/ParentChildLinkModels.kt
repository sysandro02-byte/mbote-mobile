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
    val alertId: String,
    val childId: String,
    val childName: String,
    val childAvatar: String = "",
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val city: String = "",
    val batteryLevel: Int = 0,
    val emergencyType: String,
    val emergencyMessage: String,
    val accuracyMeters: Float = 0f,
    val networkStatus: String = "",
    val isResolved: Boolean = false
)

@Immutable
@Serializable
data class LinkedChildInfo(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val avatar: String = "",
    val age: Int = 0,
    val schoolName: String = "",
    val deviceModel: String = "",
    val batteryLevel: Int = 0,
    val isOnline: Boolean = false,
    val lastActive: String = "",
    val linkToken: String = "",
    val installedApps: List<ChildInstalledApp> = emptyList(),
    val lastPanicAlert: ChildPanicAlert? = null
)

val defaultChildInstalledApps: List<ChildInstalledApp> = emptyList()

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
