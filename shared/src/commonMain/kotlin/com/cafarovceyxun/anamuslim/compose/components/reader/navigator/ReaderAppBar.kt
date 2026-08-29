package com.cafarovceyxun.anamuslim.compose.components.reader.navigator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.autoScroll
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.ic_mode_book
import com.cafarovceyxun.anamuslim.resources.ic_mode_mushaf
import com.cafarovceyxun.anamuslim.resources.ic_mode_translation_vertical
import com.cafarovceyxun.anamuslim.resources.labelHizbNo
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.modeMushaf
import com.cafarovceyxun.anamuslim.resources.readerBookModeDisable
import com.cafarovceyxun.anamuslim.resources.readerBookModeEnable
import com.cafarovceyxun.anamuslim.resources.modeVerseByVerse
import com.cafarovceyxun.anamuslim.resources.strDescGoBack
import com.cafarovceyxun.anamuslim.resources.strLabelJuzNo
import com.cafarovceyxun.anamuslim.resources.strLabelPageNo
import com.cafarovceyxun.anamuslim.resources.strTitleReaderHizb
import com.cafarovceyxun.anamuslim.resources.strTitleSettings
import com.cafarovceyxun.anamuslim.compose.components.ChapterIcon
import com.cafarovceyxun.anamuslim.compose.components.JuzIcon
import com.cafarovceyxun.anamuslim.compose.components.common.AppBarDefaults
import com.cafarovceyxun.anamuslim.compose.components.common.appBarInsetsPadding
import com.cafarovceyxun.anamuslim.compose.components.common.tabStripSwipeModifier
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.reader.AutoScroll
import com.cafarovceyxun.anamuslim.compose.components.reader.KeepScreenOnToggle
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleKind
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.AutoScrollSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.VolumeKeyToggle
import com.cafarovceyxun.anamuslim.compose.theme.AppIcons
import com.cafarovceyxun.anamuslim.compose.theme.tightTextStyle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.compose.utils.rememberSystemBack
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalReaderActions
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.ReaderSettingsSheet
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.reader.toQuranMushafId
import com.cafarovceyxun.anamuslim.viewModels.ReaderUiState
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewType
import kotlinx.coroutines.launch

internal data class ReaderAppBarDimensions(
    val barHeight: Dp,
    val headerHeight: Dp,
    val dividerHeight: Dp,
    val dividerCount: Int,
) {
    val expandedHeight: Dp
        get() = barHeight + headerHeight + (dividerHeight * dividerCount)
}

