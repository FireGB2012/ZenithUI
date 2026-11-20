package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Six more designs that drift on their own, the Live shelf's second batch: Borealis, Petals,
 * Kaleido, Flow, Jelly and Clouds. As the first five, each paints its layers once, lit as the Depth
 * designs are, and then only moves, turns, bends and fades them from frame to frame, so a frame
 * costs a phone's graphics next to nothing. The light picture is drawn for a pale page, not washed
 * out from the dark one, and the lower part of both is kept quiet for the icons.
 */

private const val TAU = (2.0 * PI).toFloat()
private const val TAU_D = 2.0 * PI

/** Noise for the Live designs: numbers worked out from where a point is, never from a seed kept anywhere. */
internal object LiveNoise {

    /** A number from 0 to 1 that is always the same for [a] and [b]. */
    fun hash01(a: Int, b: Int): Float {
        var n = a * 374761393 + b * 668265263
        n = (n xor (n ushr 13)) * 1274126177
        n = n xor (n ushr 16)
        return (n and 0xFFFFFF) / 16777216f
    }

    /** A smooth hill-and-dale of numbers from 0 to 1 across the plane, one hill or so to a unit. */
    fun value(x: Float, y: Float): Float {
        val xf = floor(x)
        val yf = floor(y)
        return lattice(xf.toInt(), xf.toInt() + 1, yf.toInt(), x - xf, y - yf)
    }

    /** [value] that comes round again every [period] units across, for a strip whose ends meet. */
    fun valueTiled(x: Float, y: Float, period: Int): Float {
        val xf = floor(x)
        val yf = floor(y)
        val xi = Math.floorMod(xf.toInt(), period)
        return lattice(xi, (xi + 1) % period, yf.toInt(), x - xf, y - yf)
    }

    private fun lattice(x0: Int, x1: Int, y0: Int, fx: Float, fy: Float): Float {
        val sx = fx * fx * (3f - 2f * fx)
        val sy = fy * fy * (3f - 2f * fy)
        val a = hash01(x0, y0)
        val b = hash01(x1, y0)
        val c = hash01(x0, y0 + 1)
        val d = hash01(x1, y0 + 1)
        return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
    }

    /** Three [value]s at finer and finer sizes, 0 to 1: the lumpiness of clouds. */
    fun fbm(x: Float, y: Float): Float =
        0.5714f * value(x, y) + 0.2857f * value(2.03f * x + 5.2f, 2.03f * y + 1.3f) + 0.1429f * value(4.1f * x + 1.7f, 4.1f * y + 9.2f)

    /** Four [value]s at finer and finer sizes that come round again every [period] units across. */
    fun fbmTiled(x: Float, y: Float, period: Int): Float =
        0.5333f * valueTiled(x, y, period) + 0.2667f * valueTiled(2f * x, 2f * y + 1.3f, 2 * period) +
            0.1333f * valueTiled(4f * x, 4f * y + 9.2f, 4 * period) + 0.0667f * valueTiled(8f * x, 8f * y + 4.7f, 8 * period)

    /** Linear light as a colour for a paint. */
    fun colour(light: FloatArray): Int = Color.rgb(Oklab.toChannel(light[0]), Oklab.toChannel(light[1]), Oklab.toChannel(light[2]))
}

/** The phase of something going round [speed] radians a second from [start], kept small however long it has run. */
private fun phase(start: Float, speed: Float, seconds: Double): Float = ((start + speed * seconds) % TAU_D).toFloat()

/** [x] wrapped into 0 until [span]. */
private fun wrap(x: Double, span: Double): Float = (((x % span) + span) % span).toFloat()

// -------------------------------------------------------------------------------------- Borealis

/**
 * Borealis: curtains of the northern lights hanging over a dark ridge, their folds rolling along
 * them and their rays swaying, under the stars. Each curtain is painted once as a strip, bright at
 * its foot and fading up its rays, and every frame bends that strip to where its folds have rolled.
 * By day the curtains are pastel veils over a pale sky and snowy hills.
 */
