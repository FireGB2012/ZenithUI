# Coming home like liquid

- Date: 2026-11-16
- Branch: `claude/project-thread-3fx0d9`, off the release branch (`claude/project-thread-jqkd80`, after #10)
- Follows: `2026-11-16-waiting-for-androids-close-design.md`
- Status: built. Gabriel, on his Galaxy A55 with gestures: "can u make a smoth like comming home anim
  were when o open ur phone or exits and app it like smoths in like rlly smoth liquid glass like".

## What it was

- After an unlock, and on a Galaxy once Android's own close of the app is over (#10), the home
  screen flew in: every item started half as far again from the middle as it belongs, 70% of its
  size, and came in to its place on one 600 ms clock, the middle first (`FlyIn`).
- It reads as things being thrown at the screen, not as the home screen coming back. Nothing in it
  is glass, and it has nothing to do with the app that was just closed.

## What it is now

A wave runs out across the screen, and everything rises as it reaches it.

- **Where from.** Back from an app, the icon the app goes back into (`IconPlaces`, as the close
  already finds it), or the icon it was opened from. With neither, and after an unlock, a cold start
  or a Look put on, the middle of the screen. The card shrinking into its icon is the drop; the
  home screen is the water it falls into.
- **The front.** Its distance from the start is `reach * (1 - (1 - t)^1.5)` over the run, 520 ms
  as Zenith has it. It slows as it spreads, but less than edit mode's waves (a bezier that covers
  nearly half the screen in the first fifth of its run), so it is seen to travel rather than burst.
  `reach` is the light's own reach: past the corner farthest from the start by two band widths.
- **Each thing.** It sets off 90 ms before the front reaches its middle, so it is half way up as the
  light passes over it. It rises on a spring (stiffness 220, damping 0.8: a hair, 1.5%, past its
  place and back) from 80% of its size, from fully clear (`alpha = 1 - out²`), and drawn in towards
  the start by up to 24 dp, all of it for anything at least half the reach away. The spring is
  Compose's own, worked out in closed form for any damping, so it can be read on the arrival clock
  while drawing instead of being forty `Animatable`s.
- **The light.** The same wave is sent on the home screen's one `GlassRing` (`RingStyle.arrive`): every
  pane of glass lights up rim and face as it passes, and every icon and colour widget gleams, exactly
  as a tap's ring makes them. On the wallpaper it is 60% of a tap's band and a fifth of its line, at
  60% of its strength, so it reads as light going through the glass, not a circle drawn on the
  screen. It keeps most of its light past half way, then fades out (`waveFade`).
- **Depth.** Unchanged: the full depth after an unlock and after waiting for Android's close, a fifth
  of it (a settle) where Android's own window springs into the icon (`GestureHandshake`), since
  that window lands on icons that are already there.

At Zenith's own numbers the whole screen has come home in about 0.6 s, and is still, to a five
hundredth of the way, in just under a second.

## One clock, one wave

- `LiquidWave` is worked out once, as the arrival sets off (`HomeScreen.arrive`), and kept in a
  state that everything reads while drawing. Each item, the dock and the Search pill keep a
  `LiquidSpot`: the first time they draw with a new wave they work out when they set off and from
  where, from where their middle is then, and keep that while it runs. The home screen coming
  forward meanwhile (its zoom from behind the app) changes nothing about when they rise.
- The arrival clock runs to the wave's own end, which depends on the spring (`LiquidArrival.settleMs`),
  not to 600 ms. Once any arrival is over the clock is left at `FlyIn.AT_REST`, past the end of every
  style, so changing Coming home never finds the home screen part way in, and the at-rest check
  (`IconPlaces.atRest`) asks for the end of the arrival that is running.
- Nothing new is composed per frame: the clock and the ring are read in `graphicsLayer` and draw
  lambdas, as before. No blur, no `RenderEffect`.

## Settings

- Motion & feel → Coming home: Liquid, Middle, Below, Cascade. Liquid is the default.
- `middle` was written by every phone before there was liquid, chosen or not, so it is read as
  liquid. From the middle chosen now is written `centre` and kept. Below and Cascade are kept.
- Advanced animation settings → **Liquid coming home**, after Closing an app: Wave across the screen
  (520 ms), Rising stiffness (220) and damping (0.8), Rises from (80%), Drawn in (24 dp), Light of the
  wave (60%; 0% sends no ring at all). The speed slider runs it all, light included: the run and the
  lead divide by it, the stiffness goes up by its square. Fluid rises without passing its place,
  Bouncy further from smaller, Dynamic stops where it lands, Sweet is slower and brighter; Classic is
  how opening and closing were, and leaves coming home as Zenith has it. The preview at the top of
  the page plays the wave out from the icon as the app closes, with a band of its light.
- With movement reduced nothing arrives at all, as before.

## Not changed

- The close into the icon (#9, #10): the card from 72% on a Galaxy, the picture, Android's own window
  with the handshake, the landing and the ripple. The icon the app goes into rises with the wave too,
  at once, and is all but up by the time the card reaches it, so the landing plays as it did.
- The wait for Android's own close: the home screen stays empty until it is over, and the wave
  starts then.
