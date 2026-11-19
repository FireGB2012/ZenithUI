package com.gabriel.zenithui.data.connect

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.Locale

// Zenith Connect's rules: which setups each list shows and in what order, what a card says, what
// a setup puts on and in which steps, and what publishing one asks for. Nothing here draws; the
// pages in ui/connect read these. The setups, posts and builds themselves are samples for now
// (ConnectSamples): Connect has no server of its own yet.

/** What a setup is: a whole home screen, a set of widgets, or icons. */
enum class SetupKind(val label: String) {
    SETUP("Full setup"),
    WIDGETS("Widgets"),
    ICONS("Icon pack"),
}

/** A home screen someone made and shared, as Discover shows it. */
data class ConnectSetup(
    val id: String,
    val name: String,
    /** Who made it, without the @. */
    val by: String,
    /** Its rating out of 5, or null while it is too new to have one. */
    val rating: Float?,
    /** How many times it has been put on. */
    val applies: Int,
    /** How fast it is going this week, 0 to 100: Trending's order. */
    val trend: Int,
    /** Days since it was published. */
    val ageDays: Int,
    val kind: SetupKind,
    /** The time its preview's clock shows; empty for an icon pack, which has no clock. */
    val clock: String,
    /** How round its icons are, as a share of their side: 0.5 is a circle. */
    val iconRound: Float,
    /** Whether its wallpaper is a dark one, which its preview's words and icons are drawn for. */
    val dark: Boolean,
    /** Its five colours, as ARGB. */
    val palette: List<Long>,
    val pages: Int,
    val widgets: Int,
    /** Its icon pack's name, or for an icon pack how many icons it has. */
    val pack: String,
    /** Words it is found by, separated by spaces. */
    val tags: String,
    /** Its wallpaper (see [ConnectWalls]). */
    val wall: String,
)

/** Discover's four lists. */
enum class FeedTab(val label: String) {
    WEEK("Setup of the Week"),
    TRENDING("Trending"),
    TOP("Top Rated"),
    NEW("New Release"),
}

object ConnectFeed {

    /** The setup of the week, which has the big card at the top of This week. */
    const val SPOTLIGHT = "nightfall"

    /**
     * The setups [tab] lists, found by [query] if there is one: This week leaves out the setup of
     * the week (it has the big card) unless you are searching; Top rated goes by rating, then by
     * trend; New releases newest first; the rest by trend. A search looks at the name, the creator,
     * the kind and the tags, in any case, and keeps the tab's order.
     */
    fun list(all: List<ConnectSetup>, tab: FeedTab, query: String): List<ConnectSetup> {
        val q = query.trim().lowercase()
        var list = all
        if (q.isNotEmpty()) list = list.filter { "${it.name} ${it.by} ${it.kind.label} ${it.tags}".lowercase().contains(q) }
        if (tab == FeedTab.WEEK && q.isEmpty()) list = list.filter { it.id != SPOTLIGHT }
        return when (tab) {
            FeedTab.TOP -> list.sortedWith(compareByDescending<ConnectSetup> { it.rating ?: -1f }.thenByDescending { it.trend })
            FeedTab.NEW -> list.sortedBy { it.ageDays }
            else -> list.sortedByDescending { it.trend }
        }
    }

    /** The setup of the week's big card shows on This week, with nothing searched. */
    fun heroShown(tab: FeedTab, query: String): Boolean = tab == FeedTab.WEEK && query.isBlank()

    /** The heading over the cards and the small note beside it. */
    fun heading(tab: FeedTab, query: String, found: Int, today: LocalDate): Pair<String, String> {
        if (query.isNotBlank()) return "Results" to "$found found"
        return when (tab) {
            FeedTab.WEEK -> "More this week" to "wk ${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
            FeedTab.TRENDING -> "Trending now" to "live"
            FeedTab.TOP -> "Top rated" to "all time"
            FeedTab.NEW -> "New releases" to "last 7 days"
        }
    }

