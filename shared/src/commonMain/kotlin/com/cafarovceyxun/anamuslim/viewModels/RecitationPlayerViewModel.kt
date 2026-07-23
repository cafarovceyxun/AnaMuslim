package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationServiceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the recitation player session for the player UI, keeping it connected for as long as the
 * UI that owns this view model lives.
 *
 * Like [ReaderProviderViewModel], its dependencies come from the shared startup seams
 * (`RecitationPlayerProvider`, `RepositoryProvider`) rather than an `Application`, so it lives in
 * `commonMain` and the existing `viewModel()` call sites are unchanged.
 */
class RecitationPlayerViewModel : ViewModel() {
    val controller = RecitationPlayerProvider.player
    val repository get() = RepositoryProvider.quranRepository

    init {
        controller.connect()
    }

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }

    val state: StateFlow<RecitationServiceState> = controller.state

    val isPlaying: StateFlow<Boolean> = controller.isPlayingState

    val isLoading: StateFlow<Boolean> = combine(
        controller.state,
        controller.isBufferingState,
    ) { currentState, isBuffering ->
        currentState.resolvingChapterNo != null || isBuffering
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = controller.isLoading,
    )
}
