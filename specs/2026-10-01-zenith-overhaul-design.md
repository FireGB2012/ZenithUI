# Zenith overhaul: line-up, wallpapers, settings, motion

- Date: 2026-10-01
- Branch: `overhaul` (checkpoint of the previous work is `fb38bc5` on `main`)
- Status: design agreed in conversation; this document awaits review

## Goal

Gabriel asked for a home screen where everything lines up, animation everywhere so people think
"wow, this is cool", an app that is easy to use, a redone Settings, more wallpapers, and a wallpaper
that changes colour with the theme.

The work is done when:

1. Nothing on the home screen can end up out of line unless Plus free placement is on.
2. Changing palette or light/dark repaints the chosen Zenith wallpaper in the new colours.
3. Settings is a full page with search, a live preview, and every option reachable in two taps.
4. The motion listed in stage 4 is in, and all of it can be turned down with one switch.
5. Unit tests and the debug build pass after every stage.

## Decisions made with Gabriel

| Question | Answer |
| --- | --- |
| What the wallpaper follows | Both light/dark and the palette |
| What is out of line today | Widgets vs icons, icons vs dock, gaps between rows, top of the page |
| Home grid rule | Whole rows only; the search bar becomes a fixed bar above the dock |
| New wallpapers | About half free, half Zenith Plus |
| Settings shape | Full-height page with search and a live mini phone |
| Bigger motion pieces | Folders open in place, jiggle while carrying, page swipe styles, fly-in on coming home |
| Git | Checkpoint commit on `main`, then all work on `overhaul` |

## Not in scope

- Hosting third-party Android widgets.
- Landscape (the activity stays portrait).
- Live wallpapers, or changing a photo wallpaper with the theme.
- Redesigning onboarding. It only changes where it must (see stage 1, search bar).

## Delivery

Four stages, in order. Each stage gets its own implementation plan, written just before it starts.
Each ends with unit tests and `assembleDebug` passing, a check on the emulator, README and
`play/listing.md` brought up to date, and a commit on `overhaul`.

---

## Stage 1: the home grid lines up

### Why it is off today

- `ROW_UNITS = 4` splits a row into quarter-steps. `DropPlanner.canTake` lets a widget sit on any
  step, and widget heights are counted in steps, so widget edges fall between icon lines.
- `LayoutBuilder.blockedSpanY` gives up `widgetTuck` steps at the bottom of a widget so an icon can
  tuck in under it. A tucked icon is a quarter row above the icons beside it.
- `Modifier.toIconLines` stops a widget at the bottom of the icons in its last row. The label strip
  under it stays empty, so the gap under a widget (about 59 dp on a Pixel) is bigger than the gap
  under an icon's label (about 39 dp). Tucking was added to hide that.
- `DockBar` centres its icons under the columns only when `keys.size == columns`.
- `Greeting` uses a fixed `padding(horizontal = 26.dp)`. That is within 2 dp of the grid's left
  line on a four-column phone, and further off on every other grid (about 15 dp on three columns).
- Positions are rounded per item (`roundToInt` for the offset, dp for the size), so two edges that
  should meet can differ by a pixel.

### Rules

1. Unless Plus free placement is on, every item starts on a whole row and is whole rows tall:
   `row % ROW_UNITS == 0` and `spanY % ROW_UNITS == 0`. This holds for icons, folders, super icons
   and widgets.
2. A widget is drawn from the left edge of the icon in its first column to the right edge of the
   icon in its last column, and from the top of the icons in its first row to the bottom of the
   label strip in its last row. With labels off the label strip is zero high.
3. Nothing tucks. `widgetTuck` is always 0 and is no longer worked out; the field stays in
   `ZenithSettings` so old backups still read.
4. Plus free placement keeps today's behaviour: any step, any size, with offsets.
5. `ROW_UNITS` stays 4 and the saved layout format does not change.

Widget smallest sizes become whole rows: Clock, Power, Quick Note and Quick Toggles one row,
Calendar two rows. Default sizes stay 2 by 2 rows.

### One source of positions: `HomeGeometry`

A new pure Kotlin class, `data/HomeGeometry.kt`, with no Compose or Android types, works out every
position in whole pixels:

- Input: page width and height in px, columns, rows, pixels per dp, labels on or off.
- Every cell is the same whole number of pixels wide and every row the same number tall. Pixels
  left over when the page does not divide evenly are shared between the two outer edges, so no
  cell is a pixel bigger than its neighbour and every inset is the same in every cell.
