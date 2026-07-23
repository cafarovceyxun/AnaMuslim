package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCard
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCardAction
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCardStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavFabPadding
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.approve_selected
import com.cafarovceyxun.anamuslim.resources.delete_selected
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.edits_management
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.label_editor
import com.cafarovceyxun.anamuslim.resources.no_edits_found
import com.cafarovceyxun.anamuslim.resources.search_edits
import com.cafarovceyxun.anamuslim.resources.selectAll
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.status_approved
import com.cafarovceyxun.anamuslim.resources.status_pending
import com.cafarovceyxun.anamuslim.resources.status_rejected
import com.cafarovceyxun.anamuslim.resources.strLabelAll
import com.cafarovceyxun.anamuslim.resources.strLabelApprove
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelDeselectAll
import com.cafarovceyxun.anamuslim.resources.strLabelNavHadith
import com.cafarovceyxun.anamuslim.resources.strLabelQuranOnly
import com.cafarovceyxun.anamuslim.resources.strLabelReject
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strLabelSelectedCount
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strMsgApproveSelectedConfirm
import com.cafarovceyxun.anamuslim.resources.strMsgDeleteEditConfirm
import com.cafarovceyxun.anamuslim.resources.strMsgDeleteSelectedConfirm
import com.cafarovceyxun.anamuslim.resources.strTitleFailed
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.utils.supabase.HadithEdit
import com.cafarovceyxun.anamuslim.utils.supabase.QuranEdit
import com.cafarovceyxun.anamuslim.viewModels.EditsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val FILTER_ALL = "All"
private const val FILTER_PENDING = "Pending"
private const val FILTER_APPROVED = "Approved"
private const val FILTER_REJECTED = "Rejected"

private const val STATUS_PENDING = "pending"
private const val STATUS_APPROVED = "approved"
private const val STATUS_REJECTED = "rejected"

