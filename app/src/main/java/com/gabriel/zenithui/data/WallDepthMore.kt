package com.gabriel.zenithui.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * More of the Depth shelf (see [WallDepth]): reeded glass over colour, a glass ball, glossy arcs,
 * a pond's rings, dew on a window, a fan of tinted glass, and for Plus a stack of frosted cards and
 * a cut gem. Lit as the others are, by a light above and to the left, in the colour set's spectrum,
 * a pixel at a time on all the phone's cores, and kept quiet where the clock and the icons sit.
 */
internal object WallDepthMore {

    fun paint(style: WallStyle, easel: Easel) {
        val kit = DepthKit(easel.c)
        when (style) {
            WallStyle.Fluted -> Fluted(easel, kit).paint()
            WallStyle.Orb -> Orb(easel, kit).paint()
            WallStyle.Arcs -> Arcs(easel, kit).paint()
            WallStyle.Pond -> Pond(easel, kit).paint()
            WallStyle.Dew -> Dew(easel, kit).paint()
            WallStyle.Fan -> Fan(easel, kit).paint()
            WallStyle.Stack -> Stack(easel, kit).paint()
            WallStyle.Gem -> Gem(easel, kit).paint()
            else -> throw IllegalArgumentException("${style.name} is not painted here")
        }
    }
}

private const val TAU = (2.0 * PI).toFloat()

/** A number from 0 to 1 that is the same for the same two numbers, every time. */
private fun hash(a: Int, b: Int): Float {
    var x = a * 374761393 + b * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFFFF) / 16777216f
}

/** Moves the colour at [at] towards [colour] by [t]. */
private fun FloatArray.towards(at: Int, colour: FloatArray, t: Float) {
    this[at] += (colour[0] - this[at]) * t
    this[at + 1] += (colour[1] - this[at + 1]) * t
    this[at + 2] += (colour[2] - this[at + 2]) * t
}

/** Moves the colour at [into] towards the one at [from] by [t]. */
private fun FloatArray.mixFrom(into: Int, from: Int, t: Float) {
    this[into] += (this[from] - this[into]) * t
    this[into + 1] += (this[from + 1] - this[into + 1]) * t
    this[into + 2] += (this[from + 2] - this[into + 2]) * t
}

/**
 * A soft picture worked out on a coarse grid once and read back anywhere, smoothly: what glass
 * bends in the designs made of glass, which read it many times a pixel, and off the canvas too.
 */
private class Field(private val w: Float, private val h: Float, private val step: Float, shade: (x: Float, y: Float, out: FloatArray) -> Unit) {

    private val across = ceil(w / step).toInt() + 2
    private val down = ceil(h / step).toInt() + 2
    private val values = FloatArray(across * down * 3)

    init {
        val one = FloatArray(3)
        for (j in 0 until down) for (i in 0 until across) {
            shade(i * step, j * step, one)
            val at = (j * across + i) * 3
            values[at] = one[0]
            values[at + 1] = one[1]
            values[at + 2] = one[2]
        }
    }

    /** The colour at ([x], [y]), held at the edge beyond the canvas, into [out] from [at]. */
    fun sample(x: Float, y: Float, out: FloatArray, at: Int) {
        val fx = (x / step).coerceIn(0f, across - 1.001f)
        val fy = (y / step).coerceIn(0f, down - 1.001f)
        val i = fx.toInt()
        val j = fy.toInt()
        val tx = fx - i
        val ty = fy - j
        val a = (j * across + i) * 3
        val b = a + across * 3
        for (c in 0..2) {
            val top = values[a + c] + (values[a + 3 + c] - values[a + c]) * tx
            val bottom = values[b + c] + (values[b + 3 + c] - values[b + c]) * tx
            out[at + c] = top + (bottom - top) * ty
        }
    }
}

/**
 * Soft pools of colour on the sky, each [SPOT] numbers: across and down (of the canvas), radius (of
 * the width), where on the spectrum, and how strong. Bright and full on a dark page, pale by day.
 */
private class Pools(private val k: DepthKit, private val w: Float, private val h: Float, spots: Array<FloatArray>) {

    private val count = spots.size
    private val xs = FloatArray(count) { spots[it][0] * w }
    private val ys = FloatArray(count) { spots[it][1] * h }
    private val inv = FloatArray(count) { 1f / (spots[it][2] * w).let { r -> r * r } }
    private val strength = FloatArray(count) { spots[it][4] }
    private val colours = Array(count) {
        val t = spots[it][3]
        if (k.dark) k.colour(t, lightness = 0.95f, chroma = 1.05f) else k.colour(t, chroma = 0.85f, atLightness = 0.8f)
    }

    fun at(x: Float, y: Float, out: FloatArray) {
        k.sky(y / h, out, 0)
        for (i in 0 until count) {
            val dx = x - xs[i]
            val dy = y - ys[i]
            val g = exp(-(dx * dx + dy * dy) * inv[i]) * strength[i]
            out.towards(0, colours[i], g)
        }
    }
}

// --------------------------------------------------------------------------------------- Fluted

/**
 * Reeded glass over soft pools of colour: tall half-round ribs, each a lens that squeezes and turns
 * about the colour behind it, red bent a touch more than blue, a hairline of shine down the side
 * that faces the light and a shadow in the groove between two.
 */
