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
