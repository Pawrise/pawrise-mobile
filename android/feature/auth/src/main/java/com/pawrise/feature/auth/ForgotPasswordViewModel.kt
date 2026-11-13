package com.pawrise.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.network.ApiErrorMessages
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.CredentialRules
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val codeSent: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val signedIn: Boolean = false,
)

/**
 * Mot de passe oublié en deux étapes (port d'iOS `ForgotPasswordView`) : l'e-mail
 * (`POST /v1/auth/forgot-password`), puis le code à 6 chiffres et le nouveau mot de passe
 * (`POST /v1/auth/reset-password`), suivis d'une connexion directe.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()
    private var prefilled = false

    /** Reprend l'e-mail saisi sur Connexion (une seule fois). */
    fun prefill(email: String) {
        if (prefilled) return
        prefilled = true
        if (_state.value.email.isEmpty()) _state.update { it.copy(email = email.trim()) }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    /** Chiffres seulement, 6 au plus. */
    fun onCodeChange(value: String) =
        _state.update { it.copy(code = value.filter { c -> c in '0'..'9' }.take(CredentialRules.RESET_CODE_LENGTH), error = null) }

    fun onNewPasswordChange(value: String) = _state.update { it.copy(newPassword = value, error = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val invalid = when {
            !s.codeSent && !CredentialRules.isValidEmail(s.email) -> ForgotMessages.EMAIL
            s.codeSent && !CredentialRules.isValidResetCode(s.code) -> ForgotMessages.CODE
            s.codeSent && !CredentialRules.isValidPassword(s.newPassword) -> ForgotMessages.PASSWORD
            else -> null
        }
        if (invalid != null) return _state.update { it.copy(error = invalid) }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (s.codeSent) {
                when (val result = authRepository.resetPassword(s.email, s.code, s.newPassword)) {
                    is ApiResult.Success -> _state.update { it.copy(loading = false, signedIn = true) }
                    is ApiResult.Failure -> _state.update { it.copy(loading = false, error = ApiErrorMessages.of(result.error)) }
                }
            } else {
                when (val result = authRepository.requestPasswordReset(s.email)) {
                    is ApiResult.Success -> _state.update { it.copy(loading = false, codeSent = true, info = ForgotMessages.SENT) }
                    is ApiResult.Failure -> _state.update { it.copy(loading = false, error = ApiErrorMessages.of(result.error)) }
                }
            }
        }
    }

    /** « Renvoyer un code » : redemande un code pour la même adresse. */
    fun resend() {
        if (_state.value.loading) return
        _state.update { it.copy(codeSent = false, code = "", info = null, error = null) }
        submit()
    }
}

internal object ForgotMessages {
    const val EMAIL = "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
    const val CODE = "Le code contient 6 chiffres."
    val PASSWORD = "Le mot de passe doit faire au moins ${CredentialRules.PASSWORD_MIN_LENGTH} caractères."
    const val SENT = "Si un compte existe avec cette adresse, un code vient de lui être envoyé. Il est valable 15 minutes."
}
