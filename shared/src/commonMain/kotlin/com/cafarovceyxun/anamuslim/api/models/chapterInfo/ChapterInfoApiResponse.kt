package com.cafarovceyxun.anamuslim.api.models.chapterInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterInfoApiResponse(
    @SerialName("chapter_info") val chapterInfo: ChapterInfoModel
)

@Serializable
data class ChapterInfoModel(
    @SerialName("language_name") val languageName: String? = null,
    @SerialName("short_text") val shortText: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("language_code") val languageCode: String? = null
) {
    fun primaryContent(): String = text ?: shortText ?: ""
}
