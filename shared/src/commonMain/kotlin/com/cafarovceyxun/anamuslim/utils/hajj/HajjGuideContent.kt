package com.cafarovceyxun.anamuslim.utils.hajj

/**
 * Həcc və Ümrə bələdçisinin **addımları** — prototip (2026-10-03).
 *
 * Mənbə: Muheymin 2-ci cild → «Həcc kitabı» (Supabase `hadith_book.slug = 'c2he3'`, 53 bab).
 *
 * İki qat var:
 * - **addımlar** (bu fayl) — ardıcıllıq, günlər, növlər, alətlər. Sabitdir, kodla gəlir.
 * - **dəlillər və zikrlər** — `hajj_evidence` cədvəli, [HajjTopic] açarı ilə bağlanır. Admin onları
 *   oxucudan əlavə edir və bələdçinin içində redaktə edir, ona görə burada **yoxdur**.
 *
 * Addımların izah mətnləri (**[HajjStep.text]**, ihram siyahıları, növlərin fərqi) qısa izahdır, əsas olan
 * hədisin özüdür — ekranda bu nişanla göstərilir. Hər cümlə kitabdakı hədisə (№) söykənir; kitabda olmayan hökm
 * yazılmır (2026-10-06 tam yoxlaması: «İfrad — qurban vacib deyil», «İfadədən sonra səy hamıya» kimi cümlələr
 * kitabda yox idi və çıxarıldı).
 *
 * Məzmun hədis kimi **həmişə azərbaycancadır** — UI dili ərəbcə olanda belə `withScriptDirection`
 * ilə LTR qalır (CLAUDE.md → «Ərəbcə interfeys bütün düzülüşü RTL edir»).
 */
enum class HajjType(val title: String, val titleAr: String, val ihram: String, val sacrifice: String) {
    TAMATTU(
        title = "Təməttu'",
        titleAr = "تمتّع",
        ihram = "Ümrədən sonra açılır, 8 Zilhiccədə yenidən",
        sacrifice = "Qurban; tapmayan Həcdə üç, qayıdanda yeddi gün oruc (№ 858)",
    ),
    QIRAN(
        title = "Qiran",
        titleAr = "قِران",
        ihram = "Ümrə və Həcc bir ihramla, 10 Zilhiccəyə qədər",
        sacrifice = "Qurbanlıq özü ilə gətirilir (№ 858)",
    ),
    IFRAD(
        title = "İfrad",
        titleAr = "إفراد",
        ihram = "Yalnız Həcc, 10 Zilhiccəyə qədər",
        sacrifice = "Kitabda ayrıca hökm yoxdur",
    ),
}

/**
 * Dəlil mövzusu — `hajj_evidence.topic`.
 *
 * ⚠️ [key] bazada saxlanır: **heç vaxt dəyişmə**, yoxsa həmin mövzunun dəlilləri qopar. Yeni mövzu
 * əlavə etmək təhlükəsizdir — köhnə tətbiq onu sadəcə göstərmir.
 */
enum class HajjTopic(val key: String, val title: String) {
    VIRTUE("virtue", "Fəzilət"),
    TYPES("types", "Həcc növləri"),
    MIQAT("miqat", "Miqat"),
    IHRAM("ihram", "İhram və qadağaları"),
    TALBIYAH("talbiyah", "Təlbiyə"),
    TAWAF("tawaf", "Tavaf"),
    SAY("say", "Səy (Səfa və Mərvə)"),
    TARWIYA("tarwiya", "Tərviyə günü və Mina"),
    ARAFAH("arafah", "Ərəfat"),
    MUZDALIFAH("muzdalifah", "Müzdəlifə"),
    JAMARAT("jamarat", "Cəmərat (daş atmaq)"),
    HADY("hady", "Qurban (hədy)"),
    SHAVE("shave", "Saç qırxmaq / qısaltmaq"),
    IFADA("ifada", "İfadə tavafı"),
    TASHRIQ("tashriq", "Təşriq günləri"),
    WADA("wada", "Vida tavafı"),
    UMRAH("umrah", "Ümrə"),

    // 2026-10-06 — kitabda olub bələdçidə yeri olmayan hədislər üçün.
    WHO("who", "Kim Həcc edir"),
    CONDITION("condition", "Şərtli ihram və maneə"),
    HAIDH("haidh", "Heyzli və nifaslı qadın"),
    MAKKAH("makkah", "Məkkəyə giriş"),
    SAY_SINGLE("say_single", "Qiran və İfradda bir tavaf"),
    ZAMZAM("zamzam", "Zəmzəm"),
    ;

