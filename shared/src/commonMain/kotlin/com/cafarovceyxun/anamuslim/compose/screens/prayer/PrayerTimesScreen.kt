package com.cafarovceyxun.anamuslim.compose.screens.prayer

import androidx.compose.foundation.background
import com.cafarovceyxun.anamuslim.viewModels.PrayerLocationViewModel
import com.cafarovceyxun.anamuslim.utils.formatLocalDateLong
import com.cafarovceyxun.anamuslim.resources.prayerUseMyLocation
import com.cafarovceyxun.anamuslim.resources.dr_icon_location
import com.cafarovceyxun.anamuslim.resources.dr_icon_crosshair
import com.cafarovceyxun.anamuslim.compose.components.prayer.CityPickerSheet
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.animateScrollBy
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerTime
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerSettingsSection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerPermissionBanner
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerSettingsSheet
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat.ltrDigits
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.prayerApproximateNote
import com.cafarovceyxun.anamuslim.resources.prayerChooseLocation
import com.cafarovceyxun.anamuslim.resources.prayerNextDay
import com.cafarovceyxun.anamuslim.resources.prayerNextLabel
import com.cafarovceyxun.anamuslim.resources.prayerPreviousDay
import com.cafarovceyxun.anamuslim.resources.prayerRemoteLocationNote
import com.cafarovceyxun.anamuslim.resources.prayerTimesTitle
import com.cafarovceyxun.anamuslim.resources.prayerToday
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.prayer.NextPrayer
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerDay
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerDayTimes
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerSettings
import com.cafarovceyxun.anamuslim.utils.prayer.TimeSource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** Seçilmiş yerin günəş ofseti cihazın qurşağından bu qədər fərqlənəndə xəbərdarlıq göstərilir. */
private const val REMOTE_OFFSET_WARNING_SECONDS = 2 * 3600

@Composable
fun PrayerTimesScreen() {
    val settings = PrayerPreferences.observeSettings()
    var dayOffset by remember { mutableStateOf(0) }

    val now = rememberTickingClock()
    val todayIso = remember(now / 60_000L) { PrayerUiFormat.localDate(now) }
    val shownIso = remember(todayIso, dayOffset) { IsoDate.plusDays(todayIso, dayOffset) ?: todayIso }

    val point = settings.point

    // Yer kartı GPS düyməsi daşıyır, ona görə ViewModel burada qurulur və şəhər vərəqinə ötürülür —
    // iki ayrı instansiya 126 KB-lıq kataloqu iki dəfə parse edərdi.
    val locationVm: PrayerLocationViewModel = viewModel { PrayerLocationViewModel() }
    val locating by locationVm.locating.collectAsState()
    var showCityPicker by remember { mutableStateOf(false) }

    // Astronomiya hər rekompozisiyada deyil, yalnız giriş dəyişəndə hesablanır.
    val shownDay = remember(shownIso, point, settings.params) {
        point?.let { PrayerDay.forLocalDate(shownIso, it, settings.params) }
    }
    val upcomingDays = remember(todayIso, point, settings.params) {
        point?.let { PrayerDay.forLocalDates(todayIso, count = 2, at = it, params = settings.params) }
            .orEmpty()
    }

    val upcoming = remember(now / 30_000L, upcomingDays, settings.notify) {
        NextPrayer.after(now, upcomingDays, settings.notify.ifEmpty { Prayer.entries.toSet() })
    }

    Scaffold(
        topBar = {
            // Dişli yoxdur: ayarlar bu ekranın öz axınının altındadır, ayrıca vərəq deyil.
            AppBar(title = stringResource(Res.string.prayerTimesTitle))
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            ReadableWidthColumn {
                // ⚠️ Adına baxmayaraq `ReadableWidthColumn` **Box**-dur (bax onun tərifi), ona görə
                // birbaşa verilən uşaqlar bir-birinin ÜSTÜNƏ düşür. Daxili `Column` məcburidir —
                // `HomeScreen` də eyni səbəbdən belə edir. Kompilyator susur; səhv yalnız ekranda
                // görünür (banner, tarix zolağı və cədvəl üst-üstə düşmüşdü).
                Column {
                    // İcazə çatışmazlığı bu funksiyada SƏSSİZDİR (bax bannerin KDoc-u), ona görə
                    // xəbərdarlıq yerin təyin olunub-olunmamasından asılı olmayaraq ən üstdədir.
                    PrayerPermissionBanner()

                    if (upcoming != null && dayOffset == 0) {
                        NextPrayerBanner(
                            prayerName = PrayerUiFormat.label(upcoming.prayer),
                            atMillis = upcoming.atMillis,
                            remainingMillis = upcoming.atMillis - now,
                        )
                    }

                    LocationCard(
                        label = settings.placeName.ifBlank {
                            stringResource(Res.string.prayerChooseLocation)
                        },
                        locating = locating,
                        onPick = { showCityPicker = true },
                        onLocate = { locationVm.useDeviceLocation() },
                    )

                    DayCard(
                        shownIso = shownIso,
                        onPrevious = { dayOffset-- },
                        onNext = { dayOffset++ },
                    )

                    shownDay?.let { day ->
                        PrayerCarousel(
                            day = day,
                            highlighted = if (dayOffset == 0) upcoming?.prayer else null,
                        )
                        Notes(day = day, settings = settings, nowMillis = now)
                    }

                    // Ayarlar ekranın öz axınındadır — istifadəçi vaxtları görüb dərhal altında
                    // bucağı və ya bildirişi dəyişə bilir, vərəq açmadan.
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 16.dp),
                        color = colorScheme.outlineVariant.alpha(0.4f),
                    )
                    PrayerSettingsSection()
                }
            }
        }
    }

    CityPickerSheet(
        isOpen = showCityPicker,
        onClose = { showCityPicker = false },
        vm = locationVm,
    )
}

