package com.gabriel.zenithui.data

import android.content.Intent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * How one of the designs on the Live shelf moves. They are real Android live wallpapers
 * ([com.gabriel.zenithui.live.LiveWallpaper]): Android draws them behind everything, the home
 * screen and, where the phone allows it, the lock screen.
 */
enum class LiveMotion {
    /** Moves gently on its own whenever it can be seen. */
    Idle,

    /** Holds still on the lock screen and opens, once, as the phone is unlocked into the home screen. */
    Unlock,
}

/**
 * Where an unlock design is between its two poses, as a number: 0 is how it sits on the lock
 * screen, 1 how it sits on the home screen, and in between is the opening, in a straight line of
 * time ([pose]); each design eases its own parts along it.
 *
 * It goes back to the lock pose the moment the screen goes off, so that is what is there when the
 * screen comes back, and opens once the phone is unlocked: at once if it can be seen, or on the
 * first frame it can be. Plain arithmetic on the clock it is given, so it is tested without a phone.
 */
class LiveUnlock(private val openMs: Long) {

    /** When the opening began, or [NOT_OPENING]. */
    private var openedAt = NOT_OPENING

    /** At the lock pose, waiting for the phone to be unlocked. */
    private var shut = false

    /** Unlocked while not on show: opens on the first frame it is seen. */
    private var pending = false

    /** Whether it is at, or on its way from, the lock pose. */
    val closed: Boolean get() = shut

    /** The screen went off: back to the lock pose, which is what the screen will show when it comes on. */
    fun screenOff() {
        shut = true
        pending = false
        openedAt = NOT_OPENING
    }

    /** The phone was unlocked at [now]: it opens, now if it is [visible], otherwise as soon as it is. */
    fun unlocked(now: Long, visible: Boolean) {
        if (!shut || openedAt != NOT_OPENING) return
        if (visible) openedAt = now else pending = true
    }

    /**
     * It came into view at [now], [locked] if the lock screen is still up. On the lock screen it
     * holds still; anywhere else, coming into view at the lock pose means the phone was unlocked,
     * whether or not Android has said so yet.
     */
    fun shown(now: Long, locked: Boolean) {
        if (!shut || openedAt != NOT_OPENING) return
        if (pending || !locked) {
            pending = false
            openedAt = now
        }
    }

    /**
     * Straight to the home pose, without opening: what reduced movement asks for when the phone is
     * unlocked, and what a design that cannot move does. An opening on its way ends where it ends.
     */
    fun settle() {
        shut = false
        pending = false
        openedAt = NOT_OPENING
    }

    /** Opens it from the lock pose now, whatever the screen is doing: the preview's way of showing what it does. */
    fun replay(now: Long) {
        shut = true
        pending = false
        openedAt = now
    }

    /** The pose at [now]: 0 at the lock pose, rising in a straight line to 1 over the opening. */
    fun pose(now: Long): Float {
        if (openedAt == NOT_OPENING) return if (shut) 0f else 1f
        val t = (now - openedAt).toFloat() / openMs
        if (t >= 1f) {
            shut = false
            openedAt = NOT_OPENING
            return 1f
        }
        return t.coerceAtLeast(0f)
    }

    /** Whether it is opening at [now], so frames are wanted. */
    fun opening(now: Long): Boolean = openedAt != NOT_OPENING && now - openedAt < openMs

    /**
     * Whether it is at the lock pose with nothing yet having said the phone was unlocked: while it
     * is on show like this, the wallpaper looks at the lock screen itself ([LiveLock.WATCH_MS]), as
     * the word can come late, or not at all.
     */
    fun waiting(): Boolean = shut && !pending && openedAt == NOT_OPENING

    private companion object {
        const val NOT_OPENING = Long.MIN_VALUE
    }
}

/** What one thing Android says means for an unlock design. */
enum class LockWord {
    /** The phone is being unlocked, or is unlocked: open, if it is shut. */
    Unlocked,

    /** The screen is going off: back to the lock pose. */
    ScreenOff,

    /** Nothing for it to do. */
    Nothing,
}

/**
 * How an unlock design hears that the phone was unlocked. Android says so in more than one way,
 * and phones differ in which they say and when, so it listens for all of them and opens on the
 * first ([LiveUnlock] opens only once for each time the screen went off):
 * - the user being present, a broadcast System UI sends once the lock screen is gone. System UI is
 *   not Android's own process, so a receiver that is not exported never hears it, which is why
 *   unlock designs once never opened on a Galaxy;
 * - Android's word to the wallpaper itself that the lock screen is starting to go, which is the
 *   moment to start;
 * - and, while it waits on show at its lock pose, the lock screen itself, looked at every
 *   [WATCH_MS].
 */
object LiveLock {

    /** Android's word to a wallpaper that the lock screen is starting to go (WallpaperManager's, hidden). */
    const val KEYGUARD_GOING_AWAY = "android.wallpaper.keyguardgoingaway"

    /** Android's word to a wallpaper that the screen is starting to go off (WallpaperManager's, hidden). */
    const val GOING_TO_SLEEP = "android.wallpaper.goingtosleep"

