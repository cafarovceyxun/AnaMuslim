package com.cafarovceyxun.anamuslim.utils.managers

import com.cafarovceyxun.anamuslim.db.entities.hadith.toEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * The full hadith sync: pull every table from Supabase, then replace the local database once.
 *
 * This is the *work*, shared by both platforms; scheduling it is the platform's business —
 * Android runs it inside `HadithDownloadWorker` (WorkManager, for background survival and the
 * progress notification), iOS from [SharedHadithSync]. Same split as
 * [SharedTranslationDownloader] and
 * [TranslationSearchIndexer][com.cafarovceyxun.anamuslim.search.TranslationSearchIndexer].
 */
object HadithSyncEngine {

    private const val PAGE_SIZE = 1000

    /**
     * Fetches everything into memory first and touches the database only at the end, in a single
     * atomic replace — a sync killed midway therefore never leaves a partial hadith database.
     *
     * [onProgress] receives a user-facing label and a 0..100 percentage.
     */
    suspend fun sync(onProgress: suspend (label: String, progress: Int) -> Unit) =
        withContext(Dispatchers.IO) {
            onProgress("Baza yenilənir...", 5)
            val volumes = SupabaseProvider.client.from("hadith_volume").select()
                .decodeList<HadithVolume>()

            onProgress("Kitablar endirilir...", 10)
            val books = fetchPaged<HadithBook>("hadith_book", "slug")

            onProgress("Bablara keçilir...", 20)
            val chapters = fetchPaged<HadithChapter>("hadith_chapter", "slug")

            onProgress("Alt bablar endirilir...", 30)
            val subChapters = fetchPaged<HadithSubChapter>("hadith_sub_chapter", "slug")

            onProgress("Hədislər endirilir...", 40)
            val allHadiths = fetchPaged<Hadith>("hadith", "id") { count ->
                onProgress("Hədislər endirilir: $count", 40 + (count / 250).coerceAtMost(55))
            }

            onProgress("Yadda saxlanılır...", 96)
            val hadithEntities = allHadiths.mapNotNull { it.toEntity() }
            val dropped = allHadiths.size - hadithEntities.size
            if (dropped > 0) {
                AppLogger.d("HadithSync: Skipped $dropped hadith(s) with a null id during sync")
            }

            RepositoryProvider.hadithDatabase.hadithDao().replaceAll(
                volumes = volumes.map { it.toEntity() },
                books = books.map { it.toEntity() },
                chapters = chapters.map { it.toEntity() },
                subChapters = subChapters.map { it.toEntity() },
                hadiths = hadithEntities,
            )

            onProgress("Tamamlandı", 100)
        }

    /** Postgrest caps a response at 1000 rows, so every table is walked in ordered ranges. */
    private suspend inline fun <reified T : Any> fetchPaged(
        table: String,
        orderColumn: String,
        onPage: (count: Int) -> Unit = {},
    ): List<T> {
        val all = mutableListOf<T>()
        var offset = 0

        while (true) {
            val page = SupabaseProvider.client.from(table).select {
                order(orderColumn, Order.ASCENDING)
                range(offset.toLong(), (offset + PAGE_SIZE - 1).toLong())
            }.decodeList<T>()

            if (page.isEmpty()) break
            all.addAll(page)
            onPage(all.size)

            if (page.size < PAGE_SIZE) break
            offset += PAGE_SIZE
        }

        return all
    }
}