    companion object {
        fun of(key: String): HajjTopic? = entries.firstOrNull { it.key == key }
    }
}

/** Günlər xəttinin bölmələri. [number] boşdursa çip ad göstərir («Gəliş», «Vida»). */
enum class HajjDay(val number: String, val title: String, val titleAr: String) {
    ARRIVAL("", "Gəliş", ""),
    TARWIYA("8", "Tərviyə", "يوم التروية"),
    ARAFAH("9", "Ərəfə", "يوم عرفة"),
    NAHR("10", "Nəhr", "يوم النحر"),
    TASHRIQ("11–13", "Təşriq", "أيام التشريق"),
    FAREWELL("", "Vida", "طواف الوداع");

    /** Bölmə başlığı: «9 Zilhiccə, Ərəfə» və ya sadəcə «Miqat və Məkkə». */
    val heading: String
        get() = when (this) {
            ARRIVAL -> "Miqat və Məkkə"
            FAREWELL -> "Məkkədən çıxış"
            else -> "$number Zilhiccə, $title"
        }
}

/** Addımın açdığı alət. */
enum class HajjTool { IHRAM, COUNTER_TAWAF, COUNTER_SAY, MIQAT }

data class HajjStep(
    val id: String,
    val day: HajjDay,
    val title: String,
    val text: String,
    /**
     * Addım səhifəsində göstərilən mövzular — **zikrlər əvvəl**, sonra dəlillər. Birinci mövzu
     * əsasdır (sətirdəki say nişanı ondan gəlir).
     */
    val topics: List<HajjTopic>,
    /** Boş = bütün növlər. */
    val only: Set<HajjType> = emptySet(),
    val tool: HajjTool? = null,
) {
    fun appliesTo(type: HajjType): Boolean = only.isEmpty() || type in only
}

data class HajjLabeledItem(val title: String, val text: String)

data class HajjMiqat(val name: String, val nameAr: String, val forWhom: String)

object HajjGuideContent {

    private val T = HajjType.TAMATTU
    private val Q = HajjType.QIRAN
    private val I = HajjType.IFRAD

    private val IHRAM_TOPICS = listOf(HajjTopic.TALBIYAH, HajjTopic.IHRAM, HajjTopic.MIQAT, HajjTopic.CONDITION, HajjTopic.HAIDH)
    private val ARRIVAL_TAWAF_TOPICS = listOf(HajjTopic.TAWAF, HajjTopic.MAKKAH, HajjTopic.HAIDH)

