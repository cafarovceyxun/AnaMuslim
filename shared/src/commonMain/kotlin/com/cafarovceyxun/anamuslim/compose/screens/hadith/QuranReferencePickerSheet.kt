package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.ChapterCard
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.NumeralSystem
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.compose.utils.shapeDigits
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.strActionAddReference
import com.cafarovceyxun.anamuslim.resources.strHintSearchChapter
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.resources.strMsgPickVerseReference
import com.cafarovceyxun.anamuslim.resources.strMsgVerseTextUnavailable
import com.cafarovceyxun.anamuslim.resources.strTitleQuranReference
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import com.cafarovceyxun.anamuslim.viewModels.ChapterNavigatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * One verse pick, ready to be dropped into the editor: the reference goes to the source field, the
 * Arabic and the translation into their own text fields. Both texts are nullable — the editor can
 * switch either of them off before adding, and an undo has to put back exactly what went in.
 */
data class QuranVerseInsertion(
    val reference: String,
    val arabic: String?,
    val translation: String?,
)

/**
 * Pulls verses straight out of the Quran into the hadith editor: `Fatihə 1:7`, `Bəqərə 2:1-5` into
 * the source field, and the verse's Arabic text and translation into the two text fields.
 *
 * Adding closes the sheet — the editor gets its verse and lands straight back on the fields, and a
 * second reference is one tap on the picker button away. Corrections are made in the fields
 * themselves, which is why the sheet keeps no undo affordance of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReferencePickerSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAdd: (QuranVerseInsertion) -> Unit,
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        PickerContent(
            onAdd = { insertion ->
                onAdd(insertion)
                // Slide the sheet out first, then drop it: setting the flag straight away would make
                // it vanish without the exit animation.
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onDismiss()
                }
            },
        )
    }
}

@Composable
private fun PickerContent(
    onAdd: (QuranVerseInsertion) -> Unit,
) {
    val viewModel = viewModel { ChapterNavigatorViewModel() }
    val surahs by viewModel.surahs.collectAsState()
    val translationFactory = QuranTranslationFactory.remember()

    var selectedChapterNo by rememberSaveable { mutableStateOf<Int?>(null) }
    var fromVerse by rememberSaveable { mutableStateOf<Int?>(null) }
    var toVerse by rememberSaveable { mutableStateOf<Int?>(null) }
    var includeArabic by rememberSaveable { mutableStateOf(true) }
    var includeTranslation by rememberSaveable { mutableStateOf(true) }

    var verseTexts by remember { mutableStateOf<VerseTexts?>(null) }
    var isLoadingTexts by remember { mutableStateOf(value = false) }
    // Bumped whenever the selection changes by tapping, which is the only time the range field is
    // allowed to overwrite what the editor is typing into it.
    var selectionRevision by remember { mutableIntStateOf(0) }

    val selectedSurah = surahs.firstOrNull { it.surah.surahNo == selectedChapterNo }
    val reference = selectedSurah?.let { surah ->
        val from = fromVerse ?: return@let null
        val to = toVerse ?: from
        quranReference(surah.getCurrentName(), surah.surah.surahNo, from, to)
    }
    // The chapter list is already loaded with every localization, so the Arabic name of the surah
    // is here without another query.
    val arabicChapterName = selectedSurah?.localizations
        ?.firstOrNull { it.langCode == ARABIC_LANG_CODE && !it.name.isNullOrBlank() }
        ?.name

    // The texts are fetched as soon as a verse is picked, so the editor sees what is about to land
    // in the fields instead of finding out after tapping add.
    LaunchedEffect(selectedChapterNo, fromVerse, toVerse, arabicChapterName) {
        val chapterNo = selectedChapterNo
        val from = fromVerse

        if (chapterNo == null || from == null) {
            verseTexts = null
            isLoadingTexts = false
            return@LaunchedEffect
        }

        isLoadingTexts = true
        verseTexts = loadVerseTexts(
            translationFactory = translationFactory,
            chapterNo = chapterNo,
            fromVerse = from,
            toVerse = toVerse ?: from,
            arabicChapterName = arabicChapterName,
        )
        isLoadingTexts = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
    ) {
        BottomSheetHeader(
            icon = Res.drawable.dr_icon_read_quran,
            title = stringResource(Res.string.strTitleQuranReference),
            hasDragHandle = true,
        )

        if (surahs.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) { Loader(fill = true) }
        } else {
            Row(modifier = Modifier.weight(1f)) {
                ChapterList(
                    repository = viewModel.repository,
                    surahs = surahs,
                    selectedChapterNo = selectedChapterNo,
                    onSelect = { chapterNo ->
                        selectedChapterNo = chapterNo
                        fromVerse = null
                        toVerse = null
                        selectionRevision++
                    },
                )

                VerseRangeList(
                    ayahCount = selectedSurah?.surah?.ayahCount ?: 0,
                    chapterNo = selectedChapterNo,
                    fromVerse = fromVerse,
                    toVerse = toVerse,
                    selectionRevision = selectionRevision,
                    onSelect = { verseNo ->
                        val from = fromVerse
                        val to = toVerse
                        when {
                            // Nothing picked yet, or a finished range — start over from this verse.
                            from == null || to == null || from != to -> {
                                fromVerse = verseNo
                                toVerse = verseNo
                            }
                            // Second tap on the same verse takes the selection back off.
                            verseNo == from -> {
                                fromVerse = null
                                toVerse = null
                            }
                            // Second tap elsewhere closes the range, in either direction.
                            else -> {
                                fromVerse = minOf(from, verseNo)
                                toVerse = maxOf(from, verseNo)
                            }
                        }
                        selectionRevision++
                    },
                    onRangeTyped = { typedFrom, typedTo ->
                        val ayahCount = selectedSurah?.surah?.ayahCount ?: 0
                        val start = typedFrom?.takeIf { ayahCount > 0 }?.coerceIn(1, ayahCount)

                        if (start == null) {
                            fromVerse = null
                            toVerse = null
                        } else {
                            fromVerse = start
                            toVerse = (typedTo?.coerceIn(1, ayahCount) ?: start)
                                .coerceAtLeast(start)
                        }
                    },
                )
            }
        }

        PickerFooter(
            reference = reference,
            verseTexts = verseTexts,
            isLoadingTexts = isLoadingTexts,
            includeArabic = includeArabic,
            includeTranslation = includeTranslation,
            onToggleArabic = { includeArabic = it },
            onToggleTranslation = { includeTranslation = it },
            onAdd = {
                val ref = reference ?: return@PickerFooter
                val texts = verseTexts
                onAdd(
                    QuranVerseInsertion(
                        reference = ref,
                        arabic = texts?.arabic?.takeIf { includeArabic && it.isNotBlank() },
                        translation = texts?.translation?.takeIf {
                            includeTranslation && it.isNotBlank()
                        },
                    )
                )
            },
        )
    }
}

@Composable
private fun RowScope.ChapterList(
    repository: QuranRepository,
    surahs: List<SurahWithLocalizations>,
    selectedChapterNo: Int?,
    onSelect: (Int) -> Unit,
) {
    val gridState = rememberLazyGridState()
    var filteredSurahs by remember { mutableStateOf(surahs) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchQuery, surahs) {
        val query = searchQuery.lowercase().trim()

        if (query.isEmpty()) {
            filteredSurahs = surahs
        } else {
            val surahNos = repository.searchSurahNos(query)
            filteredSurahs = surahs.filter { it.surah.surahNo in surahNos }
            gridState.scrollToItem(0)
        }
    }

    Column(Modifier.weight(1f)) {
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            FilterField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                hint = stringResource(Res.string.strHintSearchChapter),
            )
        }

        BoxWithConstraints(Modifier.verticalFadingEdge(gridState)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (maxWidth < 800.dp) 1 else 2),
                modifier = Modifier.fillMaxWidth(),
                state = gridState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredSurahs, key = { it.surah.surahNo }) { surah ->
                    ChapterCard(
                        surah = surah,
                        isCurrent = selectedChapterNo == surah.surah.surahNo,
                        iconWithPrefix = false,
                        onClick = { onSelect(surah.surah.surahNo) },
                    )
                }
            }
        }
    }
}

/**
 * The verse column. A range is either typed straight into the field on top — `1-5`, the way it ends
 * up in the source — or tapped out on the list: first tap picks a verse, a second one closes the
 * range around it.
 */