private class Fluted(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val rib = w / RIBS
    private val pools = Pools(
        k, w, h,
        arrayOf(
            floatArrayOf(0.22f, 0.20f, 0.36f, 0.0f, 0.95f),
            floatArrayOf(0.82f, 0.36f, 0.44f, 1.0f, 0.9f),
            floatArrayOf(0.42f, 0.56f, 0.40f, 0.5f, 0.75f),
            floatArrayOf(0.08f, 0.78f, 0.34f, 0.8f, 0.5f),
        ),
    )
    private val behind = Field(w, h, w / 90f) { x, y, out -> pools.at(x, y, out) }
    private val light = Shade.unit(-0.6f, -0.45f, 0.66f)
    private val half = Shade.half(light)
    private val groove = if (dark) 0.5f else 0.16f
    private val gloss = if (dark) 0.42f else 0.3f
    private val sheen = if (dark) 0.05f else 0.06f

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        val f = x / rib
        val u = f - floor(f)
        // -1 at a rib's left edge to 1 at its right: its face leans across it as a half-round does.
        val s = 2f * u - 1f
        val nx = s * 0.78f
        val nz = sqrt(1f - nx * nx)
        // Through it, the colour behind, pulled across and turned about.
        val pull = -s * rib * 1.15f
        behind.sample(x + pull * 1.07f, y, px, 3)
        behind.sample(x + pull, y, px, 6)
        behind.sample(x + pull * 0.93f, y, px, 9)
        px[0] = px[3]
        px[1] = px[7]
        px[2] = px[11]
        val facing = nx * light[0] + nz * light[2]
        val lift = if (dark) 0.8f + 0.32f * facing else 0.9f + 0.14f * facing
        val nh = Shade.clamp01(nx * half[0] + nz * half[2])
        val spec = Shade.pow64(nh) * gloss + Shade.pow8(nh) * sheen
        val edge = min(u, 1f - u)
        val dip = exp(-(edge * edge) / (0.05f * 0.05f)) * groove
        for (c in 0..2) px[c] = px[c] * lift * (1f - dip) + spec
        k.towardsDeep(px, (1f - Shade.smoothstep(0f, 0.1f, v)) * 0.25f)
        k.towardsBase(px, Shade.smoothstep(0.5f, 1f, v) * if (dark) 0.5f else 0.4f)
    }

    private companion object {
        /** How many ribs across the screen. */
        const val RIBS = 11f
    }
}

// ------------------------------------------------------------------------------------------ Orb

/**
 * A ball of clear glass floating in front of soft bands of colour. Through it the bands show upside
 * down and drawn in, as a glass ball shows the world; its rim takes on the light, a bright point
 * shines where it faces the lamp, the light it gathers glows inside its far side, and below it its
 * shadow lies on the colour with a little of that gathered light at its heart.
 */
private class Orb(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val cx = 0.5f * w
    private val cy = 0.38f * h
    private val radius = 0.33f * w
    private val ca = cos(Math.toRadians(32.0)).toFloat()
    private val sa = sin(Math.toRadians(32.0)).toFloat()
    private val band = Array(BANDS) {
        val t = it / (BANDS - 1f)
        if (dark) k.colour(t, lightness = 1f, chroma = 1.05f) else k.colour(t, chroma = 0.9f, atLightness = 0.78f)
    }
    private val bandStrength = if (dark) 0.85f else 0.7f
    private val behind = Field(w, h, w / 90f) { x, y, out -> backdrop(x, y, out) }
    private val light = Shade.unit(-0.5f, -0.62f, 0.6f)
    private val half = Shade.half(light)
    private val rimColour = if (dark) k.colour(0.5f, lightness = 1.2f, chroma = 0.4f) else k.colour(0.5f, chroma = 0.3f, atLightness = 0.97f)
    private val gathered = if (dark) k.colour(0.3f, lightness = 1.1f, chroma = 1.1f) else k.colour(0.3f, chroma = 0.8f, atLightness = 0.92f)
    private val through = if (dark) 1.12f else 1.04f
    private val rimStrength = if (dark) 0.75f else 0.5f
    private val shadowStrength = if (dark) 0.6f else 0.22f
    private val causticStrength = if (dark) 0.22f else 0.45f

    private fun backdrop(x: Float, y: Float, out: FloatArray) {
        k.sky(y / h, out, 0)
        // Wide soft bands running across at a slant.
        val q = (x * ca + y * sa) / w
        for (i in 0 until BANDS) {
            val d = (q - (0.32f + 0.3f * i)) / 0.1f
            out.towards(0, band[i], exp(-d * d) * bandStrength)
        }
    }

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        behind.sample(x, y, px, 0)
        // Its shadow, below and away from the light, with the light it gathered at its heart.
        val sx = (x - (cx + 0.07f * w)) / (radius * 1.05f)
        val sy = (y - (cy + radius * 1.32f)) / (radius * 0.3f)
        val shadow = exp(-(sx * sx + sy * sy) * 1.1f) * shadowStrength
        for (c in 0..2) px[c] *= 1f - shadow
        val cxs = sx + 0.12f
        val caustic = exp(-(cxs * cxs + sy * sy) * (if (dark) 9f else 5f)) * causticStrength
        if (dark) {
            for (c in 0..2) px[c] += gathered[c] * caustic
        } else {
            for (c in 0..2) px[c] += gathered[c] * caustic * 0.25f
            px.towards(0, gathered, caustic * 0.5f)
        }
        val dx = x - cx
        val dy = y - cy
        val d = sqrt(dx * dx + dy * dy)
        if (d < radius + 1f) {
            val q = Shade.clamp01(d / radius)
            val nz = sqrt(max(0f, 1f - q * q))
            val nx = dx / radius
            val ny = dy / radius
            // The world behind, turned upside down and drawn in, more so towards the rim.
            val bend = -0.6f - 0.3f * (1f - nz)
            behind.sample(cx + dx * bend, cy + dy * bend, px, 3)
            for (c in 3..5) px[c] *= through
            // Light gathered inside its far side.
            val gx = nx - 0.34f
            val gy = ny - 0.42f
            val glow = exp(-(gx * gx + gy * gy) / 0.05f) * (if (dark) 0.3f else 0.4f)
            for (c in 0..2) px[3 + c] += gathered[c] * glow * (if (dark) 0.6f else 0.35f)
            // The rim: light caught from all round, and dark just inside it, where the glass turns the light away.
            val fresnel = (1f - nz).let { it * it * it }
            val inside = Shade.smoothstep(0.82f, 0.97f, q) * (1f - Shade.smoothstep(0.97f, 1f, q))
            for (c in 0..2) {
                px[3 + c] = px[3 + c] * (1f - inside * (if (dark) 0.35f else 0.15f)) + rimColour[c] * fresnel * rimStrength
            }
            val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
            val spec = Shade.pow64(nh) * 1.1f + Shade.pow16(nh) * 0.12f
            for (c in 3..5) px[c] += spec
            px.mixFrom(0, 3, Shade.clamp01(radius - d + 0.5f))
        }
        k.towardsDeep(px, (1f - Shade.smoothstep(0f, 0.1f, v)) * 0.2f)
        k.towardsBase(px, Shade.smoothstep(0.62f, 1f, v) * if (dark) 0.5f else 0.4f)
    }

    private companion object {
        const val BANDS = 3
    }
}

