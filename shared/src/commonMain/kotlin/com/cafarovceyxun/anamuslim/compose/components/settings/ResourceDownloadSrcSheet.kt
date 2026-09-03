package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AlertCard
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.msgResourceDownloadSource
import com.cafarovceyxun.anamuslim.resources.strLabelGitHubRaw
import com.cafarovceyxun.anamuslim.resources.strLabelJsDelivr
import com.cafarovceyxun.anamuslim.resources.titleResourceDownloadSource
import com.cafarovceyxun.anamuslim.utils.app.DownloadSourceUtils
import com.cafarovceyxun.anamuslim.utils.app.ResourceDownloadProxy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ResourceDownloadSrcSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()

    val currentDownloadSrc = AppPreferences.observeResourceDownloadProxy()

    val items = listOf(
        Pair(ResourceDownloadProxy.GITHUB, stringResource(Res.string.strLabelGitHubRaw)),
        Pair(ResourceDownloadProxy.JSDELIVR, stringResource(Res.string.strLabelJsDelivr)),
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_download,
        title = stringResource(Res.string.titleResourceDownloadSource),
    ) {
        AlertCard(
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                stringResource(Res.string.msgResourceDownloadSource),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            items.forEach { (downloadSrc, title) ->
                RadioItem(
                    titleStr = title,
                    subtitleStr = DownloadSourceUtils.getDownloadSourceName(downloadSrc),
                    selected = currentDownloadSrc == downloadSrc,
                    onClick = {
                        scope.launch {
                            DownloadSourceUtils.setDownloadSource(downloadSrc)
                        }
                        onDismiss()
                    },
                )
            }
        }
    }
}
