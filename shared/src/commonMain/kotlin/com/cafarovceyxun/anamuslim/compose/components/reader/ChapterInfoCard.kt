package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_madina_old
import com.cafarovceyxun.anamuslim.resources.dr_makkah_old
import com.cafarovceyxun.anamuslim.resources.strChapInfoSeeMore
import com.cafarovceyxun.anamuslim.resources.strLabelOrder
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoRukus
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoVerses
import com.cafarovceyxun.anamuslim.resources.strTitleMadani
import com.cafarovceyxun.anamuslim.resources.strTitleMakki
import com.cafarovceyxun.anamuslim.compose.theme.LegacyColors
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.db.entities.quran.RevelationType
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Composable
fun ChapterInfoCard(chapterNo: Int) {
    val appLocale = LocalAppLocale.current

    val repository = remember { RepositoryProvider.quranRepository }
    var _swl by remember { mutableStateOf<SurahWithLocalizations?>(null) }

    LaunchedEffect(chapterNo) {
        _swl = withContext(Dispatchers.IO) {
            repository.getSurahWithLocalizations(chapterNo)
        }
    }

    val swl = _swl ?: return

    var expanded by remember(chapterNo) { mutableStateOf(false) }

    val isMeccan = swl.surah.revelationType == RevelationType.meccan
    val title = stringResource(Res.string.strLabelSurah, swl.getCurrentName())
    val revelationLabel =
        stringResource(if (isMeccan) Res.string.strTitleMakki else Res.string.strTitleMadani)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_info),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = title,
                        style = typography.titleSmall,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) -90f else 0f),
                    tint = colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .clickable {
                            ReaderUiHooks.openChapterInfo?.invoke(chapterNo)
                        }
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colorScheme.primary,
                        ) {
                            Text(
                                text = revelationLabel,
                                style = typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChapterInfoStatChip(
                                text = stringResource(Res.string.strTitleChapInfoVerses) + ": " +
                                        appLocale.numeralSystem.formatNumber(swl.surah.ayahCount),
                            )
                            ChapterInfoStatChip(
                                text = stringResource(Res.string.strTitleChapInfoRukus) + ": " +
                                        appLocale.numeralSystem.formatNumber(swl.surah.rukusCount),
                            )
                            ChapterInfoStatChip(
                                text = stringResource(Res.string.strLabelOrder) + ": " +
                                        appLocale.numeralSystem.formatNumber(swl.surah.revelationOrder),
                            )
                        }

                        Text(
                            text = stringResource(Res.string.strChapInfoSeeMore),
                            style = typography.bodySmall,
                            color = LegacyColors.text2(),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Image(
                        painter = painterResource(
                            if (isMeccan) Res.drawable.dr_makkah_old else Res.drawable.dr_madina_old,
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(72.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterInfoStatChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = colorScheme.background
    ) {
        Text(
            text = text,
            style = typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(6.dp),
        )
    }
}
