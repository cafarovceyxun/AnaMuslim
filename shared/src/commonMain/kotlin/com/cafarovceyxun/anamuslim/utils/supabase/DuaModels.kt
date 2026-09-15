package com.cafarovceyxun.anamuslim.utils.supabase

import kotlinx.serialization.Serializable

/**
 * `dua.source_type` / `asma_evidence.source_type` dəyərləri — bazadakı CHECK ilə eyni sətirlər.
 *
 * Enum deyil: sütun mətn saxlayır və gələcəkdə əlavə oluna biləcək növ köhnə tətbiqi
 * `SerializationException` ilə çökdürərdi. Tanınmayan növ sadəcə göstərilmir.
 */
object DuaSourceType {
    const val HADITH = "hadith"
    const val QURAN = "quran"
}

/**
 * Bir çıxarışın **mənbəyi** — hədis, yoxsa Quran ayəsi.
 *
 * [Dua] və [AsmaEvidence] eyni formadadır (eyni sütunlar, eyni CHECK), ona görə mənbə ilə işləyən
 * hər şey — qaynaq vərəqi, vurğu, «oxu» düyməsi — bu interfeys üzərindən yazılır və iki dəfə
 * təkrarlanmır.
 */
interface DuaSourceRef {
    val source_type: String
    val hadith_id: Long?
    val chapter_no: Int?
    val verse_no: Int?
    val verse_end: Int?

    /** Seçilmiş ərəbcə hissə — mənbədə məhz bu parça sarı ilə işarələnir. */
    val text_ar: String

    /** Seçilmiş tərcümə hissəsi. */
    val text_az: String

    /**
     * Seçilmiş **oxunuş** — latın hərfləri ilə.
     *
     * İnterfeysdədir, çünki qaynaq vərəqi vurğunu bütün saxlanmış parçalar üzrə qurur: bu toplusunda
     * oxunuş rəvayətin içindəki `{…}`-dan, mənası isə qeyddən götürülür — yəni latın mətnin hansı
     * blokda olduğu əvvəlcədən bilinmir.
     */
    val transliteration: String?

    val source: String?
    val note: String?

    val isHadith: Boolean get() = source_type == DuaSourceType.HADITH
    val isQuran: Boolean get() = source_type == DuaSourceType.QURAN

    /**
     * Ayə aralığı — tək ayədə bir elementli. Hədis mənbəyində boşdur.
     *
     * Aralıq [DailyContent.verseNumbers] ilə eyni qaydadadır: `verse_end` null olanda yalnız
     * başlanğıc ayə, tərs aralıq (baza CHECK-i buraxmır) isə boş siyahı.
     */
    val verseNumbers: List<Int>
        get() {
            val start = verse_no ?: return emptyList()
            val end = verse_end ?: return listOf(start)
            if (end < start) return listOf(start)
            return (start..end).toList()
        }
}

// ---------------------------------------------------------------------------- dua

/**
 * Dua başlığı (`dua_category`) — «Səhər duaları», «Yemək duası» kimi.
 *
 * Duanın özü deyil, yalnız qrupdur: ekranda bu başlıqlar siyahılanır, içinə girəndə həmin
 * başlığa bağlanmış [Dua] sətirləri kitab kimi vərəqlənir.
 */
@Serializable
data class DuaCategory(
    val slug: String,
    val name: String,
    val name_ar: String? = null,
    val description: String? = null,
    val sort_no: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
)

/**
 * Alt başlıq (`dua_subcategory`) — «Səhər duaları → Yuxudan duranda» kimi.
 *
 * **Məcburi deyil**: dua birbaşa başlığın altında da dura bilər (hədis ağacındakı `sub_chapter_slug`
 * ilə eyni qayda). Alt başlıq silinəndə içindəki dualar itmir, başlığın birbaşa altına qalxır —
 * bazada `on delete set null`.
 */
@Serializable
data class DuaSubcategory(
    val slug: String,
    val category_slug: String,
    val name: String,
    val name_ar: String? = null,
    val sort_no: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
)

/**
 * Bir dua — mənbədən **seçilmiş** ərəbcə və tərcümə hissəsi, başlığa bağlanmış.
 *
 * Mətn qəsdən kopyalanır (mənbəyə istinad kifayət etmir): hədis sonradan redaktə olunsa dua öz
 * mətni ilə qalır, qaynaq vərəqi isə həmin mətni tam mətnin içində axtarıb işarələyir — tapmasa
 * sadəcə vurğusuz göstərir.
 *
 * ⚠️ `id` **GENERATED ALWAYS**-dır: yazarkən `null` qalmalıdır. supabase-kt-nin serializatoru
 * `explicitNulls = false` ilə işlədiyi üçün null sahələr JSON-a heç düşmür, yəni `id = null`
 * insert-ində sütun ümumiyyətlə göndərilmir. `created_by` isə **modeldə yoxdur**: onu baza
 * `default auth.uid()` ilə özü doldurur və RLS elə həmin dəyərə baxır.
 */
