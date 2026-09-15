package com.cafarovceyxun.anamuslim.compose.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.noResults
import com.cafarovceyxun.anamuslim.resources.hadithSearchNameMatch
import com.cafarovceyxun.anamuslim.resources.strLabelBab
import com.cafarovceyxun.anamuslim.resources.strLabelBook
import com.cafarovceyxun.anamuslim.resources.strLabelSubBab
import com.cafarovceyxun.anamuslim.resources.strLabelHadithNo
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerial
import com.cafarovceyxun.anamuslim.resources.strLabelVolume
import com.cafarovceyxun.anamuslim.resources.strMsgSearchNoResultsFoundAbsolute
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.theme.appFontFamily
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReference
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceData
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithShareSheet
import com.cafarovceyxun.anamuslim.repository.loadHadithLocation
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithLocation
import com.cafarovceyxun.anamuslim.compose.screens.hadith.hadithDisplayName
import com.cafarovceyxun.anamuslim.compose.screens.hadith.hadithTitleTextNow
import com.cafarovceyxun.anamuslim.compose.screens.hadith.isArabicAppLanguage
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.search.SearchResult
import com.cafarovceyxun.anamuslim.search.SearchResultMatch
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import com.cafarovceyxun.anamuslim.viewModels.QuranSearchViewModel

/**
 * @param onOpenHadith opens the hadith list for one slug path. No default on purpose: the string
 *   route this used to navigate to itself (`hadith_items/...`) exists only in Android's `MainScreen`
 *   graph, so on iOS every hadith result crashed the app with "destination cannot be found" — and in
 *   `ActivitySearch`, whose NavController has no graph at all, it threw there too. Each host now
 *   spells out its own hop, and a missing one is a compile error rather than a crash.
 *
 *   `hadithId` və `query` babın içindəki hədəfi daşıyır: oxucu həmin hədisə enir və sorğunun
 *   sözlərini sarı ilə işarələyir. Başlıq/bab səviyyəsindəki nəticədə `hadithId` null olur — orada
 *   konkret hədis yoxdur, bab öz başından açılır.
 */
