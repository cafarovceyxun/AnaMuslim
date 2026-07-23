@file:OptIn(ExperimentalCoroutinesApi::class)

package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.app.ResourceDownloadProxy
import kotlinx.coroutines.ExperimentalCoroutinesApi

object AppPreferences {
    val KEY_DOWNLOAD_PROXY =
        PrefKey(stringPreferencesKey("resource_download_proxy"), ResourceDownloadProxy.DEFAULT.value)

    val KEY_VOLUME_KEY_NAVIGATION =
        PrefKey(androidx.datastore.preferences.core.booleanPreferencesKey("volume_key_navigation"), true)

    /**
     * Which version of onboarding the user has finished — see
     * `com.cafarovceyxun.anamuslim.compose.screens.onboarding.OnboardingGate`.
     *
     * A version rather than a flag so a release can require the screens again. Both platforms read
     * it: Android's older `SPAppActions` boolean cannot express "seen, but not this version".
     */
    val KEY_ONBOARDING_COMPLETED_VERSION =
        PrefKey(androidx.datastore.preferences.core.intPreferencesKey("onboarding_completed_version"), 0)

    fun getResourceDownloadProxy(): ResourceDownloadProxy {
        return DataStoreManager.read(KEY_DOWNLOAD_PROXY).let { ResourceDownloadProxy.fromValue(it) }
    }

    suspend fun setResourceDownloadProxy(src: ResourceDownloadProxy) {
        DataStoreManager.write(KEY_DOWNLOAD_PROXY, src.value)
    }

    @Composable
    fun observeResourceDownloadProxy(): ResourceDownloadProxy {
        return DataStoreManager.observe(KEY_DOWNLOAD_PROXY)
            .let { ResourceDownloadProxy.fromValue(it) }
    }

    fun getOnboardingCompletedVersion(): Int {
        return DataStoreManager.read(KEY_ONBOARDING_COMPLETED_VERSION)
    }

    suspend fun setOnboardingCompletedVersion(version: Int) {
        DataStoreManager.write(KEY_ONBOARDING_COMPLETED_VERSION, version)
    }

    fun getVolumeKeyNavigationEnabled(): Boolean {
        return DataStoreManager.read(KEY_VOLUME_KEY_NAVIGATION)
    }

    suspend fun setVolumeKeyNavigationEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_VOLUME_KEY_NAVIGATION, enabled)
    }

    @Composable
    fun observeVolumeKeyNavigationEnabled(): Boolean {
        return DataStoreManager.observe(KEY_VOLUME_KEY_NAVIGATION)
    }
}
