package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.ic_volume_off
import com.cafarovceyxun.anamuslim.resources.ic_volume_up
import com.cafarovceyxun.anamuslim.resources.mediaCompressing
import com.cafarovceyxun.anamuslim.resources.reset
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDone
import com.cafarovceyxun.anamuslim.resources.videoEditorCrop
import com.cafarovceyxun.anamuslim.resources.videoEditorCropHint
import com.cafarovceyxun.anamuslim.resources.videoEditorMute
import com.cafarovceyxun.anamuslim.resources.videoEditorOutputSize
import com.cafarovceyxun.anamuslim.resources.videoEditorPause
import com.cafarovceyxun.anamuslim.resources.videoEditorPlay
import com.cafarovceyxun.anamuslim.resources.videoEditorSelection
import com.cafarovceyxun.anamuslim.resources.videoEditorTitle
import com.cafarovceyxun.anamuslim.resources.videoEditorTrim
import com.cafarovceyxun.anamuslim.resources.videoEditorUnmute
import com.cafarovceyxun.anamuslim.utils.app.MediaPickLimits
import com.cafarovceyxun.anamuslim.utils.app.MediaPickResult
import com.cafarovceyxun.anamuslim.utils.app.PickedVideo
import com.cafarovceyxun.anamuslim.utils.app.VideoCrop
import com.cafarovceyxun.anamuslim.utils.app.VideoEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hekayə videosunun redaktoru: **uzunluq** (başlanğıc/son, ən çox
 * [MediaPickLimits.MAX_VIDEO_MILLIS]), **kadr** (kənarları kəsmək — məs. ekran yazısının status və
 * naviqasiya zolaqları) və **səs**. «Hazır» videonu seçimlərlə sıxışdırır və nəticəni [onFinished]-ə
 * verir; ləğvdə `null`.
 *
 * Tam ekran səth olduğu üçün `Dialog`-dur (bax CLAUDE.md «Tam ekran səth `Dialog` olmalıdır»):
 * seçici admin siyahısının kartından çağırılır. Kodlama gedərkən dialoq bağlanmır — kodlama
 * dialoqun scope-undadır və bağlanması onu ləğv edərdi.
 */
