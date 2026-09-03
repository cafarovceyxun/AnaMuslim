package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.app.BatteryOptimizationPrompt
import com.cafarovceyxun.anamuslim.compose.utils.app.ExactAlarmPrompt
import com.cafarovceyxun.anamuslim.compose.utils.app.openAppSettings
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberLocationPermission
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerWarningGrant
import com.cafarovceyxun.anamuslim.resources.prayerWarningLocation
import com.cafarovceyxun.anamuslim.resources.prayerWarningNotifications
import com.cafarovceyxun.anamuslim.resources.prayerWarningTitle
import org.jetbrains.compose.resources.stringResource

/**
 * Verilməmiş icazələr barədə xəbərdarlıq — hamısı verilənə qədər görünür.
 *
 * Niyə lazımdır: bu funksiyada icazə çatışmazlığı **səssizdir**. Bildiriş icazəsi olmayanda ayar
 * «açıq» görünür, amma heç nə çalmır; yer icazəsi olmayanda «Mövcud yerimi işlət» düyməsi basılır
 * və heç nə olmur; dəqiq siqnal icazəsi olmayanda bildiriş gəlir, sadəcə bir neçə dəqiqə gec.
 * Üç halın da səbəbi ekranda yazılmasa istifadəçi tətbiqi sınıq sayır.
 *
 * Xəbərdarlıq **yalnız real problem olanda** çıxır:
 * - yer icazəsi — yalnız yer hələ təyin edilməyibsə (şəhər seçilibsə GPS lazım deyil);
 * - bildiriş icazəsi — yalnız bildirişlər açıqdırsa;
 * - dəqiq siqnal və pil optimizasyonu — [ExactAlarmPrompt] və [BatteryOptimizationPrompt] öz
 *   şərtlərini özləri yoxlayır (iOS-da heç nə çəkmirlər).
 */
@Composable
fun PrayerPermissionBanner(modifier: Modifier = Modifier) {
    val settings = PrayerPreferences.observeSettings()
    val locationPermission = rememberLocationPermission()
    val notificationPermission = rememberNotificationPermission()

    val locationMissing = settings.point == null && !locationPermission.isGranted
    val notificationsMissing = settings.enabled &&
        notificationPermission != null && !notificationPermission.isGranted

    // `ExactAlarmPrompt` öz şərtini özü yoxlayır, ona görə burada yalnız «bildirişlər açıqdır»
    // filtri var; boş qutu çəkilməsin deyə çərçivə digər iki şərtdən asılıdır.
    if (!locationMissing && !notificationsMissing) {
        if (settings.enabled) {
            ExactAlarmPrompt()
            BatteryOptimizationPrompt()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shapes.large)
            .background(colorScheme.errorContainer.alpha(0.35f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.prayerWarningTitle),
            style = typography.titleSmall,
            color = colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
        )

        if (locationMissing) {
            WarningRow(text = stringResource(Res.string.prayerWarningLocation)) {
                // `canPrompt` false = sistem artıq soruşmayacaq → yeganə yol Ayarlardır.
                if (locationPermission.canPrompt) {
                    locationPermission.request()
                } else {
                    openAppSettings()
                }
            }
        }

        if (notificationsMissing) {
            WarningRow(text = stringResource(Res.string.prayerWarningNotifications)) {
                if (notificationPermission?.canPrompt == true) {
                    notificationPermission.request()
                } else {
                    openAppSettings()
                }
            }
        }

        if (settings.enabled) {
            ExactAlarmPrompt()
            BatteryOptimizationPrompt()
        }
    }
}

@Composable
private fun WarningRow(text: String, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = typography.bodyMedium,
            color = colorScheme.onErrorContainer.alpha(0.9f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(Res.string.prayerWarningGrant),
            style = typography.labelLarge,
            color = colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(shapes.small)
                .clickable(onClick = onGrant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
