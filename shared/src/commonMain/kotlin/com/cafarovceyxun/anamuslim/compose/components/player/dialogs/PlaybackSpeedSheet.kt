package com.cafarovceyxun.anamuslim.compose.components.player.dialogs

import com.cafarovceyxun.anamuslim.resources.icon_playback_speed
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.playbackSpeed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatOneDecimal
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import kotlinx.coroutines.launch

@Composable
fun PlaybackSpeedSheet(
    controller: RecitationPlayer,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val appLocale = LocalAppLocale.current
    val selectedSpeed = RecitationPreferences.observeSpeed()
    val coroutineScope = rememberCoroutineScope()
    val speedOptions = listOf(0.1f, 0.3f, 0.5f, 0.7f, 1f, 1.3f, 1.5f, 1.7f, 2f, 3f)

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.icon_playback_speed,
        title = stringResource(Res.string.playbackSpeed),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp),
        ) {
            items(
                count = speedOptions.size,
                key = { speedOptions[it] },
            ) { index ->
                val speed = speedOptions[index]

                RadioItem(
                    titleStr = appLocale.formatOneDecimal(speed) + "x",
                    selected = speed == selectedSpeed,
                    onClick = {
                        if (speed == selectedSpeed) return@RadioItem

                        coroutineScope.launch {
                            RecitationPreferences.setSpeed(speed)
                            controller.setSpeed(speed)
                        }
                    },
                )
            }
        }
    }
}
