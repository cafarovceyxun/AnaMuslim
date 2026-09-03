package com.cafarovceyxun.anamuslim.shared

import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.compose.utils.DailyReminderProvider
import com.cafarovceyxun.anamuslim.compose.utils.IosDailyReminder
import com.cafarovceyxun.anamuslim.compose.utils.IosNotificationCenterDelegate
import com.cafarovceyxun.anamuslim.compose.utils.IosPrayerReminder
import com.cafarovceyxun.anamuslim.compose.utils.PrayerReminderProvider
import com.cafarovceyxun.anamuslim.compose.utils.installIosAppLanguage
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.IosDatabaseProvider
import com.cafarovceyxun.anamuslim.db.search.SearchHistoryProvider
import com.cafarovceyxun.anamuslim.db.search.SharedSearchHistorySource
import com.cafarovceyxun.anamuslim.db.translation.QuranTranslationDatabase
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.UserRepository
import com.cafarovceyxun.anamuslim.utils.preferences.getDataStorePath
import com.cafarovceyxun.anamuslim.search.TranslationSearchIndexer
import com.cafarovceyxun.anamuslim.utils.managers.HadithSyncProvider
import com.cafarovceyxun.anamuslim.utils.managers.ScriptResourceProvider
import com.cafarovceyxun.anamuslim.utils.managers.SharedHadithSync
import com.cafarovceyxun.anamuslim.utils.managers.SharedScriptResourceSource
import com.cafarovceyxun.anamuslim.utils.managers.SharedTranslationDownloader
import com.cafarovceyxun.anamuslim.utils.managers.TranslationDownloadProvider
import com.cafarovceyxun.anamuslim.utils.managers.TranslationPlatformHooks
import com.cafarovceyxun.anamuslim.utils.others.IosReadHistoryShortcuts
import com.cafarovceyxun.anamuslim.utils.app.AppStoreReviewProvider
import com.cafarovceyxun.anamuslim.utils.app.IosAppStoreReview
import com.cafarovceyxun.anamuslim.utils.app.IosDownloadNotifier
import com.cafarovceyxun.anamuslim.utils.download.IosBackgroundDownloads
import com.cafarovceyxun.anamuslim.utils.others.IosVotdShortcut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSBundle
import com.cafarovceyxun.anamuslim.utils.mediaplayer.AVFoundationAudioOutput
import com.cafarovceyxun.anamuslim.utils.mediaplayer.IosNowPlaying
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationDownloadProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelManager
import com.cafarovceyxun.anamuslim.utils.mediaplayer.IosWbwAudioPlayer
import com.cafarovceyxun.anamuslim.utils.mediaplayer.SharedRecitationDownloader
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.SharedRecitationPlayer
import com.cafarovceyxun.anamuslim.utils.reader.atlas.QuranAtlasLoader
import com.cafarovceyxun.anamuslim.utils.reader.atlas.SharedAtlasBundleLoader
import com.cafarovceyxun.anamuslim.utils.reader.wbw.SharedWbwResourceSource
import com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwManifest
import com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwResourceProvider
import com.cafarovceyxun.anamuslim.utils.supabase.ResourceUpdateManager
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.withContext
import com.cafarovceyxun.anamuslim.utils.prayer.location.CityCatalogStore

/**
 * iOS dependency-injection bootstrap — the platform-neutral subset of what `QuranApp.onCreate()`
 * does on Android, wiring the shared layer so it can run on-device. The Android-only seams
 * (WorkManager, notifications, widgets, resource IDs) are intentionally omitted; they belong to
 * later iOS-native phases. Most shared seams (`NetworkConfig`, `AppLocaleProvider`, `TranslUtils`
 * paths) already carry safe defaults, so only the mandatory ones are set here.
 */
private var initialized = false

/**
 * Serialises [initSharedForIos]. The plain `initialized` flag was enough while bootstrap was
 * synchronous; now that it suspends, the composition root and a background launch can both be
 * inside it, and the second caller has to *wait* rather than see the flag already set and go on to
 * use seams that are not registered yet.
 */
private val bootstrapMutex = Mutex()

/** Cached iOS [QuranRepository], built from the iOS Room databases. */
private val iosQuranRepository by lazy {
    QuranRepository(IosDatabaseProvider.quranDatabase, IosDatabaseProvider.externalQuranDatabase)
}

