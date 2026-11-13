package com.pawrise.feature.auth

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * Téléphone de l'inscription (facultatif), avec libphonenumber : région par défaut FR, numéros internationaux
 * acceptés (« +32 … », « 0032 … »), envoi au format E.164 attendu par pawrise-auth (`INVALID_PHONE` sinon).
 */
internal object PhoneNumbers {
    const val DEFAULT_REGION = "FR"

    /** Longueur maximale saisie : « + » et 15 chiffres (E.164), ou un préfixe « 00 » et 15 chiffres. */
    private const val MAX_LENGTH = 17

    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    /** Ne garde que ce qui compose un numéro : chiffres et un « + » initial (collage de « 06.12.34… » compris). */
    fun sanitize(input: String): String {
        val plus = input.trimStart().startsWith("+")
        val digits = input.filter { it in '0'..'9' }
        return ((if (plus) "+" else "") + digits).take(MAX_LENGTH)
    }

    /** Numéro saisi → E.164 (`+33612345678`), ou `null` s'il n'est pas valide. */
    fun toE164(input: String): String? {
        val raw = sanitize(input)
        if (raw.isEmpty() || raw == "+") return null
        return try {
            val number = util.parse(raw, DEFAULT_REGION)
            if (util.isValidNumber(number)) util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164) else null
        } catch (_: NumberParseException) {
            null
        }
    }

    /** Affichage pendant la saisie : « 06 12 34 56 78 », « +32 2 512 34 56 ». */
    fun formatAsYouType(input: String): String {
        val formatter = util.getAsYouTypeFormatter(DEFAULT_REGION)
        var out = ""
        sanitize(input).forEach { out = formatter.inputDigit(it) }
        return out
    }
}

/** Numéro saisi → format E.164 attendu par l'API, `null` s'il est invalide (saisie vide gérée par l'appelant). */
internal fun toE164(input: String): String? = PhoneNumbers.toE164(input)

/**
 * Mise en forme du téléphone pendant la saisie : l'état garde les caractères saisis (« 0612345678 »), le champ
 * affiche « 06 12 34 56 78 » et le curseur suit les chiffres (les espaces ajoutés sont sautés).
 */
internal object PhoneVisualTransformation : VisualTransformation {
    private fun Char.isDialable() = this in '0'..'9' || this == '+'

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = PhoneNumbers.formatAsYouType(raw)
        // Le formateur n'ajoute que des séparateurs ; sinon (saisie inattendue), le texte est affiché tel quel.
        if (formatted.filter { it.isDialable() } != raw) return TransformedText(text, OffsetMapping.Identity)
        // Position dans le texte affiché juste après le i-ème caractère saisi.
        val toFormatted = IntArray(raw.length + 1)
        var seen = 0
        formatted.forEachIndexed { index, c ->
            if (c.isDialable()) {
                seen++
                toFormatted[seen] = index + 1
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = toFormatted[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset.coerceIn(0, formatted.length)).count { it.isDialable() }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
