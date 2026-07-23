package com.cafarovceyxun.anamuslim.components.reader

import com.cafarovceyxun.anamuslim.PlatformSerializable
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta

data class ChapterVersePair(val chapterNo: Int, val verseNo: Int) : PlatformSerializable {
    constructor(verse: VerseWithDetails) : this(verse.chapterNo, verse.verseNo)

    val isValid: Boolean get() = chapterNo > 0 && verseNo > 0

    fun doesEqual(verse: VerseWithDetails): Boolean {
        return doesEqual(verse.chapterNo, verse.verseNo)
    }

    fun doesEqual(ayahId: Int): Boolean {
        val pair = QuranMeta.getVerseNoFromAyahId(ayahId)

        return doesEqual(pair.first, pair.second)
    }

    fun doesEqual(chapterNo: Int, verseNo: Int): Boolean {
        return chapterNo == this.chapterNo && verseNo == this.verseNo
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChapterVersePair) return false

        return this.chapterNo == other.chapterNo && this.verseNo == other.verseNo
    }

    override fun toString(): String {
        return "($chapterNo:$verseNo)"
    }

    override fun hashCode(): Int {
        var result = chapterNo
        result = 31 * result + verseNo
        return result
    }

    companion object {
        val NONE = ChapterVersePair(
            chapterNo = -1,
            verseNo = -1
        )
    }
}
