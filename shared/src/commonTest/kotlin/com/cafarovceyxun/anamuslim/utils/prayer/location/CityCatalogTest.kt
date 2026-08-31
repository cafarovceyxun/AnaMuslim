package com.cafarovceyxun.anamuslim.utils.prayer.location

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Qatlama cədvəli `tools/prayer/gen_cities.py`-dan generasiya olunub; fayldakı adlar orada,
 * sorğu isə burada qatlanır. İki tərəf sürüşsə axtarış **səssizcə boş** qayıdır — bu testlər
 * həmin sürüşməni tutur.
 */
class CityCatalogTest {

    /** Sətirlər real `cities.tsv`-dən köçürülüb (sıra əhaliyə görə azalandır). */
    private val sample = """
        # Mənbə: GeoNames — CC BY 4.0.
        Istanbul${'\t'}istanbul${'\t'}TR${'\t'}41.014${'\t'}28.950${'\t'}39
        Moscow${'\t'}moscow|moskova|moskva${'\t'}RU${'\t'}55.752${'\t'}37.618${'\t'}155
        New York City${'\t'}new york city|nyu york${'\t'}US${'\t'}40.714${'\t'}-74.006${'\t'}10
        Bakı${'\t'}baki|baku${'\t'}AZ${'\t'}40.378${'\t'}49.892${'\t'}0
        Sumqayıt${'\t'}sumqayit|sumgayit|sumgait${'\t'}AZ${'\t'}40.590${'\t'}49.669${'\t'}0
        Gəncə${'\t'}gence|ganja${'\t'}AZ${'\t'}40.682${'\t'}46.361${'\t'}400
        Şəki${'\t'}seki|sheki${'\t'}AZ${'\t'}41.192${'\t'}47.171${'\t'}700
        Yorkton${'\t'}yorkton${'\t'}CA${'\t'}51.213${'\t'}-102.462${'\t'}498
        St. Louis${'\t'}st louis${'\t'}US${'\t'}38.627${'\t'}-90.198${'\t'}142
    """.trimIndent()

    private val catalog = CityCatalog.parse(sample)

    // region — qatlama

    @Test
    fun foldsAzerbaijaniLetters() {
        assertEquals("seki", CityCatalog.fold("Şəki"))
        assertEquals("gence", CityCatalog.fold("Gəncə"))
        assertEquals("baki", CityCatalog.fold("Bakı"))
        assertEquals("sumqayit", CityCatalog.fold("Sumqayıt"))
    }

    @Test
    fun turkishDottedCapitalIFolds() {
        // ⚠️ Kotlin-də "İstanbul".lowercase() `i` + birləşən nöqtə (İKİ kod nöqtəsi) verir,
        // ona görə `İ` cədvəldə açıq şəkildə köçürülür. Bu test həmin tələni qoruyur.
        assertEquals("istanbul", CityCatalog.fold("İstanbul"))
        assertEquals(1, CityCatalog.fold("İ").length, "bir kod nöqtəsi olmalıdır")
    }

    @Test
    fun foldsRussianCyrillic() {
        assertEquals("moskva", CityCatalog.fold("Москва"))
        assertEquals("baku", CityCatalog.fold("Баку"))
        assertEquals("zhukovskiy", CityCatalog.fold("Жуковский"))
    }

    @Test
    fun foldsCommonEuropeanDiacritics() {
        assertEquals("koln", CityCatalog.fold("Köln"))
        assertEquals("malmo", CityCatalog.fold("Malmö"))
        assertEquals("gdansk", CityCatalog.fold("Gdańsk"))
        assertEquals("zurich", CityCatalog.fold("Zürich"))
    }

    @Test
    fun foldCollapsesPunctuationAndSpacing() {
        assertEquals("new york", CityCatalog.fold("  New-York!  "))
        assertEquals("", CityCatalog.fold("   "))
    }

    @Test
    fun foldCollapsesInternalWhitespaceLikeTheGenerator() {
        // ⚠️ Bu sürüşmə REAL baş verdi: generator daxili boşluqları sıxmırdı, ona görə faylda
        // "st  louis" (qoşa boşluq) yazılırdı, sorğu isə "st louis"-ə qatlanırdı — uyğunluq
        // heç vaxt tapılmırdı. Ayrıca, durğu işarəli adlar ümumiyyətlə qatlanmır və altı şəhər
        // (St. Louis, St. Petersburg, Halle (Saale), …) səssizcə itirdi.
        assertEquals("st louis", CityCatalog.fold("St. Louis"))
        assertEquals("halle saale", CityCatalog.fold("Halle (Saale)"))
        assertEquals("jose c paz", CityCatalog.fold("José C. Paz"))
        assertEquals("gasteiz vitoria", CityCatalog.fold("Gasteiz / Vitoria"))
    }

    // endregion

    // region — parse

