package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat.ltrDigits
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.utils.PrayerReminderProvider
import com.cafarovceyxun.anamuslim.compose.utils.app.openAppSettings
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_location
import com.cafarovceyxun.anamuslim.resources.dr_icon_prayer_times
import com.cafarovceyxun.anamuslim.resources.lunarCalendarTitle
import com.cafarovceyxun.anamuslim.resources.lunarOffsetSubtitle
import com.cafarovceyxun.anamuslim.resources.lunarOffsetTitle
import com.cafarovceyxun.anamuslim.resources.lunarOffsetValue
import com.cafarovceyxun.anamuslim.resources.msgVerseReminderNotifPermission
import com.cafarovceyxun.anamuslim.resources.notification_permission
import com.cafarovceyxun.anamuslim.resources.prayerAngleValue
import com.cafarovceyxun.anamuslim.resources.prayerCalculationTitle
import com.cafarovceyxun.anamuslim.resources.prayerFajrAngle
import com.cafarovceyxun.anamuslim.resources.prayerIshaAngle
import com.cafarovceyxun.anamuslim.resources.prayerLocationNotSet
import com.cafarovceyxun.anamuslim.resources.prayerLocationTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotificationsTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotifySubtitle
import com.cafarovceyxun.anamuslim.resources.prayerOffsetValue
import com.cafarovceyxun.anamuslim.resources.prayerOffsetsSubtitle
import com.cafarovceyxun.anamuslim.resources.prayerOffsetsTitle
import com.cafarovceyxun.anamuslim.resources.prayerSoundSheetTitle
import com.cafarovceyxun.anamuslim.resources.prayerTimesTitle
import com.cafarovceyxun.anamuslim.resources.prayerUseElevation
import com.cafarovceyxun.anamuslim.resources.prayerUseElevationDesc
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelGotIt
import com.cafarovceyxun.anamuslim.resources.strLabelOpenSettings
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerParams
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round

/**
 * Namaz vaxtlarının bütün ayarları. `DailyReminderSheet` naxışını izləyir — bildiriş icazəsi
 * itəndə seçim avtomatik söndürülür, yoxsa istifadəçi «açıqdır, amma gəlmir» halında qalır.
 */
@Composable
fun PrayerSettingsSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.dr_icon_prayer_times,
        title = stringResource(Res.string.prayerTimesTitle),
    ) {
        PrayerSettingsSection(modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()))
    }
}

/**
 * Ayarların özü — vərəqdən **ayrıdır**, çünki namaz ekranı onu birbaşa öz axınının altında
 * göstərir. Bir məzmun, iki yer: ekranda inline, Ayarlar tabından isə vərəq kimi.
 */
