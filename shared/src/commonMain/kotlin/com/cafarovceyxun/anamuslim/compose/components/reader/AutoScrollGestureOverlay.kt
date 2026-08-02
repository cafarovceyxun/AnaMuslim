package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.cafarovceyxun.anamuslim.resources.autoScrollTryIt
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.strLabelSkip
import org.jetbrains.compose.resources.StringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.autoScroll
import com.cafarovceyxun.anamuslim.resources.autoScrollGestureHintHorizontal
import com.cafarovceyxun.anamuslim.resources.autoScrollGestureHintLongPress
import com.cafarovceyxun.anamuslim.resources.autoScrollGestureHintTap
import com.cafarovceyxun.anamuslim.resources.autoScrollGestureHintVertical
import com.cafarovceyxun.anamuslim.resources.autoScrollHideInstructions
import com.cafarovceyxun.anamuslim.resources.autoScrollSpeedValue
import com.cafarovceyxun.anamuslim.resources.dr_icon_arrow_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AutoScrollGestureOverlay(
    autoScrollSpeed: MutableState<Float?>,
    isAutoScrollGestureMode: MutableState<Boolean>,
    autoScrollStep: MutableIntState,
    onManualScroll: ((Float) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var hasStarted by remember { mutableStateOf(false) }
    var showHud by remember { mutableStateOf(false) }
    var hudVersion by remember { mutableIntStateOf(0) }
    var hideInstructionsChecked by remember { mutableStateOf(false) }
    var showCheckbox by remember { mutableStateOf(false) }

    var isTemporarilyPaused by remember { mutableStateOf(false) }
    var speedBeforeTempPause by remember { mutableStateOf<Float?>(null) }

    // Reset speed to 1x on initialization
    LaunchedEffect(Unit) {
        autoScrollSpeed.value = null

        val hide = ReaderPreferences.getAutoScrollHideInstructions()
        val count = ReaderPreferences.getAutoScrollInstructionsShownCount()
        
        showCheckbox = count >= 3

        if (hide) {
            hasStarted = true
            autoScrollSpeed.value = AutoScroll.speedOfStep(autoScrollStep.intValue)
            hudVersion++
        } else {
            ReaderPreferences.incrementAutoScrollInstructionsShownCount()
        }
    }

    LaunchedEffect(hudVersion) {
        if (hudVersion > 0) {
            showHud = true
            delay(1500)
            showHud = false
        }
    }

    if (!isAutoScrollGestureMode.value) return

    BackHandler {
        isAutoScrollGestureMode.value = false
        autoScrollSpeed.value = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (!hasStarted) Color.Black.copy(alpha = 0.6f) else Color.Transparent)
            .pointerInput(hasStarted) {
                if (!hasStarted) return@pointerInput

                awaitEachGesture {
                    awaitFirstDown()
                    var dragY = 0f
                    var dragX = 0f
                    var isDrag = false
                    var isLongPressHandled = false
                    val startTime = currentEpochMillis()

                    val isInitiallyPlaying = autoScrollSpeed.value != null

                    // Monitor pointer movements
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.isEmpty()) break

                        val change = event.changes[0]

                        if (change.pressed) {
                            val deltaY = change.position.y - change.previousPosition.y
                            val deltaX = change.position.x - change.previousPosition.x
                            
                            dragY += deltaY
                            dragX += deltaX

                            // Long press detection (only if not already dragging much)
                            // Reduced delay to 200ms for faster response
                            if (!isDrag && !isLongPressHandled && isInitiallyPlaying && currentEpochMillis() - startTime > 200) {
                                isLongPressHandled = true
                                speedBeforeTempPause = autoScrollSpeed.value
                                autoScrollSpeed.value = null
                                isTemporarilyPaused = true
                                hudVersion++
                            }

                            if (abs(dragY) > 10f || abs(dragX) > 10f) {
                                isDrag = true
                            }

                            if (isDrag) {
                                // Horizontal exit detection - Reduced sensitivity
                                // Now requires at least 30% of screen width to exit
                                val screenWidth = size.width
                                val exitThreshold = screenWidth / 3f

                                if (abs(dragX) > abs(dragY) * 1.5f && abs(dragX) > exitThreshold) {
                                    isAutoScrollGestureMode.value = false
                                    autoScrollSpeed.value = null
                                    isTemporarilyPaused = false
                                    return@awaitEachGesture
                                }

                                if (isTemporarilyPaused || isInitiallyPlaying) {
                                    // Speed change logic (Vertical drag)
                                    // Use absolute dragY for steps
                                    if (abs(dragY) > 60f) {
                                        val next = if (dragY > 0) { // Swipe down -> Speed DOWN
                                            autoScrollStep.intValue - 1
                                        } else { // Swipe up -> Speed UP
                                            autoScrollStep.intValue + 1
                                        }

                                        if (next in AutoScroll.MIN_STEP..AutoScroll.MAX_STEP) {
                                            autoScrollStep.intValue = next
                                            val speed = AutoScroll.speedOfStep(next)
                                            if (isTemporarilyPaused) speedBeforeTempPause = speed
                                            else autoScrollSpeed.value = speed
                                            hudVersion++
                                            scope.launch { ReaderPreferences.setAutoScrollStep(next) }
                                        }
                                        dragY = 0f
                                    }
                                } else {
                                    // Manual scroll (Only if was paused and not doing long press)
                                    onManualScroll?.invoke(-deltaY)
                                }
                            }
                        } else {
                            // Up event
                            if (isTemporarilyPaused) {
                                // Release from temporary pause (Long press or Drag-Speed-Change)
                                autoScrollSpeed.value = speedBeforeTempPause
                                isTemporarilyPaused = false
                                hudVersion++
                            } else if (!isDrag) {
                                // Short tap logic (only if it wasn't a long press)
                                if (autoScrollSpeed.value == null) {
                                    autoScrollSpeed.value = AutoScroll.speedOfStep(autoScrollStep.intValue)
                                } else {
                                    autoScrollSpeed.value = null
                                }
                                hudVersion++
                            }
                            break
                        }
                    }
                }
            }
    ) {
        if (!hasStarted) {
            AutoScrollCoach(
                showHideOption = showCheckbox,
                hideChecked = hideInstructionsChecked,
                onHideCheckedChange = { hideInstructionsChecked = it },
                onDone = {
                    hasStarted = true
                    autoScrollSpeed.value = AutoScroll.speedOfStep(autoScrollStep.intValue)
                    hudVersion++

                    if (hideInstructionsChecked) {
                        scope.launch {
                            ReaderPreferences.setAutoScrollHideInstructions(true)
                        }
                    }
                },
            )
        }

        // Professional HUD at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            AnimatedVisibility(
                visible = showHud && hasStarted,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            MaterialTheme.shapes.extraLarge
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (autoScrollSpeed.value == null) Res.drawable.ic_pause else Res.drawable.ic_play
                        ),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    if (autoScrollSpeed.value != null) {
                        Text(
                            text = stringResource(
                                Res.string.autoScrollSpeedValue,
                                AutoScroll.levelLabel(AutoScroll.levelOfStep(autoScrollStep.intValue)),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.strLabelPause),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Jestləri siyahı kimi oxutmaq əvəzinə **ekranın üstündə bir-bir təcrübə etdirən** təlim.
 *
 * Hər addımda hərəkət canlandırılır və istifadəçi həmin jesti özü icra edənə qədər gözlənilir —
 * yalnız ondan sonra növbətiyə keçilir. Sonuncu addım (yana sürüşdürüb çıxmaq) burada təhlükəsizdir:
 * təlim zamanı tətbiqdən çıxarmır, sadəcə jesti təsdiqləyir.
 */
private enum class CoachStep(val hint: StringResource) {
    Tap(Res.string.autoScrollGestureHintTap),
    LongPress(Res.string.autoScrollGestureHintLongPress),
    Speed(Res.string.autoScrollGestureHintVertical),
    Exit(Res.string.autoScrollGestureHintHorizontal),
}

@Composable
private fun AutoScrollCoach(
    showHideOption: Boolean,
    hideChecked: Boolean,
    onHideCheckedChange: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val steps = CoachStep.entries
    var index by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }
    val step = steps[index]

    // Jest tanınandan sonra qısa təsdiq göstərilir, sonra növbəti addıma keçilir.
    LaunchedEffect(completed) {
        if (!completed) return@LaunchedEffect
        delay(500)
        completed = false
        if (index == steps.lastIndex) onDone() else index++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(step, completed) {
                if (completed) return@pointerInput

                when (step) {
                    CoachStep.Tap -> detectTapGestures(onTap = { completed = true })

                    CoachStep.LongPress -> detectTapGestures(onLongPress = { completed = true })

                    CoachStep.Speed -> {
                        var travelled = 0f
                        detectVerticalDragGestures(
                            onDragEnd = { travelled = 0f },
                            onDragCancel = { travelled = 0f },
                        ) { _, amount ->
                            travelled += amount
                            if (abs(travelled) > SPEED_DRAG_THRESHOLD) completed = true
                        }
                    }

                    CoachStep.Exit -> {
                        var travelled = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = { travelled = 0f },
                            onDragCancel = { travelled = 0f },
                        ) { _, amount ->
                            travelled += amount
                            if (abs(travelled) > EXIT_DRAG_THRESHOLD) completed = true
                        }
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.autoScroll),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(28.dp))

            CoachStage(step = step, completed = completed)

            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(step.hint),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (completed) "" else stringResource(Res.string.autoScrollTryIt),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            CoachProgress(current = index, total = steps.size)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showHideOption) {
                Row(
                    modifier = Modifier.clickable { onHideCheckedChange(!hideChecked) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = hideChecked,
                        onCheckedChange = onHideCheckedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = Color.White.copy(alpha = 0.6f),
                        ),
                    )
                    Text(
                        text = stringResource(Res.string.autoScrollHideInstructions),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            TextButton(onClick = onDone) {
                Text(
                    text = stringResource(Res.string.strLabelSkip),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** Barmaq izini canlandıran səhnə — hər addım öz hərəkətini göstərir. */
@Composable
private fun CoachStage(step: CoachStep, completed: Boolean) {
    val transition = rememberInfiniteTransition()
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = Modifier.size(COACH_STAGE_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        if (completed) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            return@Box
        }

        when (step) {
            CoachStep.Tap -> {
                CoachRipple(scale = progress)
                CoachFingerprint()
            }

            CoachStep.LongPress -> {
                // Basılı saxlama: halqa yavaş-yavaş dolur, sonra sıfırlanır.
                Box(
                    modifier = Modifier
                        .size(40.dp + 44.dp * progress)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f + 0.10f * progress)),
                )
                CoachFingerprint()
            }

            CoachStep.Speed -> {
                CoachArrow(Res.drawable.dr_icon_chevron_down, rotation = 180f, offsetY = (-58).dp)
                CoachArrow(Res.drawable.dr_icon_chevron_down, rotation = 0f, offsetY = 58.dp)
                CoachFingerprint(
                    modifier = Modifier.offset(y = (-34).dp + 68.dp * progress),
                )
            }

            CoachStep.Exit -> {
                CoachArrow(Res.drawable.dr_icon_arrow_left, rotation = 0f, offsetX = (-70).dp)
                CoachArrow(Res.drawable.dr_icon_arrow_left, rotation = 180f, offsetX = 70.dp)
                CoachFingerprint(
                    modifier = Modifier.offset(x = (-40).dp + 80.dp * progress),
                )
            }
        }
    }
}

@Composable
private fun CoachFingerprint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f)),
    )
}

@Composable
private fun CoachRipple(scale: Float) {
    Box(
        modifier = Modifier
            .size(40.dp + 56.dp * scale)
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.7f * (1f - scale)), CircleShape),
    )
}

@Composable
private fun CoachArrow(
    icon: DrawableResource,
    rotation: Float,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .rotate(rotation)
            .size(22.dp),
    )
}

@Composable
private fun CoachProgress(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i == current) 20.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= current) Color.White.copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.3f)
                    ),
            )
        }
    }
}

private val COACH_STAGE_SIZE = 180.dp
private const val SPEED_DRAG_THRESHOLD = 60f
private const val EXIT_DRAG_THRESHOLD = 90f