/**
 * Saniyəlik saat — geri sayım üçün.
 *
 * Növbəti vaxta bir saatdan çox qalıbsa dəqiqədə bir dəfə oyanır: ekran açıq qalanda saniyəlik
 * rekompozisiya batareyanı yeyir, geri sayım isə onsuz da «2 saat 15 dəqiqə» kimi göstərilir.
 */
@Composable
private fun rememberTickingClock(): Long = produceState(initialValue = currentEpochMillis()) {
    while (true) {
        value = currentEpochMillis()
        delay(1_000L)
    }
}.value

@Composable
private fun NextPrayerBanner(prayerName: String, atMillis: Long, remainingMillis: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(shapes.large)
            .background(colorScheme.primaryContainer.alpha(0.55f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.prayerNextLabel, prayerName),
            style = typography.titleMedium,
            color = colorScheme.onPrimaryContainer,
        )
        Text(
            text = PrayerUiFormat.clock(atMillis),
            style = typography.displaySmall.ltrDigits(),
            color = colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = PrayerUiFormat.remaining(remainingMillis.coerceAtLeast(0L)),
            style = typography.bodyMedium,
            color = colorScheme.onPrimaryContainer.alpha(0.85f),
        )
    }
}

/**
 * Yer kartı — ekranın ən üstündə, şəkildəki kimi: sol tərəfdə nişan, ortada ad, sağda GPS düyməsi.
 *
 * Yer artıq ayarların içində gizlənmir: istifadəçinin ən çox dəyişdirdiyi şey budur (səyahət,
 * yeni şəhər), ona görə bir toxunuş məsafəsindədir. Nişana və ada toxunmaq siyahını açır, sağdakı
 * düymə isə birbaşa cihazın mövqeyini götürür.
 */
