# Opening and closing apps, all the way

- Date: 2026-11-16
- Branch: `claude/project-thread-uabqs6`, off the release branch (`claude/project-thread-jqkd80`)
- Status: built. Gabriel: "one of my competers have found a way to hvae full open and close
  animations its like it wait for the app for the default animtion to be done na dthen it takes a
  screen shor of the app and like makes the cloase anim".

## What Android lets a home app do

Only the launcher built into the phone may move another app's window: that takes
`CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS`, which Android gives to nothing that can be installed.
Opening, Zenith already draws the whole motion itself (the card out of the icon) and the app's own
window fades in over it. Closing is where a launcher like Zenith has always been left with half:
Android plays its own close, then shows the home screen. There are three ways round it.

1. **Android's gesture handshake** (Launcher3's `GestureNavContract`, Android 11 and later). With
   gesture navigation on a phone whose recents come from AOSP's Quickstep (Pixel, and most phones
   that keep close to it), going home from an app sends the home app a question with the home
   intent: where is this app's icon? A home app that answers gets the app's real window sprung into
   that spot. No permission, nothing to switch on. Samsung's One UI does not ask launchers that
   are not its own (Niagara's developer, Android Authority, February 2025); Xiaomi does not let
   them have gestures at all; with buttons, nobody is asked.
2. **A picture of the app** (what the competitor does). Android gives no app a picture of another,
   with two exceptions. Screen recording (`MediaProjection`) asks every time it starts, puts a
   recording dot up and needs a foreground service: no use to a launcher. An accessibility service
   that says so can take a screenshot (`takeScreenshot`, Android 11 and later), once switched on in
   Accessibility. That is Zenith's **app picture helper**.
3. **Nothing**: Android's own close, then the home screen comes forward and the icon lands. What
   Zenith did before, and still does wherever neither of the others can.

## Closing

Tried in this order, each the moment it can be (`CloseRules.route`):

- **The handshake**, when Android asks. Zenith answers at once with where the icon is when the
  home screen is at rest (`IconPlaces`), so the window springs into the spot the icon will be in
  once the home screen has come forward. Android fades the window as it arrives; the home screen
  comes forward as before; when Android says it has finished (or 0.9 s after the answer, whichever
  is first) the icons round it are knocked back and the icon itself swells a little and settles.
  It is sent off at speed from where it stands, never moved and let go, so there is no jump.
- **The app's picture**, with the helper on and a picture of the app taken while it was open this
  time. Zenith is shown once Android's own close is done, and then a card holding the picture
  starts at 92% of the screen (*Picture starts at*, a new number in Advanced animation settings),
  fades in over its first 12% of the way and shrinks into the icon on the very springs the card
  opens on, the other way. As it gets small the picture gives way to the icon's own colour and
  logo, over the same stretch it came in over on the way out, and then the icon lands.
- **The colour card**, as before: off unless *Closing card* is turned up. It now fades in too.
- **The landing alone**, as before.

Where it goes: the icon it was opened from; failing that, that app's icon on the page on show or
in the dock (an app opened from the drawer, Search or a notification); failing that, the folder
holding it. With none, the picture shrinks into the middle of the screen and fades, as iOS does,
and the handshake answers nothing, so Android does its own.

Coming back from an app that closes into an icon on the page on show stays on that page. Before,
coming home always went back to the first page, so an app opened from the second page could never
be seen going back into its icon. Home pressed while Zenith is already in front still goes to the
first page, or does its gesture.

**Settings → Motion & feel → Closing apps**: *Into its icon* (all of the above) or *Plain* (only
the landing, and the colour card if it is turned up: how it was).

## Opening

The card grows out of the icon as before. With the helper on and a picture of the app from the last
five minutes, the picture fades in over the card between 25% and 80% of the way to full size and
the logo fades out under it, so the app's window fades in over what is already a picture of it.
Five minutes because an app opened again within them has almost always been kept in memory and
comes back where it was left; one that has to start again shows its splash screen, which is the
app's colour and logo, and that is what the card shows without a picture.

## The app picture helper

A second accessibility service (`PictureService`), apart from the lock helper so each is switched
on for its own reason. A sheet says what it does before Android's page opens, which is also the
disclosure Play asks for.

- **What it asks for**: screenshots, and the one event that says another screen came to the front.
  Apps send that event whatever is listening; asking for more (what changed on the screen, what
  scrolled) would make every app send more, and cost them. It cannot read what is on the screen
  (`canRetrieveWindowContent` is false) and asks for nothing else.
- **When**: 0.7 s after an app's screen has come to the front and stayed there, then every 3 s
  while the same app stays in front with the screen on and unlocked. Never Zenith, another home app
  (recents comes from one), System UI or the lock screen. An app opened from Zenith is often still
  coming in over it at 0.7 s, so it is given up to three more goes; an app in a window over Zenith
  never has its picture taken.
- **Which are kept**: only once the app has stayed in front for 2 s after it was taken. A swipe home
  moves the app before Android says anything has changed, and a picture taken then would show it
  half way down the screen. So a picture is never newer than 2 s, and never older than 5 s, while
  the app is in front.
- **Where**: in memory, at most 1280 px on its longer side, one per app, the last four apps. All
  are dropped when the screen goes off, when the helper is switched off and when Android is short
  of memory. Nothing is written down or sent anywhere. Which app is in front is kept through the
  screen going off, since nothing says so again once it is back on: its picture is taken afresh
  once the phone is unlocked.
- **Not kept**: a picture 97% black between the status and navigation bars: an app that hides itself
  from screenshots (a bank, a private tab) comes out black, and opens and closes with the colour
  card instead. A picture taken sideways is not used on an upright home screen.
- Android 10 has no screenshots for accessibility services, so there the helper does not appear.

### Cost

- **Battery**: one screenshot every 3 s while an app is in front: the screen composed once more,
  then shrunk on a background thread. Some milliseconds each, nothing while Zenith is in front or
  the screen is off.
- **Memory**: four pictures of at most about 3 MB.
- **Play**: the Accessibility API declaration Zenith already makes for the lock helper gets a second
  use, "pictures of the app being closed, for the animation back into its icon; kept in memory for
  seconds, never stored or sent", with a video of the sheet. Installed from outside Play, Android
  greys the switch out until *Allow restricted settings* is chosen in Zenith's App info, as it does
  for the lock helper.

## Limits

- None of it has been seen on a phone. The handshake can only be tried on a Pixel-type phone with
  gesture navigation; the helper only on a phone with the service switched on.
- Without the handshake, Android's own close still plays first and the picture follows it, as the
  competitor's does. Android does not let a launcher that is not built in skip it.
- The picture can be up to 5 s old, and has the status bar in it.
- An app Android had to start again shows its splash screen after the picture, for a moment.
- The window springing into the icon does not carry the icon along with it. Launcher3 also hands
  Android a surface with the icon drawn on it, which Android moves with the window; that is left
  for later, since only a phone can show whether it lines up.

## Checked

- Unit tests: `CloseRulesTest` (which way an app closes, where it goes, the card's fades and how a
  picture fills it), `AppPicturesTest` (which pictures are kept, for how long, for which apps, and
  which are blank), `GestureHandshakeTest` (reading Android's question and the answer's shape),
  `IconPlacesTest` (which icon an app closes into), plus `AnimationTuningTest` and `SettingsJsonTest`
  for the new number and setting.
- Frames from the JVM screenshot rig: an app opening and closing with a picture, and with none.
