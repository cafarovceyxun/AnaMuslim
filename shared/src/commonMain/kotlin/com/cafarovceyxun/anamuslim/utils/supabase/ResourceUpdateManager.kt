package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.managers.HadithSyncProvider
import com.cafarovceyxun.anamuslim.utils.managers.TranslationDownloadProvider
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import io.github.jan.supabase.postgrest.from

/**
 * Once-a-day check against the `resource_updates` Supabase table to see whether new
 * hadith/translation resources are available, plus the admin read/write helpers for
 * `resource_updates_admin`.
 *
 * When a new version is detected the refresh itself runs through the existing download seams
 * ([HadithSyncProvider], [TranslationDownloadProvider]), so it needs no platform hook of its own:
 * Android's registered sources enqueue their WorkManager jobs exactly as the old
 * `performPlatformResourceUpdates` lambda did, and iOS runs the shared in-process implementations.
 * (That lambda was registered on Android only, so this whole path was dead on iOS.)
 */
object ResourceUpdateManager {

    suspend fun checkAndPerformUpdate() {
        val lastCheckDate = DataStoreManager.readFirst(ResourceUpdatePreferences.KEY_LAST_UPDATE_CHECK_DATE)
        val today = currentLocalDateIsoString()

        if (lastCheckDate == today) {
            // Already checked today. Skip the Supabase request.
            return
        }

        try {
            // Mark as checked IMMEDIATELY to prevent race conditions during app startup.
            DataStoreManager.write(ResourceUpdatePreferences.KEY_LAST_UPDATE_CHECK_DATE, today)

            val status = SupabaseProvider.client.from("resource_updates")
                .select { filter { eq("id", 1) } }
                .decodeSingleOrNull<ResourceUpdateStatus>()
                ?: return // Resource status row not found (id=1).

            val localVersion = DataStoreManager.readFirst(ResourceUpdatePreferences.KEY_CURRENT_RESOURCE_VERSION)

            if (status.version > localVersion) {
                // CRITICAL: Update local version BEFORE starting downloads to prevent repeat on
                // next launch.
                DataStoreManager.write(ResourceUpdatePreferences.KEY_CURRENT_RESOURCE_VERSION, status.version)

                refreshDownloadedResources()
            }
        } catch (ex: Exception) {
            // Update check failed; will simply retry on the next daily check.
        }
    }

    /**
     * Re-pulls what the user already has: the hadith collection plus every downloaded translation.
     * Both go through the registered sources, i.e. WorkManager on Android and the shared
     * downloaders on iOS.
     */
    private suspend fun refreshDownloadedResources() {
        HadithSyncProvider.source.startSync()

        val factory = QuranTranslationFactory()
        try {
            factory.getDownloadedTranslationBooksInfo().values.forEach { book ->
                TranslationDownloadProvider.source.startDownload(book)
            }
        } finally {
            factory.close()
        }
    }

    suspend fun getAdminUpdateStatus(): ResourceUpdateStatus? {
        return try {
            SupabaseProvider.client.from("resource_updates_admin")
                .select { filter { eq("id", 1) } }
                .decodeSingleOrNull<ResourceUpdateStatus>()
        } catch (ex: Exception) {
            null
        }
    }

    suspend fun updateVersionByAdmin(newVersion: Int) {
        try {
            val newStatus = ResourceUpdateStatus(id = 1, version = newVersion)
            SupabaseProvider.client.from("resource_updates_admin").upsert(newStatus)

            // When admin updates, force a re-check for testing.
            DataStoreManager.write(ResourceUpdatePreferences.KEY_LAST_UPDATE_CHECK_DATE, "")
        } catch (ex: Exception) {
            // Admin update failed; ignore.
        }
    }
}
