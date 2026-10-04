package com.cafarovceyxun.anamuslim.compose.screens.salah.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils

/**
 * Namaz bələdçisinin xətti illüstrasiyaları (Təharət, 2026-10-04).
 *
 * Şəkillər fayl deyil, **məlumatdır**: [TaharahArtData] `tools/salah-art/ill.js`-dən skriptlə
 * yaradılır, burada isə `Canvas`-da çəkilir. Səbəb: rənglər mövzudan gəlir (tünd rejimdə də işləyir),
 * qırıq xətlər (vurğu, tikiş) `ImageVector`-da yoxdur, eyni çertyoj həm maketdə, həm tətbiqdə işləyir.
 *
 * Üslub istifadəçinin seçimidir: **xətti**, kontur qalın, detallar nazik, çox yüngül çalarlar.
 * Üzdə göz, burun, ağız dəqiq çəkilmir — yalnız çox solğun qaş/burun işarəsi ([ArtRole.SOFT]).
 */
@Immutable
internal class Art(val width: Float, val height: Float, val items: List<ArtItem>)

internal sealed interface ArtItem

/** Bir forma; [m] SVG qrup transformlarının birləşmiş afin matrisi (a, b, c, d, e, f). */
@Immutable
internal class Shape(val role: ArtRole, val d: String, val m: FloatArray? = null, val w: Float? = null) : ArtItem

/** İllüstrasiyanın içindəki qısa yazı («daban», «burun»). Məzmun kimi azərbaycancadır. */
@Immutable
internal class Label(
    val text: String,
    val x: Float,
    val y: Float,
    val role: LabelRole,
    val anchor: LabelAnchor,
) : ArtItem

internal fun m(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) = floatArrayOf(a, b, c, d, e, f)

internal enum class ArtRole {
    SKIN, CLOTH, HAIR, DETAIL, CLOTH_DETAIL, SOFT, STITCH,
    WATER, WATER_LINE, WATER_BAND, WATER_HIGHLIGHT,
    EARTH, EARTH_DETAIL, DUST,
    METAL, METAL_HANDLE, METAL_DETAIL, LEATHER, LEATHER_DETAIL,
    SILHOUETTE, SHADOW_LINE, MAT, HIGHLIGHT, MOTION, AIR, RING,
    BAD, OK, KAABA, KISWA, YOU, NUMBER, NUMBER_ALT,
}

internal enum class LabelRole { ACCENT, ON_ACCENT, BAD, MUTED }
internal enum class LabelAnchor { START, MIDDLE, END }

/** İllüstrasiyanın palitrası. İşıqlı/tünd dəyərlər maketdəki `ill.css` tokenləri ilə eynidir. */
@Immutable
private class ArtPalette(
    val bg: Color,
    val paper: Color,
    val ink: Color,
    val skin: Color,
    val hair: Color,
    val water: Color,
    val waterTint: Color,
    val earth: Color,
    val earthTint: Color,
    val metalTint: Color,
    val leatherTint: Color,
    val accent: Color,
    val bad: Color,
    val gold: Color,
    val label: Color,
)

private val LightPalette = ArtPalette(
    bg = Color(0xFFF7F4EF), paper = Color.White, ink = Color(0xFF2B3430), skin = Color(0xFFFBF1E6),
    hair = Color(0xFFE7E1DB), water = Color(0xFF2F9BB8), waterTint = Color(0xFFD8EEF4), earth = Color(0xFFA9855A),
    earthTint = Color(0xFFEFE3D0), metalTint = Color(0xFFF3E3CF), leatherTint = Color(0xFFE9D7C6),
    accent = Color(0xFF008B5B), bad = Color(0xFF9C3E00), gold = Color(0xFFC9A227), label = Color(0xFF5D6068),
)

