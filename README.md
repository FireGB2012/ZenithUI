# Zenith UI

A launcher for Android that puts iOS-style liquid glass on top of Android's customisability —
built entirely in Jetpack Compose.

## First run

1. **Welcome** — animated glass mark, the four pillars, and a name field. What you type here is
   used across the launcher ("Good afternoon, Gabriel").
2. **Palette** — six palettes (Midnight, Aurora, Sunset, Frost, Sakura, Graphite), light/dark/auto,
   and a Glass slider. A live phone mock updates as you choose.
3. **Widgets & grid** — pick the widgets for page one, the grid density (Compact 5×6, Balanced 4×5,
   Roomy 4×4) and the icon shape.
4. **Setup** — a 20-second progress ring. It is not a fake wait: while it fills, the launcher reads
   every launchable activity, picks a dock from your dialer/messenger/browser/camera, groups apps
   into folders by their declared category, places the widgets you chose and reads the wallpaper.
   The ring lands on 100%, greets you by name and drops you on the finished home screen.

## Home screen

- **Free placement** — every page is a `columns × rows` grid; long-press any icon, folder or widget
  and drag it to any free cell. A drop hint shows where it will land.
- **Everything lines up** — icons and widgets keep to whole rows. A widget runs from the left edge
  of the icon in its first column to the right edge of the icon in its last, and from the top of
  its first row's icons to the bottom of its last row's labels, so the space under a widget is the
  space under any icon. A full dock stands on the same columns as the page, and the name at the top
  starts on the same line as the first column. One class, `HomeGeometry`, works out every position
  in whole pixels. (Zenith Plus free placement still puts things anywhere.)
- **Folders** — drop one app onto another to create a folder; drop onto a folder to add to it.
  Tap a folder for a bottom sheet with an editable name; long-press an app inside to take it out.
- **Remove** — drag to the "Remove" target at the top.
- **Resize and move** — long-press any app or widget → **Resize and move**, then pull the pointer
  on its bottom-right corner. The outline follows your finger and the item snaps to whole cells and whole rows as you
  pass them, with a size badge; it can be any size up to the whole page (a widget stops at the
  smallest its design allows). An app bigger than 1×1 becomes a super icon, its icon stretched to fill the space. A
  size with no room turns the outline red. Tap anywhere to finish.
  While it is up, a bar at the foot of the screen does the same in taps (`ui/home/ResizeBar.kt`):
  a widget's named sizes — Small, Medium, Wide, Tall, Large — one tap each, and four arrows that
  move the item a cell at a time. A named size is taken where the widget stands, or at the closest
  opening on its page when there is no room there; an arrow never pushes anything aside, and a tap
  with no room shakes the bar instead. The rules are `Reshape.resized` and `Reshape.nudged`.
- **App drawer** — a sheet that tracks your finger: drag up anywhere on the page and it follows,
  release and a fling (or the halfway point) decides whether it opens or falls back. Close it by
  dragging down anywhere — the handle, the header, the search row, or the app grid once it is
  scrolled to the top (nested scroll hands the gesture over) — or with back, or the X. The home
  screen blurs and scales behind it in step with the drag. The single-line search field sits at the
  *bottom*, next to your thumb; it searches installed apps and falls through to Google: enter opens
  the first match or, when nothing matches, the web search.
- **Every menu is a bottom sheet** — item menus, Customise, wallpapers. Nothing needs a second
  hand. Settings and the widget gallery are pages of their own, the full height of the screen, on
  one shell (`ui/common/FullPage.kt`): a bar with a way back and a way out, pulled down to close.
- **Dock** — a bar as wide as the page however many apps it holds, as on an iPhone. It takes one
  app for each column of the page, and never fewer than four, and they share the bar out equally:
  a full dock has each app under the icons above it, and a smaller one spreads across the bar
  (`HomeGeometry.dockIconLeft`). Pin from any app's long-press menu.

## Built-in widgets

