package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.dialogs.WaitingDialog
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.BookmarkNoteSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.BookmarkViewerData
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.BookmarkViewerSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReference
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceData
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.SimilarVersesSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.VerseOptionsData
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.VerseOptionsSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.VerseReportSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.VerseShareSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.WbwSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.WbwSheetData
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.repository.BookmarkAddResult
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.msgPreparingPrebuiltAtlas
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkAddFailed
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkAdded
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkAddedAlready
import com.cafarovceyxun.anamuslim.resources.strMsgNoInternetLong
import com.cafarovceyxun.anamuslim.resources.wbwAudioCouldNotPlay
import com.cafarovceyxun.anamuslim.resources.wbwAudioTimingsCouldNotLoad
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioPlayResult
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioProvider
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.reader.VerseActions
import com.cafarovceyxun.anamuslim.utils.reader.atlas.LocalQuranAtlasBundle
import com.cafarovceyxun.anamuslim.utils.reader.atlas.LocalTajweedPalette
import com.cafarovceyxun.anamuslim.utils.reader.atlas.QuranAtlasLoader
import com.cafarovceyxun.anamuslim.utils.reader.atlas.rememberQuranAtlasBundle
import com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed.TajweedColorSource
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import com.cafarovceyxun.anamuslim.viewModels.ReaderProviderViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource


