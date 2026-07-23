package com.cafarovceyxun.anamuslim.db.relations

import com.cafarovceyxun.anamuslim.components.quran.subcomponents.Translation
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.db.interfaces.SurahMethods
import com.cafarovceyxun.anamuslim.utils.verse.VerseUtils

data class VerseWithDetails(
    val words: List<AyahWordEntity>,
    val pageNo: Int,
    val verse: AyahEntity,
    val chapter: SurahWithLocalizations
) : SurahMethods by chapter {
    val id get() = verse.ayahId
    val chapterNo get() = verse.surahNo
    val verseNo get() = verse.ayahNo

    var translations: List<Translation> = ArrayList()
    var includeChapterNameInSerial = false

    fun getTranslationCount() = translations.size

    fun isVOTD() = VerseUtils.isVOTD(chapterNo, verseNo)

    fun isIdealForVOTD(): Boolean {
        val arabicText = words.joinToString(" ") { it.text }
        return arabicText.length in 6..300
    }

    override fun toString(): String {
        return "VERSE ($id) -  $chapterNo:$verseNo"
    }
}
