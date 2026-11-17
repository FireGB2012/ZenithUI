# Liquid glass folders

- Date: 2026-11-17
- Branch: `claude/project-thread-o3yi5o`, off `claude/project-thread-xktrin` (df9c96d, PR #15: the
  One UI close fix and Aura levels)
- Follows: part 3 of `2026-11-11-ios26-folders-menus-edit-design.md` (folders), and the feel of
  `2026-11-16-liquid-coming-home-design.md`
- Status: built. Gabriel sent a screenshot of an iOS 26 folder open ("Social": a big pane of frosted
  glass with a bright hairline round its edge, a bold white name above it, three by three large
  icons with their names, red badges with numbers, two page dots) and asked: "make folders like this
  with liquid animation".

## What the screenshot shows, and what Zenith had

| | iOS 26, as sent | Zenith before |
| --- | --- | --- |
| Name | Big and bold, white, above the pane, in line with the first icon | Small, in the pane, with "N apps" beside it |
| Pane | One size whatever is in it, a little over three quarters of the screen wide, very round corners | As tall as its apps, up to 340 dp, then it scrolls |
| Apps | Three by three, large, names under them, pages to the side | Three or four across, standard 60 dp icons, one long list |
| Under the apps | Page dots, only with more than one page | A hint: "Long press an app to move it back onto the home screen." |
| Glass | The same glass in Light and in Dark, white names | Followed the mode, with the mode's ink |
| Behind | The home screen blurred, hardly dimmed | Blurred and dimmed by 38% |
| The tile | Nine tiny icons, three by three | Four, two by two |

## What changes

### The open folder (`FolderPanel`)

1. **The name** is written above the pane in white, 32 sp, bold, a hair tight (-0.6 sp), with the
   soft shadow names on the wallpaper have, its first letter in line with the first column's icon.
   It is still a field: tap it to rename, as before, and while it has the keyboard it stands in a
   capsule of glass with a clear button, as iOS's does in edit mode. The "N apps" count is gone.
