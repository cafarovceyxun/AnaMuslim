package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.workers.RecitationAudioDownloadWorker
import com.cafarovceyxun.anamuslim.utils.workers.RecitationBulkDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/** WorkManager emits often during batching; debounce so the view model's disk scans stay cheap. */
private const val WORK_INFO_DEBOUNCE_MS = 300L

private val WORK_ACTIVE_STATES = setOf(
    WorkInfo.State.RUNNING,
    WorkInfo.State.ENQUEUED,
    WorkInfo.State.BLOCKED,
)

/**
 * WorkManager-backed implementation of the shared [RecitationDownloadSource].
 *
 * The worker-tag parsing below used to live in `RecitationDownloadViewModel` (as `ParsedWorkState`);
 * it moved here because it only makes sense against the Android worker tag format. Publishing byte
 * progress to [RecitationDownloadProgressBus] happens as a side effect of parsing, exactly as
 * before. Same shape as [AndroidWbwAudioDownloadSource].
 */
@OptIn(FlowPreview::class)
class AndroidRecitationDownloadSource(private val context: Context) : RecitationDownloadSource {

    private val workManager = WorkManager.getInstance(context)
    private val modelManager = RecitationModelManager

    override val activeDownloads: Flow<RecitationActiveDownloads> =
        combine(
            workManager.getWorkInfosByTagLiveData(RecitationAudioDownloadWorker.TAG).asFlow(),
            workManager.getWorkInfosByTagLiveData(RecitationBulkDownloadWorker.TAG).asFlow(),
            ::mergeWorkInfos,
        )
            .debounce(WORK_INFO_DEBOUNCE_MS)
            .map(::parse)

    override suspend fun currentActiveDownloads(): RecitationActiveDownloads =
        parse(mergedWorkInfos())

    // Explicitly on IO: the view model now calls these from `viewModelScope` (main), whereas the
    // pre-seam code launched every block with `Dispatchers.IO` itself. Scanning 114 chapters and
    // deleting files must not run on the main thread.
    override suspend fun downloadedChapters(reciterId: String): Set<Int> =
        withContext(Dispatchers.IO) {
            buildSet {
                for (chapterNo in QuranMeta.chapterRange) {
                    val f = modelManager.getRecitationAudioFile(reciterId, chapterNo)
                    if (f.exists() && f.length() > 0L) add(chapterNo)
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
    ) = withContext(Dispatchers.IO) {
        val url = RecitationAudioRepository.prepareAudioUrl(urlTemplate, chapterNo)
            ?: return@withContext
        val outputPath = modelManager.getRecitationAudioFile(reciterId, chapterNo).absolutePath

        workManager.enqueueUniqueWork(
            RecitationAudioDownloadWorker.uniqueWorkName(reciterId, chapterNo),
            ExistingWorkPolicy.KEEP,
            RecitationAudioDownloadWorker.oneTimeRequest(
                url = url,
                outputPath = outputPath,
                title = title,
                subtitle = subtitle,
                reciterId = reciterId,
                audioKind = kind,
                chapterNo = chapterNo,
            ),
        )
        Unit
    }

    override suspend fun cancelChapter(reciterId: String, chapterNo: Int) {
        workManager.cancelUniqueWork(
            RecitationAudioDownloadWorker.uniqueWorkName(reciterId, chapterNo),
        )
    }

    override suspend fun deleteChapter(reciterId: String, chapterNo: Int) =
        withContext(Dispatchers.IO) {
            val f = modelManager.getRecitationAudioFile(reciterId, chapterNo)
            if (f.exists()) f.delete()
            Unit
        }

    override suspend fun startBulk(
        reciterId: String,
        kind: RecitationAudioKind,
        urlTemplate: String,
        displayTitle: String,
    ) {
        workManager.enqueueUniqueWork(
            RecitationBulkDownloadWorker.uniqueWorkName(reciterId, kind),
            ExistingWorkPolicy.KEEP,
            RecitationBulkDownloadWorker.oneTimeRequest(
                reciterId = reciterId,
                kind = kind,
                urlTemplate = urlTemplate,
                displayTitle = displayTitle,
            ),
        )
    }

    override suspend fun cancelBulk(reciterId: String, kind: RecitationAudioKind) {
        workManager.cancelUniqueWork(RecitationBulkDownloadWorker.uniqueWorkName(reciterId, kind))
        workManager.cancelAllWorkByTag(RecitationAudioDownloadWorker.reciterTag(reciterId, kind))
        workManager.cancelAllWorkByTag(RecitationBulkDownloadWorker.reciterTag(reciterId, kind))
    }

    private suspend fun mergedWorkInfos(): List<WorkInfo> = mergeWorkInfos(
        workManager.getWorkInfosByTag(RecitationAudioDownloadWorker.TAG).await(),
        workManager.getWorkInfosByTag(RecitationBulkDownloadWorker.TAG).await(),
    )

    /** Tags → domain state; also republishes byte progress to the shared bus. */
    private fun parse(infos: List<WorkInfo>): RecitationActiveDownloads {
        val activeChapters = mutableMapOf<String, MutableSet<Int>>()
        val activeBulks = mutableSetOf<String>()
        val inProgressCounts = mutableMapOf<String, Int>()

        for (info in infos) {
            if (info.state !in WORK_ACTIVE_STATES) continue

            var reciterId: String? = null
            var chapterNo: Int? = null
            var isBulk = false
            var isAudio = false

            for (tag in info.tags) {
                if (tag == RecitationAudioDownloadWorker.TAG) {
                    isAudio = true
                }

                RecitationAudioDownloadWorker.parseChapterWorkTag(tag)?.let {
                    reciterId = it.first
                    chapterNo = it.second
                }

                RecitationBulkDownloadWorker.parseBulkReciterTag(tag)?.let {
                    reciterId = it.second
                    isBulk = true
                }
            }

            if (reciterId != null) {
                inProgressCounts[reciterId] = (inProgressCounts[reciterId] ?: 0) + 1

                if (isBulk) {
                    activeBulks.add(reciterId)
                } else if (chapterNo != null) {
                    activeChapters.getOrPut(reciterId) { mutableSetOf() }.add(chapterNo)
                }
            }

            if (isAudio && reciterId != null && chapterNo != null) {
                val p = info.progress
                val bytes = p.getLong(RecitationAudioDownloadWorker.KEY_PROGRESS_BYTES, -1L)
                val total = p.getLong(RecitationAudioDownloadWorker.KEY_PROGRESS_TOTAL, -1L)

                if (bytes >= 0L) {
                    RecitationDownloadProgressBus.set(reciterId, chapterNo, bytes, total)
                }
            }
        }

        return RecitationActiveDownloads(activeChapters, activeBulks, inProgressCounts)
    }
}

private fun mergeWorkInfos(audio: List<WorkInfo>, bulk: List<WorkInfo>): List<WorkInfo> {
    val byId = LinkedHashMap<UUID, WorkInfo>()
    audio.forEach { byId[it.id] = it }
    bulk.forEach { byId[it.id] = it }
    return byId.values.toList()
}
