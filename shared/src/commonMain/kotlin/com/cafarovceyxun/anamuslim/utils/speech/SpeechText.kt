package com.cafarovceyxun.anamuslim.utils.speech

/**
 * Bazadakı hədis mətnini səsləndiriləcək mətnə çevirir və onu cihazda sintez üçün parçalara bölür.
 *
 * Qaydalar `tools/tts/normalize.py`-dakı `normalize_hadith`-in köçürməsidir — onlar 2026-08-24-də
 * bazadakı 1487 hədisin real formaları üzərində sayılıb (səbəblər həmin faylın başlığındadır). Model
 * də elə bu formada mətnlə öyrədilib, ona görə iki yer bir-birindən ayrılmamalıdır.
 *
 * ⚠️ Python-dakı `\w` və geriyə baxış (`(?<=…)`) burada **regex ilə yazılmayıb**: JVM-də `\w` yalnız
 * ASCII-dir (`ə`, `ş`, `ı`… onu keçmir), Kotlin/Native regex-i isə `\p{L}`-i fərqli həll edir. Ona
 * görə hərf yoxlaması `Char.isLetter()` ilə, simvol-simvol aparılır — iki platformada eyni nəticə.
 */
object SpeechText {

    private val invisible = mapOf(
        ' ' to " ", '​' to "", '­' to "", '�' to "", '۩' to "",
        '–' to "-", '—' to "-", '“' to "\"", '”' to "\"",
        '’' to "'", '‘' to "'", '\t' to " ",
    )

    /** `(Buxari-4981)`, `(Əhməd-7813)` — mənbə istinadı, oxunmur. */
    private val sourceRef = Regex("""\(\s*[^()\d]{2,30}-\d{1,6}\s*\)""")
    private val hadithPrefix = Regex("""^\s*-?\s*\d{1,5}\s*[.)]\s*""")
    private val footnoteInParens = Regex("""\(\s*\d{1,2}\s*\)""")
    private val spaceBeforePunct = Regex("""\s+([,.;:!?])""")
    private val doubleComma = Regex(""",\s*,""")
    private val multiSpace = Regex("""[  - ]{2,}""")
    private val emptyParens = Regex("""\(\s*\)""")

    /** Hədis mətni → oxunacaq mətn. Mətn yoxdursa boş sətir qayıdır. */
    fun hadith(raw: String): String {
        var text = buildString(raw.length) {
            for (c in raw.replace("\r\n", "\n").replace('\r', '\n')) append(invisible[c] ?: c.toString())
        }
        // Ərəb yazısı (tərcümə içində sitat) azərbaycanca modeldə hərf-hərf adlarla oxunur — atılır.
        text = buildString(text.length) { for (c in text) if (c !in '\u0600'..'\u06FF' && c !in '\u0750'..'\u077F' && c !in '\uFB50'..'\uFDFF' && c !in '\uFE70'..'\uFEFF') append(c) }
        text = sourceRef.replace(text, "")
        text = text.replace("{", "").replace("}", "")
        text = hadithPrefix.replace(text, "")
        if (text.isBlank()) return ""
        text = footnoteInParens.replace(text, "")
        text = stripGluedFootnotes(text)
        text = unwrapParens(text)
        return finish(text)
    }

