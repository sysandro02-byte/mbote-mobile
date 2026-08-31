package com.loukatech.mbote.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
enum class MessageStatus {
    SENT, DELIVERED, READ
}

@Serializable
enum class MediaType {
    NONE, IMAGE, AUDIO, VIDEO, FILE, LOCATION, POLL, PAYMENT, ARON_QUESTION
}

@Serializable
data class PollOption(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val votesCount: Int = 0,
    val voterIds: List<String> = emptyList()
)

@Serializable
data class PollData(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val options: List<PollOption>,
    val isMultipleChoice: Boolean = false,
    val isClosed: Boolean = false
)

@Serializable
data class LocationData(
    val latitude: Double = -4.2634,
    val longitude: Double = 15.2429,
    val placeName: String = "Brazzaville Centre, Congo",
    val isLive: Boolean = false,
    val durationRemainingText: String? = null
)

@Serializable
data class PaymentTransferData(
    val transactionId: String = "MBT-" + UUID.randomUUID().toString().take(8).uppercase(),
    val amount: String = "5 000 FCFA",
    val provider: String = "MTN MoMo",
    val note: String = "Contribution projet",
    val isRequest: Boolean = false,
    val status: String = "Effectué avec succès"
)

@Serializable
data class AronQuestion(
    val id: Int,
    val setNumber: Int, // 1: Curiosité & Découverte, 2: Expériences & Émotions, 3: Intimité & Vulnérabilité
    val questionFr: String,
    val questionLn: String? = null,
    val category: String = "Connexion humaine"
)

@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val senderId: String,
    val senderName: String = "",
    val senderAvatar: String = "",
    val timestamp: String = "12:00",
    val status: MessageStatus = MessageStatus.READ,
    val isMine: Boolean = false,
    val isEncrypted: Boolean = true,
    val mediaType: MediaType = MediaType.NONE,
    val mediaUrl: String? = null,
    val audioDurationSec: Int = 0,
    val isRecalled: Boolean = false,
    val isStarred: Boolean = false,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val translatedText: String? = null,
    val isTranslating: Boolean = false,
    val targetLanguage: String? = null,
    val pollData: PollData? = null,
    val locationData: LocationData? = null,
    val paymentData: PaymentTransferData? = null,
    val aronQuestion: AronQuestion? = null,
    val disappearingDurationSec: Int = 0, // 0 = permanent
    val expiresAtTimestamp: Long? = null
)

@Serializable
data class Participant(
    val id: String,
    val name: String,
    val avatar: String,
    val isOnline: Boolean = true,
    val role: String = "Membre"
)

@Serializable
data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatar: String,
    val lastMessage: String = "",
    val lastMessageTime: String = "Aujourd'hui",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isGroup: Boolean = false,
    val isAI: Boolean = false,
    val isChannel: Boolean = false,
    val isVerified: Boolean = false,
    val isPinned: Boolean = false,
    val disappearingTimerSec: Int = 0, // 0 = off, 5, 3600, 86400, 604800
    val wallpaperColor: String? = null,
    val wallpaperImageUrl: String? = null,
    val participants: List<Participant> = emptyList(),
    val messages: List<Message> = emptyList()
)

@Serializable
enum class CallType {
    INCOMING, OUTGOING, MISSED
}

@Serializable
data class CallItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatar: String,
    val type: CallType,
    val isVideo: Boolean,
    val timestamp: String,
    val durationText: String = "2 min 45 s",
    val phoneNumber: String = "+242 06 123 4567"
)

@Serializable
data class StatusItem(
    val id: String = UUID.randomUUID().toString(),
    val userName: String,
    val userAvatar: String,
    val timestamp: String,
    val text: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val isAudioStatus: Boolean = false,
    val audioDurationSec: Int = 0,
    val isViewed: Boolean = false,
    val isMine: Boolean = false,
    val reactionsCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val userReaction: String? = null,
    val background: String? = null
)

@Serializable
data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val timestamp: String
)

@Serializable
data class NewsPost(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val authorAvatar: String,
    val authorRole: String = "Journaliste MBoté",
    val category: String = "Actualités",
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val sharesCount: Int = 0,
    val mediaType: String = "text"
)

data class MeetingItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val hostName: String,
    val code: String,
    val scheduledTime: String,
    val durationMinutes: Int = 45,
    val isLive: Boolean = false,
    val participantsCount: Int = 4
)

data class JobOffer(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val company: String,
    val companyLogo: String? = null,
    val location: String,
    val type: String = "Temps plein",
    val contractType: String = "CDI",
    val workMode: String = "Hybride",
    val experienceLevel: String = "Intermédiaire",
    val salary: String = "Selon profil",
    val duration: String = "CDI",
    val domain: String = "Tech & Télécoms",
    val description: String,
    val requirements: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val postedDate: String = "Aujourd'hui",
    val deadline: String = "30 Septembre 2026",
    val applicantsCount: Int = 3,
    val applyUrl: String = "https://mbote.app/jobs",
    val likesCount: Int = 12,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
)

data class DiscoverProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val age: Int,
    val city: String,
    val country: String = "Congo",
    val avatar: String,
    val bio: String,
    val matchAffinity: Int = 85, // percentage
    val interests: List<String> = emptyList(),
    val languages: List<String> = listOf("Français", "Lingala"),
    val favoriteAronQuestion: String = "Si vous pouviez inviter n'importe qui dans le monde à dîner, qui choisiriez-vous ?"
)

