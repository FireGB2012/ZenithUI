# Stage 1: the home grid lines up — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nothing on the home screen can end up out of line unless Plus free placement is on.

**Architecture:** A pure Kotlin `HomeGeometry` works out every position on a page in whole pixels, and the page, drop hint, resize outline, drag ghost, dock, header and search bar all read from it. Placement (`DropPlanner`, `LayoutBuilder`) keeps to whole rows; tucking is removed; the search bar becomes a setting. A one-time upgrade (`LayoutTidy`) brings saved layouts onto the new rules.

**Tech Stack:** Kotlin 2.2, Jetpack Compose (BOM 2024.12.01), JUnit 4 for JVM tests, no third-party libraries.

**Spec:** `specs/2026-10-01-zenith-overhaul-design.md`, section "Stage 1: the home grid lines up".

## Global Constraints

- minSdk 29, targetSdk 37, Compose BOM 2024.12.01, no new app dependencies.
- `ROW_UNITS` stays 4 and the saved layout JSON format does not change.
- Plus free placement keeps today's behaviour: any step, any size, with offsets.
- `widgetTuck` stays in `ZenithSettings` (old backups must read) but is always 0.
- An item lands where it is let go, at most half a row away; neighbours step aside; nothing flies across the page (`DropPlannerTest` sweep must keep passing).
- Build and test with `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` in Git Bash, from the repo root.
- Never run `connectedDebugAndroidTest`. Hands-on checks go on the `Android10_test` emulator; the finished build is installed on `Pixel_10a` (`emulator-5554`) last. Launch with `am start -n`, never `monkey`.
- Code comments match the file they are in: plain sentences that say why.

## Review Focus

1. An older backup restored in a running session (version 1, with a search widget and a tucked icon) must be tidied at once, not on the next start. Pinned by `LayoutTidyTest` and by `restoreBackup` calling `upgradeLayout()` (Task 3).
2. Rounding widget heights up can overfill a page: the item with no room goes to the next page and nothing is lost. Pinned by `LayoutBuilderTest` (Task 2) and `LayoutTidyTest` (Task 3).
3. A dock that already holds more apps than there are columns still shows all of them, inside the grid, and takes no more. Pinned by `HomeGeometryTest` (Task 1) and `toggleDock`/`moveToDock` (Task 3).
4. Labels off: the label strip is zero high and a widget ends where its last icons end. Pinned by `HomeGeometryTest` (Task 1).
5. A page whose size does not divide evenly by the grid: every cell is still the same size and no inset is negative. Pinned by `HomeGeometryTest` (Task 1).

## File structure

| File | Responsibility |
| --- | --- |
| `app/src/main/java/com/gabriel/zenithui/data/HomeGeometry.kt` (new) | Every position on a home page, in whole pixels. Pure. |
| `app/src/main/java/com/gabriel/zenithui/data/LayoutTidy.kt` (new) | Brings a saved layout onto whole rows; turns the search widget into a setting. Pure. |
| `app/src/main/java/com/gabriel/zenithui/data/HomeHeader.kt` (new) | The line written at the top of the home screen. Pure. |
| `data/Models.kt`, `data/LayoutBuilder.kt`, `data/DropPlanner.kt`, `data/Settings.kt` | Whole-row rules, no tuck, two new settings. |
| `ZenithViewModel.kt` | Upgrade path, placement step, dock limit. |
| `ui/home/HomeGrid.kt`, `ui/home/HomeScreen.kt` | Draw from `HomeGeometry`; dock slots; header; search bar. |
| `ui/home/ZenithWidgets.kt`, `ui/home/Sheets.kt`, `ui/home/SettingsSheet.kt` | Search bar composable, widget picker list, two settings rows. |
| `ui/onboarding/PhonePreview.kt`, `ui/onboarding/OnboardingFlow.kt` | The preview phone uses the same geometry and shows the search bar. |
| `app/src/test/java/com/gabriel/zenithui/data/*Test.kt` | JVM tests for all pure pieces. |

All source paths below are under `app/src/main/java/com/gabriel/zenithui/` and all test paths under `app/src/test/java/com/gabriel/zenithui/` unless written in full.

---

### Task 1: `HomeGeometry`

**Files:**
- Create: `data/HomeGeometry.kt`
- Test: `data/HomeGeometryTest.kt`

