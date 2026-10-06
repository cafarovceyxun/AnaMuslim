package com.cafarovceyxun.anamuslim.utils.salah

/**
 * Namaz bələdçisi — **Mərhələ 3: Camaat və xüsusi namazlar** (2026-10-04).
 *
 * Mənbə: Muheymin 1-ci cild → «Namaz Kitabı» (`c1na4`, № 371–510): camaat, imamlıq, səflər, qunut, Cümə, səfər,
 * iki bayram, istisqa, küsuf, qorxu namazı. Quruluş əvvəlki mərhələlərlə eynidir: qısa izah burada, hədis
 * çıxarışları `salah_evidence`-də ([SalahTopic] açarı ilə, admin tətbiqin içindən dəyişir).
 * Maket və generator: `tools/salah-content/phase3/`.
 *
 * Kitabda olmayanlar ekrana yazılmayıb (istifadəçinin qaydası): bayram namazının əlavə təkbirləri, səfərin
 * məsafəsi və müddəti. Bunların yoxluğu ekranda açıq deyilir. İstisqa dualarının oxunuşu kitabda yoxdur —
 * bazadakı `transliteration` təklifdir, admin dəyişə bilər.
 */
object SalahGroupContent {

    // ---------------------------------------------------------------------------------------------
    // Camaat namazı
    // ---------------------------------------------------------------------------------------------

    val jamaahVirtue: List<RuleRow> = listOf(
        RuleRow(SalahTopic.JAMAAH_STEPS, RuleMark.YES, "Məscidə hər addım", "Gözəl dəstəmaz alıb yalnız namaz üçün çıxanın hər addımı bir dərəcə qaldırır, bir günah silir."),
        RuleRow(SalahTopic.JAMAAH_ISHA_FAJR, RuleMark.YES, "İşa və Sübh", null),
        RuleRow(SalahTopic.JAMAAH_FIRST_ROW, RuleMark.YES, "Əzan və ilk səf", "Bilsəydilər, püşk atardılar."),
        RuleRow(SalahTopic.JAMAAH_ABSENT, RuleMark.NO, "Camaatdan geri qalmaq", "İbn Məsud: münafiqdən başqası geri qalmazdı. Xəstəni də iki nəfərə söykədib gətirərdilər."),
    )

    val imamWho: List<RuleRow> = listOf(
        RuleRow(SalahTopic.IMAM_QURAN, RuleMark.INFO, "Quranı ən çox bilən", "Amr ibn Səlimə uşaq ikən öz qövmünə imam oldu."),
        RuleRow(SalahTopic.IMAM_OLDEST, RuleMark.INFO, "Yaşıd gənclərdə", "Nəbinin yanında iyirmi gecə qalan gənclərə dedi."),
        RuleRow(SalahTopic.IMAM_LIKE_ME, RuleMark.YES, "Nəbi kimi qılmaq", null),
    )

    /** «Amin» — kitabda tərcüməsi yoxdur, ona görə bazada deyil, oxunuşu ilə burada (№ 416). */
    val amin = FixedDhikr(arabic = "آمِينَ", transliteration = "Əəmiin", meaning = "")

    val follow: List<RuleRow> = listOf(
        RuleRow(SalahTopic.FOLLOW_BEFORE, RuleMark.NO, "İmamdan qabaq", null),
        RuleRow(SalahTopic.FOLLOW_AFTER, RuleMark.INFO, "İmam səcdəyə çatandan sonra", "Bəra: imam alnını yerə qoymamış heç kim belini bükməzdi."),
        RuleRow(SalahTopic.FOLLOW_SITTING, RuleMark.INFO, "İmam oturaraq qılanda",
            "Nəbi xəstə ikən arxasındakılara oturmağı əmr etdi. Humeydinin Buxaridəki qeydi: sonuncu dəfə Nəbi oturaraq qıldırdı, insanlar ayaq üstə qıldı və onlara oturmağı əmr etmədi."),
        RuleRow(SalahTopic.FOLLOW_COMPLETE, RuleMark.YES, "Rüku və səcdəni tamamlamaq", "Nəbi arxadakıları da görürdü."),
    )

