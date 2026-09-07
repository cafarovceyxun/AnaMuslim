package com.cafarovceyxun.anamuslim.utils.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextDiffTest {

    private fun TextDiffResult.oldParts(source: String) = oldSpans.map { source.substring(it.start, it.end) }
    private fun TextDiffResult.newParts(source: String) = newSpans.map { source.substring(it.start, it.end) }

    /** Boşluqdan başqa hər simvolun hansısa intervalın içində olduğunu yoxlayır. */
    private fun List<DiffSpan>.coversNonBlank(source: String) = source.indices.all { i ->
        source[i].isWhitespace() || any { i >= it.start && i < it.end }
    }

    @Test
    fun identicalTextHasNoSpans() {
        val result = diffText("Bismillah", "Bismillah")
        assertFalse(result.hasChanges)
        assertEquals(emptyList(), result.oldSpans)
        assertEquals(emptyList(), result.newSpans)
    }

    @Test
    fun bothEmptyIsIdentical() {
        assertFalse(diffText("", "").hasChanges)
    }

    @Test
    fun singleLetterChangeMarksOnlyThatLetter() {
        val old = "Rəhmli və Mərhəmətli"
        val new = "Rəhmli və Mərhamətli"
        val result = diffText(old, new)
        assertEquals(listOf("ə"), result.oldParts(old))
        assertEquals(listOf("a"), result.newParts(new))
    }

    @Test
    fun addedPunctuationMarksOnlyThePunctuation() {
        val old = "Aləmlərin Rəbbi Allaha həmd olsun"
        val new = "Aləmlərin Rəbbi Allaha həmd olsun."
        val result = diffText(old, new)
        assertEquals(emptyList(), result.oldParts(old))
        assertEquals(listOf("."), result.newParts(new))
    }

    @Test
    fun changedPunctuationInsideSentenceIsNarrowedToOneChar() {
        val old = "Ey iman gətirənlər, oruc tutun"
        val new = "Ey iman gətirənlər! oruc tutun"
        val result = diffText(old, new)
        assertEquals(listOf(","), result.oldParts(old))
        assertEquals(listOf("!"), result.newParts(new))
    }

    @Test
    fun changedDigitMarksOnlyTheDigit() {
        // İki simvol dəyişib (5→6 və i→ı) — hər ikisi ayrıca vurğulanmalıdır, «255-ci» bütöv yox.
        val old = "Bəqərə surəsi, 255-ci ayə"
        val new = "Bəqərə surəsi, 256-cı ayə"
        val result = diffText(old, new)
        assertEquals(listOf("5", "i"), result.oldParts(old))
        assertEquals(listOf("6", "ı"), result.newParts(new))
    }

    @Test
    fun insertedWordIsMarkedOnTheNewSideOnly() {
        val old = "Allah böyükdür"
        val new = "Allah çox böyükdür"
        val result = diffText(old, new)
        assertEquals(emptyList(), result.oldParts(old))
        assertEquals(listOf("çox"), result.newParts(new))
    }

    @Test
    fun deletedWordIsMarkedOnTheOldSideOnly() {
        val old = "Allah çox böyükdür"
        val new = "Allah böyükdür"
        val result = diffText(old, new)
        assertEquals(listOf("çox"), result.oldParts(old))
        assertEquals(emptyList(), result.newParts(new))
    }

    @Test
    fun replacedWordInTheMiddleIsMarkedOnBothSides() {
        // «gün» və «zaman» oxşamır (yalnız təsadüfi «n» ortaqdır) — söz bütöv boyanmalıdır,
        // «gü»/«zama» kimi parçalanmamalıdır.
        val old = "bir gün gələcək və hamı biləcək"
        val new = "bir zaman gələcək və hamı biləcək"
        val result = diffText(old, new)
        assertEquals(listOf("gün"), result.oldParts(old))
        assertEquals(listOf("zaman"), result.newParts(new))
    }

    @Test
    fun similarWordsAreNarrowedToTheChangedLetters() {
        // «görəcək» → «biləcək»: ortaq «əcək» saxlanır, yalnız dəyişən kök boyanır.
        val old = "hamı görəcək"
        val new = "hamı biləcək"
        val result = diffText(old, new)
        assertEquals(listOf("gör"), result.oldParts(old))
        assertEquals(listOf("bil"), result.newParts(new))
    }

    @Test
    fun twoSeparateChangesProduceTwoSpans() {
        val old = "bir gün gələcək və hamı görəcək"
        val new = "bir zaman gələcək və hamı biləcək"
        val result = diffText(old, new)
        assertEquals(listOf("gün", "gör"), result.oldParts(old))
        assertEquals(listOf("zaman", "bil"), result.newParts(new))
    }

    @Test
    fun completeReplacementMarksEveryNonBlankCharacter() {
        val old = "birinci mətn"
        val new = "tamam başqa cümlə"
        val result = diffText(old, new)
        assertTrue(result.oldSpans.coversNonBlank(old))
        assertTrue(result.newSpans.coversNonBlank(new))
    }

    @Test
    fun emptyOldMarksWholeNewText() {
        val new = "yeni tərcümə"
        val result = diffText("", new)
        assertEquals(emptyList(), result.oldSpans)
        assertEquals(listOf(new), result.newParts(new))
    }

    @Test
    fun emptyNewMarksWholeOldText() {
        val old = "köhnə tərcümə"
        val result = diffText(old, "")
        assertEquals(listOf(old), result.oldParts(old))
        assertEquals(emptyList(), result.newSpans)
    }

    @Test
    fun trailingWhitespaceChangeProducesNoVisibleSpan() {
        // Yalnız boşluq dəyişibsə vurğu boş qalmalıdır — sətrin sonunda sarı ləkə görünməsin.
        val result = diffText("mətn", "mətn  ")
        assertEquals(emptyList(), result.oldSpans)
        assertEquals(emptyList(), result.newSpans)
    }

    @Test
    fun arabicDiacriticChangeIsDetected() {
        val old = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val new = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ"
        val result = diffText(old, new)
        assertTrue(result.hasChanges)
        assertEquals(1, result.oldSpans.size)
        // Yalnız çıxarılan hərfaltı işarə qeyd olunmalıdır, bütöv söz yox.
        assertEquals("ٰ", result.oldParts(old).single())
        assertEquals(emptyList(), result.newParts(new))
    }

    @Test
    fun spansAreOrderedAndWithinBounds() {
        val old = "bir iki üç dörd beş altı yeddi"
        val new = "bir 2 üç dörd 5 altı yeddi"
        val result = diffText(old, new)
        assertTrue(result.oldSpans.zipWithNext().all { (a, b) -> a.end <= b.start })
        assertTrue(result.newSpans.zipWithNext().all { (a, b) -> a.end <= b.start })
        assertTrue(result.oldSpans.all { it.start >= 0 && it.end <= old.length && it.start < it.end })
        assertTrue(result.newSpans.all { it.start >= 0 && it.end <= new.length && it.start < it.end })
    }

    @Test
    fun singleWordChangeInALongTextStaysNarrow() {
        val old = (1..600).joinToString(" ") { "söz$it" }
        val new = (1..600).joinToString(" ") { if (it == 300) "dəyişdi" else "söz$it" }
        val result = diffText(old, new)
        assertEquals(listOf("söz300"), result.oldParts(old))
        assertEquals(listOf("dəyişdi"), result.newParts(new))
    }

    @Test
    fun veryLongDivergentTextFallsBackToASingleSpanWithoutFailing() {
        // Hər ikinci söz fərqlidir → orta hissə token həddini aşır, kobud rejimə düşür.
        val old = (1..600).joinToString(" ") { "söz$it" }
        val new = (1..600).joinToString(" ") { if (it % 2 == 0) "başqa$it" else "söz$it" }
        val result = diffText(old, new)
        assertTrue(result.hasChanges)
        assertEquals(1, result.oldSpans.size)
        assertEquals(1, result.newSpans.size)
    }
}
