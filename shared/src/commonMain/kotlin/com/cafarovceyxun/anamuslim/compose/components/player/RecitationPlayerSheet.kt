package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.ui.backhandler.BackHandler
import kotlin.math.abs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationServiceState
import com.cafarovceyxun.anamuslim.utils.univ.ErrorEvent
import com.cafarovceyxun.anamuslim.utils.univ.EventBus
import com.cafarovceyxun.anamuslim.utils.univ.MessageEvent
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.viewModels.RecitationPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

val MINI_PLAYER_HEIGHT = 72.dp

/** How far the collapsed player floats above the bottom inset its host gives it. */
val MINI_PLAYER_BOTTOM_GAP = 8.dp
private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

private val PlayerMotionSpring = spring<Dp>(
    dampingRatio = 0.82f,
    stiffness = Spring.StiffnessLow,
)

enum class MiniPlayerVisibility {
    HIDDEN,
    HIDDEN_BY_DEFAULT,
    ALWAYS_SHOWN
}

data class RecitationPlayerVisibilityState(
    val visibility: MiniPlayerVisibility,
    val isVisible: Boolean,
    val onDismiss: (stopPlayback: Boolean) -> Unit
)

/**
 * Playback has been started and the user has not swiped the player away.
 *
 * Global on purpose: the player is hosted once, above the nav graph, but screens call
 * [rememberMiniPlayerVisibilityState] again just to reserve room for it. While this lived in each
 * caller's own `rememberSaveable`, dismissing the player in the host left the reader still padding
 * for a player that was no longer on screen.
 */
private val playerActivatedState = MutableStateFlow(false)

/**
 * The user swiped the player away while a recitation was still loaded — the one case where a
 * "bring it back" affordance is warranted. Distinct from [playerActivatedState] being false, which
 * also covers a player that was never opened in the first place (a single-verse burst).
 */
private val playerDismissedState = MutableStateFlow(false)

@Composable
fun rememberMiniPlayerVisibilityState(
    visibility: MiniPlayerVisibility = MiniPlayerVisibility.ALWAYS_SHOWN
): RecitationPlayerVisibilityState {
    val viewModel = viewModel { RecitationPlayerViewModel() }
    val state by viewModel.state.collectAsState()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRunning = isPlaying || isLoading

    val playerActivated by playerActivatedState.collectAsState()

    // "Recite only this verse" is a one-verse burst the reader fires in place, with its own control
    // on the verse itself; it must not pull the whole player open. An already-open player is left
    // alone — this only withholds the activation, it never dismisses.
    val isSingleVersePlayback = state.isSingleVersePlayback

    LaunchedEffect(isRunning, isSingleVersePlayback) {
        if (isRunning && !isSingleVersePlayback) {
            playerActivatedState.value = true
            playerDismissedState.value = false
        }
    }

    val isVisible = when (visibility) {
        MiniPlayerVisibility.HIDDEN -> false
        else -> {
            // Can be dismissed manually even if ALWAYS_SHOWN if user swipes it away
            (isRunning || state.currentVerse.isValid) && playerActivated
        }
    }

    return remember(visibility, isVisible, isRunning, state.currentVerse.isValid, playerActivated) {
        RecitationPlayerVisibilityState(visibility, isVisible) { stopPlayback ->
            playerActivatedState.value = false
            playerDismissedState.value = true
            if (stopPlayback) {
                viewModel.controller.stop()
            }
        }
    }
}

/** Re-reveal the mini player after it was swiped away, as long as a recitation is still loaded. */
fun requestShowMiniPlayer() {
    playerActivatedState.value = true
    playerDismissedState.value = false
}

/**
 * True when a recitation is loaded (playing, buffering, or paused on a valid verse) but its mini
 * player has been swiped away. Screens can use this to let another control re-reveal the player via
 * [requestShowMiniPlayer] instead of toggling playback.
 */
@Composable
fun rememberIsMiniPlayerHidden(): Boolean {
    val viewModel = viewModel { RecitationPlayerViewModel() }
    val state by viewModel.state.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playerDismissed by playerDismissedState.collectAsState()

    val recitationLoaded = isPlaying || isLoading || state.currentVerse.isValid
    return recitationLoaded && playerDismissed
}

