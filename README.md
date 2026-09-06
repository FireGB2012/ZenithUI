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
- **Folders** — drop one app onto another to create a folder; drop onto a folder to add to it.
  Tap a folder for a bottom sheet with an editable name; long-press an app inside to take it out.
- **Remove** — drag to the "Remove" target at the top.
- **App drawer** — a sheet that tracks your finger: drag up anywhere on the page and it follows,
  release and a fling (or the halfway point) decides whether it opens or falls back. Close it by
  dragging down anywhere — the handle, the header, the search row, or the app grid once it is
  scrolled to the top (nested scroll hands the gesture over) — or with back, or the X. The home
  screen blurs and scales behind it in step with the drag. The single-line search field sits at the
  *bottom*, next to your thumb; it searches installed apps and falls through to Google: enter opens
  the first match or, when nothing matches, the web search.
- **Every menu is a bottom sheet** — item menus, Customise, widget picker, settings. Nothing needs
  a second hand.
- **Dock** — up to five pinned apps; pin from any app's long-press menu.

## Built-in widgets

| Widget | Size | What it does |
| --- | --- | --- |
| Zenith Clock | 2×2 | Small tile: time, short date and a greeting that uses your name |
| Power | 2×2 | Battery ring from `ACTION_BATTERY_CHANGED`, with charge state |
| Calendar | 2×2 | Real month grid, today highlighted, days left in the month |
| Quick Note | 2×2 | Scratchpad persisted with your settings |
| Quick Toggles | 2×2 | Small tile, 2×2 buttons: working torch (Camera2) plus Wi-Fi / Bluetooth / DND |
| Search Bar | 4×1 | A single-line pill: tap to search apps, or hit the button for Google |

## Customisation

Long-press an empty spot → **Customise** → **Zenith settings**: palette, appearance, glass strength,
columns (3–6), rows (4–7), icon size, labels, icon shape (squircle / circle / rounded / square) and
icon packs. Any Nova- or ADW-compatible icon pack installed on the device is detected and its
`appfilter.xml` is parsed. Changing the grid re-flows the existing layout instead of dropping items.

## How the glass works

`Modifier.glass()` is the whole system. When Zenith is the default home app it can read the
wallpaper, so `WallpaperSource` keeps one small pre-blurred copy of it; each panel paints that copy
at exactly the offset the panel occupies on screen, then a translucent gradient, a palette tint and
a specular rim. The blur is computed once, not per frame. When the wallpaper is not readable, panels
fall back to a frosted body in the palette's own colour, so sheets stay legible over anything.

## Building

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Compose BOM 2024.12.01, minSdk 31, application ID `com.gabriel.zenithui`, no third-party libraries.
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
