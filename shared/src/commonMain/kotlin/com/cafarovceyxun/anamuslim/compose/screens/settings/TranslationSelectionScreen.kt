package com.cafarovceyxun.anamuslim.compose.screens.settings

import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.nItem
import com.cafarovceyxun.anamuslim.resources.nItems
import com.cafarovceyxun.anamuslim.resources.strDescCollapse
import com.cafarovceyxun.anamuslim.resources.strDescExpand
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strLabelAllChapters
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.strMsgDeleteAllHadithsConfirm
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.resources.strTitleHadithDelete
import com.cafarovceyxun.anamuslim.resources.strTitleTranslations
import com.cafarovceyxun.anamuslim.resources.textDownloading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.components.transls.TranslModel
import com.cafarovceyxun.anamuslim.components.transls.TranslationGroupModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ErrorMessageCard
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.ProgressOverlay
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.MessageDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.TranslationConfirm
import com.cafarovceyxun.anamuslim.compose.components.dialogs.HadithSyncConfirmDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.TranslationConfirmDialog
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import com.cafarovceyxun.anamuslim.viewModels.TranslationEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationUiEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationViewModel

@Composable
fun TranslationSelectionScreen() {
    val viewModel = viewModel { TranslationViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteHadithsConfirm by remember { mutableStateOf(false) }
    var showSyncHadithsConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<TranslationUiEvent.ShowMessage?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TranslationUiEvent.ShowMessage -> message = event
            }
        }
    }

    val visibleGroups = remember(uiState.translationGroups, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.translationGroups
        } else {
            uiState.translationGroups.map { group ->
                val filteredTransls = group.translations.filter { transl ->
                    // Kotlin Regex stands in for java.util.regex.Pattern: IGNORE_CASE ==
                    // CASE_INSENSITIVE, containsMatchIn == matcher(..).find(). DOTALL is dropped: the
                    // query is escaped (a literal), so it has no `.` for dot-all to affect — and
                    // RegexOption.DOT_MATCHES_ALL is JVM/Native-only, absent from the common stdlib.
                    val pattern = Regex(
                        StringUtils.escapeRegex(uiState.searchQuery),
                        RegexOption.IGNORE_CASE,
                    )

                    val bookInfo = transl.bookInfo
                    val bookName = bookInfo.bookName
                    val authorName = bookInfo.authorName
                    val langName = bookInfo.langName

                    pattern.containsMatchIn(bookName + authorName + langName)
                }

                group.copy(translations = ArrayList(filteredTransls))
            }.filter { it.translations.isNotEmpty() }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            AppBar(
                stringResource(Res.string.strTitleTranslations),
                searchPlaceholder = stringResource(Res.string.strHintSearch),
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = {
                    viewModel.onEvent(TranslationEvent.Search(it))
                },
                actions = {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_refresh),
                        onClick = { viewModel.onEvent(TranslationEvent.Refresh) }
                    )
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorMessageCard(
                    error = uiState.error!!,
                    onRetry = { viewModel.onEvent(TranslationEvent.Refresh) })

                else -> Content(
                    groups = visibleGroups,
                    selectedSlugs = uiState.selectedSlugs,
                    downloadStates = uiState.downloadStates,
                    hadithStatus = uiState.hadithSyncStatus,
                    isHadithDownloaded = uiState.isHadithDownloaded,
                    onSyncHadiths = { showSyncHadithsConfirm = true },
                    onCancelHadithSync = { viewModel.onEvent(TranslationEvent.CancelHadithSync) },
                    onDeleteHadiths = { showDeleteHadithsConfirm = true }
                )
            }

            ProgressOverlay(
                isUpdating = uiState.isForceUpdating,
                isUpdated = uiState.isForceUpdated
            )
        }
    }

    HadithSyncConfirmDialog(
        isOpen = showSyncHadithsConfirm,
        isUpdate = uiState.isHadithDownloaded,
        onClose = { showSyncHadithsConfirm = false },
        onConfirmed = {
            viewModel.onEvent(TranslationEvent.SyncHadiths)
            showSyncHadithsConfirm = false
        },
    )

    AlertDialog(
        isOpen = showDeleteHadithsConfirm,
        onClose = { showDeleteHadithsConfirm = false },
        // Was `strTitleBookmarkDeleteAll`, so deleting hadith books asked "delete all bookmarks?".
        title = stringResource(Res.string.strTitleHadithDelete),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = { showDeleteHadithsConfirm = false }
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    viewModel.onEvent(TranslationEvent.DeleteHadiths)
                    showDeleteHadithsConfirm = false
                }
            )
        )
    ) {
        Text(text = stringResource(Res.string.strMsgDeleteAllHadithsConfirm))
    }

    MessageDialog(
        title = message?.title,
        message = message?.message,
        onClose = { message = null },
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp
        )
    }
}

