package com.cafarovceyxun.anamuslim.views.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetPinner
import com.cafarovceyxun.anamuslim.views.player.RecitationPlayerWidgetReceiver
import com.cafarovceyxun.anamuslim.views.reader.VotdWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Places widgets through the launcher's own pin dialog.
 *
 * `requestPinAppWidget` arrived in API 26 and is optional even above it — a launcher that answers
 * `false` to [AppWidgetManager.isRequestPinAppWidgetSupported] silently ignores the request. Both
 * cases are reported as "nothing to offer" so Settings hides the section rather than showing a row
 * that does nothing when tapped.
 */
class AndroidHomeWidgetPinner(context: Context) : HomeWidgetPinner {
    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)

    override suspend fun offerableWidgets(): List<HomeWidgetKind> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext emptyList()
        if (!manager.isRequestPinAppWidgetSupported) return@withContext emptyList()

        HomeWidgetKind.entries
    }

    override fun requestPin(kind: HomeWidgetKind) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!manager.isRequestPinAppWidgetSupported) return

        // No success callback: the confirmation dialog belongs to the launcher, and the outcome is
        // visible on the home screen. Settings re-reads the pinned set when it resumes.
        manager.requestPinAppWidget(kind.provider(), null, null)
    }

    private fun HomeWidgetKind.provider(): ComponentName {
        val receiver = when (this) {
            HomeWidgetKind.RecitationPlayer -> RecitationPlayerWidgetReceiver::class.java
            HomeWidgetKind.VerseOfTheDay -> VotdWidgetReceiver::class.java
        }

        return ComponentName(appContext, receiver)
    }
}
