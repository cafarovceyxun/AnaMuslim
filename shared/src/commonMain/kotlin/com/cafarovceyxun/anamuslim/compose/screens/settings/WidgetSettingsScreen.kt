package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.WidgetAppearancePreview
import com.cafarovceyxun.anamuslim.compose.components.settings.WidgetOpacitySlider
import com.cafarovceyxun.anamuslim.compose.components.settings.WidgetTextScaleSlider
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerTimesTitle
import com.cafarovceyxun.anamuslim.resources.prayerTimesWidgetWithLogo
import com.cafarovceyxun.anamuslim.resources.recitationPlayer
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.resources.widgetPreviewTitle
import com.cafarovceyxun.anamuslim.resources.widgetSettingsNote
import com.cafarovceyxun.anamuslim.resources.widgetSettingsTitle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Vidcet görünüşü: önizləmə + seçilmiş vidcet növünün fonu və yazı ölçüsü.
 *
 * İki yerdən açılır və ikisi də eyni ekrandır: ana ekranda vidcetə uzun basanda launcher-in
 * «Ayarlar» düyməsi (Android-in vidcet konfiqurasiya Activity-si, `ActivityWidgetSettings`) və
 * tətbiq Ayarlarındakı «Vidcet görünüşü» sətri. Birincidə [initialKind] həmin vidcetin növüdür və
 * [placedSize] onun launcher-dəki həqiqi ölçüsüdür — önizləmə məhz o vidceti göstərir.
 *
 * Ayarlar növə görədir ([com.cafarovceyxun.anamuslim.compose.utils.preferences.WidgetAppearancePreferences]):
 * pleyerin fonu namaz vidcetinə toxunmur. Dəyişiklik dərhal yazılır, «Yadda saxla» düyməsi yoxdur.
 *
 * Parametrlərin default-u **yoxdur** (CLAUDE.md: paylaşılan ekrana default-lu platforma davranışı
 * vermə) — hostu yalnız Android-dir və hər dəyəri o verir.
 */
@Composable
fun WidgetSettingsScreen(
    initialKind: HomeWidgetKind,
    placedSize: DpSize?,
    weekdayName: (Long) -> String,
) {
    var kind by rememberSaveable { mutableStateOf(initialKind) }

    Scaffold(
        topBar = { AppBar(stringResource(Res.string.widgetSettingsTitle)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeWidgetKind.entries.forEach { entry ->
                    FilterChip(
                        selected = entry == kind,
                        onClick = { kind = entry },
                        label = { Text(stringResource(titleOf(entry))) },
                    )
                }
            }

            ListItemCategoryLabel(title = stringResource(Res.string.widgetPreviewTitle))

            WidgetAppearancePreview(
                kind = kind,
                // Həqiqi ölçü yalnız konfiqurasiya olunan vidcetə aiddir; digər növlər üçün təxmin.
                placedSize = placedSize?.takeIf { kind == initialKind },
                weekdayName = weekdayName,
            )

            SettingsGroup {
                item { WidgetTextScaleSlider(kind) }
                item { WidgetOpacitySlider(kind) }
            }

            Text(
                text = stringResource(Res.string.widgetSettingsNote),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun titleOf(kind: HomeWidgetKind): StringResource = when (kind) {
    HomeWidgetKind.RecitationPlayer -> Res.string.recitationPlayer
    HomeWidgetKind.VerseOfTheDay -> Res.string.strTitleVOTD
    HomeWidgetKind.PrayerTimes -> Res.string.prayerTimesTitle
    HomeWidgetKind.PrayerTimesWithLogo -> Res.string.prayerTimesWidgetWithLogo
}
