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
 * The lens itself, on Android 13 and up: the wallpaper seen through a slab of glass with a rounded
 * rim, following Apple's description of Liquid Glass. It does three things to what is round it.
 *
 * It bends it: through the flat middle you see the wallpaper where it really is; towards the rim
 * the glass curves away, so what is behind bends and magnifies inward and splits very slightly
 * into colour at the very edge. A bigger pane is a thicker slab and bends more.
 *
 * It softens it: by [GlassLook.frost], with a true blur, from barely misted to fully frosted. What
 * is dark comes through a little brighter, in its own colour ([GlassLook.gather]).
 *
 * It reflects it: the steep outer part of the rim is a mirror as much as a window, and holds the
 * picture just outside the pane, turned over at the edge ([GlassLook.mirror]); and the light the
 * glass catches is the colour of what is round it ([GlassLook.glow]). That light sits in the
 * world: a crisp highlight runs all the way round the rim, brightest where it faces the light,
 * with a weaker one on the far side, where the light leaves, and a soft glow just inside it.
 *
 * Tip the phone and all three move: the light swings, and what the rim bends and holds slides.
 *
 * And it is a thing, not a picture of one ([GlassLook.relief]): a slab with a rounded shoulder and
 * a polished edge, shaded by the same light. The shoulder turned from the lamp is in its own shade
 * and the one turned to it is lit, the face falls away gently from the light, and where the
 * polished edge mirrors the lamp there is a glint, with a fainter one opposite.
 *
 * A finger on the glass lights it from within, the glow starting under the fingertip and spreading
 * out. One shader per pane, built once; each frame only sets numbers on it.
 */
@RequiresApi(33)
internal class LiquidGlassShader(levels: GlassLevels) {
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
        /** Which way the phone leans (see [LocalTilt]): what the rim bends and reflects slides with it. */
        lean: Offset = Offset.Zero,
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
        shader.setFloatUniform("refraction", look.refraction)
        shader.setFloatUniform("frost", look.frost.coerceIn(0f, 3f))
        shader.setFloatUniform("dispersion", look.dispersion)
        shader.setFloatUniform("shine", look.shine)
        shader.setFloatUniform("vibrance", look.vibrance)
        shader.setFloatUniform("mirror", look.mirror.coerceIn(0f, 1f))
        shader.setFloatUniform("glow", look.glow.coerceIn(0f, 1f))
        shader.setFloatUniform("gather", look.gather.coerceIn(0f, 1f))
        shader.setFloatUniform("rimmed", if (look.rimmed) 1f else 0f)
        shader.setFloatUniform("relief", look.relief.coerceIn(0f, 1f))
        val length = hypot(light.x, light.y).coerceAtLeast(0.001f)
        shader.setFloatUniform("light", light.x / length, light.y / length)
        shader.setFloatUniform("lean", lean.x.coerceIn(-1f, 1f), lean.y.coerceIn(-1f, 1f))
        shader.setFloatUniform("dark", if (dark) 1f else 0f)
        val wash = look.wash
        shader.setFloatUniform("wash", wash.red, wash.green, wash.blue, wash.alpha)
        shader.setFloatUniform("adapt", look.adapt)
        val lift = look.lift
        shader.setFloatUniform("lift", lift.red, lift.green, lift.blue, lift.alpha)
        // Glass materialises by bending light, not by fading: a pane on its way in bends less.
        shader.setFloatUniform("presence", look.opacity.coerceIn(0f, 1f))
        shader.setFloatUniform("touch", touch.at.x, touch.at.y, touch.reach, touch.strength)
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
                // Zenith's only free-form outline is the squircle icon tile.
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
            uniform float density;
            uniform float refraction;
            uniform float frost;
            uniform float dispersion;
            uniform float shine;
            uniform float vibrance;
            uniform float mirror;
            uniform float glow;
            uniform float gather;
            uniform float rimmed;
            uniform float relief;
            uniform float2 light;
            uniform float2 lean;
            uniform float dark;
            uniform float4 wash;
            uniform float adapt;
            uniform float4 lift;
            uniform float presence;
            uniform float4 touch;

