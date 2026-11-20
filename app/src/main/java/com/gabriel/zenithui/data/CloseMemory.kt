package com.gabriel.zenithui.data

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import org.json.JSONObject
import kotlin.math.abs

/**
 * What Zenith last put in front of itself, kept until it comes back, so the app goes back into its
 * icon however it was opened (its icon, the drawer, Search, a gesture, a shortcut), however long it
 * was open, and with Zenith started afresh meanwhile: Android lets a home app's process go when it
 * wants the memory, and makes its screen again for some changes of settings, and either way all
 * Zenith held about the app it opened was gone, so that app closed with nothing of Zenith's.
 *
 * Kept on the phone as well, so it outlives both, and let go of once Zenith has come back and set
 * off a close, or the app never opened, or the phone has been started again since. See
 * `specs/2026-11-20-the-close-always-plays-design.md`.
 *
 * It also keeps what Zenith has found out about Android's gesture handshake on this phone
 * ([GestureHandshake]): a phone that asks where the icon is and then never says the window got
 * there ([CloseRules.trustsHandshake]) is not told any more, and closes on Zenith's own card.
 */
object CloseMemory {

    /** An app Zenith opened. */
    data class Launch(
        /** The app, as Zenith writes it. */
        val key: String,
        /** Where it was opened from, in Zenith's own coordinates: the icon touched, if one was. */
        val from: Rect?,
        /** That icon's place at rest, if it was one on the home screen's page or dock. */
        val spot: Rect?,
        /** Whether that is a dock icon, on show whatever the page. */
        val dock: Boolean,
        /** The page on show as it opened. */
        val page: Int,
        /** When, on the clock that runs from the phone starting (SystemClock.elapsedRealtime). */
        val at: Long,
        /** When the phone started, on the wall clock: one from before the phone was started again is stale. */
        val boot: Long,
    ) {
        /**
         * Its icon's place for a Zenith started afresh, which has not yet seen where its icons are:
         * the one it was opened from, if that was on the dock, or on the page on show again now.
         */
        fun restSpot(shownPage: Int): Rect? = spot?.takeIf { dock || page == shownPage }
    }

    /** How far apart two readings of when the phone started can be and still be the same start: the clocks drift. */
    const val BOOT_SLACK_MS = 10_000L

    /** Whether two readings of when the phone started are the same start. */
    fun sameBoot(a: Long, b: Long): Boolean = abs(a - b) <= BOOT_SLACK_MS

    /** [launch] as it is kept on the phone. */
    fun encode(launch: Launch): String = JSONObject().apply {
        put("key", launch.key)
        launch.from?.let { put("from", rect(it)) }
        launch.spot?.let { put("spot", rect(it)) }
        put("dock", launch.dock)
        put("page", launch.page)
        put("at", launch.at)
        put("boot", launch.boot)
    }.toString()

    /** What [encode] wrote, or null for nothing, or anything else. */
    fun decode(text: String?): Launch? {
        if (text.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(text)
            val key = o.getString("key")
            if (key.isBlank()) return null
            Launch(
                key = key,
                from = o.optString("from").takeIf { it.isNotEmpty() }?.let(::rectOf),
                spot = o.optString("spot").takeIf { it.isNotEmpty() }?.let(::rectOf),
                dock = o.optBoolean("dock", false),
                page = o.optInt("page", 0),
                at = o.getLong("at"),
                boot = o.getLong("boot"),
            )
        }.getOrNull()
    }

    /** A launch kept on the phone, if it is still good now the phone started at [bootNow]. */
    fun usable(kept: Launch?, bootNow: Long): Launch? = kept?.takeIf { sameBoot(it.boot, bootNow) }

    private fun rect(r: Rect): String = "${r.left},${r.top},${r.right},${r.bottom}"

    private fun rectOf(text: String): Rect? {
        val parts = text.split(',').mapNotNull { it.trim().toFloatOrNull() }
        if (parts.size != 4) return null
        val r = Rect(parts[0], parts[1], parts[2], parts[3])
        return r.takeIf { it.width > 0f && it.height > 0f }
    }

    // ------------------------------------------------------------ on the phone

    private const val PREFS = "zenith_close"
    private const val LAUNCH = "launch"
    private const val MISSED = "handshake_missed"

    private var held: Launch? = null
    private var loaded = false

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** When the phone started, on the wall clock. */
    fun bootNow(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    /** Zenith has just asked Android to open [key] (see [Launch] for the rest): what is now kept. */
    fun launched(context: Context, key: String, from: Rect?, spot: Rect?, dock: Boolean, page: Int): Launch {
        val launch = Launch(key, from, spot, dock, page, at = SystemClock.elapsedRealtime(), boot = bootNow())
        held = launch
        loaded = true
        runCatching { prefs(context).edit().putString(LAUNCH, encode(launch)).apply() }
        return launch
    }

    /** The last app Zenith opened that has not come back since, if there is one. */
    fun latest(context: Context): Launch? {
        if (!loaded) {
            loaded = true
            held = usable(decode(runCatching { prefs(context).getString(LAUNCH, null) }.getOrNull()), bootNow())
        }
        return held
    }

    /** It has come back, or never went: let go of. */
    fun spent(context: Context) {
        val had = held != null || !loaded
        held = null
        loaded = true
        if (had) runCatching { prefs(context).edit().remove(LAUNCH).apply() }
    }

    /** [launch] never went, if it is still the one kept: a later one is left alone. */
    fun spent(context: Context, launch: Launch) {
        if (latest(context) == launch) spent(context)
    }

    /** How many times in a row Android has been told where an icon is and not said the window got there. */
    fun handshakeMisses(context: Context): Int = runCatching { prefs(context).getInt(MISSED, 0) }.getOrDefault(0)

    /** Android told where an icon was: whether it then said the window got there ([came]). */
    fun handshakeHeard(context: Context, came: Boolean) {
        val missed = if (came) 0 else handshakeMisses(context) + 1
        runCatching { prefs(context).edit().putInt(MISSED, missed).apply() }
    }
}
