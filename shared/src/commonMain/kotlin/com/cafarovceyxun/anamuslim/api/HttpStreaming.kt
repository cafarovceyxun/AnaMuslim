package com.cafarovceyxun.anamuslim.api

import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel

/**
 * Scoped view over a streaming HTTP response. Valid only inside the [downloadStream] /
 * [GithubApi.getTranslation] block — the underlying connection is released when the block returns.
 */
class HttpDownloadScope internal constructor(
    val statusCode: Int,
    val contentLength: Long,
    val channel: ByteReadChannel,
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Streams [url] and hands a [HttpDownloadScope] to [block] for reading the body incrementally.
 * Replaces the former Retrofit `@Streaming` + `Response<ResponseBody>` pattern. The connection
 * stays open for the duration of [block] and is closed afterwards.
 */
suspend fun <T> downloadStream(
    url: String,
    block: suspend (HttpDownloadScope) -> T,
): T = NetworkClient.client.prepareGet(url).execute { response: HttpResponse ->
    block(
        HttpDownloadScope(
            statusCode = response.status.value,
            contentLength = response.contentLength() ?: -1L,
            channel = response.bodyAsChannel(),
        )
    )
}
