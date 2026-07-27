package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.add_sub_chapter
import com.cafarovceyxun.anamuslim.resources.arabic_text
import com.cafarovceyxun.anamuslim.resources.az_translation
import com.cafarovceyxun.anamuslim.resources.clear
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_footnote
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_mic
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.hadith_number
import com.cafarovceyxun.anamuslim.resources.ic_lock_keyhole_closed
import com.cafarovceyxun.anamuslim.resources.name_az
import com.cafarovceyxun.anamuslim.resources.order_no
import com.cafarovceyxun.anamuslim.resources.placeholder_book_name
import com.cafarovceyxun.anamuslim.resources.placeholder_hadith_ar
import com.cafarovceyxun.anamuslim.resources.placeholder_hadith_az
import com.cafarovceyxun.anamuslim.resources.placeholder_note
import com.cafarovceyxun.anamuslim.resources.placeholder_slug
import com.cafarovceyxun.anamuslim.resources.placeholder_source
import com.cafarovceyxun.anamuslim.resources.save
import com.cafarovceyxun.anamuslim.resources.slug_system_name
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.strActionPickVerseReference
import com.cafarovceyxun.anamuslim.resources.strActionUndo
import com.cafarovceyxun.anamuslim.resources.strHintVolumeAuthor
import com.cafarovceyxun.anamuslim.resources.strHintVolumeDescription
import com.cafarovceyxun.anamuslim.resources.strLabelAdditionalInfo
import com.cafarovceyxun.anamuslim.resources.strLabelBasicInfo
import com.cafarovceyxun.anamuslim.resources.strHintNameAr
import com.cafarovceyxun.anamuslim.resources.strLabelHadithInfo
import com.cafarovceyxun.anamuslim.resources.strLabelNameAr
import com.cafarovceyxun.anamuslim.resources.strLabelOptional
import com.cafarovceyxun.anamuslim.resources.strLabelTexts
import com.cafarovceyxun.anamuslim.resources.strLabelVolumeAuthor
import com.cafarovceyxun.anamuslim.resources.strLabelVolumeDescription
import com.cafarovceyxun.anamuslim.resources.strMsgFieldCleared
import com.cafarovceyxun.anamuslim.resources.strMsgFieldRequired
import com.cafarovceyxun.anamuslim.resources.strMsgSlugLocked
import com.cafarovceyxun.anamuslim.resources.strTitleAddBab
import com.cafarovceyxun.anamuslim.resources.strTitleAddBook
import com.cafarovceyxun.anamuslim.resources.strTitleAddHadith
import com.cafarovceyxun.anamuslim.resources.strTitleAddVolume
import com.cafarovceyxun.anamuslim.resources.strTitleEditBab
import com.cafarovceyxun.anamuslim.resources.strTitleEditBook
import com.cafarovceyxun.anamuslim.resources.strTitleEditHadith
import com.cafarovceyxun.anamuslim.resources.strTitleEditSubBab
import com.cafarovceyxun.anamuslim.resources.strTitleEditVolume
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Create/edit surface for every hadith entity.
 *
 * Passing one of the `initial*` records switches the screen into edit mode: the fields start filled
 * and the slug — the primary key every table upserts on — is shown locked, so saving updates the
 * existing row instead of creating a second one under a new slug.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithEditorScreen(
    type: EditorType,
    initialHadith: Hadith? = null,
    initialVolume: HadithVolume? = null,
    initialBook: HadithBook? = null,
    initialChapter: HadithChapter? = null,
    initialSubChapter: HadithSubChapter? = null,
    volumeSlug: String? = null,
    bookSlug: String? = null,
    chapterSlug: String? = null,
    subChapterSlug: String? = null,
    reserveBottomSpace: Boolean = true,
    onBack: () -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val isLoading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    val isEditing = initialHadith != null || initialVolume != null || initialBook != null ||
        initialChapter != null || initialSubChapter != null

    var name by remember {
        mutableStateOf(
            initialVolume?.name ?: initialBook?.name ?: initialChapter?.name
            ?: initialSubChapter?.name ?: ""
        )
    }
    // Optional across every level: the Arabic name of the volume/book/bab, kept beside the
    // Azerbaijani one rather than replacing it.
    var nameAr by remember {
        mutableStateOf(
            initialVolume?.name_ar ?: initialBook?.name_ar ?: initialChapter?.name_ar
            ?: initialSubChapter?.name_ar ?: ""
        )
    }
    var slugPart by remember {
        mutableStateOf(
            initialVolume?.slug ?: initialBook?.slug ?: initialChapter?.slug
            ?: initialSubChapter?.slug ?: ""
        )
    }
    var no by remember {
        mutableStateOf(
            initialHadith?.hadith_no?.toString()
                ?: initialBook?.book_no?.toString()
                ?: initialChapter?.chapter_no?.toString()
                ?: initialSubChapter?.sub_chapter_no?.toString()
                ?: ""
        )
    }

    // Volume-only. `hadith_volume` has carried these columns all along and the index screen renders
    // them, but the editor never collected them — so every volume was saved with a null author.
    var author by remember { mutableStateOf(initialVolume?.author ?: "") }
    var description by remember { mutableStateOf(initialVolume?.description ?: "") }

    var textAr by remember { mutableStateOf(initialHadith?.text_ar ?: "") }
    var textAz by remember { mutableStateOf(initialHadith?.text_az ?: "") }
    var source by remember { mutableStateOf(initialHadith?.source ?: "") }
    var note by remember { mutableStateOf(initialHadith?.note ?: "") }

    var showError by remember { mutableStateOf(value = false) }
    var isSlugManuallyEdited by remember { mutableStateOf(false) }
    var showQuranReferencePicker by remember { mutableStateOf(value = false) }

    // Clearing a field is one tap and the fields hold up to fifteen lines of hadith text, so every
    // clear is offered back through the snackbar instead of being final.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clearedMessage = stringResource(Res.string.strMsgFieldCleared)
    val undoLabel = stringResource(Res.string.strActionUndo)

    val clearWithUndo: (String, (String) -> Unit) -> Unit = { previous, setValue ->
        setValue("")
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = clearedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) setValue(previous)
        }
    }

    val selectedFont = HadithPreferences.observeArabicFont()
    val arabicFontFamily = hadithArabicFontFamily(selectedFont)

    val namedType = type != EditorType.HADITH
    // A volume has no number column of its own; its order number only seeds the slug, so it is
    // neither shown nor required once the slug is fixed.
    val numberedType = type == EditorType.BOOK || type == EditorType.CHAPTER ||
        type == EditorType.SUB_CHAPTER || type == EditorType.HADITH
    val showNumberField = numberedType || !isEditing

    val nameError = showError && namedType && name.isBlank()
    val slugError = showError && namedType && slugPart.isBlank()
    val numberError = showError && numberedType && no.isBlank()

    // Fetch next number on start
    LaunchedEffect(type, isEditing, volumeSlug, bookSlug, chapterSlug, subChapterSlug) {
        if (!isEditing && (no.isEmpty() || no == "0")) {
            val next = viewModel.getNextNumber(type, volumeSlug, bookSlug, chapterSlug, subChapterSlug)
            no = next.toString()
        }
    }

    // Auto-slug logic. Never while editing: the slug is the row's identity there.
    LaunchedEffect(name, no, type, volumeSlug, bookSlug, chapterSlug, isEditing) {
        if (!isSlugManuallyEdited && !isEditing) {
            val prefix = when (type) {
                EditorType.VOLUME -> "c"
                else -> {
                    val cleanName = name.trim().toSlugPart()
                    if (cleanName.length >= 2) cleanName.take(2)
                    else cleanName
                }
            }
            if (prefix.isNotEmpty() && no.isNotEmpty()) {
                val currentPart = prefix + no
                slugPart = when (type) {
                    EditorType.VOLUME -> currentPart
                    EditorType.BOOK -> (volumeSlug ?: "") + currentPart
                    EditorType.CHAPTER -> (bookSlug?.replace("/", "") ?: "") + currentPart
                    EditorType.SUB_CHAPTER -> (chapterSlug?.replace("/", "") ?: "") + currentPart
                    else -> slugPart
                }
            }
        }
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(onBack = onBack)

    val onSave = save@{
        val invalid = (namedType && (name.isBlank() || slugPart.isBlank())) ||
            (numberedType && no.isBlank())
        if (invalid) {
            showError = true
            return@save
        }

        focusManager.clearFocus()
        val slug = slugPart.trim()

        when (type) {
            EditorType.VOLUME -> viewModel.upsertVolume(
                HadithVolume(
                    slug = slug,
                    name = name.trim(),
                    name_ar = nameAr.trim().ifBlank { null },
                    author = author.trim().ifBlank { null },
                    description = description.trim().ifBlank { null },
                ),
                onBack,
            )

            EditorType.BOOK -> viewModel.upsertBook(
                HadithBook(
                    slug = slug,
                    volume_slug = initialBook?.volume_slug ?: volumeSlug.orEmpty(),
                    book_no = no.toIntOrNull() ?: 0,
                    name = name.trim(),
                    name_ar = nameAr.trim().ifBlank { null },
                ),
                onBack,
            )

            EditorType.CHAPTER -> viewModel.upsertChapter(
                HadithChapter(
                    slug = slug,
                    book_slug = initialChapter?.book_slug ?: bookSlug.orEmpty(),
                    chapter_no = no.toIntOrNull() ?: 0,
                    name = name.trim(),
                    name_ar = nameAr.trim().ifBlank { null },
                ),
                onBack,
            )

            EditorType.SUB_CHAPTER -> viewModel.upsertSubChapter(
                HadithSubChapter(
                    slug = slug,
                    chapter_slug = initialSubChapter?.chapter_slug ?: chapterSlug.orEmpty(),
                    sub_chapter_no = no.toIntOrNull() ?: 0,
                    name = name.trim(),
                    name_ar = nameAr.trim().ifBlank { null },
                ),
                onBack,
            )

            EditorType.HADITH -> viewModel.upsertHadith(
                Hadith(
                    id = initialHadith?.id,
                    chapter_slug = chapterSlug ?: initialHadith?.chapter_slug,
                    sub_chapter_slug = subChapterSlug ?: initialHadith?.sub_chapter_slug,
                    hadith_no = no.toIntOrNull() ?: 0,
                    text_ar = textAr,
                    text_az = textAz,
                    source = source,
                    note = note,
                ),
                onBack,
            )
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = editorTitle(type, isEditing),
                onBack = onBack,
            )
        },
        bottomBar = {
            SaveBar(
                isLoading = isLoading,
                reserveBottomSpace = reserveBottomSpace,
                onSave = onSave,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (namedType) {
                EditorSection(title = stringResource(Res.string.strLabelBasicInfo)) {
                    FormTextField(
                        value = name,
                        onValueChange = { name = it; showError = false },
                        label = stringResource(Res.string.name_az),
                        placeholder = stringResource(Res.string.placeholder_book_name),
                        icon = Res.drawable.dr_icon_edit,
                        error = nameError,
                        errorText = stringResource(Res.string.strMsgFieldRequired),
                        maxLines = 3,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        onClear = { clearWithUndo(name) { name = it } },
                    )

                    FormTextField(
                        value = nameAr,
                        onValueChange = { nameAr = it },
                        label = stringResource(Res.string.strLabelNameAr),
                        placeholder = stringResource(Res.string.strHintNameAr),
                        icon = Res.drawable.dr_icon_read_quran,
                        supportingText = stringResource(Res.string.strLabelOptional),
                        maxLines = 2,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Rtl,
                            fontFamily = arabicFontFamily,
                        ),
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        onClear = { clearWithUndo(nameAr) { nameAr = it } },
                    )

                    if (type == EditorType.VOLUME) {
                        FormTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = stringResource(Res.string.strLabelVolumeAuthor),
                            placeholder = stringResource(Res.string.strHintVolumeAuthor),
                            icon = Res.drawable.dr_icon_mic,
                            supportingText = stringResource(Res.string.strLabelOptional),
                            imeAction = ImeAction.Next,
                            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                            onClear = { clearWithUndo(author) { author = it } },
                        )

                        FormTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = stringResource(Res.string.strLabelVolumeDescription),
                            placeholder = stringResource(Res.string.strHintVolumeDescription),
                            icon = Res.drawable.dr_icon_footnote,
                            supportingText = stringResource(Res.string.strLabelOptional),
                            minLines = 2,
                            maxLines = 5,
                            onClear = { clearWithUndo(description) { description = it } },
                        )
                    }

                    FormTextField(
                        value = slugPart,
                        onValueChange = {
                            slugPart = it
                            isSlugManuallyEdited = true
                            showError = false
                        },
                        label = stringResource(Res.string.slug_system_name),
                        placeholder = stringResource(Res.string.placeholder_slug),
                        icon = if (isEditing) Res.drawable.ic_lock_keyhole_closed else Res.drawable.dr_icon_info,
                        readOnly = isEditing,
                        supportingText = if (isEditing) stringResource(Res.string.strMsgSlugLocked) else null,
                        error = slugError,
                        errorText = stringResource(Res.string.strMsgFieldRequired),
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        onClear = {
                            clearWithUndo(slugPart) { slugPart = it }
                            isSlugManuallyEdited = true
                        },
                    )

                    if (showNumberField) {
                        FormTextField(
                            value = no,
                            onValueChange = { no = it; showError = false },
                            label = stringResource(Res.string.order_no),
                            placeholder = "0",
                            icon = Res.drawable.dr_icon_quran_script,
                            keyboardType = KeyboardType.Number,
                            error = numberError,
                            errorText = stringResource(Res.string.strMsgFieldRequired),
                            imeAction = ImeAction.Done,
                            onImeAction = { focusManager.clearFocus() },
                            onClear = { clearWithUndo(no) { no = it } },
                        )
                    }
                }
            } else {
                EditorSection(title = stringResource(Res.string.strLabelHadithInfo)) {
                    FormTextField(
                        value = no,
                        onValueChange = { no = it; showError = false },
                        label = stringResource(Res.string.hadith_number),
                        placeholder = "0",
                        icon = Res.drawable.dr_icon_quran_script,
                        keyboardType = KeyboardType.Number,
                        error = numberError,
                        errorText = stringResource(Res.string.strMsgFieldRequired),
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        onClear = { clearWithUndo(no) { no = it } },
                    )
                }

                EditorSection(title = stringResource(Res.string.strLabelTexts)) {
                    FormTextField(
                        value = textAr,
                        onValueChange = { textAr = it },
                        label = stringResource(Res.string.arabic_text),
                        placeholder = stringResource(Res.string.placeholder_hadith_ar),
                        minLines = 4,
                        maxLines = 15,
                        icon = Res.drawable.dr_icon_read_quran,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = TextDirection.Rtl,
                            fontFamily = arabicFontFamily,
                            fontSize = 20.sp
                        ),
                        onClear = { clearWithUndo(textAr) { textAr = it } },
                    )

                    FormTextField(
                        value = textAz,
                        onValueChange = { textAz = it },
                        label = stringResource(Res.string.az_translation),
                        placeholder = stringResource(Res.string.placeholder_hadith_az),
                        minLines = 4,
                        maxLines = 15,
                        icon = Res.drawable.dr_icon_translations,
                        onClear = { clearWithUndo(textAz) { textAz = it } },
                    )
                }

                EditorSection(title = stringResource(Res.string.strLabelAdditionalInfo)) {
                    FormTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = stringResource(Res.string.source),
                        placeholder = stringResource(Res.string.placeholder_source),
                        icon = Res.drawable.dr_icon_share,
                        maxLines = 3,
                        topEndAction = {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    showQuranReferencePicker = true
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_read_quran),
                                    contentDescription = stringResource(Res.string.strActionPickVerseReference),
                                    modifier = Modifier.size(18.dp),
                                    tint = colorScheme.primary,
                                )
                            }
                        },
                        onClear = { clearWithUndo(source) { source = it } },
                    )

                    FormTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(Res.string.strTitleNote),
                        placeholder = stringResource(Res.string.placeholder_note),
                        minLines = 2,
                        maxLines = 10,
                        icon = Res.drawable.dr_icon_info,
                        onClear = { clearWithUndo(note) { note = it } },
                    )
                }
            }
        }
    }

    QuranReferencePickerSheet(
        isOpen = showQuranReferencePicker,
        onDismiss = { showQuranReferencePicker = false },
        onAdd = { insertion ->
            source = source.withReferenceAdded(insertion.reference)
            insertion.arabic?.let { textAr = textAr.withParagraphAdded(it) }
            insertion.translation?.let { textAz = textAz.withParagraphAdded(it) }
        },
        onRemove = { insertion ->
            source = source.withReferenceRemoved(insertion.reference)
            insertion.arabic?.let { textAr = textAr.withParagraphRemoved(it) }
            insertion.translation?.let { textAz = textAz.withParagraphRemoved(it) }
        },
    )
}

