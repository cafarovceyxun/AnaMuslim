package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.animation.animateContentSize
import com.cafarovceyxun.anamuslim.resources.dr_icon_eye
import com.cafarovceyxun.anamuslim.resources.suggestionsEditNote
import com.cafarovceyxun.anamuslim.resources.suggestionsNoteLabel
import com.cafarovceyxun.anamuslim.resources.suggestionsViews
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.suggestionsImageFailed
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.suggestionsAddMedia
import com.cafarovceyxun.anamuslim.resources.suggestionsMediaHint
import com.cafarovceyxun.anamuslim.resources.suggestionsMediaTooLarge
import com.cafarovceyxun.anamuslim.resources.suggestionsVideoTooLong
import com.cafarovceyxun.anamuslim.utils.app.MediaPickResult
import com.cafarovceyxun.anamuslim.utils.app.PickedMedia
import com.cafarovceyxun.anamuslim.utils.app.rememberMediaPicker
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.layout.ContentScale
import com.cafarovceyxun.anamuslim.utils.app.rememberRemoteImage
import com.cafarovceyxun.anamuslim.resources.suggestionsImageUploading
import com.cafarovceyxun.anamuslim.resources.suggestionsRemoveImage
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
import androidx.compose.ui.graphics.Color
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
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.settings.suggestionCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.suggestionStatusLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.suggestionSubmissionStatusLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.strLabelAll
import com.cafarovceyxun.anamuslim.resources.strLabelApprove
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelReject
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.save
import com.cafarovceyxun.anamuslim.resources.strTitleFailed
import com.cafarovceyxun.anamuslim.resources.suggestionsAdminNoteLabel
import com.cafarovceyxun.anamuslim.resources.suggestionsBodyLabel
import com.cafarovceyxun.anamuslim.resources.suggestionsDeleteConfirm
import com.cafarovceyxun.anamuslim.resources.suggestionsEditTitle
import com.cafarovceyxun.anamuslim.resources.suggestionsManagementTitle
import com.cafarovceyxun.anamuslim.resources.suggestionsPublishedEmpty
import com.cafarovceyxun.anamuslim.resources.suggestionsPublishedTab
import com.cafarovceyxun.anamuslim.resources.suggestionsQueueEmpty
import com.cafarovceyxun.anamuslim.resources.suggestionsQueueTab
import com.cafarovceyxun.anamuslim.resources.suggestionsRejectConfirm
import com.cafarovceyxun.anamuslim.resources.suggestionsSearchHint
import com.cafarovceyxun.anamuslim.resources.suggestionsSectionDone
import com.cafarovceyxun.anamuslim.resources.suggestionsSectionOpen
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionCategory
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionStatus
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionSubmissionRow
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionSubmissionStatus
import com.cafarovceyxun.anamuslim.viewModels.SuggestionsManagementViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Təkliflərin moderasiya paneli — Ayarlar → İdarəetmə.
 *
 * İki tab: **növbə** (`suggestion_submissions`, istifadəçinin yazdığı, hələ heç kimə görünməyən
 * mətnlər) və **yayımlanan** (`suggestions`, təsdiqdən sonra hamının gördüyü siyahı). Təsdiq
 * bazadakı trigger ilə köçürülür; təsdiqi geri almaq yayımlanan sətri və onun səslərini silir.
 */
