package com.cafarovceyxun.anamuslim.compose.screens.hadith

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The hadith editor fills itself from a labelled block written on a Mac and carried over by
 * clipboard sync, so the parser has to survive whatever a text editor there produces: CRLF line
 * endings, a BOM, and Arabic that runs over several lines.
 *
 * A label is a number and a `§` — `1§` text, `2§` translation, `3§` source, `4§` note, and
 * `5§`–`9§` for the volume/book/bab fields. The sign was chosen because it appears in no hadith
 * text: the dot this format used before ended most sentences too, which is what the bulk of these
 * tests guard against.
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
    fun readsEveryHadithFieldFromANumberedBlock() {
        val parsed = parseHadith(
            """
            1§ حدثنا
            2§ Bizə rəvayət etdi
            3§ Buxari 42
            4§ qısa qeyd
            """.trimIndent()
        )

        assertEquals("حدثنا", parsed[EditorField.TEXT_AR])
        assertEquals("Bizə rəvayət etdi", parsed[EditorField.TEXT_AZ])
        assertEquals("Buxari 42", parsed[EditorField.SOURCE])
        assertEquals("qısa qeyd", parsed[EditorField.NOTE])
    }

    @Test
    fun aLabelNeedsNoSpaceAfterTheSign() {
        val parsed = parseHadith("1§حدثنا\n2§tərcümə")

        assertEquals("حدثنا", parsed[EditorField.TEXT_AR])
        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun theNumberIsNeverTakenFromTheClipboard() {
        // `getNextNumber` owns the hadith number; there is no label for it on purpose.
        val parsed = parseHadith("№: 42\nnömrə: 42\n2§ tərcümə")

        assertEquals(mapOf(EditorField.TEXT_AZ to "tərcümə"), parsed)
    }

    @Test
    fun continuationLinesStayInTheirBlock() {
        val parsed = parseHadith(
            """
            1§ birinci sətir
            ikinci sətir
            üçüncü sətir
            2§ tərcümə
            """.trimIndent()
        )

        assertEquals("birinci sətir\nikinci sətir\nüçüncü sətir", parsed[EditorField.TEXT_AR])
        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun ordinarySentencesFullOfDotsStayInTheirBlock() {
        // A dot no longer separates anything, so this is now structurally impossible to get wrong.
        val parsed = parseHadith(
            """
            2§ Birinci cümlə.
            Bu ikinci cümlədir. Ardı var.
            ar. Yaxşı. Davam edir.
            """.trimIndent()
        )

        assertEquals(1, parsed.size)
        assertEquals(
            "Birinci cümlə.\nBu ikinci cümlədir. Ardı var.\nar. Yaxşı. Davam edir.",
            parsed[EditorField.TEXT_AZ],
        )
    }

    @Test
    fun aSentenceWithAColonDoesNotOpenABlock() {
        val parsed = parseHadith(
            """
            2§ Peyğəmbər dedi: bu bir cümlədir
            davamı buradadır
            """.trimIndent()
        )

        assertEquals(1, parsed.size)
        assertEquals("Peyğəmbər dedi: bu bir cümlədir\ndavamı buradadır", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun aSectionSignInTheMiddleOfASentenceDoesNotOpenABlock() {
        // The text before the sign has to be a known number; "Qanun" is not.
        val parsed = parseHadith("2§ tərcümə\nQanun § 5 haqqında\n1§ حدثنا")

        assertEquals("tərcümə\nQanun § 5 haqqında", parsed[EditorField.TEXT_AZ])
        assertEquals("حدثنا", parsed[EditorField.TEXT_AR])
    }

    @Test
    fun anUnknownNumberIsTreatedAsPlainTextNotAsALabel() {
        assertEquals(mapOf(EditorField.TEXT_AZ to "0§ dəyər"), parseHadith("0§ dəyər"))
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
        val parsed = parseHadith("حدثنا\nBizə rəvayət etdi")

        assertEquals("حدثنا", parsed[EditorField.TEXT_AR])
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
    fun textBeforeTheFirstLabelIsDropped() {
        val parsed = parseHadith("başlıq sətri\n2§ tərcümə")

        assertEquals(mapOf(EditorField.TEXT_AZ to "tərcümə"), parsed)
    }

    @Test
    fun survivesCrlfBomAndBlankLines() {
        val parsed = parseHadith("﻿1§ نص\r\n\r\n2§ tərcümə\r\n\r\n")

        assertEquals("نص", parsed[EditorField.TEXT_AR])
        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun anEmptyLabelledBlockIsDropped() {
        val parsed = parseHadith("4§\n2§ tərcümə")

        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
        assertTrue(EditorField.NOTE !in parsed)
    }

    @Test
    fun namedEntityFieldsAreRecognisedToo() {
        val parsed = parseNamed("5§ İman kitabı\n6§ كتاب الإيمان\n7§ iman\n8§ Buxari")

        assertEquals("İman kitabı", parsed[EditorField.NAME])
        assertEquals("كتاب الإيمان", parsed[EditorField.NAME_AR])
        assertEquals("iman", parsed[EditorField.SLUG])
        assertEquals("Buxari", parsed[EditorField.AUTHOR])
    }

    // ---- Bir panoda bir neçə hədis ------------------------------------------------------------

    @Test
    fun aSecondCycleOfLabelsBecomesASecondHadith() {
        val records = parseHadiths(
            """
            1§ نص أول
            2§ birinci tərcümə
            3§ Buxari 42
            4§ birinci qeyd
            1§ نص ثاني
            2§ ikinci tərcümə
            3§ Müslim 10
            4§ ikinci qeyd
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
        val records = parseHadiths("1§ نص\n2§ tərcümə\n1§ ikinci\n2§ ikinci tərcümə\n3§ Müslim")

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
            1§ birinci sətir
            davamı
            2§ birinci tərcümə
            1§ ikinci sətir
            onun davamı
            2§ ikinci tərcümə
            """.trimIndent()
        )

        assertEquals(2, records.size)
        assertEquals("birinci sətir\ndavamı", records[0][EditorField.TEXT_AR])
        assertEquals("ikinci sətir\nonun davamı", records[1][EditorField.TEXT_AR])
    }

    @Test
    fun twoAdjacentLinesUnderTheSameLabelStayOneHadith() {
        // İki mənbə bir hədisə aiddir — qonşu təkrar yeni qeyd açmır.
        val records = parseHadiths("1§ نص\n2§ tərcümə\n3§ Buxari 42\n3§ Müslim 10")

        assertEquals(1, records.size)
        assertEquals("Buxari 42\nMüslim 10", records.single()[EditorField.SOURCE])
    }

    @Test
    fun oneCycleIsStillASingleRecord() {
        assertEquals(1, parseHadiths("1§ نص\n2§ tərcümə\n3§ Buxari 42").size)
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
        val parsed = parseHadith("1§ نص\n2§ tərcümə\n1§ ikinci\n2§ ikinci tərcümə")

        assertEquals(mapOf(EditorField.TEXT_AR to "نص", EditorField.TEXT_AZ to "tərcümə"), parsed)
    }

    // ---- Etiketin tanınması ------------------------------------------------------------------

    @Test
    fun anIndentedLabelStillOpensItsBlock() {
        // Mac-dəki redaktor nömrələnmiş siyahını girinti ilə yazır; girinti etiketin hissəsi deyil.
        val records = parseHadiths(
            "1§ نص\n2§ tərcümə\n  3§ Buxari 42\n  4§ qeyd\n" +
                "1§ نص آخر\n2§ ikinci tərcümə\n  3§ Müslim 10\n  4§ ikinci qeyd"
        )

        assertEquals(2, records.size)
        // Girinti tanınmayanda bu iki sətir tərcümənin sonuna yapışırdı.
        assertEquals("tərcümə", records[0][EditorField.TEXT_AZ])
        assertEquals("Buxari 42", records[0][EditorField.SOURCE])
        assertEquals("qeyd", records[0][EditorField.NOTE])
        assertEquals("Müslim 10", records[1][EditorField.SOURCE])
        assertEquals("ikinci qeyd", records[1][EditorField.NOTE])
    }

    @Test
    fun invisibleMarksBeforeALabelDoNotHideIt() {
        // Sətir daxilindəki BOM, RTL işarəsi və qopmayan boşluq — heç biri panoda görünmür.
        val parsed = parseHadith(
            "1§ نص\n2§ tərcümə\n\uFEFF3§ Buxari 42\n\u200F4§ qeyd\n\u00A05§ ad"
        )

        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
        assertEquals("Buxari 42", parsed[EditorField.SOURCE])
        assertEquals("qeyd", parsed[EditorField.NOTE])
        assertEquals("ad", parsed[EditorField.NAME])
    }

    @Test
    fun aLabelWrittenInArabicIndicDigitsIsRecognised() {
        // Ərəb mətninin yanında yazılan etiketi redaktor ərəb rəqəminə çevirə bilir.
        val parsed = parseHadith("١§ حدثنا\n٢§ tərcümə\n٣§ Buxari 42")

        assertEquals("حدثنا", parsed[EditorField.TEXT_AR])
        assertEquals("tərcümə", parsed[EditorField.TEXT_AZ])
        assertEquals("Buxari 42", parsed[EditorField.SOURCE])
    }

    @Test
    fun aValueThatIsOnlyAnInvisibleMarkCountsAsEmpty() {
        val parsed = parseHadith("3§ \u200F\n2§ tərcümə")

        assertEquals(mapOf(EditorField.TEXT_AZ to "tərcümə"), parsed)
    }

    @Test
    fun aLongPrefixIsStillNotALabelAfterCleaning() {
        // Təmizləmə uzun mətni qısaltmır — cümlənin içindəki § blok açmır.
        val parsed = parseHadith("2§ tərcümə\n  Qanun § 5 haqqında\n  1 2 3 § beş")

        assertEquals(1, parsed.size)
        assertEquals("tərcümə\n  Qanun § 5 haqqında\n  1 2 3 § beş", parsed[EditorField.TEXT_AZ])
    }

    // ---- Ərəb rəqəmlərinə çevirmə -------------------------------------------------------------

    @Test
    fun latinDigitsInTheArabicTextBecomeArabicIndic() {
        // The case this exists for: the source leaves the hadith number in Latin digits.
        val parsed = parseHadith("1§ 927-حدثنا يحيى\n2§ 927. Yəhya bizə danışdı")

        assertEquals("٩٢٧-حدثنا يحيى", parsed[EditorField.TEXT_AR])
        // Tərcümə toxunulmur.
        assertEquals("927. Yəhya bizə danışdı", parsed[EditorField.TEXT_AZ])
    }

    @Test
    fun digitsOutsideTheArabicFieldsAreLeftAlone() {
        val parsed = parseHadith("1§ حدثنا\n2§ tərcümə 42\n3§ Buxari 42\n4§ 1-ci qeyd")

        assertEquals("tərcümə 42", parsed[EditorField.TEXT_AZ])
        assertEquals("Buxari 42", parsed[EditorField.SOURCE])
        assertEquals("1-ci qeyd", parsed[EditorField.NOTE])
    }

    @Test
    fun aSourceWrittenInArabicInsideTheSourceFieldKeepsItsDigits() {
        // Ərəb yazısı olsa belə, mənbə xanası çevirmə sahəsindən kənardadır.
        val parsed = parseHadith("1§ حدثنا\n3§ البخاري-162")

        assertEquals("البخاري-162", parsed[EditorField.SOURCE])
    }

    @Test
    fun aLatinLineInsideTheArabicFieldKeepsItsDigits() {
        val parsed = parseHadith("1§ 927-حدثنا\nBuxari 162\n2§ tərcümə")

        assertEquals("٩٢٧-حدثنا\nBuxari 162", parsed[EditorField.TEXT_AR])
    }

    @Test
    fun theBareBabHeadingGetsItsArabicLineShapedToo() {
        val parsed = parseNamed("12-باب الوضوء\n12-Dəstəmaz babı")

        assertEquals("١٢-باب الوضوء", parsed[EditorField.NAME_AR])
        assertEquals("12-Dəstəmaz babı", parsed[EditorField.NAME])
    }

    @Test
    fun everySecondHadithIsShapedAsWell() {
        val records = parseHadiths("1§ 1-حدثنا\n2§ bir\n1§ 2-حدثنا\n2§ iki")

        assertEquals("١-حدثنا", records[0][EditorField.TEXT_AR])
        assertEquals("٢-حدثنا", records[1][EditorField.TEXT_AR])
    }

    @Test
    fun shapingTouchesNothingButLatinDigits() {
        // Hərəkə, durğu işarəsi, artıq ərəb olan rəqəm — hamısı olduğu kimi qalır.
        val diacritics = "حَدَّثَنَا عَبْدُ اللَّهِ،قَالَ:"
        assertEquals(diacritics, diacritics.withArabicDigitsShaped())
        assertEquals("(البخاري-١٦٢)", "(البخاري-١٦٢)".withArabicDigitsShaped())
        assertEquals("٩٢٧-حدثنا", "927-حدثنا".withArabicDigitsShaped())
    }

    @Test
    fun shapingLeavesLinesWithoutArabicScriptUntouched() {
        assertEquals("Buxari 162", "Buxari 162".withArabicDigitsShaped())
        assertEquals("162", "162".withArabicDigitsShaped())
        assertEquals("", "".withArabicDigitsShaped())
        // Qarışıq blokda yalnız ərəb sətri dəyişir.
        assertEquals("١٦٢-حدثنا\nBuxari 162", "162-حدثنا\nBuxari 162".withArabicDigitsShaped())
    }

    // ---- Köhnə format ------------------------------------------------------------------------

    @Test
    fun theRetiredWordSyntaxIsRecognisedAsLegacy() {
        assertTrue("ar. نص\naz. tərcümə".looksLikeLegacyClipboardForm())
        assertTrue("mə. Buxari 42".looksLikeLegacyClipboardForm())
        assertTrue("Mənbə: Müslim".looksLikeLegacyClipboardForm())
    }

    @Test
    fun theCurrentSyntaxAndPlainTextAreNotFlaggedAsLegacy() {
        assertFalse("1§ نص\n2§ tərcümə".looksLikeLegacyClipboardForm())
        assertFalse("Bu adi bir cümlədir. Davamı var.".looksLikeLegacyClipboardForm())
        assertFalse("باب في أن الجنب.\nCünub haqqında bab.".looksLikeLegacyClipboardForm())
    }
}
