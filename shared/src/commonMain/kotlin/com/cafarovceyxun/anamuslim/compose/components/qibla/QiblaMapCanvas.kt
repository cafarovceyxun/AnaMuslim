package com.cafarovceyxun.anamuslim.compose.components.qibla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.qibla.QiblaMath
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Sancağın ilkin nöqtədən uzaqlaşa biləcəyi hədd. Trafik xərcini və süiistifadəni məhdudlaşdırır. */
private const val PAN_RADIUS_METERS = 2_000.0

/** Ən kiçik zoom — bundan aşağı düşmək qiblə xəttini yerli səviyyədə mənasız edir. */
private const val MIN_ZOOM = 12.0

/**
 * Qiblə xəritəsi — **Compose Canvas**, kənar SDK yox.
 *
 * ### Niyə interop yox
 * `UIKitView`/`AndroidView` ilə yerləşdirilən native xəritə altındakı Compose jestlərini udur
 * (CLAUDE.md-dəki sənədləşmiş tələ) və hər platforma üçün ayrıca SDK + asılılıq versiyası gətirir.
 * Burada taylar birbaşa `Canvas`-a çəkilir: jestlər adi Compose jestidir, əlavə asılılıq sıfırdır.
 *
 * ### Qarşılıqlı təsir
 * Sancaq **həmişə ekranın mərkəzindədir**, xəritə isə altından sürüşür. Beləcə barmaq hədəfi
 * örtmür və nöqtəni dəqiq qoymaq mümkün olur. Zoom da mərkəzə bağlıdır.
 *
 * ### Qiblə xətti düz çəkilir
 * Mercator konformaldır (bucaqları lokal qoruyur), ona görə mərkəzdən çəkilən düz şüa həmin nöqtədə
 * həqiqi bucağı düzgün göstərir. Bu, yalnız xəritə dar olduğu üçün etibarlıdır — bax [WebMercator].
 */
