package com.cafarovceyxun.anamuslim.compose.screens.hadith

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The bulk paste carries a whole book — babs, alt-babs, hadiths and verse references — and its rows
 * go into the database in the order they appear, each hadith inheriting the bab above it. A label
 * misread here does not fail loudly: it writes hundreds of rows under the wrong parent, so these
 * tests hold the grammar and the numbering down.
 */
class HadithBulkParserTest {

    private val book = "cbki1"

    @Test
    fun `bab alt bab and hadith come out in the order they were written`() {
        val result = parseHadithBulk(
            """
            a§ كتاب بدء الوحي
            q§ Vəhyin başlanğıcı
            aa§ باب كيف كان بدء الوحي
            qq§ Vəhyin necə başladığı
            1§ حدثنا الحميدي
            2§ Bizə rəvayət etdi
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(emptyList(), result.problems)
        assertEquals(
            listOf(
                BulkEntry.Chapter(name = "Vəhyin başlanğıcı", nameAr = "كتاب بدء الوحي"),
                BulkEntry.SubChapter(name = "Vəhyin necə başladığı", nameAr = "باب كيف كان بدء الوحي"),
                BulkEntry.HadithText(
                    textAr = "حدثنا الحميدي",
                    textAz = "Bizə rəvayət etdi",
                    source = "Buxari 1",
                    note = "",
                ),
            ),
            result.entries,
        )
    }

    /** Each `1§` cycle is its own hadith — the rule the single-hadith parser already follows. */
    @Test
    fun `a repeated label opens the next hadith`() {
        val result = parseHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص أول
            2§ birinci
            1§ نص ثان
            2§ ikinci
            """.trimIndent()
        )

        assertEquals(3, result.entries.size)
        assertEquals(2, result.hadithCount)
        assertEquals("ikinci", (result.entries[2] as BulkEntry.HadithText).textAz)
    }

    /** Two adjacent `3§` lines are one hadith with two sources, not two hadiths. */
    @Test
    fun `an adjacent duplicate label stays in the same hadith`() {
        val result = parseHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            3§ Buxari 1
            3§ Müslim 2
            """.trimIndent()
        )

        assertEquals(1, result.hadithCount)
        assertEquals(
            "Buxari 1\nMüslim 2",
            (result.entries.last() as BulkEntry.HadithText).source,
        )
    }

    @Test
    fun `an unlabelled line continues the block above it`() {
        val result = parseHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ birinci sətir
            ikinci sətir
            """.trimIndent()
        )