**Interfaces:**
- Consumes: `ROW_UNITS` from `data/Models.kt`.
- Produces: `data class PxRect(left, top, right, bottom)` with `width`, `height`; `class HomeGeometry(width: Int, height: Int, columns: Int, rows: Int, density: Float, showLabels: Boolean)` with `cellWidth: Int`, `rowHeight: Int`, `unitHeight: Float`, `gridWidth`, `gridHeight`, `originX`, `originY`, `iconDp: Float`, `icon: Int`, `label: Int`, `sideInset`, `sideInsetEnd`, `topInset`, `underInset`, `iconFrame: PxRect`, `leftLine: Int`, `rightLine: Int`, `widgetFrame(spanX: Int, spanY: Int): PxRect`, `superIconFrame(spanX: Int, rowsTall: Int): PxRect`, `dockIconLeft(index: Int, count: Int): Int`, `dockPane(count: Int): Pair<Int, Int>`, and `HomeGeometry.LABEL_DP`, `HomeGeometry.iconSizeDp(cellWidthDp, rowHeightDp, showLabels)`.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/gabriel/zenithui/data/HomeGeometryTest.kt`:

```kotlin
package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeGeometryTest {

    /** Every grid a phone can end up with, at three densities, on pages that do not divide evenly. */
    private fun everyPage(check: (HomeGeometry) -> Unit) {
        for (density in listOf(2.0f, 2.625f, 3.5f)) {
            for (columns in 3..6) for (rows in 4..7) for (labels in listOf(true, false)) {
                for (widthDp in listOf(336f, 387.4f, 411f)) for (heightDp in listOf(560f, 657.3f, 720f)) {
                    check(HomeGeometry((widthDp * density).toInt(), (heightDp * density).toInt(), columns, rows, density, labels))
                }
            }
        }
    }

    /** The Pixel 10a's page: 1080 px less the page margins, at 2.625 px per dp, 4 by 5. */
    private val pixel = HomeGeometry(width = 1016, height = 1724, columns = 4, rows = 5, density = 2.625f, showLabels = true)

    @Test
    fun `every cell is the same size and the spare pixels are shared between the edges`() = everyPage { g ->
        assertEquals(g.width / g.columns, g.cellWidth)
        assertEquals(g.height / g.rows, g.rowHeight)
        assertEquals(g.cellWidth * g.columns, g.gridWidth)
        assertEquals(g.rowHeight * g.rows, g.gridHeight)
        assertTrue(g.originX >= 0 && g.width - g.gridWidth - g.originX * 2 in 0..1)
        assertTrue(g.originY >= 0 && g.height - g.gridHeight - g.originY * 2 in 0..1)
        assertEquals(g.rowHeight.toFloat(), g.unitHeight * ROW_UNITS, 0.001f)
    }

    @Test
    fun `an icon sits inside its cell with its label under it`() = everyPage { g ->
        val icon = g.iconFrame
        assertEquals(g.icon, icon.width)
        assertEquals(g.icon, icon.height)
        assertTrue(icon.left >= 0 && icon.right <= g.cellWidth)
        assertTrue(icon.top >= 0 && icon.bottom + g.label <= g.rowHeight)
        assertEquals(g.cellWidth, g.sideInset + g.icon + g.sideInsetEnd)
        assertEquals(g.rowHeight, g.topInset + g.icon + g.label + g.underInset)
    }

    @Test
    fun `a widget runs from icon edge to icon edge`() = everyPage { g ->
        for (spanX in 1..g.columns) {
            val widget = g.widgetFrame(spanX, 2 * ROW_UNITS)
            assertEquals(g.iconFrame.left, widget.left)
            assertEquals((spanX - 1) * g.cellWidth + g.iconFrame.right, widget.right)
        }
    }

    @Test
    fun `a widget runs from the top of its icons to the bottom of its last labels`() = everyPage { g ->
        for (rows in 1..g.rows) {
            val widget = g.widgetFrame(2, rows * ROW_UNITS)
            assertEquals(g.iconFrame.top, widget.top)
            assertEquals((rows - 1) * g.rowHeight + g.iconFrame.bottom + g.label, widget.bottom)
        }
    }

    @Test
    fun `the gap under a widget is the gap under a label`() = everyPage { g ->
        assertEquals(g.underInset, 2 * g.rowHeight - g.widgetFrame(2, 2 * ROW_UNITS).bottom)
    }

    @Test
    fun `with labels off a widget ends where its last icons end`() {
        val g = HomeGeometry(1016, 1724, 4, 5, 2.625f, showLabels = false)
        assertEquals(0, g.label)
        assertEquals(g.rowHeight + g.iconFrame.bottom, g.widgetFrame(2, 2 * ROW_UNITS).bottom)
    }

    @Test
    fun `a widget shorter than a row keeps its whole height`() {
        // Free placement only: a strip half a row tall.
        val strip = pixel.widgetFrame(4, 2)
        assertEquals(0, strip.top)
        assertEquals((2 * pixel.unitHeight).roundToInt(), strip.bottom)
        assertEquals(pixel.iconFrame.left, strip.left)
    }

    @Test
    fun `a super icon spans icon edge to icon edge and one row of it is an icon tall`() = everyPage { g ->
        val wide = g.superIconFrame(spanX = 2, rowsTall = 1)
        assertEquals(g.iconFrame.left, wide.left)
        assertEquals(g.cellWidth + g.iconFrame.right, wide.right)
        assertEquals(g.iconFrame.top, wide.top)
        assertEquals(g.iconFrame.bottom, wide.bottom)
        // Two by two sits between the top of its first icons and the bottom of its last.
        val square = g.superIconFrame(spanX = 2, rowsTall = 2)
        assertTrue(square.top >= g.iconFrame.top && square.bottom <= g.rowHeight + g.iconFrame.bottom)
        assertTrue(square.height <= square.width)
    }

    @Test
    fun `a dock with one app for each column stands under the columns`() = everyPage { g ->
        for (index in 0 until g.columns) {
            assertEquals(index * g.cellWidth + g.iconFrame.left, g.dockIconLeft(index, g.columns))
        }
        assertEquals(0 to g.gridWidth, g.dockPane(g.columns))
    }

    @Test
    fun `a smaller dock is centred on the same spacing`() = everyPage { g ->
        for (count in 1 until g.columns) {
            for (index in 1 until count) {
                assertEquals(g.cellWidth, g.dockIconLeft(index, count) - g.dockIconLeft(index - 1, count))
            }
            val (left, right) = g.dockPane(count)
            assertEquals(count * g.cellWidth, right - left)
            // As far from the left edge as from the right, give or take a pixel of rounding.
            assertTrue(abs(left - (g.gridWidth - right)) <= 1)
            assertEquals(left + g.sideInset, g.dockIconLeft(0, count))
        }
    }

    @Test
    fun `a dock with more apps than columns is spread evenly inside the grid`() {
        val count = pixel.columns + 1
        val lefts = (0 until count).map { pixel.dockIconLeft(it, count) }
        assertTrue(lefts.first() >= 0 && lefts.last() + pixel.icon <= pixel.gridWidth)
        assertTrue(lefts.zipWithNext().all { (a, b) -> b - a >= pixel.icon })
        assertEquals(0 to pixel.gridWidth, pixel.dockPane(count))
    }

    @Test
    fun `an empty dock is as wide as the grid`() {
        assertEquals(0 to pixel.gridWidth, pixel.dockPane(0))
    }

    @Test
    fun `the page's left and right lines are the outer icon edges`() = everyPage { g ->
        assertEquals(g.iconFrame.left, g.leftLine)
        assertEquals((g.columns - 1) * g.cellWidth + g.iconFrame.right, g.rightLine)
    }

    @Test
    fun `icons are sized as they always were`() {
        // Three quarters of the cell, held between 54 and 72 dp...
        assertEquals(72f, HomeGeometry.iconSizeDp(96.8f, 131f, showLabels = true), 0.001f)
        assertEquals(58.5f, HomeGeometry.iconSizeDp(78f, 131f, showLabels = true), 0.001f)
        // ...never so tall that the label is pushed out of the row...
        assertEquals(48f, HomeGeometry.iconSizeDp(96.8f, 80f, showLabels = true), 0.001f)
        // ...and with labels off the row gives the icon that room back.
        assertEquals(68f, HomeGeometry.iconSizeDp(96.8f, 80f, showLabels = false), 0.001f)
        assertEquals(189, pixel.icon)
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gabriel.zenithui.data.HomeGeometryTest" --console=plain`
Expected: compilation fails with `Unresolved reference: HomeGeometry`.

- [ ] **Step 3: Write `data/HomeGeometry.kt`**

```kotlin
package com.gabriel.zenithui.data

import kotlin.math.roundToInt

/** A rectangle in whole pixels, measured from the top left corner of whatever it sits in. */
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Where everything on a home page sits, in whole pixels. The page, the drop hint, the resize
 * outline, the dock, the name at the top and the search bar all ask the same object, so two edges
 * that should meet are one number rather than two roundings of it.
 *
 * Every cell is the same whole number of pixels wide and every row the same number tall. Pixels
 * left over when the page does not divide evenly are shared between the outer edges ([originX],
 * [originY]) rather than making some cells a pixel bigger than their neighbours.
 */
class HomeGeometry(
    /** The page area in pixels: the screen less the page's own margins. */
    val width: Int,
    val height: Int,
    val columns: Int,
    val rows: Int,
    /** Pixels per dp. */
    val density: Float,
    val showLabels: Boolean,
) {
    val cellWidth: Int = width / columns
    val rowHeight: Int = height / rows

    /** A row unit, a quarter of a row. Only free placement puts anything between rows. */
    val unitHeight: Float = rowHeight / ROW_UNITS.toFloat()

    val gridWidth: Int = cellWidth * columns
    val gridHeight: Int = rowHeight * rows

    /** The grid's top left corner inside the page area. */
    val originX: Int = (width - gridWidth) / 2
    val originY: Int = (height - gridHeight) / 2

    /** The icon size in dp, as it is handed to an icon, and the pixels that comes to. */
    val iconDp: Float = iconSizeDp(cellWidth / density, rowHeight / density, showLabels)
    val icon: Int = (iconDp * density).roundToInt()

    /** The strip a label is written in under an icon; nothing when labels are off. */
    val label: Int = if (showLabels) (LABEL_DP * density).roundToInt() else 0

    /** From a cell's left edge to its icon, and from the icon to the cell's right edge. */
    val sideInset: Int = ((cellWidth - icon) / 2).coerceAtLeast(0)
    val sideInsetEnd: Int = (cellWidth - icon - sideInset).coerceAtLeast(0)

    /** From a row's top edge to its icons. */
    val topInset: Int = ((rowHeight - icon - label) / 2).coerceAtLeast(0)

    /** From the bottom of a row's labels to the bottom edge of the row. */
    val underInset: Int = (rowHeight - topInset - icon - label).coerceAtLeast(0)

    /** Where an icon is drawn inside its own cell. */
    val iconFrame: PxRect = PxRect(sideInset, topInset, sideInset + icon, topInset + icon)

    /** The page's left line, the left edge of the first column's icons, from the grid's left edge. */
    val leftLine: Int get() = sideInset

    /** The page's right line, the right edge of the last column's icons, from the grid's left edge. */
    val rightLine: Int get() = gridWidth - sideInsetEnd

    /**
     * Where a widget [spanX] columns wide and [spanY] row units tall is drawn inside the cells it
     * covers: from the left edge of the icon in its first column to the right edge of the icon in
     * its last, and from the top of the icons in its first row to the bottom of the labels in its
     * last. That leaves the same gap under a widget as under any icon's label. One shorter than a
     * row, which only free placement allows, keeps its whole height.
     */
    fun widgetFrame(spanX: Int, spanY: Int): PxRect {
        val bottom = (spanY * unitHeight).roundToInt()
        val right = spanX * cellWidth - sideInsetEnd
        return if (spanY >= ROW_UNITS) {
            PxRect(sideInset, topInset, right, bottom - underInset)
        } else {
            PxRect(sideInset, 0, right, bottom)
        }
    }

    /**
     * Where a super icon [spanX] columns wide and [rowsTall] rows tall is drawn inside the cells
     * it covers. It runs from the left edge of the icon in its first column to the right edge of
     * the icon in its last. Going down, each row past the first adds a column's width rather than
     * a whole row, so it keeps a wide one's proportions: a 2 by 2 is square. It sits centred
     * between the top of the icons in its first row and the bottom of the icons in its last.
     */
    fun superIconFrame(spanX: Int, rowsTall: Int): PxRect {
        val span = rowHeight * (rowsTall - 1) + icon
        val height = minOf(cellWidth * (rowsTall - 1) + icon, span)
        val top = topInset + (span - height) / 2
        return PxRect(sideInset, top, sideInset + cellWidth * (spanX - 1) + icon, top + height)
    }

    /**
     * The left edge of dock icon [index] of [count], from the grid's left edge. The dock stands on
     * the page's own columns: with one app for each column every icon is under its column, and
     * with fewer the group is centred on the same spacing. More than fit are spread evenly.
     */
    fun dockIconLeft(index: Int, count: Int): Int {
        if (count > columns) {
            val slot = gridWidth.toFloat() / count
            return ((index + 0.5f) * slot - icon / 2f).roundToInt()
        }
        // Counted in half columns, so a group one short of the columns sits between them.
        val halfColumns = (columns - count) + 2 * index
        return sideInset + halfColumns * cellWidth / 2
    }

    /** The left and right edges of the dock's pane for [count] apps: one column for each icon. */
    fun dockPane(count: Int): Pair<Int, Int> {
        if (count <= 0 || count >= columns) return 0 to gridWidth
        val left = dockIconLeft(0, count) - sideInset
        return left to left + count * cellWidth
    }

    companion object {
        /** The height of the strip an 11sp label sits in, in dp. */
        const val LABEL_DP = 20f

        /**
         * How big an icon is, worked out from the cell it sits in rather than chosen: about three
         * quarters of the cell's width, never so tall that it pushes its label out, and kept
         * between sizes that read well.
         */
        fun iconSizeDp(cellWidthDp: Float, rowHeightDp: Float, showLabels: Boolean): Float {
            val labelSpace = if (showLabels) 24f else 4f
            return minOf(
                (cellWidthDp * 0.75f).coerceIn(54f, 72f),
                (rowHeightDp - labelSpace - 8f).coerceAtLeast(28f),
                (cellWidthDp - 8f).coerceAtLeast(28f),
            )
        }
    }
}
```

- [ ] **Step 4: Run the test and see it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gabriel.zenithui.data.HomeGeometryTest" --console=plain`
Expected: `BUILD SUCCESSFUL`, 13 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gabriel/zenithui/data/HomeGeometry.kt app/src/test/java/com/gabriel/zenithui/data/HomeGeometryTest.kt
git commit -m "HomeGeometry: one source of positions for the home page"
```

---

### Task 2: whole rows in the placement code

**Files:**
- Modify: `data/Models.kt` (widget smallest sizes, `Widgets.placeable`, `ROW_UNITS` comment)
- Modify: `data/LayoutBuilder.kt` (no tuck, whole-row steps, rounding helpers, `build` skips search)
- Modify: `data/DropPlanner.kt` (no tuck, whole rows for everything)
- Modify: `ZenithViewModel.kt` (call sites only)
- Modify: `ui/home/HomeScreen.kt` (call sites only)
- Test: `data/DropPlannerTest.kt` (update), `data/LayoutBuilderTest.kt` (new)

**Interfaces:**
- Produces: `LayoutBuilder.settle(items, columns, rows, free)` (no `tuck`), `LayoutBuilder.wholeRows(spanY: Int): Int`, `LayoutBuilder.nearestRow(row: Float): Int`, `PageGrid.occupyItem(item)` (no `tuck`), `DropPlanner.Page(columns, unitRows, free, colWidthInRowUnits)` (no `tuck`), `Widgets.placeable: List<WidgetSpec>`.
- Removed: `LayoutBuilder.blockedSpanY` (both), `LayoutBuilder.leavesBand`, `LayoutBuilder.ICON_ROW_STEP`, `ZenithViewModel.setWidgetTuck`.

- [ ] **Step 1: Update `DropPlannerTest.kt`**

Replace the `pixel` page:

```kotlin
    /** The Pixel 10a's home page: 4 by 5, a column about 2.9 row units wide. */
    private val pixel = DropPlanner.Page(columns = 4, unitRows = 20, free = false, colWidthInRowUnits = 2.9f)
```

In `assertSound`, replace the two `blockedSpanY` lines:

```kotlin
            val aBottom = a.row + a.spanY
            val bBottom = b.row + b.spanY
```

Replace the three tuck tests (`an icon let go a little high under a widget tucks in snug under it`, `an icon under a widget stays in line with the icon beside it rather than tucking in`, `an icon sits snug under an icon that is itself tucked under a widget`) with:

```kotlin
    @Test
    fun `an icon let go a little high under a widget lands on the row under it`() {
        // Its top edge is still a little inside the clock; the row just under the clock is meant.
        assertEquals(Plan(Spot(0, 8)), plan(listOf(clock, google), google, 0.1f, 6.4f))
        assertEquals(Plan(Spot(0, 8)), plan(home, google, 0.1f, 6.4f))
    }

    @Test
    fun `a widget never lands between rows`() {
        var y = 0f
        while (y <= 12f) {
            val plan = plan(home, clock, 2.0f, y)
            if (plan != null) assertEquals("let go at $y", 0, plan.target.row % ROW_UNITS)
            y += 0.25f
        }
    }
```

Rename `an icon beside a widget keeps to the rows of the page, not the widget's tuck row` to `an icon beside a widget keeps to the rows of the page` and change its comment to `// Let go a little above row 8 with nothing above it in column 3.` (the assertion stays `Plan(Spot(3, 8))`).

In `every plan on random pages leaves a sound page`, replace the candidate generation and slot lines:

```kotlin
                val candidate: HomeItem = when (random.nextInt(4)) {
                    0 -> widget("w$n", 0, 0, random.nextInt(1, 5), ROW_UNITS * random.nextInt(1, 4), if (random.nextBoolean()) "clock" else "notes")
                    1 -> app("s$n", 0, 0, spanX = random.nextInt(1, 3), spanY = ROW_UNITS * random.nextInt(1, 3))
                    else -> app("a$n", 0, 0)
                }
                val slot = grid.findSlot(candidate.spanX, candidate.spanY, ROW_UNITS)
                if (slot != null && slot.second + candidate.spanY <= pixel.unitRows) {
                    grid.occupy(slot.first, slot.second, candidate.spanX, candidate.spanY)
                    items += candidate.moved(0, slot.first, slot.second)
                }
```

- [ ] **Step 2: Write `data/LayoutBuilderTest.kt`**

```kotlin
package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutBuilderTest {

    private fun app(id: String, col: Int, row: Int, dy: Float = 0f) =
        AppCell(id, 0, col, row, "pkg.$id", "Main", dy = dy)

    private fun widget(id: String, col: Int, row: Int, spanX: Int, spanY: Int, type: String = "clock") =
        WidgetCell(id, 0, col, row, type, spanX, spanY)

    @Test
    fun `heights round to the nearest whole row and never to none`() {
        assertEquals(listOf(4, 4, 4, 4, 4, 8, 8, 8, 8, 12), (1..10).map { LayoutBuilder.wholeRows(it) })
    }

    @Test
    fun `a row between two goes to the nearer and halfway goes up the page`() {
        assertEquals(0, LayoutBuilder.nearestRow(1f))
        assertEquals(0, LayoutBuilder.nearestRow(2f))
        assertEquals(4, LayoutBuilder.nearestRow(2.5f))
        assertEquals(4, LayoutBuilder.nearestRow(3f))
        assertEquals(8, LayoutBuilder.nearestRow(7f))
        assertEquals(8, LayoutBuilder.nearestRow(10f))
    }

    @Test
    fun `an icon that was tucked under a widget settles on the row under it`() {
        val clock = widget("clock", 0, 0, 2, 8)
        val tucked = app("tucked", 0, 7)
        val settled = LayoutBuilder.settle(listOf(clock, tucked), columns = 4, rows = 5, free = false)
        assertEquals(8, settled.first { it.id == "tucked" }.row)
        assertEquals(0, settled.first { it.id == "tucked" }.col)
        assertEquals(clock, settled.first { it.id == "clock" })
    }

    @Test
    fun `a widget a row and a half tall grows to two rows and what it then covers moves down`() {
        val note = widget("note", 0, 0, 2, 6, type = "notes")
        val under = app("under", 0, 6)
        val settled = LayoutBuilder.settle(listOf(note, under), 4, 5, free = false)
        val grown = settled.first { it.id == "note" }
        assertEquals(8, grown.spanY)
        assertEquals(0, grown.row)
        // Straight down in its own column, not two columns across.
        assertEquals(0 to 8, settled.first { it.id == "under" }.let { it.col to it.row })
    }

    @Test
    fun `a layout already on whole rows is left alone`() {
        val items = listOf(widget("clock", 0, 0, 2, 8), app("a", 2, 0), app("b", 0, 8), app("c", 3, 16))
        assertEquals(items, LayoutBuilder.settle(items, 4, 5, free = false))
    }

    @Test
    fun `free placement keeps quarter rows and offsets`() {
        val items = listOf(app("a", 1, 5, dy = 0.3f), widget("w", 2, 1, 2, 3, type = "battery"))
        assertEquals(items, LayoutBuilder.settle(items, 4, 5, free = true))
    }

    @Test
    fun `nothing is lost when taller widgets no longer fit the page`() {
        // Three full-width widgets a row and a half tall filled the page; as two-row widgets only two fit.
        val items = listOf(
            widget("one", 0, 0, 4, 6, type = "notes"),
            widget("two", 0, 6, 4, 6, type = "notes"),
            widget("three", 0, 12, 4, 6, type = "notes"),
        )
        val settled = LayoutBuilder.settle(items, 4, 5, free = false)
        assertEquals(listOf("one", "two", "three"), settled.map { it.id })
        assertTrue(settled.all { it.spanY == 8 && it.row % ROW_UNITS == 0 })
        assertEquals(listOf(0 to 0, 0 to 8, 1 to 0), settled.map { it.page to it.row })
    }

    @Test
    fun `setup puts widgets and icons on whole rows and leaves the search bar out`() {
        val grid = PageGrid(4, 20)
        assertEquals(0 to 0, grid.place(2, 8, rowStep = ROW_UNITS))
        assertEquals(2 to 0, grid.place(1, ROW_UNITS, rowStep = ROW_UNITS))
        assertEquals(listOf(Widgets.Clock, Widgets.Battery, Widgets.Calendar, Widgets.Notes, Widgets.Toggles), Widgets.placeable)
        assertTrue(Widgets.placeable.all { it.minSpanY % ROW_UNITS == 0 })
    }
}
```

- [ ] **Step 3: Run both and see them fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gabriel.zenithui.data.*" --console=plain`
Expected: compilation fails (`No parameter with name 'tuck'` is gone from `Page`, `Unresolved reference: wholeRows`, `nearestRow`, `placeable`).

- [ ] **Step 4: `data/Models.kt`**

Replace the `ROW_UNITS` comment:

```kotlin
/**
 * A home row is divided into this many vertical units. Icons and widgets keep to whole rows, so
 * everything on a page lines up; the quarter steps are what Plus free placement puts things on.
 */
const val ROW_UNITS = 4
```

Change the smallest sizes: `Clock` `minSpanY = ROW_UNITS`, `Battery` `minSpanY = ROW_UNITS`, `Calendar` `minSpanY = 2 * ROW_UNITS`, `Notes` `minSpanY = ROW_UNITS`, `Toggles` `minSpanY = ROW_UNITS`. `Search` stays as it is, with this comment above it:

```kotlin
    // No longer placed on a page: the search bar is the bar above the dock, switched on in
    // settings. The type stays so that layouts saved with one can be read and converted.
```

After `val all = ...` add:

```kotlin
    /** The widgets that can be put on a page. */
    val placeable = listOf(Clock, Battery, Calendar, Notes, Toggles)
```

- [ ] **Step 5: `data/LayoutBuilder.kt`**

Replace the `occupyItem` extension and its comment:

```kotlin
/** Marks the cells [item] covers as taken. */
fun PageGrid.occupyItem(item: HomeItem) = occupy(item.col, item.row, item.spanX, item.spanY)
```

In `PageGrid.nearestSlot`, a column now counts as three row units, which is its real shape on a phone and stops a displaced icon preferring two columns across over one row down. Replace the comment and the `dx` line:

```kotlin
    /**
     * The opening closest to [col], [row], or null when nothing that size fits. A column counts as
     * three row units of distance — about the shape of a cell — so "closest" matches what you see.
     */
```
```kotlin
                val dx = (c - col) * 3
```

In `build`, replace the widget loop header and both `place` calls:

```kotlin
        // The widgets you asked for, sized for the grid you chose. The search bar is not one of
        // them: it is the bar above the dock, and setup switches that on instead.
        widgetTypes.filter { it != Widgets.Search.type }.mapNotNull { Widgets.byType(it) }.forEach { spec ->
            val (spanX, spanY) = Widgets.defaultSpan(spec, columns, rows)
            val slot = page.place(spanX, spanY, rowStep = ROW_UNITS) ?: return@forEach
            items += WidgetCell(newId(), 0, slot.first, slot.second, spec.type, spanX, spanY)
        }
```
```kotlin
            val slot = page.place(1, ROW_UNITS, rowStep = ROW_UNITS) ?: break
```

Delete `leavesBand`, both `blockedSpanY` functions and `ICON_ROW_STEP`, with their comments. In their place:

```kotlin
    /** [spanY] row units as whole rows: the nearest, and never fewer than one. */
    fun wholeRows(spanY: Int): Int = ((spanY + ROW_UNITS / 2) / ROW_UNITS).coerceAtLeast(1) * ROW_UNITS

    /** The whole row nearest to [row], in row units. Exactly halfway goes to the row above. */
    fun nearestRow(row: Float): Int {
        val whole = kotlin.math.floor(row / ROW_UNITS).toInt()
        val past = row - whole * ROW_UNITS
        return (if (past > ROW_UNITS / 2f) whole + 1 else whole) * ROW_UNITS
    }
```

Replace `settle` (comment and body up to `fit`):

```kotlin
    /**
     * Puts every item somewhere it may be on a [columns] by [rows] grid, moving as little as it
     * can. Items are taken from the top of each page down, so whatever sits higher keeps its place
     * and anything overlapping it moves to the closest opening — not the first gap on the page,
     * which is what used to shuffle icons around.
     *
     * With [free] false everything goes onto whole rows: an offset folds into the nearest cell, a
     * row between two becomes the nearer one, and widgets become whole rows tall. A layout that
     * already follows the rules comes back unchanged, so it is always safe to run again.
     */
    fun settle(items: List<HomeItem>, columns: Int, rows: Int, free: Boolean): List<HomeItem> {
        val unitRows = rows * ROW_UNITS
        val grids = HashMap<Int, PageGrid>()
        fun grid(page: Int) = grids.getOrPut(page) { PageGrid(columns, unitRows) }
        val step = if (free) 1 else ROW_UNITS

        val placed = HashMap<String, HomeItem>()
        val overflow = mutableListOf<HomeItem>()
        val topDown = items.map { fit(it, columns, rows, free) }
            .sortedWith(compareBy({ it.page }, { it.row }, { it.col }))
        for (item in topDown) {
            val g = grid(item.page)
            val slot = g.nearestSlot(item.col, item.row, item.spanX, item.spanY, step)
            if (slot == null) {
                overflow += item
                continue
            }
            g.occupy(slot.first, slot.second, item.spanX, item.spanY)
            placed[item.id] = if (slot.first == item.col && slot.second == item.row) {
                item
            } else {
                item.moved(item.page, slot.first, slot.second)
            }
        }

        // Anything with no room left on its own page goes to the next page that has some.
        var page = items.maxOfOrNull { it.page } ?: 0
        for (item in overflow) {
            var slot = grid(page).findSlot(item.spanX, item.spanY, step)
            while (slot == null && page < MAX_PAGES - 1) {
                page += 1
                slot = grid(page).findSlot(item.spanX, item.spanY, step)
            }
            if (slot == null) continue
            grid(page).occupy(slot.first, slot.second, item.spanX, item.spanY)
            placed[item.id] = item.moved(page, slot.first, slot.second)
        }

        // Kept in the original order, so nothing is drawn in a different stacking; and nothing is
        // ever dropped — an item with no room anywhere simply stays as it was.
        return items.map { placed[it.id] ?: it }
    }
```

In `fit`, replace the non-free widget branch:

```kotlin
            } else {
                val spec = Widgets.byType(item.type)
                val tall = wholeRows(item.spanY)
                val (spanX, spanY) = if (spec != null) {
                    Widgets.clampSpan(spec, item.spanX, tall, columns, rows)
                } else {
                    item.spanX.coerceIn(1, columns) to tall.coerceIn(ROW_UNITS, unitRows)
                }
                item.copy(spanX = spanX, spanY = spanY)
            }
```

and replace everything in `fit` after the `if (free) { ... }` block:

```kotlin
        // Where the item is actually drawn, on the nearest whole row.
        val col = (sized.col + sized.dx).roundToInt().coerceIn(0, maxCol)
        val row = nearestRow(sized.row + sized.dy).coerceIn(0, maxRow)
        return if (col == sized.col && row == sized.row && sized.dx == 0f && sized.dy == 0f) {
            sized
        } else {
            sized.moved(sized.page, col, row)
        }
```

`fit`'s `item.copy(spanX, spanY)` for an unchanged widget must stay equal to the original, which a data class copy is.

- [ ] **Step 6: `data/DropPlanner.kt`**

Replace `Page` and `pageFor`:

```kotlin
    /** The page something is being let go on. */
    data class Page(
        val columns: Int,
        /** Its height in row units, [ROW_UNITS] to a row. */
        val unitRows: Int,
        /** Plus free placement: any row unit will do. */
        val free: Boolean,
        /** How wide a column is in row units, so that "nearest" means nearest on the glass. */
        val colWidthInRowUnits: Float,
    )

    /**
     * The page [settings] describe, with [freePlacement] when Plus has it on. [colWidthInRowUnits]
     * is measured from the grid on screen; before there is one, a typical phone's will do.
     */
    fun pageFor(settings: ZenithSettings, freePlacement: Boolean, colWidthInRowUnits: Float): Page = Page(
        columns = settings.columns,
        unitRows = settings.rows * ROW_UNITS,
        free = freePlacement,
        colWidthInRowUnits = colWidthInRowUnits.takeIf { it.isFinite() && it > 0f } ?: 2.9f,
    )
```

In the class comment, add this sentence at the end of the first paragraph: "Everything keeps to whole rows unless free placement is on, so nothing can end up out of line with its neighbours."

Replace the body of `plan` up to the `makeRoom` call:

```kotlin
        if (dragged.spanX > page.columns || dragged.spanY > page.unitRows) return null
        val others = items.filter { it.id != dragged.id }.map { Box.of(it) }
        val height = dragged.spanY
        val col = exactCol.roundToInt().coerceIn(0, page.columns - dragged.spanX)
        val rows = (0..(page.unitRows - dragged.spanY)).filter { canTake(it, page) }
        fun fits(row: Int) = others.none { it.overlaps(col, row, dragged.spanX, height) }
        fun cost(row: Int) = abs(row - exactRow)

        val row = rows.minByOrNull(::cost) ?: return null
        val target = Spot(col, row)
        if (fits(row)) return Plan(target)

        // Let go only partly over something, less than half its height into it, with an opening in
        // the same column: that opening is what was meant.
        rows.filter { abs(it - exactRow) <= dragged.spanY / 2f && fits(it) }
            .minByOrNull(::cost)
            ?.let { return Plan(Spot(col, it), exact = false) }
```

In the public `opening`, use `Box.of(it)`. Replace the private `opening`:

```kotlin
    private fun opening(dragged: HomeItem, exactCol: Float, exactRow: Float, others: List<Box>, page: Page): Spot? {
        var best: Spot? = null
        var bestCost = Float.MAX_VALUE
        for (r in 0..(page.unitRows - dragged.spanY)) {
            if (!canTake(r, page)) continue
            for (c in 0..(page.columns - dragged.spanX)) {
                if (others.any { it.overlaps(c, r, dragged.spanX, dragged.spanY) }) continue
                val cost = hypot((c - exactCol) * page.colWidthInRowUnits, r - exactRow)
                if (cost < bestCost) {
                    bestCost = cost
                    best = Spot(c, r)
                }
            }
        }
        return best
    }
```

Replace the whole "rows" section (`isIcon`, `canTake`, `linesUp`, `misfit`, `outOfLine`) with:

```kotlin
    // ------------------------------------------------------------------ rows

    /** Whether anything may sit on [row]: a whole row, or with free placement any row unit. */
    private fun canTake(row: Int, page: Page): Boolean = page.free || row % ROW_UNITS == 0
```

In `makeRoom`, `val drop = Box(dragged.id, dragged, target.col, target.row, height)` stays. In `moveTogether`, replace the `step` line with `val step = if (page.free) 1 else ROW_UNITS`. In `moveApart`, replace the two lines that call `canTake` and `misfit`:

```kotlin
                    if (!canTake(r, page)) continue
                    val dx = (c - h.col) * page.colWidthInRowUnits
                    val dy = (r - h.row).toFloat()
                    val cost = hypot(dx, dy)
```

In `sound`, replace the last expression:

```kotlin
        return trial.zip(before).all { (now, was) ->
            (now.row - was.row) % ROW_UNITS == 0 || canTake(now.row, page)
        }
```

In `Box.companion`, replace `of`:

```kotlin
            fun of(item: HomeItem): Box = Box(item.id, item, item.col, item.row, item.spanY)
```

Delete `OUT_OF_LINE` and `WIDGET_PULL` with their comments, and the `[height] is how much of it blocks` half-sentence in `Box`'s comment (it now reads `/** An item's place while a way of making room is tried out. */`).

- [ ] **Step 7: Call sites in `ZenithViewModel.kt`**

- `init`: `LayoutBuilder.settle(layout.value, current.columns, current.rows, free = false)`.
- `updateSettings`: `LayoutBuilder.settle(layout.value, after.columns, after.rows, free = after.freeform)`.
- Add, in the "home editing" section:

```kotlin
    /** Whole rows, unless Plus free placement is in effect. */
    private fun rowStep(): Int = if (settings.value.freeform && plus.value.active) 1 else ROW_UNITS
```

- `addAppToFirstFreeSlot`: `firstFreeSlot(1, ROW_UNITS, rowStep())`.
- `addWidget`: `firstFreeSlot(spanX, spanY, rowStep())`.
- `resizeWidget`: in the non-free branch, `Widgets.clampSpan(spec, spanX, LayoutBuilder.wholeRows(spanY), s.columns, s.rows)`; change the function's first comment line to `/** Resizes a widget where it stands, in whole rows unless free placement is on, refusing sizes that would overlap or overflow. */`.
- Every `grid.occupyItem(it, ...widgetTuck)` becomes `grid.occupyItem(it)` (five places: `freeSlotOnPage`, `firstFreeSlotFrom`, `firstFreeSlot`, `isSlotFree`, `removeFromFolder`).
- `isSlotFree`: delete the `moving`/`blocked` lines and their comment; return `grid.isFree(col, row, spanX, spanY)`.
- `removeFromFolder`: `grid.findSlot(1, ROW_UNITS, rowStep())`.
- Delete `setWidgetTuck` and its comment.

- [ ] **Step 8: Call sites in `ui/home/HomeScreen.kt`**

- Delete `private val TUCK_GAP = 14.dp` and its comment.
- Delete the block that starts `// How far an icon may tuck in under a widget on this screen` through the end of its `LaunchedEffect(drag.lines, drag.cellHeight) { ... }`, but keep `val tuckDensity = LocalDensity.current`, renamed to `val density = LocalDensity.current` (rename its other uses in this file too).
- In `dropOnPage`, replace the `rowStep` line: `val rowStep = if (settings.freeform && plus.active) 1 else ROW_UNITS`.

- [ ] **Step 9: Run the data tests and see them pass**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`; `DropPlannerTest`, `LayoutBuilderTest` and `HomeGeometryTest` all pass.

- [ ] **Step 10: Commit**

```bash
git add -A app/src
git commit -m "Whole rows for everything on a page; nothing tucks any more"
```

---

### Task 3: settings, the one-time tidy, and the dock limit

**Files:**
- Modify: `data/Settings.kt`
- Create: `data/LayoutTidy.kt`, `data/HomeHeader.kt`
- Modify: `ZenithViewModel.kt`
- Modify: `ui/home/HomeScreen.kt` (toast when the dock is full)
- Test: `data/LayoutTidyTest.kt`, `data/HomeHeaderTest.kt`

**Interfaces:**
- Consumes: `LayoutBuilder.settle(items, columns, rows, free)`, `Widgets.Search`.
- Produces: `ZenithSettings.searchBar: Boolean`, `ZenithSettings.header: String`; `LayoutTidy.VERSION = 2`, `LayoutTidy.upgrade(settings, items): Pair<ZenithSettings, List<HomeItem>>`; `HomeHeader.NAME/GREETING/OFF`, `HomeHeader.text(style: String, name: String, hour: Int): String?`; `ZenithViewModel.toggleDock(key): Boolean`.

- [ ] **Step 1: Write the failing tests**

`data/LayoutTidyTest.kt`:

```kotlin
package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTidyTest {

    /** A phone that was last run before whole rows: version 1, icons tucking one unit. */
    private val old = ZenithSettings(columns = 4, rows = 5, layoutVersion = 1, widgetTuck = 1)

    private fun app(id: String, col: Int, row: Int) = AppCell(id, 0, col, row, "pkg.$id", "Main")

    private fun widget(id: String, col: Int, row: Int, spanX: Int, spanY: Int, type: String = "clock") =
        WidgetCell(id, 0, col, row, type, spanX, spanY)

    @Test
    fun `a search bar on the page becomes the bar above the dock`() {
        val bar = widget("bar", 0, 0, 4, 2, type = "search")
        val (settings, items) = LayoutTidy.upgrade(old, listOf(bar, app("a", 0, 2), app("b", 1, 2)))
        assertTrue(settings.searchBar)
        assertEquals(LayoutTidy.VERSION, settings.layoutVersion)
        assertEquals(0, settings.widgetTuck)
        // The icons that sat under it close the gap it left.
        assertEquals(listOf("a" to 0, "b" to 0), items.map { it.id to it.row })
    }

    @Test
    fun `a tucked icon comes down to the row under its widget`() {
        val clock = widget("clock", 0, 0, 2, 8)
        val beside = app("beside", 2, 8)
        val (settings, items) = LayoutTidy.upgrade(old, listOf(clock, app("tucked", 0, 7), beside))
        assertEquals(false, settings.searchBar)
        assertEquals(8, items.first { it.id == "tucked" }.row)
        assertEquals(beside, items.first { it.id == "beside" })
        assertEquals(clock, items.first { it.id == "clock" })
    }

    @Test
    fun `a widget on a quarter step moves to the nearest whole row`() {
        val (_, items) = LayoutTidy.upgrade(old, listOf(widget("clock", 0, 5, 2, 8)))
        assertEquals(4, items.single().row)
    }

    @Test
    fun `free placement keeps its layout and still loses the search widget`() {
        val bar = widget("bar", 0, 3, 4, 2, type = "search")
        val icon = AppCell("a", 0, 1, 5, "pkg.a", "Main", dx = 0.2f, dy = -0.3f)
        val small = widget("w", 2, 9, 2, 3, type = "battery")
        val (settings, items) = LayoutTidy.upgrade(old.copy(freeform = true), listOf(bar, icon, small))
        assertTrue(settings.searchBar)
        assertEquals(listOf<HomeItem>(icon, small), items)
    }

    @Test
    fun `a layout already brought up to date comes back as it is`() {
        val now = old.copy(layoutVersion = LayoutTidy.VERSION, widgetTuck = 0)
        val items = listOf<HomeItem>(widget("clock", 0, 5, 2, 6))
        assertEquals(now to items, LayoutTidy.upgrade(now, items))
    }

    @Test
    fun `nothing is lost when the page has no room for the taller widgets`() {
        val items = listOf(
            widget("one", 0, 0, 4, 6, type = "notes"),
            widget("two", 0, 6, 4, 6, type = "notes"),
            widget("three", 0, 12, 4, 6, type = "notes"),
        )
        val (_, tidied) = LayoutTidy.upgrade(old, items)
        assertEquals(listOf("one", "two", "three"), tidied.map { it.id })
        assertEquals(listOf(0, 0, 1), tidied.map { it.page })
    }
}
```

`data/HomeHeaderTest.kt`:

```kotlin
package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeHeaderTest {

    @Test
    fun `the name is shown as it is, and Zenith when there is none`() {
        assertEquals("Gabriel", HomeHeader.text(HomeHeader.NAME, " Gabriel ", hour = 9))
        assertEquals("Zenith", HomeHeader.text(HomeHeader.NAME, "", hour = 9))
    }

    @Test
    fun `the greeting follows the time of day`() {
        assertEquals("Good morning, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 5))
        assertEquals("Good afternoon, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 12))
        assertEquals("Good evening, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 22))
        assertEquals("Good night, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 23))
        assertEquals("Good night, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 4))
    }

    @Test
    fun `a greeting with no name is just the greeting`() {
        assertEquals("Good morning", HomeHeader.text(HomeHeader.GREETING, "  ", hour = 8))
    }

    @Test
    fun `off shows nothing and an unknown style shows the name`() {
        assertNull(HomeHeader.text(HomeHeader.OFF, "Gabriel", hour = 8))
        assertEquals("Gabriel", HomeHeader.text("something else", "Gabriel", hour = 8))
    }
}
```

- [ ] **Step 2: Run them and see them fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gabriel.zenithui.data.LayoutTidyTest" --tests "com.gabriel.zenithui.data.HomeHeaderTest" --console=plain`
Expected: compilation fails with `Unresolved reference: LayoutTidy` and `HomeHeader`.

- [ ] **Step 3: `data/Settings.kt`**

After `showDock`, add:

```kotlin
    /** A search bar above the dock, between the page dots and the dock. */
    val searchBar: Boolean = false,
    /** What is written at the top of the home screen: name | greeting | off. */
    val header: String = "name",
```

In `widgetTuck`'s comment, replace the whole comment with:

```kotlin
    /** No longer used: nothing tucks under a widget. Kept so that older backups still read. */
```

Change `layoutVersion`'s comment to `/** 0 = rows only, 1 = rows divided into units, 2 = everything on whole rows. */`.

In `toJson`, after `put("showDock", showDock)`:

```kotlin
        put("searchBar", searchBar)
        put("header", header)
```

In `fromJson`, after `showDock = ...`:

```kotlin
                searchBar = o.optBoolean("searchBar", d.searchBar),
                header = o.optString("header", d.header),
```

- [ ] **Step 4: `data/LayoutTidy.kt`**

```kotlin
package com.gabriel.zenithui.data

/**
 * Brings a home screen saved before everything kept to whole rows up to that rule. It runs once
 * when Zenith starts on an older layout, and whenever an older backup is put back.
 */
object LayoutTidy {

    /** From this layout version on, every item sits on a whole row and the search bar is a setting. */
    const val VERSION = 2

    /**
     * [settings] and [items] as they are under today's rules. The search bar leaves the page and
     * becomes the bar above the dock, and nothing tucks under a widget any more. Unless free
     * placement is on, every item also moves to the nearest whole row and every widget becomes
     * whole rows tall, with whatever then overlaps settled the usual way: the higher item keeps
     * its place. A layout already at [VERSION] comes back untouched.
     */
    fun upgrade(settings: ZenithSettings, items: List<HomeItem>): Pair<ZenithSettings, List<HomeItem>> {
        if (settings.layoutVersion >= VERSION) return settings to items
        val rest = items.filterNot { it is WidgetCell && it.type == Widgets.Search.type }
        val next = settings.copy(
            searchBar = settings.searchBar || rest.size != items.size,
            widgetTuck = 0,
            layoutVersion = VERSION,
        )
        val placed = if (settings.freeform) {
            rest
        } else {
            LayoutBuilder.settle(rest, settings.columns, settings.rows, free = false)
        }
        return next to placed
    }
}
```

- [ ] **Step 5: `data/HomeHeader.kt`**

```kotlin
package com.gabriel.zenithui.data

/** The line written at the top of the home screen. */
object HomeHeader {
    const val NAME = "name"
    const val GREETING = "greeting"
    const val OFF = "off"

    /** The line for [style], or null when there is none. [hour] is the hour of the day, 0 to 23. */
    fun text(style: String, name: String, hour: Int): String? {
        val who = name.trim()
        return when (style) {
            OFF -> null
            GREETING -> {
                val hello = when (hour) {
                    in 5..11 -> "Good morning"
                    in 12..17 -> "Good afternoon"
                    in 18..22 -> "Good evening"
                    else -> "Good night"
                }
                if (who.isEmpty()) hello else "$hello, $who"
            }
            else -> who.ifEmpty { "Zenith" }
        }
    }
}
```

- [ ] **Step 6: `ZenithViewModel.kt`**

Replace the first three blocks of `init` (the version check, `undoDoubleUpgrade`, and the `if (!settings.value.freeform) { ... settle ... }` block) with one call, `upgradeLayout()`, and add the function below `init`:

```kotlin
    /**
     * Brings whatever layout is stored up to today's rules: rescaled into row units, onto whole
     * rows, and settled so nothing overlaps. Safe to run on a layout that already follows them,
     * which is why a restored backup goes through it too.
     */
    private fun upgradeLayout() {
        // Layouts saved before the grid gained sub-rows are rescaled once.
        if (settings.value.layoutVersion < 1) {
            if (layout.value.isNotEmpty()) store.setLayout(LayoutBuilder.upgradeToRowUnits(layout.value))
            store.update { it.copy(layoutVersion = 1) }
        }
        // Undo that fourfold rescale on a home screen that already went through it.
        LayoutBuilder.undoDoubleUpgrade(layout.value, settings.value.rows).let { fixed ->
            if (fixed != layout.value) store.setLayout(fixed)
        }
        // Whole rows for everything, and the search bar as a setting rather than a widget.
        if (settings.value.layoutVersion < LayoutTidy.VERSION) {
            val (next, items) = LayoutTidy.upgrade(settings.value, layout.value)
            store.restore(next, items)
        }
        // A layout left in a state normal placement does not allow — offsets from free placement,
        // widgets grown over their neighbours — is put right once, as soon as it is loaded.
        if (!settings.value.freeform) {
            val current = settings.value
            val settled = LayoutBuilder.settle(layout.value, current.columns, current.rows, free = false)
            if (settled != layout.value) store.setLayout(settled)
        }
    }
```

In `restoreBackup`, after `store.restore(...)`, add `upgradeLayout()`.

In `buildHome`: `val dock = repository.suggestedDock(minOf(4, columns))`; in the `store.update` block set `layoutVersion = LayoutTidy.VERSION` and add `searchBar = Widgets.Search.type in widgets,`. Change the comment above `layoutVersion` to end "...Without this a rerun of setup left the version behind, and the next launch upgraded the new layout as if it were an old one."

Replace the dock functions `toggleDock`, `setDock` and the limit in `moveToDock`:

```kotlin
    /** How many apps the dock takes: one for each column of the page, so each can stand under one. */
    private fun dockLimit(): Int = settings.value.columns

    /** Adds [key] to the dock or takes it off. False when it was to be added and the dock is full. */
    fun toggleDock(key: String): Boolean {
        val dock = settings.value.dock
        if (key !in dock && dock.size >= dockLimit()) return false
        store.update { it.copy(dock = if (key in it.dock) it.dock - key else it.dock + key) }
        return true
    }

    fun setDock(keys: List<String>) = store.update { it.copy(dock = keys.take(dockLimit())) }
```

and in `moveToDock`: `if (dock.size >= dockLimit()) return false`.

- [ ] **Step 7: `ui/home/HomeScreen.kt`**

Replace `onToggleDock = { viewModel.toggleDock(current.key) },` with:

```kotlin
                onToggleDock = {
                    if (!viewModel.toggleDock(current.key)) {
                        Toast.makeText(context, "The dock is full — drag one out first", Toast.LENGTH_SHORT).show()
                    }
                },
```

- [ ] **Step 8: Run all unit tests and see them pass**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`; all five test classes pass.

- [ ] **Step 9: Commit**

```bash
git add -A app/src
git commit -m "One-time tidy onto whole rows; search bar and header as settings; dock holds one app per column"
```

---

### Task 4: the page draws from `HomeGeometry`

**Files:**
- Modify: `ui/home/HomeGrid.kt` (`HomeDragState`, `HomePage`, `HomeItemContent`, `SuperAppTile`, `AppTile`, `FolderTile`, `ResizeFrame`, `DragGhost`; delete `homeIconSize`, `SuperIconFrame`, `superIconFrame`, `IconLines`, `iconLines`, `toIconLines`)
- Modify: `ui/home/HomeScreen.kt` (one use of `drag.lines`)
- Modify: `ui/onboarding/PhonePreview.kt` (page geometry)

**Interfaces:**
- Consumes: `HomeGeometry`, `PxRect` (Task 1).
- Produces: `HomeDragState.geometry: HomeGeometry?`; `Modifier.pixelFrame(frame: PxRect)`; `HomeItemContent(item, settings, appsByKey, geometry: HomeGeometry, onSearch, onNoteChange, onAmbient)`; `AppTile(..., frame: PxRect? = null)`, `FolderTile(..., frame: PxRect? = null)`.

There is no JVM test for this task: it is Compose layout, pinned by `HomeGeometryTest` for the numbers and checked on the emulator in Task 8. Each step ends with the build.

- [ ] **Step 1: `HomeDragState` and the `pixelFrame` modifier**

In `HomeDragState`, replace the `lines` property and its comment:

```kotlin
    /** Where everything sits on the live page, so the ghost and the dock are cut to the same lines. */
    var geometry by mutableStateOf<HomeGeometry?>(null)
```

Add imports `androidx.compose.ui.layout.layout`, `androidx.compose.ui.unit.Constraints`, `androidx.compose.ui.unit.LayoutDirection`, `com.gabriel.zenithui.data.HomeGeometry`, `com.gabriel.zenithui.data.PxRect`. Above `AppTile` add:

```kotlin
/**
 * Draws content at exactly [frame], in whole pixels, inside the space this is given. The space
 * itself is taken whole, so the parent and anything beside it see no difference.
 */
fun Modifier.pixelFrame(frame: PxRect): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints.fixed(frame.width.coerceAtLeast(0), frame.height.coerceAtLeast(0)))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(frame.left, frame.top) }
}
```

- [ ] **Step 2: `HomePage`**

Replace from `BoxWithConstraints(` through the line `if (active) drag.lines = lines`:

```kotlin
    BoxWithConstraints(modifier.fillMaxSize()) {
        val geometry = remember(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels) {
            HomeGeometry(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels)
        }
        val cellWidthPx = geometry.cellWidth.toFloat()
        // One cell is a row unit tall; an icon spans ROW_UNITS of them.
        val cellHeightPx = geometry.unitHeight
        val iconSize = geometry.iconDp.dp
        if (active) {
            drag.cellWidth = cellWidthPx
            drag.cellHeight = cellHeightPx
            drag.iconSize = iconSize
            drag.iconSizePx = geometry.icon.toFloat()
            drag.geometry = geometry
        }
        val cellWidthDp = with(density) { cellWidthPx.toDp() }
        val cellHeightDp = with(density) { cellHeightPx.toDp() }

        // The grid itself: every cell the same whole number of pixels, with any left over shared
        // between the two edges. Everything below is placed from this box's corner.
        Box(
            Modifier
                .padding(start = with(density) { geometry.originX.toDp() }, top = with(density) { geometry.originY.toDp() })
                .size(with(density) { geometry.gridWidth.toDp() }, with(density) { geometry.gridHeight.toDp() })
                .onGloballyPositioned { coords ->
                    // Only the page in view owns the geometry drops are resolved against.
                    if (active) drag.gridOrigin = coords.positionInRoot()
                },
        ) {
```

and close that `Box` with one more `}` just before the closing brace of `BoxWithConstraints`. Re-indenting the body is optional; do not change anything else in it except the points below.

In the drop hint, replace the three shapes inside `Box(Modifier.offset { hintOffset }.requiredSize(...), contentAlignment = Alignment.Center) { ... }`:

```kotlin
                    if (dragged is AppCell && !dragged.isPlainIcon) {
                        // A super icon's hint is the icon itself, not the cells around it.
                        val frame = geometry.superIconFrame(dragged.spanX, dragged.spanY / ROW_UNITS)
                        val round = with(density) { minOf(frame.width, frame.height).toDp() } * 0.26f
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pixelFrame(frame)
                                .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                .clip(RoundedCornerShape(round))
                                .background(palette.onGlass.copy(alpha = 0.12f))
                        )
                    } else if (dragged is WidgetCell) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pixelFrame(geometry.widgetFrame(dragged.spanX, dragged.spanY))
                                .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                .clip(ZenithShapes.tile)
                                .background(palette.onGlass.copy(alpha = 0.12f))
                        )
                    } else {
                        // Round the icon itself, a little bigger, so the hint sits where the icon will.
                        val pad = with(density) { 7.dp.roundToPx() }
                        val icon = geometry.iconFrame
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pixelFrame(PxRect(icon.left - pad, icon.top - pad, icon.right + pad, icon.bottom + pad))
                                .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                .clip(ZenithShapes.tile)
                                .background(palette.onGlass.copy(alpha = 0.12f))
                        )
                    }
```

In each item's inner `Box`, drop the slim search bar's special case. Replace

```kotlin
                // A slim widget only listens inside its own bar, so the empty part of the cell
                // still belongs to the home screen.
                val slim = item is WidgetCell && item.type == "search"
                Box(
                    modifier = (if (slim) Modifier.fillMaxWidth().height(SEARCH_BAR_HEIGHT) else Modifier.fillMaxSize())
```

with

```kotlin
                Box(
                    modifier = Modifier
                        .fillMaxSize()
```

Replace the `HomeItemContent(...)` call in the page:

```kotlin
                    HomeItemContent(
                        item = item,
                        settings = settings,
                        appsByKey = appsByKey,
                        geometry = geometry,
                        onSearch = onSearch,
                        onNoteChange = onNoteChange,
                        onAmbient = onAmbient,
                    )
```

Replace the `ResizeFrame(...)` call:

```kotlin
                    ResizeFrame(
                        item = item,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        geometry = geometry,
                        free = freePlacement,
                        columns = settings.columns,
                        unitRows = settings.rows * ROW_UNITS,
                        onResize = onResize,
                        onDone = onResizeDone,
                    )
```

Change the comment above `if (resizing && ...)` to: `// As a sibling on top, so a finger on the pointer is never also a press on the item.`

- [ ] **Step 3: `HomeItemContent` and the tiles**

Replace `HomeItemContent`:

```kotlin
@Composable
fun HomeItemContent(
    item: HomeItem,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
    /** The page it is on: icons, super icons and widgets are all cut to its lines. */
    geometry: HomeGeometry,
    onSearch: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAmbient: () -> Unit = {},
) {
    val iconSize = geometry.iconDp.dp
    when (item) {
        is AppCell -> {
            val app = appsByKey[item.key]
            val label = app?.label ?: item.packageName.substringAfterLast('.')
            when {
                !item.isPlainIcon ->
                    SuperAppTile(
                        key = item.key,
                        label = label,
                        frame = geometry.superIconFrame(item.spanX, (item.spanY / ROW_UNITS).coerceAtLeast(1)),
                        showLabel = settings.showLabels,
                    )
                else -> AppTile(
                    key = item.key,
                    label = label,
                    iconSize = iconSize,
                    showLabel = settings.showLabels,
                    shape = ZenithShapes.forIconShape(settings.iconShape),
                    frame = geometry.iconFrame,
                )
            }
        }

        is FolderCell -> FolderTile(
            folder = item,
            appsByKey = appsByKey,
            iconSize = iconSize,
            showLabel = settings.showLabels,
            frame = geometry.iconFrame,
        )

        is WidgetCell -> ZenithWidget(
            type = item.type,
            userName = settings.userName,
            note = settings.note,
            toggles = settings.toggles,
            onNoteChange = onNoteChange,
            onSearch = onSearch,
            onAmbient = onAmbient,
            modifier = Modifier
                .fillMaxSize()
                .pixelFrame(geometry.widgetFrame(item.spanX, item.spanY)),
        )
    }
}
```

Replace `SuperAppTile`:

```kotlin
/**
 * A super icon, as on HyperOS: the app's icon stretched over several columns and rows, its
 * background filling the whole shape and its logo in the middle, with the name underneath like any
 * other icon. [frame] is where the picture goes inside its cells (see [HomeGeometry.superIconFrame]),
 * which puts it on the same lines as every icon around it.
 */
@Composable
private fun SuperAppTile(key: String, label: String, frame: PxRect, showLabel: Boolean) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(with(density) { frame.top.toDp() }))
        WideAppIcon(
            key = key,
            label = label,
            modifier = Modifier
                .padding(start = with(density) { frame.left.toDp() })
                .size(with(density) { frame.width.toDp() }, with(density) { frame.height.toDp() })
                .launchOrigin(key)
                .pressBounce(0.9f),
        )
        if (showLabel) {
            Box(Modifier.fillMaxWidth().height(LABEL_LINE_HEIGHT), contentAlignment = Alignment.BottomCenter) {
                Text(
                    text = label,
                    color = palette.onGlass,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
```

Delete `homeIconSize`, `SuperIconFrame`, `superIconFrame`, `IconLines`, `iconLines` and `toIconLines` with their comments. Keep `STANDARD_ICON_SIZE`.

Replace `AppTile`:

```kotlin
@Composable
fun AppTile(
    key: String,
    label: String,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    shape: androidx.compose.ui.graphics.Shape = ZenithShapes.icon,
    modifier: Modifier = Modifier,
    /** On a home page: exactly where the icon goes inside its cell. Elsewhere it is centred. */
    frame: PxRect? = null,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    Column(
        modifier = if (frame == null) modifier.padding(horizontal = 2.dp) else modifier.fillMaxSize(),
        horizontalAlignment = if (frame == null) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (frame == null) Arrangement.Center else Arrangement.Top,
    ) {
        if (frame != null) Spacer(Modifier.height(with(density) { frame.top.toDp() }))
        AppIcon(
            key = key,
            label = label,
            size = iconSize,
            shape = shape,
            modifier = Modifier
                .then(if (frame != null) Modifier.padding(start = with(density) { frame.left.toDp() }) else Modifier)
                .launchOrigin(key)
                .pressBounce(),
        )
        if (showLabel) {
            // Always exactly LABEL_LINE_HEIGHT tall, so every icon's top edge sits on one line and
            // widgets beside it can be cut to the bottom of this strip.
            Box(
                (if (frame != null) Modifier.fillMaxWidth().padding(horizontal = 2.dp) else Modifier).height(LABEL_LINE_HEIGHT),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = label,
                    color = palette.onGlass,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.graphicsLayer { shadowElevation = 0f },
                )
            }
        }
    }
}
```

In `FolderTile`, add the same `frame: PxRect? = null` parameter (with the same comment) after `modifier`, and apply it the same way: the `Column` becomes

```kotlin
    val density = LocalDensity.current
    Column(
        modifier = if (frame == null) modifier else modifier.fillMaxSize(),
        horizontalAlignment = if (frame == null) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (frame == null) Arrangement.Center else Arrangement.Top,
    ) {
        if (frame != null) Spacer(Modifier.height(with(density) { frame.top.toDp() }))
        Box(
            Modifier
                .then(if (frame != null) Modifier.padding(start = with(density) { frame.left.toDp() }) else Modifier)
                .size(iconSize)
                .pressBounce()
                .glass(ZenithShapes.icon, strength = 1.2f, material = GlassMaterial.Clear, interactive = true)
                .padding(iconSize * 0.12f),
            contentAlignment = Alignment.Center,
        ) {
```

and its label `Box` becomes `Box((if (frame != null) Modifier.fillMaxWidth() else Modifier).height(LABEL_LINE_HEIGHT), contentAlignment = Alignment.BottomCenter) {`. The folder's inner grid of small icons is unchanged.

Change the last constant in the file so the Compose label strip and the geometry's cannot drift apart:

```kotlin
/** Height of a single 11sp app label line, plus its spacer — the same strip [HomeGeometry] counts. */
private val LABEL_LINE_HEIGHT = HomeGeometry.LABEL_DP.dp
```

- [ ] **Step 4: `ResizeFrame`**

Change its parameters: replace `lines: IconLines,` and `iconSize: Dp,` with

```kotlin
    geometry: HomeGeometry,
    /** Plus free placement: a widget may be any number of row units tall. */
    free: Boolean,
```

Replace from `val side = ...` down to and including `fun restingBottom(...)`:

```kotlin
    val side = geometry.sideInset.toFloat()
    val sideEnd = geometry.sideInsetEnd.toFloat()
    val topInset = geometry.topInset.toFloat()
    val underInset = geometry.underInset.toFloat()
    val pageRows = unitRows / ROW_UNITS

    // Where an app's picture starts and ends for each number of rows it could span, from the same
    // frame the icon is drawn in (see HomeGeometry.superIconFrame); index 0 is unused.
    val appTop = FloatArray(pageRows + 1)
    val appBottom = FloatArray(pageRows + 1)
    for (rows in 1..pageRows) {
        val frame = geometry.superIconFrame(1, rows)
        appTop[rows] = frame.top.toFloat()
        appBottom[rows] = frame.bottom.toFloat()
    }
    fun rowsOf(spanY: Int) = (spanY / ROW_UNITS).coerceIn(1, pageRows.coerceAtLeast(1))

    // The outline runs along the lines the item is drawn on, in the item's own pixels. Its right
    // and bottom edges are the ones you pull.
    fun top(spanY: Int) = when {
        isApp -> appTop[rowsOf(spanY)]
        spanY >= ROW_UNITS -> topInset
        else -> 0f
    }
    fun restingRight(spanX: Int) = spanX * cellWidthPx - sideEnd
    fun restingBottom(spanY: Int) = when {
        isApp -> appBottom[rowsOf(spanY)]
        spanY >= ROW_UNITS -> spanY * cellHeightPx - underInset
        else -> spanY * cellHeightPx
    }
```

(`val density = LocalDensity.current` stays; it is still used further down.)

Replace `sizeFor`:

```kotlin
    /** The size the outline is asking for, in cells across and row units down. */
    fun sizeFor(r: Float, b: Float): Pair<Int, Int> {
        val spanX = ((r + sideEnd) / cellWidthPx).roundToInt().coerceIn(1, columns - live.col)
        val room = ((unitRows - live.row) / ROW_UNITS).coerceIn(1, pageRows.coerceAtLeast(1))
        return when {
            // Whole rows: whichever height's picture ends nearest the finger.
            isApp -> spanX to (((1..room).minByOrNull { abs(appBottom[it] - b) } ?: 1) * ROW_UNITS)
            free -> spanX to ((b + underInset) / cellHeightPx).roundToInt().coerceIn(1, unitRows - live.row)
            else -> spanX to (((1..room).minByOrNull { abs(restingBottom(it * ROW_UNITS) - b) } ?: 1) * ROW_UNITS)
        }
    }
```

Nothing else in `ResizeFrame` changes: `side` is still the outline's left edge.

- [ ] **Step 5: `DragGhost`**

After `val item = drag.item ?: return` add `val geometry = drag.geometry ?: return`, and replace its `HomeItemContent(...)` call:

```kotlin
        HomeItemContent(
            item = item,
            settings = settings,
            appsByKey = appsByKey,
            geometry = geometry,
            onSearch = {},
            onNoteChange = {},
        )
```

- [ ] **Step 6: `ui/home/HomeScreen.kt`**

In the dock's `onDragOut`, replace the `iconMiddle` line:

```kotlin
                            val iconMiddle = (drag.geometry?.topInset ?: 0) + drag.iconSizePx / 2f
```

- [ ] **Step 7: `ui/onboarding/PhonePreview.kt`**

In `MiniHome`, replace `var pageIcon by remember { mutableStateOf(STANDARD_ICON_SIZE) }` and its comment:

```kotlin
    // The page settles where everything sits; the name, the search bar and the dock read the same.
    var page by remember { mutableStateOf<HomeGeometry?>(null) }
    val density = LocalDensity.current
```

Replace the page's first six lines inside `BoxWithConstraints(...) {` (from `val cellWidth = ...` through `SideEffect { pageIcon = iconSize }`):

```kotlin
                val geometry = remember(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels) {
                    HomeGeometry(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels)
                }
                val cellWidth = with(density) { geometry.cellWidth.toDp() }
                val cellHeight = with(density) { geometry.unitHeight.toDp() }
                val originX = with(density) { geometry.originX.toDp() }
                val originY = with(density) { geometry.originY.toDp() }
                SideEffect { page = geometry }
```

Replace the two `animateDpAsState` targets with `originX + cellWidth * item.col` and `originY + cellHeight * item.row`, and the `HomeItemContent(...)` call:

```kotlin
                            HomeItemContent(
                                item = item,
                                settings = settings,
                                appsByKey = appsByKey,
                                geometry = geometry,
                                onSearch = {},
                                onNoteChange = {},
                            )
```

In the dock below, replace `size = pageIcon,` with `size = page?.iconDp?.dp ?: STANDARD_ICON_SIZE,`. Replace the imports of `homeIconSize` and `iconLines` with `com.gabriel.zenithui.data.HomeGeometry`, and add `androidx.compose.ui.platform.LocalDensity` if it is not imported.

- [ ] **Step 8: Build and run the unit tests**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`. Fix any unused-import warnings the removed code leaves behind (`SEARCH_BAR_HEIGHT` is no longer used in `HomeGrid.kt`).

- [ ] **Step 9: Commit**

```bash
git add -A app/src
git commit -m "The page, the hint, the resize outline and the ghost draw from HomeGeometry"
```

---

### Task 5: the dock stands on the grid's columns

**Files:**
- Modify: `ui/home/HomeGrid.kt` (`DockBar`, new `SlotArrangement`)
- Modify: `ui/home/HomeScreen.kt` (the `DockBar` call)
- Modify: `ui/onboarding/PhonePreview.kt` (its dock)

**Interfaces:**
- Consumes: `HomeGeometry.dockIconLeft`, `HomeGeometry.dockPane`, `HomeDragState.geometry`.
- Produces: `DockBar(..., geometry: HomeGeometry?, ...)` in place of `columns` and `pageIconSize`; `internal class SlotArrangement(lefts: List<Int>)`.

- [ ] **Step 1: `SlotArrangement`**

Above `DockBar`, replace `DOCK_ICON_GAP` and its comment with:

```kotlin
/** Puts a row's children at fixed places from its left edge, mirrored when the layout is. */
internal class SlotArrangement(private val lefts: List<Int>) : Arrangement.Horizontal {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, layoutDirection: LayoutDirection, outPositions: IntArray) {
        for (i in sizes.indices) {
            val left = lefts.getOrElse(i) { 0 }
            outPositions[i] = if (layoutDirection == LayoutDirection.Ltr) left else totalSize - left - sizes[i]
        }
    }
}
```

- [ ] **Step 2: `DockBar`**

Replace the two parameters `columns: Int = 0,` and `pageIconSize: Dp? = null,` (with their comments) with:

```kotlin
    /** The page's geometry: the dock stands on the same columns and its icons are the same size. */
    geometry: HomeGeometry? = null,
```

Replace from `val iconSize = pageIconSize ?: STANDARD_ICON_SIZE` through the `widest` declaration:

```kotlin
    val iconSize = geometry?.iconDp?.dp ?: STANDARD_ICON_SIZE
    val iconSizePx = with(density) { iconSize.toPx() }
    // Where the pane and each icon stand, on the page's own columns (see HomeGeometry.dockIconLeft).
    // Before a page has been measured there is no geometry, and the icons are simply spread out.
    val pane = geometry?.dockPane(keys.size)
    val arrangement = remember(geometry, keys.size) {
        if (geometry == null || keys.isEmpty()) {
            null
        } else {
            val paneLeft = geometry.dockPane(keys.size).first
            SlotArrangement(List(keys.size) { geometry.dockIconLeft(it, keys.size) - paneLeft })
        }
    }
```

Replace the outer `Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {` and the `Row(` modifier's first two lines (`.widthIn(max = widest)` and `.fillMaxWidth()`):

```kotlin
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .then(
                    if (geometry != null && pane != null) {
                        Modifier
                            .padding(start = with(density) { (geometry.originX + pane.first).toDp() })
                            .width(with(density) { (pane.second - pane.first).toDp() })
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
```

Replace the Row's last modifier line and its arrangement:

```kotlin
                .padding(vertical = 12.dp),
            horizontalArrangement = when {
                keys.isEmpty() -> Arrangement.Center
                arrangement != null -> arrangement
                else -> Arrangement.SpaceEvenly
            },
```

In each child `Box`, delete the line `.then(if (aligned) Modifier.weight(1f) else Modifier)`. Delete the now unused `aligned` value and its comment. Update the function's comment by adding a last sentence: "Its icons stand on the page's own columns, so each is under the icons above it."

- [ ] **Step 3: The call in `HomeScreen.kt`**

Replace `columns = settings.columns,` and `pageIconSize = drag.iconSize,` with `geometry = drag.geometry,`. Name the page margin once, near the other constants at the top of the file:

```kotlin
/** The space between the edge of the screen and the page, the dock and everything else on it. */
private val PAGE_MARGIN = 12.dp
```

and use it in the page's `.padding(horizontal = PAGE_MARGIN, vertical = 4.dp)` and the dock's `.padding(horizontal = PAGE_MARGIN)`.

- [ ] **Step 4: The preview's dock in `PhonePreview.kt`**

Replace the dock `Row`'s modifier and arrangement, and drop the weights:

```kotlin
            if (settings.showDock && dock.isNotEmpty()) {
                // On the page's own columns, exactly as the real dock stands.
                val g = page
                val pane = g?.dockPane(dock.size)
                val arrangement = remember(g, dock.size) {
                    if (g == null) null else SlotArrangement(List(dock.size) { g.dockIconLeft(it, dock.size) - g.dockPane(dock.size).first })
                }
                Box(Modifier.padding(horizontal = 12.dp).padding(bottom = navBottom + 10.dp).fillMaxWidth()) {
                    Row(
                        Modifier
                            .then(
                                if (g != null && pane != null) {
                                    Modifier
                                        .padding(start = with(density) { (g.originX + pane.first).toDp() })
                                        .width(with(density) { (pane.second - pane.first).toDp() })
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            )
                            .glass(ZenithShapes.panel, strength = 1.15f, material = GlassMaterial.Clear)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = arrangement ?: Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
```

with each child `Box(contentAlignment = Alignment.Center) {` (no weight), one more closing `}` for the new outer `Box`, and an import of `com.gabriel.zenithui.ui.home.SlotArrangement`.

- [ ] **Step 5: Build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add -A app/src
git commit -m "The dock stands on the page's columns"
```

---

### Task 6: the header on the page's left line, and its two settings rows

**Files:**
- Modify: `ui/home/HomeScreen.kt` (`Greeting`, its call)
- Modify: `ui/home/SettingsSheet.kt` (`HomePageSettings`)
- Modify: `ui/onboarding/PhonePreview.kt` (the name's inset)

**Interfaces:**
- Consumes: `HomeHeader.text`, `HomeGeometry.leftLine`, `ZenithSettings.header`, `ZenithSettings.searchBar`.

- [ ] **Step 1: `Greeting` in `HomeScreen.kt`**

Replace the `Greeting` composable:

```kotlin
@Composable
private fun Greeting(text: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = palette.onGlass.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

In `HomeContent`, before the `Box(Modifier.fillMaxSize()...)` that holds the home screen, add:

```kotlin
    // The hour, for a greeting at the top of the page; looked at again every minute.
    val hour by produceState(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        while (true) {
            delay(60_000)
            value = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        }
    }
```

Replace the `Greeting(...)` call at the top of the home `Column`:

```kotlin
                // The page's left line: the left edge of the first column's icons. What is written
                // at the top starts on it, and so does the search bar.
                val geometry = drag.geometry
                val leftLine = if (geometry != null) {
                    PAGE_MARGIN + with(density) { (geometry.originX + geometry.leftLine).toDp() }
                } else {
                    26.dp
                }
                val rightLine = if (geometry != null) {
                    PAGE_MARGIN + with(density) { (geometry.width - geometry.originX - geometry.rightLine).toDp() }
                } else {
                    26.dp
                }
                val header = HomeHeader.text(settings.header, settings.userName, hour)
                if (header != null) {
                    Greeting(
                        text = header,
                        modifier = Modifier
                            .statusBarsPadding()
                            // Holding it opens Customise too — always reachable, however full
                            // the page underneath is.
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showSheet(HomeSheet.Customize)
                                })
                            }
                            .padding(start = leftLine, end = rightLine, top = 8.dp, bottom = 8.dp),
                    )
                } else {
                    Spacer(Modifier.statusBarsPadding().height(8.dp))
                }
```

Add imports for `com.gabriel.zenithui.data.HomeHeader`, `androidx.compose.runtime.produceState` and `androidx.compose.ui.text.style.TextOverflow` if they are missing.

- [ ] **Step 2: `HomePageSettings` in `SettingsSheet.kt`**

In the group that holds Labels and Dock, after the Dock row:

```kotlin
        GroupDivider()
        ToggleRow("Search bar", "Above the dock, for your apps and the web", settings.searchBar) { value ->
            onChange { it.copy(searchBar = value) }
        }
```

After that group, add:

```kotlin
    Group(padding = 14.dp) {
        Caption("Top of the page")
        Spacer(Modifier.height(10.dp))
        Segmented(
            options = listOf(HomeHeader.NAME to "Name", HomeHeader.GREETING to "Greeting", HomeHeader.OFF to "Nothing"),
            selected = settings.header,
            onSelect = { value -> onChange { it.copy(header = value) } },
        )
    }
```

with an import of `com.gabriel.zenithui.data.HomeHeader`.

- [ ] **Step 3: The preview's name in `PhonePreview.kt`**

Replace the name `Text`'s modifier:

```kotlin
                modifier = Modifier.padding(
                    start = 12.dp + (page?.let { with(density) { (it.originX + it.leftLine).toDp() } } ?: 14.dp),
                    end = 26.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "The top line starts on the page's left line; name, greeting or nothing"
```

---

### Task 7: the search bar above the dock

**Files:**
- Modify: `ui/home/ZenithWidgets.kt` (`HomeSearchBar`)
- Modify: `ui/home/HomeScreen.kt` (the bar in the home column)
- Modify: `ui/home/Sheets.kt` (`WidgetPickerSheet`)
- Modify: `ui/onboarding/OnboardingFlow.kt`, `ui/onboarding/PhonePreview.kt`

**Interfaces:**
- Consumes: `ZenithSettings.searchBar`, `Widgets.placeable`, `leftLine`/`rightLine` from Task 6.
- Produces: `@Composable fun HomeSearchBar(onSearch: () -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Step 1: `HomeSearchBar` in `ZenithWidgets.kt`**

Above `SearchWidget`:

```kotlin
/** The search bar that sits above the dock when it is switched on in settings. */
@Composable
fun HomeSearchBar(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) { SearchBar(onSearch) }
}
```

- [ ] **Step 2: The bar in `HomeScreen.kt`**

Between the `PageDots(...)` call and `if (settings.showDock) DockBar(`:

```kotlin
                // From the first column's icons to the last column's, like everything on the page.
                if (settings.searchBar) {
                    HomeSearchBar(
                        onSearch = { openDrawer(search = true) },
                        modifier = Modifier.padding(start = leftLine, end = rightLine, bottom = 10.dp),
                    )
                }
```

- [ ] **Step 3: `WidgetPickerSheet` in `Sheets.kt`**

`Widgets.all.forEachIndexed` becomes `Widgets.placeable.forEachIndexed`, and `index = Widgets.all.size + index` becomes `index = Widgets.placeable.size + index`.

- [ ] **Step 4: Onboarding**

In `OnboardingFlow.kt`, add `searchBar = "search" in widgets,` to the `preview` settings. In `PhonePreview.kt`, between the page dots `Row` and the dock:

```kotlin
            if (settings.searchBar) {
                val g = page
                HomeSearchBar(
                    onSearch = {},
                    modifier = Modifier.padding(
                        start = 12.dp + (g?.let { with(density) { (it.originX + it.leftLine).toDp() } } ?: 14.dp),
                        end = 12.dp + (g?.let { with(density) { (it.width - it.originX - it.rightLine).toDp() } } ?: 14.dp),
                        bottom = 10.dp,
                    ),
                )
            }
```

with an import of `com.gabriel.zenithui.ui.home.HomeSearchBar`. The preview already sits under a touch guard ("nothing inside takes a touch"), so the bar's buttons do nothing there.

- [ ] **Step 5: Build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add -A app/src
git commit -m "The search bar is a bar above the dock, switched on in settings"
```

---

### Task 8: check it on the emulators, bring the words up to date

**Files:**
- Modify: `README.md`, `play/listing.md`

- [ ] **Step 1: Start `Android10_test` and install the build**

```bash
export PATH="$PATH:$LOCALAPPDATA/Android/Sdk/platform-tools:$LOCALAPPDATA/Android/Sdk/emulator"
emulator -avd Android10_test -no-snapshot-save -no-boot-anim &
adb -s emulator-5556 wait-for-device
until [ "$(adb -s emulator-5556 shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 2: Seed a layout from before whole rows and open the app**

List launchable apps to use as keys: `MSYS_NO_PATHCONV=1 adb -s emulator-5556 shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER`. Each line `pkg/cls` gives a dock key `"pkg/cls"` and the `pkg`, `cls` of a layout entry (expand a leading `.` in `cls` to the full class name). `SCRATCH` is the session's scratchpad directory. Write `$SCRATCH/zenith.xml` from this template, with six real apps in place of `P1/C1` to `P6/C6` (`&quot;` is how a double quote is written inside the XML):

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="settings">{&quot;onboarded&quot;:true,&quot;userName&quot;:&quot;Gabriel&quot;,&quot;columns&quot;:4,&quot;rows&quot;:4,&quot;layoutVersion&quot;:1,&quot;widgetTuck&quot;:1,&quot;dock&quot;:[&quot;P1/C1&quot;,&quot;P2/C2&quot;,&quot;P3/C3&quot;]}</string>
    <string name="layout">[{&quot;t&quot;:&quot;widget&quot;,&quot;id&quot;:&quot;bar&quot;,&quot;p&quot;:0,&quot;c&quot;:0,&quot;r&quot;:0,&quot;type&quot;:&quot;search&quot;,&quot;sx&quot;:4,&quot;sy&quot;:2},{&quot;t&quot;:&quot;widget&quot;,&quot;id&quot;:&quot;clock&quot;,&quot;p&quot;:0,&quot;c&quot;:0,&quot;r&quot;:2,&quot;type&quot;:&quot;clock&quot;,&quot;sx&quot;:2,&quot;sy&quot;:8},{&quot;t&quot;:&quot;app&quot;,&quot;id&quot;:&quot;a&quot;,&quot;p&quot;:0,&quot;c&quot;:2,&quot;r&quot;:2,&quot;pkg&quot;:&quot;P4&quot;,&quot;cls&quot;:&quot;C4&quot;},{&quot;t&quot;:&quot;app&quot;,&quot;id&quot;:&quot;b&quot;,&quot;p&quot;:0,&quot;c&quot;:3,&quot;r&quot;:2,&quot;pkg&quot;:&quot;P5&quot;,&quot;cls&quot;:&quot;C5&quot;},{&quot;t&quot;:&quot;app&quot;,&quot;id&quot;:&quot;tucked&quot;,&quot;p&quot;:0,&quot;c&quot;:0,&quot;r&quot;:9,&quot;pkg&quot;:&quot;P6&quot;,&quot;cls&quot;:&quot;C6&quot;}]</string>
</map>
```

That is a layout from before whole rows: a search widget across the top, a clock under it on a half row, two icons beside the clock on that half row, and one icon tucked a quarter row up under the clock. Then:

```bash
adb -s emulator-5556 shell am force-stop com.gabriel.zenith_Launcher.debug
MSYS_NO_PATHCONV=1 adb -s emulator-5556 push zenith.xml /data/local/tmp/zenith.xml
MSYS_NO_PATHCONV=1 adb -s emulator-5556 shell run-as com.gabriel.zenith_Launcher.debug cp /data/local/tmp/zenith.xml shared_prefs/zenith.xml
adb -s emulator-5556 shell am start -n com.gabriel.zenith_Launcher.debug/com.gabriel.zenithui.MainActivity
adb -s emulator-5556 exec-out screencap -p > "$SCRATCH/stage1-home.png"
```

Expected in the screenshot: no search widget on the page and a search bar above the dock; the clock on the top row with its top edge level with the icons beside it and its bottom edge level with the bottom of the labels of the row it ends on; the tucked icon on the row under the clock, level with nothing else out of line; three dock icons on the grid's spacing, the pane three columns wide and centred; the name starting on the same vertical line as the first column's icons and the widget's left edge.

- [ ] **Step 3: Drag, resize and settings by hand**

- `adb -s emulator-5556 shell input draganddrop x1 y1 x2 y2 1200` an icon to a spot between two rows: it lands on a whole row.
- Long press a widget (add a 2 to 3 px wobble to the swipe), choose Resize, pull the pointer down by about a row and a half: the size goes 2, then 3 rows, never a part row.
- Open Customise, Zenith settings, Home screen: switch Search bar off and on and change "Top of the page" to Greeting and to Nothing; force-stop and start the app again and see that both choices were kept.
- Take a screenshot with labels off and check a widget's bottom edge is level with the icons' bottom edges.

Then `adb -s emulator-5556 emu kill`.

- [ ] **Step 4: `README.md`**

Replace the "Resize" bullet's parenthesis and the lines after it so the bullet reads:

```markdown
- **Resize** — long-press any app or widget → **Resize**, then pull the pointer on its bottom-right
  corner. The outline follows your finger and the item snaps to whole cells and whole rows as you
  pass them, with a size badge; it can be any size up to the whole page (a widget stops at the
  smallest its design allows). An app bigger than 1×1 becomes a super icon, its icon stretched to
  fill the space. A size with no room turns the outline red. Tap anywhere to finish.
```

After the "Free placement" bullet add:

```markdown
- **Everything lines up** — icons and widgets keep to whole rows. A widget runs from the left edge
  of the icon in its first column to the right edge of the icon in its last, and from the top of
  its first row's icons to the bottom of its last row's labels, so the space under a widget is the
  space under any icon. The dock stands on the same columns as the page, and the name at the top
  starts on the same line as the first column. One class, `HomeGeometry`, works out every position
  in whole pixels. (Zenith Plus free placement still puts things anywhere.)
```

Replace the Search Bar row of the widget table with a sentence under the table:

```markdown
The **search bar** is not a widget: switch it on under Zenith settings → Home screen and it sits
above the dock, as wide as the icons above it. Tap to search apps, or hit the button for Google.
```

In "Customisation", replace "columns (3–6), rows (4–7), icon size, labels" with "grid density, labels, the search bar, what is written at the top of the page".

- [ ] **Step 5: `play/listing.md`**

Replace the last sentence of "PLACEMENT THAT JUST WORKS" ("Icons tuck in right under a widget.") with: "Everything keeps to the same lines: widgets, icons and the dock."

Replace the "WIDGETS THAT FIT" paragraph with:

```
Five built-in widgets: an oversized clock, a live battery ring, a month calendar, a quick note and quick toggles with a working torch, plus a search bar for your apps or Google that sits above the dock. Long press any widget or app, tap Resize and pull the pointer on its corner - make it any size you like, up to the whole page. Widgets are cut to the same lines as the icons around them, so nothing looks out of place.
```

- [ ] **Step 6: Final check, commit, and install for Gabriel**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
git add README.md play/listing.md
git commit -m "README and listing: whole rows, the search bar above the dock"
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.gabriel.zenith_Launcher.debug/com.gabriel.zenithui.MainActivity
```

Expected: tests and build pass; the Pixel emulator shows the tidied home screen.
