package com.cafarovceyxun.anamuslim.utils.notify

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Büdcənin aşılması **heç bir yerdə görünmür** — iOS ən yaxın 64 tələbi saxlayıb qalanını səssizcə
 * atır. Ona görə bu test qapı rolunu oynayır: kimsə payı qaldırmaq istəyəndə əvvəl burada dayanır.
 */
class NotificationBudgetTest {

    @Test
    fun budgetsFitTheIosPendingLimit() {
        val total = NotificationBudget.PRAYER + NotificationBudget.VOTD + NotificationBudget.RESERVE

        assertTrue(
            total <= NotificationBudget.IOS_PENDING_LIMIT,
            "büdcə cəmi $total, iOS limiti ${NotificationBudget.IOS_PENDING_LIMIT}",
        )
    }

    @Test
    fun everyShareLeavesRoomForAtLeastAFewDays() {
        // Namaz: gündə 5 vaxt → ən azı bir həftə. Günün ayəsi: gündə 5 yuva → ən azı üç gün.
        assertTrue(NotificationBudget.PRAYER >= 35, "namaz payı bir həftədən az olmamalıdır")
        assertTrue(NotificationBudget.VOTD >= 15, "günün ayəsi payı üç gündən az olmamalıdır")
        assertTrue(NotificationBudget.RESERVE > 0, "ehtiyat sıfır ola bilməz")
    }
}
