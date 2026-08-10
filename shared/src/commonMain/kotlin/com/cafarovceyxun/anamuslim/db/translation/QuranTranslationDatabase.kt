package com.cafarovceyxun.anamuslim.db.translation

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Opens `QuranTranslation.db` as a [QuranTranslationStore], reproducing the version lifecycle the
 * Android `SQLiteOpenHelper` used to provide (it kept the schema version in SQLite's `user_version`
 * pragma, so existing Android installs carry over unchanged):
 *
 * - `user_version == 0` (brand-new file): create the info table, run [onFreshCreate] (the Android
 *   one-time file-based-translations migration; a no-op on iOS/new installs), then stamp
 *   [QuranTranslationStore.DB_VERSION].
 * - `user_version == 1`: run the v1→v2 upgrade (adds the `note` column to each per-slug table).
 * - `user_version >= 2`: nothing to do.
 */
object QuranTranslationDatabase {

    /**
     * @param dbPath absolute path to the DB file (or `:memory:` in tests).
     * @param onFreshCreate invoked once, on a freshly created DB, before the version is stamped.
     */
    fun open(
        dbPath: String,
        onFreshCreate: ((QuranTranslationStore) -> Unit)? = null,
    ): QuranTranslationStore {
        val connection = BundledSQLiteDriver().open(dbPath)
        val store = QuranTranslationStore(connection)
        store.configure()

        when (store.userVersion()) {
            0 -> {
                store.createInfoTable()
                onFreshCreate?.invoke(store)
                store.setUserVersion(QuranTranslationStore.DB_VERSION)
            }

            1 -> {
                store.migrateV1ToV2()
                store.setUserVersion(QuranTranslationStore.DB_VERSION)
            }
        }

        return store
    }
}
