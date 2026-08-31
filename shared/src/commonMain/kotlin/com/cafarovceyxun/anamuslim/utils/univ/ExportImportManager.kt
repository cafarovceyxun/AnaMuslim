package com.cafarovceyxun.anamuslim.utils.univ

import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.safeBoolean
import com.cafarovceyxun.anamuslim.api.safeFloat
import com.cafarovceyxun.anamuslim.api.safeInt
import com.cafarovceyxun.anamuslim.api.safeJsonArray
import com.cafarovceyxun.anamuslim.api.safeJsonObject
import com.cafarovceyxun.anamuslim.api.safeLong
import com.cafarovceyxun.anamuslim.api.safeString
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.utils.NumeralSystem
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.applyAppLanguage
import com.cafarovceyxun.anamuslim.compose.utils.applyThemeModeToPlatform
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.ResourceDownloadProxy
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.formatLocalDateTime
import com.cafarovceyxun.anamuslim.utils.parseLocalDateTime
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Builds and applies the backup file behind the Export/Import screen.
 *
 * Previously this lived in the Android `ActivityExportImport`, built on `org.json` and
 * `ActivityResultContracts`; iOS reached the same screen through the shared NavHost but got no
 * callbacks at all, so both buttons were silently inert. The format-level work is platform-neutral
 * and now lives here; only the file picker stays platform-bound ([TextDocumentSaver] /
 * [TextDocumentOpener]).
 *
 * **Format v2 — telefon dəyişəndə bir fayl bəs edir.** v1 yalnız Quran əlfəcinlərini və əl ilə
 * saxlanan ~18 ayarı daşıyırdı; hədis əlfəcinləri, oxuma tarixçəsi, mövzu rəngi, ana ekran düzəni,
 * hədis oxucusunun ayarları və sonradan əlavə olunan hər şey faylda **yox idi**. v2-də:
 *  - `preferences` — bütün DataStore ayarlarının tipli dumpı ([PreferenceBackup]),
 *  - `hadithBookmarks`, `readHistory`, `hadithReadHistory` — qalan istifadəçi məlumatı,
 *  - `settings` — v1 bloku olduğu kimi qalır (aşağı uyğunluq + dil, çünki dil DataStore-da deyil).
 *
 * Hər iki istiqamət işləyir: v1 faylı v2 tətbiqində, v2 faylı isə köhnə buraxılışda (tanımadığı
 * bölmələri atır) import olunur.
 */
object ExportImportManager {

    /**
     * Faylın adında tarix var: ehtiyat nüsxə bir dəfəlik deyil, istifadəçi bir neçəsini saxlayır və
     * hansının nə vaxt alındığını fayl seçicisində görməlidir. v1-in sabit adı
     * (`quranapp-exported-data-v1.json`) yalnız təklif idi — import ada baxmır, ona görə köhnə
     * fayllar oxunmağa davam edir.
     */
    fun exportFileName(): String = "anamuslim-backup-${currentLocalDateIsoString()}.json"

    private const val VERSION = 2

    /**
     * Deliberately **not** the caller's `rememberCoroutineScope()`. Importing a locale goes through
     * `AppLocaleHooks.applyLanguage`, which on Android drives `AppCompatDelegate` and recreates the
     * Activity — that disposes the composition and cancels its scope, so every write queued after
     * the locale was silently dropped mid-import. (Seen in the simulator run: the UI language
     * changed and the imported theme did not.) A process-lived scope finishes the job; the locale
     * is also applied last in [applyImport] so the recreation happens after the other writes.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Builds the export file off the UI's scope, then hands it to [onReady]. */
    fun export(scopes: Map<String, Boolean>, onReady: (String) -> Unit) {
        scope.launch { onReady(buildExportJson(scopes)) }
    }

    /** Applies [content] off the UI's scope, then reports to [onResult]. */
    fun import(content: String, scopes: Map<String, Boolean>, onResult: (ImportResult) -> Unit) {
        scope.launch { onResult(applyImport(content, scopes)) }
    }

