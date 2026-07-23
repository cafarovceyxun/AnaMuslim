package com.cafarovceyxun.anamuslim.utils.reader.atlas

import com.cafarovceyxun.anamuslim.db.entities.atlas.AtlasWordShapeEntity
import kotlinx.coroutines.test.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers the hand-written JSON pull scanner that replaced `android.util.JsonReader` when the atlas
 * import moved to `commonMain`. A library parser is not an option there — the Uthmani `layout.json`
 * is ~11 MB and decoding it into a tree costs tens of megabytes of peak memory on a phone — so the
 * scanner is ours to get right, which is exactly why it is worth a test.
 *
 * The fixtures are written as literal JSON rather than serialized from models: what is under test
 * is the reader's tolerance for the *text* it will meet in real bundles (escapes, unknown members,
 * whitespace, exponents), not a round-trip through our own writer.
 */
class AtlasLayoutParserTest {

    @Test
    fun readsWordsPagesAndGlyphPlacements() = runTest {
        val json = """
            {
              "documents": {
                "doc-1": {
                  "text": "بِسْمِ",
                  "page": 1,
                  "glyphs": [
                    { "g": 12, "xa": 1.5, "ya": -2, "xo": 0, "yo": 3 },
                    { "g": 13, "xa": 4, "ya": 5, "xo": 6, "yo": 7 }
                  ]
                },
                "doc-2": {
                  "text": "ٱللَّهِ",
                  "page": 2,
                  "glyphs": []
                }
              }
            }
        """.trimIndent()

        val shapes = parse(json, bundleKey = "uthmani", requirePage = true)

        assertEquals(2, shapes.size)
        assertEquals("بِسْمِ", shapes[0].word)
        assertEquals(1, shapes[0].page)
        assertEquals("uthmani", shapes[0].bundleKey)
        assertEquals(
            """[{"g":12,"xa":1.5,"ya":-2,"xo":0,"yo":3},{"g":13,"xa":4,"ya":5,"xo":6,"yo":7}]""",
            shapes[0].placementsJson,
        )
        assertEquals("ٱللَّهِ", shapes[1].word)
        assertEquals(2, shapes[1].page)
        assertEquals("[]", shapes[1].placementsJson)
    }

    /** Word-global bundles carry no page; every row must land on the shared sentinel key. */
    @Test
    fun usesPageSentinelWhenPageIsNotRequired() = runTest {
        val json = """
            {"documents":{"a":{"text":"word","page":7,"glyphs":[{"g":1,"xa":0,"ya":0,"xo":0,"yo":0}]}}}
        """.trimIndent()

        val shapes = parse(json, bundleKey = "word_global", requirePage = false)

        assertEquals(1, shapes.size)
        // Even though the document *has* a page, a word-global bundle must ignore it — otherwise
        // the primary key (bundle, word, page) would split rows the renderer looks up by sentinel.
        assertEquals(AtlasWordShapeEntity.ATLAS_PAGE_NONE, shapes[0].page)
    }

    /**
     * Real bundles carry metadata the renderer never reads. Skipping has to work at every level and
     * for every JSON type, or the scanner desynchronises and the rest of the file is garbage.
     */
    @Test
    fun skipsUnknownMembersOfEveryType() = runTest {
        val json = """
            {
              "version": 3,
              "meta": { "kind": "page_glyph_atlas", "nested": { "deep": [1, 2, {"x": null}] } },
              "flags": [true, false, null],
              "documents": {
                "doc": {
                  "unused_object": { "a": [1, {"b": "c"}] },
                  "text": "word",
                  "unused_array": ["x", 2, false],
                  "page": 5,
                  "unused_bool": true,
                  "glyphs": [
                    { "g": 1, "unused": "skip me", "xa": 2, "ya": 3, "xo": 4, "yo": 5, "extra": [9] }
                  ],
                  "unused_null": null
                }
              },
              "trailing": "ignored"
            }
        """.trimIndent()

        val shapes = parse(json, bundleKey = "b", requirePage = true)

        assertEquals(1, shapes.size)
        assertEquals("word", shapes[0].word)
        assertEquals(5, shapes[0].page)
        // Only the five fields the renderer decodes survive, in source order.
        assertEquals("""[{"g":1,"xa":2,"ya":3,"xo":4,"yo":5}]""", shapes[0].placementsJson)
    }

    /** Arabic text arrives escaped from some exporters, and diacritics must survive byte-exact. */
    @Test
    fun decodesStringEscapes() = runTest {
        val json = """
            {"documents":{"d":{"text":"بِسْ\tquote:\" back:\\ slash:\/","page":1,"glyphs":[]}}}
        """.trimIndent()

        val shapes = parse(json, bundleKey = "b", requirePage = true)

        assertEquals("بِسْ\tquote:\" back:\\ slash:/", shapes[0].word)
    }

