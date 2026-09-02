package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter

/**
 * One thing a bulk paste describes, in the order it was written.
 *
 * The list is a *stream*, not a tree: a bab opens everything under it until the next bab, an alt-bab
 * until the next alt-bab or bab. Keeping it flat is what lets the preview show exactly the order the
 * rows will be written in, and [buildBulkPlan] is the single place that turns the order back into
 * parent/child links.
 */
internal sealed interface BulkEntry {
    /** `a§` (Arabic name) + `q§` (Azerbaijani name). */
    data class Chapter(val name: String, val nameAr: String) : BulkEntry

    /** `aa§` + `qq§`, under whichever [Chapter] came before it. */
    data class SubChapter(val name: String, val nameAr: String) : BulkEntry

    /** `1§`–`4§`, the same four fields the single-hadith editor fills from the clipboard. */
    data class HadithText(
        val textAr: String,
        val textAz: String,
        val source: String,
        val note: String,
    ) : BulkEntry {
        val isEmpty: Boolean get() = textAr.isBlank() && textAz.isBlank() && source.isBlank()
    }

    /**
     * `3:51§` / `5:13-15§` — a verse the paste only *points at*. The text is not in the paste at
     * all; [com.cafarovceyxun.anamuslim.compose.screens.hadith.resolveBulkVerses] reads it out of
     * the Quran database later, exactly the way the source field's verse picker does.
     */
    data class Verse(val chapterNo: Int, val fromVerse: Int, val toVerse: Int) : BulkEntry
}

/**
 * Something the paste asks for that cannot be carried out, surfaced in the preview before anything
 * is written.
 *
 * Every one of these means content was **dropped**, so they are counted rather than merely logged:
 * a whole book arrives in one paste and a silently skipped bab would only be noticed chapters later.
 */
internal sealed interface BulkProblem {
    /** Lines with no block above them to belong to — text before the first label, mostly. */
    data class DroppedLines(val count: Int) : BulkProblem

    /** An alt-bab, hadith or verse that appeared before any bab, so it has no parent. */
    data class Orphan(val count: Int) : BulkProblem

    /** A bab/alt-bab whose two name labels were both blank. */
    data class NamelessSection(val count: Int) : BulkProblem

    /** A label shaped like a verse reference that points nowhere: `0:5§`, `115:1§`, `2:9-3§`. */
    data class BadVerseLabel(val labels: List<String>) : BulkProblem

    /** A `5§`–`9§` label: real in the single-row editors, meaningless in a bulk stream. */
    data class UnsupportedLabel(val labels: List<String>) : BulkProblem

    /** The verse exists but the database returned neither Arabic nor translation for it. */
    data class VerseUnavailable(val references: List<String>) : BulkProblem
}

internal data class BulkParseResult(
    val entries: List<BulkEntry>,
    val problems: List<BulkProblem>,
) {
    val chapterCount: Int get() = entries.count { it is BulkEntry.Chapter }
    val subChapterCount: Int get() = entries.count { it is BulkEntry.SubChapter }
    val hadithCount: Int get() = entries.count { it is BulkEntry.HadithText }
    val verseCount: Int get() = entries.count { it is BulkEntry.Verse }
    val isEmpty: Boolean get() = entries.isEmpty()
}

/** Longest label the bulk grammar can produce — `114:286-286` — plus nothing to spare. */
private const val MaxBulkLabelLength = 12

/** Surah count; a reference outside it is a typo, not a verse. */
private const val QuranChapterCount = 114

/** Longest surah. Per-surah bounds are checked later, against the database. */
private const val MaxVerseNo = 286

private val VerseLabelPattern = Regex("""^(\d{1,3})[:：](\d{1,3})(?:[-–—](\d{1,3}))?$""")

