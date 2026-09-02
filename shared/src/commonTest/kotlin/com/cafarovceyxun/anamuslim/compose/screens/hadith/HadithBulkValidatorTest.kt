package com.cafarovceyxun.anamuslim.compose.screens.hadith

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bulk parser is forgiving by design — a missing or repeated label still produces rows, and the
 * preview shows them looking perfectly ordinary. These tests hold the check that refuses them down:
 * an escaped mistake is a hundred hadiths written with the wrong translation and the wrong source.
 */
class HadithBulkValidatorTest {

    @Test
    fun `a hadith with all three labels is clean with or without the note`() {
        val issues = validateHadithBulk(
            """
            a§ كتاب بدء الوحي
            q§ Vəhyin başlanğıcı
            1§ حدثنا الحميدي
            2§ Bizə rəvayət etdi
            3§ Buxari 1
            1§ حدثنا عبد الله
            2§ Bizə xəbər verdi
            3§ Buxari 2
            4§ qeyd
            """.trimIndent()
        )

        assertEquals(emptyList(), issues)
    }

    @Test
    fun `the same label twice in one hadith is an error`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            1§ نص آخر
            2§ tərcümə
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(1, issues.size)
        assertEquals(BulkIssueKind.Repeated("1§"), issues.single().kind)
        assertEquals(4, issues.single().line)
        assertEquals(BulkIssueLevel.ERROR, issues.single().level)
    }

    @Test
    fun `a skipped label is reported where the wrong one arrived`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(1, issues.size)
        assertEquals(BulkIssueKind.OutOfOrder(found = "3§", expected = "2§"), issues.single().kind)
        assertEquals(4, issues.single().line)
    }

    @Test
    fun `a hadith that starts at the translation is missing its Arabic`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            2§ tərcümə
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(
            listOf(BulkIssueKind.OutOfOrder(found = "2§", expected = "1§")),
            issues.map { it.kind },
        )
    }

    /** The note is the only optional one: a hadith is over when `3§` has been written. */
    @Test
    fun `a hadith cut off before the source is reported as unfinished`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            2§ tərcümə
            a§ باب ثان
            q§ İkinci bab
            1§ نص ثان
            """.trimIndent()
        )

        assertEquals(
            listOf(
                BulkIssueKind.Incomplete(listOf("3§")),
                BulkIssueKind.Incomplete(listOf("2§", "3§")),
            ),
            issues.map { it.kind },
        )
        // Yarımçıq hədis öz son etiketinin sətrində bildirilir — düzəliş məhz oraya yazılacaq.
        assertEquals(listOf(4, 7), issues.map { it.line })
    }

    /** One mistake is one issue: the labels after it are read from where they actually are. */
    @Test
    fun `an error does not cascade into the hadiths after it`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            3§ Buxari 1
            1§ نص ثان
            2§ tərcümə
            3§ Buxari 2
            """.trimIndent()
        )

        assertEquals(1, issues.size)
    }

    @Test
    fun `continuation lines keep the sequence intact`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ حدثنا الحميدي
            قال حدثنا سفيان
            2§ Bizə rəvayət etdi,
            sonra davam etdi
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(emptyList(), issues)
    }

    @Test
    fun `latin letters in the arabic text are an error but digits are not`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ 927-حدثنا Bize sonra
            2§ tərcümə
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(1, issues.size)
        assertEquals(BulkIssueKind.LatinInArabic("Bize, sonra"), issues.single().kind)
        assertEquals(BulkIssueLevel.ERROR, issues.single().level)
    }

    @Test
    fun `arabic letters in the translation are only a warning`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            2§ Peyğəmbər ﷺ dedi: الله
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(1, issues.size)
        assertEquals(BulkIssueLevel.WARNING, issues.single().level)
        assertTrue(issues.single().kind is BulkIssueKind.ArabicInLatin)
    }

    /** The offset is what the panel jumps to; a line number alone would still mean scrolling. */
    @Test
    fun `the offset points at the offending word itself`() {
        val raw = """
            a§ باب
            q§ Bab
            1§ نص Bize
            2§ tərcümə
            3§ Buxari 1
        """.trimIndent()

        val issue = validateHadithBulk(raw).single()

        assertEquals("Bize", raw.substring(issue.start, issue.end))
    }

    @Test
    fun `a label issue selects the line it is on`() {
        val raw = """
            a§ باب
            q§ Bab
            1§ نص
            3§ Buxari 1
        """.trimIndent()

        val issue = validateHadithBulk(raw).single()

        assertTrue(raw.substring(issue.start, issue.end).startsWith("3§"))
    }

    @Test
    fun `a label with nothing after it is an empty field`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            2§
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(listOf(BulkIssueKind.EmptyField("2§")), issues.map { it.kind })
        assertEquals(4, issues.single().line)
    }

    /** `buildBulkPlan` boş azərbaycanca adı ərəbcə ilə doldurur — yəni səhv sütuna yazır. */
    @Test
    fun `a bab with only its arabic name is an error`() {
        val issues = validateHadithBulk(
            """
            a§ كتاب بدء الوحي
            1§ نص
            2§ tərcümə
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(listOf(BulkIssueKind.MissingLatinName("q§")), issues.map { it.kind })
        assertEquals(BulkIssueLevel.ERROR, issues.single().level)
        assertEquals(1, issues.single().line)
    }

    @Test
    fun `a bab with only its azerbaijani name is a warning`() {
        val issues = validateHadithBulk(
            """
            q§ Vəhyin başlanğıcı
            1§ نص
            2§ tərcümə
            3§ Buxari 1
            """.trimIndent()
        )

        assertEquals(listOf(BulkIssueKind.MissingArabicName("a§")), issues.map { it.kind })
        assertEquals(BulkIssueLevel.WARNING, issues.single().level)
    }

    @Test
    fun `the same hadith text twice in one paste warns and points at the first`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            2§ tərcümə
            3§ Buxari 1
            1§ نص
            2§ ikinci
            3§ Buxari 2
            """.trimIndent()
        )

        assertEquals(listOf(BulkIssueKind.DuplicateHadith(firstLine = 3)), issues.map { it.kind })
        assertEquals(6, issues.single().line)
        assertEquals(BulkIssueLevel.WARNING, issues.single().level)
    }

    @Test
    fun `two babs with the same name warn`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            2§ tərcümə
            3§ Buxari 1
            a§ باب ثان
            q§ Bab
            1§ نص ثان
            2§ ikinci
            3§ Buxari 2
            """.trimIndent()
        )

        assertEquals(listOf(BulkIssueKind.DuplicateChapter(firstLine = 1)), issues.map { it.kind })
        assertEquals(6, issues.single().line)
    }

    /** «باب» alt bab adı kimi kitab boyu qanuni şəkildə təkrarlanır — orada təkrar axtarılmır. */
    @Test
    fun `alt babs may repeat their name freely`() {
        val issues = validateHadithBulk(
            """
            a§ كتاب
            q§ Kitab
            aa§ باب
            qq§ Bab
            1§ نص
            2§ tərcümə
            3§ Buxari 1
            aa§ باب
            qq§ Bab
            1§ نص ثان
            2§ ikinci
            3§ Buxari 2
            """.trimIndent()
        )

        assertEquals(emptyList(), issues)
    }

    @Test
    fun `a bab the book already holds warns about a second import`() {
        val issues = validateHadithBulk(
            """
            a§ كتاب بدء الوحي
            q§ Vəhyin başlanğıcı
            1§ نص
            2§ tərcümə
            3§ Buxari 1
            """.trimIndent(),
            // Boşluq və baş hərf fərqi ad tutuşdurmasını pozmamalıdır.
            existingChapterNames = setOf(" vəhyin  başlanğıcı "),
        )

        assertEquals(listOf(BulkIssueKind.ChapterExists), issues.map { it.kind })
        assertEquals(BulkIssueLevel.WARNING, issues.single().level)
    }

    @Test
    fun `a paste with no hadiths at all has nothing to check`() {
        val issues = validateHadithBulk(
            """
            a§ كتاب
            q§ Kitab
            aa§ باب
            qq§ Bab
            """.trimIndent()
        )

        assertEquals(emptyList(), issues)
    }

    @Test
    fun `a verse reference closes the hadith before it`() {
        val issues = validateHadithBulk(
            """
            a§ باب
            q§ Bab
            1§ نص
            2§ tərcümə
            3:51§
            1§ نص ثان
            2§ ikinci
            3§ Buxari 2
            """.trimIndent()
        )

        assertEquals(listOf(BulkIssueKind.Incomplete(listOf("3§"))), issues.map { it.kind })
    }
}