    /** How often an unlock design waiting on show at its lock pose looks for the lock screen having gone. */
    const val WATCH_MS = 100L

    /** What a command sent to the wallpaper, [action], means. */
    fun ofCommand(action: String?): LockWord = when (action) {
        KEYGUARD_GOING_AWAY -> LockWord.Unlocked
        GOING_TO_SLEEP -> LockWord.ScreenOff
        else -> LockWord.Nothing
    }

    /** What a broadcast, [action], means, the lock screen being up ([keyguardLocked]) or not as it came. */
    fun ofBroadcast(action: String?, keyguardLocked: Boolean): LockWord = when (action) {
        Intent.ACTION_USER_PRESENT -> LockWord.Unlocked
        // With no lock screen to get past, the screen coming on is the phone unlocked.
        Intent.ACTION_SCREEN_ON -> if (keyguardLocked) LockWord.Nothing else LockWord.Unlocked
        Intent.ACTION_SCREEN_OFF -> LockWord.ScreenOff
        else -> LockWord.Nothing
    }
}

/**
 * How often a live design is drawn. An idle one keeps moving at a calm 30 frames a second in Lite
 * and 60 in Full, and holds still (nothing drawn until something changes) with movement reduced or
 * Battery saver on; an unlock design is only drawn while it opens, at 60, the short burst it is.
 */
object LivePace {

    /** Milliseconds between frames, or 0 for none: what is on screen stays until something changes. */
    fun frameMs(motion: LiveMotion, lite: Boolean, reduced: Boolean, powerSave: Boolean, opening: Boolean): Int = when {
        opening -> FULL_MS
        motion == LiveMotion.Unlock -> 0
        reduced || powerSave -> 0
        lite -> LITE_MS
        else -> FULL_MS
    }

    /**
     * Whether a vsync at [now] nanoseconds is the one to draw on, the last frame having been drawn
     * at [last]: a frame is drawn once its time has come, less half a vsync of [vsyncNanos], so a
     * 120 Hz screen paced to 30 draws every fourth vsync and does not slip to every fifth.
     */
    fun due(now: Long, last: Long, frameMs: Int, vsyncNanos: Long): Boolean =
        last == 0L || now - last >= frameMs * 1_000_000L - vsyncNanos / 2

    const val FULL_MS = 16
    const val LITE_MS = 33
}

/** The sums the live designs move by: all closed-form, so any frame can be drawn on its own. */
object LiveCurves {

    private const val TAU = (2.0 * PI)

    /** A slow swing between -1 and 1, once every [period] seconds, started at [phase] of the way round. */
    fun swing(seconds: Double, period: Double, phase: Double = 0.0): Float =
        sin(TAU * ((seconds / period + phase) % 1.0)).toFloat()

    /** How far round a full turn, 0 to 1, something turning once every [period] seconds has gone. */
    fun turn(seconds: Double, period: Double): Float = ((seconds / period) % 1.0).toFloat()

    /** [x] eased out: fast at first, settling into 1. */
    fun easeOut(x: Float): Float {
        val t = 1f - x.coerceIn(0f, 1f)
        return 1f - t * t * t
    }

    /** [x] eased in and out, as a cubic. */
    fun easeInOut(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
    }

    /**
     * A spring let go at 0 towards 1, with [damping] (under 1 overshoots once and settles), worked
     * out at [x] of the way through a time it has settled by: within a percent of 1 near x = 1, so
     * nothing is seen to jump when it is let go of there.
     */
    fun spring(x: Float, damping: Float = 0.72f): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        val z = damping.coerceIn(0.2f, 0.99f)
        // Settled to 0.5% by the end: the envelope e^(-zωt) is 0.005 at t = 1.
        val w = (SETTLE / z).toFloat()
        val wd = w * sqrt(1f - z * z)
        val envelope = exp(-z * w * x)
        return 1f - envelope * (cos(wd * x) + z * w / wd * sin(wd * x))
    }

    /**
     * A flash that rises quickly to [peak] and falls back to 1, the way a light bursting out and
     * settling does: 0 at x = 0, [peak] near x = 0.35, back to 1 by x = 1.
     */
    fun burst(x: Float, peak: Float): Float {
        if (x >= 1f) return 1f
        val t = x.coerceAtLeast(0f)
        val rise = 1f - exp(-t * 9f)
        val hump = ((t - 0.35f) / 0.18f).let { exp(-it * it) }
        return rise * (1f + (peak - 1f) * hump)
    }

    /** -ln(0.005): how many time constants a spring needs to settle to half a percent. */
    private const val SETTLE = 5.298
}

/**
 * Which design Zenith's live wallpaper shows: the one the settings name, moving or still, so that
 * wherever Android keeps it up (the lock screen, once a still design has gone on the home screen
 * alone) it shows what was chosen. With none named (a photo, or a wallpaper chosen elsewhere) it
 * keeps to the one it showed [last]; Hover only when it has shown nothing yet.
 */
object LiveDesign {
    fun of(named: String, last: String?): WallStyle =
        WallStyle.byName(named) ?: last?.let(WallStyle::byName) ?: WallStyle.Hover
}
