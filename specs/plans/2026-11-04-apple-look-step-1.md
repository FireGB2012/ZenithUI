# Apple look, step 1: colours, the card, the font, clean glass — implementation plan

**Goal:** The ground the rest of the iPhone look stands on. Apple's page, card and word colours exist
for every screen to use; every accent is readable on Apple's white and black; a solid card exists to
draw widgets and grouped rows with; a new install starts in Apple blue and Inter; and the glass that
is left has a clean white rim, no colour from the wallpaper, no theme colour in its veil and no light
under a finger.

**Architecture:** The colours and the accent rule are plain functions (`AppleColours`) with a test,
provided to the interface once, in `ZenithTheme`, as `LocalApple`, and the readable accent goes into
`LocalPalette.accent`, so every accent already drawn becomes the readable one with no change at the
call. Wallpapers are painted through `Palettes.forWallpaper`, not `LocalPalette`, so they keep the
palette's own colours. The glass changes are in `glassLook` and `glassLift`, which are tested, and
in `glass()`, which loses the finger's light (`PressLight`, `interactive`, `light`).

Spec: `specs/2026-11-04-apple-look-design.md`, "The rule for glass" and "1. Colours, text and the
solid card".

## Global constraints

- minSdk 29, no new dependencies, Compose BOM 2024.12.01.
- Old settings, backups and Looks read as before: defaults change for a new install only (every
  field is written when settings are saved, so a saved choice stays).
- The press motions (`GlassPress`) are unchanged; only the light under the finger goes.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` after each task; pictures on a throwaway
  emulator at the end, deleted afterwards. Nothing on Gabriel's phone unless he asks.

## File structure

| File | Responsibility |
| --- | --- |
| `ui/theme/AppleColours.kt` (new) | Apple's page, card, raised, words, second line, separator, grey and badge colours, light and dark; `contrast`; the readable accent |
| `ui/theme/Card.kt` (new) | `CardKind` (widget 22 dp, grouped 12 dp, sheet 38 dp at the top) and `Modifier.solidCard` |
| `ui/theme/Palette.kt` | `accentLight` on a palette; Frost's accent is Apple's blue |
| `ui/theme/Theme.kt` | provides `LocalApple`, and the readable accent in `LocalPalette` |
| `ui/theme/Glass.kt` | neutral veil and lift, `GLOW` 0, no `PressLight`, no `interactive` or `light` |
| `data/Settings.kt`, `ui/onboarding/OnboardingFlow.kt` | a new install starts on Frost and Inter |
| callers of `glass(interactive = …)`, `ui/onboarding/Segmented.kt` | drop the finger light |
| `README.md` | "How the glass works" and "Fonts" say what is now true |

## Tasks

### Task 1: Apple's colours and the readable accent

- [x] `AppleColoursTest`: words and second lines stand 4.5 to 1 on page and card, light and dark;
  all eleven palettes' accents stand 3 to 1 on card and page, light and dark; Frost gives `#0A84FF`
  in the dark and `#007AFF` in the light; an accent that already stands is left alone; a near-white
  accent in the light, and a near-black one in the dark, are moved until they stand.
- [x] `AppleColours.kt`, `accentLight`, Frost's blue.
- [x] `ZenithTheme`: `LocalApple`, readable accent.

### Task 2: the solid card

- [x] `Card.kt`: `CardKind`, `Modifier.solidCard(kind, colour)` in the card colour unless given one.

### Task 3: a new install starts on Frost and Inter

- [x] `NewInstallDefaultsTest`: `ZenithSettings()` is Frost and Inter; settings saved with Midnight
  and System read back as Midnight and System.
- [x] `Settings.kt` defaults, onboarding's first palette, `LocalPalette`'s default.

### Task 4: clean glass

- [x] `GlassLookTest`: no pane has any glow; the veil and the lift are the same whatever the tint.
- [x] `glassLook`, `glassLift`: neutral; `GLOW` 0.
- [x] `glass()`: no `PressLight`, `interactive` or `light`; callers and `Segmented` updated.

### Task 5: check and ship

- [x] README brought up to date; build and every unit test; pictures in light and dark on a
  throwaway emulator (glass rims white, nothing lights under a finger, Apple blue, Inter on a new
  install); commit and push to `apple-look`.
