package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.includeBookmarkNote
import com.cafarovceyxun.anamuslim.resources.openImageEditor
import com.cafarovceyxun.anamuslim.resources.strHintFromVerse
import com.cafarovceyxun.anamuslim.resources.strHintToVerse
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCopy
import com.cafarovceyxun.anamuslim.resources.strLabelCurrentVerse
import com.cafarovceyxun.anamuslim.resources.strLabelIncludeArabic
import com.cafarovceyxun.anamuslim.resources.strLabelSelectTranslations
import com.cafarovceyxun.anamuslim.resources.strLabelSelectVerse
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strLabelVerseRange
import com.cafarovceyxun.anamuslim.resources.strLabelWhatsappStyling
import com.cafarovceyxun.anamuslim.resources.strMsgEnterValidRange
import com.cafarovceyxun.anamuslim.resources.strMsgShareRange
import com.cafarovceyxun.anamuslim.resources.strTitleShareVerse
import org.jetbrains.compose.resources.getString
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.components.quran.subcomponents.Translation
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.coroutines.launch

/** Haşiyə istinadları paylaşılan mətndən tamamilə çıxarılır. */
private val footnoteTagPattern = Regex("""(?s)<fn.*?>(.*?)<.*?fn>""")

private data class VerseShareState(
    val useVerseRange: Boolean = false,
    val fromVerseText: String = "",
    val toVerseText: String = "",
    val whatsappStyling: Boolean = false,
    val includeArabic: Boolean = true,
    val includeBookmarkNote: Boolean = false,
    val selectedSlugs: Set<String> = setOf(),
)

