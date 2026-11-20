package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Four more designs that open as the phone is unlocked, the Live shelf's second batch: Planet,
 * Aperture, Gyro and Ink. As Lotus and Daybreak, each holds still on the lock screen in its first
 * pose and plays its opening once as the phone is unlocked, coming to rest in its home pose. The
 * layers are painted once; the opening only moves, turns, squashes and fades them, apart from
 * Aperture's blades, which change shape as they open and so are drawn as plain shapes. Nothing
 * here looks at the clock: an opening is all in how far open it is.
 */

private const val TAU = (2.0 * PI).toFloat()
private const val DEGREES = (180.0 / PI).toFloat()

private fun ease(edge0: Float, edge1: Float, x: Float): Float = Shade.smoothstep(edge0, edge1, x)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** A bump that rises to 1 at [at] and falls away either side, [width] wide. */
private fun hump(x: Float, at: Float, width: Float): Float = ((x - at) / width).let { exp(-it * it) }

// ---------------------------------------------------------------------------------------- Planet

/**
 * Planet: a ringed planet far off on the lock screen, a crescent of day round its edge. Unlocked,
 * the planet swoops up close, its rings sweeping wide across the screen and the stars streaming
 * out past it, and the sun flares up over its edge. By day the planet is pastel on a pale sky, lit
 * from in front and above, its air glowing round its sunward edge.
 */
