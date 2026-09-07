package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_footnote
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_search
import com.cafarovceyxun.anamuslim.resources.dr_icon_undo
import com.cafarovceyxun.anamuslim.resources.hedis
import com.cafarovceyxun.anamuslim.resources.ic_book_copy
import com.cafarovceyxun.anamuslim.resources.ic_mode_book
import com.cafarovceyxun.anamuslim.resources.clear
import com.cafarovceyxun.anamuslim.resources.strActionBulkAnalyze
import com.cafarovceyxun.anamuslim.resources.strActionBulkCollapseText
import com.cafarovceyxun.anamuslim.resources.strActionBulkImport
import com.cafarovceyxun.anamuslim.resources.strActionBulkJump
import com.cafarovceyxun.anamuslim.resources.strActionUndo
import com.cafarovceyxun.anamuslim.resources.strHintBulkText
import com.cafarovceyxun.anamuslim.resources.strLabelBulkCheck
import com.cafarovceyxun.anamuslim.resources.strLabelBulkFormat
import com.cafarovceyxun.anamuslim.resources.strLabelBulkImported
import com.cafarovceyxun.anamuslim.resources.strLabelBulkPreview
import com.cafarovceyxun.anamuslim.resources.strLabelBulkText
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDone
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strMsgBulkTextCollapsed
import com.cafarovceyxun.anamuslim.resources.strMsgBulkCheckBlocked
import com.cafarovceyxun.anamuslim.resources.strMsgBulkCheckClean
import com.cafarovceyxun.anamuslim.resources.strMsgBulkCheckCounts
import com.cafarovceyxun.anamuslim.resources.strMsgBulkFormatHelp
import com.cafarovceyxun.anamuslim.resources.strMsgBulkImportDone
import com.cafarovceyxun.anamuslim.resources.strMsgBulkImportFailedRows
import com.cafarovceyxun.anamuslim.resources.strMsgBulkImportStopped
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueArabicInLatin
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueChapterExists
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueDuplicateChapter
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueDuplicateHadith
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueEmptyField
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueIncomplete
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueLatinInArabic
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueLine
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueMissingArabicName
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueMissingLatinName
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueOutOfOrder
import com.cafarovceyxun.anamuslim.resources.strMsgBulkIssueRepeated
import com.cafarovceyxun.anamuslim.resources.strMsgBulkAnalyzeHint
import com.cafarovceyxun.anamuslim.resources.strMsgBulkAnalyzeStale
import com.cafarovceyxun.anamuslim.resources.strMsgBulkMoreIssues
import com.cafarovceyxun.anamuslim.resources.strMsgBulkNothingParsed
import com.cafarovceyxun.anamuslim.resources.strMsgBulkTextLength
import com.cafarovceyxun.anamuslim.resources.strMsgBulkUndoBlocked
import com.cafarovceyxun.anamuslim.resources.strMsgBulkUndoDone
import com.cafarovceyxun.anamuslim.resources.strMsgBulkUndoHint
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // Mətn `TextFieldValue`-dur, çünki yoxlama paneli xətanın olduğu sətrə tullanır — tullanmaq
    // seçim qoymaq deməkdir, `String` isə kursordan xəbərsizdir.
    var raw by remember { mutableStateOf(TextFieldValue()) }
    var parsed by remember { mutableStateOf<BulkParseResult?>(null) }
    var issues by remember { mutableStateOf<List<BulkIssue>>(emptyList()) }
    var isParsing by remember { mutableStateOf(false) }
    var nextChapterNo by remember { mutableStateOf<Int?>(null) }
    val textFieldFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Nəticə hansı mətn üçün alınıb. `null` — hələ analiz olunmayıb; `raw.text`-dən fərqli — nəticə
    // köhnədir. İkincisi idxalı bağlayır: analizdən sonra redaktə edilmiş mətnlə köhnə plan
    // göndərilsəydi, bazaya ekranda görünəndən başqa sətirlər düşərdi.
    var analyzedText by remember { mutableStateOf<String?>(null) }
    var analyzeRequest by remember { mutableIntStateOf(0) }

    // Kitab boyda mətn redaktə sahəsində qalanda ekran kasır: `OutlinedTextField` `maxLines`-dən
    // asılı olmayaraq bütün mətni layout edir, fokuslananda isə onu bütövlükdə IME-yə ötürür —
    // cihazda ölçdüm, klaviatura açılanda «Skipped 53 frames». Ona görə böyük mətn default olaraq
    // yığcam göstərilir və redaktə yalnız istənəndə açılır (xətaya tullanma onu özü açır).
    var isEditing by remember { mutableStateOf(false) }
    var pendingJump by remember { mutableStateOf(false) }
    val isHuge = raw.text.length > BulkFieldInlineLimit
    val showField = !isHuge || isEditing

    var isImporting by remember { mutableStateOf(false) }
    var isUndoing by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }

    // İdxalın arxasında qoyduğu iz — «Geri al» məhz bunu silir. Yarımçıq idxaldan sonra təkrar
    // cəhd olarsa toplanır, çünki hər cəhd öz sətirlərini yazır.
    var written by remember { mutableStateOf(HadithViewModel.BulkWritten()) }
    var imported by remember { mutableStateOf(false) }

    // Yazılandan sonra həm növbəti bab nömrəsi, həm də mövcud adlar dəyişir — ikisi də yenidən oxunur.
    var refreshKey by remember { mutableIntStateOf(0) }
    var existingChapterNames by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Yarımçıq idxaldan sonra yazılmamış sətirlər: təkrar «İdxal et» bütün planı yox, məhz bunları
    // göndərir. Hədis sətrinin `id`-si yoxdur — eyni planı ikinci dəfə göndərmək artıq yazılmış
    // hədisləri surətləyərdi.
    var pendingRows by remember { mutableStateOf<List<BulkRow>?>(null) }

    // Mətni dəyişən hər yol buradan keçir — köhnə avtomatik təhlil bu sıfırlamaları özü edirdi.
    // `onJump` isə keçmir: o yalnız seçimi tərpədir, mətn eyni qalır.
    val setRaw: (TextFieldValue) -> Unit = { next ->
        if (next.text != raw.text) {
            pendingRows = null
            // Mətn dəyişdisə bu artıq başqa idxaldır; yazılanların izi («Geri al») isə qalır.
            imported = false
            // Sahə boşaldılıbsa köhnə nəticəni «köhnədir» etiketi ilə saxlamağın mənası yoxdur —
            // silinmiş mətnin önizləməsi ekranda qalardı.
            if (next.text.isBlank()) {
                analyzedText = null
                parsed = null
                issues = emptyList()
            }
        }
        raw = next
    }

    LaunchedEffect(bookSlug, refreshKey) {
        nextChapterNo = viewModel.getNextNumber(EditorType.CHAPTER, null, bookSlug, null, null)
        existingChapterNames = viewModel.getChapterNames(bookSlug)
    }

    // Təhlil mətnə yox, düyməyə bağlıdır. Əvvəllər açar `raw.text` idi, yəni hər hərf bütün kitabı
    // yenidən oxudurdu — kitab boyda yapışdırmada ekran donur, böyüyəndə tətbiq çökürdü.
    // `LaunchedEffect`-in ləğvi burada kömək etmirdi: aşağıdakı iki funksiyanın suspension point-i
    // yoxdur, yəni ləğv onları dayandırmır, işlər sadəcə Main-də növbəyə düşür.
    // Açar sayğacdır, mətn deyil: eyni mətni ikinci dəfə analiz etmək də mümkün olsun.
    LaunchedEffect(analyzeRequest) {
        if (analyzeRequest == 0) return@LaunchedEffect
        val text = raw.text
        val names = existingChapterNames
        if (text.isBlank()) {
            // Düymə boş mətndə onsuz da sönükdür; bura yalnız yarışa qarşı qalır və `setRaw`-un boş
            // sahə üçün etdiyi sıfırlamanın eynisini edir.
            parsed = null
            issues = emptyList()
            analyzedText = null
            isParsing = false
            return@LaunchedEffect
        }
        isParsing = true
        // Hər ikisi tam mətn üzərində sinxron keçiddir, ona görə CPU dispetçerinə çıxarılır;
        // `resolveBulkVerses` onsuz da özü `Dispatchers.IO`-ya keçir.
        val checked = withContext(Dispatchers.Default) { validateHadithBulk(text, names) }
        val result = resolveBulkVerses(
            withContext(Dispatchers.Default) { parseHadithBulk(text) },
            translationFactory,
        )
        issues = checked
        parsed = result
        analyzedText = text
        isParsing = false
    }

    // Mövcud bab adları bazadan gec gəlir və hər idxaldan sonra dəyişir. Analiz artıq olubsa yalnız
    // yoxlama təkrarlanır (təhlil yox, o bu adlardan asılı deyil) — «bu bab kitabda artıq var»
    // xəbərdarlığı olmasa eyni kitab ikinci dəfə idxal olunur və heç nə toqquşmur.
    LaunchedEffect(existingChapterNames) {
        val text = analyzedText
        if (text.isNullOrBlank()) return@LaunchedEffect
        issues = withContext(Dispatchers.Default) { validateHadithBulk(text, existingChapterNames) }
    }

    // Xətaya tullanmanın ikinci yarısı. `showField` açardır: yığcam rejimdən açılan sahə
    // kompozisiyaya düşən kimi effekt yenidən işə düşsün.
    LaunchedEffect(pendingJump, showField) {
        if (!pendingJump || !showField) return@LaunchedEffect
        // Sürüşmə şərtdir: `LazyColumn` yalnız görünən elementləri kompozisiya edir, yəni sahə
        // ekrandan kənardadırsa `FocusRequester`-i hələ bağlanmayıb. Seçim isə yalnız fokuslanmış
        // sahədə görünür, sahə də kursoru öz-özünə görünən yerə sürüşdürür.
        listState.scrollToItem(TextFieldItemIndex)
        textFieldFocus.requestFocus()
        pendingJump = false
    }

    val parsedPlan = remember(parsed, nextChapterNo, bookSlug) {
        val entries = parsed?.entries.orEmpty()
        val first = nextChapterNo
        if (entries.isEmpty() || first == null) emptyList()
        else buildBulkPlan(bookSlug, entries, first)
    }
    val plan = pendingRows ?: parsedPlan

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(enabled = !isImporting && !isUndoing, onBack = onBack)

    val doneTemplate = stringResource(Res.string.strMsgBulkImportDone)
    val stoppedMessage = stringResource(Res.string.strMsgBulkImportStopped)
    val failedTemplate = stringResource(Res.string.strMsgBulkImportFailedRows)
    val undoDoneTemplate = stringResource(Res.string.strMsgBulkUndoDone)
    val undoBlockedTemplate = stringResource(Res.string.strMsgBulkUndoBlocked)

    val onImport = {
        progress = 0
        isImporting = true
        viewModel.importBulkRows(
            rows = plan,
            onProgress = { progress = it },
            onResult = { outcome ->
                isImporting = false
                written += outcome.written
                // Yazıldı: növbəti bab nömrəsi də, mövcud bab adları da dəyişdi.
                refreshKey++
                PlatformUtils.showLongToast(
                    doneTemplate
                        .replace("%1\$d", outcome.chapters.toString())
                        .replace("%2\$d", outcome.subChapters.toString())
                        .replace("%3\$d", (outcome.hadiths + outcome.queued).toString())
                )

                if (outcome.remaining.isEmpty()) {
                    // Ekran açıq qalır: geri alma yalnız buradan mümkündür, çıxandan sonra iz itir.
                    pendingRows = null
                    imported = true
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

    val onUndo = {
        progress = 0
        isUndoing = true
        viewModel.undoBulkImport(
            written = written,
            onProgress = { progress = it },
            onResult = { outcome ->
                isUndoing = false
                PlatformUtils.showLongToast(
                    if (outcome.blocked > 0) {
                        undoBlockedTemplate
                            .replace("%1\$d", outcome.removed.toString())
                            .replace("%2\$d", outcome.blocked.toString())
                    } else {
                        undoDoneTemplate.replace("%1\$d", outcome.removed.toString())
                    }
                )
                // Keçid birdəfəlikdir: bloklanan sətirlər RLS ucbatındandır, təkrar cəhd onları
                // yenə buraxmayacaq. İz təmizlənir, ekran isə yenidən idxala hazır olur.
                written = HadithViewModel.BulkWritten()
                imported = false
                pendingRows = null
                refreshKey++
            },
        )
    }

    Scaffold(
        topBar = {
            AppBar(
                title = bookName.ifBlank { stringResource(Res.string.strTitleBulkAdd) },
                onBack = { if (!isImporting && !isUndoing) onBack() },
                actions = {
                    BulkBarActions(
                        // `imported` idxaldan sonra düyməni bağlayır: eyni plan ikinci dəfə
                        // getsəydi kitab olduğu kimi təkrarlanardı — bablar yeni nömrə alır, yəni
                        // heç nə toqquşmur və ikinci nüsxə tamamilə qanuni görünür.
                        // `analyzedText == raw.text` şərtsiz olsa, analizdən sonra redaktə edilmiş
                        // mətnlə köhnə plan göndərilərdi — ekranda görünəndən başqa sətirlər yazılar.
                        canImport = plan.isNotEmpty() && issues.errorCount() == 0 && !imported &&
                            analyzedText == raw.text &&
                            !isImporting && !isUndoing && !isParsing && !isLoading,
                        isBusy = isImporting || isUndoing || isLoading,
                        onCancel = onBack,
                        onImport = onImport,
                    )
                },
            )
        },
    ) { padding ->
        // Lazy siyahının `item` blokları kompozisiya deyil, ona görə burada oxunur. Ərəb şrifti də:
        // sətrin öz içində oxunsaydı hər önizləmə sətri üçün yenidən çağırılardı.
        val previewTitle = stringResource(Res.string.strLabelBulkPreview)
        val nothingParsed = stringResource(Res.string.strMsgBulkNothingParsed)
        val arabicFontFamily = hadithArabicFontFamily(HadithPreferences.observeArabicFont())
        val hasAnalysis = analyzedText != null
        val isStale = hasAnalysis && analyzedText != raw.text

        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(bottom = 32.dp + mainBottomNavigationOuterHeight()),
        ) {
            item {
                EditorSection(title = stringResource(Res.string.strLabelBulkFormat)) {
                    Text(
                        text = stringResource(Res.string.strMsgBulkFormatHelp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface.alpha(0.75f),
                    )
                }
            }

            // ⚠️ Bu elementin indeksi [TextFieldItemIndex]-dir və yoxlama panelinin tullanması ona
            // sürüşür — yuxarısına şərtli element əlavə etsən, tullanma yanlış yerə düşər.
            item {
                EditorSection(title = stringResource(Res.string.strLabelBulkText)) {
                    if (showField) {
                        FormTextField(
                            value = raw,
                            onValueChange = setRaw,
                            label = stringResource(Res.string.strLabelBulkText),
                            placeholder = stringResource(Res.string.strHintBulkText),
                            icon = Res.drawable.dr_icon_footnote,
                            modifier = Modifier.focusRequester(textFieldFocus),
                            minLines = 8,
                            maxLines = 24,
                            readOnly = isImporting || isUndoing,
                            onClear = { setRaw(TextFieldValue()) },
                            onPaste = { setRaw(TextFieldValue(it, TextRange(it.length))) },
                        )

                        if (isHuge) {
                            BulkCollapseAction(
                                enabled = !isImporting && !isUndoing,
                                onCollapse = { isEditing = false },
                            )
                        }
                    } else {
                        // Yapışdırma düyməsi burada lazım deyil: mətn təmizlənən kimi sahə adi
                        // (boş) rejimə qayıdır və `FormTextField`-in öz «yapışdır» ikonu çıxır.
                        BulkTextSummary(
                            text = raw.text,
                            enabled = !isImporting && !isUndoing,
                            onEdit = { isEditing = true },
                            onClear = { setRaw(TextFieldValue()) },
                        )
                    }

                    BulkAnalyzeBar(
                        length = raw.text.length,
                        hasAnalysis = hasAnalysis,
                        isStale = isStale,
                        isBusy = isParsing,
                        enabled = raw.text.isNotBlank() && !isParsing && !isImporting && !isUndoing,
                        onAnalyze = { analyzeRequest++ },
                    )
                }
            }

            if (isImporting || isUndoing) {
                item {
                    BulkProgress(
                        done = progress,
                        total = if (isUndoing) written.total else plan.size,
                    )
                }
            }

            if (!written.isEmpty && !isImporting) {
                item {
                    BulkUndoSection(
                        written = written,
                        canFinish = imported,
                        isBusy = isUndoing || isLoading,
                        onUndo = onUndo,
                        onFinish = onBack,
                    )
                }
            }

            if (hasAnalysis && !isParsing) {
                item {
                    BulkCheckSection(
                        issues = issues,
                        onJump = { issue ->
                            val length = raw.text.length
                            raw = raw.copy(
                                selection = TextRange(
                                    issue.start.coerceIn(0, length),
                                    issue.end.coerceIn(0, length),
                                ),
                            )
                            // Tullanmaq redaktə deməkdir, ona görə yığcam mətn burada açılır.
                            // Fokus elə bu anda istənə bilməz: sahə ya hələ kompozisiyada yoxdur
                            // (yığcam rejim), ya da lazy siyahıda ekrandan kənarda qalıb — hər iki
                            // halda `requestFocus()` bağlanmamış requester-də partlayır. Qalanını
                            // aşağıdakı effekt edir.
                            isEditing = true
                            pendingJump = true
                        },
                    )
                }
            }

            when {
                isParsing -> item { Loader(false) }

                !hasAnalysis -> Unit

                plan.isEmpty() -> item {
                    EditorSection(title = previewTitle) {
                        BulkProblemRow(nothingParsed)
                        parsed?.problems?.forEach { BulkProblemRow(it.describe()) }
                    }
                }

                else -> {
                    item { ListItemCategoryLabel(title = previewTitle) }

                    item {
                        BulkPreviewSlice(isFirst = true, isLast = false) {
                            BulkSummary(parsed)
                            parsed?.problems?.forEach { BulkProblemRow(it.describe()) }
                        }
                    }

                    itemsIndexed(plan) { index, row ->
                        BulkPreviewSlice(isFirst = false, isLast = index == plan.lastIndex) {
                            BulkPreviewEntry(row = row, arabicFontFamily = arabicFontFamily)
                        }
                    }
                }
            }
        }
    }
}

/** Mətn sahəsinin lazy siyahıdakı sabit yeri — yoxlama panelinin tullanması bura sürüşür. */
private const val TextFieldItemIndex = 1

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
 * The guard: every broken rule the paste carries, and one tap to the place it broke.
 *
 * It sits between the text and the preview rather than under it, because the preview of a paste with
 * a skipped `2§` looks perfectly reasonable — the rows are there, one of them is simply missing its
 * translation and the next one has the wrong source. The panel is what says so.
 *
 * Errors hold the import button down until they are gone; warnings are only shown. See
 * [validateHadithBulk] for which is which.
 */
@Composable
private fun BulkCheckSection(issues: List<BulkIssue>, onJump: (BulkIssue) -> Unit) {
    EditorSection(title = stringResource(Res.string.strLabelBulkCheck)) {
        if (issues.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.strMsgBulkCheckClean),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.alpha(0.75f),
                )
            }
            return@EditorSection
        }

        val errors = issues.errorCount()

        Text(
            text = stringResource(Res.string.strMsgBulkCheckCounts, errors, issues.warningCount()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (errors > 0) colorScheme.error else colorScheme.onSurface,
        )

        if (errors > 0) {
            Text(
                text = stringResource(Res.string.strMsgBulkCheckBlocked),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.alpha(0.75f),
            )
        }

        Spacer(Modifier.height(2.dp))

        // Sıralama: əvvəlcə xətalar, sonra xəbərdarlıqlar — hər ikisi sətir sırası ilə. İdxalı
        // bloklayan şey siyahının başında olmalıdır, yoxsa yüz xəbərdarlığın altında qalır.
        val shown = issues
            .sortedWith(compareBy({ it.level != BulkIssueLevel.ERROR }, { it.line }))
            .take(BulkIssueLimit)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            shown.forEach { issue ->
                BulkIssueRow(issue = issue, onClick = { onJump(issue) })
            }

            if (issues.size > shown.size) {
                Text(
                    text = stringResource(Res.string.strMsgBulkMoreIssues, issues.size - shown.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.alpha(0.6f),
                )
            }
        }
    }
}

/**
 * One issue as a button: what is wrong, the line it is on, and the line itself.
 *
 * The whole row is the tap target — the arrow is there to say the row goes somewhere, not to be
 * aimed at. The offending line is quoted underneath because the fix is usually obvious from it, and
 * reading it here beats jumping into a thousand-line field to find out what the message meant.
 */
@Composable
private fun BulkIssueRow(issue: BulkIssue, onClick: () -> Unit) {
    val isError = issue.level == BulkIssueLevel.ERROR
    val container = if (isError) colorScheme.errorContainer else colorScheme.tertiaryContainer
    val content = if (isError) colorScheme.onErrorContainer else colorScheme.onTertiaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(container.alpha(0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.strMsgBulkIssueLine, issue.line),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = content.alpha(0.7f),
                )
                Text(
                    text = issue.describe(),
                    style = MaterialTheme.typography.bodySmall,
                    color = content,
                )
            }

            if (issue.lineText.isNotBlank()) {
                Text(
                    text = issue.lineText,
                    style = MaterialTheme.typography.labelSmall,
                    color = content.alpha(0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Icon(
            painter = painterResource(Res.drawable.dr_icon_chevron_right),
            contentDescription = stringResource(Res.string.strActionBulkJump),
            modifier = Modifier.size(16.dp),
            tint = content.alpha(0.8f),
        )
    }
}

@Composable
private fun BulkIssue.describe(): String = when (val issue = kind) {
    is BulkIssueKind.OutOfOrder ->
        stringResource(Res.string.strMsgBulkIssueOutOfOrder, issue.expected, issue.found)

    is BulkIssueKind.Repeated -> stringResource(Res.string.strMsgBulkIssueRepeated, issue.label)

    is BulkIssueKind.Incomplete ->
        stringResource(Res.string.strMsgBulkIssueIncomplete, issue.missing.joinToString(", "))

    is BulkIssueKind.LatinInArabic ->
        stringResource(Res.string.strMsgBulkIssueLatinInArabic, issue.sample)

    is BulkIssueKind.ArabicInLatin ->
        stringResource(Res.string.strMsgBulkIssueArabicInLatin, issue.sample)

    is BulkIssueKind.EmptyField -> stringResource(Res.string.strMsgBulkIssueEmptyField, issue.label)

    is BulkIssueKind.MissingLatinName ->
        stringResource(Res.string.strMsgBulkIssueMissingLatinName, issue.label)

    is BulkIssueKind.MissingArabicName ->
        stringResource(Res.string.strMsgBulkIssueMissingArabicName, issue.label)

    is BulkIssueKind.DuplicateHadith ->
        stringResource(Res.string.strMsgBulkIssueDuplicateHadith, issue.firstLine)

    is BulkIssueKind.DuplicateChapter ->
        stringResource(Res.string.strMsgBulkIssueDuplicateChapter, issue.firstLine)

    BulkIssueKind.ChapterExists -> stringResource(Res.string.strMsgBulkIssueChapterExists)
}

/**
 * What the import left behind, and the one chance to take it back.
 *
 * The screen no longer leaves by itself when the import finishes, and this panel is why: the rows
 * are already in the database, and the only thing that knows which of them this paste wrote is this
 * composition. Walk away and the undo is gone — a bab imported under the wrong parent then has to be
 * deleted by hand, one row at a time, in an editor that asks for confirmation on each.
 *
 * «Geri al» deletes children before parents; a row it cannot remove is counted rather than hidden,
 * because deleting is admin-only and an editor's undo would otherwise look like it worked.
 */
@Composable
private fun BulkUndoSection(
    written: HadithViewModel.BulkWritten,
    canFinish: Boolean,
    isBusy: Boolean,
    onUndo: () -> Unit,
    onFinish: () -> Unit,
) {
    EditorSection(title = stringResource(Res.string.strLabelBulkImported)) {
        Text(
            text = stringResource(
                Res.string.strMsgBulkImportDone,
                written.chapters.size,
                written.subChapters.size,
                written.hadiths.size,
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(Res.string.strMsgBulkUndoHint),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.alpha(0.75f),
        )

        Spacer(Modifier.height(2.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onUndo,
                enabled = !isBusy,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.errorContainer,
                    contentColor = colorScheme.onErrorContainer,
                ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_undo),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.strActionUndo),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (canFinish) {
                Button(
                    onClick = onFinish,
                    enabled = !isBusy,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.strLabelDone),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * The paste as a folded block: how much of it there is, and enough of the opening to recognise it by.
 *
 * A book-sized paste cannot sit in an editable field. `OutlinedTextField` lays the whole string out
 * whatever `maxLines` says, and on focus it hands all of it to the IME — measured on the device, that
 * is «Skipped 53 frames» every time the keyboard opens, and the stutter stopped only when the import
 * turned the field read-only. Nothing is lost by folding it: [onEdit] opens the real field, and the
 * check panel opens it by itself when a line actually has to be fixed.
 */
@Composable
private fun BulkTextSummary(
    text: String,
    enabled: Boolean,
    onEdit: () -> Unit,
    onClear: () -> Unit,
) {
    val excerpt = remember(text) {
        text.take(BulkSummaryChars).split('\n').joinToString(" ") { it.trim() }.trim()
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(Res.string.strMsgBulkTextCollapsed),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.alpha(0.75f),
        )

        Text(
            text = "$excerpt …",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.alpha(0.6f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onEdit,
                enabled = enabled,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = stringResource(Res.string.strLabelEdit),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            TextButton(onClick = onClear, enabled = enabled) {
                Text(
                    text = stringResource(Res.string.clear),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** The way back out of the heavy field, once a big paste has been opened for editing. */
@Composable
private fun BulkCollapseAction(enabled: Boolean, onCollapse: () -> Unit) {
    TextButton(onClick = onCollapse, enabled = enabled) {
        Text(
            text = stringResource(Res.string.strActionBulkCollapseText),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * The button the rest of the screen now hangs off, with the two things that have to be said next to
 * it: how big the paste is, and whether what is shown below still belongs to it.
 *
 * Nothing is read while typing any more — a book-sized paste re-parsed on every keystroke froze the
 * screen and took the app down on the larger ones. That moves a burden onto the UI: a stale preview
 * looks exactly like a fresh one, so it has to say so, and the import button stays down until it is
 * fresh again.
 */
@Composable
private fun BulkAnalyzeBar(
    length: Int,
    hasAnalysis: Boolean,
    isStale: Boolean,
    isBusy: Boolean,
    enabled: Boolean,
    onAnalyze: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(Res.string.strMsgBulkTextLength, length),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.alpha(0.6f),
            )

            when {
                isStale -> Text(
                    text = stringResource(Res.string.strMsgBulkAnalyzeStale),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )

                !hasAnalysis -> Text(
                    text = stringResource(Res.string.strMsgBulkAnalyzeHint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.alpha(0.75f),
                )
            }
        }

        Button(
            onClick = onAnalyze,
            enabled = enabled,
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            if (isBusy) {
                Loader(size = 18.dp)
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_search),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.strActionBulkAnalyze),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * One slice of the card the preview would otherwise be wrapped in.
 *
 * The rows are items of the screen's own lazy list — that is the whole point, a book-sized paste
 * must not compose all of its rows at once — so no single container can be drawn around them. Each
 * slice paints the same surface instead and only the first and last round their corners, which reads
 * as one card while staying one row per item.
 */
@Composable
private fun BulkPreviewSlice(
    isFirst: Boolean,
    isLast: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val square = CornerSize(0.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(
                RoundedCornerShape(
                    topStart = if (isFirst) shape.topStart else square,
                    topEnd = if (isFirst) shape.topEnd else square,
                    bottomEnd = if (isLast) shape.bottomEnd else square,
                    bottomStart = if (isLast) shape.bottomStart else square,
                )
            )
            .background(colorScheme.surfaceContainerLow)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (isFirst) 16.dp else 0.dp,
                bottom = if (isLast) 16.dp else 6.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

/** One planned row as its preview line — the numbers and indents the import will actually use. */
@Composable
private fun BulkPreviewEntry(row: BulkRow, arabicFontFamily: FontFamily) {
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

/** Enough of the list to work through in one pass; the count above it stays honest either way. */
private const val BulkIssueLimit = 40

/**
 * Above this many characters the paste is folded instead of being kept in an editable field.
 *
 * Far below a book on purpose: the cost is the field's own text layout and its hand-off to the IME,
 * both of which grow with the whole string and neither of which `maxLines` bounds.
 */
private const val BulkFieldInlineLimit = 4000

/** How much of the folded text is shown — enough to tell one paste from another. */
private const val BulkSummaryChars = 300

private const val BulkPreviewLines = 3
private const val BulkPreviewInlineLimit = 260
private const val BulkPreviewTailChars = 110
