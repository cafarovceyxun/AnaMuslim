package com.cafarovceyxun.anamuslim.utils.salah

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Namaz bələdçisinin addımları əl ilə yazılıb, hədis çıxarışları isə bazada mövzu **açarı** ilə saxlanır
 * (`salah_evidence.topic`). Açar səhv yazılsa, bazanın CHECK formasına uymasa və ya heç bir ekranda
 * işlənməsə, çıxarış səssizcə görünmür — nə kompilyator, nə ekran bunu göstərir.
 */
class SalahGuideContentTest {

    @Test
    fun topicKeysAreUniqueAndMatchTheDatabaseCheck() {
        val keys = SalahTopic.entries.map { it.key }
        assertEquals(keys.size, keys.distinct().size, "Təkrar mövzu açarı")
        // `salah_evidence_topic_shape`: ^[a-z][a-z0-9_]{0,39}$
        val shape = Regex("^[a-z][a-z0-9_]{0,39}$")
        keys.forEach { assertTrue(shape.matches(it), "Açar bazanın CHECK-inə uymur: $it") }
        keys.forEach { assertEquals(it, SalahTopic.of(it)?.key) }
    }

    /** Admin seçim siyahısında olub heç bir ekranda görünməyən mövzu — əlavə edilən hədis itər. */
    @Test
    fun everyTopicIsShownSomewhere() {
        val orphans = SalahTopic.entries.toSet() - SalahGuideContent.usedTopics
        assertTrue(orphans.isEmpty(), "Ekranda göstərilməyən mövzular: $orphans")
    }

    @Test
    fun bothWuduFormsAreComplete() {
        // «Bir dəfə» forması üzdən başlayır (№ 114) — istifadəçinin istəyi, 2026-10-04.
        val once = SalahGuideContent.wuduOnce
        assertEquals(SalahTopic.WUDU1_MOUTH, once.first().topic)
        assertTrue(once.all { it.times == 1 }, "Bir dəfə formasında hər üzv bir dəfə yuyulur")
        assertEquals(7, once.size)

        val full = SalahGuideContent.wuduFull
        assertEquals(listOf(2, 3, 3, 2, 1, null), full.map { it.times }, "№ 112-dəki saylar")
        assertTrue((once + full).all { it.picture != null }, "Dəstəmaz addımının şəkli yoxdur")
    }

    @Test
    fun stepTopicsAreNotReusedWithinAList() {
        listOf(
            SalahGuideContent.wuduOnce,
            SalahGuideContent.wuduFull,
            SalahGuideContent.ghuslSteps,
            SalahGuideContent.tayammumSteps,
        ).forEach { steps ->
            val topics = steps.map { it.topic }
            assertEquals(topics.size, topics.distinct().size, "Eyni siyahıda təkrar mövzu: $topics")
        }
    }

    // ---- Mərhələ 2: Əzan və namaz ----

    @Test
    fun prayerStepsFollowTheUsersScheme() {
        val steps = SalahPrayerContent.steps
        assertEquals(16, steps.size, "Salavat çıxarıldı (dəlil yoxdur), iki səcdə arası duasız — 16 addım")
        assertEquals(steps.size, steps.map { it.topic }.distinct().size, "Təkrar addım mövzusu")
        assertEquals(SalahTopic.PRAYER_NIYYAH, steps.first().topic)
        val last = steps.last()
        assertEquals(SalahTopic.PRAYER_SALAM, last.topic)
        assertTrue(last.pair && last.showsSalamWords, "Salam: sağa/sola və istifadəçinin verdiyi sözlər")
        // Rükudan sonra əllər yenə sinədə (istifadəçi, № 330 + № 194).
        val standing = steps.first { it.topic == SalahTopic.PRAYER_STANDING }
        assertEquals(PrayerPose.QIYAM, standing.pose)
    }

    @Test
    fun rakatCountsMatchTheBook() {
        // Sübh 2 (№ 397), Zöhr 4 (№ 472), Əsr və İşa 4 (№ 467), Məğrib 3 (2-ci cild № 872).
        assertEquals(listOf(2, 4, 4, 3, 4), SalahPrayerContent.prayers.map { it.rakats })
        val maghrib = SalahPrayerContent.prayers.first { it.rakats == 3 }
        assertTrue(RakatPart.FIRST_TASHAHHUD in maghrib.parts(2))
        assertEquals(RakatPart.FATIHA_ONLY, maghrib.parts(3).first())
        assertTrue(RakatPart.SALAM in maghrib.parts(3))
        val fajr = SalahPrayerContent.prayers.first()
        assertTrue(RakatPart.FIRST_TASHAHHUD !in fajr.parts(2), "İki rükətlikdə ilk təşəhhüd olmur, son təşəhhüd olur")
        assertTrue(RakatPart.LAST_TASHAHHUD in fajr.parts(2))
    }

    @Test
    fun adhanTableIsTheUsersTable() {
        val adhan = SalahPrayerContent.adhan
        assertEquals(8, adhan.size)
        // № 307: əzan cüt, iqamə tək — «Qad qamətis-saləh» yalnız iqamədə, iki dəfə.
        assertEquals(listOf(2, 2, 2, 2, 2, 0, 2, 2), adhan.map { it.adhan })
        assertEquals(listOf(1, 1, 1, 1, 1, 2, 1, 1), adhan.map { it.iqama })
    }
}