    /** v1-dən qalan qısa açarlar — buraxılmış build-lər bu adları yazır, dəyişdirmək olmaz. */
    private object BookmarkKeys {
        const val ID = "id"
        const val CHAPTER_NO = "cn"
        const val FROM_VERSE_NO = "fvn"
        const val TO_VERSE_NO = "tvn"
        const val DATE = "dt"
        const val NOTE = "nt"
    }

    /**
     * v2 bölmələri açıq adlarla və **epoxa millisaniyəsi** ilə yazılır.
     *
     * v1 tarixi yerli divar saatı mətni kimi saxlayır (`parseLocalDateTime`), yəni fayl başqa
     * qurşaqdakı telefonda oxunanda saatlar sürüşür. Köhnə bölmə üçün format dondurulub, yeniləri
     * isə qurşaqdan asılı olmayan rəqəm daşıyır.
     */
    private object HadithBookmarkKeys {
        const val HADITH_ID = "hadithId"
        const val VOLUME_SLUG = "volumeSlug"
        const val BOOK_SLUG = "bookSlug"
        const val CHAPTER_SLUG = "chapterSlug"
        const val SUB_CHAPTER_SLUG = "subChapterSlug"
        const val HADITH_NO = "hadithNo"
        const val TITLE = "title"
        const val PREVIEW = "preview"
        const val NOTE = "note"
        const val DATE = "date"
    }

    private object ReadHistoryKeys {
        const val READ_TYPE = "readType"
        const val READER_MODE = "readerMode"
        const val DIVISION_NO = "divisionNo"
        const val CHAPTER_NO = "chapterNo"
        const val FROM_VERSE_NO = "fromVerseNo"
        const val TO_VERSE_NO = "toVerseNo"
        const val MUSHAF_CODE = "mushafCode"
        const val MUSHAF_VARIANT = "mushafVariant"
        const val PAGE_NO = "pageNo"
        const val DATE = "date"
    }

    private object HadithReadHistoryKeys {
        const val VOLUME_SLUG = "volumeSlug"
        const val BOOK_SLUG = "bookSlug"
        const val CHAPTER_SLUG = "chapterSlug"
        const val SUB_CHAPTER_SLUG = "subChapterSlug"
        const val TITLE = "title"
        const val DATE = "date"
    }

    /** What an import actually changed, so the caller can tell the user something truthful. */
    data class ImportResult(
        val bookmarksImported: Int,
        val hadithBookmarksImported: Int,
        val historyImported: Int,
        val settingsImported: Boolean,
        val failed: Boolean,
    ) {
        val changedAnything: Boolean
            get() = bookmarksImported > 0 ||
                hadithBookmarksImported > 0 ||
                historyImported > 0 ||
                settingsImported
    }

    /**
     * Serializes the selected [scopes] to the export format. Callers hand the result straight to
     * [TextDocumentSaver.save].
     */
    internal suspend fun buildExportJson(scopes: Map<String, Boolean>): String {
        val repository = RepositoryProvider.userRepository

        val root = buildJsonObject {
            if (scopes[ExportKeys.BOOKMARKS] == true) {
                // An empty array is omitted rather than written, matching the Android format:
                // an import then leaves existing bookmarks alone instead of "restoring" nothing.
                exportBookmarks().takeIf { it.isNotEmpty() }
                    ?.let { put(ExportKeys.BOOKMARKS, it) }

                exportHadithBookmarks().takeIf { it.isNotEmpty() }
                    ?.let { put(ExportKeys.HADITH_BOOKMARKS, it) }
            }

            if (scopes[ExportKeys.HISTORY] == true) {
                exportReadHistory(repository.getReadHistories()).takeIf { it.isNotEmpty() }
                    ?.let { put(ExportKeys.READ_HISTORY, it) }

                exportHadithReadHistory(repository.getHadithReadHistories())
                    .takeIf { it.isNotEmpty() }
                    ?.let { put(ExportKeys.HADITH_READ_HISTORY, it) }
            }

            if (scopes[ExportKeys.SETTINGS] == true) {
                put(ExportKeys.SETTINGS, exportSettings())
                put(
                    ExportKeys.PREFERENCES,
                    PreferenceBackup.encode(DataStoreManager.snapshotAll()),
                )
            }

            put(ExportKeys.EXPORTED_AT, formatLocalDateTime(currentEpochMillis()))
            put(ExportKeys.VERSION, VERSION)
        }

        return JsonHelper.json.encodeToString(JsonObject.serializer(), root)
    }

