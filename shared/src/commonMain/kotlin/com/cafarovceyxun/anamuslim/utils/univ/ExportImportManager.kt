package com.cafarovceyxun.anamuslim.utils.univ

import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.safeBoolean
import com.cafarovceyxun.anamuslim.api.safeFloat
import com.cafarovceyxun.anamuslim.api.safeJsonArray
import com.cafarovceyxun.anamuslim.api.safeJsonObject
import com.cafarovceyxun.anamuslim.api.safeString
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.applyAppLanguage
import com.cafarovceyxun.anamuslim.compose.utils.applyThemeModeToPlatform
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.ResourceDownloadProxy
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
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
 * The on-disk format is unchanged, so files written by released Android builds still import.
 */
object ExportImportManager {

    /** Matches the name released Android builds have written since v1. */
    const val EXPORT_FILE_NAME = "quranapp-exported-data-v1.json"

    private const val VERSION = 1

    /**
     * Deliberately **not** the caller's `rememberCoroutineScope()`. Importing a locale goes through
     * `AppLocaleHooks.applyLanguage`, which on Android drives `AppCompatDelegate` and recreates the
     * Activity — that disposes the composition and cancels its scope, so every write queued after
     * the locale was silently dropped mid-import. (Seen in the simulator run: the UI language
     * changed and the imported theme did not.) A process-lived scope finishes the job; the locale
     * is also applied last in [importSettings] so the recreation happens after the other writes.
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

    private object BookmarkKeys {
        const val ID = "id"
        const val CHAPTER_NO = "cn"
        const val FROM_VERSE_NO = "fvn"
        const val TO_VERSE_NO = "tvn"
        const val DATE = "dt"
        const val NOTE = "nt"
    }

    /** What an import actually changed, so the caller can tell the user something truthful. */
    data class ImportResult(
        val bookmarksImported: Int,
        val settingsImported: Boolean,
        val failed: Boolean,
    ) {
        val changedAnything: Boolean get() = bookmarksImported > 0 || settingsImported
    }

    /**
     * Serializes the selected [scopes] to the export format. Callers hand the result straight to
     * [TextDocumentSaver.save].
     */
    internal suspend fun buildExportJson(scopes: Map<String, Boolean>): String {
        val root = buildJsonObject {
            if (scopes[ExportKeys.BOOKMARKS] == true) {
                val bookmarks = exportBookmarks()
                // An empty array is omitted rather than written, matching the Android format:
                // an import then leaves existing bookmarks alone instead of "restoring" nothing.
                if (bookmarks.isNotEmpty()) put(ExportKeys.BOOKMARKS, bookmarks)
            }

            if (scopes[ExportKeys.SETTINGS] == true) {
                put(ExportKeys.SETTINGS, exportSettings())
            }

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
            return ImportResult(bookmarksImported = 0, settingsImported = false, failed = true)
        }

        var bookmarksImported = 0
        var settingsImported = false
        var failed = false

        if (scopes[ExportKeys.BOOKMARKS] == true) {
            root.safeJsonArray(ExportKeys.BOOKMARKS)?.let { bookmarks ->
                try {
                    val entities = parseBookmarks(bookmarks)
                    RepositoryProvider.userRepository.addMultipleBookmarks(entities)
                    bookmarksImported = entities.size
                } catch (e: Exception) {
                    AppLogger.saveError(e, "ExportImportManager.importBookmarks")
                    failed = true
                }
            }
        }

        if (scopes[ExportKeys.SETTINGS] == true) {
            root.safeJsonObject(ExportKeys.SETTINGS)?.let { settings ->
                try {
                    importSettings(settings)
                    settingsImported = true
                } catch (e: Exception) {
                    AppLogger.saveError(e, "ExportImportManager.importSettings")
                    failed = true
                }
            }
        }

        return ImportResult(bookmarksImported, settingsImported, failed)
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

    private suspend fun exportSettings(): JsonObject = buildJsonObject {
        put(ExportKeys.LOCALE, appLocale().rawLanguageTag)
        put(ExportKeys.THEME, ThemeUtils.getThemeMode())
        put(ExportKeys.DL_SRC, AppPreferences.getResourceDownloadProxy().value)

        put(ExportKeys.READER_AUTO_SCROLL_SPEED, ReaderPreferences.getAutoScrollSpeed())
        put(ExportKeys.READER_ARABIC_TEXT_ENABLED, ReaderPreferences.getArabicTextEnabled())
        // `.value`, not the enum: the import side resolves through `ReaderMode.fromValue`, which
        // does not know the enum's own name. The Android build wrote the name here, so reader mode
        // silently fell back to verse-by-verse on every import.
        put(ExportKeys.READER_MODE, ReaderPreferences.getReaderMode().value)

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

    private suspend fun importSettings(settings: JsonObject) {
        settings.safeString(ExportKeys.THEME)?.let {
            ThemeUtils.setThemeMode(it)
            applyThemeModeToPlatform(it)
        }

        settings.safeString(ExportKeys.DL_SRC)?.let {
            AppPreferences.setResourceDownloadProxy(ResourceDownloadProxy.fromValue(it))
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

        // Last on purpose: persistence *and* the platform's own language switch live behind this
        // seam, and on Android the switch recreates the Activity. Anything written after it would
        // race that recreation.
        settings.safeString(ExportKeys.LOCALE)?.let {
            applyAppLanguage(it, appLocale().numeralSystem)
        }
    }
}
