package com.pawrise.core.testing

import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.RegistrationForm
import com.pawrise.core.auth.SessionState
import com.pawrise.core.auth.User
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.FieldError
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Faux [AuthRepository] scriptable : chaque test choisit la réponse du « serveur ».
 * [gate] permet de garder un appel en cours (état de chargement, double clic).
 */
class FakeAuthRepository(initial: SessionState = SessionState.SignedOut) : AuthRepository {

    private val state = MutableStateFlow(initial)
    override val session: Flow<SessionState> = state

    var loginResult: ApiResult<User> = ApiResult.Success(USER)
    var registerResult: ApiResult<User> = ApiResult.Success(USER)
    var gate: CompletableDeferred<Unit>? = null

    val loginCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<RegistrationForm>()

    override suspend fun restoreSession(): SessionState = state.value

    override suspend fun register(form: RegistrationForm): ApiResult<User> {
        registerCalls += form
        gate?.await()
        return registerResult.also { if (it is ApiResult.Success) state.value = SessionState.SignedIn }
    }

    override suspend fun login(email: String, password: String): ApiResult<User> {
        loginCalls += email to password
        gate?.await()
        return loginResult.also { if (it is ApiResult.Success) state.value = SessionState.SignedIn }
    }

    override suspend fun logout() {
        state.value = SessionState.SignedOut
    }

    override suspend fun me(): ApiResult<User> = ApiResult.Success(USER)

    /** Simule une session perdue (refresh refusé). */
    fun expireSession() {
        state.value = SessionState.SignedOut
    }

    companion object {
        val USER = User("u1", "Camille", "Martin", "camille.martin@example.com", null)

        fun problem(status: Int, code: String, vararg fields: Pair<String, String>, retryAfter: Long? = null) =
            ApiResult.Failure(
                ApiError.Problem(status, code, fieldErrors = fields.map { FieldError(it.first, it.second) }, retryAfterSeconds = retryAfter),
            )

        val NETWORK_DOWN = ApiResult.Failure(ApiError.Network(IOException("offline")))
    }
}
