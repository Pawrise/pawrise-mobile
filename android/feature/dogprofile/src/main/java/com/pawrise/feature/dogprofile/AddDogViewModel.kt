package com.pawrise.feature.dogprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.ErrorCodes
import com.pawrise.core.pets.Breed
import com.pawrise.core.pets.NewPet
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.pets.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Étapes de la maquette, dans l'ordre (Figma 08 → 16). */
enum class AddDogStep { Photo, Name, BirthDate, Sex, Breed, Weight, Height, Medical, Summary }

sealed interface PhotoChoice {
    data object None : PhotoChoice
    data class Sample(val sample: SamplePhoto) : PhotoChoice
    data class Own(val uri: String) : PhotoChoice
}

sealed interface BreedChoice {
    data object Unknown : BreedChoice
    data class Known(val breed: Breed) : BreedChoice
}

data class AddDogUiState(
    val step: AddDogStep = AddDogStep.Photo,
    val photo: PhotoChoice = PhotoChoice.None,
    val name: String = "",
    val birthDigits: String = "",
    val sex: Sex? = null,
    val breed: BreedChoice? = null,
    val weight: String = "",
    val height: String = "",
    val medical: String = "",
    val breedQuery: String = "",
    val breeds: List<Breed> = emptyList(),
    val breedsLoading: Boolean = false,
    val breedsFailed: Boolean = false,
    val photoError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val created: CreatedDog? = null,
) {
    val dogName: String get() = name.trim().ifEmpty { "votre chien" }

    /** Même règle que la maquette (`canNext`) : nom et sexe requis, le reste facultatif mais valide. */
    val canContinue: Boolean
        get() = when (step) {
            AddDogStep.Name -> name.isNotBlank()
            AddDogStep.Sex -> sex != null
            AddDogStep.BirthDate -> parseBirthDate(birthDigits) != BirthDate.Invalid
            AddDogStep.Weight -> parseMeasurement(weight) != Measurement.Invalid
            AddDogStep.Height -> parseMeasurement(height) != Measurement.Invalid
            else -> true
        }
}

/** Chien créé, transmis à l'association du collier (17-20). [photo] : clé d'exemple ou URI locale. */
data class CreatedDog(val petId: String, val name: String, val photo: PhotoChoice)

