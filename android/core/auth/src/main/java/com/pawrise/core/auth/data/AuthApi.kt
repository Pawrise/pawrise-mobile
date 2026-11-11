package com.pawrise.core.auth.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Routes « Compte » appelées avec le client principal (Bearer ajouté hors routes publiques). */
interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthSessionDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthSessionDto

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)

    @GET("me")
    suspend fun me(): UserDto
}

/** Refresh appelé avec le client nu : il ne doit jamais repasser par l'Authenticator. */
interface RefreshApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthTokensDto
}
