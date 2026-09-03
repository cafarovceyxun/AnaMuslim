package com.cafarovceyxun.anamuslim.utils.prayer.location

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.NetworkClient
import com.cafarovceyxun.anamuslim.api.resolveInventoryUrl
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrefKey
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationAudioFileDownloader
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okio.GzipSource
import okio.Path
import okio.buffer
import okio.use

/**
 * Genişləndirilmiş şəhər kataloqunun endirilməsi və saxlanması.
 *
 * ### Niyə var
 * Paketdəki [CityCatalog] siyahısında yalnız iri şəhərlər var (bütün Azərbaycan + paytaxtlar +
 * region ≥ 100k + dünya ≥ 200k = 3 500). Bu, kiçik yaşayış məntəqələrini tamamilə kənarda qoyur:
 * Gədəbəy (≈14 500 nəfər) siyahıda **yoxdur**, ona görə oradakı istifadəçi ya qonşu şəhəri seçir,
 * ya da koordinatı əl ilə yazır. GeoNames `cities5000` həmin məntəqələri əhatə edir, amma xam
 * halda ~3 MB-dır — APK-ya qoymaq üçün çox, bir dəfə endirmək üçün az.
 *
 * ### Oflaynlıq pozulmur
 * [CityCatalog] sənədində oflaynlıq **dizayn qərarı** kimi yazılıb, ölçü güzəşti kimi yox: şəhər
 * seçimi məhz internetin olmadığı anlarda lazım olur (təyyarə rejimi, SIM-siz telefon, rədd edilmiş
 * icazə). Ona görə paketdəki siyahı **yerində qalır** və heç vaxt silinmir; bu store yalnız onun
 * üstünə daha zəngin nüsxə qoyur. Endirmə baş tutmasa da, yarımçıq qalsa da tətbiq eyni şəkildə
 * işləyir — sadəcə kiçik kəndlər tapılmır.
 *
 * ### Axın
 * Manifest (kiçik JSON) → versiya paketdəkindən yenidirsə `.tsv.gz` endirilir → açılır →
 * **oxunub yoxlanılır** → atomik köçürmə ilə quraşdırılır. Yoxlama quraşdırmadan əvvəldir: pozuq
 * yükləmə işlək kataloqu əvəz etməməlidir.
 *
 * ⚠️ Endirmə üçün [com.cafarovceyxun.anamuslim.utils.univ.BackgroundFileTransfer] **qəsdən
 * işlədilmir**. O, iOS-da fon `NSURLSession`-a keçir və yükləmə tətbiq bağlandıqdan sonra bitəndə
 * sistemi tətbiqi yenidən işə salmağa məcbur edir — istifadəçinin xəbəri olmayan 1,5 MB-lıq arxa fon
 * işi üçün bu mütənasib deyil. Adi Ktor axını yarımçıq qalsa növbəti açılışda təkrarlanır, vəssalam.
 *
 * ⚠️ İki açar da **cihaza bağlıdır** ([com.cafarovceyxun.anamuslim.utils.univ.PreferenceBackup.DEVICE_LOCAL_KEYS]):
 * versiya nömrəsi ehtiyat nüsxə ilə yeni telefona köçsə, fayl köçmədiyi üçün tətbiq «artıq var»
 * sanıb yükləməni bloklayardı.
 */
object CityCatalogStore {

    /**
     * Manifest bu reponun `inventory/`-sindədir, Supabase Storage-da yox.
     *
     * `ghraw://` sxemi [resolveInventoryUrl] ilə istifadəçinin **seçdiyi mirror-a** açılır
     * (`gh-proxy` / `raw.githubusercontent` / `cdn.jsdelivr`, bax `DownloadSourceUtils`). Supabase
     * Storage-ın tək hostu var: o bloklananda istifadəçinin əlində heç bir alternativ qalmır,
     * halbuki burada Ayarlardan bir toxunuşla dəyişir. Statik resursların hamısı (atlas, WBW,
     * fontlar, TTS manifesti) onsuz da bu yoldan gedir; Supabase dinamik/moderasiya olunan məzmun
     * üçündür.
     */
    private const val MANIFEST_URL =
        "ghraw://cafarovceyxun/AnaMuslim/main/inventory/prayer/cities.json"

    private const val DIR_NAME = "prayer"
    private const val FILE_NAME = "cities.tsv"

    /**
     * Bundan az sətir olan nüsxə quraşdırılmır — paketdəki siyahı onsuz da 3 500 şəhərdir, yəni
     * kiçik fayl «yeniləmə» deyil, itkidir. Kəsilmiş yükləmə (gzip-i açılır, amma yarımçıqdır) məhz
     * bu qapıya ilişir.
     */
    private const val MIN_ROWS = 3_500

    private const val TAG = "CityCatalogStore"

    /** Quraşdırılmış nüsxənin manifest versiyası; 0 = yalnız paketdəki siyahı var. */
    private val KEY_VERSION = PrefKey(intPreferencesKey("prayer.cities_version"), 0)

    /** Manifestin son yoxlandığı yerli tarix (`yyyy-MM-dd`), boş = heç vaxt. */
    private val KEY_CHECKED_ON = PrefKey(stringPreferencesKey("prayer.cities_checked_on"), "")

    private val lock = Mutex()

