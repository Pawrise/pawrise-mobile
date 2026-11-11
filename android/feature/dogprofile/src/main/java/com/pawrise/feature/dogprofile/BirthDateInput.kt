package com.pawrise.feature.dogprofile

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.time.DateTimeException
import java.time.LocalDate

/** Saisie « JJ / MM / AAAA » : on ne garde que 8 chiffres, les séparateurs sont affichés. */
internal fun birthDateDigits(input: String): String = input.filter(Char::isDigit).take(8)

/** `null` si vide ; [BirthDate.Invalid] si incomplète, inexistante ou dans le futur. */
internal sealed interface BirthDate {
    data object Empty : BirthDate
    data object Invalid : BirthDate
    data class Valid(val date: LocalDate) : BirthDate
}

internal fun parseBirthDate(digits: String, today: LocalDate = LocalDate.now()): BirthDate {
    if (digits.isEmpty()) return BirthDate.Empty
    if (digits.length != 8) return BirthDate.Invalid
    val date = try {
        LocalDate.of(digits.substring(4, 8).toInt(), digits.substring(2, 4).toInt(), digits.substring(0, 2).toInt())
    } catch (e: DateTimeException) {
        return BirthDate.Invalid
    }
    return if (date.isAfter(today) || date.year < 1980) BirthDate.Invalid else BirthDate.Valid(date)
}

internal fun LocalDate.toDigits(): String = "%02d%02d%04d".format(dayOfMonth, monthValue, year)

/** Affiche « 12 / 04 / 2023 » à partir de « 12042023 ». */
internal object BirthDateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 2 || i == 4) append(" / ")
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + 3 * listOf(2, 4).count { offset > it }
            override fun transformedToOriginal(offset: Int) = when {
                offset <= 2 -> offset
                offset <= 7 -> (offset - 3).coerceIn(2, 4)
                else -> (offset - 6).coerceAtMost(digits.length)
            }.coerceAtMost(digits.length)
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/** « 28 », « 27,5 », « 27.5 » → nombre > 0 ; vide → [Measurement.Empty]. */
internal sealed interface Measurement {
    data object Empty : Measurement
    data object Invalid : Measurement
    data class Valid(val value: Double) : Measurement
}

internal fun parseMeasurement(text: String): Measurement {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return Measurement.Empty
    val value = t.toDoubleOrNull() ?: return Measurement.Invalid
    return if (value > 0 && value.isFinite()) Measurement.Valid(value) else Measurement.Invalid
}

/** Saisie décimale : chiffres et un seul séparateur. */
internal fun decimalInput(text: String): String {
    val cleaned = text.filter { it.isDigit() || it == ',' || it == '.' }
    val first = cleaned.indexOfFirst { it == ',' || it == '.' }
    return if (first < 0) cleaned.take(6) else (cleaned.take(first + 1) + cleaned.drop(first + 1).filter(Char::isDigit)).take(6)
}