    val late: List<RuleRow> = listOf(
        RuleRow(SalahTopic.LATE_WALK, RuleMark.YES, "Tələsmədən gəlmək", null),
        RuleRow(SalahTopic.LATE_STAND, RuleMark.INFO, "İmamı görməmiş durmamaq", null),
        RuleRow(SalahTopic.LATE_IQAMA, RuleMark.NO, "İqamədən sonra başqa namaz", "Sübhün iqaməsindən sonra iki rükət qılana dedi."),
    )

    val light: List<RuleRow> = listOf(
        RuleRow(SalahTopic.LIGHT_WEAK, RuleMark.YES, "Camaatda zəif və qoca var", "Təkbaşına qılan istədiyi qədər uzadır."),
        RuleRow(SalahTopic.LIGHT_ANGRY, RuleMark.NO, "Uzadıb insanları uzaqlaşdırmaq", "Nəbi bu barədə bərk qəzəbləndi."),
        RuleRow(SalahTopic.LIGHT_MUADH, RuleMark.INFO, "Muaza tövsiyə", "Muaz qövmünə İşada Bəqərəni oxuyanda."),
        RuleRow(SalahTopic.LIGHT_SHORT, RuleMark.INFO, "Xəfif, amma tam", "Yenə də rükudan sonra və iki səcdə arasında uzun dururdu (№ 380)."),
    )

    val qunut: List<RuleRow> = listOf(
        RuleRow(SalahTopic.QUNUT_AFTER, RuleMark.INFO, "Rükudan sonra", "Ənəsdən Sübhdə qunut soruşuldu."),
        RuleRow(SalahTopic.QUNUT_WHEN, RuleMark.INFO, "Kimin üçün və kimin əleyhinə", null),
        RuleRow(SalahTopic.QUNUT_MONTH, RuleMark.INFO, "Bir ay, sonra tərk", null),
        RuleRow(SalahTopic.QUNUT_AYAH, RuleMark.INFO, "Ayə nazil oldu", "Ali-İmran, 128."),
        RuleRow(SalahTopic.QUNUT_ABUHURAYRA, RuleMark.INFO, "Əbu Hureyrə", "«Sizə Rəsulullahın namazını yaxınlaşdıracağam» deyirdi."),
    )

    // ---------------------------------------------------------------------------------------------
    // Səflər
    // ---------------------------------------------------------------------------------------------

    /** Yuxarıdan baxış, qiblə yuxarıda; imamın sağı ekranın sağıdır. Koordinatlar 280 × hündürlük sahəsindədir. */
    val seats: List<SeatLayout> = listOf(
        SeatLayout("Bir kişi", SalahTopic.SEAT_ONE, "Tək kişi imamın sağında durur.", 100,
            listOf(SeatMark(SeatKind.IMAM, 140, 66, "İ"), SeatMark(SeatKind.MAN, 178, 66, "K"))),
        SeatLayout("Kişi və qadın", SalahTopic.SEAT_ONE_WOMAN, "Kişi imamın sağında, qadın onların arxasında.", 150,
            listOf(SeatMark(SeatKind.IMAM, 140, 62, "İ"), SeatMark(SeatKind.MAN, 178, 62, "K"), SeatMark(SeatKind.WOMAN, 159, 122, "Q"))),
        SeatLayout("Kişi və iki qadın", SalahTopic.SEAT_TWO_WOMEN, "Ənəs imamın sağında, anası və xalası arxada.", 150,
            listOf(SeatMark(SeatKind.IMAM, 140, 62, "İ"), SeatMark(SeatKind.MAN, 178, 62, "K"), SeatMark(SeatKind.WOMAN, 140, 122, "Q"), SeatMark(SeatKind.WOMAN, 178, 122, "Q"))),
        SeatLayout("İki uşaq və qadın", SalahTopic.SEAT_TWO_BOYS, "İki nəfər imamın arxasında bir səf olur, qadın onların arxasında.", 175,
            listOf(SeatMark(SeatKind.IMAM, 140, 48, "İ"), SeatMark(SeatKind.MAN, 120, 100, "U"), SeatMark(SeatKind.MAN, 160, 100, "U"), SeatMark(SeatKind.WOMAN, 140, 150, "Q"))),
        SeatLayout("Səflər", SalahTopic.ROWS_TIGHT, "Səflər düz, çiyin çiyinə, ayaq ayağa, aralarında boşluq qalmır. Qadınlar kişilərin arxasında səf tutur.", 195,
            buildList {
                add(SeatMark(SeatKind.IMAM, 140, 36, "İ"))
                (0..6).forEach { add(SeatMark(SeatKind.MAN, 56 + it * 28, 94, "")) }
                (0..6).forEach { add(SeatMark(SeatKind.MAN, 56 + it * 28, 130, "")) }
                (0..4).forEach { add(SeatMark(SeatKind.WOMAN, 84 + it * 28, 168, "")) }
            }),
    )

