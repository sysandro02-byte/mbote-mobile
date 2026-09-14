package com.loukatech.mbote.service.api

/**
 * Compatibility façade kept for older UI call sites.
 *
 * Transactional e-mail credentials must never be embedded in an APK. Password
 * recovery is performed by MboteApiService through the authenticated backend.
 */
@Deprecated(
    message = "Use MboteApiService.requestForgotPassword; e-mail is sent by the backend",
    level = DeprecationLevel.WARNING
)
object BrevoEmailService {
    suspend fun sendPasswordRecoveryEmail(
        recipientEmail: String,
        recoveryCode: String
    ): Result<Boolean> = Result.failure(
        UnsupportedOperationException(
            "L’envoi direct depuis l’application est désactivé. Utilisez la récupération sécurisée côté serveur."
        )
    )
}
