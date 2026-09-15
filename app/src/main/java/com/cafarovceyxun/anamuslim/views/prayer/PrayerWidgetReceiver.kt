package com.cafarovceyxun.anamuslim.views.prayer

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivityPrayerTimes
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.localizedAppContext
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.hijriDate
import com.cafarovceyxun.anamuslim.utils.prayer.NextPrayer
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerDay
import com.cafarovceyxun.anamuslim.utils.prayer.TimeSource
import com.cafarovceyxun.anamuslim.views.widget.refreshAllInstances
import com.cafarovceyxun.anamuslim.views.widget.updateInstanceOnResize
import java.text.SimpleDateFormat
import java.util.Date

private const val CORNER_RADIUS_DP = 16f
private const val CARD_PADDING_DP = 8f

/** Namaz ikonu — sətirdə ad və saatla birlikdə bir sütuna sığmalıdır. */
private const val ICON_SIZE_DP = 12f

/** Kompakt variantda çatan namazın ikonu — saatın yanında dayandığı üçün daha iridir. */
private const val COMPACT_ICON_SIZE_DP = 30f

/** Bundan enli xanada geri sayım və yer adı sağ sütuna keçir; dardan saatın altında qalır. */
private const val COMPACT_WIDE_MIN_WIDTH_DP = 220f

/** Dar xanada yer adı yalnız hündürlük çatanda göstərilir — saat və geri sayım ondan vacibdir. */
private const val COMPACT_PLACE_MIN_HEIGHT_DP = 130f

/** Bundan enli xanada tarix başlığın sağında dayanır; dardan altına düşüb ortalanır. */
private const val DATE_BESIDE_MIN_WIDTH_DP = 320f

/**
 * Hündürlük hədləri (dp, yerləşdirilmiş **həqiqi** ölçü — `SizeMode.Exact`).
 *
 * Vidcet bir xanaya qədər kiçildilə bilir; hər şeyi sığdırmağa çalışmaq əvəzinə əvvəlcə tarix sətri,
 * sonra ikonlar düşür — qalan hissə isə həmişə oxunaqlı qalır.
 */
private const val DATE_LINE_MIN_HEIGHT_DP = 92f
private const val ICON_MIN_HEIGHT_DP = 76f

private const val MILLIS_PER_DAY = 86_400_000L

/** Logo başlıq sətrini hündürləndirməsin — mətn ölçüsü ilə eyni sırada qalır. */
private const val LOGO_SIZE_DP = 18f

/**
 * Qara kartda oxunan brend yaşılı.
 *
 * `colorPrimary` (#008B5B) divar kağızının üstündəki tünd kartda sönük çıxır — vidcet üçün eyni
 * çalarlın açığı götürülüb.
 */
private val ACCENT_GREEN = Color(0xFF19B37E)

private data class PrayerWidgetRow(
    val label: String,
    val time: String,
    val iconRes: Int,
    val isNext: Boolean,
)

private data class PrayerWidgetUiState(
    val nextLabel: String?,
    val nextTime: String?,
    /** Geri sayımın hədəfi. `Chronometer` özü işlədiyi üçün yenilənmə arasında köhnəlmir. */
    val nextAtMillis: Long?,
    /** Çatan namazın ikonu — kompakt variantda solda dayanır. */
    val nextIconRes: Int?,
    val placeName: String,
    /** «Bazar ertəsi, 25 Rəbiül-əvvəl 1448» — həftənin günü və qəməri tarix bir sətirdə. */
    val dateLine: String?,
    val rows: List<PrayerWidgetRow>,
)

/** Kompakt variant: yalnız növbəti namaz — istifadəçinin göstərdiyi 2x1 kart kimi. */
class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerNextGlanceWidget()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        glanceAppWidget.updateInstanceOnResize(context, appWidgetId)
    }
}

/** Geniş variant: bütün vaxtlar + başlıqda tətbiq logosu. */
class PrayerLogoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerLogoGlanceWidget()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        glanceAppWidget.updateInstanceOnResize(context, appWidgetId)
    }
}

/**
 * ⚠️ İki variant **ayrı-ayrı alt siniflərdir**, bayraq verilmiş tək sinfin iki nüsxəsi yox:
 * `GlanceAppWidgetManager` yerləşdirilmiş instansiyaları vidcet **sinfinə** görə tapır və hər sinif
 * üçün bir receiver saxlayır. Eyni sinfi paylaşan iki receiver-dən biri xəritədə digərini əzir və
 * heç vaxt yenilənməzdi — nə kompilyator, nə də test bunu tutur.
 */
