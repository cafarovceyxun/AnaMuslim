package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.cafarovceyxun.anamuslim.compose.theme.tightTextStyle
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.strLabelBack
import com.cafarovceyxun.anamuslim.resources.strTitleSettings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun getHadithIndexHeaderHeights(): Pair<Dp, Dp> {
    return if (isLandscape()) {
        140.dp to 56.dp
    } else {
        220.dp to 84.dp
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithCollapsingToolbar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    expandedHeight: Dp,
    collapsedHeight: Dp,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    showLogo: Boolean = true
) {
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    val isLandscape = isLandscape()

    val appBarHeight = if (showLogo) {
        lerp(
            start = expandedHeight,
            stop = collapsedHeight,
            fraction = collapsedFraction
        )
    } else {
        collapsedHeight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(appBarHeight)
        ) {
            val logoScale = lerp(1f, 0.4f, collapsedFraction)

            // Container for logo and title that morphs from Column to Row-like
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                val baseLogoWidth = if (isLandscape) 140.dp else 180.dp
                val baseLogoHeight = if (isLandscape) 60.dp else 100.dp

                val logoSizeWidth = baseLogoWidth * logoScale
                val logoSizeHeight = baseLogoHeight * logoScale

                val logoOffsetX = lerp(0.dp, if (isLandscape) (-60).dp else (-80).dp, collapsedFraction)
                val titleOffsetX = lerp(0.dp, if (isLandscape) 40.dp else 30.dp, collapsedFraction)
                val titleOffsetY = lerp(if (isLandscape) 30.dp else 60.dp, 0.dp, collapsedFraction)

                if (showLogo) {
                    Image(
                        painter = painterResource(Res.drawable.dr_icon_read_quran),
                        contentDescription = null,
                        modifier = Modifier
                            .size(logoSizeWidth, logoSizeHeight)
                            .offset(x = logoOffsetX, y = lerp(if (isLandscape) (-10).dp else (-20).dp, 0.dp, collapsedFraction)),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(colorScheme.primary)
                    )
                }

                Text(
                    text = title,
                    style = if (collapsedFraction > 0.5f) {
                        if (isLandscape) MaterialTheme.typography.labelMedium.merge(tightTextStyle)
                        else MaterialTheme.typography.titleMedium.merge(tightTextStyle)
                    } else {
                        if (isLandscape) MaterialTheme.typography.labelSmall.merge(tightTextStyle)
                        else MaterialTheme.typography.titleSmall.merge(tightTextStyle)
                    },
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(x = titleOffsetX, y = titleOffsetY),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_chevron_left),
                        contentDescription = stringResource(Res.string.strLabelBack),
                        tint = colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onSettingsClick != null) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_settings),
                            contentDescription = stringResource(Res.string.strTitleSettings),
                            tint = colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