/**
 * Turns one paste holding a whole book — babs, alt-babs, hadiths and verse references — into the
 * stream of rows it describes.
 *
 * The grammar extends the single-hadith one ([parseClipboardForms]) rather than replacing it: same
 * `§` separator, same "a label the current block already carries starts the next one" rule, same
 * invisible-character cleaning ([cleanedLabel]). What is new is the section and verse labels:
 *
 * ```
 * a§ كتاب بدء الوحي          ← bab, ərəbcə adı
 * q§ Vəhyin başlanğıcı        ← bab, azərbaycanca adı
 * aa§ باب كيف كان بدء الوحي   ← alt bab, ərəbcə
 * qq§ Vəhyin necə başladığı   ← alt bab, azərbaycanca
 * 3:51§                       ← ayə: mətnini Qurandan özü çəkir
 * 1§ حَدَّثَنَا ...              ← hədis (dörd sahə əvvəlki formatın eynisi)
 * 2§ Bizə rəvayət etdi ...
 * 3§ Buxari 42
 * ```
 *
 * Everything is positional: a hadith belongs to the last alt-bab, or to the last bab when no alt-bab
 * has been opened. Anything before the first bab has no parent at all and is reported as
 * [BulkProblem.Orphan] instead of being written somewhere plausible.
 *
 * Unlabelled lines continue the block above them, so multi-line Arabic survives — but unlike the
 * single-hadith parser there is no by-script fallback here. A bulk paste with no labels at all is
 * simply not in this format, and guessing on a thousand lines would create a thousand wrong rows.
 */
internal fun parseHadithBulk(raw: String): BulkParseResult {
    val entries = mutableListOf<BulkEntry>()
    var droppedLines = 0
    var namelessSections = 0
    val badVerseLabels = mutableListOf<String>()
    val unsupportedLabels = mutableListOf<String>()

    var block: BulkBlock? = null
    var lastLabel: BulkLabel? = null
    var lastBuilder: StringBuilder? = null

    fun flush() {
        val current = block
        block = null
        lastBuilder = null
        lastLabel = null
        if (current == null) return

        val entry = current.toEntry()
        // Boş hədis bloku sadəcə buraxılır; adsız bölmə isə itən başlıqdır, ona görə sayılır.
        if (entry != null) entries += entry else if (current is BulkBlock.Section) namelessSections++
    }

    // Mac-dəki mətn redaktorları blokun əvvəlinə BOM qoya bilir; qalsa ilk etiket tanınmır.
    val lines = raw.removePrefix("﻿").split("\r\n", "\n", "\r")

    lines.forEach { line ->
        val opened = line.openedBulkBlock()
        if (opened == null) {
            val builder = lastBuilder
            if (builder == null) {
                if (line.isNotBlank()) droppedLines++
            } else {
                builder.append('\n').append(line)
            }
            return@forEach
        }

        val (label, value) = opened
        when (label) {
            is BulkLabel.Section -> {
                val current = block as? BulkBlock.Section
                val builder = current
                    ?.takeIf { it.isSub == label.isSub }
                    ?.builderFor(label.isArabic)
                // Yalnız qonşu eyni etiket bloku davam etdirir; təkrarı yeni bölmə açır.
                if (builder == null || (builder.isNotEmpty() && lastLabel != label)) {
                    flush()
                    block = BulkBlock.Section(label.isSub)
                }
                val target = (block as BulkBlock.Section).builderFor(label.isArabic)
                if (target.isNotEmpty()) target.append('\n')
                target.append(value)
                lastBuilder = target
                lastLabel = label
            }

            is BulkLabel.Field -> {
                val current = block as? BulkBlock.Hadith
                if (current == null || (label.field in current.fields && lastLabel != label)) {
                    flush()
                    block = BulkBlock.Hadith()
                }
                val target = (block as BulkBlock.Hadith).fields
                    .getOrPut(label.field) { StringBuilder() }
                if (target.isNotEmpty()) target.append('\n')
                target.append(value)
                lastBuilder = target
                lastLabel = label
            }

            is BulkLabel.VerseRef -> {
                flush()
                entries += BulkEntry.Verse(label.chapterNo, label.fromVerse, label.toVerse)
                // Ayə etiketi öz sətri ilə bitir: ardınca gələn etiketsiz mətnin gedəcəyi yer yoxdur,
                // ona görə davam bloku açılmır və həmin sətirlər «atıldı» kimi sayılır.
            }

            is BulkLabel.Bad -> {
                if (label.isVerseShaped) badVerseLabels += label.text else unsupportedLabels += label.text
            }
        }
    }
    flush()

    // Valideynsiz elementlər yazıla bilmir: ilk babdan əvvəlkilər buraxılır və sayılır.
    val firstChapter = entries.indexOfFirst { it is BulkEntry.Chapter }
    val orphans = if (firstChapter < 0) entries.size else firstChapter
    val kept = if (firstChapter < 0) emptyList() else entries.drop(firstChapter)

    val problems = buildList {
        if (orphans > 0) add(BulkProblem.Orphan(orphans))
        if (droppedLines > 0) add(BulkProblem.DroppedLines(droppedLines))
        if (namelessSections > 0) add(BulkProblem.NamelessSection(namelessSections))
        if (badVerseLabels.isNotEmpty()) add(BulkProblem.BadVerseLabel(badVerseLabels))
        if (unsupportedLabels.isNotEmpty()) add(BulkProblem.UnsupportedLabel(unsupportedLabels))
    }

    return BulkParseResult(entries = kept, problems = problems)
}

