package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Depth shelf: designs with a body to them, as the wallpapers that come with an iPhone or a
 * Galaxy have. Petals laid over one another, hills of colour one behind the next, satin, glass
 * marbles, panes of glass over colour, a twisted ribbon, two rings through each other, an eclipse.
 *
 * Each is lit as a thing in the world is, by a light above and to the left: light is added and
 * scaled as light, not as paint, and the colours run between the colour set's two accents round
 * the colour wheel the short way ([Spectrum]), so they are as vivid in the middle as at the ends.
 * They are worked out a pixel at a time on all the phone's cores ([pixels]), except the ribbon,
 * which is drawn.
 *
 * As with every design, everything is placed as a fraction of the canvas, so a thumbnail is the
 * same picture as the wallpaper, and nothing is left to chance, so a design is the same picture
 * every time it is painted. The light picture is drawn for a pale page, not washed out from the
 * dark one, and the lower part of both is kept quiet for the icons and the dock.
 */
internal object WallDepth {

    fun paint(style: WallStyle, easel: Easel) {
        val kit = DepthKit(easel.c)
        when (style) {
            WallStyle.Iris -> Iris(easel, kit).paint()
            WallStyle.Layers -> Layers(easel, kit).paint()
            WallStyle.Satin -> Satin(easel, kit).paint()
            WallStyle.Marbles -> Marbles(easel, kit).paint()
            WallStyle.Liquid -> Liquid(easel, kit).paint()
            WallStyle.Twist -> Twist(easel, kit).paint()
            WallStyle.Loop -> Loop(easel, kit).paint()
            WallStyle.Eclipse -> Eclipse(easel, kit).paint()
            else -> throw IllegalArgumentException("${style.name} is not on the Depth shelf")
        }
    }
}

private const val TAU = (2.0 * PI).toFloat()

/** Copies the colour at [from] in this pixel's numbers to [to]. */
private fun FloatArray.put(to: Int, from: Int) {
    this[to] = this[from]
    this[to + 1] = this[from + 1]
    this[to + 2] = this[from + 2]
}

/** Moves the colour at [into] towards the one at [from] by [t]. */
private fun FloatArray.blend(into: Int, from: Int, t: Float) {
    this[into] += (this[from] - this[into]) * t
    this[into + 1] += (this[from + 1] - this[into + 1]) * t
    this[into + 2] += (this[from + 2] - this[into + 2]) * t
}

/** Moves the colour at [into] towards colour [k] of [colours] (three numbers each) by [t]. */
private fun FloatArray.blendTo(into: Int, colours: FloatArray, k: Int, t: Float) {
    this[into] += (colours[k * 3] - this[into]) * t
    this[into + 1] += (colours[k * 3 + 1] - this[into + 1]) * t
    this[into + 2] += (colours[k * 3 + 2] - this[into + 2]) * t
}

/** How fast [Shade.smoothstep] climbs at [x]. */
private fun climb(edge0: Float, edge1: Float, x: Float): Float {
    val t = (x - edge0) / (edge1 - edge0)
    return if (t <= 0f || t >= 1f) 0f else 6f * t * (1f - t) / (edge1 - edge0)
}

// ---------------------------------------------------------------------------------------- Iris

/**
 * Nine petals turning out from a bright heart, each laid over the last and tucked under the next,
 * as the leaves of a camera's iris are. A petal leans: flat where it lies on top, diving where it
 * goes under, so the light runs across it and a shadow gathers where it tucks in. They take their
 * colours round the spectrum and back, so the last meets the first without a jump.
 */
private class Iris(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val cx = 0.5f * w
    private val cy = 0.42f * h
    private val radius = 0.80f * w
    private val twist = 2.4f
    private val perRadian = COUNT / TAU
    private val light = Shade.unit(-0.5f, -0.62f, 0.6f)
    private val half = Shade.half(light)
    private val lit = FloatArray(COUNT * 3)
    private val deep = FloatArray(COUNT * 3)
    private val reach = FloatArray(COUNT)
    private val glow = k.colour(0.5f, lightness = if (dark) 0.8f else 1f)
    private val glowStrength = if (dark) 0.12f else 0.1f
    private val heartStrength = if (dark) 0.55f else 0.6f
    private val tuck = if (dark) 0.6f else 0.28f
    private val tuckWidth = 0.03f * w
    private val shine = if (dark) 0.35f else 0.25f
    private val rimWidth = 0.004f * w
    private val rimStrength = if (dark) 0.18f else 0.12f

    init {
        for (p in 0 until COUNT) {
            val t = 0.5f - 0.5f * cos(TAU * (p + 0.5f) / COUNT)
            val a = if (dark) k.colour(t) else k.colour(t, chroma = 0.75f, atLightness = 0.86f)
            val b = if (dark) k.colour(t, lightness = 0.45f, chroma = 1.1f) else k.colour(t, chroma = 0.95f, atLightness = 0.70f)
            a.copyInto(lit, p * 3)
            b.copyInto(deep, p * 3)
            // Each petal reaches out its own way, and fades softly past it.
            reach[p] = 1f + 0.12f * sin(p * 2.3f + 0.7f)
        }
    }

    fun paint() = e.pixels { x, y, px ->
        val dx = x - cx
        val dy = y - cy
        val rp = sqrt(dx * dx + dy * dy) + 1e-3f
        val r = rp / radius
        // Which petal, and how far across it: the turn round the heart, bent further the further out.
        val p = (atan2(dy, dx) - twist * r) * perRadian
        val whole = floor(p)
        val s = p - whole
        val petal = Math.floorMod(whole.toInt(), COUNT)
        // How fast that changes across the screen, so the distances to a petal's edges come out in pixels.
        val inv = 1f / (rp * rp)
        val outward = twist / (rp * radius)
        val gx = (-dy * inv - dx * outward) * perRadian
        val gy = (dx * inv - dy * outward) * perRadian
        val g = sqrt(gx * gx + gy * gy) + 1e-9f
        val ux = gx / g
        val uy = gy / g
        val over = s / g
        val under = (1f - s) / g
        val rh = r / 0.22f
        val heart = exp(-rh * rh) * heartStrength
        val dim = 1f - 0.35f * Shade.smoothstep(0.2f, 1f, r)
        // The backdrop, with a little light behind the flower.
        k.sky(y / h, px, 3)
        val rg = r / 0.9f
        val gl = exp(-rg * rg) * glowStrength
        px[3] += glow[0] * gl
        px[4] += glow[1] * gl
        px[5] += glow[2] * gl
        colour(petal, s, over, under, ux, uy, r, heart, dim, px, 6)
        // Within half a pixel of an edge, the petal across it shares the pixel, so the edge is smooth.
        if (over < 0.5f) {
            colour((petal + COUNT - 1) % COUNT, 1f, Float.MAX_VALUE, 0f, ux, uy, r, heart, dim, px, 9)
            px.put(0, 9)
            px.blend(0, 6, over + 0.5f)
        } else if (under < 0.5f) {
            colour((petal + 1) % COUNT, 0f, 0f, Float.MAX_VALUE, ux, uy, r, heart, dim, px, 9)
            px.put(0, 6)
            px.blend(0, 9, 0.5f - under)
        } else {
            px.put(0, 6)
        }
    }

    /**
     * Petal [p]'s colour into [px] from [at]: [s] is how far across it the point is, 0 where it
     * lies over the last petal to 1 where it tucks under the next, and [over] and [under] are how
     * many pixels the point is from those two edges. The backdrop is in px[3..5].
     */
    private fun colour(p: Int, s: Float, over: Float, under: Float, ux: Float, uy: Float, r: Float, heart: Float, dim: Float, px: FloatArray, at: Int) {
        val tilt = 0.35f + 1.1f * s * sqrt(s)
        val tx = ux * tilt
        val ty = uy * tilt
        val nz = 1f / sqrt(tx * tx + ty * ty + 1f)
        val nx = tx * nz
        val ny = ty * nz
        val diffuse = Shade.clamp01(nx * light[0] + ny * light[1] + nz * light[2])
        val spec = Shade.pow64(nx * half[0] + ny * half[1] + nz * half[2]) * shine
        val f = 0.25f + 0.75f * diffuse
        val shadow = if (under < 12f * tuckWidth) 1f - tuck * exp(-under / tuckWidth) else 1f
        val rim = if (over < 30f * rimWidth) exp(-over / rimWidth) * rimStrength else 0f
        val fade = 1f - Shade.smoothstep(reach[p] - 0.45f, reach[p] + 0.25f, r)
        val j = p * 3
        for (c in 0..2) {
            var v = (deep[j + c] + (lit[j + c] - deep[j + c]) * f) * dim
            v += (1f - v) * heart
            v = v * shadow + spec + rim
            px[at + c] = px[3 + c] + (v - px[3 + c]) * fade
        }
    }

    private companion object {
        const val COUNT = 9
    }
}

