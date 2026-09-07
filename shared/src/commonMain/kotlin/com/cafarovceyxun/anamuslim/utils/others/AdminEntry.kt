package com.cafarovceyxun.anamuslim.utils.others

import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth

/**
 * İdarəetmə panelinin gizli giriş nöqtəsi — alt bardakı **Əsas** düyməsini 5 saniyə basılı
 * saxlamaq.
 *
 * Jest qəsdən uzundur: qısa uzun-basma təsadüfən (məsələn cibdə, ya da sürüşdürmə cəhdində) baş
 * verə bilər, panel isə adi istifadəçiyə heç vaxt görünməməlidir.
 *
 * **Sessiya yoxdursa heç nə olmur** — nə panel açılır, nə də hər hansı əlamət verilir; jestin
 * mövcudluğu belə bilinmir. Giriş yolu Ayarlardakı kilid ikonudur (5 klik → `LoginSheet`).
 *
 * Naviqasiya mövcud [ReaderUiHooks.openSettingsRoute] seam-i ilə gedir: Android-də
 * `ActivitySettings`, iOS-da `SettingsDetail` açılır — yəni tab kökü push edilmir.
 */
object AdminEntry {

    /** Jest tamamlananda çağırılır. Panel açıldısa `true` — çağıran tərəf geri əlaqə verə bilər. */
    fun openIfAuthorized(): Boolean {
        if (SupabaseProvider.client.auth.currentSessionOrNull() == null) return false
        val open = ReaderUiHooks.openSettingsRoute ?: return false
        open(SettingRoutes.ADMIN_HUB)
        return true
    }
}
