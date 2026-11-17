# Closing an app like Void

- Date: 2026-11-17
- Branch: `claude/project-thread-uabqs6`, off `claude/project-thread-063v1v` (after #13)
- Follows: `2026-11-16-waiting-for-androids-close-design.md`
- Status: built. Gabriel sent a screen recording of the Void launcher closing Chrome on his Galaxy
  A55 with gestures ("Watch this this ishow void does th anim thing close stuff"), and asked for
  Zenith's close to be like it: the app, the icon, the home screen and the timing.
- Then he tried it on the A55 ("it doesnt rlly work") and sent a recording of Zenith: on his phone
  it never got as far as the card. See "On Gabriel's phone", below.

## What Void does

Read off the recording frame by frame. There are two closes of Chrome: a swipe home, and one from
Recents. The recording drops frames now and then, so the times are to within a frame or two.

1. **Android first.** One UI's own close plays (about 300 ms). Void draws nothing during it, and
   for about 270 ms after it: only the wallpaper.
2. **The app back.** A card in the icon's colour fills the whole screen at once, with the screen's
   own corners. On it is the app's icon blown up to the card's width, about 6% opaque.
3. **A beat, then into the icon.** It sits there for a moment (40 ms in one close, 190 ms in the
   other), then goes into the icon fast and faster: from 2.3 times the icon to the icon in about
   20 ms at the end. The icon on it is solid by the time the card is a third of the screen.
4. **A squash.** It lands flattened, about 1.3 times as wide and 0.73 times as tall as the icon,
   for 100 to 150 ms, and springs back to the icon's shape. About 480 ms after the card went up.
5. **The home screen.** It is all there behind the card when it goes up, with nothing flying in:
   about 90% of its size, coming forward to 100% about the middle of its foot, over the dock. It is
   most of the way forward as the card lands (60 to 80%), and creeps the rest of the way over about
   400 ms more: it settles about 650 ms after the card went up. The card goes into the icon where
   the icon is on the home screen on its way, and moves with it as it comes forward.
6. **Nothing else.** No ripple, no bounce of the icon, and the app's name under its icon stays on
   show all the way through.

## What changes in Zenith

1. **The card fills the screen again.** `CloseRules.CARD_START` is 1 (it was 72%), and a picture
   of the app starts at 100% too. A card that fills the screen is solid from its first frame
   (`CloseRules.closingAlpha`): after Android's close there is nothing under it but the wallpaper.
2. **The icon on it is the icon, blown up.** It is drawn as wide as the card and cut to it, 6%
   opaque at full size and solid from 30% (`ghostAlpha`, `ghostSide`). A picture of the app from
   the picture helper is drawn over it as before, giving way as it shrinks.
3. **A beat, then faster and faster** (`CloseRules.intoIcon`). Over the *Shrinking time*, 280 ms,
   the card goes as the fraction cubed: it has barely moved a third of the way through, is an
   eighth of the way in half way through, and lands at three times an even pace. Its middle
   travels with it, so it shrinks mostly where it is and then goes to the icon.
4. **The squash** (`LaunchMotion.frame`'s `squash`). On landing it is sent off from its shape at
   the speed that takes it *Squash as it lands* (30%) wider and flatter, about its middle, on the
   landing's spring (380, 0.5), and comes back a hair past its shape before it settles, to within a
   pixel (`SQUASH_REST`). Never more than half either way.
5. **The card is the icon by the time it gets there.** Nothing lands and nothing is knocked: no
   landing of the icon and no ripple. Once it is still, the icon under it is shown and the card
   fades out over 90 ms (`HAND_OVER_MS`), so an icon drawn its own way (glass, a shape of its own)
   takes over without a jump.
6. **The home screen comes back with the card** (`CloseRules.bringsHome`): once Android's own close
   has been waited out, when a card or a picture goes into the icon.
   - It is shown whole behind the card in the frame the card goes up, sunk back (Home screen zoom,
     90%), with no arrival of its own: no fly-in and no liquid wave.
   - It starts forward on the Return spring once the card is well on its way (down to 70% of the
     screen, `HOME_GOES_AT`), so it is seen coming forward as the card lands, and is not over
     behind the card before anyone sees it.
   - It comes forward about the middle of its foot (`HOME_FOOT`, 88% down), not about the icon.
   - The card goes into the icon where the icon is drawn on the way, and moves with it as the home
     screen comes forward (`CloseRules.fromFoot`), so the icon's name stays right under it.
7. **The name stays.** While a closing card is on its way, only the icon waits out of sight; its
   name under it stays (`AppLanding.alphaFor(iconOnly)`, `AppLanding.iconAlphaFor`). A super icon
   or a dock icon waits whole, as before.
8. **Settings.** Advanced animation settings, Closing an app:
   - New: *Shrinking time* (120 to 700 ms) and *Squash as it lands* (0 to 50%), after *Picture
     starts at*.
   - The *Landing* stiffness and damping now also run the squash. *Icon lands from* and *Landing
     fade* are for closing Plain, where there is no card.
   - The presets: Fluid 330 ms and 8%, Bouncy 260 ms and 45%, Dynamic 12% (and quicker, with its
     speed), Sweet 35% (and slower), Classic 240 ms and no squash, and with movement reduced 200 ms
     and no squash.
   - The preview at the top of the page plays it: the card fills the little screen, goes into the
     icon and lands squashed while the home screen comes forward about its foot.

## Side by side

Times from the frame the card goes up.

| | Void (Chrome) | Zenith (the rig, Mail) |
| --- | --- | --- |
| After Android's close | about 270 ms of wallpaper | when Android says its close is over (#10) |
| Into the icon | about 290 ms: a beat, then fast | 288 ms: barely moving, then fast |
| Squashed | 1.3 times as wide, 0.73 times as tall | 1.3 times as wide, 0.7 times as tall, at 350 to 380 ms |
| Back in shape | about 480 ms | about 480 ms, then a hair taller and back by 670 ms |
| Home screen at the landing | 60 to 80% of the way forward | 55 to 60% of the way forward |
| Home screen still | about 650 ms | about 600 ms |

## On Gabriel's phone

Read off his recording of Zenith closing Gmail and Messages, frame by frame.

- **No card at all.** Nothing filled the screen and nothing went into the icon: the home screen
  came forward about the icon, as it does after the handshake, and about 0.9 s later the icon
  swelled a little (Gmail's from 181 to 197 px across, Messages' from 181 to 198). That swell is
  the icon taking Android's window (`receiveWindow`), which only happens on the handshake route.
- **Why.** One UI does ask where the icon is: the home intent carries `GestureNavContract`'s
  question, which the 11-16 design had down as never happening on a Galaxy. Zenith answered it,
  so the close took the handshake route: no card, no wait for Android's close, and the window left
  to Android to spring into the icon. One UI then closed the app into the middle of the screen
  whatever it was told and never said the window had got anywhere, so after `HANDSHAKE_WAIT_MS`
  the icon took a window that never came.
- **How One UI shows Zenith.** Zenith is resumed as the finger lets go, and kept out of sight
  while One UI shrinks the app into the middle of the screen (about 0.15 s). The wallpaper shows
  alone for about 0.13 s. Then a still picture of Zenith, of what it is drawing then, fades in over
  about 0.22 s, and Zenith's own frames take over only 0.52 to 0.62 s after it resumed. Void's
  recording shows the same steps: its still picture is its card at full size, which is why its
  close looks right on One UI.

What changes:

1. **A Galaxy is not answered** (`AndroidClose.followsHandshake`). It is told there is no icon,
   so that it does its own close, and the app goes into its icon Zenith's way after it.
2. **The card goes up at once** on One UI with gestures (`AndroidClose.Plan.cardFirst`,
   `CloseRules.cardFirst`): full size, in the frame Zenith resumes in, over the home screen sunk
   back, and it waits there until Android's close has been waited out. One UI's still picture is
   then the app back on the screen, as Void's is, and the card is seen leaving it for its icon.
3. **No sooner than 0.6 s.** One UI with gestures is waited out for 600 to 900 ms (it was 450 to
   1000), counted to Zenith's own frames being on show rather than to the app being gone, or the
   card would shrink unseen behind the still picture.
4. Covered again while the card waits (another app, the screen off), the card is put away with the
   close it was waiting for, and the home screen let go of at rest.

Checked in the JVM rig (`ComingHomeShotsTest.closingMailOnOneUi`), set up as Gabriel's phone: One
UI's question comes with the home intent, and the frames are composed as the A55 shows them (One
UI's close, the wallpaper, the still picture fading in from 280 ms, Zenith's own frames from 560 ms,
with Android saying its close is over then). One UI was answered once, with no icon. Zenith draws
the card full size from its first frame; the still picture is that card; it leaves at about 600 ms,
is in the icon by about 880 ms, squashes and lands, with the home screen coming forward about its
foot. On the commit before (`beb87f7`) the same rig shows no card and the icon swelling, as on the
phone.

## Not changed

- Closing Plain: Android's own close, then the icon landing, as before.
- The Pixel handshake: Android's own window goes into the icon, and the home screen arrives as
  it did. Nothing waits there, so nothing comes back with a card. Every make but Samsung is still
  answered.
- One UI with buttons: the home screen waits, empty, for Android's close, and the card goes up
  after it, as before.
- Unlocking: the home screen arrives the way Coming home says, liquid by default.
- How long Zenith waits for Android's own close (`AndroidClose`).

## Limits

- The close as it is now (the card up at once) has not been seen on a phone yet, only in the JVM
  rig modelled on Gabriel's recording.
- When One UI takes its still picture, and when it says its close is over
  (`onEnterAnimationComplete`), are not known. The card is up from the first frame and waits at
  least 600 ms, so the still picture is the card either way; if the word never comes, it leaves at
  900 ms.
- Only Samsung has been seen asking and then closing its own way. Another make doing the same
  would show the old fault (no card, and the icon swelling late).
- Void's beat at full screen was 40 ms in one close and 190 ms in the other, which looks like Void
  loading its home screen. Zenith's beat comes from the curve, and is the same every time.
- The card draws the app's icon picture. An icon drawn as glass or in 3D on the home screen looks a
  little different until the 90 ms hand-over.
- Swiping to another page while the card lands leaves it behind for a moment, as before.
- Zenith's home screen comes forward on the Return spring: all the way about 100 ms after the card
  lands, a hair past, and back by about 600 ms. Void's creeps the last of the way instead.

## Checked

- Unit tests:
  - `CloseRulesTest`: the card from the whole screen, solid from its first frame, the icon on it,
    the curve into the icon, the home screen coming back with the card and about its foot, the card
    going into the icon where it is drawn on the way, the squash's rest and the hand-over.
  - `LaunchMotionTest`: the squash, about the middle and never more than half.
  - `AnimationTuningTest`: the new numbers, their place in the list, the presets and the speed.
- Frames from the JVM rig, as a Galaxy with gestures closing Mail: One UI's fall drawn in over the
  first 480 ms, then Zenith's part, every 16 ms.
- After Gabriel's recording: `AndroidCloseTest` (a Galaxy is not answered; on One UI with gestures
  the card goes up at once, whatever the wait is set to; One UI's 600 to 900 ms), `CloseRulesTest`
  (the card goes up at once only to bring the home screen back after a wait), and the rig's
  `closingMailOnOneUi`, before and after.
