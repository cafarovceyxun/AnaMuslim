package com.cafarovceyxun.anamuslim.utils.salah

/**
 * Namaz bələdçisi — **Mərhələ 4: Nafilə və cənazə** (2026-10-04).
 *
 * Mənbə: Muheymin 1-ci cild → «Namaz Kitabı» (`c1na4`, № 511–595; bazada № 559 yoxdur): sünnət və nafilə
 * namazları, gecə namazı və vitr, səhv və tilavət səcdəsi, cənazə. Quruluş əvvəlki mərhələlərlə eynidir: qısa
 * izah burada, hədis çıxarışları və üç gecə duası `salah_evidence`-də ([SalahTopic] açarı ilə).
 * Maket və generator: `tools/salah-content/phase4/`.
 *
 * Kitabda olmayanlar ekrana yazılmayıb, yoxluğu açıq deyilir: cənazə təkbirlərindən sonra deyilənlər və salam
 * (7-ci cildin hələ yüklənməmiş «Cənazələr» kitabındadır), tilavət səcdəsinin zikri və səcdə ayələrinin siyahısı.
 */
object SalahNaflContent {

    // ---------------------------------------------------------------------------------------------
    // Sünnət namazları
    // ---------------------------------------------------------------------------------------------

    /**
     * Fərzdən əvvəl və sonra — yalnız İbn Ömərin on rükəti (№ 512), Məğribdən əvvəlki iki rükət (№ 519–521) və
     * qadağa (№ 516). On iki rükət (№ 511) bu bablarda tək-tək sadalanmır, ona görə toplanmayıb.
     * Fərzin rükət sayı Mərhələ 2-dəndir ([SalahPrayerContent.prayers]).
     */
    val sunnahMap: List<SunnahRow> = listOf(
        SunnahRow("Sübh", SunnahCell("2", "heç tərk etmədi", SunnahKind.PROPHET), SunnahCell("2"), SunnahCell("—", "günəş doğanadək qadağa", SunnahKind.FORBIDDEN)),
        SunnahRow("Zöhr", SunnahCell("2", null, SunnahKind.PROPHET), SunnahCell("4"), SunnahCell("2", null, SunnahKind.PROPHET)),
        SunnahRow("Əsr", SunnahCell(null, NOT_HERE, SunnahKind.NONE), SunnahCell("4"), SunnahCell("—", "günəş batanadək qadağa", SunnahKind.FORBIDDEN)),
        SunnahRow("Məğrib", SunnahCell("2", "istəyən üçün", SunnahKind.OPTIONAL), SunnahCell("3"), SunnahCell("2", "evdə", SunnahKind.PROPHET)),
        SunnahRow("İşa", SunnahCell(null, NOT_HERE, SunnahKind.NONE), SunnahCell("4"), SunnahCell("2", "evdə", SunnahKind.PROPHET)),
        SunnahRow("Cümə", SunnahCell(null, NOT_HERE, SunnahKind.NONE), SunnahCell("Cümə"), SunnahCell("2", "evdə", SunnahKind.PROPHET)),
    )

    private const val NOT_HERE = "bu bablarda yoxdur"

    const val SUNNAH_TWELVE_NOTE = "Bu bablarda on iki rükət tək-tək sadalanmır, sadalanan İbn Ömərin on rükətidir. Fərzlərin rükət sayı «Əzan və namaz» bölməsindəndir."

    val fajr: List<RuleRow> = listOf(
        RuleRow(SalahTopic.FAJR_NEVER, RuleMark.YES, "Ən çox qorunan nafilə", "Aişə: Sübhdən əvvəlki və Əsrdən sonrakı iki rükəti gizlində də, aşkarda da tərk etmədi."),
        RuleRow(SalahTopic.FAJR_EAGER, RuleMark.YES, "Tələsərdi", null),
        RuleRow(SalahTopic.FAJR_LIGHT, RuleMark.INFO, "Xəfif", "Əzan ilə iqamə arasında, o qədər yüngül ki, Aişə Fatihəni oxudumu deyə düşünərdi."),
        RuleRow(SalahTopic.FAJR_READING, RuleMark.INFO, "Nə oxunur",
            "İbn Abbas: birinci rükətdə Bəqərə, 136. İkinci rükət üçün rəvayətlər fərqlidir: İbn Xuzeymədə Ali-İmran, 64; Əhmədin rəvayətindəki sözlər aşağıdadır."),
        RuleRow(SalahTopic.FAJR_LIE, RuleMark.INFO, "Sonra uzanmaq", "Sağ böyrü üstə, müəzzin iqamə üçün gələnədək. Aişə oyaq olsa, onunla danışardı."),
    )

