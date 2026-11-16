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