| Widget | Size | What it does |
| --- | --- | --- |
| Zenith Clock | 2×2 | Small tile: time, short date and a greeting that uses your name |
| Power | 2×2 | Battery ring from `ACTION_BATTERY_CHANGED`, with charge state |
| Calendar | 2×2 | Real month grid, today highlighted, days left in the month |
| Quick Note | 2×2 | Scratchpad persisted with your settings |
| Quick Toggles | 2×2 | Small tile, 2×2 buttons: working torch (Camera2) plus Wi-Fi / Bluetooth / DND |

The **search bar** is not a widget: switch it on under Zenith settings → Home screen and it sits
above the dock, as wide as the icons above it. Tap to search apps, or hit the button for Google.

**The gallery** — long-press an empty spot → **Customise** → **Widgets**
(`ui/home/WidgetGallery.kt`). Every widget is drawn by the code that draws it on the home screen,
at the size it is there, on a window onto your wallpaper, so what you see is what lands on the
page. Tap one and it grows into a showcase: its sizes in a row, all to one scale — Small, Medium,
Wide, Tall and Large, each fitted to the widget and to your grid (`WidgetSizes`), and each a
different design — with a switch between Glass and No glass and one button that adds the size in
the middle. Under the row the sizes are named, each over the room it takes on the page: a little
grid as wide as your page, its cells filled in (`SizeShape`), so Wide is plainly the width of the
page and Tall a row more. A tap on a name brings that size to the middle, and under it a line says
what that size shows ("Hours over minutes, like a Lock Screen"). The lines come from
`WidgetLayouts`, which reads the same rules the widgets lay themselves out by (`WidgetForm`, and
`ToggleGrid` for the toggles), so they cannot say a size shows what it does not. The bar that
comes up for **Resize and move** shows the same little grid on each of its sizes.

The switch between Glass and No glass carries a symbol on each side, a pane and a pane struck
through, and what the side chosen means is said right under it.

**No glass** — the other side of that switch: the same widgets with no pane round
them, like the clock on an iPhone's Lock Screen: the big numbers, rings and icons are made of glass
themselves and sit straight on the wallpaper, with small glass icons added (a sun or moon on the
clock, a battery on Power, a calendar, a pencil). Their words are white with a soft shadow. They are
stored as `bare_<type>` (`bare_clock`, …) and otherwise behave exactly like the widget they are a
version of.

**Words that can be read.** A widget is read at a glance, over a wallpaper, so its second line (the
date, "On battery", "25 days left") is set stronger than the muted grey a page uses, nothing in a
widget is smaller than 11sp at its ordinary size, and the accent is taken part of the way towards
the pane's own ink when it is used for letters. The names under the Quick Toggles are the size of
the label under an app; one too long for its cell ("Do Not Disturb" in a two by two) goes onto a
second line, and every name takes the same number of lines so the buttons stay level
(`ToggleNames.fit`). Their symbols fill a little over half of their button, as Android's own quick
settings do. Everywhere else, second lines and small print (the subtitles in Settings, the words
under a choice) are 74% of the ink, which stands six to one or better against every
theme's backdrop, light and dark (`PaletteToneTest`).

## Customisation

Long-press an empty spot → **Customise** → **Settings**. Settings is a page of its own, the full
height of the screen (`ui/settings/`): a search box and eight groups, each saying how it is set
now — Wallpaper & theme, Home screen, Motion & feel, Apps, Zenith Plus, Aura, Backup & reset, About
& help — and a ninth, Account, in a build that has a server. They stand in three blocks, each
headed with what it holds (Look and feel; Plus and Aura, or Plus, Aura and account; Backup and
help) and set further from
the next than its rows are from each other. A group pushes in from the side; back, the arrow or a swipe from the left edge pops it.
Search finds any option by its name or by other words for it ("dark", "vibrate", "battery"), opens
its page and pulses the row. The Theme and Home pages start with a small live copy of your own home
screen, drawn by the same code as the real one, so a change is seen as it is made.

