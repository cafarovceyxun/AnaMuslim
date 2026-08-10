package com.cafarovceyxun.anamuslim.utils.reader

import com.cafarovceyxun.anamuslim.utils.quran.QuranConstants

sealed interface RichTextPart {
    data class Plain(val text: String) : RichTextPart

    data class QuranRef(
        val text: String,
        val slugs: Set<String>,
        val chapter: Int,
        val verses: String
    ) : RichTextPart
}

/**
 * Splits a translation string into plain runs and `<reference>` links.
 *
 * `<fn>` (haşiyə) istinadları artıq göstərilmir: etiket də, içindəki nömrə də mətndən tamamilə atılır.
 *
 * Previously backed by `org.xmlpull.v1.XmlPullParser`; reimplemented in pure Kotlin for KMP.
 * Behaviour is kept identical to the old kXML2 pass for this narrow grammar: only the two known
 * tags carry meaning, text is XML-entity decoded, and — crucially — an **unknown or malformed
 * entity** (`&nbsp;`, a stray `&`, …) or a malformed tag makes the whole parse throw and fall back
 * to [stripTagsAndDecodeEntities], exactly as the old parser did in its `catch` block.
 */
fun parseTranslationText(html: String, slug: String): List<RichTextPart> {
    return try {
        scanMarkup(html, slug)
    } catch (e: Exception) {
        // Some translations/footnotes can contain malformed entities (e.g. stray '&').
        listOf(RichTextPart.Plain(stripTagsAndDecodeEntities(html)))
    }
}

private fun scanMarkup(html: String, slug: String): List<RichTextPart> {
    val parts = mutableListOf<RichTextPart>()
    val plainBuffer = StringBuilder()

    var currentTag: String? = null
    var currentText = StringBuilder()
    var currentChapter = -1
    var currentVerses = ""

    fun flushPlain() {
        if (plainBuffer.isNotEmpty()) {
            parts += RichTextPart.Plain(plainBuffer.toString())
            plainBuffer.clear()
        }
    }

    fun appendText(text: String) {
        if (currentTag == QuranConstants.REFERENCE_TAG || currentTag == QuranConstants.FOOTNOTE_REF_TAG) {
            currentText.append(text)
        } else {
            plainBuffer.append(text)
        }
    }

    fun startTag(name: String, attrs: Map<String, String>) {
        when (name) {
            QuranConstants.REFERENCE_TAG -> {
                flushPlain()
                currentTag = QuranConstants.REFERENCE_TAG
                currentText = StringBuilder()
                currentChapter =
                    attrs[QuranConstants.REFERENCE_ATTR_CHAPTER_NO]?.toIntOrNull() ?: -1
                currentVerses = attrs[QuranConstants.REFERENCE_ATTR_VERSES] ?: ""
            }

            // Haşiyə istinadı: mətnə heç nə əlavə etmirik, yalnız içindəki nömrəni udmaq üçün
            // cari etiket kimi qeyd edirik.
            QuranConstants.FOOTNOTE_REF_TAG -> {
                flushPlain()
                currentTag = QuranConstants.FOOTNOTE_REF_TAG
                currentText = StringBuilder()
            }
        }
    }

    fun endTag(name: String) {
        when (name) {
            QuranConstants.REFERENCE_TAG -> {
                parts += RichTextPart.QuranRef(
                    text = currentText.toString(),
                    slugs = setOf(slug),
                    chapter = currentChapter,
                    verses = currentVerses
                )
                currentTag = null
                currentText = StringBuilder()
            }

            QuranConstants.FOOTNOTE_REF_TAG -> {
                currentTag = null
                currentText = StringBuilder()
            }
        }
    }

    var i = 0
    val n = html.length
    while (i < n) {
        when (val c = html[i]) {
            '<' -> {
                val gt = html.indexOf('>', i + 1)
                if (gt < 0) error("unterminated tag")
                val raw = html.substring(i + 1, gt).trim()
                if (raw.isEmpty()) error("empty tag")

                if (raw.startsWith("/")) {
                    endTag(raw.substring(1).trim())
                } else {
                    val selfClose = raw.endsWith("/")
                    val body = if (selfClose) raw.dropLast(1).trim() else raw
                    val (name, attrs) = parseTagBody(body)
                    startTag(name, attrs)
                    if (selfClose) endTag(name)
                }
                i = gt + 1
            }

            '&' -> {
                val semi = html.indexOf(';', i + 1)
                if (semi < 0) error("unterminated entity")
                appendText(decodeEntity(html.substring(i + 1, semi)))
                i = semi + 1
            }

            else -> {
                var j = i + 1
                while (j < n && html[j] != '<' && html[j] != '&') j++
                appendText(html.substring(i, j))
                i = j
            }
        }
    }

    flushPlain()
    return parts
}

/** Parses `name attr="v" attr2='v2'` into the tag name and its attribute map. */
private fun parseTagBody(body: String): Pair<String, Map<String, String>> {
    val len = body.length
    var k = 0
    while (k < len && !body[k].isWhitespace()) k++
    val name = body.substring(0, k)

    val attrs = mutableMapOf<String, String>()
    while (k < len) {
        while (k < len && body[k].isWhitespace()) k++
        if (k >= len) break

        val nameStart = k
        while (k < len && body[k] != '=' && !body[k].isWhitespace()) k++
        val attrName = body.substring(nameStart, k)

        while (k < len && body[k].isWhitespace()) k++
        if (k < len && body[k] == '=') {
            k++
            while (k < len && body[k].isWhitespace()) k++
            if (k >= len || (body[k] != '"' && body[k] != '\'')) error("unquoted attribute value")
            val quote = body[k]
            k++
            val valueStart = k
            while (k < len && body[k] != quote) k++
            if (k >= len) error("unterminated attribute value")
            val value = body.substring(valueStart, k)
            k++
            if (attrName.isNotEmpty()) attrs[attrName] = decodeAllEntities(value)
        } else if (attrName.isNotEmpty()) {
            attrs[attrName] = ""
        }
    }

    return name to attrs
}

/** Decodes a single XML entity body (the text between `&` and `;`); throws on anything unknown. */
private fun decodeEntity(body: String): String = when {
    body == "amp" -> "&"
    body == "lt" -> "<"
    body == "gt" -> ">"
    body == "quot" -> "\""
    body == "apos" -> "'"
    body.startsWith("#x") || body.startsWith("#X") ->
        (body.substring(2).toIntOrNull(16) ?: error("bad hex entity: $body")).toChar().toString()
    body.startsWith("#") ->
        (body.substring(1).toIntOrNull() ?: error("bad numeric entity: $body")).toChar().toString()
    else -> error("unknown entity: $body")
}

private fun decodeAllEntities(text: String): String {
    if ('&' !in text) return text
    val out = StringBuilder(text.length)
    var i = 0
    val n = text.length
    while (i < n) {
        if (text[i] == '&') {
            val semi = text.indexOf(';', i + 1)
            if (semi < 0) error("unterminated entity")
            out.append(decodeEntity(text.substring(i + 1, semi)))
            i = semi + 1
        } else {
            out.append(text[i])
            i++
        }
    }
    return out.toString()
}

private val htmlTagRegex = Regex("<[^>]+>")
private val danglingAmpRegex = Regex("&(?!#\\d+;|#x[0-9a-fA-F]+;|[a-zA-Z]+;)")

private fun stripTagsAndDecodeEntities(raw: String): String {
    return raw
        .replace(htmlTagRegex, "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(danglingAmpRegex, "&amp;")
}
