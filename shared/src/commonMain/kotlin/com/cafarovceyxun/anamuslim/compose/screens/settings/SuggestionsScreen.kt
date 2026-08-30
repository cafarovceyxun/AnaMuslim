package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCard
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCardAction
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCardStyle
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.settings.SuggestionSubmitSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.suggestionCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.suggestionStatusLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.suggestionSubmissionStatusLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.strLabelAll
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strTitleFailed
import com.cafarovceyxun.anamuslim.resources.suggestionsAdminNoteLabel
import com.cafarovceyxun.anamuslim.resources.suggestionsEmpty
import com.cafarovceyxun.anamuslim.resources.suggestionsMine
import com.cafarovceyxun.anamuslim.resources.suggestionsMineEmpty
import com.cafarovceyxun.anamuslim.resources.suggestionsPrivacyNote
import com.cafarovceyxun.anamuslim.resources.suggestionsSectionDone
import com.cafarovceyxun.anamuslim.resources.suggestionsSectionOpen
import com.cafarovceyxun.anamuslim.resources.suggestionsSortNewest
import com.cafarovceyxun.anamuslim.resources.suggestionsSortPopular
import com.cafarovceyxun.anamuslim.resources.suggestionsSubmit
import com.cafarovceyxun.anamuslim.resources.suggestionsTitle
import com.cafarovceyxun.anamuslim.resources.suggestionsVoteAction
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionCategory
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionStatus
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionSubmissionStatus
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionTicket
import com.cafarovceyxun.anamuslim.viewModels.SuggestionSort
import com.cafarovceyxun.anamuslim.viewModels.SuggestionsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * İstifadəçi təklifləri: təsdiqlənmiş siyahı, səsvermə və göndərmə.
 *
 * Siyahıda yalnız moderasiyadan keçmiş təkliflər var (`suggestions` cədvəli) — növbədəki mətnlər
 * heç kimə görünmür. «Mənim təkliflərim» tabı isə bu cihazdan göndərilənlərin statusudur; onu
 * cihazdakı qəbzlər açır, serverdə istifadəçi kimliyi saxlanmır.
 */