@Composable
fun QiblaMapCanvas(
    anchor: GeoPoint,
    initialPin: GeoPoint,
    recenterKey: Int,
    layer: QiblaMapLayer,
    onPointChange: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ⚠️ Lövbər **namaz koordinatıdır**, sancaq deyil. Əvvəl lövbər sancaqdan hesablanırdı və
    // sancaq hər jestdə dəyişdiyi üçün lövbər onunla birlikdə sürüşürdü: [PAN_RADIUS_METERS]
    // həddi hər jestdə sıfırlanırdı, yəni faktiki heç bir hədd yox idi. Cihazda sancaq Ciddədən
    // Məkkəyə — 71 km — sürüşdü. Eyni səbəbdən mərkəz və zoom da hər jestdə sıfırlanırdı.
    // [recenterKey] «yerimi dəqiqləşdir» düyməsi ilə artır — koordinat dəyişməsə belə xəritəni
    // istifadəçinin öz mövqeyinə qaytarır.
    val start = remember(anchor, recenterKey) { initialPin }

    var centerLat by remember(anchor, recenterKey) { mutableStateOf(start.latitude) }
    var centerLng by remember(anchor, recenterKey) { mutableStateOf(start.longitude) }
    var zoom by remember(anchor, recenterKey) { mutableStateOf(minOf(17.0, layer.maxZoom.toDouble())) }

    // Qat dəyişəndə zoom yeni qatın nativ həddindən yuxarı qala bilməz — yoxsa mövcud olmayan
    // tayl istənir və xəritə boş qalır.
    LaunchedEffect(layer) { zoom = zoom.coerceIn(MIN_ZOOM, layer.maxZoom.toDouble()) }

    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val textMeasurer = rememberTextMeasurer()
    val onPointChanged by rememberUpdatedState(onPointChange)

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Hazırda yüklənməkdə olan tayllar. Yalnız kompozisiya dispetçerindən toxunulur (həm bu blok,
    // həm də onun `launch` uşaqları orada başlayır), ona görə adi dəst kifayətdir.
    val inFlight = remember(layer) { mutableSetOf<String>() }

    LaunchedEffect(layer, tiles, inFlight) {
        snapshotFlow { VisibleTiles.of(centerLat, centerLng, zoom, canvasSize, layer) }
            // ⚠️ `collectLatest` DEYİL: o, sürüşdürmə zamanı hər kadrda gedən yükləməni ləğv edərdi
            // və barmaq qalxana qədər **heç bir tayl gəlməzdi** — xəritə boş qalardı.
            .collect { visible ->
                for (key in visible) {
                    if (tiles.containsKey(key.cacheKey) || !inFlight.add(key.cacheKey)) continue

                    launch {
                        QiblaTileStore.tile(layer, key.zoom, key.x, key.y)
                            ?.let { tiles[key.cacheKey] = it }
                        inFlight.remove(key.cacheKey)
                    }
                }

                // Ekrandan çıxan tayllar buraxılır. Bu dəst gəzinti boyunca böyüsəydi hər tayl
                // 256 KB olduğu üçün yüz-iki yüz tayldan sonra onlarla meqabayta çatardı.
                // Geri qayıdanda tayl `QiblaTileStore`-un öz yaddaş keşindən gəlir, şəbəkədən yox.
                val keep = visible.mapTo(HashSet()) { it.cacheKey }
                tiles.keys.retainAll { it in keep || it in inFlight }
            }
    }

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // ⚠️ `clipToBounds` şərtdir: Canvas öz sərhədindən kənara çəkə bilir və qiblə
                // xətti yuxarıdakı tab sırasının («Kompas» yazısının) üstünə daşırdı.
                .clipToBounds()
                // ⚠️ Ölçü çəkiliş blokunda yox, burada oxunur: `DrawScope` içindən state yazmaq
                // çəkiliş → rekompozisiya → çəkiliş dövrəsi yaradır.
                .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(anchor, layer) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        if (zoomChange != 1f) {
                            zoom = (zoom + ln(zoomChange.toDouble()) / LN_2)
                                .coerceIn(MIN_ZOOM, layer.maxZoom.toDouble())
                        }

                        val world = WebMercator.worldSizePixels(zoom)
                        var x = WebMercator.lonToWorldX(centerLng, zoom) - pan.x
                        var y = WebMercator.latToWorldY(centerLat, zoom) - pan.y

                        // Sancağı lövbərin ətrafındakı dairədə saxlayır.
                        val clamped = clampToCircle(
                            x = x,
                            y = y,
                            centreX = WebMercator.lonToWorldX(anchor.longitude, zoom),
                            centreY = WebMercator.latToWorldY(anchor.latitude, zoom),
                            radius = PAN_RADIUS_METERS /
                                WebMercator.metersPerPixel(anchor.latitude, zoom),
                        )
                        x = clamped.first
                        y = clamped.second

                        val newLng = WebMercator.worldXToLon(x.coerceIn(0.0, world), zoom)
                        val newLat = WebMercator.worldYToLat(y.coerceIn(0.0, world), zoom)

                        // Adi toxunuş da jest sayılır (pan = 0). Mövqe dəyişməyibsə geri bildirmə
                        // göndərmirik — yoxsa hər toxunuşda sancaq yenidən yazılırdı.
                        if (newLat != centerLat || newLng != centerLng) {
                            centerLat = newLat
                            centerLng = newLng

                            onPointChanged(
                                GeoPoint(
                                    latitude = newLat,
                                    longitude = newLng,
                                    elevationMeters = anchor.elevationMeters,
                                ),
                            )
                        }
                    }
                },
        ) {
            drawTiles(tiles, centerLat, centerLng, zoom, layer)

            // Kəbənin ekrandakı mövqeyi — yaxın olanda xətt ona qədər çəkilib orada bitir.
            val centreX = WebMercator.lonToWorldX(centerLng, zoom)
            val centreY = WebMercator.latToWorldY(centerLat, zoom)
            val kaabaOffset = Offset(
                x = (WebMercator.lonToWorldX(QiblaMath.KAABA.longitude, zoom) - centreX).toFloat(),
                y = (WebMercator.latToWorldY(QiblaMath.KAABA.latitude, zoom) - centreY).toFloat(),
            )

            drawQiblaOverlay(
                bearing = QiblaMath.bearingToKaaba(GeoPoint(centerLat, centerLng)),
                kaabaOffset = kaabaOffset,
                textMeasurer = textMeasurer,
            )
        }
    }
}

