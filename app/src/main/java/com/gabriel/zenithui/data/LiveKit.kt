package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import java.util.stream.IntStream
import kotlin.math.ceil

/**
 * One of the designs that move (the Live shelf), as Android's live wallpaper draws it: layers
 * painted once, slowly and with all the care of the Depth designs, then posed afresh on every
 * frame with nothing heavier than a move, a turn or a fade, which a phone's graphics do for
 * almost nothing. A frame never paints a pixel by hand.
 *
 * [draw] is given the time in seconds, which an idle design moves by, and how open an unlock
 * design is: 0 its lock-screen pose, 1 its home-screen pose (see [LiveUnlock]). Any frame can be
 * drawn on its own, in any order, so a picture of the design is the frame at 0 seconds, open.
 */
internal abstract class LiveScene(val c: WallColors, val w: Float, val h: Float) {

    val dark: Boolean = c.dark

    /** Sizes are designed at 1080 pixels wide and scaled by this, as on [Easel]. */
    val unit: Float = w / 1080f

    /** How long the opening of an unlock design takes; nothing for an idle one. */
    open val openMs: Long = 0L

    private val made = mutableListOf<Bitmap>()

    /** Paints the layers: the slow part, done once, off the main thread. */
    abstract fun prepare()

    /** Draws the design [seconds] into its motion, [open] of the way from its lock pose to its home pose. */
    abstract fun draw(canvas: Canvas, seconds: Double, open: Float)

    /** Lets go of the layers. The scene cannot be drawn again. */
    fun release() {
        made.forEach { it.recycle() }
        made.clear()
    }

    /** Keeps [bitmap] to be let go of with the scene. */
    protected fun keep(bitmap: Bitmap): Bitmap = bitmap.also { made += it }

    /** A paint for drawing layers: smooth when scaled or turned. */
    protected fun layerPaint(): Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    /**
     * A soft layer worked out a point at a time, every [step] pixels, as [Easel.pixels] does, and
     * kept small: [drawField] stretches it over the canvas. Opaque.
     */
    protected fun field(step: Int, shade: (x: Float, y: Float, px: FloatArray) -> Unit): Bitmap {
        val across = ceil(w / step).toInt().coerceAtLeast(1)
        val down = ceil(h / step).toInt().coerceAtLeast(1)
        val bitmap = keep(paintPixels(across, down, opaque = true) { i, j, px -> shade((i + 0.5f) * step, (j + 0.5f) * step, px) })
        fieldRects[bitmap] = Rect(0, 0, across, down) to RectF(0f, 0f, across * step.toFloat(), down * step.toFloat())
        return bitmap
    }

    private val fieldRects = HashMap<Bitmap, Pair<Rect, RectF>>()

    /** A [field] drawn over the canvas, each point where it was worked out. */
    protected fun drawField(canvas: Canvas, field: Bitmap, paint: Paint) {
        val (from, to) = fieldRects.getValue(field)
        canvas.drawBitmap(field, from, to, paint)
    }

    private val box = RectF()

    /** [bitmap] drawn [scale] times its size with its middle at ([x], [y]), [alpha] of the way there. */
    protected fun drawCentred(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, scale: Float, paint: Paint, alpha: Float = 1f) {
        val across = bitmap.width * scale / 2f
        val down = bitmap.height * scale / 2f
        box.set(x - across, y - down, x + across, y + down)
        val before = paint.alpha
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        canvas.drawBitmap(bitmap, null, box, paint)
        paint.alpha = before
    }

    /**
     * A layer [width] by [height] pixels with see-through parts: [shade] writes the linear light of
     * each pixel into px[0..2] and how much of it is there, 0 to 1, into px[3].
     */
    protected fun sprite(width: Int, height: Int, shade: (x: Float, y: Float, px: FloatArray) -> Unit): Bitmap =
        keep(paintPixels(width.coerceAtLeast(1), height.coerceAtLeast(1), opaque = false) { i, j, px -> shade(i + 0.5f, j + 0.5f, px) })

    /**
     * A shape alone, [width] by [height] pixels, kept as one number a pixel: [cover] says how much
     * of each pixel it covers, 0 to 1. It is drawn in the colour of the paint it is drawn with, as
     * a shadow is.
     */
    protected fun mask(width: Int, height: Int, cover: (x: Float, y: Float) -> Float): Bitmap {
        val full = paintPixels(width.coerceAtLeast(1), height.coerceAtLeast(1), opaque = false) { i, j, px -> px[3] = cover(i + 0.5f, j + 0.5f) }
        val alpha = full.extractAlpha()
        full.recycle()
        return keep(alpha)
    }

