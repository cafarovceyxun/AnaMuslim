package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** The language code the surahs' Arabic names are stored under. */
private const val ArabicLangCode = "ar"

/**
 * Fills in the verses a bulk paste only pointed at (`3:51§`), turning each into an ordinary entry
 * with the reference in the source field and the two texts in their own — exactly what the source
 * field's verse picker produces for a hand-added hadith, so an imported verse and a picked one are
 * the same row.
 *
 * Runs before the plan is built, so the preview shows the verse text the import will actually write
 * rather than the reference the user typed. A verse whose text the database has neither half of is
 * dropped and reported: writing a row with an empty Arabic and an empty translation would look like
 * a successful import and read as a blank entry in the book.
 *
 * The range is clamped to the surah's own length here — the parser only knows the longest surah in
 * the Quran, so `2:280-290` reaches this far and is trimmed to the verses that exist.
 */
internal suspend fun resolveBulkVerses(
    parsed: BulkParseResult,
    translationFactory: QuranTranslationFactory,
): BulkParseResult = withContext(Dispatchers.IO) {
    val verses = parsed.entries.filterIsInstance<BulkEntry.Verse>()
    if (verses.isEmpty()) return@withContext parsed

    val surahs = RepositoryProvider.quranRepository
        .getSurahsWithLocalizationsByChapterNos(verses.map { it.chapterNo }.distinct())

    val unavailable = mutableListOf<String>()

    val entries = parsed.entries.map { entry ->
        if (entry !is BulkEntry.Verse) return@map entry

        val surah = surahs[entry.chapterNo]
        if (surah == null) {
            unavailable += "${entry.chapterNo}:${entry.fromVerse}"
            return@map entry
        }

        val ayahCount = surah.surah.ayahCount
        val from = entry.fromVerse.coerceIn(1, ayahCount)
        val to = entry.toVerse.coerceIn(from, ayahCount)
        val reference = quranReference(surah.getCurrentName(), entry.chapterNo, from, to)

        val texts = loadVerseTexts(
            translationFactory = translationFactory,
            chapterNo = entry.chapterNo,
            fromVerse = from,
            toVerse = to,
            arabicChapterName = surah.localizations
                .firstOrNull { it.langCode == ArabicLangCode && !it.name.isNullOrBlank() }
                ?.name,
        )

        if (texts.arabic.isBlank() && texts.translation.isBlank()) {
            unavailable += reference
            return@map entry
        }

        BulkEntry.HadithText(
            textAr = texts.arabic,
            textAz = texts.translation,
            source = reference,
            note = "",
        )
    }

    BulkParseResult(
        entries = entries.filter { it !is BulkEntry.Verse },
        problems = if (unavailable.isEmpty()) {
            parsed.problems
        } else {
            parsed.problems + BulkProblem.VerseUnavailable(unavailable)
        },
    )
}
