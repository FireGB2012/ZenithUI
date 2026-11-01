package com.gabriel.zenithui.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Reads the device's launchable activities and knows how to start them. */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val user: UserHandle = Process.myUserHandle()

    private val activities = HashMap<String, LauncherActivityInfo>()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private var callback: LauncherApps.Callback? = null

    suspend fun refresh(): List<AppInfo> = withContext(Dispatchers.IO) {
        val list = runCatching { launcherApps.getActivityList(null, user) }.getOrNull().orEmpty()
        val result = ArrayList<AppInfo>(list.size)
        val map = HashMap<String, LauncherActivityInfo>(list.size)
        for (info in list) {
            val cn = info.componentName
            if (cn.packageName == appContext.packageName) continue
            val key = AppInfo.keyOf(cn.packageName, cn.className)
            map[key] = info
            result += AppInfo(
                packageName = cn.packageName,
                className = cn.className,
                label = info.label?.toString().orEmpty().ifEmpty { cn.packageName },
                system = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }
        result.sortBy { it.label.lowercase() }
        synchronized(activities) {
            activities.clear()
            activities.putAll(map)
        }
        _apps.value = result
        result
    }

    fun observePackages(onChanged: () -> Unit) {
        if (callback != null) return
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChanged()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChanged()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChanged()
            override fun onPackagesAvailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChanged()
            override fun onPackagesUnavailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChanged()
        }
        runCatching { launcherApps.registerCallback(cb) }
        callback = cb
    }

    fun stopObserving() {
        callback?.let { runCatching { launcherApps.unregisterCallback(it) } }
        callback = null
    }

    fun byKey(key: String): AppInfo? = _apps.value.firstOrNull { it.key == key }

    fun activityInfo(key: String): LauncherActivityInfo? = synchronized(activities) { activities[key] }

    /**
     * The icon the app itself ships, read straight from its resources. Some phones — Xiaomi's MIUI
     * and HyperOS among them — hand launchers a themed copy instead, which is why icons looked like
     * MIUI's. Null when the app's icon cannot be read that way; then [systemIcon] is used.
     */
    fun originalIcon(key: String, densityDpi: Int): Drawable? = runCatching {
        val launcherInfo = activityInfo(key) ?: return@runCatching null
        // LauncherActivityInfo carries its ActivityInfo only from Android 12; before that the
        // package manager hands over the same thing, looked up by component name.
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcherInfo.activityInfo
        } else {
            appContext.packageManager.getActivityInfo(launcherInfo.componentName, 0)
        }
        val id = info.iconResource.takeIf { it != 0 } ?: return@runCatching null
        appContext.packageManager
            .getResourcesForApplication(info.applicationInfo)
            .getDrawableForDensity(id, densityDpi, null)
    }.getOrNull()

    fun systemIcon(key: String, densityDpi: Int): Drawable? =
        runCatching { activityInfo(key)?.getIcon(densityDpi) }.getOrNull()

    fun applicationCategory(key: String): Int =
        activityInfo(key)?.applicationInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED

    fun launch(key: String, sourceBounds: Rect? = null, opts: Bundle? = null): Boolean {
        val app = byKey(key) ?: return false
        val component = ComponentName(app.packageName, app.className)
        return runCatching {
            launcherApps.startMainActivity(component, user, sourceBounds, opts)
            true
        }.getOrElse {
            runCatching {
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                appContext.startActivity(intent, opts)
                true
            }.getOrDefault(false)
        }
    }

    // ------------------------------------------------------------ the work profile

    private val workActivities = HashMap<String, Pair<LauncherActivityInfo, UserHandle>>()

    private val _workApps = MutableStateFlow<List<AppInfo>>(emptyList())

    /** The work profile's apps, A to Z; empty on a phone with no work profile. */
    val workApps: StateFlow<List<AppInfo>> = _workApps.asStateFlow()

    suspend fun refreshWork() = withContext(Dispatchers.IO) {
        val others = runCatching { launcherApps.profiles }.getOrNull().orEmpty().filter { it != user }
        val found = HashMap<String, Pair<LauncherActivityInfo, UserHandle>>()
        val list = ArrayList<AppInfo>()
        for (profile in others) {
            for (info in runCatching { launcherApps.getActivityList(null, profile) }.getOrNull().orEmpty()) {
                val cn = info.componentName
                val app = AppInfo(
                    packageName = cn.packageName,
                    className = cn.className,
                    label = info.label?.toString().orEmpty().ifEmpty { cn.packageName },
                    system = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    work = true,
                )
                found[app.key] = info to profile
                list += app
            }
        }
        list.sortBy { it.label.lowercase() }
        synchronized(workActivities) {
            workActivities.clear()
            workActivities.putAll(found)
        }
        _workApps.value = list
    }

    fun workIcon(key: String, densityDpi: Int): Drawable? =
        runCatching { synchronized(workActivities) { workActivities[key] }?.first?.getIcon(densityDpi) }.getOrNull()

    /** Opens a work app, in the work profile. */
    fun launchWork(key: String, sourceBounds: Rect? = null, opts: Bundle? = null): Boolean {
        val (info, profile) = synchronized(workActivities) { workActivities[key] } ?: return false
        return runCatching {
            launcherApps.startMainActivity(info.componentName, profile, sourceBounds, opts)
            true
        }.getOrDefault(false)
    }

    fun openWorkAppInfo(key: String) {
        val (info, profile) = synchronized(workActivities) { workActivities[key] } ?: return
        runCatching { launcherApps.startAppDetailsActivity(info.componentName, profile, null, null) }
    }

    // ------------------------------------------------------------ shortcuts

    /** Every app's shortcuts at once, for finding one by its name from the drawer's search. */
    suspend fun allShortcuts(): List<AppShortcut> = withContext(Dispatchers.IO) {
        if (!canUseShortcuts()) return@withContext emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        val found = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty().filter { it.isEnabled }
        synchronized(shortcutInfos) { found.forEach { shortcutInfos[ShortcutKeys.of(it.`package`, it.id)] = it } }
        found.map { AppShortcut(it.`package`, it.id, (it.shortLabel ?: it.longLabel ?: "").toString()) }
    }

    /** Whether Android lets Zenith read and start apps' shortcuts: only while it is the home app. */
    fun canUseShortcuts(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    private val shortcutInfos = HashMap<String, android.content.pm.ShortcutInfo>()

    /** The shortcuts [packageName] offers from its icon, in the order it ranks them: up to five. */
    suspend fun shortcuts(packageName: String): List<AppShortcut> = withContext(Dispatchers.IO) {
        if (!canUseShortcuts()) return@withContext emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        val found = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty()
            .filter { it.isEnabled }
            // As Android's own launcher lists them: the ones the app always has first, then the
            // ones it changes as it goes, each in the app's own order.
            .sortedWith(compareBy({ if (it.isDeclaredInManifest) 0 else 1 }, { it.rank }))
            .take(MAX_SHORTCUTS)
        synchronized(shortcutInfos) { found.forEach { shortcutInfos[ShortcutKeys.of(it.`package`, it.id)] = it } }
        found.map { AppShortcut(it.`package`, it.id, (it.shortLabel ?: it.longLabel ?: "").toString()) }
    }

    private fun findShortcut(key: String): android.content.pm.ShortcutInfo? {
        synchronized(shortcutInfos) { shortcutInfos[key] }?.let { return it }
        val (pkg, id) = ShortcutKeys.parse(key) ?: return null
        if (!canUseShortcuts()) return null
        val query = LauncherApps.ShortcutQuery()
            .setPackage(pkg)
            .setShortcutIds(listOf(id))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        val info = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull()?.firstOrNull() ?: return null
        synchronized(shortcutInfos) { shortcutInfos[key] = info }
        return info
    }

    fun shortcutIcon(key: String, densityDpi: Int): Drawable? = runCatching {
        findShortcut(key)?.let { launcherApps.getShortcutIconDrawable(it, densityDpi) }
    }.getOrNull()

    /** Runs a shortcut, the way tapping it in the app's own menu would. False when it is not offered any more. */
    fun startShortcut(key: String, sourceBounds: Rect? = null, opts: Bundle? = null): Boolean {
        val (pkg, id) = ShortcutKeys.parse(key) ?: return false
        return runCatching {
            launcherApps.startShortcut(pkg, id, sourceBounds, opts, user)
            true
        }.getOrDefault(false)
    }

    /**
     * Keeps a shortcut while it is on a page, or lets it go. An app may take back a shortcut that
     * nobody has pinned, so one on the home screen is pinned for as long as it is there.
     */
    fun pinShortcut(key: String, pinned: Boolean) {
        val (pkg, id) = ShortcutKeys.parse(key) ?: return
        if (!canUseShortcuts()) return
        runCatching {
            val query = LauncherApps.ShortcutQuery().setPackage(pkg).setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            val already = launcherApps.getShortcuts(query, user).orEmpty().map { it.id }
            launcherApps.pinShortcuts(pkg, if (pinned) (already + id).distinct() else already - id, user)
        }
    }

    fun openAppInfo(key: String) {
        val app = byKey(key) ?: return
        runCatching {
            launcherApps.startAppDetailsActivity(ComponentName(app.packageName, app.className), user, null, null)
        }.onFailure {
            runCatching {
                appContext.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun uninstall(key: String) {
        val app = byKey(key) ?: return
        runCatching {
            appContext.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Best guess at the apps a person reaches for most, used to seed the dock. */
    fun suggestedDock(limit: Int = 4): List<String> {
        val picks = LinkedHashSet<String>()
        val probes = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://zenith.example")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY),
        )
        val pm = appContext.packageManager
        for (probe in probes) {
            if (picks.size >= limit) break
            val resolved = runCatching { pm.resolveActivity(probe, 0) }.getOrNull() ?: continue
            val pkg = resolved.activityInfo?.packageName ?: continue
            val app = _apps.value.firstOrNull { it.packageName == pkg } ?: continue
            picks += app.key
        }
        if (picks.size < limit) {
            _apps.value.asSequence()
                .filterNot { it.system }
                .take(limit * 2)
                .forEach { if (picks.size < limit) picks += it.key }
        }
        return picks.take(limit).toList()
    }

    /**
     * The handful of apps a fresh home screen starts with: search, mail and the browser you
     * already use. Everything else lives in the drawer.
     */
    fun essentialApps(exclude: Collection<String> = emptyList()): List<String> {
        val pm = appContext.packageManager
        val wanted = LinkedHashSet<String>()

        fun addPackage(packageName: String?) {
            val app = packageName?.let { pkg -> _apps.value.firstOrNull { it.packageName == pkg } }
            if (app != null && app.key !in exclude) wanted += app.key
        }

        fun addResolved(intent: Intent) {
            addPackage(runCatching { pm.resolveActivity(intent, 0) }.getOrNull()?.activityInfo?.packageName)
        }

        addPackage(GOOGLE_PACKAGES.firstOrNull { pkg -> _apps.value.any { it.packageName == pkg } })
        addPackage(MAIL_PACKAGES.firstOrNull { pkg -> _apps.value.any { it.packageName == pkg } })
        if (wanted.size < 2) addResolved(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL))
        addResolved(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenith.example")))

        return wanted.toList()
    }

    /** Groups apps into named buckets so onboarding can build tidy folders. */
    fun categorise(keys: List<String>): Map<String, List<String>> {
        val buckets = LinkedHashMap<String, MutableList<String>>()
        for (key in keys) {
            val name = categoryName(applicationCategory(key)) ?: continue
            buckets.getOrPut(name) { mutableListOf() } += key
        }
        return buckets.filterValues { it.size >= 3 }
    }

    /**
     * The App Library group an app belongs to. Unlike [categorise] every app gets one: whatever
     * the store did not label goes to Utilities if it came with the phone, Other if you added it.
     */
    fun libraryCategory(key: String): String =
        categoryName(applicationCategory(key))
            ?: if (byKey(key)?.system == true) "Utilities" else "Other"

    private val installTimes = HashMap<String, Long>()

    /** When the app was first installed, in epoch millis; 0 if the system will not say. */
    fun installedAt(key: String): Long {
        val pkg = byKey(key)?.packageName ?: return 0L
        return synchronized(installTimes) {
            installTimes.getOrPut(pkg) {
                runCatching { appContext.packageManager.getPackageInfo(pkg, 0).firstInstallTime }
                    .getOrDefault(0L)
            }
        }
    }

    private companion object {
        /** As many as Android's own launcher shows from an icon. */
        const val MAX_SHORTCUTS = 5

        val GOOGLE_PACKAGES = listOf("com.google.android.googlequicksearchbox", "com.google.android.apps.search")
        val MAIL_PACKAGES = listOf("com.google.android.gm", "com.samsung.android.email.provider")
    }

    private fun categoryName(category: Int): String? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "Games"
        ApplicationInfo.CATEGORY_AUDIO -> "Music"
        ApplicationInfo.CATEGORY_VIDEO -> "Video"
        ApplicationInfo.CATEGORY_IMAGE -> "Photos"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_NEWS -> "News"
        ApplicationInfo.CATEGORY_MAPS -> "Travel"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Work"
        else -> null
    }
}
