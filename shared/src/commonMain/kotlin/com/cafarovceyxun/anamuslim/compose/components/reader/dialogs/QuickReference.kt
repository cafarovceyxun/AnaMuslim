package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelOpen
import com.cafarovceyxun.anamuslim.resources.strLabelQuranOnly
import com.cafarovceyxun.anamuslim.resources.strLabelQuranPrefix
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayoutItem
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderPreparedData
import com.cafarovceyxun.anamuslim.compose.components.reader.TextStyleProvider
import com.cafarovceyxun.anamuslim.compose.components.reader.VerseView
import com.cafarovceyxun.anamuslim.compose.extensions.bottomBorder
import com.cafarovceyxun.anamuslim.compose.theme.LegacyColors
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.UserRepository
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderProvider
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.ReaderItemsBuilder
import com.cafarovceyxun.anamuslim.utils.reader.TextBuilderParams
import com.cafarovceyxun.anamuslim.utils.univ.RegexPattern
import com.cafarovceyxun.anamuslim.viewModels.ReaderProviderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

data class QuickReferenceData(
    val slugs: Set<String>,
    val chapterNo: Int,
    val verses: String? = null,
    val parsedVerses: QuickReferenceVerses? = null
)

sealed class QuickReferenceVerses(open val chapterNo: Int) {
    data class Range(override val chapterNo: Int, val range: IntRange) :
        QuickReferenceVerses(chapterNo)

    data class Discrete(override val chapterNo: Int, val verseNos: List<Int>) :
        QuickReferenceVerses(chapterNo)

    data class ChapterOnly(override val chapterNo: Int) : QuickReferenceVerses(chapterNo)
}

fun parseVerses(chapterNo: Int, versesStr: String): QuickReferenceVerses {
    if (versesStr.isBlank()) return QuickReferenceVerses.ChapterOnly(chapterNo)

    val parts = versesStr.split(",")
    if (parts.size > 1) {
        val ints = parts.mapNotNull { it.trim().toIntOrNull() }.sorted()
        return QuickReferenceVerses.Discrete(chapterNo, ints)
    }

    val matcher = RegexPattern.VERSE_RANGE_PATTERN.find(versesStr)
    if (matcher != null && matcher.groupValues.size >= 3) {
        val from = matcher.groupValues[1].toInt()
        val to = matcher.groupValues[2].toInt()
        return QuickReferenceVerses.Range(chapterNo, from..to)
    }

    val single = versesStr.trim().toIntOrNull()

    return if (single != null) QuickReferenceVerses.Range(chapterNo, single..single)
    else QuickReferenceVerses.ChapterOnly(chapterNo)
}

private fun parsedVersesToList(parsed: QuickReferenceVerses): List<Int> = when (parsed) {
    is QuickReferenceVerses.Range -> parsed.range.toList()
    is QuickReferenceVerses.Discrete -> parsed.verseNos
    is QuickReferenceVerses.ChapterOnly -> emptyList()
}

private fun parsedVersesToIntRange(parsed: QuickReferenceVerses): IntRange? = when (parsed) {
    is QuickReferenceVerses.Range -> parsed.range
    is QuickReferenceVerses.Discrete -> {
        if (parsed.verseNos.isNotEmpty()) parsed.verseNos.min()..parsed.verseNos.max()
        else null
    }

    is QuickReferenceVerses.ChapterOnly -> null
}

