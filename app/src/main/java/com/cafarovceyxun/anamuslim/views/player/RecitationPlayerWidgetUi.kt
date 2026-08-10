package com.cafarovceyxun.anamuslim.views.player

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.Bitmap
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.MainActivity
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.AndroidThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.db.DatabaseProvider
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.extensions.dp2px
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationChapterArtwork
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationController
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelManager
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationService
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationServiceState
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Breakpoints for the recitation widget, in dp of the *actual* placed size (`SizeMode.Exact`).
 *
 * The widget is resizable from a one-cell strip up to four cells tall, so every surface below has to
 * survive both extremes rather than assume the default placement.
 */
private object WidgetBreakpoints {
    /** Above this height the player gets its own controls row instead of one crowded line. */
    const val TALL_PLAYER_MIN_HEIGHT_DP = 108f

    /**
     * Below this height the picker falls back to a slim back strip instead of the full header bar.
     * Set just under one launcher cell (~105dp on One UI) so an ordinary 5x1 widget still gets the
     * real header; only genuinely tiny placements lose it.
     */
    const val PICKER_HEADER_MIN_HEIGHT_DP = 96f

    /** Below this width prev/next are dropped so the title keeps a readable share of the strip. */
    const val TRANSPORT_MIN_WIDTH_DP = 300f

    /**
     * Width budget per verse cell, including its gutter.
     *
     * Generous on purpose. `LocalSize` under `SizeMode.Exact` reports the host's declared widget
     * size, which on One UI runs well above the view's real width (475 reported against ~330 drawn),
     * so a budget close to the intended cell size silently yields far too many, far too small
     * columns. The count is capped as well — a wide widget gets bigger cells, not more of them.
     */
    const val VERSE_CELL_WIDTH_DP = 78f
    const val VERSE_GRID_MIN_COLUMNS = 3
    const val VERSE_GRID_MAX_COLUMNS = 6

    /** Same over-reporting applies vertically, hence the matching slack and the row cap. */
    const val VERSE_CELL_HEIGHT_DP = 42f
    const val VERSE_GRID_MAX_ROWS = 4

}

internal sealed interface RecitationWidgetContent {
    /** Transport controls for whatever is playing (or was played last). */
    data class Player(
        val artwork: Bitmap,
        val title: String,
        val subtitle: String,
        val isPlaying: Boolean,
        val isLoading: Boolean,
        val openReaderIntent: Intent,
    ) : RecitationWidgetContent

    data class ChapterPicker(
        val chapters: List<ChapterListEntry>,
        val currentChapterNo: Int,
        /** Index of the first row of the visible page; see `PickerPager`. */
        val offset: Int,
    ) : RecitationWidgetContent

    data class VersePicker(
        val chapterNo: Int,
        val chapterName: String,
        val verseCount: Int,
        /** 0 when the playing verse belongs to another surah, so nothing gets highlighted. */
        val currentVerseNo: Int,
        val offset: Int,
    ) : RecitationWidgetContent
}

internal data class ChapterListEntry(
    val chapterNo: Int,
    val name: String,
    val verseCount: Int,
)

internal data class RecitationPlayerWidgetUiState(
    val colors: ColorScheme,
    val content: RecitationWidgetContent,
)

// ==================== State building ====================

internal suspend fun buildRecitationPlayerWidgetState(
    context: Context,
    navMode: String,
    navChapterNo: Int,
    navOffset: Int,
): RecitationPlayerWidgetUiState {
    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val systemDarkMode = uiMode == Configuration.UI_MODE_NIGHT_YES
    val colorScheme = AndroidThemeUtils.colorSchemeFromPreferences(context, systemDarkMode)

    val repository = DatabaseProvider.getQuranRepository(context)
    val verse = currentRecitationVerse()

    val content = when (navMode) {
        NAV_MODE_CHAPTERS -> buildChapterPickerContent(repository, verse, navOffset)
        NAV_MODE_VERSES -> buildVersePickerContent(repository, verse, navChapterNo, navOffset)
        else -> buildPlayerContent(context, repository, verse)
    }

    return RecitationPlayerWidgetUiState(colors = colorScheme, content = content)
}

