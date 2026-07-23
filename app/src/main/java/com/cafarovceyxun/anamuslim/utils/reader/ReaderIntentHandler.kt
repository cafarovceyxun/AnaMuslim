package com.cafarovceyxun.anamuslim.utils.reader

import android.content.Intent
import android.net.Uri
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory

object ReaderIntentHandler {

    fun validateIntent(intent: Intent): ReaderLaunchParams? {
        val action = intent.action ?: return null

        if (Intent.ACTION_VIEW == action) {
            val url = intent.data ?: return null

            if (url.host.equals("quran.com", ignoreCase = true)) {
                validateQuranComIntent(intent, url)
            }
        } else if (INTENT_ACTION_OPEN_READER.equals(action, ignoreCase = true)) {
            validateQuranAppIntent(intent)
        } else {
            return null
        }

        return try {
            ReaderLaunchParams.fromIntent(intent)
        } catch (e: Exception) {
            Log.saveError(e, "ReaderIntentHandler.validateIntent")
            null
        }
    }

    private fun validateQuranComIntent(intent: Intent, url: Uri) {
        val pathSegments = url.pathSegments

        try {
            if (pathSegments.size >= 2) {
                val firstSeg = pathSegments[0]
                val secondSeg = pathSegments[1]

                if (firstSeg.equals("juz", ignoreCase = true)) {
                    val juzNo = secondSeg.toInt()
                    intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo))
                } else {
                    val chapterNo = firstSeg.toInt()
                    val splits = secondSeg.split("-")
                    val verseRange = if (splits.size >= 2) {
                        Pair(splits[0].toInt(), splits[1].toInt())
                    } else {
                        val verseNo = splits[0].toInt()
                        Pair(verseNo, verseNo)
                    }
                    intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange))
                }
            } else if (pathSegments.size == 1) {
                var splits = pathSegments[0].split(":")
                val chapterNo = splits[0].toInt()

                if (splits.size >= 2) {
                    splits = splits[1].split("-")
                    val verseRange = if (splits.size >= 2) {
                        Pair(splits[0].toInt(), splits[1].toInt())
                    } else {
                        val verseNo = splits[0].toInt()
                        Pair(verseNo, verseNo)
                    }
                    intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange))
                } else {
                    intent.putExtras(ReaderFactory.prepareChapterIntent(chapterNo))
                }
            }

            if (url.queryParameterNames.contains("reading")) {
                val reading = url.getBooleanQueryParameter("reading", false)
                val mode = if (reading) ReaderMode.Reading else ReaderMode.VerseByVerse
                intent.putExtra(ReaderLaunchParams.EXTERNAL_KEY_READER_MODE, mode.value)
            }
        } catch (e: Exception) {
            Log.saveError(e, "validateQuranComIntent")
        }
    }

    private fun validateQuranAppIntent(intent: Intent) {
        val requestedTranslSlugs = intent.getStringArrayExtra("translations")
        if (requestedTranslSlugs != null) {
            intent.putExtra(
                ReaderLaunchParams.EXTERNAL_KEY_TRANSL_SLUGS,
                requestedTranslSlugs.toCollection(sortedSetOf())
            )
        }

        if (intent.getBooleanExtra("isJuz", false)) {
            val juzNo = intent.getIntExtra("juzNo", -1)
            if (juzNo != -1) intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo))
        } else {
            val chapterNo = intent.getIntExtra("chapterNo", -1)
            val verses = intent.getIntArrayExtra("verses")
            val verseNo = intent.getIntExtra("verseNo", -1)

            when {
                verses != null -> {
                    intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verses[0], verses[1]))
                }
                verseNo != -1 -> {
                    intent.putExtras(ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo))
                }
                chapterNo != -1 -> {
                    intent.putExtras(ReaderFactory.prepareChapterIntent(chapterNo))
                }
            }
        }
    }
}