    val rows: List<RuleRow> = listOf(
        RuleRow(SalahTopic.ROWS_STRAIGHT, RuleMark.YES, "Ox kimi düz", "Səflər düzələndən sonra təkbir edərdi."),
        RuleRow(SalahTopic.ROWS_GAPS, RuleMark.YES, "Boşluq qoymamaq", null),
        RuleRow(SalahTopic.ROWS_SEES, RuleMark.INFO, "İmam deyir: «Düzəlin!»", null),
    )

    // ---------------------------------------------------------------------------------------------
    // Cümə
    // ---------------------------------------------------------------------------------------------

    /** Cümə günü — № 439-un ardıcıllığı ilə, əzan və xütbə № 453, 456-dan. */
    val jumuahDay: List<GuideStep> = listOf(
        GuideStep(SalahTopic.JUMUAH_GHUSL, "Qüsl", "Nəbi minbərdə əmr etdi."),
        GuideStep(SalahTopic.JUMUAH_PREP, "Təmizlənmək, yağ və ətir", null),
        GuideStep(SalahTopic.JUMUAH_EARLY, "Erkən getmək", "Erkən gələnin savabı aşağıdadır."),
        GuideStep(SalahTopic.JUMUAH_NO_SPLIT, "İki nəfərin arasını ayırmamaq", null),
        GuideStep(SalahTopic.JUMUAH_PRAY, "Namaz qılmaq", "Nə qədər yazılıbsa. İmam xütbədə olsa da gələn iki rükət qılır."),
        GuideStep(SalahTopic.JUMUAH_QUIET, "İmam çıxanda susmaq", null),
        GuideStep(SalahTopic.JUMUAH_ADHAN, "Əzan", "İmam minbərdə oturanda."),
        GuideStep(SalahTopic.JUMUAH_KHUTBAH, "İki xütbə", "Ayaq üstə, arada oturur. Duada yalnız şəhadət barmağı ilə işarə."),
        GuideStep(SalahTopic.JUMUAH_RAKAH, "İki rükət namaz", "Birinci rükətdə Cümə surəsi, ikincidə Münafiqun və ya Ğaşiyə."),
        GuideStep(SalahTopic.JUMUAH_HOME, "Evdə iki rükət", "İbn Ömər Cümədən sonra evinə qayıdıb iki rükət qılardı."),
    )

    /** № 442: erkən gələnin dərəcələri. */
    val earlyHours: List<Pair<String, String>> = listOf(
        "1-ci saat" to "dəvə",
        "2-ci saat" to "inək",
        "3-cü saat" to "buynuzlu qoç",
        "4-cü saat" to "toyuq",
        "5-ci saat" to "yumurta",
    )