@Composable
fun TextSearchResults(
    viewModel: QuranSearchViewModel,
    results: LazyPagingItems<SearchResult>,
    hasFilters: Boolean,
    onOpenHadith: (
        volumeSlug: String?,
        bookSlug: String?,
        chapterSlug: String?,
        subChapterSlug: String?,
        title: String,
        hadithId: Long?,
        query: String,
    ) -> Unit,
) {
    if (results.loadState.refresh is LoadState.Loading) {
        return Loader(true)
    }

    if (results.itemCount == 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(
                    if (hasFilters)
                        Res.string.strMsgSearchNoResultsFoundAbsolute
                    else
                        Res.string.noResults
                ),
                style = typography.labelLarge,
            )
        }

        return
    }

    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }

    // Hədis nəticəsinin vərəqi və oradan açılan paylaşma. Paylaşma vərəqi də `ModalBottomSheet`
    // olduğu üçün ikisi üst-üstə yığılmır: oxucudakı qayda ilə (`HadithOptionsSheet` →
    // `HadithShareSheet`) birinci bağlanır, ikinci açılır.
    var hadithRefData by remember { mutableStateOf<HadithQuickReferenceData?>(null) }
    var sharingHadith by remember { mutableStateOf<Hadith?>(null) }

    // «Əlavə qaynaq» sətri üçün hədisin ağacdakı yeri. Nəticə sətri yalnız cild/kitab/bab daşıyır,
    // alt bab isə yalnız slug kimi hədisin içindədir — ona görə tam zəncir bazadan oxunur.
    var shareLocation by remember { mutableStateOf(HadithLocation()) }
    LaunchedEffect(sharingHadith) {
        val hadith = sharingHadith
        shareLocation = if (hadith == null) HadithLocation() else loadHadithLocation(hadith)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // The floating bottom nav overlays this list, so the list runs to the window edge and only
        // reserves scroll room under the bar. A fixed 120dp guessed at that height and left a dead
        // band on short/landscape windows; `mainBottomNavigationOuterHeight` is the real metric.
        contentPadding = PaddingValues(
            start = 12.dp + readableWidthInset(),
            end = 12.dp + readableWidthInset(),
            top = 16.dp,
            bottom = mainBottomNavigationOuterHeight() + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            results.itemCount,
            key = {
                val result = results[it]
                if (result != null) {
                    // Most specific level first, matching the card: every title match now carries its
                    // ancestors, so a volume-first check would key half the page off the same volume.
                    val baseKey = when {
                        result.hadith != null -> "h_${result.hadith.id}"
                        result.subChapter != null -> "s_${result.subChapter.slug}"
                        result.chapter != null -> "c_${result.chapter.slug}"
                        result.book != null -> "b_${result.book.slug}"
                        result.volume != null -> "v_${result.volume.slug}"
                        else -> "q_${result.chapterNo}:${result.verseNo}"
                    }
                    "$baseKey-$it"
                } else {
                    "loading-$it"
                }
            }
        ) {
            val result = results[it] ?: return@items

            if (result.hadith != null || result.volume != null || result.book != null || result.chapter != null || result.subChapter != null) {
                HadithSearchResultCard(result) {
                    val hadith = result.hadith
                    if (hadith != null) {
                        // Mətn uyğunluğu **vərəq açır**, oxucuya atmır — ayə nəticəsindəki jestin
                        // eynisi. Kartda mətnin yalnız bir parçası görünür, «bu, axtardığım
                        // hədisdirmi?» sualı isə tam mətni istəyir.
                        viewModel.recordCurrentSearchQuery()
                        hadithRefData = HadithQuickReferenceData(
                            hadith = hadith,
                            query = viewModel.searchQuery.value,
                            volume = result.volume,
                            book = result.book,
                            chapter = result.chapter,
                        )
                    } else {
                        // Başlıq uyğunluğunda göstəriləcək mətn yoxdur — həmin səviyyə birbaşa açılır.
                        // Səviyyə adları indeksdəki kimi titullanır: ərəbcə interfeysdə oxucunun bar
                        // başlığı da ərəbcə adla açılsın.
                        val title = result.subChapter?.let { hadithTitleTextNow(it.name, it.name_ar) }
                            ?: result.chapter?.let { hadithTitleTextNow(it.name, it.name_ar) }
                            ?: result.book?.let { hadithTitleTextNow(it.name, it.name_ar) }
                            ?: result.volume?.let { hadithTitleTextNow(it.name, it.name_ar) }
                            ?: ""

                        onOpenHadith(
                            result.volume?.slug ?: result.book?.volume_slug,
                            result.book?.slug ?: result.chapter?.book_slug,
                            result.chapter?.slug ?: result.subChapter?.chapter_slug,
                            result.subChapter?.slug,
                            title,
                            null,
                            viewModel.searchQuery.value,
                        )
                    }
                }
            } else {
                TextSearchResultCard(result) {
                    viewModel.recordCurrentSearchQuery()

                    quickRefData = QuickReferenceData(
                        chapterNo = result.chapterNo ?: 0,
                        verses = result.verseNo.toString(),
                        slugs = result.matches
                            .filterIsInstance<SearchResultMatch.TranslationMatch>()
                            .map { it.slug }
                            .toSet(),
                        // Sorğu vərəqə də gedir ki, tapılan söz orada sarı ilə işarələnsin.
                        query = viewModel.searchQuery.value,
                    )
                }
            }
        }
    }

    QuickReference(
        data = quickRefData,
        onOpenInReader = { chapterNo, range ->
            quickRefData = null
            ReaderUiHooks.openVerseRange?.invoke(chapterNo, range.first, range.last)
        },
        onClose = { quickRefData = null },
    )

    HadithQuickReference(
        data = hadithRefData,
        onOpen = { data ->
            hadithRefData = null
            val hadith = data.hadith
            val title = data.chapter?.let { hadithTitleTextNow(it.name, it.name_ar) }
                ?: data.book?.let { hadithTitleTextNow(it.name, it.name_ar) }
                ?: ""

            onOpenHadith(
                data.volume?.slug ?: data.book?.volume_slug,
                data.book?.slug ?: data.chapter?.book_slug,
                data.chapter?.slug ?: hadith.chapter_slug,
                hadith.sub_chapter_slug,
                title,
                hadith.id,
                data.query,
            )
        },
        onShare = { hadith ->
            hadithRefData = null
            sharingHadith = hadith
        },
        onClose = { hadithRefData = null },
    )

    HadithShareSheet(
        hadith = sharingHadith,
        location = shareLocation,
        onDismiss = { sharingHadith = null },
    )
}

