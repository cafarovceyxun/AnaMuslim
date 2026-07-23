package com.cafarovceyxun.anamuslim.db.translation

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.components.quran.subcomponents.Translation
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.utils.quran.QuranConstants
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Multiplatform replacement for the Android `QuranTranslDBHelper` (`QuranTranslation.db`).
 *
 * Downloaded translations use a **dynamic per-slug table** layout (one table per translation,
 * named by its slug) plus a fixed info table — a shape Room cannot model (it needs compile-time
 * `@Entity` tables). So this uses the low-level `androidx.sqlite` API directly (works on Android
 * and iOS via `BundledSQLiteDriver`), and parses translation JSON with kotlinx.serialization
 * (replacing the JVM-only `org.json`).
 *
 * This is the single DB gateway for translations — the Android `QuranTranslationFactory` and the
 * indexing worker delegate all their SQL here. Open a real DB via
 * [QuranTranslationDatabase.open]; version/migration handling (mirroring the old
 * `SQLiteOpenHelper` lifecycle) lives there.
 *
 * A single [SQLiteConnection] is shared process-wide (as the old `SQLiteOpenHelper` was), so every
 * operation is serialized behind [lock] — `androidx.sqlite` connections are not safe for concurrent
 * use across threads.
 */
class QuranTranslationStore(private val connection: SQLiteConnection) {

    private val lock = ReentrantLock()

    private data class VerseRow(
        val chapterNo: Int,
        val verseNo: Int,
        val text: String,
        val note: String?,
    )

    // region schema / versioning

    /** Configures connection pragmas equivalent to the old helper's `onConfigure`. */
    internal fun configure() = lock.withLock {
        connection.execSQL("PRAGMA busy_timeout=$BUSY_TIMEOUT_MS")
        connection.execSQL("PRAGMA journal_mode=WAL")
    }

    internal fun userVersion(): Int = lock.withLock {
        connection.prepare("PRAGMA user_version").use { stmt ->
            if (stmt.step()) stmt.getLong(0).toInt() else 0
        }
    }

    internal fun setUserVersion(version: Int) = lock.withLock {
        connection.execSQL("PRAGMA user_version=$version")
    }

    /** v1 → v2: the old `onUpgrade` added a `note` column to every existing per-slug table. */
    internal fun migrateV1ToV2() = lock.withLock {
        for (slug in getStoredSlugsLocked()) {
            try {
                connection.execSQL(
                    "ALTER TABLE ${escapeTableName(slug)} ADD COLUMN $COL_NOTE TEXT"
                )
            } catch (e: Throwable) {
                // Column may already exist on partially-migrated DBs; ignore, as the old code did.
            }
        }
    }

    fun createInfoTable() = lock.withLock {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS $INFO_TABLE (" +
                "$COL_SLUG TEXT PRIMARY KEY," +
                "$COL_LANG_CODE TEXT," +
                "$COL_LANG_NAME TEXT," +
                "$COL_BOOK_NAME TEXT," +
                "$COL_AUTHOR_NAME TEXT," +
                "$COL_DISPLAY_NAME TEXT," +
                "$COL_LAST_UPDATED INTEGER," +
                "$COL_DOWNLOAD_PATH TEXT," +
                "$COL_IS_PREMIUM INTEGER)"
        )
    }

    // endregion

    // region writes

    /** Stores a translation's info row and its verses (parsed from [translData]) into its own table. */
    fun storeTranslation(bookInfo: TranslationBookInfoModel, translData: String) = lock.withLock {
        createInfoTableLocked()
        storeTranslationInfoLocked(bookInfo)
        createTranslTableLocked(bookInfo.slug)

        val rows = parseVerses(translData)
        val sql = "INSERT OR REPLACE INTO ${escapeTableName(bookInfo.slug)} " +
            "($COL_ID, $COL_CHAPTER_NO, $COL_VERSE_NO, $COL_TEXT, $COL_NOTE) " +
            "VALUES (?, ?, ?, ?, ?)"
        connection.prepare(sql).use { stmt ->
            for (row in rows) {
                stmt.bindText(1, makeVerseKey(row.chapterNo, row.verseNo))
                stmt.bindLong(2, row.chapterNo.toLong())
                stmt.bindLong(3, row.verseNo.toLong())
                stmt.bindText(4, row.text)
                if (row.note == null) stmt.bindNull(5) else stmt.bindText(5, row.note)
                stmt.step()
                stmt.reset()
            }
        }
    }