    /** What a card says beside its creator: how new it is on New releases, else its rating, or New. */
    fun stat(setup: ConnectSetup, tab: FeedTab): String = when {
        tab == FeedTab.NEW -> if (setup.ageDays == 0) "just now" else "${setup.ageDays}d ago"
        setup.rating == null -> "New"
        else -> rating(setup.rating)
    }

    /** Whether that comes with a star: only a rating does. */
    fun starred(setup: ConnectSetup, tab: FeedTab): Boolean = tab != FeedTab.NEW && setup.rating != null

    fun rating(value: Float): String = String.format(Locale.US, "%.1f", value)

    /** 18200 as 18.2k, 10000 as 10k, 980 as it is. */
    fun count(n: Int): String {
        fun short(value: Double, unit: String): String {
            val tenths = Math.round(value * 10.0)
            return if (tenths % 10 == 0L) "${tenths / 10}$unit" else "${tenths / 10}.${tenths % 10}$unit"
        }
        return when {
            n >= 999_950 -> short(n / 1_000_000.0, "M")
            n >= 1_000 -> short(n / 1_000.0, "k")
            else -> n.toString()
        }
    }

    /** How tall each kind's preview is on a card, in dp, as the cards are measured for the columns. */
    fun previewHeight(kind: SetupKind): Int = when (kind) {
        SetupKind.SETUP -> 244
        SetupKind.WIDGETS -> 150
        SetupKind.ICONS -> 160
    }
}

/** What a setup can put on Zenith. */
enum class Layer { WALLPAPER, WIDGETS, ICONS }

/** A step of the apply screen, or of anything else that runs: not started, going, or finished. */
enum class StepState { WAITING, RUNNING, DONE }

/** What the Beta Hub's download, or the Studio's publish, is doing. */
enum class DownloadState { IDLE, BUSY, DONE }

/** The apply screen at one moment (see [ConnectApply.stage]). */
data class ApplyStage(
    val steps: List<StepState>,
    /** How many of the setup's five colours are in. */
    val swatches: Int,
    /** How far through its time the progress bar is, 0 to 1. */
    val bar: Float,
    /** The line under the last step. */
    val line: String,
    val done: Boolean,
)

object ConnectApply {

    /** A full setup has all three; a widget preset only widgets; an icon pack only icons. */
    fun available(setup: ConnectSetup): Set<Layer> = buildSet {
        if (setup.kind == SetupKind.SETUP) add(Layer.WALLPAPER)
        if (setup.kind != SetupKind.ICONS) add(Layer.WIDGETS)
        if (setup.kind != SetupKind.WIDGETS) add(Layer.ICONS)
    }

    fun label(layer: Layer, setup: ConnectSetup): String = when (layer) {
        Layer.WALLPAPER -> "Wallpaper"
        Layer.WIDGETS -> "${setup.widgets} widgets"
        Layer.ICONS -> if (setup.kind == SetupKind.ICONS) "Icon pack" else "Icons"
    }

    /** Whether there is anything to put on: at least one layer that is both on and the setup's to give. */
    fun canApply(setup: ConnectSetup, on: Set<Layer>): Boolean = available(setup).any { it in on }

    /** What is coming, in order, for the apply screen. */
    fun assets(setup: ConnectSetup, on: Set<Layer>): String {
        val given = available(setup).filter { it in on }.toSet()
        return buildList {
            if (Layer.WALLPAPER in given) add("Wallpaper")
            if (Layer.WIDGETS in given) add("${setup.widgets} widgets")
            if (Layer.ICONS in given) add(if (setup.kind == SetupKind.ICONS) setup.pack else "${setup.pack} icons")
        }.joinToString(" · ")
    }

    /** Its three steps, each as it reads while running and once done. */
    fun steps(kind: SetupKind): List<Pair<String, String>> = when (kind) {
        SetupKind.SETUP -> listOf("Pulling assets" to "Assets pulled", "Applying colors" to "Colors applied", "Arranging layout" to "Layout arranged")
        SetupKind.WIDGETS -> listOf("Pulling widgets" to "Widgets pulled", "Applying colors" to "Colors applied", "Placing widgets" to "Widgets placed")
        SetupKind.ICONS -> listOf("Pulling icons" to "Icons pulled", "Applying colors" to "Colors applied", "Swapping icons" to "Icons swapped")
    }

