package com.pawrise.feature.dogprofile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwFlowTextField
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.component.PwQuestion
import com.pawrise.core.designsystem.component.pwFieldBox
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwText
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.pets.Sex
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Une étape du profil (Figma 08 à 16) : question puis contenu, écarts 24, marges 20, 16 dp sous l'en-tête. */
@Composable
internal fun AddDogStepContent(state: AddDogUiState, actions: AddDogActions) {
    val dog = state.dogName
    StepColumn {
        when (state.step) {
            AddDogStep.Photo -> {
                PwQuestion("Ajoutez une photo de votre chien")
                PhotoStep(state, actions)
            }
            AddDogStep.Name -> {
                PwQuestion("Comment s’appelle votre chien ?")
                PwFlowTextField(
                    value = state.name, onValueChange = actions.onName, label = "Nom du chien", placeholder = "Ex. Nala",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                    // Comme iOS : « Suivant » du clavier passe à l'étape suivante.
                    keyboardActions = KeyboardActions(onNext = { actions.onNext() }),
                )
            }
            AddDogStep.BirthDate -> {
                // Texte de la maquette (« né » quel que soit le sexe, choisi à l'étape suivante), comme iOS.
                PwQuestion(
                    "Quand est né $dog ?",
                    "Si vous ne connaissez pas la date exacte, vous pouvez indiquer une date approximative.",
                )
                val invalid = state.birthDigits.length == 8 && parseBirthDate(state.birthDigits) == BirthDate.Invalid
                FieldWithError(if (invalid) AddDogMessages.BIRTH_INVALID else null) {
                    PwFlowTextField(
                        value = state.birthDigits, onValueChange = actions.onBirthDate, label = "Date de naissance",
                        placeholder = "JJ / MM / AAAA", invalid = invalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = BirthDateTransformation,
                    )
                }
            }
            AddDogStep.Sex -> {
                PwQuestion("$dog est…")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SexCard("Femelle", "♀", state.sex == Sex.Female, Modifier.weight(1f)) { actions.onSex(Sex.Female) }
                    SexCard("Mâle", "♂", state.sex == Sex.Male, Modifier.weight(1f)) { actions.onSex(Sex.Male) }
                }
            }
            AddDogStep.Breed -> {
                PwQuestion("Quelle est la race de $dog ?")
                BreedStep(state, actions)
            }
            AddDogStep.Weight -> {
                PwQuestion("Combien pèse $dog ?", "Un poids approximatif suffit.")
                val invalid = parseWeight(state.weight) == Measurement.Invalid
                FieldWithError(if (invalid) AddDogMessages.WEIGHT_INVALID else null) {
                    UnitField(state.weight, actions.onWeight, "kg", "Poids en kilogrammes", invalid)
                }
            }
            AddDogStep.Height -> {
                PwQuestion("Quelle est la taille de $dog ?", "Au garrot, en centimètres. Une mesure approximative est acceptée.")
                val invalid = parseHeight(state.height) == Measurement.Invalid
                FieldWithError(if (invalid) AddDogMessages.HEIGHT_INVALID else null) {
                    UnitField(state.height, actions.onHeight, "cm", "Taille en centimètres", invalid)
                }
            }
            AddDogStep.Medical -> {
                PwQuestion(
                    "Y a-t-il quelque chose d’important à savoir sur la santé de $dog ?",
                    "Optionnel — conditions connues, traitements, allergies, antécédents… Ces informations pourront aider votre vétérinaire plus tard.",
                )
                PwFlowTextField(
                    value = state.medical, onValueChange = actions.onMedical, label = "Infos santé",
                    placeholder = "Écrire ici (facultatif)…", singleLine = false, minHeight = 174.dp,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
            AddDogStep.Summary -> {
                PwQuestion("Le profil de $dog", "Vérifiez les informations avant de continuer.")
                Summary(state, actions.onEdit)
            }
        }
    }
}

@Composable
private fun StepColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}

/** Champ + message d'erreur (Inter 600 12, #B4233A) à 8 dp dessous. */
@Composable
private fun FieldWithError(error: String?, field: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        field()
        if (error != null) PwFieldError(error)
    }
}

/**
 * Poids / taille (maquette A13 / A14) : valeur à gauche, unité à 221,5 dp du bord du champ, Inter 17.
 */