// ----------------------------------------------------------------------------------------- Arcs

/**
 * Thick glossy tubes curving in from the top corner, one inside the next, each in its own colour of
 * the spectrum: lit across their roundness, a line of shine along each, and each lying a little
 * above the sky, so a soft shadow falls beside it.
 */
private class Arcs(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val cx = 1.06f * w
    private val cy = -0.05f * h
    private val radii = FloatArray(COUNT) { (0.40f + 0.205f * it) * w }
    private val thick = 0.078f * w
    private val lit = Array(COUNT) { colours(it, true) }
    private val deep = Array(COUNT) { colours(it, false) }
    private val light = Shade.unit(-0.55f, -0.6f, 0.58f)
    private val half = Shade.half(light)
    private val shiftX = 0.02f * w
    private val shiftY = 0.03f * w
    private val soft = 0.035f * w
    private val shadowStrength = if (dark) 0.6f else 0.2f
    private val gloss = if (dark) 0.75f else 0.5f
    private val glow = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.6f, atLightness = 0.92f)

    private fun colours(i: Int, bright: Boolean): FloatArray {
        val t = i / (COUNT - 1f)
        return when {
            dark && bright -> k.colour(t, lightness = 1.02f, chroma = 1f)
            dark -> k.colour(t, lightness = 0.36f, chroma = 1.1f)
            bright -> k.colour(t, chroma = 0.7f, atLightness = 0.9f)
            else -> k.colour(t, chroma = 1f, atLightness = 0.64f)
        }
    }

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        k.sky(v, px, 0)
        // The sky lit a little behind them.
        val gx = (x - 0.7f * w) / w
        val gy = (y - 0.2f * h) / w
        px.towards(0, glow, exp(-(gx * gx + gy * gy) / 0.3f) * (if (dark) 0.12f else 0.3f))
        // Shadows first: each tube's own outline, moved away from the light and softened.
        val sdx = x - shiftX - cx
        val sdy = y - shiftY - cy
        val sd = sqrt(sdx * sdx + sdy * sdy)
        var shade = 0f
        for (i in 0 until COUNT) {
            val off = abs(sd - radii[i]) - thick
            shade = max(shade, 1f - Shade.smoothstep(-soft, soft, off))
        }
        for (c in 0..2) px[c] *= 1f - shade * shadowStrength
        val dx = x - cx
        val dy = y - cy
        val d = sqrt(dx * dx + dy * dy) + 1e-3f
        for (i in 0 until COUNT) {
            val off = d - radii[i]
            if (abs(off) > thick + 1f) continue
            val s = (off / thick).coerceIn(-1f, 1f)
            val nz = sqrt(max(0f, 1f - s * s))
            val nx = dx / d * s
            val ny = dy / d * s
            val wrap = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + nz * light[2])
            val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
            val n8 = Shade.pow8(nh)
            val spec = n8 * n8 * n8 * gloss + n8 * 0.1f
            val fresnel = (1f - nz).let { it * it } * (if (dark) 0.3f else 0.15f)
            val shaded = wrap * sqrt(wrap)
            val cover = Shade.clamp01(thick - abs(off) + 0.5f)
            for (c in 0..2) {
                val col = deep[i][c] + (lit[i][c] - deep[i][c]) * shaded + spec + lit[i][c] * fresnel
                px[c] += (col - px[c]) * cover
            }
        }
        k.towardsBase(px, Shade.smoothstep(0.5f, 1f, v) * if (dark) 0.4f else 0.3f)
    }

    private companion object {
        const val COUNT = 5
    }
}

// ----------------------------------------------------------------------------------------- Pond

/**
 * Still water a drop has fallen into, twice: rings running out from each, fading as they go and
 * crossing where they meet, the water lit by the sky, deeper in colour in the troughs and catching
 * points of light on the crests.
 */
private class Pond(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val xs = floatArrayOf(0.36f * w, 0.74f * w)
    private val ys = floatArrayOf(0.28f * h, 0.47f * h)
    private val wave = floatArrayOf(TAU / (0.062f * w), TAU / (0.05f * w))
    private val fade = floatArrayOf(0.55f * w, 0.42f * w)
    private val slope = floatArrayOf(0.55f, 0.42f)
    private val still = 0.012f * w
    private val light = Shade.unit(-0.45f, -0.7f, 0.55f)
    private val half = Shade.half(light)
    private val lit = if (dark) k.ramp(0.8f, 1f) else k.rampAt(0.86f, 0.75f)
    private val deep = if (dark) k.ramp(0.2f, 1.1f) else k.rampAt(0.66f, 0.95f)
    private val skyLight = if (dark) k.colour(0.5f, lightness = 1f, chroma = 0.6f) else k.colour(0.5f, chroma = 0.3f, atLightness = 0.97f)
    private val gloss = if (dark) 1.1f else 0.7f

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        var gx = 0f
        var gy = 0f
        var crest = 0f
        for (i in 0..1) {
            val dx = x - xs[i]
            val dy = y - ys[i]
            val r = sqrt(dx * dx + dy * dy) + 1e-3f
            val env = exp(-r / fade[i]) * Shade.smoothstep(0f, still, r)
            val phase = r * wave[i]
            val cs = cos(phase)
            // How steep the water is here, out from the drop: the rings' height, a falling sine.
            val dh = slope[i] * env * (cs - sin(phase) / (wave[i] * fade[i]))
            gx += dh * dx / r
            gy += dh * dy / r
            crest += sin(phase) * env
        }
        val l = sqrt(gx * gx + gy * gy + 1f)
        val nx = -gx / l
        val ny = -gy / l
        val nz = 1f / l
        val t = Shade.clamp01(0.15f + 0.7f * v + 0.12f * crest)
        lit.get(t, px, 3)
        deep.get(t, px, 6)
        val wrap = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + nz * light[2])
        val shaded = wrap * wrap
        val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
        val n64 = Shade.pow64(nh)
        val spec = n64 * n64 * gloss + n64 * 0.15f
        val fresnel = (1f - nz).let { it * sqrt(it) } * (if (dark) 0.7f else 0.6f)
        for (c in 0..2) px[c] = px[6 + c] + (px[3 + c] - px[6 + c]) * shaded + spec
        px.towards(0, skyLight, Shade.clamp01(fresnel))
        // The sky above it, faintly, at the top.
        k.towardsDeep(px, (1f - Shade.smoothstep(0f, 0.12f, v)) * if (dark) 0.5f else 0.35f)
        k.towardsBase(px, Shade.smoothstep(0.5f, 1f, v) * if (dark) 0.65f else 0.45f)
    }
}