/** Appends verse text below whatever is already in the field, separated by a blank line. */
internal fun String.withParagraphAdded(paragraph: String): String {
    val current = trimEnd()
    return if (current.isEmpty()) paragraph else "$current\n\n$paragraph"
}

/** Undo for [withParagraphAdded]: drops the paragraph and the blank line it came with. */
internal fun String.withParagraphRemoved(paragraph: String): String {
    val start = indexOf(paragraph)
    if (start < 0) return this

    var end = start + paragraph.length
    while (end < length && (this[end] == '\n' || this[end] == ' ')) end++

    var head = start
    if (end >= length) {
        while (head > 0 && (this[head - 1] == '\n' || this[head - 1] == ' ')) head--
    }

    return (take(head) + substring(end)).trim()
}

/** Appends a reference to the source field, keeping whatever the editor typed by hand. */
internal fun String.withReferenceAdded(reference: String): String {
    val current = trimEnd()
    return when {
        current.isEmpty() -> reference
        current.endsWith(",") || current.endsWith(";") -> "$current $reference"
        else -> "$current, $reference"
    }
}

/** Takes a reference back out again, together with the separator it was added with. */
internal fun String.withReferenceRemoved(reference: String): String {
    val start = indexOfReference(reference)
    if (start < 0) return this

    var end = start + reference.length
    while (end < length && (this[end] == ',' || this[end] == ';' || this[end] == ' ')) end++

    var head = start
    // Nothing follows, so the separator that joined this reference sits in front of it instead.
    if (end >= length) {
        while (head > 0 && (this[head - 1] == ',' || this[head - 1] == ';' || this[head - 1] == ' ')) head--
    }

    return (take(head) + substring(end)).trim()
}

