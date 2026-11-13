package com.pawrise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius
import com.pawrise.core.designsystem.theme.PwText

/** Bouton plein « action » (Se connecter, Continuer, Suivant…). [loading] : roue + clic ignoré. */
@Composable
fun PwPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        shape = PwRadius.Control,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = PwColors.Action.copy(alpha = 0.4f),
            disabledContentColor = PwColors.ActionText,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = PwColors.ActionText, strokeWidth = 2.5.dp)
        } else {
            Text(text, style = PwText.Button)
        }
    }
}

/**
 * Bouton blanc à texte action (Créer un compte sur l'écran Bienvenue).
 * [border] : nul pour le « Button/Secondary » sans bordure de la maquette (Saisir le code manuellement, A17).
 */
@Composable
fun PwSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    border: BorderStroke? = CardBorder,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PwRadius.Control,
        colors = ButtonDefaults.buttonColors(containerColor = PwColors.SurfaceCard, contentColor = PwColors.Action),
        border = border,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, style = PwText.Button)
    }
}