    /** Drops the translation's per-slug table and removes its info row. */
    fun deleteTranslation(slug: String) = lock.withLock {
        connection.execSQL("BEGIN")
        try {
            connection.execSQL("DROP TABLE IF EXISTS ${escapeTableName(slug)}")
            connection.prepare("DELETE FROM $INFO_TABLE WHERE $COL_SLUG=?").use { stmt ->
                stmt.bindText(1, slug)
                stmt.step()
            }
            connection.execSQL("COMMIT")
        } catch (e: Throwable) {
            connection.execSQL("ROLLBACK")
            throw e
        }
    }

    /** Updates the text (and optionally note) of a single verse in a stored translation. */
    fun updateTranslation(
        slug: String,
        chapterNo: Int,
        verseNo: Int,
        newText: String,
        newNote: String? = null,
    ) = lock.withLock {
        val sql = buildString {
            append("UPDATE ${escapeTableName(slug)} SET $COL_TEXT=?")
            if (newNote != null) append(", $COL_NOTE=?")
            append(" WHERE $COL_CHAPTER_NO=? AND $COL_VERSE_NO=?")
        }
        connection.prepare(sql).use { stmt ->
            var i = 1
            stmt.bindText(i++, newText)
            if (newNote != null) stmt.bindText(i++, newNote)
            stmt.bindLong(i++, chapterNo.toLong())
            stmt.bindLong(i, verseNo.toLong())
            stmt.step()
        }
    }

    // endregion

    // region reads

    /** Whether a per-slug translation table exists in the DB. */
    fun isTranslationDownloaded(slug: String): Boolean = lock.withLock {
        connection.prepare(
            "SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name=?"
        ).use { stmt ->
            stmt.bindText(1, slug)
            stmt.step()
        }
    }

    /**
     * Book info for the given [slugs] (or all stored books when null), keyed by slug and ordered by
     * slug — mirrors the old `getTranslationBooksInfo`.
     */
    fun getBooksInfo(slugs: Set<String>? = null): Map<String, TranslationBookInfoModel> = lock.withLock {
        val sql = buildString {
            append(
                "SELECT DISTINCT $COL_SLUG, $COL_LANG_CODE, $COL_LANG_NAME, $COL_BOOK_NAME, " +
                    "$COL_AUTHOR_NAME, $COL_DISPLAY_NAME, $COL_LAST_UPDATED, $COL_DOWNLOAD_PATH " +
                    "FROM $INFO_TABLE"
            )
            if (slugs != null) {
                append(" WHERE ")
                append(List(slugs.size) { "$COL_SLUG=?" }.joinToString(" OR "))
            }
            append(" ORDER BY $COL_SLUG ASC")
        }
        if (!infoTableExistsLocked()) return@withLock emptyMap()

        val out = LinkedHashMap<String, TranslationBookInfoModel>()
        connection.prepare(sql).use { stmt ->
            slugs?.forEachIndexed { index, slug -> stmt.bindText(index + 1, slug) }
            while (stmt.step()) {
                val book = TranslationBookInfoModel(stmt.getText(0)).apply {
                    langCode = stmt.textOrEmpty(1)
                    langName = stmt.textOrEmpty(2)
                    bookName = stmt.textOrEmpty(3)
                    authorName = stmt.textOrEmpty(4)
                    displayName = stmt.textOrEmpty(5)
                    lastUpdated = if (stmt.isNull(6)) -1L else stmt.getLong(6)
                    downloadPath = stmt.textOrEmpty(7)
                }
                out[book.slug] = book
            }
        }
        out
    }

    /** Chapter/verse ordered text for a stored translation (`chapterNo` → verseNo → text). */
    fun getVerseTexts(slug: String): List<String> = lock.withLock {
        val out = ArrayList<String>()
        connection.prepare(
            "SELECT $COL_TEXT FROM ${escapeTableName(slug)} " +
                "ORDER BY $COL_CHAPTER_NO ASC, $COL_VERSE_NO ASC"
        ).use { stmt ->
            while (stmt.step()) out.add(stmt.getText(0))
        }
        out
    }

