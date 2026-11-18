package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuraShopTest {

    private val now = 1_800_000_000_000L
    private val day = AuraShop.DAY_MS
    private val nobody = PlusStatus(trialAvailable = false)

    private fun bought(spend: AuraSpend): ZenithSettings = (spend as AuraSpend.Done).settings
    private fun refused(spend: AuraSpend): String = (spend as AuraSpend.Refused).why

    // ------------------------------------------------------------------------------- earning

    @Test
    fun `earning puts aura in hand and into what was earned`() {
        val after = AuraShop.earn(ZenithSettings(aura = 40, auraEarned = 400), 25, now).settings
        assertEquals(65, after.aura)
        assertEquals(425, after.auraEarned)
    }

    @Test
    fun `earning across a level says which levels were reached`() {
        val start = ZenithSettings(aura = 0, auraEarned = 140)
        val once = AuraShop.earn(start, 20, now)
        assertEquals(listOf(3), once.reached)
        val twice = AuraShop.earn(ZenithSettings(auraEarned = 0), 300, now)
        assertEquals(listOf(2, 3, 4), twice.reached)
        assertEquals(emptyList<Int>(), AuraShop.earn(start, 5, now).reached)
    }

    @Test
    fun `nothing, or less than nothing, earns nothing`() {
        val start = ZenithSettings(aura = 30, auraEarned = 300)
        assertEquals(start, AuraShop.earn(start, 0, now).settings)
        assertEquals(start, AuraShop.earn(start, -50, now).settings)
    }

    @Test
    fun `a level past the last reward adds a day of Plus, on top of any day still running`() {
        val nearly = ZenithSettings(auraEarned = AuraLevels.threshold(13) - 10)
        val reached = AuraShop.earn(nearly, 10, now).settings
        assertEquals(now + day, reached.plusPassUntil)
        val running = nearly.copy(plusPassUntil = now + 5 * 3_600_000L)
        assertEquals(now + 5 * 3_600_000L + day, AuraShop.earn(running, 10, now).settings.plusPassUntil)
        // Two such levels at once are two days.
        val far = ZenithSettings(auraEarned = AuraLevels.threshold(13) - 10)
        val two = AuraShop.earn(far, AuraLevels.threshold(14) - AuraLevels.threshold(13) + 10, now).settings
        assertEquals(now + 2 * day, two.plusPassUntil)
        // A level that brings a flame or a colour set brings no day.
        assertEquals(0L, AuraShop.earn(ZenithSettings(auraEarned = 140), 20, now).settings.plusPassUntil)
    }

    // ------------------------------------------------------------------------------- a day of Plus

    @Test
    fun `a day of Plus costs its price and runs a day from now`() {
        val after = bought(AuraShop.buyPlusDay(ZenithSettings(aura = 300, auraEarned = 900), nobody, now))
        assertEquals(300 - AuraShop.PLUS_DAY, after.aura)
        assertEquals(900, after.auraEarned)
        assertEquals(now + day, after.plusPassUntil)
    }

    @Test
    fun `another day goes on the end of the one running, and an old one that ran out is forgotten`() {
        val running = ZenithSettings(aura = 600, plusPassUntil = now + 3_600_000L)
        assertEquals(now + 3_600_000L + day, bought(AuraShop.buyPlusDay(running, PlusStatus(passUntil = now + 3_600_000L, trialAvailable = false), now)).plusPassUntil)
        val over = ZenithSettings(aura = 600, plusPassUntil = now - 3 * day)
        assertEquals(now + day, bought(AuraShop.buyPlusDay(over, nobody, now)).plusPassUntil)
    }

    @Test
    fun `a day of Plus is not sold to somebody who has Plus already`() {
        val rich = ZenithSettings(aura = 5_000)
        assertEquals("Plus is yours already.", refused(AuraShop.buyPlusDay(rich, PlusStatus(purchased = true, lifetime = true), now)))
        assertEquals("Plus is yours already.", refused(AuraShop.buyPlusDay(rich, PlusStatus(purchased = true, subscribed = true), now)))
        assertEquals("Plus is on for your free week.", refused(AuraShop.buyPlusDay(rich, PlusStatus(trialDaysLeft = 3, trialAvailable = false), now)))
    }

    @Test
    fun `without enough aura it says how much more is needed`() {
        assertEquals("You need 50 more aura.", refused(AuraShop.buyPlusDay(ZenithSettings(aura = 200), nobody, now)))
        assertEquals("You need 250 more aura.", refused(AuraShop.buyPlusDay(ZenithSettings(aura = -4), nobody, now)))
    }

    // ------------------------------------------------------------------------------- Plus wallpapers

    @Test
    fun `the wallpapers aura buys are the eleven Plus designs, in the picker's order`() {
        assertEquals(WallStyle.entries.filter { it.plus }, AuraShop.designs)
        assertEquals(11, AuraShop.designs.size)
    }

    @Test
    fun `a Plus wallpaper costs its price and is yours for good`() {
        val after = bought(AuraShop.buyDesign(ZenithSettings(aura = 1_200, auraEarned = 3_000), WallStyle.Nebula, nobody))
        assertEquals(200, after.aura)
        assertEquals(3_000, after.auraEarned)
        assertEquals(listOf("Nebula"), after.auraDesigns)
        assertTrue(AuraShop.ownsDesign(after, plusActive = false, style = WallStyle.Nebula))
        assertFalse(AuraShop.ownsDesign(after, plusActive = false, style = WallStyle.Loop))
    }

    @Test
    fun `a wallpaper is not sold twice, nor one that is free, nor to somebody with Plus for good`() {
        val owner = ZenithSettings(aura = 5_000, auraDesigns = listOf("Loop"))
        assertEquals("It is yours already.", refused(AuraShop.buyDesign(owner, WallStyle.Loop, nobody)))
        assertEquals("That one is free.", refused(AuraShop.buyDesign(owner, WallStyle.entries.first { !it.plus }, nobody)))
        assertEquals("Plus is yours for good, and has it.", refused(AuraShop.buyDesign(owner, WallStyle.Halo, PlusStatus(purchased = true, lifetime = true))))
        // A subscription can end, so a subscriber may buy one to keep.
        assertTrue(AuraShop.buyDesign(owner, WallStyle.Halo, PlusStatus(purchased = true, subscribed = true)) is AuraSpend.Done)
        assertEquals("You need 400 more aura.", refused(AuraShop.buyDesign(ZenithSettings(aura = 600), WallStyle.Halo, nobody)))
    }

    @Test
    fun `a design is yours if it is free, with Plus on, or bought with aura`() {
        val free = WallStyle.entries.first { !it.plus }
        assertTrue(AuraShop.ownsDesign(ZenithSettings(), plusActive = false, style = free))
        assertTrue(AuraShop.ownsDesign(ZenithSettings(), plusActive = true, style = WallStyle.Eclipse))
        assertFalse(AuraShop.ownsDesign(ZenithSettings(), plusActive = false, style = WallStyle.Eclipse))
        assertTrue(AuraShop.ownsDesign(ZenithSettings(auraDesigns = listOf("Eclipse")), plusActive = false, style = WallStyle.Eclipse))
    }

    // ------------------------------------------------------------------------------- Plus itself

    @Test
    fun `Plus is on while a day bought with aura runs, and says so`() {
        val onAura = PlusStatus(passUntil = now + day, trialAvailable = false)
        assertTrue(onAura.active)
        assertTrue(onAura.onAura)
        assertFalse(onAura.inTrial)
        assertFalse(PlusStatus(passUntil = 0L, trialAvailable = false).active)
        // Bought or in the free week, it is not "on aura", whatever is left of a day.
        assertFalse(PlusStatus(purchased = true, passUntil = now + day).onAura)
        assertFalse(PlusStatus(trialDaysLeft = 2, passUntil = now + day).onAura)
    }

    @Test
    fun `a day of Plus that has run out is no day at all`() {
        assertEquals(now + day, AuraShop.passUntil(now + day, now))
        assertEquals(0L, AuraShop.passUntil(now - 1, now))
        assertEquals(0L, AuraShop.passUntil(now, now))
        assertEquals(0L, AuraShop.passUntil(0L, now))
    }

    @Test
    fun `a backup never brings a day of Plus with it, nor sends one to the account`() {
        val phone = ZenithSettings(plusPassUntil = now + day, aura = 10)
        val file = ZenithSettings(plusPassUntil = now + 30 * day, aura = 900, auraEarned = 4_000, auraFlame = "blue", auraDesigns = listOf("Loop"))
        val restored = AuraShop.restoring(file, phone)
        assertEquals(now + day, restored.plusPassUntil)
        // What is yours travels: the aura, what was earned, the flame and the wallpapers.
        assertEquals(900, restored.aura)
        assertEquals(4_000, restored.auraEarned)
        assertEquals("blue", restored.auraFlame)
        assertEquals(listOf("Loop"), restored.auraDesigns)
        assertEquals(0L, com.gabriel.zenithui.data.server.AccountRules.forAccount(file).plusPassUntil)
    }

    // ------------------------------------------------------------------------------- words

    @Test
    fun `when a day of Plus ends is said as somebody would say it`() {
        val zone = java.time.ZoneId.of("Europe/Copenhagen")
        val saved = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.UK)
        try {
            val morning = java.time.ZonedDateTime.of(2026, 11, 17, 11, 20, 0, 0, zone).toInstant().toEpochMilli()
            assertEquals("15:00", AuraWords.until(morning + 3 * 3_600_000L + 40 * 60_000L, morning, zone))
            assertEquals("11:20 tomorrow", AuraWords.until(morning + day, morning, zone))
            assertEquals("Thu 19 Nov, 11:20", AuraWords.until(morning + 2 * day, morning, zone))
            assertEquals("1,240", AuraWords.count(1_240))
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }
}
