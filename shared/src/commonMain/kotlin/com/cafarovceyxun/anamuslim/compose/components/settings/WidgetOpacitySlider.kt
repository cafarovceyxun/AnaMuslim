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
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetPinProvider
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerWidgetOpacitySubtitle
import com.cafarovceyxun.anamuslim.resources.prayerWidgetOpacityTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Ana ekran vidcetlərinin fon qatılığı — bütün beşi (namaz sadə/logolu, günün ayəsi, pleyer) eyni
 * [PrayerPreferences.KEY_WIDGET_OPACITY] dəyərini oxuyur.
 *
 * [ScrollStepSlider] ilə eyni quruluş: dəyər sürükləndikcə yazılır, çünki DataStore yazısı ucuzdur
 * və ekran onu dərhal göstərir. **Vidcetlərin yenidən çəkilməsi isə yalnız barmaq qalxanda** olur
 * ([Slider.onValueChangeFinished]) — hər dayanacaqda `updateAppWidgetState` çağırmaq launcher-i
 * onlarla yenidən çəkilişlə yükləyir və sürüşdürücü tutulur.
 */
@Composable
fun WidgetOpacitySlider(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current
    val percent = PrayerPreferences.observeWidgetOpacityPercent()

    val min = PrayerPreferences.WIDGET_OPACITY_RANGE.first
    val max = PrayerPreferences.WIDGET_OPACITY_RANGE.last
    // `steps` yalnız uc nöqtələr arasındakı dayanacaqları sayır.
    val steps = ((max - min) / PrayerPreferences.WIDGET_OPACITY_STEP) - 1

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.prayerWidgetOpacityTitle),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.prayerWidgetOpacitySubtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                modifier = Modifier.weight(1f),
                value = percent.toFloat(),
                onValueChange = { value ->
                    val step = PrayerPreferences.WIDGET_OPACITY_STEP
                    // `steps` thumb-u onsuz da dayanacağa oturdur; yuvarlaqlaşdırma saxlanan dəyəri
                    // təmiz misl saxlayır.
                    val snapped = ((value - min) / step).toInt() * step + min
                    scope.launch { PrayerPreferences.setWidgetOpacityPercent(snapped) }
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