private typealias UpdateVerseShareState = (VerseShareState.() -> VerseShareState) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseShareSheet(
    vwd: VerseWithDetails?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (vwd == null) return

    val coroutineScope = rememberCoroutineScope()
    val translFactory = QuranTranslationFactory.remember()

    val translationBooks = remember(translFactory) {
        translFactory.getAvailableTranslationBooksInfo()
    }

    var state by remember { mutableStateOf(VerseShareState()) }
    var showImageEditor by remember { mutableStateOf(false) }

    var combinedArabic by remember { mutableStateOf("") }
    var combinedTranslation by remember { mutableStateOf("") }

    val updateState: UpdateVerseShareState = { transform ->
        state = state.transform()
    }

    val (fromVerse, toVerse) = resolveVerseRange(state, vwd.verseNo)

    LaunchedEffect(fromVerse, toVerse, state.selectedSlugs, state.includeArabic) {
        val repository = RepositoryProvider.quranRepository
        val arSb = StringBuilder()
        val trSb = StringBuilder()

        if (fromVerse != null && toVerse != null && vwd.chapter.isVerseRangeValid(fromVerse, toVerse)) {
            for (vNo in fromVerse..toVerse) {
                if (state.includeArabic) {
                    val words = repository.getWordsForAyah(vwd.chapterNo, vNo, QuranScriptUtils.SCRIPT_UTHMANI)
                    arSb.append(words.joinToString(" ") { it.text }).append("  ") // 2 spaces
                }

                val transls = translFactory.getTranslationsSingleVerse(state.selectedSlugs, vwd.chapterNo, vNo)
                transls.firstOrNull()?.let {
                    val cleaned = cleanHtml(it.text)
                    trSb.append(cleaned).append(" ") // 1 space between translations
                }
            }

            val rangeStr = if (fromVerse == toVerse) "$fromVerse" else "$fromVerse-$toVerse"
            trSb.append("\n").append(vwd.chapter.getCurrentName()).append(", ").append(rangeStr)

            combinedArabic = arSb.toString().trim()
            combinedTranslation = trSb.toString().trim()
        }
    }

    if (showImageEditor) {
        QuranImageEditorScreen(
            combinedArabic,
            combinedTranslation,
            state.includeArabic,
            state.selectedSlugs.isNotEmpty(),
        ) { showImageEditor = false }
        return
    }

    LaunchedEffect(vwd) {
        updateState { VerseShareState() }
    }

    LaunchedEffect(translationBooks) {
        val first = translationBooks.keys.firstOrNull()

        updateState {
            copy(
                selectedSlugs = if (first != null) setOf(first) else emptySet()
            )
        }
    }

    val clipboardMsg = stringResource(Res.string.copiedToClipboard)
    val shareTitle = stringResource(Res.string.strTitleShareVerse)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            BottomSheetHeader(
                title = stringResource(Res.string.strTitleShareVerse),
                hasDragHandle = true,
            )

            Column(
                modifier = Modifier
                    .weight(1f, false)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AdvancedShareForm(
                    vwd = vwd,
                    translationBooks = translationBooks,
                    state,
                    updateState = updateState
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.strLabelCancel))
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val text = buildShareText(
                                translFactory,
                                state = state,
                                vwd = vwd,
                            )

                            if (text.isNullOrEmpty()) return@launch

                            PlatformUtils.copyToClipboard(text)

                            PlatformUtils.showClipboardMessage(clipboardMsg)
                        }

                        onDismiss()
                    },
                ) {
                    Text(stringResource(Res.string.strLabelCopy))
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val text = buildShareText(
                                translFactory = translFactory,
                                state = state,
                                vwd = vwd,
                            )

                            if (text.isNullOrEmpty()) return@launch

                            PlatformUtils.shareText(text, shareTitle)

                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer,
                    )
                ) {
                    Text(stringResource(Res.string.strLabelShare))
                }
            }

            Button(
                onClick = { showImageEditor = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer
                )
            ) {
                Icon(painterResource(Res.drawable.dr_icon_share), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.openImageEditor))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdvancedShareForm(
    vwd: VerseWithDetails,
    translationBooks: Map<String, TranslationBookInfoModel>,
    state: VerseShareState,
    updateState: UpdateVerseShareState,
) {
    Text(
        text = stringResource(Res.string.strLabelSelectVerse),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        color = colorScheme.primary,
        style = typography.titleSmall,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
    ) {
        Chip(
            selected = !state.useVerseRange,
            label = { Text(stringResource(Res.string.strLabelCurrentVerse)) },
        ) {
            updateState {
                copy(useVerseRange = false)
            }
        }
        Chip(
            selected = state.useVerseRange,
            label = { Text(stringResource(Res.string.strLabelVerseRange)) },
        ) {
            updateState {
                copy(useVerseRange = true)
            }
        }
    }

    if (state.useVerseRange) {
        Text(
            text = stringResource(
                Res.string.strMsgShareRange,
                1,
                vwd.chapter.surah.ayahCount
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            style = typography.bodyMedium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.fromVerseText,
                onValueChange = {
                    updateState {
                        copy(fromVerseText = it)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(Res.string.strHintFromVerse)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = state.toVerseText,
                onValueChange = {
                    updateState {
                        copy(toVerseText = it)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(Res.string.strHintToVerse)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }

    CheckboxRow(
        checked = state.whatsappStyling,
        onCheckedChange = {
            updateState {
                copy(whatsappStyling = it)
            }
        },
        label = stringResource(Res.string.strLabelWhatsappStyling),
    )

    CheckboxRow(
        checked = state.includeArabic,
        onCheckedChange = {
            updateState {
                copy(includeArabic = it)
            }
        },
        label = stringResource(Res.string.strLabelIncludeArabic),
    )

    CheckboxRow(
        checked = state.includeBookmarkNote,
        onCheckedChange = {
            updateState {
                copy(includeBookmarkNote = it)
            }
        },
        label = stringResource(Res.string.includeBookmarkNote),
    )

    Text(
        text = stringResource(Res.string.strLabelSelectTranslations),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        color = colorScheme.primary,
        style = typography.titleSmall,
    )

    translationBooks.forEach { (slug, book) ->
        val checked = slug in state.selectedSlugs


        CheckboxRow(
            checked,
            onCheckedChange = {
                updateState {
                    copy(
                        selectedSlugs = if (it) selectedSlugs + slug else selectedSlugs - slug,
                    )
                }
            },
            label = book.getDisplayName(true),
        )
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(0.dp)
        )

        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun resolveVerseRange(
    state: VerseShareState,
    defaultVerse: Int,
): Pair<Int?, Int?> {
    if (!state.useVerseRange) {
        return defaultVerse to defaultVerse
    }

    val from = state.fromVerseText.trim().toIntOrNull()
    val to = state.toVerseText.trim().toIntOrNull()

    if (from == null || to == null) return null to null

    return from to to
}

private suspend fun buildShareText(
    translFactory: QuranTranslationFactory,
    state: VerseShareState,
    vwd: VerseWithDetails,
): String? {
    val proceed = state.selectedSlugs.isNotEmpty() || state.includeArabic
    if (!proceed) return null

    val (fromVerse, toVerse) = resolveVerseRange(state, vwd.verseNo)
    if (fromVerse == null || toVerse == null || !vwd.chapter.isVerseRangeValid(fromVerse, toVerse)) {
        PlatformUtils.showLongToast(getString(Res.string.strMsgEnterValidRange))
        return null
    }

    val repository = RepositoryProvider.quranRepository
    val sb = StringBuilder()

    // 1. Arabic Block
    if (state.includeArabic) {
        for (vNo in fromVerse..toVerse) {
            val words = repository.getWordsForAyah(vwd.chapterNo, vNo, QuranScriptUtils.SCRIPT_UTHMANI)
            sb.append(words.joinToString(" ") { it.text }).append("  ")
        }
        sb.setLength(sb.length - 2) // trim 2 spaces
        sb.append("\n\n")
    }

    // 2. Translation Block
    for (vNo in fromVerse..toVerse) {
        val transls = translFactory.getTranslationsSingleVerse(state.selectedSlugs, vwd.chapterNo, vNo)
        transls.firstOrNull()?.let {
            val cleaned = cleanHtml(it.text)
            sb.append(cleaned).append(" ") // 1 space
        }
    }
    sb.trimEnd()

    // 3. Final Attribution
    val rangeStr = if (fromVerse == toVerse) "$fromVerse" else "$fromVerse-$toVerse"
    sb.append("\n").append(vwd.chapter.getCurrentName()).append(", ").append(rangeStr)

    return sb.toString()
}



private fun cleanHtml(string: String): String {
    val replaced = footnoteTagPattern.replace(string, "")

    return StringUtils.htmlToPlainText(replaced)
}


