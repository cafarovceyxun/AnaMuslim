package com.cafarovceyxun.anamuslim.utils.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Where an in-app APK update currently stands, as the update buttons show it. */
sealed interface SelfUpdateState {
    data object Idle : SelfUpdateState

    /** [fraction] is null while the download size is unknown, so the UI can show no percentage. */
    data class Downloading(val fraction: Float?) : SelfUpdateState

    /** The APK is handed to the system installer; the process may be replaced at any moment. */
    data object Installing : SelfUpdateState

    data object Failed : SelfUpdateState
}

/**
 * Downloads a new build and hands it to the system installer from inside the app, for installs that
 * did not come from a store.
 *
 * A store build must never do this: Google Play forbids self-updating and restricts the install
 * permission, and iOS has no equivalent at all. The work therefore lives behind a seam that only
 * the Android `direct` build registers — everywhere else the update buttons keep opening the
 * store listing.
 */
interface AppSelfUpdate {
    val state: StateFlow<SelfUpdateState>

    /** Starts downloading and installing [versionCode]; a call while one is running does nothing. */
    fun start(versionCode: Long)
}

/**
 * Startup seam handing shared code the platform's [AppSelfUpdate].
 *
 * Registration point: Android `QuranApp.onCreate()`, and only when `BuildConfig.SELF_UPDATE` is set.
 */
object AppSelfUpdateProvider {

    private var provider: (() -> AppSelfUpdate)? = null

    /** Registers how the [AppSelfUpdate] is obtained on this platform. Call once at startup. */
    fun setProvider(provider: () -> AppSelfUpdate) {
        this.provider = provider
    }

    val updater: AppSelfUpdate get() = provider?.invoke() ?: NoAppSelfUpdate

    /**
     * Whether this build registered a self-updater.
     *
     * The inert default below keeps an unregistered seam from crashing, but it cannot download
     * anything — so the update buttons ask this first and fall back to the store listing otherwise,
     * rather than showing a button that quietly does nothing (the rule in `CLAUDE.md`).
     */
    val isAvailable: Boolean get() = provider != null
}

private object NoAppSelfUpdate : AppSelfUpdate {
    override val state: StateFlow<SelfUpdateState> = MutableStateFlow(SelfUpdateState.Idle)
    override fun start(versionCode: Long) = Unit
}
