package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.Size
import com.gabriel.zenithui.data.GlassBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassLookTest {

    private val tint = Palettes.Midnight.glassTint

    private fun look(
        material: GlassMaterial = GlassMaterial.Regular,
        strength: Float = 0.8f,
        solid: Float = 0f,
        dark: Boolean = true,
        border: Boolean = true,
        body: GlassBody = GlassBody.Clear,
    ) = glassLook(material, strength, solid, opacity = 1f, dark = dark, tint = tint, border = border, depth = body)

    @Test
    fun `clear glass mists the wallpaper and regular glass frosts it`() {
        val clear = look(GlassMaterial.Clear)
        val regular = look(GlassMaterial.Regular)
        assertEquals(FROST_CLEAR, clear.frost, 0f)
        assertEquals(FROST_REGULAR, regular.frost, 0f)
        // Clear still softens: glass with the wallpaper sharp through it reads as a hole.
        assertTrue(clear.frost >= 0.6f)
        assertTrue(clear.frost < regular.frost)
        // And it bends, shines and throws back more than glass with words on it.
        assertTrue(clear.refraction > regular.refraction)
        assertTrue(clear.mirror > regular.mirror)
    }

    @Test
    fun `a pane with more body is softer, and never past the softest there is`() {
        val pane = look()
        val sheet = look(solid = 0.65f)
        val page = look(solid = 1.2f)
        assertTrue(pane.frost < sheet.frost && sheet.frost < page.frost)
        assertTrue(look(body = GlassBody.Solid).frost > pane.frost)
        for (solid in listOf(0f, 0.65f, 1.5f, 9f)) for (material in GlassMaterial.entries) for (body in GlassBody.entries) {
            assertTrue(look(material, solid = solid, body = body).frost in 0f..3f)
        }
    }

    @Test
    fun `a page with no edge has no rim to bend, light or reflect anything`() {
        val page = look(border = false, solid = 0.9f)
        assertFalse(page.rimmed)
        assertEquals(0f, page.shine, 0f)
        assertEquals(0f, page.mirror, 0f)
        assertEquals(0f, page.glow, 0f)
        assertEquals(0f, page.relief, 0f)
        assertTrue(look().rimmed)
    }

    @Test
    fun `a pane is a slab, and turning the glass down flattens it`() {
        assertEquals(1f, look(strength = 1f).relief, 0f)
        assertEquals(0.8f, look(strength = 0.8f).relief, 0.001f)
        assertEquals(0.2f, look(strength = 0.2f).relief, 0.001f)
        // Glass turned up past full is no more of a slab than a slab.
        assertEquals(1f, look(strength = 1.4f).relief, 0f)
        for (material in GlassMaterial.entries) assertTrue(look(material).relief > 0f)
    }

    @Test
    fun `a bigger pane is a thicker slab`() {
        val density = 2.625f
        fun thickness(widthDp: Float, heightDp: Float) = slabThickness(Size(widthDp * density, heightDp * density), density)
        // A widget and a sheet are as thick as glass gets; the dock a little less; a small control least.
        assertEquals(1f, thickness(176f, 176f), 0.001f)
        assertEquals(1f, thickness(387f, 250f), 0.001f)
        assertEquals(86f * 0.24f / 28f, thickness(387f, 86f), 0.001f)
        assertEquals(12f / 28f, thickness(194f, 46f), 0.001f)
        // The shorter side decides, whichever it is.
        assertEquals(thickness(387f, 86f), thickness(86f, 387f), 0f)
        // Something too small for two shoulders has what room there is, and nothing has none.
        assertEquals(20f * 0.45f / 28f, thickness(20f, 20f), 0.001f)
        assertEquals(0f, thickness(0f, 0f), 0f)
    }

    @Test
    fun `turning the glass down turns down its bend, its light and its reflection together`() {
        val full = look(strength = 1f)
        val low = look(strength = 0.2f)
        assertTrue(low.refraction < full.refraction)
        assertTrue(low.shine < full.shine)
        assertTrue(low.mirror < full.mirror)
        assertTrue(low.relief < full.relief)
        // What is turned down is how lively the glass is, not what it is made of.
        assertEquals(full.frost, low.frost, 0f)
        assertEquals(full.wash, low.wash)
    }

    @Test
    fun `the glass takes no colour from anywhere, as Apple's does not`() {
        for (material in GlassMaterial.entries) for (dark in listOf(true, false)) for (body in GlassBody.entries) {
            val warm = glassLook(material, 0.8f, solid = 0.4f, opacity = 1f, dark = dark, tint = Palettes.Sunset.glassTint, depth = body)
            val cool = glassLook(material, 0.8f, solid = 0.4f, opacity = 1f, dark = dark, tint = Palettes.Aurora.glassTint, depth = body)
            // No light taken from the wallpaper round it into the rim.
            assertEquals(0f, warm.glow, 0f)
            // And nothing of the theme in the veil or in the frost that lifts it.
            assertEquals(warm.wash, cool.wash)
            assertEquals(warm.lift, cool.lift)
        }
    }

    @Test
    fun `every number the shader is given is one it can use`() {
        for (material in GlassMaterial.entries) for (dark in listOf(true, false)) for (body in GlassBody.entries) {
            for (strength in listOf(0f, 0.2f, 0.8f, 1.2f, 5f)) {
                val pane = look(material, strength, solid = 0.4f, dark = dark, body = body)
                assertTrue(pane.mirror in 0f..1f)
                assertTrue(pane.glow in 0f..1f)
                assertTrue(pane.gather in 0f..1f)
                assertTrue(pane.relief in 0f..1f)
                assertTrue(pane.refraction in 0f..1.2f)
                assertTrue(pane.wash.alpha in 0f..0.92f)
            }
        }
    }
}
