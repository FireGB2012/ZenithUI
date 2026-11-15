# Liquid Glass, 1:1 with Apple's

- Date: 2026-11-15
- Branch: `claude/project-thread-a7d4wc`, off the iOS 26 home screen branch (`claude/project-thread-6610om`)
- Status: built. Gabriel: "make it look 1 to 1 to apple liquid glass cuz rn it looks like a blob of
  goo".

## Where the goo came from

The glass was drawn as a thick candy slab that is lit and shaded, which is what made it read as a
blob of goo rather than a sheet of glass:

- **A shaded slab** (`GlassLook.relief`): every pane had a rounded shoulder darkened by up to 55% on
  the side turned from the light, a lit one facing it, light falling away across the face, and two
  glints on the edge. Gummy and three-dimensional; Apple's face is flat.
- **A mirror at the rim** (`GlassLook.mirror`): the outer part of the rim showed the wallpaper outside
  the pane flipped over, smeared all the way round.
- **A thick glow round the whole edge**: a hairline, a 6 dp band and a bloom from the bevel, each
  lit, plus extra blur forced into the rim.
- **Milky light** (`gather`): every pane brightened what was behind it.
- **Heavy shadows**: two black shadows down and to the right (up to 34% and 46%) and a coloured glow
  under the glass.
- **Gummy minus beads** in edit mode: a shaded drop with a highlight on its face and a dark foot.
- **Jelly springs**: presses on damping ratios of 0.18 to 0.41, which go 24% to 56% past their rest
  and wobble three or four times.

## What Apple's glass is, and what Zenith's does now

From Apple's own description of Liquid Glass and its material, and the motion guidance in the
`liquid-glass` skills (Apple's reference pictures could not be fetched from here):

- **It bends at the edge, not the face.** The face shows the wallpaper exactly where it is. A band
  round the rim (a quarter of the pane's shorter side, 5 to 22 dp, never wider than its tightest
  corner: `lensBand`) bends it inward like the edge of a thick convex lens, on a circle's profile
  (`1 - sqrt(1 - x^2)`): gentle where the band starts, hardest at the outline, where what is behind
  turns over. Clear glass splits a whisper of colour there.
- **Bigger is thicker** (`glassThickness`, 0 for 28 dp and under, 1 from 120 dp): a wider band, a
  harder bend and a deeper shadow. Size is part of the material.
- **Light on the edge only.** A hairline hugging the outline and a little sheen just inside it,
  brightest on the corner facing the light and again on the corner opposite, faint between, and a
  touch of Fresnel brightening in the bent band. Nothing on the face. The light now comes from up and
  to the left on the diagonal (225 degrees), as on iOS, and still moves with the phone's tilt.
- **Blur and vibrancy.** Clear mists the wallpaper a little (frost 0.7, was 1.0); Regular frosts it
  (2.0, was 1.55) so words on it stay readable. What is seen through is more saturated, not greyer
  (vibrance 1.3 for Clear and tinted, 1.45 for Regular).
- **A veil only where words would fight.** A thin coat that thickens only over parts of the
  wallpaper that would fight the text, as Apple's dimming does for Clear glass over bright content.
- **Tinted glass** keeps clear glass's bend under the accent, less frosted (0.6).
- **Soft shadows straight down**: one wide faint one (10 to 24 dp of blur, 2 to 7 dp down, 16% at
  most in Dark and 10% in Light) and a hairline one under the edge; the coloured glow beneath glass
  made from the wallpaper is softer and fainter, also straight down.
- **Thin strokes**: the rim drawn without the shader (Android 12, no copy of the wallpaper) is a
  1.6 dp hairline graded from the light round to the far corner, with a faint sheen inside it.
- **It materialises by bending** (`presence`), not by fading, as before.
- **Glass ink** ("No glass" widgets) follows the same rules: flat strokes with a hairline of light.
- **Minus beads** are flat pale glass discs: the corner they show under a veil of their own grey, a
  narrow brighter band at the edge, a hairline rim lit up and to the left and opposite, and one soft
  shadow.

## Motion

Apple's springs keep to a response of 0.2 to 0.5 s and a damping ratio of 0.7 to 1.0 for nearly
everything; `.bouncy`, its bounciest, is 0.7. Every glass press now springs back with one small
bounce: Flex, Drag and Lean 0.7, the lens let go on a row of options 0.7, the home screen's jelly
icons 0.6 with their squash 0.45 (one rebound, still the fifth twist). `GlassPressTest` holds every
glass spring to at most a fifth past its rest and a second swing under 5%. Tinted glass buttons
(`PrimaryButton`, `GlassButton(filled = true)`) now swell and light up under a finger as iOS's
tinted glass buttons do (Flex) instead of shrinking and wobbling (Jelly). The morph of the Search
pill into its field and the hold menu were already on Apple-like springs (0.7 to 0.86) and are
unchanged; so is the app-wide Pressing setting.

## Decisions

- **No reflection of the outside at the rim.** It was the strongest source of the smeared look and
  Apple's edge reads as a lens, not a mirror.
- **Glass in Settings still scales the bend and the light**, but there is no longer a slab to flatten.
- **The bead keeps its magnifier.** It was liked; it now sits under the bead's own grey, so every
  bead is the same pale disc whatever corner it is over.

## Checked

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`, with `GlassLookTest`, `GlassTuningTest` and
  `GlassPressTest` rewritten for the new rules first.
- The device tests (`LiquidGlassShaderTest`, `GlassInkShaderTest`, `GlassSheetTest`) updated and
  compiled with `:app:compileDebugAndroidTestKotlin`. The shader's tests, including the two new ones
  (the face is flat and only the edge is lit; the edge bends what is behind it and the face does
  not), were also run on the JVM through Robolectric's native graphics, which runs the real AGSL
  program: all pass.
- Screenshots of a home screen's worth of glass (widgets, folder, icons, buttons, Search, the dock,
  a hold menu, edit-mode beads) over a dark painted wallpaper, a striped and gridded one, and a bright
  one, in Dark and Light, before and after, rendered the same way.

## Not checked

- On a phone or an emulator: there is none here. The look under a moving tilt, the press springs
  under a finger, and Android 12's frosted fallback are unseen.
- Side by side with Apple's own screenshots: Apple's reference images could not be downloaded here.
