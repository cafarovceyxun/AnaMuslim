package com.cafarovceyxun.anamuslim.utils.reader

import kotlinx.serialization.Serializable
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.db.entities.quran.RevelationType

@Serializable
enum class ReaderChapterRevelationFilter {
    any, meccan, medinan
}

@Serializable
enum class ReaderChapterSajdaFilter {
    any, withSajda, withoutSajda
}

@Serializable
enum class ReaderChapterLengthFilter {
    any, short, medium, long
}

@Serializable
data class ReaderChapterIndexFilters(
    val revelation: ReaderChapterRevelationFilter = ReaderChapterRevelationFilter.any,
    val sajda: ReaderChapterSajdaFilter = ReaderChapterSajdaFilter.any,
    val length: ReaderChapterLengthFilter = ReaderChapterLengthFilter.any,
) {
    fun isDefault(): Boolean = this == Default

    companion object {
        val Default = ReaderChapterIndexFilters()
    }
}

fun List<SurahWithLocalizations>.filteredByChapterIndex(
    filters: ReaderChapterIndexFilters,
    surahNosWithSajdah: Set<Int>
): List<SurahWithLocalizations> {
    return filter { item ->
        val surah = item.surah
        
        val revMatch = when (filters.revelation) {
            ReaderChapterRevelationFilter.any -> true
            ReaderChapterRevelationFilter.meccan -> surah.revelationType == RevelationType.meccan
            ReaderChapterRevelationFilter.medinan -> surah.revelationType == RevelationType.medinan
        }
        
        val sajdaMatch = when (filters.sajda) {
            ReaderChapterSajdaFilter.any -> true
            ReaderChapterSajdaFilter.withSajda -> surahNosWithSajdah.contains(surah.surahNo)
            ReaderChapterSajdaFilter.withoutSajda -> !surahNosWithSajdah.contains(surah.surahNo)
        }
        
        val lengthMatch = when (filters.length) {
            ReaderChapterLengthFilter.any -> true
            ReaderChapterLengthFilter.short -> surah.ayahCount <= 20
            ReaderChapterLengthFilter.medium -> surah.ayahCount in 21..100
            ReaderChapterLengthFilter.long -> surah.ayahCount > 100
        }
        
        revMatch && sajdaMatch && lengthMatch
    }
}
