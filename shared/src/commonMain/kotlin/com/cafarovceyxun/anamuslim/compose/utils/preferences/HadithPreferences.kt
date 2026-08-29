package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils

object HadithPreferences {
    /**
     * [DEFAULT_VIEW_MODE]-un «sonuncu istifadə olunan» dəyəri — tab indeksi deyil, ona görə mənfidir
     * və heç vaxt [VIEW_MODE]-a yazılmır.
     */
    const val VIEW_MODE_LAST_USED = -1

    private const val KEY_ARABIC_ENABLED = "hadith.arabic_enabled"
    private const val KEY_AZERBAIJANI_ENABLED = "hadith.azerbaijani_enabled"
    private const val KEY_SOURCE_ENABLED = "hadith.source_enabled"
    private const val KEY_ARABIC_SIZE = "hadith.arabic_size"
    private const val KEY_AZERBAIJANI_SIZE = "hadith.azerbaijani_size"
    private const val KEY_HIGHLIGHT_PARENTHESES = "hadith.highlight_parentheses"
    private const val KEY_SHOW_PARENTHESES = "hadith.show_parentheses"
    private const val KEY_ARABIC_FONT = "hadith.arabic_font"
    private const val KEY_VIEW_MODE = "hadith.v_mode"
    private const val KEY_DEFAULT_VIEW_MODE = "hadith.default_v_mode"
    private const val KEY_BOOK_MODE = "hadith.book_mode"
    private const val KEY_SCROLL_AMOUNT_MODE = "hadith.scroll_mode"

    val ARABIC_ENABLED = PrefKey(booleanPreferencesKey(KEY_ARABIC_ENABLED), true)
    val AZERBAIJANI_ENABLED = PrefKey(booleanPreferencesKey(KEY_AZERBAIJANI_ENABLED), true)
    val SOURCE_ENABLED = PrefKey(booleanPreferencesKey(KEY_SOURCE_ENABLED), true)
    val ARABIC_SIZE_MULT = PrefKey(floatPreferencesKey(KEY_ARABIC_SIZE), ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT)
    val AZERBAIJANI_SIZE_MULT = PrefKey(floatPreferencesKey(KEY_AZERBAIJANI_SIZE), ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT)
    val HIGHLIGHT_PARENTHESES = PrefKey(booleanPreferencesKey(KEY_HIGHLIGHT_PARENTHESES), true)
    val SHOW_PARENTHESES = PrefKey(booleanPreferencesKey(KEY_SHOW_PARENTHESES), true)
    val ARABIC_FONT = PrefKey(stringPreferencesKey(KEY_ARABIC_FONT), QuranScriptUtils.HADITH_ARABIC_FONT_DEFAULT)
    val VIEW_MODE = PrefKey(intPreferencesKey(KEY_VIEW_MODE), 0)

    /**
     * Oxucunun **açılış** rejimi — istifadəçinin ayarlarda seçdiyi tab.
     *
     * [VIEW_MODE] canlı vəziyyətdir (oxuyarkən tab dəyişdikcə yazılır), bu isə niyyətdir: hədisə
     * əlfəcindən, oxuma tarixçəsindən və ya indeksdən girəndə [applyDefaultViewMode] onu [VIEW_MODE]-a
     * köçürür. [VIEW_MODE_LAST_USED] seçilibsə heç nə yazılmır — yəni «harada qalmışdımsa o rejimdə aç».
     *
     * Əvvəllər bu yollar bilavasitə `setViewMode(0)` yazırdı: istifadəçi ərəbcə/tərcümə tabına keçsə də
     * növbəti giriş onu qarışıq rejimə qaytarırdı və seçimin heç bir yadda qalan yeri yox idi.
     */
    val DEFAULT_VIEW_MODE = PrefKey(intPreferencesKey(KEY_DEFAULT_VIEW_MODE), VIEW_MODE_LAST_USED)

    /**
     * Kitab rejimi — hədislər kart-kart yox, davamlı kitab mətni kimi axır.
     *
     * [VIEW_MODE]-dan asılı deyil: tab hansı mətnin (qarışıq / ərəbcə / tərcümə) görünəcəyini,
     * bu bayraq isə yalnız düzülüşü təyin edir. Ayarlar vərəqindəki açar da, oxuma ekranındakı
     * üzən düymə də eyni açarı yazır — ona görə ikisi öz-özünə sinxron qalır.
     */
    val BOOK_MODE = PrefKey(booleanPreferencesKey(KEY_BOOK_MODE), false)

