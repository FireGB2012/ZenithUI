package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class DayPartsTest {

    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute)

    @Test
    fun `each part of the day runs from its hour to the next part's`() {
        assertEquals("night", DayParts.at(4 * 60 + 59).id)
        assertEquals("dawn", DayParts.at(5 * 60).id)
        assertEquals("day", DayParts.at(8 * 60).id)
        assertEquals("day", DayParts.at(16 * 60 + 59).id)
        assertEquals("dusk", DayParts.at(17 * 60).id)
        assertEquals("night", DayParts.at(20 * 60).id)
        assertEquals("night", DayParts.at(0).id)
        assertEquals(8 * 60, DayParts.until(DayParts.all.first()))
        assertEquals(5 * 60, DayParts.until(DayParts.all.last()))
    }

    @Test
    fun `a part's palette comes on once, as the part begins`() {
        val chosen = mapOf("dawn" to "sunset", "night" to "midnight")
        val (palette, occasion) = DayParts.due(chosen, at(2, 20, 1), last = "")!!
        assertEquals("midnight", palette)
        // Later the same night, with a palette chosen by hand meanwhile: left alone.
        assertNull(DayParts.due(chosen, at(2, 23, 30), last = occasion))
        // After midnight it is still that night.
        assertNull(DayParts.due(chosen, at(3, 2, 0), last = occasion))
        // Dawn brings its own.
        assertEquals("sunset", DayParts.due(chosen, at(3, 5, 0), last = occasion)?.first)
    }

    @Test
    fun `a part with no palette leaves the palette as it is`() {
        assertNull(DayParts.due(mapOf("night" to "midnight"), at(2, 12), last = ""))
        assertNull(DayParts.due(emptyMap(), at(2, 21), last = ""))
        assertNull(DayParts.due(mapOf("day" to ""), at(2, 12), last = ""))
    }

    @Test
    fun `the palettes read back as written, and parts nobody has heard of are dropped`() {
        val chosen = mapOf("dawn" to "sunset", "dusk" to "ember")
        assertEquals(chosen, DayParts.fromJson(DayParts.toJson(chosen)))
        assertEquals(mapOf("day" to "frost"), DayParts.fromJson(JSONObject("""{"day":"frost","teatime":"aurora"}""")))
        assertEquals(emptyMap<String, String>(), DayParts.fromJson(null))
    }
}