@Composable
fun EditsManagementScreen() {
    val viewModel = viewModel { EditsViewModel() }
    val quranEdits by viewModel.quranEdits.collectAsState()
    val hadithEdits by viewModel.hadithEdits.collectAsState()
    val selectedQuranIds by viewModel.selectedQuranEditIds.collectAsState()
    val selectedHadithIds by viewModel.selectedHadithEditIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val quranFilter by viewModel.quranFilter.collectAsState()
    val hadithFilter by viewModel.hadithFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val isQuranTab = pagerState.currentPage == 0

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) viewModel.fetchQuranEdits()
        else viewModel.fetchHadithEdits()
    }

    // Only the search query narrows both tabs; the status filter is per tab, so the counts shown on
    // the filter chips come from the search-matched list, not the filtered one.
    val quranSearched = remember(quranEdits, searchQuery) {
        quranEdits.filter { it.matchesSearch(searchQuery) }
    }
    val hadithSearched = remember(hadithEdits, searchQuery) {
        hadithEdits.filter { it.matchesSearch(searchQuery) }
    }

    val quranVisible = remember(quranSearched, quranFilter) {
        quranSearched.filter { edit ->
            when (quranFilter) {
                FILTER_PENDING -> !edit.is_approved
                FILTER_APPROVED -> edit.is_approved
                else -> true
            }
        }
    }
    val hadithVisible = remember(hadithSearched, hadithFilter) {
        hadithSearched.filter { edit ->
            when (hadithFilter) {
                FILTER_PENDING -> edit.statusKey == STATUS_PENDING
                FILTER_APPROVED -> edit.statusKey == STATUS_APPROVED
                FILTER_REJECTED -> edit.statusKey == STATUS_REJECTED
                else -> true
            }
        }
    }

    val selectionCount = if (isQuranTab) selectedQuranIds.size else selectedHadithIds.size
    val visibleCount = if (isQuranTab) quranVisible.size else hadithVisible.size
    val allSelected = visibleCount > 0 && selectionCount >= visibleCount

    fun refresh() {
        if (isQuranTab) viewModel.fetchQuranEdits() else viewModel.fetchHadithEdits()
    }

    fun clearSelection() {
        if (isQuranTab) viewModel.clearQuranSelection() else viewModel.clearHadithSelection()
    }

    fun toggleSelectAll() {
        if (allSelected) {
            clearSelection()
        } else if (isQuranTab) {
            viewModel.selectAllQuranEdits(quranVisible)
        } else {
            viewModel.selectAllHadithEdits(hadithVisible)
        }
    }

    var pendingBulkAction by remember { mutableStateOf<BulkAction?>(null) }

    Scaffold(
        containerColor = colorScheme.background,
        floatingActionButton = {
            BulkActionButtons(
                visible = visibleCount > 0,
                selectionCount = selectionCount,
                allSelected = allSelected,
                onToggleSelectAll = { toggleSelectAll() },
                onApprove = { pendingBulkAction = BulkAction.Approve },
                onDelete = { pendingBulkAction = BulkAction.Delete },
            )
        },
        topBar = {
            Column(modifier = Modifier.background(colorScheme.surfaceContainer)) {
                if (selectionCount > 0) {
                    SelectionBar(
                        count = selectionCount,
                        onClose = { clearSelection() },
                    )
                } else {
                    AppBar(
                        title = stringResource(Res.string.edits_management),
                        shadowElevation = 0.dp,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        searchPlaceholder = stringResource(Res.string.search_edits),
                        actions = {
                            IconButton(
                                painter = painterResource(Res.drawable.dr_icon_refresh),
                            ) { refresh() }
                        },
                    )
                }

                EditsTabs(
                    selectedIndex = pagerState.currentPage,
                    quranCount = quranSearched.size,
                    hadithCount = hadithSearched.size,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )

                EditsFilterRow(
                    filters = if (isQuranTab) {
                        listOf(FILTER_ALL, FILTER_PENDING, FILTER_APPROVED)
                    } else {
                        listOf(FILTER_ALL, FILTER_PENDING, FILTER_APPROVED, FILTER_REJECTED)
                    },
                    selected = if (isQuranTab) quranFilter else hadithFilter,
                    countOf = { filter ->
                        if (isQuranTab) {
                            when (filter) {
                                FILTER_PENDING -> quranSearched.count { !it.is_approved }
                                FILTER_APPROVED -> quranSearched.count { it.is_approved }
                                else -> quranSearched.size
                            }
                        } else {
                            when (filter) {
                                FILTER_ALL -> hadithSearched.size
                                else -> hadithSearched.count { it.statusKey == filter.lowercase() }
                            }
                        }
                    },
                    onSelect = { filter ->
                        if (isQuranTab) viewModel.setQuranFilter(filter)
                        else viewModel.setHadithFilter(filter)
                    },
                )

                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when {
                    isLoading -> Loader(fill = true)

                    error != null -> MessageCard(
                        icon = Res.drawable.dr_icon_info,
                        title = stringResource(Res.string.strTitleFailed),
                        message = error.orEmpty(),
                        style = MessageCardStyle.Error,
                        primaryAction = MessageCardAction(
                            textRes = Res.string.strLabelRetry,
                            onClick = { refresh() },
                        ),
                    )

                    page == 0 -> QuranEditsList(quranVisible, selectedQuranIds, viewModel)

                    else -> HadithEditsList(hadithVisible, selectedHadithIds, viewModel)
                }
            }
        }
    }

    val bulkAction = pendingBulkAction
    AlertDialog(
        isOpen = bulkAction != null,
        onClose = { pendingBulkAction = null },
        title = when (bulkAction) {
            BulkAction.Delete -> stringResource(Res.string.delete_selected)
            else -> stringResource(Res.string.approve_selected)
        },
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                style = AlertDialogActionStyle.Default,
            ),
            AlertDialogAction(
                text = when (bulkAction) {
                    BulkAction.Delete -> stringResource(Res.string.strLabelDelete)
                    else -> stringResource(Res.string.strLabelApprove)
                },
                style = when (bulkAction) {
                    BulkAction.Delete -> AlertDialogActionStyle.Danger
                    else -> AlertDialogActionStyle.Primary
                },
                onClick = {
                    when (bulkAction) {
                        BulkAction.Approve ->
                            if (isQuranTab) viewModel.approveSelectedQuranEdits()
                            else viewModel.updateSelectedHadithStatus(STATUS_APPROVED)

                        BulkAction.Delete ->
                            if (isQuranTab) viewModel.deleteSelectedQuranEdits()
                            else viewModel.deleteSelectedHadithEdits()

                        null -> Unit
                    }
                },
            ),
        ),
        content = {
            Text(
                text = when (bulkAction) {
                    BulkAction.Delete ->
                        stringResource(Res.string.strMsgDeleteSelectedConfirm, selectionCount)

                    else -> stringResource(Res.string.strMsgApproveSelectedConfirm, selectionCount)
                },
                style = typography.bodyMedium,
            )
        },
    )
}

private enum class BulkAction { Approve, Delete }

