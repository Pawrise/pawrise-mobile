package com.pawrise.core.pets.scenario

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Scénario des données simulées de l'onglet Pawrise, tant que le collier n'envoie pas de vraies mesures.
 * Choisi dans Paramètres → Données simulées, lu par la homepage Santé.
 */
enum class HealthScenario(val label: String) {
    Normal("Normal"),
    Anomaly("Anomalie"),
}

/** Scénario courant, partagé entre Paramètres (écriture) et l'onglet Santé (lecture). */
interface HealthScenarioStore {
    val scenario: StateFlow<HealthScenario>

    fun select(scenario: HealthScenario)
}

/** Implémentation en mémoire : le choix vaut pour la durée de vie de l'app (comme sur iOS). */
@Singleton
class InMemoryHealthScenarioStore @Inject constructor() : HealthScenarioStore {
    private val state = MutableStateFlow(HealthScenario.Normal)
    override val scenario: StateFlow<HealthScenario> = state.asStateFlow()

    override fun select(scenario: HealthScenario) {
        state.value = scenario
    }
}

/** Module séparé de `PetBindings` : les tests qui remplacent le dépôt des chiens gardent le vrai scénario. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HealthScenarioBindings {
    @Binds
    abstract fun healthScenarioStore(impl: InMemoryHealthScenarioStore): HealthScenarioStore
}
