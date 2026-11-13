package com.pawrise.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.pawrise.core.auth.SessionState
import com.pawrise.feature.auth.ForgotPasswordRoute
import com.pawrise.feature.auth.LoginRoute
import com.pawrise.feature.auth.SignupRoute
import com.pawrise.feature.auth.WelcomeRoute
import com.pawrise.feature.collar.CollarPairingRoute
import com.pawrise.feature.dogprofile.AddDogRoute
import com.pawrise.feature.dogprofile.decodePhotoChoice
import com.pawrise.feature.dogprofile.encode
import com.pawrise.feature.onboarding.LearnFlowRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun PawriseNavDisplay(
    startDestination: NavKey,
    session: Flow<SessionState>,
    destinationAfterSignIn: suspend () -> NavKey,
) {
    val backStack = rememberNavBackStack(startDestination)
    val scope = rememberCoroutineScope()

    // Session perdue (refresh refusé, déconnexion) → retour à Bienvenue, historique vidé.
    LaunchedEffect(session) {
        session.collect { state ->
            val top = backStack.lastOrNull()
            if (state == SessionState.SignedOut && top !in AUTH_DESTINATIONS && top !is ForgotPassword) {
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
                    onLoggedIn = { scope.launch { backStack.resetTo(destinationAfterSignIn()) } },
                    onForgotPassword = { email -> backStack.add(ForgotPassword(email)) },
                )
            }
            entry<ForgotPassword> { key ->
                ForgotPasswordRoute(
                    initialEmail = key.email,
                    onBack = { backStack.removeLastOrNull() },
                    onSignedIn = { scope.launch { backStack.resetTo(destinationAfterSignIn()) } },
                )
            }
            entry<Signup> {
                SignupRoute(
                    onBack = { backStack.removeLastOrNull() },
                    onLogin = { backStack.replaceTop(Login) },
                    onRegistered = { backStack.resetTo(LearnFlow) },
                )
            }
            entry<LearnFlow> { LearnFlowRoute(onDone = { backStack.add(AddDogFlow) }) }
            entry<AddDogFlow> {
                AddDogRoute(
                    // Pendant l'inscription : retour à la découverte. Compte repris sans chien : idem.
                    onLeave = { if (backStack.size > 1) backStack.removeLastOrNull() else backStack.replaceTop(LearnFlow) },
                    onCreated = { dog -> backStack.resetTo(CollarPairing(dog.petId, dog.name, dog.photo.encode())) },
                )
            }
            entry<CollarPairing> { key ->
                CollarPairingRoute(
                    petId = key.petId,
                    petName = key.petName,
                    photo = decodePhotoChoice(key.photo),
                    onDone = { backStack.resetTo(Home) },
                )
            }
            entry<Home> { MainShell(onSignedOut = { backStack.resetTo(Welcome) }) }
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