/**
 * Finds [reference] only where it stands on its own, so removing `Bəqərə 2:1` never bites into a
 * `Bəqərə 2:1-5` sitting next to it.
 */
private fun String.indexOfReference(reference: String): Int {
    var from = 0
    while (from <= length - reference.length) {
        val index = indexOf(reference, from)
        if (index < 0) return -1

        val before = getOrNull(index - 1)
        val after = getOrNull(index + reference.length)
        val isBounded = (before == null || before == ',' || before == ';' || before == ' ') &&
            (after == null || after == ',' || after == ';' || after == ' ')
        if (isBounded) return index

        from = index + 1
    }
    return -1
}

@Composable
private fun editorTitle(type: EditorType, isEditing: Boolean): String = when (type) {
    EditorType.VOLUME ->
        if (isEditing) stringResource(Res.string.strTitleEditVolume)
        else stringResource(Res.string.strTitleAddVolume)

    EditorType.BOOK ->
        if (isEditing) stringResource(Res.string.strTitleEditBook)
        else stringResource(Res.string.strTitleAddBook)

    EditorType.CHAPTER ->
        if (isEditing) stringResource(Res.string.strTitleEditBab)
        else stringResource(Res.string.strTitleAddBab)

    EditorType.SUB_CHAPTER ->
        if (isEditing) stringResource(Res.string.strTitleEditSubBab)
        else stringResource(Res.string.add_sub_chapter)

    EditorType.HADITH ->
        if (isEditing) stringResource(Res.string.strTitleEditHadith)
        else stringResource(Res.string.strTitleAddHadith)
}

