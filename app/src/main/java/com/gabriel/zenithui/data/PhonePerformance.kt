package com.gabriel.zenithui.data

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What Auto has found this phone to be ([Performance.Verdict]), for the whole of Zenith: by its
 * chip as soon as it is asked ([Performance.byChip]), and for a chip that says nothing, by how its
 * frames keep up once enough of them have been drawn ([Performance.byFrames]).
 *
 * A phone its frames found struggling is remembered as one, on the phone and outside the config:
 * it is about this phone, so it is never backed up or carried to another in a Look, and it is not
 * made to struggle through the test again every time Zenith starts. One that kept up is watched
 * again each start, which costs it nothing.
 */
object PhonePerformance {
    private val found = MutableStateFlow(Performance.Verdict.UNKNOWN)

    /** What Auto has found: [Performance.Verdict.UNKNOWN] until the chip or the frames say. */
    val verdict: StateFlow<Performance.Verdict> = found.asStateFlow()

    @Volatile private var started = false

    /** Asks the chip, or failing that what the frames said before. Once, however often it is called. */
    fun start(context: Context) {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
            val app = context.applicationContext ?: context
            val chip = runCatching { byChip(app) }.getOrDefault(Performance.Verdict.UNKNOWN)
            found.value = if (chip != Performance.Verdict.UNKNOWN) chip else saved(app)
        }
    }

    /**
     * Watches [activity]'s frames while Auto still does not know what this phone is, and decides
     * once it has seen enough of them. Answers with what stops the watching, to be called when the
     * activity goes; null when there is nothing to watch.
     */
    fun watch(activity: Activity): (() -> Unit)? {
        start(activity)
        if (found.value != Performance.Verdict.UNKNOWN) return null
        return runCatching { FrameWatch(activity).begin() }.getOrNull()
    }

    /** What this phone's chip and memory say, before a frame is drawn. */
    private fun byChip(context: Context): Performance.Verdict {
        // Android 12 names the chip itself; before that, or where it will not say, the hardware's
        // name is the chip's on Exynos and MediaTek phones, and says nothing on the rest.
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.orEmpty() else ""
        val model = soc.takeUnless { it.isBlank() || it.equals(Build.UNKNOWN, ignoreCase = true) } ?: Build.HARDWARE.orEmpty()
        val maker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER.orEmpty() else ""
        val memory = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val totalMb = memory?.let { manager ->
            ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }.totalMem / (1024L * 1024L)
        } ?: 0L
        return Performance.byChip(model, maker, totalMb, memory?.isLowRamDevice == true)
    }

    private fun saved(context: Context): Performance.Verdict =
        if (prefs(context).getString(KEY_FRAMES, null) == Performance.LITE) Performance.Verdict.LITE else Performance.Verdict.UNKNOWN

    private fun decided(context: Context, verdict: Performance.Verdict) {
        if (verdict == Performance.Verdict.LITE) prefs(context).edit().putString(KEY_FRAMES, Performance.LITE).apply()
        found.value = verdict
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * The frames of one activity's window, counted off the main thread as Android reports them: how
     * many were drawn and how many of those were late. The first few after each start are left out,
     * while Zenith warms up; after that every frame drawn counts, and a phone lying still draws none.
     */
    private class FrameWatch(private val activity: Activity) {
        private val window: Window = activity.window
        private val thread = HandlerThread("zenith-frames").apply { start() }
        private val context = activity.applicationContext
        private var seen = 0
        private var drawn = 0
        private var late = 0
        private var done = false
        private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ -> count(metrics) }

        fun begin(): () -> Unit {
            window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
            return ::end
        }

        private fun count(metrics: FrameMetrics) {
            if (done || metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return
            if (++seen <= WARM_UP_FRAMES) return
            drawn++
            if (Performance.isLate(took(metrics), interval())) late++
            val verdict = Performance.byFrames(late, drawn)
            if (verdict != Performance.Verdict.UNKNOWN) {
                done = true
                decided(context, verdict)
                activity.runOnUiThread(::end)
            }
        }

        /** How long the frame took: on its way through the app, or on the graphics chip, whichever was longer. */
        private fun took(metrics: FrameMetrics): Long {
            val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) maxOf(total, gpu(metrics)) else total
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun gpu(metrics: FrameMetrics): Long = metrics.getMetric(FrameMetrics.GPU_DURATION)

        /** How long the screen gives a frame, by how fast it is running now: 8.3 ms at 120 Hz. */
        private fun interval(): Long {
            val hz = RefreshRate.currentHz(activity).coerceAtLeast(RefreshRate.FLOOR_HZ)
            return (1_000_000_000L / hz).toLong()
        }

        private fun end() {
            runCatching { window.removeOnFrameMetricsAvailableListener(listener) }
            thread.quitSafely()
        }
    }

    /** Frames left out at the start of each watch, while Zenith warms up. */
    private const val WARM_UP_FRAMES = 90

    private const val PREFS = "zenith_device"
    private const val KEY_FRAMES = "frames"
}