    /**
     * Applies [content] to the selected [scopes]. Malformed input is reported through the returned
     * [ImportResult] rather than thrown: the file comes from the user's storage and may be anything.
     */
    internal suspend fun applyImport(content: String, scopes: Map<String, Boolean>): ImportResult {
        val root = try {
            JsonHelper.json.parseToJsonElement(content).jsonObject
        } catch (e: Exception) {
            AppLogger.saveError(e, "ExportImportManager.parse")
            return ImportResult(
                bookmarksImported = 0,
                hadithBookmarksImported = 0,
                historyImported = 0,
                settingsImported = false,
                failed = true,
            )
        }

        val repository = RepositoryProvider.userRepository

        var bookmarksImported = 0
        var hadithBookmarksImported = 0
        var historyImported = 0
        var settingsImported = false
        var failed = false

        if (scopes[ExportKeys.BOOKMARKS] == true) {
            root.safeJsonArray(ExportKeys.BOOKMARKS)?.let { bookmarks ->
                try {
                    bookmarksImported = repository.addMissingBookmarks(parseBookmarks(bookmarks))
                } catch (e: Exception) {
                    AppLogger.saveError(e, "ExportImportManager.importBookmarks")
                    failed = true
                }
            }

            root.safeJsonArray(ExportKeys.HADITH_BOOKMARKS)?.let { bookmarks ->
                try {
                    hadithBookmarksImported =
                        repository.addMissingHadithBookmarks(parseHadithBookmarks(bookmarks))
                } catch (e: Exception) {
                    AppLogger.saveError(e, "ExportImportManager.importHadithBookmarks")
                    failed = true
                }
            }
        }

        if (scopes[ExportKeys.HISTORY] == true) {
            root.safeJsonArray(ExportKeys.READ_HISTORY)?.let { entries ->
                try {
                    // Köhnədən yeniyə: `saveReadHistory` siyahını sona görə kəsir, ona görə ən yeni
                    // sətir axırda yazılmalıdır ki, kəsilən köhnələr olsun.
                    parseReadHistory(entries).sortedBy { it.datetime }.forEach {
                        repository.saveReadHistory(it)
                        historyImported++
                    }
                } catch (e: Exception) {
                    AppLogger.saveError(e, "ExportImportManager.importReadHistory")
                    failed = true
                }
            }

            root.safeJsonArray(ExportKeys.HADITH_READ_HISTORY)?.let { entries ->
                try {
                    parseHadithReadHistory(entries).sortedBy { it.datetime }.forEach {
                        repository.saveHadithReadHistory(it)
                        historyImported++
                    }
                } catch (e: Exception) {
                    AppLogger.saveError(e, "ExportImportManager.importHadithReadHistory")
                    failed = true
                }
            }
        }

        if (scopes[ExportKeys.SETTINGS] == true) {
            val preferences = root.safeJsonArray(ExportKeys.PREFERENCES)
            val settings = root.safeJsonObject(ExportKeys.SETTINGS)

            try {
                if (preferences != null) {
                    importPreferences(preferences)
                    settingsImported = true
                } else if (settings != null) {
                    // v1 faylı: yalnız tanınan açarlar bərpa olunur.
                    importLegacySettings(settings)
                    settingsImported = true
                }

                // Dil DataStore-da deyil (platformanın öz yaddaşındadır), ona görə hər iki formatda
                // `settings` blokundan gəlir və **ən sonda** tətbiq olunur.
                settings?.let { applyImportedLocale(it) }
            } catch (e: Exception) {
                AppLogger.saveError(e, "ExportImportManager.importSettings")
                failed = true
            }
        }

        return ImportResult(
            bookmarksImported = bookmarksImported,
            hadithBookmarksImported = hadithBookmarksImported,
            historyImported = historyImported,
            settingsImported = settingsImported,
            failed = failed,
        )
    }