private val DarkPalette = ArtPalette(
    bg = Color(0xFF1E2220), paper = Color(0xFF272C29), ink = Color(0xFFD8E2DC), skin = Color(0xFF302B27),
    hair = Color(0xFF3B3835), water = Color(0xFF4CC3DC), waterTint = Color(0xFF1F3A42), earth = Color(0xFFC4A07A),
    earthTint = Color(0xFF3A3127), metalTint = Color(0xFF3A3026), leatherTint = Color(0xFF3A2E25),
    accent = Color(0xFF2FD08F), bad = Color(0xFFE08A4C), gold = Color(0xFFD9B440), label = Color(0xFFA3A7AD),
)

/** Bir formanın boyağı: dolğu (varsa) və xətt (varsa), qalınlıq viewBox vahidindədir. */
private class Paint(
    val fill: Color? = null,
    val stroke: Color? = null,
    val width: Float = 1f,
    val dash: FloatArray? = null,
)

private fun ArtPalette.paintFor(role: ArtRole, w: Float?): Paint = when (role) {
    ArtRole.SKIN -> Paint(skin, ink, 1.9f)
    ArtRole.CLOTH -> Paint(paper, ink, 1.9f)
    ArtRole.HAIR -> Paint(hair, ink, 1.5f)
    ArtRole.DETAIL -> Paint(stroke = ink.copy(alpha = 0.7f), width = 1f)
    ArtRole.CLOTH_DETAIL -> Paint(stroke = ink.copy(alpha = 0.45f), width = 0.9f)
    ArtRole.SOFT -> Paint(stroke = ink.copy(alpha = 0.22f), width = 1f)
    ArtRole.STITCH -> Paint(stroke = ink.copy(alpha = 0.5f), width = 1f, dash = floatArrayOf(1.5f, 3.2f))
    ArtRole.WATER -> Paint(waterTint, water, 1.3f)
    ArtRole.WATER_LINE -> Paint(stroke = water, width = w ?: 2f)
    ArtRole.WATER_BAND -> Paint(stroke = waterTint, width = w ?: 6f)
    ArtRole.WATER_HIGHLIGHT -> Paint(stroke = water.copy(alpha = 0.9f), width = w ?: 1.2f)
    ArtRole.EARTH -> Paint(earthTint, ink, 1.4f)
    ArtRole.EARTH_DETAIL -> Paint(stroke = ink.copy(alpha = 0.45f), width = 1f)
    ArtRole.DUST -> Paint(stroke = earth, width = 1.3f)
    ArtRole.METAL -> Paint(metalTint, ink, 1.7f)
    ArtRole.METAL_HANDLE -> Paint(stroke = ink, width = 1.7f)
    ArtRole.METAL_DETAIL -> Paint(stroke = ink.copy(alpha = 0.55f), width = 0.9f)
    ArtRole.LEATHER -> Paint(leatherTint, ink, 1.9f)
    ArtRole.LEATHER_DETAIL -> Paint(stroke = ink.copy(alpha = 0.5f), width = 1f, dash = floatArrayOf(3f, 3f))
    ArtRole.SILHOUETTE -> Paint(paper, ink, 1.7f)
    ArtRole.SHADOW_LINE -> Paint(stroke = ink.copy(alpha = 0.25f), width = 1.2f)
    ArtRole.MAT -> Paint(accent.copy(alpha = 0.12f), ink, 1.2f)
    ArtRole.HIGHLIGHT -> Paint(stroke = accent, width = 1.8f, dash = floatArrayOf(4f, 4f))
    ArtRole.MOTION -> Paint(stroke = accent, width = 2f)
    ArtRole.AIR -> Paint(stroke = water, width = 1.6f, dash = floatArrayOf(2f, 4f))
    ArtRole.RING -> Paint(stroke = ink.copy(alpha = 0.3f), width = 1f, dash = floatArrayOf(3f, 4f))
    ArtRole.BAD -> Paint(bad.copy(alpha = 0.12f), bad, 1.2f)
    ArtRole.OK -> Paint(accent.copy(alpha = 0.12f), accent, 1.2f)
    ArtRole.KAABA -> Paint(fill = ink)
    ArtRole.KISWA -> Paint(stroke = gold, width = 2.4f)
    ArtRole.YOU -> Paint(fill = ink)
    ArtRole.NUMBER -> Paint(fill = accent)
    ArtRole.NUMBER_ALT -> Paint(fill = water)
}