        assertEquals(
            "birinci sətir\nikinci sətir",
            (result.entries.last() as BulkEntry.HadithText).textAr,
        )
    }

    @Test
    fun `a verse reference becomes its own entry`() {
        val result = parseHadithBulk(
            """
            a§ باب
            q§ Bab
            3:51§
            5:13-15§
            """.trimIndent()
        )

        assertEquals(emptyList(), result.problems)
        assertEquals(
            listOf(
                BulkEntry.Verse(chapterNo = 3, fromVerse = 51, toVerse = 51),
                BulkEntry.Verse(chapterNo = 5, fromVerse = 13, toVerse = 15),
            ),
            result.entries.drop(1),
        )
    }

    /** The labels are typed on a Mac, where Arabic context autocorrects `3` into `٣`. */
    @Test
    fun `arabic-indic digits work in labels`() {
        val result = parseHadithBulk("a§ باب\nq§ Bab\n٣:٥١§\n١§ نص")

        assertEquals(BulkEntry.Verse(3, 51, 51), result.entries[1])
        assertEquals("نص", (result.entries[2] as BulkEntry.HadithText).textAr)
    }

    @Test
    fun `a reference pointing outside the quran is reported rather than written`() {
        val result = parseHadithBulk("a§ باب\nq§ Bab\n115:1§\n2:9-3§")

        val bad = result.problems.filterIsInstance<BulkProblem.BadVerseLabel>().single()
        assertEquals(listOf("115:1", "2:9-3"), bad.labels)
        assertEquals(0, result.verseCount)
    }

    /** `5§`–`9§` fill the volume/book editors' name fields; in a stream they mean nothing. */
    @Test
    fun `a named-editor label is reported instead of becoming text`() {
        val result = parseHadithBulk("a§ باب\nq§ Bab\n5§ Ad")

        assertEquals(
            listOf("5"),
            result.problems.filterIsInstance<BulkProblem.UnsupportedLabel>().single().labels,
        )
    }

    @Test
    fun `everything before the first bab is dropped and counted`() {
        val result = parseHadithBulk(
            """
            1§ نص
            2§ tərcümə
            a§ باب
            q§ Bab
            1§ نص ثان
            """.trimIndent()
        )

        assertEquals(1, result.problems.filterIsInstance<BulkProblem.Orphan>().single().count)
        assertTrue(result.entries.first() is BulkEntry.Chapter)
        assertEquals(1, result.hadithCount)
    }

    @Test
    fun `text before any label has nowhere to go and is counted`() {
        val result = parseHadithBulk("Buxarinin səhihi\n\na§ باب\nq§ Bab")

        assertEquals(1, result.problems.filterIsInstance<BulkProblem.DroppedLines>().single().count)
    }

    /** No labels at all is simply not this format — guessing would create a book of wrong rows. */
    @Test
    fun `an unlabelled paste yields nothing`() {
        val result = parseHadithBulk("باب في الوضوء\nDəstəmaz babı")

        assertTrue(result.isEmpty)
    }

    @Test
    fun `the plan numbers babs from the next free number and hadiths per container`() {
        val result = parseHadithBulk(
            """
            a§ باب
            q§ Vəhyin başlanğıcı
            1§ نص
            aa§ باب فرعي
            qq§ Necə başladı
            1§ نص ثان
            2§ ikinci
            1§ نص ثالث
            2§ üçüncü
            """.trimIndent()
        )

        val plan = buildBulkPlan(book, result.entries, firstChapterNo = 5)

        val chapter = (plan[0] as BulkRow.Chapter).row
        assertEquals(5, chapter.chapter_no)
        assertEquals("${book}ve5", chapter.slug)
        assertEquals(book, chapter.book_slug)

        // Bab birbaşa saxladığı hədis: alt bab yoxdur, nömrə 1-dən başlayır.
        val direct = (plan[1] as BulkRow.HadithRow).row
        assertEquals(1, direct.hadith_no)
        assertEquals(chapter.slug, direct.chapter_slug)
        assertNull(direct.sub_chapter_slug)

        val subChapter = (plan[2] as BulkRow.SubChapter).row
        assertEquals(1, subChapter.sub_chapter_no)
        assertEquals("${book}ve5ne1", subChapter.slug)
        assertEquals(chapter.slug, subChapter.chapter_slug)

        // Alt babın içində nömrə yenidən 1-dən sayılır — bazada nömrə konteyner başınadır.
        val inSub = plan.drop(3).map { (it as BulkRow.HadithRow).row }
        assertEquals(listOf(1, 2), inSub.map { it.hadith_no })
        assertEquals(listOf(subChapter.slug, subChapter.slug), inSub.map { it.sub_chapter_slug })
    }

    /**
     * Two `1§` lines in a row are one hadith with two paragraphs, not two hadiths — the same rule
     * the single-hadith paste follows, so one syntax does not mean two different things.
     */
    @Test
    fun `adjacent text labels stay one hadith`() {
        val result = parseHadithBulk("a§ باب\nq§ Bab\n1§ نص أول\n1§ نص ثان")

        assertEquals(1, result.hadithCount)
        assertEquals(
            "نص أول\nنص ثان",
            (result.entries.last() as BulkEntry.HadithText).textAr,
        )
    }

    /**
     * `t§`/`tt§` were this format's first names for the two Azerbaijani labels. They still open a
     * section: an unrecognised label is not an error here, it is a *continuation line*, so dropping
     * them would fold an already-written `t§ Bab` silently into the bab's Arabic name.
     */
    @Test
    fun `the retired t and tt labels still open a section`() {
        val result = parseHadithBulk("a§ باب\nt§ Bab\naa§ باب فرعي\ntt§ Alt bab")

        assertEquals(emptyList(), result.problems)
        assertEquals(
            listOf(
                BulkEntry.Chapter(name = "Bab", nameAr = "باب"),
                BulkEntry.SubChapter(name = "Alt bab", nameAr = "باب فرعي"),
            ),
            result.entries,
        )
    }

    /** A bab written in Arabic only still needs a name column and a slug that cannot collide. */
    @Test
    fun `an arabic-only bab falls back to its arabic name and a fixed slug prefix`() {
        val result = parseHadithBulk("a§ كتاب الإيمان")
        val plan = buildBulkPlan(book, result.entries, firstChapterNo = 2)

        val chapter = (plan.single() as BulkRow.Chapter).row
        assertEquals("كتاب الإيمان", chapter.name)
        assertEquals("كتاب الإيمان", chapter.name_ar)
        assertEquals("${book}bb2", chapter.slug)
    }

    /** An alt-bab with no bab above it has no parent slug to take, so it is not written. */
    @Test
    fun `an alt bab before any bab is dropped`() {
        val result = parseHadithBulk("aa§ باب فرعي\nqq§ Alt bab\na§ باب\nq§ Bab")

        assertEquals(1, result.problems.filterIsInstance<BulkProblem.Orphan>().single().count)
        assertEquals(0, result.subChapterCount)
    }

    @Test
    fun `a bab whose labels are both blank is reported rather than written`() {
        val result = parseHadithBulk("a§ باب\nq§ Bab\na§\nq§\n1§ نص")

        assertEquals(
            1,
            result.problems.filterIsInstance<BulkProblem.NamelessSection>().single().count,
        )
        assertEquals(1, result.chapterCount)
    }

    /** CRLF, a BOM and an indented label: whatever the Mac editor put in front of the sign. */
    @Test
    fun `a label survives a bom and crlf and indentation`() {
        val result = parseHadithBulk("﻿a§ باب\r\nq§ Bab\r\n  1§ نص\r\n‏2§ tərcümə")

        assertEquals(1, result.chapterCount)
        val hadith = result.entries.last() as BulkEntry.HadithText
        assertEquals("نص", hadith.textAr)
        assertEquals("tərcümə", hadith.textAz)
    }

    /** `Qanun § 5` is a sentence, not a label — the text before the sign has to match the table. */
    @Test
    fun `a stray section sign stays ordinary text`() {
        val result = parseHadithBulk("a§ باب\nq§ Bab\n1§ نص\nQanun § 5 haqqında")

        assertEquals(1, result.hadithCount)
        assertTrue((result.entries.last() as BulkEntry.HadithText).textAr.endsWith("Qanun § 5 haqqında"))
    }
}
