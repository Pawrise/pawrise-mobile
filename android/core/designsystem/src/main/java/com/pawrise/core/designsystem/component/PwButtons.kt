package com.pawrise.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius

/** Bouton plein « action » (Se connecter, Continuer, Suivant…). */
@Composable
fun PwPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PwRadius.Control,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Bouton blanc à texte action (Créer un compte sur l'écran Bienvenue). */
@Composable
fun PwSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PwRadius.Control,
        colors = ButtonDefaults.buttonColors(containerColor = PwColors.SurfaceCard, contentColor = PwColors.Action),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
