# Icon depth and smoother motion

- Date: 2026-11-16
- Branch: `claude/project-thread-al1jba`, off the Liquid Glass branch (`claude/project-thread-a7d4wc`)
- Status: built. Gabriel: "the normal icon should have the kinda a 3d effect that appel haves dont
  push it on apps thats to hard also make smothe liquid animations better".

## Which icons, and what "don't push it on apps" means

"Normal" is the **Original** icon style, Zenith's default: each app's icon as the app draws it.
Tinted and Glass are left as they are (the drawer's glass icons are being redone elsewhere).

The depth is light and shadow drawn **over** the icon, inside its outline (`Modifier.iconDepth`,
`ui/theme/IconDepth.kt`). Nothing about the app's own picture is changed, re-drawn, re-coloured or
re-shaped: switch it off and the icon is exactly what it was.

## What an iPhone icon's depth is, and Zenith's

An iPhone icon reads as a thin tile with a little thickness to it, lit from up and to the left, as
Zenith's glass is. All of it is subtle; any one part turned up turns it into a bevelled button,
which is the "goo" look Gabriel threw out of the glass (see
`2026-11-15-apple-liquid-glass-design.md`). So:

- **A hairline of light on the rim** (2.2% of the side, 0.6 to 1.4 dp), brightest on the corner
  facing the light (62% white), bright again on the corner opposite (62% of that) and faint along
  the sides (14%): the same profile as the glass's rim, so icons and glass are lit by one light. It
  moves with the phone's tilt, as the glass's does, read while drawing so nothing recomposes.
- **A softer light just inside it** (8.5% of the side, 1.6 to 7 dp, 12% white), fading inward: the
  edge of the tile catching the light.
- **The face**: 12% white at the top, gone by the middle, and 7% black at the foot. Depth, not the
  lit and darkened shoulders of a slab.
- **A shadow, only on the wallpaper** (home pages and the dock), from 36 dp up: a wide soft one
  (blur 9% of the side, 3.5 to 10 dp, falling 4%, 1.5 to 4 dp, 18% at most) and a close one under
  the edge (1.2 dp, 0.6 dp down, 10%) so the outline holds on a pale picture. Straight down, no
  darker than the glass's. A bigger icon stands higher: a wider shadow, falling further.
- **No shadow** where the icon stands on a surface of its own: the drawer, a folder's panel, and
  inside a folder's glass on the home screen.
- **Light mode, off the wallpaper**: 72% of the light, or the rim would whiten against the pale
  page. On the wallpaper it is the same in Light and Dark, since the picture is the same.
- **Too small to show it** (under 18 dp, a folder's previews): flat.
- **Super icons** get the same, on their own outline (corners 26% of the short side), with the rim
  lit on its corner however wide it is.

### Cost

The shadow is the same for every icon of one outline and size, so it is blurred once into a bitmap
and shared (`CastShadows`, the last 12 kept), not blurred per icon per frame. The rest is three
strokes and a gradient over each icon, drawn from a cache built once per size.

## Smoother motion

Apple's springs are damped between 0.7 and 1 with a response of 0.2 to 0.5 s: they come to rest
with one small bounce at most. Several of Zenith's were damped at 0.36 to 0.55, which goes 13% to
30% past and swings back and forth three or four times. That reads as jelly, not liquid.

| Spring | Was (damping / stiffness) | Now | Used for |
|---|---|---|---|
| `snappy` | 0.48 / 260 | 0.78 / 320 | state flips, toggles, swatch ticks |
| `bouncy` | 0.40 / 200 | 0.70 / 230 | the luxurious ones |
| `cell` | 0.66 / 420 | 0.80 / 400 | icons gliding to a new cell |
| `press` | 0.36 / 420 | 0.62 / 520 | everything squeezed under a finger |
| `lift` | 0.38 / 260 | 0.70 / 340 | something picked up |
| `drop` | 0.62 / 380 | 0.70 / 420 | something put down |
| `rise` (new) | 0.55 / 170 | 0.80 / 220 | the drawer's rows rising in |
| `riseGently` (new) | 0.86 / 240 | same | whole blocks of a page |
| `roll` (now shared) | 0.48 / 260 | 0.78 / 320 | numbers rolling over |
| `edge` (now shared) | 0.72 / 320 | same | bars arriving from an edge |
| the corner to pull, appearing | 0.50 / 300 | `open` (0.78 / 300) | edit mode |
| the corner to pull, held | 0.50 / 500 | `press` | edit mode |
| the Dynamic Island's press | 0.50 / 700 | `press` | |

`press` stays a little looser than the rest (0.62, 7% past and back) because it is a finger's own
squeeze, the one place a bounce is the point. `MotionTest` holds every one of them to: at most 10%
past its rest, at most 1% on the swing back, a response between 0.2 and 0.5 s, and the sheets,
pages and closes at 0.9 or more.

### Settings and the widget gallery follow your thumb

Before, pulling a page's bar down more than 30 px in one frame closed it outright, and the page
then slid away on a fixed 320 ms tween. Now a full page is pulled the way a sheet is:

- **By its bar, or by what it holds** once that is scrolled to the top, it goes exactly as far as
  the finger takes it. Back up, and it goes back before anything scrolls.
- **Let go**: a flick down (over 900 px/s) or further than 30% of the way closes it, and it carries
  on down at the speed it was thrown at; anything else springs it back on the sheet's spring
  (`SheetPull`, the same rules as the sheets).
- **The home screen comes back under it** exactly as far as the page has gone: it fades back in
  and grows from 96% to its full size (`HomeCover`, `pageCover`, `homeScaleUnderPage`), and the dim
  behind the page clears with it. Before, the home screen faded on a timer of its own that knew
  nothing of the page.
- **Its top corners round** (30 dp) while it is off the screen's edges, as a sheet's are, and it
  is square to the screen while it fills it.
- **It comes up on the sheet's spring and goes on `close`** (damped at 1, no bounce), its own
  animation rather than a slide, so a page let go part way goes on from where the finger left it
  instead of jumping back to start its slide from the top.
- With movement reduced it does not travel: it fades, as before.

## Checked, and not

- Unit tests: `IconDepthTest` (the light's bounds and profile, hairline widths, where the lit corner
  is for any light and any shape, shadows only where they belong), `MotionTest` (every spring's
  overshoot, swing and response), `FullPageTest` (where a pulled or thrown page is drawn and how
  much of the home screen it covers).
- Seen on the JVM screenshot rig (Robolectric, native graphics): the home screen in Dark and Light,
  the drawer, Settings open and pulled down part way.
- Not seen on a phone yet. The tilt, the springs and the pull can only be judged by hand.
