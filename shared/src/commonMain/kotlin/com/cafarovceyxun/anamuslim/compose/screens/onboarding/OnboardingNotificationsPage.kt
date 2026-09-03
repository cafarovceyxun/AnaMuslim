package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.prayer.CityPickerSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.DailyReminderProvider
import com.cafarovceyxun.anamuslim.compose.utils.PrayerReminderProvider
import com.cafarovceyxun.anamuslim.compose.utils.app.BatteryOptimizationPrompt
import com.cafarovceyxun.anamuslim.compose.utils.app.ExactAlarmPrompt
import com.cafarovceyxun.anamuslim.compose.utils.app.NotificationPermissionState
import com.cafarovceyxun.anamuslim.compose.utils.app.openAppSettings
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberLocationPermission
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dailyReminderMsg
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_location
import com.cafarovceyxun.anamuslim.resources.dr_icon_prayer_times
import com.cafarovceyxun.anamuslim.resources.ic_bell
import com.cafarovceyxun.anamuslim.resources.onboardAllowNotifications
import com.cafarovceyxun.anamuslim.resources.onboardNotificationsDeniedHint
import com.cafarovceyxun.anamuslim.resources.onboardNotificationsGranted
import com.cafarovceyxun.anamuslim.resources.onboardNotificationsRequired
import com.cafarovceyxun.anamuslim.resources.prayerLocationNotSet
import com.cafarovceyxun.anamuslim.resources.prayerLocationTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotificationsTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotifySubtitle
import com.cafarovceyxun.anamuslim.resources.strLabelOpenSettings
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Onboarding-in 4-cü səhifəsi: bildiriş icazəsi + **iki ayrı** xatırlatma açarı.
 *
 * Niyə ayrı açarlar: namaz vaxtı bildirişi ilə günün ayəsi bildirişi fərqli şeylərdir — ayrı
 * planlayıcı, ayrı Android kanalı, ayrı ayar. Hər ikisi default **sönülüdür** və indiyə qədər
 * yalnız Ayarların dərinliyində görünürdü, yəni istifadəçilərin çoxu bildiriş icazəsini verib heç
 * bir bildiriş almırdı. Burada seçim şüurlu edilir.
 *
 * [permission] **yuxarıdan verilir**, burada `rememberNotificationPermission()` çağırılmır:
 * [OnboardingScreen] onu onsuz da oxuyur («Başla» düyməsinin `enabled` şərti buna bağlıdır) və iki
 * ayrı çağırış iki ayrı `ON_RESUME` abunəliyi deməkdir — düymə ilə səhifə bir kadr fərqlə ayrı
 * vəziyyət göstərə bilərdi. `null` = Android 12 və aşağısı: soruşulası bir şey yoxdur, icazə bloku
 * ümumiyyətlə çəkilmir və açarlar aktivdir.
 */
