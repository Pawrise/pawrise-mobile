package com.pawrise.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.pets.Pet
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.scenario.HealthScenario
import com.pawrise.core.pets.scenario.HealthScenarioStore
import com.pawrise.feature.home.data.HealthRepository
import com.pawrise.feature.home.data.HealthSnapshot
import com.pawrise.feature.home.data.HealthState
import com.pawrise.feature.home.data.IndicatorKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Panneau du bas ouvert depuis la homepage (US-B 02 à 07). */
sealed interface HealthSheet {
    data class Indicator(val kind: IndicatorKind) : HealthSheet
    data object Collar : HealthSheet
}

data class HealthHomeUiState(
    val pets: List<Pet> = emptyList(),
    val selectedPetId: String? = null,
    val loadingPets: Boolean = true,
    val petsError: String? = null,
    val snapshot: HealthSnapshot? = null,
    val snapshotError: String? = null,
    val sheet: HealthSheet? = null,
) {
    val selectedPet: Pet? get() = pets.firstOrNull { it.id == selectedPetId } ?: pets.firstOrNull()
    val canSwitchDog: Boolean get() = pets.size > 1

    /**
     * Panneau d'un indicateur ouvert : le badge de l'en-tête prend l'état de cet indicateur
     * (maquette US-B · alerte : « Alerte » derrière le panneau Fibrillation atriale).
     */
    val focusedState: HealthState?
        get() = (sheet as? HealthSheet.Indicator)?.let { open -> snapshot?.indicators?.firstOrNull { it.kind == open.kind }?.pillState }
}

/**
 * Onglet Pawrise · Santé : chiens du compte (chien affiché, bascule), mesures du chien affiché selon le scénario
 * de données simulées, panneau ouvert. Partagé par la homepage, son panneau et le Chat IA (chien affiché).
 */
@HiltViewModel
class HealthHomeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val healthRepository: HealthRepository,
    private val scenarioStore: HealthScenarioStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HealthHomeUiState())
    val state: StateFlow<HealthHomeUiState> = _state.asStateFlow()

    /** Chien affiché (homepage et Chat IA). */
    val selectedPet: StateFlow<Pet?> = _state.map { it.selectedPet }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var snapshotJob: Job? = null
    private val attempt = MutableStateFlow(0)

    init {
        loadPets()
        viewModelScope.launch {
            combine(selectedPet, scenarioStore.scenario, attempt) { pet, scenario, _ -> pet to scenario }
                .collect { (pet, scenario) -> loadSnapshot(pet, scenario) }
        }
    }

    fun loadPets() {
        _state.update { it.copy(loadingPets = true, petsError = null) }
        viewModelScope.launch {
            when (val result = petRepository.listPets()) {
                is ApiResult.Success -> _state.update { current ->
                    val keep = current.selectedPetId?.takeIf { id -> result.value.any { it.id == id } }
                    current.copy(pets = result.value, selectedPetId = keep ?: result.value.firstOrNull()?.id, loadingPets = false)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loadingPets = false, petsError = "Impossible de charger vos chiens. ${result.error.message()}")
                }
            }
        }
    }

    /** Chien suivant (icône ⇄ de l'en-tête, active dès qu'il y a un deuxième chien). */
    fun selectNextDog() {
        _state.update { current ->
            if (current.pets.size < 2) return@update current
            val index = current.pets.indexOfFirst { it.id == current.selectedPet?.id }
            current.copy(selectedPetId = current.pets[(index + 1) % current.pets.size].id, sheet = null)
        }
    }

    fun retrySnapshot() {
        attempt.value += 1
    }

    fun open(sheet: HealthSheet?) {
        _state.update { it.copy(sheet = sheet) }
    }

    /** Icône d'alerte de l'en-tête : ouvre le premier indicateur en anomalie. */
    fun openFirstAnomaly() {
        val first = _state.value.snapshot?.indicators?.firstOrNull { it.state != HealthState.Good } ?: return
        open(HealthSheet.Indicator(first.kind))
    }

    private fun loadSnapshot(pet: Pet?, scenario: HealthScenario) {
        snapshotJob?.cancel()
        if (pet == null) {
            _state.update { it.copy(snapshot = null, snapshotError = null) }
            return
        }
        _state.update { it.copy(snapshot = null, snapshotError = null) }
        snapshotJob = viewModelScope.launch {
            val result = runCatching { healthRepository.snapshot(pet, scenario) }
            result.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(snapshot = it, snapshotError = null) },
                    onFailure = { current.copy(snapshot = null, snapshotError = "Impossible de charger les mesures de ${pet.name}. ${it.message.orEmpty()}".trim()) },
                )
            }
        }
    }
}

private fun ApiError.message(): String = when (this) {
    is ApiError.Network -> "Vérifiez votre connexion et réessayez."
    is ApiError.Problem -> detail ?: "Réessayez dans un instant."
    is ApiError.Unexpected -> "Réessayez dans un instant."
}

/** ViewModel de l'onglet Santé, partagé par la coque (homepage, panneau, chien du Chat IA). */
@androidx.compose.runtime.Composable
fun healthHomeViewModel(): HealthHomeViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