// -------------------------------------------------------------------------------------- Layers

/**
 * Seven hills of colour, one behind the next: the far ones high up, pale and hazy at night, the
 * near ones low and deep, each with a soft sheen along its crest and a shadow thrown on the one
 * behind it. By day the near ones are the palest, so the icons sit on light.
 */
private class Layers(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val count = ROWS.size
    private val across = ceil(w).toInt().coerceAtLeast(1)

    /** Where each crest is in every column, and how much its slope there stretches a pixel. */
    private val crest = FloatArray(count * across)
    private val square = FloatArray(count * across)
    private val top = FloatArray(count * 3)
    private val low = FloatArray(count * 3)
    private val fall = 0.12f * h
    private val sheenWidth = 0.012f * h
    private val sheen = if (dark) 0.10f else 0.08f
    private val shadowWidth = 0.022f * h
    private val shadow = if (dark) 0.55f else 0.22f

    init {
        for (j in 0 until count) {
            val row = ROWS[j]
            for (i in 0 until across) {
                val u = (i + 0.5f) / w
                val a1 = TAU * row[2] * u + row[3]
                val a2 = TAU * row[5] * u + row[6]
                crest[j * across + i] = h * (row[0] + row[1] * sin(a1) + row[4] * sin(a2) + row[7] * (u - 0.5f))
                val slope = (row[1] * TAU * row[2] * cos(a1) + row[4] * TAU * row[5] * cos(a2) + row[7]) * h / w
                square[j * across + i] = 1f / sqrt(1f + slope * slope)
            }
            val t = j / (count - 1f)
            val hue = 0.5f - 0.5f * cos(PI.toFloat() * (0.15f + 0.85f * t))
            val a = if (dark) k.colour(hue, lightness = 1f - 0.38f * t) else k.colour(hue, chroma = 0.95f - 0.5f * t, atLightness = 0.80f + 0.12f * t)
            val b = if (dark) k.colour(hue, lightness = 0.55f - 0.25f * t, chroma = 1.05f) else k.colour(hue, chroma = 0.7f - 0.4f * t, atLightness = 0.88f + 0.08f * t)
            a.copyInto(top, j * 3)
            b.copyInto(low, j * 3)
        }
    }

    fun paint() = e.pixels { x, y, px ->
        val i = x.toInt().coerceIn(0, across - 1)
        if (dark) k.dusk(y / h, px, 0) else k.sky(y / h, px, 0)
        // The nearest hill that covers the pixel whole hides everything behind it.
        var start = -1
        var j = count - 1
        while (j >= 0) {
            if ((y - crest[j * across + i]) * square[j * across + i] >= 0.5f) {
                start = j
                break
            }
            j--
        }
        if (start >= 0) hill(start, (y - crest[start * across + i]) * square[start * across + i], px, 0)
        // Nearer ones only reach it with the half pixel of their edge, and their shadows.
        for (n in start + 1 until count) {
            val d = (y - crest[n * across + i]) * square[n * across + i]
            val gap = -d
            if (d > -0.5f) {
                val cover = d + 0.5f
                hill(n, d, px, 3)
                px.blend(0, 3, cover)
                if (gap > 0f) {
                    val keep = 1f - exp(-gap / shadowWidth) * shadow * (1f - cover)
                    px[0] *= keep
                    px[1] *= keep
                    px[2] *= keep
                }
            } else if (gap < 12f * shadowWidth) {
                val keep = 1f - exp(-gap / shadowWidth) * shadow
                px[0] *= keep
                px[1] *= keep
                px[2] *= keep
            }
        }
    }

    /** Hill [j]'s colour [d] pixels below its crest, into [px] from [at]. */
    private fun hill(j: Int, d: Float, px: FloatArray, at: Int) {
        val below = if (d > 0f) d else 0f
        val f = 1f - exp(-below / fall)
        val s = exp(-below / sheenWidth) * sheen
        val b = j * 3
        px[at] = top[b] + (low[b] - top[b]) * f + s
        px[at + 1] = top[b + 1] + (low[b + 1] - top[b + 1]) * f + s
        px[at + 2] = top[b + 2] + (low[b + 2] - top[b + 2]) * f + s
    }

    private companion object {
        /**
         * Each crest: its height down the screen, a long swell (size, how many across, where it
         * starts), a shorter one on it, and a lean. Drawn once at random and kept, so the hills are
         * the same every time.
         */
        val ROWS = arrayOf(
            floatArrayOf(0.160f, 0.0639f, 0.725f, 3.777f, 0.0183f, 1.718f, 5.829f, 0.100f),
            floatArrayOf(0.277f, 0.0621f, 0.595f, 5.956f, 0.0255f, 1.895f, 3.212f, -0.060f),
            floatArrayOf(0.393f, 0.0799f, 0.646f, 0.866f, 0.0275f, 2.136f, 3.218f, 0.100f),
            floatArrayOf(0.510f, 0.0845f, 0.742f, 6.160f, 0.0205f, 2.043f, 3.037f, -0.060f),
            floatArrayOf(0.627f, 0.0706f, 0.757f, 1.478f, 0.0276f, 2.294f, 0.809f, 0.100f),
            floatArrayOf(0.743f, 0.0740f, 0.647f, 0.522f, 0.0288f, 1.944f, 0.928f, -0.060f),
            floatArrayOf(0.860f, 0.0802f, 0.621f, 5.661f, 0.0206f, 1.626f, 1.261f, 0.100f),
        )
    }
}