@Composable
fun VideoEditorDialog(
    video: PickedVideo,
    onFinished: (MediaPickResult?) -> Unit,
) {
    val maxSpan = MediaPickLimits.MAX_VIDEO_MILLIS
    val duration = video.durationMillis.coerceAtLeast(MIN_SPAN_MILLIS)

    var start by remember(video) { mutableLongStateOf(0L) }
    var end by remember(video) { mutableLongStateOf(minOf(duration, maxSpan)) }
    var crop by remember(video) { mutableStateOf(VideoCrop.Full) }
    var muted by remember(video) { mutableStateOf(false) }
    var mode by remember(video) { mutableStateOf(EditorMode.Trim) }
    var playing by remember(video) { mutableStateOf(false) }
    var position by remember(video) { mutableLongStateOf(0L) }
    var seek by remember(video) { mutableStateOf<SeekRequest?>(null) }
    var exportProgress by remember(video) { mutableStateOf<Float?>(null) }
    val exporting = exportProgress != null

    val scope = rememberCoroutineScope()
    val currentOnFinished by rememberUpdatedState(onFinished)

    val thumbnails by produceState(emptyList<ImageBitmap>(), video) {
        value = withContext(Dispatchers.Default) {
            video.thumbnails(THUMBNAIL_COUNT, THUMBNAIL_HEIGHT_PX)
                .mapNotNull { runCatching { it.decodeToImageBitmap() }.getOrNull() }
        }
    }

    fun export() {
        if (exporting) return
        playing = false
        exportProgress = 0f
        scope.launch {
            val result = video.export(VideoEdit(start, end, crop, muted)) { progress ->
                exportProgress = progress.coerceIn(0f, 1f)
            }
            currentOnFinished(result)
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
                    enabled = !exporting,
                    onCancel = { currentOnFinished(null) },
                    onDone = ::export,
                )

                // Video, kadr çərçivəsi və zaman zolağı **şəkil fəzasıdır**: kadr ərəbcədə
                // güzgülənmir, zaman da soldan sağa gedir — jest riyaziyyatı bunu güman edir.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    PreviewArea(
                        video = video,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                        playing = playing,
                        muted = muted,
                        start = start,
                        end = end,
                        seek = seek,
                        crop = crop,
                        cropInteractive = mode == EditorMode.Crop && !exporting,
                        onTogglePlay = { if (!exporting) playing = !playing },
                        onPosition = { position = it },
                        onCropChange = { crop = it },
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PanelBackground)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModePill(
                            label = stringResource(Res.string.videoEditorTrim),
                            icon = Icons.Rounded.ContentCut,
                            selected = mode == EditorMode.Trim,
                            onClick = { mode = EditorMode.Trim },
                        )
                        Spacer(Modifier.width(8.dp))
                        ModePill(
                            label = stringResource(Res.string.videoEditorCrop),
                            icon = Icons.Rounded.Crop,
                            selected = mode == EditorMode.Crop,
                            onClick = {
                                mode = EditorMode.Crop
                                // Çərçivəni hərəkətli kadrda tənzimləmək çətindir.
                                playing = false
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { muted = !muted }, enabled = !exporting) {
                            Icon(
                                painter = painterResource(
                                    if (muted) Res.drawable.ic_volume_off else Res.drawable.ic_volume_up,
                                ),
                                contentDescription = stringResource(
                                    if (muted) Res.string.videoEditorUnmute else Res.string.videoEditorMute,
                                ),
                                tint = if (muted) MaterialTheme.colorScheme.primary else Color.White,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    when (mode) {
                        EditorMode.Trim -> TrimPanel(
                            duration = duration,
                            maxSpan = maxSpan,
                            start = start,
                            end = end,
                            position = position,
                            playing = playing,
                            thumbnails = thumbnails,
                            enabled = !exporting,
                            onTogglePlay = { playing = !playing },
                            onRangeChange = { newStart, newEnd, seekTo ->
                                playing = false
                                start = newStart
                                end = newEnd
                                seek = SeekRequest(seekTo)
                            },
                            onSeek = { millis ->
                                seek = SeekRequest(millis.coerceIn(start, end))
                            },
                        )

                        EditorMode.Crop -> CropPanel(
                            video = video,
                            crop = crop,
                            enabled = !exporting,
                            onReset = { crop = VideoCrop.Full },
                        )
                    }
                }
            }

            exportProgress?.let { progress -> ExportOverlay(progress) }
        }
    }
}

private enum class EditorMode { Trim, Crop }

