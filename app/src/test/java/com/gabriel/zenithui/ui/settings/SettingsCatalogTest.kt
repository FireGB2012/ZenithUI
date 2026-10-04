package com.gabriel.zenithui.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCatalogTest {

    private fun found(query: String): List<String> = SettingsCatalog.search(query).map { it.id }

    @Test
    fun `every option has an id of its own`() {
        val ids = SettingsCatalog.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every page has something on it`() {
        SettingsPage.entries.forEach { page ->
            assertTrue(page.name, SettingsCatalog.on(page).isNotEmpty())
        }
        assertEquals(SettingsCatalog.entries.size, SettingsPage.entries.sumOf { SettingsCatalog.on(it).size })
    }

    @Test
    fun `an option is found by its own title, and comes first`() {
        SettingsCatalog.entries.forEach { entry ->
            assertEquals(entry.title, entry.id, found(entry.title).firstOrNull())
            // However it is typed.
            assertEquals(entry.title, entry.id, found("  ${entry.title.uppercase()} ").firstOrNull())
        }
    }

    @Test
    fun `an option is found by the start of a word`() {
        assertEquals("wallpaper", found("wall").first())
        assertTrue("iconShape" in found("sha"))
        // Not by the middle of one.
        assertTrue("wallpaper" !in found("paper"))
    }

    @Test
    fun `an option is found by other words for it`() {
        assertEquals("appearance", found("dark").first())
        assertEquals("appearance", found("night").first())
        assertTrue("palette" in found("color"))
        assertTrue("palette" in found("colour"))
        assertEquals("haptics", found("vibrate").first())
        assertEquals("keepLoaded", found("battery").first())
        assertEquals("reset", found("start over").first())
    }

    @Test
    fun `a title that matches comes before a keyword that does`() {
        val glass = found("glass")
        assertEquals("glass", glass.first())
        assertTrue("openAnimation" in glass)
    }

    @Test
    fun `a page's name finds what is on it`() {
        val motion = found("motion")
        SettingsCatalog.on(SettingsPage.Motion).forEach { assertTrue(it.id, it.id in motion) }
    }

    @Test
    fun `every word typed has to match`() {
        assertEquals(listOf("iconPack"), found("icon pack"))
        assertEquals(emptyList<String>(), found("icon zzz"))
    }

    @Test
    fun `nothing typed, or nothing like it, finds nothing`() {
        assertEquals(emptyList<String>(), found(""))
        assertEquals(emptyList<String>(), found("   "))
        assertEquals(emptyList<String>(), found("zzz"))
    }

    @Test
    fun `only Zenith Plus opens a sheet, and it is in the catalog all the same`() {
        assertEquals(listOf(SettingsPage.Plus), SettingsPage.entries.filter { it.opensSheet })
        assertEquals("plus", found("subscription").first())
    }

    @Test
    fun `Aura is a page of its own, and what is on it can be searched for`() {
        assertEquals("aura", found("streak").first())
        assertEquals("auraThemes", found("unlock").first())
        assertEquals("auraThemes", found("obsidian").first())
        assertEquals("auraCode", found("share").first())
        assertEquals("auraRedeem", found("redeem").first())
        SettingsCatalog.on(SettingsPage.Aura).forEach { assertTrue(it.id, it.id.startsWith("aura")) }
    }
}
