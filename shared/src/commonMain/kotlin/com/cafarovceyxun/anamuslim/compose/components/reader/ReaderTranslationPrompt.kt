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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.components.transls.TranslModel
import com.cafarovceyxun.anamuslim.components.transls.TranslationGroupModel
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.dialogs.MessageDialog
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.isBusy
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationReviewRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptDownload
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptDownloading
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptMsg
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptSelect
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptSelectMsg
import com.cafarovceyxun.anamuslim.resources.readerTranslPromptSelectTitle
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/*
 * Quran ekranlarında tərcümə xəbərdarlığı.
 *
 * Tərcüməsiz istifadəçi oxucunu yalnız ərəbcə görür və səbəbini bilmir. Kart iki halı ayırır:
 *  1. Cihazda tərcümə **var**, amma heç biri seçilməyib → «Göstər» bir toxunuşla onu seçir.
 *  2. Cihazda tərcümə **yoxdur** (heç vaxt yüklənməyib və ya silinib) → «Yüklə» məsləhət görülən
 *     kitabı yükləyir və seçir.
 *
 * Harada durur: Quran indeksinin başında və oxucunun bütün tərcümə göstərən rejimlərində (ayə-ayə,
 * kitab, tərcümə) məzmunun **üstündə** — siyahının ilk elementində deyil, çünki surə ortadan
 * açılanda (davam et, əlfəcin, tarixçə) ilk element ekrandan kənarda qalırdı.
 *
 * Göstərilmir: interfeys ərəbcədirsə, istifadəçi «×» basıbsa (həmişəlik) və ya «Sonra» deyibsə
 * ([SNOOZE_MILLIS] ərzində). Tərcüməni silmək/seçimi boşaltmaq bu bayraqları sıfırlayır
 * ([TranslationViewModel]) — istifadəçi tərcüməni bilərəkdən götürəndə xəbərdarlıq yenə çıxır.
 */

/** «Sonra» kartı bu qədər gizlədir. */
private const val SNOOZE_MILLIS = 3L * 24 * 60 * 60 * 1000

/** Yoxlamanın nəticəsi; `null` = hələ bilinmir (kart bir anlıq görünüb yox olmasın). */
private data class TranslationAvailability(
    val hasReadable: Boolean,
    /** Seçilməmiş, amma cihazda olan ən uyğun kitab (yoxdursa `null`). */
    val downloadedUnselected: TranslationBookInfoModel?,
)

private val NoRefreshTick = MutableStateFlow(0)

@Composable
internal fun ReaderTranslationPrompt(
    readerVm: ReaderViewModel?,
    modifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
) {
    val language = LocalAppLocale.current.language
    if (language == "ar") return
    if (ReaderPreferences.observeTranslationPromptDismissed()) return

    val snoozeUntil = ReaderPreferences.observeTranslationPromptSnoozeUntil()
    if (snoozeUntil > currentEpochMillis()) return

    val selectedSlugs by ReaderPreferences.translationsFlow().collectAsState(initial = null)
    val refreshTick by (readerVm?.refreshTrigger ?: NoRefreshTick).collectAsState()

    // Ayarlardan (Android-də ayrıca Activity) qayıdanda seçim dəyişməmiş ola bilər, kitab isə
    // silinib — yoxlama hər qayıdışda təkrarlanır.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }

    val availability by produceState<TranslationAvailability?>(
        null, selectedSlugs, refreshTick, resumeTick, language,
    ) {
        val slugs = selectedSlugs ?: return@produceState
        value = withContext(Dispatchers.IO) {
            QuranTranslationFactory().use { factory ->
                val hasReadable = slugs.any { factory.isTranslationDownloaded(it) }
                val downloaded = if (hasReadable) null else {
                    pickDownloadedBook(factory.getDownloadedTranslationBooksInfo(), language)
                }
                TranslationAvailability(hasReadable, downloaded)
            }
        }
    }

    // Yükləmə/seçim bitəndə yoxlama kartdan **əvvəl** `true` olur — kart kompozisiyadan çıxır və öz
    // yeniləmə effektinə çatmır. Ona görə «tərcümə yox idi → indi var» keçidini burada tuturuq.
    var sawMissingTranslation by remember { mutableStateOf(false) }
    val hasReadable = availability?.hasReadable
    LaunchedEffect(hasReadable) {
        when (hasReadable) {
            false -> sawMissingTranslation = true
            true -> if (sawMissingTranslation) {
                sawMissingTranslation = false
                readerVm?.refreshContent()
            }
            null -> Unit
        }
    }

    val state = availability ?: return
    if (state.hasReadable) return

    val downloadedBook = state.downloadedUnselected
    if (downloadedBook != null) {
        ReaderTranslationSelectCard(downloadedBook, modifier)
    } else {
        ReaderTranslationDownloadCard(readerVm, modifier)
    }
}

