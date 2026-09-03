package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.api.ApiConfig
import com.cafarovceyxun.anamuslim.api.GithubApi
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import kotlin.jvm.JvmStatic

object DownloadSourceUtils {
    @Composable
    fun observeCurrentSourceName(): String {
        return getDownloadSourceName(AppPreferences.observeResourceDownloadProxy())
    }

    fun getDownloadSourceName(src: ResourceDownloadProxy): String {
        return when (src) {
            ResourceDownloadProxy.GITHUB -> "raw.githubusercontent.com"
            ResourceDownloadProxy.JSDELIVR -> "cdn.jsdelivr.net"
        }
    }

    /**
     * `owner/repo/ref/yol` formasındakı inventory yolunu seçilmiş mirror üçün tam URL-ə çevirir.
     *
     * ⚠️ jsDelivr kökü **sadə birləşdirmə ilə işləmir**: `cdn.jsdelivr.net/gh/` `owner/repo@ref/yol`
     * gözləyir, `owner/repo/ref/yol` isə 404 verir. Əvvəl kök sadəcə yola yapışdırılırdı, ona görə
     * jsDelivr seçimi **heç vaxt işləməyib** — nə yuxarı axın faylları, nə bizimkilər. Səssiz idi,
     * çünki yükləmə xətaları onsuz da udulur.
     */
    fun buildInventoryUrl(path: String): String {
        return when (AppPreferences.getResourceDownloadProxy()) {
            ResourceDownloadProxy.GITHUB -> ApiConfig.GH_RAW_ROOT + path
            ResourceDownloadProxy.JSDELIVR -> ApiConfig.JS_DELIVR_ROOT + toJsDelivrPath(path)
        }
    }

    /** `owner/repo/ref/qalan` → `owner/repo@ref/qalan`. Forma gözlənilməzdirsə olduğu kimi qalır. */
    internal fun toJsDelivrPath(path: String): String {
        val parts = path.split('/')
        if (parts.size < 4) return path
        return "${parts[0]}/${parts[1]}@${parts[2]}/${parts.drop(3).joinToString("/")}"
    }

    fun getDownloadSourceBaseUrl(): String {
        return when (AppPreferences.getResourceDownloadProxy()) {
            ResourceDownloadProxy.GITHUB -> ApiConfig.GH_RAW_BASE_URL
            ResourceDownloadProxy.JSDELIVR -> ApiConfig.JS_DELIVR_BASE_URL
        }
    }

    suspend fun setDownloadSource(downloadSrc: ResourceDownloadProxy) {
        AppPreferences.setResourceDownloadProxy(downloadSrc)
        resetDownloadSourceBaseUrl()
    }

    @JvmStatic
    fun resetDownloadSourceBaseUrl() {
        GithubApi.baseUrl = getDownloadSourceBaseUrl()
    }
}
