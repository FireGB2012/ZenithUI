package com.gabriel.zenithui.data

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
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
            WallStyle.Mesh -> mesh()
            WallStyle.Drift -> drift()
            WallStyle.Silk -> silk()
            WallStyle.Halo -> halo()
            WallStyle.Waves -> waves()
            WallStyle.Orbit -> orbit()
            WallStyle.Contour -> contour()
            WallStyle.Ripple -> ripple()
            WallStyle.Grid -> grid()
            WallStyle.Ribbons -> ribbons()
            WallStyle.Facets -> facets()
            WallStyle.Bands -> bands()
            WallStyle.Bubbles -> bubbles()
            WallStyle.Prism -> prism()
            WallStyle.Bloom -> bloom()
            WallStyle.Dunes -> dunes()
            WallStyle.Aurora -> aurora()
            WallStyle.Peaks -> peaks()
            WallStyle.Horizon -> horizon()
            WallStyle.Nebula -> nebula()
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

    /** Pools of colour that run into each other, worked out point by point so no pool has an edge. */
    private fun Easel.mesh() {
        val between = mix(c.accent, c.accentAlt, 0.5f)
        val tones = intArrayOf(c.accent, c.accentAlt, between, c.accentAlt, c.accent)
        val xs = floatArrayOf(0.1f, 0.92f, 0.38f, 0.05f, 0.85f)
        val ys = floatArrayOf(0.08f, 0.24f, 0.46f, 0.74f, 0.96f)
        val reach = floatArrayOf(0.52f, 0.46f, 0.4f, 0.36f, 0.44f)
        // The pools lower down are thinner, and on a pale page all of them are.
        val pull = if (dark) floatArrayOf(2.2f, 2f, 1.5f, 0.9f, 0.8f) else floatArrayOf(1.4f, 1.2f, 0.9f, 0.5f, 0.45f)
        for (i in xs.indices) {
            xs[i] += (random.nextFloat() - 0.5f) * 0.12f
            ys[i] += (random.nextFloat() - 0.5f) * 0.08f
        }
        val tall = h / w
        field(96) { u, v ->
            val ground = ground(v)
            var weight = 1f
            var red = Color.red(ground).toFloat()
            var green = Color.green(ground).toFloat()
            var blue = Color.blue(ground).toFloat()
            for (i in tones.indices) {
                val dx = u - xs[i]
                val dy = (v - ys[i]) * tall
                val k = pull[i] * exp(-(dx * dx + dy * dy) / (reach[i] * reach[i]))
                red += Color.red(tones[i]) * k
                green += Color.green(tones[i]) * k
                blue += Color.blue(tones[i]) * k
                weight += k
            }
            Color.rgb((red / weight).toInt(), (green / weight).toInt(), (blue / weight).toInt())
        }
    }

    /** Streaks of light lying across the page at a slant, each with a brighter thread down its middle. */
    private fun Easel.drift() {
        backdrop()
        val tones = intArrayOf(c.accent, c.accentAlt, mix(c.accent, c.accentAlt, 0.5f))
        val count = 9
        repeat(count) { i ->
            val t = i / (count - 1f)
            val x = w * (0.1f + random.nextFloat() * 0.8f)
            val y = h * (0.04f + 0.86f * t + (random.nextFloat() - 0.5f) * 0.06f)
            val length = w * (1.1f + random.nextFloat() * 0.9f)
            val width = w * (0.1f + random.nextFloat() * 0.2f)
            // Bright near the top, fainter towards the dock.
            val strength = 0.62f - 0.4f * t
            val tone = tones[i % tones.size]
            streak(x, y, length, width, -34f, tone, strength)
            streak(x, y, length * 0.8f, width * 0.16f, -34f, mix(tone, if (dark) Color.WHITE else c.base, 0.35f), strength * 0.8f)
        }
    }

    /** Cloth lying in folds: bright along each ridge, deep in each hollow, and smooth between. */
    private fun Easel.silk() {
        val phase = random.nextFloat() * 6f
        val turn = 2f * PI.toFloat() * 1.6f
        field(144) { u, v ->
            // The folds run at a slant and wander, as cloth does when nothing pulls it straight.
            val wander = 0.22f * sin(v * 4.2f + phase) + 0.1f * sin(u * 5f - v * 3f + phase * 2f)
            val fold = 0.5f + 0.5f * sin((u * 0.9f + v * 1.35f + wander) * turn)
            val sheen = fold * fold * (3f - 2f * fold)
            val tone = mix(c.accent, c.accentAlt, (u * 0.4f + v * 0.9f).coerceIn(0f, 1f))
            // Towards the dock the cloth sinks into the backdrop.
            val sunk = ((v - 0.3f) / 0.6f).coerceIn(0f, 1f)
            val depth = (if (dark) 0.1f + 0.62f * sheen else 0.05f + 0.42f * sheen) * (1f - 0.78f * sunk)
            val cloth = mix(ground(v), tone, depth)
            // The very top of each fold catches the light.
            mix(cloth, Color.WHITE, sheen * sheen * sheen * sheen * (if (dark) 0.16f else 0.3f) * (1f - sunk))
        }
        calmTop(if (dark) 70 else 50)
    }

    /** One ring of light, with what is inside it a shade quieter than what is outside. */
    private fun Easel.halo() {
        backdrop()
        val cx = w * 0.5f
        val cy = h * 0.37f
        val r = w * 0.31f
        light(cx, cy, w * 1.15f, mix(c.accent, c.accentAlt, 0.5f), 0.32f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // The hush inside the ring.
        paint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(alpha(shade, if (dark) 120 else 170), alpha(shade, if (dark) 105 else 150), alpha(shade, 0)),
            floatArrayOf(0f, 0.86f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        // The ring and its corona: nothing until just inside the ring, brightest on it, then a
        // long fall away. Drawn twice, a hair apart, one in each accent, so its edges are two colours.
        val reach = r * 2.1f
        val at = r / reach
        val k = if (dark) 1f else 0.78f
        fun ring(x: Float, y: Float, tone: Int) {
            paint.shader = RadialGradient(
                x, y, reach,
                intArrayOf(
                    alpha(tone, 0), alpha(tone, 0), alpha(tone, (70 * k).toInt()), alpha(tone, (240 * k).toInt()),
                    alpha(tone, (95 * k).toInt()), alpha(tone, (30 * k).toInt()), alpha(tone, 0),
                ),
                floatArrayOf(0f, at * 0.8f, at * 0.955f, at, at * 1.08f, at * 1.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, reach, paint)
        }
        val slip = r * 0.035f
        ring(cx - slip, cy - slip, c.accent)
        ring(cx + slip, cy + slip, c.accentAlt)
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, 2f * unit)
            color = if (dark) alpha(Color.WHITE, 150) else alpha(ink(mix(c.accent, c.accentAlt, 0.5f)), 120)
        })
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

    /** The lines of a map: two hills, and every line one height all the way round. */
    private fun Easel.contour() {
        backdrop()
        val ax = 0.24f + random.nextFloat() * 0.08f
        val ay = 0.22f + random.nextFloat() * 0.06f
        val bx = 0.72f + random.nextFloat() * 0.08f
        val by = 0.54f + random.nextFloat() * 0.06f
        light(w * ax, h * ay, w * 0.8f, c.accent, 0.4f)
        light(w * bx, h * by, w * 0.7f, c.accentAlt, 0.3f)
        val tall = h / w
        val across = 54
        val down = (across * tall).toInt().coerceAtLeast(2)
        fun hill(dx: Float, dy: Float, size: Float): Float = exp(-(dx * dx + dy * dy) / (size * size))
        val land = Array(down + 1) { j ->
            FloatArray(across + 1) { i ->
                val u = i / across.toFloat()
                val v = j / down.toFloat()
                hill(u - ax, (v - ay) * tall, 0.36f) + 0.8f * hill(u - bx, (v - by) * tall, 0.32f) +
                    0.06f * sin(u * 9f + v * 7f) + 0.05f * sin(v * 13f - u * 4f)
            }
        }
        val cellW = w / across
        val cellH = h / down
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, 2.2f * unit)
            strokeCap = Paint.Cap.ROUND
        }
        val points = FloatArray(across * down * 8)
        val cut = FloatArray(8)
        val levels = 12
        for (k in 1..levels) {
            val level = 0.06f + 0.94f * k / (levels + 1f)
            var n = 0
            for (j in 0 until down) for (i in 0 until across) {
                // Where this height crosses each side of the square, going round it.
                var m = 0
                fun side(x1: Float, y1: Float, v1: Float, x2: Float, y2: Float, v2: Float) {
                    if ((v1 < level) != (v2 < level)) {
                        val t = (level - v1) / (v2 - v1)
                        cut[m++] = x1 + (x2 - x1) * t
                        cut[m++] = y1 + (y2 - y1) * t
                    }
                }
                val left = i * cellW
                val top = j * cellH
                val a = land[j][i]
                val b = land[j][i + 1]
                val d = land[j + 1][i + 1]
                val e = land[j + 1][i]
                side(left, top, a, left + cellW, top, b)
                side(left + cellW, top, b, left + cellW, top + cellH, d)
                side(left + cellW, top + cellH, d, left, top + cellH, e)
                side(left, top + cellH, e, left, top, a)
                if (m >= 4) for (q in 0 until 4) points[n++] = cut[q]
                if (m == 8) for (q in 4 until 8) points[n++] = cut[q]
            }
            val t = k / levels.toFloat()
            line.color = alpha(ink(mix(c.accentAlt, c.accent, t)), (if (dark) 70 + 130 * t else 80 + 140 * t).toInt())
            canvas.drawLines(points, 0, n, line)
        }
        settle(0.5f, if (dark) 150 else 190)
    }

    /** Rings spreading over still water, seen from a little above it. */
    private fun Easel.ripple() {
        backdrop()
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        fun spread(cx: Float, cy: Float, rings: Int, first: Float, strength: Int) {
            canvas.save()
            canvas.translate(cx, cy)
            // Flattened, as circles on water are when you are not straight above them.
            canvas.scale(1f, 0.46f)
            ring.shader = SweepGradient(0f, 0f, intArrayOf(ink(c.accent), ink(c.accentAlt), ink(c.accent)), null)
            for (i in 1..rings) {
                val t = i / rings.toFloat()
                ring.strokeWidth = max(1f, (5.5f - 3.5f * t) * unit)
                ring.alpha = (strength * (1f - t).pow(1.3f)).toInt()
                canvas.drawCircle(0f, 0f, first * i * (1f + 0.04f * i), ring)
            }
            canvas.restore()
        }
        val cx = w * 0.64f
        val cy = h * 0.33f
        light(cx, cy, w * 0.95f, c.accent, 0.42f)
        spread(w * 0.14f, h * 0.61f, rings = 9, first = w * 0.07f, strength = if (dark) 120 else 140)
        spread(cx, cy, rings = 17, first = w * 0.085f, strength = if (dark) 225 else 235)
        // Where the drop went in.
        streak(cx, cy, w * 0.3f, w * 0.14f, 0f, mix(c.accentAlt, Color.WHITE, 0.4f), 0.7f)
    }

    /** A floor of squares running away to a bright horizon. */
    private fun Easel.grid() {
        backdrop()
        val horizon = h * 0.44f
        streak(w * 0.5f, horizon, w * 1.9f, h * 0.34f, 0f, c.accent, 0.6f)
        streak(w * 0.5f, horizon, w * 1.3f, h * 0.11f, 0f, c.accentAlt, 0.55f)
        canvas.save()
        canvas.clipRect(0f, horizon, w, h)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, 2.4f * unit)
        }
        // Lines running away from you all meet at one point; they fade as they near it.
        line.shader = LinearGradient(
            0f, horizon, 0f, h,
            alpha(ink(c.accentAlt), 20), alpha(ink(c.accent), if (dark) 150 else 170),
            Shader.TileMode.CLAMP,
        )
        for (k in -10..10) canvas.drawLine(w * 0.5f, horizon, w * (0.5f + k * 0.3f), h, line)
        // Lines across: evenly spaced on the floor, so closer and closer together on the page.
        line.shader = null
        var z = 1f
        while (z < 26f) {
            val y = horizon + (h - horizon) / z
            line.color = alpha(ink(mix(c.accent, c.accentAlt, (z - 1f) / 8f)), ((if (dark) 150 else 170) / z.pow(0.75f)).toInt())
            canvas.drawLine(0f, y, w, y, line)
            z *= 1.24f
        }
        canvas.restore()
        // The horizon itself, bright in the middle and gone by the edges.
        val edge = if (dark) mix(c.accentAlt, Color.WHITE, 0.5f) else ink(c.accentAlt)
        canvas.drawLine(0f, horizon, w, horizon, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = max(1f, 3f * unit)
            shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(alpha(edge, 0), alpha(edge, 230), alpha(edge, 0)), null, Shader.TileMode.CLAMP)
        })
    }

    /** Wide ribbons you can see through, crossing the page; each turns over as it goes. */
    private fun Easel.ribbons() {
        backdrop()
        light(w * 0.8f, h * 0.2f, w * 0.9f, c.accentAlt, 0.3f)
        val tones = intArrayOf(c.accent, c.accentAlt, mix(c.accent, c.accentAlt, 0.5f), c.accent)
        val around = 2f * PI.toFloat()
        val steps = 80
        val upper = FloatArray(steps + 1)
        val lower = FloatArray(steps + 1)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, 2f * unit)
        }
        for (k in tones.indices) {
            val middle = 0.2f + 0.15f * k
            val swing = 0.1f + random.nextFloat() * 0.08f
            val turns = 0.7f + random.nextFloat() * 0.6f
            val phase = random.nextFloat() * around
            val twists = 1.3f + random.nextFloat() * 1.2f
            val twistPhase = random.nextFloat() * PI.toFloat()
            val broad = h * (0.085f - 0.012f * k)
            val lean = if (k % 2 == 0) 0.12f else -0.12f
            for (s in 0..steps) {
                val u = s / steps.toFloat()
                val centre = h * (middle + swing * sin(u * turns * around + phase) + lean * (u - 0.5f))
                // A ribbon turning over shows its edge: it narrows to a line and widens again.
                val across = broad * (0.14f + 0.86f * abs(cos(u * twists * PI.toFloat() + twistPhase)))
                upper[s] = centre - across
                lower[s] = centre + across
            }
            val band = Path().apply {
                moveTo(0f, upper[0])
                for (s in 1..steps) lineTo(w * s / steps, upper[s])
                for (s in steps downTo 0) lineTo(w * s / steps, lower[s])
                close()
            }
            val top = Path().apply {
                moveTo(0f, upper[0])
                for (s in 1..steps) lineTo(w * s / steps, upper[s])
            }
            // The ribbons lower down are fainter, to leave the icons alone.
            val strength = (if (dark) 150 else 105) - 22 * k
            val tone = ink(tones[k])
            val next = ink(tones[(k + 1) % tones.size])
            fill.shader = LinearGradient(0f, 0f, w, 0f, alpha(tone, strength), alpha(next, strength * 45 / 100), Shader.TileMode.CLAMP)
            canvas.drawPath(band, fill)
            edge.color = alpha(if (dark) mix(tones[k], Color.WHITE, 0.5f) else tone, strength)
            canvas.drawPath(top, edge)
        }
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
        settle(0.4f, if (dark) 170 else 215)
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

    /** Clear orbs, coloured only towards the rim, each with a glint where the light catches it. */
    private fun Easel.bubbles() {
        backdrop()
        light(w * 0.2f, h * 0.2f, w * 0.9f, c.accent, 0.3f)
        light(w * 0.85f, h * 0.6f, w * 0.8f, c.accentAlt, 0.22f)
        // Across, down and size; the biggest first, so the small ones float in front.
        val orbs = arrayOf(
            floatArrayOf(0.72f, 0.2f, 0.3f), floatArrayOf(0.2f, 0.4f, 0.22f), floatArrayOf(0.62f, 0.52f, 0.15f),
            floatArrayOf(0.8f, 0.76f, 0.13f), floatArrayOf(0.14f, 0.13f, 0.1f), floatArrayOf(0.38f, 0.66f, 0.1f),
            floatArrayOf(0.86f, 0.42f, 0.07f), floatArrayOf(0.2f, 0.84f, 0.06f), floatArrayOf(0.46f, 0.3f, 0.05f),
            floatArrayOf(0.55f, 0.9f, 0.045f), floatArrayOf(0.36f, 0.08f, 0.04f),
        )
        val tones = intArrayOf(c.accent, c.accentAlt, mix(c.accent, c.accentAlt, 0.5f))
        val body = Paint(Paint.ANTI_ALIAS_FLAG)
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        orbs.forEachIndexed { i, orb ->
            val x = w * (orb[0] + (random.nextFloat() - 0.5f) * 0.06f)
            val y = h * (orb[1] + (random.nextFloat() - 0.5f) * 0.04f)
            val r = w * orb[2]
            val tone = tones[i % tones.size]
            body.shader = RadialGradient(
                x, y, r,
                intArrayOf(alpha(tone, 12), alpha(tone, 38), alpha(tone, if (dark) 150 else 115)),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r, body)
            val bright = if (dark) mix(tone, Color.WHITE, 0.55f) else ink(tone)
            rim.strokeWidth = max(1f, 2.5f * unit)
            rim.shader = LinearGradient(x - r, y - r, x + r, y + r, alpha(bright, 230), alpha(bright, 45), Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, r - rim.strokeWidth / 2f, rim)
            streak(x - r * 0.42f, y - r * 0.46f, r * 0.52f, r * 0.2f, -40f, Color.WHITE, if (dark) 0.6f else 1f)
        }
    }

    /** Long shards of colour fanning out from one corner, each lying partly over the last. */
    private fun Easel.prism() {
        backdrop()
        val ox = w * 0.86f
        val oy = h * 0.12f
        light(ox, oy, w * 0.9f, c.accent, 0.4f)
        val tones = intArrayOf(c.accent, c.accentAlt, mix(c.accent, c.accentAlt, 0.5f))
        val face = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, 2f * unit)
        }
        val strength = if (dark) 175 else 120
        // Straight down is 90 degrees and straight left is 180: the fan opens from one to the other.
        var angle = 74f
        repeat(9) { i ->
            val span = 12f + random.nextFloat() * 15f
            val reach = h * (0.6f + random.nextFloat() * 0.6f)
            val a1 = angle * PI.toFloat() / 180f
            val a2 = (angle + span) * PI.toFloat() / 180f
            val x1 = ox + reach * cos(a1)
            val y1 = oy + reach * sin(a1)
            val x2 = ox + reach * cos(a2)
            val y2 = oy + reach * sin(a2)
            val tone = tones[i % tones.size]
            face.shader = LinearGradient(
                ox, oy, (x1 + x2) / 2f, (y1 + y2) / 2f,
                intArrayOf(alpha(tone, strength), alpha(tone, strength / 2), alpha(tone, 0)),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(Path().apply { moveTo(ox, oy); lineTo(x1, y1); lineTo(x2, y2); close() }, face)
            val bright = if (dark) mix(tone, Color.WHITE, 0.6f) else ink(tone)
            edge.shader = LinearGradient(ox, oy, x1, y1, alpha(bright, 190), alpha(bright, 0), Shader.TileMode.CLAMP)
            canvas.drawLine(ox, oy, x1, y1, edge)
            angle += span * (0.55f + random.nextFloat() * 0.35f)
        }
        settle(0.5f, if (dark) 130 else 160)
        calmTop(70)
    }

    /** Petals opening upwards from a point low on the page: a long row behind, a shorter one in front. */
    private fun Easel.bloom() {
        backdrop()
        val ox = w * 0.5f
        val oy = h * 0.7f
        light(ox, oy - h * 0.12f, w * 0.95f, c.accent, 0.38f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, 2f * unit)
        }
        // Drawn pointing straight up, then turned to its place.
        fun petal(degrees: Float, length: Float, broad: Float, tone: Int, strength: Int) {
            canvas.save()
            canvas.rotate(degrees, ox, oy)
            val path = Path().apply {
                moveTo(ox, oy)
                cubicTo(ox - broad, oy - length * 0.35f, ox - broad * 0.75f, oy - length * 0.85f, ox, oy - length)
                cubicTo(ox + broad * 0.75f, oy - length * 0.85f, ox + broad, oy - length * 0.35f, ox, oy)
                close()
            }
            // Clear at the root, where the icons are, and full at the tip.
            fill.shader = LinearGradient(
                ox, oy, ox, oy - length,
                intArrayOf(alpha(tone, strength / 6), alpha(tone, strength), alpha(tone, strength * 7 / 10)),
                floatArrayOf(0f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, fill)
            rim.shader = LinearGradient(
                ox, oy, ox, oy - length,
                alpha(tone, 0), alpha(if (dark) mix(tone, Color.WHITE, 0.5f) else ink(tone), strength),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, rim)
            canvas.restore()
        }
        val back = if (dark) 120 else 85
        val front = if (dark) 165 else 115
        for (k in -4..4) petal(k * 21f, w * (0.74f + random.nextFloat() * 0.1f), w * 0.17f, c.accentAlt, back)
        for (k in -4..3) petal(k * 21f + 10.5f, w * (0.52f + random.nextFloat() * 0.08f), w * 0.14f, c.accent, front)
        light(ox, oy, w * 0.22f, if (dark) mix(c.accentAlt, Color.WHITE, 0.5f) else c.base, 0.7f)
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

    /** Ridge behind ridge, each one clearer than the last, with haze lying in the valleys between. */
    private fun Easel.peaks() {
        backdrop()
        light(w * 0.3f, h * 0.3f, w * 0.85f, c.accent, 0.5f)
        light(w * 0.8f, h * 0.12f, w * 0.6f, c.accentAlt, 0.25f)
        // At night the far ridges catch what light there is and the near ones are black against
        // them; by day it is the other way about, pale in the distance and full colour close to.
        val far = if (dark) mix(c.mid, c.accent, 0.42f) else mix(c.mid, c.accentAlt, 0.16f)
        val near = if (dark) mix(c.base, Color.BLACK, 0.35f) else mix(c.base, ink(c.accent), 0.42f)
        val haze = if (dark) mix(c.mid, c.accentAlt, 0.18f) else c.base
        val layers = 5
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until layers) {
            val t = i / (layers - 1f)
            val foot = h * (0.42f + 0.115f * i)
            // The far ridges are lower and smoother; the near ones stand up and show their rock.
            val rise = h * (0.07f + 0.05f * t)
            val profile = skyline(0.42f + 0.08f * t)
            val path = Path().apply {
                moveTo(0f, h)
                for (k in profile.indices) lineTo(w * k / (profile.size - 1f), foot - rise * profile[k])
                lineTo(w, h)
                close()
            }
            val tone = mix(far, near, t)
            fill.shader = LinearGradient(0f, foot - rise, 0f, foot + h * 0.15f, tone, mix(tone, haze, if (dark) 0.55f else 0.4f), Shader.TileMode.CLAMP)
            canvas.drawPath(path, fill)
        }
    }

    /**
     * The outline of a ridge, 0 for its lowest point to 1 for its highest: start with the two ends,
     * then keep nudging the middle of every stretch up or down, by less each time round.
     */
    private fun Easel.skyline(roughness: Float): FloatArray {
        val n = 64
        val p = FloatArray(n + 1)
        p[0] = random.nextFloat()
        p[n] = random.nextFloat()
        var step = n
        var range = 1f
        while (step > 1) {
            val half = step / 2
            for (i in half until n step step) p[i] = (p[i - half] + p[i + half]) / 2f + (random.nextFloat() - 0.5f) * range
            step = half
            range *= roughness
        }
        // Worn down a little: without this the last nudges leave needles no mountain has.
        val worn = FloatArray(n + 1) { i -> (p[max(i - 1, 0)] + 2f * p[i] + p[minOf(i + 1, n)]) / 4f }
        val low = worn.min()
        val span = (worn.max() - low).coerceAtLeast(0.001f)
        for (i in worn.indices) worn[i] = (worn[i] - low) / span
        return worn
    }

    /** Water to the horizon, haze lying on it, and over it the moon by night and the sun by day. */
    private fun Easel.horizon() {
        backdrop()
        val line = h * 0.6f
        val sx = w * 0.5f
        val sy = h * 0.47f
        val r = w * 0.13f
        light(sx, sy, w * 0.95f, c.accent, 0.5f)
        light(sx, sy, r * 2.8f, c.accentAlt, 0.6f)
        val disc = Paint(Paint.ANTI_ALIAS_FLAG)
        if (dark) {
            // The moon: pale, and lit from the upper left.
            val pale = mix(Color.WHITE, c.accentAlt, 0.14f)
            disc.shader = RadialGradient(sx - r * 0.35f, sy - r * 0.35f, r * 1.7f, pale, mix(pale, c.accentAlt, 0.55f), Shader.TileMode.CLAMP)
        } else {
            // The sun: full colour, deepest at its foot.
            disc.shader = LinearGradient(sx, sy - r, sx, sy + r, mix(c.accent, Color.WHITE, 0.3f), c.accentAlt, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(sx, sy, r, disc)
        // Haze along the horizon, some of it drifting across the disc.
        val mist = if (dark) mix(c.mid, c.accentAlt, 0.4f) else mix(c.base, c.accentAlt, 0.25f)
        repeat(5) { i ->
            streak(
                w * (0.15f + random.nextFloat() * 0.7f), line - h * (0.012f + 0.03f * i),
                w * (1.1f + random.nextFloat() * 0.8f), h * (0.022f + 0.012f * i),
                0f, mist, if (dark) 0.6f else 0.85f,
            )
        }
        canvas.drawRect(0f, line, w, h, Paint().apply {
            shader = LinearGradient(
                0f, line, 0f, h,
                mix(c.mid, c.accent, if (dark) 0.2f else 0.16f), if (dark) mix(c.base, Color.BLACK, 0.3f) else c.base,
                Shader.TileMode.CLAMP,
            )
        })
        // The light on the water: a path of short strokes, wider and fainter as it comes towards you.
        val shine = if (dark) mix(Color.WHITE, c.accentAlt, 0.3f) else mix(c.accent, c.accentAlt, 0.5f)
        val glint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        val strokes = 30
        for (i in 0 until strokes) {
            val t = i / (strokes - 1f)
            val y = line + (h - line) * (0.015f + 0.85f * t.pow(1.5f))
            val half = r * (0.35f + 1.5f * t) * (0.55f + 0.45f * random.nextFloat())
            val x = sx + (random.nextFloat() - 0.5f) * r * 1.2f * t
            glint.strokeWidth = max(1f, (2.5f + 5f * t) * unit)
            glint.color = alpha(shine, ((if (dark) 170 else 150) * (1f - t).pow(1.1f)).toInt())
            canvas.drawLine(x - half, y, x + half, y, glint)
        }
        glint.strokeWidth = max(1f, 1.5f * unit)
        glint.color = alpha(shine, 70)
        canvas.drawLine(0f, line, w, line, glint)
    }

    /** Clouds of colour hanging in the sky: among stars by night, and by day as pale wisps. */
    private fun Easel.nebula() {
        val size = 32
        val lattice = FloatArray(size * size) { random.nextFloat() }
        // Smooth noise: a grid of chances, and between its points an eased blend of the four nearest.
        fun noise(x: Float, y: Float): Float {
            val x0 = floor(x).toInt()
            val y0 = floor(y).toInt()
            val fx = x - x0
            val fy = y - y0
            val ex = fx * fx * (3f - 2f * fx)
            val ey = fy * fy * (3f - 2f * fy)
            fun at(i: Int, j: Int) = lattice[(j and (size - 1)) * size + (i and (size - 1))]
            val top = at(x0, y0) + (at(x0 + 1, y0) - at(x0, y0)) * ex
            val bottom = at(x0, y0 + 1) + (at(x0 + 1, y0 + 1) - at(x0, y0 + 1)) * ex
            return top + (bottom - top) * ey
        }
        // Big shapes with smaller ones on them, and smaller again: what makes it cloud and not fog.
        fun cloud(x: Float, y: Float): Float =
            noise(x, y) * 0.5f + noise(x * 2f + 7.3f, y * 2f + 1.7f) * 0.27f +
                noise(x * 4f + 13.1f, y * 4f + 5.9f) * 0.15f + noise(x * 8f + 3.7f, y * 8f + 11.3f) * 0.08f
        fun ease(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
        val tall = h / w
        val night = mix(c.deep, Color.BLACK, 0.45f)
        field(144) { u, v ->
            val sky = if (dark) mix(night, c.base, v) else ground(v)
            val x = u * 2.6f
            val y = v * tall * 2.6f
            // Thickest across the upper middle; gone by the dock.
            val gathered = exp(-(v - 0.36f) * (v - 0.36f) / 0.09f)
            val one = ease((cloud(x, y) - 0.36f) / 0.3f) * gathered
            val two = ease((cloud(x + 31.7f, y + 17.3f) - 0.38f) / 0.3f) * gathered
            var colour = mix(sky, c.accent, one * (if (dark) 0.62f else 0.4f))
            colour = mix(colour, c.accentAlt, two * (if (dark) 0.55f else 0.36f))
            // Where the two clouds lie over each other the night one burns brighter.
            if (dark) mix(colour, Color.WHITE, one * two * 0.22f) else colour
        }
        val star = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(170) {
            val a = 50 + random.nextInt(160)
            star.color = if (dark) alpha(Color.WHITE, a) else alpha(ink(c.accentAlt), a / 5)
            canvas.drawCircle(random.nextFloat() * w, random.nextFloat() * h * 0.82f, max(0.6f, (0.7f + random.nextFloat() * 1.7f) * unit), star)
        }
        // A few near stars, each with a glow.
        repeat(7) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h * 0.6f
            if (dark) {
                light(x, y, 22f * unit, Color.WHITE, 0.55f)
                star.color = Color.WHITE
                canvas.drawCircle(x, y, max(0.8f, 2.6f * unit), star)
            }
        }
    }
}
