package com.pawrise.core.network

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorMessagesTest {

    private fun problem(status: Int, code: String?, vararg fields: Pair<String, String>, retryAfter: Long? = null) =
        ApiError.Problem(status, code, fieldErrors = fields.map { FieldError(it.first, it.second) }, retryAfterSeconds = retryAfter)

    @Test
    fun `message par code de mot de passe`() {
        val cases = mapOf(
            ErrorCodes.PASSWORD_TOO_COMMON to "Ce mot de passe est trop courant. Choisissez-en un plus personnel.",
            ErrorCodes.PASSWORD_BREACHED to "Ce mot de passe apparaît dans une fuite de données connue. Choisissez-en un autre.",
            ErrorCodes.PASSWORD_TOO_LONG to "Le mot de passe doit faire au plus 128 caractères.",
            ErrorCodes.PASSWORD_TOO_SHORT to "Le mot de passe doit faire au moins 8 caractères.",
        )
        cases.forEach { (code, message) ->
            assertEquals(message, ApiErrorMessages.of(problem(400, ErrorCodes.VALIDATION_FAILED, "password" to code)))
        }
    }

    @Test
    fun `telephone et CGU`() {
        assertEquals(
            "Saisissez un numéro valide, par exemple 06 12 34 56 78.",
            ApiErrorMessages.field("phoneNumber", ErrorCodes.INVALID_PHONE),
        )
        assertEquals(
            "Acceptez les conditions d'utilisation pour continuer.",
            ApiErrorMessages.field("acceptedTermsVersion", ErrorCodes.TERMS_NOT_ACCEPTED),
        )
    }

    @Test
    fun `trop d'essais selon Retry-After`() {
        assertEquals(
            "Trop d'essais ratés. Réessayez dans 15 minutes ou réinitialisez votre mot de passe.",
            ApiErrorMessages.of(problem(429, ErrorCodes.TOO_MANY_ATTEMPTS)),
        )
        assertEquals(
            "Trop d'essais ratés. Réessayez dans 2 minutes ou réinitialisez votre mot de passe.",
            ApiErrorMessages.of(problem(429, ErrorCodes.TOO_MANY_ATTEMPTS, retryAfter = 90)),
        )
        assertEquals(
            "Trop d'essais ratés. Réessayez dans 1 minute ou réinitialisez votre mot de passe.",
            ApiErrorMessages.of(problem(429, ErrorCodes.TOO_MANY_ATTEMPTS, retryAfter = 30)),
        )
    }

    @Test
    fun `codes metier et repli`() {
        assertEquals(
            "Code incorrect ou expiré. Vérifiez-le ou demandez-en un nouveau.",
            ApiErrorMessages.of(problem(400, ErrorCodes.INVALID_OR_EXPIRED_TOKEN)),
        )
        assertTrue(ApiErrorMessages.of(problem(409, ErrorCodes.CHIEF_TRANSFER_REQUIRED)).startsWith("Vous êtes le propriétaire principal"))
        assertEquals(ApiErrorMessages.SESSION_EXPIRED, ApiErrorMessages.of(problem(401, null)))
        assertEquals(ApiErrorMessages.GENERIC, ApiErrorMessages.of(problem(500, "SOMETHING_NEW")))
    }

    @Test
    fun `erreurs reseau`() {
        assertEquals(ApiErrorMessages.OFFLINE, ApiErrorMessages.of(ApiError.Network(UnknownHostException())))
        assertEquals(ApiErrorMessages.TIMEOUT, ApiErrorMessages.of(ApiError.Network(SocketTimeoutException())))
        assertEquals(ApiErrorMessages.UNREACHABLE, ApiErrorMessages.of(ApiError.Network(IOException())))
    }

    @Test
    fun `regles de saisie`() {
        assertTrue(CredentialRules.isValidEmail(" Alex@Exemple.fr "))
        assertFalse(CredentialRules.isValidEmail("alex@exemple"))
        assertFalse(CredentialRules.isValidEmail("@exemple.fr"))
        assertTrue(CredentialRules.isValidResetCode("012345"))
        assertFalse(CredentialRules.isValidResetCode("12345a"))
        assertFalse(CredentialRules.isValidPassword("a".repeat(129)))
    }
}
