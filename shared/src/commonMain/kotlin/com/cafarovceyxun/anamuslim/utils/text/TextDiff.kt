package com.cafarovceyxun.anamuslim.utils.text

/**
 * Moderasiya panelində «hazırkı mətn» ilə «təklif olunan mətn» arasındakı fərqi tapır.
 *
 * Nəticə iki tərəf üçün ayrıca **interval siyahısıdır**: [TextDiffResult.oldSpans] köhnə mətndə
 * silinən/dəyişən yerlər, [TextDiffResult.newSpans] yeni mətndə əlavə olunan/dəyişən yerlər.
 * İntervallar `[start, end)` şəklindədir və birbaşa `AnnotatedString`-ə `SpanStyle` kimi tətbiq
 * olunur.
 *
 * Üç addım:
 *  1. Simvol səviyyəsində ortaq prefiks/suffiks kəsilir, sonra **söz sərhədinə** düzləşdirilir.
 *     Düzləşdirmə olmasa orta hissə sözün ortasından başlayır və növbəti addım yarımçıq sözlərlə
 *     işləyir.
 *  2. Qalan orta hissə sözlərə (və boşluq bloklarına) bölünüb LCS ilə tutuşdurulur.
 *  3. Bir söz bir sözlə əvəzlənibsə və sözlər **bir-birinə oxşayırsa** ([MIN_SIMILARITY]) fərq
 *     simvol səviyyəsində daraldılır — «255-ci» → «256-cı» halında yalnız `5`/`6` və `i`/`ı`
 *     boyanır. Oxşamayan sözlər (məs. «gün» → «zaman») bütöv boyanır: təsadüfi ortaq hərflərə görə
 *     sözü parçalamaq oxunuşu çətinləşdirir.
 *
 * Mühafizə: orta hissə [MAX_TOKENS] tokendən uzundursa LCS işlədilmir (O(n·m) cədvəl əsas ipdə
 * donma verə bilər) — bütün orta hissə tək interval kimi qeyd olunur. Fərq yenə görünür, sadəcə
 * daha kobud olur.
 */
data class DiffSpan(val start: Int, val end: Int)

data class TextDiffResult(
    val oldSpans: List<DiffSpan>,
    val newSpans: List<DiffSpan>,
) {
    val hasChanges: Boolean get() = oldSpans.isNotEmpty() || newSpans.isNotEmpty()

    companion object {
        val Identical = TextDiffResult(emptyList(), emptyList())
    }
}

/** LCS cədvəlinin yuxarı həddi — bundan uzun mətndə orta hissə bütöv vurğulanır. */
private const val MAX_TOKENS = 400

/** Simvol səviyyəsində daraltmanın həddi — daha uzun söz bütöv boyanır. */
private const val MAX_REFINE_CHARS = 200

/** Bundan az oxşayan söz cütü simvola bölünmür, bütöv boyanır. */
private const val MIN_SIMILARITY = 0.5

