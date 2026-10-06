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
import kotlin.math.hypot
import kotlin.math.max

/**
 * The lens itself, on Android 13 and up: the wallpaper seen through a slab of glass with a rounded
 * rim, following Apple's description of Liquid Glass. It lenses rather than scatters: through the
 * flat middle you see the wallpaper where it really is, softened by [GlassLook.frost]; towards the
 * rim the glass curves away, so what is behind bends and magnifies inward, splits very slightly into
 * colour at the very edge, and the rim brightens the way glass does at a grazing angle. Light sits
 * in the world: a crisp highlight runs round the rim where it faces the light and a weaker one on
 * the far side, where the light leaves. A bigger pane is a thicker slab and bends more. A finger on
 * the glass lights it from within, the glow starting under the fingertip and spreading out.
 * One shader per pane, built once; each frame only sets numbers on it.
 */
@RequiresApi(33)
internal class LiquidGlassShader(sharp: ImageBitmap, blurred: ImageBitmap) {
    private val sharpSize = Size(sharp.width.toFloat(), sharp.height.toFloat())
    private val blurSize = Size(blurred.width.toFloat(), blurred.height.toFloat())
    private val shader = RuntimeShader(PROGRAM).apply {
        setInputShader("wallpaper", bitmapShader(sharp))
        setInputShader("diffused", bitmapShader(blurred))
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
        shader.setFloatUniform("blurRatio", blurSize.width / sharpSize.width, blurSize.height / sharpSize.height)
        shader.setFloatUniform("density", density)
        shader.setFloatUniform("refraction", look.refraction)
        shader.setFloatUniform("frost", look.frost.coerceIn(0f, 1f))
        shader.setFloatUniform("dispersion", look.dispersion)
        shader.setFloatUniform("shine", look.shine)
        shader.setFloatUniform("vibrance", look.vibrance)
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

    private fun bitmapShader(image: ImageBitmap) =
        BitmapShader(image.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }

    companion object {
        /** Everything in panel pixels until the wallpaper is sampled, so the rim is the same thickness on a pill and a sheet. */
        internal const val PROGRAM = """
            uniform shader wallpaper;
            uniform shader diffused;
            uniform float2 size;
            uniform float2 origin;
            uniform float imageScale;
            uniform float2 blurRatio;
            uniform float4 corners;
            uniform float squircle;
            uniform float density;
            uniform float refraction;
            uniform float frost;
            uniform float dispersion;
            uniform float shine;
            uniform float vibrance;
            uniform float2 light;
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

            float3 behind(float2 point, float blur) {
                float2 uv = (point + origin) * imageScale;
                float3 sharp = wallpaper.eval(uv).rgb;
                float3 soft = diffused.eval(uv * blurRatio).rgb;
                return mix(sharp, soft, blur);
            }

            half4 main(float2 point) {
                float d = edgeDistance(point);
                float inside = max(-d, 0.0);
                float shortest = min(size.x, size.y);

                // The rim: a quarter-circle bevel, flat in the middle and steepest at the edge. A
                // bigger pane is a thicker slab, so its rim is wider and it bends more.
                float bevel = clamp(shortest * 0.22, 10.0 * density, 28.0 * density);
                bevel = max(min(bevel, shortest * 0.42), 1.0);
                float t = clamp(1.0 - inside / bevel, 0.0, 1.0);
                float curve = 1.0 - sqrt(max(1.0 - t * t, 0.0));

                float2 gradient = float2(
                    edgeDistance(point + float2(0.75, 0.0)) - edgeDistance(point - float2(0.75, 0.0)),
                    edgeDistance(point + float2(0.0, 0.75)) - edgeDistance(point - float2(0.0, 0.75))
                );
                float2 normal = gradient / max(length(gradient), 0.0001);

                // What is behind the rim is pulled in from further inside: the magnifying bend
                // of a thick lens. The middle stays exactly where it is.
                float2 bend = -normal * curve * bevel * 0.95 * refraction * presence;
                // The rim is clearer than the face, as real glass thins there - except right at the
                // edge, where the bend squeezes so much picture into so little glass that its grain
                // would streak; there it softens, as the rim of real glass scatters.
                float blur = max(frost * (1.0 - 0.55 * t), 0.55 * curve);

                float3 color;
                if (t > 0.0 && dispersion > 0.0) {
                    float spread = dispersion * curve;
                    color = float3(
                        behind(point + bend * (1.0 - spread), blur).r,
                        behind(point + bend, blur).g,
                        behind(point + bend * (1.0 + spread), blur).b
                    );
                } else {
                    color = behind(point + bend, blur);
                }

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

                // Light. A crisp line right on the rim, brightest where it faces the light, and a
                // weaker reflection opposite, where light that crossed the glass leaves it.
                float facing = dot(normal, light);
                float lit = max(facing, 0.0);
                float away = max(-facing, 0.0);
                float hairline = 1.0 - smoothstep(0.0, 1.5 * density, inside);
                float rim = hairline * (0.26 + 1.15 * pow(lit, 1.5) + 0.6 * pow(away, 1.8));
                // The bevel catching the light: a soft band just inside the rim on the lit side.
                float sheen = t * t;
                float bloom = pow(t, 2.5) * (0.28 * pow(lit, 2.0) + 0.10 * pow(away, 2.4));
                // Glass reflects more at a grazing angle, so the curved rim reads a touch brighter
                // than the flat face all the way round.
                float fresnel = pow(t, 4.0) * 0.07 * min(shine, 1.0);
                color += shine * rim * (1.0 - 0.12 * dark) + (shine * bloom + fresnel) * (1.0 - 0.3 * dark);
                // The side away from the light sits in the glass's own shadow, which gives it depth.
                color *= 1.0 - 0.08 * sheen * away;

                // A finger lights the glass from within: brightest under the fingertip, spreading
                // through the pane, and flaring the rim where the glow reaches it.
                if (touch.w > 0.0) {
                    float2 fromTouch = point - touch.xy;
                    float reach = max(touch.z, 1.0);
                    float near = dot(fromTouch, fromTouch) / (reach * reach);
                    color += touch.w * (0.13 * exp(-near) + 0.04) * (1.0 - 0.25 * dark);
                    color += touch.w * hairline * 0.55 * exp(-near * 0.25);
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
)

/** A finger on a pane: where, in pane pixels; how far its glow has spread; and how bright it is. */
internal data class GlassTouch(val at: Offset, val reach: Float, val strength: Float) {
    companion object {
        val None = GlassTouch(Offset.Zero, 1f, 0f)
    }
}
