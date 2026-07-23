package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strLabelGotIt
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
            autoScrollSpeed.value = autoScrollStep.intValue * 0.4f // Start with stored step
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
                                        if (dragY > 0) { // Swipe down -> Speed DOWN
                                            if (autoScrollStep.intValue > 1) {
                                                autoScrollStep.intValue--
                                                if (isTemporarilyPaused) speedBeforeTempPause = autoScrollStep.intValue * 0.4f
                                                else autoScrollSpeed.value = autoScrollStep.intValue * 0.4f
                                                hudVersion++
                                                scope.launch { ReaderPreferences.setAutoScrollStep(autoScrollStep.intValue) }
                                            }
                                        } else { // Swipe up -> Speed UP
                                            if (autoScrollStep.intValue < 20) {
                                                autoScrollStep.intValue++
                                                if (isTemporarilyPaused) speedBeforeTempPause = autoScrollStep.intValue * 0.4f
                                                else autoScrollSpeed.value = autoScrollStep.intValue * 0.4f
                                                hudVersion++
                                                scope.launch { ReaderPreferences.setAutoScrollStep(autoScrollStep.intValue) }
                                            }
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
                                    autoScrollSpeed.value = autoScrollStep.intValue * 0.4f
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
            // Professional Instruction Box
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        MaterialTheme.shapes.extraLarge
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.autoScroll),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.alpha(0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InstructionItemRow(
                            icon = Res.drawable.dr_icon_chevron_down,
                            text = stringResource(Res.string.autoScrollGestureHintVertical),
                            isVertical = true
                        )
                        InstructionItemRow(
                            icon = Res.drawable.ic_pause,
                            text = stringResource(Res.string.autoScrollGestureHintLongPress),
                            isLongPress = true
                        )
                        InstructionItemRow(
                            icon = Res.drawable.dr_icon_chevron_left,
                            text = stringResource(Res.string.autoScrollGestureHintHorizontal),
                            isHorizontal = true
                        )
                        InstructionItemRow(
                            icon = Res.drawable.ic_play,
                            text = stringResource(Res.string.autoScrollGestureHintTap),
                            isTap = true
                        )
                    }

                    if (showCheckbox) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hideInstructionsChecked = !hideInstructionsChecked },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hideInstructionsChecked,
                                onCheckedChange = { hideInstructionsChecked = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface.alpha(0.6f)
                                )
                            )
                            Text(
                                text = stringResource(Res.string.autoScrollHideInstructions),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = {
                            hasStarted = true
                            autoScrollSpeed.value = autoScrollStep.intValue * 0.4f // Start at stored step
                            hudVersion++

                            if (hideInstructionsChecked) {
                                scope.launch {
                                    ReaderPreferences.setAutoScrollHideInstructions(true)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(stringResource(Res.string.strLabelGotIt))
                    }
                }
            }
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
                            text = stringResource(Res.string.autoScrollSpeedValue, autoScrollStep.intValue),
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

@Composable
private fun InstructionItemRow(
    icon: DrawableResource,
    text: String,
    isVertical: Boolean = false,
    isHorizontal: Boolean = false,
    isTap: Boolean = false,
    isLongPress: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.alpha(0.4f),
                    MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isVertical) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .rotate(180f)
                            .size(16.dp)
                    )
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (isHorizontal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_arrow_left),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_arrow_left),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .rotate(180f)
                            .size(18.dp)
                    )
                }
            } else if (isLongPress) {
                // Circular indicator for long press/touch
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
