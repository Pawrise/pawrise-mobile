package com.pawrise.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Destinations de l'app. Les noms suivent les blocs du Figma
 * (US-A « Je crée mon compte », US-J « Je me connecte »).
 */
@Serializable data object Welcome : NavKey
@Serializable data object Login : NavKey
@Serializable data object Signup : NavKey

/** Mot de passe oublié, ouvert depuis Connexion avec l'e-mail déjà saisi. */
@Serializable data class ForgotPassword(val email: String) : NavKey
@Serializable data object LearnFlow : NavKey
@Serializable data object AddDogFlow : NavKey
@Serializable data object Home : NavKey

/** US-A · 17-20 : association du collier du chien tout juste créé. [photo] : `PhotoChoice.encode()`. */
@Serializable data class CollarPairing(val petId: String, val petName: String, val photo: String?) : NavKey
