package com.cafarovceyxun.anamuslim.compose.screens.hadith

/**
 * How much a broken rule costs: an error stops the import, a warning only asks to be looked at.
 *
 * The split is the whole point of the check. A paste holds a book; refusing it over something that
 * merely *looks* unusual would teach the user to ignore the panel, and accepting something that is
 * plainly wrong writes hundreds of rows that then have to be deleted by hand.
 */
internal enum class BulkIssueLevel { ERROR, WARNING }

/** What the paste got wrong, in the pieces the message is built from. */
internal sealed interface BulkIssueKind {
    /** A label out of turn: `1§` then `3§`, so the translation is missing. */
    data class OutOfOrder(val found: String, val expected: String) : BulkIssueKind

    /** The same label twice inside one hadith: `2§` … `2§`. */
    data class Repeated(val label: String) : BulkIssueKind

    /** The hadith ended — a bab, a verse or the end of the paste — before `3§`. */
    data class Incomplete(val missing: List<String>) : BulkIssueKind

    /** The label is there but nothing follows it, so the field is written empty. */
    data class EmptyField(val label: String) : BulkIssueKind

    /**
     * A bab with an Arabic name and no Azerbaijani one. An error, because the plan does not leave
     * the column empty — it copies the Arabic name into it (`name.ifBlank { nameAr }`), and the
     * book then shows an Arabic heading where its translation belongs.
     */
    data class MissingLatinName(val label: String) : BulkIssueKind

    /** A bab with no Arabic name: the row is fine, the heading simply loses its Arabic half. */
    data class MissingArabicName(val label: String) : BulkIssueKind

    /** Latin (or any non-Arabic) letters inside an Arabic field. Digits are not letters. */
    data class LatinInArabic(val sample: String) : BulkIssueKind

    /** Arabic letters inside an Azerbaijani field — legitimate often enough to only warn. */
    data class ArabicInLatin(val sample: String) : BulkIssueKind

    /** The same hadith text appears twice in this paste; [firstLine] is where it was first seen. */
    data class DuplicateHadith(val firstLine: Int) : BulkIssueKind

    /** Two babs in this paste carry the same name. */
    data class DuplicateChapter(val firstLine: Int) : BulkIssueKind

    /** A bab by this name is already in the book — the sign of a paste being imported twice. */
    data object ChapterExists : BulkIssueKind
}

/**
 * One issue, anchored at the exact character range in the pasted text that caused it.
 *
 * [start]/[end] are offsets into the **raw** string the field holds, so the screen can select that
 * range and let the text field scroll itself there. A book-sized paste is thousands of lines long;
 * a message without an offset is a message that gets hunted for by hand.
 */
internal data class BulkIssue(
    val kind: BulkIssueKind,
    val line: Int,
    val start: Int,
    val end: Int,
    val lineText: String,
) {
    val level: BulkIssueLevel
        get() = when (kind) {
            is BulkIssueKind.ArabicInLatin,
            is BulkIssueKind.MissingArabicName,
            is BulkIssueKind.DuplicateHadith,
            is BulkIssueKind.DuplicateChapter,
            is BulkIssueKind.ChapterExists -> BulkIssueLevel.WARNING

            is BulkIssueKind.OutOfOrder,
            is BulkIssueKind.Repeated,
            is BulkIssueKind.Incomplete,
            is BulkIssueKind.EmptyField,
            is BulkIssueKind.MissingLatinName,
            is BulkIssueKind.LatinInArabic -> BulkIssueLevel.ERROR
        }
}

internal fun List<BulkIssue>.errorCount(): Int = count { it.level == BulkIssueLevel.ERROR }

internal fun List<BulkIssue>.warningCount(): Int = count { it.level == BulkIssueLevel.WARNING }

