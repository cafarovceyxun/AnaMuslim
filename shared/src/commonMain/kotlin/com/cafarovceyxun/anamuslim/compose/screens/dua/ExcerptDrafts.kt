package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.TextRange
import com.cafarovceyxun.anamuslim.utils.supabase.Dua

/**
 * Seçim ekranının qaralamaları: bir mənbədən **bir neçə dua**, hər duada **bir neçə hissə**.
 *
 * İki ayrı ehtiyac var və ikisi də eyni hədisdə rast gəlir:
 * - **Bir mənbə → bir neçə mövzu.** Namaz hədisində (Əhməd 803) açılış duası, rüku, rükudan
 *   qalxma, səcdə və salamdan sonrakı zikr yan-yanadır — hər biri öz alt başlığına gedir. Əvvəl
 *   bunun üçün seçim ekranını beş dəfə açıb eyni hədisi beş dəfə sürüşdürmək lazım idi.
 * - **Bir dua → bir neçə say.** Namazdan sonrakı təsbih 33 + 33 + 33 + 1-dir: dörd ayrı sayı olan,
 *   amma **bir** zikr. Baza bunu çoxdan saxlayır (`dua.part_of_id`, hər hissənin öz
 *   `repeat_count`-u), seçim ekranı isə bir dəfəyə bir sətir yazırdı — hissələr sonradan dua
 *   ekranında bir-bir birləşdirilirdi.
 *
 * Ona görə qaralama iki səviyyəlidir: [EntryDraft] = bir dua və onun hədəfi (başlıq/alt başlıq,
 * Əsmada ad), [PartDraft] = onun bir hissəsi. Yadda saxlananda hər hissə bir `dua` sətridir.
 */
@Stable
internal class PartDraft {
    var arabic by mutableStateOf("")
    var translit by mutableStateOf("")
    var translation by mutableStateOf("")
    var countText by mutableStateOf("")

    /**
     * «Aktiv hissəyə get» düyməsinin hədəfi. UI obyektidir, amma qaralama ilə bir ömür yaşayır:
     * hissə siyahıdan çıxanda onun sahəsi də kompozisiyadan çıxır.
     */
    val bringIntoView = BringIntoViewRequester()

    /** Yalnız ərəbcə məcburidir — oxunuş və tərcümə mənbədə olmaya bilər. */
    val isReady: Boolean get() = arabic.isNotBlank()

    val count: Int? get() = parseRepeatCount(countText)
}

/**
 * Bir dua: hədəfi və 1..[MAX_DRAFT_PARTS] hissəsi.
 *
 * Hədəf açar → dəyər xəritəsidir ([TargetSpec.key] ilə), çünki dua axınında iki pillə (başlıq, alt
 * başlıq), Əsma axınında isə bir pillə (ad) var — ekran ikisini eyni kodla çəkir.
 */
@Stable
internal class EntryDraft private constructor(
    selected: Map<String, String>,
    newNames: Map<String, String>,
    newNamesAr: Map<String, String>,
) {
    constructor() : this(emptyMap(), emptyMap(), emptyMap())

    val parts: SnapshotStateList<PartDraft> = mutableStateListOf(PartDraft())

    private val selected = mutableStateMapOf<String, String>().apply { putAll(selected) }
    private val newNames = mutableStateMapOf<String, String>().apply { putAll(newNames) }
    private val newNamesAr = mutableStateMapOf<String, String>().apply { putAll(newNamesAr) }

    fun selectedId(key: String): String? = selected[key]

    fun select(key: String, id: String?) {
        if (id == null) selected.remove(key) else selected[key] = id
    }

    fun newName(key: String): String = newNames[key].orEmpty()

    fun setNewName(key: String, value: String) {
        newNames[key] = value
    }

    fun newNameAr(key: String): String = newNamesAr[key].orEmpty()

    fun setNewNameAr(key: String, value: String) {
        newNamesAr[key] = value
    }

    /**
     * Növbəti dua — hədəfi **bu duadan** miras alır, bir boş hissə ilə başlayır.
     *
     * Miras qəsdəndir: bir mənbədəki dualar adətən eyni başlığa düşür (namaz zikrləri), çox vaxt
     * eyni alt başlığa da. Fərqlidirsə bir toxunuşla dəyişir — boş hədəfdən başlamaq isə hər dəfə
     * iki seçim deməkdir. Kart başlığında hədəf açıq yazıldığı üçün yanlış miras gözdən qaçmır.
     */
    fun next(): EntryDraft = EntryDraft(selected.toMap(), newNames.toMap(), newNamesAr.toMap())

    val canAddPart: Boolean get() = parts.size < MAX_DRAFT_PARTS
}