fun diffText(old: String, new: String): TextDiffResult {
    if (old == new) return TextDiffResult.Identical
    if (old.isEmpty()) return finish(old, new, emptyList(), listOf(DiffSpan(0, new.length)))
    if (new.isEmpty()) return finish(old, new, listOf(DiffSpan(0, old.length)), emptyList())

    val prefix = snapPrefixToWordStart(old, commonPrefixLength(old, new))
    val suffix = snapSuffixToWordStart(old, commonSuffixLength(old, new, prefix))

    val oldMid = old.substring(prefix, old.length - suffix)
    val newMid = new.substring(prefix, new.length - suffix)

    // Bir tərəf tamamilə boşdursa bu, təmiz əlavə ya təmiz silmədir — bölməyə ehtiyac yoxdur.
    if (oldMid.isEmpty() || newMid.isEmpty()) {
        return finish(
            old, new,
            oldRanges = if (oldMid.isEmpty()) emptyList() else listOf(DiffSpan(prefix, prefix + oldMid.length)),
            newRanges = if (newMid.isEmpty()) emptyList() else listOf(DiffSpan(prefix, prefix + newMid.length)),
        )
    }

    val oldTokens = tokenize(oldMid)
    val newTokens = tokenize(newMid)

    if (oldTokens.size > MAX_TOKENS || newTokens.size > MAX_TOKENS) {
        return finish(
            old, new,
            oldRanges = listOf(DiffSpan(prefix, prefix + oldMid.length)),
            newRanges = listOf(DiffSpan(prefix, prefix + newMid.length)),
        )
    }

    val oldWords = oldTokens.map { oldMid.substring(it.start, it.end) }
    val newWords = newTokens.map { newMid.substring(it.start, it.end) }

    val oldRanges = ArrayList<DiffSpan>()
    val newRanges = ArrayList<DiffSpan>()

    for (block in diffTokens(oldWords, newWords).blocks) {
        val oldToken = block.oldRange.singleOrNull(oldTokens)
        val newToken = block.newRange.singleOrNull(newTokens)

        if (oldToken != null && newToken != null) {
            val refined = refineWordPair(
                oldMid.substring(oldToken.start, oldToken.end),
                newMid.substring(newToken.start, newToken.end),
            )
            if (refined != null) {
                refined.first.forEach { addSpan(oldRanges, prefix + oldToken.start + it.start, prefix + oldToken.start + it.end) }
                refined.second.forEach { addSpan(newRanges, prefix + newToken.start + it.start, prefix + newToken.start + it.end) }
                continue
            }
        }

        block.oldRange.toSpan(oldTokens)?.let { addSpan(oldRanges, prefix + it.start, prefix + it.end) }
        block.newRange.toSpan(newTokens)?.let { addSpan(newRanges, prefix + it.start, prefix + it.end) }
    }

    return finish(old, new, oldRanges, newRanges)
}

// ---------------------------------------------------------------------------------------------

private class Token(val start: Int, val end: Int)

private class TokenRange(val from: Int, val toExclusive: Int) {
    val isEmpty: Boolean get() = from >= toExclusive

    fun toSpan(tokens: List<Token>): DiffSpan? {
        if (isEmpty) return null
        return DiffSpan(tokens[from].start, tokens[toExclusive - 1].end)
    }

    fun singleOrNull(tokens: List<Token>): Token? =
        if (toExclusive - from == 1) tokens[from] else null
}

private class ChangedBlock(val oldRange: TokenRange, val newRange: TokenRange)

private class DiffOps(val lcsLength: Int, val blocks: List<ChangedBlock>)

/**
 * Boşluq blokları ilə söz bloklarını növbələşdirərək bölür. Söz tokeni ardınca gələn durğu
 * işarəsini də özündə saxlayır («ayədir.») — nöqtə dəyişəndə cüt bir sözlə tutuşur və 3-cü addım
 * onu tək simvola qədər daraldır.
 */
private fun tokenize(s: String): List<Token> {
    val out = ArrayList<Token>()
    var i = 0
    while (i < s.length) {
        val whitespace = s[i].isWhitespace()
        var j = i + 1
        while (j < s.length && s[j].isWhitespace() == whitespace) j++
        out.add(Token(i, j))
        i = j
    }
    return out
}