    val sunnahOther: List<RuleRow> = listOf(
        RuleRow(SalahTopic.SUNNAH_NAHY, RuleMark.NO, "Sübhdən və Əsrdən sonra", "Günəş doğanadək və batanadək."),
        RuleRow(SalahTopic.SUNNAH_ASR_TWO, RuleMark.INFO, "Əsrdən sonrakı iki rükət",
            "Babın adı «Nəbiyə xas olması»dır. Aişə: ümmətinə ağır olmasın deyə onları məsciddə qılmazdı."),
    )

    // ---------------------------------------------------------------------------------------------
    // Duha və nafilələr
    // ---------------------------------------------------------------------------------------------

    val duha: List<RuleRow> = listOf(
        RuleRow(SalahTopic.DUHA_WILL, RuleMark.YES, "İki rükət", "Nəbi Əbu Hureyrəyə üç şeyi vəsiyyət etdi."),
        RuleRow(SalahTopic.DUHA_EIGHT, RuleMark.INFO, "Səkkiz rükət", "Məkkənin fəthi günü Ummu Həninin evində, qüsldən sonra."),
        RuleRow(SalahTopic.DUHA_FULL, RuleMark.INFO, "Xəfif, amma tam", null),
        RuleRow(SalahTopic.DUHA_ANAS, RuleMark.INFO, "Ənəs", "Ənsardan bir kişinin evində həsirin üstündə iki rükət qıldı."),
        RuleRow(SalahTopic.DUHA_AISHA, RuleMark.INFO, "Aişə", "Nəbi sevdiyi əməli insanlara fərz olunmasın deyə tərk edirdi."),
        RuleRow(SalahTopic.DUHA_IBN_UMAR, RuleMark.INFO, "İbn Ömər", "Özü, Ömər və Əbu Bəkr haqqında «Xeyr» dedi."),
    )

    /** № 532-nin qeydi (müəllifin sözü), olduğu kimi. */
    const val DUHA_NOTE = "Əbu Hureyrə və Əbu Zər hədislərindən açıq-aydın şəkildə bilinir ki, Rəsulullah sallallahu aleyhi və səlləm ümmətini bu namaza irşad etmişdir."

    val nafl: List<RuleRow> = listOf(
        RuleRow(SalahTopic.NAFL_RETURN, RuleMark.YES, "Səfərdən qayıdanda", "Nəbi Mədinəyə çatanda Cabirə dedi."),
        RuleRow(SalahTopic.NAFL_BILAL, RuleMark.YES, "Dəstəmazdan sonra", "Nəbi Cənnətdə Bilalın addım səsini eşitdi və səbəbini soruşdu."),
        RuleRow(SalahTopic.NAFL_HOME, RuleMark.YES, "Evdə qılmaq", null),
        RuleRow(SalahTopic.NAFL_ABLE, RuleMark.YES, "Az da olsa, davamlı", null),
        RuleRow(SalahTopic.NAFL_TIRED, RuleMark.INFO, "Yorulanda oturmaq", "Zeynəbin məsciddə yorulanda tutduğu ipi açdırdı."),
    )

    // ---------------------------------------------------------------------------------------------
    // Gecə namazı və vitr
    // ---------------------------------------------------------------------------------------------

    val nightWhy: List<RuleRow> = listOf(
        RuleRow(SalahTopic.NIGHT_KNOTS, RuleMark.INFO, "Şeytanın üç düyünü", null),
        RuleRow(SalahTopic.NIGHT_SLEPT, RuleMark.NO, "Səhərədək yatan", null),
        RuleRow(SalahTopic.NIGHT_RIGHTS, RuleMark.INFO, "Özünü yormamaq", "Gecələri qılıb gündüzləri oruc tutan Abdullah ibn Amra dedi."),
    )

    val nightWhen: List<RuleRow> = listOf(
        RuleRow(SalahTopic.NIGHT_TIME, RuleMark.INFO, "Gecənin axırı", "Xoruz səsi eşidiləndə qalxardı."),
        RuleRow(SalahTopic.WITR_ANY, RuleMark.INFO, "Vitr gecənin hər vaxtında", null),
        RuleRow(SalahTopic.NIGHT_SLEEP_AFTER, RuleMark.INFO, "Vitrdən sonra yatmaq", null),
    )