@Composable
private fun VerseRangeList(
    ayahCount: Int,
    chapterNo: Int?,
    fromVerse: Int?,
    toVerse: Int?,
    selectionRevision: Int,
    onSelect: (Int) -> Unit,
    onRangeTyped: (from: Int?, to: Int?) -> Unit,
) {
    if (ayahCount <= 0) return

    val state = rememberLazyListState()
    val verses = remember(ayahCount) { (1..ayahCount).toList() }
    var rangeText by rememberSaveable { mutableStateOf("") }

    // Only tapping (and switching surah) writes back into the field; typing must not have its own
    // half-finished input — `1-` on the way to `1-5` — normalized away underneath it.
    LaunchedEffect(selectionRevision, chapterNo) {
        rangeText = when {
            fromVerse == null -> ""
            toVerse == null || toVerse == fromVerse -> "$fromVerse"
            else -> "$fromVerse-$toVerse"
        }
    }

    // A typed verse number is worth nothing if it stays off-screen, so the list follows it — but
    // only when it is not already in view, otherwise every tap would yank the list around.
    LaunchedEffect(fromVerse) {
        val target = fromVerse ?: return@LaunchedEffect
        val index = (target - 1).coerceIn(0, ayahCount - 1)
        if (state.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            state.animateScrollToItem(index)
        }
    }

    Column(modifier = Modifier.width(110.dp)) {
        Box(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
            FilterField(
                value = rangeText,
                onValueChange = { input ->
                    val cleaned = input.filter { it.isDigit() || it == '-' }.take(9)
                    rangeText = cleaned

                    val parts = cleaned.split('-')
                    onRangeTyped(parts.getOrNull(0)?.toIntOrNull(), parts.getOrNull(1)?.toIntOrNull())
                },
                hint = VERSE_RANGE_HINT,
                keyboardType = KeyboardType.Text,
                showLeading = false,
            )
        }

        Box(Modifier.verticalFadingEdge(state)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 32.dp),
                state = state,
            ) {
                items(verses) { verseNo ->
                    val inRange = fromVerse != null && toVerse != null &&
                        verseNo in fromVerse..toVerse
                    val isEdge = verseNo == fromVerse || verseNo == toVerse

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (inRange && !isEdge) colorScheme.primary.alpha(0.10f)
                            else colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(
                            width = if (isEdge) 1.5.dp else 1.dp,
                            color = if (inRange) colorScheme.primary
                            else colorScheme.outlineVariant.alpha(0.5f),
                        ),
                    ) {
                        Text(
                            text = stringResource(Res.string.strLabelVerseNo, verseNo),
                            modifier = Modifier
                                .clickable { onSelect(verseNo) }
                                .padding(10.dp),
                            fontWeight = if (isEdge) FontWeight.Bold else FontWeight.Normal,
                            color = if (inRange) colorScheme.primary else colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerFooter(
    reference: String?,
    verseTexts: VerseTexts?,
    isLoadingTexts: Boolean,
    includeArabic: Boolean,
    includeTranslation: Boolean,
    onToggleArabic: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        color = colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (reference != null) {
                VersePreview(
                    verseTexts = verseTexts,
                    isLoading = isLoadingTexts,
                    includeArabic = includeArabic,
                    includeTranslation = includeTranslation,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        selected = includeArabic,
                        enabled = !verseTexts?.arabic.isNullOrBlank(),
                        label = { Text(stringResource(Res.string.labelArabic)) },
                        onClick = { onToggleArabic(!includeArabic) },
                    )
                    Chip(
                        selected = includeTranslation,
                        enabled = !verseTexts?.translation.isNullOrBlank(),
                        label = { Text(stringResource(Res.string.labelTranslation)) },
                        onClick = { onToggleTranslation(!includeTranslation) },
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reference ?: stringResource(Res.string.strMsgPickVerseReference),
                    modifier = Modifier.weight(1f),
                    style = if (reference != null) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodySmall,
                    color = if (reference != null) colorScheme.onSurface
                    else colorScheme.onSurface.alpha(0.6f),
                )

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = onAdd,
                    enabled = reference != null && !isLoadingTexts,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(Res.string.strActionAddReference))
                }
            }
        }
    }
}