@Composable
internal fun rememberAppBarDimensions(isWideScreen: Boolean): ReaderAppBarDimensions {
    val isLandscape = isLandscape()

    // The primary row rides the shared scale in both orientations, so the reader's bar lines up with
    // every other screen instead of standing 12dp taller in portrait; see AppBarDefaults.
    val barHeight = AppBarDefaults.barHeight

    return remember(isWideScreen, isLandscape, barHeight) {
        if (isWideScreen) {
            ReaderAppBarDimensions(
                barHeight = barHeight,
                headerHeight = 0.dp,
                dividerHeight = 1.dp,
                dividerCount = 1,
            )
        } else {
            ReaderAppBarDimensions(
                barHeight = barHeight,
                headerHeight = if (isLandscape) 44.dp else 52.dp,
                dividerHeight = 1.dp,
                dividerCount = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppBar(
    readerVm: ReaderViewModel,
    isWideScreen: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val appBarDims = rememberAppBarDimensions(isWideScreen)

    val readerActions = LocalReaderActions.current
    val systemBack = rememberSystemBack()
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val readerMode by readerVm.readerMode.collectAsState()
    var showNavigatorSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalExpandedHeight = appBarDims.expandedHeight + statusBarHeight
    
    val heightOffset = scrollBehavior.state.heightOffset
    val visibleHeight = with(density) {
        (totalExpandedHeight.toPx() + heightOffset).coerceAtLeast(0f).toDp()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(visibleHeight),
        shadowElevation = AppBarDefaults.ShadowElevation,
        color = colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appBarInsetsPadding()
        ) {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(appBarDims.barHeight),
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                title = {
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        ModeTabs(readerVm, readerMode)
                    }
                },
                navigationIcon = {
                    SimpleTooltip(text = stringResource(Res.string.strDescGoBack)) {
                        IconButton(
                            onClick = {
                                systemBack?.invoke()
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                                contentDescription = stringResource(Res.string.strDescGoBack),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isWideScreen) {
                        when (readerMode) {
                            ReaderMode.Reading -> {
                            }

                            ReaderMode.Translation, ReaderMode.TranslationVertical -> {
                                TranslationModeToggle(readerVm, readerMode)
                            }

                            else -> {
                                StickyHeaderModeVbV(readerVm, uiState, true) {}
                            }
                        }
                    }

                    SimpleTooltip(text = stringResource(Res.string.strTitleSettings)) {
                        IconButton(
                            // Oxucudan çıxmadan — hədis oxucusundakı ⚙ ilə eyni davranış.
                            // Tam ayarlar ekranı vərəqin «Bütün ayarlar» sətrindən açılır.
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_settings),
                                contentDescription = stringResource(Res.string.strTitleSettings),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
            )

            HorizontalDivider(
                thickness = appBarDims.dividerHeight,
                color = colorScheme.outlineVariant.alpha(0.3f)
            )

            if (!isWideScreen) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(appBarDims.headerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    when (readerMode) {
                        ReaderMode.Reading -> {
                            StickyHeaderModeMushaf(readerVm) {
                                if (!isWideScreen) {
                                    showNavigatorSheet = true
                                }
                            }
                        }

                        ReaderMode.Translation, ReaderMode.TranslationVertical -> {
                            StickyHeaderModeTranslation(readerVm) {
                                if (!isWideScreen) {
                                    showNavigatorSheet = true
                                }
                            }
                        }

                        else -> {
                            StickyHeaderModeVbV(readerVm, uiState, false) {
                                if (!isWideScreen) {
                                    showNavigatorSheet = true
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = appBarDims.dividerHeight,
                    color = colorScheme.outlineVariant.alpha(0.3f)
                )
            }
        }
    }

    if (showNavigatorSheet && !isWideScreen) {
        ModalBottomSheet(
            onDismissRequest = { showNavigatorSheet = false },
            sheetState = sheetState,
            scrimColor = colorScheme.scrim.alpha(0.5f),
            containerColor = colorScheme.background,
            contentColor = colorScheme.onSurface,
            dragHandle = null,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
        ) {
            ReaderNavigator(
                readerVm = readerVm,
                isInModal = true,
            ) { showNavigatorSheet = false }
        }
    }

    ReaderSettingsSheet(
        isOpen = showSettingsSheet,
        onDismiss = { showSettingsSheet = false },
        onOpenRoute = { route -> ReaderUiHooks.openSettingsRoute?.invoke(route) },
        onOpenAllSettings = { readerActions.onOpenReaderSettings() },
    )
}

@Composable
private fun ModeTabs(
    readerVm: ReaderViewModel,
    readerMode: ReaderMode?
) {
    val modes = remember {
        listOf(
            ReaderMode.VerseByVerse,
            ReaderMode.Reading,
            ReaderMode.Translation,
        )
    }
    // `TranslationVertical` is the translation tab in another orientation, not a fourth tab, so it
    // lights the same segment — and a swipe out of it lands on the neighbour of *that* segment.
    val selectedIndex = modes.indexOfFirst {
        it == readerMode || (it == ReaderMode.Translation && readerMode == ReaderMode.TranslationVertical)
    }.coerceAtLeast(0)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHighest.alpha(0.4f))
            // Same gesture as the bottom bar: drag across the strip to walk the modes.
            .then(
                tabStripSwipeModifier(
                    tabCount = modes.size,
                    selectedIndex = selectedIndex,
                    onSelect = { index -> readerVm.setReaderMode(modes[index]) },
                )
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            val isSelected = mode == readerMode || (mode == ReaderMode.Translation && readerMode == ReaderMode.TranslationVertical)
            val label = stringResource(
                when (mode) {
                    ReaderMode.VerseByVerse -> Res.string.modeVerseByVerse
                    ReaderMode.Reading -> Res.string.modeMushaf
                    ReaderMode.Translation, ReaderMode.TranslationVertical -> Res.string.labelTranslation
                },
            )

            SimpleTooltip(label) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) colorScheme.primary else Color.Transparent,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                readerVm.setReaderMode(mode)
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = when (mode) {
                                ReaderMode.VerseByVerse -> AppIcons.Hadith
                                ReaderMode.Reading -> AppIcons.Quran
                                ReaderMode.Translation, ReaderMode.TranslationVertical -> AppIcons.Translation
                            },
                            contentDescription = label,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface.alpha(
                                0.6f
                            )
                        )
                        if (isSelected) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 8.dp),
                                style = typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickyHeaderModeVbV(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
    isWideScreen: Boolean,
    onNavigatorRequest: () -> Unit
) {

    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookModeToggle()

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colorScheme.surfaceVariant.alpha(0.3f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VolumeKeyToggle { readerVm.triggerToggleFeedback(ReaderToggleKind.VolumeKeyNavigation, it) }

            KeepScreenOnToggle { readerVm.triggerToggleFeedback(ReaderToggleKind.KeepScreenOn, it) }

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                color = colorScheme.outlineVariant.alpha(0.3f)
            )

            AutoScrollButton(
                autoScrollSpeed = readerVm.autoScrollSpeed,
                isAutoScrollGestureMode = readerVm.isAutoScrollGestureMode,
                autoScrollStep = readerVm.autoScrollStep,
                readerMode = readerVm.readerMode.collectAsState().value
            )
        }

        if (!isWideScreen) {
            Spacer(
                Modifier.weight(1f)
            )

            TextButton(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = colorScheme.primary
                ),
                onClick = onNavigatorRequest,
            ) {
                Icon(
                    painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp)
                )

                when (val vt = uiState.viewType) {
                    is ReaderViewType.Juz -> JuzIcon(
                        juzNo = vt.juzNo,
                        fontSize = 22.sp,
                        color = colorScheme.primary
                    )

                    is ReaderViewType.Hizb -> Text(
                        text = stringResource(Res.string.labelHizbNo, vt.hizbNo),
                        style = typography.labelLarge,
                        color = colorScheme.primary,
                    )

                    is ReaderViewType.Chapter -> ChapterIcon(
                        modifier = Modifier.padding(top = 8.dp),
                        chapterNo = vt.chapterNo,
                        fontSize = 32.sp,
                        color = colorScheme.primary
                    )

                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun StickyHeaderModeMushaf(
    readerVm: ReaderViewModel,
    onNavigatorRequest: () -> Unit
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val currentPageNo = mushafSession.currentPageNo
    val scriptCode = ReaderPreferences.observeQuranScript()
    val scriptVariant = ReaderPreferences.observeQuranScriptVariant()

    val chaptersOfPage by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo
        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }
        val mushafId = scriptCode.toQuranMushafId(scriptVariant)
        if (mushafId <= 0) {
            value = ""
            return@produceState
        }
        value = readerVm.repository.getChapterNamesOnMushafPage(
            mushafId,
            pageNo,
        )
    }

    val pageDivisionLabel by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo

        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }

        val mushafId = scriptCode.toQuranMushafId(scriptVariant)

        if (mushafId <= 0) {
            value = ""
            return@produceState
        }

        val juzNo = readerVm.repository.getJuzForMushafPages(mushafId, listOf(pageNo))[pageNo]
            ?: 0
        val hizbNos = readerVm.repository.getHizbForMushafPages(mushafId, listOf(pageNo))[pageNo]
            .orEmpty()
            .filter { it > 0 }
            .distinct()
            .sorted()

        value = buildString {
            if (juzNo > 0) {
                append(getString(Res.string.strLabelJuzNo, juzNo))
            }

            if (hizbNos.isNotEmpty()) {
                if (isNotEmpty()) append(" \u2022 ")
                append(getString(Res.string.strTitleReaderHizb))
                append(" ")
                append(hizbNos.joinToString(" / "))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            chaptersOfPage,
            style = typography.labelMedium.copy(
                lineHeightStyle = tightTextStyle.lineHeightStyle
            ),
            modifier = Modifier.weight(0.4f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = colorScheme.onSurface.alpha(0.85f)
        )


        Box(
            Modifier.weight(0.6f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VolumeKeyToggle { readerVm.triggerToggleFeedback(ReaderToggleKind.VolumeKeyNavigation, it) }

                KeepScreenOnToggle { readerVm.triggerToggleFeedback(ReaderToggleKind.KeepScreenOn, it) }

                Spacer(Modifier.width(4.dp))

                TextButton(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = colorScheme.onSurface
                ),
                onClick = onNavigatorRequest,
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    if (currentPageNo != null) {
                        Text(
                            stringResource(Res.string.strLabelPageNo, currentPageNo),
                            style = typography.labelLarge.copy(
                                lineHeightStyle = tightTextStyle.lineHeightStyle
                            ),
                            color = colorScheme.primary,
                        )
                    }

                    if (pageDivisionLabel.isNotBlank()) {
                        Text(
                            text = pageDivisionLabel,
                            style = typography.labelSmall.copy(
                                lineHeightStyle = tightTextStyle.lineHeightStyle
                            ),
                            color = colorScheme.onSurface.alpha(0.85f),
                        )
                    }
                }

                Icon(
                    painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    tint = colorScheme.primary,
                )
            }
          }
        }
    }
}

@Composable
private fun StickyHeaderModeTranslation(
    readerVm: ReaderViewModel,
    onNavigatorRequest: () -> Unit
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val readerMode by readerVm.readerMode.collectAsState()
    val currentPageNo = mushafSession.currentPageNo

    val isVertical = readerMode == ReaderMode.TranslationVertical

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TranslationModeToggle(readerVm, readerMode)

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colorScheme.surfaceVariant.alpha(0.3f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VolumeKeyToggle { readerVm.triggerToggleFeedback(ReaderToggleKind.VolumeKeyNavigation, it) }

            KeepScreenOnToggle { readerVm.triggerToggleFeedback(ReaderToggleKind.KeepScreenOn, it) }

            if (isVertical) {
                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 2.dp),
                    color = colorScheme.outlineVariant.alpha(0.3f)
                )

                AutoScrollButton(
                    autoScrollSpeed = readerVm.autoScrollSpeed,
                    isAutoScrollGestureMode = readerVm.isAutoScrollGestureMode,
                    autoScrollStep = readerVm.autoScrollStep,
                    readerMode = readerMode
                )
            }
        }

        Spacer(
            Modifier.weight(1f)
        )

        TextButton(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorScheme.onSurface
            ),
            onClick = onNavigatorRequest,
        ) {
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                if (currentPageNo != null) {
                    Text(
                        stringResource(Res.string.strLabelPageNo, currentPageNo),
                        style = typography.labelLarge,
                        color = colorScheme.primary,
                    )
                }
            }

            Icon(
                painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = null,
                tint = colorScheme.primary,
            )
        }
    }
}

