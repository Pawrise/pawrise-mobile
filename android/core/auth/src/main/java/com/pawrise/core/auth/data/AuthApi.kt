package com.pawrise.core.auth.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * Routes de pawrise-auth (`/v1/auth/…`, `/v1/me…`), relatives à `pawrise.authBaseUrl` (qui finit par `/v1/`).
 * Client principal : Bearer ajouté hors routes publiques, refresh sur 401.
 */
interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthSessionDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthSessionDto

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)

    /** Envoie un code à 6 chiffres par e-mail ; réponse identique si le compte n'existe pas. */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest)

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest)

    @POST("auth/resend-verification")
    suspend fun resendVerification()

    @GET("me")
    suspend fun me(): UserDto

    @GET("me/preferences")
    suspend fun preferences(): PreferencesDto

    @PATCH("me/preferences")
    suspend fun updatePreferences(@Body body: PreferencesUpdateDto): PreferencesDto

    /** Suppression définitive du compte (S25) ; les sessions sont fermées côté serveur. */
    @DELETE("me")
    suspend fun deleteAccount()
}

/** Refresh appelé avec le client nu : il ne doit jamais repasser par l'Authenticator. */
interface RefreshApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthTokensDto
}