@Composable
fun SuggestionsManagementScreen() {
    val viewModel = viewModel { SuggestionsManagementViewModel() }

    val submissions by viewModel.submissions.collectAsState()
    val published by viewModel.published.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showPublished by remember { mutableStateOf(false) }
    val uploadingFor by viewModel.uploadingFor.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    val searchedQueue = remember(submissions, searchQuery) {
        submissions.filter { searchQuery.isBlank() || it.body.contains(searchQuery, ignoreCase = true) }
    }

    val visibleQueue = remember(searchedQueue, statusFilter) {
        if (statusFilter == SuggestionsManagementViewModel.FILTER_ALL) searchedQueue
        else searchedQueue.filter { it.status == statusFilter }
    }

    val visiblePublished = remember(published, searchQuery) {
        published.filter { searchQuery.isBlank() || it.body.contains(searchQuery, ignoreCase = true) }
    }

    // İstifadəçi ekranındakı ilə eyni bölgü: hazır iş («Tamamlandı») artıq gözləyən təkliflərlə
    // bir siyahıda deyil, aşağıda «əlavə olunmuş funksiyalar» kimi durur.
    val publishedOpen = remember(visiblePublished) {
        visiblePublished.filter { it.status != SuggestionStatus.DONE }
    }
    val publishedDone = remember(visiblePublished) {
        visiblePublished.filter { it.status == SuggestionStatus.DONE }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(colorScheme.surfaceContainer)) {
                AppBar(
                    title = stringResource(Res.string.suggestionsManagementTitle),
                    shadowElevation = 0.dp,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    searchPlaceholder = stringResource(Res.string.suggestionsSearchHint),
                    actions = {
                        IconButton(
                            painter = painterResource(Res.drawable.dr_icon_refresh),
                        ) { viewModel.refresh() }
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip(
                        selected = !showPublished,
                        onClick = { showPublished = false },
                        label = {
                            Text(
                                text = "${stringResource(Res.string.suggestionsQueueTab)} ${submissions.size}",
                                style = typography.labelMedium,
                            )
                        },
                    )

                    Chip(
                        selected = showPublished,
                        onClick = { showPublished = true },
                        label = {
                            Text(
                                text = "${stringResource(Res.string.suggestionsPublishedTab)} ${published.size}",
                                style = typography.labelMedium,
                            )
                        },
                    )
                }

                if (!showPublished) {
                    StatusFilterRow(
                        selected = statusFilter,
                        countOf = { filter ->
                            if (filter == SuggestionsManagementViewModel.FILTER_ALL) searchedQueue.size
                            else searchedQueue.count { it.status == filter }
                        },
                        onSelect = { viewModel.setStatusFilter(it) },
                    )
                }

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
                isLoading && submissions.isEmpty() && published.isEmpty() -> Loader(fill = true)

                error != null -> MessageCard(
                    icon = Res.drawable.dr_icon_info,
                    title = stringResource(Res.string.strTitleFailed),
                    message = error.orEmpty(),
                    style = MessageCardStyle.Error,
                    primaryAction = MessageCardAction(
                        textRes = Res.string.strLabelRetry,
                        onClick = { viewModel.refresh() },
                    ),
                )

                showPublished && visiblePublished.isEmpty() -> MessageCard(
                    icon = Res.drawable.dr_icon_info,
                    message = stringResource(Res.string.suggestionsPublishedEmpty),
                    style = MessageCardStyle.Info,
                )

                !showPublished && visibleQueue.isEmpty() -> MessageCard(
                    icon = Res.drawable.dr_icon_info,
                    message = stringResource(Res.string.suggestionsQueueEmpty),
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
                    if (showPublished) {
                        if (publishedOpen.isNotEmpty()) {
                            item(key = "header-open") {
                                SectionHeader(
                                    text = stringResource(Res.string.suggestionsSectionOpen),
                                    count = publishedOpen.size,
                                )
                            }

                            items(publishedOpen, key = { it.id }) { suggestion ->
                                PublishedCard(
                                    suggestion = suggestion,
                                    uploading = uploadingFor == suggestion.id,
                                    onStatusChange = { viewModel.setPublishedStatus(suggestion, it) },
                                    onPickMedia = { viewModel.addMedia(suggestion, it) },
                                    onSaveNote = { viewModel.setNote(suggestion, it) },
                                    onRemoveMedia = { viewModel.removeMedia(suggestion, it) },
                                    onDelete = { viewModel.deletePublished(suggestion) },
                                )
                            }
                        }

                        if (publishedDone.isNotEmpty()) {
                            item(key = "header-done") {
                                SectionHeader(
                                    text = stringResource(Res.string.suggestionsSectionDone),
                                    count = publishedDone.size,
                                )
                            }

                            items(publishedDone, key = { it.id }) { suggestion ->
                                PublishedCard(
                                    suggestion = suggestion,
                                    uploading = uploadingFor == suggestion.id,
                                    onStatusChange = { viewModel.setPublishedStatus(suggestion, it) },
                                    onPickMedia = { viewModel.addMedia(suggestion, it) },
                                    onSaveNote = { viewModel.setNote(suggestion, it) },
                                    onRemoveMedia = { viewModel.removeMedia(suggestion, it) },
                                    onDelete = { viewModel.deletePublished(suggestion) },
                                )
                            }
                        }
                    } else {
                        items(visibleQueue, key = { it.id }) { row ->
                            QueueCard(
                                row = row,
                                onApprove = { viewModel.approve(row) },
                                onReject = { viewModel.reject(row) },
                                onEdit = { body, category, note ->
                                    viewModel.editSubmission(row, body, category, note)
                                },
                                onDelete = { viewModel.deleteSubmission(row) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    selected: String,
    countOf: (String) -> Int,
    onSelect: (String) -> Unit,
) {
    val filters = listOf(SuggestionsManagementViewModel.FILTER_ALL) + SuggestionSubmissionStatus.ALL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            val label = if (filter == SuggestionsManagementViewModel.FILTER_ALL) {
                stringResource(Res.string.strLabelAll)
            } else {
                suggestionSubmissionStatusLabel(filter)
            }
            val count = countOf(filter)

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
private fun QueueCard(
    row: SuggestionSubmissionRow,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onEdit: (body: String, category: String, note: String?) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReject by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(
                text = suggestionCategoryLabel(row.category),
                color = colorScheme.secondary,
            )

            Spacer(Modifier.width(6.dp))

            StatusBadge(
                text = suggestionSubmissionStatusLabel(row.status),
                color = when (row.status) {
                    SuggestionSubmissionStatus.APPROVED -> colorScheme.primary
                    SuggestionSubmissionStatus.REJECTED -> colorScheme.error
                    else -> colorScheme.tertiary
                },
            )

            Spacer(Modifier.weight(1f))

            row.created_at?.displayDate()?.let {
                Text(
                    text = it,
                    style = typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

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
            text = row.body,
            style = typography.bodyMedium.withContentDirection(),
            color = colorScheme.onSurface.alpha(0.9f),
            maxLines = if (expanded) Int.MAX_VALUE else 3,
        )

        if (expanded) {
            Spacer(Modifier.height(10.dp))

            row.platform?.let { MetaRow("Platforma", it) }
            row.app_version?.takeIf { it.isNotBlank() }?.let { MetaRow("Versiya", it) }
            row.admin_note?.takeIf { it.isNotBlank() }?.let {
                MetaRow(stringResource(Res.string.suggestionsAdminNoteLabel), it)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (row.status != SuggestionSubmissionStatus.APPROVED) {
                    Button(
                        onClick = onApprove,
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

                if (row.status != SuggestionSubmissionStatus.REJECTED) {
                    OutlinedButton(
                        onClick = {
                            // Təsdiqlənmişi rədd etmək yayımlanan sətri silir — ona görə soruşulur.
                            if (row.status == SuggestionSubmissionStatus.APPROVED) {
                                confirmReject = true
                            } else {
                                onReject()
                            }
                        },
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
                    painter = painterResource(Res.drawable.dr_icon_edit),
                    small = true,
                ) { showEditor = true }

                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_delete),
                    contentDescription = stringResource(Res.string.strLabelDelete),
                    tint = colorScheme.error,
                    small = true,
                ) { confirmDelete = true }
            }
        }
    }

    SuggestionEditorDialog(
        isOpen = showEditor,
        initialBody = row.body,
        initialCategory = row.category,
        initialNote = row.admin_note.orEmpty(),
        onClose = { showEditor = false },
        onSave = onEdit,
    )

    ConfirmDialog(
        isOpen = confirmDelete,
        title = stringResource(Res.string.strLabelDelete),
        message = stringResource(Res.string.suggestionsDeleteConfirm),
        confirmText = stringResource(Res.string.strLabelDelete),
        onClose = { confirmDelete = false },
        onConfirm = onDelete,
    )

    ConfirmDialog(
        isOpen = confirmReject,
        title = stringResource(Res.string.strLabelReject),
        message = stringResource(Res.string.suggestionsRejectConfirm),
        confirmText = stringResource(Res.string.strLabelReject),
        onClose = { confirmReject = false },
        onConfirm = onReject,
    )
}

@Composable
private fun PublishedCard(
    suggestion: Suggestion,
    uploading: Boolean,
    onStatusChange: (String) -> Unit,
    onPickMedia: (PickedMedia) -> Unit,
    onRemoveMedia: (SuggestionMedia) -> Unit,
    onSaveNote: (String?) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }

    val tooLongMsg = stringResource(Res.string.suggestionsVideoTooLong)
    val tooLargeMsg = stringResource(Res.string.suggestionsMediaTooLarge)
    val failedMsg = stringResource(Res.string.suggestionsImageFailed)

    // Platformada seçici yoxdursa `null` gəlir və düymə ümumiyyətlə görünmür — basılıb heç nə
    // etməyən düymədən yaxşıdır (bax CLAUDE.md, «Provider/DI seam qaydası»).
    val pickMedia = rememberMediaPicker { result ->
        when (result) {
            is MediaPickResult.Picked -> onPickMedia(result.media)
            MediaPickResult.TooLong -> PlatformUtils.showLongToast(tooLongMsg)
            MediaPickResult.TooLarge -> PlatformUtils.showLongToast(tooLargeMsg)
            MediaPickResult.Failed -> PlatformUtils.showLongToast(failedMsg)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(
                text = suggestionCategoryLabel(suggestion.category),
                color = colorScheme.secondary,
            )

            Spacer(Modifier.width(6.dp))

            Icon(
                painter = painterResource(Res.drawable.ic_arrow_up),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )

            Text(
                text = suggestion.vote_count.toString(),
                style = typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
            )

            Spacer(Modifier.width(10.dp))

            Icon(
                painter = painterResource(Res.drawable.dr_icon_eye),
                contentDescription = stringResource(Res.string.suggestionsViews),
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )

            Text(
                text = " ${suggestion.view_count}",
                style = typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_delete),
                contentDescription = stringResource(Res.string.strLabelDelete),
                tint = colorScheme.error,
                small = true,
            ) { confirmDelete = true }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = suggestion.body,
            style = typography.bodyMedium.withContentDirection(),
            color = colorScheme.onSurface.alpha(0.9f),
        )

        Spacer(Modifier.height(12.dp))

        suggestion.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = typography.bodySmall.withContentDirection(),
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
            )

            Spacer(Modifier.height(8.dp))
        }

        // Hekayənin slaydları: sıra burada nə cürdürsə, istifadəçidə də o cür oynayır.
        if (suggestion.media.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestion.media.forEach { item ->
                    MediaThumbnail(item = item, onRemove = { onRemoveMedia(item) })
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                uploading -> {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = stringResource(Res.string.suggestionsImageUploading),
                        style = typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }

                pickMedia != null -> {
                    OutlinedButton(
                        onClick = pickMedia,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.suggestionsAddMedia),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    }

                    OutlinedButton(
                        onClick = { showNoteEditor = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.suggestionsEditNote),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(Res.string.suggestionsMediaHint),
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionStatus.ALL.forEach { status ->
                Chip(
                    selected = suggestion.status == status,
                    onClick = { onStatusChange(status) },
                    label = {
                        Text(
                            text = suggestionStatusLabel(status),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    },
                )
            }
        }
    }

    ConfirmDialog(
        isOpen = confirmDelete,
        title = stringResource(Res.string.strLabelDelete),
        message = stringResource(Res.string.suggestionsDeleteConfirm),
        confirmText = stringResource(Res.string.strLabelDelete),
        onClose = { confirmDelete = false },
        onConfirm = onDelete,
    )

    StoryNoteDialog(
        isOpen = showNoteEditor,
        initialNote = suggestion.note.orEmpty(),
        onClose = { showNoteEditor = false },
        onSave = onSaveNote,
    )
}

/** Hekayədə mətnin üstündə görünən qeyd — «bu funksiya haradadır» izahı. */
@Composable
private fun StoryNoteDialog(
    isOpen: Boolean,
    initialNote: String,
    onClose: () -> Unit,
    onSave: (String?) -> Unit,
) {
    if (!isOpen) return

    var note by remember(initialNote) { mutableStateOf(initialNote) }

    AlertDialog(
        isOpen = true,
        onClose = onClose,
        title = stringResource(Res.string.suggestionsEditNote),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                style = AlertDialogActionStyle.Default,
            ),
            AlertDialogAction(
                text = stringResource(Res.string.save),
                style = AlertDialogActionStyle.Primary,
                onClick = { onSave(note.takeIf { it.isNotBlank() }) },
            ),
        ),
        content = {
            FormTextField(
                value = note,
                onValueChange = { if (it.length <= 300) note = it },
                label = stringResource(Res.string.suggestionsNoteLabel),
                icon = Res.drawable.dr_icon_info,
                minLines = 2,
                maxLines = 4,
                supportingText = "${note.length}/300",
            )
        },
    )
}

/** Paneldəki kiçik slayd önizləməsi — videoda kadr yox, nişan göstərilir. */
@Composable
private fun MediaThumbnail(item: SuggestionMedia, onRemove: () -> Unit) {
    val preview = if (item.isVideo) null else rememberRemoteImage(item.url)

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(
                    if (item.isVideo) Res.drawable.ic_play else Res.drawable.dr_icon_feature
                ),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        // Ortaq IconButton `modifier` qəbul etmir, ona görə yerləşdirmə Box-dadır.
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
            IconButton(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.suggestionsRemoveImage),
                tint = colorScheme.onError,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.error,
                    contentColor = colorScheme.onError,
                ),
                small = true,
            ) { onRemove() }
        }
    }
}

@Composable
private fun SuggestionEditorDialog(
    isOpen: Boolean,
    initialBody: String,
    initialCategory: String,
    initialNote: String,
    onClose: () -> Unit,
    onSave: (body: String, category: String, note: String?) -> Unit,
) {
    if (!isOpen) return

    var body by remember(initialBody) { mutableStateOf(initialBody) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    AlertDialog(
        isOpen = true,
        onClose = onClose,
        title = stringResource(Res.string.suggestionsEditTitle),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                style = AlertDialogActionStyle.Default,
            ),
            AlertDialogAction(
                text = stringResource(Res.string.save),
                style = AlertDialogActionStyle.Primary,
                onClick = { onSave(body, category, note.takeIf { it.isNotBlank() }) },
            ),
        ),
        content = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SuggestionCategory.ALL.forEach { value ->
                        Chip(
                            selected = category == value,
                            onClick = { category = value },
                            label = {
                                Text(
                                    text = suggestionCategoryLabel(value),
                                    style = typography.labelMedium,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                FormTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = stringResource(Res.string.suggestionsBodyLabel),
                    icon = Res.drawable.dr_icon_edit,
                    minLines = 3,
                    maxLines = 8,
                )

                Spacer(Modifier.height(10.dp))

                FormTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(Res.string.suggestionsAdminNoteLabel),
                    icon = Res.drawable.dr_icon_info,
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    isOpen: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        isOpen = isOpen,
        onClose = onClose,
        title = title,
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                style = AlertDialogActionStyle.Default,
            ),
            AlertDialogAction(
                text = confirmText,
                style = AlertDialogActionStyle.Danger,
                onClick = onConfirm,
            ),
        ),
        content = { Text(text = message, style = typography.bodyMedium) },
    )
}

/** Yayımlananları «təklif olunub» və «əlavə olunanlar» deyə ikiyə bölən başlıq (sayı ilə). */
@Composable
private fun SectionHeader(text: String, count: Int) {
    Text(
        text = "$text  $count",
        style = typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun StatusBadge(text: String, color: Color) {
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

@Composable
private fun MetaRow(label: String, value: String) {
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

/** `2026-08-30T07:41:05Z` -> `30.08.2026`; gözlənilməyən format olduğu kimi göstərilir. */
private fun String.displayDate(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
}
