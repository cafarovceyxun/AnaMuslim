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
 * **Dəlillər iki yoldan gəlir:**
 * - [fetchEvidence] — bir ad, ani yol. Ad açılanda çağırılır ki, istifadəçi gözləməsin.
 * - [fetchAllEvidence] — hamısı, arxa fonda. Ekran bir dəfə açılandan sonra tətbiq oflayn qalsa da
 *   **açılmamış** adların dəlilləri əldə olsun.
 *
 * ⚠️ Hər ikisi [fetchAllPages] ilə səhifələnir. PostgREST bir cavabda **1000 sətir** verir və limitə
 * dəyən sorğu xəta yox, sadəcə qısa cavab qaytarır — yəni artıq məzmun səssizcə yoxa çıxardı.
 *
 * Siyahıdakı say nişanı `asma_evidence_count` **view**-undan gəlir (ən çox 99 sətir), sətirləri
 * çəkib sayaraq yox: nişan 99 ad üçün birdən lazımdır, dəlillərin özü isə hələ gəlməmiş ola bilər.
 */
class AsmaRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val nameSerializer = ListSerializer(AsmaName.serializer())
    private val evidenceSerializer = ListSerializer(AsmaEvidence.serializer())
    private val countSerializer = MapSerializer(Int.serializer(), Int.serializer())
    private val cacheSerializer = MapSerializer(Int.serializer(), evidenceSerializer)
    private val hiddenSerializer = ListSerializer(AutoHiddenRow.serializer())

    /** `asma_evidence_count` view-unun sətri. */
    @Serializable
    private data class EvidenceCount(val name_no: Int, val evidence_count: Int)

    /**
     * `asma_auto_hidden` sətri.
     *
     * `hidden_by`/`created_at` **qəsdən yoxdur**: ikisini də baza doldurur (`auth.uid()`, `now()`),
     * klientdən göndərilsə INSERT siyasətindən keçib sahibliyi saxtalaşdırmaq olardı.
     */
    @Serializable
    private data class AutoHiddenRow(
        val name_no: Int,
        val chapter_no: Int,
        val verse_no: Int,
    )

    /**
     * 99 ad, **`sort_no` sırası** ilə. Sətir sayı sabitdir, ona görə səhifələmə lazım deyil.
     *
     * `no` ikinci açardır: bərabər `sort_no`-lu adlar (məsələn, sıra hələ dəyişdirilməyib və hamısı
     * defolt dəyərdədir) ənənəvi nömrə sırasında qalsın deyə.
     */
    suspend fun fetchNames(): List<AsmaName> = withContext(Dispatchers.IO) {
        try {
            val items = SupabaseProvider.client.from(TABLE_NAME)
                .select {
                    order("sort_no", Order.ASCENDING)
                    order("no", Order.ASCENDING)
                }
                .decodeList<AsmaName>()
                .sortedWith(compareBy({ it.sort_no }, { it.no }))

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

    /**
     * **Bütün** adların dəlilləri, bir keçiddə — oflayn istifadə üçün.
     *
     * Ekran bir dəfə açılanda çağırılır ki, şəbəkə kəsiləndən sonra istifadəçi **açmadığı** adların
     * da dəlillərini görsün. [fetchEvidence] ada görə yükləməyə davam edir: ad açılanda gözləmə
     * olmasın deyə ani yol odur, bu isə arxa fonda keşi tamamlayır.
     *
     * Sıralama üç sütunludur — `range()` ilə səhifələnən sorğuda sıra qeyri-müəyyən olsa sətirlər
     * səhifələr arasında sürüşüb təkrarlana və ya düşə bilər.
     *
     * @return uğurda ad → dəlillər xəritəsi (keşə də yazılır); şəbəkə xətasında `failure` —
     *   çağıran tərəf o zaman «hamısı yükləndi» bayrağını qaldırmamalıdır.
     */
    suspend fun fetchAllEvidence(): Result<Map<Int, List<AsmaEvidence>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val grouped = fetchAllPages { from, to ->
                    SupabaseProvider.client.from(TABLE_EVIDENCE)
                        .select {
                            order("name_no", Order.ASCENDING)
                            order("sort_no", Order.ASCENDING)
                            order("id", Order.ASCENDING)
                            range(from, to)
                        }
                        .decodeList<AsmaEvidence>()
                }
                    .groupBy { it.name_no }
                    .mapValues { (_, items) -> items.inQuranOrder() }

                DuaPreferences.setAsmaEvidenceCache(
                    json.encodeToString(cacheSerializer, grouped),
                )
                grouped
            }
        }

    /**
     * Admin tərəfindən gizlədilmiş avtomatik uyğunluqlar: ad → `(surə, ayə)` dəsti.
     *
     * Cədvəl böyüyə bilər (99 ad × onlarla səhv uyğunluq), ona görə səhifələnir — PostgREST bir
     * cavabda 1000 sətir verir və limitə dəyən sorğu xəta yox, **qısa cavab** qaytarır.
     */
    suspend fun fetchAutoHidden(): Map<Int, Set<Pair<Int, Int>>> = withContext(Dispatchers.IO) {
        try {
            val rows = fetchAllPages { from, to ->
                SupabaseProvider.client.from(TABLE_AUTO_HIDDEN)
                    .select {
                        order("name_no", Order.ASCENDING)
                        order("chapter_no", Order.ASCENDING)
                        order("verse_no", Order.ASCENDING)
                        range(from, to)
                    }
                    .decodeList<AutoHiddenRow>()
            }

            DuaPreferences.setAsmaHiddenCache(json.encodeToString(hiddenSerializer, rows))
            rows.toHiddenMap()
        } catch (e: Exception) {
            cachedAutoHidden()
        }
    }

    /** Gizlətmə qərarlarının oflayn keşi — bax [fetchAutoHidden]. */
    fun cachedAutoHidden(): Map<Int, Set<Pair<Int, Int>>> =
        decodeOr(DuaPreferences.getAsmaHiddenCache(), hiddenSerializer, emptyList()).toHiddenMap()

    /**
     * Bir avtomatik uyğunluğu gizlədir — yalnız admin (qapı bazadadır, RLS).
     *
     * ⚠️ Yazma `select()` ilə gedir və qaytarılan sətir sayı yoxlanılır: RLS bir əməliyyatı
     * bloklayanda PostgREST **xəta yox, boş nəticə** qaytarır, yəni admin olmayan istifadəçi
     * «gizlətdim» görüb heç nə dəyişməmiş olardı (CLAUDE.md qaydası).
     */
    suspend fun hideAuto(nameNo: Int, chapterNo: Int, verseNo: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val inserted = SupabaseProvider.client.from(TABLE_AUTO_HIDDEN)
                    .insert(
                        AutoHiddenRow(name_no = nameNo, chapter_no = chapterNo, verse_no = verseNo),
                    ) { select() }
                    .decodeList<AutoHiddenRow>()

                if (inserted.isEmpty()) throw IllegalStateException("Sətir yazılmadı (RLS?)")
            }
        }

    /** Gizlədilmiş uyğunluğu geri qaytarır — bax [hideAuto]. */
    suspend fun unhideAuto(nameNo: Int, chapterNo: Int, verseNo: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseProvider.client.from(TABLE_AUTO_HIDDEN).delete {
                    filter {
                        eq("name_no", nameNo)
                        eq("chapter_no", chapterNo)
                        eq("verse_no", verseNo)
                    }
                }

                // `delete` sətir qaytarmır, ona görə silinib-silinmədiyi ayrıca yoxlanılır —
                // RLS bloklasaydı sətir yerində qalardı və ekran «göstərildi» deyərdi.
                val stillThere = SupabaseProvider.client.from(TABLE_AUTO_HIDDEN)
                    .select {
                        filter {
                            eq("name_no", nameNo)
                            eq("chapter_no", chapterNo)
                            eq("verse_no", verseNo)
                        }
                    }
                    .decodeList<AutoHiddenRow>()
                    .isNotEmpty()

                if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
            }
        }

    private fun List<AutoHiddenRow>.toHiddenMap(): Map<Int, Set<Pair<Int, Int>>> =
        groupBy { it.name_no }
            .mapValues { (_, rows) -> rows.mapTo(mutableSetOf()) { it.chapter_no to it.verse_no } }

    /** Keşdəki bütün dəlillər — soyuq açılışda ekranı dərhal doldurmaq üçün. */
    fun cachedAllEvidence(): Map<Int, List<AsmaEvidence>> =
        decodeOr(DuaPreferences.getAsmaEvidenceCache(), cacheSerializer, emptyMap())
            .mapValues { (_, items) -> items.inQuranOrder() }

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

    /**
     * Adların yeni sırasını yazır — [changes] yalnız **dəyişən** sətirlərdir (`no` → yeni `sort_no`).
     *
     * `no` sütununa toxunulmur: o, PK və `asma_evidence.name_no`-nun hədəfidir, dəyişsə dəlillər
     * qoparardı. Qayda `DuaRepository.updateCategoryOrder` ilə eynidir — yalnız dəyişənlər gedir.
     */
    suspend fun updateNameOrder(changes: List<Pair<Int, Int>>): Result<Unit> =
        withContext(Dispatchers.IO) {
            writeSortOrder(changes) { no, sortNo ->
                SupabaseProvider.client.from(TABLE_NAME)
                    .update({ set("sort_no", sortNo) }) {
                        select()
                        filter { eq("no", no) }
                    }
                    .decodeList<AsmaName>()
                    .isNotEmpty()
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
     * Bir adın dəlillərini keşə yazır — **qalan adlara toxunmadan**.
     *
     * ⚠️ Burada LRU kəsimi yoxdur və olmamalıdır. Əvvəl keş son 20 adla məhdudlaşırdı; [fetchAllEvidence]
     * gələndən sonra bu, oflayn dəstəyi sındırırdı: bir dəlil redaktə olunan kimi kəsim işə düşüb
     * qalan 79 adı keşdən atırdı və istifadəçi şəbəkəsiz qalanda onları boş görürdü. Dəst 99 adla
     * məhduddur, yəni sətir sərbəst böyümür.
     */
    private suspend fun cacheEvidence(nameNo: Int, items: List<AsmaEvidence>) {
        val current = decodeOr(
            DuaPreferences.getAsmaEvidenceCache(),
            cacheSerializer,
            emptyMap(),
        )

        val updated = current + (nameNo to items)

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
        const val TABLE_AUTO_HIDDEN = "asma_auto_hidden"
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