    /**
     * Sözə və ya bağlanan dırnağa **bitişik** bir-iki rəqəmli say haşiyə nişanıdır (`görməyi1`,
     * `”Rainə”1`) — silinməsə model onu «bir» deyə oxuyur. Boşluqla ayrılan həqiqi rəqəmlər
     * (`3 il ilə 9 il`) toxunulmaz qalır.
     */
    private fun stripGluedFootnotes(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c.isDigit() && i > 0) {
                val prev = text[i - 1]
                var end = i
                while (end < text.length && text[end].isDigit()) end++
                val runLength = end - i
                val next = text.getOrNull(end)
                val glued = prev.isLetter() || prev == '"' || prev == '\'' || prev == ')'
                val standalone = next == null || !(next.isLetterOrDigit() || next == '_')
                if (glued && runLength <= 2 && standalone) {
                    i = end
                    continue
                }
                out.append(text, i, end)
                i = end
                continue
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    /**
     * Mötərizələri açır: `mələk(lər)` → `mələklər` (bitişik şəkilçi), `həmd(-sənalar)` →
     * `həmd, sənalar` (tire ilə başlayan izah), qalanında isə mötərizə sadəcə silinir — süni vergül
     * TTS-də yersiz fasilə yaradır. İç-içə və 200 simvoldan uzun mötərizəyə toxunulmur.
     */
    private fun unwrapParens(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '(') {
                val close = text.indexOf(')', i + 1)
                val inner = if (close > i) text.substring(i + 1, close) else null
                if (inner != null && inner.isNotEmpty() && inner.length <= 200 && '(' !in inner) {
                    val prev = text.getOrNull(i - 1)
                    val glued = prev != null && (prev.isLetterOrDigit() || prev == '_')
                    val replacement = when {
                        glued && inner.startsWith("-") && inner.length <= 81 ->
                            ", " + inner.drop(1).trim()
                        glued && inner.length <= 6 && inner.all { it.isLetterOrDigit() || it == '_' } ->
                            inner
                        else -> inner.trim(' ', ',', ';')
                    }
                    out.append(replacement)
                    i = close + 1
                    continue
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    private fun finish(input: String): String {
        // Dırnaqdan və ya durğu işarəsindən sonra sözə bitişik tire («"-Bəli», «.-Bəli») danışıq
        // nişanıdır, oxunmur — yerinə fasilə qalsın.
        val dashed = StringBuilder(input.length)
        for (i in input.indices) {
            val c = input[i]
            val prev = input.getOrNull(i - 1)
            val next = input.getOrNull(i + 1)
            if (c == '-' && prev != null && next != null && next.isLetterOrDigit() &&
                (prev == '"' || prev == '\'' || prev in ".!?,;")
            ) {
                dashed.append(if (prev == '"' || prev == '\'') ", " else " ")
            } else {
                dashed.append(c)
            }
        }
        var text = dashed.toString()
        text = emptyParens.replace(text, "")
        text = multiSpace.replace(text, " ")
        text = spaceBeforePunct.replace(text, "$1")
        text = doubleComma.replace(text, ",")
        text = text.split('\n')
            .map { it.trim(' ', '-') }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .trim()
        if (text.isNotEmpty() && text.last() !in ".!?\"'") text += "."
        return text
    }

    /**
     * Sintez parçaları — cümlə sərhədində, hər biri ən çox [maxChars] simvol.
     *
     * Niyə parçalanır: telefon bütöv hədisi bir dəfəyə sintez etsə, uzun hədisdə səs bir neçə saniyə
     * gecikərdi. Parça-parça işlədikdə birinci cümlə dərhal səslənir, növbəti isə o çalınarkən
     * hazırlanır. Limitdən uzun cümlə (hədisdə isnad zənciri adətən elədir) əvvəl `:`/`;`, sonra
     * vergüldən bölünür; çox qısa cümlələr (`Bəli.`) növbətisi ilə birləşir ki, intonasiya qırılmasın.
     */
    fun chunks(text: String, maxChars: Int = 220, minChars: Int = 24): List<SpeechChunk> {
        val sentences = mutableListOf<SpeechChunk>()
        for (line in text.split('\n')) {
            for (sentence in splitAfter(line, ".!?…")) {
                if (sentence.length <= maxChars) {
                    sentences += SpeechChunk(sentence, SpeechPause.Sentence)
                } else {
                    sentences += splitLong(sentence, maxChars)
                }
            }
        }
        // Qısa parçanı növbətisinə birləşdir (eyni sətir daxilində olub-olmamasından asılı olmayaraq).
        val merged = mutableListOf<SpeechChunk>()
        var pending: SpeechChunk? = null
        for (chunk in sentences) {
            val joined = pending?.let { SpeechChunk(it.text + " " + chunk.text, chunk.pause) } ?: chunk
            if (joined.text.length < minChars) {
                pending = joined
            } else {
                merged += joined
                pending = null
            }
        }
        // Sonda qalan qısa parça əvvəlkinə qoşulur (yer varsa), yoxsa tək gedir.
        pending?.let { rest ->
            val last = merged.lastOrNull()
            if (last != null && last.text.length + rest.text.length < maxChars) {
                merged[merged.lastIndex] = SpeechChunk(last.text + " " + rest.text, rest.pause)
            } else {
                merged += rest
            }
        }
        return merged
    }

    private fun splitLong(sentence: String, maxChars: Int): List<SpeechChunk> {
        val clauses = splitAfter(sentence, ":;").flatMap { clause ->
            if (clause.length <= maxChars) listOf(clause) else splitAfter(clause, ",")
        }
        val out = mutableListOf<SpeechChunk>()
        var buffer = ""
        for (clause in clauses) {
            val candidate = if (buffer.isEmpty()) clause else "$buffer $clause"
            if (candidate.length <= maxChars || buffer.isEmpty()) {
                buffer = candidate
            } else {
                out += SpeechChunk(buffer, SpeechPause.Clause)
                buffer = clause
            }
        }
        if (buffer.isNotEmpty()) out += SpeechChunk(buffer, SpeechPause.Sentence)
        return out
    }

    /** [text]-i [terminators]-dan biri + boşluq olan yerlərdən bölür, işarəni parçada saxlayır. */
    private fun splitAfter(text: String, terminators: String): List<String> {
        val parts = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            if (text[i] in terminators) {
                var end = i + 1
                // `?!`, `...` və bağlanan dırnaq işarənin bir hissəsidir.
                while (end < text.length && (text[end] in terminators || text[end] == '"' || text[end] == '\'')) end++
                if (end >= text.length || text[end].isWhitespace()) {
                    text.substring(start, end).trim().takeIf { it.isNotEmpty() }?.let(parts::add)
                    start = end
                }
                i = end
                continue
            }
            i++
        }
        text.substring(start).trim().takeIf { it.isNotEmpty() }?.let(parts::add)
        return parts
    }
}

/** Parçadan sonra nə qədər susulsun — cümlə sonu ilə vergül/iki nöqtə fərqli fasilədir. */
enum class SpeechPause(val millis: Int) {
    Clause(120),
    Sentence(320),
}

data class SpeechChunk(val text: String, val pause: SpeechPause)
