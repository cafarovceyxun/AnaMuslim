package com.cafarovceyxun.anamuslim.compose.screens.hadith

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
 * Labels accepted in front of a block, lowercased. Both the Azerbaijani word and a short ASCII
 * alias are listed for every field, so a block typed on a Mac keyboard without Azerbaijani letters
 * still lands in the right place.
 */
private val ClipboardFormLabels: Map<String, EditorField> = mapOf(
    "ar" to EditorField.TEXT_AR,
    "ərəb" to EditorField.TEXT_AR,
    "ereb" to EditorField.TEXT_AR,
    "ərəbcə" to EditorField.TEXT_AR,
    "arabic" to EditorField.TEXT_AR,
    "text_ar" to EditorField.TEXT_AR,

    "az" to EditorField.TEXT_AZ,
    "tərcümə" to EditorField.TEXT_AZ,
    "tercume" to EditorField.TEXT_AZ,
    "azərbaycanca" to EditorField.TEXT_AZ,
    "translation" to EditorField.TEXT_AZ,
    "text_az" to EditorField.TEXT_AZ,

    "mənbə" to EditorField.SOURCE,
    "menbe" to EditorField.SOURCE,
    "mə" to EditorField.SOURCE,
    "me" to EditorField.SOURCE,
    "source" to EditorField.SOURCE,

    "qeyd" to EditorField.NOTE,
    "qe" to EditorField.NOTE,
    "note" to EditorField.NOTE,

    "ad" to EditorField.NAME,
    "ad_az" to EditorField.NAME,
    "name" to EditorField.NAME,
    "name_az" to EditorField.NAME,

    "ad_ar" to EditorField.NAME_AR,
    "name_ar" to EditorField.NAME_AR,

    "slug" to EditorField.SLUG,

    "müəllif" to EditorField.AUTHOR,
    "muellif" to EditorField.AUTHOR,
    "author" to EditorField.AUTHOR,

    "təsvir" to EditorField.DESCRIPTION,
    "tesvir" to EditorField.DESCRIPTION,
    "izah" to EditorField.DESCRIPTION,
    "description" to EditorField.DESCRIPTION,
)

/**
 * Comfortably longer than the longest label above; anything before a separator that exceeds it
 * cannot be a label. Cheap first cut that keeps a whole sentence ending in a dot from being hashed.
 */
private const val MaxLabelLength = 16

/**
 * Turns a labelled clipboard block into the fields it fills.
 *
 * A line whose text before the first separator matches a known label opens a block; every following
 * line that does not itself open a block is appended to it, so multi-line Arabic pastes survive
 * intact. Text before the first label is dropped.
 *
 * The separator is a colon **or a plain dot** and labels are matched case-insensitively, so the
 * whole block can be typed in lowercase without ever reaching for the shift key. The two long
 * Azerbaijani labels have abbreviations to match:
 *
 * ```
 * ar. حَدَّثَنَا ...
 * ... ikinci sətir
 * az. Bizə rəvayət etdi
 * mə. Buxari 42
 * qe. bir qeyd
 * ```
 *
 * A separator alone does not make a label — the text before it has to match [ClipboardFormLabels]
 * exactly. That is what keeps an ordinary sentence (which is full of dots) from being cut into new
 * blocks.
 *
 * **Unlabelled text is split by script instead.** Bab headings get copied as a bare pair —
 *
 * ```
 * باب في أن الجنب إذا أراد أن ينام، عليه أن يتوضأ وضوءه للصلاة.
 * Cünub olan kimsənin ... haqqında bab.
 * ```
 *
 * — so when no label is found, every Arabic-script line goes to [arabicFallback] and everything
 * else to [latinFallback]. The caller picks the pair that fits the screen it is on: name/Arabic
 * name on the volume-book-bab editor, hadith text/translation on the hadith one.
 */
internal fun parseClipboardForm(
    raw: String,
    arabicFallback: EditorField,
    latinFallback: EditorField,
): Map<EditorField, String> {
    val blocks = LinkedHashMap<EditorField, StringBuilder>()
    var current: StringBuilder? = null

    // Mac-dəki mətn redaktorları blokun əvvəlinə BOM qoya bilir; qalsa ilk etiket tanınmır.
    val lines = raw.removePrefix("﻿").split("\r\n", "\n", "\r")

    lines.forEach { line ->
        val opened = line.openedBlock()
        if (opened != null) {
            val (field, value) = opened
            val builder = blocks.getOrPut(field) { StringBuilder() }
            // A repeated label continues the block it already opened rather than replacing it.
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(value)
            current = builder
        } else {
            current?.append('\n')?.append(line)
        }
    }

    val labelled = blocks.mapValues { it.value.toString().trim() }.filterValues { it.isNotEmpty() }
    if (labelled.isNotEmpty()) return labelled

    return lines.splitByScript(arabicFallback, latinFallback)
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
    val separator = indexOfFirst { it == ':' || it == '：' || it == '.' }
    if (separator <= 0 || separator > MaxLabelLength) return null
    val label = substring(0, separator).trim().trimStart('#').trim().lowercase()
    val field = ClipboardFormLabels[label] ?: return null
    return field to substring(separator + 1).trim()
}
