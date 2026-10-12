package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Stable

/**
 * Runtime dialoqu olmayan, yalnız **sistem ayarlarından** verilən icazənin vəziyyəti
 * (dəqiq siqnal, pil optimizasyonu).
 *
 * UI çəkmir — yalnız vəziyyət və «ayarı aç» əməliyyatı verir. Görünüş paylaşılan ekranın işidir
 * ([com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerPermissionBanner]): əvvəllər hər
 * seam öz `Text` sətrini çəkirdi və o sətir xəbərdarlıq kartının üslubundan kənar, düyməsiz, adi
 * mətn kimi görünürdü — istifadəçi onu icazə istəyi kimi tanımırdı.
 */
@Stable
interface SystemSettingAccess {
    /** İstifadəçi ayarı verib-verməyib; ayarlardan qayıdanda (`ON_RESUME`) özü yenilənir. */
    val isGranted: Boolean

    /** Müvafiq sistem ekranını açır. */
    fun open()
}
