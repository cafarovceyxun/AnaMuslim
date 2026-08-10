package com.cafarovceyxun.anamuslim.views.player

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.localizedAppContext
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationController
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationService
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationServiceState
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.views.widget.appWidgetScope
import com.cafarovceyxun.anamuslim.views.widget.refreshAllInstances
import com.cafarovceyxun.anamuslim.views.widget.updateInstance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The widget shows one of three faces; which one is per-instance state, so two copies of the widget
// can sit on the same home screen with one browsing surahs while the other keeps showing the player.
internal const val NAV_MODE_PLAYER = "player"
internal const val NAV_MODE_CHAPTERS = "chapters"
internal const val NAV_MODE_VERSES = "verses"

private val KEY_LAST_UPDATE = longPreferencesKey("recitation_player_last_update")
private val KEY_NAV_MODE = stringPreferencesKey("recitation_player_nav_mode")
private val KEY_NAV_CHAPTER = intPreferencesKey("recitation_player_nav_chapter")

/** Index of the first item on the visible picker page. Stored in items, not pages: see `PickerPager`. */
private val KEY_NAV_OFFSET = intPreferencesKey("recitation_player_nav_offset")

internal object WidgetActionKeys {
    val chapterNo = ActionParameters.Key<Int>("chapterNo")
    val verseNo = ActionParameters.Key<Int>("verseNo")
    val navOffset = ActionParameters.Key<Int>("navOffset")
}

private data class RecitationWidgetRefreshTrigger(
    val service: RecitationServiceState,
    val controllerPlaying: Boolean,
    val controllerBuffering: Boolean,
    val controllerConnected: Boolean,
    val appearance: Triple<String, String, Boolean>,
)

class RecitationPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecitationPlayerGlanceWidget()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        val manager = GlanceAppWidgetManager(context)
        val widget = RecitationPlayerGlanceWidget()
        val glanceId = manager.getGlanceIdBy(appWidgetId)

        appWidgetScope.launch {
            widget.update(context, glanceId)
        }
    }
}

internal class RecitationPlayerGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Every label below goes through this context; see [localizedAppContext] for why the plain
        // one renders in the phone's language instead of the app's.
        val localizedContext = localizedAppContext(context)

        provideContent {
            val sizes = LocalSize.current
            val glanceState = currentState<Preferences>()

            val navMode = glanceState[KEY_NAV_MODE] ?: NAV_MODE_PLAYER
            val navChapterNo = glanceState[KEY_NAV_CHAPTER] ?: 0
            val navOffset = glanceState[KEY_NAV_OFFSET] ?: 0

            val state by produceState<RecitationPlayerWidgetUiState?>(null, sizes, glanceState) {
                try {
                    value = buildRecitationPlayerWidgetState(
                        context = localizedContext,
                        navMode = navMode,
                        navChapterNo = navChapterNo,
                        navOffset = navOffset,
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    e.printStackTrace()
                }
            }

            RecitationPlayerGlanceContent(
                context = localizedContext,
                state = state,
                widthDp = sizes.width.value,
                heightDp = sizes.height.value,
            )
        }
    }
}

fun updateAllRecitationPlayerWidgets(context: Context) {
    RecitationPlayerGlanceWidget().refreshAllInstances(context, KEY_LAST_UPDATE)
}

fun startRecitationPlayerWidgetObserver(context: Context) {
    val app = context.applicationContext

    appWidgetScope.launch {
        val controller = RecitationController.getInstance(app)

        combine(
            RecitationService.sharedState,
            controller.isPlayingState,
            controller.isBufferingState,
            controller.isConnectedState,
            ThemeUtils.widgetAppearancePreferencesFlow(),
        ) { service, playing, buffering, connected, appearance ->
            RecitationWidgetRefreshTrigger(service, playing, buffering, connected, appearance)
        }
            .distinctUntilChanged()
            .collect {
                updateAllRecitationPlayerWidgets(app)
            }
    }
}

// ==================== Actions ====================

