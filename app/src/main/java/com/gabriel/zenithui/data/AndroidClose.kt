package com.gabriel.zenithui.data

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong

/**
 * Android's own close of an app on the way home, which a launcher that is not built into the
 * phone can neither change nor skip: with gestures the phone's own launcher shrinks the app's
 * window and drops it towards the foot of the screen, and with buttons Android slides it off the
 * bottom. Zenith is already in front underneath while that plays, so anything it does then is
 * hidden behind the app on its way out. Instead the home screen stays empty, the wallpaper only,
 * until the close is over, and only then do the icons come in and the app go into its icon.
 *
 * Android says when that is: an activity is told its entrance is over once the transition that
 * brought it into view has finished, and going home from an app the phone's launcher keeps that
 * transition open until its own animation of the app is done. That is from Android 14, where every
 * transition runs that way. Before it, the word comes as soon as Zenith is shown, so the usual
 * length of the close is waited out instead. Either way it is kept between the least the close
 * takes on this make of phone and the most, should the word be early or never come; and a wait
 * set by hand (Settings, Motion, the Closing an app numbers) is kept to exactly.
 *
 * How long: Android as Google writes it, from its source (Launcher3's quickstep, and the window
 * animations in frameworks/base): with gestures the window springs into a small square over the
 * dock in about 400 ms while the home screen fades up over 250 ms; with buttons the app drops off
 * the bottom of the screen in 225 ms. The makers who write their own do not say, so theirs are
 * set a little long; OnePlus, Oppo and realme are known to show a launcher not their own about a
 * second late since Android 14.
 */
object AndroidClose {

    /** The makes of phone whose own close runs its own way. */
    enum class Make { Pixel, Samsung, Xiaomi, Oppo, Vivo, Honor, Other }

    /** A slider below this is Auto; from it on, a wait in milliseconds. */
    const val WAIT_AUTO = 5f

    /** From this Android on, its word on the close being over comes when it really is. */
    const val SAYS_DONE_SDK = 34

    /** How long Android's close usually takes, and the most it may, at Android's normal speed. */
    data class Span(val usualMs: Long, val mostMs: Long)

    /**
     * How Zenith waits on coming back from an app: at least [atLeastMs], then until Android says
     * it is done if it [listens], and [atMostMs] at the most, all from the moment Zenith is back.
     */
    data class Plan(val atLeastMs: Long, val atMostMs: Long, val listens: Boolean)

    /** The make of phone, from Build.MANUFACTURER and Build.BRAND. */
    fun make(manufacturer: String, brand: String): Make {
        val names = setOf(manufacturer.trim().lowercase(), brand.trim().lowercase())
        fun any(vararg of: String) = of.any { it in names }
        return when {
            any("samsung") -> Make.Samsung
            any("xiaomi", "redmi", "poco") -> Make.Xiaomi
            any("oppo", "oneplus", "realme") -> Make.Oppo
            any("vivo", "iqoo") -> Make.Vivo
            any("honor", "huawei") -> Make.Honor
            any("google", "motorola", "lenovo", "nothing", "sony", "fairphone", "hmd global", "nokia") -> Make.Pixel
            else -> Make.Other
        }
    }

    /**
     * Whether the phone goes home with a gesture: Android's own setting (2 is gestures), or
     * Samsung's or Xiaomi's own, which can say so when Android's does not.
     */
    fun gestures(navigationMode: Int, samsungGestures: Int, xiaomiGestures: Int): Boolean =
        navigationMode == 2 || samsungGestures == 1 || xiaomiGestures == 1

    /** How long this make's own close takes, at normal speed, with gestures or with buttons. */
    fun span(make: Make, gestures: Boolean): Span = if (gestures) {
        when (make) {
            Make.Pixel -> Span(400, 800)
            Make.Samsung -> Span(450, 1000)
            Make.Xiaomi -> Span(450, 1000)
            Make.Oppo -> Span(600, 1300)
            Make.Vivo, Make.Honor -> Span(500, 1100)
            Make.Other -> Span(450, 1000)
        }
    } else {
        when (make) {
            Make.Pixel -> Span(250, 600)
            Make.Samsung, Make.Other -> Span(350, 800)
            Make.Xiaomi, Make.Oppo, Make.Vivo, Make.Honor -> Span(400, 900)
        }
    }

