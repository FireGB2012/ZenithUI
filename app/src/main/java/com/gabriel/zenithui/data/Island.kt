package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONObject

/**
 * The Zenith Notch (Zenith Plus, Settings → Zenith Notch; "the island" in the code, which is what it
 * was called first): the camera, stretched. At rest it is a small pill round the camera with the
 * battery and a status icon or two, or, if you would rather, the camera and nothing else; it grows
 * out of it for what is going on — music, a call, a timer — and for a moment when something happens:
 * a notification, the ringer changed, the charger plugged in. Only what is changed is saved.
 */
@Immutable
data class IslandSettings(
    val on: Boolean = false,
    val music: Boolean = true,
    /** Calls, timers, stopwatches, recordings: anything an app keeps a running clock for. */
    val live: Boolean = true,
    val notifications: Boolean = true,
    /** The charger going in, for a moment. */
    val charging: Boolean = true,
    /** Ring, vibrate or silent, for a moment as it changes. */
    val ringer: Boolean = true,
    /**
     * At rest, a small pill round the camera with the battery and a status icon or two; off, at rest
     * it is the camera and nothing more.
     */
    val always: Boolean = true,
    /** Out of the way while an app is full screen: a video, a game. */
    val hideFullScreen: Boolean = true,
    /** Out of the way while the phone is on its side. */
    val hideSideways: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        val d = IslandSettings()
        if (on != d.on) put("on", on)
        if (music != d.music) put("music", music)
        if (live != d.live) put("live", live)
        if (notifications != d.notifications) put("notifications", notifications)
        if (charging != d.charging) put("charging", charging)
        if (ringer != d.ringer) put("ringer", ringer)
        if (always != d.always) put("always", always)
        if (hideFullScreen != d.hideFullScreen) put("fullScreen", hideFullScreen)
        if (hideSideways != d.hideSideways) put("sideways", hideSideways)
    }

    companion object {
        fun fromJson(o: JSONObject?): IslandSettings {
            if (o == null) return IslandSettings()
            val d = IslandSettings()
            return IslandSettings(
                on = o.optBoolean("on", d.on),
                music = o.optBoolean("music", d.music),
                live = o.optBoolean("live", d.live),
                notifications = o.optBoolean("notifications", d.notifications),
                charging = o.optBoolean("charging", d.charging),
                ringer = o.optBoolean("ringer", d.ringer),
                // An island set to a small pill, or to the camera alone, before this was a switch
                // keeps what it was set to; otherwise the pill is the notch at rest.
                always = o.optBoolean("always", if (o.has("idle")) o.optString("idle") == "pill" else d.always),
                hideFullScreen = o.optBoolean("fullScreen", d.hideFullScreen),
                hideSideways = o.optBoolean("sideways", d.hideSideways),
            )
        }
    }
}

/** What the island can be showing. */
enum class IslandKind {
    /** Music playing: the cover, and bars in its colour. */
    MUSIC,

    /** A call, a timer, a stopwatch: the app, and its running clock. */
    LIVE,

    /** A notification, for a moment: the app's icon and who it is from. */
    NOTIFICATION,

    /** The ringer changed, for a moment: ring, vibrate or silent. */
    RINGER,

    /** The charger plugged in, for a moment: how full. */
    PLUGGED,

    /** Nothing going on: the small pill at rest, with the battery and the status icons. */
    IDLE;

    /** Whether a tap opens it into a card: the ones with more to them than fits round the camera. */
    val opens: Boolean get() = this == MUSIC || this == LIVE || this == NOTIFICATION
}

/**
 * Something that happened, shown for a moment: a notification ([packageName], [title] and
 * [text]), the ringer ([text] is "ring", "vibrate" or "silent"), the charger plugged in ([level]
 * in percent).
 */
@Immutable
data class IslandMoment(
    val kind: IslandKind,
    val at: Long,
    val packageName: String = "",
    val title: String = "",
    val text: String = "",
    val level: Int = 0,
)

/** One of the two permissions the Notch needs from Android, each explained before Android's page opens. */
enum class NotchAccess {
    /** "Display over other apps": for the Notch to be drawn round the camera over every app. */
    OVER_APPS,

    /** Notification access: for music, calls, timers and messages. */
    NOTIFICATIONS,
}

/** A status icon the notch shows at rest, beside the camera. */
enum class NotchStatus {
    /** Do Not Disturb is on: a moon. */
    QUIET,

    /** The ringer is off: a bell, struck through, in red. */
    SILENT,

    /** The ringer only vibrates. */
    VIBRATE,
}