    private suspend fun exportBookmarks(): JsonArray {
        val bookmarks = RepositoryProvider.userRepository.getBookmarks()

        return buildJsonArray {
            bookmarks.forEach { bookmark ->
                add(
                    buildJsonObject {
                        put(BookmarkKeys.ID, bookmark.id)
                        put(BookmarkKeys.CHAPTER_NO, bookmark.chapterNo)
                        put(BookmarkKeys.FROM_VERSE_NO, bookmark.fromVerseNo)
                        put(BookmarkKeys.TO_VERSE_NO, bookmark.toVerseNo)
                        put(BookmarkKeys.DATE, formatLocalDateTime(bookmark.dateTime))
                        bookmark.note?.let { put(BookmarkKeys.NOTE, it) }
                    }
                )
            }
        }
    }

    private fun parseBookmarks(array: JsonArray): List<BookmarkEntity> = array.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null

        val chapterNo = obj[BookmarkKeys.CHAPTER_NO]?.jsonPrimitive?.intOrNull
        val fromVerseNo = obj[BookmarkKeys.FROM_VERSE_NO]?.jsonPrimitive?.intOrNull
        val toVerseNo = obj[BookmarkKeys.TO_VERSE_NO]?.jsonPrimitive?.intOrNull

        if (chapterNo == null || fromVerseNo == null || toVerseNo == null) return@mapNotNull null

        val date = obj[BookmarkKeys.DATE]?.jsonPrimitive?.contentOrNull

