package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.arabicLabel
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.ic_mode_mushaf
import com.cafarovceyxun.anamuslim.resources.ic_mode_translation
import com.cafarovceyxun.anamuslim.resources.ic_mode_verse
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.strLabelBack
import com.cafarovceyxun.anamuslim.resources.strLabelMixed
import com.cafarovceyxun.anamuslim.resources.strTitleSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.MutableIntState
import com.cafarovceyxun.anamuslim.compose.components.common.AppBarDefaults
import com.cafarovceyxun.anamuslim.compose.components.common.appBarInsetsPadding
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.AutoScrollButton
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.reader.VolumeKeyToggle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import kotlinx.coroutines.launch

// Public (not `internal` like ReaderAppBarDimensions) because `HadithItemsScreen` still lives in
// `:app` and consumes it; narrow this back to `internal` once that screen moves to commonMain.
data class HadithAppBarDimensions(
    val barHeight: Dp,
    val dividerHeight: Dp,
    val dividerCount: Int,
) {
    val baseExpandedHeight: Dp
        get() = barHeight + (dividerHeight * dividerCount) + 56.dp
}

@Composable
fun rememberHadithAppBarDimensions(): HadithAppBarDimensions {
    // `barHeight` is the primary row's real height, straight from the shared scale — the bar used to
    // carry a padded total and then render the row at `barHeight - 14.dp`, which left the hadith row
    // 8dp taller than every other screen's in portrait.
    val barHeight = AppBarDefaults.barHeight

    return remember(barHeight) {
        HadithAppBarDimensions(
            barHeight = barHeight,
            dividerHeight = 1.dp,
            dividerCount = 2,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithAppBar(
    title: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    autoScrollSpeed: MutableState<Float?>,
    isAutoScrollGestureMode: MutableState<Boolean>,
    autoScrollStep: MutableIntState,
    onVolumeToggle: (Boolean) -> Unit = {},
    onNavigatorClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val appBarDims = rememberHadithAppBarDimensions()
    val density = LocalDensity.current
    
    // We use a state that won't reset to 0 unless explicitly told
    var fullHeightPx by remember { mutableIntStateOf(0) }
    
    // Update the height offset limit based on the actual measured height
    LaunchedEffect(fullHeightPx) {
        if (fullHeightPx > 0) {
            scrollBehavior.state.heightOffsetLimit = -fullHeightPx.toFloat()
        }
    }

    // Dynamic height for the layout to pull up the list content
    val heightOffset = scrollBehavior.state.heightOffset
    val layoutHeight = remember(fullHeightPx, heightOffset) {
        if (fullHeightPx <= 0) {
            Dp.Unspecified 
        } else {
            // Use coerceIn to keep height stable and ensure it can expand back
            with(density) { (fullHeightPx + heightOffset).coerceIn(0f, fullHeightPx.toFloat()).toDp() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (layoutHeight != Dp.Unspecified) Modifier.height(layoutHeight) 
                else Modifier.onGloballyPositioned { coords ->
                    if (coords.size.height > 0) {
                        fullHeightPx = coords.size.height
                    }
                }
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = heightOffset
                }
                .onGloballyPositioned { coordinates ->
                    if (coordinates.size.height > 0 && coordinates.size.height > fullHeightPx) {
                        fullHeightPx = coordinates.size.height
                    }
                },
            color = colorScheme.surfaceContainer,
            shadowElevation = AppBarDefaults.ShadowElevation
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
                            HadithModeTabs(selectedTab, onTabSelected)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                                contentDescription = stringResource(Res.string.strLabelBack),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_settings),
                                contentDescription = stringResource(Res.string.strTitleSettings),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )

                HorizontalDivider(
                    thickness = appBarDims.dividerHeight,
                    color = colorScheme.outlineVariant.alpha(0.3f)
                )

                val isLandscape = isLandscape()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left icons Group
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant.alpha(0.3f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VolumeKeyToggle { onVolumeToggle(it) }

                        VerticalDivider(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(horizontal = 2.dp),
                            color = colorScheme.outlineVariant.alpha(0.3f)
                        )

                        AutoScrollButton(
                            autoScrollSpeed = autoScrollSpeed,
                            isAutoScrollGestureMode = isAutoScrollGestureMode,
                            autoScrollStep = autoScrollStep,
                            readerMode = ReaderMode.VerseByVerse // Hadith is vertical
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Dynamic Navigator
                    Surface(
                        onClick = onNavigatorClick,
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primary.alpha(0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = title,
                                style = typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_chevron_down),
                                contentDescription = null,
                                tint = Color(0xFF2E7D32).alpha(0.6f),
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(16.dp)
                            )
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
}

@Composable
private fun HadithModeTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHighest.alpha(0.4f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            Res.drawable.ic_mode_verse to stringResource(Res.string.strLabelMixed),
            Res.drawable.ic_mode_mushaf to stringResource(Res.string.arabicLabel),
            Res.drawable.ic_mode_translation to stringResource(Res.string.labelTranslation)
        )

        tabs.forEachIndexed { index, (iconRes, label) ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) colorScheme.primary else Color.Transparent,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(index) },
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        painterResource(iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) colorScheme.onPrimary 
                               else colorScheme.onSurface.alpha(0.6f)
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
