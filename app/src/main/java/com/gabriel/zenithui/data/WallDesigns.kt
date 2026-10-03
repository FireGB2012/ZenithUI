package com.gabriel.zenithui.data

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The designs themselves. Each paints a dark picture and a light one from the same five colours:
 * the light one is not the dark one washed out, but drawn for a pale page — gentler light, lines
 * a shade deeper, and the lower part kept bright enough for dark labels to be read on it.
 */
internal object WallDesigns {

    fun paint(style: WallStyle, easel: Easel) = with(easel) {
        when (style) {
            WallStyle.Glow -> glow()
            WallStyle.Calm -> calm()
            WallStyle.Waves -> waves()
            WallStyle.Orbit -> orbit()
            WallStyle.Facets -> facets()
            WallStyle.Bands -> bands()
            WallStyle.Dunes -> dunes()
            WallStyle.Aurora -> aurora()
        }
    }

    // ------------------------------------------------------------------------------ soft

    private fun Easel.glow() {
        backdrop()
        light(w * 0.15f, h * 0.14f, w * 1.0f, c.accent, 0.62f)
        light(w * 0.95f, h * 0.48f, w * 0.9f, c.accentAlt, 0.48f)
        light(w * 0.35f, h * 0.98f, w * 1.15f, mix(c.accent, c.accentAlt, 0.5f), 0.38f)
    }

    private fun Easel.calm() {
        backdrop()
        light(w * 0.5f, h * 1.08f, w * 1.05f, c.accent, 0.5f)
        light(w * 0.5f, -h * 0.08f, w * 0.75f, c.accentAlt, 0.16f)
    }

    // ------------------------------------------------------------------------------ lines

    private fun Easel.waves() {
        backdrop()
        light(w * 0.1f, h * 0.3f, w * 0.9f, c.accent, 0.35f)
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
                alpha(ink(c.accent), (220 * fade).toInt()), alpha(ink(c.accentAlt), (220 * fade).toInt()),
                Shader.TileMode.CLAMP,
            )
            // A soft glow under each line, then the line itself, crisp.
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 16f * unit
                shader = gradient
                alpha = if (dark) 70 else 46
                maskFilter = BlurMaskFilter(18f * unit.coerceAtLeast(0.2f), BlurMaskFilter.Blur.NORMAL)
            })
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, 3f * unit)
                shader = gradient
            })
        }
    }

    private fun Easel.orbit() {
        backdrop()
        val cx = -w * 0.18f
        val cy = h * 1.04f
        light(cx, cy, w * 1.1f, c.accent, 0.55f)
        val rings = 14
        for (i in 1..rings) {
            val t = i / rings.toFloat()
            canvas.drawCircle(cx, cy, w * 0.13f * i, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, 2f * unit)
                color = alpha(ink(mix(c.accent, c.accentAlt, t)), ((if (dark) 120 else 150) * (1f - t * 0.7f)).toInt())
            })
        }
        // One small world on the sixth ring.
        val angle = -PI.toFloat() * 0.34f
        val r = w * 0.13f * 6
        val px = cx + r * cos(angle)
        val py = cy + r * sin(angle)
        val size = w * 0.055f
        light(px, py, size * 4f, c.accentAlt, 0.35f)
        canvas.drawCircle(px, py, size, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(px - size, py - size, px + size, py + size, ink(c.accentAlt), ink(c.accent), Shader.TileMode.CLAMP)
        })
    }

    // ------------------------------------------------------------------------------ shapes

    private fun Easel.facets() {
        val cols = 6
        val rows = 13
        val cellW = w / cols
        val cellH = h / rows
        // A grid of points nudged off true, so the triangles between them are all a little different.
        val xs = Array(rows + 1) {
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
            val tint = (random.nextFloat() - 0.5f) * 0.14f
            tone = if (tint > 0) mix(tone, Color.WHITE, tint) else mix(tone, Color.BLACK, -tint)
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
        // The lower half fades into the backdrop — darker in the dark picture, paler in the light
        // one — so icons and the dock stay easy to read.
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, h * 0.4f, 0f, h, alpha(c.base, 0), alpha(c.base, if (dark) 170 else 215), Shader.TileMode.CLAMP)
        })
        calmTop()
    }

    private fun Easel.bands() {
        backdrop()
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
            shader = LinearGradient(0f, 0f, 0f, h, alpha(c.base, 60), alpha(c.base, if (dark) 200 else 225), Shader.TileMode.CLAMP)
        })
        calmTop(60)
    }

    // ------------------------------------------------------------------------------ scenes

    private fun Easel.dunes() {
        backdrop()
        light(w * 0.72f, h * 0.22f, w * 0.75f, c.accent, 0.5f)
        val layers = 5
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * unit
            color = if (dark) alpha(Color.WHITE, 26) else alpha(ink(c.accentAlt), 60)
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
            // In the light picture the dunes are paler from the start, and each one nearer the
            // foot of the screen is paler still, so that labels are never set on a strong colour.
            val crest = mix(c.accentAlt, c.mid, if (dark) 0.35f else 0.62f)
            val tone = mix(crest, c.base, t * 0.85f)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, top - h * 0.06f, 0f, h, tone, mix(tone, c.base, 0.6f), Shader.TileMode.CLAMP)
            }
            canvas.drawPath(path, fill)
            canvas.drawPath(path, edge)
        }
    }

    private fun Easel.aurora() {
        if (dark) {
            val night = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h, mix(c.deep, Color.BLACK, 0.45f), c.base, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, w, h, night)
        } else {
            // By day the same curtains hang in a pale sky.
            backdrop()
        }
        // Stars at night; by day a few faint motes, so the sky above the curtains is not empty.
        val star = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(140) {
            val a = 60 + random.nextInt(150)
            star.color = if (dark) alpha(Color.WHITE, a) else alpha(ink(c.accentAlt), a / 5)
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
                    intArrayOf(alpha(tone, 0), alpha(tone, if (dark) 150 else 120), alpha(tone, 0)),
                    floatArrayOf(0f, 0.25f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawLine(x, top, x, top + length, curtain)
                x += step
            }
        }
        // A ridge to stand on: a dark one at night, a pale one by day.
        val ridge = Path().apply {
            moveTo(0f, h * 0.83f)
            cubicTo(w * 0.25f, h * 0.76f, w * 0.55f, h * 0.86f, w, h * 0.79f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        canvas.drawPath(ridge, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) mix(c.base, Color.BLACK, 0.55f) else mix(c.base, c.accent, 0.12f)
        })
    }
}