internal class BorealisScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val verts = FloatArray((COLS + 1) * 4)
    private val ridgeTop = (RIDGE_FROM * h).toInt()
    private val stars = arrayOfNulls<FloatArray>(GROUPS)
    private val starColour = IntArray(GROUPS)
    private lateinit var sky: Bitmap
    private lateinit var curtains: Array<Bitmap>
    private lateinit var ridges: Bitmap

    override fun prepare() {
        val glow = if (dark) k.colour(0.3f, lightness = 1.05f, chroma = 1f) else k.colour(0.4f, chroma = 0.6f, atLightness = 0.93f)
        sky = field(4) { _, y, px ->
            val v = y / h
            if (dark) k.dusk(v * 0.95f, px, 0) else k.sky(v * 0.7f, px, 0)
            // The sky over the ridge lit by the curtains.
            val g = exp(-((v - 0.5f) / 0.14f).let { it * it }) * (if (dark) 0.10f else 0.35f)
            Shade.mixInto(px, glow[0], glow[1], glow[2], g)
        }
        curtains = Array(CURTAINS.size) { curtain(it) }
        ridges = paintRidges()
        if (dark) {
            val random = Random(17)
            for (g in 0 until GROUPS) {
                val pts = FloatArray(STARS / GROUPS * 2)
                for (s in pts.indices step 2) {
                    pts[s] = random.nextFloat() * w
                    pts[s + 1] = random.nextFloat().pow(1.3f) * h * 0.62f
                }
                stars[g] = pts
                starColour[g] = when (g) {
                    0 -> Color.WHITE
                    1 -> mix(Color.WHITE, c.accent, 0.3f)
                    else -> mix(Color.WHITE, c.accentAlt, 0.3f)
                }
            }
        }
    }

    /**
     * Curtain [j] as a strip, its foot along the bottom: bright just above a soft lower edge and
     * fading up its rays, which are fine streaks of every width, brighter in patches along it. The
     * colour runs from one end of the spectrum at its foot to the other at the top of its rays.
     */
    private fun curtain(j: Int): Bitmap {
        val cfg = CURTAINS[j]
        val seed = j * 13.7f + 3.1f
        val low = if (dark) k.colour(cfg[6], lightness = 1.2f, chroma = 1.1f) else k.colour(cfg[6], chroma = 1f, atLightness = 0.72f)
        val high = if (dark) k.colour(cfg[7], lightness = 1.0f, chroma = 1.15f) else k.colour(cfg[7], chroma = 0.95f, atLightness = 0.76f)
        val strength = cfg[8]
        return sprite(TEX_W, TEX_H) { x, y, px ->
            val u = x / TEX_W
            val v = 1f - y / TEX_H
            // A soft lower edge with a brighter seam just above it, as a curtain's foot has.
            val foot = Shade.smoothstep(0f, 0.05f, v) * (1f + 0.5f * exp(-((v - 0.05f) / 0.03f).let { it * it }))
            // Each ray reaches its own height and fades out into the sky well before it.
            val reach = 0.62f + 0.38f * LiveNoise.value(u * 46f + seed, 2.3f + seed)
            val top = 1f - Shade.smoothstep(reach * 0.3f, reach, v)
            val rise = 0.2f + 0.8f * exp(-v / 0.3f)
            val n1 = LiveNoise.value(u * 64f + v * 1.4f + seed, seed * 3f)
            val n2 = LiveNoise.value(u * 170f - v * 2.2f + seed * 2f, 7.1f + seed)
            val rays = 0.3f + 0.7f * (0.62f * n1 * n1 + 0.38f * n2 * n2)
            val patch = 0.35f + 0.65f * Shade.smoothstep(0.3f, 0.75f, LiveNoise.fbm(u * 4.5f + seed, 1.7f + seed))
            val ends = Shade.smoothstep(0f, 0.16f, u) * Shade.smoothstep(1f, 0.84f, u)
            val i = foot * top * rise * rays * patch * ends * strength
            val m = Shade.smoothstep(0.05f, 0.75f, v)
            if (dark) {
                for (ch in 0..2) px[ch] = (low[ch] + (high[ch] - low[ch]) * m) * i
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = low[ch] + (high[ch] - low[ch]) * m
                px[3] = Shade.clamp01(i * 1.25f)
            }
        }
    }

    /**
     * Two ridges: the far one's face lit by the curtains, most at its crest, the near one a dark
     * line of hills running down into the ground; by day, peaks capped with snow on the side the
     * light comes from, in blue shade on the other, and the near hills fading into snow.
     */
    private fun paintRidges(): Bitmap {
        val down = (h - ridgeTop).toInt().coerceAtLeast(1)
        val across = ceil(w).toInt()
        val far = if (dark) FloatArray(3) { k.deepLight[it] * 0.8f } else k.colour(0.6f, chroma = 0.32f, atLightness = 0.83f)
        val near = if (dark) FloatArray(3) { k.deepLight[it] * 0.38f } else k.colour(0.6f, chroma = 0.35f, atLightness = 0.77f)
        val lit = if (dark) k.colour(0.3f, lightness = 0.7f, chroma = 0.8f) else k.colour(0.4f, chroma = 0.1f, atLightness = 0.98f)
        val snow = k.colour(0.55f, chroma = 0.12f, atLightness = 0.97f)
        val ground = if (dark) FloatArray(3) { k.deepLight[it] * 0.3f } else k.baseLight
        return sprite(across, down) { x, y, px ->
            val sy = y + ridgeTop
            val u = x / w
            val farLine = h * (0.64f - 0.085f * mountain(u * 2.6f + 1.3f))
            val nearLine = h * (0.735f - 0.06f * mountain(u * 1.9f + 7.7f))
            val inFar = Shade.clamp01(sy - farLine + 0.5f)
            if (inFar <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val below = (sy - farLine) / h
            if (dark) {
                // Lit from the sky above, the more the higher: the same at every point across, so no streaks run down it.
                val glow = exp(-(sy / h - 0.55f) / 0.045f) * 0.45f
                for (ch in 0..2) px[ch] = far[ch] + lit[ch] * glow
            } else {
                // Snow on the peaks, giving way to blue haze lower down.
                val cap = exp(-(sy / h - 0.555f) / 0.05f)
                for (ch in 0..2) px[ch] = far[ch] + (snow[ch] - far[ch]) * Shade.clamp01(cap) * 0.55f
            }
            val inNear = Shade.clamp01(sy - nearLine + 0.5f)
            if (inNear > 0f) {
                val nb = (sy - nearLine) / h
                val toGround = Shade.smoothstep(0.03f, 0.2f, nb)
                for (ch in 0..2) {
                    var col = near[ch]
                    col += (ground[ch] - col) * toGround
                    px[ch] = px[ch] + (col - px[ch]) * inNear
                }
            }
            px[3] = inFar
        }
    }

    /** A line of peaks, 0 to 1: noise folded so its tops are sharp. */
    private fun mountain(x: Float): Float {
        val a = 1f - abs(2f * LiveNoise.value(x, 3.3f) - 1f)
        val b = 1f - abs(2f * LiveNoise.value(x * 2.7f + 4f, 8.1f) - 1f)
        return 0.7f * a * a + 0.3f * b
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, sky, paint)
        if (dark) {
            for (g in 0 until GROUPS) {
                val twinkle = 0.65f + 0.35f * (0.5f + 0.5f * LiveCurves.swing(seconds, TWINKLE[g], g / 3.0))
                dots.color = starColour[g]
                dots.alpha = (0.85f * twinkle * 255f).toInt()
                dots.strokeWidth = (1.6f + 0.6f * g) * unit
                canvas.drawPoints(stars[g]!!, dots)
            }
        }
        val lower = (COLS + 1) * 2
        for (j in CURTAINS.indices) {
            val cfg = CURTAINS[j]
            val tall = cfg[4] * h
            val folds = cfg[5]
            // As deep as the folds can be without the curtain bunching up into a seam.
            val fold = w * min(0.06f, 0.55f * (cfg[1] - cfg[0]) / (TAU * folds))
            val p1 = phase(j * 1.9f, 0.11f + 0.03f * j, seconds)
            val p2 = phase(j * 0.7f, -0.23f + 0.04f * j, seconds)
            val p3 = phase(j * 2.3f, 0.17f, seconds)
            val pf = phase(j * 1.1f, 0.21f - 0.05f * j, seconds)
            val pl = phase(j * 0.4f, 0.09f, seconds)
            for (i in 0..COLS) {
                val s = i / COLS.toFloat()
                val x0 = w * (cfg[0] + (cfg[1] - cfg[0]) * s)
                // Folds rolling along it: the strip bunches up and spreads out as they pass.
                val x = x0 + fold * sin(TAU * folds * s + pf)
                val foot = h * (cfg[2] + (cfg[3] - cfg[2]) * s)
                val footY = foot + h * (0.022f * sin(TAU * 1.2f * s + p1) + 0.01f * sin(TAU * 3.1f * s + p2))
                val topY = footY - tall * (0.84f + 0.16f * sin(TAU * 2.1f * s + p3))
                // The rays lean and sway a little, as the curtain stirs.
                val lean = 0.05f * w * sin(TAU * 0.7f * s + pl)
                verts[i * 2] = x + lean
                verts[i * 2 + 1] = topY
                verts[lower + i * 2] = x
                verts[lower + i * 2 + 1] = footY
            }
            val pulse = 0.78f + 0.22f * LiveCurves.swing(seconds, 11.0 + 4.0 * j, j * 0.31)
            glowPaint.alpha = (pulse * 255f).toInt()
            canvas.drawBitmapMesh(curtains[j], COLS, 1, verts, 0, null, 0, glowPaint)
        }
        glowPaint.alpha = 255
        canvas.drawBitmap(ridges, 0f, ridgeTop.toFloat(), paint)
        settle(canvas, 0.7f, if (dark) 0.35f else 0.25f)
    }

    private companion object {
        const val COLS = 48
        const val TEX_W = 512
        const val TEX_H = 192
        const val GROUPS = 3
        const val STARS = 210
        val TWINKLE = doubleArrayOf(2.9, 4.1, 5.3)

        /** Where the ridges' layer starts, of the height. */
        const val RIDGE_FROM = 0.56f

        /**
         * Each curtain, far first: where it starts and ends across, of the width; where its foot
         * hangs at either end, of the height, so it sweeps across the sky rather than lying flat;
         * how tall its rays reach, of the height; how many folds roll along it; where on the
         * spectrum its foot and the top of its rays are; and how bright it is.
         */
        val CURTAINS = arrayOf(
            floatArrayOf(-0.2f, 0.9f, 0.3f, 0.21f, 0.2f, 2.0f, 0.25f, 0.85f, 0.62f),
            floatArrayOf(-0.25f, 1.25f, 0.47f, 0.33f, 0.29f, 1.6f, 0.1f, 0.95f, 1f),
            floatArrayOf(0.3f, 1.3f, 0.52f, 0.46f, 0.2f, 2.6f, 0.4f, 0.7f, 0.72f),
        )
    }
}

// ---------------------------------------------------------------------------------------- Petals

/**
 * Petals: blossom petals tumbling down through the air, the far ones small, sharp and slow, the
 * near ones large, soft with being out of focus, and quicker. Each petal is lit once and then only
 * moved, turned and flipped as it falls, swaying on the air. Behind them, out-of-focus blossom.
 */
