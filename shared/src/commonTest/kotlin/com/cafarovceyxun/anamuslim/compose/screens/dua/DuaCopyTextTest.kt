package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Paylaşılan/kopyalanan mətnin quruluşu.
 *
 * Səhv nəticə ekranda görünmür — istifadəçi onu yalnız yapışdırandan **sonra** görür, ona görə hər
 * qayda burada bağlanır.
 */
class DuaCopyTextTest {

    private fun dua(
        textAr: String = "ع",
        translit: String? = "oxunuş",
        textAz: String = "tərcümə",
        note: String? = "qeyd",
        source: String? = "Buxari",
    ) = Dua(
        id = 1,
        category_slug = "a",
        source_type = "hadith",
        text_ar = textAr,
        text_az = textAz,
        transliteration = translit,
        note = note,
        source = source,
    )

    @Test
    fun blocksAreSeparatedByBlankLinesAndSourceIsPrefixed() {
        assertEquals(
            "ع\n\noxunuş\n\ntərcümə\n\nqeyd\n\n— Buxari",
            buildDuaShareText(dua()),
        )
    }

    @Test
    fun unselectedBlocksAreDropped() {
        val onlyArabic = buildDuaShareText(
            dua(),
            DuaShareParts(arabic = true, transliteration = false, translation = false, note = false, source = false),
        )

        assertEquals("ع", onlyArabic)
    }

    /** Seçilmiş, amma boş blok izahsız boş sətir buraxardı. */
    @Test
    fun selectedButEmptyBlocksAreDropped() {
        val text = buildDuaShareText(dua(translit = null, note = "  "))

        assertEquals("ع\n\ntərcümə\n\n— Buxari", text)
    }

    @Test
    fun everythingUnselectedGivesAnEmptyString() {
        val parts = DuaShareParts(
            arabic = false,
            transliteration = false,
            translation = false,
            note = false,
            source = false,
        )

        assertTrue(parts.isEmpty)
        assertEquals("", buildDuaShareText(dua(), parts))
    }

    /** Uzun basıb kopyalayan adam **ekranda gördüyünü** gözləyir. */
    @Test
    fun visiblePartsFollowTheScreen() {
        val arabicOnly = DuaShareParts.visible(
            DuaBlockVisibility(arabic = true, transliteration = false, translation = false),
        )

        assertEquals("ع", buildDuaShareText(dua(), arabicOnly))
        assertFalse(arabicOnly.isEmpty)
    }

    /** Qeyd və mənbə tərcümənin davamıdır — «Ərəbcə» rejimində onlar da getməlidir. */
    @Test
    fun noteAndSourceFollowTheTranslationVisibility() {
        val parts = DuaShareParts.visible(
            DuaBlockVisibility(arabic = true, transliteration = true, translation = false),
        )

        assertFalse(parts.note)
        assertFalse(parts.source)
        assertEquals("ع\n\noxunuş", buildDuaShareText(dua(), parts))
    }
}