/** Cached iOS [UserRepository], built from the iOS user Room database. */
private val iosUserRepository by lazy {
    UserRepository(IosDatabaseProvider.userDatabase)
}

/** Cached iOS translations store — one process-wide connection, like Android's `DatabaseProvider`. */
private val iosQuranTranslationStore by lazy {
    QuranTranslationDatabase.open(quranTranslationDbPath())
}

/**
 * Cached iOS recitation player — one playback session per process, like Android's single media
 * service. Built lazily because constructing it activates the audio session.
 */
private val iosRecitationPlayer by lazy {
    SharedRecitationPlayer(AVFoundationAudioOutput()).also { player ->
        // Lock screen / Control Centre metadata and transport controls. Android gets the equivalent
        // from its media session; on iOS it has to be published explicitly.
        IosNowPlaying(player).start()
    }
}

/** Cached iOS translation downloader — one scope for the process, like Android's single WorkManager. */
private val iosTranslationDownloader by lazy { SharedTranslationDownloader() }

/** Cached iOS recitation downloader — owns the in-flight download state, like Android's WorkManager. */
private val iosRecitationDownloader by lazy { SharedRecitationDownloader() }

/** Cached iOS word-by-word resource source — catalogue cache plus in-flight pack installs. */
private val iosWbwResourceSource by lazy { SharedWbwResourceSource() }

/** Cached iOS script-resource source — KFQPC font installs plus atlas bundle downloads. */
private val iosScriptResourceSource by lazy { SharedScriptResourceSource() }

/** Cached iOS word-by-word clip player — one per process. */
private val iosWbwAudioPlayer by lazy { IosWbwAudioPlayer() }

/** Cached iOS search-history store — one instance so its write lock actually serialises. */
private val iosSearchHistorySource by lazy { SharedSearchHistorySource() }

/** Cached iOS hadith sync — one in-flight sync per process, like Android's unique work. */
private val iosHadithSync by lazy { SharedHadithSync() }

/** Startup-time background work that has no UI owner — Android's stand-in for this is WorkManager. */
private val iosBackgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

