package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.settings.DiffPair
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.appScopedViewModelStoreOwner
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewCategory
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewSaveResult
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewSuggestion
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationReviewRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.save
import com.cafarovceyxun.anamuslim.resources.strActionUndo
import com.cafarovceyxun.anamuslim.resources.strLabelAll
import com.cafarovceyxun.anamuslim.resources.strLabelApprove
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelCurrentText
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelReject
import com.cafarovceyxun.anamuslim.resources.strLabelReviewApprovedCount
import com.cafarovceyxun.anamuslim.resources.strLabelReviewEditedCount
import com.cafarovceyxun.anamuslim.resources.strLabelReviewReason
import com.cafarovceyxun.anamuslim.resources.strLabelReviewRejectedCount
import com.cafarovceyxun.anamuslim.resources.strLabelReviewSerious
import com.cafarovceyxun.anamuslim.resources.strLabelReviewSession
import com.cafarovceyxun.anamuslim.resources.strLabelSaveAndApprove
import com.cafarovceyxun.anamuslim.resources.strLabelSkip
import com.cafarovceyxun.anamuslim.resources.strLabelSuggestedNote
import com.cafarovceyxun.anamuslim.resources.strLabelSuggestion
import com.cafarovceyxun.anamuslim.resources.strMsgEditQueuedForReview
import com.cafarovceyxun.anamuslim.resources.strMsgReviewApproved
import com.cafarovceyxun.anamuslim.resources.strMsgReviewDecisionFailed
import com.cafarovceyxun.anamuslim.resources.strMsgReviewEmpty
import com.cafarovceyxun.anamuslim.resources.strMsgReviewFailed
import com.cafarovceyxun.anamuslim.resources.strMsgReviewNotAllowed
import com.cafarovceyxun.anamuslim.resources.strMsgReviewRejected
import com.cafarovceyxun.anamuslim.resources.strMsgReviewSavingIn
import com.cafarovceyxun.anamuslim.resources.strMsgReviewUpdated
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.strTitleTranslationReview
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import com.cafarovceyxun.anamuslim.viewModels.ReviewDecision
import com.cafarovceyxun.anamuslim.viewModels.ReviewNoticeKind
import com.cafarovceyxun.anamuslim.viewModels.TranslationReviewState
import com.cafarovceyxun.anamuslim.viewModels.TranslationReviewViewModel
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.ReaderItemsBuilder
import com.cafarovceyxun.anamuslim.utils.reader.TextBuilderParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Növbənin vəziyyəti app-scoped-dur — üst panel, ayə düyməsi və növbə eyni instansiyanı görür. */
@Composable
fun rememberTranslationReviewViewModel(): TranslationReviewViewModel =
    viewModel(appScopedViewModelStoreOwner()) { TranslationReviewViewModel() }

/**
 * Tərcümə yoxlanışının təkliflərini bir-bir oxuyub qərar vermək üçün **tam ekran növbə**: ərəb mətni,
 * hazırkı tərcümə ilə təklifin fərqi, səbəb və dörd düymə — rədd et / keç / düzəlt / təsdiq et.
 *
 * `Dialog`-dur, inline yox: host [ReaderProvider]-in içindədir (CLAUDE.md, «Tam ekran səth `Dialog`
 * olmalıdır»). Bütün nəticə mesajları dialoqun öz zolağında göstərilir — iOS-da dialoqun üstündəki
 * toast onunla birlikdə sökülə bilər. Qərar gecikmə ilə yazılır, «Geri al» zolaqdadır
 * ([TranslationReviewViewModel.UNDO_WINDOW_MS]). Növbə bağlananda oxucu son baxılan ayəyə keçir.
 */
