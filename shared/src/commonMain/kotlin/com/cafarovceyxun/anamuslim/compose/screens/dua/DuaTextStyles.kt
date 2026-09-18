package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha

/**
 * Dua və Əsmaül Hüsnə mətn bloklarının ortaq stil qaydaları.
 *
 * Hər iki ekran eyni üç-bloklu quruluşu göstərir (ərəbcə → oxunuş → tərcümə), ona görə həm ayırıcı,
 * həm də sətir aralığı qaydası burada bir dəfə yazılır.
 */

/**
 * Sətir aralığını stilin **öz** `fontSize`-ından hesablayır.
 *
 * ⚠️ `lineHeight`-ı sabit `sp` ilə yazma. `typography` onsuz da `AppTextScale` ilə miqyaslanır
 * (`compose/theme/Type.kt` → `style(...)` hər iki dəyəri `f` faktoruna vurur), ona görə sabit
 * `lineHeight` yalnız 100%-də düzgün görünür: istifadəçi mətni 150% edəndə `fontSize` böyüyür,
 * `lineHeight` isə yerində qalır və sətirlər bir-birinə yapışır. Əksinə, 100%-də nisbət həddən
 * artıq havalı çıxır — `bodyLarge` (16sp) üzərinə yazılmış `17.sp * 1.7` faktiki olaraq 1.81
 * nisbət verirdi.
 *
 * Nümunə: `typography.bodyLarge.withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)`.
 */
internal fun TextStyle.withLineHeightRatio(ratio: Float): TextStyle =
    copy(lineHeight = fontSize * ratio)

/**
 * Tərcümə və qeyd kimi latın mətnləri üçün sətir aralığı nisbəti.
 *
 * Hədis tərcüməsi ilə eynidir (`HadithItemsScreen` → `17.sp * 1.6`) ki, iki ekran arasında keçəndə
 * mətnin sıxlığı dəyişməsin.
 */
internal const val TRANSLATION_LINE_HEIGHT_RATIO = 1.6f

/**
 * Tərcüməyə **bağlı** köməkçi sətirlərin (qeyd, mənbə/istinad) ondan neçə sp kiçik olduğu.
 *
 * Bu sətirləri sabit `typography` ölçüsü ilə yazmaq olmur: tərcümə pinch jesti ilə miqyaslanır
 * (`DuaPreferences.translationSizeMultiplier`), onlar isə yerində qalırdı — 150%-də mənbə
 * tərcümədən xeyli balaca, 70%-də isə ondan **böyük** görünürdü. Ona görə ölçü tərcümənin öz
 * `fontSize`-ından çıxılır və eyni çarpana vurulur; nəticədə nisbət hər miqyasda sabit qalır.
 *
 * Nümunə: `(typography.bodyLarge.fontSize.value - TRANSLATION_SUBTEXT_DROP_SP).sp * mult`.
 */
internal const val TRANSLATION_SUBTEXT_DROP_SP = 3f

/**
 * İki **görünən** mətn bloku arasındakı qısa dekorativ xətt.
 *
 * Çağıran tərəf bunu yalnız qonşu iki blokun ikisi də mövcud olanda emit etməlidir — boş tərcümə
 * (tək ilahi ad, qısa zikr) ekranda qoşa xətt buraxardı.
 */
@Composable
internal fun DuaBlockDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.width(72.dp),
        color = colorScheme.primary.alpha(0.35f),
        thickness = 1.dp,
    )
}

/**
 * Ərəbcə çıxarış blokları üçün sətir aralığı nisbəti.
 *
 * Latın mətnindən böyükdür, çünki ərəbcə hərəkələr sətrin üstündə və altında yer tutur — 1.6-da
 * qonşu sətirlərin hərəkələri bir-birinə dəyir.
 */
internal const val ARABIC_EXCERPT_LINE_HEIGHT_RATIO = 1.9f
