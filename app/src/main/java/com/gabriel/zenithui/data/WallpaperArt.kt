package com.gabriel.zenithui.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.ColorUtils
import kotlin.random.Random

/**
 * The five colours a wallpaper is painted from: a theme's two accents and its three backdrop
 * tones, and whether they are the theme's dark colours or its light ones. Every design paints a
 * light picture and a dark one.
 */
data class WallColors(val accent: Int, val accentAlt: Int, val deep: Int, val mid: Int, val base: Int, val dark: Boolean = true)

/** The shelves the designs are sorted onto in the picker. */
enum class WallGroup(val label: String) {
    Soft("Soft"),
    Lines("Lines"),
    Shapes("Shapes"),
    Scenes("Scenes"),
}

/**
 * The designs Zenith can paint. Every one of them works in any theme's colours, light and dark.
 * Those marked [plus] are for Zenith Plus; each shelf keeps designs anyone can use, and they come
 * first on it. A design is remembered by its name, so the names stay as they are; the order here
 * is the order in the picker.
 */
enum class WallStyle(val label: String, val group: WallGroup, val plus: Boolean = false) {
    Glow("Glow", WallGroup.Soft),
    Calm("Calm", WallGroup.Soft),
    Mesh("Mesh", WallGroup.Soft),
    Drift("Drift", WallGroup.Soft),
    Dawn("Dawn", WallGroup.Soft),
    Haze("Haze", WallGroup.Soft),
    Bokeh("Bokeh", WallGroup.Soft),
    Silk("Silk", WallGroup.Soft, plus = true),
    Halo("Halo", WallGroup.Soft, plus = true),
    Waves("Waves", WallGroup.Lines),
    Orbit("Orbit", WallGroup.Lines),
    Contour("Contour", WallGroup.Lines),
    Ripple("Ripple", WallGroup.Lines),
    Grid("Grid", WallGroup.Lines),
    Rays("Rays", WallGroup.Lines),
    Spiral("Spiral", WallGroup.Lines),
    Weave("Weave", WallGroup.Lines),
    Ribbons("Ribbons", WallGroup.Lines, plus = true),
    Facets("Facets", WallGroup.Shapes),
    Bands("Bands", WallGroup.Shapes),
    Bubbles("Bubbles", WallGroup.Shapes),
    Tiles("Tiles", WallGroup.Shapes),
    Dots("Dots", WallGroup.Shapes),
    Hex("Hex", WallGroup.Shapes),
    Prism("Prism", WallGroup.Shapes, plus = true),
    Bloom("Bloom", WallGroup.Shapes, plus = true),
    Dunes("Dunes", WallGroup.Scenes),
    Aurora("Aurora", WallGroup.Scenes),
    Peaks("Peaks", WallGroup.Scenes),
    City("City", WallGroup.Scenes),
    Forest("Forest", WallGroup.Scenes),
    Horizon("Horizon", WallGroup.Scenes, plus = true),
    Nebula("Nebula", WallGroup.Scenes, plus = true);

