package com.pawrise.core.pets

import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.apiCall
import com.pawrise.core.network.map
import com.pawrise.core.pets.data.BreedDto
import com.pawrise.core.pets.data.CollarDto
import com.pawrise.core.pets.data.CreatePetRequest
import com.pawrise.core.pets.data.PetApi
import com.pawrise.core.pets.data.PetDto
import com.pawrise.core.pets.data.SamplePhotoRequest
import com.pawrise.core.pets.data.SetCollarRequest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

enum class Sex(val wire: String) {
    Female("female"),
    Male("male"),
    Unknown("unknown"),
    ;

    companion object {
        /** Valeur inconnue de l'app → neutre (convention 7). */
        fun fromWire(value: String) = entries.firstOrNull { it.wire == value } ?: Unknown
    }
}

data class Breed(val code: String, val name: String)

/** Photo d'exemple embarquée dans l'app (enum `SamplePhotoKey` du contrat). */
enum class SamplePhoto(val key: String) {
    Sample1("sample_1"),
    Sample2("sample_2"),
    Sample3("sample_3"),
    Sample4("sample_4"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

data class Pet(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val photoSample: SamplePhoto?,
    val birthDate: LocalDate?,
    val sex: Sex,
    val breeds: List<Breed>,
    val weightKg: Double?,
    val heightCm: Double?,
    val medicalNotes: String?,
    val collarSerial: String?,
)

/** Saisie de l'ajout d'un chien (US-A · 08-16). `breeds` vide = « Je ne sais pas ». */
data class NewPet(
    val name: String,
    val birthDate: LocalDate?,
    val sex: Sex,
    val breeds: List<Breed>,
    val weightKg: Double?,
    val heightCm: Double?,
    val medicalNotes: String?,
)

data class Collar(val serialNumber: String, val model: String, val batteryPct: Int?, val connected: Boolean)

/** Ma propre photo, déjà réduite par l'app (JPEG ou PNG, 1,5 Mo maximum). */
class PhotoFile(val bytes: ByteArray, val mimeType: String)

interface PetRepository {
    suspend fun listPets(): ApiResult<List<Pet>>
    suspend fun createPet(pet: NewPet): ApiResult<Pet>
    suspend fun setSamplePhoto(petId: String, sample: SamplePhoto): ApiResult<Pet>
    suspend fun uploadPhoto(petId: String, photo: PhotoFile): ApiResult<Pet>
    suspend fun searchBreeds(query: String): ApiResult<List<Breed>>

    /** Associe (ou remplace) le collier ; [serialNumber] tel que scanné ou saisi. */
    suspend fun setCollar(petId: String, serialNumber: String): ApiResult<Collar>
}

@Singleton
class DefaultPetRepository @Inject constructor(private val api: PetApi, private val json: Json) : PetRepository {

    override suspend fun listPets() = apiCall(json) { api.listPets().items.map { it.toDomain() } }

    override suspend fun createPet(pet: NewPet) = apiCall(json) {
        api.createPet(
            CreatePetRequest(
                name = pet.name.trim(),
                birthDate = pet.birthDate?.toString(),
                sex = pet.sex.wire,
                breedCodes = pet.breeds.map { it.code },
                weightKg = pet.weightKg,
                heightCm = pet.heightCm,
                medicalNotes = pet.medicalNotes?.trim()?.takeIf { it.isNotEmpty() },
            ),
        ).toDomain()
    }

    override suspend fun setSamplePhoto(petId: String, sample: SamplePhoto) =
        apiCall(json) { api.setSamplePhoto(petId, SamplePhotoRequest(sample.key)).toDomain() }

    override suspend fun uploadPhoto(petId: String, photo: PhotoFile) = apiCall(json) {
        val extension = if (photo.mimeType == "image/png") "png" else "jpg"
        val part = MultipartBody.Part.createFormData(
            "file",
            "photo.$extension",
            photo.bytes.toRequestBody(photo.mimeType.toMediaType()),
        )
        api.uploadPhoto(petId, part).toDomain()
    }

    override suspend fun searchBreeds(query: String) =
        apiCall(json) { api.listBreeds(query.trim().takeIf { it.isNotEmpty() }).items.map { it.toDomain() } }

    override suspend fun setCollar(petId: String, serialNumber: String): ApiResult<Collar> =
        apiCall(json) { api.setCollar(petId, SetCollarRequest(normalizeCollarCode(serialNumber))) }.map { it.toDomain() }
}

/** Code collier normalisé comme côté serveur : majuscules, sans espaces (`PW-8A4F2C`). */
fun normalizeCollarCode(code: String): String = code.trim().uppercase().replace(Regex("\\s"), "")

/** Format attendu : `PW-` + 6 lettres ou chiffres (maquette `validCollar`). */
fun isValidCollarCode(code: String): Boolean = Regex("^PW-[A-Z0-9]{6}$").matches(normalizeCollarCode(code))

private fun BreedDto.toDomain() = Breed(code, name)

private fun CollarDto.toDomain() = Collar(serialNumber, model, batteryPct, connected)

private fun PetDto.toDomain() = Pet(
    id = id,
    name = name,
    photoUrl = photoUrl,
    photoSample = SamplePhoto.fromKey(photoSample),
    birthDate = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    sex = Sex.fromWire(sex),
    breeds = breeds.map { it.toDomain() },
    weightKg = weightKg,
    heightCm = heightCm,
    medicalNotes = medicalNotes,
    collarSerial = collarSerial,
)