    fun getStoredSlugs(): List<String> = lock.withLock { getStoredSlugsLocked() }

    /** Translations for a single verse of one book (empty if none). */
    fun getVersesSingle(slug: String, chapterNo: Int, verseNo: Int): List<Translation> =
        lock.withLock {
            queryVersesLocked(
                slug,
                "$COL_CHAPTER_NO=? AND $COL_VERSE_NO=?",
                listOf(chapterNo.toLong(), verseNo.toLong()),
            )
        }

    /** Translations for a contiguous verse range of one book, verse-ordered. */
    fun getVersesRange(
        slug: String,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): List<Translation> = lock.withLock {
        queryVersesLocked(
            slug,
            "$COL_CHAPTER_NO=? AND $COL_VERSE_NO>=? AND $COL_VERSE_NO<=?",
            listOf(chapterNo.toLong(), fromVerse.toLong(), toVerse.toLong()),
        )
    }

    /** Translations for a set of (possibly non-contiguous) verses of one book, verse-ordered. */
    fun getVersesDistinct(slug: String, chapterNo: Int, verses: IntArray): List<Translation> =
        lock.withLock {
            if (verses.isEmpty()) return@withLock emptyList()
            val where = buildString {
                append("$COL_CHAPTER_NO=? AND (")
                append(List(verses.size) { "$COL_VERSE_NO=?" }.joinToString(" OR "))
                append(")")
            }
            val args = ArrayList<Long>(verses.size + 1)
            args.add(chapterNo.toLong())
            verses.forEach { args.add(it.toLong()) }
            queryVersesLocked(slug, where, args)
        }

    /**
     * Bulk lookup of verse texts across multiple books for search, keyed by slug then (chap, verse).
     * Mirrors the old UNION-ALL query; only text is populated (as the original did).
     */
    fun getTranslationsBulkForSearch(
        slugs: Set<String>,
        verseKeys: List<Pair<Int, Int>>,
    ): Map<String, Map<Pair<Int, Int>, Translation>> = lock.withLock {
        if (slugs.isEmpty() || verseKeys.isEmpty()) return@withLock emptyMap()

        val ids = verseKeys.map { "${it.first}:${it.second}" }
        val sql = buildString {
            slugs.forEachIndexed { index, slug ->
                if (index > 0) append(" UNION ALL ")
                append(
                    "SELECT '" + slug.replace("'", "''") + "' AS $COL_SLUG, " +
                        "$COL_CHAPTER_NO, $COL_VERSE_NO, $COL_TEXT " +
                        "FROM ${escapeTableName(slug)} " +
                        "WHERE $COL_ID IN (${ids.joinToString(",") { "?" }})"
                )
            }
            append(" ORDER BY $COL_CHAPTER_NO, $COL_VERSE_NO")
        }

        val result = LinkedHashMap<String, MutableMap<Pair<Int, Int>, Translation>>()
        connection.prepare(sql).use { stmt ->
            var bindIndex = 1
            repeat(slugs.size) {
                for (id in ids) stmt.bindText(bindIndex++, id)
            }
            while (stmt.step()) {
                val slug = stmt.getText(0)
                val surahNo = stmt.getLong(1).toInt()
                val ayahNo = stmt.getLong(2).toInt()
                val text = stmt.getText(3)
                val map = result.getOrPut(slug) { LinkedHashMap() }
                map[surahNo to ayahNo] = Translation().apply {
                    chapterNo = surahNo
                    verseNo = ayahNo
                    this.text = text
                    bookSlug = slug
                }
            }
        }
        result
    }

    /** (chapterNo, verseNo, rawText) rows for one book, ordered — used to build the search index. */
    fun getIndexRows(slug: String): List<IndexRow> = lock.withLock {
        val out = ArrayList<IndexRow>()
        connection.prepare(
            "SELECT $COL_CHAPTER_NO, $COL_VERSE_NO, $COL_TEXT FROM ${escapeTableName(slug)} " +
                "ORDER BY $COL_CHAPTER_NO ASC, $COL_VERSE_NO ASC"
        ).use { stmt ->
            while (stmt.step()) {
                out.add(IndexRow(stmt.getLong(0).toInt(), stmt.getLong(1).toInt(), stmt.getText(2)))
            }
        }
        out
    }

