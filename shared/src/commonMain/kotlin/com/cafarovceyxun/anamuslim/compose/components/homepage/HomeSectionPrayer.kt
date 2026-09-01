package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat.ltrDigits
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_prayer_times
import com.cafarovceyxun.anamuslim.resources.prayerHomeCardEmpty
import com.cafarovceyxun.anamuslim.resources.prayerNextLabel
import com.cafarovceyxun.anamuslim.resources.prayerTimesTitle
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.prayer.NextPrayer
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerDay
import com.cafarovceyxun.anamuslim.utils.prayer.TimeSource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Ana ekranda namaz vaxtları — növbəti vaxt, geri sayım və günün altı vaxtı.
 *
 * Yer təyin edilməyibsə bölmə **gizlənmir**, dəvət göstərir: gizlənsə istifadəçi funksiyanın
 * mövcudluğunu heç vaxt bilməzdi (bölmə düzəndə sona görünən halda düşür).
 */
@Composable
fun HomeSectionPrayer() {
    val actions = LocalHomeActions.current
    val settings = PrayerPreferences.observeSettings()
    val point = settings.point

    // Ana ekranda saniyəlik geri sayım lazım deyil — dəqiqədə bir yenilənmə kifayətdir və
    // siyahı sürüşdürülərkən rekompozisiya yığılmır.
    val now by produceState(initialValue = currentEpochMillis()) {
        while (true) {
            value = currentEpochMillis()
            delay(30_000L)
        }
    }

    val todayIso = remember(now / 60_000L) { PrayerUiFormat.localDate(now) }

    val days = remember(todayIso, point, settings.params) {
        point?.let { PrayerDay.forLocalDates(todayIso, count = 2, at = it, params = settings.params) }
            .orEmpty()
    }
    val upcoming = remember(now / 30_000L, days, settings.notify) {
        NextPrayer.after(now, days, settings.notify.ifEmpty { Prayer.entries.toSet() })
    }

    HomeSectionContainer {
        HomeSectionHeader(
            icon = Res.drawable.dr_icon_prayer_times,
            title = Res.string.prayerTimesTitle,
            horizontalPadding = SECTION_CONTENT_PADDING,
            onViewAllClick = actions.onOpenPrayerTimes,
        )

        if (point == null) {
            Text(
                text = stringResource(Res.string.prayerHomeCardEmpty),
                style = typography.bodyMedium,
                color = colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = actions.onOpenPrayerTimes)
                    .padding(horizontal = SECTION_CONTENT_PADDING, vertical = 12.dp),
            )
            return@HomeSectionContainer
        }

        if (upcoming != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = actions.onOpenPrayerTimes)
                    .padding(horizontal = SECTION_CONTENT_PADDING),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            Res.string.prayerNextLabel,
                            PrayerUiFormat.label(upcoming.prayer),
                        ),
                        style = typography.titleSmall,
                        color = colorScheme.primary,
                    )
                    Text(
                        text = PrayerUiFormat.clock(upcoming.atMillis),
                        style = typography.titleMedium.ltrDigits(),
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = PrayerUiFormat.remaining((upcoming.atMillis - now).coerceAtLeast(0L)),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }

        days.firstOrNull()?.let { today ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = actions.onOpenPrayerTimes)
                    .padding(horizontal = SECTION_CONTENT_PADDING, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Prayer.entries.forEach { prayer ->
                    val time = today[prayer] ?: return@forEach
                    val isNext = prayer == upcoming?.prayer

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = PrayerUiFormat.label(prayer),
                            style = typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.alpha(0.9f),
                        )
                        Text(
                            text = if (time.source == TimeSource.ASTRONOMICAL) {
                                PrayerUiFormat.clock(time.atMillis)
                            } else {
                                "≈" + PrayerUiFormat.clock(time.atMillis)
                            },
                            style = typography.bodyMedium.ltrDigits(),
                            color = if (isNext) colorScheme.primary else colorScheme.onSurface,
                            fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