    val jumuahRules: List<RuleRow> = listOf(
        RuleRow(SalahTopic.JUMUAH_TIME, RuleMark.INFO, "Vaxtı", "Günəş meyl edəndə. Namazdan sonra qailə edərdilər."),
        RuleRow(SalahTopic.JUMUAH_HOUR, RuleMark.YES, "Duanın qəbul olduğu saat", "Nəbi əli ilə onun az olduğunu göstərdi."),
        RuleRow(SalahTopic.JUMUAH_GHUSL_WHY, RuleMark.INFO, "Qüslün səbəbi", "İnsanlar işdən tozlu, tərli gəlirdi."),
        RuleRow(SalahTopic.JUMUAH_TAHIYYA, RuleMark.YES, "Xütbədə gələn", "Oturan kişiyə soruşdu, «Xeyr» deyəndə iki rükət qılmağı əmr etdi."),
        RuleRow(SalahTopic.JUMUAH_FAJR, RuleMark.INFO, "Cümə gününün Sübhündə", "Səcdə və İnsan surələri."),
        RuleRow(SalahTopic.JUMUAH_CARAVAN, RuleMark.NO, "Xütbəni qoyub getmək", "Cümə surəsinin 11-ci ayəsi nazil oldu."),
    )

    // ---------------------------------------------------------------------------------------------
    // Səfər
    // ---------------------------------------------------------------------------------------------

    /** Evdə və səfərdə rükət sayı. Səfərdə Məğrib 3, İşa 2 — İbn Ömərin birləşdirməsi (№ 478); Sübh dəyişmir. */
    val travelRakats: List<TravelRakat> = listOf(
        TravelRakat("Sübh", 2, 2),
        TravelRakat("Zöhr", 4, 2),
        TravelRakat("Əsr", 4, 2),
        TravelRakat("Məğrib", 3, 3),
        TravelRakat("İşa", 4, 2),
    )

    val safarStart: List<RuleRow> = listOf(
        RuleRow(SalahTopic.SAFAR_TOWN, RuleMark.INFO, "Şəhərdən çıxanda", null),
        RuleRow(SalahTopic.SAFAR_SAFE, RuleMark.INFO, "Əmin-amanlıqda da", null),
    )

    val safarCombine: List<CombineCard> = listOf(
        CombineCard(SalahTopic.SAFAR_ZUHR_ASR, "Zöhr və Əsr", "Zöhr vaxtı", "Əsr vaxtı: Zöhr + Əsr",
            "Günəş meyl etməzdən əvvəl yola çıxanda Zöhrü Əsrə ertələyirdi. Günəş meyl edəndə Zöhrü qılıb yola çıxırdı."),
        CombineCard(SalahTopic.SAFAR_MAGHRIB_ISHA, "Məğrib və İşa", "Məğrib vaxtı", "şəfəq itəndə: Məğrib + İşa",
            "Yol tələsdirəndə. Nəbi Muzdəlifədə Məğribi üç, İşanı iki rükət qıldı, aralarında namaz qılmadı. İbn Ömər hər birinə iqamə verdirirdi."),
    )

    val safarOther: List<RuleRow> = listOf(
        RuleRow(SalahTopic.SAFAR_ARAFAH, RuleMark.INFO, "Ərəfatda Zöhr vaxtında", "Günəş meyl edəndə xütbə verdi, sonra Zöhrü və Əsri arada heç nə qılmadan qıldı."),
        RuleRow(SalahTopic.SAFAR_HADAR, RuleMark.INFO, "Şəhərdə də birləşdirdi", "İbn Abbas Mədinədə Nəbi ilə səkkiz və yeddi rükəti birləşdirərək qıldı."),
        RuleRow(SalahTopic.SAFAR_NO_SUNNAH, RuleMark.NO, "Səfərdə sünnət", "Fərzdən əvvəl və sonra qılmazdılar."),
        RuleRow(SalahTopic.SAFAR_MINA, RuleMark.INFO, "Mukim imamın arxasında", "Osman Minada dörd qıldı. İbn Məsud onu qınadı, sonra özü də dörd qıldı."),
    )

