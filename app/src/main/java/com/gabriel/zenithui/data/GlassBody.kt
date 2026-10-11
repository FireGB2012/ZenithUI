package com.gabriel.zenithui.data

/**
 * How much body the glass has. Clear is Zenith's liquid glass with nothing added, and what
 * everybody has unless they ask for more: the wallpaper through it, softened and bent and lit as
 * glass does, so over a picture as dark as the pane itself, or as pale, the pane is mostly its
 * rim. Frosted lifts every pane a step away from whatever is behind it, and Solid lays a fuller
 * veil on top of that for a busy wallpaper.
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
    Clear("clear", "Clear", "Liquid glass with nothing added: the wallpaper shows through, softened, and lights the rim.", 0f, 0f),
    Frosted("frosted", "Frosted", "A light frost lifts every panel off a dark or plain wallpaper, so its edges stand out.", 1f, 0f),
    Solid("solid", "Solid", "The fullest panels: best over a busy photo, with the least of it showing through.", 1f, 0.4f);

    companion object {
        /** Clear for anything unknown, including settings saved before there was a choice. */
        fun byId(id: String): GlassBody = entries.firstOrNull { it.id == id } ?: Clear
    }
}
