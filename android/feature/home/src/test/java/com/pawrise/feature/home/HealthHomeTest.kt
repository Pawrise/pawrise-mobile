package com.pawrise.feature.home

import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.pets.scenario.HealthScenario
import com.pawrise.core.pets.scenario.InMemoryHealthScenarioStore
import com.pawrise.core.testing.FakePetRepository
import com.pawrise.feature.home.data.HealthPeriod
import com.pawrise.feature.home.data.HealthState
import com.pawrise.feature.home.data.IndicatorKind
import com.pawrise.feature.home.data.SeedHealthRepository
import com.pawrise.feature.home.data.batteryState
import com.pawrise.feature.home.ui.ageLabel
import com.pawrise.feature.home.ui.subtitle
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthHomeTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val nala = FakePetRepository.NALA

    @Test
    fun `données normales de la maquette`() = runTest {
        val snapshot = SeedHealthRepository().snapshot(nala, HealthScenario.Normal)
        assertEquals(87, snapshot.score)
        assertEquals(HealthState.Good, snapshot.state)
        assertFalse(snapshot.hasAnomaly)
        assertEquals("Tout semble normal. Rien à signaler pour le moment.", snapshot.summary)
        assertEquals(
            listOf("78 bpm", "Rythme régulier", "94 min actives", "11 h de sommeil", "38,5 °C"),
            snapshot.indicators.map { it.headline },
        )
        val heart = snapshot.indicators.first { it.kind == IndicatorKind.Heart }
        assertEquals("Le rythme cardiaque de Nala est dans ses valeurs habituelles.", heart.message)
        assertEquals(listOf("6h", "15h", "23h"), heart.series.getValue(HealthPeriod.Today).let { listOf(it.first(), it[it.size / 2], it.last()).map { p -> p.label } })
        assertFalse(heart.offersAssistant)
    }

    @Test
    fun `données en anomalie de la maquette`() = runTest {
        val snapshot = SeedHealthRepository().snapshot(nala, HealthScenario.Anomaly)
        assertEquals(64, snapshot.score)
        assertEquals(HealthState.Watch, snapshot.state)
        assertTrue(snapshot.hasAnomaly)
        assertEquals(
            listOf(HealthState.Watch, HealthState.Alert, HealthState.Watch, HealthState.Watch, HealthState.Alert),
            snapshot.indicators.map { it.state },
        )
        val temperature = snapshot.indicators.first { it.kind == IndicatorKind.Temperature }
        assertEquals(HealthState.Watch, temperature.pillState)
        assertEquals("22.5h", temperature.series.getValue(HealthPeriod.Today).last().label)
        assertTrue(snapshot.indicators.filter { it.kind != IndicatorKind.Afib }.all { it.offersAssistant })
    }

    @Test
    fun `seuils du score et de la batterie`() {
        assertEquals(HealthState.Good, HealthState.forScore(80))
        assertEquals(HealthState.Watch, HealthState.forScore(55))
        assertEquals(HealthState.Alert, HealthState.forScore(54))
        assertEquals(HealthState.Good, batteryState(20))
        assertEquals(HealthState.Watch, batteryState(10))
        assertEquals(HealthState.Alert, batteryState(9))
    }

    @Test
    fun `sous-titre race et âge`() {
        val today = LocalDate.of(2026, 11, 13)
        val pet = nala.copy(breeds = FakePetRepository.BREEDS.filter { it.code == "golden_retriever" }, birthDate = LocalDate.of(2023, 4, 12))
        assertEquals("Golden Retriever · 3 ans", pet.subtitle(today))
        assertEquals("8 mois", pet.copy(birthDate = today.minusMonths(8)).ageLabel(today))
        assertEquals("1 an", pet.copy(birthDate = today.minusMonths(13)).ageLabel(today))
        assertEquals("", pet.copy(breeds = emptyList(), birthDate = null).subtitle(today))
    }

    @Test
    fun `scénario, anomalie, bascule de chien`() = runTest {
        val luna = nala.copy(id = "pet-luna", name = "Luna")
        val store = InMemoryHealthScenarioStore()
        val viewModel = HealthHomeViewModel(FakePetRepository(listOf(nala, luna)), SeedHealthRepository(), store)

        assertEquals("Nala", viewModel.state.value.selectedPet?.name)
        assertEquals(87, viewModel.state.value.snapshot?.score)
        assertTrue(viewModel.state.value.canSwitchDog)

        store.select(HealthScenario.Anomaly)
        assertEquals(64, viewModel.state.value.snapshot?.score)

        viewModel.openFirstAnomaly()
        assertEquals(HealthSheet.Indicator(IndicatorKind.Heart), viewModel.state.value.sheet)
        assertEquals(HealthState.Watch, viewModel.state.value.focusedState)
        viewModel.open(HealthSheet.Indicator(IndicatorKind.Afib))
        assertEquals(HealthState.Alert, viewModel.state.value.focusedState)

        viewModel.selectNextDog()
        assertEquals("Luna", viewModel.selectedPet.value?.name)
        assertNull(viewModel.state.value.sheet)
        assertTrue(viewModel.state.value.snapshot!!.indicators.first().message.contains("Luna"))
    }

    @Test
    fun `erreur de chargement des chiens puis nouvel essai`() = runTest {
        val pets = FakePetRepository().apply { listResult = ApiResult.Failure(ApiError.Network(IOException())) }
        val viewModel = HealthHomeViewModel(pets, SeedHealthRepository(), InMemoryHealthScenarioStore())
        assertEquals("Impossible de charger vos chiens. Vérifiez votre connexion et réessayez.", viewModel.state.value.petsError)

        pets.listResult = null
        viewModel.loadPets()
        assertNull(viewModel.state.value.petsError)
        assertEquals("Nala", viewModel.selectedPet.value?.name)
    }
}
