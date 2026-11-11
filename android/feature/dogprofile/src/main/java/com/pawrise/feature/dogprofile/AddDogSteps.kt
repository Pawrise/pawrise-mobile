package com.pawrise.feature.dogprofile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.pets.Sex
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun AddDogStepContent(state: AddDogUiState, actions: AddDogActions) {
    val dog = state.dogName
    when (state.step) {
        AddDogStep.Photo -> Scrollable {
            Question("Ajoutez une photo de votre chien")
            PhotoStep(state, actions)
        }
        AddDogStep.Name -> Scrollable {
            Question("Comment s'appelle votre chien ?")
            QuestionField(
                value = state.name, onValueChange = actions.onName, a11yLabel = "Nom du chien", placeholder = "Ex. Rio",
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        }
        AddDogStep.BirthDate -> Scrollable {
            Question(
                "Quand est né${if (state.sex == Sex.Female) "e" else ""} $dog ?",
                "Si vous ne connaissez pas la date exacte, vous pouvez indiquer une date approximative.",
            )
            QuestionField(
                value = state.birthDigits, onValueChange = actions.onBirthDate, a11yLabel = "Date de naissance",
                placeholder = "JJ / MM / AAAA", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
                transformation = BirthDateTransformation,
            )
        }
        AddDogStep.Sex -> Scrollable {
            Question("$dog est…")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SexCard("Femelle", PwIcons.Female, state.sex == Sex.Female, Modifier.weight(1f)) { actions.onSex(Sex.Female) }
                SexCard("Mâle", PwIcons.Male, state.sex == Sex.Male, Modifier.weight(1f)) { actions.onSex(Sex.Male) }
            }
        }
        AddDogStep.Breed -> BreedStep(state, actions)
        AddDogStep.Weight -> Scrollable {
            Question("Combien pèse $dog ?", "Un poids approximatif suffit.")
            UnitField(state.weight, actions.onWeight, "kg", "Poids en kilogrammes")
        }
        AddDogStep.Height -> Scrollable {
            Question("Quelle est la taille de $dog ?", "Au garrot, en centimètres. Une mesure approximative est acceptée.")
            UnitField(state.height, actions.onHeight, "cm", "Taille en centimètres")
        }
        AddDogStep.Medical -> Scrollable {
            Question(
                "Y a-t-il quelque chose d'important à savoir sur la santé de $dog ?",
                "Optionnel — conditions connues, traitements, allergies, antécédents… Ces informations pourront aider votre vétérinaire plus tard.",
            )
            QuestionField(
                value = state.medical, onValueChange = actions.onMedical, a11yLabel = "Infos santé",
                placeholder = "Écrire ici (facultatif)…", singleLine = false, minLines = 6,
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        AddDogStep.Summary -> Scrollable { Summary(state, actions.onEdit) }
    }
}

@Composable
private fun Scrollable(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { content() }
}

@Composable
internal fun Question(title: String, hint: String? = null) {
    Column(Modifier.padding(top = 16.dp, bottom = 24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuestionField(
    value: String,
    onValueChange: (String) -> Unit,
    a11yLabel: String,
    placeholder: String,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    transformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboard,
        visualTransformation = transformation,
        shape = PwRadius.Control,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = a11yLabel },
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = PwColors.SurfaceCard,
    unfocusedContainerColor = PwColors.SurfaceCard,
    unfocusedBorderColor = PwColors.BorderStrong,
    focusedBorderColor = PwColors.Action,
)

/** Poids / taille : grand chiffre + unité (maquette `UnitInput`). */
@Composable
private fun UnitField(value: String, onValueChange: (String) -> Unit, unit: String, a11yLabel: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("0", style = MaterialTheme.typography.headlineMedium) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = { Text(unit, style = MaterialTheme.typography.titleMedium, color = PwColors.TextMuted, modifier = Modifier.padding(end = 12.dp)) },
        textStyle = MaterialTheme.typography.headlineMedium,
        shape = PwRadius.Control,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = a11yLabel },
    )
}

@Composable
private fun PhotoStep(state: AddDogUiState, actions: AddDogActions) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(160.dp)) {
            val chosen = state.photo != PhotoChoice.None
            Box(
                Modifier.fillMaxSize().clip(CircleShape)
                    .border(2.dp, if (chosen) PwColors.Action else PwColors.BorderStrong, CircleShape)
                    .clickable(onClickLabel = "Choisir une photo dans la galerie", onClick = actions.onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                if (chosen) {
                    PetPhoto(state.photo, contentDescription = "Aperçu de la photo", modifier = Modifier.fillMaxSize())
                } else {
                    Icon(PwIcons.Paw, contentDescription = "Ajouter une photo", tint = PwColors.TextMuted, modifier = Modifier.size(44.dp))
                }
            }
            Surface(
                shape = CircleShape, color = PwColors.Action, contentColor = PwColors.ActionText,
                modifier = Modifier.align(Alignment.BottomEnd).size(44.dp),
            ) { Box(contentAlignment = Alignment.Center) { Icon(PwIcons.Plus, null, Modifier.size(22.dp)) } }
        }
        state.photoError?.let { PwFieldError(it, Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("ou choisissez un exemple", style = MaterialTheme.typography.bodySmall, color = PwColors.TextMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SamplePhoto.entries.forEachIndexed { index, sample ->
                val selected = (state.photo as? PhotoChoice.Sample)?.sample == sample
                PetPhoto(
                    PhotoChoice.Sample(sample),
                    contentDescription = "Exemple de photo ${index + 1}",
                    modifier = Modifier.size(64.dp).clip(PwRadius.Control)
                        .border(2.dp, if (selected) PwColors.Action else PwColors.SurfacePage, PwRadius.Control)
                        .semantics { this.selected = selected }
                        .clickable(role = Role.RadioButton) { actions.onSample(sample) },
                )
            }
        }
    }
}

@Composable
private fun SexCard(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) PwColors.Action.copy(alpha = 0.1f) else PwColors.SurfaceCard,
        contentColor = if (selected) PwColors.Action else PwColors.TextPrimary,
        border = BorderStroke(1.dp, if (selected) PwColors.Action else PwColors.BorderStrong),
        modifier = modifier.semantics { this.selected = selected },
    ) {
        Column(Modifier.padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BreedStep(state: AddDogUiState, actions: AddDogActions) {
    Column(Modifier.fillMaxSize()) {
        Question("Quelle est la race de ${state.dogName} ?")
        OutlinedTextField(
            value = state.breedQuery,
            onValueChange = actions.onBreedQuery,
            placeholder = { Text("Rechercher une race…") },
            leadingIcon = { Icon(PwIcons.Search, null, Modifier.size(18.dp)) },
            singleLine = true,
            shape = PwRadius.Control,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Rechercher une race" },
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            item {
                BreedRow("Je ne sais pas", state.breed == BreedChoice.Unknown) { actions.onBreed(BreedChoice.Unknown) }
            }
            items(state.breeds, key = { it.code }) { breed ->
                BreedRow(breed.name, (state.breed as? BreedChoice.Known)?.breed?.code == breed.code) {
                    actions.onBreed(BreedChoice.Known(breed))
                }
            }
            item {
                when {
                    state.breedsLoading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    }
                    state.breedsFailed -> Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        PwFieldError(AddDogMessages.BREEDS_FAILED)
                        TextButton(onClick = actions.onRetryBreeds) { Text("Réessayer") }
                    }
                    state.breeds.isEmpty() && state.breedQuery.isNotBlank() -> Text(
                        "Aucune race trouvée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PwColors.TextMuted,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreedRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = PwRadius.Control,
        color = if (selected) PwColors.Action.copy(alpha = 0.1f) else PwColors.SurfaceCard,
        contentColor = if (selected) PwColors.Action else PwColors.TextPrimary,
        border = BorderStroke(1.dp, if (selected) PwColors.Action else PwColors.BorderStrong),
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (selected) Icon(PwIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

private val DATE_FR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

/** 28.0 → « 28 », 27.5 → « 27,5 ». */
internal fun Double.toFrench(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')

@Composable
private fun Summary(state: AddDogUiState, onEdit: (AddDogStep) -> Unit) {
    Question("Le profil de ${state.dogName}", "Vérifiez les informations avant de continuer.")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PetPhoto(state.photo, "Photo de ${state.dogName}", Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)))
        Spacer(Modifier.height(12.dp))
        Text(state.dogName, style = MaterialTheme.typography.headlineSmall)
    }
    Spacer(Modifier.height(20.dp))
    val birth = (parseBirthDate(state.birthDigits) as? BirthDate.Valid)?.date?.format(DATE_FR) ?: "—"
    val rows = listOf(
        Triple("Date de naissance", birth, AddDogStep.BirthDate),
        Triple("Sexe", when (state.sex) { Sex.Female -> "Femelle"; Sex.Male -> "Mâle"; else -> "—" }, AddDogStep.Sex),
        Triple(
            "Race",
            when (val b = state.breed) { is BreedChoice.Known -> b.breed.name; BreedChoice.Unknown -> "Je ne sais pas"; null -> "—" },
            AddDogStep.Breed,
        ),
        Triple("Poids", (parseMeasurement(state.weight) as? Measurement.Valid)?.let { "${it.value.toFrench()} kg" } ?: "—", AddDogStep.Weight),
        Triple("Taille", (parseMeasurement(state.height) as? Measurement.Valid)?.let { "${it.value.toFrench()} cm" } ?: "—", AddDogStep.Height),
        Triple("Infos santé", state.medical.trim().ifEmpty { "Aucune" }, AddDogStep.Medical),
    )
    Surface(shape = PwRadius.Card, color = PwColors.SurfaceCard, border = BorderStroke(1.dp, PwColors.BorderStrong)) {
        Column {
            rows.forEachIndexed { i, (label, value, step) ->
                if (i > 0) HorizontalDivider(color = PwColors.Border)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(onClickLabel = "Modifier $label") { onEdit(step) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = PwColors.TextMuted, modifier = Modifier.width(128.dp))
                    Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(PwIcons.ChevronRight, contentDescription = null, tint = PwColors.TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