internal class PetalsScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val matrix = Matrix()
    private val light = Shade.unit(-0.45f, -0.7f, 0.55f)
    private lateinit var back: Bitmap
    private lateinit var sprites: Array<Bitmap>
    private val falling = mutableListOf<FloatArray>()

    override fun prepare() {
        val glow = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.6f, atLightness = 0.94f)
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v * 0.8f, px, 0)
            val gx = (x - 0.62f * w) / w
            val gy = (y - 0.22f * h) / w
            Shade.mixInto(px, glow[0], glow[1], glow[2], exp(-(gx * gx + gy * gy) / 0.35f) * (if (dark) 0.14f else 0.3f))
            // Blossom far behind, out of focus: soft discs, a little brighter at their rims, as a lens draws them.
            for (b in BOKEH) {
                val dx = (x - b[0] * w) / w
                val dy = (y - b[1] * h) / w
                val r = sqrt(dx * dx + dy * dy) / b[2]
                if (r > 1.1f) continue
                val g = (1f - Shade.smoothstep(0.82f, 1f, r)) * (0.8f + 0.2f * Shade.smoothstep(0.5f, 0.95f, r))
                val col = bokehColour(b[3])
                Shade.mixInto(px, col[0], col[1], col[2], g * (if (dark) b[4] else b[4] * 1.3f))
            }
        }
        sprites = Array(DEPTHS * 2) { petal(it / 2, it % 2) }
        val random = Random(41)
        for (d in 0 until DEPTHS) repeat(COUNT[d]) {
            falling += floatArrayOf(
                d.toFloat(),
                random.nextInt(2).toFloat(),
                random.nextFloat(),
                random.nextFloat(),
                FALL[d] * (0.8f + 0.4f * random.nextFloat()),
                (0.02f + 0.03f * random.nextFloat()) * (1f + d * 0.4f),
                4f + 4f * random.nextFloat(),
                random.nextFloat(),
                360f * random.nextFloat(),
                (12f + 30f * random.nextFloat()) * (if (random.nextBoolean()) 1f else -1f),
                3f + 4f * random.nextFloat(),
                random.nextFloat(),
            )
        }
    }

    private val bokehLight = HashMap<Float, FloatArray>()

    private fun bokehColour(t: Float): FloatArray = bokehLight.getOrPut(t) {
        if (dark) k.colour(t, lightness = 0.75f, chroma = 0.9f) else k.colour(t, chroma = 0.6f, atLightness = 0.88f)
    }

    /**
     * A petal of depth [depth] (0 far to 2 near) in colour [variant]: narrow at its base, broad and
     * notched at its tip, a little cupped, deeper in colour at its base and fine veins running out
     * from it. The nearest are soft at the edges and plainer, as things out of focus are.
     */
    private fun petal(depth: Int, variant: Int): Bitmap {
        val long = LENGTH[depth] * w
        val wide = 0.78f * long
        val soft = SOFT[depth] * w + 0.6f
        val pad = ceil(soft * 2f + 2f)
        val sw = ceil(wide + 2f * pad).toInt()
        val sh = ceil(long + 2f * pad).toInt()
        val midX = sw / 2f
        val baseY = sh - pad
        val t = if (variant == 0) 0.12f else 0.82f
        val face = if (dark) k.colour(t, lightness = 1.35f, chroma = 0.38f) else k.colour(t, chroma = 0.6f, atLightness = 0.8f)
        val heart = if (dark) k.colour(t, lightness = 0.95f, chroma = 1f) else k.colour(t, chroma = 1f, atLightness = 0.62f)
        val tip = if (dark) k.colour(t, lightness = 1.5f, chroma = 0.15f) else k.colour(t, chroma = 0.3f, atLightness = 0.9f)
        val detail = if (depth == DEPTHS - 1) 0.4f else 1f
        val hv = Shade.half(light)
        return sprite(sw, sh) { x, y, px ->
            val ax = x - midX
            val s = (baseY - y) / long
            // Its outline: narrow at the base, widest [WIDEST] of the way along, round over the tip.
            val taper = 0.4f + 0.6f * Shade.smoothstep(0f, WIDEST, s)
            val halfW = 0.5f * wide * taper
            val reach = if (s < WIDEST) WIDEST else 1f - WIDEST
            val ex = ax / halfW
            val ey = (s - WIDEST) / reach
            val f = ex * ex + ey * ey - 1f
            val gx = 2f * ex / halfW
            val gy = 2f * ey / (reach * long)
            var d = f / sqrt(gx * gx + gy * gy).coerceAtLeast(1e-4f)
            // And the small notch in its tip.
            val q = abs(ax) / (0.5f * wide)
            d = max(d, (s - (1f - 0.09f * max(0f, 1f - q / 0.2f))) * long)
            val cover = Shade.clamp01(0.5f - d / soft)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val sc = s.coerceIn(0f, 1f)
            val qq = ex.coerceIn(-1f, 1f)
            // Cupped: the edges turn up towards the light.
            val nx0 = 0.7f * qq
            val ny0 = -0.5f * sc
            val nl = 1f / sqrt(nx0 * nx0 + ny0 * ny0 + 1f)
            val nx = -nx0 * nl
            val ny = -ny0 * nl
            val wrapped = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + nl * light[2])
            val spec = Shade.pow16(Shade.clamp01(nx * hv[0] + ny * hv[1] + nl * hv[2])) * 0.12f * detail
            val angle = atan2(ax, sc * long + 0.08f * long)
            val vein = (0.5f + 0.5f * cos(angle * 22f)).let { val v2 = it * it; v2 * v2 } * 0.06f * detail * (1f - sc)
            val toHeart = (1f - sc).let { it * it * it } * 0.8f
            val toTip = Shade.smoothstep(0.55f, 1f, sc) * 0.55f
            for (ch in 0..2) {
                var col = face[ch] + (heart[ch] - face[ch]) * toHeart
                col += (tip[ch] - col) * toTip
                px[ch] = col * (0.72f + 0.28f * wrapped) * (1f - vein) + spec
            }
            px[3] = cover * (if (dark) 0.95f else 0.92f)
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, back, paint)
        val marginY = 0.08f * h
        val marginX = 0.08f * w
        for (p in falling) {
            val depth = p[0].toInt()
            val b = sprites[depth * 2 + p[1].toInt()]
            val y = wrap(p[3] * (h + 2f * marginY) + p[4] * h * seconds, (h + 2f * marginY).toDouble()) - marginY
            val drift = WIND * w * seconds * (0.6 + 0.2 * depth)
            val sway = p[5] * w * sin(phase(TAU * p[7], TAU / p[6], seconds))
            val x = wrap(p[2] * (w + 2f * marginX) + drift + sway, (w + 2f * marginX).toDouble()) - marginX
            val turn = ((p[8] + p[9] * seconds) % 360.0).toFloat()
            // Flipping over as it falls: seen edge on at times, its other face at others.
            val flip = cos(phase(TAU * p[11], TAU / p[10], seconds))
            val across = if (abs(flip) < 0.08f) 0.08f * (if (flip < 0f) -1f else 1f) else flip
            matrix.setTranslate(-b.width / 2f, -b.height / 2f)
            matrix.postScale(across, 1f)
            matrix.postRotate(turn)
            matrix.postTranslate(x, y)
            paint.alpha = (ALPHA[depth] * 255f).toInt()
            canvas.drawBitmap(b, matrix, paint)
        }
        paint.alpha = 255
        settle(canvas, 0.6f, if (dark) 0.36f else 0.28f)
    }

    private companion object {
        const val DEPTHS = 3

        /** How many petals at each depth, far to near; how long they are, of the width; how soft their edges. */
        val COUNT = intArrayOf(18, 14, 7)
        val LENGTH = floatArrayOf(0.036f, 0.06f, 0.1f)
        val SOFT = floatArrayOf(0f, 0.001f, 0.007f)

        /** Where along a petal it is widest. */
        const val WIDEST = 0.64f

        /** How fast each depth falls, of the height a second, and how see-through it is. */
        val FALL = floatArrayOf(0.018f, 0.03f, 0.048f)
        val ALPHA = floatArrayOf(0.66f, 0.92f, 0.8f)

        /** The air carries them a little to the right, of the width a second. */
        const val WIND = 0.006

        /** Blossom out of focus behind: across, down, its radius (of the width), where on the spectrum, how strong. */
        val BOKEH = arrayOf(
            floatArrayOf(0.12f, 0.1f, 0.09f, 0.15f, 0.22f),
            floatArrayOf(0.86f, 0.07f, 0.11f, 0.8f, 0.2f),
            floatArrayOf(0.7f, 0.3f, 0.06f, 0.2f, 0.16f),
            floatArrayOf(0.24f, 0.38f, 0.07f, 0.75f, 0.14f),
            floatArrayOf(0.5f, 0.05f, 0.08f, 0.5f, 0.15f),
            floatArrayOf(0.93f, 0.42f, 0.05f, 0.3f, 0.12f),
            floatArrayOf(0.04f, 0.52f, 0.06f, 0.6f, 0.1f),
            floatArrayOf(0.4f, 0.2f, 0.045f, 0.9f, 0.12f),
        )
    }
}