    /** Kitabdakı rəvayətlər yan-yana, biri seçilmir. [NightForm.total] hədisdəki saydır. */
    val nightForms: List<NightForm> = listOf(
        NightForm("4 + 4 + 3", SalahTopic.NIGHT_FORM_A, "Ramazanda da, başqa aylarda da on bir rükətdən artıq qılmırdı.", 11,
            listOf(NightBlock("4", 4), NightBlock("4", 4), NightBlock("3", 3, NightBlockKind.WITR))),
        NightForm("2 × 5 + 1", SalahTopic.NIGHT_FORM_B, "İşadan Fəcrə qədər, hər iki rükətdə salam; sonra sağ böyrü üstə uzanardı.", 11,
            List(5) { NightBlock("2", 2) } + NightBlock("1", 1, NightBlockKind.WITR)),
        NightForm("10 + 1 + Fəcr", SalahTopic.NIGHT_FORM_C, "Aişə: Fəcrin iki rükəti ilə birlikdə on üç.", 13,
            listOf(NightBlock("10", 10), NightBlock("1", 1, NightBlockKind.WITR), NightBlock("Fəcr 2", 2, NightBlockKind.SUNNAH))),
        NightForm("Zeyd: 13", SalahTopic.NIGHT_FORM_D, "İşanın sünnəti, iki çox uzun rükət, sonra hər biri əvvəlkindən qısa dörd cüt, sonra vitr.", 13,
            listOf(NightBlock("İşa 2", 2, NightBlockKind.SUNNAH)) + List(5) { NightBlock("2", 2) } + NightBlock("1", 1, NightBlockKind.WITR)),
        NightForm("İbn Abbas", SalahTopic.NIGHT_FORM_E,
            "Altı cüt və vitr, sonra uzandı; müəzzin gələndə iki xəfif rükət. Başqa gecə: İşadan sonra evdə dörd, yatdı, beş, sonra iki.", 13,
            List(6) { NightBlock("2", 2) } + NightBlock("1", 1, NightBlockKind.WITR) + NightBlock("yatdı", 0, NightBlockKind.SLEEP) +
                NightBlock("Fəcr 2", 2, NightBlockKind.SUNNAH, counted = false)),
    )

    val nightWake: List<GuideStep> = listOf(
        GuideStep(SalahTopic.NIGHT_WAKE_FACE, "Üzü silmək", "Oyanıb əli ilə üzündəki yuxunu silərdi."),
        GuideStep(SalahTopic.NIGHT_WAKE_QURAN, "Ali-İmranın sonunu oxumaq", "Bəzən çölə çıxıb səmaya baxar, sonra oxuyardı."),
        GuideStep(SalahTopic.NIGHT_WAKE_MISWAK, "Misvak və dəstəmaz", null),
    )

    /** № 551-in qeydindəki iki izah (dua kartının altında). */
    const val TAHAJJUD_NOTES = "Qəyyum: işlərini idarə edən, bütün hallarını bilən, himayə edən. «Sənin üçün xüsumət etdim»: Sənin uğrunda düşmənçilik etdim, Sənin üçün mücadilə etdim (№ 551-in qeydi)."

    val nightOther: List<RuleRow> = listOf(
        RuleRow(SalahTopic.NIGHT_EYES, RuleMark.INFO, "«Gözlərim yatır»", "Aişə vitrdən əvvəl yatırmı deyə soruşanda."),
        RuleRow(SalahTopic.NIGHT_RIGHT_SIDE, RuleMark.INFO, "Tək qoşulan sağda durur", "İbn Abbas solunda durdu, Nəbi onu sağına keçirdi."),
        RuleRow(SalahTopic.NIGHT_RAMADAN, RuleMark.INFO, "Ramazanda camaatla", "Bir neçə gecə arxasında toplaşdılar, sonra çıxmadı."),
        RuleRow(SalahTopic.NIGHT_TARTIL, RuleMark.INFO, "Tərtil", null),
        RuleRow(SalahTopic.NIGHT_SITTING, RuleMark.INFO, "Oturaraq", "Yaşlananda oturaraq oxuyardı."),
        RuleRow(SalahTopic.NIGHT_LONG_SAJDA, RuleMark.INFO, "Uzun səcdə", null),
    )

    // ---------------------------------------------------------------------------------------------
    // Səhv səcdəsi
    // ---------------------------------------------------------------------------------------------

    private val salam = SahwPart("salam", SahwPartKind.SALAM)
    private fun sajda(label: String) = SahwPart(label, SahwPartKind.SAJDA)
    private fun step(label: String) = SahwPart(label)