/**
 * The verse the widget is "about": what the service is playing, or — once the service has shut down
 * — the last verse the user actually listened to, so a cold home screen still shows something real.
 */
internal suspend fun currentRecitationVerse(): ChapterVersePair {
    val playbackState = RecitationService.sharedState.value
    val lastVerse = RecitationPreferences.getLastPlayedVerse()

    val verse = if (playbackState == RecitationServiceState.EMPTY && lastVerse != null) {
        lastVerse
    } else {
        playbackState.currentVerse
    }

    return verse.takeIf { it.isValid } ?: ChapterVersePair(1, 1)
}

private suspend fun buildPlayerContent(
    context: Context,
    repository: QuranRepository,
    verse: ChapterVersePair,
): RecitationWidgetContent.Player {
    val controller = RecitationController.getInstance(context)
    val playbackState = RecitationService.sharedState.value
    val connected = controller.isConnectedState.value

    val isPlaying = if (connected) controller.isPlaying else playbackState.isPlaying
    val isLoading = if (connected) {
        controller.isLoading
    } else {
        playbackState.resolvingChapterNo != null || playbackState.isBuffering
    }

    val (chapterName, reciterName) = withContext(Dispatchers.IO) {
        coroutineScope {
            val chapterDeferred = async {
                repository.getChapterName(verse.chapterNo).ifBlank { verse.chapterNo.toString() }
            }

            val reciterDeferred = async {
                RecitationModelManager
                    .getCurrentReciterNameForAudioOption()
                    .ifBlank { context.getString(R.string.strTitleVerseRecitation) }
            }

            chapterDeferred.await() to reciterDeferred.await()
        }
    }

    val openReaderIntent = ReaderFactory
        .prepareSingleVerseIntent(verse.chapterNo, verse.verseNo)
        .apply {
            setClass(context, MainActivity::class.java)
            action = INTENT_ACTION_OPEN_READER
        }

    return RecitationWidgetContent.Player(
        artwork = RecitationChapterArtwork.getChapterArtworkBitmap(
            context,
            verse.chapterNo,
            context.dp2px(72f),
        ),
        title = context.getString(R.string.strLabelSurah, chapterName),
        subtitle = context.getString(
            R.string.strLabelVerseNoWithReciter,
            verse.verseNo,
            reciterName,
        ),
        isPlaying = isPlaying,
        isLoading = isLoading,
        openReaderIntent = openReaderIntent,
    )
}

private suspend fun buildChapterPickerContent(
    repository: QuranRepository,
    verse: ChapterVersePair,
    navOffset: Int,
): RecitationWidgetContent.ChapterPicker = withContext(Dispatchers.IO) {
    val chapterNos = QuranMeta.chapterRange.toList()
    // Two bulk reads rather than 114 round trips: names go through the localisation fallback chain,
    // ayah counts come straight off the surah rows.
    val names = repository.getChapterNames(chapterNos)
    val surahs = repository.getSurahsWithLocalizationsByChapterNos(chapterNos)

    RecitationWidgetContent.ChapterPicker(
        chapters = chapterNos.map { chapterNo ->
            ChapterListEntry(
                chapterNo = chapterNo,
                name = names[chapterNo] ?: chapterNo.toString(),
                verseCount = surahs[chapterNo]?.surah?.ayahCount ?: 0,
            )
        },
        currentChapterNo = verse.chapterNo,
        offset = navOffset,
    )
}

