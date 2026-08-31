package com.cafarovceyxun.anamuslim.utils.prayer.location

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.PI
import kotlin.math.cos

/** Seçilə bilən yer. [point] namaz hesablamasına birbaşa verilir. */
data class City(
    val name: String,
    val country: String,
    val point: GeoPoint,
)

/**
 * Oflayn şəhər siyahısı — GPS icazəsi verilmədikdə və ya mövqe alınmadıqda yeganə yoldur.
 *
 * ⚠️ Siyahının **oflayn** olması dizayn qərarıdır, ölçü güzəşti deyil. Şəhər seçimi məhz GPS
 * işləmədiyi an lazım olur: təyyarə rejimi, SIM-siz telefon, rədd edilmiş icazə — bunlar eyni
 * zamanda internetin də olmadığı anlardır. Serverdən axtarış bu ssenarinin özünü sındırardı.
 *
 * Məlumat: GeoNames `cities15000` (CC BY 4.0) → bütün Azərbaycan (65) + bütün paytaxtlar +
 * region (TR/RU/GE/IR/KZ/UZ/TM/TJ/KG/AM ≥ 100k) + dünya ≥ 200k = **3521 şəhər**, 159 KB.
 * Hündürlük sütunu da var (GeoNames `elevation`, yoxdursa `dem`) — Axşam vaxtı üçün lazımdır.
 * Generator: `tools/prayer/gen_cities.py`.
 *
 * Sətirlər **əhaliyə görə azalan** sıradadır, ona görə ayrıca əhali sütunu yoxdur: nəticələr
 * fayl sırasını saxladığı üçün böyük şəhərlər özlüyündə əvvəldə gəlir.
 *
 * ### Adlar və axtarış aliasları
 *
 * **Göstərilən ad** Azərbaycan şəhərləri üçün əl ilə yoxlanılıb (`tools/prayer/az_names.py`),
 * qalanları üçün GeoNames-in beynəlxalq adıdır.
 *
 * **Qatlanmış adlar** isə əlavə olaraq `alternateNamesV2`-dən gələn **az/tr/ru** adlarını da
 * daşıyır: `Moscow` → `moscow|moskova|moskva`, `London` → `london|londra`,
 * `New York City` → `new york city|nyu york`. Rus adı da latına qatlandığı üçün **kiril sorğusu
 * işləyir** (`Москва` → `moskva`) və ayrıca kiril sütunu saxlamağa ehtiyac qalmır.
 *
 * ⚠️ GeoNames-in `az` sahəsi **göstərmək üçün işlədilmir**, yalnız alias kimi. 65 Azərbaycan
 * şəhərində tutuşduruldu: 57 uyğun, 7 fərqli və hər yeddisində GeoNames pis — «Şamxor» (sovet adı),
 * əskik `ə`/`ı` hərfləri, və Yevlax üçün **«Gəncə»**. Səhv alias ən pis halda artıq nəticə verir;
 * səhv **ad** isə istifadəçini çaşdırır. Generator həmçinin başqa şəhərin əsas adı ilə toqquşan
 * aliası atır, ona görə «gence» sorğusuna yalnız Gəncə cavab verir.
 */
class CityCatalog private constructor(private val entries: List<Entry>) {

    private class Entry(val city: City, val folds: List<String>)

    val size: Int get() = entries.size

    /**
     * Sorğuya uyğun şəhərlər — dörd pillə: tam uyğunluq → prefiks → söz başlanğıcı → daxilində.
     * Hər pillədə fayl sırası (əhali) qorunur.
     */
    fun search(query: String, limit: Int = DEFAULT_LIMIT): List<City> {
        val needle = fold(query)
        if (needle.isEmpty() || limit <= 0) return emptyList()

        val exact = ArrayList<City>()
        val prefix = ArrayList<City>()
        val wordStart = ArrayList<City>()
        val contains = ArrayList<City>()

        for (entry in entries) {
            when (entry.rank(needle)) {
                RANK_EXACT -> exact
                RANK_PREFIX -> prefix
                RANK_WORD -> wordStart
                RANK_CONTAINS -> contains
                else -> null
            }?.add(entry.city)

            // Ən yaxşı pillə onsuz da dolubsa axtarışı davam etdirmək mənasızdır.
            if (exact.size >= limit) break
        }

        return (exact + prefix + wordStart + contains).take(limit)
    }

