# The lag, and Lite

- Date: 2026-11-16
- Branch: `claude/project-thread-063v1v`, off `claude/project-thread-3fx0d9` (43eb5b4, after #12)
- Status: built. Gabriel, on his Galaxy A55 (Exynos 1480, 120 Hz): "there is till crazy lag its
  like 20 fps at all times".

## What was measured

There is no phone here, so the home screen was run on the JVM (Robolectric with native graphics,
the rig in project files under `tools/jvm-perf-rig/`), frame by frame: what was composed, what was
written, which layers were recorded again, how long the UI thread took, and what one full render
of the window costs on the CPU. Times on the JVM are not a phone's, so only the comparisons count.

| | before | Full now | Lite now |
|---|---|---|---|
| Two page swipes: composables run | 87,006 | 4,505 | 4,557 |
| Two page swipes: layers recorded again | 783 | 368 | 370 |
| Two page swipes: UI thread p90 | 7.3 ms | 1.6 ms | 2.4 ms |
| Drawer opening: composables run | 12,576 | 1,788 | 2,360 |
| Phone held in the hand, 240 frames: frames drawn again | (every one the tilt moved) | 37 | 9 |
| Phone held in the hand: layers recorded again | | 346 | 9 |
| Render, glass widgets (CPU) | 682 ms | 573 ms | 505 ms |
| Render, glass icons (CPU) | | 549-573 ms | 427-441 ms |

## Why it lagged

- **Every frame of a swipe composed the whole home screen.** The pager's page content read
  `currentPageOffsetFraction` while composing, every icon wrote where it was on screen with
  `onGloballyPositioned` into state on every frame its page moved, the home screen read
  `currentPage` (for the page overview and the drop preview) and the dock worked out its spacing
  from where it was on screen. Now the swipe is read only in the page's and the icons' layers, the
  icons work out where they are when asked (`Placed`, `onPlaced`), and the few readers of the page
  in view read it where they are.
- **Holding the phone redrew everything.** The tilt light and the floating icons move on every
  frame the phone moves, which in a hand is always: every pane of glass, every icon's light and
  the shadows were drawn again at 120 Hz.
- **The glass worked out its rim everywhere.** The lens found which way the rim faces with four
  more distances to the outline at every pixel of every pane. Now it is worked out exactly
  (`rimNormal`), and only within the band or 24 dp of an edge with a rim; the same pixels to 1/255.
- **The music bars composed again on every frame** while something played. Now they are read
  while drawing.

## Performance: Auto, Full and Lite

In Motion & feel. Saved in the config (`performance`), but not in a Look: it is about the phone.

- **Full** is all of Liquid Glass.
- **Lite** keeps the glass, its lens, its rim of light and its shadow, and lets go of what is
  drawn again on every frame something moves: no tilt sensor at all (the light rests up and to the
  left, nothing floats), no split of colour at the rim, the blur behind a folder, Search, an
  opening app and the drawer becomes the dim that Clean and Retro have, the light on the icons is
  drawn once for each outline and size and laid over each icon, and the music bars and the
  charging glint stand still. A phone lying in your hand draws nothing until something changes.
- **Auto**, where everybody starts, is Lite on a phone whose graphics would struggle:
  - by its chip, before a frame is drawn (`Performance.byChip`, from `Build.SOC_MODEL`, or
    `Build.HARDWARE` where that says nothing): Lite for the Galaxy A series' Exynos (the A55's is
    `s5e8845`), Snapdragon 4, 6 and the older 7s, Dimensity below 8000 and Helio, Unisoc, and any
    phone with less than 4 GB; Full for the Exynos 2100 and up, Snapdragon 8 and the newest 7s,
    Dimensity 1200, 8000 and 9000, and Tensor;
  - for a chip it does not know, by its frames (`PhonePerformance`): after 90 frames of warming up
    it watches 240, and if more than a quarter of them took half as long again as the screen gives
    a frame, it is Lite. That is kept on the phone (`zenith_device`), never in the config, so the
    phone is not made to struggle through it again each start; a phone that kept up is watched
    again each start.

## Left as it was

- The glass's shadows under icon-shaped panes are blurred by Android's own drawing, which on
  Android keeps the blurred shape however the pane moves, so drawing them once ourselves would add
  a copy for nothing.
- Nothing here has been on a real phone. Gabriel's A55 starts on Lite by its chip, and Full is one
  tap away to compare.
