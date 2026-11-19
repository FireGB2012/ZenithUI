# Aurora and Frost: two glass themes in place of Neon and Retro

- Date: 2026-11-19
- Branch: `claude/project-thread-170d08`, off `claude/project-thread-yjufje` (12c8fd3, 108 for Play)
- Follows: the four themes of `2026-11-16-zenith-build-brief-design.md`, and Zenith's glass as
  `2026-11-15-apple-liquid-glass-design.md` has it (flat, light on the edge only, no goo)
- Status: built. Gabriel: "The neon and the orange look looks bad dont use that make some thing that
  still glass but like idk something that looks cool".

## What was wrong with Neon and Retro

Neon was smoked glass with a coloured line drawn round every pane and a glow of it outside: the glow
round the whole edge that the Liquid Glass work took out of Zenith's glass ("goo"). Retro (the
orange one) was not glass at all: cream and brown blocks with thick outlines and hard paper
shadows. Both drew their panes with code of their own (`themedPanel`'s NEON and RETRO branches),
so none of the glass work reached them.

## What replaces them

Two themes that are the same Liquid Glass as the first, made by the same engine, each with colours
of its own. Nothing about how the glass bends, frosts or shines changes; what changes is the light
it catches and the veil it wears.

| | Liquid Glass | Aurora | Frost |
| --- | --- | --- | --- |
| Tone | Follows Light and Dark | Always dark | Follows Light and Dark |
| Page | Black to iOS's dark grey; pale grey to white | Deep teal into violet night | Polar blue at night; pale frost by day |
| Veil on the wallpaper | `#3A3A3C`, grey | `#1C3A40`, a little teal | `#2A3A4A`, a little ice blue |
| Light at the lit corner | White | `#4DFFB0`, aurora green | `#D2F3FF`, ice white |
| Light at the far corner | White | `#AE8CFF`, aurora violet | `#52B9FF`, ice blue |
| Accent | iOS's blue | `#4BE3B4` mint, dark words on it | `#0279BE` by day, `#4DC3FA` at night |
| Setup's lights | White | Green and violet | Ice white and blue |

*(My call)* **Aurora is always dark**, as Neon was: the northern lights are a night thing, and a
pale Aurora is only a teal tint. Light and Dark still apply to the others, and the theme step and
the appearance page say so while it is picked, as they did for Neon.

*(My call)* **Light, not paint.** The colour is in the light the edge catches: added to what the
glass shows, on the hairline and the soft light just inside it, brightest where the edge faces the
lamp, as Liquid Glass's white is. Never a coloured line drawn round the pane with a glow outside it,
which is what Neon's edge was. Each colour is bright (luminance at least 0.6 at the lit corner, a
third of white's at the far one, `ThemesTest`). A first pass with paler colours (`#9CFFDA` and
`#C4B2FF`, `#EAF8FF` and `#8FD3FF`) was too close to white to read on a phone-sized screenshot, so
they were made more vivid, still light.

*(My call)* **A veil, not a card.** Each veil is as deep as Liquid Glass's grey, within 0.015 of
its luminance, laid on just as thinly: the wallpaper shows through the same, only cooler or
greener. A colour somebody chose for a pane (a folder's, a widget's) comes in from the theme's
veil as it did from the grey.

## How it is built

- `ThemeGlass` (data): a theme's `veil`, `rim` and `rimFar`, as colours, written to JSON with the
  rest of it under `glass` and read back whole; a theme with none, or a field that cannot be read,
  has Liquid Glass's (`Themes.LIQUID_GLASS`).
- `GlassColours` (UI): the same three as Compose colours, `Theme.glassColours()`, with
  `GlassColours.Liquid` for Liquid Glass. `lightAt(at)` is the light at a point on the line from the
  lit corner (0) to the far one (1).
- `glassLook(..., colours)` carries them in `GlassLook.colours`. Glass on the wallpaper is veiled
  in `colours.veil` instead of the grey; the Glass studio's tuning keeps them.
- The lens shader takes two new uniforms, `rimNear` and `rimFar`, declared and set on every draw.
  The lamp's colour is `mix(rimFar, rimNear, 0.5 + 0.5 * facing)`, then a little of what is round
  the pane as before (`glow`). In Liquid Glass both are white, so its glass is what it was, pixel
  for pixel.
- Without the shader (Android 12, and glass with nothing readable behind it), `drawRim` strokes the
  same gradient in `lightAt`, and the body on the wallpaper is the theme's veil.
- Glass lying in glass (`glassOnGlass`), the launch card, the Search field and widgets with a colour
  of their own all take the theme's colours.
- Edit mode's Done is the glass theme's accent as Dark has it (glass on the wallpaper is made as
  Dark makes it): iOS's blue in Liquid Glass, as before.
- The Notch is the camera's black in every theme, with the hairline it has on a dark app; Neon's
  and Retro's coloured edges went with them.
- `ThemeMaterial` is `GLASS` or `FLAT`. `ThemeBlur`, `Theme.panelBlur` and `Theme.glow` went with
  Neon. `themedPanel` draws Clean only; its signatures are kept for callers on other branches.

## Phones that were in Neon or Retro

`Themes.successor`: Neon is Aurora (what Neon was reaching for, in glass), Retro is Liquid Glass
(nothing is like it any more, and Liquid Glass is where everybody starts). Settings read from
anywhere go through it (`ZenithSettings.fromJson`: the phone's own, a backup, a Look), and so does
`Themes.byId`, so an old name asked for anywhere is the theme after it. A theme of your own keeps
its id; one written while Neon or Retro were here with material `neon` or `retro` is glass
(`ThemeMaterial.byId`).

## Checked

- `ThemesTest`: the lineup and order, the successors and that saved settings read as them, Aurora
  always dark, every glass theme's corners as drawn, Liquid Glass's glass Apple's, Aurora's and
  Frost's their own, the light and veil rules above, contrast in Light and Dark (words 7:1, second
  lines 4.5:1 and 7:1 in the dark), JSON round trip and nonsense.
- `GlassLookTest`: Liquid Glass's colours are the engine's own and glass not told whose it is is
  exactly as before; a theme's veil on the wallpaper is laid on as thinly as the grey; `lightAt`;
  tuning and glass in glass keep the colours.
- `LiquidGlassShaderTest` (device, also run on the JVM rig): the new uniforms compile and are set;
  Aurora's edge is greener at the lit corner and more violet at the far one than white light leaves
  it, still as bright near enough, and the face is the same glass.
- Screenshots from the JVM rig, before and after, in the PR.

## Not done

- No new versionCode: this goes with the next Play build, whose What's new should name Aurora and
  Frost (3.8's still names Neon and Retro, as it shipped).
- Not seen on the A55 yet.
