package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.settings.DiffPair
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.appScopedViewModelStoreOwner
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewSaveResult
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationReviewRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCurrentText
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelNext
import com.cafarovceyxun.anamuslim.resources.strLabelPrevious
import com.cafarovceyxun.anamuslim.resources.strLabelReviewReason
import com.cafarovceyxun.anamuslim.resources.strLabelReviewUnapproved
import com.cafarovceyxun.anamuslim.resources.strLabelApply
import com.cafarovceyxun.anamuslim.resources.strLabelSuggestedNote
import com.cafarovceyxun.anamuslim.resources.strLabelSuggestion
import com.cafarovceyxun.anamuslim.resources.strMsgEditQueuedForReview
import com.cafarovceyxun.anamuslim.resources.strMsgReviewEmpty
import com.cafarovceyxun.anamuslim.resources.strMsgReviewFailed
import com.cafarovceyxun.anamuslim.resources.strMsgReviewUpdated
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.strTitleTranslationReview
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import com.cafarovceyxun.anamuslim.viewModels.TranslationReviewViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Vərəqin vəziyyəti app-scoped-dur — üst panel, ayə düyməsi və vərəq eyni instansiyanı görür. */
@Composable
fun rememberTranslationReviewViewModel(): TranslationReviewViewModel =
    viewModel(appScopedViewModelStoreOwner()) { TranslationReviewViewModel() }

