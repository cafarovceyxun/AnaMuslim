package com.cafarovceyxun.anamuslim.utils.translation

/** İdxal mətninin ayələrə necə bölünəcəyi. */
enum class ImportParseMode {
    /** Hər abzasın əvvəlindəki rəqəm ayə nömrəsidir: «1. Bismillah…», «(2) …», «3) …». */
    ByVerseNumber,

    /** Rəqəm yoxdur — abzaslar ardıcıl 1..N ayələrə düşür. */
    SequentialParagraphs,
}

/** Bir abzasın ayəyə bağlanmış hali. */
data class ParsedVerse(
    val verseNo: Int,
    val text: String,
    val problem: ImportProblem? = null,
)

enum class ImportProblem {
    /** Eyni ayə nömrəsi iki dəfə gəlib — sonuncu mətn qalır. */
    Duplicate,

    /** Nömrə surənin ayə sayından kənardadır. */
    OutOfRange,
}

data class ImportParseResult(
    val verses: List<ParsedVerse>,
    /** Surədə mətnsiz qalan ayələr — yükləmədən əvvəl görünsün. */
    val missing: List<Int>,
    /** İstifadəçiyə göstərilən xəbərdarlıqlar (azərbaycanca). */
    val warnings: List<String>,
) {
    val hasProblems: Boolean get() = warnings.isNotEmpty() || verses.any { it.problem != null }
    val usable: List<ParsedVerse> get() = verses.filter { it.problem != ImportProblem.OutOfRange }
}

/**
 * Yapışdırılmış tərcümə mətnini ayələrə bölür.
 *
 * Nömrəli rejimdə nömrəsiz abzas **əvvəlki ayənin davamı** sayılır (uzun ayələr çox abzasda gəlir),
 * ona görə mətn itmir. Bölgü heç nə yükləmir — nəticə əvvəlcə admin-ə göstərilir.
 */
fun parseTranslationImport(
    raw: String,
    verseCount: Int,
    mode: ImportParseMode,
): ImportParseResult {
    val paragraphs = splitParagraphs(raw)
    if (paragraphs.isEmpty()) {
        return ImportParseResult(emptyList(), (1..verseCount).toList(), listOf("Mətn boşdur."))
    }

    val warnings = mutableListOf<String>()
    val ordered = when (mode) {
        ImportParseMode.ByVerseNumber -> parseNumbered(paragraphs, warnings)
        ImportParseMode.SequentialParagraphs -> paragraphs.mapIndexed { index, text ->
            index + 1 to text
        }
    }

    // Təkrar nömrədə sonuncu mətn qalır, amma sətir işarələnir ki, admin gözdən qaçırmasın.
    val seen = mutableMapOf<Int, String>()
    val duplicates = mutableSetOf<Int>()
    ordered.forEach { (no, text) ->
        if (seen.containsKey(no)) duplicates.add(no)
        seen[no] = text
    }

    val verses = seen.entries.sortedBy { it.key }.map { (no, text) ->
        ParsedVerse(
            verseNo = no,
            text = text,
            problem = when {
                no < 1 || no > verseCount -> ImportProblem.OutOfRange
                no in duplicates -> ImportProblem.Duplicate
                else -> null
            },
        )
    }

    val filled = verses.filter { it.problem != ImportProblem.OutOfRange }.map { it.verseNo }.toSet()
    val missing = (1..verseCount).filterNot { it in filled }

    if (duplicates.isNotEmpty()) {
        warnings.add("Təkrarlanan ayə nömrəsi: ${duplicates.sorted().joinToString(", ")}")
    }
    verses.count { it.problem == ImportProblem.OutOfRange }.takeIf { it > 0 }?.let {
        warnings.add("$it abzasın nömrəsi surənin ayə sayından ($verseCount) kənardadır.")
    }
    if (missing.isNotEmpty()) {
        warnings.add("${missing.size} ayə mətnsiz qalır (məs. ${missing.take(5).joinToString(", ")}).")
    }
    if (mode == ImportParseMode.SequentialParagraphs && paragraphs.size != verseCount) {
        warnings.add("Abzas sayı ${paragraphs.size}, surədə isə $verseCount ayə var.")
    }

    return ImportParseResult(verses, missing, warnings)
}

/**
 * Abzaslara bölür: əvvəlcə boş sətirə görə, o alınmasa hər sətir bir abzasdır. Sənədlərin çoxu
 * ayələri boş sətirlə ayırır, bəziləri isə hər ayəni bir sətirdə saxlayır.
 */
private fun splitParagraphs(raw: String): List<String> {
    val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
    val byBlankLine = normalized.split(BLANK_LINE)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (byBlankLine.size > 1) return byBlankLine

    return normalized.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun parseNumbered(
    paragraphs: List<String>,
    warnings: MutableList<String>,
): List<Pair<Int, String>> {
    val out = mutableListOf<Pair<Int, String>>()

    paragraphs.forEach { paragraph ->
        val match = LEADING_NUMBER.find(paragraph)
        val number = match?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (number == null) {
            // Nömrəsiz abzas — əvvəlki ayənin davamı. Əvvəlki yoxdursa mətn itməsin deyə
            // xəbərdarlıq veririk.
            val previous = out.removeLastOrNull()
            if (previous == null) {
                warnings.add("İlk abzasda ayə nömrəsi tapılmadı — «Ardıcıl abzas» rejimini yoxlayın.")
            } else {
                out.add(previous.first to "${previous.second}\n\n$paragraph")
            }
            return@forEach
        }
        out.add(number to paragraph.removeRange(match.range).trim())
    }

    return out
}

/** «1.», «1)», «(1)», «[1]», «1 -», «1:» — nömrə ilə mətnin arasındakı ayırıcının hamısı. */
private val LEADING_NUMBER = Regex("""^\s*[\[(]?(\d{1,3})[\])]?\s*[.:)\-–—]?\s*""")

private val BLANK_LINE = Regex("""\n\s*\n""")
