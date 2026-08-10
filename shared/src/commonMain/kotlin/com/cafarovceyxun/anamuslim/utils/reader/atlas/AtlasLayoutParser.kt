package com.cafarovceyxun.anamuslim.utils.reader.atlas

import com.cafarovceyxun.anamuslim.db.entities.atlas.AtlasWordShapeEntity
import okio.Buffer
import okio.BufferedSource

/**
 * Streams `layout.json` out of an atlas bundle, one word shape at a time.
 *
 * **Why a hand-written scanner.** The Uthmani layout file is ~11 MB; decoding it into a
 * `JsonElement` tree (the only thing `kotlinx.serialization` offers in common code) would cost
 * tens of megabytes of peak memory on a phone. Android already avoided that with
 * `android.util.JsonReader`, which has no multiplatform counterpart — hence this deliberately
 * narrow reader, in the same spirit as the ustar reader in
 * [com.cafarovceyxun.anamuslim.utils.univ.TarGzExtractor]: it understands exactly the shape of
 * this file and nothing more.
 *
 * Expected shape (unknown members at any level are skipped):
 * ```
 * { "documents": { "<any key>": { "text": "…", "page": 3, "glyphs": [ {g,xa,ya,xo,yo}, … ] }, … } }
 * ```
 */
internal object AtlasLayoutParser {

    /**
     * Emits every layout document as an [AtlasWordShapeEntity]. [requirePage] mirrors
     * `meta.json`'s `page_glyph_atlas` kind: page-scoped bundles must carry a page per document,
     * word-global ones store [AtlasWordShapeEntity.ATLAS_PAGE_NONE].
     */
    suspend fun streamWordShapes(
        source: BufferedSource,
        bundleKey: String,
        requirePage: Boolean,
        onShape: suspend (AtlasWordShapeEntity) -> Unit,
    ) {
        val scanner = JsonScanner(source)

        scanner.beginObject()
        while (scanner.hasNextMember()) {
            when (scanner.readName()) {
                "documents" -> {
                    scanner.beginObject()
                    while (scanner.hasNextMember()) {
                        scanner.readName() // the document key itself is not used
                        onShape(readDocument(scanner, bundleKey, requirePage))
                    }
                    scanner.endObject()
                }

                else -> scanner.skipValue()
            }
        }
        scanner.endObject()
    }

    private fun readDocument(
        scanner: JsonScanner,
        bundleKey: String,
        requirePage: Boolean,
    ): AtlasWordShapeEntity {
        var text: String? = null
        var page: Int? = null
        var placementsJson = "[]"

        scanner.beginObject()
        while (scanner.hasNextMember()) {
            when (scanner.readName()) {
                "text" -> text = scanner.readString()
                "page" -> page = scanner.readNumberLiteralOrNull()?.toIntOrNull()
                "glyphs" -> placementsJson = readGlyphPlacements(scanner)
                else -> scanner.skipValue()
            }
        }
        scanner.endObject()

        val word = text ?: error("atlas layout document missing text")
        val resolvedPage = if (requirePage) {
            page ?: error("page_glyph_atlas layout document missing page: $word")
        } else {
            AtlasWordShapeEntity.ATLAS_PAGE_NONE
        }

        return AtlasWordShapeEntity(
            bundleKey = bundleKey,
            word = word,
            page = resolvedPage,
            placementsJson = placementsJson,
        )
    }

    /**
     * Re-emits the glyph array as compact JSON with only the fields the renderer reads, exactly
     * like the Android importer did — the placements are stored as a string column and decoded
     * lazily by `QuranAtlasLoader.decodePlacementsJson`.
     */
    private fun readGlyphPlacements(scanner: JsonScanner): String {
        val out = StringBuilder()
        out.append('[')

        scanner.beginArray()
        var firstGlyph = true
        while (scanner.hasNextElement()) {
            if (firstGlyph) firstGlyph = false else out.append(',')

            out.append('{')
            var firstField = true
            scanner.beginObject()
            while (scanner.hasNextMember()) {
                when (val name = scanner.readName()) {
                    "g", "xa", "ya", "xo", "yo" -> {
                        val value = scanner.readNumberLiteralOrNull()
                        if (value != null) {
                            if (firstField) firstField = false else out.append(',')
                            out.append('"').append(name).append("\":").append(value)
                        }
                    }

                    else -> scanner.skipValue()
                }
            }
            scanner.endObject()
            out.append('}')
        }
        scanner.endArray()

        out.append(']')
        return out.toString()
    }
}

/**
 * Minimal pull scanner for well-formed JSON. Supports exactly what [AtlasLayoutParser] needs:
 * object/array traversal, string and number values, and skipping anything unrecognised.
 */
private class JsonScanner(private val source: BufferedSource) {

    /** One-character lookahead; `NONE` when nothing is buffered. */
    private var peeked: Int = NONE

    fun beginObject() = expect('{')

