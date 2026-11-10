package com.gabriel.zenithui.data

/**
 * Finding apps by their names, for the drawer's search and the Search pill's, so the two always
 * find the same apps in the same order for the same letters. A name that starts with what was typed
 * comes before one that only has it somewhere inside, since that is the app being spelt out; within
 * each, the apps keep the order they were given in, so a list that was A to Z stays A to Z. Case
 * does not matter, nor do spaces either side of what was typed, and an empty search finds nothing:
 * an empty box is not a question.
 */
object AppSearch {

    /**
     * The apps among [apps] whose names have [query] in them, those starting with it first, and no
     * more than [limit] of them: enough for a list of results, while the drawer, which lists every
     * app it finds, asks for them all. Work apps are found like any others when they are passed in
     * with the rest.
     */
    fun match(query: String, apps: List<AppInfo>, limit: Int = 24): List<AppInfo> {
        val typed = query.trim()
        if (typed.isEmpty() || limit <= 0) return emptyList()
        val starting = ArrayList<AppInfo>()
        val inside = ArrayList<AppInfo>()
        for (app in apps) {
            when {
                app.label.startsWith(typed, ignoreCase = true) -> starting += app
                app.label.contains(typed, ignoreCase = true) -> inside += app
            }
        }
        return (starting + inside).take(limit)
    }

    /** The app the keyboard's Go opens for [query]: the first one [match] finds, or none. */
    fun go(query: String, apps: List<AppInfo>): AppInfo? = match(query, apps, limit = 1).firstOrNull()
}
