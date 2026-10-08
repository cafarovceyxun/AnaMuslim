package com.cafarovceyxun.anamuslim.compose.components.common

import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.AVFoundation.AVAssetResourceLoader
import platform.AVFoundation.AVAssetResourceLoaderDelegateProtocol
import platform.AVFoundation.AVAssetResourceLoadingRequest
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.resourceLoader
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMakeRange
import platform.Foundation.NSMutableData
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionResponseCancel
import platform.Foundation.NSURLSessionResponseDisposition
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSUserDomainMask
import platform.Foundation.appendData
import platform.Foundation.dataTaskWithURL
import platform.Foundation.subdataWithRange
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToURL
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t

/**
 * Hekayə videosu üçün pleyer: diskdə varsa oradan, yoxsa şəbəkədən **və eyni anda keşə**.
 *
 * `AVPlayer`-in öz disk keşi yoxdur — eyni hekayəyə hər baxış videonu yenidən tam endirirdi
 * (baxanın mobil trafiki də, server trafiki də). Paralel ikinci endirmə ilə keşləmək trafiki iki
 * dəfə artırardı, ona görə baytları pleyerə **özümüz** veririk ([CachingVideoLoader]): video bir
 * dəfə endirilir, gələn hissə dərhal oynanır, tam bitəndə isə fayl keşə yazılır.
 */
@OptIn(ExperimentalForeignApi::class)
internal class StoryVideoSource private constructor(
    val player: AVPlayer?,
    private val loader: CachingVideoLoader?,
) {
    /** Hekayədən çıxanda yarımçıq endirmə dayanır — baxılmayan hissə üçün trafik getməsin. */
    fun release() {
        loader?.cancel()
    }

    companion object {
        fun create(url: String): StoryVideoSource {
            StoryVideoDiskCache.cachedFile(url)?.let { file ->
                return StoryVideoSource(AVPlayer(uRL = file), loader = null)
            }

            val remote = NSURL.URLWithString(url) ?: return StoryVideoSource(null, null)
            // Resource loader yalnız tanımadığı sxem üçün çağırılır — `https` olsa pleyer özü endirər.
            val proxied = NSURLComponents(uRL = remote, resolvingAgainstBaseURL = false)
                .apply { scheme = PROXY_SCHEME }
                .URL
                ?: return StoryVideoSource(AVPlayer(uRL = remote), loader = null)

            val loader = CachingVideoLoader(remote, url)
            val asset = AVURLAsset(uRL = proxied, options = null)
            // ⚠️ Delegate zəif referansdır — loader-i [StoryVideoSource] saxlayır.
            asset.resourceLoader.setDelegate(loader, queue = loader.queue)
            return StoryVideoSource(AVPlayer(playerItem = AVPlayerItem(asset = asset)), loader)
        }
    }
}

/**
 * Bütün faylı **ardıcıl** endirir və pleyerin bayt sorğularını gələn hissədən cavablayır.
 * Hər iki delegate (resource loader və `NSURLSession`) eyni ardıcıl növbədədir, ona görə vəziyyət
 * kilidsiz dəyişir.
 *
 * Yeni videolarda `moov` faylın əvvəlindədir (hər iki platformanın kodlayıcısı «fast start» yazır),
 * yəni oynatma dərhal başlayır. Köhnə Android videolarında `moov` sondadır — onlar fayl tam
 * gələndən sonra başlayır (bir dəfə; sonrakı baxışlar keşdəndir).
 */
