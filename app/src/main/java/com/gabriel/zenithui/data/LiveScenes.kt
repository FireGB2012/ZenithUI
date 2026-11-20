package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
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
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * The first five designs that move; the next ten are in LiveScenesDrift and LiveScenesUnlock. Each
 * paints its layers once, lit as the Depth designs are (light added as light, colours run round the
 * spectrum between the set's two accents), and then only moves, turns and fades them from frame to
 * frame. The light picture is drawn for a pale page, not washed out from the dark one, and the
 * lower part of both is kept quiet for the icons.
 */

private const val TAU = (2.0 * PI).toFloat()
private const val TAU_D = 2.0 * PI

/** A number from 0 to 1 that is always the same for [a] and [b]: chance without a seed to keep. */
private fun hash01(a: Int, b: Int): Float {
    var n = a * 374761393 + b * 668265263
    n = (n xor (n ushr 13)) * 1274126177
    n = n xor (n ushr 16)
    return (n and 0xFFFFFF) / 16777216f
}

/** A smooth hill-and-dale of numbers from 0 to 1 across the plane, one hill or so to a unit. */
private fun valueNoise(x: Float, y: Float): Float {
    val xf = floor(x)
    val yf = floor(y)
    val xi = xf.toInt()
    val yi = yf.toInt()
    val fx = x - xf
    val fy = y - yf
    val sx = fx * fx * (3f - 2f * fx)
    val sy = fy * fy * (3f - 2f * fy)
    val a = hash01(xi, yi)
    val b = hash01(xi + 1, yi)
    val c = hash01(xi, yi + 1)
    val d = hash01(xi + 1, yi + 1)
    return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
}

/** Three [valueNoise]s at finer and finer sizes, 0 to 1: the lumpiness of clouds. */
private fun clouds(x: Float, y: Float): Float =
    (0.5714f * valueNoise(x, y) + 0.2857f * valueNoise(2.03f * x + 5.2f, 2.03f * y + 1.3f) + 0.1429f * valueNoise(4.1f * x + 1.7f, 4.1f * y + 9.2f))

/** [Shade.smoothstep] that may run down as well as up. */
private fun ease(edge0: Float, edge1: Float, x: Float): Float = Shade.smoothstep(edge0, edge1, x)

// ----------------------------------------------------------------------------------------- Hover

/**
 * Hover: glass marbles hanging in a glowing haze, each drifting on a slow loop of its own, the
 * near ones further than the far, as two pools of colour wander behind them. Each marble is lit
 * once, as the Marbles design lights them, and only moved after that.
 */
internal class HoverScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val poolPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val poolMatrix = Matrix()
    private val poolRadius = 0.85f * w
    private val light = Shade.unit(-0.45f, -0.65f, 0.62f)
    private val half = Shade.half(light)
    private val awayX: Float
    private val awayY: Float
    private lateinit var back: Bitmap
    private lateinit var pools: Array<RadialGradient>
    private lateinit var orbs: Array<Bitmap>

    init {
        val l = sqrt(light[0] * light[0] + light[1] * light[1])
        awayX = -light[0] / l
        awayY = -light[1] / l
    }

    override fun prepare() {
        val glow = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.7f, atLightness = 0.9f)
        val glowStrength = if (dark) 0.12f else 0.3f
        back = field(4) { x, y, px ->
            k.sky(y / h, px, 0)
            val gx = (x - 0.5f * w) / w
            val gy = (y - 0.36f * h) / w
            Shade.mixInto(px, glow[0], glow[1], glow[2], exp(-(gx * gx + gy * gy) / 0.5f) * glowStrength)
        }
        val strength = if (dark) 0.22f else 0.14f
        pools = Array(POOLS.size) {
            val colour = if (it == 0) c.accent else c.accentAlt
            val a = (strength * 255f).toInt()
            RadialGradient(
                0f, 0f, poolRadius,
                intArrayOf(alpha(colour, a), alpha(colour, (a * 0.42f).toInt()), alpha(colour, 0)),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val lit = if (dark) k.ramp(1.05f, 1f) else k.rampAt(0.88f, 0.8f)
        val deep = if (dark) k.ramp(0.32f, 1.1f) else k.rampAt(0.62f, 1f)
        val rim = if (dark) k.ramp(1.1f, 0.9f) else k.rampAt(0.9f, 0.7f)
        orbs = Array(ORBS.size) { orb(it, lit, deep, rim) }
    }

    /** Marble [i], lit, with its soft edge and the haze it sits in as how much of it shows. */
    private fun orb(i: Int, lit: Ramp, deep: Ramp, rim: Ramp): Bitmap {
        val o = ORBS[i]
        val r = o[2] * w
        val depth = o[3]
        val blur = 0.5f + depth * 0.035f * w
        val size = ceil(2f * (r + blur) + 4f).toInt()
        val mid = size / 2f
        val sway = 0.25f * sin(i * 1.7f)
        val gloss = if (dark) 0.8f else 0.6f
        val rimStrength = if (dark) 0.55f else 0.35f
        val caustic = if (dark) 0.45f else 0.3f
        val clear = 1f - depth * (if (dark) 0.55f else 0.5f)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / r
            val dy = (y - mid) / r
            val d = sqrt(dx * dx + dy * dy)
            val cover = Shade.clamp01((1f - d) * r / blur + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
            } else {
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
                val spec = n32 * n8 * gloss * (1f - 0.7f * depth) + n8 * n4 * 0.12f
                val fresnel = (1f - nz).let { it * it * it } * rimStrength
                val t = Shade.clamp01(0.5f + 0.45f * (nx * awayX + ny * awayY) + sway)
                lit.get(t, px, 6)
                deep.get(t, px, 9)
                rim.get(1f - t, px, 12)
                val shade = wrap * sqrt(wrap)
                // Light gathered on the far side, as in a glass marble.
                val gx = dx - 0.42f
                val gy = dy - 0.52f
                val gather = exp(-(gx * gx + gy * gy) / 0.09f) * caustic
                for (ch in 0..2) px[ch] = px[9 + ch] + (px[6 + ch] - px[9 + ch]) * shade + px[6 + ch] * gather + px[12 + ch] * fresnel + spec
                px[3] = cover * clear
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, back, paint)
        for (i in POOLS.indices) {
            val p = POOLS[i]
            val x = w * (p[0] + p[2] * LiveCurves.swing(seconds, p[4].toDouble(), p[6].toDouble()))
            val y = h * (p[1] + p[3] * LiveCurves.swing(seconds, p[5].toDouble(), p[7].toDouble()))
            poolMatrix.setTranslate(x, y)
            pools[i].setLocalMatrix(poolMatrix)
            poolPaint.shader = pools[i]
            canvas.drawCircle(x, y, poolRadius, poolPaint)
        }
        for (i in ORBS.indices) {
            val o = ORBS[i]
            // The near ones drift further, as things nearer the eye seem to.
            val reach = w * (0.012f + 0.03f * (1f - o[3]))
            val dx = reach * LiveCurves.swing(seconds, o[4].toDouble(), o[5].toDouble())
            val dy = reach * 1.4f * LiveCurves.swing(seconds, o[6].toDouble(), o[7].toDouble())
            val b = orbs[i]
            canvas.drawBitmap(b, o[0] * w - b.width / 2f + dx, o[1] * h - b.height / 2f + dy, paint)
        }
        settle(canvas, 0.6f, if (dark) 0.35f else 0.3f)
    }

    private companion object {
        /**
         * Each marble: across, down, size (of the width), how far away (0 near to 1 far), and its
         * two loops, across and down: how many seconds each takes, and whether it sets off one way
         * or the other (0 or a half, so every loop is at rest at 0 seconds). The far ones first.
         */
        val ORBS = arrayOf(
            floatArrayOf(0.80f, 0.13f, 0.075f, 0.95f, 23f, 0f, 17f, 0.5f),
            floatArrayOf(0.12f, 0.36f, 0.055f, 0.92f, 19f, 0.5f, 26f, 0f),
            floatArrayOf(0.66f, 0.72f, 0.065f, 0.88f, 21f, 0f, 15f, 0f),
            floatArrayOf(0.27f, 0.16f, 0.12f, 0.60f, 17f, 0.5f, 22f, 0.5f),
            floatArrayOf(0.90f, 0.47f, 0.16f, 0.50f, 25f, 0f, 18f, 0.5f),
            floatArrayOf(0.13f, 0.62f, 0.17f, 0.40f, 20f, 0.5f, 14f, 0f),
            floatArrayOf(0.80f, 0.87f, 0.10f, 0.22f, 16f, 0.5f, 24f, 0.5f),
            floatArrayOf(0.40f, 0.81f, 0.08f, 0.30f, 18f, 0f, 27f, 0.5f),
            floatArrayOf(0.56f, 0.34f, 0.25f, 0.05f, 29f, 0f, 21f, 0f),
        )

        /** Each pool of colour: where it rests, how far it wanders across and down, its two loops in seconds, and their starts. */
        val POOLS = arrayOf(
            floatArrayOf(0.22f, 0.22f, 0.16f, 0.06f, 41f, 31f, 0f, 0.5f),
            floatArrayOf(0.82f, 0.58f, 0.14f, 0.07f, 37f, 47f, 0.5f, 0f),
        )
    }
}

// ------------------------------------------------------------------------------------------ Tide

/**
 * Tide: swells of colour rolling in towards the screen from a glowing horizon, each one behind the
 * next, the far ones small and slow, the near ones broad and quicker, each with a sheen on its crest
 * and a shadow on the swell behind it. A swell's colour is worked out once, down from its crest, and
 * each frame bends that strip to where the crest has rolled to.
 */
internal class TideScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val count = ROWS.size
    private val paint = layerPaint()
    private val skyFrom = Rect()
    private val skyTo = RectF()
    private val skirt = FloatArray(count)
    private val reach = FloatArray(count)
    private val verts = Array(count) { FloatArray((COLS + 1) * 4) }
    private lateinit var sky: Bitmap
    private lateinit var layers: Array<Bitmap>

    /** How far each crest can stray from its own height, in pixels. */
    private val stray = FloatArray(count) { j ->
        val r = ROWS[j]
        h * (abs(r[1]) + abs(r[4]) + abs(r[7]) * 0.5f + BOB * (0.5f + j / (count - 1f)))
    }

    override fun prepare() {
        val horizon = ROWS[0][0]
        val glow = if (dark) k.colour(0.5f, lightness = 1.05f, chroma = 0.8f) else k.colour(0.5f, chroma = 0.6f, atLightness = 0.95f)
        sky = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v / horizon * 0.85f, px, 0) else k.sky(v / horizon * 0.7f, px, 0)
            val band = ((v - horizon + 0.012f) / 0.07f).let { exp(-it * it) } * (if (dark) 0.24f else 0.3f)
            val gx = (x - 0.68f * w) / w
            val gy = (y - 0.15f * h) / w
            val moon = exp(-(gx * gx + gy * gy) / 0.08f) * (if (dark) 0.16f else 0.2f)
            Shade.mixInto(px, glow[0], glow[1], glow[2], Shade.clamp01(band + moon))
        }
        // Only the strip above the horizon ever shows.
        val down = ceil((horizon * h + stray[0] + 8f) / 4f).toInt().coerceAtMost(sky.height)
        skyFrom.set(0, 0, sky.width, down)
        skyTo.set(0f, 0f, sky.width * 4f, down * 4f)
        layers = Array(count) { swell(it) }
    }

    /**
     * Swell [j] as a strip one pixel wide, a pixel to a pixel down from just above its crest: the
     * shadow it throws on the one behind, its edge, its sheen, and its colour deepening below.
     */
    private fun swell(j: Int): Bitmap {
        val t = j / (count - 1f)
        // Nearer swells are bigger, as they would be seen over the water.
        val near = 0.3f + 0.7f * t
        val hue = 0.5f - 0.5f * cos(PI.toFloat() * (0.15f + 0.85f * t))
        val top = if (dark) k.colour(hue, lightness = 1f - 0.4f * t) else k.colour(hue, chroma = 0.95f - 0.5f * t, atLightness = 0.80f + 0.13f * t)
        val low = if (dark) k.colour(hue, lightness = 0.55f - 0.27f * t, chroma = 1.05f) else k.colour(hue, chroma = 0.7f - 0.4f * t, atLightness = 0.88f + 0.09f * t)
        val sheenWidth = 0.012f * h * near
        val sheen = if (dark) 0.10f else 0.08f
        val shadowWidth = 0.022f * h * near
        val shadow = if (dark) 0.55f else 0.22f
        val fall = 0.12f * h * near
        skirt[j] = ceil(10f * shadowWidth)
        // Down far enough to pass under the next swell however the two of them roll.
        val next = if (j < count - 1) ROWS[j + 1][0] * h + stray[j + 1] else h
        reach[j] = ceil(next - (ROWS[j][0] * h - stray[j]) + 6f)
        val edge = 1f - (1f - shadow).pow(1f / 2.2f)
        return sprite(1, (skirt[j] + reach[j]).toInt()) { _, y, px ->
            val d = y - skirt[j]
            if (d < -0.5f) {
                // Light is darkened as light: what takes away a share of it takes less of the screen's value.
                val keep = 1f - exp(d / shadowWidth) * shadow
                px[3] = 1f - keep.pow(1f / 2.2f)
            } else {
                val below = max(d, 0f)
                val f = 1f - exp(-below / fall)
                val s = exp(-below / sheenWidth) * sheen
                for (ch in 0..2) px[ch] = top[ch] + (low[ch] - top[ch]) * f + s
                val cover = Shade.clamp01(d + 0.5f)
                px[3] = cover + (1f - cover) * edge
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        canvas.drawBitmap(sky, skyFrom, skyTo, paint)
        val span = (w + 2f * EDGE) / COLS
        val lower = (COLS + 1) * 2
        for (j in 0 until count) {
            val r = ROWS[j]
            val a = ((r[3] + r[8] * seconds) % TAU_D).toFloat()
            val b = ((r[6] + r[9] * seconds) % TAU_D).toFloat()
            val bob = h * BOB * (0.5f + j / (count - 1f)) * LiveCurves.swing(seconds, 9.0 + 2.0 * j, if (j % 2 == 0) 0.0 else 0.5)
            val v = verts[j]
            for (i in 0..COLS) {
                val x = -EDGE + i * span
                val u = x / w
                val y = h * (r[0] + r[1] * sin(TAU * r[2] * u + a) + r[4] * sin(TAU * r[5] * u + b) + r[7] * (u - 0.5f)) + bob
                v[i * 2] = x
                v[i * 2 + 1] = y - skirt[j]
                v[lower + i * 2] = x
                v[lower + i * 2 + 1] = y + reach[j]
            }
            canvas.drawBitmapMesh(layers[j], COLS, 1, v, 0, null, 0, paint)
        }
    }

    private companion object {
        /** How many pieces a crest is drawn in across the screen. */
        const val COLS = 72

        /** How far past each side of the screen the swells reach, in pixels. */
        const val EDGE = 4f

        /** How far a swell bobs up and down, of the height, at most. */
        const val BOB = 0.003f

        /**
         * Each crest: its height down the screen; a long swell (size, how many across, where it
         * starts); a shorter one on it; a lean; and how fast each of the two rolls, in turns of a
         * radian a second, the short one against the long. Far ones first.
         */
        val ROWS = arrayOf(
            floatArrayOf(0.300f, 0.0050f, 2.60f, 0.40f, 0.0022f, 5.30f, 2.10f, 0.000f, 0.045f, -0.070f),
            floatArrayOf(0.336f, 0.0080f, 2.20f, 3.70f, 0.0032f, 4.70f, 0.90f, 0.012f, 0.055f, -0.085f),
            floatArrayOf(0.382f, 0.0125f, 1.85f, 1.30f, 0.0045f, 4.10f, 4.40f, -0.014f, 0.065f, -0.100f),
            floatArrayOf(0.440f, 0.0180f, 1.50f, 5.20f, 0.0065f, 3.60f, 2.70f, 0.018f, 0.075f, -0.115f),
            floatArrayOf(0.512f, 0.0250f, 1.24f, 2.40f, 0.0090f, 3.10f, 0.30f, -0.022f, 0.085f, -0.130f),
            floatArrayOf(0.600f, 0.0340f, 1.02f, 4.60f, 0.0115f, 2.60f, 5.50f, 0.026f, 0.095f, -0.145f),
            floatArrayOf(0.708f, 0.0430f, 0.86f, 0.90f, 0.0145f, 2.20f, 3.10f, -0.030f, 0.105f, -0.160f),
            floatArrayOf(0.840f, 0.0520f, 0.72f, 3.30f, 0.0175f, 1.90f, 1.60f, 0.034f, 0.115f, -0.175f),
        )
    }
}

// ---------------------------------------------------------------------------------------- Cosmos

/**
 * Cosmos: a spiral galaxy seen at a slant, turning slowly among stars that twinkle, two depths of
 * them drifting a little apart, and now and then a shooting star. By day it is drawn as ink on a
 * pale sky, without the meteors.
 */
internal class CosmosScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val matrix = Matrix()
    private val gx = 0.5f * w
    private val gy = 0.37f * h
    private val diameter = 1.05f * w
    private val stars = arrayOfNulls<FloatArray>(LAYERS * GROUPS)
    private val starColour = IntArray(LAYERS * GROUPS)
    private lateinit var space: Bitmap
    private lateinit var galaxy: Bitmap
    private lateinit var glare: Bitmap
    private lateinit var streak: Bitmap

    override fun prepare() {
        space = field(4) { x, y, px ->
            k.sky(y / h, px, 0)
            for (cl in CLOUDS) {
                val dx = (x - cl[0] * w) / w
                val dy = (y - cl[1] * h) / w
                val g = exp(-(dx * dx + dy * dy) / (cl[2] * cl[2])) * (0.45f + 0.9f * clouds(x / w * 3.2f + cl[3] * 7f, y / w * 3.2f))
                val col = cloudColour(cl[3])
                if (dark) {
                    val s = g * cl[4]
                    px[0] += col[0] * s
                    px[1] += col[1] * s
                    px[2] += col[2] * s
                } else {
                    Shade.mixInto(px, col[0], col[1], col[2], Shade.clamp01(g * cl[5]))
                }
            }
        }
        galaxy = paintGalaxy().also { it.setHasMipMap(true) }
        glare = paintGlare()
        streak = paintStreak()
        val random = Random(23)
        for (layer in 0 until LAYERS) {
            val n = if (layer == 0) (if (dark) 264 else 132) else (if (dark) 66 else 36)
            for (g in 0 until GROUPS) {
                val pts = FloatArray(n / GROUPS * 2)
                for (s in pts.indices step 2) {
                    pts[s] = random.nextFloat() * w
                    pts[s + 1] = random.nextFloat() * h
                }
                stars[layer * GROUPS + g] = pts
                val tint = when (g) {
                    0 -> Color.WHITE
                    1 -> mix(Color.WHITE, c.accent, 0.35f)
                    else -> mix(Color.WHITE, c.accentAlt, 0.35f)
                }
                starColour[layer * GROUPS + g] = if (dark) tint else mix(if (g == 2) c.accentAlt else c.accent, Color.BLACK, 0.35f)
            }
        }
    }

    private val cloudLight = HashMap<Float, FloatArray>()

    private fun cloudColour(t: Float): FloatArray = cloudLight.getOrPut(t) {
        if (dark) k.colour(t, lightness = 0.8f, chroma = 1f) else k.colour(t, chroma = 0.7f, atLightness = 0.84f)
    }

    /**
     * The galaxy face on, painted at [SHARE] of its size: two arms winding out from a bright core,
     * lumpy with star clouds and dark with dust along their inner sides, and sprinkled with knots
     * of new stars. Drawn by adding its light at night, as ink by day.
     */
    private fun paintGalaxy(): Bitmap {
        val size = ceil(diameter * SHARE).toInt()
        val mid = size / 2f
        val core = if (dark) k.colour(0.12f, lightness = 1.3f, chroma = 0.35f) else k.colour(0.12f, chroma = 0.8f, atLightness = 0.86f)
        val arms = if (dark) k.ramp(0.9f, 0.9f) else k.rampAt(0.55f, 1.1f)
        val knot = if (dark) k.colour(0.6f, lightness = 1.2f, chroma = 0.7f) else k.colour(0.6f, chroma = 1f, atLightness = 0.55f)
        val cell = 9f * SHARE * unit.coerceAtLeast(0.25f)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / mid
            val dy = (y - mid) / mid
            val r = sqrt(dx * dx + dy * dy)
            if (r >= 1f) {
                px[3] = 0f
            } else {
                val phase = atan2(dy, dx) - WIND * ln(r + 0.05f)
                val arm = 0.5f + 0.5f * cos(2f * phase)
                val arm3 = arm * arm * arm
                val lane = (0.5f + 0.5f * cos(2f * (phase - 0.55f))).let { val l2 = it * it; l2 * l2 * l2 }
                val clump = 0.55f + 0.9f * clouds(dx * 6f + 3f, dy * 6f + 7f)
                val disk = exp(-r / 0.32f)
                val bulge = exp(-(r / 0.085f).let { it * it })
                val halo = exp(-r / 0.13f)
                val fade = 1f - Shade.smoothstep(0.72f, 1f, r)
                // Knots of young stars: one to a cell at most, more of them where the arms are.
                val ci = floor(x / cell).toInt()
                val cj = floor(y / cell).toInt()
                var spark = 0f
                if (hash01(ci * 7 + 1, cj * 13 + 5) < arm3 * disk * 1.2f) {
                    val kx = (ci + 0.28f + 0.44f * hash01(ci, cj)) * cell
                    val ky = (cj + 0.28f + 0.44f * hash01(cj + 91, ci + 17)) * cell
                    val dd = ((x - kx) * (x - kx) + (y - ky) * (y - ky)) / (0.11f * cell * cell)
                    spark = exp(-dd) * (0.4f + 0.6f * hash01(ci + 5, cj + 3))
                }
                arms.get(0.3f + 0.7f * r, px, 3)
                val body = disk * (0.18f + 0.9f * arm3 * clump) * (1f - 0.7f * lane)
                if (dark) {
                    val glow = bulge * 1.6f + halo * 0.35f
                    for (ch in 0..2) px[ch] = (core[ch] * glow + px[3 + ch] * body * 0.8f + knot[ch] * spark * 0.45f) * fade
                    px[3] = 1f
                } else {
                    val armA = Shade.clamp01(body * 1.7f + spark * 0.3f) * fade
                    val coreA = Shade.clamp01(bulge * 0.9f + halo * 0.25f) * fade
                    val a = coreA + armA * (1f - coreA)
                    if (a <= 0f) {
                        px[3] = 0f
                    } else {
                        val armCol0 = px[3] + (knot[0] - px[3]) * Shade.clamp01(spark)
                        val armCol1 = px[4] + (knot[1] - px[4]) * Shade.clamp01(spark)
                        val armCol2 = px[5] + (knot[2] - px[5]) * Shade.clamp01(spark)
                        px[0] = (core[0] * coreA + armCol0 * armA * (1f - coreA)) / a
                        px[1] = (core[1] * coreA + armCol1 * armA * (1f - coreA)) / a
                        px[2] = (core[2] * coreA + armCol2 * armA * (1f - coreA)) / a
                        px[3] = a
                    }
                }
            }
        }
    }

    /** A bright star's glare: a point, a soft halo and four fine spikes. Added at night only. */
    private fun paintGlare(): Bitmap {
        val size = ceil(0.09f * w).toInt().coerceAtLeast(4)
        val mid = size / 2f
        val tint = k.colour(0.3f, lightness = 1.3f, chroma = 0.25f)
        return sprite(size, size) { x, y, px ->
            val dx = abs(x - mid) / mid
            val dy = abs(y - mid) / mid
            val r = sqrt(dx * dx + dy * dy)
            val core = exp(-(r / 0.06f).let { it * it }) * 1.6f + exp(-r / 0.14f) * 0.35f
            val spikes = exp(-dy / 0.018f) * exp(-dx / 0.32f) + exp(-dx / 0.018f) * exp(-dy / 0.32f)
            val g = (core + spikes * 0.6f) * (1f - Shade.smoothstep(0.7f, 1f, r))
            for (ch in 0..2) px[ch] = tint[ch] * g
            px[3] = 1f
        }
    }

    /** A shooting star lying along the strip, its head at the right. */
    private fun paintStreak(): Bitmap {
        val tint = k.colour(0.4f, lightness = 1.3f, chroma = 0.3f)
        return sprite(STREAK_LONG, STREAK_WIDE) { x, y, px ->
            val along = x / STREAK_LONG
            val across = (y - STREAK_WIDE / 2f) / (STREAK_WIDE * 0.16f)
            val tail = along.pow(2.4f) * exp(-across * across)
            val hx = (x - (STREAK_LONG - 6f)) / 3f
            val head = exp(-(hx * hx + across * across * 0.5f)) * 1.4f
            for (ch in 0..2) px[ch] = tint[ch] * (tail + head)
            px[3] = 1f
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, space, paint)
        // Two depths of stars drifting a little against each other, the near ones further.
        val driftX = 0.006f * w * LiveCurves.swing(seconds, 67.0, 0.0)
        val driftY = 0.004f * h * LiveCurves.swing(seconds, 83.0, 0.5)
        for (layer in 0 until LAYERS) {
            val far = layer == 0
            val by = if (far) 1f else 2.2f
            canvas.save()
            canvas.translate(driftX * by, driftY * by)
            dots.strokeWidth = (if (far) 1.7f else 2.8f) * unit
            for (g in 0 until GROUPS) {
                val twinkle = 0.6f + 0.4f * (0.5f + 0.5f * LiveCurves.swing(seconds, TWINKLE[g], g / 3.0 + layer * 0.17))
                val base = if (dark) (if (far) 0.75f else 0.95f) else (if (far) 0.32f else 0.42f)
                dots.color = starColour[layer * GROUPS + g]
                dots.alpha = (base * twinkle * 255f).toInt()
                canvas.drawPoints(stars[layer * GROUPS + g]!!, dots)
            }
            canvas.restore()
        }
        if (dark) {
            for (i in BRIGHT.indices) {
                val b = BRIGHT[i]
                val twinkle = 0.7f + 0.3f * LiveCurves.swing(seconds, 3.7 + i * 0.9, i / 6.0)
                drawCentred(canvas, glare, b[0] * w + driftX * 2.2f, b[1] * h + driftY * 2.2f, b[2], glowPaint, twinkle)
            }
        }
        // The galaxy: turned in its own plane, then tipped away and leant.
        val s = galaxy.width.toFloat()
        matrix.setTranslate(-s / 2f, -s / 2f)
        matrix.postScale(diameter / s, diameter / s)
        matrix.postRotate(-360f * LiveCurves.turn(seconds, GALAXY_TURN))
        matrix.postScale(1f, TILT)
        matrix.postRotate(ROLL)
        matrix.postTranslate(gx, gy)
        canvas.drawBitmap(galaxy, matrix, glowPaint)
        if (dark) meteor(canvas, seconds)
    }

    /**
     * Now and then a shooting star: once in every [METEOR_EVERY] seconds, at a time and a place
     * that are the same for that stretch every time it is drawn, never in the first few seconds.
     * The motion starts again each time the phone is unlocked, so the first comes soon after: a
     * glance at the home screen can catch one.
     */
    private fun meteor(canvas: Canvas, seconds: Double) {
        val n = floor(seconds / METEOR_EVERY).toInt()
        val start = if (n == 0) 2.5 + 1.5 * hash01(n, 1) else 3.0 + 9.0 * hash01(n, 1)
        val p = ((seconds - n * METEOR_EVERY - start) / METEOR_FOR).toFloat()
        if (p <= 0f || p >= 1f) return
        val angle = (if (hash01(n, 4) < 0.5f) 148f else 32f) + 12f * (hash01(n, 5) - 0.5f)
        val rad = Math.toRadians(angle.toDouble())
        val travel = 0.42f * w * p
        val x = w * (0.2f + 0.6f * hash01(n, 2)) + (cos(rad) * travel).toFloat()
        val y = h * (0.06f + 0.3f * hash01(n, 3)) + (sin(rad) * travel).toFloat()
        val bright = sin(PI.toFloat() * p)
        val length = 0.22f * w * (0.5f + 0.5f * bright)
        matrix.setTranslate(-STREAK_LONG.toFloat(), -STREAK_WIDE / 2f)
        matrix.postScale(length / STREAK_LONG, unit)
        matrix.postRotate(angle)
        matrix.postTranslate(x, y)
        glowPaint.alpha = (bright * 0.9f * 255f).toInt()
        canvas.drawBitmap(streak, matrix, glowPaint)
        glowPaint.alpha = 255
    }

    private companion object {
        const val LAYERS = 2
        const val GROUPS = 3

        /** How many seconds each group of stars takes to twinkle. */
        val TWINKLE = doubleArrayOf(3.1, 4.3, 5.7)

        /** The galaxy is painted at this share of the size it is drawn, which is soft enough to bear it. */
        const val SHARE = 0.7f

        /** How tightly the arms wind. */
        const val WIND = 2.4f

        /** Seconds for one turn of the galaxy. */
        const val GALAXY_TURN = 360.0

        /** How far it is tipped away, as how tall it looks for how wide, and how far it leans, in degrees. */
        const val TILT = 0.4f
        const val ROLL = -22f

        const val METEOR_EVERY = 17.0
        const val METEOR_FOR = 0.9
        const val STREAK_LONG = 256
        const val STREAK_WIDE = 16

        /** The bright stars: across, down, and how big their glare is drawn. */
        val BRIGHT = arrayOf(
            floatArrayOf(0.14f, 0.09f, 1.0f),
            floatArrayOf(0.84f, 0.19f, 0.8f),
            floatArrayOf(0.27f, 0.57f, 0.7f),
            floatArrayOf(0.90f, 0.66f, 0.9f),
            floatArrayOf(0.08f, 0.81f, 0.6f),
            floatArrayOf(0.63f, 0.93f, 0.75f),
        )

        /** Clouds of light in the dark: across, down, how wide (of the width), where on the spectrum, and how strong at night and by day. */
        val CLOUDS = arrayOf(
            floatArrayOf(0.22f, 0.24f, 0.45f, 0.15f, 0.10f, 0.16f),
            floatArrayOf(0.82f, 0.50f, 0.40f, 0.85f, 0.09f, 0.14f),
            floatArrayOf(0.50f, 0.37f, 0.60f, 0.50f, 0.06f, 0.10f),
        )
    }
}

