package com.cafarovceyxun.anamuslim.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cafarovceyxun.anamuslim.db.entities.hadith.HadithBookEntity
import com.cafarovceyxun.anamuslim.db.entities.hadith.HadithChapterEntity
import com.cafarovceyxun.anamuslim.db.entities.hadith.HadithEntity
import com.cafarovceyxun.anamuslim.db.entities.hadith.HadithSubChapterEntity
import com.cafarovceyxun.anamuslim.db.entities.hadith.HadithVolumeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Runtime proof that the real HadithDatabase works on iOS/native: the full
 * volume→book→chapter→subchapter→hadith hierarchy, the transactional replaceAll,
 * LIKE search, MAX aggregates, Flow queries and the cascading deleteFullVolume all
 * run against the bundled SQLite driver. Second real database migrated to commonMain.
 */
class HadithDatabaseTest {

    private fun db() = Room.inMemoryDatabaseBuilder<HadithDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

    @Test
    fun hierarchyRoundTripAndCascadingDelete() = runBlocking {
        val db = db()
        val dao = db.hadithDao()

        dao.replaceAll(
            volumes = listOf(
                HadithVolumeEntity(slug = "bukhari", name = "Sahih Bukhari"),
                HadithVolumeEntity(slug = "muslim", name = "Sahih Muslim"),
            ),
            books = listOf(
                HadithBookEntity(slug = "b-iman", volume_slug = "bukhari", book_no = 1, name = "Iman"),
                HadithBookEntity(slug = "m-iman", volume_slug = "muslim", book_no = 1, name = "Iman"),
            ),
            chapters = listOf(
                HadithChapterEntity(slug = "c1", book_slug = "b-iman", chapter_no = 1, name = "Baslangic"),
            ),
            subChapters = listOf(
                HadithSubChapterEntity(slug = "s1", chapter_slug = "c1", sub_chapter_no = 1, name = "Alt"),
            ),
            hadiths = listOf(
                HadithEntity(id = 1L, chapter_slug = "c1", sub_chapter_slug = "s1", hadith_no = 1, text_ar = "نية", text_az = "Emeller niyyetlere goredir"),
                HadithEntity(id = 2L, chapter_slug = "c1", sub_chapter_slug = null, hadith_no = 2, text_ar = "علم", text_az = "Ilm axtarmaq vacibdir"),
            ),
        )

        // Counts / hierarchy reads.
        assertEquals(2, dao.getVolumeCount())
        assertEquals(1, dao.getBooksByVolume("bukhari").size)
        assertEquals(2, dao.getHadithsByChapter("c1").size)
        assertEquals(1, dao.getHadithsBySubChapter("c1", "s1").size)
        assertEquals("Emeller niyyetlere goredir", dao.getHadithById(1L)?.text_az)

        // Flow query returns current snapshot.
        assertEquals(2, dao.getAllVolumesFlow().first().size)

        // LIKE search on text_az.
        assertEquals(1, dao.searchHadiths("niyyet", limit = 10, offset = 0).size)

        // MAX aggregate.
        assertEquals(2, dao.getMaxHadithNoByChapter("c1"))

        // Cascading delete removes only the bukhari subtree.
        dao.deleteFullVolume("bukhari")
        assertEquals(1, dao.getVolumeCount())
        assertNull(dao.getVolumeBySlug("bukhari"))
        assertEquals(0, dao.getHadithsByChapter("c1").size)
        assertEquals(1, dao.getBooksByVolume("muslim").size)

        db.close()
    }
}
