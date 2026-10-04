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
        assertTrue((once + full).all { it.drawings.isNotEmpty() }, "Dəstəmaz addımının kadrı yoxdur")
        // Sağ/sol nişanı olan addımın kadrı həmin tərəfdir.
        once.filter { it.side == Side.RIGHT }.forEach { assertTrue(it.drawings.single().name.endsWith("_RIGHT"), it.title) }
        once.filter { it.side == Side.LEFT }.forEach { assertTrue(it.drawings.single().name.endsWith("_LEFT"), it.title) }
    }

    @Test
    fun tayammumStepsHaveTheirOwnDrawings() {
        val drawings = SalahGuideContent.tayammumSteps.map { it.drawings.single() }
        assertTrue(drawings.all { it.name.startsWith("TAYAMMUM_") }, "Təyəmmüm addımına başqa bölmənin kadrı düşüb")
        assertEquals(drawings.size, drawings.toSet().size, "Təyəmmüm kadrı təkrarlanır")
    }

    @Test
    fun ghuslDrawingsStayInGhusl() {
        val drawings = SalahGuideContent.ghuslSteps.flatMap { it.drawings }
        assertTrue(drawings.all { it.name.startsWith("GHUSL_") }, "Qüsl addımına başqa bölmənin kadrı düşüb")
        assertEquals(drawings.size, drawings.toSet().size, "Qüsl kadrı təkrarlanır")
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

    // ---- Mərhələ 3: Camaat və xüsusi namazlar ----

    @Test
    fun travelRakatsMatchTheBook() {
        // Səfərdə dördlüklər iki (№ 467), Məğrib üç və İşa iki (№ 478), Sübh dəyişmir.
        val r = SalahGroupContent.travelRakats
        assertEquals(listOf(2, 4, 4, 3, 4), r.map { it.home }, "Evdəki say Mərhələ 2 ilə eyni olmalıdır")
        assertEquals(SalahPrayerContent.prayers.map { it.rakats }, r.map { it.home })
        assertEquals(listOf(2, 2, 2, 3, 2), r.map { it.travel })
    }

    @Test
    fun khawfFormsAreTheBooksFive() {
        val forms = SalahGroupContent.khawfForms
        assertEquals(5, forms.size, "№ 505–510: beş forma")
        assertEquals(forms.size, forms.map { it.topic }.distinct().size)
        forms.forEach { f ->
            assertEquals(3, f.lanes.size)
            assertTrue(f.rows.all { it.size == f.lanes.size }, "${f.title}: hər mərhələdə hər sütun üçün xana olmalıdır")
        }
    }

    @Test
    fun kusufHasTwoRukusInEachRakat() {
        // № 500: «dörd rüku və dörd səcdəni tamamladı».
        val rakats = SalahGroupContent.kusufRakats
        assertEquals(2, rakats.size)
        rakats.forEach { parts -> assertEquals(2, parts.count { it.label.startsWith("rüku") }) }
    }

    @Test
    fun seatDiagramsFitTheirArea() {
        SalahGroupContent.seats.forEach { layout ->
            layout.marks.forEach { m ->
                assertTrue(m.x in 15..265 && m.y in 15..(layout.height - 13), "${layout.title}: dairə sahədən çıxır (${m.x}, ${m.y})")
            }
            assertEquals(1, layout.marks.count { it.kind == SeatKind.IMAM }, "${layout.title}: bir imam")
        }
    }

    // ---- Mərhələ 4: Nafilə və cənazə ----

    @Test
    fun nightFormsAddUpToTheBooksCount() {
        // № 554: on birdən artıq deyil; № 555, 557–558, 561–562: on üç (kimisi sünnətlə birlikdə sayır).
        val forms = SalahNaflContent.nightForms
        assertEquals(listOf(11, 11, 13, 13, 13), forms.map { it.total })
        assertEquals(forms.size, forms.map { it.topic }.distinct().size)
        forms.forEach { f ->
            assertEquals(f.total, f.blocks.filter { it.counted && it.kind != NightBlockKind.SLEEP }.sumOf { it.rakats }, "${f.title}: bloklar saya uyğun deyil")
            assertEquals(1, f.blocks.count { it.kind == NightBlockKind.WITR }, "${f.title}: bir vitr")
        }
    }

    @Test
    fun sunnahTableUsesThePrayerCounts() {
        val map = SalahNaflContent.sunnahMap
        assertEquals(SalahPrayerContent.prayers.map { "${it.rakats}" }, map.take(5).map { it.fard.main })
        // № 516: Sübhdən və Əsrdən sonra qadağa.
        assertEquals(listOf("Sübh", "Əsr"), map.filter { it.after.kind == SunnahKind.FORBIDDEN }.map { it.name })
    }

    @Test
    fun sahwOrderFollowsEachHadith() {
        val ways = SalahNaflContent.sahwCases.flatMap { it.ways }
        assertEquals(ways.size, ways.map { it.topic }.distinct().size)
        fun order(topic: SalahTopic) = ways.first { it.topic == topic }.parts.filter { it.kind != SahwPartKind.STEP }.map { it.kind }
        // № 572 və № 575: səcdə salamdan əvvəl; № 573: salamdan sonra.
        assertEquals(listOf(SahwPartKind.SAJDA, SahwPartKind.SALAM), order(SalahTopic.SAHW_YAQIN))
        assertEquals(listOf(SahwPartKind.SALAM, SahwPartKind.SAJDA), order(SalahTopic.SAHW_TAHARRI))
        assertEquals(SahwPartKind.SALAM, order(SalahTopic.SAHW_TASHAHHUD).last())
    }
}
