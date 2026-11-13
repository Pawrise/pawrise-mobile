package com.pawrise.core.testing

import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.Preferences
import com.pawrise.core.auth.PreferencesUpdate
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
import kotlinx.coroutines.flow.StateFlow

/**
 * Faux [AuthRepository] scriptable : chaque test choisit la réponse du « serveur ».
 * [gate] permet de garder un appel en cours (état de chargement, double clic).
 */
class FakeAuthRepository(
    initial: SessionState = SessionState.SignedOut,
    /** Compte renvoyé par connexion, inscription et `me()`. */
    var account: User = USER,
) : AuthRepository {

    private val state = MutableStateFlow(initial)
    override val session: Flow<SessionState> = state

    private val currentUser = MutableStateFlow(if (initial == SessionState.SignedIn) account else null)
    override val user: StateFlow<User?> = currentUser

    var loginResult: ApiResult<User>? = null
    var registerResult: ApiResult<User>? = null
    var meResult: ApiResult<User>? = null
    var forgotResult: ApiResult<Unit> = ApiResult.Success(Unit)
    var resetResult: ApiResult<Unit> = ApiResult.Success(Unit)
    var resendResult: ApiResult<Unit> = ApiResult.Success(Unit)
    var preferencesResult: ApiResult<Preferences>? = null
    var updatePreferencesResult: ApiResult<Preferences>? = null
    var deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)
    var gate: CompletableDeferred<Unit>? = null

    /** Préférences « côté serveur ». */
    var serverPreferences: Preferences = Preferences.Defaults

    val loginCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<RegistrationForm>()
    val forgotCalls = mutableListOf<String>()
    val resetCalls = mutableListOf<Triple<String, String, String>>()
    val preferenceUpdates = mutableListOf<PreferencesUpdate>()
    var resendCalls = 0
        private set
    var deleteCalls = 0
        private set
    var logoutCalls = 0
        private set

    override suspend fun restoreSession(): SessionState = state.value

    override suspend fun register(form: RegistrationForm): ApiResult<User> {
        registerCalls += form
        gate?.await()
        return (registerResult ?: ApiResult.Success(account)).also(::openIfSuccess)
    }

    override suspend fun login(email: String, password: String): ApiResult<User> {
        loginCalls += email to password
        gate?.await()
        return (loginResult ?: ApiResult.Success(account)).also(::openIfSuccess)
    }

    override suspend fun logout() {
        logoutCalls++
        close()
    }

    override suspend fun me(): ApiResult<User> =
        (meResult ?: ApiResult.Success(account)).also { if (it is ApiResult.Success) currentUser.value = it.value }

    override suspend fun requestPasswordReset(email: String): ApiResult<Unit> {
        forgotCalls += email
        gate?.await()
        return forgotResult
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<User> {
        resetCalls += Triple(email, code, newPassword)
        gate?.await()
        return when (val reset = resetResult) {
            is ApiResult.Failure -> reset
            is ApiResult.Success -> login(email, newPassword)
        }
    }

    override suspend fun resendVerificationEmail(): ApiResult<Unit> {
        resendCalls++
        gate?.await()
        return resendResult
    }

    override suspend fun preferences(): ApiResult<Preferences> = preferencesResult ?: ApiResult.Success(serverPreferences)

    override suspend fun updatePreferences(update: PreferencesUpdate): ApiResult<Preferences> {
        preferenceUpdates += update
        gate?.await()
        updatePreferencesResult?.let { return it }
        serverPreferences = serverPreferences.copy(
            deviceLocationSharing = update.deviceLocationSharing ?: serverPreferences.deviceLocationSharing,
            healthNotifications = update.healthNotifications ?: serverPreferences.healthNotifications,
            anonymizedDataSharing = update.anonymizedDataSharing ?: serverPreferences.anonymizedDataSharing,
        )
        return ApiResult.Success(serverPreferences)
    }

    override suspend fun deleteAccount(): ApiResult<Unit> {
        deleteCalls++
        gate?.await()
        return deleteResult.also { if (it is ApiResult.Success) close() }
    }

    /** Simule une session perdue (refresh refusé). */
    fun expireSession() = close()

    private fun openIfSuccess(result: ApiResult<User>) {
        if (result is ApiResult.Success) {
            currentUser.value = result.value
            state.value = SessionState.SignedIn
        }
    }

    private fun close() {
        currentUser.value = null
        state.value = SessionState.SignedOut
    }

    companion object {
        val USER = User("u1", "Camille", "Martin", "camille.martin@example.com", null, emailVerified = true)

        fun problem(status: Int, code: String, vararg fields: Pair<String, String>, retryAfter: Long? = null) =
            ApiResult.Failure(
                ApiError.Problem(status, code, fieldErrors = fields.map { FieldError(it.first, it.second) }, retryAfterSeconds = retryAfter),
            )

        val NETWORK_DOWN = ApiResult.Failure(ApiError.Network(IOException("offline")))
    }
}
