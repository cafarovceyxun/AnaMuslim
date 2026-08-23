package com.cafarovceyxun.anamuslim.repository

import com.cafarovceyxun.anamuslim.compose.utils.appFallbackLanguageCodes
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.ChapterVerseBatch
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.db.QuranDatabase
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.MushafLineType
import com.cafarovceyxun.anamuslim.db.entities.quran.MushafMapEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.NavigationType
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahEntity
import com.cafarovceyxun.anamuslim.db.entities.wbw.WbwWordEntity
import com.cafarovceyxun.anamuslim.db.relations.NavigationUnit
import com.cafarovceyxun.anamuslim.db.relations.NavigationUnitRange
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.quran.AzerbaijaniSurahNames
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.toQuranMushafId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class QuranRepository(
    private val database: QuranDatabase,
    private val extDatabase: ExternalQuranDatabase
) : QuranVerseStructure {
    companion object {
        private const val ARBITRARY_BATCH_CHUNK_SIZE = 400
    }

    private val mushafDao get() = database.mushafDao()
    private val arabicSearchDao get() = database.arabicSearchDao()
    private val ayahDao get() = database.ayahDao()
    private val ayahWordDao get() = database.ayahWordDao()
    private val surahDao get() = database.surahDao()
    private val surahSearchDao get() = database.surahSearchDao()
    private val navigationDao get() = database.navigationDao()
    private val wbwDao get() = extDatabase.wbwDao()
    val extrasDao get() = database.extrasDao()

    suspend fun getNumberOfPages(mushafId: Int): Int {
        if (mushafId <= 0) return 0
        return mushafDao.getMushaf(mushafId)?.noOfPages ?: 0
    }

    suspend fun getPageLines(mushafId: Int, pageNo: Int): List<MushafMapEntity> {
        if (mushafId <= 0 || pageNo <= 0) return emptyList()
        return mushafDao.getPageLines(mushafId, pageNo)
    }

    suspend fun getJuzForMushafPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): Map<Int, Int> {
        if (mushafId <= 0 || pageNumbers.isEmpty()) return emptyMap()

        return mushafDao.getJuzForPages(mushafId, pageNumbers)
            .associate { it.pageNumber to it.juzNo }
    }

    suspend fun getHizbForMushafPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): Map<Int, List<Int>> {
        if (mushafId <= 0 || pageNumbers.isEmpty()) return emptyMap()

        return mushafDao.getHizbForPages(mushafId, pageNumbers)
            .groupBy { it.pageNumber }
            .mapValues { (_, rows) ->
                rows.map { it.hizbNo }.distinct().sorted()
            }
    }


    suspend fun getSurah(
        chapterNo: Int,
    ): SurahEntity? {
        return surahDao.getSurah(chapterNo)
    }

    suspend fun getSurahWithLocalizations(
        chapterNo: Int,
    ): SurahWithLocalizations? {
        return surahDao.getSurahWithLocalization(chapterNo)
    }

    suspend fun getAyah(
        chapterNo: Int,
        verseNo: Int,
    ): AyahEntity? {
        return ayahDao.getAyah(chapterNo, verseNo)
    }

    suspend fun getAyahById(ayahId: Int): AyahEntity? = ayahDao.getAyahById(ayahId)

    suspend fun getVerseWithDetails(
        chapterNo: Int,
        verseNo: Int,
        scriptCode: String? = null,
        arabicEnabled: Boolean,
    ): VerseWithDetails? {
        val script = scriptCode ?: ReaderPreferences.getQuranScript()

        val batch =
            loadVersesBatch(chapterNo, verseNo, verseNo, script, arabicEnabled) ?: return null

        val ayah = batch.ayahByVerseNo[verseNo] ?: return null
        val words = batch.wordsByVerseNo[verseNo] ?: emptyList()

        return VerseWithDetails(
            words = words,
            pageNo = batch.pageByVerseNo[verseNo] ?: 0,
            verse = ayah,
            chapter = batch.surah
        )
    }

    suspend fun loadVersesBatch(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        scriptCode: String,
        arabicEnabled: Boolean,
    ): ChapterVerseBatch? {
        val surah = surahDao.getSurahWithLocalization(chapterNo) ?: return null

        val lo = minOf(fromVerse, toVerse)
        val hi = minOf(maxOf(fromVerse, toVerse), surah.surah.ayahCount)

        val ayahs = ayahDao.getAyahsInRange(chapterNo, lo, hi)

        if (ayahs.isEmpty()) return null

        val mushafId = scriptCode.toQuranMushafId(ReaderPreferences.getQuranScriptVariant())
        val ayahByVerse = ayahs.associateBy { it.ayahNo }
        val verseIds = ayahs.map { it.ayahId }

        val pageByAyahId = if (verseIds.isNotEmpty()) {
            mushafDao.getPagesForAyahIds(mushafId, verseIds)
                .associate { it.ayahId to it.pageNumber }
        } else {
            emptyMap()
        }

        val pageByVerse = HashMap<Int, Int>(ayahs.size)

        for (a in ayahs) {
            pageByVerse[a.ayahNo] = pageByAyahId[a.ayahId] ?: -1
        }

        val wordsFlat = if (arabicEnabled && verseIds.isNotEmpty()) {
            ayahWordDao.getWordsForAyahs(verseIds, scriptCode)
        } else {
            emptyList()
        }

        val wordsByAyahId = groupWordsByAyahIdWithLastFlags(wordsFlat)

        val wordsByVerse = HashMap<Int, List<AyahWordEntity>>(ayahs.size)
        for (a in ayahs) {
            wordsByVerse[a.ayahNo] = wordsByAyahId[a.ayahId] ?: emptyList()
        }

        return ChapterVerseBatch(
            surah = surah,
            ayahByVerseNo = ayahByVerse,
            wordsByVerseNo = wordsByVerse,
            pageByVerseNo = pageByVerse,
        )
    }

    /**
     * Batched load for an arbitrary set of verse numbers in one chapter (e.g. quick reference).
     */
    suspend fun loadArbitraryVersesBatch(
        chapterNo: Int,
        verseNos: List<Int>,
        scriptCode: String,
        arabicEnabled: Boolean,
    ): ChapterVerseBatch? {
        val distinct = verseNos.asSequence()
            .filter { it > 0 }
            .distinct()
            .sorted()
            .toList()
        if (distinct.isEmpty()) return null

        if (distinct.isContiguousRange()) {
            return loadVersesBatch(
                chapterNo = chapterNo,
                fromVerse = distinct.first(),
                toVerse = distinct.last(),
                scriptCode = scriptCode,
                arabicEnabled = arabicEnabled,
            )
        }

        val ayahIds = distinct.map { QuranMeta.getAyahId(chapterNo, it) }
        val ayahs = ayahIds.chunked(ARBITRARY_BATCH_CHUNK_SIZE).flatMap { idsChunk ->
            ayahDao.getAyahsByIds(idsChunk)
        }

        if (ayahs.isEmpty()) return null

        val surah = surahDao.getSurahWithLocalization(chapterNo) ?: return null

        val mushafId = scriptCode.toQuranMushafId(ReaderPreferences.getQuranScriptVariant())
        val ayahByVerse = ayahs.associateBy { it.ayahNo }
        val verseIds = ayahs.map { it.ayahId }

        val wordsFlat = if (arabicEnabled) {
            verseIds.chunked(ARBITRARY_BATCH_CHUNK_SIZE).flatMap { verseIdChunk ->
                ayahWordDao.getWordsForAyahs(verseIdChunk, scriptCode)
            }
        } else emptyList()

        val wordsByAyahId = groupWordsByAyahIdWithLastFlags(wordsFlat)

        val pageByAyahId = if (verseIds.isNotEmpty()) {
            verseIds.chunked(ARBITRARY_BATCH_CHUNK_SIZE)
                .flatMap { verseIdChunk ->
                    mushafDao.getPagesForAyahIds(mushafId, verseIdChunk)
                }
                .associate { it.ayahId to it.pageNumber }
        } else {
            emptyMap()
        }

        val pageByVerse = HashMap<Int, Int>(ayahs.size)

        for (a in ayahs) {
            pageByVerse[a.ayahNo] = pageByAyahId[a.ayahId] ?: -1
        }

        val wordsByVerse = HashMap<Int, List<AyahWordEntity>>(ayahs.size)

        for (a in ayahs) {
            wordsByVerse[a.ayahNo] = wordsByAyahId[a.ayahId] ?: emptyList()
        }

        return ChapterVerseBatch(
            surah = surah,
            ayahByVerseNo = ayahByVerse,
            wordsByVerseNo = wordsByVerse,
            pageByVerseNo = pageByVerse,
        )
    }

    suspend fun getWordsForAyah(
        chapterNo: Int,
        verseNo: Int,
        scriptCode: String
    ): List<AyahWordEntity> {
        val words = ayahWordDao.getWordsForAyah(chapterNo, verseNo, scriptCode)
            .sortedBy { it.wordIndex }

        val lastWordIndex = words.lastOrNull()?.wordIndex

        return words.map {
            it.apply {
                isLastWordOfAyah = it.wordIndex == lastWordIndex
            }
        }
    }

    suspend fun getWordsForAyahById(ayahId: Int, scriptCode: String): List<AyahWordEntity> {
        val words = ayahWordDao.getWordsForAyahById(ayahId, scriptCode)
            .sortedBy { it.wordIndex }

        val lastWordIndex = words.lastOrNull()?.wordIndex

        return words.map {
            it.apply {
                isLastWordOfAyah = it.wordIndex == lastWordIndex
            }
        }
    }

    suspend fun getWbwWordsForAyahs(
        wbwId: String,
        ayahIds: List<Int>,
        wbwTranslation: Boolean,
        wbwTransliteration: Boolean,
    ): Map<Int, Map<Int, WbwWordEntity>> {
        if (wbwId.isBlank() || ayahIds.isEmpty()) return emptyMap()

        val rows = wbwDao.getWordsForAyahs(wbwId, ayahIds.distinct())

        if (rows.isEmpty()) return emptyMap()

        val byAyah = LinkedHashMap<Int, MutableMap<Int, WbwWordEntity>>()

        for (row in rows) {
            byAyah.getOrPut(row.ayahId) { LinkedHashMap() }[row.wordIndex] = row.copy(
                translation = if (wbwTranslation) row.translation else null,
                transliteration = if (wbwTransliteration) row.transliteration else null,
            )
        }

        return byAyah.mapValues { it.value.toMap() }
    }

    suspend fun resolveMushafLineWords(
        row: MushafMapEntity,
        scriptCode: String,
        wordCache: Map<Int, List<AyahWordEntity>>? = null,
    ): List<AyahWordEntity> {
        if (row.lineType != MushafLineType.ayah) return emptyList()

        val startAyah = row.startAyahId ?: return emptyList()
        val endAyah = row.endAyahId ?: return emptyList()
        val startWi = row.startWordIndex ?: return emptyList()
        val endWi = row.endWordIndex ?: return emptyList()

        if (startAyah > endAyah) return emptyList()

        if (startAyah == endAyah) {
            val lastWordIndex = if (wordCache != null) {
                (wordCache[startAyah] ?: getWordsForAyahById(startAyah, scriptCode))
                    .lastOrNull()?.wordIndex
            } else {
                ayahWordDao.getLastWordIndexForAyah(startAyah, scriptCode)
            }

            val words = if (wordCache != null) {
                (wordCache[startAyah] ?: getWordsForAyahById(startAyah, scriptCode))
                    .asSequence()
                    .filter { it.wordIndex in startWi..endWi }
                    .sortedBy { it.wordIndex }
                    .toList()
            } else {
                ayahWordDao.getWordsForAyahByIndexRange(
                    startAyah,
                    scriptCode,
                    startWi,
                    endWi,
                )
            }

            return words.map {
                it.apply {
                    isLastWordOfAyah = lastWordIndex != null && wordIndex == lastWordIndex
                }
            }
        }

        val out = ArrayList<AyahWordEntity>(32)

        if (wordCache != null) {
            (wordCache[startAyah] ?: getWordsForAyahById(startAyah, scriptCode))
                .asSequence()
                .filter { it.wordIndex >= startWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        } else {
            getWordsForAyahById(startAyah, scriptCode)
                .asSequence()
                .filter { it.wordIndex >= startWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        }

        if (endAyah - startAyah > 1) {
            val middle = ayahDao.getAyahsStrictlyBetween(startAyah, endAyah)

            if (wordCache != null) {
                for (ayah in middle) {
                    (wordCache[ayah.ayahId] ?: getWordsForAyahById(ayah.ayahId, scriptCode))
                        .mapTo(out) { it }
                }
            } else {
                val middleIds = middle.map { it.ayahId }
                if (middleIds.isNotEmpty()) {
                    val flat = ayahWordDao.getWordsForAyahs(middleIds, scriptCode)
                    val byId = groupWordsByAyahIdWithLastFlags(flat)
                    for (ayah in middle) {
                        byId[ayah.ayahId]?.mapTo(out) { it }
                    }
                }
            }
        }

        if (wordCache != null) {
            (wordCache[endAyah] ?: getWordsForAyahById(endAyah, scriptCode))
                .asSequence()
                .filter { it.wordIndex <= endWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        } else {
            getWordsForAyahById(endAyah, scriptCode)
                .asSequence()
                .filter { it.wordIndex <= endWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        }

        return out
    }

    private fun groupWordsByAyahIdWithLastFlags(
        flat: List<AyahWordEntity>,
    ): Map<Int, List<AyahWordEntity>> {
        if (flat.isEmpty()) return emptyMap()

        val grouped = flat.groupBy { it.ayahId }
        val out = HashMap<Int, List<AyahWordEntity>>(grouped.size)

        for ((ayahId, list) in grouped) {
            val sorted = list.sortedBy { it.wordIndex }
            val last = sorted.lastOrNull()?.wordIndex
            out[ayahId] = sorted.map { w ->
                w.apply {
                    isLastWordOfAyah = last != null && w.wordIndex == last
                }
            }
        }

        return out
    }

    suspend fun getChapterVerseRangesInJuz(juzNo: Int): List<Pair<Int, IntRange>> {
        if (juzNo <= 0) return emptyList()

        val ayahs = ayahDao.getAyahsByJuz(juzNo)

        if (ayahs.isEmpty()) return emptyList()

        return ayahs.groupBy { it.surahNo }
            .entries
            .sortedBy { it.key }
            .map { (surahNo, list) ->
                val minAyah = list.minOf { it.ayahNo }
                val maxAyah = list.maxOf { it.ayahNo }
                surahNo to (minAyah..maxAyah)
            }
    }

    suspend fun getOrderedSurahNosOnMushafPage(mushafId: Int, pageNo: Int): List<Int> {
        if (mushafId <= 0 || pageNo <= 0) return emptyList()

        val rows = getPageLines(mushafId, pageNo)

        val idsNeedingLookup = rows.mapNotNull { row ->
            if ((row.surahNo ?: 0) > 0) null
            else row.startAyahId?.takeIf { it > 0 }
        }.distinct()

        val ayahById = if (idsNeedingLookup.isNotEmpty()) {
            ayahDao.getAyahsByIds(idsNeedingLookup).associateBy { it.ayahId }
        } else {
            emptyMap()
        }

        val ordered = LinkedHashSet<Int>()

        for (row in rows) {
            val surahNo = row.surahNo?.takeIf { it > 0 }
                ?: row.startAyahId?.let { ayahById[it]?.surahNo }
            if (surahNo != null && surahNo > 0) {
                ordered.add(surahNo)
            }
        }

        return ordered.toList()
    }

    suspend fun getChapterNamesOnMushafPage(
        mushafId: Int,
        pageNo: Int,
    ): String {
        val surahNos = getOrderedSurahNosOnMushafPage(mushafId, pageNo)
        if (surahNos.isEmpty()) return ""

        val withLocs = surahDao.getSurahsWithLocalizationsByNos(surahNos)
        val byNo = withLocs.associateBy { it.surah.surahNo }

        return buildString {
            for (surahNo in surahNos) {
                if (isNotEmpty()) append(", ")
                append(byNo[surahNo]?.getCurrentName().orEmpty())
            }
        }
    }

    suspend fun getPageLinesGroupedForPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): Map<Int, List<MushafMapEntity>> {
        if (mushafId <= 0 || pageNumbers.isEmpty()) return emptyMap()
        val rows = mushafDao.getPageLinesForPages(mushafId, pageNumbers)
        return rows.groupBy { it.pageNumber }
    }

    /**
     * Ayah ids covered by a mushaf ayah line, in canonical order (matches preload semantics).
     */
    suspend fun ayahIdsForMushafAyahLine(row: MushafMapEntity): List<Int> {
        if (row.lineType != MushafLineType.ayah) return emptyList()

        val startAyah = row.startAyahId ?: return emptyList()
        val endAyah = row.endAyahId ?: return emptyList()

        if (row.startWordIndex == null || row.endWordIndex == null) return emptyList()

        if (startAyah > endAyah) return emptyList()

        return if (startAyah == endAyah) {
            listOf(startAyah)
        } else {
            buildList {
                add(startAyah)

                if (endAyah - startAyah > 1) {
                    val middle = ayahDao.getAyahsStrictlyBetween(startAyah, endAyah)
                    for (ayah in middle) {
                        add(ayah.ayahId)
                    }
                }

                add(endAyah)
            }
        }
    }

    suspend fun getSurahsWithLocalizationsByChapterNos(
        chapterNos: List<Int>,
    ): Map<Int, SurahWithLocalizations> {
        if (chapterNos.isEmpty()) return emptyMap()

        return surahDao.getSurahsWithLocalizationsByNos(chapterNos)
            .associateBy { it.surah.surahNo }
    }

    suspend fun getAyahEntitiesForMushafAyahLines(rows: List<MushafMapEntity>): Map<Int, AyahEntity> {
        val allIds = LinkedHashSet<Int>()
        val intervals = mutableListOf<Pair<Int, Int>>()
        for (row in rows) {
            if (row.lineType != MushafLineType.ayah) continue
            val start = row.startAyahId ?: continue
            val end = row.endAyahId ?: continue
            if (row.startWordIndex == null || row.endWordIndex == null) continue
            if (start > end) continue
            if (start == end) {
                allIds.add(start)
            } else {
                allIds.add(start)
                allIds.add(end)
                intervals.add(start to end)
            }
        }
        val byId = HashMap<Int, AyahEntity>()
        if (allIds.isNotEmpty()) {
            for (a in ayahDao.getAyahsByIds(allIds.toList())) {
                byId[a.ayahId] = a
            }
        }
        for ((s, e) in mergeAyahIdIntervals(intervals)) {
            if (e - s > 1) {
                for (a in ayahDao.getAyahsStrictlyBetween(s, e)) {
                    byId[a.ayahId] = a
                }
            }
        }
        return byId
    }

    /**
     * Preloads full-word lists for all ayahs touched by mushaf ayah lines (for a prefetch batch).
     */
    suspend fun preloadMushafLineWordCache(
        ayahLineRows: List<MushafMapEntity>,
        scriptCode: String,
    ): Map<Int, List<AyahWordEntity>> {
        val ids = LinkedHashSet<Int>()

        for (row in ayahLineRows) {
            if (row.lineType != MushafLineType.ayah) continue
            val startAyah = row.startAyahId ?: continue
            val endAyah = row.endAyahId ?: continue
            if (row.startWordIndex == null || row.endWordIndex == null) continue
            if (startAyah > endAyah) continue
            if (startAyah == endAyah) {
                ids.add(startAyah)
            } else {
                ids.add(startAyah)
                ids.add(endAyah)
                if (endAyah - startAyah > 1) {
                    val middle = ayahDao.getAyahsStrictlyBetween(startAyah, endAyah)
                    for (ayah in middle) ids.add(ayah.ayahId)
                }
            }
        }

        if (ids.isEmpty()) return emptyMap()
        val flat = ayahWordDao.getWordsForAyahs(ids.toList(), scriptCode)
        return groupWordsByAyahIdWithLastFlags(flat)
    }

    fun getAllSurahs(): Flow<List<SurahWithLocalizations>> {
        return surahDao.getAllSurahsWithLocalizations()
    }

    suspend fun getSurahNosWithSajdah(): Set<Int> {
        return ayahDao.getDistinctSurahNosWithSajdah().toSet()
    }

    suspend fun getJuzNosForChapter(chapterNo: Int): List<Int> {
        return ayahDao.getDistinctJuzNosForSurah(chapterNo)
    }

    fun getJuzs() = getRangesResolved(NavigationType.juz)

    fun getHizbs() = getRangesResolved(NavigationType.hizb)

    fun getRubs() = getRangesResolved(NavigationType.rub)
    fun getManzil() = getRangesResolved(NavigationType.manzil)

    private fun getRangesResolved(type: NavigationType): Flow<List<NavigationUnit>> {
        return combine(
            navigationDao.getRanges(type),
            surahDao.getAllSurahsWithLocalizations()
        ) { ranges, surahs ->
            val surahsByNo = surahs.associateBy { it.surah.surahNo }
            ranges.groupBy { it.type to it.unitNo }
                .map { (key, ranges) ->
                    val (type, unitNo) = key

                    NavigationUnit(
                        type = type,
                        unitNo = unitNo,
                        ranges = ranges
                            .sortedBy { it.surahNo }
                            .mapNotNull { range ->
                                surahsByNo[range.surahNo]?.let { surah ->
                                    NavigationUnitRange(
                                        surah = surah,
                                        startAyah = range.startAyah,
                                        endAyah = range.endAyah
                                    )
                                }
                            }
                    )
                }
                .sortedWith(compareBy<NavigationUnit> { it.type.name }.thenBy { it.unitNo })
        }
    }

    suspend fun arabicTextSearch(
        ftsQuery: String,
        limit: Int,
        offset: Int,
    ) = arabicSearchDao.pageMatchedAyahs(ftsQuery, limit, offset)

    /**
     * The verse text as it is written — with its harakat — for each of [ayahIds].
     *
     * [arabicTextSearch] answers out of `arabic_search`, whose rows are stored undiacritised (that
     * is what lets a query carrying harakat match at all), so a preview built from them showed the
     * reader a stripped muṣḥaf. `ayah_words` is where the vowel marks survive.
     *
     * Always the uthmani script, never the reader's: the KFQPC scripts store private-use glyph
     * codes that only their per-page fonts can draw, and a search preview is rendered with the
     * app's generic Arabic face.
     */
    suspend fun getVerseTextsForAyahs(ayahIds: List<Int>): Map<Int, String> {
        val ids = ayahIds.distinct()
        if (ids.isEmpty()) return emptyMap()

        return ids.chunked(ARBITRARY_BATCH_CHUNK_SIZE)
            .flatMap { idsChunk ->
                ayahWordDao.getWordsForAyahs(idsChunk, QuranScriptUtils.SCRIPT_DEFAULT)
            }
            .groupBy { it.ayahId }
            .mapValues { (_, words) ->
                words
                    .sortedBy { it.wordIndex }
                    // The last word of a verse is its number marker (see `isLastWordOfAyah`); the
                    // result card already carries the reference, so in a preview it would just read
                    // as a stray digit at the end of the text.
                    .dropLast(1)
                    .joinToString(" ") { it.text }
            }
    }

    suspend fun searchSurahNos(query: String): List<Int> {
        val dbResults = try {
            surahSearchDao.searchSurahNos(
                query
                    .trim()
                    .lowercase()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { "$it*" }
            ).map { it.surahNo }
        } catch (_: Exception) {
            emptyList()
        }

        if (appFallbackLanguageCodes().any { it.startsWith("az") }) {
            return AzerbaijaniSurahNames.search(query)
        }

        return dbResults
    }

    suspend fun searchSurahs(query: String): List<SurahWithLocalizations> {
        val surahNos = searchSurahNos(query)
        if (surahNos.isEmpty()) return emptyList()

        val byNo = surahDao.getSurahsWithLocalizationsByNos(surahNos)
            .associateBy { it.surah.surahNo }

        return surahNos.mapNotNull { byNo[it] }
    }

    suspend fun getChapterName(
        chapterNo: Int,
    ): String {
        if (chapterNo <= 0) return ""

        val codes = appFallbackLanguageCodes()
        if (codes.any { it.startsWith("az") }) {
            return AzerbaijaniSurahNames.getName(chapterNo).orEmpty()
        }

        for (code in codes) {
            val name = surahDao.getLocalization(chapterNo, code)?.name
            if (!name.isNullOrBlank()) return name
        }

        return ""
    }

    suspend fun getChapterNames(chapterNos: List<Int>): Map<Int, String> {
        if (chapterNos.isEmpty()) return emptyMap()

        val result = mutableMapOf<Int, String>()
        val codes = appFallbackLanguageCodes()

        if (codes.any { it.startsWith("az") }) {
            chapterNos.forEach { chapterNo ->
                AzerbaijaniSurahNames.getName(chapterNo)?.let {
                    result[chapterNo] = it
                }
            }
            return result
        }

        for (code in codes) {
            val remaining = chapterNos.filter { it !in result.keys }
            if (remaining.isEmpty()) break

            val localizations = surahDao.getLocalizations(remaining, code)

            localizations.forEach { entity ->
                val name = entity.name

                if (!name.isNullOrBlank()) {
                    result[entity.surahNo] = name
                }
            }
        }

        return result
    }

    suspend fun getFirstPageOfChapter(chapterNo: Int, scriptCode: String? = null): Int? {
        val mushafId = (scriptCode ?: ReaderPreferences.getQuranScript())
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || chapterNo <= 0) return null

        return mushafDao.getFirstPageOfChapter(mushafId, chapterNo)
    }

    suspend fun getPageForVerse(surahNo: Int, ayahNo: Int, mushafId: Int): Int? {
        if (mushafId <= 0 || surahNo <= 0 || ayahNo <= 0) return null

        return mushafDao.getPageForVerse(mushafId, ayahId = QuranMeta.getAyahId(surahNo, ayahNo))
    }

    suspend fun getFirstPageOfJuz(juzNo: Int, scriptCode: String? = null): Int? {
        val mushafId = (scriptCode ?: ReaderPreferences.getQuranScript())
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || juzNo <= 0) return null
        return mushafDao.getFirstPageOfJuz(mushafId, juzNo)
    }

    suspend fun getFirstPageOfHizb(hizbNo: Int, scriptCode: String? = null): Int? {
        val mushafId = (scriptCode ?: ReaderPreferences.getQuranScript())
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || hizbNo <= 0) return null
        return mushafDao.getFirstPageOfHizb(mushafId, hizbNo)
    }

    suspend fun getChapterVerseRangesInHizb(hizbNo: Int): List<Pair<Int, IntRange>> {
        if (hizbNo <= 0) return emptyList()

        val ayahs = ayahDao.getAyahsByHizb(hizbNo)
        if (ayahs.isEmpty()) return emptyList()

        return ayahs.groupBy { it.surahNo }
            .entries
            .sortedBy { it.key }
            .map { (surahNo, list) ->
                val minAyah = list.minOf { it.ayahNo }
                val maxAyah = list.maxOf { it.ayahNo }
                surahNo to (minAyah..maxAyah)
            }
    }

    suspend fun getFirstAyahIdOnPage(pageNo: Int): Int? {
        val mushafId = ReaderPreferences.getQuranScript()
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        return getFirstAyahIdOnPage(mushafId, pageNo)
    }

    suspend fun getFirstAyahIdOnPage(mushafId: Int, pageNo: Int): Int? {
        if (mushafId <= 0 || pageNo <= 0) return null
        return mushafDao.getFirstAyahIdOnPage(mushafId, pageNo)
    }

    override suspend fun getChapterVerseCount(chapterNo: Int): Int {
        if (!QuranMeta.isChapterValid(chapterNo)) return 0

        return surahDao.getSurah(chapterNo)?.ayahCount ?: 0
    }

    override suspend fun isVerseValid4Chapter(chapterNo: Int, verseNo: Int): Boolean {
        return getSurah(chapterNo)?.isVerseValid(verseNo) == true
    }
}

private fun List<Int>.isContiguousRange(): Boolean {
    if (size <= 1) return true
    for (i in 1 until size) {
        if (this[i] != this[i - 1] + 1) return false
    }
    return true
}

private fun mergeAyahIdIntervals(intervals: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
    if (intervals.isEmpty()) return emptyList()
    val sorted = intervals.sortedBy { it.first }
    val out = mutableListOf<Pair<Int, Int>>()
    var cs = sorted[0].first
    var ce = sorted[0].second
    for (i in 1 until sorted.size) {
        val (s, e) = sorted[i]
        if (s <= ce) {
            ce = maxOf(ce, e)
        } else {
            out.add(cs to ce)
            cs = s
            ce = e
        }
    }
    out.add(cs to ce)
    return out
}
