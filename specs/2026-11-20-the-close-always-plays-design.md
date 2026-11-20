# The close always plays

- Date: 2026-11-20
- Branch: `claude/project-thread-q308pv`, off `claude/project-thread-23neth` (PR #24, 111 / 3.9.2)
- Follows: `2026-11-16-waiting-for-androids-close-design.md`, `2026-11-17-closing-like-void-design.md`,
  `2026-11-18-closing-off-to-where-it-lives-design.md`
- Status: built. Gabriel: "Also the anim close thing make it proff so it always works": the close
  of an app back into its icon should play every time, on his Galaxy A55 (One UI, gestures) and
  on any other phone.

## What it did

Coming back from an app, Zenith worked out which app was closing from three things: Android's
answered question (the gesture handshake, which One UI is never answered), the app picture helper
(off on most phones), and the card the app was opened on, if it was still up as the app covered
Zenith. With none of them nothing of Zenith's played, and the home screen only came in. Ten ways
that happened, or the close went by unseen:

1. **An app Zenith did not open.** One opened from a notification, from another app, or from
   Recents. On a Galaxy One UI asks where its icon is, naming it, and was told nothing more than
   "no icon": the name was thrown away.
2. **An app opened with no card.** From Search, a gesture, a shortcut or anything else with no
   icon to grow out of: no card, so nothing to come back to.
3. **Zenith started afresh.** Android lets a home app's process go when it wants the memory (a
   heavy game in front, say) and makes its screen again for some changes of settings. Everything
   Zenith held about the app it opened was gone.
4. **Open for more than half an hour.** Taken to be some other app, and nothing played.
5. **Asked, with no icon on show** (an app in the drawer, on a phone that answers): Android was
   told there was none and did its own close, and nothing of Zenith's came after it.
6. **Answered, and never moved.** A phone that asks, is told, and then closes its own way (as One
   UI does, on a make Zenith does not know of): the icon swelled as if the window had got there.
7. **Opened as the screen went off.** An app that came up behind the lock screen, closed after
   the unlock: Zenith had been covered with the screen off, so it did not wait for Android's close
   and played its own hidden under it.
8. **An app that does not cover Zenith** (see-through, or a window over part of the screen):
   Zenith is paused and never stopped, so the card it opened on stayed up, full size, over the
   home screen until it was tapped.
9. **Covered again before it went.** Home, and straight back into the app before the close set
   off: the close was dropped, and the app forgotten, so the next time home nothing played.
10. **Waiting on something that never comes.** A card held up for One UI's close waited for the
    wait to end however long that took, and a closing card waited for its icon to be read.

## What changes

1. **Which app** (`CloseRules.whoCloses`), the surest first: the app Android was told an icon for;
   the app Android's question named (`GestureHandshake.named`, kept whether it was answered or
   not); the app the picture helper saw go; the app whose card was up; the last app Zenith opened
   any way at all (`CloseMemory`). When Android or the helper names the app Zenith opened, the key
   of the icon it was opened from is kept, as Android names the screen that was in front. Android's
   question counts for 5 s after it is asked, and only if it was asked since Zenith was covered.
2. **Every app Zenith opens is kept until it comes back** (`CloseMemory`): its key, the icon it was
   opened from, that icon's place at rest if it is on the page or the dock, the page, and when. It
   is kept on the phone too, so it outlives Zenith being started afresh, and let go of once a close
   sets off, or the app never covered Zenith, or the phone has been started again since.
3. **Started afresh** with an app kept, or with Android's question naming one, Zenith counts the
   app as what covered it from its first frame: the home screen sunk back, Android's close waited
   for, and the app's card into the icon it was opened from (its place kept at the launch, as
   Zenith has not seen its icons yet), or towards where it lives.
4. **Into its icon, a known app always closes some way of Zenith's** (`CloseRules.route`): Android's
   window into the icon, a picture of the app, or its card into the icon, off towards where it lives
   or into the middle. However long ago it was opened. *Plain* keeps its old rules.
5. **No icon on show, on a phone that answers**: Android is told there is none, does its own close,
   and the card goes towards where the app lives after it.
6. **A phone that never says the window got there**: the icon lands as the window gets nowhere, so
   the app is still seen going back. Twice in a row and the phone is not told any more
   (`CloseRules.trustsHandshake`, kept on the phone), and its apps close on Zenith's card. Once the
   window gets there, the count starts again.
7. **Covered with the screen off, then sent home** with an app to close is an app that came up
   behind the lock screen (`CloseRules.fromApp`): Android's close is waited for as from any app. A
   plain unlock sends nothing home, and the home screen comes in at once, as before.
8. **Back over an app that did not cover Zenith**, the card left up goes back into its icon as
   Zenith resumes (`CloseRules.cameBackOver`). One still growing is left to its own check.
9. **Covered again before it went**, the app is kept for the next time Zenith comes back.
10. **Nothing waits for long**: a card held up for Android's close goes at the most Android's close
    can take plus 1.5 s (`CloseRules.holdCap`), and a closing card waits 250 ms for its icon, then
    goes up in a colour of its own.
11. **Said in Settings** (`CloseLog`): Advanced animation settings, under *Wait for Android's
    close*, says what the last close did: which app, how Zenith knew it was that one, where it went,
    and whether Android's own close was waited out. So a close that looks wrong on a phone can be
    told apart from one that went as meant.

## Not changed

- An app Zenith knows nothing of (it did not open it, Android did not name it, the helper did not
  see it) still comes home on the home screen's own arrival: with no name there is no telling which
  app it was, and the wrong app's card would be worse than none. On a Galaxy with gestures Android
  always names it.
- The handshake on phones that follow it, the One UI card-first close, the closes off to where an
  app lives, and closing Plain or with movement reduced.

## Limits

- Not seen on a phone yet, only in the JVM rig modelled on Gabriel's A55.
- Started afresh on the way home from an app Zenith did not open, its icon's place is not known
  yet (the home screen is held sunk back until the close is over, so no place is taken down), and
  it goes into the middle of the screen, or towards its page or the drawer.
- With buttons after Recents, the last app Zenith opened is taken for the one closing, as before.

## Checked

- `CloseRulesTest`: which app closes, from each source and their order, with another of the app's
  screens, the work profile; when Android's question still names it; an app opened as the screen
  went off; the handshake trust; the hold cap; a card left up by an app that did not cover Zenith;
  and that into its icon every case closes some way of Zenith's.
- `CloseMemoryTest`: what is kept on the phone, read back as it was; anything else read as nothing;
  a phone started again since; the icon's place for a Zenith started afresh.
- `CloseLogTest`: where an app went, and what Settings says for each way.
- The close-guard rig (`/mnt/project-files/tools/close-guard-rig`, never committed): each of the
  ten ways played on the home screen, a frame at a time, as One UI with gestures, a Galaxy with
  buttons and a Pixel, checking what Zenith did and that the icon was kept out of sight for its
  card.
