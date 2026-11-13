package com.pawrise.feature.auth

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Téléphone (libphonenumber, région FR par défaut) : validation, E.164, mise en forme pendant la saisie. */
class PhoneNumberTest {
    @Test
    fun `formats francais et internationaux en E164`() {
        assertEquals("+33612345678", toE164("06 12 34 56 78"))
        assertEquals("+33612345678", toE164("06.12.34.56.78"))
        assertEquals("+33612345678", toE164("0612345678"))
        assertEquals("+33612345678", toE164("+33 6 12 34 56 78"))
        assertEquals("+33612345678", toE164("0033612345678"))
        assertEquals("+33123456789", toE164("01 23 45 67 89"))
        assertEquals("+3225123456", toE164("+32 2 512 34 56"))
        assertEquals("+3225123456", toE164("0032 2 512 34 56"))
        assertEquals("+14155552671", toE164("+1 415 555 2671"))
    }

    @Test
    fun `numeros invalides`() {
        assertNull(toE164(""))
        assertNull(toE164("+"))
        assertNull(toE164("12345"))
        assertNull(toE164("06 12 34"))
        assertNull(toE164("06 12 34 56 78 9"))
        assertNull(toE164("abc"))
        assertNull(toE164("+0612345678"))
        assertNull(toE164("+33 0 12 34 56 78 90"))
    }

    @Test
    fun `saisie nettoyee - chiffres et plus initial seulement`() {
        assertEquals("0612345678", PhoneNumbers.sanitize("06.12 34-56 78"))
        assertEquals("+3225123456", PhoneNumbers.sanitize(" +32 (2) 512 34 56"))
        assertEquals("0612", PhoneNumbers.sanitize("06+12"))
        assertEquals("", PhoneNumbers.sanitize("abc"))
    }

    @Test
    fun `mise en forme pendant la saisie`() {
        assertEquals("06", PhoneNumbers.formatAsYouType("06"))
        assertEquals("06 12 34 56 78", PhoneNumbers.formatAsYouType("0612345678"))
        assertEquals("+33 6 12 34 56 78", PhoneNumbers.formatAsYouType("+33612345678"))
        assertEquals("+32 2 512 34 56", PhoneNumbers.formatAsYouType("+3225123456"))
    }

    @Test
    fun `le curseur suit les chiffres malgre les espaces ajoutes`() {
        val transformed = PhoneVisualTransformation.filter(AnnotatedString("0612345678"))
        assertEquals("06 12 34 56 78", transformed.text.text)
        val map = transformed.offsetMapping
        assertEquals(0, map.originalToTransformed(0))
        assertEquals(2, map.originalToTransformed(2)) // après « 06 »
        assertEquals(4, map.originalToTransformed(3)) // après « 06 1 »
        assertEquals(14, map.originalToTransformed(10))
        assertEquals(2, map.transformedToOriginal(3)) // sur l'espace : avant le « 1 »
        assertEquals(10, map.transformedToOriginal(14))
        // Aucun caractère d'origine ne se perd ni ne s'invente.
        for (offset in 0..10) assertEquals(offset, map.transformedToOriginal(map.originalToTransformed(offset)))
    }
}
