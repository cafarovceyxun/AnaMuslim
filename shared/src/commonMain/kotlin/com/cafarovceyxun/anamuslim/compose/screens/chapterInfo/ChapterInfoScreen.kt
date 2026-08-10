package com.cafarovceyxun.anamuslim.compose.screens.chapterInfo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strMsgInvalidParams
import com.cafarovceyxun.anamuslim.resources.strMsgNoInternet
import com.cafarovceyxun.anamuslim.resources.strMsgNoInternetLong
import com.cafarovceyxun.anamuslim.resources.strTitleAboutSurah
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReference
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceData
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceVerses
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.ChapterVerseNavigator
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.quran.RevelationType
import com.cafarovceyxun.anamuslim.viewModels.ChapterInfoContentState
import com.cafarovceyxun.anamuslim.viewModels.ChapterInfoEvent
import com.cafarovceyxun.anamuslim.viewModels.ChapterInfoUiState
import com.cafarovceyxun.anamuslim.viewModels.ChapterInfoViewModel

data class ChapterInfoContentData(
    val chapterNo: Int,
    val chapterName: String,
    val language: String,
    val verseCount: Int,
    val rukuCount: Int,
    val revelationOrder: Int,
    val revelationType: RevelationType,
    val juzNos: List<Int>,
)


@Composable
fun ChapterInfoScreen(
    initialChapterNo: Int,
    initialLanguage: String?,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel { ChapterInfoViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val onEvent = viewModel::onEvent

    LaunchedEffect(initialChapterNo, initialLanguage) {
        onEvent(ChapterInfoEvent.Init(initialChapterNo, initialLanguage))
    }

    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }
    var showChapterNavigator by rememberSaveable { mutableStateOf(false) }
    val translationSlugs = ReaderPreferences.observeTranslations()

    Scaffold(
        modifier = modifier,
        topBar = {
            ChapterInfoTopBar(
                uiState = uiState,
                onOpenChapterNavigator = { showChapterNavigator = true },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val contentState = uiState.contentState) {
                is ChapterInfoContentState.Loading -> LoadingContent()

                is ChapterInfoContentState.Success -> {
                    ChapterInfoWebViewContent(
                        contentState = contentState,
                        onOpenReference = { chapterNo, fromVerse, toVerse ->
                            // Validation used to live in the Android JS interface; it is portable,
                            // so it moved here and both platforms get it.
                            val verseCount = uiState.swl?.surah?.ayahCount ?: 0
                            if (!QuranMeta.isChapterValid(chapterNo) ||
                                fromVerse < 1 || toVerse < 1 || fromVerse > toVerse ||
                                fromVerse > verseCount || toVerse > verseCount
                            ) return@ChapterInfoWebViewContent

                            quickRefData = QuickReferenceData(
                                slugs = translationSlugs,
                                chapterNo = chapterNo,
                                parsedVerses = QuickReferenceVerses.Range(
                                    chapterNo,
                                    fromVerse..toVerse
                                ),
                            )
                        },
                    )
                }

                is ChapterInfoContentState.Error -> {
                    ErrorContent(
                        message = contentState.message,
                        canRetry = contentState.canRetry,
                        onRetry = { onEvent(ChapterInfoEvent.Retry) },
                    )
                }

                is ChapterInfoContentState.NoInternet -> {
                    NoInternetContent(
                        onRetry = { onEvent(ChapterInfoEvent.Retry) },
                    )
                }

                is ChapterInfoContentState.InvalidParams -> {
                    InvalidParamsContent()
                }
            }
        }
    }

    ChapterVerseNavigator(
        isOpen = showChapterNavigator,
        onDismiss = { showChapterNavigator = false },
        selectedChapterNo = uiState.chapterNo.takeIf { it >= 1 },
        selectedVerseNos = emptySet(),
        onChapterSelected = { chapterNo ->
            onEvent(ChapterInfoEvent.Init(chapterNo, initialLanguage))
        },
    )

    QuickReference(
        data = quickRefData,
        onOpenInReader = { chapterNo, range ->
            quickRefData = null
            ReaderUiHooks.openVerseRange?.invoke(chapterNo, range.first, range.last)
        },
        onClose = { quickRefData = null },
    )
}

@Composable
private fun ChapterInfoTopBar(
    uiState: ChapterInfoUiState,
    onOpenChapterNavigator: () -> Unit,
) {
    val appLocale = LocalAppLocale.current
    val chapterName = uiState.swl?.let { it.getCurrentName() }.orEmpty()
    val chapterNo = uiState.chapterNo

    AppBar(
        title = stringResource(Res.string.strTitleAboutSurah),
        actions = {
            TextButton(onOpenChapterNavigator) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (chapterNo >= 1) {
                            if (chapterName.isNotEmpty()) {
                                "${appLocale.numeralSystem.formatNumber(chapterNo)}. $chapterName"
                            } else {
                                appLocale.numeralSystem.formatNumber(chapterNo)
                            }
                        } else {
                            ""
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun ChapterInfoWebViewContent(
    contentState: ChapterInfoContentState.Success,
    onOpenReference: (Int, Int, Int) -> Unit,
) {
    // The page is fully self-contained (CSS/JS/font/image inlined by the view model), so the
    // platform only has to render an HTML string: WebView on Android, WKWebView on iOS.
    ChapterInfoHtmlView(
        html = contentState.html,
        onOpenReference = onOpenReference,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = colorScheme.primary,
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_info),
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (canRetry) {
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(Res.string.strLabelRetry),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun NoInternetContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_info),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.strMsgNoInternet),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.strMsgNoInternetLong),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(Res.string.strLabelRetry),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun InvalidParamsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_info),
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.strMsgInvalidParams),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
