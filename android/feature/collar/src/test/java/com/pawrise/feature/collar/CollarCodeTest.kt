package com.pawrise.feature.collar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollarCodeTest {
    @Test
    fun `code seul, en minuscules ou dans une URL`() {
        assertEquals("PW-8A4F2C", extractCollarCode("PW-8A4F2C"))
        assertEquals("PW-8A4F2C", extractCollarCode("pw-8a4f2c"))
        assertEquals("PW-8A4F2C", extractCollarCode("https://pawrise.fr/c/PW-8A4F2C?src=box"))
    }

    @Test
    fun `QR sans code Pawrise`() {
        assertNull(extractCollarCode("https://example.com"))
        assertNull(extractCollarCode("PW-8A4F2"))
        assertNull(extractCollarCode("PW-8A4F2CX"))
    }

    @Test
    fun `saisie manuelle mise en forme`() {
        assertEquals("PW-8A4F2C", collarInput("pw-8a 4f2c"))
        assertEquals("PW-8A4F2C", collarInput("PW-8A4F2C999"))
    }
}
