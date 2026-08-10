package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * The owner whose [androidx.lifecycle.ViewModelStore] outlives individual navigation destinations,
 * so screens that must share view-model state (hadith index ↔ hadith items) resolve the *same*
 * instance.
 *
 * On Android this is the hosting `ComponentActivity` — the platform-neutral stand-in for the
 * `viewModel<T>(LocalContext.current as ComponentActivity)` calls the screens used before. Leave it
 * unset and [appScopedViewModelStoreOwner] falls back to [LocalViewModelStoreOwner], which inside a
 * `NavHost` is the current back-stack entry — i.e. *not* shared. Hosts that rely on sharing must
 * provide it.
 */
val LocalAppViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner?> { null }

/** The app-scoped owner, or the nearest one if no app scope was provided. See [LocalAppViewModelStoreOwner]. */
@Composable
fun appScopedViewModelStoreOwner(): ViewModelStoreOwner =
    LocalAppViewModelStoreOwner.current
        ?: checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner available — provide LocalAppViewModelStoreOwner at the app root."
        }
