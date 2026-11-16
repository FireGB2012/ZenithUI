package com.gabriel.zenithui.island

import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.NotchFit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandGeometryTest {

    private val phone = IslandGeometry.typical(392.dp)

    @Test
    fun `as it is made, the Notch sits on the camera`() {
        assertEquals(27.dp, phone.height)
        assertEquals(196.dp, phone.centreX)
        assertEquals(phone.holeY - phone.height / 2, phone.top)
    }

    @Test
    fun `taller and wider are shares of its own size`() {
        val fitted = phone.copy(fit = NotchFit(width = 1.2f, height = 1.2f))
        assertEquals(phone.height * 1.2f, fitted.height)
        assertEquals(phone.widest * 1.2f, fitted.widest)
        // Still on the camera: it grows from its middle.
        assertEquals(phone.centreX, fitted.centreX)
        assertEquals(fitted.holeY - fitted.height / 2, fitted.top)
    }

    @Test
    fun `a nudge moves it that many dp from the camera`() {
        val fitted = phone.copy(fit = NotchFit(x = 6f, y = -3f))
        assertEquals(phone.centreX + 6.dp, fitted.centreX)
        assertEquals(phone.top - 3.dp, fitted.top)
    }

    @Test
    fun `whatever it is asked, it never leaves the top of the screen`() {
        val high = phone.copy(holeY = 6.dp, fit = NotchFit(height = 1.35f, y = -6f))
        assertEquals(0.dp, high.top)
        val edge = phone.copy(holeX = 20.dp, fit = NotchFit(x = -12f))
        assertTrue(edge.centreX >= edge.height)
        val wide = IslandGeometry.typical(300.dp).copy(fit = NotchFit(width = 1.4f))
        assertTrue(wide.widest <= wide.width - 24.dp)
    }
}
