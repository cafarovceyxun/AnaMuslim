package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.cafarovceyxun.anamuslim.components.quran.subcomponents.Translation
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.utils.quran.QuranConstants
import com.cafarovceyxun.anamuslim.utils.text.withSearchHighlight

fun buildTranslationAnnotatedString(
    translation: Translation,
    colorScheme: ColorScheme,
    actions: VerseActions?,
    highlightParentheses: Boolean = true,
    showParentheses: Boolean = true,
    searchQuery: String? = null,
): AnnotatedString {
    // early check for potential clickables
    if (actions == null) {
        val raw = normalizeFootnoteMarkers(translation.text)
        if ('<' !in raw) {
            return if (showParentheses) {
                buildAnnotatedString {
                    appendWithParentheses(raw, if (highlightParentheses) Color(0xFFE53935) else Color.Unspecified)
                }
            } else {
                AnnotatedString(removeParentheses(raw))
            }.withFootnoteSuperscripts().withSearchHighlight(searchQuery)
        }
    }

    return buildTranslationAnnotatedString(
        parseTranslationText(normalizeFootnoteMarkers(translation.text), translation.bookSlug),
        colorScheme,
        actions,
        highlightParentheses,
        showParentheses,
        searchQuery,
    )
}

fun buildTranslationAnnotatedString(
    text: String,
    slug: String,
    colorScheme: ColorScheme,
    actions: VerseActions?,
    highlightParentheses: Boolean = true,
    showParentheses: Boolean = true,
    searchQuery: String? = null,
): AnnotatedString {
    return buildTranslationAnnotatedString(
        parseTranslationText(normalizeFootnoteMarkers(text), slug),
        colorScheme,
        actions,
        highlightParentheses,
        showParentheses,
        searchQuery,
    )
}

/**
 * @param searchQuery axtarışdan gəlirsə sorğu: hazır mətnin üstündən uyğun gələn sözlər sarı fonla
 *   işarələnir ([withSearchHighlight]). Vurğu **ən sonda**, mətn tam qurulandan sonra qoyulur — ayə
 *   istinadları, mötərizə rəngi və tərcüməçi sətri onsuz da öz üslublarını saxlayır, sarı fon isə
 *   onların üstünə düşür. `null` = adi oxuma, heç nə dəyişmir.
 */
fun buildTranslationAnnotatedString(
    parts: List<RichTextPart>,
    colorScheme: ColorScheme,
    actions: VerseActions?,
    highlightParentheses: Boolean = true,
    showParentheses: Boolean = true,
    searchQuery: String? = null,
): AnnotatedString {
    return buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is RichTextPart.Plain -> {
                    if (showParentheses) {
                        appendWithParentheses(
                            part.text,
                            if (highlightParentheses) Color(0xFFE53935) else Color.Unspecified
                        )
                    } else {
                        append(removeParentheses(part.text))
                    }
                }

                is RichTextPart.QuranRef -> {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = QuranConstants.REFERENCE_TAG,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = colorScheme.primary,
                                    background = colorScheme.primary.alpha(0.1f),
                                    fontWeight = FontWeight.Medium
                                ),
                                pressedStyle = SpanStyle(
                                    color = colorScheme.primary,
                                    background = colorScheme.primary.alpha(0.5f),
                                    fontWeight = FontWeight.Medium
                                ),
                            ),
                        ) {
                            actions?.onReferenceClick(part.slugs, part.chapter, part.verses)
                        }
                    ) {
                        append(part.text)
                    }
                }
            }
        }
    }.withFootnoteSuperscripts().withSearchHighlight(searchQuery)
}

/**
 * Azərbaycanca tərcümədə haşiyə nömrələri etiketsiz, sözə yapışıq adi rəqəm kimi gəlir
 * (`parçalanma1 (ixtilaf)`, `edin!1`, `(həsəd)1`, bəzən `tuturuq (1)`). Onları mətnin içində
 * **nişan** kimi göstəririk: yaşıl halqa (içi boş), içində ağ rəqəm. Nömrə sayılan: hərfə və ya
 * söz bitirən işarəyə (`)`, `!`, `?`, dırnaq) boşluqsuz yapışan 1–2 rəqəm, arxasında rəqəm və `:`
 * olmayan. Ayə başlığı (`-198:`), istinad (`2:255`) və adi say (`40 gün`) bu şərtə düşmür.
 *
 * Span dairə çəkə bilmir (fon yalnız düzbucaqlıdır), ona görə rəqəm **inline content** yeridir:
 * mətni çəkən `Text` `inlineContent = rememberFootnoteInlineContent()` almalıdır
 * (`FootnoteBadge.kt`). ⚠️ Almayan `Text` çökmür, amma rəqəmi adi mətn kimi, sözə yapışıq göstərir —
 * bu funksiyanın nəticəsini yeni yerdə çəkəndə xəritəni də ver. Seçib kopyalayanda rəqəm özü gedir.
 */
