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

fun buildTranslationAnnotatedString(
    translation: Translation,
    colorScheme: ColorScheme,
    actions: VerseActions?,
    highlightParentheses: Boolean = true,
    showParentheses: Boolean = true,
): AnnotatedString {
    // early check for potential clickables
    if (actions == null) {
        val raw = translation.text
        if ('<' !in raw) {
            return if (showParentheses) {
                buildAnnotatedString {
                    appendWithParentheses(raw, if (highlightParentheses) Color(0xFFE53935) else Color.Unspecified)
                }
            } else {
                AnnotatedString(removeParentheses(raw))
            }
        }
    }

    return buildTranslationAnnotatedString(
        parseTranslationText(translation.text, translation.bookSlug),
        colorScheme,
        actions,
        highlightParentheses,
        showParentheses
    )
}

fun buildTranslationAnnotatedString(
    text: String,
    slug: String,
    colorScheme: ColorScheme,
    actions: VerseActions?,
    highlightParentheses: Boolean = true,
    showParentheses: Boolean = true,
): AnnotatedString {
    return buildTranslationAnnotatedString(
        parseTranslationText(text, slug),
        colorScheme,
        actions,
        highlightParentheses,
        showParentheses
    )
}

fun buildTranslationAnnotatedString(
    parts: List<RichTextPart>,
    colorScheme: ColorScheme,
    actions: VerseActions?,
    highlightParentheses: Boolean = true,
    showParentheses: Boolean = true,
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