/**
 * Checks a bulk paste against the rules the format cannot check for itself, before any of it is
 * written.
 *
 * **The order of the hadith labels.** `1§ 2§ 3§` is the whole hadith — Arabic, translation, source —
 * and `4§` an optional note after them. [parseHadithBulk] is deliberately forgiving here: a repeated
 * label simply opens the next hadith, a missing one leaves an empty field, and both produce rows
 * that look plausible in the preview. So one skipped `2§` in the middle of a book shifts nothing and
 * says nothing — it writes a hadith with no translation, and the next one with the wrong source.
 *
 * **That each field actually holds something.** A label with nothing after it passes the sequence
 * and still writes an empty column.
 *
 * **That a bab carries both of its names.** [buildBulkPlan] fills a missing Azerbaijani name with
 * the Arabic one rather than leaving it null, so a forgotten `q§` is invisible until the book is
 * read.
 *
 * **The script each field is written in.** Azerbaijani letters inside the Arabic text mean the block
 * was cut in the wrong place, so that is an error; Arabic letters inside the translation are usually
 * a quoted name or a `ﷺ`, so that is only a warning. Digits count as neither — the corpus writes
 * hadith numbers in Latin digits inside Arabic paragraphs on purpose ([withArabicDigitsShaped]).
 *
 * **That nothing is being written twice.** The same hadith text or bab name twice in one paste, or a
 * bab whose name is already in the book ([existingChapterNames]), warns rather than blocks: nothing
 * about the rows is malformed, but a book imported twice is the most expensive mistake here — the
 * numbering simply continues, so the second copy looks exactly as legitimate as the first.
 *
 * Every issue carries the offset of the exact character range that caused it, not just the line
 * number: the panel jumps the caret there.
 *
 * The walk mirrors [parseHadithBulk] line for line — same [openedBulkBlock] grammar and the same
 * rules for when a block ends, so a line the parser reads as a continuation is a continuation here
 * too and multi-line Arabic never trips the sequence. What it does *not* do is stop at the first
 * mistake: a paste is fixed in one pass, so all of them are collected.
 */
internal fun validateHadithBulk(
    raw: String,
    existingChapterNames: Set<String> = emptySet(),
): List<BulkIssue> {
    if (raw.isBlank()) return emptyList()

    val walk = BulkWalk(existingChapterNames)
    raw.rawLines().forEach(walk::read)
    return walk.finish()
}

/** `1§`, `2§` … — the label as it is typed, for the messages. */
internal fun bulkLabelText(number: Int): String = "$number$LabelSeparator"

/**
 * The line-by-line walk, holding both of the states the check needs: which label the hadith is up to
 * (the sequence), and which block is still collecting text (everything else).
 *
 * They are separate on purpose. The sequence has to survive a mistake — after a skipped `2§` the
 * labels that follow are read from where they actually are — while the blocks have to end exactly
 * where [parseHadithBulk] ends them, or the emptiness check would fire on text the parser will
 * happily append a line later.
 */
private class BulkWalk(existingChapterNames: Set<String>) {

    private val existing = existingChapterNames
        .map { it.normalizedForMatching() }
        .filterTo(mutableSetOf()) { it.isNotEmpty() }

    private val issues = mutableListOf<BulkIssue>()
    private val seenChapters = mutableMapOf<String, Int>()
    private val seenHadiths = mutableMapOf<String, Int>()

    /** The last hadith label of the open record (1–4), and the line it was on. */
    private var open: Int? = null
    private var openLine: RawLine? = null

    /** Which script unlabelled continuation lines belong to; null while nothing is checked. */
    private var script: BlockScript? = null

    private var lastLabel: BulkLabel? = null
    private var lastBuilder: StringBuilder? = null
    private var section: SectionBlock? = null
    private var hadith: HadithBlock? = null

