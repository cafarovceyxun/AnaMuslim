package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentSchedule
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json

/**
 * Günün ayəsi/hədisi növbəsi (`daily_content_item`).
 *
 * ⚠️ Cədvəlin adı köhnə `daily_content` **deyil** — o ad indi yalnız `slot_index = 0` sətirlərini
 * göstərən uyğunluq view-udur, mağazadakı yenilənməmiş tətbiqlər üçün saxlanılıb. Bura yazmaq
 * yalnız adminə açıqdır (`daily_content_write_admin` siyasəti), oxumaq hamıya.
 *
 * Növbənin sırası `(date, slot_index)` cütüdür: gündə [com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots.COUNT]
 * element, sonrakılar avtomatik növbəti günə keçir. Yerdəyişmə tək `upsert` ilə edilir — bazadakı
 * unikal məhdudiyyət **DEFERRABLE**-dır, ona görə aralıq toqquşma partlatmır; `on conflict` arbitri
 * isə həmişə `id` olmalıdır (təxirə salınmış məhdudiyyət arbitr ola bilmir).
 */
class DailyContentRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(DailyContent.serializer())

    /**
     * Bugündən başlayaraq planlaşdırılmış bütün elementlər, sıra ilə.
     *
     * Uğurlu sorğu keşlənir; şəbəkə yoxdursa keşdən **bugündən sonrakı** hissə qaytarılır. Kart,
     * story və bildiriş cədvəli hamısı bunun üzərində qurulur, ona görə oflayn cihaz da növbəni
     * bilir və bildirişlər çalınmağa davam edir.
     */
    suspend fun fetchUpcoming(): List<DailyContent> = withContext(Dispatchers.IO) {
        val today = currentLocalDateIsoString()
        val until = IsoDate.plusDays(today, DailyContentSchedule.MAX_DAYS_AHEAD) ?: today

        try {
            val items = SupabaseProvider.client.from(TABLE)
                .select {
                    filter {
                        gte("date", today)
                        lte("date", until)
                    }
                }
                .decodeList<DailyContent>()
                .inQueueOrder()

            VersePreferences.setDailyContentItemsCache(
                json.encodeToString(listSerializer, items)
            )

            items
        } catch (e: Exception) {
            cachedUpcoming(today)
        }
    }

    /** Bugünkü elementlər, yuva sırası ilə — ana səhifə kartı və story bunu göstərir. */
    suspend fun fetchTodayItems(): List<DailyContent> =
        fetchUpcoming().filter { it.date == currentLocalDateIsoString() }

    /** Şəbəkəyə toxunmadan son bilinən növbə — bildiriş çalınan anda işlədilir. */
    fun cachedUpcoming(today: String = currentLocalDateIsoString()): List<DailyContent> {
        val cached = VersePreferences.getDailyContentItemsCache()
        if (cached.isBlank()) return emptyList()

        val items = try {
            json.decodeFromString(listSerializer, cached)
        } catch (e: Exception) {
            // Sxem dəyişibsə (və ya köhnə tək-sətirli keş qalıbsa) oxunmur; boş növbə çökmədən yaxşıdır.
            return emptyList()
        }

        return items.filter { (it.date ?: return@filter false) >= today }.inQueueOrder()
    }

    /** Admin paneli: bugündən sonrakı növbə. */
    suspend fun fetchQueue(): List<DailyContent> = fetchUpcoming()

    /** Admin paneli: artıq göstərilmiş elementlər, yenidən növbəyə salmaq üçün. */
    suspend fun fetchHistory(limit: Int = HISTORY_LIMIT): List<DailyContent> =
        withContext(Dispatchers.IO) {
            val today = currentLocalDateIsoString()

            runCatching {
                SupabaseProvider.client.from(TABLE)
                    .select {
                        filter { lt("date", today) }
                        order("date", Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<DailyContent>()
                    .sortedWith(compareByDescending<DailyContent> { it.date.orEmpty() }
                        .thenByDescending { it.slot_index })
            }.getOrDefault(emptyList())
        }

    /**
     * Elementi növbənin sonuna əlavə edir — boş olan **ilk gələcək** yuvaya.
     *
     * Bu günün vaxtı keçmiş yuvaları atlanır: ora düşən element heç vaxt bildirilməzdi.
     */
    suspend fun enqueue(item: DailyContent): Result<DailyContent> = withContext(Dispatchers.IO) {
        runCatching {
            val queue = fetchQueue()
            val taken = queue.mapTo(HashSet()) { it.slotKey }

            val (date, slot) = DailyContentSchedule.firstFreeSlot(
                taken = taken,
                today = currentLocalDateIsoString(),
                nowMillis = currentEpochMillis(),
            ) ?: throw IllegalStateException("Növbədə boş yuva qalmayıb")

            insertAt(item, date, slot)
        }
    }

    private suspend fun insertAt(item: DailyContent, date: String, slot: Int): DailyContent {
        val inserted = SupabaseProvider.client.from(TABLE)
            .insert(item.copy(id = null, date = date, slot_index = slot)) { select() }
            .decodeList<DailyContent>()

        // RLS bloklayanda PostgREST xəta yox, boş nəticə qaytarır (CLAUDE.md) — sətir sayını yoxla.
        return inserted.firstOrNull() ?: throw IllegalStateException("Sətir yazılmadı (RLS?)")
    }

    /** Mövcud elementin məzmununu yeniləyir; yeri (`date`/`slot_index`) dəyişmir. */
    suspend fun update(item: DailyContent): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val id = item.id ?: throw IllegalArgumentException("id yoxdur")

            val updated = SupabaseProvider.client.from(TABLE)
                .update({
                    set("content_type", item.content_type)
                    set("chapter_no", item.chapter_no)
                    set("verse_no", item.verse_no)
                    set("verse_end", item.verse_end)
                    set("hadith_id", item.hadith_id)
                    set("text_ar", item.text_ar)
                    set("text_az", item.text_az)
                    // Çıxarışı təmizləmək mümkün olsun deyə null da açıq göndərilir.
                    set("excerpt_ar", item.excerpt_ar)
                    set("excerpt_az", item.excerpt_az)
                    set("source", item.source)
                }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<DailyContent>()

            if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
        }
    }

    /**
     * Elementi silir və nəticəni **yenidən oxuyaraq** yoxlayır.
     *
     * `delete { select() }`-in qaytardığı gövdəyə güvənmirik: RLS bloklayanda PostgREST xəta yox,
     * boş nəticə qaytarır (CLAUDE.md), amma boş nəticə həm də «silindi, sadəcə gövdə qayıtmadı»
     * demək ola bilər — bu halda uğurlu silmə səhvən xəta kimi göstərilirdi. Sətrin qalıb-qalmadığı
     * yeganə birmənalı cavabdır.
     */
    suspend fun delete(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE).delete { filter { eq("id", id) } }

            val stillThere = SupabaseProvider.client.from(TABLE)
                .select { filter { eq("id", id) } }
                .decodeList<DailyContent>()
                .isNotEmpty()

            if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
        }
    }

    /**
     * Verilmiş sıranı bugündən başlayan ardıcıl yuvalara yazır — həm yerdəyişmə, həm də silmədən
     * sonra qalan boşluqların doldurulması buradan keçir.
     *
     * Hamısı **tək** upsert-dir: ara vəziyyətdə iki element eyni yuvada görünə bilər, unikal
     * məhdudiyyət isə təxirə salındığı üçün yalnız sonda yoxlanılır.
     */
    suspend fun reschedule(orderedItems: List<DailyContent>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (orderedItems.isEmpty()) return@runCatching

                val now = currentEpochMillis()
                val today = currentLocalDateIsoString()

                // ⚠️ Vaxtı keçmiş yuvalar toxunulmazdır. Onlar artıq bildirilib (və kartda
                // görünür); gələcək yuvaya köçürülsələr eyni ayə ikinci dəfə çalınardı.
                val movable = orderedItems.filterNot { it.isPast(now) }
                if (movable.isEmpty()) return@runCatching

                val startSlot = DailyContentSchedule.firstFreeSlot(
                    taken = emptySet(),
                    today = today,
                    nowMillis = now,
                ) ?: (today to 0)

                val slots = DailyContentSchedule.consecutiveSlots(
                    count = movable.size,
                    startDate = startSlot.first,
                    startSlot = startSlot.second,
                )

                val rows = movable.mapIndexed { index, item ->
                    item.copy(date = slots[index].first, slot_index = slots[index].second)
                }

                // Yalnız həqiqətən yeri dəyişənlər göndərilir — boş yazma sorğusu etməmək üçün.
                val changed = rows.filterIndexed { index, row ->
                    movable[index].date != row.date || movable[index].slot_index != row.slot_index
                }

                if (changed.isEmpty()) return@runCatching

                // ⚠️ Upsert **yaramır**: `id` sütunu `GENERATED ALWAYS AS IDENTITY`-dir, PostgREST-in
                // `on_conflict=id` upsert-i isə açıq id göndərir. Ona görə yerdəyişmə serverdəki
                // `reschedule_daily_content` funksiyasından keçir — bir ifadə, bir tranzaksiya,
                // yəni təxirə salınmış unikal məhdudiyyət aralıq toqquşmanı bağışlayır.
                val payload = buildJsonArray {
                    changed.forEach { row ->
                        addJsonObject {
                            put("id", row.id)
                            put("date", row.date)
                            put("slot_index", row.slot_index)
                        }
                    }
                }

                val written = SupabaseProvider.client.postgrest.rpc(
                    function = "reschedule_daily_content",
                    parameters = buildJsonObject { put("items", payload) },
                ).decodeAs<Int>()

                // RLS bloklayanda funksiya xəta yox, 0 sətir qaytarır (CLAUDE.md).
                if (written <= 0) throw IllegalStateException("Növbə yazılmadı (RLS?)")
            }
        }

    /**
     * Hekayəyə baxışı sayır — **cihaz başına bir dəfə**.
     *
     * Sayğac serverdədir, dedupe isə cihazda ([VersePreferences.isViewCounted]): baza kimin
     * baxdığını bilmir, `suggestions.vote_count` ilə eyni yanaşma. Uğursuzluq səssiz keçir — baxış
     * sayı analitikadır, istifadəçinin gördüyü heç nəyi dəyişmir.
     */
    suspend fun registerView(itemId: Long) = withContext(Dispatchers.IO) {
        if (VersePreferences.isViewCounted(itemId)) return@withContext

        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                function = "increment_daily_content_view",
                parameters = buildJsonObject { put("p_id", itemId) },
            )
        }.onSuccess {
            VersePreferences.markViewCounted(itemId)
        }

        Unit
    }

    private fun List<DailyContent>.inQueueOrder(): List<DailyContent> =
        sortedWith(compareBy<DailyContent> { it.date.orEmpty() }.thenBy { it.slot_index })

    private companion object {
        /** Uyğunluq view-u `daily_content` adını daşıyır — cədvəlin özü budur. */
        const val TABLE = "daily_content_item"
        const val HISTORY_LIMIT = 60
    }
}
