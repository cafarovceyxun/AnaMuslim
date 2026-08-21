package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.compose.utils.NumeralSystem
import com.cafarovceyxun.anamuslim.compose.utils.shapeDigits

/**
 * A field of [HadithEditorScreen] that a clipboard block can fill.
 *
 * The hadith/order number is deliberately **not** here: the editor asks the database for the next
 * free number on entry, so a number carried in from the clipboard would overwrite that and hand out
 * a duplicate. It stays a typed-by-hand field.
 */
internal enum class EditorField {
    TEXT_AR,
    TEXT_AZ,
    SOURCE,
    NOTE,
    NAME,
    NAME_AR,
    SLUG,
    AUTHOR,
    DESCRIPTION,
}

/**
 * Labels accepted in front of a block: a number, then [LabelSeparator].
 *
 * Numbers rather than words, and `§` rather than a dot, because the separator used to be the
 * ambiguous part. A dot ends most sentences, so every ordinary line was a candidate block opener and
 * only an exact word-list match kept the parser honest. `§` appears in practically no hadith text,
 * so a line either starts a block or plainly does not.
 *
 * One table for every editor — the number means the same field wherever it is pasted. If the numbers
 * restarted per screen, a block written for a hadith and pasted on a bab editor would silently fill
 * the wrong fields; this way it matches nothing and the editor says so.
 *
 * Written out rather than derived from [EditorField.ordinal]: the paste format is a thing the user
 * types from memory, and reordering the enum must not quietly move it.
 */
private val ClipboardFormLabels: Map<String, EditorField> = mapOf(
    // Hədis redaktoru
    "1" to EditorField.TEXT_AR,
    "2" to EditorField.TEXT_AZ,
    "3" to EditorField.SOURCE,
    "4" to EditorField.NOTE,

    // Cild / kitab / bab redaktoru
    "5" to EditorField.NAME,
    "6" to EditorField.NAME_AR,
    "7" to EditorField.SLUG,
    "8" to EditorField.AUTHOR,
    "9" to EditorField.DESCRIPTION,
)

/** Separates the label from its value. Deliberately a character no hadith text contains. */
private const val LabelSeparator = '§'

/**
 * Longest label above plus room for a two-digit one; anything longer before the separator cannot be
 * a label, so `Qanun § 5` stays ordinary text.
 *
 * Measured on the **cleaned** label ([cleanedLabel]), not on the raw characters before the sign:
 * indentation and invisible marks are not part of the label and used to push it past this limit.
 */
private const val MaxLabelLength = 2

/**
 * Turns a labelled clipboard block into the fields it fills — see [parseClipboardForms] for the
 * grammar. Keeps only the first record, for the editors that hold a single row (volume/book/bab).
 */
internal fun parseClipboardForm(
    raw: String,
    arabicFallback: EditorField,
    latinFallback: EditorField,
): Map<EditorField, String> =
    parseClipboardForms(raw, arabicFallback, latinFallback).firstOrNull() ?: emptyMap()

/**
 * Turns a labelled clipboard block into **one record per hadith** it describes.
 *
 * A line whose text before the first separator matches a known label opens a block; every following
 * line that does not itself open a block is appended to it, so multi-line Arabic pastes survive
 * intact. Text before the first label is dropped.
 *
 * A label is a number and a `§` — see [ClipboardFormLabels] for the table:
 *
 * ```
 * 1§ حَدَّثَنَا ...
 * ... ikinci sətir
 * 2§ Bizə rəvayət etdi
 * 3§ Buxari 42
 * 4§ bir qeyd
 * ```
 *
 * **A label that the current record already carries starts the next record.** That is what turns a
 * second `1§/2§/3§/4§` cycle in the same paste into a second hadith instead of appending it to the
 * first — one copy on the Mac, however many hadiths it holds. Two *adjacent* lines under the same
 * label stay one block, so a second `3§` right under the first is still one hadith with two sources.
 *
 * A `§` alone does not make a label — the text before it has to match [ClipboardFormLabels] exactly,
 * so a sentence that happens to contain the sign carries on as ordinary text.
 *
 * Values bound for an Arabic field are passed through [withArabicDigitsShaped] on the way out; the
 * sources these blocks are copied from sometimes carry the hadith number in Latin digits
 * (`927-حَدَّثَنَا`) while the surrounding corpus uses Arabic-Indic ones.
 *
 * **Unlabelled text is split by script instead.** Bab headings get copied as a bare pair —
 *
 * ```
 * باب في أن الجنب إذا أراد أن ينام، عليه أن يتوضأ وضوءه للصلاة.
 * Cünub olan kimsənin ... haqqında bab.
 * ```
 *
 * — so when no label is found, every Arabic-script line goes to [arabicFallback] and everything
 * else to [latinFallback], as a single record. The caller picks the pair that fits the screen it is
 * on: name/Arabic name on the volume-book-bab editor, hadith text/translation on the hadith one.
 */
