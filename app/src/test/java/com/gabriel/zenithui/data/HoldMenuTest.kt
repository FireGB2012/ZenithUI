package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldMenuTest {

    private fun labels(subject: HoldSubject, locked: Boolean = false) =
        HoldMenuRules.rows(subject, locked).map {
            when (it) {
                is HoldEntry.Row -> it.label
                is HoldEntry.Sizes -> "<sizes>"
            }
        }

    private fun rows(subject: HoldSubject, locked: Boolean = false) =
        HoldMenuRules.rows(subject, locked).filterIsInstance<HoldEntry.Row>()

    // ------------------------------------------------------------------ the rows

    @Test
    fun `an app on a page gets its shortcuts first and Remove App last`() {
        val app = HoldSubject.App(shortcuts = listOf("Search for a Place", "Directions Home"))
        assertEquals(
            listOf(
                "Search for a Place", "Directions Home", "Edit Home Screen", "Edit Name and Icon",
                "Resize", "App Info", "Remove App",
            ),
            labels(app),
        )
        val all = rows(app)
        // Each shortcut carries the "+" that puts it on the home screen, and knows which it is.
        assertEquals(listOf(true, true), all.take(2).map { it.plus })
        assertEquals(listOf(0, 1), all.take(2).map { it.index })
        // Only the last row is the red one.
        assertEquals(listOf("Remove App"), all.filter { it.danger }.map { it.label })
        assertEquals(HoldDo.REMOVE, all.last().what)
    }

    @Test
    fun `a dock app can be put on a page, and is not resized there`() {
        assertEquals(
            listOf("Edit Home Screen", "Edit Name and Icon", "Add to Home Screen", "App Info", "Remove App"),
            labels(HoldSubject.App(inDock = true)),
        )
    }

    @Test
    fun `a pinned shortcut is only edited round or removed`() {
        assertEquals(listOf("Edit Home Screen", "Remove Shortcut"), labels(HoldSubject.Pinned))
        assertTrue(rows(HoldSubject.Pinned).last().danger)
    }

    @Test
    fun `a folder is renamed, coloured and removed`() {
        assertEquals(listOf("Rename", "Colour", "Edit Home Screen", "Remove Folder"), labels(HoldSubject.Folder))
    }

    private val sizes = listOf(
        SizeChoice("Small", 2, 4, current = false, fits = true),
        SizeChoice("Medium", 2, 8, current = true, fits = true),
        SizeChoice("Wide", 4, 8, current = false, fits = false),
    )

    @Test
    fun `one of Zenith's widgets leads with its sizes, then its set-up`() {
        assertEquals(
            listOf("<sizes>", "Edit Widget", "Colour", "Stack Another Widget", "Edit Home Screen", "Remove Widget"),
            labels(HoldSubject.ZenithWidget("weather", sizes)),
        )
        val first = HoldMenuRules.rows(HoldSubject.ZenithWidget("weather", sizes), locked = false).first()
        assertEquals(sizes, (first as HoldEntry.Sizes).choices)
    }

    @Test
    fun `a widget with one size has no size row, and one with nothing to set up no set-up row`() {
        assertEquals(
            listOf("Colour", "Stack Another Widget", "Edit Home Screen", "Remove Widget"),
            labels(HoldSubject.ZenithWidget("clock", sizes.take(1))),
        )
    }

    @Test
    fun `a set-up that is an action says what it does`() {
        fun setUp(kind: String, mediaAllowed: Boolean = true) =
            rows(HoldSubject.ZenithWidget(kind, mediaAllowed = mediaAllowed)).firstOrNull { it.what == HoldDo.SET_UP }?.label
        for (kind in listOf("toggles", "world", "countdown", "weather")) assertEquals(kind, "Edit Widget", setUp(kind))
        assertEquals("Choose Photos", setUp("photo"))
        assertEquals("Clear What Is Done", setUp("todo"))
        // Now playing only has something to ask for while it cannot see what is playing.
        assertEquals("Let Zenith See What Is Playing", setUp("media", mediaAllowed = false))
        assertNull(setUp("media", mediaAllowed = true))
        for (kind in listOf("clock", "battery", "calendar", "notes", "aura")) assertNull(kind, setUp(kind))
    }

    @Test
    fun `another app's widget offers the glass either way round`() {
        assertEquals(
            listOf("Put Glass Behind It", "Stack Another Widget", "Edit Home Screen", "Remove Widget"),
            labels(HoldSubject.HostedWidget(glass = false)),
        )
        assertEquals("Take the Glass Away", labels(HoldSubject.HostedWidget(glass = true)).first())
    }

    @Test
    fun `a stack lets each widget out by name, and takes no more when it is full`() {
        assertEquals(
            listOf("Stack Another Widget", "Take Clock Out", "Take Weather Out", "Edit Home Screen", "Remove Stack"),
            labels(HoldSubject.Stack(listOf("Clock", "Weather"), full = false)),
        )
        val full = HoldSubject.Stack(listOf("Clock", "Weather"), full = true)
        assertFalse("Stack Another Widget" in labels(full))
        assertEquals(listOf(0, 1), rows(full).filter { it.what == HoldDo.UNSTACK }.map { it.index })
    }

    @Test
    fun `locked, a menu offers to unlock first and nothing that moves, resizes or removes`() {
        assertEquals(
            listOf("Unlock Layout", "Open Maps", "Edit Name and Icon", "App Info"),
            labels(HoldSubject.App(shortcuts = listOf("Open Maps")), locked = true),
        )
        assertEquals(listOf("Unlock Layout", "Edit Name and Icon", "App Info"), labels(HoldSubject.App(inDock = true), locked = true))
        assertEquals(listOf("Unlock Layout"), labels(HoldSubject.Pinned, locked = true))
        assertEquals(listOf("Unlock Layout", "Rename", "Colour"), labels(HoldSubject.Folder, locked = true))
        assertEquals(
            listOf("Unlock Layout", "Edit Widget", "Colour"),
            labels(HoldSubject.ZenithWidget("weather", sizes), locked = true),
        )
        assertEquals(listOf("Unlock Layout", "Put Glass Behind It"), labels(HoldSubject.HostedWidget(glass = false), locked = true))
        // Taking a widget out of a stack is kept, as it is today.
        assertEquals(
            listOf("Unlock Layout", "Take Clock Out", "Take Weather Out"),
            labels(HoldSubject.Stack(listOf("Clock", "Weather"), full = false), locked = true),
        )
    }

    @Test
    fun `no menu ever has two red rows, and the red one is always last`() {
        val subjects = listOf(
            HoldSubject.App(listOf("One")), HoldSubject.App(inDock = true), HoldSubject.Pinned, HoldSubject.Folder,
            HoldSubject.ZenithWidget("weather", sizes), HoldSubject.HostedWidget(true), HoldSubject.Stack(listOf("A", "B"), false),
        )
        for (subject in subjects) {
            val all = rows(subject)
            assertEquals(subject.toString(), 1, all.count { it.danger })
            assertTrue(subject.toString(), all.last().danger)
        }
    }

    @Test
    fun `the menu's height is its rows, the size row with its gap, and its padding`() {
        val app = HoldMenuRules.rows(HoldSubject.App(listOf("One")), locked = false)
        // Six rows of 44 between 8 above and 8 below.
        assertEquals(6 * 44f + 16f, HoldMenuRules.height(app, row = 44f, sizeRow = 48f, gap = 8f, pad = 8f), 0.01f)
        val widget = HoldMenuRules.rows(HoldSubject.ZenithWidget("weather", sizes), locked = false)
        assertEquals(48f + 8f + 5 * 44f + 16f, HoldMenuRules.height(widget, row = 44f, sizeRow = 48f, gap = 8f, pad = 8f), 0.01f)
    }

    // ------------------------------------------------------------------ where it stands

    /** A phone 390 wide and 844 tall, the menu 250 wide: the mockup's numbers. */
    private fun place(left: Float, top: Float, right: Float, bottom: Float, height: Float = 280f) =
        HoldMenuPlace.place(
            itemLeft = left, itemTop = top, itemRight = right, itemBottom = bottom,
            menuWidth = 250f, menuHeight = height, screenWidth = 390f,
            roomTop = 50f, roomBottom = 818f, gap = 10f, margin = 10f,
        )

    @Test
    fun `the menu stands under an icon, its left edge on the icon's, when there is room`() {
        val at = place(left = 27f, top = 300f, right = 87f, bottom = 360f)
        assertFalse(at.above)
        assertEquals(370f, at.top, 0.01f)
        assertEquals(27f, at.left, 0.01f)
    }

    @Test
    fun `an icon in the right half lines the menu's right edge up with its own`() {
        val at = place(left = 303f, top = 300f, right = 363f, bottom = 360f)
        assertEquals(363f - 250f, at.left, 0.01f)
    }

    @Test
    fun `low on the screen the menu stands above, and says so`() {
        // A dock icon: no room under it.
        val at = place(left = 27f, top = 740f, right = 87f, bottom = 800f)
        assertTrue(at.above)
        assertEquals(740f - 10f - 280f, at.top, 0.01f)
    }

    @Test
    fun `with room neither above nor below it takes the roomier side and stays on the screen`() {
        // A tall widget in the middle, and a long menu.
        val at = place(left = 20f, top = 200f, right = 370f, bottom = 560f, height = 400f)
        // 258 under it against 150 over it: under, pushed up to stay inside the room.
        assertFalse(at.above)
        assertEquals(818f - 400f, at.top, 0.01f)
        // Nearer the foot there is more room above.
        val low = place(left = 20f, top = 380f, right = 370f, bottom = 740f, height = 400f)
        assertTrue(low.above)
        assertEquals(50f, low.top, 0.01f)
    }

    @Test
    fun `the menu never hangs off either side`() {
        // A widget as wide as the page, on a narrow phone.
        val wide = HoldMenuPlace.place(4f, 100f, 236f, 200f, 250f, 200f, 240f, 30f, 500f, 10f, 10f)
        assertEquals(0f, wide.left, 0.01f)
        val right = place(left = 340f, top = 300f, right = 400f, bottom = 360f)
        assertEquals(390f - 10f - 250f, right.left, 0.01f)
        val left = place(left = -20f, top = 300f, right = 40f, bottom = 360f)
        assertEquals(10f, left.left, 0.01f)
    }

    // ------------------------------------------------------------------ the finger

    @Test
    fun `a finger that wanders is a pick-up, one heading for the menu is not`() {
        // The menu is under the icon: straight down is on the way to a row.
        assertFalse(HoldGesture.picksUp(dx = 0f, dy = 30f, slop = 10f, above = false, onMenu = false, locked = false))
        assertFalse(HoldGesture.picksUp(dx = 20f, dy = 30f, slop = 10f, above = false, onMenu = false, locked = false))
        // Sideways, or up and away from it, picks the icon up.
        assertTrue(HoldGesture.picksUp(dx = 30f, dy = 5f, slop = 10f, above = false, onMenu = false, locked = false))
        assertTrue(HoldGesture.picksUp(dx = 0f, dy = -30f, slop = 10f, above = false, onMenu = false, locked = false))
        // With the menu above, it is the other way up.
        assertFalse(HoldGesture.picksUp(dx = 0f, dy = -30f, slop = 10f, above = true, onMenu = false, locked = false))
        assertTrue(HoldGesture.picksUp(dx = 0f, dy = 30f, slop = 10f, above = true, onMenu = false, locked = false))
    }

    @Test
    fun `a small tremble, a finger that has reached the menu, and a locked layout never pick up`() {
        assertFalse(HoldGesture.picksUp(dx = 6f, dy = -6f, slop = 10f, above = false, onMenu = false, locked = false))
        assertFalse(HoldGesture.picksUp(dx = 80f, dy = 0f, slop = 10f, above = false, onMenu = true, locked = false))
        assertFalse(HoldGesture.picksUp(dx = 80f, dy = 0f, slop = 10f, above = false, onMenu = false, locked = true))
    }

    // ------------------------------------------------------------------ the pour

    @Test
    fun `the liquid spring starts at rest, overshoots once when it is loose, and settles`() {
        assertEquals(0f, LiquidSpring.step(0f, 27f, 0.72f), 0f)
        assertEquals(0f, LiquidSpring.step(-0.1f, 27f, 0.72f), 0f)
        val peak = (1..60).maxOf { LiquidSpring.step(it / 100f, 27f, 0.72f) }
        assertTrue("it should overshoot a little: $peak", peak > 1.01f && peak < 1.12f)
        assertEquals(1f, LiquidSpring.step(1.5f, 27f, 0.72f), 0.002f)
        // Critically damped, it never passes its mark.
        assertTrue((1..200).all { LiquidSpring.step(it / 100f, 20f, 1f) <= 1f })
    }

    /** Maps held at (27..87, 300..360), lifted 1.15 times; its menu under it at (27, 370), 250 by 280. */
    private val pour = PourGeometry(
        above = false, iconCentreX = 57f, iconCentreY = 330f, iconHeight = 60f, lift = 1.15f,
        menuLeft = 27f, menuTop = 370f, menuWidth = 250f, menuHeight = 280f, unit = 1f,
    )

    @Test
    fun `the menu starts as a drop on the icon's lower edge, joined to it by a neck`() {
        val start = Pour.frameIn(0f, pour)
        // A drop about 22 across, hanging from the lifted icon's edge at 330 + 34.5.
        assertEquals(22f, start.width, 0.5f)
        assertTrue(start.height < 30f)
        // Under the icon's middle, but never nearer than 36 to the menu's own corner.
        assertEquals(63f, start.left + start.width / 2f, 0.5f)
        assertEquals(364.5f - 3f, start.top, 0.5f)
        assertNotNull(start.neck)
    }

    @Test
    fun `the neck has snapped by a quarter of a second and the menu is where it belongs at the end`() {
        assertNull(Pour.frameIn(0.25f, pour).neck)
        val end = Pour.frameIn(Pour.MS / 1000f + 0.6f, pour)
        assertEquals(27f, end.left, 0.6f)
        assertEquals(370f, end.top, 0.6f)
        assertEquals(250f, end.width, 0.6f)
        assertEquals(280f, end.height, 0.6f)
        assertEquals(28f, end.radius, 0.01f)
    }

    @Test
    fun `it is a touch wide before it is tall`() {
        // The width's spring leads the height's: one slosh, not a uniform zoom.
        val early = Pour.frameIn(0.08f, pour)
        assertTrue(early.width / 250f > early.height / 280f)
    }

    @Test
    fun `above an icon the drop hangs from its upper edge and the menu grows upwards`() {
        val up = pour.copy(above = true, iconCentreY = 770f, menuTop = 770f - 34.5f - 10f - 280f)
        val start = Pour.frameIn(0f, up)
        // Its far edge is above the icon's lifted top edge at 770 - 34.5.
        assertTrue(start.top + start.height <= 735.5f + 3.5f)
        val end = Pour.frameIn(1f, up)
        assertEquals(up.menuTop, end.top, 0.6f)
        assertEquals(280f, end.height, 0.6f)
    }

    @Test
    fun `drawn back, it ends as a small drop at the icon and fades only at the very end`() {
        val begin = Pour.frameOut(0f, pour)
        assertEquals(250f, begin.width, 0.01f)
        assertEquals(370f, begin.top, 0.01f)
        assertEquals(1f, begin.alpha, 0f)
        val end = Pour.frameOut(1f, pour)
        assertEquals(18f, end.width, 0.01f)
        assertEquals(63f, end.left + end.width / 2f, 0.01f)
        assertEquals(0f, end.alpha, 0f)
        assertEquals(1f, Pour.frameOut(0.5f, pour).alpha, 0f)
    }
}
