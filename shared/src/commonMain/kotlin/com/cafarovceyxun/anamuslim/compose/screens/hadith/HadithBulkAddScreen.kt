package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_footnote
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.hedis
import com.cafarovceyxun.anamuslim.resources.ic_book_copy
import com.cafarovceyxun.anamuslim.resources.ic_mode_book
import com.cafarovceyxun.anamuslim.resources.strActionBulkImport
import com.cafarovceyxun.anamuslim.resources.strHintBulkText
import com.cafarovceyxun.anamuslim.resources.strLabelBulkFormat
import com.cafarovceyxun.anamuslim.resources.strLabelBulkPreview
import com.cafarovceyxun.anamuslim.resources.strLabelBulkText
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strMsgBulkFormatHelp
import com.cafarovceyxun.anamuslim.resources.strMsgBulkImportDone
import com.cafarovceyxun.anamuslim.resources.strMsgBulkImportFailedRows
import com.cafarovceyxun.anamuslim.resources.strMsgBulkImportStopped
import com.cafarovceyxun.anamuslim.resources.strMsgBulkMoreRows
import com.cafarovceyxun.anamuslim.resources.strMsgBulkNothingParsed
import com.cafarovceyxun.anamuslim.resources.strMsgBulkProblemBadVerse
import com.cafarovceyxun.anamuslim.resources.strMsgBulkProblemDropped
import com.cafarovceyxun.anamuslim.resources.strMsgBulkProblemNameless
import com.cafarovceyxun.anamuslim.resources.strMsgBulkProblemOrphan
import com.cafarovceyxun.anamuslim.resources.strMsgBulkProblemUnsupported
import com.cafarovceyxun.anamuslim.resources.strMsgBulkProblemVerseMissing
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.strMsgBulkSummary
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.strTitleBulkAdd
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Adds a whole book at once: one paste holding the babs, alt-babs, hadiths and Quran references in
 * the order they appear on the page, written to the database in that same order.
 *
 * The screen is deliberately a *preview* first. Everything the paste describes — the numbers each
 * row will get, the slugs, the verse texts pulled out of the Quran — is worked out and shown before
 * a single row is written, because the alternative is finding out about a mis-typed label a hundred
 * rows into a book that now has to be deleted by hand. Nothing here writes until the import button
 * is tapped.
 *
 * See [parseHadithBulk] for the format and [buildBulkPlan] for the numbering.
 */
