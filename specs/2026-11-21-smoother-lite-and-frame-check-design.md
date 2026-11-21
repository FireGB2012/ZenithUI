# Smoother in Lite, and the frame check

- Date: 2026-11-21
- Branch: `claude/project-thread-vrtov1` (PR #28), on top of 4.0.1 (116); this is 4.0.2 (117)
- Status: built. Gabriel, on his Galaxy A55 in Lite, minutes after installing 4.0.1 by tapping it:
  "my phone is on lite mode and still runs like idk 20 fps and is slow".

## What was looked at

His phone was not plugged into his PC, so nothing could be read off it. The JVM perf rig
(`tools/jvm-perf-rig/` in project files) at the A55's size in Lite put Zenith's own thread where
the first Lite fix (2026-11-16) left it: no regression there, nothing composed or redrawn at rest,
and the blurred shadows under the labels cost under a millisecond. What the rig cannot see is the
drawing side on the phone itself, and that is where these changes go:

- **A moving wallpaper shares the drawing with Zenith.** The live wallpaper runs in Zenith's own
  process, and Android draws a process's windows on one drawing thread, with one graphics chip:
  every frame of a moving design (30 a second in Lite) is drawn by the same thread, in the same
  moments, as the home screen's frames while it moves.
- **Fades drew the screen twice.** A page turning (the default turn fades it to 60%), and the home
  screen going back behind the drawer or an opening app, faded a layer the size of the screen:
  Android draws all of it into a picture first and then fades the picture, on every frame.
- **The glass heard every frame of a swipe.** The home screen told the glass where across the
  wallpaper the pages were on every frame, and every pane that reads it was drawn again, although
  from Android 14 the wallpaper stays in the middle whatever page is shown, and so does the glass.
- **A copy installed by tapping it may run uncompiled at first.** Installed from Play, Zenith is
  compiled with its baseline profile as it installs. A copy installed by tapping it hands the
  profile to Android, which may only compile it the next time the phone sits charging and unused.
  Whether that is so on One UI's Android 16 is not known here, so the frame check says what
  Android says.

## What changed

- **The live wallpaper makes way** (`LivePace.holds`, `LauncherMotion`): in Lite, a design with a
  motion of its own holds still while Zenith is moving (four of its frames within a quarter of a
  second), and carries on from where it stopped: its clock waits with it, so it does not jump. An
  unlock design's opening, a fade from one design into another and Android's preview never wait.
  Full is unchanged.
- **Lite fades in place** (`Performance.fadesInPlace`): the page turn and the home screen's own
  fades use `CompositingStrategy.ModulateAlpha`, so each thing is drawn a little lighter and no
  picture of the screen is drawn first. The two only differ where things overlap while they fade.
- **The glass leaves the pages' offset alone from Android 14** (`glassFollowsPages`):
  `systemWallpaperOffset` was already the middle there, so nothing drawn changes; Connect's ground,
  which read the offset without it, now lines up with the real wallpaper on every page.
- **The frame check** (Settings, Motion & feel, Performance): `FrameRecorder` counts every frame of
  Zenith's window off the main thread (FrameMetrics), tells `LauncherMotion`, and keeps the last
  600 while no page covers the home screen. `FrameReport` turns them into frames a second, the
  share on time (a step of more than half as long again as the screen gives a frame is late) and
  how long each part takes, nine frames in ten (Zenith's own thread, the drawing thread, the
  graphics chip), and names the slowest. Only runs of six frames or more count, each within
  100 ms of the one before it being done, so a clock ticking over is not movement and the wait
  between two swipes is not a slow frame. `CompiledCode` says whether Android has compiled
  Zenith (`ProfileVerifier`). With a moving wallpaper on, it says so. A screenshot of it is what
  to ask him for next time it is slow.

## What the rig shows after

The JVM perf rig in Lite at the A55's size, 4.0.1 against 4.0.2:

| | 4.0.1 | 4.0.2 |
|---|---|---|
| Two page swipes: layers recorded again | 367 | 145 |
| Two page swipes: writes of the wallpaper offset | 73 | 0 |
| Drawer opening: layers recorded again | 243 | 242 |
| Drawer closing: layers recorded again | 194 | 193 |

The panes off the page that moves (the dock, Search) are no longer drawn again on every frame of
a swipe. The clock widget's ticks follow the wall clock, not the rig's own, so how many of them fall
in a run differs from run to run; they are left out of the comparison. Fading in place and the
moving wallpaper's pause are on the drawing side, which the rig does not time.

## Not changed

Lite keeps all it kept: the glass, its lens, its rim and its shadow. Nothing about Full changed but
the glass offset on Android 14 and later, which draws the same pixels.

## Untested on the A55

All of it. The frame check is there to say what his phone does.