    /**
     * Fades the lower part towards the backdrop's foot, from [from] of the way down, by [strength]
     * at the foot, eased in: the quiet the icons and the dock sit on, as the still designs have.
     * Worked out on the first frame and kept, as a frame makes nothing new.
     */
    protected fun settle(canvas: Canvas, from: Float, strength: Float) {
        val paint = settlePaint ?: Paint().apply {
            val stops = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
            val colours = IntArray(stops.size) { alpha(c.base, (Shade.smoothstep(0f, 1f, stops[it]) * strength * 255f).toInt()) }
            shader = LinearGradient(0f, h * from, 0f, h, colours, stops, Shader.TileMode.CLAMP)
        }.also { settlePaint = it }
        canvas.drawRect(0f, h * from, w, h, paint)
    }

    private var settlePaint: Paint? = null

    companion object {

        /** The scene for one of the moving designs. */
        fun of(style: WallStyle, colors: WallColors, width: Float, height: Float): LiveScene = when (style) {
            WallStyle.Hover -> HoverScene(colors, width, height)
            WallStyle.Tide -> TideScene(colors, width, height)
            WallStyle.Cosmos -> CosmosScene(colors, width, height)
            WallStyle.Borealis -> BorealisScene(colors, width, height)
            WallStyle.Petals -> PetalsScene(colors, width, height)
            WallStyle.Kaleido -> KaleidoScene(colors, width, height)
            WallStyle.Flow -> FlowScene(colors, width, height)
            WallStyle.Jelly -> JellyScene(colors, width, height)
            WallStyle.Clouds -> CloudsScene(colors, width, height)
            WallStyle.Lotus -> LotusScene(colors, width, height)
            WallStyle.Daybreak -> DaybreakScene(colors, width, height)
            WallStyle.Planet -> PlanetScene(colors, width, height)
            WallStyle.Aperture -> ApertureScene(colors, width, height)
            WallStyle.Gyro -> GyroScene(colors, width, height)
            WallStyle.Ink -> InkScene(colors, width, height)
            else -> throw IllegalArgumentException("${style.name} does not move")
        }

        /**
         * A still picture of [style] on [canvas], [width] by [height]: the frame at 0 seconds,
         * [open] of the way open. What the picker's thumbnails, the glass and the lock screen show.
         */
        fun paintStill(style: WallStyle, colors: WallColors, canvas: Canvas, width: Float, height: Float, open: Float = 1f) {
            val scene = of(style, colors, width, height)
            scene.prepare()
            scene.draw(canvas, 0.0, open)
            scene.release()
        }

        /** The grain every wallpaper wears on a real screen, laid over a frame so long gradients do not band. */
        fun grain(canvas: Canvas, width: Float, height: Float, paint: Paint) {
            if (paint.shader == null) {
                paint.shader = BitmapShader(WallpaperArt.noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
    }
}

/**
 * Works out a bitmap [across] by [down] a pixel at a time on all the phone's cores: [shade] is
 * given the pixel's column and row and writes its linear light into px[0..2] and, unless [opaque],
 * how much of it is there into px[3]. Dithered as [Easel.pixels] is, so gradients do not band.
 */
internal inline fun paintPixels(across: Int, down: Int, opaque: Boolean, crossinline shade: (i: Int, j: Int, px: FloatArray) -> Unit): Bitmap {
    val out = IntArray(across * down)
    val band = 16
    val bands = (down + band - 1) / band
    IntStream.range(0, bands).parallel().forEach { b ->
        val px = FloatArray(SCRATCH)
        val top = b * band
        val bottom = minOf(down, top + band)
        for (j in top until bottom) {
            val row = j * across
            for (i in 0 until across) {
                px[0] = 0f; px[1] = 0f; px[2] = 0f; px[3] = 1f
                shade(i, j, px)
                val a = if (opaque) 1f else Shade.clamp01(px[3])
                if (a <= 0f) {
                    out[row + i] = 0
                    continue
                }
                val d = Shade.dither(i, j)
                val alpha = if (opaque) 255 else (a * 255f + 0.5f + d).toInt().coerceIn(1, 255)
                out[row + i] = (alpha shl 24) or
                    (Oklab.toChannel(px[0], d) shl 16) or
                    (Oklab.toChannel(px[1], d) shl 8) or
                    Oklab.toChannel(px[2], d)
            }
        }
    }
    return Bitmap.createBitmap(out, across, down, Bitmap.Config.ARGB_8888)
}