// ------------------------------------------------------------------------------------------ Dew

/**
 * Drops on a window with colour beyond it, out of focus. Each drop is a little lens: the colour
 * beyond shows through it upside down and brighter, it is dark at the edge that faces the light and
 * bright inside the other, with a point of light near the top, and a faint shadow beside it. They
 * gather near the top, thin out lower down, and leave the bottom of the window clear.
 */
private class Dew(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val pools = Pools(
        k, w, h,
        arrayOf(
            floatArrayOf(0.18f, 0.14f, 0.30f, 0.0f, 0.9f),
            floatArrayOf(0.78f, 0.22f, 0.34f, 0.75f, 0.85f),
            floatArrayOf(0.46f, 0.42f, 0.36f, 0.4f, 0.8f),
            floatArrayOf(0.92f, 0.62f, 0.32f, 1.0f, 0.6f),
            floatArrayOf(0.12f, 0.66f, 0.30f, 0.6f, 0.55f),
        ),
    )
    private val behind = Field(w, h, w / 90f) { x, y, out -> pools.at(x, y, out) }
    private val cell = 0.092f * w
    private val fine = 0.03f * w
    private val light = Shade.unit(-0.5f, -0.65f, 0.58f)
    private val half = Shade.half(light)
    private val haze = if (dark) k.colour(0.5f, lightness = 0.5f, chroma = 0.4f) else k.colour(0.5f, chroma = 0.25f, atLightness = 0.93f)
    private val bright = if (dark) 1.2f else 1.06f
    private val rimDark = if (dark) 0.6f else 0.28f

    /** How likely a cell is to hold a drop at height [v]: many near the top, none at the bottom. */
    private fun density(v: Float): Float = 0.85f * (1f - Shade.smoothstep(0.25f, 0.8f, v))

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        behind.sample(x, y, px, 0)
        // The glass misted a little.
        px.towards(0, haze, 0.12f)
        if (!drop(x, y, px, cell, 0, 0.16f, 0.42f)) drop(x, y, px, fine, 1, 0.12f, 0.3f)
        k.towardsDeep(px, (1f - Shade.smoothstep(0f, 0.08f, v)) * 0.2f)
        k.towardsBase(px, Shade.smoothstep(0.55f, 1f, v) * if (dark) 0.5f else 0.4f)
    }

    /**
     * Shades the drop of the grid [size] apart (its [layer], for its own chances) that this point is
     * in or beside, if there is one: [small] to [large] of a cell across. True if it was inside one.
     */
    private fun drop(x: Float, y: Float, px: FloatArray, size: Float, layer: Int, small: Float, large: Float): Boolean {
        val i = floor(x / size).toInt()
        val j = floor(y / size).toInt()
        val seed = layer * 7919
        if (hash(i + seed, j) > density((j + 0.5f) * size / h) * (if (layer == 0) 1f else 0.8f)) return false
        val big = hash(i + seed, j + 101)
        val r = size * (small + (large - small) * big * big)
        // Wholly inside its own cell, so no other cell need be asked.
        val room = size - 2f * r * 1.2f
        val ox = (i + 0f) * size + r * 1.2f + room * hash(i + seed, j + 202)
        val oy = (j + 0f) * size + r * 1.2f + room * hash(i + seed, j + 303)
        val dx = x - ox
        val dy = (y - oy) / 0.94f
        val d = sqrt(dx * dx + dy * dy)
        // Its shadow, beside it away from the light.
        val sx = dx - 0.14f * r
        val sy = dy - 0.2f * r
        val sd = sqrt(sx * sx + sy * sy)
        if (d >= r + 1f) {
            if (sd < r * 1.15f) {
                val s = (1f - Shade.smoothstep(r * 0.7f, r * 1.15f, sd)) * (if (dark) 0.3f else 0.12f)
                for (c in 0..2) px[c] *= 1f - s
            }
            return false
        }
        val q = Shade.clamp01(d / r)
        val nz = sqrt(max(0f, 1f - q * q))
        val nx = dx / r
        val ny = dy / r
        // The colour beyond, turned upside down and drawn from a wider stretch of it.
        behind.sample(ox - dx * 3f, oy - dy * 3f, px, 3)
        for (c in 3..5) px[c] *= bright
        // Dark at the edge towards the light, bright inside the edge away from it.
        val facing = if (q > 1e-3f) -(nx * light[0] + ny * light[1]) / q else 0f
        val edge = Shade.smoothstep(0.62f, 1f, q)
        val dim = edge * rimDark * (0.55f + 0.45f * facing)
        val cxs = nx - 0.38f
        val cys = ny - 0.46f
        val caustic = exp(-(cxs * cxs + cys * cys) / 0.07f) * (if (dark) 0.5f else 0.3f)
        val nh = Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])
        val n16 = Shade.pow16(nh)
        val hx = nx + 0.36f
        val hy = ny + 0.42f
        val point = exp(-(hx * hx + hy * hy) / 0.012f) * 0.9f
        for (c in 0..2) px[3 + c] = px[3 + c] * (1f - dim) + px[3 + c] * caustic + n16 * n16 * 0.6f + point
        px.mixFrom(0, 3, Shade.clamp01(r - d + 0.5f))
        return true
    }
}

// ------------------------------------------------------------------------------------------ Fan

/**
 * A fan of tinted glass petals opening upwards from below the screen, each lying over the one
 * before. Every petal widens from the pivot to a rounded end, clear down by the pivot and richer
 * towards its end, so where two cross their colours lie one over the other. A bevel runs round
 * each, bright on the edges that face the light and shaded on the others, a sheen of light
 * crosses them all, and each lays a soft shadow on the petals beneath it.
 */
