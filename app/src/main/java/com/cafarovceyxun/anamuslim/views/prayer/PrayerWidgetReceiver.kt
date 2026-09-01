package com.cafarovceyxun.anamuslim.views.prayer

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
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
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivityPrayerTimes
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.localizedAppContext
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.prayer.NextPrayer
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerDay
import com.cafarovceyxun.anamuslim.utils.prayer.TimeSource
import com.cafarovceyxun.anamuslim.views.widget.refreshAllInstances

private const val CORNER_RADIUS_DP = 16f

private data class PrayerWidgetRow(
    val label: String,
    val time: String,
    val isNext: Boolean,
)

private data class PrayerWidgetUiState(
    val nextLabel: String?,
    val nextTime: String?,
    val rows: List<PrayerWidgetRow>,
)

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerGlanceWidget()
}

private class PrayerGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // ⚠️ İLK sətir. `wrapContextWithAppLocale` API 33+-da bilərəkdən no-op-dur (platforma dili
        // Activity-lərə özü tətbiq edir), Glance isə kompozisiyanı fon worker-ində qurur — adi
        // context ilə vidcet **sistem dilində** çıxardı.
        val localizedContext = localizedAppContext(context)

        provideContent {
            val glanceState = currentState<Preferences>()

            val state by produceState<PrayerWidgetUiState?>(null, glanceState) {
                value = buildState(localizedContext)
            }

            PrayerWidgetContent(localizedContext, state)
        }
    }
}

@Composable
private fun PrayerWidgetContent(context: Context, state: PrayerWidgetUiState?) {
    val openIntent = Intent(context, ActivityPrayerTimes::class.java)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.Black.alpha(0.85f)))
            .cornerRadius(CORNER_RADIUS_DP.dp)
            // ⚠️ Lazy konteyner YOXDUR: `LazyColumn` sətirlərində `clickable` Android-də yalnız
            // Activity PendingIntent ola bilər və Glance onu görünməz tramplin Activity-dən
            // keçirir; proses soyuq olanda One UI onu kəsir və toxunuş SƏSSİZCƏ düşür (CLAUDE.md).
            // Adi `Column`/`Row` ilə eyni klik adi broadcast-a çevrilir.
            .clickable(actionStartActivity(openIntent))
            .padding(12.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.prayer_widget_title),
                    style = TextStyle(
                        color = ColorProvider(Color.White.alpha(0.7f)),
                        fontSize = 12.sp,
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())

                if (state?.nextLabel != null && state.nextTime != null) {
                    Text(
                        text = "${state.nextLabel} · ${state.nextTime}",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.padding(4.dp))

            if (state == null || state.rows.isEmpty()) {
                Text(
                    text = context.getString(R.string.prayer_widget_no_location),
                    style = TextStyle(color = ColorProvider(Color.White.alpha(0.8f)), fontSize = 13.sp),
                )
                return@Column
            }

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                state.rows.forEach { row ->
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    ) {
                        Text(
                            text = row.label,
                            style = TextStyle(
                                color = ColorProvider(Color.White.alpha(0.6f)),
                                fontSize = 11.sp,
                            ),
                        )
                        Text(
                            text = row.time,
                            style = TextStyle(
                                color = ColorProvider(if (row.isNext) Color.White else Color.White.alpha(0.9f)),
                                fontSize = 14.sp,
                                fontWeight = if (row.isNext) FontWeight.Bold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
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
            label = context.getString(labelResOf(prayer)),
            time = if (time.source == TimeSource.ASTRONOMICAL) {
                PrayerUiFormat.clock(time.atMillis)
            } else {
                "≈" + PrayerUiFormat.clock(time.atMillis)
            },
            isNext = prayer == upcoming?.prayer,
        )
    }

    return PrayerWidgetUiState(
        nextLabel = upcoming?.let { context.getString(labelResOf(it.prayer)) },
        nextTime = upcoming?.let { PrayerUiFormat.clock(it.atMillis) },
        rows = rows,
    )
}

/**
 * ⚠️ Vidcet `R.string`-dən oxuyur, ekranlar isə Compose Resources-dan — ona görə namaz adları
 * **iki dəfə** tərcümə olunur. Bu, layihədəki mövcud bölünmənin qaçılmaz nəticəsidir
 * (`app/src/main/res` vidcetlərindir, `composeResources` isə paylaşılan UI-nindir).
 */
private fun labelResOf(prayer: Prayer): Int = when (prayer) {
    Prayer.FAJR -> R.string.prayer_widget_fajr
    Prayer.SUNRISE -> R.string.prayer_widget_sunrise
    Prayer.DHUHR -> R.string.prayer_widget_dhuhr
    Prayer.ASR -> R.string.prayer_widget_asr
    Prayer.MAGHRIB -> R.string.prayer_widget_maghrib
    Prayer.ISHA -> R.string.prayer_widget_isha
}

private val KEY_LAST_UPDATE = longPreferencesKey("prayer_widget_last_update")

/** Alarm çalandan sonra və ayar dəyişikliyində çağırılır. */
fun updateAllPrayerWidgets(context: Context) {
    PrayerGlanceWidget().refreshAllInstances(context, KEY_LAST_UPDATE)
}
