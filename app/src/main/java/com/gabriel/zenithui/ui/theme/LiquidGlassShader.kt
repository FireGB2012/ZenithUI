package com.gabriel.zenithui.ui.theme

import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntSize
import com.gabriel.zenithui.data.GlassLevels
import kotlin.math.hypot
import kotlin.math.max

/**
 * The lens itself, on Android 13 and up: the wallpaper seen through a pane of Liquid Glass, after
 * Apple's. Apple's glass is not a jelly slab that is shaded and glows; it is clear, thin and hard,
 * and almost everything that says "glass" happens in a narrow band at its edge. So, in order:
 *
 * It bends what is behind it, but only near the rim. The middle is flat and shows the wallpaper
 * exactly where it is. In a band round the edge ([lensBand]) the glass curves away like the rounded
 * edge of a thick lens, so what is behind is drawn in from further inside: magnified, and right at
 * the edge squeezed and turned over, which is the bright, bent sliver iOS's glass has at its rim.
 * A bigger pane is a thicker slab and bends harder ([glassThickness]). Clear glass splits a whisper
 * of colour where it bends most.
 *
 * It softens it, with a true blur, from barely misted (Clear) to frosted (Regular) and more for a
 * sheet ([GlassLook.frost]); and it makes its colour richer rather than greyer ([GlassLook.vibrance]).
 *
 * It keeps words on it readable: a thin veil, which thickens only where what is behind would fight
 * the words ([GlassLook.adapt]), as iOS shifts the glass's tint and range under its labels.
 *
 * It catches the light on its edge, and nowhere else: a hairline of light hugging the outline, at
 * its brightest on the corner facing the lamp and again on the corner opposite, where light that
 * crossed the glass leaves it, and faint everywhere between, with a soft glow just inside it. The
 * light swings as the phone tilts. Its colour is a little of what is round the pane ([GlassLook.glow]).
 *
 * A finger on the glass lights it from within, the glow starting under the fingertip and spreading
 * out. A ring of light from a tap on the wallpaper (see [GlassRing]) lights its rim, and a little of
 * its face, where the ring's band crosses it.
 *
 * One shader for the wallpaper in [levels], built once and shared by every pane (see `sharedLens`):
 * each pane sets every number on it before it draws, and Android copies those numbers into the
 * drawing as it is recorded, so no pane is drawn with another's.
 */
@RequiresApi(33)
internal class LiquidGlassShader(val levels: GlassLevels) {
    private val sharpSize = sizeOf(levels.sharp)
    private val mistSize = sizeOf(levels.mist)
    private val frostSize = sizeOf(levels.frost)
    private val hazeSize = sizeOf(levels.haze)
    private val shader = RuntimeShader(PROGRAM).apply {
        setInputShader("wallpaper", bitmapShader(levels.sharp))
        setInputShader("misted", bitmapShader(levels.mist))
        setInputShader("frosted", bitmapShader(levels.frost))
        setInputShader("hazed", bitmapShader(levels.haze))
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = this@LiquidGlassShader.shader }

