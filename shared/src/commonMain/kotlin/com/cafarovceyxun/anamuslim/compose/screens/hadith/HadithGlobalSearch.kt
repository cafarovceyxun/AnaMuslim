package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.search.HadithSearchResultCard
import com.cafarovceyxun.anamuslim.compose.components.search.SearchLinkRow
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.hadithSearchHere
import com.cafarovceyxun.anamuslim.resources.hadithSearchInText
import com.cafarovceyxun.anamuslim.resources.hadithSearchOtherTitles
import com.cafarovceyxun.anamuslim.resources.hadithSearchSeeAll
import com.cafarovceyxun.anamuslim.search.HadithSearch
import com.cafarovceyxun.anamuslim.search.SearchResult
import com.cafarovceyxun.anamuslim.search.hadithLevelKey
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Hədis indeks ekranlarındakı axtarış qutusunun qlobal yarısı.
 *
 * Qutu əvvəl yalnız açıq səviyyənin adlarını süzürdü (cildlər ekranında cild adlarını, kitab
 * ekranında kitab adlarını …): istifadəçi babın hansı cilddə olduğunu, sözün isə başlıqda yoxsa
 * mətndə keçdiyini bilmir, ekran boş qalırdı. İndi yerli nəticələrin altında bütün bazadan başlıq
 * və mətn uyğunluqları gəlir — Axtarış ekranı ilə eyni funksiyalardan ([HadithSearch]).
 */

/** Qısa sorğu (bir hərf) hər hədisin mətnində keçir — siyahı faydasız və yavaş olur. */
private const val MIN_QUERY_LENGTH = 2
private const val TITLE_LIMIT = 8
private const val TEXT_LIMIT = 5

/** Hərf yığılarkən hər basışda baza sorğusu getməsin. */
private const val DEBOUNCE_MS = 250L

data class HadithGlobalSearchState(
    val query: String = "",
    val titles: List<SearchResult> = emptyList(),
    val texts: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
) {
    val isEmpty: Boolean get() = titles.isEmpty() && texts.isEmpty()

    /** Boş-vəziyyət kartı yalnız axtarış bitəndən sonra: yoxsa hər hərfdə bir anlıq yanıb-sönür. */
    val isSettledEmpty: Boolean get() = isEmpty && !isLoading
}

/**
 * Qlobal nəticəyə toxunanda nə olacağı. `HadithIndexScreen` verir — keçid məntiqi (alt babsız bab
 * → `DIRECT_VIEW`, yerində və ya host naviqasiyası) orada bir dəfə yazılıb.
 */
class HadithGlobalSearchActions(
    /** Cild / kitab / bab / alt bab başlığı — həmin səviyyə açılır. */
    val onOpenTitle: (SearchResult) -> Unit,
    /**
     * Mətn uyğunluğu — hədisin tam mətni vərəqdə açılır, oradan oxucuya. Bölmənin bütün mətn
     * nəticələri ötürülür ki, vərəqi sağa-sola sürüşdürərək onların arasında keçmək olsun.
     */
    val onOpenText: (results: List<SearchResult>, index: Int, query: String) -> Unit,
)

/**
 * [query] üçün bütün hədis bazasında başlıq və mətn axtarışı.
 *
 * [shownKeys] ekranın özünün artıq göstərdiyi sətirlərdir ([hadithLevelKey] formasında) — qlobal
 * bölmə onları ikinci dəfə sadalamır. Çağıran onu `remember` ilə saxlamalıdır: hər kompozisiyada
 * yeni dəst axtarışı yenidən başladır.
 *
 * Kompozisiyanın scope-u burada təhlükəsizdir: iş tək addımlı və yalnız oxuyandır, ləğv olunsa
 * itən heç nə yoxdur.
 */
