package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.quran_wallpaper
import org.jetbrains.compose.resources.DrawableResource

/**
 * Paylaşılan şəklin kətan ölçüsü — **piksellə**, dp ilə yox.
 *
 * Kart [ShareImageCard] daxilində `Density(1f, 1f)` altında qurulur, yəni `1.dp == 1.sp == 1px`.
 * Beləliklə nəticə şəkli cihazın ekran sıxlığından asılı olmur: hər telefonda eyni 1080px enli fayl
 * çıxır. (Əvvəllər kart cihaz sıxlığında 720.dp ölçüsündə qurulur və kiçik önizləmə qutusuna
 * **kəsilirdi** — ona görə şrift xətkeşlərinin defolt dəyərləri 5–10 kimi mənasız kiçik idi.)
 */
enum class ShareImageRatio(val label: String, val widthPx: Int, val heightPx: Int) {
    /** Instagram/WhatsApp status — tam ekran şaquli. */
    Story("9:16", 1080, 1920),

    /** Lent üçün şaquli post. */
    Portrait("4:5", 1080, 1350),

    /** Kvadrat post. */
    Square("1:1", 1080, 1080),
}

/**
 * Kartın rəng dəsti. [photo] verilibsə fon şəkli qradiyentin üstünə çəkilir və qaraldılır ki, mətn
 * hər halda oxunaqlı qalsın (şəkil aktivləri kətandan kiçikdir — güclü qaraltma həm kontrastı, həm
 * də böyütmə artefaktlarını gizlədir).
 *
 * ℹ️ [scrim] burada **başlanğıc dəyərdir**: redaktor onu tema seçiləndə toxum kimi götürür, sonra
 * istifadəçi xətkeşlə dəyişə bilir ([ShareImageStyle.scrim]). Kart yalnız `ShareImageStyle`-dakı
 * dəyəri çəkir. Namaz cədvəli kartı isə hələ də birbaşa bunu oxuyur.
 */
data class ShareImageTheme(
    val gradient: List<Color>,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val photo: DrawableResource? = null,
    val scrim: Float = 0f,
)

/** Redaktorun fon seçimləri, göründükləri sıra ilə. */
val ShareImageThemes: List<ShareImageTheme> = listOf(
    // Gecə — mövcud kartın davamı (tünd + turkuaz vurğu).
    ShareImageTheme(
        gradient = listOf(Color(0xFF14181A), Color(0xFF05070A)),
        text = Color(0xFFF7FAFA),
        secondaryText = Color(0xFFB9C4C6),
        accent = Color(0xFF4DB6AC),
    ),
    // Zümrüd — tünd yaşıl + qızılı.
    ShareImageTheme(
        gradient = listOf(Color(0xFF0A3327), Color(0xFF04180F)),
        text = Color(0xFFF4EFE2),
        secondaryText = Color(0xFFC9D6CC),
        accent = Color(0xFFD2AE6D),
    ),
    // Qızılı gecə.
    ShareImageTheme(
        gradient = listOf(Color(0xFF1B1710), Color(0xFF0B0906)),
        text = Color(0xFFF8EED8),
        secondaryText = Color(0xFFCDBFA3),
        accent = Color(0xFFD4AF6A),
    ),
    // Kağız — açıq, çap hissi.
    ShareImageTheme(
        gradient = listOf(Color(0xFFFBF7EF), Color(0xFFEFE5D3)),
        text = Color(0xFF241D14),
        secondaryText = Color(0xFF6A5B45),
        accent = Color(0xFF9A7434),
    ),
    // Ağ — neytral, mesajlaşma tətbiqlərində ən yaxşı oxunan.
    ShareImageTheme(
        gradient = listOf(Color(0xFFFFFFFF), Color(0xFFF1F3F2)),
        text = Color(0xFF111513),
        secondaryText = Color(0xFF5B6663),
        accent = Color(0xFF2E7D6B),
    ),
    // Fotolu.
    ShareImageTheme(
        gradient = listOf(Color(0xFF06090A), Color(0xFF06090A)),
        text = Color(0xFFFFFFFF),
        secondaryText = Color(0xFFDBD5C8),
        accent = Color(0xFFE0C48C),
        photo = Res.drawable.quran_wallpaper,
        scrim = 0.74f,
    ),
)

/** Mətnin kartdakı üfüqi düzülüşü. Kart LTR kompozisiyada qurulur, yəni sol/sağ hərfidir. */
enum class ShareImageAlign { Left, Center, Right }

/**
 * Tərcümə mətninin yazı stili.
 *
 * ⚠️ Bunlar **paket şrifti deyil** — layihədə latın üzü yoxdur (bütün 7 şrift ərəb/Quran üçündür),
 * ona görə Compose-un ümumi ailələrinə bağlanır və üzü platforma seçir. Nəticə Android ilə iOS
 * arasında bir qədər fərqli görünə bilər; paylaşılan şəkil isə **yaradıldığı cihazda** çəkildiyi
 * üçün istifadəçi gördüyünü paylaşır.
 */
enum class ShareTextFamily { Sans, Serif, Mono }

