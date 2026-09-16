package com.cafarovceyxun.anamuslim.compose.components.qibla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Qiblə kompasının üzü.
 *
 * ### Oxunuş modeli
 * Mərkəzdəki ox **cihazın baxdığı istiqamətdir** və həmişə yuxarı baxır; halqa və onun üstündəki
 * Kəbə nişanı altından fırlanır. İstifadəçi telefonu Kəbə nişanı oxun üstünə gələnə qədər çevirir.
 *
 * ### ⚠️ Niyə əyri (halqa boyunca) mətn yoxdur
 * İlk variantda etiketlər halqa boyunca çəkilirdi — nümunədəki kimi. İki səbəbdən atıldı:
 * birincisi, oriyentasiya cihazda tərs çıxırdı; ikincisi və həlledicisi — belə çəkiliş sətri
 * **hərf-hərf** ayırmağı tələb edir, **ərəbcə isə birləşən yazıdır**. Hərfləri ayrı-ayrı çəkmək
 * onların birləşmə formalarını dağıdır, yəni tətbiqin beş dilindən biri tamamilə sınıq görünərdi.
 * Nə kompilyator, nə testlər bunu tutardı — yalnız ərəb dilinə keçəndə görünərdi.
 *
 * ### Rəng
 * Fon və ox **tədricən** dəyişir: qiblədən uzaqda soyuq/qırmızı, yaxınlaşdıqca isti/yaşıl.
 * Beləcə istiqamət hissi «isti-soyuq» oyunu kimi işləyir — yalnız son anda yanan bir işıq deyil.
 * Rənglər mövzudan **asılı olmayaraq** eyni mənanı daşıyır, amma fon mövzunun öz səthinin üstünə
 * qatılır ki, işıqlı və qaranlıq rejimin hər ikisində oxunaqlı qalsın.
 */
@Composable
fun QiblaCompassFace(
    placeName: String,
    subtitle: String,
    statusText: String,
    trueHeadingDeg: Double,
    qiblaBearingDeg: Double,
    deltaDeg: Double,
    distanceLabel: String,
    elevationLabel: String?,
    modifier: Modifier = Modifier,
) {
    val nearness = (1.0 - (abs(deltaDeg) / COLOR_SPAN_DEG).coerceIn(0.0, 1.0)).toFloat()
    val accent = lerp(FarRed, NearGreen, nearness)

    val surface = colorScheme.surface
    // Qatılıq yaxınlıqla artır: qibləyə yönələndə ekran nəzərəçarpacaq dərəcədə «isinir»,
    // uzaqda isə fon sakit qalır və mətni boğmur.
    val tintAlpha = 0.30f + 0.45f * nearness
    val tint = lerp(FarTint, NearTint, nearness).copy(alpha = tintAlpha).compositeOver(surface)

    val ringColor = colorScheme.onSurface.copy(alpha = 0.22f)
    val labelColor = colorScheme.onSurface.copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tint, surface))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = placeName,
                style = typography.headlineSmall,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val centre = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) / 2f * 0.78f

                drawCircle(
                    color = ringColor,
                    radius = radius,
                    center = centre,
                    style = Stroke(width = 2f),
                )

                // Halqanın altındakı kiçik nişan — istinad nöqtəsi.
                drawLine(
                    color = ringColor,
                    start = centre + polarOffset(180.0, radius - 10f),
                    end = centre + polarOffset(180.0, radius + 10f),
                    strokeWidth = 3f,
                )

                // --- Kəbə nişanı halqanın üstündə, qiblə istiqamətində ---
                val kaabaCentre = centre + polarOffset(qiblaBearingDeg - trueHeadingDeg, radius)
                val badge = radius * 0.13f

                drawCircle(color = BadgeFill, radius = badge, center = kaabaCentre)
                drawCircle(
                    color = accent,
                    radius = badge,
                    center = kaabaCentre,
                    style = Stroke(width = 3f),
                )
                drawKaabaMark(centre = kaabaCentre, size = badge * 0.5f, color = BadgeInk)

                // --- Mərkəzdəki ox: cihazın baxdığı istiqamət ---
                drawHeadingArrow(centre = centre, size = radius * 0.3f, color = accent)
              }

              Text(
                  text = distanceLabel,
                  style = typography.bodyMedium,
                  color = labelColor,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).width(88.dp),
              )
              elevationLabel?.let {
                  Text(
                      text = it,
                      style = typography.bodyMedium,
                      color = labelColor,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).width(88.dp),
                  )
              }
            }

            Text(
                text = statusText,
                style = typography.titleMedium,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

/** Qiblədən uzaq. */
private val FarRed = Color(0xFFC62828)

/** Qibləyə yönəlmiş. */
private val NearGreen = Color(0xFF2E7D32)

/** Fon çalarları — səthin üstünə qatılır, ona görə hər iki mövzuda işləyir. */
private val FarTint = Color(0xFF8C4038)
private val NearTint = Color(0xFFE8B75A)

private val BadgeFill = Color(0xFFFFFFFF)
private val BadgeInk = Color(0xFF1B1B1F)

/**
 * Rəng keçidinin əhatəsi: bu bucaqdan uzaqda rəng tam «uzaq» olur.
 *
 * 60° seçilib ki, telefonu çevirərkən dəyişim gözlə izlənə bilsin — dar aralıqda rəng sıçrayır,
 * çox geniş aralıqda isə hərəkətsiz görünür.
 */
private const val COLOR_SPAN_DEG = 60.0

/** Kəbə nişanı — sadə kub. İkon yükləmədən Canvas-da çəkilir. */
internal fun DrawScope.drawKaabaMark(centre: Offset, size: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(centre.x - size, centre.y - size),
        size = androidx.compose.ui.geometry.Size(size * 2, size * 2),
    )
    // Kisvənin qızıl zolağı — nişanı adi kvadratdan ayırır.
    drawLine(
        color = Color(0xFFD4AF37),
        start = Offset(centre.x - size, centre.y - size * 0.25f),
        end = Offset(centre.x + size, centre.y - size * 0.25f),
        strokeWidth = size * 0.45f,
    )
}

/** Naviqasiya oxu — ucu yuxarı, dibi içəri çökük. */
private fun DrawScope.drawHeadingArrow(centre: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(centre.x, centre.y - size)
        lineTo(centre.x + size * 0.62f, centre.y + size * 0.78f)
        lineTo(centre.x, centre.y + size * 0.34f)
        lineTo(centre.x - size * 0.62f, centre.y + size * 0.78f)
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
