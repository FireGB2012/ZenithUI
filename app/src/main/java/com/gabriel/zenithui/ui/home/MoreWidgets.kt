package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.Forecast
import com.gabriel.zenithui.data.WorldClocks
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.theme.LocalPalette
import java.time.Instant

/**
 * What Zenith's widgets read beyond the room they have: your settings, a way to change them, the
 * forecast, how many times the home screen has been come back to (for the photo frame), and a way
 * to ask the home screen for what only it can show — a picker, a sheet — by the widget's kind.
 */
@Stable
class WidgetWorld(
    val settings: ZenithSettings,
    val change: ((ZenithSettings) -> ZenithSettings) -> Unit,
    val forecast: Forecast?,
    val visits: Int,
    val setUp: (String) -> Unit,
    /** Where the Photo frame's pictures are kept. */
    val frameDir: java.io.File?,
)

val LocalWidgetWorld = compositionLocalOf<WidgetWorld?> { null }

/** A city's sun or moon, for lists outside the widget. */
@Composable
internal fun CityIcon(zone: String) = Icon(
    if (WorldClocks.daytime(zone, Instant.now())) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
    contentDescription = null,
    tint = LocalPalette.current.onGlassMuted,
    modifier = Modifier.size(18.dp),
)
