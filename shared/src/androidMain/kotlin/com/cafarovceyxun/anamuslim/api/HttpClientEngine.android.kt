package com.cafarovceyxun.anamuslim.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionSpec

internal actual fun httpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        // Preserve the previous OkHttp behaviour: allow modern + legacy TLS and cleartext.
        connectionSpecs(
            listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT,
            )
        )
        cache(null)
    }
}