**Aura** is one of those pages (`ui/settings/AuraPage.kt`): the balance, counted up inside a ring
drawn round to how far it goes towards the next theme; the streak as a ladder of seven days, each
with what it pays; every theme on a card in its own colours, with a bar towards its price and,
when it is out of reach, how many more days it will take; your code to copy or share, and the box
for somebody else's. `Aura.ladder`, `Aura.daysToAfford` and `AuraThemes.next` are plain functions
with tests.

Every option is listed once in `SettingsCatalog`, which is what the search reads. Any Nova- or
ADW-compatible icon pack installed on the device is detected and its `appfilter.xml` is parsed.
The grid is Compact, Balanced or Roomy — a cell size, from which the columns and rows are worked out
for the screen — or **Custom**: columns and rows of your own, three to six across and three to
eight down on a phone and more on a bigger screen (`GridDensity.resolve`). Changing the grid
re-flows the existing layout instead of dropping items, and **Tidy up** puts anything out of line
back in its place.

Every choice says what it does: the four tiles of Customise each carry a line, and under each
control in Settings a line of small print describes the option that is selected. The Wallpaper
tile shows the wallpaper you have, and says which of Zenith's designs it is; the rows under the
tiles sit in a group of their own, a line between each.

## Wallpapers

Zenith paints its own wallpapers on the phone (`WallpaperArt`, `WallDesigns`): thirty-three designs
on four shelves — Soft, Lines, Shapes and Scenes — seven of them for Zenith Plus. Every design is
drawn from a theme's five colours and has a light picture and a dark one, each made so labels and
the status bar stay readable over it. Tap one and both pictures are shown side by side, each with
your own home screen on it, drawn by the code that draws the real one in the ink it takes in that
light. Its glass is frosted there, as over any wallpaper Zenith cannot see: the real glass will
bend the new wallpaper, which is not behind it yet.

A design, once set, **follows the theme**. Change the palette, or go from light to dark (by hand
or with the phone, on Auto), and `WallpaperDirector` paints the same design again in the new
colours, fades it in over the old wallpaper, moves the glass onto it half way, and then hands it to
Android — which cannot fade between wallpapers itself. Only where a run of changes stops is
painted. A design set in another theme's colours without switching Zenith to that theme keeps those
colours and still changes between light and dark. The switch is under Wallpapers and under
Settings → Look.

Zenith only ever repaints a wallpaper it set and that is still on screen. A photo, or anything
chosen in another app, is never replaced, and choosing one ends the following.

## Motion

