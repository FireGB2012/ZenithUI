package com.gabriel.zenithui.ui.connect

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.gabriel.zenithui.data.connect.Channel
import com.gabriel.zenithui.data.connect.ConnectAccent
import com.gabriel.zenithui.data.connect.ConnectApply
import com.gabriel.zenithui.data.connect.ConnectBeta
import com.gabriel.zenithui.data.connect.ConnectCommunity
import com.gabriel.zenithui.data.connect.ConnectMessage
import com.gabriel.zenithui.data.connect.ConnectSamples
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.ConnectStudio
import com.gabriel.zenithui.data.connect.ConnectVault
import com.gabriel.zenithui.data.connect.DownloadState
import com.gabriel.zenithui.data.connect.FeedTab
import com.gabriel.zenithui.data.connect.Layer
import com.gabriel.zenithui.data.connect.RoadmapItem
import com.gabriel.zenithui.data.connect.VaultLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Connect's four tabs, in the order the tab bar has them. */
enum class ConnectTab(val label: String) {
    DISCOVER("Discover"),
    COMMUNITY("Community"),
    BETA("Beta"),
    VAULT("Vault"),
}

/**
 * Everything Connect is showing and has been told, kept for as long as Zenith runs, the way an
 * app stays as it was left: closed into its icon and opened again, it is where you left it, and
 * what it had started (a download, an apply, a post on its way in) has carried on meanwhile on
 * [scope], which is Zenith's own. Only the colour chosen for it and the setups saved are kept for
 * longer, in its own small store on the phone (see [load]), never in Zenith's settings or a Look.
 *
 * [clock] is the time in milliseconds that the things that run are measured by.
 */