    fun countRows(slug: String): Int = lock.withLock {
        connection.prepare("SELECT COUNT(*) FROM ${escapeTableName(slug)}").use { stmt ->
            if (stmt.step()) stmt.getLong(0).toInt() else 0
        }
    }

    // endregion

    // region internals (assume [lock] already held)

    private fun getStoredSlugsLocked(): List<String> {
        if (!infoTableExistsLocked()) return emptyList()
        val out = ArrayList<String>()
        connection.prepare("SELECT $COL_SLUG FROM $INFO_TABLE ORDER BY $COL_SLUG").use { stmt ->
            while (stmt.step()) out.add(stmt.getText(0))
        }
        return out
    }

    private fun infoTableExistsLocked(): Boolean {
        connection.prepare(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?"
        ).use { stmt ->
            stmt.bindText(1, INFO_TABLE)
            return stmt.step()
        }
    }

    private fun queryVersesLocked(
        slug: String,
        where: String,
        args: List<Long>,
    ): List<Translation> {
        val out = ArrayList<Translation>()
        val sql = "SELECT $COL_CHAPTER_NO, $COL_VERSE_NO, $COL_TEXT, $COL_NOTE " +
            "FROM ${escapeTableName(slug)} WHERE $where " +
            "ORDER BY $COL_CHAPTER_NO ASC, $COL_VERSE_NO ASC"
        connection.prepare(sql).use { stmt ->
            args.forEachIndexed { index, arg -> stmt.bindLong(index + 1, arg) }
            while (stmt.step()) {
                val chapterNo = stmt.getLong(0).toInt()
                val verseNo = stmt.getLong(1).toInt()
                val text = stmt.getText(2)
                val note = if (stmt.isNull(3)) "" else stmt.getText(3)
                out.add(
                    Translation().apply {
                        this.chapterNo = chapterNo
                        this.verseNo = verseNo
                        this.text = text
                        this.note = note
                        bookSlug = slug
                    }
                )
            }
        }
        return out
    }

    private fun createInfoTableLocked() {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS $INFO_TABLE (" +
                "$COL_SLUG TEXT PRIMARY KEY," +
                "$COL_LANG_CODE TEXT," +
                "$COL_LANG_NAME TEXT," +
                "$COL_BOOK_NAME TEXT," +
                "$COL_AUTHOR_NAME TEXT," +
                "$COL_DISPLAY_NAME TEXT," +
                "$COL_LAST_UPDATED INTEGER," +
                "$COL_DOWNLOAD_PATH TEXT," +
                "$COL_IS_PREMIUM INTEGER)"
        )
    }

