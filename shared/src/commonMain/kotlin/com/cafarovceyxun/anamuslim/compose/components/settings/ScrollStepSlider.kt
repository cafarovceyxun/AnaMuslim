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
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.scrollStepSubtitle
import com.cafarovceyxun.anamuslim.resources.strTitleScrollStep
import com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The one control for how far a key press (volume / S Pen / page keys) scrolls a reader — shared by
 * the Quran and hadith readers. Modelled on the text-size sliders: a percentage the user drags, in
 * the same visual language as font size, rather than the old three fixed "small / medium / large"
 * pixel choices. The value is a share of the viewport; see [ReaderScrollStep].
 *
 * Writes straight to [AppPreferences]; both readers observe it live.
 */
@Composable
fun ScrollStepSlider(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current
    val percent = AppPreferences.observeReaderScrollStepPercent()

    val min = ReaderScrollStep.MIN_PERCENT
    val max = ReaderScrollStep.MAX_PERCENT
    // Discrete notches between endpoints: Slider's `steps` counts the interior stops only.
    val steps = ((max - min) / ReaderScrollStep.PERCENT_STEP) - 1

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.strTitleScrollStep),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.scrollStepSubtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                modifier = Modifier.weight(1f),
                value = percent.toFloat(),
                onValueChange = {
                    // Snap to the notch: `steps` already quantises the thumb, but rounding keeps the
                    // stored value on a clean multiple even if a fractional value slips through.
                    val snapped = (it / ReaderScrollStep.PERCENT_STEP).toInt() * ReaderScrollStep.PERCENT_STEP
                    scope.launch { AppPreferences.setReaderScrollStepPercent(snapped) }
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
