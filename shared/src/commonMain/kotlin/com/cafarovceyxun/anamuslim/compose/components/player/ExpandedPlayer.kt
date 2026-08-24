package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.ic_forward_5
import com.cafarovceyxun.anamuslim.resources.ic_mic
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.ic_repeat
import com.cafarovceyxun.anamuslim.resources.ic_replay_5
import com.cafarovceyxun.anamuslim.resources.ic_skip_back
import com.cafarovceyxun.anamuslim.resources.ic_skip_forward
import com.cafarovceyxun.anamuslim.resources.icon_playback_speed
import com.cafarovceyxun.anamuslim.resources.expandedPlayerModeControls
import com.cafarovceyxun.anamuslim.resources.expandedPlayerModeSpotlight
import com.cafarovceyxun.anamuslim.resources.nTimes
import com.cafarovceyxun.anamuslim.resources.once
import com.cafarovceyxun.anamuslim.resources.playNextSurah
import com.cafarovceyxun.anamuslim.resources.playbackCount
import com.cafarovceyxun.anamuslim.resources.playbackSpeed
import com.cafarovceyxun.anamuslim.resources.repeatCurrentSurah
import com.cafarovceyxun.anamuslim.resources.stopPlayback
import com.cafarovceyxun.anamuslim.resources.strDescCollapse
import com.cafarovceyxun.anamuslim.resources.strTitleSelectReciter
import com.cafarovceyxun.anamuslim.resources.whenChapterEnds
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviourSheet
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.resources.audioBothArabicTranslation
import com.cafarovceyxun.anamuslim.resources.audioOnlyArabic
import com.cafarovceyxun.anamuslim.resources.audioOnlyTranslation
import com.cafarovceyxun.anamuslim.resources.audioOption
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOptionsSheet
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.PlaybackSpeedSheet
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.ReciterSelectorSheet
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.RepeatOptionsSheet
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatOneDecimal
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.utils.mediaplayer.rememberCurrentReciterNameForAudioOption
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationServiceState
import com.cafarovceyxun.anamuslim.utils.univ.formatDuration

private enum class ExpandedPlayerMode {
    Controls, Spotlight
}

@Composable
fun ExpandedPlayer(
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationPlayer,
    onPlayPause: (() -> Unit)? = null,
    onCollapse: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(ExpandedPlayerMode.Controls) }
    var chromeVisible by remember { mutableStateOf(true) }

    val showReciterSelector = rememberSaveable { mutableStateOf(false) }
    val showRepeatOptions = rememberSaveable { mutableStateOf(false) }
    val showPlaybackSpeedOptions = rememberSaveable { mutableStateOf(false) }
    val showEndBehaviorOptions = rememberSaveable { mutableStateOf(false) }
    val showAudioOptions = rememberSaveable { mutableStateOf(false) }

    Background(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalContentColor provides playerContentColor()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    // Landscape puts the cutout on a side edge, where it would sit over the
                    // collapse button and the artwork.
                    .displayCutoutPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Tapping the verse in Spotlight hides the chrome; the header is chrome too, so it
                // goes with the playback bar instead of staying behind on its own.
                AnimatedVisibility(
                    visible = mode == ExpandedPlayerMode.Controls || chromeVisible,
                    enter = fadeIn(tween(220)) + expandVertically(
                        animationSpec = tween(280),
                        expandFrom = Alignment.Bottom,
                    ),
                    exit = fadeOut(tween(180)) + shrinkVertically(
                        animationSpec = tween(220),
                        shrinkTowards = Alignment.Bottom,
                    ),
                ) {
                    PlayerHeader(
                        onCollapse = onCollapse,
                        mode = mode,
                        onModeChange = { mode = it }
                    )
                }

                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(300, easing = LinearEasing)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )).togetherWith(
                            fadeOut(tween(250, easing = LinearEasing))
                        ).using(SizeTransform(clip = false))
                    },
                    modifier = Modifier
                        .weight(1f)
                        // Swiping anywhere over the body switches modes; children (the seek slider,
                        // the spotlight's own gestures) are hit first, so they keep their drags.
                        .pointerInput(Unit) {
                            val threshold = 64.dp.toPx()
                            var dragged = 0f

                            detectHorizontalDragGestures(
                                onDragStart = { dragged = 0f },
                                onDragEnd = {
                                    when {
                                        dragged <= -threshold ->
                                            mode = ExpandedPlayerMode.Spotlight

                                        dragged >= threshold ->
                                            mode = ExpandedPlayerMode.Controls
                                    }
                                },
                            ) { change, dragAmount ->
                                dragged += dragAmount
                                change.consume()
                            }
                        },
                    label = "playerModeTransition"
                ) { currentMode ->
                    when (currentMode) {
                        ExpandedPlayerMode.Controls -> {
                            PlayerControlsContent(
                                state = state,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                controller = controller,
                                onPlayPause = onPlayPause,
                                showReciterSelector = showReciterSelector,
                                showRepeatOptions = showRepeatOptions,
                                showPlaybackSpeedOptions = showPlaybackSpeedOptions,
                                showEndBehaviorOptions = showEndBehaviorOptions,
                                showAudioOptions = showAudioOptions,
                            )
                        }

                        ExpandedPlayerMode.Spotlight -> {
                            ExpandedPlayerSpotlightSection(
                                modifier = Modifier.fillMaxSize(),
                                verse = state.currentVerse,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                controller = controller,
                                chromeVisible = chromeVisible,
                                onChromeVisibilityChanged = { chromeVisible = it }
                            )
                        }
                    }
                }
            }
        }
    }

    ReciterSelectorSheet(
        controller = controller,
        isOpen = showReciterSelector.value,
        onClose = { showReciterSelector.value = false }
    )

    RepeatOptionsSheet(
        controller = controller,
        isOpen = showRepeatOptions.value,
        onClose = { showRepeatOptions.value = false }
    )

    PlaybackSpeedSheet(
        controller = controller,
        isOpen = showPlaybackSpeedOptions.value,
        onClose = { showPlaybackSpeedOptions.value = false }
    )

    AudioEndBehaviourSheet(
        controller = controller,
        isOpen = showEndBehaviorOptions.value,
        onClose = { showEndBehaviorOptions.value = false }
    )

    AudioOptionsSheet(
        controller = controller,
        isOpen = showAudioOptions.value,
        onClose = { showAudioOptions.value = false }
    )
}