    private fun createTranslTableLocked(slug: String) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS ${escapeTableName(slug)} (" +
                "$COL_ID TEXT PRIMARY KEY," +
                "$COL_CHAPTER_NO INTEGER," +
                "$COL_VERSE_NO INTEGER," +
                "$COL_TEXT TEXT," +
                "$COL_NOTE TEXT)"
        )
    }

    private fun storeTranslationInfoLocked(bookInfo: TranslationBookInfoModel) {
        connection.prepare(
            "INSERT OR REPLACE INTO $INFO_TABLE " +
                "($COL_SLUG, $COL_LANG_CODE, $COL_LANG_NAME, $COL_BOOK_NAME, " +
                "$COL_AUTHOR_NAME, $COL_DISPLAY_NAME, $COL_LAST_UPDATED, $COL_DOWNLOAD_PATH) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { stmt ->
            stmt.bindText(1, bookInfo.slug)
            stmt.bindText(2, bookInfo.langCode)
            stmt.bindText(3, bookInfo.langName)
            stmt.bindText(4, bookInfo.bookName)
            stmt.bindText(5, bookInfo.authorName)
            stmt.bindText(6, bookInfo.displayName)
            stmt.bindLong(7, bookInfo.lastUpdated)
            stmt.bindText(8, bookInfo.downloadPath)
            stmt.step()
        }
    }

    private fun parseVerses(translData: String): List<VerseRow> {
        val root = Json.parseToJsonElement(translData).jsonObject
        return if (root.containsKey(QuranConstants.KEY_CHAPTER_LIST)) {
            parseChapterListFormat(root)
        } else {
            parseKeyedFormat(root)
        }
    }

    /** Standard format: `{ "suras": [ { "index": n, "ayas": [ { "index": v, "translation": ... } ] } ] }`. */
    private fun parseChapterListFormat(root: kotlinx.serialization.json.JsonObject): List<VerseRow> {
        val out = ArrayList<VerseRow>()
        val chapters = root[QuranConstants.KEY_CHAPTER_LIST]?.jsonArray ?: return out
        for (chapterEl in chapters) {
            val chapter = chapterEl.jsonObject
            val chapterNo = chapter[QuranConstants.KEY_NUMBER]?.jsonPrimitive?.int ?: -1
            val verses = chapter[QuranConstants.KEY_VERSE_LIST]?.jsonArray ?: continue
            for (verseEl in verses) {
                val verse = verseEl.jsonObject
                out.add(
                    VerseRow(
                        chapterNo = chapterNo,
                        verseNo = verse[QuranConstants.KEY_NUMBER]?.jsonPrimitive?.int ?: -1,
                        text = verse[QuranConstants.KEY_TRANSLATION_TEXT]?.jsonPrimitive?.content ?: "",
                        note = verse["note"]?.jsonPrimitive?.content ?: "",
                    )
                )
            }
        }
        return out
    }

    /** Compact ("az") format: `{ "chap:verse": { "t": text, "n": note } }`. */
    private fun parseKeyedFormat(root: kotlinx.serialization.json.JsonObject): List<VerseRow> {
        val out = ArrayList<VerseRow>()
        for ((key, valueEl) in root) {
            val parts = key.split(":")
            if (parts.size != 2) continue
            val chapterNo = parts[0].toIntOrNull() ?: continue
            val verseNo = parts[1].toIntOrNull() ?: continue
            val verse = valueEl.jsonObject
            out.add(
                VerseRow(
                    chapterNo = chapterNo,
                    verseNo = verseNo,
                    text = verse["t"]?.jsonPrimitive?.content ?: "",
                    note = verse["n"]?.jsonPrimitive?.content ?: "",
                )
            )
        }
        return out
    }

    private fun makeVerseKey(chapterNo: Int, verseNo: Int): String = "$chapterNo:$verseNo"

    /** Reads a text column, mapping SQL NULL to `""` (the model's non-null fields expect this). */
    private fun SQLiteStatement.textOrEmpty(index: Int): String =
        if (isNull(index)) "" else getText(index)

    // endregion

    /** A raw verse row (chapter, verse, unprocessed text) for external index building. */
    data class IndexRow(val chapterNo: Int, val verseNo: Int, val text: String)

    companion object {
        const val DB_NAME = "QuranTranslation.db"
        const val DB_VERSION = 2
        private const val BUSY_TIMEOUT_MS = 5000

        const val INFO_TABLE = "QuranTranslationBookInfo"
        const val COL_SLUG = "slug"
        const val COL_LANG_CODE = "langCode"
        const val COL_LANG_NAME = "langName"
        const val COL_BOOK_NAME = "bookName"
        const val COL_AUTHOR_NAME = "authorName"
        const val COL_DISPLAY_NAME = "displayName"
        const val COL_LAST_UPDATED = "lastUpdated"
        const val COL_DOWNLOAD_PATH = "downloadPath"
        const val COL_IS_PREMIUM = "isPremium"

        const val COL_ID = "_id"
        const val COL_CHAPTER_NO = "chapterNo"
        const val COL_VERSE_NO = "verseNo"
        const val COL_TEXT = "text"
        const val COL_NOTE = "note"

        /** Slugs may contain special chars (e.g. `en_sahih-international`), so quote table names. */
        fun escapeTableName(tableName: String): String = "`$tableName`"
    }
}