    const val SAFAR_GAP = "Səfərin məsafəsi və neçə gün qalanda qısaldıldığı bu bablarda yoxdur."

    // ---------------------------------------------------------------------------------------------
    // Bayram
    // ---------------------------------------------------------------------------------------------

    val eidSteps: List<GuideStep> = listOf(
        GuideStep(SalahTopic.EID_MUSALLA, "Musalləyə çıxmaq", null),
        GuideStep(SalahTopic.EID_NO_ADHAN, "Əzan və iqamə yoxdur", null),
        GuideStep(SalahTopic.EID_TWO, "İki rükət namaz", null),
        GuideStep(SalahTopic.EID_KHUTBAH, "Sonra xütbə, ayaq üstə", "Salamdan sonra oturan camaata üz tutur."),
        GuideStep(SalahTopic.EID_SADAQA, "Sədəqəyə çağırış", null),
    )

    const val EID_GAP = "Bayram namazında əlavə təkbirlərin sayı və nə oxunduğu bu bablarda yoxdur."

    val eidRules: List<RuleRow> = listOf(
        RuleRow(SalahTopic.EID_FIRST, RuleMark.YES, "Namaz xütbədən əvvəl", "Nəbi, Əbu Bəkr, Ömər və Osman belə edərdi."),
        RuleRow(SalahTopic.EID_MARWAN, RuleMark.NO, "Xütbəni namazdan qabağa keçirmək", "Mərvan minbərə çıxmaq istəyəndə Əbu Səid dedi."),
    )

    val eidWomen: List<RuleRow> = listOf(
        RuleRow(SalahTopic.EID_WOMEN, RuleMark.INFO, "Qadınlar da çıxsın", "Heyzli qadınlar musallədən kənarda durur, örtüyü olmayana rəfiqəsi verir."),
        RuleRow(SalahTopic.EID_WOMEN_SADAQA, RuleMark.INFO, "Qadınların sədəqəsi", "Xütbədən sonra qadınların yanına gəldi."),
    )

    // ---------------------------------------------------------------------------------------------
    // İstisqa
    // ---------------------------------------------------------------------------------------------

    val istisqaMusalla: List<GuideStep> = listOf(
        GuideStep(SalahTopic.ISTISQA_OUT, "Musalləyə çıxmaq", null),
        GuideStep(SalahTopic.ISTISQA_BACK, "Qibləyə dönüb dua etmək", "Arxasını camaata çevirir, sonra ridanı tərsinə çevirir."),
        GuideStep(SalahTopic.ISTISQA_HANDS, "Əlləri yuxarı qaldırmaq", null),
        GuideStep(SalahTopic.ISTISQA_PRAY, "İki rükət, uca səslə", null),
    )

    val istisqaJumuah: List<GuideStep> = listOf(
        GuideStep(SalahTopic.ISTISQA_JUMUAH, "Minbərdə dua", "Əllərini sinəsinin önündə açdı, ovuclarının içi yerə baxırdı (Həmmadın vəsfi)."),
    )

    val raisingHands: List<RuleRow> = listOf(
        RuleRow(SalahTopic.ISTISQA_ONLY, RuleMark.INFO, "İstisqada", null),
        RuleRow(SalahTopic.ISTISQA_OTHER, RuleMark.INFO, "Başqa dualarda da", "Xalidin əməlindən sonra Nəbi, namazda isə Əbu Bəkr əllərini qaldırdı."),
    )

    // ---------------------------------------------------------------------------------------------
    // Küsuf
    // ---------------------------------------------------------------------------------------------