// --------------------------------------------------------------------------------------- Satin

/**
 * Satin in long folds, each a wide hill and a narrow crease, bent by a slow sway: a broad sheen
 * and a sharp one where the light catches a fold, and a second, lower light from the other side
 * in the spectrum's other end. It has no hard edges, so it is worked out at half size and
 * stretched.
 */
private class Satin(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val tall = h / w
    private val light = Shade.unit(-0.55f, -0.7f, 0.45f)
    private val half = Shade.half(light)
    private val fillLight = Shade.unit(0.6f, 0.5f, 0.35f)
    private val body = if (dark) k.ramp(0.78f, 1.05f) else k.rampAt(0.84f, 0.8f)
    private val deep = if (dark) k.ramp(0.30f, 1.1f) else k.rampAt(0.62f, 0.95f)
    private val high = k.ramp(1.15f, 0.6f)
    private val fill = if (dark) k.ramp(0.8f, 1f) else k.rampAt(0.86f, 0.8f)
    private val fillStrength = if (dark) 0.25f else 0.12f
    private val broadStrength = if (dark) 0.35f else 0.22f
    private val sharpStrength = if (dark) 0.35f else 0.25f
    private val settle = if (dark) 0.75f else 0.55f

    fun paint() = e.pixels(step = 2) { x, y, px ->
        val u = x / w
        val v = y / h
        val q = v * tall
        // The sway the folds are bent by.
        val a1 = 1.7f * q + 0.6f
        val a2 = 3.9f * q + 2.4f * u + 1.9f
        val a3 = 2.7f * u + 2.2f
        val s1 = sin(a1)
        val c1 = cos(a1)
        val s2 = sin(a2)
        val c2 = cos(a2)
        val s3 = sin(a3)
        val c3 = cos(a3)
        val wu = u + 0.16f * s1 + 0.06f * s2
        val wv = q + 0.10f * s3
        val wuU = 1f + 0.144f * c2
        val wuQ = 0.272f * c1 + 0.234f * c2
        val wvU = 0.27f * c3
        // Three sets of folds, each with its crease, and how steep they are.
        val z1 = 17f * wu + 6f * wv + 0.4f
        val z2 = 9.5f * wu - 3f * wv + 1.7f
        val z3 = 29f * wu + 9f * wv + 2.9f
        val f1 = sin(z1) + 0.32f * sin(2f * z1 + 0.9f)
        val g1 = cos(z1) + 0.64f * cos(2f * z1 + 0.9f)
        val f2 = sin(z2) + 0.32f * sin(2f * z2 + 0.9f)
        val g2 = cos(z2) + 0.64f * cos(2f * z2 + 0.9f)
        val f3 = sin(z3) + 0.32f * sin(2f * z3 + 0.9f)
        val g3 = cos(z3) + 0.64f * cos(2f * z3 + 0.9f)
        val f = 0.55f * f1 + 0.35f * f2 + 0.10f * f3
        val fWu = 9.35f * g1 + 3.325f * g2 + 2.9f * g3
        val fWv = 3.3f * g1 - 1.05f * g2 + 0.9f * g3
        val fU = fWu * wuU + fWv * wvU
        val fQ = fWu * wuQ + fWv
        // Flatter towards the foot, and lower under the status bar.
        val sa = Shade.smoothstep(0.45f, 1f, v)
        val sb = Shade.smoothstep(0f, 0.12f, v)
        val env = (1f - 0.6f * sa) * (0.35f + 0.65f * sb)
        val envV = -0.6f * climb(0.45f, 1f, v) * (0.35f + 0.65f * sb) + (1f - 0.6f * sa) * 0.65f * climb(0f, 0.12f, v)
        val height = f * env
        val hx = env * fU
        val hy = env * fQ + f * envV / tall
        val tx = -hx * 0.05f
        val ty = -hy * 0.05f
        val nz = 1f / sqrt(tx * tx + ty * ty + 1f)
        val nx = tx * nz
        val ny = ty * nz
        val diffuse = Shade.clamp01(nx * light[0] + ny * light[1] + nz * light[2])
        val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
        val n2 = nh * nh
        val n4 = n2 * n2
        val n8 = n4 * n4
        val n16 = n8 * n8
        val n64 = n16 * n16 * n16 * n16
        val broad = n8 * n4
        val sharp = n64 * n16 * n8 * n2
        val fl = Shade.clamp01(nx * fillLight[0] + ny * fillLight[1] + nz * fillLight[2])
        val t = Shade.clamp01(0.15f + 0.275f * u + 0.48f * v + 0.08f * height)
        body.get(t, px, 3)
        deep.get(t, px, 6)
        fill.get(1f - t, px, 9)
        if (dark) high.get(t, px, 12) else {
            px[12] = 1f
            px[13] = 1f
            px[14] = 1f
        }
        val shade = diffuse.pow(1.4f)
        val fillAmount = fl * fl * fillStrength
        val b = broad * broadStrength
        val sh = sharp * sharpStrength
        for (c in 0..2) px[c] = px[6 + c] + (px[3 + c] - px[6 + c]) * shade + px[9 + c] * fillAmount + px[12 + c] * b + sh
        // Into the backdrop at the foot, and quiet under the clock.
        k.towardsBase(px, Shade.smoothstep(0.55f, 1.05f, v) * settle)
        k.towardsDeep(px, (1f - Shade.smoothstep(0f, 0.09f, v)) * 0.25f)
    }
}

// ------------------------------------------------------------------------------------- Marbles

/**
 * Glass marbles at different depths: the near ones sharp and glossy, the far ones soft and sunk
 * in haze, as a camera sees them. Each is lit from the top left and gathers the light on its far
 * side as glass does, its colour running across it from one accent to the other.
 */
