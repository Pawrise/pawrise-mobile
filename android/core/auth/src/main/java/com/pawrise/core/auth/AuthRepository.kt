package com.pawrise.core.auth

import com.pawrise.core.auth.data.AuthApi
import com.pawrise.core.auth.data.AuthSessionDto
import com.pawrise.core.auth.data.LoginRequest
import com.pawrise.core.auth.data.RefreshRequest
import com.pawrise.core.auth.data.RegisterRequest
import com.pawrise.core.auth.data.UserDto
import com.pawrise.core.auth.session.TokenStore
import com.pawrise.core.auth.session.Tokens
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.apiCall
import com.pawrise.core.network.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String?,
)

enum class SessionState { SignedOut, SignedIn }

/** Version des CGU acceptée à l'inscription (RG11, D13). À aligner sur le texte publié. */
const val CURRENT_TERMS_VERSION = "2026-10"

data class RegistrationForm(
    val firstName: String,
    val lastName: String,
    val email: String,
    /** Format E.164 (`+33612345678`) ou `null`. */
    val phoneNumber: String?,
    val password: String,
)

/** Point d'entrée unique des écrans pour tout ce qui touche au compte et à la session. */
interface AuthRepository {
    /** Suit la session : passe à [SessionState.SignedOut] si le refresh échoue. */
    val session: Flow<SessionState>

    /** Charge la session persistée. À appeler une fois au démarrage. */
    suspend fun restoreSession(): SessionState

    /** Crée le compte et ouvre la session (l'app enchaîne sur la découverte). */
    suspend fun register(form: RegistrationForm): ApiResult<User>

    suspend fun login(email: String, password: String): ApiResult<User>

    /** Ferme la session localement même si l'appel serveur échoue. */
    suspend fun logout()

    suspend fun me(): ApiResult<User>
}

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : AuthRepository {

    override val session: Flow<SessionState> =
        tokenStore.tokens.map { if (it == null) SessionState.SignedOut else SessionState.SignedIn }

    override suspend fun restoreSession(): SessionState {
        tokenStore.load()
        return if (tokenStore.tokens.value == null) SessionState.SignedOut else SessionState.SignedIn
    }

    override suspend fun register(form: RegistrationForm): ApiResult<User> = openSession {
        api.register(
            RegisterRequest(
                firstName = form.firstName.trim(),
                lastName = form.lastName.trim(),
                email = form.email.trim(),
                phoneNumber = form.phoneNumber,
                password = form.password,
                acceptedTermsVersion = CURRENT_TERMS_VERSION,
            ),
        )
    }

    override suspend fun login(email: String, password: String): ApiResult<User> =
        openSession { api.login(LoginRequest(email.trim(), password)) }

    override suspend fun logout() {
        tokenStore.tokens.value?.let { tokens -> apiCall(json) { api.logout(RefreshRequest(tokens.refreshToken)) } }
        tokenStore.clear()
    }

    override suspend fun me(): ApiResult<User> = apiCall(json) { api.me().toDomain() }

    private suspend fun openSession(call: suspend () -> AuthSessionDto): ApiResult<User> =
        apiCall(json) { call() }.also { result ->
            if (result is ApiResult.Success) tokenStore.save(Tokens(result.value.accessToken, result.value.refreshToken))
        }.map { it.user.toDomain() }
}

private fun UserDto.toDomain() = User(id, firstName, lastName, email, phoneNumber)
