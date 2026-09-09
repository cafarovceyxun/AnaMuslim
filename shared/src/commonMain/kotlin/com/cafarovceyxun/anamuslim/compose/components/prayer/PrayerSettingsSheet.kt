package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat.ltrDigits
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroupScope
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.utils.PrayerReminderProvider
import com.cafarovceyxun.anamuslim.compose.utils.app.openAppSettings
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
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
import com.cafarovceyxun.anamuslim.resources.prayerFollowUpTitle
import com.cafarovceyxun.anamuslim.resources.prayerFollowUpValue
import com.cafarovceyxun.anamuslim.resources.prayerIshaAngle
import com.cafarovceyxun.anamuslim.resources.prayerLocationNotSet
import com.cafarovceyxun.anamuslim.resources.prayerLocationTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotifyAllTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotificationsTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotifySubtitle
import com.cafarovceyxun.anamuslim.resources.prayerOffsetValue
import com.cafarovceyxun.anamuslim.resources.prayerOffsetsSubtitle
import com.cafarovceyxun.anamuslim.resources.prayerOffsetsTitle
import com.cafarovceyxun.anamuslim.resources.prayerReminderMinutesUnit
import com.cafarovceyxun.anamuslim.resources.prayerReminderTitle
import com.cafarovceyxun.anamuslim.resources.prayerReminderValue
import com.cafarovceyxun.anamuslim.resources.prayerSoundSheetTitle
import com.cafarovceyxun.anamuslim.resources.prayerTimesTitle
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDecrease
import com.cafarovceyxun.anamuslim.resources.strLabelGotIt
import com.cafarovceyxun.anamuslim.resources.strLabelIncrease
import com.cafarovceyxun.anamuslim.resources.strLabelOpenSettings
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerParams
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
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
 *
 * Düzülüş `SettingsMainScreen`/`HadithSettingsSheet` ilə eynidir: hər bölmə öz [SettingsGroup]
 * kartındadır. Əvvəl sətirlər kartsız, ekranın enində düz axırdı və altı vaxtın hər biri üç sətir
 * (keçid + səs + xəbərdarlıq) tuturdu — 18 sətir ayırdedici sərhəd olmadan yan-yana düşürdü,
 * hansı alt sətrin hansı vaxta aid olduğu görünmürdü.
 *
 * [showLocation] — namaz ekranının öz `LocationCard`-ı olduğu üçün orada sətir təkrarlanmasın deyə.
 */
