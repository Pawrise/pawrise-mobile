package com.pawrise.core.auth.data

import kotlinx.serialization.Serializable

// Schémas « Compte » du contrat pawrise-doc (camelCase, convention 3). Voir api/openapi.yaml.

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String? = null,
    val password: String,
    val acceptedTermsVersion: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class AuthTokensDto(val accessToken: String, val refreshToken: String)

@Serializable
data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String? = null,
    val registeredAt: String,
    /** Ajout pawrise-auth (E6) : adresse confirmée par le lien reçu. */
    val emailVerified: Boolean = false,
)

/** `AuthSession` = `AuthTokens` + `user`. */
@Serializable
data class AuthSessionDto(val accessToken: String, val refreshToken: String, val user: UserDto)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)

/** S17 Confidentialité : `GET /v1/me/preferences`. */
@Serializable
data class PreferencesDto(
    val deviceLocationSharing: Boolean,
    val healthNotifications: Boolean,
    val anonymizedDataSharing: Boolean,
)

/** `PATCH /v1/me/preferences` : champ absent = inchangé (convention 9, `explicitNulls = false`). */
@Serializable
data class PreferencesUpdateDto(
    val deviceLocationSharing: Boolean? = null,
    val healthNotifications: Boolean? = null,
    val anonymizedDataSharing: Boolean? = null,
)
