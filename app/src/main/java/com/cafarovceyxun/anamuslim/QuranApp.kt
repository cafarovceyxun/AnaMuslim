package com.cafarovceyxun.anamuslim

import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.cafarovceyxun.anamuslim.compose.utils.AndroidThemeUtils
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferencesHooks
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AndroidReaderPreferences
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptPlatformHooks
import com.cafarovceyxun.anamuslim.utils.reader.atlas.QuranAtlasLoader
import com.cafarovceyxun.anamuslim.utils.reader.FontResolver
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtilsAndroid
import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.univ.FileUtils
import com.cafarovceyxun.anamuslim.utils.univ.AndroidAppFiles
import com.cafarovceyxun.anamuslim.utils.verse.VerseUtils
import com.cafarovceyxun.anamuslim.utils.verse.VerseUtilsHooks
import com.cafarovceyxun.anamuslim.utils.others.ShortcutUtils
import java.io.File
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.refreshAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.setAppLocale
import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.utils.Logger
import com.cafarovceyxun.anamuslim.db.DatabaseProvider
import com.cafarovceyxun.anamuslim.db.bookmark.UserDataMigrationManager
import com.cafarovceyxun.anamuslim.db.migrations.ExternalQuranMigrationHooks
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.isQuranAtlasScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.cafarovceyxun.anamuslim.search.SearchIndexScheduler
import com.cafarovceyxun.anamuslim.utils.app.DownloadSourceUtils
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.exceptions.CustomExceptionHandler
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelManager
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioRepository
import com.cafarovceyxun.anamuslim.viewModels.ReaderIndexFavouritesMigration
import com.cafarovceyxun.anamuslim.views.player.startRecitationPlayerWidgetObserver
import com.cafarovceyxun.anamuslim.views.reader.startVotdWidgetPreferenceObserver

class QuranApp : Application() {
    /** Single instance: [HadithDownloadManager.initialize] registers a permanent WorkManager observer. */
    private val androidHadithSyncSource by lazy {
        com.cafarovceyxun.anamuslim.utils.managers.AndroidHadithSyncSource(applicationContext)
    }

    /**
     * Single instance: a `SQLiteOpenHelper` is meant to be long-lived, and the shared
     * [SearchHistorySource] seam has no `close()` (the VM used to close a per-VM store).
     */
    private val searchHistoryStore by lazy {
        com.cafarovceyxun.anamuslim.db.search.SearchHistoryStore(applicationContext)
    }

    /** Single instance: holds one WorkManager handle and its LiveData->Flow observers. */
    private val androidWbwAudioDownloadSource by lazy {
        com.cafarovceyxun.anamuslim.utils.mediaplayer.AndroidWbwAudioDownloadSource(applicationContext)
    }

    /** Single instance: holds one WorkManager handle and its LiveData->Flow observers. */
    private val androidRecitationDownloadSource by lazy {
        com.cafarovceyxun.anamuslim.utils.mediaplayer.AndroidRecitationDownloadSource(applicationContext)
    }

    /** Single instance: [TranslationDownloadManager.initialize] registers a WorkManager observer. */
    private val androidTranslationDownloadSource by lazy {
        com.cafarovceyxun.anamuslim.utils.managers.AndroidTranslationDownloadSource(applicationContext)
    }

    override fun attachBaseContext(base: Context) {
        initBeforeBaseAttach(base)
        super.attachBaseContext(base)
    }

    private fun initBeforeBaseAttach(base: Context) {
        FileUtils.appFilesDir = base.filesDir
        // Seam for the shared okio AppFileSystem (shared/androidMain has no Context).
        AndroidAppFiles.filesDirPath = base.filesDir.absolutePath
    }

    private fun updateTheme() {
        AppCompatDelegate.setDefaultNightMode(AndroidThemeUtils.resolveThemeModeForDelegate())
    }

