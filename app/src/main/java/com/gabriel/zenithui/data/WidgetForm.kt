package com.gabriel.zenithui.data

/**
 * The shape of space a widget has been given. Every widget has a layout of its own for each one,
 * so a widget is designed for the space it is in rather than the same design stretched or
 * squashed. Sizes are in dp.
 */
enum class WidgetForm {
    /** About one icon: the single most useful thing, and nothing else. */
    Mini,

    /** Short and wide, about one row tall: a line of information. */
    Strip,

    /** Roughly square: the classic small widget. */
    Square,

    /** Clearly wider than tall. */
    Wide,

    /** Clearly taller than wide. */
    Tall,

    /** Large both ways. */
    Big;

    companion object {
        fun of(width: Float, height: Float): WidgetForm = when {
            height < 100f && width >= 130f -> Strip
            width < 130f || height < 100f -> Mini
            width >= 260f && height >= 230f -> Big
            width >= height * 1.55f -> Wide
            height >= width * 1.45f -> Tall
            else -> Square
        }
    }
}

/**
 * How the Quick Toggles widget sets out the controls chosen for it at one size: on whichever grid
 * gives each of them the most room, as many of them as can still be pressed, and in which style.
 * Sizes are in dp.
 */
data class ToggleGrid(
    /** How many of the chosen controls there is room for, counted from the first. */
    val shown: Int,
    val columns: Int,
    val rows: Int,
    val cellWidth: Float,
    val cellHeight: Float,
    val style: Style,
    /** Whether a tile has room for a second line under its name. */
    val detail: Boolean,
) {
    enum class Style {
        /** Big cells: a tile for each, with its name. */
        Tiles,

        /** One column of wide cells: a button with its name beside it. */
        List,

        /** Round buttons with their names underneath. */
        Named,

        /** Round buttons only. */
        Buttons,
    }

    companion object {
        /** Below this each control would be too small to press reliably. */
        const val MIN_CELL = 30f

        /** The height of cell from which a tile has room for a second line under its name. */
        const val DETAIL_FROM = 92f

        /**
         * [chosen] controls in a widget [width] by [height]. A widget too small for all of them
         * shows the first few that still fit at a size you can press, rather than shrinking them
         * all to specks.
         */
        fun of(width: Float, height: Float, chosen: Int): ToggleGrid {
            val pad = if (WidgetForm.of(width, height) == WidgetForm.Mini) 6f else 12f
            val innerWidth = width - pad * 2
            val innerHeight = height - pad * 2
            fun rowsFor(count: Int, columns: Int) = (count + columns - 1) / columns
            fun room(count: Int, columns: Int) = minOf(innerWidth / columns, innerHeight / rowsFor(count, columns))
            fun bestColumns(count: Int) = (1..count).maxByOrNull { room(count, it) } ?: 1
            val shown = (chosen.coerceAtLeast(1) downTo 1).firstOrNull { room(it, bestColumns(it)) >= MIN_CELL } ?: 1
            val columns = bestColumns(shown)
            val rows = rowsFor(shown, columns)
            val cellWidth = innerWidth / columns
            val cellHeight = innerHeight / rows
            // Big cells become named tiles; a single column of wide cells becomes a list;
            // otherwise round buttons, named underneath when there is room.
            val style = when {
                cellWidth >= 120f && cellHeight >= 80f -> Style.Tiles
                columns == 1 && cellWidth >= 110f -> Style.List
                cellHeight >= 66f && cellWidth >= 58f -> Style.Named
                else -> Style.Buttons
            }
            return ToggleGrid(shown, columns, rows, cellWidth, cellHeight, style, detail = style == Style.Tiles && cellHeight >= DETAIL_FROM)
        }
    }
}

/**
 * What a widget shows at one size, in a line: the widget showcase says it under each size, so the
 * sizes can be told apart by more than their outline. Each line describes the layout the widget
 * itself picks for that shape of space ([WidgetForm.of]), and nothing it does not draw.
 */
object WidgetLayouts {

