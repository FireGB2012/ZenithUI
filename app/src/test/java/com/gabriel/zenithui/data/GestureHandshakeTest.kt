package com.gabriel.zenithui.data

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureHandshakeTest {

    @Test
    fun `the handshake is spoken in Launcher3's own words`() {
        // Android only hears an answer under exactly these names (GestureNavContract).
        assertEquals("gesture_nav_contract_v1", GestureHandshake.EXTRA_CONTRACT)
        assertEquals("gesture_nav_contract_icon_position", GestureHandshake.EXTRA_ICON_POSITION)
        assertEquals("gesture_nav_contract_surface_control", GestureHandshake.EXTRA_ICON_SURFACE)
        assertEquals("android.intent.extra.REMOTE_CALLBACK", GestureHandshake.EXTRA_REMOTE_CALLBACK)
        assertEquals("gesture_nav_contract_finish_callback", GestureHandshake.EXTRA_ON_FINISH_CALLBACK)
    }

    @Test
    fun `the icon's place is given on the whole screen`() {
        // Zenith fills the screen, so this is the place itself; anywhere else it is moved by as much.
        assertEquals(Rect(100f, 600f, 268f, 768f), GestureHandshake.onScreen(Rect(100f, 600f, 268f, 768f), 0, 0))
        assertEquals(Rect(100f, 680f, 268f, 848f), GestureHandshake.onScreen(Rect(100f, 600f, 268f, 768f), 0, 80))
    }

    @Test
    fun `the answer counts for a moment, and is only used once`() {
        val answer = GestureHandshake.Answer(key = "a/b", at = 1_000L, target = Rect(0f, 0f, 10f, 10f))
        assertEquals(true, answer.fresh(now = 1_000L + CloseRules.HANDSHAKE_FRESH_MS))
        assertEquals(false, answer.fresh(now = 1_001L + CloseRules.HANDSHAKE_FRESH_MS))
        assertEquals(false, answer.fresh(now = 999L))
    }
}
