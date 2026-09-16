package com.cafarovceyxun.anamuslim.compose.components.qibla

import androidx.compose.ui.graphics.ImageBitmap
import com.cafarovceyxun.anamuslim.api.NetworkClient
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okio.Path
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Qiblə xəritəsinin tayl qatları.
 *
 * ### Niyə üç qat
 * İstifadəçinin əsas ssenarisi «öz damımı görüm və qibləyə yönəlim»dir, bu isə yüksək ayırdetməli
 * peyk tələb edir. Amma **açıq lisenziyalı yüksək ayırdetməli peyk mövcud deyil**: OpenStreetMap
 * peyk təsviri vermir, həqiqətən açıq olan Sentinel-2 isə 10 m/pikseldir (məhəllə seçilir, fərdi
 * dam yox). Ona görə default **pulsuz və açıq** qat, dam səviyyəsi isə istifadəçinin özünün açdığı
 * [SATELLITE_HD] qatıdır — yəni ödənişli kvota yalnız həqiqətən lazım olanda xərclənir.
 *
 * ### Zoom hədləri
 * [maxZoom] hər qatın **nativ** həddidir. Ondan yuxarı tayl sadəcə mövcud deyil; xəritə oradan
 * yuxarı qaldırılmır və UI istifadəçiyə HD qatını təklif edir. Nativ həddən yuxarı tayl istəmək
 * (overzoom) qəsdən edilmir — 404 gələr və xəritə boş qalar.
 */
enum class QiblaMapLayer(
    /** Proxy yolundakı ad. Serverdə ağ siyahıdadır. */
    val id: String,
    val maxZoom: Int,
    /** Hüquqi tələb — ekranda həmişə görünməlidir. Tərcümə olunmur. */
    val attribution: String,
) {
    /** OSM küçə xəritəsi. Şəhərdə bina konturları çəkilib, kənd yerində seyrəkdir. */
    STREET(id = "street", maxZoom = 18, attribution = "© OpenStreetMap contributors"),

    /** Sentinel-2 cloudless — CC BY 4.0, pulsuz, açarsız. 10 m/piksel. */
    SATELLITE(id = "sat", maxZoom = 14, attribution = "Sentinel-2 cloudless — EOX IT Services"),

    /** Yüksək ayırdetməli peyk. Açar **serverdədir**; kvota bitərsə [QiblaTileStore] onu söndürür. */
    SATELLITE_HD(id = "sat_hd", maxZoom = 18, attribution = "© MapTiler © OpenStreetMap contributors"),
}

/** Yaddaşda saxlanan tayl sayı. Bir ekran ~12–20 tayldır, yəni bu, bir neçə ekranlıq gəzintidir. */
private const val MEMORY_CACHE_TILES = 96

/** Diskdəki keşin yuxarı həddi. */
private const val DISK_CACHE_BYTES = 40L * 1024L * 1024L

/** Neçə yazıdan bir disk keşinin ölçüsü yoxlanılır — hər yazıda qovluq taramaq baha olardı. */
private const val EVICTION_CHECK_EVERY = 64

private const val LOG_TAG = "qibla.tiles"

/**
 * HD qatını söndürən cavab statusları: `401`/`403` (açar rədd edildi), `503` (server qatı
 * bilərəkdən bağlayıb — bax `supabase/functions/qibla-tiles`).
 */
private val DISABLING_STATUSES = setOf(401, 403, 503)

/**
 * Tayl anbarı: **yaddaş → disk → şəbəkə**.
 *
 * ### Məxfilik
 * Taylar birbaşa xəritə provayderinə yox, tətbiqin **öz Supabase Edge Function proxy-sinə** gedir.
 * Nəticədə istifadəçinin IP-si provayderə çatmır və HD qatının açarı repoda görünmür. Disk keşi
 * eyni məqsədə xidmət edir: bir dəfə görülmüş tayl bir daha sorğu yaratmır, yəni ekranı ikinci
 * dəfə açanda **heç nə göndərilmir**.
 *
 * ℹ️ Tayl URL-ləri paylaşılan [NetworkClient]-dən keçir, yəni `NetworkConfig.logger`-ə düşür — o
 * isə Android-də `Logger.print`-dir və **yalnız debug build-də** işləyir (`BuildConfig.DEBUG`),
 * relizdə heç nə yazılmır. Ona görə ayrıca HTTP klienti qurulmur.
 */
object QiblaTileStore {

    private val lock = ReentrantLock()
    private val memory = LinkedHashMap<String, ImageBitmap>()
    private var writesSinceEviction = 0

    private val _highResAvailable = MutableStateFlow(true)

    /**
     * HD qatı hazırda işləyirmi.
     *
     * Server onu söndürəndə (kvota, açarın ləğvi) proxy uğursuz cavab verir və bu bayraq düşür;
     * UI həmin an qatı **təklif etməyi dayandırır** və açıq peyk qatına qayıdır. Xəritənin ağarıb
     * qalması əvəzinə davranış dəyişir — `AppStoreReviewProvider.isAvailable` ilə eyni naxış.
     */
    val highResAvailable: StateFlow<Boolean> = _highResAvailable.asStateFlow()

    private val cacheDir: Path by lazy { AppFileSystem.makeAndGetAppResourceDir("qibla_tiles") }

