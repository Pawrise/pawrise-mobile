package com.pawrise.feature.home.data

/** État d'une mesure ou du score (pastilles « Tout va bien », « À surveiller », « Alerte »). */
enum class HealthState(val label: String) {
    Good("Tout va bien"),
    Watch("À surveiller"),
    Alert("Alerte"),
    ;

    companion object {
        /** Seuils du prototype pour le score de bien-être. */
        fun forScore(score: Int): HealthState = when {
            score >= 80 -> Good
            score >= 55 -> Watch
            else -> Alert
        }
    }
}

enum class HealthPeriod(val label: String) {
    Today("Aujourd'hui"),
    Week("7 jours"),
    Month("30 jours"),
}

data class ChartPoint(val label: String, val value: Double)

enum class IndicatorKind { Heart, Afib, Activity, Sleep, Temperature }

data class HealthIndicator(
    val kind: IndicatorKind,
    val label: String,
    val unit: String,
    val hasPeriods: Boolean,
    /** Plage habituelle affichée en fond du graphique. */
    val normalBand: ClosedFloatingPointRange<Double>?,
    val state: HealthState,
    /** Valeur courte affichée sur la tuile : « 78 bpm », « Rythme régulier ». */
    val headline: String,
    /** Phrase simple, sans vocabulaire médical. */
    val message: String,
    val detail: String,
    val series: Map<HealthPeriod, List<ChartPoint>>,
    /**
     * État de la pastille du panneau quand il diffère de celui de la tuile
     * (maquette US-B · alerte : température en alerte, pastille « À surveiller »).
     */
    val badgeState: HealthState? = null,
    /** Le panneau propose « Demander à Pawrise » (maquette US-B · alerte). */
    val offersAssistant: Boolean = false,
) {
    val pillState: HealthState get() = badgeState ?: state
}

/** Ce que l'écran Santé affiche pour un chien à un instant donné. */
data class HealthSnapshot(
    val score: Int,
    val summary: String,
    val indicators: List<HealthIndicator>,
    val batteryPercent: Int,
) {
    val state: HealthState get() = HealthState.forScore(score)
    val hasAnomaly: Boolean get() = indicators.any { it.state != HealthState.Good }
}

/** Seuils de la batterie du collier (tuile et panneau Collier). */
fun batteryState(percent: Int): HealthState = when {
    percent >= 20 -> HealthState.Good
    percent >= 10 -> HealthState.Watch
    else -> HealthState.Alert
}
