# The iOS 26 home screen, with Zenith's twists

- Date: 2026-11-09
- Branch: `apple-look`, reset on 2026-11-09 to 51b48f5 ("Glass that moves like glass", 4 Nov). The flat
  iPhone look it held before is kept on GitHub as `apple-look-flat` and is not coming back.
- Status: agreed with Gabriel in chat and in a clickable mockup,
  `specs/mockups/2026-11-09-home-screen-twists.html` (open it in Chrome or Edge: move the mouse over
  the phone to tilt it, tap the wallpaper, press an icon, drag the page, tap Search). Read by a
  builder check the same day; its questions are answered below. Not built yet.
- Marked *(my call)*: decisions Claude made that Gabriel has not been asked about. Anything not
  marked was Gabriel's.

## Why

Gabriel wants Zenith to look like an iPhone on iOS 26 and 27: the layout, and above all the Liquid
Glass. The first attempt (`apple-look-flat`) took the glass off pages and sheets and made them solid;
it read as iOS 15 and was scrapped. Zenith's own glass, as it stood on 4 Nov (the AGSL lens, the
light that follows the tilt, the four glass buttons from the Claude Design handoff), is the material.

He also asked for Zenith to have its own twist rather than copy Apple: "make it look awesome". The
five twists below were picked from the mockup, all five of them.

## The whole rebuild, and where this fits

Everything that is not a screen stays: `data/`, `island/`, the server, `ZenithViewModel`, the glass
engine in `ui/theme`. Every screen is rebuilt to look like iOS 26, one at a time:

| Step | Screen |
|---|---|
| 1 | **The home screen** (this document) |
| 2 | Folders, the menus you get when you hold something, and edit mode (icons wiggling, Edit > Customise) |
| 3 | The app drawer, as iOS's App Library |
| 4 | Search |
| 5 | The widget picker |
| 6 | Settings |
| 7 | Setup |
| 8 | Zenith Plus, Aura, the island |

Until its turn a screen stays as it was on 4 Nov, glass and all, so Zenith works the whole way
through; a screen's old code is deleted when its new one lands. Each step starts with a clickable
mockup for Gabriel's OK, is built by a small workflow of subagents (tests first, a reviewer after),
is checked on a throwaway emulator with pictures sent to Gabriel, and is committed and pushed to
`apple-look` before the next starts.

## Goals for the home screen

1. It looks like Gabriel's iOS 26 photo (described under "What it looks like").
2. Widgets come in two looks, A (colour) and B (glass), and Gabriel switches between them in the app.
3. The five twists.
4. Everything the home screen does today still works: pages, drag and drop with things stepping
   aside, folders by dropping one icon on another, the bin and Undo, widgets and other apps' widgets
   and stacks, resizing, the dock and its reordering, opening and closing apps from their icon (the
   card and the landing), swipe up for all apps, swipe down and double tap gestures, holding an empty
   spot for Customise, the default-launcher hint, badges, the arrival of the icons, page effects.

## Not in this step

- The other screens in the table. The drawer, its search, the folder panel, the hold menus, the
  widget gallery, sheets, Settings and setup stay as on 4 Nov.
- Edit mode (icons wiggling, Edit > Customise) is step 2. Until then holding an empty spot opens
  Customise directly, as it does today *(my call)*.
- Copying iOS exactly. Where Zenith's twists differ from Apple, the twists win.

## Anti-goals: a failure even if it works

- Flat, solid surfaces where glass belongs. The dock and Search are always Liquid Glass.
- A home screen that keeps redrawing when nothing moves, or stutters while paging.
- Anyone losing a layout, a setting, a Look or a backup; anything in Goals 4 breaking.

## What it looks like

### Layout

From the top: Android's status bar; the page, with its widgets and icons; the Search pill; the dock.
Nothing else is on the home screen: no greeting or clock line under the status bar (the `header`
setting), no page dots (the `indicator` setting), no full-width search bar (the `searchBar`
setting). Those settings stay stored and readable for old backups and Looks; the new home screen
simply does not draw them *(my call)*.

