package com.cafarovceyxun.anamuslim.compose.components.search

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.clear
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.msgClearSearchHistory
import com.cafarovceyxun.anamuslim.resources.searchTipArabic
import com.cafarovceyxun.anamuslim.resources.searchTipChapter
import com.cafarovceyxun.anamuslim.resources.searchTipDirectJuz
import com.cafarovceyxun.anamuslim.resources.searchTipDirectVerse
import com.cafarovceyxun.anamuslim.resources.searchTipTranslation
import com.cafarovceyxun.anamuslim.resources.searchTipsTitle
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelRemoveAll
import com.cafarovceyxun.anamuslim.resources.strMsgSearchHistoryDeleteAll
import com.cafarovceyxun.anamuslim.resources.titleRecentSearches
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.db.search.SearchHistoryEntry
import com.cafarovceyxun.anamuslim.viewModels.QuranSearchViewModel

/**
 * Recent-query chips.
 *
 * A scrolling [Row] rather than a `LazyRow`: the suggestion set is a handful of entries, and on wide
 * windows this strip shares one row with the results tabs, where it has to measure to its content so
 * the tabs sit right beside it. A `LazyRow` always expands to the width it is offered, which parked
 * the tabs against the far edge.
 */
@Composable
fun SearchHistorySuggestionStrip(
    suggestions: List<SearchHistoryEntry>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 4.dp,
        bottom = 10.dp,
    ),
) {
    if (suggestions.isEmpty()) return

    Surface(
        color = containerColor,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            suggestions.forEach { entry ->
                key(entry.id) {
                    SearchHistoryQueryChip(
                        text = entry.text,
                        onClick = { onSelect(entry.text) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryQueryChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.alpha(0.45f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun SearchEmptyScrollContent(
    viewModel: QuranSearchViewModel,
    modifier: Modifier = Modifier,
) {
    val history by viewModel.searchHistory.collectAsState()
    var showClearAllDialog by remember { mutableStateOf(false) }

    ClearSearchHistoryDialog(
        isOpen = showClearAllDialog,
        onDismiss = { showClearAllDialog = false },
        onConfirm = {
            viewModel.clearSearchHistory()
        },
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = readableWidthInset(),
            end = readableWidthInset(),
            bottom = mainBottomNavigationOuterHeight() + 12.dp,
        ),
    ) {
        item {
            SearchTipsCard(viewModel)
        }

        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.titleRecentSearches),
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onBackground.alpha(0.75f),
                    )

                    TextButton(
                        onClick = { showClearAllDialog = true },
                    ) {
                        Text(
                            stringResource(Res.string.clear),
                            style = typography.labelMedium
                        )
                    }
                }
            }

            items(
                items = history,
                key = { it.id },
            ) { entry ->
                SearchHistoryRow(
                    entry = entry,
                    onClick = {
                        viewModel.recordSearchQuery(entry.text)
                        viewModel.onQueryChange(entry.text)
                    },
                    onRemove = { viewModel.removeSearchHistory(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun ClearSearchHistoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        isOpen = isOpen,
        onClose = onDismiss,
        title = stringResource(Res.string.msgClearSearchHistory),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelRemoveAll),
                style = AlertDialogActionStyle.Danger,
                onClick = onConfirm,
            ),
        ),
    ) {
        Text(
            text = stringResource(Res.string.strMsgSearchHistoryDeleteAll),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurface,
        )
    }
}

@Composable
private fun SearchHistoryRow(
    entry: SearchHistoryEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.dr_icon_history),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(22.dp),
            tint = colorScheme.primary,
        )

        Text(
            text = entry.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelClose),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SearchTipsCard(
    viewModel: QuranSearchViewModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.searchTipsTitle),
                    style = typography.titleSmall,
                    color = colorScheme.onBackground.alpha(0.75f)
                )
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Column(
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                TipRow("2:255", stringResource(Res.string.searchTipDirectVerse)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("baqarah", stringResource(Res.string.searchTipChapter)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("30", stringResource(Res.string.searchTipDirectJuz)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("mercy", stringResource(Res.string.searchTipTranslation)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("الرحيم", stringResource(Res.string.searchTipArabic)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example, true)
                }
            }
        }
    }
}

@Composable
private fun TipRow(example: String, description: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(example)
            }
            .padding(vertical = 7.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(82.dp)
        ) {
            Text(
                text = example,
                color = MaterialTheme.colorScheme.primary,
                style = typography.labelMedium,
                modifier = Modifier
                    .background(colorScheme.background, shapes.small)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onBackground.alpha(0.75f)
        )
    }
}
