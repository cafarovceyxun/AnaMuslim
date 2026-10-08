package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.preferences.WidgetAppearancePreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_menu
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.ic_skip_back
import com.cafarovceyxun.anamuslim.resources.ic_skip_forward
import com.cafarovceyxun.anamuslim.resources.prayerTimesTitle
import com.cafarovceyxun.anamuslim.resources.prayerWidgetDateLine
import com.cafarovceyxun.anamuslim.resources.prayerWidgetRemaining
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.resources.strLabelVerseWithChapNameAndNo
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.resources.widgetPreviewChapterName
import com.cafarovceyxun.anamuslim.resources.widgetPreviewVerseTranslation
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerWidgetSnapshot
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerWidgetSnapshotBuilder
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Ana ekran vidcetinin canlı önizləməsi — sürüşdürücü çəkiləndə fon və yazı **dərhal** dəyişir,
 * launcher-in yenidən çəkməsini gözləmədən.
 *
 * ### Nə qədər dəqiqdir
 * Vidcet Glance-dir (RemoteViews), bu isə onun Compose qarşılığıdır: düzülüş, ölçülər, rənglər və
 * kiçilmə hədləri `:app`-dakı Glance kodundan **köçürülüb** (`PrayerWidgetReceiver.kt`,
 * `VotdWidgetReceiver.kt`, `RecitationPlayerWidgetUi.kt`). Orada düzülüş dəyişəndə burada da dəyiş —
 * kompilyator bu iki yerin ayrıldığını xəbər vermir, önizləmə sadəcə yalan danışmağa başlayır.
 *
 * Vidcet öz **həqiqi dp ölçüsündə** çəkilir və sonra ekrana sığana qədər bütövlükdə kiçildilir
 * ([FitToWidth]) — yəni 475dp-lik pleyer 360dp-lik ekranda da sətir bölgüsünü və kəsilməni olduğu
 * kimi göstərir, eni daralıb yazını başqa cür sındırmır.
 *
 * Namaz önizləməsi istifadəçinin **həqiqi** vaxtlarını göstərir (iOS vidcetinin snapshot-u ilə
 * eyni qurucu); yer seçilməyibsə nümunə vaxtlar. Günün ayəsi və pleyer nümunə ayə ilə çəkilir —
 * onların məzmunu Quran bazasından gəlir, ölçünü yoxlamaq üçün isə həqiqi ayə lazım deyil.
 *
 * Önizləmə `fontScale = 1`-də çəkilir: vidcet yazıları sistem şrift miqyasını kompensasiya edir
 * (`:app` → `WidgetTextScale.kt`, `wsp`), yəni telefonda şrift nə qədər böyük olsa da vidcetdəki
 * ölçünü yalnız bu ekrandakı əmsal təyin edir.
 *
 * @param placedSize konfiqurasiya olunan vidcetin launcher-də **həqiqi** ölçüsü (bilinirsə).
 * @param weekdayName həftənin günü — namaz vidcetinin tarix sətri üçün, vidcetdəki kimi platformadan.
 */
@Composable
fun WidgetAppearancePreview(
    kind: HomeWidgetKind,
    placedSize: DpSize?,
    weekdayName: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val alpha = WidgetAppearancePreferences.observeOpacityPercent(kind) / 100f
    val textScale = WidgetAppearancePreferences.observeTextScalePercent(kind) / 100f
    val size = placedSize ?: defaultSizeOf(kind)

    // Divar kağızı qəsdən həm tünd, həm açıq zolaqlıdır: qatılıq yalnız açıq fonda gözə çarpır.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF14365C),
                        Color(0xFF2E8B83),
                        Color(0xFFF2C14E),
                        Color(0xFFF4E3C1),
                        Color(0xFFE76F51),
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        FitToWidth(size) {
            val density = LocalDensity.current

            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1f),
            ) {
                val ts = PreviewTextScale(textScale)

                when (kind) {
                    HomeWidgetKind.PrayerTimes,
                    HomeWidgetKind.PrayerTimesWithLogo,
                    -> PrayerPreview(
                        showAllTimes = kind == HomeWidgetKind.PrayerTimesWithLogo,
                        size = size,
                        alpha = alpha,
                        ts = ts,
                        weekdayName = weekdayName,
                    )

                    HomeWidgetKind.VerseOfTheDay -> VotdPreview(size, alpha, ts)
                    HomeWidgetKind.RecitationPlayer -> PlayerPreview(size, alpha, ts)
                }
            }
        }
    }
}