private class Marbles(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val count = ORBS.size
    private val ox = FloatArray(count) { ORBS[it][0] * w }
    private val oy = FloatArray(count) { ORBS[it][1] * h }
    private val or = FloatArray(count) { ORBS[it][2] * w }
    private val depth = FloatArray(count) { ORBS[it][3] }

    /** How soft each one's edge is, in pixels: the far ones are out of focus. */
    private val blur = FloatArray(count) { 0.5f + depth[it] * 0.035f * w }
    private val reach = FloatArray(count) { (or[it] + 0.5f * blur[it] + 1f).let { r -> r * r } }
    private val sway = FloatArray(count) { 0.25f * sin(it * 1.7f) }
    private val light = Shade.unit(-0.45f, -0.65f, 0.62f)
    private val half = Shade.half(light)
    private val awayX: Float
    private val awayY: Float
    private val lit = if (dark) k.ramp(1.05f, 1f) else k.rampAt(0.88f, 0.8f)
    private val deep = if (dark) k.ramp(0.32f, 1.1f) else k.rampAt(0.62f, 1f)
    private val rim = if (dark) k.ramp(1.1f, 0.9f) else k.rampAt(0.9f, 0.7f)
    private val glow = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.7f, atLightness = 0.9f)
    private val glowStrength = if (dark) 0.10f else 0.30f
    private val caustic = if (dark) 0.45f else 0.3f
    private val rimStrength = if (dark) 0.55f else 0.35f
    private val gloss = if (dark) 0.8f else 0.6f
    private val haze = if (dark) 0.55f else 0.5f

    init {
        val l = sqrt(light[0] * light[0] + light[1] * light[1])
        awayX = -light[0] / l
        awayY = -light[1] / l
    }

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        k.sky(v, px, 0)
        // The space behind them lit a little, in the middle of the spectrum.
        val gx = (x - 0.45f * w) / w
        val gy = (y - 0.38f * h) / w
        val g = exp(-(gx * gx + gy * gy) / 0.49f) * glowStrength
        px[0] += (glow[0] - px[0]) * g
        px[1] += (glow[1] - px[1]) * g
        px[2] += (glow[2] - px[2]) * g
        // Far ones first, so the near ones lie over them.
        for (i in 0 until count) {
            val ddx = x - ox[i]
            val ddy = y - oy[i]
            if (ddx * ddx + ddy * ddy > reach[i]) continue
            val r = or[i]
            val dx = ddx / r
            val dy = ddy / r
            val d = sqrt(dx * dx + dy * dy)
            val cover = Shade.clamp01((r - d * r) / blur[i] + 0.5f)
            if (cover <= 0f) continue
            val inside = min(d, 1f)
            val nz = sqrt(max(0f, 1f - inside * inside))
            val sc = if (d > 1e-6f) inside / d else 0f
            val nx = dx * sc
            val ny = dy * sc
            val wrap = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + nz * light[2])
            val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
            val n2 = nh * nh
            val n4 = n2 * n2
            val n8 = n4 * n4
            val n16 = n8 * n8
            val n32 = n16 * n16
            val soft = n8 * n4
            val spec = n32 * n8 * gloss * (1f - 0.7f * depth[i]) + soft * 0.12f
            val fresnel = (1f - nz).let { it * it * it } * rimStrength
            val t = Shade.clamp01(0.5f + 0.45f * (nx * awayX + ny * awayY) + sway[i])
            lit.get(t, px, 6)
            deep.get(t, px, 9)
            rim.get(1f - t, px, 12)
            val shade = wrap * sqrt(wrap)
            // Light gathered on the far side, as in a glass marble.
            val cx = dx - 0.42f
            val cy = dy - 0.52f
            val gather = exp(-(cx * cx + cy * cy) / 0.09f) * caustic
            val sink = depth[i] * haze
            for (c in 0..2) {
                var col = px[9 + c] + (px[6 + c] - px[9 + c]) * shade
                col += px[6 + c] * gather + px[12 + c] * fresnel + spec
                col += (px[c] - col) * sink
                px[c] += (col - px[c]) * cover
            }
        }
        k.towardsBase(px, Shade.smoothstep(0.6f, 1f, v) * 0.35f)
    }

    private companion object {
        /** Across, down, size (of the width), and how far away, 0 near to 1 far; the far ones first. */
        val ORBS = arrayOf(
            floatArrayOf(0.86f, 0.16f, 0.10f, 0.95f),
            floatArrayOf(0.18f, 0.62f, 0.07f, 0.90f),
            floatArrayOf(0.62f, 0.84f, 0.08f, 0.85f),
            floatArrayOf(0.10f, 0.20f, 0.17f, 0.55f),
            floatArrayOf(0.80f, 0.56f, 0.20f, 0.45f),
            floatArrayOf(0.40f, 0.36f, 0.30f, 0.00f),
            floatArrayOf(0.30f, 0.76f, 0.12f, 0.25f),
        )
    }
}

// -------------------------------------------------------------------------------------- Liquid

/**
 * Capsules of clear glass over pools of colour, as Liquid Glass is: through the middle of each the
 * colour behind shows as it is, a little lifted; towards its rim the glass bends it, pulling in
 * what lies beyond, red a touch more than blue, and a hairline of light runs round the edge that
 * faces the light. Wide bands lie across the pools the other way, so the bending can be seen.
 */
