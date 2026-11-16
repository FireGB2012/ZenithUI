package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizingTest {

    private fun started(apps: Int) = Optimizing.start(apps, startedAtMs = 1_000L)

    @Test
    fun `it starts with every job to do, the icons and the drawer counted in apps`() {
        val o = started(118)
        assertEquals(OptimizeJob.entries, o.jobs.map { it.job })
        assertEquals(118, o.of(OptimizeJob.ICONS).total)
        assertEquals(118, o.of(OptimizeJob.DRAWER).total)
        assertEquals(1, o.of(OptimizeJob.SAVE).total)
        assertEquals(0f, o.fraction, 0f)
        assertEquals(OptimizeJob.APPS, o.current?.job)
        assertFalse(o.finished)
    }

    @Test
    fun `how far it has got is the work done, each job weighed by what it costs`() {
        val o = started(100).with(OptimizeJob.APPS, 1, 1).with(OptimizeJob.ICONS, 50, 100)
        // Apps 4, icons 1 each, the drawer a quarter each, glass 6, graphics 2, saving 1.
        val whole = 4f + 100f + 25f + 6f + 2f + 1f
        assertEquals((4f + 50f) / whole, o.fraction, 0.0001f)
        assertEquals(OptimizeJob.ICONS, o.current?.job)
    }

    @Test
    fun `what is said is the job in hand, with how many of how many when there are many`() {
        val o = started(118).with(OptimizeJob.APPS, 1, 1).with(OptimizeJob.ICONS, 42, 118)
        assertEquals("Caching app icons… 42 of 118", OptimizeWords.line(o, glassy = true))
        val drawer = o.with(OptimizeJob.ICONS, 118, 118).with(OptimizeJob.DRAWER, 7, 118)
        assertEquals("Building the app drawer… 7 of 118", OptimizeWords.line(drawer, glassy = true))
        assertEquals("Reading your apps…", OptimizeWords.line(started(118), glassy = true))
    }

    @Test
    fun `the glass is only called glass in a theme made of it`() {
        val o = OptimizeJob.entries.takeWhile { it != OptimizeJob.GLASS }
            .fold(started(3)) { at, job -> at.with(job, at.of(job).total, at.of(job).total) }
        assertEquals("Preparing the glass…", OptimizeWords.line(o, glassy = true))
        assertEquals("Preparing your wallpaper…", OptimizeWords.line(o, glassy = false))
    }

    @Test
    fun `when everything is done it is all set, at a hundred percent`() {
        val o = OptimizeJob.entries.fold(started(10)) { at, job -> at.with(job, at.of(job).total, at.of(job).total) }
        assertTrue(o.finished)
        assertNull(o.current)
        assertEquals(1f, o.fraction, 0f)
        assertEquals("All set", OptimizeWords.line(o, glassy = true))
        assertEquals("100%", OptimizeWords.percent(o))
    }

    @Test
    fun `it never says a hundred percent before it is finished`() {
        val almost = OptimizeJob.entries.fold(started(1000)) { at, job ->
            if (job == OptimizeJob.SAVE) at else at.with(job, at.of(job).total, at.of(job).total)
        }
        assertFalse(almost.finished)
        assertEquals("99%", OptimizeWords.percent(almost))
    }

    @Test
    fun `a phone with no apps to read has nothing to divide by, and its icons are done`() {
        val o = started(0).with(OptimizeJob.APPS, 1, 1)
        assertTrue(o.of(OptimizeJob.ICONS).finished)
        assertTrue(o.of(OptimizeJob.DRAWER).finished)
        assertEquals(OptimizeJob.GLASS, o.current?.job)
        assertTrue(o.fraction > 0f && o.fraction < 1f)
    }

    @Test
    fun `a count is kept within its job, and the apps read can change how many icons there are`() {
        val o = started(10).with(OptimizeJob.ICONS, 25, 10)
        assertEquals(10, o.of(OptimizeJob.ICONS).done)
        val more = started(10).with(OptimizeJob.ICONS, 3, 12)
        assertEquals(12, more.of(OptimizeJob.ICONS).total)
        assertEquals(3, more.of(OptimizeJob.ICONS).done)
    }

    @Test
    fun `the time left is not guessed until there is enough to go on`() {
        val o = started(100).with(OptimizeJob.APPS, 1, 1)
        // A twentieth done, but only a moment in.
        val early = o.with(OptimizeJob.ICONS, 20, 100)
        assertNull(early.leftMs(nowMs = 1_000L + 200L))
        // Hardly anything done, however long it has taken.
        assertNull(started(100).leftMs(nowMs = 1_000L + 5_000L))
    }

    @Test
    fun `the time left is the time so far, spread over what is still to do`() {
        val o = started(100).with(OptimizeJob.APPS, 1, 1).with(OptimizeJob.ICONS, 100, 100)
        val elapsed = 4_000L
        val f = o.fraction
        val expected = elapsed * (1f - f) / f
        assertEquals(expected.toDouble(), o.leftMs(nowMs = 1_000L + elapsed)!!.toDouble(), 2.0)
        // And none is left once it is done.
        val done = OptimizeJob.entries.fold(o) { at, job -> at.with(job, at.of(job).total, at.of(job).total) }
        assertEquals(0L, done.leftMs(nowMs = 1_000L + elapsed))
    }

    @Test
    fun `the time left is said in words a person would use`() {
        assertEquals("Working out how long…", OptimizeWords.left(null))
        assertEquals("Almost done", OptimizeWords.left(900L))
        assertEquals("About 3 seconds left", OptimizeWords.left(2_400L))
        assertEquals("About 45 seconds left", OptimizeWords.left(44_100L))
        assertEquals("About a minute left", OptimizeWords.left(70_000L))
        assertEquals("About 3 minutes left", OptimizeWords.left(170_000L))
    }
}
