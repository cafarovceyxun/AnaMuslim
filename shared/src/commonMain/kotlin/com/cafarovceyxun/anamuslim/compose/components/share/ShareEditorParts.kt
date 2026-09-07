package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.shareImageCustomBackground
import com.cafarovceyxun.anamuslim.utils.AppLogger
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Paylaşma redaktorlarının **ortaq** hissələri.
 *
 * Ayə/hədis redaktoru ([ShareImageEditorScreen]) və namaz vaxtları redaktoru
 * ([com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerShareEditorScreen]) fərqli kətan
 * çəkir, amma önizləmə miqyası, qatın yazılması, paylaşma və panel elementləri eynidir. Onları
 * hərəsində ayrıca saxlasaydıq, biri düzələndə digəri geridə qalardı — kart tutulmasının incə
 * qaydası (aşağıdakı KDoc) məhz belə bir düzəlişin nəticəsidir.
 */

/** Yaxınlaşdırmanın yuxarı həddi. 5× 1080px kətanda piksel səviyyəsinə baxmaq üçün bəsdir. */
private const val PreviewMaxZoom = 5f

/** Bundan yuxarıda «yaxınlaşdırılıb» sayılır: nişan görünür, ikiqat toxunuş sıfırlayır. */
private const val PreviewZoomedThreshold = 1.01f

/**
 * Kartı mövcud sahəyə **tam sığdırıb** göstərir və eyni anda tam ölçülü qatı yazır.
 *
 * Miqyas `graphicsLayer` modifikatoru ilə verilir, ölçü modifikatoru ilə yox: belə olanda kartın
 * daxili koordinat sistemi 1080px qalır, `record` da elə həmin ölçüdə yazır. Kartı kiçik qutuya
 * yerləşdirməklə kiçiltmək isə mətni kəsərdi.
 *
 * [card]-a verilən modifikator **ölçü modifikatorlarından əvvəl** tətbiq olunmalıdır — kart onu öz
 * `requiredSize`-ından qabaq zəncirə qoyur.
 *
 * ### Yaxınlaşdırma niyə kənar qatdadır
 * Barmaqla böyütmə/sürüşdürmə **sığdırma qutusuna** verilir, kartın öz zəncirinə yox. Kartın
 * zəncirindəki `graphicsLayer` yazılan qatın koordinat sistemini təyin edir — oraya istifadəçi
 * miqyasını qatsaq, paylaşılan fayl da yaxınlaşdırılmış (və kəsilmiş) çıxardı. Kənar qat isə yalnız
 * ekrandakı görüntüyə təsir edir: `record` onun **içindəki** tam ölçülü kartı yazmağa davam edir.
 */
@Composable
internal fun SharePreviewCanvas(
    widthPx: Int,
    heightPx: Int,
    graphicsLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
    card: @Composable (Modifier) -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Nisbət dəyişəndə kadr da dəyişir; köhnə sürüşmə yeni kadrda mənasız yerə düşürdü.
    LaunchedEffect(widthPx, heightPx) {
        zoom = 1f
        offset = Offset.Zero
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            // Yaxınlaşdırılmış kart öz sahəsindən kənara çıxmasın — altdakı alət paneli örtülərdi.
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        val scale = min(
            availableWidth / widthPx,
            availableHeight / heightPx,
        ).coerceAtLeast(0.01f)

        val fittedWidth = widthPx * scale
        val fittedHeight = heightPx * scale

        Box(
            modifier = Modifier
                .size(
                    width = with(density) { fittedWidth.toDp() },
                    height = with(density) { fittedHeight.toDp() },
                )
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = offset.x
                    translationY = offset.y
                }
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            card(
                Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            graphicsLayer.record {
                                this@onDrawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                    }
            )
        }

        // Jestlər kartın deyil, **bütün önizləmə sahəsinin** üstündədir: yaxınlaşdırılmış kartın
        // kənarından tutub sürüşdürmək də işləsin.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(fittedWidth, fittedHeight) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val previous = zoom
                        val next = (previous * gestureZoom).coerceIn(1f, PreviewMaxZoom)

                        // Barmaqların altındakı nöqtə yerində qalsın: kart mərkəzə görə
                        // miqyaslandığı üçün mərkəzdən ölçülən vektor yeni miqyasla yenidən
                        // hesablanır. Bunsuz iki barmaqla böyütmə həmişə kartın **mərkəzinə**
                        // yaxınlaşırdı, baxılan yerə yox.
                        val focus = Offset(
                            centroid.x - size.width / 2f,
                            centroid.y - size.height / 2f,
                        )
                        val panned = offset + pan
                        val zoomed = focus + (panned - focus) * (next / previous)

                        // Kart pəncərədən böyük olduğu qədər sürüşə bilir; kiçikdirsə mərkəzdə
                        // qalır (əks halda tam görünən kartı kadrdan çıxarmaq olurdu).
                        val maxX = ((fittedWidth * next - size.width) / 2f).coerceAtLeast(0f)
                        val maxY = ((fittedHeight * next - size.height) / 2f).coerceAtLeast(0f)

                        zoom = next
                        offset = Offset(
                            zoomed.x.coerceIn(-maxX, maxX),
                            zoomed.y.coerceIn(-maxY, maxY),
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoom = 1f
                            offset = Offset.Zero
                        },
                    )
                },
        )

        if (zoom > PreviewZoomedThreshold) {
            ZoomBadge(
                zoom = zoom,
                onReset = {
                    zoom = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * Cari yaxınlaşdırma nişanı — toxunanda sıfırlayır.
 *
 * Sıfırlama üçün ikiqat toxunuş da var, amma o, **kəşf olunmur**: nişan həm vəziyyəti göstərir, həm
 * də görünən çıxış yolu verir. Kart qatının **kənarındadır**, ona görə paylaşılan şəklə düşmür.
 */
@Composable
private fun ZoomBadge(zoom: Float, onReset: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(50))
            .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.92f))
            .clickable(onClick = onReset)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            // Onda birə yuvarlaqlaşdırma: `1.7×` oxunur, `1.7333×` yox.
            text = "${(zoom * 10f).roundToInt() / 10f}×",
            style = typography.labelMedium,
            color = colorScheme.onSurface,
        )
    }
}