private const val LN_2 = 0.6931471805599453

/**
 * [x]/[y] nöqtəsini ([centreX], [centreY]) mərkəzli, [radius] radiuslu dairənin içində saxlayır.
 *
 * Ayrıca funksiyadır ki, test edilə bilsin: səhvin özü sıxmanın **düsturunda** deyil, mərkəzin
 * hardan gəldiyində idi (bax [QiblaMapCanvas] KDoc-u). Test mərkəzin sabit qaldığı halda təkrar
 * hərəkətlərin heç vaxt radiusu aşmadığını yoxlayır.
 */
internal fun clampToCircle(
    x: Double,
    y: Double,
    centreX: Double,
    centreY: Double,
    radius: Double,
): Pair<Double, Double> {
    val dx = x - centreX
    val dy = y - centreY
    val distance = sqrt(dx * dx + dy * dy)

    if (distance <= radius || distance == 0.0) return x to y

    return (centreX + dx / distance * radius) to (centreY + dy / distance * radius)
}

/** Bir taylın ünvanı. */
private data class TileKey(val zoom: Int, val x: Int, val y: Int, val layerId: String) {
    val cacheKey: String get() = "$layerId/$zoom/$x/$y"
}

private object VisibleTiles {

    /** Ekranı örtən tayl siyahısı. Tayl zoom-u kəsr zoom-un tam hissəsidir. */
    fun of(
        centerLat: Double,
        centerLng: Double,
        zoom: Double,
        size: Size,
        layer: QiblaMapLayer,
    ): List<TileKey> {
        if (size.width <= 0f || size.height <= 0f) return emptyList()

        val tileZoom = floor(zoom).toInt().coerceIn(0, layer.maxZoom)
        val tileScale = 2.0.pow(zoom - tileZoom)
        val tileSpan = WebMercator.TILE_SIZE * tileScale
        val tileCount = 1 shl tileZoom

        val centerX = WebMercator.lonToWorldX(centerLng, zoom)
        val centerY = WebMercator.latToWorldY(centerLat, zoom)

        val minX = floor((centerX - size.width / 2.0) / tileSpan).toInt()
        val maxX = floor((centerX + size.width / 2.0) / tileSpan).toInt()
        val minY = floor((centerY - size.height / 2.0) / tileSpan).toInt()
        val maxY = floor((centerY + size.height / 2.0) / tileSpan).toInt()

        val keys = ArrayList<TileKey>()
        for (y in minY..maxY) {
            if (y < 0 || y >= tileCount) continue
            for (x in minX..maxX) {
                if (x < 0 || x >= tileCount) continue
                keys.add(TileKey(zoom = tileZoom, x = x, y = y, layerId = layer.id))
            }
        }

        return keys
    }
}

private fun DrawScope.drawTiles(
    tiles: SnapshotStateMap<String, ImageBitmap>,
    centerLat: Double,
    centerLng: Double,
    zoom: Double,
    layer: QiblaMapLayer,
) {
    val tileZoom = floor(zoom).toInt().coerceIn(0, layer.maxZoom)
    val tileScale = 2.0.pow(zoom - tileZoom)
    val tileSpan = WebMercator.TILE_SIZE * tileScale

    val centerX = WebMercator.lonToWorldX(centerLng, zoom)
    val centerY = WebMercator.latToWorldY(centerLat, zoom)
    val originX = size.width / 2.0 - centerX
    val originY = size.height / 2.0 - centerY

    for (key in VisibleTiles.of(centerLat, centerLng, zoom, size, layer)) {
        val image = tiles[key.cacheKey] ?: continue

        // Ölçü qonşu taylın başlanğıcından çıxarılır: kəsr zoom-da hər taylı ayrıca yuvarlaqlasaq
        // aralarında bir piksellik boşluqlar qalır və xəritə cızıqlı görünür.
        val left = originX + key.x * tileSpan
        val top = originY + key.y * tileSpan
        val leftPx = left.roundToInt()
        val topPx = top.roundToInt()
        val widthPx = (left + tileSpan).roundToInt() - leftPx
        val heightPx = (top + tileSpan).roundToInt() - topPx

        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(leftPx, topPx),
            dstSize = IntSize(widthPx, heightPx),
        )
    }
}