@Composable
fun PrayerSettingsSection(
    modifier: Modifier = Modifier,
    showLocation: Boolean = true,
) {
    val settings = PrayerPreferences.observeSettings()
    val scope = rememberCoroutineScope()

    val notificationPermission = rememberNotificationPermission()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }

    // Hansı namazın səs vərəqi açıqdır; null = bağlıdır.
    var soundPickerFor by remember { mutableStateOf<Prayer?>(null) }

    // Alt ayarları (səs + xəbərdarlıq) açıq olan vaxt; null = hamısı yığılıb. Eyni anda **bir**
    // vaxt açılır — altısı birdən açıq qalsaydı siyahı yenə 18 sətrə qayıdardı.
    var expandedPrayer by remember { mutableStateOf<Prayer?>(null) }

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showLocation) {
            SettingsGroup {
                item {
                    SettingsItem(
                        title = Res.string.prayerLocationTitle,
                        subtitleStr = settings.placeName.ifBlank {
                            stringResource(Res.string.prayerLocationNotSet)
                        },
                        icon = Res.drawable.dr_icon_location,
                        flat = true,
                    ) { showCityPicker = true }
                }
            }
        }

        SettingsGroup(title = stringResource(Res.string.prayerNotificationsTitle)) {
            item {
                SwitchItem(
                    title = Res.string.prayerNotifyAllTitle,
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
            }

            // Hər vaxt üçün ayrıca keçid, səs və xəbərdarlıq. Günəş ibadət vaxtı deyil, ona görə
            // default sönülüdür, amma siyahıda qalır — bəziləri şüruq üçün xatırlatma istəyir.
            Prayer.entries.forEach { prayer ->
                item {
                    val notifying = prayer in settings.notify
                    val reminder = settings.reminderMinutes[prayer] ?: 0
                    val followUp = settings.followUpMinutes[prayer] ?: 0
                    val soundLabel = stringResource(titleOf(settings.soundOf(prayer)))

                    // Yığılmış sətir vəziyyəti özü danışır: səs adı, sonra qurulmuş xatırlatmalar.
                    // Açmadan da görünür ki, hansı vaxt necə qurulub.
                    val summary = listOfNotNull(
                        soundLabel,
                        stringResource(Res.string.prayerReminderValue, reminder).takeIf { reminder > 0 },
                        stringResource(Res.string.prayerFollowUpValue, followUp).takeIf { followUp > 0 },
                    ).joinToString(" · ")

                    PrayerNotifyRow(
                        title = PrayerUiFormat.label(prayer),
                        icon = PrayerUiFormat.iconOf(prayer),
                        summary = summary,
                        checked = notifying,
                        enabled = settings.enabled,
                        expanded = expandedPrayer == prayer,
                        soundLabel = soundLabel,
                        reminderMinutes = reminder,
                        followUpMinutes = followUp,
                        onCheckedChange = { checked ->
                            // Söndürülən vaxtın açıq qalmış alt ayarları da yığılır, yoxsa ekranda
                            // toxunula bilməyən sətirlər qalırdı.
                            if (!checked && expandedPrayer == prayer) expandedPrayer = null
                            val updated =
                                if (checked) settings.notify + prayer else settings.notify - prayer
                            scope.launch { PrayerPreferences.setNotify(updated) }
                        },
                        onToggleExpand = {
                            expandedPrayer = if (expandedPrayer == prayer) null else prayer
                        },
                        onSound = { soundPickerFor = prayer },
                        onReminderChange = { minutes ->
                            val updated = settings.reminderMinutes.toMutableMap()
                            if (minutes == 0) updated.remove(prayer) else updated[prayer] = minutes
                            scope.launch { PrayerPreferences.setReminders(updated) }
                        },
                        onFollowUpChange = { minutes ->
                            val updated = settings.followUpMinutes.toMutableMap()
                            if (minutes == 0) updated.remove(prayer) else updated[prayer] = minutes
                            scope.launch { PrayerPreferences.setFollowUps(updated) }
                        },
                    )
                }
            }
        }

        SettingsGroup(title = stringResource(Res.string.prayerCalculationTitle)) {
            item {
                AngleSlider(
                    label = stringResource(Res.string.prayerFajrAngle),
                    value = settings.params.fajrAngle,
                ) { scope.launch { PrayerPreferences.setAngles(it, settings.params.ishaAngle) } }
            }
            item {
                AngleSlider(
                    label = stringResource(Res.string.prayerIshaAngle),
                    value = settings.params.ishaAngle,
                ) { scope.launch { PrayerPreferences.setAngles(settings.params.fajrAngle, it) } }
            }
        }

        NotedGroup(
            title = stringResource(Res.string.prayerOffsetsTitle),
            note = stringResource(Res.string.prayerOffsetsSubtitle),
        ) {
            Prayer.entries.forEach { prayer ->
                item {
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
            }
        }

        NotedGroup(
            title = stringResource(Res.string.lunarCalendarTitle),
            note = stringResource(Res.string.lunarOffsetSubtitle),
        ) {
            item {
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
        onApplyToAll = { sound ->
            scope.launch { PrayerPreferences.setSoundForAll(sound) }
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

/**
 * Başlığı **və** izah sətri olan qrup. [SettingsGroup] yalnız başlıq tanıyır, izah isə kartın
 * içində sətir kimi görünsəydi ayar sanılardı — ona görə hər ikisi kartdan kənarda, üstündədir.
 */
@Composable
private fun NotedGroup(
    title: String,
    note: String,
    content: SettingsGroupScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItemCategoryLabel(title = title)
        Text(
            text = note,
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        )
        SettingsGroup(content = content)
    }
}

/**
 * Bir namaz vaxtının bildiriş sətri: ad + vəziyyət + keçid, alt ayarları isə **yığılıb**.
 *
 * Alt ayarlar (səs, əvvəlcədən xəbərdarlıq) yalnız həmin vaxt xatırladılanda mənalıdır, ona görə
 * sətir yalnız [checked] olduqda açılır. Əvvəl bu iki sətir hər vaxt üçün həmişə çəkilirdi —
 * sönülü vaxtlarda solğun, toxunula bilməyən, amma yer tutan sətirlər qalırdı.
 *
 * Keçid sətrin klikindən ayrıdır: sətrə toxunmaq açır/yığır, keçid isə yalnız öz üstündə işləyir —
 * `SwitchItem`-də olduğu kimi bütün sətir keçidi çevirsəydi, açmaq mümkün olmazdı.
 */
@Composable
private fun PrayerNotifyRow(
    title: String,
    icon: DrawableResource,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    expanded: Boolean,
    soundLabel: String,
    reminderMinutes: Int,
    followUpMinutes: Int,
    onCheckedChange: (Boolean) -> Unit,
    onToggleExpand: () -> Unit,
    onSound: () -> Unit,
    onReminderChange: (Int) -> Unit,
    onFollowUpChange: (Int) -> Unit,
) {
    val canExpand = enabled && checked
    val chevronRotation by animateFloatAsState(if (expanded && canExpand) 180f else 0f)

    Column(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.6f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canExpand, onClick = onToggleExpand)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Vidcetdəki nişanın eynisi — sətri adı oxumadan tanıtmaq üçün.
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (checked) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp).size(20.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = typography.labelLarge,
                    color = colorScheme.onSurface,
                )
                if (checked) {
                    Text(
                        text = summary,
                        // Rəqəm daşıyır («10 dəqiqə əvvəl») — ərəb interfeysində sətir güzgülənməsin.
                        style = typography.labelMedium.ltrDigits(),
                        fontWeight = FontWeight.Normal,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (canExpand) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
            }

            Switch(
                modifier = Modifier.padding(start = 12.dp).height(24.dp),
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }

        AnimatedVisibility(visible = canExpand && expanded) {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                ValueRow(
                    label = stringResource(Res.string.prayerSoundSheetTitle),
                    value = soundLabel,
                    onClick = onSound,
                )

                // İki müstəqil dəyər: biri vaxt girməzdən əvvəl, digəri girdikdən sonra. Heç biri
                // vaxtın öz bildirişini əvəz etmir — hər ikisi ƏLAVƏ bildiriş doğurur, ona görə
                // birlikdə də qurula bilər.
                MinutesRow(
                    label = stringResource(Res.string.prayerReminderTitle),
                    minutes = reminderMinutes,
                    onChange = onReminderChange,
                )
                MinutesRow(
                    label = stringResource(Res.string.prayerFollowUpTitle),
                    minutes = followUpMinutes,
                    onChange = onFollowUpChange,
                )
            }
        }
    }
}

/**
 * «Etiket … − [rəqəm] + dəq» — dəqiqəni həm **klaviatura ilə**, həm bir-bir düymə ilə qurduran sətir.
 *
 * İkisi birlikdədir, çünki ikisi ayrı işə yarayır: 7, 13, 40 kimi ixtiyari dəyər yazılır, yanındakı
 * kiçik düzəliş isə klaviatura açmadan edilir. `0` = xatırlatma yoxdur — «Sönülü» sözü əvəzinə
 * rəqəmin özü yazılır ki, sahə boş qalanda nə yazılacağı aydın olsun.
 *
 * ### Hədddəki düymə görünmür, amma yeri qalır
 * `0`-da azaldan, maksimumda artıran çəkilmir — basılıb heç nə etməyən düymə olmasın. Yuvanın
 * özü ([StepSlotSize]) yerində qalır: düymə tamam yox olsaydı sahə sağa-sola sıçrayar və iki
 * sətrin rəqəmləri bir-birinin altından çıxardı.
 *
 * ### Fokus ikiqat idarə olunur
 * Mətn yerli vəziyyətdədir və yazarkən **kənardan yenilənmir**: hər hərfdən sonra dəyər yadda
 * saxlanılır, geri qayıdan dəyər isə sahəni yenidən yazsaydı, boş sahə dərhal `0`-a çevrilər və
 * növbəti rəqəm `05` kimi düşərdi. Ona görə kənar dəyər yalnız fokus gedəndə mətnə köçürülür,
 * normallaşdırma (boş → `0`, hədd aşımı → maksimum) da orada baş verir. Düymə isə mətni **özü**
 * yazır — fokus sahədə ikən basılsa `LaunchedEffect` onu yeniləməzdi.
 */
@Composable
private fun MinutesRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(minutes.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val range = PrayerSettings.REMINDER_RANGE

    /**
     * Ən son **bizim** yazdığımız dəyər.
     *
     * ⚠️ [minutes] DataStore-dan qayıdır və yazı asinxron olduğu üçün bir neçə kadr **gec** gəlir.
     * Bu echo-nu həqiqi kənar dəyişiklikdən ayırmasaq iki şey pozulur: ekranda hələ köhnə rəqəm
     * durur, sinxronlaşdırma effekti isə fokus gedən kimi təzə yazılanı köhnəsi ilə **geri əzir**.
     */
    var committed by remember { mutableStateOf(minutes) }

    // Ekrandakı dəyər həmişə YERLİ mətndəndir — düymə də, klaviatura da dərhal görünür.
    val shown = text.toIntOrNull()?.coerceIn(range) ?: committed

    // Yalnız həqiqi kənar dəyişiklik (məs. ehtiyat nüsxədən bərpa) mətnə düşür, öz echo-muz yox.
    LaunchedEffect(minutes) {
        if (minutes != committed) {
            committed = minutes
            text = minutes.toString()
        }
    }

    fun commit(value: Int) {
        committed = value
        onChange(value)
    }

    /**
     * Addım biridir: iri sıçrayış üçün onsuz da rəqəm yazılır, düymə isə dəqiq düzəliş üçündür.
     *
     * ⚠️ Sonda **fokus buraxılır**. Fokusda real mətn sahəsi çəkilir, sayğac isə görünmür — yəni
     * istifadəçi bir dəfə rəqəmə toxunub klaviaturanı açandan sonra `+`/`−` fırlanmadan, quru
     * rəqəm kimi dəyişirdi. Dəyər əvvəl yazılır, fokus **sonra** buraxılır: `onFocusChanged`-dəki
     * normallaşdırma cari mətni oxuyur, ona görə bu sıra ilə addım itmir.
     */
    fun step(delta: Int) {
        val next = (shown + delta).coerceIn(range)
        text = next.toString()
        commit(next)
        if (focused) focusManager.clearFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 27.dp, end = 15.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        StepSlot(
            visible = shown > range.first,
            icon = Icons.Rounded.Remove,
            contentDescription = stringResource(Res.string.strLabelDecrease),
        ) { step(-1) }

        val valueStyle = typography.bodyMedium
            .ltrDigits()
            .copy(color = colorScheme.primary, textAlign = TextAlign.Center)

        Box(contentAlignment = Alignment.Center) {
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    // Yalnız rəqəm və ən çoxu üç işarə: `REMINDER_RANGE` onsuz da üç rəqəmlidir və
                    // filtrsiz sahəyə yapışdırılan mətn (məs. «12 dəq») parse-ı sındırardı.
                    val digits = raw.filter { it.isDigit() }.take(3)
                    text = digits
                    digits.toIntOrNull()?.let { commit(it.coerceIn(range)) }
                },
                // Fokusdan kənarda mətn ŞƏFFAFDIR: eyni yerdə fırlanan sayğac çəkilir. Sahənin
                // özü yerində qalır ki, ölçü sabit olsun və toxunuş yenə ona düşsün.
                textStyle = valueStyle.copy(
                    color = if (focused) colorScheme.primary else Color.Transparent,
                ),
                singleLine = true,
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .width(56.dp)
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        // Fokus gedəndə normallaşdır: «05» → «5», boş sahə → son dəyər.
                        // Dəyər CARİ mətndən oxunur — kompozisiyada tutulmuş `shown` bu geri
                        // çağırış işləyəndə artıq köhnəlmiş ola bilər və addımı geri əzərdi.
                        if (!state.isFocused) {
                            text = (text.toIntOrNull()?.coerceIn(range) ?: committed).toString()
                        }
                    }
                    .clip(MaterialTheme.shapes.small)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
            )

            // ⚠️ Üstdə durur, amma toxunuşu UDMUR: `Text`-in pointer-input modifikatoru yoxdur,
            // ona görə toxunuş altdakı sahəyə keçir və klaviatura normal açılır.
            //
            // Fokusda kompozisiyadan **çıxmır**, yalnız şəffaflaşır: çıxsaydı `AnimatedContent`
            // vəziyyətini itirər və klaviatura bağlanandan sonrakı ilk dəyişiklik fırlanmadan,
            // sıçrayışla görünərdi.
            RollingNumber(
                value = shown,
                style = valueStyle,
                modifier = Modifier.alpha(if (focused) 0f else 1f),
            )
        }

        StepSlot(
            visible = shown < range.last,
            icon = Icons.Rounded.Add,
            contentDescription = stringResource(Res.string.strLabelIncrease),
        ) { step(1) }

        Text(
            text = stringResource(Res.string.prayerReminderMinutesUnit),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/**
 * Odometr: rəqəm dəyişəndə köhnəsi sürüşüb çıxır, yenisi eyni istiqamətdə sürüşüb gəlir.
 *
 * İstiqamət **bütün ədədin** müqayisəsindən gəlir, tək rəqəmin yox. Rəqəm-rəqəm baxsaydıq `9 → 10`
 * keçidində təklər `'9' → '0'` olur və simvol müqayisəsi «azalır» deyib o biri tərəfə fırlanardı —
 * sayğacın bütün çarxları eyni tərəfə dönməlidir.
 *
 * Fokusda çağırılmır: istifadəçi öz yazdığını görməlidir, hər hərfdən sonra fırlanan rəqəm yox.
 */
@Composable
private fun RollingNumber(value: Int, style: TextStyle, modifier: Modifier = Modifier) {
    var previous by remember { mutableStateOf(value) }
    val goingUp = value >= previous
    SideEffect { previous = value }

    Row(modifier = modifier) {
        value.toString().forEach { digit ->
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    val enter = slideInVertically(tween(RollDurationMillis)) { height ->
                        if (goingUp) height else -height
                    } + fadeIn(tween(RollDurationMillis))
                    val exit = slideOutVertically(tween(RollDurationMillis)) { height ->
                        if (goingUp) -height else height
                    } + fadeOut(tween(RollDurationMillis))

                    enter togetherWith exit
                },
            ) { shown ->
                Text(text = shown.toString(), style = style)
            }
        }
    }
}

