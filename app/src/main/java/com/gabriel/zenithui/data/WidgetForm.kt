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
     * [kind] is the widget's type without its "No glass" prefix ([Widgets.baseType]); [width] and
     * [height] are its size on the home page, in dp; [toggles] is how many controls the Quick
     * Toggles widget has been given. Null for a kind that has no layouts of its own.
     */
    fun note(kind: String, width: Float, height: Float, toggles: Int = 4): String? {
        val form = WidgetForm.of(width, height)
        return when (kind) {
            "clock" -> when (form) {
                WidgetForm.Mini -> "Just the time, as large as it will go."
                WidgetForm.Strip -> "The time, with the day and the date beside it."
                WidgetForm.Square -> "Hours over minutes, like a Lock Screen, with the day and the date."
                WidgetForm.Wide -> "The time, with the day, the date and a greeting beside it."
                WidgetForm.Tall -> "A clock face, with the time and the date under it."
                WidgetForm.Big -> "A large clock face: the date and the time above it, a greeting below."
            }
            "battery" -> when (form) {
                WidgetForm.Mini -> "A thin ring, with the charge inside it."
                WidgetForm.Strip -> "A bar that fills with the charge, and the percentage."
                WidgetForm.Square -> "A ring round the charge, and whether it is charging."
                WidgetForm.Wide -> "A ring, with the charge, its state and a gauge beside it."
                WidgetForm.Tall -> "A tall capsule that fills from the bottom, with the charge."
                WidgetForm.Big -> "A large ring with the charge in the middle, and a gauge under it."
            }
            "calendar" -> when (form) {
                WidgetForm.Mini -> "The day of the week over today's date."
                WidgetForm.Strip -> "Today's date, with the day and the month beside it."
                WidgetForm.Square -> "The day, a big date and the month, like a tear-off calendar."
                WidgetForm.Wide -> "Today on the left, the whole month on the right."
                WidgetForm.Tall -> "The whole month, headed by today."
                WidgetForm.Big -> "The whole month, large, with today's date over it."
            }
            "notes" -> when (form) {
                WidgetForm.Mini -> "A pencil, and a couple of lines to write on."
                WidgetForm.Strip -> "One line to jot on."
                WidgetForm.Square -> "A little card to write on."
                WidgetForm.Wide -> "A sticky note, with a margin down its side."
                WidgetForm.Tall -> "Ruled paper, with today's date."
                WidgetForm.Big -> "A notepad page: a heading, today's date and ruled lines."
            }
            "toggles" -> togglesNote(ToggleGrid.of(width, height, toggles), toggles.coerceAtLeast(1))
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