    /**
     * Legacy: the hadith reader's old three-step scroll distance. No longer read at runtime — the
     * shared [AppPreferences.KEY_READER_SCROLL_STEP_PERCENT] replaced it — but kept so
     * [AppPreferences.migrateLegacyScrollStep] can fold a previously-chosen value into the new one.
     */
    val SCROLL_AMOUNT_MODE = PrefKey(intPreferencesKey(KEY_SCROLL_AMOUNT_MODE), 1)

    suspend fun setArabicEnabled(enabled: Boolean) = DataStoreManager.write(ARABIC_ENABLED, enabled)
    suspend fun setAzerbaijaniEnabled(enabled: Boolean) = DataStoreManager.write(AZERBAIJANI_ENABLED, enabled)
    suspend fun setSourceEnabled(enabled: Boolean) = DataStoreManager.write(SOURCE_ENABLED, enabled)
    suspend fun setArabicSizeMultiplier(mult: Float) = DataStoreManager.write(ARABIC_SIZE_MULT, mult)
    suspend fun setAzerbaijaniSizeMultiplier(mult: Float) = DataStoreManager.write(AZERBAIJANI_SIZE_MULT, mult)
    suspend fun setHighlightParentheses(highlight: Boolean) = DataStoreManager.write(HIGHLIGHT_PARENTHESES, highlight)
    suspend fun setShowParentheses(show: Boolean) = DataStoreManager.write(SHOW_PARENTHESES, show)
    suspend fun setArabicFont(font: String) = DataStoreManager.write(ARABIC_FONT, font)
    suspend fun setViewMode(mode: Int) = DataStoreManager.write(VIEW_MODE, mode)
    suspend fun setDefaultViewMode(mode: Int) = DataStoreManager.write(DEFAULT_VIEW_MODE, mode)

    /**
     * Oxucunu istifadəçinin seçdiyi açılış rejiminə qaytarır — hədisə kənardan (indeks, əlfəcin,
     * oxuma tarixçəsi) girən hər yol bunu çağırır.
     *
     * «Sonuncu istifadə olunan» seçilibsə **heç nə yazmır**: cari tab olduğu kimi qalır.
     */
    suspend fun applyDefaultViewMode() {
        val mode = DataStoreManager.readFirst(DEFAULT_VIEW_MODE)
        if (mode != VIEW_MODE_LAST_USED) DataStoreManager.write(VIEW_MODE, mode)
    }
    suspend fun setBookMode(enabled: Boolean) = DataStoreManager.write(BOOK_MODE, enabled)

    suspend fun getShowParentheses() = DataStoreManager.readFirst(SHOW_PARENTHESES)

    @Composable
    fun observeArabicEnabled() = DataStoreManager.observe(ARABIC_ENABLED)
    @Composable
    fun observeAzerbaijaniEnabled() = DataStoreManager.observe(AZERBAIJANI_ENABLED)
    @Composable
    fun observeSourceEnabled() = DataStoreManager.observe(SOURCE_ENABLED)
    @Composable
    fun observeArabicSizeMultiplier() = DataStoreManager.observe(ARABIC_SIZE_MULT)
    @Composable
    fun observeAzerbaijaniSizeMultiplier() = DataStoreManager.observe(AZERBAIJANI_SIZE_MULT)
    @Composable
    fun observeHighlightParentheses() = DataStoreManager.observe(HIGHLIGHT_PARENTHESES)
    @Composable
    fun observeShowParentheses() = DataStoreManager.observe(SHOW_PARENTHESES)
    @Composable
    fun observeArabicFont() = DataStoreManager.observe(ARABIC_FONT)
    @Composable
    fun observeViewMode() = DataStoreManager.observe(VIEW_MODE)
    @Composable
    fun observeDefaultViewMode() = DataStoreManager.observe(DEFAULT_VIEW_MODE)
    @Composable
    fun observeBookMode() = DataStoreManager.observe(BOOK_MODE)

    /**
     * Moves a stored Arabic font off a value the picker no longer offers — the old Quran mushaf
     * faces (`uthmani`, `pdms_islamic`, `uthmani_hafs`) — onto the default book font, so the picker
     * highlights the row the reader is actually showing. Idempotent (a supported value is left
     * alone) and self-limiting (the picker can only ever store a supported value), so it needs no
     * "already migrated" flag. Call once at startup, after [DataStoreManager.warmUp].
     */
    suspend fun migrateArabicFontToBookFonts() {
        val current = DataStoreManager.readFirst(ARABIC_FONT)
        if (current !in QuranScriptUtils.HADITH_ARABIC_FONTS) {
            DataStoreManager.write(ARABIC_FONT, QuranScriptUtils.HADITH_ARABIC_FONT_DEFAULT)
        }
    }
}
