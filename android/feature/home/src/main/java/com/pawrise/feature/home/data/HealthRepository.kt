package com.pawrise.feature.home.data

import com.pawrise.core.pets.Pet
import com.pawrise.core.pets.scenario.HealthScenario
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.sin

/**
 * Source des mesures du collier. Aujourd'hui : données simulées.
 * Plus tard : Core API (séries alimentées par l'ingestion du collier).
 */
interface HealthRepository {
    suspend fun snapshot(pet: Pet, scenario: HealthScenario): HealthSnapshot
}

/**
 * Données reprises du prototype (pawrise-figma/src/data/health.ts), identiques à l'app iOS
 * (`Data/HealthRepository.swift`) : 5 indicateurs, chacun en version normale et anomalie,
 * avec des séries Aujourd'hui / 7 jours / 30 jours.
 */
class SeedHealthRepository @Inject constructor() : HealthRepository {

    override suspend fun snapshot(pet: Pet, scenario: HealthScenario): HealthSnapshot {
        val anomaly = scenario == HealthScenario.Anomaly
        val name = pet.name
        return HealthSnapshot(
            score = if (anomaly) 64 else 87,
            summary = if (anomaly) {
                "Des indicateurs méritent votre attention aujourd'hui."
            } else {
                "Tout semble normal. Rien à signaler pour le moment."
            },
            indicators = listOf(
                heart(name, anomaly),
                afib(name, anomaly),
                activity(name, anomaly),
                sleep(name, anomaly),
                temperature(name, anomaly),
            ),
            batteryPercent = DEFAULT_BATTERY_PERCENT,
        )
    }

    private fun heart(name: String, anomaly: Boolean) = HealthIndicator(
        kind = IndicatorKind.Heart,
        label = "Rythme cardiaque",
        unit = "bpm",
        hasPeriods = true,
        normalBand = 60.0..100.0,
        state = if (anomaly) HealthState.Watch else HealthState.Good,
        headline = if (anomaly) "128 bpm" else "78 bpm",
        message = if (anomaly) {
            "Le rythme cardiaque de $name est inhabituellement élevé depuis 45 minutes."
        } else {
            "Le rythme cardiaque de $name est dans ses valeurs habituelles."
        },
        // Textes de la maquette (US-B · alerte, écran 02).
        detail = if (anomaly) {
            "Moyenne de 128 bpm sur la journée, avec des variations normales pendant le repos et l'activité."
        } else {
            "Moyenne de 78 bpm sur la journée, avec des variations normales pendant le repos et l'activité."
        },
        series = if (anomaly) {
            mapOf(
                HealthPeriod.Today to points(
                    hours(6.0, 18),
                    listOf(76, 78, 74, 80, 79, 77, 82, 81, 79, 84, 88, 96, 108, 118, 124, 128, 127, 126).d(),
                ),
                HealthPeriod.Week to points(DAYS_7, listOf(77, 79, 78, 80, 82, 90, 112).d()),
                HealthPeriod.Month to points(DAYS_30, wave(80.0, 6.0, 30, seed = 1.0, drift = 0.9)),
            )
        } else {
            mapOf(
                HealthPeriod.Today to points(hours(6.0, 18), wave(78.0, 9.0, 18, seed = 2.0)),
                HealthPeriod.Week to points(DAYS_7, wave(78.0, 5.0, 7, seed = 3.0)),
                HealthPeriod.Month to points(DAYS_30, wave(78.0, 6.0, 30, seed = 1.0)),
            )
        },
        offersAssistant = anomaly,
    )