    /** «Nə oldu?» — hər halda hədisdəki ardıcıllıq. Şəkk üçün iki hədis var, biri o birinin üstünə qoyulmur. */
    val sahwCases: List<SahwCase> = listOf(
        SahwCase("Şəkk etdim", listOf(
            SahwWay("Yəqin üzərində · № 572", SalahTopic.SAHW_YAQIN, listOf(step("şəkki at, yəqinə görə tamamla"), sajda("2 səcdə"), salam)),
            SahwWay("Doğrunu araşdırmaq · № 573", SalahTopic.SAHW_TAHARRI, listOf(step("araşdır, onun üzərində tamamla"), salam, sajda("2 səcdə"))),
        )),
        SahwCase("Artıq qıldım", listOf(
            SahwWay("Zöhrü beş rükət qıldı · № 573", SalahTopic.SAHW_EXTRA, listOf(step("5 rükət"), salam, step("xəbər verdilər"), sajda("2 səcdə")),
                "Başqa rəvayətdə iki səcdədən sonra yenə salam verdi."),
        )),
        SahwCase("Tez salam verdim", listOf(
            SahwWay("Zul-Yədeyn · № 574", SalahTopic.SAHW_LESS,
                listOf(SahwPart("2 rükətdə salam", SahwPartKind.SALAM), step("xatırlatdılar"), step("qalan 2 rükət"), salam, sajda("təkbir, səcdə"), sajda("təkbir, səcdə"))),
        )),
        SahwCase("Təşəhhüdü unutdum", listOf(
            SahwWay("İbn Büheynə · № 575", SalahTopic.SAHW_TASHAHHUD,
                listOf(step("2-ci rükətdə oturmadan qalxdı"), step("namazı bitirdi"), sajda("təkbir, oturaraq 2 səcdə"), salam)),
        )),
    )

    // ---------------------------------------------------------------------------------------------
    // Tilavət səcdəsi
    // ---------------------------------------------------------------------------------------------

    /** Kitabda adı keçən üç surə. Səcdə ayəsinin nömrəsi kitabda yoxdur — düymə surəni əvvəldən açır. */
    val tilawahSurahs: List<TilawahSurah> = listOf(
        TilawahSurah("النَّجْم", "Nəcm", 53, "№ 577"),
        TilawahSurah("الِانْشِقَاق", "İnşiqaq", 84, "№ 578–579"),
        TilawahSurah("الْعَلَق", "Aləq", 96, "№ 579"),
    )

    val tilawah: List<RuleRow> = listOf(
        RuleRow(SalahTopic.TILAWAH_NAJM, RuleMark.YES, "Nəcm", "Məkkədə; yalnız bir şeyx bir ovuc torpağı alnına qaldırdı."),
        RuleRow(SalahTopic.TILAWAH_INSHIQAQ, RuleMark.YES, "İnşiqaq, namazda da", "Əbu Hureyrə İşa namazında oxuyub səcdə etdi."),
        RuleRow(SalahTopic.TILAWAH_ALAQ, RuleMark.YES, "Aləq", "Əbu Hureyrə Nəbi ilə birlikdə İnşiqaqda və Aləqdə səcdə etdi."),
    )

    const val TILAWAH_GAP = "Bu səcdədə nə deyildiyi və səcdə ayələrinin tam siyahısı bu bablarda yoxdur."

    // ---------------------------------------------------------------------------------------------
    // Cənazə
    // ---------------------------------------------------------------------------------------------

    val janazahSteps: List<GuideStep> = listOf(
        GuideStep(SalahTopic.JANAZAH_WASH, "Yumaq", "Tək sayda: üç, beş, yeddi və ya daha çox; su və sidr ilə, sonuncuda kafur."),
        GuideStep(SalahTopic.JANAZAH_WASH_RIGHT, "Sağdan başlamaq", "Sağ tərəflərdən və dəstəmaz yerlərindən."),
        GuideStep(SalahTopic.JANAZAH_SHROUD, "Kəfən", "Nəbi qızı üçün öz izarını verdi."),
        GuideStep(SalahTopic.JANAZAH_ROWS, "Musallə və səflər", null),
    )

    val janazahPrayer: List<GuideStep> = listOf(
        GuideStep(SalahTopic.JANAZAH_TAKBIR, "Dörd təkbir", null),
        GuideStep(SalahTopic.JANAZAH_FATIHA, "Fatihə", "İbn Abbas cənazədə oxudu və bunun sünnət olduğunu dedi."),
    )

    /** Cənazənin kitabda olmayan hissəsi — uydurulmur, 7-ci cildin «Cənazələr» kitabı yüklənəndə doldurulur. */
    const val JANAZAH_WAIT = "Təkbirlərdən sonra deyilənlər və salam bu bablarda yoxdur. Onlar 7-ci cildin «Cənazələr» kitabındadır, o yüklənəndən sonra buraya əlavə olunacaq."

