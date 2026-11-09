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

- [x] `AppleColoursTest`: the plain page is white in the light and black in the dark, a step up on a
  sheet; words, second lines and the accent stand on it, and words in Apple's grey field on it.
- [x] `AppleSurfaces.plain`.

### Part 2: setup

- [x] The plain page in place of the lights, fading at the hand-off; the lights, the wave and
  `AuroraBackdrop` deleted; the hello words keep their flowing colour on a clock of their own.
- [x] Back as an accent chevron; the step marks, titles and every line in Apple's words.
- [x] The name field Apple's grey; the colour swatches ringed in the words' colour; the widget
  choices Apple's chips; the phone without its halo.
- [x] Plus on a solid sheet, a step up from the page, its features on a card; the quick tips' symbols
  in the accent with nothing glass behind them.

### Part 3: check and ship

- [x] A review of the step from more than one angle, each finding checked against the code.
- [x] README; build and every unit test; setup from the hello to the hand-off on a throwaway
  emulator, light and dark; commit and push to `apple-look`.

## What the review found

Three reviewers (the design, behaviour kept, Compose and accessibility), each finding then checked
against the code by another.

- **Fixed:** a ticked widget chip's white words stood only 3.4 to 4 to 1 on the accent (Frost's blue
  in the dark, and Aurora, Sunset, Sakura and Graphite in the light), under the 4.5 to 1 words need;
  the old chip had been black and white. `AppleColours.chosen` takes a chip's fill only as deep as
  white words need, keeping its colour, so Frost's chips are a touch deeper than its blue; near-black
  on a pale accent already stands. A test holds it for every palette, light and dark. Main buttons
  keep their own rule from step 3 (three to one, for large and heavy words).
- **Fixed:** with big text a chip's name wrapped and was cut off by the chip, and could break the
  moment it was ticked, since only a ticked name was bold. A chip now grows to hold its name, keeps
  one weight on or off, and a row holds only as many chips as the widest name fits: three, then two,
  then one (`chipsPerRow`, with a test). At 180% and 200% text the six chips stand two to a row.
- **Fixed:** Back showed nothing when a keyboard came to it. It is ringed in the accent while it has
  the focus, as an iPhone's Full Keyboard Access does; a touch still only dims it.
- **Not changed:** picking Light or Dark turns the page at once rather than easing it. Settings has
  done the same since step 3, the design does not ask for an ease, and turning the page and its words
  together keeps the words readable the whole time.

Seen while checking, older than this step and left for their own piece of work: the segmented control
(here, in Settings and in sheets) keeps one line at a fixed height, so at 180% text and above its
words are cut off; Continue, the swatches and the segmented control show nothing when a keyboard comes
to them; the chips elsewhere (Settings, the drawer, sheets) still put white words on the accent itself.