@Composable
internal fun EditorTopBar(
    enabled: Boolean,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    title: String = stringResource(Res.string.videoEditorTitle),
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel, enabled = enabled) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelCancel),
                tint = Color.White,
            )
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        TextButton(onClick = onDone, enabled = enabled) {
            Text(
                text = stringResource(Res.string.strLabelDone),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Videonu nisbətini saxlayaraq yerləşdirir; çərçivə və oynat düyməsi eyni qutunun üstündədir. */
@Composable
private fun PreviewArea(
    video: PickedVideo,
    modifier: Modifier,
    playing: Boolean,
    muted: Boolean,
    start: Long,
    end: Long,
    seek: SeekRequest?,
    crop: VideoCrop,
    cropInteractive: Boolean,
    onTogglePlay: () -> Unit,
    onPosition: (Long) -> Unit,
    onCropChange: (VideoCrop) -> Unit,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val ratio = if (video.width > 0 && video.height > 0) {
            video.width.toFloat() / video.height
        } else {
            DEFAULT_ASPECT
        }
        val fitsWidth = ratio > maxWidth / maxHeight
        val frameWidth = if (fitsWidth) maxWidth else maxHeight * ratio
        val frameHeight = if (fitsWidth) maxWidth / ratio else maxHeight

        Box(modifier = Modifier.width(frameWidth).height(frameHeight)) {
            VideoEditorPreview(
                url = video.playbackUrl,
                modifier = Modifier.fillMaxSize(),
                playing = playing,
                muted = muted,
                loopStartMillis = start,
                loopEndMillis = end,
                seek = seek,
                onPosition = onPosition,
            )

            if (!cropInteractive) {
                // Uzunluq rejimində kadra toxunmaq oynadır/dayandırır.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTogglePlay,
                        ),
                )
            }

            CropOverlay(
                crop = crop,
                interactive = cropInteractive,
                onCropChange = onCropChange,
                onTap = onTogglePlay,
                modifier = Modifier.fillMaxSize(),
            )

            // İşarə toxunuş tutmur — altdakı çərçivəyə/kliklənən qutuya keçir.
            if (!playing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ModePill(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = if (selected) primary else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) primary else Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) primary else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TrimPanel(
    duration: Long,
    maxSpan: Long,
    start: Long,
    end: Long,
    position: Long,
    playing: Boolean,
    thumbnails: List<ImageBitmap>,
    enabled: Boolean,
    onTogglePlay: () -> Unit,
    onRangeChange: (start: Long, end: Long, seekTo: Long) -> Unit,
    onSeek: (Long) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onTogglePlay, enabled = enabled) {
            Icon(
                painter = painterResource(if (playing) Res.drawable.ic_pause else Res.drawable.ic_play),
                contentDescription = stringResource(
                    if (playing) Res.string.videoEditorPause else Res.string.videoEditorPlay,
                ),
                tint = Color.White,
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            TrimBar(
                duration = duration,
                maxSpan = maxSpan,
                start = start,
                end = end,
                position = position,
                thumbnails = thumbnails,
                enabled = enabled,
                onRangeChange = onRangeChange,
                onSeek = onSeek,
                modifier = Modifier.weight(1f),
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatClock(start), color = SecondaryText, fontSize = 12.sp)
        Text(
            text = stringResource(
                Res.string.videoEditorSelection,
                formatClock(end - start),
                formatClock(maxSpan),
            ),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(formatClock(end), color = SecondaryText, fontSize = 12.sp)
    }
}

/**
 * Kadr zolağı + seçim çərçivəsi. Sol/sağ tutacaq başlanğıcı/sonu dəyişir, seçimin ortası bütöv
 * aralığı sürüşdürür (3 dəqiqədən uzun videoda lazımdır), toxunuş isə həmin ana keçir.
 */
@Composable
private fun TrimBar(
    duration: Long,
    maxSpan: Long,
    start: Long,
    end: Long,
    position: Long,
    thumbnails: List<ImageBitmap>,
    enabled: Boolean,
    onRangeChange: (start: Long, end: Long, seekTo: Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val handleWidthPx = with(density) { HANDLE_WIDTH.toPx() }
    val touchPx = with(density) { HANDLE_TOUCH.toPx() }

    val currentStart by rememberUpdatedState(start)
    val currentEnd by rememberUpdatedState(end)
    val currentOnRangeChange by rememberUpdatedState(onRangeChange)
    val currentOnSeek by rememberUpdatedState(onSeek)

    BoxWithConstraints(modifier = modifier.height(TRIM_BAR_HEIGHT)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val trackPx = (widthPx - 2 * handleWidthPx).coerceAtLeast(1f)
        fun millisToX(millis: Long) = handleWidthPx + trackPx * millis / duration
        fun xToMillis(x: Float) =
            (((x - handleWidthPx) / trackPx) * duration).roundToInt().toLong().coerceIn(0L, duration)

        // Kadrlar tutacaqların arasındakı izdədir — kənar kadr tutacağın altında qalmasın.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HANDLE_WIDTH, vertical = 3.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            thumbnails.forEach { thumbnail ->
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, duration) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> currentOnSeek(xToMillis(offset.x)) }
                }
                .pointerInput(enabled, duration, maxSpan) {
                    if (!enabled) return@pointerInput
                    var target = TrimTarget.None
                    var workingStart = 0L
                    var workingEnd = 0L
                    var anchorX = 0f
                    var anchorStart = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            workingStart = currentStart
                            workingEnd = currentEnd
                            val startX = millisToX(workingStart) - handleWidthPx / 2
                            val endX = millisToX(workingEnd) + handleWidthPx / 2
                            target = when {
                                abs(offset.x - startX) <= touchPx -> TrimTarget.Start
                                abs(offset.x - endX) <= touchPx -> TrimTarget.End
                                offset.x in startX..endX -> TrimTarget.Move
                                else -> TrimTarget.None
                            }
                            anchorX = offset.x
                            anchorStart = workingStart
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val x = change.position.x
                            when (target) {
                                TrimTarget.Start -> {
                                    workingStart = xToMillis(x + handleWidthPx / 2).coerceIn(
                                        maxOf(0L, workingEnd - maxSpan),
                                        workingEnd - MIN_SPAN_MILLIS,
                                    )
                                    currentOnRangeChange(workingStart, workingEnd, workingStart)
                                }

                                TrimTarget.End -> {
                                    workingEnd = xToMillis(x - handleWidthPx / 2).coerceIn(
                                        workingStart + MIN_SPAN_MILLIS,
                                        minOf(duration, workingStart + maxSpan),
                                    )
                                    currentOnRangeChange(workingStart, workingEnd, workingEnd)
                                }

                                TrimTarget.Move -> {
                                    val span = workingEnd - workingStart
                                    val delta = ((x - anchorX) / trackPx * duration).toLong()
                                    workingStart = (anchorStart + delta).coerceIn(0L, duration - span)
                                    workingEnd = workingStart + span
                                    currentOnRangeChange(workingStart, workingEnd, workingStart)
                                }

                                TrimTarget.None -> Unit
                            }
                        },
                    )
                },
        ) {
            val startX = millisToX(start)
            val endX = millisToX(end)
            val inset = 3.dp.toPx()

            // Seçimdən kənar qaraldılır.
            drawRect(DimColor, Offset(handleWidthPx, inset), Size(startX - handleWidthPx, size.height - 2 * inset))
            drawRect(DimColor, Offset(endX, inset), Size(handleWidthPx + trackPx - endX, size.height - 2 * inset))

            // Çərçivə: üst/alt zolaq + iki tutacaq.
            val border = 3.dp.toPx()
            drawRect(primary, Offset(startX, 0f), Size(endX - startX, border))
            drawRect(primary, Offset(startX, size.height - border), Size(endX - startX, border))
            drawHandle(primary, Rect(startX - handleWidthPx, 0f, startX, size.height))
            drawHandle(primary, Rect(endX, 0f, endX + handleWidthPx, size.height))

            // Oynatma mövqeyi.
            if (position in start..end) {
                val x = millisToX(position)
                drawRect(Color.White, Offset(x - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
            }
        }
    }
}