private suspend fun buildVersePickerContent(
    repository: QuranRepository,
    verse: ChapterVersePair,
    navChapterNo: Int,
    navOffset: Int,
): RecitationWidgetContent = withContext(Dispatchers.IO) {
    val chapterNo = navChapterNo.takeIf { QuranMeta.isChapterValid(it) } ?: verse.chapterNo
    val verseCount = repository.getChapterVerseCount(chapterNo)

    // An empty surah means the Quran database has not been prepared yet; fall back to the surah list
    // rather than rendering a picker with nothing to pick.
    if (verseCount <= 0) {
        return@withContext buildChapterPickerContent(repository, verse, 0)
    }

    RecitationWidgetContent.VersePicker(
        chapterNo = chapterNo,
        chapterName = repository.getChapterName(chapterNo).ifBlank { chapterNo.toString() },
        verseCount = verseCount,
        currentVerseNo = if (chapterNo == verse.chapterNo) verse.verseNo else 0,
        offset = navOffset,
    )
}

// ==================== Content ====================

@Composable
internal fun RecitationPlayerGlanceContent(
    context: Context,
    state: RecitationPlayerWidgetUiState?,
    widthDp: Float,
    heightDp: Float,
) {
    if (state == null) {
        WidgetPlaceholder(context)
        return
    }

    val colors = state.colors

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(18.dp)
            .background(colors.surfaceContainer),
    ) {
        when (val content = state.content) {
            is RecitationWidgetContent.Player -> PlayerFace(
                context = context,
                colors = colors,
                content = content,
                widthDp = widthDp,
                heightDp = heightDp,
            )

            is RecitationWidgetContent.ChapterPicker -> ChapterPickerFace(
                context = context,
                colors = colors,
                content = content,
                heightDp = heightDp,
            )

            is RecitationWidgetContent.VersePicker -> VersePickerFace(
                context = context,
                colors = colors,
                content = content,
                widthDp = widthDp,
                heightDp = heightDp,
            )
        }
    }
}

/**
 * Shown while the first load runs. Glance renders this synchronously, before any preference read, so
 * it deliberately avoids the themed colours the rest of the widget uses.
 */
@Composable
private fun WidgetPlaceholder(context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(18.dp)
            .background(ColorProvider(Color.Black)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// ==================== Player face ====================

@Composable
private fun PlayerFace(
    context: Context,
    colors: ColorScheme,
    content: RecitationWidgetContent.Player,
    widthDp: Float,
    heightDp: Float,
) {
    val isTall = heightDp >= WidgetBreakpoints.TALL_PLAYER_MIN_HEIGHT_DP

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(if (isTall) 14.dp else 10.dp)
            .clickable(actionStartActivity(content.openReaderIntent)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(content.artwork),
                contentDescription = null,
                modifier = GlanceModifier
                    .size(if (isTall) 56.dp else 44.dp)
                    .cornerRadius(if (isTall) 16.dp else 12.dp),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = if (isTall) 12.dp else 10.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = content.title,
                    style = TextStyle(
                        color = ColorProvider(colors.onSurface),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTall) 17.sp else 15.sp,
                    ),
                    maxLines = if (isTall) 2 else 1,
                )

                Spacer(modifier = GlanceModifier.height(3.dp))

                Text(
                    text = content.subtitle,
                    style = TextStyle(
                        color = ColorProvider(colors.onSurface.alpha(0.72f)),
                        fontSize = if (isTall) 13.sp else 12.sp,
                    ),
                    maxLines = 1,
                )
            }

            WidgetIconButton(
                colors = colors,
                icon = R.drawable.ic_widget_list,
                contentDescription = context.getString(R.string.strLabelSelectSurah),
                onClick = actionRunCallback<RecitationWidgetOpenNavigatorAction>(),
                sizeDp = if (isTall) 44 else 36,
            )

            // On a tall widget the transport moves to its own row below, so the header ends here.
            if (!isTall) {
                Spacer(modifier = GlanceModifier.width(6.dp))

                TransportControls(
                    context = context,
                    colors = colors,
                    content = content,
                    showSkip = widthDp >= WidgetBreakpoints.TRANSPORT_MIN_WIDTH_DP,
                    // The surah jumps are the first thing to go: on a one-cell strip six controls
                    // would leave the title nothing to sit in.
                    showChapterSkip = false,
                    skipSizeDp = 38,
                    playSizeDp = 48,
                    spacingDp = 6,
                )
            }
        }

        if (isTall) {
            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                TransportControls(
                    context = context,
                    colors = colors,
                    content = content,
                    showSkip = true,
                    showChapterSkip = true,
                    skipSizeDp = 50,
                    playSizeDp = 62,
                    spacingDp = 12,
                )
            }
        }
    }
}

