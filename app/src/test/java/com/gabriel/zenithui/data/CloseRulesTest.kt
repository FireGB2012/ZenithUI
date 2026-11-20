package com.gabriel.zenithui.data

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloseRulesTest {

    private fun route(
        style: String = CloseRules.INTO_ICON,
        asked: Boolean = false,
        target: Boolean = true,
        picture: Boolean = false,
        recent: Boolean = true,
        cardFrom: Float = 0f,
    ) = CloseRules.route(style, asked, target, picture, recent, cardFrom)

    @Test
    fun `when Android asks where the icon is, the real window goes into it`() {
        assertEquals(CloseRoute.Handshake, route(asked = true))
        // Ahead of a picture, which would only play the close a second time.
        assertEquals(CloseRoute.Handshake, route(asked = true, picture = true))
        // Asked, with no icon to give, Android is told there is none and does its own: the app
        // still goes some way of Zenith's after it, never nothing.
        assertEquals(CloseRoute.Picture, route(asked = true, target = false, picture = true))
        assertEquals(CloseRoute.Card, route(asked = true, target = false))
    }

    @Test
    fun `without Android's question, a picture of the app shrinks into its icon, or the middle`() {
        assertEquals(CloseRoute.Picture, route(picture = true))
        assertEquals(CloseRoute.Picture, route(picture = true, target = false))
        assertEquals(CloseRoute.Picture, route(picture = true, cardFrom = 0.6f))
    }

    @Test
    fun `with neither, the app's card shrinks into its icon, or the middle`() {
        // What a Galaxy, or any phone with buttons, gets from a copy of Zenith with no picture helper.
        assertEquals(CloseRoute.Card, route())
        // Left at Auto, or set to a size: the card is there either way.
        assertEquals(CloseRoute.Card, route(cardFrom = 0.004f))
        assertEquals(CloseRoute.Card, route(cardFrom = 0.4f))
        // No icon on show: into the middle of the screen, fading as it goes, as a picture does.
        assertEquals(CloseRoute.Card, route(target = false))
        // However long ago it was opened: Zenith knows it is the app closing (CloseRules.whoCloses).
        assertEquals(CloseRoute.Card, route(recent = false))
        assertEquals(CloseRoute.Card, route(recent = false, target = false, cardFrom = 0.4f))
        assertEquals(CloseRoute.Picture, route(recent = false, picture = true))
    }

    @Test
    fun `into its icon, an app Zenith knows is closing always closes some way of Zenith's`() {
        for (asked in listOf(false, true)) for (target in listOf(false, true)) for (picture in listOf(false, true)) {
            for (recent in listOf(false, true)) for (cardFrom in listOf(0f, 0.4f, 1f)) {
                val way = route(asked = asked, target = target, picture = picture, recent = recent, cardFrom = cardFrom)
                assertTrue("$asked $target $picture $recent $cardFrom: $way", way != CloseRoute.None && way != CloseRoute.Land)
            }
        }
    }

    @Test
    fun `back from an app the card fills the screen again unless it is set to a size, as Void's does`() {
        // The app as it was left, going back into its icon: Android's own close has taken the app
        // away by the time it starts (AndroidClose), so it does not carry on from part way.
        assertEquals(1f, CloseRules.CARD_START, 0f)
        assertEquals(CloseRules.CARD_START, CloseRules.cardStart(0f), 0f)
        assertEquals(CloseRules.CARD_START, CloseRules.cardStart(0.004f), 0f)
        assertEquals(0.4f, CloseRules.cardStart(0.4f), 0f)
        assertEquals(1f, CloseRules.cardStart(1f), 0f)
        // As big as a picture of the app starts.
        assertEquals(Tune.ClosePicture.own, CloseRules.CARD_START, 0f)
    }

    @Test
    fun `the icon on a closing card is as wide as the card, faint while it fills the screen and solid once it is small`() {
        // As wide as the card at the icon, and a little wider than it at full size.
        assertEquals(168f, CloseRules.ghostSide(168f, 0f), 0.01f)
        assertEquals(1080f * CloseRules.GHOST_SPAN, CloseRules.ghostSide(1080f, 1f), 0.01f)
        assertTrue(CloseRules.GHOST_SPAN in 1f..1.3f)
        // A spring carrying it a little past the icon: as wide as the card, no narrower.
        assertEquals(170f, CloseRules.ghostSide(170f, -0.02f), 0.01f)
        // Faint at full size, a ghost of the icon, and solid from GHOST_SOLID down.
        assertEquals(CloseRules.GHOST_FAINT, CloseRules.ghostAlpha(1f), 0.0001f)
        assertTrue(CloseRules.GHOST_FAINT in 0.01f..0.15f)
        assertEquals(1f, CloseRules.ghostAlpha(CloseRules.GHOST_SOLID), 0.0001f)
        assertEquals(1f, CloseRules.ghostAlpha(0f), 0f)
        assertEquals(1f, CloseRules.ghostAlpha(-0.03f), 0f)
        assertEquals(CloseRules.GHOST_FAINT, CloseRules.ghostAlpha(1.04f), 0.0001f)
        // Coming in steadily as the card shrinks, never fading back.
        var last = 2f
        for (i in 0..100) {
            val alpha = CloseRules.ghostAlpha(i / 100f)
            assertTrue(alpha <= last)
            last = alpha
        }
    }

    @Test
    fun `it goes into its icon slowly at first, the app back for a beat, then faster all the way in, landing with a bump`() {
        assertEquals(0f, CloseRules.intoIcon(0f), 0f)
        assertEquals(1f, CloseRules.intoIcon(1f), 0f)
        assertEquals(0f, CloseRules.intoIcon(-0.1f), 0f)
        assertEquals(1f, CloseRules.intoIcon(1.2f), 0f)
        // A third of the way through its time it has barely moved; half way, an eighth of its way,
        // as Void's card, still nearly the whole screen, is about to go.
        assertTrue(CloseRules.intoIcon(1f / 3f) < 0.05f)
        assertEquals(0.125f, CloseRules.intoIcon(0.5f), 0.001f)
        // Faster and faster: each step covers more than the one before, and the last, as it lands,
        // three times as much as an even pace would.
        var last = 0f
        var step = 0f
        for (i in 1..100) {
            val now = CloseRules.intoIcon(i / 100f)
            assertTrue(now - last >= step)
            step = now - last
            last = now
        }
        assertEquals(3f, (CloseRules.intoIcon(1f) - CloseRules.intoIcon(0.999f)) / 0.001f, 0.01f)
    }

    @Test
    fun `the home screen starts forward once the card is well on its way, so it is seen coming forward as the card lands`() {
        // Any sooner and it is over behind the card before it is seen, as Void's is not; any later
        // and it sits there sunk back with the card all but gone.
        assertTrue(CloseRules.HOME_GOES_AT in 0.5f..0.9f)
    }

    @Test
    fun `the home screen comes forward about the middle of its foot, where Android's own close left the app`() {
        val (across, down) = CloseRules.comeBackPivot()
        assertEquals(0.5f, across, 0f)
        assertTrue(down in 0.8f..0.95f)
    }

    @Test
    fun `the card goes into its icon where it is drawn while the home screen comes forward about its foot`() {
        // Mail's icon on a 1233 by 2673 screen, at rest.
        val rest = Rect(656f, 980f, 865f, 1189f)
        // At rest it is where it is.
        assertEquals(rest, CloseRules.fromFoot(rest, zoom = 1f, width = 1233f, height = 2673f))
        // Sunk back to 90%, an icon above the foot is drawn lower, nearer it, and smaller.
        val sunk = CloseRules.fromFoot(rest, zoom = 0.9f, width = 1233f, height = 2673f)
        val footY = 2673f * CloseRules.HOME_FOOT
        assertEquals(footY + (rest.center.y - footY) * 0.9f, sunk.center.y, 0.01f)
        assertEquals(616.5f + (rest.center.x - 616.5f) * 0.9f, sunk.center.x, 0.01f)
        assertEquals(rest.width * 0.9f, sunk.width, 0.01f)
        assertEquals(rest.height * 0.9f, sunk.height, 0.01f)
        assertTrue(sunk.center.y > rest.center.y)
        // A little past its size, as the home screen's spring carries it, it is drawn a little higher.
        assertTrue(CloseRules.fromFoot(rest, zoom = 1.01f, width = 1233f, height = 2673f).center.y < rest.center.y)
        // Something at the foot itself stays put, whatever the zoom.
        val atFoot = Rect(600f, footY - 10f, 633f, footY + 10f)
        assertEquals(atFoot.center.y, CloseRules.fromFoot(atFoot, zoom = 0.9f, width = 1233f, height = 2673f).center.y, 0.01f)
    }

    @Test
    fun `after Android's own close a card brings the home screen with it, and nothing else does`() {
        assertTrue(CloseRules.bringsHome(waited = true, route = CloseRoute.Card))
        assertTrue(CloseRules.bringsHome(waited = true, route = CloseRoute.Picture))
        // Not waited for (Android's window going into the icon, movement reduced): it arrives as ever.
        assertFalse(CloseRules.bringsHome(waited = false, route = CloseRoute.Card))
        assertFalse(CloseRules.bringsHome(waited = false, route = CloseRoute.Picture))
        // Only the icon landing, Android's window, or nothing of Zenith's: it arrives on its own.
        assertFalse(CloseRules.bringsHome(waited = true, route = CloseRoute.Land))
        assertFalse(CloseRules.bringsHome(waited = true, route = CloseRoute.Handshake))
        assertFalse(CloseRules.bringsHome(waited = true, route = CloseRoute.None))
        assertFalse(CloseRules.bringsHome(waited = true, route = null))
    }

    @Test
    fun `the card goes up at once only to bring the home screen back after a wait, on a phone that shows Zenith late`() {
        val late = AndroidClose.Plan(atLeastMs = 600, atMostMs = 900, listens = true, cardFirst = true)
        val prompt = AndroidClose.Plan(atLeastMs = 400, atMostMs = 800, listens = true)
        assertTrue(CloseRules.cardFirst(late, CloseRoute.Card))
        assertTrue(CloseRules.cardFirst(late, CloseRoute.Picture))
        // A phone that shows Zenith as it is: the home screen waits empty, and the card goes up after.
        assertFalse(CloseRules.cardFirst(prompt, CloseRoute.Card))
        // Nothing waited for (movement reduced, Android's animations off): no card held up either.
        assertFalse(CloseRules.cardFirst(null, CloseRoute.Card))
        for (route in listOf(CloseRoute.Land, CloseRoute.Handshake, CloseRoute.None, null)) {
            assertFalse("$route", CloseRules.cardFirst(late, route))
        }
    }

    @Test
    fun `its squash comes to rest within a pixel, so it is never seen to jump back into shape`() {
        // On the biggest icon a phone shows, about 230 pixels; Compose's own, 0.01, is two of them or more.
        assertTrue(CloseRules.SQUASH_REST > 0f)
        assertTrue(CloseRules.SQUASH_REST * 230f <= 1.5f)
    }

    @Test
    fun `the card gives way to the icon under it in a few frames, neither a cut nor a fade to watch`() {
        assertTrue(CloseRules.HAND_OVER_MS in 50..150)
    }

    @Test
    fun `Plain closes as it did before, whatever Android or the helper offers`() {
        assertEquals(CloseRoute.Land, route(style = CloseRules.PLAIN, asked = true, picture = true))
        assertEquals(CloseRoute.Card, route(style = CloseRules.PLAIN, picture = true, cardFrom = 0.5f))
        assertEquals(CloseRoute.None, route(style = CloseRules.PLAIN, target = false, picture = true))
        // A style this version has not heard of is Zenith's own.
        assertEquals(CloseRoute.Handshake, route(style = "somethingNew", asked = true))
    }

    @Test
    fun `the picture comes in over the way out and goes over the same stretch on the way back`() {
        assertEquals(0f, CloseRules.pictureShare(0f), 0f)
        assertEquals(0f, CloseRules.pictureShare(CloseRules.PICTURE_FROM), 0f)
        assertEquals(1f, CloseRules.pictureShare(CloseRules.PICTURE_TO), 0f)
        assertEquals(1f, CloseRules.pictureShare(1.04f), 0f)
        assertEquals(0.5f, CloseRules.pictureShare((CloseRules.PICTURE_FROM + CloseRules.PICTURE_TO) / 2f), 0.001f)
        // Always on its way in as the card grows.
        var last = -1f
        for (i in 0..100) {
            val share = CloseRules.pictureShare(i / 100f)
            assertTrue(share >= last)
            last = share
        }
    }

    @Test
    fun `a closing card fades in over its first stretch, so it never pops up over the home screen`() {
        assertEquals(0f, CloseRules.closingAlpha(0.92f, from = 0.92f), 0f)
        assertEquals(0.5f, CloseRules.closingAlpha(0.92f - CloseRules.CLOSE_FADE_IN / 2f, from = 0.92f), 0.001f)
        assertEquals(1f, CloseRules.closingAlpha(0.92f - CloseRules.CLOSE_FADE_IN, from = 0.92f), 0f)
        assertEquals(1f, CloseRules.closingAlpha(0f, from = 0.92f), 0f)
        // A spring carrying it a little past the icon keeps it whole.
        assertEquals(1f, CloseRules.closingAlpha(-0.03f, from = 0.92f), 0f)
    }

    @Test
    fun `one that fills the screen is solid from its first frame, the app back as it was left, as Void's is`() {
        // Over nothing but the wallpaper, after Android's own close: faded in, the home screen
        // brought back behind it would show through it for its first frames.
        assertEquals(1f, CloseRules.closingAlpha(1f, from = 1f), 0f)
        assertEquals(1f, CloseRules.closingAlpha(1.03f, from = 1f), 0f)
        assertEquals(1f, CloseRules.closingAlpha(0.5f, from = 1f), 0f)
        assertEquals(1f, CloseRules.closingAlpha(-0.02f, from = 1f), 0f)
    }

    @Test
    fun `with no icon to go into, it fades out over its last stretch`() {
        assertEquals(1f, CloseRules.vanishAlpha(1f), 0f)
        assertEquals(1f, CloseRules.vanishAlpha(CloseRules.VANISH), 0f)
        assertEquals(0.5f, CloseRules.vanishAlpha(CloseRules.VANISH / 2f), 0.001f)
        assertEquals(0f, CloseRules.vanishAlpha(0f), 0f)
        assertEquals(0f, CloseRules.vanishAlpha(-0.05f), 0f)
    }

    @Test
    fun `a picture fills the card without being stretched, keeping its middle`() {
        // A 576 by 1280 picture into a square: all of its width, the middle of its height.
        assertEquals(CloseRules.Crop(0, 352, 576, 576), CloseRules.coverCrop(576, 1280, 168f, 168f))
        // Into a card the shape of the screen it was taken on: all of it.
        assertEquals(CloseRules.Crop(0, 0, 576, 1280), CloseRules.coverCrop(576, 1280, 1080f, 2400f))
        // Into a card wider than it: all of its height, the middle of its width.
        assertEquals(CloseRules.Crop(144, 0, 288, 1280), CloseRules.coverCrop(576, 1280, 300f, 1333.3334f))
        // Nothing to fill, or nothing to fill it with.
        assertEquals(CloseRules.Crop(0, 0, 576, 1280), CloseRules.coverCrop(576, 1280, 0f, 0f))
        assertEquals(CloseRules.Crop(0, 0, 0, 0), CloseRules.coverCrop(0, 0, 100f, 100f))
    }

    @Test
    fun `an app with no icon on show goes into the middle of the screen`() {
        assertEquals(Rect(510f, 1170f, 570f, 1230f), CloseRules.middle(1080f, 2400f, 60f))
    }

    @Test
    fun `two keys are the same app when they are the same package in the same profile`() {
        assertTrue(CloseRules.sameApp("com.example.mail/.Main", "com.example.mail/.Compose"))
        assertTrue(CloseRules.sameApp("shortcut:com.example.mail/compose/new", "com.example.mail/.Main"))
        assertTrue(CloseRules.sameApp("work:com.example.mail/.Main", "work:com.example.mail/.Inbox"))
        assertFalse(CloseRules.sameApp("work:com.example.mail/.Main", "com.example.mail/.Main"))
        assertFalse(CloseRules.sameApp("com.example.mail/.Main", "com.example.maps/.Main"))
    }

    private val mail = "com.example.mail/.Main"
    private val maps = "com.example.maps/.Main"
    private val notes = "com.example.notes/.Main"

    @Test
    fun `the app Android was told about is the one closing, whatever else says otherwise`() {
        val pick = CloseRules.whoCloses(answered = mail, named = maps, seen = maps, opened = notes, remembered = notes)
        assertEquals(CloseRules.Pick(mail, CloseSource.Answered), pick)
    }

    @Test
    fun `then the one Android named, then the one the helper saw, ahead of the one Zenith opened`() {
        // Opened from a notification, or from the app Zenith opened, or switched to: the one in front.
        assertEquals(CloseRules.Pick(maps, CloseSource.Named), CloseRules.whoCloses(null, maps, notes, mail, mail))
        assertEquals(CloseRules.Pick(maps, CloseSource.Seen), CloseRules.whoCloses(null, null, maps, mail, mail))
    }

    @Test
    fun `named as the app Zenith opened, the key of the icon it was opened from is kept`() {
        // Android names the screen of it that was in front, which may not be the one its icon opens.
        val compose = "com.example.mail/.Compose"
        assertEquals(CloseRules.Pick(mail, CloseSource.Named), CloseRules.whoCloses(null, compose, null, mail, null))
        assertEquals(CloseRules.Pick(mail, CloseSource.Named), CloseRules.whoCloses(null, compose, null, null, mail))
        assertEquals(CloseRules.Pick(mail, CloseSource.Seen), CloseRules.whoCloses(null, null, compose, mail, null))
        // The work profile's copy of it is another app.
        val work = "work:com.example.mail/.Main"
        assertEquals(CloseRules.Pick(work, CloseSource.Named), CloseRules.whoCloses(null, work, null, mail, mail))
    }

    @Test
    fun `told nothing, the app Zenith opened on its card, then the last one it opened any way`() {
        assertEquals(CloseRules.Pick(mail, CloseSource.Opened), CloseRules.whoCloses(null, null, null, mail, maps))
        assertEquals(CloseRules.Pick(maps, CloseSource.Remembered), CloseRules.whoCloses(null, null, null, null, maps))
        // Nothing at all: an app Zenith knows nothing of, and the home screen comes in on its own.
        assertEquals(null, CloseRules.whoCloses(null, null, null, null, null))
    }

    @Test
    fun `Android's question names the app closing only if it was asked since Zenith was covered, and lately`() {
        assertTrue(CloseRules.namedFresh(at = 1_000, coveredAt = 500, now = 1_200))
        assertTrue(CloseRules.namedFresh(at = 1_000, coveredAt = 500, now = 1_000 + CloseRules.NAMED_FRESH_MS))
        // A Zenith started afresh is asked a second or more after it was shown.
        assertTrue(CloseRules.NAMED_FRESH_MS >= 2_000L)
        // Asked about an earlier visit, before this one covered Zenith.
        assertFalse(CloseRules.namedFresh(at = 400, coveredAt = 500, now = 1_200))
        assertFalse(CloseRules.namedFresh(at = 500, coveredAt = 500, now = 1_200))
        // Long ago, or a clock that has gone backwards.
        assertFalse(CloseRules.namedFresh(at = 1_000, coveredAt = 500, now = 1_001 + CloseRules.NAMED_FRESH_MS))
        assertFalse(CloseRules.namedFresh(at = 1_000, coveredAt = 500, now = 900))
    }

    @Test
    fun `an app that opened as the screen went off, closed after the unlock, is waited out like any other`() {
        // Covered with the screen on: an app.
        assertTrue(CloseRules.fromApp(coveredByApp = true, homeAt = 0, coveredAt = 500, known = false))
        // Covered with the screen off, then sent home with an app to close: it came up behind the lock.
        assertTrue(CloseRules.fromApp(coveredByApp = false, homeAt = 900, coveredAt = 500, known = true))
        // A plain unlock sends nothing home; a home press from before does not count.
        assertFalse(CloseRules.fromApp(coveredByApp = false, homeAt = 0, coveredAt = 500, known = true))
        assertFalse(CloseRules.fromApp(coveredByApp = false, homeAt = 400, coveredAt = 500, known = true))
        // Sent home with nothing Zenith knows of to close: the home screen is there at once.
        assertFalse(CloseRules.fromApp(coveredByApp = false, homeAt = 900, coveredAt = 500, known = false))
    }

    @Test
    fun `a phone that twice in a row never says the window got there is not told where icons are any more`() {
        assertTrue(CloseRules.trustsHandshake(0))
        // Once could be a close cut short.
        assertTrue(CloseRules.trustsHandshake(1))
        assertFalse(CloseRules.trustsHandshake(2))
        assertFalse(CloseRules.trustsHandshake(7))
        assertEquals(2, CloseRules.HANDSHAKE_MISSES)
    }

    @Test
    fun `a card held up for Android's close is let go of before long, whatever happens`() {
        val late = AndroidClose.Plan(atLeastMs = 600, atMostMs = 900, listens = true, cardFirst = true)
        assertEquals(900L + CloseRules.HOLD_SLACK_MS, CloseRules.holdCap(late))
        assertEquals(CloseRules.HOLD_SLACK_MS, CloseRules.holdCap(null))
        // Long enough for Android's word to be late, never so long the screen looks stuck.
        assertTrue(CloseRules.HOLD_SLACK_MS in 500L..3_000L)
    }

    @Test
    fun `a closing card is never kept from going up for long by its icon`() {
        // About a quarter of a second: a few frames, then its own colour.
        assertTrue(CloseRules.ICON_WAIT_MS in 100L..400L)
    }

    @Test
    fun `a card left up by an app that came over Zenith without covering it goes back into its icon`() {
        assertTrue(CloseRules.cameBackOver(away = false, opening = true, launched = true, settled = true))
        // Covered after all: it closes the usual way as Zenith comes forward.
        assertFalse(CloseRules.cameBackOver(away = true, opening = true, launched = true, settled = true))
        // Still growing, or the app not yet asked to open: its own check sees to it.
        assertFalse(CloseRules.cameBackOver(away = false, opening = true, launched = true, settled = false))
        assertFalse(CloseRules.cameBackOver(away = false, opening = true, launched = false, settled = true))
        // No card opening an app (one closing, or none).
        assertFalse(CloseRules.cameBackOver(away = false, opening = false, launched = true, settled = true))
    }

    @Test
    fun `which app a key opens`() {
        assertEquals("com.example.mail", CloseRules.packageOf("com.example.mail/com.example.mail.Main"))
        assertEquals("com.example.mail", CloseRules.packageOf("work:com.example.mail/com.example.mail.Main"))
        assertEquals("com.example.mail", CloseRules.packageOf("shortcut:com.example.mail/compose/new"))
        assertEquals("", CloseRules.packageOf("shortcut:broken"))
        assertTrue(CloseRules.isWork("work:com.example.mail/com.example.mail.Main"))
        assertTrue(!CloseRules.isWork("com.example.mail/com.example.mail.Main"))
        assertEquals("com.example.mail/.Main", CloseRules.keyOf("com.example.mail", ".Main", work = false))
        assertEquals("work:com.example.mail/.Main", CloseRules.keyOf("com.example.mail", ".Main", work = true))
    }
}