@Composable
private fun LocationCard(
    label: String,
    locating: Boolean,
    onPick: () -> Unit,
    onLocate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(shapes.large)
            .background(colorScheme.surfaceVariant.alpha(0.45f))
            .clickable(onClick = onPick)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.dr_icon_location),
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )

        Text(
            text = label,
            style = typography.titleMedium,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        IconButton(onClick = onLocate, enabled = !locating) {
            if (locating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_crosshair),
                    contentDescription = stringResource(Res.string.prayerUseMyLocation),
                    tint = colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Tarix kartı — oxlarla gün dəyişir, mərkəzdə miladi, altında hicri tarix.
 *
 * Hər iki tarix **platformanın öz formatlayıcısından** gəlir, ona görə həftə günü və ay adları
 * beş dilə ayrıca yazılmır. Hicri sətir Android API 26-dan aşağıda `null` olur və sadəcə çəkilmir.
 */
@Composable
private fun DayCard(shownIso: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    val atMillis = remember(shownIso) {
        IsoDate.toEpochDay(shownIso)?.let { it * 86_400_000L + 12 * 3_600_000L }
    } ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(shapes.large)
            .background(colorScheme.surfaceVariant.alpha(0.45f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                contentDescription = stringResource(Res.string.prayerPreviousDay),
                tint = colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatLocalDateLong(atMillis),
                style = typography.titleSmall,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            PrayerUiFormat.hijri(atMillis)?.let { hijri ->
                Text(
                    text = hijri,
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = stringResource(Res.string.prayerNextDay),
                tint = colorScheme.primary,
            )
        }
    }
}

private val CARD_SPACING = 8.dp

/**
 * Günün vaxtları — üfüqi sürüşən kartlar.
 *
 * Cari (növbəti) vaxt **avtomatik mərkəzə gətirilir**: Zöhrə yaxın Zöhr ortada olur.
 *
 * ⚠️ Kartların eni məzmuna görədir («Sabah namazı» geniş, «Zöhr» dar), ona görə sabit ofsetlə
 * mərkəzləmək mümkün deyil. Əvvəlcə element görünür edilir (`scrollToItem`), sonra `layoutInfo`-dan
 * onun **real** ölçüsü oxunub dəqiq fərq qədər sürüşdürülür. Yalnız `animateScrollToItem` işlətmək
 * elementi sola yapışdırardı, mərkəzə yox.
 */
@Composable
private fun PrayerCarousel(day: PrayerDayTimes, highlighted: Prayer?) {
    val times = remember(day) { day.times.sortedBy { it.prayer.ordinal } }
    if (times.isEmpty()) return

    val listState = rememberLazyListState()
    val targetIndex = remember(times, highlighted) { times.indexOfFirst { it.prayer == highlighted } }

    LaunchedEffect(targetIndex, times) {
        if (targetIndex < 0) return@LaunchedEffect

        listState.scrollToItem(targetIndex)

        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            ?: return@LaunchedEffect
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f

        listState.animateScrollBy(item.offset + item.size / 2f - viewportCenter)
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
    ) {
        items(times, key = { it.prayer }) { time ->
            PrayerCard(time = time, isHighlighted = time.prayer == highlighted)
        }
    }
}

@Composable
private fun PrayerCard(time: PrayerTime, isHighlighted: Boolean) {
    val background = if (isHighlighted) colorScheme.primary else colorScheme.surfaceVariant.alpha(0.45f)
    val content = if (isHighlighted) colorScheme.onPrimary else colorScheme.onSurface

    Column(
        modifier = Modifier
            .clip(shapes.large)
            .background(background)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = PrayerUiFormat.label(time.prayer),
            style = typography.titleMedium,
            color = content.alpha(if (isHighlighted) 1f else 0.85f),
            maxLines = 1,
        )
        Text(
            // `≈` = vaxt təxminidir (yuxarı enlikdə bucaq həll olunmur).
            text = if (time.source == TimeSource.ASTRONOMICAL) {
                PrayerUiFormat.clock(time.atMillis)
            } else {
                "≈ " + PrayerUiFormat.clock(time.atMillis)
            },
            style = typography.titleMedium.ltrDigits(),
            color = content.alpha(if (isHighlighted) 0.95f else 0.7f),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Notes(day: PrayerDayTimes, settings: PrayerSettings, nowMillis: Long) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (day.hasFallback) {
            Text(
                text = stringResource(Res.string.prayerApproximateNote),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }

        // Cihazın qurşağı seçilmiş yerin günəş vaxtından çox uzaqdırsa vaxtlar «yad» görünür —
        // səbəbini demək «tətbiq səhvdir» şikayətinin qarşısını alır.
        val point = settings.point

    // Yer kartı GPS düyməsi daşıyır, ona görə ViewModel burada qurulur və şəhər vərəqinə ötürülür —
    // iki ayrı instansiya 126 KB-lıq kataloqu iki dəfə parse edərdi.
    val locationVm: PrayerLocationViewModel = viewModel { PrayerLocationViewModel() }
    val locating by locationVm.locating.collectAsState()
    var showCityPicker by remember { mutableStateOf(false) }
        if (point != null && settings.placeName.isNotBlank()) {
            val deviceOffset = PrayerDay.deviceUtcOffsetSeconds(nowMillis)
            val solarOffset = (point.longitude / 15.0 * 3600.0).toInt()

            if (abs(deviceOffset - solarOffset) > REMOTE_OFFSET_WARNING_SECONDS) {
                Text(
                    text = stringResource(Res.string.prayerRemoteLocationNote, settings.placeName),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyLocationPrompt() {
    Text(
        text = stringResource(Res.string.prayerChooseLocation),
        style = typography.bodyLarge,
        color = colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
}