    /** Hər rükətdə iki qiyam və iki rüku (№ 500, 503). [KusufPart.long] — uzun; ikinci rükət birincidən qısadır. */
    val kusufRakats: List<List<KusufPart>> = listOf(
        listOf(KusufPart("qiyam · uzun qiraət", true), KusufPart("rüku · uzun", true), KusufPart("qiyam · bir az qısa"), KusufPart("rüku · bir az qısa"), KusufPart("2 səcdə · uzun", sajda = true)),
        listOf(KusufPart("qiyam · birincidən qısa"), KusufPart("rüku"), KusufPart("qiyam"), KusufPart("rüku"), KusufPart("2 səcdə", sajda = true)),
    )

    val kusufDo: List<RuleRow> = listOf(
        RuleRow(SalahTopic.KUSUF_UNTIL, RuleMark.YES, "Namaz, açılanadək", null),
        RuleRow(SalahTopic.KUSUF_DO, RuleMark.YES, "Dua, təkbir, sədəqə", null),
        RuleRow(SalahTopic.KUSUF_ZIKR, RuleMark.YES, "Zikr", null),
        RuleRow(SalahTopic.KUSUF_ITQ, RuleMark.YES, "Qul azad etmək", null),
        RuleRow(SalahTopic.KUSUF_KHUTBAH, RuleMark.INFO, "Namazdan sonra xütbə", "Günəş açılandan sonra."),
    )

    // ---------------------------------------------------------------------------------------------
    // Qorxu namazı
    // ---------------------------------------------------------------------------------------------

    private val P = LaneKind.PRAYER
    private val G = LaneKind.GUARD
    private val S = LaneKind.SALAM
    private val X = LaneKind.NONE
    private fun c(text: String, kind: LaneKind) = LaneCell(text, kind)

    val khawfForms: List<KhawfForm> = listOf(
        KhawfForm("1 + 1", SalahTopic.KHAWF_1, "Hər dəstə imamla bir rükət, sonra təkbaşına bir rükət qılır. İmam iki rükət qılır.", listOf(
            listOf(c("1-ci rükət", P), c("imamla 1 rükət", P), c("keşikdə", G)),
            listOf(c("", X), c("salamsız keşiyə keçir", G), c("gəlir", X)),
            listOf(c("2-ci rükət, salam", S), c("keşikdə", G), c("imamla 1 rükət", P)),
            listOf(c("", X), c("qalan 1 rükət təkbaşına", P), c("qalan 1 rükət təkbaşına", P)),
        )),
        KhawfForm("Növbəli keşik", SalahTopic.KHAWF_2, "Hamı imamla təkbir gətirir. Bir dəstə onunla səcdə edir, ikinci rükətdə qalxıb keşik çəkir, o biri dəstə gəlib imamla rüku və səcdə edir.", listOf(
            listOf(c("təkbir", P), c("təkbir", P), c("təkbir", P)),
            listOf(c("rüku, səcdə", P), c("imamla rüku, səcdə", P), c("keşikdə", G)),
            listOf(c("2-ci rükət", P), c("qalxıb keşikdə", G), c("imamla rüku, səcdə", P)),
        )),
        KhawfForm("Düşmən qiblədə", SalahTopic.KHAWF_3, "Düşmən qiblə tərəfdədir. İki səf hamısı imamla təkbir və rüku edir, səcdəni növbə ilə edirlər, ikinci rükətdə səflər yer dəyişir, hamı birlikdə salam verir.", listOf(
            listOf(c("təkbir, rüku", P), c("təkbir, rüku", P), c("təkbir, rüku", P)),
            listOf(c("səcdə", P), c("imamla səcdə", P), c("ayaqda keşik", G)),
            listOf(c("qalxır", P), c("qalxır", P), c("səcdə edir", P)),
            listOf(c("", X), c("arxaya keçir", X), c("önə keçir", X)),
            listOf(c("2-ci rükət: rüku", P), c("rüku", P), c("rüku", P)),
            listOf(c("səcdə", P), c("keşik", G), c("imamla səcdə", P)),
            listOf(c("oturur", P), c("səcdə edir", P), c("oturur", P)),
            listOf(c("salam", S), c("salam", S), c("salam", S)),
        ), lanes = listOf("İmam", "Ön səf", "Arxa səf")),
        KhawfForm("İmam 4", SalahTopic.KHAWF_4, "Zatur-Riqada imam hər dəstə ilə ayrıca iki rükət qıldı. İmam dörd, hər dəstə iki rükət qıldı.", listOf(
            listOf(c("2 rükət", P), c("imamla 2 rükət", P), c("keşikdə", G)),
            listOf(c("daha 2 rükət", P), c("keşikdə", G), c("imamla 2 rükət", P)),
        )),
        KhawfForm("Hər dəstə 1", SalahTopic.KHAWF_5, "İmam iki rükət, hər dəstə bir rükət qıldı. Cabir: döyüşdə qəsr budur.", listOf(
            listOf(c("1-ci rükət", P), c("imamla 1 rükət", P), c("keşikdə", G)),
            listOf(c("2-ci rükət", P), c("keşiyə keçir", G), c("imamla 1 rükət", P)),
            listOf(c("oturur, salam", S), c("salam", S), c("salam", S)),
        )),
    )