    val steps: List<HajjStep> = listOf(
        HajjStep("ih-t", HajjDay.ARRIVAL, "Miqatda Ümrə üçün ihram", "İhram geyimi, niyyət və Təlbiyə.",
            IHRAM_TOPICS, setOf(T), HajjTool.IHRAM),
        HajjStep("ih-q", HajjDay.ARRIVAL, "Miqatda Həcc və Ümrə üçün ihram", "Bir niyyətlə ikisinə birlikdə, sonra Təlbiyə.",
            IHRAM_TOPICS + HajjTopic.TYPES, setOf(Q), HajjTool.IHRAM),
        HajjStep("ih-i", HajjDay.ARRIVAL, "Miqatda yalnız Həcc üçün ihram", "Niyyət və Təlbiyə.",
            IHRAM_TOPICS + HajjTopic.TYPES, setOf(I), HajjTool.IHRAM),
        HajjStep("tw-u", HajjDay.ARRIVAL, "Ümrə tavafı, yeddi dövrə",
            "Həcərul-Əsvəddən başlanır, ilk üç dövrə iti yerişlə (№ 861). Sonra İbrahim məqamının arxasında iki rükət.",
            ARRIVAL_TAWAF_TOPICS, setOf(T), HajjTool.COUNTER_TAWAF),
        HajjStep("tw-q", HajjDay.ARRIVAL, "Qudum (gəliş) tavafı",
            "Yeddi dövrə, ilk üçü iti yerişlə (№ 858, 861). Sonra məqamın arxasında iki rükət.",
            ARRIVAL_TAWAF_TOPICS, setOf(Q, I), HajjTool.COUNTER_TAWAF),
        HajjStep("sy-u", HajjDay.ARRIVAL, "Səfa ilə Mərvə arasında səy",
            "Səfadan başlayıb Mərvədə bitən yeddi gediş. Hər təpədə üzü Beytə zikr və dua.",
            listOf(HajjTopic.SAY), setOf(T), HajjTool.COUNTER_SAY),
        HajjStep("sy-q", HajjDay.ARRIVAL, "Səfa ilə Mərvə arasında səy",
            "Səfadan başlayıb Mərvədə bitən yeddi gediş. Həccə və ya cəmə təlbiyə edənlər «yalnız bir tavaf etdilər» " +
                "(№ 858); ibn Ömər Beyti və Səfa-Mərvəni bir dəfə tavaf edib Nəhr gününə qədər ihramda qaldı (№ 857).",
            listOf(HajjTopic.SAY, HajjTopic.SAY_SINGLE), setOf(Q, I), HajjTool.COUNTER_SAY),
        HajjStep("hl-u", HajjDay.ARRIVAL, "Saçı qısaltmaq, ihramdan çıxmaq",
            "Ümrə bitir. 8 Zilhiccəyə qədər ihram qadağaları qalxır.", listOf(HajjTopic.SHAVE), setOf(T)),
        HajjStep("st-q", HajjDay.ARRIVAL, "İhramda qalmaq",
            "Həccə və ya cəmə təlbiyə edənlər Nəhr gününə qədər ihramdan çıxmadılar (№ 821). Qurbanlığı olan qurban " +
                "kəsilənə qədər çıxmır (№ 823, 830).",
            listOf(HajjTopic.TYPES), setOf(Q, I)),
        HajjStep("ih8", HajjDay.TARWIYA, "Olduğu yerdən Həcc üçün ihram", "Məkkədə olan Məkkədən ihrama girir.",
            IHRAM_TOPICS, setOf(T), HajjTool.IHRAM),
        HajjStep("mina8", HajjDay.TARWIYA, "Minaya getmək, namazları orada qılmaq",
            "Rəsulullah Zöhr, Əsr, Məğrib, İşa və Sübhü Minada qıldı, sonra gün doğana qədər gözlədi (№ 861). " +
                "Dörd rükətliləri orada iki rükət qıldı (№ 905–908).",
            listOf(HajjTopic.TARWIYA)),
        HajjStep("arafat", HajjDay.ARAFAH, "Ərəfata getmək", "Yolda təlbiyə də, təkbir də deyilir (№ 862–863).",
            listOf(HajjTopic.ARAFAH, HajjTopic.TALBIYAH)),
        HajjStep("wuquf", HajjDay.ARAFAH, "Ərəfatda vüquf",
            "Zöhr və Əsr birlikdə, aralarında heç nə qılınmadan; sonra üzü qibləyə, gün batana qədər vüquf (№ 861). " +
                "Səlim: «Sünnətə uymaq istəyirsənsə, xütbəni qısa et, namazı tezləşdir»; ibn Ömər təsdiqlədi (№ 867).",
            listOf(HajjTopic.ARAFAH)),
        HajjStep("muzd", HajjDay.ARAFAH, "Müzdəlifəyə ifada, Məğrib və İşanı cəm",
            "Sükunətlə gedilir. Məğrib və İşa birlikdə, aralarında heç nə qılınmadan (№ 870, 872). Sübh erkən, " +
                "Məş'ərul-Haramda üzü qibləyə dua, gün doğmadan yola (№ 861, 878). Zəiflərə və qadınlara gecə " +
                "getməyə izin verildi (№ 874–877).",
            listOf(HajjTopic.MUZDALIFAH)),
        HajjStep("aqaba", HajjDay.NAHR, "Cəmrətul-Əqəbəyə yeddi daş",
            "Vadinin içindən, hər daşla təkbir (№ 861); Beyt solda, Mina sağda (№ 881). Rəsulullah təlbiyəni bu " +
                "daşlara qədər davam etdirdi (№ 880).",
            listOf(HajjTopic.JAMARAT)),
        HajjStep("hady", HajjDay.NAHR, "Qurban (hədy) kəsmək",
            "Dəvə ayaq üstə, bağlı halda kəsilir (№ 894); ətindən yeyilir (№ 861), qəssabın haqqı ondan verilmir " +
                "(№ 897). Qurban tapmayan Həcdə üç, qayıdanda yeddi gün oruc tutur (№ 858).",
            listOf(HajjTopic.HADY), setOf(T, Q)),
        HajjStep("shave", HajjDay.NAHR, "Saçı qırxmaq və ya qısaltmaq",
            "Daş və qurbandan sonra, sağ tərəfdən başlanır (№ 883). Qırxanlara iki dəfə, üçüncüdə qısaldanlara da dua " +
                "edildi (№ 887). Qadınlar yalnız qısaldır (№ 889).",
            listOf(HajjTopic.SHAVE)),
        HajjStep("ifada", HajjDay.NAHR, "İfadə tavafı və səy",
            "Ümrəyə təlbiyə edənlər Minadan qayıdandan sonra başqa bir tavaf etdilər (№ 858). Ardıcıllıq dəyişsə, " +
                "«bir günah yoxdur» (№ 899–901).",
            listOf(HajjTopic.IFADA, HajjTopic.SAY, HajjTopic.ZAMZAM), setOf(T), HajjTool.COUNTER_TAWAF),
        HajjStep("ifada-q", HajjDay.NAHR, "İfadə tavafı",
            "Həccə və ya cəmə təlbiyə edənlər «yalnız bir tavaf etdilər» (№ 858). Bu tavafdan sonra Rəsulullah " +
                "ihramın hər qadağasından çıxdı (№ 858). Ardıcıllıq dəyişsə, «bir günah yoxdur» (№ 899–901).",
            listOf(HajjTopic.IFADA, HajjTopic.SAY_SINGLE, HajjTopic.ZAMZAM), setOf(Q, I), HajjTool.COUNTER_TAWAF),
        HajjStep("jamarat", HajjDay.TASHRIQ, "Hər gün üç cəmrəyə yeddi daş",
            "Günəş zenitdən meyl edəndən sonra (№ 879). Kiçik, orta, sonra Əqəbə, hər daşla təkbir; ilk ikisindən " +
                "sonra üzü qibləyə uzun dua (№ 882).",
            listOf(HajjTopic.JAMARAT)),
        HajjStep("tashriq", HajjDay.TASHRIQ, "Təşriq günləri",
            "Yeyib-içmək günləridir, oruc tutulmur (№ 914–915). Abbas Mina gecələrini Məkkədə keçirmək üçün izin " +
                "istədi, su payladığı üçün izin verildi (№ 910).",
            listOf(HajjTopic.TASHRIQ, HajjTopic.TARWIYA)),
        HajjStep("wada", HajjDay.FAREWELL, "Vida tavafı",
            "Məkkədən çıxmazdan əvvəl son iş (№ 916). İfadə tavafını etmiş heyzli qadın gözləmədən gedir (№ 917, 920).",
            listOf(HajjTopic.WADA), tool = HajjTool.COUNTER_TAWAF),
    )

