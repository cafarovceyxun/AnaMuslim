package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.cafarovceyxun.anamuslim.compose.navigation.AppDestination

/**
 * Back navigation for the Mac build — the same binary running as "Designed for iPhone" on Apple
 * Silicon — where the system back gesture never fires.
 *
 * macOS hands an iOS app the trackpad as *indirect scroll* events, not touches, so the gesture
 * recognizer behind Compose Multiplatform's `enableBackGesture` (on by default; nothing here turns
 * it off) is never triggered: on a Mac the only way back is clicking the app bar's arrow. This
 * holder is the seam between the UIKit side that recognises the Mac inputs
 * ([MacBackHostController], swipe and ⌘) and the composition that knows what "back" means.
 *
 * The action is *not* a plain `popBackStack`: it is dispatched as a real navigation event, so the
 * `BackHandler`s that sit in front of the back stack still get their turn (leave reader fullscreen,
 * collapse the expanded player, exit bookmark selection) exactly as a hardware back does on Android.
 */
object MacBack {

    /**
     * True while a horizontally paged surface owns the screen — the reader. There a two-finger
     * horizontal swipe is a page turn, so the back swipe is narrowed to the left edge (the iOS rule)
     * instead of competing with it. Everywhere else the swipe is accepted anywhere in the window,
     * which is what a Mac user expects.
     */
    var edgeOnly: Boolean = false
        internal set

    private var action: (() -> Unit)? = null

    internal fun bind(action: (() -> Unit)?) {
        this.action = action
    }

    /** Runs the bound back action. False when nothing is bound yet (bootstrap, onboarding). */
    fun perform(): Boolean {
        val bound = action ?: return false
        bound()
        return true
    }
}

/**
 * Publishes the composition's back action to [MacBack] for as long as the nav host is alive.
 *
 * [LocalCompatNavigationEventDispatcherOwner] is the dispatcher Compose Multiplatform's own
 * `BackHandler` registers on, which is why feeding it a [DirectNavigationEventInput] — the API
 * androidx.navigationevent publishes for custom back inputs — reproduces a system back press rather
 * than only popping the graph. It is marked internal to compose-ui, so the binding keeps a
 * `popBackStack` fallback: if a future Compose version drops the local, back still works, it just
 * stops respecting the handlers in front of it.
 */
@OptIn(InternalComposeUiApi::class)
@Composable
fun BindMacBackGestures(navController: NavHostController) {
    val dispatcher = LocalCompatNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val input = remember { DirectNavigationEventInput() }

    DisposableEffect(dispatcher, navController) {
        dispatcher?.addInput(input)
        MacBack.bind {
            if (dispatcher == null) {
                navController.popBackStack()
            } else {
                input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT))
                input.backCompleted()
            }
        }
        onDispose {
            MacBack.bind(null)
            dispatcher?.removeInput(input)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val isReader = backStackEntry?.destination?.hasRoute<AppDestination.Reader>() == true
    SideEffect { MacBack.edgeOnly = isReader }
}