enum class AppThemeMode(val label: String, val description: String) {
    LIGHT("Clair", "Thème lumineux avec contraste soigné"),
    DARK("Sombre", "Thème nuit reposant pour les yeux"),
    SYSTEM("Système", "S'adapte automatiquement au système")
}

@Serializable
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val description: String
) {
    FRENCH("fr", "Français", "Français", "🇫🇷", "Langue par défaut • République du Congo"),
    LINGALA("ln", "Lingala", "Lingála ya Kongó", "🇨🇬", "Lokóta ya mbóka • Brazzaville & Kinshasa"),
    KITUBA("kt", "Kituba", "Kikongo ya Leta (Kituba)", "🇨🇬", "Munukutuba • Pointe-Noire, Niari, Bouenza"),
    ENGLISH("en", "Anglais", "English (International)", "🇬🇧", "International standard")
}

@Serializable
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val rateToFcfa: Double,
    val flag: String
) {
    FCFA("XAF", "FCFA", "Franc CFA (XAF/XOF)", 1.0, "🇨🇬"),
    CDF("CDF", "FC", "Franc Congolais (CDF)", 4.5, "🇨🇩"), // 1 FCFA ~ 4.5 CDF
    EUR("EUR", "€", "Euro (€)", 0.001524, "🇪🇺"), // 655.957 FCFA = 1 EUR
    USD("USD", "$", "Dollar Américain ($)", 0.00165, "🇺🇸") // ~600 FCFA = 1 USD
}

fun formatAppCurrency(amountFcfa: Long, currency: AppCurrency = AppCurrency.FCFA): String {
    return when (currency) {
        AppCurrency.FCFA -> {
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRENCH).format(amountFcfa)
            "$formatted FCFA"
        }
        AppCurrency.CDF -> {
            val converted = (amountFcfa * currency.rateToFcfa).toLong()
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRENCH).format(converted)
            "$formatted FC"
        }
        AppCurrency.EUR -> {
            val converted = amountFcfa * currency.rateToFcfa
            val formatted = String.format(java.util.Locale.FRENCH, "%.2f", converted)
            "$formatted €"
        }
        AppCurrency.USD -> {
            val converted = amountFcfa * currency.rateToFcfa
            val formatted = String.format(java.util.Locale.US, "%.2f", converted)
            "$$formatted"
        }
    }
}

@Serializable
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val avatar: String = "",
    val coverUrl: String = "",
    val country: String = "Congo",
    val city: String = "Brazzaville",
    val isVerified: Boolean = false,
    val role: String = "USER",
    val status: String = "",
    val e2eEncryptionEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val darkModeEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.FRENCH,
    val currency: AppCurrency = AppCurrency.FCFA,
    val autoTranslateTo: String = "Français",
    val walletBalanceFcfa: Long = 0L,
    val isPremium: Boolean = false,
    val badges: List<BadgeType> = emptyList(),
    val totalGiftsSentFcfa: Long = 0L,
    val channelName: String = "",
    val channelAvatar: String = "",
    val channelBanner: String = "",
    // Parental Control & Child Protection (Protection des mineurs 🔞)
    val dateOfBirth: String = "",
    val isMinor: Boolean = false,
    val parentalControlActive: Boolean = false,
    val isChildAccountLinkedByQrScan: Boolean = false, // 1) Must scan child account QR code to link accounts
    val parentEmail: String = "",
    val nightLockdownEnabled: Boolean = false, // 1) 00:00 to 06:00 lockdown & reduced notifications
    val maxDailyScreenTimeMinutes: Int = 0, // 2) Max 2 hours per day
    val currentScreenTimeMinutes: Int = 0,
    val commentCurfewHour: Int = 20, // 3) Evening comment reaction curfew (e.g. 20:00)
    val schoolHoursRestrictionEnabled: Boolean = false, // 4) School hours (08:00 - 16:00) & night notification limits
    val isLoggedOutDueToQuota: Boolean = false,
    val atRiskActions: List<AtRiskAction> = emptyList()
)

@Serializable
enum class RiskSeverity {
    LOW, MEDIUM, HIGH
}

@Serializable
data class AtRiskAction(
    val timestamp: String,
    val title: String,
    val description: String,
    val severity: RiskSeverity
)

enum class NavigationTab(val label: String) {
    ACTUS("Actus"),
    CALLS("Appel"),
    MESSAGES("Message"),
    SHORTS("Shorts"),
    MASTA("Masta"),
    SETTINGS("Paramètres"),
    MEETINGS("Réunions"),
    DISCOVER("Connexions")
}

@Serializable
data class ReportItem(
    val id: String,
    val type: String, // "Profil", "Annonce", "Actualité", "Vidéo Short", "Réunion"
    val targetName: String,
    val reporterName: String = "Anonyme",
    val reason: String = "Contenu inapproprié ou indésirable",
    val status: String = "Envoyé à l'Admin", // "Envoyé à l'Admin", "Transmis aux Co-Admins", "Transmis aux Modérateurs", "Résolu"
    val timestamp: String = "À l'instant"
)
