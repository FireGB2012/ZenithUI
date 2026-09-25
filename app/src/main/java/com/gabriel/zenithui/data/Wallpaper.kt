package com.gabriel.zenithui.data

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Keeps a detailed copy of the wallpaper for the glass to bend and a small pre-blurred one for its
 * frosted face. Both are prepared once when the wallpaper changes, never while a pane is drawing.
 *
 * Where it comes from: Android's own copy when this phone lets a launcher read it (most no longer
 * do), otherwise the copy Zenith kept of a wallpaper it set itself — see [GlassWallpaper]. With
 * neither, the glass goes frosted instead of showing a picture that is not really behind it.
 */
class WallpaperSource(context: Context) {

    private val appContext = context.applicationContext

    private val _backdrop = MutableStateFlow<ImageBitmap?>(null)
    val backdrop: StateFlow<ImageBitmap?> = _backdrop.asStateFlow()

    private val _sharpBackdrop = MutableStateFlow<ImageBitmap?>(null)
    val sharpBackdrop: StateFlow<ImageBitmap?> = _sharpBackdrop.asStateFlow()

    private val refreshMutex = Mutex()
    /** What the prepared copies were made from, so coming home again does not redo the work. */
    private var loadedKey: String? = null

    private var listener: WallpaperManager.OnColorsChangedListener? = null

    /** Calls [onChange] whenever the wallpaper changes, wherever it was changed from, until [stopWatching]. */
    fun watch(onChange: () -> Unit) {
        stopWatching()
        val watcher = WallpaperManager.OnColorsChangedListener { colours, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                // If Zenith set this wallpaper a moment ago, these colours now vouch for its copy.
                GlassWallpaper.onColoursChanged(appContext, colours)
                onChange()
            }
        }
        runCatching {
            WallpaperManager.getInstance(appContext).addOnColorsChangedListener(watcher, Handler(Looper.getMainLooper()))
            listener = watcher
        }
    }

    fun stopWatching() {
        val watcher = listener ?: return
        runCatching { WallpaperManager.getInstance(appContext).removeOnColorsChangedListener(watcher) }
        listener = null
    }

    suspend fun refresh() = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            val manager = WallpaperManager.getInstance(appContext)
            if (runCatching { manager.wallpaperInfo != null }.getOrDefault(false)) {
                // A live wallpaper moves; any still picture of it in the glass would be wrong.
                clear()
                return@withContext
            }
            val signature = GlassWallpaper.currentSignature(appContext)
            val keptKey = "kept:$signature:${GlassWallpaper.stamp(appContext)}"
            // Coming home is when this runs; if the wallpaper has not changed, there is nothing to do.
            if (_backdrop.value != null && (loadedKey == "system:$signature" || loadedKey == keptKey)) {
                return@withContext
            }
            val system = readSystemWallpaper(manager)
            val source = system ?: GlassWallpaper.load(appContext)
            if (source == null) {
                clear()
                return@withContext
            }
            val key = if (system != null) "system:$signature" else keptKey
            prepare(source)
            loadedKey = key
        }
    }

    /**
     * Android's copy, on the few phones that still hand it to a launcher. The permission lint asks
     * for is not one a launcher may hold; a phone that refuses throws, and runCatching turns that
     * into "no copy", which sends the glass to Zenith's own copy or to frosted.
     */
    @SuppressLint("MissingPermission")
    private fun readSystemWallpaper(manager: WallpaperManager): Bitmap? = runCatching {
        val drawable = manager.drawable ?: return@runCatching null
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: SHARP_WIDTH
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: SHARP_HEIGHT
        val scale = min(1f, min(SHARP_WIDTH.toFloat() / w, SHARP_HEIGHT.toFloat() / h))
        val outW = (w * scale).roundToInt().coerceAtLeast(1)
        val outH = (h * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, outW, outH)
        drawable.draw(Canvas(bitmap))
        // A wallpaper we may not read can come back as an empty picture rather than an error.
        if (bitmap.hasVisiblePixels()) bitmap else {
            bitmap.recycle()
            null
        }
    }.getOrNull()

    private fun prepare(source: Bitmap) {
        val scale = min(1f, min(SHARP_WIDTH.toFloat() / source.width, SHARP_HEIGHT.toFloat() / source.height))
        val sharp = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).roundToInt(), (source.height * scale).roundToInt(), true)
                .also { if (it !== source) source.recycle() }
        } else {
            source
        }
        val blurWidth = min(BLUR_WIDTH, sharp.width)
        val blurHeight = (sharp.height * blurWidth.toFloat() / sharp.width).roundToInt().coerceAtLeast(1)
        val reduced = Bitmap.createScaledBitmap(sharp, blurWidth, blurHeight, true)
        // createScaledBitmap can hand back its input; the blur must never eat the sharp copy.
        val blurSource = if (reduced === sharp) sharp.copy(Bitmap.Config.ARGB_8888, true) else reduced
        val blurred = blur(blur(blurSource, 4), 3)
        _sharpBackdrop.value = sharp.asImageBitmap()
        _backdrop.value = blurred.asImageBitmap()
    }

    private fun clear() {
        // A live or unreadable wallpaper must never leave a picture of the previous wallpaper in glass.
        _sharpBackdrop.value = null
        _backdrop.value = null
        loadedKey = null
    }

    /** A wallpaper we are not allowed to read comes back empty; do not paint it as glass. */
    private fun Bitmap.hasVisiblePixels(): Boolean {
        val stepX = (width / 8).coerceAtLeast(1)
        val stepY = (height / 8).coerceAtLeast(1)
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (getPixel(x, y) ushr 24 != 0) return true
                x += stepX
            }
            y += stepY
        }
        return false
    }

    private fun blur(source: Bitmap, radius: Int): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = IntArray(w * h)
        boxBlurPass(pixels, temp, w, h, radius)
        boxBlurPass(temp, pixels, h, w, radius)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        if (source !== out) source.recycle()
        return out
    }

    /** Horizontal box blur that also transposes, so two passes blur both axes. */
    private fun boxBlurPass(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        val window = radius * 2 + 1
        for (y in 0 until h) {
            val row = y * w
            var a = 0; var r = 0; var g = 0; var b = 0
            for (i in -radius..radius) {
                val p = src[row + i.coerceIn(0, w - 1)]
                a += (p ushr 24) and 0xFF; r += (p ushr 16) and 0xFF
                g += (p ushr 8) and 0xFF; b += p and 0xFF
            }
            for (x in 0 until w) {
                dst[x * h + y] = ((a / window) shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
                val outgoing = src[row + (x - radius).coerceIn(0, w - 1)]
                val incoming = src[row + (x + radius + 1).coerceIn(0, w - 1)]
                a += ((incoming ushr 24) and 0xFF) - ((outgoing ushr 24) and 0xFF)
                r += ((incoming ushr 16) and 0xFF) - ((outgoing ushr 16) and 0xFF)
                g += ((incoming ushr 8) and 0xFF) - ((outgoing ushr 8) and 0xFF)
                b += (incoming and 0xFF) - (outgoing and 0xFF)
            }
        }
    }

    private companion object {
        const val BLUR_WIDTH = 220
        const val SHARP_WIDTH = 960
        const val SHARP_HEIGHT = 1920
    }
}
