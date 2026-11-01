package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerRulesTest {

    private val alpha = AppInfo("a", "A", "Alpha")
    private val beta = AppInfo("b", "B", "Beta")
    private val gamma = AppInfo("g", "G", "Gamma", system = true)
    private val apps = listOf(alpha, beta, gamma)

    @Test
    fun `most used puts the apps opened most first, the rest A to Z`() {
        val counts = mapOf(gamma.key to 9, beta.key to 2)
        assertEquals(listOf(gamma, beta, alpha), DrawerSort.sort(apps, DrawerSort.USED, counts))
        // Ties keep the A to Z order.
        assertEquals(apps, DrawerSort.sort(apps, DrawerSort.USED, emptyMap()))
    }

    @Test
    fun `newest puts the apps added last first, the phone's own after them`() {
        val installed = mapOf(alpha.key to 100L, beta.key to 300L, gamma.key to 999L)
        assertEquals(listOf(beta, alpha, gamma), DrawerSort.sort(apps, DrawerSort.NEW, installedAt = { installed[it] ?: 0L }))
    }

    @Test
    fun `by colour goes round the wheel, the greys last`() {
        val colours = mapOf(
            alpha.key to IconColour.of(30, 90, 230),   // blue
            beta.key to IconColour.of(220, 40, 40),    // red
            gamma.key to IconColour.of(128, 128, 128), // grey
        )
        assertEquals(listOf(beta, alpha, gamma), DrawerSort.sort(apps, DrawerSort.COLOUR, colours = colours))
        assertEquals(12, IconColour.of(250, 250, 250).band())
        assertEquals(0, IconColour.of(230, 20, 20).band())
        assertEquals(4, IconColour.of(20, 200, 40).band())
    }

    @Test
    fun `an unknown order is A to Z`() {
        assertEquals(apps, DrawerSort.sort(apps, "shuffled"))
        assertEquals(DrawerSort.AZ, DrawerSort.of("shuffled"))
        DrawerSort.ids.forEach { assertTrue(DrawerSort.detail(it).isNotBlank() && DrawerSort.short(it).isNotBlank()) }
    }

    @Test
    fun `counting openings never grows without end`() {
        var counts = emptyMap<String, Int>()
        repeat(3) { counts = DrawerSort.counted(counts, "a/A") }
        assertEquals(3, counts["a/A"])
        for (i in 0 until 200) counts = DrawerSort.counted(counts, "app$i/X")
        assertTrue(counts.size <= 120)
        // What is opened most is what is kept.
        assertEquals(3, counts["a/A"])
    }

    @Test
    fun `a tab survives being written and read back, and one with no name is not read`() {
        val tab = DrawerTab("Games", listOf("a/A", "b/B"))
        assertEquals(tab, DrawerTab.fromJson(JSONObject(tab.toJson().toString())))
        assertNull(DrawerTab.fromJson(JSONObject("""{"name":"  ","keys":["a/A"]}""")))
    }

    @Test
    fun `a work app's key is never a personal app's`() {
        val work = AppInfo("a", "A", "Alpha", work = true)
        assertEquals("work:a/A", work.key)
        assertTrue(WorkKeys.isWork(work.key))
        assertEquals(alpha.key, WorkKeys.appKey(work.key))
    }

    @Test
    fun `sums are answered by the usual rules`() {
        assertEquals(8.0, Calculator.evaluate("2+2*3")!!, 1e-9)
        assertEquals(7.0, Calculator.evaluate("(4.5 − 1) × 2")!!, 1e-9)
        assertEquals(1024.0, Calculator.evaluate("2^10")!!, 1e-9)
        assertEquals(2.5, Calculator.evaluate("10 ÷ 4")!!, 1e-9)
        assertEquals(12.0, Calculator.evaluate("15% of 80")!!, 1e-9)
        assertEquals(92.0, Calculator.evaluate("80 + 15%")!!, 1e-9)
        assertEquals(-6.0, Calculator.evaluate("-2*3")!!, 1e-9)
    }

    @Test
    fun `what is not a sum is not answered`() {
        assertNull(Calculator.evaluate("spotify"))
        assertNull(Calculator.evaluate("2024"))
        assertNull(Calculator.evaluate("3d"))
        assertNull(Calculator.evaluate("1/0"))
        assertNull(Calculator.evaluate("2+"))
        assertNull(Calculator.evaluate(""))
    }

    @Test
    fun `answers are written as people write them`() {
        assertEquals("8", Calculator.show(8.0))
        assertEquals("0.3333333333", Calculator.show(1.0 / 3.0))
        assertEquals("2.5", Calculator.show(2.5))
    }

    @Test
    fun `conversions between units of a kind`() {
        assertEquals(Conversion("5 km", "3.11 miles"), Units.convert("5 km in miles"))
        assertEquals(Conversion("30 °C", "86 °F"), Units.convert("30 c to f"))
        assertEquals(Conversion("2 cups", "480 ml"), Units.convert("2 cups in ml"))
        assertEquals(Conversion("100 mph", "160.9 km/h"), Units.convert("100 mph to km/h"))
        assertEquals(Conversion("5 in", "12.7 cm"), Units.convert("5 in to cm"))
        assertEquals(Conversion("1 GB", "1024 MB"), Units.convert("1 gb in mb"))
        // Not two of a kind, or not units at all.
        assertNull(Units.convert("5 km in kg"))
        assertNull(Units.convert("5 apples in pears"))
        assertNull(Units.convert("hello"))
    }

    @Test
    fun `Android's settings are found by their names and other words for them`() {
        assertEquals("Bluetooth", SettingsLinks.find("blue").first().title)
        assertEquals("Battery", SettingsLinks.find("battery").first().title)
        assertTrue(SettingsLinks.find("wifi").any { it.title == "Wi-Fi" })
        assertTrue(SettingsLinks.find("dnd").any { it.title == "Do Not Disturb" })
        assertTrue(SettingsLinks.find("x").isEmpty())
        assertTrue(SettingsLinks.find("zenith").any { it.action == SettingsLinks.ZENITH })
    }
}
