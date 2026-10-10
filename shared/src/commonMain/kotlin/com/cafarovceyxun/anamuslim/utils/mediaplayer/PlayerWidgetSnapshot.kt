package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.nextChapter
import com.cafarovceyxun.anamuslim.resources.previousChapter
import com.cafarovceyxun.anamuslim.resources.recitationPlayer
import com.cafarovceyxun.anamuslim.resources.strLabelNextVerse
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.resources.strLabelPreviousVerse
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strLabelTexts
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.resources.strTitleVerseRecitation
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.quran.QuranGlyphs
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString

/**
 * iOS qiraət pleyeri vidcetinin oxuduğu hazır məzmun.
 *
 * Namaz vidcetinin modeli ilə eynidir ([com.cafarovceyxun.anamuslim.utils.prayer.PrayerWidgetSnapshot]):
 * WidgetKit uzantısı ayrı prosesdir, nə Quran bazasını, nə qari kataloqunu, nə də tərcümələri görür —
 * tətbiq hazır sətirləri App Group-a yazır, uzantı yalnız çəkir. Yeni sətir lazım olsa **buraya**
 * əlavə et, uzantıya `Localizable.strings` yazma.
 *
 * Android vidceti bunu işlətmir: orada Glance eyni prosesdə məlumatı özü oxuyur
 * (`RecitationPlayerWidgetUi.kt`).
 */
@Serializable
data class PlayerWidgetSnapshot(
    val generatedAtMillis: Long,
    val chapterNo: Int,
    val verseNo: Int,
    /** `0` — Quran bazası hələ hazır deyil; uzantı onda irəliləmə zolağını gizlədir. */
    val verseCount: Int,
    /** Surə adının `suracon` şriftindəki qlifi — Android vidcetinin surə şəkli ilə eyni yazı. */
    val chapterGlyph: String,
    /** «Fatihə surəsi». */
    val title: String,
    /** «Ayə 2». */
    val verseLabel: String,
    val reciter: String,
    /**
     * Oxunan ayənin ərəbcə mətni (uthmani, hərəkələrlə) və əsas tərcüməsi — böyük vidcet üçün.
     * Boş ola bilər (baza/tərcümə hələ yüklənməyib); uzantı onda həmin sətri göstərmir.
     */
    val arabicText: String,
    val translation: String,
    val isPlaying: Boolean,
    /** Səs həll olunur və ya buferlənir — oxutma düyməsi gözləmə göstərir. */
    val isLoading: Boolean,
    val labels: Labels,
) {
    /** Düymələrin əlçatanlıq adları və qalereya başlığı — tətbiqin dilində. */
    @Serializable
    data class Labels(
        val widgetTitle: String,
        val play: String,
        val pause: String,
        val previousVerse: String,
        val nextVerse: String,
        val previousChapter: String,
        val nextChapter: String,
        /** Böyük vidcetin «ayə mətni» düyməsi (ərəbcə → tərcümə → ikisi). */
        val verseText: String = "",
    )
}

/** Pleyer vidcetinin göstərdiyi ayə mətni — hər iki boş ola bilər (baza/tərcümə hələ hazır deyil). */
data class PlayerWidgetVerseText(val arabic: String, val translation: String)

/**
 * Oxunan ayənin ərəbcə mətni və əsas tərcüməsi, vidcet üçün təmizlənmiş.
 *
 * iOS ([PlayerWidgetSnapshotBuilder]) və Android (`RecitationPlayerWidgetUi.kt`) eyni funksiyanı
 * çağırır ki, iki platformanın vidceti eyni mətni göstərsin. Ərəbcə həmişə uthmani yazısıdır,
 * hərəkələrlə ([QuranRepository.getVerseTextsForAyahs] — KFQPC yazıları yalnız öz şriftləri ilə
 * oxunur); tərcümədən haşiyə işarələri və «-1: » kimi nömrə prefiksi çıxarılır.
 */