@Composable
private fun UnitField(value: String, onValueChange: (String) -> Unit, unit: String, label: String, invalid: Boolean) {
    PwFlowTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = "—",
        invalid = invalid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        fieldWidth = UNIT_OFFSET,
        trailing = {
            Text(unit, style = PwText.FlowField, color = PwColors.TextPrimary, modifier = Modifier.clearAndSetSemantics {})
        },
    )
}

/** Position de l'unité : 221,5 dp du bord du champ, moins la marge intérieure de 16. */
private val UNIT_OFFSET = 203.dp

/**
 * Maquette A08 : photo ronde 160, bouton « + » 44 en bas à droite, « ou choisissez un exemple » (Inter 12,5),
 * 4 vignettes 64 × 64 rayon 16 écartées de 12, sélection bordée de #0A5C78 (2 dp) ; écarts 20.
 */
@Composable
private fun PhotoStep(state: AddDogUiState, actions: AddDogActions) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val chosen = state.photo != PhotoChoice.None
        Box(
            Modifier.size(160.dp).clickable(
                role = Role.Button,
                onClickLabel = if (chosen) "Changer la photo" else "Ajouter une photo depuis la galerie",
                onClick = actions.onPickPhoto,
            ),
        ) {
            if (chosen) {
                PetPhoto(state.photo, contentDescription = "Aperçu de la photo", modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Box(
                    Modifier.fillMaxSize().background(PwColors.AvatarBackdrop, CircleShape).semantics { contentDescription = "Ajouter une photo" },
                    contentAlignment = Alignment.Center,
                ) { Icon(PwIcons.Paw, contentDescription = null, tint = PwColors.Action.copy(alpha = 0.5f), modifier = Modifier.size(56.dp)) }
            }
            PhotoPlusBadge(Modifier.align(Alignment.BottomEnd).offset((-1).dp, (-1).dp))
        }
        state.photoError?.let { PwFieldError(it) }
        Text("ou choisissez un exemple", style = PwText.inter(12.5f), color = PwColors.TextMuted)
        // Cadre Figma de 296 dp, vignettes calées à gauche (292 dp) : même position que la maquette.
        Row(Modifier.width(296.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SamplePhoto.entries.forEachIndexed { index, sample ->
                val selected = (state.photo as? PhotoChoice.Sample)?.sample == sample
                val shape = RoundedCornerShape(16.dp)
                PetPhoto(
                    PhotoChoice.Sample(sample),
                    contentDescription = "Exemple de photo ${index + 1}",
                    modifier = Modifier.size(64.dp).clip(shape)
                        .then(if (selected) Modifier.border(2.dp, PwColors.Action, shape) else Modifier)
                        .semantics { this.selected = selected }
                        .clickable(role = Role.RadioButton) { actions.onSample(sample) },
                )
            }
        }
    }
}

/** Bouton « + » de la maquette : disque 44 #0A5C78, croix blanche 16 dp, trait 2,6, bouts ronds. */
@Composable
private fun PhotoPlusBadge(modifier: Modifier) {
    Canvas(modifier.size(44.dp)) {
        drawCircle(PwColors.Action)
        val c = center
        val half = 8.dp.toPx()
        val w = 2.6.dp.toPx()
        drawLine(PwColors.ActionText, Offset(c.x, c.y - half), Offset(c.x, c.y + half), w, StrokeCap.Round)
        drawLine(PwColors.ActionText, Offset(c.x - half, c.y), Offset(c.x + half, c.y), w, StrokeCap.Round)
    }
}

/**
 * Maquette A11 : carte 132 dp rayon 24, symbole Noto Sans Symbols 36 (boîte 49), libellé Inter 600 15, écart 12 ;
 * choisie : fond #E5F1F7, bordure #0A5C78.
 */
@Composable
private fun SexCard(label: String, symbol: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.heightIn(min = 132.dp)
            .background(if (selected) PwColors.SurfaceElevated else PwColors.SurfaceCard, shape)
            .border(1.dp, if (selected) PwColors.Action else PwColors.BorderStrong, shape)
            .clip(shape)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        // Boîte de 49 dp (interligne Figma) ; la police a de grandes métriques (2,05 em) : on place la ligne
        // naturelle nous-mêmes pour que l'encre tombe où Figma la dessine.
        Box(Modifier.height(49.dp).clearAndSetSemantics {}, contentAlignment = Alignment.TopCenter) {
            Text(
                symbol,
                style = PwText.symbols(36f).copy(lineHeight = TextUnit.Unspecified),
                color = PwColors.TextPrimary,
                modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true).offset(y = SYMBOL_OFFSET),
            )
        }
        Text(label, style = PwText.inter(15f, FontWeight.SemiBold), color = PwColors.TextPrimary)
    }
}

