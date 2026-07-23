package com.cafarovceyxun.anamuslim.db.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.cafarovceyxun.anamuslim.compose.utils.appFallbackLanguageCodes
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahLocalizationEntity
import com.cafarovceyxun.anamuslim.db.interfaces.SurahMethods
import com.cafarovceyxun.anamuslim.utils.quran.AzerbaijaniSurahNames

data class SurahWithLocalizations(
    @Embedded
    val surah: SurahEntity,

    @Relation(
        parentColumn = "surah_no",
        entityColumn = "surah_no"
    )
    val localizations: List<SurahLocalizationEntity>
) : SurahMethods by surah {
    fun getCurrentName(): String {
        val codes = appFallbackLanguageCodes()
        val isAz = codes.any { it.startsWith("az") }

        if (isAz) {
            return AzerbaijaniSurahNames.getName(surah.surahNo).orEmpty()
        }

        for (code in codes) {
            val loc = localizations.firstOrNull { it.langCode == code && !it.name.isNullOrBlank() }
            if (loc?.name != null) return loc.name
        }

        return ""
    }

    fun getCurrentMeaning(): String {
        val codes = appFallbackLanguageCodes()
        val isAz = codes.any { it.startsWith("az") }

        if (isAz) {
            return AzerbaijaniSurahNames.getMeaning(surah.surahNo).orEmpty()
        }

        for (code in codes) {
            val loc = localizations.firstOrNull { it.langCode == code && !it.meaning.isNullOrBlank() }
            if (loc?.meaning != null) return loc.meaning
        }

        return ""
    }
}
