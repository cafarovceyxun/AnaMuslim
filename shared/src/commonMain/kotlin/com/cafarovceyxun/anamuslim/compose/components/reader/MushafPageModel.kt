package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasGlyphPlacement

data class MushafLineLayout(
    val fittedStyle: TextStyle,
    val centeredGap: Dp,
)

sealed class QuranPageLineItem {
    abstract val lineNo: Int

    data class Title(
        override val lineNo: Int, val chapterNo: Int
    ) : QuranPageLineItem()

    data class Bismillah(
        override val lineNo: Int
    ) : QuranPageLineItem()

    data class Text(
        override val lineNo: Int,
        val centered: Boolean,
        val words: List<AyahWordEntity>,
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
        val layout: MushafLineLayout,
        /** Per-glyph tajweed classes keyed like [atlasPlacements]; empty when tajweed is off. */
        val tajweedClasses: Map<Int, ByteArray> = emptyMap(),
    ) : QuranPageLineItem()
}

data class QuranPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val lines: List<QuranPageLineItem>,
    val cacheKey: String,
)