private class Liquid(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val tall = h / w
    private val turn = Math.toRadians(-32.0)
    private val cosA = cos(turn).toFloat()
    private val sinA = sin(turn).toFloat()
    private val paneX = FloatArray(PANES.size) { PANES[it][0] * w }
    private val paneY = FloatArray(PANES.size) { PANES[it][1] * h }
    private val paneHalf = FloatArray(PANES.size) { PANES[it][2] * w }
    private val paneRadius = FloatArray(PANES.size) { PANES[it][3] * w }
    private val poolColour = FloatArray(POOLS.size * 3)
    private val poolStrength = if (dark) 0.95f else 0.75f
    private val bandColour = FloatArray(BANDS * 3)
    private val bandStrength = if (dark) 0.14f else 0.18f
    private val shadowWidth = 0.03f * w
    private val shadowDrop = 0.012f * w
    private val shadow = if (dark) 0.35f else 0.14f
    private val edgeWidth = 0.006f * w
    private val lift = if (dark) 1.08f else 1f
    private val lighten = if (dark) 0.04f else 0.05f
    private val edgeLight = if (dark) 0.9f else 0.8f

    init {
        for (p in POOLS.indices) {
            val t = POOLS[p][2]
            val c = if (dark) k.colour(t, lightness = 0.85f, chroma = 1.05f) else k.colour(t, chroma = 0.9f, atLightness = 0.80f)
            c.copyInto(poolColour, p * 3)
        }
        val bands = if (dark) k.ramp(1f, 1f) else k.rampAt(0.86f, 0.8f)
        for (b in 0 until BANDS) bands.get(Shade.clamp01(0.5f + 0.5f * sin((b - BANDS / 2) * 2.1f)), bandColour, b * 3)
    }

    fun paint() = e.pixels { x, y, px ->
        backdrop(x, y, px, 3)
        px.put(0, 3)
        // The panes' shadows, a little below them.
        for (p in PANES.indices) {
            val d = pane(p, x, y - shadowDrop, px, 12)
            if (d > 12f * shadowWidth) continue
            val keep = 1f - exp(-max(d, 0f) / shadowWidth) * shadow
            px[0] *= keep
            px[1] *= keep
            px[2] *= keep
        }
        for (p in PANES.indices) {
            val d = pane(p, x, y, px, 12)
            val cover = Shade.clamp01(0.5f - d)
            if (cover <= 0f) continue
            val gx = px[12]
            val gy = px[13]
            val depth = if (d < 0f) -d else 0f
            // Through the glass: near the rim, the backdrop pulled in from beyond it.
            val bend = 1f - Shade.smoothstep(0f, paneRadius[p] * 0.55f, depth)
            val off = bend * bend * paneRadius[p] * 0.9f
            if (off > 0.05f) {
                backdrop(x + gx * off * 1.06f, y + gy * off * 1.06f, px, 15)
                px[6] = px[15]
                backdrop(x + gx * off, y + gy * off, px, 15)
                px[7] = px[16]
                backdrop(x + gx * off * 0.94f, y + gy * off * 0.94f, px, 15)
                px[8] = px[17]
            } else {
                px.put(6, 3)
            }
            // A hairline of light where the edge faces the light, a fainter one opposite.
            val facing = gx * -0.55f + gy * -0.83f
            val edge = if (depth < 20f * edgeWidth) exp(-depth / edgeWidth) else 0f
            val near = Shade.clamp01(facing)
            val far = Shade.clamp01(-facing)
            val hl = edge * (near * near * 0.9f + far * far * 0.35f) * edgeLight
            for (c in 6..8) px[c] = px[c] * lift + lighten + hl
            px.blend(0, 6, cover)
        }
        k.towardsBase(px, Shade.smoothstep(0.58f, 1f, y / h) * (if (dark) 0.5f else 0.35f))
    }

    /** What the glass lies over at a point, into [px] from [at]: pools of colour running into each other, and the bands. */
    private fun backdrop(x: Float, y: Float, px: FloatArray, at: Int) {
        val u = x / w
        val v = y / h
        k.sky(v, px, at)
        for (p in POOLS.indices) {
            val du = u - POOLS[p][0]
            val dv = (v - POOLS[p][1]) * tall
            val rr = POOLS[p][3]
            val dd = (du * du + dv * dv) / (rr * rr)
            if (dd > 12f) continue
            px.blendTo(at, poolColour, p, exp(-dd * 1.6f) * poolStrength)
        }
        val band = (u * cosA + v * tall * sinA) * 2.4f + 0.3f
        val whole = floor(band)
        val f = band - whole
        val edge = Shade.smoothstep(0f, 0.12f, f) * (1f - Shade.smoothstep(0.38f, 0.5f, f))
        if (edge > 0f) px.blendTo(at, bandColour, (whole.toInt() + BANDS / 2).coerceIn(0, BANDS - 1), edge * bandStrength)
    }

    /**
     * How far a point is outside pane [p], in pixels, less than nothing inside it; and the way out
     * of the glass from the point, into px[at] and px[at + 1].
     */
    private fun pane(p: Int, x: Float, y: Float, px: FloatArray, at: Int): Float {
        val ox = x - paneX[p]
        val oy = y - paneY[p]
        val lx = ox * cosA + oy * sinA
        val ly = -ox * sinA + oy * cosA
        val ax = abs(lx)
        val qx = if (ax > paneHalf[p]) ax - paneHalf[p] else 0f
        val len = sqrt(qx * qx + ly * ly)
        val gl = 1f / (len + 1e-6f)
        val glx = (if (lx < 0f) -qx else qx) * gl
        val gly = ly * gl
        px[at] = glx * cosA - gly * sinA
        px[at + 1] = glx * sinA + gly * cosA
        return len - paneRadius[p]
    }

    private companion object {
        /** Each pane: its middle, across and down; half its length and its radius, of the width. All lie at the same slant. */
        val PANES = arrayOf(
            floatArrayOf(0.30f, 0.22f, 0.30f, 0.13f),
            floatArrayOf(0.74f, 0.40f, 0.26f, 0.11f),
            floatArrayOf(0.38f, 0.60f, 0.34f, 0.12f),
            floatArrayOf(0.82f, 0.78f, 0.22f, 0.09f),
        )

        /** Each pool: its middle, across and down, where on the spectrum, and how wide, of the width. */
        val POOLS = arrayOf(
            floatArrayOf(0.15f, 0.18f, 0.0f, 0.55f),
            floatArrayOf(0.90f, 0.35f, 0.35f, 0.5f),
            floatArrayOf(0.25f, 0.62f, 0.7f, 0.5f),
            floatArrayOf(0.85f, 0.85f, 1.0f, 0.45f),
        )

        /** Enough bands for any glass to look through to, however far it pulls. */
        const val BANDS = 32
    }
}

// --------------------------------------------------------------------------------------- Twist

/**
 * A wide ribbon falling through the screen and turning over once as it goes, so it shows one face
 * and then the other, in the spectrum's two directions, and catches the light where it folds. It
 * is drawn as a fine mesh, nearest last, each piece shaded across its width, over a soft shadow
 * of the whole.
 */
