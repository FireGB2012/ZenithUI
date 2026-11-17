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
        assertEquals(0f, calm[Tune.CloseSquash], 0f)
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
        // Its own size closing Into its icon, and no card closing Plain.
        assertEquals("Auto", Tune.CloseCard.show(0f))
        assertEquals("Auto", Tune.CloseCard.show(0.004f))
        assertEquals("60%", Tune.CloseCard.show(0.6f))
        assertEquals("92%", Tune.ClosePicture.show(0.92f))
        assertEquals("280 ms", Tune.CloseTime.show(280f))
        assertEquals("30%", Tune.CloseSquash.show(0.3f))
        // Waiting for Android's own close: as long as Android says, or as long as set.
        assertEquals("Auto", Tune.CloseWait.show(0f))
        assertEquals("Auto", Tune.CloseWait.show(4f))
        assertEquals("450 ms", Tune.CloseWait.show(450f))
        assertEquals("1200 ms", Tune.CloseWait.show(1200f))
    }

    @Test
    fun `the wait for Android's own close comes first in closing an app, and is Auto in every style`() {
        assertEquals(Tune.CloseWait, Tune.entries.first { it.group == TuneGroup.Close })
        AnimationPreset.entries.forEach { assertEquals(it.id, 0f, AnimationTuning.of(it)[Tune.CloseWait], 0f) }
        // Set by hand to its first step, it is a wait of its own and not Auto any more.
        assertTrue(AnimationTuning().with(Tune.CloseWait, Tune.CloseWait.step)[Tune.CloseWait] >= AndroidClose.WAIT_AUTO)
    }

    @Test
    fun `a picture of the app starts back into its icon as big as the screen, whatever the style`() {
        AnimationPreset.entries.forEach { assertEquals(it.id, 1f, AnimationTuning.of(it)[Tune.ClosePicture], 0f) }
        assertEquals(TuneGroup.Close, Tune.ClosePicture.group)
        // Never so small that it is only an icon again, and never bigger than the screen.
        assertEquals(0.3f, AnimationTuning().with(Tune.ClosePicture, 0.1f)[Tune.ClosePicture], 0f)
        assertEquals(1f, AnimationTuning().with(Tune.ClosePicture, 1.4f)[Tune.ClosePicture], 0f)
    }

    @Test
    fun `the card shrinking back into its icon has a time of its own, and lands a little flattened`() {
        val close = Tune.entries.filter { it.group == TuneGroup.Close }
        // After where it starts from, and before the landing, whose spring its squash springs back on.
        assertEquals(close.indexOf(Tune.ClosePicture) + 1, close.indexOf(Tune.CloseTime))
        assertEquals(close.indexOf(Tune.CloseTime) + 1, close.indexOf(Tune.CloseSquash))
        assertEquals(close.indexOf(Tune.CloseSquash) + 1, close.indexOf(Tune.LandScale))
        // About as long as Void's takes to get into its icon.
        assertEquals(TuneUnit.Ms, Tune.CloseTime.unit)
        assertTrue(Tune.CloseTime.own in 220f..340f)
        // Zenith's own lands flattened, as Void's does; Bouncy more, Fluid and Dynamic less, Classic not at all.
        val own = AnimationTuning()[Tune.CloseSquash]
        assertTrue(own >= 0.2f)
        assertTrue(AnimationTuning.of(AnimationPreset.Bouncy)[Tune.CloseSquash] > own)
        assertTrue(AnimationTuning.of(AnimationPreset.Fluid)[Tune.CloseSquash] < own)
        assertTrue(AnimationTuning.of(AnimationPreset.Dynamic)[Tune.CloseSquash] < own)
        assertEquals(0f, AnimationTuning.of(AnimationPreset.Classic)[Tune.CloseSquash], 0f)
        // Never flatter than half its icon.
        assertTrue(Tune.CloseSquash.max <= 0.5f)
        // Calm is longer; the speed runs it as it runs every other time, so Dynamic is quicker and Sweet slower.
        assertTrue(AnimationTuning.of(AnimationPreset.Fluid)[Tune.CloseTime] > Tune.CloseTime.own)
        assertEquals(Tune.CloseTime.own / 2f, AnimationTuning().paced(2f).ms(Tune.CloseTime), 0.01f)
        assertTrue(AnimationTuning.of(AnimationPreset.Dynamic).ms(Tune.CloseTime) < Tune.CloseTime.own)
        assertTrue(AnimationTuning.of(AnimationPreset.Sweet).ms(Tune.CloseTime) > Tune.CloseTime.own)
    }

    @Test
    fun `coming home like liquid has numbers of its own, after closing an app`() {
        assertEquals(TuneGroup.Arrive, TuneGroup.entries.last())
        assertEquals(
            listOf(Tune.ArriveWave, Tune.ArriveStiffness, Tune.ArriveDamping, Tune.ArriveFrom, Tune.ArrivePull, Tune.ArriveLight),
            Tune.entries.filter { it.group == TuneGroup.Arrive },
        )
        // Zenith's own rises a hair past its place; Fluid's never does, and Bouncy's more.
        assertTrue(AnimationTuning()[Tune.ArriveDamping] < 1f)
        assertEquals(1f, AnimationTuning.of(AnimationPreset.Fluid)[Tune.ArriveDamping], 0f)
        assertTrue(AnimationTuning.of(AnimationPreset.Bouncy)[Tune.ArriveDamping] < AnimationTuning()[Tune.ArriveDamping])
        // Classic is how opening and closing were, and coming home is not part of that.
        Tune.entries.filter { it.group == TuneGroup.Arrive }.forEach {
            assertEquals(it.id, it.own, AnimationTuning.of(AnimationPreset.Classic)[it], 0f)
        }
        assertEquals("520 ms", Tune.ArriveWave.show(520f))
        assertEquals("80%", Tune.ArriveFrom.show(0.8f))
        assertEquals("24 dp", Tune.ArrivePull.show(24f))
        assertEquals("60%", Tune.ArriveLight.show(0.6f))
    }
}

