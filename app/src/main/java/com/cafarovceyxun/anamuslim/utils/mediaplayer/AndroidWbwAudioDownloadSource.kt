package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.workers.WbwAudioBulkDownloadWorker
import com.cafarovceyxun.anamuslim.utils.workers.WbwAudioDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

private const val WORK_INFO_DEBOUNCE_MS = 300L

private val WORK_ACTIVE_STATES = setOf(
    WorkInfo.State.RUNNING,
    WorkInfo.State.ENQUEUED,
    WorkInfo.State.BLOCKED,
)

/**
 * WorkManager-backed implementation of the shared [WbwAudioDownloadSource].
 *
 * The worker-tag parsing below used to live in `WbwAudioDownloadViewModel`; it moved here because it
 * only makes sense against the Android worker tag format. Publishing byte progress to
 * [WbwAudioDownloadProgressBus] happens as a side effect of parsing, exactly as before.
 */
@OptIn(FlowPreview::class)
class AndroidWbwAudioDownloadSource(private val context: Context) : WbwAudioDownloadSource {

    private val workManager = WorkManager.getInstance(context)

    override val audioId: String get() = WbwAudioRepository.AUDIO_ID

    override val activeDownloads: Flow<WbwActiveDownloads> =
        combine(
            workManager.getWorkInfosByTagLiveData(WbwAudioDownloadWorker.TAG).asFlow(),
            workManager.getWorkInfosByTagLiveData(WbwAudioBulkDownloadWorker.TAG).asFlow(),
            ::mergeWorkInfos,
        )
            .debounce(WORK_INFO_DEBOUNCE_MS)
            .map(::parse)

    override suspend fun currentActiveDownloads(): WbwActiveDownloads = parse(mergedWorkInfos())

    // Explicitly on IO: the view model now calls these from `viewModelScope` (main), whereas the
    // pre-seam code launched every block with `Dispatchers.IO` itself. Scanning 114 chapters and
    // deleting files must not run on the main thread.
    override suspend fun downloadedChapters(): Set<Int> = withContext(Dispatchers.IO) {
        buildSet {
            for (chapterNo in QuranMeta.chapterRange) {
                if (WbwAudioRepository.isChapterAudioDownloaded(context, chapterNo)) add(chapterNo)
            }
        }
    }

    override suspend fun startChapter(audioId: String, chapterNo: Int) = withContext(Dispatchers.IO) {
        val url = WbwAudioRepository.prepareChapterAudioUrl(chapterNo) ?: return@withContext
        val outputPath = WbwAudioRepository.getChapterAudioFile(context, chapterNo).absolutePath

        workManager.enqueueUniqueWork(
            WbwAudioDownloadWorker.uniqueWorkName(chapterNo),
            ExistingWorkPolicy.KEEP,
            WbwAudioDownloadWorker.oneTimeRequest(
                url = url,
                outputPath = outputPath,
                chapterNo = chapterNo,
                audioId = audioId,
            ),
        )
        Unit
    }

    override suspend fun cancelChapter(chapterNo: Int) {
        workManager.cancelUniqueWork(WbwAudioDownloadWorker.uniqueWorkName(chapterNo))
    }

    override suspend fun deleteChapter(chapterNo: Int) = withContext(Dispatchers.IO) {
        WbwAudioRepository.deleteChapterAudio(context, chapterNo)
        Unit
    }

    override suspend fun startBulk(audioId: String) {
        workManager.enqueueUniqueWork(
            WbwAudioBulkDownloadWorker.uniqueWorkName(),
            ExistingWorkPolicy.KEEP,
            WbwAudioBulkDownloadWorker.oneTimeRequest(audioId = audioId),
        )
    }

    override suspend fun cancelBulk(audioId: String) {
        workManager.cancelUniqueWork(WbwAudioBulkDownloadWorker.uniqueWorkName())
        workManager.cancelAllWorkByTag(WbwAudioBulkDownloadWorker.bulkTag(audioId))
    }

    private suspend fun mergedWorkInfos(): List<WorkInfo> = mergeWorkInfos(
        workManager.getWorkInfosByTag(WbwAudioDownloadWorker.TAG).await(),
        workManager.getWorkInfosByTag(WbwAudioBulkDownloadWorker.TAG).await(),
    )

    /** Tags → domain state; also republishes byte progress to the shared bus. */
    private fun parse(infos: List<WorkInfo>): WbwActiveDownloads {
        val activeChapters = mutableMapOf<String, MutableSet<Int>>()
        val activeBulks = mutableSetOf<String>()

        for (info in infos) {
            if (info.state !in WORK_ACTIVE_STATES) continue

            var bulkBusId: String? = null
            for (tag in info.tags) {
                WbwAudioBulkDownloadWorker.parseBulkTag(tag)?.let { bulkBusId = it }
            }

            if (bulkBusId != null) {
                activeBulks.add(bulkBusId)
                continue
            }

            var busId: String? = null
            var chapterNo: Int? = null

            for (tag in info.tags) {
                WbwAudioDownloadWorker.parseChapterWorkTag(tag)?.let { (bid, ch) ->
                    busId = bid
                    chapterNo = ch
                }
            }

            if (busId == null || chapterNo == null) continue

            activeChapters.getOrPut(busId) { mutableSetOf() }.add(chapterNo)

            val bytes = info.progress.getLong(WbwAudioDownloadWorker.KEY_PROGRESS_BYTES, -1L)
            val total = info.progress.getLong(WbwAudioDownloadWorker.KEY_PROGRESS_TOTAL, -1L)

            if (bytes >= 0L) {
                WbwAudioDownloadProgressBus.set(busId, chapterNo, bytes, total)
            }
        }

        return WbwActiveDownloads(activeChapters, activeBulks)
    }
}

private fun mergeWorkInfos(audio: List<WorkInfo>, bulk: List<WorkInfo>): List<WorkInfo> {
    val byId = LinkedHashMap<UUID, WorkInfo>()
    audio.forEach { byId[it.id] = it }
    bulk.forEach { byId[it.id] = it }
    return byId.values.toList()
}
