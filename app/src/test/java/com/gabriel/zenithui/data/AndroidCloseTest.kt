package com.gabriel.zenithui.data

import com.gabriel.zenithui.data.AndroidClose.Make
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCloseTest {

    private fun plan(
        make: Make = Make.Samsung,
        gestures: Boolean = true,
        animators: Float = 1f,
        transitions: Float = 1f,
        sdk: Int = 35,
        chosen: Float = 0f,
    ) = AndroidClose.plan(make, gestures, animators, transitions, sdk, chosen)

    @Test
    fun `the make of phone is read from its maker and its brand`() {
        assertEquals(Make.Samsung, AndroidClose.make("samsung", "samsung"))
        assertEquals(Make.Pixel, AndroidClose.make("Google", "google"))
        // A Redmi or a POCO is a Xiaomi underneath, and runs the same system.
        assertEquals(Make.Xiaomi, AndroidClose.make("Xiaomi", "Redmi"))
        assertEquals(Make.Xiaomi, AndroidClose.make("Xiaomi", "POCO"))
        // OnePlus and realme run Oppo's ColorOS underneath, OxygenOS included.
        assertEquals(Make.Oppo, AndroidClose.make("OnePlus", "OnePlus"))
        assertEquals(Make.Oppo, AndroidClose.make("OPPO", "OPPO"))
        assertEquals(Make.Oppo, AndroidClose.make("realme", "realme"))
        assertEquals(Make.Vivo, AndroidClose.make("vivo", "iQOO"))
        assertEquals(Make.Honor, AndroidClose.make("HONOR", "HONOR"))
        assertEquals(Make.Honor, AndroidClose.make("HUAWEI", "HUAWEI"))
        // Close to Android as Google writes it, gestures and all.
        assertEquals(Make.Pixel, AndroidClose.make("motorola", "motorola"))
        assertEquals(Make.Pixel, AndroidClose.make("Nothing", "Nothing"))
        assertEquals(Make.Pixel, AndroidClose.make("Sony", "Sony"))
        assertEquals(Make.Pixel, AndroidClose.make("Fairphone", "Fairphone"))
        assertEquals(Make.Other, AndroidClose.make("TECNO MOBILE LIMITED", "TECNO"))
        assertEquals(Make.Other, AndroidClose.make("", ""))
    }

    @Test
    fun `gestures are read from Android's own setting, and from Samsung's and Xiaomi's own`() {
        assertTrue(AndroidClose.gestures(navigationMode = 2, samsungGestures = -1, xiaomiGestures = -1))
        assertFalse(AndroidClose.gestures(navigationMode = 0, samsungGestures = -1, xiaomiGestures = -1))
        // Two buttons, the old Pixel way, are buttons.
        assertFalse(AndroidClose.gestures(navigationMode = 1, samsungGestures = -1, xiaomiGestures = -1))
        // A Galaxy swiping up from the bottom only, One UI's own way, can leave Android's at buttons.
        assertTrue(AndroidClose.gestures(navigationMode = 0, samsungGestures = 1, xiaomiGestures = -1))
        assertTrue(AndroidClose.gestures(navigationMode = -1, samsungGestures = -1, xiaomiGestures = 1))
        assertFalse(AndroidClose.gestures(navigationMode = -1, samsungGestures = 0, xiaomiGestures = 0))
    }

    @Test
    fun `left at Auto, Zenith waits for Android to say its close is over, at least as long as the close takes`() {
        val galaxy = plan()!!
        assertTrue(galaxy.listens)
        assertEquals(AndroidClose.span(Make.Samsung, gestures = true).usualMs, galaxy.atLeastMs)
        assertEquals(AndroidClose.span(Make.Samsung, gestures = true).mostMs, galaxy.atMostMs)
        assertTrue(galaxy.atLeastMs < galaxy.atMostMs)
    }

    @Test
    fun `before Android 14 its word comes too early to go by, so the usual length is waited out`() {
        val old = plan(sdk = 33)!!
        assertFalse(old.listens)
        assertEquals(AndroidClose.span(Make.Samsung, gestures = true).usualMs, old.atLeastMs)
        assertEquals(old.atLeastMs, old.atMostMs)
    }

    @Test
    fun `a wait set by hand is kept to exactly, whatever Android says`() {
        val set = plan(chosen = 800f)!!
        assertFalse(set.listens)
        assertEquals(800L, set.atLeastMs)
        assertEquals(800L, set.atMostMs)
        // Not slowed or sped up with Android's animations either: it is the wait asked for.
        assertEquals(800L, plan(chosen = 800f, animators = 2f)!!.atLeastMs)
        // Below the first step it is Auto.
        assertTrue(plan(chosen = AndroidClose.WAIT_AUTO - 1f)!!.listens)
    }

    @Test
    fun `with Android's animations off there is nothing to wait for`() {
        assertNull(plan(animators = 0f))
        assertNull(plan(gestures = false, transitions = 0f))
        // A wait set by hand still holds.
        assertEquals(300L, plan(animators = 0f, chosen = 300f)!!.atLeastMs)
    }

    @Test
    fun `Android's animation speed stretches the wait, gestures by animators and buttons by transitions`() {
        val normal = plan()!!
        val slow = plan(animators = 2f)!!
        assertEquals(normal.atLeastMs * 2, slow.atLeastMs)
        assertEquals(normal.atMostMs * 2, slow.atMostMs)
        // A close with gestures is the phone's launcher moving the app on its own animators; one
        // with buttons is a window animation of Android's.
        assertEquals(plan(gestures = false)!!.atLeastMs, plan(gestures = false, animators = 2f)!!.atLeastMs)
        assertEquals(plan(gestures = false)!!.atLeastMs * 2, plan(gestures = false, transitions = 2f)!!.atLeastMs)
    }

    @Test
    fun `every make waits a beat and never a second and a half`() {
        for (make in Make.entries) for (gestures in listOf(true, false)) {
            val span = AndroidClose.span(make, gestures)
            assertTrue("$make $gestures", span.usualMs in 200L..span.mostMs)
            assertTrue("$make $gestures", span.mostMs <= 1500L)
        }
        // Android's own with buttons: the app drops off the foot of the screen in 225 ms.
        assertEquals(250L, AndroidClose.span(Make.Pixel, gestures = false).usualMs)
        // Oppo's, OnePlus's and realme's are known to show a launcher not their own a second late.
        assertTrue(AndroidClose.span(Make.Oppo, gestures = true).mostMs >= 1200L)
    }

    @Test
    fun `it sets off when Android says it is done, but not before the close could be over, and not after the most it takes`() {
        val wait = AndroidClose.Plan(atLeastMs = 400, atMostMs = 1000, listens = true)
        // Said at 600 ms after coming back: then.
        assertEquals(1600L, AndroidClose.setOffAt(wait, startedAt = 1000, saidDoneAt = 1600))
        // Said too soon to be believed: at the least it takes.
        assertEquals(1400L, AndroidClose.setOffAt(wait, startedAt = 1000, saidDoneAt = 1100))
        // Never said: at the most.
        assertEquals(2000L, AndroidClose.setOffAt(wait, startedAt = 1000, saidDoneAt = null))
        assertEquals(2000L, AndroidClose.setOffAt(wait, startedAt = 1000, saidDoneAt = 2500))
        // Not listening: the wait as it is, whatever was said.
        val set = AndroidClose.Plan(atLeastMs = 700, atMostMs = 700, listens = false)
        assertEquals(1700L, AndroidClose.setOffAt(set, startedAt = 1000, saidDoneAt = 1100))
        assertEquals(1700L, AndroidClose.setOffAt(set, startedAt = 1000, saidDoneAt = null))
    }
}
