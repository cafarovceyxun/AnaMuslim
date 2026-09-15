package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Dua/Əsmaül Hüsnə ekranlarının **özünün edə bilmədiyi** naviqasiya: mənbəni açmaq.
 *
 * İki hərəkət də hədəfi platforma ekranıdır (Android-də `Activity`, paylaşılan hostda
 * `AppDestination`), ona görə host verir. Eyni forma [com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithActions]
 * və `ReaderActions`-dakı kimidir.
 *
 * ⚠️ Defolt **`null`-dur, no-op deyil** (CLAUDE.md, «inert default UI-ni azad etmir»): qoşulmamış
 * hostda «Hədisi aç» düyməsi **görünmür** — basılıb səssizcə heç nə etmir yerinə. Qaynaq vərəqinin
 * əsas məzmunu onsuz da vurğulanmış mətndir, düymə isə əlavədir.
 */
data class DuaActions(
    /** Hədisi öz ekranında açır. */
    val onOpenHadith: ((hadithId: Long) -> Unit)? = null,
    /** Oxucunu həmin ayədə açır. */
    val onOpenVerse: ((chapterNo: Int, verseNo: Int) -> Unit)? = null,
)

val LocalDuaActions = staticCompositionLocalOf { DuaActions() }
