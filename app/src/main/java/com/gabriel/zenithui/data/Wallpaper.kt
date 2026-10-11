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
 * The wallpaper as the glass needs it: as it is, and three times softer, each about three times as
 * soft as the one before. A pane shows a blend of two neighbours, which is how one can be barely
 * misted and the next fully frosted, each with a true blur, and nothing blurred while it draws.
 */
class GlassLevels(
    /** The picture itself, for the bend at a pane's rim. */
    val sharp: ImageBitmap,
    /** Lightly softened: about what clear glass shows through its face. */
    val mist: ImageBitmap,
    /** Frosted: about the face of a pane with words on it. */
    val frost: ImageBitmap,
    /** Little left but its colours: a sheet's face, and the light the picture throws on the glass round it. */
    val haze: ImageBitmap,
) {
    companion object {
        /**
         * The softer three, made from [sharp]. Each is made from the one before at half its size,
         * so none costs more than the first, and each is softened by three box blurs, which
         * between them are a Gaussian near enough. Measured in pixels of the sharp picture (about
         * half a dp on a phone) they blur by roughly 6, 18 and 54.
         */
        fun of(sharp: Bitmap): GlassLevels {
            val mist = softenedHalf(sharp, intArrayOf(2, 2, 3))
            val frost = softenedHalf(mist, intArrayOf(4, 4, 3))
            val haze = softenedHalf(frost, intArrayOf(6, 6, 6))
            return GlassLevels(sharp.asImageBitmap(), mist.asImageBitmap(), frost.asImageBitmap(), haze.asImageBitmap())
        }

        private fun softenedHalf(source: Bitmap, radii: IntArray): Bitmap {
            val w = (source.width / 2).coerceAtLeast(1)
            val h = (source.height / 2).coerceAtLeast(1)
            val reduced = Bitmap.createScaledBitmap(source, w, h, true)
            val pixels = IntArray(w * h)
            reduced.getPixels(pixels, 0, w, 0, 0, w, h)
            // createScaledBitmap hands back its input when there is nothing to scale, and that one is still in use.
            if (reduced !== source) reduced.recycle()
            val temp = IntArray(w * h)
            for (radius in radii) {
                boxBlurPass(pixels, temp, w, h, radius)
                boxBlurPass(temp, pixels, h, w, radius)
            }
            return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
        }

        /**
         * Horizontal box blur that also transposes, so two passes blur both axes. Each average is
         * rounded, not cut short: a level is eighteen passes from the picture by the last of them,
         * and losing half a shade at each would leave a dark wallpaper visibly darker in the glass.
         */
        private fun boxBlurPass(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
            val window = radius * 2 + 1
            for (y in 0 until h) {
                val row = y * w
                // Starting each sum at half the window is what rounds the division below.
                var a = radius; var r = radius; var g = radius; var b = radius
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
    }
}

/**
 * Keeps the copies of the wallpaper the glass bends, softens and reflects: see [GlassLevels]. They
 * are prepared once when the wallpaper changes, never while a pane is drawing.
 *
 * Where it comes from: Android's own copy when this phone lets a launcher read it (most no longer
 * do), otherwise the copy Zenith kept of a wallpaper it set itself — see [GlassWallpaper]. With
 * neither, the glass goes frosted instead of showing a picture that is not really behind it.
 */
class WallpaperSource(context: Context) {

    private val appContext = context.applicationContext

    private val _levels = MutableStateFlow<GlassLevels?>(null)

    /** The wallpaper for the glass, or null when there is no copy of it that may be shown. */
    val levels: StateFlow<GlassLevels?> = _levels.asStateFlow()

    private val refreshMutex = Mutex()
    /** What the prepared copies were made from, so coming home again does not redo the work. */
    private var loadedKey: String? = null

    /** Set while the glass is showing a picture that is not the wallpaper yet; see [present]. */
    @Volatile
    private var presenting = false

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

    /**
     * Gives the glass [bitmap] to bend before it is the wallpaper: a wallpaper that fades in has
     * to bring its glass with it. The glass keeps it, whatever the real wallpaper does, until
     * [release]. The bitmap is still the caller's; it is not kept and not recycled.
     */
    suspend fun present(bitmap: Bitmap) = refreshMutex.withLock {
        withContext(Dispatchers.Default) {
            presenting = true
            prepare(bitmap, owned = false)
            loadedKey = PRESENTED
        }
    }

    /** Ends [present]. True if a picture was being presented, in which case a [refresh] is due. */
    fun release(): Boolean = presenting.also { presenting = false }

    suspend fun refresh() = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            if (presenting) return@withContext
            val manager = WallpaperManager.getInstance(appContext)
            if (runCatching { manager.wallpaperInfo != null }.getOrDefault(false)) {
                // A live wallpaper moves; any still picture of it in the glass would be wrong.
                clear()
                return@withContext
            }
            val onScreen = GlassWallpaper.screenKey(appContext)
            val keptKey = "kept:$onScreen:${GlassWallpaper.stamp(appContext)}"
            // Coming home is when this runs; if the wallpaper has not changed, there is nothing to do.
            if (_levels.value != null && (loadedKey == "system:$onScreen" || loadedKey == keptKey)) {
                return@withContext
            }
            val system = readSystemWallpaper(manager)
            val source = system ?: GlassWallpaper.load(appContext)
            if (source == null) {
                clear()
                return@withContext
            }
            val key = if (system != null) "system:$onScreen" else keptKey
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

    /** Makes the copies the glass uses. An [owned] source is used up; any other is left as it was. */
    private fun prepare(source: Bitmap, owned: Boolean = true) {
        val scale = min(1f, min(SHARP_WIDTH.toFloat() / source.width, SHARP_HEIGHT.toFloat() / source.height))
        val sharp = when {
            scale < 1f -> Bitmap.createScaledBitmap(source, (source.width * scale).roundToInt(), (source.height * scale).roundToInt(), true)
                .also { if (owned && it !== source) source.recycle() }
            owned -> source
            else -> source.copy(Bitmap.Config.ARGB_8888, false)
        }
        _levels.value = GlassLevels.of(sharp)
    }

    private fun clear() {
        // A live or unreadable wallpaper must never leave a picture of the previous wallpaper in glass.
        _levels.value = null
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

    private companion object {
        const val PRESENTED = "presented"
        const val SHARP_WIDTH = 960
        const val SHARP_HEIGHT = 1920
    }
}
