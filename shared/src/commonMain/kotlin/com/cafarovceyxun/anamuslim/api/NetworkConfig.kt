package com.cafarovceyxun.anamuslim.api

/**
 * Platform-agnostic configuration seam for the shared Ktor network layer.
 *
 * The app registers real providers in `QuranApp.onCreate()` before any request is made.
 * Kept as settable lambdas (not constructor params) because [NetworkClient] is a singleton
 * built lazily and must stay free of app-only dependencies (BuildConfig, Logger).
 */
object NetworkConfig {
    /** App version code, sent as the `X-QuranApp-Version` header on every request. */
    var appVersionCode: () -> String = { "0" }

    /** App version name (e.g. "3.1.3") for display in UI. */
    var appVersionName: () -> String = { "" }

    /** Called with the final request URL for debug logging. */
    var logger: (String) -> Unit = {}
}