Everything that moves is a spring, and anything that can be worked out without a screen is a plain
function with a test: `MotionLevel` (how much moves), `PageEffects` (where a page is drawn part way
through a swipe), `FlyIn` (when each item arrives), `LaunchMotion` (where an opening app's card is),
`SheetPull` (where a sheet sits under a finger, and whether letting go puts it away), `Unpack` (how
far out of an opening folder each app is) and `AnimationTuning` (the numbers it all runs on).

- **Opening an app.** The icon grows into a card that fills the screen, and the app appears over
  it. The card's middle travels across and down on springs of their own while a third grows it, so
  it takes a slight curve rather than a straight line, with a soft shadow under it on the way.
  Behind it the home screen sinks back about the icon that was tapped, dims and goes out of focus,
  and the icons nearest make way. Tap the card, or press home, and it turns round where it is.
- **Closing an app.** Android does not let a launcher that is not built into the phone move another
  app's window, so Zenith draws the half it may: the home screen comes forward again on a spring
  that carries it a little too far, the app's icon lands in its place with a bounce, and the icons
  round it are knocked back and swing home. A card that shrinks back into the icon can be switched
  on as well.
- **Advanced animation settings** (Settings → Motion & feel). Every one of those numbers can be
  changed: stiffness and damping for each spring, the arc of the path, how far the card has grown
  when the app starts, corners, logo and shadow, the home screen's zoom, dimming, blur and how far
  icons are pushed, and the return, the landing and the ripple. Each has a slider and a minus and a
  plus that move it one step, a dp or a hundredth at a time. Six styles to start from — Zenith,
  Fluid, Bouncy, Dynamic, Sweet and Classic, the last being how it was before any of this — one
  slider for the speed of everything, and a small screen at the top of the page that plays whatever
  is set. Only what is changed is saved (`AnimationTuning`), and with animations reduced none of it
  swings, bounces, curves or zooms.

- **Arriving.** On a cold start and after an unlock the home screen flies in: every item starts
  out from the middle and comes in to its place, the middle of the page first, inside 600 ms.
  Coming back from an app it is the same movement over a fifth of the distance.
- **Pages.** Four ways to turn one — Depth, Slide, Cube, Stack — and dots whose pill stretches
  towards the next as the page turns. A page added grows its dot out of the end of the row, and
  one forgotten draws it back in.
- **Carrying.** The copy under the finger lifts and casts a shadow; everything else on the page
  rocks, each item out of step; a tick as the landing spot changes; a ring where it lands. Put
  down, the thing itself comes back to its own size from the size it was carried at. Held over
  another icon, a plate spreads out under that one: the folder the two will make. Let go into the
  bin, a folder or the dock, the copy shrinks away into it, and anything taken off the page,
  however it was taken, is seen to go. The bin comes down from the top edge, tips and reddens when
  it is ready, and goes back up.
- **The dock.** Its icons never jump: the others close up round a gap or make room, and the one in
  your hand settles from where it was let go, each starting from wherever it was last seen.
- **Folders** grow out of their own icon into a pane of glass and go back into it, and their apps
  come out one after another as it opens.
- **Widgets** animate their own changes: rolling digits, a ring that springs to its value and
  glints while charging, every other gauge filling the same way as it appears, today's dot drawing
  itself in, toggles that change shape.
- **The drawer and sheets.** The drawer is put together just after the home screen arrives, out of
  sight, so the first swipe up is as smooth as the tenth. Rows rise in each time it opens, its
  title and count turn over as you search, and a wallpaper's preview grows out of the thumbnail
  that was tapped. A sheet comes up from the edge on a spring and follows a finger: down as far as
  it is pulled, up only a little. Let go and it settles, or, past a third of its height or with a
  flick, goes. One sheet opened from another is seen going down as the new one comes up.
- **Settings and the gallery.** Settings arrives in order — its title, the search box, then each
  group in turn — and a row answers a press. The pill of a row of options stretches towards the
  one that was tapped and draws itself up there; a switch's knob spreads under a finger; a number
  rolls up or down a step; the small print under a control changes where it stands. The gallery's
  cards come in one after another, and a widget's showcase grows out of the card that was tapped
  and goes back into it. In the showcase the pill under the names of the sizes goes with the row
  of sizes as it is pulled, stretching towards the next on the way and drawing itself up there
  (`PageEffects.pillSpan`); that size's little grid lights up in the accent, and its name below
  slides in the way the row went.

Movement goes through `graphicsLayer` or draw-time state rather than layout, except where glass
has to be told where it is: a sheet, and the bars that come in from an edge, are moved by being
placed. Anything that runs for a while runs on one clock per page, so nothing is left running once
it has settled (the one exception is the charging glint, while there is a charger).
**Animations: Reduced** in Settings, or the phone's own *Remove animations*, turns all of it down
to quick fades: no fly-in, no rocking, no tilt; sheets and full-screen pages fade in where they
stand, a sheet still follows a finger but settles without a spring, and home pages simply slide.
Haptics go through one `Haptics` helper — a tick, a pick-up, a confirmation — and are silent when
the switch is off.

## How the glass works

`Modifier.glass()` is Zenith's Liquid Glass, modelled on Apple's own description of it and on the
pictures Apple published of it, in two finishes: **Clear** for the dock, folders and small
controls, which only mists the wallpaper and bends it most, and **Regular** for widgets, search and
sheets, which frosts it so text on top stays readable. On Android 13+ an AGSL shader
(`LiquidGlassShader`) draws each pane, and does three things to the wallpaper round it:

- **It bends it.** The flat middle shows the wallpaper exactly where it is; the rounded rim bends
  and magnifies it inward like the edge of a thick lens, with a faint colour split on clear glass.
  A bigger pane is a thicker slab and bends more.
- **It softens it**, with a true blur. `GlassLevels` keeps the wallpaper four times over — as it
  is, misted, frosted, and with little left but its colours, each made from the one before at half
  its size when the wallpaper changes — and a pane shows a blend of the two either side of how soft
  it is (`GlassLook.frost`), so any softness costs two lookups and nothing is blurred while
  drawing. What is dark behind the glass comes through a little brighter, in its own colour.
- **It reflects it.** The steep outer part of the rim is a mirror as much as a window and holds the
  wallpaper just outside the pane, turned over at the edge (`GlassLook.mirror`). The light the
  glass catches is the colour of the wallpaper round it (`GlassLook.glow`): a crisp line all the
  way round the rim, brightest where it faces the light and bright again opposite, with a soft glow
  just inside it. Light, bend and reflection all move as the phone tilts.
- **It is a thing, not a picture of one** (`GlassLook.relief`). A pane is a slab with a rounded
  shoulder and a polished edge, and the same light shows its shape: the shoulder turned to the lamp
  is lit and the one turned from it is in the slab's own shade, light falls away gently across the
  face, and where the polished edge mirrors the lamp there is a glint, with a fainter one opposite.
  A bigger pane is a thicker slab. Turning **Glass** down in Settings flattens it.

A readable veil thickens only over parts of the wallpaper that would fight the text. That is
**Glass panels: Clear** in Settings → Wallpaper & theme, the glass with nothing added and what
everybody has unless they ask for more. **Frosted** is for a wallpaper as dark as the pane itself,
or in the light as pale, where clear glass is mostly its rim: a little frost in the theme's colour
lifts the pane a step away, there and nowhere else (`glassLift`). **Solid** adds a fuller veil on
top of that (`GlassBody`). Every pane casts a shadow in two parts, away from the light: a wide
faint one that says how far off the wallpaper it floats and a close dark one under its edge that
says it is solid, both deeper for a thicker slab (`shadowPaints`). Panes you press
(`interactive = true`: the dock, folders, widgets, search) light up from within, the glow starting
under your finger.

A material can only be judged by looking at it, so `GlassSheetTest` paints a home screen's worth of
panes over several wallpapers with the launcher's own drawing code and leaves the pictures in the
app's external cache (`glass-*.png`), for Clear, Frosted and Solid and for both fallbacks.

`Modifier.glassInk()` makes whatever a composable draws out of glass, for the "No glass" widgets:
its own outline becomes the rim, so the wallpaper bends at the edges of each numeral and icon,
which throw back what is beside them, and the highlight runs along them in the wallpaper's colour.
Each stroke is a rounded bar of glass, shaded and glinting by the same light as the panes.
What is drawn white is clear glass; a colour becomes tinted glass.
With Glass panels on Frosted or Solid the glass also gathers more light over a dark wallpaper, so
a glass clock stands out from it.

Android 13+ does not let a launcher read the wallpaper, so `GlassWallpaper` keeps its own copy of
any wallpaper Zenith sets (its own designs, or a photo picked under Wallpapers → Your photos) and
checks it is still current by the number Android gives every wallpaper (`WallpaperManager.getWallpaperId`,
noted when the copy is made; see `WallpaperTrust`). The wallpaper's colours are only the fallback:
Samsung phones never announce them to the app that set the wallpaper. Android 14+ shows a
still wallpaper zoomed by a tenth to leave room for parallax, and the shader matches that. With no
usable copy (a live wallpaper, or one set in another app) panes fall back to frosted glass with a
rim of the same kind and the slab's form drawn in plain shading, rather than showing a picture that
is not really behind them. Android 12
uses the frosted copy, and "No glass" widgets draw their numbers and icons plain white below
Android 13. Nothing is captured or blurred per frame.

## Accounts and the server

Zenith works with no account, and then nothing leaves the phone. `server.properties` says where
Zenith's server is; while its two lines are empty the app has no server, shows nothing of accounts
and sends nothing. With an address:

- **Settings → Account** (`ui/settings/AccountPage.kt`): create an account or sign in with a
  username and a password, or Continue with Google (Credential Manager, `GoogleSignIn.kt`) when a
  client ID is set; then who you are, your code, how many friends used it, when the backup was
  saved, sign out, and delete — which asks twice and, for a password account, for the password.
- **Backup & reset** gains "Back up to your account" and "Restore from your account" for Zenith
  Plus. The copy sent leaves out the hidden-apps PIN and the purchase; restoring keeps the phone's.
- **Aura**: a code is checked by the server. One nobody has, or your own, is refused; a real one
  pays you 150 and its owner 150, for the owner's first ten friends. The owner's phone is paid the
  next time it asks how the account stands: on the Account or Backup page, and at most every ten
  minutes on the way back to the home screen.

`data/server/` is the app's side: `ZenithServer` (every route, over `HttpURLConnection` and
`org.json`; it answers yes, a no the app can act on, or "out of reach", and never throws),
`AccountStore` (the sign-in, in a preferences file that Android's backups leave out) and
`AccountRules` (what an account changes on the phone, as plain functions with tests). The view
model sees each request through even if the page that asked is closed, one at a time.

`server/` is the other side: `zenith_server.py`, one file of standard-library Python with SQLite,
and `PROMPT-FOR-SERVER-AI.md`, the message that has it installed. `ZenithServerTest` starts that
program and runs the app's own client against it, so the two cannot drift apart (it needs Python on
the PC, and is skipped without it). What the two say to each other, and what was decided, is in
[`specs/2026-10-04-zenith-server-design.md`](specs/2026-10-04-zenith-server-design.md); turning it
on is in [RELEASING.md](RELEASING.md).

