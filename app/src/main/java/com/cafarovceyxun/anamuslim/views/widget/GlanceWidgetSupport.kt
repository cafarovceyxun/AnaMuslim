package com.cafarovceyxun.anamuslim.views.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
 *
 * ⚠️ **İd-lər [receiver]-dən alınır, `GlanceAppWidgetManager.getGlanceIds(providerClass)`-dan yox.**
 * Həmin API vidcet sinfinin `canonicalName`-i ilə açarlanan **öz DataStore xəritəsindən** keçir
 * (`provider:<sinif adı>` → receiver), xəritəni isə hər receiver `onUpdate`-də yazır. Glance-ın öz
 * consumer ProGuard qaydaları yalnız `ActionCallback`-i saxlayır, `GlanceAppWidget` varislərini yox
 * — release-də (`isMinifyEnabled = true`) sinif adları R8-in verdiyi adlardır və **buraxılışdan
 * buraxılışa sürüşür**. Xəritə isə cihazda qalır: bir güncəllədən sonra `a.b` adı artıq başqa vidcet
 * sinfinə düşür, köhnə sətir isə hələ də əvvəlki receiver-i göstərir → `update()` səhv
 * `appWidgetId`-yə yazır və ana ekranda **pleyer vidceti günün ayəsi kimi görünür**. `update()`
 * yalnız id ilə işlədiyi üçün id-ləri manifestdəki `ComponentName`-dən almaq bu zənciri tamamilə
 * kəsir (id-lər sistemdədir, obfuskasiyadan asılı deyil) və xəritəsi artıq korlanmış cihazları da
 * özü sağaldır.
 */
internal fun GlanceAppWidget.refreshAllInstances(
    context: Context,
    receiver: Class<out GlanceAppWidgetReceiver>,
    refreshKey: Preferences.Key<Long>,
) {
    val widget = this
    val provider = ComponentName(context, receiver)

    appWidgetScope.launch {
        val manager = GlanceAppWidgetManager(context)

        AppWidgetManager.getInstance(context).getAppWidgetIds(provider).forEach { appWidgetId ->
            val glanceId = manager.getGlanceIdBy(appWidgetId)

            widget.updateInstance(context, glanceId) { it[refreshKey] = System.currentTimeMillis() }
        }
    }
}

/**
 * Re-renders one instance after the host resized it.
 *
 * Needed by every widget that reads `LocalSize`: under `SizeMode.Exact` the composition is built for
 * the size the host declared *at that moment*, so a resize without a re-render leaves the widget
 * laid out for its old width.
 */
internal fun GlanceAppWidget.updateInstanceOnResize(context: Context, appWidgetId: Int) {
    val widget = this
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)

    appWidgetScope.launch { widget.update(context, glanceId) }
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
