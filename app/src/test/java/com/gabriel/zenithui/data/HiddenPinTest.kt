package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The PIN on the hidden apps is kept only as a salted hash: not in the settings as typed, and so
 * not in a backup file either, where anybody who opened the file could read it, and where it might
 * be the PIN that opens your phone or your bank.
 */
class HiddenPinTest {

    @Test
    fun `a PIN is kept only as a salted hash, and only the right one opens the list`() {
        val sealed = HiddenPin.seal("2468")
        assertFalse("2468" in sealed)
        assertFalse(HiddenPin.isPlain(sealed))
        assertTrue(HiddenPin.matches("2468", sealed))
        assertFalse(HiddenPin.matches("2469", sealed))
        assertFalse(HiddenPin.matches("", sealed))
        assertFalse(HiddenPin.matches("24680", sealed))
        // Salted: the same PIN twice is kept two ways, so one cannot be looked up from the other.
        assertNotEquals(sealed, HiddenPin.seal("2468"))
    }

    @Test
    fun `a PIN an older Zenith kept as typed still opens the list, and is known to need sealing`() {
        assertTrue(HiddenPin.isPlain("1234"))
        assertTrue(HiddenPin.matches("1234", "1234"))
        assertFalse(HiddenPin.matches("4321", "1234"))
    }

    @Test
    fun `no PIN is no PIN, and a spoilt one opens nothing`() {
        assertEquals("", HiddenPin.seal(""))
        assertFalse(HiddenPin.isPlain(""))
        for (spoilt in listOf("pbkdf2", "pbkdf2:x:y:z", "pbkdf2:100:AAAA", "pbkdf2:-1:AAAA:AAAA", "pbkdf2:99999999999:AAAA:AAAA", "abc")) {
            assertFalse(spoilt, HiddenPin.matches("1234", spoilt))
        }
    }

    @Test
    fun `only the digits of what is typed count, six at most, as when it is set`() {
        val sealed = HiddenPin.seal("12 34")
        assertTrue(HiddenPin.matches("1234", sealed))
        assertTrue(HiddenPin.matches("123456", HiddenPin.seal("12345678")))
    }
}