/** Xəritə qatının üstündəki rənglər sabitdir: mövzu rəngləri peyk təsvirində itə bilər. */
private val OverlayInk = Color(0xFF1B1B1F)
private val OverlayPaper = Color(0xFFFFFFFF)
private val QiblaLine = Color(0xFFD32F2F)

private fun DrawScope.drawQiblaOverlay(
    bearing: Double,
    kaabaOffset: Offset,
    textMeasurer: TextMeasurer,
) {
    val centre = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.22f

    // --- Şimal gülü ---
    drawCircle(color = OverlayPaper.copy(alpha = 0.85f), radius = radius, center = centre, style = Stroke(width = 2f))

    for (index in 0 until 8) {
        val angle = index * 45.0
        val length = if (index % 2 == 0) radius else radius * 0.6f
        val start = centre + polar(angle, radius * 0.18f)
        val end = centre + polar(angle, length)

        drawLine(
            color = OverlayPaper.copy(alpha = 0.9f),
            start = start,
            end = end,
            strokeWidth = if (index % 2 == 0) 3f else 1.5f,
        )
    }

    for ((index, label) in listOf("N", "E", "S", "W").withIndex()) {
        val position = centre + polar(index * 90.0, radius * 1.16f)
        val measured = textMeasurer.measure(
            text = label,
            style = TextStyle(color = OverlayPaper, fontSize = 13.sp),
        )

        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                position.x - measured.size.width / 2f,
                position.y - measured.size.height / 2f,
            ),
        )
    }

    // --- Qiblə xətti ---
    //
    // Kəbə ekrana yaxındırsa (Məkkə və ətrafı) xətt **onun üstündə bitir** və nişanla göstərilir —
    // əks halda xətt Kəbənin yanından keçib kənara çıxır və istifadəçi hədəfi görmür.
    // Uzaqda isə proyeksiya nöqtəsi mənasızdır: orada böyük dairə bucağı üzrə şüa çəkilir
    // (səbəb [WebMercator] KDoc-unda — konformallıq lokal bucağı qoruyur, uzaq nöqtəni yox).
    val reach = sqrt(size.width * size.width + size.height * size.height)
    val kaabaDistance = sqrt(
        kaabaOffset.x * kaabaOffset.x + kaabaOffset.y * kaabaOffset.y,
    )
    val kaabaIsNear = kaabaDistance <= reach

    val tip = if (kaabaIsNear) centre + kaabaOffset else centre + polar(bearing, reach)

    // Ağ astar xətti hər fonda (açıq dam, tünd ağac) görünməsini təmin edir.
    drawLine(color = OverlayPaper, start = centre, end = tip, strokeWidth = 7f)
    drawLine(color = QiblaLine, start = centre, end = tip, strokeWidth = 3.5f)

    if (kaabaIsNear) {
        drawCircle(color = OverlayPaper, radius = 15f, center = tip)
        drawKaabaMark(centre = tip, size = 9f, color = QiblaLine)
    }

    // --- Mərkəzdəki sancaq ---
    drawCircle(color = OverlayPaper, radius = 8f, center = centre)
    drawCircle(color = QiblaLine, radius = 5f, center = centre)
    drawCircle(color = OverlayInk.copy(alpha = 0.4f), radius = 8f, center = centre, style = Stroke(width = 1f))
}

/** Bucağı (şimaldan, saat əqrəbi ilə) ekran ofsetinə çevirir. Ekranda Y aşağı artır. */
private fun polar(bearingDeg: Double, distance: Float): Offset {
    val radians = bearingDeg * PI / 180.0

    return Offset(
        x = (sin(radians) * distance).toFloat(),
        y = (-cos(radians) * distance).toFloat(),
    )
}
