package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.cafarovceyxun.anamuslim.api.streamInventory
import io.ktor.utils.io.jvm.javaio.toInputStream
import com.cafarovceyxun.anamuslim.db.DatabaseProvider
import com.cafarovceyxun.anamuslim.db.entities.wbw.WbwAudioTimingEntity
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.extensions.isGzip
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioRepository.AUDIO_ID
import com.cafarovceyxun.anamuslim.utils.reader.recitation.RecitationUtils
import com.cafarovceyxun.anamuslim.utils.receivers.NetworkStateReceiver
import com.cafarovceyxun.anamuslim.utils.univ.FileUtils
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

object WbwAudioRepository {
    // File names, URLs and the dataset id are shared with iOS so both platforms lay audio out
    // identically; only playback and the timing import below stay Android-specific here.
    const val AUDIO_ID = WbwAudioFiles.AUDIO_ID

    private const val TIMING_URL =
        "ghraw://AlfaazPlus/QuranAppInventory/master/wbw_timings/wbw_a1.json.gz"


    private val timingLoadMutex = Mutex()

    fun migrateLegacyData(appContext: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val dir = File(appContext.cacheDir, "wbw_audio")

            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    fun getChapterAudioFile(context: Context, chapterNo: Int): File =
        File(WbwAudioFiles.chapterAudioPath(chapterNo).toString())

    private fun hasValidChapterAudioFile(file: File): Boolean {
        return file.exists() && file.length() > 0L
    }

    fun isChapterAudioDownloaded(context: Context, chapterNo: Int): Boolean {
        val f = getChapterAudioFile(context.applicationContext, chapterNo)
        return hasValidChapterAudioFile(f)
    }

    fun prepareOneOffWordAudioUrl(
        chapterNo: Int,
        verseNo: Int,
        appWordIndex: Int,
    ): String? = WbwAudioFiles.prepareOneOffWordAudioUrl(chapterNo, verseNo, appWordIndex)

    suspend fun deleteChapterAudio(context: Context, chapterNo: Int) =
        withContext(Dispatchers.IO) {
            val f = getChapterAudioFile(context.applicationContext, chapterNo)
            if (f.exists()) {
                f.delete()
            }
        }

    fun prepareChapterAudioUrl(chapterNo: Int): String? =
        WbwAudioFiles.prepareChapterAudioUrl(chapterNo)

    suspend fun getTimingCount(context: Context): Int =
        withContext(Dispatchers.IO) {
            DatabaseProvider.getExternalQuranDatabase(context.applicationContext)
                .wbwDao()
                .getTimingCount(AUDIO_ID)
        }

    suspend fun resolveChapterAudioUri(context: Context, chapterNo: Int): Uri? =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext

            val local = getChapterAudioFile(app, chapterNo)

            if (hasValidChapterAudioFile(local)) {
                return@withContext local.toUri()
            }

            if (!NetworkStateReceiver.isNetworkConnected(app)) {
                return@withContext null
            }

            val url = prepareChapterAudioUrl(chapterNo) ?: return@withContext null

            url.toUri()
        }

    suspend fun resolveWordPlaybackSource(
        context: Context,
        chapterNo: Int,
        verseNo: Int,
        appWordIndex: Int,
    ): WbwWordPlaybackSource? = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val local = getChapterAudioFile(app, chapterNo)

        if (hasValidChapterAudioFile(local)) {
            return@withContext WbwWordPlaybackSource.Chapter(local.toUri())
        }

        if (!NetworkStateReceiver.isNetworkConnected(app)) {
            return@withContext null
        }

        val oneOffUrl = prepareOneOffWordAudioUrl(chapterNo, verseNo, appWordIndex)
            ?: return@withContext null

