package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassPressTest {

    private val eps = 1e-4f

    @Test
    fun `let go, every motion is back at rest`() {
        for (motion in GlassMotion.entries) {
            for (reduced in listOf(false, true)) {
                assertEquals(PressTarget(1f, 0f), pressTarget(motion, pressed = false, reduced = reduced))
            }
        }
    }

    @Test
    fun `pressed, each motion goes to its own size`() {
        assertEquals(1.075f, pressTarget(GlassMotion.Flex, pressed = true, reduced = false).scale, eps)
        assertEquals(0.95f, pressTarget(GlassMotion.Jelly, pressed = true, reduced = false).scale, eps)
        assertEquals(1.05f, pressTarget(GlassMotion.Lean, pressed = true, reduced = false).scale, eps)
        assertEquals(1.09f, pressTarget(GlassMotion.Drag, pressed = true, reduced = false).scale, eps)
    }

    @Test
    fun `only jelly squashes`() {
        assertEquals(0.05f, pressTarget(GlassMotion.Jelly, pressed = true, reduced = false).squash, eps)
        for (motion in listOf(GlassMotion.Flex, GlassMotion.Lean, GlassMotion.Drag)) {
            assertEquals(0f, pressTarget(motion, pressed = true, reduced = false).squash, eps)
        }
    }

    @Test
    fun `with movement reduced a press changes size two fifths as much and nothing squashes`() {
        assertEquals(1.03f, pressTarget(GlassMotion.Flex, pressed = true, reduced = true).scale, eps)
        assertEquals(0.98f, pressTarget(GlassMotion.Jelly, pressed = true, reduced = true).scale, eps)
        assertEquals(1.02f, pressTarget(GlassMotion.Lean, pressed = true, reduced = true).scale, eps)
        assertEquals(1.036f, pressTarget(GlassMotion.Drag, pressed = true, reduced = true).scale, eps)
        for (motion in GlassMotion.entries) {
            assertEquals(0f, pressTarget(motion, pressed = true, reduced = true).squash, eps)
        }
    }

    @Test
    fun `each motion springs as designed`() {
        assertEquals(SpringParams(0.41f, 420f), pressSpring(GlassMotion.Flex, reduced = false))
        assertEquals(SpringParams(0.24f, 520f), pressSpring(GlassMotion.Jelly, reduced = false))
        assertEquals(SpringParams(0.56f, 380f), pressSpring(GlassMotion.Lean, reduced = false))
        assertEquals(SpringParams(0.41f, 420f), pressSpring(GlassMotion.Drag, reduced = false))
        assertEquals(SpringParams(0.18f, 640f), squashSpring(reduced = false))
        assertEquals(SpringParams(0.74f, 260f), leanSpring(reduced = false))
        assertEquals(SpringParams(0.57f, 700f), snapSpring(reduced = false))
    }

    @Test
    fun `with movement reduced every spring is critically damped`() {
        for (motion in GlassMotion.entries) {
            assertEquals(SpringParams(1f, 320f), pressSpring(motion, reduced = true))
        }
        assertEquals(SpringParams(1f, 320f), squashSpring(reduced = true))
        assertEquals(SpringParams(1f, 320f), leanSpring(reduced = true))
        assertEquals(1f, snapSpring(reduced = true).dampingRatio, eps)
    }

    @Test
    fun `a pane leans a tenth of the way across to the finger and a little more down`() {
        val lean = leanOffset(finger = Offset(130f, 60f), centre = Offset(100f, 50f), density = 2f)
        assertEquals(3f, lean.x, eps)
        assertEquals(1.4f, lean.y, eps)
    }

    @Test
    fun `it leans no more than 12 dp across and 9 dp down, either way`() {
        val far = leanOffset(finger = Offset(5000f, 5000f), centre = Offset.Zero, density = 3f)
        assertEquals(36f, far.x, eps)
        assertEquals(27f, far.y, eps)
        val back = leanOffset(finger = Offset(-5000f, -5000f), centre = Offset.Zero, density = 3f)
        assertEquals(-36f, back.x, eps)
        assertEquals(-27f, back.y, eps)
    }

    @Test
    fun `a finger on the middle does not lean it`() {
        assertEquals(Offset.Zero, leanOffset(Offset(80f, 40f), Offset(80f, 40f), density = 2.5f))
    }

    @Test
    fun `a dragged lens stretches with speed, up to a fifth`() {
        assertEquals(0f, dragStretch(0f, reduced = false), eps)
        assertEquals(0.1f, dragStretch(260f, reduced = false), eps)
        assertEquals(0.1f, dragStretch(-260f, reduced = false), eps)
        assertEquals(0.2f, dragStretch(2600f, reduced = false), eps)
        assertEquals(0.2f, dragStretch(9000f, reduced = false), eps)
    }

    @Test
    fun `with movement reduced a dragged lens does not stretch`() {
        assertEquals(0f, dragStretch(2000f, reduced = true), eps)
    }

    @Test
    fun `words on tinted glass are dark on a light accent and white on a deep one`() {
        assertEquals(Color(0xFF0B0C10), inkOnTint(Color(0xFFFFB86B)))
        assertEquals(Color(0xFF0B0C10), inkOnTint(Color(0xFF9FE3FF)))
        assertEquals(Color.White, inkOnTint(Color(0xFF5B3FD9)))
        assertEquals(Color.White, inkOnTint(Color(0xFF1E2A44)))
    }
}
