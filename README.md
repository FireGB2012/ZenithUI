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
the middle.

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
(`ToggleNames.fit`).

## Customisation

Long-press an empty spot → **Customise** → **Settings**. Settings is a page of its own, the full
height of the screen (`ui/settings/`): a search box and eight groups, each saying how it is set
now — Wallpaper & theme, Home screen, Motion & feel, Apps, Zenith Plus, Aura, Backup & reset, About
& help — and a ninth, Account, in a build that has a server. A group pushes in from the side; back, the arrow or a swipe from the left edge pops it.
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
control in Settings a line of small print describes the option that is selected.

## Wallpapers

Zenith paints its own wallpapers on the phone (`WallpaperArt`, `WallDesigns`): thirty-three designs
on four shelves — Soft, Lines, Shapes and Scenes — seven of them for Zenith Plus. Every design is
drawn from a theme's five colours and has a light picture and a dark one, each made so labels and
the status bar stay readable over it.

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
through a swipe) and `FlyIn` (when each item arrives).

- **Arriving.** On a cold start and after an unlock the home screen flies in: every item starts
  out from the middle and comes in to its place, the middle of the page first, inside 600 ms.
  Coming back from an app it is the same movement over a fifth of the distance.
- **Pages.** Four ways to turn one — Depth, Slide, Cube, Stack — and dots whose pill stretches
  towards the next as the page turns.
- **Carrying.** The copy under the finger lifts and casts a shadow; everything else on the page
  rocks, each item out of step; a tick as the landing spot changes; a ring where it lands.
- **Folders** grow out of their own icon into a pane of glass and go back into it.
- **Widgets** animate their own changes: rolling digits, a ring that springs to its value and
  glints while charging, today's dot drawing itself in, toggles that change shape.
- **The drawer and sheets.** Rows rise in each time the drawer opens; a wallpaper's preview grows
  out of the thumbnail that was tapped.
- **Settings and the gallery.** Settings arrives in order — its title, the search box, then each
  group in turn — and a row answers a press. The gallery's cards come in one after another, and a
  widget's showcase grows out of the card that was tapped and goes back into it.

Movement goes through `graphicsLayer` or draw-time state, never layout, and anything that runs
for a while runs on one clock per page, so nothing is left running once it has settled (the one
exception is the charging glint, while there is a charger). **Animations: Reduced** in Settings,
or the phone's own *Remove animations*, turns all of it down to quick fades: no fly-in, no
rocking, no tilt; sheets and full-screen pages fade in where they stand, and home pages simply
slide. Haptics go through one `Haptics` helper — a tick, a pick-up,
a confirmation — and are silent when the switch is off.

## How the glass works

`Modifier.glass()` is Zenith's Liquid Glass, modelled on Apple's own description of it, in two
finishes: **Clear** for the dock, folders and small controls, which shows the wallpaper sharp and
bends it most, and **Regular** for widgets, search and sheets, which frosts it so text on top stays
readable. On Android 13+ an AGSL shader (`LiquidGlassShader`) draws each pane: the flat middle
shows the wallpaper exactly where it is, the rounded rim bends and magnifies it inward like the
edge of a thick lens with a faint colour split — a bigger pane is a thicker slab and bends more —
and a crisp highlight runs round the rim where it faces the light, with a weaker one opposite, both
moving as the phone tilts. A readable veil thickens only over parts of the wallpaper that would
fight the text. And glass never quite matches what is behind it: over a wallpaper as dark as the
pane itself, or in the light as pale, a little frost in the theme's colour lifts the pane a step
away, there and nowhere else (`glassLift`). That is **Glass panels: Frosted** in Settings → Wallpaper
& theme, the default; **Clear** leaves it out and **Solid** adds a fuller veil (`GlassBody`). Every pane floats on a soft shadow, and panes you press (`interactive = true`: the
dock, folders, widgets, search) light up from within, the glow starting under your finger.

`Modifier.glassInk()` makes whatever a composable draws out of glass, for the "No glass" widgets:
its own outline becomes the rim, so the wallpaper bends at the edges of each numeral and icon and
the highlight runs along them. What is drawn white is clear glass; a colour becomes tinted glass.
Lifted glass gathers more light over a dark wallpaper, so a glass clock is read as easily as a
printed one.

Android 13+ does not let a launcher read the wallpaper, so `GlassWallpaper` keeps its own copy of
any wallpaper Zenith sets (its own designs, or a photo picked under Wallpapers → Your photos) and
checks it is still current by the number Android gives every wallpaper (`WallpaperManager.getWallpaperId`,
noted when the copy is made; see `WallpaperTrust`). The wallpaper's colours are only the fallback:
Samsung phones never announce them to the app that set the wallpaper. Android 14+ shows a
still wallpaper zoomed by a tenth to leave room for parallax, and the shader matches that. With no
usable copy (a live wallpaper, or one set in another app) panes fall back to frosted glass with the
same rim, rather than showing a picture that is not really behind them. Android 12 uses the cached
blur, and "No glass" widgets draw their numbers and icons plain white below Android 13. Nothing is
captured or blurred per frame.

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

## Known limits

- Third-party `AppWidgetHost` widgets are not hosted yet — the widgets above are Zenith's own.
- Reading the wallpaper for the glass backdrop requires Zenith to be the default home app. Entering
  the launcher raises the system "Set Zenith UI as your default home app?" dialog (`RoleManager`,
  `ROLE_HOME`, falling back to the home settings screen); it is asked at most three times, and a
  one-tap hint stays above the dock until it is granted.
- Legacy (non-adaptive) app icons are drawn as-is rather than masked to the chosen icon shape.