/**
 * Tərcümə yoxlanışının təklifləri arasında gəzmək üçün **yarımçıq açılan** vərəq.
 *
 * Vərəq ekranın yarısında açılır, yuxarıda isə oxucu həmin ayəyə sürüşür — ayə əsas tərcümə və
 * ikinci tərcümə (yoxlanış kitabı endirilibsə) ilə birlikdə görünür. ‹ › hər addımda oxucunu da
 * aparır ([ReaderViewModel.jumpToVerse]). Redaktə təklifin özünü dəyişir (admin) və ya redaktorun öz
 * düzəlişi kimi növbəyə düşür — nəticə vərəqin içində yazılır: iOS-da modal vərəqin üstündə
 * göstərilən toast vərəqlə birlikdə sökülə bilər (CLAUDE.md, «Dialoq bağlanarkən göstərilən toast»).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationReviewSheetHost(readerVm: ReaderViewModel) {
    val vm = rememberTranslationReviewViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.isOpen) return

    val current = state.current
    LaunchedEffect(current?.chapterNo, current?.verseNo) {
        if (current != null) readerVm.jumpToVerse(current.chapterNo, current.verseNo)
    }

    val chapterNames by produceState(emptyMap<Int, String>()) {
        value = RepositoryProvider.quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }

    ModalBottomSheet(
        onDismissRequest = vm::close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        // Yüngül pərdə: vərəqin üstündəki ayə oxunaqlı qalmalıdır — bütün məqsəd ikisini yan-yana görməkdir.
        scrimColor = colorScheme.scrim.alpha(0.12f),
        containerColor = colorScheme.background,
        contentColor = colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ReviewHeader(
                title = current?.let { c ->
                    val name = chapterNames[c.chapterNo]?.takeIf { it.isNotBlank() }
                    if (name != null) "$name ${c.chapterNo}:${c.verseNo}" else "${c.chapterNo}:${c.verseNo}"
                } ?: stringResource(Res.string.strTitleTranslationReview),
                counter = if (state.items.isEmpty()) "" else "${state.index + 1} / ${state.items.size}",
                canPrev = state.index > 0,
                canNext = state.index < state.items.size - 1,
                onPrev = { vm.step(-1) },
                onNext = { vm.step(1) },
            )

            when {
                state.isLoading && state.items.isEmpty() -> Row(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(Modifier.size(28.dp)) }

                current == null -> Text(
                    text = state.error ?: stringResource(Res.string.strMsgReviewEmpty),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )

                else -> ReviewBody(
                    // Açar: başqa ayəyə keçəndə redaktə sahəsi təzədən qurulsun.
                    key = current.edit.id ?: 0L,
                    current = current,
                    isSaving = state.isSaving,
                    lastSave = state.lastSave,
                    onSave = { text ->
                        vm.save(text) { item, applied ->
                            readerVm.applyLocalTranslation(
                                TranslationReviewRepository.BOOK_SLUG,
                                item.chapterNo,
                                item.verseNo,
                                applied,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReviewHeader(
    title: String,
    counter: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPrev, enabled = canPrev) {
            Icon(
                painterResource(Res.drawable.dr_icon_chevron_left),
                contentDescription = stringResource(Res.string.strLabelPrevious),
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (counter.isNotEmpty()) {
                Text(counter, style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(
                painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = stringResource(Res.string.strLabelNext),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ReviewBody(
    key: Long,
    current: com.cafarovceyxun.anamuslim.repository.supabase.ReviewSuggestion,
    isSaving: Boolean,
    lastSave: ReviewSaveResult?,
    onSave: (String) -> Unit,
) {
    var editing by rememberSaveable(key) { mutableStateOf(false) }
    var draft by rememberSaveable(key) { mutableStateOf(current.edit.new_text) }
    // Uğurlu yazmadan sonra redaktə rejimindən çıx.
    LaunchedEffect(lastSave) { if (lastSave != null && lastSave != ReviewSaveResult.Failed) editing = false }

    // Mətnlər həmişə azərbaycancadır — ərəbcə interfeysdə RTL-ə çevrilməsin (HadithDisplayNames.kt).
    val azStyle = typography.bodySmall.withScriptDirection(arabic = false)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.strLabelReviewUnapproved),
            style = typography.labelSmall,
            color = colorScheme.error,
        )

        current.edit.reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Column {
                Text(
                    stringResource(Res.string.strLabelReviewReason),
                    style = typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant,
                )
                Text(reason, style = azStyle, color = colorScheme.onSurfaceVariant)
            }
        }

        if (editing) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.strLabelSuggestion)) },
                textStyle = typography.bodyMedium.withScriptDirection(arabic = false)
                    .copy(color = colorScheme.onBackground),
                enabled = !isSaving,
            )
        } else {
            DiffPair(
                old = current.currentText,
                new = current.edit.new_text,
                oldLabel = stringResource(Res.string.strLabelCurrentText),
                newLabel = stringResource(Res.string.strLabelSuggestion),
            )
            current.edit.note?.let { note ->
                DiffPair(
                    old = current.currentNote.orEmpty(),
                    new = note,
                    oldLabel = stringResource(Res.string.strTitleNote),
                    newLabel = stringResource(Res.string.strLabelSuggestedNote),
                )
            }
        }

        lastSave?.let { result ->
            Text(
                text = stringResource(
                    when (result) {
                        ReviewSaveResult.SuggestionUpdated -> Res.string.strMsgReviewUpdated
                        ReviewSaveResult.QueuedAsOwnEdit -> Res.string.strMsgEditQueuedForReview
                        ReviewSaveResult.Failed -> Res.string.strMsgReviewFailed
                    }
                ),
                style = typography.bodySmall,
                color = if (result == ReviewSaveResult.Failed) colorScheme.error else colorScheme.primary,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.CenterVertically))
                Spacer(Modifier.width(8.dp))
            }
            if (editing) {
                TextButton(onClick = {
                    editing = false
                    draft = current.edit.new_text
                }, enabled = !isSaving) { Text(stringResource(Res.string.strLabelCancel)) }
                TextButton(
                    onClick = { onSave(draft) },
                    enabled = !isSaving && draft.isNotBlank() && draft != current.edit.new_text,
                ) { Text(stringResource(Res.string.strLabelApply)) }
            } else {
                TextButton(onClick = {
                    draft = current.edit.new_text
                    editing = true
                }) { Text(stringResource(Res.string.strLabelEdit)) }
            }
        }
    }
}