## Building

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Compose BOM 2024.12.01, minSdk 29, application ID `com.gabriel.zenith_Launcher`, no third-party libraries:
only Google's own (Play Billing, and Credential Manager with `googleid` for Continue with Google).
Settings and layout are stored as JSON in `SharedPreferences` (`SettingsStore`).

For signed release builds and the Play Store checklist see [RELEASING.md](RELEASING.md); store
copy, graphics and the privacy policy live in [`play/`](play/).

## Working on another PC

Everything the app needs is in the repository, including where its server is (`server.properties`),
so a fresh copy builds the same app, talking to the same server:

```bash
git clone https://github.com/FireGB2012/ZenithUI.git
cd ZenithUI
./gradlew :app:assembleDebug
```

Opening the folder in Android Studio writes `local.properties` (where the Android SDK is); from a
bare terminal set `ANDROID_HOME` instead. The unit tests that start the server program need Python
and are skipped without it.

Three things are left out of the repository on purpose, and travel by hand when they are needed:

- **The debug key**, `.android/debug.keystore` in your user folder. Every PC makes its own, and
  Android will not install a build signed with one key over a build signed with another. To install
  from a second PC onto a phone that already has Zenith from the first, copy that one file across to
  the same place. Do not uninstall instead: that clears the home screen. Continue with Google is
  tied to the same key.
- **The upload key**, `zenith-upload.jks` with `keystore.properties`, for builds that go to Play:
  see RELEASING.md.
- **`.superpowers/`**: working notes, and the copies of the phone's settings taken before each
  install.

Finish a piece of work by committing and pushing it. Work left on one PC has been lost to the other
before.

## Known limits

- Third-party `AppWidgetHost` widgets are not hosted yet — the widgets above are Zenith's own.
- Reading the wallpaper for the glass backdrop requires Zenith to be the default home app. Entering
  the launcher raises the system "Set Zenith UI as your default home app?" dialog (`RoleManager`,
  `ROLE_HOME`, falling back to the home settings screen); it is asked at most three times, and a
  one-tap hint stays above the dock until it is granted.
- Legacy (non-adaptive) app icons are drawn as-is rather than masked to the chosen icon shape.
