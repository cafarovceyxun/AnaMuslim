package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.storyImageEditorFill
import com.cafarovceyxun.anamuslim.resources.storyImageEditorFit
import com.cafarovceyxun.anamuslim.resources.storyImageEditorHint
import com.cafarovceyxun.anamuslim.resources.storyImageEditorRotate
import com.cafarovceyxun.anamuslim.resources.storyImageEditorTitle
import com.cafarovceyxun.anamuslim.utils.app.MediaPickResult
import com.cafarovceyxun.anamuslim.utils.app.PickedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Hekayə şəklinin redaktoru: şəkil 9:16 hekayə çərçivəsində yerləşdirilir — iki barmaqla böyütmə/
 * kiçiltmə və fırlatma, bir barmaqla sürüşdürmə, «Sığdır» (bütün şəkil görünür), «Doldur» (çərçivə
 * tam örtülür) və 90° döndərmə. «Hazır» çərçivəni [OUTPUT_WIDTH]×[OUTPUT_HEIGHT] JPEG kimi çəkir;
 * heç nəyə toxunulmayıbsa orijinal şəkil olduğu kimi gedir (əvvəlki davranış — nisbəti saxlanılır).
 *
 * Çərçivə qara fondadır, çünki hekayə pleyeri də şəkli qara fonda göstərir — boş qalan yer orada
 * eyni görünür. Tam ekran səth olduğu üçün `Dialog`-dur (bax CLAUDE.md).
 */