    fun stepsFor(type: HajjType): List<HajjStep> = steps.filter { it.appliesTo(type) }

    /** Ümrə rejiminin dörd addımı — eyni səhifələyici ilə vərəqlənir. */
    val umrahSteps: List<HajjStep> = listOf(
        HajjStep("u-ihram", HajjDay.ARRIVAL, "İhram", "Miqatda niyyət və Təlbiyə.",
            IHRAM_TOPICS + HajjTopic.UMRAH, tool = HajjTool.IHRAM),
        HajjStep("u-tawaf", HajjDay.ARRIVAL, "Tavaf",
            "Kə'bənin ətrafında yeddi dövrə, ilk üçü iti yerişlə (№ 861); məqamda iki rükət.",
            ARRIVAL_TAWAF_TOPICS, tool = HajjTool.COUNTER_TAWAF),
        HajjStep("u-say", HajjDay.ARRIVAL, "Səy", "Səfadan Mərvəyə yeddi gediş. Hər təpədə üzü Beytə zikr və dua.",
            listOf(HajjTopic.SAY), tool = HajjTool.COUNTER_SAY),
        HajjStep("u-shave", HajjDay.ARRIVAL, "Saç", "Qırxmaq və ya qısaltmaq, ihramdan çıxış.",
            listOf(HajjTopic.SHAVE)),
    )

    /**
     * Görüləcək işlər — yalnız kitabdakı hədislərdən (2026-10-06). Əvvəlki qaralamadakı «dırnaq təmizliyi», «iki
     * ağ parça (izar və rida)», «qadınlar adi paltarında qalır» kitabda yox idi; «təlbiyə Məkkəyə qədər» isə
     * № 880-i (Cəmrətul-Əqəbəyə qədər) gizlədirdi — indi iki hədis yanaşı verilir.
     */
    val ihramTodo: List<String> = listOf(
        "Miqatda ihrama girmək; miqatdan içəridə olan olduğu yerdən, Məkkə əhli Məkkədən (№ 807).",
        "Rəsulullah nifaslı Əsmaya «Qüsl et, bir paltara bürün və ihrama gir» dedi (№ 861). Heyzli qadın da " +
            "hacıların etdiyini edir, yalnız təmizlənənə qədər tavaf etmir (№ 859).",
        "Seçilən növə görə niyyət və təlbiyə: Ümrəyə, Həccə və ya ikisinə birlikdə (№ 820, 821).",
        "Xəstə şərt qoşa bilər: «Allahummə! Harada məni həbs etsən, ora mənim ihramdan çıxdığım yerdir» (№ 837).",
        "Təlbiyə ucadan deyilir (№ 819). İbn Ömər Hərəmə yaxınlaşanda təlbiyəni kəsər, Zu-Tuvada gecələyib qüsl " +
            "edər və bunu Rəsulullahdan danışardı (№ 833). Fadl isə xəbər verir ki, Rəsulullah Cəmrətul-Əqəbəyə " +
            "daş atana qədər təlbiyə etdi (№ 880).",
    )

