package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuraLadderTest {

    private fun states(streak: Int) = Aura.ladder(streak).map { it.state }

    @Test
    fun `before a streak has begun the ladder starts at day one, and day one is next`() {
        val ladder = Aura.ladder(0)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), ladder.map { it.day })
        assertEquals(AuraDayState.Next, ladder.first().state)
        assertTrue(ladder.drop(1).all { it.state == AuraDayState.Later })
        assertEquals(listOf(10, 15, 20, 25, 30, 35, 40), ladder.map { it.reward })
    }

    @Test
    fun `days behind you are done, today is marked, tomorrow is next`() {
        assertEquals(
            listOf(
                AuraDayState.Done, AuraDayState.Done, AuraDayState.Today, AuraDayState.Next,
                AuraDayState.Later, AuraDayState.Later, AuraDayState.Later,
            ),
            states(3),
        )
        assertEquals(AuraDayState.Today, states(1).first())
        assertEquals(AuraDayState.Next, states(1)[1])
    }

    @Test
    fun `a long streak shows the days round where it stands, not the first week`() {
        val ladder = Aura.ladder(10)
        assertEquals(listOf(7, 8, 9, 10, 11, 12, 13), ladder.map { it.day })
        assertEquals(AuraDayState.Today, ladder.first { it.day == 10 }.state)
        assertEquals(AuraDayState.Next, ladder.first { it.day == 11 }.state)
        assertTrue(ladder.filter { it.day < 10 }.all { it.state == AuraDayState.Done })
        // Past the top of the ladder every day pays the most a day can.
        assertTrue(ladder.filter { it.day >= 9 }.all { it.reward == Aura.MAX_DAILY })
    }

    @Test
    fun `whatever the streak there are seven days, one of them next, and never more than one today`() {
        for (streak in listOf(-2, 0, 1, 2, 5, 9, 40, 365)) {
            val ladder = Aura.ladder(streak)
            assertEquals("streak $streak", 7, ladder.size)
            assertEquals("streak $streak", 1, ladder.count { it.state == AuraDayState.Next })
            assertEquals("streak $streak", if (streak >= 1) 1 else 0, ladder.count { it.state == AuraDayState.Today })
            assertEquals("streak $streak", ladder.map { it.day }, ladder.map { it.day }.sorted())
            ladder.forEach { assertEquals(Aura.dailyReward(it.day), it.reward) }
        }
    }

    @Test
    fun `what you can already pay for takes no days`() {
        assertEquals(0, Aura.daysToAfford(balance = 150, price = 150, streak = 4))
        assertEquals(0, Aura.daysToAfford(balance = 900, price = 150, streak = 0))
    }

    @Test
    fun `the days to a price are counted up the ladder from where the streak stands`() {
        // From nothing: 10, 15, 20, 25, 30, 35, 40 comes to 175 on the seventh day.
        assertEquals(7, Aura.daysToAfford(balance = 0, price = 150, streak = 0))
        // At the top of the ladder every day is 50.
        assertEquals(3, Aura.daysToAfford(balance = 0, price = 150, streak = 20))
        // Ten short, and tomorrow pays fifteen.
        assertEquals(1, Aura.daysToAfford(balance = 140, price = 150, streak = 1))
        // A streak below nought is no streak.
        assertEquals(7, Aura.daysToAfford(balance = 0, price = 150, streak = -3))
    }

    @Test
    fun `what the points are being saved towards is the cheapest theme not yet owned`() {
        assertEquals("ember", AuraThemes.next(emptyList())?.id)
        assertEquals("tide", AuraThemes.next(listOf("ember"))?.id)
        // Whatever order they were bought in.
        assertEquals("ember", AuraThemes.next(listOf("obsidian", "tide"))?.id)
        assertEquals(null, AuraThemes.next(AuraThemes.all.map { it.id }))
    }
}