/** Cihazdakı kitablardan ən uyğunu: tətbiq dilindəki, sonra `az`, sonra ilki. Yoxlanış kitabı deyil. */
private fun pickDownloadedBook(
    books: Map<String, TranslationBookInfoModel>,
    language: String,
): TranslationBookInfoModel? {
    val readable = books.values.filter { it.slug != TranslationReviewRepository.BOOK_SLUG }
    return readable.firstOrNull { it.slug == language }
        ?: readable.firstOrNull { it.langCode == language }
        ?: readable.firstOrNull { it.slug == "az" }
        ?: readable.firstOrNull()
}

/**
 * Yüklənəcək kitab: tətbiq dilindəki, sonra `az`, sonra ilki. «Yoxlanış təklifləri» oxu tərcüməsi
 * deyil — burada heç vaxt təklif olunmur.
 */
private fun recommendedDownload(
    groups: List<TranslationGroupModel>,
    language: String,
): TranslModel? {
    val all = groups.flatMap { it.translations }
        .filter { it.bookInfo.slug != TranslationReviewRepository.BOOK_SLUG }
    return all.firstOrNull { it.bookInfo.slug == language }
        ?: all.firstOrNull { it.bookInfo.langCode == language }
        ?: all.firstOrNull { it.bookInfo.slug == "az" }
        ?: all.firstOrNull()
}

/** Hal 1: tərcümə cihazdadır, sadəcə seçilməyib. */
@Composable
private fun ReaderTranslationSelectCard(book: TranslationBookInfoModel, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var hiddenNow by rememberSaveable { mutableStateOf(false) }
    if (hiddenNow) return

    PromptSurface(
        modifier = modifier,
        onClose = {
            hiddenNow = true
            scope.launch { ReaderPreferences.setTranslationPromptDismissed(true) }
        },
    ) {
        PromptTexts(
            title = stringResource(Res.string.readerTranslPromptSelectTitle),
            message = stringResource(Res.string.readerTranslPromptSelectMsg),
            bookName = book.bookName,
        )

        PromptActions(
            primaryText = stringResource(Res.string.readerTranslPromptSelect),
            primaryIcon = null,
            onPrimary = {
                // Seçim ayarı dəyişir → [ReaderTranslationPrompt]-in yoxlaması `true` olur və
                // oxucunu özü yeniləyir.
                scope.launch {
                    val current = ReaderPreferences.getTranslations()
                    ReaderPreferences.setTranslations(current + book.slug)
                }
            },
            onLater = {
                hiddenNow = true
                scope.launch {
                    ReaderPreferences.setTranslationPromptSnoozeUntil(currentEpochMillis() + SNOOZE_MILLIS)
                }
            },
        )
    }
}

