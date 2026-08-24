package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.GithubApi
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.models.recitation2.AvailableRecitationTranslationsModel
import com.cafarovceyxun.anamuslim.api.models.recitation2.AvailableRecitationsModel
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationModelBase
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationQuranModel
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationTranslationModel
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.compose.utils.appFallbackLanguageCodes
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path

/**
 * The reciter catalog: manifests (downloaded from GitHub, cached on disk), reciter selection, and
 * the on-disk layout of downloaded chapter audio and timing files.
 *
 * Platform-neutral — file I/O goes through [AppFileSystem] (okio) and the network through
 * [GithubApi] (Ktor), so both platforms share one catalog. Android exposes the `java.io.File`
 * views its download workers need via extensions in `RecitationModelManagerAndroid.kt`.
 */
object RecitationModelManager : RecitationModelSource {

    private val DIR_NAME_LEGACY: String = AppFileSystem.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "recitations"
    )
    private val DIR_NAME: String = AppFileSystem.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "recitations_v2"
    )

    private const val QURAN_MANIFEST_FILENAME = "available_recitations.json"
    private const val TRANSLATION_MANIFEST_FILENAME = "available_recitation_translations.json"
    private const val RECITATION_AUDIO_FILENAME_FORMAT_LOCAL: String = "%03d.mp3"

    private var cachedQuran: AvailableRecitationsModel? = null

    private var cachedTranslation: AvailableRecitationTranslationsModel? = null

    override var forceRefreshQuran = false
    override var forceRefreshTranslation = false

    // RecitationModelSource no-arg entry points; delegate to the parameterised overloads below
    // using the current force-refresh flags (identical to those methods' default arguments).
    override suspend fun getAllQuranModel(): AvailableRecitationsModel? =
        getAllQuranModel(forceRefreshQuran)

    override suspend fun getAllTranslationModel(): AvailableRecitationTranslationsModel? =
        getAllTranslationModel(forceRefreshTranslation)

    private val quranLoadLock = Mutex()
    private val translationLoadLock = Mutex()

    fun migrateLegacyData() {
        CoroutineScope(Dispatchers.IO).launch {
            // There is nothing to migrate as the new implementation is completely different
            // and does not rely on the old data structure. We can simply delete the old data
            // to free up space.
            AppFileSystem.deleteRecursively(AppFileSystem.appFilesDir() / DIR_NAME_LEGACY)
        }
    }

    suspend fun resolveModels(settings: PlayerSettings): Pair<RecitationQuranModel?, RecitationTranslationModel?> {
        val audioOption = settings.audioOption
        val reciterId = settings.reciter
        val translationReciterId = settings.translationReciter
        val resolveQuran = audioOption != AudioOption.ONLY_TRANSLATION
        val resolveTranslation = audioOption != AudioOption.ONLY_QURAN

        return Pair(
            if (resolveQuran) (if (reciterId.isNullOrBlank()) getSelectedQuranModel() else getQuranModel(
                reciterId
            )) else null,
            if (resolveTranslation) (if (translationReciterId.isNullOrBlank()) getSelectedTranslationModel() else getTranslationModel(
                translationReciterId
            )) else null
        )
    }

    suspend fun getSelectedQuranModel(): RecitationQuranModel? {
        val id = RecitationPreferences.getReciterId()

        if (id.isNullOrBlank() || id == "as_sudais") {
            val reciters = getAllQuranModel()?.reciters
            if (reciters.isNullOrEmpty()) return null

            // Prioritize yasser_ad_dussary as default
            val chosen = reciters.firstOrNull { it.id == "ad_dussary" }
                ?: reciters.firstOrNull { it.isDefault }
                ?: reciters.first()

            RecitationPreferences.setReciterId(chosen.id)

            return chosen
        }

        return getQuranModel(id)
    }

    suspend fun getSelectedTranslationModel(): RecitationTranslationModel? {
        val id = RecitationPreferences.getTranslationReciterId()

        if (id.isNullOrBlank()) {
            val reciters = getAllTranslationModel()?.reciters ?: return null

            val chosen = reciters.selectTranslationByLocaleWithFallback() ?: return null

            RecitationPreferences.setTranslationReciterId(chosen.id)

            return chosen
        }

        return getTranslationModel(id)
    }

    suspend fun getQuranModel(
        id: String?
    ): RecitationQuranModel? {
        return getAllQuranModel()?.reciters?.selectById(id)
    }

    suspend fun getTranslationModel(
        id: String?
    ): RecitationTranslationModel? {
        return getAllTranslationModel()?.reciters?.selectById(id)
    }

    suspend fun getAllQuranModel(
        forceRefresh: Boolean = forceRefreshQuran
    ): AvailableRecitationsModel? {
        val inMemory = cachedQuran

        if (!forceRefresh && inMemory != null) {
            return inMemory
        }

        return quranLoadLock.withLock {
            val recheck = cachedQuran

            if (!forceRefresh && recheck != null) {
                return@withLock recheck
            }

            val model = if (!forceRefresh) {
                loadQuranFromLocal()
            } else {
                null
            } ?: loadQuranFromNetwork()

            cachedQuran = model
            forceRefreshQuran = false

            model
        }
    }

    suspend fun getAllTranslationModel(
        forceRefresh: Boolean = forceRefreshTranslation
    ): AvailableRecitationTranslationsModel? {
        val inMemory = cachedTranslation

        if (!forceRefresh && inMemory != null) {
            return inMemory
        }

        return translationLoadLock.withLock {
            val recheck = cachedTranslation

            if (!forceRefresh && recheck != null) {
                return@withLock recheck
            }

            val fetched = (if (!forceRefresh) loadTranslationFromLocal() else null)
                ?: loadTranslationFromNetwork()

            // Never null: the bundled Azerbaijani entry stands in when the manifest is
            // unreachable, so a fresh offline install still offers translation audio.
            val model = withBundledTranslations(fetched)

            cachedTranslation = model
            forceRefreshTranslation = false

            model
        }
    }

    suspend fun refreshManifests() {
        loadQuranFromNetwork()
        loadTranslationFromNetwork()
    }

    override suspend fun getCurrentReciterNameForAudioOption(): String {
        val audioAudio = RecitationPreferences.getAudioOption()

        val isBoth = audioAudio == AudioOption.BOTH
        val isOnlyTransl = audioAudio == AudioOption.ONLY_TRANSLATION

        val quranReciterName =
            if (!isOnlyTransl) getSelectedQuranModel()?.getReciterName() else null

        val translationReciterName =
            if (isBoth || isOnlyTransl) getSelectedTranslationModel()?.getReciterName() else null

        val reciterName = if (
            isBoth &&
            !quranReciterName.isNullOrEmpty() &&
            !translationReciterName.isNullOrEmpty()
        ) {
            "$quranReciterName & $translationReciterName"
        } else {
            quranReciterName ?: translationReciterName ?: ""
        }

        return reciterName
    }

    private suspend fun loadQuranFromLocal(): AvailableRecitationsModel? =
        withContext(Dispatchers.IO) {
            val file = getQuranManifestPath()

            if ((AppFileSystem.size(file) ?: 0L) == 0L) {
                return@withContext null
            }

            try {
                val model = JsonHelper.json.decodeFromString<AvailableRecitationsModel>(
                    AppFileSystem.readText(file)
                )
                filterReciters(model)
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationManager.loadQuranFromLocal")
                null
            }
        }

    private suspend fun loadTranslationFromLocal(): AvailableRecitationTranslationsModel? =
        withContext(Dispatchers.IO) {
            val file = getTranslationManifestPath()

            if ((AppFileSystem.size(file) ?: 0L) == 0L) {
                return@withContext null
            }

            try {
                JsonHelper.json.decodeFromString<AvailableRecitationTranslationsModel>(
                    AppFileSystem.readText(file)
                )
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationManager.loadTranslationFromLocal")
                null
            }
        }

    private suspend fun loadQuranFromNetwork(): AvailableRecitationsModel? =
        withContext(Dispatchers.IO) {
            try {
                downloadManifest(
                    getQuranManifestPath(),
                    GithubApi.getAvailableRecitations(),
                )
                loadQuranFromLocal()
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationManager.loadQuranFromNetwork")
                null
            }
        }

    private suspend fun loadTranslationFromNetwork(): AvailableRecitationTranslationsModel? =
        withContext(Dispatchers.IO) {
            try {
                downloadManifest(
                    getTranslationManifestPath(),
                    GithubApi.getAvailableRecitationTranslations(),
                )
                loadTranslationFromLocal()
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationManager.loadTranslationFromNetwork")
                null
            }
        }

    /** Root of the reciter storage tree; created on first access. */
    fun getRecitationsDir(): Path = AppFileSystem.makeAndGetAppResourceDir(DIR_NAME)

    /**
     * Counts non-empty `.mp3` files under per-reciter dirs and how many reciter dirs have at least one
     * (excludes `timing_metadata` and manifest JSON files).
     */
    override fun getDownloadedAudioStats(): Pair<Int, Int> {
        val root = getRecitationsDir()
        var mp3Count = 0
        var recitersWithAudio = 0

        AppFileSystem.listDirectories(root)
            .filter { it.name != "timing_metadata" }
            .forEach { dir ->
                var hasMp3 = false
                AppFileSystem.listFilesRecursively(dir).forEach { f ->
                    if ((AppFileSystem.size(f) ?: 0L) > 0L && f.name.endsWith(".mp3", ignoreCase = true)) {
                        mp3Count++
                        hasMp3 = true
                    }
                }
                if (hasMp3) recitersWithAudio++
            }

        return mp3Count to recitersWithAudio
    }

    /** Removes all downloaded chapter audio (and any other files) for this reciter id. */
    override fun deleteReciterAudioDirectory(reciterId: String) {
        AppFileSystem.deleteRecursively(getRecitationsDir() / reciterId)
        AppFileSystem.delete(getRecitationTimingPath(reciterId))
    }

    fun getRecitationAudioPath(reciterId: String, chapterNo: Int): Path {
        val filename = StringUtils.formatInvariant(
            RECITATION_AUDIO_FILENAME_FORMAT_LOCAL,
            chapterNo,
        )

        return getRecitationsDir() / reciterId / filename
    }

    fun getRecitationTimingPath(reciterId: String): Path =
        getRecitationsDir() / "timing_metadata" / "$reciterId.json"

    private fun getQuranManifestPath() = getRecitationsDir() / QURAN_MANIFEST_FILENAME

    private fun getTranslationManifestPath() = getRecitationsDir() / TRANSLATION_MANIFEST_FILENAME

    /** Writes to a sibling temp file first so a failed write cannot leave a corrupt manifest. */
    private fun downloadManifest(file: Path, content: String) {
        val tempFile = file.parent!! / "${file.name}.tmp"

        try {
            AppFileSystem.writeText(tempFile, content)
            AppFileSystem.atomicMove(tempFile, file)
        } finally {
            AppFileSystem.delete(tempFile)
        }
    }

    private fun filterReciters(model: AvailableRecitationsModel): AvailableRecitationsModel {
        // Allow-list: only these reciters are shown, in this exact order.
        // Every id here has verse (or word) timing available.
        val allowedIdsInOrder = listOf(
            "al_afasy",
            "ad_dussary",
            "al_husary_muallim",
            "al_ghamdi",
            "al_qatami",
            "al_ajmi",
            "fares_abbad",
            "muhammad_jibreel",
            "basfar",
        )
        val allowed = allowedIdsInOrder.toHashSet()

        // Reciters that are not in the upstream manifest but ship with the app
        // (audio streamed from a public source, timing bundled in assets).
        val bundled = bundledReciters().filter { b -> model.reciters.none { it.id == b.id } }

        val filtered = (model.reciters + bundled)
            .filter { it.id in allowed }
            .onEach { it.isDefault = (it.id == "ad_dussary") }
            .sortedBy { allowedIdsInOrder.indexOf(it.id) }
            .toMutableList()

        // Ensure at least one default if Dossari is missing from the manifest
        if (filtered.isNotEmpty() && filtered.none { it.isDefault }) {
            filtered[0].isDefault = true
        }

        return model.copy(reciters = filtered)
    }

    /**
     * Reciters bundled with the app. Audio is streamed from quranicaudio and the
     * verse timing ships as a gzipped asset (see `assets/recitation_timings/`),
     * referenced via the `asset://` scheme in [RecitationModelBase.timingUrl].
     */
    private fun bundledReciters(): List<RecitationQuranModel> = listOf(
        RecitationQuranModel(style = null).apply {
            id = "basfar"
            reciter = "Abdullah Basfar"
            urlTemplate =
                "https://download.quranicaudio.com/quran/abdullaah_basfar/{chapNo:%03d}.mp3"
            timingUrl = "asset://recitation_timings/basfar.json"
            timingVersion = 1
        },
    )

    /**
     * Merges the reciters that ship with the app into whatever the manifest gave us (manifest
     * wins on id collision, so a published entry can re-point the URL without an app update).
     *
     * The Azerbaijani track is a synthetic voice generated by `tools/tts` — no human reciter
     * exists for this translation — so it is named as such in the picker.
     */
    private fun withBundledTranslations(
        model: AvailableRecitationTranslationsModel?,
    ): AvailableRecitationTranslationsModel {
        // Allow-list, exactly like [filterReciters] does for Quran reciters: only voices this
        // project publishes are offered. Without it the upstream manifest's German/French/Turkish
        // entries show up — and they keep showing up after the switch to our own manifest, because
        // a cached manifest on disk is read in preference to the network.
        val allowedIds = setOf("tts_az_v1")

        val fromManifest = model?.reciters.orEmpty().filter { it.id in allowedIds }
        val missing = bundledTranslationReciters().filter { bundled ->
            fromManifest.none { it.id == bundled.id }
        }

        val all = fromManifest + missing

        return AvailableRecitationTranslationsModel(reciters = all)
    }

    private fun bundledTranslationReciters(): List<RecitationTranslationModel> = listOf(
        RecitationTranslationModel(
            langCode = "az",
            langName = "Azərbaycan",
            book = "AnaMuslim",
        ).apply {
            id = "tts_az_v1"
            reciter = "AnaMuslim TTS"
            isDefault = true
            urlTemplate =
                "https://github.com/cafarovceyxun/AnaMuslim/releases/download/tts-az-quran-v1/{chapNo:%03d}.mp3"
            // Vaxt cədvəli **paketlə gəlir** (49 KB, gzip): şəbəkədən çəkilsəydi, fayl repoya
            // push olunana qədər ayə sinxronu işləməzdi — telefonda məhz bu baş verdi.
            // `RecitationAudioResolver` `asset://` sxemini Compose Resources-dan oxuyur və
            // gzip-i özü açır (`basfar` qarisi ilə eyni yol).
            timingUrl = "asset://recitation_timings/tts_az_v1.json.gz"
            timingVersion = 1
        },
    )

    private fun <T : RecitationModelBase> List<T>.selectById(id: String?): T? {
        if (isEmpty()) return null
        if (id.isNullOrBlank()) return firstOrNull()
        return firstOrNull { it.id == id } ?: firstOrNull()
    }

    private fun List<RecitationTranslationModel>.selectTranslationByLocaleWithFallback(): RecitationTranslationModel? {
        if (isEmpty()) return null

        val candidates = appFallbackLanguageCodes()
            .map { it.lowercase() }
            .flatMap { sequenceOf(it, it.substringBefore('-')) }
            .distinct()

        return candidates
            .mapNotNull { candidate ->
                firstOrNull { it.langCode.equals(candidate, ignoreCase = true) }
            }
            .firstOrNull()
            ?: firstOrNull { it.langCode.equals("en", ignoreCase = true) }
            ?: firstOrNull()
    }
}