// --------------------------------------------------------------------------------------- Kaleido

/**
 * Kaleido: a kaleidoscope of cut glass, three rings of facets turning slowly against each other
 * round a jewelled heart, so the colours they pass through come and go. Each ring is cut and lit
 * once, as stained glass with light behind it, and only turned after that; a sheen that stays put
 * across them makes the facets catch the light as they go by.
 */
internal class KaleidoScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glassPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val matrix = Matrix()
    private val cx = 0.5f * w
    private val cy = 0.4f * h
    private val radius = 0.47f * w
    private val light = Shade.unit(-0.5f, -0.75f, 0.45f)
    private lateinit var back: Bitmap
    private lateinit var rings: Array<Bitmap>
    private lateinit var sheen: Bitmap

    override fun prepare() {
        val glow = if (dark) k.colour(0.5f, lightness = 0.95f) else k.colour(0.5f, chroma = 0.6f, atLightness = 0.92f)
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v * 0.8f, px, 0)
            val gx = (x - cx) / w
            val gy = (y - cy) / w
            Shade.mixInto(px, glow[0], glow[1], glow[2], exp(-(gx * gx + gy * gy) / 0.3f) * (if (dark) 0.16f else 0.3f))
        }
        rings = Array(RINGS.size) { ring(it) }
        sheen = paintSheen()
    }

    /**
     * Ring [r]: kites of glass round it, each cut in four faces about its long middle line and its
     * waist, bevelled at its edges, each pair either side of a mirror in the same colour.
     */
    private fun ring(r: Int): Bitmap {
        val cfg = RINGS[r]
        val n = cfg[0].toInt()
        val inner = cfg[1] * radius
        val outer = cfg[2] * radius
        val fill = cfg[3]
        val hue = cfg[4]
        val size = ceil(2f * outer + 4f).toInt()
        val mid = size / 2f
        val sector = TAU / n
        val bevel = 0.006f * w + 1f
        val hv = Shade.half(light)
        val colours = Array(n) { i ->
            // Mirror-matched: facet i and facet n - 1 - i alike, the spectrum run round twice.
            val t = 0.5f + 0.5f * cos(TAU * (2f * i / n) + hue)
            if (dark) k.colour(t, lightness = 1.0f, chroma = 1.05f) else k.colour(t, chroma = 0.95f, atLightness = 0.72f)
        }
        val pale = if (dark) k.colour(0.5f, lightness = 1.35f, chroma = 0.25f) else k.colour(0.5f, chroma = 0.25f, atLightness = 0.97f)
        return sprite(size, size) { x, y, px ->
            val dx = x - mid
            val dy = y - mid
            val rho = sqrt(dx * dx + dy * dy)
            if (rho < inner - 2f || rho > outer + 2f) {
                px[3] = 0f
                return@sprite
            }
            val phi = atan2(dy, dx) + PI.toFloat() / 2f + sector / 2f
            val f = floor(phi / sector)
            val i = Math.floorMod(f.toInt(), n)
            val local = phi - (f + 0.5f) * sector
            val q = abs(local) / (sector / 2f)
            val along = ((rho - inner) / (outer - inner)).coerceIn(0f, 1f)
            // Straight sides to sharp points at both ends, as a cut shard has.
            val widest = fill * (1f - abs(2f * along - 1f))
            val dAng = (q - widest) * rho * sector / 2f
            val dRad = max(inner - rho, rho - outer)
            val d = max(dAng, dRad)
            val cover = Shade.clamp01(0.5f - d)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            // The four faces: tipped across, either side of its middle line, and along, either side of its waist.
            val centre = (f + 0.5f) * sector - PI.toFloat() / 2f - sector / 2f
            val cr = cos(centre)
            val sr = sin(centre)
            val nr = if (along > 0.5f) 0.55f else -0.55f
            val nt = if (local > 0f) 0.55f else -0.55f
            val nx0 = nr * cr - nt * sr
            val ny0 = nr * sr + nt * cr
            val l = 1f / sqrt(nx0 * nx0 + ny0 * ny0 + 1f)
            val nx = nx0 * l
            val ny = ny0 * l
            val wrapped = 0.5f + 0.5f * (nx * light[0] + ny * light[1] + l * light[2])
            val spec = Shade.pow64(Shade.clamp01(nx * hv[0] + ny * hv[1] + l * hv[2]))
            val edge = exp(-max(0f, -d) / bevel)
            val col = colours[i]
            if (dark) {
                val body = 0.2f + 0.8f * wrapped
                for (ch in 0..2) px[ch] = col[ch] * body * 0.78f + pale[ch] * (edge * 0.4f + spec * 0.55f)
                px[3] = 1f
                // Outside the glass nothing is added.
                for (ch in 0..2) px[ch] *= cover
            } else {
                val body = 0.68f + 0.42f * wrapped
                for (ch in 0..2) px[ch] = (col[ch] * body).coerceAtMost(1f) + (pale[ch] - col[ch]) * (edge * 0.55f + spec * 0.6f)
                px[3] = cover * 0.8f
            }
        }
    }

    /** Light lying across the kaleidoscope from the top left, staying put while the glass turns under it. */
    private fun paintSheen(): Bitmap {
        val size = ceil(2f * radius).toInt()
        val mid = size / 2f
        val tint = if (dark) k.colour(0.5f, lightness = 1.3f, chroma = 0.2f) else k.colour(0.5f, chroma = 0.1f, atLightness = 0.99f)
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / radius
            val dy = (y - mid) / radius
            val r = sqrt(dx * dx + dy * dy)
            val band = (dx * 0.6f + dy * 0.8f + 0.25f).let { exp(-(it / 0.22f).let { b -> b * b }) }
            val g = band * (1f - Shade.smoothstep(0.75f, 1f, r)) * (if (dark) 0.12f else 0.3f)
            if (dark) {
                for (ch in 0..2) px[ch] = tint[ch] * g
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = tint[ch]
                px[3] = g
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, back, paint)
        for (r in rings.indices) {
            val b = rings[r]
            val turn = 360f * LiveCurves.turn(seconds, TURN[r].toDouble()) * (if (r % 2 == 0) 1f else -1f)
            matrix.setTranslate(-b.width / 2f, -b.height / 2f)
            matrix.postRotate(turn)
            matrix.postTranslate(cx, cy)
            canvas.drawBitmap(b, matrix, glassPaint)
        }
        drawCentred(canvas, sheen, cx, cy, 1f, glassPaint)
        settle(canvas, 0.64f, if (dark) 0.32f else 0.26f)
    }

    private companion object {
        /** Each ring, outside in: how many facets; inner and outer edge, of the radius; how much of its share each facet fills; where its colours start. */
        val RINGS = arrayOf(
            floatArrayOf(12f, 0.62f, 1.0f, 0.92f, 0f),
            floatArrayOf(8f, 0.35f, 0.66f, 0.92f, 1.6f),
            floatArrayOf(6f, 0.13f, 0.39f, 0.95f, 3.1f),
            floatArrayOf(12f, 0f, 0.15f, 0.98f, 0.8f),
        )

        /** Seconds for one turn of each ring; neighbours turn opposite ways. */
        val TURN = floatArrayOf(150f, 105f, 75f, 240f)
    }
}

