package com.cafarovceyxun.anamuslim.utils.translation

import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.openZip

/**
 * İdxal faylını mətnə çevirir: Word (`.docx`) faylı zip arxividir və onu birbaşa mətn kimi oxumaq
 * mənasız simvollar verirdi; düz mətn faylı olduğu kimi qalır.
 *
 * Word-dən çıxan mətndə hər abzas boş sətirlə ayrılır, həqiqi dipnotlar isə dayandıqları yerdə
 * `{{…}}` kimi yazılır ([extractInlineFootnotes] onları sonra nömrə + qeydə çevirir). Mətn sahəsinə
 * düşən nəticə admin-ə görünür və redaktə oluna bilir — çevirmə heç nə yükləmir.
 *
 * `null` — fayl oxunmadı (pozuq arxiv, `document.xml` yoxdur).
 */
suspend fun readImportDocument(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
    if (!bytes.isZip()) {
        return@withContext bytes.decodeToString().removePrefix("\uFEFF").takeIf { it.looksLikeText() }
    }

    // okio zip-i yalnız fayl sistemindən açır; müvəqqəti fayl iş bitən kimi silinir.
    val temp = AppFileSystem.appFilesDir() / "translation_import.docx"
    try {
        AppFileSystem.write(temp) { it.write(bytes) }
        val zip = AppFileSystem.fileSystem.openZip(temp)
        val document = zip.readEntry("word/document.xml") ?: return@withContext null
        docxXmlToImportText(
            documentXml = document,
            footnotesXml = zip.readEntry("word/footnotes.xml"),
            endnotesXml = zip.readEntry("word/endnotes.xml"),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.saveError(e, "readImportDocument")
        null
    } finally {
        AppFileSystem.delete(temp)
    }
}

/**
 * Seçici bütün faylları göstərir, ona görə PDF/şəkil də gələ bilər — UTF-8 kimi açılanda onlar
 * `\u0000` və əvəzetmə simvolu (`\uFFFD`) ilə dolur. Belə mətn mətn sahəsinə düşməsin.
 */
internal fun String.looksLikeText(): Boolean {
    if (isEmpty()) return false
    val bad = count { it == '\u0000' || it == '\uFFFD' }
    return bad * 100 <= length
}

private fun ByteArray.isZip(): Boolean =
    size >= 4 && this[0] == 'P'.code.toByte() && this[1] == 'K'.code.toByte() &&
        this[2] == 3.toByte() && this[3] == 4.toByte()

private fun FileSystem.readEntry(path: String): String? {
    val entry = "/$path".toPath()
    return if (exists(entry)) read(entry) { readUtf8() } else null
}

/**
 * `word/document.xml`-dən abzas mətni. XML kitabxanası ortaq kodda yoxdur, WordprocessingML-in
 * bizə lazım olan hissəsi isə kiçikdir: abzas (`w:p`), mətn qaçışı (`w:t`), tab/sətir keçidi və
 * dipnot istinadı (`w:footnoteReference`). Silinmiş düzəliş (`w:delText`), sahə kodu
 * (`w:instrText`) və `mc:Fallback`-dakı dublikat mətn oxunmur.
 */
internal fun docxXmlToImportText(
    documentXml: String,
    footnotesXml: String? = null,
    endnotesXml: String? = null,
): String {
    val footnotes = footnotesXml?.let { notesById(it, "w:footnote") }.orEmpty()
    val endnotes = endnotesXml?.let { notesById(it, "w:endnote") }.orEmpty()

    return paragraphs(documentXml) { tag, attrs ->
        val id = ID_ATTR.find(attrs)?.groupValues?.get(1)
        when (tag) {
            "w:footnoteReference" -> footnotes[id]
            "w:endnoteReference" -> endnotes[id]
            else -> null
        }?.let { "$FOOTNOTE_OPEN$it$FOOTNOTE_CLOSE" }
    }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

/**
 * `footnotes.xml` → id → bir sətirlik mətn. Ayırıcı xətlər (`w:type="separator"`) atılır.
 * `<w:footnote ` (boşluqla) axtarılır ki, `<w:footnotes>` kökü və `<w:footnotePr>` tutulmasın.
 */
private fun notesById(xml: String, element: String): Map<String, String> {
    val open = "<$element "
    val close = "</$element>"
    val result = mutableMapOf<String, String>()
    var from = 0
    while (true) {
        val start = xml.indexOf(open, from)
        if (start < 0) break
        val attrsEnd = xml.indexOf('>', start)
        val end = xml.indexOf(close, start)
        if (attrsEnd < 0 || end < 0) break
        val attrs = xml.substring(start + open.length, attrsEnd)
        from = end + close.length
        if (attrs.contains("w:type=")) continue
        val id = ID_ATTR.find(attrs)?.groupValues?.get(1) ?: continue
        val text = paragraphs(xml.substring(attrsEnd + 1, end)) { _, _ -> null }
            .joinToString(" ")
            .replace(WHITESPACE_RUN, " ")
            .trim()
        if (text.isNotEmpty()) result[id] = text
    }
    return result
}

/**
 * XML-i teq-teq gəzir və abzasları qaytarır. [inline] boş elementlər (`<w:x …/>`) üçün mətn
 * qaytara bilər — dipnot istinadları belə yerinə qoyulur.
 *
 * ⚠️ Əl ilə skan edilir, `Regex.findAll` ilə yox: Kotlin hər uyğunluq üçün yeni `Matcher` qurur,
 * Android-də (ICU) isə hər `Matcher` bütün girişi native yaddaşa köçürür. 193 KB-lıq
 * `document.xml`-də on minlərlə teq var — telefonda çevirmə dəqiqələrlə çəkir və fasiləsiz
 * `NativeAlloc GC` verirdi, JVM testində isə dərhal bitirdi.
 */
private fun paragraphs(xml: String, inline: (tag: String, attrs: String) -> String?): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inText = false
    var skipDepth = 0
    var i = 0
    val n = xml.length

    while (i < n) {
        if (xml[i] != '<') {
            val next = xml.indexOf('<', i).let { if (it < 0) n else it }
            if (inText && skipDepth == 0) current.append(decodeEntities(xml.substring(i, next)))
            i = next
            continue
        }
        val end = xml.indexOf('>', i)
        if (end < 0) break
        var nameStart = i + 1
        val closing = nameStart < end && xml[nameStart] == '/'
        if (closing) nameStart++
        val selfClosing = end > nameStart && xml[end - 1] == '/'
        val contentEnd = if (selfClosing) end - 1 else end
        var nameEnd = nameStart
        while (nameEnd < contentEnd && !xml[nameEnd].isWhitespace()) nameEnd++
        val tag = xml.substring(nameStart, nameEnd)
        i = end + 1

        if (tag in SKIPPED) {
            if (!selfClosing) skipDepth += if (closing) -1 else 1
            continue
        }
        if (skipDepth > 0) continue

        when {
            tag == "w:t" -> inText = !closing && !selfClosing
            tag == "w:p" && (closing || selfClosing) -> {
                result += current.toString().trim()
                current.clear()
            }
            closing -> Unit
            tag == "w:tab" -> current.append(' ')
            tag == "w:br" || tag == "w:cr" -> current.append('\n')
            tag == "w:noBreakHyphen" -> current.append('-')
            selfClosing -> inline(tag, xml.substring(nameEnd, contentEnd))?.let { current.append(it) }
        }
    }
    if (current.isNotBlank()) result += current.toString().trim()
    return result
}

private val ID_ATTR = Regex("""\bw:id="(-?\d+)"""")
private val WHITESPACE_RUN = Regex("""\s+""")

/** İçindəki mətn oxunmayan elementlər — ya dublikatdır, ya da görünən mətn deyil. */
private val SKIPPED = setOf("mc:Fallback", "w:instrText", "w:delText", "w:fldData")

private val ENTITY = Regex("""&(#x[0-9A-Fa-f]+|#\d+|amp|lt|gt|quot|apos);""")

private fun decodeEntities(text: String): String =
    if ('&' !in text) text else ENTITY.replace(text) { match ->
        when (val name = match.groupValues[1]) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            else -> {
                val code = if (name.startsWith("#x")) name.drop(2).toIntOrNull(16) else name.drop(1).toIntOrNull()
                code?.let { codePointToString(it) } ?: match.value
            }
        }
    }

private fun codePointToString(code: Int): String =
    if (code < 0x10000) code.toChar().toString()
    else {
        val v = code - 0x10000
        charArrayOf((0xD800 + (v shr 10)).toChar(), (0xDC00 + (v and 0x3FF)).toChar()).concatToString()
    }
