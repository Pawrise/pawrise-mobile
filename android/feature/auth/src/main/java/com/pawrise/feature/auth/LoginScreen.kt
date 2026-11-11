package com.pawrise.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.component.PwBackLink
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.component.PwTextField
import com.pawrise.core.designsystem.component.PwTextLink
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwSpacing

/** US-J · 02 Connexion (`AuthScreen · login`, Figma 2025:5801). */
@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onSignup: () -> Unit,
    onLoggedIn: () -> Unit,
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
    )
}

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSignup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormScaffold(
        title = "Connexion",
        hint = "Retrouvez vos chiens et leur suivi.",
        onBack = onBack,
        modifier = modifier,
    ) {
        PwTextField(
            label = "E-mail",
            value = state.email,
            onValueChange = onEmailChange,
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(12.dp))
        PwTextField(
            label = "Mot de passe",
            value = state.password,
            onValueChange = onPasswordChange,
            enabled = !state.loading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        state.error?.let { PwFieldError(it, Modifier.padding(start = 4.dp, top = 12.dp)) }
        Spacer(Modifier.height(20.dp))
        PwPrimaryButton("Se connecter", onClick = onSubmit, loading = state.loading)
        Spacer(Modifier.height(8.dp))
        PwTextLink("Créer un compte", onClick = onSignup, enabled = !state.loading)
    }
}

/** Mise en page commune Connexion / Inscription : « < Retour », titre, sous-titre, formulaire défilant. */
@Composable
internal fun AuthFormScaffold(
    title: String,
    hint: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PwSpacing.Section, vertical = PwSpacing.Page),
    ) {
        PwBackLink(onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    PawriseTheme {
        LoginScreen(
            state = LoginUiState(email = "alex@example.com", password = "secret12", error = AuthMessages.INVALID_CREDENTIALS),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {}, onBack = {}, onSignup = {},
        )
    }
}
