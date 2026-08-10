/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.cafarovceyxun.anamuslim.components.transls

import com.cafarovceyxun.anamuslim.PlatformSerializable
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel

class TranslModel(val bookInfo: TranslationBookInfoModel) : TranslBaseModel(), PlatformSerializable {
    var isChecked = false
    var isDownloaded: Boolean = true
    var isDownloading: Boolean = false
    var isDownloadingDisabled: Boolean = false
    var miniInfos = ArrayList<String>()

    fun addMiniInfo(miniInfo: String) {
        miniInfos.add(miniInfo)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TranslModel) {
            return false
        }
        return other.bookInfo.slug == bookInfo.slug
    }

    override fun hashCode(): Int {
        var result = bookInfo.hashCode()
        result = 31 * result + isChecked.hashCode()
        result = 31 * result + isDownloading.hashCode()
        result = 31 * result + isDownloadingDisabled.hashCode()
        result = 31 * result + miniInfos.hashCode()
        return result
    }

    override fun toString(): String {
        return "TranslModel(bookInfo=$bookInfo, isChecked=$isChecked, isDownloading=$isDownloading, miniInfos=$miniInfos)"
    }
}