            // Signed distance to the outline: negative inside, in pixels.
            float edgeDistance(float2 point) {
                float2 halfSize = size * 0.5;
                float2 p = point - halfSize;
                if (squircle > 0.5) {
                    float2 v = abs(p) / max(halfSize, float2(1.0));
                    float k = pow(pow(v.x, 2.9) + pow(v.y, 2.9), 1.0 / 2.9);
                    return (k - 1.0) * min(halfSize.x, halfSize.y);
                }
                float radius = p.y < 0.0
                    ? (p.x < 0.0 ? corners.x : corners.y)
                    : (p.x < 0.0 ? corners.w : corners.z);
                radius = min(radius, min(halfSize.x, halfSize.y));
                float2 q = abs(p) - halfSize + radius;
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
            }

            // What is behind a point, as soft as asked: 0 is the picture as it is, 3 little but
            // its colours. The pictures either side of that softness are blended, which is a true
            // blur of any strength for the price of two lookups.
            float3 behind(float2 point, float soft) {
                float2 uv = (point + origin) * imageScale;
                if (soft < 1.0) {
                    return mix(wallpaper.eval(uv).rgb, misted.eval(uv * mistRatio).rgb, max(soft, 0.0));
                }
                if (soft < 2.0) {
                    return mix(misted.eval(uv * mistRatio).rgb, frosted.eval(uv * frostRatio).rgb, soft - 1.0);
                }
                return mix(frosted.eval(uv * frostRatio).rgb, hazed.eval(uv * hazeRatio).rgb, min(soft - 2.0, 1.0));
            }