@Composable
fun ReaderProvider(
    viewModel: ReaderProviderViewModel = viewModel { ReaderProviderViewModel() },
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bundle = rememberQuranAtlasBundle(viewModel.externalQuranDb)
    val isAtlasImporting by QuranAtlasLoader.isImporting

    val controller = viewModel.controller
    val recitationState by controller.state.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlayingState.collectAsStateWithLifecycle()

    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var pendingBookmark by remember { mutableStateOf<PendingBookmark?>(null) }
    var verseOptionsData by remember { mutableStateOf<VerseOptionsData?>(null) }
    var shareSheetVerse by remember { mutableStateOf<VerseWithDetails?>(null) }
    var reportSheetVerse by remember { mutableStateOf<VerseWithDetails?>(null) }
    var similarVersesSheetVerse by remember { mutableStateOf<VerseWithDetails?>(null) }
    var quickReferenceData by remember { mutableStateOf<QuickReferenceData?>(null) }
    var wbwSheetData by remember { mutableStateOf<WbwSheetData?>(null) }

    var wbwWordLoadingKey by remember { mutableStateOf<String?>(null) }

    var activeTooltipWord by remember { mutableStateOf<AyahWordEntity?>(null) }

    val wbwId = ReaderPreferences.observeWbwId()
    val isWbwRtl by produceState<Boolean>(false, wbwId) {
        value = StringUtils.isRtlLanguage(WbwAudioProvider.langCode(wbwId))
    }


    fun playWord(word: AyahWordEntity) {
        val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(word.ayahId)

        coroutineScope.launch {
            val key = "$chapterNo:$verseNo:${word.wordIndex}"
            wbwWordLoadingKey = key

            try {
                when (WbwAudioProvider.play(
                    chapterNo,
                    verseNo,
                    word.wordIndex,
                )) {
                    WbwAudioPlayResult.Success -> Unit
                    WbwAudioPlayResult.NoInternet ->
                        PlatformUtils.showLongToast(getString(Res.string.strMsgNoInternetLong))

                    WbwAudioPlayResult.TimingsNotLoaded ->
                        PlatformUtils.showLongToast(getString(Res.string.wbwAudioTimingsCouldNotLoad))

                    WbwAudioPlayResult.InvalidTiming, WbwAudioPlayResult.NoChapterAudio ->
                        PlatformUtils.showLongToast(getString(Res.string.wbwAudioCouldNotPlay))
                }
            } finally {
                if (wbwWordLoadingKey == key) {
                    wbwWordLoadingKey = null
                }
            }
        }
    }

    val tajweedPalette by TajweedColorSource.paletteState

    CompositionLocalProvider(
        LocalReaderViewModel provides viewModel,
        LocalQuranAtlasBundle provides bundle,
        LocalTajweedPalette provides tajweedPalette,
        LocalVerseActions provides remember {
            VerseActions(
                onReferenceClick = { slugs, chapterNo, verses ->
                    quickReferenceData = QuickReferenceData(slugs, chapterNo, verses)
                },
                onVerseOption = { verse -> verseOptionsData = VerseOptionsData(verse) },
                // Saxlanılmış ayəyə basanda mövcud qeyd açılır; yenisində əvvəlcə qeyd formu
                // göstərilir və yazı yalnız təsdiqdən sonra bazaya düşür.
                onBookmarkRequest = { chapterNo, verseRange ->
                    coroutineScope.launch {
                        if (viewModel.userRepository.isBookmarked(chapterNo, verseRange)) {
                            bookmarkViewerData = BookmarkViewerData(
                                chapterNo = chapterNo,
                                fromVerse = verseRange.first,
                                toVerse = verseRange.last,
                                showOpenInReaderButton = false,
                            )
                        } else {
                            val chapterName = viewModel.repository.getChapterName(chapterNo)
                            val verses = if (verseRange.first == verseRange.last) {
                                "${verseRange.first}"
                            } else {
                                "${verseRange.first}-${verseRange.last}"
                            }
                            pendingBookmark = PendingBookmark(
                                chapterNo = chapterNo,
                                verseRange = verseRange,
                                subtitle = "$chapterName $chapterNo:$verses",
                            )
                        }
                    }
                },
                onShareRequest = { verse -> shareSheetVerse = verse },
                onReportRequest = { verse -> reportSheetVerse = verse },
                onSimilarVerses = { verse -> similarVersesSheetVerse = verse },
            )
        },
        LocalRecitation provides LocalRecitationStateData(
            controller = controller,
            isAnyPlaying = isPlaying,
            playingVerse = recitationState.currentVerse,
        ),
        LocalWbwState provides LocalWbwStateData(
            isWbwRtl = isWbwRtl,
            isWbwAudioLoading = { chapterNo, verseNo, wordIndex ->
                wbwWordLoadingKey == "$chapterNo:$verseNo:$wordIndex"
            },
            activeTooltipWord = activeTooltipWord,
            onDismissTooltip = { activeTooltipWord = null },
            onForcePlay = ::playWord,
            onWordClick = { word ->
                coroutineScope.launch {
                    val shouldPlay = ReaderPreferences.getWbwRecitationEnabled()

                    if (shouldPlay) {
                        playWord(word)
                    }

                    val tooltipEnabled =
                        ReaderPreferences.getWbwTooltipShowTranslation() || ReaderPreferences.getWbwTooltipShowTransliteration()

                    activeTooltipWord = if (tooltipEnabled) {
                        word
                    } else {
                        null
                    }
                }
            },
            toggleWbwSheet = { data ->
                wbwSheetData = data
            },
            isWbwSheetOpen = wbwSheetData != null,
        )
    ) {
        content()

        WaitingDialog(
            isOpen = isAtlasImporting,
            text = stringResource(Res.string.msgPreparingPrebuiltAtlas)
        )

        VerseOptionsSheet(data = verseOptionsData) { verseOptionsData = null }

        VerseShareSheet(
            vwd = shareSheetVerse,
            onDismiss = { shareSheetVerse = null },
        )

        VerseReportSheet(
            vwd = reportSheetVerse,
            onDismiss = { reportSheetVerse = null },
        )

        SimilarVersesSheet(
            sourceVerse = similarVersesSheetVerse,
            onDismiss = { similarVersesSheetVerse = null },
        )

        BookmarkViewerSheet(bookmarkViewerData) {
            bookmarkViewerData = null
        }

        BookmarkNoteSheet(
            subtitle = pendingBookmark?.subtitle,
            onDismiss = { pendingBookmark = null },
            onSave = { note ->
                val request = pendingBookmark ?: return@BookmarkNoteSheet
                pendingBookmark = null

                coroutineScope.launch {
                    val result = viewModel.userRepository.addToBookmark(
                        chapterNo = request.chapterNo,
                        verseRange = request.verseRange,
                        note = note,
                    )
                    PlatformUtils.showToast(
                        when (result) {
                            BookmarkAddResult.Added -> getString(Res.string.strMsgBookmarkAdded)
                            BookmarkAddResult.AlreadyBookmarked ->
                                getString(Res.string.strMsgBookmarkAddedAlready)

                            BookmarkAddResult.Failed ->
                                getString(Res.string.strMsgBookmarkAddFailed)
                        }
                    )
                }
            },
        )

        WbwSheet(
            data = wbwSheetData,
            onDismiss = { wbwSheetData = null },
        )
    }

    // Should stay outside the composition provider
    QuickReference(
        data = quickReferenceData,
        onOpenInReader = { chapterNo, range ->
            quickReferenceData = null
            ReaderUiHooks.openVerseRange?.invoke(chapterNo, range.first, range.last)
        },
        onClose = {
            quickReferenceData = null
        },
    )
}

/** Qeyd formu açıq olduğu müddətdə hansı ayənin saxlanılacağını daşıyır. */
private data class PendingBookmark(
    val chapterNo: Int,
    val verseRange: IntRange,
    val subtitle: String,
)
