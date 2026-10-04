package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.utils.supabase.GuideEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Bələdçinin dəlilləri və zikrləri — bir cədvəl, bir keş.
 *
 * İki bələdçi eyni formanı işlədir: Həcc və Ümrə (`hajj_evidence`) və Namaz (`salah_evidence`).
 * Hər birinin dəsti kiçikdir (bir neçə onlarla sətir), ona görə hamısı bir keçiddə oxunur və bütöv
 * keşlənir — bələdçi şəbəkəsiz yerdə də (Ərəfat, Mina, səfər) açılmalıdır. Yenə də [fetchAllPages] ilə:
 * PostgREST bir cavabda 1000 sətir verir və limitə dəyən sorğu xəta yox, **qısa cavab** qaytarır.
 *
 * Yazma qaydaları `AsmaRepository` ilə eynidir: RLS bloklayanda PostgREST xəta yox, boş nəticə
 * qaytarır, ona görə hər yazma `select()` ilə gedir və təsirlənən sətir yoxlanır.
 */
class GuideEvidenceRepository(
    private val table: String,
    private val readCache: () -> String,
    private val writeCache: suspend (String) -> Unit,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(GuideEvidence.serializer())

    /** Hamısı, mövzu → növ → sıra ilə; uğurda keşə də yazılır. */
    suspend fun fetchAll(): Result<List<GuideEvidence>> = withContext(Dispatchers.IO) {
        runCatching {
            val items = fetchAllPages { from, to ->
                SupabaseProvider.client.from(table)
                    .select {
                        order("topic", Order.ASCENDING)
                        order("sort_no", Order.ASCENDING)
                        order("id", Order.ASCENDING)
                        range(from, to)
                    }
                    .decodeList<GuideEvidence>()
            }
            writeCache(json.encodeToString(serializer, items))
            items
        }
    }

    fun cached(): List<GuideEvidence> {
        val raw = readCache()
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    suspend fun add(evidence: GuideEvidence): Result<GuideEvidence> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(table)
                .insert(evidence.copy(id = null)) { select() }
                .decodeList<GuideEvidence>()
                .firstOrNull()
                ?: throw IllegalStateException("Sətir yazılmadı (RLS?)")
        }.recoverCatching { throw mapWriteError(it) }
    }

    /** Mətnləri, qeydi və sıranı yeniləyir; mənbə və mövzu dəyişmir (bax `AsmaRepository.updateEvidence`). */
    suspend fun update(evidence: GuideEvidence): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val id = evidence.id ?: throw IllegalArgumentException("id yoxdur")

            val updated = SupabaseProvider.client.from(table)
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
                .decodeList<GuideEvidence>()

            if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
        }.recoverCatching { throw mapWriteError(it) }
    }

    suspend fun delete(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(table).delete { filter { eq("id", id) } }

            val stillThere = SupabaseProvider.client.from(table)
                .select { filter { eq("id", id) } }
                .decodeList<GuideEvidence>()
                .isNotEmpty()

            if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
        }
    }
}

/** Həcc və Ümrə bələdçisi — `hajj_evidence`. */
@Suppress("FunctionName")
fun HajjRepository() = GuideEvidenceRepository(
    table = "hajj_evidence",
    readCache = { DuaPreferences.getHajjEvidenceCache() },
    writeCache = { DuaPreferences.setHajjEvidenceCache(it) },
)

/** Namaz bələdçisi — `salah_evidence` (2026-10-04). */
@Suppress("FunctionName")
fun SalahRepository() = GuideEvidenceRepository(
    table = "salah_evidence",
    readCache = { DuaPreferences.getSalahEvidenceCache() },
    writeCache = { DuaPreferences.setSalahEvidenceCache(it) },
)
