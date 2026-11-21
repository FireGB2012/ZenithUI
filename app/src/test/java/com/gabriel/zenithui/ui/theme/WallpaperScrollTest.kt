package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperScrollTest {

    @Test
    fun `from android 14 the wallpaper stays in the middle whatever page you are on, and so does the glass`() {
        for (sdk in listOf(34, 35, 36)) {
            for (page in listOf(0f, 0.25f, 1f)) {
                assertEquals(Offset(0.5f, 0.5f), systemWallpaperOffset(Offset(page, 0.5f), sdk))
            }
        }
    }

    @Test
    fun `before android 14 the wallpaper slides with the pages, and the glass with it`() {
        assertEquals(Offset(0.25f, 0.5f), systemWallpaperOffset(Offset(0.25f, 0.5f), 33))
        assertEquals(Offset(1f, 0.5f), systemWallpaperOffset(Offset(1f, 0.5f), 29))
    }

    @Test
    fun `the glass is only told the pages' offset where it moves the wallpaper, so a swipe does not redraw every pane for the same picture`() {
        assertTrue(glassFollowsPages(33))
        assertFalse(glassFollowsPages(34))
        assertFalse(glassFollowsPages(36))
    }
}