/** Bir duaya ən çox neçə hissə — baza da eyni həddi qoyur (`part_no` 1..5). */
internal const val MAX_DRAFT_PARTS = 5

/** Sayın yuxarı həddi — bazadakı CHECK `1..100000`; sahə 5 rəqəmlə məhduddur. */
internal const val MAX_REPEAT_DIGITS = 5

/** Tez seçilən saylar: 33 + 33 + 33(34) + 1 təsbihi, 3/7/10 isə səhər-axşam zikrlərində. */
internal val QUICK_REPEAT_COUNTS = listOf(1, 3, 7, 10, 33, 34, 100)

/** Sahədəki mətndən say; boş və ya sıfır → `null` (say göstərilmir). */
internal fun parseRepeatCount(text: String): Int? = text.toIntOrNull()?.takeIf { it > 0 }

/**
 * Qaralamanın hissələri `dua` sətirləri kimi — birincisi baş sətirdir.
 *
 * Hədəf (başlıq) və hissə bağlantısı burada **qoyulmur**: başlıq yeni ola bilər və slug-ı yalnız
 * yazılanda bəlli olur, `part_of_id` isə baş sətrin bazanın verdiyi id-sidir.
 */
internal fun EntryDraft.toDuaRows(data: ExcerptSourceData): List<Dua> =
    parts.map { part ->
        Dua(
            category_slug = "",
            source_type = data.sourceType,
            hadith_id = data.hadithId,
            chapter_no = data.chapterNo,
            verse_no = data.verseNo,
            verse_end = data.verseEnd,
            text_ar = part.arabic.trim(),
            text_az = part.translation.trim(),
            transliteration = part.translit.trim().takeIf { it.isNotBlank() },
            repeat_count = part.count,
            source = data.reference,
        )
    }

/**
 * Mətndəki bütün `{…}` parçaları, soldan sağa.
 *
 * Mötərizələrin **özü** də aralığa düşür: onları kənarda saxlamaq sözü ortasından bölərdi, halbuki
 * oxunuş sətrində mötərizə onsuz da sözün bir hissəsi kimi yazılır. Bağlanmayan son `{` nəzərə
 * alınmır.
 */
internal fun bracesRanges(text: String): List<TextRange> = buildList {
    var from = 0
    while (true) {
        val open = text.indexOf('{', startIndex = from)
        if (open < 0) break

        val close = text.indexOf('}', startIndex = open + 1)
        if (close < 0) break

        add(TextRange(open, close + 1))
        from = close + 1
    }
}

/**
 * «Mötərizədə» düyməsinin növbəti seçimi.
 *
 * Bir rəvayətdə bir neçə dua ola bilər (Əhməd 803-də beşi), ona görə düymə hər basışda **növbəti**
 * mötərizəyə keçir və sonuncudan sonra birinciyə qayıdır. Əl ilə seçilmiş parça bir mötərizənin
 * içindədirsə əvvəlcə elə onu bütöv seçir — istifadəçi «bu dua» deyə göstərib.
 */
internal fun nextBracesRange(ranges: List<TextRange>, current: TextRange?): TextRange? {
    if (ranges.isEmpty()) return null
    if (current == null || current.collapsed) return ranges.first()

    val exact = ranges.indexOfFirst { it.min == current.min && it.max == current.max }
    if (exact >= 0) return ranges[(exact + 1) % ranges.size]

    ranges.firstOrNull { current.min >= it.min && current.max <= it.max }?.let { return it }

    return ranges.firstOrNull { it.min >= current.min } ?: ranges.first()
}