suspend fun initSharedForIos() = bootstrapMutex.withLock {
    if (initialized) return@withLock
    initialized = true

    // DataStore is `lateinit`; nothing may read preferences until this runs.
    DataStoreManager.init { getDataStorePath() }
    // …and nothing may read them *blocking* on the main thread: `DataStoreManager.read` is
    // synchronous and used all over the shared layer, so the store is loaded into memory here,
    // off the caller's thread. Without it the first read on the UI thread parks a user-interactive
    // thread on DataStore's IO worker — the priority inversion Xcode flags as a hang risk.
    withContext(Dispatchers.Default) {
        DataStoreManager.warmUp()
        // Fold the hadith reader's old scroll-distance choice into the shared step (Android parity).
        AppPreferences.migrateLegacyScrollStep()
        // Move any hadith Arabic font off a now-removed mushaf face onto the default book font.
        HadithPreferences.migrateArabicFontToBookFonts()
    }
    // `CFBundleVersion` (the build number), which `AppUpdateChecker` compares only against the
    // `ios` row of `app_releases` — never against Android's `versionCode`, which lives in a
    // different number space entirely. Falls back to "0", read as "unknown build", so a bundle
    // without the key never claims to be out of date.
    NetworkConfig.appVersionCode = {
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "0"
    }
    NetworkConfig.appVersionName = {
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    }
    // Repo DI seam: shared ViewModels obtain repositories without a Context (Android parity).
    RepositoryProvider.setQuranRepositoryProvider { iosQuranRepository }
    RepositoryProvider.setUserRepositoryProvider { iosUserRepository }
    RepositoryProvider.setExternalQuranDatabaseProvider { IosDatabaseProvider.externalQuranDatabase }
    RepositoryProvider.setHadithDatabaseProvider { IosDatabaseProvider.hadithDatabase }
    RepositoryProvider.setSearchIndexDatabaseProvider { IosDatabaseProvider.searchIndexDatabase }
    RepositoryProvider.setQuranTranslationStoreProvider { iosQuranTranslationStore }
    // Reciter catalog: the manifest loader is shared (Ktor + okio), so iOS registers the same
    // implementation Android does instead of falling back to the inert catalog.
    RecitationModelProvider.setSource { RecitationModelManager }
    // Recitation playback: shared policy (verse tracking, repeat, chapter transitions) over an
    // AVFoundation output. Android stays on its media3 service, which also owns the notification.
    RecitationPlayerProvider.setProvider { iosRecitationPlayer }
    // Store rating: StoreKit's own sheet, plus the App Store listing for the overflow menu's
    // "Rate App" row. Registering it is also what makes both appear — the shared prompt and the
    // menu row check `AppStoreReviewProvider.isAvailable` first.
    AppStoreReviewProvider.setProvider { IosAppStoreReview }
    // Translation downloads: the whole transfer is shared code (Ktor / Supabase / translation
    // store), so iOS runs it in-process. Android keeps WorkManager because it also needs the
    // download to survive backgrounding and show a notification; a background-capable iOS
    // transport (URLSession background session) can replace this later behind the same seam.
    TranslationDownloadProvider.setSource { iosTranslationDownloader }
    // Recitation downloads: same transfer code Android's workers run, driven by coroutines here.
    RecitationDownloadProvider.setProvider { iosRecitationDownloader }
    // Word-by-word audio: iOS plays the CDN's per-word MP3 clip (AVFoundation cannot decode the
    // downloadable WebM chapter files — see IosWbwAudioPlayer), so the *download* provider stays
    // unset (inert) and only the player + langCode are wired. `langCode` comes from the shared
    // WbW catalogue and drives RTL layout.
    WbwAudioProvider.langCodeProvider = { wbwId ->
        WbwManifest.getAvailable(false)?.wbw?.firstOrNull { it.id == wbwId }?.langCode
    }
    WbwAudioProvider.warmUpProvider = { iosWbwAudioPlayer.warmUp() }
    WbwAudioProvider.playProvider = { chapterNo, verseNo, wordIndex ->
        iosWbwAudioPlayer.play(chapterNo, verseNo, wordIndex)
    }
    // KFQPC page fonts and atlas glyph bundles: same archives, same destination directories both
    // resolvers already read. The atlas download streams the inventory zip and hands it to the
    // shared importer (SharedScriptResourceSource); the renderer landed in the atlas wave.
    ScriptResourceProvider.setSource { iosScriptResourceSource }
    // Word-by-word packs: shared catalogue + shared installer; versions live in DataStore here.
    WbwResourceProvider.setSource { iosWbwResourceSource }
    // Search history: Android keeps its legacy SQLite table; iOS starts empty, so it uses the
    // simpler DataStore-backed store rather than a second SQLite helper.
    SearchHistoryProvider.setProvider { iosSearchHistorySource }
    // Hadith sync: same shared engine Android's worker runs, without the scheduler around it.
    HadithSyncProvider.setSource { iosHadithSync }
    // Deleting a translation must drop its search rows too. Android schedules a worker for this;
    // iOS runs the same shared indexer directly (the hook is not suspend, hence the scope).
    TranslationPlatformHooks.removeSlugFromSearchIndex = { slug ->
        iosBackgroundScope.launch { TranslationSearchIndexer.removeSlug(slug) }
    }
    // Daily resource check + search-index reconciliation. Off the main thread; nothing on screen
    // waits for it. The same routine is what the BGProcessingTask runs when iOS wakes the app in
    // the background, so it lives in one place (see [runIosResourceMaintenance]).
    iosBackgroundScope.launch { runIosResourceMaintenance() }
    // Atlas glyph bundles: the importer (okio zip + streaming layout parser) and the texture
    // source (Compose Resources decode) are shared, so iOS registers the multiplatform loader.
    // Android keeps its own loader only for its ALPHA_8 texture store and the widget rasterizer.
    QuranAtlasLoader.setLoader(SharedAtlasBundleLoader())
    // Daily verse reminder: Android posts it from a WorkManager worker; iOS registers a repeating
    // local notification up front (it cannot run code at an arbitrary future moment). Same shared
    // content builder on both sides. `refresh()` re-arms the pending request with today's verse,
    // `installTapHandler()` routes a tapped notification into the reader.
    // ⚠️ `UNUserNotificationCenter.delegate` TƏK qlobaldır: iki modul onu ayrı-ayrı quranda
    // birincinin toxunuşları səssizcə ölür. Ona görə ortaq registry əvvəlcə qurulur, modullar
    // sonra öz `kind`-larını qeyd edir.
    IosNotificationCenterDelegate.install()
    DailyReminderProvider.setProvider { IosDailyReminder }
    IosDailyReminder.registerTapHandler()
    IosDailyReminder.refresh()
    // Namaz bildirişləri: eyni «əvvəlcədən yaz» modeli, ayrı `prayer_` prefiksi və sistem səsi ilə.
    // `install()` ön plana qayıdış müşahidəçisini qurur — məhdud üfüqün əsas kompensasiyası budur.
    PrayerReminderProvider.setProvider { IosPrayerReminder }
    IosPrayerReminder.registerTapHandler()
    IosPrayerReminder.install()
    IosPrayerReminder.refresh()
    // Download "finished / failed" notifications. Android gets these from its WorkManager foreground
    // workers, which do not exist here, so iOS posts them itself. Must come after
    // `IosNotificationCenterDelegate.install()` — that delegate is what lets a banner show while
    // the app is foregrounded.
    IosDownloadNotifier.install()
    // Re-attach the background download session: transfers started in a previous run may have
    // finished while the app was away, and the system only replays their callbacks to a session
    // created with the same identifier.
    IosBackgroundDownloads.install()
    // Repair stored reader preferences (Android runs this in QuranApp): reset a script whose fonts
    // or atlas bundle are not installed, and drop unavailable translation slugs. Shared logic; off
    // the main thread since it touches the DB and translation factory.
    iosBackgroundScope.launch {
        runCatching { ReaderPreferences.repairStoredPreferences() }
            .onFailure { println("[ios-bootstrap] reader tənzimləmə təmiri uğursuz: $it") }
    }
    // Bundled reader fonts (default Uthmani Hafs, DK Indopak, PDMS Islamic): extract the shared
    // composeResources copies to disk so the synchronous FontResolver can read them. Awaited rather
    // than launched because it must finish before the reader renders — otherwise the resolver caches
    // Default for a script whose font arrived a moment late. First launch copies three small TTFs;
    // later launches are three exists() checks. Off the caller's thread: this used to be a
    // `runBlocking` that stalled the main thread on file IO for the whole copy.
    withContext(Dispatchers.Default) {
        com.cafarovceyxun.anamuslim.utils.reader.BundledScriptFonts.ensureExtracted()
    }
    // Home-screen quick actions (Android launcher shortcuts): "continue reading" and "verse of the
    // day". Both publish through IosQuickActions so they coexist; the Swift app delegate forwards a
    // tap into IosQuickActions.handle(), which routes to the reader hooks by action type.
    IosReadHistoryShortcuts.install()
    IosVotdShortcut.install()
    // App language: restores the saved selection and registers the change hook. Must run before
    // the first composition — Compose Resources resolves strings against the locale it finds then.
    installIosAppLanguage()
}