private class Twist(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val light = Shade.unit(-0.45f, -0.7f, 0.55f)
    private val half = Shade.half(light)
    private val stride = ACROSS + 1

    /** The mesh's points on the screen, with how near each is, and its colour as the screen shows it. */
    private val sx = FloatArray((ALONG + 1) * stride)
    private val sy = FloatArray((ALONG + 1) * stride)
    private val sz = FloatArray((ALONG + 1) * stride)
    private val colour = IntArray((ALONG + 1) * stride)

    fun paint() {
        backdrop()
        shape()
        shadow()
        ribbon()
        settle()
    }

    private fun backdrop() {
        val glow = k.colour(0.5f, lightness = if (dark) 0.9f else 1f)
        val strength = if (dark) 0.12f else 0.08f
        e.pixels(step = 4) { x, y, px ->
            k.sky(y / h, px, 0)
            val gx = (x - 0.5f * w) / w
            val gy = (y - 0.45f * h) / w
            val g = exp(-(gx * gx + gy * gy) / 0.64f) * strength
            px[0] += glow[0] * g
            px[1] += glow[1] * g
            px[2] += glow[2] * g
        }
    }

    /** Where the ribbon runs, how it turns, and how each point of it is lit. */
    private fun shape() {
        val m = ALONG
        val cx = FloatArray(m + 1)
        val cy = FloatArray(m + 1)
        val cz = FloatArray(m + 1)
        for (i in 0..m) {
            val t = i / m.toFloat()
            cx[i] = w * (0.50f + 0.30f * sin(TAU * 0.7f * t + 0.5f))
            cy[i] = h * (-0.12f + 1.24f * t) + h * 0.05f * sin(TAU * 1.1f * t + 0.6f)
            cz[i] = w * 0.5f * sin(TAU * 0.6f * t + 0.9f)
        }
        // The way across the ribbon at each point along it, turning steadily round the way it runs.
        val ax = FloatArray(m + 1)
        val ay = FloatArray(m + 1)
        val az = FloatArray(m + 1)
        for (i in 0..m) {
            val a = max(i - 1, 0)
            val b = min(i + 1, m)
            var tx = cx[b] - cx[a]
            var ty = cy[b] - cy[a]
            var tz = cz[b] - cz[a]
            val tl = sqrt(tx * tx + ty * ty + tz * tz)
            tx /= tl
            ty /= tl
            tz /= tl
            val nl = sqrt(tx * tx + ty * ty)
            val nx = ty / nl
            val ny = -tx / nl
            val bx = -tz * ny
            val by = tz * nx
            val bz = tx * ny - ty * nx
            val t = i / m.toFloat()
            val theta = TAU * t + 0.25f
            val c = cos(theta)
            val s = sin(theta)
            ax[i] = c * nx + s * bx
            ay[i] = c * ny + s * by
            az[i] = s * bz
            val hw = 0.36f * w * (0.85f + 0.15f * sin(TAU * 1.3f * t + 0.2f))
            for (j in 0..ACROSS) {
                val sj = (-1f + 2f * j / ACROSS) * hw
                val p = i * stride + j
                sx[p] = cx[i] + sj * ax[i]
                sy[p] = cy[i] + sj * ay[i]
                sz[p] = cz[i] + sj * az[i]
            }
        }
        val frontLit = if (dark) k.ramp(1.05f) else k.rampAt(0.92f, 0.75f)
        val frontDeep = if (dark) k.ramp(0.30f, 1.1f) else k.rampAt(0.66f, 1f)
        val backLit = if (dark) k.ramp(0.8f, 1.1f) else k.rampAt(0.86f, 0.85f)
        val backDeep = if (dark) k.ramp(0.22f, 1.1f) else k.rampAt(0.6f, 1f)
        val specStrength = if (dark) 0.8f else 0.6f
        val sheen = if (dark) 0.14f else 0.09f
        val rimStrength = if (dark) 0.15f else 0.1f
        val lit = FloatArray(3)
        val deep = FloatArray(3)
        for (i in 0..m) {
            val t = i / m.toFloat()
            for (j in 0..ACROSS) {
                val p = i * stride + j
                val from = if (i < m) p else p - stride
                val dx = sx[from + stride] - sx[from]
                val dy = sy[from + stride] - sy[from]
                val dz = sz[from + stride] - sz[from]
                var nx = dy * az[i] - dz * ay[i]
                var ny = dz * ax[i] - dx * az[i]
                var nz = dx * ay[i] - dy * ax[i]
                val nl = sqrt(nx * nx + ny * ny + nz * nz) + 1e-9f
                nx /= nl
                ny /= nl
                nz /= nl
                // Its back is the spectrum the other way round, a shade deeper.
                val back = nz < 0f
                if (back) {
                    nx = -nx
                    ny = -ny
                    nz = -nz
                    backLit.get(1f - t, lit)
                    backDeep.get(1f - t, deep)
                } else {
                    frontLit.get(t, lit)
                    frontDeep.get(t, deep)
                }
                val wrap = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + nz * light[2])
                val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
                val n2 = nh * nh
                val n4 = n2 * n2
                val n8 = n4 * n4
                val n32 = n8 * n8 * n8 * n8
                val shade = wrap.pow(1.6f)
                val glint = n32 * n8 * specStrength + n4 * n2 * sheen
                val rim = (1f - nz) * (1f - nz) * rimStrength
                colour[p] = Color.rgb(
                    Oklab.toChannel(deep[0] + (lit[0] - deep[0]) * shade + glint + rim * lit[0]),
                    Oklab.toChannel(deep[1] + (lit[1] - deep[1]) * shade + glint + rim * lit[1]),
                    Oklab.toChannel(deep[2] + (lit[2] - deep[2]) * shade + glint + rim * lit[2]),
                )
            }
        }
    }

    /** The ribbon's shadow on the backdrop: the whole of it, drawn small, blurred, and laid down a little below and to the right. */
    private fun shadow() {
        val scale = 8f
        val across = ceil(w / scale).toInt() + 2
        val down = ceil(h / scale).toInt() + 2
        val mask = Bitmap.createBitmap(across, down, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        canvas.scale(1f / scale, 1f / scale)
        // Hard-edged, so the pieces fill the mask solidly between them: the blur softens it after.
        val paint = Paint().apply { color = Color.WHITE }
        val path = Path()
        for (i in 0 until ALONG) for (j in 0 until ACROSS) {
            val a = i * stride + j
            path.rewind()
            path.moveTo(sx[a], sy[a])
            path.lineTo(sx[a + stride], sy[a + stride])
            path.lineTo(sx[a + stride + 1], sy[a + stride + 1])
            path.lineTo(sx[a + 1], sy[a + 1])
            path.close()
            canvas.drawPath(path, paint)
        }
        val pixels = IntArray(across * down)
        mask.getPixels(pixels, 0, across, 0, 0, across, down)
        mask.recycle()
        val alpha = FloatArray(pixels.size) { (pixels[it] ushr 24) / 255f }
        blur(alpha, across, down, 0.04f * w / scale)
        // Light is darkened as light: what takes away a share of the light takes away less of the screen's value.
        val strength = if (dark) 0.45f else 0.18f
        for (i in pixels.indices) {
            val a = 1f - (1f - alpha[i] * strength).pow(1f / 2.2f)
            pixels[i] = (a * 255f + 0.5f).toInt().coerceIn(0, 255) shl 24
        }
        val shade = Bitmap.createBitmap(pixels, across, down, Bitmap.Config.ARGB_8888)
        val left = 0.015f * w
        val top = 0.03f * w
        e.canvas.drawBitmap(shade, Rect(0, 0, across, down), RectF(left, top, left + across * scale, top + down * scale), Paint(Paint.FILTER_BITMAP_FLAG))
        shade.recycle()
    }

    /** Every piece of the mesh, furthest first, each shaded across its width, and each reaching a little into its neighbours so no seam shows. */
    private fun ribbon() {
        val order = LongArray(ALONG * ACROSS)
        for (i in 0 until ALONG) for (j in 0 until ACROSS) {
            val a = i * stride + j
            val z = (sz[a] + sz[a + 1] + sz[a + stride] + sz[a + stride + 1]) / 4f
            val bits = java.lang.Float.floatToIntBits(z)
            val key = bits xor ((bits shr 31) and 0x7fffffff)
            order[i * ACROSS + j] = (key.toLong() shl 32) or (i * ACROSS + j).toLong()
        }
        order.sort()
        val path = Path()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (key in order) {
            val q = (key and 0xffffffffL).toInt()
            val i = q / ACROSS
            val j = q % ACROSS
            val a = i * stride + j
            val b = a + stride
            val c = b + 1
            val d = a + 1
            // A little past its far end, and past its far side unless that is the ribbon's edge.
            val (ex, ey) = reach(sx[b] - sx[a], sy[b] - sy[a])
            val (fx, fy) = if (j < ACROSS - 1) reach(sx[d] - sx[a], sy[d] - sy[a]) else Pair(0f, 0f)
            path.rewind()
            path.moveTo(sx[a], sy[a])
            path.lineTo(sx[b] + ex, sy[b] + ey)
            path.lineTo(sx[c] + ex + fx, sy[c] + ey + fy)
            path.lineTo(sx[d] + fx, sy[d] + fy)
            path.close()
            val x0 = (sx[a] + sx[b]) / 2f
            val y0 = (sy[a] + sy[b]) / 2f
            val x1 = (sx[d] + sx[c]) / 2f
            val y1 = (sy[d] + sy[c]) / 2f
            if ((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) > 0.0625f) {
                paint.shader = LinearGradient(x0, y0, x1, y1, colour[a], colour[d], Shader.TileMode.CLAMP)
            } else {
                paint.shader = null
                paint.color = colour[a]
            }
            e.canvas.drawPath(path, paint)
        }
    }

    /** Eight tenths of a pixel the way ([x], [y]) points. */
    private fun reach(x: Float, y: Float): Pair<Float, Float> {
        val l = sqrt(x * x + y * y)
        return if (l < 1e-3f) Pair(0f, 0f) else Pair(x / l * 0.8f, y / l * 0.8f)
    }

    /** The lower part eased into the backdrop's foot, for the icons and the dock. */
    private fun settle() {
        val strength = if (dark) 0.35f else 0.3f
        val stops = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
        val colours = IntArray(stops.size) { alpha(e.c.base, (Shade.smoothstep(0f, 1f, stops[it]) * strength * 255f).toInt()) }
        val from = 0.65f * h
        e.canvas.drawRect(0f, from, w, h, Paint().apply { shader = LinearGradient(0f, from, 0f, h, colours, stops, Shader.TileMode.CLAMP) })
    }

    private companion object {
        const val ALONG = 500
        const val ACROSS = 20

        /** A soft blur of [values], [across] wide, by three passes of a box each way: near enough a Gaussian of [sigma]. */
        fun blur(values: FloatArray, across: Int, down: Int, sigma: Float) {
            val r = ((sqrt(12f * sigma * sigma / 3f + 1f) - 1f) / 2f).roundToInt().coerceAtLeast(1)
            val line = FloatArray(max(across, down))
            repeat(3) {
                for (y in 0 until down) box(values, y * across, 1, across, r, line)
                for (x in 0 until across) box(values, x, across, down, r, line)
            }
        }

        private fun box(values: FloatArray, start: Int, step: Int, n: Int, r: Int, line: FloatArray) {
            for (i in 0 until n) line[i] = values[start + i * step]
            var sum = 0f
            for (i in -r..r) sum += line[i.coerceIn(0, n - 1)]
            val inv = 1f / (2 * r + 1)
            for (i in 0 until n) {
                values[start + i * step] = sum * inv
                sum += line[(i + r + 1).coerceAtMost(n - 1)] - line[(i - r).coerceAtLeast(0)]
            }
        }
    }
}