internal class PlanetScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    override val openMs: Long = 1800L

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val ringPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; color = Color.WHITE }
    private val matrix = Matrix()
    private val radius = PLANET * w

    /** Which way the sun is from the planet in the rings' own flat plane, for lighting them. */
    private val sunInRing: Float
    private lateinit var space: Bitmap
    private lateinit var planet: Bitmap
    private lateinit var rim: Bitmap
    private lateinit var ring: Bitmap
    private var flare: Bitmap? = null
    private lateinit var stars: FloatArray
    private lateinit var moved: FloatArray

    init {
        val t = HOME_TILT / DEGREES
        val along = SUN_X * cos(t) + SUN_Y * sin(t)
        val across = -SUN_X * sin(t) + SUN_Y * cos(t)
        sunInRing = atan2(across / SIN_I, along)
    }

    override fun prepare() {
        val nebula = if (dark) k.colour(0.75f, lightness = 0.8f, chroma = 0.9f) else k.colour(0.75f, chroma = 0.45f, atLightness = 0.9f)
        val nebula2 = if (dark) k.colour(0.2f, lightness = 0.7f, chroma = 0.9f) else k.colour(0.2f, chroma = 0.4f, atLightness = 0.92f)
        space = field(4) { x, y, px ->
            val v = y / h
            if (dark) {
                k.dusk(v * 0.85f, px, 0)
                for (ch in 0..2) px[ch] *= 0.7f
            } else {
                k.sky(v * 0.8f, px, 0)
            }
            // Faint clouds of colour far behind.
            val ax = (x - 0.18f * w) / w
            val ay = (y - 0.2f * h) / w
            Shade.mixInto(px, nebula[0], nebula[1], nebula[2], exp(-(ax * ax * 1.6f + ay * ay) / 0.1f) * (if (dark) 0.16f else 0.3f))
            val bx = (x - 0.9f * w) / w
            val by = (y - 0.62f * h) / w
            Shade.mixInto(px, nebula2[0], nebula2[1], nebula2[2], exp(-(bx * bx + by * by * 1.4f) / 0.12f) * (if (dark) 0.12f else 0.25f))
        }
        planet = paintPlanet()
        rim = paintRim()
        ring = paintRing()
        if (dark) flare = paintFlare()
        val random = Random(61)
        stars = FloatArray(STARS * 2)
        for (i in 0 until STARS) {
            stars[i * 2] = random.nextFloat() * w
            stars[i * 2 + 1] = random.nextFloat() * h * 0.8f
        }
        moved = FloatArray(STARS * 2)
    }

    /**
     * The planet at its home size: bands of the colour set's spectrum along its latitudes, their
     * edges stirred into eddies. At night it is lit from behind and to the upper right, so most of
     * it is in night and a crescent of day curves round that edge, its air glowing; by day it is
     * lit from in front and above, its bands showing all across it and its night a soft shade.
     */
    private fun paintPlanet(): Bitmap {
        val size = ceil(2f * radius + 4f).toInt()
        val mid = size / 2f
        val day = if (dark) k.ramp(1.05f, 0.85f) else k.rampAt(0.82f, 0.75f)
        val dusk = if (dark) k.ramp(0.62f, 1f) else k.rampAt(0.7f, 0.9f)
        val night = if (dark) FloatArray(3) { k.deepLight[it] * 0.55f } else k.colour(0.62f, chroma = 0.35f, atLightness = 0.66f)
        val air = if (dark) k.colour(0.5f, lightness = 1.25f, chroma = 0.6f) else k.colour(0.5f, chroma = 0.45f, atLightness = 0.97f)
        val tilt = HOME_TILT / DEGREES
        val ct = cos(tilt)
        val st = sin(tilt)
        val lx = if (dark) LX else FX
        val ly = if (dark) LY else FY
        val lz = if (dark) LZ else FZ
        return sprite(size, size) { x, y, px ->
            val nx = (x - mid) / radius
            val ny = (y - mid) / radius
            val q = nx * nx + ny * ny
            val cover = Shade.clamp01((1f - sqrt(q)) * radius + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val nz = sqrt(max(0f, 1f - q))
            // Its equator lies in the rings' plane, seen from a little above.
            val across = -nx * st + ny * ct
            val sinLat = -across * COS_I + nz * SIN_I
            val stir = LiveNoise.fbm(nx * 2.5f + 3.1f, sinLat * 7f + 1.7f) - 0.5f
            val b = sinLat * 4.5f + stir * 0.6f
            val t = 0.5f + 0.5f * sin(b * 2.3f + 0.9f)
            day.get(t, px, 4)
            dusk.get(t, px, 7)
            val stripe = 0.5f + 0.5f * sin(b * 11f + 2f * stir)
            val ndl = nx * lx + ny * ly + nz * lz
            val lit = if (dark) Shade.smoothstep(-0.1f, 0.45f, ndl) else Shade.smoothstep(-0.5f, 0.55f, ndl)
            val limb = exp(-nz / 0.1f) * Shade.smoothstep(-0.25f, 0.4f, ndl)
            for (ch in 0..2) {
                val surface = px[7 + ch] + (px[4 + ch] - px[7 + ch]) * (0.35f + 0.65f * stripe)
                // The night side keeps a trace of its bands, lit by the rings.
                var col = night[ch] + surface * (if (dark) 0.05f else 0.3f)
                col += (surface - col) * lit
                col += (air[ch] - col) * limb * 0.6f
                px[ch] = col
            }
            px[3] = cover
        }.apply { setHasMipMap(true) }
    }

    /** The glow of the planet's air round the edge towards the sun, a little inside and out. */
    private fun paintRim(): Bitmap {
        val reach = 1.3f * radius
        val size = ceil(2f * reach).toInt()
        val mid = size / 2f
        val colour = if (dark) k.colour(0.5f, lightness = 1.2f, chroma = 0.7f) else k.colour(0.5f, chroma = 0.55f, atLightness = 0.96f)
        return sprite(size, size) { x, y, px ->
            val nx = (x - mid) / radius
            val ny = (y - mid) / radius
            val d = sqrt(nx * nx + ny * ny)
            val towards = Shade.smoothstep(-0.2f, 0.85f, (nx * SUN_X + ny * SUN_Y) / max(d, 1e-3f))
            val g = (if (d < 1f) Shade.smoothstep(0.86f, 1f, d) else exp(-(d - 1f) / 0.045f)) * towards * (1f - Shade.smoothstep(1.2f, 1.3f, d))
            if (dark) {
                for (ch in 0..2) px[ch] = colour[ch] * g * 0.75f
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = colour[ch]
                px[3] = Shade.clamp01(g * 0.55f)
            }
        }.apply { setHasMipMap(true) }
    }

    /**
     * The rings, flat and face on, painted at [RING_RES] of their size: many fine ringlets, a dark
     * gap two thirds of the way out, thinning at both edges, brighter on the side towards the sun.
     */
    private fun paintRing(): Bitmap {
        val outer = RING_OUT * radius * RING_RES
        val inner = RING_IN * radius * RING_RES
        val size = ceil(2f * outer + 4f).toInt()
        val mid = size / 2f
        val tone = if (dark) k.ramp(1.05f, 0.5f) else k.rampAt(0.74f, 0.6f)
        val col = FloatArray(3)
        return sprite(size, size) { x, y, px ->
            val dx = x - mid
            val dy = y - mid
            val r = sqrt(dx * dx + dy * dy)
            if (r < inner - 2f || r > outer + 2f) {
                px[3] = 0f
                return@sprite
            }
            val u = (r - inner) / (outer - inner)
            val edge = Shade.clamp01(r - inner + 0.5f) * Shade.clamp01(outer - r + 0.5f)
            val fine = 0.6f + 0.4f * LiveNoise.value(u * 90f, 3.3f)
            val coarse = 0.5f + 0.5f * LiveNoise.value(u * 14f, 8.1f)
            val gap = 1f - 0.92f * exp(-((u - 0.64f) / 0.025f).let { it * it })
            val body = Shade.smoothstep(0f, 0.12f, u) * (1f - Shade.smoothstep(0.85f, 1f, u))
            val density = body * gap * (0.35f + 0.65f * fine * coarse)
            val sunward = 0.55f + 0.45f * cos(atan2(dy, dx) - sunInRing)
            tone.get(0.25f + 0.5f * u, col, 0)
            if (dark) {
                for (ch in 0..2) px[ch] = col[ch] * density * sunward * 0.8f * edge
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = col[ch]
                px[3] = Shade.clamp01(density * (0.62f + 0.38f * sunward)) * edge
            }
        }.apply { setHasMipMap(true) }
    }

    /** The sun as it comes over the planet's edge, at night: a blinding point, its halo, a short streak across and fine rays. */
    private fun paintFlare(): Bitmap {
        val reach = 0.5f * w
        val size = ceil(2f * reach).toInt()
        val mid = size / 2f
        val hot = k.colour(0.3f, lightness = 1.45f, chroma = 0.25f)
        val tint = k.colour(0.3f, lightness = 1.1f, chroma = 0.8f)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / w
            val dy = (y - mid) / w
            val r = sqrt(dx * dx + dy * dy)
            val core = exp(-(r / 0.014f).let { it * it }) * 2.5f + exp(-r / 0.035f) * 0.7f
            val halo = exp(-r / 0.11f) * 0.4f
            val streak = exp(-abs(dy) / 0.002f) * exp(-abs(dx) / 0.08f) * 0.35f
            val phi = atan2(dy, dx)
            val rays = (0.5f + 0.5f * cos(phi * 7f + 0.4f)).pow(14f) * exp(-r / 0.08f) * 0.6f
            val fade = 1f - Shade.smoothstep(0.75f, 1f, r / 0.5f)
            for (ch in 0..2) px[ch] = (hot[ch] * core + tint[ch] * (halo + streak + rays)) * fade
            px[3] = 1f
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        val x = open.coerceIn(0f, 1f)
        val e = LiveCurves.easeInOut(x)
        drawField(canvas, space, paint)
        // Coming up on the planet at an even pace: its size grows by the same share every moment.
        val scale = START * (1f / START).pow(e)
        val cx = w * lerp(START_X, HOME_X, e)
        val cy = h * lerp(START_Y, HOME_Y, e)
        val turn = lerp(START_TILT, HOME_TILT, e)
        if (dark) {
            // The stars stream out past it as it comes.
            val spread = 1f + 0.6f * e
            val fx = w * START_X
            val fy = h * START_Y
            for (i in 0 until STARS) {
                moved[i * 2] = fx + (stars[i * 2] - fx) * spread
                moved[i * 2 + 1] = fy + (stars[i * 2 + 1] - fy) * spread
            }
            dots.strokeWidth = (1.8f + 0.8f * e) * unit
            dots.alpha = ((0.8f - 0.2f * e) * 255f).toInt()
            canvas.drawPoints(moved, dots)
        }
        drawRing(canvas, cx, cy, scale, turn, back = true)
        matrix.setTranslate(-planet.width / 2f, -planet.height / 2f)
        matrix.postRotate(turn - HOME_TILT)
        matrix.postScale(scale, scale)
        matrix.postTranslate(cx, cy)
        canvas.drawBitmap(planet, matrix, paint)
        drawRing(canvas, cx, cy, scale, turn, back = false)
        // The sun coming up over its edge: a flash that settles into its steady light.
        val rise = if (x <= 0.3f) 0f else LiveCurves.burst((x - 0.3f) / 0.7f, 1.7f)
        drawCentred(canvas, rim, cx, cy, scale, glowPaint, 0.55f + 0.45f * min(rise, 1f))
        val sun = flare
        if (sun != null && rise > 0.004f) {
            val r = radius * scale * 0.99f
            drawCentred(canvas, sun, cx + r * SUN_X, cy + r * SUN_Y, 0.75f + 0.25f * rise, glowPaint, min(rise, 1f))
        }
        settle(canvas, 0.66f, if (dark) 0.28f else 0.22f)
    }

    /** The far half of the rings, behind the planet, or the near half, in front of it. */
    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, scale: Float, turn: Float, back: Boolean) {
        val mid = ring.width / 2f
        val s = scale / RING_RES
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(turn)
        canvas.scale(s, s * SIN_I)
        if (back) canvas.clipRect(-mid, -mid, mid, 0f) else canvas.clipRect(-mid, 0f, mid, mid)
        canvas.drawBitmap(ring, -mid, -mid, ringPaint)
        canvas.restore()
    }

    private companion object {
        /** The planet's radius at home, of the width, and how big it starts, of that. */
        const val PLANET = 0.3f
        const val START = 0.3f

        /** Where it is on the lock screen and at home, of the width and height. */
        const val START_X = 0.5f
        const val START_Y = 0.36f
        const val HOME_X = 0.47f
        const val HOME_Y = 0.46f

        /** How the rings lie across the screen, in degrees, far off and close up. */
        const val START_TILT = -6f
        const val HOME_TILT = -14f

        /** How far the rings are seen from above their plane: how squashed they look. */
        const val SIN_I = 0.22f
        const val COS_I = 0.9755f

        /** The rings' inner and outer edge, in the planet's radii, and how finely they are painted. */
        const val RING_IN = 1.3f
        const val RING_OUT = 2.15f
        const val RING_RES = 0.6f

        /** Where the sun is at night: behind the planet and to its upper right. */
        const val LX = 0.8253f
        const val LY = -0.5502f
        const val LZ = -0.127f

        /** And by day: in front of it, above and to the right. */
        const val FX = 0.6217f
        const val FY = -0.5014f
        const val FZ = 0.6017f

        /** The same, across the screen alone. */
        const val SUN_X = 0.8321f
        const val SUN_Y = -0.5547f

        const val STARS = 150
    }
}

