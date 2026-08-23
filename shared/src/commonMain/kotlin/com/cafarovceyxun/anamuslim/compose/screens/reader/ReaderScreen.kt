package com.cafarovceyxun.anamuslim.compose.screens.reader

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.enterFullscreen
import com.cafarovceyxun.anamuslim.resources.exitFullscreen
import com.cafarovceyxun.anamuslim.resources.ic_expand
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.ic_shrink
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.resources.tajweedColors
import com.cafarovceyxun.anamuslim.resources.tajweed_connected_madd
import com.cafarovceyxun.anamuslim.resources.tajweed_ghunna
import com.cafarovceyxun.anamuslim.resources.tajweed_ghunna_only
import com.cafarovceyxun.anamuslim.resources.tajweed_idgham
import com.cafarovceyxun.anamuslim.resources.tajweed_idgham_no_ghunna
import com.cafarovceyxun.anamuslim.resources.tajweed_ikhfa
import com.cafarovceyxun.anamuslim.resources.tajweed_iqlab
import com.cafarovceyxun.anamuslim.resources.tajweed_necessary_madd
import com.cafarovceyxun.anamuslim.resources.tajweed_normal_madd
import com.cafarovceyxun.anamuslim.resources.tajweed_qalqala
import com.cafarovceyxun.anamuslim.resources.tajweed_separated_madd
import com.cafarovceyxun.anamuslim.resources.tajweed_silent_letter
import com.cafarovceyxun.anamuslim.resources.tajweed_tafkhim
import com.cafarovceyxun.anamuslim.compose.components.READER_FLOATING_CONTROLS_STRIP
import com.cafarovceyxun.anamuslim.compose.components.player.MINI_PLAYER_BOTTOM_GAP
import com.cafarovceyxun.anamuslim.compose.components.player.MINI_PLAYER_HEIGHT
import com.cafarovceyxun.anamuslim.compose.components.player.MiniPlayerVisibility
import com.cafarovceyxun.anamuslim.compose.components.player.RecitationPlayerSheet
import com.cafarovceyxun.anamuslim.compose.components.player.rememberIsMiniPlayerHidden
import com.cafarovceyxun.anamuslim.compose.components.player.rememberMiniPlayerVisibilityState
import com.cafarovceyxun.anamuslim.compose.components.player.requestShowMiniPlayer
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalRecitation
import com.cafarovceyxun.anamuslim.compose.components.reader.expandReaderChrome
import com.cafarovceyxun.anamuslim.compose.components.reader.readerChromeRevealGesture
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayout
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderProvider
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleFeedbackOverlay
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FullscreenMushafHeader
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.ReaderAppBar
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.ReaderNavigator
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.rememberAppBarDimensions
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.appScopedViewModelStoreOwner
import com.cafarovceyxun.anamuslim.compose.utils.isExpandedWindow
import com.cafarovceyxun.anamuslim.compose.components.reader.VolumeKeyToggle
import com.cafarovceyxun.anamuslim.compose.utils.app.KeepScreenOnIfEnabled
import com.cafarovceyxun.anamuslim.compose.utils.app.ReaderFullscreenEffect
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberToggleScreenRotation
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed.TajweedPalette
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(params: ReaderLaunchParams) {
    KeepScreenOnIfEnabled()
    // App-scoped, not back-stack-scoped: Android's `MainActivity.onKeyDown` routes volume / S Pen /
    // page keys through its own `by viewModels()` ReaderViewModel. A back-stack-scoped instance here
    // is a *different* object, so the `isReadingActive` flag below was set on one instance while the
    // key handler read it on another — and key navigation silently did nothing.
    val readerVm = viewModel(appScopedViewModelStoreOwner()) { ReaderViewModel() }

    LaunchedEffect(Unit) {
        readerVm.isReadingActive = true
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            readerVm.saveReadHistory()
            readerVm.isReadingActive = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val textMeasurer = rememberTextMeasurer()
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)
    val density = LocalDensity.current

    val isDark = ThemeUtils.observeDarkTheme()

    val coroutineScope = rememberCoroutineScope()

    var playerVerseSyncPref by readerVm.playerVerseSync
    var isSyncing by rememberSaveable { mutableStateOf(false) }
    val syncIndicatorLocked = playerVerseSyncPref && isSyncing

    val readerMode by readerVm.readerMode.collectAsStateWithLifecycle()

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var tajweedBarVisible by rememberSaveable { mutableStateOf(false) }

    val hidePlayerForChrome = isFullscreen || tajweedBarVisible

    // The player itself is hosted above the nav graph, so hiding it takes more than the local
    // visibility below — that one only zeroes out the height this screen reserves for it.
    DisposableEffect(hidePlayerForChrome) {
        ReaderChromeState.setHidePlayer(hidePlayerForChrome)
        onDispose { ReaderChromeState.setHidePlayer(false) }
    }

    // The player's verse-follow (lock) button is drawn by the host, which cannot reach this screen's
    // ReaderViewModel — publish the toggle so it acts on *this* reader.
    DisposableEffect(playerVerseSyncPref) {
        ReaderChromeState.setVerseSync(
            ReaderVerseSync(isEnabled = playerVerseSyncPref) {
                val enabled = !readerVm.playerVerseSync.value
                readerVm.playerVerseSync.value = enabled

                if (enabled) {
                    coroutineScope.launch { readerVm.syncToPlayingVerse() }
                }
            }
        )
        onDispose { ReaderChromeState.setVerseSync(null) }
    }

    // Publish the reader's current chapter to the host so the mini player can hide its "go to
    // reciting surah" button while that surah is already open. Cleared on leave.
    val readingVerse by readerVm.lastKnownVerseState.collectAsStateWithLifecycle()
    DisposableEffect(readingVerse?.chapterNo) {
        ReaderChromeState.setReadingChapterNo(readingVerse?.chapterNo)
        onDispose { ReaderChromeState.setReadingChapterNo(null) }
    }

    val playerVisibilityState = rememberMiniPlayerVisibilityState(
        visibility = if (hidePlayerForChrome) {
            MiniPlayerVisibility.HIDDEN
        } else {
            MiniPlayerVisibility.HIDDEN_BY_DEFAULT
        }
    )

    // The player's whole footprint, not just its bar: the host floats it a gap above the strip this
    // screen's own floating controls occupy. Reserving only the bar height left the last verse
    // sitting behind it. The window inset underneath is already in the Scaffold's content padding.
    val miniPlayerHeight = if (playerVisibilityState.isVisible) {
        MINI_PLAYER_HEIGHT + MINI_PLAYER_BOTTOM_GAP + READER_FLOATING_CONTROLS_STRIP
    } else {
        0.dp
    }

    val showTwoPane = isExpandedWindow()

    val isAutoScrollGestureMode by readerVm.isAutoScrollGestureMode
    val effectivelyFullscreen = isFullscreen || isAutoScrollGestureMode

    ReaderFullscreenEffect(effectivelyFullscreen)

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(enabled = effectivelyFullscreen) {
        if (isAutoScrollGestureMode) {
            readerVm.isAutoScrollGestureMode.value = false
            readerVm.autoScrollSpeed.value = null
        } else {
            isFullscreen = false
        }
    }

    LaunchedEffect(params) {
        isSyncing = false
        readerVm.initReaderIfNeeded(params)
    }

    ReaderProvider(viewModel = readerVm) {
        val verseActions = LocalVerseActions.current

        LaunchedEffect(lifecycleOwner, colors, type, density) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                readerVm.observeChanges(
                    uiConfig = ComposeUiConfig(
                        colors = colors,
                        type = type,
                        textMeasurer = textMeasurer,
                        density = density,
                        isDark = isDark,
                    ),
                    verseActions
                )
            }
        }

        val appBarDims = rememberAppBarDimensions(showTwoPane)
        val density = LocalDensity.current
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val totalExpandedHeight = appBarDims.expandedHeight + statusBarHeight

        val readerTopBarState = rememberTopAppBarState(
            initialHeightOffsetLimit = with(density) { -totalExpandedHeight.toPx() },
        )
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(readerTopBarState)
        
        LaunchedEffect(totalExpandedHeight) {
            scrollBehavior.state.heightOffsetLimit = -with(density) { totalExpandedHeight.toPx() }
        }

        // Toxunuş jesti bütün ekranı əhatə edir (bar və üzən düymələr də daxil), çünki heç nə
        // udmur — yalnız müşahidə edir və Final pass-da uşaq kliki olub-olmadığına baxır.
        val revealChrome: () -> Unit = remember(scrollBehavior, coroutineScope) {
            {
                coroutineScope.launch { expandReaderChrome(scrollBehavior.state) }
                Unit
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .readerChromeRevealGesture(
                    enabled = !effectivelyFullscreen,
                    onReveal = revealChrome,
                )
        ) {
            val chromeCollapsedFraction = scrollBehavior.state.collapsedFraction
            val bottomNavHeight = 0.dp
            val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding()

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!effectivelyFullscreen) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                        else Modifier
                    ),
                topBar = {
                    if (!effectivelyFullscreen) {
                        ReaderAppBar(
                            readerVm = readerVm,
                            isWideScreen = showTwoPane,
                            scrollBehavior = scrollBehavior,
                        )
                    }
                },
                containerColor = if (isDark || readerMode == ReaderMode.Translation) colorScheme.background
                else colorScheme.surface
            ) { padding ->
                val bottomChromeInset =
                    if (effectivelyFullscreen) 0.dp
                    else (miniPlayerHeight + bottomNavHeight) * (1f - chromeCollapsedFraction)

                // These modes hand this to their own scroll container as padding instead, so the
                // text shows through the floating chrome (mini player, tajweed/fullscreen strip)
                // rather than stopping at an opaque band above it. Translation mode still reserves
                // the space by shrinking — its page cards are clipped, not scrolled, at the bottom.
                val scrollsUnderChrome =
                    readerMode == ReaderMode.VerseByVerse || readerMode == ReaderMode.Reading

                val contentModifier = Modifier
                    .padding(padding)
                    .padding(bottom = if (scrollsUnderChrome) 0.dp else bottomChromeInset)

                if (showTwoPane && !effectivelyFullscreen) {
                    Row(
                        modifier = contentModifier.fillMaxSize(),
                    ) {
                        ReaderWideSidebar(
                            readerVm = readerVm,
                        )

                        VerticalDivider(color = colorScheme.outlineVariant.alpha(0.6f))

                        ReaderContentColumn(
                            modifier = Modifier.weight(1f),
                            isFullscreen = effectivelyFullscreen,
                            isWideScreen = true,
                            readerMode = readerMode,
                            readerVm = readerVm,
                            scrollBehavior = scrollBehavior,
                            onSyncStateChanged = { isSyncing = it },
                            bottomChromeInset = if (scrollsUnderChrome) bottomChromeInset else 0.dp,
                        )
                    }
                } else {
                    ReaderContentColumn(
                        modifier = contentModifier,
                        isFullscreen = effectivelyFullscreen,
                        isWideScreen = false,
                        readerMode = readerMode,
                        readerVm = readerVm,
                        scrollBehavior = scrollBehavior,
                        onSyncStateChanged = { isSyncing = it },
                        bottomChromeInset = if (scrollsUnderChrome) bottomChromeInset else 0.dp,
                    )
                }
            }


            val totalBottomOffset =
                navBarBottomInset + bottomNavHeight * (1f - chromeCollapsedFraction)

            if (!isAutoScrollGestureMode) {
                FloatingBar(
                    readerVm,
                    effectivelyFullscreen,
                    onChangeFullscreen = { isFullscreen = it },
                    tajweedBarVisible,
                    onChangeTajweedBarVisible = { tajweedBarVisible = it },
                    chromeCollapsedFraction,
                    totalBottomOffset,
                )
            }
        }

        val toggleFeedback by readerVm.toggleFeedback.collectAsStateWithLifecycle()

        ReaderToggleFeedbackOverlay(toggleFeedback)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContentColumn(
    modifier: Modifier,
    isFullscreen: Boolean,
    isWideScreen: Boolean,
    readerMode: ReaderMode?,
    readerVm: ReaderViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onSyncStateChanged: (Boolean) -> Unit,
    bottomChromeInset: Dp = 0.dp,
) {
    Column(modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        if ((isFullscreen || isWideScreen) && readerMode == ReaderMode.Reading) {
            FullscreenMushafHeader(
                readerVm = readerVm,
            )
            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
        }

        ReaderLayout(
            readerVm = readerVm,
            nestedScrollConnection = if (!isFullscreen) scrollBehavior.nestedScrollConnection else null,
            onSyncStateChanged = onSyncStateChanged,
            bottomChromeInset = bottomChromeInset,
        )
    }
}

