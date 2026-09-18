package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
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
 * Tərcümədəki mötərizəli **tərcüməçi qeydləri** üçün rəng.
 *
 * Oxucudakı dəyərin eynisidir (`TextAnnotator` → `Color(0xFFE53935)`), qəsdən: istifadəçi ekranda
 * hansı hissəni tərcüməçi əlavəsi kimi görürsə, paylaşdığı şəkildə də eyni rəngdə görməlidir.
 * Temadan çıxarılmır, çünki palitranın vurğu rəngi əl ilə seçilmiş yazı rənginə yaxınlaşa bilir
 * (bax [resolvePalette]-in birinci halı) və qeyd mətndən **seçilməz** olardı.
 */
val ShareTranslatorNoteColor: Color = Color(0xFFE53935)

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
    /**
     * Qaynaq sətrinin (və onun üstündəki qısa xəttin) **öz** düzülüşü.
     *
     * ⚠️ Əvvəl qaynaq [align]-ı izləyirdi, yəni tərcüməni sola yaslayanda ünvan da sola düşürdü.
     * Poster quruluşunda isə imza sətri adətən mətnin düzülüşündən asılı olmur — ona görə ayrıldı.
     */
    val referenceAlign: ShareImageAlign,
    /** Loqo sətrinin (nişan + tətbiq adı) miqyası. */
    val brandingScale: Float,
    /** Loqo/QR sətrinin **öz** düzülüşü — [referenceAlign] ilə eyni səbəb. */
    val brandingAlign: ShareImageAlign,
    val showArabic: Boolean,
    val showTranslation: Boolean,
    /** Qeyd bloku. Məzmunda qeyd yoxdursa bu bayraq nəzərə alınmır. */
    val showNote: Boolean,
    val showReference: Boolean,
    val showBranding: Boolean,
    /** Mağaza QR-ləri — namaz cədvəli kartındakı ilə eyni cüt (App Store + Play). */
    val showQr: Boolean,
    /**
     * İstifadəçinin əl ilə seçdiyi yazı rəngi. `null` = **avtomatik**: rəng temadan, fon şəkli
     * varsa isə onun parlaqlığından çıxarılır (bax [resolvePalette]).
     */
    val textColor: Color? = null,
    /**
     * [customBackground]-un ölçülmüş orta parlaqlığı (0…1) və ya `null` — hələ ölçülməyibsə, ya da
     * öz şəkli seçilməyibsə.
     *
     * Paket temaları burada **qəsdən** iştirak etmir: onların mətn rəngləri əl ilə uyğunlaşdırılıb,
     * avtomatik hesablama isə onları yalnız pisləşdirərdi.
     */
    val backgroundLuminance: Float? = null,
)

/**
 * Kartın son mətn rəngləri. [ShareImageTheme]-in eyni adlı sahələrini əvəz edir: tema artıq yalnız
 * **toxumdur**, son sözü [ShareImageStyle.resolvePalette] deyir.
 */
data class ShareCardPalette(
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
)

/**
 * Fonun **xətti** parlaqlıq həddi: bundan yuxarısı açıq fon sayılır və tünd yazı seçilir.
 *
 * ⚠️ `Color.luminance()` sRGB deyil, xətti dəyər qaytarır — gözə «orta boz» görünən rəng burada
 * 0.5 deyil, ~0.2-dir. Ona görə hədd 0.5 yox, 0.22-dir; 0.5 yazsaq demək olar hər şəkil «tünd»
 * sayılar və ağ yazı ağ divarın üstünə düşərdi.
 */
private const val ShareAutoTextThreshold = 0.22f

/** Avtomatik rejimin açıq (tünd fon üçün) və tünd (açıq fon üçün) yazı dəstləri. */
private val AutoTextOnDark = Color(0xFFF9FBFB)
private val AutoSecondaryOnDark = Color(0xFFDFE6E6)
private val AutoTextOnLight = Color(0xFF14181A)
private val AutoSecondaryOnLight = Color(0xFF3D4649)

/**
 * Kartın çəkəcəyi rəngləri hesablayır.
 *
 * Üç hal var, bu sıra ilə:
 *  1. **Əl ilə seçilmiş rəng** ([ShareImageStyle.textColor]) — hər şeyə şamil olunur (vurğu
 *     rənginə də), yoxsa istifadəçi «yazı rəngini» dəyişəndə istinad sətri və ayırıcı köhnə
 *     temanın qızılında qalıb yamaq kimi görünürdü.
 *  2. **Ölçülmüş fon şəkli** — parlaqlıq qaraltma ilə birlikdə hesablanır və açıq/tünd dəst seçilir.
 *     Qaraltma da nəzərə alınmasa, günəşli şəkil 0.7 qaraltma altında hələ də «açıq» sayılıb tünd
 *     yazı verərdi.
 *  3. **Qalan hər şey** — temanın öz rəngləri.
 */
fun ShareImageStyle.resolvePalette(): ShareCardPalette {
    val manual = textColor
    if (manual != null) {
        return ShareCardPalette(
            text = manual,
            secondaryText = manual.copy(alpha = 0.80f),
            accent = manual.copy(alpha = 0.92f),
        )
    }

    val luminance = backgroundLuminance
        ?: return ShareCardPalette(theme.text, theme.secondaryText, theme.accent)

    // Qaraltma qara örtükdür, yəni parlaqlığı sadəcə azaldır. Qradiyentin üç dayağının orta alfası
    // praktiki olaraq `scrim`-in özüdür (s, s-0.12, s+0.1), ona görə tək əmsal kifayətdir.
    val effective = luminance * (1f - scrim).coerceIn(0f, 1f)

    return if (effective > ShareAutoTextThreshold) {
        ShareCardPalette(
            text = AutoTextOnLight,
            secondaryText = AutoSecondaryOnLight,
            // Temaların vurğuları tünd fon üçün seçilib (qızılı, açıq turkuaz); açıq şəklin üstündə
            // onlar itir, ona görə qaraya doğru qarışdırılır.
            accent = lerp(theme.accent, Color.Black, 0.42f),
        )
    } else {
        ShareCardPalette(
            text = AutoTextOnDark,
            secondaryText = AutoSecondaryOnDark,
            accent = lerp(theme.accent, Color.White, 0.12f),
        )
    }
}

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