@Composable
fun RecitationPlayerSheet(
    modifier: Modifier = Modifier,
    collapsedBottomInset: Dp = 0.dp,
    barsCollapsedFraction: Float = 0f,
    playerVisibilityState: RecitationPlayerVisibilityState = rememberMiniPlayerVisibilityState(
        MiniPlayerVisibility.ALWAYS_SHOWN
    ),
    isExpanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    isSyncing: Boolean = false,
    onSyncRequest: (() -> Unit)? = null,
    onPlayPause: (() -> Unit)? = null,
    reopenAffordance: Boolean = false,
) {
    val viewModel = viewModel { RecitationPlayerViewModel() }

    var internalExpanded by rememberSaveable { mutableStateOf(false) }
    val expanded = isExpanded ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = {
        if (onExpandedChange != null) {
            onExpandedChange(it)
        } else {
            internalExpanded = it
        }
    }

    val state by viewModel.state.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isVisible = playerVisibilityState.isVisible

    LaunchedEffect(Unit) {
        EventBus.events.collect {
            when (it) {
                is ErrorEvent -> {
                    it.message?.let(PlatformUtils::showLongToast)
                }

                is MessageEvent -> {
                    it.message?.let(PlatformUtils::showLongToast)
                }
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) setExpanded(false)
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(enabled = expanded && isVisible) {
        setExpanded(false)
    }

    val miniPlayerTotalHeight = MINI_PLAYER_HEIGHT

    val targetBottomInset = if (expanded) 0.dp else collapsedBottomInset
    val animatedBottomInset by animateDpAsState(
        targetValue = targetBottomInset,
        animationSpec = PlayerMotionSpring,
        label = "playerBottomInset",
    )

    val hideOnScrollOffset =
        if (expanded) {
            0.dp
        } else {
            (MINI_PLAYER_HEIGHT + collapsedBottomInset) * barsCollapsedFraction.coerceIn(
                0f,
                1f
            )
        }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fullHeight = maxHeight

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = animatedBottomInset.coerceAtLeast(0.dp))
                .offset(y = hideOnScrollOffset),
        ) {
            PlayerContainer(
                expanded = expanded,
                onExpandedChange = setExpanded,
                fullHeight = fullHeight,
                miniPlayerTotalHeight = miniPlayerTotalHeight,
                controller = viewModel.controller,
                state = state,
                isPlaying = isPlaying,
                isLoading = isLoading,
                isSyncing = isSyncing,
                onSyncRequest = onSyncRequest,
                onPlayPause = onPlayPause,
                onDismiss = playerVisibilityState.onDismiss,
                canDismiss = true,
            )
        }

        // When the player has been swiped away but a recitation is still loaded, offer a quiet way
        // back to it. Gated by the host so it only surfaces on the Quran home/index surfaces, and by
        // an actual dismissal so a never-opened player (single-verse burst) does not summon it.
        val recitationLoaded = isPlaying || isLoading || state.currentVerse.isValid
        val playerDismissed by playerDismissedState.collectAsState()
        AnimatedVisibility(
            visible = reopenAffordance && !isVisible && recitationLoaded && playerDismissed,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = collapsedBottomInset.coerceAtLeast(0.dp) + MINI_PLAYER_BOTTOM_GAP)
                .padding(horizontal = 16.dp),
        ) {
            MiniPlayerReopenButton(
                controller = viewModel.controller,
                isPlaying = isPlaying,
                onClick = { requestShowMiniPlayer() },
            )
        }
    }
}

/**
 * Minimalist affordance that brings the dismissed mini player back. A small, flat circle in the
 * surface tone with a primary-tinted play/pause glyph, wrapped in a thin ring that tracks how much
 * of the current recitation is left — deliberately quieter than a filled FAB.
 */