/** A row the import is about to write, with its number and slug already decided. */
internal sealed interface BulkRow {
    data class Chapter(val row: HadithChapter) : BulkRow
    data class SubChapter(val row: HadithSubChapter) : BulkRow
    data class HadithRow(val row: Hadith) : BulkRow
}

/**
 * Numbers and slugs the whole stream **before** anything is written, so the preview shows the rows
 * that will land and the import itself needs no bookkeeping of its own.
 *
 * Slugs follow the editor's own scheme — parent slug, the first two letters of the Azerbaijani name,
 * then the number — so a bulk-imported bab is indistinguishable from a hand-added one. A name in
 * Arabic only transliterates to nothing, so those fall back to a fixed prefix; the number keeps the
 * slug unique either way.
 *
 * Hadith numbering restarts inside every bab and alt-bab, matching
 * [com.cafarovceyxun.anamuslim.viewModels.HadithViewModel.getNextNumber]: the number is per
 * container in this database, not per book.
 */
internal fun buildBulkPlan(
    bookSlug: String,
    entries: List<BulkEntry>,
    firstChapterNo: Int,
): List<BulkRow> {
    val rows = mutableListOf<BulkRow>()
    val bookPart = bookSlug.replace("/", "")

    var chapterNo = firstChapterNo - 1
    var chapterSlug: String? = null
    var subChapterSlug: String? = null
    var subChapterNo = 0
    var hadithNo = 0

    entries.forEach { entry ->
        when (entry) {
            is BulkEntry.Chapter -> {
                chapterNo++
                val slug = bulkSlug(bookPart, entry.name, chapterNo, ChapterSlugFallback)
                chapterSlug = slug
                subChapterSlug = null
                subChapterNo = 0
                hadithNo = 0
                rows += BulkRow.Chapter(
                    HadithChapter(
                        slug = slug,
                        book_slug = bookSlug,
                        chapter_no = chapterNo,
                        name = entry.name.ifBlank { entry.nameAr },
                        name_ar = entry.nameAr.ifBlank { null },
                    )
                )
            }

            is BulkEntry.SubChapter -> {
                val parent = chapterSlug ?: return@forEach
                subChapterNo++
                val slug = bulkSlug(parent.replace("/", ""), entry.name, subChapterNo, SubChapterSlugFallback)
                subChapterSlug = slug
                hadithNo = 0
                rows += BulkRow.SubChapter(
                    HadithSubChapter(
                        slug = slug,
                        chapter_slug = parent,
                        sub_chapter_no = subChapterNo,
                        name = entry.name.ifBlank { entry.nameAr },
                        name_ar = entry.nameAr.ifBlank { null },
                    )
                )
            }

            is BulkEntry.HadithText -> {
                val parent = chapterSlug ?: return@forEach
                hadithNo++
                rows += BulkRow.HadithRow(
                    Hadith(
                        chapter_slug = parent,
                        sub_chapter_slug = subChapterSlug,
                        hadith_no = hadithNo,
                        text_ar = entry.textAr,
                        text_az = entry.textAz,
                        source = entry.source.ifBlank { null },
                        note = entry.note.ifBlank { null },
                    )
                )
            }

            // Ayələr plandan əvvəl mətnə çevrilir; qalıbsa deməli oxuna bilməyib və yazılmır.
            is BulkEntry.Verse -> Unit
        }
    }

    return rows
}

/** `<parent><ad ilk iki hərfi><nömrə>` — redaktordakı avto-slug ilə eyni. */
private fun bulkSlug(parentPart: String, name: String, number: Int, fallback: String): String {
    val cleaned = name.trim().toSlugPart()
    val prefix = when {
        cleaned.length >= 2 -> cleaned.take(2)
        cleaned.isNotEmpty() -> cleaned
        else -> fallback
    }
    return "$parentPart$prefix$number"
}

