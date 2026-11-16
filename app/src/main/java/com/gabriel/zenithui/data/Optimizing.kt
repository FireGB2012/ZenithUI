package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * What setup's "Optimizing Zenith" does, in order, so that the first home screen, the first swipe
 * into the drawer and the first sheet have nothing left to work out:
 *
 * - [APPS]: every app and its name read again, A to Z, as the drawer lists them.
 * - [ICONS]: every app's icon drawn at the drawer's size and kept in memory ([IconLoader.warm]).
 * - [DRAWER]: each icon's main colour worked out, for the drawer's By colour ([IconLoader.colours]).
 * - [GLASS]: the wallpaper's softened copies made, which the glass and the frost show.
 * - [GRAPHICS]: the glass's shader and the blur drawn once, so the phone has them compiled.
 * - [SAVE]: what setup chose written down, so it is there when Zenith next starts.
 *
 * [weight] is what one unit of a job costs against the others, a unit being one app for the icons
 * and the drawer and the whole job for the rest: drawing an icon is the yardstick.
 */
enum class OptimizeJob(val weight: Float) {
    APPS(4f),
    ICONS(1f),
    DRAWER(0.25f),
    GLASS(6f),
    GRAPHICS(2f),
    SAVE(1f),
}

/** One job, [done] units of [total]. One with nothing to do is done. */
@Immutable
data class JobProgress(val job: OptimizeJob, val done: Int, val total: Int) {
    val finished: Boolean get() = done >= total
}

/**
 * Where "Optimizing Zenith" has got: each job's count, and when it started, from which the share
 * done ([fraction]) and the time left ([leftMs]) are worked out, from the work itself rather than
 * from a clock that only looks like progress.
 */
@Immutable
data class Optimizing(val jobs: List<JobProgress>, val startedAtMs: Long) {

    fun of(job: OptimizeJob): JobProgress = jobs.first { it.job == job }

    /** [job] at [done] of [total]: the total as it is now known, the count kept within it. */
    fun with(job: OptimizeJob, done: Int, total: Int): Optimizing {
        val most = total.coerceAtLeast(0)
        return copy(jobs = jobs.map { if (it.job == job) JobProgress(job, done.coerceIn(0, most), most) else it })
    }

    /** The job in hand: the first not yet done, or none when everything is. */
    val current: JobProgress? get() = jobs.firstOrNull { !it.finished }

    val finished: Boolean get() = jobs.all { it.finished }

    /** How much of the work is done, 0 to 1, each job weighed by what its units cost. */
    val fraction: Float
        get() {
            var whole = 0f
            var done = 0f
            for (it in jobs) {
                whole += it.total * it.job.weight
                done += it.done * it.job.weight
            }
            return if (whole <= 0f) 1f else (done / whole).coerceIn(0f, 1f)
        }

    /**
     * How long is left at [nowMs]: the time so far, spread over what is still to do. Null until
     * there is enough to go on (a twentieth of the work, or a third of a second), and 0 when done.
     */
    fun leftMs(nowMs: Long): Long? {
        if (finished) return 0L
        val share = fraction
        val elapsed = nowMs - startedAtMs
        if (share < MIN_SHARE || elapsed < MIN_ELAPSED_MS) return null
        return (elapsed * (1f - share) / share).toLong()
    }

    companion object {
        /** Everything still to do, for a phone with [apps] apps, started at [startedAtMs]. */
        fun start(apps: Int, startedAtMs: Long): Optimizing = Optimizing(
            jobs = OptimizeJob.entries.map { job ->
                JobProgress(job, 0, if (job == OptimizeJob.ICONS || job == OptimizeJob.DRAWER) apps.coerceAtLeast(0) else 1)
            },
            startedAtMs = startedAtMs,
        )

        private const val MIN_SHARE = 0.05f
        private const val MIN_ELAPSED_MS = 300L
    }
}

/** What "Optimizing Zenith" says as it goes. */
object OptimizeWords {

    /**
     * The job in hand, as a line: "Caching app icons… 42 of 118". [glassy] is whether the theme is
     * made of glass, for which the softened wallpaper is the glass; for the others it is the
     * wallpaper's.
     */
    fun line(at: Optimizing, glassy: Boolean): String {
        val now = at.current ?: return "All set"
        val doing = when (now.job) {
            OptimizeJob.APPS -> "Reading your apps"
            OptimizeJob.ICONS -> "Caching app icons"
            OptimizeJob.DRAWER -> "Building the app drawer"
            OptimizeJob.GLASS -> if (glassy) "Preparing the glass" else "Preparing your wallpaper"
            OptimizeJob.GRAPHICS -> "Warming up the graphics"
            OptimizeJob.SAVE -> "Saving your setup"
        }
        return if (now.total > 1) "$doing… ${now.done} of ${now.total}" else "$doing…"
    }

    /** The share done in whole percent, never a hundred before everything is. */
    fun percent(at: Optimizing): String {
        val whole = if (at.finished) 100 else floor(at.fraction * 100f).toInt().coerceIn(0, 99)
        return "$whole%"
    }

    /** The time left, as somebody would say it: see [Optimizing.leftMs]. */
    fun left(ms: Long?): String = when {
        ms == null -> "Working out how long…"
        ms <= ALMOST_MS -> "Almost done"
        ms < 60_000L -> "About ${ceil(ms / 1000.0).toInt()} seconds left"
        ms < 90_000L -> "About a minute left"
        else -> "About ${(ms / 60_000.0).roundToInt()} minutes left"
    }

    private const val ALMOST_MS = 1_500L
}