private class Fan(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val pivotX = 0.5f * w
    private val pivotY = 1.06f * h
    private val sinA = FloatArray(COUNT) { sin(angle(it)) }
    private val cosA = FloatArray(COUNT) { cos(angle(it)) }
    private val sinHalf = sin(HALF)
    private val cosHalf = cos(HALF)
    private val lengths = FloatArray(COUNT) {
        val off = abs(it - (COUNT - 1) / 2f) / ((COUNT - 1) / 2f)
        (0.84f - 0.1f * off * off) * h
    }

    /** How far along each petal the middle of its rounded end lies, and that end's radius. */
    private val ends = FloatArray(COUNT) { lengths[it] / (1f + sinHalf) }
    private val endRadius = FloatArray(COUNT) { ends[it] * sinHalf }
    private val colour = Array(COUNT) {
        val t = it / (COUNT - 1f)
        if (dark) k.colour(t, lightness = 0.95f, chroma = 1.05f) else k.colour(t, chroma = 0.9f, atLightness = 0.78f)
    }
    private val dusk = Array(COUNT) {
        val t = it / (COUNT - 1f)
        if (dark) k.colour(t, lightness = 0.4f, chroma = 1.1f) else k.colour(t, chroma = 1f, atLightness = 0.6f)
    }
    private val glow = if (dark) k.colour(0.5f, lightness = 0.85f) else k.colour(0.5f, chroma = 0.6f, atLightness = 0.93f)
    private val opacity = if (dark) 0.62f else 0.5f
    private val rimLight = if (dark) 0.55f else 0.5f
    private val rimShade = if (dark) 0.35f else 0.15f
    private val sheenStrength = if (dark) 0.1f else 0.12f
    private val shadowStrength = if (dark) 0.42f else 0.14f
    private val shiftX = 0.016f * w
    private val shiftY = 0.024f * w
    private val soft = 0.05f * w
    private val bevel = 0.007f * w

    private fun angle(i: Int) = Math.toRadians(SPREAD * (2.0 * i / (COUNT - 1) - 1.0)).toFloat()

    /** How far the point [across] and [along] petal [i] lies outside it, in pixels: below 0 inside. */
    private fun outline(i: Int, across: Float, along: Float): Float {
        val ax = abs(across)
        val reach = ax * sinHalf + along * cosHalf
        return when {
            reach < 0f -> sqrt(ax * ax + along * along)
            reach > cosHalf * ends[i] -> {
                val up = along - ends[i]
                sqrt(ax * ax + up * up) - endRadius[i]
            }
            else -> ax * cosHalf - along * sinHalf
        }
    }

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        k.sky(v, px, 0)
        val gx = (x - 0.5f * w) / w
        val gy = (y - 0.4f * h) / w
        px.towards(0, glow, exp(-(gx * gx + gy * gy) / 0.4f) * (if (dark) 0.16f else 0.3f))
        // A band of light lying across all of them, as on glass under a window.
        val band = (x * 0.5f - y * 0.3f) / w + 0.05f
        val sheen = exp(-band * band / 0.012f) * sheenStrength
        val dx = x - pivotX
        val dy = y - pivotY
        // Left to right, each over the one before.
        for (i in 0 until COUNT) {
            val sa = sinA[i]
            val ca = cosA[i]
            // Along the petal from the pivot, and across it, to its right.
            val along = dx * sa - dy * ca
            val across = dx * ca + dy * sa
            // Its shadow on whatever lies beneath it, moved down and to the right.
            val shadowAt = outline(i, across - shiftX * ca - shiftY * sa, along - shiftX * sa + shiftY * ca)
            if (shadowAt < soft) {
                val s = (1f - Shade.smoothstep(-soft, soft, shadowAt)) * shadowStrength * Shade.smoothstep(0.05f, 0.35f, along / lengths[i])
                for (c in 0..2) px[c] *= 1f - s
            }
            val at = outline(i, across, along)
            if (at > 1f) continue
            val f = Shade.clamp01(along / lengths[i])
            // Clear down by the pivot, richest towards its end.
            val a = opacity * Shade.smoothstep(0.04f, 0.4f, f) * Shade.clamp01(0.5f - at)
            val tone = Shade.smoothstep(0.1f, 1f, f)
            val lit = colour[i]
            val shade = dusk[i]
            for (c in 0..2) px[c] += (shade[c] + (lit[c] - shade[c]) * tone + sheen - px[c]) * a
            // The bevel: which way the nearest edge faces, and whether that is towards the light.
            if (at < -4f * bevel) continue
            val ax = abs(across)
            var nAcross = cosHalf
            var nAlong = -sinHalf
            if (ax * sinHalf + along * cosHalf > cosHalf * ends[i]) {
                val up = along - ends[i]
                val l = sqrt(ax * ax + up * up) + 1e-3f
                nAcross = ax / l
                nAlong = up / l
            }
            if (across < 0f) nAcross = -nAcross
            val facing = (nAcross * ca + nAlong * sa) * LIGHT_X + (nAcross * sa - nAlong * ca) * LIGHT_Y
            val rim = exp(at / bevel) * Shade.smoothstep(0.04f, 0.3f, f) * Shade.clamp01(0.5f - at)
            if (facing > 0f) {
                for (c in 0..2) px[c] += rim * facing * rimLight
            } else {
                for (c in 0..2) px[c] *= 1f + rim * facing * rimShade
            }
        }
        k.towardsDeep(px, (1f - Shade.smoothstep(0f, 0.1f, v)) * 0.3f)
        k.towardsBase(px, Shade.smoothstep(0.55f, 1f, v) * if (dark) 0.55f else 0.45f)
    }

    private companion object {
        const val COUNT = 7

        /** How far either way the outermost petals lean, in degrees. */
        const val SPREAD = 42.0

        /** Half a petal's width, as an angle at the pivot. */
        val HALF = Math.toRadians(10.5).toFloat()

        /** Towards the light, on the screen: up and to the left. */
        const val LIGHT_X = -0.55f
        const val LIGHT_Y = -0.83f
    }
}

// ---------------------------------------------------------------------------------------- Stack

