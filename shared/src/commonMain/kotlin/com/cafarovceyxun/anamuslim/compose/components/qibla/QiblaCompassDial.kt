package com.cafarovceyxun.anamuslim.compose.components.qibla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.qiblaAboveSeaLevel
import com.cafarovceyxun.anamuslim.resources.qiblaAligned
import com.cafarovceyxun.anamuslim.resources.qiblaAtKaaba
import com.cafarovceyxun.anamuslim.resources.qiblaBearingValue
import com.cafarovceyxun.anamuslim.resources.qiblaDeclinationNone
import com.cafarovceyxun.anamuslim.resources.qiblaDeclinationValue
import com.cafarovceyxun.anamuslim.resources.qiblaHeld
import com.cafarovceyxun.anamuslim.resources.qiblaHold
import com.cafarovceyxun.anamuslim.resources.qiblaKmShort
import com.cafarovceyxun.anamuslim.resources.qiblaSunDown
import com.cafarovceyxun.anamuslim.resources.qiblaSunValue
import com.cafarovceyxun.anamuslim.resources.qiblaTurnLeftShort
import com.cafarovceyxun.anamuslim.resources.qiblaTurnRightShort
import com.cafarovceyxun.anamuslim.utils.qibla.QiblaHeading
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Qiblə kompasının üzü.
 *
 * ### Oxunuş modeli
 * Halqanın təpəsindəki **sabit indeks** cihazın baxdığı istiqamətdir. Qövs oradan başlayır və Kəbə
 * nişanında bitir: **qövsün özü təlimatdır** — nə qədər dönmək lazımdırsa, o qədər uzundur və
 * düzləndikcə yox olur. İstifadəçi rəqəm oxumaya da bilər; nişanı təpədəki dilimin içinə salmaq
 * kifayətdir.
 *
 * ### ⚠️ Niyə əyri (halqa boyunca) mətn yoxdur
 * Belə çəkiliş sətri **hərf-hərf** ayırmağı tələb edir, **ərəbcə isə birləşən yazıdır** — hərfləri
 * ayrı çəkmək onların birləşmə formalarını dağıdır, yəni tətbiqin beş dilindən biri tamamilə sınıq
 * görünərdi. Nə kompilyator, nə testlər bunu tutur. Ona görə halqada ümumiyyətlə mətn yoxdur,
 * bütün rəqəmlər altdakı çiplərdədir.
 *
 * ### Rəng və forma
 * Rəng **tədricən** dəyişir: qiblədən uzaqda qırmızı, yaxınlaşdıqca tətbiqin yaşılı. Amma
 * «düzləndim» siqnalı yalnız rəng deyil — hədəf dilimi dolur, qövs yox olur, mərkəzdə təsdiq
 * nişanı çıxır. Rəng korluğunda da işləməsi üçün **forma da dəyişməlidir**.
 *
 * ⚠️ Bütün xətt qalınlıqları `dp.toPx()`-dən keçir. [DrawScope] piksellə işləyir, ona görə birbaşa
 * `Stroke(width = 2f)` yazmaq halqanı sıxlıq artdıqca nazildir (3x cihazda 0.67 dp, 4x-də 0.5 dp).
 */
