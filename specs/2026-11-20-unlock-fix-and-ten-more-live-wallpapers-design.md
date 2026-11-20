# Unlock designs that open on a Galaxy, and ten more that move

2026-11-20. Gabriel: "The open on open phone wallpapirs dose t work and make some more wallpiprs
that move use brainstorm to make like 10". Ten ideas went to him on a card; he picked "Build all
10".

## Why

Lotus and Daybreak, the two Live designs that open as the phone is unlocked
(`specs/2026-11-18-live-and-more-depth-wallpapers-design.md`), never opened on his Galaxy A55: they
went to their lock pose when the screen went off and stayed shut. And the Live shelf had only five
designs.

## What was wrong

The word that the phone was unlocked (`ACTION_USER_PRESENT`) is sent by System UI, not by Android's
own process. The live wallpaper registered its receiver as not exported, and a receiver that is not
exported only hears the system's own broadcasts. The screen going off did arrive, as the system
sends that itself, so the designs closed and stayed closed. Their one other way of opening, coming
into view off the lock screen, happens while the lock screen is still on its way out, when it still
counts as up, so on One UI that missed too.

## What changes

- **Unlock designs open again** (`LiveLock` in `LiveWall`, `live/LiveWallpaper`; commit 45dc7ca).
  They now hear the unlock three ways and open on the first:
  - the receiver is exported. Screen on and off, the user being present and Battery saver are
    protected broadcasts, which only the system can send, so this lets nothing else in;
  - Android's own word to the wallpaper that the lock screen is starting to go
    (`android.wallpaper.keyguardgoingaway`, through `Engine.onCommand`), the moment to start, and
    its word that the screen is going to sleep, to close at once;
  - while one waits on show at its lock pose, it looks at the lock screen ten times a second and
    opens once the screen is on and the lock screen gone.

  A new surface also tries the hardware canvas again, rather than holding still for good after the
  last one would not give it.
- **Ten more Live designs**, so the shelf has fifteen. All free, each light and dark in every colour
  set, made as the first five were: layers painted once (`LiveScenesDrift`, `LiveScenesUnlock`),
  then only moved, turned, faded or bent along a mesh each frame. Nothing is worked out a pixel at a
  time while it moves, so Lite (30 frames a second) and Full (60) pace them as before.
  - Drifting on their own while they can be seen (**idle**):
    - **Borealis**: curtains of the northern lights over a dark ridge, their folds rolling along
      them and their rays swaying, under the stars. By day, pastel veils over snowy hills.
    - **Petals**: blossom petals tumbling down, the far ones small, sharp and slow, the near ones
      large, soft with being out of focus, and quicker, over out-of-focus blossom.
    - **Kaleido**: a kaleidoscope of cut glass, three rings of facets turning slowly against each
      other round a jewelled heart, a sheen that stays put making the facets flash as they pass.
    - **Flow**: broad satin ribbons rolling across the screen, turning over here and there to show
      their other face.
    - **Jelly**: jellyfish drifting up through deep water, bells pulsing, threads waving, specks in
      the light from above. They glow at night and are glass by day.
    - **Clouds**: a sky of soft clouds sliding past at three depths, lit by the moon under the
      stars; by day, by the sun.
  - Still on the lock screen, opening as you unlock (**unlock**):
    - **Planet**: a ringed planet far off; it swoops up close, its rings sweeping wide, the stars
      streaming past and the sun flaring over its edge (1.8 s). By day it is pastel, lit from in
      front.
    - **Aperture**: a camera lens's iris shut tight with a pin of light; its seven blades swing
      open, the light pours out in rays and settles to a glow (1.5 s). The blades are drawn as
      plain shapes each frame, as they change shape as they open.
    - **Gyro**: four glass rings tipped every which way round a glowing core, like a gyroscope;
      they swing into line, inner ones first, rock past and settle nested in one plane, seen a
      little from above, and the core flares (1.7 s).
    - **Ink**: a drop on still water; it bursts, a ripple runs out, a cloud of colour billows up
      and tendrils of ink curl out of it and unwind into a flower of ink (2 s).

## What does not break

- The fifty-four designs that were there are untouched, so `WallpaperFollow.ART_VERSION` stays and
  nothing is painted again on the update. Designs are remembered by name, and the new ones go at
  the end of the Live shelf's idle and unlock rows, so nothing stored moves.
- The lock screen still, the glass's still, the picker's LIVE badge and moving preview, light and
  dark, and Battery saver and reduced movement all work from a design's `live` kind, so the new
  ones get them with no change.

## Checked

- Unit tests: `WallStyleTest` (64 designs; the Live shelf is fifteen, nine idle then six unlock)
  and `LiveWallTest` (26, five new for `LiveLock`).
- There is no phone or emulator in the cloud, so the live wallpaper service has not run on Android
  here, and `WallpaperArtTest` did not run as an instrumented test. Its rules were run on the JVM
  screenshot rig for all ten new designs in all eighteen colour sets, light and dark, at their
  still and, for the unlock ones, at their lock pose, half way and home pose: none failed.
- Cost on the rig (software canvas, full size 1080 by 2340): painting a design's layers took 41 to
  198 ms; a frame took 31 to 82 ms, against 39 to 108 ms for the first five (Tide 108, Lotus 91).
  On the phone a frame is drawn with the hardware canvas, which moves bitmaps for almost nothing.
  The closest any came to a rule was Clouds at night in Tide's colours, whose lower part measured
  0.436 against a ceiling of 0.45.
- Sheets of every design (lock and home, or two moments, dark and light, in Midnight, Sunset and
  Ocean) and a clip of each moving were made on the rig.

## Left for later

- Untested on the A55: that the unlock designs now open there (the one thing this was for), and
  the frame rate of the new designs.
- The moving designs still do not follow the home screen's pages, nor tilt with the phone.
