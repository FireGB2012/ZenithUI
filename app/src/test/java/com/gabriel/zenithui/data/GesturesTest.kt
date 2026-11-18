package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturesTest {

    @Test
    fun `a gesture nobody has touched does what Zenith does by default`() {
        assertEquals(GestureActions.NOTIFICATIONS, Gestures.actionFor(Gestures.SWIPE_DOWN, emptyMap()))
        assertEquals(GestureActions.CUSTOMISE, Gestures.actionFor(Gestures.PINCH_IN, emptyMap()))
        assertEquals(GestureActions.FIRST_PAGE, Gestures.actionFor(Gestures.HOME, emptyMap()))
        assertEquals(GestureActions.NONE, Gestures.actionFor(Gestures.DOUBLE_TAP, emptyMap()))
        // Every gesture has a default, and every default is something on offer.
        Gestures.ids.forEach { assertTrue(Gestures.default(it) in GestureActions.fixed) }
    }

    @Test
    fun `only what is changed is kept`() {
        val chosen = Gestures.with(emptyMap(), Gestures.DOUBLE_TAP, GestureActions.LOCK)
        assertEquals(mapOf(Gestures.DOUBLE_TAP to GestureActions.LOCK), chosen)
        assertEquals(GestureActions.LOCK, Gestures.actionFor(Gestures.DOUBLE_TAP, chosen))
        // Choosing the default again is the same as never having chosen.
        assertTrue(Gestures.with(chosen, Gestures.DOUBLE_TAP, GestureActions.NONE).isEmpty())
    }

    @Test
    fun `an app or a shortcut is known by its key`() {
        val app = GestureActions.app("com.maps/com.maps.Main")
        assertEquals("com.maps/com.maps.Main", GestureActions.appKey(app))
        assertEquals("Open Maps", GestureActions.label(app) { if (it == "com.maps/com.maps.Main") "Maps" else null })
        assertNull(GestureActions.appKey(GestureActions.TORCH))
        assertNull(GestureActions.appKey("app:nonsense"))
        val shortcut = ShortcutKeys.of("com.maps", "home")
        assertTrue(GestureActions.isShortcut(shortcut))
        assertEquals("Route home", GestureActions.label(shortcut) { "Route home" })
    }

    @Test
    fun `every action has a name, and only locking needs the helper`() {
        val names = GestureActions.fixed.map { action -> GestureActions.label(action) { null } }
        assertEquals(names.size, names.toSet().size)
        assertTrue(GestureActions.needsHelper(GestureActions.LOCK))
        GestureActions.fixed.filter { it != GestureActions.LOCK }.forEach { assertFalse(GestureActions.needsHelper(it)) }
        Gestures.ids.forEach { assertTrue(Gestures.label(it).isNotBlank()) }
    }

    @Test
    fun `a copy of Zenith without the lock helper does not offer locking`() {
        assertEquals(GestureActions.fixed, GestureActions.offered(lockHelper = true))
        val without = GestureActions.offered(lockHelper = false)
        assertEquals(GestureActions.fixed - GestureActions.LOCK, without)
        // No gesture needs the helper by default, so every default is still on offer.
        Gestures.ids.forEach { assertTrue(it, Gestures.default(it) in without) }
    }

    @Test
    fun `locking chosen in a copy with the lock helper does the gesture's default in one without`() {
        // From a backup, or a phone that had a copy with the helper: the choice itself is kept, for
        // a copy that has the helper again.
        val chosen = mapOf(
            Gestures.DOUBLE_TAP to GestureActions.LOCK,
            Gestures.SWIPE_DOWN to GestureActions.LOCK,
            Gestures.TWO_UP to GestureActions.TORCH,
        )
        assertEquals(GestureActions.NONE, Gestures.actionFor(Gestures.DOUBLE_TAP, chosen, lockHelper = false))
        assertEquals(GestureActions.NOTIFICATIONS, Gestures.actionFor(Gestures.SWIPE_DOWN, chosen, lockHelper = false))
        assertEquals(GestureActions.TORCH, Gestures.actionFor(Gestures.TWO_UP, chosen, lockHelper = false))
        // With the helper, locking is locking.
        assertEquals(GestureActions.LOCK, Gestures.actionFor(Gestures.DOUBLE_TAP, chosen, lockHelper = true))
        assertEquals(GestureActions.LOCK, Gestures.actionFor(Gestures.DOUBLE_TAP, chosen))
    }

    @Test
    fun `chosen gestures survive being written and read back`() {
        val settings = ZenithSettings(gestures = mapOf(Gestures.DOUBLE_TAP to GestureActions.LOCK, Gestures.TWO_UP to GestureActions.app("a/b")))
        assertEquals(settings.gestures, ZenithSettings.fromJson(JSONObject(settings.toJson().toString())).gestures)
        // Nothing written is nothing chosen.
        assertTrue(ZenithSettings.fromJson(JSONObject("{}")).gestures.isEmpty())
    }
}