// ---------------------------------------------------------------------------------------- Loop

/**
 * Two glossy rings, one through the other: each leans, so one side of it is nearer than the
 * other, and where they cross, the nearer passes over and throws a little shadow on the other.
 */
private class Loop(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val cx = FloatArray(2) { RINGS[it][0] * w }
    private val cy = FloatArray(2) { RINGS[it][1] * h }
    private val radius = FloatArray(2) { RINGS[it][2] * w }
    private val tube = FloatArray(2) { RINGS[it][3] * w }
    private val place = FloatArray(2) { RINGS[it][4] }
    private val swing = FloatArray(2) { RINGS[it][5] }
    private val turn = FloatArray(2) { RINGS[it][6] }
    private val light = Shade.unit(-0.45f, -0.7f, 0.55f)
    private val half = Shade.half(light)
    private val lit = if (dark) k.ramp(1.05f) else k.rampAt(0.9f, 0.8f)
    private val deep = if (dark) k.ramp(0.28f, 1.1f) else k.rampAt(0.62f, 1f)
    private val rim = if (dark) k.ramp(1.05f, 0.9f) else k.rampAt(0.9f, 0.7f)
    private val glow = k.colour(0.5f, lightness = if (dark) 0.9f else 1f)
    private val glowStrength = if (dark) 0.12f else 0.08f
    private val rimStrength = if (dark) 0.45f else 0.3f
    private val gloss = if (dark) 0.85f else 0.6f
    private val dropWidth = 0.03f * w
    private val drop = if (dark) 0.35f else 0.15f
    private val contactWidth = 0.012f * w
    private val contact = if (dark) 0.5f else 0.25f

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        k.sky(v, px, 0)
        val gx = (x - 0.5f * w) / w
        val gy = (y - 0.42f * h) / w
        val g = exp(-(gx * gx + gy * gy) / 0.5625f) * glowStrength
        px[0] += glow[0] * g
        px[1] += glow[1] * g
        px[2] += glow[2] * g
        val dx0 = x - cx[0]
        val dy0 = y - cy[0]
        val r0 = sqrt(dx0 * dx0 + dy0 * dy0) + 1e-3f
        val d0 = r0 - radius[0]
        val dx1 = x - cx[1]
        val dy1 = y - cy[1]
        val r1 = sqrt(dx1 * dx1 + dy1 * dy1) + 1e-3f
        val d1 = r1 - radius[1]
        // How far outside each ring's body the point is.
        val out0 = abs(d0) - tube[0]
        val out1 = abs(d1) - tube[1]
        // Their soft shadows on the backdrop.
        var fall = 0f
        if (out0 < 12f * dropWidth) fall = exp(-max(out0, 0f) / dropWidth) * 0.8f
        if (out1 < 12f * dropWidth) fall = max(fall, exp(-max(out1, 0f) / dropWidth) * 0.8f)
        val keep = 1f - fall * drop
        px[0] *= keep
        px[1] *= keep
        px[2] *= keep
        val cover0 = Shade.clamp01(0.5f - out0)
        val cover1 = Shade.clamp01(0.5f - out1)
        if (cover0 > 0f && cover1 > 0f) {
            val z0 = ring(0, dx0, dy0, r0, d0, px, 3)
            val z1 = ring(1, dx1, dy1, r1, d1, px, 6)
            if (z0 >= z1) {
                shade(px, 6, out0)
                px.blend(0, 6, cover1)
                px.blend(0, 3, cover0)
            } else {
                shade(px, 3, out1)
                px.blend(0, 3, cover0)
                px.blend(0, 6, cover1)
            }
        } else if (cover0 > 0f) {
            val z0 = ring(0, dx0, dy0, r0, d0, px, 3)
            if (out1 < 8f * contactWidth && depth(1, dx1, dy1, d1) > z0) shade(px, 3, out1)
            px.blend(0, 3, cover0)
        } else if (cover1 > 0f) {
            val z1 = ring(1, dx1, dy1, r1, d1, px, 6)
            if (out0 < 8f * contactWidth && depth(0, dx0, dy0, d0) > z1) shade(px, 6, out0)
            px.blend(0, 6, cover1)
        }
        k.towardsBase(px, Shade.smoothstep(0.65f, 1f, v) * (if (dark) 0.3f else 0.25f))
    }

    /** Darkens the ring colour at [at] where the other ring, [out] pixels away, passes over it. */
    private fun shade(px: FloatArray, at: Int, out: Float) {
        val s = 1f - exp(-max(out, 0f) / contactWidth) * contact
        px[at] *= s
        px[at + 1] *= s
        px[at + 2] *= s
    }

    /** How near ring [i] is at this point. */
    private fun depth(i: Int, dx: Float, dy: Float, d: Float): Float {
        val q = (d / tube[i]).coerceIn(-1f, 1f)
        val nz = sqrt(max(0f, 1f - q * q))
        return swing[i] * tube[i] * 2.2f * sin(atan2(dy, dx) - turn[i]) + nz * tube[i]
    }

    /** Ring [i]'s colour at this point, into [px] from [at]; it answers how near the ring is there. */
    private fun ring(i: Int, dx: Float, dy: Float, r: Float, d: Float, px: FloatArray, at: Int): Float {
        val q = (d / tube[i]).coerceIn(-1f, 1f)
        val nz = sqrt(max(0f, 1f - q * q))
        val nx = q * dx / r
        val ny = q * dy / r
        val phi = atan2(dy, dx)
        val wrap = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + nz * light[2])
        val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
        val n8 = Shade.pow8(nh)
        val spec = Shade.pow64(nh) * gloss + n8 * 0.06f
        val fresnel = (1f - nz).let { it * it * it } * rimStrength
        val t = Shade.clamp01(0.5f + 0.4f * sin(phi + 2f * place[i]) + (place[i] - 0.5f) * 0.4f)
        lit.get(t, px, 12)
        deep.get(t, px, 15)
        rim.get(1f - t, px, 18)
        val shade = wrap.pow(1.6f)
        for (c in 0..2) px[at + c] = px[15 + c] + (px[12 + c] - px[15 + c]) * shade + px[18 + c] * fresnel + spec
        return swing[i] * tube[i] * 2.2f * sin(phi - turn[i]) + nz * tube[i]
    }

    private companion object {
        /** Each ring: its middle, across and down; its radius and its thickness, of the width; where on the spectrum; how far it leans, and which way. */
        val RINGS = arrayOf(
            floatArrayOf(0.40f, 0.33f, 0.30f, 0.075f, 0.0f, 1.0f, 0.6f),
            floatArrayOf(0.66f, 0.50f, 0.26f, 0.065f, 1.0f, 1.0f, 2.4f),
        )
    }
}

