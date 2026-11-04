package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class NewInstallDefaultsTest {

    @Test
    fun `a new install starts in apple blue and inter`() {
        val fresh = ZenithSettings()
        assertEquals("frost", fresh.paletteId)
        assertEquals("inter", fresh.font)
    }

    @Test
    fun `somebody who chose midnight and the phone's font keeps them`() {
        val saved = ZenithSettings.fromJson(JSONObject("""{"onboarded":true,"paletteId":"midnight","font":"system"}"""))
        assertEquals("midnight", saved.paletteId)
        assertEquals("system", saved.font)
    }
}