@Composable
fun HadithBulkAddScreen(
    bookSlug: String,
    bookName: String,
    onBack: () -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val isLoading by viewModel.isLoading.collectAsState()
    val translationFactory = QuranTranslationFactory.remember()

    var raw by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<BulkParseResult?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    var nextChapterNo by remember { mutableStateOf<Int?>(null) }

    var isImporting by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }

    // Yarımçıq idxaldan sonra yazılmamış sətirlər: təkrar «İdxal et» bütün planı yox, məhz bunları
    // göndərir. Hədis sətrinin `id`-si yoxdur — eyni planı ikinci dəfə göndərmək artıq yazılmış
    // hədisləri surətləyərdi.
    var pendingRows by remember { mutableStateOf<List<BulkRow>?>(null) }

    LaunchedEffect(bookSlug) {
        nextChapterNo = viewModel.getNextNumber(EditorType.CHAPTER, null, bookSlug, null, null)
    }

    // Ayələrin mətni bazadan gəlir, ona görə təhlil arxa planda və yazmaqdan asılı olmayan gecikmə
    // ilə işləyir: mətn yapışdırıldıqdan sonra bir dəfə, hər hərfdən sonra yox.
    LaunchedEffect(raw) {
        pendingRows = null
        if (raw.isBlank()) {
            parsed = null
            isParsing = false
            return@LaunchedEffect
        }
        isParsing = true
        parsed = resolveBulkVerses(parseHadithBulk(raw), translationFactory)
        isParsing = false
    }

    val parsedPlan = remember(parsed, nextChapterNo, bookSlug) {
        val entries = parsed?.entries.orEmpty()
        val first = nextChapterNo
        if (entries.isEmpty() || first == null) emptyList()
        else buildBulkPlan(bookSlug, entries, first)
    }
    val plan = pendingRows ?: parsedPlan

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(enabled = !isImporting, onBack = onBack)

    val doneTemplate = stringResource(Res.string.strMsgBulkImportDone)
    val stoppedMessage = stringResource(Res.string.strMsgBulkImportStopped)
    val failedTemplate = stringResource(Res.string.strMsgBulkImportFailedRows)

    val onImport = {
        progress = 0
        isImporting = true
        viewModel.importBulkRows(
            rows = plan,
            onProgress = { progress = it },
            onResult = { outcome ->
                isImporting = false
                PlatformUtils.showLongToast(
                    doneTemplate
                        .replace("%1\$d", outcome.chapters.toString())
                        .replace("%2\$d", outcome.subChapters.toString())
                        .replace("%3\$d", (outcome.hadiths + outcome.queued).toString())
                )

                if (outcome.remaining.isEmpty()) {
                    onBack()
                } else {
                    // Formada yalnız qalanlar durur — təkrar «İdxal et» yazılanları surətləmir.
                    pendingRows = outcome.remaining
                    PlatformUtils.showLongToast(
                        if (outcome.stoppedAt != null) {
                            stoppedMessage
                        } else {
                            failedTemplate.replace("%1\$d", outcome.failed.toString())
                        }
                    )
                }
            },
        )
    }

    Scaffold(
        topBar = {
            AppBar(
                title = bookName.ifBlank { stringResource(Res.string.strTitleBulkAdd) },
                onBack = { if (!isImporting) onBack() },
                actions = {
                    BulkBarActions(
                        canImport = plan.isNotEmpty() && !isImporting && !isParsing && !isLoading,
                        isBusy = isImporting || isLoading,
                        onCancel = onBack,
                        onImport = onImport,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // imePadding AFTER verticalScroll — eyni səbəb `HadithEditorScreen`-dəki kimi.
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = 32.dp + mainBottomNavigationOuterHeight()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorSection(title = stringResource(Res.string.strLabelBulkFormat)) {
                Text(
                    text = stringResource(Res.string.strMsgBulkFormatHelp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.alpha(0.75f),
                )
            }

            EditorSection(title = stringResource(Res.string.strLabelBulkText)) {
                FormTextField(
                    value = raw,
                    onValueChange = { raw = it },
                    label = stringResource(Res.string.strLabelBulkText),
                    placeholder = stringResource(Res.string.strHintBulkText),
                    icon = Res.drawable.dr_icon_footnote,
                    minLines = 8,
                    maxLines = 24,
                    readOnly = isImporting,
                    onClear = { raw = "" },
                    onPaste = { raw = it },
                )
            }

            if (isImporting) {
                BulkProgress(done = progress, total = plan.size)
            }

            when {
                isParsing -> Loader(false)

                raw.isNotBlank() && plan.isEmpty() -> EditorSection(
                    title = stringResource(Res.string.strLabelBulkPreview),
                ) {
                    BulkProblemRow(stringResource(Res.string.strMsgBulkNothingParsed))
                    parsed?.problems?.forEach { BulkProblemRow(it.describe()) }
                }

                plan.isNotEmpty() -> EditorSection(
                    title = stringResource(Res.string.strLabelBulkPreview),
                ) {
                    BulkSummary(parsed)
                    parsed?.problems?.forEach { BulkProblemRow(it.describe()) }
                    Spacer(Modifier.height(4.dp))
                    BulkPreviewList(plan)
                }
            }
        }
    }
}

/** `%1$d bab · %2$d alt bab · %3$d hədis` — nə qədər sətir yazılacağı. */
@Composable
private fun BulkSummary(parsed: BulkParseResult?) {
    val result = parsed ?: return
    Text(
        text = stringResource(
            Res.string.strMsgBulkSummary,
            result.chapterCount,
            result.subChapterCount,
            result.hadithCount,
        ),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
}

/** Buraxılan hər şey burada görünür — sayılmayan itki idxalın ən pis nəticəsidir. */
@Composable
private fun BulkProblemRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.dr_icon_info),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorScheme.error,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.error,
        )
    }
}

