package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.ChapterIcon
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.ChapterVerseNavigator
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_quran_wallpaper
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.viewModels.RecitationPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExtendedThumbnail(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val headerShape = RoundedCornerShape(32.dp)
    val viewModel = viewModel { RecitationPlayerViewModel() }

    var showChapterVerseNavigator by rememberSaveable { mutableStateOf(false) }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(verse.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            viewModel.repository.getChapterName(verse.chapterNo)
        }
    }

    // The card keeps the wallpaper in both themes; only the scrim over it flips, so the artwork stays
    // a faint watermark behind text that is dark on light and light on dark.
    val isDark = ThemeUtils.observeDarkTheme()
    val scrim = if (isDark) Color.Black else colorScheme.surface
    val contentColor = playerContentColor()

    BoxWithConstraints(
        modifier = modifier
            .clip(headerShape)
            .background(if (isDark) Color(0xFF10151C) else colorScheme.surface)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.08f),
                shape = headerShape
            )
    ) {
        Image(
            painter = painterResource(Res.drawable.dr_quran_wallpaper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )

        // Deep tint
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scrim.copy(alpha = if (isDark) 0.78f else 0.86f),
                            scrim.copy(alpha = if (isDark) 0.55f else 0.74f),
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            scrim.copy(alpha = 0.28f),
                            Color.Transparent,
                            scrim.copy(alpha = 0.22f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )

        val smallestAxis = minOf(maxWidth, maxHeight)
        val compact = smallestAxis <= 190.dp
        val medium = smallestAxis <= 250.dp

        val horizontalPadding: Dp = when {
            compact -> 14.dp
            medium -> 18.dp
            else -> 24.dp
        }
        val verticalPadding: Dp = when {
            compact -> 14.dp
            medium -> 18.dp
            else -> 28.dp
        }
        val chapterIconInset: Dp = when {
            compact -> 10.dp
            medium -> 16.dp
            else -> 24.dp
        }
        val chapterIconBottomInset: Dp = when {
            compact -> 4.dp
            else -> 8.dp
        }

        val chapterIconSize = (smallestAxis.value * 0.2f)
            .coerceIn(44f, 72f)
            .sp

        val titleStyle = when {
            compact -> MaterialTheme.typography.titleMedium
            medium -> MaterialTheme.typography.titleLarge
            else -> MaterialTheme.typography.headlineSmall
        }
        val titleToBadgeGap: Dp = if (compact) 4.dp else 6.dp
        val iconToTitleGap: Dp = when {
            compact -> 10.dp
            medium -> 14.dp
            else -> 22.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(headerShape)
                    .background(contentColor.copy(alpha = 0.05f))
                    .border(1.dp, contentColor.copy(alpha = 0.10f), headerShape)
            ) {
                ChapterIcon(
                    modifier = Modifier.padding(
                        start = chapterIconInset,
                        end = chapterIconInset,
                        top = chapterIconInset,
                        bottom = chapterIconBottomInset,
                    ),
                    chapterNo = verse.chapterNo,
                    fontSize = chapterIconSize,
                    color = playerContentColor(),
                )
            }

            Spacer(Modifier.height(iconToTitleGap))

            Text(
                text = stringResource(Res.string.strLabelSurah, chapterName),
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = playerContentColor(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(titleToBadgeGap))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(contentColor.copy(alpha = 0.1f))
                    .border(1.dp, contentColor.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .clickable {
                        showChapterVerseNavigator = true
                    }
                    .padding(
                        horizontal = 10.dp,
                        vertical = 6.dp,
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.strLabelVerseNo, verse.verseNo),
                        color = contentColor.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        painterResource(Res.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    ChapterVerseNavigator(
        isOpen = showChapterVerseNavigator,
        onDismiss = { showChapterVerseNavigator = false },
        selectedChapterNo = verse.chapterNo,
        selectedVerseNos = setOf(verse.verseNo),
        onVerseSelected = { chapterNo, verseNo ->
            viewModel.controller.start(ChapterVersePair(chapterNo, verseNo))
        },
    )
}


@Composable
fun MiniPlayerThumbnail(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val isDark = ThemeUtils.observeDarkTheme()
    val scrim = if (isDark) Color.Black else colorScheme.surface

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .background(if (isDark) Color(0xFF11161D) else colorScheme.surface)
            .border(
                width = 1.dp,
                color = playerContentColor().copy(alpha = 0.08f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.dr_quran_wallpaper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.3f),
                            scrim.copy(alpha = if (isDark) 0.38f else 0.62f),
                            scrim.copy(alpha = if (isDark) 0.68f else 0.82f)
                        )
                    )
                )
        )

        ChapterIcon(
            modifier = Modifier.padding(top = 4.dp),
            chapterNo = verse.chapterNo,
            fontSize = 24.sp,
            color = playerContentColor(),
            withPrefix = false
        )
    }
}
