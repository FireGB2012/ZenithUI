package com.gabriel.zenithui.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** The five colours a wallpaper is painted from: a theme's two accents and its three backdrop tones. */
data class WallColors(val accent: Int, val accentAlt: Int, val deep: Int, val mid: Int, val base: Int)

/** The designs Zenith can paint. Every one of them works in any theme's colours. */
enum class WallStyle(val label: String) {
    Glow("Glow"),
    Dunes("Dunes"),
    Waves("Waves"),
    Orbit("Orbit"),
    Aurora("Aurora"),
    Facets("Facets"),
    Bands("Bands"),
    Calm("Calm"),
}

/**
 * Wallpapers painted on the phone rather than shipped as pictures: nothing to download, nothing
 * to fall behind a new screen size, and every design can be drawn in the colours of any theme.
 *
 * Everything is placed as a fraction of the canvas and seeded from the design and the theme, so a
 * thumbnail and the full-size wallpaper are the same picture at two sizes.
 */
object WallpaperArt {

    fun render(style: WallStyle, colors: WallColors, width: Int, height: Int, seed: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        // Line widths and blurs are designed at 1080 pixels wide and scaled from there.
        val unit = w / 1080f
        val random = Random(seed)
        when (style) {
            WallStyle.Glow -> glow(canvas, colors, w, h)
            WallStyle.Dunes -> dunes(canvas, colors, w, h, unit, random)
            WallStyle.Waves -> waves(canvas, colors, w, h, unit, random)
            WallStyle.Orbit -> orbit(canvas, colors, w, h, unit)
            WallStyle.Aurora -> aurora(canvas, colors, w, h, unit, random)
            WallStyle.Facets -> facets(canvas, colors, w, h, random)
            WallStyle.Bands -> bands(canvas, colors, w, h)
            WallStyle.Calm -> calm(canvas, colors, w, h)
        }
        // A fine grain keeps big gradients from banding on a real screen. Thumbnails are too small
        // to show it, so they skip it.
        if (width >= 600) grain(canvas, w, h)
        return bitmap
    }

    /**
     * The whole screen in pixels, including whatever the status and navigation bars cover, because
     * a wallpaper is painted behind them. Window metrics arrived in Android 11; before that the
     * display is measured directly.
     */
    @Suppress("DEPRECATION")
    private fun screenSize(context: Context): Pair<Int, Int> {
        val windows = context.getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windows.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val metrics = DisplayMetrics()
        windows.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    /**
     * Paints [style] at the size of this phone's screen and makes it the wallpaper. [lockToo] sets
     * the lock screen as well. Returns false if Android would not take it.
     */
    suspend fun apply(context: Context, style: WallStyle, colors: WallColors, seed: Int, lockToo: Boolean): Boolean =
        withContext(Dispatchers.Default) {
            runCatching {
                val (screenWidth, screenHeight) = screenSize(context)
                val bitmap = render(style, colors, screenWidth, screenHeight, seed)
                val which = if (lockToo) {
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                } else {
                    WallpaperManager.FLAG_SYSTEM
                }
                withContext(Dispatchers.IO) {
                    WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, which)
                }
                bitmap.recycle()
                true
            }.getOrDefault(false)
        }

    // ------------------------------------------------------------------------------ designs

