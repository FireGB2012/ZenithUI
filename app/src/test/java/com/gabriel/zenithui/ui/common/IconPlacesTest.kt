package com.gabriel.zenithui.ui.common

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.gabriel.zenithui.ui.common.IconPlaces.Spot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconPlacesTest {

    private val screen = Size(1080f, 2400f)
    private val mail = "com.example.mail/com.example.mail.Main"
    private val compose = "com.example.mail/com.example.mail.Compose"
    private val maps = "com.example.maps/com.example.maps.Main"

    private fun at(left: Float, top: Float) = Rect(left, top, left + 168f, top + 168f)

    private fun choose(spots: List<Spot>, key: String?, pkg: String = "com.example.mail", work: Boolean = false, near: Rect? = null) =
        IconPlaces.choose(spots, key, pkg, work, near, screen)

    @Test
    fun `an app goes back into the icon it was opened from`() {
        val page = Spot(listOf(mail), at(100f, 600f))
        val dock = Spot(listOf(mail), at(100f, 2100f), dock = true)
        // Opened from the dock: back into the dock, not the copy on the page.
        assertEquals(dock, choose(listOf(page, dock), mail, near = at(100f, 2100f)))
        assertEquals(page, choose(listOf(page, dock), mail, near = at(104f, 598f)))
        // Not opened from Zenith: the page before the dock, as Android's own launcher does.
        assertEquals(page, choose(listOf(dock, page), mail))
    }

    @Test
    fun `failing that, that app's icon, then a folder holding it`() {
        val other = Spot(listOf(compose), at(400f, 600f))
        val folder = Spot(listOf(maps, mail), at(700f, 900f), folder = true)
        // Opened from the drawer, say, as an activity the page does not have: the app's own icon.
        assertEquals(other, choose(listOf(folder, other), mail))
        assertEquals(folder, choose(listOf(folder), mail))
        assertEquals(folder, choose(listOf(folder), key = null))
        // A shortcut of the app is not the app.
        assertNull(choose(listOf(Spot(listOf("shortcut:com.example.mail/new"), at(0f, 0f))), mail))
        // Its own shortcut is still where it goes back to.
        val shortcut = Spot(listOf("shortcut:com.example.mail/new"), at(0f, 0f))
        assertEquals(shortcut, choose(listOf(shortcut), "shortcut:com.example.mail/new"))
    }

    @Test
    fun `a work app goes into the work icon, and a personal one into the personal`() {
        val personal = Spot(listOf(mail), at(100f, 600f))
        val work = Spot(listOf("work:$mail"), at(400f, 600f))
        assertEquals(work, choose(listOf(personal, work), key = null, work = true))
        assertEquals(personal, choose(listOf(work, personal), key = null, work = false))
        assertNull(choose(listOf(work), key = null, work = false))
    }

    @Test
    fun `only an icon on show counts`() {
        // Off the page on show, or not laid out yet.
        assertNull(choose(listOf(Spot(listOf(mail), Rect.Zero)), mail))
        assertNull(choose(listOf(Spot(listOf(mail), at(1200f, 600f))), mail))
        assertNull(choose(listOf(Spot(listOf(mail), at(-400f, 600f))), mail))
        assertNull(choose(listOf(Spot(listOf(maps), at(100f, 600f))), mail))
    }
}
