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

// Copie locale provisoire (équipe Accueil) : l'équipe Compte fournit ce contrat dans core:pets.
// À la fusion, supprimer ce fichier de feature:home et garder la version de core:pets.

/** Scénario de données simulées affiché dans Santé (bascule dans Paramètres › Données simulées). */
enum class HealthScenario(val label: String) {
    Normal("Normal"),
    Anomaly("Anomalie"),
}

/** Scénario courant, partagé entre Paramètres (écriture) et Santé (lecture). */
interface HealthScenarioStore {
    val scenario: StateFlow<HealthScenario>
    fun set(scenario: HealthScenario)
}

@Singleton
class InMemoryHealthScenarioStore @Inject constructor() : HealthScenarioStore {
    private val state = MutableStateFlow(HealthScenario.Normal)
    override val scenario: StateFlow<HealthScenario> = state.asStateFlow()
    override fun set(scenario: HealthScenario) {
        state.value = scenario
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthScenarioBindings {
    @Binds
    abstract fun bindHealthScenarioStore(store: InMemoryHealthScenarioStore): HealthScenarioStore
}