    fun read(line: RawLine) {
        val opened = line.text.openedBulkBlock()
        if (opened == null) {
            // Davam sətri: axını dəyişmir, yalnız blokun mətninə əlavə olunur və yoxlanır.
            lastBuilder?.append('\n')?.append(line.text)
            script?.let { expected -> add(line.scriptIssue(line.text, line.start, expected)) }
            return
        }

        val (label, value) = opened
        val valueStart = line.text.indexOf(LabelSeparator) + 1

        when (label) {
            is BulkLabel.Section -> openSection(label, line, value)

            is BulkLabel.VerseRef -> {
                closeRecord()
                closeBlock()
                script = null
            }

            // Tanınmayan etiket sətri parser tərəfindən onsuz da bildirilir; blok da bağlanmır —
            // sonrakı sətirlər yuxarıdakı bloka yapışmağa davam edir.
            is BulkLabel.Bad -> return

            is BulkLabel.Field -> openField(label, line, value)
        }

        script?.let { expected ->
            add(line.scriptIssue(line.text.substring(valueStart), line.start + valueStart, expected))
        }
    }

    fun finish(): List<BulkIssue> {
        closeBlock()
        closeRecord()
        // Bir sətir üçün iki xəbər ola bilər (məsələn boş sahə və təkrar etiket) — sıralama sabit
        // olduğu üçün onlar yazıldıqları ardıcıllıqla qalır.
        return issues.sortedBy { it.line }
    }

    private fun openSection(label: BulkLabel.Section, line: RawLine, value: String) {
        closeRecord()

        val current = section?.takeIf { it.isSub == label.isSub }
        val builder = current?.builderFor(label.isArabic)
        // Parserdəki eyni qayda: yalnız qonşu eyni etiket bloku davam etdirir, təkrarı yenisini açır.
        if (builder == null || (builder.isNotEmpty() && lastLabel != label)) {
            closeBlock()
            section = SectionBlock(label.isSub, line)
        }

        val block = section ?: return
        val target = block.builderFor(label.isArabic)
        if (target.isEmpty()) block.rememberLine(label.isArabic, line) else target.append('\n')
        target.append(value)

        lastBuilder = target
        lastLabel = label
        script = if (label.isArabic) BlockScript.ARABIC else BlockScript.LATIN
    }

    private fun openField(label: BulkLabel.Field, line: RawLine, value: String) {
        val number = HadithLabelNumbers[label.field] ?: return
        checkSequence(number, line)

        val current = hadith
        if (current == null || (number in current.fields && lastLabel != label)) {
            closeBlock()
            hadith = HadithBlock()
        }

        val block = hadith ?: return
        val target = block.fields.getOrPut(number) {
            StringBuilder().also { block.lines[number] = line }
        }
        if (target.isNotEmpty()) target.append('\n')
        target.append(value)

        lastBuilder = target
        lastLabel = label
        script = when (number) {
            1 -> BlockScript.ARABIC
            2 -> BlockScript.LATIN
            else -> null
        }
    }

    /** `1§ → 2§ → 3§`, then either the note or the next hadith. Anything else is named and passed. */
    private fun checkSequence(found: Int, line: RawLine) {
        val previous = open

        val fits = when (previous) {
            null -> found == 1
            1 -> found == 2
            2 -> found == 3
            // 3§-dən sonra ya qeyd (4§) gəlir, ya da növbəti hədis (1§) — ikisi də düzgündür.
            3 -> found == 4 || found == 1
            else -> found == 1
        }
        if (!fits) {
            add(
                line,
                if (found == previous) {
                    BulkIssueKind.Repeated(bulkLabelText(found))
                } else {
                    BulkIssueKind.OutOfOrder(bulkLabelText(found), expectedAfter(previous))
                },
            )
        }

        // Xətadan sonra da axın tapılan etiketdən davam edir: əks halda bir buraxılmış etiket
        // ondan sonrakı bütün hədisləri saxta xəta ilə doldurardı.
        open = found
        openLine = line
    }

