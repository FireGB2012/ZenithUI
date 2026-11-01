package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Sums typed into the drawer's search: `2+2*3`, `(4.5 − 1) × 2`, `2^10`, `15% of 80`, `80 + 15%`.
 * Read by a small recursive parser over the usual rules: brackets, then powers, then times and
 * divided by, then plus and minus. Anything it cannot read is not a sum.
 */
object Calculator {

    /** The answer to [text] if it is a sum — something with a number and an operation in it. */
    fun evaluate(text: String): Double? {
        val clean = text.trim().lowercase()
            .replace('×', '*').replace('÷', '/').replace('−', '-').replace(',', '.')
            .replace(" ", "")
        if (clean.isEmpty() || clean.none { it.isDigit() }) return null
        // "15% of 80" is a share, and needs no other operation to be a sum.
        Regex("""^(-?\d+(?:\.\d+)?)%of(-?\d+(?:\.\d+)?)$""").matchEntire(clean)?.let { m ->
            return m.groupValues[1].toDouble() / 100.0 * m.groupValues[2].toDouble()
        }
        // A number on its own is not a sum worth answering.
        if (clean.none { it in "+-*/^%()" } || Regex("""^-?\d+(\.\d+)?$""").matches(clean)) return null
        return runCatching { Parser(clean).whole() }.getOrNull()?.takeIf { it.isFinite() }
    }

    /** An answer as it should be read: no long tail of digits, no trailing ".0". */
    fun show(value: Double): String {
        if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
        val rounded = BigDecimal(value).round(MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros()
        return rounded.toPlainString()
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun whole(): Double {
            val value = sum()
            require(i == s.length)
            return value
        }

        private fun sum(): Double {
            var value = product()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                // "80 + 15%" is eighty and fifteen per cent of it, as on a pocket calculator.
                val start = i
                val next = product()
                val percent = i > start && s[i - 1] == '%'
                val amount = if (percent) value * next else next
                value = if (op == '+') value + amount else value - amount
            }
            return value
        }

        private fun product(): Double {
            var value = power()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]
                val next = power()
                value = if (op == '*') value * next else value / next
            }
            return value
        }

        private fun power(): Double {
            val base = unary()
            if (i < s.length && s[i] == '^') {
                i++
                return Math.pow(base, power())
            }
            return base
        }

        private fun unary(): Double {
            if (i < s.length && s[i] == '-') {
                i++
                return -unary()
            }
            if (i < s.length && s[i] == '+') {
                i++
                return unary()
            }
            return percent()
        }

        private fun percent(): Double {
            val value = atom()
            if (i < s.length && s[i] == '%') {
                i++
                return value / 100.0
            }
            return value
        }

        private fun atom(): Double {
            if (i < s.length && s[i] == '(') {
                i++
                val value = sum()
                require(i < s.length && s[i] == ')')
                i++
                return value
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start)
            return s.substring(start, i).toDouble()
        }
    }
}

/** One conversion found in what was typed: "5 km" is "3.107 miles". */
@Immutable
data class Conversion(val from: String, val to: String)

/**
 * Conversions typed into the drawer's search: `5 km in miles`, `30 c to f`, `2 cups in ml`,
 * `100 mph to km/h`, `3 gb in mb`. A handful of everyday units of length, weight, temperature,
 * volume, speed and data, by their names, plurals and short forms.
 */
object Units {
    private data class Unit(val names: List<String>, val kind: String, val toBase: (Double) -> Double, val fromBase: (Double) -> Double, val say: String)

    private fun scaled(kind: String, factor: Double, say: String, vararg names: String) =
        Unit(names.toList(), kind, { it * factor }, { it / factor }, say)

