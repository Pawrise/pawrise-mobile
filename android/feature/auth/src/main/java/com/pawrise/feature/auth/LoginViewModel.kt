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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
) {
    /** Comme iOS : bouton actif dès que les deux champs sont remplis. */
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !loading
}

/** US-J · 02 Connexion (`POST /v1/auth/login`). */
@HiltViewModel
class LoginViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.login(CredentialRules.normalizedEmail(current.email), current.password)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, loggedIn = true) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = ApiErrorMessages.of(result.error)) }
            }
        }
    }
}
