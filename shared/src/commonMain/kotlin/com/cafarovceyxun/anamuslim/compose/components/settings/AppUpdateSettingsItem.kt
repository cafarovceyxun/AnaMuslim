package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.cafarovceyxun.anamuslim.compose.components.homepage.isIdleOrFailed
import com.cafarovceyxun.anamuslim.compose.components.homepage.rememberSelfUpdateState
import com.cafarovceyxun.anamuslim.compose.components.homepage.selfUpdateButtonLabel
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.strLabelUpdateChecking
import com.cafarovceyxun.anamuslim.resources.strMsgAppUpToDate
import com.cafarovceyxun.anamuslim.resources.strMsgSelfUpdateFailed
import com.cafarovceyxun.anamuslim.resources.strMsgUpdateCheckFailed
import com.cafarovceyxun.anamuslim.utils.app.AppSelfUpdateProvider
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateChecker
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateStatus
import com.cafarovceyxun.anamuslim.utils.app.SelfUpdateState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Settings → "Update": asks the server **now** whether a newer build exists, instead of sending the
 * user to the store blindly. An up-to-date app says so; otherwise the `direct` APK build downloads
 * the update itself and store builds open their listing.
 *
 * The row used to open the store unconditionally, which told the user nothing when there was no
 * update — and on the `direct` build it opened Play, where that install cannot be updated at all.
 *
 * [openStore] is the store hand-off of the host (null where there is none); a self-updating build
 * shows the row even without it.
 */
@Composable
fun AppUpdateSettingsItem(openStore: (() -> Unit)?) {
    val selfUpdate = AppSelfUpdateProvider.isAvailable
    if (!selfUpdate && openStore == null) return

    val selfState = rememberSelfUpdateState()
    // One short request, so the composition's scope is enough: if the screen goes away mid-check,
    // there is nobody left to show the answer to.
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    // Read up front, not with the suspending `getString` after the request (see CLAUDE.md).
    val upToDate = stringResource(Res.string.strMsgAppUpToDate)
    val checkFailed = stringResource(Res.string.strMsgUpdateCheckFailed)
    val checkingLabel = stringResource(Res.string.strLabelUpdateChecking)
    val selfFailed = stringResource(Res.string.strMsgSelfUpdateFailed)
    val progressLabel = selfUpdateButtonLabel(selfState)

    val busy = checking || !selfState.isIdleOrFailed
    val subtitle = when {
        checking -> checkingLabel
        !selfState.isIdleOrFailed -> progressLabel
        selfState == SelfUpdateState.Failed -> selfFailed
        else -> null
    }

    SettingsItem(
        title = Res.string.strLabelUpdate,
        subtitleStr = subtitle,
        // Not `dr_icon_update_app`: that glyph is a white disc with a white arrow on top, drawn for
        // the coloured update dialog. Tinted flat like every other row here it collapses into a
        // solid circle — the same reason `AppUpdateBanner` rebuilds the badge from this arrow.
        icon = Res.drawable.ic_arrow_up,
        flat = true,
    ) {
        if (busy) return@SettingsItem
        checking = true
        scope.launch {
            val release = AppUpdateChecker.checkNow()
            checking = false
            when {
                // No answer: a store build can still let the store decide; the APK build cannot.
                release == null ->
                    if (!selfUpdate && openStore != null) openStore() else PlatformUtils.showToast(checkFailed)

                AppUpdateChecker.statusOf(release) == AppUpdateStatus.NONE -> PlatformUtils.showToast(upToDate)

                selfUpdate -> AppSelfUpdateProvider.updater.start(release.latest_version)

                else -> AppUpdateChecker.actionUrl(release)?.let(PlatformUtils::browseLink) ?: openStore?.invoke()
            }
        }
    }
}
