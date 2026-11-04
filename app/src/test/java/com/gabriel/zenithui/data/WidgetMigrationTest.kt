package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetMigrationTest {

    @Test
    fun `a no-glass widget on a page is read as its solid self, and keeps everything else`() {
        val read = homeItemFromJson(
            JSONObject("""{"t":"widget","id":"w1","p":1,"c":2,"r":3,"type":"bare_clock","sx":2,"sy":4,"acc":4278255360}"""),
        ) as WidgetCell
        assertEquals("clock", read.type)
        assertEquals("w1", read.id)
        assertEquals(1, read.page)
        assertEquals(2, read.col)
        assertEquals(3, read.row)
        assertEquals(2, read.spanX)
        assertEquals(4, read.spanY)
        assertEquals(4278255360L, read.accent)
    }

    @Test
    fun `a solid widget is read as it was`() {
        val read = homeItemFromJson(JSONObject("""{"t":"widget","id":"w2","p":0,"c":0,"r":0,"type":"weather","sx":4,"sy":4}""")) as WidgetCell
        assertEquals("weather", read.type)
    }

    @Test
    fun `a no-glass widget in a stack is read as its solid self`() {
        val read = homeItemFromJson(
            JSONObject(
                """{"t":"stack","id":"s1","p":0,"c":0,"r":0,"sx":2,"sy":4,"members":[""" +
                    """{"id":"a","type":"bare_weather"},{"id":"b","type":"clock"},{"id":"c","wid":7,"prov":"x/y"}]}""",
            ),
        ) as StackCell
        assertEquals(listOf("weather", "clock", ""), read.members.map { it.type })
        assertEquals(listOf("a", "b", "c"), read.members.map { it.id })
    }

    @Test
    fun `the catalogue knows a no-glass type as the solid widget`() {
        assertEquals(Widgets.Clock, Widgets.byType("bare_clock"))
        assertEquals(Widgets.Clock, Widgets.byType("clock"))
        assertEquals(Widgets.Weather, Widgets.byType("bare_weather"))
    }

    @Test
    fun `only the no-glass mark is taken off`() {
        assertEquals("clock", Widgets.solid("bare_clock"))
        assertEquals("clock", Widgets.solid("clock"))
        assertEquals("", Widgets.solid(""))
    }
}
