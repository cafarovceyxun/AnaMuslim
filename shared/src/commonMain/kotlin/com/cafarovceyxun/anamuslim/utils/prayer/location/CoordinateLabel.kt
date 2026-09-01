package com.cafarovceyxun.anamuslim.utils.prayer.location

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.abs
import kotlin.math.round

/**
 * Koordinatın etiket kimi göstərilməsi — `reverseGeocode` ad qaytarmayanda işlədilir.
 *
 * İki onluq rəqəm ≈ 1.1 km: yer seçimini tanımaq üçün kifayətdir, daha uzun rəqəm isə sətri
 * oxunmaz edir. Saf funksiyadır ki, testlənə bilsin — əvvəl ViewModel-in içində `private` idi.
 */
object CoordinateLabel {

    fun of(point: GeoPoint): String = "${format(point.latitude)}, ${format(point.longitude)}"

    /**
     * ⚠️ `toString()`-in kəsilməsi ilə edilmir: Kotlin `-2.673` üçün `"-2.673"`, `40.0` üçün isə
     * `"40.0"` verir — sətir uzunluğu sabit olmur. Burada tam və kəsr hissə ayrıca qurulur, ona
     * görə mənfi sıfır (`-0.004` → `-0.00`) da düzgün çıxır.
     */
    internal fun format(value: Double): String {
        val scaled = round(value * 100.0).toLong()
        val sign = if (scaled < 0L) "-" else ""
        val magnitude = abs(scaled)

        return "$sign${magnitude / 100}.${(magnitude % 100).toString().padStart(2, '0')}"
    }
}