private fun FloatArray.toMatrix(): Matrix = Matrix().apply {
    values[Matrix.ScaleX] = this@toMatrix[0]
    values[Matrix.SkewY] = this@toMatrix[1]
    values[Matrix.SkewX] = this@toMatrix[2]
    values[Matrix.ScaleY] = this@toMatrix[3]
    values[Matrix.TranslateX] = this@toMatrix[4]
    values[Matrix.TranslateY] = this@toMatrix[5]
}

private class ParsedShape(val shape: Shape, val path: Path)

/** Yollar bir dəfə qurulur (sətirdən `Path`-a) — sürüşdürmədə hər kadrda təkrar parse olunmasın. */
private val parsedCache = mutableMapOf<Art, List<ParsedShape>>()

private fun Art.parsed(): List<ParsedShape> = parsedCache.getOrPut(this) {
    items.filterIsInstance<Shape>().map { shape ->
        val path = PathParser().parsePathString(shape.d).toPath()
        shape.m?.let { path.transform(it.toMatrix()) }
        ParsedShape(shape, path)
    }
}

/**
 * Bir illüstrasiya — eni doldurur, nisbəti çertyojdan gəlir.
 *
 * Fon rəngi illüstrasiyanın özünə aiddir (kağız tonu), kart isə yalnız küncləri yuvarlayır.
 */
@Composable
internal fun ArtImage(
    art: Art,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    corner: Int = 16,
) {
    val palette = if (ThemeUtils.observeDarkTheme()) DarkPalette else LightPalette
    val shapes = remember(art) { art.parsed() }
    val labels = remember(art) { art.items.filterIsInstance<Label>() }
    val measurer = rememberTextMeasurer()
    val fallbackLabelColor = colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(art.width / art.height)
            .clip(RoundedCornerShape(corner.dp))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        drawRect(palette.bg)
        val k = size.width / art.width
        scale(k, k, pivot = Offset.Zero) {
            shapes.forEach { (shape, path) ->
                val paint = palette.paintFor(shape.role, shape.w)
                paint.fill?.let { drawPath(path, it) }
                paint.stroke?.let { color ->
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = paint.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = paint.dash?.let { PathEffect.dashPathEffect(it) },
                        ),
                    )
                }
            }
        }
        labels.forEach { label ->
            val color = when (label.role) {
                LabelRole.ACCENT -> palette.accent
                LabelRole.ON_ACCENT -> Color.White
                LabelRole.BAD -> palette.bad
                LabelRole.MUTED -> palette.label.takeIf { it.alpha > 0f } ?: fallbackLabelColor
            }
            val fontPx = (if (label.role == LabelRole.BAD) 13f else 11f) * k
            val layout = measurer.measure(
                text = label.text,
                style = TextStyle(
                    color = color,
                    fontSize = (fontPx / density / fontScale).sp,
                    fontWeight = if (label.role == LabelRole.ON_ACCENT) FontWeight.Bold else FontWeight.SemiBold,
                ),
            )
            val w = layout.size.width.toFloat()
            val x = label.x * k - when (label.anchor) {
                LabelAnchor.START -> 0f
                LabelAnchor.MIDDLE -> w / 2f
                LabelAnchor.END -> w
            }
            val y = label.y * k - layout.firstBaseline
            drawText(layout, topLeft = Offset(x, y))
        }
    }
}

private operator fun ParsedShape.component1() = shape
private operator fun ParsedShape.component2() = path
