package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.utils.app.supportsVolumeKeyNavigation
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_mode_book
import com.cafarovceyxun.anamuslim.resources.pageTurnAnimation
import com.cafarovceyxun.anamuslim.resources.readerPinchZoom
import com.cafarovceyxun.anamuslim.resources.readerPinchZoomDesc
import com.cafarovceyxun.anamuslim.resources.readerSharedSettings
import com.cafarovceyxun.anamuslim.resources.strTitleVolumeKeyNavigation
import com.cafarovceyxun.anamuslim.resources.volumeKeyNavSubtitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Quran və hədis oxucusunun **paylaşdığı** ayarlar — ikisi də [AppPreferences]-ə yazır, ona görə
 * birində dəyişən o birində də dəyişir.
 *
 * Öz qrupu var ki, bu görünsün: əvvəllər eyni açarlar hədis vərəqində «Naviqasiya» başlığı altında
 * dururdu və oradan sürüşmə addımını dəyişən adam Quran oxucusunu da dəyişdiyini bilmirdi.
 */
@Composable
fun ReaderSharedSettingsGroup() {
    val scope = rememberCoroutineScope()

    var showPageTurnSheet by remember { mutableStateOf(false) }

    // Observe-lər qrup lambdasından kənarda: `SettingsGroup`-un content-i @Composable deyil.
    val pinchZoomEnabled = AppPreferences.observeReaderPinchZoomEnabled()
    val keyNavEnabled = AppPreferences.observeVolumeKeyNavigationEnabled()
    val pageTurnLabel = pageTurnAnimationLabel(AppPreferences.observeReaderPageTurnAnimation())

    SettingsGroup(title = stringResource(Res.string.readerSharedSettings)) {
        item {
            SettingsItem(
                title = Res.string.pageTurnAnimation,
                subtitleStr = pageTurnLabel,
                icon = Res.drawable.ic_mode_book,
                flat = true,
            ) { showPageTurnSheet = true }
        }

        item {
            SwitchItem(
                title = Res.string.readerPinchZoom,
                subtitle = Res.string.readerPinchZoomDesc,
                checked = pinchZoomEnabled,
                onCheckedChange = {
                    scope.launch { AppPreferences.setReaderPinchZoomEnabled(it) }
                },
            )
        }

        // Səs düymələri platformanın öz nəzarətindədirsə (iOS) bağlanacaq düymə yoxdur.
        if (supportsVolumeKeyNavigation) {
            item {
                SwitchItem(
                    title = Res.string.strTitleVolumeKeyNavigation,
                    subtitle = Res.string.volumeKeyNavSubtitle,
                    checked = keyNavEnabled,
                    onCheckedChange = {
                        scope.launch { AppPreferences.setVolumeKeyNavigationEnabled(it) }
                    },
                )
            }

            // Addım yalnız düymələr oxucuya veriləndən sonra nəyəsə təsir edir.
            if (keyNavEnabled) {
                item { ScrollStepSlider() }
            }
        }
    }

    PageTurnAnimationSheet(isOpen = showPageTurnSheet) { showPageTurnSheet = false }
}
