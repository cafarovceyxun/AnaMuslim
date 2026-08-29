package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.ui.text.AnnotatedString
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations

open class TranslationPageVerse(
    val chapterNo: Int,
    val verseNo: Int,
    val rangeStart: Int,
    val rangeEnd: Int,
)

sealed class TranslationPageSection {
    object Divider : TranslationPageSection()
    data class Title(val swl: SurahWithLocalizations) : TranslationPageSection()
    object Bismillah : TranslationPageSection()

    data class Text(
        val annotatedText: AnnotatedString,
        val verses: List<TranslationPageVerse>,
    ) : TranslationPageSection()
}

data class TranslationPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val hizbNos: List<Int>,
    val chapterNames: String,
    val translationSlug: String,
    val sections: List<TranslationPageSection>,
)

/**
 * Kitab rejiminin bir səhifəsi.
 *
 * Səhifələmə tərcümə rejimindən gəlir (eyni müshəf səhifə nömrələri, eyni prefetch), məzmun isə
 * ayə-ayə rejiminin qurduğu elementlərdir — ona görə burada ayrıca mətn modeli yoxdur, sadəcə
 * [ReaderPreparedData] səhifə başlığı üçün lazım olan iki sahə ilə bükülür.
 */
data class BookPageItem(
    val pageNo: Int,
    val chapterNames: String,
    val prepared: ReaderPreparedData,
)