    override fun onCreate() {
        super.onCreate()
        // Seam for shared platform utils (clipboard, browser, toasts).
        com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext.context = applicationContext
        DataStoreManager.init { File(applicationContext.filesDir, "datastore/app_preferences.preferences_pb").absolutePath }
        // One disk read, here, instead of one on whichever thread happens to call `read()` first —
        // every synchronous preference lookup after this is a map access. Costs no more than the
        // blocking read the first `read()` did anyway, and takes it off the UI thread's path.
        kotlinx.coroutines.runBlocking {
            DataStoreManager.warmUp()
            // Fold the hadith reader's old scroll-distance choice into the shared step, now that one
            // percentage drives both readers. After warm-up so it reads a live snapshot.
            com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences.migrateLegacyScrollStep()
            // Move any hadith Arabic font off a now-removed mushaf face onto the default book font.
            com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences.migrateArabicFontToBookFonts()
        }


        // Register ReaderPreferencesHooks. Only legacy migration is Android-specific now (it reads
        // old SharedPreferences); repairStoredPreferences moved to shared ReaderPreferences.
        ReaderPreferencesHooks.migrateFromLegacy = { AndroidReaderPreferences.migrateFromLegacy(applicationContext) }

        // Register ReaderUiHooks
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openVerse = { chapterNo, verseNo ->
            val intent = com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory
                .prepareSingleVerseIntent(chapterNo, verseNo)
                .setClass(applicationContext, com.cafarovceyxun.anamuslim.activities.MainActivity::class.java)
                .setAction(com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(intent)
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openSearch = {
            val intent = android.content.Intent(applicationContext, com.cafarovceyxun.anamuslim.activities.ActivitySearch::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(intent)
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openVerseRange = { chapterNo, fromVerse, toVerse ->
            val intent = com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory
                .prepareVerseRangeIntent(chapterNo, fromVerse, toVerse)
                .setClass(applicationContext, com.cafarovceyxun.anamuslim.activities.MainActivity::class.java)
                .setAction(com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(intent)
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openChapter = { chapterNo ->
            com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory.startChapter(applicationContext, chapterNo)
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openJuz = { juzNo ->
            com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory.startJuz(applicationContext, juzNo)
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openHizb = { hizbNo ->
            com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory.startHizb(applicationContext, hizbNo)
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openReaderFromHistory = { history ->
            com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory
                .prepareHistoryIntent(history)?.let { intent ->
                    intent.setClass(applicationContext, com.cafarovceyxun.anamuslim.activities.MainActivity::class.java)
                    intent.action = com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(intent)
                }
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openVerseRangeWithTranslations =
            { chapterNo, fromVerse, toVerse, translationSlugs ->
                val intent = com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory
                    .prepareVerseRangeIntent(chapterNo, fromVerse, toVerse)
                    .setClass(applicationContext, com.cafarovceyxun.anamuslim.activities.MainActivity::class.java)
                    .setAction(com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(
                        com.cafarovceyxun.anamuslim.utils.univ.Keys.READER_KEY_TRANSL_SLUGS,
                        translationSlugs.toTypedArray()
                    )
                    .putExtra(
                        com.cafarovceyxun.anamuslim.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES,
                        false
                    )
                applicationContext.startActivity(intent)
            }

        // Register WbwAudioProvider (word-by-word audio subsystem)
        com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioProvider.langCodeProvider = { wbwId ->
            com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwManager
                .getAvailable(applicationContext, false)?.wbw?.firstOrNull { it.id == wbwId }?.langCode
        }
        com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioProvider.warmUpProvider = {
            com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioPlayer.warmUp(applicationContext)
        }
        com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioProvider.playProvider = { chapterNo, verseNo, wordIndex ->
            com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioPlayer.play(
                applicationContext, chapterNo, verseNo, wordIndex
            )
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openSettingsRoute = { route ->
            val intent = android.content.Intent(applicationContext, com.cafarovceyxun.anamuslim.activities.ActivitySettings::class.java).apply {
                putExtra(com.cafarovceyxun.anamuslim.utils.univ.Keys.NAV_DESTINATION, route)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            applicationContext.startActivity(intent)
        }
        com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openChapterInfo = { chapterNo ->
            val intent = android.content.Intent(applicationContext, com.cafarovceyxun.anamuslim.activities.ActivityChapInfo::class.java).apply {
                putExtra(com.cafarovceyxun.anamuslim.utils.univ.Keys.READER_KEY_CHAPTER_NO, chapterNo)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            applicationContext.startActivity(intent)
        }

        // Register TranslUtils hooks and paths
        TranslUtils.isPrebuilt = { TranslUtilsAndroid.isPrebuilt(it) }
        TranslUtils.DIR_NAME = TranslUtilsAndroid.DIR_NAME
        TranslUtils.DIR_NAME_4_AVAILABLE_DOWNLOADS = TranslUtilsAndroid.DIR_NAME_4_AVAILABLE_DOWNLOADS

        // Register QuranScriptPlatformHooks
        QuranScriptUtils.FONTS_DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "script_fonts")
        QuranScriptUtils.SCRIPT_DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "scripts")

        // The downloaded-font count is shared now (QuranScriptUtils.getKFQPCFontDownloadedCount) —
        // it reads the same directory the shared installer writes, so no Android hook is needed.
        QuranScriptPlatformHooks.getFontRes = { script, isDark ->
            when (script) {
                QuranScriptUtils.SCRIPT_DK_INDOPAK -> R.font.scheherazadenew_regular
                QuranScriptUtils.SCRIPT_PDMS_ISLAMIC -> R.font.quran_common
                else -> R.font.uthmanic_hafs
            }
        }
        com.cafarovceyxun.anamuslim.db.search.SearchHistoryProvider.setProvider { searchHistoryStore }

        com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioDownloadProvider.setProvider {
            androidWbwAudioDownloadSource
        }

        com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationDownloadProvider.setProvider {
            androidRecitationDownloadSource
        }

        com.cafarovceyxun.anamuslim.compose.utils.DailyReminderProvider.setProvider {
            object : com.cafarovceyxun.anamuslim.compose.utils.DailyReminderScheduler {
                override fun schedule() =
                    com.cafarovceyxun.anamuslim.compose.utils.VerseOfTheDayScheduler
                        .scheduleDailyNotification(applicationContext)

                override fun cancel() =
                    com.cafarovceyxun.anamuslim.compose.utils.VerseOfTheDayScheduler
                        .cancelDailyNotification(applicationContext)
            }
        }

        // Namaz bildirişləri: eyni səbəbdən seam — planlayıcı `AlarmManager`-ə və :app-dakı
        // receiver-lərə söykənir. `schedule()` idempotentdir və növbəti bir alarmı qurur.
        com.cafarovceyxun.anamuslim.compose.utils.PrayerReminderProvider.setProvider {
            object : com.cafarovceyxun.anamuslim.compose.utils.PrayerReminderScheduler {
                override fun schedule() =
                    com.cafarovceyxun.anamuslim.compose.utils.PrayerAlarmScheduler
                        .schedule(applicationContext)

                override fun cancel() =
                    com.cafarovceyxun.anamuslim.compose.utils.PrayerAlarmScheduler
                        .cancel(applicationContext)
            }
        }

        // Home screen widget seam: the receivers live in :app, so shared code cannot name them.
        com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetPinProvider.setProvider {
            com.cafarovceyxun.anamuslim.views.widget.AndroidHomeWidgetPinner(applicationContext)
        }

        // Locale seam for shared language UI: persistence + AppCompatDelegate stay on this side.
        com.cafarovceyxun.anamuslim.compose.utils.AppLocaleHooks.applyLanguage = { tag, numeral ->
            setAppLocale(
                applicationContext,
                com.cafarovceyxun.anamuslim.compose.utils.appLocaleForLanguageChange(
                    applicationContext, tag, numeral,
                ),
            )
        }

        refreshAppLocale(applicationContext)
        NetworkConfig.appVersionCode = { BuildConfig.VERSION_CODE.toString() }
        NetworkConfig.appVersionName = { BuildConfig.VERSION_NAME }
        NetworkConfig.logger = { url -> Logger.print(url) }
        // Logging seam: shared code logs via AppLogger; route it to the full Android logcat/file/Supabase Log.
        com.cafarovceyxun.anamuslim.utils.AppLogger.debugSink = { tag, message ->
            com.cafarovceyxun.anamuslim.utils.Log.d(tag ?: "AppLog", message)
        }
        com.cafarovceyxun.anamuslim.utils.AppLogger.errorSink = { throwable, place ->
            com.cafarovceyxun.anamuslim.utils.Log.saveError(throwable, place)
        }
        // Reciter-catalog seam: shared ViewModels read the reciter list without a Context.
        com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelProvider.setSource {
            com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelManager
        }
        // Word-by-word dataset catalog + downloads, for the shared WbwSettingsViewModel.
        val wbwResourceSource by lazy {
            com.cafarovceyxun.anamuslim.utils.reader.wbw.AndroidWbwResourceSource(applicationContext)
        }
        com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwResourceProvider.setSource { wbwResourceSource }
        // Quran-script font/atlas downloads, for the shared ScriptsViewModel.
        val scriptResourceSource by lazy {
            com.cafarovceyxun.anamuslim.utils.managers.AndroidScriptResourceSource(applicationContext)
        }
        com.cafarovceyxun.anamuslim.utils.managers.ScriptResourceProvider.setSource { scriptResourceSource }
        // Hadith sync seam: the shared HadithViewModel drives sync without a Context; WorkManager
        // stays here. iOS falls back to HadithSyncProvider's no-op until BGTaskScheduler lands.
        com.cafarovceyxun.anamuslim.utils.managers.HadithSyncProvider.setSource {
            androidHadithSyncSource
        }
        // Translation-download seam: same shape as the hadith one — the shared TranslationViewModel
        // drives WorkManager downloads without a Context. iOS keeps the no-op fallback for now.
        com.cafarovceyxun.anamuslim.utils.managers.TranslationDownloadProvider.setSource {
            androidTranslationDownloadSource
        }
        // The remaining Android-only touchpoint of the shared TranslationViewModel: the
        // WorkManager-backed search index.
        com.cafarovceyxun.anamuslim.utils.managers.TranslationPlatformHooks.removeSlugFromSearchIndex = { slug ->
            SearchIndexScheduler.enqueueRemoveSlug(applicationContext, slug)
        }
        // Connectivity seam: shared code checks network via isNetworkConnected() (no Context).
        com.cafarovceyxun.anamuslim.utils.network.AndroidConnectivity.context = applicationContext
        // Repo DI seam: shared (commonMain) ViewModels obtain repositories without a Context.
        com.cafarovceyxun.anamuslim.repository.RepositoryProvider.setQuranRepositoryProvider {
            com.cafarovceyxun.anamuslim.db.DatabaseProvider.getQuranRepository(applicationContext)
        }
        com.cafarovceyxun.anamuslim.repository.RepositoryProvider.setUserRepositoryProvider {
            com.cafarovceyxun.anamuslim.db.DatabaseProvider.getUserRepository(applicationContext)
        }
        com.cafarovceyxun.anamuslim.repository.RepositoryProvider.setExternalQuranDatabaseProvider {
            com.cafarovceyxun.anamuslim.db.DatabaseProvider.getExternalQuranDatabase(applicationContext)
        }
        com.cafarovceyxun.anamuslim.repository.RepositoryProvider.setHadithDatabaseProvider {
            com.cafarovceyxun.anamuslim.db.DatabaseProvider.getHadithDatabase(applicationContext)
        }
        com.cafarovceyxun.anamuslim.repository.RepositoryProvider.setSearchIndexDatabaseProvider {
            com.cafarovceyxun.anamuslim.db.DatabaseProvider.getSearchIndexDatabase(applicationContext)
        }
        com.cafarovceyxun.anamuslim.repository.RepositoryProvider.setQuranTranslationStoreProvider {
            com.cafarovceyxun.anamuslim.db.DatabaseProvider.getQuranTranslationStore(applicationContext)
        }
        // Read-history shortcut seam: the shared reader VM surfaces "continue reading" without a Context.
        // Verse-of-the-day launcher shortcut: the only Android-only part of the VOTD logic.
        com.cafarovceyxun.anamuslim.utils.verse.VerseUtilsHooks.pushVotdShortcut = { chapterNo, verseNo ->
            com.cafarovceyxun.anamuslim.utils.others.ShortcutUtils.pushVOTDShortcut(
                applicationContext, chapterNo, verseNo,
            )
        }
        com.cafarovceyxun.anamuslim.utils.others.ReadHistoryShortcuts.pushLastVerses = { entity ->
            com.cafarovceyxun.anamuslim.utils.others.ShortcutUtils.pushLastVersesShortcut(applicationContext, entity)
        }
        // Player DI seam: shared player/reader UI obtains the recitation player without a Context.
        com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider.setProvider {
            com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationController.getInstance(applicationContext)
        }
        // Store rating DI seam: the shared rate prompt and the overflow menu's "Rate App" row hand
        // off here. Registering it is also what makes them appear at all — both check
        // `AppStoreReviewProvider.isAvailable` first.
        com.cafarovceyxun.anamuslim.utils.app.AppStoreReviewProvider.setProvider {
            com.cafarovceyxun.anamuslim.utils.app.AndroidAppStoreReview(applicationContext)
        }
        // Atlas DI seam: shared reader builder loads glyph-atlas bundles (asset import + PNG decode)
        // without a Context; the Android loader holds the app context and BitmapFactory decoder.
        com.cafarovceyxun.anamuslim.utils.reader.atlas.QuranAtlasLoader.setLoader(
            com.cafarovceyxun.anamuslim.utils.reader.atlas.AndroidAtlasBundleLoader(applicationContext)
        )
        // The shared ResourceUpdateManager refreshes resources through the sync/download seams
        // registered above (WorkManager-backed here), so it needs no platform lambda anymore.
        // ExternalQuranDatabase MIGRATION_3_4 side effect (rebuilt Atlas shapes → reset reader
        // script if it was on the now-invalidated Atlas script). Registered before any DB access.
        ExternalQuranMigrationHooks.onAtlasWordShapesReset = {
            runBlocking {
                withContext(Dispatchers.IO) {
                    if (ReaderPreferences.getQuranScript().isQuranAtlasScript()) {
                        ReaderPreferences.setQuranScript(QuranScriptUtils.SCRIPT_DEFAULT)
                    }
                }
            }
        }
        DownloadSourceUtils.resetDownloadSourceBaseUrl()
        NotificationUtils.createNotificationChannels(this)
        com.cafarovceyxun.anamuslim.utils.managers.HadithDownloadManager.initialize(this)
        com.cafarovceyxun.anamuslim.utils.managers.TranslationDownloadManager.initialize(this)
        updateTheme()
        startVotdWidgetPreferenceObserver(this)
        startRecitationPlayerWidgetObserver(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        // Handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler(this))

        // Migrations
        ReaderPreferences.migrateFromLegacyIfNeeded()
        ReaderPreferences.repairStoredPreferencesIfNeeded()
        RecitationModelManager.migrateLegacyData()
        WbwAudioRepository.migrateLegacyData(applicationContext)
        ReaderIndexFavouritesMigration.migrate(this)
        UserDataMigrationManager(this).migrate()

        SearchIndexScheduler.scheduleTranslationSearchIndexIfNeeded(applicationContext)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // The atlas reader is by far the largest resident allocation: up to 32 MB of decoded
        // texture pages, plus a placement list for every (page, word) ever rendered and the merged
        // tajweed classes beside it. None of it was ever released — `QuranAtlasLoader.clearCache()`
        // existed but had no call site — so a session that read a few juz kept that memory for the
        // whole process, background included, which is exactly what Play's memory thresholds
        // measure.
        //
        // The split matters: dropping a bundle's placement cache while the reader is on screen is
        // harmless (built items keep their own maps) but costs the next page build a re-query, so
        // foreground pressure only gives back the textures, which re-decode from disk on demand.
        @Suppress("DEPRECATION")
        when (level) {
            TRIM_MEMORY_RUNNING_LOW, TRIM_MEMORY_RUNNING_CRITICAL -> QuranAtlasLoader.trimTextures()

            TRIM_MEMORY_UI_HIDDEN,
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE,
                -> {
                QuranAtlasLoader.clearCache()
                FontResolver.getInstance().clearCache()

                // Evicting only drops the references. The texture pages are large — the Uthmani
                // atlas is a single 4093x3409 page, 13 MB once decoded to ALPHA_8 — and their
                // pixels come back only when the collector runs, which a process that just went
                // to background has no reason to do: measured on an A55, anonymous RSS gave back
                // 1.7 MB in two minutes on its own. `QuranAtlasTextureStore` used to make this
                // deterministic with `recycle()`, but that raced with in-flight draws (the reader
                // fetches each texture inside the draw lambda, and the widget rasterizer reads the
                // same bitmaps from a worker), so ask for the collection instead. The pause is
                // invisible here — there is no UI left to stutter.
                //
                // Play's memory metric is anonymous RSS + swap, so a release that is merely
                // unreachable does not count; it has to be handed back to the OS.
                System.gc()
            }
        }

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE && level == TRIM_MEMORY_COMPLETE) {
            DatabaseProvider.closeAll()
        }
    }
}