@Composable
fun QiblaCompassFace(
    state: QiblaFaceState,
    notice: QiblaNotice?,
    isHeld: Boolean,
    onHeldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nearness = (1.0 - (abs(state.deltaDeg) / COLOR_SPAN_DEG).coerceIn(0.0, 1.0)).toFloat()
    val accent = qiblaAccent(nearness)

    val disc = colorScheme.surfaceVariant
    val track = colorScheme.onSurfaceVariant.copy(alpha = 0.12f).compositeOver(disc)
    val hint = colorScheme.onSurfaceVariant

    Column(modifier.fillMaxSize()) {
        notice?.let {
            QiblaNoticeBar(
                notice = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.placeName,
                style = typography.headlineSmall,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .widthIn(max = MAX_FACE_WIDTH)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawQiblaFace(
                        state = state,
                        accent = accent,
                        disc = disc,
                        track = track,
                        hint = hint,
                    )
                }

                FaceCentre(state = state, accent = accent)
            }

            FaceChips(state = state, modifier = Modifier.padding(top = 16.dp))

            HoldButton(
                isHeld = isHeld,
                accent = accent,
                onHeldChange = onHeldChange,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/**
 * Üzün bir andakı vəziyyəti.
 *
 * Bucaqların hamısı **həqiqi şimaldan**, dərəcə ilə. Hesablama burada aparılmır — bu, sırf çəkiliş
 * üçün hazırlanmış mənzərədir.
 */
data class QiblaFaceState(
    val placeName: String,
    /** Cihazın baxdığı istiqamət — halqadakı hər şey bunun əksinə fırlanır. */
    val trueHeadingDeg: Double,
    val qiblaBearingDeg: Double,
    /** Qibləyə çatmaq üçün neçə dərəcə dönmək lazımdır, `−180..180` (müsbət = sağa). */
    val deltaDeg: Double,
    val isAligned: Boolean,
    /** Sensorun ± xətası, və ya heç bir mənbə bilmirsə null — onda yay çəkilmir. */
    val accuracyDeg: Double?,
    /** Maqnit sapması, və ya tapılmayıbsa null — onda maqnit nişanı çəkilmir. */
    val declinationDeg: Double?,
    /** Günəşin azimutu, **yalnız üfüqün üstündə olanda**; batıbsa null. */
    val sunAzimuthDeg: Double?,
    val distanceMeters: Double,
    /**
     * Dəniz səviyyəsindən hündürlük, **yalnız həqiqətən gələndə**.
     *
     * Tətbiq kobud mövqe istəyir, kobud mövqe isə adətən hündürlük vermir və sahə 0 qalır —
     * sıfırı «dəniz səviyyəsi» kimi yazmaq yanlış olardı, ona görə belə halda null gəlir və çip
     * ümumiyyətlə görünmür.
     */
    val elevationMeters: Double?,
)

/** Dəqiqlik zolağının ciddiliyi — rəngi və ikonu bundan çıxır. */
enum class QiblaNoticeLevel { OK, INFO, WARN, BAD }

/**
 * Ekranın başındakı tək zolaq.
 *
 * ⚠️ Əvvəl burada **üç ayrı `Text`** vardı və hamısı `error` rəngində, ekranın dibində yığılırdı —
 * «kalibrlə» ilə «metal var» eyni təcillikdə görünürdü, üçü birdən çıxanda isə status mətninin
 * üstünə minirdi. İndi yalnız ən ciddisi göstərilir, [detail] isə basanda açılır.
 */
data class QiblaNotice(
    val level: QiblaNoticeLevel,
    /** Qısa vəziyyət — bir sətir. */
    val text: String,
    /** Basanda açılan izah, və ya yoxdursa null (onda zolaq basıla bilmir). */
    val detail: String?,
)

@Composable
private fun QiblaNoticeBar(notice: QiblaNotice, modifier: Modifier = Modifier) {
    var expanded by remember(notice.text) { mutableStateOf(false) }

    val container = when (notice.level) {
        QiblaNoticeLevel.OK -> Color.Transparent
        QiblaNoticeLevel.INFO, QiblaNoticeLevel.WARN -> colorScheme.surfaceVariant
        QiblaNoticeLevel.BAD -> colorScheme.errorContainer
    }
    val content = when (notice.level) {
        QiblaNoticeLevel.OK, QiblaNoticeLevel.INFO, QiblaNoticeLevel.WARN ->
            colorScheme.onSurfaceVariant

        QiblaNoticeLevel.BAD -> colorScheme.onErrorContainer
    }
    val iconTint = when (notice.level) {
        QiblaNoticeLevel.OK -> content
        QiblaNoticeLevel.INFO -> colorScheme.secondary
        QiblaNoticeLevel.WARN -> colorScheme.tertiary
        QiblaNoticeLevel.BAD -> content
    }

    // Sakit haldakı zolaq sadəcə bir sətirdir: heç nə tələb etmir, ona görə qutu da geyinmir.
    if (notice.level == QiblaNoticeLevel.OK) {
        Text(
            text = notice.text,
            style = typography.bodySmall,
            color = content,
            textAlign = TextAlign.Center,
            modifier = modifier,
        )
        return
    }

    val icon = when (notice.level) {
        QiblaNoticeLevel.INFO -> Res.drawable.dr_icon_info
        else -> Res.drawable.dr_icon_report_problem
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .then(
                if (notice.detail == null) Modifier
                else Modifier.clickable { expanded = !expanded },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = notice.text,
                style = typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            if (notice.detail != null) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (expanded && notice.detail != null) {
            Text(
                text = notice.detail,
                style = typography.bodySmall,
                color = content,
                modifier = Modifier.padding(top = 8.dp, start = 28.dp),
            )
        }
    }
}

@Composable
private fun FaceCentre(state: QiblaFaceState, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.isAligned) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_check_circle),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(Res.string.qiblaAligned),
                style = typography.titleSmall,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            return@Column
        }

        Text(
            text = "${abs(state.deltaDeg).roundToInt()}°",
            // ⚠️ Ərəbcə interfeysdə istiqamətini özü təyin etməyən mətn çevrilir və dərəcə işarəsi
            // rəqəmin soluna düşür. Ölçü rəqəmidir, ona görə yazı istiqaməti açıq verilir.
            style = typography.displaySmall.copy(textDirection = TextDirection.Ltr),
            color = colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                if (state.deltaDeg >= 0.0) Res.string.qiblaTurnRightShort
                else Res.string.qiblaTurnLeftShort,
            ),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FaceChips(state: QiblaFaceState, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InfoChip(
            text = stringResource(
                Res.string.qiblaBearingValue,
                state.qiblaBearingDeg.roundToInt().toString(),
            ),
            emphasised = true,
        )

        InfoChip(
            text = if (state.distanceMeters < 1_000.0) {
                stringResource(Res.string.qiblaAtKaaba)
            } else {
                stringResource(Res.string.qiblaKmShort, kilometreLabel(state.distanceMeters))
            },
        )

        InfoChip(
            text = state.declinationDeg?.let {
                stringResource(Res.string.qiblaDeclinationValue, signedDegrees(it))
            } ?: stringResource(Res.string.qiblaDeclinationNone),
        )

        InfoChip(
            text = state.sunAzimuthDeg?.let {
                stringResource(Res.string.qiblaSunValue, it.roundToInt().toString())
            } ?: stringResource(Res.string.qiblaSunDown),
        )

        state.elevationMeters?.let {
            InfoChip(
                text = stringResource(
                    Res.string.qiblaAboveSeaLevel,
                    it.roundToInt().toString(),
                ),
            )
        }
    }
}

@Composable
private fun InfoChip(text: String, emphasised: Boolean = false) {
    Text(
        text = text,
        style = typography.labelMedium,
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (emphasised) colorScheme.surfaceVariant
                else colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun HoldButton(
    isHeld: Boolean,
    accent: Color,
    onHeldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onHeldChange(!isHeld) },
        modifier = modifier,
        colors = if (isHeld) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = accent,
                contentColor = Color.White,
            )
        } else {
            // Mövzunun `secondaryContainer`-i doymuş firuzəyidir və bu ekranda tək yad ləkə kimi
            // görünür — sakit halda düymə səthin öz tonunda qalır.
            ButtonDefaults.filledTonalButtonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurfaceVariant,
            )
        },
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_pause),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(if (isHeld) Res.string.qiblaHeld else Res.string.qiblaHold),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Məsafə etiketi: 10 km-ə qədər bir onluq rəqəmlə, sonra tam ədədlə.
 *
 * Yaxında tam kilometr çox kobuddur — Kəbəyə 1.2 km ilə 1.8 km arasındakı fərq istifadəçi üçün
 * mənalıdır; uzaqda isə onluq rəqəm mənasız dəqiqlik təəssüratı yaradır.
 */