@Composable
private fun Content(
    groups: List<TranslationGroupModel>,
    selectedSlugs: Set<String>,
    downloadStates: Map<String, ResourceDownloadStatus>,
    hadithStatus: ResourceDownloadStatus,
    isHadithDownloaded: Boolean,
    onSyncHadiths: () -> Unit,
    onCancelHadithSync: () -> Unit,
    onDeleteHadiths: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp + readableWidthInset(),
            end = 16.dp + readableWidthInset(),
            top = 16.dp,
            bottom = mainBottomNavigationOuterHeight() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ListItemCategoryLabel(title = stringResource(Res.string.strTitleHadith))
        }

        item {
            HadithSyncItem(
                status = hadithStatus,
                isDownloaded = isHadithDownloaded,
                onSync = onSyncHadiths,
                onCancel = onCancelHadithSync,
                onDelete = onDeleteHadiths
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            ListItemCategoryLabel(title = stringResource(Res.string.strTitleTranslations))
        }

        items(groups, key = { it.langCode }) { group ->
            LanguageGroupCard(
                group = group,
                selectedSlugs = selectedSlugs,
                downloadStates = downloadStates,
            )
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun LanguageGroupCard(
    group: TranslationGroupModel,
    selectedSlugs: Set<String>,
    downloadStates: Map<String, ResourceDownloadStatus>,
) {
    val viewModel = viewModel { TranslationViewModel() }
    var confirm by remember { mutableStateOf<TranslationConfirm?>(null) }

    val hasSelection = remember(group.translations, selectedSlugs) {
        group.translations.any { selectedSlugs.contains(it.bookInfo.slug) }
    }

    val isAnyDownloading = downloadStates.values.any {
        it is ResourceDownloadStatus.Started || it is ResourceDownloadStatus.InProgress
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (group.isExpanded) 90f else 0f,
        animationSpec = tween(250),
        label = "chevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { viewModel.onEvent(TranslationEvent.ToggleGroup(group.langCode)) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.langName,
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (hasSelection) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = stringResource(
                                if (group.translations.size > 1) Res.string.nItems else Res.string.nItem,
                                group.translations.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = if (group.isExpanded) stringResource(Res.string.strDescCollapse) else stringResource(Res.string.strDescExpand),
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp
                    )

                    group.translations.forEachIndexed { index, translation ->
                        val slug = translation.bookInfo.slug
                        val downloadState = downloadStates[slug] ?: ResourceDownloadStatus.Idle

                        TranslationRow(
                            translation = translation,
                            isSelected = selectedSlugs.contains(slug),
                            downloadState = downloadState,
                            onCheckChanged = {
                                viewModel.onEvent(
                                    TranslationEvent.SelectionChanged(
                                        translation, it
                                    )
                                )
                            },
                            onDelete = { confirm = TranslationConfirm.Delete(translation) },
                            onForceUpdate = { confirm = TranslationConfirm.ForceUpdate(translation) },
                            onDownload = {
                                if (!isAnyDownloading) {
                                    confirm = TranslationConfirm.Download(translation)
                                }
                            },
                            onCancelDownload = {
                                confirm = TranslationConfirm.CancelDownload(translation)
                            }
                        )

                        if (index < group.translations.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }

    TranslationConfirmDialog(
        confirm = confirm,
        onClose = { confirm = null },
        onConfirmed = { action ->
            val slug = action.translation.bookInfo.slug
            when (action) {
                is TranslationConfirm.Download -> viewModel.onEvent(TranslationEvent.DownloadTranslation(slug))
                is TranslationConfirm.CancelDownload -> viewModel.onEvent(TranslationEvent.CancelDownload(slug))
                is TranslationConfirm.ForceUpdate -> viewModel.onEvent(TranslationEvent.ForceUpdateTranslation(slug))
                is TranslationConfirm.Delete -> viewModel.onEvent(TranslationEvent.DeleteTranslation(slug))
            }
            confirm = null
        },
    )
}

@Composable
private fun HadithSyncItem(
    status: ResourceDownloadStatus,
    isDownloaded: Boolean,
    onSync: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val isSyncing = status is ResourceDownloadStatus.InProgress || status is ResourceDownloadStatus.Started
    val progress = if (status is ResourceDownloadStatus.InProgress) status.progress / 100f else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant,
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_read_quran),
                                contentDescription = null,
                                tint = if (isDownloaded) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.strTitleHadith),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        if (isSyncing && status is ResourceDownloadStatus.InProgress) {
                            Text(
                                text = "${stringResource(Res.string.textDownloading)} ${status.progress}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = if (isDownloaded) stringResource(Res.string.strLabelDownloaded) else stringResource(Res.string.strLabelAllChapters),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (isDownloaded && !isSyncing) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_refresh),
                        contentDescription = stringResource(Res.string.strLabelUpdate),
                        onClick = onSync,
                        tint = colorScheme.primary,
                        small = true
                    )

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight(0.6f)
                            .width(1.dp),
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        contentDescription = stringResource(Res.string.strLabelDelete),
                        onClick = onDelete,
                        tint = colorScheme.error,
                        small = true
                    )
                } else if (!isDownloaded && !isSyncing) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_download),
                        contentDescription = null,
                        onClick = onSync,
                        tint = colorScheme.primary,
                        small = true
                    )
                } else if (isSyncing) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelCancel),
                        onClick = onCancel,
                        tint = colorScheme.onSurfaceVariant,
                        small = true
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationRow(
    translation: TranslModel,
    isSelected: Boolean,
    downloadState: ResourceDownloadStatus,
    onCheckChanged: (isChecked: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onForceUpdate: (String) -> Unit = {}
) {
    val bookInfo = translation.bookInfo

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colorScheme.primary),
                    onClick = {
                        if (translation.isDownloaded) {
                            onCheckChanged(!isSelected)
                        } else {
                            if (downloadState is ResourceDownloadStatus.Started || downloadState is ResourceDownloadStatus.InProgress) {
                                onCancelDownload()
                            } else {
                                onDownload()
                            }
                        }
                    }
                )
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (translation.isDownloaded) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onCheckChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colorScheme.primary,
                        uncheckedColor = colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(0.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (downloadState) {
                        is ResourceDownloadStatus.Started -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary
                            )
                        }

                        is ResourceDownloadStatus.InProgress -> {
                            CircularProgressIndicator(
                                progress = { downloadState.progress / 100f },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant
                            )
                        }

                        else -> {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = stringResource(Res.string.labelDownload),
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bookInfo.bookName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (downloadState is ResourceDownloadStatus.InProgress) {
                    Text(
                        text = stringResource(Res.string.textDownloading) + " ${downloadState.progress}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (downloadState is ResourceDownloadStatus.Started) {
                    Text(
                        text = stringResource(Res.string.textDownloading),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = bookInfo.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Light,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (translation.isDownloaded && !TranslUtils.isPrebuilt(bookInfo.slug)) {
            if (bookInfo.slug == "az") {
                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_refresh),
                    contentDescription = stringResource(Res.string.strLabelUpdate),
                    onClick = { onForceUpdate(bookInfo.slug) },
                    tint = colorScheme.primary,
                    small = true
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp),
                    color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )
            }

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_delete),
                contentDescription = stringResource(Res.string.strLabelDelete),
                onClick = { onDelete() },
                tint = colorScheme.error,
                small = true
            )
        } else if (!translation.isDownloaded) {
            if (downloadState is ResourceDownloadStatus.InProgress || downloadState is ResourceDownloadStatus.Started) {
                Box(
                    modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelCancel),
                        onClick = onCancelDownload,
                        tint = colorScheme.onSurfaceVariant,
                        small = true
                    )
                }
            }
        }
    }
}
