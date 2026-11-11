package com.pawrise.core.testing

import com.pawrise.core.network.ApiResult
import com.pawrise.core.pets.Breed
import com.pawrise.core.pets.Collar
import com.pawrise.core.pets.NewPet
import com.pawrise.core.pets.Pet
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.PhotoFile
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.pets.Sex
import com.pawrise.core.pets.normalizeCollarCode
import kotlinx.coroutines.CompletableDeferred

/** Faux [PetRepository] scriptable, qui garde trace de ce que l'app envoie. */
class FakePetRepository(var pets: List<Pet> = listOf(NALA)) : PetRepository {

    var listResult: ApiResult<List<Pet>>? = null
    var createResult: ApiResult<Pet>? = null
    var photoResult: ApiResult<Pet>? = null
    var collarResult: ApiResult<Collar>? = null
    var breedsResult: ApiResult<List<Breed>>? = null
    var gate: CompletableDeferred<Unit>? = null

    val created = mutableListOf<NewPet>()
    val samplePhotos = mutableListOf<Pair<String, SamplePhoto>>()
    val uploadedPhotos = mutableListOf<Pair<String, PhotoFile>>()
    val breedQueries = mutableListOf<String>()
    val collars = mutableListOf<Pair<String, String>>()

    override suspend fun listPets(): ApiResult<List<Pet>> = listResult ?: ApiResult.Success(pets)

    override suspend fun createPet(pet: NewPet): ApiResult<Pet> {
        created += pet
        gate?.await()
        return createResult ?: ApiResult.Success(
            Pet("pet-new", pet.name.trim(), null, null, pet.birthDate, pet.sex, pet.breeds, pet.weightKg, pet.heightCm, pet.medicalNotes, null),
        ).also { pets = pets + it.value }
    }

    override suspend fun setSamplePhoto(petId: String, sample: SamplePhoto): ApiResult<Pet> {
        samplePhotos += petId to sample
        return photoResult ?: ApiResult.Success(pets.first { it.id == petId }.copy(photoSample = sample))
    }

    override suspend fun uploadPhoto(petId: String, photo: PhotoFile): ApiResult<Pet> {
        uploadedPhotos += petId to photo
        return photoResult ?: ApiResult.Success(pets.first { it.id == petId }.copy(photoUrl = "https://cdn.example/p.jpg"))
    }

    override suspend fun searchBreeds(query: String): ApiResult<List<Breed>> {
        breedQueries += query
        return breedsResult ?: ApiResult.Success(BREEDS.filter { it.name.contains(query.trim(), ignoreCase = true) })
    }

    override suspend fun setCollar(petId: String, serialNumber: String): ApiResult<Collar> {
        collars += petId to serialNumber
        gate?.await()
        return collarResult ?: ApiResult.Success(Collar(normalizeCollarCode(serialNumber), "C2", null, false))
    }

    companion object {
        val NALA = Pet("pet-nala", "Nala", null, SamplePhoto.Sample1, null, Sex.Female, emptyList(), 27.4, 56.0, null, "PW-C28A4F")
        val BREEDS = listOf(
            Breed("mixed", "Croisé"),
            Breed("akita_inu", "Akita Inu"),
            Breed("beagle", "Beagle"),
            Breed("golden_retriever", "Golden Retriever"),
        )
    }
}
