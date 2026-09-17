package com.cafarovceyxun.anamuslim.repository

import com.cafarovceyxun.anamuslim.search.FtsQueryBuilder
import com.cafarovceyxun.anamuslim.utils.dua.AsmaVerseMatcher
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta

/**
 * Bir adın avtomatik tapılmış ayəsi.
 *
 * `asma_evidence` deyil: bu sətir bazada yoxdur, hər dəfə lokal `arabic_search` indeksindən
 * hesablanır. Ona görə `id`-si də yoxdur — birmənalı açarı `(chapterNo, verseNo)`-dur.
 */
data class AutoVerseMatch(
    val chapterNo: Int,
    val verseNo: Int,
    /** Göstərmək üçün **hərəkəli** mətn — indeksdəki hərəkəsiz forma deyil. */
    val textAr: String,
)

/**
 * Əsmaül Hüsnə adlarını Quran ayələri ilə avtomatik uyğunlaşdırır.
 *
 * Hər şey **lokaldır**: `quranapp.db` içindəki `arabic_search` FTS4 cədvəli Quran mətnini onsuz da
 * hərəkəsiz saxlayır, yəni «hərəkələrə baxmadan uyğunlaşdırma» üçün yeni indeks qurmağa ehtiyac
 * yoxdur. Buna görə bu axtarış **oflayn** da işləyir — yalnız gizlətmə qərarları serverdəndir.
 *
 * ⚠️ Nəticələr keşlənmir: bu, tətbiqin öz axtarış ekranı ilə eyni qiymət sinfindən lokal FTS
 * sorğusudur (millisaniyələr). Keşləmək `DataStore` sətrini meqabaytlarla şişirdərdi. Yalnız 99 adın
 * **sayğacı** keşlənir (`AsmaViewModel`), çünki siyahı ekranı açılanda 99 ayrı `COUNT(*)` yeganə
 * həqiqətən bahalı hissədir.
 */
class AsmaAutoEvidenceRepository(
    private val quranRepository: QuranRepository = RepositoryProvider.quranRepository,
) {

    /**
     * Ada neçə ayə uyğun gəlir.
     *
     * @return `0` — ad ərəbcə deyilsə və ya sorğuya çevrilə bilməyəndə.
     */
    suspend fun matchCount(nameAr: String): Int {
        val query = queryFor(nameAr) ?: return 0
        return quranRepository.countMatchedAyahs(query)
    }

    /**
     * Uyğun gələn ayələr, mushaf sırası ilə, səhifə-səhifə.
     *
     * Səhifələmə məcburidir: bəzi adlar minlərlə ayədə keçir («الله» ~2700), hamısını birdən
     * kompozisiya etmək ekranı dondurar.
     *
     * @param exclude göstərilməyəcək `(surə, ayə)` cütləri — admin tərəfindən gizlədilmişlər.
     *   Süzgəc **çəkildikdən sonra** tətbiq olunur, ona görə bir səhifə gözləniləndən az element
     *   qaytara bilər; bu, «daha çox yüklə» məntiqini pozmur, çünki növbəti səhifə `offset`-lə gəlir.
     */
    suspend fun matches(
        nameAr: String,
        limit: Int,
        offset: Int,
        exclude: Set<Pair<Int, Int>> = emptySet(),
    ): List<AutoVerseMatch> {
        val query = queryFor(nameAr) ?: return emptyList()

        val rows = quranRepository.arabicTextSearch(query, limit, offset)
        if (rows.isEmpty()) return emptyList()

        // İndeksdəki mətn hərəkəsizdir — ekranda mushaf yazılışı görünməlidir.
        val texts = quranRepository.getVerseTextsForAyahs(rows.map { it.ayahId })

        return rows.mapNotNull { row ->
            val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)
            if (chapterNo to verseNo in exclude) return@mapNotNull null

            AutoVerseMatch(
                chapterNo = chapterNo,
                verseNo = verseNo,
                textAr = texts[row.ayahId]?.takeIf { it.isNotBlank() } ?: row.text,
            )
        }
    }

    private fun queryFor(nameAr: String): String? =
        FtsQueryBuilder.toExactOrQuery(AsmaVerseMatcher.arabicNameVariants(nameAr))
}