@OptIn(ExperimentalForeignApi::class)
internal class CachingVideoLoader(
    private val remote: NSURL,
    private val cacheKey: String,
) : NSObject(), AVAssetResourceLoaderDelegateProtocol, NSURLSessionDataDelegateProtocol {

    val queue: dispatch_queue_t = dispatch_queue_create("com.cafarovceyxun.anamuslim.story-video", null)

    private val delegateQueue = NSOperationQueue().apply {
        maxConcurrentOperationCount = 1
        underlyingQueue = queue
    }

    private var session: NSURLSession? = null
    private val data = NSMutableData()
    private var contentLength = -1L
    private var contentType = DEFAULT_CONTENT_TYPE
    private var completed = false
    private var cancelled = false
    private val pending = mutableListOf<AVAssetResourceLoadingRequest>()

    fun cancel() {
        dispatch_async(queue) {
            cancelled = true
            pending.clear()
            // Sessiya delegate-i güclü saxlayır — ləğv olunmasa loader heç vaxt buraxılmır.
            session?.invalidateAndCancel()
            session = null
        }
    }

    @ObjCSignatureOverride
    override fun resourceLoader(
        resourceLoader: AVAssetResourceLoader,
        shouldWaitForLoadingOfRequestedResource: AVAssetResourceLoadingRequest,
    ): Boolean {
        if (cancelled) return false
        if (session == null && !completed) startDownload()
        pending += shouldWaitForLoadingOfRequestedResource
        respond()
        return true
    }

    @ObjCSignatureOverride
    override fun resourceLoader(
        resourceLoader: AVAssetResourceLoader,
        didCancelLoadingRequest: AVAssetResourceLoadingRequest,
    ) {
        pending.remove(didCancelLoadingRequest)
    }

    private fun startDownload() {
        val newSession = NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
            delegate = this,
            delegateQueue = delegateQueue,
        )
        session = newSession
        newSession.dataTaskWithURL(remote).resume()
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (NSURLSessionResponseDisposition) -> Unit,
    ) {
        val status = (didReceiveResponse as? NSHTTPURLResponse)?.statusCode ?: 200L
        if (status !in 200L..299L) {
            AppLogger.d(LOADER_TAG, "HTTP $status")
            completionHandler(NSURLSessionResponseCancel)
            return
        }
        contentLength = didReceiveResponse.expectedContentLength
        didReceiveResponse.MIMEType
            ?.let { UTType.typeWithMIMEType(it)?.identifier }
            ?.let { contentType = it }
        completionHandler(NSURLSessionResponseAllow)
        respond()
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData,
    ) {
        data.appendData(didReceiveData)
        respond()
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        session.finishTasksAndInvalidate()
        this.session = null
        if (cancelled) return

        val received = data.length.toLong()
        if (didCompleteWithError == null && (contentLength < 0 || received == contentLength)) {
            completed = true
            contentLength = received
            StoryVideoDiskCache.store(data, cacheKey)
            respond()
        } else {
            AppLogger.d(LOADER_TAG, "Download failed: ${didCompleteWithError?.localizedDescription}")
            val error = didCompleteWithError
            pending.forEach { request ->
                if (error != null) request.finishLoadingWithError(error) else request.finishLoading()
            }
            pending.clear()
            // Pleyer yenidən istəsə endirmə sıfırdan başlasın, yarımçıq baytların üstünə yox.
            data.setLength(0u)
            contentLength = -1L
        }
    }

    /** Gözləyən hər sorğuya indiyə qədər gələn baytlardan cavab verir; tam ödənəni bağlayır. */
    private fun respond() {
        // Uzunluq bilinmədən nə məlumat sorğusu, nə də «sona qədər» sorğusu cavablana bilər.
        if (contentLength < 0) return
        val available = data.length.toLong()

        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            val request = iterator.next()
            request.contentInformationRequest?.apply {
                contentType = this@CachingVideoLoader.contentType
                contentLength = this@CachingVideoLoader.contentLength
                byteRangeAccessSupported = true
            }

            val dataRequest = request.dataRequest
            if (dataRequest == null) {
                request.finishLoading()
                iterator.remove()
                continue
            }

            val start = dataRequest.currentOffset
            val end = if (dataRequest.requestsAllDataToEndOfResource) {
                contentLength
            } else {
                dataRequest.requestedOffset + dataRequest.requestedLength
            }.coerceAtMost(contentLength)

            if (start < available && start < end) {
                val count = minOf(available, end) - start
                dataRequest.respondWithData(
                    data.subdataWithRange(NSMakeRange(start.toULong(), count.toULong())),
                )
            }
            if (dataRequest.currentOffset >= end) {
                request.finishLoading()
                iterator.remove()
            }
        }
    }
}

/**
 * `Library/Caches/story_video_cache` — ən çox [MAX_CACHE_BYTES], köhnəsi (son baxışa görə) ilk
 * silinir. Caches qovluğunu sistem yer lazım olanda özü də təmizləyir.
 */
@OptIn(ExperimentalForeignApi::class)
internal object StoryVideoDiskCache {
    private val fileManager get() = NSFileManager.defaultManager

    private val directory: NSURL? by lazy {
        val caches = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            .firstOrNull() as? NSURL
        caches?.URLByAppendingPathComponent("story_video_cache")?.also {
            fileManager.createDirectoryAtURL(it, withIntermediateDirectories = true, attributes = null, error = null)
        }
    }

    fun cachedFile(url: String): NSURL? {
        val file = fileFor(url) ?: return null
        val path = file.path ?: return null
        if (!fileManager.fileExistsAtPath(path)) return null
        // Son baxış tarixi — LRU təmizləməsi buna görə sıralayır.
        fileManager.setAttributes(mapOf<Any?, Any?>(NSFileModificationDate to NSDate()), ofItemAtPath = path, error = null)
        return file
    }

    fun store(data: NSData, url: String) {
        val file = fileFor(url) ?: return
        if (!data.writeToURL(file, atomically = true)) {
            AppLogger.d(TAG, "Cache write failed")
            return
        }
        trim()
    }

    private fun fileFor(url: String): NSURL? {
        // Storage obyekt adı onsuz da unikaldır; hash eyni adlı iki bucket faylını ayırır.
        val name = url.substringAfterLast('/').substringBefore('?')
            .filter { it.isLetterOrDigit() || it in "-_." }
            .takeLast(MAX_NAME_LENGTH)
        return directory?.URLByAppendingPathComponent("${url.hashCode().toUInt().toString(16)}_$name")
    }

    private fun trim() {
        val dir = directory?.path ?: return
        val entries = fileManager.contentsOfDirectoryAtPath(dir, error = null)
            ?.mapNotNull { name ->
                val path = "$dir/$name"
                val attributes = fileManager.attributesOfItemAtPath(path, error = null) ?: return@mapNotNull null
                val size = when (val value = attributes[NSFileSize]) {
                    is Number -> value.toLong()
                    is NSNumber -> value.longLongValue
                    else -> 0L
                }
                val modified = (attributes[NSFileModificationDate] as? NSDate)?.timeIntervalSince1970 ?: 0.0
                Triple(path, size, modified)
            }
            ?: return

        var total = entries.sumOf { it.second }
        for ((path, size, _) in entries.sortedBy { it.third }) {
            if (total <= MAX_CACHE_BYTES) break
            if (fileManager.removeItemAtPath(path, error = null)) total -= size
        }
    }

    /** ~10 hekayə videosu (Android-dəki `StoryVideoCache` ilə eyni). */
    private const val MAX_CACHE_BYTES = 200L * 1024 * 1024
    private const val MAX_NAME_LENGTH = 80
    private const val TAG = "StoryVideoCache"
}

private const val PROXY_SCHEME = "anamuslim-story"

// Obj-C alt sinfinin companion-unda sahə ola bilmir — loader sabitləri burada.
private const val DEFAULT_CONTENT_TYPE = "public.mpeg-4"
private const val LOADER_TAG = "StoryVideoLoader"