- Output: icon size (today's `homeIconSize` rule), the icon rectangle and label strip of a cell, the
  frame of a widget or super icon for a column, row and span, the header's left inset, the search
  bar's frame, and the centre of each dock slot for a given number of dock apps.

`HomePage`, the drop hint, `ResizeFrame`, `DragGhost`, `DockBar`, `Greeting` and the new search bar
all read from it. `iconLines`, `toIconLines` and `superIconFrame` are replaced by calls into it.

### Placement code

- `DropPlanner`: `canTake` becomes "free placement, or the row is a whole row"; widgets follow the
  same rule as icons. `Page.tuck` and `misfit`'s out-of-line and widget-pull terms are removed,
  since nothing can be out of line any more. The promise in `DropPlannerTest` stays: an item lands
  where it is let go, at most half a row away, and neighbours step aside.
- `LayoutBuilder`: `ICON_ROW_STEP` becomes `ROW_UNITS`; `settle` uses whole rows for everything
  unless `free`; `blockedSpanY` returns `spanY`.
- `ZenithViewModel`: `resizeWidget` snaps to whole rows unless free; `setWidgetTuck` and the
  `LaunchedEffect` in `HomeScreen` that feeds it are removed.

### Search bar

The search bar stops being a grid widget.

- New setting `searchBar: Boolean` (default false). When on, the bar is drawn between the page dots
  and the dock at its current height (50 dp), as wide as the grid from the first column's icon edge
  to the last column's. Tapping it opens the drawer with search focused, as the widget does today.
- `Widgets.Search` leaves the widget picker. The type is still read from saved layouts so they can
  be converted.
- Onboarding: choosing Search Bar in the widget step turns the setting on instead of placing a cell.
- Stage 1 adds a "Search bar" switch to the existing Home screen settings page so it is usable at
  once. Stage 3 moves it.

### Dock

- Slots use the page's column spacing. With `n` apps and `n <= columns`, the group is centred: app
  `i` sits at column position `(columns - n) / 2 + i`. With `n == columns` each icon is exactly
  under a column. With a different count the group is still on the grid's spacing, offset by half
  a column when the difference is odd, as on an iPhone.
- The dock takes at most `columns` apps, and never fewer than four: on a three-column grid a
  fourth is allowed, spread evenly with the others. A dock that already holds more keeps them, spread evenly
  and drawn a little smaller if that is what it takes to fit, and takes no more until it is back
  under the limit.
- Dock icon size stays the page's icon size.
- The dock pane's height comes from the column width alone, never from the page's icons. On a
  cramped page the icons shrink to fit their rows; if the dock shrank with them the page would
  gain the room back, the icons would grow, and the two would chase each other on every frame.

### Header

- The name's left edge is the left edge of the first column's icon, from `HomeGeometry`.
- New setting `header`: `name` (default, as today), `greeting` ("Good morning, Gabriel", by time of
  day) or `off`. Long press still opens Customise when it is shown.

### One-time tidy (`layoutVersion` 2)

Runs once at start-up, and on restoring an older backup, in `ZenithViewModel` beside the existing
version upgrades:

1. Any `search` widget is removed and `searchBar` is turned on.
2. `widgetTuck` is set to 0.
3. If free placement is not in effect: each widget's height is rounded to the nearest whole row, no
   smaller than its new smallest size; each item's row is rounded to the nearest whole row. A row
   exactly halfway between two goes to the one nearer the top of the page, so icons that sat under
   a search bar close the gap it left.
4. `LayoutBuilder.settle` resolves any overlap, taking items from the top down so the higher one
   keeps its place and the other moves to the nearest opening. Anything with no room goes to the
   next page with room, as today.

A layout with free placement on keeps its offsets and part rows; only what now overlaps moves, by
as little as it can (an icon that was tucked onto the last step of a widget moves one step down).
Free placement left switched on by someone whose Plus has ended is not in effect: their layout goes
onto whole rows too, but only once Play has confirmed that nothing is bought, so a subscriber who
is merely offline never has an arrangement undone. A layout already at version 2 is never run
through the older repairs again.

### Tests

- `HomeGeometryTest` (JVM): for every grid from 3 to 6 columns and 4 to 7 rows, at 2.0, 2.625 and
  3.5 px per dp, labels on and off: a widget's left and right equal the icon edges of its first and
  last column; its top equals the icon top of its first row; its bottom equals the label bottom of
  its last row; neighbouring cells share edges; dock slots equal the column centres when the counts
  match; the header inset equals the first icon's left edge.
- `DropPlannerTest` (JVM): updated for whole rows; the sweep over every drop point still passes.
- `LayoutTidyTest` (JVM): a tucked icon, icons under a half-row search bar, a widget one and a half
  rows tall, a widget on a quarter step, and a free-placement layout that must come back unchanged.

---

## Stage 2: wallpapers that follow the theme

### What Zenith remembers

New `ZenithSettings` fields:

| Field | Default | Meaning |
| --- | --- | --- |
| `wallpaperStyle` | `""` | The `WallStyle` in use, or empty when the wallpaper is a photo or was set elsewhere |
| `wallpaperPalette` | `""` | Empty: use the theme's palette. Otherwise the palette the wallpaper is pinned to |
| `wallpaperLock` | `false` | Also set the lock screen |
| `wallpaperFollow` | `true` | Repaint when the theme changes |

Setting a design from the wallpaper sheet fills these in. Setting it in another palette's colours
without switching Zenith to that palette pins `wallpaperPalette`. Picking a photo, or Android
reporting wallpaper colours Zenith did not just set, clears `wallpaperStyle`, so a wallpaper chosen
elsewhere is never replaced.

People upgrading have an empty `wallpaperStyle`; their wallpaper starts following once they pick a
Zenith design again. The wallpaper sheet says so in one line while it is empty.

### Light and dark for every palette

`ZenithPalette.toned(dark: Boolean)` returns the palette for that appearance. When it differs from
the palette's own, the backdrop is derived from the palette's own accents (a near-white or
near-black base mixed with a little accent) instead of borrowing Midnight's or Frost's. `ZenithTheme`
and the wallpaper colours both use it, so the interface and the wallpaper always agree.

`WallColors` gains `dark: Boolean`. Each design draws a real light version: a light backdrop,
accents deepened for contrast, white stars and highlights replaced by soft tinted marks, and
darkening washes turned into lightening ones.

### Designs

Twenty-two designs in four groups. New ones are marked; `Plus` needs Zenith Plus.

| Group | Designs |
| --- | --- |
| Soft | Glow, Calm, Mesh (new), Drift (new), Silk (new, Plus), Halo (new, Plus) |
| Lines | Waves, Orbit, Contour (new), Ripple (new), Grid (new), Ribbons (new, Plus) |
| Shapes | Facets, Bands, Bubbles (new), Prism (new, Plus), Bloom (new, Plus) |
| Scenes | Dunes, Aurora, Peaks (new), Horizon (new, Plus), Nebula (new, Plus) |

- Mesh: four or five soft pools of colour blended together.
- Drift: diagonal streaks of blurred light.
- Contour: map contour lines over a soft gradient.
- Ripple: rings spreading from a point off centre, fading out.
- Grid: a grid in perspective running to a glowing horizon.
- Bubbles: clear glass orbs of different sizes with bright rims.
- Peaks: layered mountain ridges fading into haze.
- Silk: smooth folded bands shaded like cloth.
- Halo: one large ring of light with a soft corona.
- Ribbons: wide see-through ribbons crossing in curves.
- Prism: large angled shards of glass with gradient faces.
- Bloom: petals spreading from a point low on the screen.
- Horizon: a sun in light, a moon in dark, over layered haze and water.
- Nebula: soft clouds of colour, with stars in dark and pale wisps in light.

Every design draws in any palette, light and dark, and is the same picture as a thumbnail and at
full size. `WallpaperArt.kt` keeps the engine and helpers; the designs move to `WallDesigns.kt` so
neither file grows past a few hundred lines. Smooth gradient layers that need per-pixel maths are
drawn small and scaled up; crisp shapes and the grain go on top at full size.

A locked design shows a lock on its thumbnail; its preview offers "Unlock with Plus" in place of
"Set wallpaper". A Plus design already in use stays, and keeps following the theme, if Plus ends.

### Following the theme: `WallpaperDirector`

One class owned by `ZenithViewModel`.

- It watches the chosen design, the palette in effect, light or dark, and the screen size, and
  builds a key from them. Light or dark comes from the appearance setting and, for Auto, from the
  system night mode, read through `ComponentCallbacks` on the application so it is right even when
  the home screen is not showing.
- When the key differs from the one last applied, `wallpaperFollow` is on, `wallpaperStyle` is set
  and Zenith's kept copy is still the wallpaper on screen, it paints the new picture off the main
  thread and sets it. Changes are held for 200 ms and a newer change cancels an older one that is
  still painting, so tapping through palettes does not queue up wallpapers.
- With the home screen showing: the new picture fades in over 700 ms as a layer behind the home
  screen, drawn with the same zoom and scroll Android uses, while the interface colours make their
  existing change. Glass panes switch to the new picture halfway. Only when the fade is complete is
  the system wallpaper set; once Android confirms it, the layer fades out over what is by then the
  same picture.
- With the home screen not showing: it sets the wallpaper straight away.
- If Android refuses the wallpaper, the layer fades back out and nothing is recorded as applied.

### Wallpaper sheet

- Group chips (All, Soft, Lines, Shapes, Scenes) above the existing palette chips.
- Thumbnails show the appearance in use. The preview shows the light and dark versions side by side.
- A "Follows your theme" switch at the top.
- After "Set wallpaper" the sheet closes and the wallpaper fades in behind it; no toast on success.

### Risk: Android restarting the home screen

On Android 12 and later, a wallpaper change can make Android rebuild its own colours and restart
every visible activity. Because the system wallpaper is only set after the fade has finished, the
worst case is the home screen's contents blinking once over a wallpaper that is already correct.
This is checked on the API 37 emulator in this stage; if the restart happens, the home screen's
contents fade in over 150 ms after such a restart instead of popping.

### Tests

- `PaletteToneTest` (JVM): every palette has a light and a dark tone; toning to its own appearance
  returns it unchanged; a light backdrop is light and a dark one dark.
- `WallpaperKeyTest` (JVM): the key changes with design, palette colours, light or dark and size,
  and with nothing else; the director's "should repaint" rule for follow off, empty style, pinned
  palette and a wallpaper changed elsewhere.
- `WallpaperArtTest` (on device): every design, in every palette, light and dark, paints without
  error; its light and dark versions differ; the lower 45% is light enough for dark labels in light
  and dark enough for white labels in dark; a full-size paint takes under 1.5 s.

---

## Stage 3: Settings

### Shape

- Settings opens as a full-height page over the home screen, with a large title that shrinks as you
  scroll and a search box under it. It lives in a new package, `ui/settings/`.
- The first page lists the groups, each with a coloured icon and a line saying how it is set now.
  A group opens with a push to the side; back, the arrow or a swipe from the edge pops it.
- Look and Home screen pages start with a live mini phone (the one onboarding already has in
  `PhonePreview.kt`) that shows the change as you make it.
- Search matches titles and keywords from a `SettingsCatalog`, a plain list with one entry per
  option. Choosing a result opens its page and pulses the row.

### Groups

| Group | Contents |
| --- | --- |
| Wallpaper & theme | Colour (a grid of every palette you can use, and "Make your own" for the Plus theme maker), Auto / Light / Dark, Wallpaper (opens the wallpaper sheet), Wallpaper follows theme, Glass, Tilt shine |
| Home screen | Grid, Icon shape, Icon pack, Labels, Dock, Search bar, Header, Free placement (Plus), Tidy up |
| Motion & feel | Animations (Full / Reduced), Opening apps (Colour / Liquid glass), Page swipe (Depth / Slide / Cube / Stack), Haptics |
| Apps | Swipe up for (App drawer / App Library), Hidden apps (Plus) |
| Zenith Plus | Opens the existing Plus sheet |
| Aura | Opens the existing Aura sheet |
| Backup & reset | Back up (Plus), Restore (Plus), Run setup again |
| About & help | Default home app, Keep Zenith loaded, Help, Version, Privacy policy |

"Tidy up" runs `LayoutBuilder.settle` on the layout and reports how many items moved.

### New settings fields

| Field | Default | Values |
| --- | --- | --- |
| `motion` | `full` | `full`, `reduced` |
| `pageEffect` | `depth` | `depth`, `slide`, `cube`, `stack` |
| `haptics` | `true` | |

`searchBar` and `header` arrive in stage 1 and the wallpaper fields in stage 2. Every new field is
written to and read from the settings JSON with its default, so old backups restore cleanly.

### Parts

`ui/settings/SettingsKit.kt` holds the rows, groups, switches, sliders and choice controls that
are spread across `SettingsSheet.kt` and the onboarding kit today, in one version each. The palette
picker becomes a wrapping grid, so nothing is cut off at the edge. Every control animates its own
change and gives a tick when haptics are on. `SettingsSheet.kt` is removed once nothing uses it.

### Tests

- `SettingsCatalogTest` (JVM): ids are unique; every entry names a page that exists; every page has
  at least one entry; search finds each entry by its title.
- `SettingsJsonTest` (JVM): a settings object survives a round trip; JSON from before this work
  reads with the new defaults.

---

## Stage 4: motion

### Foundation

- `Motion` gains named specs for press, lift, drop, open, close, sheet, page and theme change, and
  the existing ones stay. New code and the code touched in this stage use the names.
- `LocalMotionLevel` is `Reduced` when the `motion` setting says so or the phone's "Remove
  animations" is on, otherwise `Full`. Reduced means: no fly-in, no jiggle, no parallax, page swipe
  forced to Slide, and springs replaced by fades of 150 ms or less.
- One `Haptics` helper maps events to feedback and does nothing when the `haptics` setting is off.

### What moves

| Moment | Behaviour |
| --- | --- |
| Unlock and cold start | Items start further from the centre of the screen and spring into place, those nearest the centre first, inside 600 ms |
| Coming back from an app | The app lands in its icon as today; the other items settle with a short stagger |
| Page swipe | Depth (today's), Slide, Cube (pages turn about their shared edge), Stack (the page you leave shrinks and fades under the next) |
| Page dots | The active pill stretches towards the next dot as the page moves |
| Picking up | The item lifts and gains a shadow; the others jiggle about 1.2 degrees, each out of step, until it is put down |
| Carrying | A tick as the landing spot changes |
| Dropping | The item settles with a ring that spreads and fades |
| Folders | A folder grows from its icon into a glass panel in the middle of the screen while the home screen dims and blurs, and shrinks back into its icon. It replaces the folder bottom sheet; renaming and taking an app out work as today |
| Widgets | Clock digits roll; the battery ring springs to its value and shimmers while charging; toggles change colour and shape; today's ring on the calendar draws in |
| App drawer | Rows rise and fade in, one after another, as it opens |
| Sheets and settings | Contents stagger in; pages push and pop on springs |
| Wallpapers | A thumbnail grows into its preview |
| Theme change | Colours and wallpaper change together (stage 2) |

Page swipe styles are pure functions from a page's offset to scale, alpha, rotation and shift, so
they are tested without a device.

### Haptics

Pick up: long press. Landing spot changes: tick. Drop: confirm. Folder made: confirm. Switch or
choice changed: tick. Slider step: tick. Nothing on page turns or sheets.

### Keeping it smooth

- Movement goes through `graphicsLayer` or draw-time state, never through layout.
- Jiggle uses one shared clock for the page, not one animation per item.
- No new per-frame allocation and no per-frame rebuild of a blur or shader.
- Checked on the Android 10 emulator, where glass falls back to frosted panes.

### Stretch

If it runs smoothly: the wallpaper zooms out as the drawer opens, through
`WallpaperManager.setWallpaperZoomOut` (Android 11 and later). Dropped without replacement if it
upsets the alignment between the wallpaper and the glass.

### Tests

- `PageEffectTest` (JVM): each style at offsets -1, -0.5, 0, 0.5 and 1; every style is the identity
  at 0.
- `MotionLevelTest` (JVM): the level for each mix of setting and system switch.
- On the emulators: each row of the table above is exercised by hand with screenshots, in Full and
  in Reduced.

---

## Checking each stage

- `JAVA_HOME` is Android Studio's `jbr`; run `gradlew :app:testDebugUnitTest :app:assembleDebug`.
- On-device tests run with `adb shell am instrument`, never `connectedDebugAndroidTest`, which
  uninstalls the app.
- Hands-on checks happen on the `Android10_test` emulator. The finished stage is installed on
  `Pixel_10a` last, for Gabriel to try.

## Risks

| Risk | Answer |
| --- | --- |
| A placement change brings back "it lands somewhere else" | `DropPlannerTest` sweeps every drop point; a resting drag is checked on the emulator |
| The one-time tidy moves things people placed on purpose | Nearest whole row only, top item keeps its place, fixtures in `LayoutTidyTest`; free-placement layouts are untouched |
| Android restarts the home screen after a wallpaper change | The wallpaper is set after the fade, so the worst case is one blink; checked on API 37 |
| Painting a full-size wallpaper is slow on old phones | Gradient layers are drawn small and scaled up; a time limit in `WallpaperArtTest` |
| A usage-limit pause mid-stage | Stages are committed one at a time, and each plan is split into small tasks |
