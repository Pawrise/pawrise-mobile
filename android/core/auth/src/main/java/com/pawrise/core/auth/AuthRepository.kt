package com.pawrise.core.auth

import com.pawrise.core.auth.data.AuthApi
import com.pawrise.core.auth.data.AuthSessionDto
import com.pawrise.core.auth.data.ForgotPasswordRequest
import com.pawrise.core.auth.data.LoginRequest
import com.pawrise.core.auth.data.PreferencesDto
import com.pawrise.core.auth.data.PreferencesUpdateDto
import com.pawrise.core.auth.data.RefreshRequest
import com.pawrise.core.auth.data.RegisterRequest
import com.pawrise.core.auth.data.ResetPasswordRequest
import com.pawrise.core.auth.data.UserDto
import com.pawrise.core.auth.session.TokenStore
import com.pawrise.core.auth.session.Tokens
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.CredentialRules
import com.pawrise.core.network.apiCall
import com.pawrise.core.network.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String?,
    /** Adresse confirmée par le lien reçu par e-mail (ajout pawrise-auth E6). */
    val emailVerified: Boolean = false,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

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

/** S17 Confidentialité : les trois interrupteurs de `GET/PATCH /v1/me/preferences` (D6). */
data class Preferences(
    val deviceLocationSharing: Boolean,
    val healthNotifications: Boolean,
    val anonymizedDataSharing: Boolean,
) {
    companion object {
        /** Valeurs d'un compte neuf côté serveur. */
        val Defaults = Preferences(deviceLocationSharing = true, healthNotifications = true, anonymizedDataSharing = false)
    }
}

/** PATCH : seuls les champs non `null` sont envoyés (absent = inchangé). */
data class PreferencesUpdate(
    val deviceLocationSharing: Boolean? = null,
    val healthNotifications: Boolean? = null,
    val anonymizedDataSharing: Boolean? = null,
)

/** Point d'entrée unique des écrans pour tout ce qui touche au compte et à la session (pawrise-auth). */
interface AuthRepository {
    /** Suit la session : passe à [SessionState.SignedOut] si le refresh échoue. */
    val session: Flow<SessionState>

    /** Dernier compte connu (connexion, inscription, `GET /me`) ; `null` hors session ou pas encore chargé. */
    val user: StateFlow<User?>

    /** Charge la session persistée. À appeler une fois au démarrage. */
    suspend fun restoreSession(): SessionState

    /** Crée le compte et ouvre la session (l'app enchaîne sur la découverte). */
    suspend fun register(form: RegistrationForm): ApiResult<User>

    suspend fun login(email: String, password: String): ApiResult<User>

    /** Ferme la session localement même si l'appel serveur échoue. */
    suspend fun logout()

    /** Recharge le compte (et [user]). */
    suspend fun me(): ApiResult<User>

    /** Mot de passe oublié : le serveur envoie un code à 6 chiffres (réponse identique si le compte n'existe pas). */
    suspend fun requestPasswordReset(email: String): ApiResult<Unit>

    /** Nouveau mot de passe avec le code reçu, puis connexion directe. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<User>

    suspend fun resendVerificationEmail(): ApiResult<Unit>

    suspend fun preferences(): ApiResult<Preferences>

    suspend fun updatePreferences(update: PreferencesUpdate): ApiResult<Preferences>

    /** Supprime le compte sur le serveur puis ferme la session locale. En cas d'erreur, rien n'est effacé. */
    suspend fun deleteAccount(): ApiResult<Unit>
}

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : AuthRepository {

    override val session: Flow<SessionState> =
        tokenStore.tokens.map { if (it == null) SessionState.SignedOut else SessionState.SignedIn }

    private val currentUser = MutableStateFlow<User?>(null)
    override val user: StateFlow<User?> = currentUser.asStateFlow()

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
        clearLocalSession()
    }

    override suspend fun me(): ApiResult<User> =
        apiCall(json) { api.me().toDomain() }.also { if (it is ApiResult.Success) currentUser.value = it.value }

    override suspend fun requestPasswordReset(email: String): ApiResult<Unit> =
        apiCall(json) { api.forgotPassword(ForgotPasswordRequest(CredentialRules.normalizedEmail(email))) }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<User> {
        val normalized = CredentialRules.normalizedEmail(email)
        return when (val reset = apiCall(json) { api.resetPassword(ResetPasswordRequest(normalized, code, newPassword)) }) {
            is ApiResult.Failure -> reset
            is ApiResult.Success -> login(normalized, newPassword)
        }
    }

    override suspend fun resendVerificationEmail(): ApiResult<Unit> = apiCall(json) { api.resendVerification() }

    override suspend fun preferences(): ApiResult<Preferences> = apiCall(json) { api.preferences().toDomain() }

    override suspend fun updatePreferences(update: PreferencesUpdate): ApiResult<Preferences> = apiCall(json) {
        api.updatePreferences(
            PreferencesUpdateDto(update.deviceLocationSharing, update.healthNotifications, update.anonymizedDataSharing),
        ).toDomain()
    }

    override suspend fun deleteAccount(): ApiResult<Unit> =
        apiCall(json) { api.deleteAccount() }.also { if (it is ApiResult.Success) clearLocalSession() }

    private suspend fun clearLocalSession() {
        tokenStore.clear()
        currentUser.value = null
    }

    private suspend fun openSession(call: suspend () -> AuthSessionDto): ApiResult<User> =
        apiCall(json) { call() }.also { result ->
            if (result is ApiResult.Success) tokenStore.save(Tokens(result.value.accessToken, result.value.refreshToken))
        }.map { it.user.toDomain() }.also { if (it is ApiResult.Success) currentUser.value = it.value }
}

private fun UserDto.toDomain() = User(id, firstName, lastName, email, phoneNumber, emailVerified)

private fun PreferencesDto.toDomain() = Preferences(deviceLocationSharing, healthNotifications, anonymizedDataSharing)