    /**
     * [point]-ə ən yaxın şəhər — GPS koordinatına ad vermək üçün.
     *
     * Məsafə ekvidistant yaxınlaşma ilə ölçülür (uzunluq `cos(enlik)` ilə daralır); ən yaxını
     * seçmək üçün bu kifayətdir, haversine-in dəqiqliyi burada heç nəyi dəyişmir.
     */
    fun nearest(point: GeoPoint): City? {
        if (!point.isValid) return null

        val scale = cos(point.latitude * PI / 180.0)
        var best: City? = null
        var bestDistance = Double.MAX_VALUE

        for (entry in entries) {
            val dLat = entry.city.point.latitude - point.latitude
            val dLng = (entry.city.point.longitude - point.longitude) * scale
            val distance = dLat * dLat + dLng * dLng

            if (distance < bestDistance) {
                bestDistance = distance
                best = entry.city
            }
        }

        return best
    }

    private fun Entry.rank(needle: String): Int {
        var best = RANK_NONE

        for (candidate in folds) {
            val rank = when {
                candidate == needle -> RANK_EXACT
                candidate.startsWith(needle) -> RANK_PREFIX
                candidate.contains(" $needle") -> RANK_WORD
                candidate.contains(needle) -> RANK_CONTAINS
                else -> RANK_NONE
            }
            if (rank != RANK_NONE && rank < best) best = rank
            if (best == RANK_EXACT) break
        }

        return best
    }