@Composable
fun rememberHadithGlobalSearch(query: String, shownKeys: Set<String>): HadithGlobalSearchState {
    val trimmed = query.trim()
    val state by produceState(HadithGlobalSearchState(), trimmed, shownKeys) {
        if (trimmed.length < MIN_QUERY_LENGTH) {
            value = HadithGlobalSearchState()
            return@produceState
        }

        // Köhnə nəticələr yeni gələnə qədər qalır — hər hərfdə siyahının boşalıb dolması gözü yorur.
        value = value.copy(isLoading = true)
        delay(DEBOUNCE_MS)

        // `runCatching` yox: o, ləğvi də udur və yeni hərf köhnə axtarışı kəsəndə boş nəticə yazardı.
        value = try {
            withContext(Dispatchers.IO) {
                HadithGlobalSearchState(
                    query = trimmed,
                    titles = HadithSearch.searchTitles(trimmed, TITLE_LIMIT, shownKeys),
                    texts = HadithSearch.searchTexts(trimmed, TEXT_LIMIT, 0),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HadithGlobalSearchState(query = trimmed)
        }
    }
    return state
}

/** «Bu siyahıda» — yerli nəticələrin başlığı; yalnız altında qlobal bölmə olanda mənası var. */
fun LazyGridScope.hadithLocalResultsTitle(global: HadithGlobalSearchState) {
    if (global.isEmpty) return
    item(key = "hadith-search-here", span = { GridItemSpan(maxLineSpan) }) {
        HadithSearchSectionTitle(stringResource(Res.string.hadithSearchHere), top = 0.dp)
    }
}

/** Qlobal bölmə — `LazyVerticalGrid` ekranları üçün; hər sətir bütün eni tutur. */
fun LazyGridScope.hadithGlobalSearchItems(
    global: HadithGlobalSearchState,
    query: String,
    actions: HadithGlobalSearchActions,
) {
    hadithGlobalSearchEntries(global, query, actions).forEach { entry ->
        item(key = entry.key, span = { GridItemSpan(maxLineSpan) }) { entry.content() }
    }
}

/** Eyni bölmə — `LazyColumn` ekranı (mündəricat görünüşü) üçün. */
fun LazyListScope.hadithGlobalSearchItems(
    global: HadithGlobalSearchState,
    query: String,
    actions: HadithGlobalSearchActions,
) {
    hadithGlobalSearchEntries(global, query, actions).forEach { entry ->
        item(key = entry.key) { entry.content() }
    }
}

private class GlobalEntry(val key: String, val content: @Composable () -> Unit)

/** İki lazy konteynerin paylaşdığı sətir siyahısı — sıra və açarlar bir yerdə. */
private fun hadithGlobalSearchEntries(
    global: HadithGlobalSearchState,
    query: String,
    actions: HadithGlobalSearchActions,
): List<GlobalEntry> = buildList {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return@buildList

    if (global.titles.isNotEmpty()) {
        add(GlobalEntry("hadith-search-titles") {
            HadithSearchSectionTitle(stringResource(Res.string.hadithSearchOtherTitles))
        })
        global.titles.forEach { result ->
            // `g:` prefiksi: ekranın öz sətirləri eyni səviyyə açarlarını işlədə bilər.
            add(GlobalEntry("g:" + (result.hadithLevelKey ?: result.hashCode().toString())) {
                HadithSearchResultCard(result) { actions.onOpenTitle(result) }
            })
        }
    }

    if (global.texts.isNotEmpty()) {
        add(GlobalEntry("hadith-search-texts") {
            HadithSearchSectionTitle(stringResource(Res.string.hadithSearchInText))
        })
        global.texts.forEachIndexed { index, result ->
            add(GlobalEntry("gh:" + (result.hadith?.id ?: result.hashCode())) {
                HadithSearchResultCard(result) { actions.onOpenText(global.texts, index, global.query) }
            })
        }
    }

    // Tam siyahı Axtarış ekranındadır, yalnız hədis əhatəsində. Seam qeydiyyatsızdırsa sətir
    // göstərilmir — yoxsa basılar və heç nə olmazdı.
    val openHadithSearch = ReaderUiHooks.openHadithSearch
    if (openHadithSearch != null) {
        add(GlobalEntry("hadith-search-all") {
            SearchLinkRow(
                text = stringResource(Res.string.hadithSearchSeeAll, trimmed),
                onClick = { openHadithSearch(trimmed) },
                modifier = Modifier.padding(top = 8.dp),
            )
        })
    }
}

@Composable
internal fun HadithSearchSectionTitle(text: String, top: Dp = 12.dp) {
    Text(
        text = text,
        style = typography.titleSmall,
        color = colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = top, bottom = 4.dp),
    )
}
