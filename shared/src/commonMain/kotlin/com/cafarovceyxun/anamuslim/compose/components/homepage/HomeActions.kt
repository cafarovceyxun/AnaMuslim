package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.runtime.staticCompositionLocalOf
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity

/**
 * Navigation hand-offs the homepage sections need, kept as lambdas so the sections themselves stay
 * platform-neutral. Android wires these to `Intent`/`NavController` (see `rememberHomeActions`);
 * iOS will wire them to [com.cafarovceyxun.anamuslim.compose.navigation.AppNavHost] destinations.
 *
 * Same seam shape as [com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions].
 */
data class HomeActions(
    val onOpenHadithReadHistory: () -> Unit = {},
    val onOpenHadithItem: (
        volumeSlug: String,
        bookSlug: String?,
        chapterSlug: String?,
        subChapterSlug: String?,
        title: String,
    ) -> Unit = { _, _, _, _, _ -> },
    /** Opens the hadith screen at a single hadith, addressed by its id (verse-of-the-day card). */
    val onOpenHadithById: (hadithId: Long?) -> Unit = {},
    val onOpenReadHistory: () -> Unit = {},
    /** Yadda saxlanılanlar siyahısını açır (ana səhifədəki zolağın "hamısı" düyməsi). */
    val onOpenBookmarks: () -> Unit = {},
    /**
     * Təkliflər ekranını açır (ana səhifədəki «Təkliflər» zolağı).
     *
     * Default `{}`-dir, çünki [LocalHomeActions] parametrsiz [HomeActions] ilə qurulur — amma
     * **hər iki host onu verməlidir**: Android `rememberHomeActions`, paylaşılan host isə
     * `rememberNavHomeActions`. Biri unudulsa zolaq həmin platformada səssizcə heç nə etmir.
     */
    val onOpenSuggestions: () -> Unit = {},
    /** Resumes the reader at [history]. Android rebuilds the reader intent via `ReaderFactory`. */
    val onOpenReaderFromHistory: (history: ReadHistoryEntity) -> Unit = {},
)

val LocalHomeActions = staticCompositionLocalOf { HomeActions() }
