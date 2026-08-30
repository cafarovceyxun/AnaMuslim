package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
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
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.screens.hadith.QuranReferencePickerSheet
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dailyContentAddVerse
import com.cafarovceyxun.anamuslim.resources.dailyContentDeleteConfirm
import com.cafarovceyxun.anamuslim.resources.dailyContentEditRange
import com.cafarovceyxun.anamuslim.resources.dailyContentExcerptTitle
import com.cafarovceyxun.anamuslim.resources.dailyContentHistoryEmpty
import com.cafarovceyxun.anamuslim.resources.dailyContentHistoryTab
import com.cafarovceyxun.anamuslim.resources.dailyContentManagementTitle
import com.cafarovceyxun.anamuslim.resources.dailyContentMoveDown
import com.cafarovceyxun.anamuslim.resources.dailyContentMoveUp
import com.cafarovceyxun.anamuslim.resources.dailyContentQueueEmpty
import com.cafarovceyxun.anamuslim.resources.dailyContentQueueTab
import com.cafarovceyxun.anamuslim.resources.dailyContentRequeue
import com.cafarovceyxun.anamuslim.resources.dailyContentScheduleHint
import com.cafarovceyxun.anamuslim.resources.dailyContentToday
import com.cafarovceyxun.anamuslim.resources.dailyContentTomorrow
import com.cafarovceyxun.anamuslim.resources.dailyContentVerseCount
import com.cafarovceyxun.anamuslim.resources.dailyContentViewCount
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.ic_book_copy
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strTitleDailyHadith
import com.cafarovceyxun.anamuslim.resources.strTitleFailed
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentSchedule
import com.cafarovceyxun.anamuslim.viewModels.DailyContentManagementViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * «Günün ayəsi» növbəsinin admin paneli — Ayarlar → İdarəetmə.
 *
 * İki tab: **növbə** (bugündən sonra göstəriləcəklər) və **göstərilənlər** (keçmiş, oradan bir
 * toxunuşla yenidən növbəyə salınır). Növbə **günlərə görə qruplaşdırılır**: hər gün öz başlığı
 * altında beş yuvaya qədər element saxlayır, beşdən sonrakılar özləri növbəti günə keçir — sıra
 * elə bildiriş sırasıdır.
 *
 * Ayə seçimi hədis redaktorundakı naviqatorun **eynisidir** ([QuranReferencePickerSheet]); hədis isə
 * öz oxuma ekranından növbəyə düşür, çünki id istifadəçinin gördüyü şey deyil.
 */
