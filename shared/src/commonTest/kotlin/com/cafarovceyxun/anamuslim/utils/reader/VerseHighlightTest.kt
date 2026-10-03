package com.cafarovceyxun.anamuslim.utils.reader

import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sürətli baxış vərəqində ərəbcə vurğunun söz xəritəsi ([wordIndexesIn]): uyğunluq simvol aralığı
 * kimi tapılır və ona toxunan sözlərə köçürülür.
 */
class VerseHighlightTest {

    // Fatihə 1:1 — uthmani yazısındakı kimi, sonuncu söz ayə nömrəsidir.
    private val basmala = listOf("بِسْمِ", "ٱللَّهِ", "ٱلرَّحْمَٰنِ", "ٱلرَّحِيمِ", "١")
        .mapIndexed { i, text -> AyahWordEntity(ayahId = 1, scriptId = 1, wordIndex = i + 1, text = text) }

    @Test
    fun `arabic query marks the word without harakat`() {
        assertEquals(setOf(4), VerseHighlight.Query("الرحيم").wordIndexesIn(basmala))
    }

    @Test
    fun `latin query marks nothing in arabic text`() {
        assertTrue(VerseHighlight.Query("rahim").wordIndexesIn(basmala).isEmpty())
    }

    @Test
    fun `excerpt marks every word it spans`() {
        assertEquals(setOf(3, 4), VerseHighlight.Excerpt("الرحمن الرحيم").wordIndexesIn(basmala))
    }

    @Test
    fun `asma name marks the name as a whole word`() {
        // «ٱلرَّحْمَٰنِ» «الرحيم»-i ehtiva etmir — tam söz qaydası yalnız dördüncü sözü seçir.
        assertEquals(setOf(4), VerseHighlight.Name("الرَّحِيمُ").wordIndexesIn(basmala))
    }

    @Test
    fun `missing excerpt marks nothing`() {
        assertTrue(VerseHighlight.Excerpt("الحمد لله").wordIndexesIn(basmala).isEmpty())
    }

    @Test
    fun `no words no highlight`() {
        assertTrue(VerseHighlight.Query("الرحيم").wordIndexesIn(emptyList()).isEmpty())
    }
}