// ------------------------------------------------------------------------------------------ Flow

/**
 * Flow: broad satin ribbons rolling across the screen, each turning over on itself here and there
 * and showing its other face, a soft shadow under each. A ribbon is painted once as a strip, its
 * sheen across it and its colours along it, and every frame lays that strip along where its
 * curve has rolled to, narrowing it and darkening it where it turns edge on.
 */
internal class FlowScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val shadowPaint = layerPaint()
    private val verts = FloatArray((SEGS + 1) * (ACROSS + 1) * 2)
    private val tints = IntArray((SEGS + 1) * (ACROSS + 1))
    private val lineX = FloatArray(SEGS + 1)
    private val lineY = FloatArray(SEGS + 1)
    private val turn = FloatArray(SEGS + 1)
    private lateinit var back: Bitmap
    private lateinit var ribbons: Array<Bitmap>
    private lateinit var shadow: Bitmap

    override fun prepare() {
        val glow = if (dark) k.colour(0.5f, lightness = 0.9f) else k.colour(0.5f, chroma = 0.5f, atLightness = 0.93f)
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(v, px, 0) else k.sky(v * 0.8f, px, 0)
            val gx = (x - 0.4f * w) / w
            val gy = (y - 0.3f * h) / w
            Shade.mixInto(px, glow[0], glow[1], glow[2], exp(-(gx * gx + gy * gy) / 0.4f) * (if (dark) 0.12f else 0.28f))
        }
        ribbons = Array(RIBBONS.size) { ribbon(it) }
        shadow = sprite(8, SHADOW_TEX) { _, y, px ->
            val v = y / SHADOW_TEX
            val a = exp(-((v - 0.5f) / 0.3f).let { val s = it * it; s * s })
            px[0] = 0f; px[1] = 0f; px[2] = 0f
            px[3] = a
        }
        shadowPaint.color = if (dark) Color.BLACK else LiveNoise.colour(k.colour(0.5f, chroma = 0.6f, atLightness = 0.5f))
        shadowPaint.alpha = if (dark) 150 else 60
    }

    /** Ribbon [j] as a strip: its colours running along it, a broad sheen a third of the way across it, a fine one near its far edge, its edges falling away. */
    private fun ribbon(j: Int): Bitmap {
        val cfg = RIBBONS[j]
        val lit = if (dark) k.ramp(1.08f, 1f) else k.rampAt(0.84f, 0.8f)
        val deep = if (dark) k.ramp(0.42f, 1.1f) else k.rampAt(0.64f, 1f)
        val gloss = if (dark) k.colour(0.5f, lightness = 1.4f, chroma = 0.2f) else k.colour(0.5f, chroma = 0.15f, atLightness = 0.99f)
        val edge = 0.8f / TEX_ACROSS
        return sprite(TEX_ALONG, TEX_ACROSS) { x, y, px ->
            val u = x / TEX_ALONG
            val v = y / TEX_ACROSS
            val t = cfg[8] + (cfg[9] - cfg[8]) * u
            lit.get(t, px, 4)
            deep.get(t, px, 7)
            val body = sin(PI.toFloat() * v).pow(0.7f)
            val sheen = exp(-((v - 0.3f) / 0.12f).let { it * it }) * 0.55f + exp(-((v - 0.76f) / 0.06f).let { it * it }) * 0.22f
            val s = if (dark) sheen else sheen * 0.8f
            for (ch in 0..2) px[ch] = px[7 + ch] + (px[4 + ch] - px[7 + ch]) * (0.25f + 0.75f * body) + gloss[ch] * s
            px[3] = Shade.smoothstep(0f, edge, v) * Shade.smoothstep(1f, 1f - edge, v)
        }
    }

    /** Lays ribbon [j] along its curve at [seconds], [widen] times as wide and moved by ([dx], [dy]), into [verts], and its shading into [tints]. */
    private fun lay(j: Int, seconds: Double, widen: Float, dx: Float, dy: Float) {
        val cfg = RIBBONS[j]
        val p1 = phase(j * 1.3f, cfg[5], seconds)
        val p2 = phase(j * 2.9f, -cfg[5] * 0.7f, seconds)
        val pt = phase(j * 0.9f, cfg[7], seconds)
        for (i in 0..SEGS) {
            val u = i / SEGS.toFloat()
            lineX[i] = -0.3f * w + 1.6f * w * u
            lineY[i] = h * (cfg[0] + cfg[1] * (u - 0.5f) + cfg[2] * sin(TAU * 0.75f * u + p1) + cfg[2] * 0.4f * sin(TAU * 1.7f * u + p2))
            turn[i] = cos(TAU * 0.5f * cfg[6] * u + pt)
        }
        val half = 0.5f * cfg[3] * w * widen
        for (i in 0..SEGS) {
            val a = max(0, i - 1)
            val b = min(SEGS, i + 1)
            val tx = lineX[b] - lineX[a]
            val ty = lineY[b] - lineY[a]
            val l = hypot(tx, ty).coerceAtLeast(1e-3f)
            val nx = -ty / l
            val ny = tx / l
            // Turning over, it narrows to its edge and widens again showing its other face. Laid
            // in several rows across, so its sheen stays straight where it narrows.
            val across = half * turn[i]
            val facing = sqrt(abs(turn[i]))
            val g = ((if (dark) 0.45f + 0.55f * facing else 0.72f + 0.28f * facing) * 255f).toInt()
            val tint = Color.rgb(g, g, g)
            for (r in 0..ACROSS) {
                val o = across * (1f - 2f * r / ACROSS)
                val at = r * (SEGS + 1) + i
                verts[at * 2] = lineX[i] + nx * o + dx
                verts[at * 2 + 1] = lineY[i] + ny * o + dy
                tints[at] = tint
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, back, paint)
        for (j in RIBBONS.indices) {
            lay(j, seconds, 1.12f, 0.012f * w, 0.022f * w)
            canvas.drawBitmapMesh(shadow, SEGS, ACROSS, verts, 0, null, 0, shadowPaint)
            lay(j, seconds, 1f, 0f, 0f)
            canvas.drawBitmapMesh(ribbons[j], SEGS, ACROSS, verts, 0, tints, 0, paint)
        }
        settle(canvas, 0.6f, if (dark) 0.34f else 0.28f)
    }

    private companion object {
        const val SEGS = 96
        const val ACROSS = 4
        const val TEX_ALONG = 256
        const val TEX_ACROSS = 128
        const val SHADOW_TEX = 64

        /**
         * Each ribbon, back first: its height down the screen; its slant; how far it swells; how
         * wide it is, of the width; unused; how fast its swell rolls and how many turns it makes
         * along its length and how fast they move; and where on the spectrum its two ends are.
         */
        val RIBBONS = arrayOf(
            floatArrayOf(0.17f, 0.12f, 0.035f, 0.16f, 0f, 0.13f, 1.4f, 0.21f, 0f, 0.45f),
            floatArrayOf(0.3f, -0.1f, 0.045f, 0.2f, 0f, -0.1f, 1.1f, -0.17f, 0.35f, 0.85f),
            floatArrayOf(0.43f, 0.16f, 0.04f, 0.15f, 0f, 0.16f, 1.7f, 0.25f, 0.6f, 1f),
            floatArrayOf(0.54f, -0.08f, 0.03f, 0.11f, 0f, -0.12f, 1.2f, -0.2f, 0.15f, 0.7f),
        )
    }
}