// -------------------------------------------------------------------------------------- Aperture

/**
 * Aperture: the iris of a camera lens, shut tight on the lock screen with a pin of light at its
 * heart. Unlocked, its seven blades swing open, turning as they go, the light pours out of the lens
 * in rays and settles into a glow within the open iris. The blades are drawn as plain shapes each
 * frame, as they change shape as they open; the lens, its light and its bezel are painted once.
 */
internal class ApertureScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    override val openMs: Long = 1500L

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val cx = 0.5f * w
    private val cy = 0.4f * h
    private val housing = HOUSING * w
    private val reach = housing * 1.02f
    private val path = Path()
    private val oval = RectF(cx - reach, cy - reach, cx + reach, cy + reach)
    private val vx = FloatArray(BLADES + 1)
    private val vy = FloatArray(BLADES + 1)
    private val ex = FloatArray(BLADES)
    private val ey = FloatArray(BLADES)
    private val faces: Array<LinearGradient>
    private val shades: Array<LinearGradient>
    private val local = Matrix()
    private lateinit var back: Bitmap
    private lateinit var lens: Bitmap
    private lateinit var glow: Bitmap
    private lateinit var rays: Bitmap
    private lateinit var bezel: Bitmap

    init {
        val edge = LiveNoise.colour(if (dark) k.colour(0.5f, lightness = 0.95f, chroma = 0.35f) else k.colour(0.5f, chroma = 0.06f, atLightness = 0.98f))
        val body = LiveNoise.colour(if (dark) k.colour(0.5f, lightness = 0.42f, chroma = 0.55f) else k.colour(0.5f, chroma = 0.12f, atLightness = 0.86f))
        val far = LiveNoise.colour(if (dark) k.colour(0.5f, lightness = 0.26f, chroma = 0.6f) else k.colour(0.5f, chroma = 0.16f, atLightness = 0.74f))
        // Each blade lit a little differently, as the light catches each at its own angle.
        faces = Array(BLADES) { j ->
            val lift = 0.85f + 0.3f * (0.5f + 0.5f * cos(TAU * j / BLADES + 2.2f))
            LinearGradient(
                0f, 0f, 0f, housing,
                intArrayOf(edge, scale(body, lift), scale(far, lift)),
                floatArrayOf(0f, 0.06f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val shadow = if (dark) Color.argb(150, 0, 0, 0) else alpha(LiveNoise.colour(k.colour(0.5f, chroma = 0.3f, atLightness = 0.45f)), 90)
        shades = Array(BLADES) { LinearGradient(0f, 0f, 0f, 1f, shadow, alpha(shadow, 0), Shader.TileMode.CLAMP) }
        edgePaint.color = if (dark) alpha(edge, 120) else alpha(Color.WHITE, 200)
        edgePaint.strokeWidth = 2f * unit
    }

    /** [colour] [by] times as bright, as a paint's colour. */
    private fun scale(colour: Int, by: Float): Int = Color.rgb(
        (Color.red(colour) * by).toInt().coerceIn(0, 255),
        (Color.green(colour) * by).toInt().coerceIn(0, 255),
        (Color.blue(colour) * by).toInt().coerceIn(0, 255),
    )

    override fun prepare() {
        val glowColour = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.55f, atLightness = 0.92f)
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v * 0.8f, px, 0)
            val gx = (x - cx) / w
            val gy = (y - cy) / w
            Shade.mixInto(px, glowColour[0], glowColour[1], glowColour[2], exp(-(gx * gx + gy * gy) / 0.22f) * (if (dark) 0.14f else 0.3f))
        }
        lens = paintLens()
        glow = paintGlow()
        rays = paintRays()
        bezel = paintBezel()
    }

    /** The glass inside the iris: deep, with the coloured gleams of its coatings at the top left and bottom right. */
    private fun paintLens(): Bitmap {
        val size = ceil(2f * reach + 4f).toInt()
        val mid = size / 2f
        val deep = if (dark) FloatArray(3) { k.deepLight[it] * 0.7f } else k.colour(0.6f, chroma = 0.7f, atLightness = 0.5f)
        val coatA = if (dark) k.colour(0.15f, lightness = 0.95f, chroma = 1f) else k.colour(0.15f, chroma = 0.9f, atLightness = 0.78f)
        val coatB = if (dark) k.colour(0.85f, lightness = 0.95f, chroma = 1f) else k.colour(0.85f, chroma = 0.9f, atLightness = 0.78f)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / reach
            val dy = (y - mid) / reach
            val r = sqrt(dx * dx + dy * dy)
            val cover = Shade.clamp01((1f - r) * reach + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val arcA = exp(-((r - 0.62f) / 0.05f).let { it * it }) * Shade.clamp01(-(dx + dy) * 1.2f)
            val arcB = exp(-((r - 0.4f) / 0.035f).let { it * it }) * Shade.clamp01((dx + dy) * 1.2f)
            val sheen = exp(-((dx + 0.3f) * (dx + 0.3f) + (dy + 0.35f) * (dy + 0.35f)) / 0.04f) * 0.35f
            for (ch in 0..2) px[ch] = deep[ch] * (0.55f + 0.45f * (1f - r * r)) + coatA[ch] * arcA * 0.5f + coatB[ch] * arcB * 0.45f + sheen * coatA[ch] * 0.4f
            px[3] = cover
        }
    }

    /** The light inside the lens: a hot heart fading out to its edge and a little past it. */
    private fun paintGlow(): Bitmap {
        val far = 1.5f * housing
        val size = ceil(2f * far).toInt()
        val mid = size / 2f
        val hot = if (dark) k.colour(0.5f, lightness = 1.45f, chroma = 0.3f) else k.colour(0.5f, chroma = 0.2f, atLightness = 0.995f)
        val tint = if (dark) k.colour(0.5f, lightness = 1.05f, chroma = 0.9f) else k.colour(0.5f, chroma = 0.7f, atLightness = 0.88f)
        return sprite(size, size) { x, y, px ->
            val r = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)) / housing
            val core = exp(-(r / 0.22f).let { it * it }) * 1.4f
            val body = exp(-(r / 0.6f).let { it * it }) * 0.55f + exp(-r / 0.45f) * 0.2f
            val fade = 1f - Shade.smoothstep(1.2f, 1.5f, r)
            if (dark) {
                for (ch in 0..2) px[ch] = (hot[ch] * core + tint[ch] * body) * fade
                px[3] = 1f
            } else {
                val a = Shade.clamp01((core + body) * fade)
                val share = if (a > 0f) Shade.clamp01(core * fade / a) else 0f
                for (ch in 0..2) px[ch] = tint[ch] + (hot[ch] - tint[ch]) * share
                px[3] = a
            }
        }
    }

    /** Rays of light pouring out of the iris, painted at half size. */
    private fun paintRays(): Bitmap {
        val far = 0.62f * w
        val size = ceil(far).toInt()
        val mid = size / 2f
        val tint = if (dark) k.colour(0.5f, lightness = 1.15f, chroma = 0.6f) else k.colour(0.5f, chroma = 0.35f, atLightness = 0.99f)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) * 2f / housing
            val dy = (y - mid) * 2f / housing
            val r = sqrt(dx * dx + dy * dy)
            val phi = atan2(dy, dx)
            val streaks = 0.1f + 0.9f * (0.5f + 0.5f * sin(phi * 11f + 1.3f * sin(phi * 3f))).pow(6f) *
                (0.55f + 0.45f * LiveNoise.value(phi * 9f + 20f, 1.7f))
            val g = streaks * exp(-(max(0f, r - 0.75f) / 0.42f).let { it * it }) * Shade.smoothstep(0.4f, 0.85f, r)
            if (dark) {
                for (ch in 0..2) px[ch] = tint[ch] * g * 0.55f
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = tint[ch]
                px[3] = Shade.clamp01(g * 0.6f)
            }
        }
    }

    /** The lens's barrel round the iris: a rounded lip, a knurled band to turn it by, lit from the top left. */
    private fun paintBezel(): Bitmap {
        val inner = 0.97f * housing
        val outer = BEZEL * housing
        val size = ceil(2f * outer + 4f).toInt()
        val mid = size / 2f
        val metal = if (dark) k.colour(0.5f, lightness = 0.5f, chroma = 0.3f) else k.colour(0.5f, chroma = 0.08f, atLightness = 0.88f)
        val shine = if (dark) k.colour(0.5f, lightness = 1.2f, chroma = 0.25f) else k.colour(0.5f, chroma = 0.03f, atLightness = 0.995f)
        val deep = if (dark) FloatArray(3) { k.deepLight[it] * 0.5f } else k.colour(0.5f, chroma = 0.15f, atLightness = 0.62f)
        val light = Shade.unit(-0.5f, -0.7f, 0.6f)
        val hv = Shade.half(light)
        return sprite(size, size) { x, y, px ->
            val dx = x - mid
            val dy = y - mid
            val r = sqrt(dx * dx + dy * dy)
            if (r < inner - 1.5f || r > outer + 1.5f) {
                px[3] = 0f
                return@sprite
            }
            val cover = Shade.clamp01(r - inner + 0.5f) * Shade.clamp01(outer - r + 0.5f)
            val u = ((r - inner) / (outer - inner)).coerceIn(0f, 1f)
            val cr = dx / max(r, 1f)
            val sr = dy / max(r, 1f)
            // The slope of its surface outwards: up over the lip, flat across the band, round over the edge.
            val slope = when {
                u < 0.22f -> 1.6f * (1f - u / 0.11f).coerceIn(-1f, 1f)
                u > 0.86f -> -1.8f * ((u - 0.86f) / 0.14f)
                else -> 0f
            }
            // Fine ridges round the band, to grip it by.
            val knurl = if (u in 0.36f..0.8f) 0.35f * sin(atan2(dy, dx) * 160f) else 0f
            var nx = -slope * cr - knurl * sr
            var ny = -slope * sr + knurl * cr
            val l = 1f / sqrt(nx * nx + ny * ny + 1f)
            nx *= l
            ny *= l
            val ndl = Shade.clamp01(nx * light[0] + ny * light[1] + l * light[2])
            val spec = Shade.pow16(Shade.clamp01(nx * hv[0] + ny * hv[1] + l * hv[2]))
            val groove = exp(-((u - 0.27f) / 0.03f).let { it * it }) + exp(-((u - 0.83f) / 0.025f).let { it * it })
            for (ch in 0..2) {
                val col = deep[ch] + (metal[ch] - deep[ch]) * ndl
                px[ch] = (col + (shine[ch] - col) * spec * 0.7f) * (1f - 0.45f * groove)
            }
            px[3] = cover
        }
    }

    /** Where the iris's opening and its blades' edges are, [open] of the way open, into [vx], [vy], [ex] and [ey]. */
    private fun lay(open: Float): Float {
        val sector = TAU / BLADES
        val inradius = housing * lerp(PINHOLE, WIDE, open)
        val turn = TURN * open
        val corner = inradius / cos(sector / 2f)
        for (i in 0..BLADES) {
            val a = turn + (i + 0.5f) * sector
            vx[i] = cx + corner * cos(a)
            vy[i] = cy + corner * sin(a)
        }
        // Each side of the opening carried on out to the barrel: the edge of the blade on top.
        for (i in 0 until BLADES) {
            val m = turn + (i + 1f) * sector
            val dx = -sin(m)
            val dy = cos(m)
            val px = vx[i + 1] - cx
            val py = vy[i + 1] - cy
            val b = px * dx + py * dy
            val t = -b + sqrt(max(0f, b * b - (px * px + py * py - reach * reach)))
            ex[i] = vx[i + 1] + t * dx
            ey[i] = vy[i + 1] + t * dy
        }
        return turn
    }

    /** Blade [j]'s outline into [path]: its inner edge, the edge of the blade over it, the barrel, its own edge carried on. */
    private fun outline(j: Int) {
        val before = (j + BLADES - 1) % BLADES
        path.rewind()
        path.moveTo(vx[j], vy[j])
        path.lineTo(ex[before], ey[before])
        val start = atan2(ey[before] - cy, ex[before] - cx) * DEGREES
        val end = atan2(ey[j] - cy, ex[j] - cx) * DEGREES
        val sweep = ((end - start) % 360f + 360f) % 360f
        path.arcTo(oval, start, sweep, false)
        path.lineTo(vx[j + 1], vy[j + 1])
        path.close()
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        val x = open.coerceIn(0f, 1f)
        drawField(canvas, back, paint)
        drawCentred(canvas, lens, cx, cy, 1f, paint)
        // The light within: a pin of it through the shut iris, all of it once open.
        drawCentred(canvas, glow, cx, cy, 1f, glowPaint, 0.4f + 0.6f * ease(0.05f, 0.7f, x))
        val turn = lay(LiveCurves.spring(x, 0.8f))
        val sector = TAU / BLADES
        for (j in 0 until BLADES) {
            outline(j)
            // Lit from its inner edge outwards.
            val m = turn + (j + 1f) * sector
            local.setRotate(m * DEGREES - 90f)
            local.postTranslate((vx[j] + vx[j + 1]) / 2f, (vy[j] + vy[j + 1]) / 2f)
            faces[j].setLocalMatrix(local)
            bladePaint.shader = faces[j]
            canvas.drawPath(path, bladePaint)
            // The shadow of the blade over it, along the edge it lies under.
            val over = turn + j * sector
            local.setScale(SHADOW * w, SHADOW * w)
            local.postRotate(over * DEGREES + 90f)
            local.postTranslate(vx[j], vy[j])
            shades[j].setLocalMatrix(local)
            shadePaint.shader = shades[j]
            canvas.drawPath(path, shadePaint)
        }
        for (i in 0 until BLADES) canvas.drawLine(vx[i], vy[i], ex[i], ey[i], edgePaint)
        drawCentred(canvas, bezel, cx, cy, 1f, paint)
        // Light pouring out as it opens, settling to a glow.
        val pour = ease(0.08f, 0.4f, x) * (0.35f + 0.65f * hump(x, 0.45f, 0.22f))
        if (pour > 0.004f) drawCentred(canvas, rays, cx, cy, 2f, glowPaint, pour)
        settle(canvas, 0.64f, if (dark) 0.3f else 0.24f)
    }

    private companion object {
        const val BLADES = 7

        /** The iris's radius, of the width, and its barrel's, of that. */
        const val HOUSING = 0.36f
        const val BEZEL = 1.17f

        /** How wide the opening is, of the iris's radius, shut and open, and how far the blades turn as it opens. */
        const val PINHOLE = 0.035f
        const val WIDE = 0.72f
        const val TURN = 0.6f

        /** How far a blade's shadow reaches over the one under it, of the width. */
        const val SHADOW = 0.035f
    }
}