    /**
     * [kind] is the widget's type without its "No glass" prefix ([Widgets.baseType]), which is set
     * out just as the glass one is; [width] and [height] are its size on the home page, in dp;
     * [toggles] is how many controls the Quick Toggles widget has been given. Null for a kind that has
     * no layouts of its own.
     */
    fun note(kind: String, width: Float, height: Float, toggles: Int = 4): String? {
        val form = WidgetForm.of(width, height)
        return when (kind) {
            "clock" -> when (form) {
                WidgetForm.Mini -> "A clock face, and nothing else."
                WidgetForm.Strip -> "The time, the day and the date; wider, your next alarm too."
                WidgetForm.Square -> "A clock face with its hours round it, and the day on it."
                WidgetForm.Wide -> "A greeting, the time and the date, beside a clock face."
                WidgetForm.Tall -> "A clock face, the time and the date under it, and your next alarm or a greeting."
                WidgetForm.Big -> "A greeting, the date and the time across the top, a large clock face, and your next alarm."
            }
            "battery" -> when (form) {
                WidgetForm.Mini -> "A ring round the charge."
                WidgetForm.Strip -> "A battery filled as far as the charge, the percentage, and what it is doing."
                WidgetForm.Square -> "A ring round the charge, and what it is doing under it."
                WidgetForm.Wide -> "The ring, with what it is doing and the battery's state beside it."
                WidgetForm.Tall -> "The ring, what it is doing, and the battery's state at the foot."
                WidgetForm.Big -> "A large ring, and four tiles: power, temperature, health and voltage."
            }
            "calendar" -> when (form) {
                WidgetForm.Mini -> "The day over today's date."
                WidgetForm.Strip -> "Today's date, and your next event beside it."
                WidgetForm.Square -> "The day in red, a big date, and your next event."
                WidgetForm.Wide -> "Today's date, with the week and your next events beside it."
                WidgetForm.Tall -> "Today's date over the month, and your next events under it."
                WidgetForm.Big -> "Today's date across the top; the month and the day's events side by side."
            }
            "notes" -> when (form) {
                WidgetForm.Mini -> "Your note's first words."
                WidgetForm.Strip -> "Your note on one line, to jot on."
                WidgetForm.Square -> "A note to write on, its first line its title."
                WidgetForm.Wide -> "A wide note, with today's date."
                WidgetForm.Tall -> "A tall note, with today's date."
                WidgetForm.Big -> "A large note, with today's date, in bigger writing."
            }
            "toggles" -> togglesNote(ToggleGrid.of(width, height, toggles), toggles.coerceAtLeast(1))
            "media" -> when (form) {
                WidgetForm.Mini -> "The album's picture, with play and pause on it."
                WidgetForm.Strip -> "The picture, the song and play; wider, next too."
                WidgetForm.Square -> "The picture and play at the top, the song and how far in under them."
                WidgetForm.Wide -> "The picture on the left; the song, how far in and the controls on the right."
                WidgetForm.Tall -> "The picture across the top; the song, how far in and the controls under it."
                WidgetForm.Big -> "The picture and the song side by side; how far in and large controls under them."
            }
            "world" -> when (form) {
                WidgetForm.Mini -> "The first city's clock face, its name on it."
                WidgetForm.Strip -> "Two cities in a line, four when wider, each with its time."
                WidgetForm.Square -> "Three cities, a line each: how far from yours, and the time."
                WidgetForm.Wide -> "Up to four cities side by side, each a clock face."
                WidgetForm.Tall -> "Up to four cities down the widget, each a clock face with its time."
                WidgetForm.Big -> "Four large clock faces, two by two, with each city's time and how far."
            }
            "countdown" -> when (form) {
                WidgetForm.Mini -> "The days left."
                WidgetForm.Strip -> "The days left, what to and when; wider, how far along."
                WidgetForm.Square -> "What to, the days left, and the wait as dots."
                WidgetForm.Wide -> "The days left and the date on the left; the wait as dots on the right."
                WidgetForm.Tall -> "The days left and the date, and the wait as dots under them."
                WidgetForm.Big -> "The days left large, and the wait as a field of dots, a dot a day where it fits."
            }
            "photo" -> when (form) {
                WidgetForm.Mini -> "A small square of your photo."
                WidgetForm.Strip -> "A letterbox of your photo."
                WidgetForm.Square -> "Your photo, cut to a square."
                WidgetForm.Wide -> "Your photo, wide."
                WidgetForm.Tall -> "Your photo, tall."
                WidgetForm.Big -> "Your photo, large."
            }
            "todo" -> when (form) {
                WidgetForm.Mini -> "How many things are left to do."
                WidgetForm.Strip -> "The next thing to do, its tick, and how many more."
                WidgetForm.Square -> "The first few things to do, with a tick by each."
                WidgetForm.Wide -> "The list in two columns."
                WidgetForm.Tall -> "The list, and a line to add one at the foot."
                WidgetForm.Big -> "The whole list, a line to add one, and a way to clear what is done."
            }
            "aura" -> when (form) {
                WidgetForm.Mini -> "Your streak under a flame."
                WidgetForm.Strip -> "The flame and your streak; wider, your aura and the week as dots."
                WidgetForm.Square -> "Your streak, large, and this week's days ticked off."
                WidgetForm.Wide -> "The streak on the left; this week and your aura on the right."
                WidgetForm.Tall -> "The flame, the streak, the week and your aura, down the widget."
                WidgetForm.Big -> "A large flame and streak, your aura, and this week."
            }
            "weather" -> when (form) {
                WidgetForm.Mini -> "The sky and the temperature."
                WidgetForm.Strip -> "The sky, the temperature, and today's high and low; wider, the place too."
                WidgetForm.Square -> "The place, the temperature large, the sky, and today's high and low on a bar."
                WidgetForm.Wide -> "Now on the left, and the next hours charted on the right."
                WidgetForm.Tall -> "Now and today's bar, and the next hours down the widget."
                WidgetForm.Big -> "Now, the next hours charted, and the days ahead on bars."
            }
            else -> null
        }
    }

    private fun togglesNote(grid: ToggleGrid, chosen: Int): String {
        val how = when (grid.style) {
            ToggleGrid.Style.Tiles -> if (grid.detail) "Tiles, each with its name and On, Off or Open under it." else "Tiles, each with its name."
            ToggleGrid.Style.List -> "A list, each control with its name beside it."
            ToggleGrid.Style.Named -> "Round buttons, each with its name under it."
            ToggleGrid.Style.Buttons -> "Round buttons, the icons alone."
        }
        return if (grid.shown < chosen) "$how Room for ${grid.shown} of your $chosen." else how
    }
}