2. **The pane** is one size, whatever is in the folder (`FolderSize.of`): 78% of the screen's width,
   kept between 280 and 360 dp (320 dp on a 411 dp Galaxy A55), with 36 dp corners. Inside it 11 dp
   from each side, three columns; the icons are 70% of a column, between 52 and 76 dp (about 70 dp on the
   A55, about the home screen's own); each row is the icon, 6 dp, its name's line (20 dp) and 14 dp;
   24 dp above the first row and 18 dp under the last. The pane and the name above it stand in the
   middle of the screen together, and go up with the keyboard.
3. **Nine to a page** (`FolderGrid`): apps 1 to 9 on the first page, three by three, in order across
   then down; 10 to 18 on the second, and so on. Swipe across the pane for the next page. Page dots,
   7 dp, 8 dp apart and 13 dp off the pane's foot, white for the page you are on and faint for the
   rest, only when there is more than one page.
4. **The apps** are the home screen's icons at the pane's size, with their names in white under them
   and their notification badges (red, with the number when Badges is set to Count). A tap opens the
   app, which grows out of its icon there; hold one and it goes back onto the home screen, as
   before. One taken out fades where it stood and the rest glide up to close the gap.
5. **The glass** is the wallpaper's own (`OnWallpaper`): the same clear glass in Light and in Dark,
   white names, as the hold menu's is (part 1 of the folders spec says why). It has the body the
   menu has (`solid` 0.55) so the names are read over a busy wallpaper, and the dark grey body
   under it where Zenith has no copy of the wallpaper. Its hairline rim is Zenith's Apple glass, lit
   up-left and opposite: no goo (see `2026-11-15-apple-liquid-glass-design.md`).
6. **Behind it** the home screen is out of focus as before (14 dp), and dimmed only 12%. Where
   nothing is blurred (Lite, Clean, Retro, Android 11) it is dimmed 38% as before, so the icons
   behind are quiet.
7. **The hint is gone**, as the mockup Gabriel approved has it. Nothing is lost: holding an app
   inside still takes it out.

### The closed folder (`FolderTile`)

The Grid style shows nine tiny icons, three by three, as iOS's folders do: each 22% of the tile,
5% of it apart, 12% in from its edges, in the order the open folder has them. Stack and Cover are as
they were. Its badge, its colour and its glass are as they were.

### Liquid: opening (`FolderPour.frameIn`)

The folder's glass tile becomes the pane: the open pane is one drop of glass that starts as the tile,
exactly where it is and as round, and spreads out to the pane, the springs worked out rather than
stepped, so any frame can be asked for:

- **Wide, then tall.** Its width leads on a spring of 21.5 rad/s damped at 0.72; its height follows
  40 ms behind, 20 rad/s damped at 0.7. So it opens a touch wide and then a touch tall, about 4%
  past each, and settles: it sloshes once, as the approved mockup's twist 4 has it. Never a wobble:
  no spring under 0.7.
- **Across the screen** its middle travels from the tile's to the pane's at 22 rad/s damped 0.86:
  hardly past.
- **Its corners** go from the tile's to 36 dp as it widens.
- **What is in it zooms with it.** The first page is drawn in the drop at the drop's width over the
  pane's, about the drop's middle, and cut to the drop's outline. Each app starts as the tile's tiny
  icon, exactly where it is and as big (`FolderPour.flight`): the tiny icons ride the drop as it
  spreads, three by three across its width and down its height, so they never leave it, and each
  app flies out of its tiny icon into its place on a spring of its own (26 rad/s, 0.72), nearest the
  finger first, 16 ms apart: the drop's light reaches the near ones first. Nothing fades in: the
  tile's icons are in the drop from the first frame. Names come in once an app is 70% of the way
  out and of its size.
- **The name** above comes in from 60% of its size about its left edge, 60 ms in, on a spring of
  22 rad/s damped 0.8, and fades in over 180 ms.
- **Light.** A ring of light runs out from the finger as it opens (the home screen's one
  `GlassRing`, the light the coming-home wave carries, 600 ms, 70% as bright), so the drop's rim and
  face catch it as they spread, as every pane does.
- It is still after about 0.5 s, and the clock stops at 0.62 s, by when it is within half a pixel.
  The Speed slider of Motion & feel runs it faster or slower, as it does opening apps.

### Liquid: closing (`FolderPour.frameOut`)

From wherever the drop is (a folder closed while still opening goes back from there), it draws back
into its tile over 300 ms, slow out, quicker, and slow onto the tile (a cubic in and out), with no
overshoot. The apps fly back with it into the tile's tiny icons, each from as far out as it had
come, landing in them as the drop lands; the names and the name above go first, in the first 35%. Over the last 70 ms the drop fades and the tile shows through. Then the
tile **gulps** (`FolderPour.gulp`): 7% wider and 6.5% flatter, then 2.5% narrower and taller, and still,
over 340 ms. The blur behind clears as it goes, not after.

### Lite, and movement reduced

- **Lite** (Motion & feel > Performance): no ring of light, no blur (the 38% dim instead). The
  drop, the zoom and the gulp are the same: they draw only the folder, and only while it moves.
- **Movement reduced**: no drop, no zoom, no gulp; the pane fades in and out where it stands, in
  150 ms.

## What would be a failure even if it works

- Glass that differs between Light and Dark, or a body on it that hides the rim.
- Anything that wobbles: a spring under 0.7, a drop that swings more than once.
- The home screen redrawing while the folder is open and nothing moves.
- Something a folder could do before that it can no longer do: rename it, open its apps, take one
  out, see its badges.

## Not in this step

Part 3's other twists wait for their own step: holding an app inside for its menu and dragging it
out past the edge (twist 4's stretch), the bridge of glass between two apps that makes a folder
(twist 3), and the waiting app's face on the folder's badge (twist 5).

## On Gabriel's phone

The badges need notification access, which the tap-install copy cannot have (Play Protect refuses a
sideloaded notification listener), so on his A55 there are no badges until the full APK goes on with
adb. Nothing here has run on a phone: only the unit tests and the JVM screenshot rig.
