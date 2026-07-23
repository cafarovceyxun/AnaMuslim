package com.cafarovceyxun.anamuslim.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json

/** Platform HTTP engine: OkHttp on Android, Darwin on iOS. */
internal expect fun httpClientEngine(): HttpClientEngine

/**
 * Single shared Ktor [HttpClient] replacing the former Retrofit + OkHttp stack.
 *
 * - JSON (de)serialization via the shared [JsonHelper.json].
 * - `X-QuranApp-Version` header injected on every request ([NetworkConfig.appVersionCode]).
 * - Every request URL logged through [NetworkConfig.logger].
 */
object NetworkClient {
    val client: HttpClient by lazy {
        HttpClient(httpClientEngine()) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(JsonHelper.json)
            }
            install(DefaultRequest) {
                header("X-QuranApp-Version", NetworkConfig.appVersionCode())
            }
        }.also { http ->
            http.plugin(HttpSend).intercept { request ->
                NetworkConfig.logger(request.url.buildString())
                execute(request)
            }
        }
    }
}
