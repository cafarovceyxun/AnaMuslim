package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.downloadStream
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

/**
 * Streams one audio file to disk, reporting byte progress.
 *
 * Was Android-only (`java.io` streams over the Ktor channel); the body is now okio + the shared
 * [AppFileSystem], so iOS runs the same downloader. It writes to a `.part` file and publishes it
 * with an atomic move, so a killed download never leaves a truncated file that later looks
 * "already downloaded".
 */
object RecitationAudioFileDownloader {
    private const val CHUNK_SIZE = 64L * 1024
    private const val PROGRESS_INTERVAL_MS = 200L

    /** Path-string overload for the Android workers, which work in `java.io.File` terms. */
    suspend fun downloadToFile(
        urlStr: String,
        outputPath: String,
        onProgress: suspend (consumed: Long, total: Long) -> Unit,
    ) = downloadToFile(urlStr, outputPath.toPath(), onProgress)

    suspend fun downloadToFile(
        urlStr: String,
        finalFile: Path,
        onProgress: suspend (consumed: Long, total: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val parent = finalFile.parent ?: throw IllegalStateException("Output path has no parent directory")
        val tempFile = parent / "${finalFile.name}.part"

        AppFileSystem.delete(tempFile)

        try {
            downloadStream(urlStr) { scope ->
                if (scope.statusCode == 404) throw IOException("Audio file not found")
                if (!scope.isSuccessful) throw IOException("Download failed: HTTP ${scope.statusCode}")

                val totalLength = scope.contentLength
                var totalConsumed = 0L
                var lastEmitTime = 0L

                AppFileSystem.write(tempFile) { sink ->
                    while (true) {
                        currentCoroutineContext().ensureActive()

                        val bytes = scope.channel.readRemaining(CHUNK_SIZE).readByteArray()
                        if (bytes.isEmpty()) break

                        sink.write(bytes)
                        totalConsumed += bytes.size

                        val now = currentEpochMillis()
                        if (now - lastEmitTime >= PROGRESS_INTERVAL_MS) {
                            lastEmitTime = now
                            emitProgress(totalConsumed, totalLength, onProgress)
                        }
                    }
                }

                // The final tick always fires, so a UI bound to progress lands on 100%.
                emitProgress(totalConsumed, totalLength, onProgress)
            }

            if (!AppFileSystem.atomicMove(tempFile, finalFile)) {
                throw IOException("Could not finalize download file")
            }
        } catch (e: Throwable) {
            AppFileSystem.delete(tempFile)
            throw e
        }
    }

    private suspend inline fun emitProgress(
        consumed: Long,
        total: Long,
        onProgress: suspend (Long, Long) -> Unit,
    ) {
        if (total > 0L) onProgress(consumed, total) else if (consumed > 0L) onProgress(consumed, -1L)
    }
}