private class PrayerNextGlanceWidget : PrayerGlanceWidget(showAllTimes = false)

private class PrayerLogoGlanceWidget : PrayerGlanceWidget(showAllTimes = true)

private open class PrayerGlanceWidget(private val showAllTimes: Boolean) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // ⚠️ İLK sətir. `wrapContextWithAppLocale` API 33+-da bilərəkdən no-op-dur (platforma dili
        // Activity-lərə özü tətbiq edir), Glance isə kompozisiyanı fon worker-ində qurur — adi
        // context ilə vidcet **sistem dilində** çıxardı.
        val localizedContext = localizedAppContext(context)

        // Fon qatılığı burada oxunur, kompozisiyada yox: `provideGlance` fon işçisindədir, ayar isə
        // hər yenilənmədə təzədən oxunur (sürüşdürücü `refreshPlacedWidgets` çağırır).
        val backgroundAlpha = PrayerPreferences.getWidgetOpacityPercent() / 100f

        provideContent {
            val glanceState = currentState<Preferences>()

            val state by produceState<PrayerWidgetUiState?>(null, glanceState) {
                value = buildState(localizedContext)
            }

            PrayerWidgetCard(localizedContext, backgroundAlpha) {
                if (showAllTimes) {
                    AllTimesFace(localizedContext, state)
                } else {
                    NextPrayerFace(localizedContext, state)
                }
            }
        }
    }
}

@Composable
private fun PrayerWidgetCard(
    context: Context,
    backgroundAlpha: Float,
    content: @Composable () -> Unit,
) {
    val openIntent = Intent(context, ActivityPrayerTimes::class.java)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.Black.alpha(backgroundAlpha)))
            .cornerRadius(CORNER_RADIUS_DP.dp)
            // ⚠️ Lazy konteyner YOXDUR: `LazyColumn` sətirlərində `clickable` Android-də yalnız
            // Activity PendingIntent ola bilər və Glance onu görünməz tramplin Activity-dən
            // keçirir; proses soyuq olanda One UI onu kəsir və toxunuş SƏSSİZCƏ düşür (CLAUDE.md).
            // Adi `Column`/`Row` ilə eyni klik adi broadcast-a çevrilir.
            .clickable(actionStartActivity(openIntent))
            .padding(CARD_PADDING_DP.dp),
    ) {
        content()
    }
}

/**
 * Növbəti namaz: solda ikon, yanında ad və saat, sonra geri sayım və yer adı.
 *
 * ⚠️ Enə görə iki düzülüş var. 2x1 xana bu telefonda **145x106dp**-dir: ikon (26dp) və «19:20»
 * (28sp) onsuz da 112dp tutur, ona görə geri sayımla yer adı yanda **kəsilirdi**. Dar halda onlar
 * saatın altına düşür — hündürlük onsuz da artıqdır. Enli halda
 * ([COMPACT_WIDE_MIN_WIDTH_DP]) sağ sütun qayıdır, yoxsa geniş vidcetin sağ yarısı boş qalar.
 *
 * Geri sayım `Chronometer`-dir (`AndroidRemoteViews` ilə), adi mətn deyil: vidcet yarım saatda bir
 * yenilənir, ona görə hesablanıb yazılmış «29 dəq qaldı» dərhal yalana çevrilərdi. `Chronometer`
 * launcher prosesində özü işləyir və yenilənməyə ehtiyac duymur.
 */
