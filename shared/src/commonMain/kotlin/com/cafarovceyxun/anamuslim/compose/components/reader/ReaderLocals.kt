package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.runtime.staticCompositionLocalOf
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.WbwSheetData
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.viewModels.ReaderProviderViewModel

val LocalReaderViewModel = staticCompositionLocalOf<ReaderProviderViewModel> {
    error("ReaderProviderViewModel not provided")
}

data class LocalRecitationStateData(
    val controller: RecitationPlayer,
    val isAnyPlaying: Boolean,
    val playingVerse: ChapterVersePair,
)

val LocalRecitation = staticCompositionLocalOf<LocalRecitationStateData> {
    error("LocalRecitationState not provided")
}

data class LocalWbwStateData(
    val isWbwRtl: Boolean,
    val activeTooltipWord: AyahWordEntity?,
    val onDismissTooltip: () -> Unit,
    val onForcePlay: (AyahWordEntity) -> Unit,
    val onWordClick: (AyahWordEntity) -> Unit,
    val isWbwAudioLoading: (Int, Int, Int) -> Boolean,
    val toggleWbwSheet: (WbwSheetData?) -> Unit,
    val isWbwSheetOpen: Boolean,
)

val LocalWbwState = staticCompositionLocalOf<LocalWbwStateData> {
    error("LocalWbwState not provided")
}