internal fun kilometreLabel(distanceMeters: Double): String {
    val km = distanceMeters / 1000.0
    if (km >= 10.0) return km.roundToInt().toString()

    val tenths = (km * 10.0).roundToInt()

    return "${tenths / 10}.${tenths % 10}"
}

/** Sapma işarəsi ilə: `+6`, `−3`. Sıfır da müsbət sayılır — «sapma yoxdur» ayrıca haldır. */
private fun signedDegrees(value: Double): String {
    val rounded = value.roundToInt()

    return if (rounded < 0) "−${-rounded}" else "+$rounded"
}

// region — çəkiliş

private fun DrawScope.drawQiblaFace(
    state: QiblaFaceState,
    accent: Color,
    disc: Color,
    track: Color,
    hint: Color,
) {
    val centre = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) / 2f

    val discRadius = radius * 0.92f
    val outerRadius = radius * 0.80f
    val trackRadius = radius * 0.64f
    val trackWidth = 11.dp.toPx()
    val markerRadius = radius * 0.125f

    // --- Tonal disk; düzləndikdə rəngə dolur və konturla halqalanır ---
    drawCircle(
        color = if (state.isAligned) accent.copy(alpha = 0.12f).compositeOver(disc) else disc,
        radius = discRadius,
        center = centre,
    )
    if (state.isAligned) {
        drawCircle(
            color = accent.copy(alpha = 0.5f),
            radius = discRadius,
            center = centre,
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    // --- Dörd istiqamət nöqtəsi: cihazla fırlanır, şimal bir az iridir ---
    // Sadə üzün tək zəifliyi bu idi — qövs «nə qədər» deyirdi, amma ekranda heç nə fırlanmırdı,
    // yəni telefonu çevirəndə hərəkət hissi olmurdu. Dörd nöqtə bunu bölgü qoymadan verir.
    for (quarter in 0 until 4) {
        val bearing = quarter * 90.0 - state.trueHeadingDeg
        val isNorth = quarter == 0

        drawCircle(
            color = hint.copy(alpha = if (isNorth) 0.6f else 0.3f),
            radius = if (isNorth) 3.dp.toPx() else 2.dp.toPx(),
            center = centre + polarOffset(bearing, outerRadius),
        )
    }

    // --- Maqnit şimalı: kəsik nişan. Sapma tapılmayanda ümumiyyətlə çəkilmir ---
    state.declinationDeg?.let { declination ->
        val bearing = declination - state.trueHeadingDeg
        val arm = 5.dp.toPx()

        drawLine(
            color = hint.copy(alpha = 0.5f),
            start = centre + polarOffset(bearing, outerRadius - arm),
            end = centre + polarOffset(bearing, outerRadius + arm),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(2.dp.toPx(), 2.dp.toPx()),
            ),
        )
    }

    // --- Qeyri-müəyyənlik yayı: nişanın gəzə biləcəyi sahə ---
    state.accuracyDeg?.let { accuracy ->
        val half = accuracy.coerceIn(0.0, MAX_ACCURACY_ARC_DEG)

        drawBearingArc(
            centre = centre,
            radius = outerRadius,
            startBearing = state.deltaDeg - half,
            sweepDeg = half * 2.0,
            color = accent.copy(alpha = 0.5f),
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // --- Trek ---
    drawCircle(
        color = track,
        radius = trackRadius,
        center = centre,
        style = Stroke(width = trackWidth),
    )

    // --- Hədəf pəncərəsi: qövsün bitməli olduğu yer, sabitdir ---
    drawBearingArc(
        centre = centre,
        radius = trackRadius,
        startBearing = -QiblaHeading.ALIGNED_THRESHOLD_DEG,
        sweepDeg = QiblaHeading.ALIGNED_THRESHOLD_DEG * 2.0,
        color = accent.copy(alpha = if (state.isAligned) 1f else 0.45f),
        width = trackWidth,
        cap = StrokeCap.Butt,
    )

    // --- Qövsün özü təlimatdır ---
    if (!state.isAligned) {
        drawBearingArc(
            centre = centre,
            radius = trackRadius,
            startBearing = 0.0,
            sweepDeg = state.deltaDeg,
            color = accent,
            width = trackWidth,
            cap = StrokeCap.Round,
        )
    }

    // --- Günəş: sensordan asılı olmayan yoxlama nöqtəsi ---
    state.sunAzimuthDeg?.let { azimuth ->
        drawSunMark(
            centre = centre + polarOffset(azimuth - state.trueHeadingDeg, outerRadius),
            size = radius * 0.055f,
            color = SunGold,
        )
    }

    // --- Kəbə nişanı qövsün ucunda ---
    val markerCentre = centre + polarOffset(state.deltaDeg, trackRadius)
    drawCircle(color = accent, radius = markerRadius, center = markerCentre)
    drawKaabaMark(centre = markerCentre, size = markerRadius * 0.42f, color = Color.White)

    // --- Sabit indeks: cihazın baxdığı istiqamət ---
    drawIndexMark(centre = centre, radius = radius * 0.86f, color = hint.copy(alpha = 0.7f))
}

/** Şimaldan ölçülən bucaqla yay çəkir. Compose-un 0°-si saat 3-dədir, ona görə 90° çıxılır. */
private fun DrawScope.drawBearingArc(
    centre: Offset,
    radius: Float,
    startBearing: Double,
    sweepDeg: Double,
    color: Color,
    width: Float,
    cap: StrokeCap,
) {
    drawArc(
        color = color,
        startAngle = (startBearing - 90.0).toFloat(),
        sweepAngle = sweepDeg.toFloat(),
        useCenter = false,
        topLeft = Offset(centre.x - radius, centre.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = width, cap = cap),
    )
}

/** Kəbə nişanı — sadə kub. İkon yükləmədən Canvas-da çəkilir. */
internal fun DrawScope.drawKaabaMark(centre: Offset, size: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(centre.x - size, centre.y - size),
        size = Size(size * 2, size * 2),
    )
    // Kisvənin qızıl zolağı — nişanı adi kvadratdan ayırır.
    drawLine(
        color = KaabaGold,
        start = Offset(centre.x - size, centre.y - size * 0.25f),
        end = Offset(centre.x + size, centre.y - size * 0.25f),
        strokeWidth = size * 0.45f,
    )
}

private fun DrawScope.drawSunMark(centre: Offset, size: Float, color: Color) {
    drawCircle(color = color, radius = size * 0.5f, center = centre)

    for (ray in 0 until 8) {
        val bearing = ray * 45.0

        drawLine(
            color = color,
            start = centre + polarOffset(bearing, size * 0.8f),
            end = centre + polarOffset(bearing, size * 1.25f),
            strokeWidth = size * 0.22f,
            cap = StrokeCap.Round,
        )
    }
}

/** Təpədəki sabit üçbucaq — aşağı, hədəf pəncərəsinə baxır. */
private fun DrawScope.drawIndexMark(centre: Offset, radius: Float, color: Color) {
    val tip = centre + polarOffset(0.0, radius)
    val halfWidth = 6.dp.toPx()
    val height = 8.dp.toPx()

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x - halfWidth, tip.y - height)
        lineTo(tip.x + halfWidth, tip.y - height)
        close()
    }

    drawPath(path = path, color = color)
}

