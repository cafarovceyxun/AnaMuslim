package com.cafarovceyxun.anamuslim.utils.translation

/**
 * İdxal mətnində dipnot **sözün dalınca, iki qıvrım mötərizədə** yazılır:
 * `Səni təqdis edirik{{Ayədə keçən “Nuqaddisu” kəliməsi …}}”.` Word faylından oxunanda həqiqi
 * dipnotlar avtomatik bu şəklə salınır ([docxXmlToImportText]), yapışdırılan mətnə isə əl ilə
 * yazmaq olar.
 *
 * Bazada isə dipnot iki yerdə yaşayır (`TextAnnotator.findFootnoteMarkers`, `tools/quran-notes`):
 * mətndə sözə **boşluqsuz yapışıq** rəqəm (`edirik1”.` → tətbiq üst-indeks çəkir), qeyd
 * sütununda `(1) - …` sətri. Nömrələr hər ayədə 1-dən başlayır — kitabdakı ümumi sıra (1…84)
 * iki rəqəmli marker həddini tez keçər və ayə daxilində mənasızdır.
 */
data class InlineFootnotes(
    val text: String,
    val notes: List<String>,
    /** Marker rəqəmin dalına düşüb (`19{{…}}` → `191`) — admin-ə xəbərdarlıq üçün. */
    val markerAfterDigit: Boolean = false,
)

const val FOOTNOTE_OPEN = "{{"
const val FOOTNOTE_CLOSE = "}}"

fun extractInlineFootnotes(raw: String): InlineFootnotes {
    if (!raw.contains(FOOTNOTE_OPEN)) return InlineFootnotes(raw, emptyList())

    val out = StringBuilder(raw.length)
    val notes = mutableListOf<String>()
    var afterDigit = false
    var i = 0
    while (i < raw.length) {
        val open = raw.indexOf(FOOTNOTE_OPEN, i)
        val close = if (open < 0) -1 else raw.indexOf(FOOTNOTE_CLOSE, open + FOOTNOTE_OPEN.length)
        if (open < 0 || close < 0) {
            out.append(raw, i, raw.length)
            break
        }
        out.append(raw, i, open)
        val body = raw.substring(open + FOOTNOTE_OPEN.length, close).collapseWhitespace()
        if (body.isNotEmpty()) {
            // Marker sözə yapışmalıdır, yoxsa üst-indeks çəkilmir: «sözü {{…}}» → «sözü1».
            while (out.isNotEmpty() && out.last().isWhitespace()) out.setLength(out.length - 1)
            if (out.isNotEmpty() && out.last().isDigit()) afterDigit = true
            notes += body
            out.append(notes.size)
        }
        i = close + FOOTNOTE_CLOSE.length
    }
    return InlineFootnotes(out.toString().trim(), notes, afterDigit)
}

/** `["a", "b"]` → `"(1) - a\n(2) - b"` — `tools/quran-notes/common.py` `compose_note` ilə eyni. */
fun composeFootnoteNote(notes: List<String>): String? =
    notes.takeIf { it.isNotEmpty() }
        ?.mapIndexed { index, note -> "(${index + 1}) - $note" }
        ?.joinToString("\n")

/** `{{` açılıb bağlanmayıbsa mətn olduğu kimi qalır — bunu admin görməlidir. */
fun hasUnclosedFootnote(raw: String): Boolean {
    var i = 0
    while (true) {
        val open = raw.indexOf(FOOTNOTE_OPEN, i)
        if (open < 0) return false
        val close = raw.indexOf(FOOTNOTE_CLOSE, open + FOOTNOTE_OPEN.length)
        if (close < 0) return true
        i = close + FOOTNOTE_CLOSE.length
    }
}

private val WHITESPACE = Regex("""\s+""")

private fun String.collapseWhitespace(): String = replace(WHITESPACE, " ").trim()
