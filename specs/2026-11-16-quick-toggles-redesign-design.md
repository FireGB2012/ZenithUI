# Quick Toggles, set out as Control Center is

- Date: 2026-11-16
- Branch: `claude/project-thread-db3t8e`, off the Liquid Glass branch (`claude/project-thread-a7d4wc`)
- Status: built. Gabriel: "better quick settings it looks rlly stuffed ... redesign them as a
  professional would".

## What made it stuffed

- **Names under every button.** Each round button had its name under it at 12sp, so a row of them
  ran together ("Airplane mode Do Not Disturb" in a Large), long ones broke over two lines
  ("Do Not / Disturb") and every name in the widget shrank to the longest.
- **As many as would go.** Buttons went down to 30 dp to fit everything in: a Medium with eight
  chosen was a three by three of small discs, a Wide with twelve two rows of six.
- **No hierarchy.** Every control was the same button, whatever room there was.
- **Uneven room.** A Tall with four was a two by two with a hole in the middle; a Wide with four, a
  row of four floating in an empty pane.
- **Silently dropped.** A Small with twelve showed four and said nothing about the other eight.
- **Nothing lit but the torch.** Wi-Fi, Bluetooth and the rest looked off even when they were on.

## The layout (`ToggleGrid`)

Control Center's own way: a grid of equal cells, the same margin all round (14 dp) and the same gap
between every two (10 dp), whatever the widget holds. A widget under 100 dp tall is a strip, one row,
with a 9 dp margin and 6 dp gaps.

- **Round buttons**, the symbol alone, as big as the cell up to 64 dp (Control Center's), and never
  under 44 dp, the least a finger presses without missing (30 dp in a strip, so a two-by-one still
  holds four on a phone about 390 dp across or wider).
- **Tiles**: where a cell, or two side by side, is 128 dp across, the first controls become capsules
  with their disc at the round end and, beside it, the name and whether it is on.
- **Cards**: a tile with a cell at least 100 dp tall stands up, disc at the top, name at the foot.
- **More**: when not every control fits, the last cell is a "+N" button that opens the phone's own
  quick settings, where all of them are. A grid of one cell never gives up its only place to it.

Every grid that fits is tried, and the one that wins shows the most controls; then the one whose
buttons are at least 52 dp; then the one that names the most; then bigger buttons; then the one with
fewer empty cells. Controls keep the order they were chosen in, tiles first, and a tile is never
split across two rows. On a phone 412 dp across:

| Size | 4 chosen | 12 chosen |
| --- | --- | --- |
| Small (164 × 61) | four buttons, 32 dp | three and +9 |
| Medium (164 × 164) | a two by two of 63 dp buttons | three and +9 |
| Wide (354 × 164) | four tiles | twelve buttons, 46 dp |
| Tall (164 × 268) | four tiles, one over another | seven and +5 |
| Large (354 × 268) | four cards | four tiles over eight buttons |

The widget gallery's line for each size comes from the same grid (`ToggleGrid.note`), so it says
"Room for 3 of your 8; the last button opens the rest." where that is what happens.

## Names (`ToggleNames`)

Names are only on tiles and cards, at 14sp (16sp on a card), beside the symbol. A name too long for
its tile takes its short form: "Focus" for Do Not Disturb, "Airplane", "Data", "Low Power". If even
that will not fit, every name in the widget shrinks together, never under 12sp, so a row reads as
one; past that the longest is cut short. They are measured in the font they are drawn in.

## Whether each one is on (`ToggleStates`)

Read without asking for anything: Wi-Fi, Bluetooth, mobile data and airplane mode from the switches
Android keeps (`Settings.Global`; Bluetooth from its adapter first, which Android 12 and later give
freely), Do Not Disturb from the interruption filter, location, battery saver and NFC from their
services, the ringer as Ring, Vibrate or Silent, brightness as Auto. It is read again on Android's
broadcasts, on changes to the settings it reads, and whenever the home screen comes back, which is
how a change made in a panel the widget opened is seen. Anything that cannot be read is left
unknown, never guessed: no colour and no word. The ringer and brightness change their symbol
(vibrate, muted, auto).

## Colour

Each control lights up in Control Center's colour for it, in its Dark or Light shade: the radios
blue, mobile data and the hotspot green, airplane mode orange, Do Not Disturb indigo, battery saver
yellow, the torch white in Dark and Zenith's near-black in Light, where a white torch would be lost
on a white card. A "No glass" torch lights up yellow, since its symbol is white while it is off.
Symbols on a lit disc are white, as Control Center has them even on green and orange, and dark only
on yellow and the white torch (`litInk`). Off is a faint coat of the ink (14% in Dark, 8% in Light),
and a tile's body is fainter still (10%, 6%), so the disc reads as the switch. A widget with its own
colour takes the lit colours towards it, as far as it has come in.

## Motion

The colour pours into the disc from the middle as a control comes on, and drains away as it goes
off (a spring of damping 0.86); its words change over with a short fade and rise. Under a finger,
buttons, tiles and cards swell and lean towards it (`GlassMotion.Lean`) and spring back with one
small bounce. With movement reduced, all of it is a plain fade.

## Decisions

- **Discs stay round.** A disc used to square up as it came on; on a tile that broke the curve of
  the tile's end, and Control Center's stay round. The colour says it, and the tile's words.
- **44 dp, not 30.** A widget shows fewer controls at a size a finger can hit rather than every one
  at a size it cannot; the +N button says how many are left out.
- **+N opens the phone's quick settings**, not a panel of Zenith's: the switches there work, which
  a launcher's mostly cannot.
- **The Quick Toggles code has its own file** (`ui/home/QuickToggles.kt`), and the grid its own
  (`data/ToggleGrid.kt`), out of `ZenithWidgets.kt` and `WidgetForm.kt`, which other work edits.

## Checked

- Unit tests: `ToggleGridTest`, `ToggleNamesTest`, `ToggleStatesTest`, `QuickTogglesTest` and
  `WidgetFormTest`'s toggles cases, written before the code; the whole suite and `assembleDebug`.
- The JVM screenshot rig (Robolectric with the real AGSL glass), before and after, on six boards
  (Glass in Dark and Light, Colour in Light and Dark, No glass, glass over a busy wallpaper), each
  at nine sizes and mixes, with a phone's settings seeded as in use (Wi-Fi, Bluetooth, data and
  location on).

## Not checked

- A real phone: each panel a control opens, the states catching up after coming back from one, and
  Bluetooth on Android 10 and 11, which should fall back to the setting.
- Whether a phone lets a launcher read "mobile_data": where it does not, mobile data shows no state
  rather than a wrong one.
- TalkBack on a device (each button says its name and On or Off; tiles read their words).