    /** The hadith is over — a bab, a verse or the end of the paste. Say so if it never reached `3§`. */
    private fun closeRecord() {
        val last = open
        val line = openLine
        open = null
        openLine = null
        if (last == null || line == null || last >= LastRequiredLabel) return
        add(line, BulkIssueKind.Incomplete((last + 1..LastRequiredLabel).map(::bulkLabelText)))
    }

    /** The block has all the text it is going to get, so what it holds can finally be judged. */
    private fun closeBlock() {
        section?.let(::finishSection)
        hadith?.let(::finishHadith)
        section = null
        hadith = null
        lastBuilder = null
        lastLabel = null
    }

    private fun finishSection(block: SectionBlock) {
        val arabic = block.arabic.toString().trim()
        val latin = block.latin.toString().trim()
        // Hər iki adı boş bölmə yazılmır və parser onu `NamelessSection` kimi onsuz da sayır.
        if (arabic.isEmpty() && latin.isEmpty()) return

        when {
            latin.isEmpty() -> add(
                block.arabicLine ?: block.anchor,
                BulkIssueKind.MissingLatinName(if (block.isSub) "qq$LabelSeparator" else "q$LabelSeparator"),
            )

            arabic.isEmpty() -> add(
                block.latinLine ?: block.anchor,
                BulkIssueKind.MissingArabicName(if (block.isSub) "aa$LabelSeparator" else "a$LabelSeparator"),
            )
        }

        // Yalnız bablar: alt bab adları («باب», «Bab») kitab boyu qanuni şəkildə təkrarlanır.
        if (block.isSub) return

        val key = latin.ifEmpty { arabic }.normalizedForMatching()
        if (key.isEmpty()) return

        val first = seenChapters[key]
        if (first != null) {
            add(block.anchor, BulkIssueKind.DuplicateChapter(first))
            return
        }
        seenChapters[key] = block.anchor.number

        if (key in existing || arabic.normalizedForMatching() in existing) {
            add(block.anchor, BulkIssueKind.ChapterExists)
        }
    }

    private fun finishHadith(block: HadithBlock) {
        for (number in 1..LastRequiredLabel) {
            val text = block.fields[number] ?: continue
            val line = block.lines[number] ?: continue
            // Etiketin özü yoxdursa ardıcıllıq yoxlaması bildirir; burada yalnız boş qalanı deyirik.
            if (text.isBlank()) add(line, BulkIssueKind.EmptyField(bulkLabelText(number)))
        }

        val arabic = block.fields[1]?.toString()?.trim().orEmpty()
        val body = if (arabic.isNotEmpty()) arabic else block.fields[2]?.toString()?.trim().orEmpty()
        if (body.isEmpty()) return
        val anchor = block.lines[1] ?: block.lines[2] ?: return

        val key = body.normalizedForMatching()
        val first = seenHadiths[key]
        if (first != null) add(anchor, BulkIssueKind.DuplicateHadith(first)) else seenHadiths[key] = anchor.number
    }

    private fun add(line: RawLine, kind: BulkIssueKind) {
        issues += BulkIssue(
            kind = kind,
            line = line.number,
            start = line.start,
            end = minOf(line.end, line.start + LabelHighlightChars),
            lineText = line.text.trim(),
        )
    }

    private fun add(issue: BulkIssue?) {
        if (issue != null) issues += issue
    }
}

/** The label the grammar wants next; `3§` accepts two, and both are worth naming in the message. */
private fun expectedAfter(previous: Int?): String = when (previous) {
    null -> bulkLabelText(1)
    1 -> bulkLabelText(2)
    2 -> bulkLabelText(3)
    3 -> "${bulkLabelText(4)} / ${bulkLabelText(1)}"
    else -> bulkLabelText(1)
}

/** Which script a block's lines are supposed to be written in. */
private enum class BlockScript { ARABIC, LATIN }

/** A bab or alt-bab, which the grammar spells as a pair of labels rather than one. */
private class SectionBlock(val isSub: Boolean, val anchor: RawLine) {
    val arabic = StringBuilder()
    val latin = StringBuilder()
    var arabicLine: RawLine? = null
    var latinLine: RawLine? = null

