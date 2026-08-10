package com.cafarovceyxun.anamuslim.compose.components.homepage

import com.cafarovceyxun.anamuslim.resources.strTitleFeaturedQuran
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_quran_wallpaper
import com.cafarovceyxun.anamuslim.resources.strAyatulKursi
import com.cafarovceyxun.anamuslim.resources.strLabelFeatureQuranMiniInfo
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.resources.strLabelVerseWithChapNameWithBar
import com.cafarovceyxun.anamuslim.resources.strLabelVerses
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.getString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.tightTextStyle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.featuredQuranItems
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks

private data class FeaturedQuranModel(
    val chapterNo: Int,
    val verseRange: Pair<Int, Int>,
) {
    var title: String = ""
    var subtext: String = ""
}

@Composable
fun HomeSectionFeaturedReading() {
    val featuredItems by getFeaturedQuranModels()

    if (featuredItems == null) return

    Column(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeSectionHeader(
            icon = Res.drawable.dr_icon_feature,
            title = Res.string.strTitleFeaturedQuran,
            iconTint = null
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(featuredItems!!, key = { it.chapterNo.toString() + it.subtext }) {
                FeaturedQuranCard(it)
            }
        }
    }
}


@Composable
private fun FeaturedQuranCard(
    model: FeaturedQuranModel
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(110.dp)
            .clip(shapes.medium)
            .background(Color.Black)
            .clickable {
                ReaderUiHooks.openVerseRange?.invoke(
                    model.chapterNo,
                    model.verseRange.first,
                    model.verseRange.second,
                )
            },
    ) {
        Image(
            painter = painterResource(Res.drawable.dr_quran_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to colorScheme.primary.alpha(0.1f),
                        1f to Color.Black.alpha(0.9f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ).merge(tightTextStyle),
                color = Color.White,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = model.subtext,
                style = MaterialTheme.typography.labelSmall.merge(tightTextStyle),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun getFeaturedQuranModels(): State<List<FeaturedQuranModel>?> {
    val appLocale = LocalAppLocale.current

    // `appLocale` is the only key left: the Android context/configuration/resources keys existed
    // solely to re-run this on a locale change, which the locale itself already signals.
    return produceState<List<FeaturedQuranModel>?>(null, appLocale) {
        val repo = RepositoryProvider.quranRepository

        val models = featuredQuranItems.map { raw ->
            val (chapterNo, start, end) = parseItem(raw, repo)

            val chapterName = repo.getChapterName(chapterNo)

            FeaturedQuranModel(
                chapterNo,
                start to end,
            ).apply {
                if (start == 1 && end == repo.getChapterVerseCount(chapterNo) && !raw.contains(":")) {
                    title = getString(Res.string.strLabelSurah, chapterName)
                    subtext = getString(
                        Res.string.strLabelFeatureQuranMiniInfo,
                        chapterNo,
                        1,
                        end,
                    )
                } else if (start == end) {
                    if (chapterNo == 2 && start == 255) {
                        title = getString(Res.string.strAyatulKursi)
                        subtext = getString(
                            Res.string.strLabelVerseWithChapNameWithBar,
                            chapterName,
                            255,
                        )
                    } else {
                        title = getString(Res.string.strLabelSurah, chapterName)
                        subtext = getString(Res.string.strLabelVerseNo, start)
                    }
                } else {
                    title = getString(Res.string.strLabelSurah, chapterName)
                    subtext = getString(Res.string.strLabelVerses, start, end)
                }
            }
        }

        value = models
    }
}

private suspend fun parseItem(
    raw: String,
    repo: QuranRepository
): Triple<Int, Int, Int> {
    val colonIndex = raw.indexOf(':')

    if (colonIndex == -1) {
        val chapter = raw.toInt()
        return Triple(
            chapter,
            1,
            repo.getChapterVerseCount(chapter)
        )
    }

    val chapter = raw.substring(0, colonIndex).toInt()
    val versePart = raw.substring(colonIndex + 1)

    val dashIndex = versePart.indexOfFirst { it == '-' || it == '–' }

    return if (dashIndex == -1) {
        val verse = versePart.toInt()
        Triple(chapter, verse, verse)
    } else {
        val start = versePart.substring(0, dashIndex).toInt()
        val end = versePart.substring(dashIndex + 1).toInt()
        Triple(chapter, start, end)
    }
}
