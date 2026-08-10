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
import com.cafarovceyxun.anamuslim.resources.strLabelBabPrefix
import com.cafarovceyxun.anamuslim.resources.strLabelBookPrefix
import com.cafarovceyxun.anamuslim.resources.strLabelSubBabPrefix
import com.cafarovceyxun.anamuslim.resources.strLabelHadithNo
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerial
import com.cafarovceyxun.anamuslim.resources.strLabelVolumePrefix
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
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.search.SearchResult
import com.cafarovceyxun.anamuslim.search.SearchResultMatch
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import com.cafarovceyxun.anamuslim.viewModels.QuranSearchViewModel

import androidx.navigation.NavController

@Composable
fun TextSearchResults(
    viewModel: QuranSearchViewModel,
    results: LazyPagingItems<SearchResult>,
    hasFilters: Boolean,
    navController: NavController
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
                    val volume = result.volume?.slug ?: result.book?.volume_slug ?: "null"
                    val book = result.book?.slug ?: result.chapter?.book_slug ?: "null"
                    val chapter = result.chapter?.slug
                        ?: result.subChapter?.chapter_slug
                        ?: result.hadith?.chapter_slug
                        ?: "null"
                    val sub = result.subChapter?.slug ?: result.hadith?.sub_chapter_slug ?: "null"
                    // Most specific level first: a title match carries its whole ancestor chain, so
                    // checking volume first would label every sub-bab hit with its volume's name.
                    val title = result.hadith?.text_az?.take(30)
                        ?: result.subChapter?.name
                        ?: result.chapter?.name
                        ?: result.book?.name
                        ?: result.volume?.name
                        ?: ""

                    val route = "hadith_items/$volume/$book/$chapter/$sub/$title"
                    navController.navigate(route)
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
                            .toSet()
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = shapes.small,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.75f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ordered most specific first. Title matches now carry their ancestors so the card can
            // show the breadcrumb below, which means a sub-bab hit also has a volume, book and bab
            // set — checking volume first would relabel every one of them as a volume match.
            val title = when {
                result.hadith != null -> stringResource(Res.string.strLabelHadithNo, result.hadith.hadith_no)
                result.subChapter != null -> stringResource(Res.string.strLabelSubBabPrefix, result.subChapter.name)
                result.chapter != null -> stringResource(Res.string.strLabelBabPrefix, result.chapter.name)
                result.book != null -> stringResource(Res.string.strLabelBookPrefix, result.book.name)
                result.volume != null -> stringResource(Res.string.strLabelVolumePrefix, result.volume.name)
                else -> stringResource(Res.string.hadith)
            }

            Text(
                text = title,
                style = typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )

            result.matches.filterIsInstance<SearchResultMatch.HadithMatch>().forEachIndexed { index, match ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = match.source,
                        style = typography.labelSmall,
                        color = colorScheme.secondary.alpha(0.7f)
                    )

                    if (match.isArabic) {
                        // `textDirection` alone only orders the glyphs; the paragraph still sat where
                        // an LTR box put it, so lines began short of the right edge instead of flush
                        // against it. Filling the width and aligning right is what starts the text
                        // where an Arabic reader begins.
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

            run {
                // Breadcrumb of everything *above* the level that matched, so a bab hit reads
                // "Volume › Book" rather than repeating its own name.
                val contextText = when {
                    result.hadith != null || result.subChapter != null -> listOfNotNull(
                        result.volume?.name,
                        result.book?.name,
                        result.chapter?.name,
                    )

                    result.chapter != null -> listOfNotNull(result.volume?.name, result.book?.name)
                    result.book != null -> listOfNotNull(result.volume?.name)
                    else -> emptyList()
                }.joinToString(" › ")

                if (contextText.isNotEmpty()) {
                    Text(
                        text = contextText,
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.alpha(0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
