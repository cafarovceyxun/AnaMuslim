@file:Suppress("UNCHECKED_CAST")

package com.cafarovceyxun.anamuslim.utils.reader

import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


data class VerseModeConfig(
    val script: QuranScript,
    val translations: Set<String>,
    val arabicSize: Float,
    val translationSize: Float,
    val arabicEnabled: Boolean,
    val wbwId: String,
    val highlightParentheses: Boolean,
    val showParentheses: Boolean,
)

data class MushafModeConfig(
    val script: QuranScript,
)

data class TranslationModeConfig(
    val script: QuranScript,
    val translations: Set<String>,
    val translationSize: Float,
    val highlightParentheses: Boolean,
    val showParentheses: Boolean,
) {
    fun toCacheKey(): String {
        return "${script.scriptCode}_${script.variant}_${translations.joinToString()}_${translationSize}_${highlightParentheses}_${showParentheses}"
    }
}


sealed interface ReaderObserveAction {
    data class BuildVerse(
        val cfg: VerseModeConfig
    ) : ReaderObserveAction

    data class SwitchMushaf(
        val cfg: MushafModeConfig
    ) : ReaderObserveAction

    data class BuildTranslation(
        val cfg: TranslationModeConfig
    ) : ReaderObserveAction
}

object ReaderChangeManager {

    fun verseModeFlow(): Flow<ReaderObserveAction> {
        return combine(
            scriptFlow(),
            translationSlugsFlow(),
            arabicSizeFlow(),
            translationSizeFlow(),
            arabicEnabledFlow(),
            wbwIdFlow(),
            highlightParenthesesFlow(),
            showParenthesesFlow(),
        ) { values ->
            ReaderObserveAction.BuildVerse(
                VerseModeConfig(
                    script = values[0] as QuranScript,
                    translations = values[1] as Set<String>,
                    arabicSize = values[2] as Float,
                    translationSize = values[3] as Float,
                    arabicEnabled = values[4] as Boolean,
                    wbwId = values[5] as String,
                    highlightParentheses = values[6] as Boolean,
                    showParentheses = values[7] as Boolean,
                )
            )
        }.distinctUntilChanged()
    }

    fun mushafModeFlow(): Flow<ReaderObserveAction> {
        return scriptFlow()
            .map {
                ReaderObserveAction.SwitchMushaf(
                    MushafModeConfig(it)
                )
            }
            .distinctUntilChanged()
    }

    fun translationModeFlow(): Flow<ReaderObserveAction> {
        return combine(
            scriptFlow(),
            translationSlugsFlow(),
            translationSizeFlow(),
            highlightParenthesesFlow(),
            showParenthesesFlow(),
        ) { values ->
            val script = values[0] as QuranScript
            val slugs = values[1] as Set<String>
            val size = values[2] as Float
            val highlight = values[3] as Boolean
            val show = values[4] as Boolean

            ReaderObserveAction.BuildTranslation(
                TranslationModeConfig(
                    script = script,
                    translations = slugs,
                    translationSize = size,
                    highlightParentheses = highlight,
                    showParentheses = show,
                )
            )
        }.distinctUntilChanged()
    }


    private fun scriptFlow(): Flow<QuranScript> {
        return DataStoreManager.flowMultiple(
            ReaderPreferences.KEY_SCRIPT,
            ReaderPreferences.KEY_SCRIPT_VARIANT,
        ).map { prefs ->
            val script = QuranScriptUtils.validatePreferredScript(
                prefs.get(ReaderPreferences.KEY_SCRIPT)
            )

            val variant = prefs.get(ReaderPreferences.KEY_SCRIPT_VARIANT)

            QuranScript.fromRawValues(script, variant)
        }.distinctUntilChanged()
    }

    private fun translationSlugsFlow(): Flow<Set<String>> {
        return ReaderPreferences.translationsFlow()
            .distinctUntilChanged()
    }

    private fun arabicSizeFlow(): Flow<Float> {
        return DataStoreManager.flow(
            ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC
        ).distinctUntilChanged()
    }

    private fun translationSizeFlow(): Flow<Float> {
        return DataStoreManager.flow(
            ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL
        ).distinctUntilChanged()
    }

    private fun arabicEnabledFlow(): Flow<Boolean> {
        return DataStoreManager.flow(
            ReaderPreferences.KEY_ARABIC_TEXT_ENABLED
        ).distinctUntilChanged()
    }

    private fun wbwIdFlow(): Flow<String> {
        return DataStoreManager.flowMultiple(
            ReaderPreferences.KEY_WBW,
            ReaderPreferences.KEY_WBW_CONTENT_EPOCH,
        ).map {
            it.get(ReaderPreferences.KEY_WBW)
        }.distinctUntilChanged()
    }

    private fun highlightParenthesesFlow(): Flow<Boolean> {
        return ReaderPreferences.translHighlightParenthesesFlow().distinctUntilChanged()
    }

    private fun showParenthesesFlow(): Flow<Boolean> {
        return ReaderPreferences.translShowParenthesesFlow().distinctUntilChanged()
    }
}
