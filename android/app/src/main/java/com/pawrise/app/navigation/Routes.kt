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
@Serializable data object LearnFlow : NavKey
@Serializable data object AddDogFlow : NavKey
@Serializable data object Home : NavKey
