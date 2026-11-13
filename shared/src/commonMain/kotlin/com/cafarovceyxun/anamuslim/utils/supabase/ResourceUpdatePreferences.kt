package com.cafarovceyxun.anamuslim.utils.supabase

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrefKey

object ResourceUpdatePreferences {
    /**
     * Son resurs-yeniləmə yoxlamasının vaxtı (epoch ms), `0` — heç yoxlanmayıb.
     *
     * 2026-11-13-ə qədər `last_resource_update_check_date` (gün sətri) idi və yoxlama **gündə bir
     * dəfə** gedirdi: admin səhər yenilik buraxanda o gün artıq yoxlamış telefonlar onu yalnız
     * ertəsi gün görürdü. Köhnə açar sadəcə yetim qalır, miqrasiya lazım deyil — ilk açılışda
     * yoxlama bir dəfə artıq gedir, o qədər.
     */
    val KEY_LAST_UPDATE_CHECK_AT = PrefKey(longPreferencesKey("last_resource_update_check_at"), 0L)
    val KEY_CURRENT_RESOURCE_VERSION = PrefKey(intPreferencesKey("current_resource_version"), 0)
}
