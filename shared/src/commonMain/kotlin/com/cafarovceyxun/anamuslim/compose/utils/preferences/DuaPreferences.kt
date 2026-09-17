package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils

/**
 * Dualar və Əsmaül Hüsnə bölmələrinin **oflayn keşi** və **oxuma ayarları**.
 *
 * Ayar açarları nöqtəli `dua.` prefiksi ilə gedir ([HadithPreferences] ilə eyni forma); keş açarları
 * isə prefikssiz köhnə adlarını saxlayır — adı dəyişsə həm mövcud keş itərdi, həm də
 * [com.cafarovceyxun.anamuslim.utils.univ.PreferenceBackup]-dakı sətirlər sınardı.
 *
 * Əsmanın ayrıca `AsmaPreferences`-i yoxdur: istifadəçi üçün Dua və Əsma bir bölmədir, ayar vərəqi
 * ortaqdır və keş onsuz da burada yaşayır.
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

    /**
     * [DEFAULT_VIEW_MODE]-un «sonuncu istifadə olunan» dəyəri — tab indeksi deyil, ona görə mənfidir
     * və heç vaxt [VIEW_MODE]-a yazılmır. Bax [HadithPreferences.VIEW_MODE_LAST_USED].
     */
    const val VIEW_MODE_LAST_USED = -1

    // ---- Oxuma ayarları ----

    private const val KEY_ARABIC_ENABLED = "dua.arabic_enabled"
    private const val KEY_TRANSLITERATION_ENABLED = "dua.transliteration_enabled"
    private const val KEY_TRANSLATION_ENABLED = "dua.translation_enabled"
    private const val KEY_ARABIC_SIZE = "dua.arabic_size"
    private const val KEY_TRANSLATION_SIZE = "dua.translation_size"
    private const val KEY_ARABIC_FONT = "dua.arabic_font"
    private const val KEY_VIEW_MODE = "dua.v_mode"
    private const val KEY_DEFAULT_VIEW_MODE = "dua.default_v_mode"
    private const val KEY_READING_PROGRESS = "dua.reading_progress"

    val ARABIC_ENABLED = PrefKey(booleanPreferencesKey(KEY_ARABIC_ENABLED), true)
    val TRANSLITERATION_ENABLED = PrefKey(booleanPreferencesKey(KEY_TRANSLITERATION_ENABLED), true)
    val TRANSLATION_ENABLED = PrefKey(booleanPreferencesKey(KEY_TRANSLATION_ENABLED), true)

    val ARABIC_SIZE_MULT =
        PrefKey(floatPreferencesKey(KEY_ARABIC_SIZE), ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT)
    val TRANSLATION_SIZE_MULT = PrefKey(
        floatPreferencesKey(KEY_TRANSLATION_SIZE),
        ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT,
    )

    val ARABIC_FONT =
        PrefKey(stringPreferencesKey(KEY_ARABIC_FONT), QuranScriptUtils.HADITH_ARABIC_FONT_DEFAULT)

    /**
     * Canlı rejim — 0 qarışıq, 1 ərəbcə, 2 tərcümə (oxunuş tərcümə ilə birlikdə gəlir).
     *
     * Rejim gizlətmə açarlarını **əvəz etmir**, onlarla kəsişir: effektiv görünüş
     * `rejim ∧ açar`-dır (bax `screens/dua/DuaVisibility.kt`).
     */
    val VIEW_MODE = PrefKey(intPreferencesKey(KEY_VIEW_MODE), 0)

    /** Açılış rejimi — [VIEW_MODE_LAST_USED] seçilibsə cari rejim toxunulmadan qalır. */
    val DEFAULT_VIEW_MODE = PrefKey(intPreferencesKey(KEY_DEFAULT_VIEW_MODE), 0)

    val READING_PROGRESS = PrefKey(booleanPreferencesKey(KEY_READING_PROGRESS), true)

    suspend fun setArabicEnabled(enabled: Boolean) = DataStoreManager.write(ARABIC_ENABLED, enabled)

    suspend fun setTransliterationEnabled(enabled: Boolean) =
        DataStoreManager.write(TRANSLITERATION_ENABLED, enabled)

    suspend fun setTranslationEnabled(enabled: Boolean) =
        DataStoreManager.write(TRANSLATION_ENABLED, enabled)

    suspend fun setArabicSizeMultiplier(mult: Float) =
        DataStoreManager.write(ARABIC_SIZE_MULT, mult)

    suspend fun setTranslationSizeMultiplier(mult: Float) =
        DataStoreManager.write(TRANSLATION_SIZE_MULT, mult)

    suspend fun setArabicFont(font: String) = DataStoreManager.write(ARABIC_FONT, font)

    suspend fun setViewMode(mode: Int) = DataStoreManager.write(VIEW_MODE, mode)

    suspend fun setDefaultViewMode(mode: Int) = DataStoreManager.write(DEFAULT_VIEW_MODE, mode)

    suspend fun setReadingProgress(enabled: Boolean) =
        DataStoreManager.write(READING_PROGRESS, enabled)

    /** Duaya kənardan (ana səhifə kartı, əlfəcin, tarixçə) girən hər yol bunu çağırır. */
    suspend fun applyDefaultViewMode() {
        val mode = DataStoreManager.readFirst(DEFAULT_VIEW_MODE)
        if (mode != VIEW_MODE_LAST_USED) DataStoreManager.write(VIEW_MODE, mode)
    }

    @Composable
    fun observeArabicEnabled() = DataStoreManager.observe(ARABIC_ENABLED)

    @Composable
    fun observeTransliterationEnabled() = DataStoreManager.observe(TRANSLITERATION_ENABLED)

    @Composable
    fun observeTranslationEnabled() = DataStoreManager.observe(TRANSLATION_ENABLED)

    @Composable
    fun observeArabicSizeMultiplier() = DataStoreManager.observe(ARABIC_SIZE_MULT)

    @Composable
    fun observeTranslationSizeMultiplier() = DataStoreManager.observe(TRANSLATION_SIZE_MULT)

    @Composable
    fun observeArabicFont() = DataStoreManager.observe(ARABIC_FONT)

    @Composable
    fun observeViewMode() = DataStoreManager.observe(VIEW_MODE)

    @Composable
    fun observeDefaultViewMode() = DataStoreManager.observe(DEFAULT_VIEW_MODE)

    @Composable
    fun observeReadingProgress() = DataStoreManager.observe(READING_PROGRESS)

    /**
     * Avtomatik uyğunlaşdırma açıqdırmı.
     *
     * Açardır, çünki nəticələr **maşın təxminidir**: «رب» kimi ümumi köklər yüzlərlə ayə verir və
     * bu, bəzi istifadəçi üçün faydadan çox səs-küydür.
     */
    val AUTO_EVIDENCE_ENABLED = PrefKey(booleanPreferencesKey("asma.auto_evidence_enabled"), true)

    suspend fun setAutoEvidenceEnabled(enabled: Boolean) =
        DataStoreManager.write(AUTO_EVIDENCE_ENABLED, enabled)

    @Composable
    fun observeAutoEvidenceEnabled() = DataStoreManager.observe(AUTO_EVIDENCE_ENABLED)

    // ---- Oflayn keş ----

    /**
     * Admin tərəfindən gizlədilmiş avtomatik uyğunluqlar (`asma_auto_hidden`-in keşi).
     *
     * ⚠️ **Cihaza bağlıdır** — qalan keşlərlə eyni səbəb. Avtomatik siyahının özü lokal Quran
     * assetindən hesablandığı üçün oflayn işləyir; gizlətmə qərarları isə serverdəndir, ona görə
     * onlar da keşlənməlidir, yoxsa şəbəkəsiz qalanda səhv sayılan ayələr geri qayıdardı.
     */
    private val KEY_ASMA_HIDDEN_CACHE = PrefKey(stringPreferencesKey("asma.hidden_cache"), "")

    fun getAsmaHiddenCache(): String = DataStoreManager.read(KEY_ASMA_HIDDEN_CACHE)

    suspend fun setAsmaHiddenCache(json: String) =
        DataStoreManager.write(KEY_ASMA_HIDDEN_CACHE, json)

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