// ----------------------------------------------------------------------------------------- Jelly

/**
 * Jelly: jellyfish drifting up through deep water, each bell pulsing, its trailing threads waving
 * behind it, while specks drift in the light from above. Each bell is lit once, glowing at night as
 * the deep sea's do and like glass by day, and its threads are a strip bent along a travelling wave.
 */
internal class JellyScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val matrix = Matrix()
    private val verts = FloatArray(2 * (ROWS + 1) * 2)
    private lateinit var back: Bitmap
    private lateinit var bells: Array<Bitmap>
    private lateinit var halos: Array<Bitmap>
    private lateinit var threads: Array<Bitmap>
    private lateinit var arms: Array<Bitmap>
    private lateinit var specks: FloatArray

    override fun prepare() {
        val shine = if (dark) k.colour(0.45f, lightness = 0.95f, chroma = 0.8f) else k.colour(0.45f, chroma = 0.4f, atLightness = 0.96f)
        back = field(4) { x, y, px ->
            val v = y / h
            if (dark) k.dusk(0.2f + 0.8f * v, px, 0) else k.sky(v * 0.8f, px, 0)
            if (dark) for (ch in 0..2) px[ch] *= 0.75f + 0.25f * (1f - v)
            // Light from the surface: a glow at the top and soft shafts slanting down.
            val top = exp(-v / 0.22f) * (if (dark) 0.16f else 0.3f)
            val u = x / w - 0.25f * v
            val shafts = (0.5f + 0.5f * sin(u * 19f + 1.3f)) * (0.5f + 0.5f * sin(u * 31f + 4f)) * exp(-v / 0.5f) * (if (dark) 0.07f else 0.12f)
            Shade.mixInto(px, shine[0], shine[1], shine[2], Shade.clamp01(top + shafts))
        }
        bells = Array(KINDS) { bell(it) }
        halos = Array(KINDS) { halo(it) }
        threads = Array(KINDS) { strands(it, arms = false) }
        arms = Array(KINDS) { strands(it, arms = true) }
        val random = Random(29)
        specks = FloatArray(SPECKS * 4)
        for (i in 0 until SPECKS) {
            specks[i * 4] = random.nextFloat()
            specks[i * 4 + 1] = random.nextFloat()
            specks[i * 4 + 2] = 0.004f + 0.01f * random.nextFloat()
            specks[i * 4 + 3] = random.nextFloat()
        }
    }

    private fun hue(kind: Int): Float = if (kind == 0) 0.15f else if (kind == 1) 0.85f else 0.5f

    /**
     * A bell, [BELL] pixels to its radius times the scale: a dome, scalloped at its rim, see-through
     * in its middle and bright where it curves away, with four rings inside it and fine canals
     * running out to the rim.
     */
    private fun bell(kind: Int): Bitmap {
        val r = BELL * w
        val sw = ceil(2.3f * r).toInt()
        val sh = ceil(1.5f * r).toInt()
        val mx = sw / 2f
        val top = 0.12f * r
        val t = hue(kind)
        val body = if (dark) k.colour(t, lightness = 1.1f, chroma = 0.8f) else k.colour(t, chroma = 0.8f, atLightness = 0.74f)
        val rim = if (dark) k.colour(t, lightness = 1.35f, chroma = 0.45f) else k.colour(t, chroma = 0.95f, atLightness = 0.52f)
        val organ = if (dark) k.colour(1f - t, lightness = 1.25f, chroma = 0.9f) else k.colour(1f - t, chroma = 1f, atLightness = 0.56f)
        return sprite(sw, sh) { x, y, px ->
            val dx = (x - mx) / r
            val dy = (y - top) / r - 1f
            // The dome: an ellipse whose lower part is cut by the scalloped rim.
            val e = sqrt(dx * dx + dy * dy * 1.15f)
            val scallop = 0.035f * (0.5f + 0.5f * cos(dx * 26f))
            val rimLine = 0.08f + scallop
            val dIn = max(e - 1f, dy - rimLine) * r
            val cover = Shade.clamp01(0.5f - dIn)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val edge = Shade.smoothstep(0.55f, 1f, e) + Shade.smoothstep(-0.05f, rimLine, dy) * 0.6f
            val phi = atan2(dy + 0.35f, dx)
            val canal = (0.5f + 0.5f * cos(phi * 16f)).let { val c2 = it * it; c2 * c2 * c2 } * Shade.smoothstep(0.3f, 0.9f, e) * 0.35f
            // A soft glow in the top of the bell, where its stomach is.
            val rings = exp(-((dx / 0.36f).let { it * it } + ((dy + 0.42f) / 0.2f).let { it * it }))
            if (dark) {
                for (ch in 0..2) px[ch] = (body[ch] * (0.16f + 0.5f * edge) + rim[ch] * edge * 0.45f + organ[ch] * rings * 0.55f + rim[ch] * canal * 0.4f) * cover
                px[3] = 1f
            } else {
                val a = Shade.clamp01(0.3f + 0.6f * edge + 0.5f * rings + canal * 0.3f)
                for (ch in 0..2) px[ch] = body[ch] + (rim[ch] - body[ch]) * Shade.clamp01(edge * 0.8f) + (organ[ch] - body[ch]) * rings * 0.7f
                px[3] = a * cover * 0.9f
            }
        }
    }

    /** The glow round a bell at night, or by day the faint shade of it in the water. */
    private fun halo(kind: Int): Bitmap {
        val r = BELL * w
        val size = ceil(3.2f * r).toInt()
        val mid = size / 2f
        val colour = if (dark) k.colour(hue(kind), lightness = 1f, chroma = 0.9f) else k.colour(hue(kind), chroma = 0.6f, atLightness = 0.85f)
        return sprite(size, size) { x, y, px ->
            val d = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)) / r
            val g = exp(-(d / 0.9f).let { it * it }) * (1f - Shade.smoothstep(1.2f, 1.6f, d))
            if (dark) {
                for (ch in 0..2) px[ch] = colour[ch] * g * 0.35f
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = colour[ch]
                px[3] = g * 0.3f
            }
        }
    }

    /**
     * The threads trailing from a bell as a strip, top to bottom: fine strands that fade along their
     * length, or for [arms], four broader frilled ones from its middle.
     */
    private fun strands(kind: Int, arms: Boolean): Bitmap {
        val across = 64
        val down = 256
        val count = if (arms) 4 else 9
        val t = hue(kind)
        val colour = if (dark) k.colour(if (arms) 1f - t else t, lightness = 1.15f, chroma = 0.7f) else k.colour(if (arms) 1f - t else t, chroma = 0.85f, atLightness = 0.58f)
        return sprite(across, down) { x, y, px ->
            val u = x / across
            val v = y / down
            var g = 0f
            for (s in 0 until count) {
                val at = (s + 0.5f) / count + 0.03f * sin(v * 9f + s * 1.7f)
                val width = if (arms) 0.06f * (1f - 0.6f * v) * (0.7f + 0.3f * sin(v * 40f + s)) else 0.012f
                g = max(g, exp(-((u - at) / width).let { it * it }))
            }
            val fade = (1f - v).pow(if (arms) 1.6f else 1.1f) * Shade.smoothstep(0f, 0.04f, v)
            if (dark) {
                for (ch in 0..2) px[ch] = colour[ch] * g * fade * (if (arms) 0.55f else 0.7f)
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = colour[ch]
                px[3] = g * fade * (if (arms) 0.55f else 0.7f)
            }
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, back, paint)
        // Specks drifting slowly up through the light.
        dots.color = if (dark) Color.WHITE else LiveNoise.colour(k.colour(0.5f, chroma = 0.5f, atLightness = 0.6f))
        for (i in 0 until SPECKS) {
            val x = specks[i * 4] * w + 0.01f * w * sin(phase(i * 1.3f, 0.4f, seconds))
            val y = wrap(specks[i * 4 + 1] * h - specks[i * 4 + 2] * h * seconds, h.toDouble())
            dots.strokeWidth = (2f + 2f * specks[i * 4 + 3]) * unit
            dots.alpha = ((if (dark) 0.35f else 0.25f) * (0.5f + 0.5f * specks[i * 4 + 3]) * 255f).toInt()
            canvas.drawPoint(x, y, dots)
        }
        for (j in SWARM.indices) drawJelly(canvas, j, seconds)
        settle(canvas, 0.62f, if (dark) 0.36f else 0.28f)
    }

    /** Jelly [j] at [seconds]: where it has risen to, how far into its beat it is, and its threads waving behind it. */
    private fun drawJelly(canvas: Canvas, j: Int, seconds: Double) {
        val cfg = SWARM[j]
        val kind = cfg[2].toInt()
        val size = cfg[3]
        val period = cfg[5].toDouble()
        val beat = ((seconds / period + cfg[6]) % 1.0).toFloat()
        // A beat: the bell squeezes quickly and lets go slowly.
        val squeeze = Shade.smoothstep(0f, 0.22f, beat) * (1f - Shade.smoothstep(0.22f, 0.9f, beat))
        val bw = size * (1f - 0.14f * squeeze)
        val bh = size * (1f + 0.1f * squeeze)
        val span = h * 1.5f
        val rise = cfg[4] * h * seconds + cfg[4] * h * period * 0.3 * squeezeSoFar(seconds, period, cfg[6])
        val y = wrap(cfg[1] * span - rise, span.toDouble()) - 0.25f * h
        val x = cfg[0] * w + 0.03f * w * sin(phase(j * 2.1f, 0.13f, seconds))
        val lean = 6f * sin(phase(j * 0.8f, 0.17f, seconds))
        val bell = bells[kind]
        val r = BELL * w
        val alpha = cfg[7]
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(lean)
        drawCentred(canvas, halos[kind], 0f, 0.55f * r * size, size, glowPaint, alpha)
        // Threads first, behind the bell, from its rim.
        // The rim: the bell's top sits 0.12 of its radius down its layer, its rim 1.08 radii below that.
        val rimY = 1.2f * r * bh
        val sway = phase(j * 1.7f, TAU / 3.1f, seconds)
        bend(rimY, 1.6f * r * bw, r * size * cfg[8], sway, beat, 0.9f)
        glowPaint.alpha = (alpha * 255f).toInt()
        canvas.drawBitmapMesh(threads[kind], 1, ROWS, verts, 0, null, 0, glowPaint)
        bend(rimY - 0.1f * r * bh, 0.7f * r * bw, r * size * cfg[8] * 0.55f, sway + 1.1f, beat, 0.6f)
        canvas.drawBitmapMesh(arms[kind], 1, ROWS, verts, 0, null, 0, glowPaint)
        glowPaint.alpha = 255
        matrix.setTranslate(-bell.width / 2f, 0f)
        matrix.postScale(bw, bh)
        canvas.concat(matrix)
        if (dark) {
            glowPaint.alpha = (alpha * 255f).toInt()
            canvas.drawBitmap(bell, 0f, 0f, glowPaint)
            glowPaint.alpha = 255
        } else {
            paint.alpha = (alpha * 255f).toInt()
            canvas.drawBitmap(bell, 0f, 0f, paint)
            paint.alpha = 255
        }
        canvas.restore()
    }

    /** How many beats' worth of squeeze have gone by at [seconds]: the bell moves up most while it squeezes. */
    private fun squeezeSoFar(seconds: Double, period: Double, start: Float): Double {
        val beats = seconds / period + start
        val whole = floor(beats)
        val part = (beats - whole).toFloat()
        return whole + Shade.smoothstep(0f, 0.35f, part) - start
    }

    /** A strip [long] pixels down from [top], [wide] across at the top, bent along a wave travelling down it, into [verts]. */
    private fun bend(top: Float, wide: Float, long: Float, sway: Float, beat: Float, reach: Float) {
        for (i in 0..ROWS) {
            val s = i / ROWS.toFloat()
            val y = top + long * s
            val wave = sin(sway - s * 5.5f) * 0.12f * long * s.pow(1.2f) * reach
            val pulse = 1f + 0.18f * sin(TAU * beat - s * 3f) * s
            val half = 0.5f * wide * (1f - 0.35f * s) * pulse
            verts[i * 4] = -half + wave
            verts[i * 4 + 1] = y
            verts[i * 4 + 2] = half + wave
            verts[i * 4 + 3] = y
        }
    }

    private companion object {
        const val KINDS = 3
        const val ROWS = 24
        const val SPECKS = 70

        /** A bell's radius, of the width, at a scale of 1. */
        const val BELL = 0.13f

        /**
         * Each jelly, far first: across; where it starts, of its round trip; which kind; how big;
         * how fast it rises, of the height a second; seconds to a beat; where in its beat it
         * starts; how clearly it shows; how long its threads are, in bell radii.
         */
        val SWARM = arrayOf(
            floatArrayOf(0.78f, 0.22f, 2f, 0.45f, 0.006f, 3.6f, 0.2f, 0.55f, 2.6f),
            floatArrayOf(0.2f, 0.34f, 1f, 0.55f, 0.007f, 3.2f, 0.6f, 0.65f, 2.8f),
            floatArrayOf(0.62f, 0.48f, 0f, 0.8f, 0.009f, 3.9f, 0.0f, 0.85f, 3f),
            floatArrayOf(0.3f, 0.97f, 2f, 1.0f, 0.011f, 4.4f, 0.45f, 0.95f, 3.2f),
            floatArrayOf(0.86f, 0.64f, 1f, 0.7f, 0.008f, 3.4f, 0.75f, 0.8f, 2.9f),
        )
    }
}