    private fun backdrop(canvas: Canvas, c: WallColors, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(c.deep, c.mid, c.base), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    /** A soft pool of light: brightest in the middle, gone by [radius]. */
    private fun light(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int, strength: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val a = (strength * 255).toInt().coerceIn(0, 255)
        paint.shader = RadialGradient(
            x, y, radius,
            intArrayOf(alpha(color, a), alpha(color, (a * 0.42f).toInt()), alpha(color, 0)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(x, y, radius, paint)
    }

    private fun glow(canvas: Canvas, c: WallColors, w: Float, h: Float) {
        backdrop(canvas, c, w, h)
        light(canvas, w * 0.15f, h * 0.14f, w * 1.0f, c.accent, 0.62f)
        light(canvas, w * 0.95f, h * 0.48f, w * 0.9f, c.accentAlt, 0.48f)
        light(canvas, w * 0.35f, h * 0.98f, w * 1.15f, mix(c.accent, c.accentAlt, 0.5f), 0.38f)
    }

    private fun calm(canvas: Canvas, c: WallColors, w: Float, h: Float) {
        backdrop(canvas, c, w, h)
        light(canvas, w * 0.5f, h * 1.08f, w * 1.05f, c.accent, 0.5f)
        light(canvas, w * 0.5f, -h * 0.08f, w * 0.75f, c.accentAlt, 0.16f)
    }

    private fun dunes(canvas: Canvas, c: WallColors, w: Float, h: Float, unit: Float, random: Random) {
        backdrop(canvas, c, w, h)
        light(canvas, w * 0.72f, h * 0.22f, w * 0.75f, c.accent, 0.5f)
        val layers = 5
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * unit
            color = alpha(Color.WHITE, 26)
        }
        for (i in 0 until layers) {
            val t = i / (layers - 1f)
            val top = h * (0.46f + 0.1f * i)
            val path = Path().apply {
                moveTo(0f, top + random.nextFloat() * h * 0.04f)
                cubicTo(
                    w * 0.3f, top - h * (0.04f + random.nextFloat() * 0.06f),
                    w * 0.65f, top + h * (0.02f + random.nextFloat() * 0.06f),
                    w, top - h * random.nextFloat() * 0.05f,
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            val tone = mix(mix(c.accentAlt, c.mid, 0.35f), c.base, t * 0.85f)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, top - h * 0.06f, 0f, h, tone, mix(tone, c.base, 0.6f), Shader.TileMode.CLAMP)
            }
            canvas.drawPath(path, fill)
            canvas.drawPath(path, edge)
        }
    }

    private fun waves(canvas: Canvas, c: WallColors, w: Float, h: Float, unit: Float, random: Random) {
        backdrop(canvas, c, w, h)
        light(canvas, w * 0.1f, h * 0.3f, w * 0.9f, c.accent, 0.35f)
        val lines = 9
        val phase = random.nextFloat() * 2f * PI.toFloat()
        for (k in 0 until lines) {
            val baseY = h * (0.38f + 0.035f * k)
            val amplitude = h * (0.05f + 0.01f * k)
            val path = Path()
            val steps = 90
            for (s in 0..steps) {
                val x = w * s / steps
                val y = baseY + amplitude * sin(x / w * 2f * PI.toFloat() * 1.1f + phase + k * 0.35f)
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val fade = 1f - k / lines.toFloat()
            val gradient = LinearGradient(
                0f, 0f, w, 0f,
                alpha(c.accent, (220 * fade).toInt()), alpha(c.accentAlt, (220 * fade).toInt()),
                Shader.TileMode.CLAMP,
            )
            // A soft glow under each line, then the line itself, crisp.
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 16f * unit
                shader = gradient
                alpha = 70
                maskFilter = BlurMaskFilter(18f * unit.coerceAtLeast(0.2f), BlurMaskFilter.Blur.NORMAL)
            })
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, 3f * unit)
                shader = gradient
            })
        }
    }

    private fun orbit(canvas: Canvas, c: WallColors, w: Float, h: Float, unit: Float) {
        backdrop(canvas, c, w, h)
        val cx = -w * 0.18f
        val cy = h * 1.04f
        light(canvas, cx, cy, w * 1.1f, c.accent, 0.55f)
        val rings = 14
        for (i in 1..rings) {
            val t = i / rings.toFloat()
            canvas.drawCircle(cx, cy, w * 0.13f * i, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, 2f * unit)
                color = alpha(mix(c.accent, c.accentAlt, t), (120 * (1f - t * 0.7f)).toInt())
            })
        }
        // One small world on the sixth ring.
        val angle = -PI.toFloat() * 0.34f
        val r = w * 0.13f * 6
        val px = cx + r * cos(angle)
        val py = cy + r * sin(angle)
        val size = w * 0.055f
        light(canvas, px, py, size * 4f, c.accentAlt, 0.35f)
        canvas.drawCircle(px, py, size, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(px - size, py - size, px + size, py + size, c.accentAlt, c.accent, Shader.TileMode.CLAMP)
        })
    }

    private fun aurora(canvas: Canvas, c: WallColors, w: Float, h: Float, unit: Float, random: Random) {
        val night = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, mix(c.deep, Color.BLACK, 0.45f), c.base, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, night)
        // Stars.
        val star = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(140) {
            star.color = alpha(Color.WHITE, 60 + random.nextInt(150))
            canvas.drawCircle(random.nextFloat() * w, random.nextFloat() * h * 0.75f, max(0.6f, (0.8f + random.nextFloat() * 1.8f) * unit), star)
        }
        // Curtains: rows of short vertical strokes whose tops ripple, each fading at both ends.
        val step = max(1f, 3f * unit)
        val curtain = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = step * 1.4f
            maskFilter = BlurMaskFilter(max(0.5f, 5f * unit), BlurMaskFilter.Blur.NORMAL)
        }
        val tones = listOf(c.accent, c.accentAlt, mix(c.accent, c.accentAlt, 0.5f))
        tones.forEachIndexed { j, tone ->
            val phase = random.nextFloat() * 6f
            var x = 0f
            while (x <= w) {
                val u = x / w
                val top = h * (0.16f + 0.07f * j) + h * 0.05f * sin(u * 5.5f + phase) + h * 0.02f * sin(u * 13f + phase * 2f)
                val length = h * (0.16f + 0.06f * sin(u * 3f + phase))
                curtain.shader = LinearGradient(
                    0f, top, 0f, top + length,
                    intArrayOf(alpha(tone, 0), alpha(tone, 150), alpha(tone, 0)),
                    floatArrayOf(0f, 0.25f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawLine(x, top, x, top + length, curtain)
                x += step
            }
        }
        // A dark ridge to stand on.
        val ridge = Path().apply {
            moveTo(0f, h * 0.83f)
            cubicTo(w * 0.25f, h * 0.76f, w * 0.55f, h * 0.86f, w, h * 0.79f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        canvas.drawPath(ridge, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mix(c.base, Color.BLACK, 0.55f) })
    }

    private fun facets(canvas: Canvas, c: WallColors, w: Float, h: Float, random: Random) {
        val cols = 6
        val rows = 13
        val cellW = w / cols
        val cellH = h / rows
        // A grid of points nudged off true, so the triangles between them are all a little different.
        val xs = Array(rows + 1) { r ->
            FloatArray(cols + 1) { col ->
                val edge = col == 0 || col == cols
                col * cellW + if (edge) 0f else (random.nextFloat() - 0.5f) * cellW * 0.7f
            }
        }
        val ys = Array(rows + 1) { r ->
            FloatArray(cols + 1) {
                val edge = r == 0 || r == rows
                r * cellH + if (edge) 0f else (random.nextFloat() - 0.5f) * cellH * 0.7f
            }
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val seam = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
        fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            val cx = (x1 + x2 + x3) / 3f / w
            val cy = (y1 + y2 + y3) / 3f / h
            val along = (cy * 0.75f + cx * 0.25f).coerceIn(0f, 1f)
            var tone = if (along < 0.45f) mix(c.accent, c.accentAlt, along / 0.45f) else mix(c.accentAlt, c.base, ((along - 0.45f) / 0.55f).coerceIn(0f, 1f))
            tone = mix(tone, c.deep, 0.25f)
            val shade = (random.nextFloat() - 0.5f) * 0.14f
            tone = if (shade > 0) mix(tone, Color.WHITE, shade) else mix(tone, Color.BLACK, -shade)
            fill.color = tone
            seam.color = tone
            val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close() }
            canvas.drawPath(path, fill)
            canvas.drawPath(path, seam)
        }
        for (r in 0 until rows) for (col in 0 until cols) {
            tri(xs[r][col], ys[r][col], xs[r][col + 1], ys[r][col + 1], xs[r + 1][col], ys[r + 1][col])
            tri(xs[r][col + 1], ys[r][col + 1], xs[r + 1][col + 1], ys[r + 1][col + 1], xs[r + 1][col], ys[r + 1][col])
        }
        // Darken the lower half so icons and the dock stay easy to read.
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, h * 0.4f, 0f, h, alpha(c.base, 0), alpha(c.base, 170), Shader.TileMode.CLAMP)
        })
    }

    private fun bands(canvas: Canvas, c: WallColors, w: Float, h: Float) {
        backdrop(canvas, c, w, h)
        val tones = listOf(c.accent, c.accentAlt, c.mid, mix(c.accent, c.base, 0.5f), mix(c.accentAlt, c.deep, 0.4f))
        canvas.save()
        canvas.rotate(-24f, w / 2f, h / 2f)
        val band = w * 0.2f
        var x = -w
        var i = 0
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        while (x < w * 2f) {
            paint.color = alpha(tones[i % tones.size], 150 - (i % 3) * 30)
            canvas.drawRect(x, -h, x + band, h * 2f, paint)
            x += band
            i++
        }
        canvas.restore()
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, alpha(c.base, 60), alpha(c.base, 200), Shader.TileMode.CLAMP)
        })
    }

    // ------------------------------------------------------------------------------ helpers

    private val noise: Bitmap by lazy {
        val size = 160
        val random = Random(7)
        val pixels = IntArray(size * size) {
            val v = random.nextInt(256)
            Color.argb(random.nextInt(18), v, v, v)
        }
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun grain(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint().apply { shader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun mix(a: Int, b: Int, t: Float): Int = ColorUtils.blendARGB(a, b, t.coerceIn(0f, 1f))

    private fun alpha(color: Int, a: Int): Int = ColorUtils.setAlphaComponent(color, a.coerceIn(0, 255))
}