/** Serialises [runIosResourceMaintenance] so the launch run and a background run never overlap. */
private val maintenanceMutex = Mutex()

/**
 * The launch-time maintenance Android schedules from `AppActions.scheduleActions`:
 *
 * 1. the once-a-day resource-version check — when a new version lands it re-pulls the hadith
 *    collection and every downloaded translation through the registered download seams;
 * 2. the extended city catalogue check — downloads it on first launch, then compares a manifest
 *    version once a day. Silent on failure: the bundled city list keeps working either way;
 * 3. the counterpart of `scheduleTranslationSearchIndexIfNeeded` — reconcile the FTS index with
 *    what is actually downloaded. Cheap when nothing changed (each book compares a fingerprint and
 *    stops), so it runs on every launch instead of tracking a schema/version preference.
 *
 * Called both from [initSharedForIos] and from the `BGProcessingTask` handler, which needs to
 * *await* it — hence the mutex: the background run waits for a launch run already in flight instead
 * of racing it, and its own pass then costs two guarded no-ops.
 */
internal suspend fun runIosResourceMaintenance() = maintenanceMutex.withLock {
    runCatching { ResourceUpdateManager.checkAndPerformUpdate() }
        .onFailure { println("[ios-bootstrap] resurs yeniləmə yoxlaması uğursuz: $it") }
    runCatching { CityCatalogStore.refreshIfNeeded() }
        .onFailure { println("[ios-bootstrap] şəhər kataloqu yoxlaması uğursuz: $it") }
    runCatching { TranslationSearchIndexer.syncAll() }
        .onFailure { println("[ios-bootstrap] axtarış indeksi sinxronu uğursuz: $it") }
}

/** Real on-device path for the translations DB, under `Documents/databases/`. */
private fun quranTranslationDbPath(): String {
    val dir = AppFileSystem.makeAndGetAppResourceDir("databases")
    return (dir / "QuranTranslation.db").toString()
}
