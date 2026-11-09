# The iOS 26 home screen: implementation plan

> **For agentic workers:** Gabriel chose the execution: each part below is run as a Workflow of
> subagents, one implementer per task in order (each task touches files its neighbours touch), then
> one reviewer for the part. Claude then runs the full check, looks at the part on a throwaway
> emulator, sends Gabriel pictures, and pushes. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Zenith's home screen looks like Gabriel's iOS 26 photo, with widgets in two looks and the
five twists, on the 4 Nov glass code.

**Architecture:** The data layer, drag and drop, app open and close, the drawer and the sheets stay.
The visible home screen moves out of `HomeContent` into `HomeLayout`; `HomeGeometry` gains the iPhone
proportions; the glass engine gains a wallpaper tint, a glow and a ring, each with a default that
changes nothing elsewhere. Rules are pure Kotlin objects with JVM unit tests, as everywhere in Zenith.

**Tech stack:** Kotlin, Jetpack Compose (BOM 2024.12.01), AGSL on API 33+, JUnit 4.

**Spec:** `specs/2026-11-09-ios26-home-screen-design.md`. The mockup it agrees with is
`specs/mockups/2026-11-09-home-screen-twists.html`.

## Global constraints

- Compose BOM 2024.12.01, minSdk 29, no new libraries.
- The lens, tint and ring need Android 13 (API 33); Android 12 gets the frosted copy and Android 10
  and 11 the plain body, each with a fallback for every new effect.
- Old settings, layouts, Looks and backups keep reading; a key is never reused with a new meaning.
- A rule gets a unit test before its code.
- Values that change every frame (tilt, parallax, ring, the pill's stretch, the pager's position) are
  read only inside `graphicsLayer`, draw or offset lambdas, never in composition, and never through
  `LocalPalette`. A glass pane moved by a layer passes `glass(tracking = ...)` or `glass(motion = ...)`.
- Check: in Git Bash, `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`, then
  `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`.
  One test class: `./gradlew :app:testDebugUnitTest --tests "<fully.qualified.Class>" --console=plain`.
  Instrumented tests only with `adb shell am instrument` on a throwaway emulator, never
  `connectedDebugAndroidTest`. Nothing on Gabriel's phone or his own emulator.
- Commit after each task, staging only the task's files (never `.claude/settings.local.json` or
  `AGENTS.md`). Subject "The iOS 26 home screen, part N: <what>", a prose body, and the line
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Push to `apple-look` after each part;
  never merge into `main`.
- Paths below are under `app/src/main/java/com/gabriel/zenithui/` and
  `app/src/test/java/com/gabriel/zenithui/` unless written in full.

## Review focus

1. An app opened from a home icon after the jelly press still grows out of that icon (its launch
   origin is measured at rest). Checked on the emulator after part 5.
2. A page swipe that starts on an icon turns the page with no buzz and no icon left squashed. Pinned
   by `GlassPressTest` (task 26) and checked on the emulator.
3. Holding an empty spot still opens Customise, swipe up still opens the drawer, and a tap on the
   Search pill or the dock never starts the ring. Checked on the emulator after parts 1, 4 and 5.
4. An upgraded install that answers "Switch to the new look" loses nothing: every item is still there
   after the grid changes, some on later pages. Pinned by `NewLookTest` (task 4) and checked on the
   emulator after part 2.
5. A phone lying still stops redrawing the home screen. Pinned by `TiltRestTest` (task 18) and
   checked on the emulator with `dumpsys gfxinfo` after part 3.

---

## Part 1: the grid, icons, names, dock and Search pill

### Task 1: the new settings

**Files:** Modify `data/Settings.kt`, `data/IconLook.kt`, `data/Looks.kt`. Test
`data/SettingsJsonTest.kt`, `data/LooksTest.kt`.

**Produces:** `ZenithSettings.widgetLook: String` and `ZenithSettings.newLookAsked: Boolean`;
`object WidgetLooks { const val COLOUR = "colour"; const val GLASS = "glass"; val ids; fun of(id: String): String; fun label(id: String): String; fun detail(id: String): String }`.

- [ ] **Step 1: tests.** In `SettingsJsonTest` add `widgetLook = "colour"` and `newLookAsked = true` to
  the `changed` fixture, and a test `` `settings saved before the new look read as glass widgets, not yet asked` ``:
  `fromJson(JSONObject("{}"))` gives `widgetLook == "glass"` and `newLookAsked == false`. In `LooksTest`
  add `` `a Look carries the widget look but not whether the new look was offered` ``:
  `"widgetLook" in Looks.KEYS` and `"newLookAsked" !in Looks.KEYS`. In `IconLookTest` add
  `` `widget looks fall back to glass` ``: `WidgetLooks.of("odd") == "glass"`, `of("colour") == "colour"`.
