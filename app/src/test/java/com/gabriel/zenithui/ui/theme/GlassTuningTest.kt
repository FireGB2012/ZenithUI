package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.gabriel.zenithui.data.GlassBody
import com.gabriel.zenithui.data.GlassTuning
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassTuningTest {

    private fun pane(material: GlassMaterial = GlassMaterial.Clear) =
        glassLook(material, strength = 0.8f, solid = 0f, opacity = 1f, dark = true, tint = Color(0xFF7C6CFF), depth = GlassBody.Clear)

    @Test
    fun `the defaults change nothing at all`() {
        val look = pane()
        assertSame(look, look.tuned(GlassTuning(), Color(0xFF7C6CFF)))
        for (lean in listOf(Offset.Zero, Offset(0.3f, -0.2f), Offset(-1f, 1f))) {
            // Zenith's own light, exactly as it was worked out before there was a studio.
            assertEquals(Offset(-0.45f - 1.6f * lean.x, -1f - 1.2f * lean.y), lightFrom(lean, GlassTuning()))
        }
    }

    @Test
    fun `each slider changes what it says and nothing else`() {
        val look = pane()
        val bent = look.tuned(GlassTuning(bend = 2f), Color.Red)
        assertEquals(look.refraction * 2f, bent.refraction, 1e-6f)
        assertEquals(look.copy(refraction = bent.refraction), bent)
        val frosted = look.tuned(GlassTuning(frost = 1f), Color.Red)
        assertEquals(look.frost + 1f, frosted.frost, 1e-6f)
        val rimless = look.tuned(GlassTuning(rim = 0f), Color.Red)
        assertEquals(0f, rimless.shine, 0f)
        assertEquals(0f, rimless.mirror, 0f)
        val rainbow = look.tuned(GlassTuning(rainbow = 3f), Color.Red)
        assertEquals(look.dispersion * 3f, rainbow.dispersion, 1e-6f)
    }

    @Test
    fun `more theme in the glass colours the veil without thinning it`() {
        val look = pane()
        val tinted = look.tuned(GlassTuning(tint = 0.4f), Color(0xFFFF0000))
        assertEquals(look.wash.alpha, tinted.wash.alpha, 1e-6f)
        assertTrue(tinted.wash.red > look.wash.red)
    }

    @Test
    fun `frost stays within what the shader can draw`() {
        val sheet = glassLook(GlassMaterial.Regular, 1f, solid = 1.5f, opacity = 1f, dark = true, tint = Color.White)
        assertEquals(3f, sheet.tuned(GlassTuning(frost = 1f), Color.White).frost, 0f)
        assertTrue(pane().tuned(GlassTuning(frost = -1f), Color.White).frost >= 0f)
    }

    @Test
    fun `Zenith's light as an angle falls where Zenith's own does`() {
        val own = lightFrom(Offset.Zero, GlassTuning())
        val asAngle = lightFrom(Offset.Zero, GlassTuning(lightAngle = GlassTuning.ZENITH_LIGHT))
        assertEquals(own.x, asAngle.x, 1e-3f)
        assertEquals(own.y, asAngle.y, 1e-3f)
        // From the right, the light points right; stilled, tipping the phone does not move it.
        val right = lightFrom(Offset(0.5f, 0.5f), GlassTuning(lightAngle = 0f, lightTilt = false))
        assertTrue(right.x > 1f)
        assertEquals(0f, right.y, 1e-4f)
    }

    @Test
    fun `only what is changed is saved, and nonsense is made safe`() {
        assertEquals(0, GlassTuning().toJson().length())
        val tuned = GlassTuning(bend = 1.5f, lightAngle = 30f, lightTilt = false)
        assertEquals(setOf("bend", "light", "tilt"), tuned.toJson().keys().asSequence().toSet())
        assertEquals(tuned, GlassTuning.fromJson(tuned.toJson()))
        val wild = GlassTuning.fromJson(JSONObject("""{"bend":9,"frost":-7,"rim":"x","tint":0.9,"light":-90}"""))
        assertEquals(2f, wild.bend, 0f)
        assertEquals(-1f, wild.frost, 0f)
        assertEquals(1f, wild.rim, 0f)
        assertEquals(0.4f, wild.tint, 0f)
        assertEquals(270f, wild.lightAngle!!, 0f)
        assertTrue(GlassTuning.fromJson(null).isDefault)
        assertFalse(tuned.isDefault)
    }
}
