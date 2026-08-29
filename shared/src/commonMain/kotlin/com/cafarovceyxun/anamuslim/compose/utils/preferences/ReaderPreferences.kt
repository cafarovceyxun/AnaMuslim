@file:OptIn(ExperimentalCoroutinesApi::class)

package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptVariant
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.reader.isKFQPCScript
import com.cafarovceyxun.anamuslim.utils.reader.isQuranAtlasScript
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object ReaderPreferences {

    val KEY_ARABIC_TEXT_ENABLED =
        PrefKey(booleanPreferencesKey(Keys.READER_KEY_ARABIC_TEXT_ENABLED), true)

    // Default ON: tajweed colours ship enabled; the onboarding script page surfaces the toggle
    // (pre-checked) so a first-time user is asked and can opt out.
    val KEY_TAJWEED_COLORS_ENABLED =
        PrefKey(booleanPreferencesKey("reader.tajweed_colors_enabled"), true)

    /**
     * Ayə-ayə rejiminin kitab düzülüşü: ayə kartlarının siyahısı əvəzinə müshəf səhifəsi başına
     * vərəqlənən səhifələr — hər ayə ərəbcəsi, tərcüməsi və qeydi ilə.
     *
     * Ayrıca rejim deyil, rejimin içindəki açardır: [KEY_READER_MODE]-a qarışmır, ona görə tablar
     * üçdə qalır və «açılış rejimi» ayarı bölünmür.
     */
    val KEY_READER_BOOK_MODE =
        PrefKey(booleanPreferencesKey("reader.book_mode"), false)

    val KEY_TRANSL_HIGHLIGHT_PARENTHESES =
        PrefKey(booleanPreferencesKey(Keys.READER_KEY_TRANSL_HIGHLIGHT_PARENTHESES), true)

    val KEY_TRANSL_SHOW_PARENTHESES =
        PrefKey(booleanPreferencesKey(Keys.READER_KEY_TRANSL_SHOW_PARENTHESES), true)

    val KEY_AUTO_SCROLL_SPEED =
        PrefKey(floatPreferencesKey(Keys.READER_KEY_AUTO_SCROLL_SPEED), 7f)

    // Addım `AutoScroll` nərdivanının indeksidir: 2 = 1x. Aralıq 0.5x-ə qədər enəndən sonra 1
    // (= 0.5x) ən yavaş pillə oldu, ona görə standart bir pillə yuxarı sürüşdü.
    val KEY_AUTO_SCROLL_STEP =
        PrefKey(intPreferencesKey(Keys.READER_KEY_AUTO_SCROLL_STEP), 2)

    val KEY_TEXT_SIZE_MULT_ARABIC =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT
        )

    val KEY_TEXT_SIZE_MULT_TRANSL =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT
        )

    val KEY_TRANSLATIONS =
        PrefKey(
            stringSetPreferencesKey(TranslUtils.KEY_TRANSLATIONS),
            TranslUtils.defaultTranslationSlugs()
        )

    val KEY_SCRIPT =
        PrefKey(stringPreferencesKey(QuranScriptUtils.KEY_SCRIPT), QuranScriptUtils.SCRIPT_DEFAULT)

    val KEY_SCRIPT_VARIANT = PrefKey(stringPreferencesKey("script_variant"), "")

    val KEY_READER_MODE =
        PrefKey(stringPreferencesKey(Keys.READER_KEY_READER_MODE), ReaderMode.VerseByVerse.value)

    /**
     * Oxucunun **açılış** rejimi — istifadəçinin ayarlarda seçdiyi rejim.
     *
     * [KEY_READER_MODE] canlı vəziyyətdir (oxuyarkən rejim tabı dəyişdikcə yazılır), bu isə niyyətdir:
     * boş sətir «sonuncu istifadə olunan» deməkdir və [resolveLaunchReaderMode] onda çağırışın öz
     * rejiminə, o da yoxdursa sonuncu rejimə düşür.
     */
    val KEY_DEFAULT_READER_MODE = PrefKey(stringPreferencesKey("reader.default_mode"), "")

    val KEY_WBW =
        PrefKey(stringPreferencesKey("key.wbw"), "")

    val KEY_WBW_CONTENT_EPOCH =
        PrefKey(longPreferencesKey("reader.wbw.content_epoch"), 0L)

    val KEY_WBW_SHOW_TRANSLATION =
        PrefKey(booleanPreferencesKey("reader.wbw.show_translation"), false)

    val KEY_WBW_SHOW_TRANSLITERATION =
        PrefKey(booleanPreferencesKey("reader.wbw.show_transliteration"), false)


    val KEY_WBW_TOOLTIP_SHOW_TRANSLATION =
        PrefKey(booleanPreferencesKey("reader.wbw_tooltip.show_translation"), true)

    val KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION =
        PrefKey(booleanPreferencesKey("reader.wbw_tooltip.show_transliteration"), true)

    val KEY_WBW_RECITATION =
        PrefKey(booleanPreferencesKey("reader.wbw.recitation"), true)

    val KEY_TEXT_SIZE_MULT_WBW =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_WBW),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_WBW_DEFAULT
        )

    val KEY_LEGACY_MIGRATED =
        PrefKey(booleanPreferencesKey("reader.prefs.legacy_migrated_v1"), false)

    val KEY_AUTO_SCROLL_INSTRUCTIONS_SHOWN_COUNT =
        PrefKey(longPreferencesKey("reader.auto_scroll_instructions_shown_count"), 0L)

    val KEY_AUTO_SCROLL_HIDE_INSTRUCTIONS =
        PrefKey(booleanPreferencesKey("reader.auto_scroll_hide_instructions"), false)

    fun migrateFromLegacyIfNeeded() {
        runBlocking {
            ReaderPreferencesHooks.migrateFromLegacy?.invoke()
        }
    }

    fun repairStoredPreferencesIfNeeded() {
        runBlocking {
            repairStoredPreferences()
        }
    }

    /**
     * Resets stored reader preferences that can no longer be honoured, so the reader never opens on
     * a script it cannot draw. The whole check is platform-neutral (DataStore + the shared script
     * utils, atlas DAO and translation factory), so it lives here and runs on both platforms — was
     * an Android-only hook until the atlas/font pipeline became shared.
     *
     * - A KFQPC script with fonts still missing → default script.
     * - An atlas script whose bundle is not imported → default script.
     * - Stored translation slugs that are no longer available → the available subset, or the
     *   defaults when none survive.
     */
    suspend fun repairStoredPreferences() = withContext(Dispatchers.IO) {
        val storedScript = DataStoreManager.readFirst(KEY_SCRIPT)
        val storedTranslations = DataStoreManager.readFirst(KEY_TRANSLATIONS)

        var working = QuranScriptUtils.validatePreferredScript(storedScript)

        if (working.isKFQPCScript()) {
            if (QuranScriptUtils.getKFQPCFontDownloadedCount(working).remaining > 0) {
                working = QuranScriptUtils.SCRIPT_DEFAULT
            }
        }

        if (working.isQuranAtlasScript()) {
            val installed = RepositoryProvider.externalQuranDatabase
                .atlasWordShapeDao()
                .getBundleByKey(working) != null

            if (!installed) {
                working = QuranScriptUtils.SCRIPT_DEFAULT
            }
        }

        val availableTranslationSlugs = QuranTranslationFactory().use { factory ->
            factory.getAvailableTranslationBooksInfo().keys
        }
        val repairedTranslations = storedTranslations.filterTo(hashSetOf()) {
            it in availableTranslationSlugs
        }.ifEmpty {
            TranslUtils.defaultTranslationSlugs()
        }

        if (working != storedScript || repairedTranslations != storedTranslations) {
            DataStoreManager.edit {
                if (working != storedScript) {
                    this[KEY_SCRIPT.key] = working
                }
                if (repairedTranslations != storedTranslations) {
                    this[KEY_TRANSLATIONS.key] = repairedTranslations
                }
            }
        }
    }

    suspend fun getArabicTextEnabled(): Boolean {
        return DataStoreManager.readFirst(KEY_ARABIC_TEXT_ENABLED)
    }

    suspend fun setArabicTextEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_ARABIC_TEXT_ENABLED, enabled)
    }

    @Composable
    fun observeArabicTextEnabled(): Boolean {
        return DataStoreManager.observe(KEY_ARABIC_TEXT_ENABLED)
    }

    suspend fun getTajweedColorsEnabled(): Boolean {
        return DataStoreManager.readFirst(KEY_TAJWEED_COLORS_ENABLED)
    }

    suspend fun setTajweedColorsEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_TAJWEED_COLORS_ENABLED, enabled)
    }

    @Composable
    fun observeTajweedColorsEnabled(): Boolean {
        return DataStoreManager.observe(KEY_TAJWEED_COLORS_ENABLED)
    }

    suspend fun getBookMode(): Boolean {
        return DataStoreManager.readFirst(KEY_READER_BOOK_MODE)
    }

    suspend fun setBookMode(enabled: Boolean) {
        DataStoreManager.write(KEY_READER_BOOK_MODE, enabled)
    }

    @Composable
    fun observeBookMode(): Boolean {
        return DataStoreManager.observe(KEY_READER_BOOK_MODE)
    }

    suspend fun getTranslHighlightParentheses(): Boolean {
        return DataStoreManager.readFirst(KEY_TRANSL_HIGHLIGHT_PARENTHESES)
    }

    suspend fun setTranslHighlightParentheses(highlight: Boolean) {
        DataStoreManager.write(KEY_TRANSL_HIGHLIGHT_PARENTHESES, highlight)
    }

    @Composable
    fun observeTranslHighlightParentheses(): Boolean {
        return DataStoreManager.observe(KEY_TRANSL_HIGHLIGHT_PARENTHESES)
    }

    fun translHighlightParenthesesFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_TRANSL_HIGHLIGHT_PARENTHESES)
    }

    suspend fun getTranslShowParentheses(): Boolean {
        return DataStoreManager.readFirst(KEY_TRANSL_SHOW_PARENTHESES)
    }

    suspend fun setTranslShowParentheses(show: Boolean) {
        DataStoreManager.write(KEY_TRANSL_SHOW_PARENTHESES, show)
    }

    @Composable
    fun observeTranslShowParentheses(): Boolean {
        return DataStoreManager.observe(KEY_TRANSL_SHOW_PARENTHESES)
    }

    fun translShowParenthesesFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_TRANSL_SHOW_PARENTHESES)
    }

    suspend fun getAutoScrollSpeed(): Float {
        return DataStoreManager.readFirst(KEY_AUTO_SCROLL_SPEED)
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        DataStoreManager.write(KEY_AUTO_SCROLL_SPEED, speed)
    }

    @Composable
    fun observeAutoScrollSpeed(): Float {
        return DataStoreManager.observe(KEY_AUTO_SCROLL_SPEED)
    }

    suspend fun getAutoScrollStep(): Int {
        return DataStoreManager.readFirst(KEY_AUTO_SCROLL_STEP)
    }

    suspend fun setAutoScrollStep(step: Int) {
        DataStoreManager.write(KEY_AUTO_SCROLL_STEP, step)
    }

    @Composable
    fun observeAutoScrollStep(): Int {
        return DataStoreManager.observe(KEY_AUTO_SCROLL_STEP)
    }

    fun getAutoScrollStepSync(): Int {
        return runBlocking { getAutoScrollStep() }
    }

    fun getAutoScrollSpeedSync(): Float {
        return runBlocking { getAutoScrollSpeed() }
    }

    suspend fun getArabicTextSizeMultiplier(): Float {
        return DataStoreManager.readFirst(KEY_TEXT_SIZE_MULT_ARABIC)
    }

    suspend fun setArabicTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_ARABIC, sizeMult)
    }

    @Composable
    fun observeArabicTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_ARABIC)
    }

    suspend fun getTranslationTextSizeMultiplier(): Float {
        return DataStoreManager.readFirst(KEY_TEXT_SIZE_MULT_TRANSL)
    }

    suspend fun setTranslationTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_TRANSL, sizeMult)
    }

    @Composable
    fun observeTranlationTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_TRANSL)
    }

    suspend fun getTranslations(): Set<String> {
        return DataStoreManager.readFirst(KEY_TRANSLATIONS)
    }

    suspend fun primaryTranslationSlug(): String {
        val saved = getTranslations()
        return saved.firstOrNull { !TranslUtils.isTransliteration(it) }
            ?: TranslUtils.TRANSL_SLUG_DEFAULT
    }

    @Composable
    fun observePrimaryTranslationSlug(): String {
        val saved = observeTranslations()
        return saved.firstOrNull { !TranslUtils.isTransliteration(it) }
            ?: TranslUtils.TRANSL_SLUG_DEFAULT
    }

    suspend fun setTranslations(translSlugsSet: Set<String>) {
        DataStoreManager.write(KEY_TRANSLATIONS, HashSet(translSlugsSet))
    }

    @Composable
    fun observeTranslations(): Set<String> {
        return DataStoreManager.observe(KEY_TRANSLATIONS)
    }

    fun translationsFlow(): Flow<Set<String>> {
        return DataStoreManager.flow(KEY_TRANSLATIONS)
    }

    suspend fun getQuranScript(): String {
        return QuranScriptUtils.validatePreferredScript(DataStoreManager.readFirst(KEY_SCRIPT))
    }

    suspend fun setQuranScript(font: String?) {
        DataStoreManager.write(KEY_SCRIPT, font ?: QuranScriptUtils.SCRIPT_DEFAULT)
    }

    suspend fun setQuranScriptWithVariant(
        font: String?,
        variant: QuranScriptVariant?,
    ) {
        DataStoreManager.edit {
            this[KEY_SCRIPT.key] = font ?: QuranScriptUtils.SCRIPT_DEFAULT
            this[KEY_SCRIPT_VARIANT.key] = variant?.value ?: ""
        }
    }

    fun quranScriptFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_SCRIPT)
            .mapLatest {
                QuranScriptUtils.validatePreferredScript(it)
            }
    }

    @Composable
    fun observeQuranScript(): String {
        val s = DataStoreManager.observe(KEY_SCRIPT)

        if (!QuranScriptUtils.availableScripts().contains(s)) {
            return QuranScriptUtils.SCRIPT_DEFAULT
        }

        return s
    }


    suspend fun getQuranScriptVariant(): QuranScriptVariant? {
        return QuranScriptVariant.fromValue(DataStoreManager.readFirst(KEY_SCRIPT_VARIANT))
    }

    suspend fun setQuranScriptVariant(variant: QuranScriptVariant?) {
        DataStoreManager.write(KEY_SCRIPT_VARIANT, variant?.value ?: "")
    }

    fun quranScriptVariantFlow(): Flow<QuranScriptVariant?> {
        return DataStoreManager.flow(KEY_SCRIPT_VARIANT)
            .mapLatest {
                QuranScriptVariant.fromValue(it)
            }
    }

    @Composable
    fun observeQuranScriptVariant(): QuranScriptVariant? {
        val value = DataStoreManager.observe(KEY_SCRIPT_VARIANT)

        return QuranScriptVariant.fromValue(value)
    }

    suspend fun getReaderMode(): ReaderMode {
        return ReaderMode.fromValue(DataStoreManager.readFirst(KEY_READER_MODE))
    }

    suspend fun setReaderMode(mode: ReaderMode) {
        DataStoreManager.write(KEY_READER_MODE, mode.value)
    }

    /** Ayarda konkret rejim seçilibmi; `null` — «sonuncu istifadə olunan». */
    suspend fun getDefaultReaderMode(): ReaderMode? {
        return DataStoreManager.readFirst(KEY_DEFAULT_READER_MODE)
            .takeIf { it.isNotEmpty() }
            ?.let { ReaderMode.fromValue(it) }
    }

    /** `null` yazmaq «sonuncu istifadə olunan» deməkdir. */
    suspend fun setDefaultReaderMode(mode: ReaderMode?) {
        DataStoreManager.write(KEY_DEFAULT_READER_MODE, mode?.value ?: "")
    }

    @Composable
    fun observeDefaultReaderMode(): ReaderMode? {
        return DataStoreManager.observe(KEY_DEFAULT_READER_MODE)
            .takeIf { it.isNotEmpty() }
            ?.let { ReaderMode.fromValue(it) }
    }

    /**
     * Oxucu bayırdan açılanda hansı rejimdə oyanacağını həll edir.
     *
     * Sıra: ayarda **konkret** seçim → çağırışın istədiyi rejim ([requested]) → sonuncu istifadə
     * olunan. İstifadəçi ayarda rejim seçibsə o hər şeyi üstələyir — əlfəcindən və oxuma
     * tarixçəsindən girişin həmişə həmin rejimdə oyanmasının səbəbi budur; əvvəllər bu yollar
     * rejimi [ReaderMode.VerseByVerse]-ə sabitləyirdi.
     */
    suspend fun resolveLaunchReaderMode(requested: ReaderMode?): ReaderMode {
        return getDefaultReaderMode() ?: requested ?: getReaderMode()
    }

    fun readerModeFlow(): Flow<ReaderMode> {
        return DataStoreManager.flow(KEY_READER_MODE).map { ReaderMode.fromValue(it) }
    }

    @Composable
    fun observeReaderMode(): ReaderMode {
        return DataStoreManager.observe(KEY_READER_MODE).let { ReaderMode.fromValue(it) }
    }

    suspend fun getWbwId(): String? {
        return DataStoreManager.readFirst(KEY_WBW).takeIf { it.isNotEmpty() }
    }

    suspend fun setWbwId(id: String) {
        DataStoreManager.write(KEY_WBW, id)
    }

    fun wbwIdFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_WBW)
    }

    @Composable
    fun observeWbwId(): String {
        return DataStoreManager.observe(KEY_WBW)
    }

    suspend fun getWbwContentEpoch(): Long {
        return DataStoreManager.readFirst(KEY_WBW_CONTENT_EPOCH)
    }

    suspend fun bumpWbwContentEpoch() {
        val next = DataStoreManager.readFirst(KEY_WBW_CONTENT_EPOCH) + 1L
        DataStoreManager.write(KEY_WBW_CONTENT_EPOCH, next)
    }

    suspend fun getWbwShowTranslation(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_SHOW_TRANSLATION)
    }

    suspend fun setWbwShowTranslation(show: Boolean) {
        DataStoreManager.write(KEY_WBW_SHOW_TRANSLATION, show)
    }

    @Composable
    fun observeWbwShowTranslation(): Boolean {
        return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLATION)
    }

    fun wbwShowTranslationFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_SHOW_TRANSLATION)
    }

    suspend fun getWbwShowTransliteration(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_SHOW_TRANSLITERATION)
    }

    suspend fun setWbwShowTransliteration(show: Boolean) {
        DataStoreManager.write(KEY_WBW_SHOW_TRANSLITERATION, show)
    }

    @Composable
    fun observeWbwShowTransliteration(): Boolean {
        return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLITERATION)
    }

    fun wbwShowTransliterationFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_SHOW_TRANSLITERATION)
    }

    suspend fun getWbwTooltipShowTranslation(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    }

    suspend fun setWbwTooltipShowTranslation(show: Boolean) {
        DataStoreManager.write(KEY_WBW_TOOLTIP_SHOW_TRANSLATION, show)
    }

    @Composable
    fun observeWbwTooltipShowTranslation(): Boolean {
        return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    }

    suspend fun getWbwTooltipShowTransliteration(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    }

    suspend fun setWbwTooltipShowTransliteration(show: Boolean) {
        DataStoreManager.write(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION, show)
    }

    @Composable
    fun observeWbwTooltipShowTransliteration(): Boolean {
        return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    }


    suspend fun getWbwRecitationEnabled(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_RECITATION)
    }

    suspend fun setWbwRecitationEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_WBW_RECITATION, enabled)
    }

    @Composable
    fun observeWbwRecitationEnabled(): Boolean {
        return DataStoreManager.observe(KEY_WBW_RECITATION)
    }

    fun wbwRecitationEnabledFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_RECITATION)
    }

    suspend fun getWbwTextSizeMultiplier(): Float {
        return DataStoreManager.readFirst(KEY_TEXT_SIZE_MULT_WBW)
    }

    suspend fun setWbwTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_WBW, sizeMult)
    }

    @Composable
    fun observeWbwTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_WBW)
    }

    fun wbwTextSizeMultiplierFlow(): Flow<Float> {
        return DataStoreManager.flow(KEY_TEXT_SIZE_MULT_WBW)
    }

    suspend fun getAutoScrollInstructionsShownCount(): Long {
        return DataStoreManager.readFirst(KEY_AUTO_SCROLL_INSTRUCTIONS_SHOWN_COUNT)
    }

    suspend fun incrementAutoScrollInstructionsShownCount() {
        val current = getAutoScrollInstructionsShownCount()
        DataStoreManager.write(KEY_AUTO_SCROLL_INSTRUCTIONS_SHOWN_COUNT, current + 1)
    }

    suspend fun getAutoScrollHideInstructions(): Boolean {
        return DataStoreManager.readFirst(KEY_AUTO_SCROLL_HIDE_INSTRUCTIONS)
    }

    suspend fun setAutoScrollHideInstructions(hide: Boolean) {
        DataStoreManager.write(KEY_AUTO_SCROLL_HIDE_INSTRUCTIONS, hide)
    }
}
