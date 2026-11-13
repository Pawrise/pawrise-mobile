package com.pawrise.feature.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.pawrise.core.designsystem.R
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.pets.Pet
import com.pawrise.core.pets.SamplePhoto
import java.time.LocalDate
import java.time.Period

/** Photo du chien : la sienne, sinon la photo d'exemple choisie, sinon `sample_1` (règle du contrat). */
@Composable
fun DogAvatar(pet: Pet, size: Dp, modifier: Modifier = Modifier, cornerRadius: Dp? = null) {
    val shape = cornerRadius?.let { RoundedCornerShape(it) } ?: CircleShape
    val m = modifier.size(size).clip(shape).background(PwColors.AvatarBackdrop)
    val url = pet.photoUrl
    if (url != null) {
        AsyncImage(url, contentDescription = null, modifier = m, contentScale = ContentScale.Crop)
    } else {
        Image(painterResource(pet.samplePhotoRes), contentDescription = null, modifier = m, contentScale = ContentScale.Crop)
    }
}

private val Pet.samplePhotoRes: Int
    get() = when (photoSample ?: SamplePhoto.Sample1) {
        SamplePhoto.Sample1 -> R.drawable.pet_sample_1
        SamplePhoto.Sample2 -> R.drawable.pet_sample_2
        SamplePhoto.Sample3 -> R.drawable.pet_sample_3
        SamplePhoto.Sample4 -> R.drawable.pet_sample_4
    }

/** Libellé d'âge comme dans le Figma : « 3 ans », « 8 mois ». */
fun Pet.ageLabel(today: LocalDate = LocalDate.now()): String? {
    val birth = birthDate ?: return null
    val months = Period.between(birth, today).toTotalMonths().toInt()
    if (months < 12) return "${months.coerceAtLeast(0)} mois"
    val years = months / 12
    return if (years >= 2) "$years ans" else "1 an"
}

/** Sous-titre de l'en-tête : « Golden Retriever · 3 ans ». */
fun Pet.subtitle(today: LocalDate = LocalDate.now()): String =
    listOfNotNull(breeds.joinToString(" / ") { it.name }.ifEmpty { null }, ageLabel(today)).joinToString(" · ")

/** État du collier affiché dans Santé et dans le Chat IA. */
data class CollarInfo(val code: String, val model: String, val signalStrong: Boolean, val connected: Boolean)

/**
 * Collier du chien. L'API ne renvoie aujourd'hui que le code associé : modèle et réseau reprennent les valeurs
 * de démonstration de la maquette (comme l'app iOS), en attendant les mesures du collier.
 */
val Pet.collar: CollarInfo?
    get() = collarSerial?.let { CollarInfo(code = it, model = "Pawrise Collar C2", signalStrong = true, connected = true) }
