package com.cafarovceyxun.anamuslim.compose.components.settings

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
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetPinProvider
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.preferences.WidgetAppearancePreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerWidgetOpacitySubtitle
import com.cafarovceyxun.anamuslim.resources.prayerWidgetOpacityTitle
import com.cafarovceyxun.anamuslim.resources.prayerWidgetTextScaleSubtitle
import com.cafarovceyxun.anamuslim.resources.prayerWidgetTextScaleTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** [kind] vidcetinin fon qatılığı — digər vidcetlərə təsir etmir ([WidgetAppearancePreferences]). */
@Composable
fun WidgetOpacitySlider(kind: HomeWidgetKind, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    WidgetPercentSlider(
        title = Res.string.prayerWidgetOpacityTitle,
        subtitle = Res.string.prayerWidgetOpacitySubtitle,
        percent = WidgetAppearancePreferences.observeOpacityPercent(kind),
        range = WidgetAppearancePreferences.OPACITY_RANGE,
        step = WidgetAppearancePreferences.OPACITY_STEP,
        onChange = { scope.launch { WidgetAppearancePreferences.setOpacityPercent(kind, it) } },
        modifier = modifier,
    )
}

/** [kind] vidcetinin yazı ölçüsü — digər vidcetlərə təsir etmir. */
@Composable
fun WidgetTextScaleSlider(kind: HomeWidgetKind, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    WidgetPercentSlider(
        title = Res.string.prayerWidgetTextScaleTitle,
        subtitle = Res.string.prayerWidgetTextScaleSubtitle,
        percent = WidgetAppearancePreferences.observeTextScalePercent(kind),
        range = WidgetAppearancePreferences.TEXT_SCALE_RANGE,
        step = WidgetAppearancePreferences.TEXT_SCALE_STEP,
        onChange = { scope.launch { WidgetAppearancePreferences.setTextScalePercent(kind, it) } },
        modifier = modifier,
    )
}

/**
 * [ScrollStepSlider] ilə eyni quruluş: dəyər sürükləndikcə yazılır, çünki DataStore yazısı ucuzdur
 * və ekran (önizləmə daxil) onu dərhal göstərir. **Vidcetlərin yenidən çəkilməsi isə yalnız barmaq
 * qalxanda** olur ([Slider.onValueChangeFinished]) — hər dayanacaqda `updateAppWidgetState`
 * çağırmaq launcher-i onlarla yenidən çəkilişlə yükləyir və sürüşdürücü tutulur.
 */
@Composable
private fun WidgetPercentSlider(
    title: StringResource,
    subtitle: StringResource,
    percent: Int,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier,
) {
    val appLocale = LocalAppLocale.current

    val min = range.first
    val max = range.last
    // `steps` yalnız uc nöqtələr arasındakı dayanacaqları sayır.
    val steps = ((max - min) / step) - 1

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                modifier = Modifier.weight(1f),
                value = percent.toFloat(),
                onValueChange = { value ->
                    // `steps` thumb-u onsuz da dayanacağa oturdur; yuvarlaqlaşdırma saxlanan dəyəri
                    // təmiz misl saxlayır.
                    onChange(((value - min + step / 2f) / step).toInt() * step + min)
                },
                onValueChangeFinished = { HomeWidgetPinProvider.pinner.refreshPlacedWidgets() },
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
