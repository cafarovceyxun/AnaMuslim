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

    /** Latin (or any non-Arabic) letters inside an Arabic field. Digits are not letters. */
    data class LatinInArabic(val sample: String) : BulkIssueKind

    /** Arabic letters inside an Azerbaijani field — legitimate often enough to only warn. */
    data class ArabicInLatin(val sample: String) : BulkIssueKind
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
            is BulkIssueKind.ArabicInLatin -> BulkIssueLevel.WARNING

            is BulkIssueKind.OutOfOrder,
            is BulkIssueKind.Repeated,
            is BulkIssueKind.Incomplete,
            is BulkIssueKind.LatinInArabic -> BulkIssueLevel.ERROR
        }
}

internal fun List<BulkIssue>.errorCount(): Int = count { it.level == BulkIssueLevel.ERROR }

internal fun List<BulkIssue>.warningCount(): Int = count { it.level == BulkIssueLevel.WARNING }

/**
 * Checks a bulk paste against the two rules the format cannot check for itself, before any of it is
 * written.
 *
 * **The order of the hadith labels.** `1§ 2§ 3§` is the whole hadith — Arabic, translation, source —
 * and `4§` an optional note after them. [parseHadithBulk] is deliberately forgiving here: a repeated
 * label simply opens the next hadith, a missing one leaves an empty field, and both produce rows
 * that look plausible in the preview. So one skipped `2§` in the middle of a book shifts nothing and
 * says nothing — it writes a hadith with no translation, and the next one with the wrong source. The
 * sequence is checked here instead, and an error stops the import until it is fixed.
 *
 * **The script each field is written in.** Azerbaijani letters inside the Arabic text mean the block
 * was cut in the wrong place, so that is an error; Arabic letters inside the translation are usually
 * a quoted name or a `ﷺ`, so that is only a warning. Digits count as neither — the corpus writes
 * hadith numbers in Latin digits inside Arabic paragraphs on purpose ([withArabicDigitsShaped]).
 *
 * Every issue carries the offset of the exact character range that caused it, not just the line
 * number: the panel jumps the caret there.
 *
 * The walk mirrors [parseHadithBulk] line for line — same [openedBulkBlock] grammar, so a line the
 * parser reads as a continuation is a continuation here too, and multi-line Arabic never trips the
 * sequence. What it does *not* do is stop at the first mistake: a paste is fixed in one pass, so all
 * of them are collected.
 */
internal fun validateHadithBulk(raw: String): List<BulkIssue> {
    if (raw.isBlank()) return emptyList()

    val issues = mutableListOf<BulkIssue>()

    // Açıq hədisin son etiketi (1–4) və onun sətri — «yarımçıq» xəbəri məhz oraya bağlanır.
    var open: Int? = null
    var openLine: RawLine? = null

    // Etiketsiz davam sətirlərinin hansı yazıda olmalı olduğu; null = yoxlanılmır (3§, 4§, ayə).
    var script: BlockScript? = null

    fun add(line: RawLine, kind: BulkIssueKind) {
        issues += BulkIssue(
            kind = kind,
            line = line.number,
            start = line.start,
            end = minOf(line.end, line.start + LabelHighlightChars),
            lineText = line.text.trim(),
        )
    }

    fun closeHadith() {
        val last = open
        val line = openLine
        open = null
        openLine = null
        if (last == null || line == null || last >= LastRequiredLabel) return
        add(line, BulkIssueKind.Incomplete((last + 1..LastRequiredLabel).map(::bulkLabelText)))
    }

    raw.rawLines().forEach { line ->
        val opened = line.text.openedBulkBlock()
        if (opened == null) {
            // Davam sətri: axını dəyişmir, yalnız öz yazısına görə yoxlanır.
            script?.let { expected -> line.scriptIssue(line.text, line.start, expected)?.let(issues::add) }
            return@forEach
        }

        val (label, _) = opened
        val valueStart = line.text.indexOf(LabelSeparator) + 1
        val value = line.text.substring(valueStart)

        when (label) {
            is BulkLabel.Section -> {
                closeHadith()
                script = if (label.isArabic) BlockScript.ARABIC else BlockScript.LATIN
            }

            is BulkLabel.VerseRef -> {
                closeHadith()
                script = null
            }

            // Tanınmayan etiket sətri parser tərəfindən onsuz da bildirilir; ardıcıllığı pozmur,
            // çünki blok da bağlanmır — sonrakı sətirlər yuxarıdakı bloka yapışmağa davam edir.
            is BulkLabel.Bad -> return@forEach

            is BulkLabel.Field -> {
                val found = HadithLabelNumbers[label.field] ?: return@forEach
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

                // Xətadan sonra da axın tapılan etiketdən davam edir: əks halda bir buraxılmış
                // etiket ondan sonrakı bütün hədisləri saxta xəta ilə doldurardı.
                open = found
                openLine = line
                script = when (found) {
                    1 -> BlockScript.ARABIC
                    2 -> BlockScript.LATIN
                    else -> null
                }
            }
        }

        script?.let { expected -> line.scriptIssue(value, line.start + valueStart, expected)?.let(issues::add) }
    }

    closeHadith()
    return issues
}

/** `1§`, `2§` … — the label as it is typed, for the messages. */
internal fun bulkLabelText(number: Int): String = "$number$LabelSeparator"

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
