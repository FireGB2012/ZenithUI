# Wallpapers that move, and eight more Depth designs

2026-11-18. Gabriel: "U know the wallpapers we made that looked like they were from apple and
samsung add a bench more of these and also add some that are moveing like 3 that are idle moveing
and 2 that are like still ok th lock screen and when u open the phone into home screen. It moves in
some kinda way so it looks super cool".

## Why

The Depth shelf (`specs/2026-11-16-depth-wallpapers-design.md`) gave Zenith wallpapers with a body
to them, but they are still pictures, and it left "a slow drift" for later. The wallpapers that come
with an iPhone or a Galaxy move: some drift while you look at them, and some sit still on the lock
screen and come to life as the phone unlocks.

## What changes

- **A Live shelf**, first in the picker, with five designs that move. Anyone can have them.
  - Drifting on their own while they can be seen (**idle**):
    - **Hover**: glass marbles hanging in a glowing haze, each on a slow loop of its own, the near
      ones moving further than the far, while two pools of colour wander behind them.
    - **Tide**: swells of colour rolling in from a glowing horizon, the far ones small and slow,
      the near ones broad and quicker, each with a sheen on its crest.
    - **Cosmos**: a spiral galaxy at a slant, turning slowly among twinkling stars, and now and
      then a shooting star. By day it is ink on a pale sky.
  - Still on the lock screen, opening as you unlock into the home screen (**unlock**):
    - **Lotus**: a closed bud that opens, the outer ring of petals first, each petal lengthening,
      widening, unwinding and springing a little past open before it settles, while the heart
      glows up (1.5 s).
    - **Daybreak**: a total eclipse that turns into day: the moon slides away, the sun bursts out
      at its edge as a diamond ring, the corona gives way to sunlight, the stars go and the sky
      lightens (1.8 s).
- **How they move.** A design that moves is Android's live wallpaper (`live/LiveWallpaper`, a
  `WallpaperService` in Zenith's own process), since a still wallpaper cannot move under other
  apps or on the lock screen. Each design paints its layers once, lit as the Depth designs are
  (`LiveScenes`, `LiveKit`), and then only moves, turns and fades them, on a thread of its own with
  the hardware canvas. The rules for when it moves (`LiveWall`: `LiveUnlock`, `LivePace`) are
  plain Kotlin with unit tests:
  - It only draws while it can be seen. Idle designs run at 60 frames a second in Full and 30 in
    Lite (the Performance setting, Auto choosing as it does for the home screen); with Battery
    saver on, or movement reduced, they hold still.
  - An unlock design is posed shut as the screen goes off. Unlocked, it opens once, as the home
    screen shows, and then holds still: it draws nothing more until the next unlock. With
    movement reduced it is simply open.
  - Changing colours or design fades the new one in over the old (0.6 s), as a still design does.
  - On a phone where the hardware canvas will not lock, it is drawn in software and holds still.
- **Setting one.** Android puts up a live wallpaper only once you confirm it in its own screen, so
  the first time, "Set live wallpaper" opens that screen (its preview of Zenith Live with its own
  Set button, or its list of live wallpapers where there is no preview). Back from it, Zenith keeps
  the choice if the live wallpaper is up and puts the old design back if not. After that, choosing
  another live design is instant: the live wallpaper reads the settings.
- **Lock screen.** Where the lock screen shows the live wallpaper itself (a lock screen with no
  wallpaper of its own does), nothing more is needed. Where it does not and "Home and lock" is
  chosen, Zenith puts a still on the lock screen: an unlock design in its lock pose, so that
  unlocking carries straight on into its opening, and an idle one as it starts out. That still is
  only ever painted over a lock screen Zenith put there itself.
- **Light and dark** work as for a still design: following, it changes with Zenith; not
  following, it keeps the light it was set in (kept with the live wallpaper's own preferences, so
  the service reads it without the app).
- **The glass** bends a still of the live design at its home pose (720 pixels wide; for the idle
  designs 480 and softened, since they are never quite where a still of them would be).
- **The picker** marks live designs with a LIVE badge, and their preview moves (30 frames a
  second, held still with movement reduced): an unlock design loops from home pose back to its
  lock pose and opens again. Its caption says how it moves and that Android asks once.
- **Eight more Depth designs**, so the shelf has sixteen (`WallDepthMore`), lit and coloured as the
  first eight:
  - **Fluted**: reeded glass over pools of colour, each rib a lens, red bent a touch more than blue.
  - **Orb**: a glass ball over slanted bands of colour, bending them upside down, with a caustic
    in its shadow.
  - **Arcs**: five glossy tubes curving in from the top corner.
  - **Pond**: rings spreading from two drops on still water, catching the sky.
  - **Dew**: drops on a window, large and fine, each a little lens turning the colour behind it over.
  - **Fan**: tinted glass petals fanned out from below the screen, each over the last, with a
    bevel of light, a sheen across them and soft shadows.
  - **Stack** (Plus): frosted glass cards stacked at a slant, the colour beyond softened through
    each, the top edges catching the light.
  - **Gem** (Plus): a round brilliant seen from above, cut as one is (table, stars, kites and girdle
    facets), some facets flashing, some throwing the colour set's spectrum as fire, and the cut seen
    again through the table with its dark arrows; a glow, rays and glints.

  As before, the two that took most work are Plus and come after the free ones. Plus now has eleven.

## What does not break

- The forty-one designs that were there are untouched, so `WallpaperFollow.ART_VERSION` stays and
  nothing is painted again on the update. Designs are remembered by name, so putting the new
  shelves first changes nothing stored.
- A still wallpaper is set exactly as before. Choosing a live design never paints over a photo or
  anything set in another app without the confirm screen, and the lock screen still is only
  repainted where Zenith put it.
- Every new design, live ones in their still (and, for unlock ones, lock) pose too, keeps to
  `WallpaperArtTest`'s rules in every colour set, light and dark.

## Checked

- Unit tests: `LiveWallTest` (21: when an unlock design opens, holds and replays, settles with
  movement reduced; frame pacing at 60, 90 and 120 Hz screens; the easing curves) and
  `WallStyleTest` (54 designs on six shelves, Live first then Depth, three idle and two unlock, only
  the Live shelf moves, eleven for Plus).
- There is no emulator or phone in the cloud, so the live wallpaper service itself has not run on
  Android, and `WallpaperArtTest` did not run as an instrumented test. Its rules were run on the
  JVM screenshot rig for all thirteen new designs in all eighteen colour sets, light and dark: none
  failed. A full-size picture (1080 by 2400) of a new design took 55 to 160 ms there.
- Screens and clips were made on that rig: the home screen over the new designs, the picker's Live
  and Depth shelves, a live design's moving preview, and each live design moving.

## Left for later

- Untested on a phone: how One UI's lock screen treats a live wallpaper (it may show its own
  still), the confirm screen on Samsung, and the frame rate on the A55. If One UI's lock screen
  does not show the opening, the lock screen still is what carries the pose over.
- The moving designs do not yet follow the home screen's pages (Android's offsets are turned off),
  nor tilt with the phone.
