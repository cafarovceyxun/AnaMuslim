package com.cafarovceyxun.anamuslim.utils.others

import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplicationShortcutItem

/**
 * Android-dəki launcher shortcut-un iOS qarşılığı: ikona basıb saxlayanda çıxan «İdarəetmə paneli»
 * quick action-ı. Yalnız sessiya varsa qoyulur — [AdminShortcutSync] onu idarə edir.
 *
 * Etiket azərbaycancadır və sabitdir: admin UI-nin qalanı kimi lokalizə fayllarına düşmür.
 */
object IosAdminShortcut {

    const val TYPE_ADMIN = "admin_hub"

    private const val TITLE = "İdarəetmə paneli"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Toxunuş idarəçisini və paylaşılan [AdminShortcut] seam-ini bağlayır. */
    fun install() {
        IosQuickActions.registerHandler(TYPE_ADMIN) { open() }
        AdminShortcut.push = { subtitle ->
            IosQuickActions.setItem(
                UIApplicationShortcutItem(
                    type = TYPE_ADMIN,
                    localizedTitle = TITLE,
                    localizedSubtitle = subtitle,
                    icon = null,
                    userInfo = null,
                ),
            )
        }
        AdminShortcut.remove = { IosQuickActions.removeItem(TYPE_ADMIN) }
    }

    /** Soyuq açılışda toxunuş naviqasiya hook-undan əvvəl gəlir; qısa müddət gözləyib açırıq. */
    private fun open() {
        scope.launch {
            repeat(50) {
                val open = ReaderUiHooks.openSettingsRoute
                if (open != null) {
                    withContext(Dispatchers.Main) { open(SettingRoutes.ADMIN_HUB) }
                    return@launch
                }
                delay(100)
            }
            AppLogger.d("IosAdminShortcut: settings hook never became available, tap dropped")
        }
    }
}