/**
 * Vidcetin olmadığı (Ayarlardan açılan) hal üçün ölçülər — Galaxy-nin 6 sütunlu şəbəkəsində
 * `targetCell*` dəyərlərinin verdiyi təxmini dp (dumpsys: 3 sütun ≈ 227dp, 6 sütun ≈ 475dp,
 * bir sıra ≈ 106dp).
 */
private fun defaultSizeOf(kind: HomeWidgetKind): DpSize = when (kind) {
    HomeWidgetKind.PrayerTimes -> DpSize(227.dp, 106.dp)
    HomeWidgetKind.PrayerTimesWithLogo -> DpSize(475.dp, 106.dp)
    HomeWidgetKind.RecitationPlayer -> DpSize(475.dp, 232.dp)
    HomeWidgetKind.VerseOfTheDay -> DpSize(475.dp, 232.dp)
}

/** [size]-da çəkir, sonra mövcud enə sığana qədər bütövlükdə kiçildir (heç vaxt böyütmür). */
@Composable
private fun FitToWidth(size: DpSize, content: @Composable () -> Unit) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val scale = (maxWidth / size.width).coerceAtMost(1f)

        Box(
            modifier = Modifier.size(size.width * scale, size.height * scale),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(size)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                content()
            }
        }
    }
}

/** Vidcetdəki `wsp`-nin qarşılığı: vidcetin öz `sp` dəyəri × istifadəçinin əmsalı. */
private class PreviewTextScale(val factor: Float) {
    fun sp(value: Int): TextUnit = (value * factor).sp
}

/** Launcher vidcet mətnini sistem şrifti ilə çəkir, tətbiqin tipoqrafiyası ilə yox. */
private fun widgetText(color: Color, size: TextUnit, weight: FontWeight? = null) = TextStyle(
    color = color,
    fontSize = size,
    fontWeight = weight,
    fontFamily = FontFamily.Default,
)

@Composable
private fun WText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        // RemoteViews `TextView`-u sığmayanı kəsir, «…» qoymur.
        overflow = TextOverflow.Clip,
    )
}

// ==================== Namaz ====================
// `PrayerWidgetReceiver.kt`-dən köçürülmüş hədlər.

private const val PRAYER_CORNER_DP = 16
private const val PRAYER_PADDING_DP = 8
private const val COMPACT_ICON_DP = 30
private const val COMPACT_WIDE_MIN_WIDTH_DP = 220
private const val COMPACT_PLACE_MIN_HEIGHT_DP = 130
private const val DATE_BESIDE_MIN_WIDTH_DP = 320
private const val DATE_LINE_MIN_HEIGHT_DP = 92
private const val ICON_MIN_HEIGHT_DP = 76
private const val COUNTDOWN_SP = 18
private val ACCENT_GREEN = Color(0xFF19B37E)

private data class PreviewPrayerRow(
    val label: String,
    val clock: String,
    val icon: DrawableResource?,
    val isNext: Boolean,
)

private data class PreviewPrayerState(
    val nextLabel: String?,
    val nextClock: String?,
    val nextIcon: DrawableResource?,
    val nextAtMillis: Long?,
    val placeName: String,
    val dateLine: String?,
    val rows: List<PreviewPrayerRow>,
)

private fun iconOf(name: String): DrawableResource? =
    Prayer.entries.firstOrNull { it.name.lowercase() == name }?.let(PrayerUiFormat::iconOf)

private fun PrayerWidgetSnapshot.toPreview(now: Long): PreviewPrayerState? {
    val today = days.firstOrNull() ?: return null
    val next = days.asSequence()
        .flatMap { it.items.asSequence() }
        .firstOrNull { it.countsAsNext && it.atMillis > now }

    return PreviewPrayerState(
        nextLabel = next?.label,
        nextClock = next?.clock,
        nextIcon = next?.let { iconOf(it.icon) },
        nextAtMillis = next?.atMillis,
        placeName = placeName,
        dateLine = today.dateLine.ifBlank { null },
        rows = today.items.map {
            PreviewPrayerRow(
                label = it.label,
                clock = it.clock,
                icon = iconOf(it.icon),
                isNext = next != null && it.atMillis == next.atMillis,
            )
        },
    )
}

