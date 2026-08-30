package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionSubmissionRow
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionTicket
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Təkliflərin şəbəkə tərəfi.
 *
 * İstifadəçi yolu **yalnız üç RPC**-dən keçir (`submit_suggestion`, `get_suggestion_tickets`,
 * `vote_suggestion`) — `anon` rolunun `suggestion_submissions` cədvəlinə heç bir icazəsi yoxdur,
 * `suggestions` cədvəlində isə yalnız SELECT var. Admin yolu adi cədvəl sorğularıdır; RLS onları
 * e-poçta görə süzür.
 */
class SuggestionRepository {

    // ── İstifadəçi yolu ──────────────────────────────────────────────────────────────────────

    /** Təsdiqlənmiş təkliflər — çox səs toplayan öndə, bərabərlikdə yenisi öndə. */
    suspend fun fetchApproved(): List<Suggestion> = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_PUBLIC)
            .select {
                order("vote_count", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
                limit(200)
            }
            .decodeList<Suggestion>()
    }

    /** Göndərir və qəbzi qaytarır — cihaz onu saxlayır ki, sonra statusu soruşa bilsin. */
    suspend fun submit(
        body: String,
        category: String,
        appVersion: String?,
        platform: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                function = "submit_suggestion",
                parameters = buildJsonObject {
                    put("p_body", body)
                    put("p_category", category)
                    put("p_app_version", appVersion)
                    put("p_platform", platform)
                },
            ).decodeAs<String>()
        }
    }

    /** Cihazdakı qəbzlərin cari vəziyyəti. Qəbz siyahısı boşdursa şəbəkəyə çıxılmır. */
    suspend fun fetchTickets(tickets: List<String>): List<SuggestionTicket> =
        withContext(Dispatchers.IO) {
            if (tickets.isEmpty()) return@withContext emptyList()

            SupabaseProvider.client.postgrest.rpc(
                function = "get_suggestion_tickets",
                parameters = buildJsonObject {
                    put("p_tickets", JsonArray(tickets.map(::JsonPrimitive)))
                },
            ).decodeList<SuggestionTicket>()
        }

    /** [delta] yalnız `+1` və ya `-1` ola bilər; nəticə səsin yeni sayıdır. */
    suspend fun vote(suggestionId: Long, delta: Int): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                function = "vote_suggestion",
                parameters = buildJsonObject {
                    put("p_suggestion_id", suggestionId)
                    put("p_delta", delta)
                },
            ).decodeAs<Int>()
        }
    }

    /**
     * Baxış sayğacını artırır və yeni sayı qaytarır. Yalnız **ilk baxışda** çağırılır — hekayənin
     * baxılma vəziyyəti cihazdadır, ona görə sayğac təxminidir.
     */
    suspend fun markViewed(suggestionId: Long): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                function = "mark_suggestion_viewed",
                parameters = buildJsonObject { put("p_suggestion_id", suggestionId) },
            ).decodeAs<Int>()
        }
    }

    // ── Admin yolu ───────────────────────────────────────────────────────────────────────────

    suspend fun fetchSubmissions(): List<SuggestionSubmissionRow> = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_QUEUE)
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<SuggestionSubmissionRow>()
    }

    /**
     * RLS bir əməliyyatı bloklayanda PostgREST xəta yox, boş nəticə qaytarır — ona görə bütün
     * admin əməliyyatları `select()` ilə gedir və təsirlənən sətir sayını qaytarır. Sıfır qayıdırsa
     * yazma səssizcə düşüb (bax `EditsViewModel` nümunəsi).
     */
    suspend fun updateSubmissionStatus(id: Long, status: String): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_QUEUE)
            .update({ set("status", status) }) {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    /** Təsdiqdən əvvəl mətnin redaktəsi — trigger `suggestions`-a məhz bu mətni köçürür. */
    suspend fun updateSubmissionContent(
        id: Long,
        body: String,
        category: String,
        adminNote: String?,
    ): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_QUEUE)
            .update({
                set("body", body)
                set("category", category)
                set("admin_note", adminNote)
            }) {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    suspend fun deleteSubmission(id: Long): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_QUEUE)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    /** İctimai siyahıdakı sətrin iş vəziyyəti: açıq / planlaşdırılıb / tamamlandı. */
    suspend fun updatePublicStatus(id: Long, status: String): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_PUBLIC)
            .update({ set("status", status) }) {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    /** Hekayənin media siyahısı — boş siyahı göndərmək bütün mediaları götürür. */
    suspend fun updatePublicMedia(id: Long, media: List<SuggestionMedia>): Int =
        withContext(Dispatchers.IO) {
            val encoded = JsonArray(
                media.map { item ->
                    buildJsonObject {
                        put("url", item.url)
                        put("type", item.type)
                    }
                },
            )

            SupabaseProvider.client.from(TABLE_PUBLIC)
                .update({ set("media", encoded) }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<JsonObject>().size
        }

    /** Hekayədə görünən admin qeydi (`null` → qeyd götürülür). */
    suspend fun updatePublicNote(id: Long, note: String?): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_PUBLIC)
            .update({ set("note", note) }) {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    suspend fun deletePublic(id: Long): Int = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE_PUBLIC)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeList<JsonObject>().size
    }

    private companion object {
        const val TABLE_PUBLIC = "suggestions"
        const val TABLE_QUEUE = "suggestion_submissions"
    }
}
