# 3D logos and bigger icons

- Date: 2026-11-16
- Branch: `claude/project-thread-jqkd80` (the full release: `apple-look` with PRs #1 to #6 merged)
- Status: built. Gabriel: "so the logo inside the icon is like 3d out appels does this look for it
  also make apps icon bigger".

## The logo, lifted off its tile

On an iPhone with iOS 26 an icon is two things, a tile and the logo standing a little off it: the
logo casts a soft shadow on the tile, its own face is lit from above, and the edge of it catches the
light. Zenith now draws an app's logo that way (`LogoLift`, drawn by `IconLoader.drawLifted`).

The earlier icon depth (`specs/2026-11-16-icon-depth-and-smoother-motion-design.md`) never touched
the app's own picture: it lights the tile from outside. This does go inside the picture, because
that is what was asked for, but it still never re-draws, re-colours or re-shapes the logo: the
shadow goes under it, and a little light and shade over it.

Every number is a share of the icon's side, so a small icon and a big one look the same:

- **Shadow**: the logo's own shape, blurred (spread 3.2% of the side) and moved 2.8% straight
  down, at 34% black where it is darkest. Straight down, as every shadow in Zenith falls, and soft,
  never a black blot (the tests hold it between 15% and 35%).
- **Its face**: over the logo's own height, wherever it sits on the tile, 7% white at its top,
  plain from 45% to 55% of the way down, and 12% black at its foot. A white logo goes a pale grey at
  its foot, as an iPhone's do, which is what lets the edge light opposite the lamp show on it. The
  light is the fainter of the two so a coloured logo's top keeps its colour instead of going milky.
- **Edge light**: a hairline (1.4% of the side, at least a pixel, about a dp on a home screen),
  50% white on the edges facing the light and 18% on those opposite. The light comes from where
  Zenith's glass is lit, up and to the left at 225 degrees, so logos, icons and glass share one
  lamp. Only solid parts of the logo catch it: a faint wash inside a foreground (some apps draw a
  long shadow into it) catches none.

### Which logos

- An **adaptive icon's foreground** is the logo when it covers between 0.4% and 62% of the tile.
  Past that the foreground is the whole icon, with no background to stand off, and it is drawn as
  it was.
- An **old (legacy) icon** that Zenith puts on a tile of its own colour: its artwork is the logo,
  and is lifted the same way. One that already fills its square, a photo you chose, or a pack's
  full picture, is the whole icon and is drawn as it was.
- **Super icons**: the logo in the middle is lifted as it is on the ordinary icon, measured over
  the same square.
- **Tinted and Glass** are left as they are: their symbol is Zenith's own, drawn in one ink. The
  picture an app's symbol is cut from is drawn without any lift, or the shadow would become part of
  the symbol.

### Cost

All of it is worked out once per icon per run, on the icon loader's background thread, with the
icon: three box blurs (as near a true blur as the eye can tell) and one pass over a ~180 px square.
Nothing extra is drawn per frame. Icons are saved in a new folder (`cacheDir/icons-2`) so none drawn
the old, flat way is shown again, and the old folder is deleted on the first start.

## Bigger icons

| | Was | Now |
|---|---|---|
| Icon on a page, with names | 0.64 of a column, 52 to 68 dp | 0.72 of a column, 56 to 78 dp |
| Pixel 10a, iPhone grid (96.8 dp columns) | 61.9 dp (163 px) | 69.7 dp (183 px) |
| "Larger icons, with no names" | the same 0.64 (on most grids it only took the names away) | 0.8 of a column: 77.4 dp on the Pixel 10a |
| Dock height | 92 dp | 102 dp (the biggest icon, 78 dp, and 12 dp above and below) |
| Drawer | 60 dp | 0.68 of its column, 60 to 68 dp: 64.4 dp four across; five or six across keep 60 |

- Icons still never push their names out of a row, nor meet their neighbours: the row and width
  limits are unchanged.
- Widgets run from icon edge to icon edge, so they grow by the same few dp, and a 2 by 2 on the
  iPhone grid is still square (459 by 460 px on the Pixel 10a).
- The iPhone grid's rows are worked out from the screen as before (`GridDensity`), with the same
  allowance for the status bar, Search and the dock, so no one's grid gains or loses a row.
- Folders and Search results keep their sizes: their panels are narrower than the screen.
