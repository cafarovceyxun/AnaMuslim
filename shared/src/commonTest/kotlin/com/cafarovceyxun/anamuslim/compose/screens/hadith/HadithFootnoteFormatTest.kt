package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Hədis 298 (Kişi və qadınların bir qabdan dəstəmaz alması babı, telefondakı bazadan, 2026-11-15):
 * cümlə sonundakı `". (1).` dipnotdur və nişan olmalıdır. Mətn 00:58 nüsxəsindən olduğu kimi götürülüb.
 */
class HadithFootnoteFormatTest {
    private val arabic = "١١٧-مَالِكٌ،عَنْ نَافِعٍ،أَنَّ عَبْدَ اللَّهِ بْنَ عُمَرَ كَانَ يَقُولُ: \r\n\"إِنْ كَانَ الرِّجَالُ وَالنِّسَاءُ فِي زَمَانِ رَسُولِ اللَّهِ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ لَيَتَوَضَّئُونَ جَمِيعًا\". \r\n(مالك-٤٨). \r\n\r\nوفي رواية: \r\n-حَدَّثَنَا إِسْمَاعِيلُ،أَخْبَرَنَا أَيُّوبُ،عَنْ نَافِعٍ،عَنِ ابْنِ عُمَرَ،قَالَ: \r\nرَأَيْتُ الرِّجَالَ وَالنِّسَاءَ يَتَوَضَّئُونَ عَلَى عَهْدِ رَسُولِ اللَّهِ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ جَمِيعًا مِنْ إِنَاءٍ وَاحِدٍ\". \r\n(أحمد-٤٤٨١). \r\n\r\nوفي رواية: \r\n-حَدَّثَنَا عَبْدُ اللَّهِ بْنُ يُوسُفَ،قَالَ: أَخْبَرَنَا مَالِكٌ،عَنْ نَافِعٍ،عَنْ عَبْدِ اللَّهِ بْنِ عُمَرَ،أَنَّهُ قَالَ: \r\n\"كَانَ الرِّجَالُ وَالنِّسَاءُ يَتَوَضَّئُونَ فِي زَمَانِ رَسُولِ اللَّهِ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ جَمِيعًا\". \r\n(البخاري-١٩٣). \r\n\r\nوفي رواية: \r\n-حَدَّثَنَا مُسَدَّدٌ،حَدَّثَنَا يَحْيَى،عَنْ عُبَيْدِ اللَّهِ،حَدَّثَنِي نَافِعٌ،عَنْ عَبْدِ اللَّهِ بْنِ عُمَرَ،قَالَ: \r\n\"كُنَّا نَتَوَضَّأُ نَحْنُ وَالنِّسَاءُ عَلَى عَهْدِ رَسُولِ اللَّهِ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ مِنْ إِنَاءٍ وَاحِدٍ،نُدْلِي فِيهِ أَيْدِيَنَا\". \r\n(أبو داود-٧٩). \r\nأخرجه مالك،وأحمد،والبخاري،وأبو داود،والنسائي،وابن ماجة،وابن خزيمة. \r"
    private val translation = "117. Məlik, Nəfi'dən: Abdullah ibn Ömər (belə) deyərdi: \r\n\"Həqiqətən də, heç şübhəsiz ki, Rəsulullah sallallahu aleyhi və səlləmin zamanında kişilər və qadınlar birlikdə dəstəmaz alırdılar\". \r\n(Məlik-58). \r\n\r\nDigər bir rəvayətdə: \r\n-Bizə İsmayıl danışdı, bizə Əyyub xəbər verdi, Nəfi'dən, Abdullah bin Ömərdən, dedi: \r\n\"Rəsulullah sallallahu aleyhi və səlləmin dövründə kişilərlə qadınların bir (qabdan) dəstəmaz aldıqlarını gördüm\". (1). \r\n(Əhməd-4481). \r\n\r\nBaşqa bir rəvayətdə: \r\n-Bizə Abdullah ibn Yusuf danışdı, dedi: bizə Məlik xəbər verdi, Nəfi'dən, Abdullah bin Ömərdən, o, dedi: \r\n\"Rəsulullah sallallahu aleyhi və səlləmin zamanında kişilər və qadınlar hamısı (bir qabdan) dəstəmaz alırdılar\". \r\n(Buxari-193). \r\n\r\nDigər bir rəvayətdə: \r\n-Bizə Müsəddəd danışdı, bizə Yəhya danışdı, Ubeydullahdan, mənə Nəfi' danışdı, Abdullah ibn Ömərdən, dedi: \r\n\"Rəsulullah sallallahu aleyhi və səlləmin dövründə biz və qadınlar bir qabdan dəstəmaz alırdıq, o qabın içinə əllərimizi salırdıq\". \r\n(Əbu Davud-79). \r"