    /**
     * Endirilmiş kataloqun mətni, yoxdursa **null** — çağıran tərəf paketdəki siyahıya qayıdır.
     *
     * Fayl bütöv oxunur (~3 MB): axtarış onsuz da bütün sətirləri yaddaşda gəzir, ona görə burada
     * axın vermək heç nə qazandırmır.
     */
    fun downloadedText(): String? {
        val file = catalogFile()
        if ((AppFileSystem.size(file) ?: 0L) <= 0L) return null

        return runCatching { AppFileSystem.readText(file) }
            .onFailure { AppLogger.saveError(it, "prayer.cities.read") }
            .getOrNull()
    }

    /**
     * Açılışda çağırılır. Şəbəkə yoxdursa, manifest oxunmursa və ya versiya köhnədirsə **səssizcə**
     * geri qayıdır: bu, istifadəçinin xahiş etmədiyi arxa fon işidir, xəta göstərməyə dəyməz.
     */
    suspend fun refreshIfNeeded() {
        lock.withLock {
            try {
                val present = AppFileSystem.exists(catalogFile())

                // Fayl yoxdursa gündəlik qapı keçilir — əks halda ilk açılışın yükləməsi sabaha qalar.
                if (present && DataStoreManager.readFirst(KEY_CHECKED_ON) == currentLocalDateIsoString()) {
                    return
                }

                val manifest = fetchManifest() ?: return

                val upToDate = present && manifest.version <= DataStoreManager.readFirst(KEY_VERSION)
                val usable = manifest.version > 0 && manifest.file.isNotBlank()

                // Gündəlik qapı YALNIZ iş bitəndə bağlanır: uğursuz yükləmə növbəti açılışda
                // təkrarlanmalıdır. Əks halda ilk açılışda kəsilən 1,5 MB istifadəçini bütün günü
                // kiçik siyahı ilə qoyardı — halbuki o, elə həmin gün şəhərini axtaracaq.
                // Pozuq manifest (versiya yoxdur/fayl adı boşdur) server problemidir, təkrar cəhd
                // onu düzəltmir — ona görə o hal da «bitdi» sayılır.
                if (upToDate || !usable || install(manifest)) {
                    DataStoreManager.write(KEY_CHECKED_ON, currentLocalDateIsoString())
                }
            } catch (e: Exception) {
                AppLogger.saveError(e, "prayer.cities.refresh")
            }
        }
    }

    /** Quraşdırma baş tutdumu — **false** olanda gündəlik qapı bağlanmır və növbəti açılış təkrarlayır. */
    private suspend fun install(manifest: CityCatalogManifest): Boolean = withContext(Dispatchers.IO) {
        val dir = dir()
        val archive = dir / "$FILE_NAME.gz.tmp"
        val staged = dir / "$FILE_NAME.staged"

        try {
            RecitationAudioFileDownloader.downloadToFile(
                resolveInventoryUrl(manifest.file),
                archive,
            ) { _, _ -> }
            gunzip(archive, staged)

            // Quraşdırmadan ƏVVƏL parse edilir: sayı az olan (yəni kəsilmiş və ya səhv) fayl
            // işlək kataloqu əvəz etməməlidir.
            val rows = CityCatalog.parse(AppFileSystem.readText(staged)).size
            if (rows < MIN_ROWS) {
                AppLogger.d(TAG, "kataloq rədd edildi: cəmi $rows sətir")
                return@withContext false
            }

            if (!AppFileSystem.atomicMove(staged, catalogFile())) {
                AppLogger.d(TAG, "kataloq köçürülə bilmədi")
                return@withContext false
            }

            DataStoreManager.write(KEY_VERSION, manifest.version)
            AppLogger.d(TAG, "kataloq quraşdırıldı: v${manifest.version}, $rows şəhər")
            true
        } finally {
            AppFileSystem.delete(archive)
            AppFileSystem.delete(staged)
        }
    }

    private suspend fun fetchManifest(): CityCatalogManifest? = withContext(Dispatchers.IO) {
        val response = NetworkClient.client.get(resolveInventoryUrl(MANIFEST_URL))
        if (!response.status.isSuccess()) return@withContext null

        JsonHelper.json.decodeFromString<CityCatalogManifest>(response.bodyAsText())
    }

    /** okio-nun `GzipSource`-u hər iki platformada var — ayrıca sıxma kitabxanası lazım deyil. */
    private fun gunzip(source: Path, target: Path) {
        target.parent?.let { AppFileSystem.fileSystem.createDirectories(it) }

        GzipSource(AppFileSystem.fileSystem.source(source)).buffer().use { gz ->
            AppFileSystem.fileSystem.sink(target).buffer().use { sink -> sink.writeAll(gz) }
        }
    }

    private fun dir(): Path = AppFileSystem.makeAndGetAppResourceDir(
        AppFileSystem.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, DIR_NAME)
    )

    private fun catalogFile(): Path = dir() / FILE_NAME
}

/**
 * `inventory/prayer/cities.json` — data faylının yanında duran kiçik manifest.
 *
 * Versiya ayrıca saxlanılır ki, kataloqu yeniləmək **iki faylı commit etməkdən** ibarət olsun.
 * Eyni forma [com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwManifest]-dədir.
 */
@Serializable
internal data class CityCatalogManifest(
    val version: Int = 0,
    /**
     * Data faylının tam URL-i, adətən `ghraw://…/inventory/prayer/cities-v<N>.tsv.gz`.
     *
     * Ad **versiya damğalıdır**, çünki jsDelivr branch ref-lərini bir həftəyə qədər keşləyir:
     * köhnə manifest ən pis halda yeniləməni gecikdirir, heç vaxt mövcud olmayan fayla işarə etmir.
     */
    val file: String = "",
    /** Yalnız məlumat üçün — qərarı store fayldan oxunan sətir sayı ilə verir. */
    val rows: Int = 0,
)