@Composable
fun DailyContentManagementScreen() {
    val viewModel = viewModel { DailyContentManagementViewModel() }

    val queue by viewModel.queue.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()

    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showAddVerse by rememberSaveable { mutableStateOf(false) }

    var excerptFor by remember { mutableStateOf<DailyContent?>(null) }
    var rangeFor by remember { mutableStateOf<DailyContent?>(null) }
    var deleteCandidate by remember { mutableStateOf<DailyContent?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Yuvanın vaxtının keçib-keçmədiyi bununla ölçülür. Növbə yenilənəndə birlikdə təzələnir; saat
    // əqrəbini saniyəbəsaniyə izləmək lazım deyil, sərhəd gündə beş dəfə keçilir.
    val now = remember(queue) { currentEpochMillis() }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(colorScheme.surfaceContainer)) {
                AppBar(
                    title = stringResource(Res.string.dailyContentManagementTitle),
                    shadowElevation = 0.dp,
                    actions = {
                        IconButton(painter = painterResource(Res.drawable.dr_icon_refresh)) {
                            viewModel.refresh()
                        }
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip(
                        selected = !showHistory,
                        onClick = { showHistory = false },
                        label = {
                            Text(
                                text = "${stringResource(Res.string.dailyContentQueueTab)} · ${queue.size}",
                                style = typography.labelMedium,
                            )
                        },
                    )

                    Chip(
                        selected = showHistory,
                        onClick = { showHistory = true },
                        label = {
                            Text(
                                text = "${stringResource(Res.string.dailyContentHistoryTab)} · ${history.size}",
                                style = typography.labelMedium,
                            )
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 10.dp),
                    color = colorScheme.outlineVariant.alpha(0.4f),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && queue.isEmpty() && history.isEmpty()) {
                Loader(fill = true)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = mainBottomNavigationOuterHeight() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // ⚠️ Xəta siyahını **əvəz etmir**, üstündə banner kimi çıxır: əvvəl bir uğursuz
                    // yazma bütün növbəni ekrandan silirdi və «heç nə işləmir» kimi görünürdü.
                    error?.let { message ->
                        item(key = "error") {
                            MessageCard(
                                icon = Res.drawable.dr_icon_info,
                                title = stringResource(Res.string.strTitleFailed),
                                message = message,
                                style = MessageCardStyle.Error,
                                fillMaxSize = false,
                                showCard = true,
                                primaryAction = MessageCardAction(
                                    textRes = Res.string.strLabelRetry,
                                    onClick = { viewModel.refresh() },
                                ),
                            )
                        }
                    }

                    if (showHistory) {
                        historySection(
                            history = history,
                            enabled = !isSaving,
                            onRequeue = viewModel::requeue,
                        )
                    } else {
                        item(key = "add") {
                            AddSection(enabled = !isSaving, onAddVerse = { showAddVerse = true })
                        }

                        queueSection(
                            queue = queue,
                            now = now,
                            enabled = !isSaving,
                            onMoveUp = viewModel::moveUp,
                            onMoveDown = viewModel::moveDown,
                            onEditExcerpt = { excerptFor = it },
                            onEditRange = { rangeFor = it },
                            onDelete = { deleteCandidate = it },
                        )
                    }
                }
            }
        }
    }

    // Ayə seçimi hədis redaktorundakı **eyni** naviqatordur: surə siyahısı, aralıq sahəsi və
    // önizləmə. Ayrı bir «nömrə yaz» dialoqu iki fərqli ayə seçmə üsulu demək olardı.
    QuranReferencePickerSheet(
        isOpen = showAddVerse,
        onDismiss = { showAddVerse = false },
        onAdd = { insertion ->
            viewModel.addVerses(
                chapterNo = insertion.chapterNo,
                verseStart = insertion.fromVerse,
                verseEnd = insertion.toVerse.takeIf { it > insertion.fromVerse },
            )
        },
    )

    excerptFor?.let { item ->
        HadithExcerptDialog(
            item = item,
            onDismiss = { excerptFor = null },
            onConfirm = { excerptAz, excerptAr ->
                excerptFor = null
                viewModel.setExcerpt(item, excerptAz, excerptAr)
            },
        )
    }

    rangeFor?.let { item ->
        VerseRangeDialog(
            item = item,
            onDismiss = { rangeFor = null },
            onConfirm = { verseStart, verseEnd ->
                rangeFor = null
                viewModel.setVerseRange(item, verseStart, verseEnd)
            },
        )
    }

    // ⚠️ Silinəcək element `let` ilə **tutulur**, düymənin içində state-dən oxunmur:
    // `AlertDialogAction.dismissOnClick` defolt `true`-dur və komponent əvvəl `onClose()`, sonra
    // `onClick()` çağırır — yəni lambda işləyəndə `deleteCandidate` artıq null olurdu və silmə
    // heç vaxt işə düşmürdü. Kompilyator da, testlər də bunu tutmur.
    deleteCandidate?.let { candidate ->
        AlertDialog(
            isOpen = true,
            onClose = { deleteCandidate = null },
            title = stringResource(Res.string.dailyContentDeleteConfirm),
            actions = listOf(
                AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelDelete),
                    style = AlertDialogActionStyle.Danger,
                    onClick = { viewModel.delete(candidate) },
                ),
            ),
        )
    }
}