internal fun AnnotatedString.withFootnoteSuperscripts(): AnnotatedString {
    // [FORCED_FOOTNOTE] ilə işarələnmiş nömrələr evristikadan keçmir — onları mənbəyin öz qaydası
    // (hədislərdə «(N)», [markParenthesizedFootnotes]) artıq dipnot sayıb. İşarənin özü atılır.
    val forced = findForcedFootnotes(text)
    val ranges = (findFootnoteMarkers(text) + forced).sortedBy { it.first }
    if (ranges.isEmpty()) return this
    val forcedStarts = forced.mapTo(HashSet()) { it.first }
    val source = this
    return buildAnnotatedString {
        var last = 0
        ranges.forEach { range ->
            val cut = if (range.first in forcedStarts) range.first - 1 else range.first
            // `subSequence` span-ları və keçidləri (ayə istinadı) öz hissəsində saxlayır.
            append(source.subSequence(last, cut))
            val digits = source.text.substring(range.first, range.last + 1)
            // Söz birləşdirici (U+2060): onsuz sətir sözlə nişanın arasından qırılır və nişan
            // növbəti sətrin başında tək qalır (yer simvolu U+FFFC-dən əvvəl qırılma icazəlidir).
            append(WORD_JOINER)
            appendInlineContent(footnoteInlineId(digits), digits)
            last = range.last + 1
        }
        append(source.subSequence(last, source.length))
    }
}

private const val WORD_JOINER = "\u2060"

/**
 * «Bu rəqəm dipnotdur» işarəsi (Unicode şəxsi istifadə sahəsi, mətndə heç vaxt olmur). Yalnız
 * normallaşdırma ilə nişan qurulması arasında yaşayır — ekrana çıxmır.
 */
private const val FORCED_FOOTNOTE = '\uE000'

private fun findForcedFootnotes(text: String): List<IntRange> {
    if (FORCED_FOOTNOTE !in text) return emptyList()
    val result = mutableListOf<IntRange>()
    var i = text.indexOf(FORCED_FOOTNOTE)
    while (i >= 0) {
        var j = i + 1
        while (j < text.length && text[j].isDigit()) j++
        if (j > i + 1) result += (i + 1) until j
        i = text.indexOf(FORCED_FOOTNOTE, j)
    }
    return result
}

/**
 * Hədis mətnində **hər** «(N)» (1–2 rəqəm) dipnotdur — 2026-11-15-də bütün cədvəl yoxlandı:
 * 3998 hədisdə 621 belə yer var, 605-i qeyddəki «N: …» sətrinə uyğundur, qalan 16-sı da nömrəsiz
 * yazılmış qeydə aiddir; «( 1 )» forması yoxdur, ayə istinadı («(Duha, 1-3)») bu formaya düşmür.
 * Önündəki işarə isə hər cürdür: hərf (384), cümlə sonu `". (1).` (45), dırnaq, `}`, `)`, `!`, `?`.
 * Quran tərcüməsindəki evristika ([normalizeFootnoteMarkers]) cümlə sonunu qəsdən buraxır, ona görə
 * hədis öz qaydası ilə işarələnir və nişan önündəki işarədən asılı olmadan qurulur.
 *
 * Önündəki boşluq (sətir keçidi yox) silinir ki, nişan sözə/işarəyə yapışsın.
 */
internal fun markParenthesizedFootnotes(text: String): String =
    if ('(' !in text) text
    else hadithParenFootnoteRegex.replace(text) { "$FORCED_FOOTNOTE${it.groupValues[1]}" }

private val hadithParenFootnoteRegex = Regex("""[ \t\u00A0]*\((\d{1,2})\)""")

/**
 * Mötərizəsiz, cümlə sonundan sonra tək duran nömrə: `gördüm". 1.` (hədis 298, 2026-11-15 redaktəsi).
 * Belə rəqəm ancaq hədisin qeydində eyni nömrəli sətir («1: …») olanda dipnot sayılır — yoxsa «2.
 * bənd», «. 40 gün» kimi adi saylar da nişana çevrilərdi. Nömrədən sonra yalnız durğu/boşluq/son.
 */
internal fun markBareFootnotes(text: String, noteNumbers: Set<Int>): String =
    if (noteNumbers.isEmpty()) text
    else bareFootnoteRegex.replace(text) { m ->
        val n = m.groupValues[2]
        if (n.toInt() in noteNumbers) "${m.groupValues[1]}$FORCED_FOOTNOTE$n" else m.value
    }

