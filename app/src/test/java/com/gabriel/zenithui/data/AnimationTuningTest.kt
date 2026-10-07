package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationTuningTest {

    private fun throughText(tuning: AnimationTuning) = AnimationTuning.fromJson(JSONObject(tuning.toJson().toString()))

    @Test
    fun `every number has a name of its own and a range its own value sits in, on a step`() {
        assertEquals(Tune.entries.size, Tune.entries.map { it.id }.toSet().size)
        for (tune in Tune.entries) {
            assertTrue(tune.id, tune.min < tune.max && tune.step > 0f)
            assertTrue(tune.id, tune.own in tune.min..tune.max)
            assertEquals(tune.id, tune.own, tune.snap(tune.own), tune.step / 100f)
            assertEquals(tune, Tune.byId(tune.id))
        }
    }

    @Test
    fun `every preset keeps every number inside its range, on a step`() {
        assertEquals(AnimationPreset.entries.size, AnimationPreset.entries.map { it.id }.toSet().size)
        for (preset in AnimationPreset.entries) for (tune in Tune.entries) {
            val value = preset.value(tune)
            assertTrue("${preset.id} ${tune.id}", value in tune.min..tune.max)
            assertEquals("${preset.id} ${tune.id}", value, tune.snap(value), tune.step / 100f)
            assertEquals(value, AnimationTuning.of(preset)[tune], 0f)
        }
        assertTrue(AnimationPreset.entries.all { it.speed in AnimationTuning.MIN_SPEED..AnimationTuning.MAX_SPEED })
    }

    @Test
    fun `classic is opening and closing as they were before they could be tuned`() {
        val classic = AnimationTuning.of(AnimationPreset.Classic)
        for (tune in listOf(Tune.OpenXStiffness, Tune.OpenYStiffness, Tune.OpenSizeStiffness)) assertEquals(300f, classic[tune], 0f)
        for (tune in listOf(Tune.OpenXDamping, Tune.OpenYDamping, Tune.OpenSizeDamping)) assertEquals(0.8f, classic[tune], 0f)
        assertEquals(0f, classic[Tune.OpenArc], 0f)
        assertEquals(0.55f, classic[Tune.OpenLaunchAt], 0f)
        assertEquals(28f, classic[Tune.OpenCorner], 0f)
        assertEquals(96f, classic[Tune.OpenLogo], 0f)
        assertEquals(0f, classic[Tune.OpenShadow], 0f)
        assertEquals(0.9f, classic[Tune.HomeScale], 0f)
        assertEquals(0f, classic[Tune.HomePivot], 0f)
        assertEquals(0.25f, classic[Tune.HomeDim], 0f)
        assertEquals(8f, classic[Tune.HomeBlur], 0f)
        assertEquals(0f, classic[Tune.IconPush], 0f)
        assertEquals(320f, classic[Tune.ReturnStiffness], 0f)
        assertEquals(0.72f, classic[Tune.ReturnDamping], 0f)
        assertEquals(0f, classic[Tune.CloseCard], 0f)
        assertEquals(1.22f, classic[Tune.LandScale], 0f)
        assertEquals(380f, classic[Tune.LandStiffness], 0f)
        assertEquals(0.5f, classic[Tune.LandDamping], 0f)
        assertEquals(0.65f, classic[Tune.LandFade], 0f)
        assertEquals(0f, classic[Tune.Ripple], 0f)
        assertEquals(1f, classic.pace, 0f)
    }

    @Test
    fun `a number moved by hand is kept on its step and inside its range`() {
        val tuned = AnimationTuning().with(Tune.OpenArc, 23.4f).with(Tune.OpenXDamping, 0.777f)
        assertEquals(23f, tuned[Tune.OpenArc], 0f)
        assertEquals(0.78f, tuned[Tune.OpenXDamping], 0.0001f)
        assertTrue(tuned.custom)
        // Past either end it stops at the end.
        assertEquals(Tune.OpenArc.max, AnimationTuning().with(Tune.OpenArc, 500f)[Tune.OpenArc], 0f)
        assertEquals(Tune.HomeBlur.min, AnimationTuning().with(Tune.HomeBlur, -3f)[Tune.HomeBlur], 0f)
        // Everything else is still the preset's.
        assertEquals(Tune.Ripple.own, tuned[Tune.Ripple], 0f)
    }

    @Test
    fun `moved back to the preset's own value, it is no longer a change`() {
        val tuned = AnimationTuning().with(Tune.HomeBlur, 20f)
        assertTrue(tuned.custom)
        val back = tuned.with(Tune.HomeBlur, Tune.HomeBlur.own)
        assertFalse(back.custom)
        assertEquals(AnimationTuning(), back)
        // Against the preset in use, not against Zenith's.
        val fluid = AnimationTuning.of(AnimationPreset.Fluid)
        assertFalse(fluid.with(Tune.HomeScale, 1.12f).custom)
        assertTrue(fluid.with(Tune.HomeScale, Tune.HomeScale.own).custom)
    }

    @Test
    fun `minus and plus move a number one step, and stop at the ends`() {
        val start = AnimationTuning()
        assertEquals(Tune.OpenCorner.own + 1f, start.nudged(Tune.OpenCorner, 1)[Tune.OpenCorner], 0f)
        assertEquals(Tune.OpenXDamping.own - 0.01f, start.nudged(Tune.OpenXDamping, -1)[Tune.OpenXDamping], 0.0001f)
        assertEquals(Tune.HomeScale.own + 0.005f, start.nudged(Tune.HomeScale, 1)[Tune.HomeScale], 0.0001f)
        val full = start.with(Tune.OpenShadow, 1f)
        assertEquals(1f, full.nudged(Tune.OpenShadow, 1)[Tune.OpenShadow], 0f)
        val none = start.with(Tune.Ripple, 0f)
        assertEquals(0f, none.nudged(Tune.Ripple, -1)[Tune.Ripple], 0f)
    }

    @Test
    fun `speed is the preset's until it is set, and stiffness follows it squared`() {
        assertEquals(1f, AnimationTuning().pace, 0f)
        val dynamic = AnimationTuning.of(AnimationPreset.Dynamic)
        assertEquals(1.5f, dynamic.pace, 0f)
        assertFalse(dynamic.custom)
        assertEquals(Tune.OpenXStiffness.own * 2.25f, dynamic.stiffness(Tune.OpenXStiffness), 0.01f)
        // Twice as quick is four times as stiff.
        val twice = AnimationTuning().paced(2f)
        assertEquals(2f, twice.pace, 0f)
        assertTrue(twice.custom)
        assertEquals(Tune.ReturnStiffness.own * 4f, twice.stiffness(Tune.ReturnStiffness), 0.01f)
        // Set back to the preset's own speed, it is no longer a change; and it never leaves its range.
        assertFalse(twice.paced(1f).custom)
        assertFalse(dynamic.paced(1.5f).custom)
        assertEquals(AnimationTuning.MAX_SPEED, AnimationTuning().paced(9f).pace, 0f)
        assertEquals(AnimationTuning.MIN_SPEED, AnimationTuning().paced(0.01f).pace, 0f)
        assertEquals(1.25f, AnimationTuning().paced(1.26f).pace, 0.0001f)
    }

    @Test
    fun `a tuning survives being written and read back`() {
        val tuned = AnimationTuning.of(AnimationPreset.Bouncy).paced(1.3f).with(Tune.OpenArc, -12f).with(Tune.CloseCard, 0.6f)
        assertEquals(tuned, throughText(tuned))
        assertEquals(AnimationTuning(), throughText(AnimationTuning()))
        AnimationPreset.entries.forEach { assertEquals(AnimationTuning.of(it), throughText(AnimationTuning.of(it))) }
    }

    @Test
    fun `what is read back is brought inside its range, and what is not known is kept but does nothing`() {
        val odd = AnimationTuning.fromJson(
            JSONObject("""{"preset":"origami","speed":40,"changed":{"openArc":900,"homeScale":0.1,"wobble":3,"ripple":"lots"}}""")
        )
        assertEquals(Tune.OpenArc.max, odd[Tune.OpenArc], 0f)
        assertEquals(Tune.HomeScale.min, odd[Tune.HomeScale], 0f)
        assertEquals(Tune.Ripple.own, odd[Tune.Ripple], 0f)
        assertEquals(AnimationTuning.MAX_SPEED, odd.pace, 0f)
        // A preset from a newer Zenith behaves as Zenith's own here, and is handed on as it came.
        assertEquals("origami", odd.preset)
        assertEquals(Tune.LandScale.own, odd[Tune.LandScale], 0f)
        assertEquals("origami", throughText(odd).preset)
        assertEquals(3f, throughText(odd).changed["wobble"])
        // Nothing saved, or something that is not a tuning, is Zenith's own.
        assertEquals(AnimationTuning(), AnimationTuning.fromJson(null))
        assertEquals(AnimationTuning(), AnimationTuning.fromJson(JSONObject("""{"changed":7}""")))
    }

    @Test
    fun `with movement reduced nothing swings, bounces, curves or zooms`() {
        val calm = AnimationTuning.Reduced
        for (tune in listOf(Tune.OpenXDamping, Tune.OpenYDamping, Tune.OpenSizeDamping, Tune.ReturnDamping, Tune.LandDamping)) {
            assertTrue(tune.id, calm[tune] >= 1f)
        }
        assertEquals(0f, calm[Tune.OpenArc], 0f)
        assertEquals(1f, calm[Tune.HomeScale], 0f)
        assertEquals(0f, calm[Tune.HomeBlur], 0f)
        assertEquals(0f, calm[Tune.IconPush], 0f)
        assertEquals(0f, calm[Tune.Ripple], 0f)
        assertEquals(1f, calm[Tune.LandScale], 0f)
        assertEquals(0f, calm[Tune.CloseCard], 0f)
    }

    @Test
    fun `numbers are written the way they are meant`() {
        assertEquals("330", Tune.OpenXStiffness.show(330f))
        assertEquals("0.86", Tune.OpenYDamping.show(0.86f))
        assertEquals("1.00", Tune.OpenYDamping.show(1f))
        assertEquals("50%", Tune.OpenLaunchAt.show(0.5f))
        assertEquals("90%", Tune.HomeScale.show(0.9f))
        assertEquals("112.5%", Tune.HomeScale.show(1.125f))
        assertEquals("28 dp", Tune.OpenCorner.show(28f))
        assertEquals("-12 dp", Tune.OpenArc.show(-12f))
        assertEquals("1.22×", Tune.LandScale.show(1.22f))
        assertEquals("Off", Tune.CloseCard.show(0f))
        assertEquals("60%", Tune.CloseCard.show(0.6f))
    }
}
