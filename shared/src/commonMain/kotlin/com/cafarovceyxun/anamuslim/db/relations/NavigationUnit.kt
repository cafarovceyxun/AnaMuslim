package com.cafarovceyxun.anamuslim.db.relations

import com.cafarovceyxun.anamuslim.db.entities.quran.NavigationType

data class NavigationUnitRange(
    val surah: SurahWithLocalizations,
    val startAyah: Int,
    val endAyah: Int
)

data class NavigationUnit(
    val type: NavigationType,
    val unitNo: Int,
    val ranges: List<NavigationUnitRange>
)
