package com.cafarovceyxun.anamuslim.utils.notify

/**
 * iOS-un **64 gözləyən bildiriş** limitinin bölgüsü — tək həqiqət mənbəyi.
 *
 * iOS-da tətbiq bağlıykən oyanıb bildiriş qura bilmir, ona görə gələcək bildirişlər **əvvəlcədən**
 * yazılır. Sistem limitə çatanda ən yaxın 64-ü saxlayıb qalanını **səssizcə atır** — yəni büdcəni
 * aşmaq nə kompilyasiyada, nə testdə, nə də logda görünür, sadəcə bildirişlər kəsilir.
 *
 * Rəqəmlər ayrı-ayrı fayllarda dağınıq qalsa gec-tez cəmi 64-ü keçir; buna görə hamısı buradadır və
 * `NotificationBudgetTest` cəmi yoxlayır.
 *
 * ⚠️ Android-də belə limit yoxdur — bu sabitlər orada da işlədilir ki, iki platformanın üfüqü
 * eyni olsun və «iPhone-da fərqli davranır» şikayəti yaranmasın.
 */
object NotificationBudget {

    /** Apple-ın sərt həddi: `UNUserNotificationCenter` bundan çoxunu saxlamır. */
    const val IOS_PENDING_LIMIT = 64

    /**
     * Namaz vaxtları: 7 gün × 5 vaxt.
     *
     * İstifadəçi az vaxt seçəndə eyni büdcə daha çox günə çatır
     * ([com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationPlan] üfüqü özü hesablayır).
     */
    const val PRAYER = 35

    /**
     * Günün ayəsi: 4 gün × 5 yuva. **Əvvəl 40 idi.**
     *
     * Azaldıldı, çünki namaz vaxtı gündəlik açılış səbəbidir və istifadəçi qərarı belədir:
     * tətbiqi dörd gün açmayan adam onsuz da günün ayəsini oxumur. Növbə keşdə 60 günə qədər durur,
     * hər açılışda və hər fon yenilənməsində yenidən yazılır — 4 gün geniş ehtiyatdır.
     */
    const val VOTD = 20

    /**
     * Boş buraxılan pay. Endirmə bildirişləri dərhal göstərilir (gözləyən növbəyə düşmür), amma
     * gələcək funksiyalar üçün yer qalmalıdır — limitə dirənmiş büdcə səssiz itki deməkdir.
     */
    const val RESERVE = 9

    init {
        require(PRAYER + VOTD + RESERVE <= IOS_PENDING_LIMIT) {
            "Bildiriş büdcəsi iOS limitini aşır: $PRAYER + $VOTD + $RESERVE > $IOS_PENDING_LIMIT"
        }
    }
}