    fun endObject() = Unit // consumed by hasNextMember()

    fun beginArray() = expect('[')

    fun endArray() = Unit // consumed by hasNextElement()

    /** True when another `"name": value` pair follows; consumes the closing `}` otherwise. */
    fun hasNextMember(): Boolean = hasNextEntry('}')

    /** True when another array element follows; consumes the closing `]` otherwise. */
    fun hasNextElement(): Boolean = hasNextEntry(']')

    private fun hasNextEntry(close: Char): Boolean {
        var c = nextNonWhitespace()

        if (c == ',') {
            c = nextNonWhitespace()
        }

        if (c == close) return false

        push(c)
        return true
    }

    fun readName(): String {
        val name = readString()
        expect(':')
        return name
    }

    fun readString(): String {
        expect('"')

        val buffer = Buffer()

        while (true) {
            // Code points, not chars, so text outside the BMP survives the round-trip.
            when (val codePoint = readCodePoint()) {
                '"'.code -> return buffer.readUtf8()

                '\\'.code -> when (val escaped = readChar()) {
                    '"', '\\', '/' -> buffer.writeUtf8CodePoint(escaped.code)
                    'b' -> buffer.writeUtf8CodePoint(8)
                    'f' -> buffer.writeUtf8CodePoint(12)
                    'n' -> buffer.writeUtf8CodePoint(10)
                    'r' -> buffer.writeUtf8CodePoint(13)
                    't' -> buffer.writeUtf8CodePoint(9)
                    'u' -> buffer.writeUtf8CodePoint(readUnicodeEscape())
                    else -> error("unsupported JSON escape: \\$escaped")
                }

                else -> buffer.writeUtf8CodePoint(codePoint)
            }
        }
    }

    /**
     * The next value as its literal text when it is a number (or a numeric string), null when it
     * is JSON `null`. Anything else is skipped and reported as null.
     */
    fun readNumberLiteralOrNull(): String? {
        val c = nextNonWhitespace()
        push(c)

        return when {
            c == '"' -> readString().takeIf { it.isNotEmpty() }
            c == '-' || c in '0'..'9' -> readNumberLiteral()
            c == 'n' -> {
                skipValue()
                null
            }

            else -> {
                skipValue()
                null
            }
        }
    }

    fun skipValue() {
        when (val c = nextNonWhitespace()) {
            '{' -> {
                while (hasNextMember()) {
                    readName()
                    skipValue()
                }
            }

            '[' -> {
                while (hasNextElement()) {
                    skipValue()
                }
            }

            '"' -> {
                push(c)
                readString()
            }

            else -> {
                push(c)
                skipLiteral()
            }
        }
    }

    private fun readNumberLiteral(): String {
        val out = StringBuilder()

        while (true) {
            val c = readCharOrNull() ?: break

            if (c.isNumberChar()) {
                out.append(c)
            } else {
                push(c)
                break
            }
        }

        return out.toString()
    }

    /** Consumes `true` / `false` / `null` (or a bare number) without interpreting it. */
    private fun skipLiteral() {
        while (true) {
            val c = readCharOrNull() ?: return

            if (c.isNumberChar() || c in 'a'..'z') continue

            push(c)
            return
        }
    }

    private fun readUnicodeEscape(): Int {
        var value = 0
        repeat(4) {
            val digit = readChar().digitToIntOrNull(16) ?: error("bad \\u escape in JSON")
            value = value * 16 + digit
        }
        return value
    }

    private fun nextNonWhitespace(): Char {
        while (true) {
            val c = readChar()
            if (!c.isJsonWhitespace()) return c
        }
    }

    private fun expect(expected: Char) {
        val c = nextNonWhitespace()
        if (c != expected) error("expected '$expected' but found '$c'")
    }

    private fun readChar(): Char = readCharOrNull() ?: error("unexpected end of JSON")

    /** Full code point, so string contents outside the BMP are not truncated. */
    private fun readCodePoint(): Int {
        if (peeked != NONE) {
            val c = peeked
            peeked = NONE
            return c
        }

        if (source.exhausted()) error("unexpected end of JSON")

        return source.readUtf8CodePoint()
    }

    private fun readCharOrNull(): Char? {
        if (peeked != NONE) {
            val c = peeked.toChar()
            peeked = NONE
            return c
        }

        if (source.exhausted()) return null

        // Structural JSON is ASCII; multi-byte text only appears inside strings, which are read
        // through readCodePoint().
        return source.readUtf8CodePoint().toChar()
    }

    private fun push(c: Char) {
        peeked = c.code
    }

    private fun Char.isJsonWhitespace(): Boolean = this == ' ' || this == '\n' || this == '\r' || this == '\t'

    private fun Char.isNumberChar(): Boolean =
        this in '0'..'9' || this == '-' || this == '+' || this == '.' || this == 'e' || this == 'E'

    private companion object {
        const val NONE = -1
    }
}