    companion object {
        const val RESOURCE_PATH = "files/prayer/cities.tsv"
        const val DEFAULT_LIMIT = 20

        private const val RANK_EXACT = 0
        private const val RANK_PREFIX = 1
        private const val RANK_WORD = 2
        private const val RANK_CONTAINS = 3
        private const val RANK_NONE = Int.MAX_VALUE

        private const val COLUMN_COUNT = 6

        /**
         * `ad<TAB>qatlanmış adlar (|)<TAB>ölkə<TAB>enlik<TAB>uzunluq<TAB>hündürlük(m)`.
         *
         * Pozuq sətir **atılır**, bütün faylı yıxmır: fayl generasiya olunur, amma tək bir səhv
         * sətrə görə şəhər seçimini tamamilə itirmək mütənasib deyil.
         */
        fun parse(text: String): CityCatalog {
            val entries = ArrayList<Entry>()

            for (line in text.lineSequence()) {
                if (line.isEmpty() || line.startsWith('#')) continue

                val columns = line.split('\t')
                if (columns.size < COLUMN_COUNT) continue

                val latitude = columns[3].toDoubleOrNull() ?: continue
                val longitude = columns[4].toDoubleOrNull() ?: continue
                // Hündürlük pozuqdursa dəniz səviyyəsi — bu, yalnız Axşamı bir neçə dəqiqə
                // erkənləşdirir, sətri tamamilə atmaqdan yaxşıdır.
                val elevation = columns[5].toDoubleOrNull()?.coerceIn(0.0, 9000.0) ?: 0.0
                val point = GeoPoint(latitude, longitude, elevation)
                if (!point.isValid) continue

                val name = columns[0]
                if (name.isEmpty()) continue

                val folds = columns[1].split('|').filter { it.isNotEmpty() }
                if (folds.isEmpty()) continue

                entries += Entry(City(name, columns[2], point), folds)
            }

            return CityCatalog(entries)
        }

        /**
         * Diakritikanı ataraq axtarışa hazırlayır — `unaccent` ekvivalenti, sıfır asılılıqla.
         *
         * ⚠️ Aşağıdakı cədvəl `tools/prayer/gen_cities.py`-dakı `fold()` funksiyasından
         * **generasiya olunub**. Fayldakı qatlanmış adlar həmin Python funksiyası ilə yazılır,
         * sorğu isə burada qatlanır — iki tərəf sürüşsə axtarış səssizcə boş qayıdar.
         * Cədvəli əl ilə redaktə etmə; generatoru dəyişib yenidən çıxart.
         *
         * ⚠️ `lowercase()` **kifayət etmir**: Kotlin-də `"İstanbul".lowercase()` locale-dan asılı
         * olmayaraq `i` + birləşən nöqtə (İKİ kod nöqtəsi) verir, ona görə `İ` cədvəldə açıq şəkildə
         * `i`-yə köçürülür.
         */
        fun fold(text: String): String {
            val builder = StringBuilder(text.length)

            for (char in text) {
                val multi = FOLD_MULTI[char]
                if (multi != null) {
                    builder.append(multi)
                    continue
                }

                val index = FOLD_FROM.indexOf(char)
                if (index >= 0) {
                    builder.append(FOLD_TO[index])
                    continue
                }

                val lower = char.lowercaseChar()
                builder.append(if (lower.isLetterOrDigit() && lower.code < 128) lower else ' ')
            }

            return builder.toString().trim().replace(WHITESPACE, " ")
        }

        private val WHITESPACE = Regex("\\s+")

        private const val FOLD_FROM =
            "ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝàáâãäåçèéêëìíîïðñòóôõöøùúûüýÿĀāĂ" +
            "ăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĨĩĪīĬĭĮįİıĴĵĶķĹĺĻļĽľŁłŃńŅ" +
            "ņŇňŌōŎŏŐőŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſƏƠơƯưǍǎǏǐ" +
            "ǑǒǓǔǕǖǗǘǙǚǛǜǞǟǠǡǦǧǨǩǪǫǬǭǰǴǵǸǹǺǻȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȔȕȖȗȘșȚțȞ" +
            "ȟȦȧȨȩȪȫȬȭȮȯȰȱȲȳəЁАБВГДЕЗИЙКЛМНОПРСТУФХЫЭабвгдезийклмнопрстуф" +
            "хыэёḀḁḂḃḄḅḆḇḈḉḊḋḌḍḎḏḐḑḒḓḔḕḖḗḘḙḚḛḜḝḞḟḠḡḢḣḤḥḦḧḨḩḪḫḬḭḮḯḰḱḲḳḴḵḶḷ" +
            "ḸḹḺḻḼḽḾḿṀṁṂṃṄṅṆṇṈṉṊṋṌṍṎṏṐṑṒṓṔṕṖṗṘṙṚṛṜṝṞṟṠṡṢṣṤṥṦṧṨṩṪṫṬṭṮṯṰṱṲṳ" +
            "ṴṵṶṷṸṹṺṻṼṽṾṿẀẁẂẃẄẅẆẇẈẉẊẋẌẍẎẏẐẑẒẓẔẕẖẗẘẙẛẠạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴ" +
            "ẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰ" +
            "ựỲỳỴỵỶỷỸỹ" +
            ""

        private const val FOLD_TO =
            "aaaaaaceeeeiiiidnoooooouuuuyaaaaaaceeeeiiiidnoooooouuuuyyaaa" +
            "aaaccccccccddddeeeeeeeeeegggggggghhiiiiiiiiiijjkkllllllllnnn" +
            "nnnoooooorrrrrrssssssssttttuuuuuuuuuuuuwwyyyzzzzzzseoouuaaii" +
            "oouuuuuuuuuuaaaaggkkoooojggnnaaaaaaeeeeiiiioooorrrruuuusstth" +
            "haaeeooooooooyyeeabvgdeziyklmnoprstufhieabvgdeziyklmnoprstuf" +
            "hieeaabbbbbbccddddddddddeeeeeeeeeeffgghhhhhhhhhhiiiikkkkkkll" +
            "llllllmmmmmmnnnnnnnnoooooooopppprrrrrrrrssssssssssttttttttuu" +
            "uuuuuuuuvvvvwwwwwwwwwwxxxxyyzzzzzzhtwysaaaaaaaaaaaaaaaaaaaaa" +
            "aaaeeeeeeeeeeeeeeeeiiiioooooooooooooooooooooooouuuuuuuuuuuuu" +
            "uyyyyyyyy" +
            ""

        private val FOLD_MULTI: Map<Char, String> = mapOf(
            'Æ' to "ae",
            'Þ' to "th",
            'ß' to "ss",
            'æ' to "ae",
            'þ' to "th",
            'Ĳ' to "ij",
            'ĳ' to "ij",
            'Œ' to "oe",
            'œ' to "oe",
            'Ǆ' to "dz",
            'ǅ' to "dz",
            'ǆ' to "dz",
            'Ǉ' to "lj",
            'ǈ' to "lj",
            'ǉ' to "lj",
            'Ǌ' to "nj",
            'ǋ' to "nj",
            'ǌ' to "nj",
            'Ǳ' to "dz",
            'ǲ' to "dz",
            'ǳ' to "dz",
            'Ж' to "zh",
            'Ц' to "ts",
            'Ч' to "ch",
            'Ш' to "sh",
            'Щ' to "sch",
            'Ю' to "yu",
            'Я' to "ya",
            'ж' to "zh",
            'ц' to "ts",
            'ч' to "ch",
            'ш' to "sh",
            'щ' to "sch",
            'ю' to "yu",
            'я' to "ya",
            'ẞ' to "ss",
        )
    }
}
