package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInAppsTest {

    private fun app(pkg: String, label: String) = AppInfo(pkg, "$pkg.Main", label)

    @Test
    fun `Connect is written like any app, under a package no phone app is listed under`() {
        val connect = BuiltInApps.app(BuiltInApps.CONNECT)
        assertNotNull(connect)
        // "package/class", like every app's key, so a cell, the dock, a Look and a backup keep it as they are.
        assertEquals("${BuiltInApps.PACKAGE}/connect", connect!!.key)
        assertEquals(BuiltInApps.CONNECT, AppInfo.keyOf(connect.packageName, connect.className))
        assertEquals("Connect", connect.label)
        // It came with Zenith, so it is not among the apps you installed lately.
        assertTrue(connect.system)
    }

    @Test
    fun `only Zenith's own apps are built in`() {
        assertTrue(BuiltInApps.isBuiltIn(BuiltInApps.CONNECT))
        assertFalse(BuiltInApps.isBuiltIn("com.android.chrome/com.google.android.apps.chrome.Main"))
        // Not a package that merely starts the same way, a work app's key, or a shortcut.
        assertFalse(BuiltInApps.isBuiltIn("zenith.builtinx/connect"))
        assertFalse(BuiltInApps.isBuiltIn(WorkKeys.of(BuiltInApps.CONNECT)))
        assertFalse(BuiltInApps.isBuiltIn(ShortcutKeys.PREFIX + "zenith.builtin/connect"))
        assertFalse(BuiltInApps.isBuiltIn(null))
        assertFalse(BuiltInApps.isBuiltIn(""))
    }

    @Test
    fun `the built-in apps join the phone's, in order of name with them`() {
        val phone = listOf(app("com.a", "Calendar"), app("com.b", "Camera"), app("com.c", "Docs"))
        val all = BuiltInApps.joinedWith(phone)
        assertEquals(listOf("Calendar", "Camera", "Connect", "Docs"), all.map { it.label })
        // Asked again with the list it made, it does not add them twice.
        assertEquals(all, BuiltInApps.joinedWith(all))
        // A phone with no apps it can read still has Zenith's.
        assertEquals(listOf("Connect"), BuiltInApps.joinedWith(emptyList()).map { it.label })
    }

    @Test
    fun `a built-in app goes on the home screen once, and not again once taken off`() {
        val key = BuiltInApps.CONNECT
        assertTrue(BuiltInApps.needsPlacing(key, placed = emptySet(), onHome = false))
        // Put there by a Look, a backup or by hand: it counts as put there.
        assertFalse(BuiltInApps.needsPlacing(key, placed = emptySet(), onHome = true))
        // Put there once and taken off since: it stays in the drawer.
        assertFalse(BuiltInApps.needsPlacing(key, placed = setOf(key), onHome = false))
    }
}
