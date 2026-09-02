package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.appFontFamily
import com.cafarovceyxun.anamuslim.compose.theme.arabicReaderLineHeightStyle
import com.cafarovceyxun.anamuslim.compose.theme.translationPlatformTextStyle
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils

data class ComposeUiConfig(
    val textMeasurer: TextMeasurer,
    val density: Density,
    val colors: ColorScheme,
    val type: Typography,
    val isDark: Boolean,
)

data class TextBuilderParams(
    val uiConfig: ComposeUiConfig,
    val verseActions: VerseActions,
    val fontResolver: FontResolver,
    val arabicEnabled: Boolean,
    val script: String,
    val arabicSizeMultiplier: Float,
    val translationSizeMultiplier: Float,
    val slugs: Set<String>,
    val highlightParentheses: Boolean = true,
    val showParentheses: Boolean = true,
    val tajweedColorsEnabled: Boolean = false,
) {
    /**
     * Qurulmuş məzmunun keş açarı.
     *
     * [arabicEnabled] burada **olmalıdır**: ərəbcə mətn açarı bu parametrlə sönür, kitab rejimi isə
     * səhifələri məhz bu açarla keşləyir ([com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel.fetchBookPages]).
     * Açar bayrağı görməyəndə ayarda ərəbcə söndürülür, ayə-ayə siyahısı yenilənir, kitab səhifələri
     * isə keşdə ərəbcəsi ilə qalırdı — eyni ayar iki düzülüşdə iki cür işləyirdi.
     */
    fun toKey(): String {
        return "$script,$arabicEnabled,$arabicSizeMultiplier,$translationSizeMultiplier,$slugs,$highlightParentheses,$showParentheses,$tajweedColorsEnabled"
    }

    override fun toString(): String {
        return "script:$script, arabicSizeMultiplier:$arabicSizeMultiplier, translationSizeMultiplier:$translationSizeMultiplier ,slugs:$slugs"
    }
}

data class PageBuilderParams(
    val uiConfig: ComposeUiConfig,
    val contentWidthPx: Int,
    val isDark: Boolean,
    val tajweedColorsEnabled: Boolean = false,
) {
    fun toKey(): String {
        return "$contentWidthPx:${uiConfig.density.density}:$isDark:$tajweedColorsEnabled"
    }
}

data class TranslationPageBuilderParams(
    val colors: ColorScheme,
    val type: Typography,
    val verseActions: VerseActions,
    val translationSizeMultiplier: Float,
    val highlightParentheses: Boolean = true,
    val showParentheses: Boolean = true,
)

data class TranslationTextStyleParams(
    val slug: String,
    val sizeMultiplier: Float,
    val useSmallSize: Boolean = false,
    val baselineHeightMultiplier: Float = 1.5f
)

data class QuranTextStyleParams(
    val fontResolver: FontResolver,
    val colors: ColorScheme,
    val type: Typography,
    val pageNo: Int,
    val script: String,
    val sizeMultiplier: Float,
    val useSmallSize: Boolean = false,
    val isDark: Boolean,
)

/**
 * Tərcümə mətninin baza ölçüsü — **qəsdən sabitdir**, `MaterialTheme.typography`-dən oxunmur.
 *
 * Tipoqrafiya ümumi interfeys sürüşdürücüsü ilə miqyaslanır
 * ([com.cafarovceyxun.anamuslim.compose.theme.AppTextScale]); baza oradan gəlsəydi iki ölçü
 * bir-birinə vurulardı və Quran tərcüməsi öz sürüşdürücüsündən əlavə olaraq interfeys
 * sürüşdürücüsündən də böyüyərdi. Rəqəmlər `Type.kt`-dəki `bodyLarge`/`bodyMedium` ilə eynidir.
 */
private val TRANSLATION_BASE_SIZE = 16.sp
private val TRANSLATION_BASE_SIZE_SMALL = 14.sp

