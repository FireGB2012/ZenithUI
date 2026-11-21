package com.gabriel.zenithui.data

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * One of Zenith's frames as Android timed it (FrameMetrics), all in nanoseconds: the vsync it was
 * meant for ([start]); how long Zenith's own thread spent on it ([app]: waiting to get to it, the
 * touch, the animations, the layout and noting down what to draw); how long the drawing thread
 * took to hand it to the graphics chip ([drawing]); how long the chip took ([gpu], or -1 where
 * Android does not say, before Android 12); and how long the screen gives a frame ([interval]:
 * 8.3 ms at 120 Hz).
 */
data class FrameSample(val start: Long, val app: Long, val drawing: Long, val gpu: Long, val interval: Long) {
    /** How long the frame took from its start to the chip being done with it. */
    val took: Long get() = app + drawing + gpu.coerceAtLeast(0L)
}

/**
 * How Zenith ran the last times it moved, from its frames ([FrameSample]), for the frame check in
 * Settings: how many frames a second it showed ([fps]), how many of them came on their turn
 * ([onTime], 0 to 1), and how long each part of a frame takes ([appMs], [drawingMs], [gpuMs]: what
 * nine frames in ten take at most, so a few spikes show), with the [slowest] of them, which is what
 * holds every frame back.
 *
 * Only movement counts: runs of at least [MIN_RUN] frames, each one coming within [GAP_NANOS] of the
 * one before it is done. A clock ticking over draws a frame on its own, and the wait between two
 * swipes is not a slow frame; a frame that took long, though, is part of its run.
 */
data class FrameReport(
    val frames: Int,
    val fps: Int,
    val onTime: Float,
    val appMs: Float,
    val drawingMs: Float,
    val gpuMs: Float?,
    val slowest: Part,
) {
    /** The parts a frame goes through: Zenith's own thread, the drawing thread, the graphics chip. */
    enum class Part { APP, DRAWING, GPU }

    /** How long [part] takes, in milliseconds. */
    fun ms(part: Part): Float = when (part) {
        Part.APP -> appMs
        Part.DRAWING -> drawingMs
        Part.GPU -> gpuMs ?: 0f
    }

    companion object {
        /** Fewer frames of movement than this, and there is nothing to say yet. */
        const val MIN_FRAMES = 30

        /** Fewer frames in a row than this are not movement: a dot coming up, a tap's ripple starting. */
        const val MIN_RUN = 6

        /** The longest wait after a frame is done for the next to still be the same movement. */
        const val GAP_NANOS = 100_000_000L

        fun of(samples: List<FrameSample>): FrameReport? {
            if (samples.size < MIN_FRAMES) return null
            val runs = runs(samples.sortedBy { it.start }).filter { it.size >= MIN_RUN }
            val moving = runs.flatten()
            if (moving.size < MIN_FRAMES) return null
            var steps = 0
            var span = 0L
            var late = 0
            for (run in runs) {
                for (i in 1 until run.size) {
                    val step = run[i].start - run[i - 1].start
                    steps++
                    span += step
                    if (Performance.isLate(step, run[i].interval)) late++
                }
            }
            if (steps == 0 || span <= 0L) return null
            val app = ninthInTen(moving.map { it.app })
            val drawing = ninthInTen(moving.map { it.drawing })
            val timed = moving.map { it.gpu }.filter { it >= 0L }
            val gpu = if (timed.isEmpty()) null else ninthInTen(timed)
            val parts = buildList {
                add(Part.APP to app)
                add(Part.DRAWING to drawing)
                if (gpu != null) add(Part.GPU to gpu)
            }
            return FrameReport(
                frames = moving.size,
                fps = (steps * 1e9 / span).roundToInt(),
                onTime = (steps - late) / steps.toFloat(),
                appMs = app,
                drawingMs = drawing,
                gpuMs = gpu,
                slowest = parts.maxBy { it.second }.first,
            )
        }

        /** [sorted] frames, in runs of ones that follow each other. */
        private fun runs(sorted: List<FrameSample>): List<List<FrameSample>> {
            val runs = mutableListOf<List<FrameSample>>()
            var run = mutableListOf<FrameSample>()
            for (frame in sorted) {
                val last = run.lastOrNull()
                if (last != null && frame.start - last.start > last.took + GAP_NANOS) {
                    runs += run
                    run = mutableListOf()
                }
                run += frame
            }
            if (run.isNotEmpty()) runs += run
            return runs
        }

        /** What nine in ten of [nanos] are at most, in milliseconds. */
        private fun ninthInTen(nanos: List<Long>): Float {
            val sorted = nanos.sorted()
            val at = (ceil(sorted.size * 0.9).toInt() - 1).coerceIn(0, sorted.lastIndex)
            return sorted[at] / 1_000_000f
        }
    }
}

