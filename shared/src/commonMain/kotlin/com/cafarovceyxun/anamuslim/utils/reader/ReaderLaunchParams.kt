package com.cafarovceyxun.anamuslim.utils.reader

import com.cafarovceyxun.anamuslim.PlatformSerializable
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode

enum class ReadType(val value: String) {
    Chapter("chapter"),
    Juz("juz"),
    Hizb("hizb");

    companion object {
        fun fromValue(value: String?): ReadType {
            return entries.find { it.value == value } ?: Chapter
        }

        fun fromLegacyInt(type: Int): ReadType = when (type) {
            0x5 -> Juz
            else -> Chapter
        }
    }
}

// `PlatformSerializable` here (and on ReaderIntentData) is what lets Android's `MainScreen` keep the
// launch params in `rememberSaveable`: without it a rotation drops them and the reader silently
// falls back to chapter 1 (Al-Fatiha).
sealed class ReaderIntentData : PlatformSerializable {
    open val initialVerse: ChapterVersePair? get() = null

    data class FullChapter(
        val chapterNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class FullJuz(
        val juzNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class FullHizb(
        val hizbNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class MushafPage(
        val mushafCode: String?,
        val mushafVariant: QuranScriptVariant?,
        val pageNo: Int,
        val fallbackChapterNo: Int = 0,
        val fallbackVerseNo: Int = 0,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()
}

data class ReaderLaunchParams(
    val data: ReaderIntentData,
    val readerMode: ReaderMode? = null,
    val slugs: Set<String>? = null,
) : PlatformSerializable {
    fun toInitSignature(): String {
        val slugsPart = slugs
            ?.toList()
            ?.sorted()
            ?.joinToString(separator = ",")
            ?: "-"

        val modePart = readerMode?.value ?: "-"

        val dataPart = when (val d = data) {
            is ReaderIntentData.FullChapter -> "chapter:${d.chapterNo}"
            is ReaderIntentData.FullJuz -> "juz:${d.juzNo}"
            is ReaderIntentData.FullHizb -> "hizb:${d.hizbNo}"
            is ReaderIntentData.MushafPage -> "mushaf:${d.mushafCode}|${d.mushafVariant}|${d.pageNo}"
        }

        return "$dataPart|mode:$modePart|slugs:$slugsPart"
    }

    companion object {
        /** Intent-extra keys shared with external intent builders (deep links, app intents). */
        const val EXTERNAL_KEY_READER_MODE = "reader.mode"
        const val EXTERNAL_KEY_TRANSL_SLUGS = "reader.translation_slugs"
    }
}