@Composable
private fun NextPrayerFace(context: Context, state: PrayerWidgetUiState?) {
    if (state?.nextLabel == null || state.nextTime == null) {
        PlaceholderFace(context, state)
        return
    }

    val size = LocalSize.current
    val wide = size.width >= COMPACT_WIDE_MIN_WIDTH_DP.dp
    val showPlace = state.placeName.isNotBlank() &&
        (wide || size.height >= COMPACT_PLACE_MIN_HEIGHT_DP.dp)

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        if (state.nextIconRes != null) {
            Image(
                provider = ImageProvider(state.nextIconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(COMPACT_ICON_SIZE_DP.dp),
                colorFilter = ColorFilter.tint(ColorProvider(ACCENT_GREEN)),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
        }

        Column {
            Text(
                text = state.nextLabel,
                style = TextStyle(
                    color = ColorProvider(Color.White.alpha(0.7f)),
                    fontSize = 19.sp,
                ),
            )
            Text(
                text = state.nextTime,
                style = TextStyle(
                    color = ColorProvider(ACCENT_GREEN),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            if (!wide) {
                if (state.nextAtMillis != null) Countdown(context, state.nextAtMillis)
                if (showPlace) PlaceName(state.placeName)
            }
        }

        if (wide) {
            Spacer(modifier = GlanceModifier.defaultWeight())

            Column(horizontalAlignment = Alignment.Horizontal.End) {
                if (state.nextAtMillis != null) Countdown(context, state.nextAtMillis)
                if (showPlace) PlaceName(state.placeName)
            }
        }
    }
}

@Composable
private fun PlaceName(name: String) {
    Text(
        text = name,
        style = TextStyle(
            color = ColorProvider(Color.White.alpha(0.45f)),
            fontSize = 18.sp,
        ),
    )
}

/**
 * Bütün vaxtlar: solda çatan namaz, sağda həftənin günü + qəməri tarix, altında hər vaxt üçün
 * ikonlu sütun.
 *
 * Xana daraldıqca sıra ilə: tarix başlığın altına düşüb ortalanır, sonra ümumiyyətlə gizlənir, ən
 * sonda ikonlar gedir — ad və saat həmişə qalır.
 */
@Composable
private fun AllTimesFace(context: Context, state: PrayerWidgetUiState?) {
    val size = LocalSize.current
    val scale = timesScaleFor(size.height)
    val showIcons = size.height >= ICON_MIN_HEIGHT_DP.dp

    val dateLine = state?.dateLine?.takeIf { size.height >= DATE_LINE_MIN_HEIGHT_DP.dp }
    val dateBeside = dateLine != null && size.width >= DATE_BESIDE_MIN_WIDTH_DP.dp

    val headline = if (state?.nextLabel != null && state.nextTime != null) {
        "${state.nextLabel} · ${state.nextTime}"
    } else {
        context.getString(R.string.prayer_widget_title)
    }

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = if (dateBeside) {
                Alignment.Horizontal.Start
            } else {
                Alignment.Horizontal.CenterHorizontally
            },
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = GlanceModifier.size(scale.logoDp.dp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))

            Text(
                text = headline,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = scale.headerSp.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            if (dateBeside && dateLine != null) {
                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = dateLine,
                    style = TextStyle(
                        color = ColorProvider(Color.White.alpha(0.55f)),
                        fontSize = scale.dateSp.sp,
                    ),
                )
            }
        }

        if (!dateBeside && dateLine != null) {
            Text(
                text = dateLine,
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    color = ColorProvider(Color.White.alpha(0.55f)),
                    fontSize = scale.dateSp.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        Spacer(modifier = GlanceModifier.height(scale.gapDp.dp))

        if (state == null || state.rows.isEmpty()) {
            PlaceholderFace(context, state)
            return@Column
        }

        // ⚠️ Sütun eni ölçülüb verilir, `defaultWeight()` ilə yox: çəki `layout_weight`-ə çevrilir
        // və eni 0dp qoyur — konteynerin eni host tərəfindən dəqiq verilmədikdə altı sütunun hamısı
        // sıfır enlə qalır, yəni sətir GÖRÜNMÜR (kompilyator da, log da susur). `SizeMode.Exact`
        // altında `LocalSize` vidcetin həqiqi ölçüsüdür, ona görə bölmək etibarlıdır.
        val columnWidth = (size.width - (CARD_PADDING_DP * 2).dp) / state.rows.size

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            state.rows.forEach { row ->
                Column(
                    modifier = GlanceModifier.width(columnWidth),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    if (showIcons) {
                        Image(
                            provider = ImageProvider(row.iconRes),
                            contentDescription = null,
                            modifier = GlanceModifier.size(scale.iconDp.dp),
                            colorFilter = ColorFilter.tint(
                                ColorProvider(
                                    if (row.isNext) ACCENT_GREEN else Color.White.alpha(0.55f)
                                )
                            ),
                        )
                    }

                    Text(
                        text = row.label,
                        style = TextStyle(
                            color = ColorProvider(Color.White.alpha(0.6f)),
                            fontSize = scale.labelSp.sp,
                        ),
                    )
                    Text(
                        text = row.time,
                        style = TextStyle(
                            color = ColorProvider(
                                if (row.isNext) ACCENT_GREEN else Color.White.alpha(0.9f)
                            ),
                            fontSize = scale.timeSp.sp,
                            fontWeight = if (row.isNext) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Ölçüyə görə tipoqrafiya.
 *
 * 5x1 xana bu telefonda **475x106dp**, 5x2 isə **475x232dp**-dir — eyni ölçülərlə ikincisi yarıya
 * qədər boş qalırdı. Pillələr `LocalSize`-ın **həqiqi** hündürlüyünə baxır (`SizeMode.Exact`), ona
 * görə istifadəçi vidceti dartan kimi yazı da böyüyür.
 */
private data class TimesScale(
    val headerSp: Int,
    val dateSp: Int,
    val labelSp: Int,
    val timeSp: Int,
    val iconDp: Float,
    val logoDp: Float,
    val gapDp: Float,
)

private fun timesScaleFor(height: Dp): TimesScale = when {
    // 5x2 (≈232dp). Tarix qəsdən başlıqdan az böyüyür: «Bazar ertəsi, 24 Rəbiül-əvvəl 1448» uzun
    // sətirdir və 5 xananın eni (≈475dp) ilə başlıqla yanaşı ancaq bu ölçüdə yerləşir.
    height >= 190.dp -> TimesScale(29, 19, 20, 26, 32f, 31f, 12f)
    height >= 140.dp -> TimesScale(24, 18, 17, 21, 22f, 25f, 6f)
    else -> TimesScale(21, 17, 16, 19, 14f, 22f, 2f)
}

/** Yer seçilməyib (və ya hələ yüklənir) — hər iki variantda eyni mətn. */
@Composable
private fun PlaceholderFace(context: Context, state: PrayerWidgetUiState?) {
    Text(
        text = context.getString(
            if (state == null) R.string.prayer_widget_title else R.string.prayer_widget_no_location
        ),
        style = TextStyle(color = ColorProvider(Color.White.alpha(0.8f)), fontSize = 19.sp),
    )
}

/**
 * Canlı geri sayım.
 *
 * `Chronometer` RemoteViews-in icazə verdiyi görünüşdür və `setChronometerCountDown` (API 24) ilə
 * geriyə sayır; Glance-in öz `Text`-i ilə bunu etmək mümkün deyil — vidcet kompozisiyası yalnız
 * yenilənəndə işləyir.
 *
 * ⚠️ «qaldı» sözü `Chronometer`-in öz `setFormat`-ı ilə verilmir, ayrıca `Text`-dir: `AndroidRemoteViews`
 * içindəki uşaq RemoteViews-da format tətbiq olunmur — saat işləyir, format isə səssizcə düşür
 * (cihazda ölçüldü, logda bir dənə də xəbərdarlıq yoxdur). Sözün yeri sətrin özündən yox, dilin
 * istiqamətindən gəlir: ərəbcədə sıra düzülüşlə birlikdə güzgülənir.
 */
@Composable
private fun Countdown(context: Context, atMillis: Long) {
    val remaining = (atMillis - currentEpochMillis()).coerceAtLeast(0L)

    val views = RemoteViews(context.packageName, R.layout.prayer_widget_countdown).apply {
        setChronometerCountDown(R.id.prayer_widget_countdown, true)
        setChronometer(
            R.id.prayer_widget_countdown,
            SystemClock.elapsedRealtime() + remaining,
            null,
            true,
        )
    }

    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        AndroidRemoteViews(remoteViews = views)
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = context.getString(R.string.prayer_widget_remaining),
            style = TextStyle(
                color = ColorProvider(Color.White.alpha(0.6f)),
                fontSize = 18.sp,
            ),
        )
    }
}

private fun buildState(context: Context): PrayerWidgetUiState? {
    val settings = PrayerPreferences.getSettings()
    val point = settings.point ?: return null

    val now = currentEpochMillis()
    val todayIso = PrayerUiFormat.localDate(now)
    val days = PrayerDay.forLocalDates(todayIso, count = 2, at = point, params = settings.params)
    val today = days.firstOrNull() ?: return null

    val include = settings.notify.ifEmpty { Prayer.entries.filter { it.isPrayer }.toSet() }
    val upcoming = NextPrayer.after(now, days, include)

    val rows = Prayer.entries.mapNotNull { prayer ->
        val time = today[prayer] ?: return@mapNotNull null

        PrayerWidgetRow(
            label = context.getString(labelResOf(prayer, today.dateIso)),
            iconRes = iconResOf(prayer),
            time = if (time.source == TimeSource.ASTRONOMICAL) {
                PrayerUiFormat.clock(time.atMillis)
            } else {
                "≈" + PrayerUiFormat.clock(time.atMillis)
            },
            isNext = prayer == upcoming?.prayer,
        )
    }

    return PrayerWidgetUiState(
        nextLabel = upcoming?.let {
            context.getString(labelResOf(it.prayer, PrayerUiFormat.localDate(it.atMillis)))
        },
        nextTime = upcoming?.let { PrayerUiFormat.clock(it.atMillis) },
        nextAtMillis = upcoming?.atMillis,
        nextIconRes = upcoming?.let { iconResOf(it.prayer) },
        placeName = settings.placeName,
        dateLine = dateLine(context, now, settings.effectiveLunarOffsetDays),
        rows = rows,
    )
}

/**
 * «Bazar ertəsi, 25 Rəbiül-əvvəl 1448».
 *
 * Həftənin günü platformadan gəlir (ayrıca tərcümə saxlamağa dəyməz), qəməri tarix isə tətbiqin öz
 * çevirməsindən — `android.icu` təqvimi istifadəçinin **qəməri gün düzəlişini** bilmir və vidcet
 * ekranlardan bir gün fərqli göstərərdi.
 */
private fun dateLine(context: Context, atMillis: Long, lunarOffsetDays: Int): String {
    val locale = context.resources.configuration.locales[0]
    val weekday = SimpleDateFormat("EEEE", locale).format(Date(atMillis))
    val hijri = hijriDate(atMillis + lunarOffsetDays * MILLIS_PER_DAY) ?: return weekday
    val (day, month, year) = hijri

    val hijriText = context.getString(
        R.string.prayer_widget_hijri_date,
        day,
        context.getString(hijriMonthResOf(month)),
        year,
    )

    return context.getString(R.string.prayer_widget_date_line, weekday, hijriText)
}

private fun hijriMonthResOf(month: Int): Int = when (month) {
    2 -> R.string.prayer_widget_hijri_month_2
    3 -> R.string.prayer_widget_hijri_month_3
    4 -> R.string.prayer_widget_hijri_month_4
    5 -> R.string.prayer_widget_hijri_month_5
    6 -> R.string.prayer_widget_hijri_month_6
    7 -> R.string.prayer_widget_hijri_month_7
    8 -> R.string.prayer_widget_hijri_month_8
    9 -> R.string.prayer_widget_hijri_month_9
    10 -> R.string.prayer_widget_hijri_month_10
    11 -> R.string.prayer_widget_hijri_month_11
    12 -> R.string.prayer_widget_hijri_month_12
    else -> R.string.prayer_widget_hijri_month_1
}

private fun iconResOf(prayer: Prayer): Int = when (prayer) {
    Prayer.FAJR -> R.drawable.ic_prayer_fajr
    Prayer.SUNRISE -> R.drawable.ic_prayer_sunrise
    Prayer.DHUHR -> R.drawable.ic_prayer_dhuhr
    Prayer.ASR -> R.drawable.ic_prayer_asr
    Prayer.MAGHRIB -> R.drawable.ic_prayer_maghrib
    Prayer.ISHA -> R.drawable.ic_prayer_isha
}

/**
 * ⚠️ Vidcet `R.string`-dən oxuyur, ekranlar isə Compose Resources-dan — ona görə namaz adları
 * **iki dəfə** tərcümə olunur. Bu, layihədəki mövcud bölünmənin qaçılmaz nəticəsidir
 * (`app/src/main/res` vidcetlərindir, `composeResources` isə paylaşılan UI-nindir).
 */
private fun labelResOf(prayer: Prayer, dateIso: String): Int = when {
    // Cümə günü zöhr «Cümə» olur — ekranlardakı ilə eyni qayda, sadəcə `R.string` tərəfində.
    prayer == Prayer.DHUHR && IsoDate.dayOfWeek(dateIso) == IsoDate.FRIDAY ->
        R.string.prayer_widget_jumuah

    else -> when (prayer) {
        Prayer.FAJR -> R.string.prayer_widget_fajr
        Prayer.SUNRISE -> R.string.prayer_widget_sunrise
        Prayer.DHUHR -> R.string.prayer_widget_dhuhr
        Prayer.ASR -> R.string.prayer_widget_asr
        Prayer.MAGHRIB -> R.string.prayer_widget_maghrib
        Prayer.ISHA -> R.string.prayer_widget_isha
    }
}

private val KEY_LAST_UPDATE = longPreferencesKey("prayer_widget_last_update")

/** Alarm çalandan sonra və ayar dəyişikliyində çağırılır — hər iki variant üçün. */
fun updateAllPrayerWidgets(context: Context) {
    PrayerNextGlanceWidget().refreshAllInstances(
        context,
        PrayerWidgetReceiver::class.java,
        KEY_LAST_UPDATE,
    )
    PrayerLogoGlanceWidget().refreshAllInstances(
        context,
        PrayerLogoWidgetReceiver::class.java,
        KEY_LAST_UPDATE,
    )
}
