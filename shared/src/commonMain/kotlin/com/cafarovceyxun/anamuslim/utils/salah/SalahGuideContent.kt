package com.cafarovceyxun.anamuslim.utils.salah

/**
 * Namaz bələdçisi — **Mərhələ 1: Təharət** (2026-10-04).
 *
 * Mənbə: Muheymin 1-ci cild → «Təharət kitabı» (Supabase `hadith_book.slug = 'c1te3'`, 13 bab, № 91–182).
 *
 * İki qat var, Həcc bələdçisində olduğu kimi:
 * - **addımlar** (bu fayl) — ardıcıllıq, şəkil, neçə dəfə, qısa izah. Sabitdir, kodla gəlir.
 * - **hədis çıxarışları** — `salah_evidence` cədvəli, [SalahTopic] açarı ilə. Admin tətbiqin içindən
 *   dəyişir, ona görə burada **yoxdur**.
 *
 * Qayda (istifadəçi, 2026-10-04): Rəsulullahın etdiyi və dediyi dindir. Hədis kitabda necə gəlirsə elə
 * göstərilir; buradakı qısa izahlar yalnız onu tapmağa kömək edir. İzah hədislə uyğun gəlmirsə,
 * düzəldilən izahdır.
 *
 * Məzmun azərbaycancadır və ərəbcə UI-də də LTR qalır (`withScriptDirection`).
 */
object SalahGuideContent {

    /** «Bir dəfə» forması — İbn Abbasın dəstəmazı (№ 114). Üzdən başlayır. */
    val wuduOnce: List<TaharahStep> = listOf(
        TaharahStep(
            topic = SalahTopic.WUDU1_MOUTH, picture = TaharahPicture.MOUTH, times = 1,
            title = "Üz: ağız və burun",
            text = "Üz yuyulmağa ağızdan və burundan başlanır. Bir ovuc su ilə həm ağız yaxalanır, həm burna su çəkilib geri tökülür.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU1_FACE, picture = TaharahPicture.FACE, times = 1,
            title = "Üz: bütün üz",
            text = "Bir ovuc su alınır, iki ovuc birləşdirilir və bütün üz yuyulur.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU1_RARM, picture = TaharahPicture.ARM, times = 1, side = Side.RIGHT,
            title = "Sağ qol",
            text = "Bir ovuc su ilə sağ qol dirsəyə qədər yuyulur. Sağdan başlamaq Nəbinin sevdiyi işdir.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU1_LARM, picture = TaharahPicture.ARM, times = 1, side = Side.LEFT,
            title = "Sol qol",
            text = "Bir ovuc su ilə sol qol yuyulur.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU1_HEAD, picture = TaharahPicture.HEAD, times = 1,
            title = "Başa məsh",
            text = "Yaş əllər başın üstündən keçirilir.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU1_RFOOT, picture = TaharahPicture.FOOT, times = 1, side = Side.RIGHT,
            title = "Sağ ayaq",
            text = "Bir ovuc su az-az axıdılaraq sağ ayaq yuyulur. Dabanlar unudulmur.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU1_LFOOT, picture = TaharahPicture.FOOT, times = 1, side = Side.LEFT,
            title = "Sol ayaq",
            text = "Bir ovuc su ilə sol ayaq yuyulur. Dəstəmaz bitdi.",
        ),
    )

    /** «Tam forma» — Abdullah ibn Zeydin göstərdiyi dəstəmaz (№ 112). */
    val wuduFull: List<TaharahStep> = listOf(
        TaharahStep(
            topic = SalahTopic.WUDU3_HANDS, picture = TaharahPicture.HANDS, times = 2,
            title = "Əllər",
            text = "Su əllərə tökülür, əllər iki dəfə yuyulur.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU3_MOUTH, picture = TaharahPicture.MOUTH, times = 3,
            title = "Ağız və burun",
            text = "Ağız yaxalanır, burna su çəkilib geri tökülür, üç dəfə.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU3_FACE, picture = TaharahPicture.FACE, times = 3,
            title = "Üz",
            text = "Bütün üz üç dəfə yuyulur.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU3_ARMS, picture = TaharahPicture.ARM, times = 2,
            title = "Qollar, dirsəklə birlikdə",
            text = "Hər qol dirsəklə birlikdə iki dəfə yuyulur, sağdan başlanır.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU3_HEAD, picture = TaharahPicture.HEAD, times = 1,
            title = "Başa məsh",
            text = "Əllər alından başın arxasına aparılır, sonra başlanğıca qaytarılır.",
        ),
        TaharahStep(
            topic = SalahTopic.WUDU3_FEET, picture = TaharahPicture.FOOT, times = null,
            title = "Ayaqlar",
            text = "Ayaqlar yuyulur. Hədisdə say çəkilmir. Dabanlar unudulmur.",
        ),
    )