    @Test
    fun parseKeepsEveryValidRowAndSkipsTheRest() {
        assertEquals(9, catalog.size)

        val broken = CityCatalog.parse(
            """
            # şərh
            Yaxşı${'\t'}yaxsi${'\t'}AZ${'\t'}40.0${'\t'}49.0${'\t'}12
            PozuqHündürlük${'\t'}poz${'\t'}AZ${'\t'}40.0${'\t'}49.0${'\t'}filan
            AzSütun${'\t'}azsutun${'\t'}AZ${'\t'}40.0${'\t'}49.0
            PozuqEnlik${'\t'}pozuq2${'\t'}AZ${'\t'}filan${'\t'}49.0${'\t'}0
            DiapazondanKənar${'\t'}kenar${'\t'}AZ${'\t'}95.0${'\t'}49.0${'\t'}0
            QatlamaYoxdur${'\t'}${'\t'}AZ${'\t'}40.0${'\t'}49.0${'\t'}0
            """.trimIndent()
        )

        assertEquals(2, broken.size, "yalnız etibarlı sətirlər qalmalıdır")
        assertEquals(12.0, broken.search("yaxsi").single().point.elevationMeters)
        // Pozuq hündürlük sətri ATMIR — dəniz səviyyəsinə düşür: Axşamı bir neçə dəqiqə
        // erkənləşdirir, şəhəri tamamilə itirməkdən yaxşıdır.
        assertEquals(0.0, broken.search("poz").single().point.elevationMeters)
    }

    // endregion

    // region — axtarış

    @Test
    fun searchIsDiacriticInsensitiveInBothDirections() {
        assertEquals("Şəki", catalog.search("seki").first().name)
        assertEquals("Şəki", catalog.search("Şəki").first().name)
        assertEquals("Şəki", catalog.search("SHEKI").first().name)
        assertEquals("Gəncə", catalog.search("gence").first().name)
        assertEquals("Gəncə", catalog.search("Ganja").first().name)
    }

    @Test
    fun searchFindsCitiesWhoseNamesCarryPunctuation() {
        assertEquals("St. Louis", catalog.search("st louis").first().name)
        assertEquals("St. Louis", catalog.search("St. Louis").first().name)
        assertEquals("St. Louis", catalog.search("louis").first().name, "söz başlanğıcı")
    }

    @Test
    fun searchFindsTurkishCityByDottedOrPlainI() {
        assertEquals("Istanbul", catalog.search("İstanbul").first().name)
        assertEquals("Istanbul", catalog.search("istanbul").first().name)
    }

    @Test
    fun localisedAliasesMakeCyrillicAndTurkishQueriesWork() {
        // Fayl az/tr/ru adlarını da qatlanmış şəkildə daşıyır, ona görə ayrıca kiril sütunu yoxdur:
        // sorğu da, saxlanan alias da eyni latın qatlamasına düşür.
        assertEquals("Moscow", catalog.search("Москва").first().name)
        assertEquals("Moscow", catalog.search("Moskova").first().name, "türkcə")
        assertEquals("New York City", catalog.search("Nyu-York").first().name, "azərbaycanca")
    }

    @Test
    fun exactMatchOutranksPrefixMatch() {
        // "baku" Bakının tam qatlamasıdır; "bak" isə yalnız prefiksdir.
        assertEquals("Bakı", catalog.search("baku").first().name)
        assertEquals("Bakı", catalog.search("bak").first().name)
    }

    @Test
    fun prefixOutranksWordStartWhichOutranksContains() {
        val results = catalog.search("york").map { it.name }

        // "Yorkton" prefiksdir, "New York City" isə söz başlanğıcı — prefiks öndə gəlir.
        assertEquals(listOf("Yorkton", "New York City"), results)
    }

    @Test
    fun fileOrderMeansPopulationOrder() {
        // Sətirlər əhaliyə görə azalandır, ona görə ayrıca əhali sütunu yoxdur.
        val results = catalog.search("a").map { it.name }

        assertTrue(results.indexOf("Istanbul") < results.indexOf("Bakı"), "$results")
    }

    @Test
    fun searchRespectsTheLimitAndEmptyQuery() {
        assertEquals(2, catalog.search("a", limit = 2).size)
        assertTrue(catalog.search("").isEmpty())
        assertTrue(catalog.search("   ").isEmpty())
        assertTrue(catalog.search("baku", limit = 0).isEmpty())
        assertTrue(catalog.search("beləşəhəryoxdur").isEmpty())
    }

    // endregion

    // region — ən yaxın

    @Test
    fun elevationIsParsedForTheChosenCity() {
        assertEquals(700.0, catalog.search("seki").first().point.elevationMeters)
        assertEquals(0.0, catalog.search("baku").first().point.elevationMeters)
    }

    @Test
    fun nearestPicksTheClosestCity() {
        // Xırdalan (40.448, 49.755) — Bakıya ən yaxın.
        assertEquals("Bakı", catalog.nearest(GeoPoint(40.448, 49.755))?.name)
        // Zaqatala ətrafı — Şəkiyə yaxındır.
        assertEquals("Şəki", catalog.nearest(GeoPoint(41.63, 46.64))?.name)
    }

    @Test
    fun nearestHandlesNegativeLongitude() {
        assertEquals("New York City", catalog.nearest(GeoPoint(40.7, -73.9))?.name)
    }

    @Test
    fun nearestRejectsAnUnsetPoint() {
        assertNull(catalog.nearest(GeoPoint(0.0, 0.0)), "0,0 «təyin edilməyib» deməkdir")
        assertNull(catalog.nearest(GeoPoint(95.0, 10.0)))
        assertNull(CityCatalog.parse("").nearest(GeoPoint(40.0, 49.0)), "boş kataloq")
    }

    // endregion
}