/**
 * Şəklin **orta parlaqlığı**, 0 (qara) … 1 (ağ). Avtomatik yazı rəngi bunun üstündə qurulur.
 *
 * Bütün pikselləri oxumur: şəkil 2160px-ə qədər ola bilər (`PICKED_IMAGE_MAX_DIMENSION`) və tam
 * piksel xəritəsi onlarla MB tutardı. Bunun əvəzinə [LuminanceSampleRows] sətir bərabər aralıqla
 * oxunur, hər sətirdən [LuminanceSampleColumns] nöqtə götürülür — ~576 piksel, gözlə fərqi
 * bilinməyən dəqiqliklə.
 *
 * ⚠️ `Color.luminance()` **xətti** dəyər qaytarır, sRGB deyil: gözə «orta boz» görünən rəng burada
 * 0.5 yox, ~0.2-dir. Həddi seçəndə bunu nəzərə al (bax [ShareAutoTextThreshold]).
 */
internal fun ImageBitmap.averageLuminance(): Float {
    if (width <= 0 || height <= 0) return 0f

    val rowCount = min(height, LuminanceSampleRows)
    val columnCount = min(width, LuminanceSampleColumns)
    val buffer = IntArray(width)
    var sum = 0f
    var samples = 0

    for (row in 0 until rowCount) {
        val y = ((row + 0.5f) / rowCount * height).toInt().coerceIn(0, height - 1)
        val pixels = toPixelMap(startX = 0, startY = y, width = width, height = 1, buffer = buffer)
        for (column in 0 until columnCount) {
            val x = ((column + 0.5f) / columnCount * width).toInt().coerceIn(0, width - 1)
            sum += pixels[x, 0].luminance()
            samples++
        }
    }

    return if (samples == 0) 0f else sum / samples
}

private const val LuminanceSampleRows = 24
private const val LuminanceSampleColumns = 24

/** Yazılmış qatı şəklə çevirib paylaşma vərəqinə verir. `false` = paylaşıla bilmədi. */
internal suspend fun shareCapturedCard(
    graphicsLayer: GraphicsLayer,
    chooserTitle: String,
    logTag: String,
): Boolean = try {
    PlatformUtils.shareImage(graphicsLayer.toImageBitmap(), chooserTitle)
} catch (e: Exception) {
    AppLogger.saveError(e, logTag)
    false
}