internal fun parseClipboardForms(
    raw: String,
    arabicFallback: EditorField,
    latinFallback: EditorField,
): List<Map<EditorField, String>> {
    val records = mutableListOf<LinkedHashMap<EditorField, StringBuilder>>()
    var current: LinkedHashMap<EditorField, StringBuilder>? = null
    var currentBlock: StringBuilder? = null
    var lastField: EditorField? = null

    // Mac-dəki mətn redaktorları blokun əvvəlinə BOM qoya bilir; qalsa ilk etiket tanınmır.
    val lines = raw.removePrefix("﻿").split("\r\n", "\n", "\r")

    lines.forEach { line ->
        val opened = line.openedBlock()
        if (opened != null) {
            val (field, value) = opened
            val record = current
            val startsNewRecord = record == null || (field in record && field != lastField)
            val target = if (startsNewRecord) {
                LinkedHashMap<EditorField, StringBuilder>().also { records += it; current = it }
            } else {
                record!!
            }

            val builder = target.getOrPut(field) { StringBuilder() }
            // Yalnız qonşu eyni etiket bura düşür — blok davam edir, yeni qeyd açılmır.
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(value)
            currentBlock = builder
            lastField = field
        } else {
            currentBlock?.append('\n')?.append(line)
        }
    }

    val labelled = records
        .map { record -> record.mapValues { it.value.toString().trim() }.filterValues { it.isNotEmpty() } }
        .filter { it.isNotEmpty() }
        .map { it.withArabicFieldsShaped() }
    if (labelled.isNotEmpty()) return labelled

    val bare = lines.splitByScript(arabicFallback, latinFallback)
    return if (bare.isEmpty()) emptyList() else listOf(bare.withArabicFieldsShaped())
}

/**
 * The word labels this format used before the `N§` one, kept for a single purpose: recognising a
 * block written in the retired syntax so the editor can say so.
 *
 * Without this a legacy block finds no label at all, falls through to the by-script split and lands
 * *almost* right — Arabic in the Arabic field, everything else in the translation, each line still
 * carrying its `ar. ` / `az. ` prefix. Half-filled and plausible-looking is worse than refused.
 */
private val LegacyClipboardLabels: Set<String> = setOf(
    "ar", "ərəb", "ereb", "ərəbcə", "arabic", "text_ar",
    "az", "tərcümə", "tercume", "azərbaycanca", "translation", "text_az",
    "mənbə", "menbe", "mə", "me", "source",
    "qeyd", "qe", "note",
    "ad", "ad_az", "name", "name_az", "ad_ar", "name_ar", "slug",
    "müəllif", "muellif", "author", "təsvir", "tesvir", "izah", "description",
)

/** True when some line opens a block in the retired `ar.` / `az.` / `mə.` / `qe.` syntax. */
internal fun String.looksLikeLegacyClipboardForm(): Boolean =
    split("\r\n", "\n", "\r").any { line ->
        val separator = line.indexOfFirst { it == ':' || it == '：' || it == '.' }
        separator > 0 && separator <= 16 &&
            line.substring(0, separator).trim().lowercase() in LegacyClipboardLabels
    }

/** The two fields that hold Arabic script, and so the only ones digit shaping is applied to. */
private val ArabicFields = setOf(EditorField.TEXT_AR, EditorField.NAME_AR)

private fun Map<EditorField, String>.withArabicFieldsShaped(): Map<EditorField, String> =
    mapValues { (field, value) ->
        if (field in ArabicFields) value.withArabicDigitsShaped() else value
    }

/**
 * Rewrites Latin digits as Arabic-Indic ones (`927-حَدَّثَنَا` → `٩٢٧-حَدَّثَنَا`), **only on lines that
 * carry Arabic script**.
 *
 * The corpus writes its numbers in Arabic-Indic (`(البخاري-١٦٢)`), but the sources these blocks are
 * copied from sometimes leave the leading hadith number in Latin digits, which then sits in the
 * middle of an Arabic paragraph in the wrong script.
 *
 * Line-scoped on purpose. A line with no Arabic letter is left byte-for-byte alone, so a Latin
 * sentence that ended up in an Arabic field keeps its own digits, and the shaping can never reach
 * a source, note, translation or slug. Within a shaped line only `0`–`9` move: [shapeDigits] passes
 * every letter, diacritic, punctuation mark and already-Arabic digit through untouched.
 */
