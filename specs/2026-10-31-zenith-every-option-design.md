# Zenith, every option: nine tracks

- Date: 2026-10-31
- Branch: `overhaul`
- Status: Gabriel asked for Zenith to be "the best launcher ever and the most customisable", the
  same as before but better, and chose all nine tracks of the brainstorm in order: "JUST DO
  ABCDEFGHI". No design was reviewed track by track. The decisions below were made while building,
  and any of them can be changed.

## Goal

Keep what Zenith is — liquid glass that is a copy of your own wallpaper, springs that start from
where things were last seen, menus under the thumb — and close the gap to Nova, Niagara and a
Pixel: everything around the apps themselves, and far more that can be set.

Every track is done when:

1. Unit tests and `:app:assembleDebug` pass.
2. Every new action has a way in and a way out that moves, in the vocabulary of `Motion.kt`, and
   is turned down to quick fades by Animations: Reduced.
3. Every new option is in `SettingsCatalog` and found by search.
4. Old settings and backups still read: every new field has a default and is optional on the way in.
5. README and `play/listing.md` say what is new; the privacy policy changes wherever data does.

## Already there (not built again)

- Wallpaper parallax across pages: `HomeScreen` sets `setWallpaperOffsets` as pages turn.
- Legacy icons are already cut to the chosen shape (`IconLoader.render` gives them a tile). The
  README's known limit saying otherwise is out of date and is removed.

## Plus

The rule stays: nothing that is free becomes Plus. New in Plus: **Looks** (saving, switching,
scheduling and sharing them; opening a Look somebody sent you is free), **Glass studio** (the fine
glass dials) and **palettes by time of day**. Everything else in these nine tracks is free.

---

## A. Every icon, yours

### Edit an app
Long-press an app (on a page, in the dock, in a folder or in the drawer) → **Edit**. A sheet with
the icon large at the top, and:

- **Name** — a text field; empty puts the app's own name back.
- **Icon** — the app's own, any drawable from any installed icon pack (a searchable grid, the
  pack's suggestion for this app first), or a photo (Android's photo picker, cropped square and
  kept in Zenith's files). The chosen icon is still cut to the icon shape.
- **Hide name** — this icon only.
- **Reset** — all three back.

Stored per app, not per icon, in `appEdits: Map<key, AppEdit>`, so the drawer, dock, folders and
search all agree. `IconLoader` consults it before the pack. A photo icon is a file under
`files/icons/`; backups carry the name and the pack choice but not the photo.

