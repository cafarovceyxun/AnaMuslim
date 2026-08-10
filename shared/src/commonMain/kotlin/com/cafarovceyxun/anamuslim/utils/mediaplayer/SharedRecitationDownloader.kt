package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.DownloadNotifier
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.BackgroundFileTransfer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** How many chapter downloads a bulk run keeps in flight — the bulk worker's limit on Android. */
private const val MAX_PARALLEL_DOWNLOADS = 4

/**
 * Multiplatform [RecitationDownloadSource]: runs chapter and bulk downloads on a coroutine scope.
 *
 * Android stays on WorkManager (`AndroidRecitationDownloadSource`), where downloads must survive
 * backgrounding and drive a progress notification. Both platforms share the transfer seam
 * ([BackgroundFileTransfer] — the Ktor streamer on Android, a background `NSURLSession` on iOS),
 * the URL preparation and the on-disk layout, so a file downloaded on either platform is
 * byte-identical and lands in the same place.
 *
 * The in-flight state that Android derives by parsing worker tags is held directly here: jobs are
 * the source of truth, and the same [RecitationDownloadProgressBus] carries byte progress to the UI.
 */
class SharedRecitationDownloader(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : RecitationDownloadSource {

    private val state = MutableStateFlow(RecitationActiveDownloads())
    private val mutex = Mutex()

    /** Keyed by reciter+chapter for single downloads and by reciter for bulk runs. */
    private val chapterJobs = mutableMapOf<String, Job>()
    private val bulkJobs = mutableMapOf<String, Job>()

    override val activeDownloads: Flow<RecitationActiveDownloads> = state.asStateFlow()

    override suspend fun currentActiveDownloads(): RecitationActiveDownloads = state.value

    override suspend fun downloadedChapters(reciterId: String): Set<Int> =
        withContext(Dispatchers.IO) {
            buildSet {
                for (chapterNo in QuranMeta.chapterRange) {
                    val path = RecitationModelManager.getRecitationAudioPath(reciterId, chapterNo)
                    if ((AppFileSystem.size(path) ?: 0L) > 0L) add(chapterNo)
                }
            }
        }

    override suspend fun startChapter(
        reciterId: String,
        kind: RecitationAudioKind,
        urlTemplate: String,
        chapterNo: Int,
        title: String,
        subtitle: String,
    ) {
        val key = chapterKey(reciterId, chapterNo)
        // WorkManager's KEEP policy: an already running download for this chapter wins.
        mutex.withLock { if (chapterJobs[key]?.isActive == true) return }

        val url = RecitationAudioResolver.prepareAudioUrl(urlTemplate, chapterNo) ?: return
        val target = RecitationModelManager.getRecitationAudioPath(reciterId, chapterNo)
        if ((AppFileSystem.size(target) ?: 0L) > 0L) return

        val job = scope.launch {
            RecitationDownloadProgressBus.set(reciterId, chapterNo, 0L, -1L)
            try {
                BackgroundFileTransfer.download(
                    url = url,
                    target = target,
                    label = notificationLabel(title, subtitle),
                ) { consumed, total ->
                    RecitationDownloadProgressBus.set(reciterId, chapterNo, consumed, total)
                }
                DownloadNotifier.completed(notificationLabel(title, subtitle))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedRecitationDownloader:$reciterId:$chapterNo")
                DownloadNotifier.failed(notificationLabel(title, subtitle))
            } finally {
                // NonCancellable is what makes cancelling work at all — see [startBulk].
                withContext(NonCancellable) {
                    RecitationDownloadProgressBus.clear(reciterId, chapterNo)
                    mutex.withLock { chapterJobs.remove(key) }
                    publishState()
                }
            }
        }

        mutex.withLock { chapterJobs[key] = job }
        publishState()
    }

    override suspend fun cancelChapter(reciterId: String, chapterNo: Int) {
        mutex.withLock { chapterJobs[chapterKey(reciterId, chapterNo)] }?.cancel()
    }

    override suspend fun deleteChapter(reciterId: String, chapterNo: Int) {
        withContext(Dispatchers.IO) {
            AppFileSystem.delete(RecitationModelManager.getRecitationAudioPath(reciterId, chapterNo))
        }
    }

    override suspend fun startBulk(
        reciterId: String,
        kind: RecitationAudioKind,
        urlTemplate: String,
        displayTitle: String,
    ) {
        mutex.withLock { if (bulkJobs[reciterId]?.isActive == true) return }

        val job = scope.launch {
            try {
                downloadAllChapters(reciterId, urlTemplate)
                DownloadNotifier.completed(displayTitle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedRecitationDownloader:bulk:$reciterId")
                DownloadNotifier.failed(displayTitle)
            } finally {
                // `mutex.withLock` and `publishState` both suspend, and this `finally` runs on an
                // already-cancelled coroutine — without NonCancellable the very first suspension
                // point rethrows `CancellationException` and neither line ever runs. The transfers
                // did stop, but the job stayed in `bulkJobs` and no new state was published, so the
                // row kept its spinner and cancel button forever: pressing cancel looked like it
                // did nothing, and pressing it again re-ran the same no-op.
                withContext(NonCancellable) {
                    mutex.withLock { bulkJobs.remove(reciterId) }
                    publishState()
                }
            }
        }

        mutex.withLock { bulkJobs[reciterId] = job }
        publishState()
    }

    /**
     * Stops **everything** running for this reciter, not just a bulk run.
     *
     * The reciter row shows one cancel button, and it is offered whenever the reciter has any work
     * in flight — which includes the case where the user picked a couple of surahs by hand from the
     * chapter sheet and never started a bulk run at all. Cancelling only [bulkJobs] then found
     * nothing to cancel: those chapter downloads carried on, and the row kept its cancel button, so
     * pressing it did nothing however many times it was pressed. Bulk runs hid the bug, because
     * there the one job it did cancel was the right one.
     *
     * Android has always meant it this way — `AndroidRecitationDownloadSource.cancelBulk` cancels
     * the unique bulk work *and* every chapter worker tagged with the reciter.
     */
    override suspend fun cancelBulk(reciterId: String, kind: RecitationAudioKind) {
        val running = mutex.withLock {
            buildList {
                bulkJobs[reciterId]?.let(::add)
                for ((key, job) in chapterJobs) {
                    if (parseChapterKey(key)?.first == reciterId) add(job)
                }
            }
        }

        running.forEach { it.cancel() }
    }

    /**
     * Reciter + chapter, the same pair the download row shows. A bulk run notifies once with its
     * own title instead — it downloads chapters directly, not through [startChapter], so there is
     * no 114-banner storm.
     */
    private fun notificationLabel(title: String, subtitle: String): String =
        if (subtitle.isBlank()) title else "$title · $subtitle"

    /** Missing chapters only, [MAX_PARALLEL_DOWNLOADS] at a time — the bulk worker's behaviour. */
    private suspend fun downloadAllChapters(reciterId: String, urlTemplate: String) {
        val pending = withContext(Dispatchers.IO) {
            QuranMeta.chapterRange.mapNotNull { chapterNo ->
                val path = RecitationModelManager.getRecitationAudioPath(reciterId, chapterNo)
                if ((AppFileSystem.size(path) ?: 0L) > 0L) return@mapNotNull null
                val url = RecitationAudioResolver.prepareAudioUrl(urlTemplate, chapterNo)
                    ?: return@mapNotNull null
                Triple(chapterNo, url, path)
            }
        }

        val semaphore = Semaphore(MAX_PARALLEL_DOWNLOADS)

        coroutineScope {
            pending.map { (chapterNo, url, path) ->
                async {
                    semaphore.withPermit {
                        RecitationDownloadProgressBus.set(reciterId, chapterNo, 0L, -1L)
                        try {
                            // No label: a bulk run notifies once at the end, and an orphaned chapter
                            // must not produce a banner of its own.
                            BackgroundFileTransfer.download(url, path) { consumed, total ->
                                RecitationDownloadProgressBus.set(reciterId, chapterNo, consumed, total)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // One failed chapter must not abort the run — the user can retry it.
                            AppLogger.saveError(e, "SharedRecitationDownloader:bulk:$reciterId:$chapterNo")
                        } finally {
                            RecitationDownloadProgressBus.clear(reciterId, chapterNo)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    /** Recomputes the snapshot the view model consumes; jobs are the source of truth. */
    private suspend fun publishState() {
        val (chapters, bulks) = mutex.withLock {
            chapterJobs.filterValues { it.isActive }.keys.toList() to
                    bulkJobs.filterValues { it.isActive }.keys.toSet()
        }

        val activeByReciter = mutableMapOf<String, MutableSet<Int>>()
        for (key in chapters) {
            val (reciterId, chapterNo) = parseChapterKey(key) ?: continue
            activeByReciter.getOrPut(reciterId) { mutableSetOf() }.add(chapterNo)
        }

        val counts = mutableMapOf<String, Int>()
        for ((reciterId, set) in activeByReciter) counts[reciterId] = set.size
        for (reciterId in bulks) counts[reciterId] = (counts[reciterId] ?: 0) + 1

        state.value = RecitationActiveDownloads(
            activeChaptersByReciter = activeByReciter,
            activeBulkReciters = bulks,
            inProgressCountsByReciter = counts,
        )
    }

    private fun chapterKey(reciterId: String, chapterNo: Int) = "$reciterId:$chapterNo"

    private fun parseChapterKey(key: String): Pair<String, Int>? {
        val i = key.lastIndexOf(':')
        if (i <= 0) return null
        val chapterNo = key.substring(i + 1).toIntOrNull() ?: return null
        return key.substring(0, i) to chapterNo
    }
}