private fun diffTokens(a: List<String>, b: List<String>): DiffOps {
    // dp[i][j] = a[i..] ilə b[j..] arasındakı ən uzun ortaq alt-ardıcıllığın uzunluğu.
    val dp = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.indices.reversed()) {
        for (j in b.indices.reversed()) {
            dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1
            else if (dp[i + 1][j] >= dp[i][j + 1]) dp[i + 1][j] else dp[i][j + 1]
        }
    }

    val blocks = ArrayList<ChangedBlock>()
    var i = 0
    var j = 0
    var oldStart = 0
    var newStart = 0

    fun closeBlock(oldEnd: Int, newEnd: Int) {
        if (oldEnd > oldStart || newEnd > newStart) {
            blocks.add(ChangedBlock(TokenRange(oldStart, oldEnd), TokenRange(newStart, newEnd)))
        }
    }

    while (i < a.size && j < b.size) {
        if (a[i] == b[j]) {
            closeBlock(i, j)
            i++
            j++
            oldStart = i
            newStart = j
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            i++
        } else {
            j++
        }
    }
    closeBlock(a.size, b.size)

    return DiffOps(dp[0][0], blocks)
}

/** Söz cütünü simvollara bölür; oxşarlıq həddindən aşağıdırsa `null` (bütöv boyansın). */
private fun refineWordPair(oldText: String, newText: String): Pair<List<DiffSpan>, List<DiffSpan>>? {
    if (oldText.length > MAX_REFINE_CHARS || newText.length > MAX_REFINE_CHARS) return null

    val a = oldText.map { it.toString() }
    val b = newText.map { it.toString() }
    val ops = diffTokens(a, b)
    if (ops.lcsLength.toDouble() / maxOf(a.size, b.size) < MIN_SIMILARITY) return null

    val oldSpans = ops.blocks.mapNotNull { it.oldRange.toCharSpan() }
    val newSpans = ops.blocks.mapNotNull { it.newRange.toCharSpan() }
    return oldSpans to newSpans
}

/** Simvol səviyyəsində token indeksi elə simvol indeksidir. */
private fun TokenRange.toCharSpan(): DiffSpan? =
    if (isEmpty) null else DiffSpan(from, toExclusive)

private fun commonPrefixLength(a: String, b: String): Int {
    val max = minOf(a.length, b.length)
    var i = 0
    while (i < max && a[i] == b[i]) i++
    return i
}

private fun commonSuffixLength(a: String, b: String, prefix: Int): Int {
    val max = minOf(a.length, b.length) - prefix
    var i = 0
    while (i < max && a[a.length - 1 - i] == b[b.length - 1 - i]) i++
    return i
}

/** Prefiks söz ortasında bitibsə əvvəlki boşluğa qədər geri çəkilir. */
private fun snapPrefixToWordStart(a: String, prefix: Int): Int {
    var p = prefix
    while (p > 0 && !a[p - 1].isWhitespace()) p--
    return p
}

/** Suffiks söz ortasından başlayırsa boşluğa qədər qısaldılır (lazım gələrsə sıfıra qədər). */
private fun snapSuffixToWordStart(a: String, suffix: Int): Int {
    var s = suffix
    while (s > 0 && !a[a.length - s].isWhitespace()) s--
    return s
}

private fun addSpan(target: MutableList<DiffSpan>, start: Int, end: Int) {
    if (end <= start) return
    val last = target.lastOrNull()
    // Qonşu intervalları birləşdir — eyni sözün iki hissəsi ayrı-ayrı fon ləkəsi kimi görünməsin.
    if (last != null && start <= last.end) {
        target[target.size - 1] = DiffSpan(last.start, maxOf(last.end, end))
    } else {
        target.add(DiffSpan(start, end))
    }
}

/** Vurğunun kənarındakı boşluqları kəsir — boş sətirdə sarı ləkə qalmasın. */
private fun finish(
    old: String,
    new: String,
    oldRanges: List<DiffSpan>,
    newRanges: List<DiffSpan>,
): TextDiffResult = TextDiffResult(trim(old, oldRanges), trim(new, newRanges))

private fun trim(source: String, spans: List<DiffSpan>): List<DiffSpan> = spans.mapNotNull { span ->
    var start = span.start
    var end = span.end
    while (start < end && source[start].isWhitespace()) start++
    while (end > start && source[end - 1].isWhitespace()) end--
    if (end > start) DiffSpan(start, end) else null
}
