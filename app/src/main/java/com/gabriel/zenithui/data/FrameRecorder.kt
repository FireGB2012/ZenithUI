package com.gabriel.zenithui.data

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.profileinstaller.ProfileVerifier

/**
 * Zenith's frames as Android times them, for as long as its window is up: every one told to
 * [LauncherMotion], so the live wallpaper knows when to make way, and the last few seconds of them
 * kept for the frame check in Settings ([FrameReport]), while no page covers the home screen
 * ([covered]), since the check is about the home screen. Counted off the main thread; a phone lying
 * still draws no frames, and none are counted.
 */
object FrameRecorder {

    /** Whether a page, Settings among them, is over the home screen, whose frames are then not kept. */
    @Volatile var covered = false

    private val kept = ArrayDeque<FrameSample>()

    /** Starts counting [activity]'s frames. Answers with what stops it, for when the activity goes. */
    fun attach(activity: Activity): (() -> Unit)? = runCatching { Counting(activity).begin() }.getOrNull()

    /** How the home screen ran the last times it moved, or null until it has moved enough. */
    fun report(): FrameReport? = FrameReport.of(synchronized(kept) { kept.toList() })

    private fun keep(frame: FrameSample) = synchronized(kept) {
        kept.addLast(frame)
        while (kept.size > KEEP) kept.removeFirst()
    }

    /**
     * Whether Android has compiled Zenith's code yet, by what it tells the profile installer
     * ([CompiledCode]). Reads and writes a small file, so it is asked off the main thread.
     */
    @WorkerThread
    fun code(context: Context): CompiledCode = runCatching {
        val status = ProfileVerifier.writeProfileVerification(context.applicationContext)
        CompiledCode.of(
            compiled = status.isCompiledWithProfile,
            queued = status.hasProfileEnqueuedForCompilation(),
            known = status.profileInstallResultCode < ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST,
        )
    }.getOrDefault(CompiledCode.UNKNOWN)

    /** One window's frames, as Android reports them on a thread of their own. */
    private class Counting(private val activity: Activity) {
        private val window: Window = activity.window
        private val thread = HandlerThread("zenith-frame-check").apply { start() }
        private var seen = 0
        private var interval = interval()
        private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ -> count(metrics) }

        fun begin(): () -> Unit {
            window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
            return ::end
        }

        private fun count(metrics: FrameMetrics) {
            if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return
            val start = metrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP)
            LauncherMotion.record(start)
            if (covered) return
            // The screen can change speed, to save power or for a game: looked at again now and then.
            if (++seen % RATE_EVERY == 0) interval = interval()
            val app = metrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION) +
                metrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) +
                metrics.getMetric(FrameMetrics.ANIMATION_DURATION) +
                metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) +
                metrics.getMetric(FrameMetrics.DRAW_DURATION)
            val drawing = metrics.getMetric(FrameMetrics.SYNC_DURATION) +
                metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION) +
                metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
            val gpu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) gpu(metrics) else -1L
            keep(FrameSample(start = start, app = app, drawing = drawing, gpu = gpu, interval = interval))
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun gpu(metrics: FrameMetrics): Long = metrics.getMetric(FrameMetrics.GPU_DURATION)

        /** How long the screen gives a frame now: 8.3 ms at 120 Hz. */
        private fun interval(): Long {
            val hz = RefreshRate.currentHz(activity).coerceAtLeast(RefreshRate.FLOOR_HZ)
            return (1_000_000_000L / hz).toLong()
        }

        private fun end() {
            runCatching { window.removeOnFrameMetricsAvailableListener(listener) }
            thread.quitSafely()
        }
    }

    /** About five seconds of frames at 120 Hz. */
    private const val KEEP = 600

    /** How many frames go by between looks at the screen's speed. */
    private const val RATE_EVERY = 120
}