/**
 * Cards of frosted glass stacked at a slant over soft colour, each a little further along than the
 * one behind it: the colour beyond softened through each, tinted with its own colour of the
 * spectrum, the top edges catching the light, a hairline of shade along the bottom ones, and each
 * card's shadow lying on the ones below.
 */
private class Stack(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val pools = Pools(
        k, w, h,
        arrayOf(
            floatArrayOf(0.25f, 0.18f, 0.4f, 0.0f, 0.9f),
            floatArrayOf(0.85f, 0.42f, 0.42f, 1.0f, 0.85f),
            floatArrayOf(0.4f, 0.58f, 0.36f, 0.5f, 0.6f),
        ),
    )
    private val behind = Field(w, h, w / 90f) { x, y, out -> pools.at(x, y, out) }
    private val cx = FloatArray(COUNT) { (0.66f - 0.07f * it) * w }
    private val cy = FloatArray(COUNT) { (0.14f + 0.065f * it) * h }
    private val cosA = FloatArray(COUNT) { cos(Math.toRadians(-18.0 + 3.0 * it)).toFloat() }
    private val sinA = FloatArray(COUNT) { sin(Math.toRadians(-18.0 + 3.0 * it)).toFloat() }
    private val halfW = 0.31f * w
    private val halfH = 0.19f * w
    private val corner = 0.06f * w
    private val tint = Array(COUNT) {
        val t = it / (COUNT - 1f)
        if (dark) k.colour(t, lightness = 0.9f, chroma = 0.9f) else k.colour(t, chroma = 0.55f, atLightness = 0.9f)
    }
    private val frost = if (dark) k.colour(0.5f, lightness = 0.75f, chroma = 0.3f) else k.colour(0.5f, chroma = 0.12f, atLightness = 0.97f)
    private val lightX = -0.55f
    private val lightY = -0.83f
    private val shiftX = 0.018f * w
    private val shiftY = 0.03f * w
    private val soft = 0.045f * w
    private val edge = 0.006f * w
    private val shadowStrength = if (dark) 0.5f else 0.16f
    private val tintStrength = if (dark) 0.42f else 0.4f
    private val frostStrength = if (dark) 0.28f else 0.42f
    private val rimLight = if (dark) 0.6f else 0.55f

    /** How far ([lx], [ly]) is outside the card's rounded outline, in pixels: below 0 inside. */
    private fun outline(lx: Float, ly: Float): Float {
        val qx = abs(lx) - (halfW - corner)
        val qy = abs(ly) - (halfH - corner)
        val ox = max(qx, 0f)
        val oy = max(qy, 0f)
        return sqrt(ox * ox + oy * oy) + min(max(qx, qy), 0f) - corner
    }

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        behind.sample(x, y, px, 0)
        // The softened colour each card shows, from the same pools blurred a little further.
        behind.sample(x - 0.03f * w, y - 0.02f * w, px, 6)
        for (i in 0 until COUNT) {
            // Its shadow on what lies below.
            val sx = x - shiftX - cx[i]
            val sy = y - shiftY - cy[i]
            val shadowAt = outline(sx * cosA[i] + sy * sinA[i], -sx * sinA[i] + sy * cosA[i])
            if (shadowAt < soft) {
                val s = (1f - Shade.smoothstep(-soft, soft, shadowAt)) * shadowStrength
                for (c in 0..2) px[c] *= 1f - s
            }
            val dx = x - cx[i]
            val dy = y - cy[i]
            val lx = dx * cosA[i] + dy * sinA[i]
            val ly = -dx * sinA[i] + dy * cosA[i]
            val at = outline(lx, ly)
            if (at > 1f) continue
            val cover = Shade.clamp01(0.5f - at)
            // Frosted: the colour beyond, softened and lifted, tinted with the card's own colour.
            for (c in 0..2) px[3 + c] = px[6 + c]
            px.towards(3, frost, frostStrength)
            px.towards(3, tint[i], tintStrength)
            // Lighter towards its top.
            val down = Shade.clamp01(0.5f + 0.5f * ly / halfH)
            for (c in 3..5) px[c] *= (if (dark) 1.08f else 1.03f) - 0.12f * down
            // Its edge: which way it faces, and whether that is towards the light.
            val qx = abs(lx) - (halfW - corner)
            val qy = abs(ly) - (halfH - corner)
            var ex = 0f
            var ey = 0f
            if (qx > 0f && qy > 0f) {
                val l = sqrt(qx * qx + qy * qy)
                ex = qx / l * (if (lx < 0f) -1f else 1f)
                ey = qy / l * (if (ly < 0f) -1f else 1f)
            } else if (qx > qy) {
                ex = if (lx < 0f) -1f else 1f
            } else {
                ey = if (ly < 0f) -1f else 1f
            }
            // Turned back to the screen's way up, to meet the light.
            val wx = ex * cosA[i] - ey * sinA[i]
            val wy = ex * sinA[i] + ey * cosA[i]
            val facing = wx * lightX + wy * lightY
            val band = exp(at / edge)
            val rim = band * (if (facing > 0f) facing * rimLight else facing * (if (dark) 0.45f else 0.2f))
            for (c in 3..5) px[c] = max(0f, px[c] + rim)
            // The cards behind show faintly through it.
            px.mixFrom(0, 3, cover * 0.95f)
        }
        k.towardsBase(px, Shade.smoothstep(0.55f, 1f, v) * if (dark) 0.45f else 0.35f)
    }

    private companion object {
        const val COUNT = 5
    }
}

// ------------------------------------------------------------------------------------------ Gem

/**
 * A round brilliant seen from above, cut as one is: an eight-sided table in the middle, eight star
 * facets round it, eight kites reaching out to the girdle and sixteen small facets along it. Each
 * facet is turned its own way to the light, so some flash, some throw back the colour set's
 * spectrum from inside the way a diamond throws its fire, and some are dark. Through the table the
 * cut shows again, smaller and turned, with dark arrows running out to its corners. A glow round
 * it, faint rays, and a few glints.
 */
private class Gem(private val e: Easel, private val k: DepthKit) {