@Composable
private fun TransportControls(
    context: Context,
    colors: ColorScheme,
    content: RecitationWidgetContent.Player,
    showSkip: Boolean,
    showChapterSkip: Boolean,
    skipSizeDp: Int,
    playSizeDp: Int,
    spacingDp: Int,
) {
    // Double chevrons for the surah jumps against the single skip glyphs for verses: same visual
    // language as the picker's pager, where the same pair means "move a bigger step".
    if (showChapterSkip) {
        WidgetIconButton(
            colors = colors,
            icon = R.drawable.ic_widget_page_back,
            contentDescription = context.getString(R.string.previousChapter),
            onClick = actionRunCallback<RecitationPlayerPreviousChapterAction>(),
            sizeDp = (skipSizeDp - 6).coerceAtLeast(32),
        )

        Spacer(modifier = GlanceModifier.width(spacingDp.dp))
    }

    if (showSkip) {
        WidgetIconButton(
            colors = colors,
            icon = R.drawable.ic_skip_back,
            contentDescription = context.getString(R.string.strLabelPreviousVerse),
            onClick = actionRunCallback<RecitationPlayerPreviousAction>(),
            sizeDp = skipSizeDp,
        )

        Spacer(modifier = GlanceModifier.width(spacingDp.dp))
    }

    WidgetPlayPauseButton(
        colors = colors,
        isLoading = content.isLoading,
        isPlaying = content.isPlaying,
        contentDescription = context.getString(
            when {
                content.isLoading -> R.string.textPreparingAudio
                content.isPlaying -> R.string.strLabelPause
                else -> R.string.strLabelPlay
            }
        ),
        onClick = actionRunCallback<RecitationPlayerToggleAction>(),
        sizeDp = playSizeDp,
    )

    if (showSkip) {
        Spacer(modifier = GlanceModifier.width(spacingDp.dp))

        WidgetIconButton(
            colors = colors,
            icon = R.drawable.ic_skip_forward,
            contentDescription = context.getString(R.string.strLabelNextVerse),
            onClick = actionRunCallback<RecitationPlayerNextAction>(),
            sizeDp = skipSizeDp,
        )
    }

    if (showChapterSkip) {
        Spacer(modifier = GlanceModifier.width(spacingDp.dp))

        WidgetIconButton(
            colors = colors,
            icon = R.drawable.ic_widget_page_forward,
            contentDescription = context.getString(R.string.nextChapter),
            onClick = actionRunCallback<RecitationPlayerNextChapterAction>(),
            sizeDp = (skipSizeDp - 6).coerceAtLeast(32),
        )
    }
}

// ==================== Picker faces ====================
//
// Deliberately built from plain Columns and Rows with a pager, not `LazyColumn`/`LazyVerticalGrid`.
//
// A Glance lazy container is a RemoteViews collection, and the framework only accepts an *Activity*
// PendingIntent as a collection's click template — so Glance routes every item tap through
// `InvisibleActionTrampolineActivity`. On a home screen that means: launch an invisible activity,
// cold-start the app process if it is not running, then finally run the callback. Samsung's
// background management cuts that short (`Skip pre-destroyed transaction item: LaunchActivityItem
// {dat=glance-action:...}`), and on this device only 2 of 8 taps ever reached the callback — the
// list scrolled beautifully and selecting a surah did nothing. Outside a collection the same action
// compiles to a plain broadcast PendingIntent, which is what the transport buttons use and why they
// never missed.
//
// Paging is the price. It is kept small by opening the picker at the surah/verse already playing
// rather than at item 1, so the common case needs no paging at all.

