package com.cafarovceyxun.anamuslim.components.quran.subcomponents

import com.cafarovceyxun.anamuslim.PlatformSerializable

class Translation(
    var id: Int = 0,
    var bookSlug: String = "",
    var ayahId: Int = 0,
    var chapterNo: Int = 0,
    var verseNo: Int = 0,
    var text: String = "",
    var textSimple: String? = null,
    var note: String = "",
) : PlatformSerializable
