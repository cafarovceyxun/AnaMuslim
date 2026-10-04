package com.cafarovceyxun.anamuslim.utils.salah

/**
 * Namaz bələdçisi — **Mərhələ 2: Əzan və namaz** (2026-10-04).
 *
 * Mənbə: Muheymin 1-ci cild → «Namaz Kitabı» (`c1na4`, № 183–370), rükət sayında 2-ci cild № 872.
 * Quruluş Mərhələ 1 ilə eynidir: addımın sabit mətni burada, hədis çıxarışları və zikrlər `salah_evidence`-də
 * ([SalahTopic] açarı ilə, admin tətbiqin içindən dəyişir).
 *
 * İstifadəçinin qərarları (2026-10-04):
 * - şəkillər onun öz kadrlarıdır (uşaq fiquru), vektora çevrilib — `dr_salah_pose_*`;
 * - rükudan qalxanda əllər yenə sinədə bağlanır — dəlil № 330 və № 194 (ixtisar);
 * - iki səcdə arasında dua yoxdur (kitabda yoxdur), salavat addımı yoxdur (namazda deyilməsinə dəlil yoxdur);
 * - salamın sözləri istifadəçidəndir ([salamWords]), kitabda yoxdur.
 */
object SalahPrayerContent {

    /** 16 addım — istifadəçinin sxeminin ardıcıllığı ilə. */
    val steps: List<PrayerStep> = listOf(
        PrayerStep(SalahTopic.PRAYER_NIYYAH, PrayerPose.STAND, "Niyyət",
            "Namaz niyyətlə başlayır. Hədis bütün əməllər üçündür və cildin ilk hədisidir."),
        PrayerStep(SalahTopic.PRAYER_TAKBIR, PrayerPose.TAKBIR, "Təkbir",
            "Qibləyə yönəlib «Allahu Əkbər» deyilir. Əllər çiyin səviyyəsinə qaldırılır."),
        PrayerStep(SalahTopic.PRAYER_HANDS, PrayerPose.QIYAM, "Əllər sinədə",
            "Sağ əl sol biləyin üstünə qoyulur."),
        PrayerStep(SalahTopic.PRAYER_OPENING, PrayerPose.QIYAM, "Açılış duası",
            "Nəbi təkbirlə qiraət arasında bir az susardı. Əbu Hureyrə soruşdu, o da bu duanı dediyini bildirdi. Başqa açılış duası № 332-dədir."),
        PrayerStep(SalahTopic.PRAYER_READING, PrayerPose.QIYAM, "Qiraət",
            "Fatihə oxunur. İlk iki rükətdə ondan sonra bir surə də oxunur, qalan rükətlərdə yalnız Fatihə."),
        PrayerStep(SalahTopic.PRAYER_RUKU_TAKBIR, PrayerPose.TAKBIR, "Rükuya təkbir",
            "Rükuya gedərkən əllər yenə qaldırılır və təkbir deyilir."),
        PrayerStep(SalahTopic.PRAYER_RUKU, PrayerPose.RUKU, "Rüku",
            "Əllər dizlərə qoyulur, bel düz saxlanılır. Əzalar sakitləşənədək rükuda qalınır."),
        PrayerStep(SalahTopic.PRAYER_RISE, PrayerPose.TAKBIR, "Rükudan qalxmaq",
            "Baş qaldırılarkən əllər yenə qalxır və bu zikr deyilir."),
        PrayerStep(SalahTopic.PRAYER_STANDING, PrayerPose.QIYAM, "Qiyamda dayanmaq",
            "Əllər yenə sinədə bağlanır. Bütün sümüklər yerinə oturanadək düz durulur. Nəbi burada uzun dayanardı."),
        PrayerStep(SalahTopic.PRAYER_SAJDA, PrayerPose.SAJDA, "Səcdə",
            "Təkbirlə səcdəyə gedilir. Yeddi üzv yerə dəyir: alın (burunla), iki əl, iki diz, ayaq barmaqları. Əllər yerdə, dirsəklər yuxarıda. Səcdədə əllər qaldırılmır."),
        PrayerStep(SalahTopic.PRAYER_BETWEEN, PrayerPose.SIT_FRONT, "İki səcdə arası",
            "Təkbirlə qalxılıb oturulur. Oturuşun forması № 358-də təşəhhüd üçün gəlir: sol ayağın üstündə, sağ ayaq dik. Nəbi burada da uzun oturardı.",
            insets = listOf(PrayerPose.FEET, PrayerPose.FEET_CIRCLE)),
        PrayerStep(SalahTopic.PRAYER_SAJDA2, PrayerPose.SAJDA, "İkinci səcdə",
            "Təkbirlə ikinci səcdə edilir, birinci kimi."),
        PrayerStep(SalahTopic.PRAYER_REST, PrayerPose.SIT_FRONT, "İkinci rükətə qalxmaq",
            "Birinci rükətin ikinci səcdəsindən sonra bir az oturub qalxırdı. İkinci rükət birincinin eynidir."),
        PrayerStep(SalahTopic.PRAYER_TASHAHHUD, PrayerPose.SIT_GAZE, "Təşəhhüd",
            "İkinci rükətdən sonra və son rükətdə oturub təşəhhüd oxunur. Sağ əl budun üstündə, şəhadət barmağı ilə qibləyə işarə edilir və baxış barmağa yönəlir.",
            insets = listOf(PrayerPose.HAND, PrayerPose.FRONT_GAZE)),
        PrayerStep(SalahTopic.PRAYER_DUA, PrayerPose.FRONT_GAZE, "Dua",
            "Son təşəhhüddən sonra dörd şeydən Allaha sığınılır, sonra istənilən dua seçilir."),
        PrayerStep(SalahTopic.PRAYER_SALAM, PrayerPose.SALAM, "Salam",
            "Əvvəl sağa, sonra sola salam verilir. Yanağın ağlığı görünənədək dönülür.",
            pair = true, showsSalamWords = true),
    )