@Composable
private fun TextSearchResultCard(result: SearchResult, onClick: (SearchResult) -> Unit) {

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = shapes.small,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.75f)),
        onClick = {
            onClick(result)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        Res.string.strLabelVerseSerial,
                        result.chapterNo ?: 0,
                        result.verseNo ?: 0
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(colorScheme.background)
                        .clickable(
                            onClick = {
                                PlatformUtils.copyToClipboard("${result.chapterNo}:${result.verseNo}")
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = colorScheme.onBackground,
                    style = typography.labelLarge,
                )
            }

            result.matches.forEachIndexed { index, match ->
                if (index > 0) {
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                when (match) {
                    is SearchResultMatch.TranslationMatch -> {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides if (StringUtils.isRtlLanguage(
                                    match.slug
                                )
                            ) LayoutDirection.Rtl else LayoutDirection.Ltr
                        ) {
                            val fontFamily = appFontFamily
                            val baseFontSize = typography.bodyMedium.fontSize

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = match.displayName,
                                    style = typography.labelMedium,
                                    color = colorScheme.primary,
                                    fontFamily = fontFamily
                                )

                                Text(
                                    text = match.preview,
                                    style = typography.bodyMedium,
                                    color = colorScheme.onSurface,
                                    fontFamily = fontFamily,
                                    lineHeight = baseFontSize * 1.5,
                                )
                            }
                        }
                    }

                    is SearchResultMatch.QuranTextMatch -> {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = match.preview,
                                style = typography.bodyLarge.copy(
                                    textDirection = TextDirection.Rtl,
                                    fontFamily = arabicFontFamily()
                                ),
                                color = colorScheme.onSurface,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun HadithSearchResultCard(result: SearchResult, onClick: () -> Unit) {
    // Ad hədis indeksindəki qayda ilə seçilir: ərəbcə interfeysdə səviyyənin öz ərəbcə adı gəlir,
    // ərəbcə adı olmayan səviyyə isə azərbaycanca adında qalır.
    val arabicUi = isArabicAppLanguage()
    fun levelName(name: String, nameAr: String?) = hadithDisplayName(name, nameAr, arabicUi).text

    // Ən dəqiq səviyyə əvvəl: başlıq uyğunluğu bütün əcdadlarını daşıyır, ona görə cildi əvvəl
    // yoxlasaq hər alt-bab uyğunluğu «cild» kimi etiketlənərdi.
    val isTextMatch = result.hadith != null
    val levelLabel = when {
        result.hadith != null -> null
        result.subChapter != null -> stringResource(Res.string.strLabelSubBab)
        result.chapter != null -> stringResource(Res.string.strLabelBab)
        result.book != null -> stringResource(Res.string.strLabelBook)
        result.volume != null -> stringResource(Res.string.strLabelVolume)
        else -> null
    }

    // Nişan sualı bir baxışda bağlayır: söz hədisin MƏTNİNDƏ tapılıb, yoxsa bir başlıqda.
    // Əvvəl ikisi eyni görünürdü (qalın yaşıl sətir + altında həmin mətnin təkrarı), ona görə
    // siyahıda «bu nədir?» sualı hər karta ayrıca baxmağı tələb edirdi.
    val badgeText = when {
        result.hadith != null ->
            stringResource(Res.string.strLabelHadithNo, result.hadith.hadith_no)
        levelLabel != null -> stringResource(Res.string.hadithSearchNameMatch, levelLabel)
        else -> stringResource(Res.string.hadith)
    }

    // Uyğunluğun ÖZÜ olan parçalar: `highlightMatches` yalnız sorğu tapılanda üslub qoyur, ona görə
    // vurğusuz önizləmə «bu blokda söz yoxdur» deməkdir. Azərbaycanca sorğuda hədisin ərəbcə bloku
    // belə süzülür — kart iki dəfə qısalır. Heç biri vurğulu deyilsə (normallaşdırma fərqi)
    // birincisi qalır, yoxsa kart tamam boş görünərdi.
    val hadithMatches = result.matches.filterIsInstance<SearchResultMatch.HadithMatch>()
    val shownMatches = remember(hadithMatches) {
        hadithMatches.filter { it.preview.spanStyles.isNotEmpty() }
            .ifEmpty { hadithMatches.take(1) }
    }

    // Uyğunluğun ÜSTÜNDƏKİ səviyyələr: bab uyğunluğu «Cild › Kitab» oxunur, öz adını təkrarlamır.
    val breadcrumb = when {
        result.hadith != null || result.subChapter != null -> listOfNotNull(
            result.volume?.let { levelName(it.name, it.name_ar) },
            result.book?.let { levelName(it.name, it.name_ar) },
            result.chapter?.let { levelName(it.name, it.name_ar) },
        )

        result.chapter != null -> listOfNotNull(
            result.volume?.let { levelName(it.name, it.name_ar) },
            result.book?.let { levelName(it.name, it.name_ar) },
        )

        result.book != null -> listOfNotNull(result.volume?.let { levelName(it.name, it.name_ar) })
        else -> emptyList()
    }.joinToString(" › ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = shapes.small,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.75f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mətn uyğunluğu dolu nişandır, başlıq uyğunluğu solğun/konturlu: rəng fərqi
                // siyahını sürüşdürərkən oxumadan da işləyir.
                Surface(
                    color = if (isTextMatch) colorScheme.primaryContainer
                    else colorScheme.surfaceVariant.alpha(0.6f),
                    contentColor = if (isTextMatch) colorScheme.onPrimaryContainer
                    else colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isTextMatch) null
                    else BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.8f)),
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }

                if (breadcrumb.isNotEmpty()) {
                    Text(
                        text = breadcrumb,
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.alpha(0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            shownMatches.forEach { match ->
                if (match.isArabic) {
                    // `textDirection` tək başına yalnız hərflərin sırasını düzəldir; abzas yenə LTR
                    // qutusunda dayanır və sətirlər sağ kənara çatmır. Eni doldurub sağa
                    // düzləndirmək mətni ərəb oxucusunun başladığı yerdən başladır.
                    Text(
                        text = match.preview,
                        modifier = Modifier.fillMaxWidth(),
                        style = typography.bodyLarge.copy(
                            fontFamily = arabicFontFamily(),
                            textDirection = TextDirection.Rtl,
                        ),
                        textAlign = TextAlign.Right,
                        color = colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = match.preview,
                        style = typography.bodyMedium,
                        color = colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
