package com.gabriel.zenithui.ui.connect

import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.settings.SettingsPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Sharing the home screen saves it as a Look first. Only so many Looks are kept, so a share that
 * fails (the server out of reach) and is tried again must share the Look it already saved, not
 * save another each try and push the oldest of yours out.
 */
class ShareAgainTest {

    private val saved = mutableListOf<Look>()
    private var answer: Pair<String?, String?> = null to "Zenith's server can't be reached."

    /** The bridge as the home screen makes it again after every change: holding the Looks saved so far. */
    private fun bridge() = ConnectBridge(
        server = true,
        settings = ZenithSettings(),
        currentSettings = "{}",
        currentLayout = { "[]" },
        home = SettingsPreview(emptyList(), emptyMap(), null, 0.dp, 0.dp),
        looks = saved.toList(),
        plus = true,
        account = "rig",
        backupAt = null,
        worn = null,
        owns = { true },
        wear = { _, _, _ -> },
        takeOff = { false },
        serverUp = { true },
        featured = { emptyList() },
        lookByCode = { null to null },
        saveLook = { name -> Look("look-${saved.size}", name, "{}", "[]", 0L).also { saved.add(0, it) } },
        shareLook = { answer },
        stopSharing = { null },
        deleteLook = { id -> saved.removeAll { it.id == id } },
        signIn = { _, _ -> null },
        createAccount = { _, _ -> null },
        signOut = {},
        backUp = { null },
        restore = { null },
        needPlus = {},
    )

    private val state = ConnectState(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `a share that failed is tried again with the Look it saved, not a new one`() {
        state.shareNamed(bridge(), "Night")
        state.shareNamed(bridge(), " Night ")
        assertEquals(1, saved.size)
        assertEquals("Zenith's server can't be reached.", state.problem)

        answer = "BCDFGH" to null
        state.shareNamed(bridge(), "Night")
        assertEquals(1, saved.size)
        assertEquals("Night" to "BCDFGH", state.madeCode)
    }

    @Test
    fun `once shared, or under another name, a share saves the home screen as it is now`() {
        state.shareNamed(bridge(), "Night")
        // Another name is another Look.
        state.shareNamed(bridge(), "Day")
        assertEquals(listOf("Day", "Night"), saved.map { it.name })

        answer = "BCDFGH" to null
        state.shareNamed(bridge(), "Day")
        assertEquals(2, saved.size)
        // Shared, the next share is of the home screen as it is then.
        state.shareNamed(bridge(), "Day")
        assertEquals(3, saved.size)
    }

    @Test
    fun `a Look deleted after a failed share is saved afresh, not shared from nowhere`() {
        state.shareNamed(bridge(), "Night")
        saved.clear()
        answer = "BCDFGH" to null
        state.shareNamed(bridge(), "Night")
        assertEquals(1, saved.size)
        assertNull(state.problem)
        assertEquals("Night" to "BCDFGH", state.madeCode)
    }
}