/** Növbə günlərə görə qruplaşdırılır — hər gün öz başlığı altında beş yuvaya qədər element. */
private fun LazyListScope.queueSection(
    queue: List<DailyContent>,
    now: Long,
    enabled: Boolean,
    onMoveUp: (DailyContent) -> Unit,
    onMoveDown: (DailyContent) -> Unit,
    onEditExcerpt: (DailyContent) -> Unit,
    onEditRange: (DailyContent) -> Unit,
    onDelete: (DailyContent) -> Unit,
) {
    if (queue.isEmpty()) {
        item(key = "queue-empty") {
            MessageCard(
                icon = Res.drawable.dr_icon_info,
                message = stringResource(Res.string.dailyContentQueueEmpty),
                style = MessageCardStyle.Info,
                fillMaxSize = false,
                showCard = true,
            )
        }

        return
    }

    queue.groupBy { it.date.orEmpty() }.forEach { (date, dayItems) ->
        item(key = "day-$date") {
            DayHeader(date = date, count = dayItems.size)
        }

        items(count = dayItems.size, key = { dayItems[it].id ?: 0L }) { indexInDay ->
            val item = dayItems[indexInDay]
            val index = queue.indexOfFirst { it.id == item.id }

            // Vaxtı keçmiş yuva (bu gün artıq bildirilmiş element) yerindən tərpənmir — düymələr
            // də sönük olsun ki, basıb «heç nə olmadı» nəticəsi çıxmasın.
            val movable = !item.isPast(now)

            QueueCard(
                item = item,
                enabled = enabled,
                isPast = !movable,
                canMoveUp = movable && index > 0 && !queue[index - 1].isPast(now),
                canMoveDown = movable && index >= 0 && index < queue.lastIndex,
                onMoveUp = { onMoveUp(item) },
                onMoveDown = { onMoveDown(item) },
                onEditExcerpt = { onEditExcerpt(item) },
                onEditRange = { onEditRange(item) },
                onDelete = { onDelete(item) },
            )
        }
    }
}

private fun LazyListScope.historySection(
    history: List<DailyContent>,
    enabled: Boolean,
    onRequeue: (DailyContent) -> Unit,
) {
    if (history.isEmpty()) {
        item(key = "history-empty") {
            MessageCard(
                icon = Res.drawable.dr_icon_info,
                message = stringResource(Res.string.dailyContentHistoryEmpty),
                style = MessageCardStyle.Info,
                fillMaxSize = false,
                showCard = true,
            )
        }

        return
    }

    items(count = history.size, key = { history[it].id ?: 0L }) { index ->
        val item = history[index]

        ItemCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.date.orEmpty(),
                    style = typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                TypeBadge(item)
                Spacer(Modifier.width(6.dp))
                ViewCountBadge(item.view_count)
            }

            Spacer(Modifier.height(8.dp))

            ItemPreview(item)

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onRequeue(item) }, enabled = enabled) {
                    Text(
                        text = stringResource(Res.string.dailyContentRequeue),
                        style = typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSection(enabled: Boolean, onAddVerse: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Hədis buradan **id ilə** əlavə edilmir: id istifadəçi üçün görünən şey deyil. Hədis öz
        // oxuma ekranından «Günün hədisi» düyməsi ilə növbəyə düşür və hamısı/bir qismi elə orada
        // seçilir.
        OutlinedButton(
            onClick = onAddVerse,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.dailyContentAddVerse),
                style = typography.labelLarge,
            )
        }

        Text(
            text = stringResource(Res.string.dailyContentScheduleHint),
            style = typography.bodySmall.withContentDirection(),
            color = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayHeader(date: String, count: Int) {
    val today = currentLocalDateIsoString()

    val label = when (IsoDate.daysBetween(today, date)) {
        0L -> stringResource(Res.string.dailyContentToday)
        1L -> stringResource(Res.string.dailyContentTomorrow)
        else -> date
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
        )

        Text(
            text = "$count/${DailyContentSlots.COUNT}",
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colorScheme.outlineVariant.alpha(0.4f),
        )
    }
}

@Composable
private fun QueueCard(
    item: DailyContent,
    enabled: Boolean,
    isPast: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditExcerpt: () -> Unit,
    onEditRange: () -> Unit,
    onDelete: () -> Unit,
) {
    ItemCard(dimmed = isPast) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBadge(slotIndex = item.slot_index, isPast = isPast)

            Spacer(Modifier.width(10.dp))

            TypeBadge(item)

            Spacer(Modifier.weight(1f))

            ViewCountBadge(item.view_count)
        }

        Spacer(Modifier.height(10.dp))

        ItemPreview(item)

        Spacer(Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                painter = painterResource(Res.drawable.ic_arrow_up),
                contentDescription = stringResource(Res.string.dailyContentMoveUp),
                small = true,
                enabled = enabled && canMoveUp,
                onClick = onMoveUp,
            )

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = stringResource(Res.string.dailyContentMoveDown),
                small = true,
                enabled = enabled && canMoveDown,
                onClick = onMoveDown,
            )

            Spacer(Modifier.weight(1f))

            // Hədisdə göstəriləcək hissə seçilir, ayədə isə aralıq dəyişdirilir — hər tipin
            // özünəməxsus redaktəsi.
            IconButton(
                painter = painterResource(
                    if (item.isHadith) Res.drawable.dr_icon_edit else Res.drawable.ic_book_copy
                ),
                contentDescription = if (item.isHadith) {
                    stringResource(Res.string.dailyContentExcerptTitle)
                } else {
                    stringResource(Res.string.dailyContentEditRange)
                },
                small = true,
                enabled = enabled,
                onClick = if (item.isHadith) onEditExcerpt else onEditRange,
            )

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_delete),
                contentDescription = stringResource(Res.string.strLabelDelete),
                tint = colorScheme.error,
                small = true,
                enabled = enabled,
                onClick = onDelete,
            )
        }
    }
}