private suspend fun runRecitationControls(
    context: Context,
    block: suspend RecitationController.() -> Unit,
) {
    withContext(Dispatchers.Main) {
        RecitationController.getInstance(context).block()
    }
}

/** Switches one instance to another face and re-renders only that instance. */
private suspend fun setWidgetFace(
    context: Context,
    glanceId: GlanceId,
    edit: (MutablePreferences) -> Unit,
) {
    RecitationPlayerGlanceWidget().updateInstance(context, glanceId, edit)
}

class RecitationPlayerToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        runRecitationControls(context) { playPause() }
        updateAllRecitationPlayerWidgets(context)
    }
}

class RecitationPlayerPreviousAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        runRecitationControls(context) { previousVerse() }
        updateAllRecitationPlayerWidgets(context)
    }
}

class RecitationPlayerNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        runRecitationControls(context) { nextVerse() }
        updateAllRecitationPlayerWidgets(context)
    }
}

/**
 * Jumps whole surahs.
 *
 * The controller has no chapter-level command — [RecitationController.nextVerse] only ever steps one
 * ayah — so this restarts playback at the first verse of the neighbouring surah.
 */
private suspend fun jumpChapter(context: Context, delta: Int) {
    val current = currentRecitationVerse().chapterNo
    val target = (current + delta).coerceIn(QuranMeta.chapterRange)

    if (target == current) return

    runRecitationControls(context) { start(ChapterVersePair(target, 1)) }
    updateAllRecitationPlayerWidgets(context)
}

class RecitationPlayerPreviousChapterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        jumpChapter(context, -1)
    }
}

class RecitationPlayerNextChapterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        jumpChapter(context, +1)
    }
}

/**
 * Opens the surah list — also the "back" step out of the verse grid.
 *
 * The page opens on the surah being recited rather than at Al-Fatihah, which is what keeps a paged
 * picker practical: the item the user is most likely to want is already on screen.
 */
class RecitationWidgetOpenNavigatorAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val currentChapterNo = currentRecitationVerse().chapterNo

        setWidgetFace(context, glanceId) {
            it[KEY_NAV_MODE] = NAV_MODE_CHAPTERS
            it[KEY_NAV_OFFSET] = (currentChapterNo - 1).coerceAtLeast(0)
        }
    }
}

class RecitationWidgetCloseNavigatorAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        setWidgetFace(context, glanceId) { it[KEY_NAV_MODE] = NAV_MODE_PLAYER }
    }
}

class RecitationWidgetSelectChapterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val chapterNo = parameters[WidgetActionKeys.chapterNo] ?: return
        val currentVerse = currentRecitationVerse()

        setWidgetFace(context, glanceId) {
            it[KEY_NAV_MODE] = NAV_MODE_VERSES
            it[KEY_NAV_CHAPTER] = chapterNo

            // Same idea as opening the surah list: land on the verse in progress when the user
            // picked the surah they are already listening to, otherwise start at its first verse.
            it[KEY_NAV_OFFSET] = if (chapterNo == currentVerse.chapterNo) {
                (currentVerse.verseNo - 1).coerceAtLeast(0)
            } else {
                0
            }
        }
    }
}

/** Moves the picker window; the offset is computed by the UI, which knows how many rows fit. */
class RecitationWidgetNavPageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val offset = parameters[WidgetActionKeys.navOffset] ?: return

        setWidgetFace(context, glanceId) { it[KEY_NAV_OFFSET] = offset.coerceAtLeast(0) }
    }
}

/** Starts recitation at the chosen verse and drops the widget back to the player face. */
class RecitationWidgetPlayVerseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val chapterNo = parameters[WidgetActionKeys.chapterNo] ?: return
        val verseNo = parameters[WidgetActionKeys.verseNo] ?: return

        runRecitationControls(context) { start(ChapterVersePair(chapterNo, verseNo)) }

        setWidgetFace(context, glanceId) { it[KEY_NAV_MODE] = NAV_MODE_PLAYER }
    }
}
