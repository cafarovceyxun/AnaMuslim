package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaCategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSubcategory
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Dua başlıqları (`dua_category`) və duaların özü (`dua`).
 *
 * Oxu hamıya açıqdır, yazma isə yalnız giriş etmiş istifadəçiyə — **və yalnız öz sətrinə** (admin
 * hamısına). Qapı bazadadır, klientdə admin yoxlaması yoxdur.
 *
 * ⚠️ RLS bir əməliyyatı bloklayanda PostgREST xəta yox, **boş nəticə** qaytarır (CLAUDE.md), ona
 * görə hər yazma `select()` ilə gedir və qayıdan sətir sayı yoxlanılır — əks halda əməliyyat
 * uğurlu görünər, amma heç nə dəyişməzdi.
 */
class DuaRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val categorySerializer = ListSerializer(DuaCategory.serializer())
    private val duaSerializer = ListSerializer(Dua.serializer())
    private val subcategorySerializer = ListSerializer(DuaSubcategory.serializer())

    /**
     * Bütün başlıqlar, sıralı.
     *
     * Uğurlu cavab keşlənir; şəbəkə yoxdursa keşdən qaytarılır, ona görə oflayn cihazda bölmə boş
     * açılmır.
     */
    suspend fun fetchCategories(): List<DuaCategory> = withContext(Dispatchers.IO) {
        try {
            val items = fetchAllPages { from, to ->
                SupabaseProvider.client.from(TABLE_CATEGORY)
                    .select {
                        order("sort_no", Order.ASCENDING)
                        range(from, to)
                    }
                    .decodeList<DuaCategory>()
            }.sortedWith(compareBy({ it.sort_no }, { it.name }))

            DuaPreferences.setCategoriesCache(json.encodeToString(categorySerializer, items))
            items
        } catch (e: Exception) {
            cachedCategories()
        }
    }

    /** Bütün alt başlıqlar; ekran onları başlığa görə qruplaşdırır. */
    suspend fun fetchSubcategories(): List<DuaSubcategory> = withContext(Dispatchers.IO) {
        try {
            val items = fetchAllPages { from, to ->
                SupabaseProvider.client.from(TABLE_SUBCATEGORY)
                    .select {
                        order("sort_no", Order.ASCENDING)
                        range(from, to)
                    }
                    .decodeList<DuaSubcategory>()
            }.sortedWith(compareBy({ it.sort_no }, { it.name }))

            DuaPreferences.setSubcategoriesCache(
                json.encodeToString(subcategorySerializer, items)
            )
            items
        } catch (e: Exception) {
            cachedSubcategories()
        }
    }

    /**
     * Bütün dualar, başlıq üzrə qruplaşdırıla bilən düz siyahı kimi.
     *
     * ⚠️ **Səhifə-səhifə** oxunur: PostgREST bir cavabda ən çox 1000 sətir verir və limitə dəyən
     * sorğu xəta yox, **qısa cavab** qaytarır — yəni sonrakı dualar səssizcə yoxa çıxardı
     * (bax [fetchAllPages]).
     */
    suspend fun fetchDuas(): List<Dua> = withContext(Dispatchers.IO) {
        try {
            val items = fetchAllPages { from, to ->
                SupabaseProvider.client.from(TABLE_DUA)
                    .select {
                        order("sort_no", Order.ASCENDING)
                        range(from, to)
                    }
                    .decodeList<Dua>()
            }.sortedWith(compareBy({ it.sort_no }, { it.id ?: 0L }))

            DuaPreferences.setDuasCache(json.encodeToString(duaSerializer, items))
            items
        } catch (e: Exception) {
            cachedDuas()
        }
    }

    fun cachedCategories(): List<DuaCategory> =
        decode(DuaPreferences.getCategoriesCache(), categorySerializer)

    fun cachedDuas(): List<Dua> = decode(DuaPreferences.getDuasCache(), duaSerializer)

    fun cachedSubcategories(): List<DuaSubcategory> =
        decode(DuaPreferences.getSubcategoriesCache(), subcategorySerializer)

    /**
     * Yeni başlıq yaradır və yaranmış sətri qaytarır.
     *
     * `slug` çağıran tərəfdə qurulur ([com.cafarovceyxun.anamuslim.utils.supabase.duaCategorySlug]);
     * eyni slug artıq varsa baza PK toqquşması verir və çağıran tərəf ona görə növbəti variantı
     * sınayır.
     */
    suspend fun createCategory(category: DuaCategory): Result<DuaCategory> =
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseProvider.client.from(TABLE_CATEGORY)
                    .insert(category) { select() }
                    .decodeList<DuaCategory>()
                    .firstOrNull()
                    ?: throw IllegalStateException("Sətir yazılmadı (RLS?)")
            }
        }

    /** Yeni alt başlıq; slug çağıran tərəfdə qurulur (bax [createCategory]). */
    suspend fun createSubcategory(subcategory: DuaSubcategory): Result<DuaSubcategory> =
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseProvider.client.from(TABLE_SUBCATEGORY)
                    .insert(subcategory) { select() }
                    .decodeList<DuaSubcategory>()
                    .firstOrNull()
                    ?: throw IllegalStateException("Sətir yazılmadı (RLS?)")
            }
        }

    /**
     * Başlığın adını dəyişir. **Slug dəyişmir** — o, duaların xarici açarıdır; adı dəyişmək
     * qruplaşdırmanı pozmamalıdır.
     */
    suspend fun renameCategory(slug: String, name: String, nameAr: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val updated = SupabaseProvider.client.from(TABLE_CATEGORY)
                    .update({
                        set("name", name)
                        set("name_ar", nameAr)
                    }) {
                        select()
                        filter { eq("slug", slug) }
                    }
                    .decodeList<DuaCategory>()

                if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
            }
        }

    /** Alt başlığın adını dəyişir — bax [renameCategory]. */
    suspend fun renameSubcategory(slug: String, name: String, nameAr: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val updated = SupabaseProvider.client.from(TABLE_SUBCATEGORY)
                    .update({
                        set("name", name)
                        set("name_ar", nameAr)
                    }) {
                        select()
                        filter { eq("slug", slug) }
                    }
                    .decodeList<DuaSubcategory>()

                if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
            }
        }

    /**
     * Alt başlığı silir.
     *
     * İçindəki dualar **itmir**: bazada `on delete set null`, yəni onlar başlığın birbaşa altına
     * qalxır. Başlıq silmədən fərqi budur (orada CASCADE var).
     */
    suspend fun deleteSubcategory(slug: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE_SUBCATEGORY).delete { filter { eq("slug", slug) } }

            val stillThere = SupabaseProvider.client.from(TABLE_SUBCATEGORY)
                .select { filter { eq("slug", slug) } }
                .decodeList<DuaSubcategory>()
                .isNotEmpty()

            if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
        }
    }

    /** Duanı əlavə edir. `id` göndərilmir — sütun `GENERATED ALWAYS`-dır. */
    suspend fun addDua(dua: Dua): Result<Dua> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE_DUA)
                .insert(dua.copy(id = null)) { select() }
                .decodeList<Dua>()
                .firstOrNull()
                ?: throw IllegalStateException("Sətir yazılmadı (RLS?)")
        }.recoverCatching { throw mapWriteError(it) }
    }

    /**
     * Duanın mətnini, qeydini, sayını və başlığını yeniləyir; **mənbəyi dəyişmir**.
     *
     * Mənbə (hansı hədis/ayə) yalnız əlavə edərkən təyin olunur: onu redaktədə dəyişmək yeni sətir
     * yaratmaqla eynidir, üstəlik `md5(text_ar)` unikal indeksi ilə də ziddiyyət yarada bilər.
     */
    suspend fun updateDua(dua: Dua): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val id = dua.id ?: throw IllegalArgumentException("id yoxdur")

            val updated = SupabaseProvider.client.from(TABLE_DUA)
                .update({
                    set("category_slug", dua.category_slug)
                    // Alt başlığı təmizləmək («birbaşa başlığın altına qaytar») mümkün olsun deyə
                    // null da açıq göndərilir.
                    set("subcategory_slug", dua.subcategory_slug)
                    set("text_ar", dua.text_ar)
                    set("text_az", dua.text_az)
                    set("transliteration", dua.transliteration)
                    // Qeydi təmizləmək mümkün olsun deyə null da açıq göndərilir.
                    set("note", dua.note)
                    set("source", dua.source)
                    // Sayı təmizləmək mümkün olsun deyə null da açıq göndərilir.
                    set("repeat_count", dua.repeat_count)
                    set("sort_no", dua.sort_no)
                }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<Dua>()

            if (updated.isEmpty()) throw IllegalStateException("Sətir dəyişmədi (RLS?)")
        }
    }

    /**
     * Duanı silir və nəticəni **yenidən oxuyaraq** yoxlayır.
     *
     * `delete { select() }`-in boş gövdəsi iki şey deməkdir — «RLS bloklad*ı*» və «silindi, gövdə
     * qayıtmadı». Sətrin qalıb-qalmaması yeganə birmənalı cavabdır (`DailyContentRepository` ilə
     * eyni səbəb).
     */
    suspend fun deleteDua(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE_DUA).delete { filter { eq("id", id) } }

            val stillThere = SupabaseProvider.client.from(TABLE_DUA)
                .select { filter { eq("id", id) } }
                .decodeList<Dua>()
                .isNotEmpty()

            if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
        }
    }

    /** Başlığı silir — bazadakı `on delete cascade` içindəki duaları da aparır. */
    suspend fun deleteCategory(slug: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE_CATEGORY).delete { filter { eq("slug", slug) } }

            val stillThere = SupabaseProvider.client.from(TABLE_CATEGORY)
                .select { filter { eq("slug", slug) } }
                .decodeList<DuaCategory>()
                .isNotEmpty()

            if (stillThere) throw IllegalStateException("Sətir silinmədi (RLS?)")
        }
    }

    /**
     * Başlıqların yeni sırasını yazır — [changes] yalnız **dəyişən** sətirlərdir (slug → yeni `sort_no`).
     *
     * Yalnız dəyişənləri göndərmək qəsdəndir: bir elementi yerindən tərpətmək adətən bir neçə
     * qonşunu sürüşdürür, qalan onlarla sətri eyni dəyərlə yenidən yazmağın mənası yoxdur
     * (hər biri həm sorğu, həm də `updated_at` trigger-i deməkdir).
     */
    suspend fun updateCategoryOrder(changes: List<Pair<String, Int>>): Result<Unit> =
        withContext(Dispatchers.IO) {
            writeSortOrder(changes) { slug, sortNo ->
                SupabaseProvider.client.from(TABLE_CATEGORY)
                    .update({ set("sort_no", sortNo) }) {
                        select()
                        filter { eq("slug", slug) }
                    }
                    .decodeList<DuaCategory>()
                    .isNotEmpty()
            }
        }

    /** Alt başlıqların sırası — bax [updateCategoryOrder]. */
    suspend fun updateSubcategoryOrder(changes: List<Pair<String, Int>>): Result<Unit> =
        withContext(Dispatchers.IO) {
            writeSortOrder(changes) { slug, sortNo ->
                SupabaseProvider.client.from(TABLE_SUBCATEGORY)
                    .update({ set("sort_no", sortNo) }) {
                        select()
                        filter { eq("slug", slug) }
                    }
                    .decodeList<DuaSubcategory>()
                    .isNotEmpty()
            }
        }

    /**
     * Duaların sırası — bax [updateCategoryOrder].
     *
     * `sort_no` yalnız **bir qrupun içində** müqayisə olunur (bir alt başlığın duaları, ya da
     * başlığın birbaşa altındakılar), ona görə hər qrup 0-dan nömrələnir; qruplar arasında eyni
     * dəyərlərin təkrarlanması heç nəyi pozmur.
     */
    suspend fun updateDuaOrder(changes: List<Pair<Long, Int>>): Result<Unit> =
        withContext(Dispatchers.IO) {
            writeSortOrder(changes) { id, sortNo ->
                SupabaseProvider.client.from(TABLE_DUA)
                    .update({ set("sort_no", sortNo) }) {
                        select()
                        filter { eq("id", id) }
                    }
                    .decodeList<Dua>()
                    .isNotEmpty()
            }
        }

    private fun <T> decode(
        cached: String,
        serializer: kotlinx.serialization.KSerializer<List<T>>,
    ): List<T> {
        if (cached.isBlank()) return emptyList()

        // Sxem dəyişibsə keş oxunmur; boş siyahı çökmədən yaxşıdır.
        return runCatching { json.decodeFromString(serializer, cached) }.getOrDefault(emptyList())
    }

    private companion object {
        const val TABLE_CATEGORY = "dua_category"
        const val TABLE_SUBCATEGORY = "dua_subcategory"
        const val TABLE_DUA = "dua"
    }
}
