package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.LunarAnnouncement
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Qəməri ay elanlarının şəbəkə tərəfi (`lunar_announcement`).
 *
 * Oxu hamıya açıqdır, yazma RLS ilə adminə bağlıdır. Uğurlu sorğu **keşlənir**: qəməri tarix
 * tətbiqin hər yerində (vidcet daxil) görünür, ona görə oflayn cihaz da son elanı bilməlidir —
 * yoxsa təyyarə rejimində tarix bir gün geri sürüşərdi.
 *
 * Yalnız **son [HISTORY_MONTHS] ay** çəkilir: hekayə zolağı da bu qədər göstərir, serverdə köhnəsi
 * isə [prune] ilə fayllarıyla birlikdə silinir.
 */
class LunarAnnouncementRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(LunarAnnouncement.serializer())

    /** Son 12 elan, yenisi öndə. Şəbəkə çatmasa keşdəki siyahı qayıdır. */
    suspend fun fetchRecent(): List<LunarAnnouncement> = withContext(Dispatchers.IO) {
        try {
            val items = SupabaseProvider.client.from(TABLE)
                .select {
                    order("start_date", Order.DESCENDING)
                    limit(HISTORY_MONTHS.toLong())
                }
                .decodeList<LunarAnnouncement>()

            PrayerPreferences.setLunarAnnouncementsCache(
                json.encodeToString(listSerializer, items)
            )

            items
        } catch (e: Exception) {
            AppLogger.d(TAG, "Fetch failed: ${e.message}")
            cached()
        }
    }

    /** Şəbəkəyə toxunmadan son bilinən siyahı — vidcet və açılış anı bunu oxuyur. */
    fun cached(): List<LunarAnnouncement> {
        val raw = PrayerPreferences.getLunarAnnouncementsCache()
        if (raw.isBlank()) return emptyList()

        return try {
            json.decodeFromString(listSerializer, raw)
        } catch (e: Exception) {
            // Sxem dəyişibsə keş oxunmur; boş siyahı çökmədən yaxşıdır — tətbiq platformanın öz
            // təqviminə qayıdır.
            emptyList()
        }
    }

    // ── Admin yolu ───────────────────────────────────────────────────────────────────────────

    /**
     * Elanı yazır — eyni (il, ay) cütü varsa **üzərinə**.
     *
     * Əvvəl `update`, sətir toxunmayıbsa `insert`. Tək `upsert` daha qısa olardı, amma id-nin
     * **dəyişməməsi** burada davranışdır: cihazlar «yeni elan gəldi» qərarını id ilə verir və hər
     * dəfə yeni id görsəydilər adminin kiçik bir qeyd düzəlişi də istifadəçilərin −2/+2 seçimini
     * bütün telefonlarda sıfırlayardı.
     *
     * RLS bloklayanda PostgREST xəta yox, **boş nəticə** qaytarır — ona görə hər iki sorğu
     * `select()` ilə gedir və təsirlənən sətir sayı qaytarılır (CLAUDE.md, `EditsViewModel`).
     */
    suspend fun upsert(
        hijriYear: Int,
        hijriMonth: Int,
        startDate: String,
        lengthDays: Int,
        sightedAt: String?,
        media: List<SuggestionMedia>,
        note: String?,
    ): Int = withContext(Dispatchers.IO) {
        val updated = SupabaseProvider.client.from(TABLE)
            .update({
                set("start_date", startDate)
                set("length_days", lengthDays)
                set("sighted_at", sightedAt)
                set("media", encodeMedia(media))
                set("note", note)
            }) {
                select()
                filter {
                    eq("hijri_year", hijriYear)
                    eq("hijri_month", hijriMonth)
                }
            }
            .decodeList<JsonObject>().size

        if (updated > 0) return@withContext updated

        val payload = buildJsonObject {
            put("hijri_year", hijriYear)
            put("hijri_month", hijriMonth)
            put("start_date", startDate)
            put("length_days", lengthDays)
            put("sighted_at", sightedAt)
            put("media", encodeMedia(media))
            put("note", note)
        }

        SupabaseProvider.client.from(TABLE)
            .insert(payload) { select() }
            .decodeList<JsonObject>().size
    }

    /**
     * Hekayənin media siyahısı — boş siyahı göndərmək bütün mediaları götürür.
     *
     * Media elanın **özündən ayrı** yenilənir: video yükləmək saniyələr çəkir, ayın tarixi isə
     * elan olunan kimi lazımdır. Ona görə admin əvvəl faktları yayımlayır, videonu sonra qoşur.
     */
    suspend fun updateMedia(id: Long, media: List<SuggestionMedia>): Int =
        withContext(Dispatchers.IO) {
            SupabaseProvider.client.from(TABLE)
                .update({ set("media", encodeMedia(media)) }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<JsonObject>().size
        }

    suspend fun delete(id: Long): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    /**
     * 12 aydan köhnə elanları **fayllarıyla birlikdə** götürür; silinən sətir sayını qaytarır.
     *
     * Ayrıca cron qurmaq əvəzinə admin yeni elan yayımlayanda çağırılır — təmizləmə elanın özü ilə
     * eyni ritmdə (ayda bir) gedir.
     */
    suspend fun prune(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.postgrest
                .rpc(function = "prune_lunar_announcements")
                .decodeAs<Int>()
        }.onFailure { AppLogger.d(TAG, "Prune failed: ${it.message}") }
    }

    private fun encodeMedia(media: List<SuggestionMedia>) = JsonArray(
        media.map { item ->
            buildJsonObject {
                put("url", item.url)
                put("type", item.type)
            }
        },
    )

    companion object {
        /** Hekayədə göstərilən və serverdə saxlanılan ay sayı. */
        const val HISTORY_MONTHS = 12

        private const val TABLE = "lunar_announcement"
        private const val TAG = "LunarAnnouncement"
    }
}
