package com.cafarovceyxun.anamuslim.compose.components.reference

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderProvider
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceContent
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceData
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceVerses
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.quickReferenceSwipe
import com.cafarovceyxun.anamuslim.compose.components.search.HadithQuickReferenceContent
import com.cafarovceyxun.anamuslim.compose.components.search.HadithQuickReferenceData
import com.cafarovceyxun.anamuslim.compose.screens.dua.DuaSourcePeekContent
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithShareSheet
import com.cafarovceyxun.anamuslim.compose.screens.hadith.hadithTitleTextNow
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.repository.loadHadithLocation
import com.cafarovceyxun.anamuslim.search.SearchResult
import com.cafarovceyxun.anamuslim.search.SearchResultMatch
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.utils.reader.VerseHighlight
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithLocation

/**
 * İstinad vərəqində göstərilən bir element: ayə, axtarışdan gələn hədis və ya dua/dəlil qaynağı.
 *
 * Bir ekranın istinadları bir siyahıdır və vərəq sağa-sola sürüşdürəndə həmin siyahı boyu gedir —
 * növ dəyişəndə də (Əsmada hədis dəlilindən ayə dəlilinə, axtarışda ayədən hədisə).
 */
sealed interface ReferencePeekItem {
    /** Ayə — oxucunun sürətli baxışı ([QuickReferenceContent]). */
    data class Verse(val data: QuickReferenceData, val verses: QuickReferenceVerses) : ReferencePeekItem

    /** Axtarış nəticəsindəki hədis — sorğunun sözləri işarələnir ([HadithQuickReferenceContent]). */
    data class Hadith(val data: HadithQuickReferenceData) : ReferencePeekItem

    /**
     * Dua, Əsma dəlili və ya Həcc dəlilinin **qaynağı** — tam mətn, çıxarış işarələnmiş
     * ([DuaSourcePeekContent]).
     *
     * @param isEvidence başlıq: «Dəlilin qaynağı» (true) və ya «Duanın qaynağı».
     */
    data class Source(val ref: DuaSourceRef, val isEvidence: Boolean) : ReferencePeekItem
}

/**
 * Ayə aralığının vərəq elementi — tərcümələr istifadəçinin öz seçimidir.
 *
 * @param highlight ərəbcə mətndə işarələnəcək hissə (Əsmada adın özü və ya dəlilin çıxarışı).
 */
fun verseReferenceItem(
    chapterNo: Int,
    verseNo: Int,
    verseEnd: Int?,
    highlight: VerseHighlight?,
): ReferencePeekItem.Verse {
    val verses = QuickReferenceVerses.Range(chapterNo, verseNo..(verseEnd ?: verseNo))
    return ReferencePeekItem.Verse(
        data = QuickReferenceData(
            slugs = emptySet(),
            chapterNo = chapterNo,
            parsedVerses = verses,
            arabicHighlight = highlight,
        ),
        verses = verses,
    )
}

/**
 * Axtarış nəticəsinin vərəq elementi; başlıq uyğunluğu (cild/kitab/bab adı) üçün null — o vərəq
 * açmır, səviyyəni birbaşa açır, ona görə sürüşdürmə onları atlayır.
 */
fun SearchResult.toPeekItem(query: String): ReferencePeekItem? {
    hadith?.let {
        return ReferencePeekItem.Hadith(
            HadithQuickReferenceData(
                hadith = it,
                query = query,
                volume = volume,
                book = book,
                chapter = chapter,
            ),
        )
    }
    if (volume != null || book != null || chapter != null || subChapter != null) return null

    val surah = chapterNo ?: return null
    val verse = verseNo ?: return null
    return ReferencePeekItem.Verse(
        data = QuickReferenceData(
            // Tərcümə uyğunluğunda vərəq həmin tərcümələri göstərir; ərəbcə mətn uyğunluğunda
            // siyahı boşdur və vərəq istifadəçinin seçdiyi tərcümələrə düşür.
            slugs = matches.filterIsInstance<SearchResultMatch.TranslationMatch>()
                .map { it.slug }
                .toSet(),
            chapterNo = surah,
            verses = verse.toString(),
            // Sorğu vərəqə də gedir ki, tapılan söz orada sarı ilə işarələnsin.
            query = query,
        ),
        verses = QuickReferenceVerses.Range(surah, verse..verse),
    )
}

