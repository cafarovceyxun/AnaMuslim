package com.cafarovceyxun.anamuslim.views.widget

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind
import com.cafarovceyxun.anamuslim.compose.utils.preferences.WidgetAppearancePreferences

/**
 * Vidcetin yazı ölçüsü əmsalı (`1f` = 100%) — hər növün özününkü
 * ([WidgetAppearancePreferences.getTextScalePercent]).
 *
 * Hər `fontSize`-a ayrıca parametr ötürmək əvəzinə kompozisiya boyu paylanır: vidcetlərdə onlarla
 * mətn var və yeni yazılan birinin əmsalı unutması ayarı **səssizcə yarımçıq** edərdi (bir sətir
 * böyüyür, yanındakı yox). Ölçüləri [wsp] ilə yaz, `sp` ilə yox.
 */
internal val LocalWidgetTextScale = staticCompositionLocalOf { 1f }

/**
 * Əmsalı kompozisiyaya verir. Dəyəri `remember(glanceState) { currentWidgetTextScale(kind) }` ilə oxu,
 * `provideContent`-dən əvvəl yox: sessiya açıq ikən `update()` `provideGlance`-i təkrar çağırmır,
 * yalnız `glanceState`-i dəyişib kompozisiyanı yeniləyir (`refreshAllInstances`).
 */
@Composable
internal fun ProvideWidgetTextScale(scale: Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWidgetTextScale provides scale, content = content)
}

/** [kind] vidcetinin yazı əmsalı, `1f` = 100%. Bitmap çəkən kod da (günün ayəsi) bunu oxuyur. */
internal fun currentWidgetTextScale(kind: HomeWidgetKind): Float =
    WidgetAppearancePreferences.getTextScalePercent(kind) / 100f

/** [kind] vidcetinin fon qatılığı, `0..1`. */
internal fun currentWidgetBackgroundAlpha(kind: HomeWidgetKind): Float =
    WidgetAppearancePreferences.getOpacityPercent(kind) / 100f

/**
 * Telefonun sistem şrift miqyası (Ayarlar → Şrift ölçüsü). Launcher vidcetin hər `sp`-sini buna
 * vurur.
 *
 * `Resources.getSystem()`-dən oxunur, tətbiqin öz konfiqurasiyasından yox: `BaseActivity` öz
 * Activity-lərində miqyası `1`-ə kilidləyir və tətbiq resursları həmin dəyişikliyi görə bilər.
 */
internal fun systemFontScale(): Float =
    Resources.getSystem().configuration.fontScale.takeIf { it > 0f } ?: 1f

/**
 * Vidcet yazı ölçüsü: `19.wsp` = 19 × istifadəçinin əmsalı, **sistem şrift miqyasından asılı
 * olmayaraq**.
 *
 * ⚠️ Glance yalnız `sp` qəbul edir (RemoteViews `COMPLEX_UNIT_SP`), launcher isə onu sistem
 * miqyasına vurur. Telefonda şrift böyüdüləndə (məs. 1.3×) bizim ən kiçik əmsalımız da (80%)
 * vidceti böyük saxlayırdı — 0.8 × 1.3 ≈ 1.04 — və kiçiltmə ayarı «işləmirdi». Burada miqyasa
 * bölünür, launcher yenidən vurur, nəticədə ölçünü **yalnız** vidcet ayarı idarə edir. Tətbiqin öz
 * ekranları da sistem şriftindən asılı deyil (`BaseActivity`), vidcetlər indi onlarla eynidir.
 *
 * Miqyas dəyişəndə yerləşdirilmiş vidcetlər köhnə kompensasiya ilə qalır — ona görə
 * `QuranApp.onConfigurationChanged` onları yenidən çəkir.
 */
internal val Int.wsp: TextUnit
    @Composable get() = (this * LocalWidgetTextScale.current / systemFontScale()).sp