/** Vertical chrome the pager and header take away from the rows. */
private const val PICKER_PAGER_HEIGHT_DP = 46f
private const val PICKER_HEADER_HEIGHT_DP = 44f
private const val PICKER_COMPACT_HEADER_HEIGHT_DP = 30f
private const val CHAPTER_ROW_HEIGHT_DP = 42f

/** How far the double arrows jump, in pages. */
private const val PICKER_FAST_PAGE_STEP = 5

@Composable
private fun ChapterPickerFace(
    context: Context,
    colors: ColorScheme,
    content: RecitationWidgetContent.ChapterPicker,
    heightDp: Float,
) {
    val showHeader = heightDp >= WidgetBreakpoints.PICKER_HEADER_MIN_HEIGHT_DP
    val chromeDp =
        (if (showHeader) PICKER_HEADER_HEIGHT_DP else PICKER_COMPACT_HEADER_HEIGHT_DP) +
            PICKER_PAGER_HEIGHT_DP

    val pageSize = ((heightDp - chromeDp) / CHAPTER_ROW_HEIGHT_DP).toInt().coerceAtLeast(1)
    val total = content.chapters.size
    val firstIndex = content.offset.coerceIn(0, (total - pageSize).coerceAtLeast(0))

    Column(modifier = GlanceModifier.fillMaxSize()) {
        PickerChrome(
            context = context,
            colors = colors,
            showHeader = showHeader,
            title = context.getString(R.string.strLabelSelectSurah),
            subtitle = null,
            backAction = actionRunCallback<RecitationWidgetCloseNavigatorAction>(),
        )

        Column(modifier = GlanceModifier.defaultWeight()) {
            repeat(pageSize) { row ->
                val index = firstIndex + row

                if (index < total) {
                    val chapter = content.chapters[index]

                    ChapterRow(
                        context = context,
                        colors = colors,
                        chapter = chapter,
                        isCurrent = chapter.chapterNo == content.currentChapterNo,
                    )
                }
            }
        }

        PickerPager(
            colors = colors,
            firstIndex = firstIndex,
            pageSize = pageSize,
            total = total,
        )
    }
}

