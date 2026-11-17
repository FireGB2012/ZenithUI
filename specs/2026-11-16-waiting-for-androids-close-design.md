# Waiting for Android's own close

- Date: 2026-11-16
- Branch: `claude/project-thread-uabqs6`, off the release branch (`claude/project-thread-jqkd80`, after #9)
- Follows: `2026-11-16-closing-on-every-phone-design.md`
- Status: built. Gabriel, on his Galaxy A55 with gestures, after #9: "i see it its happining to fast
  it works but the animtion thing that goes down whyen u throw the app with the ios string bottom
  like blocks teh weiw so like while its falling dont even make thg apps sapwn so like wait a sec
  with nothing adn then it does the naim serch up how long it does it on every brand and make it work".

## What he saw

- Swiping an app away on the gesture bar, One UI plays its own close over everything: the app
  shrinks and drops to the foot of the screen. Zenith is already in front underneath it, resumed and
  with its window's focus early in that fall.
- #9 set Zenith's close off at that focus, or 450 ms after resuming. So the icons were already there
  and the card was shrinking into its icon behind the falling app, where nobody could see it.

## How long Android's own close takes

From Android's source, Android 15 as LineageOS 22.2 carries it (Launcher3's quickstep and
frameworks/base):

- **Gestures**, with a launcher that is not built in (`FallbackSwipeHandler`): the home screen is
  started as the finger lets go, with no animation of its own (`makeCustomAnimation(0, 0)`). The
  app's window springs into an icon-sized square over where the dock would be. The springs are
  stiffness 200 with damping 0.75 to 0.8, or 400 to 500 with damping 0.95 to 0.99 in the newer
  version. Meanwhile the home screen fades up over 250 ms (`StaggeredWorkspaceAnim.DURATION_MS`).
  About 400 ms in all.
- **Buttons**, home pressed: `wallpaper_open_exit`. The app drops 110% down off the bottom of the
  screen in 225 ms (`fast_out_linear_in`), and the home screen is shown at once
  (`wallpaper_open_enter`).
- Samsung, Xiaomi, Oppo with OnePlus and realme, vivo, and Honor with Huawei write their own, and
  none of them publish timings.
  - Launcher developers report that a launcher which is not the phone's own shows about a second
    late on Oppo, OnePlus and realme since Android 14. Niagara's feature board has this on a OnePlus
    15 with OxygenOS 16, and Lawnchair's gesture page says the same.
  - Xiaomi does not let other launchers use gestures at all.
  - Nobody gives a figure for One UI.
- Developer options stretch both closes: the animator duration scale stretches the gesture close
  (it runs on the phone's launcher's animators), and the transition animation scale stretches the
  button one.

## Android says when it is over

- Since Android 14 every transition is a shell transition. When one finishes, each activity it
  brought into view is told so, through this chain:
  1. `Transition.finishTransition`
  2. `dispatchLegacyAppTransitionFinished`
  3. `mEnteringAnimation`
  4. `scheduleEnterAnimationComplete`
  5. `Activity.onEnterAnimationComplete()`
- Going home with gestures, the phone's launcher starts the home screen while its own animation of
  the app is still running. That start is merged into the recents transition.
- The shell finishes a merged transition only when the one it went into finishes (`Transitions`:
  "starting with the playing one and then all the transitions merged into it"). The phone's
  launcher finishes that one once its animation is done. So Zenith is told when the fall is over.
- Before Android 14 the old transitions tell Zenith as soon as it is shown, because its own
  animation is none. That is too early to go by.

## What changes

1. **The home screen waits, empty, while Android closes the app.** This is when Zenith comes back
   from an app with nothing of Android's coming into its icon, so no handshake. Only the wallpaper
   shows: everything is wound right back (`arrivalDepth` FULL, the arrival clock at 0, nothing
   drawn). Then the icons fly in all the way, as after an unlock. At the same moment the app goes
   into its icon: as the card, the picture, or the landing.
2. **How long it waits (`AndroidClose.plan`).**
   - On Android 14 and later, until Android says its close is over. It never sets off sooner than
     the close usually takes on this make of phone, and never later than the most it takes.
   - Before Android 14, the usual length.
   - Both are stretched by Android's animation speed. With Android's animations off there is no
     wait.

   | Make | Gestures: usual, most | Buttons: usual, most |
   | --- | --- | --- |
   | Pixel, Motorola, Nothing, Sony, Fairphone, Nokia | 400, 800 ms | 250, 600 ms |
   | Samsung | 450, 1000 ms | 350, 800 ms |
   | Xiaomi, Redmi, POCO | 450, 1000 ms | 400, 900 ms |
   | Oppo, OnePlus, realme | 600, 1300 ms | 400, 900 ms |
   | vivo, iQOO, Honor, Huawei | 500, 1100 ms | 400, 900 ms |
   | Others | 450, 1000 ms | 350, 800 ms |

   (2026-11-17: Samsung with gestures is now 600, 900 ms, counted to Zenith's own frames being on
   show rather than to the app being gone, and the card waits up, full size: see
   `2026-11-17-closing-like-void-design.md`.)

   Gestures are read from Android's `navigation_mode`, with One UI's and MIUI's own settings
   besides. Apps are allowed to read `navigation_mode`, and makers' own keys are always readable.
3. **Some cases don't wait.**
   - The handshake (a Pixel-type phone with gestures): Android flies the window into the icon
     itself, and the icons are wanted in place for it to land in.
   - Reduced motion.
   - An unlock: the screen was off when Zenith was covered.
4. **A slider to set the wait by hand.** It is *Wait for Android's close*, under Advanced
   animation settings, Closing an app. It is Auto, or 10 to 1500 ms kept to exactly. Its note says
   what Auto comes to on this phone, and how long Android's own close took the last time it said.
5. **A tap during the wait lets the close go at once**, for example on an icon not shown yet.
   Being covered again drops the wait with the close it held.

## Limits

- None of it has been seen on a phone.
- The makers' numbers are guesses. On Android 14 and later, Android's word decides when to go,
  and the numbers only keep it in bounds.
  - If One UI said it was done before its fall ended, Zenith would still wait 450 ms.
  - If it never says, Zenith waits 1 s.
- Phones whose own close is short may wait a little longer than they need to. That is by design:
  he asked to "wait a sec with nothing".
- After the wait the icons flew in all the way, as after an unlock. Since
  `2026-11-17-closing-like-void-design.md` a card going into the icon brings the home screen back
  with it instead, whole and sunk back, coming forward about its foot, as Void's does.

## Checked

- Unit tests:
  - `AndroidCloseTest`: the makes, gestures, the plan, and when to set off.
  - `AnimationTuningTest`: the slider.
- Frames from the JVM screenshot rig, set up as a Galaxy with gestures:
  1. One UI's fall is drawn in over the first 480 ms.
  2. Android says it is done at 480 ms.
  3. Zenith's part follows.
