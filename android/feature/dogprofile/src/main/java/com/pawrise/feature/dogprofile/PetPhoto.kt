package com.pawrise.feature.dogprofile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.pawrise.core.designsystem.R
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.pets.SamplePhoto

/** Photos d'exemple embarquées (contrat : `sample_1` … `sample_4`). */
internal val SamplePhoto.drawable: Int
    get() = when (this) {
        SamplePhoto.Sample1 -> R.drawable.pet_sample_1
        SamplePhoto.Sample2 -> R.drawable.pet_sample_2
        SamplePhoto.Sample3 -> R.drawable.pet_sample_3
        SamplePhoto.Sample4 -> R.drawable.pet_sample_4
    }

/** Photo du chien ; sans photo choisie, l'app affiche `sample_1` (règle du contrat). */
@Composable
fun PetPhoto(photo: PhotoChoice, contentDescription: String?, modifier: Modifier = Modifier) {
    val m = modifier.background(PwColors.AvatarBackdrop)
    when (photo) {
        is PhotoChoice.Own -> AsyncImage(photo.uri, contentDescription, m, contentScale = ContentScale.Crop)
        is PhotoChoice.Sample -> Image(painterResource(photo.sample.drawable), contentDescription, m, contentScale = ContentScale.Crop)
        PhotoChoice.None -> Image(painterResource(SamplePhoto.Sample1.drawable), contentDescription, m, contentScale = ContentScale.Crop)
    }
}

/** Sérialise le choix de photo pour la navigation (clé d'exemple ou URI locale). */
fun PhotoChoice.encode(): String? = when (this) {
    PhotoChoice.None -> null
    is PhotoChoice.Sample -> "sample:${sample.key}"
    is PhotoChoice.Own -> "uri:$uri"
}

fun decodePhotoChoice(value: String?): PhotoChoice = when {
    value == null -> PhotoChoice.None
    value.startsWith("sample:") -> SamplePhoto.fromKey(value.removePrefix("sample:"))?.let { PhotoChoice.Sample(it) } ?: PhotoChoice.None
    value.startsWith("uri:") -> PhotoChoice.Own(value.removePrefix("uri:"))
    else -> PhotoChoice.None
}