/** Yer seçilməyibsə: vidcet «yer seçin» yazır, amma ölçünü görmək üçün nümunə cədvəl lazımdır. */
@Composable
private fun samplePrayerState(now: Long, weekdayName: (Long) -> String): PreviewPrayerState {
    val clocks = listOf("05:12", "06:41", "12:47", "15:58", "18:43", "20:06")
    val next = Prayer.DHUHR
    val nextAt = remember { now + (2 * 60 + 15) * 60_000L + 3_000L }

    val hijri = PrayerUiFormat.hijri(now)
    val dateLine = hijri?.let { stringResource(Res.string.prayerWidgetDateLine, weekdayName(now), it) }

    return PreviewPrayerState(
        nextLabel = PrayerUiFormat.label(next),
        nextClock = clocks[next.ordinal],
        nextIcon = PrayerUiFormat.iconOf(next),
        nextAtMillis = nextAt,
        placeName = "",
        dateLine = dateLine,
        rows = Prayer.entries.mapIndexed { index, prayer ->
            PreviewPrayerRow(
                label = PrayerUiFormat.label(prayer),
                clock = clocks.getOrElse(index) { "" },
                icon = PrayerUiFormat.iconOf(prayer),
                isNext = prayer == next,
            )
        },
    )
}

@Composable
private fun PrayerPreview(
    showAllTimes: Boolean,
    size: DpSize,
    alpha: Float,
    ts: PreviewTextScale,
    weekdayName: (Long) -> String,
) {
    val appLocale = LocalAppLocale.current
    val snapshot by produceState<PrayerWidgetSnapshot?>(null, appLocale) {
        value = PrayerWidgetSnapshotBuilder.build(weekdayName)
    }
    // Geri sayım vidcetdə `Chronometer`-dir və saniyəbəsaniyə işləyir — önizləmədə də.
    val now by produceState(currentEpochMillis()) {
        while (true) {
            delay(1_000)
            value = currentEpochMillis()
        }
    }

    val state = snapshot?.toPreview(now) ?: samplePrayerState(now, weekdayName)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(PRAYER_CORNER_DP.dp))
            .background(Color.Black.alpha(alpha))
            .padding(PRAYER_PADDING_DP.dp),
    ) {
        if (showAllTimes) {
            AllTimesPreview(state, size, ts)
        } else {
            NextPrayerPreview(state, size, now, ts)
        }
    }
}

@Composable
private fun NextPrayerPreview(state: PreviewPrayerState, size: DpSize, now: Long, ts: PreviewTextScale) {
    if (state.nextLabel == null || state.nextClock == null) {
        WText(stringResource(Res.string.prayerTimesTitle), widgetText(Color.White.alpha(0.8f), ts.sp(19)))
        return
    }

    val wide = size.width >= COMPACT_WIDE_MIN_WIDTH_DP.dp
    val showPlace = state.placeName.isNotBlank() &&
        (wide || size.height >= COMPACT_PLACE_MIN_HEIGHT_DP.dp)

    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        if (state.nextIcon != null) {
            Image(
                painter = painterResource(state.nextIcon),
                contentDescription = null,
                modifier = Modifier.size(COMPACT_ICON_DP.dp),
                colorFilter = ColorFilter.tint(ACCENT_GREEN),
            )
            Spacer(Modifier.width(8.dp))
        }

        Column {
            WText(state.nextLabel, widgetText(Color.White.alpha(0.7f), ts.sp(19)), maxLines = 1)
            WText(
                state.nextClock,
                widgetText(ACCENT_GREEN, ts.sp(32), FontWeight.Medium),
                maxLines = 1,
            )

            if (!wide) {
                state.nextAtMillis?.let { CountdownPreview(it, now, ts) }
                if (showPlace) PlacePreview(state.placeName, ts)
            }
        }

        if (wide) {
            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                state.nextAtMillis?.let { CountdownPreview(it, now, ts) }
                if (showPlace) PlacePreview(state.placeName, ts)
            }
        }
    }
}

@Composable
private fun PlacePreview(name: String, ts: PreviewTextScale) {
    WText(name, widgetText(Color.White.alpha(0.45f), ts.sp(18)), maxLines = 1)
}

