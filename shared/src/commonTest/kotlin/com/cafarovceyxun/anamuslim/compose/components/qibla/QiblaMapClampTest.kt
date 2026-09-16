package com.cafarovceyxun.anamuslim.compose.components.qibla

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Xəritənin gəzmə həddi.
 *
 * ⚠️ Bu test bir **regressiyanı** bağlayır. Sıxma düsturu əvvəl də düzgün idi, amma mərkəz
 * sancağın özündən hesablanırdı — sancaq hər jestdə dəyişdiyi üçün mərkəz onunla sürüşürdü və
 * hədd hər jestdə sıfırlanırdı. Nəticədə cihazda sancaq Ciddədən Məkkəyə, 71 km sürüşdü.
 * Kompilyator da, digər testlər də susurdu.
 *
 * Ona görə burada yoxlanılan şey tək bir sıxma deyil, **təkrar hərəkətlərin cəmidir**.
 */
class QiblaMapClampTest {

    private fun distance(x: Double, y: Double, cx: Double, cy: Double): Double {
        val dx = x - cx
        val dy = y - cy

        return sqrt(dx * dx + dy * dy)
    }

    @Test
    fun pointInsideTheCircleIsLeftAlone() {
        val (x, y) = clampToCircle(x = 105.0, y = 100.0, centreX = 100.0, centreY = 100.0, radius = 50.0)

        assertTrue(x == 105.0 && y == 100.0, "alındı $x, $y")
    }

    @Test
    fun pointOutsideIsPulledOntoTheEdge() {
        val (x, y) = clampToCircle(x = 400.0, y = 100.0, centreX = 100.0, centreY = 100.0, radius = 50.0)

        assertTrue(distance(x, y, 100.0, 100.0) - 50.0 < 1e-9, "alındı $x, $y")
    }

    @Test
    fun repeatedMovesNeverEscapeTheFixedCentre() {
        // Səhvin özü budur: mərkəz sabit qaldıqca yüz jest də həddi aşa bilməz.
        val centreX = 1000.0
        val centreY = 1000.0
        val radius = 120.0

        var x = centreX
        var y = centreY

        repeat(100) {
            // Hər dəfə eyni istiqamətdə böyük bir sürüşdürmə.
            val moved = clampToCircle(x + 400.0, y + 250.0, centreX, centreY, radius)
            x = moved.first
            y = moved.second

            assertTrue(
                distance(x, y, centreX, centreY) <= radius + 1e-9,
                "addım $it: $x, $y — radius aşıldı",
            )
        }
    }

    @Test
    fun movingTheCentreWithThePointIsWhatBrokeIt() {
        // Köhnə davranışın sənədləşdirilmiş nümayişi: mərkəz nöqtə ilə birlikdə sürüşəndə hədd
        // heç nə etmir və nöqtə istənilən qədər uzağa gedir.
        var centreX = 0.0
        var centreY = 0.0
        var x = 0.0
        var y = 0.0

        repeat(50) {
            val moved = clampToCircle(x + 400.0, y, centreX, centreY, radius = 120.0)
            x = moved.first
            y = moved.second
            // ⚠️ məhz bu sətir səhv idi
            centreX = x
            centreY = y
        }

        assertTrue(x > 5_000.0, "köhnə davranış artıq təkrarlanmır: $x")
    }

    @Test
    fun zeroDistanceDoesNotDivideByZero() {
        val (x, y) = clampToCircle(x = 50.0, y = 50.0, centreX = 50.0, centreY = 50.0, radius = 10.0)

        assertTrue(!x.isNaN() && !y.isNaN(), "NaN gəldi: $x, $y")
    }
}