/**
 * Kitab rejimi açarı — ayə-ayə rejiminin barında.
 *
 * Açıq olanda eyni məzmun (ərəbcə + tərcümə + qeyd) kart siyahısı yerinə müshəf səhifəsi başına
 * vərəqlənən kitab səhifələri kimi verilir
 * ([com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayoutBookPageMode]).
 *
 * Rejim tabı deyil — [ReaderMode]-a dördüncü üzv tabları da, «açılış rejimi» ayarını da bölərdi;
 * bu isə ayə-ayə rejiminin içindəki müstəqil açardır.
 */
@Composable
private fun BookModeToggle() {
    val scope = rememberCoroutineScope()
    val bookMode = ReaderPreferences.observeBookMode()

    val label = stringResource(
        if (bookMode) Res.string.readerBookModeDisable else Res.string.readerBookModeEnable
    )

    SimpleTooltip(text = label) {
        IconButton(
            onClick = {
                scope.launch { ReaderPreferences.setBookMode(!bookMode) }
            },
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (bookMode) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                    shapes.medium,
                )
                .clip(shapes.medium)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_mode_book),
                contentDescription = label,
                tint = if (bookMode) colorScheme.primary else colorScheme.onSurface.alpha(0.75f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TranslationModeToggle(
    readerVm: ReaderViewModel,
    readerMode: ReaderMode?
) {
    val isVertical = readerMode == ReaderMode.TranslationVertical

    IconButton(
        onClick = {
            val nextMode = if (isVertical) ReaderMode.Translation else ReaderMode.TranslationVertical
            readerVm.setReaderMode(nextMode)
        },
        modifier = Modifier
            .size(40.dp)
            .background(colorScheme.surfaceContainer, shapes.medium)
            .clip(shapes.medium)
    ) {
        AnimatedContent(
            targetState = isVertical,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> width } + fadeOut())
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "TranslationModeToggle"
        ) { vertical ->
            Icon(
                painter = painterResource(
                    if (vertical) Res.drawable.ic_mode_translation_vertical
                    else Res.drawable.ic_mode_mushaf
                ),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
            )
        }
    }
}


