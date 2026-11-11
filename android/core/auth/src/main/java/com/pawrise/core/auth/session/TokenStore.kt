package com.pawrise.core.auth.session

import kotlinx.coroutines.flow.StateFlow

/**
 * Jetons de la session courante. [tokens] vaut `null` hors session.
 * L'implémentation garde une copie en mémoire : l'en-tête Bearer est lu à chaque requête.
 */
interface TokenStore {
    val tokens: StateFlow<Tokens?>

    /** Charge les jetons persistés. Appelé une fois au démarrage, avant [tokens]. */
    suspend fun load()

    suspend fun save(tokens: Tokens)

    suspend fun clear()
}
