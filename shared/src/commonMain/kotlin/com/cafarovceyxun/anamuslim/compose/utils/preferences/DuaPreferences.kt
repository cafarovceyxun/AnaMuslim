package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Dualar və Əsmaül Hüsnə bölmələrinin **oflayn keşi**.
 *
 * Məzmun Supabase-dədir və hədis kimi cihazdakı Room bazasına sinxronlaşdırılmır: sətir sayı
 * kiçikdir, yeni dua isə tətbiq buraxılışı gözləmədən görünməlidir. Şəbəkə olmayanda ekran boş
 * qalmasın deyə son uğurlu cavab burada JSON kimi saxlanılır — `DailyContentRepository`-dəki
 * keşlə eyni yanaşma.
 *
 * ⚠️ Altı açar da **cihaza bağlıdır** ([com.cafarovceyxun.anamuslim.utils.univ.PreferenceBackup.DEVICE_LOCAL_KEYS]):
 * ehtiyat nüsxə ilə köçürülsə yeni telefonda köhnə məzmun «təzə» sayılardı, halbuki serverdən
 * onsuz da bir sorğuda gəlir.
 */
object DuaPreferences {

    private val KEY_CATEGORIES_CACHE = PrefKey(stringPreferencesKey("dua_categories_cache"), "")
    private val KEY_SUBCATEGORIES_CACHE =
        PrefKey(stringPreferencesKey("dua_subcategories_cache"), "")
    private val KEY_DUAS_CACHE = PrefKey(stringPreferencesKey("dua_items_cache"), "")
    private val KEY_ASMA_NAMES_CACHE = PrefKey(stringPreferencesKey("asma_names_cache"), "")
    private val KEY_ASMA_EVIDENCE_CACHE = PrefKey(stringPreferencesKey("asma_evidence_cache"), "")

    /**
     * Ad → dəlil sayı xəritəsi.
     *
     * Dəlillərin özündən ayrıdır, çünki sayı **bütün** adlar üçün lazımdır (siyahıdakı nişan),
     * dəlillərin özü isə yalnız açılan ad üçün yüklənir.
     */
    private val KEY_ASMA_COUNTS_CACHE = PrefKey(stringPreferencesKey("asma_counts_cache"), "")

    fun getCategoriesCache(): String = DataStoreManager.read(KEY_CATEGORIES_CACHE)

    suspend fun setCategoriesCache(json: String) =
        DataStoreManager.write(KEY_CATEGORIES_CACHE, json)

    fun getSubcategoriesCache(): String = DataStoreManager.read(KEY_SUBCATEGORIES_CACHE)

    suspend fun setSubcategoriesCache(json: String) =
        DataStoreManager.write(KEY_SUBCATEGORIES_CACHE, json)

    fun getDuasCache(): String = DataStoreManager.read(KEY_DUAS_CACHE)

    suspend fun setDuasCache(json: String) = DataStoreManager.write(KEY_DUAS_CACHE, json)

    fun getAsmaNamesCache(): String = DataStoreManager.read(KEY_ASMA_NAMES_CACHE)

    suspend fun setAsmaNamesCache(json: String) =
        DataStoreManager.write(KEY_ASMA_NAMES_CACHE, json)

    fun getAsmaCountsCache(): String = DataStoreManager.read(KEY_ASMA_COUNTS_CACHE)

    suspend fun setAsmaCountsCache(json: String) =
        DataStoreManager.write(KEY_ASMA_COUNTS_CACHE, json)

    fun getAsmaEvidenceCache(): String = DataStoreManager.read(KEY_ASMA_EVIDENCE_CACHE)

    suspend fun setAsmaEvidenceCache(json: String) =
        DataStoreManager.write(KEY_ASMA_EVIDENCE_CACHE, json)
}
