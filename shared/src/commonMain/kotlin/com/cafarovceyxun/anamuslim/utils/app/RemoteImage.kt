package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.cafarovceyxun.anamuslim.api.NetworkClient
import com.cafarovceyxun.anamuslim.utils.AppLogger
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Uzaqdakı şəkli göstərmək üçün minimal yükləyici.
 *
 * Coil/Kamel kimi kitabxana **qəsdən əlavə edilmədi**: onların KMP variantları Ktor-u özləri ilə
 * gətirir və Gradle konflikti «ən yüksək versiya qalib» ilə həll etdiyi üçün bu, `ktor-client-core`
 * versiyasını sürüşdürə bilər — həmin nasazlıq yalnız Kotlin/Native-də, runtime-da çıxır
 * (bax CLAUDE.md, «Asılılıq versiya sürüşməsi tələsi»). Bizə lazım olan isə bir neçə kiçik PNG-dir:
 * paylaşılan Ktor klienti ilə endirmək və Compose Resources-un `decodeToImageBitmap()`-i ilə açmaq
 * kifayətdir.
 */
object RemoteImageLoader {

    private const val MAX_CACHED = 24

    private val cache = LinkedHashMap<String, ImageBitmap>()
    private val mutex = Mutex()

    suspend fun load(url: String): ImageBitmap? {
        mutex.withLock { cache[url] }?.let { return it }

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val response: HttpResponse = NetworkClient.client.get(url)
                if (!response.status.isSuccess()) return@runCatching null

                response.body<ByteArray>().takeIf { it.isNotEmpty() }?.decodeToImageBitmap()
            }.onFailure {
                AppLogger.d("RemoteImage", "Load failed for $url: ${it.message}")
            }.getOrNull()
        } ?: return null

        mutex.withLock {
            cache[url] = bitmap
            // Sadə FIFO: ekranda eyni anda bir neçə şəkil olur, keş yaddaşı yeməsin.
            while (cache.size > MAX_CACHED) {
                cache.remove(cache.keys.first())
            }
        }

        return bitmap
    }

    /** Şəkil dəyişəndə (admin yenisini yükləyəndə) köhnə nüsxə qalmasın. */
    suspend fun evict(url: String) = mutex.withLock { cache.remove(url); Unit }
}

/** [url] üçün şəkil; hələ yüklənməyibsə və ya alınmayıbsa `null`. */
@Composable
fun rememberRemoteImage(url: String?): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = url?.let { RemoteImageLoader.load(it) }
    }
    return state
}
