package com.cafarovceyxun.anamuslim.compose.screens.hadith

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The hadith editor fills itself from a labelled block written on a Mac and carried over by
 * clipboard sync, so the parser has to survive whatever a text editor there produces: CRLF line
 * endings, a BOM, and Arabic that runs over several lines. The block is meant to be typeable
 * entirely in lowercase with a plain dot after each label.
 */
class ClipboardFormParserTest {

    /** How the hadith editor calls it: an unlabelled block falls back to text/translation. */
    private fun parseHadith(raw: String) =
        parseClipboardForm(raw, EditorField.TEXT_AR, EditorField.TEXT_AZ)

    /** How the volume/book/bab editor calls it: the fallback pair is the two name fields. */
    private fun parseNamed(raw: String) =
        parseClipboardForm(raw, EditorField.NAME_AR, EditorField.NAME)

    /** The hadith editor's multi-record call: every cycle of labels is its own hadith. */
    private fun parseHadiths(raw: String) =
        parseClipboardForms(raw, EditorField.TEXT_AR, EditorField.TEXT_AZ)

    @Test
    fun readsEveryHadithFieldFromALowercaseDottedBlock() {
        val parsed = parseHadith(
            """
            ar. حَدَّثَنَا
            az. Bizə rəvayət etdi
            mə. Buxari 42
            qe. qısa qeyd
            """.trimIndent()
        )

        assertEquals("حَدَّثَنَا", parsed[EditorField.TEXT_AR])
        assertEquals("Bizə rəvayət etdi", parsed[EditorField.TEXT_AZ])
        assertEquals("Buxari 42", parsed[EditorField.SOURCE])
        assertEquals("qısa qeyd", parsed[EditorField.NOTE])
    }

    @Test
    fun theNumberIsNeverTakenFromTheClipboard() {
        // `getNextNumber` owns it; a pasted number would hand out a duplicate.
        val parsed = parseHadith("№: 42\nno: 42\nnömrə: 42\naz. tərcümə")

        assertEquals(mapOf(EditorField.TEXT_AZ to "tərcümə"), parsed)
    }

