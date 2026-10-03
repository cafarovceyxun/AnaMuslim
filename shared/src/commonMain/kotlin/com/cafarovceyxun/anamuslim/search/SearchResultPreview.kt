package com.cafarovceyxun.anamuslim.search

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.cafarovceyxun.anamuslim.utils.text.SearchHighlightStyle
import com.cafarovceyxun.anamuslim.utils.text.foldSearchTextWithOffsets
import com.cafarovceyxun.anamuslim.utils.text.searchMatchRanges

/**
 * Axtarış nəticəsi kartının önizləməsi: ilk uyğunluğun ətrafından bir pəncərə, uyğunluqlar
 * vurğulanmış. Quran, tərcümə və hədis nəticələri ([SearchPagingSource], [HadithSearch]) bir qaydadan.
 */
internal fun highlightMatches(text: String, rawQuery: String): AnnotatedString {
    val ellipsis = "…"

    val source = text
    // Uyğunluqlar diakritiksiz nüsxədə axtarılır, sonra orijinalın ofsetlərinə qaytarılır ki,
    // önizləmə mətni yazıldığı kimi göstərsin. Bu olmasa hər hərəkəsi yerində duran hədis
    // `text_ar`-ı sətri tapır, amma önizləmədə heç nə vurğulanmır və mətnin əvvəli göstərilirdi.
    // Eyni funksiya oxucudakı vurğunu da qurur ([withSearchHighlight]) — iki yer bir qaydadan.
    val folded = foldSearchTextWithOffsets(source).first

    // Harakat are characters too: 180 raw characters of muṣḥaf text carry barely half the words
    // of 180 characters of translation. The window is scaled by the text's own mark density so
    // every preview reads about the same length; for text without marks the scale is 1.
    val markScale = if (folded.isNotEmpty()) source.length.toDouble() / folded.length else 1.0
    val contextWindow = (180 * markScale).toInt()
    val sidePadding = (48 * markScale).toInt()

    val merged = searchMatchRanges(source, rawQuery)

    if (merged.isEmpty()) {
        if (text.length <= contextWindow) return buildAnnotatedString { append(text) }

        return buildAnnotatedString {
            append(text.take(contextWindow).trimEnd())
            append(ellipsis)
        }
    }

    val firstHit = merged.first()
    val sliceStart = maxOf(0, firstHit.first - sidePadding)
    val sliceEndExclusive = minOf(source.length, sliceStart + contextWindow)

    val prefix = if (sliceStart > 0) ellipsis else ""
    val suffix = if (sliceEndExclusive < source.length) ellipsis else ""

    val rawSlice = source.substring(sliceStart, sliceEndExclusive)
    val leadingTrimCount = rawSlice.length - rawSlice.trimStart().length
    val visibleText = rawSlice.trimStart().trimEnd()
    val contentStartInSource = sliceStart + leadingTrimCount

    val highlightStyle = SearchHighlightStyle

    return buildAnnotatedString {
        append(prefix)
        append(visibleText)
        append(suffix)

        val textOffset = prefix.length

        for (range in merged) {
            val clippedStart = maxOf(range.first, sliceStart)
            val clippedEndExclusive = minOf(range.last + 1, sliceEndExclusive)
            if (clippedStart >= clippedEndExclusive) continue

            val startInVisible = clippedStart - contentStartInSource
            val endInVisible = clippedEndExclusive - contentStartInSource
            val styleStart = textOffset + maxOf(0, startInVisible)
            val styleEnd = textOffset + minOf(visibleText.length, endInVisible)
            if (styleStart >= styleEnd) continue

            addStyle(
                style = highlightStyle,
                start = styleStart,
                end = styleEnd,
            )
        }
    }
}