@Composable
fun PrayerSettingsSection(modifier: Modifier = Modifier) {
    val settings = PrayerPreferences.observeSettings()
    val scope = rememberCoroutineScope()

    val notificationPermission = rememberNotificationPermission()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }

    // Hansı namazın səs vərəqi açıqdır; null = bağlıdır.
    var soundPickerFor by remember { mutableStateOf<Prayer?>(null) }

    /**
     * Hər ayar dəyişikliyindən sonra növbəni yenidən qur.
     *
     * Tək yerdə saxlanılır: çağırışları hər keçidə, hər sürüşdürücüyə və şəhər seçiminə ayrı-ayrı
     * səpsək, biri gec-tez unudulur və istifadəçi «ayar dəyişdi, bildiriş köhnə qaldı» halında
     * qalır. `settings` data sinfi olduğu üçün effekt yalnız real dəyişiklikdə işə düşür.
     */
    LaunchedEffect(settings) {
        if (settings.canSchedule) {
            PrayerReminderProvider.scheduler.schedule()
        } else {
            PrayerReminderProvider.scheduler.cancel()
        }
    }

    // ⚠️ Burada `DailyReminderSheet`-dən QƏSDƏN ayrılırıq: o, icazə yoxdursa ayarı avtomatik
    // söndürür. Namaz üçün bu, iki səbəbdən pisdir — (a) istifadəçinin niyyəti («xatırlat») səssizcə
    // itir, (b) söndürülmüş ayar «bildiriş gəlmir» xəbərdarlığını da yox edir, çünki xəbərdarlıq
    // məhz «açıqdır, amma işləmir» halını izah edir. Əvəzinə niyyət saxlanılır və səbəb
    // `PrayerPermissionBanner`-də göstərilir.

    suspend fun enableNotifications(): Boolean {
        if (notificationPermission != null && !notificationPermission.isGranted) {
            showPermissionDialog = true
            return false
        }
        return true
    }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        SettingsItem(
            title = Res.string.prayerLocationTitle,
            subtitleStr = settings.placeName.ifBlank { stringResource(Res.string.prayerLocationNotSet) },
            icon = Res.drawable.dr_icon_location,
            flat = true,
        ) { showCityPicker = true }

        HorizontalDivider()
        SectionLabel(stringResource(Res.string.prayerNotificationsTitle))

        SwitchItem(
            title = Res.string.prayerNotificationsTitle,
            subtitle = Res.string.prayerNotifySubtitle,
            checked = settings.enabled,
            onCheckedChange = { wanted ->
                scope.launch {
                    if (!wanted) {
                        PrayerPreferences.setEnabled(false)
                    } else if (enableNotifications()) {
                        PrayerPreferences.setEnabled(true)
                    }
                }
            },
        )

        // Hər vaxt üçün ayrıca keçid və ayrıca səs. Günəş ibadət vaxtı deyil, ona görə default
        // sönülüdür, amma siyahıda qalır — bəziləri şüruq üçün xatırlatma istəyir.
        Prayer.entries.forEach { prayer ->
            SwitchItem(
                title = PrayerUiFormat.labelOf(prayer),
                checked = prayer in settings.notify,
                enabled = settings.enabled,
                onCheckedChange = { checked ->
                    val updated =
                        if (checked) settings.notify + prayer else settings.notify - prayer
                    scope.launch { PrayerPreferences.setNotify(updated) }
                },
            )

            // Səs seçimi ayrıca sətirdir: `SwitchItem`-in bütün sahəsi keçidi çevirir, ona görə
            // eyni sətrin içində ikinci bir toxunma hədəfi gizli tələ olardı.
            SettingsItem(
                title = Res.string.prayerSoundSheetTitle,
                subtitleStr = stringResource(titleOf(settings.soundOf(prayer))),
                flat = true,
                enabled = settings.enabled && prayer in settings.notify,
                modifier = Modifier.padding(start = 16.dp),
            ) { soundPickerFor = prayer }
        }

        HorizontalDivider()
        SectionLabel(stringResource(Res.string.prayerCalculationTitle))

        AngleSlider(
            label = stringResource(Res.string.prayerFajrAngle),
            value = settings.params.fajrAngle,
        ) { scope.launch { PrayerPreferences.setAngles(it, settings.params.ishaAngle) } }

        AngleSlider(
            label = stringResource(Res.string.prayerIshaAngle),
            value = settings.params.ishaAngle,
        ) { scope.launch { PrayerPreferences.setAngles(settings.params.fajrAngle, it) } }

        SwitchItem(
            title = Res.string.prayerUseElevation,
            subtitle = Res.string.prayerUseElevationDesc,
            checked = settings.params.useElevation,
            onCheckedChange = { scope.launch { PrayerPreferences.setUseElevation(it) } },
        )

        HorizontalDivider()
        SectionLabel(stringResource(Res.string.prayerOffsetsTitle))
        Text(
            text = stringResource(Res.string.prayerOffsetsSubtitle),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Prayer.entries.forEach { prayer ->
            OffsetRow(
                label = PrayerUiFormat.label(prayer),
                minutes = settings.params.offsetOf(prayer),
            ) { delta ->
                val next = (settings.params.offsetOf(prayer) + delta)
                    .coerceIn(PrayerParams.OFFSET_RANGE)
                val updated = settings.params.offsetMinutes.toMutableMap()
                if (next == 0) updated.remove(prayer) else updated[prayer] = next
            scope.launch { PrayerPreferences.setOffsets(updated) }
        }
        }

        HorizontalDivider()
        SectionLabel(stringResource(Res.string.lunarCalendarTitle))
        Text(
            text = stringResource(Res.string.lunarOffsetSubtitle),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        StepperRow(
            label = stringResource(Res.string.lunarOffsetTitle),
            valueText = stringResource(
                Res.string.lunarOffsetValue,
                formatSigned(settings.lunarOffsetDays),
            ),
            isDefault = settings.lunarOffsetDays == 0,
        ) { delta ->
            scope.launch { PrayerPreferences.setLunarOffset(settings.lunarOffsetDays + delta) }
        }
    }

    CityPickerSheet(isOpen = showCityPicker, onClose = { showCityPicker = false })

    AdhanSoundSheet(
        prayer = soundPickerFor,
        selected = soundPickerFor?.let { settings.soundOf(it) } ?: AdhanSound.DEFAULT,
        onSelect = { sound ->
            val prayer = soundPickerFor ?: return@AdhanSoundSheet
            scope.launch { PrayerPreferences.setSound(prayer, sound) }
            soundPickerFor = null
        },
        onClose = { soundPickerFor = null },
    )

    AlertDialog(
        isOpen = showPermissionDialog,
        onClose = { showPermissionDialog = false },
        title = stringResource(Res.string.notification_permission),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(
                    if (notificationPermission?.canPrompt != false) {
                        Res.string.strLabelGotIt
                    } else {
                        Res.string.strLabelOpenSettings
                    }
                ),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    // Qərar KLİK anında oxunur — dialoq açılanda hesablanan snepşot istifadəçi arxa
                    // fondan qayıdanda köhnəlmiş olurdu.
                    notificationPermission?.let {
                        if (it.canPrompt) it.request() else openAppSettings()
                    }
                    showPermissionDialog = false
                },
            ),
        ),
        content = {
            Text(
                text = stringResource(Res.string.msgVerseReminderNotifPermission),
                style = typography.bodyMedium,
            )
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = typography.labelLarge,
        color = colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun AngleSlider(label: String, value: Double, onChange: (Double) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = typography.bodyLarge)
            Text(
                text = stringResource(Res.string.prayerAngleValue, formatAngle(value)),
                style = typography.bodyLarge.ltrDigits(),
                color = colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(round(it * 2.0) / 2.0) },
            valueRange = PrayerParams.ANGLE_RANGE.start.toFloat()..PrayerParams.ANGLE_RANGE.endInclusive.toFloat(),
            // Yarım dərəcəlik addım: 8.0–20.0 aralığında 24 addım.
            steps = 23,
        )
    }
}

