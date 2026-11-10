package com.gabriel.zenithui.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.NewLook
import com.gabriel.zenithui.data.WidgetLooks
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.onboarding.PhonePreview
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.settings.SettingsPreview
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalWidgetLook

/**
 * The one question a phone that had Zenith before the new look is asked, once ([NewLook.due]):
 * switch to it, the main answer, or keep the home screen as it is. It shows page one as switching
 * would leave it, and says what may move before anything does.
 *
 * It needs one of its two answers. Pulled down, it springs back; a tap beside it and Back do
 * nothing. Whichever is chosen, everything the new look sets can be changed again in Customise and
 * in Settings.
 */
@Composable
fun NewLookSheet(
    visible: Boolean,
    settings: ZenithSettings,
    /** Takes the new look: see [NewLook.apply]. */
    onSwitch: () -> Unit,
    /** Keeps the home screen as it is; the question counts as answered all the same. */
    onKeep: () -> Unit,
    /** Page one of the home screen as it is, and what it is drawn with, for the picture. Without it there is none. */
    home: SettingsPreview? = null,
    /** Free placement is on and in effect (it is Plus), so the picture settles the page as switching will. */
    free: Boolean = false,
) {
    val palette = LocalPalette.current
    // Back is no answer either: while the question is up it does nothing.
    BackHandler(enabled = visible) { }
    ZenithSheet(
        visible = visible,
        onDismiss = { },
        title = "Zenith has a new look",
        maxHeightFraction = 0.9f,
        dismissible = false,
    ) {
        // One answer only: while the sheet goes down after one, the other button takes no tap, so
        // a second tap cannot undo the first. Remembered with the sheet's contents, which are put
        // together afresh each time it comes up.
        var answered by remember { mutableStateOf(false) }
        fun answer(choice: () -> Unit) {
            if (answered) return
            answered = true
            choice()
        }
        if (home != null) {
            NewLookPicture(
                settings = settings,
                home = home,
                free = free,
                // Whatever room the words and the two answers leave, up to the height of a phone
                // that is easily read.
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .appearIn(delayMillis = staggerDelay(0), riseFrom = 18f),
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(
            "The iPhone's grid and icons, widgets in colour and a dock of glass.",
            color = palette.onGlass,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .appearIn(delayMillis = staggerDelay(1), riseFrom = 18f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your apps and widgets keep their order. Some may move to another page. Either way, each of these can be changed later in Settings.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .appearIn(delayMillis = staggerDelay(2), riseFrom = 18f),
        )
        Spacer(Modifier.height(18.dp))
        // The answers come up with the sheet itself: a pane of glass moved by a layer of its own
        // would show the wallpaper out of line while it rose.
        PrimaryButton("Switch to the new look", onClick = { answer(onSwitch) })
        Spacer(Modifier.height(10.dp))
        GlassButton(text = "Keep my home screen", onClick = { answer(onKeep) }, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Page one as switching to the new look would leave it: the iPhone grid worked out for this screen,
 * as Zenith works out any grid, and the page settled into it, as switching settles the home screen.
 * Settled on its own, page one comes out as it does with the pages after it, since what has no room
 * on a page only ever moves on to a later one.
 *
 * Its widgets are drawn in the new look's colour. Its icons keep the shape they have: the pictures
 * are cut to a shape once, for the whole home screen, and are only cut again when the look is taken.
 */
@Composable
private fun NewLookPicture(settings: ZenithSettings, home: SettingsPreview, free: Boolean, modifier: Modifier) {
    val configuration = LocalConfiguration.current
    val screenWidth = home.screenWidth.takeIf { it > 0.dp } ?: configuration.screenWidthDp.dp
    val screenHeight = home.screenHeight.takeIf { it > 0.dp } ?: configuration.screenHeightDp.dp
    val (look, page) = remember(settings, home.items, screenWidth, screenHeight, free) {
        val taken = NewLook.apply(settings)
        val (columns, rows) = GridDensity.resolve(taken.gridDensity, taken.gridColumns, taken.gridRows, screenWidth.value, screenHeight.value)
        taken.copy(columns = columns, rows = rows) to LayoutBuilder.settle(home.items, columns, rows, free).filter { it.page == 0 }
    }
    CompositionLocalProvider(LocalWidgetLook provides WidgetLooks.of(look.widgetLook)) {
        PhonePreview(
            items = page,
            dock = look.dock,
            appsByKey = home.appsByKey,
            settings = look,
            wallpaper = home.wallpaper,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            shown = Int.MAX_VALUE,
            modifier = modifier,
        )
    }
}
