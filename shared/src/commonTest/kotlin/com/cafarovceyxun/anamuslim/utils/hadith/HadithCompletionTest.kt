package com.cafarovceyxun.anamuslim.utils.hadith

import com.cafarovceyxun.anamuslim.utils.hadith.HadithCompletion.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ✓ nişanının yuxarı səviyyələrə yığılması.
 *
 * Bazada yalnız yarpaqlar var, qalan hər şey buradan çıxır — ona görə səhv burada olsa istifadəçi
 * oxumadığı kitabda ✓ görər, kompilyator və ekran isə susar.
 */
class HadithCompletionTest {

    // c1 → s1, s2 (alt-bablı bab) · c2 (alt-babsız bab) — ikisi də book_1-də
    private val books = listOf(Node("book_1", "vol_1"), Node("book_2", "vol_1"))
    private val chapters = listOf(
        Node("c1", "book_1"),
        Node("c2", "book_1"),
        Node("c3", "book_2"),
    )
    private val subChapters = listOf(Node("s1", "c1"), Node("s2", "c1"))

    private fun build(vararg leaves: String) =
        HadithCompletion.build(leaves.toSet(), books, chapters, subChapters)

    @Test
    fun aChapterWithSubChaptersNeedsAllOfThem() {
        val partial = build("s1")

        assertTrue(partial.isNodeCompleted("s1"))
        assertFalse(partial.isChapterCompleted("c1"))

        val full = build("s1", "s2")

        assertTrue(full.isChapterCompleted("c1"))
    }

    /** Alt-babı olmayan babın yarpağı öz slug-ıdır — alt-bab gözləsək belə bab heç vaxt bitməzdi. */
    @Test
    fun aChapterWithoutSubChaptersIsItsOwnLeaf() {
        assertTrue(build("c2").isChapterCompleted("c2"))
    }

    @Test
    fun aBookNeedsEveryChapterAndAVolumeEveryBook() {
        val bookOnly = build("s1", "s2", "c2")

        assertTrue(bookOnly.isBookCompleted("book_1"))
        // İkinci kitab hələ oxunmayıb — cild bitmiş sayılmır.
        assertFalse(bookOnly.isBookCompleted("book_2"))
        assertFalse(bookOnly.isVolumeCompleted("vol_1"))

        val everything = build("s1", "s2", "c2", "c3")

        assertTrue(everything.isVolumeCompleted("vol_1"))
    }

    /**
     * Uşağı olmayan valideyn **bitmiş sayılmır**.
     *
     * `all {}` boş siyahıda `true` verir: bu qorunmasaydı hələ məzmun yazılmamış kitab siyahıda
     * ✓ ilə görünərdi.
     */
    @Test
    fun anEmptyParentIsNotCompleted() {
        val completion = HadithCompletion.build(
            completedLeaves = setOf("c3"),
            books = listOf(Node("book_3", "vol_2")),
            chapters = emptyList(),
            subChapters = emptyList(),
        )

        assertFalse(completion.isBookCompleted("book_3"))
        assertFalse(completion.isVolumeCompleted("vol_2"))
    }

    /** Heç nə oxunmayıbsa ağac ümumiyyətlə qurulmur — siyahılar da nişansız çəkilir. */
    @Test
    fun nothingReadMeansNothingMarked() {
        val completion = build()

        assertEquals(HadithCompletion.EMPTY, completion)
        assertFalse(completion.isNodeCompleted("s1"))
        assertFalse(completion.isChapterCompleted(null))
    }
}
