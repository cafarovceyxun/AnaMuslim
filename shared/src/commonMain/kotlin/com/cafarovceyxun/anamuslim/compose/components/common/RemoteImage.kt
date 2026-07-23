package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.cafarovceyxun.anamuslim.api.NetworkClient
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.utils.AppLogger
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

private const val IMAGE_CACHE_SIZE = 16

/**
 * In-memory cache of decoded images, keyed by URL.
 *
 * LRU order is kept by re-inserting on hit and dropping the eldest key on overflow — the same
 * pattern [com.cafarovceyxun.anamuslim.utils.reader.FontResolver] uses, since an access-ordered
 * `LinkedHashMap` has no common equivalent.
 */
private object RemoteImageCache {
    private val lock = ReentrantLock()
    private val entries = LinkedHashMap<String, ImageBitmap>()

    fun get(url: String): ImageBitmap? = lock.withLock {
        entries.remove(url)?.also { entries[url] = it }
    }

    fun put(url: String, image: ImageBitmap) = lock.withLock {
        entries[url] = image
        if (entries.size > IMAGE_CACHE_SIZE) {
            entries.keys.firstOrNull()?.let { entries.remove(it) }
        }
    }
}

/**
 * Loads and draws a remote image, or nothing at all while it loads or if it fails.
 *
 * Deliberately **not** an image library: the app has exactly one remote image (the reference
 * screen's hero), so this is a fetch through the shared Ktor client plus Compose Resources'
 * `decodeToImageBitmap()` — which works on every target. Adding Coil 3's KMP artifact to `shared`
 * for one image would cost Kotlin/Native link time for nothing, which is why the previous
 * `PlatformImageHooks` sink existed (Android-only Coil, so iOS silently drew no image at all).
 *
 * Caching is in-memory only. Android's Coil path had a disk cache; for a single header image that
 * is re-fetched at most once per cold start, that difference is not worth a cache implementation.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var image by remember(url) { mutableStateOf(RemoteImageCache.get(url)) }

    LaunchedEffect(url) {
        if (image != null) return@LaunchedEffect

        image = loadRemoteImage(url)
    }

    val bitmap = image ?: return

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

private suspend fun loadRemoteImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val bytes = NetworkClient.client.get(url).bodyAsBytes()
        val decoded = bytes.decodeToImageBitmap()

        RemoteImageCache.put(url, decoded)
        decoded
    } catch (e: Exception) {
        AppLogger.saveError(e, "RemoteImage.load")
        null
    }
}