- [ ] **Step 2:** run the three classes; expect compile failures.
- [ ] **Step 3:** add the two fields with the defaults "glass" and false (keys `widgetLook`,
  `newLookAsked`, read with `optString`/`optBoolean`), `WidgetLooks` beside `IconStyles` (labels
  "Colour" and "Glass"), and `widgetLook` to `Looks.KEYS`.
- [ ] **Step 4:** run the three classes; expect PASS.
- [ ] **Step 5:** commit "part 1: the widget look and new-look settings".

### Task 2: the four-across grid

**Files:** Modify `data/GridDensity.kt`. Test `data/GridDensityTest.kt`.

**Produces:** `GridDensity.Four` (id `"four"`, label `"iPhone"`), resolved by the existing
`GridDensity.resolve(id, customColumns, customRows, widthDp, heightDp)` and `gridFor`.

- [ ] **Step 1: tests** in `GridDensityTest`: `` `four is four across with rows spaced like the columns` ``:
  `resolve("four", 4, 6, 411f, 923f) == 4 to 6`, `resolve("four", 4, 6, 360f, 800f) == 4 to 6`,
  `resolve("four", 4, 6, 360f, 740f) == 4 to 5`; `` `four on a tablet is Balanced` ``: for
  `800f x 1280f` it equals `resolve("balanced", ...)`; `` `the old presets keep their numbers` ``:
  the existing 5x6 / 4x5 / 4x4 assertions on 448 x 997 still hold.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** add `Four`. Below 600 dp wide: columns 4, rows
  `floor((heightDp - 230f) / (widthDp / 4f)).toInt().coerceIn(4, 8)` (230 dp is the status bar, the
  Search band and the dock); from 600 dp it behaves as Balanced.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 1: the four-across grid".

### Task 3: the iPhone icon shape

**Files:** Modify `data/IconLoader.kt` (`IconShapes`), `ui/theme/Shapes.kt`,
`ui/theme/LiquidGlassShader.kt`, `ui/theme/Glass.kt`, `ui/settings/SettingsCatalog.kt`. Test
`data/IconLookTest.kt`, new `ui/theme/SuperellipseTest.kt`.

**Produces:** icon shape id `"iphone"` (label `"iPhone"`): a superellipse with exponent 5;
`ZenithShapes.forIconShape("iphone") == SquircleShape(5f)`; `internal fun superellipseRadius(angle: Float, exponent: Float): Float`
in `Shapes.kt` (the distance from the middle to the edge of a unit superellipse at that angle), used
by both `SquircleShape` and `IconShapes.path`.

- [ ] **Step 1: tests.** `IconLookTest`: exactly 9 shape ids, `"iphone"` last, `label("iphone") == "iPhone"`,
  "Squircle" kept. `SuperellipseTest`: `superellipseRadius(0f, 5f) == 1f` (±1e-4); at 45 degrees the
  exponent-5 radius is larger than the exponent-2.9 one, and both are below `sqrt(2)`.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** add the id; draw its path with the existing private `superellipse(path, size, 5f)`;
  map it in `forIconShape`; add "iphone" to the icon shape entry's keywords. In the shader add
  `uniform float squircleN` (default 2.9, set from `(shape as? SquircleShape)` when the outline is the
  squircle branch) so a glass tile under the iPhone shape gets the matching rim.
- [ ] **Step 4:** run; expect PASS. `GlassLookTest` and `GlassTuningTest` still pass.
- [ ] **Step 5:** commit "part 1: an iPhone icon shape".

### Task 4: the new look as a rule

**Files:** Create `data/NewLook.kt`. Test new `data/NewLookTest.kt`.

**Consumes:** tasks 1 to 3. **Produces:**
`object NewLook { fun apply(s: ZenithSettings): ZenithSettings; fun due(s: ZenithSettings): Boolean; fun isOn(s: ZenithSettings): Boolean }`.

- [ ] **Step 1: tests.** `` `the new look sets the grid, the icons, the widgets and the dock, and nothing else` ``:
  `apply(s)` equals `s.copy(gridDensity = "four", iconShape = "iphone", widgetLook = "colour", dockStyle = "glass", showDock = true, newLookAsked = true)`
  for a settings value with every one of those different. `` `it is due once, on a phone that was set up before it` ``:
  `due(ZenithSettings(onboarded = true))` true; false once `newLookAsked`; false when not onboarded.
  `` `switching a 5x5 home to the iPhone grid keeps every item` ``: a 5x5 layout of 25 apps, two
  widgets and a folder, settled with `LayoutBuilder.settle` to 4x6, keeps every id.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement; `isOn` is true when the four fields already equal the new look's.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 1: the new look as one rule".

