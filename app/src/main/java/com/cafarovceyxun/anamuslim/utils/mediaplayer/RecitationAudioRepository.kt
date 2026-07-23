package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.content.Context
import androidx.work.WorkManager
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ChapterTimingMetadata
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ResolvedAudioResult
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationModelBase
import com.cafarovceyxun.anamuslim.utils.workers.RecitationAudioDownloadWorker
import com.cafarovceyxun.anamuslim.utils.workers.RecitationBulkDownloadWorker
import kotlinx.coroutines.flow.Flow

/**
 * Android face of the shared [RecitationAudioResolver].
 *
 * Resolution itself (URIs, timing download/cache/parse) is platform-neutral and lives in
 * `commonMain`; what stays here is the one Android-only concern — cancelling the WorkManager
 * download jobs that write into the same reciter directories.
 */
class RecitationAudioRepository(context: Context) {
    companion object {
        fun prepareAudioUrl(urlTemplate: String, chapterNo: Int): String? =
            RecitationAudioResolver.prepareAudioUrl(urlTemplate, chapterNo)
    }

    private val workManager = WorkManager.getInstance(context)

    fun cancelAll() {
        workManager.cancelAllWorkByTag(RecitationAudioDownloadWorker.TAG)
        workManager.cancelAllWorkByTag(RecitationBulkDownloadWorker.TAG)
    }

    fun resolveAudioUris(
        chapterNo: Int,
        settings: PlayerSettings
    ): Flow<ResolvedAudioResult> = RecitationAudioResolver.resolveAudioUris(chapterNo, settings)

    suspend fun resolveChapterTimingMetadata(
        model: RecitationModelBase,
        chapterNo: Int,
    ): ChapterTimingMetadata? = RecitationAudioResolver.resolveChapterTimingMetadata(model, chapterNo)
}
