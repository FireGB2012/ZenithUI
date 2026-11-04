package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.gabriel.zenithui.data.GlassBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassLiftTest {

    private val tint = Palettes.Frost.glassTint

    private fun look(body: GlassBody, dark: Boolean = true, material: GlassMaterial = GlassMaterial.Regular, solid: Float = 0f) =
        glassLook(material, strength = 0.8f, solid = solid, opacity = 1f, dark = dark, tint = tint, depth = body)

    @Test
    fun `clear glass is the glass with nothing added`() {
        for (dark in listOf(true, false)) {
            for (material in GlassMaterial.entries) {
                val clear = look(GlassBody.Clear, dark, material)
                assertEquals(0f, clear.lift.alpha, 0f)
                // And it is what every caller that names no body gets.
                assertEquals(clear, glassLook(material, strength = 0.8f, solid = 0f, opacity = 1f, dark = dark, tint = tint))
            }
        }
    }

    @Test
    fun `frosted glass is lifted, a regular pane more than a clear one`() {
        assertEquals(LIFT_DARK, look(GlassBody.Frosted).lift.alpha, 0.003f)
        assertEquals(LIFT_DARK_CLEAR, look(GlassBody.Frosted, material = GlassMaterial.Clear).lift.alpha, 0.003f)
        assertEquals(LIFT_LIGHT, look(GlassBody.Frosted, dark = false).lift.alpha, 0.003f)
        assertEquals(LIFT_LIGHT_CLEAR, look(GlassBody.Frosted, dark = false, material = GlassMaterial.Clear).lift.alpha, 0.003f)
    }

    @Test
    fun `in the dark the frost is pale, in the light it is a shade`() {
        assertTrue(look(GlassBody.Frosted, dark = true).lift.copy(alpha = 1f).luminance() > 0.6f)
        assertTrue(look(GlassBody.Frosted, dark = false).lift.copy(alpha = 1f).luminance() < 0.3f)
    }

    @Test
    fun `a pane with a full veil of its own needs half the lift`() {
        val sheet = look(GlassBody.Frosted, solid = 1f)
        assertEquals(LIFT_DARK * 0.5f, sheet.lift.alpha, 0.003f)
        assertEquals(LIFT_DARK * 0.5f, glassLift(clear = false, dark = true, body = 3f, lift = 1f).alpha, 0.003f)
    }

    @Test
    fun `solid glass is frosted glass with a fuller veil and a surer rim than clear`() {
        val frosted = look(GlassBody.Frosted)
        val solid = look(GlassBody.Solid)
        assertTrue(solid.wash.alpha > frosted.wash.alpha)
        assertTrue(solid.lift.alpha > 0f)
        assertTrue(frosted.shine > look(GlassBody.Clear).shine)
        // The lift never turns a pane into a painted card.
        for (body in GlassBody.entries) assertTrue(body.name, look(body).lift.alpha <= 0.15f)
    }

    @Test
    fun `a body nobody has heard of is clear`() {
        assertEquals(GlassBody.Clear, GlassBody.byId("crystal"))
        assertEquals(GlassBody.Clear, GlassBody.byId(""))
        GlassBody.entries.forEach { assertEquals(it, GlassBody.byId(it.id)) }
        assertEquals(GlassBody.entries.size, GlassBody.entries.map { it.id }.toSet().size)
        assertEquals(Color.Transparent.alpha, look(GlassBody.Clear).lift.alpha, 0f)
    }
}