    /** Nişan inline content-dir: mətndə yer simvolu (U+FFFC) dayanır, rəqəm alternativ mətndədir. */
    private fun badges(text: androidx.compose.ui.text.AnnotatedString): List<String> =
        text.getStringAnnotations(0, text.length)
            .filter { it.tag == "androidx.compose.foundation.text.inlineContent" }
            .map { it.item }

    @Test
    fun sentenceEndFootnoteBecomesBadge() {
        val shown = formatHadithText(translation, NOTE, true, true, Color.Red)
        assertEquals(listOf("footnote:1"), badges(shown))
        assertTrue("(1)" !in shown.text, shown.text.substringAfter("gördüm").take(20))
    }

    @Test
    fun badgeSurvivesHiddenParentheses() {
        val shown = formatHadithText(translation, NOTE, false, true, Color.Red)
        assertEquals(listOf("footnote:1"), badges(shown))
    }

    @Test
    fun narrationPiecesCarryTheBadge() {
        val pieces = HadithExcerpt.pairedNarrations(arabic, translation) ?: return
        val all = pieces.flatMap { badges(formatHadithText(it.second, NOTE, true, true, Color.Red)) }
        assertEquals(listOf("footnote:1"), all)
    }

    @Test
    fun everyParenthesizedNumberIsAFootnote() {
        val shown = formatHadithText("dedi: \"... ağacdır\"} (1). Sonra (Abdullah) (2), ayə (Duha, 1-3).", null, true, true, Color.Red)
        assertEquals(listOf("footnote:1", "footnote:2"), badges(shown))
        assertTrue("(Duha, 1-3)" in shown.text)
        assertTrue("(Abdullah)" in shown.text)
    }

    /** 2026-11-15 redaktəsindən sonrakı forma: mötərizə silinib, nömrə nöqtədən sonra tək qalıb. */
    @Test
    fun bareNumberAfterSentenceIsBadgeWhenNoteHasIt() {
        val edited = translation.replace("gördüm\". (1).", "gördüm\". 1.")
        assertEquals(listOf("footnote:1"), badges(formatHadithText(edited, NOTE, true, true, Color.Red)))
    }

    @Test
    fun bareNumberWithoutMatchingNoteStaysText() {
        val shown = formatHadithText("Dedi: \"Gəldim\". 2. bənd. Sonra 40 gün.", NOTE, true, true, Color.Red)
        assertEquals(emptyList(), badges(shown))
        assertTrue("\". 2. bənd" in shown.text)
        assertEquals(emptyList(), badges(formatHadithText("gördüm\". 1.", null, true, true, Color.Red)))
    }

    @Test
    fun noteNumbersAreParsedFromLineStarts() {
        assertEquals(
            setOf(1, 2, 3),
            com.cafarovceyxun.anamuslim.utils.reader.hadithNoteNumbers("Qeydlər: \r\n1: Həntəm \r\n2- Dubba\n(3) Nəqir"),
        )
        assertEquals(emptySet(), com.cafarovceyxun.anamuslim.utils.reader.hadithNoteNumbers("Faydalar: 3 nəfər"))
    }

    private companion object {
        const val NOTE = "1: Eyni qabdan, müxtəlif vaxtlarda, bir-birilərinə qarışmadan. "
    }
}