@Serializable
data class Dua(
    val id: Long? = null,
    val category_slug: String,
    /** Alt başlıq — `null` olanda dua birbaşa başlığın altındadır. */
    val subcategory_slug: String? = null,
    override val source_type: String,
    override val hadith_id: Long? = null,
    override val chapter_no: Int? = null,
    override val verse_no: Int? = null,
    override val verse_end: Int? = null,
    override val text_ar: String,
    override val text_az: String,
    /**
     * Duanın **oxunuşu** — latın hərfləri ilə.
     *
     * Ayrı sütundur, çünki mənbədə də ayrı yerdədir: hədisin `text_az`-ı rəvayətdir və dua orada
     * `{...}` içində oxunuş kimi verilir, tərcüməsi isə `note`-dadır. İkisini bir sahəyə yığmaq
     * ekranda hansının nə olduğunu itirərdi.
     */
    override val transliteration: String? = null,
    override val note: String? = null,
    override val source: String? = null,
    /**
     * Zikrin təkrar sayı — «33 dəfə» kimi. `null` = say göstərilmir (adi dua).
     *
     * Bölmənin adı «Dua və zikr» olduğu üçün lazımdır: zikrin sayı məzmunun bir hissəsidir, qeyd
     * sətrinə yazılsa siyahıda görünməzdi.
     */
    val repeat_count: Int? = null,
    val sort_no: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
) : DuaSourceRef

// --------------------------------------------------------------------------- asma

/**
 * Əsmaül Hüsnədən bir ad (`asma_name`).
 *
 * Siyahı sabitdir (99 sətir, miqrasiya ilə yazılıb) və yalnız admin dəyişə bilir — redaktor ada
 * **dəlil** əlavə edir ([AsmaEvidence]), adın özünü yox.
 */
@Serializable
data class AsmaName(
    val no: Int,
    val name_ar: String,
    val transliteration: String,
    /** Adın qısa mənası — ekranda adın altında duran sətir. */
    val meaning: String,
    /** Uzun izah; hələ doldurulmayıb, ona görə null ola bilər. */
    val description: String? = null,
    /**
     * Ad siyahıda göstərilsinmi.
     *
     * Silmək əvəzinə bayraq, çünki `asma_evidence.name_no` **CASCADE**-dir: sətir silinsə ona
     * bağlanmış bütün dəlillər də gedərdi. Bağlı ad yalnız girişi olan istifadəçiyə görünür
     * (solğun, «gizli» nişanı ilə) — adi istifadəçi onu heç görmür.
     */
    val is_visible: Boolean = true,
    val updated_at: String? = null,
)

/** Bir ada dəlil olan ayə/hədis çıxarışı (`asma_evidence`). Forması [Dua] ilə eynidir. */
@Serializable
data class AsmaEvidence(
    val id: Long? = null,
    val name_no: Int,
    override val source_type: String,
    override val hadith_id: Long? = null,
    override val chapter_no: Int? = null,
    override val verse_no: Int? = null,
    override val verse_end: Int? = null,
    override val text_ar: String,
    override val text_az: String,
    /** Bax [Dua.transliteration]. */
    override val transliteration: String? = null,
    override val note: String? = null,
    override val source: String? = null,
    val sort_no: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
) : DuaSourceRef

/**
 * Başlıq adından `dua_category.slug` qurur — bazadakı `^[a-z0-9][a-z0-9_-]{0,79}$` şəklinə uyğun.
 *
 * Azərbaycan hərfləri ASCII qarşılığına çevrilir (`ə→e`, `ş→s` …), qalan hər şey defisə düşür.
 * Ərəbcə və ya tamamilə qeyri-latın ad veriləndə nəticə boş qalır — çağıran tərəf o halda
 * [fallbackSlug]-a keçir.
 */
fun duaCategorySlug(name: String): String {
    val builder = StringBuilder(name.length)

    for (char in name.lowercase()) {
        val mapped = when (char) {
            'ə' -> "e"
            'ı' -> "i"
            'ö' -> "o"
            'ü' -> "u"
            'ğ' -> "g"
            'ş' -> "s"
            'ç' -> "c"
            'â', 'à', 'á' -> "a"
            'î', 'í' -> "i"
            'û', 'ú' -> "u"
            else -> when {
                char in 'a'..'z' || char in '0'..'9' -> char.toString()
                else -> "-"
            }
        }
        builder.append(mapped)
    }

    return builder.toString()
        .split('-')
        .filter { it.isNotEmpty() }
        .joinToString("-")
        .take(80)
        .trim('-')
        .let { if (it.isEmpty() || !it.first().isLetterOrDigit()) "" else it }
}

/** Ad latın hərfi daşımayanda işlədilən ehtiyat slug — vaxt damğası ilə unikal. */
fun fallbackSlug(seed: Long): String = "dua-$seed"
