# The Zenith build brief: a smooth drawer, the Notch, themes and a new setup

2026-11-16. Gabriel's build brief (a Claude Doc), done in its order: the drawer's speed first, then
the Zenith Notch and its controls, Liquid Glass as the default theme, a theme system with four
themes, a new first-time setup with a real "Optimizing Zenith" step, and a config model ready for
sync. He asked for no questions unless something truly blocked, and for the choices made to be
listed; they are marked *(my call)* here.

## 0. The drawer at 60 fps and more

What made it drop frames, found by reading the code:

- **Every frame of the swipe composed the whole home screen again.** `drawerProgress` was read while
  composing `HomeScreen` (and passed to `AppDrawer` as a number), so each frame recomposed both.
- **The blur behind it was worked out again every frame.** The home screen was blurred with
  `Modifier.blur`, and its glass and icons follow the phone's tilt, so their light changed every frame
  the phone moved in the hand: each change made Android blur the whole screen again.
- **Icons were read from files while the list moved.** An icon not in the cache was decoded from its
  saved PNG on the main thread as its row came into view, and drawn afresh off it.
- **Every icon ran an animation of its own** on each opening, and the drawer asked for every app's
  shortcuts half way through the swipe.

What it does now:

- **Icons, once.** As soon as the app list is known, every app's icon is drawn at exactly the size the
  drawer draws it (`HomeGeometry.drawerIconPx`, then the drawer's own measure) on one background
  thread, and kept in memory out of the cache's reach (`IconLoader.warm`). In the drawer an icon only
  ever starts from memory (`LocalIconFromMemory`). Android's package callbacks say which apps were
  installed, updated or removed, and only those are drawn again (`IconLoader.forget`,
  `IconKeys.packageOf`). With Tinted or Glass icons the symbol is what is kept, not both.
- **Built once, only moved.** The drawer was already put together out of sight after the home screen
  arrives. Its progress is now a function read only where it is placed and drawn; what composing needs
  (shown, settled) is derived, so only crossing those lines composes anything. The grid keeps stable
  keys, now with content types, and the swipe moves it by placement only.
- **One clock for the rise.** The rows still rise in each time it opens, a row at a time and along each
  row, on Motion.rise's spring, but worked out from one clock the drawer runs (`DrawerRise`, tested
  against Compose's own spring) and read while drawing.
- **A static blur while it moves, the live one once it is open.** A copy of the home screen is drawn
  from one `GraphicsLayer`, blurred once, and faded in over it as the drawer opens (`DrawerFrost`); a
  fade costs Android nothing. Once the drawer is open, that copy is the home screen's blur and follows
  anything that changes under it. Behind the drawer, and in the drawer while it moves, the light on the
  glass is held where it was (`HeldTilt`), so nothing is drawn again for the phone moving in the hand.
- **Asked once it has arrived.** The shortcuts the drawer's search looks through are asked for when it
  has come to rest open.

*(My call)* The blurred copy comes in between 20% and 60% open; the sharp home screen fades out under
it on a curve that keeps what is behind from showing through by more than a tenth at any moment
(`DrawerFrost.sharpAlpha`). Before Android 12 nothing blurs, as before, and the home screen only dims.

## 1. The Zenith Notch

Zenith already had most of it, as the island (Plus): a pill round the camera drawn over every app,
placed from the display cutout, that grows on a spring for music (cover and bars; tapped, the song,
the artist, how far through, and back, play and next, through `MediaSessionManager` and the
notification listener), calls and timers, notifications (tapped, the whole message, and a tap opens
it), the charger and the ringer. So the Notch is that island, finished to the brief:

- **The collapsed state.** At rest it is now a small pill with the battery, its number in it, and up to
  two status icons: Do Not Disturb (a moon), silent or vibrate (`IslandRules.status`, `battery`). The
  service follows the battery, the ringer and Do Not Disturb as they change.
- **Its name.** Settings, search and Plus call it the Zenith Notch; the code keeps `island`.
- **Permissions, explained.** Turning it on, or tapping either permission row, opens a sheet that says
  what the permission is for, what Zenith does not do with it, and the steps on Android's page, with a
  picture of the Notch it unlocks (`NotchAccessSheet`), before Android's page opens.
- **Its style** follows the theme: see part 4.

*(My call)* **An overlay, not an accessibility service.** Both can draw over other apps; the overlay is
the more reliable. Play allows accessibility services only for accessibility, so a notch built on one
risks the listing; Android 13 and later hide accessibility services behind "restricted settings" for
apps not installed from Play, and Android's own page warns that the app wants full control of the
phone; and some phones switch them off by themselves. The overlay already runs on the Pixel and the
Galaxy, and works round the one thing an accessibility overlay would do better (taking a tap on the
status bar itself) with the strip under it that also takes taps.

*(My call)* **The pill at rest is on for everyone**, those who had the island on before included,
since the brief makes it the Notch's resting state; an island that was set to the camera alone before
the switch existed keeps that, and the switch turns it off.

*(My call)* Searching Settings for "battery" still finds Keep Zenith loaded first; the pill at rest is
found by "notch", "status", "percent" and "rest".

## 2. Making the Notch fit

Settings → Zenith Notch → Size and position: four sliders and a way back.

- **Width** 80% to 140% and **Height** 85% to 135% of the size it is made for this camera, so it
  grows from its middle and stays on the camera. A wider Notch also grows wider when it opens, up
  to the same limit as ever: 24 dp short of the screen's width.
- **Left and right** up to 12 dp either way, **Up and down** 6 dp up or 8 dp down: about 30 pixels
  on most phones, the brief's "a few dozen". It never leaves the top of the screen or its sides.
- **Reset to default** opens out under the sliders while anything has changed, and puts it back.
- **Live preview**: the top of this phone at the top of the page, with the pill showing while a
  slider moves even if it is off at rest; and the Notch over every app moves with it, since the
  service measures again as the fit changes.

The fit is saved with the Notch's settings in whole percent and whole dp, under keys of its own
(`fitWidth`, `fitHeight`, `fitX`, `fitY`); an early island's `width` meant something else and is
never read as one. Whatever is read back is kept within the limits (`NotchFit.of`).

*(My call)* The limits: wide enough to cover a camera that sits off centre or a pill a little
short of the lens, never so far that it reads as something you drag about, which the brief rules
out. Up is shorter than down, because up runs into the top of the screen first.

## 3. Liquid Glass, the default

Zenith's glass already was Liquid Glass (the 15 Nov spec, and "How the glass works" in the README):
translucent panes, a true blur from the wallpaper kept four times over at four softnesses, soft
shadows, a hairline of light on the edge, rounded corners, the phone's own sans and iOS blue as the
one accent. So this part makes it a theme, the first of four and the one everybody starts in
(`Themes.LiquidGlass`, id `glass`), and keeps it exactly as it was: its colours are the neutral
look's, token for token; every shape is drawn as it is given; the glass engine is untouched.

The blur, phone by phone:

- **Android 13 and up**: the AGSL lens over the softened copies of the wallpaper, and the home screen
  behind a sheet or the drawer blurred with `RenderEffect`.
- **Android 12**: the frosted copy in the panes, and the same `RenderEffect` blur behind.
- **Android 10 and 11**: no live blur; panes show the frosted copy too, and the home screen behind
  a sheet or the drawer only dims, as it always has there.

*(My call)* "Subtle blur": the blur behind sheets and the drawer stays at 20 dp, as it was tuned
against iOS; it is the theme's (`Theme.blurBehind`), so another theme can have less or none.

## 4. Themes

**A theme is data** (`data/Themes.kt`): its name and line, what its panels are made of
(`ThemeMaterial`: glass, flat, neon or retro), whether it follows Light and Dark (`ThemeTone`), its
colours in each (`ThemeColours`: the page, a panel's face, the words, the accent and a second one,
the words on the accent, the edge, the shadow or glow, a switch that is on, red for what takes
something away, the glass's veil, the words on the wallpaper, the dim behind a sheet), and its
shape and light: the blur behind a sheet, how much of the wallpaper its panels show, how much of each
corner it keeps and the most it keeps, cut corners or round, pills kept or not, the edge's width, the
shadow's softness and fall, the glow, the font. It is written to JSON and read back whole
(`Theme.toJson`, `Theme.fromJson`, version 1), every number kept within limits and every colour
parsed, so a theme from anywhere (Zenith Connect, later) never throws.

**The four:**

- **Liquid Glass**: clear glass over your wallpaper, lit at the rim. Follows Light and Dark.
- **Clean**: flat and light, nothing in the way. White cards with a hairline edge and a short soft
  shadow on pale grey, near-black words, one blue; in Dark, graphite. Inter. Nothing blurs.
- **Neon**: dark, with edges that glow. Smoked glass over a night of deep violet, cyan edges with a
  glow of their own, magenta as the second colour. Condensed type. Always dark.
- **Retro**: warm colours, chunky blocks, a pixel's edge. Cream and brown with burnt orange, thick
  outlines, hard shadows cut like paper, corners cut off straight. Mono type. Follows Light and Dark.

**Everything reads it.** `ZenithTheme` provides the theme (`LocalTheme`) and the palette made from it
for Light or Dark (`Theme.palette`). Every pane in Zenith is asked for as `Modifier.glass`, and that
now draws the theme's material (`themedPanel` for the three that are not glass), so the home screen,
the dock, the Search pill, widgets, folders, menus, questions, the edit bar, the drawer, sheets,
Settings and setup all change with it. Corners go through the theme (`corners(r)`, `Theme.shape`);
the dim behind a sheet is the theme's (`ZenithPalette.dim`); words on a pane on the wallpaper are
white on Liquid Glass and the theme's own ink on its solid faces (`wallPaneInk`); Settings' groups,
its tiles and full pages are the theme's (`themeSurface`, `themeTile`, `themePage`); the Notch takes
its edge. `ThemesTest` holds every theme to its contrast, in Light and Dark: words on a panel and on
the page at 7:1 or more, muted words at 4.5:1, words on the accent at 3:1.

**Choosing.** Settings → Wallpaper & appearance → Theme: four cards, each drawn in its own theme (its
page, a panel of its material, its accent, its font). A tap tries one at once: the whole screen
cross-fades into it (`ThemeCrossfade`: the last frame is kept as a picture and faded out over the
new theme in 380 ms, once, whatever is on screen; at once with movement reduced). The star makes it
yours: saved as `theme` in the settings, and Zenith starts in it. One you are only trying lasts until
Zenith next starts (`Themes.trying`, in memory and shared with the Notch's service), so there is
always a way back.

**A new theme** is one more `Theme` in `Themes.all`: nothing else changes, and the tests check its
contrast with the others.

*(My call)* **The brief brings themes back.** The 10 Nov spec took out the colour themes; these are
whole designs rather than colour sets, and the brief is the newer word. Liquid Glass keeps the 10 Nov
look to the pixel.

*(My call)* **Neon is always dark**; a neon sign in daylight is only tubes. Light and Dark still
apply to the other three, and the appearance page says so while Neon is on.

*(My call)* **A tap tries, the star keeps.** The brief asks for a quick switch and a favourite that
comes back on restart; trying until restart gives both without a confirm.

*(My call)* **Font: "Theme" is the theme's font.** The font setting's first choice was System; it is
now Theme, and is the phone's sans in Liquid Glass, Inter in Clean, condensed in Neon and mono in
Retro. A font chosen by name wins in every theme.

*(My call)* **Only a theme cross-fades.** Light and Dark change at once, as before.

*(My call)* Clean and Retro are flat, so nothing is blurred behind their sheets and drawer: a veil of
the page's colour comes in instead. Neon blurs a little less than glass (14 dp).

*(My call)* The glass settings (Glass panels, Tilt shine) show only with Liquid Glass, the only theme
made of glass; what they are set to is kept. In the other three nothing follows the phone's tilt, the
light on the panes or the home screen's float, since there is no glass to catch it.

*(My call)* **The Notch stays black**: it is the camera's cutout. Neon gives it a glowing edge in its
accent and Retro a solid one.

*(My call)* Corners: Clean keeps 72% of each, Neon 85%, Retro half and at most 10 dp, cut off
straight; pills stay pills except in Retro. What is content keeps its own colours whatever the
theme: the wallpaper, photos, widget colours, the quick toggles' tones, app icons.

## 5. A new setup, and Optimizing Zenith

Setup keeps its steps, their order and what each one saves; what changes is how it looks and feels,
one new choice (the theme) and one new step (Optimizing Zenith).

**How it looks.** The soft light that drifts behind setup is now over the theme's own page, and
leans with the phone in Liquid Glass (`LocalParallax`, three lights at three depths, and the phone
preview a little the other way). On the stage the controls sit on one frosted card, and it is one
card all the way through: as the step changes it takes the new controls' shape on the same spring
as their height, rather than being swapped for another, and it fades where a step has nothing on it
(while the page is built, and once it is finished). Where you are is a row of glass dots, the one
you are on drawn out into a pill of the accent. Every tap ticks (the button, the chips, the theme
cards, back); the page being built, the work being done and the hand-off each give the firmer
confirm.

**The steps.**

1. **Hello**: "hello", large, fading and scaling in as before and cycling through the languages, with
   a glow behind it that comes up with the first word and breathes slowly on the backdrop's clock.
   The button says **Get started**.
2. **Your name**: stop typing for 650 ms and setup answers, "Nice to meet you, Gabriel.", popping up
   under the field on a bouncy spring, with a tick and a ring of light from behind the words. Its
   room is kept, so nothing moves when it comes. The line above the question is the hello alone now,
   so the name is said once. Skip still skips.
3. **Pick a theme**: the four themes as cards a quarter of the row wide, each drawn in itself
   (`ThemeMiniature`, the same drawing as Settings' theme cards), with Auto, Light and Dark under
   them. A tap turns all of setup into that theme at once: setup is inside `ZenithTheme`, so it is
   the same cross-fade as Settings', the phone preview included. Building saves it as your theme.
4. **What goes on it?** and 5. **Putting it together**, as before.
6. **Optimizing Zenith**, the moment the page is built.
7. **Zenith Plus** and 8. **Three quick moves**, as before; then **Take me home** gives a short
   celebration (a ring of light from the button, the phone lit and a band of light across it) and
   after 260 ms the phone grows into the home screen as setup's light and glass dissolve round it.

**Optimizing Zenith does the work, in order** (`OptimizeJob`, run by `ZenithViewModel.optimize` in
the view model's own time, so it carries on whatever setup does):

- **Apps**: every app and its name read again, as the drawer lists them.
- **Icons**: every app's icon drawn at exactly the drawer's size and kept in memory (`IconLoader.warm`),
  counted one by one ("Caching app icons… 42 of 118").
- **Drawer**: each icon's main colour, for By colour (`IconLoader.colours`, now counting as it goes).
- **Glass**: the wallpaper's softened copies, which the glass and the frost show ("Preparing the
  glass"; "Preparing your wallpaper" in a theme not made of glass).
- **Graphics**: the glass's shader and the blur behind a sheet drawn once by setup's screen, at 1%
  opacity in a corner of the card (`GraphicsWarmUp`): Liquid Glass's lens over the wallpaper's copies,
  or the theme's own panel, and the blur at the theme's strength. A phone compiles each kind of
  drawing the first time it is asked to; this way it is setup that waits, not the first sheet.
- **Save**: what setup chose, on disk rather than on its way there (`SettingsStore.flush`).

The card shows the job in hand in words, a glass channel that the accent fills, the percentage and
the time left. Both numbers come from the work (`Optimizing`, tested in `OptimizingTest`): each job's
units are weighed by what one costs (an icon is the yardstick; reading the apps is 4, the glass 6,
the graphics 2), the percentage never says 100 before everything is done, and the time left is the
time so far spread over what is still to do, said as a person would ("About 3 seconds left"), and
not guessed at all until a twentieth of the work and a third of a second are behind it.

*(My call)* **Light or dark moved onto the theme step**, under the cards, rather than being a step
of its own: they are one decision about how it looks, and setup keeps its number of steps.

*(My call)* **Neon says it is always dark** on the theme step while it is picked, as the appearance
page does; Light and Dark are kept for the other themes.

*(My call)* **Where the work goes.** Right after the page is built, before Plus: the home screen it
readies exists by then, and what is left of setup (Plus, the tips) gives it time to finish if you
skip ahead. It starts by itself; there is nothing to press.

*(My call)* **It can be skipped**, three seconds in: "Finish in the background", in the button's
place, moves on while the work carries on. The brief asks that setup never slow you down; nothing
is lost by not watching it.

*(My call)* **The drawer's own warm-up waits for it.** Before, the drawer started drawing its icons as
soon as the app list was known, setup or not. During setup it now waits until Optimizing Zenith has
drawn them (or setup is done), so the two never draw the same icons on the same thread and the count
on the card is the real work, not a race with a copy of itself.

*(My call)* **Lit for a moment at each end.** "All set" stays up 450 ms at 100% before the finished
page takes its place; the celebration before the hand-off is 260 ms. With animations off there is
neither: the step ends when the work does and the hand-off is immediate.

*(My call)* **The tilt only in Liquid Glass**, as on the home screen: in the other themes nothing
follows the tilt. Neon and Retro light setup in their own colours; Liquid Glass and Clean keep white
light. Neon's phone preview glows in its accent.

*(My call)* "hello" is lower case, like the rest of the words it cycles through, and as the iPhone
writes it.
