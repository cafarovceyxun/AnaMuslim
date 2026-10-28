package com.cafarovceyxun.anamuslim.search

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cafarovceyxun.anamuslim.db.HadithDatabase
import com.cafarovceyxun.anamuslim.db.entities.hadith.HadithEntity
import com.cafarovceyxun.anamuslim.db.searchindex.SearchIndexDatabase
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [HadithSearchIndex] real SQLite-da (bundled driver): contentless FTS4 cədvəli, dırnaqlı prefiks
 * sorğusu və barmaq izi ilə yenidən qurulma. Köhnə `LIKE` axtarışının tapmadığı hallar burada
 * sınanır — Azərbaycan hərflərinin registri, mətndəki sətir sonu, hərəkəli ərəbcə, kitab nömrəsi.
 */
class HadithSearchIndexTest {

    private fun setUp(hadiths: List<HadithEntity>): HadithDatabase = runBlocking {
        val hadithDb = Room.inMemoryDatabaseBuilder<HadithDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        val indexDb = Room.inMemoryDatabaseBuilder<SearchIndexDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

        hadithDb.hadithDao().replaceAll(emptyList(), emptyList(), emptyList(), emptyList(), hadiths)
        RepositoryProvider.setHadithDatabaseProvider { hadithDb }
        RepositoryProvider.setSearchIndexDatabaseProvider { indexDb }
        HadithSearchIndex.invalidate()
        hadithDb
    }

    private fun hadith(id: Long, az: String, ar: String = "") =
        HadithEntity(id = id, chapter_slug = "c1", hadith_no = id.toInt(), text_ar = ar, text_az = az)

    private fun search(query: String, limit: Int = 50, offset: Int = 0) = runBlocking {
        HadithSearchIndex.search(query, limit, offset)
    }

    @Test
    fun azerbaijaniCapitalsAndLineBreaksNoLongerHideAHadith() {
        setUp(
            listOf(
                hadith(1, "1. Bizə Sufyan danışdı: \r\n\"Ömər ibn Xattab dedi: {Əməllər yalnız niyyətlərə görədir.}"),
                hadith(2, "2. Bizə İsmayıl danışdı: \"İman nədir?\""),
            ),
        )

        // `LIKE` yalnız ASCII registrini udurdu: «əməllər» «Əməllər»i, «ömər» «Ömər»i tapmırdı.
        assertEquals(listOf(1L), search("əməllər yalnız niyyətlərə görədir"))
        assertEquals(listOf(1L), search("ömər ibn xattab"))
        assertEquals(listOf(2L), search("iman nədir"))
        // Mətndə `\r\n` var, yapışdırılan parçada boşluq — alt-sətir bunu tapmırdı.
        assertEquals(listOf(1L), search("Sufyan danışdı: \"Ömər ibn Xattab dedi"))
    }

    @Test
    fun arabicQueryWithoutHarakatFindsTheVowelledText() {
        setUp(listOf(hadith(1, "1. Niyyət", ar = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")))

        assertEquals(listOf(1L), search("الاعمال بالنيات"))
        assertEquals(listOf(1L), search("إِنَّمَا"))
    }

    @Test
    fun numbersAreSearchableAndTheBookNumberComesFirst() = runBlocking {
        val db = setUp(
            listOf(
                hadith(1, "40. Bizə Malik danışdı"),
                hadith(2, "41. Bizə Əli danışdı: qırx — 40 gün"),
                hadith(3, "400. Bizə Ömər danışdı"),
                hadith(4, "7. Yeddi dəfə"),
            ),
        )

        // «40» dəqiq ədəddir: «400»-ə düşmür.
        assertEquals(listOf(1L, 2L), search("40"))
        // Tək rəqəm əvvəl sorğudan tamam atılırdı.
        assertEquals(listOf(4L), search("7"))

        // Mətni «41.» ilə başlayan hədis, ədəd ondan əvvəlki id-də də keçsə, önə çəkilir.
        val numbered = db.hadithDao().idsOfHadithsNumbered("41")
        assertEquals(listOf(2L), numbered)
        assertEquals(listOf(2L), HadithSearchIndex.search("41", 50, 0, numberedFirst = numbered))

        // «400.» «40.%» şablonuna düşmür.
        assertEquals(listOf(1L), db.hadithDao().idsOfHadithsNumbered("40"))
        // Önə çəkilən id adi `id` sırasını qabaqlayır.
        assertEquals(listOf(2L, 1L), HadithSearchIndex.search("40", 50, 0, numberedFirst = listOf(2L)))
    }

    @Test
    fun indexIsRebuiltWhenTheContentChanges() = runBlocking {
        val db = setUp(listOf(hadith(1, "1. Səbr")))
        assertEquals(listOf(1L), search("səbr"))
        assertEquals(emptyList(), search("şükür"))

        db.hadithDao().replaceAll(
            emptyList(), emptyList(), emptyList(), emptyList(),
            listOf(hadith(1, "1. Səbr"), hadith(2, "2. Şükür")),
        )
        HadithSearchIndex.invalidate()

        assertEquals(listOf(2L), search("şükür"))
    }

    @Test
    fun pagingKeepsIdOrder() {
        setUp((1L..5L).map { hadith(it, "$it. Namaz haqqında") })

        assertEquals(listOf(1L, 2L), search("namaz", limit = 2, offset = 0))
        assertEquals(listOf(3L, 4L), search("namaz", limit = 2, offset = 2))
        assertEquals(listOf(5L), search("namaz", limit = 2, offset = 4))
    }
}