@Composable
fun ImageEditorDialog(
    media: PickedMedia,
    onFinished: (MediaPickResult?) -> Unit,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val scope = rememberCoroutineScope()

    var image by remember(media) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(media) {
        val decoded = withContext(Dispatchers.Default) {
            runCatching { media.bytes.decodeToImageBitmap() }.getOrNull()
        }
        // Format açılmadısa (məs. tanınmayan kodek) redaktor mənasızdır — şəkil olduğu kimi gedir.
        if (decoded == null) currentOnFinished(MediaPickResult.Picked(media)) else image = decoded
    }

    var transform by remember(media) { mutableStateOf(StoryImageTransform.Initial) }
    var exporting by remember(media) { mutableStateOf(false) }

    fun export() {
        val bitmap = image ?: return
        if (exporting) return
        if (transform == StoryImageTransform.Initial) {
            currentOnFinished(MediaPickResult.Picked(media))
            return
        }
        exporting = true
        val snapshot = transform
        scope.launch {
            val bytes = withContext(Dispatchers.Default) { renderStoryImage(bitmap, snapshot) }
            currentOnFinished(
                bytes?.let { MediaPickResult.Picked(PickedMedia(it, "image/jpeg", isVideo = false)) }
                    ?: MediaPickResult.Failed,
            )
        }
    }

    Dialog(
        onDismissRequest = { if (!exporting) currentOnFinished(null) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !exporting,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorTopBar(
                    enabled = !exporting && image != null,
                    onCancel = { currentOnFinished(null) },
                    onDone = ::export,
                    title = stringResource(Res.string.storyImageEditorTitle),
                )

                // Şəkil fəzası: kadr ərəbcədə güzgülənmir, jest riyaziyyatı LTR güman edir.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val fitsWidth = FRAME_ASPECT > maxWidth / maxHeight
                        val frameWidth = if (fitsWidth) maxWidth else maxHeight * FRAME_ASPECT
                        val frameHeight = if (fitsWidth) maxWidth / FRAME_ASPECT else maxHeight

                        val bitmap = image
                        if (bitmap == null) {
                            CircularProgressIndicator(Modifier.size(28.dp))
                        } else {
                            StoryImageCanvas(
                                image = bitmap,
                                transform = transform,
                                enabled = !exporting,
                                onTransformChange = { transform = it },
                                modifier = Modifier
                                    .width(frameWidth)
                                    .height(frameHeight)
                                    .border(1.dp, Color.White.copy(alpha = 0.18f)),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PanelBackground)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    val bitmap = image
                    val fit = bitmap?.let { StoryImageTransform.fit(it, transform.snappedRotation()) }
                    val fill = bitmap?.let { StoryImageTransform.fill(it, transform.snappedRotation()) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModePill(
                            label = stringResource(Res.string.storyImageEditorFit),
                            icon = Icons.Rounded.FitScreen,
                            selected = transform == fit,
                            onClick = { if (!exporting && fit != null) transform = fit },
                        )
                        Spacer(Modifier.width(8.dp))
                        ModePill(
                            label = stringResource(Res.string.storyImageEditorFill),
                            icon = Icons.Rounded.Fullscreen,
                            selected = transform == fill,
                            onClick = { if (!exporting && fill != null) transform = fill },
                        )
                        Spacer(Modifier.width(8.dp))
                        ModePill(
                            label = stringResource(Res.string.storyImageEditorRotate),
                            icon = Icons.Rounded.RotateRight,
                            selected = false,
                            onClick = {
                                if (!exporting && bitmap != null) {
                                    // Döndərmə «doldur» vəziyyətini saxlayır, qalan hallarda sığdırır.
                                    val next = transform.snappedRotation() + 90f
                                    transform = if (transform == fill) {
                                        StoryImageTransform.fill(bitmap, next)
                                    } else {
                                        StoryImageTransform.fit(bitmap, next)
                                    }
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(Res.string.storyImageEditorHint),
                        color = SecondaryText,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (exporting) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Hekayə çərçivəsi və şəkil — iki barmaq jesti (pinch/fırlatma) və bir barmaqla sürüşdürmə. */
@Composable
private fun StoryImageCanvas(
    image: ImageBitmap,
    transform: StoryImageTransform,
    enabled: Boolean,
    onTransformChange: (StoryImageTransform) -> Unit,
    modifier: Modifier,
) {
    val currentTransform by rememberUpdatedState(transform)
    val currentOnChange by rememberUpdatedState(onTransformChange)

    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(Color.Black)
            .pointerInput(enabled, image) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var working = currentTransform
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val rotation = event.calculateRotation()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = false)
                        if (centroid.isSpecified() && (zoom != 1f || rotation != 0f || pan != Offset.Zero)) {
                            working = working.applyGesture(
                                centroid = centroid - Offset(size.width / 2f, size.height / 2f),
                                pan = pan,
                                zoom = zoom,
                                rotation = rotation,
                                frameWidth = size.width.toFloat(),
                            )
                            currentOnChange(working)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    // Barmaq qalxanda düz bucağa yaxın fırlanma ona yapışır — əyri qalmasın.
                    val snapped = working.snapRotation()
                    if (snapped != working) currentOnChange(snapped)
                }
            },
    ) {
        drawStoryImage(image, transform)
    }
}

private fun Offset.isSpecified(): Boolean = x.isFinite() && y.isFinite()

/**
 * Şəklin çərçivədəki vəziyyəti. [scale] «0°-də sığdır» ölçüsünə nisbətdir, [rotation] dərəcə ilə
 * (saat əqrəbi istiqamətində), [offsetX]/[offsetY] şəkil mərkəzinin çərçivə mərkəzindən yerdəyişməsi
 * — **hər ikisi çərçivə eninin payı ilə**, ki ekrandakı önizləmə ilə 1080 piksellik çıxış eyni olsun.
 */
internal data class StoryImageTransform(
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    /** Ən yaxın 90°-yə yuvarlaqlaşdırılmış bucaq — sığdır/doldur hesabı üçün. */
    fun snappedRotation(): Float = (rotation / 90f).roundToInt() * 90f

    fun snapRotation(): StoryImageTransform {
        val nearest = snappedRotation()
        return if (abs(rotation - nearest) <= SNAP_DEGREES) copy(rotation = nearest) else this
    }

    /**
     * Mərkəz [centroid] ətrafında [zoom] və [rotation], sonra [pan] — barmaqların altındakı nöqtə
     * barmaqla birlikdə gedir. Piksellərlə ([frameWidth] çərçivə eni).
     */
    fun applyGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float,
        frameWidth: Float,
    ): StoryImageTransform {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val effectiveZoom = newScale / scale
        val center = Offset(offsetX, offsetY) * frameWidth
        val moved = centroid + (center - centroid).rotated(rotation) * effectiveZoom + pan
        return StoryImageTransform(
            scale = newScale,
            rotation = this.rotation + rotation,
            offsetX = moved.x / frameWidth,
            offsetY = moved.y / frameWidth,
        )
    }

    companion object {
        val Initial = StoryImageTransform()

        /** Bütün şəkil görünür (fırlanmış ölçüyə görə). */
        fun fit(image: ImageBitmap, rotation: Float): StoryImageTransform =
            fit(image.width, image.height, rotation)

        /** Çərçivə tam örtülür — kənarlar kəsilir. */
        fun fill(image: ImageBitmap, rotation: Float): StoryImageTransform =
            fill(image.width, image.height, rotation)

        fun fit(imageWidth: Int, imageHeight: Int, rotation: Float): StoryImageTransform =
            scaledFor(imageWidth, imageHeight, rotation) { horizontal, vertical -> min(horizontal, vertical) }

        fun fill(imageWidth: Int, imageHeight: Int, rotation: Float): StoryImageTransform =
            scaledFor(imageWidth, imageHeight, rotation) { horizontal, vertical -> max(horizontal, vertical) }

        private fun scaledFor(
            imageWidth: Int,
            imageHeight: Int,
            rotation: Float,
            pick: (Float, Float) -> Float,
        ): StoryImageTransform {
            val quarterTurns = ((rotation / 90f).roundToInt() % 4 + 4) % 4
            val swapped = quarterTurns % 2 == 1
            val width = (if (swapped) imageHeight else imageWidth).toFloat()
            val height = (if (swapped) imageWidth else imageHeight).toFloat()
            // Çərçivə vahidlərində (en = FRAME_ASPECT, hündürlük = 1) — nisbət kifayətdir.
            val base = baseScale(imageWidth.toFloat(), imageHeight.toFloat(), FRAME_ASPECT, 1f)
            val target = pick(FRAME_ASPECT / width, 1f / height)
            return StoryImageTransform(scale = target / base, rotation = rotation)
        }
    }
}

private fun Offset.rotated(degrees: Float): Offset {
    if (degrees == 0f) return this
    val radians = degrees * PI.toFloat() / 180f
    val c = cos(radians)
    val s = sin(radians)
    return Offset(x * c - y * s, x * s + y * c)
}

/** «0°-də sığdır» ölçüsü — [StoryImageTransform.scale] buna nisbətdir. */
private fun baseScale(imageWidth: Float, imageHeight: Float, frameWidth: Float, frameHeight: Float) =
    min(frameWidth / imageWidth, frameHeight / imageHeight)

/** Önizləmə və çıxış **eyni** funksiya ilə çəkilir — gördüyün nə isə, yüklənən odur. */
internal fun DrawScope.drawStoryImage(image: ImageBitmap, transform: StoryImageTransform) {
    val scale = baseScale(image.width.toFloat(), image.height.toFloat(), size.width, size.height) *
        transform.scale
    withTransform({
        translate(
            left = size.width / 2f + transform.offsetX * size.width,
            top = size.height / 2f + transform.offsetY * size.width,
        )
        rotate(transform.rotation, pivot = Offset.Zero)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawImage(
            image = image,
            dstOffset = IntOffset(-image.width / 2, -image.height / 2),
            dstSize = IntSize(image.width, image.height),
            filterQuality = FilterQuality.High,
        )
    }
}

/** Çərçivəni qara fonda [OUTPUT_WIDTH]×[OUTPUT_HEIGHT] JPEG kimi çəkir. */
internal fun renderStoryImage(image: ImageBitmap, transform: StoryImageTransform): ByteArray? {
    val output = ImageBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(output),
        size = Size(OUTPUT_WIDTH.toFloat(), OUTPUT_HEIGHT.toFloat()),
    ) {
        drawRect(Color.Black)
        drawStoryImage(image, transform)
    }
    return output.encodeJpeg(OUTPUT_QUALITY)
}

/** Hekayə formatı — pleyer tam ekranda «sığdır» ilə göstərir. */
private const val FRAME_ASPECT = 9f / 16f
private const val OUTPUT_WIDTH = 1080
private const val OUTPUT_HEIGHT = 1920
private const val OUTPUT_QUALITY = 90
private const val MIN_SCALE = 0.2f
private const val MAX_SCALE = 10f
private const val SNAP_DEGREES = 6f