@Composable
private fun BulkProblem.describe(): String = when (this) {
    is BulkProblem.DroppedLines -> stringResource(Res.string.strMsgBulkProblemDropped, count)
    is BulkProblem.Orphan -> stringResource(Res.string.strMsgBulkProblemOrphan, count)
    is BulkProblem.NamelessSection -> stringResource(Res.string.strMsgBulkProblemNameless, count)
    is BulkProblem.BadVerseLabel ->
        stringResource(Res.string.strMsgBulkProblemBadVerse, labels.joinToString(", "))

    is BulkProblem.UnsupportedLabel ->
        stringResource(Res.string.strMsgBulkProblemUnsupported, labels.joinToString(", "))

    is BulkProblem.VerseUnavailable ->
        stringResource(Res.string.strMsgBulkProblemVerseMissing, references.joinToString(", "))
}

/**
 * The rows in the order they will be written, with the number each one is about to get.
 *
 * Capped rather than lazy on purpose: the whole screen is one scrolling column, and a lazy list
 * inside it has no height to measure against. A book-sized paste is checked by its head and its
 * counts, not by scrolling all of it.
 */
@Composable
private fun BulkPreviewList(plan: List<BulkRow>) {
    val arabicFontFamily = hadithArabicFontFamily(HadithPreferences.observeArabicFont())
    val shown = plan.take(BulkPreviewLimit)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.forEach { row ->
            when (row) {
                is BulkRow.Chapter -> BulkPreviewRow(
                    icon = Res.drawable.ic_book_copy,
                    number = row.row.chapter_no.toString(),
                    title = row.row.name,
                    arabic = row.row.name_ar,
                    arabicFontFamily = arabicFontFamily,
                    emphasised = true,
                    indent = 0.dp,
                )

                is BulkRow.SubChapter -> BulkPreviewRow(
                    icon = Res.drawable.ic_mode_book,
                    number = row.row.sub_chapter_no.toString(),
                    title = row.row.name,
                    arabic = row.row.name_ar,
                    arabicFontFamily = arabicFontFamily,
                    emphasised = true,
                    indent = 12.dp,
                )

                is BulkRow.HadithRow -> BulkPreviewRow(
                    icon = Res.drawable.hedis,
                    number = row.row.hadith_no.toString(),
                    title = row.row.text_az,
                    arabic = row.row.text_ar.takeIf { it.isNotBlank() },
                    arabicFontFamily = arabicFontFamily,
                    emphasised = false,
                    indent = if (row.row.sub_chapter_slug != null) 24.dp else 12.dp,
                    source = row.row.source,
                    note = row.row.note,
                )
            }
        }

        if (plan.size > shown.size) {
            Text(
                text = stringResource(Res.string.strMsgBulkMoreRows, plan.size - shown.size),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.alpha(0.6f),
            )
        }
    }
}

/**
 * One row as it will be written: number, the translation, the Arabic, and — because a hadith is as
 * often recognised by where it came from as by its wording — its source and note.
 *
 * Long texts are shown from **both ends**: three lines of the opening, then three of the closing.
 * A hadith runs to a paragraph or more, and an opening alone all looks the same (`حَدَّثَنَا …`), so a
 * head-only excerpt cannot tell one row from the next, nor show that the tail arrived intact.
 */
