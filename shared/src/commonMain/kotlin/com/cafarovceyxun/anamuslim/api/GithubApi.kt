package com.cafarovceyxun.anamuslim.api

import com.cafarovceyxun.anamuslim.api.models.AppUpdate
import com.cafarovceyxun.anamuslim.api.models.ResourcesVersions
import com.cafarovceyxun.anamuslim.api.models.wbw.AvailableWbwInfoModel
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * Inventory endpoints served from the active download-source mirror (gh-proxy / raw / jsdelivr).
 * [baseUrl] is updated by the app's `DownloadSourceUtils` when the user switches mirror.
 *
 * Typed responses are decoded explicitly from text (not via ContentNegotiation) to mirror the
 * previous Retrofit converter, which deserialized regardless of the server `Content-Type`
 * (mirrors often serve `.json` as `text/plain`).
 */
object GithubApi {
    var baseUrl: String = ApiConfig.GH_PROXY_BASE_URL

    private fun url(path: String) = baseUrl.trimEnd('/') + "/" + path.trimStart('/')

    private suspend fun getText(path: String): String =
        NetworkClient.client.get(url(path)).bodyAsText()

    suspend fun getAppUpdates(): List<AppUpdate> =
        JsonHelper.json.decodeFromString(getText("inventory/versions/app_updates.json"))

    suspend fun getResourcesVersions(): ResourcesVersions =
        JsonHelper.json.decodeFromString(getText("inventory/versions/resources_versions.json"))

    suspend fun getAvailableRecitations(): String =
        NetworkClient.client.get(url("inventory/recitations/available_recitations_info_v2.json")).bodyAsText()

    suspend fun getAvailableRecitationTranslations(): String =
        NetworkClient.client.get(url("inventory/recitations/available_recitation_translations_info_v2.json")).bodyAsText()

    suspend fun getAvailableWbwInfo(): AvailableWbwInfoModel =
        JsonHelper.json.decodeFromString(getText("inventory/wbw/available_wbw_info_v2.json"))

    /** Streaming translation download; [path] is relative to [baseUrl]. */
    suspend fun <T> getTranslation(path: String, block: suspend (HttpDownloadScope) -> T): T =
        downloadStream(url(path), block)
}