// ------------------------------------------------------------------------------------- Eclipse

/**
 * A sphere with the light behind it, a little up and to the right: at night a dark body ringed by
 * a corona that spills into the sky, by day a pearl in a coloured halo. The light catches only
 * the rim on its own side, as it does round the moon in an eclipse.
 */
private class Eclipse(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val cx = 0.5f * w
    private val cy = 0.36f * h
    private val radius = 0.36f * w
    private val lx = 0.55f
    private val ly = -0.83f
    private val glow = if (dark) k.ramp(1f, 1.05f) else k.rampAt(0.72f, 1.1f)
    private val hot = if (dark) k.ramp(1.25f, 0.6f) else k.rampAt(0.95f, 0.5f)
    private val body = k.ramp(0.22f, 0.9f)
    private val pearl = k.rampAt(0.97f, 0.18f)
    private val tint = k.rampAt(0.84f, 0.75f)
    private val night = k.deepLight
    private val edgeWidth = 0.006f * w

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        val dx = x - cx
        val dy = y - cy
        val r = sqrt(dx * dx + dy * dy) + 1e-3f
        val side = Shade.clamp01(0.5f + 0.5f * (dx * lx + dy * ly) / r)
        val t = Shade.clamp01(0.5f + 0.5f * sin(atan2(dy, dx) + 0.6f))
        k.sky(v, px, 0)
        glow.get(t, px, 3)
        hot.get(t, px, 6)
        // Light spilling round the edge: strongest on the lit side, a long fall into the sky.
        val beyond = max(r - radius, 0f) / radius
        val corona = (exp(-beyond / 0.10f) * 0.9f + exp(-beyond / 0.45f) * 0.45f) * (0.25f + 0.75f * side * side)
        if (dark) {
            px[0] += px[3] * corona
            px[1] += px[4] * corona
            px[2] += px[5] * corona
        } else {
            px.blend(0, 3, Shade.clamp01(corona * 0.85f))
        }
        if (r < radius + 1f) {
            val q = Shade.clamp01(r / radius)
            val nz = sqrt(max(0f, 1f - q * q))
            val facing = (q * dx / r) * lx + (q * dy / r) * ly
            val rimLight = Shade.clamp01(facing).let { it * sqrt(it) } * (1f - nz).pow(2.2f) * (if (dark) 2.2f else 0.6f)
            val ty = Shade.clamp01(0.5f + 0.45f * dy / radius)
            if (dark) {
                body.get(ty, px, 9)
                val dim = 0.55f + 0.45f * (1f - nz)
                for (c in 0..2) px[9 + c] = (night[c] * 0.6f + px[9 + c] * 0.4f) * dim
            } else {
                // By day a pearl: pale, lit softly from the light's side, its colour gathered towards the rim.
                pearl.get(ty, px, 9)
                tint.get(ty, px, 12)
                val litSide = Shade.clamp01(0.5f + 0.5f * facing)
                val f = 0.35f + 0.65f * nz.pow(0.6f) * (0.6f + 0.4f * litSide)
                for (c in 0..2) px[9 + c] = px[12 + c] + (px[9 + c] - px[12 + c]) * f
            }
            for (c in 0..2) px[9 + c] += px[6 + c] * rimLight
            px.blend(0, 9, Shade.clamp01(radius - r + 0.5f))
        }
        // The thin bright edge on the lit side.
        val off = abs(r - radius)
        if (off < 20f * edgeWidth) {
            val edge = exp(-off / edgeWidth) * (0.2f + 0.8f * side * side * side) * (if (dark) 0.9f else 0.35f)
            px[0] += px[6] * edge
            px[1] += px[7] * edge
            px[2] += px[8] * edge
        }
        k.towardsBase(px, Shade.smoothstep(0.6f, 1f, v) * 0.3f)
    }
}