/**
 * Save action, docked to the bottom. It rides above the keyboard rather than being buried under it —
 * the bottom-nav reservation is dropped for exactly as much as the IME already covers.
 */
@Composable
private fun SaveBar(
    isLoading: Boolean,
    reserveBottomSpace: Boolean,
    onSave: () -> Unit,
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val reserved = if (reserveBottomSpace) mainBottomNavigationOuterHeight() else navBarPadding
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val extraBottom = (reserved - imeBottom).coerceAtLeast(0.dp)

    Surface(
        color = colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp)
                .padding(bottom = extraBottom)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    Loader(size = 24.dp)
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_check),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.save),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItemCategoryLabel(title = title)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                // Tighter than it looks: each field now carries its own supporting/error line, so
                // the gap between two fields is this plus that line's height.
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Material 3 text field for the hadith forms: the label floats into the outline instead of sitting
 * above the box as a separate caption, and the field carries its own supporting/error line.
 *
 * Actions ([topEndAction] and the clear button [onClear] enables) sit in the field's top-end corner
 * rather than in the trailing-icon slot: the text fields here are up to fifteen lines tall, and a
 * vertically centred icon would float in the middle of the text. The trailing slot still gets a
 * spacer so the text never runs underneath them.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: DrawableResource,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: Boolean = false,
    errorText: String? = null,
    supportingText: String? = null,
    readOnly: Boolean = false,
    imeAction: ImeAction? = null,
    onImeAction: (() -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    topEndAction: (@Composable () -> Unit)? = null,
    onClear: (() -> Unit)? = null,
) {
    val multiline = minLines > 1 || maxLines > 1
    val resolvedImeAction = imeAction ?: if (multiline) ImeAction.Default else ImeAction.Next
    val helper = if (error) errorText else supportingText

    val showClear = onClear != null && !readOnly && value.isNotEmpty()
    val actionCount = (if (topEndAction != null) 1 else 0) + (if (showClear) 1 else 0)

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            label = { Text(label) },
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyMedium)
                }
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = if (actionCount > 0) {
                { Spacer(Modifier.width((actionCount * FieldActionSize.value).dp)) }
            } else null,
            supportingText = helper?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
            readOnly = readOnly,
            minLines = minLines,
            maxLines = if (maxLines > minLines) maxLines else minLines,
            shape = OutlinedTextFieldDefaults.shape,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = resolvedImeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() },
            ),
            isError = error,
        )

        if (actionCount > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                topEndAction?.invoke()

                if (showClear) {
                    IconButton(
                        onClick = { onClear?.invoke() },
                        modifier = Modifier.size(FieldActionSize),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_close),
                            contentDescription = stringResource(Res.string.clear),
                            modifier = Modifier.size(16.dp),
                            tint = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Touch target of a field action; also what the trailing slot reserves for each of them. */
private val FieldActionSize = 32.dp

private fun String.toSlugPart(): String {
    val azToEn = mapOf(
        'ə' to "e", 'Ə' to "e",
        'ç' to "c", 'Ç' to "c",
        'ğ' to "g", 'Ğ' to "g",
        'ı' to "i", 'I' to "i",
        'i' to "i", 'İ' to "i",
        'ö' to "o", 'Ö' to "o",
        'ş' to "s", 'Ş' to "s",
        'ü' to "u", 'Ü' to "u"
    )

    val transliterated = this.map { azToEn[it] ?: it.lowercaseChar().toString() }.joinToString("")
    val slugified = transliterated.replace(" ", "-").filter { it in 'a'..'z' || it in '0'..'9' || it == '-' }
    return slugified.trim('-')
}
