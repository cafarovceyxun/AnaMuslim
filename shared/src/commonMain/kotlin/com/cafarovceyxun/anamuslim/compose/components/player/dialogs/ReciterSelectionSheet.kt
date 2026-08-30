package com.cafarovceyxun.anamuslim.compose.components.player.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.ic_mic
import com.cafarovceyxun.anamuslim.resources.downloadRecitations
import com.cafarovceyxun.anamuslim.resources.strMsgReciterDownloadHint
import com.cafarovceyxun.anamuslim.resources.titleArabicReciters
import com.cafarovceyxun.anamuslim.resources.titleTranslationVoices
import com.cafarovceyxun.anamuslim.resources.strTitleSelectReciter
import com.cafarovceyxun.anamuslim.compose.components.player.LocalPlayerActions
import com.cafarovceyxun.anamuslim.compose.components.player.ReciterPreviewButton
import com.cafarovceyxun.anamuslim.compose.components.player.rememberReciterPreview
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.compose.components.common.AlertCard
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetBare
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.viewModels.ReciterSelectorViewModel
import kotlinx.coroutines.launch

@Composable
fun ReciterSelectorSheet(
    controller: RecitationPlayer,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val playerActions = LocalPlayerActions.current
    val viewModel = viewModel { ReciterSelectorViewModel() }

    val reciterListState = rememberLazyListState()

    BottomSheetBare(
        isOpen = isOpen,
        onDismiss = onClose,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_mic),
                    contentDescription = stringResource(Res.string.strTitleSelectReciter),
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(Res.string.strTitleSelectReciter),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_download)
                ) {
                    playerActions.onOpenRecitationDownloads()
                }

                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_refresh)
                ) {
                    viewModel.invalidateReciters()
                }
            }
        },
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AlertCard(
                modifier = Modifier.padding(16.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.strMsgReciterDownloadHint),
                        style = typography.bodyMedium
                    )

                    Text(
                        stringResource(Res.string.downloadRecitations),
                        modifier = Modifier.clickable {
                            playerActions.onOpenRecitationDownloads()
                        },
                        style = typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(580.dp),
            ) {
                ReciterList(
                    viewModel = viewModel,
                    controller = controller,
                    listState = reciterListState,
                )
            }
        }
    }
}

@Composable
private fun ReciterList(
    viewModel: ReciterSelectorViewModel,
    controller: RecitationPlayer,
    listState: LazyListState,
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedQuranReciter = RecitationPreferences.observeReciterId()
    val selectedTranslationReciter = RecitationPreferences.observeTranslationReciterId()
    val quranReciters by viewModel.quranReciters.collectAsState()
    val translationReciters by viewModel.translationReciters.collectAsState()
    val preview = rememberReciterPreview()

    if (quranReciters == null) {
        return Loader(fill = true)
    }

    val reciters = quranReciters!!
    val translations = translationReciters.orEmpty()

    // Seçim saxlanmayıbsa siyahının default səsi işarəli görünür. Oxunan səs onsuz da odur
    // ([RecitationModelManager.selectById] eyni qaydada seçir) — burada işarə qoymasaq tərcümə
    // bölməsi heç bir sətri seçili olmadan açılırdı, yəni istifadəçi «heç nə seçilməyib» görürdü.
    val effectiveTranslationReciter = selectedTranslationReciter
        ?: translations.firstOrNull { it.isDefault }?.id
        ?: translations.firstOrNull()?.id

    // Tərcümə bölməsi yuxarıdadır, ona görə ərəbcə qariyə sürüşmə onun üstündən keçir. Yalnız
    // istifadəçi **birinci qaridən başqasını** seçəndə sürüşürük: default seçimdə siyahı yuxarıda
    // qalır və azərbaycanca səs ilk görünən sətir olur.
    val translationBlockSize = if (translations.isEmpty()) 0 else translations.size + 1

    LaunchedEffect(quranReciters, translationBlockSize) {
        val index = reciters.indexOfFirst { it.id == selectedQuranReciter }

        if (index > 0) {
            listState.scrollToItem(translationBlockSize + index + 1)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp),
    ) {
        // Ən yuxarıda: tərcümə səsi yeganə azərbaycanca oxunuşdur və axtarılan da odur — ərəb
        // qarilərinin arxasında qalanda tapılmırdı. Yalnız «tərcümə» və «hər ikisi» rejimlərində
        // eşidilir, buna baxmayaraq şərtsiz göstərilir ki, həmin rejimlərin hansı səsi işlədəcəyi
        // əvvəlcədən görünsün.
        if (translations.isNotEmpty()) {
            item(key = "header_translation") {
                SectionLabel(stringResource(Res.string.titleTranslationVoices))
            }

            items(
                translations.size,
                key = { "t_" + translations[it].id },
            ) { index ->
                val reciter = translations[index]

                RadioItem(
                    modifier = Modifier.fillMaxWidth(),
                    // Başlıq dildir, alt sətir tərcüməçi: səs sintetikdir, seçimi isə istifadəçi
                    // «hansı dildə» sualı ilə edir — oxunan mətnin sahibi altda dayanır.
                    titleStr = reciter.langName,
                    subtitleStr = reciter.getReciterName(),
                    selected = reciter.id == effectiveTranslationReciter,
                    onClick = {
                        if (reciter.id != selectedTranslationReciter) {
                            coroutineScope.launch {
                                RecitationPreferences.setTranslationReciterId(reciter.id)
                                controller.setReciter(reciter.id, RecitationAudioKind.TRANSLATION)
                            }
                        }
                    },
                )
            }
        }

        item(key = "header_quran") {
            SectionLabel(stringResource(Res.string.titleArabicReciters))
        }

        items(
            reciters.size,
            key = {
                reciters[it].id
            },
        ) { index ->
            val reciter = reciters[index]

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReciterPreviewButton(
                    preview = preview,
                    reciterId = reciter.id,
                )

                RadioItem(
                    modifier = Modifier.weight(1f),
                    titleStr = reciter.getReciterName(),
                    subtitleStr = reciter.getStyleName(),
                    selected = reciter.id == selectedQuranReciter,
                    onClick = {
                        if (reciter.id != selectedQuranReciter) {
                            coroutineScope.launch {
                                RecitationPreferences.setReciterId(reciter.id)
                                controller.setReciter(reciter.id, RecitationAudioKind.QURAN)
                            }
                        }
                    },
                )
            }
        }

    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = typography.titleSmall,
        color = colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