- **Grid.** Four columns, as on an iPhone, with rows spaced like an iPhone's: a row is never more than
  1.09 times as tall as a column is wide (98 pt to 90 on an iPhone 15 Pro). On a tall phone the grid
  stands at the top and the room it does not need is left under its last row, above Search, as an
  iPhone leaves it. Widgets on this grid reach a few dp past their icons at the sides, as an iPhone's
  do, so a 2×2 widget is exactly square and Medium and Large keep an iPhone's proportions (found on the
  emulator: filling the page made rows 1.17 columns and widgets a tenth taller than square). It is a new grid setting, `four`, beside Compact,
  Balanced, Roomy and Custom. A new install gets it. A phone that already has Zenith is asked once
  (see "Phones that already have Zenith"). The existing grid settings keep their numbers, since
  changing a preset would rearrange every home screen that uses it the next time Zenith starts. On a
  grid that is kept, a widget's frame still ends at its last row's icons with its name under it, so
  where rows are taller than columns are wide its widgets are taller than square *(my call)*.
- **Icons.** Cut to an iPhone's rounded square: a new icon shape, `iphone`, a rounded square with
  continuous corners. New installs get it, a phone that takes the new look gets it, and anyone can
  pick it in Customise or in Settings. An existing shape's geometry never changes, since saved icon
  pictures are keyed by the shape's id. Icon size follows iPhone proportions, about 64% of a cell's
  width, kept between 52 and 68 dp *(my call)*.
- **Names.** White with a soft shadow on the home screen, whatever the palette, as on an iPhone. The
  drawer and the folder panel keep their own ink.
- **Widgets.** Square Small (2×2), Medium (4×2) and Large (4×4), all with one corner radius, each with
  its name under it like an icon (other apps' widgets show their app's name). A widget's frame ends
  where the last row's icons end, so its name sits in the same strip as the icons' names. Widgets
  keep their free sizes, from the smallest each allows up to the whole page, and resizing works as
  now; Small, Medium and Large are the sizes new widgets come in *(my call)*.
- **Search.** A small Liquid Glass pill, a magnifier and "Search", centred between the page and the
  dock, in a band whose height does not depend on the page (the page's rows are worked out from what
  is left, so a band sized from the page would oscillate).
- **Dock.** A floating Liquid Glass slab, inset from the screen's edges, holding four apps with no
  names on a four-column grid, its corners as round as an iPhone's. How the dock behaves today stays:
  reordering with neighbours stepping aside, dragging out, dropping in. The dock styles keep their
  meanings: Glass and Floating are both the new slab (Floating hugs its icons when there are fewer than
  four), Icons only stays icons on the wallpaper with no glass, and the separate Show dock switch
  still hides it *(my call)*.

### Widgets: A and B

- **A, Colour** (what a new install starts on). Solid and colourful like iOS's own widgets. Weather
  is a sky: blue by day, deep blue at night, grey under cloud, chosen from the weather code and the
  time of day. Calendar is white with a red weekday. The other Zenith widgets get colours of their
  own in the same spirit. Each has a dark version in dark mode. No glass, so it is the cheapest look.
- **B, Glass.** Zenith's Liquid Glass pane (Regular), tinted by the wallpaper (twist 2), with white
  words and a soft shadow in light mode and dark.
- **The switch.** A setting, `widgetLook` ("colour" or "glass"), in Customise (hold an empty spot) and
  in Settings, Home screen. A Look carries it *(my call)*. When edit mode arrives in step 2, Customise
  moves to Edit > Customise. A value missing from saved settings reads as glass, so a phone that
  already has Zenith keeps its glass widgets until it is asked; setup writes colour.
- **A widget's own colour** (its menu, then Colour) is the card's fill in look A, its words in
  whichever ink stands on it, and tints the glass in look B in place of the wallpaper's colour. With
  the theme's colour chosen, look A keeps each kind's own colours and look B takes the wallpaper's.
  Nobody's chosen colours stop showing *(my call)*.
- **Other apps' widgets** draw themselves in both looks.
- **Old "No glass" widgets** (the `bare_` types) keep loading and drawing as they do now; the gallery
  keeps its Glass / No glass switch until the widget picker's own step *(my call)*.