private val bareFootnoteRegex =
    Regex("""([.!?"”»})])[ \t\u00A0]+(\d{1,2})(?=[.,;]?(?:\s|$))""")

/**
 * Hədis qeydindəki nömrələr: sətir başında `1:`, `1-`, `1.`, `(1)`, `1)` (2026-11-15 cədvəlində
 * 431 qeyd «N:» formasındadır). Qeyd nömrəsizdirsə boş çoxluq.
 */
internal fun hadithNoteNumbers(note: String?): Set<Int> =
    if (note.isNullOrBlank()) emptySet()
    else noteNumberRegex.findAll(note).mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()

private val noteNumberRegex = Regex("""(?m)^[\s\-–•*]*\(?(\d{1,2})\)?\s*[:\-–.)]""")

/** Nişan xəritəsinin açarı — `1` və `01` eyni nişandır. */
internal fun footnoteInlineId(digits: String): String = "footnote:${digits.toIntOrNull() ?: 0}"

internal fun findFootnoteMarkers(text: String): List<IntRange> {
    val result = mutableListOf<IntRange>()
    var i = 0
    val n = text.length
    while (i < n) {
        if (!text[i].isDigit()) {
            i++
            continue
        }
        var j = i
        while (j < n && text[j].isDigit()) j++
        val prev = if (i > 0) text[i - 1] else ' '
        val next = if (j < n) text[j] else ' '
        if (j - i <= 2 && (prev.isLetter() || prev in FOOTNOTE_ANCHORS) && next != ':' && !next.isLetter()) {
            result += i until j
        }
        i = j
    }
    return result
}

private const val FOOTNOTE_ANCHORS = ")!?”’\"»"

// `tuturuq (1)` → `tuturuq1`: mötərizəli nömrə də haşiyədir; sözə yapışdırılır ki, nişan olsun.
private val parenFootnoteRegex = Regex("\\s?\\((\\d{1,2})\\)")

private const val SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

/**
 * Haşiyə nömrəsinin üç yazılışını bir formaya — sözə yapışıq adi rəqəmə — salır, ki
 * [findFootnoteMarkers] onu tanısın:
 * - `tuturuq (1)` / `”Unzurnə”(2)` → `tuturuq1` / `”Unzurnə”2` (hədislərin əksəriyyəti belədir);
 * - `yaxınlıq¹` → `yaxınlıq1` (əl ilə yazılmış Unicode üst-indeks).
 *
 * Mötərizəli nömrə **yalnız** önündə hərf və ya söz bitirən işarə olanda yapışdırılır. Hədislərdə
 * `etdim". (1).` kimi cümlədən sonrakı nömrə də var — yapışdırılsaydı `etdim".1.` olardı, nişan
 * çəkilməzdi və oxucu mötərizəsiz, mənasız rəqəm görərdi; belə yer olduğu kimi `(1)` qalır.
 */
internal fun normalizeFootnoteMarkers(text: String): String {
    var out = text
    if (out.any { it in SUPERSCRIPT_DIGITS }) {
        out = buildString(out.length) {
            out.forEach { c ->
                val i = SUPERSCRIPT_DIGITS.indexOf(c)
                append(if (i >= 0) '0' + i else c)
            }
        }
    }
    if ('(' !in out) return out
    val source = out
    return parenFootnoteRegex.replace(source) { match ->
        val prev = source.getOrNull(match.range.first - 1) ?: ' '
        if (prev.isLetter() || prev in FOOTNOTE_ANCHORS) match.groupValues[1] else match.value
    }
}

// `[\s\S]` matches any char including newlines, so this needs no RegexOption — DOT_MATCHES_ALL is
// JVM/Native-only and unavailable in the common stdlib (breaks commonMain metadata / IDE analysis).
private val parenthesesRegex = Regex("\\(([\\s\\S]*?)\\)")

private fun removeParentheses(text: String): String {
    return text.replace(parenthesesRegex, "").replace(Regex("\\s+"), " ").trim()
}

private fun AnnotatedString.Builder.appendWithParentheses(text: String, color: Color) {
    var lastIndex = 0
    parenthesesRegex.findAll(text).forEach { matchResult ->
        append(text.substring(lastIndex, matchResult.range.first))
        if (color != Color.Unspecified) {
            withStyle(SpanStyle(color = color)) {
                append(matchResult.value)
            }
        } else {
            append(matchResult.value)
        }
        lastIndex = matchResult.range.last + 1
    }
    append(text.substring(lastIndex))
}