    private val w = e.w
    private val h = e.h
    private val dark = k.dark
    private val cx = 0.5f * w
    private val cy = 0.37f * h
    private val radius = 0.34f * w
    private val sector = TAU / 8f
    private val cosHalf = cos(sector / 2f)
    private val sinHalf = sin(sector / 2f)

    // A sixteenth of the cut, the next folded back over it, in radii: the table's corner on the
    // near side, the star's point on the far side, and the girdle at 1.
    private val tableEdge = TABLE_CORNER * cosHalf
    private val starX = STAR_POINT * cosHalf
    private val starY = STAR_POINT * sinHalf
    private val vsX = starX - TABLE_CORNER
    private val vsY = starY
    private val vsLength = sqrt(vsX * vsX + vsY * vsY)
    private val sgX = 1f - starX
    private val sgY = -starY
    private val sgLength = sqrt(sgX * sgX + sgY * sgY)

    private val light = Shade.unit(-0.45f, -0.6f, 0.66f)
    private val halfway = Shade.half(light)
    private val ramp = if (dark) k.ramp(1.05f, 1.1f) else k.rampAt(0.86f, 0.85f)
    private val shadowRamp = if (dark) k.ramp(0.22f, 1.1f) else k.rampAt(0.5f, 1f)
    private val fireRamp = if (dark) k.ramp(1.1f, 1.4f) else k.rampAt(0.74f, 1.35f)
    private val glow = if (dark) k.colour(0.5f, lightness = 1f, chroma = 1f) else k.colour(0.5f, chroma = 0.7f, atLightness = 0.85f)
    private val glowStrength = if (dark) 0.5f else 0.4f
    private val seamWidth = 0.0026f * w / radius
    private val glints = arrayOf(
        floatArrayOf(-0.5f, -0.48f, 1f),
        floatArrayOf(0.86f, -0.18f, 0.75f),
        floatArrayOf(0.14f, 0.82f, 0.55f),
        floatArrayOf(-0.2f, 0.12f, 0.5f),
    )

    // Each facet, the crown's first and then the cut seen again through the table, by its eighth,
    // its side of the eighth and its kind: how bright, its colour, its flash, whether it throws
    // fire, which way it runs and whether it brightens outwards or in.
    private val level = FloatArray(FACETS)
    private val hue = FloatArray(FACETS)
    private val flash = FloatArray(FACETS)
    private val fire = FloatArray(FACETS)
    private val dirX = FloatArray(FACETS)
    private val dirY = FloatArray(FACETS)
    private val slope = FloatArray(FACETS)

    init {
        for (layer in 0..1) for (eighth in 0 until 8) for (side in 0..1) for (kind in 0..3) {
            val at = index(layer, eighth, side, kind)
            // Which way it faces, as a share of its eighth, folded the same way as the cut.
            val folded = when (kind) {
                STAR -> 0.5f
                GIRDLE -> 0.22f
                else -> 0f
            }
            val azimuth = (eighth + (if (side == 1) 1f - folded else folded) - 0.5f * layer) * sector - SPIN
            val tilt = when (kind) {
                TABLE -> 0f
                STAR -> 0.38f
                KITE -> 0.6f
                else -> 0.74f
            }
            val nx = cos(azimuth) * sin(tilt)
            val ny = sin(azimuth) * sin(tilt)
            val nz = cos(tilt)
            val facing = Shade.clamp01(nx * light[0] + ny * light[1] + nz * light[2])
            val nh = Shade.clamp01(nx * halfway[0] + ny * halfway[1] + nz * halfway[2])
            // The same facet on both sides of a fold that runs through it.
            val id = when (kind) {
                STAR -> eighth
                KITE -> (eighth + side) % 8
                GIRDLE -> eighth * 2 + side
                else -> 0
            } + layer * 40
            val inside = hash(id, kind * 31 + 7)
            var bright = Math.pow((0.55f * inside + 0.45f * facing).toDouble(), 1.4).toFloat()
            // Seen through the table, the stars run dark, as the heads of the arrows.
            if (layer == 1) bright *= if (kind == STAR) 0.3f else 0.85f
            level[at] = 0.06f + 0.94f * bright
            hue[at] = hash(id + 5, kind * 17 + 3)
            flash[at] = Math.pow(nh.toDouble(), 36.0).toFloat() * (if (dark) 1.3f else 0.9f) * (if (layer == 1) 0.5f else 1f)
            fire[at] = if (hash(id + 11, kind + 90) > 0.68f) 1f else 0f
            dirX[at] = cos(azimuth)
            dirY[at] = sin(azimuth)
            slope[at] = if (hash(id + 3, kind + 50) > 0.5f) 1f else -1f
        }
    }

    private fun index(layer: Int, eighth: Int, side: Int, kind: Int) = ((layer * 8 + eighth) * 2 + side) * 4 + kind

    /** Which facet ([u], [v]) of the folded sixteenth lies in. */
    private fun facet(u: Float, v: Float): Int = when {
        u * cosHalf + v * sinHalf < tableEdge -> TABLE
        sgX * (v - starY) - sgY * (u - starX) > 0f -> GIRDLE
        vsX * v - vsY * (u - TABLE_CORNER) > 0f -> STAR
        else -> KITE
    }

    /** How far ([u], [v]) lies from the nearest edge of its facet [kind], in radii. */
    private fun seam(u: Float, v: Float, kind: Int): Float {
        val table = abs(u * cosHalf + v * sinHalf - tableEdge)
        val star = abs(vsX * v - vsY * (u - TABLE_CORNER)) / vsLength
        val girdle = abs(sgX * (v - starY) - sgY * (u - starX)) / sgLength
        return when (kind) {
            TABLE -> table
            STAR -> min(table, star)
            KITE -> min(star, girdle)
            else -> min(girdle, abs(u * sinHalf - v * cosHalf))
        }
    }

