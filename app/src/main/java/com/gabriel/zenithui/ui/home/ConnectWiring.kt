package com.gabriel.zenithui.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.AuraShop
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.Looks
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.server.ServerConfig
import com.gabriel.zenithui.data.server.Signed
import com.gabriel.zenithui.ui.connect.ConnectBridge
import com.gabriel.zenithui.ui.settings.SettingsPreview
import org.json.JSONArray

/**
 * Zenith Connect's way into Zenith: what it reads (the settings, the Looks, the account, page one
 * as the previews draw it) and the view model's own functions for what it does. Made again only
 * when one of those changes, so Connect is not drawn again every time the home screen is.
 */
@Composable
internal fun connectBridge(
    viewModel: ZenithViewModel,
    settings: ZenithSettings,
    looks: List<Look>,
    plus: Boolean,
    signed: Signed?,
    worn: String?,
    home: SettingsPreview,
    needPlus: () -> Unit,
): ConnectBridge {
    val askPlus = rememberUpdatedState(needPlus)
    return remember(viewModel, settings, looks, plus, signed, worn, home.items, home.appsByKey, home.wallpaper, home.screenWidth, home.screenHeight) {
        ConnectBridge(
            server = ServerConfig.enabled,
            settings = settings,
            currentSettings = Looks.capture(settings, emptyList(), "", "", 0L).settings,
            currentLayout = { JSONArray().apply { viewModel.layout.value.forEach { put(it.toJson()) } }.toString() },
            home = home,
            looks = looks,
            plus = plus,
            account = signed?.let { it.account.username ?: it.email ?: "Google account" },
            backupAt = signed?.account?.backupAt,
            worn = worn,
            owns = { name -> WallStyle.byName(name)?.let { AuraShop.ownsDesign(settings, plus, it) } ?: true },
            wear = { name, wearing, lookId ->
                viewModel.wearFromConnect(name, wearing.settings, wearing.layout, lookId, wearing.fromElsewhere)
            },
            takeOff = viewModel::takeOffConnect,
            serverUp = viewModel::serverUp,
            featured = viewModel::featuredLooks,
            lookByCode = viewModel::lookByCode,
            saveLook = viewModel::saveLook,
            shareLook = { id -> viewModel.shareLook(id) },
            stopSharing = viewModel::stopSharingLook,
            deleteLook = viewModel::deleteLook,
            signIn = viewModel::signIn,
            createAccount = viewModel::createAccount,
            signOut = viewModel::signOut,
            backUp = viewModel::backUpToAccount,
            restore = viewModel::restoreFromAccount,
            needPlus = { askPlus.value() },
        )
    }
}
