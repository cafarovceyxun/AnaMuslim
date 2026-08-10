package com.cafarovceyxun.anamuslim.api

import com.cafarovceyxun.anamuslim.api.models.chapterInfo.ChapterInfoApiResponse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

object AlfaazPlusApi {
    suspend fun getChapterInfo(
        chapterNo: Int,
        language: String,
        id: Int?,
    ): ChapterInfoApiResponse {
        val base = ApiConfig.ALFAAZPLUS_API_ROOT_URL.trimEnd('/')
        val text = NetworkClient.client.get("$base/quran/chapters/$chapterNo/info") {
            url {
                parameters.append("language", language)
                if (id != null) parameters.append("id", id.toString())
            }
        }.bodyAsText()
        return JsonHelper.json.decodeFromString(text)
    }
}