    fun paint() = e.pixels { x, y, px ->
        val v = y / h
        k.sky(v, px, 0)
        val dx = x - cx
        val dy = y - cy
        val r = sqrt(dx * dx + dy * dy) + 1e-3f
        val beyond = max(r - radius, 0f) / radius
        var halo = exp(-beyond / 0.4f) * glowStrength
        var a = 0f
        if (r < 2.6f * radius) {
            a = atan2(dy, dx)
            // Faint rays out from it.
            val q = 0.5f + 0.5f * cos(a * 16f + 0.5f)
            val q2 = q * q
            halo += q2 * q2 * exp(-beyond / 0.7f) * (1f - Shade.smoothstep(0.9f, 1.6f, beyond)) * (if (dark) 0.16f else 0.12f)
        }
        if (dark) {
            for (c in 0..2) px[c] += glow[c] * halo * 0.5f
        } else {
            px.towards(0, glow, Shade.clamp01(halo * 0.7f))
        }
        if (r < radius + 1f) cut(px, dx, dy, r, a)
        for (g in glints) {
            val gx = (x - (cx + g[0] * radius)) / w
            val gy = (y - (cy + g[1] * radius)) / w
            if (abs(gx) > 0.16f || abs(gy) > 0.16f) continue
            val cross = exp(-abs(gx) / 0.0016f - gy * gy / 0.0025f) + exp(-abs(gy) / 0.0016f - gx * gx / 0.0025f)
            val core = exp(-(gx * gx + gy * gy) / 0.00006f)
            val s = (cross * 0.6f + core) * g[2] * (if (dark) 1f else 0.7f)
            for (c in 0..2) px[c] += s
        }
        k.towardsBase(px, Shade.smoothstep(0.58f, 1f, v) * if (dark) 0.5f else 0.4f)
    }

    /** The gem at ([dx], [dy]) from its middle, [r] out at the angle [a], over what [px] holds. */
    private fun cut(px: FloatArray, dx: Float, dy: Float, r: Float, a: Float) {
        val unit = r / radius
        // Folded into one sixteenth: which eighth, which side of it, and how far round.
        val turned = (a + SPIN) / sector
        val whole = floor(turned)
        val part = turned - whole
        val side = if (part > 0.5f) 1 else 0
        val phi = (if (side == 1) 1f - part else part) * sector
        val u = unit * cos(phi)
        val v = unit * sin(phi)
        var kind = facet(u, v)
        val at: Int
        val seamAt: Float
        var innerSeam = 1f
        val along: Float
        var shade = 1f
        if (kind != TABLE) {
            at = index(0, ((whole.toInt() % 8) + 8) % 8, side, kind)
            seamAt = seam(u, v, kind)
            along = (dx * dirX[at] + dy * dirY[at]) / radius
        } else {
            // Through the table, the cut again, as the facets beneath throw it back: smaller, so
            // that its girdle meets the table's sides, and turned half an eighth.
            val inner = unit / tableEdge
            val turned2 = turned + 0.5f
            val whole2 = floor(turned2)
            val part2 = turned2 - whole2
            val side2 = if (part2 > 0.5f) 1 else 0
            val phi2 = (if (side2 == 1) 1f - part2 else part2) * sector
            val u2 = inner * cos(phi2)
            val v2 = inner * sin(phi2)
            kind = if (inner >= 1f) GIRDLE else facet(u2, v2)
            at = index(1, ((whole2.toInt() % 8) + 8) % 8, side2, kind)
            seamAt = abs(u * cosHalf + v * sinHalf - tableEdge)
            innerSeam = min(seam(u2, v2, kind), abs(1f - inner)) * tableEdge
            along = (dx * dirX[at] + dy * dirY[at]) / (radius * tableEdge)
            // The arrows' shafts, running out to the table's corners.
            val shaft = 1f - Shade.smoothstep(0.6f, 1f, v / (u * 0.1f + 1e-4f))
            shade = 1f - 0.75f * shaft * Shade.smoothstep(0.08f, 0.2f, unit)
            // The culet, the point at the bottom, right in the middle.
            shade *= 0.25f + 0.75f * Shade.smoothstep(0.015f, 0.05f, unit)
        }
        // Out along the facet, from its inner end to its outer; some brighten outwards and some in.
        val out = Shade.clamp01((along - REACH_IN[kind]) / (REACH_OUT[kind] - REACH_IN[kind]))
        val plain = if (kind == TABLE) {
            0.3f + 0.4f * Shade.smoothstep(0f, TABLE_CORNER, unit / tableEdge)
        } else {
            Shade.clamp01(level[at] * (0.72f + 0.56f * (out - 0.5f) * slope[at]))
        }
        val lit = plain * shade
        val t = hue[at] + 0.18f * (out - 0.5f)
        ramp.get(t, px, 3)
        shadowRamp.get(t, px, 6)
        val shine = flash[at] * (0.6f + 0.8f * out) * shade
        for (c in 0..2) px[3 + c] = px[6 + c] + (px[3 + c] - px[6 + c]) * lit + shine
        if (fire[at] > 0f) {
            fireRamp.get(1f - t, px, 6)
            px.mixFrom(3, 6, 0.5f * lit)
        }
        // Hairlines where facets meet: light catching them by night, shade by day.
        val line = max(1f - Shade.smoothstep(0f, seamWidth, seamAt), 0.5f * (1f - Shade.smoothstep(0f, seamWidth, innerSeam)))
        if (dark) {
            for (c in 3..5) px[c] += line * 0.3f
        } else {
            for (c in 3..5) px[c] *= 1f - line * 0.3f
        }
        // The girdle's edge, smooth, with a fine line round it.
        val edge = 1f - unit
        val rim = exp(-abs(edge) * radius / (0.004f * w))
        if (dark) {
            for (c in 3..5) px[c] += rim * 0.4f
        } else {
            for (c in 3..5) px[c] *= 1f - rim * 0.35f
        }
        px.mixFrom(0, 3, Shade.clamp01(edge * radius + 0.5f))
    }

    private companion object {
        const val TABLE = 0
        const val STAR = 1
        const val KITE = 2
        const val GIRDLE = 3
        const val FACETS = 128

        /** The table's corners and the stars' points, out from the middle, in radii. */
        const val TABLE_CORNER = 0.55f
        const val STAR_POINT = 0.77f

        /** How far the cut is turned from square, in radians. */
        const val SPIN = 0.12f

        /** Where each kind of facet starts and ends, out along the way it faces, in radii. */
        val REACH_IN = floatArrayOf(0f, 0.5f, 0.5f, 0.75f)
        val REACH_OUT = floatArrayOf(1f, 0.78f, 1f, 1f)
    }
}