// ------------------------------------------------------------------------------------------ Gyro

/**
 * Gyro: four rings of glass round a glowing core, tipped every which way on the lock screen like a
 * gyroscope's. Unlocked, they swing round into line, the inner ones first, rock past their place
 * and settle, nested round the core and seen a little from above, like an orrery's; the core
 * flares as the last one comes to rest. Each ring is painted once face on; tipping it is squashing
 * it along its hinge.
 */
internal class GyroScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    override val openMs: Long = 1700L

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val ringPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val cx = 0.5f * w
    private val cy = 0.4f * h
    private val tilt = FloatArray(RADII.size)
    private val axis = FloatArray(RADII.size)
    private val spin = FloatArray(RADII.size)
    private lateinit var back: Bitmap
    private lateinit var rings: Array<Bitmap>
    private lateinit var core: Bitmap
    private lateinit var halo: Bitmap
    private lateinit var glint: Bitmap

    override fun prepare() {
        val glowColour = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.55f, atLightness = 0.92f)
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v * 0.8f, px, 0)
            val gx = (x - cx) / w
            val gy = (y - cy) / w
            Shade.mixInto(px, glowColour[0], glowColour[1], glowColour[2], exp(-(gx * gx + gy * gy) / 0.25f) * (if (dark) 0.12f else 0.28f))
        }
        rings = Array(RADII.size) { ring(it) }
        core = paintCore()
        halo = paintHalo()
        glint = paintGlint()
    }

    /**
     * Ring [j] face on: a rod of glass bent round, its edges brighter than its middle as glass is,
     * the light from the top left caught along it; at night it glows, by day it is clear and tinted.
     */
    private fun ring(j: Int): Bitmap {
        val r = RADII[j] * w
        val t = TUBE[j] * w
        val margin = if (dark) 2.5f * t else 0.5f * t
        val size = ceil(2f * (r + t + margin) + 4f).toInt()
        val mid = size / 2f
        val hue = HUES[j]
        val body = if (dark) k.colour(hue, lightness = 1.05f, chroma = 0.95f) else k.colour(hue, chroma = 0.85f, atLightness = 0.66f)
        val edge = if (dark) k.colour(hue, lightness = 1.35f, chroma = 0.5f) else k.colour(hue, chroma = 0.9f, atLightness = 0.5f)
        val white = k.colour(0.5f, chroma = 0.05f, atLightness = 1f)
        val light = Shade.unit(-0.55f, -0.7f, 0.45f)
        val hv = Shade.half(light)
        return sprite(size, size) { x, y, px ->
            val dx = x - mid
            val dy = y - mid
            val rr = sqrt(dx * dx + dy * dy)
            val u = (rr - r) / t
            val outside = abs(u) - 1f
            if (outside > margin / t) {
                px[3] = 0f
                return@sprite
            }
            val inside = Shade.clamp01(0.5f - outside * t)
            val uc = u.coerceIn(-1f, 1f)
            val nz = sqrt(max(0f, 1f - uc * uc))
            val cr = dx / max(rr, 1f)
            val sr = dy / max(rr, 1f)
            val nx = uc * cr
            val ny = uc * sr
            val spec = Shade.pow64(Shade.clamp01(nx * hv[0] + ny * hv[1] + nz * hv[2]))
            val fresnel = (1f - nz).let { it * it }
            if (dark) {
                val halo = exp(-max(0f, outside) * t / (0.9f * t)) * 0.18f * (1f - inside)
                for (ch in 0..2) {
                    px[ch] = (body[ch] * (0.28f + 0.3f * nz) + edge[ch] * fresnel * 0.9f + white[ch] * spec * 0.9f) * inside + body[ch] * halo
                }
                px[3] = 1f
            } else {
                val a = (0.3f + 0.55f * fresnel + 0.6f * spec) * inside
                for (ch in 0..2) px[ch] = body[ch] + (edge[ch] - body[ch]) * fresnel + (white[ch] - body[ch]) * spec
                px[3] = Shade.clamp01(a)
            }
        }
    }

    /** The core: a small sun of the colour set's light, hot at its heart. */
    private fun paintCore(): Bitmap {
        val r = CORE * w
        val size = ceil(2f * r + 4f).toInt()
        val mid = size / 2f
        val hot = if (dark) k.colour(0.5f, lightness = 1.5f, chroma = 0.25f) else k.colour(0.5f, chroma = 0.15f, atLightness = 0.99f)
        val rim = if (dark) k.colour(0.5f, lightness = 1.1f, chroma = 1f) else k.colour(0.5f, chroma = 1f, atLightness = 0.66f)
        return sprite(size, size) { x, y, px ->
            val d = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid))
            val cover = Shade.clamp01(r - d + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val q = min(d / r, 1f)
            val mu = sqrt(max(0f, 1f - q * q))
            for (ch in 0..2) px[ch] = rim[ch] + (hot[ch] - rim[ch]) * mu.pow(0.7f)
            px[3] = cover
        }
    }

    /** The core's light round it, painted at half size. */
    private fun paintHalo(): Bitmap {
        val far = 0.36f * w
        val size = ceil(far).toInt()
        val mid = size / 2f
        val colour = if (dark) k.colour(0.5f, lightness = 1.1f, chroma = 0.85f) else k.colour(0.5f, chroma = 0.6f, atLightness = 0.93f)
        return sprite(size, size) { x, y, px ->
            val r = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)) * 2f / w
            val g = (exp(-(r / 0.07f).let { it * it }) * 0.8f + exp(-r / 0.09f) * 0.4f) * (1f - Shade.smoothstep(0.26f, 0.36f, r))
            if (dark) {
                for (ch in 0..2) px[ch] = colour[ch] * g * 0.6f
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = colour[ch]
                px[3] = Shade.clamp01(g * 0.8f)
            }
        }
    }

    /** A glint of light caught on a ring: a bright point with four fine rays. */
    private fun paintGlint(): Bitmap {
        val far = 0.05f * w
        val size = ceil(2f * far).toInt()
        val mid = size / 2f
        val hot = k.colour(0.5f, chroma = 0.1f, atLightness = 1f)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / far
            val dy = (y - mid) / far
            val r = sqrt(dx * dx + dy * dy)
            val g = (exp(-(r / 0.09f).let { it * it }) * 1.6f + exp(-abs(dy) / 0.025f) * exp(-abs(dx) / 0.45f) * 0.7f +
                exp(-abs(dx) / 0.025f) * exp(-abs(dy) / 0.45f) * 0.7f) * (1f - Shade.smoothstep(0.75f, 1f, r))
            if (dark) {
                for (ch in 0..2) px[ch] = hot[ch] * g
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = hot[ch]
                px[3] = Shade.clamp01(g)
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        val x = open.coerceIn(0f, 1f)
        drawField(canvas, back, paint)
        var settled = 1f
        for (j in RADII.indices) {
            val p = ((x - DELAY * j) / SPAN).coerceIn(0f, 1f)
            val s = LiveCurves.spring(p, 0.5f)
            tilt[j] = lerp(TILT[j], REST, s)
            axis[j] = lerp(AXIS[j], HINGE, LiveCurves.easeInOut(p))
            spin[j] = GLINT[j] + 200f * LiveCurves.easeInOut(p)
            settled = min(settled, p)
        }
        // The core flares as the last ring comes to rest.
        val flare = hump(x, 0.86f, 0.12f)
        drawCentred(canvas, halo, cx, cy, 2f * (1f + 0.25f * flare), glowPaint, 0.55f + 0.3f * x + 0.15f * flare)
        // The far halves of the rings, outside in, then the core, then the near halves, inside out.
        for (j in RADII.indices.reversed()) half(canvas, j, near = false)
        drawCentred(canvas, core, cx, cy, 1f + 0.18f * flare, paint)
        for (j in RADII.indices) half(canvas, j, near = true)
        for (j in RADII.indices) {
            val a = axis[j] / DEGREES
            val g = spin[j] / DEGREES
            val r = RADII[j] * w
            val px = r * cos(g)
            val py = r * sin(g)
            val x1 = px * cos(a) + py * sin(a)
            val y1 = (-px * sin(a) + py * cos(a)) * cos(tilt[j] / DEGREES)
            drawCentred(canvas, glint, cx + x1 * cos(a) - y1 * sin(a), cy + x1 * sin(a) + y1 * cos(a), 1f, glowPaint, 0.75f)
        }
        settle(canvas, 0.64f, if (dark) 0.3f else 0.24f)
    }

    /** Ring [j]'s half on the [near] side of the core, or the far one, as tipped now. */
    private fun half(canvas: Canvas, j: Int, near: Boolean) {
        val ring = rings[j]
        val mid = ring.width / 2f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(axis[j])
        canvas.scale(1f, cos(tilt[j] / DEGREES))
        // Tipped one way the top half comes towards us, the other way the bottom half.
        val top = near == (tilt[j] >= 0f)
        if (top) canvas.clipRect(-mid, -mid, mid, 0f) else canvas.clipRect(-mid, 0f, mid, mid)
        canvas.rotate(-axis[j])
        canvas.drawBitmap(ring, -mid, -mid, ringPaint)
        canvas.restore()
    }

    private companion object {
        /** Each ring, inside out: its radius and how thick its glass is, of the width; its colour on the spectrum. */
        val RADII = floatArrayOf(0.16f, 0.25f, 0.34f, 0.43f)
        val TUBE = floatArrayOf(0.016f, 0.018f, 0.02f, 0.022f)
        val HUES = floatArrayOf(0.1f, 0.4f, 0.7f, 0.95f)

        /** How far each is tipped on the lock screen, in degrees, about a hinge at what angle, and where its glint starts. */
        val TILT = floatArrayOf(72f, 58f, 80f, 65f)
        val AXIS = floatArrayOf(20f, 110f, 65f, 150f)
        val GLINT = floatArrayOf(200f, 30f, 290f, 120f)

        /**
         * How far they are all tipped at rest, in one plane, about one level hinge: turned half
         * round, so the near half of each is its lower one, as rings seen from above.
         */
        const val REST = 52f
        const val HINGE = 180f

        /** How much later each ring starts than the one inside it, and how long it takes, of the opening. */
        const val DELAY = 0.07f
        const val SPAN = 0.72f

        /** The core's radius, of the width. */
        const val CORE = 0.065f
    }
}