    /** Ekranlarda işlənən Mərhələ 3 mövzuları. */
    val usedTopics: Set<SalahTopic>
        get() = buildSet {
            (jamaahVirtue + imamWho + follow + late + light + qunut + rows + jumuahRules + safarStart + safarOther +
                eidRules + eidWomen + raisingHands + kusufDo).forEach { add(it.topic) }
            seats.forEach { add(it.topic) }
            (jumuahDay + eidSteps + istisqaMusalla + istisqaJumuah).forEach { add(it.topic) }
            safarCombine.forEach { add(it.topic) }
            khawfForms.forEach { add(it.topic) }
            addAll(
                listOf(
                    SalahTopic.JAMAAH_25, SalahTopic.FOLLOW_IMAM, SalahTopic.FOLLOW_AMIN, SalahTopic.FOLLOW_RABBANA,
                    SalahTopic.LATE_HAMD, SalahTopic.ROWS_OR, SalahTopic.ROWS_WOMEN, SalahTopic.JUMUAH_DAY,
                    SalahTopic.SAFAR_SADAQA, SalahTopic.SAFAR_TWO, SalahTopic.EID_DAYS, SalahTopic.ISTISQA_ASK,
                    SalahTopic.ISTISQA_DUA, SalahTopic.KUSUF_SIGNS, SalahTopic.KUSUF_HOW, SalahTopic.KUSUF_WOMEN,
                    SalahTopic.KHAWF_SEVERE,
                ),
            )
        }
}

/** Ardıcıl addım: başlıq və qısa izah koddan, çıxarış [topic] üzrə bazadan. */
data class GuideStep(val topic: SalahTopic, val title: String, val text: String?)

enum class SeatKind { IMAM, MAN, WOMAN }

/** Sxemdəki bir nəfər — mərkəzi (x, y) 280 enli sahədə, [label] dairənin içində. */
data class SeatMark(val kind: SeatKind, val x: Int, val y: Int, val label: String)

data class SeatLayout(val title: String, val topic: SalahTopic, val caption: String, val height: Int, val marks: List<SeatMark>)

data class TravelRakat(val name: String, val home: Int, val travel: Int)

data class CombineCard(val topic: SalahTopic, val title: String, val first: String, val second: String, val text: String)

data class KusufPart(val label: String, val long: Boolean = false, val sajda: Boolean = false)

enum class LaneKind { PRAYER, GUARD, SALAM, NONE }

data class LaneCell(val text: String, val kind: LaneKind)

/** Qorxu namazının bir forması: hər sətir bir mərhələdir, sütunlar [lanes] (imam və iki dəstə). */
data class KhawfForm(
    val title: String,
    val topic: SalahTopic,
    val summary: String,
    val rows: List<List<LaneCell>>,
    val lanes: List<String> = listOf("İmam", "1-ci dəstə", "2-ci dəstə"),
)
