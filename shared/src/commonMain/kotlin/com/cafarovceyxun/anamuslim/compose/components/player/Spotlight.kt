package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.ic_forward_5
import com.cafarovceyxun.anamuslim.resources.ic_mode_mushaf
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.ic_replay_5
import com.cafarovceyxun.anamuslim.resources.ic_skip_back
import com.cafarovceyxun.anamuslim.resources.ic_skip_forward
import com.cafarovceyxun.anamuslim.resources.strLabelNextVerse
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.resources.strLabelPreviousVerse
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalReaderViewModel
import com.cafarovceyxun.anamuslim.compose.components.reader.QuranWordText
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderProvider
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.utils.reader.FontResolver
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.QuranTextStyleParams
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.reader.TranslationTextStyleParams
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasGlyphPlacement
import com.cafarovceyxun.anamuslim.utils.reader.atlas.QuranAtlasLoader
import com.cafarovceyxun.anamuslim.utils.reader.atlas.getForWord
import com.cafarovceyxun.anamuslim.utils.reader.buildTranslationAnnotatedString
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.reader.getQuranTextStyle
import com.cafarovceyxun.anamuslim.utils.reader.getTranslationTextStyle
import com.cafarovceyxun.anamuslim.utils.reader.isQuranAtlasScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge


@Composable
fun ExpandedPlayerSpotlightSection(
    modifier: Modifier = Modifier,
    verse: ChapterVersePair,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationPlayer,
    chromeVisible: Boolean,
    onChromeVisibilityChanged: (Boolean) -> Unit,
) {
    // The controls used to disappear on a 3s timer, which left the mode looking like a blank screen.
    // They now stay put; tapping the verse still hides them for a distraction-free read.
    Column(
        modifier = modifier
    ) {
        SpotlightVersePanel(
            modifier = Modifier
                .weight(1f)
                .pointerInput(chromeVisible) {
                    detectTapGestures {
                        onChromeVisibilityChanged(!chromeVisible)
                    }
                },
            versePair = verse,
        )

        // The bar's row is inside the AnimatedVisibility, not around it: while it wrapped the
        // animation, hiding the chrome left an 88dp band of dead space the verse could not grow
        // into.
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(tween(220)) + expandVertically(
                animationSpec = tween(280),
                expandFrom = Alignment.Top,
            ),
            exit = fadeOut(tween(180)) + shrinkVertically(
                animationSpec = tween(220),
                shrinkTowards = Alignment.Top,
            ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                SpotlightPlaybackBar(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    controller = controller,
                )
            }
        }
    }
}

