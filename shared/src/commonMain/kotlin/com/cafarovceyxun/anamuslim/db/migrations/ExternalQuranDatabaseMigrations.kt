package com.cafarovceyxun.anamuslim.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object ExternalQuranDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `atlas_bundles` (
                        `bundle_key` TEXT NOT NULL,
                        `meta_json` TEXT NOT NULL,
                        `layer_json` TEXT NOT NULL,
                        PRIMARY KEY(`bundle_key`)
                    )
                    """.trimIndent(),
            )

            connection.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `atlas_word_shapes` (
                        `bundle_key` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `placements_json` TEXT NOT NULL,
                        PRIMARY KEY(`bundle_key`, `word`)
                    )
                    """.trimIndent(),
            )
            connection.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_atlas_word_shapes_bundle`
                    ON `atlas_word_shapes` (`bundle_key`)
                    """.trimIndent(),
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `wbw_audio_timing` (
                        `audio_id` TEXT NOT NULL,
                        `ayah_id` INTEGER NOT NULL,
                        `word_index` INTEGER NOT NULL,
                        `start_millis` INTEGER NOT NULL,
                        `end_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`audio_id`, `ayah_id`, `word_index`)
                    )
                    """.trimIndent(),
            )
            connection.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_wbw_audio_timing_ayah_word_index`
                    ON `wbw_audio_timing` (`ayah_id`, `word_index`)
                    """.trimIndent(),
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DELETE FROM `atlas_bundles`")
            connection.execSQL("DROP TABLE IF EXISTS `atlas_word_shapes`")
            connection.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `atlas_word_shapes` (
                        `bundle_key` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `page` INTEGER NOT NULL,
                        `placements_json` TEXT NOT NULL,
                        PRIMARY KEY(`bundle_key`, `word`, `page`)
                    )
                    """.trimIndent(),
            )
            connection.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_atlas_word_shapes_bundle`
                    ON `atlas_word_shapes` (`bundle_key`)
                    """.trimIndent(),
            )

            // Atlas shapes were just invalidated; ask the app to reset the reader's Quran
            // script if it was pointing at the Atlas script (no-op on platforms/tests that
            // don't register the hook).
            ExternalQuranMigrationHooks.onAtlasWordShapesReset()
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `tajweed_word_colors` (
                        `bundle_key` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `classes` BLOB NOT NULL,
                        PRIMARY KEY(`bundle_key`, `word`)
                    )
                    """.trimIndent(),
            )
            connection.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_tajweed_word_colors_bundle`
                    ON `tajweed_word_colors` (`bundle_key`)
                    """.trimIndent(),
            )

            connection.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `tajweed_overrides` (
                        `ayah_id` INTEGER NOT NULL,
                        `word_index` INTEGER NOT NULL,
                        `diffs` BLOB NOT NULL,
                        PRIMARY KEY(`ayah_id`, `word_index`)
                    )
                    """.trimIndent(),
            )
            connection.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_tajweed_overrides_ayah`
                    ON `tajweed_overrides` (`ayah_id`)
                    """.trimIndent(),
            )

            connection.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `tajweed_meta` (
                        `bundle_key` TEXT NOT NULL,
                        `version` INTEGER NOT NULL,
                        `palette` BLOB NOT NULL,
                        PRIMARY KEY(`bundle_key`)
                    )
                    """.trimIndent(),
            )
        }
    }
}