@Composable
fun SuggestionsScreen() {
    val viewModel = viewModel { SuggestionsViewModel() }

    val suggestions by viewModel.suggestions.collectAsState()
    val myTickets by viewModel.myTickets.collectAsState()
    val votedIds by viewModel.votedIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()

    var showMine by rememberSaveable { mutableStateOf(false) }
    var showSubmitSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    val visible = remember(suggestions, sort, categoryFilter) {
        suggestions
            .filter { categoryFilter == null || it.category == categoryFilter }
            .let { list ->
                when (sort) {
                    SuggestionSort.Popular -> list
                    SuggestionSort.Newest -> list.sortedByDescending { it.created_at.orEmpty() }
                }
            }
    }

    // «Əlavə olunub» artıq səs verilən bir şey deyil — hazır iş qalan təkliflərlə eyni siyahıda
    // yarışmasın deyə öz bölməsinə ayrılır və aşağı düşür.
    val openItems = remember(visible) { visible.filter { it.status != SuggestionStatus.DONE } }
    val doneItems = remember(visible) { visible.filter { it.status == SuggestionStatus.DONE } }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(colorScheme.surfaceContainer)) {
                AppBar(
                    title = stringResource(Res.string.suggestionsTitle),
                    shadowElevation = 0.dp,
                    actions = {
                        IconButton(
                            painter = painterResource(Res.drawable.dr_icon_refresh),
                        ) { viewModel.refresh() }
                    },
                )

                ModeRow(
                    showMine = showMine,
                    sort = sort,
                    onSelectSort = {
                        showMine = false
                        viewModel.setSort(it)
                    },
                    onSelectMine = { showMine = true },
                )

                if (!showMine) {
                    CategoryRow(
                        selected = categoryFilter,
                        countOf = { category ->
                            if (category == null) suggestions.size
                            else suggestions.count { it.category == category }
                        },
                        onSelect = { viewModel.setCategoryFilter(it) },
                    )
                }

                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSubmitSheet = true },
                modifier = Modifier.padding(bottom = mainBottomNavigationOuterHeight()),
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_feature),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                text = {
                    Text(
                        text = stringResource(Res.string.suggestionsSubmit),
                        style = typography.labelLarge,
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                isLoading && suggestions.isEmpty() && myTickets.isEmpty() -> Loader(fill = true)

                error != null && suggestions.isEmpty() -> MessageCard(
                    icon = Res.drawable.dr_icon_info,
                    title = stringResource(Res.string.strTitleFailed),
                    message = error.orEmpty(),
                    style = MessageCardStyle.Error,
                    primaryAction = MessageCardAction(
                        textRes = Res.string.strLabelRetry,
                        onClick = { viewModel.refresh() },
                    ),
                )

                showMine && myTickets.isEmpty() -> MessageCard(
                    icon = Res.drawable.dr_icon_info,
                    message = stringResource(Res.string.suggestionsMineEmpty),
                    style = MessageCardStyle.Info,
                )

                !showMine && visible.isEmpty() -> MessageCard(
                    icon = Res.drawable.dr_icon_feature,
                    message = stringResource(Res.string.suggestionsEmpty),
                    style = MessageCardStyle.Info,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = mainBottomNavigationOuterHeight() + 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showMine) {
                        items(myTickets, key = { it.ticket }) { ticket ->
                            MySuggestionCard(ticket)
                        }
                    } else {
                        if (openItems.isNotEmpty()) {
                            item(key = "header-open") {
                                SectionHeader(stringResource(Res.string.suggestionsSectionOpen))
                            }

                            items(openItems, key = { it.id }) { suggestion ->
                                SuggestionCard(
                                    suggestion = suggestion,
                                    voted = suggestion.id in votedIds,
                                    onVote = { viewModel.toggleVote(suggestion) },
                                )
                            }
                        }

                        if (doneItems.isNotEmpty()) {
                            item(key = "header-done") {
                                SectionHeader(stringResource(Res.string.suggestionsSectionDone))
                            }

                            items(doneItems, key = { it.id }) { suggestion ->
                                SuggestionCard(
                                    suggestion = suggestion,
                                    voted = suggestion.id in votedIds,
                                    // Hazır işə səs vermək mənasızdır: sayğac qalır, düymə yox.
                                    onVote = null,
                                )
                            }
                        }

                        item {
                            Text(
                                text = stringResource(Res.string.suggestionsPrivacyNote),
                                style = typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    SuggestionSubmitSheet(
        isOpen = showSubmitSheet,
        viewModel = viewModel,
        onDismiss = { showSubmitSheet = false },
    )
}

@Composable
private fun ModeRow(
    showMine: Boolean,
    sort: SuggestionSort,
    onSelectSort: (SuggestionSort) -> Unit,
    onSelectMine: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(
            selected = !showMine && sort == SuggestionSort.Popular,
            onClick = { onSelectSort(SuggestionSort.Popular) },
            label = { Text(stringResource(Res.string.suggestionsSortPopular), style = typography.labelMedium) },
        )

        Chip(
            selected = !showMine && sort == SuggestionSort.Newest,
            onClick = { onSelectSort(SuggestionSort.Newest) },
            label = { Text(stringResource(Res.string.suggestionsSortNewest), style = typography.labelMedium) },
        )

        Chip(
            selected = showMine,
            onClick = onSelectMine,
            label = { Text(stringResource(Res.string.suggestionsMine), style = typography.labelMedium) },
        )
    }
}

@Composable
private fun CategoryRow(
    selected: String?,
    countOf: (String?) -> Int,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val options = listOf<String?>(null) + SuggestionCategory.ALL

        options.forEach { category ->
            val label = if (category == null) {
                stringResource(Res.string.strLabelAll)
            } else {
                suggestionCategoryLabel(category)
            }
            val count = countOf(category)

            Chip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = {
                    Text(
                        text = if (count > 0) "$label $count" else label,
                        style = typography.labelMedium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: Suggestion,
    voted: Boolean,
    /** `null` → səs düyməsi əvəzinə sadəcə say (bax «Əlavə olunanlar» bölməsi). */
    onVote: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        VoteButton(count = suggestion.vote_count, voted = voted, onClick = onVote)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    text = suggestionCategoryLabel(suggestion.category),
                    color = colorScheme.secondary,
                )

                if (suggestion.status != SuggestionStatus.OPEN) {
                    Spacer(Modifier.width(6.dp))
                    Badge(
                        text = suggestionStatusLabel(suggestion.status),
                        color = if (suggestion.status == SuggestionStatus.DONE) {
                            colorScheme.primary
                        } else {
                            colorScheme.tertiary
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = suggestion.body,
                style = typography.bodyMedium.withContentDirection(),
                color = colorScheme.onSurface.alpha(0.9f),
            )
        }
    }
}

@Composable
private fun VoteButton(count: Int, voted: Boolean, onClick: (() -> Unit)?) {
    val done = onClick == null
    val filled = voted && !done
    val tint = if (filled) colorScheme.onPrimary else colorScheme.primary

    Surface(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (filled) colorScheme.primary else colorScheme.primary.alpha(0.1f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(
                    if (done) Res.drawable.dr_icon_check else Res.drawable.ic_arrow_up
                ),
                contentDescription = if (done) null else stringResource(Res.string.suggestionsVoteAction),
                tint = tint,
                modifier = Modifier.size(18.dp),
            )

            Text(
                text = count.toString(),
                style = typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
    }
}

@Composable
private fun MySuggestionCard(ticket: SuggestionTicket) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(
                text = suggestionCategoryLabel(ticket.category),
                color = colorScheme.secondary,
            )

            Spacer(Modifier.width(6.dp))

            Badge(
                text = suggestionSubmissionStatusLabel(ticket.status),
                color = when (ticket.status) {
                    SuggestionSubmissionStatus.APPROVED -> colorScheme.primary
                    SuggestionSubmissionStatus.REJECTED -> colorScheme.error
                    else -> colorScheme.tertiary
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = ticket.body,
            style = typography.bodyMedium.withContentDirection(),
            color = colorScheme.onSurface.alpha(0.9f),
        )

        ticket.admin_note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(Res.string.suggestionsAdminNoteLabel),
                style = typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant,
            )

            Text(
                text = note,
                style = typography.bodySmall.withContentDirection(),
                color = colorScheme.onSurface.alpha(0.85f),
            )
        }
    }
}

/** Siyahını «Təklif olunub» və «Əlavə olunanlar» deyə ikiyə bölən başlıq. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun Badge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.alpha(0.12f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}