    /**
     * Salamın sözləri — **istifadəçidən** (2026-10-04), kitabın yeddi cildində yoxdur; ona görə bazada deyil,
     * kodda saxlanır və mənbə kimi hədis göstərilmir. Tərcüməni istifadəçi təsdiqlədi.
     */
    val salamWords = FixedDhikr(
        arabic = "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللَّهِ",
        transliteration = "Əs-sələmu aleykum va rahmətullah",
        meaning = "Sizə Allahın salamı və rəhməti olsun",
    )

    /** Rükət sayı və qiraət — xəritənin hər namazı. */
    val prayers: List<PrayerRakat> = listOf(
        PrayerRakat("Sübh", 2, SalahTopic.RAKAT_FAJR, PrayerReading.HEARD, SalahTopic.READING_FAJR),
        PrayerRakat("Zöhr", 4, SalahTopic.RAKAT_ZUHR, PrayerReading.SILENT, SalahTopic.READING_SILENT),
        PrayerRakat("Əsr", 4, SalahTopic.RAKAT_FOUR, PrayerReading.SILENT, SalahTopic.READING_SILENT),
        PrayerRakat("Məğrib", 3, SalahTopic.RAKAT_MAGHRIB, PrayerReading.HEARD, SalahTopic.READING_MAGHRIB),
        PrayerRakat("İşa", 4, SalahTopic.RAKAT_FOUR, PrayerReading.UNKNOWN, null),
    )

    /** Cibrilin iki gündə göstərdiyi vaxtlar (№ 255) — hədisin öz təsviri, qısaldılmış. */
    val jibrilTimes: List<Triple<String, String, String>> = listOf(
        Triple("Zöhr", "Günəş meyl edəndə", "hər şeyin kölgəsi bir misli olanda"),
        Triple("Əsr", "kölgə bir misli olanda", "kölgə iki misli olanda"),
        Triple("Məğrib", "Günəş batanda", "eyni vaxtda"),
        Triple("İşa", "şəfəq itəndə", "gecənin yarısı və ya üçdə biri gedəndə"),
        Triple("Fəcr", "dan yeri ağaranda", "dan yeri çox saralanda"),
    )

    val timesRules: List<RuleRow> = listOf(
        RuleRow(SalahTopic.TIMES_ONTIME, RuleMark.YES, "Vaxtında qılmaq", "Ən fəzilətli əməllər sırasındadır."),
        RuleRow(SalahTopic.TIMES_FORGOT, RuleMark.INFO, "Unudulan namaz", null),
        RuleRow(SalahTopic.TIMES_RAKAH, RuleMark.INFO, "Bir rükətə çatan", null),
        RuleRow(SalahTopic.TIMES_COOL, RuleMark.INFO, "İstidə Zöhr", null),
        RuleRow(SalahTopic.TIMES_ISHA, RuleMark.INFO, "İşanı gecikdirmək", null),
    )
    val timesForbidden: List<RuleRow> = listOf(
        RuleRow(SalahTopic.TIMES_AFTER, RuleMark.NO, "Sübhdən və Əsrdən sonra", null),
        RuleRow(SalahTopic.TIMES_SUN, RuleMark.NO, "Günəş doğarkən və batarkən", null),
        RuleRow(SalahTopic.TIMES_NOON, RuleMark.NO, "Günəş tam ortada ikən", null),
    )

    val qiblaRules: List<RuleRow> = listOf(
        RuleRow(SalahTopic.QIBLA_TURNED, RuleMark.INFO, "Namazın içində dönmək", "Qubada xəbər gələndə namazda olanlar Kəbəyə döndülər."),
        RuleRow(SalahTopic.QIBLA_FARD, RuleMark.YES, "Fərz qibləyə", null),
        RuleRow(SalahTopic.QIBLA_MOUNT, RuleMark.INFO, "Nafilə minik üstündə", null),
    )