    val janazahWhom: List<RuleRow> = listOf(
        RuleRow(SalahTopic.JANAZAH_GRAVE, RuleMark.YES, "Qəbrə", "Dəfn olunmuş kimsəyə də qıldı."),
        RuleRow(SalahTopic.JANAZAH_ABROAD, RuleMark.YES, "Uzaqda ölənə", "Nəcaşi Həbəşistanda öləndə."),
        RuleRow(SalahTopic.JANAZAH_MARTYRS, RuleMark.YES, "Şəhidlərə", "Uhud şəhidlərinə səkkiz ildən sonra."),
        RuleRow(SalahTopic.JANAZAH_DEBT, RuleMark.INFO, "Borcluya", "Əvvəl borcunu ödəməyə mal qoymayana özü qılmazdı, sonra borcu öz öhdəsinə götürdü."),
        RuleRow(SalahTopic.JANAZAH_MUNAFIQ, RuleMark.NO, "Münafiqə", "Abdullah ibn Ubeyyə qıldıqdan sonra ayə nazil oldu."),
    )

    val graves: List<RuleRow> = listOf(
        RuleRow(SalahTopic.JANAZAH_GRAVES, RuleMark.NO, "Qəbirlərin üstündə oturmaq, onlara tərəf namaz", null),
        RuleRow(SalahTopic.JANAZAH_PLACE, RuleMark.INFO, "Cənazənin harada qoyulması",
            "Babın adı belədir; hədisin özü Aişənin Nəbi ilə qiblə arasında uzandığını deyir."),
    )

    /** Ekranlarda işlənən Mərhələ 4 mövzuları. */
    val usedTopics: Set<SalahTopic>
        get() = buildSet {
            (fajr + sunnahOther + duha + nafl + nightWhy + nightWhen + nightOther + tilawah + janazahWhom + graves).forEach { add(it.topic) }
            (nightWake + janazahSteps + janazahPrayer).forEach { add(it.topic) }
            nightForms.forEach { add(it.topic) }
            sahwCases.forEach { c -> c.ways.forEach { add(it.topic) } }
            addAll(
                listOf(
                    SalahTopic.SUNNAH_12, SalahTopic.SUNNAH_TEN, SalahTopic.SUNNAH_BETWEEN, SalahTopic.MAGHRIB_BEFORE,
                    SalahTopic.NAFL_WOMEN, SalahTopic.NIGHT_DAWUD, SalahTopic.NIGHT_TWO_TWO, SalahTopic.WITR_ONE,
                    SalahTopic.NIGHT_DUA_TAHAJJUD, SalahTopic.NIGHT_DUA_NUR, SalahTopic.NIGHT_DUA_SAJDA, SalahTopic.NIGHT_WOMEN,
                    SalahTopic.SAHW_SHAYTAN, SalahTopic.SAHW_SIT, SalahTopic.TILAWAH_OUT, SalahTopic.JANAZAH_QIRAT,
                    SalahTopic.JANAZAH_HAIR, SalahTopic.JANAZAH_ISTIGHFAR, SalahTopic.JANAZAH_WOMEN,
                ),
            )
        }
}

enum class SunnahKind { PROPHET, OPTIONAL, FORBIDDEN, FARD, NONE }

/** Cədvəl xanası: [main] böyük rəqəm (yoxdursa boş), [sub] altında kiçik izah. */
data class SunnahCell(val main: String?, val sub: String? = null, val kind: SunnahKind = SunnahKind.FARD)

data class SunnahRow(val name: String, val before: SunnahCell, val fard: SunnahCell, val after: SunnahCell)

enum class NightBlockKind { NIGHT, WITR, SUNNAH, SLEEP }

/** Gecə namazının bir hissəsi; [counted] — hədisdəki ümumi saya daxildirmi. */
data class NightBlock(val label: String, val rakats: Int, val kind: NightBlockKind = NightBlockKind.NIGHT, val counted: Boolean = true)

data class NightForm(val title: String, val topic: SalahTopic, val summary: String, val total: Int, val blocks: List<NightBlock>)

enum class SahwPartKind { STEP, SAJDA, SALAM }

data class SahwPart(val label: String, val kind: SahwPartKind = SahwPartKind.STEP)

data class SahwWay(val title: String, val topic: SalahTopic, val parts: List<SahwPart>, val note: String? = null)

data class SahwCase(val title: String, val ways: List<SahwWay>)

data class TilawahSurah(val arabic: String, val name: String, val chapter: Int, val refs: String)
