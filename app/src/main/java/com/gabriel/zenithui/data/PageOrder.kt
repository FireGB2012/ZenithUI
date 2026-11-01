package com.gabriel.zenithui.data

/**
 * Pages moved and taken away, from the page overview. Everything on a page goes where its page
 * goes, exactly where it sat on it — free-placement offsets and all — and the pages after one
 * taken away close up.
 */
object PageOrder {

    /** [item] on page [page], nothing else about it changed. */
    private fun onPage(item: HomeItem, page: Int): HomeItem =
        if (item.page == page) item else item.moved(page, item.col, item.row).nudged(item.dx, item.dy)

    /** Where page [page] is once page [from] has been moved to [to], the ones between shifting over. */
    fun newIndex(page: Int, from: Int, to: Int): Int = when {
        page == from -> to
        from < to && page in (from + 1)..to -> page - 1
        from > to && page in to until from -> page + 1
        else -> page
    }

    /** [items] with page [from] moved to [to]. */
    fun move(items: List<HomeItem>, from: Int, to: Int): List<HomeItem> {
        if (from == to) return items
        return items.map { onPage(it, newIndex(it.page, from, to)) }
    }

    /** [items] without page [page]: what stays, with the pages after it one nearer, and what went with it. */
    fun remove(items: List<HomeItem>, page: Int): Pair<List<HomeItem>, List<HomeItem>> {
        val (gone, kept) = items.partition { it.page == page }
        return kept.map { if (it.page > page) onPage(it, it.page - 1) else it } to gone
    }
}
