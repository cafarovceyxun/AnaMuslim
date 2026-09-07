package com.cafarovceyxun.anamuslim.utils.others

import com.cafarovceyxun.anamuslim.repository.supabase.AdminCountsRepository
import com.cafarovceyxun.anamuslim.repository.supabase.AdminPendingCounts
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * İdarəetmə panelini ana ekran ikonundan açan qısayolun platforma seam-i.
 *
 * Android tərəfdə `ShortcutUtils` dinamik launcher shortcut-u yaradır, iOS tərəfdə
 * `IosAdminShortcut` `UIApplicationShortcutItem` qoyur. Hər iki halda jest eynidir: ikona basıb
 * saxlamaq.
 */
object AdminShortcut {

    /** Qısayolu yaradır/yeniləyir. [subtitle] gözləyən işin sayını göstərir (ola bilməz — `null`). */
    var push: ((subtitle: String?) -> Unit)? = null

    /** Qısayolu silir — çıxışdan sonra ikonun altında admin yolu qalmamalıdır. */
    var remove: (() -> Unit)? = null
}

/**
 * Qısayolun ömrünü sessiyaya bağlayır: giriş varsa qoyulur, çıxışda silinir.
 *
 * Prosesin ömrü boyu işləyir (Android `QuranApp.onCreate`, iOS `IosBootstrap`), ekrandan asılı
 * deyil — sessiya bərpası asinxrondur və istifadəçi tətbiqi açmadan da çıxış edə bilər.
 *
 * Ayarlarda artıq «İdarəetmə» bölməsi olmadığına görə gözləyən işin sayını **qısayolun altındakı
 * sətirdə** göstəririk: panel açılmadan da görünsün. Say yalnız sessiya dəyişəndə oxunur — arxa fon
 * işi yaradılmır.
 */
object AdminShortcutSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            SupabaseProvider.client.auth.sessionStatus
                .map { it is SessionStatus.Authenticated }
                .distinctUntilChanged()
                .collect { signedIn ->
                    if (signedIn) publish() else AdminShortcut.remove?.invoke()
                }
        }
    }

    /**
     * Panel sayları təzələyəndə qısayolun alt sətrini də yeniləyir — ikinci sorğu atılmır, say
     * artıq oxunub. Sessiya yoxdursa qısayol onsuz da yoxdur, toxunmuruq.
     */
    fun publishCounts(counts: AdminPendingCounts) {
        if (SupabaseProvider.client.auth.currentSessionOrNull() == null) return
        AdminShortcut.push?.invoke(subtitleOf(counts))
    }

    private suspend fun publish() {
        val counts = try {
            AdminCountsRepository.fetch()
        } catch (e: Exception) {
            AppLogger.d("AdminShortcut", "Count fetch failed: ${e.message}")
            AdminPendingCounts()
        }
        AdminShortcut.push?.invoke(subtitleOf(counts))
    }

    private fun subtitleOf(counts: AdminPendingCounts): String? =
        if (counts.isEmpty) null else "${counts.total} gözləyən"
}
