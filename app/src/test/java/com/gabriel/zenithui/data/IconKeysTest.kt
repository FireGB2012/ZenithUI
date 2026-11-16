package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IconKeysTest {

    @Test
    fun `an app's icon belongs to its package, however it was drawn`() {
        assertEquals("com.spotify.music", IconKeys.packageOf("com.spotify.music/com.spotify.music.MainActivity|189|squircle||"))
        assertEquals("com.spotify.music", IconKeys.packageOf("glyph|com.spotify.music/.Main|189|squircle|com.pack|"))
        // One you chose a picture for is still that app's icon.
        assertEquals("org.app", IconKeys.packageOf("org.app/.Main|158|circle||pack:com.pack:org_app"))
    }

    @Test
    fun `a work app's icon and a shortcut's belong to the package behind them`() {
        assertEquals("com.slack", IconKeys.packageOf("work:com.slack/.Main|189|squircle||"))
        assertEquals("com.google.maps", IconKeys.packageOf("shortcut:com.google.maps/home/route|189|squircle||"))
    }

    @Test
    fun `a pack's picture asked for by itself belongs to no app`() {
        assertEquals("", IconKeys.packageOf("pack:com.pack:org_app|189|squircle||"))
        assertEquals("", IconKeys.packageOf("photo:photo-1.png|189|squircle||"))
    }
}
