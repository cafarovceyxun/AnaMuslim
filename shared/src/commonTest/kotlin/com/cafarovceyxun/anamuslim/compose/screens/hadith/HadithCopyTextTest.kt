package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Kartı basılı saxlayanda panoya düşən mətn — kartda **görünənin** eynisi olmalıdır.
 *
 * Jest bir anlıqdır: istifadəçi nəyi kopyaladığını yoxlaya bilmir, ona görə mətn ekrandakı ayarlara
 * (dil tabları, mötərizə, qaynaq) qəti tabe olmalıdır.
 */
class HadithCopyTextTest {

    private val hadith = Hadith(
        id = 1,
        hadith_no = 12,
        text_ar = "  حَدَّثَنَا  ",
        text_az = "Bizə rəvayət etdi (yəni nəql etdi) filankəs.",
        source = "Buxari 12",
        note = "Qeyd",
    )

    @Test
    fun `both languages and the source are joined in reading order`() {
        val text = buildHadithCopyText(
            hadith = hadith,
            includeArabic = true,
            includeTranslation = true,
            includeSource = true,
            showParentheses = true,
        )

        assertEquals(
            "حَدَّثَنَا\n\nBizə rəvayət etdi (yəni nəql etdi) filankəs.\n\nBuxari 12",
            text,
        )
    }

    @Test
    fun `a hidden tab is hidden in the clipboard too`() {
        val arabicOnly = buildHadithCopyText(
            hadith = hadith,
            includeArabic = true,
            includeTranslation = false,
            includeSource = false,
            showParentheses = true,
        )

        assertEquals("حَدَّثَنَا", arabicOnly)
    }

    @Test
    fun `parentheses follow the reader setting`() {
        val text = buildHadithCopyText(
            hadith = hadith,
            includeArabic = false,
            includeTranslation = true,
            includeSource = false,
            showParentheses = false,
        )

        // İzah atılır və arxasında ikiqat boşluq qalmır.
        assertEquals("Bizə rəvayət etdi filankəs.", text)
    }

    @Test
    fun `the note stays out of the clipboard`() {
        val text = buildHadithCopyText(
            hadith = hadith,
            includeArabic = true,
            includeTranslation = true,
            includeSource = true,
            showParentheses = true,
        )

        // Qeyd kartda yox, əməllər vərəqindədir — kopyalanan mətn kartın eynisi olmalıdır.
        assertTrue("Qeyd" !in text)
    }

    @Test
    fun `everything hidden gives an empty string rather than blank lines`() {
        val text = buildHadithCopyText(
            hadith = hadith,
            includeArabic = false,
            includeTranslation = false,
            includeSource = false,
            showParentheses = true,
        )

        // Çağıran tərəf buna baxıb panoya toxunmur — boş pano «kopyalandı» deməkdən yaxşıdır.
        assertEquals("", text)
    }
}
