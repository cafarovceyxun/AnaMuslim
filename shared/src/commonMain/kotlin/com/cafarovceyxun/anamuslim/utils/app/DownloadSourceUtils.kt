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
            ResourceDownloadProxy.ALFAAZ_PLUS -> "gh-proxy.alfaazplus.com"
            ResourceDownloadProxy.GITHUB -> "raw.githubusercontent.com"
            ResourceDownloadProxy.JSDELIVR -> "cdn.jsdelivr.net"
        }
    }

    fun getDownloadSourceRoot(): String {
        return when (AppPreferences.getResourceDownloadProxy()) {
            ResourceDownloadProxy.ALFAAZ_PLUS -> ApiConfig.GH_PROXY_ROOT
            ResourceDownloadProxy.GITHUB -> ApiConfig.GH_RAW_ROOT
            ResourceDownloadProxy.JSDELIVR -> ApiConfig.JS_DELIVR_ROOT
        }
    }

    fun getDownloadSourceBaseUrl(): String {
        return when (AppPreferences.getResourceDownloadProxy()) {
            ResourceDownloadProxy.ALFAAZ_PLUS -> ApiConfig.GH_PROXY_BASE_URL
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
