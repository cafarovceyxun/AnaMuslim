package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Which recitation downloads are currently in flight, keyed by reciter id. */
data class RecitationActiveDownloads(
    val activeChaptersByReciter: Map<String, Set<Int>> = emptyMap(),
    val activeBulkReciters: Set<String> = emptySet(),
    /** Number of in-flight work items per reciter — chapter downloads and bulk runs alike. */
    val inProgressCountsByReciter: Map<String, Int> = emptyMap(),
) {
    fun activeChapters(reciterId: String): Set<Int> =
        activeChaptersByReciter[reciterId] ?: emptySet()

    fun isBulkActive(reciterId: String): Boolean = reciterId in activeBulkReciters

    fun inProgressCount(reciterId: String): Int = inProgressCountsByReciter[reciterId] ?: 0

    /** True when some *other* reciter is downloading — the app allows only one at a time. */
    fun hasOtherReciterActive(reciterId: String): Boolean =
        inProgressCountsByReciter.any { it.key != reciterId && it.value > 0 }
}

/**
 * The recitation download subsystem, as the view model needs it — the sibling of
 * [WbwAudioDownloadSource], built on the same "mechanism on the platform, policy in the view model"
 * split.
 *
 * The Android side runs WorkManager, and everything it does with `WorkInfo` is parse worker tags
 * into the state above and publish byte progress to [RecitationDownloadProgressBus]. That parsing is
 * bound to the Android worker tag format, so it belongs behind this interface. URL preparation and
 * on-disk layout are platform concerns too, hence the `urlTemplate`-in / no-paths-out shape.
 *
 * The view model keeps the policy: which reciter may start, what the user is told, and the
 * downloaded-chapter cache that keeps disk scans off the hot path.
 */
interface RecitationDownloadSource {

    /** Debounced stream of in-flight work; emits on every enqueue/finish/cancel. */
    val activeDownloads: Flow<RecitationActiveDownloads>

    /** One-shot read, for the paths that act before the stream has emitted. */
    suspend fun currentActiveDownloads(): RecitationActiveDownloads

    /** Chapters of [reciterId] already present on disk. Uncached — the caller decides when to scan. */
    suspend fun downloadedChapters(reciterId: String): Set<Int>

    suspend fun startChapter(
        reciterId: String,
        kind: RecitationAudioKind,
        urlTemplate: String,
        chapterNo: Int,
        title: String,
        subtitle: String,
    )

    suspend fun cancelChapter(reciterId: String, chapterNo: Int)
    suspend fun deleteChapter(reciterId: String, chapterNo: Int)

    suspend fun startBulk(
        reciterId: String,
        kind: RecitationAudioKind,
        urlTemplate: String,
        displayTitle: String,
    )

    suspend fun cancelBulk(reciterId: String, kind: RecitationAudioKind)
}

/**
 * Registered at startup (Android `QuranApp.onCreate()`), mirroring [WbwAudioDownloadProvider]. Unset
 * — currently iOS — behaves as an empty, inert subsystem instead of crashing.
 */
object RecitationDownloadProvider {
    private var provider: (() -> RecitationDownloadSource)? = null

    fun setProvider(value: () -> RecitationDownloadSource) {
        provider = value
    }

    val source: RecitationDownloadSource
        get() = provider?.invoke() ?: NoRecitationDownloadSource
}

private object NoRecitationDownloadSource : RecitationDownloadSource {
    override val activeDownloads: Flow<RecitationActiveDownloads> =
        flowOf(RecitationActiveDownloads())

    override suspend fun currentActiveDownloads() = RecitationActiveDownloads()
    override suspend fun downloadedChapters(reciterId: String): Set<Int> = emptySet()

    override suspend fun startChapter(
        reciterId: String,
        kind: RecitationAudioKind,
        urlTemplate: String,
        chapterNo: Int,
        title: String,
        subtitle: String,
    ) = Unit

    override suspend fun cancelChapter(reciterId: String, chapterNo: Int) = Unit
    override suspend fun deleteChapter(reciterId: String, chapterNo: Int) = Unit

    override suspend fun startBulk(
        reciterId: String,
        kind: RecitationAudioKind,
        urlTemplate: String,
        displayTitle: String,
    ) = Unit

    override suspend fun cancelBulk(reciterId: String, kind: RecitationAudioKind) = Unit
}