/** How the battery at rest is drawn: green while it charges, red when it runs low, else in the ink. */
enum class BatteryLook { CHARGING, LOW, NORMAL }

/** What the island shows, and why: as plain functions of what is going on, so they are tested. */
object IslandRules {
    const val NOTIFICATION_MS = 5_000L
    const val RINGER_MS = 2_500L
    const val PLUGGED_MS = 3_000L

    /** How long a moment of [kind] is shown. Music and live clocks go on for as long as they do. */
    fun lasts(kind: IslandKind): Long = when (kind) {
        IslandKind.NOTIFICATION -> NOTIFICATION_MS
        IslandKind.RINGER -> RINGER_MS
        IslandKind.PLUGGED -> PLUGGED_MS
        else -> 0L
    }

    /** Whether [kind] is one the island has been asked to show. */
    fun wanted(kind: IslandKind, settings: IslandSettings): Boolean = when (kind) {
        IslandKind.MUSIC -> settings.music
        IslandKind.LIVE -> settings.live
        IslandKind.NOTIFICATION -> settings.notifications
        IslandKind.RINGER -> settings.ringer
        IslandKind.PLUGGED -> settings.charging
        IslandKind.IDLE -> settings.always
    }

    /** [moments] still being shown at [now], the newest first. */
    fun live(moments: List<IslandMoment>, now: Long): List<IslandMoment> =
        moments.filter { now - it.at in 0 until lasts(it.kind) }.sortedByDescending { it.at }

    /**
     * What the island shows at [now]: the newest moment still showing, if one is wanted; else a
     * running clock ([live]: a call, a timer); else the music, while it plays; else, if asked for,
     * the small pill. Null when it shows nothing, and is only the camera.
     */
    fun pick(settings: IslandSettings, musicPlaying: Boolean, live: Boolean, moments: List<IslandMoment>, now: Long): Pair<IslandKind, IslandMoment?>? {
        live(moments, now).firstOrNull { wanted(it.kind, settings) }?.let { return it.kind to it }
        if (live && settings.live) return IslandKind.LIVE to null
        if (musicPlaying && settings.music) return IslandKind.MUSIC to null
        if (settings.always) return IslandKind.IDLE to null
        return null
    }

    /**
     * What goes on beside what is shown, in a bubble of its own split off from the island, as two
     * things at once do on an iPhone: under a moment, the running clock or else the music; under a
     * running clock, the music. Null when nothing else is going on.
     */
    fun secondary(settings: IslandSettings, musicPlaying: Boolean, live: Boolean, shown: IslandKind?): IslandKind? = when (shown) {
        null, IslandKind.IDLE, IslandKind.MUSIC -> null
        IslandKind.LIVE -> IslandKind.MUSIC.takeIf { musicPlaying && settings.music }
        else -> when {
            live && settings.live -> IslandKind.LIVE
            musicPlaying && settings.music -> IslandKind.MUSIC
            else -> null
        }
    }

    /** When the island next needs to look again, for a moment ending; null with none to end. */
    fun nextChange(moments: List<IslandMoment>, now: Long): Long? =
        live(moments, now).minOfOrNull { it.at + lasts(it.kind) }

    /**
     * The status icons beside the camera at rest, two at most: Do Not Disturb first, then the ringer
     * when it is not ringing. [ringer] is "ring", "vibrate" or "silent". Nothing when all is as usual.
     */
    fun status(ringer: String, quiet: Boolean): List<NotchStatus> = buildList {
        if (quiet) add(NotchStatus.QUIET)
        when (ringer) {
            "silent" -> add(NotchStatus.SILENT)
            "vibrate" -> add(NotchStatus.VIBRATE)
        }
    }.take(2)

    /** At or under this much, the battery at rest turns red, as a phone's does. */
    const val LOW_BATTERY = 20

    /** How the battery at rest is drawn, at [level] percent. */
    fun battery(level: Int, charging: Boolean): BatteryLook = when {
        charging -> BatteryLook.CHARGING
        level <= LOW_BATTERY -> BatteryLook.LOW
        else -> BatteryLook.NORMAL
    }

    /** Moments kept: the ones still showing, and no more than a handful. */
    fun prune(moments: List<IslandMoment>, now: Long): List<IslandMoment> = live(moments, now).take(MOST)

    private const val MOST = 8

    /**
     * A running clock as a clock says it: 0:07, 4:32, 1:02:09. [base] is when it started, for one
     * counting up, or when it ends, for one counting [down]; never below nothing.
     */
    fun clock(base: Long, down: Boolean, now: Long): String {
        val seconds = ((if (down) base - now else now - base).coerceAtLeast(0L) + if (down) 999L else 0L) / 1000L
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
