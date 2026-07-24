package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils

object HadithPreferences {
    private const val KEY_ARABIC_ENABLED = "hadith.arabic_enabled"
    private const val KEY_AZERBAIJANI_ENABLED = "hadith.azerbaijani_enabled"
    private const val KEY_SOURCE_ENABLED = "hadith.source_enabled"
    private const val KEY_ARABIC_SIZE = "hadith.arabic_size"
    private const val KEY_AZERBAIJANI_SIZE = "hadith.azerbaijani_size"
    private const val KEY_HIGHLIGHT_PARENTHESES = "hadith.highlight_parentheses"
    private const val KEY_SHOW_PARENTHESES = "hadith.show_parentheses"
    private const val KEY_ARABIC_FONT = "hadith.arabic_font"
    private const val KEY_VIEW_MODE = "hadith.v_mode"
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