@Composable
private fun MiniPlayerReopenButton(
    controller: RecitationPlayer,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val (positionMs, durationMs) = rememberTimestamp(isPlaying, controller)
    val progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp),
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.primary,
            trackColor = colorScheme.primary.copy(alpha = 0.15f),
            strokeWidth = 2.5.dp,
            strokeCap = StrokeCap.Round,
        )

        Surface(
            onClick = onClick,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            contentColor = colorScheme.primary,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play),
                    contentDescription = stringResource(Res.string.strLabelPlay),
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PlayerContainer(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    fullHeight: Dp,
    miniPlayerTotalHeight: Dp,
    controller: RecitationPlayer,
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    isSyncing: Boolean,
    onSyncRequest: (() -> Unit)?,
    onPlayPause: (() -> Unit)?,
    onDismiss: (stopPlayback: Boolean) -> Unit,
    canDismiss: Boolean,
) {
    val density = LocalDensity.current
    val targetFraction = if (expanded) 1f else 0f

    val fractionAnim = remember { Animatable(targetFraction) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (!isDragging && fractionAnim.targetValue != targetFraction) {
            fractionAnim.animateTo(
                targetValue = targetFraction,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
            )
        }
    }

    val fraction = fractionAnim.value

    val animatedHeight =
        miniPlayerTotalHeight + (fullHeight - miniPlayerTotalHeight) * fraction.coerceAtLeast(0f)

    // Floating pill logic
    val horizontalPadding by animateDpAsState(
        targetValue = if (expanded) 0.dp else 12.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "horizontalPadding"
    )

    val bottomPadding by animateDpAsState(
        targetValue = if (expanded) 0.dp else MINI_PLAYER_BOTTOM_GAP,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "bottomPadding"
    )

    val shape = if (fraction > 0.01f) {
        val clampedCorner = (16f * (1f - fraction.coerceIn(0f, 1f))).coerceAtLeast(0f)
        RoundedCornerShape(topStart = clampedCorner.dp, topEnd = clampedCorner.dp)
    } else {
        CircleShape
    }

    val surfaceColor = if (fraction > 0.99f) {
        Color.Transparent
    } else {
        colorScheme.surfaceContainerHigh.copy(alpha = (0.95f * (1f - fraction) + fraction * 1f).coerceIn(0f, 1f))
    }

    val elevation = if (fraction > 0.99f) 0.dp else 4.dp

    val coroutineScope = rememberCoroutineScope()
    
    val dragX = remember { Animatable(0f) }
    val draggableStateX = rememberDraggableState { delta ->
        if (!expanded) {
            coroutineScope.launch {
                dragX.snapTo(dragX.value + delta)
            }
        }
    }

    val maxDragDistPx = remember(fullHeight, miniPlayerTotalHeight, density) {
        with(density) { (fullHeight - miniPlayerTotalHeight).coerceAtLeast(1.dp).toPx() }
    }

    val draggableState = rememberDraggableState { delta ->
        val fractionChange = -(delta / maxDragDistPx)
        coroutineScope.launch {
            fractionAnim.snapTo(
                (fractionAnim.value + fractionChange).coerceIn(
                    if (canDismiss) -0.5f else 0f,
                    1f
                )
            )
        }
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = horizontalPadding.coerceAtLeast(0.dp))
            .padding(bottom = bottomPadding.coerceAtLeast(0.dp))
            .fillMaxWidth()
            .height(animatedHeight.coerceAtLeast(0.dp))
            .offset(
                x = with(density) { dragX.value.toDp() },
                y = if (canDismiss && fraction < 0f) with(density) { (-fraction * maxDragDistPx).toDp() } else 0.dp
            )
            .graphicsLayer {
                if (!expanded) {
                    alpha = (1f - (abs(dragX.value) / (maxDragDistPx * 0.8f))).coerceIn(0f, 1f)
                }
            }
            .draggable(
                state = draggableStateX,
                orientation = Orientation.Horizontal,
                enabled = !expanded,
                onDragStopped = { velocity ->
                    if (abs(dragX.value) > maxDragDistPx * 0.4f || abs(velocity) > 800f) {
                        // Swipe to dismiss
                        val target = if (dragX.value > 0) maxDragDistPx * 1.5f else -maxDragDistPx * 1.5f
                        coroutineScope.launch {
                            dragX.animateTo(target)
                            onDismiss(true) // stopPlayback = true
                            dragX.snapTo(0f)
                        }
                    } else {
                        coroutineScope.launch {
                            dragX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
                        }
                    }
                }
            )
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStarted = { isDragging = true },
                onDragStopped = { velocity ->
                    isDragging = false
                    val isSwipingDown = velocity > 500f
                    val isSwipingUp = velocity < -500f

                    if (canDismiss && (fractionAnim.value < -0.1f || (fractionAnim.value < 0f && isSwipingDown))) {
                        onDismiss(false) // stopPlayback = false for swipe down (just hide)

                        coroutineScope.launch {
                            fractionAnim.animateTo(
                                0f,
                                spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
                            )
                        }
                    } else {
                        val target = when {
                            isSwipingDown -> 0f
                            isSwipingUp -> 1f
                            fractionAnim.value > 0.5f -> 1f
                            else -> 0f
                        }

                        val newExpanded = target == 1f

                        onExpandedChange(newExpanded)

                        if (expanded == newExpanded) {
                            coroutineScope.launch {
                                fractionAnim.animateTo(
                                    targetValue = target,
                                    initialVelocity = -(velocity / maxDragDistPx),
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
                }
            ),
        shape = shape,
        color = surfaceColor,
        tonalElevation = elevation,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            val expandedAlpha = ((fraction - 0.2f) * 1.25f).coerceIn(0f, 1f)

            if (expandedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = expandedAlpha },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(Alignment.Top, unbounded = true)
                            .height(fullHeight)
                    ) {
                        ExpandedPlayer(
                            state = state,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            controller = controller,
                            onPlayPause = onPlayPause,
                            onCollapse = { onExpandedChange(false) },
                        )
                    }
                }
            }

            val miniAlpha = ((0.8f - fraction) * 1.25f).coerceIn(0f, 1f)
            if (miniAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = miniAlpha },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(miniPlayerTotalHeight)
                    ) {
                        MiniPlayer(
                            state = state,
                            controller = controller,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            isSyncing = isSyncing,
                            onSyncRequest = onSyncRequest,
                            onPlayPause = onPlayPause,
                            onExpand = { onExpandedChange(true) },
                        )
                    }
                }
            }
        }
    }
}
