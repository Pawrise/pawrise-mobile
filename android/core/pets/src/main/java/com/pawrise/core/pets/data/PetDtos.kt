package com.pawrise.core.pets.data

import kotlinx.serialization.Serializable

// Schémas « Animaux » et « Colliers » du contrat pawrise-doc (camelCase). Voir api/openapi.yaml.

@Serializable
data class BreedDto(val code: String, val name: String)

@Serializable
data class BreedListDto(val items: List<BreedDto>)

@Serializable
data class PetDto(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val photoSample: String? = null,
    val birthDate: String? = null,
    val sex: String,
    val breeds: List<BreedDto> = emptyList(),
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val medicalNotes: String? = null,
    val myRole: String,
    val collarSerial: String? = null,
)

@Serializable
data class PetListDto(val items: List<PetDto>)

@Serializable
data class CreatePetRequest(
    val name: String,
    val birthDate: String? = null,
    val sex: String,
    val breedCodes: List<String> = emptyList(),
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val medicalNotes: String? = null,
)

@Serializable
data class SamplePhotoRequest(val sample: String)

@Serializable
data class SetCollarRequest(val serialNumber: String)

@Serializable
data class CollarDto(
    val serialNumber: String,
    val model: String,
    val batteryPct: Int? = null,
    val connected: Boolean,
    val lastSeenAt: String? = null,
    val assignedAt: String,
)