    fun builderFor(isArabic: Boolean): StringBuilder = if (isArabic) arabic else latin

    fun rememberLine(isArabic: Boolean, line: RawLine) {
        if (isArabic) arabicLine = line else latinLine = line
    }
}

/** One hadith's four fields, each keyed by its label number. */
private class HadithBlock {
    val fields = LinkedHashMap<Int, StringBuilder>()
    val lines = LinkedHashMap<Int, RawLine>()
}

/**
 * The one issue this line's text carries, or null when its script is clean.
 *
 * One issue per line rather than one per stray word: the panel is a list to walk, and a translation
 * accidentally pasted into `1§` would otherwise fill it with a hundred rows pointing at the same
 * mistake. The message still names the first few offending words, taken as whole runs of letters —
 * `Bizə` reads as a mistake, `B, i, z, ə` reads as noise.
 */
private fun RawLine.scriptIssue(text: String, base: Int, script: BlockScript): BulkIssue? {
    val runs = mutableListOf<IntRange>()
    var runStart = -1
    text.forEachIndexed { index, char ->
        val foreign = char.isLetter() && when (script) {
            BlockScript.ARABIC -> !char.isArabicScript()
            BlockScript.LATIN -> char.isArabicScript()
        }
        if (foreign) {
            if (runStart < 0) runStart = index
        } else if (runStart >= 0) {
            runs += runStart until index
            runStart = -1
        }
    }
    if (runStart >= 0) runs += runStart until text.length
    if (runs.isEmpty()) return null

    val words = runs.map { text.substring(it) }
    val sample = words.take(ForeignWordsShown).joinToString(", ")
    val listed = if (words.size > ForeignWordsShown) "$sample …" else sample
    val first = runs.first()

    return BulkIssue(
        kind = when (script) {
            BlockScript.ARABIC -> BulkIssueKind.LatinInArabic(listed)
            BlockScript.LATIN -> BulkIssueKind.ArabicInLatin(listed)
        },
        line = number,
        start = base + first.first,
        end = base + first.last + 1,
        lineText = this.text.trim(),
    )
}

/**
 * A line together with where it sits in the raw string.
 *
 * [String.split] would be shorter, but the offsets are the whole reason this check exists as its own
 * pass: without them the panel could only print a line number for the user to go hunting with.
 */
private class RawLine(val text: String, val number: Int, val start: Int, val end: Int)

/** Same line breaks [parseHadithBulk] splits on — `\r\n`, `\n` and a bare `\r` — with offsets kept. */
private fun String.rawLines(): List<RawLine> {
    val lines = mutableListOf<RawLine>()
    var start = 0
    var index = 0
    var number = 1

    while (index < length) {
        val char = this[index]
        if (char == '\n' || char == '\r') {
            lines += RawLine(substring(start, index), number++, start, index)
            if (char == '\r' && index + 1 < length && this[index + 1] == '\n') index++
            index++
            start = index
        } else {
            index++
        }
    }
    lines += RawLine(substring(start), number, start, length)

    return lines
}

/**
 * The form two names are compared in: case and spacing carry no meaning here, and the same heading
 * typed twice differs in exactly those.
 */
private fun String.normalizedForMatching(): String =
    trim().lowercase().split(WhitespaceRuns).joinToString(" ")

private val WhitespaceRuns = Regex("\\s+")

private val HadithLabelNumbers = mapOf(
    EditorField.TEXT_AR to 1,
    EditorField.TEXT_AZ to 2,
    EditorField.SOURCE to 3,
    EditorField.NOTE to 4,
)

/** `4§` is the optional note; everything up to and including `3§` has to be there. */
private const val LastRequiredLabel = 3

/** How much of the line a label issue selects — enough to see, short enough to stay on one screen. */
private const val LabelHighlightChars = 60

private const val ForeignWordsShown = 3