    /** Əzan və iqamə — **istifadəçinin verdiyi cədvəl**, hərfi. Dəlil: № 307 (əzan cüt, iqamə tək). */
    val adhan: List<AdhanLine> = listOf(
        AdhanLine("اللَّهُ أَكْبَرُ", "Allahu Əkbər", "Allah ən böyükdür", adhan = 2, iqama = 1),
        AdhanLine("أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ", "Əşhədu əllə iləhə illəllah", "Şahidlik edirəm ki, Allahdan başqa ilah yoxdur", adhan = 2, iqama = 1),
        AdhanLine("أَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ", "Əşhədu ənnə Muhəmmədən rasulullah", "Şahidlik edirəm ki, Muhəmməd Allahın Rəsuludur", adhan = 2, iqama = 1),
        AdhanLine("حَيَّ عَلَى الصَّلَاةِ", "Həyyə aləs-saləh", "Namaza gəlin", adhan = 2, iqama = 1),
        AdhanLine("حَيَّ عَلَى الْفَلاَحِ", "Həyyə aləl-fələh", "Qurtuluşa gəlin", adhan = 2, iqama = 1),
        AdhanLine("قَدْ قَامَتِ الصَّلَاةُ", "Qad-qamətis - saləh", "Namaz başladı", adhan = 0, iqama = 2),
        AdhanLine("اللَّهُ أَكْبَرُ", "Allahu Əkbər", "Allah ən böyükdür", adhan = 2, iqama = 1),
        AdhanLine("لَا إِلَهَ إِلَّا اللَّهُ", "Lə iləhə illəllah", "Allahdan başqa ilah yoxdur", adhan = 2, iqama = 1),
    )
    val adhanRules: List<RuleRow> = listOf(
        RuleRow(SalahTopic.ADHAN_BILAL, RuleMark.INFO, "İlk müəzzin Bilal", null),
        RuleRow(SalahTopic.ADHAN_LOUD, RuleMark.YES, "Səsi uca", null),
        RuleRow(SalahTopic.ADHAN_FAJR, RuleMark.INFO, "Fəcrin iki əzanı", "Bilal gecə əzan verirdi."),
        RuleRow(SalahTopic.ADHAN_RAIN, RuleMark.INFO, "Soyuq və yağışda", "Müəzzin bunu əlavə edir."),
        RuleRow(SalahTopic.ADHAN_LEAVE, RuleMark.NO, "Əzandan sonra çıxıb getmək", null),
    )

    /** Təsbih sayğacı (№ 369); sözlər babın adındandır. */
    val counter: List<FixedDhikr> = listOf(
        FixedDhikr("سُبْحَانَ اللَّهِ", "Subhənallah", "", times = 33),
        FixedDhikr("الْحَمْدُ لِلَّهِ", "Əlhəmdulilləh", "", times = 33),
        FixedDhikr("اللَّهُ أَكْبَرُ", "Allahu əkbər", "", times = 33),
    )

    val knowNo: List<RuleRow> = listOf(
        RuleRow(SalahTopic.KNOW_SKY, RuleMark.NO, "Gözləri səmaya qaldırmaq", "Nəbi bu barədə sərt danışdı."),
        RuleRow(SalahTopic.KNOW_HIPS, RuleMark.NO, "İxtisar", null),
        RuleRow(SalahTopic.KNOW_LOOK, RuleMark.NO, "Sağa-sola boylanmaq", null),
        RuleRow(SalahTopic.KNOW_TALK, RuleMark.NO, "Danışmaq", null),
        RuleRow(SalahTopic.KNOW_SPIT, RuleMark.NO, "Önə və sağa tüpürmək", null),
        RuleRow(SalahTopic.KNOW_FOOD, RuleMark.NO, "Yemək hazır ikən, ayaqyolu sıxanda", null),
        RuleRow(SalahTopic.KNOW_SHOULDERS, RuleMark.NO, "Çiyinlər açıq", null),
        RuleRow(SalahTopic.KNOW_SLEEPY, RuleMark.INFO, "Mürgü gələndə", null),
    )
    val knowYes: List<RuleRow> = listOf(
        RuleRow(SalahTopic.KNOW_CHILD, RuleMark.YES, "Uşağı qucaqda tutmaq", "Nəbi nəvəsi Uməməni qucağında tutardı."),
        RuleRow(SalahTopic.KNOW_PEBBLES, RuleMark.INFO, "Yerdəki daşları düzəltmək", null),
        RuleRow(SalahTopic.KNOW_TASBIH, RuleMark.INFO, "Bir şey üz verəndə", null),
        RuleRow(SalahTopic.KNOW_ENTER, RuleMark.INFO, "Məscidə girəndə", null),
    )
    val sutra: List<RuleRow> = listOf(
        RuleRow(SalahTopic.KNOW_SUTRA, RuleMark.INFO, "Nə boyda", null),
        RuleRow(SalahTopic.KNOW_PASS_SIN, RuleMark.NO, "Qarşıdan keçmək", "Keçən bilsəydi, qırx gözləməsi daha xeyirli olardı."),
        RuleRow(SalahTopic.KNOW_PASS, RuleMark.INFO, "Keçmək istəyəni", null),
    )
    val knowWomen: List<RuleRow> = listOf(
        RuleRow(SalahTopic.KNOW_CLAP, RuleMark.INFO, "Bir şey üz verəndə", "Kişilər təsbih edir."),
        RuleRow(SalahTopic.KNOW_MOSQUE, RuleMark.INFO, "Məscidə getmək", "Kitabın qeydi (1): burada «qullar» qadın qullar deməkdir."),
    )

