package com.cafarovceyxun.anamuslim.compose.components.player.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AlertCard
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.whenChapterEndsDesc
import com.cafarovceyxun.anamuslim.resources.whenChapterEnds
import com.cafarovceyxun.anamuslim.resources.playNextSurah
import com.cafarovceyxun.anamuslim.resources.repeatCurrentSurah
import com.cafarovceyxun.anamuslim.resources.stopPlayback
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import kotlinx.coroutines.launch


@Composable
fun AudioEndBehaviourSheet(
    controller: RecitationPlayer,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val selectedAudioOption = RecitationPreferences.observeAudioEndBehaviour()
    val coroutineScope = rememberCoroutineScope()

    val items = listOf(
        Pair(AudioEndBehaviour.STOP_PLAYBACK, Res.string.stopPlayback),
        Pair(AudioEndBehaviour.NEXT_CHAPTER, Res.string.playNextSurah),
        Pair(AudioEndBehaviour.REPEAT_CHAPTER, Res.string.repeatCurrentSurah),
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        title = stringResource(Res.string.whenChapterEnds),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp)
                .verticalScroll(
                    rememberScrollState()
                ),
        ) {
            AlertCard(Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = stringResource(Res.string.whenChapterEndsDesc),
                    style = typography.bodyMedium
                )
            }

            items.forEach { (option, title) ->
                RadioItem(
                    title = title,
                    selected = option == selectedAudioOption,
                    onClick = {
                        coroutineScope.launch {
                            RecitationPreferences.setAudioEndBehaviour(option)
                            controller.setAudioEndBehaviour(option)
                        }

                        onClose()
                    },
                )
            }
        }
    }
}
