package com.cafarovceyxun.anamuslim.utils.prayer.location

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Koordinat etiketi — `reverseGeocode` ad qaytarmayanda (şəbəkə yox, xidmət əlçatmaz) yeganə
 * göstəriləndir, ona görə hər halda oxunaqlı olmalıdır.
 */
class CoordinateLabelTest {

    @Test
    fun formatsTwoDecimalsWithPadding() {
        assertEquals("40.58", CoordinateLabel.format(40.5766))
        // Sıfır doldurulması: `40.5` sadəcə «40.5» yox, «40.50» olmalıdır.
        assertEquals("40.50", CoordinateLabel.format(40.5))
        assertEquals("40.00", CoordinateLabel.format(40.0))
    }

    @Test
    fun keepsTheSignOnNegativeCoordinates() {
        assertEquals("-2.67", CoordinateLabel.format(-2.673))
        assertEquals("-74.01", CoordinateLabel.format(-74.006))
        // ⚠️ Kiçik mənfi dəyər: tam hissə sıfırdır, işarə isə itməməlidir.
        assertEquals("-0.13", CoordinateLabel.format(-0.126))
    }

    @Test
    fun buildsTheFullLabel() {
        assertEquals("40.58, 45.81", CoordinateLabel.of(GeoPoint(40.5766, 45.8121)))
        assertEquals("51.51, -0.13", CoordinateLabel.of(GeoPoint(51.5074, -0.1278)))
    }
}