/** Yuvanın saatı — kartın ilk baxışda oxunan hissəsi. Vaxtı keçmiş yuva sönük çıxır. */
@Composable
private fun TimeBadge(slotIndex: Int, isPast: Boolean) {
    val container = if (isPast) colorScheme.surfaceVariant else colorScheme.primaryContainer
    val content = if (isPast) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer

    Text(
        text = DailyContentSchedule.label(slotIndex),
        style = typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = content,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun TypeBadge(item: DailyContent) {
    Text(
        text = if (item.isHadith) {
            stringResource(Res.string.strTitleDailyHadith)
        } else {
            stringResource(Res.string.strTitleVOTD)
        },
        style = typography.labelSmall,
        color = colorScheme.onSurfaceVariant,
    )
}

/**
 * Hekayəyə neçə dəfə baxıldığı. Sayğac serverdədir və **cihaz başına bir dəfə** artır
 * ([com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository.registerView]) — yəni
 * təxmini oxunma sayıdır, kimlik saxlanmır.
 */
@Composable
private fun ViewCountBadge(count: Int) {
    Text(
        text = stringResource(Res.string.dailyContentViewCount, count),
        style = typography.labelSmall,
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colorScheme.surfaceContainerHighest)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun ItemCard(dimmed: Boolean = false, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (dimmed) colorScheme.surfaceContainerLow.alpha(0.6f)
                else colorScheme.surfaceContainerLow
            )
            .border(1.dp, colorScheme.outlineVariant.alpha(0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        content()
    }
}

/** Kartın mətn önizləməsi — çıxarış varsa **o** göstərilir, yəni admin nəticəni olduğu kimi görür. */
@Composable
private fun ItemPreview(item: DailyContent) {
    Text(
        text = item.displayTextAz.takeIf { it.isNotBlank() } ?: item.displayTextAr,
        style = typography.bodyMedium.withContentDirection(),
        color = colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!item.source.isNullOrBlank()) {
            Text(
                text = item.source,
                style = typography.labelSmall.withContentDirection(),
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        // Çoxayəli element bir yuva tutur — neçə ayə olduğu kartda görünsün.
        val verseCount = item.verseNumbers.size
        if (verseCount > 1) {
            Text(
                text = stringResource(Res.string.dailyContentVerseCount, verseCount),
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}
