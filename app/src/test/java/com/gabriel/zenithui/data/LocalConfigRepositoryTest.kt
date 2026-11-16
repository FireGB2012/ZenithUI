package com.gabriel.zenithui.data

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalConfigRepositoryTest {

    /** A phone's storage, in memory: what is written, in how many steps, and how often it was waited for. */
    private class MemoryStorage(val values: MutableMap<String, String> = mutableMapOf()) : ConfigStorage {
        val steps = mutableListOf<Set<String>>()
        var commits = 0

        override fun read(key: String): String? = values[key]

        override fun write(values: Map<String, String>) {
            steps += values.keys
            this.values.putAll(values)
        }

        override fun clear() = values.clear()

        override fun commit(): Boolean {
            commits++
            return true
        }
    }

    private val settings = ZenithSettings(userName = "Gabriel", theme = "retro", columns = 5, rows = 6)

    private val layout = listOf(
        AppCell("a", 0, 0, 0, "com.mail", ".Main"),
        WidgetCell("w", 0, 0, 1, "clock", 4, 2 * ROW_UNITS),
        FolderCell("f", 1, 0, 0, "Fun", listOf("com.game/.Main", "com.music/.Main")),
    )

    private val look = Looks.capture(settings, layout, "Night", "look-1", 1_000L)

    private val mine = Themes.Retro.copy(id = "mine", name = "Mine")

    private val config = ZenithConfig(settings, layout, listOf(look), listOf(mine))

    @Test
    fun `a new phone has the config Zenith starts with`() {
        assertEquals(ZenithConfig(), LocalConfigRepository(MemoryStorage()).config())
    }

    @Test
    fun `each part is kept under its own name, and read back by the next Zenith to start`() {
        val storage = MemoryStorage()
        val first = LocalConfigRepository(storage)
        first.update { settings }
        first.setLayout(layout)
        first.setLooks(listOf(look))
        first.replace(first.config().copy(themes = listOf(mine)))

        assertEquals(listOf(setOf("settings"), setOf("layout"), setOf("looks"), setOf("themes")), storage.steps)
        assertEquals(config, first.config())
        assertEquals(config, LocalConfigRepository(storage).config())
    }

    @Test
    fun `what Zenith kept before there was a config is read as it was, so no phone starts again`() {
        val storage = MemoryStorage(
            mutableMapOf(
                "settings" to settings.toJson().toString(),
                "layout" to JSONArray().apply { layout.forEach { put(it.toJson()) } }.toString(),
                "looks" to Looks.toJsonList(listOf(look)),
            ),
        )
        assertEquals(ZenithConfig(settings, layout, listOf(look)), LocalConfigRepository(storage).config())
    }

    @Test
    fun `a whole config goes in in one step, and only the parts that changed are written`() {
        val storage = MemoryStorage()
        val repository = LocalConfigRepository(storage)
        repository.replace(config)
        assertEquals(listOf(setOf("settings", "layout", "looks", "themes")), storage.steps)
        assertEquals(config, repository.config())

        // Settings and a layout put back, as undoing a page does: the Looks and themes are left be.
        repository.restore(settings.copy(pages = 2), layout.drop(1))
        assertEquals(setOf("settings", "layout"), storage.steps.last())
        assertEquals(config.copy(settings = settings.copy(pages = 2), layout = layout.drop(1)), LocalConfigRepository(storage).config())

        // Nothing different, nothing written.
        repository.replace(repository.config())
        assertEquals(2, storage.steps.size)
    }

    @Test
    fun `a change is seen at once, before it is on disk`() {
        val repository = LocalConfigRepository(MemoryStorage())
        repository.update { it.copy(userName = "Gabriel") }
        repository.mutateLayout { layout }
        assertEquals("Gabriel", repository.settings.value.userName)
        assertEquals(layout, repository.layout.value)
    }

    @Test
    fun `a part that cannot be read starts again, and the others are kept`() {
        val storage = MemoryStorage(
            mutableMapOf(
                "settings" to "not json",
                "layout" to JSONArray().apply { layout.forEach { put(it.toJson()) } }.put("not an item").toString(),
                "looks" to "{}",
                "themes" to "[42]",
            ),
        )
        assertEquals(ZenithConfig(layout = layout), LocalConfigRepository(storage).config())
    }

    @Test
    fun `starting again forgets everything`() {
        val storage = MemoryStorage()
        val repository = LocalConfigRepository(storage)
        repository.replace(config)
        repository.reset()
        assertTrue(storage.values.isEmpty())
        assertEquals(ZenithConfig(), repository.config())
        assertEquals(ZenithConfig(), LocalConfigRepository(storage).config())
    }

    @Test
    fun `saving waits until everything so far is on disk`() = runBlocking {
        val storage = MemoryStorage()
        val repository = LocalConfigRepository(storage)
        repository.replace(config)
        assertTrue(repository.save())
        assertEquals(1, storage.commits)
    }
}
