package com.cafarovceyxun.anamuslim.db

import com.cafarovceyxun.anamuslim.db.entities.quran.AyahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations

data class ChapterVerseBatch(
    val surah: SurahWithLocalizations,
    val ayahByVerseNo: Map<Int, AyahEntity>,
    val wordsByVerseNo: Map<Int, List<AyahWordEntity>>,
    val pageByVerseNo: Map<Int, Int>,
)
