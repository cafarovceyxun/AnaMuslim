package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Çoxhissəli dua qaralamasının yoxlaması və mənbə müqayisəsi.
 *
 * Səhv burada ekranda sınmır: boş hissə bazaya boş sətir kimi gedər, eyni ərəbcəli iki hissə isə
 * unikal indeksə dəyib **bütün** duanı geri aldırar — istifadəçi səbəbini bilmədən.
 */
class ExcerptPickerPartsTest {

    @Test
    fun blankPartsAreDropped_andNumbersFollowTheScreen() {
        val result = validateParts(
            listOf(
                DuaPartDraft(arabic = "سُبْحَانَ اللَّهِ", countText = "33"),
                DuaPartDraft(),
                DuaPartDraft(arabic = "الْحَمْدُ لِلَّهِ", countText = "33"),
            ),
        )

        assertNull(result.problem)
        assertEquals(2, result.kept.size)
        assertEquals(listOf(33, 33), result.kept.map { it.repeatCount })
    }

    @Test
    fun allBlank_isEmpty() {
        assertEquals(PartsProblem.Empty, validateParts(listOf(DuaPartDraft(), DuaPartDraft())).problem)
    }

    @Test
    fun missingArabic_pointsToTheScreenNumber() {
        val result = validateParts(
            listOf(
                DuaPartDraft(arabic = "سُبْحَانَ اللَّهِ"),
                DuaPartDraft(),
                // Oxunuşu var, ərəbcəsi yox — üçüncü hissə (boş ikinci atılsa da nömrə dəyişmir).
                DuaPartDraft(transliteration = "Əlhəmdulillah"),
            ),
        )

        assertEquals(PartsProblem.MissingArabic(3), result.problem)
    }

    @Test
    fun duplicateArabic_ignoresHarakat() {
        val result = validateParts(
            listOf(
                DuaPartDraft(arabic = "سُبْحَانَ اللَّهِ"),
                DuaPartDraft(arabic = "الله أكبر"),
                DuaPartDraft(arabic = "سبحان الله"),
            ),
        )

        assertEquals(PartsProblem.DuplicateArabic(1, 3), result.problem)
    }

    @Test
    fun countText_zeroOrBlankMeansNoCount() {
        assertNull(DuaPartDraft(countText = "").repeatCount)
        assertNull(DuaPartDraft(countText = "0").repeatCount)
        assertEquals(1, DuaPartDraft(countText = "1").repeatCount)
    }

    @Test
    fun toDua_trimsAndKeepsSource() {
        val data = ExcerptSourceData(
            sourceType = DuaSourceType.HADITH,
            hadithId = 368,
            fullArabic = "",
            fullTranslation = "",
            reference = "Əhməd 803",
        )

        val row = DuaPartDraft(arabic = " ع ", transliteration = "  ", translation = " t ", countText = "34")
            .toDua(data)

        assertEquals("ع", row.text_ar)
        assertEquals("t", row.text_az)
        assertNull(row.transliteration)
        assertEquals(34, row.repeat_count)
        assertEquals(368L, row.hadith_id)
        assertEquals("Əhməd 803", row.source)
    }

    @Test
    fun isSourceOf_matchesHadithByIdAndVerseByChapterAndNumber() {
        val hadith = ExcerptSourceData(
            sourceType = DuaSourceType.HADITH,
            hadithId = 368,
            fullArabic = "",
            fullTranslation = "",
        )
        val verse = ExcerptSourceData(
            sourceType = DuaSourceType.QURAN,
            chapterNo = 2,
            verseNo = 201,
            verseEnd = 202,
            fullArabic = "",
            fullTranslation = "",
        )

        fun row(type: String, hadithId: Long? = null, chapter: Int? = null, verse: Int? = null) = Dua(
            category_slug = "x",
            source_type = type,
            hadith_id = hadithId,
            chapter_no = chapter,
            verse_no = verse,
            text_ar = "ع",
            text_az = "",
        )

        assertTrue(hadith.isSourceOf(row(DuaSourceType.HADITH, hadithId = 368)))
        assertFalse(hadith.isSourceOf(row(DuaSourceType.HADITH, hadithId = 369)))
        assertTrue(verse.isSourceOf(row(DuaSourceType.QURAN, chapter = 2, verse = 201)))
        assertFalse(verse.isSourceOf(row(DuaSourceType.QURAN, chapter = 2, verse = 200)))
        assertFalse(verse.isSourceOf(row(DuaSourceType.HADITH, hadithId = 368)))
    }

    @Test
    fun firstWords_addsEllipsisOnlyWhenCut() {
        assertEquals("Vaccəhtu vachiyə lilləzii…", "Vaccəhtu vachiyə lilləzii fətaras".firstWords(3))
        assertEquals("Allahu əkbər", " Allahu  əkbər ".firstWords(3))
    }
}
