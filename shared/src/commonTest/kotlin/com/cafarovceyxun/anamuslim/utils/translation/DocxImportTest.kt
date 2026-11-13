package com.cafarovceyxun.anamuslim.utils.translation

import kotlin.test.Test
import kotlin.test.assertEquals

class DocxImportTest {

    private fun doc(body: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<w:document xmlns:w="x"><w:body>$body</w:body></w:document>"""

    private val footnotes = """<?xml version="1.0"?><w:footnotes xmlns:w="x">""" +
        """<w:footnote w:type="separator" w:id="-1"><w:p><w:r><w:separator/></w:r></w:p></w:footnote>""" +
        """<w:footnote w:id="1"><w:p><w:r><w:footnoteRef/></w:r><w:r><w:t xml:space="preserve"> Ayədə keçən </w:t></w:r>""" +
        """<w:r><w:t>“Furqan” &amp; s.</w:t></w:r></w:p></w:footnote></w:footnotes>"""

    @Test
    fun paragraphsAreSeparatedByBlankLines() {
        val xml = doc(
            """<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space="preserve">-1: </w:t></w:r>""" +
                """<w:r><w:t>Əlif.</w:t></w:r></w:p><w:p/><w:p><w:r><w:t>-2: Bu</w:t></w:r></w:p>"""
        )
        assertEquals("-1: Əlif.\n\n-2: Bu", docxXmlToImportText(xml))
    }

    @Test
    fun footnoteReferenceIsInlinedWithItsText() {
        val xml = doc(
            """<w:p><w:r><w:t>-53: Musaya Furqanı</w:t></w:r><w:r><w:rPr><w:rStyle w:val="FootnoteReference"/></w:rPr>""" +
                """<w:footnoteReference w:id="1"/></w:r><w:r><w:t xml:space="preserve"> verdik.</w:t></w:r></w:p>"""
        )
        assertEquals(
            "-53: Musaya Furqanı{{Ayədə keçən “Furqan” & s.}} verdik.",
            docxXmlToImportText(xml, footnotesXml = footnotes),
        )
    }

    @Test
    fun deletedAndFallbackTextIsIgnored() {
        val xml = doc(
            """<w:p><w:r><w:t>a</w:t></w:r><w:del><w:r><w:delText>silinən</w:delText></w:r></w:del>""" +
                """<mc:AlternateContent><mc:Choice/><mc:Fallback><w:r><w:t>dublikat</w:t></w:r></mc:Fallback></mc:AlternateContent>""" +
                """<w:r><w:tab/><w:t>b&#x259;</w:t></w:r></w:p>"""
        )
        assertEquals("a bə", docxXmlToImportText(xml))
    }

    @Test
    fun endToEndFootnoteBecomesNote() {
        val xml = doc(
            """<w:p><w:r><w:t>-1: Furqanı</w:t></w:r><w:r><w:footnoteReference w:id="1"/></w:r>""" +
                """<w:r><w:t xml:space="preserve"> verdik.</w:t></w:r></w:p>"""
        )
        val text = docxXmlToImportText(xml, footnotesXml = footnotes)
        val verse = parseTranslationImport(text, verseCount = 1, mode = ImportParseMode.ByVerseNumber).verses.single()

        assertEquals("Furqanı1 verdik.", verse.text)
        assertEquals("(1) - Ayədə keçən “Furqan” & s.", verse.note)
    }

    @Test
    fun binaryContentIsNotText() {
        kotlin.test.assertTrue("1. Bismillah\n\n2. Həmd".looksLikeText())
        kotlin.test.assertFalse("%PDF-1.7\u0000\u0000\uFFFD\uFFFD\uFFFD".looksLikeText())
    }
}