internal fun String.withArabicDigitsShaped(): String {
    if (none { it in '0'..'9' }) return this
    return split("\n").joinToString("\n") { line ->
        if (line.any { it.isArabicScript() }) NumeralSystem.ARAB.shapeDigits(line) else line
    }
}

/**
 * Fallback for a block with no labels at all: Arabic-script lines on one side, everything else on
 * the other, each side keeping its own line order. Blank lines are dropped rather than kept as
 * separators, since the two halves are already separated by the script itself.
 */
private fun List<String>.splitByScript(
    arabicField: EditorField,
    latinField: EditorField,
): Map<EditorField, String> {
    val arabic = mutableListOf<String>()
    val latin = mutableListOf<String>()

    forEach { line ->
        val text = line.trim()
        if (text.isEmpty()) return@forEach
        if (text.any { it.isArabicScript() }) arabic += text else latin += text
    }

    return buildMap {
        if (arabic.isNotEmpty()) put(arabicField, arabic.joinToString("\n"))
        if (latin.isNotEmpty()) put(latinField, latin.joinToString("\n"))
    }
}

/** Arabic, Arabic Supplement, Extended-A and the presentation-form blocks the fonts here use. */
private fun Char.isArabicScript(): Boolean =
    this in '؀'..'ۿ' || // Arabic
        this in 'ݐ'..'ݿ' || // Arabic Supplement
        this in 'ࢠ'..'ࣿ' || // Arabic Extended-A
        this in 'ﭐ'..'﷿' || // Presentation Forms-A
        this in 'ﹰ'..'ﻼ' // Presentation Forms-B (stops short of the BOM)

/** The field this line opens a block for and the text after the label, or null for a continuation. */
private fun String.openedBlock(): Pair<EditorField, String>? {
    val separator = indexOf(LabelSeparator)
    if (separator <= 0) return null
    val label = substring(0, separator).cleanedLabel()
    if (label.isEmpty() || label.length > MaxLabelLength) return null
    val field = ClipboardFormLabels[label] ?: return null
    return field to substring(separator + 1).trimInvisible()
}

/**
 * The label as the table spells it, with everything that is not the number itself taken off:
 * indentation, no-break spaces, bidi and zero-width marks, and Arabic-Indic digits folded back to
 * Latin ones.
 *
 * This is the whole difference between a block that fills the source field and one that silently
 * lands at the end of the translation. The label is typed on a Mac and travels here by clipboard
 * sync, so it arrives with whatever the editor there put in front of it — a list indent, a `U+200F`
 * before a line that begins a right-to-left paragraph, a per-line BOM, a `٣` autocorrected out of a
 * `3` in Arabic context. None of it is visible in the paste, and the old check counted **raw**
 * characters before the sign: two spaces of indent already pushed `3§` past [MaxLabelLength], the
 * line stopped being a label and was appended to the block above it as ordinary text. Every hadith
 * after the first indented one lost its source and note into `2§`.
 *
 * `Qanun § 5` still stays ordinary text: cleaning does not shorten it to two characters.
 */
private fun String.cleanedLabel(): String = buildString {
    for (c in this@cleanedLabel) {
        when {
            c.isLabelPadding() -> Unit
            c in '٠'..'٩' -> append('0' + (c - '٠')) // Arabic-Indic
            c in '۰'..'۹' -> append('0' + (c - '۰')) // Extended Arabic-Indic
            else -> append(c)
        }
    }
}

/**
 * Trims the edges like [String.trim], and takes the invisible marks with it — a value that is
 * nothing but a bidi mark has to count as empty, or an untouched field is filled with a character
 * no one can see.
 */
private fun String.trimInvisible(): String = trim { it.isLabelPadding() }

/**
 * Whitespace, plus the spaces and marks [String.trim] leaves behind: `Char.isWhitespace` is false
 * for the no-break spaces and for every zero-width and bidi control character.
 */
private fun Char.isLabelPadding(): Boolean =
    isWhitespace() ||
        this == '\u00A0' || this == '\u202F' || this == '\u2007' || // no-break spaces
        this in '\u200B'..'\u200F' || // zero-width space/joiner, LRM, RLM
        this in '\u202A'..'\u202E' || // bidi embedding/override
        this in '\u2066'..'\u2069' || // bidi isolates
        this == '\uFEFF' // BOM, per line rather than only at the start of the paste
