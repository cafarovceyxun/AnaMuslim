package com.cafarovceyxun.anamuslim.utils.managers

import com.cafarovceyxun.anamuslim.api.GithubApi
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.search.TranslationSearchIndexer
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.DownloadNotifier
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseTranslation
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okio.Buffer

/**
 * Multiplatform [TranslationDownloadSource]: downloads a translation book straight on a coroutine
 * scope and stores it through the shared translation store.
 *
 * **Why this is not the Android implementation.** The download itself was already portable — Ktor
 * streaming, the Supabase client, [QuranTranslationFactory] and [ReaderPreferences] are all in
 * `commonMain`. What Android needs on top of it is *survival*: WorkManager keeps the transfer alive
 * when the app is backgrounded and shows the progress notification, so `AndroidTranslationDownloadSource`
 * stays as it is. iOS registers this class instead; a background-capable iOS variant
 * (`URLSession` background sessions / `BGTaskScheduler`) can replace the transport later without
 * touching the ViewModel.
 *
 * Same policy/mechanism split as [SharedRecitationPlayer][com.cafarovceyxun.anamuslim.utils.mediaplayer.SharedRecitationPlayer].
 */
class SharedTranslationDownloader(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : TranslationDownloadSource {

    private val lock = ReentrantLock()
    private val jobs = mutableMapOf<String, Job>()

    /**
     * Progress is republished on every chunk, so a ViewModel that subscribes mid-download catches up
     * on the next emission — no replay buffer needed.
     */
    private val events = MutableSharedFlow<Pair<String, ResourceDownloadStatus>>(
        extraBufferCapacity = 64,
    )

    override fun initialize() = Unit

    override fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> = events.asSharedFlow()

    override fun isDownloading(slug: String): Boolean =
        lock.withLock { jobs[slug]?.isActive == true }

    override fun startDownload(bookInfo: TranslationBookInfoModel) {
        val slug = bookInfo.slug
        // Same as WorkManager's KEEP policy: a second tap while a download runs is ignored.
        if (isDownloading(slug)) return

        val job = scope.launch {
            emit(slug, ResourceDownloadStatus.Started)
            try {
                val translData = if (slug == SUPABASE_SLUG) {
                    downloadFromSupabase()
                } else {
                    downloadFromGithub(bookInfo)
                }

                QuranTranslationFactory().use { it.store.storeTranslation(bookInfo, translData) }

                // Android's worker hands this to `SearchIndexScheduler`; here it runs inline, so a
                // freshly downloaded book is searchable without a scheduler. Indexing failures must
                // not fail the download — the book is already stored and readable.
                runCatching { TranslationSearchIndexer.indexSlugIfNeeded(slug) }
                    .onFailure { AppLogger.saveError(it, "SearchIndex:$slug") }

                // Mirrors the Android worker: drop the slug from the saved reader selection so the
                // reader re-resolves the freshly stored book instead of the stale one.
                val savedTranslations = ReaderPreferences.getTranslations().toMutableSet()
                if (savedTranslations.remove(slug)) {
                    ReaderPreferences.setTranslations(savedTranslations)
                }

                emit(slug, ResourceDownloadStatus.Completed)
                DownloadNotifier.completed(notificationLabel(bookInfo))
            } catch (e: CancellationException) {
                emit(slug, ResourceDownloadStatus.Cancelled)
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedTranslationDownloader:$slug")
                emit(slug, ResourceDownloadStatus.Failed(e.message))
                DownloadNotifier.failed(notificationLabel(bookInfo))
            } finally {
                lock.withLock { jobs.remove(slug) }
            }
        }

        lock.withLock { jobs[slug] = job }
    }

    override fun stopDownload(slug: String) {
        lock.withLock { jobs[slug] }?.cancel()
    }

    /** What the notification names: the book as the user picked it, falling back to the slug. */
    private fun notificationLabel(bookInfo: TranslationBookInfoModel): String =
        bookInfo.displayName.ifBlank { bookInfo.bookName }.ifBlank { bookInfo.slug }

    /** Streams the book JSON, publishing byte progress as it arrives. */
    private suspend fun downloadFromGithub(bookInfo: TranslationBookInfoModel): String =
        GithubApi.getTranslation(bookInfo.downloadPath) { scope ->
            if (!scope.isSuccessful) throw okio.IOException("HTTP ${scope.statusCode}")

            val totalBytes = scope.contentLength
            // Chunks are buffered as bytes, not decoded per chunk: a UTF-8 sequence can straddle a
            // chunk boundary, so decoding happens once at the end.
            val buffer = Buffer()
            var downloaded = 0L
            var lastProgress = -1

            while (true) {
                // Cancellation must stop the stream mid-body, not just between downloads.
                currentCoroutineContext().ensureActive()
                val packet = scope.channel.readRemaining(CHUNK_SIZE)
                val bytes = packet.readByteArray()
                if (bytes.isEmpty()) break

                buffer.write(bytes)
                downloaded += bytes.size

                if (totalBytes > 0) {
                    val progress = ((downloaded * 100) / totalBytes).toInt()
                    // Emitting per chunk would flood the UI; only whole-percent steps are published.
                    if (progress != lastProgress) {
                        lastProgress = progress
                        emit(bookInfo.slug, ResourceDownloadStatus.InProgress(progress))
                    }
                }
            }

            buffer.readUtf8()
        }

    /**
     * The Azerbaijani book lives in Supabase rather than the GitHub inventory, paged 1000 rows at a
     * time. Kept identical to the Android worker, including the "keep what we already fetched"
     * error handling.
     */
    private suspend fun downloadFromSupabase(): String {
        val allRows = mutableListOf<SupabaseTranslation>()
        var offset = 0

        try {
            while (offset < MAX_SUPABASE_ROWS) {
                val response = SupabaseProvider.client.from("translations").select {
                    filter { eq("slug", SUPABASE_SLUG) }
                    order("id", Order.ASCENDING)
                    range(offset.toLong(), (offset + SUPABASE_PAGE_SIZE - 1).toLong())
                }.decodeList<SupabaseTranslation>()

                if (response.isEmpty()) break
                allRows.addAll(response)

                emit(SUPABASE_SLUG, ResourceDownloadStatus.InProgress(supabaseProgress(allRows.size)))

                if (response.size < SUPABASE_PAGE_SIZE) break
                offset += SUPABASE_PAGE_SIZE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.d("Supabase yükləmə xətası ($offset): ${e.message}")
            if (allRows.isEmpty()) throw e
        }

        if (allRows.isEmpty()) throw okio.IOException("Supabase-də məlumat tapılmadı")

        val verses = allRows.associate { row ->
            "${row.chapter_no}:${row.verse_no}" to buildJsonObject {
                put("t", row.text.orEmpty())
                if (!row.note.isNullOrEmpty()) put("n", row.note)
            }
        }
        return JsonObject(verses).toString()
    }

    /**
     * Supabase pages give a row count, not a byte count, so progress is estimated against the
     * Quran's verse total and clamped — the table can hold slightly fewer or more rows.
     */
    private fun supabaseProgress(rows: Int): Int =
        ((rows * 100) / TOTAL_VERSES).coerceIn(0, 99)

    private suspend fun emit(slug: String, status: ResourceDownloadStatus) {
        events.emit(slug to status)
    }

    private companion object {
        const val SUPABASE_SLUG = "az"
        const val SUPABASE_PAGE_SIZE = 1000
        const val MAX_SUPABASE_ROWS = 7000
        const val TOTAL_VERSES = 6236
        const val CHUNK_SIZE = 64L * 1024
    }
}
