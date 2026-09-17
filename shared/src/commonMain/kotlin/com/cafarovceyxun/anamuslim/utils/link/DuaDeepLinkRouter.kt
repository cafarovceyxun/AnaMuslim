package com.cafarovceyxun.anamuslim.utils.link

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gələn dərin linklərin **gözləmə yeri**.
 *
 * Platforma linki tətbiqin istənilən anında verə bilər — soyuq açılışda (Compose hələ qurulmayıb),
 * fondan qayıdanda, hətta başqa tabda gəzərkən. Ona görə link birbaşa naviqasiyaya deyil, buradakı
 * axına yazılır; UI hazır olanda oxuyur və **istehlak edir**. Eyni forma iOS-un sürətli
 * əməliyyatlarındakı gözləmə məntiqi ilə üst-üstə düşür.
 *
 * ⚠️ Hədəf **bir dəfə** işlənir ([consume]): əks halda ekran bağlananda gözləyən hədəf yenidən
 * oxunur və dua sonsuz açılırdı.
 */
object DuaDeepLinkRouter {

    private val _pending = MutableStateFlow<DuaDeepLinkTarget?>(null)

    /** Gözləyən hədəf; `null` = yoxdur. */
    val pending: StateFlow<DuaDeepLinkTarget?> = _pending.asStateFlow()

    /**
     * Linki qəbul edir. Qaytarır: bu link bizimdirmi.
     *
     * Android intent-i və iOS `onOpenURL`-i eyni bu funksiyanı çağırır, yəni yeni platforma
     * qoşulanda burada yazılacaq heç nə yoxdur.
     */
    fun open(url: String?): Boolean {
        val target = DuaDeepLink.parse(url) ?: return false
        _pending.value = target
        return true
    }

    /** Test və host üçün: hədəfi birbaşa qoyur. */
    fun open(target: DuaDeepLinkTarget) {
        _pending.value = target
    }

    /** Hədəf açıldı — növbəti linkə qədər gözləmə yeri boşdur. */
    fun consume() {
        _pending.value = null
    }
}