    /**
     * Qadağalar — **yalnız kitabın öz bablarından**. Kitabda olmayan hökm buraya yazılmır: əvvəlki
     * qaralamada «ihramda nikah bağlanmır» vardı, halbuki 11-ci bab (№ 793) Rəsulullahın muhrim ikən
     * evləndiyini rəvayət edir (istifadəçi düzəlişi, 2026-10-03).
     */
    val ihramForbidden: List<HajjLabeledItem> = listOf(
        HajjLabeledItem("Tikili paltar", "Qamis, əmmamə, şalvar, burnus geyilmir; nə'l tapılmasa xuff topuqdan aşağı kəsilir (№ 789). İzar tapmayan şalvar geyinir (№ 790)."),
        HajjLabeledItem("Paltara ətir", "Zəfəran və ya vars dəymiş paltar geyilmir (№ 789). Cübbəsinə ətir dəymiş halda ihrama girənə: «Səndəki ətiri üç dəfə yu, cübbəni çıxar» (№ 929)."),
        HajjLabeledItem("Ov", "Ovlamaq, ova işarə etmək və kömək etmək; diri ov hədiyyəsi qəbul edilmir (№ 800–801)."),
        HajjLabeledItem("Rafəs və fisq", "Həcc edib qayıdana qədər rafəs və fisq etməyən anasından doğulduğu gün kimi qayıdar (№ 779). Kitabın qeydinə görə rafəs yaxınlıq, onun söhbəti və çirkin sözdür."),
    )

    /** Kitabın ihram bablarında **icazə** verilən işlər — qadağalarla qarışmasın deyə ayrıca. */
    val ihramAllowed: List<HajjLabeledItem> = listOf(
        HajjLabeledItem("Bədənə ətir", "İhrama girməzdən əvvəl bədənə vurulur; izi ihramda qalsa da olar (№ 791–792)."),
        HajjLabeledItem("Nikah", "Rəsulullah sallallahu aleyhi və səlləm muhrim ikən evləndi (№ 793)."),
        HajjLabeledItem("Hicamə", "Muhrim ikən qan aldırmaq (№ 794–795)."),
        HajjLabeledItem("Başı yumaq", "Muhrim başını yuya bilər (№ 796)."),
        HajjLabeledItem("Əziyyət verən heyvanlar", "Qarğa, çalağan, əqrəb, siçan və quduz it öldürülə bilər (№ 802–804)."),
        HajjLabeledItem("Ov əti", "Muhrim olmayanın öz-özünə ovladığından yemək — işarə və kömək olmayıbsa (№ 799–800)."),
        HajjLabeledItem("Zərurət olanda başı qırxmaq", "Başındakı bitlər əziyyət verirsə qırxır və fidyə verir: üç gün oruc, sədəqə və ya qurban (№ 797–798)."),
    )

    /** Hədis 805 və 807-dəki dörd miqat — kitabda olmayan miqat əlavə edilmir. */
    val miqats: List<HajjMiqat> = listOf(
        HajjMiqat("Zul-Huleyfə", "ذو الحليفة", "Mədinə tərəfdən gələnlər"),
        HajjMiqat("Cuhfə", "الجحفة", "Şam tərəfdən gələnlər"),
        HajjMiqat("Qarn", "قرن المنازل", "Nəcd tərəfdən gələnlər"),
        HajjMiqat("Yələmləm", "يلملم", "Yəmən tərəfdən gələnlər"),
    )

    // --- Sayğac ---------------------------------------------------------------------------------

    const val TAWAF_DIRECTION = "Həcərul-Əsvəddən Həcərul-Əsvədə"
    const val TAWAF_DONE = "Tavaf tamamlandı. İbrahim məqamının arxasında iki rükət."
    const val SAY_DONE = "Səy Mərvədə tamamlandı."

    /** Səyin [lap]-cı gedişinin istiqaməti (1-dən): tək dövrələr Səfadan, cütlər Mərvədən. */
    fun sayDirection(lap: Int): String = if (lap % 2 == 1) "Səfadan Mərvəyə" else "Mərvədən Səfaya"
}