/** Bir çarxın dönmə müddəti — sayğac hissi üçün qısa olmalıdır, yoxsa ləng görünür. */
private const val RollDurationMillis = 180

/** [MinutesRow]-un addım düyməsinin yuvası — düymə çəkilməsə də eni dəyişmir. */
private val StepSlotSize = 36.dp

@Composable
private fun StepSlot(
    visible: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    // Təkrar döngüsü uzun yaşayır; `onClick` isə hər rekompozisiyada yeni lambda olur.
    val step by rememberUpdatedState(onClick)

    /**
     * Basıb saxlayanda sarma [onClick]-i **əvəz etmir**, ona görə buraxılanda adi klik də gəlir —
     * uzun basış bir addım artıq sayardı. Bayraq həmin kliki udur, hər yeni basışda sıfırlanır.
     */
    var wound by remember { mutableStateOf(false) }

    LaunchedEffect(pressed, visible) {
        if (!pressed || !visible) return@LaunchedEffect

        wound = false
        // İlk addım barmağı qaldıranda `onClick`-dən gəlir: qısa toxunuş iki dəfə saymamalıdır.
        delay(RepeatStartDelayMillis)

        var interval = RepeatSlowestMillis
        while (true) {
            wound = true
            step()
            delay(interval)
            // Sürətlənmə: uzun saxlayanda 0→180 barmağı qaldırmadan keçilməlidir.
            interval = (interval - RepeatAccelerationMillis).coerceAtLeast(RepeatFastestMillis)
        }
    }

    Box(
        modifier = Modifier.size(StepSlotSize),
        contentAlignment = Alignment.Center,
    ) {
        if (visible) {
            IconButton(
                onClick = { if (!wound) onClick() },
                modifier = Modifier.size(StepSlotSize),
                interactionSource = interactions,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Sarma bu qədər saxlayandan sonra başlayır — qısa toxunuş təsadüfən sarmamalıdır. */
private const val RepeatStartDelayMillis = 350L

/** Sarmanın ilk (ən yavaş) addım aralığı. */
private const val RepeatSlowestMillis = 140L

/** Hər addımda aralıq bu qədər qısalır. */
private const val RepeatAccelerationMillis = 12L

/** Aralığın alt həddi — bundan sürətli sarma rəqəmi oxunmaz edir. */
private const val RepeatFastestMillis = 20L

/** «Etiket … dəyər ›» — alt ayarın vərəq açan sətri. Girinti üst sətri ilə eyni oxa düşür. */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 27.dp, end = 15.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = typography.bodyMedium,
            color = colorScheme.primary,
        )
        Icon(
            painter = painterResource(Res.drawable.dr_icon_chevron_right),
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp).size(18.dp),
        )
    }
}