@Composable
private fun ReaderWideSidebar(
    readerVm: ReaderViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(360.dp)
            .background(colorScheme.surfaceContainerLow),
    ) {
        ReaderNavigator(
            readerVm = readerVm,
            isInModal = false,
            onClose = {},
        )
    }
}

@Composable
private fun BoxScope.FloatingBar(
    readerVm: ReaderViewModel,
    isFullscreen: Boolean,
    onChangeFullscreen: (Boolean) -> Unit,
    tajweedBarVisible: Boolean,
    onChangeTajweedBarVisible: (Boolean) -> Unit,
    chromeCollapsedFraction: Float,
    totalBottomOffset: Dp
) {
    val fullscreenButtonAlpha = if (isFullscreen) if (tajweedBarVisible) 1f else 0.65f
    else (1f - chromeCollapsedFraction).coerceIn(0f, 1f)

    val currentScript = ReaderPreferences.observeQuranScript()
    val tajweedColorsEnabled = ReaderPreferences.observeTajweedColorsEnabled()
    val isUthmaniAtlas = currentScript == QuranScriptUtils.SCRIPT_UTHMANI

    val tajweedSupported =
        currentScript == QuranScriptUtils.SCRIPT_KFQPC_V4 ||
                (isUthmaniAtlas && tajweedColorsEnabled)

    LaunchedEffect(tajweedSupported) {
        if (!tajweedSupported) {
            onChangeTajweedBarVisible(false)
        }
    }

    // Uthmani atlas uses its own granular rule set (idgham with and without ghunna,
    // ghunna, qalqalah, ikhfa, iqlab) with colors distinct from the KFQPC V4
    // legend below — e.g. qalqalah is green here vs. cyan for KFQPC V4, and
    // tafkhim isn't available in the Uthmani atlas source data yet.
    val rulesMap = remember(isUthmaniAtlas) {
        buildMap {
            if (isUthmaniAtlas) {
                // The consonant rules are the coloured set in the Uthmani atlas (user request,
                // 2026-07-25); the madds render in the plain text colour, so classes 1 and 3-5
                // are absent from both the data and this legend. Class 2 carries
                // idghaam *without* ghunnah (added 2026-08-10, amber) — listed first so the two
                // idghaam rows do not sit apart in the legend.
                put(TajweedPalette.colors[2], Res.string.tajweed_idgham_no_ghunna)
                put(TajweedPalette.colors[6], Res.string.tajweed_ghunna_only)
                put(TajweedPalette.colors[7], Res.string.tajweed_qalqala)
                put(TajweedPalette.colors[8], Res.string.tajweed_ikhfa)
                put(TajweedPalette.colors[9], Res.string.tajweed_idgham)
                put(TajweedPalette.colors[10], Res.string.tajweed_iqlab)
            } else {
                put(Color(0xFF999999), Res.string.tajweed_silent_letter)
                put(Color(0xFFffc1e0), Res.string.tajweed_normal_madd)
                put(Color(0xFFff8e3b), Res.string.tajweed_separated_madd)
                put(Color(0xFFff5e8e), Res.string.tajweed_connected_madd)
                put(Color(0xFFe30000), Res.string.tajweed_necessary_madd)
                put(Color(0xFF26b55d), Res.string.tajweed_ghunna)
                put(Color(0xFF00deff), Res.string.tajweed_qalqala)
                put(Color(0xFF3c84d5), Res.string.tajweed_tafkhim)
            }
        }
    }

    Column(
        Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = totalBottomOffset)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
            .zIndex(10f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val toggleRotation = rememberToggleScreenRotation()

            if (tajweedSupported) {
                Box(
                    modifier = Modifier.alpha(fullscreenButtonAlpha),
                ) {
                    TextButton(
                        onClick = {
                            onChangeTajweedBarVisible(!tajweedBarVisible)
                        },
                        modifier = Modifier.height(32.dp),
                        shape = shapes.extraLarge,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorScheme.surfaceContainer,
                            contentColor = colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp
                        ),
                        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 16.dp)
                    ) {
                        Text(stringResource(Res.string.tajweedColors))
                        Icon(
                            painterResource(Res.drawable.dr_icon_chevron_down),
                            contentDescription = null,
                            modifier = Modifier
                                .rotate(if (tajweedBarVisible) 0f else 180f)
                                .size(16.dp),
                        )
                    }
                }
            }

            if (isFullscreen || fullscreenButtonAlpha > 0.01f) {
                Row(
                    modifier = Modifier.alpha(fullscreenButtonAlpha),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rotation is Android-only (iOS returns null), but fullscreen is pure Compose
                    // and works everywhere — so the guard covers the rotation button alone. Wrapping
                    // both took the fullscreen button down with it on iOS.
                    if (toggleRotation != null) {
                        TextButton(
                            onClick = {
                                toggleRotation.invoke()
                            },
                            modifier = Modifier.height(32.dp),
                            shape = shapes.extraLarge,
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = colorScheme.surfaceContainer,
                                contentColor = colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp
                            ),
                            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ScreenRotation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // Starts the recitation at the verse the reader is actually on (and with it,
                    // brings the mini player back); once playing it just toggles play/pause. If a
                    // recitation is loaded but its mini player was swiped away, the first tap simply
                    // re-reveals the player instead of pausing.
                    val recitation = LocalRecitation.current
                    val miniPlayerHidden = rememberIsMiniPlayerHidden()

                    TextButton(
                        onClick = {
                            if (miniPlayerHidden) {
                                requestShowMiniPlayer()
                            } else if (recitation.isAnyPlaying) {
                                recitation.controller.playPause()
                            } else {
                                val verse = readerVm.lastKnownVerse?.takeIf { it.isValid }

                                if (verse != null) recitation.controller.playControl(verse)
                                else recitation.controller.playPause()
                            }
                        },
                        modifier = Modifier.height(32.dp),
                        shape = shapes.extraLarge,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorScheme.surfaceContainer,
                            contentColor = if (recitation.isAnyPlaying) colorScheme.primary
                            else colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp
                        ),
                        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (recitation.isAnyPlaying) Res.drawable.ic_pause
                                else Res.drawable.ic_play
                            ),
                            contentDescription = stringResource(
                                if (recitation.isAnyPlaying) Res.string.strLabelPause
                                else Res.string.strLabelPlay
                            ),
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    TextButton(
                        onClick = {
                            onChangeFullscreen(!isFullscreen)
                        },
                        modifier = Modifier.height(32.dp),
                        shape = shapes.extraLarge,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorScheme.surfaceContainer,
                            contentColor = colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp
                        ),
                        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isFullscreen) Res.drawable.ic_shrink
                                else Res.drawable.ic_expand
                            ),
                            contentDescription = stringResource(
                                if (isFullscreen) Res.string.exitFullscreen
                                else Res.string.enterFullscreen
                            ),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(tajweedSupported && tajweedBarVisible) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .shadow(4.dp, shape = shapes.large)
                    .background(
                        colorScheme.surfaceContainer,
                        shape = shapes.large
                    )
                    .border(
                        BorderStroke(1.dp, colorScheme.outlineVariant),
                        shape = shapes.large
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rulesMap.forEach { (color, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(color, shape = shapes.small)
                        )
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
