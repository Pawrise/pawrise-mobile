package com.pawrise.core.auth.session

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val AUTHORIZATION = "Authorization"

/**
 * Routes publiques du contrat (convention 6) : jamais de Bearer. Indispensable pour `login` :
 * un 401 INVALID_CREDENTIALS ne doit pas déclencher de refresh.
 */
private val PUBLIC_PATHS = listOf(
    "/auth/register",
    "/auth/login",
    "/auth/refresh",
    "/auth/forgot-password",
    "/auth/reset-password",
)

private fun Request.isPublic(): Boolean = PUBLIC_PATHS.any { url.encodedPath.endsWith(it) }

/** Ajoute `Authorization: Bearer …` aux routes protégées quand une session est ouverte. */
class BearerInterceptor @Inject constructor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenStore.tokens.value?.accessToken
        if (accessToken == null || request.header(AUTHORIZATION) != null || request.isPublic()) {
            return chain.proceed(request)
        }
        return chain.proceed(request.withBearer(accessToken))
    }
}

/** Sur 401 d'une requête authentifiée : refresh (unique) puis rejoue la requête une seule fois. */
class TokenAuthenticator @Inject constructor(private val refresher: TokenRefresher) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header(AUTHORIZATION)?.removePrefix("Bearer ") ?: return null
        if (response.priorResponse != null) return null // déjà rejouée une fois
        val fresh = runBlocking { refresher.refresh(sent) } ?: return null
        return response.request.withBearer(fresh)
    }
}

private fun Request.withBearer(token: String): Request =
    newBuilder().header(AUTHORIZATION, "Bearer $token").build()
