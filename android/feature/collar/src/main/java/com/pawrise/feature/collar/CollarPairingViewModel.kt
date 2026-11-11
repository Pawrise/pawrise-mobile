package com.pawrise.feature.collar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.ErrorCodes
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.isValidCollarCode
import com.pawrise.core.pets.normalizeCollarCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CollarPhase { Intro, Scan, Manual, Activating, Success }

data class CollarUiState(
    val phase: CollarPhase = CollarPhase.Intro,
    val manualCode: String = "",
    val error: String? = null,
    val serialNumber: String? = null,
) {
    val manualValid: Boolean get() = isValidCollarCode(manualCode)
}

/** US-A · 17-20 Associer le collier (`PUT /pets/{petId}/collar`). */
@HiltViewModel
class CollarPairingViewModel @Inject constructor(private val petRepository: PetRepository) : ViewModel() {

    private val _state = MutableStateFlow(CollarUiState())
    val state: StateFlow<CollarUiState> = _state.asStateFlow()

    /** Durée minimale de l'écran « Activation… » (maquette : 1 s), pour éviter un flash. */
    internal var minActivationMillis = 1_000L

    /** Saisie d'où vient le code : on y revient en cas d'erreur. */
    private var source = CollarPhase.Manual

    /** Code scanné refusé : la caméra le relit en continu, on ne le renvoie pas en boucle. */
    private var rejectedScan: String? = null

    fun openScanner() = _state.update { it.copy(phase = CollarPhase.Scan, error = null) }
    fun openManual() = _state.update { it.copy(phase = CollarPhase.Manual, error = null) }
    fun onManualCode(value: String) = _state.update { it.copy(manualCode = collarInput(value), error = null) }

    /** Retour : scan / saisie → choix ; `false` si l'écran doit laisser partir l'utilisateur. */
    fun back(): Boolean = when (_state.value.phase) {
        CollarPhase.Scan, CollarPhase.Manual -> {
            _state.update { it.copy(phase = CollarPhase.Intro, error = null) }
            true
        }
        CollarPhase.Activating -> true // on attend la réponse
        CollarPhase.Intro, CollarPhase.Success -> false
    }

    fun submitManual(petId: String) {
        if (!_state.value.manualValid) return
        source = CollarPhase.Manual
        activate(petId, _state.value.manualCode)
    }

    /** QR lu par la caméra ; un QR sans code Pawrise est signalé, la caméra continue. */
    fun onScanned(petId: String, raw: String) {
        if (_state.value.phase != CollarPhase.Scan) return
        val code = extractCollarCode(raw)
        if (code == null) return _state.update { it.copy(error = CollarMessages.NOT_A_COLLAR) }
        if (code == rejectedScan) return
        source = CollarPhase.Scan
        activate(petId, code)
    }

    private fun activate(petId: String, code: String) {
        _state.update { it.copy(phase = CollarPhase.Activating, error = null) }
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            val result = petRepository.setCollar(petId, code)
            delay((minActivationMillis - (System.currentTimeMillis() - started)).coerceAtLeast(0))
            when (result) {
                is ApiResult.Success -> _state.update {
                    it.copy(phase = CollarPhase.Success, serialNumber = result.value.serialNumber)
                }
                is ApiResult.Failure -> {
                    if (source == CollarPhase.Scan) rejectedScan = normalizeCollarCode(code)
                    _state.update { it.copy(phase = source, error = result.error.collarMessage(), manualCode = normalizeCollarCode(code)) }
                }
            }
        }
    }
}

internal object CollarMessages {
    const val NOT_A_COLLAR = "Ce QR code n'est pas celui d'un collier Pawrise."
    const val UNKNOWN = "Code inconnu. Vérifiez le code inscrit sous le collier."
    const val ALREADY_ASSIGNED = "Ce collier est déjà associé à un autre chien."
    const val INVALID = "Code invalide. Format attendu : PW- suivi de 6 lettres ou chiffres."
    const val NETWORK = "Connexion impossible. Vérifiez votre réseau et réessayez."
    const val GENERIC = "Une erreur est survenue. Réessayez."
    const val CAMERA_DENIED = "Autorisez l'appareil photo pour scanner le QR code, ou saisissez le code."
}

private fun ApiError.collarMessage(): String = when {
    this is ApiError.Network -> CollarMessages.NETWORK
    this is ApiError.Problem && code == ErrorCodes.COLLAR_ALREADY_ASSIGNED -> CollarMessages.ALREADY_ASSIGNED
    this is ApiError.Problem && hasFieldError("serialNumber", ErrorCodes.UNKNOWN_COLLAR) -> CollarMessages.UNKNOWN
    this is ApiError.Problem && hasFieldError("serialNumber", ErrorCodes.INVALID_COLLAR_CODE) -> CollarMessages.INVALID
    else -> CollarMessages.GENERIC
}