/** `Chronometer`-in geri sayım formatı: `H:MM:SS`, bir saatdan az qalanda `MM:SS`. */
@Composable
private fun CountdownPreview(atMillis: Long, now: Long, ts: PreviewTextScale) {
    val totalSeconds = ((atMillis - now).coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mmss = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val clock = if (hours > 0) "$hours:$mmss" else mmss

    Row(verticalAlignment = Alignment.CenterVertically) {
        WText(clock, widgetText(Color(0x99FFFFFF), ts.sp(COUNTDOWN_SP)), maxLines = 1)
        Spacer(Modifier.width(4.dp))
        WText(
            stringResource(Res.string.prayerWidgetRemaining),
            widgetText(Color.White.alpha(0.6f), ts.sp(COUNTDOWN_SP)),
            maxLines = 1,
        )
    }
}

private data class TimesScale(
    val headerSp: Int,
    val dateSp: Int,
    val labelSp: Int,
    val timeSp: Int,
    val iconDp: Int,
    val logoDp: Int,
    val gapDp: Int,
)

private fun timesScaleFor(heightDp: Float): TimesScale = when {
    heightDp >= 190f -> TimesScale(29, 19, 20, 26, 32, 31, 12)
    heightDp >= 140f -> TimesScale(24, 18, 17, 21, 22, 25, 6)
    else -> TimesScale(21, 17, 16, 19, 14, 22, 2)
}

@Composable
private fun AllTimesPreview(state: PreviewPrayerState, size: DpSize, ts: PreviewTextScale) {
    val scale = timesScaleFor(size.height.value)
    val showIcons = size.height >= ICON_MIN_HEIGHT_DP.dp
    val dateLine = state.dateLine?.takeIf { size.height >= DATE_LINE_MIN_HEIGHT_DP.dp }
    val dateBeside = dateLine != null && size.width >= DATE_BESIDE_MIN_WIDTH_DP.dp

    val headline = if (state.nextLabel != null && state.nextClock != null) {
        "${state.nextLabel} · ${state.nextClock}"
    } else {
        stringResource(Res.string.prayerTimesTitle)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (dateBeside) Arrangement.Start else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(scale.logoDp.dp),
            )
            Spacer(Modifier.width(6.dp))
            WText(headline, widgetText(Color.White, ts.sp(scale.headerSp), FontWeight.Bold), maxLines = 1)

            if (dateBeside && dateLine != null) {
                Spacer(Modifier.weight(1f))
                WText(dateLine, widgetText(Color.White.alpha(0.55f), ts.sp(scale.dateSp)), maxLines = 1)
            }
        }

        if (!dateBeside && dateLine != null) {
            WText(
                dateLine,
                widgetText(Color.White.alpha(0.55f), ts.sp(scale.dateSp)).copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(scale.gapDp.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            state.rows.forEach { row ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (showIcons && row.icon != null) {
                        Image(
                            painter = painterResource(row.icon),
                            contentDescription = null,
                            modifier = Modifier.size(scale.iconDp.dp),
                            colorFilter = ColorFilter.tint(
                                if (row.isNext) ACCENT_GREEN else Color.White.alpha(0.55f)
                            ),
                        )
                    }
                    WText(row.label, widgetText(Color.White.alpha(0.6f), ts.sp(scale.labelSp)), maxLines = 1)
                    WText(
                        row.clock,
                        widgetText(
                            if (row.isNext) ACCENT_GREEN else Color.White.alpha(0.9f),
                            ts.sp(scale.timeSp),
                            if (row.isNext) FontWeight.Bold else FontWeight.Normal,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ==================== Günün ayəsi ====================
// `VotdWidgetReceiver.kt`-dən köçürülmüş hədlər.

private const val VOTD_ARABIC_MIN_HEIGHT_DP = 203
private const val VOTD_HEADER_DP = 42
private const val VOTD_FOOTER_DP = 32
private const val VOTD_TEXT_PADDING_DP = 12

/** Nümunə ayə — Fatihə 1. Vidcet ərəb mətnini seçilmiş mushaf şrifti ilə çəkir, bura təxminidir. */
private const val SAMPLE_ARABIC = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

@Composable
private fun VotdPreview(size: DpSize, alpha: Float, ts: PreviewTextScale) {
    val primary = MaterialTheme.colorScheme.primary
    val chapter = stringResource(Res.string.widgetPreviewChapterName)
    val hasArabic = size.height >= VOTD_ARABIC_MIN_HEIGHT_DP.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            // Vidcetin bişirilmiş fonu: qradiyent + 80% qara örtük, bir qrup kimi [alpha] ilə.
            .graphicsLayer { this.alpha = alpha }
            .background(Brush.linearGradient(listOf(Color.Black, primary, Color.Black)))
            .background(Color.Black.alpha(0.8f)),
    )

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().height(VOTD_HEADER_DP.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WText(
                stringResource(Res.string.strTitleVOTD),
                widgetText(Color.White, ts.sp(13), FontWeight.Medium),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.alpha(0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }

        // Vidcetdə hər iki blok bitmap-dir və öz qutusuna sığana qədər kiçildilir (12sp-dən aşağı
        // yox); əmsal yalnız yuxarı həddi qaldırır. `TextAutoSize` eyni axtarışı edir.
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(VOTD_TEXT_PADDING_DP.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (hasArabic) {
                AutoFitText(
                    text = SAMPLE_ARABIC,
                    style = TextStyle(color = Color.White, fontFamily = arabicFontFamily()),
                    maxSize = ts.sp(36),
                    modifier = Modifier.weight(0.45f),
                )
                Spacer(Modifier.height(8.dp))
            }
            AutoFitText(
                text = stringResource(Res.string.widgetPreviewVerseTranslation),
                style = widgetText(Color.White, 20.sp),
                maxSize = ts.sp(20),
                modifier = Modifier.weight(0.55f),
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.alpha(0.15f)))

        Box(
            modifier = Modifier.fillMaxWidth().height((VOTD_FOOTER_DP - 1).dp),
            contentAlignment = Alignment.Center,
        ) {
            WText(
                stringResource(Res.string.strLabelVerseWithChapNameAndNo, chapter, 1, 1),
                widgetText(Color.White.alpha(0.75f), ts.sp(12)).copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AutoFitText(text: String, style: TextStyle, maxSize: TextUnit, modifier: Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            style = style.copy(textAlign = TextAlign.Center),
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = maxSize, stepSize = 1.sp),
            // `Ellipsis` olmamalıdır — `StepBased` qısaldılmış mətni «sığdı» sayır (ShareImageCard).
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ==================== Pleyer ====================
// `RecitationPlayerWidgetUi.kt` → `PlayerFace`-dən köçürülmüş hədlər.

private const val TALL_PLAYER_MIN_HEIGHT_DP = 108
private const val TRANSPORT_MIN_WIDTH_DP = 300

@Composable
private fun PlayerPreview(size: DpSize, alpha: Float, ts: PreviewTextScale) {
    val colors = MaterialTheme.colorScheme
    val isTall = size.height >= TALL_PLAYER_MIN_HEIGHT_DP.dp
    val chapter = stringResource(Res.string.widgetPreviewChapterName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainer.alpha(alpha))
            .padding(if (isTall) 14.dp else 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (isTall) 56.dp else 44.dp)
                    .clip(RoundedCornerShape(if (isTall) 16.dp else 12.dp))
                    .background(Brush.linearGradient(listOf(colors.primary, colors.primaryContainer))),
            )

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = if (isTall) 12.dp else 10.dp),
            ) {
                WText(
                    stringResource(Res.string.strLabelSurah, chapter),
                    widgetText(colors.onSurface, ts.sp(if (isTall) 17 else 15), FontWeight.Bold),
                    maxLines = if (isTall) 2 else 1,
                )
                Spacer(Modifier.height(3.dp))
                WText(
                    stringResource(Res.string.strLabelVerseNo, 1),
                    widgetText(colors.onSurface.alpha(0.72f), ts.sp(if (isTall) 13 else 12)),
                    maxLines = 1,
                )
            }

            PreviewIconButton(Res.drawable.dr_icon_menu, if (isTall) 44 else 36)

            if (!isTall) {
                Spacer(Modifier.width(6.dp))
                PreviewTransport(
                    showSkip = size.width >= TRANSPORT_MIN_WIDTH_DP.dp,
                    skipSize = 38,
                    playSize = 48,
                    spacing = 6,
                )
            }
        }

        if (isTall) {
            Spacer(Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PreviewTransport(showSkip = true, skipSize = 50, playSize = 62, spacing = 12)
            }
        }
    }
}

@Composable
private fun PreviewTransport(showSkip: Boolean, skipSize: Int, playSize: Int, spacing: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showSkip) {
            PreviewIconButton(Res.drawable.ic_skip_back, skipSize)
            Spacer(Modifier.width(spacing.dp))
        }

        Box(
            modifier = Modifier
                .size(playSize.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_play),
                contentDescription = null,
                modifier = Modifier.size((playSize * 0.5f).dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
            )
        }

        if (showSkip) {
            Spacer(Modifier.width(spacing.dp))
            PreviewIconButton(Res.drawable.ic_skip_forward, skipSize)
        }
    }
}

@Composable
private fun PreviewIconButton(icon: DrawableResource, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size((sizeDp * 0.56f).toInt().coerceAtLeast(17).dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        )
    }
}