    companion object {
        fun byName(name: String): WallStyle? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Wallpapers painted on the phone rather than shipped as pictures: nothing to download, nothing
 * to fall behind a new screen size, and every design can be drawn in the colours of any theme.
 *
 * Everything is placed as a fraction of the canvas and seeded from the design alone, so a
 * thumbnail and the full-size wallpaper are the same picture at two sizes, and a design keeps its
 * shape when the theme changes under it: only the colours move.
 */
object WallpaperArt {

    fun render(style: WallStyle, colors: WallColors, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val easel = Easel(Canvas(bitmap), colors, width.toFloat(), height.toFloat(), Random(style.name.hashCode()))
        WallDesigns.paint(style, easel)
        // A fine grain keeps big gradients from banding on a real screen. Thumbnails are too small
        // to show it, so they skip it.
        if (width >= 600) easel.grain()
        return bitmap
    }

    /**
     * The whole screen in pixels, including whatever the status and navigation bars cover, because
     * a wallpaper is painted behind them. Window metrics arrived in Android 11; before that the
     * display is measured directly.
     */
    @Suppress("DEPRECATION")
    internal fun screenSize(context: Context): Pair<Int, Int> {
        val windows = context.getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windows.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val metrics = DisplayMetrics()
        windows.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    internal val noise: Bitmap by lazy {
        val size = 160
        val random = Random(7)
        val pixels = IntArray(size * size) {
            val v = random.nextInt(256)
            Color.argb(random.nextInt(18), v, v, v)
        }
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}

/**
 * What a design paints with: the canvas, the five colours, and the few strokes every design
 * shares. Sizes are fractions of [w] and [h]; line widths and blurs are designed at 1080 pixels
 * wide and scaled by [unit].
 */
internal class Easel(val canvas: Canvas, val c: WallColors, val w: Float, val h: Float, val random: Random) {

    val unit: Float = w / 1080f

    val dark: Boolean get() = c.dark

    /** What the page fades towards: black at night, white by day. */
    val shade: Int get() = if (c.dark) Color.BLACK else Color.WHITE

    fun backdrop() {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(c.deep, c.mid, c.base), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    /** The backdrop's colour at one height, 0 at the top to 1 at the foot, for designs worked out point by point. */
    fun ground(v: Float): Int = if (v < 0.5f) mix(c.deep, c.mid, v * 2f) else mix(c.mid, c.base, v * 2f - 1f)

    /**
     * A soft pool of colour: strongest in the middle, gone by [radius]. On a pale page the same
     * pool reads far stronger than it does in the dark, so there it is laid on more thinly.
     */
    fun light(x: Float, y: Float, radius: Float, color: Int, strength: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val a = ((if (c.dark) strength else strength * 0.66f) * 255).toInt().coerceIn(0, 255)
        paint.shader = RadialGradient(
            x, y, radius,
            intArrayOf(alpha(color, a), alpha(color, (a * 0.42f).toInt()), alpha(color, 0)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(x, y, radius, paint)
    }

    /** A pool of light pulled long: [length] from end to end, [width] across, turned by [degrees]. */
    fun streak(x: Float, y: Float, length: Float, width: Float, degrees: Float, color: Int, strength: Float) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(degrees)
        canvas.scale(length / width, 1f)
        light(0f, 0f, width / 2f, color, strength)
        canvas.restore()
    }

    /** A colour for a line: as it is on a dark page, a shade deeper on a pale one so it still shows. */
    fun ink(color: Int): Int = if (c.dark) color else mix(color, Color.BLACK, 0.16f)

    /**
     * A picture worked out colour by colour rather than drawn: [colourAt] is asked for each point
     * of a small grid, [across] points wide, u and v running from 0 to 1, and the grid is stretched
     * smoothly over the canvas. Soft things only — anything with an edge would come out blurred.
     */
    fun field(across: Int = 72, colourAt: (u: Float, v: Float) -> Int) {
        val down = (across * h / w).toInt().coerceAtLeast(2)
        val pixels = IntArray(across * down)
        for (y in 0 until down) {
            val v = y / (down - 1f)
            for (x in 0 until across) pixels[y * across + x] = colourAt(x / (across - 1f), v)
        }
        val small = Bitmap.createBitmap(pixels, across, down, Bitmap.Config.ARGB_8888)
        // Half a point is cut from every side, so the edge of the canvas is a real colour and not
        // the smear the stretching makes past the last point.
        val from = Rect(0, 0, across, down)
        val to = RectF(-w / across / 2f, -h / down / 2f, w + w / across / 2f, h + h / down / 2f)
        canvas.drawBitmap(small, from, to, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        small.recycle()
    }

    /** Quietens the top of a busy design, where the clock and the status icons sit. */
    fun calmTop(strength: Int = 90) {
        canvas.drawRect(0f, 0f, w, h * 0.16f, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h * 0.16f, alpha(shade, strength), alpha(shade, 0), Shader.TileMode.CLAMP)
        })
    }

    /** Fades the lower part into the backdrop, so the icons and the dock sit on something quiet. */
    fun settle(from: Float = 0.4f, strength: Int = 170) {
        canvas.drawRect(0f, h * from, w, h, Paint().apply {
            shader = LinearGradient(0f, h * from, 0f, h, alpha(c.base, 0), alpha(c.base, strength), Shader.TileMode.CLAMP)
        })
    }

    fun grain() {
        val paint = Paint().apply { shader = BitmapShader(WallpaperArt.noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
        canvas.drawRect(0f, 0f, w, h, paint)
    }
}

internal fun mix(a: Int, b: Int, t: Float): Int = ColorUtils.blendARGB(a, b, t.coerceIn(0f, 1f))

internal fun alpha(color: Int, a: Int): Int = ColorUtils.setAlphaComponent(color, a.coerceIn(0, 255))
