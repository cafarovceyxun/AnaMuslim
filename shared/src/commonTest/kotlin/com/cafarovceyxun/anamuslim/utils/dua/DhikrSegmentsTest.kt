package com.cafarovceyxun.anamuslim.utils.dua

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Hədisdəki zikrlərin tapılması — fixture **hədis 368-in özüdür** (Əhməd 803): bir hədisdə beş zikr,
 * qeyddə beş tərcümə və bir dırnaqlı kitab adı («Raf'ul-yədeyn»), yəni bütün tələlər bir yerdədir.
 *
 * Səhv cütləşmə ekranda heç nə sındırmır — sadəcə rüku zikrinin altına səcdə tərcüməsi yazılır və
 * bazaya elə də gedir. Ona görə say uyğunsuzluğunun hər yolu ayrıca yoxlanılır.
 */
class DhikrSegmentsTest {

    @Test
    fun hadith368_hasFiveSegmentsInEveryBlock() {
        assertEquals(5, braceRanges(ARABIC_368).size)
        assertEquals(5, braceRanges(NARRATION_368).size)
        // Altıncı dırnaq — «Raf'ul-yədeyn» — qəlibə uymur və atılır.
        assertEquals(5, noteTranslations(NOTE_368).size)

        val segments = dhikrSegments(ARABIC_368, NARRATION_368, NOTE_368)
        assertEquals(5, segments.size)
        segments.forEach { segment ->
            assertTrue(segment.transliteration != null, "oxunuş cütlənməyib: ${segment.arabic}")
            assertTrue(segment.translation != null, "tərcümə cütlənməyib: ${segment.arabic}")
        }
    }

    @Test
    fun hadith368_pairsBlocksByIndex() {
        val segments = dhikrSegments(ARABIC_368, NARRATION_368, NOTE_368)

        assertTrue(segments[0].arabic.startsWith("وَجَّهْتُ وَجْهِي"))
        assertTrue(segments[0].transliteration!!.startsWith("Vaccəhtu vachiyə"))
        assertTrue(segments[0].translation!!.startsWith("Hənif olaraq"))

        // Rüku zikri — ikinci blok hər üç mətndə.
        assertTrue(segments[1].arabic.startsWith("اللَّهُمَّ لَكَ رَكَعْتُ"))
        assertTrue(segments[1].transliteration!!.startsWith("Allahummə ləkə rakə′tu"))
        assertTrue(segments[1].translation!!.startsWith("Allahummə! Sənə rüku etdim"))

        assertTrue(segments[4].arabic.startsWith("اللَّهُمَّ اغْفِرْ لِي مَا قَدَّمْتُ"))
        assertTrue(segments[4].translation!!.endsWith("Yoxdur ilah Səndən başqa!"))
    }

    @Test
    fun hadith368_dropsBracesAndQuotes() {
        dhikrSegments(ARABIC_368, NARRATION_368, NOTE_368).forEach { segment ->
            listOfNotNull(segment.arabic, segment.transliteration, segment.translation)
                .forEach { text ->
                    assertFalse('{' in text || '}' in text, "mötərizə qalıb: $text")
                    assertFalse(text.startsWith("\"") || text.endsWith("\""), "dırnaq qalıb: $text")
                    assertEquals(text.trim(), text)
                }
        }
    }

    @Test
    fun countMismatch_leavesFieldNull() {
        // Rəvayətdə iki mötərizə, ərəbcədə üç — indeks uyğunluğu saxta olardı.
        val segments = dhikrSegments(
            arabic = "{أ} {ب} {ج}",
            narration = "{a} {b}",
            note = "Birinci: \"bir\" ikinci: \"iki\" üçüncü: \"üç\"",
        )

        assertEquals(3, segments.size)
        segments.forEach { assertNull(it.transliteration) }
        assertEquals(listOf("bir", "iki", "üç"), segments.map { it.translation })
    }

    @Test
    fun translationCountMismatch_leavesTranslationNull() {
        val segments = dhikrSegments(
            arabic = "{أ} {ب}",
            narration = "{a} {b}",
            note = "Tərcüməsi: \"yalnız biri\"",
        )

        assertEquals(listOf("a", "b"), segments.map { it.transliteration })
        segments.forEach { assertNull(it.translation) }
    }

    @Test
    fun noBraces_returnsEmpty() {
        assertTrue(dhikrSegments("بِسْمِ اللَّهِ", "{a}", "x: \"y\"").isEmpty())
        assertTrue(braceRanges("").isEmpty())
    }

    @Test
    fun unclosedBrace_stopsWithoutBreakingEarlierRanges() {
        val text = "{bir} və {iki} sonra {bağlanmayan"

        assertEquals(listOf(0..4, 9..13), braceRanges(text))
        assertEquals(listOf("bir", "iki"), dhikrSegments(text, text, null).map { it.arabic })
    }

    @Test
    fun noteTranslations_recognisesGuillemetsAndCurlyQuotes() {
        val note = "Birincinin tərcüməsi: «Allah böyükdür». İkincisi belədir: “Allaha həmd olsun”."

        assertEquals(listOf("Allah böyükdür", "Allaha həmd olsun"), noteTranslations(note))
    }

    @Test
    fun noteTranslations_skipsQuotesWithoutColon() {
        // Kitab adı keçilir, onun bağlanan dırnağı isə növbəti parçanın açılanı sayılmır.
        val note = "Buxari \"Raf'ul-yədeyn\"-də, tərcüməsi: \"həqiqi\""

        assertEquals(listOf("həqiqi"), noteTranslations(note))
        assertTrue(noteTranslations(null).isEmpty())
    }

    @Test
    fun nextBraceRange_cyclesAndSnapsToContainingBlock() {
        val ranges = listOf(0..4, 9..13, 20..25)

        assertEquals(0..4, nextBraceRange(ranges, null))
        assertEquals(9..13, nextBraceRange(ranges, 0..4))
        // Sonuncudan sonra birinciyə qayıdır.
        assertEquals(0..4, nextBraceRange(ranges, 20..25))
        // Mötərizənin içindən əl ilə seçilmiş parça — həmin mötərizə bütöv seçilir.
        assertEquals(9..13, nextBraceRange(ranges, 10..11))
        // Mötərizələr arasında — ondan sonrakı ilk mötərizə.
        assertEquals(20..25, nextBraceRange(ranges, 15..16))
        assertNull(nextBraceRange(emptyList(), 0..1))
    }

    private companion object {
        const val ARABIC_368 = """٣٦٨-حَدَّثَنَا هَاشِمُ بْنُ الْقَاسِمِ،حَدَّثَنَا عَبْدُ الْعَزِيزِ-يَعْنِي ابْنَ عَبْدِ اللَّهِ بْنِ أَبِي سَلَمَةَ- عَنْ عَمِّهِ الْمَاجِشُونِ بْنِ أَبِي سَلَمَةَ،عَنِ الْأَعْرَجِ،عَنْ عُبَيْدِ اللَّهِ بْنِ أَبِي رَافِعٍ،عَنْ عَلِيِّ بْنِ أَبِي طَالِبٍ رَضِيَ اللَّهُ عَنْهُ:
"أَنَّ النَّبِيَّ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ كَانَ إِذَا اسْتَفْتَحَ الصَّلَاةَ يُكَبِّرُ ثُمَّ يَقُولُ:{وَجَّهْتُ وَجْهِي لِلَّذِي فَطَرَ السَّمَاوَاتِ وَالْأَرْضَ حَنِيفًا وَمَا أَنَا مِنَ الْمُشْرِكِينَ،إِنَّ صَلَاتِي وَنُسُكِي وَمَحْيَايَ وَمَمَاتِي لِلَّهِ رَبِّ الْعَالَمِينَ،لَا شَرِيكَ لَهُ وَبِذَلِكَ أُمِرْتُ وَأَنَا أَوَّلُ الْمُسْلِمِينَ،اللَّهُمَّ أَنْتَ الْمَلِكُ لَا إِلَهَ إِلَّا أَنْتَ،أَنْتَ رَبِّي وَأَنَا عَبْدُكَ،ظَلَمْتُ نَفْسِي، وَاعْتَرَفْتُ بِذَنْبِي،فَاغْفِرْ لِي ذُنُوبِي جَمِيعًا، لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ،اهْدِنِي لِأَحْسَنِ الْأَخْلَاقِ،لَا يَهْدِي لِأَحْسَنِهَا إِلَّا أَنْتَ،اصْرِفْ عَنِّي سَيِّئَهَا لَا يَصْرِفُ عَنِّي سَيِّئَهَا إِلَّا أَنْتَ،لَبَّيْكَ وَسَعْدَيْكَ،وَالْخَيْرُ كُلُّهُ فِي يَدَيْكَ،وَالشَّرُّ لَيْسَ إِلَيْكَ،أَنَا بِكَ وَإِلَيْكَ، تَبَارَكْتَ وَتَعَالَيْتَ،أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ}.وَإِذَا رَكَعَ قَالَ:{اللَّهُمَّ لَكَ رَكَعْتُ، وَبِكَ آمَنْتُ،وَلَكَ أَسْلَمْتُ،خَشَعَ لَكَ سَمْعِي، وَبَصَرِي،وَمُخِّي وَعِظَامِي،وَعَصَبِي}.
وَإِذَا رَفَعَ رَأْسَهُ قَالَ:{سَمِعَ اللَّهُ لِمَنْ حَمِدَهُ،رَبَّنَا وَلَكَ الْحَمْدُ،مِلْءَ السَّمَاوَاتِ وَالْأَرْضِ وَمَا بَيْنَهُمَا،وَمِلْءَ مَا شِئْتَ مِنْ شَيْءٍ بَعْدُ}.وَإِذَا سَجَدَ قَالَ:{اللَّهُمَّ لَكَ سَجَدْتُ،وَبِكَ آمَنْتُ،وَلَكَ أَسْلَمْتُ،سَجَدَ وَجْهِي لِلَّذِي خَلَقَهُ وَصَوَّرَهُ فَأَحْسَنَ صُوَرَهُ،فَشَقَّ سَمْعَهُ وَبَصَرَهُ،فَتَبَارَكَ اللَّهُ أَحْسَنُ الْخَالِقِينَ}.
وَإِذَا فَرَغَ مِنَ الصَّلَاةِ وَسَلَّمَ قَالَ:{اللَّهُمَّ اغْفِرْ لِي مَا قَدَّمْتُ وَمَا أَخَّرْتُ،وَمَا أَسْرَرْتُ وَمَا أَعْلَنْتُ،وَمَا أَسْرَفْتُ،وَمَا أَنْتَ أَعْلَمُ بِهِ مِنِّي،أَنْتَ الْمُقَدِّمُ وَأَنْتَ الْمُؤَخِّرُ، لَا إِلَهَ إِلَّا أَنْتَ}".
(أحمد-٨٠٣)."""

        const val NARRATION_368 = """368. Bizə Hişam ibnul-Qasim danışdı, bizə Abdul-Aziz-yə'ni ibn Abdullah ibn əbi Sələmə-əmisi Məcişun ibn əbi Sələmədən, Ə'racdan, Ubeydullah ibn əbi Rafi'dən, Əli ibn əbi Talib radıyallahu anhudan:
"Nəbi sallallahu aleyhi və səlləm namazı(nı) açanda (başlayanda) təkbir edir, sonra (isə belə) deyirdi:
{Vaccəhtu vachiyə lilləzii fətaras-səməəvaati val-arda həniifən va məə ənə minəl-muşrikiinə, innə saləətii, va nusukii, va məhyəəyə, va məməətii lilləhi rabbil-aaləmiinə, ləə şəriikə ləhu va bizəlikə umirtu va ənə əvvalul-muslimiinə. Allahummə əntəl-məliku ləə iləhə illə əntə, əntə rabbii va ənə abdukə, zaləmtu nəfsii va’taraftu bizənbii, fəğfir lii zunubii cəmiiən, innəhu ləə yəğfiruz-zunuubə illə əntə, vahdini li əhsənil-əxləəqi, ləə yəhdii liəhsənihəə illə əntə, vasrıf annii seyyiəhə, ləə yasrıfu annii seyyiəhə illə əntə. Ləbbeykə va sə'deykə, val-xeyru kulluhu biyədeykə, vaş-şərru leysə ileykə, ənə bikə va ileykə, təbəraktə va təaaleytə, əstəğfirukə va ətuubu ileykə}.
Rüku etdikdə (isə belə) deyərdi:
{Allahummə ləkə rakə′tu va bikə əəməntu, va ləkə əsləmtu, xaşəa ləkə səm′ii va bəsarii, va muxxii, va izaamii va asabii}.
Başını (rükudan) qaldırdıqda (isə belə) deyərdi: {Səmia'llahu limən həmidəhu, mil'əs-səməəvaati val-ardi, va məə beynəhumə, va mil'ə məə şi'tə min şey'in bə'du}.
Səcdə etdikdə (isə belə) deyərdi: {Allahummə ləkə səcədtu, va bikə əəməntu, va ləkə əsləmtu, səcədə vachii lilləzii xaləqahu, va savvərahu, fəhsənə suvarahu, fəşəqqa səm'əhu va bəsarahu, təbərəkəllahu əhsənul-xaaliqiinə}.
Namazını bitirdikdən və salam verdikdən sonra (isə belə) deyərdi: {Allahummə-ğfir lii məə qaddəmtu, va məə əxxartu, va məə əsrartu, va məə ə'ləntu, va məə əsraftu, va məə əntə ə'ləmu bihi minnii, əntəl-muqaddimu va əntəl-muəxxiru, ləə iləhə illə əntə}".
(Əhməd-803)."""

        const val NOTE_368 = """Qeyd: 803 nömrəli hədisdəki zikrin tərcüməsi belədir:
"Hənif olaraq üzümü səmaları və yeri ilk Yaradana çevirdim və mən muşriklərdən deyiləm. Şübhəsiz ki, mənim namazım (da), qurbanım (və ya bütün ibadətlərim də), həyatım (da), ölümüm (də), aləmlərin Rəbbi olan Allah üçündür. Onun (heç bir) şəriki yoxdur. Mən bununla əmr olunmuşam və mən müsəlmanların əvvəliyəm (ilkiyəm). Allahummə!Sənsən Məlik, Səndən başqa (ibadətə və itaətə layiq haqq olan) ilah yoxdur. Sənsən Rəbbim!Mən (isə) Sənin qulunam. Nəfsimə zülm etdim. Günahımı e'tiraf etdim. Bütün günahlarımı bağışla. Şübhəsiz ki, günahları bağışlayan yalnız Sənsən. Məni əxlaqın ən gözəli ilə hidayətləndir, onun ən gözəlinə Səndən başqa heç kəs hidayətləndirməz. Onun (əxlaqın) pisini (pis əxlaqdan) məni uzaqlaşdır, onun pisliyini məndən yalnız Sən uzaqlaşdırırsan. Budur, Sənin qarşındayam, əmrindəyəm. Xeyrin hamısı (təkcə) Sənin əlindədir və şər Səndən deyil. Məni Sən yaratdın və Sənin hüzuruna qayıdacağam. Mübarək və Ucasan. Səndən günahlarımın bağışlanmasını diləyir və Sənə tövbə edirəm".
Rükuda deyilən zikrin tərcüməsi belədir:
"Allahummə! Sənə rüku etdim, Sənə iman gətirdim, Sənə təslim oldum!Qulağım, gözüm, beynim, sümüklərim, sinirim Sənə boyun əydi".
Rükudan qalxdıqda edilən zikrin tərcüməsi belədir:
"Allah Ona həmd edəni eşitdi. Ey Rəbbimiz, Sənə aiddir həmd, səmalar dolusu, yer dolusu, onların arasında olanlar və bundan sonra dilədiyin şey(lər) dolusu (Sənə həmd olsun)".
Səcdədə edilən zikrin tərcüməsi belədir:
"Allahummə!Sənə səcdə etdim, Sənə iman gətirdim, Sənə təslim oldum!Üzüm onu Yaradana, ona surət verib şəklini gözəl edənə, onda göz və qulaq açana səcdə etdi. Yaradanların ən gözəli olan Allahın şanı nə qədər ucadır!".
Salamdan sonra edilən zikrin tərcüməsi belədir:
"Allahummə! Önə göndərdiyimi və ertələdiyimi, gizlətdiyimi və aşkara çıxardığımı, israf etdiyimi və Sənin məndən daha yaxşı bildiyini mənə bağışla!Sənsən önə çəkən və Sənsən ertələyən!Yoxdur ilah Səndən başqa!"
Digər rəvayətlərdəki zikrlər bu hədisdə olandan fərqli olmadığı üçün təkrar etmədik.
Əbu Bəkr ibn əbi Şeybə, Əhməd, Darimi, Buxari "Raf'ul-yədeyn"-də, Muslim, Əbu Davud, ibn Macə, Tirmizi, Nəsai, Əbu Yə'lə, ibn Xuzeymə və ibn Hibban rəvayət etmişdir."""
    }
}