- **Weather and Calendar are redesigned the iOS way at Small, Medium and Large;** their other sizes
  keep today's layouts in the new colours *(my call)*.
  - Weather: the place, a big temperature, the sky's symbol and words, high and low. Medium adds the
    next hours; Large adds the next days, which needs the daily forecast added to the weather fetch
    (still only latitude and longitude are sent, as the README promises) *(my call)*.
  - Calendar: the weekday in red, the date large, and today's next events once calendar access is
    given. The widget asks for access itself, once, from a "Show your events" prompt *(my call)*.
    Its date turns over at midnight (today it stays on the day it was drawn).
  - The other Zenith widgets (clock, battery, toggles, note, and the rest) get their A and B looks
    without changing what they show.

### Phones that already have Zenith

Gabriel's answer: let each person choose, with Apple's look as the default. The first time the
updated Zenith opens on a phone that already had it, a sheet asks once: **Switch to the new look**,
the main, highlighted answer, or **Keep my home screen**. Switching sets the iPhone grid (`four`),
colour widgets (A), the `iphone` icon shape and the glass dock; apps and widgets keep their order but
some may move to another page, and the sheet says so before anything moves. Keeping changes nothing.
The sheet needs one of the two answers (tapping outside does not close it) and is not shown again;
either way every one of these is in Customise and Settings afterwards. A new install never sees it:
setup starts on the new look. A backup restored from before this version asks again.

### The five twists

1. **Light that moves with you.** Tilt the phone and the glass catches the light on its rim and face.
   Icons, widgets and the dock float a hair above the wallpaper, moving a few dp against the tilt
   (icons and widgets about 4 dp, the dock about 2 dp *(my call)*), so the wallpaper reads as deeper.
   Holding the phone the way you normally do is the rest pose, not "upright". The Tilt shine switch
   turns the light and the floating off together, so the motion sensor stays off for anyone who has
   turned it off *(my call)*.
2. **Glass made from your wallpaper.** The dock, Search and look-B widgets take on a tint of the
   wallpaper's own colour where each one sits, with a soft glow of that colour beneath it. Change the
   wallpaper and the glass follows.
3. **Liquid Search.** Tap the pill and its glass stretches into a wide search field that rides up on
   the keyboard, the page dimming and softening behind it. Typing shows matching apps and the answers
   the drawer gives today (calculator, units, settings links, a web search). Tap outside or press
   back and the field shrinks back into the pill with a small wobble. While you swipe between pages,
   the pill's words give way to the page dots, the current one long, and come back about a second
   after the page settles. The swipe-up drawer and its own search stay as they are. *(my call: the
   pill's search shows apps and the drawer's answers; how results look is redesigned in step 4.)*
4. **A drop on the glass.** Tap an empty bit of wallpaper and a ring of light runs out from the finger
   across the screen; every glass pane lights up, rim and face, as the ring passes over it, and the
   icons and colour widgets catch a faint gleam. It replaces today's double-tap ring *(my call)*.
5. **Jelly icons.** Press an app, folder or dock icon and it squishes and lights up from under the
   finger, then springs back with a wobble. On the home screen icons always do this; the Pressing
   setting keeps applying everywhere else *(my call)*.

With movement reduced: no parallax, no ring, the pill's field fades in rather than stretching, and the
jelly is a small, critically damped press. The light follows the tilt only while Tilt shine is on, as
now.

## How it is built: decided

### The glass engine

- **Glass that moves tells the glass.** A pane moved by a graphicsLayer (parallax, jelly) lines up its
  wallpaper with `glass(tracking = ...)` or `glass(motion = ...)`, as on 4 Nov, so the picture stays
  put and only the pane moves.
- **Tilt.** `LocalTilt` stays the one light source. `rememberTilt` gains: a rest pose that slowly
  follows how the phone is held; a dead zone, so a still phone stops redrawing every pane; and smooth
  steps between sensor readings (stepped on the frame clock), since parallax at the sensor's 15 Hz
  would visibly step. Parallax is read inside the per-item graphicsLayer in `HomePage` and in the
  dock's, never in composition, and is off with movement reduced or Tilt shine off.
