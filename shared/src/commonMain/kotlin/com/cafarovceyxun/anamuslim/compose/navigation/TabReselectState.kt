package com.cafarovceyxun.anamuslim.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * The bottom-nav tabs, in bar order.
 *
 * [ordinal] is the index `MainBottomNavigationBar` reports through `onSelect`, and lines up with
 * `rememberMainNavItems()` and [MainRoutes.BOTTOM_NAV_ROUTES]. Reordering any one of those four
 * without the others silently sends taps to the wrong tab.
 */
enum class MainTab { HOME, QURAN, HADITH, SEARCH, SETTINGS }

/**
 * What a tab does when it is tapped while already selected.
 *
 * The bar is route-agnostic and the hosts only own the nav graph, so neither can reach the state a
 * re-tap should act on — a screen's scroll position, its search query, or the in-screen hierarchy
 * `HadithIndexScreen` keeps in `rememberSaveable` state rather than on the back stack. This is that
 * seam, in the same direction as `ReaderChromeState`: the on-screen tab publishes what re-tap means
 * for it, the host just reports the tap.
 *
 * A tab with nothing registered is inert by design — re-tap has no universal fallback, and a tab
 * that has not composed yet must not crash the bar.
 */
object TabReselectState {

    /** Identity wrapper: lets [unregister] tell "my registration" from a newer screen's. */
    private class Registration(val handler: () -> Unit)

    private val registrations = mutableMapOf<MainTab, Registration>()

    private fun register(tab: MainTab, handler: () -> Unit): Any {
        val registration = Registration(handler)
        registrations[tab] = registration
        return registration
    }

    /**
     * Removes [token]'s registration, and only that one.
     *
     * Compose does not guarantee that a leaving screen disposes before the arriving one registers,
     * so an unconditional `remove` would let a disposing screen delete its replacement's handler and
     * leave the tab permanently inert.
     */
    private fun unregister(tab: MainTab, token: Any) {
        if (registrations[tab] === token) registrations.remove(tab)
    }

    /** Called by the host when [tab] was tapped while already selected. */
    fun reselect(tab: MainTab) {
        registrations[tab]?.handler?.invoke()
    }

    /**
     * Registers [onReselect] for as long as the calling composable is in the composition.
     *
     * [onReselect] is read through `rememberUpdatedState`, so it may close over state that changes
     * every recomposition without re-registering on each one.
     */
    @Composable
    fun OnTabReselect(tab: MainTab, onReselect: () -> Unit) {
        val currentHandler by rememberUpdatedState(onReselect)
        DisposableEffect(tab) {
            val token = register(tab) { currentHandler() }
            onDispose { unregister(tab, token) }
        }
    }
}