/** Hal 2: cihazda tərcümə yoxdur. [TranslationViewModel] (kataloq sorğusu) yalnız burada qurulur. */
@Composable
private fun ReaderTranslationDownloadCard(readerVm: ReaderViewModel?, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val language = LocalAppLocale.current.language
    val viewModel = viewModel { TranslationViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    // ViewModel ekranın naviqasiya girişinə bağlıdır və kart yenidən görünəndə köhnə nüsxə qayıdır:
    // onun siyahısında silinmiş kitab hələ «yüklənib» sayılır və kart heç görünmürdü. İlk qurulmada
    // `init` onsuz da yükləyir; yalnız hazır (köhnə) vəziyyəti təzələyirik.
    LaunchedEffect(viewModel) {
        if (!viewModel.uiState.value.isLoading) viewModel.onEvent(TranslationEvent.RefreshQuiet)
    }

    var hiddenNow by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<TranslationUiEvent.ShowMessage?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is TranslationUiEvent.ShowMessage && event.isError) errorMessage = event
        }
    }

    val recommended = recommendedDownload(uiState.translationGroups, language)
    val slug = recommended?.bookInfo?.slug
    val status = slug?.let { uiState.downloadStates[it] } ?: ResourceDownloadStatus.Idle

    // Yüklənən kitabın slug-ı çox vaxt artıq seçilmiş olur (default `az`), yəni tərcümə ayarı
    // dəyişmir və oxucu özü yenilənmir — ona görə bitəndə məzmunu açıq-aydın yenidən qururuq.
    LaunchedEffect(recommended?.isDownloaded) {
        if (recommended?.isDownloaded == true) readerVm?.refreshContent()
    }

    MessageDialog(
        title = errorMessage?.title,
        message = errorMessage?.message,
        onClose = { errorMessage = null },
    )

    if (hiddenNow || recommended == null || recommended.isDownloaded) return

    val isBusy = status.isBusy()

    PromptSurface(
        modifier = modifier,
        closeDescription = if (isBusy) Res.string.strLabelCancel else Res.string.strLabelClose,
        onClose = {
            if (isBusy) {
                viewModel.onEvent(TranslationEvent.CancelDownload(slug!!))
            } else {
                hiddenNow = true
                scope.launch { ReaderPreferences.setTranslationPromptDismissed(true) }
            }
        },
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
            PromptTexts(
                title = stringResource(Res.string.readerTranslPromptTitle),
                message = stringResource(Res.string.readerTranslPromptMsg),
                bookName = recommended.bookInfo.bookName,
            )

            PromptActions(
                primaryText = stringResource(Res.string.readerTranslPromptDownload),
                primaryIcon = Res.drawable.dr_icon_download,
                onPrimary = { viewModel.onEvent(TranslationEvent.DownloadTranslation(slug!!)) },
                onLater = {
                    hiddenNow = true
                    scope.launch {
                        ReaderPreferences.setTranslationPromptSnoozeUntil(currentEpochMillis() + SNOOZE_MILLIS)
                    }
                },
            )
        }
    }
}

@Composable
private fun PromptSurface(
    modifier: Modifier,
    closeDescription: org.jetbrains.compose.resources.StringResource = Res.string.strLabelClose,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
                content()
            }

            Box {
                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_close),
                    contentDescription = stringResource(closeDescription),
                    onClick = onClose,
                    tint = colorScheme.onSurfaceVariant,
                    small = true,
                )
            }
        }
    }
}

@Composable
private fun PromptTexts(title: String, message: String, bookName: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = colorScheme.onSurface,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = colorScheme.onSurfaceVariant,
    )
    if (bookName.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = bookName,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PromptActions(
    primaryText: String,
    primaryIcon: org.jetbrains.compose.resources.DrawableResource?,
    onPrimary: () -> Unit,
    onLater: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = onPrimary,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            if (primaryIcon != null) {
                Icon(
                    painter = painterResource(primaryIcon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
            }
            Text(
                text = primaryText,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        TextButton(onClick = onLater) {
            Text(
                text = stringResource(Res.string.strLabelLater),
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}
