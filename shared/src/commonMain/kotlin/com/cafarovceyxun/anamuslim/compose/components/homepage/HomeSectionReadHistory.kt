package com.cafarovceyxun.anamuslim.compose.components.homepage

import com.cafarovceyxun.anamuslim.resources.strTitleReadHistory
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.formatDateTime
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity

@Composable
fun HomeSectionReadHistory() {
    val actions = LocalHomeActions.current
    val userRepo = remember { RepositoryProvider.userRepository }
    val quranRepo = remember { RepositoryProvider.quranRepository }
    
    val histories by userRepo.getHistoriesFlow(Int.MAX_VALUE).collectAsState(emptyList())
    
    // Using produceState for a more robust handling of side-effects in composition
    val chapterNames by androidx.compose.runtime.produceState(initialValue = emptyMap<Int, String>(), key1 = histories) {
        if (histories.isNotEmpty()) {
            val names = quranRepo.getChapterNames(histories.map { it.chapterNo })
            value = names
        }
    }

    if (histories.isEmpty()) return

    HomeSectionContainer {
        HomeSectionHeader(
            icon = Res.drawable.dr_icon_history,
            title = Res.string.strTitleReadHistory,
            horizontalPadding = SECTION_CONTENT_PADDING,
            onViewAllClick = actions.onOpenReadHistory,
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = SECTION_CONTENT_PADDING),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(histories, key = { it.id }) { history ->
                val chapterName = chapterNames[history.chapterNo] ?: "Surə ${history.chapterNo}"
                QuranHistoryCard(
                    history = history,
                    chapterName = chapterName,
                    onOpen = { actions.onOpenReaderFromHistory(history) },
                )
            }
        }
    }
}

@Composable
private fun QuranHistoryCard(
    history: ReadHistoryEntity,
    chapterName: String,
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
                text = "$chapterName ${history.chapterNo}:${history.fromVerseNo}",
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