    fun draw(
        canvas: Canvas,
        size: Size,
        outline: Outline,
        /** Top-left of the pane on screen, in pixels. */
        origin: Offset,
        screen: IntSize,
        /** Android's wallpaper scroll, 0..1 on each axis, as the launcher reports it. */
        wallpaperOffset: Offset,
        density: Float,
        look: GlassLook,
        /** Where the light comes from, tilted by the phone; a unit-ish vector in screen space. */
        light: Offset,
        dark: Boolean,
        /** A finger on the glass, in pane pixels, or [GlassTouch.None]. */
        touch: GlassTouch = GlassTouch.None,
        /** The exponent of a free-form outline's superellipse: see [squircleExponent]. */
        squircleN: Float = SQUIRCLE_EXPONENT,
        /** A ring of light from a tap on the wallpaper, its centre in pane pixels, or null for none. */
        ring: RingNow? = null,
        /** How much darker the wallpaper is drawn under the home screen ([LocalWallpaperDim]): 0 for not at all. */
        dim: Float = 0f,
    ) {
        if (size.width <= 0f || size.height <= 0f || screen.width <= 0 || screen.height <= 0) return
        // The wallpaper as Android shows it: centre-cropped to fill the screen, zoomed a little
        // on newer versions to leave room for parallax, and sliding with the pages.
        val scale = max(screen.width / sharpSize.width, screen.height / sharpSize.height) * SYSTEM_WALLPAPER_ZOOM
        val cropX = (sharpSize.width * scale - screen.width) * wallpaperOffset.x.coerceIn(0f, 1f)
        val cropY = (sharpSize.height * scale - screen.height) * wallpaperOffset.y.coerceIn(0f, 1f)
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("origin", origin.x + cropX, origin.y + cropY)
        shader.setFloatUniform("imageScale", 1f / scale)
        shader.setFloatUniform("mistRatio", mistSize.width / sharpSize.width, mistSize.height / sharpSize.height)
        shader.setFloatUniform("frostRatio", frostSize.width / sharpSize.width, frostSize.height / sharpSize.height)
        shader.setFloatUniform("hazeRatio", hazeSize.width / sharpSize.width, hazeSize.height / sharpSize.height)
        shader.setFloatUniform("density", density)
        // How wide the band is that bends, and how thick a slab the pane is: worked out here, by the
        // rules the tests hold, rather than in the shader.
        shader.setFloatUniform("band", lensBand(size, density, (outline as? Outline.Rounded)?.roundRect))
        shader.setFloatUniform("thick", glassThickness(size, density))
        shader.setFloatUniform("refraction", look.refraction)
        shader.setFloatUniform("frost", look.frost.coerceIn(0f, 3f))
        shader.setFloatUniform("dispersion", look.dispersion)
        shader.setFloatUniform("shine", look.shine)
        shader.setFloatUniform("vibrance", look.vibrance)
        shader.setFloatUniform("glow", look.glow.coerceIn(0f, 1f))
        shader.setFloatUniform("rimmed", if (look.rimmed) 1f else 0f)
        val length = hypot(light.x, light.y).coerceAtLeast(0.001f)
        shader.setFloatUniform("light", light.x / length, light.y / length)
        shader.setFloatUniform("dark", if (dark) 1f else 0f)
        val wash = look.wash
        shader.setFloatUniform("wash", wash.red, wash.green, wash.blue, wash.alpha)
        shader.setFloatUniform("adapt", look.adapt)
        val lift = look.lift
        shader.setFloatUniform("lift", lift.red, lift.green, lift.blue, lift.alpha)
        // Glass materialises by bending light, not by fading: a pane on its way in bends less.
        shader.setFloatUniform("presence", look.opacity.coerceIn(0f, 1f))
        shader.setFloatUniform("touch", touch.at.x, touch.at.y, touch.reach, touch.strength)
        // Set on every draw, so a pane that is not made from the wallpaper never keeps one's tint.
        shader.setFloatUniform("wallTint", look.wallTint.coerceIn(0f, 1f))
        // Set on every draw, whatever the outline, so a pane is never left with another pane's.
        shader.setFloatUniform("squircleN", squircleN)
        // Set on every draw too, so a pane the ring is not crossing never keeps another pane's band.
        shader.setFloatUniform("ringCentre", ring?.centre?.x ?: 0f, ring?.centre?.y ?: 0f)
        shader.setFloatUniform("ringRadius", ring?.radius ?: 0f)
        shader.setFloatUniform("ringWidth", ring?.width ?: 1f)
        shader.setFloatUniform("ringStrength", (ring?.strength ?: 0f).coerceIn(0f, 1f))
        // Set on every draw, so glass off the home screen never keeps the home screen's dimming.
        shader.setFloatUniform("dim", dim.coerceIn(0f, 1f))
        when (outline) {
            is Outline.Rounded -> {
                val r = outline.roundRect
                shader.setFloatUniform(
                    "corners",
                    r.topLeftCornerRadius.x, r.topRightCornerRadius.x,
                    r.bottomRightCornerRadius.x, r.bottomLeftCornerRadius.x,
                )
                shader.setFloatUniform("squircle", 0f)
            }
            is Outline.Generic -> {
                // The free-form outlines a pane has are icon tiles, drawn as a superellipse: the
                // squircle, or the iPhone's fuller one, as squircleN says.
                shader.setFloatUniform("corners", 0f, 0f, 0f, 0f)
                shader.setFloatUniform("squircle", 1f)
            }
            is Outline.Rectangle -> {
                shader.setFloatUniform("corners", 0f, 0f, 0f, 0f)
                shader.setFloatUniform("squircle", 0f)
            }
        }
        paint.alpha = (look.opacity.coerceIn(0f, 1f) * 255f).toInt()
        canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }

    private fun sizeOf(image: ImageBitmap) = Size(image.width.toFloat(), image.height.toFloat())

    private fun bitmapShader(image: ImageBitmap) =
        BitmapShader(image.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }

    companion object {
        /** Everything in panel pixels until the wallpaper is sampled, so the rim is the same thickness on a pill and a sheet. */
        internal const val PROGRAM = """
            uniform shader wallpaper;
            uniform shader misted;
            uniform shader frosted;
            uniform shader hazed;
            uniform float2 size;
            uniform float2 origin;
            uniform float imageScale;
            uniform float2 mistRatio;
            uniform float2 frostRatio;
            uniform float2 hazeRatio;
            uniform float4 corners;
            uniform float squircle;
            uniform float squircleN;
            uniform float density;
            uniform float band;
            uniform float thick;
            uniform float refraction;
            uniform float frost;
            uniform float dispersion;
            uniform float shine;
            uniform float vibrance;
            uniform float glow;
            uniform float rimmed;
            uniform float2 light;
            uniform float dark;
            uniform float4 wash;
            uniform float adapt;
            uniform float4 lift;
            uniform float presence;
            uniform float4 touch;
            uniform float wallTint;
            uniform float2 ringCentre;
            uniform float ringRadius;
            uniform float ringWidth;
            uniform float ringStrength;
            uniform float dim;

            const float3 LUMA = float3(0.2126, 0.7152, 0.0722);
            // How far in from the outline, in dp, the light on the edge reaches before it is gone.
            const float RIM_REACH = 24.0;

            // Signed distance to the outline, negative inside, in pixels. Its corners are rounded out
            // to at least [round], which is how the direction the rim faces is found: from a
            // rounder outline, so it turns smoothly round a corner instead of snapping at it.
            float edgeDistance(float2 point, float round) {
                float2 halfSize = size * 0.5;
                float2 p = point - halfSize;
                if (squircle > 0.5) {
                    float2 v = abs(p) / max(halfSize, float2(1.0));
                    float k = pow(pow(v.x, squircleN) + pow(v.y, squircleN), 1.0 / squircleN);
                    return (k - 1.0) * min(halfSize.x, halfSize.y);
                }
                float radius = p.y < 0.0
                    ? (p.x < 0.0 ? corners.x : corners.y)
                    : (p.x < 0.0 ? corners.w : corners.z);
                radius = min(max(radius, round), min(halfSize.x, halfSize.y));
                float2 q = abs(p) - halfSize + radius;
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
            }

            // Which way the outline faces near [point], outwards: the way [edgeDistance] grows, worked
            // out exactly rather than measured either side of the point. Its corners are rounded out
            // to at least [round], so that it turns smoothly round a corner instead of snapping at it.
            float2 rimNormal(float2 point, float round) {
                float2 halfSize = size * 0.5;
                float2 p = point - halfSize;
                float2 side = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
                if (squircle > 0.5) {
                    float2 scale = max(halfSize, float2(1.0));
                    float2 v = abs(p) / scale;
                    float2 g = side * pow(v, float2(squircleN - 1.0)) / scale;
                    return g / max(length(g), 0.0001);
                }
                float radius = p.y < 0.0
                    ? (p.x < 0.0 ? corners.x : corners.y)
                    : (p.x < 0.0 ? corners.w : corners.z);
                radius = min(max(radius, round), min(halfSize.x, halfSize.y));
                float2 q = abs(p) - halfSize + radius;
                if (q.x > 0.0 && q.y > 0.0) {
                    return side * normalize(q);
                }
                return q.x > q.y ? float2(side.x, 0.0) : float2(0.0, side.y);
            }

            // What is behind a point, as soft as asked: 0 is the picture as it is, 3 little but
            // its colours. The pictures either side of that softness are blended, which is a true
            // blur of any strength for the price of two lookups.
            float3 picture(float2 point, float soft) {
                float2 uv = (point + origin) * imageScale;
                if (soft < 1.0) {
                    return mix(wallpaper.eval(uv).rgb, misted.eval(uv * mistRatio).rgb, max(soft, 0.0));
                }
                if (soft < 2.0) {
                    return mix(misted.eval(uv * mistRatio).rgb, frosted.eval(uv * frostRatio).rgb, soft - 1.0);
                }
                return mix(frosted.eval(uv * frostRatio).rgb, hazed.eval(uv * hazeRatio).rgb, min(soft - 2.0, 1.0));
            }

            // The wallpaper as the screen shows it: under a dimming veil, when one is asked for.
            float3 behind(float2 point, float soft) {
                return picture(point, soft) * (1.0 - dim);
            }

            half4 main(float2 point) {
                float d = edgeDistance(point, 0.0);
                float inside = max(-d, 0.0);
                float px = density;

                // Which way the rim faces here, outwards, and the light on the edge: a hairline hugging
                // the outline and a softer glow just inside it. Only the rim has them. Past the band
                // and the glow, which has faded to nothing [RIM_REACH] in, nothing on the pane needs
                // them, so most of a big pane is spared working them out.
                float2 normal = float2(0.0);
                float hairline = 0.0;
                float sheen = 0.0;
                if (rimmed > 0.5 && inside < max(band, RIM_REACH * px)) {
                    normal = rimNormal(point, band * 1.5);
                    hairline = exp(-inside / (0.8 * px));
                    sheen = exp(-inside / (3.5 * px));
                }

                // The lens. 0 where the band starts, 1 at the very edge. A pane that runs off the
                // screen has no rim: it is all flat middle.
                float x = clamp(1.0 - inside / max(band, 1.0), 0.0, 1.0) * rimmed;
                // The edge of a convex lens: flat where the band starts, curving away ever faster to
                // the outline, so the bend is gentle at first and hard at the edge.
                float curve = 1.0 - sqrt(max(1.0 - x * x, 0.0));
                // What is behind is drawn in from further inside (a convex lens magnifies), by more
                // than the band is wide right at the edge, where it turns over. A thicker slab bends
                // harder. Glass coming into being bends less ([presence]): it materialises by bending.
                float reach = band * mix(1.25, 1.75, thick) * refraction * presence;
                float2 bend = -normal * reach * curve;

                float soft = frost;
                float3 color;
                if (x > 0.0 && dispersion > 0.0) {
                    // Blue bends a little more than red, so the turned-over sliver at the edge has a
                    // faint fringe of colour, as real glass has where it bends hardest.
                    float spread = dispersion * curve;
                    color = float3(
                        behind(point + bend * (1.0 - spread), soft).r,
                        behind(point + bend, soft).g,
                        behind(point + bend * (1.0 + spread), soft).b
                    );
                } else {
                    color = behind(point + bend, soft);
                }

                // Glass makes the colour behind it richer rather than greyer.
                float luma = dot(color, LUMA);
                color = clamp(mix(float3(luma), color, vibrance), 0.0, 1.0);

                // What is round this point, with little left but its colours.
                float3 air = hazed.eval((point + origin) * imageScale * hazeRatio).rgb * (1.0 - dim);

                // The coating that keeps labels readable: a thin veil, never a painted slab. It
                // thickens only where what is behind would fight the words - bright patches under
                // white words, dark ones under dark words - and stays clear everywhere else.
                float seen = dot(color, LUMA);
                float fight = dark > 0.5 ? smoothstep(0.45, 1.0, seen) : 1.0 - smoothstep(0.06, 0.5, seen);
                // Glass made from the wallpaper: the veil takes a little of the colour round it, as
                // far as wallTint says, and keeps its own where it thickens for the words on it.
                float3 veil = mix(wash.rgb, air, wallTint * (1.0 - fight));
                color = mix(color, veil, clamp(wash.a + fight * adapt, 0.0, 0.88));
                // Separation. Over a picture as dark as the pane itself, or in the light as pale,
                // the glass would vanish into it; there, and only there, a little frost lifts the
                // pane a step away from what is round it.
                float same = dark > 0.5 ? 1.0 - smoothstep(0.05, 0.42, seen) : smoothstep(0.84, 0.99, seen);
                color = mix(color, lift.rgb, lift.a * same);

                // The colour of the light. Glass has none of its own: the light it catches comes
                // off what is round it, so over a sunset its rim is warm and over the sea it is
                // cool, a little. Round something too dark to have a colour, the light is white.
                float peak = max(air.r, max(air.g, air.b));
                float3 tone = mix(float3(1.0), air / max(peak, 0.001), smoothstep(0.04, 0.3, peak));
                float3 lamp = mix(float3(1.0), tone, glow);

                // Light, on the edge only. A hairline hugging the outline and a softer glow just
                // inside it, at their brightest on the corner that faces the lamp and on the corner
                // opposite, and faint all the way round between.
                float facing = dot(normal, light);
                float toward = max(facing, 0.0);
                float away = max(-facing, 0.0);
                float spec = toward * toward * toward + 0.65 * away * away * away;
                // Glass reflects more the more it is seen edge-on: the bent band is a touch brighter
                // than the face, and more on the lit side.
                float fresnel = x * x * (0.035 + 0.05 * spec);
                float lit = hairline * (0.2 + 0.85 * spec) + sheen * (0.03 + 0.2 * spec) + fresnel;
                // In the light the pane is pale already, and the same light would whiten its edge.
                color += lamp * shine * presence * lit * (1.0 - 0.25 * (1.0 - dark));

                // A ring of light from a tap on the wallpaper, running out across the screen: where
                // its band crosses the pane the face brightens a little, and the rim catches it.
                if (ringStrength > 0.0) {
                    float ringOff = (length(point - ringCentre) - ringRadius) / max(ringWidth, 1.0);
                    float ringLit = ringStrength * exp(-ringOff * ringOff);
                    color += lamp * ringLit * (0.08 + rimmed * (0.8 * hairline + 0.25 * sheen) + 0.12 * x * x);
                }

                // A finger lights the glass from within: brightest under the fingertip, spreading
                // through the pane, and brightening the rim where the glow reaches it.
                if (touch.w > 0.0) {
                    float2 fromTouch = point - touch.xy;
                    float reachOfTouch = max(touch.z, 1.0);
                    float spread = dot(fromTouch, fromTouch) / (reachOfTouch * reachOfTouch);
                    color += touch.w * (0.12 * exp(-spread) + 0.035) * (1.0 - 0.25 * dark);
                    color += touch.w * (hairline * 0.45 + sheen * 0.12) * exp(-spread * 0.25);
                }

                return half4(half3(clamp(color, 0.0, 1.0)), 1.0);
            }
        """
    }
}