    @Test
    fun continuationLinesStayInTheirBlock() {
        val parsed = parseHadith(
            """
            ar. birinci sətir
            ikinci sətir
            üçüncü sətir
            az. tərcümə
            """.trimIndent()
        )

        assertEquals("birinci sətir\nikinci sətir\nüçüncü sətir", parsed[EditorField.TEXT_AR])
        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun ordinarySentencesFullOfDotsStayInTheirBlock() {
        val parsed = parseHadith(
            """
            az. Birinci cümlə.
            Bu ikinci cümlədir. Ardı var.
            Yaxşı. Davam edir.
            """.trimIndent()
        )

        assertEquals(1, parsed.size)
        assertEquals(
            "Birinci cümlə.\nBu ikinci cümlədir. Ardı var.\nYaxşı. Davam edir.",
            parsed[EditorField.TEXT_AZ],
        )
    }

    @Test
    fun aSentenceWithAColonDoesNotOpenABlock() {
        val parsed = parseHadith(
            """
            az: Peyğəmbər dedi: bu bir cümlədir
            davamı buradadır
            """.trimIndent()
        )

        assertEquals(1, parsed.size)
        assertEquals("Peyğəmbər dedi: bu bir cümlədir\ndavamı buradadır", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun dotAndColonBlocksCanBeMixed() {
        val parsed = parseHadith("az. tərcümə\nMənbə: Müslim")

        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
        assertEquals("Müslim", parsed[EditorField.SOURCE])
    }

    @Test
    fun aBareBabHeadingSplitsByScript() {
        // Exactly how bab headings are copied: one Arabic line, one Azerbaijani line, no labels.
        val parsed = parseNamed(
            """
            باب في أن الجنب إذا أراد أن ينام، عليه أن يتوضأ وضوءه للصلاة.
            Cünub olan kimsənin yatmaq istədikdə namaz üçün aldığı kimi dəstəmaz almasının gərəkli olması haqqında bab.
            """.trimIndent()
        )

        assertEquals(
            "باب في أن الجنب إذا أراد أن ينام، عليه أن يتوضأ وضوءه للصلاة.",
            parsed[EditorField.NAME_AR],
        )
        assertEquals(
            "Cünub olan kimsənin yatmaq istədikdə namaz üçün aldığı kimi dəstəmaz almasının gərəkli olması haqqında bab.",
            parsed[EditorField.NAME],
        )
    }

    @Test
    fun theSameBareBlockFeedsTextAndTranslationOnTheHadithEditor() {
        val parsed = parseHadith("حَدَّثَنَا\nBizə rəvayət etdi")

        assertEquals("حَدَّثَنَا", parsed[EditorField.TEXT_AR])
        assertEquals("Bizə rəvayət etdi", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun eachHalfOfABareBlockKeepsItsOwnLineOrder() {
        val parsed = parseHadith("نص أول\nنص ثاني\n\nbirinci sətir\nikinci sətir")

        assertEquals("نص أول\nنص ثاني", parsed[EditorField.TEXT_AR])
        assertEquals("birinci sətir\nikinci sətir", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun aBareBlockOfOneScriptFillsOnlyThatField() {
        val onlyLatin = parseHadith("sadəcə bir mətn, etiketsiz")
        assertEquals(mapOf(EditorField.TEXT_AZ to "sadəcə bir mətn, etiketsiz"), onlyLatin)

        val onlyArabic = parseHadith("نص عربي فقط")
        assertEquals(mapOf(EditorField.TEXT_AR to "نص عربي فقط"), onlyArabic)
    }

    @Test
    fun blankClipboardYieldsNothing() {
        assertTrue(parseHadith("").isEmpty())
        assertTrue(parseHadith("   \n\n  ").isEmpty())
    }

    @Test
    fun anUnknownLabelIsTreatedAsPlainTextNotAsALabel() {
        // "naməlum" is no label, so the line is ordinary Latin text and lands in the translation.
        assertEquals(mapOf(EditorField.TEXT_AZ to "naməlum: dəyər"), parseHadith("naməlum: dəyər"))
    }

    @Test
    fun textBeforeTheFirstLabelIsDropped() {
        val parsed = parseHadith("başlıq sətri\naz. tərcümə")

        assertEquals(mapOf(EditorField.TEXT_AZ to "tərcümə"), parsed)
    }

    @Test
    fun labelsAreCaseInsensitiveAndAcceptAsciiAliases() {
        val parsed = parseHadith("Arabic: نص\nTRANSLATION: tərcümə\nMe. Müslim")

        assertEquals("نص", parsed[EditorField.TEXT_AR])
        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
        assertEquals("Müslim", parsed[EditorField.SOURCE])
    }

    @Test
    fun survivesCrlfBomAndBlankLines() {
        val parsed = parseHadith("﻿ar. نص\r\n\r\naz. tərcümə\r\n\r\n")

        assertEquals("نص", parsed[EditorField.TEXT_AR])
        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun anEmptyLabelledBlockIsDropped() {
        val parsed = parseHadith("qe.\naz. tərcümə")

        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
        assertTrue(EditorField.NOTE !in parsed)
    }

    @Test
    fun aSecondCycleOfLabelsBecomesASecondHadith() {
        val records = parseHadiths(
            """
            ar. نص أول
            az. birinci tərcümə
            mə. Buxari 42
            qe. birinci qeyd
            ar. نص ثاني
            az. ikinci tərcümə
            mə. Müslim 10
            qe. ikinci qeyd
            """.trimIndent()
        )

        assertEquals(2, records.size)
        assertEquals(
            mapOf(
                EditorField.TEXT_AR to "نص أول",
                EditorField.TEXT_AZ to "birinci tərcümə",
                EditorField.SOURCE to "Buxari 42",
                EditorField.NOTE to "birinci qeyd",
            ),
            records[0],
        )
        assertEquals(
            mapOf(
                EditorField.TEXT_AR to "نص ثاني",
                EditorField.TEXT_AZ to "ikinci tərcümə",
                EditorField.SOURCE to "Müslim 10",
                EditorField.NOTE to "ikinci qeyd",
            ),
            records[1],
        )
    }

    @Test
    fun aLaterRecordMayLeaveFieldsOutOrAddTheOnesTheFirstLacked() {
        val records = parseHadiths("ar. نص\naz. tərcümə\nar. ikinci\naz. ikinci tərcümə\nmə. Müslim")

        assertEquals(2, records.size)
        assertEquals(mapOf(EditorField.TEXT_AR to "نص", EditorField.TEXT_AZ to "tərcümə"), records[0])
        assertEquals(
            mapOf(
                EditorField.TEXT_AR to "ikinci",
                EditorField.TEXT_AZ to "ikinci tərcümə",
                EditorField.SOURCE to "Müslim",
            ),
            records[1],
        )
    }

    @Test
    fun continuationLinesFollowTheirOwnRecord() {
        val records = parseHadiths(
            """
            ar. birinci sətir
            davamı
            az. birinci tərcümə
            ar. ikinci sətir
            onun davamı
            az. ikinci tərcümə
            """.trimIndent()
        )

        assertEquals(2, records.size)
        assertEquals("birinci sətir\ndavamı", records[0][EditorField.TEXT_AR])
        assertEquals("ikinci sətir\nonun davamı", records[1][EditorField.TEXT_AR])
    }

    @Test
    fun twoAdjacentLinesUnderTheSameLabelStayOneHadith() {
        // İki mənbə bir hədisə aiddir — qonşu təkrar yeni qeyd açmır.
        val records = parseHadiths("ar. نص\naz. tərcümə\nmə. Buxari 42\nmə. Müslim 10")

        assertEquals(1, records.size)
        assertEquals("Buxari 42\nMüslim 10", records.single()[EditorField.SOURCE])
    }

    @Test
    fun oneCycleIsStillASingleRecord() {
        val records = parseHadiths("ar. نص\naz. tərcümə\nmə. Buxari 42")

        assertEquals(1, records.size)
    }

    @Test
    fun anUnlabelledBlockIsNeverSplitIntoSeveralHadiths() {
        val records = parseHadiths("نص أول\nنص ثاني\nbirinci sətir\nikinci sətir")

        assertEquals(1, records.size)
        assertEquals("نص أول\nنص ثاني", records.single()[EditorField.TEXT_AR])
        assertEquals("birinci sətir\nikinci sətir", records.single()[EditorField.TEXT_AZ])
    }

    @Test
    fun anEmptyClipboardYieldsNoRecords() {
        assertTrue(parseHadiths("").isEmpty())
        assertTrue(parseHadiths("   \n\n  ").isEmpty())
    }

    @Test
    fun theSingleRecordCallKeepsOnlyTheFirstCycle() {
        // Ad daşıyan redaktorlar (cild/kitab/bab) bir sətirlikdir — ikinci dövr onlara sızmamalıdır.
        val parsed = parseHadith("ar. نص\naz. tərcümə\nar. ikinci\naz. ikinci tərcümə")

        assertEquals(mapOf(EditorField.TEXT_AR to "نص", EditorField.TEXT_AZ to "tərcümə"), parsed)
    }

    @Test
    fun namedEntityFieldsAreRecognisedToo() {
        val parsed = parseNamed("ad. İman kitabı\nad_ar. كتاب الإيمان\nslug. iman\nmüəllif. Buxari")

        assertEquals("İman kitabı", parsed[EditorField.NAME])
        assertEquals("كتاب الإيمان", parsed[EditorField.NAME_AR])
        assertEquals("iman", parsed[EditorField.SLUG])
        assertEquals("Buxari", parsed[EditorField.AUTHOR])
    }
}
