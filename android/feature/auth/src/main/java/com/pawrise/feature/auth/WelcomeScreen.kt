package com.pawrise.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.component.PawriseMark
import com.pawrise.core.designsystem.theme.bricolageGrotesque
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwColors

/** 01 · Bienvenue (maquette A01, Figma 2025:1824). */
@Composable
fun WelcomeRoute(onLogin: () -> Unit, onSignup: () -> Unit) {
    WelcomeScreen(onLogin = onLogin, onSignup = onSignup)
}

/**
 * Bloc centré dans les 680 premiers points (écran de 844), boutons à 680 et 748, marges 24, 40 sous les boutons.
 * Sur un écran d'une autre hauteur, seul le bloc du haut s'étire.
 */
@Composable
internal fun WelcomeScreen(onLogin: () -> Unit, onSignup: () -> Unit, modifier: Modifier = Modifier) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier.fillMaxSize().background(PwColors.SurfacePage)) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier.size(80.dp).background(PwColors.SurfaceElevated, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) { PawriseMark(size = 48.dp) }
            Text(
                "Pawrise",
                style = pwText(32f, FontWeight.Bold, family = bricolageGrotesque(32f)),
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Le collier connecté qui suit la santé et la position de votre chien.",
                style = pwText(15f, color = PwColors.TextMuted, align = TextAlign.Center),
                modifier = Modifier.width(288.dp),
            )
        }
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = maxOf(40.dp, bottomInset + 16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthPrimaryButton("Se connecter", onClick = onLogin)
            AuthSecondaryButton("Créer un compte", onClick = onSignup)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WelcomeScreenPreview() {
    PawriseTheme { WelcomeScreen(onLogin = {}, onSignup = {}) }
}