    private fun afib(name: String, anomaly: Boolean): HealthIndicator {
        // 0 = battement régulier, 1 = intervalle irrégulier détecté
        val irregular = listOf(0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0).d()
        val hours24 = (0 until 24).map { "$it" }
        return HealthIndicator(
            kind = IndicatorKind.Afib,
            label = "Fibrillation atriale",
            unit = "",
            hasPeriods = false,
            normalBand = null,
            state = if (anomaly) HealthState.Alert else HealthState.Good,
            headline = if (anomaly) "Rythme irrégulier" else "Rythme régulier",
            message = if (anomaly) {
                "Des battements irréguliers ont été détectés chez $name."
            } else {
                "Aucun signe de rythme irrégulier détecté chez $name."
            },
            detail = if (anomaly) {
                "Plusieurs intervalles irréguliers repérés sur la dernière heure. À faire vérifier par un vétérinaire."
            } else {
                "Le collier analyse le rythme en continu. Les intervalles entre battements sont réguliers."
            },
            series = mapOf(
                HealthPeriod.Today to points(hours24, if (anomaly) irregular else List(24) { 0.0 }),
                HealthPeriod.Week to points(DAYS_7, if (anomaly) listOf(0, 0, 0, 0, 1, 0, 1).d() else List(7) { 0.0 }),
                HealthPeriod.Month to points(DAYS_30, (0 until 30).map { if (anomaly && it > 24) 1.0 else 0.0 }),
            ),
        )
    }

    private fun activity(name: String, anomaly: Boolean) = HealthIndicator(
        kind = IndicatorKind.Activity,
        label = "Activité",
        unit = "min",
        hasPeriods = true,
        normalBand = null,
        state = if (anomaly) HealthState.Watch else HealthState.Good,
        headline = if (anomaly) "31 min actives" else "94 min actives",
        message = if (anomaly) {
            "$name bouge nettement moins que d'habitude aujourd'hui."
        } else {
            "$name a bougé autant que d'habitude aujourd'hui."
        },
        detail = if (anomaly) {
            "Seulement 31 minutes actives, contre 95 min en moyenne. À surveiller."
        } else {
            "94 minutes actives, réparties sur ses trois promenades."
        },
        series = if (anomaly) {
            mapOf(
                HealthPeriod.Today to points(hours(6.0, 12, step = 1.5), listOf(2, 6, 4, 3, 2, 5, 3, 1, 1, 2, 1, 1).d()),
                HealthPeriod.Week to points(DAYS_7, listOf(92, 98, 88, 96, 90, 70, 31).d()),
                HealthPeriod.Month to points(DAYS_30, wave(90.0, 18.0, 30, seed = 5.0, drift = -1.2)),
            )
        } else {
            mapOf(
                HealthPeriod.Today to points(hours(6.0, 12, step = 1.5), listOf(4, 22, 12, 8, 6, 18, 10, 6, 4, 14, 20, 8).d()),
                HealthPeriod.Week to points(DAYS_7, listOf(88, 102, 76, 94, 110, 130, 96).d()),
                HealthPeriod.Month to points(DAYS_30, wave(95.0, 20.0, 30, seed = 5.0)),
            )
        },
        offersAssistant = anomaly,
    )

    private fun sleep(name: String, anomaly: Boolean) = HealthIndicator(
        kind = IndicatorKind.Sleep,
        label = "Sommeil",
        unit = "h",
        hasPeriods = true,
        normalBand = null,
        state = if (anomaly) HealthState.Watch else HealthState.Good,
        headline = if (anomaly) "7 h · agité" else "11 h de sommeil",
        message = if (anomaly) "Le sommeil de $name a été agité cette nuit." else "$name a bien dormi cette nuit.",
        // Texte et répartition de la maquette (US-B · alerte, écran 05).
        detail = if (anomaly) {
            "7 h · agité, dont 6 h de sommeil profond. Peu de réveils."
        } else {
            "11 h de sommeil, dont 6 h de sommeil profond. Peu de réveils."
        },
        series = mapOf(
            // « Aujourd'hui » = répartition des phases de la nuit
            HealthPeriod.Today to points(listOf("Profond", "Léger", "Éveillé"), listOf(6.0, 5.0, 1.0)),
            HealthPeriod.Week to points(
                DAYS_7,
                (if (anomaly) listOf(11, 10, 11, 10, 9, 8, 7) else listOf(11, 10, 12, 11, 10, 11, 11)).d(),
            ),
            HealthPeriod.Month to points(
                DAYS_30,
                if (anomaly) wave(10.0, 2.0, 30, seed = 2.0, drift = -0.05) else wave(11.0, 1.0, 30, seed = 2.0),
            ),
        ),
        offersAssistant = anomaly,
    )

