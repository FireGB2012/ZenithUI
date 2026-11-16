package com.gabriel.zenithui.data

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Where Zenith's config ([ZenithConfig]) is kept, and the only way it is changed. Whatever saves or
 * loads settings, the layout, Looks or themes goes through this and never to the storage under it,
 * so where they are kept can change without anything else changing: on this phone today
 * ([LocalConfigRepository]), and one day on your own server as well, by another implementation that
 * [get] hands out instead.
 *
 * A change is seen at once, in the flows, and kept for good afterwards: nothing on screen ever waits
 * for a disk, or a network. [save] is what waits, until everything so far is kept.
 */
interface ConfigRepository {
    val settings: StateFlow<ZenithSettings>
    val layout: StateFlow<List<HomeItem>>

    /** The Looks saved on this phone, newest first. */
    val looks: StateFlow<List<Look>>

    /** Themes of your own: see [ZenithConfig.themes]. */
    val themes: StateFlow<List<Theme>>

    /** All of it as it is now, as one config: what a backup holds. */
    fun config(): ZenithConfig = ZenithConfig(settings.value, layout.value, looks.value, themes.value)

    fun update(transform: (ZenithSettings) -> ZenithSettings)

    fun setLayout(items: List<HomeItem>)

    fun mutateLayout(transform: (List<HomeItem>) -> List<HomeItem>) = setLayout(transform(layout.value))

    fun setLooks(looks: List<Look>)

    /** A whole config put in, in one step, so nothing is ever half put back. */
    fun replace(config: ZenithConfig)

    /** Settings and a layout put back together, in one step; the Looks and themes stay as they are. */
    fun restore(settings: ZenithSettings, items: List<HomeItem>) = replace(config().copy(settings = settings, layout = items))

    /** Everything forgotten: Zenith as it is the first time it starts. */
    fun reset()

    /** Waits, off the main thread, until everything changed so far is kept for good. Whether it is. */
    suspend fun save(): Boolean

    companion object {
        /**
         * The one for the whole app, so a screen of its own (the one that takes a shortcut another
         * app asks to put on the home screen) changes the same layout the home screen is showing.
         */
        fun get(context: Context): ConfigRepository = LocalConfigRepository.get(context)
    }
}
