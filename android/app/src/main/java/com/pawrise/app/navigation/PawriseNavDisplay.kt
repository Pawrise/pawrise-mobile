package com.pawrise.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.pawrise.core.designsystem.component.PlaceholderScreen
import com.pawrise.feature.auth.WelcomeRoute

@Composable
fun PawriseNavDisplay() {
    val backStack = rememberNavBackStack(Welcome)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Welcome> {
                WelcomeRoute(
                    onLogin = { backStack.add(Login) },
                    onSignup = { backStack.add(Signup) },
                )
            }
            // Écrans à implémenter : remplacer chaque placeholder par la Route du module feature.
            entry<Login> { PlaceholderScreen("02 · Connexion (US-J)") }
            entry<Signup> { PlaceholderScreen("02 · Créer un compte (US-A)") }
            entry<LearnFlow> { PlaceholderScreen("03-07 · Découverte (LearnFlow)") }
            entry<AddDogFlow> { PlaceholderScreen("08-20 · Profil du chien et collier (AddDogFlow)") }
            entry<Home> { PlaceholderScreen("Homepage · Santé") }
        },
    )
}
