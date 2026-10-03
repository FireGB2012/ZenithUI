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
  space under any icon. The dock stands on the same columns as the page, and the name at the top
  starts on the same line as the first column. One class, `HomeGeometry`, works out every position
  in whole pixels. (Zenith Plus free placement still puts things anywhere.)
- **Folders** — drop one app onto another to create a folder; drop onto a folder to add to it.
  Tap a folder for a bottom sheet with an editable name; long-press an app inside to take it out.
- **Remove** — drag to the "Remove" target at the top.
- **Resize** — long-press any app or widget → **Resize**, then pull the pointer on its bottom-right
  corner. The outline follows your finger and the item snaps to whole cells and whole rows as you
  pass them, with a size badge; it can be any size up to the whole page (a widget stops at the
  smallest its design allows). An app bigger than 1×1 becomes a super icon, its icon stretched to fill the space. A
  size with no room turns the outline red. Tap anywhere to finish.
- **App drawer** — a sheet that tracks your finger: drag up anywhere on the page and it follows,
  release and a fling (or the halfway point) decides whether it opens or falls back. Close it by
  dragging down anywhere — the handle, the header, the search row, or the app grid once it is
  scrolled to the top (nested scroll hands the gesture over) — or with back, or the X. The home
  screen blurs and scales behind it in step with the drag. The single-line search field sits at the
  *bottom*, next to your thumb; it searches installed apps and falls through to Google: enter opens
  the first match or, when nothing matches, the web search.
- **Every menu is a bottom sheet** — item menus, Customise, widget picker, settings. Nothing needs
  a second hand.
- **Dock** — one pinned app for each column of the page, each standing under the icons above it,
  and never fewer than four (on a three-column grid the four are spread evenly); pin from any
  app's long-press menu.

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

**No glass** — a second section in the widget picker with the same widgets and no pane round
them, like the clock on an iPhone's Lock Screen: the big numbers, rings and icons are made of glass
themselves and sit straight on the wallpaper, with small glass icons added (a sun or moon on the
clock, a battery on Power, a calendar, a pencil). Their words are white with a soft shadow. They are
stored as `bare_<type>` (`bare_clock`, …) and otherwise behave exactly like the widget they are a
version of.

## Customisation

Long-press an empty spot → **Customise** → **Zenith settings**: palette, appearance, glass strength,
grid density, labels, the search bar, what is written at the top of the page, icon shape (squircle /
circle / rounded / square) and icon packs. Any Nova- or ADW-compatible icon pack installed on the
device is detected and its `appfilter.xml` is parsed. Changing the grid re-flows the existing layout
instead of dropping items.

## Wallpapers

Zenith paints its own wallpapers on the phone (`WallpaperArt`, `WallDesigns`): twenty-two designs
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

## How the glass works

`Modifier.glass()` is Zenith's Liquid Glass, modelled on Apple's own description of it, in two
finishes: **Clear** for the dock, folders and small controls, which shows the wallpaper sharp and
bends it most, and **Regular** for widgets, search and sheets, which frosts it so text on top stays
readable. On Android 13+ an AGSL shader (`LiquidGlassShader`) draws each pane: the flat middle
shows the wallpaper exactly where it is, the rounded rim bends and magnifies it inward like the
edge of a thick lens with a faint colour split — a bigger pane is a thicker slab and bends more —
and a crisp highlight runs round the rim where it faces the light, with a weaker one opposite, both
moving as the phone tilts. A readable veil thickens only over parts of the wallpaper that would
fight the text. Every pane floats on a soft shadow, and panes you press (`interactive = true`: the
dock, folders, widgets, search) light up from within, the glow starting under your finger.

`Modifier.glassInk()` makes whatever a composable draws out of glass, for the "No glass" widgets:
its own outline becomes the rim, so the wallpaper bends at the edges of each numeral and icon and
the highlight runs along them. What is drawn white is clear glass; a colour becomes tinted glass.

Android 13+ does not let a launcher read the wallpaper, so `GlassWallpaper` keeps its own copy of
any wallpaper Zenith sets (its own designs, or a photo picked under Wallpapers → Your photos) and
checks it is still current by comparing the wallpaper colours Android reports. Android 14+ shows a
still wallpaper zoomed by a tenth to leave room for parallax, and the shader matches that. With no
usable copy (a live wallpaper, or one set in another app) panes fall back to frosted glass with the
same rim, rather than showing a picture that is not really behind them. Android 12 uses the cached
blur, and "No glass" widgets draw their numbers and icons plain white below Android 13. Nothing is
captured or blurred per frame.

## Building

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Compose BOM 2024.12.01, minSdk 29, application ID `com.gabriel.zenith_Launcher`, no third-party libraries.
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
