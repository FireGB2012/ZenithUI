package com.gabriel.zenithui.ui.connect

import androidx.compose.runtime.Stable
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.connect.Wearing
import com.gabriel.zenithui.data.server.SharedLook
import com.gabriel.zenithui.ui.settings.SettingsPreview

/**
 * What Zenith Connect reads of Zenith and asks of it. Everything Connect does for real goes through
 * here, to the view model the rest of Zenith uses: putting a setup on, taking it off again, the
 * Looks saved on this phone, and Zenith's own server with the account on it. Made again by the home
 * screen as what it holds changes; the functions in it are the view model's own.
 */
@Stable
class ConnectBridge(
    /** Whether this build has a server (server.properties). Without one nothing is ever sent, and Connect says so. */
    val server: Boolean,
    val settings: ZenithSettings,
    /** The settings as they are now, as a Look keeps them (JSON). */
    val currentSettings: String,
    /** The home screen's layout as it is now, as a Look keeps it (a JSON array): asked for only when it is needed. */
    val currentLayout: () -> String,
    /** Page one of your home screen, its apps, and Zenith's copy of your wallpaper, as the small previews draw them. */
    val home: SettingsPreview,
    /** The Looks saved on this phone, newest first. */
    val looks: List<Look>,
    val plus: Boolean,
    /** Who is signed in to a Zenith account on this phone, as Connect names them; null when nobody is. */
    val account: String?,
    /** When the account's backup was saved, if there is one. */
    val backupAt: Long?,
    /** What Connect put on last, while Undo can still take it off. */
    val worn: String?,
    /** Whether this phone may wear a wallpaper design, by its name: the ones marked Plus need Plus. */
    val owns: (String) -> Boolean,
    /** Puts a setup on: its name, what to wear (ConnectApply.wear), and the saved Look it is, if it is one of yours in full. */
    val wear: (name: String, wearing: Wearing, lookId: String) -> Unit,
    /** Takes off what Connect put on last. False when there was nothing to take off. */
    val takeOff: () -> Boolean,
    /** Whether the server answers; null for a build with none. */
    val serverUp: suspend () -> Boolean?,
    val featured: suspend () -> List<SharedLook>,
    /** A shared Look by its code, without putting it on: the Look, or why not. */
    val lookByCode: suspend (String) -> Pair<Look?, String?>,
    /** Saves the home screen as it is now as a Look with this name. */
    val saveLook: (String) -> Look,
    /** Shares a saved Look by its id: its code, or why not. */
    val shareLook: suspend (String) -> Pair<String?, String?>,
    val stopSharing: suspend (String) -> String?,
    val deleteLook: (String) -> Unit,
    val signIn: suspend (String, String) -> String?,
    val createAccount: suspend (String, String) -> String?,
    val signOut: suspend () -> Unit,
    val backUp: suspend () -> String?,
    val restore: suspend () -> String?,
    /** Shows what Zenith Plus is, for something only Plus does. */
    val needPlus: () -> Unit,
)
