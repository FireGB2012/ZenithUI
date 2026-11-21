package com.gabriel.zenithui.ui.connect

import android.os.SystemClock
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.connect.ConnectApply
import com.gabriel.zenithui.data.connect.ConnectCodes
import com.gabriel.zenithui.data.connect.ConnectLayers
import com.gabriel.zenithui.data.connect.ConnectReturn
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.ConnectStudio
import com.gabriel.zenithui.data.connect.Filter
import com.gabriel.zenithui.data.connect.Layer
import com.gabriel.zenithui.data.connect.ServerState
import com.gabriel.zenithui.data.connect.SetupSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Connect's tabs, in the order the tab bar has them. */
enum class ConnectTab(val label: String) {
    DISCOVER("Discover"),
    SHARE("Share"),
    VAULT("Vault"),
    UPDATES("Updates"),
}

/** The sheets Connect opens over itself. */
enum class ConnectSheet {
    /** Type or paste somebody's code. */
    CODE,

    /** Sign in to a Zenith account, or make one. */
    SIGN_IN,

    /** Share a Look: the home screen as it is, named first, or one already saved; then its code. */
    SHARE,

    /** Save the home screen as a Look, with a name. */
    SAVE,

    /** Put the account's backup back, which replaces the home screen: asked first. */
    RESTORE,
}

/** A setup going on: what, as much of it as was chosen, the lines the apply screen ticks, and what to say about it. */
@Immutable
class Applying(
    val setup: ConnectSetup,
    val on: Set<Layer>,
    val steps: List<String>,
    /** A photo of yours as the wallpaper was swapped for one of Zenith's designs. */
    val photoGone: Boolean,
    /** Its wallpaper is a design that needs Plus, so yours stayed. */
    val keptWallpaper: Boolean,
)

/**
 * Zenith Connect as it was left, kept by the home screen while Zenith runs, so that it opens again
 * where it was: the tab, a setup's page, what was typed. Not a setup it has put on, though: left
 * with one of those on show, it opens next time on the tab it was on (ConnectReturn, [left]). It
 * holds what came from Zenith's server, too, so opening Connect again does not ask for it again
 * for a minute.
 *
 * What it does, it does through [ConnectBridge], given each time: nothing here is sample data.
 */
@Stable
class ConnectState(private val scope: CoroutineScope) {

    var tab by mutableStateOf(ConnectTab.DISCOVER)
        private set
    var query by mutableStateOf("")
    var filter by mutableStateOf(Filter.ALL)

    /** The setup open on its own page, if one is, and the layers of it that are on. */
    var detail by mutableStateOf<ConnectSetup?>(null)
        private set
    var layers by mutableStateOf<Set<Layer>>(emptySet())
        private set

    /** A setup going on, with the apply screen up; how many of its steps are ticked, and whether it is all on. */
    var applying by mutableStateOf<Applying?>(null)
        private set
    var ticked by mutableIntStateOf(0)
        private set
    var applied by mutableStateOf(false)
        private set

    var server by mutableStateOf(ServerState.ASKING)
        private set
    /** The Looks the server's keeper features, each fetched by its code. */
    var featured by mutableStateOf<List<ConnectSetup>>(emptyList())
        private set
    var featuredLoading by mutableStateOf(false)
        private set
    /** Looks opened by their codes since Zenith started, newest first. */
    var opened by mutableStateOf<List<ConnectSetup>>(emptyList())
        private set

    var sheet by mutableStateOf<ConnectSheet?>(null)
        private set
    /** The saved Look the share sheet is for; null for the home screen as it is now. */
    var sheetLook by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    /** What went wrong with what a sheet or page was asked to do, in words. */
    var problem by mutableStateOf<String?>(null)
    /** A code just made, to show in the share sheet: the Look's name, and the code. */
    var madeCode by mutableStateOf<Pair<String, String>?>(null)
        private set
    /** A Look of yours the Delete button has been pressed once for: a second press deletes it. */
    var deleting by mutableStateOf<String?>(null)
        private set
    /** What was being written in Updates' problem report. */
    var report by mutableStateOf("")

    /** What to do once signed in, when signing in was asked for on the way to something. */
    private var afterSignIn: (() -> Unit)? = null

    /**
     * The Look saved for a share that did not go through: its name and its id. Share again under
     * the same name and that one is shared, rather than a copy saved each try, which, with only so
     * many Looks kept, would push your oldest one out.
     */
    private var savedForShare: Pair<String, String>? = null

    var toast by mutableStateOf<String?>(null)
        private set
    var toastTurn by mutableIntStateOf(0)
        private set
    private var toastJob: Job? = null