/** Shows what the two text fields are about to receive — greyed out when its chip is off. */
@Composable
private fun VersePreview(
    verseTexts: VerseTexts?,
    isLoading: Boolean,
    includeArabic: Boolean,
    includeTranslation: Boolean,
) {
    val arabicFontFamily = hadithArabicFontFamily(HadithPreferences.observeArabicFont())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp, max = 132.dp)
            .padding(bottom = 8.dp),
    ) {
        when {
            isLoading -> Loader(fill = true)

            verseTexts == null || (verseTexts.arabic.isBlank() && verseTexts.translation.isBlank()) ->
                Text(
                    text = stringResource(Res.string.strMsgVerseTextUnavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.alpha(0.6f),
                )

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (verseTexts.arabic.isNotBlank()) {
                    Text(
                        text = verseTexts.arabic,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Rtl,
                            fontFamily = arabicFontFamily,
                            fontSize = 18.sp,
                        ),
                        color = if (includeArabic) colorScheme.onSurface
                        else colorScheme.onSurface.alpha(0.35f),
                    )
                }

                if (verseTexts.translation.isNotBlank()) {
                    Text(
                        text = verseTexts.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (includeTranslation) colorScheme.onSurface.alpha(0.8f)
                        else colorScheme.onSurface.alpha(0.35f),
                    )
                }
            }
        }
    }
}

