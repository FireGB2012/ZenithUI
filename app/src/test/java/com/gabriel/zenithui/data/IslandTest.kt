package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandTest {

    private val all = IslandSettings(on = true)
    private fun note(at: Long) = IslandMoment(IslandKind.NOTIFICATION, at, "com.chat", "Sam", "Hi")

    @Test
    fun `with nothing going on it is only the camera, or a small pill if asked`() {
        assertNull(IslandRules.pick(all, musicPlaying = false, live = false, moments = emptyList(), now = 0L))
        assertEquals(IslandKind.IDLE, IslandRules.pick(all.copy(always = true), false, false, emptyList(), 0L)?.first)
    }

    @Test
    fun `a moment comes first, then a running clock, then the music`() {
        assertEquals(IslandKind.MUSIC, IslandRules.pick(all, true, false, emptyList(), 0L)?.first)
        assertEquals(IslandKind.LIVE, IslandRules.pick(all, true, true, emptyList(), 0L)?.first)
        val picked = IslandRules.pick(all, true, true, listOf(note(1_000L)), 2_000L)
        assertEquals(IslandKind.NOTIFICATION, picked?.first)
        assertEquals("Sam", picked?.second?.title)
    }

    @Test
    fun `charging is a moment, not something that stays`() {
        val plugged = IslandMoment(IslandKind.PLUGGED, 0L, level = 64)
        assertEquals(IslandKind.PLUGGED, IslandRules.pick(all, false, false, listOf(plugged), 1_000L)?.first)
        assertNull(IslandRules.pick(all, false, false, listOf(plugged), IslandRules.PLUGGED_MS))
    }

    @Test
    fun `a moment lasts its time and then gives way`() {
        val moments = listOf(note(1_000L))
        assertEquals(IslandKind.NOTIFICATION, IslandRules.pick(all, true, false, moments, 1_000L + IslandRules.NOTIFICATION_MS - 1)?.first)
        assertEquals(IslandKind.MUSIC, IslandRules.pick(all, true, false, moments, 1_000L + IslandRules.NOTIFICATION_MS)?.first)
        assertEquals(1_000L + IslandRules.NOTIFICATION_MS, IslandRules.nextChange(moments, 1_500L))
        assertNull(IslandRules.nextChange(moments, 9_000L))
    }

    @Test
    fun `the newest moment is the one shown`() {
        val ringer = IslandMoment(IslandKind.RINGER, 2_000L, text = "silent")
        assertEquals(IslandKind.RINGER, IslandRules.pick(all, false, false, listOf(note(1_000L), ringer), 2_100L)?.first)
    }

    @Test
    fun `what is switched off is never shown, and lets the next thing through`() {
        val off = all.copy(music = false, live = false, notifications = false, charging = false, ringer = false)
        val plugged = IslandMoment(IslandKind.PLUGGED, 0L, level = 50)
        assertNull(IslandRules.pick(off, true, true, listOf(note(0L), plugged), 100L))
        assertEquals(IslandKind.MUSIC, IslandRules.pick(all.copy(notifications = false, live = false), true, true, listOf(note(0L)), 100L)?.first)
    }

    @Test
    fun `beside what is shown, what else is going on splits off into a bubble`() {
        assertEquals(IslandKind.LIVE, IslandRules.secondary(all, musicPlaying = true, live = true, shown = IslandKind.NOTIFICATION))
        assertEquals(IslandKind.MUSIC, IslandRules.secondary(all, true, false, IslandKind.RINGER))
        assertEquals(IslandKind.MUSIC, IslandRules.secondary(all, true, true, IslandKind.LIVE))
        assertNull(IslandRules.secondary(all, true, true, IslandKind.MUSIC))
        assertNull(IslandRules.secondary(all.copy(music = false), true, false, IslandKind.NOTIFICATION))
        assertNull(IslandRules.secondary(all, true, true, null))
    }

    @Test
    fun `only music, running clocks and notifications open into a card`() {
        assertTrue(IslandKind.MUSIC.opens && IslandKind.LIVE.opens && IslandKind.NOTIFICATION.opens)
        assertFalse(IslandKind.RINGER.opens || IslandKind.PLUGGED.opens || IslandKind.IDLE.opens)
    }

    @Test
    fun `a running clock reads as a clock does`() {
        assertEquals("0:07", IslandRules.clock(base = 0L, down = false, now = 7_400L))
        assertEquals("4:32", IslandRules.clock(0L, false, 272_000L))
        assertEquals("1:02:09", IslandRules.clock(0L, false, 3_729_000L))
        // Counting down, a part second still to go is a second still to go, and it stops at nothing.
        assertEquals("1:00", IslandRules.clock(base = 60_000L, down = true, now = 500L))
        assertEquals("0:00", IslandRules.clock(60_000L, true, 70_000L))
    }

    @Test
    fun `old moments are let go, and only a handful kept`() {
        val many = (0 until 20).map { note(it * 10L) }
        val kept = IslandRules.prune(many, 200L)
        assertTrue(kept.size <= 8)
        assertEquals(190L, kept.first().at)
        assertTrue(IslandRules.prune(many, 100_000L).isEmpty())
    }

    @Test
    fun `only what is changed is saved, and an island set to a small pill before keeps it`() {
        assertEquals(0, IslandSettings().toJson().length())
        val mine = IslandSettings(on = true, live = false, always = true, hideSideways = false)
        assertEquals(mine, IslandSettings.fromJson(mine.toJson()))
        assertTrue(IslandSettings.fromJson(JSONObject("""{"idle":"pill","style":"glow","width":1.3}""")).always)
    }
}
