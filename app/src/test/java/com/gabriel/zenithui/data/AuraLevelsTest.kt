package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuraLevelsTest {

    @Test
    fun `everybody starts at level one, and each level asks fifty more than the last`() {
        assertEquals(listOf(0, 0, 50, 150, 300, 500, 750, 1_050, 1_400, 1_800, 2_250, 2_750, 3_300), (0..12).map { AuraLevels.threshold(it) })
        for (level in 2..60) {
            val step = AuraLevels.threshold(level + 1) - AuraLevels.threshold(level)
            assertEquals("level $level", 50 * level, step)
        }
    }

    @Test
    fun `a level is reached on its threshold exactly, not one short of it`() {
        assertEquals(1, AuraLevels.of(0))
        assertEquals(1, AuraLevels.of(49))
        assertEquals(2, AuraLevels.of(50))
        assertEquals(2, AuraLevels.of(149))
        assertEquals(3, AuraLevels.of(150))
        assertEquals(12, AuraLevels.of(3_300))
        assertEquals(11, AuraLevels.of(3_299))
        for (level in 1..200) {
            assertEquals("level $level", level, AuraLevels.of(AuraLevels.threshold(level)))
            if (level > 1) assertEquals("level $level", level - 1, AuraLevels.of(AuraLevels.threshold(level) - 1))
        }
    }

    @Test
    fun `nothing earned, or a nonsense number, is level one and never throws`() {
        assertEquals(1, AuraLevels.of(-5))
        assertEquals(1, AuraLevels.of(Int.MIN_VALUE))
        val top = AuraLevels.of(Int.MAX_VALUE)
        assertTrue(top > 9_000)
        assertTrue(AuraLevels.threshold(top) <= Int.MAX_VALUE)
        assertEquals(0f, AuraLevels.progress(-5), 0f)
        assertTrue(AuraLevels.progress(Int.MAX_VALUE) in 0f..1f)
    }

    @Test
    fun `progress runs from nought at a level to just short of one before the next`() {
        assertEquals(0f, AuraLevels.progress(0), 0f)
        assertEquals(0.5f, AuraLevels.progress(25), 0.0001f)
        assertEquals(0f, AuraLevels.progress(50), 0f)
        assertEquals(0.5f, AuraLevels.progress(100), 0.0001f)
        assertEquals(50, AuraLevels.toNext(0))
        assertEquals(1, AuraLevels.toNext(149))
        assertEquals(150, AuraLevels.toNext(150))
    }

    @Test
    fun `every level up to twelve brings one thing, flames and colour sets taking turns`() {
        val brought = (2..12).map { AuraLevels.reward(it) }
        assertTrue(brought.all { it != null })
        assertEquals(
            listOf("blue", "ember", "purple", "tide", "green", "orchid", "gold", "solstice", "rose", "obsidian", "white"),
            brought.map {
                when (it) {
                    is AuraReward.Flame -> it.flame.id
                    is AuraReward.Colours -> it.set.id
                    else -> "?"
                }
            },
        )
        brought.forEachIndexed { index, reward -> assertEquals(index + 2, reward!!.level) }
        assertNull(AuraLevels.reward(1))
        assertNull(AuraLevels.reward(0))
    }

    @Test
    fun `every Aura set and every flame but the first is somewhere on the way`() {
        val sets = AuraLevels.rewards.filterIsInstance<AuraReward.Colours>().map { it.set.id }
        assertEquals(AuraThemes.all.map { it.id }, sets)
        val flames = AuraLevels.rewards.filterIsInstance<AuraReward.Flame>().map { it.flame }
        assertEquals(AuraFlame.entries.drop(1), flames)
        AuraFlame.entries.forEach { flame -> assertEquals(flame.id, flame.level, AuraLevels.levelOf(flame)) }
        assertEquals(1, AuraFlame.Orange.level)
    }

    @Test
    fun `each reward has a name of its own`() {
        assertEquals("Blue flame", AuraLevels.reward(2)!!.title)
        assertEquals("Ember colours", AuraLevels.reward(3)!!.title)
        assertEquals("A day of Plus", AuraLevels.reward(13)!!.title)
        assertEquals(11, AuraLevels.rewards.map { it.title }.distinct().size)
    }

    @Test
    fun `past the last of those, each level is a day of Plus`() {
        assertEquals(12, AuraLevels.LAST_REWARD)
        for (level in 13..40) assertEquals(AuraReward.PlusDay(level), AuraLevels.reward(level))
    }

    @Test
    fun `what comes next is the reward of the next level`() {
        assertEquals(AuraReward.Flame(AuraFlame.Blue), AuraLevels.next(1))
        assertEquals(AuraReward.Colours(AuraThemes.byId("tide")!!, 5), AuraLevels.next(4))
        assertEquals(AuraReward.PlusDay(13), AuraLevels.next(12))
        assertEquals(AuraReward.PlusDay(21), AuraLevels.next(20))
    }

    @Test
    fun `the days it takes from nothing, turning up every day, are the ones the spec gives`() {
        val days = (2..12).map { Aura.daysToAfford(balance = 0, price = AuraLevels.threshold(it), streak = 0) }
        assertEquals(listOf(4, 7, 10, 14, 19, 25, 32, 40, 49, 59, 70), days)
    }

    @Test
    fun `the level comes from what was earned, and never stands below what is in hand`() {
        assertEquals(1, AuraLevels.of(ZenithSettings()))
        assertEquals(5, AuraLevels.of(ZenithSettings(aura = 10, auraEarned = 600)))
        // Settings with aura in hand but nothing marked earned: whatever wrote them, the aura was earned.
        assertEquals(3, AuraLevels.of(ZenithSettings(aura = 200, auraEarned = 0)))
        assertEquals(200, Aura.earned(ZenithSettings(aura = 200, auraEarned = 0)))
    }

    @Test
    fun `settings from before levels count what the old shop cost back into what was earned`() {
        val old = JSONObject(ZenithSettings().toJson().toString()).apply {
            remove("auraEarned")
            put("aura", 120)
            put("unlockedThemes", org.json.JSONArray(listOf("ember", "tide")))
        }
        val read = ZenithSettings.fromJson(old)
        assertEquals(120, read.aura)
        assertEquals(120 + 150 + 250, read.auraEarned)
        // A hand-written number below what is in hand is read as written, and counted as the aura in hand.
        val odd = ZenithSettings.fromJson(JSONObject(ZenithSettings().toJson().toString()).put("aura", 500).put("auraEarned", 20))
        assertEquals(20, odd.auraEarned)
        assertEquals(500, Aura.earned(odd))
        assertEquals(AuraLevels.of(500), AuraLevels.of(odd))
        // Earning puts it right.
        assertEquals(510, AuraShop.earn(odd, 10, 0L).settings.auraEarned)
    }

    @Test
    fun `a flame is worn once its level is reached, and the orange one otherwise`() {
        assertEquals(AuraFlame.Orange, AuraFlame.worn(ZenithSettings()))
        assertEquals(AuraFlame.Orange, AuraFlame.worn(ZenithSettings(auraFlame = "gold", auraEarned = 0)))
        assertEquals(AuraFlame.Gold, AuraFlame.worn(ZenithSettings(auraFlame = "gold", auraEarned = AuraLevels.threshold(8))))
        assertEquals(AuraFlame.Orange, AuraFlame.worn(ZenithSettings(auraFlame = "plaid", auraEarned = 99_999)))
        assertEquals(AuraFlame.Orange, AuraFlame.byId(null))
    }

    @Test
    fun `a flame's mark stands out on a white card, as the widget asks of any colour it wears`() {
        // Orange is the widget's own from before flames, and gives way to the card's ink there as it always has.
        for (flame in AuraFlame.entries - AuraFlame.Orange) {
            assertTrue("${flame.id} on white", contrast(flame.markLight, 0xFFFFFFFF) >= 2.6)
            assertTrue("${flame.id} on dark glass", contrast(flame.markDark, 0xFF1C1C1E) >= 2.6)
        }
    }

    /** WCAG's contrast between two opaque colours. */
    private fun contrast(a: Long, b: Long): Double {
        fun channel(c: Long): Double {
            val v = c / 255.0
            return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        fun luminance(c: Long) = 0.2126 * channel((c shr 16) and 0xFF) + 0.7152 * channel((c shr 8) and 0xFF) + 0.0722 * channel(c and 0xFF)
        val (x, y) = luminance(a) to luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    @Test
    fun `a colour set is yours by level, or because it was bought before levels`() {
        assertFalse(AuraLevels.hasColours(ZenithSettings(), "ember"))
        assertTrue(AuraLevels.hasColours(ZenithSettings(auraEarned = AuraLevels.threshold(3)), "ember"))
        assertFalse(AuraLevels.hasColours(ZenithSettings(auraEarned = AuraLevels.threshold(3)), "tide"))
        assertTrue(AuraLevels.hasColours(ZenithSettings(unlockedThemes = listOf("obsidian")), "obsidian"))
        assertEquals(listOf("ember", "tide"), AuraLevels.colourSets(ZenithSettings(auraEarned = AuraLevels.threshold(6))))
        assertEquals(listOf("ember", "obsidian"), AuraLevels.colourSets(ZenithSettings(auraEarned = 150, unlockedThemes = listOf("obsidian"))))
        // A colour set nobody can reach is nobody's.
        assertFalse(AuraLevels.hasColours(ZenithSettings(auraEarned = 999_999), "midnight"))
    }
}