@Composable
private fun SpotlightVersePanel(
    versePair: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val prefs by DataStoreManager.flowMultiple(
        ReaderPreferences.KEY_TRANSLATIONS,
        ReaderPreferences.KEY_SCRIPT,
        ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL,
    ).collectAsStateWithLifecycle(null)

    if (prefs == null) {
        return
    }

    val preferences = prefs!!
    val slugs = preferences.get(ReaderPreferences.KEY_TRANSLATIONS)

    val scriptCode = QuranScriptUtils.validatePreferredScript(
        preferences.get(ReaderPreferences.KEY_SCRIPT)
    )

    val arabicEnabled = preferences.get(ReaderPreferences.KEY_ARABIC_TEXT_ENABLED)
    val translationEnabled = slugs.isNotEmpty()
    val arabicMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC)
    val translationMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL)

    ReaderProvider {
        val vm = LocalReaderViewModel.current
        val bgColor = playerBgColor()

        Column(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpotlightVerseHeader(versePair = versePair)

            AnimatedContent<ChapterVersePair>(
                modifier = Modifier.fillMaxSize(),
                targetState = versePair,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 1000,
                            delayMillis = 500,
                            easing = FastOutSlowInEasing,
                        ),
                    ) togetherWith fadeOut(
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    )
                },
                contentAlignment = Alignment.Center,
                label = "spotlightVerse",
            ) { pair ->
                if (!pair.isValid) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    val chapterNo = pair.chapterNo
                    val verseNo = pair.verseNo

                    val verse by produceState<VerseWithDetails?>(
                        initialValue = null,
                        chapterNo,
                        verseNo,
                        scriptCode,
                        slugs,
                        arabicEnabled, // Added to force update when arabic is toggled
                    ) {
                        value = withContext(Dispatchers.IO) {
                            val vwd = vm.repository.getVerseWithDetails(
                                chapterNo,
                                verseNo,
                                scriptCode,
                                arabicEnabled
                            )
                                ?: return@withContext null
                            val aSlug = slugs.firstOrNull() ?: TranslUtils.TRANSL_SLUG_DEFAULT

                            vwd.apply {
                                translations = if (translationEnabled) {
                                    QuranTranslationFactory().use {
                                        it.getTranslationsSingleVerse(
                                            setOf(aSlug),
                                            chapterNo,
                                            verseNo,
                                        )
                                    }
                                } else emptyList()
                            }
                        }
                    }

                    val scrollState = rememberScrollState()

                    LaunchedEffect(pair) {
                        scrollState.scrollTo(0)
                    }

                    when (val v = verse) {
                        null -> Loader(true)
                        else -> {
                            val contentColor = playerContentColor()
                            val cardShape = RoundedCornerShape(28.dp)

                            Box(
                                Modifier
                                    .verticalFadingEdge(
                                        scrollState,
                                        color = bgColor,
                                        length = 48.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    // The verse sits on its own card so it reads as a plate on the
                                    // stage instead of text floating in an empty screen.
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .clip(cardShape)
                                            .background(contentColor.alpha(0.05f))
                                            .border(1.dp, contentColor.alpha(0.08f), cardShape)
                                            .padding(vertical = 20.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        SpotlightQuranText(
                                            vwd = v,
                                            arabicEnabled = arabicEnabled,
                                            fontResolver = vm.fontResolver,
                                            scriptCode = scriptCode,
                                            arabicMultiplier = arabicMultiplier,
                                            externalQuranDb = vm.externalQuranDb
                                        )

                                        if (arabicEnabled && translationEnabled) {
                                            HorizontalDivider(
                                                modifier = Modifier
                                                    .padding(vertical = 14.dp)
                                                    .fillMaxWidth(0.35f),
                                                color = contentColor.alpha(0.15f),
                                            )
                                        }

                                        SpotlightTranslationText(
                                            vwd = v,
                                            translationMultiplier = translationMultiplier,
                                            translationEnabled = translationEnabled,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Surah name + verse number, so the spotlight says *what* is on screen without leaving the mode. */
@Composable
private fun SpotlightVerseHeader(versePair: ChapterVersePair) {
    val vm = LocalReaderViewModel.current
    val contentColor = playerContentColor()

    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(versePair.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            vm.repository.getChapterName(versePair.chapterNo)
        }
    }

    if (chapterName.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.strLabelSurah, chapterName),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = stringResource(Res.string.strLabelVerseNo, versePair.verseNo),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.alpha(0.6f),
        )
    }
}

@Composable
private fun SpotlightQuranText(
    vwd: VerseWithDetails,
    arabicEnabled: Boolean,
    fontResolver: FontResolver,
    scriptCode: String,
    arabicMultiplier: Float,
    externalQuranDb: ExternalQuranDatabase,
) {
    if (!arabicEnabled) return

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val contentColor = playerContentColor()
    val isDark = ThemeUtils.observeDarkTheme()

    val style = remember(
        vwd.pageNo,
        scriptCode,
        arabicMultiplier,
        fontResolver,
        colors,
        type,
        contentColor,
        isDark,
    ) {
        getQuranTextStyle(
            QuranTextStyleParams(
                fontResolver = fontResolver,
                colors = colors,
                type = type,
                pageNo = vwd.pageNo,
                script = scriptCode,
                sizeMultiplier = arabicMultiplier,
                useSmallSize = false,
                isDark = isDark
            ),
        ).copy(
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }

    val words = vwd.words

    val atlasPlacements by produceState<Map<Int, List<AtlasGlyphPlacement>>>(
        initialValue = emptyMap(),
        scriptCode,
        words,
    ) {
        value = withContext(Dispatchers.IO) {
            val atlasBundle = if (scriptCode.isQuranAtlasScript()) {
                QuranAtlasLoader.getBundle(externalQuranDb, scriptCode)
            } else null

            atlasBundle?.getPlacementsForWords(words, vwd.pageNo) ?: emptyMap()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(
                (0.5).dp,
                Alignment.Start
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            words.forEach { word ->
                QuranWordText(
                    word = word,
                    atlasPlacements = atlasPlacements.getForWord(word),
                    style = style,
                )
            }
        }
    }
}

@Composable
private fun SpotlightTranslationText(
    vwd: VerseWithDetails,
    translationMultiplier: Float,
    translationEnabled: Boolean,
) {
    if (!translationEnabled) return
    val translations = vwd.translations
    if (translations.isEmpty()) return

    val type = MaterialTheme.typography
    val contentColor = playerContentColor()

    SelectionContainer {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            translations.forEach { translation ->
                val tStyle = remember(translation.bookSlug, translationMultiplier, contentColor) {
                    getTranslationTextStyle(
                        TranslationTextStyleParams(
                            translation.bookSlug,
                            type,
                            translationMultiplier
                        ),
                    ).copy(color = contentColor.alpha(0.85f))
                }

                Text(
                    text = buildTranslationAnnotatedString(
                        translation,
                        colorScheme,
                        actions = null,
                        highlightParentheses = ReaderPreferences.observeTranslHighlightParentheses(),
                        showParentheses = ReaderPreferences.observeTranslShowParentheses(),
                    ),
                    style = tStyle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}


@Composable
private fun SpotlightPlaybackBar(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationPlayer,
) {
    val scope = rememberCoroutineScope()
    val arabicEnabled = ReaderPreferences.observeArabicTextEnabled()
    val translations = ReaderPreferences.observeTranslations()
    val translationEnabled = translations.isNotEmpty()
    
    val contentColor = playerContentColor()

    // Controls live on their own pill so they stay readable over any verse and in either theme.
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(contentColor.alpha(0.06f))
            .border(1.dp, contentColor.alpha(0.08f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    ReaderPreferences.setArabicTextEnabled(!arabicEnabled)
                }
            },
            modifier = Modifier.size(35.dp),
        ) {
            Icon(
                painterResource(Res.drawable.ic_mode_mushaf),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (arabicEnabled) playerContentColor() else playerContentColor().alpha(.3f),
            )
        }

        IconButton(
            onClick = {
                controller.seekLeft()
            },
            modifier = Modifier.size(35.dp),
        ) {
            Icon(
                painterResource(Res.drawable.ic_replay_5),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = playerContentColor().alpha(.7f),
            )
        }

        IconButton(
            onClick = {
                controller.previousVerse()
            },
            modifier = Modifier.size(35.dp),
        ) {
            Icon(
                painterResource(Res.drawable.ic_skip_back),
                contentDescription = stringResource(Res.string.strLabelPreviousVerse),
                modifier = Modifier.size(18.dp),
                tint = playerContentColor().alpha(.7f),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = colorScheme.primary.copy(alpha = 0.35f),
                    spotColor = colorScheme.primary.copy(alpha = 0.45f),
                )
                .clip(CircleShape)
                .background(colorScheme.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        controller.playPause()
                    },
                ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painterResource(
                        if (isPlaying) Res.drawable.ic_pause
                        else Res.drawable.ic_play,
                    ),
                    contentDescription = if (isPlaying) stringResource(Res.string.strLabelPause)
                    else stringResource(Res.string.strLabelPlay),
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.onPrimary,
                )
            }
        }

        IconButton(
            onClick = {
                controller.nextVerse()
            },
            modifier = Modifier.size(35.dp),
        ) {
            Icon(
                painterResource(Res.drawable.ic_skip_forward),
                contentDescription = stringResource(Res.string.strLabelNextVerse),
                modifier = Modifier.size(18.dp),
                tint = playerContentColor().alpha(.7f),
            )
        }

        IconButton(
            onClick = {
                controller.seekRight()
            },
            modifier = Modifier.size(35.dp),
        ) {
            Icon(
                painterResource(Res.drawable.ic_forward_5),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = playerContentColor().alpha(.7f),
            )
        }

        IconButton(
            onClick = {
                scope.launch {
                    if (translationEnabled) {
                        ReaderPreferences.setTranslations(emptySet())
                    } else {
                        ReaderPreferences.setTranslations(setOf(TranslUtils.TRANSL_SLUG_DEFAULT))
                    }
                }
            },
            modifier = Modifier.size(30.dp),
        ) {
            Icon(
                painterResource(Res.drawable.dr_icon_translations),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (translationEnabled) playerContentColor() else playerContentColor().alpha(.3f),
            )
        }
    }
}