// ----------------------------------------------------------------------------------------- Lotus

/**
 * Lotus: a flower that is a closed bud on the lock screen and opens as the phone is unlocked. Two
 * rings of petals, each petal lit once for where it lies when open; opening, each lengthens from
 * standing up to lying flat, widens, unwinds from the bud's twist and springs a little past open
 * before it settles, the outer ring first and a wave running round each ring, while the heart
 * glows up and settles. Even petals lie under the odd ones in each ring, as in a real flower.
 */
internal class LotusScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    override val openMs: Long = 1500L

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val shadowPaint = layerPaint()
    private val matrix = Matrix()
    private val cx = 0.5f * w
    private val cy = 0.41f * h
    private val light = Shade.unit(-0.45f, -0.7f, 0.55f)
    private val angle = FloatArray(PETALS) { p ->
        val ring = p / RING
        -PI.toFloat() / 2f + TAU * (p % RING) / RING + ring * PI.toFloat() / RING + 0.05f * sin(p * 2.7f)
    }
    private val length = FloatArray(PETALS) { 1f + 0.05f * sin(it * 1.9f + 0.4f) }
    private val blur = 0.022f * w
    private val pad = ceil(blur + 3f)
    private lateinit var back: Bitmap
    private lateinit var aura: Bitmap
    private lateinit var heart: Bitmap
    private lateinit var petals: Array<Bitmap>
    private lateinit var shadows: Array<Bitmap>

    override fun prepare() {
        val glow = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.7f, atLightness = 0.92f)
        val glowStrength = if (dark) 0.14f else 0.28f
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v, px, 0)
            val gx = (x - cx) / w
            val gy = (y - cy) / w
            Shade.mixInto(px, glow[0], glow[1], glow[2], exp(-(gx * gx + gy * gy) / 0.45f) * glowStrength)
        }
        aura = paintAura()
        heart = paintHeart()
        petals = Array(PETALS) { paintPetal(it) }
        shadows = Array(PETALS) { paintShadow(it) }
        shadowPaint.color = if (dark) Color.BLACK else toColour(k.colour(0.5f, chroma = 0.9f, atLightness = 0.42f))
    }

    private fun petalLong(p: Int): Float = RING_LONG[p / RING] * w * length[p]

    private fun petalHalf(p: Int): Float = RING_HALF[p / RING] * w

    private fun spriteWidth(p: Int): Int = ceil(2f * petalHalf(p) + 2f * pad).toInt()

    private fun spriteHeight(p: Int): Int = ceil(petalLong(p) + 2f * pad).toInt()

    /** How wide a petal is at [s] of the way from its base to its tip, of its widest: narrow at the base, broadest short of half way, pointed. */
    private fun shape(s: Float): Float {
        if (s <= 0f || s >= 1f) return 0f
        return s.pow(0.8f) * (1f - s) / SHAPE_PEAK
    }

    /** How fast [shape] changes along the petal. */
    private fun shapeSlope(s: Float): Float {
        val sc = s.coerceIn(0.002f, 0.998f)
        return shape(sc) * (0.8f / sc - 1f / (1f - sc))
    }

    /** How far a point is outside petal [p], in pixels, less than nothing inside; [ax] across from its middle line, [s] of the way along it. */
    private fun outside(ax: Float, s: Float, long: Float, half: Float): Float = when {
        s < 0f -> sqrt(ax * ax + (s * long) * (s * long))
        s > 1f -> sqrt(ax * ax + ((s - 1f) * long) * ((s - 1f) * long))
        else -> {
            val slope = half * shapeSlope(s) / long
            (ax - half * shape(s)) / sqrt(1f + slope * slope)
        }
    }

    /**
     * Petal [p], lying up its sprite with its base at the foot: cupped, so its edges catch the
     * light and its middle holds a crease, rising a little to its tip, deeper at its base and
     * blushing pale at its tip. Lit for where it lies in the open flower.
     */
    private fun paintPetal(p: Int): Bitmap {
        val long = petalLong(p)
        val half = petalHalf(p)
        val width = spriteWidth(p)
        val height = spriteHeight(p)
        val baseY = height - pad
        val mid = width / 2f
        val st = sin(angle[p])
        val ct = cos(angle[p])
        // The light as the petal sees it, in its sprite's own across and down.
        val lx = -light[0] * st + light[1] * ct
        val ly = -(light[0] * ct + light[1] * st)
        val lz = light[2]
        val hv = Shade.unit(lx, ly, lz + 1f)
        val i = p % RING
        val t = 0.5f - 0.5f * cos(TAU * (i + 0.5f + (p / RING) * 0.5f) / RING)
        val deep = if (dark) k.colour(t, lightness = 0.4f, chroma = 1.1f) else k.colour(t, chroma = 1f, atLightness = 0.62f)
        val lit = if (dark) k.colour(t, lightness = 1.02f) else k.colour(t, chroma = 0.8f, atLightness = 0.86f)
        val tip = if (dark) k.colour(t, lightness = 1.2f, chroma = 0.5f) else k.colour(t, chroma = 0.3f, atLightness = 0.97f)
        val gloss = if (dark) 0.35f else 0.25f
        val rimWidth = 0.004f * w
        val rimStrength = if (dark) 0.35f else 0.2f
        return sprite(width, height) { x, y, px ->
            val across = x - mid
            val ax = abs(across)
            val s = (baseY - y) / long
            val cover = Shade.clamp01(0.5f - outside(ax, s, long, half))
            if (cover <= 0f) {
                px[3] = 0f
            } else {
                val sc = s.coerceIn(0f, 1f)
                val hw = max(half * shape(sc), 0.5f)
                val q = (across / hw).coerceIn(-1f, 1f)
                val cq = q / 0.14f
                val crease = 2f * 0.03f * cq / 0.14f * exp(-cq * cq)
                val gx = 2f * CUP * q + crease
                val gy = -2f * LIFT * sc
                val nl = 1f / sqrt(gx * gx + gy * gy + 1f)
                val nx = -gx * nl
                val ny = -gy * nl
                val nz = nl
                val wrap = 0.5f + 0.5f * (nx * lx + ny * ly + nz * lz)
                val shade = wrap * sqrt(wrap)
                val nh = Shade.clamp01(nx * hv[0] + ny * hv[1] + nz * hv[2])
                val spec = Shade.pow64(nh) * gloss + Shade.pow8(nh) * 0.04f
                val along = Shade.smoothstep(0f, 0.75f, sc)
                val blush = Shade.smoothstep(0.55f, 1.05f, sc) * 0.6f
                val vein = (0.5f + 0.5f * cos(q * 7f * PI.toFloat())).let { val v2 = it * it; v2 * v2 } * (0.35f + 0.65f * (1f - sc)) * 0.07f
                val hollow = 0.55f + 0.45f * Shade.smoothstep(0f, 0.3f, sc)
                val facing = Shade.clamp01(0.3f + 0.7f * (if (across >= 0f) lx else -lx) / (sqrt(lx * lx + ly * ly) + 1e-6f))
                val rim = exp(-(hw - ax).coerceAtLeast(0f) / rimWidth) * facing * rimStrength
                for (ch in 0..2) {
                    var body = deep[ch] + (lit[ch] - deep[ch]) * (0.25f + 0.75f * along)
                    body += (tip[ch] - body) * blush
                    val low = deep[ch] * 0.6f
                    px[ch] = (low + (body - low) * shade) * hollow * (1f - vein) + spec + rim * lit[ch]
                }
                px[3] = cover
            }
        }
    }

    /** Petal [p]'s shadow: its shape, softened, in the same sprite. */
    private fun paintShadow(p: Int): Bitmap {
        val long = petalLong(p)
        val half = petalHalf(p)
        val width = spriteWidth(p)
        val height = spriteHeight(p)
        val baseY = height - pad
        val mid = width / 2f
        val strength = if (dark) 0.5f else 0.22f
        return mask(width, height) { x, y ->
            val d = outside(abs(x - mid), (baseY - y) / long, long, half)
            val a = Shade.smoothstep(blur, -blur, d) * strength
            if (dark) 1f - (1f - a).pow(1f / 2.2f) else a
        }
    }

    /** The glow behind the flower, painted at half size. At night its light is added. */
    private fun paintAura(): Bitmap {
        val reach = 0.55f * w
        val size = ceil(reach).toInt().coerceAtLeast(2)
        val mid = size / 2f
        val colour = if (dark) k.colour(0.5f, lightness = 1f, chroma = 0.9f) else k.colour(0.5f, chroma = 0.8f, atLightness = 0.9f)
        return sprite(size, size) { x, y, px ->
            val r = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)) * 2f
            val g = (exp(-(r / (0.2f * w)).let { it * it }) * 0.55f + exp(-r / (0.16f * w)) * 0.25f) * (1f - Shade.smoothstep(0.7f, 1f, r / reach))
            if (dark) {
                for (ch in 0..2) px[ch] = colour[ch] * g
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = colour[ch]
                px[3] = Shade.clamp01(g * 0.9f)
            }
        }
    }

    /** The heart: a domed pod set with seeds, ringed with fine stamens. */
    private fun paintHeart(): Bitmap {
        val radius = HEART * w
        val size = ceil(2f * 1.6f * radius + 4f).toInt()
        val mid = size / 2f
        val pod = if (dark) k.colour(0.5f, lightness = 1.15f, chroma = 0.65f) else k.colour(0.5f, chroma = 0.75f, atLightness = 0.88f)
        val podDeep = if (dark) k.colour(0.5f, lightness = 0.6f, chroma = 0.9f) else k.colour(0.5f, chroma = 0.9f, atLightness = 0.7f)
        val stamen = if (dark) k.colour(0.2f, lightness = 1.25f, chroma = 0.55f) else k.colour(0.2f, chroma = 0.7f, atLightness = 0.82f)
        val half = Shade.half(light)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / radius
            val dy = (y - mid) / radius
            val r = sqrt(dx * dx + dy * dy)
            val phi = atan2(dy, dx)
            val threads = (0.5f + 0.5f * cos(44f * phi)).let { val a2 = it * it; a2 * a2 * a2 } *
                Shade.smoothstep(1.55f, 1.12f, r) * Shade.smoothstep(0.85f, 1.02f, r)
            for (ch in 0..2) px[ch] = stamen[ch]
            var a = Shade.clamp01(threads * 0.95f)
            val cover = Shade.clamp01((1f - r) * radius + 0.5f)
            if (cover > 0f) {
                val q = min(r, 1f)
                val nz = sqrt(max(0f, 1f - q * q * 0.75f))
                val sc = if (r > 1e-6f) q / r * 0.87f else 0f
                val nx = dx * sc
                val ny = dy * sc
                val wrap = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + nz * light[2])
                val spec = Shade.pow64(Shade.clamp01(nx * half[0] + ny * half[1] + nz * half[2])) * (if (dark) 0.5f else 0.3f)
                // Seeds: one in the middle and six round it, each a small hollow.
                var hollow = 0f
                for (s in 0..6) {
                    val sa = s * TAU / 6f + 0.26f
                    val ox = if (s == 6) 0f else 0.52f * cos(sa)
                    val oy = if (s == 6) 0f else 0.52f * sin(sa)
                    val dd = sqrt((dx - ox) * (dx - ox) + (dy - oy) * (dy - oy))
                    hollow = max(hollow, Shade.smoothstep(0.17f, 0.1f, dd))
                }
                val shade = wrap * sqrt(wrap) * (1f - 0.55f * hollow)
                for (ch in 0..2) {
                    val col = podDeep[ch] + (pod[ch] - podDeep[ch]) * shade + spec
                    px[ch] = px[ch] + (col - px[ch]) * cover
                }
                a = a + (1f - a) * cover
            }
            px[3] = a
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        val x = open.coerceIn(0f, 1f)
        drawField(canvas, back, paint)
        val swell = LiveCurves.burst(x, 1.6f)
        drawCentred(canvas, aura, cx, cy, 2f * (0.85f + 0.15f * swell), glowPaint, 0.42f + 0.4f * swell)
        val grow = 0.86f + 0.14f * LiveCurves.easeOut(x)
        val lift = 0.5f + 0.5f * LiveCurves.easeOut(x)
        for (ring in 0 until PETALS / RING) for (parity in 0..1) {
            var i = parity
            while (i < RING) {
                val p = ring * RING + i
                val u = LiveCurves.spring(((x - DELAY[ring] - i * WAVE) / SPAN).coerceIn(0f, 1f), 0.6f)
                val long = CLOSED_LONG[ring] + (1f - CLOSED_LONG[ring]) * u
                val wide = CLOSED_WIDE + (1f - CLOSED_WIDE) * u
                val turn = angle[p] + TWIST[ring] * (1f - u)
                val b = petals[p]
                matrix.setTranslate(-b.width / 2f, -(b.height - pad))
                matrix.postScale(wide, long)
                matrix.postTranslate(0f, -OFFSET[ring] * w * u)
                matrix.postRotate(Math.toDegrees(turn.toDouble()).toFloat() + 90f)
                matrix.postScale(grow, grow)
                matrix.postTranslate(cx, cy)
                matrix.postTranslate(SHADOW_X * w * lift, SHADOW_Y * w * lift)
                canvas.drawBitmap(shadows[p], matrix, shadowPaint)
                matrix.postTranslate(-SHADOW_X * w * lift, -SHADOW_Y * w * lift)
                canvas.drawBitmap(b, matrix, paint)
                i += 2
            }
        }
        val pop = 0.4f + 0.6f * LiveCurves.spring(((x - 0.18f) / 0.7f).coerceIn(0f, 1f), 0.55f)
        drawCentred(canvas, heart, cx, cy, pop * grow, paint)
        settle(canvas, 0.62f, if (dark) 0.3f else 0.25f)
    }

    private companion object {
        const val RING = 8
        const val PETALS = 16

        /** Each ring's petals, back then front: how long and how wide (half), of the width. */
        val RING_LONG = floatArrayOf(0.44f, 0.31f)
        val RING_HALF = floatArrayOf(0.105f, 0.088f)

        /** How far out from the middle each ring's petals sit when open, of the width. */
        val OFFSET = floatArrayOf(0.02f, 0.008f)

        /** In the bud: how long each ring's petals look, standing up; how wide; and how far the bud is wound. */
        val CLOSED_LONG = floatArrayOf(0.46f, 0.4f)
        const val CLOSED_WIDE = 0.72f
        val TWIST = floatArrayOf(-0.32f, -0.48f)

        /** When each ring starts opening, of the opening; how far behind the next petal round is; how long each takes. */
        val DELAY = floatArrayOf(0f, 0.13f)
        const val WAVE = 0.012f
        const val SPAN = 0.72f

        /** How cupped a petal is, and how it rises to its tip. */
        const val CUP = 0.45f
        const val LIFT = 0.35f

        /** The widest a petal's [shape] reaches before it is scaled, s^0.8 (1 - s) at s = 4/9. */
        const val SHAPE_PEAK = 0.2903f

        /** How far a petal's shadow falls, across and down, of the width. */
        const val SHADOW_X = 0.012f
        const val SHADOW_Y = 0.022f

        /** The heart's radius, of the width. */
        const val HEART = 0.07f
    }
}