            half4 main(float2 point) {
                float d = edgeDistance(point);
                float inside = max(-d, 0.0);
                float shortest = min(size.x, size.y);

                // The rim: a quarter-circle bevel, flat in the middle and steepest at the edge. A
                // bigger pane is a thicker slab, so its rim is wider and it bends more.
                float bevel = clamp(shortest * 0.24, 12.0 * density, 28.0 * density);
                bevel = max(min(bevel, shortest * 0.45), 1.0);
                // A pane that runs off the screen has no rim: it is all flat middle.
                float t = clamp(1.0 - inside / bevel, 0.0, 1.0) * rimmed;
                // How far the glass has curved away by here. A thin pane only curls at its very
                // edge; a slab is a lens across the whole of its shoulder, and bends what is behind
                // it sooner.
                float curve = mix(1.0 - sqrt(max(1.0 - t * t, 0.0)), t * t, 0.6 * relief);

                float2 gradient = float2(
                    edgeDistance(point + float2(0.75, 0.0)) - edgeDistance(point - float2(0.75, 0.0)),
                    edgeDistance(point + float2(0.0, 0.75)) - edgeDistance(point - float2(0.0, 0.75))
                );
                float2 normal = gradient / max(length(gradient), 0.0001);

                // What is behind the rim is pulled in from further inside: the magnifying bend
                // of a thick lens. The middle stays exactly where it is. Tip the phone and you look
                // through the rim from another side, so what it holds slides a little.
                float2 bend = (-normal * 0.95 * refraction + lean * 0.3) * curve * bevel * presence;
                // The rim shows the picture nearly as soft as the face does, and softer right at
                // the edge, where the bend squeezes so much picture into so little glass that its
                // grain would streak.
                float soft = max(frost * (1.0 - 0.25 * t), 1.2 * smoothstep(0.8, 1.0, t));

                float3 color;
                if (t > 0.0 && dispersion > 0.0) {
                    float spread = dispersion * curve;
                    color = float3(
                        behind(point + bend * (1.0 - spread), soft).r,
                        behind(point + bend, soft).g,
                        behind(point + bend * (1.0 + spread), soft).b
                    );
                } else {
                    color = behind(point + bend, soft);
                }

                // What is round this point, with only its colours left: in the face what is behind
                // it and about it, towards the rim what lies beyond the edge on that side.
                float3 air = hazed.eval((point + normal * bevel * t + origin) * imageScale * hazeRatio).rgb;

                // Glass gathers light. What is dark behind it comes through a little brighter,
                // keeping its colour, and whatever round it is brighter still spills a little of
                // its light in: a pane beside a sunset is warm on that side.
                float through = dot(color, float3(0.2126, 0.7152, 0.0722));
                color *= 1.0 + gather * (1.0 - smoothstep(0.0, 0.7, through));
                color += max(air - color, 0.0) * 0.5 * glow;

                // Glass makes colour behind it a touch richer rather than washing it out.
                float luma = dot(color, float3(0.2126, 0.7152, 0.0722));
                color = mix(float3(luma), color, vibrance);
                // The coating that keeps labels readable: a light veil, never a painted slab. It
                // thickens only where what is behind would fight the text - bright patches under
                // white text, dark ones under dark text - and stays clear everywhere else.
                float seen = dot(color, float3(0.2126, 0.7152, 0.0722));
                float fight = dark > 0.5 ? smoothstep(0.42, 0.95, seen) : 1.0 - smoothstep(0.08, 0.55, seen);
                color = mix(color, wash.rgb, clamp(wash.a + fight * adapt, 0.0, 0.95));
                // Separation. Over a picture as dark as the pane itself, or in the light as pale,
                // the glass would vanish into it; there, and only there, a little frost lifts the
                // pane a step away from what is round it.
                float same = dark > 0.5 ? 1.0 - smoothstep(0.05, 0.42, seen) : smoothstep(0.84, 0.99, seen);
                color = mix(color, lift.rgb, lift.a * same);

                // Reflection. Where the rim rounds away steeply it is a mirror as much as a
                // window, and what it holds is the picture just outside the pane, turned over at
                // the edge: nearest the edge what is right beside it, further in what is further
                // out. Softened and a touch richer, as a reflection in the edge of a tumbler is.
                if (t > 0.0 && mirror > 0.0) {
                    float2 beside = point + normal * (inside * 2.6 + 3.0 * density) + lean * 14.0 * density;
                    float3 thrown = frosted.eval((beside + origin) * imageScale * frostRatio).rgb;
                    float bright = dot(thrown, float3(0.2126, 0.7152, 0.0722));
                    thrown = mix(float3(bright), thrown, 1.3);
                    color = mix(color, clamp(thrown, 0.0, 1.0), mirror * presence * t * t * t);
                }

                // The colour of the light. Glass has none of its own: the light it catches comes
                // off what is round it, so over a sunset its rim is warm and over the sea it is
                // cool. Round something too dark to have a colour, the light is white.
                float peak = max(air.r, max(air.g, air.b));
                float3 tone = mix(float3(1.0), air / max(peak, 0.001), smoothstep(0.04, 0.3, peak));
                float3 lamp = mix(float3(1.0), tone, glow);

                // Light. A crisp line right on the rim, all the way round, brightest where it
                // faces the light, with a weaker reflection opposite, where light that crossed the
                // glass leaves it.
                float facing = dot(normal, light);
                float lit = max(facing, 0.0);
                float away = max(-facing, 0.0);

                // Form. The pane is a slab, and it is light that shows a slab its shape. The lamp
                // hangs up and to the left, well above the glass, so the shoulder rounds away from
                // it everywhere but on the lit side, and furthest on the far side: that is how it
                // is shaded. In the light a pane is pale, and shading that shapes a dark one would
                // turn it to plaster, so there it is gentler.
                float3 above = normalize(float3(light * 0.77, 0.64));
                float3 shoulder = float3(normal * t, sqrt(max(1.0 - t * t, 0.0)));
                float form = dot(shoulder, above) - above.z;
                color *= 1.0 - mix(0.28, 0.55, dark) * relief * clamp(-form, 0.0, 1.0);
                // The face is not quite level with the light either: it falls away across the pane.
                float across = dot(point - size * 0.5, light) / max(0.5 * (abs(light.x) * size.x + abs(light.y) * size.y), 1.0);
                color *= 1.0 - 0.08 * relief * smoothstep(0.0, 1.0, -across);
                float sheen = 0.10 * smoothstep(0.0, 1.0, across);
                // And its very edge is polished round: a few dp of it, more on a thicker slab.
                // Where that curve mirrors the lamp there is a glint, and a fainter one opposite.
                float polish = clamp(1.0 - inside / max(bevel * 0.3, 1.0), 0.0, 1.0) * rimmed;
                float3 edge = float3(normal * polish, sqrt(max(1.0 - polish * polish, 0.0)));
                float3 toLight = normalize(above + float3(0.0, 0.0, 1.0));
                float3 toFar = normalize(float3(-above.xy, above.z + 1.0));
                float level = pow(toLight.z, 40.0);
                float glint = max(pow(max(dot(edge, toLight), 0.0), 40.0) - level, 0.0)
                    + 0.35 * max(pow(max(dot(edge, toFar), 0.0), 40.0) - level, 0.0);

                float hairline = 1.0 - smoothstep(0.5 * density, 1.6 * density, inside);
                float rim = hairline * (0.42 + 1.15 * pow(lit, 1.5) + 0.55 * pow(away, 1.8));
                // Just inside the line the glass glows, all the way round, because glass reflects
                // more the more it is seen edge-on; a few dp wide whatever the size of the pane.
                float near = 1.0 - smoothstep(0.0, 6.0 * density, inside);
                float band = near * near * (0.30 + 0.30 * pow(lit, 2.0) + 0.16 * pow(away, 2.4));
                // And the bevel catches the light: a wider, softer bloom on the lit side.
                float bloom = pow(t, 2.5) * (0.26 * pow(lit, 2.0) + 0.10 * pow(away, 2.4));
                // In the light the pane is pale already, and the same glow would whiten its whole edge.
                color += lamp * shine * (rim * (1.0 - 0.12 * dark) + (band + bloom) * (0.5 + 0.22 * dark)
                    + relief * (1.1 * glint + sheen + mix(0.40, 0.90, dark) * max(form, 0.0)));

                // A finger lights the glass from within: brightest under the fingertip, spreading
                // through the pane, and flaring the rim where the glow reaches it.
                if (touch.w > 0.0) {
                    float2 fromTouch = point - touch.xy;
                    float reach = max(touch.z, 1.0);
                    float spread = dot(fromTouch, fromTouch) / (reach * reach);
                    color += touch.w * (0.13 * exp(-spread) + 0.04) * (1.0 - 0.25 * dark);
                    color += touch.w * hairline * 0.55 * exp(-spread * 0.25);
                }

                return half4(half3(clamp(color, 0.0, 1.0)), 1.0);
            }
        """
    }
}

/**
 * How one piece of glass looks. Clear floats over wallpaper with the most bend; Regular diffuses
 * what is behind it so text on it stays readable; both share the same rim and light.
 */
internal data class GlassLook(
    val refraction: Float,
    /** How soft the picture is through the face: 0 as it is, 1 misted, 2 frosted, 3 little but its colours. */
    val frost: Float,
    val dispersion: Float,
    val shine: Float,
    val vibrance: Float,
    /** The readable veil laid over what is seen through the glass. */
    val wash: Color,
    /** How much thicker the veil gets over backgrounds that would make the text hard to read. */
    val adapt: Float,
    val opacity: Float,
    /** The frost that lifts the pane off a picture of its own tone; its alpha is how much. */
    val lift: Color = Color.Transparent,
    /** How much of the picture beside the pane its rim throws back: 0 none, 1 a mirror at the very edge. */
    val mirror: Float = 0f,
    /** How far the light the glass catches takes the colour of what is round it: 0 white, 1 wholly. */
    val glow: Float = 0f,
    /** How much brighter what is dark comes through, in its own colour: 0 as it is, 1 twice as bright. */
    val gather: Float = 0f,
    /** Whether the pane has an edge of its own. A page that fills the screen has none: the screen's edge is not the glass's. */
    val rimmed: Boolean = true,
    /** How far the pane stands out as a solid slab, shaded and glinting: 0 a flat sheet of light, 1 fully. */
    val relief: Float = 0f,
    /**
     * The pane covers what is behind it rather than floating over the wallpaper: a sheet, an open
     * folder, the drawer. Behind those is the home screen, so with no wallpaper to show their own
     * frosted body is not see-through (see [fallbackFill]).
     */
    val covers: Boolean = false,
) {
    /**
     * Whether the pane looks any different as the phone tips: only one with a rim, which bends what
     * is behind it and catches the light, does. One without need not be drawn again for the tilt.
     */
    val followsTilt: Boolean get() = rimmed
}

/** A finger on a pane: where, in pane pixels; how far its glow has spread; and how bright it is. */
internal data class GlassTouch(val at: Offset, val reach: Float, val strength: Float) {
    companion object {
        val None = GlassTouch(Offset.Zero, 1f, 0f)
    }
}
