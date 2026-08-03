package com.cafarovceyxun.anamuslim.views.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Single process-lifetime scope shared by every app widget refresh.
 *
 * Refreshes used to be launched from throwaway `CoroutineScope(Dispatchers.Default)` instances built
 * at the call site: each one owns a job nothing ever cancels, so a burst of playback events leaves a
 * pile of orphaned scopes behind.
 */
internal val appWidgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Re-renders every placed instance of this widget.
 *
 * The [refreshKey] bump is not bookkeeping — Glance only relaunches the `produceState` that loads the
 * widget's data when its keys change, and the Glance state is one of those keys. Without a changing
 * value here an [update] on a live widget would recompose against the previous, stale snapshot.
 */
internal fun GlanceAppWidget.refreshAllInstances(
    context: Context,
    refreshKey: Preferences.Key<Long>,
) {
    val widget = this
    // Resolved out here on purpose: inside the coroutine `javaClass` would be the scope's, not ours.
    val providerClass = javaClass

    appWidgetScope.launch {
        GlanceAppWidgetManager(context).getGlanceIds(providerClass).forEach { glanceId ->
            widget.updateInstance(context, glanceId) { it[refreshKey] = System.currentTimeMillis() }
        }
    }
}

/** Applies [edit] to one instance's Glance state and re-renders just that instance. */
internal suspend fun GlanceAppWidget.updateInstance(
    context: Context,
    glanceId: GlanceId,
    edit: (MutablePreferences) -> Unit,
) {
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply(edit)
    }

    update(context, glanceId)
}
