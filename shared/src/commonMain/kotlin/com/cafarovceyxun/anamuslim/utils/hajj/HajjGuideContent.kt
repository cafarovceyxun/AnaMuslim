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
 * ⚠️ Addımların izah mətnləri (**[HajjStep.text]**, ihram siyahıları, növlərin fərqi) **qaralamadır** —
 * ekranda «alim yoxlaması lazımdır» nişanı ilə göstərilir.
 *
 * Məzmun hədis kimi **həmişə azərbaycancadır** — UI dili ərəbcə olanda belə `withScriptDirection`
 * ilə LTR qalır (CLAUDE.md → «Ərəbcə interfeys bütün düzülüşü RTL edir»).
 */
enum class HajjType(val title: String, val titleAr: String, val ihram: String, val sacrifice: String) {
    TAMATTU(
        title = "Təməttu'",
        titleAr = "تمتّع",
        ihram = "Ümrədən sonra açılır, 8 Zilhiccədə yenidən",
        sacrifice = "Vacibdir",
    ),
    QIRAN(
        title = "Qiran",
        titleAr = "قِران",
        ihram = "Ümrə və Həcc bir ihramla, 10 Zilhiccəyə qədər",
        sacrifice = "Vacibdir",
    ),
    IFRAD(
        title = "İfrad",
        titleAr = "إفراد",
        ihram = "Yalnız Həcc, 10 Zilhiccəyə qədər",
        sacrifice = "Vacib deyil",
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

    private val IHRAM_TOPICS = listOf(HajjTopic.TALBIYAH, HajjTopic.IHRAM, HajjTopic.MIQAT)

    val steps: List<HajjStep> = listOf(
        HajjStep("ih-t", HajjDay.ARRIVAL, "Miqatda Ümrə üçün ihram", "İhram geyimi, niyyət və Təlbiyə.",
            IHRAM_TOPICS, setOf(T), HajjTool.IHRAM),
        HajjStep("ih-q", HajjDay.ARRIVAL, "Miqatda Həcc və Ümrə üçün ihram", "Bir niyyətlə ikisinə birlikdə, sonra Təlbiyə.",
            IHRAM_TOPICS + HajjTopic.TYPES, setOf(Q), HajjTool.IHRAM),
        HajjStep("ih-i", HajjDay.ARRIVAL, "Miqatda yalnız Həcc üçün ihram", "Niyyət və Təlbiyə.",
            IHRAM_TOPICS + HajjTopic.TYPES, setOf(I), HajjTool.IHRAM),
        HajjStep("tw-u", HajjDay.ARRIVAL, "Ümrə tavafı, yeddi dövrə", "Sonra İbrahim məqamının arxasında iki rükət.",
            listOf(HajjTopic.TAWAF), setOf(T), HajjTool.COUNTER_TAWAF),
        HajjStep("tw-q", HajjDay.ARRIVAL, "Qudum (gəliş) tavafı", "Yeddi dövrə, sonra məqamın arxasında iki rükət.",
            listOf(HajjTopic.TAWAF), setOf(Q, I), HajjTool.COUNTER_TAWAF),
        HajjStep("sy-u", HajjDay.ARRIVAL, "Səfa ilə Mərvə arasında səy",
            "Səfadan başlayıb Mərvədə bitən yeddi gediş. Hər təpədə üzü Beytə zikr və dua.",
            listOf(HajjTopic.SAY), setOf(T), HajjTool.COUNTER_SAY),
        HajjStep("sy-q", HajjDay.ARRIVAL, "Səfa ilə Mərvə arasında səy",
            "Qudum tavafından sonra edilə bilər; edilməsə İfadə tavafından sonra edilir.",
            listOf(HajjTopic.SAY), setOf(Q, I), HajjTool.COUNTER_SAY),
        HajjStep("hl-u", HajjDay.ARRIVAL, "Saçı qısaltmaq, ihramdan çıxmaq",
            "Ümrə bitir. 8 Zilhiccəyə qədər ihram qadağaları qalxır.", listOf(HajjTopic.SHAVE), setOf(T)),
        HajjStep("st-q", HajjDay.ARRIVAL, "İhramda qalmaq", "İhram 10 Zilhiccədə daş atma və saçdan sonra açılır.",
            listOf(HajjTopic.TYPES), setOf(Q, I)),
        HajjStep("ih8", HajjDay.TARWIYA, "Olduğu yerdən Həcc üçün ihram", "Məkkədə olan Məkkədən ihrama girir.",
            IHRAM_TOPICS, setOf(T), HajjTool.IHRAM),
        HajjStep("mina8", HajjDay.TARWIYA, "Minaya getmək, Zöhrü orada qılmaq", "Gecə Minada keçirilir.",
            listOf(HajjTopic.TARWIYA)),
        HajjStep("arafat", HajjDay.ARAFAH, "Ərəfata getmək", "Yolda təlbiyə və təkbir.",
            listOf(HajjTopic.ARAFAH, HajjTopic.TALBIYAH)),
        HajjStep("wuquf", HajjDay.ARAFAH, "Ərəfatda vüquf", "Həccin rüknü. Gün batana qədər dua və zikr.",
            listOf(HajjTopic.ARAFAH)),
        HajjStep("muzd", HajjDay.ARAFAH, "Müzdəlifəyə ifada, Məğrib və İşanı cəm",
            "Gecə Müzdəlifədə. Zəiflərə erkən getmək üçün icazə var (bab 39).", listOf(HajjTopic.MUZDALIFAH)),
        HajjStep("aqaba", HajjDay.NAHR, "Cəmrətul-Əqəbəyə yeddi daş", "Hər daşla təkbir.",
            listOf(HajjTopic.JAMARAT)),
        HajjStep("hady", HajjDay.NAHR, "Qurban (hədy) kəsmək", "Təməttu' və Qiranda vacibdir.",
            listOf(HajjTopic.HADY), setOf(T, Q)),
        HajjStep("shave", HajjDay.NAHR, "Saçı qırxmaq və ya qısaltmaq", "Qadınlar yalnız qısaldır.",
            listOf(HajjTopic.SHAVE)),
        HajjStep("ifada", HajjDay.NAHR, "İfadə tavafı və səy", "Ardıcıllıq dəyişsə, «bir günah yoxdur».",
            listOf(HajjTopic.IFADA, HajjTopic.SAY), tool = HajjTool.COUNTER_TAWAF),
        HajjStep("jamarat", HajjDay.TASHRIQ, "Hər gün üç cəmrəyə yeddi daş",
            "Kiçik, orta, sonra Əqəbə. İlk ikisindən sonra üzü qibləyə dua.", listOf(HajjTopic.JAMARAT)),
        HajjStep("tashriq", HajjDay.TASHRIQ, "Təşriq günləri", "Yeyib-içmək günləridir, oruc tutulmur.",
            listOf(HajjTopic.TASHRIQ)),
        HajjStep("wada", HajjDay.FAREWELL, "Vida tavafı", "Məkkədən çıxmazdan əvvəl son iş. Heyzli qadına güzəşt var.",
            listOf(HajjTopic.WADA), tool = HajjTool.COUNTER_TAWAF),
    )

    fun stepsFor(type: HajjType): List<HajjStep> = steps.filter { it.appliesTo(type) }

    /** Ümrə rejiminin dörd addımı — eyni səhifələyici ilə vərəqlənir. */
    val umrahSteps: List<HajjStep> = listOf(
        HajjStep("u-ihram", HajjDay.ARRIVAL, "İhram", "Miqatda niyyət və Təlbiyə.",
            listOf(HajjTopic.TALBIYAH, HajjTopic.IHRAM, HajjTopic.MIQAT, HajjTopic.UMRAH), tool = HajjTool.IHRAM),
        HajjStep("u-tawaf", HajjDay.ARRIVAL, "Tavaf", "Kə'bənin ətrafında yeddi dövrə, məqamda iki rükət.",
            listOf(HajjTopic.TAWAF), tool = HajjTool.COUNTER_TAWAF),
        HajjStep("u-say", HajjDay.ARRIVAL, "Səy", "Səfadan Mərvəyə yeddi gediş. Hər təpədə üzü Beytə zikr və dua.",
            listOf(HajjTopic.SAY), tool = HajjTool.COUNTER_SAY),
        HajjStep("u-shave", HajjDay.ARRIVAL, "Saç", "Qırxmaq və ya qısaltmaq, ihramdan çıxış.",
            listOf(HajjTopic.SHAVE)),
    )

    val ihramTodo: List<String> = listOf(
        "Miqata çatmazdan əvvəl qüsl, dırnaq və bədən təmizliyi.",
        "Kişilər iki ağ parça (izar və rida) geyinir. Qadınlar adi örtülü paltarında qalır.",
        "Miqatda seçilən növə görə niyyət etmək.",
        "Təlbiyəni başlamaq və Məkkəyə qədər tez-tez təkrarlamaq.",
    )

    /**
     * Qadağalar — **yalnız kitabın öz bablarından**. Kitabda olmayan hökm buraya yazılmır: əvvəlki
     * qaralamada «ihramda nikah bağlanmır» vardı, halbuki 11-ci bab (№ 793) Rəsulullahın muhrim ikən
     * evləndiyini rəvayət edir (istifadəçi düzəlişi, 2026-10-03).
     */
    val ihramForbidden: List<HajjLabeledItem> = listOf(
        HajjLabeledItem("Tikili paltar", "Qamis, əmmamə, şalvar, xuff geyilmir; nə'l tapılmasa xuff topuqdan aşağı kəsilir. Kişilər üçün (№ 789)."),
        HajjLabeledItem("Paltara ətir", "İhram paltarına ətir vurulmur; zəfəran və ya varsla boyanmış paltar geyilmir (№ 789)."),
        HajjLabeledItem("Ov", "Ovlamaq, ova işarə etmək və kömək etmək; diri ov hədiyyəsi qəbul edilmir (№ 800–801)."),
    )

    /** Kitabın ihram bablarında **icazə** verilən işlər — qadağalarla qarışmasın deyə ayrıca. */
    val ihramAllowed: List<HajjLabeledItem> = listOf(
        HajjLabeledItem("Bədənə ətir", "İhrama girməzdən əvvəl bədənə vurulur; izi ihramda qalsa da olar (bab 10, № 791–792)."),
        HajjLabeledItem("Nikah", "Rəsulullah sallallahu aleyhi və səlləm muhrim ikən evləndi (bab 11, № 793)."),
        HajjLabeledItem("Hicamə", "Muhrim ikən qan aldırmaq (bab 12, № 794–795)."),
        HajjLabeledItem("Başı yumaq", "Muhrim başını yuya bilər (bab 13, № 796)."),
        HajjLabeledItem("Əziyyət verən heyvanlar", "Qarğa, çalağan, əqrəb, siçan və quduz it öldürülə bilər (bab 17, № 802–804)."),
        HajjLabeledItem("Ov əti", "Muhrim olmayanın öz-özünə ovladığından yemək — işarə və kömək olmayıbsa (bab 15, № 799–800)."),
        HajjLabeledItem("Zərurət olanda başı qırxmaq", "Başındakı bitlər əziyyət verirsə qırxır və fidyə verir: üç gün oruc, sədəqə və ya qurban (bab 14, № 797–798)."),
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