/** The Arabic text and the translation of the picked verse (or range), already joined. */
private data class VerseTexts(
    val arabic: String,
    val translation: String,
)

/**
 * Reads the verse text out of the Quran database: the Uthmani script for the Arabic — the same
 * script the verse share sheet copies — and the reader's primary translation for the rest.
 *
 * The Arabic block closes with the reference in Arabic ([arabicChapterName] plus Arabic-Indic
 * numerals), so the Arabic field carries its own citation the way the source field does.
 */
private suspend fun loadVerseTexts(
    translationFactory: QuranTranslationFactory,
    chapterNo: Int,
    fromVerse: Int,
    toVerse: Int,
    arabicChapterName: String?,
): VerseTexts = withContext(Dispatchers.IO) {
    val repository = RepositoryProvider.quranRepository
    val translationSlug = ReaderPreferences.primaryTranslationSlug()

    val arabic = StringBuilder()
    val translation = StringBuilder()

    for (verseNo in fromVerse..toVerse) {
        val words = repository.getWordsForAyah(chapterNo, verseNo, QuranScriptUtils.SCRIPT_UTHMANI)
        if (words.isNotEmpty()) {
            if (arabic.isNotEmpty()) arabic.append(' ')
            arabic.append(words.joinToString(" ") { it.text })
        }

        val verseTranslation = translationFactory
            .getTranslationsSingleSlugVerse(translationSlug, chapterNo, verseNo)
            ?.text
            ?.let { cleanTranslationHtml(it) }
            ?.trim()

        if (!verseTranslation.isNullOrBlank()) {
            if (translation.isNotEmpty()) translation.append(' ')
            translation.append(verseTranslation)
        }
    }

    val arabicText = arabic.toString().trim()
    val arabicWithReference = if (arabicText.isNotEmpty() && !arabicChapterName.isNullOrBlank()) {
        arabicText + "\n" + arabicQuranReference(arabicChapterName, chapterNo, fromVerse, toVerse)
    } else {
        arabicText
    }

    VerseTexts(arabic = arabicWithReference, translation = translation.toString().trim())
}

private val footnoteTagPattern = Regex("""(?s)<fn.*?>(.*?)<.*?fn>""")

private fun cleanTranslationHtml(text: String): String =
    StringUtils.htmlToPlainText(footnoteTagPattern.replace(text, ""))

/** `Fatihə 1:7` for a single verse, `Bəqərə 2:1-5` for a range. */
internal fun quranReference(chapterName: String, chapterNo: Int, from: Int, to: Int): String {
    val verses = if (from == to) "$from" else "$from-$to"
    return "$chapterName $chapterNo:$verses"
}

/** The same reference in Arabic, for the Arabic text field: `البقرة ٢:١-٥`. */
internal fun arabicQuranReference(chapterName: String, chapterNo: Int, from: Int, to: Int): String {
    val verses = if (from == to) "$from" else "$from-$to"
    return "$chapterName ${NumeralSystem.ARAB.shapeDigits("$chapterNo:$verses")}"
}

private const val ARABIC_LANG_CODE = "ar"

/** Placeholder of the verse-range field. Digits carry the meaning, so it stays untranslated. */
private const val VERSE_RANGE_HINT = "1-5"
