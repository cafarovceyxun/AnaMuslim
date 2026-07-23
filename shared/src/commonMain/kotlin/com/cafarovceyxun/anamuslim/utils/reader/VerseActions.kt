package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.runtime.staticCompositionLocalOf
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails

typealias OnReferenceClick = (slugs: Set<String>, chapterNo: Int, verses: String) -> Unit
typealias OnVerseOption = (verse: VerseWithDetails) -> Unit
typealias OnBookmarkRequest = (chapterNo: Int, verseRange: IntRange) -> Unit
typealias OnShareRequest = (verse: VerseWithDetails) -> Unit
typealias OnReportRequest = (verse: VerseWithDetails) -> Unit
typealias OnSimilarVersesRequest = (verse: VerseWithDetails) -> Unit

data class VerseActions(
    val onReferenceClick: OnReferenceClick,
    val onVerseOption: OnVerseOption? = null,
    val onBookmarkRequest: OnBookmarkRequest? = null,
    val onShareRequest: OnShareRequest? = null,
    val onReportRequest: OnReportRequest? = null,
    val onSimilarVerses: OnSimilarVersesRequest? = null,
)

val LocalVerseActions = staticCompositionLocalOf<VerseActions> {
    error("VerseActions not provided")
}