/**
 * Bulk controls sit above the app's floating bottom navigation, within thumb reach: select-all is
 * always offered, approve/delete appear once something is selected.
 */
@Composable
private fun BulkActionButtons(
    visible: Boolean,
    selectionCount: Int,
    allSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onApprove: () -> Unit,
    onDelete: () -> Unit,
) {
    if (!visible) return

    Column(
        modifier = Modifier.padding(bottom = mainBottomNavFabPadding()),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectionCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = onDelete,
                    containerColor = colorScheme.errorContainer,
                    contentColor = colorScheme.onErrorContainer,
                    shape = CircleShape,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        contentDescription = stringResource(Res.string.delete_selected),
                        modifier = Modifier.size(20.dp),
                    )
                }

                SmallFloatingActionButton(
                    onClick = onApprove,
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    shape = CircleShape,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_check),
                        contentDescription = stringResource(Res.string.approve_selected),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onToggleSelectAll,
            containerColor = if (allSelected) colorScheme.surfaceVariant else colorScheme.primary,
            contentColor = if (allSelected) colorScheme.onSurfaceVariant else colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(
                painter = painterResource(
                    if (allSelected) Res.drawable.dr_icon_close else Res.drawable.dr_icon_check_circle,
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (allSelected) stringResource(Res.string.strLabelDeselectAll)
                else stringResource(Res.string.selectAll),
                style = typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private fun QuranEdit.matchesSearch(query: String): Boolean =
    query.isBlank() ||
            new_text.contains(query, ignoreCase = true) ||
            editor_email.contains(query, ignoreCase = true)

private fun HadithEdit.matchesSearch(query: String): Boolean =
    query.isBlank() ||
            text_az?.contains(query, ignoreCase = true) == true ||
            text_ar?.contains(query, ignoreCase = true) == true ||
            editor_email?.contains(query, ignoreCase = true) == true

private val HadithEdit.statusKey: String
    get() = status?.lowercase() ?: STATUS_PENDING

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            painter = painterResource(Res.drawable.dr_icon_close),
            contentDescription = stringResource(Res.string.strLabelCancel),
        ) { onClose() }

        Text(
            text = stringResource(Res.string.strLabelSelectedCount, count),
            style = typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
    }
}