/** Bucağı (şimaldan, saat əqrəbi ilə) ekran ofsetinə çevirir. Ekranda Y aşağı artır. */
private fun polarOffset(angleDeg: Double, distance: Float): Offset {
    val radians = angleDeg * PI / 180.0

    return Offset(
        x = (sin(radians) * distance).toFloat(),
        y = (-cos(radians) * distance).toFloat(),
    )
}

// endregion

/**
 * Yaxınlıq rəngi: qırmızı → narıncı → yaşıl.
 *
 * ⚠️ **Orta nöqtə qəsdən var.** Qırmızını birbaşa yaşıla qarışdırmaq (istər sRGB, istər Oklab)
 * aralıqda **palçıq qəhvəyi** verir — cihazda ölçüldü: 33° fərqdə qövs çirkli qəhvəyi çıxırdı və
 * «isinir» yox, «xarab olub» kimi oxunurdu. İsti tondan isti tona keçid bu zonanı yaradmır, ona
 * görə yol iki mərhələyə bölünür.
 */
private fun qiblaAccent(nearness: Float): Color =
    if (nearness <= 0.5f) lerp(FarRed, MidAmber, nearness * 2f)
    else lerp(MidAmber, NearGreen, (nearness - 0.5f) * 2f)

/** Qiblədən uzaq. */
private val FarRed = Color(0xFFC62828)

/** Yarı yolda — narıncı. Günəş nişanının sarısından fərqlənməsi üçün qəsdən qırmızıya yaxındır. */
private val MidAmber = Color(0xFFE0701E)

/** Qibləyə yönəlmiş — tətbiqin öz yaşılı. */
private val NearGreen = Color(0xFF008B5B)

private val SunGold = Color(0xFFE8B930)
private val KaabaGold = Color(0xFFD4AF37)

/**
 * Rəng keçidinin əhatəsi: bu bucaqdan uzaqda rəng tam «uzaq» olur.
 *
 * 60° seçilib ki, telefonu çevirərkən dəyişim gözlə izlənə bilsin — dar aralıqda rəng sıçrayır,
 * çox geniş aralıqda isə hərəkətsiz görünür.
 */
private const val COLOR_SPAN_DEG = 60.0

/** Yay bundan geniş çəkilmir: yarım dairəni keçən yay artıq məlumat vermir, sadəcə səliqəsizdir. */
private const val MAX_ACCURACY_ARC_DEG = 60.0

/** Planşetdə üz bütün eni tutmasın — 380 dp-dən sonra oxunaqlılıq artmır, boşluq isə itir. */
private val MAX_FACE_WIDTH = 380.dp
