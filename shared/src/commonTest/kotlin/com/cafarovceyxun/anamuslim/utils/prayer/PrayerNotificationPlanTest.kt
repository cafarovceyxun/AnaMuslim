package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Bildiriş planı — saf qat, ona görə burada nə DataStore, nə resurs var.
 *
 * `nowMillis` **məcburi parametrdir** (`DailyContentSchedule.firstFreeSlot` ilə eyni konvensiya),
 * ona görə testlər sistemin saatından asılı deyil.
 */
class PrayerNotificationPlanTest {

    /** 2026-09-01, 00:00 UTC — sabit sətirdən çıxarılır ki, əl ilə yazılmış rəqəm sürüşməsin. */
    private val now = IsoDate.toEpochDay("2026-09-01")!! * 86_400_000L

    private val fivePrayers = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

    @Test
    fun upcomingReturnsOnlyFutureTimes() {
        val refs = PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 35)

        assertTrue(refs.isNotEmpty())
        assertTrue(refs.all { it.atMillis > now }, "keçmiş vaxt planlaşdırılmamalıdır")
        assertEquals(refs.map { it.atMillis }.sorted(), refs.map { it.atMillis }, "sıralı olmalıdır")
    }

    @Test
    fun respectsPerPrayerToggles() {
        val refs = PrayerNotificationPlan.upcoming(
            Fx.settings(notify = setOf(Prayer.FAJR, Prayer.MAGHRIB)),
            now,
            limit = 20,
        )

        assertTrue(refs.all { it.prayer == Prayer.FAJR || it.prayer == Prayer.MAGHRIB }, "${refs.map { it.prayer }}")
        assertTrue(refs.any { it.prayer == Prayer.FAJR })
        assertTrue(refs.any { it.prayer == Prayer.MAGHRIB })
    }

    @Test
    fun honoursBudgetLimit() {
        val refs = PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 7)

        assertEquals(7, refs.size)
    }

    @Test
    fun plansMoreDaysWhenFewerPrayersAreEnabled() {
        val allFive = PrayerNotificationPlan.upcoming(Fx.settings(notify = fivePrayers), now, limit = 35)
        val onlyTwo = PrayerNotificationPlan.upcoming(
            Fx.settings(notify = setOf(Prayer.FAJR, Prayer.MAGHRIB)),
            now,
            limit = 35,
        )

        val allFiveDays = allFive.map { it.dateIso }.distinct().size
        val onlyTwoDays = onlyTwo.map { it.dateIso }.distinct().size

        assertTrue(onlyTwoDays > allFiveDays, "az vaxt = eyni büdcə ilə daha çox gün: $onlyTwoDays vs $allFiveDays")
    }

    @Test
    fun horizonNeverExceedsTheCap() {
        val refs = PrayerNotificationPlan.upcoming(
            Fx.settings(notify = setOf(Prayer.FAJR)),
            now,
            limit = 200,
        )

        val days = refs.map { it.dateIso }.distinct().size
        assertTrue(days <= PrayerNotificationPlan.MAX_DAYS_AHEAD + 1, "üfüq həddi aşdı: $days")
    }

    @Test
    fun skipsDeliveredKeys() {
        val all = PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 35)
        val first = all.first()

        val filtered = PrayerNotificationPlan.upcoming(
            Fx.settings(),
            now,
            limit = 35,
            delivered = setOf(first.key),
        )

        assertTrue(filtered.none { it.key == first.key }, "çatdırılmış açar təkrar planlaşdırılmır")
        // Ölçü dəyişmir: üfüq limitdən uzun olduğu üçün düşən elementin yerini növbəti doldurur.
        assertEquals(all.size, filtered.size, "büdcə tam doldurulmalıdır")
        assertTrue(filtered.last().atMillis > all.last().atMillis, "üfüq bir addım irəli sürüşür")
    }

    @Test
    fun keyIsStableAndCarriesDateAndPrayer() {
        val ref = PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 5).first()

        assertEquals("${ref.dateIso}#${ref.prayer.name}", ref.key)
        assertEquals(ref.key, PrayerNotificationPlan.keyOf(ref.dateIso, ref.prayer))
    }

    @Test
    fun disabledOrIncompleteSettingsProduceNothing() {
        assertTrue(PrayerNotificationPlan.upcoming(Fx.settings(enabled = false), now, 35).isEmpty())
        assertTrue(PrayerNotificationPlan.upcoming(Fx.settings(notify = emptySet()), now, 35).isEmpty())
        assertTrue(
            PrayerNotificationPlan.upcoming(
                PrayerSettings(enabled = true, point = null, notify = fivePrayers),
                now,
                35,
            ).isEmpty(),
            "yer təyin edilməyibsə plan yoxdur",
        )
        assertTrue(PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 0).isEmpty())
    }

    @Test
    fun dueReturnsOnlyTimesInsideTheGraceWindow() {
        val upcoming = PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 10)
        val target = upcoming.first()

        // Hədəf vaxtdan bir dəqiqə sonraya baxırıq: o, «qaçırılmış»dır.
        val justAfter = target.atMillis + Fx.ONE_MINUTE
        val due = PrayerNotificationPlan.due(Fx.settings(), justAfter, graceMillis = 6 * 3_600_000L)

        assertTrue(due.any { it.key == target.key }, "qaçırılmış vaxt qayıtmalıdır")
        assertTrue(due.all { it.atMillis <= justAfter }, "gələcək vaxt `due` deyil")
    }

    @Test
    fun dueIgnoresTimesOlderThanTheGraceWindow() {
        val upcoming = PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 10)
        val target = upcoming.first()

        val longAfter = target.atMillis + 7 * 3_600_000L
        val due = PrayerNotificationPlan.due(Fx.settings(), longAfter, graceMillis = 6 * 3_600_000L)

        assertTrue(due.none { it.key == target.key }, "pəncərədən köhnə vaxt susdurulur")
    }

    @Test
    fun dueRespectsDeliveredAndZeroGrace() {
        val target = PrayerNotificationPlan.upcoming(Fx.settings(), now, limit = 10).first()
        val justAfter = target.atMillis + Fx.ONE_MINUTE

        assertTrue(
            PrayerNotificationPlan.due(Fx.settings(), justAfter, 6 * 3_600_000L, setOf(target.key))
                .none { it.key == target.key },
        )
        assertTrue(PrayerNotificationPlan.due(Fx.settings(), justAfter, graceMillis = 0L).isEmpty())
    }

    @Test
    fun pruneDeliveredDropsOldDatesAndKeepsRecentOnes() {
        val delivered = setOf(
            "2026-08-01#FAJR",
            "2026-08-28#ISHA",
            "2026-08-31#MAGHRIB",
            "2026-09-01#DHUHR",
        )

        val pruned = PrayerNotificationPlan.pruneDelivered(delivered, now, keepDays = 3)

        assertTrue("2026-09-01#DHUHR" in pruned)
        assertTrue("2026-08-31#MAGHRIB" in pruned)
        assertTrue("2026-08-01#FAJR" !in pruned, "bir aylıq açar atılmalıdır")
        assertTrue("2026-08-28#ISHA" !in pruned)
    }

    @Test
    fun highLatitudeSettingsStillProduceAPlan() {
        val refs = PrayerNotificationPlan.upcoming(Fx.settings(at = Fx.MURMANSK), now, limit = 35)

        assertTrue(refs.isNotEmpty(), "qütb enliyində də bildiriş planlaşdırılır")
        assertNotNull(refs.firstOrNull { it.prayer == Prayer.DHUHR })
    }
}
