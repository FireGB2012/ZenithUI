# Zenith's widgets, designed again

- Date: 2026-11-16
- Branch: `claude/project-thread-pz15n1`, off the Liquid Glass branch (`claude/project-thread-a7d4wc`)
- Status: built. Gabriel: "new widget and not the one from old zenith redesign them as a
  professional would".

## What was wrong with the old ones

Each widget had been designed on its own, at a different time, and it showed:

- **No common measure.** Every widget had its own padding, its own type sizes and its own way of
  heading itself (an accent-coloured caps line, a gradient number, a Material icon). Side by side on
  a home screen they did not look like one family.
- **Sizes that did not fit.** Some shapes cut their words off (the narrow strips, a long event, a
  city's "tomorrow"); others left half the widget empty.
- **Decoration in place of information.** A capsule and segments for the battery, a ring round the
  countdown, ruled paper behind the note.
- **Two designs for the same widget.** Weather and Calendar had an iOS layout on glass and the old
  one in "No glass".
- **Borrowed symbols.** Weather was Material icons, the clock face a plain circle.

## The family

Everything shared lives in `ui/home/widgets/WidgetKit.kt`, and each widget in a file of its own
beside it. The old widget code in `ZenithWidgets.kt` and `MoreWidgets.kt` is gone, except Quick
Toggles and Search, which are untouched.

- **One grid.** 16 dp in from the edge (9 dp on a Mini, 14 dp at the ends of a strip), 12 dp
  corners for anything set inside a widget, and a *well* (a faint rounded fill) for things that
  belong together: an event, the battery's state, an alarm.
- **One type scale.** Headline 15 sp, body 13, meta 12, caps 11 (bold, tracked, for labels). The
  big number of each widget (`Hero`) is light, with figures all one width, and rolls when it
  changes. A degree sign is pulled in so "21°" sits as one word.
- **Ink.** White on glass, with the soft shadow the icons' names have; the card's own ink on a
  colour card. Second lines are `soft()`, marks that are not read `faint()`.
- **Colour with a job.** Each widget has one hue, from iOS's: red for the calendar's day, orange
  for the flame, the second hand and alarms, yellow for notes and the sun, green for a healthy
  battery, cyan for rain, blue for lists, indigo for a countdown. A widget's own colour still eases
  in over it. On the wallpaper, through its glass or in "No glass", the picture can be any colour,
  the widget's own among them (the To-do's blue all but vanished on a blue wallpaper), so small
  coloured words there are in the ink, as an iPhone's clear widgets have them, in Light and Dark
  alike, and the colour stays in the marks: the flame, the dots, the rings, the ticks, today's disc
  (`wordHue`). On a colour card, whose colours are known, words keep their hue, and a hue that would
  not stand out gives way to the card's ink.
- **Every size designed, then fitted.** Each of the six shapes (Mini, Strip, Square, Wide, Tall,
  Big) is laid out at the phone's own widget size and scaled down as a whole when the cell is
  smaller (`Fitted`), so nothing is cut or squeezed out of place. Strips also have a narrow layout.
- **Words that fit.** Where a line can be said more or less fully, the widget uses the fullest one
  that fits its room (`FirstThatFits`): "5:30–6:15 PM · In 18 min", "5:30 PM · In 18 min",
  "In 18 min"; "+8 h, tomorrow" or "+8 h".
- **Drawn symbols.** Weather's skies (sun, moon, clouds, rain, storm, snow, mist) and the streak's
  flame are drawn paths with one soft shadow straight down (`Glyphs.kt`). The clock is a real dial
  (`Dial.kt`): a paper face by day and a dark one by night (six to six), a clear one on cards and in
  "No glass", the hours written round it on the Square, and an orange second hand.
- **Glass, Apple's way.** A button on a widget's glass is glass lying in it
  (`Modifier.glassOnGlass`), not a pane of its own, which would show the wallpaper through a hole.
  Presses spring back with one small bounce, shadows fall straight down and are soft. Nothing glows.
- **"No glass".** The same layouts, drawn in glass ink. A filled disc would swallow its symbol in
  glass ink, so the play button there is a ring.

## Each widget

- **Clock.** Mini: the face. Strip: the time, the day and the date, and the next alarm where it
  fits. Square: the face with its hours and the day under twelve. Wide: a greeting, the time and the
  date beside the face. Tall: the face, the time and date, the next alarm or a greeting. Big: a
  greeting, the date and the time across the top, a large face, the next alarm.
- **Weather.** Mini: the sky and the temperature. Strip: the sky, the temperature, the high and low,
  and the place wider. Square: the place, the temperature, the sky and today's bar. Wide: now, and
  the next hours charted. Tall: now, today's bar, the next hours down the widget. Big: now, the hours
  charted, and the days ahead on bars, as many as fit. The chart's line runs through the hours'
  temperatures, its colour from blue in the cold to red in the heat.
- **Calendar.** The day in red capitals over the date, as a calendar page has it; the events still
  to come as cards with a bar in their calendar's colour, red while one is under way. Wide adds the
  week, Tall and Big the month.
- **Music.** The album's picture with a soft shadow in its own colour, the song, three moving bars
  while it plays, how far in and how long is left, and back, play and next. Play is glass lying in
  the widget's.
- **Battery.** The charge as a ring, green, yellow in battery saver and red when low, a light running
  round it while it charges; the bigger ones add where the power comes from, the temperature, the
  health and the voltage.
- **World Clock.** A face for each city, day or night as it is there, its name or a short code, and
  how far it is from yours.
- **Countdown.** The days left large, and the wait as a field of dots, the ones gone lit and today
  ringed: a dot a day, a week or a month, as many as the field has room for (a Big one shows twelve
  weeks a dot a day).
- **Notes.** The note itself, typed on the widget, its first line as its title, headed in yellow.
  The strip shows it on one line, its end fading out, and keeps its lines for the bigger sizes.
- **To-do.** A list ticked off on the widget, in blue, with how many are left.
- **Streak.** The flame, the days in a row, this week ticked off, and your aura.
- **Photo.** Your photo to the widget's corners, the next one at a tap. A photo that cannot be read
  any more asks for photos again rather than leave the pane empty.

The gallery's line for each size (`WidgetLayouts.note`) describes the new layouts.

## Rules

`data/WidgetFacts.kt`, each tested first in `WidgetFactsTest`: the greeting, how the time and an
alarm are written, the temperature colours, the battery's words, when an event is and how long
until it, a city's short code, the hours' chart, a song's time, the week and the month, the
countdown's dots and the streak's week. Two changed on the way:

- The battery says "Charged" only at 100%. Some phones report no time left to full long before.
- The countdown's dots take a field size: a bigger widget may show a wait a dot a day.

## Checked

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- Screenshots of every widget at every size, on glass, on a colour card, in "No glass", and in
  Light, rendered on the JVM through Robolectric's native graphics.

## Not checked

- On a phone or an emulator: there is none here. Live data (a real calendar, a real song, the
  battery as it charges) and the motion are unseen there.