@Composable
private fun AngleSlider(label: String, value: Double, onChange: (Double) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 6.dp)) {
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
 * «− dəyər +» sətri. Namaz dəqiqə düzəlişləri, qəməri gün düzəlişi və əvvəlcədən xəbərdarlıq eyni
 * görünüşü paylaşır — chevron/rəng məntiqi üç yerdə təkrarlansaydı biri gec-tez digərindən
 * sürüşərdi.
 *
 * [isDefault] yalnız rəng üçündür: toxunulmamış dəyər sönük, dəyişdirilmiş dəyər vurğulu yazılır.
 */
@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    isDefault: Boolean,
    modifier: Modifier = Modifier,
    onStep: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = typography.bodyMedium, modifier = Modifier.weight(1f))

        IconButton(onClick = { onStep(-1) }, modifier = Modifier.size(STEPPER_BUTTON_SIZE)) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = valueText,
            style = typography.bodyMedium.ltrDigits(),
            color = if (isDefault) colorScheme.onSurfaceVariant else colorScheme.primary,
            // Sarılmır: dəyər iki sətrə düşəndə sətrin hündürlüyü tullanır və düymələr sürüşür.
            // Yer çatmayanda daralan tərəf etiketdir (`weight(1f)`).
            softWrap = false,
        )
        IconButton(onClick = { onStep(1) }, modifier = Modifier.size(STEPPER_BUTTON_SIZE)) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Steppər düyməsinin ölçüsü. Material defoltu 48dp-dir; iki düymə + etiket + dəyər bir sətrə
 * sığmırdı və etiket qırılırdı. 44dp Apple/Material-ın minimal toxunuş hədəfidir — bundan aşağı
 * salma.
 */
private val STEPPER_BUTTON_SIZE = 44.dp

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
