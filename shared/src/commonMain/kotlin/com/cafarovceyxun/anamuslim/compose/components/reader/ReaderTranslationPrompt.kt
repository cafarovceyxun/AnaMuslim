package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.dialogs.MessageDialog
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.isBusy
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.recommendedTranslation
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptDownload
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptDownloading
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptMsg
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptTitle
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelLater
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import com.cafarovceyxun.anamuslim.viewModels.TranslationEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationUiEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/*
 * Oxucuda tərcümə təklifi (ayə-ayə rejimi).
 *
 * Onbordinqdə tərcüməni ötürən istifadəçi oxucunu yalnız ərəbcə görür və səbəbini bilmir: seçilmiş
 * slug (default `az`) var, kitab isə cihazda yoxdur — ayə sadəcə tərcüməsiz çıxır. Kart surənin
 * başında, ilk siyahı elementinin içində durur (ayrıca `item` yox — oxucunun sürüşmə məntiqi
 * indekslərlə işləyir və əlavə element hər hədəfi bir sətir sürüşdürərdi), oxuduqca yuxarı gedir.
 *
 * Göstərilmir: interfeys ərəbcədirsə, seçilmiş tərcümələrdən biri artıq cihazdadırsa, istifadəçi
 * «×» basıbsa (həmişəlik) və ya «Sonra» deyibsə ([SNOOZE_MILLIS] ərzində).
 */

/** «Sonra» kartı bu qədər gizlədir. */
private const val SNOOZE_MILLIS = 3L * 24 * 60 * 60 * 1000

@Composable
internal fun ReaderTranslationPrompt(readerVm: ReaderViewModel) {
    if (LocalAppLocale.current.language == "ar") return
    if (ReaderPreferences.observeTranslationPromptDismissed()) return

    val snoozeUntil = ReaderPreferences.observeTranslationPromptSnoozeUntil()
    if (snoozeUntil > currentEpochMillis()) return

    val selectedSlugs by ReaderPreferences.translationsFlow().collectAsState(initial = null)
    val refreshTick by readerVm.refreshTrigger.collectAsState()

    // null = hələ bilinmir: kart yoxlamadan əvvəl bir anlıq görünüb yox olmasın.
    val hasReadableTranslation by produceState<Boolean?>(null, selectedSlugs, refreshTick) {
        val slugs = selectedSlugs ?: return@produceState
        value = withContext(Dispatchers.IO) {
            QuranTranslationFactory().use { factory ->
                slugs.any { factory.isTranslationDownloaded(it) }
            }
        }
    }
    // Yükləmə bitəndə TranslationViewModel seçimi yenidən yazır və bu yoxlama kartdan **əvvəl**
    // `true` olur — kart kompozisiyadan çıxır və öz yeniləmə effektinə çatmır. Ona görə «tərcümə
    // yox idi → indi var» keçidini burada tuturuq və oxucunu buradan yeniləyirik.
    var sawMissingTranslation by remember { mutableStateOf(false) }
    LaunchedEffect(hasReadableTranslation) {
        when (hasReadableTranslation) {
            false -> sawMissingTranslation = true
            true -> if (sawMissingTranslation) {
                sawMissingTranslation = false
                readerVm.refreshContent()
            }
            null -> Unit
        }
    }

    if (hasReadableTranslation != false) return

    ReaderTranslationPromptCard(readerVm)
}

/** Ayrı composable: [TranslationViewModel] (kataloq sorğusu) yalnız kart həqiqətən lazım olanda qurulur. */
@Composable
private fun ReaderTranslationPromptCard(readerVm: ReaderViewModel) {
    val scope = rememberCoroutineScope()
    val language = LocalAppLocale.current.language
    val viewModel = viewModel { TranslationViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    // «Sonra» bu açılışda dərhal gizlədir; DataStore yazısı növbəti açılışlar üçündür.
    var hiddenNow by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<TranslationUiEvent.ShowMessage?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is TranslationUiEvent.ShowMessage && event.isError) errorMessage = event
        }
    }

    val recommended = recommendedTranslation(uiState.translationGroups, language)
    val slug = recommended?.bookInfo?.slug
    val status = slug?.let { uiState.downloadStates[it] } ?: ResourceDownloadStatus.Idle

    // Yüklənən kitabın slug-ı çox vaxt artıq seçilmiş olur (default `az`), yəni tərcümə ayarı
    // dəyişmir və oxucu özü yenilənmir — ona görə bitəndə məzmunu açıq-aydın yenidən qururuq.
    // (Kart bu anda artıq çıxarılıbsa, eyni işi [ReaderTranslationPrompt] görür.)
    LaunchedEffect(recommended?.isDownloaded) {
        if (recommended?.isDownloaded == true) readerVm.refreshContent()
    }

    MessageDialog(
        title = errorMessage?.title,
        message = errorMessage?.message,
        onClose = { errorMessage = null },
    )

    if (hiddenNow || recommended == null || recommended.isDownloaded) return

    val isBusy = status.isBusy()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_translations),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp),
            ) {
                if (isBusy) {
                    Text(
                        text = if (status is ResourceDownloadStatus.InProgress) {
                            "${stringResource(Res.string.readerTranslPromptDownloading)} ${status.progress}%"
                        } else {
                            stringResource(Res.string.readerTranslPromptDownloading)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (status is ResourceDownloadStatus.InProgress) status.progress / 100f else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                } else {
                    Text(
                        text = stringResource(Res.string.readerTranslPromptTitle),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.readerTranslPromptMsg),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = recommended.bookInfo.bookName,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = { viewModel.onEvent(TranslationEvent.DownloadTranslation(slug!!)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = stringResource(Res.string.readerTranslPromptDownload),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        TextButton(
                            onClick = {
                                hiddenNow = true
                                scope.launch {
                                    ReaderPreferences.setTranslationPromptSnoozeUntil(
                                        currentEpochMillis() + SNOOZE_MILLIS
                                    )
                                }
                            },
                        ) {
                            Text(
                                text = stringResource(Res.string.strLabelLater),
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Box {
                if (isBusy) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelCancel),
                        onClick = { viewModel.onEvent(TranslationEvent.CancelDownload(slug!!)) },
                        tint = colorScheme.onSurfaceVariant,
                        small = true,
                    )
                } else {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelClose),
                        onClick = {
                            hiddenNow = true
                            scope.launch { ReaderPreferences.setTranslationPromptDismissed(true) }
                        },
                        tint = colorScheme.onSurfaceVariant,
                        small = true,
                    )
                }
            }
        }
    }
}
