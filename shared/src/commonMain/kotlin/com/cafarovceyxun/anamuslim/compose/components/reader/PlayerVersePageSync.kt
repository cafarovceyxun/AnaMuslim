package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Pleyerin qıfılı — səhifə-əsaslı rejimlərdə səsləndirilən ayənin müshəf səhifəsini izləyir.
 *
 * Qıfıl (`ReaderViewModel.playerVerseSync`) bütün rejimlər üçün bir düymədir, izləmə isə hər
 * rejimdə ayrıca yazılmışdı: ayə-ayə siyahısında (`ReaderLayoutVerseMode`) və müshəfdə
 * ([Mushaf]) var idi, **tərcümə** və **kitab** rejimlərində isə heç nə yox idi — düymə basılırdı,
 * ikonu bağlı qıfıla dönürdü və səhifə səsləndirmənin arxasınca getmirdi. Kompilyator da, testlər
 * də bunu tutmur; qıfılı yandırıb səsi dinləməkdən başqa yolu yoxdur.
 *
 * Bu effekt həmin izləməni bir yerə yığır. Səhifəni özü sürüşdürmür — yalnız
 * [ReaderViewModel.requestPageNavigation] çağırır, sürüşdürməni hər rejim öz `navigateToPage`
 * effektində edir (üfüqi vərəqləyici `scrollToPage`, şaquli siyahı `scrollToItem`).
 *
 * [currentPageNo] hər kompozisiyada yeni lambda olur, ona görə [rememberUpdatedState] ilə oxunur —
 * effekt yalnız səsləndirmə vəziyyəti dəyişəndə yenidən qurulur, sürüşmə hər kadrda onu yenidən
 * başlatmır.
 */
@Composable
fun PlayerVersePageSyncEffect(
    readerVm: ReaderViewModel,
    pageCount: Int,
    currentPageNo: () -> Int,
) {
    val playerVerseSync by readerVm.playerVerseSync
    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse

    val readCurrentPage by rememberUpdatedState(currentPageNo)

    LaunchedEffect(playerVerseSync, isPlaying, playingVerse, pageCount) {
        if (!playerVerseSync || !isPlaying || !playingVerse.isValid || pageCount <= 0) {
            return@LaunchedEffect
        }

        val targetPage = readerVm.resolvePageNo(playingVerse.chapterNo, playingVerse.verseNo)
            ?: return@LaunchedEffect

        if (targetPage !in 1..pageCount) return@LaunchedEffect

        snapshotFlow { readCurrentPage() }
            .distinctUntilChanged()
            .collect { current ->
                if (current != targetPage) readerVm.requestPageNavigation(targetPage)
            }
    }
}