@Composable
private fun OffsetRow(label: String, minutes: Int, onStep: (Int) -> Unit) {
    StepperRow(
        label = label,
        valueText = stringResource(Res.string.prayerOffsetValue, formatSigned(minutes)),
        isDefault = minutes == 0,
        onStep = onStep,
    )
}

/**
 * «− dəyər +» sətri. Namaz dəqiqə düzəlişləri və qəməri gün düzəlişi eyni görünüşü paylaşır —
 * chevron/rəng məntiqi iki yerdə təkrarlansaydı biri gec-tez digərindən sürüşərdi.
 *
 * [isDefault] yalnız rəng üçündür: toxunulmamış dəyər sönük, dəyişdirilmiş dəyər vurğulu yazılır.
 */
@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    isDefault: Boolean,
    onStep: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = typography.bodyLarge, modifier = Modifier.weight(1f))

        IconButton(onClick = { onStep(-1) }) {
            androidx.compose.material3.Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                contentDescription = null,
            )
        }
        Text(
            text = valueText,
            style = typography.bodyMedium.ltrDigits(),
            color = if (isDefault) colorScheme.onSurfaceVariant else colorScheme.primary,
        )
        IconButton(onClick = { onStep(1) }) {
            androidx.compose.material3.Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
            )
        }
    }
}

/**
 * `+5` / `−5` / `0` — dəqiqə də, gün də.
 *
 * ⚠️ Sətirdə `%1$+d` **işlədilə bilməz**: Compose Resources-un formatlayıcısı Android `getString`-dən
 * fərqli olaraq işarə bayrağını açmır və ekranda hərfi `%1$+d` görünür — kompilyator da, testlər də
 * susur, yalnız ekran göstərir. (CLAUDE.md-dəki `%%` tələsinin eyni ailəsi.)
 */
private fun formatSigned(value: Int): String =
    if (value > 0) "+$value" else value.toString()

/** `12.0` / `12.5` — yarım dərəcəlik addımda üçüncü rəqəm mənasızdır. */
private fun formatAngle(value: Double): String {
    val halves = round(value * 2.0).toInt()
    return "${halves / 2}.${if (halves % 2 == 0) "0" else "5"}"
}