@Composable
private fun VersePickerFace(
    context: Context,
    colors: ColorScheme,
    content: RecitationWidgetContent.VersePicker,
    widthDp: Float,
    heightDp: Float,
) {
    val showHeader = heightDp >= WidgetBreakpoints.PICKER_HEADER_MIN_HEIGHT_DP

    val chromeDp =
        (if (showHeader) PICKER_HEADER_HEIGHT_DP else PICKER_COMPACT_HEADER_HEIGHT_DP) +
            PICKER_PAGER_HEIGHT_DP

    val columns = (widthDp / WidgetBreakpoints.VERSE_CELL_WIDTH_DP)
        .toInt()
        .coerceIn(
            WidgetBreakpoints.VERSE_GRID_MIN_COLUMNS,
            WidgetBreakpoints.VERSE_GRID_MAX_COLUMNS,
        )

    val rows = ((heightDp - chromeDp) / WidgetBreakpoints.VERSE_CELL_HEIGHT_DP)
        .toInt()
        .coerceIn(1, WidgetBreakpoints.VERSE_GRID_MAX_ROWS)

    val pageSize = columns * rows

    val total = content.verseCount
    val firstIndex = content.offset.coerceIn(0, (total - pageSize).coerceAtLeast(0))

    Column(modifier = GlanceModifier.fillMaxSize()) {
        PickerChrome(
            context = context,
            colors = colors,
            showHeader = showHeader,
            title = content.chapterName,
            subtitle = context.getString(R.string.strLabelVerseCountShort, total),
            backAction = actionRunCallback<RecitationWidgetOpenNavigatorAction>(),
            // Most of the time the intent is "play this surah", not "play verse 47". It rides in the
            // header rather than on a row of its own: a row cost 40dp of a widget that only has room
            // for two or three, and the number grid is what the user came here for.
            trailingAction = PickerHeaderAction(
                icon = R.drawable.ic_play,
                contentDescription = context.getString(R.string.strLabelPlayFromBeginning),
                onClick = actionRunCallback<RecitationWidgetPlayVerseAction>(
                    actionParametersOf(
                        WidgetActionKeys.chapterNo to content.chapterNo,
                        WidgetActionKeys.verseNo to 1,
                    )
                ),
            ),
        )

        Column(modifier = GlanceModifier.defaultWeight()) {
            repeat(rows) { row ->
                VerseGridRow(
                    // Weighted rather than a fixed height: the reported widget size overshoots the
                    // real one, so a hard height would let the host squeeze the rows down to
                    // unreadable slivers. Sharing the space that actually exists keeps the cells as
                    // tall as they can be.
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    colors = colors,
                    chapterNo = content.chapterNo,
                    firstVerseNo = firstIndex + row * columns + 1,
                    columns = columns,
                    verseCount = total,
                    currentVerseNo = content.currentVerseNo,
                )
            }
        }

        PickerPager(
            colors = colors,
            firstIndex = firstIndex,
            pageSize = pageSize,
            total = total,
        )
    }
}

/** An extra affordance the header hosts next to "close", currently "play from the beginning". */
private data class PickerHeaderAction(
    val icon: Int,
    val contentDescription: String,
    val onClick: Action,
)

@Composable
private fun PickerChrome(
    context: Context,
    colors: ColorScheme,
    showHeader: Boolean,
    title: String,
    subtitle: String?,
    backAction: Action,
    trailingAction: PickerHeaderAction? = null,
) {
    if (showHeader) {
        PickerHeader(context, colors, title, subtitle, backAction, trailingAction)
    } else {
        CompactBackRow(context, colors, backAction, title)
    }
}

@Composable
private fun PickerHeader(
    context: Context,
    colors: ColorScheme,
    title: String,
    subtitle: String?,
    backAction: Action,
    trailingAction: PickerHeaderAction?,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(PICKER_HEADER_HEIGHT_DP.dp)
            .background(colors.surfaceContainerHigh)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        WidgetIconButton(
            colors = colors,
            icon = R.drawable.ic_widget_back,
            contentDescription = context.getString(R.string.strLabelBack),
            onClick = backAction,
            sizeDp = 32,
            filled = false,
        )

        Column(
            modifier = GlanceModifier.defaultWeight().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = ColorProvider(colors.onSurface),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = ColorProvider(colors.onSurface.alpha(0.6f)),
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
            }
        }

        if (trailingAction != null) {
            WidgetIconButton(
                colors = colors,
                icon = trailingAction.icon,
                contentDescription = trailingAction.contentDescription,
                onClick = trailingAction.onClick,
                sizeDp = 36,
                filled = true,
                tint = colors.primary,
            )

            Spacer(modifier = GlanceModifier.width(4.dp))
        }

        WidgetIconButton(
            colors = colors,
            icon = R.drawable.ic_widget_close,
            contentDescription = context.getString(R.string.strLabelClose),
            onClick = actionRunCallback<RecitationWidgetCloseNavigatorAction>(),
            sizeDp = 36,
            filled = false,
        )
    }
}

/**
 * Stand-in for [PickerHeader] on a one-cell widget: a single 30dp strip instead of a 44dp bar with a
 * subtitle, because at that height every dp spent on chrome is a row of content not shown.
 */
