package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
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
 * (`parçalanma1 (ixtilaf)`, `edin!1`, `(həsəd)1`, bəzən `tuturuq (1)`). Onları kiçik üst-indeks
 * kimi göstəririk. Nömrə sayılan: hərfə və ya söz bitirən işarəyə (`)`, `!`, `?`, dırnaq) boşluqsuz
 * yapışan 1–2 rəqəm, arxasında rəqəm və `:` olmayan. Ayə başlığı (`-198:`), istinad (`2:255`) və
 * adi say (`40 gün`) bu şərtə düşmür.
 */
private fun AnnotatedString.withFootnoteSuperscripts(): AnnotatedString {
    val ranges = findFootnoteMarkers(text)
    if (ranges.isEmpty()) return this
    return buildAnnotatedString {
        append(this@withFootnoteSuperscripts)
        ranges.forEach { range ->
            addStyle(footnoteStyle, range.first, range.last + 1)
        }
    }
}

private val footnoteStyle = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.65.em)

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

// `tuturuq (1)` → `tuturuq1`: mötərizəli nömrə də haşiyədir; sözə yapışdırılır ki, üst-indeks olsun.
private val parenFootnoteRegex = Regex("\\s?\\((\\d{1,2})\\)")

internal fun normalizeFootnoteMarkers(text: String): String =
    if ('(' in text) text.replace(parenFootnoteRegex, "$1") else text

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