        WbwWordPlaybackSource.OneOff(oneOffUrl.toUri())
    }

    suspend fun getWordTiming(
        context: Context,
        chapterNo: Int,
        verseNo: Int,
        wordIndex: Int,
    ): WbwAudioTimingEntity? {
        if (!ensureTimingsInDb(context.applicationContext)) {
            return null
        }

        val ayahId = chapterNo * 1000 + verseNo

        return DatabaseProvider.getExternalQuranDatabase(context.applicationContext)
            .wbwDao()
            .getWordTiming(AUDIO_ID, ayahId, wordIndex)
    }

    suspend fun ensureTimingsAvailable(context: Context) {
        ensureTimingsInDb(context.applicationContext)
    }

    /**
     * Intended when the published WBW audio timings version increases.
     */
    suspend fun refreshTimingsFromRemote(context: Context) {
        val app = context.applicationContext
        timingLoadMutex.withLock {
            if (!NetworkStateReceiver.isNetworkConnected(app)) return@withLock
            downloadAndImportTimings(app)
        }
    }

    /**
     * @return true if timing rows exist for [AUDIO_ID] after any required download/import attempt.
     */
    private suspend fun ensureTimingsInDb(appContext: Context): Boolean {
        val db = DatabaseProvider.getExternalQuranDatabase(appContext)
        val dao = db.wbwDao()

        if (dao.getTimingCount(AUDIO_ID) > 0) return true

        return timingLoadMutex.withLock {
            if (db.wbwDao().getTimingCount(AUDIO_ID) > 0) return@withLock true
            if (!NetworkStateReceiver.isNetworkConnected(appContext)) return@withLock false

            downloadAndImportTimings(appContext)

            db.wbwDao().getTimingCount(AUDIO_ID) > 0
        }
    }

    private suspend fun downloadAndImportTimings(appContext: Context) {
        val tmp = runCatching { downloadTimingToTemp(appContext) }
            .onFailure { Log.saveError(it, "WbwAudioRepository.downloadTimingToTemp") }
            .getOrNull() ?: return

        try {
            importTimingFromFile(appContext, tmp)
        } catch (e: Exception) {
            Log.saveError(e, "WbwAudioRepository.importTimingFromFile")
        } finally {
            tmp.delete()
        }
    }

    private suspend fun downloadTimingToTemp(appContext: Context): File =
        withContext(Dispatchers.IO) {
            val dest = File(appContext.cacheDir, "wbw_timing_${System.currentTimeMillis()}.tmp")

            streamInventory(TIMING_URL) { scope ->
                if (!scope.isSuccessful) {
                    throw IOException("WBW timing download failed: HTTP ${scope.statusCode}")
                }

                scope.channel.toInputStream().use { input ->
                    dest.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            dest
        }

    private suspend fun importTimingFromFile(appContext: Context, file: File) =
        withContext(Dispatchers.IO) {
            val INSERT_CHUNK = 750

            val db = DatabaseProvider.getExternalQuranDatabase(appContext)
            val dao = db.wbwDao()

            val rawStream = if (file.isGzip()) {
                GZIPInputStream(file.inputStream().buffered())
            } else {
                file.inputStream().buffered()
            }

            rawStream.use { raw ->
                JsonReader(InputStreamReader(raw, StandardCharsets.UTF_8)).use { reader ->
                    db.withTransaction {
                        dao.deleteTimingByAudioId(AUDIO_ID)

                        val buffer = ArrayList<WbwAudioTimingEntity>(INSERT_CHUNK)

                        reader.beginObject()

                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            reader.beginArray()

                            val startMs = reader.nextLong()
                            val endMs = reader.nextLong()

                            reader.endArray()

                            val triple = parseTimingKey(key) ?: continue

                            val (chapterNo, verseNo, wordIdx) = triple

                            if (endMs <= startMs || startMs < 0L) continue

                            val ayahId = chapterNo * 1000 + verseNo

                            buffer.add(
                                WbwAudioTimingEntity(
                                    audioId = AUDIO_ID,
                                    ayahId = ayahId,
                                    wordIndex = wordIdx,
                                    startMillis = startMs,
                                    endMillis = endMs,
                                ),
                            )

                            if (buffer.size >= INSERT_CHUNK) {
                                dao.upsertTimings(ArrayList(buffer))

                                buffer.clear()
                            }
                        }

                        reader.endObject()

                        if (buffer.isNotEmpty()) {
                            dao.upsertTimings(buffer)
                        }
                    }
                }
            }
        }

    /**
     * Parses timing map keys shaped like "1_1_0" -> (chapter, verse, wordIndex).
     */
    private fun parseTimingKey(key: String): Triple<Int, Int, Int>? {
        val parts = key.split('_')
        if (parts.size < 3) return null

        val chapter = parts[0].toIntOrNull() ?: return null
        val verse = parts[1].toIntOrNull() ?: return null
        val wordIndex = parts[2].toIntOrNull() ?: return null

        if (chapter <= 0 || verse <= 0) return null

        return Triple(chapter, verse, wordIndex)
    }
}

sealed interface WbwWordPlaybackSource {
    data class Chapter(val uri: Uri) : WbwWordPlaybackSource
    data class OneOff(val uri: Uri) : WbwWordPlaybackSource
}
