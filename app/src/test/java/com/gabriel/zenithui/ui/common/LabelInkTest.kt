package com.gabriel.zenithui.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelInkTest {

    /** A light palette's ink, a dark one's, and a coloured one. */
    private val inks = listOf(Color(0xFF15161C), Color(0xFFF4F5FA), Color(0xFF3A6FD8))

    private val densities = listOf(1f, 2.625f, 3.5f)

    @Test
    fun `names on the wallpaper are white whatever the palette's ink`() {
        // As on an iPhone: a dark name could not be read over a dark wallpaper.
        inks.forEach { assertEquals(Color.White, labelInk(onWallpaper = true, onGlass = it)) }
    }

    @Test
    fun `names on the wallpaper have a soft shadow, and a darker, closer one with Name shadow on`() {
        for (density in densities) {
            assertEquals(
                Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f * density), 3f * density),
                labelShadow(onWallpaper = true, legacyShadow = false, density = density),
            )
            // For a pale or busy wallpaper: darker and tighter, not wider, which only thins it out.
            assertEquals(
                Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 1f * density), 1.5f * density),
                labelShadow(onWallpaper = true, legacyShadow = true, density = density),
            )
        }
    }

    @Test
    fun `names anywhere else keep the palette's ink`() {
        // The drawer and the folder panel have their own surface under them, and its own ink.
        inks.forEach { assertEquals(it, labelInk(onWallpaper = false, onGlass = it)) }
    }

    @Test
    fun `names anywhere else have the shadow they always had, and only with Name shadow on`() {
        for (density in densities) {
            assertEquals(
                Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1.5f), 7f),
                labelShadow(onWallpaper = false, legacyShadow = true, density = density),
            )
            assertNull(labelShadow(onWallpaper = false, legacyShadow = false, density = density))
        }
    }
}
