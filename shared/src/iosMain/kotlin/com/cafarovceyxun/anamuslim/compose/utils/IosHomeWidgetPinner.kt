package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Ana ekran vidcetlərinin iOS tərəfi.
 *
 * iOS-da vidcet **tətbiqin içindən yerləşdirilə bilmir** — istifadəçi onu sistemin qalereyasından
 * əlavə edir, `requestPinAppWidget` qarşılığı yoxdur. Ona görə [offerableWidgets] həmişə boşdur və
 * Ayarlarda «ana ekrana əlavə et» sətirləri çıxmır.
 *
 * Seam yenə də qeydiyyatdan keçir, çünki iki şey ondan asılıdır: fon şəffaflığı sürüşdürücüsünün
 * **görünməsi** (`HomeWidgetPinProvider.isAvailable`) və dəyər dəyişəndə yerləşdirilmiş vidcetlərin
 * **yenidən çəkilməsi** ([refreshPlacedWidgets]).
 */
object IosHomeWidgetPinner : HomeWidgetPinner {
    override suspend fun offerableWidgets(): List<HomeWidgetKind> = emptyList()

    override fun requestPin(kind: HomeWidgetKind) = Unit

    override fun refreshPlacedWidgets() = IosPrayerWidgetBridge.refresh()
}