private enum class TrimTarget { None, Start, End, Move }

private fun DrawScope.drawHandle(color: Color, rect: Rect) {
    drawRoundRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
    )
    // Tutacaq xətti — sürüşdürülə bildiyini göstərir.
    val gripHeight = rect.height * 0.35f
    drawRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(rect.center.x - 1.dp.toPx(), rect.center.y - gripHeight / 2),
        size = Size(2.dp.toPx(), gripHeight),
    )
}

@Composable
private fun CropPanel(
    video: PickedVideo,
    crop: VideoCrop,
    enabled: Boolean,
    onReset: () -> Unit,
) {
    val croppedWidth = ((crop.right - crop.left) * video.width).roundToInt()
    val croppedHeight = ((crop.bottom - crop.top) * video.height).roundToInt()
    val (outWidth, outHeight) = MediaPickLimits.scaledSize(
        croppedWidth,
        croppedHeight,
        MediaPickLimits.TARGET_VIDEO_SHORT_SIDE,
    ) ?: ((croppedWidth / 2 * 2) to (croppedHeight / 2 * 2))

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.videoEditorCropHint), color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(Res.string.videoEditorOutputSize, outWidth, outHeight),
                color = SecondaryText,
                fontSize = 12.sp,
            )
        }
        TextButton(onClick = onReset, enabled = enabled && !crop.isFull) {
            Text(stringResource(Res.string.reset))
        }
    }
    // Uzunluq panelinin hündürlüyünə yaxın — rejim dəyişəndə önizləmə atılmasın.
    Spacer(Modifier.height(30.dp))
}

/**
 * Kadr çərçivəsi: kənardakı hissə qaraldılır. [interactive] olanda kənar/künc tutacaqları
 * sürüşdürülür, içəridən tutmaq bütöv çərçivəni daşıyır. Paylar göstərilən kadra görədir.
 */
