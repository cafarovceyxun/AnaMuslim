package com.cafarovceyxun.anamuslim.db.migrations

/**
 * DI seam for platform-specific side effects inside ExternalQuranDatabase migrations.
 *
 * `MIGRATION_3_4` rebuilds the Atlas word-shape tables, which can leave the reader pointing at an
 * Atlas script that no longer has a bundle. Android registers the reset here (`QuranApp.onCreate`)
 * because the hook predates the migration of `ReaderPreferences`/`QuranScriptUtils` into
 * commonMain.
 *
 * **The empty default is not a gap on iOS.** Both hosts call
 * `ReaderPreferences.repairStoredPreferences()` at startup (`QuranApp.onCreate` /
 * `initSharedForIos`), and that check is strictly broader — it asks the atlas DAO whether the
 * bundle is actually installed, which is exactly what the rebuilt table answers. So iOS reaches the
 * same end state one step later, without the hook.
 */
object ExternalQuranMigrationHooks {
    var onAtlasWordShapesReset: () -> Unit = {}
}
