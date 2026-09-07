package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.SearchTextField
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.settings.DiffPair
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.COLUMN_TEXT
import com.cafarovceyxun.anamuslim.repository.supabase.ImportRow
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationCatalogBook
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.dr_icon_paste
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.icon_clean
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.translation.ImportParseMode
import com.cafarovceyxun.anamuslim.utils.translation.ImportProblem
import com.cafarovceyxun.anamuslim.utils.translation.ParsedVerse
import com.cafarovceyxun.anamuslim.utils.translation.parseTranslationImport
import com.cafarovceyxun.anamuslim.utils.univ.rememberTextDocumentOpener
import com.cafarovceyxun.anamuslim.viewModels.AdminTranslationViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Yeni tərcüməni surə-surə Supabase-ə yükləyən admin ekranı.
 *
 * Mətn `quran_translations_data`-nın **yeni sütununa** yazılır (`text_alt`) — sətirlər eyni `az`
 * sətirləridir, ona görə ayə uyğunluğu birə-birdir və mövcud tərcümə toxunulmaz qalır.
 *
 * Axın dörd nömrələnmiş addımdır: kitab → surə → mətn → önizləmə. Heç nə avtomatik yazılmır:
 * önizləmədə hər ayə **statusu ilə** görünür (yeni / dəyişən / eyni / problemli), yükləmə düyməsi
 * isə app bar-dadır ki, 286 sətirlik siyahını sürüşdürərkən də əlçatan qalsın.
 *
 * Yükləmədən əvvəl təsdiq vərəqi çıxır: bir ayənin **üzərinə yazmaq** geri qaytarılmır, ona görə
 * neçə sətrin əvəzlənəcəyi rəqəmlə soruşulur. Əsas sütun (`text`) seçilibsə xəbərdarlıq daha
 * kəskindir — ora yazmaq canlı tərcüməni dəyişir.
 */
