package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.utils.supabase.HajjEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Həcc və Ümrə bələdçisinin dəlilləri və zikrləri (`hajj_evidence`).
 *
 * Dəst kiçikdir (17 mövzu, hər birində bir neçə sətir), ona görə hamısı bir keçiddə oxunur və
 * bütöv keşlənir — bələdçi şəbəkəsiz yerdə (Ərəfat, Mina) açıla bilməlidir. Yenə də [fetchAllPages]
 * ilə: PostgREST bir cavabda 1000 sətir verir və limitə dəyən sorğu xəta yox, **qısa cavab** qaytarır.
 *
 * Yazma qaydaları `AsmaRepository` ilə eynidir: RLS bloklayanda PostgREST xəta yox, boş nəticə
 * qaytarır, ona görə hər yazma `select()` ilə gedir və təsirlənən sətir yoxlanır.
 */
class HajjRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(HajjEvidence.serializer())

    /** Hamısı, mövzu → növ → sıra ilə; uğurda keşə də yazılır. */
    suspend fun fetchAll(): Result<List<HajjEvidence>> = withContext(Dispatchers.IO) {
        runCatching {
            val items = fetchAllPages { from, to ->
                SupabaseProvider.client.from(TABLE)
                    .select {
                        order("topic", Order.ASCENDING)
                        order("sort_no", Order.ASCENDING)
                        order("id", Order.ASCENDING)
                        range(from, to)
                    }
                    .decodeList<HajjEvidence>()
            }
            DuaPreferences.setHajjEvidenceCache(json.encodeToString(serializer, items))
            items
        }
    }

    fun cached(): List<HajjEvidence> {
        val raw = DuaPreferences.getHajjEvidenceCache()
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    suspend fun add(evidence: HajjEvidence): Result<HajjEvidence> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE)
                .insert(evidence.copy(id = null)) { select() }
                .decodeList<HajjEvidence>()
                .firstOrNull()
                ?: throw IllegalStateException("Sətir yazılmadı (RLS?)")
        }.recoverCatching { throw mapWriteError(it) }
    }

    /** Mətnləri, qeydi və sıranı yeniləyir; mənbə və mövzu dəyişmir (bax `AsmaRepository.updateEvidence`). */
    suspend fun update(evidence: HajjEvidence): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val id = evidence.id ?: throw IllegalArgumentException("id yoxdur")

            val updated = SupabaseProvider.client.from(TABLE)
                .update({
                    set("text_ar", evidence.text_ar)
                    set("text_az", evidence.text_az)
                    set("transliteration", evidence.transliteration)
                    set("note", evidence.note)
                    set("sort_no", evidence.sort_no)
                }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<HajjEvidence>()

            if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
        }.recoverCatching { throw mapWriteError(it) }
    }

    suspend fun delete(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE).delete { filter { eq("id", id) } }

            val stillThere = SupabaseProvider.client.from(TABLE)
                .select { filter { eq("id", id) } }
                .decodeList<HajjEvidence>()
                .isNotEmpty()

            if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
        }
    }

    private companion object {
        const val TABLE = "hajj_evidence"
    }
}
