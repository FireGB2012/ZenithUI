package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetNamesTest {

    @Test
    fun `every widget that can be put on a page has a short name`() {
        val names = Widgets.placeable.map { Widgets.shortName(it.type) }
        assertEquals(
            listOf("Clock", "Battery", "Calendar", "Notes", "Controls", "Music", "World Clock", "Countdown", "Photos", "To-do", "Aura", "Weather"),
            names,
        )
        // Short enough to sit under a small widget as an icon's name sits under it.
        names.forEach { assertTrue(it, it.length in 1..12) }
    }

    @Test
    fun `a no glass widget has the name of the widget it is a version of`() {
        Widgets.noGlass.forEach { assertEquals(Widgets.shortName(Widgets.baseType(it.type)), Widgets.shortName(it.type)) }
        assertEquals("Clock", Widgets.shortName("bare_clock"))
        assertEquals("To-do", Widgets.shortName("bare_todo"))
    }

    @Test
    fun `a widget this Zenith does not know is simply a widget`() {
        assertEquals("Widget", Widgets.shortName("hologram"))
        assertEquals("Widget", Widgets.shortName(""))
        assertEquals("Widget", Widgets.shortName("bare_hologram"))
    }

    @Test
    fun `another app's widget is named after the app, as the drawer has it`() {
        val apps = listOf(
            AppInfo("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail", "Gmail"),
            AppInfo("com.spotify.music", "com.spotify.music.MainActivity", "Spotify"),
        )
        assertEquals("Spotify", Widgets.hostedName("com.spotify.music/com.spotify.widget.PlayerProvider", apps))
        // The short form of a component name is the same app.
        assertEquals("Gmail", Widgets.hostedName("com.google.android.gm/.widget.GmailWidgetProvider", apps))
        // An app that is not in the drawer, one that is only widgets, is for Android to name.
        assertNull(Widgets.hostedName("com.example.weather/.Provider", apps))
    }
}
