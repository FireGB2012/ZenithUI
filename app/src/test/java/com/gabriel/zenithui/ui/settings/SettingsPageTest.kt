package com.gabriel.zenithui.ui.settings

import androidx.compose.ui.graphics.Color
import com.gabriel.zenithui.ui.theme.contrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPageTest {

    @Test
    fun `white stands three to one on every group's tile`() {
        for (page in SettingsPage.entries) {
            val stands = contrast(Color.White, page.tile())
            assertTrue("${page.name}: $stands", stands >= 3f)
        }
    }

    @Test
    fun `every tile is solid`() {
        for (page in SettingsPage.entries) {
            assertEquals(page.name, 1f, page.tile().alpha, 0f)
        }
    }

    @Test
    fun `back from advanced animation goes to motion and feel`() {
        assertEquals(SettingsPage.Motion, SettingsPage.AdvancedMotion.parent)
        assertEquals("Motion & feel", backTo(SettingsPage.AdvancedMotion))
    }

    @Test
    fun `back from every other page goes to the first page of settings`() {
        for (page in SettingsPage.entries - SettingsPage.AdvancedMotion) {
            assertNull(page.name, page.parent)
            assertEquals(page.name, "Settings", backTo(page))
        }
    }
}
