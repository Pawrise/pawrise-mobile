package com.pawrise.feature.auth

import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ErrorCodes
import kotlin.math.ceil

/** Textes affichés par l'app (convention 4 : l'API donne un `code`, l'app écrit le message). */
internal object AuthMessages {
    const val FILL_LOGIN = "Renseignez votre e-mail et votre mot de passe."
    const val INVALID_EMAIL = "Indiquez un e-mail valide."
    const val INVALID_CREDENTIALS = "E-mail ou mot de passe incorrect."
    const val TOO_MANY_ATTEMPTS_LATER = "Trop d'essais. Réessayez plus tard."
    const val NETWORK = "Connexion impossible. Vérifiez votre réseau et réessayez."
    const val GENERIC = "Une erreur est survenue. Réessayez."

    fun tooManyAttempts(retryAfterSeconds: Long?): String {
        if (retryAfterSeconds == null || retryAfterSeconds <= 0) return TOO_MANY_ATTEMPTS_LATER
        val minutes = ceil(retryAfterSeconds / 60.0).toInt()
        return "Trop d'essais. Réessayez dans $minutes min."
    }

    /** Message générique d'un échec qui n'est pas propre à un champ. */
    fun of(error: ApiError): String = when (error) {
        is ApiError.Network -> NETWORK
        is ApiError.Unexpected -> GENERIC
        is ApiError.Problem -> when (error.code) {
            ErrorCodes.INVALID_CREDENTIALS -> INVALID_CREDENTIALS
            ErrorCodes.TOO_MANY_ATTEMPTS -> tooManyAttempts(error.retryAfterSeconds)
            else -> GENERIC
        }
    }
}

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Même règle que la maquette (`validEmail`). Le serveur reste juge (INVALID_EMAIL). */
internal fun isValidEmail(email: String) = EMAIL.matches(email.trim())
