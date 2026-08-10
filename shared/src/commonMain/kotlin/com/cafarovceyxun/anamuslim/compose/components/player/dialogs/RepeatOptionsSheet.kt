package com.cafarovceyxun.anamuslim.compose.components.player.dialogs

import com.cafarovceyxun.anamuslim.resources.ic_repeat
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.twice
import com.cafarovceyxun.anamuslim.resources.playbackCount
import com.cafarovceyxun.anamuslim.resources.once
import com.cafarovceyxun.anamuslim.resources.nTimes
import com.cafarovceyxun.anamuslim.resources.msgRepeat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AlertCard
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import kotlinx.coroutines.launch

@Composable
fun RepeatOptionsSheet(
    controller: RecitationPlayer,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val selectedAudioOption = RecitationPreferences.observeAudioOption()
    val repeatSupported = selectedAudioOption == AudioOption.ONLY_QURAN

    val currentRepeatCount = RecitationPreferences.observeRepeatCount()
    val coroutineScope = rememberCoroutineScope()
    val repeatOptions = listOf(0, 1, 2, 4, 9)

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.ic_repeat,
        title = stringResource(Res.string.playbackCount),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 48.dp
            ),
        ) {
            item {
                AlertCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.msgRepeat),
                        style = typography.bodyMedium
                    )
                }
            }
            items(
                count = repeatOptions.size,
                key = { repeatOptions[it] },
            ) { index ->
                val repeatCount = repeatOptions[index]

                RadioItem(
                    titleStr = when (repeatCount) {
                        0 -> stringResource(Res.string.once)
                        1 -> stringResource(Res.string.twice)
                        else -> stringResource(Res.string.nTimes, repeatCount + 1)
                    },
                    enabled = repeatSupported,
                    selected = repeatCount == currentRepeatCount,
                    onClick = {
                        if (repeatCount == currentRepeatCount) return@RadioItem

                        coroutineScope.launch {
                            RecitationPreferences.setRepeatCount(repeatCount)
                            controller.setRepeatCount(repeatCount)
                        }
                    },
                )
            }
        }
    }
}