/** Yalnız ərəbcə adı olan bab latın hərfi vermir — slug o zaman bu prefikslə qurulur. */
private const val ChapterSlugFallback = "bb"
private const val SubChapterSlugFallback = "ab"

internal sealed interface BulkLabel {
    data class Section(val isSub: Boolean, val isArabic: Boolean) : BulkLabel
    data class Field(val field: EditorField) : BulkLabel
    data class VerseRef(val chapterNo: Int, val fromVerse: Int, val toVerse: Int) : BulkLabel

    /** Recognised as a label, but not one this grammar can act on. Reported, never guessed at. */
    data class Bad(val text: String, val isVerseShaped: Boolean) : BulkLabel
}

private sealed class BulkBlock {
    class Section(val isSub: Boolean) : BulkBlock() {
        val arabic = StringBuilder()
        val latin = StringBuilder()

        fun builderFor(isArabic: Boolean): StringBuilder = if (isArabic) arabic else latin
    }

    class Hadith : BulkBlock() {
        val fields = LinkedHashMap<EditorField, StringBuilder>()
    }

    /** The finished entry, or null when the block collected nothing worth writing. */
    fun toEntry(): BulkEntry? = when (this) {
        is Section -> {
            val name = latin.toString().trim()
            val nameAr = arabic.toString().trim().withArabicDigitsShaped()
            if (name.isEmpty() && nameAr.isEmpty()) {
                null
            } else if (isSub) {
                BulkEntry.SubChapter(name = name, nameAr = nameAr)
            } else {
                BulkEntry.Chapter(name = name, nameAr = nameAr)
            }
        }

        is Hadith -> {
            val values = fields.mapValues { it.value.toString().trim() }
            val entry = BulkEntry.HadithText(
                textAr = values[EditorField.TEXT_AR].orEmpty().withArabicDigitsShaped(),
                textAz = values[EditorField.TEXT_AZ].orEmpty(),
                source = values[EditorField.SOURCE].orEmpty(),
                note = values[EditorField.NOTE].orEmpty(),
            )
            entry.takeIf { !it.isEmpty }
        }
    }
}

/** The label this line opens a block for and the text after it, or null for a continuation line. */
internal fun String.openedBulkBlock(): Pair<BulkLabel, String>? {
    val separator = indexOf(LabelSeparator)
    if (separator <= 0) return null
    val label = substring(0, separator).cleanedLabel()
    if (label.isEmpty() || label.length > MaxBulkLabelLength) return null
    val kind = bulkLabelOf(label) ?: return null
    return kind to substring(separator + 1).trimInvisible()
}

private fun bulkLabelOf(label: String): BulkLabel? {
    when (label.lowercase()) {
        "a" -> return BulkLabel.Section(isSub = false, isArabic = true)
        // `t`/`tt` bu formatın ilk adları idi. Qəbul olunmağa davam edir, çünki tanınmayan etiket
        // sətri **davam sətri** kimi yuxarıdakı bloka yapışır: köhnə mətn səssizcə babın ərəbcə
        // adının içinə düşərdi. Bir hərfin əvəzi olaraq bu, ödəniləsi qiymət deyil.
        "q", "t" -> return BulkLabel.Section(isSub = false, isArabic = false)
        "aa" -> return BulkLabel.Section(isSub = true, isArabic = true)
        "qq", "tt" -> return BulkLabel.Section(isSub = true, isArabic = false)
    }

    ClipboardFormLabels[label]?.let { field ->
        // 5§–9§ ad/slug/müəllif sahələridir: axında bab adı `a§`/`t§` ilə verilir, ona görə bunlar
        // burada mənasızdır. Səssizcə mətnə çevirmək əvəzinə bildirilir.
        return if (field in BulkHadithFields) BulkLabel.Field(field) else BulkLabel.Bad(label, false)
    }

    val match = VerseLabelPattern.matchEntire(label) ?: return null
    val chapterNo = match.groupValues[1].toIntOrNull() ?: return null
    val from = match.groupValues[2].toIntOrNull() ?: return null
    val to = match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: from

    val valid = chapterNo in 1..QuranChapterCount && from in 1..MaxVerseNo &&
        to in from..MaxVerseNo
    return if (valid) BulkLabel.VerseRef(chapterNo, from, to) else BulkLabel.Bad(label, true)
}

private val BulkHadithFields = setOf(
    EditorField.TEXT_AR,
    EditorField.TEXT_AZ,
    EditorField.SOURCE,
    EditorField.NOTE,
)
