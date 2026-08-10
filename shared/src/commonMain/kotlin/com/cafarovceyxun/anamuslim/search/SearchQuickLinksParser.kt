package com.cafarovceyxun.anamuslim.search

import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.univ.RegexPattern

sealed class QuickLinkItem {
    data class Chapter(val surah: SurahWithLocalizations) : QuickLinkItem()
    data class Verse(val chapterNo: Int, val verseNo: Int) : QuickLinkItem()
    data class Juz(val juzNo: Int) : QuickLinkItem()
    data class Hizb(val hizbNo: Int) : QuickLinkItem()
}

object SearchQuickLinksParser {

    private suspend fun chapterQuickLink(
        repository: QuranRepository,
        chapterNo: Int,
    ): QuickLinkItem.Chapter? =
        repository.getSurahWithLocalizations(chapterNo)?.let { QuickLinkItem.Chapter(it) }

    /**
     * Parses quick navigation patterns from the search query (verse refs, chapter/juz numbers,
     * and surah name/alias substring matches.
     */
    suspend fun parse(repository: QuranRepository, rawQuery: String): List<QuickLinkItem> {
        val q = rawQuery.trim()
        if (q.isEmpty()) return emptyList()

        val out = ArrayList<QuickLinkItem>()
        val mtchrVRangeJump = RegexPattern.VERSE_RANGE_JUMP_PATTERN.find(q)
        val mtchrVJump = RegexPattern.VERSE_JUMP_PATTERN.find(q)
        val mtchrChapOrJuzNo = RegexPattern.CHAPTER_OR_JUZ_PATTERN.find(q)

        if (mtchrVRangeJump != null) {
            val r = mtchrVRangeJump

            if (r.groupValues.size >= 4) {
                val chapNo = r.groupValues[1].toInt()

                if (!QuranMeta.isChapterValid(chapNo)) {
                    return out
                }

                var fromVerse = r.groupValues[2].toInt()
                var toVerse = r.groupValues[3].toInt()

                if (fromVerse > toVerse) {
                    val tmp = fromVerse
                    fromVerse = toVerse
                    toVerse = tmp
                }

                if (repository.isVerseValid4Chapter(chapNo, fromVerse)) {
                    out += QuickLinkItem.Verse(chapNo, fromVerse)
                }

                val isFromValid = repository.isVerseValid4Chapter(chapNo, fromVerse)
                val isToValid = repository.isVerseValid4Chapter(chapNo, toVerse)

                if (isFromValid) {
                    out += QuickLinkItem.Verse(chapNo, fromVerse)
                }

                if (isToValid && toVerse != fromVerse) {
                    out += QuickLinkItem.Verse(chapNo, toVerse)
                }

                chapterQuickLink(repository, chapNo)?.let { out += it }
            }
        } else if (mtchrVJump != null) {
            val r = mtchrVJump

            if (r.groupValues.size >= 3) {
                val chapNo = r.groupValues[1].toInt()
                val verseNo = r.groupValues[2].toInt()

                if (!QuranMeta.isChapterValid(chapNo)) {
                    return out
                }

                if (repository.isVerseValid4Chapter(chapNo, verseNo)) {
                    out += QuickLinkItem.Verse(chapNo, verseNo)
                }

                chapterQuickLink(repository, chapNo)?.let { out += it }
            }
        } else if (mtchrChapOrJuzNo != null) {
            val r = mtchrChapOrJuzNo

            if (r.groupValues.size >= 2) {
                val chapOrJuz = r.groupValues[1].toInt()

                if (QuranMeta.isChapterValid(chapOrJuz)) {
                    chapterQuickLink(repository, chapOrJuz)?.let { out += it }
                }

                if (QuranMeta.isJuzValid(chapOrJuz)) {
                    out += QuickLinkItem.Juz(chapOrJuz)
                }

                if (QuranMeta.isHizbValid(chapOrJuz)) {
                    out += QuickLinkItem.Hizb(chapOrJuz)
                }
            }
        }

        return out.distinctBy { it.stableKey() }
    }
}

fun QuickLinkItem.stableKey(): String =
    when (this) {
        is QuickLinkItem.Chapter -> "c${surah.surah.surahNo}"
        is QuickLinkItem.Juz -> "j$juzNo"
        is QuickLinkItem.Hizb -> "h$hizbNo"
        is QuickLinkItem.Verse -> "v$chapterNo:$verseNo"
    }
