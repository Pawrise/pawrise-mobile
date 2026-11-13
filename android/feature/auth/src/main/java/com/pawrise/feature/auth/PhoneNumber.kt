package com.pawrise.feature.auth

/**
 * Numéro saisi → format E.164 attendu par l'API (`+33612345678`).
 * Accepte « 06 12 34 56 78 », « 06.12.34.56.78 », « +33 6 12 34 56 78 ».
 * Renvoie `null` si le numéro est invalide ; une saisie vide est gérée par l'appelant (champ facultatif).
 */
internal fun toE164(input: String): String? {
    val compact = input.replace(Regex("[\\s.\\-()]"), "")
    return when {
        Regex("^0[1-9]\\d{8}$").matches(compact) -> "+33" + compact.drop(1)
        Regex("^\\+[1-9]\\d{7,14}$").matches(compact) -> compact
        Regex("^00[1-9]\\d{7,14}$").matches(compact) -> "+" + compact.drop(2)
        else -> null
    }
}