/**
 * İstinadın tam mətni — ayə, hədis və ya dua qaynağı — və **sağa-sola sürüşdürərək** ekranın
 * qonşu istinadlarına keçid: axtarış nəticələri, duanın hissələri, Əsma adının dəlilləri, Həcc
 * bölməsinin dəlilləri.
 *
 * Niyə bir vərəq: siyahılarda növlər qarışıqdır, ayrı vərəqlər olsaydı ayədən hədisə keçmək üçün
 * vərəqi bağlayıb yenidən açmaq lazım gələrdi. Burada vərəq açıq qalır, yalnız içi dəyişir.
 *
 * Mövqe çağıranda saxlanılır ([index]/[onIndexChange]): Axtarış ekranının siyahısı səhifələnir və
 * sona yaxınlaşanda növbəti səhifəni o özü istəyir — [items] da elə ona görə canlı siyahıdır.
 *
 * Paylaşma vərəqi də buradadır: o da `ModalBottomSheet`-dir, ona görə əvvəl bu vərəq bağlanır,
 * sonra o açılır (oxucudakı `HadithOptionsSheet` → `HadithShareSheet` qaydası).
 *
 * @param index açıq elementin [items]-dəki yeri; null = vərəq bağlıdır.
 * @param hasMore siyahının sonundan sonra hələ yüklənməmiş nəticə var — sayğac «25+» yazır.
 * @param onOpenHadith axtarış hədisini oxucuda açır və hədisə enir; alt babsız hədisdə
 *   `subChapterSlug` null-dur. Siyahıda [ReferencePeekItem.Hadith] olmayan ekranlar null verir
 *   (dua qaynağı «aç» düyməsini `LocalDuaActions`-dan alır).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferencePeek(
    items: List<ReferencePeekItem>,
    index: Int?,
    onIndexChange: (Int) -> Unit,
    hasMore: Boolean,
    onOpenVerse: (chapterNo: Int, range: IntRange) -> Unit,
    onOpenHadith: ((
        volumeSlug: String?,
        bookSlug: String?,
        chapterSlug: String?,
        subChapterSlug: String?,
        title: String,
        hadithId: Long?,
        query: String,
    ) -> Unit)?,
    onClose: () -> Unit,
) {
    var sharingHadith by remember { mutableStateOf<Hadith?>(null) }

    // «Əlavə qaynaq» sətri üçün hədisin ağacdakı yeri. Nəticə sətri yalnız cild/kitab/bab daşıyır,
    // alt bab isə yalnız slug kimi hədisin içindədir — ona görə tam zəncir bazadan oxunur.
    var shareLocation by remember { mutableStateOf(HadithLocation()) }
    LaunchedEffect(sharingHadith) {
        val hadith = sharingHadith
        shareLocation = if (hadith == null) HadithLocation() else loadHadithLocation(hadith)
    }

    HadithShareSheet(
        hadith = sharingHadith,
        location = shareLocation,
        onDismiss = { sharingHadith = null },
    )

    if (index == null || items.getOrNull(index) == null) return

    val step: (Int) -> Unit = { direction ->
        val next = index + direction
        // Sərhəddə heç nə etmir — dairəvi keçid uzun siyahıda «harada idim?» sualı yaradır.
        if (next in items.indices) onIndexChange(next)
    }

    // Tək nəticədə sayğac yoxdur — sürüşdürüləcək yer də yoxdur. Funksiyadır, dəyər deyil: keçid
    // canlandırılarkən çıxan element öz nömrəsini göstərməlidir, gələnin nömrəsini yox.
    val positionOf: (Int) -> String? = { i ->
        if (items.size > 1 || hasMore) "${i + 1} / ${items.size}${if (hasMore) "+" else ""}" else null
    }

    // Ayə vərəqi ilə eyni davranış: yarıda açılır, yuxarı çəkəndə böyüyür (bax `QuickReference`).
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ReaderProvider {
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = sheetState,
            scrimColor = colorScheme.scrim.alpha(0.5f),
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface,
            dragHandle = null,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Top) },
        ) {
            // Keçid sürüşmə ilə canlandırılır ki, barmağın istiqaməti ilə yeni nəticənin gəldiyi
            // tərəf üst-üstə düşsün — yoxsa ayə dəyişir, amma «nə oldu?» sualı qalırdı.
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it else it } + fadeOut()) using
                        SizeTransform(clip = true)
                },
                label = "referencePeek",
            ) { shownIndex ->
                when (val item = items.getOrNull(shownIndex)) {
                    is ReferencePeekItem.Verse -> QuickReferenceContent(
                        data = item.data,
                        parsed = item.verses,
                        onStep = step,
                        onOpenInReader = onOpenVerse,
                        onClose = onClose,
                        position = positionOf(shownIndex),
                    )

                    is ReferencePeekItem.Hadith -> HadithQuickReferenceContent(
                        data = item.data,
                        onOpen = open@{ ref ->
                            val openHadith = onOpenHadith ?: return@open
                            onClose()
                            val hadith = ref.hadith
                            val title = ref.chapter?.let { hadithTitleTextNow(it.name, it.name_ar) }
                                ?: ref.book?.let { hadithTitleTextNow(it.name, it.name_ar) }
                                ?: ""

                            openHadith(
                                ref.volume?.slug ?: ref.book?.volume_slug,
                                ref.book?.slug ?: ref.chapter?.book_slug,
                                ref.chapter?.slug ?: hadith.chapter_slug,
                                hadith.sub_chapter_slug,
                                title,
                                hadith.id,
                                ref.query,
                            )
                        },
                        onShare = { hadith ->
                            onClose()
                            sharingHadith = hadith
                        },
                        onClose = onClose,
                        position = positionOf(shownIndex),
                        modifier = Modifier.quickReferenceSwipe(step),
                    )

                    is ReferencePeekItem.Source -> DuaSourcePeekContent(
                        ref = item.ref,
                        isEvidence = item.isEvidence,
                        onClose = onClose,
                        position = positionOf(shownIndex),
                        modifier = Modifier.quickReferenceSwipe(step),
                    )

                    null -> Unit
                }
            }
        }
    }
}
