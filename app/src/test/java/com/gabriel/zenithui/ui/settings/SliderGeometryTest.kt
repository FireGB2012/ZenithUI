package com.gabriel.zenithui.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SliderGeometryTest {

    // A track 300 across with a knob 28 across: the knob's middle travels from 14 to 286.
    private val width = 300f
    private val knob = 28f

    @Test
    fun `the knob's middle stays a knob's radius inside each end`() {
        assertEquals(14f, SliderGeometry.knobCentre(0f, width, knob), 0.001f)
        assertEquals(286f, SliderGeometry.knobCentre(1f, width, knob), 0.001f)
        assertEquals(14f, SliderGeometry.knobCentre(-0.5f, width, knob), 0.001f)
        assertEquals(286f, SliderGeometry.knobCentre(1.5f, width, knob), 0.001f)
    }

    @Test
    fun `a value puts the knob its share of the way along`() {
        assertEquals(150f, SliderGeometry.knobCentre(0.5f, width, knob), 0.001f)
        assertEquals(14f + 272f * 0.25f, SliderGeometry.knobCentre(0.25f, width, knob), 0.001f)
        assertEquals(0.25f, SliderGeometry.fraction(value = 0.45f, range = 0.2f..1.2f), 0.001f)
        assertEquals(0.45f, SliderGeometry.value(fraction = 0.25f, range = 0.2f..1.2f), 0.001f)
        assertEquals(1f, SliderGeometry.fraction(value = 5f, range = 0f..1f), 0f)
        assertEquals(0f, SliderGeometry.fraction(value = -5f, range = 0f..1f), 0f)
    }

    @Test
    fun `a touch picks the value under it, and the end value past either end`() {
        assertEquals(0.5f, SliderGeometry.fractionAt(150f, width, knob), 0.001f)
        assertEquals(0f, SliderGeometry.fractionAt(14f, width, knob), 0.001f)
        assertEquals(1f, SliderGeometry.fractionAt(286f, width, knob), 0.001f)
        assertEquals(0f, SliderGeometry.fractionAt(-50f, width, knob), 0f)
        assertEquals(1f, SliderGeometry.fractionAt(400f, width, knob), 0f)
    }

    @Test
    fun `where the knob is drawn and where a finger picks it are the same place`() {
        for (share in listOf(0f, 0.1f, 0.37f, 0.5f, 0.82f, 1f)) {
            val at = SliderGeometry.knobCentre(share, width, knob)
            assertEquals(share, SliderGeometry.fractionAt(at, width, knob), 0.0001f)
        }
    }

    @Test
    fun `a track no wider than its knob, or a range of nothing, divides by nothing`() {
        assertEquals(0f, SliderGeometry.fractionAt(10f, width = 20f, knob = 28f), 0f)
        assertEquals(10f, SliderGeometry.knobCentre(0.7f, width = 20f, knob = 28f), 0.001f)
        assertEquals(0f, SliderGeometry.fraction(value = 3f, range = 3f..3f), 0f)
        assertEquals(3f, SliderGeometry.value(fraction = 0.6f, range = 3f..3f), 0f)
    }
}
