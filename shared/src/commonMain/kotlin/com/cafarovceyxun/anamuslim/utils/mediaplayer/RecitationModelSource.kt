package com.cafarovceyxun.anamuslim.utils.mediaplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.cafarovceyxun.anamuslim.api.models.recitation2.AvailableRecitationTranslationsModel
import com.cafarovceyxun.anamuslim.api.models.recitation2.AvailableRecitationsModel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences

/**
 * Platform-neutral view of the reciter catalog that shared ViewModels consume. The Android
 * implementation (`RecitationModelManager`, backed by Context/file/network) stays in `:app`;
 * commonMain depends only on this interface, registered via [RecitationModelProvider] at startup.
 * This mirrors the repository DI seam ([RepositoryProvider]) but for a manager-backed source.
 */
interface RecitationModelSource {
    var forceRefreshQuran: Boolean
    var forceRefreshTranslation: Boolean
    suspend fun getAllQuranModel(): AvailableRecitationsModel?
    suspend fun getAllTranslationModel(): AvailableRecitationTranslationsModel?

    /**
     * Display name of the reciter(s) the current audio option selects — one name, or both joined
     * with `&` when Quran and translation are played together. Read by the player UI through
     * [rememberCurrentReciterNameForAudioOption].
     */
    suspend fun getCurrentReciterNameForAudioOption(): String

    /**
     * Downloaded audio totals as `fileCount to reciterCount`, for the storage-cleanup UI.
     * Defaults to nothing downloaded, so platforms without local recitation storage need no impl.
     */
    fun getDownloadedAudioStats(): Pair<Int, Int> = 0 to 0

    /**
     * Removes all downloaded audio (and timing files) for [reciterId]. Defaults to a no-op, so
     * platforms without local recitation storage need no implementation — same rationale as
     * [getDownloadedAudioStats].
     */
    fun deleteReciterAudioDirectory(reciterId: String) = Unit
}

/** Startup-provider seam for [RecitationModelSource]; Android registers, iOS wires it when the
 *  reciter UI arrives (Faza 6). */
object RecitationModelProvider {
    private var provider: (() -> RecitationModelSource)? = null

    fun setSource(provider: () -> RecitationModelSource) {
        this.provider = provider
    }

    val source: RecitationModelSource
        get() = provider?.invoke()
            ?: NoRecitationModelSource
}

/**
 * The current reciter name, recomputed whenever the audio option or either selected reciter
 * changes. Lives here rather than on the platform manager so the player UI stays in `commonMain`.
 */
@Composable
fun rememberCurrentReciterNameForAudioOption(): String {
    val audioOption = RecitationPreferences.observeAudioOption()
    val reciterId = RecitationPreferences.observeReciterId()
    val translationReciterId = RecitationPreferences.observeTranslationReciterId()

    val reciterName by produceState(
        initialValue = "",
        audioOption,
        reciterId,
        translationReciterId,
    ) {
        value = RecitationModelProvider.source.getCurrentReciterNameForAudioOption()
    }

    return reciterName
}

/**
 * Inert catalog for platforms without a reciter-catalog implementation (currently iOS): the
 * recitation screens show their "nothing loaded" state rather than crashing. Same rule as the other
 * download seams.
 */
private object NoRecitationModelSource : RecitationModelSource {
    override var forceRefreshQuran: Boolean = false
    override var forceRefreshTranslation: Boolean = false
    override suspend fun getAllQuranModel(): AvailableRecitationsModel? = null
    override suspend fun getAllTranslationModel(): AvailableRecitationTranslationsModel? = null
    override suspend fun getCurrentReciterNameForAudioOption(): String = ""
}
