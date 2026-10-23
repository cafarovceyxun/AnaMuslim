package com.cafarovceyxun.anamuslim.utils.reader

import kotlin.test.Test
import kotlin.test.assertEquals

/** Haşiyə nömrələrinin üst-indeks seçimi — nümunələr azərbaycanca tərcümədən götürülüb. */
class FootnoteMarkerTest {
    private fun markers(text: String): List<String> =
        findFootnoteMarkers(normalizeFootnoteMarkers(text)).map { normalizeFootnoteMarkers(text).substring(it) }

    @Test
    fun digitGluedToWordIsMarker() {
        assertEquals(listOf("1"), markers("onlar yalnız parçalanma1 (ixtilaf) içində olarlar"))
        assertEquals(listOf("4", "5", "6"), markers("tabe olunanlar4 əzabı tabe olanlardan5 uzaqlaşacaqlar6 və"))
    }

    @Test
    fun digitAfterPunctuationIsMarker() {
        assertEquals(listOf("1"), markers("vəfa edin!1 (Haram"))
        assertEquals(listOf("1"), markers("qısqanclıq (həsəd)1 ucbatından"))
        assertEquals(listOf("6"), markers("uzaqlaşardıq!6” Beləcə"))
        assertEquals(listOf("3"), markers("“ham”3 kimi"))
    }

    @Test
    fun parenthesizedNumberIsGluedAndMarked() {
        assertEquals("təqdis edirik1”.", normalizeFootnoteMarkers("təqdis edirik (1)”."))
        assertEquals(listOf("2"), markers("”Unzurnə”(2)-deyin"))
    }

    @Test
    fun ordinaryNumbersAreLeftAlone() {
        assertEquals(emptyList(), markers("-198: Rəbbinizdən"))
        assertEquals(emptyList(), markers("bax 2:255 və 40 gün, 1000 il"))
        assertEquals(emptyList(), markers("-5, 6: Həqiqətən"))
        assertEquals(emptyList(), markers("Sura2:255"))
        assertEquals(emptyList(), markers("söz123 burada"))
    }
}
