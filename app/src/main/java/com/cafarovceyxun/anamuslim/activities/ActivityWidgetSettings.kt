package com.cafarovceyxun.anamuslim.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.compose.screens.settings.WidgetSettingsScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetPinProvider
import com.cafarovceyxun.anamuslim.compose.utils.localizedAppContext
import com.cafarovceyxun.anamuslim.views.widget.homeWidgetKindOf
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Vidcet görünüşü ekranı. İki giriş yolu var:
 *
 * - **Vidcet konfiqurasiyası** — ana ekranda vidcetə uzun basanda launcher-in «Ayarlar» düyməsi
 *   (`*_widget_info.xml` → `android:configure`, `widgetFeatures="reconfigurable"`). Intent
 *   `EXTRA_APPWIDGET_ID` daşıyır; ekran həmin vidcetin növünü və həqiqi ölçüsünü göstərir.
 * - **Tətbiq Ayarları** — `HomeWidgetPinner.openAppearanceSettings()`, id-siz.
 *
 * ⚠️ `RESULT_OK` **dərhal** qoyulur. `configuration_optional` yalnız API 31+-dadır: köhnə
 * launcher-lər vidceti yerləşdirəndə də bu ekranı açır və nəticə `RESULT_CANCELED` olsa (istifadəçi
 * sadəcə geri basır) **vidceti silir**. Burada «ləğv» anlayışı yoxdur — ayarlar dərhal yazılır —
 * ona görə hər çıxış uğurludur.
 */
class ActivityWidgetSettings : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )

        val manager = AppWidgetManager.getInstance(this)
        val placedKind = appWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            ?.let { homeWidgetKindOf(manager.getAppWidgetInfo(it)?.provider) }
        val placedSize = placedKind?.let { placedSizeOf(manager, appWidgetId) }

        // Vidcetin tarix sətri ilə eyni yol (`PrayerWidgetReceiver.dateLine`): tətbiq dilində.
        val locale = localizedAppContext(this).resources.configuration.locales[0]
        val weekdayName = { atMillis: Long -> SimpleDateFormat("EEEE", locale).format(Date(atMillis)) }

        setContent {
            QuranAppTheme {
                WidgetSettingsScreen(
                    initialKind = placedKind ?: HomeWidgetKind.entries.first(),
                    placedSize = placedSize,
                    weekdayName = weekdayName,
                )
            }
        }
    }

    /**
     * Launcher-in bildirdiyi ölçü. Portret rejimdə en `MIN_WIDTH`, hündürlük `MAX_HEIGHT`-dir
     * (Glance `SizeMode.Exact` də portretdə bu cütü götürür); bəzi launcher-lər isə dəqiq
     * `appWidgetSizes` siyahısını verir — o varsa onu götür.
     */
    private fun placedSizeOf(manager: AppWidgetManager, appWidgetId: Int): DpSize? {
        val options = manager.getAppWidgetOptions(appWidgetId)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            val exact = options.getParcelableArrayList<android.util.SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            exact?.firstOrNull()?.let { return DpSize(it.width.dp, it.height.dp) }
        }

        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
    }

    override fun onPause() {
        super.onPause()
        // Sürüşdürücü özü buraxılanda yeniləyir; bu, launcher-ə qayıdanda son dəyərin çəkildiyinə
        // zəmanətdir (məs. yerləşdirmə axınında konfiqurasiyadan əvvəl çəkilmiş ilk görünüş).
        if (isFinishing) HomeWidgetPinProvider.pinner.refreshPlacedWidgets()
    }
}
