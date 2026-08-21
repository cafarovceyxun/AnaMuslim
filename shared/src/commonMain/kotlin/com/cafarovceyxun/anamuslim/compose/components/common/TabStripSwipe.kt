package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Üfüqi sürüşdürməni qonşu tab-a keçidə çevirir — aşağı barın jestinin **kompakt zolaqlar** üçün
 * variantı (Quran və hədis oxuma ekranlarındakı rejim tabları).
 *
 * Barda astana bir tab enidir, çünki orada pilyul barmağı 1:1 izləyir; bu zolaqlarda seçilmiş tab
 * etiketi ilə birlikdə genişləndiyi üçün tabların eni bərabər deyil, ona görə astana sabit [STEP]-dir.
 * Uzun bir sürüşmə bir neçə tabı keçə bilir: hər astana bir addım sayılır.
 *
 * Modifikator zolağın **özünə** verilməlidir — daxilindəki tablar `clickable` olduğu üçün `down`-u
 * udurlar, ona görə `awaitFirstDown(requireUnconsumed = false)`; slop keçiləndən sonra hərəkəti biz
 * uduruq, beləcə sürüşdürmə eyni zamanda tap kimi işləmir.
 */
@Composable
fun tabStripSwipeModifier(
    tabCount: Int,
    selectedIndex: Int,
    onSelect: (index: Int) -> Unit,
): Modifier {
    // Keyed on the tab count alone: keying on the selection would restart the handler at the first
    // switch and cancel the very swipe that caused it.
    val currentIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val layoutDirection = LocalLayoutDirection.current

    return Modifier.pointerInput(tabCount, layoutDirection) {
        if (tabCount <= 1) return@pointerInput
        val step = STEP.toPx()
        // In an Arabic (RTL) layout the strip is mirrored, so the next tab is to the left.
        val towardsNextTab = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var travel = 0f
            var index = currentIndex

            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                change.consume()
                travel = overSlop * towardsNextTab
            } ?: return@awaitEachGesture

            horizontalDrag(drag.id) { change ->
                travel += change.positionChange().x * towardsNextTab
                change.consume()

                while (travel >= step && index < tabCount - 1) {
                    index++
                    travel -= step
                    currentOnSelect(index)
                }
                while (travel <= -step && index > 0) {
                    index--
                    travel += step
                    currentOnSelect(index)
                }
                // Nothing further that way: stop the count from running up, so swiping back
                // responds on the first step instead of unwinding a debt.
                if (index == tabCount - 1) travel = travel.coerceAtMost(step)
                if (index == 0) travel = travel.coerceAtLeast(-step)
            }
        }
    }
}

/** Finger travel that counts as one tab. Shorter than a segment: the whole strip is ~200dp wide. */
private val STEP = 48.dp
