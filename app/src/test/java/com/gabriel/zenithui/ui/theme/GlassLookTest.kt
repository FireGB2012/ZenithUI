package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
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
        // And it bends more than glass with words on it, and only it splits the light into colour.
        assertTrue(clear.refraction > regular.refraction)
        assertTrue(clear.dispersion > 0f)
        assertEquals(0f, regular.dispersion, 0f)
        // Both make what is behind them richer, Regular, which frosts it, the more.
        assertTrue(clear.vibrance > 1f && regular.vibrance > clear.vibrance)
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
    fun `a page with no edge has no rim to bend or light`() {
        val page = look(border = false, solid = 0.9f)
        assertFalse(page.rimmed)
        assertEquals(0f, page.shine, 0f)
        assertEquals(0f, page.glow, 0f)
        assertTrue(look().rimmed)
    }

    @Test
    fun `the edge of a pane always catches some light, and more the livelier the glass`() {
        assertEquals(0.55f, shineFor(0f), 0.001f)
        assertEquals(0.79f, shineFor(0.8f), 0.001f)
        // Glass turned up past what the edge can show is no brighter for it.
        assertEquals(shineFor(1.2f), shineFor(5f), 0f)
        for (material in GlassMaterial.entries) assertTrue(look(material).shine > 0.5f)
    }

    @Test
    fun `a bigger pane is a thicker slab`() {
        val density = 2.625f
        fun thickness(widthDp: Float, heightDp: Float) = glassThickness(Size(widthDp * density, heightDp * density), density)
        // A widget and a sheet are as thick as glass gets; the dock most of the way; Search hardly at all.
        assertEquals(1f, thickness(170f, 170f), 0.001f)
        assertEquals(1f, thickness(387f, 250f), 0.001f)
        assertEquals((92f - 28f) / 92f, thickness(387f, 92f), 0.001f)
        assertEquals(4f / 92f, thickness(108f, 32f), 0.001f)
        // The shorter side decides, whichever it is.
        assertEquals(thickness(387f, 92f), thickness(92f, 387f), 0f)
        // Anything small is thin glass, and nothing is no glass at all.
        assertEquals(0f, thickness(20f, 20f), 0f)
        assertEquals(0f, thickness(0f, 0f), 0f)
    }

    @Test
    fun `only a band round the rim bends, a quarter of the pane and no wider than glass that thick`() {
        val density = 2.625f
        fun rounded(widthDp: Float, heightDp: Float, cornerDp: Float) =
            RoundRect(0f, 0f, widthDp * density, heightDp * density, CornerRadius(cornerDp * density, cornerDp * density))
        fun bandDp(widthDp: Float, heightDp: Float, outline: RoundRect?) =
            lensBand(Size(widthDp * density, heightDp * density), density, outline) / density
        // The dock: a quarter of 92 dp would be 23, more than glass that thick bends.
        assertEquals(12f + 10f * (64f / 92f), bandDp(387f, 92f, rounded(387f, 92f, 36f)), 0.01f)
        // Search: a quarter of its height.
        assertEquals(8f, bandDp(108f, 32f, rounded(108f, 32f, 16f)), 0.01f)
        // A widget: as wide as the thickest glass bends, which its corners allow.
        assertEquals(22f, bandDp(170f, 170f, rounded(170f, 170f, 22f)), 0.01f)
        // Tighter corners than that hold it in, or the band would crease at each corner.
        assertEquals(10f, bandDp(170f, 170f, rounded(170f, 170f, 10f)), 0.01f)
        // An icon's tile, a free-form outline with no round corners to hold it in.
        val tile: RoundRect? = null
        assertEquals(15.5f, bandDp(62f, 62f, tile), 0.01f)
        // Something small still has a band it can be seen to bend in, but never more than half of it.
        assertEquals(5f, bandDp(16f, 16f, tile), 0.01f)
        assertEquals(4f, bandDp(8f, 8f, tile), 0.01f)
        // A sheet with square corners at its foot is held in by its round ones only.
        val sheet = RoundRect(
            0f, 0f, 400f * density, 300f * density,
            topLeftCornerRadius = CornerRadius(32f * density), topRightCornerRadius = CornerRadius(32f * density),
            bottomRightCornerRadius = CornerRadius.Zero, bottomLeftCornerRadius = CornerRadius.Zero,
        )
        assertEquals(22f, bandDp(400f, 300f, sheet), 0.01f)
    }

    @Test
    fun `glass floats on one soft shadow that falls straight down, and nothing heavier`() {
        for (material in GlassMaterial.entries) for (dark in listOf(true, false)) for (thick in listOf(0f, 0.5f, 1f)) {
            val (wide, close) = glassShadows(material, dark, thick)
            assertTrue(wide.alpha in 0.05f..SHADOW_DARK)
            assertTrue(wide.blurDp >= 10f && wide.dropDp >= 2f)
            // The close one is only there to draw the outline on a pale picture.
            assertTrue(close.alpha <= 0.06f && close.blurDp <= 2f)
        }
        // A thicker slab floats higher: its shadow is softer, falls further and is a little deeper.
        val thin = glassShadows(GlassMaterial.Regular, dark = true, thick = 0f).first()
        val thick = glassShadows(GlassMaterial.Regular, dark = true, thick = 1f).first()
        assertTrue(thick.blurDp > thin.blurDp && thick.dropDp > thin.dropDp && thick.alpha > thin.alpha)
        assertEquals(24f, thick.blurDp, 0.001f)
        assertEquals(7f, thick.dropDp, 0.001f)
        assertEquals(SHADOW_DARK, thick.alpha, 0.001f)
        // Deeper in the dark, lighter under Clear.
        assertTrue(glassShadows(GlassMaterial.Regular, dark = false, thick = 1f).first().alpha < thick.alpha)
        assertTrue(glassShadows(GlassMaterial.Clear, dark = true, thick = 1f).first().alpha < thick.alpha)
    }

    @Test
    fun `turning the glass down turns down its bend and its light together`() {
        val full = look(strength = 1f)
        val low = look(strength = 0.2f)
        assertTrue(low.refraction < full.refraction)
        assertTrue(low.shine < full.shine)
        // What is turned down is how lively the glass is, not what it is made of.
        assertEquals(full.frost, low.frost, 0f)
        assertEquals(full.wash, low.wash)
    }

    @Test
    fun `every number the shader is given is one it can use`() {
        for (material in GlassMaterial.entries) for (dark in listOf(true, false)) for (body in GlassBody.entries) {
            for (strength in listOf(0f, 0.2f, 0.8f, 1.2f, 5f)) {
                val pane = look(material, strength, solid = 0.4f, dark = dark, body = body)
                assertTrue(pane.glow in 0f..1f)
                assertTrue(pane.refraction in 0f..1.2f)
                assertTrue(pane.wash.alpha in 0f..0.92f)
                assertTrue(pane.vibrance in 1f..1.6f)
                assertTrue(pane.frost in 0f..3f)
            }
        }
    }
}
