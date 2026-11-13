package com.pawrise.core.auth.session

import com.pawrise.core.auth.data.RefreshApi
import com.pawrise.core.auth.data.RefreshRequest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/**
 * Un seul refresh à la fois (exigence du contrat) : le refresh token change à chaque appel,
 * et réutiliser l'ancien révoque toute la session côté serveur.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val refreshApi: RefreshApi,
    private val tokenStore: TokenStore,
) {
    private val mutex = Mutex()

    /**
     * Renvoie un access token plus récent que [staleAccessToken], ou `null` si la session est perdue.
     * Si une autre requête a déjà rafraîchi pendant l'attente du verrou, on réutilise son résultat.
     */
    suspend fun refresh(staleAccessToken: String): String? = mutex.withLock {
        val current = tokenStore.tokens.value ?: return null
        if (current.accessToken != staleAccessToken) return current.accessToken

        try {
            val pair = refreshApi.refresh(RefreshRequest(current.refreshToken))
            tokenStore.save(Tokens(pair.accessToken, pair.refreshToken))
            pair.accessToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // 401 INVALID_REFRESH_TOKEN (expiré, révoqué, réutilisé) → déconnexion.
            if (e.code() == 401) tokenStore.clear()
            null
        } catch (e: IOException) {
            // Réseau indisponible : on garde la session, la requête échouera en 401.
            null
        }
    }
}
