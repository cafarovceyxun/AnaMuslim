package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import kotlin.test.Test
import kotlin.test.assertEquals

/** Paylaşılan düz mətndə haşiyə nömrələri Unicode üst-indeks olur — oxucunun üslubu ilə eyni qayda. */
class ShareFootnoteTest {
    @Test
    fun parenthesizedNumberBecomesSuperscript() {
        assertEquals(
            "-2: Bu, elə bir kitabdır ki, onda heç bir şəkk-şübhə yoxdur. O, muttəqilər¹ (Allahdan haqqı ilə qorxanlar) üçün hidayətdir.",
            footnoteMarkersAsSuperscript(
                "-2: Bu, elə bir kitabdır ki, onda heç bir şəkk-şübhə yoxdur. O, muttəqilər (1) (Allahdan haqqı ilə qorxanlar) üçün hidayətdir.",
            ),
        )
    }

    @Test
    fun gluedDigitsBecomeSuperscript() {
        assertEquals("parçalanma¹ (ixtilaf)", footnoteMarkersAsSuperscript("parçalanma1 (ixtilaf)"))
        assertEquals("vəfa edin!¹ (Haram", footnoteMarkersAsSuperscript("vəfa edin!1 (Haram"))
        assertEquals("olanlardan¹² uzaq", footnoteMarkersAsSuperscript("olanlardan12 uzaq"))
    }

    @Test
    fun prefixReferencesAndCountsUntouched() {
        val text = "-198: Bax 2:255, 40 gün."
        assertEquals(text, footnoteMarkersAsSuperscript(text))
    }
}
