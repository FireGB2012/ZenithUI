# Apple look, step 5: setup on a plain page — implementation plan

**Goal:** Setup is a plain page, as an iPhone's is: white in the light, black in the dark, with the
bold titles as they are and nothing drifting or glowing behind them. The moving lights go, and with
them the coloured wave a choice sent through them and the light behind the phone. What stands on the
page is solid: the name field is Apple's grey, the choices are Apple's chips and segmented control,
the main buttons are solid pills of the accent (step 3), and Plus comes up on a solid sheet, a step
up from the page. Nothing in setup is glass any more, since on a page of its own glass would be a
hole through to the wallpaper.

**Architecture:** `OnboardingFlow` paints the page itself, in Apple's plain colour
(`AppleSurfaces.plain`, a rule with a test), and lets it fade as the phone grows into the home
screen, as the lights did, so the real wallpaper is there when the phone lands. `OnboardingBackdrop`
(the lights, their scenes and the colour wave) and the unused `AuroraBackdrop` are deleted. The clock
that pours colour through the hello words, which the lights shared, moves into `HelloStep` as its
own. `PhonePreview` loses its halo; its sweep of light across the finished phone stays, as a white
glint rather than a glow. Words take Apple's colours (`LocalApple`) rather than the glass's, since
they stand on a page.

Spec: `specs/2026-11-04-apple-look-design.md`, "4. Setup, wallpapers and Aura": its first part, and
"The rule for glass".

## Global constraints

- As before: no new dependencies; old settings read as before; nothing on Gabriel's phone or his own
  emulator. `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`,
  then a commit and a push to `apple-look` for each part. Setup is looked at from its first screen to
  the hand-off on a throwaway emulator, in the light and the dark.
- Setup does everything it did: every step, choice, word and limit, the order, back, the build with
  its ticks, the Plus offer only while it is true, the hand-off into the real home screen.
- With movement reduced nothing moves; the hello words hold still in their colours.

## File structure

| File | Responsibility |
| --- | --- |
| `ui/theme/AppleColours.kt` | `plain`: Apple's plain page, white or black, and a step up on a sheet |
| `ui/onboarding/OnboardingFlow.kt` | the plain page and its fade at the hand-off; Back in the accent; Plus on a solid sheet |
| `ui/onboarding/OnboardingBackdrop.kt`, `ui/common/Components.kt` | deleted: the lights; `AuroraBackdrop` |
| `ui/onboarding/HelloStep.kt` | the hello words' own clock |
| `ui/onboarding/NameStep.kt` | the name in Apple's grey field |
| `ui/onboarding/StudioStep.kt`, `OnboardingKit.kt` | Apple's words, chips and marks; no wave to start |
| `ui/onboarding/PhonePreview.kt` | no halo; a shadow with no colour in it |
| `ui/onboarding/PlusStep.kt`, `HelpStep.kt` | the Plus pitch and the quick tips solid |
| `README.md` | setup as it now is |

## Tasks

### Part 1: the rule, test first

- [ ] `AppleColoursTest`: the plain page is white in the light and black in the dark, a step up on a
  sheet; words, second lines and the accent stand on it, and words in Apple's grey field on it.
- [ ] `AppleSurfaces.plain`.

### Part 2: setup

- [ ] The plain page in place of the lights, fading at the hand-off; the lights, the wave and
  `AuroraBackdrop` deleted; the hello words keep their flowing colour on a clock of their own.
- [ ] Back as an accent chevron; the step marks, titles and every line in Apple's words.
- [ ] The name field Apple's grey; the colour swatches ringed in the words' colour; the widget
  choices Apple's chips; the phone without its halo.
- [ ] Plus on a solid sheet, a step up from the page, its features on a card; the quick tips' symbols
  in the accent with nothing glass behind them.

### Part 3: check and ship

- [ ] A review of the step from more than one angle, each finding checked against the code.
- [ ] README; build and every unit test; setup from the hello to the hand-off on a throwaway
  emulator, light and dark; commit and push to `apple-look`.