- **The wallpaper's colour in the glass.** The lens's veil is tinted per pane from the wallpaper copy
  it already samples, added to the shader with a default that changes nothing for anyone else. The
  fallbacks (Android 12's frosted copy, Android 10 and 11's plain body) take one colour sampled at the
  pane's middle from the small haze level, or the saved wallpaper colours or the palette when there is
  no copy. The glow is a third paint beside the pane's shadow, drawn outside its clip.
- **The ring.** One ring state for the screen (where and when), read by every pane while drawing, as
  `LocalTilt` is; a ring band in the lens, and a brighter rim in the fallbacks. Things that change
  fast (tint, ring, parallax) are read while drawing and never go through `LocalPalette`, which is a
  static local that would recompose the whole screen.
- **The pill to the field** is drawn at its animated rectangle with `drawLiquidGlass` in a Canvas, as
  `AppLaunchCard` already does, not stretched by a layer: a stretched layer widens the rim, bends the
  corners and magnifies the wallpaper.
- **One lens per wallpaper copy,** shared by every pane, instead of one compiled per pane; this ends
  the burst of shader compiles when pages appear or the wallpaper changes.
- **Jelly** is `GlassMotion.Jelly` through `glassPress`, with its tested springs, plus a finger light
  drawn over the icon's own pixels; without a haptic tick on every finger-down (a page swipe that
  starts on an icon must not buzz); letting go when a swipe or a carry takes the finger; with the
  icon's launch origin measured at rest so apps still open from their icon.
- **Look B's words** are white in light mode too, so the pane is drawn with the dark tone's veil
  underneath its words, and the words get the soft shadow the "No glass" widgets already use.

### The code

- The visible home screen (status bar space, pager, Search band, dock) moves out of the 2,390-line
  `HomeContent` into a composable of its own; `HomeContent` keeps the state and the orchestration
  (drawer, app card, sheets, gestures, lifecycle) as it is.
- `HomeGeometry` stays the one source of whole-pixel lines and is extended for the iPhone icon size,
  widget frames that leave a name strip, the Search band and the floating dock.
- The old search bar code (`HomeSearchBar`, `SearchBar`, `SearchButton`) is replaced by the pill; the
  `search` widget type keeps converting as it does.
- The pill's search gets its own small surface. The drawer's app matching moves into a pure function
  both use.
- The previews follow the new layout: `PhonePreview`'s `MiniHome` (setup's hand-off into the real home
  screen, Settings' previews, the page overview) draws with the same new pieces, or setup's hand-off
  would jump.
- New settings (`widgetLook`, the `iphone` shape, the `four` grid, and a flag that the new look has
  been offered) are added the way every setting is: read with a default, old values kept as written,
  a Settings search entry, and a decision on Looks. The new-look flag is not carried by a Look.

### Cost

On one page the glass is the dock, Search and the look-B widgets: about four to seven panes, double
that for a moment while two pages are on screen. Icons are plain pictures (the existing Glass icon
style still exists and costs more). A still phone draws nothing.

## Constraints

- Compose BOM 2024.12.01, minSdk 29, no new libraries.
- Android versions: the lens, tint and ring need Android 13 (API 33); Android 12 gets the frosted
  copy and Android 10 and 11 the plain body, each with a fallback for every new effect.
- Old settings, layouts, Looks and backups keep reading; a key is never reused with a new meaning.
- A rule gets a unit test before its code: the geometry, the new shape's maths, the settings, the
  tilt's rest pose and dead zone, parallax, when the ring reaches a pane, the search matching, the
  look-A colours and the contrast of words on them.
- Checking: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`;
  the instrumented glass tests run with `adb shell am instrument` on a throwaway emulator, never
  `connectedDebugAndroidTest`. Nothing runs on Gabriel's phone or his own emulator.
- Each part is committed and pushed to `apple-look`; it is merged into `main` only when Gabriel asks.

## Left to the builder

Exact sizes and spacing within the proportions above; the look-A colours of each widget; the curves,
within the springs Zenith already uses; how the code is split into files; the order of the parts in
the plan.