/**
 * Redaktorun bütün tənzimləri. [textScale] və [margin] mütləq ölçü yox, **əmsaldır**: mətn həmişə
 * kətana avtomatik sığdırılır, xətkeşlər isə nəticəni miqyaslayır və kənar məsafəni dəyişir. Bu,
 * uzun ayələrdə mətnin kəsilməsini strukturca imkansız edir.
 */
data class ShareImageStyle(
    val theme: ShareImageTheme,
    val ratio: ShareImageRatio,
    /**
     * Ərəb mətninin miqyası. [translationScale]-dən **ayrıdır**: ərəb xətti eyni piksel hündürlüyündə
     * latın mətnindən kiçik oxunur, ona görə tək xətkeş həmişə birini qurban verirdi.
     */
    val arabicScale: Float,
    /** Tərcümənin miqyası; köməkçi sətirlər (üst etiket, mənbə) də bununla miqyaslanır. */
    val translationScale: Float,
    val margin: Float,
    /** Tərcümənin (və köməkçi sətirlərin) düzülüşü. */
    val align: ShareImageAlign,
    /**
     * Ərəb mətninin **öz** düzülüşü.
     *
     * ⚠️ Əvvəl bu, [align]-ın **güzgüsü** kimi hesablanırdı (sol → sağ), çünki ərəbcənin oxu kənarı
     * sağdır. İndi istifadəçi birbaşa seçir, yəni burada güzgüləmə **yoxdur**: «sağ» sağ deməkdir.
     * Redaktor default olaraq köhnə güzgü davranışını verir, sonra istifadəçi ayıra bilir.
     */
    val arabicAlign: ShareImageAlign,
    /**
     * İstifadəçinin qalereyadan seçdiyi fon. Verildikdə [theme]-in öz fonunu (rəng və ya paket
     * şəkli) əvəz edir; mətn rəngləri isə [theme]-dən gəlməyə davam edir, ona görə şəkil seçildikdən
     * sonra da fərqli palitraya keçmək olur.
     */
    val customBackground: ImageBitmap? = null,
    /**
     * Fon **şəklinin** üstündəki qaraltma, 0..1.
     *
     * [ShareImageTheme.scrim]-i əvəz edir: tema dəyəri artıq yalnız başlanğıc toxumdur, son söz
     * istifadəçidədir. Səbəb: qalereyadan seçilən şəkillərin parlaqlığı çox fərqlidir və sabit
     * qaraltma ilə mətn ya oxunmurdu, ya da şəkil lazımsız yerə qaralırdı.
     *
     * Fon şəkli yoxdursa (düz qradiyent) dəyər çəkilməyə təsir etmir.
     *
     * ⚠️ Default **verilmir** — yeni fon yolu əlavə edən adam qaraltmanı da qərara almalıdır.
     */
    val scrim: Float,
    /** Tərcümənin yazı ailəsi — bax [ShareTextFamily]. Ərəb mətninə təsir etmir. */
    val translationFamily: ShareTextFamily,
    /** Tərcümə qalın yazılsınmı. */
    val translationBold: Boolean,
    /** Qeydin miqyası (hədisin `note`-u / ayənin əlfəcin qeydi). */
    val noteScale: Float,
    /** Qeydin düzülüşü — qeyd çox vaxt izahat olduğu üçün mətndən ayrı düzülməsi istənir. */
    val noteAlign: ShareImageAlign,
    /** Loqo sətrinin (nişan + tətbiq adı) miqyası. */
    val brandingScale: Float,
    val showArabic: Boolean,
    val showTranslation: Boolean,
    /** Qeyd bloku. Məzmunda qeyd yoxdursa bu bayraq nəzərə alınmır. */
    val showNote: Boolean,
    val showReference: Boolean,
    val showBranding: Boolean,
    /** Mağaza QR-ləri — namaz cədvəli kartındakı ilə eyni cüt (App Store + Play). */
    val showQr: Boolean,
)

/**
 * Bir ayə (və ya hədis) — ərəbcəsi və tərcüməsi. Boş sətir = həmin blok yoxdur.
 *
 * Kart seqmentlər siyahısı ilə işləyir ki, vərəqdəki «hər ayəni tərcüməsi ilə cütlə» rejimi şəkildə
 * də görünsün: cütlənmiş rejimdə hər ayə öz seqmentidir, bloklu rejimdə isə bütün aralıq tək
 * seqmentdə birləşdirilir.
 */
data class ShareImageSegment(
    val arabic: String,
    val translation: String,
)

/** Kartda göstəriləcək məzmun. */
data class ShareImageContent(
    val segments: List<ShareImageSegment>,
    /** Alt sətir: «Fatihə 1:1-7» və ya hədisin qaynağı. */
    val reference: String,
    /** Ən üstdəki kiçik etiket: «Hədis №12». */
    val eyebrow: String? = null,
    /**
     * Qeyd — hədisin `note` sahəsi və ya ayənin əlfəcin qeydi. Mətn paylaşımında onsuz da vardı,
     * şəkil paylaşımında isə heç vaxt görünmürdü.
     *
     * `null`/boş = qeyd yoxdur; belə olanda kartda blok çəkilmir və redaktorda «Qeyd» çipi çıxmır.
     */
    val note: String? = null,
)