    fun wudu(form: WuduForm): List<TaharahStep> = when (form) {
        WuduForm.ONCE -> wuduOnce
        WuduForm.FULL -> wuduFull
    }

    /** Dəstəmaz: nə pozur, nə pozmur, sünnətlər. */
    val wuduBreaks: List<RuleRow> = listOf(
        RuleRow(SalahTopic.WUDU_DOUBT, RuleMark.NO, "Qaz", "Səsi eşidiləndə və ya qoxusu hiss olunanda. Yalnız hiss etmək kifayət deyil."),
        RuleRow(SalahTopic.WUDU_MADHY, RuleMark.NO, "Məzi", "Dəstəmaz alınır və yuyulur."),
    )
    val wuduKeeps: List<RuleRow> = listOf(
        RuleRow(SalahTopic.WUDU_MEAT, RuleMark.YES, "Ət yemək", "Nəbi qoyun əti yedi, sonra dəstəmaz almadan namaz qıldı."),
        RuleRow(SalahTopic.WUDU_MILK, RuleMark.YES, "Süd içmək", "Ağız yaxalanır."),
        RuleRow(SalahTopic.WUDU_FOOD, RuleMark.YES, "Ayaqyolundan sonra yemək", "Yemək üçün dəstəmaz lazım deyil."),
    )
    val wuduSunnah: List<RuleRow> = listOf(
        RuleRow(SalahTopic.WUDU_MISWAK, RuleMark.INFO, "Misvak", null),
        RuleRow(SalahTopic.WUDU_RIGHT, RuleMark.INFO, "Sağdan başlamaq", null),
        RuleRow(SalahTopic.WUDU_WATER, RuleMark.INFO, "Az su", "Bir mudd təqribən yarım litrdir (kitabın qeydi)."),
        RuleRow(SalahTopic.WUDU_MANY, RuleMark.INFO, "Bir dəstəmazla bir neçə namaz", null),
    )

    /** Qüsl — Aişə (№ 140, 141) və Ummu Sələmə (№ 145). */
    val ghuslWhen: List<RuleRow> = listOf(
        RuleRow(SalahTopic.GHUSL_WET, RuleMark.INFO, "Ehtilam", "Yuxuda ehtilam olan su görəndə."),
        RuleRow(SalahTopic.GHUSL_FRIDAY, RuleMark.INFO, "Cümə günü", null),
        RuleRow(SalahTopic.GHUSL_NOINZAL, RuleMark.INFO, "Yaxınlıq edib məni axmayanda", "Kitabdakı hədislər belədir."),
    )
    val ghuslSteps: List<TaharahStep> = listOf(
        TaharahStep(SalahTopic.GHUSL_HANDS, TaharahPicture.HANDS, null, title = "Əllər", text = "Əllər qaba salınmazdan əvvəl yuyulur."),
        TaharahStep(SalahTopic.GHUSL_PRIVATE, null, null, title = "Övrət yeri", text = "Övrət yeri yuyulur."),
        TaharahStep(SalahTopic.GHUSL_WUDU, null, null, title = "Namaz dəstəmazı", text = "Namaz üçün alınan dəstəmaz alınır."),
        TaharahStep(SalahTopic.GHUSL_HAIR, null, null, title = "Saçı isladmaq", text = "Su saçın dibinə çatdırılır."),
        TaharahStep(SalahTopic.GHUSL_THREE, TaharahPicture.POUR, 3, title = "Başa üç ovuc", text = "Başa üç ovuc su tökülür."),
        TaharahStep(SalahTopic.GHUSL_SIDES, TaharahPicture.SIDES, null, title = "Sağ, sol, orta", text = "Əvvəl başın sağ tərəfi, sonra sol tərəfi, sonra ortası."),
        TaharahStep(SalahTopic.GHUSL_BODY, TaharahPicture.BODY, null, title = "Bütün bədən", text = "Sonra bütün bədənə su tökülür."),
    )
    val ghuslExtra: List<RuleRow> = listOf(
        RuleRow(SalahTopic.GHUSL_WATER, RuleMark.INFO, "Suyun miqdarı", "Nəbi bir saa' su ilə qüsl alardı. Bir saa' iki litr yarım sudur (kitabın qeydi)."),
        RuleRow(SalahTopic.GHUSL_SLEEP, RuleMark.INFO, "Cünub yatmaq istəyəndə", "Övrət yeri yuyulur və namaz dəstəmazı alınır."),
        RuleRow(SalahTopic.GHUSL_HAYD, RuleMark.INFO, "Heyzdən sonra", null),
        RuleRow(SalahTopic.TAHARAH_BELIEVER, RuleMark.INFO, "Mömin nəcis olmur", null),
    )