/** US-A · 08-16 Profil du chien (`POST /pets`, `POST /pets/{id}/photo`, `GET /breeds`). */
@HiltViewModel
class AddDogViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val photoLoader: PhotoLoader,
) : ViewModel() {

    private val _state = MutableStateFlow(AddDogUiState())
    val state: StateFlow<AddDogUiState> = _state.asStateFlow()
    private var breedSearch: Job? = null

    fun onSample(sample: SamplePhoto) = _state.update { it.copy(photo = PhotoChoice.Sample(sample), photoError = null) }
    fun onOwnPhoto(uri: String) = _state.update { it.copy(photo = PhotoChoice.Own(uri), photoError = null) }
    fun onName(value: String) = _state.update { it.copy(name = value.take(100)) }
    fun onBirthDate(value: String) = _state.update { it.copy(birthDigits = birthDateDigits(value)) }
    fun onSex(value: Sex) = _state.update { it.copy(sex = value) }
    fun onBreed(value: BreedChoice) = _state.update { it.copy(breed = value) }
    fun onWeight(value: String) = _state.update { it.copy(weight = decimalInput(value)) }
    fun onHeight(value: String) = _state.update { it.copy(height = decimalInput(value)) }
    fun onMedical(value: String) = _state.update { it.copy(medical = value.take(2000)) }

    fun onBreedQuery(value: String) {
        _state.update { it.copy(breedQuery = value) }
        searchBreeds(debounce = true)
    }

    fun retryBreeds() = searchBreeds(debounce = false)

    /** « Suivant » ; à la dernière étape, crée le chien. */
    fun next() {
        val s = _state.value
        if (!s.canContinue || s.saving) return
        if (s.step == AddDogStep.Summary) return save()
        goTo(AddDogStep.entries[s.step.ordinal + 1])
    }

    /** `false` à la première étape : l'écran laisse alors partir l'utilisateur. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step == AddDogStep.Photo || s.saving) return false
        goTo(AddDogStep.entries[s.step.ordinal - 1])
        return true
    }

    /** Depuis le récapitulatif : revenir sur une étape pour la corriger. */
    fun edit(step: AddDogStep) = goTo(step)

    private fun goTo(step: AddDogStep) {
        _state.update { it.copy(step = step, saveError = null) }
        if (step == AddDogStep.Breed && _state.value.breeds.isEmpty()) searchBreeds(debounce = false)
    }

    private fun searchBreeds(debounce: Boolean) {
        breedSearch?.cancel()
        breedSearch = viewModelScope.launch {
            if (debounce) delay(300)
            _state.update { it.copy(breedsLoading = true, breedsFailed = false) }
            when (val result = petRepository.searchBreeds(_state.value.breedQuery)) {
                is ApiResult.Success -> _state.update { it.copy(breeds = result.value, breedsLoading = false) }
                is ApiResult.Failure -> _state.update { it.copy(breedsLoading = false, breedsFailed = true) }
            }
        }
    }

    private fun save() {
        val s = _state.value
        val newPet = NewPet(
            name = s.name,
            birthDate = (parseBirthDate(s.birthDigits) as? BirthDate.Valid)?.date,
            sex = s.sex ?: Sex.Unknown,
            breeds = (s.breed as? BreedChoice.Known)?.let { listOf(it.breed) } ?: emptyList(),
            weightKg = (parseMeasurement(s.weight) as? Measurement.Valid)?.value,
            heightCm = (parseMeasurement(s.height) as? Measurement.Valid)?.value,
            medicalNotes = s.medical.trim().ifEmpty { null },
        )
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = petRepository.createPet(newPet)) {
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = result.error.saveMessage()) }
                is ApiResult.Success -> {
                    val pet = result.value
                    // Le chien existe : un échec de photo ne bloque pas l'association du collier
                    // (photo modifiable plus tard depuis la fiche du chien, US-14).
                    sendPhoto(pet.id, s.photo)
                    _state.update { it.copy(saving = false, created = CreatedDog(pet.id, pet.name, s.photo)) }
                }
            }
        }
    }

    private suspend fun sendPhoto(petId: String, photo: PhotoChoice) {
        when (photo) {
            PhotoChoice.None -> Unit
            is PhotoChoice.Sample -> petRepository.setSamplePhoto(petId, photo.sample)
            is PhotoChoice.Own -> photoLoader.load(photo.uri)?.let { petRepository.uploadPhoto(petId, it) }
        }
    }

    /** Vérifie tout de suite qu'une photo de la galerie est utilisable. */
    fun onOwnPhotoPicked(uri: String) {
        viewModelScope.launch {
            if (photoLoader.load(uri) == null) {
                _state.update { it.copy(photoError = AddDogMessages.PHOTO_UNREADABLE) }
            } else {
                onOwnPhoto(uri)
            }
        }
    }
}

internal object AddDogMessages {
    const val INVALID_VALUE = "Renseignez une valeur valide pour continuer."
    const val PHOTO_UNREADABLE = "Impossible d'utiliser cette photo. Choisissez-en une autre."
    const val BREEDS_FAILED = "Impossible de charger les races."
    const val BIRTH_FUTURE = "La date de naissance ne peut pas être dans le futur."
    const val UNKNOWN_BREED = "Cette race n'est plus proposée. Choisissez-en une autre."
    const val NETWORK = "Connexion impossible. Vérifiez votre réseau et réessayez."
    const val GENERIC = "Une erreur est survenue. Réessayez."
}

private fun ApiError.saveMessage(): String = when {
    this is ApiError.Network -> AddDogMessages.NETWORK
    this is ApiError.Problem && hasFieldError("birthDate", ErrorCodes.BIRTH_DATE_IN_FUTURE) -> AddDogMessages.BIRTH_FUTURE
    this is ApiError.Problem && hasFieldError("breedCodes", ErrorCodes.UNKNOWN_BREED) -> AddDogMessages.UNKNOWN_BREED
    else -> AddDogMessages.GENERIC
}
