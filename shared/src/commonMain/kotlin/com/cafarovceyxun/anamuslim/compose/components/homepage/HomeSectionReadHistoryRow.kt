package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.cafarovceyxun.anamuslim.compose.utils.formatDateTime
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.strLabelNavHadith
import com.cafarovceyxun.anamuslim.resources.strTitleQuran
import com.cafarovceyxun.anamuslim.resources.strTitleReadHistoryHadith
import com.cafarovceyxun.anamuslim.resources.strTitleReadHistory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource

/**
 * Quran və hədis oxuma tarixçəsi **yan-yana**.
 *
 * Ayarlar → «Ana ekranı düzənlə»-də bölmələr **ayrı** qalır (ayrıca gizlədilir və sıralanır); bu
 * sıra yalnız ikisi də görünən və düzəndə **qonşu** olanda çəkilir — bax `HomeScreen`. Ona görə
 * hansı tərəfin istəndiyi parametrlə gəlir, bölmənin özündən yox.
 *
 * Boş tərəf qutu tutmur ([HomeSplitRow]): yalnız biri doludursa o, tam eni alır və sıra yarımçıq
 * görünmür.
 */
@Composable
fun HomeSectionReadHistoryRow(
    showQuran: Boolean,
    showHadith: Boolean,
) {
    val actions = LocalHomeActions.current
    val scope = rememberCoroutineScope()
    val userRepo = remember { RepositoryProvider.userRepository }
    val quranRepo = remember { RepositoryProvider.quranRepository }

    val quranHistories by userRepo.getHistoriesFlow(HOME_SPLIT_BOX_PREVIEW_LIMIT)
        .collectAsState(emptyList())
    val hadithHistories by userRepo.getHadithHistoriesFlow(HOME_SPLIT_BOX_PREVIEW_LIMIT)
        .collectAsState(emptyList())

    val chapterNames by produceState(initialValue = emptyMap<Int, String>(), key1 = quranHistories) {
        if (quranHistories.isNotEmpty()) {
            value = quranRepo.getChapterNames(quranHistories.map { it.chapterNo })
        }
    }

    val hasQuran = showQuran && quranHistories.isNotEmpty()
    val hasHadith = showHadith && hadithHistories.isNotEmpty()

    // Tək qalan qutu bütün eni tutur — orada qısa «Quran»/«Hədis» başlığı qonşu bölmə ilə
    // qarışırdı (əlfəcin qutusunun başlığı da «Quran»-dır). Yan-yana duranda isə uzun başlıq
    // yarım endə kəsilir, ona görə qısası qalır.
    val quranTitle = stringResource(
        if (hasHadith) Res.string.strTitleQuran else Res.string.strTitleReadHistory
    )
    val hadithTitle = stringResource(
        if (hasQuran) Res.string.strLabelNavHadith else Res.string.strTitleReadHistoryHadith
    )

    HomeSplitRow(
        left = if (hasQuran) {
            {
                HomeSplitBox(
                    icon = Res.drawable.dr_icon_history,
                    title = quranTitle,
                    onViewAll = actions.onOpenReadHistory,
                ) {
                    quranHistories.forEach { history ->
                        val chapterName = chapterNames[history.chapterNo]
                            ?: history.chapterNo.toString()

                        HomeSplitBoxEntry(
                            title = "$chapterName ${history.chapterNo}:${history.fromVerseNo}",
                            subtitle = formatDateTime(history.datetime, "d MMM, HH:mm"),
                            onClick = { actions.onOpenReaderFromHistory(history) },
                        )
                    }
                }
            }
        } else null,
        right = if (hasHadith) {
            {
                HomeSplitBox(
                    icon = Res.drawable.dr_icon_history,
                    title = hadithTitle,
                    onViewAll = actions.onOpenHadithReadHistory,
                ) {
                    hadithHistories.forEach { history ->
                        HomeSplitBoxEntry(
                            title = history.title,
                            subtitle = formatDateTime(history.datetime, "d MMM, HH:mm"),
                            onClick = {
                                // Oxucu istifadəçinin ayarda seçdiyi rejimdə oyanmalıdır — rejim
                                // yazısı ekranın özündən əvvəl getsin ki, açılışda tab dəyişməsi
                                // görünməsin (`HomeSectionHadithReadHistory` ilə eyni qayda).
                                scope.launch {
                                    HadithPreferences.applyDefaultViewMode()
                                    actions.onOpenHadithItem(
                                        history.volumeSlug,
                                        history.bookSlug,
                                        history.chapterSlug,
                                        history.subChapterSlug,
                                        history.title,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        } else null,
    )
}
