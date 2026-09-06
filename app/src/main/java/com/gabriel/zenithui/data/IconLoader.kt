package com.gabriel.zenithui.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Builds masked, cached icon bitmaps, honouring the selected icon pack and icon shape. */
class IconLoader(context: Context, private val repo: AppRepository) {

    private val appContext = context.applicationContext
    private val density = appContext.resources.displayMetrics.densityDpi
    private val cache = LruCache<String, ImageBitmap>(400)
    private val lock = Mutex()

    private var pack: IconPack? = null
    private var packName: String = ""
    private var shape: String = "squircle"

    private val _version = MutableStateFlow(0)

    /** Bumped whenever the rendered look of every icon changes. */
    val version: StateFlow<Int> = _version.asStateFlow()

    suspend fun configure(iconPack: String, iconShape: String) {
        if (iconPack == packName && iconShape == shape) return
        withContext(Dispatchers.IO) {
            lock.withLock {
                shape = iconShape
                if (iconPack != packName) {
                    packName = iconPack
                    pack = if (iconPack.isEmpty()) null else IconPack(appContext, iconPack).takeIf { it.load() }
                }
                cache.evictAll()
            }
        }
        _version.value = _version.value + 1
    }

    suspend fun load(key: String, sizePx: Int): ImageBitmap? {
        val cacheKey = "$key|$sizePx|$shape|$packName"
        cache.get(cacheKey)?.let { return it }
        return withContext(Dispatchers.IO) {
            val drawable = drawableFor(key) ?: return@withContext null
            val rendered = runCatching { render(drawable, sizePx).asImageBitmap() }.getOrNull()
                ?: return@withContext null
            cache.put(cacheKey, rendered)
            rendered
        }
    }

    /**
     * Icons are asked for the moment the home screen composes, which can be before the app list
     * has finished loading. Bumping the version makes every icon ask again.
     */
    fun invalidate() {
        _version.value = _version.value + 1
    }

    private fun drawableFor(key: String): Drawable? {
        val parts = key.split('/', limit = 2)
        if (parts.size == 2) {
            pack?.drawableFor(ComponentName(parts[0], parts[1]))?.let { return it }
        }
        return repo.systemIcon(key, density)
    }

    /**
     * Every icon comes out the same size and silhouette. Adaptive icons are masked to the chosen
     * shape; legacy icons are measured, scaled to the same footprint and given a matching tile so
     * they cannot look bigger or smaller than their neighbours.
     */
    private fun render(drawable: Drawable, size: Int): Bitmap {
        val shapePath = IconShapes.path(shape, size.toFloat())

        val adaptive = drawable as? AdaptiveIconDrawable
        if (adaptive != null) {
            val layer = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val layerCanvas = Canvas(layer)
            // Adaptive icons reserve the outer 25% for parallax; expand so our mask fills the tile.
            val inset = (size * 0.25f).toInt()
            val bounds = Rect(-inset, -inset, size + inset, size + inset)
            adaptive.background?.let { it.bounds = bounds; it.draw(layerCanvas) }
            adaptive.foreground?.let { it.bounds = bounds; it.draw(layerCanvas) }
            val output = maskWith(layer, shapePath, size)
            layer.recycle()
            return output
        }

        val raw = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(raw))

        val content = visibleBounds(raw, size)
        if (content == null) {
            raw.recycle()
            return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }

        // Artwork that already fills its canvas is treated like an adaptive icon.
        val fullBleed = content.width() >= size * 0.86f && content.height() >= size * 0.86f
        if (fullBleed) {
            val output = maskWith(raw, shapePath, size)
            raw.recycle()
            return output
        }

        // Otherwise: a tile in the icon's own colour, with the artwork centred at a fixed scale.
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val tint = averageColour(raw, content)
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, size.toFloat(),
                blend(tint, Color.WHITE, 0.62f),
                blend(tint, Color.WHITE, 0.34f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(shapePath, tile)

        val longest = maxOf(content.width(), content.height()).toFloat()
        val target = size * LEGACY_CONTENT_FRACTION
        val scale = target / longest
        val destWidth = content.width() * scale
        val destHeight = content.height() * scale
        val dest = RectF(
            (size - destWidth) / 2f,
            (size - destHeight) / 2f,
            (size + destWidth) / 2f,
            (size + destHeight) / 2f,
        )
        canvas.drawBitmap(raw, content, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        raw.recycle()
        return output
    }

    private fun maskWith(source: Bitmap, shapePath: Path, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(shapePath, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /** Tight box around everything that is not transparent. */
    private fun visibleBounds(bitmap: Bitmap, size: Int): Rect? {
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        var left = size
        var top = size
        var right = -1
        var bottom = -1
        for (y in 0 until size) {
            val row = y * size
            for (x in 0 until size) {
                if ((pixels[row + x] ushr 24) > 24) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < left || bottom < top) return null
        return Rect(left, top, right + 1, bottom + 1)
    }

    private fun averageColour(bitmap: Bitmap, bounds: Rect): Int {
        var count = 0
        var r = 0L
        var g = 0L
        var b = 0L
        val step = maxOf(1, bounds.width() / 24)
        var y = bounds.top
        while (y < bounds.bottom) {
            var x = bounds.left
            while (x < bounds.right) {
                val pixel = bitmap.getPixel(x, y)
                if ((pixel ushr 24) > 128) {
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    count++
                }
                x += step
            }
            y += step
        }
        if (count == 0) return Color.rgb(190, 195, 205)
        return Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }

    private fun blend(color: Int, towards: Int, amount: Float): Int {
        fun mix(a: Int, b: Int) = (a + (b - a) * amount).toInt().coerceIn(0, 255)
        return Color.rgb(
            mix(Color.red(color), Color.red(towards)),
            mix(Color.green(color), Color.green(towards)),
            mix(Color.blue(color), Color.blue(towards)),
        )
    }

    private companion object {
        /** How much of the tile a legacy icon's artwork fills. */
        const val LEGACY_CONTENT_FRACTION = 0.62f
    }
}

/** Superellipse-based icon masks, shared by the renderer and the Compose UI. */
object IconShapes {

    val ids = listOf("squircle", "circle", "rounded", "square")

    fun label(id: String): String = when (id) {
        "circle" -> "Circle"
        "rounded" -> "Rounded"
        "square" -> "Square"
        else -> "Squircle"
    }

    fun path(shape: String, size: Float): Path {
        val path = Path()
        val r = size / 2f
        when (shape) {
            "circle" -> path.addCircle(r, r, r, Path.Direction.CW)
            "square" -> path.addRect(0f, 0f, size, size, Path.Direction.CW)
            "rounded" -> path.addRoundRect(RectF(0f, 0f, size, size), size * 0.22f, size * 0.22f, Path.Direction.CW)
            else -> superellipse(path, size, 2.9f)
        }
        return path
    }

    private fun superellipse(path: Path, size: Float, exponent: Float) {
        val r = size / 2f
        val steps = 96
        val p = 2f / exponent
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2f * Math.PI).toFloat()
            val c = cos(t)
            val s = sin(t)
            val x = r + r * sign(c) * abs(c).pow(p)
            val y = r + r * sign(s) * abs(s).pow(p)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun sign(v: Float) = if (v < 0f) -1f else 1f
}