// ------------------------------------------------------------------------------------------- Ink

/**
 * Ink: a single drop of colour resting on still water on the lock screen. Unlocked, it bursts: a
 * ripple runs out, a cloud of colour billows up where it fell and tendrils of ink reach out of it,
 * curling over at their tips and slowly unwinding as they spread, until they hang there, a flower
 * of ink. At night the ink glows; by day it is ink in milk.
 */
internal class InkScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    override val openMs: Long = 2000L

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val inkPaint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val matrix = Matrix()
    private val cx = 0.5f * w
    private val cy = 0.4f * h
    private lateinit var back: Bitmap
    private lateinit var plumes: Array<Bitmap>
    private lateinit var cloud: Bitmap
    private lateinit var drop: Bitmap
    private lateinit var shadow: Bitmap
    private lateinit var ripple: Bitmap

    override fun prepare() {
        val glowColour = if (dark) k.colour(0.5f, lightness = 0.85f) else k.colour(0.5f, chroma = 0.4f, atLightness = 0.95f)
        val ring = if (dark) k.colour(0.5f, lightness = 0.75f, chroma = 0.6f) else k.colour(0.5f, chroma = 0.4f, atLightness = 0.82f)
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v * 0.7f, px, 0)
            val gx = (x - cx) / w
            val gy = (y - cy) / w
            val d = sqrt(gx * gx + gy * gy)
            Shade.mixInto(px, glowColour[0], glowColour[1], glowColour[2], exp(-d * d / 0.2f) * (if (dark) 0.12f else 0.3f))
            // Faint rings on the still water round the drop.
            var rings = 0f
            for (r in RINGS) rings += exp(-((d - r) / 0.004f).let { it * it }) * (1f - r * 1.6f)
            Shade.mixInto(px, ring[0], ring[1], ring[2], Shade.clamp01(rings) * (if (dark) 0.1f else 0.18f))
        }
        plumes = Array(CURL.size) { plume(it) }
        cloud = paintCloud()
        drop = paintDrop()
        shadow = paintShadow()
        ripple = paintRipple()
    }

    /** The ink's colours along the colour set's spectrum: its body, and where it lies thick. */
    private fun inks(): Pair<Ramp, Ramp> =
        if (dark) k.ramp(1.18f, 1.3f) to k.ramp(0.95f, 1.3f) else k.rampAt(0.62f, 1.05f) to k.rampAt(0.48f, 1.1f)

    /**
     * [density] of ink, of colours px 4..6 (its body) and 7..9 (where it lies thick), into px
     * 0..3, as solid as [solid] at its thickest. At night the ink is bright, by day deep.
     */
    private fun shadeInk(density: Float, px: FloatArray, solid: Float) {
        val thick = Shade.smoothstep(0.35f, 0.9f, density)
        for (ch in 0..2) px[ch] = px[4 + ch] + (px[7 + ch] - px[4 + ch]) * thick * (if (dark) 0.4f else 0.6f)
        px[3] = Shade.clamp01((1f - exp(-1.9f * density)) * solid)
    }

    /**
     * Tendril of ink [j], painted at [PLUME_RES] of its size with its root in the middle of its
     * layer: it reaches out to the right, widening as it goes and curling over towards its tip,
     * thickest along its middle and fading softly to its sides, drawn out into fine streaks along
     * its length, its edges fraying into wisps.
     */
    private fun plume(j: Int): Bitmap {
        val length = PLUME * w * PLUME_RES
        val size = ceil(2f * length + 4f).toInt()
        val mid = size / 2f
        val s = j * 5.3f + 1.1f
        val curl = CURL[j]
        val t0 = HUES[j]
        val (body, thick) = inks()
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / length
            val dy = (y - mid) / length
            val rho = sqrt(dx * dx + dy * dy)
            if (rho >= 1f) {
                px[3] = 0f
                return@sprite
            }
            // How far round it is from the tendril's middle line, which bends more the further out it is.
            var off = atan2(dy, dx) - curl * rho * rho
            off -= TAU * kotlin.math.round(off / TAU)
            val billow = 0.75f + 0.5f * LiveNoise.value(rho * 4f + s, 0.5f * s)
            val width = (0.035f + 0.26f * rho.pow(0.8f)) * billow
            val fray = (LiveNoise.fbm(rho * 6f + s, off * 2.5f + s) - 0.5f) * 1.1f
            val a = rho * off / width + fray
            val core = exp(-a * a * 1.2f)
            val streak = Shade.smoothstep(0.32f, 0.68f, LiveNoise.fbm(rho * 2f + 2f * s, a * 1.6f - s))
            val tip = 1f - Shade.smoothstep(0.62f, 1f, rho)
            val density = core * (0.4f + 0.75f * streak) * tip * (1.15f - 0.5f * rho)
            // Its colour drifting along the spectrum as it reaches out, never round past its end.
            val t = (t0 + 0.18f * rho + 0.12f * (streak - 0.5f)).coerceIn(0f, 1f)
            body.get(t, px, 4)
            thick.get(t, px, 7)
            shadeInk(density, px, if (dark) 0.92f else 0.85f)
        }
    }

    /** The cloud that billows up where the drop fell: round, lumpy, soft at its edge; painted at [PLUME_RES]. */
    private fun paintCloud(): Bitmap {
        val far = CLOUD * w * PLUME_RES
        val size = ceil(2.6f * far + 4f).toInt()
        val mid = size / 2f
        val (body, thick) = inks()
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / far
            val dy = (y - mid) / far
            val r = sqrt(dx * dx + dy * dy)
            if (r >= 1.3f) {
                px[3] = 0f
                return@sprite
            }
            // Its edge in lobes, each a billow.
            val lobes = 1f + 0.36f * (LiveNoise.valueTiled((atan2(dy, dx) / TAU + 1f) * 9f, 2.7f, 9) - 0.5f)
            val q = r / lobes
            val lumps = LiveNoise.fbm(dx * 2.6f + 7.7f, dy * 2.6f + 1.3f)
            val density = exp(-q * q * 1.7f) * (0.5f + 0.8f * lumps) * (1f - Shade.smoothstep(0.95f, 1.3f, r))
            val t = (0.5f + 0.5f * (lumps - 0.5f) + 0.12f * r).coerceIn(0f, 1f)
            body.get(t, px, 4)
            thick.get(t, px, 7)
            shadeInk(density, px, if (dark) 0.7f else 0.55f)
        }
    }

    /** The drop: a bead of the colour set's colour, a bright gleam at its top left and light gathered at its foot. */
    private fun paintDrop(): Bitmap {
        val r = DROP * w
        val size = ceil(2.2f * r + 4f).toInt()
        val mid = size / 2f
        val body = if (dark) k.colour(0.3f, lightness = 0.95f, chroma = 1.05f) else k.colour(0.3f, chroma = 1f, atLightness = 0.55f)
        val deep = if (dark) k.colour(0.7f, lightness = 0.55f, chroma = 1f) else k.colour(0.7f, chroma = 1f, atLightness = 0.36f)
        val gleam = k.colour(0.5f, chroma = 0.05f, atLightness = 1f)
        val light = Shade.unit(-0.5f, -0.75f, 0.45f)
        val hv = Shade.half(light)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / r
            // A little fuller at its foot, as a drop sits.
            val dy = (y - mid) / r * (1f - 0.08f * ((y - mid) / r))
            val q = dx * dx + dy * dy
            val cover = Shade.clamp01((1f - sqrt(q)) * r + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val nz = sqrt(max(0f, 1f - q))
            val ndl = dx * light[0] + dy * light[1] + nz * light[2]
            val spec = Shade.pow64(Shade.clamp01(dx * hv[0] + dy * hv[1] + nz * hv[2]))
            val caustic = exp(-((dx - 0.22f) * (dx - 0.22f) + (dy - 0.5f) * (dy - 0.5f)) / 0.06f) * 0.55f
            for (ch in 0..2) {
                val col = deep[ch] + (body[ch] - deep[ch]) * Shade.clamp01(0.4f + 0.6f * ndl)
                px[ch] = col + (gleam[ch] - col) * (spec * 0.95f) + body[ch] * caustic
            }
            px[3] = cover
        }
    }

    /** The drop's soft shadow on the water. */
    private fun paintShadow(): Bitmap {
        val r = DROP * w
        val across = ceil(3f * r).toInt()
        val down = ceil(1.4f * r).toInt()
        val shade = if (dark) FloatArray(3) { k.deepLight[it] * 0.3f } else k.colour(0.5f, chroma = 0.5f, atLightness = 0.5f)
        return sprite(across, down) { x, y, px ->
            val dx = (x / across - 0.5f) * 2f
            val dy = (y / down - 0.5f) * 2f
            for (ch in 0..2) px[ch] = shade[ch]
            px[3] = exp(-(dx * dx + dy * dy) / 0.25f) * (if (dark) 0.6f else 0.3f)
        }
    }

    /** The ripple that runs out as the drop bursts: a fine bright ring with a soft shadow inside it. */
    private fun paintRipple(): Bitmap {
        val r = RIPPLE * w
        val size = ceil(2f * r * 1.08f + 4f).toInt()
        val mid = size / 2f
        val bright = if (dark) k.colour(0.5f, lightness = 1.2f, chroma = 0.5f) else k.colour(0.5f, chroma = 0.1f, atLightness = 0.995f)
        val line = 0.006f * w
        return sprite(size, size) { x, y, px ->
            val d = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid))
            val g = exp(-((d - r) / line).let { it * it }) + 0.4f * exp(-((d - r * 0.93f) / (2.5f * line)).let { it * it })
            if (dark) {
                for (ch in 0..2) px[ch] = bright[ch] * g * 0.6f
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = bright[ch]
                px[3] = Shade.clamp01(g * 0.8f)
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        val x = open.coerceIn(0f, 1f)
        drawField(canvas, back, paint)
        // The ripple running out as the drop bursts.
        val run = LiveCurves.easeOut(ease(0.03f, 0.75f, x))
        val rippleAlpha = ease(0.02f, 0.08f, x) * (1f - ease(0.2f, 0.75f, x))
        if (rippleAlpha > 0.004f) drawCentred(canvas, ripple, cx, cy, lerp(DROP / RIPPLE, 1f, run), glowPaint, rippleAlpha)
        // The tendrils, each reaching out of the middle and unwinding as it goes.
        for (i in SPREAD.indices) {
            val cfg = SPREAD[i]
            val p = ((x - 0.05f - 0.03f * i) / 0.78f).coerceIn(0f, 1f)
            if (p <= 0f) continue
            val g = LiveCurves.easeOut(p)
            val plume = plumes[cfg[0].toInt()]
            val angle = cfg[1] + cfg[4] * (1f - g)
            val root = cfg[2] * w * g
            val a = angle / DEGREES
            val size = lerp(0.08f, 1f, g) * cfg[3] / PLUME_RES
            matrix.setTranslate(-plume.width / 2f, -plume.height / 2f)
            matrix.postScale(size, size)
            matrix.postRotate(angle)
            matrix.postTranslate(cx + root * cos(a), cy + root * sin(a))
            inkPaint.alpha = (ease(0f, 0.15f, p) * cfg[5] * 255f).toInt()
            canvas.drawBitmap(plume, matrix, inkPaint)
        }
        inkPaint.alpha = 255
        // The cloud where it fell, over the tendrils' roots.
        val billow = LiveCurves.easeOut(ease(0.03f, 0.55f, x))
        if (x > 0.03f) drawCentred(canvas, cloud, cx, cy, lerp(0.2f, 1f, billow) / PLUME_RES, inkPaint, ease(0.03f, 0.12f, x))
        // The drop itself, squashing as it bursts and gone at once.
        val gone = ease(0.06f, 0.18f, x)
        if (gone < 1f) {
            val squash = ease(0f, 0.1f, x)
            drawCentred(canvas, shadow, cx + 0.15f * DROP * w, cy + 0.85f * DROP * w, 1f + 0.4f * squash, paint, 1f - gone)
            val r = drop.width / 2f
            matrix.setTranslate(-r, -r)
            matrix.postScale(1f + 0.35f * squash, 1f - 0.3f * squash)
            matrix.postTranslate(cx, cy + 0.3f * squash * DROP * w)
            paint.alpha = ((1f - gone) * 255f).toInt()
            canvas.drawBitmap(drop, matrix, paint)
            paint.alpha = 255
        }
        settle(canvas, 0.64f, if (dark) 0.3f else 0.24f)
    }

    private companion object {
        /** How long a tendril is, of the width; the cloud's radius; how finely they are painted. */
        const val PLUME = 0.42f
        const val CLOUD = 0.11f
        const val PLUME_RES = 0.55f

        /** Each kind of tendril: how far it curls over by its tip, in radians, which way; its colour on the spectrum. */
        val CURL = floatArrayOf(1.2f, -1f, 1.5f, -1.3f, 0.9f)
        val HUES = floatArrayOf(0.1f, 0.35f, 0.6f, 0.78f, 0.5f)

        /** The drop's radius and the ripple's widest, of the width. */
        const val DROP = 0.05f
        const val RIPPLE = 0.5f

        /** The faint rings on the water, of the width from the drop. */
        val RINGS = floatArrayOf(0.12f, 0.2f, 0.29f)

        /**
         * Each tendril: which kind; which way it reaches, in degrees; how far from the middle its
         * root is, of the width; how long it is; how far it turns as it unwinds, in degrees, against
         * its curl; how clearly it shows.
         */
        val SPREAD = arrayOf(
            floatArrayOf(0f, -95f, 0.01f, 1f, -50f, 0.9f),
            floatArrayOf(1f, -52f, 0.008f, 0.85f, 45f, 0.85f),
            floatArrayOf(2f, -10f, 0.012f, 1.05f, -55f, 0.9f),
            floatArrayOf(3f, 30f, 0.01f, 0.8f, 50f, 0.85f),
            floatArrayOf(4f, 72f, 0.008f, 0.95f, -45f, 0.8f),
            floatArrayOf(0f, 118f, 0.01f, 0.9f, -55f, 0.85f),
            floatArrayOf(1f, 160f, 0.012f, 1f, 50f, 0.9f),
            floatArrayOf(2f, 205f, 0.01f, 0.85f, -45f, 0.85f),
            floatArrayOf(3f, 248f, 0.008f, 0.95f, 55f, 0.85f),
        )
    }
}
