package com.cafarovceyxun.anamuslim.utils.reader

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins parseTranslationText to the behaviour of the old XmlPullParser (kXML2) pass it replaced:
 * only reference / fn tags carry meaning, text is XML-entity decoded, and an unknown or malformed
 * entity or tag makes the whole parse fall back to a single tag-stripped Plain part.
 */
class TextParserTest {

    private val slug = "az"

    @Test
    fun plainTextWithoutMarkupIsSinglePlainPart() {
        assertEquals(
            listOf(RichTextPart.Plain("Rahman, Rahim olan Allahin adi ile.")),
            parseTranslationText("Rahman, Rahim olan Allahin adi ile.", slug)
        )
    }

    @Test
    fun emptyStringYieldsNoParts() {
        assertEquals(emptyList(), parseTranslationText("", slug))
    }

    @Test
    fun referenceTagBecomesQuranRefWithAttributes() {
        assertEquals(
            listOf(RichTextPart.QuranRef("ref text", setOf(slug), 2, "255")),
            parseTranslationText("<reference chapter=\"2\" verses=\"255\">ref text</reference>", slug)
        )
    }

    @Test
    fun footnoteTagIsDroppedEntirely() {
        assertEquals(
            emptyList(),
            parseTranslationText("<fn index=\"7\">1</fn>", slug)
        )
    }

    @Test
    fun mixedPlainAndTagsPreserveOrder() {
        assertEquals(
            listOf(
                RichTextPart.Plain("before "),
                RichTextPart.QuranRef("ref", setOf(slug), 1, "1"),
                RichTextPart.Plain(" middle "),
                RichTextPart.Plain(" after"),
            ),
            parseTranslationText(
                "before <reference chapter=\"1\" verses=\"1\">ref</reference> middle <fn index=\"3\">*</fn> after",
                slug
            )
        )
    }

    @Test
    fun missingAttributesDefaultToMinusOneAndEmpty() {
        assertEquals(
            listOf(RichTextPart.QuranRef("x", setOf(slug), -1, "")),
            parseTranslationText("<reference>x</reference>", slug)
        )
        assertEquals(
            emptyList(),
            parseTranslationText("<fn>x</fn>", slug)
        )
    }

    @Test
    fun knownEntitiesAreDecodedInPlainText() {
        assertEquals(
            listOf(RichTextPart.Plain("a&b<c>d\"e'f")),
            parseTranslationText("a&amp;b&lt;c&gt;d&quot;e&apos;f", slug)
        )
    }

    @Test
    fun numericEntitiesAreDecoded() {
        assertEquals(
            listOf(RichTextPart.Plain("'A")),
            parseTranslationText("&#39;&#x41;", slug)
        )
    }

    @Test
    fun unknownTagsAreStrippedButInnerTextKept() {
        assertEquals(
            listOf(RichTextPart.Plain("acd")),
            parseTranslationText("a<b>c</b>d", slug)
        )
    }

    @Test
    fun strayAmpersandFallsBackAndReEncodesDanglingAmp() {
        assertEquals(
            listOf(RichTextPart.Plain("Allah &amp; Messenger")),
            parseTranslationText("Allah & Messenger", slug)
        )
    }

    @Test
    fun nbspIsUnknownEntitySoWholeStringFallsBack() {
        assertEquals(
            listOf(RichTextPart.Plain("a b")),
            parseTranslationText("a&nbsp;b", slug)
        )
    }

    @Test
    fun unterminatedTagFallsBackUnchangedWhenNothingToStrip() {
        assertEquals(
            listOf(RichTextPart.Plain("keep <b this")),
            parseTranslationText("keep <b this", slug)
        )
    }
}