@Composable
private fun PlayerHeader(
    onCollapse: () -> Unit,
    mode: ExpandedPlayerMode,
    onModeChange: (ExpandedPlayerMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = stringResource(Res.string.strDescCollapse),
                modifier = Modifier.size(28.dp)
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(playerContentColor().alpha(0.06f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HeaderTabButton(
                text = stringResource(Res.string.expandedPlayerModeControls),
                selected = mode == ExpandedPlayerMode.Controls,
                onClick = { onModeChange(ExpandedPlayerMode.Controls) }
            )
            HeaderTabButton(
                text = stringResource(Res.string.expandedPlayerModeSpotlight),
                selected = mode == ExpandedPlayerMode.Spotlight,
                onClick = { onModeChange(ExpandedPlayerMode.Spotlight) }
            )
        }

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun HeaderTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (selected) 1f else 0f, label = "tabAlpha")
    val scale by animateFloatAsState(if (selected) 1f else 0.9f, label = "tabScale")

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(playerContentColor().alpha(0.12f * alpha))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) playerContentColor() else playerContentColor().alpha(0.5f),
        )
    }
}

@Composable
private fun PlayerControlsContent(
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationPlayer,
    onPlayPause: (() -> Unit)?,
    showReciterSelector: MutableState<Boolean>,
    showRepeatOptions: MutableState<Boolean>,
    showPlaybackSpeedOptions: MutableState<Boolean>,
    showEndBehaviorOptions: MutableState<Boolean>,
    showAudioOptions: MutableState<Boolean>,
) {
    val reciterNames = rememberCurrentReciterNameForAudioOption()

    // The artwork used to be a square 85% of the width. In landscape that square is taller than the
    // whole player, so everything below it — config grid, seek bar, transport — was pushed off
    // screen and unreachable. Both layouts now size the artwork from the shorter axis, and the
    // landscape one puts it beside the controls instead of above them.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            val artworkSize = minOf(maxHeight * 0.9f, maxWidth * 0.38f)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(artworkSize)) {
                    ExtendedThumbnail(
                        verse = state.currentVerse,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReciterLabel(reciterNames)

                    PlayerConfigGrid(
                        state = state,
                        reciterNames = reciterNames,
                        showReciterSelector = showReciterSelector,
                        showRepeatOptions = showRepeatOptions,
                        showPlaybackSpeedOptions = showPlaybackSpeedOptions,
                        showEndBehaviorOptions = showEndBehaviorOptions,
                        showAudioOptions = showAudioOptions,
                    )

                    PlayerProgressArea(
                        isPlaying = isPlaying,
                        controller = controller
                    )

                    PlaybackControls(
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        controller = controller,
                        onPlayPause = onPlayPause
                    )
                }
            }
        } else {
            val artworkSize = minOf(maxWidth * 0.85f, maxHeight * 0.42f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(modifier = Modifier.size(artworkSize)) {
                    ExtendedThumbnail(
                        verse = state.currentVerse,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ReciterLabel(
                    reciterNames = reciterNames,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                PlayerConfigGrid(
                    state = state,
                    reciterNames = reciterNames,
                    showReciterSelector = showReciterSelector,
                    showRepeatOptions = showRepeatOptions,
                    showPlaybackSpeedOptions = showPlaybackSpeedOptions,
                    showEndBehaviorOptions = showEndBehaviorOptions,
                    showAudioOptions = showAudioOptions,
                )

                PlayerProgressArea(
                    isPlaying = isPlaying,
                    controller = controller
                )

                PlaybackControls(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    controller = controller,
                    onPlayPause = onPlayPause
                )
            }
        }
    }
}

@Composable
private fun ReciterLabel(
    reciterNames: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = reciterNames,
        style = MaterialTheme.typography.bodyMedium,
        color = playerContentColor().alpha(0.7f),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun PlayerConfigGrid(
    state: RecitationServiceState,
    reciterNames: String,
    showReciterSelector: MutableState<Boolean>,
    showRepeatOptions: MutableState<Boolean>,
    showPlaybackSpeedOptions: MutableState<Boolean>,
    showEndBehaviorOptions: MutableState<Boolean>,
    showAudioOptions: MutableState<Boolean>,
) {
    val appLocale = LocalAppLocale.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlayerConfigButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.strTitleSelectReciter),
                subtext = reciterNames,
                icon = painterResource(Res.drawable.ic_mic),
                onClick = { showReciterSelector.value = true }
            )
            PlayerConfigButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.whenChapterEnds),
                subtext = when (state.settings.audioEndBehaviour) {
                    AudioEndBehaviour.STOP_PLAYBACK -> stringResource(Res.string.stopPlayback)
                    AudioEndBehaviour.NEXT_CHAPTER -> stringResource(Res.string.playNextSurah)
                    AudioEndBehaviour.REPEAT_CHAPTER -> stringResource(Res.string.repeatCurrentSurah)
                },
                icon = painterResource(Res.drawable.dr_icon_settings),
                onClick = { showEndBehaviorOptions.value = true }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlayerConfigButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.playbackCount),
                subtext = when (state.settings.repeatCount) {
                    0 -> stringResource(Res.string.once)
                    else -> stringResource(Res.string.nTimes, state.settings.repeatCount + 1)
                },
                icon = painterResource(Res.drawable.ic_repeat),
                onClick = { showRepeatOptions.value = true }
            )
            PlayerConfigButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.playbackSpeed),
                subtext = appLocale.formatOneDecimal(state.settings.speed) + "x",
                icon = painterResource(Res.drawable.icon_playback_speed),
                onClick = { showPlaybackSpeedOptions.value = true }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlayerConfigButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.audioOption),
                subtext = when (state.settings.audioOption) {
                    AudioOption.ONLY_QURAN -> stringResource(Res.string.audioOnlyArabic)
                    AudioOption.ONLY_TRANSLATION -> stringResource(Res.string.audioOnlyTranslation)
                    AudioOption.BOTH -> stringResource(Res.string.audioBothArabicTranslation)
                },
                icon = painterResource(Res.drawable.ic_mic),
                onClick = { showAudioOptions.value = true }
            )
            // Deliberately half width: a single button in a two-column grid keeps the row's rhythm.
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlayerProgressArea(
    isPlaying: Boolean,
    controller: RecitationPlayer,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val (positionMs, durationMs) = rememberTimestamp(isPlaying, controller)
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

        Slider(
            value = progress,
            onValueChange = { newProgress ->
                controller.seekTo((newProgress * durationMs).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = playerContentColor(),
                activeTrackColor = playerContentColor(),
                inactiveTrackColor = playerContentColor().alpha(0.2f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = playerContentColor().alpha(0.6f)
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = playerContentColor().alpha(0.6f)
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationPlayer,
    onPlayPause: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { controller.previousVerse() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(Res.drawable.ic_skip_back),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = playerContentColor()
            )
        }

        IconButton(
            onClick = { controller.seekLeft() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(Res.drawable.ic_replay_5),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = playerContentColor()
            )
        }

        Surface(
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape, spotColor = colorScheme.primary)
                .clip(CircleShape)
                .clickable {
                    if (onPlayPause != null) onPlayPause()
                    else controller.playPause()
                },
            color = Color(0xFF00897B), // Matching the green in the user image
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        painterResource(if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = Color.White
                    )
                }
            }
        }

        IconButton(
            onClick = { controller.seekRight() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(Res.drawable.ic_forward_5),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = playerContentColor()
            )
        }

        IconButton(
            onClick = { controller.nextVerse() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(Res.drawable.ic_skip_forward),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = playerContentColor()
            )
        }
    }
}
