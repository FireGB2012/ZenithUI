# Closing apps into their icons on every phone

- Date: 2026-11-16
- Branch: `claude/project-thread-uabqs6`, off the release branch (`claude/project-thread-jqkd80`)
- Follows: `2026-11-16-app-open-close-animations-design.md`
- Status: built. Gabriel, on the release copy: "the animtion stuff with closwi g apps dosnet worknits
  void launcher that does it search it up an fix it".

## Why it did nothing on his phone

- His phone is a Galaxy A55, on One UI. One UI does not ask launchers that are not its own where an
  app's icon is (Android Authority, February 2025, from Niagara's developer; Galaxy owners on
  Niagara's feature board, 2025), so the gesture handshake never happens there. Nor does it with
  buttons, on any phone.
- The copy that installs by tapping it has no app picture helper: Play Protect refuses a sideloaded
  app with an accessibility service.
- The colour card was off unless *Closing card* was turned up in Advanced animation settings.
- That left the icon landing alone, and it started as Zenith resumed. Resuming is part of the same
  step as starting, and on One UI Android plays its own close first and shows a launcher that is not
  built in only after it, so most of the landing was over before anyone could see it.

## How Void does it

VoidLauncher (AnonLab) is closed source and says nothing in public about how it draws its close. Its
Play listing declares an accessibility service (for its Control Center) and its privacy policy usage
access. Installed from Play, Android lets an accessibility service be switched on with no fuss, and
an accessibility service may take screenshots: most likely that is where its pictures come from,
the same way as Zenith's helper. It is exactly that service that Play Protect blocks in an APK
opened from the Files app. A launcher has no other way: the handshake, a screenshot through an
accessibility service, or a stand-in it draws itself.

## What changes

1. **Into its icon, with no handshake and no picture, the app's card shrinks back into its icon**:
   its colour and logo, the card it opened on, so closing is opening played backwards. With no icon
   on show it goes into the middle of the screen and fades, as a picture does. On by default.
   *Plain* is as it was: the landing, and the card only if *Closing card* is set.
2. ***Closing card* starts at Auto**: 72% of the way to full size closing Into its icon, none
   closing Plain. Smaller than a picture's 92%, because Android's own close has already taken the
   app away and the card carries on from about where it left it, rather than filling the screen
   again. Set to a size, the card starts at that size closing either way.
3. **The close starts once Zenith can be seen**: at once if its window is already in front,
   otherwise when the window gets focus, or 450 ms after it resumed, whichever comes first. That is
   the rule the fly-in has followed since October. Until then the home screen stays sunk back and
   the icon stays out of sight, so the first frame anyone sees is the start of the close. The same
   goes for the picture, the landing and the handshake's swell.
4. **App pictures shows only where this copy of Zenith has the helper.** The copy that installs by
   tapping goes without it, so the row would only have led to an Accessibility page with no Zenith
   on it. Its *Closing apps* note says the card does it there.

## Limits

- None of it has been seen on a phone.
- The card is the app's colour and logo, not the app. The app itself needs App pictures, which
  needs a Zenith that has the helper: from the Play Store, or put on with a computer.
- Without the helper, Zenith knows which app is closing only from having opened it in the last 30
  minutes. An app opened some other way (a notification, recents) closes Android's way only. An app
  that opened another one closes into the icon of the first.
- On One UI, Android's own close still plays first and Zenith's follows it, as Void's does. Set off
  at focus it started too soon, behind the falling app: since
  `2026-11-16-waiting-for-androids-close-design.md` the home screen waits, empty, until Android says
  its close is over.

## Checked

- Unit tests: `CloseRulesTest` (the card by default, its start size), `AnimationTuningTest` (Auto),
  `AppPicturesTest` (where the helper is offered).
- Frames from the JVM screenshot rig: an app closing with no picture, the window coming to the front
  a little after Zenith resumes.