    private fun temperature(name: String, anomaly: Boolean): HealthIndicator {
        fun tenth(values: List<Double>) = values.map { it / 10 }
        return HealthIndicator(
            kind = IndicatorKind.Temperature,
            label = "Température",
            unit = "°C",
            hasPeriods = true,
            normalBand = 38.0..39.2,
            state = if (anomaly) HealthState.Alert else HealthState.Good,
            headline = if (anomaly) "39,8 °C" else "38,5 °C",
            message = if (anomaly) "La température de $name est plus élevée que la normale." else "La température de $name est normale.",
            // Texte de la maquette (US-B · alerte, écran 06).
            detail = if (anomaly) "Stable autour de 39,8 °C sur la journée." else "Stable autour de 38,5 °C sur la journée.",
            series = if (anomaly) {
                mapOf(
                    HealthPeriod.Today to points(
                        hours(6.0, 12, step = 1.5),
                        tenth(listOf(385, 386, 387, 388, 389, 391, 393, 395, 396, 397, 398, 398).d()),
                    ),
                    HealthPeriod.Week to points(DAYS_7, tenth(listOf(385, 386, 385, 387, 388, 391, 398).d())),
                    HealthPeriod.Month to points(DAYS_30, tenth(wave(386.0, 3.0, 30, seed = 1.0, drift = 0.3))),
                )
            } else {
                mapOf(
                    HealthPeriod.Today to points(
                        hours(6.0, 12, step = 1.5),
                        tenth(listOf(385, 384, 386, 385, 387, 386, 385, 384, 386, 385, 387, 386).d()),
                    ),
                    HealthPeriod.Week to points(DAYS_7, tenth(listOf(385, 386, 384, 385, 387, 386, 385).d())),
                    HealthPeriod.Month to points(DAYS_30, tenth(wave(385.0, 3.0, 30, seed = 1.0))),
                )
            },
            // Maquette : tuile en alerte, pastille « À surveiller » dans le panneau.
            badgeState = if (anomaly) HealthState.Watch else null,
            offersAssistant = anomaly,
        )
    }

    companion object {
        /** Batterie affichée tant que le collier ne remonte pas sa charge (iOS : 100 %). */
        const val DEFAULT_BATTERY_PERCENT = 100

        val DAYS_7 = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        val DAYS_30 = (1..30).map { "$it" }

        fun hours(from: Double, count: Int, step: Double = 1.0): List<String> = (0 until count).map { index ->
            val hour = (from + index * step) % 24
            if (hour == roundHalfAway(hour)) "${hour.toInt()}h" else String.format(Locale.US, "%.1fh", hour)
        }

        /** Même générateur que le prototype, pour des courbes crédibles et reproductibles. */
        fun wave(base: Double, amplitude: Double, count: Int, seed: Double, drift: Double = 0.0): List<Double> =
            (0 until count).map { index ->
                val i = index.toDouble()
                roundHalfAway(base + sin((i + seed) * 0.7) * amplitude + cos((i + seed) * 0.31) * amplitude * 0.5 + drift * i)
            }

        /** Arrondi de Swift (`rounded()`) : au plus proche, demi-valeurs loin de zéro. */
        private fun roundHalfAway(value: Double): Double = sign(value) * floor(abs(value) + 0.5)

        private fun points(labels: List<String>, values: List<Double>): List<ChartPoint> =
            labels.mapIndexed { index, label -> ChartPoint(label, values.getOrElse(index) { values.lastOrNull() ?: 0.0 }) }

        private fun List<Int>.d(): List<Double> = map { it.toDouble() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthBindings {
    @Binds
    abstract fun bindHealthRepository(repository: SeedHealthRepository): HealthRepository
}