@Composable
fun TranslationReviewQueueHost(readerVm: ReaderViewModel) {
    val vm = rememberTranslationReviewViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    // Serverə yazılmış qərar cihazdakı nüsxələrə də düşsün: təsdiqdə əsas tərcümə təklifin mətnini
    // alır, yoxlanış kitabı isə onunla eyniləşir; rədddə kitab hazırkı mətnə qayıdır.
    LaunchedEffect(vm) {
        vm.committed.collect { c ->
            val item = c.item
            when (c.decision) {
                ReviewDecision.Approve -> {
                    readerVm.applyLocalTranslation(TranslUtils.TRANSL_SLUG_DEFAULT, item.chapterNo, item.verseNo, item.edit.new_text)
                    readerVm.applyLocalTranslation(TranslationReviewRepository.BOOK_SLUG, item.chapterNo, item.verseNo, item.edit.new_text)
                }

                ReviewDecision.Reject -> item.currentText?.let {
                    readerVm.applyLocalTranslation(TranslationReviewRepository.BOOK_SLUG, item.chapterNo, item.verseNo, it)
                }
            }
        }
    }

    if (!state.isOpen) return

    // Nəticə mesajı kartın üstünü örtür — bir müddətdən sonra özü getsin.
    LaunchedEffect(state.lastSave, state.notice) {
        if (state.lastSave == null && state.notice == null) return@LaunchedEffect
        delay(4_000)
        vm.clearMessages()
    }

    val chapterNames by produceState(emptyMap<Int, String>()) {
        value = RepositoryProvider.quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }

    val close = {
        state.current?.let { readerVm.jumpToVerse(it.chapterNo, it.verseNo) }
        vm.close()
    }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Column(Modifier.fillMaxSize()) {
                    QueueHeader(state, onClose = close, onFilter = vm::setFilter)

                    val current = state.current
                    when {
                        state.isLoading && state.items.isEmpty() -> Box(
                            Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(Modifier.size(28.dp)) }

                        current == null -> Box(
                            Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = state.error ?: stringResource(Res.string.strMsgReviewEmpty),
                                style = typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                        else -> QueueItem(
                            // Açar: başqa ayəyə keçəndə redaktə vəziyyəti təzədən qurulsun.
                            key = current.edit.id ?: 0L,
                            item = current,
                            chapterName = chapterNames[current.chapterNo],
                            state = state,
                            vm = vm,
                            readerVm = readerVm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                QueueMessageBar(state, vm, Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp))
            }
        }
    }
}

