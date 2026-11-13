package com.pawrise.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.Preferences
import com.pawrise.core.auth.PreferencesUpdate
import com.pawrise.core.auth.User
import com.pawrise.core.network.ApiErrorMessages
import com.pawrise.core.network.ApiResult
import com.pawrise.core.pets.Pet
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.scenario.HealthScenario
import com.pawrise.core.pets.scenario.HealthScenarioStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Les trois interrupteurs de Confidentialité (S17), chacun envoyé seul par `PATCH /v1/me/preferences`. */
enum class PrivacyToggle(val read: (Preferences) -> Boolean, val write: (Preferences, Boolean) -> Preferences) {
    Location({ it.deviceLocationSharing }, { p, v -> p.copy(deviceLocationSharing = v) }),
    HealthNotifications({ it.healthNotifications }, { p, v -> p.copy(healthNotifications = v) }),
    AnonymizedData({ it.anonymizedDataSharing }, { p, v -> p.copy(anonymizedDataSharing = v) }),
    ;

    fun update(value: Boolean): PreferencesUpdate = when (this) {
        Location -> PreferencesUpdate(deviceLocationSharing = value)
        HealthNotifications -> PreferencesUpdate(healthNotifications = value)
        AnonymizedData -> PreferencesUpdate(anonymizedDataSharing = value)
    }
}

sealed interface PrivacyState {
    data object Loading : PrivacyState
    data class Loaded(val preferences: Preferences) : PrivacyState
    data class Failed(val message: String) : PrivacyState
}

data class SettingsUiState(
    val user: User? = null,
    val privacy: PrivacyState = PrivacyState.Loading,
    val savingPrivacy: Set<PrivacyToggle> = emptySet(),
    val privacyError: String? = null,
    val pets: List<Pet> = emptyList(),
    val scenario: HealthScenario = HealthScenario.Normal,
    val sendingVerification: Boolean = false,
    val verificationMessage: String? = null,
    val loggingOut: Boolean = false,
    val deleting: Boolean = false,
    val deleteError: String? = null,
    /** Déconnexion ou suppression terminée : l'app revient à Bienvenue. */
    val signedOut: Boolean = false,
)

/** Onglet Paramètres : compte, confidentialité, chiens, données simulées, déconnexion, suppression du compte. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val scenarioStore: HealthScenarioStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(user = authRepository.user.value))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { authRepository.user.collect { user -> _state.update { it.copy(user = user ?: it.user) } } }
        viewModelScope.launch { scenarioStore.scenario.collect { scenario -> _state.update { it.copy(scenario = scenario) } } }
        refresh()
    }

    /** Recharge le compte (adresse confirmée entre-temps…), les chiens et les préférences. */
    fun refresh() {
        viewModelScope.launch { authRepository.me() }
        viewModelScope.launch {
            (petRepository.listPets() as? ApiResult.Success)?.let { pets -> _state.update { it.copy(pets = pets.value) } }
        }
        loadPrivacy()
    }

    fun loadPrivacy() {
        _state.update { it.copy(privacy = PrivacyState.Loading) }
        viewModelScope.launch {
            val privacy = when (val result = authRepository.preferences()) {
                is ApiResult.Success -> PrivacyState.Loaded(result.value)
                is ApiResult.Failure -> PrivacyState.Failed(ApiErrorMessages.of(result.error))
            }
            _state.update { it.copy(privacy = privacy) }
        }
    }

    /** L'interrupteur change tout de suite ; refusé par le serveur, il revient à sa position et un message s'affiche. */
    fun setPrivacy(toggle: PrivacyToggle, value: Boolean) {
        val loaded = _state.value.privacy as? PrivacyState.Loaded ?: return
        if (toggle in _state.value.savingPrivacy) return
        val previous = toggle.read(loaded.preferences)
        _state.update {
            it.copy(
                privacy = PrivacyState.Loaded(toggle.write(loaded.preferences, value)),
                savingPrivacy = it.savingPrivacy + toggle,
                privacyError = null,
            )
        }
        viewModelScope.launch {
            val result = authRepository.updatePreferences(toggle.update(value))
            _state.update { s ->
                val current = (s.privacy as? PrivacyState.Loaded)?.preferences ?: return@update s
                // On ne reprend que la valeur de cet interrupteur : un autre peut être en cours d'envoi.
                val (kept, error) = when (result) {
                    is ApiResult.Success -> toggle.read(result.value) to null
                    is ApiResult.Failure -> previous to ApiErrorMessages.of(result.error)
                }
                s.copy(
                    privacy = PrivacyState.Loaded(toggle.write(current, kept)),
                    savingPrivacy = s.savingPrivacy - toggle,
                    privacyError = error ?: s.privacyError,
                )
            }
        }
    }

    fun selectScenario(scenario: HealthScenario) = scenarioStore.select(scenario)

    fun resendVerification() {
        if (_state.value.sendingVerification) return
        _state.update { it.copy(sendingVerification = true) }
        viewModelScope.launch {
            val message = when (val result = authRepository.resendVerificationEmail()) {
                is ApiResult.Success -> "Nouveau lien envoyé. Il est valable 24 heures."
                is ApiResult.Failure -> ApiErrorMessages.of(result.error)
            }
            _state.update { it.copy(sendingVerification = false, verificationMessage = message) }
        }
    }

    fun logout() {
        if (_state.value.loggingOut) return
        _state.update { it.copy(loggingOut = true) }
        viewModelScope.launch {
            authRepository.logout()
            scenarioStore.select(HealthScenario.Normal)
            _state.update { it.copy(loggingOut = false, signedOut = true) }
        }
    }

    /** `DELETE /v1/me` ; en cas de succès la session est fermée et l'app revient à l'accueil. */
    fun deleteAccount() {
        if (_state.value.deleting) return
        _state.update { it.copy(deleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = authRepository.deleteAccount()) {
                is ApiResult.Success -> {
                    scenarioStore.select(HealthScenario.Normal)
                    _state.update { it.copy(deleting = false, signedOut = true) }
                }
                is ApiResult.Failure ->
                    _state.update { it.copy(deleting = false, deleteError = ApiErrorMessages.of(result.error)) }
            }
        }
    }

    fun clearDeleteError() = _state.update { it.copy(deleteError = null) }
}
