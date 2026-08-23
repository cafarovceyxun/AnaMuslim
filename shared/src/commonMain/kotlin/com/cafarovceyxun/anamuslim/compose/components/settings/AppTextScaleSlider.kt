package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.AppTextScale
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.appTextSizeSubtitle
import com.cafarovceyxun.anamuslim.resources.strTitleAppTextSize
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Bütün interfeys mətnlərinin ölçüsünü bir yerdən idarə edən sürüşdürücü — bax [AppTextScale].
 *
 * Önizləmə sətri **qəsdən** yoxdur: sürüşdürücünün öz etiketləri də miqyaslanan tipoqrafiyadadır,
 * ona görə dəyişiklik elə orada, canlı görünür. Quran və hədis sürüşdürücülərində vəziyyət başqadır
 * (onlar yalnız oxucu mətnini dəyişir, ayarlar ekranını yox) — orada önizləmə lazımdır.
 *
 * [ScrollStepSlider] ilə eyni quruluşdadır: faiz, 5%-lik pillələr, sağda cari dəyər.
 */
@Composable
fun AppTextScaleSlider(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current
    val percent = AppPreferences.observeAppTextScalePercent()

    val min = AppTextScale.MIN_PERCENT
    val max = AppTextScale.MAX_PERCENT
    // Slider's `steps` counts the interior notches only.
    val steps = ((max - min) / AppTextScale.PERCENT_STEP) - 1

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(Res.string.strTitleAppTextSize),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.appTextSizeSubtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                modifier = Modifier.weight(1f),
                value = percent.toFloat(),
                onValueChange = {
                    // `steps` artıq barmağı pillələrə oturdur; yuvarlaqlaşdırma saxlanan dəyərin
                    // təmiz misil qalmasına zəmanət verir.
                    val snapped = (it / AppTextScale.PERCENT_STEP).toInt() * AppTextScale.PERCENT_STEP
                    scope.launch { AppPreferences.setAppTextScalePercent(snapped) }
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = steps,
            )
            Text(
                text = "${appLocale.numeralSystem.formatNumber(percent)}%",
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