/**
 * Whether Zenith is moving now, from the times of its last few frames: so that in Lite the live
 * wallpaper can hold still while it does ([LivePace.holds]). Moving is [MOVING_FRAMES] frames within
 * [WINDOW_NANOS] of now; a clock ticking over once a second is not. Frames are noted as Android
 * reports them, off the main thread, and asked about from the wallpaper's own.
 */
object LauncherMotion {
    private val times = LongArray(KEEP)
    private var next = 0
    private var size = 0

    /** Notes a frame Zenith drew, by the vsync it was for, in [System.nanoTime]'s time. */
    @Synchronized
    fun record(nanos: Long) {
        times[next] = nanos
        next = (next + 1) % KEEP
        if (size < KEEP) size++
    }

    /** Whether Zenith is moving at [now], by the frames noted so far. */
    @Synchronized
    fun moving(now: Long): Boolean {
        var near = 0
        for (i in 0 until size) if (near(times[i], now)) near++
        return near >= MOVING_FRAMES
    }

    /** Whether frames drawn at [times] mean Zenith is moving at [now]. */
    fun moving(times: List<Long>, now: Long): Boolean = times.count { near(it, now) } >= MOVING_FRAMES

    /** Lets go of every frame noted. */
    @Synchronized
    fun forget() {
        next = 0
        size = 0
    }

    private fun near(time: Long, now: Long): Boolean = abs(now - time) <= WINDOW_NANOS

    /** How many of its last frames are kept. */
    const val KEEP = 16

    /** Frames within [WINDOW_NANOS] of now for Zenith to be moving. */
    const val MOVING_FRAMES = 4

    /** A quarter of a second. */
    const val WINDOW_NANOS = 250_000_000L
}

/**
 * Whether Android has compiled Zenith's code for this phone ahead of time. Installed from Play it is
 * compiled as it installs. A copy installed by tapping it is handed to Android to compile later,
 * which Android does while the phone sits charging and unused; until then it runs slower.
 */
enum class CompiledCode {
    READY, WAITING, NOT, UNKNOWN;

    companion object {
        /** From what Android says: [compiled] with Zenith's profile, the profile [queued] for it, or neither; nothing at all where it does not [known]. */
        fun of(compiled: Boolean, queued: Boolean, known: Boolean): CompiledCode = when {
            !known -> UNKNOWN
            compiled -> READY
            queued -> WAITING
            else -> NOT
        }
    }
}

/**
 * What the frame check under Performance in Settings says, a line each: how Zenith ran the last
 * times it moved on this [hz] screen and which part held it back ([FrameReport]), or, before it
 * has moved, how to find out; whether its code is compiled yet ([CompiledCode]); and, with a moving
 * wallpaper on, that it is drawn alongside.
 */
object FrameCheck {
    fun lines(report: FrameReport?, code: CompiledCode, hz: Int, movingWallpaper: Boolean): List<String> = buildList {
        if (report == null) {
            add("Swipe between your pages or open the drawer, then come back here to see how smoothly this $hz Hz screen ran.")
        } else {
            add("When it last moved: ${report.fps} frames a second on this $hz Hz screen, ${percent(report.onTime)}% on time.")
            val slowest = report.slowest
            val others = FrameReport.Part.entries.filter { it != slowest && (it != FrameReport.Part.GPU || report.gpuMs != null) }
            val rest = others.mapIndexed { i, part ->
                val name = if (i == 0) name(part).replaceFirstChar { it.uppercase() } else name(part)
                if (i == 0) "$name took ${ms(report.ms(part))}" else "$name ${ms(report.ms(part))}"
            }.joinToString(" and ")
            add("Slowest was ${name(slowest)}, at ${ms(report.ms(slowest))} a frame where a frame gets ${ms(1000f / hz.coerceAtLeast(1))}. $rest.")
        }
        add(
            when (code) {
                CompiledCode.READY -> "Zenith's code is ready: Android has compiled it for this phone."
                CompiledCode.WAITING -> "Zenith's code is not compiled yet. Android does that the next time the phone sits charging and unused, and until then it runs slower."
                CompiledCode.NOT -> "Zenith's code is not compiled for this phone yet, so it runs slower than it can."
                CompiledCode.UNKNOWN -> "Android does not say whether Zenith's code is compiled yet."
            }
        )
        if (movingWallpaper) {
            add("A moving wallpaper is on, and the phone draws it alongside Zenith. In Lite it holds still while you swipe; a still one costs nothing.")
        }
    }

    private fun name(part: FrameReport.Part): String = when (part) {
        FrameReport.Part.APP -> "Zenith's own work"
        FrameReport.Part.DRAWING -> "drawing"
        FrameReport.Part.GPU -> "the graphics chip"
    }

    private fun percent(share: Float): Int = (share.coerceIn(0f, 1f) * 100f).roundToInt()

    /** [ms] as it reads best: whole milliseconds from 10, tenths under it. */
    private fun ms(ms: Float): String = if (ms >= 10f) "${ms.roundToInt()} ms" else "${(ms * 10f).roundToInt() / 10f} ms"
}
