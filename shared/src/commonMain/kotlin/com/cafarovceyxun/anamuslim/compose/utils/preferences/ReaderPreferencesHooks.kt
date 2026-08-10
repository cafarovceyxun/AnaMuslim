package com.cafarovceyxun.anamuslim.compose.utils.preferences

/**
 * ReaderPreferences üçün platformadan asılı olan tənzimləmə metodlarını bura yığırıq.
 * Android tərəfində proqram işə düşəndə real tətbiqləri qeyd edəcəyik.
 */
object ReaderPreferencesHooks {
    var migrateFromLegacy: (suspend () -> Unit)? = null
}
