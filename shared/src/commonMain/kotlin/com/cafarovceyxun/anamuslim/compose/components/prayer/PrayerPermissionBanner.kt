package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.cafarovceyxun.anamuslim.compose.utils.app.SystemSettingAccess
import com.cafarovceyxun.anamuslim.compose.utils.app.openAppSettings
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberBatteryOptimizationAccess
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberExactAlarmAccess
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberLocationPermission
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.prayerWarningBattery
import com.cafarovceyxun.anamuslim.resources.prayerWarningExactAlarm
import com.cafarovceyxun.anamuslim.resources.prayerWarningGrant
import com.cafarovceyxun.anamuslim.resources.prayerWarningLocation
import com.cafarovceyxun.anamuslim.resources.prayerWarningNotifications
import com.cafarovceyxun.anamuslim.resources.prayerWarningTitle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Verilməmiş icazələr barədə xəbərdarlıq — hamısı verilənə qədər görünür.
 *
 * Niyə lazımdır: bu funksiyada icazə çatışmazlığı **səssizdir**. Bildiriş icazəsi olmayanda ayar
 * «açıq» görünür, amma heç nə çalmır; yer icazəsi olmayanda «Mövcud yerimi işlət» düyməsi basılır
 * və heç nə olmur; dəqiq siqnal icazəsi olmayanda bildiriş gəlir, amma bir saata qədər gec.
 * Səbəb ekranda yazılmasa istifadəçi tətbiqi sınıq sayır.
 *
 * Xəbərdarlıq **yalnız real problem olanda** çıxır:
 * - yer icazəsi — yalnız yer hələ təyin edilməyibsə (şəhər seçilibsə GPS lazım deyil);
 * - bildiriş, dəqiq siqnal, pil optimizasyonu — yalnız bildirişlər açıqdırsa.
 *
 * Bütün sətirlər **eyni kartda, eyni düymə ilə** çəkilir. Əvvəllər dəqiq siqnal və pil sətirləri
 * kartdan kənarda, düyməsiz adi rəngli mətn idi — istifadəçi onları icazə istəyi kimi tanımırdı.
 */
@Composable
fun PrayerPermissionBanner(modifier: Modifier = Modifier) {
    val settings = PrayerPreferences.observeSettings()
    val locationPermission = rememberLocationPermission()
    val notificationPermission = rememberNotificationPermission()
    val deliveryRows = rememberDeliveryRows()

    val rows = buildList {
        if (settings.point == null && !locationPermission.isGranted) {
            add(
                PermissionRow(Res.string.prayerWarningLocation) {
                    // `canPrompt` false = sistem artıq soruşmayacaq → yeganə yol Ayarlardır.
                    if (locationPermission.canPrompt) locationPermission.request() else openAppSettings()
                }
            )
        }

        // `wantsNotifications`: namaz bildirişləri sönülü ola-ola zikr xatırlatması açıq ola bilər —
        // icazə xəbərdarlığı hər iki halda lazımdır.
        if (settings.wantsNotifications) {
            if (notificationPermission != null && !notificationPermission.isGranted) {
                add(
                    PermissionRow(Res.string.prayerWarningNotifications) {
                        if (notificationPermission.canPrompt) {
                            notificationPermission.request()
                        } else {
                            openAppSettings()
                        }
                    }
                )
            }
            addAll(deliveryRows)
        }
    }

    PermissionWarningCard(
        rows = rows,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * Yalnız çatdırma icazələri (dəqiq siqnal + pil optimizasyonu) — tanışlıq ekranı üçün.
 *
 * Orada bildiriş icazəsi və yer öz bloklarında soruşulur, ona görə bu kart yalnız qalan ikisini
 * göstərir. Hər ikisi verilibsə (və iOS-da həmişə) heç nə çəkmir.
 */
@Composable
fun PrayerDeliveryPermissionCard(modifier: Modifier = Modifier) {
    PermissionWarningCard(rows = rememberDeliveryRows(), modifier = modifier)
}

@Composable
private fun rememberDeliveryRows(): List<PermissionRow> {
    val exactAlarm = rememberExactAlarmAccess()
    val battery = rememberBatteryOptimizationAccess()

    return buildList {
        exactAlarm.missingRow(Res.string.prayerWarningExactAlarm)?.let(::add)
        battery.missingRow(Res.string.prayerWarningBattery)?.let(::add)
    }
}

private fun SystemSettingAccess?.missingRow(text: StringResource): PermissionRow? =
    if (this == null || isGranted) null else PermissionRow(text, this::open)

private class PermissionRow(val text: StringResource, val onGrant: () -> Unit)

@Composable
private fun PermissionWarningCard(rows: List<PermissionRow>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colorScheme.errorContainer.alpha(0.55f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_info),
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(Res.string.prayerWarningTitle),
                style = typography.titleSmall,
                color = colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }

        rows.forEach { row -> WarningRow(text = stringResource(row.text), onGrant = row.onGrant) }
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
            color = colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        // Dolu düymə: sətrin «toxunula bilən» olduğu ilk baxışdan görünməlidir.
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.error,
                contentColor = colorScheme.onError,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            Text(
                text = stringResource(Res.string.prayerWarningGrant),
                style = typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