@Stable
class ConnectState(
    private val scope: CoroutineScope,
    val clock: () -> Long = { android.os.SystemClock.uptimeMillis() },
) {

    var tab by mutableStateOf(ConnectTab.DISCOVER)

    /** Whether Connect is on the screen now, rather than closed into its icon. */
    var visible by mutableStateOf(false)

    // ------------------------------------------------------------------ where each page is scrolled to

    val discoverGrid = LazyStaggeredGridState()
    val communityList = LazyListState()
    val betaScroll = ScrollState(0)
    val vaultList = LazyListState()
    val detailScroll = ScrollState(0)

    // ------------------------------------------------------------------ Discover

    var feedTab by mutableStateOf(FeedTab.WEEK)
    var query by mutableStateOf("")
    var searching by mutableStateOf(false)

    /** Every setup Discover has: the samples, and any published from this phone, newest first. */
    val setups = mutableStateListOf<ConnectSetup>().apply { addAll(ConnectSamples.setups) }

    /** The setup just published, outlined in the accent for a few seconds. */
    var fresh by mutableStateOf<String?>(null)

    /** Setups saved to the Vault, by id. */
    val saved = mutableStateListOf<String>()

    /** The setup whose page is open, and the card it grew out of (in the window), if there was one. */
    var detail by mutableStateOf<ConnectSetup?>(null)
        private set
    var detailFrom by mutableStateOf<Rect?>(null)
        private set

    /** Bumped each time a setup's page opens, so it plays its opening from the start. */
    var detailRun by mutableIntStateOf(0)
        private set

    /** Which layers of the open setup are to go on: all of them, until one is turned off. */
    var layersOn by mutableStateOf(Layer.entries.toSet())

    // ------------------------------------------------------------------ apply

    /** The setup being put on, while the apply screen is up, and the layers it was asked for. */
    var applying by mutableStateOf<ConnectSetup?>(null)
        private set
    var applyLayers by mutableStateOf(Layer.entries.toSet())
        private set

    /** When Apply was pressed, by [clock]: everything on the apply screen is worked out from it. */
    var applyStartedAt by mutableLongStateOf(0L)
        private set

    // ------------------------------------------------------------------ Creator Studio

    var studioOpen by mutableStateOf(false)
        private set
    var studioName by mutableStateOf(ConnectStudio.DEFAULT_NAME)
    var studioAbout by mutableStateOf("")
    val studioTags = mutableStateListOf<String>().apply { addAll(ConnectStudio.DEFAULT_TAGS) }
    val studioParts = mutableStateListOf<String>().apply { addAll(ConnectStudio.PARTS) }
    var studioPublic by mutableStateOf(true)
    var publish by mutableStateOf(DownloadState.IDLE)
        private set
    var publishStartedAt by mutableLongStateOf(0L)
        private set
    var nameError by mutableStateOf(false)

    /** Bumped to shake what was refused: the name field, or the tag count. */
    var nameShake by mutableIntStateOf(0)
        private set
    var tagShake by mutableIntStateOf(0)
        private set

    /** Bumped each time the Studio opens, so its capture plays again. */
    var studioRun by mutableIntStateOf(0)
        private set

    var accentOpen by mutableStateOf(false)

    // ------------------------------------------------------------------ Community

    var channel by mutableStateOf(Channel.ANNOUNCEMENTS)
        private set
    val messages = mutableStateListOf<ConnectMessage>().apply { addAll(ConnectSamples.messages) }
    val liked = mutableStateListOf<String>()
    val voted = mutableStateListOf<String>()

    /** Channels with a post you have not seen, and whether the Community tab has one. */
    val unread = mutableStateListOf<Channel>()
    var communityDot by mutableStateOf(false)
        private set

    /** The post that has just come in live, flashed as it lands. */
    var landed by mutableStateOf<String?>(null)
        private set
    private var welcomeScheduled = false

    // ------------------------------------------------------------------ Beta Hub

    var download by mutableStateOf(DownloadState.IDLE)
        private set
    var downloadStartedAt by mutableLongStateOf(0L)
        private set
    var reporting by mutableStateOf(false)
        private set
    var report by mutableStateOf("")
    var reportError by mutableStateOf(false)
    var reportShake by mutableIntStateOf(0)
        private set
    val roadmap = mutableStateListOf<RoadmapItem>().apply { addAll(ConnectSamples.roadmap) }

    // ------------------------------------------------------------------ Vault

    var autoSync by mutableStateOf(true)
        private set
    var syncing by mutableStateOf(false)
        private set
    var lastSync by mutableStateOf("2 min ago")
        private set
    var syncedByHand by mutableStateOf(false)
        private set

    /** Bumped when a sync finishes, for the cloud's little pop. */
    var syncRun by mutableIntStateOf(0)
        private set
    val layouts = mutableStateListOf<VaultLayout>().apply { addAll(ConnectSamples.layouts) }
    var inUse by mutableStateOf("daily")
        private set

    /** Connect's colour: Zenith's own blue unless another was chosen. */
    var accent by mutableStateOf(ConnectSamples.accents.first())
        private set

    // ------------------------------------------------------------------ the toast

    /** The line in the pill over the tab bar, and a count that makes the same words show again. */
    var toast by mutableStateOf<String?>(null)
        private set
    var toastTurn by mutableIntStateOf(0)
        private set
    private var toastJob: Job? = null

    fun say(words: String) {
        toast = words
        toastTurn++
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(TOAST_MS)
            toast = null
        }
    }

    // ------------------------------------------------------------------ moving about

    /** To [to], from wherever Connect is: a setup's page open over it closes first; the tab you are on goes back to its top. */
    fun goTab(to: ConnectTab) {
        if (detail != null) closeSetup()
        if (to == tab) {
            scope.launch { toTop(to) }
            return
        }
        tab = to
        if (to == ConnectTab.COMMUNITY) enterCommunity()
    }

    private suspend fun toTop(which: ConnectTab) {
        when (which) {
            ConnectTab.DISCOVER -> discoverGrid.animateScrollToItem(0)
            ConnectTab.COMMUNITY -> communityList.animateScrollToItem(0)
            ConnectTab.BETA -> betaScroll.animateScrollTo(0)
            ConnectTab.VAULT -> vaultList.animateScrollToItem(0)
        }
    }

    /**
     * Back, as Android's back does inside an app: whatever is open over the page closes, the
     * topmost first, and search folds away. False once there is nothing left to close, when back
     * leaves Connect.
     */
    fun back(): Boolean {
        when {
            accentOpen -> accentOpen = false
            studioOpen -> closeStudio()
            applying != null -> leaveApply()
            detail != null -> closeSetup()
            searching -> closeSearch()
            else -> return false
        }
        return true
    }

    fun openSearch() {
        searching = true
    }

    /** Search folds away, and what was searched for with it. */
    fun closeSearch() {
        searching = false
        query = ""
    }

    // ------------------------------------------------------------------ a setup's page

    fun openSetup(setup: ConnectSetup, from: Rect?) {
        detail = setup
        detailFrom = from
        layersOn = Layer.entries.toSet()
        detailRun++
        scope.launch { detailScroll.scrollTo(0) }
    }

    fun closeSetup() {
        detail = null
    }

    /** One of the open setup's layers on or off. */
    fun toggleLayer(layer: Layer) {
        layersOn = if (layer in layersOn) layersOn - layer else layersOn + layer
    }

    fun share(setup: ConnectSetup, copy: (String) -> Boolean) {
        val link = ConnectApply.link(setup.name)
        say(if (copy(link)) "Copied $link" else "Share link: $link")
    }

    fun save(setup: ConnectSetup) {
        say(if (toggleSaved(setup)) "Saved ${setup.name} to Vault" else "Removed from Vault")
    }

    // ------------------------------------------------------------------ apply

    /** Apply: the apply screen comes up and runs through its steps, or with movement reduced is done at once. */
    fun startApply(reduced: Boolean) {
        val setup = detail ?: return
        if (!ConnectApply.canApply(setup, layersOn)) return
        applyLayers = layersOn
        applyStartedAt = clock() - if (reduced) ConnectApply.DONE_MS else 0L
        applying = setup
    }

    /** How long ago Apply was pressed. */
    fun applyElapsed(): Long = (clock() - applyStartedAt).coerceAtLeast(0L)

    /** Undo: what was put on comes off again, or, not done yet, it stops with nothing changed. */
    fun undoApply() {
        val done = applyElapsed() >= ConnectApply.DONE_MS
        applying = null
        say(if (done) "Restored your previous layout" else "Stopped. Your home screen is unchanged")
    }

    /** Back to Discover: the apply screen goes, then the setup's page, onto Discover. */
    fun backToDiscover(reduced: Boolean) {
        applying = null
        scope.launch {
            if (!reduced) delay(160)
            closeSetup()
            if (tab != ConnectTab.DISCOVER) tab = ConnectTab.DISCOVER
        }
    }

    /**
     * The apply screen goes, pulled away, its dim tapped, or Zenith opened from it, and what it
     * was doing carries on behind it: the setup's page is there under it as it was.
     */
    fun leaveApply() {
        applying = null
    }

    // ------------------------------------------------------------------ Creator Studio

    fun openStudio() {
        if (studioOpen) return
        if (publish != DownloadState.BUSY) publish = DownloadState.IDLE
        nameError = false
        studioRun++
        studioOpen = true
    }

    fun closeStudio() {
        studioOpen = false
    }

    fun toggleTag(tag: String) {
        val next = ConnectStudio.toggleTag(studioTags.toList(), tag)
        if (next == null) {
            tagShake++
            say("Up to ${ConnectStudio.MAX_TAGS} tags per setup")
            return
        }
        studioTags.clear()
        studioTags.addAll(next)
    }

    fun togglePart(part: String) {
        if (part in studioParts) studioParts.remove(part) else studioParts.add(part)
    }

    /**
     * Publishes what was captured, as [by]: the button fills, says it is done, and the sheet goes.
     * A public setup goes to the top of New releases, outlined for a moment; an unlisted one is a link.
     */
    fun publish(by: String) {
        if (publish == DownloadState.BUSY) return
        val name = studioName.trim()
        if (ConnectStudio.nameProblem(name) != null) {
            nameError = true
            nameShake++
            return
        }
        val public = studioPublic
        publish = DownloadState.BUSY
        publishStartedAt = clock()
        scope.launch {
            delay(ConnectStudio.PUBLISH_MS)
            publish = DownloadState.DONE
            delay(ConnectStudio.PUBLISHED_HOLD_MS)
            studioOpen = false
            if (public) publishNew(name, by) else say("Share link ready: ${ConnectApply.link(name)}")
        }
    }

    private fun publishNew(name: String, by: String) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
        val mine = ConnectStudio.published(name, by, now, studioTags.toList(), "mine-${clock()}")
        setups.add(0, mine)
        if (detail != null) closeSetup()
        tab = ConnectTab.DISCOVER
        closeSearch()
        feedTab = FeedTab.NEW
        fresh = mine.id
        scope.launch { discoverGrid.scrollToItem(0) }
        scope.launch {
            delay(FRESH_MS)
            if (fresh == mine.id) fresh = null
        }
        say("$name is live in Discover")
    }

    // ------------------------------------------------------------------ Community

    private fun enterCommunity() {
        communityDot = false
        if (welcomeScheduled) return
        welcomeScheduled = true
        scope.launch {
            delay(ConnectSamples.WELCOME_AFTER_MS)
            val post = ConnectSamples.welcome
            messages.add(0, post)
            val (channelDot, tabDot) = ConnectCommunity.dotsForLivePost(visible && tab == ConnectTab.COMMUNITY, channel)
            if (channelDot) {
                if (Channel.ANNOUNCEMENTS !in unread) unread.add(Channel.ANNOUNCEMENTS)
            } else {
                landed = post.id
                delay(LANDED_MS)
                if (landed == post.id) landed = null
            }
            if (tabDot) communityDot = true
        }
    }

    fun pickChannel(to: Channel) {
        channel = to
        unread.remove(to)
    }

    /** The Changelog button: Community's #update-log. */
    fun openChangelog() {
        goTab(ConnectTab.COMMUNITY)
        pickChannel(Channel.UPDATE_LOG)
    }

    fun like(message: ConnectMessage) {
        val at = messages.indexOfFirst { it.id == message.id }
        if (at < 0) return
        val (on, count) = ConnectCommunity.flip(message.id in liked, messages[at].likes)
        if (on) liked.add(message.id) else liked.remove(message.id)
        messages[at] = messages[at].copy(likes = count)
    }

    fun vote(message: ConnectMessage) {
        val at = messages.indexOfFirst { it.id == message.id }
        if (at < 0) return
        val (on, count) = ConnectCommunity.flip(message.id in voted, messages[at].votes)
        if (on) voted.add(message.id) else voted.remove(message.id)
        messages[at] = messages[at].copy(votes = count)
    }

    /** "Open in Discover": the setup a post shows, on its own page. */
    fun openFromPost(id: String) {
        val setup = setups.firstOrNull { it.id == id } ?: return
        tab = ConnectTab.DISCOVER
        openSetup(setup, null)
    }

    // ------------------------------------------------------------------ Beta Hub

    fun startDownload() {
        when (download) {
            DownloadState.BUSY -> return
            DownloadState.DONE -> {
                say("Open the APK from your downloads to install")
                return
            }
            DownloadState.IDLE -> Unit
        }
        download = DownloadState.BUSY
        downloadStartedAt = clock()
        scope.launch {
            delay(ConnectBeta.DOWNLOAD_MS)
            download = DownloadState.DONE
            say("${ConnectSamples.BUILD_FILE} downloaded")
        }
    }

    /** How far the download has got, 0 to 1. */
    fun downloadProgress(): Float = when (download) {
        DownloadState.IDLE -> 0f
        DownloadState.DONE -> 1f
        DownloadState.BUSY -> ((clock() - downloadStartedAt).toFloat() / ConnectBeta.DOWNLOAD_MS).coerceIn(0f, 1f)
    }

    fun toggleReport() {
        reporting = !reporting
        if (!reporting) reportError = false
    }

    fun sendReport() {
        if (report.isBlank()) {
            reportError = true
            reportShake++
            return
        }
        reportError = false
        report = ""
        reporting = false
        say("Report sent with device info and logs")
    }

    fun voteRoad(item: RoadmapItem) {
        val at = roadmap.indexOfFirst { it.id == item.id }
        if (at < 0) return
        val (on, count) = ConnectCommunity.flip(roadmap[at].mine, roadmap[at].votes)
        roadmap[at] = roadmap[at].copy(mine = on, votes = count)
    }

    // ------------------------------------------------------------------ Vault

    fun toggleAutoSync() {
        autoSync = !autoSync
        syncedByHand = false
    }

    fun syncNow(reduced: Boolean) {
        if (syncing) return
        syncing = true
        scope.launch {
            delay(if (reduced) 200L else ConnectVault.SYNC_MS)
            syncing = false
            lastSync = "just now"
            syncedByHand = !autoSync
            syncRun++
            say("Layouts and themes synced")
        }
    }

    fun restore(layout: VaultLayout) {
        inUse = layout.id
        say("Restoring ${layout.name} in Zenith")
    }

    fun backUp(now: LocalDateTime) {
        val current = layouts.firstOrNull { it.id == inUse } ?: layouts.firstOrNull() ?: return
        layouts.add(
            0,
            VaultLayout(
                id = "b${clock()}",
                name = ConnectVault.backupName(now),
                meta = ConnectVault.backupMeta(current.meta),
                wall = current.wall,
                dark = current.dark,
            ),
        )
        say("Backed up your current layout")
    }

    // ------------------------------------------------------------------ kept on the phone

    private var store: android.content.SharedPreferences? = null

    /** Reads back the colour and the saved setups, once. */
    fun load(context: Context) {
        if (store != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        store = prefs
        val name = prefs.getString(KEY_ACCENT, null)
        ConnectSamples.accents.firstOrNull { it.name == name }?.let { accent = it }
        prefs.getStringSet(KEY_SAVED, null)?.let { kept ->
            saved.clear()
            saved.addAll(kept.filter { id -> setups.any { it.id == id } })
        }
    }

    fun chooseAccent(choice: ConnectAccent) {
        accent = choice
        store?.edit()?.putString(KEY_ACCENT, choice.name)?.apply()
    }

    /** Saves [setup] to the Vault, or takes it out; true when it is now saved. */
    fun toggleSaved(setup: ConnectSetup): Boolean {
        val now = setup.id !in saved
        if (now) saved.add(setup.id) else saved.remove(setup.id)
        store?.edit()?.putStringSet(KEY_SAVED, saved.toSet())?.apply()
        return now
    }

    private companion object {
        const val PREFS = "zenith_connect"
        const val KEY_ACCENT = "accent"
        const val KEY_SAVED = "saved"

        /** How long a setup just published stays outlined, and a post just in stays lit. */
        const val FRESH_MS = 4_200L
        const val LANDED_MS = 2_600L
    }
}
