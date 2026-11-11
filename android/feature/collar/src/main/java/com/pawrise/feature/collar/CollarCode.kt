package com.pawrise.feature.collar

/**
 * Code collier lu dans un QR. Le contenu exact du QR n'est pas fixé par le contrat : on accepte
 * le code seul (`PW-8A4F2C`) ou un texte / une URL qui le contient. `null` si aucun code Pawrise.
 */
internal fun extractCollarCode(raw: String): String? =
    Regex("PW-[A-Z0-9]{6}(?![A-Z0-9])", RegexOption.IGNORE_CASE).find(raw)?.value?.uppercase()

/** Saisie manuelle : majuscules, sans espaces, 9 caractères max (`PW-` + 6). */
internal fun collarInput(text: String): String = text.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(9)