@Composable
internal fun PanelLabel(text: String) {
    Text(
        text = text,
        style = typography.labelMedium,
        color = colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
internal fun ThemeSwatch(
    theme: ShareImageTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(theme.gradient))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        theme.photo?.let { photo ->
            Image(
                painter = painterResource(photo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(theme.accent),
        )
    }
}

/**
 * Fon dəstinin sonundakı «öz şəklin» yeri: şəkil seçilməyibsə «+», seçiləndən sonra seçilmiş şəklin
 * özü göstərilir. Yenidən toxunmaq başqa şəkil seçdirir; paket fonlarından birinə keçmək seçimi
 * təmizləyir ([ThemeSwatch]-in `onClick`-i).
 */
@Composable
internal fun CustomBackgroundSwatch(
    image: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(Res.string.shareImageCustomBackground),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Aktivlərdə «+» ikonu yoxdur; iki zolaqla çəkmək yeni fayl əlavə etməkdən ucuzdur və
            // istənilən ölçüdə kəskin qalır.
            Box(Modifier.size(width = 16.dp, height = 2.dp).background(colorScheme.onSurfaceVariant))
            Box(Modifier.size(width = 2.dp, height = 16.dp).background(colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
internal fun RowScope.EditorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    /**
     * Sönük xətkeş — məsələn qaraltma, fon şəkli olmayanda. Görünüş bayrağıdır, davranış callback-i
     * deyil: default-u olması «paylaşılan ekrana default-lu callback vermə» qaydasına düşmür.
     */
    enabled: Boolean = true,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = label,
            style = typography.bodySmall,
            color = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            maxLines = 1,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Kartın kənar məsafə xətkeşinin aralığı — hər iki redaktorda eynidir. */
internal val ShareMarginRange = 0.4f..1.7f
internal const val ShareDefaultMargin = 1f

/** Yazı ölçüsü xətkeşi: mətnin kadrı **nə qədər doldurduğu** (1.0 = maksimum sığdırma). */
internal val ShareTextScaleRange = 0.5f..1f
internal const val ShareDefaultTextScale = 0.82f

/** İstifadəçinin öz şəkli üçün qaraltmanın **başlanğıc** dəyəri — paket fotosundan bir az yüngül. */
internal const val ShareCustomBackgroundScrim = 0.62f

/** Qaraltma xətkeşinin aralığı. 1.0 tam qara düzbucaqlıdır, ona görə üst hədd 0.95-də saxlanılır. */
internal val ShareScrimRange = 0f..0.95f

/** Qeyd blokunun miqyas aralığı — 0 deyil, çünki söndürmək üçün ayrıca «Qeyd» çipi var. */
internal val ShareNoteScaleRange = 0.5f..1.6f
internal const val ShareDefaultNoteScale = 1f

/** Loqo sətrinin miqyas aralığı. Yuxarı hədd 1.6: daha böyüyü mətn sahəsini yeməyə başlayır. */
internal val ShareBrandingScaleRange = 0.6f..1.6f
internal const val ShareDefaultBrandingScale = 1f

/** Fon şəklinin üstündəki qaraltma — mətn hər fotoda oxunaqlı qalsın deyə. */
@Composable
internal fun ShareScrim(strength: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = strength),
                        Color.Black.copy(alpha = (strength - 0.12f).coerceAtLeast(0f)),
                        Color.Black.copy(alpha = (strength + 0.1f).coerceAtMost(1f)),
                    )
                )
            ),
    )
}

/**
 * Mağaza QR-i — mərkəzində platformanın loğosu ilə.
 *
 * ⚠️ Loğo QR-in bir hissəsini **örtür**, ona görə kodlar `error="h"` (30% düzəliş) ilə
 * generasiya olunub (`tools`-suz, birdəfəlik: `segno.make(url, error="h")`). Aşağı səviyyədə
 * (M, 15%) mərkəzi örtmək kodu oxunmaz edərdi — və bunu nə kompilyator, nə test tutar, yalnız
 * telefonla skan edəndə bilinər.
 *
 * ⚠️ Bura köçürüldü ki, ayə/hədis kartı da eyni QR-i çəksin — yuxarıdakı düzəliş-səviyyəsi
 * qaydası kopyalanmaya dözmür: ikinci nüsxədə `error="h"` şərti unudulsa kod sadəcə oxunmur.
 *
 * Loğonun altındakı ağ lövhə də qəsdəndir: qara modulların üstündə birbaşa duran qara loğo
 * seçilmir, ağ fon isə skanere «boş sahə» kimi görünür və düzəliş bunu onsuz da bərpa edir.
 */
@Composable
internal fun QrCode(
    image: DrawableResource,
    logo: DrawableResource,
    sizePx: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(sizePx.dp)
            .clip(RoundedCornerShape((sizePx * 0.09f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        Box(
            modifier = Modifier
                .size((sizePx * QrLogoFraction).dp)
                .clip(RoundedCornerShape((sizePx * 0.06f).dp))
                .background(Color.White)
                .padding((sizePx * 0.03f).dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.Black),
            )
        }
    }
}

/** Loğo lövhəsinin QR-ə nisbəti. 0.28-dən yuxarı `error="h"` düzəlişini də aşır. */
private const val QrLogoFraction = 0.26f
