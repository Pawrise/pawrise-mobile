package com.pawrise.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.RegistrationForm
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiErrorMessages
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.CredentialRules
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
    /** Après un premier envoi : les erreurs locales s'affichent (et se mettent à jour à la saisie). */
    val showErrors: Boolean = false,
    /** Erreurs par champ renvoyées par le serveur (VALIDATION_FAILED), affichées sous le champ. */
    val serverErrors: Map<SignupField, String> = emptyMap(),
    val formError: String? = null,
    val loading: Boolean = false,
    val registered: Boolean = false,
) {
    /** Contrôles locaux (mêmes textes qu'iOS). */
    val localErrors: Map<SignupField, String>
        get() = buildMap {
            if (firstName.isBlank()) put(SignupField.FirstName, SignupMessages.FIRST_NAME)
            if (lastName.isBlank()) put(SignupField.LastName, SignupMessages.LAST_NAME)
            if (!CredentialRules.isValidEmail(email)) put(SignupField.Email, SignupMessages.EMAIL)
            if (phone.isNotBlank() && toE164(phone.trim()) == null) put(SignupField.Phone, SignupMessages.PHONE)
            when {
                password.length < CredentialRules.PASSWORD_MIN_LENGTH -> put(SignupField.Password, SignupMessages.PASSWORD_SHORT)
                password.length > CredentialRules.PASSWORD_MAX_LENGTH -> put(SignupField.Password, SignupMessages.PASSWORD_LONG)
            }
            if (!termsAccepted) put(SignupField.Terms, SignupMessages.TERMS)
        }

    /** Erreur affichée sous chaque champ : locale après un envoi, sinon celle du serveur. */
    val fieldErrors: Map<SignupField, String>
        get() = if (showErrors) serverErrors + localErrors else serverErrors
}

/** US-A · 02 Créer un compte (`POST /v1/auth/register`). */
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
        _state.update { it.copy(showErrors = true, serverErrors = emptyMap(), formError = null) }
        if (s.localErrors.isNotEmpty()) return

        _state.update { it.copy(loading = true) }
        val phone = s.phone.trim().takeIf { it.isNotEmpty() }?.let { toE164(it) }
        viewModelScope.launch {
            val form = RegistrationForm(s.firstName, s.lastName, CredentialRules.normalizedEmail(s.email), phone, s.password)
            when (val result = authRepository.register(form)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, registered = true) }
                is ApiResult.Failure -> _state.update { it.withServerError(result.error) }
            }
        }
    }

    private fun edit(field: SignupField, change: SignupUiState.() -> SignupUiState) =
        _state.update { it.change().copy(serverErrors = it.serverErrors - field, formError = null) }

    private fun SignupUiState.withServerError(error: ApiError): SignupUiState {
        val fields = (error as? ApiError.Problem)?.toFieldErrors().orEmpty()
        return copy(
            loading = false,
            serverErrors = fields,
            formError = if (fields.isEmpty()) ApiErrorMessages.of(error) else null,
        )
    }
}

internal object SignupMessages {
    const val FIRST_NAME = "Indiquez votre prénom."
    const val LAST_NAME = "Indiquez votre nom."
    const val EMAIL = "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
    const val PHONE = "Saisissez un numéro valide, par exemple 06 12 34 56 78."
    val PASSWORD_SHORT = "Le mot de passe doit faire au moins ${CredentialRules.PASSWORD_MIN_LENGTH} caractères."
    val PASSWORD_LONG = "Le mot de passe doit faire au plus ${CredentialRules.PASSWORD_MAX_LENGTH} caractères."
    const val TERMS = "Acceptez les conditions d’utilisation pour continuer."
}

/** `VALIDATION_FAILED` → message sous le bon champ, selon le code (PASSWORD_TOO_COMMON, INVALID_PHONE…). */
private fun ApiError.Problem.toFieldErrors(): Map<SignupField, String> {
    if (code != ErrorCodes.VALIDATION_FAILED) return emptyMap()
    return fieldErrors.mapNotNull { error ->
        val field = SignupField.entries.firstOrNull { it.apiName == error.field } ?: return@mapNotNull null
        val message = ApiErrorMessages.field(error.field, error.code) ?: return@mapNotNull null
        field to message
    }.toMap()
}
