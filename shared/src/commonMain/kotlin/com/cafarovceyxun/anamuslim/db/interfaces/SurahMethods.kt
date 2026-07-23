package com.cafarovceyxun.anamuslim.db.interfaces

interface SurahMethods {
    fun isVerseValid(verseNo: Int): Boolean

    fun isVerseRangeValid(fromVerse: Int, toVerse: Int): Boolean
}