/**
 * How one piece of glass looks. Clear floats over the wallpaper with the most bend and only a mist;
 * Regular frosts what is behind it so words on it stay readable; both share the same edge and light.
 */
internal data class GlassLook(
    /** How hard the rim bends what is behind it: 1 as hard as Clear glass bends at full strength. */
    val refraction: Float,
    /** How soft the picture is through the face: 0 as it is, 1 misted, 2 frosted, 3 little but its colours. */
    val frost: Float,
    /** How far the light splits into colour where the rim bends it hardest. */
    val dispersion: Float,
    /** How brightly the edge catches the light. */
    val shine: Float,
    /** How much richer the colour behind is through the glass: 1 as it is. */
    val vibrance: Float,
    /** The readable veil laid over what is seen through the glass. */
    val wash: Color,
    /** How much thicker the veil gets over backgrounds that would make the text hard to read. */
    val adapt: Float,
    val opacity: Float,
    /** The frost that lifts the pane off a picture of its own tone; its alpha is how much. */
    val lift: Color = Color.Transparent,
    /** How far the light the glass catches takes the colour of what is round it: 0 white, 1 wholly. */
    val glow: Float = 0f,
    /** Whether the pane has an edge of its own. A page that fills the screen has none: the screen's edge is not the glass's. */
    val rimmed: Boolean = true,
    /** Coloured glass: [wash] is its colour, and with nothing readable behind it the body keeps that colour. */
    val tinted: Boolean = false,
    /**
     * Glass standing on the wallpaper, the same in Light and in Dark: with nothing readable behind
     * it, its body is one thin neutral grey in both. The shader is told nothing new for it, since
     * this glass reaches the shader as dark: a uniform set but not declared would leave every phone
     * with the glass that has no shader.
     */
    val onWallpaper: Boolean = false,
    /**
     * Glass made from the wallpaper: how far its veil takes the wallpaper's own colour where it sits,
     * 0 for none, [WALL_TINT] for the dock, Search and the glass widgets. The lens reads that colour
     * under every point; without the lens the pane is given one colour for the whole of it.
     */
    val wallTint: Float = 0f,
)

/** A finger on a pane: where, in pane pixels; how far its glow has spread; and how bright it is. */
internal data class GlassTouch(val at: Offset, val reach: Float, val strength: Float) {
    companion object {
        val None = GlassTouch(Offset.Zero, 1f, 0f)
    }
}
