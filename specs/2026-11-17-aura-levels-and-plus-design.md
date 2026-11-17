# Aura: levels, and aura that buys bits of Plus

Gabriel, 17 November 2026: "aura does nothing right now". It earned (a day of turning up, a friend's
code) but since the colour themes went on 10 November it had nothing to buy, so the number only
went up. Of the four ways put to him he picked two, and both are built here:

- **B, levels.** Aura earned counts towards a level that never goes down, and each level brings
  something by itself.
- **C, aura buys bits of Plus.** Aura in hand buys a day of Zenith Plus, or one of Plus's
  wallpapers for good.

## Two numbers

- **Aura** (`aura`, as before): what you have to spend. Up with each day and each code, down when
  you buy something.
- **Earned** (`auraEarned`): all the aura you have ever earned. Spending never touches it, so
  spending never costs you a level.

Settings saved before this have no `auraEarned`. They read it as the aura in hand plus what the
Aura sets bought with it cost (`unlockedThemes`, at the prices they had), since that shop was the
only thing aura was ever spent on: everybody starts at the level they had already earned. The
level is worked out from the larger of the two numbers, so a balance can never stand above what was
earned, whatever wrote the settings.

## Levels

Level `n` is reached at `25 n (n - 1)` aura earned: 0, 50, 150, 300, 500, 750, 1,050, 1,400,
1,800, 2,250, 2,750, 3,300 and on. Each level asks 50 more than the one before, which is one more
day at the top of the streak (50 a day). There is no top level.

| Level | What it brings | Turning up every day from nothing |
|---|---|---|
| 2 | Blue flame | day 4 |
| 3 | Ember colours | day 7 |
| 4 | Purple flame | day 10 |
| 5 | Tide colours | day 14 |
| 6 | Green flame | day 19 |
| 7 | Orchid colours | day 25 |
| 8 | Gold flame | day 32 |
| 9 | Solstice colours | day 40 |
| 10 | Rose flame | day 49 |
| 11 | Obsidian colours | day 59 |
| 12 | White flame | day 70 |
| 13 on | a day of Plus, on the house, each level | |

- **Flames** are the Aura widget's flame (and the morning strip's), in a colour of your choosing
  among those reached: picked on the Aura page. The week's ticks and dots on the widget follow the
  flame. One not reached (a backup from further along, say) is drawn orange.
- **Colour sets** are the five Aura sets (Ember, Tide, Orchid, Solstice, Obsidian) as colours for
  Zenith's painted wallpapers. A set bought with aura before the shop went stays yours whatever the
  level.
- **A day of Plus on the house** past level 12 is added to any day of Plus already running.
- A level reached in the morning check-in is said on the strip that slides down: "Level 5 · Tide
  colours".

## What aura buys

| | Price | |
|---|---|---|
| A day of Plus | 250 | Everything Plus has, for 24 hours from now; bought again, a day is added to the end |
| A Plus wallpaper | 1,000 | One of the nine Plus designs (Loop, Eclipse, Silk, Halo, Ribbons, Prism, Bloom, Horizon, Nebula), yours for good |

*(my calls)*

- 250 for a day is five days at the top of the streak: someone who turns up every day can have Plus
  about one day in five, which is a taste of it and not a way round paying for it.
- 1,000 for a wallpaper is about three weeks of turning up: all nine are half a year. Plus itself
  still has them all at once, with the rest.
- A day is not sold while Plus is yours (bought, subscribed, or in the free week): it would be
  spent on nothing. A wallpaper is not sold once Plus is yours for good; it is to a subscriber, since
  a subscription can end and a wallpaper bought stays.
- Buying asks twice, as the App Store does: the price, then "Buy". Nothing is bought with one tap.
- When a day of Plus runs out, Zenith is as it is when the free week ends: the same rules, since
  Plus is simply no longer on.

Where they are sold: the Aura page, and a Plus design's own preview in the wallpaper picker, under
"Unlock with Plus" ("Or get it for 1,000 aura").

## What does not change

- How aura is earned: the daily check-in and friends' codes are as they were.
- A restored backup takes the day of Plus left from the phone, never from the file, as it does the
  free week and the purchase; the copy sent to an account leaves it out. Aura, what was earned, the
  flame and the wallpapers bought travel in a backup, as aura always has.
- A Look keeps none of it (`Looks.KEYS` is a list of what a Look keeps).
- Aura is kept on the phone and nothing checks it anywhere else. Like the free week and the Plus
  codes, that is a speed bump and not a lock, which is the right trade for a launcher.

## Where it lives

- `data/AuraRewards.kt`: `AuraLevels` (thresholds, the reward for each level, what is reached),
  `AuraFlame`; `data/AuraShop.kt`: prices, `earn`, `buyPlusDay`, `buyDesign`, `ownsDesign`.
  Plain functions, with `AuraLevelsTest` and `AuraShopTest`.
- `ZenithSettings`: `auraEarned`, `auraFlame`, `auraDesigns`, `plusPassUntil`.
- `PlusStatus.passUntil`: Plus is `active` while a day bought with aura runs; the view model ticks
  when it ends and puts the layout back on whole rows if free placement was only on through it.
- `ui/settings/AuraPage.kt`: the ring (aura in hand, round it the way to the next level), the
  streak, the levels and what they bring, what aura buys, the codes.
