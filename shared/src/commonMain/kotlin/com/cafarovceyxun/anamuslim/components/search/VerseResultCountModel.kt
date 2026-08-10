package com.cafarovceyxun.anamuslim.components.search

import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import kotlin.jvm.JvmField

class VerseResultCountModel(val bookInfo: TranslationBookInfoModel?) : SearchResultModelBase() {
    @JvmField
    var resultCount = 0
}