@Composable
private fun QueueHeader(
    state: TranslationReviewState,
    onClose: () -> Unit,
    onFilter: (String?) -> Unit,
) {
    val total = state.visible.size
    Surface(color = colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelClose),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    stringResource(Res.string.strTitleTranslationReview),
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (total > 0) {
                    Text(
                        "${state.index + 1} / $total",
                        style = typography.labelLarge,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { if (total == 0) 0f else (state.index + 1f) / total },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(4.dp),
                trackColor = colorScheme.outlineVariant.alpha(0.5f),
                drawStopIndicator = {},
            )

            val tally = state.tally
            if (tally.approved + tally.rejected > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                ) {
                    val small = typography.labelMedium
                    Text(stringResource(Res.string.strLabelReviewSession), style = small, color = colorScheme.onSurfaceVariant)
                    Text(stringResource(Res.string.strLabelReviewApprovedCount, tally.approved), style = small, color = colorScheme.primary)
                    if (tally.edited > 0) {
                        Text(stringResource(Res.string.strLabelReviewEditedCount, tally.edited), style = small, color = colorScheme.primary)
                    }
                    if (tally.rejected > 0) {
                        Text(stringResource(Res.string.strLabelReviewRejectedCount, tally.rejected), style = small, color = colorScheme.error)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp),
            ) {
                FilterChip(
                    selected = state.filter == null,
                    onClick = { onFilter(null) },
                    label = { Text("${stringResource(Res.string.strLabelAll)} · ${state.items.size}") },
                )
                state.categories.forEach { (category, count) ->
                    FilterChip(
                        selected = state.filter == category,
                        onClick = { onFilter(category) },
                        label = { Text("${categoryLabel(category)} · $count") },
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryLabel(category: String): String =
    if (category == ReviewCategory.SERIOUS) stringResource(Res.string.strLabelReviewSerious)
    else ReviewCategory.display(category)

@Composable
private fun QueueItem(
    key: Long,
    item: ReviewSuggestion,
    chapterName: String?,
    state: TranslationReviewState,
    vm: TranslationReviewViewModel,
    readerVm: ReaderViewModel,
    modifier: Modifier,
) {
    var editing by rememberSaveable(key) { mutableStateOf(false) }
    var draft by rememberSaveable(key) { mutableStateOf(item.edit.new_text) }
    // Uğurlu yazmadan sonra redaktə rejimindən çıx.
    LaunchedEffect(state.lastSave) {
        if (state.lastSave != null && state.lastSave != ReviewSaveResult.Failed) editing = false
    }

    val arabic by produceState<String?>(null, item.chapterNo, item.verseNo) {
        value = try {
            val id = QuranMeta.getAyahId(item.chapterNo, item.verseNo)
            RepositoryProvider.quranRepository.getVerseTextsForAyahs(listOf(id))[id]
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    val onApplied: (ReviewSuggestion, String) -> Unit = { s, applied ->
        readerVm.applyLocalTranslation(TranslationReviewRepository.BOOK_SLUG, s.chapterNo, s.verseNo, applied)
    }

    // Mətnlər həmişə azərbaycancadır — ərəbcə interfeysdə RTL-ə çevrilməsin (HadithDisplayNames.kt).
    val azStyle = typography.bodyLarge.withScriptDirection(arabic = false)

    Column(modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surfaceContainerLow,
            shadowElevation = 2.dp,
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ref = "${item.chapterNo}:${item.verseNo}"
                    Text(
                        if (chapterName.isNullOrBlank()) ref else "$chapterName $ref",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ReviewCategory.of(item.edit.reason)?.let { CategoryBadge(categoryLabel(it), serious = it == ReviewCategory.SERIOUS) }
                }

                QueueArabic(item, plainText = arabic)

                if (editing) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.strLabelSuggestion)) },
                        textStyle = azStyle.copy(color = colorScheme.onSurface),
                        enabled = !state.isSaving,
                    )
                } else {
                    DiffPair(
                        old = item.currentText,
                        new = item.edit.new_text,
                        oldLabel = stringResource(Res.string.strLabelCurrentText),
                        newLabel = stringResource(Res.string.strLabelSuggestion),
                    )
                    item.edit.note?.let { note ->
                        DiffPair(
                            old = item.currentNote.orEmpty(),
                            new = note,
                            oldLabel = stringResource(Res.string.strTitleNote),
                            newLabel = stringResource(Res.string.strLabelSuggestedNote),
                        )
                    }
                }

                item.edit.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(Res.string.strLabelReviewReason),
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                        )
                        Text(
                            reason,
                            style = typography.bodySmall.withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (editing) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 20.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { editing = false; draft = item.edit.new_text },
                    enabled = !state.isSaving,
                ) { Text(stringResource(Res.string.strLabelCancel)) }
                Spacer(Modifier.weight(1f))
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp))
                val changed = draft.isNotBlank() && draft != item.edit.new_text
                OutlinedButton(
                    onClick = { vm.save(draft, onApplied = onApplied) },
                    enabled = !state.isSaving && changed,
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(Res.string.save)) }
                Button(
                    onClick = { vm.save(draft, approveAfter = true, onApplied = onApplied) },
                    enabled = !state.isSaving && changed,
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(Res.string.strLabelSaveAndApprove), maxLines = 1) }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 20.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                QueueAction(
                    painter = painterResource(Res.drawable.dr_icon_close),
                    label = stringResource(Res.string.strLabelReject),
                    content = colorScheme.error,
                    container = colorScheme.surfaceContainerLow,
                    border = colorScheme.error.alpha(0.4f),
                    onClick = vm::reject,
                )
                QueueAction(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    label = stringResource(Res.string.strLabelSkip),
                    content = colorScheme.onSurfaceVariant,
                    container = colorScheme.surfaceContainerLow,
                    border = colorScheme.outlineVariant,
                    enabled = state.index < state.visible.size - 1,
                    onClick = { vm.step(1) },
                )
                QueueAction(
                    painter = painterResource(Res.drawable.dr_icon_edit),
                    label = stringResource(Res.string.strLabelEdit),
                    content = colorScheme.primary,
                    container = colorScheme.primary.alpha(0.12f),
                    onClick = { draft = item.edit.new_text; editing = true },
                )
                QueueAction(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    label = stringResource(Res.string.strLabelApprove),
                    content = colorScheme.onPrimary,
                    container = colorScheme.primary,
                    size = 68.dp,
                    onClick = vm::approve,
                )
            }
        }
    }
}

/**
 * Ayənin ərəb mətni oxucudakı kimi — seçilmiş yazı, təcvid və **sözbəsöz** (oxucunun sözbəsöz
 * ayarlarına görə). Hazırlıq sözbəsöz vərəqi ilə eyni yoldandır ([ReaderItemsBuilder.buildQuickReferenceItems]).
 * Hazırlanana qədər (və alınmasa) hərəkəli düz mətn göstərilir.
 *
 * Sözə toxunuş heç nə açmır: oxucunun söz izahı vərəqi bu dialoqun **altında** açılardı və görünməzdi.
 */