    val discoverList = LazyListState()
    val shareList = LazyListState()
    val vaultList = LazyListState()
    val updatesList = LazyListState()

    /** Whether Connect is on the screen now: see [shown] and [left]. */
    var visible by mutableStateOf(false)
        private set

    /** The setup Connect put on last, by its id, while it is still on: Undo takes it off. */
    private var putOn: String? = null

    /**
     * How many times Connect was left and let go of a setup it had put on ([left]). A picture of
     * Connect taken before then shows it where it no longer opens. Read outside of drawing.
     */
    var forgot = 0
        private set

    private var loadJob: Job? = null
    private var loadedAt = 0L

    fun list(of: ConnectTab): LazyListState = when (of) {
        ConnectTab.DISCOVER -> discoverList
        ConnectTab.SHARE -> shareList
        ConnectTab.VAULT -> vaultList
        ConnectTab.UPDATES -> updatesList
    }

    /** To [next]; the tab already on show goes back to its top, as iOS's tab bars do. */
    fun goTab(next: ConnectTab) {
        if (next == tab) {
            scope.launch { list(next).animateScrollToItem(0) }
        } else {
            tab = next
            problem = null
        }
    }

    // ------------------------------------------------------------------ the server

    /**
     * Asks the server how it is and what it features: when Connect opens, at most once a minute,
     * or now with [force] (Try again). A build with no server asks nothing.
     */
    fun load(bridge: ConnectBridge, force: Boolean = false) {
        if (!bridge.server) {
            server = ServerState.NONE
            return
        }
        if (loadJob?.isActive == true) return
        val now = SystemClock.elapsedRealtime()
        if (!force && server == ServerState.UP && loadedAt != 0L && now - loadedAt < RELOAD_MS) return
        loadJob = scope.launch {
            if (server != ServerState.UP || force) server = ServerState.ASKING
            featuredLoading = true
            try {
                server = when (bridge.serverUp()) {
                    null -> ServerState.NONE
                    false -> ServerState.DOWN
                    true -> ServerState.UP
                }
                if (server == ServerState.UP) {
                    val shared = bridge.featured().take(MOST_FEATURED)
                    // Each by its code, all at once: the list names them, the code holds the Look.
                    featured = coroutineScope {
                        shared.map { one ->
                            async {
                                bridge.lookByCode(one.code).first?.let { look ->
                                    ConnectSetup.of(look.copy(name = one.name.ifBlank { look.name }), SetupSource.FEATURED, one.code)
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    loadedAt = SystemClock.elapsedRealtime()
                }
            } finally {
                featuredLoading = false
            }
        }
    }

    // ------------------------------------------------------------------ a setup's page

    /** Opens [setup] on its page, with what it starts with on (ConnectLayers.startingOn). */
    fun open(setup: ConnectSetup, bridge: ConnectBridge) {
        detail = setup
        layers = ConnectLayers.startingOn(setup, ownPhoto = bridge.settings.wallpaperStyle.isEmpty())
        problem = null
        deleting = null
    }

    fun closeDetail() {
        detail = null
        deleting = null
    }

    fun toggle(layer: Layer) {
        layers = if (layer in layers) layers - layer else layers + layer
    }

    /**
     * Puts the open setup on Zenith, as much of it as is on: for real, through the view model,
     * with what was there before kept so Undo can put it back. The apply screen ticks through
     * what went on while the home screen settles under Connect.
     */
    fun apply(bridge: ConnectBridge) {
        val setup = detail ?: return
        if (applying != null) return
        val on = layers intersect ConnectLayers.available(setup)
        if (on.isEmpty()) {
            say("Turn on something to put on first.")
            return
        }
        // Switching to a Look of your own is Plus, as it is under Settings, Looks.
        if (setup.source == SetupSource.MINE && !bridge.plus) {
            bridge.needPlus()
            return
        }
        val wearing = ConnectApply.wear(bridge.currentSettings, bridge.currentLayout(), setup, on, bridge.owns)
        val whole = on.containsAll(ConnectLayers.available(setup))
        val lookId = if (setup.source == SetupSource.MINE && whole) setup.lookId else ""
        val steps = ConnectApply.steps(setup, on)
        applying = Applying(
            setup = setup,
            on = on,
            steps = steps,
            photoGone = ConnectApply.replacesPhoto(setup, on, ownPhoto = bridge.settings.wallpaperStyle.isEmpty()),
            keptWallpaper = wearing.keptWallpaper,
        )
        ticked = 0
        applied = false
        putOn = setup.id
        scope.launch {
            delay(ConnectApply.STEP_MS / 2)
            bridge.wear(setup.name, wearing, lookId)
            for (i in steps.indices) {
                delay(ConnectApply.STEP_MS)
                ticked = i + 1
            }
            delay(ConnectApply.DONE_PAUSE_MS)
            applied = true
            // On, with Connect already left to see it: it opens next time ready for the next one.
            if (!visible) letGoOfWorn()
        }
    }

    /** Takes off what was just put on: the home screen goes back to exactly what it was. */
    fun undo(bridge: ConnectBridge) {
        val name = bridge.worn
        val tookOff = bridge.takeOff()
        if (tookOff) putOn = null
        say(if (tookOff) "Took off ${name ?: "the setup"}. Your home screen is back." else "There's nothing to take off.")
        if (applied) applying = null
    }

    /** Connect is on the screen. */
    fun shown() {
        visible = true
    }

    /**
     * Connect has gone from the screen: into its icon, home, or under another app. Where it was
     * stays for next time, except a setup it put on (ConnectReturn): its apply screen and its page
     * go, and Connect opens on the tab it was on, ready for the next one.
     */
    fun left() {
        visible = false
        letGoOfWorn()
    }

    /** Lets go of the setup on show if it is one Connect has put on (ConnectReturn.forgets). */
    private fun letGoOfWorn() {
        val forgets = ConnectReturn.forgets(
            page = detail?.id,
            applyScreen = applying != null,
            applied = applied,
            putOn = putOn,
        )
        if (!forgets) return
        applying = null
        detail = null
        deleting = null
        forgot++
    }

    /** Leaves the apply screen once it is done, back to the setup's page. */
    fun leaveApply() {
        if (applied) applying = null
    }

    /** Deletes a Look of yours, on the second press of Delete. */
    fun delete(bridge: ConnectBridge, look: Look) {
        if (deleting != look.id) {
            deleting = look.id
            return
        }
        deleting = null
        bridge.deleteLook(look.id)
        if (detail?.lookId == look.id) detail = null
        say("Deleted ${look.name}")
    }

    // ------------------------------------------------------------------ sheets

    fun openSheet(which: ConnectSheet, look: String? = null) {
        sheet = which
        sheetLook = look
        problem = null
        busy = false
        madeCode = null
        // A word about what came before goes as the sheet comes up, so it never sits over the sheet.
        hush()
    }

    fun closeSheet() {
        sheet = null
        // Signing in given up on: what it was for does not happen either.
        afterSignIn = null
    }

    /** Finds a Look by its code and opens it on its page, without putting anything on. */
    fun lookUp(bridge: ConnectBridge, text: String) {
        if (busy) return
        val code = ConnectCodes.clean(text)
        if (code == null) {
            problem = "A Look's code is LOOK- and six letters and numbers."
            return
        }
        if (!bridge.server) {
            problem = "This build of Zenith has no server to ask."
            return
        }
        busy = true
        problem = null
        scope.launch {
            val (look, why) = try {
                bridge.lookByCode(code)
            } finally {
                busy = false
            }
            if (look == null) {
                problem = why ?: "That code opens nothing."
            } else {
                val setup = ConnectSetup.of(look, SetupSource.CODE, code)
                opened = (listOf(setup) + opened.filterNot { it.id == setup.id }).take(MOST_OPENED)
                sheet = null
                open(setup, bridge)
            }
        }
    }

    /** Signs in, or makes the account ([making]); then carries on with what it was asked on the way to. */
    fun signIn(bridge: ConnectBridge, username: String, password: String, making: Boolean) {
        if (busy) return
        val name = username.trim()
        if (name.isEmpty() || password.isEmpty()) {
            problem = "Type a username and a password."
            return
        }
        busy = true
        problem = null
        scope.launch {
            val why = try {
                if (making) bridge.createAccount(name, password) else bridge.signIn(name, password)
            } finally {
                busy = false
            }
            if (why != null) {
                problem = why
            } else {
                val next = afterSignIn
                afterSignIn = null
                sheet = null
                say(if (making) "Account made. You're signed in as $name." else "Signed in as $name")
                next?.invoke()
            }
        }
    }

    fun signOut(bridge: ConnectBridge) {
        scope.launch {
            bridge.signOut()
            say("Signed out. Everything on this phone stays.")
        }
    }

    /** Asks for whatever sharing needs that is missing (an account, then Plus), then does [then]. */
    private fun readyToShare(bridge: ConnectBridge, then: () -> Unit) {
        when {
            !bridge.server -> say("This build of Zenith has no server to share to.")
            bridge.account == null -> {
                openSheet(ConnectSheet.SIGN_IN)
                afterSignIn = then
            }
            !bridge.plus -> bridge.needPlus()
            else -> then()
        }
    }

    /** Shares the home screen as it is now: named first, in the share sheet. */
    fun shareHome(bridge: ConnectBridge) = readyToShare(bridge) { openSheet(ConnectSheet.SHARE) }

    /** Saves the home screen under [name] and shares it: its code comes up in the sheet. */
    fun shareNamed(bridge: ConnectBridge, name: String) {
        if (busy) return
        ConnectStudio.nameProblem(name)?.let {
            problem = it
            return
        }
        busy = true
        problem = null
        val named = name.trim().take(ConnectStudio.MAX_NAME)
        scope.launch {
            val (look, code, why) = try {
                val again = savedForShare?.takeIf { it.first == named }?.second?.let { id -> bridge.looks.firstOrNull { it.id == id } }
                val look = again ?: bridge.saveLook(named)
                val (code, why) = bridge.shareLook(look.id)
                Triple(look, code, why)
            } finally {
                busy = false
            }
            savedForShare = if (code == null) look.name to look.id else null
            if (code != null) madeCode = look.name to code else problem = why ?: "It couldn't be shared just now."
        }
    }

    /** Shares a Look already saved, or shows its code if it is shared already. */
    fun shareSaved(bridge: ConnectBridge, look: Look) = readyToShare(bridge) {
        openSheet(ConnectSheet.SHARE, look.id)
        if (look.shared.isNotEmpty() && !look.sharedIsOld) {
            madeCode = look.name to look.shared
            return@readyToShare
        }
        busy = true
        scope.launch {
            val (code, why) = try {
                bridge.shareLook(look.id)
            } finally {
                busy = false
            }
            if (code != null) madeCode = look.name to code else problem = why ?: "It couldn't be shared just now."
        }
    }

    fun stopSharing(bridge: ConnectBridge, look: Look) {
        scope.launch {
            val why = bridge.stopSharing(look.id)
            say(why ?: "${look.name} isn't shared any more. Its code opens nothing now.")
        }
    }

    /** Saving the home screen as a Look is Plus: asks for Plus, or opens the sheet that names it. */
    fun saveHome(bridge: ConnectBridge) {
        if (!bridge.plus) bridge.needPlus() else openSheet(ConnectSheet.SAVE)
    }

    fun saveNamed(bridge: ConnectBridge, name: String) {
        ConnectStudio.nameProblem(name)?.let {
            problem = it
            return
        }
        val look = bridge.saveLook(name.trim().take(ConnectStudio.MAX_NAME))
        sheet = null
        say("Saved as ${look.name}")
    }

    fun backUp(bridge: ConnectBridge) {
        when {
            !bridge.plus -> bridge.needPlus()
            bridge.account == null -> openSheet(ConnectSheet.SIGN_IN)
            busy -> Unit
            else -> {
                busy = true
                scope.launch {
                    val why = try {
                        bridge.backUp()
                    } finally {
                        busy = false
                    }
                    say(why ?: "Backed up to your account")
                }
            }
        }
    }

    fun askRestore(bridge: ConnectBridge) {
        when {
            !bridge.plus -> bridge.needPlus()
            bridge.account == null -> openSheet(ConnectSheet.SIGN_IN)
            else -> openSheet(ConnectSheet.RESTORE)
        }
    }

    fun restore(bridge: ConnectBridge) {
        if (busy) return
        busy = true
        scope.launch {
            val why = try {
                bridge.restore()
            } finally {
                busy = false
            }
            if (why == null) {
                sheet = null
                say("Your home screen is back from your account")
            } else {
                problem = why
            }
        }
    }

    // ------------------------------------------------------------------ the rest

    fun say(words: String) {
        toast = words
        toastTurn++
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(TOAST_MS)
            toast = null
        }
    }

    private fun hush() {
        toastJob?.cancel()
        toast = null
    }

    /**
     * Back: closes what is over everything else first: a sheet, the apply screen once it is done,
     * a setup's page, a search; then goes to Discover. False with nothing left to close, when
     * Connect itself closes.
     */
    fun back(): Boolean = when {
        sheet != null -> {
            closeSheet()
            true
        }
        applying != null -> {
            leaveApply()
            true
        }
        detail != null -> {
            closeDetail()
            true
        }
        tab == ConnectTab.DISCOVER && (query.isNotEmpty() || filter != Filter.ALL) -> {
            query = ""
            filter = Filter.ALL
            true
        }
        tab != ConnectTab.DISCOVER -> {
            tab = ConnectTab.DISCOVER
            true
        }
        else -> false
    }

    private companion object {
        /** How long what the server said is good for before Connect asks again on opening. */
        const val RELOAD_MS = 60_000L
        const val MOST_FEATURED = 12
        const val MOST_OPENED = 8
        const val TOAST_MS = 2_600L
    }
}
