package com.pawrise.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.pawrise.core.auth.SessionState
import com.pawrise.core.designsystem.component.PlaceholderScreen
import com.pawrise.feature.auth.LoginRoute
import com.pawrise.feature.auth.SignupRoute
import com.pawrise.feature.auth.WelcomeRoute
import kotlinx.coroutines.flow.Flow

@Composable
fun PawriseNavDisplay(initialSession: SessionState, session: Flow<SessionState>) {
    val backStack = rememberNavBackStack(if (initialSession == SessionState.SignedIn) Home else Welcome)

    // Session perdue (refresh refusé, déconnexion) → retour à Bienvenue, historique vidé.
    LaunchedEffect(session) {
        session.collect { state ->
            if (state == SessionState.SignedOut && backStack.lastOrNull() !in AUTH_DESTINATIONS) {
                backStack.resetTo(Welcome)
            }
        }
    }

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
            entry<Login> {
                LoginRoute(
                    onBack = { backStack.removeLastOrNull() },
                    onSignup = { backStack.replaceTop(Signup) },
                    onLoggedIn = { backStack.resetTo(Home) },
                )
            }
            entry<Signup> {
                SignupRoute(
                    onBack = { backStack.removeLastOrNull() },
                    onLogin = { backStack.replaceTop(Login) },
                    onRegistered = { backStack.resetTo(LearnFlow) },
                )
            }
            entry<LearnFlow> { PlaceholderScreen("03-07 · Découverte (LearnFlow)") }
            entry<AddDogFlow> { PlaceholderScreen("08-20 · Profil du chien et collier (AddDogFlow)") }
            entry<Home> { PlaceholderScreen("Homepage · Santé") }
        },
    )
}

/** Écrans accessibles sans session : on n'y force pas de retour à Bienvenue. */
private val AUTH_DESTINATIONS = setOf(Welcome, Login, Signup)

/** Remplace l'écran courant (Connexion ↔ Inscription) sans empiler. */
private fun MutableList<NavKey>.replaceTop(key: NavKey) {
    removeLastOrNull()
    add(key)
}

/** Vide l'historique : « Retour » ne ramène pas vers l'écran précédent (après connexion, session perdue). */
private fun MutableList<NavKey>.resetTo(key: NavKey) {
    clear()
    add(key)
}
