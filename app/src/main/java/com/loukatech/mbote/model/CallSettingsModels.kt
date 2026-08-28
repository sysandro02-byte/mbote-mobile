package com.loukatech.mbote.model

import java.util.UUID

data class VoicemailItem(
    val id: String = UUID.randomUUID().toString(),
    val callerName: String,
    val callerNumber: String,
    val callerAvatar: String? = null,
    val timestamp: String,
    val durationSeconds: Int,
    val transcription: String,
    val audioUrl: String? = null,
    val isRead: Boolean = false,
    val isImportant: Boolean = false
)

data class CallSettings(
    // Call Assist
    val callerIdAndSpamEnabled: Boolean = true,
    val filterSpamCalls: Boolean = true,
    val verifiedCallsBadge: Boolean = true,

    // General
    val hearingAidCompatibility: Boolean = false,
    val realTimeTextRtt: Boolean = false,
    val liveTranscribeCalls: Boolean = true,
    val assistedDialingEnabled: Boolean = true,
    val defaultCountryCode: String = "+242", // Congo (+242), RDC (+243), France (+33), etc.
    val blockUnknownCallers: Boolean = false,
    val blockedNumbers: List<String> = listOf("+242 06 999 0000", "+243 81 000 9999"),
    
    // Calls & Network
    val callWaitingEnabled: Boolean = true,
    val callForwardingEnabled: Boolean = false,
    val forwardToNumber: String = "",
    val wifiCallingHd: Boolean = true,
    val simSelection: String = "SIM 1 (MTN Congo)",
    
    // Display options
    val sortByNameFirst: Boolean = true,
    val nameDisplayFormat: String = "Prénom d'abord", // "Prénom d'abord", "Nom d'abord"
    val callTheme: String = "Sombre MBoté",
    
    // Quick responses
    val quickResponses: List<String> = listOf(
        "Je ne peux pas parler pour le moment. Que se passe-t-il ?",
        "Je vous rappelle dès que possible.",
        "Je suis en réunion, je vous recontacte plus tard.",
        "Pouvez-vous m'envoyer un message sur MBoté ?"
    ),

    // Sounds & Vibration
    val callRingtone: String = "MBoté Harmony (Par défaut)",
    val vibrateForCalls: Boolean = true,
    val dialpadTones: Boolean = true,
    val vibrateOnAnswer: Boolean = true,
    
    // Voicemail
    val voicemailNumber: String = "123",
    val voicemailNotifications: Boolean = true,
    val voicemailVibration: Boolean = true,
    val visualVoicemailActive: Boolean = false, // toggled when user retries / activates

    // Contact Ringtones
    val contactRingtones: Map<String, String> = mapOf(
        "Grace Makiese" to "MBoté VIP Flute",
        "Aron Loutala" to "MBoté Afro Pulse"
    ),

    // Advanced
    val callerIdAnnouncement: String = "Toujours", // "Toujours", "Uniquement avec casque", "Jamais"
    val flipToSilence: Boolean = true
)