    /** Təyəmmüm — Ammarın hədisindəki dörd hərəkət (№ 180). */
    val tayammumSteps: List<TaharahStep> = listOf(
        TaharahStep(SalahTopic.TAYAMMUM_STRIKE, TaharahPicture.STRIKE, null, title = "Ovucları yerə vur", text = "İki ovuc təmiz torpağa vurulur."),
        TaharahStep(SalahTopic.TAYAMMUM_BLOW, TaharahPicture.BLOW, null, title = "Üfür", text = "Ovuclara üfürülür."),
        TaharahStep(SalahTopic.TAYAMMUM_FACE, TaharahPicture.FACE_WIPE, null, title = "Üzə məsh", text = "Ovuclarla üzə məsh edilir."),
        TaharahStep(SalahTopic.TAYAMMUM_HANDS, TaharahPicture.WIPE_HANDS, null, title = "Biləklərə məsh", text = "Qolların bir hissəsinə, biləklərə məsh edilir."),
    )
    val tayammumWhen: List<RuleRow> = listOf(
        RuleRow(SalahTopic.TAYAMMUM_NOWATER, RuleMark.INFO, "Su olmayanda", null),
        RuleRow(SalahTopic.TAYAMMUM_JUNUB, RuleMark.INFO, "Cünub olan da edir", null),
        RuleRow(SalahTopic.TAYAMMUM_WALL, RuleMark.INFO, "Divarla da olur", null),
        RuleRow(SalahTopic.TAYAMMUM_EARTH, RuleMark.INFO, "Yer üzü təmizdir", null),
    )

    /** Ayaqyolu ədəbi (№ 91–99). Dua ayrıca zikr kimi göstərilir ([SalahTopic.TOILET_DUA]). */
    val toiletRules: List<RuleRow> = listOf(
        RuleRow(SalahTopic.TOILET_QIBLA, RuleMark.NO, "Üzü və ya arxası qibləyə", null),
        RuleRow(SalahTopic.TOILET_HOUSE, RuleMark.INFO, "Bina içində", "İbn Ömər damdan baxanda gördü."),
        RuleRow(SalahTopic.TOILET_RIGHT, RuleMark.NO, "Sağ əllə təmizlənmək", null),
        RuleRow(SalahTopic.TOILET_STONES, RuleMark.INFO, "Ən azı üç daş, tək sayda", null),
        RuleRow(SalahTopic.TOILET_BONES, RuleMark.NO, "Peyin və sümüklə", null),
        RuleRow(SalahTopic.TOILET_STILL, RuleMark.NO, "Durğun suya bövl", null),
        RuleRow(SalahTopic.TOILET_STANDING, RuleMark.INFO, "Ayaq üstə", "Kitabın qeydi: bunu qadağan edən hədislərin heç biri səhih deyil."),
    )

