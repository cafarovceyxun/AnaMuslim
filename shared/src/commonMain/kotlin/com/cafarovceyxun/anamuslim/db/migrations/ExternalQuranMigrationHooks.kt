package com.cafarovceyxun.anamuslim.db.migrations

/**
 * DI seam for platform-specific side effects inside ExternalQuranDatabase migrations.
 *
 * `MIGRATION_3_4` rebuilds the Atlas word-shape tables; on Android it must also reset the
 * reader's Quran script if it was pointing at the now-invalidated Atlas script. That logic
 * depends on app-level `ReaderPreferences`/`QuranScriptUtils`, which are not in commonMain, so
 * the app registers it here (see `QuranApp.onCreate`). The default is a no-op, which is correct
 * for tests and for any platform without those preferences.
 */
object ExternalQuranMigrationHooks {
    var onAtlasWordShapesReset: () -> Unit = {}
}