fun getTranslationTextStyle(
    params: TranslationTextStyleParams,
): TextStyle {
    val isRtl = StringUtils.isRtlLanguage(params.slug)

    val baselineFontSize =
        if (params.useSmallSize) TRANSLATION_BASE_SIZE_SMALL else TRANSLATION_BASE_SIZE
    val resolvedFontSize = baselineFontSize * params.sizeMultiplier

    return TextStyle(
        textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
        fontFamily = appFontFamily,
        platformStyle = translationPlatformTextStyle,
        fontSize = resolvedFontSize,
        lineHeight = resolvedFontSize * params.baselineHeightMultiplier,
    )
}

fun getQuranTextStyle(
    params: QuranTextStyleParams,
): TextStyle {
    val baseSp = params.script.getQuranScriptVerseTextSizeSp(params.useSmallSize)
    val fontSize = (baseSp * params.sizeMultiplier).sp

    return params.type.headlineSmall.copy(
        fontFamily = params.fontResolver.fontFamily(params.script, params.pageNo, params.isDark),
        fontSize = fontSize,
        color = params.colors.onBackground,
        textDirection = TextDirection.Rtl,
        lineHeight = fontSize * 1.8f,
        lineHeightStyle = arabicReaderLineHeightStyle
    )
}

fun mushafCappedBaseStyleForScale(base: TextStyle, pageScale: Float): TextStyle {
    val cap = pageScale.coerceIn(
        MUSHAF_FONT_SCALE_AT_MIN_WIDTH,
        MUSHAF_FONT_SCALE_AT_MAX_WIDTH
    )

    val scaled = (base.fontSize.value * cap).sp

    return base.copy(
        fontSize = scaled,
        lineHeight = (scaled.value * MUSHAF_LINE_HEIGHT_MULT).sp,
    )
}

/**
 * Scales the dimen-derived mushaf base font by available line width (dp).
 * Narrow phones use a slightly lower ceiling; large screens allow a larger max before shrink-only logic.
 */
private fun mushafScreenMaxFontScale(lineInnerWidthDp: Float): Float {
    val w = lineInnerWidthDp.coerceIn(MUSHAF_FONT_WIDTH_DP_MIN, MUSHAF_FONT_WIDTH_DP_MAX)
    return (MUSHAF_FONT_SCALE_AT_MIN_WIDTH + (w - MUSHAF_FONT_WIDTH_DP_MIN) / (MUSHAF_FONT_WIDTH_DP_MAX - MUSHAF_FONT_WIDTH_DP_MIN) * (MUSHAF_FONT_SCALE_AT_MAX_WIDTH - MUSHAF_FONT_SCALE_AT_MIN_WIDTH)).coerceIn(
        MUSHAF_FONT_SCALE_AT_MIN_WIDTH,
        MUSHAF_FONT_SCALE_AT_MAX_WIDTH
    )
}

fun mushafScaleForWidth(lineInnerWidthDp: Float): Float {
    return mushafScreenMaxFontScale(lineInnerWidthDp)
}


const val MUSHAF_LINE_HEIGHT_MULT = 2f
private const val MUSHAF_FONT_WIDTH_DP_MIN = 260f
const val MUSHAF_FONT_WIDTH_DP_MAX = 600f

val MUSHAF_PAGE_HORIZONTAL_PADDING = 12.dp
const val MUSHAF_FONT_SCALE_AT_MIN_WIDTH = 0.85f
const val MUSHAF_FONT_SCALE_AT_MAX_WIDTH = 1.5f

/** Inter-word gap as a fraction of font size for centered mushaf lines. */
const val MUSHAF_CENTERED_GAP_FRACTION = 0.22f

/** Minimum gap between adjacent words (all lines), as a fraction of font size — avoids overlap when justified. */
const val MUSHAF_MIN_INTER_WORD_GAP_FRACTION = 0.1f

/** When shrinking to fit, do not go below this fraction of the (screen-capped) base size. */
const val MUSHAF_LINE_SHRINK_MIN = 0.16f
