package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.components.transls.TranslModel
import com.cafarovceyxun.anamuslim.components.transls.TranslationGroupModel
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.dialogs.MessageDialog
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationReviewRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.onboardPackReady
import com.cafarovceyxun.anamuslim.resources.onboardPackTitle
import com.cafarovceyxun.anamuslim.resources.onboardPreviewTranslation
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelRecommended
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strLabelSelectTranslation
import com.cafarovceyxun.anamuslim.resources.strMsgSomethingWrong
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.resources.textDownloading
import com.cafarovceyxun.anamuslim.resources.wbwAudioDownloadAll
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.network.canProceedOnline
import com.cafarovceyxun.anamuslim.viewModels.TranslationEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationUiEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/*
 * Onbordinqin tərcümə addımı.
 *
 * Əvvəl tərcümə «Resurslar» səhifəsindəki uzun siyahının ortasında idi: kiçik yükləmə ikonu, üstəlik
 * təsdiq dialoqu, alt düymə isə həmişə aktiv — yeni istifadəçilər tərcüməsiz keçir və oxucunu yalnız
 * ərəbcə açırdılar. İndi tərcümənin öz səhifəsi var: ayə önizləməsi nə üçün lazım olduğunu göstərir,
 * bir düymə məsləhət görülən hər şeyi yükləyir, kart isə basılan kimi yüklənir (dialoqsuz).
 * Tərcüməsiz keçmək istəyəni [OnboardingScreen] ayrıca soruşur.
 */

private const val BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

internal fun ResourceDownloadStatus.isBusy(): Boolean =
    this is ResourceDownloadStatus.Started || this is ResourceDownloadStatus.InProgress

/**
 * Yeni istifadəçiyə təklif olunan kitablar. «Yoxlanış təklifləri» (təsdiqlənməmiş düzəlişlər) burada
 * göstərilmir — o, tərcümə deyil, yoxlama alətidir və ilk açılışda seçim kimi çaşdırır. Ayarlardakı
 * siyahıda qalır; cihazda artıq varsa burada da görünür ki, seçimi dəyişmək olsun.
 */
internal fun onboardingTranslations(groups: List<TranslationGroupModel>): List<TranslModel> =
    groups.flatMap { it.translations }.filter {
        it.bookInfo.slug != TranslationReviewRepository.BOOK_SLUG || it.isDownloaded
    }

/** Tətbiq dilindəki kitab, yoxdursa `az` (hər kataloqda var), o da yoxdursa siyahının ilki. */
internal fun recommendedTranslation(
    groups: List<TranslationGroupModel>,
    language: String,
): TranslModel? {
    val all = onboardingTranslations(groups)
    return all.firstOrNull { it.bookInfo.langCode == language }
        ?: all.firstOrNull { it.bookInfo.slug == "az" }
        ?: all.firstOrNull()
}