    private val all = listOf(
        scaled("length", 0.001, "mm", "mm", "millimetre", "millimetres", "millimeter", "millimeters"),
        scaled("length", 0.01, "cm", "cm", "centimetre", "centimetres", "centimeter", "centimeters"),
        scaled("length", 1.0, "m", "m", "metre", "metres", "meter", "meters"),
        scaled("length", 1000.0, "km", "km", "kilometre", "kilometres", "kilometer", "kilometers"),
        scaled("length", 0.0254, "in", "in", "inch", "inches", "\""),
        scaled("length", 0.3048, "ft", "ft", "foot", "feet", "'"),
        scaled("length", 0.9144, "yd", "yd", "yard", "yards"),
        scaled("length", 1609.344, "miles", "mi", "mile", "miles"),
        scaled("mass", 0.001, "g", "g", "gram", "grams"),
        scaled("mass", 1.0, "kg", "kg", "kilo", "kilos", "kilogram", "kilograms"),
        scaled("mass", 0.028349523125, "oz", "oz", "ounce", "ounces"),
        scaled("mass", 0.45359237, "lb", "lb", "lbs", "pound", "pounds"),
        scaled("mass", 6.35029318, "st", "st", "stone", "stones"),
        Unit(listOf("c", "°c", "celsius", "centigrade"), "temperature", { it }, { it }, "°C"),
        Unit(listOf("f", "°f", "fahrenheit"), "temperature", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }, "°F"),
        Unit(listOf("k", "kelvin"), "temperature", { it - 273.15 }, { it + 273.15 }, "K"),
        scaled("volume", 0.001, "ml", "ml", "millilitre", "millilitres", "milliliter", "milliliters"),
        scaled("volume", 1.0, "l", "l", "litre", "litres", "liter", "liters"),
        scaled("volume", 0.24, "cups", "cup", "cups"),
        scaled("volume", 0.0295735, "fl oz", "floz", "fl oz", "fluid ounce", "fluid ounces"),
        scaled("volume", 0.568261, "pints", "pint", "pints", "pt"),
        scaled("volume", 3.785411784, "gal", "gal", "gallon", "gallons"),
        scaled("speed", 1.0, "km/h", "km/h", "kmh", "kph", "kilometres an hour"),
        scaled("speed", 1.609344, "mph", "mph", "miles an hour"),
        scaled("speed", 3.6, "m/s", "m/s", "mps"),
        scaled("data", 1.0, "KB", "kb", "kilobyte", "kilobytes"),
        scaled("data", 1024.0, "MB", "mb", "megabyte", "megabytes"),
        scaled("data", 1024.0 * 1024, "GB", "gb", "gigabyte", "gigabytes"),
        scaled("data", 1024.0 * 1024 * 1024, "TB", "tb", "terabyte", "terabytes"),
    )

    private fun find(name: String): Unit? {
        val clean = name.trim().lowercase()
        return all.firstOrNull { clean in it.names }
    }

    private val pattern = Regex("""^\s*(-?\d+(?:[.,]\d+)?)\s*([a-z°"'/ ]+?)\s+(?:in|to|as|into)\s+([a-z°"'/ ]+?)\s*$""")

    /** What [text] comes to in the other unit, if it asks for a conversion between two of a kind. */
    fun convert(text: String): Conversion? {
        val m = pattern.matchEntire(text.lowercase()) ?: return null
        val amount = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val from = find(m.groupValues[2]) ?: return null
        val to = find(m.groupValues[3]) ?: return null
        if (from.kind != to.kind || from == to) return null
        val result = to.fromBase(from.toBase(amount))
        return Conversion("${Calculator.show(amount)} ${from.say}", "${round(result)} ${to.say}")
    }

    private fun round(value: Double): String {
        val places = when {
            kotlin.math.abs(value) >= 100 -> 1
            kotlin.math.abs(value) >= 1 -> 2
            else -> 4
        }
        return Calculator.show(BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble())
    }
}

/**
 * Android's own settings, found by name from the drawer: "bluetooth", "battery", "wifi". Each is
 * the screen Android opens for that intent; Zenith's own Settings is among them.
 */
object SettingsLinks {
    @Immutable
    data class Link(val title: String, val action: String, val words: String)

    /** Opens Zenith's own Settings rather than an Android screen. */
    const val ZENITH = "zenith.settings"

    val all = listOf(
        Link("Wi-Fi", android.provider.Settings.ACTION_WIFI_SETTINGS, "wifi wireless internet network"),
        Link("Bluetooth", android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, "bluetooth headphones pair"),
        Link("Mobile network", android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS, "mobile data sim cellular roaming"),
        Link("Airplane mode", android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS, "airplane aeroplane flight"),
        Link("Battery", android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS, "battery saver power"),
        Link("Display", android.provider.Settings.ACTION_DISPLAY_SETTINGS, "display brightness screen dark"),
        Link("Sound", android.provider.Settings.ACTION_SOUND_SETTINGS, "sound volume ringtone vibrate"),
        Link("Notifications", "android.settings.NOTIFICATION_SETTINGS", "notifications alerts"),
        Link("Do Not Disturb", "android.settings.ZEN_MODE_SETTINGS", "do not disturb dnd focus quiet"),
        Link("Apps", android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, "apps applications uninstall"),
        Link("Storage", android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "storage space memory"),
        Link("Location", android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS, "location gps"),
        Link("Security", android.provider.Settings.ACTION_SECURITY_SETTINGS, "security lock screen fingerprint face"),
        Link("Privacy", android.provider.Settings.ACTION_PRIVACY_SETTINGS, "privacy permissions"),
        Link("Accessibility", android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS, "accessibility talkback"),
        Link("Date and time", android.provider.Settings.ACTION_DATE_SETTINGS, "date time clock"),
        Link("Languages", android.provider.Settings.ACTION_LOCALE_SETTINGS, "language languages keyboard"),
        Link("Hotspot", "android.settings.TETHER_SETTINGS", "hotspot tethering"),
        Link("NFC", android.provider.Settings.ACTION_NFC_SETTINGS, "nfc tap pay contactless"),
        Link("About phone", android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS, "about phone version model"),
        Link("Zenith settings", ZENITH, "zenith launcher home screen settings theme wallpaper"),
    )

    /** The settings whose name, or another word for them, starts with what was typed. */
    fun find(text: String): List<Link> {
        val typed = text.trim().lowercase()
        if (typed.length < 2) return emptyList()
        return all.filter { link ->
            val words = (link.title.lowercase().split(' ', '-') + link.words.split(' '))
            words.any { it.startsWith(typed) } || link.title.lowercase().startsWith(typed)
        }.take(4)
    }
}
