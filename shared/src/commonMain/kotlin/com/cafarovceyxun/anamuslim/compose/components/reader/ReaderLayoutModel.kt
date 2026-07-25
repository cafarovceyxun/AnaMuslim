package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import com.cafarovceyxun.anamuslim.db.entities.wbw.WbwWordEntity
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasGlyphPlacement

sealed class ReaderLayoutItem() {
    abstract val key: String

    data class ChapterInfo(val chapterNo: Int, override val key: String) : ReaderLayoutItem()
    data class Bismillah(override val key: String) : ReaderLayoutItem()
    data class IsVotd(override val key: String) : ReaderLayoutItem()
    data class ChapterTitle(val chapterNo: Int, override val key: String) : ReaderLayoutItem()

    data class TranslationUI(
        val slug: String,
        val langCode: String,
        val text: AnnotatedString,
        val rawText: String,
        val note: String? = null,
        val fontSize: TextUnit = TextUnit.Unspecified
    )

    data class VerseUI(
        val verse: VerseWithDetails,
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
        val parsedTranslationTexts: List<TranslationUI> = emptyList(),
        val wbwByWordIndex: Map<Int, WbwWordEntity>? = null,
        val showDivider: Boolean = true,
        /** Per-glyph tajweed classes keyed like [atlasPlacements]; empty when tajweed is off. */
        val tajweedClasses: Map<Int, ByteArray> = emptyMap(),
        override val key: String
    ) : ReaderLayoutItem()

    data class SectionMarker(
        val text: String,
        val chapterNo: Int? = null,
        override val key: String,
    ) : ReaderLayoutItem()
}

data class ReaderPreparedData(
    val items: List<ReaderLayoutItem>,
    /** Quran text style per mushaf page for Arabic in this reader session. */
    val textStyles: Map<Int, TextStyle> = emptyMap(),
)