// ---------------------------------------------------------------------------------------- Clouds

/**
 * Clouds: a sky of soft clouds sliding past at three depths, the far ones slow, the sea of cloud
 * below quicker, their tops lit by the moon, with the stars out above; by day, by the sun in a pale
 * sky. Each layer is a strip of cloud that comes round again, painted once and slid along.
 */
internal class CloudsScene(c: WallColors, w: Float, h: Float) : LiveScene(c, w, h) {

    private val k = DepthKit(c)
    private val paint = layerPaint()
    private val glowPaint = layerPaint().apply { if (dark) blendMode = BlendMode.PLUS }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; color = Color.WHITE }
    private val mx = 0.7f * w
    private val my = 0.15f * h
    private val strip = RectF()
    private lateinit var sky: Bitmap
    private lateinit var layers: Array<Bitmap>
    private lateinit var moon: Bitmap
    private lateinit var glow: Bitmap
    private lateinit var stars: FloatArray

    override fun prepare() {
        // By day a blue sky overhead, paling to the haze over the clouds.
        val overhead = k.colour(0.6f, chroma = 0.45f, atLightness = 0.85f)
        sky = field(4) { _, y, px ->
            val v = y / h
            if (dark) {
                k.dusk(v * 0.9f, px, 0)
            } else {
                k.sky(v * 0.75f, px, 0)
                Shade.mixInto(px, overhead[0], overhead[1], overhead[2], 0.75f * (1f - Shade.smoothstep(0f, 0.5f, v)))
            }
        }
        moon = paintMoon()
        glow = paintGlow()
        layers = Array(LAYERS.size) { layer(it) }
        val random = Random(53)
        stars = FloatArray(STARS * 2)
        for (i in 0 until STARS) {
            stars[i * 2] = random.nextFloat() * w
            stars[i * 2 + 1] = random.nextFloat().pow(1.4f) * h * 0.45f
        }
    }

    /** The moon, or by day the sun: a disc with faint markings, or a bright soft one. */
    private fun paintMoon(): Bitmap {
        val r = 0.055f * w
        val size = ceil(2f * r + 4f).toInt()
        val mid = size / 2f
        val face = if (dark) k.colour(0.4f, chroma = 0.12f, atLightness = 0.95f) else k.colour(0.3f, chroma = 0.15f, atLightness = 0.995f)
        val sea = if (dark) k.colour(0.6f, chroma = 0.12f, atLightness = 0.8f) else face
        return sprite(size, size) { x, y, px ->
            val dx = (x - mid) / r
            val dy = (y - mid) / r
            val d = sqrt(dx * dx + dy * dy)
            val cover = Shade.clamp01((1f - d) * r + 0.5f)
            if (cover <= 0f) {
                px[3] = 0f
                return@sprite
            }
            val marks = if (dark) Shade.smoothstep(0.55f, 0.7f, LiveNoise.fbm(dx * 2.2f + 4f, dy * 2.2f + 1f)) * 0.7f else 0f
            val limb = sqrt(max(0f, 1f - d * d)).pow(0.25f)
            for (ch in 0..2) px[ch] = (face[ch] + (sea[ch] - face[ch]) * marks) * (0.85f + 0.15f * limb)
            px[3] = cover
        }
    }

    /** The glow round it, painted at half size. */
    private fun paintGlow(): Bitmap {
        val reach = 0.7f * w
        val size = ceil(reach).toInt()
        val mid = size / 2f
        val colour = if (dark) k.colour(0.45f, lightness = 1.1f, chroma = 0.5f) else k.colour(0.3f, chroma = 0.4f, atLightness = 0.98f)
        return sprite(size, size) { x, y, px ->
            val r = sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)) * 2f / w
            val g = (exp(-(r / 0.09f).let { it * it }) * 0.6f + exp(-r / 0.16f) * (if (dark) 0.35f else 0.5f)) * (1f - Shade.smoothstep(0.5f, 0.7f, r))
            if (dark) {
                for (ch in 0..2) px[ch] = colour[ch] * g * 0.5f
                px[3] = 1f
            } else {
                for (ch in 0..2) px[ch] = colour[ch]
                px[3] = Shade.clamp01(g * 0.8f)
            }
        }
    }

    /**
     * Layer [j] of cloud as a strip twice the width of the screen that comes round again, painted at
     * half size: lumpy cloud, fuller towards its foot, its tops catching the light from the moon's
     * side, its undersides in shade and its thin edges lit, as a silver lining is. Nothing shows at
     * the top of the strip, and every layer but the nearest thins out into mist towards its foot,
     * so no strip ever shows an edge.
     */
    private fun layer(j: Int): Bitmap {
        val cfg = LAYERS[j]
        val across = ceil(w).toInt()
        val down = ceil(cfg[1] * h / 2f).toInt().coerceAtLeast(2)
        val cells = cfg[2].toInt()
        val scale = across / cells.toFloat()
        val nearest = j == LAYERS.size - 1
        val lit = if (dark) k.colour(0.45f, chroma = 0.35f, atLightness = 0.7f + 0.05f * j) else k.colour(0.4f, chroma = 0.05f, atLightness = 0.99f)
        // In shade a cloud is a little lighter than the sky behind it, so it never sinks into a dark shelf.
        val shade = if (dark) FloatArray(3).also { k.dusk(0.45f + 0.2f * j, it); for (ch in 0..2) it[ch] *= 1.7f } else k.colour(0.6f, chroma = 0.4f, atLightness = 0.8f + 0.03f * j)
        val tint = if (dark) k.colour(cfg[4], lightness = 0.75f, chroma = 0.9f) else k.colour(cfg[4], chroma = 0.6f, atLightness = 0.88f)
        val lining = if (dark) k.colour(0.45f, chroma = 0.3f, atLightness = 0.88f) else k.colour(0.4f, chroma = 0.02f, atLightness = 1f)
        val full = cfg[5]
        val seed = j * 7.3f
        return sprite(across, down) { x, y, px ->
            val v = y / down
            val nx = x / scale
            val ny = y / scale + seed
            // Fuller towards the foot, with lumpy tops.
            val density = LiveNoise.fbmTiled(nx, ny, cells) + full * (v - 0.5f) + 0.1f
            var a = Shade.smoothstep(0.465f, 0.535f, density) * Shade.smoothstep(0f, 0.12f, v)
            if (!nearest) a *= 1f - Shade.smoothstep(0.55f, 1f, v)
            if (a <= 0f) {
                px[3] = 0f
                return@sprite
            }
            // Lit from above: thinner cloud just above means a top in the light.
            val above = LiveNoise.fbmTiled(nx + 0.06f, ny - 0.12f, cells) + full * (v - 0.04f - 0.5f) + 0.1f
            val litBy = Shade.clamp01((density - above) * 4f + 0.35f)
            val inner = Shade.smoothstep(0.5f, 0.9f, density)
            val thin = 1f - Shade.smoothstep(0.465f, 0.6f, density)
            for (ch in 0..2) {
                var col = shade[ch] + (lit[ch] - shade[ch]) * litBy * (1f - 0.35f * inner)
                col += (tint[ch] - col) * (if (dark) 0.18f else 0.22f) * (1f - litBy)
                col += (lining[ch] - col) * thin * litBy * (if (dark) 0.55f else 0.5f)
                px[ch] = col
            }
            px[3] = a * cfg[3]
        }
    }

    override fun draw(canvas: Canvas, seconds: Double, open: Float) {
        drawField(canvas, sky, paint)
        if (dark) {
            for (g in 0..1) {
                dots.strokeWidth = (1.7f + g) * unit
                dots.alpha = ((0.55f + 0.3f * (0.5f + 0.5f * LiveCurves.swing(seconds, 3.3 + g * 1.9, g * 0.4))) * 255f).toInt()
                canvas.drawPoints(stars, g * STARS, STARS, dots)
            }
        }
        drawCentred(canvas, glow, mx, my, 2f, glowPaint)
        drawCentred(canvas, moon, mx, my, 1f, paint)
        for (j in LAYERS.indices) {
            val cfg = LAYERS[j]
            val b = layers[j]
            val tile = b.width * 2f
            val off = wrap(cfg[6] * w * seconds, tile.toDouble())
            val top = cfg[0] * h
            val dh = b.height * 2f
            for (n in 0..1) {
                val left = n * tile - off
                if (left > w || left + tile < 0f) continue
                strip.set(left, top, left + tile, top + dh)
                canvas.drawBitmap(b, null, strip, paint)
            }
        }
        settle(canvas, 0.72f, if (dark) 0.3f else 0.2f)
    }

    private companion object {
        const val STARS = 70

        /**
         * Each layer, far first: where its top is and how tall it is, of the height; how many lumps
         * across its round; how much shows; where on the spectrum it is tinted; how much fuller it
         * grows towards its foot; and how fast it slides, of the width a second.
         */
        val LAYERS = arrayOf(
            floatArrayOf(0.24f, 0.17f, 6f, 0.75f, 0.2f, 0.7f, 0.004f),
            floatArrayOf(0.33f, 0.25f, 5f, 0.9f, 0.8f, 0.9f, 0.008f),
            floatArrayOf(0.32f, 0.7f, 4f, 1f, 0.5f, 1.6f, 0.014f),
        )
    }
}