@Composable
fun AdminTranslationImportScreen() {
    val vm = viewModel { AdminTranslationViewModel() }
    val books by vm.books.collectAsState()
    val chapterNames by vm.chapterNames.collectAsState()
    val verseCount by vm.verseCount.collectAsState()
    val filledCount by vm.filledCount.collectAsState()
    val chapterTexts by vm.chapterTexts.collectAsState()
    val isBusy by vm.isBusy.collectAsState()
    val message by vm.message.collectAsState()

    // Əsas kitab (`text`) qəsdən seçilmir: ora yazmaq mövcud tərcüməni əvəz edərdi. İlk hədəf
    // əlavə sütunlu kitabdır; yoxdursa siyahının ilk sətri.
    var selectedSlug by remember { mutableStateOf<String?>(null) }
    var chapterNo by remember { mutableStateOf(1) }
    var mode by remember { mutableStateOf(ImportParseMode.ByVerseNumber) }
    var rawText by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var chapterPickerOpen by remember { mutableStateOf(false) }
    var confirmOpen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<VerseStatus?>(null) }

    val selectedBook = books.firstOrNull { it.slug == selectedSlug }
    val writesToMainColumn = selectedBook?.source_column == COLUMN_TEXT

    LaunchedEffect(Unit) { vm.load() }

    LaunchedEffect(books) {
        if (selectedSlug == null && books.isNotEmpty()) {
            selectedSlug = (books.firstOrNull { it.source_column != COLUMN_TEXT } ?: books.first()).slug
        }
    }

    LaunchedEffect(selectedBook, chapterNo) {
        vm.selectChapter(selectedBook, chapterNo)
    }

    LaunchedEffect(message) {
        message?.let {
            PlatformUtils.showLongToast(it)
            vm.clearMessage()
        }
    }

    val opener = rememberTextDocumentOpener { content ->
        if (content != null) {
            rawText = content
            showPreview = false
        }
    }

    val parsed = remember(rawText, verseCount, mode, showPreview) {
        if (!showPreview || verseCount == 0) null
        else parseTranslationImport(rawText, verseCount, mode)
    }

    // Hər ayənin hazırkı mətnlə müqayisəsi bir yerdə hesablanır: həm süzgəc çipləri, həm sətirlər,
    // həm də təsdiq vərəqindəki «neçə sətrin üzərinə yazılır» rəqəmi eyni siyahıdan oxuyur.
    val items = remember(parsed, chapterTexts) {
        parsed?.verses.orEmpty().map { verse ->
            val current = chapterTexts[verse.verseNo]
            PreviewItem(
                verse = verse,
                currentText = current,
                status = when {
                    verse.problem != null -> VerseStatus.Problem
                    current == null -> VerseStatus.New
                    current.trim() == verse.text.trim() -> VerseStatus.Same
                    else -> VerseStatus.Changed
                },
            )
        }
    }
    val counts = remember(items) { items.groupingBy { it.status }.eachCount() }
    val visibleItems = remember(items, filter) {
        if (filter == null) items else items.filter { it.status == filter }
    }
    val overwriteCount = counts[VerseStatus.Changed] ?: 0

    val performUpload: () -> Unit = upload@{
        val book = selectedBook ?: return@upload
        val result = parsed ?: return@upload
        vm.upload(
            book = book,
            chapterNo = chapterNo,
            rows = result.usable.map { ImportRow(it.verseNo, it.text) },
        )
    }

    val canUpload = !isBusy && selectedBook != null && parsed != null && parsed.usable.isNotEmpty()

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column {
                AppBar(
                    title = "Tərcümə idxalı",
                    actions = {
                        IconButton(
                            painter = painterResource(Res.drawable.dr_icon_refresh),
                            contentDescription = "Yenilə",
                            enabled = !isBusy,
                            small = true,
                        ) {
                            vm.load()
                            vm.selectChapter(selectedBook, chapterNo)
                        }

                        Button(
                            onClick = {
                                // Üzərinə yazma və ya əsas sütun — geri dönüşü olmayan hal, soruş.
                                if (overwriteCount > 0 || writesToMainColumn) confirmOpen = true
                                else performUpload()
                            },
                            modifier = Modifier.padding(start = 2.dp, end = 4.dp).height(40.dp),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            enabled = canUpload,
                        ) {
                            if (isBusy) {
                                Loader(size = 18.dp)
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Yüklə",
                                    style = typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    },
                )

                // Bar-ın altındakı nazik zolaq: yükləmə/oxuma gedərkən ekranın canlı olduğunu bildirir.
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = mainBottomNavigationOuterHeight() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StepCard(step = 1, title = "Kitab", subtitle = "Mətn bu kitabın sütununa yazılacaq") {
                    if (books.isEmpty()) {
                        Text(
                            text = if (isBusy) "Kataloq yüklənir…" else "Kataloq boşdur.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            books.forEach { book ->
                                Chip(
                                    selected = book.slug == selectedSlug,
                                    label = { Text(book.book_name.ifBlank { book.slug }) },
                                ) {
                                    selectedSlug = book.slug
                                }
                            }
                        }
                    }

                    selectedBook?.let { book ->
                        Spacer(Modifier.height(10.dp))
                        BookSummary(book)
                    }

                    if (writesToMainColumn) {
                        Spacer(Modifier.height(10.dp))
                        NoticeCard(
                            style = NoticeStyle.Danger,
                            title = "Əsas sütun seçilib",
                            text = "Bu kitab canlı tərcümənin özüdür (`$COLUMN_TEXT`). Yazılan hər " +
                                "ayə istifadəçilərin oxuduğu mətni dərhal əvəz edir.",
                        )
                    }
                }
            }

            item {
                StepCard(
                    step = 2,
                    title = "Surə",
                    subtitle = "Yükləmə yalnız seçilmiş surəyə toxunur",
                ) {
                    ChapterSelector(
                        chapterNo = chapterNo,
                        chapterName = chapterNames[chapterNo].orEmpty(),
                        onPick = { chapterPickerOpen = true },
                        onPrev = {
                            chapterNo -= 1
                            showPreview = false
                        },
                        onNext = {
                            chapterNo += 1
                            showPreview = false
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    ChapterFillBar(verseCount = verseCount, filledCount = filledCount)
                }
            }

            item {
                StepCard(
                    step = 3,
                    title = "Mətn",
                    subtitle = "Yapışdırın ya da .txt faylından oxuyun",
                ) {
                    ModeSelector(
                        mode = mode,
                        onChange = {
                            mode = it
                            showPreview = false
                        },
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = {
                            rawText = it
                            showPreview = false
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                        // İnterfeys dili ərəbcə olanda RTL düzülüş latın abzasını güzgüləyir —
                        // idxal mətni həmişə azərbaycancadır, ona görə istiqamət kilidlənir.
                        textStyle = typography.bodyMedium.withScriptDirection(arabic = false),
                        label = { Text("Tərcümə mətni") },
                        placeholder = { Text("1. Mərhəmətli, Rəhmli Allahın adı ilə!\n\n2. …") },
                        supportingText = { Text(textStats(rawText)) },
                    )

                    Spacer(Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SmallAction(
                            icon = Res.drawable.dr_icon_open,
                            label = "Fayldan",
                            onClick = { opener.open() },
                        )
                        SmallAction(
                            icon = Res.drawable.dr_icon_paste,
                            label = "Panodan",
                            onClick = {
                                val clip = PlatformUtils.readFromClipboard()
                                if (clip.isNullOrBlank()) {
                                    PlatformUtils.showToast("Pano boşdur.")
                                } else {
                                    rawText = clip
                                    showPreview = false
                                }
                            },
                        )
                        SmallAction(
                            icon = Res.drawable.icon_clean,
                            label = "Təmizlə",
                            enabled = rawText.isNotEmpty(),
                            onClick = {
                                rawText = ""
                                showPreview = false
                            },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            filter = null
                            showPreview = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rawText.isNotBlank() && verseCount > 0,
                    ) {
                        Text(if (showPreview) "Yenidən böl" else "Önizlə")
                    }
                }
            }

            if (parsed != null) {
                item {
                    StepCard(
                        step = 4,
                        title = "Önizləmə",
                        subtitle = "Yükləmə düyməsi yuxarıdakı bar-dadır",
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                label = "Hamısı",
                                count = items.size,
                                selected = filter == null,
                                onClick = { filter = null },
                            )
                            VerseStatus.entries.forEach { status ->
                                val count = counts[status] ?: 0
                                if (count > 0) {
                                    FilterChip(
                                        label = status.label,
                                        count = count,
                                        selected = filter == status,
                                        onClick = { filter = if (filter == status) null else status },
                                    )
                                }
                            }
                        }

                        val missing = parsed.missing.size
                        if (missing > 0) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "$missing ayə mətnsiz qalır — onlara toxunulmayacaq.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }

                        parsed.warnings.forEach { warning ->
                            Spacer(Modifier.height(10.dp))
                            NoticeCard(style = NoticeStyle.Warning, text = warning)
                        }
                    }
                }

                if (visibleItems.isEmpty()) {
                    item {
                        Text(
                            text = "Bu süzgəcdə sətir yoxdur.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }

                items(visibleItems, key = { it.verse.verseNo }) { entry ->
                    PreviewRow(entry = entry, chapterNo = chapterNo)
                }
            }
        }
    }

    ChapterPickerDialog(
        isOpen = chapterPickerOpen,
        chapterNo = chapterNo,
        chapterNames = chapterNames,
        onClose = { chapterPickerOpen = false },
        onSelect = {
            chapterNo = it
            showPreview = false
            chapterPickerOpen = false
        },
    )

    AlertDialog(
        isOpen = confirmOpen,
        onClose = { confirmOpen = false },
        title = "Yükləməni təsdiqlə",
        actions = listOf(
            AlertDialogAction(text = "Ləğv et"),
            AlertDialogAction(
                text = "Yüklə",
                style = AlertDialogActionStyle.Danger,
                onClick = performUpload,
            ),
        ),
    ) {
        val book = selectedBook
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${chapterNo}. ${chapterNames[chapterNo].orEmpty()} · " +
                    "${book?.book_name?.ifBlank { book.slug } ?: "—"}",
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${parsed?.usable?.size ?: 0} ayə yazılacaq, bunlardan $overwriteCount " +
                    "ayənin hazırkı mətni əvəzlənəcək. Bu əməliyyat geri qaytarılmır.",
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
            if (writesToMainColumn) {
                NoticeCard(
                    style = NoticeStyle.Danger,
                    text = "Hədəf əsas sütundur — dəyişiklik istifadəçilərə dərhal çıxır.",
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Önizləmə modeli
 * ------------------------------------------------------------------------------------------- */

private data class PreviewItem(
    val verse: ParsedVerse,
    val currentText: String?,
    val status: VerseStatus,
)

/** Bir ayənin yükləmədən sonra nə olacağı — süzgəc çipləri də bu sıra ilə görünür. */
private enum class VerseStatus(val label: String) {
    New("Yeni"),
    Changed("Dəyişən"),
    Same("Eyni"),
    Problem("Problemli"),
}

@Composable
private fun VerseStatus.containerColor(): Color = when (this) {
    VerseStatus.New -> colorScheme.primary.alpha(0.12f)
    VerseStatus.Changed -> colorScheme.tertiary.alpha(0.16f)
    VerseStatus.Same -> colorScheme.onSurface.alpha(0.08f)
    VerseStatus.Problem -> colorScheme.error.alpha(0.14f)
}

@Composable
private fun VerseStatus.contentColor(): Color = when (this) {
    VerseStatus.New -> colorScheme.primary
    VerseStatus.Changed -> colorScheme.tertiary
    VerseStatus.Same -> colorScheme.onSurfaceVariant
    VerseStatus.Problem -> colorScheme.error
}

/* ---------------------------------------------------------------------------------------------
 * Addım kartları və köməkçi sətirlər
 * ------------------------------------------------------------------------------------------- */

/** Nömrələnmiş başlığı olan kart — dörd addım eyni çərçivədə görünsün deyə. */
@Composable
private fun StepCard(
    step: Int,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(colorScheme.primary.alpha(0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = step.toString(),
                    style = typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = typography.titleSmall, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun BookSummary(book: TranslationCatalogBook) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.onSurface.alpha(0.04f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.author_name.ifBlank { "Müəllif göstərilməyib" },
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // Sütun adı admin üçün vacibdir: mətnin harada saxlandığını göstərir.
                text = "${book.slug} · ${book.source_column}",
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(
            text = if (book.is_public) "Açıq" else "Gizli",
            container = if (book.is_public) colorScheme.primary.alpha(0.12f)
            else colorScheme.onSurface.alpha(0.08f),
            content = if (book.is_public) colorScheme.primary else colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChapterSelector(
    chapterNo: Int,
    chapterName: String,
    onPick: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            painter = painterResource(Res.drawable.dr_icon_chevron_left),
            contentDescription = "Əvvəlki surə",
            enabled = chapterNo > QuranMeta.chapterRange.first,
            small = true,
            tint = colorScheme.onSurface,
        ) { onPrev() }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onPick)
                .background(colorScheme.onSurface.alpha(0.04f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$chapterNo",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = chapterName.ifBlank { "…" },
                style = typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            painter = painterResource(Res.drawable.dr_icon_chevron_right),
            contentDescription = "Sonrakı surə",
            enabled = chapterNo < QuranMeta.chapterRange.last,
            small = true,
            tint = colorScheme.onSurface,
        ) { onNext() }
    }
}

/** Surənin nə qədərinin bu kitabda artıq dolu olduğu — zolaq + rəqəm. */
@Composable
private fun ChapterFillBar(verseCount: Int, filledCount: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Dolğunluq",
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    verseCount == 0 -> "…"
                    filledCount == null -> "$verseCount ayə · oxunur…"
                    else -> "$filledCount / $verseCount ayə"
                },
                style = typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = {
                if (verseCount == 0 || filledCount == null) 0f
                else filledCount.toFloat() / verseCount
            },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        )
    }
}

@Composable
private fun ModeSelector(mode: ImportParseMode, onChange: (ImportParseMode) -> Unit) {
    Column {
        Text(
            text = "Bölgü rejimi",
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(
                selected = mode == ImportParseMode.ByVerseNumber,
                label = { Text("Ayə nömrəsinə görə") },
            ) { onChange(ImportParseMode.ByVerseNumber) }
            Chip(
                selected = mode == ImportParseMode.SequentialParagraphs,
                label = { Text("Ardıcıl abzas") },
            ) { onChange(ImportParseMode.SequentialParagraphs) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = when (mode) {
                ImportParseMode.ByVerseNumber ->
                    "Abzasın əvvəlindəki rəqəm ayə nömrəsidir; nömrəsiz abzas əvvəlkinin davamıdır."
                ImportParseMode.SequentialParagraphs ->
                    "Rəqəm gözlənilmir — abzaslar sırayla 1-dən başlayaraq ayələrə düşür."
            },
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SmallAction(
    icon: DrawableResource,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun FilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Chip(
        selected = selected,
        label = { Text("$label · $count") },
        onClick = onClick,
    )
}

@Composable
private fun StatusPill(text: String, container: Color, content: Color) {
    Text(
        text = text,
        style = typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private enum class NoticeStyle { Info, Warning, Danger }

/** Xəbərdarlıqlar üçün ikonlu qutu — «⚠️» simvolu ilə qırmızı sətir əvəzinə. */
@Composable
private fun NoticeCard(style: NoticeStyle, text: String, title: String? = null) {
    val (container, content, icon) = when (style) {
        NoticeStyle.Info ->
            Triple(colorScheme.primary.alpha(0.10f), colorScheme.primary, Res.drawable.dr_icon_info)
        NoticeStyle.Warning ->
            Triple(
                colorScheme.tertiary.alpha(0.14f),
                colorScheme.tertiary,
                Res.drawable.dr_icon_report_problem,
            )
        NoticeStyle.Danger ->
            Triple(
                colorScheme.error.alpha(0.12f),
                colorScheme.error,
                Res.drawable.dr_icon_report_problem,
            )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = content,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = content,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = text,
                style = typography.bodySmall,
                color = colorScheme.onSurface.alpha(0.85f),
            )
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Önizləmə sətri
 * ------------------------------------------------------------------------------------------- */

@Composable
private fun PreviewRow(entry: PreviewItem, chapterNo: Int) {
    val verse = entry.verse
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = if (entry.status == VerseStatus.Problem) colorScheme.error.alpha(0.7f)
                else colorScheme.outlineVariant.alpha(0.6f),
                shape = shape,
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$chapterNo:${verse.verseNo}",
                style = typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            StatusPill(
                text = entry.status.label,
                container = entry.status.containerColor(),
                content = entry.status.contentColor(),
            )
            verse.problem?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (it) {
                        ImportProblem.Duplicate -> "təkrar nömrə"
                        ImportProblem.OutOfRange -> "surədən kənar — yüklənməyəcək"
                    },
                    style = typography.labelSmall,
                    color = colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Ayənin bu kitabda artıq mətni varsa üzərinə yazılacaq — fərq sarı ilə görünsün.
        // Mətn eynidirsə diff mənasızdır: sətir bir dəfə, sakit rənglə göstərilir.
        if (entry.status == VerseStatus.Changed && entry.currentText != null) {
            DiffPair(
                old = entry.currentText,
                new = verse.text,
                oldLabel = "Hazırkı",
                newLabel = "Yeni",
            )
        } else {
            Text(
                text = verse.text,
                style = typography.bodyMedium.withScriptDirection(arabic = false),
                color = if (entry.status == VerseStatus.Same) colorScheme.onSurface.alpha(0.6f)
                else colorScheme.onSurface,
            )
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Surə seçimi
 * ------------------------------------------------------------------------------------------- */

/**
 * 114 surəni axtarışla seçdirən vərəq.
 *
 * Öz `Dialog`-udur, paylaşılan [AlertDialog] deyil: oranın məzmunu `verticalScroll` Column-dur,
 * `LazyColumn` isə orada sonsuz hündürlüklə çökür. Ayrıca `LazyColumn` seçilmiş surəyə **açılan
 * kimi** sürüşür — əvvəlki inline siyahıda 114-cü surəyə çatmaq üçün əl ilə sürüşdürmək lazım idi.
 */
@Composable
private fun ChapterPickerDialog(
    isOpen: Boolean,
    chapterNo: Int,
    chapterNames: Map<Int, String>,
    onClose: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    if (!isOpen) return

    var query by remember { mutableStateOf("") }
    val all = remember(chapterNames) { QuranMeta.chapterRange.toList() }
    val filtered = remember(all, chapterNames, query) {
        val q = query.trim()
        if (q.isEmpty()) all
        else all.filter { no ->
            no.toString().startsWith(q) || chapterNames[no].orEmpty().contains(q, ignoreCase = true)
        }
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (chapterNo - 3).coerceAtLeast(0),
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.large,
            color = colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column {
                Text(
                    text = "Surə seç",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
                Spacer(Modifier.height(10.dp))
                SearchTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Nömrə ya ad",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    items(filtered, key = { it }) { no ->
                        val selected = no == chapterNo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(no) }
                                .background(
                                    if (selected) colorScheme.primary.alpha(0.10f) else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$no",
                                style = typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp),
                            )
                            Text(
                                text = chapterNames[no].orEmpty(),
                                style = typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) colorScheme.primary else colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (selected) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colorScheme.primary,
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = "Uyğun surə yoxdur.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onClose,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorScheme.onSurfaceVariant,
                        ),
                    ) { Text("Bağla") }
                }
            }
        }
    }
}

/** «3 abzas · 1 240 simvol» — mətnin gözlə ölçülməyən iki rəqəmi. */
private fun textStats(raw: String): String {
    if (raw.isBlank()) return "Mətn boşdur"
    val paragraphs = raw.replace("\r\n", "\n").replace("\r", "\n")
        .split(Regex("""\n\s*\n"""))
        .count { it.isNotBlank() }
    return "$paragraphs abzas · ${raw.length} simvol"
}
