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
 * rim. Through the flat middle you see the wallpaper where it really is, softened by [GlassLook.frost];
 * towards the rim the glass curves away, so what is behind bends and magnifies inward, splits very
 * slightly into colour at the very edge, and a thin highlight runs round the rim where it faces the
 * light. One shader per pane, built once; each frame only sets numbers on it.
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

                // The rim: a quarter-circle bevel, flat in the middle and steepest at the edge.
                float bevel = max(min(20.0 * density, shortest * 0.42), 1.0);
                float t = clamp(1.0 - inside / bevel, 0.0, 1.0);
                float curve = 1.0 - sqrt(max(1.0 - t * t, 0.0));

                float2 gradient = float2(
                    edgeDistance(point + float2(0.75, 0.0)) - edgeDistance(point - float2(0.75, 0.0)),
                    edgeDistance(point + float2(0.0, 0.75)) - edgeDistance(point - float2(0.0, 0.75))
                );
                float2 normal = gradient / max(length(gradient), 0.0001);

                // What is behind the rim is pulled in from further inside: the magnifying bend
                // of a thick lens. The middle stays exactly where it is.
                float2 bend = -normal * curve * bevel * 0.85 * refraction;
                // The rim is clearer than the face, as real glass thins there.
                float blur = frost * (1.0 - 0.55 * t);

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

                // Light. The rim facing the light glints brightest; the opposite rim catches a
                // weaker reflection, as light crosses the glass and exits on the far side.
                float facing = dot(normal, light);
                float glint = pow(max(facing, 0.0), 2.2) + 0.55 * pow(max(-facing, 0.0), 2.6);
                float hairline = 1.0 - smoothstep(0.0, 1.6 * density, inside);
                float sheen = t * t;
                float highlight = shine * (hairline * (0.22 + 0.78 * glint) + sheen * 0.14 * glint);
                color += highlight * (1.0 - 0.35 * dark);
                // The side away from the light sits in the glass's own shadow, which gives it depth.
                color *= 1.0 - 0.07 * sheen * max(-facing, 0.0);

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
)
