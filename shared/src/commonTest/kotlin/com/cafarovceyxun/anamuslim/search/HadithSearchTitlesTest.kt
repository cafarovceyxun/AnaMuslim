package com.cafarovceyxun.anamuslim.search

import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Hədis ekranlarındakı axtarış qutusunun və Axtarış ekranının başlıq uyğunluqları
 * ([HadithSearch.matchTitles]): sıra, limit, əcdadlar və ekranda artıq görünənlərin istisnası.
 */
class HadithSearchTitlesTest {

    private val volumes = listOf(
        HadithVolume("bukhari", "Səhih Buxari", "صحيح البخاري"),
        HadithVolume("iman-cild", "İman cildi"),
    )
    private val books = listOf(
        HadithBook("b-iman", "bukhari", 2, "İman kitabı", "كتاب الإيمان"),
        HadithBook("b-elm", "bukhari", 3, "Elm kitabı"),
    )
    private val chapters = listOf(
        HadithChapter("c-shaxe", "b-iman", 1, "İmanın şaxələri"),
        HadithChapter("c-niyyet", "b-elm", 1, "Niyyət"),
    )
    private val subChapters = listOf(
        HadithSubChapter("s-heya", "c-shaxe", 1, "Həya imandandır"),
    )

    private fun match(query: String, limit: Int = Int.MAX_VALUE, exclude: Set<String> = emptySet()) =
        HadithSearch.matchTitles(query, volumes, books, chapters, subChapters, limit, exclude)

    @Test
    fun `levels come in volume then book then chapter then sub-chapter order`() {
        val hits = match("iman")
        assertEquals(
            listOf(
                HadithTitleLevel.VOLUME,
                HadithTitleLevel.BOOK,
                HadithTitleLevel.CHAPTER,
                HadithTitleLevel.SUB_CHAPTER,
            ),
            hits.map { it.level },
        )
    }

    @Test
    fun `match ignores case across Unicode letters`() {
        // SQL LIKE «iman» ilə «İman»ı tutmurdu — süzgəc Kotlin tərəfindədir.
        assertEquals("İman cildi", match("İMAN").first().name)
    }

    @Test
    fun `hit carries every ancestor`() {
        val sub = match("həya").single()
        assertEquals("c-shaxe", sub.chapter?.slug)
        assertEquals("b-iman", sub.book?.slug)
        assertEquals("bukhari", sub.volume?.slug)
    }

    @Test
    fun `book hit has no chapter`() {
        val book = match("elm").single()
        assertEquals(HadithTitleLevel.BOOK, book.level)
        assertEquals("bukhari", book.volume?.slug)
        assertNull(book.chapter)
    }

    @Test
    fun `arabic name matches`() {
        val hit = match("الإيمان").single()
        assertEquals("b-iman", hit.book?.slug)
    }

    @Test
    fun `limit caps the result`() {
        assertEquals(2, match("iman", limit = 2).size)
    }

    @Test
    fun `excluded keys are dropped before the limit`() {
        // Ekranda artıq görünən sətir limiti yeməməlidir — yoxsa qlobal bölmə boş qalardı.
        val hits = match(
            "iman",
            limit = 2,
            exclude = setOf(
                hadithLevelKey(HadithTitleLevel.VOLUME, "iman-cild"),
                hadithLevelKey(HadithTitleLevel.BOOK, "b-iman"),
            ),
        )
        assertEquals(listOf("c-shaxe", "s-heya"), hits.map { it.subChapter?.slug ?: it.chapter?.slug })
    }

    @Test
    fun `keys are level-prefixed`() {
        assertEquals(
            setOf("v:iman-cild", "b:b-iman", "c:c-shaxe", "s:s-heya"),
            match("iman").map { it.key }.toSet(),
        )
    }

    @Test
    fun `blank query matches nothing`() {
        assertTrue(match("  ").isEmpty())
    }
}