    /**
     * How Zenith waits on this phone: [chosen] milliseconds if set by hand, otherwise Auto, for
     * this [make], gestures or buttons, at Android's animation speeds (Developer options): a close
     * with gestures runs on the phone's launcher's animators, one with buttons is a window
     * animation of Android's. Null when Android's animations are off and there is nothing to wait for.
     */
    fun plan(make: Make, gestures: Boolean, animatorScale: Float, transitionScale: Float, sdk: Int, chosen: Float): Plan? {
        if (chosen >= WAIT_AUTO) {
            val ms = chosen.roundToLong()
            return Plan(ms, ms, listens = false)
        }
        val scale = (if (gestures) animatorScale else transitionScale).coerceIn(0f, 10f)
        if (scale <= 0f) return null
        val span = span(make, gestures)
        val least = (span.usualMs * scale).roundToLong()
        val most = (span.mostMs * scale).roundToLong()
        return if (sdk >= SAYS_DONE_SDK) Plan(least, most, listens = true) else Plan(least, least, listens = false)
    }

    /**
     * When to set off, coming back at [startedAt] with Android having said it is done at
     * [saidDoneAt], if it has: then, but not before the least the close takes nor after the most.
     * Not listening, at the end of the wait.
     */
    fun setOffAt(plan: Plan, startedAt: Long, saidDoneAt: Long?): Long {
        val least = startedAt + plan.atLeastMs
        if (!plan.listens) return least
        val most = startedAt + plan.atMostMs
        return (saidDoneAt ?: most).coerceIn(least, most)
    }

    // ------------------------------------------------------------ on the phone

    /** When Android last said a transition that brought Zenith into view was over, in uptime. */
    private val saidDone = MutableStateFlow(0L)

    private val tookMs = MutableStateFlow(0L)

    /** How long Android's own close took the last time it said, from Zenith coming back; 0 before it has. */
    val lastTook: StateFlow<Long> = tookMs.asStateFlow()

    /** Android has said Zenith's entrance is over (Activity.onEnterAnimationComplete). */
    fun saysDone() {
        saidDone.value = SystemClock.uptimeMillis()
    }

    /**
     * Waits out [plan], counted from [startedAt], taking only what Android says after [since]: the
     * moment Zenith was covered, so the word on an earlier visit does not count for this one.
     */
    suspend fun awaitSetOff(plan: Plan, startedAt: Long, since: Long) {
        while (true) {
            val said = saidDone.value.takeIf { it > since }
            val left = setOffAt(plan, startedAt, said) - SystemClock.uptimeMillis()
            if (left <= 0L) break
            if (plan.listens && said == null) {
                // Asleep until then, or until Android says it is done before.
                withTimeoutOrNull(left) { saidDone.first { it > since } }
            } else {
                delay(left)
            }
        }
        saidDone.value.takeIf { it > since }?.let { tookMs.value = (it - startedAt).coerceAtLeast(0L) }
    }

    /** [plan] for this phone as it is set now, with [chosen] from the slider. */
    fun planHere(context: Context, chosen: Float): Plan? {
        val resolver = context.contentResolver
        fun secure(name: String) = runCatching { Settings.Secure.getInt(resolver, name, -1) }.getOrDefault(-1)
        fun global(name: String) = runCatching { Settings.Global.getInt(resolver, name, -1) }.getOrDefault(-1)
        fun scale(name: String) = runCatching { Settings.Global.getFloat(resolver, name, 1f) }.getOrDefault(1f)
        val gestures = gestures(
            navigationMode = secure("navigation_mode"),
            // One UI's "Swipe gestures", and MIUI's and HyperOS's "Full screen gestures".
            samsungGestures = global("navigation_bar_gesture_while_hidden"),
            xiaomiGestures = global("force_fsg_nav_bar"),
        )
        return plan(
            make(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty()),
            gestures,
            animatorScale = scale(Settings.Global.ANIMATOR_DURATION_SCALE),
            transitionScale = scale(Settings.Global.TRANSITION_ANIMATION_SCALE),
            sdk = Build.VERSION.SDK_INT,
            chosen = chosen,
        )
    }
}