    /** Glyph coordinates are floats; exporters emit exponents and the literal must pass through. */
    @Test
    fun preservesNumberLiteralsIncludingExponents() = runTest {
        val json = """
            {"documents":{"d":{"text":"w","page":1,"glyphs":[{"g":1,"xa":1.0e-3,"ya":-2.5E2,"xo":0,"yo":0}]}}}
        """.trimIndent()

        val shapes = parse(json, bundleKey = "b", requirePage = true)

        assertEquals("""[{"g":1,"xa":1.0e-3,"ya":-2.5E2,"xo":0,"yo":0}]""", shapes[0].placementsJson)
    }

    /** A null coordinate is dropped rather than written as `null`, which the decoder cannot read. */
    @Test
    fun dropsNullGlyphFields() = runTest {
        val json = """
            {"documents":{"d":{"text":"w","page":1,"glyphs":[{"g":1,"xa":null,"ya":2,"xo":3,"yo":4}]}}}
        """.trimIndent()

        val shapes = parse(json, bundleKey = "b", requirePage = true)

        assertEquals("""[{"g":1,"ya":2,"xo":3,"yo":4}]""", shapes[0].placementsJson)
    }

    /** Pretty-printed bundles exist in the wild; whitespace must never reach a value. */
    @Test
    fun toleratesWhitespaceBetweenEveryToken() = runTest {
        val json = "{\n\t\"documents\" : {\r\n  \"d\" : {\n \"text\" : \"w\" ,\n" +
                " \"page\" : 4 ,\n \"glyphs\" : [ { \"g\" : 1 , \"xa\" : 2 , \"ya\" : 3 ," +
                " \"xo\" : 4 , \"yo\" : 5 } ]\n }\n }\n}"

        val shapes = parse(json, bundleKey = "b", requirePage = true)

        assertEquals("w", shapes[0].word)
        assertEquals(4, shapes[0].page)
        assertEquals("""[{"g":1,"xa":2,"ya":3,"xo":4,"yo":5}]""", shapes[0].placementsJson)
    }

    /** Shapes are handed over one at a time — that streaming contract is the point of the parser. */
    @Test
    fun emitsShapesWhileReadingRatherThanAtTheEnd() = runTest {
        val json = buildString {
            append("""{"documents":{""")
            repeat(3) { index ->
                if (index > 0) append(',')
                append(""""d$index":{"text":"w$index","page":${index + 1},"glyphs":[]}""")
            }
            append("}}")
        }

        val source = Buffer().writeUtf8(json)
        val seen = mutableListOf<String>()
        var remainingWhenFirstSeen = -1L

        AtlasLayoutParser.streamWordShapes(source, bundleKey = "b", requirePage = true) { shape ->
            if (seen.isEmpty()) remainingWhenFirstSeen = source.size
            seen += shape.word
        }

        assertEquals(listOf("w0", "w1", "w2"), seen)
        // The first callback ran with the tail of the document still unread.
        assertTrue(remainingWhenFirstSeen > 0, "parser buffered the whole file before emitting")
    }

    /**
     * A page-scoped bundle without a page would silently collapse rows onto one primary key, so it
     * must fail loudly instead — the import publishes its bundle row last, and a throw here leaves
     * the previous bundle in place.
     */
    @Test
    fun failsWhenAPageScopedDocumentHasNoPage() = runTest {
        val json = """{"documents":{"d":{"text":"w","glyphs":[]}}}"""

        assertFailsWith<IllegalStateException> {
            parse(json, bundleKey = "b", requirePage = true)
        }
    }

    @Test
    fun failsWhenADocumentHasNoText() = runTest {
        val json = """{"documents":{"d":{"page":1,"glyphs":[]}}}"""

        assertFailsWith<IllegalStateException> {
            parse(json, bundleKey = "b", requirePage = true)
        }
    }

    @Test
    fun failsOnTruncatedInput() = runTest {
        val json = """{"documents":{"d":{"text":"w","page":1,"glyphs":[{"g":1"""

        assertFailsWith<IllegalStateException> {
            parse(json, bundleKey = "b", requirePage = true)
        }
    }

    private suspend fun parse(
        json: String,
        bundleKey: String,
        requirePage: Boolean,
    ): List<AtlasWordShapeEntity> {
        val shapes = mutableListOf<AtlasWordShapeEntity>()
        AtlasLayoutParser.streamWordShapes(
            source = Buffer().writeUtf8(json),
            bundleKey = bundleKey,
            requirePage = requirePage,
        ) { shapes += it }
        return shapes
    }
}
