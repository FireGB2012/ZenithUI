package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconDepthTest {

    private val eps = 1e-3f
    private val sides = listOf(18f, 24f, 32f, 40f, 48f, 56f, 64f, 72f, 96f, 140f)

    private fun everyLook() = sides.flatMap { side ->
        listOf(true, false).flatMap { onWallpaper ->
            listOf(true, false).mapNotNull { dark -> IconDepth.look(side, onWallpaper, dark)?.let { side to it } }
        }
    }

    @Test
    fun `the light is on an icon's edge and the top of its face, never a shaded slab`() {
        for ((side, look) in everyLook()) {
            assertTrue("$side rim ${look.rim}", look.rim > 0.3f && look.rim <= 0.7f)
            assertTrue("$side sheen ${look.sheen}", look.sheen > 0f && look.sheen <= 0.15f)
            // A soft light on the top of the face and a whisper of shade at its foot: depth, not
            // the lit and darkened shoulders of a slab of goo.
            assertTrue("$side top ${look.topLight}", look.topLight > 0f && look.topLight <= 0.15f)
            assertTrue("$side foot ${look.footShade}", look.footShade > 0f && look.footShade <= 0.1f)
            assertTrue(look.footShade < look.topLight)
        }
    }

    @Test
    fun `the rim is a hairline at every size, and the light inside it a narrow band`() {
        for ((side, look) in everyLook()) {
            assertTrue("$side rim ${look.rimDp} dp", look.rimDp in 0.6f..1.4f)
            assertTrue("$side rim ${look.rimDp} dp", look.rimDp <= side * 0.04f)
            assertTrue("$side sheen ${look.sheenDp} dp", look.sheenDp > look.rimDp && look.sheenDp <= side * 0.12f)
        }
    }

    @Test
    fun `an icon too small to show it is drawn flat`() {
        assertNull(IconDepth.look(11f, onWallpaper = true, dark = true))
        assertNull(IconDepth.look(IconDepth.SMALLEST_DP - 0.1f, onWallpaper = false, dark = false))
        assertNotNull(IconDepth.look(IconDepth.SMALLEST_DP, onWallpaper = false, dark = false))
    }

    @Test
    fun `only an icon standing on the wallpaper casts a shadow, and a small one or one asked not to none`() {
        assertTrue(IconDepth.look(56f, onWallpaper = true, dark = true)!!.shadows.isNotEmpty())
        assertTrue(IconDepth.look(56f, onWallpaper = true, dark = false)!!.shadows.isNotEmpty())
        // The drawer and a folder's panel are surfaces of their own: nothing falls on them.
        assertTrue(IconDepth.look(56f, onWallpaper = false, dark = true)!!.shadows.isEmpty())
        // Inside a folder's glass on the home screen.
        assertTrue(IconDepth.look(56f, onWallpaper = true, dark = true, shadow = false)!!.shadows.isEmpty())
        assertTrue(IconDepth.look(IconDepth.SHADOW_FROM_DP - 1f, onWallpaper = true, dark = true)!!.shadows.isEmpty())
    }

    @Test
    fun `its shadow is soft, falls straight down and is no darker than glass's`() {
        for ((side, look) in everyLook()) {
            for (shadow in look.shadows) {
                assertTrue("$side $shadow", shadow.dropDp > 0f)
                assertTrue("$side $shadow", shadow.blurDp >= 2f * shadow.dropDp)
                assertTrue("$side $shadow", shadow.alpha > 0f && shadow.alpha <= 0.2f)
            }
        }
        // A bigger icon stands higher off the wallpaper, so its shadow is wider and falls further.
        val small = IconDepth.look(44f, onWallpaper = true, dark = true)!!.shadows.first()
        val big = IconDepth.look(80f, onWallpaper = true, dark = true)!!.shadows.first()
        assertTrue(big.blurDp > small.blurDp && big.dropDp > small.dropDp)
    }

    @Test
    fun `on the wallpaper an icon is lit the same in Light and in Dark, and off it Light lights it less`() {
        assertEquals(
            IconDepth.look(56f, onWallpaper = true, dark = true),
            IconDepth.look(56f, onWallpaper = true, dark = false),
        )
        // In Light the page under it is pale already, and the same light would whiten its edge.
        assertTrue(IconDepth.look(56f, onWallpaper = false, dark = false)!!.rim < IconDepth.look(56f, onWallpaper = false, dark = true)!!.rim)
    }

    @Test
    fun `the rim is brightest facing the light, bright again opposite and faint between`() {
        assertEquals(1f, IconDepth.rimAt(0f), eps)
        assertTrue(IconDepth.rimAt(1f) in 0.5f..0.7f)
        assertTrue(IconDepth.rimAt(0.5f) <= 0.2f)
        assertTrue(IconDepth.rimAt(0.15f) > IconDepth.rimAt(0.3f))
    }

    private fun assertAt(expected: Offset, actual: Offset) {
        assertEquals("x of $actual", expected.x, actual.x, eps)
        assertEquals("y of $actual", expected.y, actual.y, eps)
    }

    @Test
    fun `the lit corner is the one facing the light and the far one is opposite it`() {
        val size = Size(100f, 100f)
        // Light from up and to the left, as Zenith's is: the top left corner and the bottom right.
        val (lamp, far) = IconDepth.rimEnds(Offset(-0.8f, -0.8f), size)
        assertAt(Offset(0f, 0f), lamp)
        assertAt(Offset(100f, 100f), far)
        // Light from straight above: the top edge and the foot.
        val (top, foot) = IconDepth.rimEnds(Offset(0f, -1f), size)
        assertAt(Offset(50f, 0f), top)
        assertAt(Offset(50f, 100f), foot)
        // The phone tipped so the light comes from the right: the right edge is lit.
        val (right, left) = IconDepth.rimEnds(Offset(1f, 0f), size)
        assertAt(Offset(100f, 50f), right)
        assertAt(Offset(0f, 50f), left)
        // A super icon twice as wide as it is tall is lit brightest on its corner, not part way
        // along its top, and its other two corners, half way round, alike.
        val (wideLamp, wideFar) = IconDepth.rimEnds(Offset(-0.8f, -0.8f), Size(200f, 100f))
        fun along(p: Offset): Float {
            val way = wideFar - wideLamp
            return ((p - wideLamp).x * way.x + (p - wideLamp).y * way.y) / (way.x * way.x + way.y * way.y)
        }
        assertEquals(0f, along(Offset(0f, 0f)), eps)
        assertEquals(1f, along(Offset(200f, 100f)), eps)
        assertEquals(0.5f, along(Offset(200f, 0f)), eps)
        assertEquals(0.5f, along(Offset(0f, 100f)), eps)
    }
}
