package com.cafarovceyxun.anamuslim.utils.supabase

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrefKey

object ResourceUpdatePreferences {
    /**
     * ISO `yyyy-MM-dd` local date of the last Supabase resource-update check, empty if never
     * checked. Stored as a date string (not epoch millis) so "same day" comparisons don't need
     * platform calendar math. Renamed from the old `last_resource_update_check` (Long) key when
     * this moved to commonMain -- the old key is simply orphaned, no migration needed.
     */
    val KEY_LAST_UPDATE_CHECK_DATE = PrefKey(stringPreferencesKey("last_resource_update_check_date"), "")
    val KEY_CURRENT_RESOURCE_VERSION = PrefKey(intPreferencesKey("current_resource_version"), 0)
}
