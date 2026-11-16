package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class LogoLiftTest {

    /** An icon's side, in pixels: the Pixel 10a's home screen icon, near enough. */
    private val size = 180

    /** A logo [side] pixels square in the middle of the tile, as a foreground layer covers it. */
    private fun square(side: Int, strength: Float = 1f): FloatArray {
        val alpha = FloatArray(size * size)
        val from = (size - side) / 2
        for (y in from until from + side) for (x in from until from + side) alpha[y * size + x] = strength
        return alpha
    }

    /** What is at [x], [y] of a picture as big as the tile. (Inside, [size] would be the array's own.) */
    private fun FloatArray.at(x: Int, y: Int) = this[y * this@LogoLiftTest.size + x]

    @Test
    fun `a logo on its own is lifted, and a foreground that is the whole icon is not`() {
        // A logo leaves most of the tile to the background, which it can be lifted off...
        assertTrue(LogoLift.lifts(0.3f))
        assertTrue(LogoLift.lifts(0.04f))
        // ...a foreground that covers the tile is the icon itself, with nothing under it to stand
        // off, and an empty one has nothing to lift.
        assertFalse(LogoLift.lifts(0.95f))
        assertFalse(LogoLift.lifts(0f))
    }

    @Test
    fun `coverage is the share of the tile the logo covers`() {
        assertEquals(0.25f, LogoLift.coverage(square(90)), 0.001f)
        assertEquals(0f, LogoLift.coverage(FloatArray(size * size)), 0f)
        // Measured over a square of a wider picture, as the logo in the middle of a super icon is.
        val wide = FloatArray(size * 2 * size)
        for (y in 45 until 135) for (x in 225 until 315) wide[y * size * 2 + x] = 1f
        assertEquals(0.25f, LogoLift.coverage(wide, width = size * 2, left = 180, top = 0, side = size), 0.001f)
    }

    @Test
    fun `the logo's shadow falls straight down`() {
        val look = LogoLift.look(size)
        val shadow = LogoLift.shadow(square(60), size, size, look)
        var x = 0.0
        var y = 0.0
        var weight = 0.0
        for (row in 0 until size) for (column in 0 until size) {
            val s = shadow.at(column, row).toDouble()
            x += s * column
            y += s * row
            weight += s
        }
        assertTrue(look.dropPx > 0f)
        // Not to one side at all, and down by how far it falls.
        assertEquals((size - 1) / 2.0, x / weight, 0.5)
        assertEquals((size - 1) / 2.0 + look.dropPx, y / weight, 1.0)
    }

    @Test
    fun `the shadow is soft and light, never a heavy black`() {
        val look = LogoLift.look(size)
        val shadow = LogoLift.shadow(square(60), size, size, look)
        val darkest = shadow.maxOrNull()!!
        assertTrue("darkest $darkest", darkest in 0.15f..0.35f)
        // It fades out under the logo rather than stopping at an edge...
        val under = (size + 60) / 2
        val close = shadow.at(size / 2, under + 1)
        val further = shadow.at(size / 2, under + ceil(look.blurPx * 2f).toInt())
        assertTrue(close > further && further > 0f)
        // ...and stays near it.
        assertEquals(0f, shadow.at(2, 2), 0.0001f)
        assertEquals(0f, shadow.at(size / 2, size - 2), 0.0001f)
    }

    @Test
    fun `light lies on the edges facing up and left, fainter opposite, and only on the logo`() {
        val look = LogoLift.look(size)
        val logo = square(60)
        val light = LogoLift.light(logo, size, size, look)
        val from = 60
        val to = 119
        val mid = size / 2
        val top = light.at(mid, from)
        val left = light.at(from, mid)
        val bottom = light.at(mid, to)
        val right = light.at(to, mid)
        // Brightest facing the light, as Zenith's glass is lit, and bright again opposite...
        assertTrue("top $top left $left", top > 0.2f && left > 0.2f)
        assertTrue("bottom $bottom right $right", bottom > 0f && right > 0f)
        assertTrue(bottom < top && right < left)
        // ...with nothing inside the logo away from its edge, and nothing off it at all.
        assertEquals(0f, light.at(mid, mid), 0f)
        for (i in light.indices) if (logo[i] == 0f) assertEquals(0f, light[i], 0f)
        assertTrue(light.all { it in 0f..1f })
    }

    @Test
    fun `the light is a hairline, not a bevel`() {
        val look = LogoLift.look(size)
        assertTrue(look.rimPx >= 1f && look.rimPx <= size * 0.02f)
        val light = LogoLift.light(square(60), size, size, look)
        // A couple of pixels in from the edge facing the light, there is none.
        assertEquals(0f, light.at(size / 2, 60 + ceil(look.rimPx).toInt() + 1), 0f)
    }

    @Test
    fun `the logo's own face is lit from above, a little`() {
        val look = LogoLift.look(size)
        val logo = square(60)
        val face = LogoLift.face(logo, size, size, look)
        val mid = size / 2
        // Lighter at its top, as white, and a little darker at its foot, as black (below nothing)...
        assertTrue(face.at(mid, 60) > 0f)
        assertTrue(face.at(mid, 119) < 0f)
        // ...plain across its middle...
        assertEquals(0f, face.at(mid, mid), 0f)
        // ...and subtle at both ends: a logo standing off its tile, not a bevelled button...
        assertTrue(face.all { it in -0.15f..0.12f })
        // ...and only ever on the logo.
        for (i in face.indices) if (logo[i] == 0f) assertEquals(0f, face[i], 0f)
    }

    @Test
    fun `a logo's face is lit over its own height, wherever it sits on the tile`() {
        // Low on the tile, it is still light at its own top and shaded at its own foot.
        val low = FloatArray(size * size)
        for (y in 120 until 160) for (x in 70 until 110) low[y * size + x] = 1f
        val face = LogoLift.face(low, size, size, LogoLift.look(size))
        assertTrue(face.at(90, 120) > 0f)
        assertTrue(face.at(90, 159) < 0f)
        // Nothing to light, nothing lit.
        assertTrue(LogoLift.face(FloatArray(size * size), size, size, LogoLift.look(size)).all { it == 0f })
    }

    @Test
    fun `faint parts of a foreground, such as a shadow drawn into it, catch no light`() {
        val wash = square(90, strength = 0.2f)
        assertEquals(0f, LogoLift.light(wash, size, size, LogoLift.look(size)).maxOrNull()!!, 0f)
    }

    @Test
    fun `the lift keeps its proportions at any size`() {
        val small = LogoLift.look(90)
        val big = LogoLift.look(180)
        assertEquals(small.blurPx * 2f, big.blurPx, 0.01f)
        assertEquals(small.dropPx * 2f, big.dropPx, 0.01f)
        assertEquals(small.shadow, big.shadow, 0f)
        // The smallest icon Zenith draws still has its hairline.
        assertTrue(LogoLift.look(48).rimPx >= 1f)
    }
}
