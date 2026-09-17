package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Tam ekran səth — məzmunu öz pəncərəsində göstərir.
 *
 * `Dialog(usePlatformDefaultWidth = false)` `PrayerShareEditorScreen`-dəki qurğudur: geri jesti
 * pəncərəni bağlayır, məzmun isə ekranı bütöv tutur. Inline emit ediləndə çağıran ekranın sürüşən
 * sütununun içində qalardı (bax CLAUDE.md → «Tam ekran səth `Dialog` olmalıdır, inline yox»).
 *
 * `AppDestination`-da route-u olmayan ekranlar — «Dua və zikr», «Əsmaül Hüsnə» — hər iki
 * platformada buradan açılır, ona görə forma ortaq fayldadır.
 */
@Composable
fun FullScreenSurface(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            content = content,
        )
    }
}
