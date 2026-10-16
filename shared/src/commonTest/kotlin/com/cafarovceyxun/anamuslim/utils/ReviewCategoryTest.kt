package com.cafarovceyxun.anamuslim.utils

import com.cafarovceyxun.anamuslim.repository.supabase.ReviewCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Səbəb mətnləri `tools/quran-audit/out/edits.json`-dakı real təkliflərdəndir. */
class ReviewCategoryTest {

    @Test
    fun lowercaseLabelIsItsOwnCategory() {
        assertEquals("mexaniki", ReviewCategory.of("• mexaniki: yetim dipnot rəqəmi silindi"))
        assertEquals("mötərizə", ReviewCategory.of("• mötərizə: mötərizəsiz feilsiz cümlə qalırdı"))
    }

    @Test
    fun compoundLabelKeepsFirstPart() {
        assertEquals("qrammatika", ReviewCategory.of("• qrammatika + durğu: cümlə yarıda kəsilir"))
        assertEquals("dırnaq", ReviewCategory.of("• dırnaq/mötərizə: bağlayan dırnaq yoxdur"))
    }

    @Test
    fun uppercaseLabelsAreSerious() {
        assertEquals(ReviewCategory.SERIOUS, ReviewCategory.of("• STRUKTUR (ciddi): 19:5-in mətni yalnız \"!\" işarəsidir"))
        assertEquals(ReviewCategory.SERIOUS, ReviewCategory.of("• MƏNA (kiçik): …"))
        assertEquals(ReviewCategory.SERIOUS, ReviewCategory.of("• MÖTƏRİZƏ: …"))
    }

    @Test
    fun firstBulletDecidesWhenThereAreSeveral() {
        assertEquals("mexaniki", ReviewCategory.of("• mexaniki: durğu işarəsindən əvvəl boşluq\n• mötərizə: yetim iki nöqtə"))
    }

    @Test
    fun noLabelMeansNoCategory() {
        assertNull(ReviewCategory.of(null))
        assertNull(ReviewCategory.of("səbəbsiz mətn"))
    }

    @Test
    fun displayUsesAzerbaijaniDottedCapital() {
        assertEquals("Mexaniki", ReviewCategory.display("mexaniki"))
        assertEquals("İmla", ReviewCategory.display("imla"))
    }
}
