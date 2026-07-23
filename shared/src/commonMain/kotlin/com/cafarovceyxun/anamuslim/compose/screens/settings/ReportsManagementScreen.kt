package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.no_reports_found
import com.cafarovceyxun.anamuslim.resources.reports_management
import com.cafarovceyxun.anamuslim.resources.search_reports
import com.cafarovceyxun.anamuslim.resources.status_pending
import com.cafarovceyxun.anamuslim.resources.status_rejected
import com.cafarovceyxun.anamuslim.resources.status_resolved
import com.cafarovceyxun.anamuslim.resources.status_reviewing
import com.cafarovceyxun.anamuslim.resources.strLabelAll
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCopy
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelReject
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strMsgDeleteReportConfirm
import com.cafarovceyxun.anamuslim.resources.strTitleFailed
import com.cafarovceyxun.anamuslim.utils.supabase.VerseReport
import com.cafarovceyxun.anamuslim.viewModels.VerseReportStatus
import com.cafarovceyxun.anamuslim.viewModels.VerseReportViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Adminlərin gələn ayə bildirişlərini izlədiyi panel. Cədvəli yalnız daxil olmuş istifadəçilər
 * oxuya bilir, ona görə bu ekran ayarlarda "İdarəetmə" bölməsinin altındadır.
 */
@Composable
fun ReportsManagementScreen() {
    val viewModel = viewModel { VerseReportViewModel() }
    val reports by viewModel.reports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchReports() }

    val searched = remember(reports, searchQuery) {
        reports.filter { it.matchesSearch(searchQuery) }
    }

    val visible = remember(searched, filter) {
        if (filter == VerseReportViewModel.FILTER_ALL) searched
        else searched.filter { it.statusKey == filter }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(colorScheme.surfaceContainer)) {
                AppBar(
                    title = stringResource(Res.string.reports_management),
                    shadowElevation = 0.dp,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    searchPlaceholder = stringResource(Res.string.search_reports),
                    actions = {
                        IconButton(
                            painter = painterResource(Res.drawable.dr_icon_refresh),
                        ) { viewModel.fetchReports() }
                    },
                )

                ReportsFilterRow(
                    selected = filter,
                    countOf = { f ->
                        if (f == VerseReportViewModel.FILTER_ALL) searched.size
                        else searched.count { it.statusKey == f }
                    },
                    onSelect = { viewModel.setFilter(it) },
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
            when {
                isLoading -> Loader(fill = true)

                error != null -> MessageCard(
                    icon = Res.drawable.dr_icon_info,
                    title = stringResource(Res.string.strTitleFailed),
                    message = error.orEmpty(),
                    style = MessageCardStyle.Error,
                    primaryAction = MessageCardAction(
                        textRes = Res.string.strLabelRetry,
                        onClick = { viewModel.fetchReports() },
                    ),
                )

                visible.isEmpty() -> MessageCard(
                    icon = Res.drawable.dr_icon_info,
                    message = stringResource(Res.string.no_reports_found),
                    style = MessageCardStyle.Info,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = mainBottomNavigationOuterHeight() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.id ?: it.hashCode() }) { report ->
                        ReportCard(
                            report = report,
                            onStatusChange = { viewModel.updateStatus(report, it) },
                            onDelete = { viewModel.delete(report) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsFilterRow(
    selected: String,
    countOf: (String) -> Int,
    onSelect: (String) -> Unit,
) {
    val filters = listOf(
        VerseReportViewModel.FILTER_ALL,
        VerseReportStatus.PENDING,
        VerseReportStatus.REVIEWING,
        VerseReportStatus.RESOLVED,
        VerseReportStatus.REJECTED,
    )

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
            val label = statusLabel(filter)

            Chip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
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
private fun ReportCard(
    report: VerseReport,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit,
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
                width = 1.dp,
                color = colorScheme.outlineVariant.alpha(0.6f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.verse_key ?: "${report.chapter_no}:${report.verse_no}",
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                report.created_at?.displayDate()?.let {
                    Text(
                        text = it,
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            StatusBadge(report.statusKey)

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

        Spacer(Modifier.height(10.dp))

        Text(
            text = report.message,
            style = typography.bodyMedium,
            color = colorScheme.onSurface.alpha(0.85f),
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (expanded) {
            Spacer(Modifier.height(12.dp))

            MetadataRow("Ayə", "${report.chapter_no}:${report.verse_no}")
            report.slugs?.takeIf { it.isNotBlank() }?.let { MetadataRow("Tərcümələr", it) }
            report.app_version?.takeIf { it.isNotBlank() }?.let { MetadataRow("Versiya", it) }
            MetadataRow("Göndərən", report.user_id ?: "Anonim")

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (report.statusKey != VerseReportStatus.REVIEWING &&
                    report.statusKey != VerseReportStatus.RESOLVED
                ) {
                    OutlinedButton(
                        onClick = { onStatusChange(VerseReportStatus.REVIEWING) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = statusLabel(VerseReportStatus.REVIEWING),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }

                if (report.statusKey != VerseReportStatus.RESOLVED) {
                    Button(
                        onClick = { onStatusChange(VerseReportStatus.RESOLVED) },
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
                            text = statusLabel(VerseReportStatus.RESOLVED),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }

                if (report.statusKey != VerseReportStatus.REJECTED) {
                    OutlinedButton(
                        onClick = { onStatusChange(VerseReportStatus.REJECTED) },
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

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.strLabelCopy),
                style = typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { PlatformUtils.copyToClipboard(report.message) }
                    .padding(vertical = 6.dp),
            )
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
                text = stringResource(Res.string.strMsgDeleteReportConfirm),
                style = typography.bodyMedium,
            )
        },
    )
}

@Composable
private fun StatusBadge(statusKey: String) {
    val color = when (statusKey) {
        VerseReportStatus.RESOLVED -> colorScheme.primary
        VerseReportStatus.REJECTED -> colorScheme.error
        VerseReportStatus.REVIEWING -> colorScheme.secondary
        else -> colorScheme.tertiary
    }

    Surface(
        color = color.alpha(0.12f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = statusLabel(statusKey),
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
private fun statusLabel(status: String): String = when (status) {
    VerseReportStatus.PENDING -> stringResource(Res.string.status_pending)
    VerseReportStatus.REVIEWING -> stringResource(Res.string.status_reviewing)
    VerseReportStatus.RESOLVED -> stringResource(Res.string.status_resolved)
    VerseReportStatus.REJECTED -> stringResource(Res.string.status_rejected)
    else -> stringResource(Res.string.strLabelAll)
}

private val VerseReport.statusKey: String
    get() = status?.lowercase() ?: VerseReportStatus.PENDING

private fun VerseReport.matchesSearch(query: String): Boolean =
    query.isBlank() ||
            message.contains(query, ignoreCase = true) ||
            verse_key?.contains(query, ignoreCase = true) == true

/** `2026-07-20T02:50:00Z` -> `20.07.2026`; gözlənilməyən format olduğu kimi göstərilir. */
private fun String.displayDate(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
}