@Composable
fun OnboardingTranslationStepPage(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val language = LocalAppLocale.current.language

    // Eyni instansiyanı [OnboardingScreen] (keçid qapısı) və «Resurslar» səhifəsi də alır.
    val viewModel = viewModel { TranslationViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<TranslationUiEvent.ShowMessage?>(null) }

    LaunchedEffect(Unit) {
        // Uğur kartın özündə görünür; dialoq yalnız alınmayan yükləmə üçündür.
        viewModel.events.collect { event ->
            if (event is TranslationUiEvent.ShowMessage && event.isError) errorMessage = event
        }
    }

    val allTranslations = onboardingTranslations(uiState.translationGroups)
    val recommended = recommendedTranslation(uiState.translationGroups, language)

    if (uiState.isLoading && allTranslations.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorScheme.primary)
        }
        return
    }

    if (uiState.error != null || recommended == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.strMsgSomethingWrong),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { viewModel.onEvent(TranslationEvent.Refresh) }) {
                Text(stringResource(Res.string.strLabelRetry))
            }
        }
        return
    }

    val recommendedSlug = recommended.bookInfo.slug
    val translationStatus = uiState.downloadStates[recommendedSlug] ?: ResourceDownloadStatus.Idle

    val packItems = buildList {
        add(
            PackItem(
                title = recommended.bookInfo.bookName,
                isDone = recommended.isDownloaded,
                status = translationStatus,
            )
        )
        add(
            PackItem(
                title = stringResource(Res.string.strTitleHadith),
                isDone = uiState.isHadithDownloaded && !uiState.hadithSyncStatus.isBusy(),
                status = uiState.hadithSyncStatus,
            )
        )
        // Sözbəsöz paketə girmir — o, növbəti («Resurslar») səhifədə ayrıca təklif kimi durur.
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VersePreviewCard(showTranslation = language != "ar")

        RecommendedPackCard(
            items = packItems,
            onDownloadAll = {
                scope.launch {
                    // Bir yoxlama, bir toast: əks halda hər yükləmə öz xətasını verir.
                    if (!canProceedOnline()) return@launch

                    if (!recommended.isDownloaded && !translationStatus.isBusy()) {
                        viewModel.onEvent(TranslationEvent.DownloadTranslation(recommendedSlug))
                    }
                    if (!uiState.isHadithDownloaded && !uiState.hadithSyncStatus.isBusy()) {
                        viewModel.onEvent(TranslationEvent.SyncHadiths)
                    }
                }
            },
        )

        // Tək kitab olanda siyahı paketdəki sətri təkrarlayardı.
        if (allTranslations.size > 1) {
            ListItemCategoryLabel(title = stringResource(Res.string.strLabelSelectTranslation))

            allTranslations.forEach { transl ->
                val slug = transl.bookInfo.slug
                TranslationCard(
                    translation = transl,
                    isRecommended = slug == recommendedSlug,
                    isSelected = uiState.selectedSlugs.contains(slug),
                    status = uiState.downloadStates[slug] ?: ResourceDownloadStatus.Idle,
                    onDownload = { viewModel.onEvent(TranslationEvent.DownloadTranslation(slug)) },
                    onCancel = { viewModel.onEvent(TranslationEvent.CancelDownload(slug)) },
                    onToggle = { checked ->
                        viewModel.onEvent(TranslationEvent.SelectionChanged(transl, checked))
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    MessageDialog(
        title = errorMessage?.title,
        message = errorMessage?.message,
        onClose = { errorMessage = null },
    )
}

private data class PackItem(
    val title: String,
    val isDone: Boolean,
    val status: ResourceDownloadStatus,
)

/** Tərcümənin nə verdiyini göstərir: ərəbcə ayə, altında mənası. */
@Composable
private fun VersePreviewCard(showTranslation: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Ayə həmişə RTL-dir, interfeys dili nə olursa olsun.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = BISMILLAH,
                    fontSize = 26.sp,
                    lineHeight = 44.sp,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (showTranslation) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.onboardPreviewTranslation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RecommendedPackCard(
    items: List<PackItem>,
    onDownloadAll: () -> Unit,
) {
    val allDone = items.all { it.isDone }
    val anyBusy = items.any { it.status.isBusy() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.5.dp, colorScheme.primary.copy(alpha = 0.55f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.onboardPackTitle),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))

            items.forEach { item -> PackItemRow(item) }

            Spacer(Modifier.height(12.dp))

            if (allDone) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_check_circle),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(Res.string.onboardPackReady),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary,
                    )
                }
            } else {
                Button(
                    onClick = onDownloadAll,
                    // Yükləmə gedərkən düymə söndürülmür: biri alınmasa eyni düymə qalanı yenidən
                    // başladır. Davam edən yükləmələri `onDownloadAll` özü ötürür.
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_download),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(
                            if (anyBusy) Res.string.textDownloading else Res.string.wbwAudioDownloadAll
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PackItemRow(item: PackItem) {
    val status = item.status

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                when {
                    status is ResourceDownloadStatus.Started -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.primary,
                    )

                    item.isDone -> Icon(
                        painter = painterResource(Res.drawable.dr_icon_check_circle),
                        contentDescription = stringResource(Res.string.strLabelDownloaded),
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )

                    else -> Icon(
                        painter = painterResource(Res.drawable.dr_icon_download),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            )

            if (status is ResourceDownloadStatus.InProgress) {
                Text(
                    text = "${status.progress}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                )
            }
        }

        if (status is ResourceDownloadStatus.InProgress) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { status.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp)
                    .height(3.dp),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant,
            )
        }
    }
}

/** Bir kitab: yüklənməyibsə toxunuş yükləyir (dialoqsuz), yüklənibsə seçimi dəyişir. */
@Composable
private fun TranslationCard(
    translation: TranslModel,
    isRecommended: Boolean,
    isSelected: Boolean,
    status: ResourceDownloadStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val isBusy = status.isBusy()
    val isActive = translation.isDownloaded && isSelected

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isBusy) {
                if (translation.isDownloaded) onToggle(!isSelected) else onDownload()
            },
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) {
                colorScheme.primary.copy(alpha = 0.55f)
            } else {
                colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translation.bookInfo.bookName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            status is ResourceDownloadStatus.InProgress ->
                                "${stringResource(Res.string.textDownloading)} ${status.progress}%"

                            isBusy -> stringResource(Res.string.textDownloading)

                            isRecommended -> listOf(
                                stringResource(Res.string.strLabelRecommended),
                                translation.bookInfo.authorName,
                            ).filter { it.isNotBlank() }.joinToString(" · ")

                            else -> translation.bookInfo.authorName
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isBusy) colorScheme.primary else colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                when {
                    isBusy -> IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelCancel),
                        onClick = onCancel,
                        tint = colorScheme.onSurfaceVariant,
                        small = true,
                    )

                    else -> Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        if (translation.isDownloaded) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_check_circle),
                                    contentDescription = stringResource(Res.string.strLabelDownloaded),
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = stringResource(Res.string.labelDownload),
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            if (status is ResourceDownloadStatus.InProgress) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { status.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                        .height(3.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceVariant,
                )
            }
        }
    }
}