@Composable
private fun QueueArabic(item: ReviewSuggestion, plainText: String?) {
    val textMeasurer = rememberTextMeasurer()
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)
    val density = LocalDensity.current
    val isDark = ThemeUtils.observeDarkTheme()
    val vm = LocalReaderViewModel.current
    val verseActions = LocalVerseActions.current

    val prepared by produceState<ReaderPreparedData?>(null, item.chapterNo, item.verseNo, colors, isDark) {
        value = try {
            withContext(Dispatchers.IO) {
                ReaderItemsBuilder.buildQuickReferenceItems(
                    params = TextBuilderParams(
                        uiConfig = ComposeUiConfig(
                            colors = colors,
                            type = type,
                            textMeasurer = textMeasurer,
                            density = density,
                            isDark = isDark,
                        ),
                        fontResolver = vm.fontResolver,
                        verseActions = verseActions,
                        arabicEnabled = true,
                        script = ReaderPreferences.getQuranScript(),
                        arabicSizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier(),
                        translationSizeMultiplier = ReaderPreferences.getTranslationTextSizeMultiplier(),
                        slugs = emptySet(),
                    ),
                    chapterNo = item.chapterNo,
                    verseNos = listOf(item.verseNo),
                    arabicHighlight = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.d("TranslReview", "Arabic prep failed: ${e.message}")
            null
        }
    }

    val data = prepared
    val verseUi = data?.items?.filterIsInstance<ReaderLayoutItem.VerseUI>()?.firstOrNull {
        it.verse.chapterNo == item.chapterNo && it.verse.verseNo == item.verseNo
    }

    if (data != null && verseUi != null && verseUi.verse.words.isNotEmpty()) {
        TextStyleProvider(data.textStyles) {
            QuranTextWbw(
                verseUi = verseUi,
                horizontalArrangement = Arrangement.Center,
                onWordClick = {},
            )
        }
    } else if (!plainText.isNullOrBlank()) {
        Text(
            plainText,
            style = typography.headlineSmall.withScriptDirection(arabic = true)
                .copy(fontFamily = arabicFontFamily(), fontSize = 26.sp, lineHeight = 46.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryBadge(label: String, serious: Boolean) {
    Text(
        label,
        style = typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (serious) colorScheme.error else colorScheme.primary,
        modifier = Modifier
            .background(
                (if (serious) colorScheme.error else colorScheme.primary).alpha(0.12f),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun QueueAction(
    painter: Painter,
    label: String,
    content: Color,
    container: Color,
    border: Color? = null,
    size: Dp = 56.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tint = if (enabled) content else content.alpha(0.35f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = container,
            border = border?.let { BorderStroke(1.dp, if (enabled) it else it.alpha(0.35f)) },
            modifier = Modifier.size(size),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painter, contentDescription = null, tint = tint, modifier = Modifier.size(if (size > 56.dp) 28.dp else 22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = typography.labelMedium, fontWeight = FontWeight.SemiBold, color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.alpha(0.35f), maxLines = 1)
    }
}

/** Dialoqun öz bildiriş zolağı: geri al > yazma xətası > redaktə nəticəsi. */
@Composable
private fun QueueMessageBar(state: TranslationReviewState, vm: TranslationReviewViewModel, modifier: Modifier) {
    val undo = state.undo
    val notice = state.notice
    val lastSave = state.lastSave

    val (title, subtitle) = when {
        undo != null -> {
            val ref = "${undo.item.chapterNo}:${undo.item.verseNo}"
            var remaining by rememberSaveable(undo.token) {
                mutableIntStateOf((TranslationReviewViewModel.UNDO_WINDOW_MS / 1000).toInt())
            }
            LaunchedEffect(undo.token) {
                while (remaining > 0) {
                    delay(1000)
                    remaining--
                }
            }
            stringResource(
                if (undo.decision == ReviewDecision.Approve) Res.string.strMsgReviewApproved else Res.string.strMsgReviewRejected,
                ref,
            ) to stringResource(Res.string.strMsgReviewSavingIn, remaining.coerceAtLeast(1))
        }

        notice != null -> stringResource(
            if (notice.kind == ReviewNoticeKind.NotAllowed) Res.string.strMsgReviewNotAllowed else Res.string.strMsgReviewDecisionFailed,
            "${notice.chapterNo}:${notice.verseNo}",
        ) to null

        lastSave != null -> stringResource(
            when (lastSave) {
                ReviewSaveResult.SuggestionUpdated -> Res.string.strMsgReviewUpdated
                ReviewSaveResult.QueuedAsOwnEdit -> Res.string.strMsgEditQueuedForReview
                ReviewSaveResult.Failed -> Res.string.strMsgReviewFailed
            }
        ) to null

        else -> return
    }

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.inverseSurface,
        contentColor = colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(title, style = typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                subtitle?.let {
                    Text(it, style = typography.bodySmall, color = colorScheme.inverseOnSurface.alpha(0.7f))
                }
            }
            if (undo != null) {
                TextButton(onClick = vm::undo) {
                    Text(stringResource(Res.string.strActionUndo), color = colorScheme.inversePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
