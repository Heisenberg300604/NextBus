package com.ncs.nextbus.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun `strips formatting and keeps a leading plus`() {
        assertEquals("9876543210", PhoneNumbers.normalize(" 98765 43210 "))
        assertEquals("+919876543210", PhoneNumbers.normalize("+91 (98765)-432.10"))
    }

    @Test
    fun `rejects short, long, empty and non-numeric input`() {
        assertNull(PhoneNumbers.normalize("12345"))
        assertNull(PhoneNumbers.normalize("1234567890123456"))
        assertNull(PhoneNumbers.normalize(""))
        assertNull(PhoneNumbers.normalize("98765abc10"))
        assertNull(PhoneNumbers.normalize("+"))
    }
}
