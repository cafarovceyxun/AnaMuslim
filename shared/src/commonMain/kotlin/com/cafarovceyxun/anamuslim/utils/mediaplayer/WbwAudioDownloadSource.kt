package com.cafarovceyxun.anamuslim.utils.mediaplayer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Which word-by-word audio downloads are currently in flight, keyed by audio dataset id. */
data class WbwActiveDownloads(
    val activeChaptersByAudioId: Map<String, Set<Int>> = emptyMap(),
    val activeBulkAudioIds: Set<String> = emptySet(),
) {
    fun activeChapters(audioId: String): Set<Int> =
        activeChaptersByAudioId[audioId] ?: emptySet()

    fun isBulkActive(audioId: String): Boolean = audioId in activeBulkAudioIds
}

/**
 * The word-by-word audio download subsystem, as the view model needs it.
 *
 * Deliberately domain-shaped rather than a generic background-job abstraction: the Android side
 * runs WorkManager and the *only* thing it does with `WorkInfo` is parse worker tags into the state
 * above and publish progress to [WbwAudioDownloadProgressBus]. That parsing is bound to the Android
 * worker tag format, so it belongs behind this interface, not in commonMain. iOS will implement the
 * same surface with `URLSession` background tasks.
 *
 * Same shape as the existing [WbwResourceSource] / [RecitationModelSource] download seams.
 *
 * Policy stays with the caller: the view model decides *whether* a download may start (e.g. refusing
 * a single chapter while a bulk run is active) and surfaces the message; this interface only
 * performs.
 */
interface WbwAudioDownloadSource {

    /** Id of the audio dataset in use (Android: `WbwAudioRepository.AUDIO_ID`). */
    val audioId: String

    /** Debounced stream of in-flight work; emits on every enqueue/finish/cancel. */
    val activeDownloads: Flow<WbwActiveDownloads>

    /** One-shot read, for the paths that act before the stream has emitted. */
    suspend fun currentActiveDownloads(): WbwActiveDownloads

    /** Chapters whose audio is already present on disk. */
    suspend fun downloadedChapters(): Set<Int>

    suspend fun startChapter(audioId: String, chapterNo: Int)
    suspend fun cancelChapter(chapterNo: Int)
    suspend fun deleteChapter(chapterNo: Int)
    suspend fun startBulk(audioId: String)
    suspend fun cancelBulk(audioId: String)
}

/**
 * Registered at startup (Android `QuranApp.onCreate()`), mirroring [WbwResourceProvider]. Unset —
 * currently iOS — behaves as an empty, inert subsystem instead of crashing.
 */
object WbwAudioDownloadProvider {
    private var provider: (() -> WbwAudioDownloadSource)? = null

    fun setProvider(value: () -> WbwAudioDownloadSource) {
        provider = value
    }

    val source: WbwAudioDownloadSource
        get() = provider?.invoke() ?: NoWbwAudioDownloadSource
}

private object NoWbwAudioDownloadSource : WbwAudioDownloadSource {
    override val audioId: String = ""
    override val activeDownloads: Flow<WbwActiveDownloads> = flowOf(WbwActiveDownloads())
    override suspend fun currentActiveDownloads() = WbwActiveDownloads()
    override suspend fun downloadedChapters(): Set<Int> = emptySet()
    override suspend fun startChapter(audioId: String, chapterNo: Int) = Unit
    override suspend fun cancelChapter(chapterNo: Int) = Unit
    override suspend fun deleteChapter(chapterNo: Int) = Unit
    override suspend fun startBulk(audioId: String) = Unit
    override suspend fun cancelBulk(audioId: String) = Unit
}