@Composable
private fun CompactBackRow(
    context: Context,
    colors: ColorScheme,
    backAction: Action,
    label: String,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(PICKER_COMPACT_HEADER_HEIGHT_DP.dp)
            .background(colors.surfaceContainerHigh)
            .padding(horizontal = 8.dp)
            .clickable(backAction),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_back),
            contentDescription = context.getString(R.string.strLabelBack),
            modifier = GlanceModifier.size(15.dp),
            colorFilter = ColorFilter.tint(ColorProvider(colors.onSurface.alpha(0.6f))),
        )

        Text(
            text = label,
            modifier = GlanceModifier.padding(horizontal = 6.dp),
            style = TextStyle(
                color = ColorProvider(colors.onSurface),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

/**
 * `‹‹ ‹ 31–34 / 114 › ››`.
 *
 * The window is expressed in item offsets rather than page numbers because only this composable
 * knows how many rows the current widget size fits — the callbacks just store the number they are
 * handed, which keeps the state valid when the widget is resized between taps.
 */
@Composable
private fun PickerPager(
    colors: ColorScheme,
    firstIndex: Int,
    pageSize: Int,
    total: Int,
) {
    val lastIndex = (firstIndex + pageSize - 1).coerceAtMost(total - 1)
    val maxOffset = (total - pageSize).coerceAtLeast(0)
    val fastStep = pageSize * PICKER_FAST_PAGE_STEP

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(PICKER_PAGER_HEIGHT_DP.dp)
            .background(colors.surfaceContainerHigh)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        PagerButton(
            colors = colors,
            icon = R.drawable.ic_widget_page_back,
            targetOffset = (firstIndex - fastStep).coerceIn(0, maxOffset),
            enabled = firstIndex > 0,
        )

        PagerButton(
            colors = colors,
            icon = R.drawable.ic_widget_back,
            targetOffset = (firstIndex - pageSize).coerceIn(0, maxOffset),
            enabled = firstIndex > 0,
        )

        Text(
            text = "${firstIndex + 1}–${lastIndex + 1} / $total",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(colors.onSurface.alpha(0.7f)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )

        PagerButton(
            colors = colors,
            icon = R.drawable.ic_widget_forward,
            targetOffset = (firstIndex + pageSize).coerceIn(0, maxOffset),
            enabled = firstIndex < maxOffset,
        )

        PagerButton(
            colors = colors,
            icon = R.drawable.ic_widget_page_forward,
            targetOffset = (firstIndex + fastStep).coerceIn(0, maxOffset),
            enabled = firstIndex < maxOffset,
        )
    }
}

@Composable
private fun PagerButton(
    colors: ColorScheme,
    icon: Int,
    targetOffset: Int,
    enabled: Boolean,
) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .cornerRadius(99.dp)
            .background(if (enabled) colors.surfaceContainer else Color.Transparent)
            .clickable(
                actionRunCallback<RecitationWidgetNavPageAction>(
                    actionParametersOf(WidgetActionKeys.navOffset to targetOffset)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(
                ColorProvider(colors.onSurface.alpha(if (enabled) 0.9f else 0.25f))
            ),
        )
    }
}

@Composable
private fun ChapterRow(
    context: Context,
    colors: ColorScheme,
    chapter: ChapterListEntry,
    isCurrent: Boolean,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(CHAPTER_ROW_HEIGHT_DP.dp)
            .background(if (isCurrent) colors.primaryContainer else Color.Transparent)
            .padding(horizontal = 10.dp)
            .clickable(
                actionRunCallback<RecitationWidgetSelectChapterAction>(
                    actionParametersOf(WidgetActionKeys.chapterNo to chapter.chapterNo)
                )
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(26.dp)
                .cornerRadius(9.dp)
                .background(if (isCurrent) colors.primary else colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = chapter.chapterNo.toString(),
                style = TextStyle(
                    color = ColorProvider(
                        if (isCurrent) colors.onPrimary else colors.onSurface.alpha(0.7f)
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }

        Text(
            text = chapter.name,
            modifier = GlanceModifier.defaultWeight().padding(horizontal = 10.dp),
            style = TextStyle(
                color = ColorProvider(
                    if (isCurrent) colors.onPrimaryContainer else colors.onSurface
                ),
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
        )

        Text(
            text = if (chapter.verseCount > 0) {
                context.getString(R.string.strLabelVerseCountShort, chapter.verseCount)
            } else {
                ""
            },
            style = TextStyle(
                color = ColorProvider(
                    if (isCurrent) {
                        colors.onPrimaryContainer.alpha(0.7f)
                    } else {
                        colors.onSurface.alpha(0.45f)
                    }
                ),
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun VerseGridRow(
    modifier: GlanceModifier,
    colors: ColorScheme,
    chapterNo: Int,
    firstVerseNo: Int,
    columns: Int,
    verseCount: Int,
    currentVerseNo: Int,
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        repeat(columns) { column ->
            val verseNo = firstVerseNo + column

            Box(
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                // The last page is usually short; leftover cells stay as empty spacers so the
                // numbers above them keep their column.
                if (verseNo in 1..verseCount) {
                    VerseCell(
                        colors = colors,
                        chapterNo = chapterNo,
                        verseNo = verseNo,
                        isCurrent = verseNo == currentVerseNo,
                    )
                }
            }
        }
    }
}

@Composable
private fun VerseCell(
    colors: ColorScheme,
    chapterNo: Int,
    verseNo: Int,
    isCurrent: Boolean,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(11.dp)
            .background(if (isCurrent) colors.primary else colors.surfaceContainerHigh)
            .clickable(
                actionRunCallback<RecitationWidgetPlayVerseAction>(
                    actionParametersOf(
                        WidgetActionKeys.chapterNo to chapterNo,
                        WidgetActionKeys.verseNo to verseNo,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = verseNo.toString(),
            style = TextStyle(
                color = ColorProvider(
                    if (isCurrent) colors.onPrimary else colors.onSurface.alpha(0.85f)
                ),
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

// ==================== Shared buttons ====================

@Composable
private fun WidgetPlayPauseButton(
    colors: ColorScheme,
    isLoading: Boolean,
    isPlaying: Boolean,
    contentDescription: String,
    onClick: Action,
    sizeDp: Int,
) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .cornerRadius(99.dp)
            .background(colors.primary)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size(iconSizeFor(sizeDp, 0.5f, 20).dp),
                color = ColorProvider(colors.onPrimary),
            )
        } else {
            Image(
                provider = ImageProvider(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                ),
                contentDescription = contentDescription,
                modifier = GlanceModifier.size(iconSizeFor(sizeDp, 0.56f, 20).dp),
                colorFilter = ColorFilter.tint(ColorProvider(colors.onPrimary)),
            )
        }
    }
}

@Composable
private fun WidgetIconButton(
    colors: ColorScheme,
    icon: Int,
    contentDescription: String,
    onClick: Action,
    sizeDp: Int,
    filled: Boolean = true,
    tint: Color? = null,
) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .cornerRadius(99.dp)
            .background(if (filled) colors.surfaceContainerHigh else Color.Transparent)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSizeFor(sizeDp, 0.56f, 17).dp),
            colorFilter = ColorFilter.tint(ColorProvider(tint ?: colors.onSurface)),
        )
    }
}

/**
 * Glyph size as a share of its button, instead of a fixed inset.
 *
 * A constant inset makes small buttons look empty and large ones look under-filled; a ratio keeps
 * every control on the widget reading at the same weight whatever size the layout picked for it.
 */
private fun iconSizeFor(buttonSizeDp: Int, ratio: Float, minDp: Int): Int =
    (buttonSizeDp * ratio).toInt().coerceAtLeast(minDp)