private fun formatTitle(
    prefix: String,
    quranOnlyLabel: String,
    chapterNo: Int,
    parsed: QuickReferenceVerses,
): String {
    return when (parsed) {
        is QuickReferenceVerses.Range -> {
            if (parsed.range.first == parsed.range.last) "$prefix${parsed.range.first}"
            else "$prefix${parsed.range.first}-${parsed.range.last}"
        }

        is QuickReferenceVerses.Discrete -> {
            prefix + parsed.verseNos.joinToString(", ")
        }

        is QuickReferenceVerses.ChapterOnly -> "$quranOnlyLabel $chapterNo"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReference(
    data: QuickReferenceData?,
    onOpenInReader: (chapterNo: Int, range: IntRange) -> Unit,
    onClose: () -> Unit,
) {
    if (data == null) return

    val parsed = remember(data) {
        if (data.parsedVerses != null) {
            data.parsedVerses
        } else if (data.verses != null) {
            parseVerses(data.chapterNo, data.verses)
        } else {
            null
        }
    }

    if (parsed == null) {
        return
    }

    if (parsed is QuickReferenceVerses.ChapterOnly) {
        ChapterOnlyDialog(
            chapterNo = data.chapterNo,
            slugs = data.slugs,
            onOpen = {
                onOpenInReader(data.chapterNo, 1..Int.MAX_VALUE)
                onClose()
            },
            onClose = onClose,
        )
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ReaderProvider {
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = sheetState,
            scrimColor = colorScheme.scrim.alpha(0.5f),
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface,
            dragHandle = null,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
        ) {
            QuickReferenceContent(
                data = data,
                parsed = parsed,
                onOpenInReader = onOpenInReader,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun QuickReferenceContent(
    data: QuickReferenceData,
    parsed: QuickReferenceVerses,
    onOpenInReader: (Int, IntRange) -> Unit,
    onClose: () -> Unit,
) {
    val viewModel = viewModel { ReaderProviderViewModel() }

    val textMeasurer = rememberTextMeasurer()
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)
    val density = LocalDensity.current
    val isDark = ThemeUtils.observeDarkTheme()

    val verseActions = LocalVerseActions.current

    val verseNos = remember(parsed) { parsedVersesToList(parsed) }
    val verseRange = remember(parsed) { parsedVersesToIntRange(parsed) }
    val quranPrefix = stringResource(Res.string.strLabelQuranPrefix, data.chapterNo.toString())
    val quranOnlyLabel = stringResource(Res.string.strLabelQuranOnly)
    val title = remember(quranPrefix, quranOnlyLabel, data.chapterNo, parsed) {
        formatTitle(quranPrefix, quranOnlyLabel, data.chapterNo, parsed)
    }

    var prepared by remember { mutableStateOf<ReaderPreparedData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val isBookmarked by if (verseRange != null) {
        viewModel.userRepository
            .isBookmarkedFlow(data.chapterNo, verseRange)
            .collectAsStateWithLifecycle(false)
    } else {
        remember { mutableStateOf(false) }
    }

    LaunchedEffect(data, verseActions, colors, type, density, isDark) {
        isLoading = true

        withContext(Dispatchers.IO) {
            val params = TextBuilderParams(
                uiConfig = ComposeUiConfig(
                    colors = colors,
                    type = type,
                    density = density,
                    textMeasurer = textMeasurer,
                    isDark = isDark,
                ),
                fontResolver = viewModel.fontResolver,
                verseActions = verseActions,
                arabicEnabled = ReaderPreferences.getArabicTextEnabled(),
                script = ReaderPreferences.getQuranScript(),
                arabicSizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier(),
                translationSizeMultiplier = ReaderPreferences.getTranslationTextSizeMultiplier(),
                slugs = data.slugs.takeIf { it.isNotEmpty() }
                    ?: ReaderPreferences.getTranslations(),
            )

            prepared = ReaderItemsBuilder.buildQuickReferenceItems(
                params, data.chapterNo, verseNos
            )
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
    ) {
        QuickReferenceHeader(
            title = title,
            isBookmarked = isBookmarked,
            showActions = verseRange != null && !isLoading,
            onBookmark = {
                if (verseRange == null) return@QuickReferenceHeader
                verseActions.onBookmarkRequest?.invoke(data.chapterNo, verseRange)
            },
            onOpen = {
                if (verseRange != null) {
                    onOpenInReader(data.chapterNo, verseRange)
                    onClose()
                }
            },
            onClose = onClose,
        )


        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val verseRows = prepared?.items.orEmpty().filterIsInstance<ReaderLayoutItem.VerseUI>()

            TextStyleProvider(prepared?.textStyles ?: emptyMap()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    itemsIndexed(
                        verseRows,
                        key = { _, item -> item.key }
                    ) { index, verseUi ->
                        VerseViewWrapped(
                            viewModel.userRepository,
                            verseUi = verseUi,
                            showDivier = index < verseRows.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseViewWrapped(
    bookmarksRepo: UserRepository,
    verseUi: ReaderLayoutItem.VerseUI,
    showDivier: Boolean
) {
    val verse = verseUi.verse

    val isBookmarked by bookmarksRepo
        .isBookmarkedFlow(verse.chapterNo, verse.verseNo..verse.verseNo)
        .collectAsStateWithLifecycle(false)

    VerseView(
        verseUi = verseUi,
        isBookmarked = isBookmarked,
        showDivider = showDivier,
    )
}

@Composable
private fun QuickReferenceHeader(
    title: String,
    isBookmarked: Boolean,
    showActions: Boolean,
    onBookmark: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val iconTint = colorScheme.onSurface.alpha(0.7f)
    val bookmarkTint = if (isBookmarked) LegacyColors.brandPrimary
    else iconTint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bottomBorder()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelClose),
                tint = colorScheme.onSurface,
            )
        }

        Text(
            text = title,
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )

        if (showActions) {
            IconButton(onClick = onBookmark) {
                Icon(
                    painter = painterResource(
                        if (isBookmarked) Res.drawable.ic_bookmark_added
                        else Res.drawable.ic_bookmark
                    ),
                    contentDescription = stringResource(Res.string.strLabelBookmark),
                    tint = bookmarkTint,
                )
            }

            IconButton(onClick = onOpen) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_open),
                    contentDescription = stringResource(Res.string.strLabelOpen),
                    tint = iconTint,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ChapterOnlyDialog(
    chapterNo: Int,
    slugs: Set<String>,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val repository = remember { RepositoryProvider.quranRepository }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(chapterNo)
        }
    }

    AlertDialog(
        isOpen = true,
        onClose = onClose,
        title = stringResource(Res.string.strLabelOpen),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = onClose,
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelOpen),
                style = AlertDialogActionStyle.Primary,
                onClick = onOpen,
            ),
        ),
    ) {
        Text(
            text = chapterName.ifEmpty { "Surah $chapterNo" },
            style = typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
