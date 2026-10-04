package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperTrustTest {

    private fun current(
        justSet: Boolean = false,
        storedId: Int = 0,
        currentId: Int = -1,
        storedColours: String? = null,
        currentColours: String? = null,
    ) = WallpaperTrust.isCurrent(justSet, storedId, currentId, storedColours) { currentColours }

    @Test
    fun `a wallpaper Zenith has only just set is its own, before anything else is known`() {
        assertTrue(current(justSet = true))
        assertTrue(current(justSet = true, storedId = 4, currentId = 5))
    }

    @Test
    fun `the copy is current while Android shows the wallpaper under the number it gave it`() {
        // A Samsung never announces the colours of a wallpaper an app has set: the number alone
        // has to be enough, days later and after any number of restarts.
        assertTrue(current(storedId = 81, currentId = 81))
        assertTrue(current(storedId = 81, currentId = 81, storedColours = "1,2,3", currentColours = "9,9,9"))
    }

    @Test
    fun `another number means the wallpaper was changed somewhere else, even to one of the same colours`() {
        assertFalse(current(storedId = 81, currentId = 82))
        assertFalse(current(storedId = 81, currentId = 82, storedColours = "1,2,3", currentColours = "1,2,3"))
    }

    @Test
    fun `a copy kept before numbers were noted is known by its colours`() {
        assertTrue(current(currentId = 22, storedColours = "1,2,3", currentColours = "1,2,3"))
        assertFalse(current(currentId = 22, storedColours = "1,2,3", currentColours = "1,2,4"))
        // Neither a number nor colours on record: nothing vouches for it.
        assertFalse(current(currentId = 22, currentColours = "1,2,3"))
        assertFalse(current(currentId = 22, storedColours = "1,2,3"))
    }

    @Test
    fun `when Android will not say the number, the colours decide`() {
        assertTrue(current(storedId = 81, currentId = -1, storedColours = "1,2,3", currentColours = "1,2,3"))
        assertFalse(current(storedId = 81, currentId = -1))
    }

    @Test
    fun `the colours are not asked for when the number has already decided`() {
        var asked = 0
        WallpaperTrust.isCurrent(justSet = false, storedId = 81, currentId = 81, storedColours = "1,2,3") { asked++; "1,2,3" }
        WallpaperTrust.isCurrent(justSet = false, storedId = 81, currentId = 82, storedColours = "1,2,3") { asked++; "1,2,3" }
        WallpaperTrust.isCurrent(justSet = true, storedId = 0, currentId = -1, storedColours = "1,2,3") { asked++; "1,2,3" }
        assertEquals(0, asked)
    }

    @Test
    fun `the number of a wallpaper just set is the one Android hands back`() {
        assertEquals(79, WallpaperTrust.idAfterSet(before = 78, returned = 79, after = 79))
        // Handed back before Android has finished changing over: it is still the new one's.
        assertEquals(79, WallpaperTrust.idAfterSet(before = 78, returned = 79, after = 78))
    }

    @Test
    fun `when nothing is handed back the number is read off the wallpaper, if it has changed`() {
        assertEquals(79, WallpaperTrust.idAfterSet(before = 78, returned = 0, after = 79))
        assertEquals(79, WallpaperTrust.idAfterSet(before = -1, returned = 0, after = 79))
    }

    @Test
    fun `a call that made no new wallpaper has no number`() {
        // Android took the call and left the wallpaper as it was: a phone whose owner may not
        // change it does that, and so may one handed the very picture it already shows.
        assertEquals(0, WallpaperTrust.idAfterSet(before = 78, returned = 0, after = 78))
        assertEquals(0, WallpaperTrust.idAfterSet(before = -1, returned = 0, after = -1))
        assertEquals(0, WallpaperTrust.idAfterSet(before = 78, returned = -1, after = 78))
    }
}
