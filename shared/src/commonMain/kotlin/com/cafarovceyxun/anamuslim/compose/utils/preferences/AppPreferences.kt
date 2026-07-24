@file:OptIn(ExperimentalCoroutinesApi::class)

package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.app.ResourceDownloadProxy
import com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep
import kotlinx.coroutines.ExperimentalCoroutinesApi

object AppPreferences {
    val KEY_DOWNLOAD_PROXY =
        PrefKey(stringPreferencesKey("resource_download_proxy"), ResourceDownloadProxy.DEFAULT.value)

    /**
     * Off by default: the volume buttons belong to the system until the user hands them to the
     * reader. Turning it on costs one tap in settings; leaving it on by default silently takes
     * volume control away from every reader who never asked for page keys.
     */
    val KEY_VOLUME_KEY_NAVIGATION =
        PrefKey(androidx.datastore.preferences.core.booleanPreferencesKey("volume_key_navigation"), false)

    /** How far one key press scrolls, as a share of the viewport. See [ReaderScrollStep]. */
    val KEY_READER_SCROLL_STEP_PERCENT = PrefKey(
        androidx.datastore.preferences.core.intPreferencesKey("reader_scroll_step_percent"),
        ReaderScrollStep.DEFAULT_PERCENT,
    )

    private val KEY_SCROLL_STEP_MIGRATED =
        PrefKey(androidx.datastore.preferences.core.booleanPreferencesKey("reader_scroll_step_migrated"), false)

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

    fun getReaderScrollStepPercent(): Int {
        return DataStoreManager.read(KEY_READER_SCROLL_STEP_PERCENT)
    }

    suspend fun setReaderScrollStepPercent(percent: Int) {
        DataStoreManager.write(
            KEY_READER_SCROLL_STEP_PERCENT,
            percent.coerceIn(ReaderScrollStep.MIN_PERCENT, ReaderScrollStep.MAX_PERCENT),
        )
    }

    @Composable
    fun observeReaderScrollStepPercent(): Int {
        return DataStoreManager.observe(KEY_READER_SCROLL_STEP_PERCENT)
    }

    /**
     * One-shot fold of the hadith reader's old three-step scroll distance (small / medium / large,
     * 400 / 800 / 1400px) into the shared percentage step, so a reader who had picked a distance
     * keeps it now that the two settings are one.
     *
     * Guarded by its own flag rather than by "is the new key still absent": without it every launch
     * would overwrite a step the user has since moved back to the legacy choice. Medium maps onto
     * the new default, so the overwhelming majority of installs migrate to exactly what they had.
     *
     * Call once at startup, after [DataStoreManager.warmUp] — Android from `QuranApp.onCreate()`,
     * iOS from the bootstrap the composition root awaits.
     */
    suspend fun migrateLegacyScrollStep() {
        if (DataStoreManager.readFirst(KEY_SCROLL_STEP_MIGRATED)) return

        val percent = when (DataStoreManager.readFirst(HadithPreferences.SCROLL_AMOUNT_MODE)) {
            0 -> 45
            2 -> 95
            else -> ReaderScrollStep.DEFAULT_PERCENT
        }

        DataStoreManager.write(KEY_READER_SCROLL_STEP_PERCENT, percent)
        DataStoreManager.write(KEY_SCROLL_STEP_MIGRATED, true)
    }
}