        BookmarkEntity(
            // Row ids are the local database's business; the exported one is informational only.
            id = 0,
            chapterNo = chapterNo,
            fromVerseNo = fromVerseNo,
            toVerseNo = toVerseNo,
            note = obj[BookmarkKeys.NOTE]?.jsonPrimitive?.contentOrNull,
            // A bookmark with an unreadable date is still worth keeping — only its timestamp is lost.
            dateTime = date?.let { parseLocalDateTime(it) } ?: currentEpochMillis(),
        )
    }

    private suspend fun exportHadithBookmarks(): JsonArray {
        val bookmarks = RepositoryProvider.userRepository.getHadithBookmarks()

        return buildJsonArray {
            bookmarks.forEach { bookmark ->
                add(
                    buildJsonObject {
                        put(HadithBookmarkKeys.HADITH_ID, bookmark.hadithId)
                        bookmark.volumeSlug?.let { put(HadithBookmarkKeys.VOLUME_SLUG, it) }
                        bookmark.bookSlug?.let { put(HadithBookmarkKeys.BOOK_SLUG, it) }
                        bookmark.chapterSlug?.let { put(HadithBookmarkKeys.CHAPTER_SLUG, it) }
                        bookmark.subChapterSlug?.let {
                            put(HadithBookmarkKeys.SUB_CHAPTER_SLUG, it)
                        }
                        put(HadithBookmarkKeys.HADITH_NO, bookmark.hadithNo)
                        put(HadithBookmarkKeys.TITLE, bookmark.title)
                        bookmark.preview?.let { put(HadithBookmarkKeys.PREVIEW, it) }
                        bookmark.note?.let { put(HadithBookmarkKeys.NOTE, it) }
                        put(HadithBookmarkKeys.DATE, bookmark.dateTime)
                    }
                )
            }
        }
    }

    private fun parseHadithBookmarks(array: JsonArray): List<HadithBookmarkEntity> =
        array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null

            // Hədisin özü cihazdakı bazadadır; id olmadan sətir heç nəyə işarə etmir.
            val hadithId = obj.safeLong(HadithBookmarkKeys.HADITH_ID) ?: return@mapNotNull null

            HadithBookmarkEntity(
                id = 0,
                hadithId = hadithId,
                volumeSlug = obj.safeString(HadithBookmarkKeys.VOLUME_SLUG),
                bookSlug = obj.safeString(HadithBookmarkKeys.BOOK_SLUG),
                chapterSlug = obj.safeString(HadithBookmarkKeys.CHAPTER_SLUG),
                subChapterSlug = obj.safeString(HadithBookmarkKeys.SUB_CHAPTER_SLUG),
                hadithNo = obj.safeInt(HadithBookmarkKeys.HADITH_NO, 0),
                // Başlıq siyahıda göstərilir; boş qalsa sətir görünməz olardı.
                title = obj.safeString(HadithBookmarkKeys.TITLE) ?: return@mapNotNull null,
                preview = obj.safeString(HadithBookmarkKeys.PREVIEW),
                note = obj.safeString(HadithBookmarkKeys.NOTE),
                dateTime = obj.safeLong(HadithBookmarkKeys.DATE) ?: currentEpochMillis(),
            )
        }

    private fun exportReadHistory(entries: List<ReadHistoryEntity>): JsonArray = buildJsonArray {
        entries.forEach { entry ->
            add(
                buildJsonObject {
                    put(ReadHistoryKeys.READ_TYPE, entry.readType)
                    put(ReadHistoryKeys.READER_MODE, entry.readerMode)
                    put(ReadHistoryKeys.DIVISION_NO, entry.divisionNo)
                    put(ReadHistoryKeys.CHAPTER_NO, entry.chapterNo)
                    put(ReadHistoryKeys.FROM_VERSE_NO, entry.fromVerseNo)
                    put(ReadHistoryKeys.TO_VERSE_NO, entry.toVerseNo)
                    entry.mushafCode?.let { put(ReadHistoryKeys.MUSHAF_CODE, it) }
                    entry.mushafVariant?.let { put(ReadHistoryKeys.MUSHAF_VARIANT, it) }
                    entry.pageNo?.let { put(ReadHistoryKeys.PAGE_NO, it) }
                    put(ReadHistoryKeys.DATE, entry.datetime)
                }
            )
        }
    }

    private fun parseReadHistory(array: JsonArray): List<ReadHistoryEntity> =
        array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null

            // Növ və rejim olmadan sətir oxucunu aça bilmir — belə giriş tarixçədə zibildir.
            val readType = obj.safeString(ReadHistoryKeys.READ_TYPE) ?: return@mapNotNull null
            val readerMode = obj.safeString(ReadHistoryKeys.READER_MODE) ?: return@mapNotNull null

            ReadHistoryEntity(
                id = 0,
                readType = readType,
                readerMode = readerMode,
                divisionNo = obj.safeInt(ReadHistoryKeys.DIVISION_NO, 0),
                chapterNo = obj.safeInt(ReadHistoryKeys.CHAPTER_NO, 0),
                fromVerseNo = obj.safeInt(ReadHistoryKeys.FROM_VERSE_NO, 0),
                toVerseNo = obj.safeInt(ReadHistoryKeys.TO_VERSE_NO, 0),
                mushafCode = obj.safeString(ReadHistoryKeys.MUSHAF_CODE),
                mushafVariant = obj.safeString(ReadHistoryKeys.MUSHAF_VARIANT),
                pageNo = obj.safeInt(ReadHistoryKeys.PAGE_NO),
                datetime = obj.safeLong(ReadHistoryKeys.DATE) ?: currentEpochMillis(),
            )
        }

    private fun exportHadithReadHistory(entries: List<HadithReadHistoryEntity>): JsonArray =
        buildJsonArray {
            entries.forEach { entry ->
                add(
                    buildJsonObject {
                        put(HadithReadHistoryKeys.VOLUME_SLUG, entry.volumeSlug)
                        entry.bookSlug?.let { put(HadithReadHistoryKeys.BOOK_SLUG, it) }
                        entry.chapterSlug?.let { put(HadithReadHistoryKeys.CHAPTER_SLUG, it) }
                        entry.subChapterSlug?.let {
                            put(HadithReadHistoryKeys.SUB_CHAPTER_SLUG, it)
                        }
                        put(HadithReadHistoryKeys.TITLE, entry.title)
                        put(HadithReadHistoryKeys.DATE, entry.datetime)
                    }
                )
            }
        }

    private fun parseHadithReadHistory(array: JsonArray): List<HadithReadHistoryEntity> =
        array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null

            val volumeSlug = obj.safeString(HadithReadHistoryKeys.VOLUME_SLUG)
                ?: return@mapNotNull null

            HadithReadHistoryEntity(
                id = 0,
                volumeSlug = volumeSlug,
                bookSlug = obj.safeString(HadithReadHistoryKeys.BOOK_SLUG),
                chapterSlug = obj.safeString(HadithReadHistoryKeys.CHAPTER_SLUG),
                subChapterSlug = obj.safeString(HadithReadHistoryKeys.SUB_CHAPTER_SLUG),
                title = obj.safeString(HadithReadHistoryKeys.TITLE) ?: return@mapNotNull null,
                datetime = obj.safeLong(HadithReadHistoryKeys.DATE) ?: currentEpochMillis(),
            )
        }

    /**
     * v1 `settings` bloku. Bütün ayarlar artıq [ExportKeys.PREFERENCES] dumpında olduğu üçün bu blok
     * yalnız iki iş görür: köhnə buraxılışlar faylı oxuya bilsin və **dil** daşınsın (dil
     * DataStore-da deyil, platformanın öz yaddaşındadır).
     */
    private suspend fun exportSettings(): JsonObject = buildJsonObject {
        put(ExportKeys.LOCALE, appLocale().rawLanguageTag)
        appLocale().numeralSystem?.let { put(ExportKeys.NUMERAL_SYSTEM, it.storageKey) }
        put(ExportKeys.THEME, ThemeUtils.getThemeMode())
        put(ExportKeys.DL_SRC, AppPreferences.getResourceDownloadProxy().value)
        put(ExportKeys.APP_TEXT_SCALE, AppPreferences.getAppTextScalePercent())

        put(ExportKeys.READER_AUTO_SCROLL_SPEED, ReaderPreferences.getAutoScrollSpeed())
        put(ExportKeys.READER_ARABIC_TEXT_ENABLED, ReaderPreferences.getArabicTextEnabled())
        // `.value`, not the enum: the import side resolves through `ReaderMode.fromValue`, which
        // does not know the enum's own name. The Android build wrote the name here, so reader mode
        // silently fell back to verse-by-verse on every import.
        put(ExportKeys.READER_MODE, ReaderPreferences.getReaderMode().value)
        // Boş sətir «sonuncu istifadə olunan» deməkdir və default budur — yalnız istifadəçi konkret
        // rejim seçibsə yazılır.
        ReaderPreferences.getDefaultReaderMode()?.let {
            put(ExportKeys.READER_DEFAULT_MODE, it.value)
        }

        put(ExportKeys.RECITATION_SPEED, RecitationPreferences.getSpeed())
        RecitationPreferences.getReciterId()?.let { put(ExportKeys.RECITATION_RECITER, it) }
        RecitationPreferences.getTranslationReciterId()?.let {
            put(ExportKeys.RECITATION_RECITER_TRANSLATION, it)
        }
        put(ExportKeys.RECITATION_OPTION_AUDIO, RecitationPreferences.getAudioOption().value)
        put(
            ExportKeys.RECITATION_AUDIO_END_BEHAVIOUR,
            RecitationPreferences.getAudioEndBehaviour().value,
        )

        put(ExportKeys.TEXT_SIZE_MULT_ARABIC, ReaderPreferences.getArabicTextSizeMultiplier())
        put(
            ExportKeys.TEXT_SIZE_MULT_TRANSLATION,
            ReaderPreferences.getTranslationTextSizeMultiplier(),
        )

        put(ExportKeys.SCRIPT_CURRENT, ReaderPreferences.getQuranScript())
        // The import side has always read this key; the Android export never wrote it, so a
        // restored script kept whatever variant happened to be set locally.
        ReaderPreferences.getQuranScriptVariant()?.let {
            put(ExportKeys.SCRIPT_VARIANT_CURRENT, it.value)
        }

        put(
            ExportKeys.TRANSLATION_CURRENT,
            buildJsonArray { ReaderPreferences.getTranslations().forEach { add(it) } },
        )
    }

    /**
     * v2 yolu: dumpdakı hər açar olduğu kimi geri yazılır.
     *
     * Setter-lərdən keçmir — setter-lər onsuz da yalnız DataStore-a yazır. Yeganə istisna mövzudur:
     * o, Android-də platformaya da tətbiq olunmalıdır, ona görə yazıdan sonra bir dəfə çağırılır.
     */
    private suspend fun importPreferences(entries: JsonArray) {
        DataStoreManager.writeAll(PreferenceBackup.decode(entries))
        applyThemeModeToPlatform(ThemeUtils.getThemeMode())
    }

    /** v1 faylları üçün: yalnız blokda tanınan açarlar. Dil burada **yoxdur** — o, ən sonda gəlir. */
    private suspend fun importLegacySettings(settings: JsonObject) {
        settings.safeString(ExportKeys.THEME)?.let {
            ThemeUtils.setThemeMode(it)
            applyThemeModeToPlatform(it)
        }

        settings.safeString(ExportKeys.DL_SRC)?.let {
            AppPreferences.setResourceDownloadProxy(ResourceDownloadProxy.fromValue(it))
        }

        settings.safeInt(ExportKeys.APP_TEXT_SCALE)?.let {
            AppPreferences.setAppTextScalePercent(it)
        }

        settings.safeFloat(ExportKeys.READER_AUTO_SCROLL_SPEED)?.let {
            ReaderPreferences.setAutoScrollSpeed(it)
        }

        settings.safeBoolean(ExportKeys.READER_ARABIC_TEXT_ENABLED)?.let {
            ReaderPreferences.setArabicTextEnabled(it)
        }

        settings.safeString(ExportKeys.READER_MODE)?.let {
            ReaderPreferences.setReaderMode(ReaderMode.fromValue(it))
        }

        settings.safeString(ExportKeys.READER_DEFAULT_MODE)?.let {
            ReaderPreferences.setDefaultReaderMode(ReaderMode.fromValue(it))
        }

        settings.safeFloat(ExportKeys.RECITATION_SPEED)?.let {
            RecitationPreferences.setSpeed(it)
        }

        settings.safeString(ExportKeys.RECITATION_RECITER)?.let {
            RecitationPreferences.setReciterId(it)
        }

        settings.safeString(ExportKeys.RECITATION_RECITER_TRANSLATION)?.let {
            RecitationPreferences.setTranslationReciterId(it)
        }

        settings.safeString(ExportKeys.RECITATION_OPTION_AUDIO)?.let {
            RecitationPreferences.setAudioOption(AudioOption.fromValue(it))
        }

        settings.safeString(ExportKeys.RECITATION_AUDIO_END_BEHAVIOUR)?.let {
            RecitationPreferences.setAudioEndBehaviour(AudioEndBehaviour.fromValue(it))
        }

        settings.safeFloat(ExportKeys.TEXT_SIZE_MULT_ARABIC)?.let {
            ReaderPreferences.setArabicTextSizeMultiplier(it)
        }

        settings.safeFloat(ExportKeys.TEXT_SIZE_MULT_TRANSLATION)?.let {
            ReaderPreferences.setTranslationTextSizeMultiplier(it)
        }

        settings.safeString(ExportKeys.SCRIPT_CURRENT)?.let {
            ReaderPreferences.setQuranScript(it)
        }

        settings.safeString(ExportKeys.SCRIPT_VARIANT_CURRENT)?.let {
            ReaderPreferences.setQuranScriptVariant(QuranScriptVariant.fromValue(it))
        }

        settings.safeJsonArray(ExportKeys.TRANSLATION_CURRENT)?.let { translations ->
            val slugs = translations.mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
            ReaderPreferences.setTranslations(slugs)
        }
    }

    /**
     * Last on purpose: persistence *and* the platform's own language switch live behind this
     * seam, and on Android the switch recreates the Activity. Anything written after it would
     * race that recreation.
     */
    private fun applyImportedLocale(settings: JsonObject) {
        val languageTag = settings.safeString(ExportKeys.LOCALE) ?: return
        val numeral = settings.safeString(ExportKeys.NUMERAL_SYSTEM)
            ?.let { NumeralSystem.fromStorage(it) }
            ?: appLocale().numeralSystem

        applyAppLanguage(languageTag, numeral)
    }
}
