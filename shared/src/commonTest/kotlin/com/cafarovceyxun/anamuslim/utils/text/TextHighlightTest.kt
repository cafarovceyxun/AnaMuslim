package com.cafarovceyxun.anamuslim.utils.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Axtarış vurğusunun **ofsetləri** — həm nəticə önizləməsi, həm də oxucudakı sarı işarə bundan çıxır.
 *
 * Burada yoxlanan şey gözlə tutulmur: vurğu bir-iki simvol sürüşəndə ekranda yenə «sarı bir şey»
 * görünür, sadəcə səhv hərflərin üstündə. Ona görə hər halda **hansı** parçanın işarələndiyi
 * sətirdən kəsilib müqayisə olunur.
 */
class TextHighlightTest {

    private fun highlighted(text: String, query: String): List<String> =
        searchMatchRanges(text, query).map { text.substring(it.first, it.last + 1) }

    @Test
    fun findsTheWordRegardlessOfCase() {
        assertEquals(listOf("Namaz"), highlighted("Namaz dinin dirəyidir", "namaz"))
        assertEquals(listOf("namaz"), highlighted("Möminin namazı qəbuldur", "NAMAZ"))
    }

    @Test
    fun azerbaijaniDottedCapitalDoesNotShiftTheOffsets() {
        // «İ» sətir səviyyəsində kiçildiləndə iki simvola açılır; cədvəl simvol-simvol qurulmasa,
        // ondan sonrakı hər uyğunluq bir simvol sürüşərdi.
        val text = "İman gətirənlər namaz qılır"

        assertEquals(listOf("İman"), highlighted(text, "iman"))
        assertEquals(listOf("namaz"), highlighted(text, "namaz"))
    }

    @Test
    fun matchesArabicWithoutItsHarakat() {
        // Sorğu hərəkəsizdir, mətn isə tam hərəkəlidir — uyğunluq yastılanmış nüsxədə tapılır,
        // qaytarılan aralıq isə orijinalın (hərəkələrlə birlikdə) parçasıdır.
        val text = "قَالَ رَسُولُ اللَّهِ"
        val ranges = searchMatchRanges(text, "رسول")

        assertEquals(1, ranges.size)
        val matched = text.substring(ranges[0].first, ranges[0].last + 1)
        assertTrue(matched.startsWith("رَ"), "gözlənilən parça «رَسُولُ», alınan «$matched»")
        assertTrue(matched.endsWith("ل") || matched.endsWith("لُ"), "alınan «$matched»")
    }

    @Test
    fun ignoresTokensShorterThanTwoCharacters() {
        // Tək hərflik söz demək olar hər sətirdə keçir — səhifəni bütöv sarıya boyayardı.
        assertEquals(emptyList(), highlighted("Namaz dinin dirəyidir", "a"))
    }

    @Test
    fun eachOccurrenceIsMarkedSeparately() {
        // Sorğunun iki sözü ayrı-ayrı axtarılır və hərəsi öz yerində işarələnir — aralarındakı
        // boşluq sarıya düşmür.
        assertEquals(listOf("namaz", "namaz"), highlighted("namaz vaxtı namaz", "namaz"))
        assertEquals(
            listOf("qiyamət", "günü"),
            highlighted("qiyamət günü", "qiyamət günü"),
        )
    }

    @Test
    fun mergesOverlappingMatches() {
        // Üst-üstə düşən iki uyğunluq bir zolaqdır: ayrı-ayrı fon ləkələri eyni hərfləri iki dəfə
        // boyayıb qatı bir ləkə verərdi.
        val text = "salamlar"
        val ranges = searchMatchRanges(text, "salam lamlar")

        assertEquals(1, ranges.size)
        assertEquals("salamlar", text.substring(ranges[0].first, ranges[0].last + 1))
    }

    @Test
    fun emptyQueryLeavesTheTextUntouched() {
        val text = AnnotatedString("Namaz dinin dirəyidir")

        assertEquals(text, text.withSearchHighlight(null))
        assertEquals(text, text.withSearchHighlight("   "))
        // Tapılmayan söz də mətni dəyişmir — boş `buildAnnotatedString` nüsxəsi qaytarmır.
        assertEquals(text, text.withSearchHighlight("oruc"))
    }

    @Test
    fun keepsTheTextsOwnStylesUnderTheHighlight() {
        // Mötərizə rəngi hədis mətnində onsuz da var; vurğu onun üstünə əlavə olunur, yerinə yox.
        val parenColor = Color(0xFFE53935)
        val source = buildAnnotatedString {
            append("Namaz ")
            withStyle(SpanStyle(color = parenColor)) { append("(fərz)") }
            append(" qılın")
        }

        val result = source.withSearchHighlight("fərz")

        assertEquals(source.text, result.text)
        assertTrue(
            result.spanStyles.any { it.item.color == parenColor },
            "mətnin öz üslubu itib",
        )

        val highlight = result.spanStyles.single { it.item.background == TextHighlightYellow }
        assertEquals("fərz", result.text.substring(highlight.start, highlight.end))
    }
}