@Composable
private fun BulkPreviewRow(
    icon: DrawableResource,
    number: String,
    title: String,
    arabic: String?,
    arabicFontFamily: FontFamily,
    emphasised: Boolean,
    indent: Dp,
    source: String? = null,
    note: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = indent),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
            tint = if (emphasised) colorScheme.primary else colorScheme.onSurface.alpha(0.5f),
        )

        Text(
            text = number,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.alpha(0.6f),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (title.isNotBlank()) {
                BulkPreviewText(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }

            if (!arabic.isNullOrBlank()) {
                BulkPreviewText(
                    text = arabic,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDirection = TextDirection.Rtl,
                        fontFamily = arabicFontFamily,
                        fontSize = 13.sp,
                    ),
                    color = colorScheme.onSurface.alpha(0.75f),
                )
            }

            // Mənbə və qeyd qısa olur: onlar tam görünür, kəsilmir.
            if (!source.isNullOrBlank()) {
                BulkPreviewMeta(label = stringResource(Res.string.source), value = source)
            }
            if (!note.isNullOrBlank()) {
                BulkPreviewMeta(label = stringResource(Res.string.strTitleNote), value = note)
            }
        }
    }
}

@Composable
private fun BulkProgress(done: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "$done / $total",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.alpha(0.6f),
        )
    }
}

@Composable
private fun BulkBarActions(
    canImport: Boolean,
    isBusy: Boolean,
    onCancel: () -> Unit,
    onImport: () -> Unit,
) {
    IconButton(onClick = onCancel, enabled = !isBusy) {
        Icon(
            painter = painterResource(Res.drawable.dr_icon_close),
            contentDescription = stringResource(Res.string.strLabelCancel),
            modifier = Modifier.size(20.dp),
        )
    }

    Button(
        onClick = onImport,
        modifier = Modifier.padding(start = 2.dp, end = 4.dp).height(40.dp),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 14.dp),
        enabled = canImport,
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
                text = stringResource(Res.string.strActionBulkImport),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

/**
 * The head and, for anything long enough to need it, the tail — each capped at [BulkPreviewLines]
 * lines by Compose itself rather than by a character count, so the cap holds at any text size.
 *
 * The tail *is* taken by character count ([BulkPreviewTailChars]); nothing in the layout can anchor
 * a wrapped paragraph to its end. The count is deliberately short of three lines' worth, so the tail
 * block stays inside its own cap on a narrow screen instead of losing the last words to an ellipsis.
 */
@Composable
private fun BulkPreviewText(
    text: String,
    style: TextStyle,
    color: Color = colorScheme.onSurface,
) {
    val flat = remember(text) { text.flattenedForPreview() }
    val hasTail = flat.length > BulkPreviewInlineLimit

    Text(
        text = flat,
        style = style,
        color = color,
        maxLines = if (hasTail) BulkPreviewLines else BulkPreviewLines * 2,
        overflow = TextOverflow.Ellipsis,
    )

    if (hasTail) {
        Text(
            text = "… " + flat.takeLast(BulkPreviewTailChars).trimStart(),
            style = style,
            color = color,
            maxLines = BulkPreviewLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** `Qaynaq: Buxari 42` — one muted line under the texts. */
@Composable
private fun BulkPreviewMeta(label: String, value: String) {
    Text(
        text = "$label: ${value.flattenedForPreview()}",
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.onSurface.alpha(0.6f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Abzaslar və boşluq yığınları tək boşluğa yığılır — sətir sayı yalnız sarınmadan asılı olsun. */
private fun String.flattenedForPreview(): String =
    split('\n').joinToString(" ") { it.trim() }.replace(PreviewWhitespaceRuns, " ").trim()

private val PreviewWhitespaceRuns = Regex("\\s{2,}")

private const val BulkPreviewLimit = 120
private const val BulkPreviewLines = 3
private const val BulkPreviewInlineLimit = 260
private const val BulkPreviewTailChars = 110