    /** Nəcasət necə təmizlənir. */
    val najasa: List<NajasaRow> = listOf(
        NajasaRow(SalahTopic.NAJASA_BABY, "Südəmər uşağın sidiyi", "su səpilir"),
        NajasaRow(SalahTopic.NAJASA_FLOOR, "Yerdəki sidik", "üstünə su tökülür"),
        NajasaRow(SalahTopic.NAJASA_DOG, "İtin yaladığı qab", "7 dəfə yuyulur, birincisi torpaqla", sevenWashes = true),
        NajasaRow(SalahTopic.NAJASA_BLOOD, "Paltardakı heyz qanı", "ovuşdurulur, yuyulur"),
        NajasaRow(SalahTopic.NAJASA_MANI, "Məni", "yuyulur və ya ovulur"),
    )

    /** Qadınlara aid hədislər — bölmənin sonunda və mövzuların içində qeyd kimi. */
    val hayd: List<RuleRow> = listOf(
        RuleRow(SalahTopic.HAYD_PRAYER, RuleMark.INFO, "Namaz və oruc", "Heyz günlərində namaz qılınmır, oruc tutulmur."),
        RuleRow(SalahTopic.HAYD_QADA, RuleMark.INFO, "Qəza yoxdur", "Qılınmayan namaz qəza edilmir."),
        RuleRow(SalahTopic.HAYD_ISTIHADA, RuleMark.INFO, "İstihazə", "Adət günləri bitəndə qüsl alınır və namaz qılınır."),
        RuleRow(SalahTopic.HAYD_DISCHARGE, RuleMark.INFO, "Rəngli ifrazat", "Adətdən kənar sarı və bulanıq ifrazat sayılmır."),
        RuleRow(SalahTopic.HAYD_EID, RuleMark.INFO, "Bayram namazı", "Bayram namazına çıxılır, namazdan uzaq durulur."),
    )

    /** Ekranlarda işlənən bütün mövzular — test bununla «yetim» açar olmadığını yoxlayır. */
    val usedTopics: Set<SalahTopic>
        get() = buildSet {
            (wuduOnce + wuduFull + ghuslSteps + tayammumSteps).forEach { add(it.topic) }
            (wuduBreaks + wuduKeeps + wuduSunnah + ghuslWhen + ghuslExtra + tayammumWhen + toiletRules + hayd).forEach { add(it.topic) }
            najasa.forEach { add(it.topic) }
            addAll(listOf(SalahTopic.WUDU_FORMS, SalahTopic.TAYAMMUM_ENOUGH, SalahTopic.KHUFF_WIPE, SalahTopic.KHUFF_TIME, SalahTopic.TOILET_DUA))
            addAll(SalahPrayerContent.usedTopics)
        }
}

enum class WuduForm { ONCE, FULL }

enum class Side(val label: String) { RIGHT("sağ"), LEFT("sol") }

/** Təharət illüstrasiyaları — çertyojlar `compose/screens/salah/art`-dadır. */
enum class TaharahPicture { FACE, MOUTH, HANDS, ARM, HEAD, FOOT, POUR, SIDES, BODY, STRIKE, BLOW, FACE_WIPE, WIPE_HANDS, KHUFF, QIBLA }

/**
 * Bir addım. [times] — neçə dəfə yuyulur (`null` → hədisdə say yoxdur, ekranda «—»).
 * [picture] yoxdursa addım yalnız mətnlə göstərilir (övrət yeri kimi).
 */
data class TaharahStep(
    val topic: SalahTopic,
    val picture: TaharahPicture?,
    val times: Int?,
    val side: Side? = null,
    val title: String,
    val text: String,
)

enum class RuleMark { YES, NO, INFO }

/** Qayda sətri: başlıq və qısa izah koddan, hədis çıxarışı [topic] üzrə bazadan. */
data class RuleRow(val topic: SalahTopic, val mark: RuleMark, val title: String, val text: String?)

data class NajasaRow(val topic: SalahTopic, val title: String, val how: String, val sevenWashes: Boolean = false)