/** Haut de la ligne naturelle du symbole par rapport à sa boîte de 49 dp (encre placée comme dans Figma). */
private val SYMBOL_OFFSET = (-15.7).dp

/** Maquette A12 : champ de recherche puis suggestions (même cadre ; choisie : fond #E5F1F7, bordure #0A5C78). */
@Composable
private fun BreedStep(state: AddDogUiState, actions: AddDogActions) {
    PwFlowTextField(
        value = state.breedQuery,
        onValueChange = actions.onBreedQuery,
        label = "Rechercher une race",
        placeholder = "Rechercher une race",
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.breeds.forEach { breed ->
            val selected = (state.breed as? BreedChoice.Known)?.breed?.code == breed.code
            Box(
                Modifier.fillMaxWidth().pwFieldBox(selected = selected).clip(RoundedCornerShape(16.dp))
                    .semantics { this.selected = selected }
                    .clickable(role = Role.RadioButton) { actions.onBreed(BreedChoice.Known(breed)) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(breed.name, style = PwText.FlowField, color = PwColors.TextPrimary) }
        }
        when {
            state.breedsLoading && state.breeds.isEmpty() -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = PwColors.Action)
            }
            state.breedsFailed -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PwFieldError(AddDogMessages.BREEDS_FAILED)
                TextButton(onClick = actions.onRetryBreeds) {
                    Text("Réessayer", style = PwText.inter(14f, FontWeight.SemiBold), color = PwColors.Action)
                }
            }
            !state.breedsLoading && state.breeds.isEmpty() && state.breedQuery.isNotBlank() -> Text(
                "Aucune race trouvée.",
                style = PwText.Hint,
                color = PwColors.TextMuted,
            )
        }
    }
}

private val DATE_FR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

/** 28.0 → « 28 », 27.5 → « 27,5 ». */
internal fun Double.toFrench(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')

/**
 * Maquette A16 : photo 96 rayon 16, nom Bricolage 24 (écart 8) ; carte rayon 24 bordure #C1D4E0,
 * 6 lignes de 51 dp (libellé Inter 13 sur 124 dp, valeur Inter 14, chevron 16 à 5 dp du bord), écarts 12.
 */
@Composable
private fun Summary(state: AddDogUiState, onEdit: (AddDogStep) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PetPhoto(state.photo, "Photo de ${state.dogName}", Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)))
        Text(state.dogName, style = PwText.display(24f), color = PwColors.TextPrimary)
    }
    val birth = (parseBirthDate(state.birthDigits) as? BirthDate.Valid)?.date?.format(DATE_FR) ?: "Non renseignée"
    val rows = listOf(
        Triple("Date de naissance", birth, AddDogStep.BirthDate),
        Triple("Sexe", when (state.sex) { Sex.Female -> "Femelle"; Sex.Male -> "Mâle"; else -> "Non renseigné" }, AddDogStep.Sex),
        Triple(
            "Race",
            when (val b = state.breed) { is BreedChoice.Known -> b.breed.name; else -> "Non renseignée" },
            AddDogStep.Breed,
        ),
        Triple("Poids", (parseWeight(state.weight) as? Measurement.Valid)?.let { "${it.value.toFrench()} kg" } ?: "Non renseigné", AddDogStep.Weight),
        Triple("Taille", (parseHeight(state.height) as? Measurement.Valid)?.let { "${it.value.toFrench()} cm" } ?: "Non renseignée", AddDogStep.Height),
        Triple("Infos santé", state.medical.trim().ifEmpty { "Aucune" }, AddDogStep.Medical),
    )
    val card = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().background(PwColors.SurfaceCard, card).border(1.dp, PwColors.BorderStrong, card)
            .clip(card).padding(1.dp),
    ) {
        rows.forEach { (label, value, step) ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 51.dp)
                    .clickable(onClickLabel = "Modifier $label") { onEdit(step) }
                    .padding(start = 16.dp, end = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = PwText.inter(13f), color = PwColors.TextMuted, modifier = Modifier.width(124.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    value,
                    style = PwText.inter(14f),
                    color = PwColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Icon(PwIcons.ChevronRight, contentDescription = null, tint = PwColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
