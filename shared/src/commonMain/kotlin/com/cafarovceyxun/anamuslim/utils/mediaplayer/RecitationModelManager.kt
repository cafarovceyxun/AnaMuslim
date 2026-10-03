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

    /** Layihənin öz azərbaycanca səsi — tərcümə səsi seçicisində yeganə buraxılan id. */
    const val TTS_AZ_ID = "tts_az_v1"

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
            // Səs mənbəyi dəyişmiş qarilərin köhnə endirmələri — yeni vaxt cədvəli onlarla tutmur.
            STALE_AUDIO_DIRS.forEach { AppFileSystem.deleteRecursively(getRecitationsDir() / it) }
        }
    }

    /**
     * Qari id-si dəyişmədən **səs dəsti** dəyişəndə endirilmiş fayllar yeni qovluğa yazılır: köhnə
     * fayl yerində qalsaydı, [RecitationAudioResolver] onu tapıb yeni cədvəllə çalardı (fayl adı
     * eynidir — `NNN.mp3`) və işıqlanma səssizcə sürüşərdi. Id isə dəyişmir, çünki seçilmiş qari
     * və ehtiyat nüsxə onu saxlayır.
     */
    private val AUDIO_DIR_OVERRIDES = mapOf("basfar" to "basfar_archive")
    private val STALE_AUDIO_DIRS = listOf("basfar")

    private fun audioDirName(reciterId: String) = AUDIO_DIR_OVERRIDES[reciterId] ?: reciterId

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
        AppFileSystem.deleteRecursively(getRecitationsDir() / audioDirName(reciterId))
        AppFileSystem.delete(getRecitationTimingPath(reciterId))
    }

    fun getRecitationAudioPath(reciterId: String, chapterNo: Int): Path {
        val filename = StringUtils.formatInvariant(
            RECITATION_AUDIO_FILENAME_FORMAT_LOCAL,
            chapterNo,
        )

        return getRecitationsDir() / audioDirName(reciterId) / filename
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
            "maher_al_muaiqly",
            "ad_dussary",
            "al_husary_muallim",
            "basfar",
            "badr_al_turki",
            "al_ghamdi",
            "al_qatami",
            "al_ajmi",
            "bandar_baleela",
            "fares_abbad",
            "muhammad_jibreel",
            "ali_hajjaj_alsouasi",
            "raad_al_kurdi",
            "khaled_almuhanna",
            "yasser_salama_hadr",
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
     * verse timing ships as a plain JSON resource (`composeResources/files/recitation_timings/`),
     * referenced via the `asset://` scheme in [RecitationModelBase.timingUrl].
     */
    private fun bundledReciters(): List<RecitationQuranModel> = listOf(
        // ⚠️ `archive/` dəsti — `basfar.json` (Quran.com-un `abdullah_basfar` bazası) yalnız ona
        // aiddir. 2026-10-03-ə qədər URL `abdullaah_basfar/` kökünü göstərirdi: o, BAŞQA yazıdır
        // (yalnız Fatihə eynidir, Yasin 844 s-dir, archive-də 1010 s) — 113 surədə işıqlanma
        // səhv idi. Köhnə endirmələr ona görə ayrıca qovluqda qalır, bax [AUDIO_DIR_OVERRIDES].
        RecitationQuranModel(style = null).apply {
            id = "basfar"
            reciter = "Abdullah Basfar"
            urlTemplate =
                "https://download.quranicaudio.com/quran/abdullaah_basfar/archive/{chapNo:%03d}.mp3"
            timingUrl = "asset://recitation_timings/basfar.json"
            timingVersion = 2
        },
        // 2026-10-03: aşağıdakıların ayə cədvəli Quran.com-un «Quran for Android» tətbiqinin
        // gapless bazalarından çevrilib (`files.quran.app/hafs/databases/audio/<ad>.zip`, sqlite:
        // ayə başlanğıcı, 999 = surə sonu). Hər baza **eyni quranicaudio dəstinə** bağlıdır —
        // başqa serverdəki eyni qarinin yazısı ilə vaxtlar tutmur (Bandar: yalnız `complete/`,
        // Maher: yalnız `tvquran/`; dəst-baza cütləri həmin tətbiqin qari siyahısındandır).
        // Yoxlama: Yasin-də sərhədlərin fasiləyə düşməsi + əl-Bəqərə/əl-Kəhf sonunun fayl
        // müddəti ilə tutuşdurulması.
        bundledReciter(
            "badr_al_turki", "Badr Al-Turki",
            "https://download.quranicaudio.com/quran/badr_al_turki/mp3/{chapNo:%03d}.mp3",
        ),
        bundledReciter(
            "bandar_baleela", "Bandar Baleela",
            "https://download.quranicaudio.com/quran/bandar_baleela/complete/{chapNo:%03d}.mp3",
        ),
        // Mənbə bazasında 38:86 və 56:93 səhvən surə sonuna yazılıb (mp3quran-ın cədvəlində də
        // eynidir) — JSON-da səsdəki fasilənin ortası ilə düzəldilib.
        bundledReciter(
            "ali_hajjaj_alsouasi", "Ali Hajjaj Al-Souasi",
            "https://download.quranicaudio.com/quran/ali_hajjaj_alsouasi/{chapNo:%03d}.mp3",
        ),
        bundledReciter(
            "raad_al_kurdi", "Raad Al-Kurdi",
            "https://download.quranicaudio.com/quran/raad_mohammad_al_kurdi/mp3/{chapNo:%03d}.mp3",
        ),
        // Mənbə bazasında 5 ayənin başlanğıcı korlanıb (11:123, 13:43, 20:135, 23:82, 35:45 —
        // məs. 1946501-dən sonra 4864). JSON-da səsin zəiflədiyi yerə görə **təxmini** düzəldilib
        // (qiraət əks-sədalıdır, aydın fasilə yoxdur) — bu beş ayədə işıqlanma ±1–4 s sürüşə bilər.
        bundledReciter(
            "maher_al_muaiqly", "Maher Al-Muaiqly",
            "https://mirrors.quranicaudio.com/tvquran/maher_al_mu3aiqly/{chapNo:%03d}.mp3",
        ),
        bundledReciter(
            "khaled_almuhanna", "Khalid Al-Muhanna",
            "https://mirrors.quranicaudio.com/qurancomplex/khaled_almuhanna/{chapNo:%03d}.mp3",
        ),
        // Bazada surə sonu (999) yoxdur — son ayənin sonu faylın müddətidir (ID3 teqi çıxılmaqla).
        bundledReciter(
            "yasser_salama_hadr", "Yasser Salama",
            "https://mirrors.quranicaudio.com/ayahapp/yasser_salama_hadr/{chapNo:%03d}.mp3",
        ),
    )

    private fun bundledReciter(id: String, name: String, urlTemplate: String) =
        RecitationQuranModel(style = null).apply {
            this.id = id
            reciter = name
            this.urlTemplate = urlTemplate
            timingUrl = "asset://recitation_timings/$id.json"
            timingVersion = 1
        }

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
        val allowedIds = setOf(TTS_AZ_ID)

        val fromManifest = model?.reciters.orEmpty().filter { it.id in allowedIds }
        val missing = bundledTranslationReciters().filter { bundled ->
            fromManifest.none { it.id == bundled.id }
        }

        val all = (fromManifest + missing).onEach { it.applyKnownVoiceNames() }

        return AvailableRecitationTranslationsModel(reciters = all)
    }

    /**
     * Seçici siyahısındakı adlar **koddan** gəlir, manifestdən yox.
     *
     * Səbəb: manifest paylanmış bir fayldır (`tts_az_v1` üçün GitHub release-dəki `translations.json`)
     * və diskdə keşlənir — orada qalan köhnə «AnaMuslim TTS» adı tətbiq güncəllənəndən sonra da
     * ekranda qalırdı. Ad istifadəçinin gördüyü şeydir, ona görə onu manifestin ixtiyarına
     * buraxmırıq: səs Mürşüd Yusifoğlunun tərcüməsini oxuyur, siyahıda isə əvvəlcə dil, altında
     * tərcüməçi görünür.
     */
    private fun RecitationTranslationModel.applyKnownVoiceNames() {
        if (id != TTS_AZ_ID) return
        langName = "Azərbaycan dili"
        reciter = "Mürşüd Yusifoğlu"
    }

    private fun bundledTranslationReciters(): List<RecitationTranslationModel> = listOf(
        RecitationTranslationModel(
            langCode = "az",
            langName = "Azərbaycan dili",
            book = "AnaMuslim",
        ).apply {
            id = TTS_AZ_ID
            reciter = "Mürşüd Yusifoğlu"
            isDefault = true
            urlTemplate =
                "https://github.com/cafarovceyxun/AnaMuslim/releases/download/tts-az-quran-v1/{chapNo:%03d}.mp3"
            // Vaxt cədvəli **paketlə gəlir**: şəbəkədən çəkilsəydi, fayl repoya push olunana
            // qədər ayə sinxronu işləməzdi — telefonda məhz bu baş verdi.
            // ⚠️ **Sıxılmamış saxlanılır.** `.gz` qoyanda resurs qablaşdırması onu açır və
            // uzantını atır (APK-da `tts_az_v1.json` görünürdü), tətbiq isə `.gz` adını
            // axtarıb tapmırdı → cədvəl null → ayə sinxronu yox. `basfar.json` da bu səbəbdən
            // sıxılmamışdır.
            timingUrl = "asset://recitation_timings/tts_az_v1.json"
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
