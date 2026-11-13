package com.pawrise.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwColors

/** US-J · 02 Connexion (maquette J02, Figma 2025:5801) + lien « Mot de passe oublié ? ». */
@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onSignup: () -> Unit,
    onLoggedIn: () -> Unit,
    onForgotPassword: (email: String) -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onLoggedIn() }
    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
        onSignup = onSignup,
        onForgotPassword = { onForgotPassword(state.email) },
    )
}

/** Maquette J02 : marges 24, écart 16, contenu à 48 du haut de l'écran. */
@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSignup: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PwColors.SurfacePage)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = topInsetAware(48.dp, minGap = 8.dp), bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackLink(onClick = onBack)
        AuthTitle(title = "Connexion", subtitle = "Retrouvez vos chiens et leur suivi.")
        AuthField(
            label = "E-mail",
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = "vous@exemple.fr",
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
        )
        AuthPasswordField(
            label = "Mot de passe",
            value = state.password,
            onValueChange = onPasswordChange,
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        )
        AuthLink(
            "Mot de passe oublié ?",
            onClick = onForgotPassword,
            enabled = !state.loading,
            modifier = Modifier.align(Alignment.End),
        )
        state.error?.let { ErrorBanner(it) }
        AuthPrimaryButton("Se connecter", onClick = onSubmit, enabled = state.canSubmit, loading = state.loading)
        AuthLink("Créer un compte", onClick = onSignup, enabled = !state.loading, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    PawriseTheme {
        LoginScreen(
            state = LoginUiState(email = "alex@example.com", password = "secret12", error = "E-mail ou mot de passe incorrect."),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {}, onBack = {}, onSignup = {}, onForgotPassword = {},
        )
    }
}