/** Linear light as a colour for a paint. */
private fun toColour(light: FloatArray): Int =
    Color.rgb(Oklab.toChannel(light[0]), Oklab.toChannel(light[1]), Oklab.toChannel(light[2]))

// -------------------------------------------------------------------------------------- Daybreak

/**
 * Daybreak: a total eclipse on the lock screen, the moon black in the sun's corona with a few
 * stars out. Unlocked, the moon slides away; the first of the sun bursts out at its edge as a
 * diamond ring, the corona gives way to the sun's own glow, the stars go and the sky lightens into
 * day, the moon fading into it. By day the eclipse is ink on a pale sky in a coloured corona.
 */
internal class DaybreakScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    override val openMs: Long = 1800L

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; color = Color.WHITE }
    private val cutPaint = layerPaint().apply { blendMode = BlendMode.DST_OUT }
    private val sunBox = RectF()
    private val sx = 0.5f * w
    private val sy = 0.34f * h
    private val radius = 0.17f * w
    private val moonRadius = 1.025f * radius
    private val awayX: Float
    private val awayY: Float
    private lateinit var night: Bitmap
    private lateinit var day: Bitmap
    private lateinit var corona: Bitmap
    private lateinit var halo: Bitmap
    private lateinit var sun: Bitmap
    private lateinit var moon: Bitmap
    private lateinit var bead: Bitmap
    private lateinit var stars: FloatArray

    init {
        val l = sqrt(0.72f * 0.72f + 0.69f * 0.69f)
        awayX = -0.72f / l
        awayY = 0.69f / l
    }

    override fun prepare() {
        val warm = if (dark) k.colour(0.3f, lightness = 0.75f, chroma = 0.9f) else k.colour(0.3f, chroma = 0.55f, atLightness = 0.95f)
        night = field(4) { x, y, px ->
            val v = y / h
            if (dark) {
                k.dusk(v, px, 0)
                for (ch in 0..2) px[ch] *= 0.8f
            } else {
                k.sky(v, px, 0)
            }
        }
        day = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v * 0.8f, px, 0)
            val gx = (x - sx) / w
            val gy = (y - sy) / h
            val wash = exp(-(gx * gx) / 0.5f - (gy * gy) / 0.09f) * (if (dark) 0.28f else 0.4f)
            Shade.mixInto(px, warm[0], warm[1], warm[2], wash)
        }
        corona = paintCorona()
        halo = paintHalo()
        sun = paintSun()
        moon = paintMoon()
        bead = paintBead()
        val random = Random(31)
        stars = FloatArray(STARS * 2)
        for (i in 0 until STARS) {
            stars[i * 2] = random.nextFloat() * w
            stars[i * 2 + 1] = random.nextFloat() * h * 0.62f
        }
    }

    /** The corona, painted at half size: a tight bright ring at the moon's edge, then long streamers fading out. */
    private fun paintCorona(): Bitmap {
        val reach = 2.9f * radius
        val size = ceil(reach).toInt().coerceAtLeast(2)
        val mid = size / 2f
        val glow = if (dark) k.ramp(1f, 1.05f) else k.rampAt(0.72f, 1.1f)
        val hot = if (dark) k.ramp(1.25f, 0.6f) else k.rampAt(0.95f, 0.5f)
        val edgeWidth = 0.006f * w
        val col = FloatArray(6)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) * 2f
            val dy = (y - mid) * 2f
            val dist = sqrt(dx * dx + dy * dy)
            val rr = dist / radius
            val b = max(rr - 1f, 0f)
            val phi = atan2(dy, dx)
            val cp = cos(phi)
            val sp = sin(phi)
            val streams = 0.55f + 0.45f * clouds(cp * 2.2f + 3.1f, sp * 2.2f + 1.7f)
            val fine = 0.8f + 0.4f * valueNoise(cp * 9f + 5f, sp * 9f + 2f)
            val ears = 0.8f + 0.35f * cos(2f * (phi + 0.35f))
            val fade = 1f - Shade.smoothstep(0.75f, 1f, dist / reach)
            val corona = if (dark) {
                (exp(-b / 0.08f) * 0.9f + exp(-b / 0.35f) * 0.5f * streams * fine * ears) * fade
            } else {
                (exp(-b / 0.12f) * 0.9f + exp(-b / 0.5f) * 0.6f * streams * fine * ears) * fade
            }
            val ring = exp(-abs(rr - 1.032f) * radius / edgeWidth) * (0.4f + 0.6f * (0.5f + 0.5f * cos(phi - 0.8f))) * fade
            val t = Shade.clamp01(0.5f + 0.5f * sin(phi + 0.6f))
            glow.get(t, col, 0)
            hot.get(t, col, 3)
            if (dark) {
                for (ch in 0..2) px[ch] = col[ch] * corona + col[3 + ch] * ring * 0.9f
                px[3] = 1f
            } else {
                val a = Shade.clamp01(corona * 1.05f)
                val ra = Shade.clamp01(ring * 0.6f)
                val all = a + ra * (1f - a)
                if (all <= 0f) {
                    px[3] = 0f
                } else {
                    for (ch in 0..2) px[ch] = (col[3 + ch] * ra + col[ch] * a * (1f - ra)) / all
                    px[3] = all
                }
            }
        }
    }

    /** The sun's own glow, painted at half size. */
    private fun paintHalo(): Bitmap {
        val reach = 3.4f * radius
        val size = ceil(reach).toInt().coerceAtLeast(2)
        val mid = size / 2f
        val colour = if (dark) k.colour(0.25f, lightness = 1.15f, chroma = 0.7f) else k.colour(0.25f, chroma = 1.05f, atLightness = 0.78f)
        return sprite(size, size) { x, y, px ->
            val rr = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)) * 2f / radius
            val g = (exp(-(rr / 1.15f).let { it * it }) + exp(-rr / 1.3f) * 0.4f) * (1f - Shade.smoothstep(0.7f, 1f, rr * radius / reach))
            if (dark) {
                for (ch in 0..2) px[ch] = colour[ch] * g * 0.42f
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = colour[ch]
                px[3] = Shade.clamp01(g * 0.9f)
            }
        }
    }

    /** The sun's face: near white, a little warmer and dimmer towards its edge. */
    private fun paintSun(): Bitmap {
        val size = ceil(2f * radius + 4f).toInt()
        val mid = size / 2f
        val white = k.colour(0.2f, chroma = 0.08f, atLightness = 0.99f)
        val edge = if (dark) k.colour(0.2f, chroma = 0.5f, atLightness = 0.9f) else k.colour(0.2f, chroma = 0.6f, atLightness = 0.9f)
        return sprite(size, size) { x, y, px ->
            val d = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid))
            val cover = Shade.clamp01(radius - d + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
            } else {
                val q = min(d / radius, 1f)
                val mu = sqrt(max(0f, 1f - q * q))
                val f = sqrt(mu)
                for (ch in 0..2) px[ch] = edge[ch] + (white[ch] - edge[ch]) * f
                px[3] = cover
            }
        }
    }

    /** The moon: a dark disc against the corona, faintly lit by the earth, a little lighter by day. */
    private fun paintMoon(): Bitmap {
        val size = ceil(2f * moonRadius + 4f).toInt()
        val mid = size / 2f
        val body = if (dark) FloatArray(3) { k.deepLight[it] * 0.45f } else k.colour(0.5f, chroma = 0.7f, atLightness = 0.36f)
        val earth = if (dark) k.colour(0.8f, lightness = 0.35f, chroma = 0.6f) else k.colour(0.8f, chroma = 0.8f, atLightness = 0.55f)
        return sprite(size, size) { x, y, px ->
            val dx = x - mid
            val dy = y - mid
            val d = sqrt(dx * dx + dy * dy)
            val cover = Shade.clamp01(moonRadius - d + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
            } else {
                val shine = Shade.clamp01(0.5f + 0.5f * (-dx * 0.6f + dy * 0.8f) / moonRadius) * (if (dark) 0.05f else 0.12f)
                for (ch in 0..2) px[ch] = body[ch] + (earth[ch] - body[ch]) * shine
                px[3] = cover
            }
        }
    }

    /** The diamond ring's diamond: a blinding point with its halo and six fine rays. */
    private fun paintBead(): Bitmap {
        val reach = 0.9f * radius
        val size = ceil(2f * reach).toInt().coerceAtLeast(4)
        val mid = size / 2f
        val hot = k.colour(0.2f, lightness = 1.4f, chroma = 0.25f)
        val turn = 0.26f
        val ct = cos(turn)
        val st = sin(turn)
        return sprite(size, size) { x, y, px ->
            val ox = (x - mid) / radius
            val oy = (y - mid) / radius
            val rx = ox * ct - oy * st
            val ry = ox * st + oy * ct
            val r = sqrt(rx * rx + ry * ry)
            val core = exp(-(r / 0.05f).let { it * it }) * 2.5f + exp(-r / 0.16f) * 0.7f
            val cross = exp(-abs(ry) / 0.01f) * exp(-abs(rx) / 0.38f) + exp(-abs(rx) / 0.01f) * exp(-abs(ry) / 0.38f)
            val d1 = (rx + ry) * 0.7071f
            val d2 = (rx - ry) * 0.7071f
            val diagonal = exp(-abs(d1) / 0.008f) * exp(-abs(d2) / 0.2f) + exp(-abs(d2) / 0.008f) * exp(-abs(d1) / 0.2f)
            val g = (core + cross * 0.55f + diagonal * 0.2f) * (1f - Shade.smoothstep(0.6f, 0.9f, r))
            if (dark) {
                for (ch in 0..2) px[ch] = hot[ch] * g
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = hot[ch]
                px[3] = Shade.clamp01(g * 0.8f)
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        val x = open.coerceIn(0f, 1f)
        drawField(canvas, night, paint)
        val dayAlpha = ease(0.1f, 0.85f, x)
        if (dayAlpha > 0f) {
            paint.alpha = (dayAlpha * 255f + 0.5f).toInt()
            drawField(canvas, day, paint)
            paint.alpha = 255
        }
        val starAlpha = 1f - ease(0.05f, 0.4f, x)
        if (dark && starAlpha > 0f) {
            dots.strokeWidth = 2.2f * unit
            dots.alpha = (starAlpha * 0.85f * 255f).toInt()
            canvas.drawPoints(stars, dots)
        }
        drawCentred(canvas, corona, sx, sy, 2f, glowPaint, 1f - 0.8f * ease(0.05f, 0.55f, x))
        val haloAlpha = ease(0.04f, 0.6f, x)
        if (haloAlpha > 0f) drawCentred(canvas, halo, sx, sy, 2f, glowPaint, haloAlpha)
        // The sun, less wherever the moon covers it: the moon can then fade into the sky as the
        // day comes without the sun showing through it.
        val travel = TRAVEL * radius * LiveCurves.easeInOut(x)
        val mx = sx + awayX * travel
        val my = sy + awayY * travel
        if (travel < radius + moonRadius) {
            sunBox.set(sx - radius - 2f, sy - radius - 2f, sx + radius + 2f, sy + radius + 2f)
            canvas.saveLayer(sunBox, null)
            drawCentred(canvas, sun, sx, sy, 1f, paint)
            drawCentred(canvas, moon, mx, my, 1f, cutPaint)
            canvas.restore()
        } else {
            drawCentred(canvas, sun, sx, sy, 1f, paint)
        }
        val moonAlpha = 1f - ease(0.3f, 0.75f, x)
        if (moonAlpha > 0f) drawCentred(canvas, moon, mx, my, 1f, paint, moonAlpha)
        val beadAlpha = ((x - 0.2f) / 0.085f).let { exp(-it * it) }
        if (beadAlpha > 0.004f) drawCentred(canvas, bead, sx - awayX * radius, sy - awayY * radius, 1f, glowPaint, beadAlpha)
        settle(canvas, 0.62f, if (dark) 0.3f else 0.25f)
    }

    private companion object {
        /** How far the moon slides, in the sun's radii: enough to clear it. */
        const val TRAVEL = 2.35f
        const val STARS = 46
    }
}