    /** Ekranlarda işlənən Mərhələ 2 mövzuları. */
    val usedTopics: Set<SalahTopic>
        get() = buildSet {
            steps.forEach { add(it.topic) }
            prayers.forEach { add(it.countTopic); it.readingTopic?.let(::add) }
            (timesRules + timesForbidden + qiblaRules + adhanRules + knowNo + knowYes + sutra + knowWomen).forEach { add(it.topic) }
            addAll(
                listOf(
                    SalahTopic.PRAYER_TEACH, SalahTopic.TIMES_BETWEEN, SalahTopic.TIMES_WOMEN, SalahTopic.QIBLA_AYAH,
                    SalahTopic.ADHAN_PAIRS, SalahTopic.ADHAN_REPEAT, SalahTopic.AFTER_TAKBIR, SalahTopic.AFTER_SALAM,
                    SalahTopic.AFTER_33, SalahTopic.AFTER_TAHLIL, SalahTopic.AFTER_LEFT, SalahTopic.KNOW_LIGHT,
                ),
            )
        }
}

/** İstifadəçinin kadrları, vektora çevrilmiş — `composeResources/drawable/dr_salah_pose_*.xml`. */
enum class PrayerPose { STAND, TAKBIR, QIYAM, RUKU, SAJDA, SALAM, SIT_FRONT, SIT_GAZE, FRONT_GAZE, HAND, FEET, FEET_CIRCLE }

/**
 * Bir addım. [insets] — yaxın plan kadrları; [pair] — eyni şəkil güzgü əksi ilə yanında (salam: sağa və sola);
 * [showsSalamWords] — [SalahPrayerContent.salamWords] kartı.
 */
data class PrayerStep(
    val topic: SalahTopic,
    val pose: PrayerPose,
    val title: String,
    val text: String,
    val insets: List<PrayerPose> = emptyList(),
    val pair: Boolean = false,
    val showsSalamWords: Boolean = false,
)

/** Bazada olmayan sabit zikr (salamın sözləri, sayğacın sözləri). [times] — sayğac üçün. */
data class FixedDhikr(val arabic: String, val transliteration: String, val meaning: String, val times: Int = 1)

data class AdhanLine(val arabic: String, val transliteration: String, val meaning: String, val adhan: Int, val iqama: Int)

enum class PrayerReading { HEARD, SILENT, UNKNOWN }

/** Namazın rükət sayı, onun dəlili və qiraətin eşidilib-eşidilməməsi. */
data class PrayerRakat(
    val name: String,
    val rakats: Int,
    val countTopic: SalahTopic,
    val reading: PrayerReading,
    val readingTopic: SalahTopic?,
) {
    /** Hər rükətin tərkibi — «Fatihə + surə» ilk ikidə (№ 336), ilk təşəhhüd ikincidə, sonuncu axırda (№ 358). */
    fun parts(rakat: Int): List<RakatPart> = buildList {
        add(if (rakat <= 2) RakatPart.FATIHA_SURAH else RakatPart.FATIHA_ONLY)
        add(RakatPart.RUKU)
        add(RakatPart.TWO_SAJDAS)
        if (rakat == 2 && rakats > 2) add(RakatPart.FIRST_TASHAHHUD)
        if (rakat == rakats) {
            add(RakatPart.LAST_TASHAHHUD)
            add(RakatPart.SALAM)
        }
    }
}

enum class RakatPart(val label: String) {
    FATIHA_SURAH("Fatihə + surə"),
    FATIHA_ONLY("yalnız Fatihə"),
    RUKU("rüku"),
    TWO_SAJDAS("2 səcdə"),
    FIRST_TASHAHHUD("ilk təşəhhüd"),
    LAST_TASHAHHUD("son təşəhhüd"),
    SALAM("salam"),
}