suspend fun loadPlayerWidgetVerseText(
    repository: QuranRepository,
    chapterNo: Int,
    verseNo: Int,
): PlayerWidgetVerseText {
    val ayahId = QuranMeta.getAyahId(chapterNo, verseNo)
    val arabic = runCatching {
        repository.getVerseTextsForAyahs(listOf(ayahId))[ayahId].orEmpty()
    }.getOrDefault("")
    val translation = runCatching {
        QuranTranslationFactory().use { factory ->
            factory.getTranslationsSingleSlugVerse(
                ReaderPreferences.primaryTranslationSlug(),
                chapterNo,
                verseNo,
            )?.text
        }
    }.getOrNull()
        ?.let { StringUtils.htmlToPlainText(FOOTNOTE_TAG.replace(it, "")).trim() }
        ?.let { VERSE_PREFIX.replace(it, "") }
        .orEmpty()

    // Uzun ayələr (məs. 2:282) vidcetə onsuz da sığmır — iOS-da App Group-dakı snapshot kiçik qalsın.
    return PlayerWidgetVerseText(arabic.take(MAX_TEXT_LENGTH), translation.take(MAX_TEXT_LENGTH))
}

private const val MAX_TEXT_LENGTH = 600

/** Tərcümədəki `<fn>…</fn>` haşiyə işarələri — oxucudakı kimi mətndən çıxarılır. */
private val FOOTNOTE_TAG = Regex("""(?s)<fn.*?>(.*?)<.*?fn>""")

/** Mətnin başındakı «-1: » kimi ayə nömrəsi — `VerseShareSheet`/`DailyContentFactory` ilə eyni qayda. */
private val VERSE_PREFIX = Regex("""^\s*-?\d+\s*[:.]\s*""")

object PlayerWidgetSnapshotBuilder {

    /**
     * [verse] — pleyerin cari ayəsi; pleyer hələ yaradılmayıbsa son dinlənən ayə (Android-in
     * `currentRecitationVerse()`-i ilə eyni qayda: soyuq ana ekran da həqiqi bir şey göstərsin).
     */
    suspend fun build(
        verse: ChapterVersePair,
        isPlaying: Boolean,
        isLoading: Boolean,
    ): PlayerWidgetSnapshot {
        val target = verse.takeIf { it.isValid } ?: ChapterVersePair(1, 1)
        val repository = RepositoryProvider.quranRepository

        val chapterName = repository.getChapterName(target.chapterNo)
            .ifBlank { target.chapterNo.toString() }
        val reciter = RecitationModelManager.getCurrentReciterNameForAudioOption()
            .ifBlank { getString(Res.string.strTitleVerseRecitation) }

        val verseText = loadPlayerWidgetVerseText(repository, target.chapterNo, target.verseNo)

        return PlayerWidgetSnapshot(
            generatedAtMillis = currentEpochMillis(),
            chapterNo = target.chapterNo,
            verseNo = target.verseNo,
            verseCount = repository.getChapterVerseCount(target.chapterNo),
            chapterGlyph = QuranGlyphs.Chapter.get(target.chapterNo),
            title = getString(Res.string.strLabelSurah, chapterName),
            verseLabel = getString(Res.string.strLabelVerseNo, target.verseNo),
            reciter = reciter,
            arabicText = verseText.arabic,
            translation = verseText.translation,
            isPlaying = isPlaying,
            isLoading = isLoading,
            labels = PlayerWidgetSnapshot.Labels(
                widgetTitle = getString(Res.string.recitationPlayer),
                play = getString(Res.string.strLabelPlay),
                pause = getString(Res.string.strLabelPause),
                previousVerse = getString(Res.string.strLabelPreviousVerse),
                nextVerse = getString(Res.string.strLabelNextVerse),
                previousChapter = getString(Res.string.previousChapter),
                nextChapter = getString(Res.string.nextChapter),
                verseText = getString(Res.string.strLabelTexts),
            ),
        )
    }
}
