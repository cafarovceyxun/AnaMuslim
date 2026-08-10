package com.cafarovceyxun.anamuslim.compose.utils

/** The home screen widgets the app ships, in the order Settings offers them. */
enum class HomeWidgetKind {
    RecitationPlayer,
    VerseOfTheDay,
}

/**
 * Placing a widget on the home screen from inside the app.
 *
 * A settable sink rather than `expect`/`actual` for the usual reason: the Android implementation
 * needs the `GlanceAppWidgetReceiver` classes, which live in `:app` and are therefore invisible to
 * shared `androidMain`. Same shape as [DailyReminderProvider].
 */
interface HomeWidgetPinner {
    /**
     * Widgets this device can place from inside the app.
     *
     * Empty is a normal answer, not an error: pinning is opt-in for launchers
     * (`isRequestPinAppWidgetSupported`) and older ones simply drop the request, so Settings has to
     * be able to hide the section rather than show a row that does nothing when tapped.
     *
     * Widgets already on the home screen are still listed. A second copy is a legitimate thing to
     * want — a player pinned next to the reading page and another on the main screen — and an entry
     * that vanishes once used is harder to find again than one that simply stays put.
     */
    suspend fun offerableWidgets(): List<HomeWidgetKind>

    /** Asks the launcher to place [kind]. The user confirms in the system's own dialog. */
    fun requestPin(kind: HomeWidgetKind)
}

/** Registered at startup by Android's `QuranApp.onCreate()`. iOS leaves this unset. */
object HomeWidgetPinProvider {
    private var provider: (() -> HomeWidgetPinner)? = null

    fun setProvider(value: () -> HomeWidgetPinner) {
        provider = value
    }

    val pinner: HomeWidgetPinner get() = provider?.invoke() ?: NoHomeWidgetPinner

    /**
     * Whether a platform actually registered a pinner.
     *
     * The inert default keeps an unregistered seam from crashing, but it cannot make the feature
     * work — so the settings section asks this first and stays hidden otherwise, instead of showing
     * a row that quietly does nothing. iOS home screen widgets are added from the OS widget gallery
     * and cannot be requested by the app at all, so there the answer is permanently `false`.
     */
    val isAvailable: Boolean get() = provider != null
}

private object NoHomeWidgetPinner : HomeWidgetPinner {
    override suspend fun offerableWidgets(): List<HomeWidgetKind> = emptyList()
    override fun requestPin(kind: HomeWidgetKind) = Unit
}
