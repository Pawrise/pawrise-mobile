package com.pawrise.core.network

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.ceil

/** Règles de saisie alignées sur le contrat (le serveur reste la référence). */
object CredentialRules {
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 128
    const val RESET_CODE_LENGTH = 6

    fun normalizedEmail(raw: String): String = raw.trim().lowercase()

    fun isValidEmail(raw: String): Boolean {
        val email = normalizedEmail(raw)
        val at = email.indexOf('@')
        if (email.length > 254 || at <= 0 || email.contains(' ')) return false
        val domain = email.substring(at + 1)
        return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.') && !domain.contains('@')
    }

    fun isValidPassword(password: String): Boolean = password.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH

    fun isValidResetCode(code: String): Boolean = code.length == RESET_CODE_LENGTH && code.all { it in '0'..'9' }
}

/**
 * Messages affichés par l'app (convention 4 : l'API donne un `code`, l'app écrit le texte).
 * Même textes que l'app iOS (`Core/Network/APIError.swift`).
 */
object ApiErrorMessages {
    const val GENERIC = "Une erreur est survenue. Réessayez dans un instant."
    const val SESSION_EXPIRED = "Votre session a expiré. Reconnectez-vous."
    const val OFFLINE = "Pas de connexion internet. Vérifiez votre réseau puis réessayez."
    const val TIMEOUT = "Le serveur met trop de temps à répondre. Réessayez dans un instant."
    const val UNREACHABLE = "Impossible de joindre le serveur. Réessayez dans un instant."
    const val UNEXPECTED = "Réponse inattendue du serveur. Réessayez dans un instant."

    /** Message d'un échec quelconque : par champ s'il y en a un, sinon par code. */
    fun of(error: ApiError): String = when (error) {
        is ApiError.Network -> when (error.cause) {
            is UnknownHostException, is ConnectException -> OFFLINE
            is SocketTimeoutException -> TIMEOUT
            else -> UNREACHABLE
        }
        is ApiError.Unexpected -> UNEXPECTED
        is ApiError.Problem -> problem(error)
    }

    private fun problem(error: ApiError.Problem): String {
        if (error.code == ErrorCodes.VALIDATION_FAILED) {
            error.fieldErrors.firstOrNull()?.let { first -> field(first.field, first.code)?.let { return it } }
        }
        if (error.code == ErrorCodes.TOO_MANY_ATTEMPTS) return tooManyAttempts(error.retryAfterSeconds)
        return message(error.code) ?: if (error.status == 401) SESSION_EXPIRED else GENERIC
    }

    /** 429 : délai donné par `Retry-After` (15 minutes par défaut, durée du blocage côté serveur). */
    fun tooManyAttempts(retryAfterSeconds: Long?): String {
        val minutes = retryAfterSeconds?.takeIf { it > 0 }?.let { ceil(it / 60.0).toInt() } ?: 15
        val delay = if (minutes <= 1) "1 minute" else "$minutes minutes"
        return "Trop d'essais ratés. Réessayez dans $delay ou réinitialisez votre mot de passe."
    }

    /** Codes d'erreur du contrat (openapi.yaml) et de pawrise-auth (ECARTS.md). */
    fun message(code: String?): String? = when (code) {
        ErrorCodes.EMAIL_ALREADY_USED -> "Un compte existe déjà avec cette adresse. Connectez-vous plutôt."
        ErrorCodes.INVALID_CREDENTIALS -> "E-mail ou mot de passe incorrect."
        ErrorCodes.TOO_MANY_ATTEMPTS -> tooManyAttempts(null)
        ErrorCodes.INVALID_REFRESH_TOKEN, ErrorCodes.UNAUTHORIZED -> SESSION_EXPIRED
        ErrorCodes.INVALID_OR_EXPIRED_TOKEN -> "Code incorrect ou expiré. Vérifiez-le ou demandez-en un nouveau."
        ErrorCodes.CHIEF_TRANSFER_REQUIRED ->
            "Vous êtes le propriétaire principal d'un chien partagé. Transférez d'abord ce rôle à un autre propriétaire, puis supprimez votre compte."
        ErrorCodes.VALIDATION_FAILED -> "Certains champs sont invalides. Vérifiez le formulaire."
        else -> null
    }

    /** Codes par champ de `VALIDATION_FAILED` (`errors[].code`). */
    fun field(field: String, code: String): String? = when (code) {
        ErrorCodes.REQUIRED -> when (field) {
            "firstName" -> "Indiquez votre prénom."
            "lastName" -> "Indiquez votre nom."
            "email" -> "Indiquez votre adresse e-mail."
            else -> "Ce champ est obligatoire."
        }
        ErrorCodes.INVALID_EMAIL -> "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
        ErrorCodes.INVALID_PHONE -> "Saisissez un numéro valide, par exemple 06 12 34 56 78."
        ErrorCodes.PASSWORD_TOO_SHORT -> "Le mot de passe doit faire au moins ${CredentialRules.PASSWORD_MIN_LENGTH} caractères."
        ErrorCodes.PASSWORD_TOO_LONG -> "Le mot de passe doit faire au plus ${CredentialRules.PASSWORD_MAX_LENGTH} caractères."
        ErrorCodes.PASSWORD_TOO_COMMON -> "Ce mot de passe est trop courant. Choisissez-en un plus personnel."
        ErrorCodes.PASSWORD_BREACHED -> "Ce mot de passe apparaît dans une fuite de données connue. Choisissez-en un autre."
        ErrorCodes.TERMS_NOT_ACCEPTED -> "Acceptez les conditions d'utilisation pour continuer."
        ErrorCodes.INVALID_CODE -> "Le code contient 6 chiffres."
        ErrorCodes.TOO_LONG -> "Ce champ est trop long."
        else -> null
    }
}
