package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEditsTest {

    private val mail = AppInfo("com.example.mail", "com.example.mail.Main", "Mail")
    private val zoo = AppInfo("com.example.zoo", "com.example.zoo.Main", "Zoo")
    private val apps = listOf(mail, zoo)

    @Test
    fun `an edit that changes nothing is not kept`() {
        val edits = AppEdits.with(emptyMap(), mail.key, AppEdit(label = "Post"))
        assertEquals("Post", edits[mail.key]?.label)
        // Taking the name away again leaves nothing to keep.
        assertTrue(AppEdits.with(edits, mail.key, AppEdit()).isEmpty())
        // A name of nothing but spaces is no name.
        assertTrue(AppEdits.with(emptyMap(), mail.key, AppEdit(label = "   ")).isEmpty())
    }

    @Test
    fun `a name is kept as it is typed, spaces and all`() {
        // Trimmed as it was typed, "My " would lose its space and "My Mail" could never be written.
        val edits = AppEdits.with(emptyMap(), mail.key, AppEdit(label = "My "))
        assertEquals("My ", edits[mail.key]?.label)
        // It is only where it is shown that the space goes.
        assertEquals("My", AppEdits.relabel(apps, edits).first { it.key == mail.key }.label)
    }

    @Test
    fun `a renamed app is found where its new name puts it`() {
        val edits = mapOf(zoo.key to AppEdit(label = "Aardvark"))
        val relabelled = AppEdits.relabel(apps, edits)
        assertEquals(listOf("Aardvark", "Mail"), relabelled.map { it.label })
        assertEquals(zoo.key, relabelled.first().key)
        // Without a single new name the list is the very same one.
        val unnamed = mapOf(zoo.key to AppEdit(hideLabel = true))
        assertTrue(AppEdits.relabel(apps, unnamed) === apps)
    }

    @Test
    fun `edits survive being written and read back`() {
        val edits = mapOf(
            mail.key to AppEdit(label = "Post", icon = IconSource.pack("com.pack", "mail"), hideLabel = true),
            zoo.key to AppEdit(icon = IconSource.photo("photo-1.png")),
        )
        assertEquals(edits, AppEdits.fromJson(JSONObject(AppEdits.toJson(edits).toString())))
        // Nothing saved reads as nothing changed.
        assertTrue(AppEdits.fromJson(null).isEmpty())
        assertTrue(AppEdits.fromJson(JSONObject("""{"a/b":{}}""")).isEmpty())
    }

    @Test
    fun `where an icon comes from reads back as it was written`() {
        assertEquals(IconSource.Pack("com.pack", "mail_blue"), IconSource.parse(IconSource.pack("com.pack", "mail_blue")))
        assertEquals(IconSource.Photo("photo-7.png"), IconSource.parse(IconSource.photo("photo-7.png")))
        assertNull(IconSource.parse(""))
        assertNull(IconSource.parse("pack:com.pack"))
        assertNull(IconSource.parse("pack::mail"))
        // A photo is only ever a name in Zenith's own folder, never a path somewhere else.
        assertNull(IconSource.parse("photo:../../secrets.png"))
        assertNull(IconSource.parse("photo:a\\b.png"))
        assertTrue(IconSource.isSource(IconSource.pack("p", "d")))
        assertFalse(IconSource.isSource(mail.key))
    }

    @Test
    fun `a backup for another phone leaves the photos out`() {
        val edits = mapOf(
            mail.key to AppEdit(label = "Post", icon = IconSource.photo("photo-1.png")),
            zoo.key to AppEdit(icon = IconSource.photo("photo-2.png")),
        )
        val portable = AppEdits.portable(edits)
        assertEquals(AppEdit(label = "Post"), portable[mail.key])
        // An edit that was only a photo is nothing once the photo is gone.
        assertFalse(portable.containsKey(zoo.key))
    }

    @Test
    fun `the icon loader is told only about apps with a picture of their own`() {
        val edits = mapOf(
            mail.key to AppEdit(label = "Post"),
            zoo.key to AppEdit(icon = IconSource.pack("com.pack", "zoo")),
        )
        assertEquals(mapOf(zoo.key to IconSource.pack("com.pack", "zoo")), AppEdits.icons(edits))
    }
}
