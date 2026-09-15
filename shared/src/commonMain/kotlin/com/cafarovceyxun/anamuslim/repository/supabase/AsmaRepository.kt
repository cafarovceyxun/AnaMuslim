package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaName
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json

/**
 * Əsmaül Hüsnə — 99 ad (`asma_name`) və onlara bağlanmış dəlillər (`asma_evidence`).
 *
 * Adların özü **sabitdir**: siyahı miqrasiya ilə yazılıb, yazma icazəsi yalnız admindədir. Redaktor
 * bura yalnız dəlil əlavə edir.
 *
 * ⚠️ **Dəlillər ada görə, ayrıca yüklənir** — hamısı birdən yox. İki səbəb:
 * 1. PostgREST bir sorğuda **1000 sətir** qaytarır (yoxlanıldı). Bir ada çox dəlil düşəcəyi üçün
 *    ümumi say bu həddi keçəcək və sonrakı adların dəlilləri **səssizcə** yoxa çıxardı — nə xəta,
 *    nə boş siyahı, sadəcə əskik məzmun.
 * 2. Hamısını hər ekran açılışında çəkmək mobil internetdə lazımsız yükdür: istifadəçi bir anda
 *    bir ada baxır.
 *
 * Siyahıdakı say nişanı elə buna görə `asma_evidence_count` **view**-undan gəlir (ən çox 99 sətir),
 * sətirləri sayaraq yox.
 */
class AsmaRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val nameSerializer = ListSerializer(AsmaName.serializer())
    private val evidenceSerializer = ListSerializer(AsmaEvidence.serializer())
    private val countSerializer = MapSerializer(Int.serializer(), Int.serializer())
    private val cacheSerializer = MapSerializer(Int.serializer(), evidenceSerializer)

    /** `asma_evidence_count` view-unun sətri. */
    @Serializable
    private data class EvidenceCount(val name_no: Int, val evidence_count: Int)

    /** 99 ad, nömrə sırası ilə. Sətir sayı sabitdir, ona görə səhifələmə lazım deyil. */
    suspend fun fetchNames(): List<AsmaName> = withContext(Dispatchers.IO) {
        try {
            val items = SupabaseProvider.client.from(TABLE_NAME)
                .select { order("no", Order.ASCENDING) }
                .decodeList<AsmaName>()
                .sortedBy { it.no }

            DuaPreferences.setAsmaNamesCache(json.encodeToString(nameSerializer, items))
            items
        } catch (e: Exception) {
            cachedNames()
        }
    }

    /** Ad nömrəsi → dəlil sayı. Sətirlər view-dandır, ona görə 1000 həddinə dəymir. */
    suspend fun fetchEvidenceCounts(): Map<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val counts = SupabaseProvider.client.from(VIEW_COUNT)
                .select()
                .decodeList<EvidenceCount>()
                .associate { it.name_no to it.evidence_count }

            DuaPreferences.setAsmaCountsCache(json.encodeToString(countSerializer, counts))
            counts
        } catch (e: Exception) {
            cachedCounts()
        }
    }

    /**
     * **Bir** adın bütün dəlilləri, sıralı.
     *
     * Səhifə-səhifə oxunur: tək bir adın dəlilləri də 1000-i keçə bilər, keçəndə isə sonu səssizcə
     * kəsilərdi. Dövrə tam olmayan səhifə gələndə dayanır.
     */
    suspend fun fetchEvidence(nameNo: Int): List<AsmaEvidence> = withContext(Dispatchers.IO) {
        try {
            val items = fetchAllPages { from, to ->
                SupabaseProvider.client.from(TABLE_EVIDENCE)
                    .select {
                        filter { eq("name_no", nameNo) }
                        order("sort_no", Order.ASCENDING)
                        range(from, to)
                    }
                    .decodeList<AsmaEvidence>()
            }.inQuranOrder()

            cacheEvidence(nameNo, items)
            items
        } catch (e: Exception) {
            cachedEvidence(nameNo)
        }
    }

    fun cachedNames(): List<AsmaName> =
        decodeOr(DuaPreferences.getAsmaNamesCache(), nameSerializer, emptyList())

    fun cachedCounts(): Map<Int, Int> =
        decodeOr(DuaPreferences.getAsmaCountsCache(), countSerializer, emptyMap())

    fun cachedEvidence(nameNo: Int): List<AsmaEvidence> =
        decodeOr(DuaPreferences.getAsmaEvidenceCache(), cacheSerializer, emptyMap())[nameNo]
            .orEmpty()
            // Keş köhnə (sıralanmamış) buraxılışdan qalmış ola bilər — sıra hər halda burada qurulur.
            .inQuranOrder()

    suspend fun addEvidence(evidence: AsmaEvidence): Result<AsmaEvidence> =
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseProvider.client.from(TABLE_EVIDENCE)
                    .insert(evidence.copy(id = null)) { select() }
                    .decodeList<AsmaEvidence>()
                    .firstOrNull()
                    ?: throw IllegalStateException("Sətir yazılmadı (RLS?)")
            }.recoverCatching { throw mapWriteError(it) }
        }

    /**
     * Adın mətnlərini və görünüşünü yeniləyir — yalnız admin (RLS).
     *
     * `no` dəyişmir: o, həm PK, həm də dəlillərin xarici açarıdır.
     */
    suspend fun updateName(name: AsmaName): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updated = SupabaseProvider.client.from(TABLE_NAME)
                .update({
                    set("name_ar", name.name_ar)
                    set("transliteration", name.transliteration)
                    set("meaning", name.meaning)
                    // İzahı təmizləmək mümkün olsun deyə null da açıq göndərilir.
                    set("description", name.description)
                    set("is_visible", name.is_visible)
                }) {
                    select()
                    filter { eq("no", name.no) }
                }
                .decodeList<AsmaName>()

            if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
        }
    }

    /** Dəlilin mətnini/qeydini yeniləyir; mənbəyi dəyişmir (bax `DuaRepository.updateDua`). */
    suspend fun updateEvidence(evidence: AsmaEvidence): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val id = evidence.id ?: throw IllegalArgumentException("id yoxdur")

            val updated = SupabaseProvider.client.from(TABLE_EVIDENCE)
                .update({
                    set("text_ar", evidence.text_ar)
                    set("text_az", evidence.text_az)
                    set("transliteration", evidence.transliteration)
                    set("note", evidence.note)
                    set("source", evidence.source)
                    set("sort_no", evidence.sort_no)
                }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<AsmaEvidence>()

            if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
        }
    }

    suspend fun deleteEvidence(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE_EVIDENCE).delete { filter { eq("id", id) } }

            val stillThere = SupabaseProvider.client.from(TABLE_EVIDENCE)
                .select { filter { eq("id", id) } }
                .decodeList<AsmaEvidence>()
                .isNotEmpty()

            if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
        }
    }

    /**
     * Bir adın dəlillərini keşə yazır.
     *
     * Keş **son [CACHED_NAMES] ada** qədər saxlanılır: hamısını saxlamaq DataStore sətrini
     * məhdudiyyətsiz böyüdərdi, oflayn isə praktikada yaxınlarda baxılan adlar lazım olur.
     */
    private suspend fun cacheEvidence(nameNo: Int, items: List<AsmaEvidence>) {
        val current = decodeOr(
            DuaPreferences.getAsmaEvidenceCache(),
            cacheSerializer,
            emptyMap(),
        )

        // `LinkedHashMap` sırası əlavə olunma sırasıdır: açarı əvvəlcə atıb sonra yazmaq onu sona
        // gətirir, yəni ən köhnə giriş həmişə başda qalır.
        val updated = LinkedHashMap(current)
        updated.remove(nameNo)
        updated[nameNo] = items

        while (updated.size > CACHED_NAMES) {
            val oldest = updated.keys.firstOrNull() ?: break
            updated.remove(oldest)
        }

        DuaPreferences.setAsmaEvidenceCache(json.encodeToString(cacheSerializer, updated))
    }

    private fun <T> decodeOr(
        cached: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        fallback: T,
    ): T {
        if (cached.isBlank()) return fallback
        return runCatching { json.decodeFromString(serializer, cached) }.getOrDefault(fallback)
    }

    private companion object {
        const val TABLE_NAME = "asma_name"
        const val TABLE_EVIDENCE = "asma_evidence"
        const val VIEW_COUNT = "asma_evidence_count"
        const val CACHED_NAMES = 20
    }
}

/**
 * Dəlillərin sırası — **Quran ardıcıllığı**.
 *
 * Əvvəl ayələr (surə nömrəsi, sonra ayə nömrəsi), sonra hədislər. Ardıcıllıq əlavə olunma sırası
 * ilə gəlsəydi eyni adın dəlilləri təsadüfi düzülərdi: redaktor Bəqərədən sonra Fatihəni əlavə edə
 * bilər, oxucu isə mushaf sırası gözləyir.
 *
 * Hədislər sonda: onların Quran mövqeyi yoxdur, «əvvəl Quran, sonra Sünnə» isə adi sıralamadır.
 * `sort_no` və `id` yalnız bərabərlikdə həll edicidir — əl ilə sıralama hələ yoxdur.
 */
internal fun List<AsmaEvidence>.inQuranOrder(): List<AsmaEvidence> = sortedWith(
    compareBy<AsmaEvidence> { if (it.isQuran) 0 else 1 }
        .thenBy { it.chapter_no ?: Int.MAX_VALUE }
        .thenBy { it.verse_no ?: Int.MAX_VALUE }
        .thenBy { it.hadith_id ?: Long.MAX_VALUE }
        .thenBy { it.sort_no }
        .thenBy { it.id ?: 0L },
)
