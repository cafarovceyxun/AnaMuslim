package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Növbəyə yazılacaq sətri **cihazdakı** bazadan qurur.
 *
 * Bir yerdədir, çünki eyni sətir iki yerdən yaranır: admin panelindən (surə/ayə nömrəsi yazılır) və
 * oxucudakı «günün ayəsi» düyməsindən. Ayrı-ayrı qurulanda aralığın yalnız ilk ayəsinin mətni
 * yazılırdı — kart və bildiriş isə **bütün** aralığı göstərməlidir.
 */
object DailyContentFactory {

    /**
     * [verseStart]..[verseEnd] aralığı üçün növbə sətri; ayələrdən biri tapılmasa null.
     * [verseEnd] null və ya başlanğıcdan böyük deyilsə tək ayə qurulur.
     */
    suspend fun verseContent(
        chapterNo: Int,
        verseStart: Int,
        verseEnd: Int?,
    ): DailyContent? = withContext(Dispatchers.IO) {
        val last = verseEnd?.takeIf { it > verseStart }
        val numbers = verseStart..(last ?: verseStart)
        val quranRepository = RepositoryProvider.quranRepository

        val verses = numbers.map { number ->
            quranRepository.getVerseWithDetails(chapterNo, number, arabicEnabled = true)
                ?: return@withContext null
        }

        var slugs = ReaderPreferences.getTranslations()
        if (slugs.isEmpty()) slugs = TranslUtils.defaultTranslationSlugs()

        val factory = QuranTranslationFactory()
        val translations = try {
            numbers.map { number ->
                factory.getTranslationsSingleVerse(slugs, chapterNo, number)
                    .firstOrNull()
                    ?.let { StringUtils.removeHTML(it.text, false) }
                    .orEmpty()
            }
        } finally {
            factory.close()
        }

        val chapterName = verses.first().chapter.getCurrentName()

        DailyContent(
            content_type = DailyContent.CONTENT_TYPE_VERSE,
            chapter_no = chapterNo,
            verse_no = verseStart,
            verse_end = last,
            text_ar = verses.joinToString(" ") { verse ->
                verse.words.joinToString(" ") { it.text }
            },
            text_az = translations.filter { it.isNotBlank() }.joinToString(" "),
            source = if (last != null) {
                "$chapterName $verseStart-$last"
            } else {
                "$chapterName $verseStart"
            },
        )
    }

    /**
     * Növbə elementinin **göstəriləcək** halı — story, paylaşma şəkli və kart eyni mətni alsın deyə.
     *
     * Ayə üçün mətn mümkün olduqda cihazdakı bazadan oxunur (istifadəçinin seçdiyi tərcümə ilə,
     * aralığın hamısı); alınmasa adminin yazdığı mətnə düşür. Hədisdə isə çıxarış varsa yalnız o
     * göstərilir — «hədisin müəyyən bir qismini seç» tələbi buradan keçir.
     */
    suspend fun display(content: DailyContent): DailyContentDisplay = withContext(Dispatchers.IO) {
        if (content.isHadith) {
            return@withContext DailyContentDisplay(
                arabic = content.displayTextAr,
                translation = content.displayTextAz,
                reference = content.source.orEmpty(),
            )
        }

        val chapterNo = content.chapter_no
        val numbers = content.verseNumbers

        if (chapterNo == null || numbers.isEmpty()) {
            return@withContext DailyContentDisplay(
                arabic = content.displayTextAr,
                translation = content.displayTextAz,
                reference = content.source.orEmpty(),
            )
        }

        val rebuilt = verseContent(chapterNo, numbers.first(), numbers.last())

        DailyContentDisplay(
            arabic = rebuilt?.text_ar?.takeIf { it.isNotBlank() } ?: content.displayTextAr,
            // Adminin yazdığı mətn ayə nömrəsi prefiksi ilə gəlir («-12: …»); istinad sətri onsuz
            // da nömrəni göstərir, ona görə şəkildə və story-də prefiks atılır.
            translation = rebuilt?.text_az?.takeIf { it.isNotBlank() }
                ?: versePrefixPattern.replace(content.displayTextAz, ""),
            reference = content.source?.takeIf { it.isNotBlank() }
                ?: rebuilt?.source.orEmpty(),
        )
    }

}

/** Story, paylaşma şəkli və kart üçün hazır mətnlər. */
data class DailyContentDisplay(
    val arabic: String,
    val translation: String,
    val reference: String,
)

/** «-12: » kimi ayə nömrəsi prefiksi — `VerseShareSheet`-dəki ilə eyni qayda. */
private val versePrefixPattern = Regex("""^\s*-?\d+\s*[:.]\s*""")
