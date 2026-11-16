package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToggleStatesTest {

    @Test
    fun `wifi is on when Android keeps it on, airplane mode or not`() {
        assertEquals(false, ToggleStates.wifi(0))
        assertEquals(true, ToggleStates.wifi(1))
        // Left on through airplane mode.
        assertEquals(true, ToggleStates.wifi(2))
        // Off because airplane mode turned it off.
        assertEquals(false, ToggleStates.wifi(3))
        assertNull(ToggleStates.wifi(null))
        assertNull(ToggleStates.wifi(7))
    }

    @Test
    fun `bluetooth is on when Android keeps it on, airplane mode or not`() {
        assertEquals(false, ToggleStates.bluetooth(0))
        assertEquals(true, ToggleStates.bluetooth(1))
        assertEquals(true, ToggleStates.bluetooth(2))
        assertNull(ToggleStates.bluetooth(null))
    }

    @Test
    fun `a switch kept as a number is on for anything but nought`() {
        assertEquals(false, ToggleStates.flag(0))
        assertEquals(true, ToggleStates.flag(1))
        assertNull(ToggleStates.flag(null))
    }

    @Test
    fun `do not disturb is on whenever anything is held back`() {
        // INTERRUPTION_FILTER_ALL lets everything through: it is off.
        assertEquals(false, ToggleStates.focus(1))
        // Priority only, nothing, alarms only.
        assertEquals(true, ToggleStates.focus(2))
        assertEquals(true, ToggleStates.focus(3))
        assertEquals(true, ToggleStates.focus(4))
        // Unknown: no word either way.
        assertNull(ToggleStates.focus(0))
    }

    @Test
    fun `the ringer says how the phone will ring`() {
        assertEquals("Ring", ToggleStates.ringer(2))
        assertEquals("Vibrate", ToggleStates.ringer(1))
        assertEquals("Silent", ToggleStates.ringer(0))
        assertNull(ToggleStates.ringer(-1))
    }

    @Test
    fun `a state says On or Off, or nothing when it is not known`() {
        assertEquals("On", ToggleState.of(true).words)
        assertEquals("Off", ToggleState.of(false).words)
        assertNull(ToggleState.of(null).words)
        assertEquals("Vibrate", ToggleState(on = null, note = "Vibrate").words)
    }
}