@Composable
private fun CropOverlay(
    crop: VideoCrop,
    interactive: Boolean,
    onCropChange: (VideoCrop) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier,
) {
    val currentCrop by rememberUpdatedState(crop)
    val currentOnCropChange by rememberUpdatedState(onCropChange)
    val currentOnTap by rememberUpdatedState(onTap)
    val touchPx = with(LocalDensity.current) { HANDLE_TOUCH.toPx() }

    Canvas(
        modifier = modifier.pointerInput(interactive) {
            if (!interactive) return@pointerInput

            // Toxunuş həddi (slop) olmadan və **toxunduğu nöqtədən ümumi məsafə** ilə: hər addımın
            // deltasını yığanda slop-un yediyi hissə itirdi və çərçivə barmaqdan geri qalırdı.
            // Barmaq toxunuş həddini (slop) keçməyibsə bu, **toxunuşdur** — oynat/dayandır, Uzunluq
            // rejimindəki kimi; keçibsə çərçivə sürüşür. Məsafə yenə toxunduğu nöqtədən hesablanır,
            // ona görə slop çərçivənin yerdəyişməsindən yeyilmir.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startCrop = currentCrop
                val handle = hitTestCrop(
                    down.position,
                    startCrop,
                    size.width.toFloat(),
                    size.height.toFloat(),
                    touchPx,
                )
                val slop = viewConfiguration.touchSlop
                var dragged = false

                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    val total = change.position - down.position
                    if (!dragged && total.getDistance() > slop) dragged = true
                    if (dragged && handle != CropHandle.None) {
                        change.consume()
                        currentOnCropChange(
                            startCrop.dragged(handle, total.x / size.width, total.y / size.height),
                        )
                    }
                    if (!change.pressed) break
                }

                if (!dragged) currentOnTap()
            }
        },
    ) {
        val rect = Rect(
            left = crop.left * size.width,
            top = crop.top * size.height,
            right = crop.right * size.width,
            bottom = crop.bottom * size.height,
        )

        if (!crop.isFull) {
            drawRect(DimColor, Offset.Zero, Size(size.width, rect.top))
            drawRect(DimColor, Offset(0f, rect.bottom), Size(size.width, size.height - rect.bottom))
            drawRect(DimColor, Offset(0f, rect.top), Size(rect.left, rect.height))
            drawRect(DimColor, Offset(rect.right, rect.top), Size(size.width - rect.right, rect.height))
        }

        if (!interactive) return@Canvas

        val stroke = 1.5.dp.toPx()
        drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(stroke))

        // Üçdə bir xətləri — kompozisiya üçün bələdçi.
        val guide = Color.White.copy(alpha = 0.35f)
        for (i in 1..2) {
            val x = rect.left + rect.width * i / 3
            val y = rect.top + rect.height * i / 3
            drawLine(guide, Offset(x, rect.top), Offset(x, rect.bottom), strokeWidth = 1.dp.toPx())
            drawLine(guide, Offset(rect.left, y), Offset(rect.right, y), strokeWidth = 1.dp.toPx())
        }

        // Künc tutacaqları (L şəkilli) və kənarların ortasında qısa zolaqlar.
        val arm = 22.dp.toPx()
        val thick = 4.dp.toPx()
        val corners = listOf(
            rect.topLeft to Offset(1f, 1f),
            rect.topRight to Offset(-1f, 1f),
            rect.bottomLeft to Offset(1f, -1f),
            rect.bottomRight to Offset(-1f, -1f),
        )
        corners.forEach { (corner, dir) ->
            drawLine(Color.White, corner, corner + Offset(arm * dir.x, 0f), strokeWidth = thick)
            drawLine(Color.White, corner, corner + Offset(0f, arm * dir.y), strokeWidth = thick)
        }
        val bar = 18.dp.toPx()
        drawLine(Color.White, Offset(rect.center.x - bar, rect.top), Offset(rect.center.x + bar, rect.top), thick)
        drawLine(Color.White, Offset(rect.center.x - bar, rect.bottom), Offset(rect.center.x + bar, rect.bottom), thick)
        drawLine(Color.White, Offset(rect.left, rect.center.y - bar), Offset(rect.left, rect.center.y + bar), thick)
        drawLine(Color.White, Offset(rect.right, rect.center.y - bar), Offset(rect.right, rect.center.y + bar), thick)
    }
}

