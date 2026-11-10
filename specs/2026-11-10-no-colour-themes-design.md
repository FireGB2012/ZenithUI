# No colour themes, and glass that is the same in Light and Dark

2026-11-10. Gabriel's answers, given while part 1 of the iOS 26 home screen was on the emulator.

## Why

Seeing part 1 (the new grid, the Search pill and the glass dock, with setup, the widgets' insides and
the drawer still the 4 Nov ones), Gabriel said it "just looks like the old stuff" and "remove the
colour theming stuff". Asked how far, he chose all of it: the six palettes go from the whole app, for
one neutral iOS look with iOS blue for actions, and Light and Dark stay. He added: "when you choose
light and dark the liquid glass should just be liquid glass, not more white or dark". In Light the home
screen's widgets, pill and dock had turned milky white; in Dark, smoky.

He asked for this change not to be tested: it is built and compiled, and checked by reading the whole
change for anything that would crash or lose a setting. Tests and the emulator come back with the rest
of the home screen.

## What changes

- **One neutral look.** The `ZenithPalette` type and `LocalPalette` stay (most screens read them), but
  there is one palette, made for Light or Dark:
  - Words as before: white in Dark, `#10131A` in Light, the muted words at 74%.
  - Actions in iOS blue, `#0A84FF` in Dark and `#007AFF` in Light, with white words on it: buttons,
    selected chips, tabs and segments, sliders, cursors, focus rings, checkmarks, links, the date
    picker (the Material scheme's primary).
  - A switch that is on is iOS green (`#30D158` / `#34C759`); destructive actions and badges are iOS
    red (`#FF453A` / `#FF3B30`).
  - No two-colour gradients: decoration that was made from the theme's colours (tinted headings,
    gradient numbers, glows, orbs, halos, the greeting gradient, Plus and Aura art) becomes plain words
    or goes.
  - Page backdrops are neutral: Dark `#000000` to `#1C1C1E`, Light `#F2F2F7` to `#FFFFFF`.
  - No colour in the glass's veil.
- **The glass.** A pane standing on the wallpaper (everything on the home page, the dock's slab, the
  Search pill, the app-launch card) is the same clear liquid glass in Light and in Dark, with white
  words and the soft shadow the names have. A pane on a page or a sheet (Settings, sheets, the folder
  panel, the page overview) keeps a body that follows Light and Dark, as iOS's sheets do: the glass
  engine cannot show a page under a pane, so that body is the page *(my call; the home screen is what
  Gabriel saw)*. A `CompositionLocal` set by the home page, the dock and the pill picks the recipe;
  everywhere else is unchanged by this part.
- **What goes.** Setup's "Pick a colour" becomes "Light or dark?" (in the same place in setup's
  order). Settings' colour choice, the wallpaper sheet's "Use this in Zenith too", Plus's Theme maker,
  "By time of day", the Glass studio's "Theme in the glass", and Aura's theme shop. Looks no longer
  change colours. A widget's or a folder's own colour stays: it is not a theme.
- **Wallpapers.** Zenith's painted designs keep colour sets, as wallpaper colours only. A design that
  followed the theme is pinned, the first time the new version runs, to the colours of the theme the
  phone had, so nobody's wallpaper changes colour; the colour maths and the wallpaper's key are kept
  exactly, so nothing repaints on the update.

## What does not break

Every stored field stays and is read as before (`paletteId`, the Theme maker's colours, the wallpaper's
colours, day palettes, bought Aura themes, the Glass studio's tint): old settings, Looks and backups
read, and a key is never reused with a new meaning. A settings read must never throw, since that sends
a phone back to setup.

## Left for later

- Aura points have nothing to buy until Aura is rebuilt (step 8 of the screen order).
- Plus loses the Theme maker and "By time of day"; Plus is rebuilt in step 8 too.
- The README, the Play listing, the store pictures and the website still describe themes.
