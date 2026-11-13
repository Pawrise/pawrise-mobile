package com.pawrise.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwColors

/** US-A · 02 Créer un compte (maquette A02, Figma 2025:1848) : Prénom et Nom séparés, case CGU obligatoire. */
@Composable
fun SignupRoute(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.registered) { if (state.registered) onRegistered() }
    SignupForm(
        state = state,
        onFirstName = viewModel::onFirstNameChange,
        onLastName = viewModel::onLastNameChange,
        onEmail = viewModel::onEmailChange,
        onPhone = viewModel::onPhoneChange,
        onPassword = viewModel::onPasswordChange,
        onTerms = viewModel::onTermsChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
        onLogin = onLogin,
    )
}

/** En-tête « chevron seul » (64), contenu à 16 dessous, marges 20, 24 entre titre et formulaire, 12 entre les champs. */
@Composable
internal fun SignupForm(
    state: SignupUiState,
    onFirstName: (String) -> Unit,
    onLastName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTerms: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    val enabled = !state.loading
    val errors = state.fieldErrors
    val next = KeyboardOptions(imeAction = ImeAction.Next)
    val names = next.copy(capitalization = KeyboardCapitalization.Words)
    Column(Modifier.fillMaxSize().background(PwColors.SurfacePage).imePadding()) {
        ChevronHeader(onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AuthTitle(
                title = "Créer un compte",
                subtitle = "Quelques informations, puis on vous montre Pawrise.",
                subtitleSize = 13.5f,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    AuthField(
                        label = "Prénom", value = state.firstName, onValueChange = onFirstName, placeholder = "Alex",
                        error = errors[SignupField.FirstName], textSize = 17f, borderColor = PwColors.BorderStrong,
                        enabled = enabled, keyboardOptions = names, modifier = Modifier.weight(1f),
                    )
                    AuthField(
                        label = "Nom", value = state.lastName, onValueChange = onLastName, placeholder = "Martin",
                        error = errors[SignupField.LastName], textSize = 17f, borderColor = PwColors.BorderStrong,
                        enabled = enabled, keyboardOptions = names, modifier = Modifier.weight(1f),
                    )
                }
                AuthField(
                    label = "E-mail", value = state.email, onValueChange = onEmail, placeholder = "alex@exemple.fr",
                    error = errors[SignupField.Email], textSize = 17f, borderColor = PwColors.BorderStrong, enabled = enabled,
                    keyboardOptions = next.copy(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                )
                AuthField(
                    label = "Téléphone", value = state.phone, onValueChange = onPhone,
                    error = errors[SignupField.Phone], textSize = 17f, borderColor = PwColors.BorderStrong, enabled = enabled,
                    keyboardOptions = next.copy(keyboardType = KeyboardType.Phone),
                )
                AuthField(
                    label = "Mot de passe", value = state.password, onValueChange = onPassword,
                    placeholder = "8 caractères minimum",
                    error = errors[SignupField.Password], textSize = 17f, borderColor = PwColors.BorderStrong, enabled = enabled,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                )
                TermsCheckbox(state.termsAccepted, onTerms, enabled, errors[SignupField.Terms])
                state.formError?.let { ErrorBanner(it) }
                AuthPrimaryButton("Continuer", onClick = onSubmit, loading = state.loading)
                AuthLink("J’ai déjà un compte", onClick = onLogin, enabled = enabled, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignupFormPreview() {
    PawriseTheme {
        SignupForm(
            state = SignupUiState(firstName = "Alex", lastName = "Martin", email = "alex@example.com"),
            onFirstName = {}, onLastName = {}, onEmail = {}, onPhone = {}, onPassword = {}, onTerms = {},
            onSubmit = {}, onBack = {}, onLogin = {},
        )
    }
}
