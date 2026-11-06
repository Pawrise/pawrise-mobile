package com.pawrise.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.component.PwSecondaryButton
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius
import com.pawrise.core.designsystem.theme.PwSpacing

/** 01 · Bienvenue (`AuthScreen · welcome`, Figma 2025:1824). */
@Composable
fun WelcomeRoute(onLogin: () -> Unit, onSignup: () -> Unit) {
    WelcomeScreen(onLogin = onLogin, onSignup = onSignup)
}

@Composable
internal fun WelcomeScreen(onLogin: () -> Unit, onSignup: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = PwSpacing.Page, vertical = PwSpacing.Page),
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = PwSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // TODO: remplacer par le logo Pawrise (export SVG depuis Figma → vector drawable).
            Box(Modifier.size(80.dp).background(PwColors.SurfaceElevated, PwRadius.Card))
            Spacer(Modifier.height(PwSpacing.Section))
            Text("Pawrise", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(PwSpacing.Section))
            Text(
                "Le collier connecté qui suit la santé et la position de votre chien.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        PwPrimaryButton("Se connecter", onClick = onLogin)
        Spacer(Modifier.height(12.dp))
        PwSecondaryButton("Créer un compte", onClick = onSignup)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WelcomeScreenPreview() {
    PawriseTheme { WelcomeScreen(onLogin = {}, onSignup = {}) }
}