    /** What the progress line counts through while the last step runs. */
    fun progress(setup: ConnectSetup): List<String> = when (setup.kind) {
        SetupKind.SETUP -> (1..setup.pages).map { "Page $it of ${setup.pages}" } + "Dock and app drawer"
        SetupKind.WIDGETS -> (1..setup.widgets).map { "Widget $it of ${setup.widgets}" }
        SetupKind.ICONS -> listOf("Home screen", "Dock", "App drawer")
    }

    /** The first line of a setup's page: what it is and how much of it there is. */
    fun kindLine(setup: ConnectSetup): String = when (setup.kind) {
        SetupKind.SETUP -> "Full setup · ${setup.pages} ${if (setup.pages == 1) "page" else "pages"}"
        SetupKind.WIDGETS -> "Widget preset · ${setup.widgets} ${if (setup.widgets == 1) "widget" else "widgets"}"
        SetupKind.ICONS -> "Icon pack · ${setup.pack}"
    }

    /** The link a setup is shared by: its name in lower case, an ampersand read out, joined by dashes. */
    fun link(name: String): String {
        val slug = name.lowercase().replace("&", "and").replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "setup" }
        return "zenith://setup/$slug"
    }

    /**
     * Where the apply screen is [elapsed] milliseconds after Apply: which steps are waiting, running
     * or done, how many of the five colours are in, how far through its time the progress bar is
     * (0 to 1, eased as it is drawn), the line under the last step, and whether it is all done.
     * Worked out from the time alone, so Connect closed and opened again finds it where it got to.
     */
    fun stage(elapsed: Long, setup: ConnectSetup): ApplyStage {
        fun step(from: Long, to: Long) = when {
            elapsed < from -> StepState.WAITING
            elapsed < to -> StepState.RUNNING
            else -> StepState.DONE
        }
        val lines = progress(setup)
        val line = when {
            elapsed >= DONE_MS -> "Done"
            elapsed < STEP_THREE_MS -> "Waiting"
            else -> lines[((elapsed - STEP_THREE_MS) * lines.size / BAR_MS).toInt().coerceIn(0, lines.size - 1)]
        }
        return ApplyStage(
            steps = listOf(step(STEP_ONE_MS, STEP_TWO_MS), step(STEP_TWO_MS, STEP_THREE_MS), step(STEP_THREE_MS, DONE_MS)),
            swatches = (0 until 5).count { elapsed >= STEP_TWO_MS + SWATCH_MS * (it + 1) },
            bar = ((elapsed - STEP_THREE_MS).toFloat() / BAR_MS).coerceIn(0f, 1f),
            line = line,
            done = elapsed >= DONE_MS,
        )
    }

    /** How far apart the colours come in during the second step. */
    const val SWATCH_MS = 110L

    /** When each step starts, and when it is all done, in milliseconds from Apply. */
    const val STEP_ONE_MS = 450L
    const val STEP_TWO_MS = 1400L
    const val STEP_THREE_MS = 2350L
    const val DONE_MS = 3950L

    /** How long the progress bar takes to fill during the last step. */
    const val BAR_MS = 1500
}

object ConnectStudio {

    /** The tags a setup can be given, and how many at most. */
    val TAGS = listOf("Minimal", "Dark", "Glass", "AMOLED", "Anime", "Cozy", "Gaming")
    val DEFAULT_TAGS = listOf("Minimal", "Dark", "Glass")
    const val MAX_TAGS = 5

    /** What the capture puts in, all on to begin with. */
    val PARTS = listOf("Wallpaper", "7 widgets", "Icon pack", "Colors")

    /** The name it starts with, and the longest a name can be. */
    const val DEFAULT_NAME = "Afterglow"
    const val MAX_NAME = 32

    /** How long publishing takes, and how long "Published" shows before the sheet goes. */
    const val PUBLISH_MS = 1100L
    const val PUBLISHED_HOLD_MS = 650L

    fun nameProblem(name: String): String? = if (name.isBlank()) "Give your setup a name first." else null

