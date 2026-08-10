package com.cafarovceyxun.anamuslim.compose.components.homepage

import com.cafarovceyxun.anamuslim.resources.strTitleReadHistoryHadith
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.Res
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.formatDateTime
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity

@Composable
fun HomeSectionHadithReadHistory() {
    val actions = LocalHomeActions.current
    val userRepo = remember { RepositoryProvider.userRepository }
    val histories by userRepo.getHadithHistoriesFlow(Int.MAX_VALUE).collectAsState(emptyList())

    if (histories.isEmpty()) return

    HomeSectionContainer {
        HomeSectionHeader(
            icon = Res.drawable.dr_icon_history,
            title = Res.string.strTitleReadHistoryHadith,
            horizontalPadding = SECTION_CONTENT_PADDING,
            onViewAllClick = actions.onOpenHadithReadHistory,
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = SECTION_CONTENT_PADDING),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(histories, key = { it.id }) { history ->
                HadithHistoryCard(
                    history = history,
                    onOpen = {
                        actions.onOpenHadithItem(
                            history.volumeSlug,
                            history.bookSlug,
                            history.chapterSlug,
                            history.subChapterSlug,
                            history.title,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun HadithHistoryCard(
    history: HadithReadHistoryEntity,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .clip(shapes.medium)
            .border(0.8.dp, colorScheme.outlineVariant.alpha(0.25f), shapes.medium)
            .clickable(onClick = onOpen),
        color = colorScheme.surfaceVariant.alpha(0.2f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(
                text = history.title,
                style = typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorScheme.onSurface,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = formatDateTime(history.datetime, "d MMM, HH:mm"),
                style = typography.labelSmall.copy(fontSize = 10.sp),
                color = colorScheme.onSurface.alpha(0.4f),
                maxLines = 1
            )
        }
    }
}

