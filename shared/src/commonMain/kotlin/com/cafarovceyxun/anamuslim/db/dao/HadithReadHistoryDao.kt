package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HadithReadHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HadithReadHistoryEntity): Long

    @Query("SELECT * FROM hadith_read_history ORDER BY datetime DESC LIMIT :limit")
    fun getFlow(limit: Int): Flow<List<HadithReadHistoryEntity>>

    /**
     * Hər **kitab** üzrə ən son oxunan yer — bütün səviyyələrdəki «davam et» düyməsi buna baxır.
     *
     * Cild yox, kitab: bir cilddə bir neçə kitab var və istifadəçi onları paralel oxuyur; cild
     * başına tək qeyd saxlayanda başqa kitabı açmaq əvvəlkinin yerini itirirdi.
     *
     * Cild səviyyəsindəki nişan bu siyahıdan **Kotlin tərəfdə** çıxarılır (cildin ən son sətri) —
     * ikinci sorğu yazsaydıq iki fərqli «ən son» tərifi yaranardı və onlar vaxtla bir-birindən
     * sürüşərdi.
     *
     * `GROUP BY` şərtdən sonra qalır: eyni kitaba eyni millisaniyədə iki sətir düşsə (silinmə/təkrar
     * yazılma yarışı) alt sorğu ikisini də seçər və bir kitab siyahıda iki dəfə görünərdi.
     * `IFNULL` lazımdır, çünki `book_slug` köhnə sətirlərdə boş ola bilər və SQL-də `NULL = NULL`
     * heç vaxt doğru deyil — onsuz kitabsız sətirlər qrupdan tamamilə düşərdi.
     */
    @Query(
        """
        SELECT * FROM hadith_read_history AS h
        WHERE h.datetime = (
            SELECT MAX(x.datetime) FROM hadith_read_history AS x
            WHERE x.volume_slug = h.volume_slug
              AND IFNULL(x.book_slug, '') = IFNULL(h.book_slug, '')
        )
        GROUP BY h.volume_slug, IFNULL(h.book_slug, '')
        """
    )
    fun getLatestPerBookFlow(): Flow<List<HadithReadHistoryEntity>>

    // Offset paging: consumed by an app-side PagingSource (Room-KMP paging is not wired yet).
    @Query("SELECT * FROM hadith_read_history ORDER BY datetime DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllPaged(limit: Int, offset: Int): List<HadithReadHistoryEntity>

    @Query("SELECT COUNT(*) FROM hadith_read_history")
    suspend fun countHadithReadHistory(): Int

    @Query("DELETE FROM hadith_read_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM hadith_read_history WHERE volume_slug = :v AND (book_slug = :b OR (book_slug IS NULL AND :b IS NULL)) AND (chapter_slug = :c OR (chapter_slug IS NULL AND :c IS NULL)) AND (sub_chapter_slug = :s OR (sub_chapter_slug IS NULL AND :s IS NULL))")
    suspend fun deleteDuplicate(v: String, b: String?, c: String?, s: String?)

    /**
     * Tarixçəni **hər kitab üçün ayrıca** [limit] sətirlə kəsir.
     *
     * Əvvəl qlobal tavan vardı: bir kitabı intensiv oxumaq qalan kitabların bütün tarixçəsini
     * sıxışdırıb çıxarırdı.
     *
     * Pəncərə funksiyası (`ROW_NUMBER() OVER`) **qəsdən işlədilmir**: Android tərəfdə baza sistemin
     * öz SQLite-ı ilə açılır (`DatabaseProvider`), pəncərə funksiyaları isə yalnız API 30+-dakı
     * SQLite 3.25-dən var — köhnə telefonda sorğu run-time-da partlayardı, kompilyator isə susardı.
     * Əvəzindəki əlaqəli alt sorğu O(n²)-dir, amma `n` bir neçə yüz sətirdir.
     *
     * Bərabər `datetime`-da `id` ilə qırılır ki, sıralama sabit olsun və kəsim iki sətri birdən
     * saxlayıb tavanı aşmasın.
     */
    @Query(
        """
        DELETE FROM hadith_read_history
        WHERE id NOT IN (
            SELECT h.id FROM hadith_read_history AS h
            WHERE (
                SELECT COUNT(*) FROM hadith_read_history AS x
                WHERE x.volume_slug = h.volume_slug
                  AND IFNULL(x.book_slug, '') = IFNULL(h.book_slug, '')
                  AND (x.datetime > h.datetime OR (x.datetime = h.datetime AND x.id > h.id))
            ) < :limit
        )
        """
    )
    suspend fun trimPerBook(limit: Int)

    @Query("DELETE FROM hadith_read_history")
    suspend fun deleteAll()
}