    /** The publish button's words, for a public setup or a link, as it goes. */
    fun cta(public: Boolean, phase: DownloadState): String = when (phase) {
        DownloadState.IDLE -> if (public) "Publish to Discover" else "Create share link"
        DownloadState.BUSY -> if (public) "Publishing…" else "Creating link…"
        DownloadState.DONE -> if (public) "Published" else "Link ready"
    }

    /** [tags] with [tag] on or off, or null when it would be one too many. */
    fun toggleTag(tags: List<String>, tag: String): List<String>? = when {
        tag in tags -> tags - tag
        tags.size >= MAX_TAGS -> null
        else -> tags + tag
    }

    /** A setup just published from this phone: yours, with no rating yet, at the top of New releases. */
    fun published(name: String, by: String, clock: String, tags: List<String>, id: String): ConnectSetup = ConnectSetup(
        id = id,
        name = name.trim(),
        by = by,
        rating = null,
        applies = 0,
        trend = 0,
        ageDays = 0,
        kind = SetupKind.SETUP,
        clock = clock,
        iconRound = 0.26f,
        dark = true,
        palette = listOf(0xFFFF8A5B, 0xFF5B3BD9, 0xFF1A0F24, 0xFFFFC2A6, 0xFFF6F6F9),
        pages = 3,
        widgets = 7,
        pack = name.trim(),
        tags = tags.joinToString(" ") { it.lowercase() },
        wall = "afterglow",
    )

    /** Your handle: the name you gave Zenith, made into one word, or "you" with none. */
    fun handle(userName: String): String =
        userName.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(".").ifEmpty { "you" }

    /** The letter on your avatar. */
    fun initial(handle: String): String = handle.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "Y"
}

object ConnectVault {

    /** How long a sync by hand takes. */
    const val SYNC_MS = 1400L

    /** The sync card's two lines: kept up by itself, synced by hand with auto-sync off, or neither. */
    fun syncCopy(auto: Boolean, byHand: Boolean, lastSync: String, layouts: Int): Pair<String, String> = when {
        auto -> "Everything is backed up" to "Synced $lastSync · $layouts layouts, 12 themes"
        byHand -> "Synced $lastSync" to "Auto-sync is off. Tap the cloud to sync again."
        else -> "Auto-sync is off" to "Changes stay on this phone until you sync."
    }

    /** A backup's name: when it was made, as "Backup 19 Nov, 21:08". */
    fun backupName(at: LocalDateTime): String =
        "Backup ${at.dayOfMonth} ${at.format(DateTimeFormatter.ofPattern("MMM", Locale.US))}, ${at.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))}"

    /** A backup's line: the pages of the layout it was made from, and that it is new. */
    fun backupMeta(meta: String): String = "${meta.substringBefore(" · ").trim()} · just now"
}

object ConnectBeta {

    /** How long the test build takes to come down. */
    const val DOWNLOAD_MS = 2400L

    fun downloadLabel(state: DownloadState, progress: Float): String = when (state) {
        DownloadState.IDLE -> "Download APK"
        DownloadState.BUSY -> "Downloading… ${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
        DownloadState.DONE -> "Ready to install"
    }

    /** The phone a report comes from, as its chip says it. */
    fun deviceLine(model: String, release: String): String = "${model.trim().ifEmpty { "Phone" }} · Android $release"
}

object ConnectCommunity {

    /** A like or a vote turned the other way, and the count with it, never below none. */
    fun flip(on: Boolean, count: Int): Pair<Boolean, Int> = !on to (if (on) count - 1 else count + 1).coerceAtLeast(0)

    fun replies(n: Int): String = if (n == 1) "1 reply" else "$n replies"

    /**
     * Where the post that comes in live leaves a dot: on #announcements unless you are reading it,
     * and on the Community tab too unless you are in Community. Read as it was there, it simply
     * comes into the list.
     */
    fun dotsForLivePost(onCommunity: Boolean, channel: Channel): Pair<Boolean, Boolean> {
        val reading = onCommunity && channel == Channel.ANNOUNCEMENTS
        return !reading to !onCommunity
    }
}
