package com.pawrise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius

/** Champ de formulaire de la maquette : libellé au-dessus, champ blanc arrondi. */
@Composable
fun PwTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            // Le libellé est affiché au-dessus du champ : on l'associe au champ pour TalkBack.
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            placeholder = placeholder?.let { { Text(it) } },
            isError = error != null,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            shape = PwRadius.Control,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PwColors.SurfaceCard,
                unfocusedContainerColor = PwColors.SurfaceCard,
                disabledContainerColor = PwColors.SurfaceCard,
                errorContainerColor = PwColors.SurfaceCard,
                unfocusedBorderColor = PwColors.BorderStrong,
                focusedBorderColor = PwColors.Action,
            ),
        )
        if (error != null) PwFieldError(error, Modifier.padding(start = 4.dp, top = 4.dp))
    }
}

/** Message d'erreur sous un champ ou un formulaire (annoncé par TalkBack). */
@Composable
fun PwFieldError(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Lien « < Retour » en haut des formulaires (Figma 02 · Connexion). */
@Composable
fun PwBackLink(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "Retour") {
    TextButton(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(PwIcons.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Lien texte centré (« Créer un compte », « J'ai déjà un compte »). */
@Composable
fun PwTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = PwColors.Action)
    }
}

internal val CardBorder = BorderStroke(1.dp, PwColors.BorderStrong)
