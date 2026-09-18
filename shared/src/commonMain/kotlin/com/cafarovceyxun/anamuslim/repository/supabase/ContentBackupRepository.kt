package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Məzmun cədvəllərinin **tək fayllıq** yedəyi — admin telefondan ala bilsin deyə.
 *
 * Niyə tətbiqin içində: yedəyin tam nüsxəsini Mac alır (`tools/supabase/backup.sh` → iCloud Drive),
 * amma o, yalnız Mac açıq olanda işləyir. Bu yol həmişə əldə olan nüsxədir: admin ana ekrandakı
 * xatırlatmadan basır, fayl sistem seçicisi ilə istədiyi yerə (iCloud, Drive, Files) yazılır.
 *
 * ⚠️ **Sirr işlətmir.** Server tərəfdəki `db-backup` Edge Function-una `x-backup-secret` lazımdır,
 * o sirr isə tətbiqə qoyula bilməz — tətbiq açıq qaynaqdır (GPLv3), yəni sirr hamıya görünərdi.
 * Bunun əvəzinə sorğular adminin **öz Supabase sessiyası** ilə gedir; onsuz da bu cədvəllərin
 * hamısı public oxunandır, ona görə yeni bir açıqlıq yaranmır.
 *
 * ⚠️ **Sətirlər parse edilmir** (2026-09-18): PostgREST-in cavabı mətn kimi götürülüb birbaşa
 * yapışdırılır. Əvvəlki variant hər sətri `JsonObject`-ə çevirirdi — 10 800 sətir üçün yüz minlərlə
 * obyekt yaranırdı və 16 MB-lıq fayl **üç dəfə** yaddaşda dururdu (ağac + mətn + baytlar). Nəticə:
 * fayl seçicisi açılanda tətbiq arxa fonda ən ağır proses olur və sistem onu öldürür — fayl yazılır,
 * amma «alındı» geri çağırışı heç vaxt gəlmir. Bax [com.cafarovceyxun.anamuslim.viewModels.ContentBackupViewModel].
 *
 * ⚠️ Siyahı qəsdən **sabitdir** (Mac-dəki tam yedək `backup_table_list()`-dən dinamik gəlir):
 * buradakı dəst «istifadəçinin özünün qurduğu məzmun»dur — dua, Əsmaül Hüsnə, hədis, tərcümə.
 * Yeni **məzmun** cədvəli əlavə edəndə bura da yaz, yoxsa telefondakı nüsxədə o cədvəl olmaz.
 * (Moderasiya növbələri, təkliflər, loglar burada yoxdur — onlar Mac yedəyindədir.)
 */
object ContentBackupRepository {

    /** cədvəl → sabit sıralama sütunu (`range()` ilə səhifələnən sorğuda sıra müəyyən olmalıdır). */
    private val TABLES = listOf(
        "dua_category" to "slug",
        "dua_subcategory" to "slug",
        "dua" to "id",
        "asma_name" to "no",
        "asma_evidence" to "id",
        "asma_auto_hidden" to "name_no",
        "hadith_volume" to "slug",
        "hadith_book" to "slug",
        "hadith_chapter" to "slug",
        "hadith_sub_chapter" to "slug",
        "hadith" to "id",
        "quran_translation_books" to "slug",
        "quran_translations_data" to "id",
    )

    /** PostgREST bir cavabda ən çox 1000 sətir verir (Supabase-in `db-max-rows` ayarı). */
    private const val PAGE_SIZE = 1000L
    private const val VERSION = 1

    fun fileName(): String = "anamuslim-mezmun-${currentLocalDateIsoString()}.json"

    /**
     * Bütün məzmun cədvəllərini bir JSON mətninə yığır.
     *
     * Sətirlər tipli modelə çevrilmir: yedək sxemin **o günkü** halını saxlamalıdır, modelə
     * çevirsək modeldə olmayan sütunlar səssizcə düşərdi.
     */
    suspend fun build(): String = withContext(Dispatchers.IO) {
        val out = StringBuilder(20 * 1024 * 1024)
        val counts = StringBuilder()

        out.append("{\"version\":").append(VERSION)
            .append(",\"generatedAt\":").append(currentEpochMillis())
            .append(",\"generatedOn\":\"").append(currentLocalDateIsoString()).append('"')
            .append(",\"tables\":{")

        TABLES.forEachIndexed { index, (table, orderBy) ->
            if (index > 0) {
                out.append(',')
                counts.append(',')
            }
            val rows = appendTable(out, table, orderBy)
            counts.append('"').append(table).append("\":").append(rows)
            AppLogger.d(TAG, "$table: $rows sətir")
        }

        out.append("},\"counts\":{").append(counts).append("}}")
        out.toString()
    }

    /** Bir cədvəlin bütün səhifələrini `"ad":[...]` şəklində əlavə edir və sətir sayını qaytarır. */
    private suspend fun appendTable(out: StringBuilder, table: String, orderBy: String): Long {
        out.append('"').append(table).append("\":[")

        var offset = 0L
        var total = -1L
        var written = 0L

        while (true) {
            val result = SupabaseProvider.client.from(table).select(Columns.ALL) {
                // Sayı ilk səhifə ilə birlikdə gəlir — ayrıca sorğuya ehtiyac qalmır.
                if (total < 0) count(Count.EXACT)
                order(orderBy, Order.ASCENDING)
                range(offset, offset + PAGE_SIZE - 1)
            }
            if (total < 0) total = result.countOrNull() ?: 0L

            // Massivin kənar mötərizələri atılır: səhifələr bir massivə yapışdırılır.
            val page = result.data.trim()
            val body = if (page.length <= 2) "" else page.substring(1, page.length - 1)
            if (body.isNotEmpty()) {
                if (written > 0) out.append(',')
                out.append(body)
            }

            offset += PAGE_SIZE
            written = minOf(offset, total)
            if (offset >= total) break
        }

        out.append(']')
        return total
    }

    private const val TAG = "ContentBackup"
}
