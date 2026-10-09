package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.StoryAnnouncement
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Müstəqil hekayələrin şəbəkə tərəfi (`story_announcement`).
 *
 * Oxu hamıya açıqdır, amma RLS **vaxtı keçmiş** sətri adi istifadəçidən gizlədir; admin hamısını
 * görür (idarəetmə siyahısı üçün), ona görə ana ekran siyahını əlavə olaraq klientdə də süzür
 * ([StoryAnnouncement.isActive]). Yazma RLS ilə adminə bağlıdır.
 */
class StoryAnnouncementRepository {

    /** Yenisi öndə. Şəbəkə çatmasa boş siyahı — hekayə zolağı sadəcə bu qrupu göstərmir. */
    suspend fun fetch(): List<StoryAnnouncement> = withContext(Dispatchers.IO) {
        try {
            SupabaseProvider.client.from(TABLE)
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(MAX_ROWS.toLong())
                }
                .decodeList<StoryAnnouncement>()
        } catch (e: Exception) {
            AppLogger.d(TAG, "Fetch failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Baxış sayğacı — yalnız ilk baxışda çağırılır, baxılma vəziyyəti cihazdadır; qəməri və
     * funksiya hekayələri ilə eyni naxış. Uğursuzluq səssiz keçir.
     */
    suspend fun markViewed(id: Long): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                function = "increment_story_announcement_view",
                parameters = buildJsonObject { put("p_id", id) },
            ).decodeAs<Int>()
        }.onFailure { AppLogger.d(TAG, "View count failed: ${it.message}") }
    }

    // ── Admin yolu ───────────────────────────────────────────────────────────────────────────

    /**
     * Yeni hekayə. RLS bloklayanda PostgREST xəta yox, **boş nəticə** qaytarır — ona görə sorğu
     * `select()` ilə gedir və yaradılan sətir qaytarılır (yoxdursa `null`, CLAUDE.md).
     */
    suspend fun create(
        note: String?,
        media: List<SuggestionMedia>,
        expiresAt: String?,
    ): StoryAnnouncement? = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("note", note)
            put("media", encodeMedia(media))
            put("expires_at", expiresAt)
        }

        SupabaseProvider.client.from(TABLE)
            .insert(payload) { select() }
            .decodeList<StoryAnnouncement>()
            .firstOrNull()
    }

    suspend fun delete(id: Long): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    private fun encodeMedia(media: List<SuggestionMedia>) = JsonArray(
        media.map { item ->
            buildJsonObject {
                put("url", item.url)
                put("type", item.type)
            }
        },
    )

    private companion object {
        /** Zolaq və idarəetmə siyahısı üçün kifayətdir; köhnəsi admin tərəfindən silinir. */
        const val MAX_ROWS = 50
        const val TABLE = "story_announcement"
        const val TAG = "StoryAnnouncement"
    }
}
