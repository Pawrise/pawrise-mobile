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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.network.CredentialRules

/** Mot de passe oublié (hors maquette, langage visuel de Connexion J02). Connecte directement à la fin. */
@Composable
fun ForgotPasswordRoute(
    initialEmail: String,
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.prefill(initialEmail) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }
    ForgotPasswordScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onCodeChange = viewModel::onCodeChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onSubmit = viewModel::submit,
        onResend = viewModel::resend,
        onBack = onBack,
    )
}

@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
) {
    val enabled = !state.loading
    Column(
        Modifier
            .fillMaxSize()
            .background(PwColors.SurfacePage)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = topInsetAware(48.dp, minGap = 8.dp), bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackLink(onClick = onBack)
        AuthTitle(
            title = "Mot de passe oublié",
            subtitle = if (state.codeSent) {
                "Saisissez le code reçu à ${CredentialRules.normalizedEmail(state.email)} et choisissez un nouveau mot de passe."
            } else {
                "Indiquez votre adresse : nous vous envoyons un code pour choisir un nouveau mot de passe."
            },
        )
        AuthField(
            label = "E-mail",
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = "vous@exemple.fr",
            enabled = enabled && !state.codeSent,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = if (state.codeSent) ImeAction.Next else ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        )
        if (state.codeSent) {
            AuthField(
                label = "Code à 6 chiffres",
                value = state.code,
                onValueChange = onCodeChange,
                placeholder = "123456",
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
            )
            AuthField(
                label = "Nouveau mot de passe",
                value = state.newPassword,
                onValueChange = onNewPasswordChange,
                placeholder = "8 caractères minimum",
                enabled = enabled,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            )
        }
        state.info?.let { InfoBanner(it) }
        state.error?.let { ErrorBanner(it) }
        AuthPrimaryButton(
            if (state.codeSent) "Changer le mot de passe" else "Recevoir un code",
            onClick = onSubmit,
            loading = state.loading,
        )
        if (state.codeSent) {
            AuthLink("Renvoyer un code", onClick = onResend, enabled = enabled, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ForgotPasswordPreview() {
    PawriseTheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(email = "alex@example.com", codeSent = true, info = ForgotMessages.SENT),
            onEmailChange = {}, onCodeChange = {}, onNewPasswordChange = {}, onSubmit = {}, onResend = {}, onBack = {},
        )
    }
}