### App shortcuts
`LauncherApps.getShortcuts` (dynamic, manifest and pinned) once Zenith is the home app, listed at
the top of every app's long-press sheet, up to five, each with its own icon. Tap runs it
(`startShortcut`). **Add to home** puts it on the page as a `ShortcutCell` (an icon of its own with
the app's icon in its corner) and pins it, so the app keeps it. Zenith also answers
`CONFIRM_PIN_SHORTCUT` and `CONFIRM_PIN_APPWIDGET` (Track C): Chrome's "Add to home screen" and
any app's "Add shortcut" land on the first free spot, and the page turns to show it.

### Icon style
`iconStyle`: **Original** (as now), **Tinted** (the app's one-colour glyph in the accent on a tile
of the palette's tint — Android 13's themed icons), **Glass** (the glyph in white on a tile of clear
glass that bends the wallpaper — iOS's clear icons, Zenith's own way). The glyph is the adaptive
icon's monochrome layer (Android 13+); without one, its foreground layer as a silhouette when that
layer is mostly transparent; otherwise the brightest parts of the whole icon. Folders take the
style too. Glass tiles are drawn by the same `Modifier.glass` as a folder.

### Notification dots
A `NotificationListenerService` (`BadgeService`) counts what each app has showing. Off until
notification access is given; Settings → Home screen → **Notification dots** asks with one line
of why and opens Android's page. `badges`: Off / Dot / Count. A dot is a small glass bead in the
accent, at the icon's top right; a folder shows the sum of its apps. A dot springs in when the
first notification arrives and shrinks away when the last one goes. The same listener is what
Track D's Now playing reads.

### Shapes and labels
- Shapes: Squircle, Circle, Rounded, Square, and new **Pebble**, **Teardrop**, **Hexagon**,
  **Clover**. One function per shape in `IconShapes`, used by the renderer and by Compose.
- `labelSize`: Small 10sp / Medium 11sp (now) / Large 12.5sp, inside the same label strip.
- `labelShadow`: a soft shadow under labels, for busy wallpapers.

### Decided while building
- Over the drawer, in a folder's pane and in an App Library card, a Glass icon gets a pale plate
  rather than a pane of its own: glass inside glass would show the sharp wallpaper through a hole.
- A renamed app takes its place in the drawer by its new name (`AppEdits.relabel`). A name is kept
  as typed and only trimmed where it is shown, or a space could never be typed between two words.
- Hide name is for the home screen only; the drawer always names its apps.
- Shortcuts are always one cell, cannot go in a folder or the dock, and cannot be resized.
- Edit is in an app's menu on a page, in the dock and in the drawer; not inside an open folder,
  where a long press already takes the app out.

### Tests
`AppEditsTest` (JSON, reset, names as typed, photos left out of a portable copy),
`ShortcutCellTest` (JSON round trip, keys), `IconLookTest` (dots counted, folder sums, every
choice named, eight shapes), `SettingsJsonTest` grows the new fields.

---

## B. Gestures

Settings → **Gestures**, a page of its own. Every gesture has a row; tapping it opens a sheet of
actions, the current one ticked.

| Gesture | Default |
| --- | --- |
| Swipe down | Notifications |
| Two-finger swipe down | Quick settings |
| Two-finger swipe up | Nothing |
| Double tap | Nothing (Lock screen once the helper is on) |
| Pinch in | Customise (as now) |
| Pinch out | Page overview (Track H) |
| Home button while home | First page (as now) |

Swipe up stays the apps (drawer or library); holding an empty spot stays Customise, so Settings
can always be reached.

Actions: Nothing, Notifications, Quick settings, Lock screen, App drawer, Search, an app, an app
shortcut, Torch, Bedside clock, Customise, Settings, Widgets, First page, Page overview.

- Notifications and Quick settings use `StatusBarManager.expandNotificationsPanel` /
  `expandSettingsPanel` (permission `EXPAND_STATUS_BAR`); where that is refused, the accessibility
  helper's global action if it is on.
- **Lock screen** needs the accessibility helper (`LockService`, global action only, no events read,
  `isAccessibilityTool` false). Before Android's page opens, a sheet says exactly what it is for and
  that it reads nothing. Play needs the Accessibility declaration in the console for this: noted in
  RELEASING.md.
- `gestures: Map<gestureId, actionId>`; an app is `app:<key>`, a shortcut `shortcut:<pkg>/<id>`.
  Rules in `GestureMap` (defaults, reading, what each action needs) with a test.
- Motion: a swipe down pulls the page down a little under the finger before the shade comes; a
  double tap sends a ring out from the finger; anything that needs a missing permission shakes and
  says why once.

Decided while building: the rules are `Gestures`/`GestureActions` (no separate GestureMap). A drag
on the page is told apart by its first movement: down with the drawer shut is the swipe-down
gesture, anything else the drawer as before, and a second finger takes the drag out of both. Home
counts as the gesture only when Zenith had the window focus and the intent was not a bring-to-front,
which is how Launcher3 tells "already home" from "coming home". The torch became one `Torch` object
the widget shares, so it stays right whoever turns the torch on.

Tests: `GesturesTest`.

---

## C. Real Android widgets

- `ZenithWidgetHost` (`AppWidgetHost`, one host id), listening while the activity is started.
- The gallery gains **From your apps**: every provider, grouped by app, with its preview image
  (`previewImage`, or its icon), and the size it wants. Adding allocates an id, binds it
  (`bindAppWidgetIdIfAllowed`, else Android's bind dialog), runs the widget's configure activity if
  it has one, then places a `HostedWidgetCell` at the first spot with room.
- On the page it is an `AppWidgetHostView` in the widget's frame, told its size in dp
  (`updateAppWidgetSize`), with an optional pane of glass behind it (**Glass behind** in its menu,
  on by default). A long press on it picks it up like any widget: the host view watches for the
  hold itself and hands it to Compose.
- Resizing uses the provider's minimum and maximum (`minResizeWidth/Height`, `maxResizeWidth/Height`
  where given) and the same corner pull and size bar.
- Removing it deletes the id. Restoring a backup on another phone puts a placeholder ("Tap to add
  Spotify's widget again") where it stood.
- **Widget stacks**: drop a widget on another widget of the same size to stack them. A `StackCell`
  holds Zenith widget types and hosted ids, pages vertically with small dots inside the pane, and
  its menu can take a widget out or reorder. A stack of one becomes that widget.

Decided while building: glass behind another app's widget is **off** by default (most bring their
own background, and a pane round an opaque one only shows as a frame). Stacks are made from a
widget's menu ("Stack another widget here", which opens the gallery in a stacking mode) rather
than by dropping one widget on another, which would have meant changing the drop planner. A
stack's members keep the id they had as widgets. Widget numbers nothing holds are given back to
Android at start-up and after a restore.

Tests: `HostedWidgetsTest` (JSON, a stack read back with one left, stacking and unstacking, the
most a stack holds, sizes from dp and from Android 12's cells).

---

## D. More Zenith widgets

Each in the gallery with Glass and No glass where it makes sense, every size, drawn by the code
that draws it on the page.

| Widget | What it shows | Needs |
| --- | --- | --- |
| Now playing | Art, title, artist, play / pause / skip, progress | Notification access (A) |
| World clock | One to four cities, day or night each | Nothing |
| Countdown | Days (and hours near the end) to a date you name | Nothing |
| Photo frame | A photo you pick, slowly crossing to the next if you picked several | Photo picker, no permission |
| To-do | A short checklist; ticked items strike through and drop to the bottom | Nothing |
| Aura | Today's streak, the ladder, aura in hand | Nothing |
| Weather | Now, high and low, the next hours | Off until a city is chosen; sends the city's position to Open-Meteo |

- Per-widget settings (`widgetData: Map<widgetId, json>`) — the countdown's date and name, the
  clocks' cities, the photos, the list — set from the widget's own menu.
- Weather is the one thing that sends anything without an account: a place's latitude and
  longitude to `api.open-meteo.com`, every 30 minutes while the home screen is shown. Off until a
  city is searched for and chosen; the privacy policy and Data safety answers say so.
- **At a glance** becomes a choice for the top of the page: the date, the next event (calendar, if
  allowed), what is playing, and the weather if it is on.
- Dropped: headphone battery. Android only gives it through a hidden call.

Decided while building: what each widget needs is kept once for all widgets of its kind (the
cities, the date, the photos, the list, the place), as the note and the toggles already are, not
per widget; a `widgetData` map can come later if two different countdowns are ever wanted. The
photo frame moves on with each return to the home screen rather than on a timer, so nothing keeps
running by itself. Widgets read your settings through one `WidgetWorld` the home screen provides.
At a glance moves to Track I with the other headers.

Tests: `WidgetRulesTest` (the to-do order, cities and their offsets, countdown sums and ring),
`WeatherParseTest` (real Open-Meteo answers, places, sky kinds, degrees), and `WidgetFormTest` now
checks the new kinds say something different in every shape.

---

## E. Drawer and search

- `drawerSort`: A–Z, Most used (`launchCounts`), Newest, By colour (the icon's main hue).
- `drawerColumns`: Auto, or 3 to 6.
- A fast scroller on the right edge: drag it and a glass lens follows the finger with the letter
  in it, the rows under it magnified.
- Tabs you make (name + apps), shown as a row of pills over the list; plus **Work** when the phone
  has a work profile (opened in the work profile; not placed on the home screen in this round).
- Search finds more than apps: a sum (`2+2*3`, `15% of 80`), a conversion (`5 km in miles`,
  `30 c to f`), Android settings by name ("bluetooth", "battery"), app shortcuts, and contacts if
  allowed (asked the first time a search could use them). Each kind is a section, results rise in.

Decided while building: **contacts are left out** of search for now: they would add a sensitive
permission and a Data safety entry for a small gain; everything else here needs no permission.
The fast scroller's lens shows the letter and the letters swell round the thumb; the rows
themselves are not magnified. Tabs are made from the drawer (+, and a hold to change one), which
is where they are used; Settings says so. Work apps are only in the Work tab and search.

Tests: `DrawerRulesTest` (the orders, colour bands, counting, tabs, work keys, sums, conversions,
settings by name).

---

## F. Looks (Plus)

- A **Look** is everything you see: palette, light and dark, glass, wallpaper design, icons, grid,
  dock, layout and motion. Not your name, notes, lists, hidden apps, PIN, Aura or Plus.
- Settings → **Looks**: save the current one (named), switch (the home screen and wallpaper
  cross-fade, then the page flies in), rename, delete.
- **Schedule**: a Look for a time of day on chosen days; Zenith checks on every return home and
  every minute while shown, and switches only at the edge of a slot, never under your finger.
- **Share**: with an account, a Look is sent to the server and gets a code (`LOOK-XXXXXX`); anyone
  can open a code (no account) and apply it. Apps they do not have are left out; a Plus wallpaper
  they cannot use falls back to the nearest free design. **Featured**: Looks marked featured on
  the server (by Gabriel, not by users), so there is no public upload to moderate.

### Decided while building

- What a Look keeps is a list of setting names (`Looks.KEYS`); putting one on changes nothing
  outside it, so a Look written by hand cannot give Plus or Aura. `layoutVersion` goes with the
  layout, so an old Look is brought up to date the way an old backup is.
- A Plus wallpaper without Plus keeps the wallpaper there is (there is no "nearest" free design).
- The schedule looks every 30 seconds while the home screen shows and at once on coming home; what
  it has due waits until nothing has been touched for 1.5 seconds. Each slot switches once
  (`lookOccasion`), so a Look chosen by hand mid-slot stays.
- A shared Look keeps its code. **Stop sharing** takes it back; deleting the Look or the account
  does too; saved over since, it offers **Share how it is now** (a new code, the old one taken back).
- **Featured** is asked for only when tapped, and a code only when typed: with no account, nothing
  goes to the server unless asked. A featured Look opens in the code sheet, so nothing replaces the
  home screen without a second tap.
- Looks go in backups, file and account; restoring adds the backup's to the phone's.
- Photos chosen as icons, and other apps' widgets, are kept while any saved Look still uses them.
- The server keeps an account's twenty newest Looks (featured ones always), 256 KB each, shared at
  most 30 an hour per account; opening is limited to 60 a minute per address, against guessing.

Tests: `LooksTest` (what a Look keeps and leaves out, sharing, other phones, the schedule, backups),
the routes in `ZenithServerTest` against the real server, and the Looks part of
`server/test_zenith_server.py`.

---

## G. Theme and glass studio

- **Font**: System, and a few bundled open fonts (SIL Open Font Licence, listed under About), for
  the whole launcher; the clock and At a glance can have their own.
- **From your wallpaper**: a palette worked out from the wallpaper Zenith has a copy of (main
  colours by a small quantiser, `WallpaperPalette`), so a photo gets a theme to match. Updates when
  the wallpaper does.
- **Palettes by time of day** (Plus): dawn, day, dusk and night, each a palette; changes with the
  same fade as now.
- **Glass studio** (Plus): bend, frost, rim light, light direction (fixed angle or tilt) and tint,
  each a slider over a live pane, with Reset.
- Each widget's own accent, from its menu.

### Decided while building

- Fonts: Inter and JetBrains Mono are carried (taken unchanged from the copies Android Studio
  ships, their OFL notices read from the fonts themselves); the others are the phone's own by name
  (`sans-serif-condensed`, `serif-monospace`, `casual`, `cursive`, `sans-serif-smallcaps`). "Thin"
  for numbers is a weight, not a font: Android gives a device font the weight asked for.
- The clock font is **Numbers**, for every big number, not the clock alone; At a glance waits for I.
- The wallpaper palette is read only from a wallpaper that is not one of Zenith's designs, from the
  frosted copy the glass already has, or `WallpaperManager.getWallpaperColors` without one.
- Day parts are fixed: dawn 05:00, day 08:00, dusk 17:00, night 20:00. Each switches once
  (`paletteOccasion`), on the same thirty-second look as the Looks schedule.
- The Glass studio's light is an angle, or null for Zenith's own, so the defaults are exactly the
  glass as it was. "Light follows tilt" off stills both the light and the shader's lean.
- A widget's own colour is the accent; the second accent is forty degrees round the wheel from it.

Tests: `WallpaperPaletteTest`, `DayPartsTest`, `GlassTuningTest` (JSON, clamps, the defaults change
nothing, the light), and the widget colour in `HostedWidgetsTest`.

---

## H. Motion 2.0

- **Page overview** (pinch out, or the gesture of your choice): every page shrinks onto a tray of
  glass in a row; drag one to reorder, flick it up to remove it (an empty page goes at once, a full
  one asks), tap to go there.
- **Carry many**: while holding one icon, tap others; each flies into a stack under your finger
  with a count, and they land together, side by side from where you let go.
- Page turns: **Carousel**, **Wheel**, **Flip**, **Fold**, beside Depth, Slide, Cube and Stack.
- How the drawer arrives: **Rise** (now), **Grow** from the finger, **Glass** (a pane that clears).
- How the home screen arrives: **From the middle** (now), **From below**, **Cascade** by column.
- Press: **Bounce** (now), **Squish**, **Ripple** of light from the finger.
- **Undo**: anything taken off the home screen leaves a glass pill for five seconds that puts it
  back where it was, springing out of the spot it left.

### Decided while building

- Page turns gained `rotationZ`, `pivotY` and `squash`; every turn is the plain page at rest.
- Stack sinks the page the swipe started on (`settledPage`), not the page on the left, and the
  page coming in is drawn over it. Before, going back sank the page you were going to under the
  one you left (found on the emulator, 2026-11-01).
- Arrivals: "below" sets off from the foot of the page first, "cascade" from the left column; the
  header and dock rise or drop with them.
- Drawer "Grow" grows from the foot of the screen (not the exact finger); "Glass" fades in where it
  is. Both put the drawer off screen once shut, so it never catches a touch on the home screen.
- Press "Ripple" draws its light with `SrcAtop` on an offscreen layer, only while it runs.
- Undo holds a list: what was thrown away together comes back together. It is cleared, and what
  it held let go, when a Look or backup replaces the home screen.
- The page overview moves a page on letting go, not while sliding, so the gesture stays with one
  tile. Removing a page with things on it asks first.
- Carry many: only plain app icons, up to fifteen, onto a carried plain app icon; into the dock or
  a folder only the carried one goes.

Tests: `PageEffectTest` (eight turns), `FlyInTest` (the arrivals), `PageOrderTest`, `UndoTest`,
`CarryManyTest`.

---

## I. Home screen details

- `dockStyle`: Glass bar (now), Floating (a shorter pill that hugs its icons), None (icons only).
  `dockLabels`. A second dock page, swiped sideways inside the dock.
- `indicator`: Dots (now), Line, Numbers, None.
- `header`: Name, Greeting, Nothing, and new **Clock**, **Date**, **At a glance** (D).
- **Lock layout**: nothing can be moved, removed or resized; menus say it is locked and offer to
  unlock.
- **Hide status bar**.
- Folders: **Grid** (now), **Stack** (the first three fanned), **Cover** (the first app large), and a
  colour per folder from its menu.

### Decided while building

- Left out for now: **names under dock icons** and **a second dock page**. The dock's height comes
  from the grid and its drag and reorder code places icons by column; both need that reworked, not
  added to, and are better done on a device where they can be seen.
- Floating dock: the glass is shaped to the icons; the layout and the drag are the bar's own.
- Indicator "none" hides the add-page button too; pages are added from the page overview.
- The header clock is 24-hour, as the clock widget is.
- A folder's colour is laid in its glass at 30% and tints the glass; it eases in and out.
- Lock layout guards every hold: page items, other apps' widgets, stacks and the dock. Adding from
  the gallery and the drawer is still allowed.
- Hide the status bar uses Android's transient bars, so it comes in when swiped from the top.

Tests: `HomeHeaderTest` (the new kinds, the date, the weather line), the folder's colour in
`HostedWidgetsTest`, and the settings fixture.

---

## J. The island (asked for on 2026-11-01)

The camera, stretched, over every app (Plus), with a Settings page of its own. The user chose
"everywhere, over apps" over "home screen only", knowing it needs "Display over other apps".

### Redone the same day (third version)

The user's words on the second: it did not allow for the camera in the middle, it popped out of
nowhere, and it "looks ass"; they asked for one like Apple's, that shows the app's logo for a
notification and opens to read it, "100 times better, simple, something you'd want to pay for". So:

- Measured, not set: the camera's size and place from the display cutout, the status bar from the
  insets (`IslandGeometry`). Height = camera + 3 dp, concentric with it; icons in the middle of the
  rounded ends; the middle kept clear for the lens; at rest exactly the camera. Measured on the
  A55: camera 23.5 dp at (192, 19.9), status bar 31.6 dp, 384 dp wide.
- No look, width, position or "grows" settings any more; the page is what it shows, "Always show
  it", and keeping out of the way. An old "small pill when idle" carries over as "Always show it".
- Charging only as a moment (3 s) as the charger goes in, not a pill that stays.
- New: calls and timers, from ongoing notifications with Android's chronometer, or a time in their
  words; green for a call, orange for anything else.
- Tap opens a card under the status bar joined to the island by a neck (one shape of black): music
  with its controls, a running clock large, or a notification to read. No buttons on the read card:
  a tap on it opens the notification, anywhere else closes it.
- The music's colour comes from its cover; the type is the phone's sans; it grows to at most 46%
  of the screen so the clock and status icons stay clear.

### Decided while building

- A `TYPE_APPLICATION_OVERLAY` window from a plain service, not a foreground service: an app with a
  visible overlay window is kept running by Android, so there is no notification and no Play
  special-use declaration. Zenith starts and stops it (`IslandService.sync`) from `MainActivity`.
- Not the accessibility overlay, though it would sit above the status bar: Play allows the
  accessibility API for accessibility, and the lock helper's declaration is for locking only. So the
  island is drawn under the status bar's own window. Checked on the A55: a tap on the island went
  to the status bar, one just under it opened the card, so a tappable island takes taps on a 14 dp
  strip under it.
- Checked on the A55: Android draws a window over other apps that takes no touches at most 80%
  solid (`alpha=0.8` in `dumpsys window`), and the wallpaper showed through. So the island's window
  takes touches whenever it is in sight; inside the status bar that takes nothing from the app.
- Android's own Settings hides other apps' windows over it; the island is not there, as intended.
- What shows: the newest moment (notification 5 s, ringer 2.5 s, charger in 3 s), else a running
  clock, else music while it plays, else a small pill if asked, else nothing (`IslandRules`).
- Which notifications: what Android would alert you to. Not importance below default, not held back
  by Do Not Disturb, not ongoing, a foreground service's, a group summary, of the service, progress,
  transport or system kinds, or an update to one showing that does not alert again (KWGT's silent
  "Service is running" came through before this). A chat's newest message from its MessagingStyle.
- Full screen is told by the status bar being hidden in the window's insets. Sideways by the
  orientation. A notification from the app playing the music is not news.
- The music bars move by themselves while music plays: the one exception to nothing moving on its
  own. They rest when it stops, and with movement reduced.

Tests: `IslandTest`, and the island in the settings fixture.

## Checking

- `JAVA_HOME` is Android Studio's `jbr`; `gradlew :app:testDebugUnitTest :app:assembleDebug`
  after every track.
- On a device: the `Pixel_10a` emulator only when the PC has room for it (see the memory notes);
  otherwise the Galaxy A55, with the debug build beside the Play one.

## Progress

- [x] A. Every icon, yours (built and unit-tested; not yet looked at on a device)
- [x] B. Gestures (built and unit-tested; not yet looked at on a device; Page overview waits for H)
- [x] C. Real Android widgets (built and unit-tested; not yet looked at on a device)
- [x] D. More Zenith widgets (built and unit-tested; not yet looked at on a device)
- [x] E. Drawer and search (built and unit-tested; not yet looked at on a device)
- [x] F. Looks (built and unit-tested, the server's routes too; not yet looked at on a device;
  the server on the host needs the new `zenith_server.py` before sharing works there)
- [x] G. Theme and glass studio (built and unit-tested; not yet looked at on a device)
- [x] H. Motion 2.0 (built and unit-tested; not yet looked at on a device)
- [x] I. Home screen details (built and unit-tested; not yet looked at on a device; dock names and a second dock page left out — see I)
- [x] J. The island (built and unit-tested, the third version; looked at on the user's Galaxy A55
  over a dark app and a light one, and its Settings page with the preview; music and calls only in
  the preview)

## Risks

| Risk | Answer |
| --- | --- |
| Play rejects the accessibility helper | It is optional, only for Lock screen; the declaration and disclosure are in place. Without it everything else works |
| Hosted widgets fight Compose for touches | The host view handles the long press itself, as Launcher3's does; checked on a device |
| Weather breaks "nothing leaves your phone" | Off until a city is chosen; the policy says exactly what is sent and where |
| Glass icons cost frames on old phones | Glass tiles are the folder's pane; on Android 12 and below they fall back to frosted tiles |
| Nine tracks in one go leave half-done work | Tracks are built in order and each is finished, tested and checked off before the next |
