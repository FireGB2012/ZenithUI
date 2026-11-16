package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandTest {

    /** Everything on, but the pill at rest, so what each rule does is seen on its own. */
    private val all = IslandSettings(on = true, always = false)
    private fun note(at: Long) = IslandMoment(IslandKind.NOTIFICATION, at, "com.chat", "Sam", "Hi")

    @Test
    fun `with nothing going on it is the small pill at rest, or only the camera if asked`() {
        assertEquals(IslandKind.IDLE, IslandRules.pick(IslandSettings(on = true), musicPlaying = false, live = false, moments = emptyList(), now = 0L)?.first)
        assertNull(IslandRules.pick(all, false, false, emptyList(), 0L))
    }

    @Test
    fun `at rest it shows two status icons at most, Do Not Disturb first, and none when all is usual`() {
        assertEquals(emptyList<NotchStatus>(), IslandRules.status("ring", quiet = false))
        assertEquals(listOf(NotchStatus.SILENT), IslandRules.status("silent", quiet = false))
        assertEquals(listOf(NotchStatus.VIBRATE), IslandRules.status("vibrate", quiet = false))
        assertEquals(listOf(NotchStatus.QUIET, NotchStatus.SILENT), IslandRules.status("silent", quiet = true))
        assertEquals(listOf(NotchStatus.QUIET), IslandRules.status("ring", quiet = true))
    }

    @Test
    fun `the battery at rest is green while it charges and red when it runs low`() {
        assertEquals(BatteryLook.CHARGING, IslandRules.battery(12, charging = true))
        assertEquals(BatteryLook.LOW, IslandRules.battery(IslandRules.LOW_BATTERY, charging = false))
        assertEquals(BatteryLook.NORMAL, IslandRules.battery(IslandRules.LOW_BATTERY + 1, charging = false))
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
        // One set to the camera alone before keeps that; one never set gets the pill at rest.
        assertFalse(IslandSettings.fromJson(JSONObject("""{"idle":"camera"}""")).always)
        assertTrue(IslandSettings.fromJson(JSONObject("""{"on":true}""")).always)
        assertFalse(IslandSettings.fromJson(IslandSettings(always = false).toJson()).always)
    }

    @Test
    fun `the Notch's fit is kept within a little of its own size and a few dp of the camera`() {
        assertEquals(NotchFit(), NotchFit.of(1f, 1f, 0f, 0f))
        assertTrue(NotchFit().isDefault)
        // Whatever is asked, it stays within its limits: no Notch half the screen wide, none dragged away.
        val most = NotchFit.of(width = 9f, height = 9f, x = 500f, y = 500f)
        assertEquals(NotchFit.WIDTH.endInclusive, most.width, 0f)
        assertEquals(NotchFit.HEIGHT.endInclusive, most.height, 0f)
        assertEquals(NotchFit.X.endInclusive, most.x, 0f)
        assertEquals(NotchFit.Y.endInclusive, most.y, 0f)
        val least = NotchFit.of(width = 0f, height = 0f, x = -500f, y = -500f)
        assertEquals(NotchFit.WIDTH.start, least.width, 0f)
        assertEquals(NotchFit.Y.start, least.y, 0f)
        assertTrue(NotchFit.X.endInclusive <= 16f && NotchFit.Y.endInclusive <= 16f)
        // In whole percent and whole dp, as a slider's every position lands.
        assertEquals(NotchFit(width = 1.13f, x = 3f), NotchFit.of(1.1312f, 1f, 2.6f, 0.2f))
        // Not a number is the default, not the smallest it can be.
        assertEquals(NotchFit(), NotchFit.of(Float.NaN, Float.NaN, Float.NaN, Float.NaN))
    }

    @Test
    fun `a fit is saved with the rest, and an early island's width is never read as one`() {
        val fitted = IslandSettings(on = true, fit = NotchFit(width = 1.2f, height = 0.9f, x = -4f, y = 2f))
        assertEquals(fitted, IslandSettings.fromJson(fitted.toJson()))
        assertEquals(NotchFit(), IslandSettings.fromJson(JSONObject("""{"width":1.3}""")).fit)
        assertEquals(NotchFit.X.start, IslandSettings.fromJson(JSONObject("""{"fitX":-99}""")).fit.x, 0f)
    }
}
