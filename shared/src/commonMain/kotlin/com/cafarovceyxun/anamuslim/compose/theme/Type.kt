package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val appFontFamilyTitle = FontFamily.Default
val appFontFamily = FontFamily.Default

/**
 * Android disables `includeFontPadding` here to match the previous look; iOS has no such concept
 * (there is no font padding), so it contributes no platform style.
 */
internal expect val platformTextStyle: PlatformTextStyle?

/**
 * Tətbiqin tipoqrafiyası, [scalePercent] ilə miqyaslanmış.
 *
 * Ölçülər burada `Float` sabitlərdir və `sp`-yə **çarpandan sonra** çevrilir: sətir hündürlüyü də
 * eyni çarpanla gedir, əks halda böyüdülmüş mətn öz sətrinə sığmazdı.
 *
 * `headline*` qəsdən burada yoxdur — Material default-ları qalır. Onları yalnız oxucu ekranları
 * işlədir və hamısı `fontSize`-ı açıq yazır; [com.cafarovceyxun.anamuslim.compose.components.ChapterIcon]
 * kimi sabit ölçülü dairələr isə default-un özünü ölçü kimi götürür və böyüsə daşardı.
 * Miqyasın Quran/hədis mətnlərinə niyə düşmədiyi [AppTextScale]-də yazılıb.
 */
@Composable
fun getAppTypography(scalePercent: Int = AppTextScale.DEFAULT_PERCENT): Typography {
    val baseFont = appFontFamily
    val titleFont = appFontFamilyTitle
    val f = AppTextScale.factor(scalePercent)

    fun style(
        font: FontFamily,
        weight: FontWeight,
        size: Float,
        lineHeight: Float,
    ) = TextStyle(
        platformStyle = platformTextStyle,
        fontFamily = font,
        fontWeight = weight,
        fontSize = (size * f).sp,
        lineHeight = (lineHeight * f).sp,
        letterSpacing = 0.sp,
    )

    return Typography(
        bodyLarge = style(baseFont, FontWeight.Normal, 16f, 24f),
        bodyMedium = style(baseFont, FontWeight.Normal, 14f, 20f),
        bodySmall = style(baseFont, FontWeight.Normal, 12f, 16f),
        titleLarge = style(titleFont, FontWeight.Bold, 20f, 28f),
        titleMedium = style(titleFont, FontWeight.Bold, 17f, 24f),
        titleSmall = style(titleFont, FontWeight.Bold, 15f, 20f),
        labelLarge = style(baseFont, FontWeight.Bold, 15f, 20f),
        labelMedium = style(baseFont, FontWeight.Medium, 14f, 16f),
        labelSmall = style(baseFont, FontWeight.Medium, 12f, 16f),
        displayLarge = style(titleFont, FontWeight.Bold, 32f, 40f),
        displayMedium = style(titleFont, FontWeight.Bold, 28f, 36f),
        displaySmall = style(titleFont, FontWeight.Bold, 24f, 32f),
    )
}

val type @Composable get() = MaterialTheme.typography
