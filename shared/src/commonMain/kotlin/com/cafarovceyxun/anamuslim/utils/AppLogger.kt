package com.cafarovceyxun.anamuslim.utils

/**
 * Platform-neutral logging seam, following the project's startup-sink convention
 * (`NetworkConfig`, `RepositoryProvider`). commonMain code logs through here; each platform wires
 * a sink at startup — Android routes to the full logcat/file/Supabase `utils.Log`, iOS keeps the
 * safe `println`/`printStackTrace` defaults below. Unregistered use never crashes.
 *
 * This is deliberately smaller than the Android `Log` (no stack-trace-derived call site, no crash
 * files): commonMain callers only need debug output and error reporting.
 */
object AppLogger {

    /** Debug sink; Android registers logcat + DEBUG-gating, iOS uses [println]. */
    var debugSink: (tag: String?, message: String) -> Unit = { tag, message ->
        println(if (tag != null) "[$tag] $message" else message)
    }

    /** Error sink; Android persists + uploads to Supabase, iOS prints the stack trace. */
    var errorSink: (throwable: Throwable, place: String) -> Unit = { throwable, _ ->
        throwable.printStackTrace()
    }

    fun d(tag: String?, message: String) = debugSink(tag, message)

    fun d(message: String) = debugSink(null, message)

    fun saveError(e: Throwable?, place: String) {
        if (e != null) errorSink(e, place)
    }
}
