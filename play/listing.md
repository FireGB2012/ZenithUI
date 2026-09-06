# Play Store listing — Zenith UI

Paste these into Play Console → Grow → Store presence → Main store listing.

## App details

| Field | Value |
| --- | --- |
| App name (30 chars max) | `Zenith UI` |
| Package / application ID | `com.gabriel.zenithui` (final once uploaded — cannot be changed later) |
| Category | Personalisation |
| Tags | Launcher, Home screen, Widgets |
| Contact email | gabrieltinndahn06@gmail.com |
| Website | *(optional)* |
| Privacy policy URL | `https://firegb2012.github.io/ZenithUI/` (served from `docs/index.html` via GitHub Pages) |
| Ads | No |
| In-app purchases | No |
| Content rating | Everyone (questionnaire: no user-generated content, no data sharing) |

## Short description (80 characters max)

```
Liquid glass launcher with resizable widgets, icon packs and thumb-first menus.
```

## Full description (4000 characters max)

```
Zenith UI is a home screen that pairs the calm of frosted glass with Android's freedom.

LIQUID GLASS
Panels are translucent, tinted and lit from the top edge, and once Zenith is your default home app they blur a copy of your own wallpaper — so the launcher looks like it is made of your background, not painted over it.

BUILT FOR ONE HAND
Every menu is a bottom sheet. The app drawer's search field sits at the bottom where your thumb already is, and the drawer follows your finger as you pull it up or push it down.

SET UP IN TWENTY SECONDS
Tell Zenith your name, pick a palette, choose your widgets and grid, and it builds the first page for you — your widgets, plus Google, Gmail and your browser. Everything else stays one swipe away instead of being dumped across five pages.

WIDGETS THAT FIT
Six built-in widgets: an oversized clock, a live battery ring, a month calendar, a quick note, quick toggles with a working torch, and a search bar that searches your apps or Google. Long press any widget, tap Resize and drag its handles — the grid has quarter-row steps, so a slim bar takes a slim slot instead of hogging a whole row.

MAKE IT YOURS
Six palettes, light/dark/auto, adjustable glass, grid from 3x4 to 6x7, icon size, labels on or off, four icon shapes, and support for any Nova or ADW compatible icon pack.

BEDSIDE CLOCK
Tap the clock widget for a dimmed full screen clock with the date and today's calendar — a quiet bedside face that keeps the screen awake while it is open.

SMOOTH BY DESIGN
Nothing snaps. Icons glide to their new cell, sheets spring up from the edge, palettes cross-fade, and the whole launcher runs on one spring vocabulary tuned for high refresh rate screens.

PRIVATE BY DEFAULT
No account, no analytics, no ads, no network access. Your name, layout and settings never leave your phone.

Zenith UI needs no special permissions beyond setting your wallpaper and a short vibration when you pick up an icon. Calendar access is optional and only asked for if you want the bedside clock to list today's events.
```

## What's new (500 characters max)

```
First release.
• Liquid glass home screen that blurs your wallpaper
• 20 second guided setup
• Six built-in widgets, resizable in quarter-row steps
• Drawer that tracks your thumb, with app and Google search
• Six palettes, icon packs, four icon shapes, adjustable grid
• Bedside clock with today's calendar
```

## Graphics

| Asset | File | Requirement |
| --- | --- | --- |
| App icon | `play/icon-512.png` | 512×512 PNG, 32-bit |
| Feature graphic | `play/feature-graphic-1024x500.png` | 1024×500 PNG |
| Phone screenshots | `play/screenshots/*.png` | 1080×2424, at least 2, up to 8 |

Screenshots included: welcome, home screen, app drawer, Customise sheet, Zenith settings.

## Data safety form answers

- **Does your app collect or share any of the required user data types?** No.
- **Is all of the user data collected by your app encrypted in transit?** Not applicable — no data leaves the device.
- **Do you provide a way for users to request that their data is deleted?** Not applicable; clearing app storage removes everything.
- Note for the reviewer, if asked: the launcher reads the list of installed applications through `LauncherApps` in order to display them, and — only with the user's consent — today's calendar entries for the bedside clock. Neither is transmitted, logged or stored off-device.

## Permissions declared

| Permission | Why |
| --- | --- |
| `SET_WALLPAPER` | The Customise sheet opens the system wallpaper picker |
| `VIBRATE` | A short tick when you pick up an icon or resize a widget |
| `READ_CALENDAR` | Optional. Requested at runtime the first time you ask the bedside clock to show today's events; the launcher works fully without it |

`READ_CALENDAR` is in the "personal and sensitive" family, so the listing must explain it: the bedside clock shows today's events on a dimmed screen, the data is read on the device and never leaves it, and every other feature works if you decline.

No `QUERY_ALL_PACKAGES`: app discovery uses `LauncherApps` plus a `<queries>` element, so no sensitive-permission declaration form is required.