@Composable
fun OnboardingNotificationsPage(
    permission: NotificationPermissionState?,
) {
    val scope = rememberCoroutineScope()
    val settings = PrayerPreferences.observeSettings()
    val votdEnabled = VersePreferences.observeVOTDReminderEnabled()
    val location = rememberLocationPermission()

    var showCityPicker by remember { mutableStateOf(false) }

    // Qapı: `null` (Android 12−) və ya icazə verilib → açarlar işləyir.
    val gateOpen = permission == null || permission.isGranted

    /**
     * Namaz növbəsi TƏK yerdə qurulur — həm açar, həm şəhər seçimi buna düşür.
     *
     * Bu effekt olmasa istifadəçi onboarding-də açarı yandırır, şəhəri seçir, «Başla»ya basır və
     * namaz ekranını açana qədər **heç bir bildiriş planlanmır**. `PrayerSettingsSection`-dakı eyni
     * effektin qardaşıdır; `settings` data sinfi olduğu üçün yalnız real dəyişiklikdə işə düşür.
     */
    LaunchedEffect(settings) {
        if (settings.canSchedule) {
            PrayerReminderProvider.scheduler.schedule()
        } else {
            PrayerReminderProvider.scheduler.cancel()
        }
    }

    val scrollState = rememberScrollState()

    Box(Modifier.verticalFadingEdge(scrollState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            PermissionBlock(permission = permission)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = colorScheme.outline.alpha(0.2f),
            )

            SwitchItem(
                title = Res.string.prayerNotificationsTitle,
                subtitle = Res.string.prayerNotifySubtitle,
                icon = Res.drawable.dr_icon_prayer_times,
                checked = settings.enabled,
                enabled = gateOpen,
                onCheckedChange = { wanted ->
                    scope.launch { PrayerPreferences.setEnabled(wanted) }
                    // Namaz vaxtı yer olmadan hesablanmır. Sistem dialoqu burada atılır, çünki
                    // istifadəçi məhz indi «namaz bildirişi istəyirəm» dedi — kontekst ən aydın andır.
                    if (wanted && !location.isGranted && location.canPrompt) location.request()
                },
            )

            // Şəhər sətri yalnız namaz bildirişləri açıq olanda mənalıdır — bağlıykən səhifəni
            // uzadıb diqqəti dağıdır.
            AnimatedVisibility(visible = settings.enabled) {
                Column {
                    SettingsItem(
                        title = Res.string.prayerLocationTitle,
                        subtitleStr = settings.placeName
                            .ifBlank { stringResource(Res.string.prayerLocationNotSet) },
                        icon = Res.drawable.dr_icon_location,
                        flat = true,
                    ) { showCityPicker = true }

                    // Hər ikisi öz şərtini özü yoxlayır və şərt ödənməyəndə heç nə çəkmir
                    // (iOS-da hər ikisi `Unit`-dir).
                    ExactAlarmPrompt()
                    BatteryOptimizationPrompt()
                }
            }

            SwitchItem(
                title = Res.string.strTitleVOTD,
                subtitle = Res.string.dailyReminderMsg,
                icon = Res.drawable.dr_icon_heart_filled,
                checked = votdEnabled,
                enabled = gateOpen,
                onCheckedChange = { wanted ->
                    scope.launch {
                        VersePreferences.setVOTDReminderEnabled(wanted)
                        if (wanted) {
                            DailyReminderProvider.scheduler.schedule()
                        } else {
                            DailyReminderProvider.scheduler.cancel()
                        }
                    }
                },
            )
        }
    }

    // Yer icazəsi daimi rədd edilsə də burada blok YOXDUR: şəhər kataloqu oflayndır, istifadəçi
    // şəhəri əl ilə seçib namaz vaxtlarını tam işlək hala gətirə bilər.
    CityPickerSheet(isOpen = showCityPicker, onClose = { showCityPicker = false })
}

/**
 * İcazənin üç halı. Ayarlardan qayıdış üçün əlavə kod yoxdur — hər iki platformanın actual-ı
 * statusu `ON_RESUME`-da yenidən oxuyur, ona görə blok öz-özünə dəyişir.
 */
@Composable
private fun PermissionBlock(permission: NotificationPermissionState?) {
    if (permission == null || permission.isGranted) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_check_circle),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.onboardNotificationsGranted),
                style = typography.bodyMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.onboardNotificationsRequired),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                // Sistem dialoqu, yoxsa Ayarlar — qərar KLİK anında oxunur.
                if (permission.canPrompt) permission.request() else openAppSettings()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_bell),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(
                    if (permission.canPrompt) {
                        Res.string.onboardAllowNotifications
                    } else {
                        Res.string.strLabelOpenSettings
                    }
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Daimi rədddən sonra istifadəçi «niyə düymə məni Ayarlara atır» sualında qalmasın.
        if (!permission.canPrompt) {
            Text(
                text = stringResource(Res.string.onboardNotificationsDeniedHint),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}
