package com.pawrise.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberTest {
    @Test
    fun `formats francais et internationaux`() {
        assertEquals("+33612345678", toE164("06 12 34 56 78"))
        assertEquals("+33612345678", toE164("06.12.34.56.78"))
        assertEquals("+33612345678", toE164("+33 6 12 34 56 78"))
        assertEquals("+33612345678", toE164("0033612345678"))
        assertEquals("+3225123456", toE164("+32 2 512 34 56"))
    }

    @Test
    fun `numeros invalides`() {
        assertNull(toE164("12345"))
        assertNull(toE164("06 12 34"))
        assertNull(toE164("abc"))
        assertNull(toE164("+0612345678"))
    }
}
