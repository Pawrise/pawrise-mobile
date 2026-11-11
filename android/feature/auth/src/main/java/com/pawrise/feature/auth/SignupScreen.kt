package com.pawrise.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.component.PwTextField
import com.pawrise.core.designsystem.component.PwTextLink
import com.pawrise.core.designsystem.theme.PawriseTheme

/** US-A · 02 Créer un compte (`AuthScreen · signup`, Figma 2025:1848), avec M1–M3. */
@Composable
fun SignupRoute(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.registered) { if (state.registered) onRegistered() }
    SignupScreen(state, viewModel, onBack, onLogin)
}

@Composable
private fun SignupScreen(state: SignupUiState, actions: SignupViewModel, onBack: () -> Unit, onLogin: () -> Unit) {
    SignupForm(
        state = state,
        onFirstName = actions::onFirstNameChange,
        onLastName = actions::onLastNameChange,
        onEmail = actions::onEmailChange,
        onPhone = actions::onPhoneChange,
        onPassword = actions::onPasswordChange,
        onTerms = actions::onTermsChange,
        onSubmit = actions::submit,
        onBack = onBack,
        onLogin = onLogin,
    )
}

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
    val next = KeyboardOptions(imeAction = ImeAction.Next)
    val names = next.copy(capitalization = KeyboardCapitalization.Words)
    AuthFormScaffold(
        title = "Créer un compte",
        hint = "Quelques informations, puis on vous montre Pawrise.",
        onBack = onBack,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PwTextField(
                label = "Prénom", value = state.firstName, onValueChange = onFirstName, enabled = enabled,
                error = state.fieldErrors[SignupField.FirstName], keyboardOptions = names, modifier = Modifier.weight(1f),
            )
            PwTextField(
                label = "Nom", value = state.lastName, onValueChange = onLastName, enabled = enabled,
                error = state.fieldErrors[SignupField.LastName], keyboardOptions = names, modifier = Modifier.weight(1f),
            )
        }
        Gap()
        PwTextField(
            label = "E-mail", value = state.email, onValueChange = onEmail, enabled = enabled,
            error = state.fieldErrors[SignupField.Email],
            keyboardOptions = next.copy(keyboardType = KeyboardType.Email),
        )
        Gap()
        PwTextField(
            label = "Téléphone (facultatif)", value = state.phone, onValueChange = onPhone, enabled = enabled,
            error = state.fieldErrors[SignupField.Phone],
            keyboardOptions = next.copy(keyboardType = KeyboardType.Phone),
        )
        Gap()
        PwTextField(
            label = "Mot de passe", value = state.password, onValueChange = onPassword, enabled = enabled,
            error = state.fieldErrors[SignupField.Password],
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        if (state.fieldErrors[SignupField.Password] == null) {
            Text(
                "8 caractères minimum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        Gap()
        TermsCheckbox(state.termsAccepted, onTerms, enabled)
        state.fieldErrors[SignupField.Terms]?.let { PwFieldError(it, Modifier.padding(start = 4.dp)) }
        state.formError?.let { PwFieldError(it, Modifier.padding(start = 4.dp, top = 12.dp)) }
        Spacer(Modifier.height(20.dp))
        PwPrimaryButton("Continuer", onClick = onSubmit, loading = state.loading)
        Spacer(Modifier.height(8.dp))
        PwTextLink("J'ai déjà un compte", onClick = onLogin, enabled = enabled)
    }
}

/** M1 : acceptation des CGU obligatoire (RG11). Toute la ligne est cochable. */
@Composable
private fun TermsCheckbox(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(8.dp))
        Text(
            "J'accepte les conditions d'utilisation et la politique de confidentialité.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Gap() = Spacer(Modifier.height(12.dp))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignupFormPreview() {
    PawriseTheme {
        SignupForm(
            state = SignupUiState(
                firstName = "Alex", lastName = "Martin", email = "alex@example.com",
                fieldErrors = mapOf(SignupField.Email to SignupMessages.EMAIL_USED),
            ),
            onFirstName = {}, onLastName = {}, onEmail = {}, onPhone = {}, onPassword = {}, onTerms = {},
            onSubmit = {}, onBack = {}, onLogin = {},
        )
    }
}
