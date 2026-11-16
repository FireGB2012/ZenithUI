# Depth wallpapers, and seven more colour sets

2026-11-16. The first item on Gabriel's list after the Liquid Glass work: "new wallpapers just like
Apple's and Samsung's, like cool and different colours".

## Why

The thirty-three designs Zenith paints are flat: pools of light, lines, shapes and scenes, drawn
with Android's own gradients. The wallpapers that come with an iPhone or a Galaxy have a body to
them: petals laid over one another, glass, satin, a ribbon that turns in the light. And the six
free colour sets left whole parts of the colour wheel out: nothing all blue, green, yellow or red.

## What changes

- **A Depth shelf**, first in the picker, with eight designs:
  - **Iris**: nine petals turning out from a bright heart, each over the last and under the next.
  - **Layers**: seven hills of colour one behind the next, each with a sheen on its crest and a
    shadow on the one behind.
  - **Satin**: long folds of cloth, with a broad sheen and a sharp one, and a second light in the
    other colour.
  - **Marbles**: glass marbles at different depths, the far ones soft and sunk in haze.
  - **Liquid**: capsules of clear glass over pools of colour, bending what is behind them at the rim.
  - **Twist**: a wide ribbon falling through the screen and turning over once, so both its faces show.
  - **Loop** (Plus): two glossy rings, one through the other.
  - **Eclipse** (Plus): a sphere with the light behind it, a corona at night and a pearl by day.

  Two are Plus, as on every shelf, and come after the free ones *(my call: the two that took the
  most work)*.
- **How they are painted.** Unlike the old designs they are lit as things in the world are: light
  is added and scaled as light (linear RGB) and only turned into the screen's values at the end,
  and the colours run between the set's two accents round the colour wheel the short way, in OKLab
  (`DepthKit`'s `Spectrum`), carried a quarter past each end so two close accents still give a
  range. A gradient from blue to yellow goes through green this way, where mixed as RGB it goes
  grey. A colour beyond what the screen can show gives up chroma, never its hue. They are worked out
  a pixel at a time (`Easel.pixels`), the rows shared between the phone's cores, with an ordered
  dither so the long gradients do not band; the ribbon is drawn as a mesh of 10,000 pieces, nearest
  last, over a blurred shadow. As before, everything is placed as a fraction of the canvas, nothing is
  random, and the light picture is drawn for a pale page, not washed out from the dark one.
- **Seven new colour sets**, so there is one for every colour a phone's own wallpapers come in:
  Ocean (all blue), Lime, Citrus, Peach, Cherry, Berry and Lavender. Peach and Lavender are made for
  daylight, as Frost is. The free sets now run round the colour wheel from Midnight, which stays
  first: Midnight, Ocean, Frost, Aurora, Lime, Citrus, Peach, Sunset, Cherry, Berry, Sakura,
  Lavender, Graphite.
- **The picker** needed nothing new: it reads the shelves, the designs and the colour sets from
  `WallGroup`, `WallStyle` and `Palettes.all`. It now says "41 designs".

## What does not break

- The thirty-three old designs are untouched, so `WallpaperFollow.ART_VERSION` stays and no
  wallpaper is painted again on the update. The old colour sets keep their ids and their colours to
  the bit. Designs are remembered by name and sets by id, so adding them first changes nothing that
  is stored.
- Every design, old and new, in every colour set (the thirteen free ones and the five Aura ones),
  light and dark, keeps to `WallpaperArtTest`'s rules: the light picture clearly lighter than the
  dark, the lower part light enough for dark labels or dark enough for white ones, the status bar
  readable, and the same picture every time.

## Checked

- Unit tests: `DepthKitTest` (the colour sums: OKLab both ways, the short way round, a grey end,
  the gamut, ramps, the sky), `WallStyleTest` (41 designs, five shelves, Depth first, nine for Plus)
  and `PaletteToneTest` (thirteen free sets, Midnight first, every id its own; the tone rules now
  cover the new sets).
- There is no emulator in the cloud, so `WallpaperArtTest` itself did not run. Its rules were run
  on the JVM screenshot rig (Robolectric with native graphics) for all 41 designs in all 18 sets,
  light and dark: none failed. The closest is Liquid's dark picture under the icons in the brightest
  sets: 0.42 in Tide, against the rule's 0.45. A full-size picture (1080 by 2400) of a new design
  took from 60 to 660 ms there over two runs on four cores, against the test's 1500.
- The home screen and the picker were shot on that rig with the new designs.

## Left for later

- The names under the home screen's icons are white in Light too, as on an iPhone (from the Liquid
  Glass work), so on the palest wallpapers they are faint. Dark names over a light wallpaper would
  need a change there, not here.
- The designs are still pictures. Apple's move a little as the phone wakes; a slow drift would be a
  later step.