@Composable
fun FullscreenMushafHeader(
    modifier: Modifier = Modifier,
    readerVm: ReaderViewModel,
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val currentPageNo = mushafSession.currentPageNo
    val scriptCode = ReaderPreferences.observeQuranScript()
    val scriptVariant = ReaderPreferences.observeQuranScriptVariant()

    val chapterName by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo
        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }

        val mushafId = scriptCode.toQuranMushafId(scriptVariant)
        if (mushafId <= 0) {
            value = ""
            return@produceState
        }

        value = readerVm.repository.getChapterNamesOnMushafPage(mushafId, pageNo)
    }

    val juzHizb by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo
        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }

        val mushafId = scriptCode.toQuranMushafId(scriptVariant)
        if (mushafId <= 0) {
            value = ""
            return@produceState
        }

        val juzNo = readerVm.repository.getJuzForMushafPages(mushafId, listOf(pageNo))[pageNo] ?: 0
        val hizbNos = readerVm.repository.getHizbForMushafPages(mushafId, listOf(pageNo))[pageNo]
            .orEmpty()
            .filter { it > 0 }
            .distinct()
            .sorted()

        value = buildString {
            if (juzNo > 0) {
                append(getString(Res.string.strLabelJuzNo, juzNo))
            }

            if (hizbNos.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(getString(Res.string.strTitleReaderHizb))
                append(" ")
                append(hizbNos.joinToString(" / "))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = chapterName,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(
                    initialDelayMillis = 900,
                    repeatDelayMillis = 1_200,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.alpha(0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
        )

        Text(
            text = currentPageNo?.let { stringResource(Res.string.strLabelPageNo, it) }.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Text(
            text = juzHizb,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(
                    initialDelayMillis = 900,
                    repeatDelayMillis = 1_200,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.alpha(0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoScrollButton(
    autoScrollSpeed: MutableState<Float?>,
    isAutoScrollGestureMode: MutableState<Boolean>,
    autoScrollStep: MutableIntState,
    readerMode: ReaderMode? = null,
) {
    val autoScrollSpeedValue by autoScrollSpeed
    val isGestureMode by isAutoScrollGestureMode
    val isActive = autoScrollSpeedValue != null || isGestureMode

    var autoScrollSheetOpen by rememberSaveable { mutableStateOf(false) }

    SimpleTooltip(text = stringResource(Res.string.autoScroll)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shapes.medium)
                .combinedClickable(
                    onClick = {
                        if (isActive) {
                            autoScrollSpeed.value = null
                            isAutoScrollGestureMode.value = false
                        } else {
                            if (readerMode == ReaderMode.VerseByVerse || readerMode == ReaderMode.TranslationVertical) {
                                isAutoScrollGestureMode.value = true
                            } else {
                                autoScrollSpeed.value =
                                    AutoScroll.speedOfLevel(ReaderPreferences.getAutoScrollSpeedSync())
                            }
                        }
                    },
                    onLongClick = {
                        autoScrollSheetOpen = true
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.ScrollDown,
                contentDescription = stringResource(Res.string.autoScroll),
                tint = if (isActive) colorScheme.primary else colorScheme.onSurface.alpha(0.75f)
            )
        }
    }

    AutoScrollSheet(
        autoScrollSpeedProvider = autoScrollSpeed,
        isAutoScrollGestureMode = isAutoScrollGestureMode,
        autoScrollStep = autoScrollStep,
        readerMode = readerMode,
        isOpen = autoScrollSheetOpen,
    ) {
        autoScrollSheetOpen = false
    }
}
