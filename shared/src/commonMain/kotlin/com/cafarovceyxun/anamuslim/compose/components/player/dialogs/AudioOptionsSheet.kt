package com.cafarovceyxun.anamuslim.compose.components.player.dialogs

import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.titleRecitationGroupSize
import com.cafarovceyxun.anamuslim.resources.msgRecitationGroupSize
import com.cafarovceyxun.anamuslim.resources.audioOption
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AlertCard
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.resources.audioBothArabicTranslation
import com.cafarovceyxun.anamuslim.resources.audioOnlyArabic
import com.cafarovceyxun.anamuslim.resources.audioOnlyTranslation
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import kotlinx.coroutines.launch

import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioOptionsSheet(
    controller: RecitationPlayer,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val selectedAudioOption = RecitationPreferences.observeAudioOption()
    val selectedVerseGroupSize = RecitationPreferences.observeVerseGroupSize()
    val coroutineScope = rememberCoroutineScope()

    val items = listOf(
        Pair(AudioOption.ONLY_QURAN, Res.string.audioOnlyArabic),
        Pair(AudioOption.ONLY_TRANSLATION, Res.string.audioOnlyTranslation),
        Pair(AudioOption.BOTH, Res.string.audioBothArabicTranslation),
    )

    val verseGroupSizes = listOf(1, 2, 3, 5, 10)
    val sizeOptionEnabled = selectedAudioOption == AudioOption.BOTH
    val sizeOptionOpacity = if (sizeOptionEnabled) 1f else 0.5f

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.dr_icon_settings,
        title = stringResource(Res.string.audioOption),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp)
                .verticalScroll(
                    rememberScrollState()
                ),
        ) {
            items.forEach { (option, title) ->
                RadioItem(
                    title = title,
                    selected = option == selectedAudioOption,
                    onClick = {
                        coroutineScope.launch {
                            RecitationPreferences.setAudioOption(option)
                            controller.setAudioOption(option)
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.titleRecitationGroupSize),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .alpha(sizeOptionOpacity),
                color = colorScheme.primary
            )

            AlertCard(
                modifier = Modifier.alpha(sizeOptionOpacity)
            ) {
                Text(
                    text = stringResource(Res.string.msgRecitationGroupSize),
                    style = typography.bodyMedium
                )
            }

            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .alpha(sizeOptionOpacity)
            ) {
                verseGroupSizes.forEach { size ->
                    FilterChip(
                        selected = size == selectedVerseGroupSize,
                        enabled = sizeOptionEnabled,
                        onClick = {
                            if (sizeOptionEnabled) {
                                coroutineScope.launch {
                                    RecitationPreferences.setVerseGroupSize(size)
                                    controller.setVerseGroupSize(size)
                                }
                            }
                        },
                        label = { Text(size.toString()) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = colorScheme.onPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = size == selectedVerseGroupSize,
                            borderColor = colorScheme.outlineVariant,
                        ),
                    )
                }
            }
        }
    }
}
