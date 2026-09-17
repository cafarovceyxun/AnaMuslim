package com.cafarovceyxun.anamuslim.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.components.common.FullScreenSurface
import com.cafarovceyxun.anamuslim.compose.screens.dua.AsmaScreen
import com.cafarovceyxun.anamuslim.compose.screens.dua.DuaScreen
import com.cafarovceyxun.anamuslim.utils.link.DuaDeepLinkRouter
import com.cafarovceyxun.anamuslim.utils.link.DuaDeepLinkTarget

/**
 * Dərin linklə gələn duanı/adı açır.
 *
 * **Tətbiqin kökünə** qoyulur (Android `MainActivity`, iOS `MainViewController`), ekranın içinə
 * yox: link istənilən anda gələ bilər və istifadəçi həmin an başqa tabda ola bilər. Kökdə
 * duranda ekran hər halda açılır; ana ekranın bölməsinə qoysaydıq, link yalnız istifadəçi «Əsas»
 * tabına qayıdanda işləyərdi.
 *
 * `AppDestination`-da route **açılmadı**: ekran onsuz da tam-ekran səthdə (`FullScreenSurface`)
 * yaşayır və ana ekrandan da belə açılır, yəni route əlavə etmək eyni ekranı iki yolla göstərmək
 * demək olardı.
 *
 * Hədəf açılan kimi istehlak olunur ([DuaDeepLinkRouter.consume]) — yoxsa səth bağlananda gözləyən
 * hədəf yenidən oxunar və ekran sonsuz açılardı.
 */
@Composable
fun DuaDeepLinkHost() {
    val target by DuaDeepLinkRouter.pending.collectAsStateWithLifecycle()

    when (val pending = target) {
        null -> Unit

        is DuaDeepLinkTarget.Dua -> FullScreenSurface(onDismiss = { DuaDeepLinkRouter.consume() }) {
            DuaScreen(
                onBack = { DuaDeepLinkRouter.consume() },
                initialDuaId = pending.duaId,
            )
        }

        is DuaDeepLinkTarget.Asma -> FullScreenSurface(onDismiss = { DuaDeepLinkRouter.consume() }) {
            AsmaScreen(
                onBack = { DuaDeepLinkRouter.consume() },
                initialNameNo = pending.nameNo,
            )
        }
    }
}
