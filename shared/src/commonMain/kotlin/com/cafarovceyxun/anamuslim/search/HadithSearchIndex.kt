package com.cafarovceyxun.anamuslim.search

import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Hədis mətnlərinin FTS indeksi — `SearchIndex.db`-də, tərcümə indeksinin yanında.
 *
 * Əvvəl hədis axtarışı hər hərfdə `text_az LIKE '%…%'` və 18 qatlı `replace()` ilə **bütün** cədvəli
 * (≈19 MB) oxuyurdu, nəticə isə yenə səhv idi:
 * - SQLite-ın `LIKE`-ı yalnız ASCII hərflərin registrini udur — «əməllər» «Əməllər»i, «ömər»
 *   «Ömər»i tapmırdı;
 * - uyğunluq hərfi alt-sətir idi — mətndəki sətir sonu (`\r\n`), iki boşluq və ya fərqli dırnaq
 *   yapışdırılan uzun parçanı tapılmaz edirdi.
 *
 * FTS sözlərlə işləyir: registr və boşluq fərqi yox olur, sorğu isə indeksdə dərhal cavablanır.
 *
 * Cədvəl **contentless**-dir (`content=""`): mətn onsuz da `hadith_db`-dədir, ikinci nüsxə diskdə
 * ~15 MB tutardı. Room bu növü elan edə bilmir, ona görə cədvəl xam SQL ilə qurulur (Room tanımadığı
 * cədvələ toxunmur; destructive miqrasiya onu da atır və növbəti axtarış yenidən qurur). Contentless
 * cədvəldə sətir silinə bilmir — dəyişiklikdə indeks bütövlükdə yenidən qurulur, 4 min hədis üçün bu
 * bir-iki saniyədir və yalnız məzmun dəyişəndə olur.
 *
 * Təzəlik barmaq izi ilə yoxlanılır ([com.cafarovceyxun.anamuslim.db.dao.HadithDao.searchFingerprint]),
 * proses başına bir dəfə və hər [invalidate]-dən sonra: sinxron və redaktor yazısı onu çağırır.
 */
object HadithSearchIndex {

    private val db get() = RepositoryProvider.searchIndexDatabase
    private val hadithDao get() = RepositoryProvider.hadithDatabase.hadithDao()

    private val mutex = Mutex()
    private val generation = MutableStateFlow(0)
    private var verifiedGeneration = -1

    /** Hədis məzmunu dəyişib — növbəti axtarış barmaq izini yenidən yoxlayır. */
    fun invalidate() {
        generation.update { it + 1 }
    }

    /** İndeksi əvvəlcədən hazırlayır ki, ilk axtarış yenidən qurulmanı gözləməsin. */
    suspend fun warmUp() = ensureFresh()

    /**
     * Sorğuya uyğun hədislərin `id`-ləri, `id` sırası ilə (köhnə `LIKE` sorğusunun sırası).
     *
     * Yalnız rəqəmdən ibarət sorğuda kitab nömrəsi həmin ədəd olan hədislər ([numberedFirst])
     * **önə** çəkilir: «1234» yazan 1234-cü hədisi axtarır, mətnində bu ədəd keçən o biri hədisləri
     * yox. Sıra SQL-dədir ki, səhifələr arasında təkrar və boşluq olmasın.
     *
     * Sorğuda işlənəsi söz yoxdursa ([FtsQueryBuilder.searchTerms]) boş siyahı.
     */
    suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
        numberedFirst: List<Long> = emptyList(),
    ): List<Long> = withContext(Dispatchers.IO) {
        val normalized = SearchNormalizer.arabicNormalize(SearchNormalizer.normalize(query))
        val terms = FtsQueryBuilder.searchTerms(normalized)
        if (terms.isEmpty()) return@withContext emptyList()

        ensureFresh()

        db.useReaderConnection { connection ->
            val match = FtsQueryBuilder.toMatchQueryWithFallback(terms) { candidate ->
                connection.usePrepared("SELECT 1 FROM $TABLE WHERE $TABLE MATCH ? LIMIT 1") { stmt ->
                    stmt.bindText(1, candidate)
                    stmt.step()
                }
            } ?: return@useReaderConnection emptyList()

            val priority = if (numberedFirst.isEmpty()) "" else
                "docid IN (${numberedFirst.joinToString(",")}) DESC, "

            connection.usePrepared(
                "SELECT docid FROM $TABLE WHERE $TABLE MATCH ? ORDER BY ${priority}docid LIMIT ? OFFSET ?"
            ) { stmt ->
                stmt.bindText(1, match)
                stmt.bindLong(2, limit.toLong())
                stmt.bindLong(3, offset.toLong())
                buildList { while (stmt.step()) add(stmt.getLong(0)) }
            }
        }
    }

    private suspend fun ensureFresh() {
        val wanted = generation.value
        mutex.withLock {
            if (verifiedGeneration == wanted) return

            val fingerprint = "${hadithDao.searchFingerprint()}|$INDEX_FORMAT"
            if (db.searchIndexDao().getMeta(META_KEY)?.fingerprint != fingerprint) {
                rebuild(fingerprint)
            }
            verifiedGeneration = wanted
        }
    }

    private suspend fun rebuild(fingerprint: String) = withContext(Dispatchers.IO) {
        db.useWriterConnection { connection ->
            connection.immediateTransaction {
                execSQL("DROP TABLE IF EXISTS $TABLE")
                execSQL(
                    "CREATE VIRTUAL TABLE $TABLE USING fts4(content=\"\", text_az, text_ar, " +
                        "tokenize=unicode61 \"remove_diacritics=1\")"
                )

                var afterId = Long.MIN_VALUE
                while (true) {
                    val batch = hadithDao.getHadithsAfter(afterId, BATCH_SIZE)
                    if (batch.isEmpty()) break

                    usePrepared("INSERT INTO $TABLE(docid, text_az, text_ar) VALUES (?, ?, ?)") { stmt ->
                        for (hadith in batch) {
                            stmt.bindLong(1, hadith.id)
                            stmt.bindText(2, SearchNormalizer.normalize(hadith.text_az))
                            // `text_ar` tam hərəkəli saxlanılır, sorğu isə hərəkəsiz gəlir — indeks
                            // sorğu ilə eyni reduksiyadan keçir ([SearchNormalizer.arabicNormalize]).
                            stmt.bindText(3, SearchNormalizer.arabicNormalize(hadith.text_ar))
                            stmt.step()
                            stmt.reset()
                        }
                    }
                    afterId = batch.last().id
                }

                // Barmaq izi tərcümə indeksinin meta cədvəlində saxlanılır: `#` heç bir tərcümə
                // slug-ında olmur, `syncAll` isə yalnız kitab siyahısını gəzir.
                usePrepared(
                    "INSERT OR REPLACE INTO translation_index_meta(slug, fingerprint) VALUES (?, ?)"
                ) { stmt ->
                    stmt.bindText(1, META_KEY)
                    stmt.bindText(2, fingerprint)
                    stmt.step()
                }
            }
        }
    }

    private const val TABLE = "hadith_search_fts"
    private const val META_KEY = "#hadith"
    private const val BATCH_SIZE = 400

    /** İndeksə nəyin necə yazıldığı dəyişəndə artır — köhnə indeks atılır. */
    private const val INDEX_FORMAT = 1
}