@Composable
private fun EditsTabs(
    selectedIndex: Int,
    quranCount: Int,
    hadithCount: Int,
    onSelect: (Int) -> Unit,
) {
    val tabs = listOf(
        stringResource(Res.string.strLabelQuranOnly) to quranCount,
        stringResource(Res.string.strLabelNavHadith) to hadithCount,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, (label, count) ->
            val selected = index == selectedIndex
            val container by animateColorAsState(
                if (selected) colorScheme.primary else Color.Transparent,
            )
            val content by animateColorAsState(
                if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(container)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = content.alpha(if (selected) 0.2f else 0.12f),
                        shape = CircleShape,
                    ) {
                        Text(
                            text = count.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = content,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditsFilterRow(
    filters: List<String>,
    selected: String,
    countOf: (String) -> Int,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            val count = countOf(filter)
            Chip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        text = if (count > 0) "${filterLabel(filter)} $count" else filterLabel(filter),
                        style = typography.labelMedium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun filterLabel(filter: String): String = when (filter) {
    FILTER_PENDING -> stringResource(Res.string.status_pending)
    FILTER_APPROVED -> stringResource(Res.string.status_approved)
    FILTER_REJECTED -> stringResource(Res.string.status_rejected)
    else -> stringResource(Res.string.strLabelAll)
}

@Composable
private fun QuranEditsList(
    edits: List<QuranEdit>,
    selectedIds: Set<Long>,
    viewModel: EditsViewModel,
) {
    if (edits.isEmpty()) {
        EmptyState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = mainBottomNavigationOuterHeight() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(edits, key = { it.id ?: it.hashCode() }) { edit ->
            EditCard(
                title = stringResource(Res.string.strLabelSurah, edit.chapter_no?.toString() ?: "?"),
                statusKey = if (edit.is_approved) STATUS_APPROVED else STATUS_PENDING,
                date = edit.created_at,
                isSelected = selectedIds.contains(edit.id),
                onToggleSelect = { edit.id?.let { viewModel.toggleQuranEditSelection(it) } },
                preview = edit.new_text,
                onApprove = if (edit.is_approved) null else ({ viewModel.approveQuranEdit(edit) }),
                onReject = null,
                onDelete = { viewModel.deleteQuranEdit(edit) },
            ) {
                MetadataRow(stringResource(Res.string.label_editor), edit.editor_email)
                edit.note?.takeIf { it.isNotBlank() }?.let {
                    MetadataRow(stringResource(Res.string.strTitleNote), it)
                }
            }
        }
    }
}

@Composable
private fun HadithEditsList(
    edits: List<HadithEdit>,
    selectedIds: Set<Long>,
    viewModel: EditsViewModel,
) {
    if (edits.isEmpty()) {
        EmptyState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = mainBottomNavigationOuterHeight() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(edits, key = { it.id ?: it.hashCode() }) { edit ->
            EditCard(
                title = "${stringResource(Res.string.hadith)} №${edit.hadith_no ?: "?"}",
                statusKey = edit.statusKey,
                date = edit.created_at,
                isSelected = selectedIds.contains(edit.id),
                onToggleSelect = { edit.id?.let { viewModel.toggleHadithEditSelection(it) } },
                preview = edit.text_az.orEmpty().ifBlank { edit.text_ar.orEmpty() },
                onApprove = if (edit.statusKey == STATUS_APPROVED) null
                else ({ viewModel.updateHadithStatus(edit, STATUS_APPROVED) }),
                onReject = if (edit.statusKey == STATUS_REJECTED) null
                else ({ viewModel.updateHadithStatus(edit, STATUS_REJECTED) }),
                onDelete = { viewModel.deleteHadithEdit(edit) },
            ) {
                edit.text_ar?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = typography.bodyMedium,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                MetadataRow(stringResource(Res.string.source), edit.source ?: "—")
                MetadataRow(stringResource(Res.string.label_editor), edit.editor_email ?: "—")
                edit.note?.takeIf { it.isNotBlank() }?.let {
                    MetadataRow(stringResource(Res.string.strTitleNote), it)
                }
            }
        }
    }
}

@Composable
private fun EditCard(
    title: String,
    statusKey: String,
    date: String?,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    preview: String,
    onApprove: (() -> Unit)?,
    onReject: (() -> Unit)?,
    onDelete: () -> Unit,
    details: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant.alpha(0.6f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionMarker(isSelected = isSelected, onClick = onToggleSelect)

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                date?.displayDate()?.let {
                    Text(
                        text = it,
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            StatusBadge(statusKey)

            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
                    .rotate(chevronRotation),
            )
        }

        if (preview.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = preview,
                style = typography.bodyMedium,
                color = colorScheme.onSurface.alpha(0.85f),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            details()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onApprove?.let { approve ->
                    Button(
                        onClick = approve,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_check),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.strLabelApprove),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }

                onReject?.let { reject ->
                    OutlinedButton(
                        onClick = reject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.strLabelReject),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }

                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_delete),
                    contentDescription = stringResource(Res.string.strLabelDelete),
                    tint = colorScheme.error,
                    small = true,
                ) { confirmDelete = true }
            }
        }
    }

    AlertDialog(
        isOpen = confirmDelete,
        onClose = { confirmDelete = false },
        title = stringResource(Res.string.strLabelDelete),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                style = AlertDialogActionStyle.Default,
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = onDelete,
            ),
        ),
        content = {
            Text(
                text = stringResource(Res.string.strMsgDeleteEditConfirm),
                style = typography.bodyMedium,
            )
        },
    )
}

/** A checkbox in all but name — the tap target is padded out to 40.dp around the 24.dp box. */
@Composable
private fun SelectionMarker(isSelected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (isSelected) colorScheme.primary else Color.Transparent,
    )
    val border by animateColorAsState(
        if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant.alpha(0.6f),
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(container)
                .border(2.dp, border, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    contentDescription = null,
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(statusKey: String) {
    val color = when (statusKey) {
        STATUS_APPROVED -> colorScheme.primary
        STATUS_REJECTED -> colorScheme.error
        else -> colorScheme.tertiary
    }
    val label = when (statusKey) {
        STATUS_APPROVED -> stringResource(Res.string.status_approved)
        STATUS_REJECTED -> stringResource(Res.string.status_rejected)
        else -> stringResource(Res.string.status_pending)
    }

    Surface(
        color = color.alpha(0.12f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = typography.labelSmall,
            color = colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyState() {
    MessageCard(
        icon = Res.drawable.dr_icon_info,
        message = stringResource(Res.string.no_edits_found),
        style = MessageCardStyle.Info,
    )
}

/** `2026-07-20T02:50:00Z` -> `20.07.2026`; anything unexpected is shown as-is. */
private fun String.displayDate(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
}
