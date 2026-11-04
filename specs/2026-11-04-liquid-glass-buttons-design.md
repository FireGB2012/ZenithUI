# Liquid Glass buttons

- Date: 2026-11-04
- Branch: `liquid-glass-buttons`
- Status: built from the handoff "Liquid Glass buttons for Zenith UI" (the four buttons of the
  Liquid Glass Button design, each put where it fits). Gabriel asked for "the 4 different ones in the
  places they fit perfectly". The motion numbers are the handoff's and are final; the decisions
  below were made while building, and any of them can be changed.

## The four, and where they went

| | Glass | Under a finger | Where |
|---|---|---|---|
| 1a | Regular | Flex: swells to 1.075 and lights up from the fingertip | `GlassButton`, its default: every sheet's buttons |
| 1b | Tinted (accent) | Jelly: 0.95 and squashed 5%, wobbling back | `PrimaryButton` (setup, Plus, wallpaper, gallery, Aura, account) and `GlassButton(filled = true)` |
| 1c | Clear | Lean: swells to 1.05 and leans up to 12 dp across, 9 down, towards the finger | the bedside clock's button, the round search button, the Quick Toggles |
| 1d | Clear lens | Drag: swells to 1.09, follows the finger, stretches with speed, snaps | the pill of `Segmented`, everywhere it is used |

Springs: Flex and Drag 0.41 / 420, Jelly 0.24 / 520 with its squash 0.18 / 640, Lean 0.56 / 380
with the lean 0.74 / 260, a lens let go 0.57 / 700. With movement reduced every change of size is
two fifths of what it was, nothing squashes, leans or stretches, and every spring is 1.0 / 320 (the
lens settles at 1.0 / 700). All of it, and where a lean goes, is `GlassPress.kt`, with
`GlassPressTest`; which option a lens is nearest is `nearestSegment`, with `SegmentedTest`.

## Decisions

- **A pane that moves tells its glass.** `glass()` lines the wallpaper up from where the pane was
  laid out, and a graphicsLayer moves a pane without laying it out again, so a leaning or dragged
  pane would carry its picture with it. Glass that moves under a finger is therefore asked for as
  `glass(motion = ...)`, not as a separate modifier beside it: the glass then reads the press while
  drawing and asks the layout where its middle is now (`drawnOrigin`). A pane moved by something
  else, the `Segmented` lens, says so with `glass(tracking = ...)`. `Modifier.glassPress` is kept
  for things that are not glass (the Quick Toggles).
- **A swelling pane is a lens that swells.** The picture stays put about the pane's middle and grows
  with it, by as much as the pane does (at most 9%). Measured on the emulator with a wallpaper of
  bars: a lens dragged 150 px along the row showed the bars where they were, within the 14 px its
  9% swell accounts for.
- **The Quick Toggles are not glass.** They sit on a widget's pane, and glass in glass shows a hole
  through to the wallpaper, so their discs keep their colour and shape change and only lean. The
  round glass button the handoff quoted for them is the narrow search widget's: it is Clear and
  leans too.
- **The pill of a row of options is only glass while held.** At rest it is the accent pill it was,
  stretching between options the way it did. Under a finger it fades into the lens, and the word
  under the lens takes the glass's own ink: white on a pale lens could not be read.
- **Glass buttons have their own press.** They no longer follow Settings → Pressing (Bounce, Squish,
  Ripple); everything else still does.
- **Tinted glass is a slab like the others.** As well as the handoff's numbers it has clear glass's
  reflection at the rim, the light's colour and the slab's shading, so it sits with the rest.
- **The ink rule is one function**, `inkOnTint`: dark on an accent lighter than a mid tone, white
  on a deep one.

## Checked

- Unit tests and `:app:assembleDebug`.
- On an emulator made for the purpose (Android 37, x86_64) and deleted afterwards: setup's buttons
  before any wallpaper copy (an accent body with a rim, not a white slab); Jelly's squash and the
  glow under the finger; Midnight, Sunset and Frost, light and dark; dragging the pill both ways,
  ticking, settling and choosing; a drag that starts off the pill doing nothing; a Quick Toggle
  leaning; the wallpaper staying put behind a dragged lens.

## Not checked

- Android 12 (frosted copy) and Android 10/11: no system images for them on this PC.
- On a real phone: the haptics, and how the springs feel under a finger rather than `adb input`.
- Aurora, Sakura and Graphite by eye; the ink rule's test covers light and deep accents.
