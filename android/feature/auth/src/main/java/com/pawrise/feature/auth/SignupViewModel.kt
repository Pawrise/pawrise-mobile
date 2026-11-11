package com.pawrise.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.RegistrationForm
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.ErrorCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Champs du formulaire, nommés comme dans le contrat (`errors[].field`). */
enum class SignupField(val apiName: String) {
    FirstName("firstName"),
    LastName("lastName"),
    Email("email"),
    Phone("phoneNumber"),
    Password("password"),
    Terms("acceptedTermsVersion"),
}

data class SignupUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val termsAccepted: Boolean = false,
    val fieldErrors: Map<SignupField, String> = emptyMap(),
    val formError: String? = null,
    val loading: Boolean = false,
    val registered: Boolean = false,
)

/** US-A · 02 Créer un compte (`POST /auth/register`). Modifs de maquette M1–M3 appliquées. */
@HiltViewModel
class SignupViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(SignupUiState())
    val state: StateFlow<SignupUiState> = _state.asStateFlow()

    fun onFirstNameChange(value: String) = edit(SignupField.FirstName) { copy(firstName = value) }
    fun onLastNameChange(value: String) = edit(SignupField.LastName) { copy(lastName = value) }
    fun onEmailChange(value: String) = edit(SignupField.Email) { copy(email = value) }
    fun onPhoneChange(value: String) = edit(SignupField.Phone) { copy(phone = value) }
    fun onPasswordChange(value: String) = edit(SignupField.Password) { copy(password = value) }
    fun onTermsChange(value: Boolean) = edit(SignupField.Terms) { copy(termsAccepted = value) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val phone = s.phone.trim().takeIf { it.isNotEmpty() }?.let { toE164(it) }
        val errors = buildMap {
            if (s.firstName.isBlank()) put(SignupField.FirstName, SignupMessages.FIRST_NAME)
            if (s.lastName.isBlank()) put(SignupField.LastName, SignupMessages.LAST_NAME)
            if (!isValidEmail(s.email)) put(SignupField.Email, AuthMessages.INVALID_EMAIL)
            if (s.phone.isNotBlank() && phone == null) put(SignupField.Phone, SignupMessages.PHONE)
            if (s.password.length < MIN_PASSWORD) put(SignupField.Password, SignupMessages.PASSWORD)
            if (!s.termsAccepted) put(SignupField.Terms, SignupMessages.TERMS)
        }
        if (errors.isNotEmpty()) return _state.update { it.copy(fieldErrors = errors, formError = null) }

        _state.update { it.copy(loading = true, fieldErrors = emptyMap(), formError = null) }
        viewModelScope.launch {
            val form = RegistrationForm(s.firstName, s.lastName, s.email, phone, s.password)
            when (val result = authRepository.register(form)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, registered = true) }
                is ApiResult.Failure -> _state.update { it.withServerError(result.error) }
            }
        }
    }

    private fun edit(field: SignupField, change: SignupUiState.() -> SignupUiState) =
        _state.update { it.change().copy(fieldErrors = it.fieldErrors - field, formError = null) }

    private fun SignupUiState.withServerError(error: ApiError): SignupUiState {
        val fields = if (error is ApiError.Problem) error.toFieldErrors() else emptyMap()
        return copy(
            loading = false,
            fieldErrors = fields,
            formError = if (fields.isEmpty()) AuthMessages.of(error) else null,
        )
    }

    companion object {
        const val MIN_PASSWORD = 8
    }
}

internal object SignupMessages {
    const val FIRST_NAME = "Indiquez votre prénom."
    const val LAST_NAME = "Indiquez votre nom."
    const val PHONE = "Indiquez un numéro de téléphone valide."
    const val PASSWORD = "Choisissez un mot de passe d'au moins 8 caractères."
    const val TERMS = "Acceptez les conditions pour créer votre compte."
    const val EMAIL_USED = "Un compte existe déjà avec cet e-mail."
}

/** Erreurs serveur → message sous le bon champ (convention 4 : `errors[].field`). */
private fun ApiError.Problem.toFieldErrors(): Map<SignupField, String> {
    if (code == ErrorCodes.EMAIL_ALREADY_USED) return mapOf(SignupField.Email to SignupMessages.EMAIL_USED)
    if (code != ErrorCodes.VALIDATION_FAILED) return emptyMap()
    return fieldErrors.mapNotNull { error ->
        val field = SignupField.entries.firstOrNull { it.apiName == error.field } ?: return@mapNotNull null
        field to when (field) {
            SignupField.FirstName -> SignupMessages.FIRST_NAME
            SignupField.LastName -> SignupMessages.LAST_NAME
            SignupField.Email -> AuthMessages.INVALID_EMAIL
            SignupField.Phone -> SignupMessages.PHONE
            SignupField.Password -> SignupMessages.PASSWORD
            SignupField.Terms -> SignupMessages.TERMS
        }
    }.toMap()
}
