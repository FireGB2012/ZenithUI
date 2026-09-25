package com.gabriel.zenithui.data

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Grabs the current wallpaper and keeps a small, pre-blurred copy of it around.
 * Glass panels paint that copy behind themselves, so the blur costs nothing per frame.
 */
class WallpaperSource(context: Context) {

    private val appContext = context.applicationContext

    private val _backdrop = MutableStateFlow<ImageBitmap?>(null)
    val backdrop: StateFlow<ImageBitmap?> = _backdrop.asStateFlow()

    // Android gives the wallpaper to whoever is the home app, and the permission lint asks for is
    // not one a launcher may hold. A phone that refuses throws, which runCatching already handles
    // by leaving the backdrop empty and the glass tinted rather than blurred.
    @SuppressLint("MissingPermission")
    suspend fun refresh() = withContext(Dispatchers.IO) {
        val bitmap = runCatching {
            val manager = WallpaperManager.getInstance(appContext)
            if (manager.wallpaperInfo != null) null else manager.drawable
        }.getOrNull()?.let { drawable ->
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: TARGET_WIDTH
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: (TARGET_WIDTH * 2)
            val scale = TARGET_WIDTH.toFloat() / w
            val outW = TARGET_WIDTH
            val outH = (h * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, outW, outH)
            drawable.draw(Canvas(bmp))
            bmp
        } ?: return@withContext

        if (!bitmap.hasVisiblePixels()) {
            bitmap.recycle()
            return@withContext
        }
        _backdrop.value = blur(blur(bitmap, 4), 3).asImageBitmap()
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
        const val TARGET_WIDTH = 220
    }
}
