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
)

/** `AuthSession` = `AuthTokens` + `user`. */
@Serializable
data class AuthSessionDto(val accessToken: String, val refreshToken: String, val user: UserDto)
