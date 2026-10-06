package com.gabriel.zenithui.data

/**
 * How much body the glass has. Clear glass is only as visible as the picture behind it lets it
 * be: over a wallpaper as dark as the pane itself, or as pale, it all but disappears. Frosted
 * lifts every pane a step away from whatever is behind it, and Solid lays a fuller veil on top of
 * that for a busy wallpaper.
 */
enum class GlassBody(
    val id: String,
    val label: String,
    /** What choosing it does, as Settings says it. */
    val detail: String,
    /** How far a pane is lifted off a picture of its own tone: 0 not at all, 1 a clear step. */
    val lift: Float,
    /** Extra veil over what is seen through every pane. */
    val solid: Float,
) {
    Clear("clear", "Clear", "The wallpaper shows straight through. Panels can fade into a dark or plain one.", 0f, 0f),
    Frosted("frosted", "Frosted", "A light frost lifts every panel off the wallpaper, so its edges and words stand out.", 1f, 0f),
    Solid("solid", "Solid", "The fullest panels: best over a busy photo, with the least of it showing through.", 1f, 0.4f);

    companion object {
        /** Frosted for anything unknown, including settings saved before there was a choice. */
        fun byId(id: String): GlassBody = entries.firstOrNull { it.id == id } ?: Frosted
    }
}
