package com.cafarovceyxun.anamuslim.utils.reader.factory

import android.content.Context
import android.content.Intent
import com.cafarovceyxun.anamuslim.activities.MainActivity
import com.cafarovceyxun.anamuslim.activities.reference.ActivityReference
import com.cafarovceyxun.anamuslim.components.ReferenceVerseModel
import com.cafarovceyxun.anamuslim.components.toBundle
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.utils.reader.toIntent
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptVariant
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams

object ReaderFactory {
    fun startEmptyReader(context: Context) {
        context.startActivity(Intent().setClass(context, MainActivity::class.java).apply {
            action = INTENT_ACTION_OPEN_READER
        })
    }

    fun startJuz(context: Context, juzNo: Int) {
        context.startActivity(
            prepareJuzIntent(juzNo).setClass(
                context,
                MainActivity::class.java
            ).apply {
                action = INTENT_ACTION_OPEN_READER
            }
        )
    }

    fun startHizb(context: Context, hizbNo: Int) {
        context.startActivity(
            prepareHizbIntent(hizbNo).setClass(
                context,
                MainActivity::class.java
            ).apply {
                action = INTENT_ACTION_OPEN_READER
            }
        )
    }

    suspend fun startMushafPage(context: Context, pageNo: Int) {
        val mushafCode = ReaderPreferences.getQuranScript()
        val variant = ReaderPreferences.getQuranScriptVariant()

        context.startActivity(
            ReaderLaunchParams(
                data = ReaderIntentData.MushafPage(
                    mushafCode = mushafCode,
                    mushafVariant = variant,
                    pageNo = pageNo,
                ),
                readerMode = ReaderMode.Reading,
            ).toIntent().setClass(context, MainActivity::class.java).apply {
                action = INTENT_ACTION_OPEN_READER
            }
        )
    }

    fun startChapter(context: Context, chapterNo: Int) {
        context.startActivity(
            prepareChapterIntent(chapterNo).setClass(context, MainActivity::class.java).apply {
                action = INTENT_ACTION_OPEN_READER
            }
        )
    }

    @JvmStatic
    fun startChapter(
        context: Context,
        translSlugs: Array<String>,
        saveTranslChanges: Boolean,
        chapterNo: Int
    ) {
        val params = ReaderLaunchParams(
            data = ReaderIntentData.FullChapter(chapterNo),
            slugs = translSlugs.toSet(),
        )
        context.startActivity(
            params.toIntent().setClass(context, MainActivity::class.java).apply {
                action = INTENT_ACTION_OPEN_READER
            }
        )
    }

    @JvmStatic
    fun startVerse(context: Context, chapterNo: Int, verseNo: Int) {
        context.startActivity(
            prepareSingleVerseIntent(chapterNo, verseNo)
                .setClass(context, MainActivity::class.java).apply {
                    action = INTENT_ACTION_OPEN_READER
                }
        )
    }

    fun startVerseRange(context: Context, chapterNo: Int, fromVerse: Int, toVerse: Int) {
        context.startActivity(
            prepareVerseRangeIntent(chapterNo, fromVerse, toVerse)
                .setClass(context, MainActivity::class.java).apply {
                    action = INTENT_ACTION_OPEN_READER
                }
        )
    }

    @JvmStatic
    fun startVerseRange(context: Context, chapterNo: Int, range: Pair<Int, Int>) {
        startVerseRange(context, chapterNo, range.first, range.second)
    }

    @JvmStatic
    fun prepareJuzIntent(juzNo: Int): Intent {
        return ReaderLaunchParams(
            data = ReaderIntentData.FullJuz(juzNo),
            readerMode = ReaderMode.VerseByVerse
        ).toIntent()
    }

    @JvmStatic
    fun prepareHizbIntent(hizbNo: Int): Intent {
        return ReaderLaunchParams(
            data = ReaderIntentData.FullHizb(hizbNo),
            readerMode = ReaderMode.VerseByVerse
        ).toIntent()
    }

    @JvmStatic
    fun prepareChapterIntent(chapterNo: Int): Intent {
        return ReaderLaunchParams(
            data = ReaderIntentData.FullChapter(chapterNo),
            readerMode = ReaderMode.VerseByVerse
        ).toIntent()
    }

    @JvmStatic
    fun prepareSingleVerseIntent(chapterNo: Int, verseNo: Int): Intent {
        return ReaderLaunchParams(
            data = ReaderIntentData.FullChapter(chapterNo, ChapterVersePair(chapterNo, verseNo)),
            readerMode = ReaderMode.VerseByVerse
        ).toIntent()
    }

    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, fromVerse: Int, toVerse: Int): Intent {
        return ReaderLaunchParams(
            data = ReaderIntentData.FullChapter(chapterNo, ChapterVersePair(chapterNo, fromVerse)),
            readerMode = ReaderMode.VerseByVerse
        ).toIntent()
    }

    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, range: Pair<Int, Int>): Intent {
        return prepareVerseRangeIntent(chapterNo, range.first, range.second)
    }

    fun startReferenceVerse(context: Context, referenceVerseModel: ReferenceVerseModel) {
        val intent = prepareReferenceVerseIntent(referenceVerseModel)
        intent.setClass(context, ActivityReference::class.java)
        context.startActivity(intent)
    }

    fun prepareReferenceVerseIntent(referenceVerseModel: ReferenceVerseModel): Intent {
        return Intent().apply {
            putExtras(referenceVerseModel.toBundle())
        }
    }

    fun prepareLastVersesIntent(
        readType: ReadType,
        readerMode: ReaderMode?,
        verse: ChapterVersePair,
        divisionNo: Int?,
    ): Intent? {
        val data = when (readType) {
            ReadType.Chapter -> {
                if (!QuranMeta.isChapterValid(verse.chapterNo)) return null
                ReaderIntentData.FullChapter(verse.chapterNo, verse)
            }

            ReadType.Juz -> {
                if (!QuranMeta.isJuzValid(divisionNo)) return null
                ReaderIntentData.FullJuz(divisionNo!!, verse)
            }

            ReadType.Hizb -> {
                if (!QuranMeta.isHizbValid(divisionNo)) return null
                ReaderIntentData.FullHizb(divisionNo!!, verse)
            }
        }

        return ReaderLaunchParams(data = data, readerMode = readerMode).toIntent()
    }

    fun prepareHistoryIntent(entity: ReadHistoryEntity): Intent? {
        val readType = ReadType.fromValue(entity.readType)
        val readerMode = ReaderMode.fromValue(entity.readerMode)
        val pageNo = entity.pageNo

        if ((readerMode == ReaderMode.Reading || readerMode == ReaderMode.Translation) &&
            pageNo != null && pageNo > 0 && entity.mushafCode != null
        ) {
            return ReaderLaunchParams(
                data = ReaderIntentData.MushafPage(
                    mushafCode = entity.mushafCode,
                    mushafVariant = QuranScriptVariant.fromValue(entity.mushafVariant),
                    pageNo = pageNo,
                    fallbackChapterNo = entity.chapterNo,
                    fallbackVerseNo = entity.fromVerseNo,
                ),
                readerMode = readerMode,
            ).toIntent()
        }

        return prepareLastVersesIntent(
            readType = readType,
            readerMode = readerMode,
            verse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo),
            divisionNo = entity.divisionNo,
        )
    }
}
