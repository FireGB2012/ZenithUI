package com.gabriel.zenithui.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullPageTest {

    private val eps = 1e-4f
    private val tall = 2000f

    @Test
    fun `a page in its place is drawn there and covers the whole home screen, which has sunk back a little`() {
        val down = pageDown(pulled = 0f, up = 1f, height = tall)
        assertEquals(0f, down, eps)
        assertEquals(1f, pageCover(down, tall), eps)
        assertEquals(1f - PAGE_SINK, homeScaleUnderPage(1f), eps)
    }

    @Test
    fun `a page half way up covers half the home screen`() {
        val down = pageDown(pulled = 0f, up = 0.5f, height = tall)
        assertEquals(tall / 2f, down, eps)
        assertEquals(0.5f, pageCover(down, tall), eps)
    }

    @Test
    fun `a page under a finger is exactly where the finger has pulled it`() {
        assertEquals(300f, pageDown(pulled = 300f, up = 1f, height = tall), eps)
        val cover = pageCover(pageDown(pulled = tall / 2f, up = 1f, height = tall), tall)
        assertEquals(0.5f, cover, eps)
        assertEquals(1f - PAGE_SINK / 2f, homeScaleUnderPage(cover), eps)
    }

    @Test
    fun `a page thrown away goes on down from where the finger left it, not back from its place`() {
        // Let go 800 pixels down, it is closed: on its way off it is only ever further down than that.
        assertEquals(800f, pageDown(pulled = 800f, up = 0.8f, height = tall), eps)
        // Once its own going has caught up with it, that takes it the rest of the way.
        assertEquals(1600f, pageDown(pulled = 800f, up = 0.2f, height = tall), eps)
        assertEquals(tall, pageDown(pulled = 800f, up = 0f, height = tall), eps)
    }

    @Test
    fun `a page never goes up past its place`() {
        assertEquals(0f, pageDown(pulled = -60f, up = 1f, height = tall), eps)
        assertEquals(0f, pageDown(pulled = 0f, up = 1.02f, height = tall), eps)
    }

    @Test
    fun `cover never goes past none or all`() {
        assertEquals(0f, pageCover(down = tall * 1.5f, height = tall), eps)
        assertEquals(1f, pageCover(down = -10f, height = tall), eps)
        // Not yet measured, it covers nothing.
        assertEquals(0f, pageCover(down = 0f, height = 0f), eps)
        assertEquals(1f - PAGE_SINK, homeScaleUnderPage(3f), eps)
        assertEquals(1f, homeScaleUnderPage(-1f), eps)
    }

    @Test
    fun `the home screen sinks back no further under a page than under the drawer`() {
        // The drawer sinks it by 6%; a page, which covers it whole, by less.
        assertTrue(PAGE_SINK > 0f && PAGE_SINK <= 0.06f)
    }

    @Test
    fun `the page covering the most of the home screen is the one that counts`() {
        val home = HomeCover()
        assertEquals(0f, home.amount(), eps)
        val settings = { 0.3f }
        val gallery = { 0.8f }
        home.add(settings)
        home.add(gallery)
        assertEquals(0.8f, home.amount(), eps)
        home.remove(gallery)
        assertEquals(0.3f, home.amount(), eps)
        home.remove(settings)
        assertEquals(0f, home.amount(), eps)
    }
}