    /**
     * Bir taylı qaytarır, alınmasa **null**.
     *
     * Uğursuzluq normal haldır (şəbəkə yox, tayl mövcud deyil) — çağıran tərəf həmin xananı boş
     * saxlayır, xəritə işləməyə davam edir.
     */
    suspend fun tile(layer: QiblaMapLayer, zoom: Int, x: Int, y: Int): ImageBitmap? {
        val key = "${layer.id}/$zoom/$x/$y"

        memoryGet(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            val path = cacheDir / layer.id / zoom.toString() / "${x}_$y.tile"

            readFromDisk(path)?.let { decoded ->
                memoryPut(key, decoded)
                return@withContext decoded
            }

            val bytes = download(layer, zoom, x, y) ?: return@withContext null

            val decoded = try {
                bytes.decodeToImageBitmap()
            } catch (e: Exception) {
                // Bozuk/yarımçıq cavab — diskə yazılmır ki, xəta əbədiləşməsin.
                AppLogger.saveError(e, "$LOG_TAG.decode")
                return@withContext null
            }

            writeToDisk(path, bytes)
            memoryPut(key, decoded)
            decoded
        }
    }

    private suspend fun download(layer: QiblaMapLayer, zoom: Int, x: Int, y: Int): ByteArray? = try {
        val url = "${SupabaseProvider.restUrl}/functions/v1/qibla-tiles/${layer.id}/$zoom/$x/$y"
        val response = NetworkClient.client.get(url) {
            // Edge Function default olaraq JWT gözləyir; anon açar onsuz da tətbiqin içindədir.
            // Süiistifadəyə qarşı əsl qoruma serverdəki sürət limiti və z/x/y diapazon yoxlamasıdır.
            header("Authorization", "Bearer ${SupabaseProvider.anonKey}")
        }

        when {
            response.status.isSuccess() -> response.bodyAsBytes()

            // Server HD qatını söndürüb və ya açar işləmir — qat bu sessiya üçün bağlanır.
            //
            // ⚠️ **Yalnız bu üç status.** Əvvəl hər uğursuz cavab qatı söndürürdü, yəni yuxarı
            // axının keçici xətası (`502`) və ya bir taylın olmaması (`404` — dəniz üstündə normaldır)
            // HD-ni bütün sessiya boyu gizlədərdi və istifadəçi tətbiqi bağlayana qədər geri
            // qaytara bilməzdi. Söndürmə qərarı «bu qat mənim üçün əlçatan deyil» siqnalıdır,
            // «bu tayl gəlmədi» siqnalı yox.
            layer == QiblaMapLayer.SATELLITE_HD && response.status.value in DISABLING_STATUSES -> {
                _highResAvailable.value = false
                null
            }

            else -> null
        }
    } catch (e: Exception) {
        AppLogger.saveError(e, "$LOG_TAG.download")
        null
    }

    private fun readFromDisk(path: Path): ImageBitmap? = try {
        if (AppFileSystem.exists(path)) AppFileSystem.readBytes(path).decodeToImageBitmap() else null
    } catch (e: Exception) {
        AppLogger.saveError(e, "$LOG_TAG.disk.read")
        null
    }

    private fun writeToDisk(path: Path, bytes: ByteArray) {
        try {
            AppFileSystem.write(path) { sink -> sink.write(bytes) }
        } catch (e: Exception) {
            AppLogger.saveError(e, "$LOG_TAG.disk.write")
            return
        }

        val shouldCheck = lock.withLock {
            writesSinceEviction++
            if (writesSinceEviction >= EVICTION_CHECK_EVERY) {
                writesSinceEviction = 0
                true
            } else {
                false
            }
        }

        if (shouldCheck) evictIfOverBudget()
    }

    /**
     * Disk keşini həddə salır — ən köhnə fayllar silinir.
     *
     * Hər yazıda yox, [EVICTION_CHECK_EVERY] yazıdan bir çağırılır: qovluğu taramaq minlərlə tayl
     * olanda bahadır, hədd isə bir neçə tayl aşılsa heç nə olmur.
     */
    private fun evictIfOverBudget() {
        try {
            val files = AppFileSystem.listFilesRecursively(cacheDir)
            val total = files.sumOf { AppFileSystem.size(it) ?: 0L }

            if (total <= DISK_CACHE_BYTES) return

            // Hədəf 80%-dir ki, təmizlik hər dəfə yenidən işə düşməsin.
            var remaining = total - (DISK_CACHE_BYTES * 8 / 10)

            val oldestFirst = files.sortedBy {
                // `FileSystem.SYSTEM` ortaq kodda yoxdur; layihənin seam-i işlədilir.
                AppFileSystem.fileSystem.metadataOrNull(it)?.lastModifiedAtMillis ?: 0L
            }

            for (file in oldestFirst) {
                if (remaining <= 0L) break

                val size = AppFileSystem.size(file) ?: 0L
                if (AppFileSystem.delete(file)) remaining -= size
            }
        } catch (e: Exception) {
            AppLogger.saveError(e, "$LOG_TAG.evict")
        }
    }

    private fun memoryGet(key: String): ImageBitmap? = lock.withLock {
        // Yenidən yerləşdirmə LRU sırasını saxlayır — `RemoteImageCache` ilə eyni naxış, çünki
        // ortaq kodda access-order `LinkedHashMap` yoxdur.
        memory.remove(key)?.also { memory[key] = it }
    }

    private fun memoryPut(key: String, image: ImageBitmap) = lock.withLock {
        memory[key] = image
        if (memory.size > MEMORY_CACHE_TILES) {
            memory.keys.firstOrNull()?.let { memory.remove(it) }
        }
    }
}