internal enum class CropHandle { None, Move, Left, Top, Right, Bottom, TopLeft, TopRight, BottomLeft, BottomRight }

internal fun hitTestCrop(offset: Offset, crop: VideoCrop, width: Float, height: Float, touch: Float): CropHandle {
    val left = crop.left * width
    val top = crop.top * height
    val right = crop.right * width
    val bottom = crop.bottom * height
    val nearLeft = abs(offset.x - left) <= touch
    val nearRight = abs(offset.x - right) <= touch
    val nearTop = abs(offset.y - top) <= touch
    val nearBottom = abs(offset.y - bottom) <= touch
    val withinX = offset.x in (left - touch)..(right + touch)
    val withinY = offset.y in (top - touch)..(bottom + touch)

    return when {
        nearLeft && nearTop -> CropHandle.TopLeft
        nearRight && nearTop -> CropHandle.TopRight
        nearLeft && nearBottom -> CropHandle.BottomLeft
        nearRight && nearBottom -> CropHandle.BottomRight
        nearLeft && withinY -> CropHandle.Left
        nearRight && withinY -> CropHandle.Right
        nearTop && withinX -> CropHandle.Top
        nearBottom && withinX -> CropHandle.Bottom
        offset.x in left..right && offset.y in top..bottom -> CropHandle.Move
        else -> CropHandle.None
    }
}

/** [dx]/[dy] — kadr eninin/hündürlüyünün payı ilə. Ölçü [VideoCrop.MIN_SPAN]-dan kiçilmir. */
internal fun VideoCrop.dragged(handle: CropHandle, dx: Float, dy: Float): VideoCrop {
    val min = VideoCrop.MIN_SPAN
    fun moveLeft(c: VideoCrop) = c.copy(left = (c.left + dx).coerceIn(0f, c.right - min))
    fun moveRight(c: VideoCrop) = c.copy(right = (c.right + dx).coerceIn(c.left + min, 1f))
    fun moveTop(c: VideoCrop) = c.copy(top = (c.top + dy).coerceIn(0f, c.bottom - min))
    fun moveBottom(c: VideoCrop) = c.copy(bottom = (c.bottom + dy).coerceIn(c.top + min, 1f))

    return when (handle) {
        CropHandle.None -> this
        CropHandle.Left -> moveLeft(this)
        CropHandle.Right -> moveRight(this)
        CropHandle.Top -> moveTop(this)
        CropHandle.Bottom -> moveBottom(this)
        CropHandle.TopLeft -> moveTop(moveLeft(this))
        CropHandle.TopRight -> moveTop(moveRight(this))
        CropHandle.BottomLeft -> moveBottom(moveLeft(this))
        CropHandle.BottomRight -> moveBottom(moveRight(this))
        CropHandle.Move -> {
            val width = right - left
            val height = bottom - top
            val newLeft = (left + dx).coerceIn(0f, 1f - width)
            val newTop = (top + dy).coerceIn(0f, 1f - height)
            VideoCrop(newLeft, newTop, newLeft + width, newTop + height)
        }
    }
}

@Composable
private fun ExportOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            // Altdakı düymələrə toxunuş keçməsin.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 5.dp,
                )
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.mediaCompressing), color = Color.White, fontSize = 15.sp)
        }
    }
}

/** `m:ss` — hekayə videosu onsuz da 10 dəqiqədən qısadır. */
internal fun formatClock(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) + 500) / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

internal val EditorBackground = Color(0xFF0B0B0C)
internal val PanelBackground = Color(0xFF151517)
internal val SecondaryText = Color.White.copy(alpha = 0.6f)
private val DimColor = Color.Black.copy(alpha = 0.6f)

private val TRIM_BAR_HEIGHT: Dp = 56.dp
private val HANDLE_WIDTH: Dp = 14.dp
private val HANDLE_TOUCH: Dp = 24.dp

/** Kəsilmiş hissənin ən qısa uzunluğu. */
private const val MIN_SPAN_MILLIS = 1_000L
private const val DEFAULT_ASPECT = 9f / 16f
private const val THUMBNAIL_COUNT = 10
private const val THUMBNAIL_HEIGHT_PX = 160
