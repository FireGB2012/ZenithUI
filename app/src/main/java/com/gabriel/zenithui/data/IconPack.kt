package com.gabriel.zenithui.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.util.Xml
import androidx.compose.runtime.Immutable
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser

@Immutable
data class IconPackInfo(val packageName: String, val label: String)

/** Finds third-party icon packs and reads their appfilter mapping. */
object IconPacks {

    private val ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme",
        "ch.deletescape.lawnchair.ICONPACK",
        "com.dlto.atom.launcher.THEME",
    )

    fun installed(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val found = LinkedHashMap<String, IconPackInfo>()
        for (action in ACTIONS) {
            val intent = Intent(action)
            val results = runCatching { pm.queryIntentActivities(intent, 0) }.getOrNull().orEmpty()
            for (info in results) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (found.containsKey(pkg)) continue
                val label = runCatching { info.loadLabel(pm).toString() }.getOrNull() ?: pkg
                found[pkg] = IconPackInfo(pkg, label)
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }
}

class IconPack(context: Context, val packageName: String) {

    private val appContext = context.applicationContext
    private var resources: Resources? = null
    private val mapping = HashMap<String, String>()

    val isLoaded: Boolean get() = resources != null

    fun load(): Boolean {
        val res = runCatching { appContext.packageManager.getResourcesForApplication(packageName) }.getOrNull()
            ?: return false
        resources = res
        parseAppFilter(res)
        return true
    }

    fun drawableFor(component: ComponentName): Drawable? {
        val res = resources ?: return null
        val name = mapping["${component.packageName}/${component.className}"]
            ?: mapping[component.packageName]
            ?: return null
        val id = runCatching { res.getIdentifier(name, "drawable", packageName) }.getOrDefault(0)
        if (id == 0) return null
        return runCatching { ResourcesCompat.getDrawable(res, id, null) }.getOrNull()
    }

    private fun parseAppFilter(res: Resources) {
        val parser: XmlPullParser = openAssetParser(res) ?: openXmlParser(res) ?: return
        runCatching {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (!component.isNullOrEmpty() && !drawable.isNullOrEmpty()) {
                        componentKey(component)?.let { mapping[it] = drawable }
                    }
                }
                event = parser.next()
            }
        }
        (parser as? XmlResourceParser)?.close()
    }

    private fun openAssetParser(res: Resources): XmlPullParser? = runCatching {
        val stream = res.assets.open("appfilter.xml")
        Xml.newPullParser().apply { setInput(stream, null) }
    }.getOrNull()

    private fun openXmlParser(res: Resources): XmlPullParser? = runCatching {
        val id = res.getIdentifier("appfilter", "xml", packageName)
        if (id == 0) null else res.getXml(id)
    }.getOrNull()

    /** "ComponentInfo{pkg/cls}" -> "pkg/cls" */
    private fun componentKey(raw: String): String? {
        val trimmed = raw.removePrefix("ComponentInfo{").removeSuffix("}")
        if (trimmed.isEmpty()) return null
        return if (trimmed.contains('/')) trimmed else null
    }
}