### Task 5: iPhone proportions in HomeGeometry

**Files:** Modify `data/HomeGeometry.kt`. Test `data/HomeGeometryTest.kt`.

**Produces:** icon size `(0.64f * cellWidthDp).coerceIn(52f, 68f)`, still capped by the row and the
cell as today; `fun widgetFrame(spanX: Int, spanY: Int): PxRect` ending at the last row's icon
bottom; new `fun widgetNameStrip(spanX: Int, spanY: Int): PxRect` (the label strip under the last
row, the widget's width); constants `SEARCH_BAND_DP = 44f`, `SEARCH_PILL_HEIGHT_DP = 32f`,
`DOCK_HEIGHT_DP = 92f`, `DOCK_SIDE_INSET_DP = 10f`, `DOCK_CORNER_DP = 36f`, `WIDGET_CORNER_DP = 22f`.

- [ ] **Step 1: tests.** Replace `` `icons are sized as they always were` `` with
  `` `icons are about two thirds of a cell, between 52 and 68 dp` `` on the Pixel 10a's 4x6 page
  (411 x 923 dp, density 2.625, the page area the new layout leaves); change the widget tests to
  `` `a widget runs from the top of its first icons to the bottom of its last icons` `` and
  `` `a widget's name sits in the strip under its last icons, as wide as the widget` ``; keep
  `` `with labels off a widget ends where its last icons end` ``; add
  `` `a 2x2 widget on the four grid is within a tenth of square` `` (on the Pixel 10a its rows are about
  1.12 times its columns, as an iPhone's are).
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement. The Search band and the dock do not depend on the page, so the page's
  rows cannot feed back into them.
- [ ] **Step 4:** run `HomeGeometryTest`, `DropPlannerTest`, `LayoutBuilderTest`; expect PASS.
- [ ] **Step 5:** commit "part 1: iPhone proportions".

### Task 6: names white on the wallpaper

**Files:** Modify `ui/common/Components.kt`, `ui/home/HomeGrid.kt`. Test new `ui/common/LabelInkTest.kt`.

**Produces:** `LabelStyle(size, shadow, onWallpaper: Boolean = false)`;
`internal fun labelInk(onWallpaper: Boolean, onGlass: Color): Color`;
`internal fun labelShadow(onWallpaper: Boolean, legacyShadow: Boolean, density: Float): Shadow?`.

- [ ] **Step 1: tests.** On the wallpaper the ink is `Color.White` whatever `onGlass` is, and the
  shadow is `Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f * density), 3f * density)`; off it
  the ink is `onGlass` and the shadow is today's only when `legacyShadow`.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement; `HomePage` provides `LocalLabelStyle` with `onWallpaper = true` around
  its items; the drawer and the folder panel are untouched.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 1: white names on the wallpaper".

### Task 7: the visible home screen in its own file

**Files:** Create `ui/home/HomeLayout.kt`; modify `ui/home/HomeScreen.kt` (the Column at about
1529 to 1773).

**Produces:**
`@Composable fun HomeLayout(pager: @Composable (Modifier) -> Unit, searchBand: @Composable (Modifier) -> Unit, dock: @Composable (Modifier) -> Unit, overlays: @Composable BoxScope.() -> Unit, modifier: Modifier = Modifier)`.
`HomeContent` keeps all state and passes slots.

- [ ] **Step 1:** move the Column into `HomeLayout`, behaviour unchanged (header, pager, dots, search
  bar and dock still drawn as before in this task).
- [ ] **Step 2:** run the full check; expect PASS.
- [ ] **Step 3:** commit "part 1: the home screen's layout in a file of its own".

### Task 8: the Search pill, and no header, dots or bar

**Files:** Create `ui/home/SearchPill.kt`; modify `ui/home/HomeLayout.kt`, `ui/home/HomeScreen.kt`,
`ui/settings/SettingsPages.kt`, `ui/settings/SettingsCatalog.kt`.

**Produces:**
`@Composable fun SearchPill(onOpen: () -> Unit, dots: () -> Float, pageCount: Int, position: () -> Float, modifier: Modifier = Modifier)`
(a `glass(ZenithShapes.pill, material = GlassMaterial.Clear, interactive = true)` capsule,
`SEARCH_PILL_HEIGHT_DP` tall, a magnifier and "Search" in white; `dots` is 0 here and is driven in
part 4).

- [ ] **Step 1:** the band holds the pill, centred; the header (`Greeting`, `LiveHeader`), `PageDots`
  and `HomeSearchBar` are no longer drawn on the home screen; tapping the pill calls
  `openDrawer(search = true)` until part 4; `UndoPill` sits 12 dp above the band and
  `DefaultLauncherHint` above the page, so neither covers the pill.
- [ ] **Step 2:** hide the Settings rows for header, page indicator and search bar, and drop their
  catalog entries; the stored fields stay. `SettingsCatalogTest` must still pass.
- [ ] **Step 3:** run the full check; expect PASS.
- [ ] **Step 4:** commit "part 1: the Search pill in place of the header, dots and bar".

### Task 9: the floating glass dock

**Files:** Modify `ui/home/HomeGrid.kt` (`DockBar`), `data/HomeGeometry.kt`. Test `data/HomeGeometryTest.kt`.

**Produces:** `fun dockSlabWidth(style: String, icons: Int, screenWidthPx: Int, iconPx: Int, density: Float): Int`
in `HomeGeometry`'s companion.

- [ ] **Step 1: tests.** `` `the glass dock spans the screen less its insets` ``: style "glass" gives
  `screenWidthPx - 2 * 10 dp`; `` `a floating dock hugs fewer than four icons` ``: "floating" with 2
  icons is narrower than with 4 and never wider than "glass"; "none" gives the same as "glass" (the
  icons' row; no pane is drawn).
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement and restyle `DockBar`: a `glass(RoundedCornerShape(DOCK_CORNER_DP.dp), material = GlassMaterial.Clear, interactive = true)`
  slab `DOCK_HEIGHT_DP` tall, its icons the page's icon size, evenly spaced; "none" draws no pane.
  Reordering, dragging out and dropping in are unchanged; the accent rim while an app is carried over
  it is replaced by the existing 3% swell alone.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 1: the floating glass dock".

### Task 10: widget names, and the previews and setup follow

**Files:** Modify `ui/home/HomeGrid.kt` (`HomeItemContent`), `data/Models.kt`,
`ui/onboarding/PhonePreview.kt` (`MiniHome`), `ui/onboarding/OnboardingFlow.kt`,
`ui/onboarding/StudioStep.kt`, `ZenithViewModel.kt` (`buildHome`). Test new `data/WidgetNamesTest.kt`.

**Produces:** `fun Widgets.shortName(type: String): String`.

- [ ] **Step 1: tests.** Every placeable type has a short name of 1 to 12 characters ("Clock",
  "Battery", "Calendar", "Notes", "Controls", "Music", "World Clock", "Countdown", "Photos", "To-do",
  "Aura", "Weather"); a `bare_` type has its base type's name; an unknown type is "Widget".
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement; draw each widget's name in `widgetNameStrip` (other apps' widgets: the
  provider app's label; a stack: its shown member's name, wiring `WidgetStack.onShown`). `MiniHome`
  draws the same pill, dock and white names with no header or dots, and provides
  `LocalGlassLevels provides null` inside, as `WallpaperSheet` does. Setup offers "iPhone" first in its
  grid choice and starts on it, and `buildHome` saves `NewLook.apply(...)`'s fields for a new install.
- [ ] **Step 4:** run the full check; expect PASS.
- [ ] **Step 5:** commit "part 1: widget names, and setup and the previews follow".

---

## Part 2: widgets in colour and in glass

### Task 11: the colour look's colours

**Files:** Create `ui/home/WidgetColours.kt`. Test new `ui/home/WidgetColoursTest.kt`.

**Produces:** `data class WidgetFill(val top: Color, val bottom: Color, val ink: Color, val quiet: Color, val accent: Color)`;
`enum class Sky { Day, Night, Cloud, Rain, Snow, Storm }`;
`object WidgetColours { fun of(kind: String, dark: Boolean, sky: Sky = Sky.Day, own: Color? = null): WidgetFill; fun skyFor(code: Int, day: Boolean): Sky }`;
`internal fun contrast(a: Color, b: Color): Float` (WCAG).

- [ ] **Step 1: tests.** For every `Widgets.placeable` kind, light and dark, and every `Sky`: `ink`
  and `quiet` stand at least 4.5 to 1 on both `top` and `bottom`. `skyFor`: code 0 by day is Day, by
  night Night; 3 is Cloud; 61 Rain; 71 Snow; 95 Storm. An `own` colour: deep blue gives white ink,
  pale yellow gives near-black ink, and `top`/`bottom` are that colour lightened and darkened.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement with these fills: Weather Day `#4A90E2`→`#2C64BD`, Night `#1C2A4A`→`#0B1226`,
  Cloud `#7D8A99`→`#5B6675`, Rain `#5A6F85`→`#3D4E61`, Snow `#9AB4CF`→`#7591AE`, Storm `#4A4F5C`→`#2B2E36`,
  white ink; Calendar white with black ink and red `#FF3B30` accent, dark `#1C1C1E` with white ink and
  `#FF453A`; Notes `#FFF8D6`/`#2C2A20`; every other kind white light, `#1C1C1E` dark, with the
  palette's accent where it stands (else the ink). Weather's quiet ink and the rest may be adjusted to
  pass the contrast test.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 2: the colour look's colours".

### Task 12: one switch for every Zenith widget

**Files:** Modify `ui/home/ZenithWidgets.kt` (`ZenithWidget`, the ink helpers), `ui/home/HomeGrid.kt`
(`InOwnColour`), `ui/theme/Theme.kt`, `ui/home/HostedWidgets.kt` (corner radius).

**Produces:** `val LocalWidgetLook = staticCompositionLocalOf { WidgetLooks.GLASS }` provided by
`ZenithTheme` from `settings.widgetLook`.

- [ ] **Step 1:** colour look: a `RoundedCornerShape(WIDGET_CORNER_DP.dp)` card filled with the
  `WidgetFill` gradient and a soft two-part shadow, its body under a palette whose `onGlass` and
  accent come from the fill. Glass look: the existing Regular `glass` pane with that shape, its body
  under a dark palette (white words) with the soft text shadow `bare_` widgets use, and no accent
  washes. A widget's own colour is the fill in colour and the glass tint in glass. `bare_` widgets draw
  as before. Other apps' widgets clip to the same radius.
- [ ] **Step 2:** run the full check; expect PASS.
- [ ] **Step 3:** commit "part 2: one switch for every widget".

### Task 13: Weather the iOS way

**Files:** Modify `data/Weather.kt`, `ui/home/MoreWidgets.kt` (`WeatherWidget`). Test `data/WeatherParseTest.kt`.

**Produces:** `data class WeatherDay(val day: String, val code: Int, val high: Double, val low: Double)`;
`Forecast.days: List<WeatherDay>` (default empty, so old saved forecasts read).

- [ ] **Step 1: tests.** The sample answer, extended with `daily.weather_code` and six days of
  `temperature_2m_max/min`, parses five next days with their weekday names; `toJson`/`fromJson` round
  trip exactly; a saved forecast with no days reads with an empty list.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** ask for `daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=6`
  (still only latitude and longitude); Square: place, the temperature large and light, the sky's
  symbol and words, "H:x° L:y°"; Wide: that, plus Now and five hours; Big: Wide's top plus five day
  rows (name, symbol, low and high on a bar). Its sky picks the fill (`WidgetColours.skyFor`). Other
  forms keep their layouts.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 2: Weather the iOS way".

### Task 14: Calendar the iOS way

**Files:** Modify `data/CalendarRepository.kt`, `ui/home/ZenithWidgets.kt` (`CalendarWidget`),
`ui/home/HomeScreen.kt` (`setUpWidget`). Test new `data/CalendarUpcomingTest.kt`.

**Produces:** `fun CalendarRepository.upcoming(events: List<CalendarEvent>, nowMillis: Long, most: Int = 3): List<CalendarEvent>`.

- [ ] **Step 1: tests.** Finished events are dropped, one under way is kept, all-day events come
  first, the rest by start, at most `most`.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** Square: the weekday in red capitals, the date large, the next event and its time,
  or "No events today"; Wide: the date on the left and up to three events; Big: the day's events in
  full. Without calendar access the widget shows "Show your events", which asks once through
  `CalendarAccess` (`setUpWidget("calendar")`). Today's date comes from a value that turns over at
  midnight. Other forms keep their layouts.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 2: Calendar the iOS way".

### Task 15: every other widget in both looks

**Files:** Modify `ui/home/ZenithWidgets.kt`, `ui/home/MoreWidgets.kt`.

- [ ] **Step 1:** clock, battery, toggles, notes, music, world clock, countdown, photos, to-do and
  Aura read their ink and accents from the palette the look provides, with no layout change; nothing
  in them hard-codes white or the theme's accent.
- [ ] **Step 2:** run the full check; expect PASS.
- [ ] **Step 3:** commit "part 2: every widget in both looks".

### Task 16: the switch in Customise and Settings

**Files:** Modify `ui/home/Sheets.kt` (`CustomizeSheet`), `ui/home/HomeScreen.kt`,
`ui/settings/SettingsPages.kt` (`HomeOptions`), `ui/settings/SettingsCatalog.kt`. Test
`ui/settings/SettingsCatalogTest.kt`.

**Produces:** `CustomizeSheet(..., widgetLook: String, onWidgetLook: (String) -> Unit, newLookOn: Boolean, onNewLook: () -> Unit, ...)`.

- [ ] **Step 1: tests.** `SettingsCatalogTest`: the entry `widgetLook` ("Widget look", keywords
  "colour glass solid widgets") is found first by its title.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** Customise gets a Colour / Glass control and, while `!NewLook.isOn(settings)`, a "Use the
  new look" row that applies `NewLook.apply`. Settings, Home screen gets the same control (Spot
  `widgetLook`); the grid choice lists iPhone; the shape choice shows the ninth shape.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 2: the look in Customise and Settings".

### Task 17: asking a phone that already has Zenith

**Files:** Create `ui/home/NewLookSheet.kt`; modify `ui/home/HomeScreen.kt`.

**Produces:** `@Composable fun NewLookSheet(visible: Boolean, settings: ZenithSettings, onSwitch: () -> Unit, onKeep: () -> Unit)`.

- [ ] **Step 1:** shown while `NewLook.due(settings)`: a `ZenithSheet` whose dismiss does nothing (it
  springs back), a `PhonePreview` of the home screen under `NewLook.apply(settings)`, the words "Zenith
  has a new look" and "Your apps and widgets keep their order. Some may move to another page.",
  `PrimaryButton("Switch to the new look")` and `GlassButton("Keep my home screen")`. Switch:
  `updateSettings { NewLook.apply(it) }`; Keep: `updateSettings { it.copy(newLookAsked = true) }`.
- [ ] **Step 2:** run the full check; expect PASS.
- [ ] **Step 3:** commit "part 2: asking a phone that already has Zenith".

---

## Part 3: light that moves, and glass from the wallpaper

### Task 18: tilt that rests where you hold it

**Files:** Create `ui/theme/TiltRest.kt`; modify `ui/theme/Tilt.kt`, `ui/theme/Theme.kt`. Test new
`ui/theme/TiltRestTest.kt`.

**Produces:** `object TiltRest { fun follow(rest: Offset, reading: Offset, dtSec: Float, halfLifeSec: Float = 2.5f): Offset; fun relative(reading: Offset, rest: Offset): Offset; fun still(previous: Offset, next: Offset, deadZone: Float = 0.012f): Boolean; fun parallaxShift(parallax: Offset, depthDp: Float, density: Float): Offset }`;
`val LocalParallax: ProvidableCompositionLocal<State<Offset>>` (zero when movement is reduced or Tilt
shine is off). `LocalTilt` keeps its meaning (absolute, for the light).

- [ ] **Step 1: tests.** `follow` closes half the gap in one half-life; a reading held 10 s becomes
  the rest (`relative` within 0.01 of zero); `relative` is clamped to ±1; `still` is true for a change
  under the dead zone and false above; `parallaxShift(Offset(1f, 0f), 4f, 2f) == Offset(-8f, 0f)`
  and zero tilt gives zero.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement; `rememberTilt` uses `SENSOR_DELAY_GAME`, steps its spring on the frame
  clock between readings, and stops writing its states while `still`.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 3: tilt that rests where you hold it".

### Task 19: icons, widgets and the dock float

**Files:** Modify `ui/home/HomeGrid.kt` (`HomePage`'s per-item `graphicsLayer`, `DockBar`).

- [ ] **Step 1:** items shift by `parallaxShift(parallax, 4f, density)` and the dock by
  `parallaxShift(parallax, 2f, density)` inside their existing graphicsLayers; every glass pane moved
  this way passes `glass(tracking = { parallax.value })`.
- [ ] **Step 2:** run the full check; expect PASS.
- [ ] **Step 3:** commit "part 3: icons, widgets and the dock float".

### Task 20: one lens for every pane

**Files:** Modify `ui/theme/Glass.kt` (`rememberLens`), `ui/theme/Theme.kt`.

**Produces:** `val LocalLens = staticCompositionLocalOf<LiquidGlassShader?> { null }`, made once per
`GlassLevels` in `ZenithTheme`; `glass()` uses it, falling back to `rememberLens` where none is
provided (previews, `PinItemActivity`, `IslandService`).

- [ ] **Step 1:** implement.
- [ ] **Step 2:** run the full check; install the test APK on the throwaway emulator and run
  `GlassSheetTest` and `LiquidGlassShaderTest` with `adb shell am instrument`; expect PASS.
- [ ] **Step 3:** commit "part 3: one lens for every pane".

### Task 21: glass made from your wallpaper

**Files:** Create `ui/theme/GlassTint.kt`; modify `ui/theme/LiquidGlassShader.kt`, `ui/theme/Glass.kt`,
`ui/home/SearchPill.kt`, `ui/home/HomeGrid.kt` (dock), `ui/home/ZenithWidgets.kt` (glass look). Test new
`ui/theme/GlassTintTest.kt`; instrumented `LiquidGlassShaderTest`.

**Produces:** `glass(..., wallpaperTint: Boolean = false)`;
`internal fun tintMix(veil: Color, wall: Color, amount: Float): Color`;
`internal fun glowColour(wall: Color, dark: Boolean): Color`;
`internal fun paneColour(haze: ImageBitmap, centreInScreen: Offset, screen: IntSize): Color`;
`const val WALL_TINT = 0.18f`.

- [ ] **Step 1: tests.** `tintMix(veil, wall, 0f) == veil`; the result keeps the veil's alpha; at 1
  its colour is the wall's. `glowColour` has the wall's hue, more saturation, alpha 0.28 in the light
  and 0.36 in the dark. `LiquidGlassShaderTest` sets the new uniform `wallTint` (0 and 0.18) and
  compiles.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** in the lens, mix the veil towards the colour it already samples from the haze
  ("air") by `wallTint`; in the two fallbacks use `paneColour` at the pane's middle, or the saved
  wallpaper colours, or the palette's accent when there is no copy. The glow is a third paint beside
  `shadowPaints`, outside the clip, blur 18 dp, falling 6 dp. The dock, the pill and glass-look widgets
  pass `wallpaperTint = true`; a widget with its own colour passes its colour as `tint` instead.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 3: glass made from your wallpaper".

---

## Part 4: liquid Search

### Task 22: one way to match apps

**Files:** Create `data/AppSearch.kt`; modify `ui/home/AppDrawer.kt`. Test new `data/AppSearchTest.kt`.

**Produces:** `object AppSearch { fun match(query: String, apps: List<AppInfo>, limit: Int = 24): List<AppInfo>; fun go(query: String, apps: List<AppInfo>): AppInfo? }`.

- [ ] **Step 1: tests.** Names starting with the query come before names containing it; case is
  ignored; a blank query matches nothing; work apps match; `go` is the first match or null.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement; the drawer's inline filter and Go use it.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 4: one way to match apps".

### Task 23: the pill's words, dots and stretch as rules

**Files:** Create `ui/home/SearchPillState.kt`. Test new `ui/home/SearchPillStateTest.kt`.

**Produces:** `fun pillDots(pagerMoving: Boolean, msSinceSettled: Long): Boolean` (dots while moving and
for 1000 ms after); `fun pillToField(progress: Float, pill: Rect, field: Rect): Rect`.

- [ ] **Step 1: tests.** Moving gives dots; 999 ms after settling, dots; 1000 ms, words.
  `pillToField(0f)` is the pill, `(1f)` the field, `(0.5f)` halfway on every edge, and a progress past
  1 (the overshoot) goes past the field in proportion.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 4: the pill's rules".

### Task 24: the pill stretches into search

**Files:** Create `ui/home/SearchField.kt`; modify `ui/home/SearchPill.kt`, `ui/home/HomeScreen.kt`,
`data/Gestures.kt` (`GestureActions.detail(SEARCH)`).

**Produces:**
`@Composable fun SearchField(open: Boolean, from: Rect, apps: List<AppInfo>, shortcuts: List<AppShortcut>, onOpenApp: (String) -> Unit, onClose: () -> Unit)`.

- [ ] **Step 1:** the pill shows the page dots while `pillDots` (dot sizes from
  `PageEffects.dotWidth` and `dotPresence`, reading the pager's position while drawing). Tapping it
  opens `SearchField`: its glass is drawn with `drawLiquidGlass` in a Canvas at
  `pillToField(spring, pill, field)` on a spring with a little overshoot, as `AppLaunchCard` draws its
  card; the field sits on the keyboard (`imePadding`); the home screen dims and softens behind it
  (blur on API 31+, a dim alone below); typing shows `AppSearch.match` results, then `SearchAnswers`,
  then "Search the web" (`webSearch`). Back or a tap outside shrinks it back into the pill. It shares
  `HomeTypingArea` with the note and to-do widgets. With movement reduced the field fades in. The
  Search gesture action opens it too ("Search, with the keyboard up").
- [ ] **Step 2:** run the full check; expect PASS.
- [ ] **Step 3:** commit "part 4: the pill stretches into search".

---

## Part 5: a drop on the glass, and jelly icons

### Task 25: the ring as a rule

**Files:** Create `ui/theme/GlassRing.kt`. Test new `ui/theme/GlassRingTest.kt`.

**Produces:** `@Stable class GlassRing { fun start(at: Offset, nowNanos: Long); fun now(nowNanos: Long): RingNow? }`;
`data class RingNow(val centre: Offset, val radius: Float, val width: Float, val strength: Float)`;
`fun ringRadius(elapsedMs: Float, reachPx: Float, durationMs: Float = 1200f): Float` (cubic ease-out);
`fun ringStrength(elapsedMs: Float, durationMs: Float = 1200f): Float`;
`fun paneLight(distancePx: Float, ring: RingNow): Float` (`strength * exp(-((distance - radius) / width)^2)`);
`val LocalGlassRing = staticCompositionLocalOf<GlassRing?> { null }`.

- [ ] **Step 1: tests.** The radius is 0 at the start and `reachPx` at the end; strength goes from 1
  to 0; `paneLight` peaks where the distance equals the radius and is under 0.02 two widths away;
  `now` is null after the duration.
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** implement.
- [ ] **Step 4:** run; expect PASS.
- [ ] **Step 5:** commit "part 5: the ring as a rule".

### Task 26: the ring through the glass, and jelly icons

**Files:** Modify `ui/theme/LiquidGlassShader.kt`, `ui/theme/Glass.kt`, `ui/theme/GlassPress.kt`,
`ui/home/HomeScreen.kt`, `ui/home/HomeGrid.kt`; create `ui/home/JellyIcon.kt`. Test
`ui/theme/GlassPressTest.kt`; instrumented `LiquidGlassShaderTest`.

**Produces:** `fun Modifier.glassPress(motion: GlassMotion, enabled: Boolean = true, haptic: Boolean = true): Modifier`;
`internal fun pressTicks(haptic: Boolean): Boolean`; `fun Modifier.jellyIcon(shape: Shape): Modifier`.

- [ ] **Step 1: tests.** `GlassPressTest`: `pressTicks(false)` is false and `pressTicks(true)` true,
  and every existing number holds. `LiquidGlassShaderTest` sets `ringCentre`, `ringRadius`,
  `ringWidth`, `ringStrength` (strength 0 draws as before).
- [ ] **Step 2:** run; expect FAIL.
- [ ] **Step 3:** a single tap on empty wallpaper (the page's `detectTapGestures` `onTap`, which items,
  the dock and the pill never reach) starts `GlassRing` at the finger, replacing the double-tap ring;
  no ring with movement reduced. Every pane reads `LocalGlassRing` while drawing, with the centre in
  its own pixels: the lens lights its rim and face in the band, the fallbacks brighten the rim stroke
  by `paneLight`, and icons and colour widgets get a white gleam of at most 0.18 alpha.
  `jellyIcon` is `glassPress(GlassMotion.Jelly, haptic = false)` with a finger light drawn over the
  icon's own pixels; home icons (`AppTile`, `FolderTile`, `SuperAppTile`, `ShortcutTile` on a page,
  and the dock's) use it in place of `pressBounce`, with `launchOrigin` still before it; the drawer and
  folder panel keep the Pressing setting.
- [ ] **Step 4:** run; expect PASS (instrumented with `am instrument`).
- [ ] **Step 5:** commit "part 5: a drop on the glass, and jelly icons".

---

## Part 6: finishing

### Task 27: the README, the glass pictures and the last check

**Files:** Modify `README.md` (the home screen, widgets, the five twists, the new-look question),
`app/src/androidTest/java/com/gabriel/zenithui/ui/theme/GlassSheetTest.kt` (its panes: a 4x2 glass
widget, the floating dock, the small pill).

- [ ] **Step 1:** update both.
- [ ] **Step 2:** run the full check and the instrumented glass tests; expect PASS.
- [ ] **Step 3:** on the throwaway emulator, in light and dark, colour and glass: a fresh setup to the
  home screen; an install that already had Zenith answering both ways; everything in Review focus;
  pictures to Gabriel.
- [ ] **Step 4:** commit "part 6: the README and the glass pictures"; push.
